package haven.res.gfx.terobjs.mm.kritter;

import haven.Coord;
import haven.GLState;
import haven.GOut;
import haven.GobIcon;
import haven.Message;
import haven.OwnerContext;
import haven.PUtils;
import haven.Resource;
import haven.res.lib.monochrome.Monochrome;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

public class Factory implements GobIcon.Icon.Factory {
    public static final Color monocol = Color.WHITE;
    public static final GLState mono = new Monochrome(monocol, 1);
    public final Resource res;
    public final GobIcon.Image img;
    public final BufferedImage deadimg;

    public Factory(Resource res) {
        this.res = res;
        this.img = GobIcon.Image.get(res);
        this.deadimg = PUtils.monochromize(PUtils.copy(res.flayer(Resource.imgc).img), monocol);
    }

    public class Kritter extends GobIcon.ImageIcon {
        public final boolean dead;

        public Kritter(OwnerContext owner, Resource res, boolean dead) {
            super(owner, res, haven.res.gfx.terobjs.mm.kritter.Factory.this.img);
            this.dead = dead;
        }

        public BufferedImage image() {
            if (dead)
                return (deadimg);
            return (super.image());
        }

        public void draw(GOut g, Coord sc) {
            if (dead) {
                g.usestate(mono);
                super.draw(g, sc);
                g.defstate();
            } else {
                super.draw(g, sc);
            }
        }

        public Object[] id() {
            if (dead)
                return (new Object[]{"dead"});
            return (nilid);
        }

        public String name() {
            if (dead)
                return (super.name() + ", dead");
            return (super.name());
        }
    }

    public Kritter create(OwnerContext owner, Resource res, Message sdt) {
        return (new Kritter(owner, res, !sdt.eom() && (sdt.uint8() != 0)));
    }

    public Collection<Kritter> enumerate(OwnerContext owner, Resource res, Message sdt) {
        return (Arrays.asList(new Kritter(owner, res, false),
                new Kritter(owner, res, true)));
    }
}
