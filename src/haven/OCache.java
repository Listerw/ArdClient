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


import haven.overlays.TextOverlay;
import haven.purus.pbot.PBotUtils;
import haven.res.ui.obj.buddy.Buddy;
import haven.sloth.gob.Hidden;
import modification.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static haven.MapView.markedGobs;

public class OCache implements Iterable<Gob> {
    public static final int OD_REM = 0;
    public static final int OD_MOVE = 1;
    public static final int OD_RES = 2;
    public static final int OD_LINBEG = 3;
    public static final int OD_LINSTEP = 4;
    public static final int OD_SPEECH = 5;
    public static final int OD_COMPOSE = 6;
    public static final int OD_ZOFF = 7;
    public static final int OD_LUMIN = 8;
    public static final int OD_AVATAR = 9;
    public static final int OD_FOLLOW = 10;
    public static final int OD_HOMING = 11;
    public static final int OD_OVERLAY = 12;
    /* public static final int OD_AUTH = 13; -- Removed */
    public static final int OD_HEALTH = 14;
    /* public static final int OD_BUDDY = 15; -- Removed */
    public static final int OD_CMPPOSE = 16;
    public static final int OD_CMPMOD = 17;
    public static final int OD_CMPEQU = 18;
    public static final int OD_ICON = 19;
    public static final int OD_RESATTR = 20;
    public static final int OD_END = 255;
    public static final int[] compodmap = {OD_REM, OD_RESATTR, OD_FOLLOW, OD_MOVE, OD_RES, OD_LINBEG, OD_LINSTEP, OD_HOMING};
    public static final Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
    /* XXX: Use weak refs */
    private final Collection<Collection<Gob>> local = Collections.synchronizedList(new LinkedList<>());
    private HashMultiMap<Long, Gob> objs = new HashMultiMap<>();
    private Glob glob;
    private final Map<Long, DamageSprite> gobdmgs = Collections.synchronizedMap(new HashMap<>());
    public boolean isfight = false;
    private final Collection<ChangeCallback> cbs = Collections.synchronizedCollection(new WeakList<>());


    public interface ChangeCallback {
        void added(Gob ob);

        void removed(Gob ob);
    }

    public OCache(Glob glob) {
        this.glob = glob;
    }

    public synchronized void callback(ChangeCallback cb) {
        cbs.add(cb);
    }

    public synchronized void uncallback(ChangeCallback cb) {
        cbs.remove(cb);
    }

    public void add(Gob ob) {
        synchronized (ob) {
            Collection<ChangeCallback> cbs;
            synchronized (this) {
                cbs = new ArrayList<>(this.cbs);
                objs.put(ob.id, ob);
            }
            for (ChangeCallback cb : cbs)
                cb.added(ob);
        }
    }

    public void remove(Gob ob) {
        if (DefSettings.KEEPGOBS.get()) return;
        Gob old;
        Collection<ChangeCallback> cbs;
        synchronized (this) {
            old = objs.remove(ob.id, ob);
            if (ob.id != -1 && (old != null) && (old != ob))
                throw (new RuntimeException(String.format("object %d removed wrong object", ob.id)));
            cbs = new ArrayList<>(this.cbs);
        }
        if (old != null) {
            synchronized (old) {
                old.removed();
                for (ChangeCallback cb : cbs)
                    cb.removed(old);
            }
        }
    }

    public void remove(long id) {
        Gob gob = objs.get(id);
        if (!DefSettings.KEEPGOBS.get()) {
            Gob old = objs.remove(id, gob);
            if (old != null) {
                old.dispose();
                for (ChangeCallback cb : new ArrayList<>(cbs))
                    cb.removed(old);
            }
        }
    }

    public synchronized void changed(Gob ob) {
        ob.changed();
        Collection<ChangeCallback> values = new ArrayList<>(cbs);
        for (ChangeCallback cb : values)
            cb.added(ob);
    }

