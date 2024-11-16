/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import modification.configuration;
import modification.dev;

import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;


public class UI {
    public RootWidget root;
    public static int MOD_SHIFT = 1, MOD_CTRL = 2, MOD_META = 4, MOD_SUPER = 8;
    protected final LinkedList<Grab> keygrab = new LinkedList<>(), mousegrab = new LinkedList<>();
    public final Map<Integer, Widget> widgets = new TreeMap<>();
    public final Map<Widget, Integer> rwidgets = new HashMap<>();
    Receiver rcvr;
    public Coord mc = Coord.z, lcc = Coord.z;
    public Session sess;
    public boolean modshift, modctrl, modmeta, modsuper;
    public int keycode;
    public boolean readytodrop = false;
    public Object lasttip;
    double lastevent, lasttick;
    public Widget mouseon;
    public Console cons = new WidgetConsole();
    private final Collection<AfterDraw> afterdraws = new LinkedList<>();
    public int beltWndId = -2;
    public GameUI gui;
    public WeakReference<Widget> realmchat;
    public WeakReference<FightWnd> fightwnd;
    private final Context uictx;
    public ActAudio audio = new ActAudio();
    public Charlist charlist;
    private static double scalef = 1.0;

    {
        lastevent = lasttick = Utils.rtime();
    }

    public interface Receiver {
        public void rcvmsg(int widget, String msg, Object... args);
    }

    public interface Runner {
        public Session run(UI ui) throws InterruptedException;

        public default void init(UI ui) {
        }

        public default String title() {
            return (null);
        }

        public static class Proxy implements Runner {
            public final Runner back;

            public Proxy(Runner back) {
                this.back = back;
            }

            public Session run(UI ui) throws InterruptedException {
                return (back.run(ui));
            }

            public void init(UI ui) {
                back.init(ui);
            }

            public String title() {
                return (back.title());
            }
        }
    }

    public interface Context {
        void setmousepos(Coord c);
    }


    public interface AfterDraw {
        public void draw(GOut g);
    }

    private class WidgetConsole extends Console {
        {
            setcmd("q", (cons1, args) -> HackThread.tg().interrupt());
            setcmd("lo", (cons1, args) -> sess.close());
            setcmd("kbd", (cons1, args) -> {
                Config.zkey = args[1].toString().equals("z") ? KeyEvent.VK_Y : KeyEvent.VK_Z;
                Utils.setprefi("zkey", Config.zkey);
            });
        }

        private void findcmds(Map<String, Command> map, Widget wdg) {
            if (wdg instanceof Directory) {
                Map<String, Command> cmds = ((Directory) wdg).findcmds();
                synchronized (cmds) {
                    map.putAll(cmds);
                }
            }
            for (Widget ch = wdg.child; ch != null; ch = ch.next)
                findcmds(map, ch);
        }

        public Map<String, Command> findcmds() {
            Map<String, Command> ret = super.findcmds();
            findcmds(ret, root);
            return (ret);
        }
    }

    public static class UIException extends RuntimeException {
        public String mname;
        public Object[] args;

        public UIException(String message, String mname, Object... args) {
            super(message);
            this.mname = mname;
            this.args = args;
        }
    }

    public static class UIWarning extends Warning {
        public String mname;
        public Object[] args;

        public UIWarning(String message, String mname, Object... args) {
            super(message);
            this.mname = mname;
            this.args = args;
        }
    }

    public UI(Context uictx, Coord sz, Session sess) {
        this.uictx = uictx;
        root = new RootWidget(this, sz);
        widgets.put(0, root);
        rwidgets.put(root, 0);
        setSession(sess);
//        PBotAPI.ui = this;
    }

    public UI(Context uictx, Coord sz) {
        this(uictx, sz, null);
    }

    public void setSession(final Session sess) {
        this.sess = sess;
        if (this.sess != null)
            this.sess.glob.ui = new WeakReference<>(this);
    }

    public void reset(final Coord sz) {
        final RootWidget oldroot = root;
        destroy();
        root = new RootWidget(this, sz);
        widgets.put(0, root);
        rwidgets.put(root, 0);
        root.ggprof = oldroot.ggprof;
        root.grprof = oldroot.grprof;
        root.guprof = oldroot.guprof;
        audio = new ActAudio();
    }

    public void setreceiver(Receiver rcvr) {
        this.rcvr = rcvr;
    }

    public void bind(Widget w, int id) {
        widgets.put(id, w);
        rwidgets.put(w, id);
        w.binded();
    }

