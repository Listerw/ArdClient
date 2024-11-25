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

import haven.purus.Iconfinder;
import haven.purus.pbot.PBotDiscord;
import haven.purus.pbot.PBotUtils;
import haven.res.ui.obj.buddy.Buddy;
import haven.res.ui.obj.buddy_v.Vilmate;
import haven.resutil.Ridges;
import haven.sloth.gob.Alerted;
import haven.sloth.gob.Type;
import haven.sloth.gui.DowseWnd;
import modification.configuration;
import modification.dev;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static haven.DefSettings.MINIMAPTYPE;
import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class LocalMiniMap extends Widget {
    public final static Tex roadicn = Resource.loadtex("gfx/icons/milestone");
    public final static Tex dooricn = Resource.loadtex("gfx/icons/door");
    private static final Tex resize = Resource.loadtex("gfx/hud/wndmap/lg/resize");
    private static final Tex gridblue = Resource.loadtex("gfx/hud/mmap/gridblue");
    private static final Tex gridred = Resource.loadtex("gfx/hud/mmap/gridred");
    private final static Tex bushicn = Text.renderstroked("\u22C6", Color.CYAN, Color.BLACK, Text.num12boldFnd).tex();
    private final static Tex treeicn = Text.renderstroked("\u25B2", Color.CYAN, Color.BLACK, Text.num12boldFnd).tex();
    private final static Tex bldricn = Text.renderstroked("\u25AA", Color.CYAN, Color.BLACK, Text.num12boldFnd).tex();
    public static Coord plcrel = null;
    private static float[] zArray = {.5f, .75f, 1f, 2f, 3f, 4f};
    private static float[] ziArray = {.8f, .9f, 1f, 1f, 1f, 1f};
    // public Tex biometex;
    public final MapView mv;
    private final HashSet<Long> sgobs = new HashSet<>();
    private final Map<Coord, Tex> maptiles = new LinkedHashMap<Coord, Tex>(100, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Coord, Tex> eldest) {
            if (size() > 100) {
                try {
                    eldest.getValue().dispose();
                } catch (RuntimeException e) {
                }
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            values().forEach(Tex::dispose);
            super.clear();
        }
    };
    private final Map<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>> cache = new LinkedHashMap<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>>(7, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>> eldest) {
            return size() > 7;
        }
    };
    public MapFile save;
    public MapTile cur = null;
    public List<DisplayIcon> icons = Collections.emptyList();
    public GobIcon.Settings iconconf;
    public long lastnewgid;
    private final HashMap<BufferedImage, Color> simple_textures = new HashMap<>();
    private String biome;
    private Coord cc = null;
    private UI.Grab dragging;
    private Coord doff = Coord.z;
    private Coord delta = Coord.z;
    private int zIndex = 2;
    private boolean showGrid = DefSettings.MMSHOWGRID.get();
    private boolean showView = DefSettings.MMSHOWVIEW.get();
    private float zoom = UI.scale(1f); //zoom multiplier
    private float iconZoom = 1f; //zoom multiplier for minimap icons
    private Map<Color, Tex> xmap = new HashMap<>(6);
    private static final Coord[] tecs = {
            new Coord(0, -1),
            new Coord(1, 0),
            new Coord(0, 1),
            new Coord(-1, 0)
    };

    public LocalMiniMap(Coord sz, MapView mv) {
        super(sz);
        this.mv = mv;
    }

    private static String pretty(String name) {
        int k = name.lastIndexOf("/");
        name = name.substring(k + 1);
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return name;
    }

    private static String prettybiome(String biome) {
        int k = biome.lastIndexOf("/");
        biome = biome.substring(k + 1);
        biome = biome.substring(0, 1).toUpperCase() + biome.substring(1);
        return biome;
    }

    public String tileName(int t) {
        Resource r = ui.sess.glob.map.tilesetr(t);
        if (r == null)
            return (null);
        return r.name;
    }

    public boolean isContains(int t, String type) {
        Resource r = ui.sess.glob.map.tilesetr(t);
        if (r == null) return (false);
        return r.name.contains(type);
    }

    private BufferedImage tileimg(int t, BufferedImage[] texes, String res) {
        BufferedImage img = texes[t];
        if (img == null) {
            Resource.Image ir = null;
            TexR tr = null;
            Resource r = ui.sess.glob.map.tilesetr(t);
            if (r != null)
                ir = r.layer(Resource.imgc);
            if (r == null || ir == null)
                r = Resource.remote().loadwait(res);
            if (r != null && ir == null) {
                ir = r.layer(Resource.imgc);
                tr = r.layer(TexR.class);
            }
            if (ir != null)
                img = ir.img;
            else if (tr != null)
                img = tr.tex.fill();
            else
                return (null);
            texes[t] = img;
        }
        return (img);
    }

    private BufferedImage tileimg(int t, BufferedImage[] texes) {
        BufferedImage img = texes[t];
        if (img == null) {
            Resource r = ui.sess.glob.map.tilesetr(t);
            if (r == null)
                return (null);
            Resource.Image ir = r.layer(Resource.imgc);
            if (ir == null)
                return (null);
            img = ir.img;
            texes[t] = img;
        }
        return (img);
    }

    public Tex drawmap(Coord ul, BufferedImage[] texes) {
        Coord sz = cmaps;
        MCache m = ui.sess.glob.map;
        BufferedImage buf = TexI.mkbuf(sz);
        Coord c = new Coord();

        for (c.y = 0; c.y < sz.y; c.y++) {
            for (c.x = 0; c.x < sz.x; c.x++) {
                int t = m.gettile(ul.add(c));
                BufferedImage tex;
                if (configuration.cavetileonmap && isContains(t, "gfx/tiles/rocks/")) {
                    final String tname = tileName(t);
                    final String newtype = "gfx/terobjs/bumlings/" + tname.substring(tname.lastIndexOf("/") + 1);
                    tex = tileimg(t, texes, newtype);
                } else tex = tileimg(t, texes);
                int rgb = 0;
                if (tex != null) {
                    switch (MINIMAPTYPE.get()) {
                        case 1:
                            rgb = tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()), Utils.floormod(c.y + ul.y, tex.getHeight()));

                            int mixrgb = tex.getRGB(20, 45);

                            Color mixtempColor = new Color(mixrgb, true);
                            Color tempColor = new Color(rgb, true);

                            tempColor = Utils.blendcol(tempColor, mixtempColor, configuration.simplelmapintens);
                            rgb = tempColor.getRGB();
                            break;
                        case 2:
                            Color simple_color = simple_tile_img(tex);

                            if (simple_color != null)
                                rgb = simple_color.getRGB();
                            break;
                        default:
                            rgb = tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()), Utils.floormod(c.y + ul.y, tex.getHeight()));
                            break;
                    }
                }
                buf.setRGB(c.x, c.y, rgb);
            }
        }

        if (!Config.disableBlackOutLinesOnMap) {
            for (c.y = 0; c.y < sz.y; c.y++) {
                for (c.x = 0; c.x < sz.x; c.x++) {
                    int t = m.gettile(ul.add(c));
                    if (!(configuration.disablepavingoutlineonmap && isContains(t, "gfx/tiles/paving/"))) {
                        for (Coord ec : tecs) {
                            Coord coord = c.add(ec);
                            if (coord.x < 0 || coord.x > sz.x - 1 || coord.y < 0 || coord.y > sz.y - 1)
                                continue;
                            if (m.gettile(ul.add(coord)) > t) {
                                buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (c.y = 0; c.y < sz.y; c.y++) {
            for (c.x = 0; c.x < sz.x; c.x++) {
                int t = m.gettile(ul.add(c));
                Tiler tl = m.tiler(t);
                if (tl instanceof Ridges.RidgeTile && Ridges.brokenp(m, ul, c)) {
                    buf.setRGB(c.x, c.y, Color.BLACK.getRGB());

                    if (!Config.disableBlackOutLinesOnMap) {
                        for (int y = Math.max(c.y - 1, 0); y <= Math.min(c.y + 1, sz.y - 1); y++) {
                            for (int x = Math.max(c.x - 1, 0); x <= Math.min(c.x + 1, sz.x - 1); x++) {
                                if (x == c.x && y == c.y)
                                    continue;
                                Color cc = new Color(buf.getRGB(x, y));
                                buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, 0.1).getRGB());
                            }
                        }
                    }
                }
            }
        }

        return (new TexI(buf));
    }

    @SuppressWarnings("Duplicates")
    private Color simple_tile_img(BufferedImage img) {
        synchronized (simple_textures) {
            return simple_textures.computeIfAbsent(img, i -> {
                int sumr = 0, sumg = 0, sumb = 0;
                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        int rgb = img.getRGB(x, y);

                        int red = (rgb >> 16) & 0xFF;
                        int green = (rgb >> 8) & 0xFF;
                        int blue = rgb & 0xFF;

                        sumr += red;
                        sumg += green;
                        sumb += blue;
                    }
                }

                int num = img.getWidth() * img.getHeight();
                return new Color(sumr / num, sumg / num, sumb / num);
            });
        }
    }

    public void save(MapFile file) {
        this.save = file;
    }

    public Coord p2c(Coord2d pc) {
        return (pc.floor(tilesz).sub(cc).mul(zoom).add(sz.div(2)));
    }

    public Coord2d c2p(Coord c) {
        return (c.sub(sz.div(2)).div(zoom).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    private Tex qtex = Window.cf.render("?").tex();

    public void drawicons(GOut g) {
        OCache oc = ui.sess.glob.oc;
        List<Gob> dangergobs = new ArrayList<>();
        Gob player = mv.player();
        for (Gob gob : oc.getallgobs()) {
            try {
                Resource res = gob.getres();
                if (res == null)
                    continue;

                if (gob.type == Type.HUMAN && player != null && gob.id != player.id) {
                    dangergobs.add(gob);
                    continue;
                }
                /*GobIcon icon = gob.getattr(GobIcon.class);
                if (!Config.hideallicons && (icon != null || Config.additonalicons.containsKey(res.name))) {
                    CheckListboxItem itm = Config.icons.get(res.basename());
                    if (itm != null && !itm.selected) {
                        Tex tex;
                        if (icon != null)
                            tex = gob.isDead() ? icon.texgrey() : icon.tex();
                        else
                            tex = Config.additonalicons.get(res.name).get();
                        g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(2)).add(delta), tex.dim.mul(iconZoom));
                    }
                }*/
                String basename = res.basename();
                if (gob.type == Type.BOULDER) {
                    CheckListboxItem itm = Config.boulders.get(basename.substring(0, basename.length() - 1));
                    if (itm != null && itm.selected) {
                        if (Iconfinder.icons.containsKey(basename.substring(0, basename.length() - 1))) {
                            try {
                                Tex tex = Resource.remote().loadwait(Iconfinder.icons.get(basename.substring(0, basename.length() - 1))).layer(Resource.imgc).tex();
                                g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(4)).add(delta), tex.dim.mul(iconZoom / 2));
                            } catch (Resource.Loading l) {
                            }
                        } else {
                            g.image(bldricn, p2c(gob.rc).add(delta).sub(bldricn.sz().div(2)));
                        }
                    }
                } else if (gob.type == Type.BUSH) {
                    CheckListboxItem itm = Config.bushes.get(basename);
                    if (itm != null && itm.selected) {
                        if (Iconfinder.icons.containsKey(basename)) {
                            try {
                                Tex tex = Resource.remote().loadwait(Iconfinder.icons.get(basename)).layer(Resource.imgc).tex();
                                g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(4)).add(delta), tex.dim.mul(iconZoom / 2));
                            } catch (Resource.Loading l) {

                            }
                        } else {
                            g.image(bushicn, p2c(gob.rc).add(delta).sub(bldricn.sz().div(2)));
                        }
                    }
                } else if (gob.type == Type.TREE) {
                    CheckListboxItem itm = Config.trees.get(basename);
                    if (itm != null && itm.selected) {
                        if (Iconfinder.icons.containsKey(basename)) {
                            try {
                                Tex tex = Resource.remote().loadwait(Iconfinder.icons.get(basename)).layer(Resource.imgc).tex();
                                g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(4)).add(delta), tex.dim.mul(iconZoom / 2));
                            } catch (Resource.Loading l) {

                            }
                        } else {
                            g.image(treeicn, p2c(gob.rc).add(delta).sub(bldricn.sz().div(2)));
                        }
                    }
                } else if (gob.type == Type.ROAD && Config.showroadmidpoint) {
                    g.image(roadicn, p2c(gob.rc).sub(roadicn.sz().div(2)).add(delta));
                } else if (gob.type == Type.ROADENDPOINT && Config.showroadendpoint) {
                    g.image(roadicn, p2c(gob.rc).sub(roadicn.sz().div(2)).add(delta));
                } else if (gob.type == Type.DUNGEONDOOR) {
                    int stage = 0;
                    if (gob.getattr(ResDrawable.class) != null)
                        stage = gob.getattr(ResDrawable.class).sdt.peekrbuf(0);
                    if (stage == 10 || stage == 14)
                        g.image(dooricn, p2c(gob.rc).sub(dooricn.sz().div(2)).add(delta));
                } else if (configuration.showUniconedItemsIcon && gob.getattr(GobIcon.class) == null && (gob.name().contains("items") || !gob.name().contains("terobjs") && !gob.name().contains("tiles/paving"))) {
                    double d = Utils.rtime() % 2;
                    if (d > 1)
                        d = 2 - d;
                    g.chcolor(128 + (int) (d * (255 - 128)), (int) (d * 255), (int) (d * 255), 255);
                    g.image(qtex, p2c(gob.rc).sub(qtex.sz().div(2)).add(delta));
                    g.chcolor();
                }

                if (sgobs.contains(gob.id))
                    continue;
            } catch (Loading l) {
            }
        }

        for (Gob gob : dangergobs) {
            try {
                if (gob.type == Type.HUMAN && gob.id != mv.player().id) {
                    if (ui.sess.glob.party.memb.containsKey(gob.id))
                        continue;

                    Coord pc = p2c(gob.rc).add(delta);

                    Buddy buddy = gob.getattr(Buddy.class);
                    if (pc.x >= 0 && pc.x <= sz.x && pc.y >= 0 && pc.y < sz.y) {
//                            g.chcolor(Color.BLACK);
//                            g.fcircle(pc.x, pc.y, 5, 16);
//                            g.chcolor(buddy != null ? BuddyWnd.gc[buddy.group] : Color.WHITE);
//                            g.fcircle(pc.x, pc.y, 4, 16);
//                            g.chcolor();

                        double angle = gob.geta();
//                            final Coord front = new Coord(5, 0).rotate(angle).add(pc);
//                            final Coord right = new Coord(-3, 3).rotate(angle).add(pc);
//                            final Coord left = new Coord(-3, -3).rotate(angle).add(pc);
//                            final Coord notch = new Coord(0, 0).rotate(angle).add(pc);

                        final Coord coord1 = UI.scale(8, 0).rotate(angle).add(pc);
                        final Coord coord2 = UI.scale(0, -5).rotate(angle).add(pc);
                        final Coord coord3 = UI.scale(0, -1).rotate(angle).add(pc);
                        final Coord coord4 = UI.scale(-8, -1).rotate(angle).add(pc);
                        final Coord coord5 = UI.scale(-8, 1).rotate(angle).add(pc);
                        final Coord coord6 = UI.scale(0, 1).rotate(angle).add(pc);
                        final Coord coord7 = UI.scale(0, 5).rotate(angle).add(pc);
                        g.chcolor(buddy != null ? BuddyWnd.gc[buddy.group()] : Color.WHITE);
                        g.poly(coord1, coord2, coord3, coord4, coord5, coord6, coord7);
                        g.chcolor(Color.BLACK);
                        g.polyline(1, coord1, coord2, coord3, coord4, coord5, coord6, coord7);
                        g.chcolor();
                    }

                    if (sgobs.contains(gob.id))
                        continue;

//                        boolean enemy = false;
//                        if (!Config.alarmunknownplayer.equals("None") && buddy == null) {
//                            sgobs.add(gob.id);
//                            Audio.play(Resource.local().loadwait(Config.alarmunknownplayer), Config.alarmunknownvol);
//                            if (Config.discordplayeralert) {
//                                if (Config.discorduser) {
//                                    PBotDiscord.mapAlert(Config.discordalertstring, "Player");
//                                } else if (Config.discordrole) {
//                                    PBotDiscord.mapAlertRole(Config.discordalertstring, "Player");
//                                } else {
//                                    PBotDiscord.mapAlertEveryone("Player");
//                                }
//                            }
//                            enemy = true;
//                        } else if (!Config.alarmredplayer.equals("None") && buddy != null && buddy.group == 2) {
//                            sgobs.add(gob.id);
//                            Audio.play(Resource.local().loadwait(Config.alarmredplayer), Config.alarmredvol);
//                            if (Config.discorduser) {
//                                PBotDiscord.mapAlert(Config.discordalertstring, "Player");
//                            } else if (Config.discordrole) {
//                                PBotDiscord.mapAlertRole(Config.discordalertstring, "Player");
//                            } else {
//                                PBotDiscord.mapAlertEveryone("Player");
//                            }
//                            enemy = true;
//                        }
//
//                        if (Config.autologout && enemy) {
//                            PBotUtils.sysMsg(ui, "Ememy spotted! Logging out!", Color.white);
//                            ui.gui.act("lo");
//                        } else if (Config.autohearth && enemy)
//                            ui.gui.act("travel", "hearth");

                }/* else {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    if (icon != null) {
                        Tex tex;
                        if (icon != null)
                            tex = gob.isDead() ? icon.texgrey() : icon.tex();
                        else
                            tex = Config.additonalicons.get(gob.getres().name).get();
                        g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(2)).add(delta), tex.dim.mul(iconZoom));
                    }
                }*/
            } catch (Exception e) { // fail silently
            }
        }
    }

    public Gob findicongob(Coord c) {
        OCache oc = ui.sess.glob.oc;
        for (Gob gob : oc.getallgobs()) {
            try {
                GobIcon icon = gob.getattr(GobIcon.class);
                if (icon != null) {
                    CheckListboxItem itm = Config.icons.get(gob.getres().basename());
                    if ((itm != null && !itm.selected) || icons.stream().anyMatch(i -> i.gob.equals(gob))) {
                        Coord gc = p2c(gob.rc).add(delta);
                        Coord sz = icon.tex().sz();
                        if (c.isect(gc.sub(sz.div(2)), sz)) {
                            Resource res = icon.res.get();
                            itm = Config.icons.get(res.basename());
                            if (itm == null || !itm.selected)
                                return gob;
                        }
                    }
                } else { // custom icons
                    Coord gc = p2c(gob.rc).add(delta);
                    Coord sz = UI.scale(18, 18);
                    if (c.isect(gc.sub(sz.div(2)), sz)) {
                        Resource res = gob.getres();
                        Buddy buddy = gob.getattr(Buddy.class);
                        if (buddy != null) {
                            if (gob.type == Type.HUMAN)
                                return gob;
                        } else if (gob.type == Type.TREE) {
                            CheckListboxItem itm = Config.trees.get(res.basename());
                            if (itm != null && itm.selected)
                                return gob;
                        } else if (gob.type == Type.BUSH) {
                            CheckListboxItem itm = Config.bushes.get(res.basename());
                            if (itm != null && itm.selected)
                                return gob;
                        } else if (gob.type == Type.BOULDER) {
                            CheckListboxItem itm = Config.boulders.get(res.basename().substring(0, res.basename().length() - 1));
                            if (itm != null && itm.selected)
                                return gob;
                        }/* else if (res != null && Config.additonalicons.containsKey(res.name)) {
                            CheckListboxItem itm = Config.icons.get(res.basename());
                            if (itm == null || !itm.selected) {
                                return gob;
                            }
                        }*/ else if (configuration.showUniconedItemsIcon && gob.getattr(GobIcon.class) == null && (gob.name().contains("items") || !gob.name().contains("terobjs") && !gob.name().contains("tiles/paving"))) {
                            return gob;
                        }
                    }
                }

            } catch (Loading | NullPointerException l) {
            }
        }
        return (null);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        Gob gob = findicongob(c);
        if (gob != null) {
            Resource res = gob.getres();
            try {
                GobIcon icon = gob.getattr(GobIcon.class);
                Buddy buddy = gob.getattr(Buddy.class);
                if (buddy != null)
                    return buddy.name();
                else if (icon != null)
                    return pretty(gob.getres().name);
                else { // custom icons
                    /*if (res != null && Config.additonalicons.containsKey(res.name)) {
                        CheckListboxItem itm = Config.icons.get(res.basename());
                        return pretty(itm.name);
                    } else */
                    if (gob.type == Type.BOULDER)
                        return pretty(res.basename().substring(0, res.basename().length() - 1));
                    else
                        return pretty(gob.getres().basename());
                }
            } catch (Loading | NullPointerException l) {
            }
        }
        return super.tooltip(c, prev);
    }

    static {
//        if (!Config.alarmunknownplayer.equals("None"))
//            Resource.local().loadwait(Config.alarmunknownplayer);
//        if (!Config.alarmredplayer.equals("None"))
//            Resource.local().loadwait(Config.alarmredplayer);
    }

    public void tick(double dt) {
        OCache oc = ui.sess.glob.oc;
        Gob pl = oc.getgob(mv.plgob);
        if (pl == null)
            this.cc = mv.cc.floor(tilesz);
        else
            this.cc = pl.rc.floor(tilesz);

        Coord mc = rootxlate(ui.mc);
        if (mc.isect(Coord.z, sz)) {
            setBiome(c2p(mc).div(tilesz).floor());
        } else {
            setBiome(cc);
        }

        if (Config.playerposfile != null && MapGridSave.gul != null) {
            try {
                // instead of synchronizing MapGridSave.gul we just handle NPE
                //   plcrel = pl.rc.sub((MapGridSave.gul.x + 50) * tilesz.x, (MapGridSave.gul.y + 50) * tilesz.y);
            } catch (NullPointerException npe) {
            }
        }
        icons = findicons(icons);
        icons.sort(Comparator.comparingInt(a -> a.priority));

        List<Gob> dangergobs = new ArrayList<>();
        Gob player = mv.player();
        for (Gob gob : oc.getallgobs()) {
            try {
                Resource res = gob.getres();
                if (res == null)
                    continue;
                if (gob.type == Type.HUMAN && player != null && gob.id != player.id) {
                    dangergobs.add(gob);
                    continue;
                }
                if (sgobs.contains(gob.id))
                    continue;
            } catch (Loading l) {
            }
        }
        for (Gob gob : dangergobs) {
            if (gob.type == Type.HUMAN && player != null && gob.id != player.id) {
                if (ui.sess.glob.party.memb.containsKey(gob.id))
                    continue;

                if (sgobs.contains(gob.id))
                    continue;

                sgobs.add(gob.id);
                new Thread(() -> {
                    try {
                        Thread.sleep(250);
                        Buddy buddy = gob.getattr(Buddy.class);
                        Vilmate vbuddy = gob.getattr(Vilmate.class);
                        boolean enemy = false;
                        String item = "None";
                        if (!Utils.eq(item = Config.alarmsfxlist.get("alarmunknown"), "None") && buddy == null && vbuddy == null) {
                            Double vol = Config.alarmvollist.get("alarmunknown");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                            if (Config.discordplayeralert) {
                                if (!configuration.endpoint.isEmpty() && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                                    if (!ui.gui.chrid.isEmpty()) {
                                        String username = ui.sess.username + "/" + ui.gui.chrid;
                                        if (Config.discorduser) {
                                            PBotDiscord.mapAlert(username, Config.discordalertstring, "Player");
                                        } else if (Config.discordrole) {
                                            PBotDiscord.mapAlertRole(username, Config.discordalertstring, "Player");
                                        } else {
                                            PBotDiscord.mapAlertEveryone(username, "Player");
                                        }
                                    }
                                }
                            }
                            enemy = true;
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmwhite"), "None") && buddy != null && buddy.group() == 0) {
                            Double vol = Config.alarmvollist.get("alarmwhite");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmgreen"), "None") && buddy != null && buddy.group() == 1) {
                            Double vol = Config.alarmvollist.get("alarmgreen");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmblue"), "None") && buddy != null && buddy.group() == 3) {
                            Double vol = Config.alarmvollist.get("alarmblue");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmcyan"), "None") && buddy != null && buddy.group() == 4) {
                            Double vol = Config.alarmvollist.get("alarmcyan");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmyellow"), "None") && buddy != null && buddy.group() == 5) {
                            Double vol = Config.alarmvollist.get("alarmyellow");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmpink"), "None") && buddy != null && buddy.group() == 6) {
                            Double vol = Config.alarmvollist.get("alarmpink");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmpurple"), "None") && buddy != null && buddy.group() == 7) {
                            Double vol = Config.alarmvollist.get("alarmpurple");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                        } else if (!Utils.eq(item = Config.alarmsfxlist.get("alarmred"), "None") && buddy != null && buddy.group() == 2) {
                            Double vol = Config.alarmvollist.get("alarmred");
                            if (Alerted.customsort.get(item)) Audio.play(item, vol);
                            else Audio.play(Resource.local().load(item), vol);
                            if (Config.discordplayeralert) {
                                if (!configuration.endpoint.isEmpty() && ui.sess != null && ui.sess.alive() && ui.sess.username != null && ui.gui != null) {
                                    if (!ui.gui.chrid.isEmpty()) {
                                        String username = ui.sess.username + "/" + ui.gui.chrid;
                                        if (Config.discorduser) {
                                            PBotDiscord.mapAlert(username, Config.discordalertstring, "Player");
                                        } else if (Config.discordrole) {
                                            PBotDiscord.mapAlertRole(username, Config.discordalertstring, "Player");
                                        } else {
                                            PBotDiscord.mapAlertEveryone(username, "Player");
                                        }
                                    }
                                }
                            }
                            enemy = true;
                        }

                        if (Config.autologout && enemy) {
                            PBotUtils.sysMsg(ui, "Ememy spotted! Logging out!", Color.white);
                            ui.gui.act("lo");
                        } else if (Config.autohearth && enemy)
                            ui.gui.act("travel", "hearth");
                    } catch (Exception e) {
                        dev.simpleLog(e);
                    }
                }).start();
            }
        }
    }

    private void setBiome(Coord c) {
        try {
            if (c.sub(delta).div(cmaps).manhattan2(cc.div(cmaps)) > 1) {
                return;
            }
            int t = mv.ui.sess.glob.map.gettile(c.sub(delta));
            Resource r = ui.sess.glob.map.tilesetr(t);
            String newbiome;
            if (r != null) {
                newbiome = (r.name);
            } else {
                newbiome = "Void";
            }
            if (!newbiome.equals(biome)) {
                biome = newbiome;
                MinimapWnd.biometex = Text.renderstroked(prettybiome(biome)).tex();
            }
        } catch (Loading ignored) {
        }
    }

    public void draw(GOut g) {
        if (cc == null)
            return;
        map:
        {
            final MCache.Grid plg;
            try {
                plg = ui.sess.glob.map.getgrid(cc.div(cmaps));
            } catch (Loading l) {
                break map;
            }
            final int seq = plg.seq;

            if (cur == null || plg != cur.grid || seq != cur.seq) {
                Defer.Future<MapTile> f;
                synchronized (cache) {
                    f = cache.get(new Pair<>(plg, seq));
                    if (f == null) {
                        if (cur != null && plg != cur.grid) {
                            int x = Math.abs(plg.gc.x);
                            int y = Math.abs(plg.gc.y);
                            if ((x == 0 && y == 0 || x == 10 && y == 10) && lastnewgid != plg.id) {
                                maptiles.clear();
                                lastnewgid = plg.id;
                            }
                        }
                        f = Defer.later(() -> {
                            Coord ul = plg.ul;
                            Coord gc = plg.gc;
                            BufferedImage[] texes = new BufferedImage[ui.sess.glob.map.tiles.length];
                            Coord c = new Coord();
                            for (c.y = -1; c.y <= 1; c.y++) {
                                for (c.x = -1; c.x <= 1; c.x++) {
                                    maptiles.put(gc.add(c), drawmap(ul.add(c.mul(cmaps)), texes));
                                }
                            }
//                            maptiles.put(gc.add(-1, -1), drawmap(ul.add(-100, -100), texes));
//                            maptiles.put(gc.add(0, -1), drawmap(ul.add(0, -100), texes));
//                            maptiles.put(gc.add(1, -1), drawmap(ul.add(100, -100), texes));
//                            maptiles.put(gc.add(-1, 0), drawmap(ul.add(-100, 0), texes));
//                            maptiles.put(gc, drawmap(ul, texes));
//                            maptiles.put(gc.add(1, 0), drawmap(ul.add(100, 0), texes));
//                            maptiles.put(gc.add(-1, 1), drawmap(ul.add(-100, 100), texes));
//                            maptiles.put(gc.add(0, 1), drawmap(ul.add(0, 100), texes));
//                            maptiles.put(gc.add(1, 1), drawmap(ul.add(100, 100), texes));
                            return new MapTile(plg, seq);
                        });
                        cache.put(new Pair<>(plg, seq), f);
                    }
                }
                if (f.done()) {
                    MCache map = ui.sess.glob.map;
                    try {
                        cur = f.get();
                        MapFile save = this.save;
                        if (save != null) save.update(map, cur.grid.gc);
                    } catch (Loading l) {
                        map.sendreqs();
                    }
                }
            }
        }
        if (cur != null) {
            int tileSize = (int) (100 * zoom);
            Coord ts = new Coord(tileSize, tileSize);
            Coord half = sz.div(2);
            Coord t = new Coord2d(half).div(ts).add(2, 2).ceil();
            Coord po = cur.grid.gc.mul(cmaps).sub(cc).mul(zoom).add(half).add(delta);
//            Coord to = po.div(cmaps);

            if (maptiles.size() >= 9) {
                Coord c = new Coord();
                for (c.x = -t.x; c.x < t.x; c.x++) {
                    for (c.y = -t.y; c.y < t.y; c.y++) {
                        Tex mt = maptiles.get(cur.grid.gc.add(c)/*.sub(to)*/);
                        if (mt != null) {
                            Coord mtc = c./*sub(to).*/mul(ts).add(po);
                            if (mtc.x + ts.x < 0 || mtc.x > sz.x || mtc.y + ts.y < 0 || mtc.y > sz.y)
                                continue;
                            g.image(mt, mtc, ts);
                            if (Config.mapshowgrid) {
                                g.chcolor(Color.RED);
                                g.dottedline(mtc, mtc.add(ts.x, 0), 1);
                                g.dottedline(mtc, mtc.add(0, ts.y), 1);
                                g.chcolor();
                            }
                        }
                    }
                }
            }

            g.image(resize, sz.sub(resize.sz()));

            if (Config.mapshowviewdist)
                drawview(g);
        }
        drawnewicons(g);
        drawicons(g);

        try {
            drawmovement(g);
            drawTracking(g);
        } catch (Exception e) {
        }

        synchronized (ui.sess.glob.party.memb) {
            Collection<Party.Member> members = ui.sess.glob.party.memb.values();
            for (Party.Member m : members) {
                Coord2d ppc;
                Coord ptc;
                double angle;
                try {
                    ppc = m.getc();
                    if (ppc == null) // chars are located in different worlds
                        continue;

                    ptc = p2c(ppc).add(delta);
                    Gob gob = m.getgob();
                    // draw 'x' if gob is outside of view range
                    if (gob == null) {
                        Tex tex = xmap.get(m.col);
                        if (tex == null) {
                            tex = Text.renderstroked("\u2716", m.col, Color.BLACK, Text.num12boldFnd).tex();
                            xmap.put(m.col, tex);
                        }
                        g.image(tex, ptc.sub(UI.scale(6, 6)));
                        continue;
                    }

                    angle = gob.geta();
                } catch (Loading e) {
                    continue;
                }

//                final Coord front = new Coord(5, 0).rotate(angle).add(ptc);
//                final Coord right = new Coord(-3, 3).rotate(angle).add(ptc);
//                final Coord left = new Coord(-3, -3).rotate(angle).add(ptc);
//                final Coord notch = new Coord(0, 0).rotate(angle).add(ptc);

                final Coord coord1 = UI.scale(8, 0).rotate(angle).add(ptc);
                final Coord coord2 = UI.scale(0, -5).rotate(angle).add(ptc);
                final Coord coord3 = UI.scale(0, -1).rotate(angle).add(ptc);
                final Coord coord4 = UI.scale(-8, -1).rotate(angle).add(ptc);
                final Coord coord5 = UI.scale(-8, 1).rotate(angle).add(ptc);
                final Coord coord6 = UI.scale(0, 1).rotate(angle).add(ptc);
                final Coord coord7 = UI.scale(0, 5).rotate(angle).add(ptc);
                g.chcolor(m.col);
                g.poly(coord1, coord2, coord3, coord4, coord5, coord6, coord7);
                g.chcolor(Color.BLACK);
                g.polyline(1, coord1, coord2, coord3, coord4, coord5, coord6, coord7);
                g.chcolor();
            }
        }
        if (MinimapWnd.biometex != null)
            g.image(MinimapWnd.biometex, Coord.z);
        //Improve minimap player markers slightly commit skip
    }

    public void drawview(GOut g) {
        configuration.classMaker(() -> {
            Coord2d sgridsz = new Coord2d(MCache.cmaps);
            Gob player = ui.gui.map.player();
            if (player != null) {
                Coord rc = p2c(player.rc.floor(sgridsz).mul(sgridsz).sub(sgridsz.mul(4))).add(delta);
                g.chcolor(new Color(configuration.distanceviewcolor, true));
                Coord rect = MCache.cmaps.mul(9).div(tilesz.floor()).mul(zoom);
                g.dottedline(rc, rc.add(rect.x - 1, 0), 1);
                g.dottedline(rc.add(rect.x - 1, 0), rc.add(rect), 1);
                g.dottedline(rc.add(rect).sub(1, 1), rc.add(0, rect.y - 1), 1);
                g.dottedline(rc.add(0, rect.y - 1), rc, 1);
                g.chcolor();
            }
        });
    }

    private void drawTracking(GOut g) {
        final double dist = 90000.0D;
        for (final DowseWnd wnd : new ArrayList<>(ui.gui.dowsewnds)) {
            final Coord mc = p2c(wnd.startc).add(delta);
            final Coord lc = mc.add((int) (Math.cos(Math.toRadians(wnd.a1())) * dist), (int) (Math.sin(Math.toRadians(wnd.a1())) * dist));
            final Coord rc = mc.add((int) (Math.cos(Math.toRadians(wnd.a2())) * dist), (int) (Math.sin(Math.toRadians(wnd.a2())) * dist));
            g.chcolor(new Color(configuration.dowsecolor, true));
            g.dottedline(mc, lc, 1);
            g.dottedline(mc, rc, 1);
            g.chcolor();
        }
    }

    private void drawmovement(GOut g) {
        final Coord pc = p2c(mv.player().rc).add(delta);
        final Coord2d movingto = mv.movingto();
        final Iterator<Coord2d> queue = mv.movequeue();
        Coord last;
        if (movingto != null) {
            //Make the line first
            g.chcolor(new Color(configuration.pfcolor, true));
            last = p2c(movingto).add(delta);
            g.dottedline(pc, last, 2);
            if (queue.hasNext()) {
                while (queue.hasNext()) {
                    final Coord next = p2c(queue.next()).add(delta);
                    g.dottedline(last, next, 2);
                    last = next;
                }
            }
        }
    }

    public void center() {
        delta = Coord.z;
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        DefSettings.MMSHOWGRID.set(showGrid);
    }

    public void toggleView() {
        showView = !showView;
        DefSettings.MMSHOWVIEW.set(showView);
    }

    public boolean mousedown(Coord c, int button) {
        int dragBind = Config.trollexmap ? 1 : 2;
        int clickBind = Config.trollexmap ? 3 : 1;

        if (button != dragBind) {
            if (cc == null)
                return false;
            Coord csd = c.sub(delta);
            Coord2d mc = c2p(csd);
            Gob gob = findicongob(csd.add(delta));
            int mod = ui.modflags();
            if (gob == null) { //click tile
                if (ui.modmeta && button == clickBind) {
                    mv.queuemove(c2p(c.sub(delta)));
                } else if (button == clickBind) {
                    mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), 1, mod);
                    mv.pllastcc = mod == 0 ? mc : null;
                    mv.clearmovequeue();
                }
                return true;
            } else {
                if (ui.modflags() == UI.MOD_META) {
                    if (ui.gui != null && ui.gui.map != null)
                        ui.gui.map.showSpecialMenu(gob);
                } else {
                    mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), button, mod, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                    if (button == 1) {
                        mv.pllastcc = mod == 0 ? mc : null;
                    } else {
                        mv.pllastcc = gob.rc;
                    }
                    if (gob.getres() != null) {
                        CheckListboxItem itm = Config.autoclusters.get(gob.getres().name);
                        if (itm != null && itm.selected)
                            mv.startMusselsPicker(gob);
                    }
                }
            }
        } else if (button == dragBind) {
            doff = c;
            dragging = ui.grabmouse(this);
        }

        /*if(Config.trollexmap){
            if (button != 1) {
                if (cc == null)
                    return false;
                Coord csd = c.sub(delta);
                Coord2d mc = c2p(csd);
                if (button == 3)
                    MapView.pllastcc = mc;
                Gob gob = findicongob(csd.add(delta));
                if (gob == null) { //click tile
                    if(ui.modmeta && button == 3) {
                        mv.queuemove(c2p(c.sub(delta)));
                    } else if (button == 3) {
                        mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), 1, ui.modflags());
                        mv.clearmovequeue();
                    }
                    return true;
                } else {
                    mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), button, ui.modflags(), 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                    if (Config.autopickmussels && gob.getres() != null && (gob.getres().basename().contains("mussel") || gob.getres().basename().contains("oyster")))
                        mv.startMusselsPicker(gob);
                    if(Config.autopickclay && gob.getres() != null && gob.getres().basename().contains("clay-gray"))
                        mv.startMusselsPicker(gob);
                    if(Config.autopickbarnacles && gob.getres() != null && gob.getres().basename().contains("goosebarnacle"))
                        mv.startMusselsPicker(gob);
                    if(Config.autopickcattails && gob.getres() != null && gob.getres().basename().contains("cattail"))
                        mv.startMusselsPicker(gob);
                }
            } else if (button == 1) {
                doff = c;
                dragging = ui.grabmouse(this);
            }
        } else {
            if (button != 2) {
                if (cc == null)
                    return false;
                Coord csd = c.sub(delta);
                Coord2d mc = c2p(csd);
                if (button == 1)
                    MapView.pllastcc = mc;
                Gob gob = findicongob(csd.add(delta));
                if (gob == null) { //click tile
                    if(ui.modmeta && button == 1) {
                        mv.queuemove(c2p(c.sub(delta)));
                    } else if (button == 1) {
                        mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), button, ui.modflags());
                        mv.clearmovequeue();
                    }
                    return true;
                } else {
                    mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), button, ui.modflags(), 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                    if (Config.autopickmussels && gob.getres() != null && (gob.getres().basename().contains("mussel") || gob.getres().basename().contains("oyster")))
                        mv.startMusselsPicker(gob);
                    if(Config.autopickclay && gob.getres() != null && gob.getres().basename().contains("clay-gray"))
                        mv.startMusselsPicker(gob);
                    if(Config.autopickbarnacles && gob.getres() != null && gob.getres().basename().contains("goosebarnacle"))
                        mv.startMusselsPicker(gob);
                    if(Config.autopickcattails && gob.getres() != null && gob.getres().basename().contains("cattail"))
                        mv.startMusselsPicker(gob);
                }
            } else if (button == 2) {
                doff = c;
                dragging = ui.grabmouse(this);
            }
        }*/
        return true;
    }

    public void mousemove(Coord c) {
        if (dragging != null) {
            delta = delta.add(c.sub(doff));
            doff = c;
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (dragging != null) {
            dragging.remove();
            dragging = null;
        }
        return (true);
    }

    public boolean mousewheel(Coord c, int amount) {
        /*if (!Config.mapscale) {
            if (amount > 0 && zoom > 1)
                zoom = Math.round(zoom * 100 - 20) / 100f;
            else if (amount < 0 && zoom < 3)
                zoom = Math.round(zoom * 100 + 20) / 100f;

            iconZoom = Math.round((zoom - 1) * 100 / 2) / 100f + 1;

        } else {
            if (amount == 0) {
                return false;
            } else {
                zIndex = Math.max(0, Math.min(zIndex - amount, zArray.length - 1));
            }
            zoom = zArray[zIndex];
            iconZoom = ziArray[zIndex];
        }*/
        if (amount == 0)
            return (false);
        zIndex = Math.max(0, Math.min(zIndex - amount, zArray.length - 1));
        zoom = zArray[zIndex];
        iconZoom = ziArray[zIndex];
        return (true);
    }

    public void drawnewicons(GOut g) {
        for (DisplayIcon disp : icons) {
            if (disp.sc == null)
                continue;
            GobIcon.Image img = disp.img;
            if (disp.col != null)
                g.chcolor(disp.col);
            else
                g.chcolor();

            TexI tex = disp.gob.isDead() ? img.texgrey() : img.tex();
            if (!img.rot)
                g.image(tex, disp.sc.sub(img.cc.mul(iconZoom)).add(delta), tex.dim.mul(iconZoom));
            else {
                Tex bi = new TexI(configuration.rotate(tex.back, (disp.ang + Math.PI / 2) % (2 * Math.PI)));
//                AffineTransform transform = new AffineTransform();
//                double angle = (disp.ang + Math.PI / 2) % (2 * Math.PI);
//                transform.rotate(angle, bi.getWidth() / 2f, bi.getHeight() / 2f);
//                AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
//                bi = op.filter(bi, null);
                g.image(bi, disp.sc.sub(img.cc.mul(iconZoom)).add(delta), bi.sz().mul(iconZoom));
            }
        }
        g.chcolor();
    }

    public List<DisplayIcon> findicons(Collection<? extends DisplayIcon> prev) {
        if ((ui.sess == null) /*|| (sessloc == null) || (dloc.seg != sessloc.seg) */ || (iconconf == null))
            return (Collections.emptyList());
        Map<Gob, DisplayIcon> pmap = Collections.emptyMap();
        if (prev != null) {
            pmap = new HashMap<>();
            for (DisplayIcon disp : prev)
                pmap.put(disp.gob, disp);
        }
        List<DisplayIcon> ret = new ArrayList<>();
        OCache oc = ui.sess.glob.oc;
        for (Gob gob : oc.getallgobs()) {
            try {
                GobIcon icon = gob.getattr(GobIcon.class);
                if (icon != null && icon.res != null && icon.res.get() != null) {
                    GobIcon.Setting conf = iconconf.get(icon.res.get());
                    if (conf == null && icon instanceof GobIcon.CustomGobIcon) {
                        GobIcon.CustomGobIcon cicon = (GobIcon.CustomGobIcon) icon;
                        Resource res = icon.res.get();
                        iconconf.settings.put(res.name, new GobIcon.CustomSetting(new Resource.Spec(null, res.name, -1), cicon::tex));
                        conf = iconconf.get(res);
                        ui.gui.saveiconconf();
                        if (ui.gui.iconconf.notify) {
                            ui.sess.glob.loader.defer(() -> ui.gui.msg(String.format("%s added to list of seen icons.", res.basename())), null);
                        }
                    }
                    if (conf != null) {
                        if (conf.show) {
                            DisplayIcon disp = pmap.get(gob);
                            if (disp == null) {
                                if (gob.isplayer()) disp = new DisplayIcon(icon, -1);
                                else disp = new DisplayIcon(icon);
                            }
                            disp.update(gob.rc, gob.a);
                            Buddy buddy = gob.getattr(Buddy.class);
                            if ((buddy != null) && (buddy.group() < BuddyWnd.gc.length))
                                disp.col = BuddyWnd.gc[buddy.group()];
                            ret.add(disp);
                        }
                    }
                }
            } catch (Loading l) {
            }
        }
        ret.sort(Comparator.comparingInt(a -> a.z));
        if (ret.isEmpty())
            return (Collections.emptyList());
        return (ret);
    }

    protected void attached() {
        if (iconconf == null) {
            if (ui.gui != null)
                iconconf = ui.gui.iconconf;
        }
        super.attached();
    }

    public static class MapTile {
        public MCache.Grid grid;
        public int seq;

        public MapTile(MCache.Grid grid, int seq) {
            this.grid = grid;
            this.seq = seq;
        }
    }

    public class DisplayIcon {
        public final GobIcon icon;
        public final Gob gob;
        public final GobIcon.Image img;
        public Coord2d rc = null;
        public Coord sc = null;
        public double ang = 0.0;
        public Color col = Color.WHITE;
        public int z;
        public double stime;
        public int priority = 0;

        public DisplayIcon(GobIcon icon) {
            this.icon = icon;
            this.gob = icon.gob;
            this.img = icon.img();
            this.ang = ang;
            this.z = this.img.z;
            this.stime = Utils.rtime();
        }

        public DisplayIcon(GobIcon icon, int priority) {
            this(icon);
            this.priority = priority;
        }

        public void update(Coord2d rc, double ang) {
            this.rc = rc;
            this.ang = ang;
            if ((this.rc == null))
                this.sc = null;
            else
                this.sc = p2c(this.rc);
        }
    }
}
