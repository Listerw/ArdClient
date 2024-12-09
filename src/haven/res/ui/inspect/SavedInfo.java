package haven.res.ui.inspect;

import haven.GAttrib;
import haven.Gob;

import java.util.Collections;
import java.util.List;

public class SavedInfo extends GAttrib {
    public List<String> lines = Collections.emptyList();

    public SavedInfo(Gob gob) {
        super(gob);
    }
}
