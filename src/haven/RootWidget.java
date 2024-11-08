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

import haven.sloth.gui.SessionDisplay;
import modification.configuration;
import java.awt.Color;
import java.awt.event.KeyEvent;

public class RootWidget extends ConsoleHost implements UI.MessageWidget {
    public static final Resource defcurs = Resource.remote().loadwait("gfx/hud/curs/arw");
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, UI.scale(14));
    public static final Resource errsfx = Resource.remote().loadwait("sfx/error");
    public static final Resource msgsfx = Resource.remote().loadwait("sfx/msg");
    public boolean modtip = false;
    Profile guprof, grprof, ggprof;
    boolean afk = false;
    private char last_gk;
    private long last_gk_time;
    public SessionDisplay sessionDisplay = new SessionDisplay();
    private Text lastmsg;
    private double msgtime;

    public RootWidget(UI ui, Coord sz) {
        super(ui, new Coord(0, 0), sz);
        setfocusctl(true);
        hasfocus = true;
        cursor = defcurs.indir();
        if (Config.sessiondisplay) {
            add(sessionDisplay);
        }
    }

    @Override
    public boolean globtype(char key, KeyEvent ev) {
        if (!super.globtype(key, ev)) {
            if (key == '`') {
                GameUI gi = findchild(GameUI.class);
                if (Config.profile) {
                    add(new Profwnd(guprof, "UI profile"), new Coord(100, 100));
                    add(new Profwnd(grprof, "GL profile"), new Coord(450, 100));
                    if ((gi != null) && (gi.map != null))
                        add(new Profwnd(gi.map.prof, "Map profile"), new Coord(100, 250));
                }
                if (Config.profilegpu) {
                    add(new Profwnd(ggprof, "GPU profile"), new Coord(450, 250));
                }
            } else if (key == ':') {
                entercmd();
            } else if (key != 0 && (last_gk != key || (System.currentTimeMillis() - last_gk_time) >= 500)) {
                wdgmsg("gk", (int) key);
                last_gk = key;
                last_gk_time = System.currentTimeMillis();
            }
        }
        return (true);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        return super.mousedown(c, button);
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        drawcmd(g, new Coord(20, sz.y - 20));
    }

    public Widget lastfocused;

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (configuration.focusrectangle) {
            for (Widget f = focused; f != null; f = f.focused) {
                if (f.focused == null) {
                    lastfocused = f;
                    break;
                }
            }
        }
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg == "err") {
            ui.error((String) args[0]);
        } else if (msg == "msg") {
            if (args.length == 1) {
                ui.msg((String) args[0]);
            } else {
                Defer.later(() -> {
                    int a = 0;
                    String text = (String) args[a++];
                    Color color = Color.WHITE;
                    if (args[a] instanceof Color)
                        color = (Color) args[a++];
                    if (args.length > a) {
                        Indir<Resource> res = ui.sess.getresv(args[a++]);
                        //info.sfx = (res == null) ? null : Audio.resclip(res.get());
                    }
                    ui.msg(text);
                    return (null);
                });
            }
        } else if (msg == "msg2") {
            Defer.later(() -> {
                Resource res = ui.sess.getresv(args[0]).get();
                //Notice.Factory fac = res.getcode(Notice.Factory.class, true);
                /*ui.msg(fac.format(new OwnerContext() {
                    public <T> T context(Class<T> cl) {
                        return (wdgctx.context(cl, RootWidget.this));
                    }
                }, Utils.splice(args, 1)));*/
                ui.msg(res.toString());
                return (null);
            });
        } else if (msg == "sfx") {
            int a = 0;
            Indir<Resource> resid = ui.sess.getres((Integer) args[a++]);
            double vol = (args.length > a) ? ((Number) args[a++]).doubleValue() : 1.0;
            double spd = (args.length > a) ? ((Number) args[a++]).doubleValue() : 1.0;
            ui.sess.glob.loader.defer(() -> {
                Audio.CS clip = Audio.fromres(resid.get());
                if (spd != 1.0)
                    clip = new Audio.Resampler(clip).sp(spd);
                if (vol != 1.0)
                    clip = new Audio.VolAdjust(clip, vol);
                Audio.play(clip);
            }, null);
        } else if (msg == "bgm") {
            int a = 0;
            Indir<Resource> resid = (args.length > a) ? ui.sess.getres((Integer) args[a++]) : null;
            boolean loop = (args.length > a) ? ((Number) args[a++]).intValue() != 0 : false;
            if (Music.enabled) {
                if (resid == null)
                    Music.play(null, false);
                else
                    Music.play(resid, loop);
            }
        } else {
            super.uimsg(msg, args);
        }
    }

    public void msg(String msg, Color color) {
        lastmsg = msgfoundry.render(msg, color);
        msgtime = Utils.rtime();
    }

    private double lasterrsfx = 0;

    @Override
    public void error(String msg) {
        msg(msg, new Color(192, 0, 0));
        double now = Utils.rtime();
        if (now - lasterrsfx > 0.1) {
            ui.sfx(errsfx);
            lasterrsfx = now;
        }
    }

    private double lastmsgsfx = 0;

    @Override
    public void msg(String msg) {
        msg(msg, Color.WHITE);
        double now = Utils.rtime();
        if (now - lastmsgsfx > 0.1) {
            ui.sfx(msgsfx);
            lastmsgsfx = now;
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (modtip && (ui.modflags() != 0))
            return (KeyMatch.modname(ui.modflags()));
        return (super.tooltip(c, prev));
    }
}