    public void changeAllGobs() {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            changed(g);
        }
    }

    void refreshalloverlays() {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            if (!g.ols.isEmpty())
                g.ols.clear();
        }
    }

    void refreshallresdraw() {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            ResDrawable resDrawable = g.getattr(ResDrawable.class);
            if (resDrawable != null) {
//                resDrawable.refresh();
                g.delattr(ResDrawable.class);
                g.setattr(new ResDrawable(g, resDrawable.res, resDrawable.backup));
            }
        }
    }

    public class ModdedGob extends Virtual {
        public ModdedGob(Coord2d c, double a) {
            super(c, a);
        }
    }

    public void tick() {
        if (!configuration.enablegobticks) return;
        Collection<Gob> values = new ArrayList<>();
        synchronized (this) {
            for (Gob g : this) {
                values.add(g);
            }
        }
        Consumer<Gob> task = g -> {
            synchronized (g) {
                g.tick();
            }
        };
        if (!Config.par)
            values.forEach(task);
        else
            values.parallelStream().forEach(task);
    }

    public void ctick(int dt) {
        if (!configuration.enablegobcticks) return;
        Collection<Gob> values = new ArrayList<>();
        synchronized (this) {
            for (Gob g : this) {
                values.add(g);
            }
        }
        Consumer<Gob> task = g -> {
            synchronized (g) {
                g.ctick(dt);
            }
        };
        if (!Config.par)
            values.forEach(task);
        else
            values.parallelStream().forEach(task);
    }

    @SuppressWarnings("unchecked")
    public Iterator<Gob> iterator() {
        Collection<Iterator<Gob>> is = new LinkedList<>();
        for (Collection<Gob> gc : local)
            is.add(gc.iterator());
        return (new I2<>(objs.values().iterator(), new I2<>(is)));
    }

    public void ladd(Collection<Gob> gob) {
        local.add(gob);
        Collection<ChangeCallback> values = new ArrayList<>(cbs);
        for (Gob g : gob) {
            synchronized (g) {
                for (ChangeCallback cb : values)
                    cb.added(g);
            }
        }
    }

    public void lrem(Collection<Gob> gob) {
        local.remove(gob);
        Collection<ChangeCallback> values = new ArrayList<>(cbs);
        for (Gob g : gob) {
            synchronized (g) {
                for (ChangeCallback cb : values)
                    cb.removed(g);
            }
        }
    }

    /**
     * For the Scripting API
     */
    @SuppressWarnings("unused")
    public synchronized Gob[] getallgobs() {
        return objs.values().toArray(new Gob[0]);
    }

    public synchronized Gob getgob(long id) {
        for (final Gob g : this) {
            if (g.id == id)
                return (g);
        }
        return (null);
    }

    public synchronized Gob createTempGob(long id) {
        Gob gob = new Gob(glob, Coord2d.z, id);
        gob.addol(new Gob.Overlay(gob, new Sprite(gob, null) {
            private double start = Utils.rtime();

            @Override
            public boolean setup(final RenderList d) {
                return (false);
            }

            @Override
            public boolean tick(final double dt) {
                return (Utils.rtime() - start >= 10 * 60);
            }
        }));
        add(gob);
        return (gob);
    }

    private AtomicLong nextvirt = new AtomicLong(-1);

    public class Virtual extends Gob {
        public Virtual(Coord2d c, double a) {
            super(OCache.this.glob, c, nextvirt.getAndDecrement());
            this.a = a;
            virtual = true;
            add(this);
        }
    }

    public class FixedPlace extends Virtual {
        public final Coord3f fc;

        public FixedPlace(Coord3f fc, double a) {
            super(Coord2d.of(fc), a);
            this.fc = fc;
        }

        public FixedPlace() {
            this(Coord3f.o, 0);
        }

        public Coord3f getc() {
            return (fc);
        }

        protected GLState getmapstate(Coord3f pc) {
            return (null);
        }
    }

    public interface Delta {
        void apply(Gob gob, AttrDelta msg);

        static Indir<Resource> getres(Gob gob, int id) {
            return (gob.glob.sess.getres(id));
        }
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DeltaType {
        int value();
    }

    private static final Map<Integer, Delta> deltas = new HashMap<>();

    static {
        deltas:
        for (Class<?> cl : dolda.jglob.Loader.get(DeltaType.class).classes()) {
            int id = cl.getAnnotation(DeltaType.class).value();
            if (Delta.class.isAssignableFrom(cl)) {
                try {
                    Constructor<? extends Delta> cons = cl.asSubclass(Delta.class).getConstructor();
                    deltas.put(id, Utils.construct(cons));
                    continue deltas;
                } catch (NoSuchMethodException e) {}
            }
            throw (new Error("Illegal objdelta class: " + cl));
        }
    }

    @DeltaType(OD_MOVE)
    public static class $move implements Delta {
        public void apply(Gob g, AttrDelta msg) {
            Coord2d c = msg.coord().mul(posres);
            double a = (msg.uint16() / 65536.0) * Math.PI * 2;
            g.move(c, a);
        }
    }

    public static class OlSprite implements Sprite.Mill<Sprite> {
        public final Indir<Resource> res;
        public Message sdt;

        public OlSprite(Indir<Resource> res, Message sdt) {
            this.res = res;
            this.sdt = sdt;
        }

        public Sprite create(Sprite.Owner owner) {
            return (Sprite.create(owner, res.get(), sdt));
        }
    }

    @DeltaType(OD_OVERLAY)
    public static class $overlay implements Delta {
        public void apply(Gob g, AttrDelta msg) {
            int olidf = msg.int32();
            boolean prs = (olidf & 1) != 0;
            int olid = olidf >>> 1;
            int resid = msg.uint16();
            Indir<Resource> res;
            Message sdt;
            if (resid == 65535) {
                res = null;
                sdt = Message.nil;
            } else {
                if ((resid & 0x8000) != 0) {
                    resid &= ~0x8000;
                    sdt = new MessageBuf(msg.bytes(msg.uint8()));
                } else {
                    sdt = Message.nil;
                }
                res = Delta.getres(g, resid);
            }
            Gob.Overlay ol = g.findol(olid);
            OCache oc = g.glob.oc;
            if (res != null) {
                sdt = new MessageBuf(sdt).clone();
                Gob.Overlay nol = null;
                if (ol == null) {
                    g.addol(nol = new Gob.Overlay(g, olid, res, sdt), true);
                    if (sdt.rt == 7 && oc.isfight && Config.showdmgop)
                        oc.setdmgoverlay(g, res, new MessageBuf(sdt).clone());
                } else {
                    if (!ol.sdt.equals(sdt)) {
                        if (ol.spr instanceof Sprite.CUpd || ol.spr instanceof Gob.Overlay.CUpd) {
                            MessageBuf copy = new MessageBuf(sdt).clone();
                            if (ol.spr instanceof Sprite.CUpd)
                                ((Sprite.CUpd) ol.spr).update(copy);
                            else if (ol.spr instanceof Gob.Overlay.CUpd)
                                ((Gob.Overlay.CUpd) ol.spr).update(copy);
                            ol.sdt = copy;
                        } else {
                            g.addol(nol = new Gob.Overlay(g, olid, res, sdt), true);
                            ol.remove(false);
                            if (sdt.rt == 7 && oc.isfight && Config.showdmgop)
                                oc.setdmgoverlay(g, res, new MessageBuf(sdt).clone());
                        }
                    }
                }
                if (nol != null)
                    nol.delign = prs;
            } else {
                if (ol != null) {
                    if (ol.spr instanceof Sprite.CDel)
                        ((Sprite.CDel) ol.spr).delete();
                    else if (ol.spr instanceof Gob.Overlay.CDel)
                        ((Gob.Overlay.CDel) ol.spr).delete();
                    else ol.remove(false);
                }
            }
        }
    }

    @DeltaType(OD_RESATTR)
    public static class $resattr implements Delta {
        public void apply(Gob g, AttrDelta msg) {
            Indir<Resource> resid = Delta.getres(g, msg.uint16());
            int len = msg.uint8();
            Message dat = (len > 0) ? new MessageBuf(msg.bytes(len)) : null;
//            resid.get().getcode(GAttrib.Parser.class, true).apply(g, dat);

            /*if (resid.toString().contains(configuration.crosterresid + "") || resid.toString().contains("ui/croster")) {
                try {
                    if (resid.toString().contains("ui/croster")) {
                        int id = g.glob.sess.getresid(resid.get());
                        if (configuration.crosterresid == -1 || configuration.crosterresid != id) {
                            configuration.crosterresid = id;
                            Utils.setprefi("crosterresid", id);
                        }
                    }
                } catch (Loading le) {
                }
            }*/
            g.defer(() -> {
                Resource res = resid.get();
                GAttrib.Parser parser = res.getcode(GAttrib.Parser.class, false);
                if (parser != null) {
                    parser.apply(g, dat);
                }
                if (dat != null)
                    g.setrattr(resid, dat);
                else
                    g.delrattr(resid);
                g.ocontext(OCache.class).ifPresent(oc -> oc.changed(g));
            });
        }
    }

    private Indir<Resource> getres(int id) {
        return (glob.sess.getres(id));
    }

    void changeHealthGobs() {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (Gob g : values) {
            if (g.getattr(GobHealth.class) != null &&
                    g.getattr(GobHealth.class).hp < 4)
                changed(g);
        }
    }

    private void setdmgoverlay(final Gob g, final Indir<Resource> resid, final MessageBuf sdt) {
        final int dmg = sdt.int32();
        // ignore dmg of 1 from scents
        if (dmg == 1)
            return;
        sdt.uint8();
        final int clr = sdt.uint16();
        if (clr != 61455 /* damage */ && clr != 36751 /* armor damage */)
            return;

        glob.loader.defer(new Runnable() {
            public void run() {
                try {
                    Resource res = resid.get();
                    if (res != null && res.name.equals("gfx/fx/floatimg")) {
                        synchronized (gobdmgs) {
                            DamageSprite dmgspr = gobdmgs.get(g.id);
                            if (dmgspr == null) {
                                if (Config.logcombatactions) {
                                    Buddy buddy = g.getattr(Buddy.class);
                                    if (g.isplayer())
                                        PBotUtils.sysLogAppend(getUI(), "I got hit for " + dmg + " Damage.", "white");
                                    else if (buddy != null)
                                        PBotUtils.sysLogAppend(getUI(), "Hit " + buddy.name() + " For " + dmg + " Damage.", "green");
                                    else if (g.getres().basename().contains("Body"))
                                        PBotUtils.sysLogAppend(getUI(), "Hit Unknown player For " + dmg + " Damage.", "green");
                                    else
                                        PBotUtils.sysLogAppend(getUI(), "Hit " + g.getres().basename() + " For " + dmg + " Damage.", "green");
                                }
                                gobdmgs.put(g.id, new DamageSprite(dmg, clr == 36751, g));
                            } else
                                dmgspr.update(dmg, clr == 36751);
                        }
                    }
                } catch (Loading le) {
                    glob.loader.defer(this, null);
                }
            }
        }, null);
    }

    public GameUI getGUI() {
        final GameUI gui = glob.ui.get().gui;
        return gui;
        //return HavenPanel.lui.root.findchild(GameUI.class);
    }

    public UI getUI() {
        final UI ui = glob.ui.get();
        return ui;
        //return HavenPanel.lui.root.findchild(GameUI.class);
    }

    public void removedmgoverlay(long gobid) {
        synchronized (gobdmgs) {
            gobdmgs.remove(gobid);
        }
    }

    public void quality(Gob g, int quality) {
        g.setattr(new GobQuality(g, quality));

        Gob.Overlay ol = g.findol(Sprite.GOB_QUALITY_ID);
        if (quality > 0) {
            if (ol == null)
                g.addol(new Gob.Overlay(g, Sprite.GOB_QUALITY_ID, new GobQualitySprite(quality)));
            else if (((GobQualitySprite) ol.spr).val != quality)
                ((GobQualitySprite) ol.spr).update(quality);
        } else {
            if (ol != null)
                g.ols.remove(ol);
        }
        changed(g);
    }

    public void customattr(Gob g, String attr, int life) {
        g.setattr(new GobCustomAttr(g, attr));

        Gob.Overlay ol = g.findol(Sprite.GOB_CUSTOM_ID);
        if (!attr.equals("")) {
            if (ol == null)
                g.addol(new Gob.Overlay(g, Sprite.GOB_CUSTOM_ID, new GobCustomSprite(attr, life)));
            else if (!((GobCustomSprite) ol.spr).val.equals(attr))
                ((GobCustomSprite) ol.spr).update(attr);
        } else {
            if (ol != null)
                g.ols.remove(ol);
        }
        changed(g);
    }

    public void highlightGobs(final String gname) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(name -> {
                if (gname.equals(name)) {
                    g.mark(-1);
                }
            });
        }
    }

    public void unhighlightGobs(final String gname) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(name -> {
                if (gname.equals(name)) {
                    g.unmark();
                }
            });
        }
    }

    public void ovTextGobs(final String gname) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(name -> {
                if (gname.equals(name)) {
                    g.addol(new Gob.Overlay(g, Sprite.GOB_TEXT_ID, new TextOverlay(g)));
                }
            });
        }
    }

    public void unovTextGobs(final String gname) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(name -> {
                if (gname.equals(name)) {
                    Gob.Overlay ol = g.findol(Sprite.GOB_TEXT_ID);
                    g.ols.remove(ol);
                }
            });
        }
    }

    public void ovHighGobs(final String gname) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(name -> {
                if (gname.equals(name)) {
                    if (!markedGobs.contains(g.id))
                        markedGobs.add(g.id);
                    glob.oc.changed(g);
                }
            });
        }
    }

    public void unovHighGobs(final String gname) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(name -> {
                if (gname.equals(name)) {
                    if (markedGobs.contains(g.id))
                        markedGobs.remove(g.id);
                    glob.oc.changed(g);
                }
            });
        }
    }

    public void hideAll(final String name) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(gname -> {
                if (gname.equals(name)) {
                    g.setattr(new Hidden(g));
                    changed(g);
                }
            });
        }
    }

    public void unhideAll(final String name) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        for (final Gob g : values) {
            g.resname().ifPresent(gname -> {
                if (gname.equals(name)) {
                    g.delattr(Hidden.class);
                    changed(g);
                }
            });
        }
    }

    public void removeAll(final String name) {
        Collection<Gob> values = new ArrayList<>(objs.values());
        //TODO: I2 iterator doesn't support remove and I should fix that later on, for now this is a two step process
        final List<Gob> rem = new ArrayList<>();
        for (final Gob g : values) {
            g.resname().ifPresent(gname -> {
                if (gname.equals(name)) {
                    g.dispose();
                    rem.add(g);
                }
            });
        }

        for (Gob g : rem) {
            remove(g);
        }
    }

    public void resattr(Gob g, Indir<Resource> resid, Message dat) {
        /*if (resid.toString().contains(configuration.crosterresid + "") || resid.toString().contains("ui/croster")) {
            try {
                if (resid.toString().contains("ui/croster")) {
                    int id = getUI().sess.getresid(resid.get());
                    if (configuration.crosterresid == -1 || configuration.crosterresid != id) {
                        configuration.crosterresid = id;
                        Utils.setprefi("crosterresid", id);
                    }
                }
            } catch (Loading le) {
            }
        }*/
        glob.loader.defer(() -> {
            Resource res = resid.get();
            GAttrib.Parser parser = res.getcode(GAttrib.Parser.class, false);
            if (parser != null) {
                parser.apply(g, dat);
            }
            if (dat != null)
                g.setrattr(resid, dat);
            else
                g.delrattr(resid);
            changed(g);
        }, null);
    }

    public void resattr(Gob gob, Message msg) {
        Indir<Resource> resid = getres(msg.uint16());
        int len = msg.uint8();
        Message dat = (len > 0) ? new MessageBuf(msg.bytes(len)) : null;
        if (gob != null)
            resattr(gob, resid, dat);
    }

    public class GobInfo {
        public final long id;
        public final LinkedList<AttrDelta> pending = new LinkedList<>();
        public int frame;
        public boolean nremoved, added, gremoved, virtual;
        public Gob gob;
        public Loader.Future<?> applier;

        public GobInfo(long id, int frame) {
            this.id = id;
            this.frame = frame;
        }

        private void apply() {
            main:
            {
                synchronized (this) {
                    if (nremoved && (!added || gremoved))
                        break main;
                    if (nremoved && added && !gremoved) {
                        remove(gob);
                        gob.updated();
                        gremoved = true;
                        gob = null;
                        break main;
                    }
                    if (gob == null) {
                        gob = new Gob(glob, Coord2d.z, id);
                        gob.virtual = virtual;
                    }
                }
                while (true) {
                    AttrDelta d;
                    synchronized (this) {
                        if ((d = pending.peek()) == null)
                            break;
                    }
                    synchronized (gob) {
                        deltas.get(d.type).apply(gob, d.clone());
                        changed(gob);
                    }
                    synchronized (this) {
                        if ((pending.poll()) != d)
                            throw (new RuntimeException());
                    }
                }
                if (!added) {
                    add(gob);
                    added = true;
                }
                gob.updated();
            }
            synchronized (this) {
                applier = null;
                checkdirty(false);
            }
        }

        public void checkdirty(boolean interrupt) {
            synchronized (this) {
                if (applier == null) {
                    if (nremoved ? (added && !gremoved) : (!added || !pending.isEmpty())) {
                        Loader.Future<?> f = applier = glob.loader.deferWait(this::apply, null);
                        Runnable r = f::cancel;
                        glob.sess.conn.onClose.add(r);
                        f.onDone.add(() -> glob.sess.conn.onClose.remove(r));
                        glob.loader.startTask(f);
                    }
                } else if (interrupt) {
                    applier.restart();
                }
            }
        }
    }

    private final Map<Long, GobInfo> netinfo = new HashMap<>();

    private GobInfo netremove(long id, int frame) {
        synchronized (netinfo) {
            GobInfo ng = netinfo.get(id);
            if ((ng == null) || (ng.frame > frame))
                return (null);
            synchronized (ng) {
                /* XXX: Clean up removed objects */
                ng.nremoved = true;
                ng.checkdirty(true);
            }
            return (ng);
        }
    }

    private GobInfo netget(long id, int frame) {
        synchronized (netinfo) {
            GobInfo ng = netinfo.get(id);
            if ((ng != null) && ng.nremoved) {
                if (ng.frame >= frame)
                    return (null);
                netinfo.remove(id);
                ng = null;
            }
            if (ng == null) {
                ng = new GobInfo(id, frame);
                netinfo.put(id, ng);
            } else {
                if (ng.frame >= frame)
                    return (null);
            }
            return (ng);
        }
    }

    public static class ObjDelta {
        public int fl, frame;
        public int initframe;
        public long id;
        public final List<AttrDelta> attrs = new LinkedList<>();
        public boolean rem = false;

        public ObjDelta(int fl, long id, int frame) {
            this.fl = fl;
            this.id = id;
            this.frame = frame;
        }

        public ObjDelta() {}
    }

    public static class AttrDelta extends PMessage {
        public boolean old;

        public AttrDelta(ObjDelta od, int type, Message blob, int len) {
            super(type, blob, len);
            this.old = ((od.fl & 4) != 0);
        }

        public AttrDelta(AttrDelta from) {
            super(from);
            this.old = from.old;
        }

        public AttrDelta clone() {
            return (new AttrDelta(this));
        }
    }

    public GobInfo receive(ObjDelta delta) {
        if (delta.rem)
            return (netremove(delta.id, delta.frame - 1));
        synchronized (netinfo) {
            if (delta.initframe > 0)
                netremove(delta.id, delta.initframe - 1);
            GobInfo ng = netget(delta.id, delta.frame);
            if (ng != null) {
                synchronized (ng) {
                    ng.frame = delta.frame;
                    ng.virtual = ((delta.fl & 2) != 0);
                    ng.pending.addAll(delta.attrs);
                    ng.checkdirty(false);
                }
            }
            return (ng);
        }
    }

    //OLD
    public void move(Gob g, Coord2d c, double a) {
        g.move(c, a);
        changed(g);
    }

    public void composite(Gob g, Indir<Resource> base) {
        Drawable dr = g.getattr(Drawable.class);
        Composite cmp = (dr instanceof Composite) ? (Composite) dr : null;
        if ((cmp == null) || !cmp.base.equals(base)) {
            cmp = new Composite(g, base);
            g.setattr(cmp);
            g.remol(g.findol(GobHitbox.olid_solid));
            g.remol(g.findol(GobHitbox.olid));
        }
        changed(g);
    }

    public void cmpmod(Gob g, List<Composited.MD> mod) {
        Composite cmp = (Composite) g.getattr(Drawable.class);
        if (cmp == null)
            return;
//            throw (new RuntimeException(String.format("cmpmod on non-composed object: %s", mod)));
        cmp.chmod(mod);
        changed(g);
    }

    public void cmppose(Gob g, int pseq, List<ResData> poses, List<ResData> tposes, boolean interp, float ttime) {
        Composite cmp = (Composite) g.getattr(Drawable.class);
        if (cmp == null) {
            System.out.println(String.format("cmppose on non-composed object: %s %s %s %s", poses, tposes, interp, ttime));
            return;
        }
        if (cmp.pseq != pseq) {
            cmp.pseq = pseq;
//                if (poses != null)
            cmp.chposes(poses, interp);
//                if (tposes != null)
            cmp.tposes(tposes, WrapMode.ONCE, ttime);
        }
        changed(g);
    }

    public void cres(Gob g, Indir<Resource> res, Message dat) {
        MessageBuf sdt = new MessageBuf(dat);
        Drawable dr = g.getattr(Drawable.class);
        ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
        if ((d != null) && (d.res == res) && !d.sdt.equals(sdt) && (d.spr != null) && ((d.spr instanceof Gob.Overlay.CUpd) || (d.spr instanceof Sprite.CUpd))) {
            if (d.spr instanceof Sprite.CUpd)
                ((Sprite.CUpd) d.spr).update(sdt);
            else if (d.spr instanceof Gob.Overlay.CUpd)
                ((Gob.Overlay.CUpd) d.spr).update(sdt);
            d.sdt = sdt;
            g.updsdt();
        } else if ((d == null) || (d.res != res) || !d.sdt.equals(sdt)) {
            g.setattr(new ResDrawable(g, res, sdt));
            g.remol(g.findol(GobHitbox.olid_solid));
            g.remol(g.findol(GobHitbox.olid));
        }
        changed(g);
    }
}