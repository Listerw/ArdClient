package haven.sloth.script.pathfinding;

import haven.Config;
import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Indir;
import haven.MessageBuf;
import haven.RenderLink;
import haven.ResDrawable;
import haven.Resource;
import haven.res.lib.obst.Obstacle;
import haven.sloth.gob.Type;
import haven.sloth.util.ResHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Gob Hitbox
 */
public class Hitbox {
    private static final ResHashMap<Hitbox[]> hitboxes = new ResHashMap<>();
    private static final List<Kit> nohitablekits = new ArrayList<>();
    private static final List<String> nohitableids = new ArrayList<>(Arrays.asList("build", "ext"));
    private static final List<String> nocacheends = new ArrayList<>(Arrays.asList("gate", "/pow"));
    private static final Hitbox[] NOHIT = new Hitbox[]{new Hitbox(new Coord(-1, -1), new Coord(1, 1), false, false)};
    private static final int BUFFER_SIZE = 2;

    private static class Kit {
        public final String resname;
        public final String overlay;
        public final String obstid;

        public Kit(String resname, String overlay, String obstid) {
            this.resname = resname;
            this.overlay = overlay;
            this.obstid = obstid;
        }

        @Override
        public int hashCode() {
            int result = resname != null ? resname.hashCode() : 0;
            result = 31 * result + (overlay != null ? overlay.hashCode() : 0);
            result = 31 * result + (obstid != null ? obstid.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Kit)) return (false);
            Kit kit = (Kit) obj;
            return (resname.equals(kit.resname) && overlay.equals(kit.overlay) && obstid.equals(kit.obstid));
        }
    }

    static {
        hitboxes.put("gfx/terobjs/herbs", NOHIT);
        hitboxes.put("gfx/terobjs/items", NOHIT);
        hitboxes.put("gfx/terobjs/plants", NOHIT);

        //misc
//        hitboxes.put("gfx/terobjs/consobj", new Hitbox[]{new Hitbox(new Coord(-4, -4), new Coord(8, 8))});
        hitboxes.put("gfx/terobjs/skeleton", new Hitbox[]{new Hitbox(new Coord(-4, -4), new Coord(8, 8))});
        hitboxes.put("gfx/terobjs/clue", NOHIT);
        hitboxes.put("gfx/terobjs/boostspeed", NOHIT);
        hitboxes.put("gfx/kritter/jellyfish/jellyfish", NOHIT);
        //Knarrs seem to just take the hitbox of a player?
//        hitboxes.put("gfx/terobjs/vehicle/knarr", new Hitbox[]{new Hitbox(new Coord(-4, -4), new Coord(8, 8))});

        //stone This looks wrong...
//        hitboxes.put("gfx/terobjs/bumlings", new Hitbox[]{new Hitbox(new Coord(8, 8), new Coord(-16, -16))});

        //walls
        //XXX: loftar's real hitbox size for this is certainly a decimal..
//        final Hitbox[] wallseg = new Hitbox[]{new Hitbox(new Coord(-5, -5), new Coord(5, 5))};
//        final Hitbox[] gate = new Hitbox[]{new Hitbox(new Coord(-5, -10), new Coord(5, 10))};
//        final Hitbox[] biggate = new Hitbox[]{new Hitbox(new Coord(-5, -16), new Coord(5, 16))};
//        hitboxes.put("gfx/terobjs/arch/brickwallcp", wallseg);
//        hitboxes.put("gfx/terobjs/arch/brickwallseg", wallseg);
//        hitboxes.put("gfx/terobjs/arch/brickwallgate", gate);
//        hitboxes.put("gfx/terobjs/arch/brickwallbiggate", biggate);
//        hitboxes.put("gfx/terobjs/arch/palisadecp", wallseg);
//        hitboxes.put("gfx/terobjs/arch/palisadeseg", wallseg);
//        hitboxes.put("gfx/terobjs/arch/palisadegate", gate);
//        hitboxes.put("gfx/terobjs/arch/palisadebiggate", biggate);
//        hitboxes.put("gfx/terobjs/arch/poleseg", wallseg);
//        hitboxes.put("gfx/terobjs/arch/polecp", wallseg);
//        hitboxes.put("gfx/terobjs/arch/polegate", gate);
//        hitboxes.put("gfx/terobjs/arch/polebiggate", biggate);
//        hitboxes.put("gfx/terobjs/arch/drystonewallseg", wallseg);
//        hitboxes.put("gfx/terobjs/arch/drystonewallcp", wallseg);
//        hitboxes.put("gfx/terobjs/arch/drystonewallgate", gate);
//        hitboxes.put("gfx/terobjs/arch/drystonewallbiggate", biggate);
//        hitboxes.put("gfx/terobjs/arch/hwall", new Hitbox[]{new Hitbox(new Coord(-1, 0), new Coord(1, 11))});

        //animals
        hitboxes.put("gfx/kritter/horse", new Hitbox[]{new Hitbox(new Coord(-8, -4), new Coord(8, 4))});
        hitboxes.put("gfx/kritter/cattle/calf", new Hitbox[]{new Hitbox(new Coord(-9, -3), new Coord(9, 3))});
//        hitboxes.put("gfx/kritter/cattle/cattle", new Hitbox[]{new Hitbox(new Coord(-12, -4), new Coord(12, 4))});
        hitboxes.put("gfx/kritter/pig", new Hitbox[]{new Hitbox(new Coord(-6, -3), new Coord(6, 3))});
        hitboxes.put("gfx/kritter/goat", new Hitbox[]{new Hitbox(new Coord(-3, -2), new Coord(4, 2))});
        hitboxes.put("gfx/kritter/sheep/lamb", new Hitbox[]{new Hitbox(new Coord(-4, -2), new Coord(5, 2))});
        hitboxes.put("gfx/terobjs/boostspeed", new Hitbox[]{new Hitbox(new Coord(-4, -4), new Coord(4, 4))});

//        hitboxes.put("gfx/terobjs/cupboard", new Hitbox[]{new Hitbox(new Coord(-5, -5), new Coord(5, 5))});

        nohitablekits.add(new Kit("gfx/terobjs/vehicle/wagon", "gfx/terobjs/vehicle/encampmentspices", "ext"));

        hitboxes.put("gfx/kritter/reindeer/teimdeerbull", new Hitbox[]{new Hitbox(new Coord(-6, -2), new Coord(12, 2))});
        hitboxes.put("gfx/kritter/reindeer/teimdeercow", new Hitbox[]{new Hitbox(new Coord(-6, -2), new Coord(12, 2))});
    }

    //Offset and Size with a "buffer" around it to avoid clipping
    //After floating point movement changes and "pushing" effect this might not be needed outside of
    //cliffs and cave walls
