package modification;

import haven.Config;
import haven.Coord;
import haven.DTarget;
import haven.Equipory;
import haven.GOut;
import haven.UI;
import haven.WItem;
import haven.Widget;
import haven.sloth.gui.MovableWidget;

import java.util.Arrays;

import static haven.Inventory.invsq;

public class newQuickSlotsWdg extends MovableWidget implements DTarget {
    private static final Coord ssz = UI.scale(33, 33);
    private static final Coord spz = UI.scale(4, 33);

    public static class Item {
        int eqslot;

        public Item(int eqslot) {
            this.eqslot = eqslot;
        }
    }

    //public static final Item[] items = new Item[]{new Item(6), new Item(7), new Item(5), new Item(14)};

    //public static final Coord leftCoord = new Coord((ssz.x + spz.x) * 0, 1);
    //public static final Coord rightCoord = new Coord((ssz.x + spz.x) * 1, 1);
    //public static final Coord beltCoord = new Coord((ssz.x + spz.x) * 2, 1);
    //public static final Coord cloakCoord = new Coord((ssz.x + spz.x) * 3, 1);

    public newQuickSlotsWdg() {
        super(new Coord(ssz.x * 4 + spz.x * 3, ssz.y), "NewQuickSlotsWdg");
    }

    @Override
    public void tick(final double dt) {
        Equipory e = null;
        if (ui.gui != null) e = ui.gui.getequipory();
        if (e == null) return;
        long slots = Arrays.stream(e.quicks).filter(Equipory.QuickSlot::state).count();
        resize(new Coord(ssz.x * slots + spz.x * (slots - 1), ssz.y));
    }

    public void draw(GOut g) {
        Equipory e = null;
        if (ui.gui != null) e = ui.gui.getequipory();
        if (e != null) {

            for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                Equipory.QuickSlot item = e.quicks[i];
                if (item.state()) {
                    GOut gi = g.reclip(new Coord((ssz.x + spz.x) * slot + 1, 1).add(1, 1), ssz.sub(1, 1));

                    gi.chcolor(255, 255, 0, 64);
                    gi.frect(Coord.z, ssz);
                    gi.chcolor();

                    gi.aimage(invsq, ssz.div(2), 0.5, 0.5);
                    if (Equipory.ebgs[i] != null)
                        gi.aimage(Equipory.ebgs[i], ssz.div(2), 0.5, 0.5);
                    if (e.quickslots[i] != null) {
                        e.quickslots[i].draw(gi);
                    }
                    slot++;
                }
            }

        }
        super.draw(g);
    }

    public boolean drop(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            int sl = -1;

            for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                Equipory.QuickSlot item = e.quicks[i];
                if (item.state()) {
                    if (cc.x <= (ssz.x + spz.x / 2) * (slot + 1)) {
                        sl = i;
                        break;
                    }
                    slot++;
                }
            }

            if (sl >= 0) {
                e.wdgmsg("drop", sl);
                return true;
            }
        }

        return false;
    }

    public boolean drop(int slot) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            e.wdgmsg("drop", slot);
            return true;
        }

        return false;
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                Equipory.QuickSlot item = e.quicks[i];
                if (item.state()) {
                    if (cc.x <= (ssz.x + spz.x / 2) * (slot + 1)) {
                        w = e.quickslots[i];
                        break;
                    }
                    slot++;
                }
            }
            if (w != null) {
                return w.iteminteract(cc, ul);
            }
        }

        return false;
    }

    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button))
            return true;
        if (ui.modmeta) {
            return true;
        } else if (ui.modctrl && button == 1 && Config.disablequickslotdrop) {
            return true;
        }
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                Equipory.QuickSlot item = e.quicks[i];
                if (item.state()) {
                    if (c.x <= (ssz.x + spz.x / 2) * (slot + 1)) {
                        w = e.quickslots[i];
                        break;
                    }
                    slot++;
                }
            }
            if (w != null) {
                w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), button);
                return true;
            }
        }
        if (altMoveHit(c, button)) {
            if (!isLock()) {
                movableBg = true;
                dm = ui.grabmouse(this);
                doff = c;
                parent.setfocus(this);
                raise();
            }
            return (true);
        } else {
            return (false);
        }
    }

    @Override
    public boolean mousehover(Coord c, boolean b) {
        if (b) {
            Equipory e = ui.gui.getequipory();
            if (e != null) {
                WItem w = null;
                for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                    Equipory.QuickSlot item = e.quicks[i];
                    if (item.state()) {
                        if (c.x <= (ssz.x + spz.x / 2) * (slot + 1)) {
                            w = e.quickslots[i];
                            break;
                        }
                        slot++;
                    }
                }
                if (w != null) {
                    w.mousehover(new Coord(w.sz.x / 2, w.sz.y / 2), b);
                    return (true);
                }
            }
        }
        return (super.mousehover(c, b));
    }

    public void simulateclick(Coord c) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                Equipory.QuickSlot item = e.quicks[i];
                if (item.state()) {
                    if (c.x <= (ssz.x + spz.x / 2) * (slot + 1)) {
                        w = e.quickslots[i];
                        break;
                    }
                    slot++;
                }
            }
            if (w != null) {
                w.item.wdgmsg("take", new Coord(w.sz.x / 2, w.sz.y / 2));
            }
        }
    }

    public void simulateclick(int slot) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = e.quickslots[slot];
            if (w != null) {
                w.item.wdgmsg("take", new Coord(w.sz.x / 2, w.sz.y / 2));
            }
        }
    }

    public Object tooltip(Coord c, Widget prev) {
        Object tt = super.tooltip(c, prev);
        if (tt != null) {
            return tt;
        } else {
            Equipory e = ui.gui.getequipory();
            if (e != null) {
                int sl = -1;

                for (int i = 0, slot = 0; i < e.quicks.length; ++i) {
                    Equipory.QuickSlot item = e.quicks[i];
                    if (item.state()) {
                        if (c.x <= (ssz.x + spz.x / 2) * (slot + 1)) {
                            sl = i;
                            break;
                        }
                        slot++;
                    }
                }

                if (sl >= 0) {
                    WItem it = e.quickslots[sl];
                    if (it != null) {
                        return (it.tooltip(c, prev));
                    } else {
                        return ((Equipory.etts[sl]));
                    }
                }
            }
        }
        return (null);
    }
}
