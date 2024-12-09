package haven.res.ui.inspect;

import haven.Audio;
import haven.GameUI;
import haven.Glob;
import haven.Gob;
import haven.OwnerContext;
import haven.UI;
import haven.Utils;
import haven.Widget;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Info implements UI.Notice {
    public final long gobid;
    public final boolean syn;
    public final String text;

    public Info(long gobid, boolean syn, String text) {
        this.gobid = gobid;
        this.syn = syn;
        this.text = text;
    }

    public static UI.Notice mkmessage(OwnerContext owner, Object... args) {
        long gobid = Utils.uiv(args[0]);
        String text = (String) args[1];
        boolean syn = (args.length > 2) ? Utils.bv(args[2]) : false;
        return (new Info(gobid, syn, text));
    }

    public String message() {return (text);}

    public Color color() {return (Color.WHITE);}

    public Audio.CS sfx() {return (UI.InfoMessage.sfx);}

    private void save(Glob glob) {
        Gob gob = glob.oc.getgob(gobid);
        if (gob != null) {
            SavedInfo cell = gob.getattr(SavedInfo.class);
            if (syn || (cell == null))
                gob.setattr(cell = new SavedInfo(gob));
            List<String> lines = new CopyOnWriteArrayList<>(cell.lines);
            if (cell.lines.stream().noneMatch(l -> l.equals(text)))
                lines.add(text);
            cell.lines = lines;

            if (text.startsWith("Quality:")) {
                try {
                    glob.oc.quality(gob, Integer.valueOf(text.substring(8).trim()));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean handle(Widget w) {
        if (w instanceof GameUI)
            save(w.ui.sess.glob);
        return (false);
    }
}
