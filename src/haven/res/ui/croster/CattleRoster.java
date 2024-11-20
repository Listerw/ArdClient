package haven.res.ui.croster;

import haven.Button;
import haven.CharWnd;
import haven.CheckBox;
import haven.Coord;
import haven.FastText;
import haven.GOut;
import haven.GameUI;
import haven.Indir;
import haven.Label;
import haven.Loading;
import haven.MenuGrid;
import haven.MenuGrid.Pagina;
import haven.Resource;
import haven.Scrollbar;
import haven.Tex;
import haven.UI;
import haven.UID;
import haven.Widget;
import haven.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class CattleRoster<T extends Entry> extends Widget {
    public static final int WIDTH = UI.scale(1050);
    public static final Comparator<Entry> namecmp = Comparator.comparing(a -> a.name);
    public static final int HEADH = UI.scale(40);
    public final Map<Long, T> entries = new HashMap<>();
    public final Scrollbar sb;
    public final Widget entrycont;
    public int entryseq = 0;
    public List<T> display = Collections.emptyList();
    public boolean dirty = true;
    public Comparator<? super T> order = namecmp, torder = namecmp;
    public Column mousecol, ordercol, tordercol;
    public Label lcounter;
    public Button selectAllBtn, selectNoneBtn, removeSelectedBtn;

    public CattleRoster() {
        super(new Coord(WIDTH, UI.scale(400)));
        entrycont = add(new Widget(sz), 0, HEADH);
        sb = add(new Scrollbar(sz.y, 0, 0) {
            public void changed() {
                redisplay(display);
            }
        }, sz.x, HEADH);
        selectAllBtn = add(new Button(UI.scale(100), "Select all", false).action(() -> {
            for (Entry entry : this.entries.values())
                entry.set(true);
        }), entrycont.pos("bl").adds(0, 5));
        selectNoneBtn = add(new Button(UI.scale(100), "Select none", false).action(() -> {
            for (Entry entry : this.entries.values())
                entry.set(false);
        }), selectAllBtn.pos("ur").adds(5, 0));
        lcounter = add(new Label(Integer.toString(selectedCounter.get())), selectNoneBtn.pos("ur").adds(5, 0));
        removeSelectedBtn = adda(new Button(UI.scale(150), "Remove selected", false).action(() -> {
            Collection<Object> args = new ArrayList<>();
            for (Entry entry : this.entries.values()) {
                if (entry.mark.a) {
                    args.add(entry.id);
                }
            }
            wdgmsg("rm", args.toArray(new Object[0]));
        }), entrycont.pos("br").adds(0, 5), 1, 0);
        pack();
    }

    public static <E extends Entry> List<Column> initcols(Column... attrs) {
        for (int i = 0, x = CheckBox.sbox.sz().x + UI.scale(10); i < attrs.length; i++) {
            Column attr = attrs[i];
            attr.x = x;
            x += attr.w;
            x += UI.scale(attr.r ? 5 : 1);
        }
        return (Arrays.asList(attrs));
    }

    private final AtomicInteger selectedCounter = new AtomicInteger();

    public void selected(boolean a) {
        if (a) selectedCounter.incrementAndGet();
        else selectedCounter.decrementAndGet();
        lcounter.settext(Integer.toString(selectedCounter.get()));
    }

    public void redisplay(List<T> display) {
        Set<T> hide = new HashSet<>(entries.values());
        int h = 0, th = entrycont.sz.y;
        for (T entry : display)
            h += entry.sz.y;
        sb.max = h - th;
        int y = -sb.val, idx = 0;
        for (T entry : display) {
            entry.idx = idx++;
            if ((y + entry.sz.y > 0) && (y < th)) {
                entry.move(new Coord(0, y));
                entry.show();
            } else {
                entry.hide();
            }
            hide.remove(entry);
            y += entry.sz.y;
        }
        for (T entry : hide)
            entry.hide();
        this.display = display;
    }

    @Override
    public void presize() {
        super.presize();
//        Coord nsz = ((Window)parent).asz;
    }

    @Override
    public void resize(final Coord sz) {
        if (this.sz.equals(sz)) return;
        super.resize(sz);
        int y = 0;
        y += selectAllBtn.sz.y + UI.scale(5) + HEADH;
        Coord nsz = sz.sub(0, y);
        entrycont.resize(nsz);
        sb.resizeh(nsz.y);
        sb.move(Coord.of(nsz.x, sb.c.y));
        sb.reset();
        selectAllBtn.move(entrycont.pos("bl").adds(0, 5));
        selectNoneBtn.move(selectAllBtn.pos("ur").adds(5, 0));
        lcounter.move(selectNoneBtn.pos("ur").adds(5, 0));
        removeSelectedBtn.move(entrycont.pos("br").adds(0, 5), 1, 0);
        redisplay(display);
    }

    public void tick(double dt) {
        if (dirty) {
            List<T> ndisp = new ArrayList<>(entries.values());
            if (order != torder)
                ndisp.sort(torder);
            ndisp.sort(order);
            redisplay(ndisp);
            lcounter.settext(Integer.toString(selectedCounter.get()));
            dirty = false;
        }
        super.tick(dt);
    }

    protected abstract List<Column> cols();

    public void drawcols(GOut g) {
        Column prev = null;
        for (Column col : cols()) {
            if ((prev != null) && !prev.r) {
                g.chcolor(255, 255, 0, 64);
                int x = (prev.x + prev.w + col.x) / 2;
                g.line(new Coord(x, 0), new Coord(x, sz.y), 1);
                g.chcolor();
            }
            if ((col == mousecol) && (col.order != null)) {
                g.chcolor(255, 255, 0, 16);
                g.frect2(new Coord(col.x, 0), new Coord(col.x + col.w, sz.y));
                g.chcolor();
            }
            if (col == ordercol) {
                g.chcolor(255, 255, 0, 16);
                g.frect2(new Coord(col.x, 0), new Coord(col.x + col.w, sz.y));
                g.chcolor();
            }
            if (ordercol != tordercol && col == tordercol) {
                g.chcolor(255, 0, 255, 16);
                g.frect2(new Coord(col.x, 0), new Coord(col.x + col.w, sz.y));
                g.chcolor();
            }
            Tex head = col.head();
            if (col.equals(cols().get(0))) {
                head = CharWnd.attrf.render("Name " + entries.size()).tex();
            }
            g.aimage(head, new Coord(col.x + (col.w / 2), HEADH / 2), 0.5, 0.5);
            prev = col;
        }
    }

    public void draw(GOut g) {
        if (parent != null && parent instanceof Window && !(((Window) parent).minimized()))
            drawcols(g);
        super.draw(g);
    }

    public Column onhead(Coord c) {
        if ((c.y < 0) || (c.y >= HEADH))
            return (null);
        for (Column col : cols()) {
            if ((c.x >= col.x) && (c.x < col.x + col.w))
                return (col);
        }
        return (null);
    }

    public void mousemove(Coord c) {
        super.mousemove(c);
        mousecol = onhead(c);
    }

    public boolean mousedown(Coord c, int button) {
        Column col = onhead(c);
        if (button == 1) {
            if ((col != null) && (col.order != null)) {
                this.order = this.order == col.order ? col.order.reversed() : col.order;
                ordercol = col;
                dirty = true;
                return (true);
            }
        } else if (button == 3) {
            if ((col != null) && (col.order != null)) {
                this.torder = this.torder == col.order ? col.order.reversed() : col.order;
                tordercol = col;
                dirty = true;
                return (true);
            }
        }
        return (super.mousedown(c, button));
    }

    public boolean mousewheel(Coord c, int amount) {
        sb.ch(amount * UI.scale(15));
        return (true);
    }

    public Object tooltip(Coord c, Widget prev) {
        if (mousecol != null)
            return (mousecol.tip);
        return (super.tooltip(c, prev));
    }

    public void addentry(T entry) {
        entries.put(entry.id, entry);
        entrycont.add(entry, Coord.z);
        dirty = true;
        entryseq++;
    }

    public void delentry(long id) {
        T entry = entries.remove(id);
        entry.destroy();
        dirty = true;
        entryseq++;
    }

    public void delentry(T entry) {
        delentry(entry.id);
    }

    public abstract T parse(Object... args);

    public void uimsg(String msg, Object... args) {
        if (msg == "add") {
            addentry(parse(args));
        } else if (msg == "upd") {
            T entry = parse(args);
            delentry(entry.id);
            addentry(entry);
        } else if (msg == "rm") {
            delentry(((Number) args[0]).longValue());
        } else if (msg == "addto") {
            GameUI gui = (GameUI)ui.getwidget((Integer)args[0]);
            Pagina pag = gui.menu.paginafor(ui.sess.getres((Integer)args[1]));
            RosterButton btn = (RosterButton)Loading.waitfor(pag::button);
            btn.add(this);
        } else {
            super.uimsg(msg, args);
        }
    }

    public abstract TypeButton button();

    public static TypeButton typebtn(Indir<Resource> up, Indir<Resource> dn) {
        Resource ur = Loading.waitfor(up);
        Resource.Image ui = ur.layer(Resource.imgc);
        Resource.Image di = Loading.waitfor(dn).layer(Resource.imgc);
        TypeButton ret = new TypeButton(ui.scaled(), di.scaled(), ui.z);
        Resource.Tooltip tip = ur.layer(Resource.tooltip);
        if (tip != null)
            ret.settip(tip.t);
        return (ret);
    }
}

