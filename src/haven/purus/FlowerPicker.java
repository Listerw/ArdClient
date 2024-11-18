package haven.purus;

import haven.FlowerMenu;
import haven.GameUI;
import haven.Inventory;
import haven.WItem;
import haven.Widget;
import haven.automation.GobSelectCallback;
import haven.automation.WItemDestroyCallback;
import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.util.ArrayList;

public class FlowerPicker implements Runnable, ItemClickCallback, WItemDestroyCallback, PetalClickCallback {

    private GameUI gui;
    private WItem item;
    private FlowerMenu.Petal petal;
    private boolean itemDestroyed = false;

    public FlowerPicker(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        PBotUtils.debugMsg(gui.ui, "Click an item in inventory");
        synchronized (GobSelectCallback.class) {
            gui.registerItemCallback(this);
        }
        String itemName = null;
        for (int retries = 0, time = 5000, sleep = 5; retries < time / sleep; retries++) {
            WItem item = this.item;
            if (item != null) {
                String name = item.item.getname();
                if (!name.isEmpty()) {
                    itemName = name;
                    break;
                }
            }
            PBotUtils.sleep(sleep);
        }
        if (itemName == null) {
            PBotUtils.debugMsg(gui.ui, "FlowerPicker timeout!", Color.RED);
            synchronized (GobSelectCallback.class) {
                gui.unregisterItemCallback();
            }
            return;
        }
        ArrayList<WItem> itmList = new ArrayList<>();
        synchronized (gui.ui) {
            for (Widget w : gui.ui.widgets.values()) {
                if (w instanceof Inventory) {
                    for (Widget witm = w.child; witm != null; witm = witm.next) {
                        synchronized (witm) {
                            if (witm instanceof WItem && ((WItem) witm).item.getname().equals(itemName)) {
                                itmList.add((WItem) witm);
                            }
                        }
                    }
                }
            }
        }
        FlowerMenu.Petal target = null;
        for (WItem itm : itmList) {
            if (PBotUtils.petalExists(gui.ui)) PBotUtils.closeFlowermenu(gui.ui, 500);
            if (itm.parent != null && ((Inventory) itm.parent).wmap.containsKey(itm.item)) {
                if (target == null) {
                    synchronized (PetalClickCallback.class) {
                        gui.registerPetalCallback(this);
                    }
                    itm.item.wdgmsg("iact", itm.c, 0);
                    if (PBotUtils.waitForFlowerMenu(gui.ui, 1000)) {
                        PBotUtils.debugMsg(gui.ui, "Choose your petal");
                        for (int retries = 0, time = 5000, sleep = 5; retries < time / sleep; retries++) {
                            FlowerMenu.Petal petal1 = this.petal;
                            if (petal1 != null) {
                                target = petal1;
                                break;
                            }
                            PBotUtils.sleep(sleep);
                        }
                        if (petal == null) {
                            synchronized (PetalClickCallback.class) {
                                gui.unregisterPetalCallback();
                            }
                            break;
                        }
                    } else {
                        synchronized (PetalClickCallback.class) {
                            gui.unregisterPetalCallback();
                        }
                        continue;
                    }
                }
                if (target != null) {
//                    FlowerMenu.setNextSelection(target.name);
                    itm.item.wdgmsg("iact", itm.c, 0);
                    if (PBotUtils.waitForFlowerMenu(gui.ui, 1000)) {
                        if (PBotUtils.choosePetal(gui.ui, target.name)) {//fixme what to do with autoselect?
                            PBotUtils.waitFlowermenuClose(gui.ui, 500);
                        } else {
                            PBotUtils.closeFlowermenu(gui.ui, 500);
                        }
                    }
                }
            }
        }
        if (petal == null) {
            PBotUtils.debugMsg(gui.ui, "FlowerPicker petal timeout!", Color.RED);
            return;
        }
        PBotUtils.debugMsg(gui.ui, "FlowerPicker done!", Color.GREEN);
    }

    @Override
    public void itemClick(WItem item) {
        synchronized (ItemClickCallback.class) {
            gui.unregisterItemCallback();
        }
        this.item = item;

    }

    @Override
    public void notifyDestroy() {
        itemDestroyed = true;
    }

    @Override
    public void petalClick(final FlowerMenu.Petal petal) {
        synchronized (PetalClickCallback.class) {
            gui.unregisterPetalCallback();
        }
        this.petal = petal;
        PBotUtils.debugMsg(gui.ui, "Petal: " + petal.name);
    }
}
