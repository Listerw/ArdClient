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

import haven.res.gfx.terobjs.barterarea.BarterArea;
import haven.res.gfx.terobjs.dng.beedungeon.Beehive;
import haven.res.gfx.terobjs.dng.powersplit.PowerSprite;
import haven.res.gfx.terobjs.items.decal.Decal;
import haven.res.gfx.terobjs.peacebreaker.Peacebreaker;
import haven.res.gfx.terobjs.road.routeindicator.Route;
import haven.res.lib.tree.Tree;
import modification.Bed;
import modification.Billpole;
import modification.Fixedplob;
import modification.dev;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class Sprite implements Rendered {
    public static final int GOB_HEALTH_ID = -1001;
    public static final int GROWTH_STAGE_ID = -1002;
    public static final int GROWTH_STAGE_ID2 = "treestage".hashCode();
    public static final int GOB_QUALITY_ID = -1003;
    public static final int GOB_CUSTOM_ID = -1004;
    public static final int GOB_TEXT_ID = -1005;
    public final Resource res;
    public final Owner owner;
    public static List<Factory> factories = new LinkedList<>();

    static {
        factories.add(SpriteLink.sfact);
        factories.add(SkelSprite.fact);
        factories.add(AnimSprite.fact);
        factories.add(StaticSprite.fact);
        factories.add(AudioSprite.fact);
    }

    public interface Owner extends OwnerContext {
        Random mkrandoom();

        Resource getres();

        @Deprecated
        default Glob glob() {
            return (context(Glob.class));
        }
    }

    public class RecOwner implements Owner, Skeleton.HasPose {
        public Random mkrandoom() {
            return (owner.mkrandoom());
        }

        public <T> T context(Class<T> cl) {
            return (owner.context(cl));
        }

        public Resource getres() {
            return (res);
        }

        public Skeleton.Pose getpose() {
            return (Skeleton.getpose(Sprite.this));
        }
    }

    public interface CDel {
        void delete();
    }

    public interface CUpd {
        void update(Message sdt);
    }

    public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
        public FactMaker() {
            super(Factory.class);
        }

        {
            add(new Direct<>(Factory.class));
            add(new StaticCall<>(Factory.class, "mksprite", Sprite.class, new Class<?>[]{Owner.class, Resource.class, Message.class}, (make) -> (owner, res, sdt) -> make.apply(new Object[]{owner, res, sdt})));
            add(new Construct<>(Factory.class, Sprite.class, new Class<?>[]{Owner.class, Resource.class}, (cons) -> (owner, res, sdt) -> cons.apply(new Object[]{owner, res})));
            add(new Construct<>(Factory.class, Sprite.class, new Class<?>[]{Owner.class, Resource.class, Message.class}, (cons) -> (owner, res, sdt) -> cons.apply(new Object[]{owner, res, sdt})));
        }
    }

    @Resource.PublishedCode(name = "spr", instancer = FactMaker.class)
    public interface Factory {
        Sprite create(Owner owner, Resource res, Message sdt);
    }

    public interface Mill<S extends Sprite> {
        S create(Owner owner);
    }

    public static class ResourceException extends RuntimeException {
        public Resource res;

        public ResourceException(String msg, Resource res) {
            super(msg + " (" + res + ", from " + res.source + ")");
            this.res = res;
        }

        public ResourceException(String msg, Throwable cause, Resource res) {
            super(msg + " (" + res + ", from " + res.source + ")", cause);
            this.res = res;
        }
    }

    protected Sprite(Owner owner, Resource res) {
        this.res = res;
        this.owner = owner;
    }

    public static int decnum(Message sdt) {
        if (sdt == null)
            return (0);
        int ret = 0, off = 0;
        while (!sdt.eom()) {
            ret |= sdt.uint8() << off;
            off += 8;
        }
        return (ret);
    }

    public static Sprite create(Owner owner, Resource res, Message sdt) {
        if (res instanceof Resource.FakeResource) {
            return (new FakeSprite(owner, res, sdt));
        }
        try {
            if (res.name.equals("gfx/terobjs/peacebreaker-full")) {
                return (Peacebreaker.mksprite(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/items/carrytagpole")) {
                return (Billpole.mksprite(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/dng/beedungeon")) {
                return (Beehive.mksprite(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/dng/powermonolith")) {
                return (PowerSprite.mksprite(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/barterarea")) {
                return (new BarterArea(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/road/routeindicator")) {
                return (new Route(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/coronationstone")) {
                Factory f = new modification.Corostone();
                return (f.create(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/thingwall")) {
                Factory f = new modification.Thingwall();
                return (f.create(owner, res, sdt));
            } else if (res.name.startsWith("gfx/terobjs/items/decal-") || res.name.startsWith("gfx/terobjs/items/parchment-decal")) {
                Factory f = new Decal();
                return (f.create(owner, res, sdt));
            } else if (res.name.equals("ui/plob-fdir")) {
                return (new Fixedplob(owner, res, sdt));
            } else if (res.name.equals("gfx/terobjs/furn/bed-sturdy")) {
                Factory f = new Bed();
                return (f.create(owner, res, sdt));
            } else if (res.name.matches("gfx/terobjs/trees/(pine|kingsoak)")) {
                return (Tree.mksprite(owner, res, sdt));
            }
            {
                Factory f = res.getcode(Factory.class, false);
                if (f != null)
                    return (f.create(owner, res, sdt));
            }
            for (Factory f : factories) {
                Sprite ret = f.create(owner, res, sdt);
                if (ret != null)
                    return (ret);
            }
        } catch (Loading l) {
            throw l;
        } catch (Throwable e) {
            dev.simpleLog(e);
            return (new FakeSprite(owner, res, sdt));
        }
        return (new FakeSprite(owner, res, sdt));
//        throw (new ResourceException("Does not know how to draw resource " + res.name, res));
    }

    public void draw(GOut g) {
    }

    public abstract boolean setup(RenderList d);

    public boolean tick(double dt) {
        return(false);
    }

    public boolean tick(int dt) {
        return (false);
    }

    public void age() {
    }

    public void dispose() {
    }


    public static class FakeSprite extends Sprite {
        public final byte[] data;

        public FakeSprite(Owner owner, Resource resource, Message sdt) {
            super(owner, resource);
            this.data = sdt.bytes();
            dev.simpleLog("Fake sprite: " + resource);
        }

        @Override
        public boolean setup(RenderList d) {
            return (false);
        }
    }

    public static class DelayedSprite extends Sprite {
        public Sprite sprite;

        public DelayedSprite(Owner owner, Resource resource) {
            super(owner, resource);
        }

        public void draw(GOut g) {
            if (sprite != null) sprite.draw(g);
        }

        @Override
        public boolean setup(RenderList d) {
            return (sprite != null && sprite.setup(d));
        }

        public boolean tick(int dt) {
            return (sprite != null && sprite.tick(dt));
        }

        public void dispose() {
            if (sprite != null) sprite.dispose();
        }
    }
}
