/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.sloth.gfx.GridMesh;
import haven.sloth.script.pathfinding.Tile;
import modification.dev;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class MCache {
    //All for hitmaps/pathfinding
    private static final Pattern deepwater = Pattern.compile("(gfx/tiles/deep)|(gfx/tiles/odeep)|(gfx/tiles/odeeper)");
    private static final Pattern shallowater = Pattern.compile("(gfx/tiles/water)|(gfx/tiles/owater)");
    private static final Pattern cave = Pattern.compile("(gfx/tiles/cave)|(gfx/tiles/rocks/.+)");
    private static final Tile[] id2tile = new Tile[256];

    //
    public static final Coord2d tilesz = new Coord2d(11, 11);
    public static final Coord tilesz2 = tilesz.round(); /* XXX: Remove me in due time. */
    public static final Coord cmaps = new Coord(100, 100);
    public static final Coord2d cmapsd = new Coord2d(cmaps);
    public static final Coord cutsz = new Coord(25, 25);
    public static final Coord cutn = cmaps.div(cutsz);
    private final Object setmon = new Object();
    public Resource.Spec[] nsets = new Resource.Spec[256];
    @SuppressWarnings("unchecked")
    public Reference<Resource>[] sets = new Reference[256];
    @SuppressWarnings("unchecked")
    public Reference<Tileset>[] csets = new Reference[256];
    @SuppressWarnings("unchecked")
    public Reference<Tiler>[] tiles = new Reference[256];
    private final Waitable.Queue gridwait = new Waitable.Queue();
    final Map<Coord, Request> req = new HashMap<>();
    final Map<Coord, Grid> grids = Collections.synchronizedMap(new HashMap<>());
    Session sess;
    final Set<Overlay> ols = new HashSet<>();
    public int olseq = 0, chseq = 0;
    final Map<Integer, Defrag> fragbufs = new TreeMap<>();

    public static class LoadingMap extends Loading {
        public final Coord gc;
        private final transient MCache map;

        public LoadingMap(MCache map, Coord gc) {
            super("Waiting for map data...");
            this.gc = gc;
            this.map = map;
        }

        public void waitfor(Runnable callback, Consumer<Waiting> reg) {
            synchronized (map.grids) {
                if (map.grids.containsKey(gc)) {
                    reg.accept(Waiting.dummy);
                    callback.run();
                } else {
                    reg.accept(new Waitable.Checker(callback) {
                        protected Object monitor() {
                            return (map.grids);
                        }

                        double st = Utils.rtime();

                        protected boolean check() {
                            if ((Utils.rtime() - st > 5)) {
                                st = Utils.rtime();
                                return (true);
                            }
                            return (map.grids.containsKey(gc));
                        }

                        protected Waitable.Waiting add() {
                            return (map.gridwait.add(this));
                        }
                    }.addi());
                }
            }
        }
    }

    private static class Request {
        private long lastreq = 0;
        private int reqs = 0;
    }

    public static interface ZSurface {
        public default double getz(Coord tc) {
            return (getz(tc.mul(tilesz)));
        }

        public default double getz(Coord2d pc) {
            double tw = tilesz.x, th = tilesz.y;
            Coord ul = Coord.of(Utils.floordiv(pc.x, tw), Utils.floordiv(pc.y, th));
            double sx = (pc.x - (ul.x * tw)) / tw, ix = 1.0 - sx;
            double sy = (pc.y - (ul.y * th)) / th, iy = 1.0 - sy;
            try {
                return ((iy * ((ix * getz(ul)) + (sx * getz(ul.add(1, 0))))) +
                        (sy * ((ix * getz(ul.add(0, 1))) + (sx * getz(ul.add(1, 1))))));
            } catch (ArrayIndexOutOfBoundsException e) {
                Debug.dump(pc, ul, sx, sy);
                throw (e);
            }
        }

        public default Coord3f getnorm(Coord2d pc) {
            return (getnormt(pc));
        }

        public default Coord3f getnormt(Coord2d pc) {
            double tw = tilesz.x, th = tilesz.y;
            Coord ul = Coord.of(Utils.floordiv(pc.x, tw), Utils.floordiv(pc.y, th));
            double sx = (pc.x - (ul.x * tw)) / tw, ix = 1.0 - sx;
            double sy = (pc.y - (ul.y * th)) / th, iy = 1.0 - sy;
            double z0 = getz(ul), z1 = getz(ul.add(1, 0)), z2 = getz(ul.add(1, 1)), z3 = getz(ul.add(0, 1));
            double nx = ((z1 * iy) + (z2 * sy)) - ((z0 * iy) + (z3 * sy));
            double ny = ((z3 * iy) + (z2 * sy)) - ((z0 * iy) + (z1 * sy));
            return (Coord3f.of((float) tw, 0, (float) nx).cmul(0, (float) th, (float) ny).norm());
        }

        public default Coord3f getnormp(Coord2d pc) {
            double D = 0.01;
            Coord2d tul = pc.sub(pc.mod(tilesz)), tbr = tul.add(tilesz);
            double l = Math.max(pc.x - D, tul.x), u = Math.max(pc.y - D, tul.y);
            double r = Math.min(pc.x + D, tbr.x), b = Math.min(pc.y + D, tbr.y);
            double z0 = getz(Coord2d.of(pc.x, u));
            double z1 = getz(Coord2d.of(r, pc.y));
            double z2 = getz(Coord2d.of(pc.x, b));
            double z3 = getz(Coord2d.of(l, pc.y));
            return (Coord3f.of((float) (r - l), 0, (float) (z1 - z3)).cmul(0, (float) (b - u), (float) (z2 - z0)).norm());
        }
    }

    public static class SurfaceID {
        public final SurfaceID parent;

        public SurfaceID(SurfaceID parent) {
            this.parent = parent;
        }

        public boolean hasparent(SurfaceID p) {
            for (SurfaceID id = this; id != null; id = id.parent) {
                if (id == p)
                    return (true);
            }
            return (false);
        }

        public static final SurfaceID map = new SurfaceID(null);
        public static final SurfaceID trn = new SurfaceID(map);
    }

    public final Gob.Placer mapplace = new Gob.DefaultPlace(this, SurfaceID.map);
    public final Gob.Placer trnplace = new Gob.DefaultPlace(this, SurfaceID.trn);

    public static interface OverlayInfo {
        public Collection<String> tags();

        public Material mat();
    }

    @Resource.LayerName("overlay")
    public static class ResOverlay extends Resource.Layer implements OverlayInfo {
        public final Collection<String> tags;
        private final int matid, omatid;

        public ResOverlay(Resource res, Message buf) {
            res.super();
            int ver = buf.uint8();
            if (ver == 1) {
                int matid = 0;
                int omatid = 0;
                Collection<String> tags = Collections.emptyList();
                Object[] data = buf.list();
                for (Object argp : data) {
                    Object[] arg = (Object[]) argp;
                    switch ((String) arg[0]) {
                        case "tags": {
                            ArrayList<String> tbuf = new ArrayList<>();
                            for (int i = 1; i < arg.length; i++)
                                tbuf.add(((String) arg[i]).intern());
                            tbuf.trimToSize();
                            tags = tbuf;
                            break;
                        }
                        case "mat": {
                            matid = Utils.iv(arg[1]);
                            break;
                        }
                        case "omat": {
                            omatid = Utils.iv(arg[1]);
                            break;
                        }
                    }
                }
                this.matid = matid;
                this.omatid = omatid;
                this.tags = tags;
            } else {
                throw (new Resource.LoadException("unknown overlay version: " + ver, res));
            }
        }

        public void init() {
        }

        public Collection<String> tags() {
            return (tags);
        }

        public Material mat() {
            return (getres().flayer(Material.Res.class, matid).get());
        }

        public Material omat() {
            if(omatid < 0)
                return(null);
            return(getres().flayer(Material.Res.class, omatid).get());
        }

        public String toString() {
            return (String.format("#<res-overlay %s %d>", getres().name, matid));
        }
    }

    public class Overlay {
        private Area a;
        private final OverlayInfo id;

        public Overlay(Area a, OverlayInfo id) {
            this.a = a;
            this.id = id;
            ols.add(this);
            olseq++;
        }

        @Deprecated
        public Overlay(Coord c1, Coord c2, int mask) {
            this(new Area(c1, c2.add(1, 1)), olres.get(Integer.numberOfTrailingZeros(mask)).layer(ResOverlay.class));
        }

        public void destroy() {
            ols.remove(this);
            olseq++;
        }

        public void update(Area a) {
            if (!a.equals(this.a)) {
                olseq++;
                this.a = a;
            }
        }

        @Deprecated
        public void update(Coord c1, Coord c2) {
            update(new Area(c1, c2.add(1, 1)));
        }

        public Area getA() {
            return a;
        }

        public Coord getc1() {
            return a.ul;
        }

        public Coord getc2() {
            return a.br.sub(1, 1);
        }
    }

    private void cktileid(int id) {
        if (id >= nsets.length) {
            synchronized (setmon) {
                if (id >= nsets.length) {
                    nsets = Utils.extend(nsets, Integer.highestOneBit(id) * 2);
                    sets = Utils.extend(sets, Integer.highestOneBit(id) * 2);
                    csets = Utils.extend(csets, Integer.highestOneBit(id) * 2);
                    tiles = Utils.extend(tiles, Integer.highestOneBit(id) * 2);
                }
            }
        }
    }

    /* XXX: To be abolished */
    public static final Map<Integer, Resource> olres = Utils.<Integer, Resource>map()
            .put(0, Resource.remote().loadwait("gfx/tiles/overlay/cplot-f"))
            .put(1, Resource.remote().loadwait("gfx/tiles/overlay/cplot-o"))
            .put(2, Resource.remote().loadwait("gfx/tiles/overlay/vlg-f"))
            .put(3, Resource.remote().loadwait("gfx/tiles/overlay/vlg-o"))
            .put(4, Resource.remote().loadwait("gfx/tiles/overlay/realm-f"))
            .put(5, Resource.remote().loadwait("gfx/tiles/overlay/realm-o"))
            .put(16, Resource.remote().loadwait("gfx/tiles/overlay/cplot-s"))
            .put(17, Resource.remote().loadwait("gfx/tiles/overlay/sel"))
            .map();

    public class Grid {
        public final Coord gc, ul;
        public final int tiles[] = new int[cmaps.x * cmaps.y];
        public final float z[] = new float[cmaps.x * cmaps.y];
        public final Tile hitmap[] = new Tile[cmaps.x * cmaps.y];
        public Indir<Resource> ols[];
        public boolean ol[][];
        public long id;
        public int seq = -1;
        private int olseq = -1;
        private final Cut cuts[];
        private Collection<Gob>[] fo = null;

        private class Cut {
            MapMesh mesh;
            Defer.Future<MapMesh> dmesh;

            //Grid layout view
            FastMesh grid;
            Defer.Future<FastMesh> dgrid;

            Map<OverlayInfo, Rendered> ols = new HashMap<>();
        }

        private class Flavobj extends Gob {
            private Flavobj(Coord2d c, double a) {
                super(sess.glob, c);
                this.a = a;
            }

            public Random mkrandoom() {
                Random r = new Random(Grid.this.id);
                r.setSeed(r.nextLong() ^ Double.doubleToLongBits(rc.x));
                r.setSeed(r.nextLong() ^ Double.doubleToLongBits(rc.y));
                return (r);
            }
        }

        private class Flavdraw extends ResDrawable {
            final GLState extra;

            Flavdraw(Gob gob, Indir<Resource> res, Message sdt, GLState extra) {
                super(gob, res, sdt);
                this.extra = extra;
            }

            public void setup(RenderList rl) {
                if (!inited) return;
                rl.add(spr, extra);
            }
        }

        public Grid(Coord gc) {
            this.gc = gc;
            this.ul = gc.mul(cmaps);
            cuts = new Cut[cutn.x * cutn.y];
            for (int i = 0; i < cuts.length; i++)
                cuts[i] = new Cut();
        }

        public Tile gethitmap(Coord tc) {
            return hitmap[tc.x + (tc.y * cmaps.x)];
        }

        public void sethitmap(Coord tc, Tile t) {
            hitmap[tc.x + (tc.y * cmaps.x)] = t;
        }

        public int gettile(Coord tc) {
            return (tiles[tc.x + (tc.y * cmaps.x)]);
        }

        public double getz(Coord tc) {
            return (z[tc.x + (tc.y * cmaps.x)]);
        }

        public void getol(OverlayInfo id, Area a, boolean[] buf) {
            for (int i = 0; i < ols.length; i++) {
                if (ols[i].get().layer(ResOverlay.class) == id) {
                    int o = 0;
                    for (Coord c : a)
                        buf[o++] = ol[i][c.x + (c.y * cmaps.x)];
                    return;
                }
            }
            Arrays.fill(buf, false);
        }

        private void makeflavor() {
            @SuppressWarnings("unchecked")
            Collection<Gob>[] fo = (Collection<Gob>[]) new Collection[cutn.x * cutn.y];
            for (int i = 0; i < fo.length; i++)
                fo[i] = new LinkedList<Gob>();
            Coord c = new Coord(0, 0);
            Coord tc = gc.mul(cmaps);
            {
                int i = 0;
                Random rnd = new Random(id);
                for (c.y = 0; c.y < cmaps.x; c.y++) {
                    for (c.x = 0; c.x < cmaps.y; c.x++, i++) {
                        Tileset set = tileset(tiles[i]);
                        int fp = rnd.nextInt();
                        int rp = rnd.nextInt();
                        double a = rnd.nextDouble();
                        if (set != null && set.flavobjs.size() > 0) {
                            if ((fp % set.flavprob) == 0) {
                                Indir<Resource> r = set.flavobjs.pick(rp % set.flavobjs.tw);
                                if (Config.hideflovisual) {
                                    Resource res = r.get();
                                    if (res != null && res.name.startsWith("gfx/tiles/"))
                                        continue;
                                }
                                Gob g = new Flavobj(c.add(tc).mul(tilesz).add(tilesz.div(2)), a * 2 * Math.PI);
                                g.setattr(new Flavdraw(g, r, Message.nil, set.flavobjmat));
                                Coord cc = c.div(cutsz);
                                fo[cc.x + (cc.y * cutn.x)].add(g);
                            }
                        }
                    }
                }
            }

//            Area area = Area.sized(cutc.mul(cutsz), cutsz);
//            Area garea = area.xl(gc.mul(cmaps));
//            Random rnd = new Random(id + cutc.x + (cutc.y * cutn.x));
//            Tileset.Flavor.Buffer buf = new Tileset.Flavor.Buffer(sess.glob, garea, rnd.nextLong());
//
//            int[] ids = new int[16];
//            int nids = 0;
//            {
//                boolean[] uids = new boolean[nsets.length];
//                int i = area.ul.x + (area.ul.y * cmaps.x);
//                for(int y = 0; y < cutsz.y; y++, i += (cmaps.x - cutsz.x)) {
//                    for(int x = 0; x < cutsz.x; x++, i++) {
//                        int id = tiles[i];
//                        if(!uids[id]) {
//                            uids[id] = true;
//                            if(nids >= ids.length)
//                                ids = Arrays.copyOf(ids, ids.length * 2);
//                            ids[nids++] = id;
//                        }
//                    }
//                }
//            }
//            for(int i = 0; i < nids; i++) {
//                Tileset.Flavor.Terrain trn = new Tileset.Flavor.Terrain(this, MCache.this, ids[i], garea, area.ul.sub(garea.ul));
//                Tileset set = trn.tileset(ids[i]);
//                int o = 0;
//                for(Indir<Tileset.Flavor> flp : set.flavors) {
//                    rnd.setSeed(buf.seed ^ (ids[i] << 16) ^ o);
//                    flp.get().flavor(buf, trn, rnd);
//                    o++;
//                }
//            }
            this.fo = fo;
        }

        public Collection<Gob> getfo(Coord cc) {
            if (fo == null)
                makeflavor();
            return (fo[cc.x + (cc.y * cutn.x)]);
        }

        private Cut geticut(Coord cc) {
            return (cuts[cc.x + (cc.y * cutn.x)]);
        }

        public MapMesh getcut(Coord cc) {
            Cut cut = geticut(cc);
            if (cut.dmesh != null) {
                if (!cut.dmesh.done()) throw new LoadingMap(MCache.this, cc);
                if (cut.dmesh.done() || (cut.mesh == null)) {
                    MapMesh old = cut.mesh;
                    cut.mesh = cut.dmesh.get();
                    cut.dmesh = null;
                    cut.ols.clear();
                    if (old != null)
                        old.dispose();
                }
            }
            return (cut.mesh);
        }

        public Optional<MapMesh> getcuto(Coord cc) {
            Cut cut = geticut(cc);
            if (cut.dmesh != null) {
                MapMesh nmesh = null;
                if (cut.dmesh.done()) {
                    nmesh = cut.dmesh.get();
                    cut.dmesh = null;
                }
                if ((nmesh != null) || (cut.mesh == null)) {
                    MapMesh old = cut.mesh;
                    cut.mesh = nmesh;
                    cut.ols.clear();
                    if (old != null)
                        old.dispose();
                }
            }
            return (Optional.ofNullable(cut.mesh));
        }

        /**
         * returns the Grid layout cut given the coordinate
         */
        public FastMesh getgcut(Coord cc) {
            Cut cut = geticut(cc);
            if (cut.dgrid != null) {
                if (cut.dgrid.done() || cut.grid == null) {
                    FastMesh old = cut.grid;
                    cut.grid = cut.dgrid.get();
                    cut.dgrid = null;
                    if (old != null)
                        old.dispose();
                }
            }
            return cut.grid;
        }

        public Optional<FastMesh> getgcuto(Coord cc) {
            Cut cut = geticut(cc);
            if (cut.dgrid != null) {
                FastMesh nmesh = null;
                if (cut.dgrid.done()) {
                    nmesh = cut.dgrid.get();
                    cut.dgrid = null;
                }
                if ((nmesh != null) || (cut.grid == null)) {
                    FastMesh old = cut.grid;
                    cut.grid = nmesh;
                    if (old != null)
                        old.dispose();
                }
            }
            return (Optional.ofNullable(cut.grid));
        }

        public Rendered getolcut(OverlayInfo id, Coord cc) {
            int nseq = MCache.this.olseq;
            if (this.olseq != nseq) {
                for (int i = 0; i < cutn.x * cutn.y; i++) {
                    for (Rendered r : cuts[i].ols.values()) {
                        if (r instanceof Disposable)
                            ((Disposable) r).dispose();
                    }
                    cuts[i].ols.clear();
                }
                this.olseq = nseq;
            }
            Cut cut = geticut(cc);
            if (!cut.ols.containsKey(id))
                cut.ols.put(id, getcut(cc).makeol(id));
            return (cut.ols.get(id));
        }

        private final List<Pair<OverlayInfo, AtomicBoolean>> olloaded = new ArrayList<>();
        private final List<Pair<OverlayInfo, AtomicReference<Rendered>>> ollast = new ArrayList<>();

        public Optional<Rendered> getolcuto(OverlayInfo id, Coord cc) {
            int nseq = MCache.this.olseq;
            if (this.olseq != nseq) {
                for (int i = 0; i < cutn.x * cutn.y; i++) {
                    for (Rendered r : cuts[i].ols.values()) {
                        if (r instanceof Disposable)
                            ((Disposable) r).dispose();
                    }
                    cuts[i].ols.clear();
                }
                this.olseq = nseq;
            }
            Cut cut = geticut(cc);
            AtomicReference<Rendered> ret = new AtomicReference<>();
            if (cut.ols.containsKey(id)) ret.set(cut.ols.get(id));
            else {
                Pair<OverlayInfo, AtomicBoolean> pb = olloaded.stream().filter(p -> p.a.equals(id)).findFirst().orElseGet(() -> {
                    Pair<OverlayInfo, AtomicBoolean> p = new Pair<>(id, new AtomicBoolean(true));
                    olloaded.add(p);
                    return (p);
                });
                Pair<OverlayInfo, AtomicReference<Rendered>> pr = ollast.stream().filter(p -> p.a.equals(id)).findFirst().orElseGet(() -> {
                    Pair<OverlayInfo, AtomicReference<Rendered>> p = new Pair<>(id, new AtomicReference<>());
                    ollast.add(p);
                    return (p);
                });
                if (pb.b.get()) {
                    getcuto(cc).ifPresent(mm -> {
                        try {
                            cut.ols.put(id, mm.makeol(id));
                            pr.b.set(cut.ols.get(id));
                            ret.set(pr.b.get());
                        } catch (Loading l) {
                            pb.b.set(false);
                            ret.set(pr.b.get());
                            l.waitfor(() -> pb.b.set(true), w -> {});
                        }
                    });
                } else {
                    ret.set(pr.b.get());
                }
            }
            return (Optional.ofNullable(ret.get()));
        }

        private void buildcut(final Coord cc) {
            final Cut cut = geticut(cc);
            Defer.Future<?> prev = cut.dmesh;
            cut.dmesh = Defer.later(new Defer.Callable<MapMesh>() {
                public MapMesh call() {
                    Random rnd = new Random(id);
                    rnd.setSeed(rnd.nextInt() ^ cc.x);
                    rnd.setSeed(rnd.nextInt() ^ cc.y);
                    return (MapMesh.build(MCache.this, rnd, ul.add(cc.mul(cutsz)), cutsz));
                }

                public String toString() {
                    return ("Building map...");
                }
            });
            if (prev != null)
                prev.cancel();
            //automatically build a grid mesh with every cut
            buildgcut(cc);
        }

        /**
         * Builds the grid layout mesh
         */
        private void buildgcut(final Coord cc) {
            final Cut cut = geticut(cc);
            Defer.Future<?> gprev = cut.dgrid;
            Random rnd = new Random(id);
            cut.dgrid = Defer.later(new Defer.Callable<FastMesh>() {
                public FastMesh call() {
                    return (GridMesh.build(MCache.this, ul.add(cc.mul(cutsz)), cutsz));
                }

                public String toString() {
                    return ("Building grid overlay...");
                }
            });
            if (gprev != null)
                gprev.cancel();
        }

        public void ivneigh(Coord nc) {
            Coord cc = new Coord();
            for (cc.y = 0; cc.y < cutn.y; cc.y++) {
                for (cc.x = 0; cc.x < cutn.x; cc.x++) {
                    if ((((nc.x < 0) && (cc.x == 0)) || ((nc.x > 0) && (cc.x == cutn.x - 1)) || (nc.x == 0)) &&
                            (((nc.y < 0) && (cc.y == 0)) || ((nc.y > 0) && (cc.y == cutn.y - 1)) || (nc.y == 0))) {
                        buildcut(new Coord(cc));
                    }
                }
            }
        }

        public void tick(int dt) {
            if (fo != null) {
                for (Collection<Gob> fol : fo) {
                    for (Gob fo : fol)
                        fo.ctick(dt);
                }
            }
        }

        private void invalidate() {
            for (int y = 0; y < cutn.y; y++) {
                for (int x = 0; x < cutn.x; x++)
                    buildcut(new Coord(x, y));
            }
            fo = null;
            for (Coord ic : new Coord[]{
                    new Coord(-1, -1), new Coord(0, -1), new Coord(1, -1),
                    new Coord(-1, 0), new Coord(1, 0),
                    new Coord(-1, 1), new Coord(0, 1), new Coord(1, 1)}) {
                Grid ng = grids.get(gc.add(ic));
                if (ng != null)
                    ng.ivneigh(ic.inv());
            }
        }

        public void dispose() {
            for (Cut cut : cuts) {
                if (cut.dmesh != null)
                    cut.dmesh.cancel();
                if (cut.mesh != null)
                    cut.mesh.dispose();
                if (cut.ols != null) {
                    for (Rendered r : cut.ols.values()) {
                        if (r instanceof Disposable)
                            ((Disposable) r).dispose();
                    }
                }
            }
        }

        private void oldfill(Message msg) {
            int[] pfl = new int[256];
            while (true) {
                int pidx = msg.uint8();
                if (pidx == 255)
                    break;
                pfl[pidx] = msg.uint8();
            }
            try (ZMessage blob = new ZMessage(msg)) {
                id = blob.int64();
                while (true) {
                    int tileid = blob.uint8();
                    if (tileid == 255)
                        break;
                    String resnm = blob.string();
                    int resver = blob.uint16();
                    nsets[tileid] = new Resource.Spec(Resource.remote(), resnm, resver);

                    if (shallowater.matcher(resnm).matches()) {
                        id2tile[tileid] = Tile.SHALLOWWATER;
                    } else if (deepwater.matcher(resnm).matches()) {
                        id2tile[tileid] = Tile.DEEPWATER;
                    } else if (cave.matcher(resnm).matches()) {
                        id2tile[tileid] = Tile.CAVE;
                    }
                }
                for (int i = 0; i < tiles.length; i++) {
                    tiles[i] = blob.uint8();

                    //we can figure out shallow vs deep hitmap from this info, ridges will come later
                    hitmap[i] = id2tile[tiles[i]];
                }
                for (int i = 0; i < z.length; i++)
                    z[i] = blob.int16();
                @SuppressWarnings("unchecked")
                Indir<Resource>[] olids = new Indir[0];
                boolean[][] ols = {};
                while (true) {
                    int pidx = blob.uint8();
                    if (pidx == 255)
                        break;
                    int fl = pfl[pidx];
                    int type = blob.uint8();
                    Coord c1 = new Coord(blob.uint8(), blob.uint8());
                    Coord c2 = new Coord(blob.uint8(), blob.uint8());
                    Indir<Resource> olid;
                    if (type == 0) {
                        if ((fl & 1) == 1)
                            olid = olres.get(1).indir();
                        else
                            olid = olres.get(0).indir();
                    } else if (type == 1) {
                        if ((fl & 1) == 1)
                            olid = olres.get(3).indir();
                        else
                            olid = olres.get(2).indir();
                    } else if (type == 2) {
                        if ((fl & 1) == 1)
                            olid = olres.get(5).indir();
                        else
                            olid = olres.get(4).indir();
                    } else {
                        throw (new RuntimeException("Unknown plot type " + type));
                    }
                    int oi;
                    find:
                    {
                        for (oi = 0; oi < olids.length; oi++) {
                            if (olids[oi] == olid)
                                break find;
                        }
                        olids = Arrays.copyOf(olids, oi + 1);
                        ols = Arrays.copyOf(ols, oi + 1);
                        olids[oi] = olid;
                    }
                    boolean[] ol = ols[oi];
                    if (ol == null)
                        ols[oi] = ol = new boolean[cmaps.x * cmaps.y];
                    for (int y = c1.y; y <= c2.y; y++) {
                        for (int x = c1.x; x <= c2.x; x++) {
                            ol[x + (y * cmaps.x)] = true;
                        }
                    }
                }
                this.ols = olids;
                this.ol = ols;
            } catch (IOException e) {
                dev.simpleLog(e);
            }
        }

        private void filltiles(Message buf) {
            while (true) {
                int tileid = buf.uint8();
                if (tileid == 255)
                    break;
                String resnm = buf.string();
                int resver = buf.uint16();
                cktileid(tileid);
                nsets[tileid] = new Resource.Spec(Resource.remote(), resnm, resver);

                if (shallowater.matcher(resnm).matches()) {
                    id2tile[tileid] = Tile.SHALLOWWATER;
                } else if (deepwater.matcher(resnm).matches()) {
                    id2tile[tileid] = Tile.DEEPWATER;
                } else if (cave.matcher(resnm).matches()) {
                    id2tile[tileid] = Tile.CAVE;
                }
            }
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = buf.uint8();
                hitmap[i] = id2tile[tiles[i]];
                if (nsets[tiles[i]] == null)
                    throw (new Message.FormatError(String.format("Got undefined tile: " + tiles[i])));
            }
        }

        private void filltiles2(Message buf) {
            int[] tileids = new int[1];
            int maxid = 0;
            while (true) {
                int encid = buf.uint16();
                if (encid == 65535)
                    break;
                maxid = Math.max(maxid, encid);
                int tileid = buf.uint16();
                if (encid >= tileids.length)
                    tileids = Utils.extend(tileids, Integer.highestOneBit(encid) * 2);
                tileids[encid] = tileid;
                String resnm = buf.string();
                int resver = buf.uint16();
                cktileid(tileid);
                nsets[tileid] = new Resource.Spec(Resource.remote(), resnm, resver);

                if (shallowater.matcher(resnm).matches()) {
                    id2tile[tileid] = Tile.SHALLOWWATER;
                } else if (deepwater.matcher(resnm).matches()) {
                    id2tile[tileid] = Tile.DEEPWATER;
                } else if (cave.matcher(resnm).matches()) {
                    id2tile[tileid] = Tile.CAVE;
                }
            }
            boolean lg = maxid >= 256;
            for (int i = 0; i < tiles.length; i++) {
                tiles[i] = tileids[lg ? buf.uint16() : buf.uint8()];
                hitmap[i] = id2tile[tiles[i]];
                if (nsets[tiles[i]] == null)
                    throw (new Message.FormatError(String.format("Got undefined tile: " + tiles[i])));
            }
        }

        private void fillz(Message buf) {
            int fmt = buf.uint8();
            if (fmt == 0) {
                float z = buf.float32() * 11;
                for (int i = 0; i < this.z.length; i++)
                    this.z[i] = z;
            } else if (fmt == 1) {
                float min = buf.float32() * 11, q = buf.float32() * 11;
                for (int i = 0; i < z.length; i++)
                    z[i] = min + (buf.uint8() * q);
            } else if (fmt == 2) {
                float min = buf.float32() * 11, q = buf.float32() * 11;
                for (int i = 0; i < z.length; i++)
                    z[i] = min + (buf.uint16() * q);
            } else if (fmt == 3) {
                for (int i = 0; i < z.length; i++)
                    z[i] = buf.float32() * 11;
            } else {
                throw (new Message.FormatError(String.format("Unknown z-map format: %d", fmt)));
            }
        }

        private Indir<Resource>[] fill_plots;

        private void decplots(Message buf) {
            @SuppressWarnings("unchecked") Indir<Resource>[] pt = new Indir[256];
            while (!buf.eom()) {
                int pidx = buf.uint8();
                if (pidx == 255)
                    break;
                pt[pidx] = sess.getres(buf.uint16());
            }
            fill_plots = pt;
        }

        private void fillplots(Message buf) {
            if (fill_plots == null)
                return;
            @SuppressWarnings("unchecked") Indir<Resource>[] olids = new Indir[0];
            boolean[][] ols = {};
            while (!buf.eom()) {
                int pidx = buf.uint8();
                if (pidx == 255)
                    break;
                int fl = buf.uint8();
                Coord c1 = new Coord(buf.uint8(), buf.uint8());
                Coord c2 = new Coord(buf.uint8(), buf.uint8());
                boolean[] mask = new boolean[(c2.x - c1.x) * (c2.y - c1.y)];
                if ((fl & 1) != 0) {
                    for (int i = 0, l = 0, m = buf.uint8(); i < mask.length; i++) {
                        if (l >= 8) {
                            m = buf.uint8();
                            l = 0;
                        }
                        mask[i] = (m & 1) != 0;
                        m >>= 1;
                        l++;
                    }
                } else {
                    for (int i = 0; i < mask.length; i++)
                        mask[i] = true;
                }
                Indir<Resource> olid = fill_plots[pidx];
                if (olid == null)
                    continue;
                int oi;
                find:
                {
                    for (oi = 0; oi < olids.length; oi++) {
                        if (olids[oi] == olid)
                            break find;
                    }
                    olids = Arrays.copyOf(olids, oi + 1);
                    ols = Arrays.copyOf(ols, oi + 1);
                    olids[oi] = olid;
                }
                boolean[] ol = ols[oi];
                if (ol == null)
                    ols[oi] = ol = new boolean[cmaps.x * cmaps.y];
                for (int y = c1.y, mi = 0; y < c2.y; y++) {
                    for (int x = c1.x; x < c2.x; x++) {
                        ol[x + (y * cmaps.x)] |= mask[mi++];
                    }
                }
            }
            this.ols = olids;
            this.ol = ols;
            fill_plots = null;
        }

        private void subfill(Message msg) {
            while (!msg.eom()) {
                String lnm = msg.string();
                int len = msg.uint8();
                if ((len & 0x80) != 0)
                    len = msg.int32();
                Message buf = new LimitMessage(msg, len);
                switch (lnm) {
                    case "z":
                        subfill(new ZMessage(buf));
                        break;
                    case "m":
                        id = buf.int64();
                        break;
                    case "t":
                        filltiles(buf);
                        break;
                    case "t2":
                        filltiles2(buf);
                        break;
                    case "h":
                        fillz(buf);
                        break;
                    case "pi":
                        decplots(buf);
                        break;
                    case "p":
                        fillplots(buf);
                        break;
                }
                buf.skip();
            }
        }

        public void fill(Message msg) {
            int ver = msg.uint8();
            if (ver == 0) {
                oldfill(msg);
            } else if (ver == 1) {
                subfill(msg);
            } else {
                throw (new RuntimeException("Unknown map data version " + ver));
            }
            invalidate();
            seq++;
        }
    }

    public MCache(Session sess) {
        this.sess = sess;
    }

    public void ctick(int dt) {
        Collection<Grid> copy = new ArrayList<>(grids.values());
        for (Grid g : copy)
            g.tick(dt);
    }

    public void invalidateAll() {
        Collection<Grid> copy = new ArrayList<>(grids.values());
        for (Grid gr : copy)
            gr.invalidate();
    }

    public void invalidate(Coord cc) {
        synchronized (req) {
            if (req.get(cc) == null)
                req.put(cc, new Request());
        }
    }

    public void invalblob(Message msg) {
        int type = msg.uint8();
        if (type == 0) {
            invalidate(msg.coord());
        } else if (type == 1) {
            Coord ul = msg.coord();
            Coord lr = msg.coord();
            synchronized (this) {
                trim(ul, lr);
            }
        } else if (type == 2) {
            synchronized (this) {
                trimall();
            }
        }
    }

    private Grid cached = null;

    public Grid getgrid(Coord gc) {
        synchronized (grids) {
            if ((cached == null) || !cached.gc.equals(gc)) {
                cached = grids.get(gc);
                if (cached == null) {
                    request(gc);
                    throw (new LoadingMap(this, gc));
                }
            }
            return (cached);
        }
    }

    public Optional<Grid> getgrido(final Coord gc) {
        synchronized (grids) {
            if ((cached == null) || !cached.gc.equals(gc)) {
                cached = grids.get(gc);
                if (cached == null) {
                    request(gc);
                    return (Optional.empty());
                }
            }
            return (Optional.of(cached));
        }
    }

    public Optional<Grid> getgrido(final long id) {
        synchronized (grids) {
            if ((cached == null) || cached.id != id) {
                for (Grid g : grids.values()) {
                    if (g.id == id) {
                        cached = g;
                        return (Optional.of(g));
                    }
                }
                return (Optional.empty());
            } else {
                return (Optional.of(cached));
            }
        }
    }

    public Grid getgridt(Coord tc) {
        return (getgrid(tc.div(cmaps)));
    }

    public Optional<Grid> getgridto(Coord tc) {
        return (getgrido(tc.div(cmaps)));
    }

    public int gettile_safe(Coord tc) {
        synchronized (grids) {
            final Optional<Grid> grid = getgridto(tc);
            if (grid.isPresent()) {
                final Grid g = grid.get();
                return g.gettile(tc.sub(g.ul));
            } else {
                return 0;
            }
        }
    }

    public Tile gethitmap(Coord tc) {
        synchronized (grids) {
            final Optional<Grid> g = getgridto(tc);
            return (g.map(grid -> grid.gethitmap(tc.sub(grid.ul))).orElse(null));
        }
    }

    public void sethitmap(Coord tc, Tile t) {
        synchronized (grids) {
            getgridto(tc).ifPresent(g -> {
                g.sethitmap(tc.sub(g.ul), t);
            });
        }
    }

    public int gettile(Coord tc) {
        synchronized (grids) {
            Grid g = getgridt(tc);
            return (g.gettile(tc.sub(g.ul)));
        }
    }

    public double getfz(Coord tc) {
        synchronized (grids) {
            Grid g = getgridt(tc);
            return (g.getz(tc.sub(g.ul)));
        }
    }

    @Deprecated
    public int getz(Coord tc) {
        return ((int) Math.round(getfz(tc)));
    }

    public int getz_safe(Coord tc) {
        synchronized (grids) {
            final Optional<Grid> grid = getgridto(tc);
            if (grid.isPresent()) {
                final Grid g = grid.get();
                return ((int) Math.round(g.getz(tc.sub(g.ul))));
            } else {
                return 0;
            }
        }
    }

    public double getcz_old(double px, double py) {
        double tw = tilesz.x, th = tilesz.y;
        Coord ul = new Coord(Utils.floordiv(px, tw), Utils.floordiv(py, th));
        double sx = Utils.floormod(px, tw) / tw;
        double sy = Utils.floormod(py, th) / th;
        return (((1.0f - sy) * (((1.0f - sx) * getz(ul)) + (sx * getz(ul.add(1, 0))))) +
                (sy * (((1.0f - sx) * getz(ul.add(0, 1))) + (sx * getz(ul.add(1, 1))))));
    }

    public double getcz(double px, double py) {
        double tw = tilesz.x, th = tilesz.y;
        Coord ul = new Coord(Utils.floordiv(px, tw), Utils.floordiv(py, th));
        double sx = Utils.floormod(px, tw) / tw;
        double sy = Utils.floormod(py, th) / th;
        return (((1.0f - sy) * (((1.0f - sx) * getz_safe(ul)) + (sx * getz_safe(ul.add(1, 0))))) +
                (sy * (((1.0f - sx) * getz_safe(ul.add(0, 1))) + (sx * getz_safe(ul.add(1, 1))))));
    }

    public double getcz(Coord2d pc) {
        return (getcz(pc.x, pc.y));
    }

    public double getcz_old(Coord2d pc) {
        return (getcz_old(pc.x, pc.y));
    } //only exists because follow cam hates the new getz

    public float getcz(float px, float py) {
        return ((float) getcz((double) px, (double) py));
    }

    public float getcz(Coord pc) {
        return (getcz(pc.x, pc.y));
    }

    public Coord3f getzp(Coord2d pc) {
        return (new Coord3f((float) pc.x, (float) pc.y, (float) getcz(pc)));
    }

    public final ZSurface zsurf = new ZSurface() {
        public double getz(Coord tc) {
            return (getfz(tc));
        }
    };

    public double getz(SurfaceID id, Coord tc) {
        Grid g = getgridt(tc);
        MapMesh cut = g.getcut(tc.sub(g.ul).div(cutsz));
        Tiler t = tiler(g.gettile(tc.sub(g.ul)));
        return (cut.getsurf(id, t).getz(tc));
    }

    public double getz(SurfaceID id, Coord2d pc) {
        Coord tc = pc.floor(tilesz);
        Grid g = getgridt(tc);
        MapMesh cut = g.getcut(tc.sub(g.ul).div(cutsz));
        Tiler t = tiler(g.gettile(tc.sub(g.ul)));
        ZSurface surf = cut.getsurf(id, t);
        return (surf.getz(pc));
    }

    public Coord3f getzp(SurfaceID id, Coord2d pc) {
        return (Coord3f.of((float) pc.x, (float) pc.y, (float) getz(id, pc)));
    }

    public Coord3f getnorm(SurfaceID id, Coord2d pc) {
        Coord tc = pc.floor(tilesz);
        Grid g = getgridt(tc);
        MapMesh cut = g.getcut(tc.sub(g.ul).div(cutsz));
        Tiler t = tiler(g.gettile(tc.sub(g.ul)));
        return (cut.getsurf(id, t).getnorm(pc));
    }

    public Coord3f getzp_old(Coord2d pc) {
        return (new Coord3f((float) pc.x, (float) pc.y, (float) getcz_old(pc)));
    }//only exists because follow cam hates the new getz


    public Collection<OverlayInfo> getols(Area a) {
        Collection<OverlayInfo> ret = new ArrayList<>();
        for (Coord gc : a.div(cmaps)) {
            try {
                Grid g = getgrid(gc);
                if (g.ols == null)
                    continue;
                for (Indir<Resource> res : g.ols) {
                    OverlayInfo id = res.get().layer(ResOverlay.class);
                    if (!ret.contains(id))
                        ret.add(id);
                }
            } catch (Loading e) {
//                e.printStackTrace();
            }
        }
        for (Overlay lol : ols) {
            try {
                if ((lol.a.overlap(a) != null) && !ret.contains(lol.id))
                    ret.add(lol.id);
            } catch (Exception e) { ///???
                e.printStackTrace();
            }
        }
        return (ret);
    }

    public void getol(OverlayInfo id, Area a, boolean[] buf) {
        Area ga = a.div(cmaps);
        if (ga.area() == 1) {
            Grid g = getgrid(ga.ul);
            g.getol(id, a.xl(g.ul.inv()), buf);
        } else {
            boolean[] gbuf = new boolean[cmaps.x * cmaps.y];
            for (Coord gc : ga) {
                Grid g = getgrid(gc);
                Area gt = Area.sized(g.ul, cmaps);
                g.getol(id, Area.sized(Coord.z, cmaps), gbuf);
                for (Coord tc : a.overlap(gt))
                    buf[a.ri(tc)] = gbuf[(tc.x - gt.ul.x) + ((tc.y - gt.ul.y) * cmaps.x)];
            }
        }
        for (Overlay lol : ols) {
            if (lol.id != id)
                continue;
            Area la = lol.a.overlap(a);
            if (la != null) {
                for (Coord lc : la)
                    buf[a.ri(lc)] = true;
            }
        }
    }

    public boolean getolo(OverlayInfo id, Area a, boolean[] buf) {
        boolean fail = false;
        Area ga = a.div(cmaps);
        if (ga.area() == 1) {
            Optional<Grid> og = getgrido(ga.ul);
            if (og.isPresent()) {
                Grid g = og.get();
                g.getol(id, a.xl(g.ul.inv()), buf);
            } else fail = true;
        } else {
            boolean[] gbuf = new boolean[cmaps.x * cmaps.y];
            for (Coord gc : ga) {
                Optional<Grid> og = getgrido(gc);
                if (og.isPresent()) {
                    Grid g = og.get();
                    Area gt = Area.sized(g.ul, cmaps);
                    g.getol(id, Area.sized(Coord.z, cmaps), gbuf);
                    for (Coord tc : a.overlap(gt))
                        buf[a.ri(tc)] = gbuf[(tc.x - gt.ul.x) + ((tc.y - gt.ul.y) * cmaps.x)];
                } else fail = true;
            }
        }
        for (Overlay lol : ols) {
            if (lol.id != id)
                continue;
            Area la = lol.a.overlap(a);
            if (la != null) {
                for (Coord lc : la)
                    buf[a.ri(lc)] = true;
            }
        }
        return (fail);
    }

    public MapMesh getcut(Coord cc) {
        synchronized (grids) {
            return (getgrid(cc.div(cutn)).getcut(cc.mod(cutn)));
        }
    }

    public Optional<MapMesh> getcuto(Coord cc) {
        synchronized (grids) {
            return (getgrido(cc.div(cutn)).flatMap(g -> g.getcuto(cc.mod(cutn))));
        }
    }

    public FastMesh getgcut(Coord cc) {
        synchronized (grids) {
            return (getgrid(cc.div(cutn)).getgcut(cc.mod(cutn)));
        }
    }

    public Optional<FastMesh> getgcuto(Coord cc) {
        synchronized (grids) {
            return (getgrido(cc.div(cutn)).flatMap(g -> g.getgcuto(cc.mod(cutn))));
        }
    }

    public Collection<Gob> getfo(Coord cc) {
        synchronized (grids) {
            return (getgrid(cc.div(cutn)).getfo(cc.mod(cutn)));
        }
    }

    public Rendered getolcut(OverlayInfo id, Coord cc) {
        synchronized (grids) {
            return (getgrid(cc.div(cutn)).getolcut(id, cc.mod(cutn)));
        }
    }

    public Optional<Rendered> getolcuto(OverlayInfo id, Coord cc) {
        synchronized (grids) {
            return (getgrido(cc.div(cutn)).flatMap(g -> g.getolcuto(id, cc.mod(cutn))));
        }
    }

    public void mapdata2(Message msg) {
        Coord c = msg.coord();
        synchronized (grids) {
            synchronized (req) {
                if (req.containsKey(c)) {
                    Grid g = grids.get(c);
                    if (g == null) {
                        grids.put(c, g = new Grid(c));
                        cached = null;
                    }
                    g.fill(msg);
                    req.remove(c);
                    olseq++;
                    chseq++;
                    gridwait.wnotify();
                }
            }
        }
    }

    public void mapdata(Message msg) {
        long now = System.currentTimeMillis();
        int pktid = msg.int32();
        int off = msg.uint16();
        int len = msg.uint16();
        Defrag fragbuf;
        synchronized (fragbufs) {
            if ((fragbuf = fragbufs.get(pktid)) == null) {
                fragbuf = new Defrag(len);
                fragbufs.put(pktid, fragbuf);
            }
            fragbuf.add(msg.bytes(), off);
            fragbuf.last = now;
            if (fragbuf.done()) {
                mapdata2(fragbuf.msg());
                fragbufs.remove(pktid);
            }

            /* Clean up old buffers */
            for (Iterator<Map.Entry<Integer, Defrag>> i = fragbufs.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<Integer, Defrag> e = i.next();
                Defrag old = e.getValue();
                if (now - old.last > 10000)
                    i.remove();
            }
        }
    }

    public Resource.Spec tilesetn(int i) {
        Resource.Spec[] nsets = this.nsets;
        if (i >= nsets.length)
            return (null);
        return (nsets[i]);
    }

    public Resource tilesetr(int i) {
        Reference<Resource>[] sets = this.sets;
        if (i >= sets.length)
            return (null);
        Resource res = (sets[i] == null) ? null : sets[i].get();
        if (res == null) {
            Resource.Spec[] nsets = this.nsets;
            if (nsets[i] == null)
                return (null);
            sets[i] = new SoftReference<>(res = nsets[i].get());
        }
        return (res);
    }

    public Tileset tileset(int i) {
        Reference<Tileset>[] csets = this.csets;
        if (i >= csets.length)
            return (null);
        Tileset cset = (csets[i] == null) ? null : csets[i].get();
        if (cset == null) {
            Resource res = tilesetr(i);
            if (res == null)
                return (null);
            csets[i] = new SoftReference<>(cset = res.flayer(Tileset.class));
        }
        return (cset);
    }

    public Tiler tiler(int i) {
        Reference<Tiler>[] tiles = this.tiles;
        if (i >= tiles.length)
            return (null);
        Tiler tile = (tiles[i] == null) ? null : tiles[i].get();
        if (tile == null) {
            Tileset set = tileset(i);
            if (set == null)
                return (null);
            tiles[i] = new SoftReference<>(tile = set.tfac().create(i, set));
        }
        return (tile);
    }

    public void trimall() {
        synchronized (grids) {
            synchronized (req) {
                for (Grid g : grids.values())
                    g.dispose();
                grids.clear();
                req.clear();
                cached = null;
            }
            gridwait.wnotify();
        }
    }

    public void trim(Coord ul, Coord lr) {
        if (!DefSettings.KEEPGRIDS.get()) {
            synchronized (grids) {
                synchronized (req) {
                    for (Iterator<Map.Entry<Coord, Grid>> i = grids.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry<Coord, Grid> e = i.next();
                        Coord gc = e.getKey();
                        Grid g = e.getValue();
                        if ((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y)) {
                            g.dispose();
                            i.remove();
                        }
                    }
                    for (Iterator<Coord> i = req.keySet().iterator(); i.hasNext(); ) {
                        Coord gc = i.next();
                        if ((gc.x < ul.x) || (gc.y < ul.y) || (gc.x > lr.x) || (gc.y > lr.y))
                            i.remove();
                    }
                    cached = null;
                }
                gridwait.wnotify();
            }
        }
    }

    public void request(Coord gc) {
        synchronized (req) {
            if (!req.containsKey(gc))
                req.put(Coord.of(gc), new Request());
        }
    }

    public void reqarea(Coord ul, Coord br) {
        ul = ul.div(cutsz);
        br = br.div(cutsz);
        Coord rc = new Coord();
        for (rc.y = ul.y; rc.y <= br.y; rc.y++) {
            for (rc.x = ul.x; rc.x <= br.x; rc.x++) {
                getcuto(Coord.of(rc));
            }
        }
    }

    public void sendreqs() {
        long now = System.currentTimeMillis();
        boolean updated = false;
        synchronized (req) {
            for (Iterator<Map.Entry<Coord, Request>> i = req.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<Coord, Request> e = i.next();
                Coord c = e.getKey();
                Request r = e.getValue();
                if (now - r.lastreq > 1000) {
                    r.lastreq = now;
                    if (++r.reqs >= 5) {
                        i.remove();
                        updated = true;
                    } else {
                        PMessage msg = new PMessage(Session.MSG_MAPREQ);
                        msg.addcoord(c);
                        sess.sendmsg(msg);
                    }
                }
            }
        }
        if (updated) {
            synchronized (grids) {
                gridwait.wnotify();
            }
        }
    }

    public void clearTiles() {
        synchronized (tiles) {
            Arrays.fill(tiles, null);
        }
        synchronized (sets) {
            Arrays.fill(sets, null);
        }
        synchronized (csets) {
            Arrays.fill(csets, null);
        }
    }
}
