import haven.*;
import haven.res.lib.uspr.*;

public class Roastspit extends UnivSprite {
    private int eqid;
    private Rendered equed;
    private GLState eqp;

    public Roastspit(Owner owner, Resource res, Message sdt) {
	super(owner, res, new MessageBuf(sdt.bytes(1)));
	eqid = sdt.uint16();
	updeq();
    }

    public void update(Message sdt) {
	super.update(new MessageBuf(sdt.bytes(1)));
	eqid = sdt.uint16();
	updeq();
    }

    private void updeq() {
	if(eqid >= 0) {
	    try {
		if(eqid == 65535) {
		    equed = null;
		    eqp = null;
		} else {
		    Resource eqr = owner.context(Resource.Resolver.class).getres(eqid).get();
		    Sprite eqs = Sprite.create(owner, eqr, Message.nil);
		    Skeleton.BoneOffset bo = eqr.layer(Skeleton.BoneOffset.class, "s");
		    eqp = bo.forpose(getpose());
		    equed = eqs;
		}
		eqid = -1;
		update();
	    } catch(Loading l) {
	    }
	}
    }

    public boolean tick(int idt) {
	updeq();
	return(super.tick(idt));
    }

    public boolean setup(RenderList rl) {
	if(equed != null)
	    rl.add(equed, eqp);
	return(super.setup(rl));
    }
}
