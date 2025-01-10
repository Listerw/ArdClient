package haven.sloth.gob;

import haven.GAttrib;
import haven.Gob;

public class Holding extends GAttrib {
    public final long held;

    public Holding(final Gob g, final long held) {
        super(g);
        this.held = held;
    }

    public Gob tgt() {
        return (gob.glob.oc.getgob(this.held));
    }
}
