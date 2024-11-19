package haven.purus;

import haven.AltBeltWnd;
import haven.Equipory;
import haven.GameUI;
import haven.Inventory;
import haven.ItemInfo;
import haven.Loading;
import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotUtils;
import java.awt.Color;
import java.util.regex.Pattern;
import modification.configuration;

public class DrinkWater implements Runnable {

    GameUI gui;
    final String liquid;

    public DrinkWater(GameUI gui) {
        this.gui = gui;
        liquid = null;
    }

    public DrinkWater(GameUI gui, String liquid) {
        this.gui = gui;
        this.liquid = liquid;
    }

    @Override
    public void run() {
        drink();
    }

    private void drink() {
        try {
            // Don't attempt to drink if flower menu is already open or we are already drinking
            if (gui.drinkingWater) {
                return;
            }
            gui.drinkingWater = true;
            WItem drinkFromThis = null;
            Equipory e = gui.getequipory();
            for (WItem i : e.quickslots) {
                if (canDrinkFrom(i)) {
                    drinkFromThis = i;
                    break;
                }
            }
            if (drinkFromThis == null) {
                for (Widget w = gui.lchild; w != null; w = w.prev) {
                    if (drinkFromThis != null) break;
                    if (w instanceof Window) {
                        Window wnd = (Window) w;
                        for (Widget wdg = wnd.lchild; wdg != null; wdg = wdg.prev) {
                            if (drinkFromThis != null) break;
                            if (wdg instanceof Inventory) {
                                for (WItem item : wdg.children(WItem.class)) {
                                    if (canDrinkFrom(item)) {
                                        drinkFromThis = item;
                                        break;
                                    } else if (item.item.contents instanceof Inventory) {
                                        for (WItem nitem : item.item.contents.children(WItem.class)) {
                                            if (canDrinkFrom(nitem)) {
                                                drinkFromThis = nitem;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (w instanceof AltBeltWnd) { // Alternate belt must be separately enabled
                        AltBeltWnd invBelt = (AltBeltWnd) w;
                        for (WItem item : invBelt.children(WItem.class)) {
                            if (drinkFromThis != null) break;
                            if (canDrinkFrom(item)) {
                                drinkFromThis = item;
                                break;
                            } else if (item.item.contents instanceof Inventory) {
                                for (WItem nitem : item.item.contents.children(WItem.class)) {
                                    if (canDrinkFrom(nitem)) {
                                        drinkFromThis = nitem;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (drinkFromThis == null) {
                for (WItem item : e.slots) {
                    if (drinkFromThis != null) break;
                    if (item != null && item.item.contents instanceof Inventory) {
                        for (WItem nitem : item.item.contents.children(WItem.class)) {
                            if (canDrinkFrom(nitem)) {
                                drinkFromThis = nitem;
                                break;
                            }
                        }
                    }
                }
            }
            boolean success = false;
            if (drinkFromThis != null) {
                if (PBotUtils.petalExists(gui.ui)) {
                    int limit = 1000;
                    int sleep = 10;
                    int cycles = 0;
                    while (PBotUtils.petalExists(gui.ui)) {
                        if (cycles >= limit) {
                            if (PBotUtils.getPetal(gui.ui, "Empty") != null) {
                                PBotUtils.closeFlowermenu(gui.ui);
                                break;
                            }
                            if (configuration.drinkmessage)
                                PBotUtils.debugMsg(gui.ui, "Wrong petal exist. Water timeout expired.", Color.RED);
                            gui.lastDrinkingSucessful = false;
                            gui.drinkingWater = false;
                            return;
                        }
                        sleep(sleep);
                        cycles += sleep;
                    }
                }
                if (configuration.autodrinkosip) {
                    if (getLiquid(drinkFromThis).contains("Water"))
                        success = drinkMode(drinkFromThis);
                    else
                        success = sipMode(drinkFromThis);
                } else {
                    if (configuration.drinkorsip) {
                        success = sipMode(drinkFromThis);
                    } else {
                        success = drinkMode(drinkFromThis);
                    }
                }
            }
            gui.lastDrinkingSucessful = success;
            gui.drinkingWater = false;
        } catch (Exception e) {
            gui.lastDrinkingSucessful = false;
            gui.drinkingWater = false;
            e.printStackTrace();
        }
    }

    private boolean drinkMode(WItem drinkFromThis) throws InterruptedException {
//        drinkFromThis.item.wdgmsg("iact", Coord.z, 3);
//                FlowerMenu menu = gui.ui.root.findchild(FlowerMenu.class);
//                int retries = 0; // After 100 retries aka. 5 seconds, it will probably never appear
//                while (menu == null) {
//                    if (retries++ > 100) {
//                        gui.drinkingWater = false;
//                        gui.lastDrinkingSucessful = false;
//                        return;
//                    }
//                    sleep(50);
//                    menu = gui.ui.root.findchild(FlowerMenu.class);
//                }
//                for (FlowerMenu.Petal opt : menu.opts) {
//                    if (opt.name.equals("Drink")) {
//                        menu.choose(opt);
//                        menu.destroy();
//                    }
//                }
        PBotUtils.activateItem(drinkFromThis);

        if (!PBotUtils.waitForFlowerMenu(gui.ui, 5000)) {
            if (configuration.drinkmessage)
                PBotUtils.debugMsg(gui.ui, "Flower not found. Water timeout expired. Drink failed.", Color.RED);
            return false;
        }
        if (PBotUtils.choosePetal(gui.ui, "Drink")) {
            PBotUtils.waitFlowermenuClose(gui.ui);
            if (waitDrinkPose())
                while (drinkPose()) {
                    sleep(50);
                }
            else {
                if (configuration.drinkmessage)
                    PBotUtils.debugMsg(gui.ui, "Drink pose not found. Water timeout expired. Sip failed.", Color.RED);
                return false;
            }
        } else {
            PBotUtils.closeFlowermenu(gui.ui);
            if (configuration.drinkmessage)
                PBotUtils.debugMsg(gui.ui, "Petal not found. Water timeout expired. Drink failed.", Color.RED);
            return false;
        }
        return true;
    }

    private boolean sipMode(WItem drinkFromThis) throws InterruptedException {
        int stamina = PBotUtils.getStamina(gui.ui);
        int sips = configuration.siponce ? 1 : (configuration.autosipthreshold - stamina) / 10;
//                while (PBotUtils.getStamina(gui.ui) < configuration.autosipthreshold && canDrinkFrom(drinkFromThis)) {
        for (int i = 0; i < sips; i++) {
            if (!canDrinkFrom(drinkFromThis)) {
                PBotUtils.debugMsg(gui.ui, "Can't drink from this.", Color.RED);
                return false;
            }
            if (PBotUtils.petalExists(gui.ui)) {
                int limit = configuration.sipwaiting;
                int sleep = 10;
                int cycles = 0;
                while (PBotUtils.petalExists(gui.ui)) {
                    if (cycles >= limit) {
                        if (configuration.drinkmessage)
                            PBotUtils.debugMsg(gui.ui, "Wrong petal exist. Water timeout expired. Sip failed.", Color.RED);
                        return false;
                    }
                    sleep(sleep);
                    cycles += sleep;
                }
            }
//                    drinkFromThis.item.wdgmsg("iact", Coord.z, 3);
//                    FlowerMenu menu = gui.ui.root.findchild(FlowerMenu.class);
//                    int retries = 0; // After 100 retries aka. 5 seconds, it will probably never appear
//                    while (menu == null) {
//                        if (retries++ > 100) {
//
//                        }
//                        sleep(50);
//                        menu = gui.ui.root.findchild(FlowerMenu.class);
//                    }
                    /*for (FlowerMenu.Petal opt : menu.opts) {
                        if (opt.name.equals("Sip")) {
                            menu.choose(opt);
                            menu.destroy();
                        }
                    }*/

            PBotUtils.activateItem(drinkFromThis);

            if (!PBotUtils.waitForFlowerMenu(gui.ui, 5000)) {
                return false;
            }

            if (PBotUtils.choosePetal(gui.ui, "Sip"))
                PBotUtils.waitFlowermenuClose(gui.ui);
            else {
                PBotUtils.closeFlowermenu(gui.ui);
                if (configuration.drinkmessage)
                    PBotUtils.debugMsg(gui.ui, "Petal not found. Water timeout expired. Sip failed.", Color.RED);
                return false;
            }

            if (waitDrinkPose())
                while (drinkPose()) {
                    sleep(50);
                }
            else {
                if (configuration.drinkmessage)
                    PBotUtils.debugMsg(gui.ui, "Drink pose not found. Water timeout expired. Sip failed.", Color.RED);
                return false;
            }
        }
        return true;
    }

    private boolean waitDrinkPose() throws InterruptedException {
        int limit = configuration.sipwaiting;
        int sleep = 10;
        int cycles = 0;
        while (!drinkPose()) {
            if (cycles >= limit) break;
            sleep(sleep);
            cycles += sleep;
        }
        return drinkPose();
    }

    private boolean drinkPose() {
        try {
            return PBotGobAPI.player(gui.ui).getPoses().contains("gfx/borka/drinkan");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDrinkFrom(WItem item) {
        Pattern liquidPattern = Pattern.compile(String.format("[0-9.]+ l of (%s)",
                //	String.join("|", new String[] { "Water", "Piping Hot Tea", "Tea" }), Pattern.CASE_INSENSITIVE));
                String.join("|", configuration.liquids)), Pattern.CASE_INSENSITIVE);
        ItemInfo.Contents contents = getContents(item);
        if (contents != null && contents.sub != null && contents.content >= 0.05) {
            synchronized (item.item.ui) {
                for (ItemInfo info : contents.sub) {
                    if (info instanceof ItemInfo.Name) {
                        ItemInfo.Name name = (ItemInfo.Name) info;
                        if (name.str != null) {
                            if (liquid != null) {
                                if (name.str.text.contains(liquid))
                                    return true;
                            }
                            if (name.str.text.contains(configuration.autoDrinkLiquid)) {
                                return true;
                            }
                            if (configuration.autoDrinkWhatever && liquidPattern.matcher(name.str.text).matches()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private String getLiquid(WItem item) {
        ItemInfo.Contents contents = getContents(item);
        if (contents != null && contents.sub != null) {
            synchronized (item.item.ui) {
                for (ItemInfo info : contents.sub) {
                    if (info instanceof ItemInfo.Name) {
                        ItemInfo.Name name = (ItemInfo.Name) info;
                        if (name.str != null)
                            return name.str.text;
                    }
                }
            }
        }
        return "";
    }

    private void sleep(int timeInMs) throws InterruptedException {
        Thread.sleep(timeInMs);
    }

    private ItemInfo.Contents getContents(WItem item) {
        if (item == null)
            return null;
        synchronized (item.item.ui) {
            try {
                for (ItemInfo info : item.item.info())
                    if (info instanceof ItemInfo.Contents)
                        return (ItemInfo.Contents) info;
            } catch (Loading ignored) {
            }
        }
        return null;
    }
}
