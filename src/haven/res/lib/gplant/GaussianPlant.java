package haven.res.lib.gplant;

import haven.Config;
import haven.FastMesh;
import haven.GLState;
import haven.Message;
import haven.RenderLink;
import haven.Rendered;
import haven.Resource;
import haven.Sprite;
import haven.Sprite.Owner;
import haven.resutil.CSprite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class GaussianPlant implements Sprite.Factory {
    public final int numl, numh;
    public final float r;
    public final List<Collection<Function<Owner, Rendered>>> var;

    public GaussianPlant(Resource res, int numl, int numh, float r) {
        this.numl = numl;
        this.numh = numh;
        this.r = r;
        Map<Integer, Collection<Function<Owner, Rendered>>> vars = new HashMap<>();
        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
            Rendered w = mr.mat.get().apply(mr.m);
            vars.computeIfAbsent(mr.id, k -> new ArrayList<>()).add(o -> w);
        }
        for (RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
            vars.computeIfAbsent(lr.id, k -> new ArrayList<>()).add(lr.l::make);
        }
        this.var = new ArrayList<>(vars.values());
    }

    public GaussianPlant(Resource res, Object[] args) {
        this(res, ((Number) args[0]).intValue(), ((Number) args[1]).intValue(), ((Number) args[2]).floatValue());
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
        Random rnd = owner.mkrandoom();
        CSprite spr = new CSprite(owner, res);
        if (Config.simplecrops) {
            for (Function<Owner, Rendered> mk : var.get(0)) {
                spr.addpart(0, 0, null, mk.apply(owner));
                break;
            }
        } else {
            int num = rnd.nextInt(numh - numl + 1) + numl;
            for (int i = 0; i < num; i++) {
                float x = (float) rnd.nextGaussian() * r, y = (float) rnd.nextGaussian() * r;
                for (Function<Owner, Rendered> mk : var.get(rnd.nextInt(var.size())))
                    spr.addpart(x, y, null, mk.apply(owner));
            }
        }
        return (spr);
    }
}
