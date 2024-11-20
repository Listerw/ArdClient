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

import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.sloth.gui.MovableWidget;
import modification.configuration;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMeter extends MovableWidget {
    private static final Resource ponysfx = Resource.local().loadwait("sfx/alarmpony");
    private static final Pattern hppat = Pattern.compile("Health: (\\d+)/(\\d+)/(\\d+)/?(\\d+)?");
    private static final Pattern stampat = Pattern.compile("Stamina: (\\d+)");
    private static final Pattern energypat = Pattern.compile("Energy: (\\d+)");
    static Coord off = UI.scale(22, 7);
    static Coord fsz = UI.scale(101, 24);
    static Coord msz = UI.scale(75, 10);
    static Coord miniOff = UI.scale(0, 5);
    Indir<Resource> bg;
    List<Meter> meters;
    private boolean ponyalarm = true;
    Text meterinfo = null;

    @RName("im")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            Indir<Resource> bg = ui.sess.getres((Integer) args[0]);
            List<Meter> meters = new LinkedList<>();
            for (int i = 1; i < args.length; i += 2) {
                meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
            }
            String name = null;
            while (name == null) {
                if (bg != null)
                    try {
                        name = bg.get().basename();
                    } catch (Exception ignore) {
                    }
            }
            return (new IMeter(bg, meters, "meter-" + name));
        }
    }

    protected float scale = Utils.getpreff("scale-" + this.key, 1f);
    protected int width = Utils.getprefi("width-" + this.key, 0);

    private IMeter(Indir<Resource> bg, List<Meter> meters, final String name) {
        super(fsz.add(configuration.minimalisticmeter ? Utils.getprefi("width-" + name, 0) : 0, 0).mul(Utils.getpreff("scale-" + name, 1f)), name);
        this.bg = bg;
        this.meters = meters;
    }

    public IMeter(Indir<Resource> bg, final String name) {
        this(bg, Collections.emptyList(), name);
    }

    public static class Meter {
        Color c;
        Color ca;
        public int a;

        public Meter(Color c, int a) {
            this.c = c;
            this.ca = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128).darker();
            this.a = a;
        }

        public Meter(double a, Color c) {
            this.a = (int) (a * 100);
            this.ca = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128).darker();
            this.c = c;
        }
    }

    protected Tex tex() {
        return (this.bg.get().layer(Resource.imgc).tex(UI.getScale() * this.scale));
    }

    protected void drawBg(GOut g) {
        boolean mini = configuration.minimalisticmeter;
        g.chcolor(0, 0, 0, 128);
        if (!mini) {
            g.frect(off.mul(this.scale), msz.mul(this.scale));
        } else {
            Coord off = miniOff.mul(this.scale);
            g.frect(off, sz.sub(off.mul(2)));
        }
        g.chcolor();
    }

    protected void drawMeters(GOut g) {
        boolean mini = configuration.minimalisticmeter;
        for (Meter m : meters) {
            int w = msz.x;
            w = (w * m.a) / 100;
            if (!mini) {
                g.chcolor(m.c);
                g.frect(off.mul(this.scale), Coord.of(w, msz.y).mul(this.scale));
            } else {
                g.chcolor(m.ca);
                Coord off = miniOff.mul(this.scale);
                g.frect(off, Coord.of((sz.x * m.a) / 100, sz.y).sub(off.mul(2)));
            }
        }
        g.chcolor();
    }

    public void draw(GOut g) {
        boolean mini = configuration.minimalisticmeter;
        drawBg(g);
        drawMeters(g);
        try {
            if (!mini) {
                g.image(tex(), Coord.z);
            } else {
                g.chcolor(63, 63, 63, 255);
                Coord off = miniOff.mul(this.scale);
                g.rect(off, sz.sub(off.mul(2)));
                g.chcolor();
            }
        } catch (Loading l) {
            //Ignore
        }
        if (Config.showmetertext) {
            Text meterinfo = this.meterinfo;
            if (meterinfo != null)
                g.aimage(meterinfo.tex(), sz.div(2).add(UI.scale(!mini ? 10 : 0) * this.scale, (!mini ? -1 : 0) * this.scale), 0.5, 0.5);
        }
        super.draw(g);
    }

    public void set(List<Meter> meters) {
        this.meters = meters;
    }

    public void set(double a, Color c) {
        set(Collections.singletonList(new Meter(a, c)));
    }

    private static double av(Object arg) {
        if (arg instanceof Integer)
            return (((Integer) arg).doubleValue() * 0.01);
        else
            return (((Number) arg).doubleValue());
    }

    public static List<Meter> decmeters(Object[] args, int s) {
        if (args.length == s)
            return (Collections.emptyList());
        ArrayList<Meter> buf = new ArrayList<>();
        if (args[s] instanceof Number) {
            for (int a = s; a < args.length; a += 2)
                buf.add(new Meter(av(args[a]), (Color) args[a + 1]));
        } else {
            /* XXX: To be considered deprecated, but is was the
             * traditional argument layout of IMeter, so let clients
             * with the newer convention spread before converting the
             * server. */
            for (int a = s; a < args.length; a += 2)
                buf.add(new Meter(av(args[a + 1]), (Color) args[a]));
        }
        buf.trimToSize();
        return (buf);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "set") {
            if (args.length == 1) {
                set(av(args[0]), meters.isEmpty() ? Color.WHITE : meters.get(0).c);
            } else {
                set(decmeters(args, 0));
//                for (int i = 0; i < args.length; i += 2)
//                    meters.add(new Meter((Color) args[i], (Integer) args[i + 1]));
//                this.meters = meters;
            }

            if (ponyalarm) {
                try {
                    Resource res = bg.get();
                    if (res != null && res.name.equals("gfx/hud/meter/häst")) {
                        if (meters.get(0).a <= 10) {
                            Audio.play(ponysfx, 1.0);
                            ponyalarm = false;
                        }
                    }
                } catch (Loading e) {
                }
            }
        } else {
            super.uimsg(msg, args);
            if (msg.equals("tip")) {
                final String tt = (String) args[0];
                Matcher matcher = hppat.matcher(tt);
                String meterinfo = null;
                if (matcher.find()) {
                    if (matcher.group(4) != null) {
                        meterinfo = matcher.group(1);
                    } else {
                        ui.sess.details.shp = Integer.parseInt(matcher.group(1));
                        ui.sess.details.hhp = Integer.parseInt(matcher.group(2));
                        ui.sess.details.mhp = Integer.parseInt(matcher.group(3));
                        if (ui.sess.details.shp < ui.sess.details.hhp && ui.sess.details.hhp < ui.sess.details.mhp)
                            meterinfo = ui.sess.details.shp + "/" + ui.sess.details.hhp + "/" + ui.sess.details.mhp;
                        else if ((ui.sess.details.shp < ui.sess.details.hhp && ui.sess.details.hhp == ui.sess.details.mhp) || (ui.sess.details.shp == ui.sess.details.hhp && ui.sess.details.hhp < ui.sess.details.mhp))
                            meterinfo = ui.sess.details.shp + "/" + ui.sess.details.mhp;
                        else if (ui.sess.details.shp == ui.sess.details.hhp && ui.sess.details.hhp == ui.sess.details.mhp)
                            meterinfo = ui.sess.details.mhp + "";
                    }
                } else {
                    matcher = stampat.matcher(tt);
                    if (matcher.find()) {
                        ui.sess.details.stam = Integer.parseInt(matcher.group(1));
                        meterinfo = ui.sess.details.stam + "%";
                        isStamina = true;
                    } else {
                        matcher = energypat.matcher(tt);
                        if (matcher.find()) {
                            ui.sess.details.energy = Integer.parseInt(matcher.group(1));
                            meterinfo = ui.sess.details.energy + "%";
                        } else {
                            meterinfo = tt;
                            if (meterinfo.contains("ow")) {
                                meterinfo = tt.split(" ")[2];
                            } else {
                                meterinfo = tt.split(" ")[1];
                            }
                            if (meterinfo.contains("/")) {
                                String[] hps = meterinfo.split("/");
                                meterinfo = hps[0] + "/" + hps[hps.length - 1];
                            }
                        }
                    }
                }
                if (meterinfo == null)
                    meterinfo = tt;
                updatemeterinfo(meterinfo);
            }
        }
    }

    private static final RichText.Foundry fnd = new RichText.Foundry(TextAttribute.FAMILY, "Dialog", TextAttribute.SIZE, UI.scale(10));

    @Override
    public boolean mousedown(final Coord mc, final int button) {
        if (super.mousedown(mc, button)) {
            return (true);
        } else if (altMoveHit(mc, button)) {
            if (!isLock()) {
                movableBg = true;
                dm = ui.grabmouse(this);
                doff = mc;
                parent.setfocus(this);
                raise();
            }
            return (true);
        } else {
            return (false);
        }
    }

    public boolean mousewheel(Coord coord, int amount) {
        if (ui.modflags() == (UI.MOD_CTRL | UI.MOD_META)) {
            float scale = Math.max(Math.min(this.scale - (0.1f * amount), 5f), 1f);
            float lastscale = this.scale;
            if (lastscale != scale) {
                Utils.setpreff("scale-" + this.key, this.scale = scale);

                resize(fsz.add(width, 0).mul(scale));
                move(c.sub(sz.sub(fsz.add(width, 0).mul(lastscale)).div(2)));
                dirtyText = true;
            }
            return (true);
        } else if (ui.modflags() == UI.MOD_CTRL) {
            int width = Math.max(Math.min(this.width - (1 * amount), 200), 0);
            int lastwidth = this.width;
            if (lastwidth != width) {
                Utils.setprefi("width-" + this.key, this.width = width);

                Coord center = c.add(fsz.add(lastwidth, 0).mul(scale).div(2));
                resize(fsz.add(width, 0).mul(scale));
                move(center.sub(sz.div(2)));
                dirtyText = true;
            }
            return (true);
        }
        return (super.mousewheel(c, amount));
    }

    boolean isStamina = false;
    boolean isDrink = false;
    boolean dirtyText = false;
    String drawText = null;
    static final String drinkSuf = " (Drinking)";

    @Override
    public void tick(final double dt) {
        String drawText = this.drawText;
        if (drawText != null) {
            if (isStamina) {
                PBotGob player = PBotGobAPI.player(ui);
                if (player.getPoses().stream().anyMatch(s -> s.contains("drink"))) {
                    if (!isDrink) {
                        isDrink = true;
                        dirtyText = true;
                    }
                } else {
                    if (isDrink) {
                        isDrink = false;
                        dirtyText = true;
                    }
                }
            }

            if (dirtyText) {
                dirtyText = false;
                if (isDrink)
                    drawText = drawText.concat(drinkSuf);
                this.meterinfo = Text.create(drawText, PUtils.strokeImg(fnd.render(drawText, -1, TextAttribute.SIZE, UI.scale(10) * this.scale)));
            }
        }
    }

    protected void updatemeterinfo(String str) {
        if (str == null || str.isEmpty()) return;
        String drawText = this.drawText;
        if (drawText == null || !drawText.equals(str)) {
            this.drawText = str;
            dirtyText = true;
        }
    }
}