    public Widget getwidget(int id) {
        synchronized (widgets) {
            return (widgets.get(id));
        }
    }

    public int widgetid(Widget wdg) {
        synchronized (widgets) {
            Integer id = rwidgets.get(wdg);
            if (id == null)
                return (-1);
            return (id);
        }
    }

    public void drawafter(AfterDraw ad) {
        synchronized (afterdraws) {
            afterdraws.add(ad);
        }
    }

    public void tick() {
        double now = Utils.rtime();
        root.tick(now - lasttick);
        lasttick = now;
    }

    public void draw(GOut g) {
        try {
            root.draw(g);
            synchronized (afterdraws) {
                for (AfterDraw ad : afterdraws)
                    ad.draw(g);
                afterdraws.clear();
            }
        } catch (Exception e) {
            dev.simpleLog(e);
        }
    }

    //ids go sequential, 2^16 limit judging by parent != 65535...
    //At 65535 wrap back around? Can we break the game by hitting that limit.............
    public int next_predicted_id = 2;

    public void newwidget(int id, String type, int parent, Object[] pargs, Object... cargs) throws InterruptedException {
        if (Config.quickbelt && type.equals("inv") && pargs.length > 1 && pargs[1].equals("Belt")) {
            //type = "alt-wnd-belt";
            beltWndId = parent;
        }
        if (type.equals("inv") && pargs[0].toString().equals("study")) {
            type = "inv-study";
        }

        if (type.startsWith("ui/province")) {
            if (gui != null) gui.notifyProvince(cargs);
            return;
        }

        Widget.Factory f;
        try {
            if (parent == beltWndId)
                f = Widget.gettype2("inv-belt");
    //        else if (type.startsWith("gfx/hud/rosters/"))
    //            f = Widget.gettype3(type);
            else
                f = Widget.gettype2(type);
            if (f == null) {
                dev.resourceLog("Bad widget name", type, cargs);
                return;
            }
        } catch (Loading e) {
            throw (e);
        } catch (Throwable e) {
            dev.simpleLog(e);
            f = new Widget.Factory() {
                @Override
                public Widget create(final UI ui, final Object[] par) {
                    return (new Widget());
                }
            };
        }
        synchronized (this) {
            Widget wdg;
            if (type.startsWith("ui/maillist")) {
                wdg = modification.Maillist.mkwidget(this, cargs);
            } else {
                try {
                    wdg = f.create(this, cargs);
                } catch (Loading e) {
                    throw (e);
                } catch (Throwable e) {
                    dev.simpleLog(e);
                    wdg = new Widget();
                }
            }
            wdg.attach(this);
            if (parent != -1) {
                Widget pwdg = widgets.get(parent);
                if (pwdg == null) {
                    dev.resourceLog("Null parent widget newwidget", parent, id, type, pargs, cargs);
//                        throw (new UIException("Null parent widget " + parent + " for " + id, type, cargs));
                    return;
                }
                //fix for calf info
                if (pargs.length > 0 && pargs[0].equals("!w_px^oy5S+c") && cargs.length > 0 && cargs[0].toString().contains("-- With")) {
                    pargs[0] = new Coord(0, 72);
                }
                if (type.equals("btn") && cargs.length > 1 && cargs[1].equals("Yes") && pwdg instanceof Window && ((Window) pwdg).origcap.equals("Invitation")) {
                    if (gui != null && gui.buddies != null) {
                        Runnable run = ((Button) wdg).action;
                        ((Button) wdg).action = () -> {
                            String name = gui.buddies.getCharName();
                            if (gui.ui.modflags() == UI.MOD_CTRL) {
                                gui.buddies.setpname(configuration.randomNick());
                            }
                            run.run();
                            if (gui.ui.modflags() == UI.MOD_CTRL) {
                                if (name != null)
                                    gui.buddies.setpname(name);
                            }
                        };
                        wdg.tooltip = Text.render("Random name with CTRL").tex();
                    }
                }
                if (parent == beltWndId) {
                    Widget.Factory fwnd = Widget.gettype2("alt-wnd-belt");
                    Widget wnd = fwnd.create(this, pargs);
                    GameUI gui = root.findchild(GameUI.class);
                    if (gui != null) {
                        pwdg = wnd;
                        wnd.attach(this);
                        gui.add(wnd);
                        wnd.add(wdg);
                        wnd.pack();
                    }
                } else
                    pwdg.addchild(wdg, pargs);

                if (pwdg instanceof Window && gui != null)
                    processWindowContent(parent, gui, (Window) pwdg, wdg);
            } else {
                if (wdg instanceof Window && gui != null)
                    processWindowCreation(id, gui, (Window) wdg);
            }
            if (type.equals("wnd") && cargs[1].equals("Invitation")) {
                if (wdg instanceof Window && gui != null && gui.buddies != null && gui.buddies.nextrandomnameinv) {
                    gui.buddies.nextrandomnameinv = false;
                    String name = gui.buddies.getCharName();
                    gui.buddies.setpname(configuration.randomNick());
                    ((Window) wdg).setDestroyHook(() -> {
                        if (name != null)
                            gui.buddies.setpname(name);
                    });
                }
            }
            bind(wdg, id);
            if (type.contains("rchan"))
                realmchat = new WeakReference<>(wdg);
            if (wdg instanceof FightWnd) {
                fightwnd = new WeakReference<>((FightWnd) wdg);
            }
            dev.sysLogRemote("newwidget", wdg, id, type, parent, pargs, cargs);
        }
        next_predicted_id = id + 1;
    }

