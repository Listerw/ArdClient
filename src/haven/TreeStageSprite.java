package haven;

import java.awt.Color;


public class TreeStageSprite extends Sprite {
    private static final Tex[] treestg = new Tex[255];
    private static final Color stagecolor = new Color(115, 255, 25);
    private static final Tex growth = Resource.local().loadwait("gfx/hud/rosters/growth").layer(Resource.imgc).tex();
    public int val;
    private Tex tex;
    private static Matrix4f mv = new Matrix4f();
    private Projection proj;
    private Coord wndsz;
    private Location.Chain loc;
    private Camera camp;

    /*static {
        for (int i = 0; i < treestg.length; i++) {
            if (i != 100)
                treestg[i] = PUtils.strokeTex(Text.num12boldFnd.render(i + "%", stagecolor));
        }
    }*/

    public TreeStageSprite(int val) {
        super(null, null);
        update(val);
    }

    public void draw(GOut g) {
        float[] c = mv.load(camp.fin(Matrix4f.id)).mul1(loc.fin(Matrix4f.id)).homoc();
        Coord sc = proj.get2dCoord(c, wndsz);
        if (tex != null) {
            sc.x -= (growth.sz().x + tex.sz().x) / 2;;
            sc.y -= 10;
            g.image(growth, sc.add(0, Math.max(growth.sz().y, tex.sz().y) / 2 - growth.sz().y / 2));
            g.image(tex, sc.add(growth.sz().x, Math.max(growth.sz().y, tex.sz().y) / 2 - tex.sz().y / 2));
        }
    }

    public boolean setup(RenderList rl) {
        rl.prepo(last);
        GLState.Buffer buf = rl.state();
        proj = buf.get(PView.proj);
        wndsz = buf.get(PView.wnd).sz();
        loc = buf.get(PView.loc);
        camp = buf.get(PView.cam);
        return true;
    }

    public void update(int val) {
        this.val = val;
        if (val >= 0 && val != 100) {
            tex = tex(val);
        }
    }

    private Tex tex(int val) {
        if (treestg[val] == null) {
            treestg[val] = PUtils.strokeTex(Text.num12boldFnd.render(val + "%", stagecolor));
        }
        return treestg[val];
    }

    public Object staticp() {
        return CONSTANS;
    }
}
