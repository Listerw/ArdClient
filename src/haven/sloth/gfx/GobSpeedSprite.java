package haven.sloth.gfx;

import haven.Camera;
import haven.Coord;
import haven.Coord3f;
import haven.GLState;
import haven.GOut;
import haven.Gob;
import haven.Location;
import haven.Matrix4f;
import haven.PUtils;
import haven.PView;
import haven.Projection;
import haven.RenderList;
import haven.Sprite;
import haven.Tex;
import haven.TexI;
import haven.Text;
import haven.UI;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class GobSpeedSprite extends Sprite {
    public static final int id = -24447;
    private Tex speed;
    private double lspeed;
    //private final Matrix4f mv = new Matrix4f();
    //private Projection proj;
    //private Coord wndsz;
    //private Location.Chain loc;
    //private Camera camp;
    //private Coord3f sc, sczu;

    public GobSpeedSprite(final Gob g) {
        super(g, null);
    }

    public void draw(GOut g) {
        if (speed != null && lspeed != 0) {
            final Gob gob = (Gob) owner;
            if (gob.sc == null) {
                return;
            }
            final Coord c = gob.sc.add(new Coord(gob.sczu.mul(15))).sub(0, UI.scale(35));
            //mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id));
            //sc = proj.toscreen(mv.mul4(Coord3f.o), wndsz);
            //sczu = proj.toscreen(mv.mul4(Coord3f.zu), wndsz).sub(sc);
            //final Coord c = new Coord(sc.add(sczu.mul(16)));
            g.aimage(speed, c, 0.5, 1.0);
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        GLState.Buffer buf = rl.state();
        //proj = buf.get(PView.proj);
        //wndsz = buf.get(PView.wnd).sz();
        //loc = buf.get(PView.loc);
        //camp = buf.get(PView.cam);
        return true;
    }

    @Override
    public boolean tick(int dt) {
        final Gob g = (Gob) owner;
        final double spd = g.getv();
        if (spd != lspeed) {
            BufferedImage img = Text.render(String.format("%.2f", spd), new Color(200, 200, 200)).img;
            speed = new TexI(PUtils.rasterimg(PUtils.blurmask2(img.getRaster(), 1, 1, Color.BLACK)));
            lspeed = spd;
        }
        return super.tick(dt);
    }

    public Object staticp() {
        return Gob.STATIC;
    }
}
