import haven.CloudShadow;
import haven.Coord3f;
import haven.Glob;
import haven.RenderList;
import haven.Resource;
import haven.TexGL;
import haven.TexR;

public class Clouds implements Glob.Weather {
    public static final TexGL clouds = (TexGL) Resource.remote().loadwait("gfx/fx/clouds").layer(TexR.class).tex();
    float scale, cmin, cmax, rmin, rmax;
    float nscale, ncmin, ncmax, nrmin, nrmax;
    float oscale, ocmin, ocmax, ormin, ormax;
    float xv, yv;
    float ia = -1;
    CloudShadow cur;

    public Clouds(Object... args) {
        update(args);
        scale = nscale;
        cmin = ncmin;
        cmax = ncmax;
        rmin = nrmin;
        rmax = nrmax;
        ia = -1;
    }

    public void gsetup(RenderList rl) {
        Coord3f vel = new Coord3f(xv, yv, 0);
        if (cur == null) {
            cur = new CloudShadow(clouds, Glob.amblight(rl), vel, scale);
        } else {
            cur.light = Glob.amblight(rl);
            cur.vel = vel;
            cur.scale = scale;
        }
        cur.cmin = cmin; cur.cmax = cmax; cur.rmin = rmin; cur.rmax = rmax;
        rl.prepc(cur);
    }

    public void update(Object... args) {
        int n = 0;
        oscale = scale;
        ocmin = cmin;
        ocmax = cmax;
        ormin = rmin;
        ormax = rmax;
        nscale = 1.0f / (Integer) args[n++];
        ncmin = ((Number) args[n++]).floatValue() / 100.0f;
        ncmax = ((Number) args[n++]).floatValue() / 100.0f;
        nrmin = ((Number) args[n++]).floatValue() / 100.0f;
        nrmax = ((Number) args[n++]).floatValue() / 100.0f;
        if (args.length > n) {
            xv = ((Number) args[n++]).floatValue();
            yv = ((Number) args[n++]).floatValue();
        } else {
            xv = 0.001f;
            yv = 0.002f;
        }
        ia = 0;
    }

    public boolean tick(int idt) {
        if (ia != -1) {
            float dt = idt / 1000.0f;
            ia += dt;
            if (ia >= 2) {
                scale = nscale;
                cmin = ncmin;
                cmax = ncmax;
                rmin = nrmin;
                rmax = nrmax;
                ia = -1;
            } else {
                float A = ia / 2.0f, B = 1.0f - A;
                scale = (A * nscale) + (B * oscale);
                cmin = (A * ncmin) + (B * ocmin);
                cmax = (A * ncmax) + (B * ocmax);
                rmin = (A * nrmin) + (B * ormin);
                rmax = (A * nrmax) + (B * ormax);
            }
        }
        return (false);
    }
}