    public void addwidget(int id, int parent, Object[] pargs) {
        synchronized (this) {
            Widget wdg = widgets.get(id);
            if (wdg == null) {
                dev.resourceLog("Null child widget addwidget", id, parent, pargs);
//                    throw (new UIException("Null child widget " + id + " added to " + parent, null, pargs));
                return;
            }
            Widget pwdg = widgets.get(parent);
            if (pwdg == null) {
                dev.resourceLog("Null parent widget addwidget", parent, id, pargs);
//                    throw (new UIException("Null parent widget " + parent + " for " + id, null, pargs));
                return;
            }
            pwdg.addchild(wdg, pargs);
            dev.sysLogRemote("addwidget", wdg, id, null, parent, pargs, (Object) null);
        }
    }

    private void processWindowContent(long wndid, GameUI gui, Window pwdg, Widget wdg) {
        String cap = pwdg.origcap;
        if (gui != null && gui.livestockwnd().pendingAnimal != null && gui.livestockwnd().pendingAnimal.wndid == wndid) {
            if (wdg instanceof TextEntry)
                gui.livestockwnd().applyName(wdg);
            else if (wdg instanceof Label)
                gui.livestockwnd().applyAttr(cap, wdg);
            else if (wdg instanceof ProxyFrame && ((ProxyFrame)wdg).ch instanceof Avaview)
                gui.livestockwnd().applyId(((ProxyFrame)wdg).ch);
        } else if (wdg instanceof ISBox && cap.equals("Stockpile")) {
            TextEntry entry = new TextEntry(40, "") {
                String backup = text();

                @Override
                public boolean keydown(KeyEvent ev) {
//                    char c = ev.getKeyChar();
//                    if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9 && buf.line().length() < 2 || c == '\b') {
//                        return (buf.key(ev));
//                    } else if (c == '\n') {
//                        try {
//                            int count = Integer.parseInt(dtext());
//                            for (int i = 0; i < count; i++)
//                                wdg.wdgmsg("xfer");
//                            return (true);
//                        } catch (NumberFormatException e) {
//                        }
//                    }
//                    return (false);

                    if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
                        try {
                            int count = Integer.parseInt(text());
                            for (int i = 0; i < count; i++)
                                wdg.wdgmsg("xfer");
                            return (true);
                        } catch (NumberFormatException ex) {
                        }
                    }
                    backup = text();
                    boolean b = super.keydown(ev);
                    try {
                        if (!text().isEmpty())
                            Integer.parseInt(text());
                        if (text().length() > 2)
                            settext(backup);
                    } catch (Exception ex) {
                        settext(backup);
                    }
                    return (b);
                }
            };
            Button btn = new Button(65, "Take") {
                @Override
                public void click() {
                    try {
                        String cs = entry.dtext();
                        int count = cs.isEmpty() ? 1 : Integer.parseInt(cs);
                        for (int i = 0; i < count; i++)
                            wdg.wdgmsg("xfer");
                    } catch (NumberFormatException e) {
                    }
                }
            };
            pwdg.add(btn, new Coord(0, wdg.sz.y + 5));
            pwdg.add(entry, new Coord(btn.sz.x + 5, wdg.sz.y + 5 + 2));
        }
    }

    private void processWindowCreation(long wdgid, GameUI gui, Window wdg) {
        String cap = wdg.origcap;
        if (cap.equals("Charter Stone") || cap.equals("Sublime Portico")) {
            // show secrets list only for already built chartes/porticos
//            if (wdg.wsz.y >= 50) {
//                wdg.adda(new Button(UI.scale(50), "Load old") {
//                    public void click() {
//                        String[] charters = Utils.getprefsa("charters", new String[0]);
//                        for (String charter : charters) {
//                            wdg.uimsg("add", charter);
//                        }
//                        hide();
//                    }
//
//                    public Object tooltip(Coord c0, Widget prev) {
//                        return Text.render("Please update your charters").tex();
//                    }
//                }, new Coord(wdg.asz.x, 0), 1, 0);
//                //wdg.add(new CharterList(150, 20), new Coord(0, 50));
//                wdg.presize();
//            }
        } else if (gui != null && gui.livestockwnd() != null && gui.livestockwnd().getAnimalPanel(cap) != null) {
            gui.livestockwnd().initPendingAnimal(wdgid, cap);
        }
    }

    public abstract class Grab {
        public final Widget wdg;

        public Grab(Widget wdg) {
            this.wdg = wdg;
        }

        public abstract void remove();
    }

    public Grab grabmouse(Widget wdg) {
        if (wdg == null) throw (new NullPointerException());
        Grab g = new Grab(wdg) {
            public void remove() {
                mousegrab.remove(this);
            }
        };
        mousegrab.addFirst(g);
        return (g);
    }

    public Grab grabkeys(Widget wdg) {
        if (wdg == null) throw (new NullPointerException());
        Grab g = new Grab(wdg) {
            public void remove() {
                keygrab.remove(this);
            }
        };
        keygrab.addFirst(g);
        return (g);
    }

    private void removeid(Widget wdg) {
        wdg.removed();
        synchronized (rwidgets) {
            if (rwidgets.containsKey(wdg)) {
                int id = rwidgets.get(wdg);
                widgets.remove(id);
                rwidgets.remove(wdg);
            }
            for (Widget child = wdg.child; child != null; child = child.next)
                removeid(child);
        }
    }

    public void removed(Widget wdg) {
        mousegrab.removeIf(g -> g.wdg.hasparent(wdg));
        keygrab.removeIf(g -> g.wdg.hasparent(wdg));
    }

    public void destroy(Widget wdg) {
//        mousegrab.removeIf(g -> g.wdg.hasparent(wdg));
//        keygrab.removeIf(g -> g.wdg.hasparent(wdg));
        if (wdg != null) {
            removeid(wdg);
            wdg.reqdestroy();
        }
    }

    public void destroy(int id) {
        synchronized (this) {
            if (widgets.containsKey(id)) {
                Widget wdg = widgets.get(id);
                destroy(wdg);
                dev.sysLogRemote("destroy", wdg, id, null, -1, null, (Object) null);
            }
        }
    }

    /**
     * For scripting only
     */
    public void wdgmsg(final int id, final String msg, Object... args) {
        if (rcvr != null)
            rcvr.rcvmsg(id, msg, args);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        int id;
        if (msg.endsWith("-identical"))
            return;

        synchronized (this) {
            if (!rwidgets.containsKey(sender)) {
                if (msg.equals("close")) {
                    sender.reqdestroy();
                    return;
                } else
                    System.err.printf("Wdgmsg sender (%s) is not in rwidgets, message is %s\n", sender.getClass().getName(), msg);
                return;
            }
            id = rwidgets.get(sender);
            if (rcvr != null)
                rcvr.rcvmsg(id, msg, args);
        }
        dev.sysLog(dev.clientSender, sender, id, msg, args);
    }

    public void uimsg(int id, String msg, Object... args) {
        Widget wdg;
        synchronized (widgets) {
            wdg = widgets.get(id);
        }
        if (realmchat != null) {
            try {
                if (realmchat.get() != null && id == realmchat.get().wdgid()) {
                    if (msg.contains("msg") && wdg.toString().contains("Realm")) {
                        ((ChatUI.EntryChannel) realmchat.get()).updurgency(1);
                        if (Config.realmchatalerts)
                            Audio.play(ChatUI.notifsfx);
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        if (wdg != null) {
            wdg.uimsg(msg.intern(), args);
        } else {
            dev.resourceLog("Uimsg to non-existent widget ", id);
            return;
        }
        dev.sysLog(dev.serverSender, wdg, id, msg, args);
    }

    public interface MessageWidget {
        void msg(String msg);

        void error(String msg);

        static MessageWidget find(Widget w) {
            for (Widget ch = w.child; ch != null; ch = ch.next) {
                MessageWidget ret = find(ch);
                if (ret != null)
                    return (ret);
            }
            if (w instanceof MessageWidget) return ((MessageWidget) w);
            return (null);
        }
    }

    public void error(String msg) {
        MessageWidget h = MessageWidget.find(root);
        if (h != null)
            h.error(msg);
    }

    public void msg(String msg) {
        MessageWidget h = MessageWidget.find(root);
        if (h != null)
            h.msg(msg);
    }

    private void setmods(InputEvent ev) {
        int mod = ev.getModifiersEx();
        Debug.kf1 = modshift = (mod & InputEvent.SHIFT_DOWN_MASK) != 0;
        Debug.kf2 = modctrl = (mod & InputEvent.CTRL_DOWN_MASK) != 0;
        Debug.kf3 = modmeta = (mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0;
    /*
    Debug.kf4 = modsuper = (mod & InputEvent.SUPER_DOWN_MASK) != 0;
	*/
    }

    private Grab[] c(Collection<Grab> g) {
        return (g.toArray(new Grab[0]));
    }

    public void type(KeyEvent ev) {
        try {
            setmods(ev);
            for (Grab g : c(keygrab)) {
                //Make sure this wdg is visible the entire way up
                if (g.wdg.tvisible()) {
                    if (g.wdg.type(ev.getKeyChar(), ev))
                        return;
                }
            }
//            if (!root.type(ev.getKeyChar(), ev))
//                root.globtype(ev.getKeyChar(), ev);
            root.type(ev.getKeyChar(), ev);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void keydown(KeyEvent ev) {
        try {
            setmods(ev);
            keycode = ev.getKeyCode();
            for (Grab g : c(keygrab)) {
                //Make sure this wdg is visible the entire way up
                if (g.wdg.tvisible()) {
                    if (g.wdg.keydown(ev))
                        return;
                }
            }
            if (!root.keydown(ev))
                root.globtype(ev.getKeyChar(), ev);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void keyup(KeyEvent ev) {
        try {
            setmods(ev);
            keycode = -1;
            for (Grab g : c(keygrab)) {
                //Make sure this wdg is visible the entire way up
                if (g.wdg.tvisible()) {
                    if (g.wdg.keyup(ev))
                        return;
                }
            }
            root.keyup(ev);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Coord wdgxlate(Coord c, Widget wdg) {
        return (c.sub(wdg.rootpos()));
    }

    public boolean dropthing(Widget w, Coord c, Object thing) {
        if (w instanceof DropTarget) {
            if (((DropTarget) w).dropthing(c, thing))
                return (true);
        }
        for (Widget wdg = w.lchild; wdg != null; wdg = wdg.prev) {
            Coord cc = w.xlate(wdg.c, true);
            if (c.isect(cc, wdg.sz)) {
                if (dropthing(wdg, c.add(cc.inv()), thing))
                    return (true);
            }
        }
        return (false);
    }
    //  public void mousedown(Coord c, int button){
    //     mousedown(new MouseEvent(panel, 0, 0, 0, c.x, c.y, 1, false, button), c, button);
    // }

    public void mousedown(MouseEvent ev, Coord c, int button) {
        setmods(ev);
        lcc = mc = c;
        for (Grab g : c(mousegrab)) {
            //Make sure this wdg is visible the entire way up
            if (g.wdg.tvisible()) {
                if (g.wdg.mousedown(wdgxlate(c, g.wdg), button))
                    return;
            }
        }
        root.mousedown(c, button);
    }

    public void mouseup(MouseEvent ev, Coord c, int button) {
        setmods(ev);
        mc = c;
        for (Grab g : c(mousegrab)) {
            //Make sure this wdg is visible the entire way up
            if (g.wdg.tvisible()) {
                if (g.wdg.mouseup(wdgxlate(c, g.wdg), button))
                    return;
            }
        }
        root.mouseup(c, button);
    }

    public void mousemove(MouseEvent ev, Coord c) {
        setmods(ev);
        mc = c;
        root.mousemove(c);
    }

    public void mousehover(Coord c) {
	    root.mousehover(c, true);
    }

    public void setmousepos(Coord c) {
        uictx.setmousepos(c);
    }


    public void mousewheel(MouseEvent ev, Coord c, int amount) {
        setmods(ev);
        lcc = mc = c;
        for (Grab g : c(mousegrab)) {
            if (g.wdg.tvisible()) {
                if (g.wdg.mousewheel(wdgxlate(c, g.wdg), amount))
                    return;
            }
        }
        root.mousewheel(c, amount);
    }

    public Resource getcurs(Coord c) {
        // should synchronize instead, but we are not looking for proper ways here!
        // thus, just iterate over an array copy to avoid concurrent modification exception
        Grab[] mousegrabCopy = mousegrab.toArray(new Grab[mousegrab.size()]);
        for (Grab g : mousegrabCopy) {
            if (g == null || g.wdg == null)
                continue;
            Resource ret = g.wdg.getcurs(wdgxlate(c, g.wdg));
            if (ret != null)
                return (ret);
        }
        return (root.getcurs(c));
    }

    public static int modflags(InputEvent ev) {
        int mod = ev.getModifiersEx();
        return ((((mod & InputEvent.SHIFT_DOWN_MASK) != 0) ? MOD_SHIFT : 0) |
                (((mod & InputEvent.CTRL_DOWN_MASK) != 0) ? MOD_CTRL : 0) |
                (((mod & (InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK)) != 0) ? MOD_META : 0)
                /* (((mod & InputEvent.SUPER_DOWN_MASK) != 0) ? MOD_SUPER : 0) */);
    }

    public int modflags() {
        return ((modshift ? MOD_SHIFT : 0) |
                (modctrl ? MOD_CTRL : 0) |
                (modmeta ? MOD_META : 0) |
                (modsuper ? MOD_SUPER : 0));
    }


    public void destroy() {
        audio.clear();
        destroy(root);
        if (cons.out != null) {
            Debug.remove(cons.out);
            cons.out.close();
            cons.out = null;
        }
    }

    public void sfx(Audio.CS clip) {
        audio.aui.add(clip);
    }

    public void sfx(Resource clip) {
        sfx(Audio.fromres(clip));
    }

    public static double scale(double v) {
        return (v * scalef);
    }

    public static float scale(float v) {
        return (v * (float) scalef);
    }

    public static int scale(int v) {
        return (Math.round(scale((float) v)));
    }

    public static int rscale(double v) {
        return ((int) Math.round(v * scalef));
    }

    public static Coord scale(Coord v) {
        return (v.mul(scalef));
    }

    public static Coord scale(int x, int y) {
        return (scale(new Coord(x, y)));
    }

    public static Coord rscale(double x, double y) {
        return (new Coord(rscale(x), rscale(y)));
    }

    public static Coord2d scale(Coord2d v) {
        return (v.mul(scalef));
    }

    static public Font scale(Font f, float size) {
        return (f.deriveFont(scale(size)));
    }

//    public static <T extends Tex> ScaledTex<T> scale(T tex) {
//        return (new ScaledTex<T>(tex, UI.scale(tex.sz())));
//    }

//    public static <T extends Tex> ScaledTex<T> scale(ScaledTex<T> tex) {
//        return (tex);
//    }

    public static double unscale(double v) {
        return (v / scalef);
    }

    public static float unscale(float v) {
        return (v / (float) scalef);
    }

    public static int unscale(int v) {
        return (Math.round(unscale((float) v)));
    }

    public static Coord unscale(Coord v) {
        return (v.div(scalef));
    }


    public long time = System.currentTimeMillis();
    public static long timewait = Utils.getprefi("uitickwaittime", 500);
    public static boolean canwait = Utils.getprefb("uitickwait", false);


    private static double maxscale = -1;

    public static double getScale() {
        return (scalef);
    }

    public static void updateScale() {
        scalef = loadscale();
    }

    public static double maxscale() {
        synchronized (UI.class) {
            if (maxscale < 0) {
                double fscale = 1.25;
                try {
                    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    for (GraphicsDevice dev : env.getScreenDevices()) {
                        DisplayMode mode = dev.getDisplayMode();
                        double scale = Math.min(mode.getWidth() / 800.0, mode.getHeight() / 600.0);
                        fscale = Math.max(fscale, scale);
                    }
                } catch (Exception exc) {
                    new Warning(exc, "could not determine maximum scaling factor").issue();
                }
                maxscale = fscale;
            }
            return (maxscale);
        }
    }

    private static double loadscale() {
//        if (Config.uiscale != null)
//            return (Config.uiscale);
        double scale = Utils.getprefd("uiscale", 1.0);
        scale = Math.max(Math.min(scale, maxscale()), 1.0);
        return (scale);
    }

    static {
        updateScale();
    }
}