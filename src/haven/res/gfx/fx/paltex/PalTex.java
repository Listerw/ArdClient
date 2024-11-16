package haven.res.gfx.fx.paltex;

import haven.Material;
import haven.Message;
import haven.Resource;
import haven.TexGL;
import haven.TexR;

public class PalTex implements Material.Factory {
    public Material create(Material.Owner owner, Resource res, Message sdt) {
        Resource.Resolver rr = owner.context(Resource.Resolver.class);
        Resource pres = rr.getres(sdt.uint16()).get();
        Resource tres = rr.getres(sdt.uint16()).get();
        Material mat = Material.fromres(owner, pres, Message.nil);
        TexGL base = tres.layer(TexR.class).tex();
        return (new Material(mat, base.draw));
    }
}