//    private final Coord off;
//    private final Coord sz;
    public final Coord2d[] points;

    private final boolean hitable;

    public boolean ishitable() {
        return hitable;
    }

    public float zplus = 0;

    public Hitbox(final Coord off, final Coord br, boolean hitable, boolean buffer) {
        Coord ac = !buffer ? off : off.add(BUFFER_SIZE, BUFFER_SIZE);
        Coord bc = !buffer ? br : br.add(BUFFER_SIZE * 2, BUFFER_SIZE * 2);
        this.points = new Coord2d[]{
                new Coord2d(ac.x, -ac.y), new Coord2d(bc.x, -ac.y), new Coord2d(bc.x, -bc.y), new Coord2d(ac.x, -bc.y)
        };
        this.hitable = hitable;
    }

    public Hitbox(final Coord off, final Coord br) {
        this(off, br, true, false);
    }

    public Hitbox() {
        this(Coord.z, Coord.z, false, false);
    }

    public Hitbox(final Coord2d[] p, boolean hitable) {
        this.points = new Coord2d[p.length];
        for (int i = 0; i < p.length; i++) {
            this.points[i] = new Coord2d(p[i].x, -p[i].y);
        }
        this.hitable = hitable;
    }

    public Hitbox(final Coord2d box) {
        double x = box.x / 2.0;
        double y = box.y / 2.0;
        this.points = new Coord2d[]{new Coord2d(x, -y), new Coord2d(-x, -y), new Coord2d(-x, y), new Coord2d(x, y)};
        this.hitable = true;
    }

    public Hitbox(final List<Coord2d> points, boolean hitable) {
        this.points = new Coord2d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            this.points[i] = new Coord2d(points.get(i).x, -points.get(i).y);
        }
        this.hitable = hitable;
    }

    public Coord2d offset() {
        double minx = Double.MAX_VALUE, miny = Double.MAX_VALUE;
        for (Coord2d c2 : points) {
            minx = Math.min(minx, c2.x);
            miny = Math.min(miny, c2.y);
        }
//        return Coord.z.sub(BUFFER_SIZE, BUFFER_SIZE);
//        return new Coord2d(minx, miny).round().add(BUFFER_SIZE, BUFFER_SIZE);
        return new Coord2d(minx, miny);
//        return Coord.z;
//        return new Coord2d(minx, miny).round().sub(BUFFER_SIZE, BUFFER_SIZE);
    }

    public Coord2d size() {
        double maxx = Double.MIN_VALUE, maxy = Double.MIN_VALUE, minx = Double.MAX_VALUE, miny = Double.MAX_VALUE;
        for (Coord2d c2 : points) {
            maxx = Math.max(maxx, c2.x);
            maxy = Math.max(maxy, c2.y);
            minx = Math.min(minx, c2.x);
            miny = Math.min(miny, c2.y);
        }
        return new Coord2d(maxx - minx, maxy - miny);
//        return new Coord2d(maxx - minx, maxy - miny).round()/*.add(BUFFER_SIZE * 2, BUFFER_SIZE * 2)*/;
//        return new Coord2d(maxx, maxy).ceil();
//        return new Coord2d(maxx - minx, maxy - miny).round().add(BUFFER_SIZE, BUFFER_SIZE);
    }

    boolean canHit() {
        return hitable;
    }


    private static Hitbox[] loadHitboxFromRes(Gob gob) {
        final Optional<Resource> ores = gob.res();
        if (ores.isPresent()) {
            Resource res = ores.get();

            if (res.name.endsWith("/consobj")) {
                ResDrawable rd = gob.getattr(ResDrawable.class);
                if (rd != null && rd.sdt.rbuf.length >= 4) {
                    MessageBuf buf = rd.sdt.clone();
                    Obstacle obst = Obstacle.parse(buf);
                    return (Arrays.stream(obst.p).map(p -> new Hitbox(p, true)).toArray(Hitbox[]::new));
                }
            }

            if (res.name.endsWith("/trellis")) {
                Optional<Hitbox[]> hitbox = hitboxes.getc(res.name);
                if (hitbox.isPresent()) {
                    return (hitbox.get());
                }
            } else {
                Optional<Hitbox[]> hitbox = hitboxes.get(res.name);
                if (hitbox.isPresent()) {
                    if (gob.type == Type.WALLSEG && Config.flatwalls) {
                        for (Hitbox h : hitbox.get()) {
                            h.zplus = 10;
                        }
                    }
                    return (hitbox.get());
                }
            }

            try {
                List<Resource.Neg> negs = new ArrayList<>(res.layers(Resource.Neg.class));
                List<Resource.Obstacle> obsts = new ArrayList<>(res.layers(Resource.Obstacle.class));
                for (RenderLink.Res link : res.layers(RenderLink.Res.class)) {
                    RenderLink l = link.l;
                    if (l instanceof RenderLink.MeshMat) {
                        RenderLink.MeshMat mm = (RenderLink.MeshMat) l;
                        addIf(negs, getLayer(Resource.Neg.class, mm.srcres.indir(), mm.mesh));
                        addIf(obsts, getLayer(Resource.Obstacle.class, mm.srcres.indir(), mm.mesh));
                    }
                    if (l instanceof RenderLink.AmbientLink) {
                        RenderLink.AmbientLink al = (RenderLink.AmbientLink) l;
                        addIf(negs, getLayer(Resource.Neg.class, al.res));
                        addIf(obsts, getLayer(Resource.Obstacle.class, al.res));
                    }
                    if (l instanceof RenderLink.Collect) {
                        RenderLink.Collect cl = (RenderLink.Collect) l;
                        addIf(negs, getLayer(Resource.Neg.class, cl.from));
                        addIf(obsts, getLayer(Resource.Obstacle.class, cl.from));
                    }
                    if (l instanceof RenderLink.Parameters) {
                        RenderLink.Parameters pl = (RenderLink.Parameters) l;
                        addIf(negs, getLayer(Resource.Neg.class, pl.res));
                        addIf(obsts, getLayer(Resource.Obstacle.class, pl.res));
                    }
                }

                final List<Hitbox> hitlist = new ArrayList<>();
                for (Resource.Obstacle o : obsts) {
                    for (int i = 0; i < o.ep.length; i++) {
                        boolean hitable = checkHitable(gob, o);
                        Hitbox hb = new Hitbox(o.ep[i], hitable);
                        hitlist.add(hb);
                    }
                }
                for (Resource.Neg o : negs) {
                    boolean hitable = checkHitable(gob);
                    Hitbox hb = new Hitbox(o.bc, o.bs, hitable, false);
                    hitlist.add(hb);
                }
                boolean isAdd = true;
                for (String s : nocacheends)
                    if (res.name.endsWith(s)) {
                        isAdd = false;
                        break;
                    }
                Hitbox[] hbs = hitlist.toArray(new Hitbox[0]);
                if (isAdd)
                    hitboxes.put(res.name, hbs);
                return (hbs);
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    @SafeVarargs
    public static <T extends Resource.Layer> List<T> getLayer(final Class<T> layer, Indir<Resource>... reses) {
        final List<T> list = new ArrayList<>();
        for (Indir<Resource> ires : reses) {
            if (ires != null) {
                Resource res = ires.get();
                if (res != null) {
                    T l = res.layer(layer);
                    if (l != null)
                        list.add(layer.cast(l));
                }
            }
        }
        return (list);
    }

    public static <T extends Resource.Layer> void addIf(final List<T> end, final List<T> start) {
        for (T l : start)
            if (!end.contains(l))
                end.add(l);
    }

    public static boolean checkOverlay(Gob gob, String id) {
        Optional<String> resname = gob.resname();
        if (resname.isPresent())
            for (Kit kit : nohitablekits)
                if (kit.resname.equals(resname.get()))
                    for (Gob.Overlay ol : gob.ols)
                        if (ol.res != null && ol.res.get() != null)
                            if (ol.res.get().name.equals(kit.overlay))
                                if (kit.obstid.equals(id))
                                    return (true);
        return (false);
    }

    public static boolean checkHitable(Gob gob, Resource.Obstacle obst) {
        Optional<Resource> ores = gob.res();
        if (ores.isPresent()) {
            Resource res = ores.get();
            String id = obst.id;
            if (id != null)
                for (String s : nohitableids)
                    if (s.equals(id))
                        return (false);
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null)
                if (res.name.endsWith("gate") && rd.sdt.peekrbuf(0) == 1)
                    return (false);
//            if (checkOverlay(gob, id)) return (false);
        }
        return (true);
    }

    public static boolean checkHitable(Gob gob) {
        Optional<Resource> ores = gob.res();
        if (ores.isPresent()) {
            Resource res = ores.get();
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null)
                if (res.name.endsWith("/pow") && (rd.sdt.peekrbuf(0) == 33 || rd.sdt.peekrbuf(0) == 17))
                    return (false);
            if (res.name.equals("gfx/terobjs/vflag"))
                return (false);
//            if (checkOverlay(gob, id)) return (false);
        }
        return (true);
    }


    public static Hitbox[] hbfor(final String res) {
        return hitboxes.get(res).orElse(null);
    }


    public static Hitbox[] hbfor(final Gob g) {
        return hbfor(g, false);
    }

    public static Hitbox[] hbfor(final Gob g, final boolean force) {
//        final Optional<Resource> res = g.res();
        return loadHitboxFromRes(g);
//        return res.map(resource -> hitboxes.get(resource.name).orElse(loadHitboxFromRes(g))).orElse(null);
//        if (res.isPresent()) {
//            if (!force) {
//                if (!res.get().name.endsWith("gate") && !res.get().name.endsWith("/pow")) {
//                    return hitboxes.get(res.get().name).orElse(loadHitboxFromRes(g));
//                } else if (res.get().name.endsWith("gate") && res.get().name.startsWith("gfx/terobjs/arch")) {
//                    ResDrawable rd = g.getattr(ResDrawable.class);
//                    if (rd != null && (rd.sdtnum() == 1)) {
//                        return NOHIT;
//                    } else {
//                        return hitboxes.get(res.get().name).orElse(loadHitboxFromRes(g));
//                    }
//                } else if (res.get().name.endsWith("/pow")) {
//                    ResDrawable rd = g.getattr(ResDrawable.class);
//                    if (rd != null && (rd.sdtnum() == 17 || rd.sdtnum() == 33)) {
//                        return NOHIT;
//                    } else {
//                        return hitboxes.get(res.get().name).orElse(loadHitboxFromRes(g));
//                    }
//                } else {
//                    return hitboxes.get(res.get().name).orElse(loadHitboxFromRes(g));
//                }
//            } else {
//                return hitboxes.get(res.get().name).orElse(loadHitboxFromRes(g));
//            }
//        } else {
//            return null;
//        }
    }
}
