package haven.res.gfx.terobjs.trees.fallenfruit;

import haven.FastMesh;
import haven.GLState;
import haven.Indir;
import haven.Material;
import haven.RenderLink;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.Utils;
import haven.resutil.CSprite;

import java.util.ArrayList;
import java.util.Random;

public class FallenFruit implements RenderLink {
    public final Indir<Resource> fspec;
    public final int numl, numh;
    public final float r;

    public FallenFruit(Resource lres, Object... args) {
        int a = 0;
        fspec = lres.pool.load((String) args[a++], (Integer) args[a++]);
        numl = Utils.iv(args[a++]);
        numh = Utils.iv(args[a++]);
        r = Utils.fv(args[a++]);
    }

    private ArrayList<FastMesh.MeshRes> var = null;

    public Rendered make(Sprite.Owner owner) {
        if (var == null) {
            Resource res = fspec.get();
            var = new ArrayList<FastMesh.MeshRes>(res.layers(FastMesh.MeshRes.class));
        }
        Random rnd = owner.mkrandoom();
        CSprite spr = new CSprite(owner, fspec.get());
        int num = rnd.nextInt(numh - numl + 1) + numl;
        GLState[] mats = new GLState[var.size()];
        for (int i = 0; i < num; i++) {
            int n = rnd.nextInt(var.size());
            FastMesh.MeshRes v = var.get(n);
            if (mats[n] == null) {
                Material mat = v.mat.get();
                /*mat = new Material(new GLState[] {mat.states, Svaj.slot.nil, Clickable.slot.nil}, new GLState[] {mat.dynstates});*/
                mats[n] = mat;
            }
            spr.addpart((float) rnd.nextGaussian() * r, (float) rnd.nextGaussian() * r, mats[n], v.m);
        }
        return (spr);
    }
}
