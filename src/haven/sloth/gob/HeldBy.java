package haven.sloth.gob;

import haven.GAttrib;
import haven.Gob;

public class HeldBy extends GAttrib {
    public final long holder;

    public HeldBy(final Gob g, final long holder) {
        super(g);
        this.holder = holder;
    }

    public Gob tgt() {
        return (gob.glob.oc.getgob(this.holder));
    }
}
