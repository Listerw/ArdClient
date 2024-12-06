package haven.purus;

import haven.Button;
import haven.Coord;
import haven.Coord2d;
import haven.FlowerMenu;
import haven.GItem;
import haven.Gob;
import haven.Inventory;
import haven.Label;
import haven.Loading;
import static haven.OCache.posres;
import haven.Resource;
import haven.Sprite;
import haven.UI;
import haven.WItem;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotGob;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotInventory;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;
import haven.res.ui.tt.q.qbuff.QBuff;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SeedCropFarmer extends Window implements Runnable {

    private Coord rc1, rc2;

    private ArrayList<Gob> crops = new ArrayList<Gob>();

    private boolean stopThread = false;

    private Label lblProg, lblProg2;

    private int stage;
    private String cropName;
    private String seedName;
    private boolean replant;
    private boolean ispumpkin;
    private boolean stockpile;
    private boolean replantcontainer; // True = Only Container, False = Only replant
    private boolean containeronly;
    private ArrayList<Coord> stockpileLocs = new ArrayList<>();
    private ArrayList<Gob> containers = new ArrayList<>();
    //private Gob barrel;

    public SeedCropFarmer(Coord rc1, Coord rc2, String cropName, String seedName, int stage, boolean replant, boolean containeronly, boolean replantcontainer, ArrayList<Gob> containers, boolean stockpile, ArrayList<Coord> stockpileLocs) {
        super(UI.scale(120, 65), cropName.substring(cropName.lastIndexOf("/") + 1).substring(0, 1).toUpperCase()
                + cropName.substring(cropName.lastIndexOf("/") + 1).substring(1) + " Farmer");
        this.rc1 = rc1;
        this.rc2 = rc2;
        this.cropName = cropName;
        this.stage = stage;
        this.containers = containers;
        this.seedName = seedName;
        this.replantcontainer = replantcontainer;
        this.replant = replant;
        this.containeronly = containeronly;
        this.stockpile = stockpile;
        this.stockpileLocs = stockpileLocs;
        //	this.barrel = barrel;

        Label lblstxt = new Label("Progress:");
        add(lblstxt, UI.scale(15, 35));
        lblProg = new Label("Initialising...");
        add(lblProg, UI.scale(67, 35));
        lblProg2 = new Label("Initialising...");
        add(lblProg2, UI.scale(0, 55));

        Button stopBtn = new Button(UI.scale(120), "Stop") {
            @Override
            public void click() {
                stop();
            }
        };
        add(stopBtn, new Coord(0, 0));

    }

    public void run() {
        try {
            if (stockpile)
                PBotUtils.debugMsg(ui, "Auto-Stockpile not yet fully tested.", Color.white);
            // Initialise crop list
            ui.gui.map.unregisterGobSelect();
            crops = Crops();
            int totalCrops = crops.size();
            int cropsHarvested = 0;
            lblProg.settext(cropsHarvested + "/" + totalCrops);
            lblProg2.settext("Starting");
            if (seedName.contains("pumpkin"))
                ispumpkin = true;
            else
                ispumpkin = false;

            List<Gob> gobs = new ArrayList<>(crops);
            crop:
            for (; gobs.size() > 0; ) {
                Gob g = closestGob(gobs);
                if (stopThread)
                    return;
                if (PBotUtils.getStamina(ui) < 60) {
                    lblProg2.settext("Drinking");
                    PBotUtils.drink(ui, true);
                }

                if (stopThread)
                    return;

                // Right click the crop
                lblProg2.settext("Harvesting");
                int retryharvest = 0;
                while (PBotGobAPI.findGobById(ui, g.id) != null) {
                    if (stopThread)
                        return;
                    if (retryharvest > 50) {
                        gobs.remove(g);
                        continue crop;
                    }
                    retryharvest++;
                    if (g.getStage() != stage) {
                        gobs.remove(g);
                        continue crop;
                    }
                    lblProg2.settext("Moving to Harvest " + retryharvest);
                    if (!PBotUtils.pfGobClick(ui, g, 1, 0)) {
                        System.out.println("Path not found");
                        gobs.remove(g);
                        continue crop;
                    }
                    lblProg2.settext("Waiting for crop to disappear " + retryharvest);
                    if (!harvest(g)) {
                        System.out.println("Harvest not found");
                        continue;
                    }
                }
                // Replant
                if (ispumpkin) {
                    try {
                        lblProg2.settext("Grabbing seeds ispumpkin2");
                        GItem item = null;
                        List<PBotItem> itemlist = PBotUtils.getInventoryItemsByName(ui.gui.maininv, "gfx/invobjs/seed-pumpkin");
                        if (cropsHarvested == 0) //half second delay for the first pumpkin harvest to register to start the seed rotation.
                            PBotUtils.sleep(500);
                        if (itemlist.size() > 0) {//If seeds are present in inventory, try to use them.
                            for (PBotItem witem : itemlist) {
                                if (PBotUtils.getAmount(witem.witem) > 5) {
                                    while (PBotUtils.getItemAtHand(ui) == null) {
                                        lblProg2.settext("Grabbing seeds ispumpkin2");
                                        witem.takeItem(1000);
                                        int retrypickup = 0;
                                        while (PBotUtils.getItemAtHand(ui) == null) {
                                            retrypickup++;
                                            if (retrypickup > 200) {
                                                witem.takeItem(1000);
                                                retrypickup = 0;
                                            }
                                            PBotUtils.sleep(10);
                                        }
                                    }
                                }
                            }
                        } else {   //Failed to pickup seeds, either from not having any or other reasons. Pick up a pumpkin and acquire seeds.
                            lblProg2.settext("Pickup Pumpkin");
                            Gob pumpkin = null;
                            while (PBotUtils.findObjectByNames(ui, 10, "gfx/terobjs/items/pumpkin") == null) {
                                if (stopThread)
                                    return;
                                PBotUtils.sleep(10);
                            }
                            pumpkin = PBotUtils.findObjectByNames(ui, 10, "gfx/terobjs/items/pumpkin");
                            PBotUtils.pfRightClick(ui, pumpkin, 0);
                            int retrypumpkinpickup = 0;
                            while (PBotUtils.getInventoryItemsByName(ui.gui.maininv, "gfx/invobjs/pumpkin").size() == 0) {
                                retrypumpkinpickup++;
                                if (retrypumpkinpickup > 50) {
                                    lblProg2.settext("Retry Pickup");
                                    retrypumpkinpickup = 0;
                                    PBotUtils.pfRightClick(ui, pumpkin, 0);
                                }
                                PBotUtils.sleep(50);
                            }
                            List<PBotItem> pumpkinlist = PBotUtils.getInventoryItemsByName(ui.gui.maininv, "gfx/invobjs/pumpkin");
                            PBotItem invpumpkin = pumpkinlist.get(0);
                            FlowerMenu.setNextSelection("Slice");
                            invpumpkin.activateItem(0);
                            int retryslice = 0;
                            lblProg2.settext("Slicing");
                            while (ui.gui.maininv.getItemsPartial("seeds").size() == 0) {
                                if (stopThread)
                                    return;
                                retryslice++;
                                if (retryslice > 50) {
                                    lblProg2.settext("Retry Slicing");
                                    retryslice = 0;
                                    FlowerMenu.setNextSelection("Slice");
                                    invpumpkin.activateItem(0);
                                }
                                PBotUtils.sleep(50);
                            }
                            List<WItem> fleshlist = ui.gui.maininv.getItemsPartial("Flesh");
                            for (WItem flesh : fleshlist) {
                                flesh.item.wdgmsg("drop", Coord.z);
                            }
                            itemlist.clear();
                            itemlist = PBotUtils.getInventoryItemsByName(ui.gui.maininv, "gfx/invobjs/seed-pumpkin");
                            if (itemlist.size() == 0) {//If seeds are present in inventory, try to use them.
                                PBotUtils.debugMsg(ui, "Somehow don't have seeds after picking up and slicing a pumpkin, stopping", Color.white);
                                stop();
                                stopThread = true;
                            }
                        }
                        lblProg2.settext("Grabbing Seeds ispumpkin3");
                        Inventory inv = ui.gui.maininv;
                        for (Widget w = inv.child; w != null; w = w.next) {
                            if (w instanceof GItem && ((GItem) w).resource().name.equals(seedName) && (!seedName.contains("seed") || PBotUtils.getAmount((GItem) w) >= 5)) {
                                item = (GItem) w;
                                break;
                            }
                        }
                        if (item != null) {
                            System.out.println("picking up item to plant");
                            PBotUtils.takeItem(ui, item, 1000);
                        } else {
                            System.out.println("picking up item null");
                        }
                        while (PBotUtils.getItemAtHand(ui) == null) {
                            if (stopThread)
                                return;
                            PBotUtils.sleep(10);
                        }
                        // Plant the seed from hand
                        int amount = 0;
                        if (seedName.contains("seed"))
                            PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui));
                        lblProg2.settext("Planting");
                        //PBotUtils.mapInteractClick();
                        ui.gui.map.wdgmsg("itemact", Coord.z, PBotUtils.player(ui).rc.floor(posres), 0, 0, (int) PBotUtils.player(ui).id, PBotUtils.player(ui).rc.floor(posres), 0, -1);
                        while (PBotUtils.findNearestStageCrop(ui, 5, 0, cropName) == null || (PBotUtils.getItemAtHand(ui) != null && (seedName.contains("seed") && amount == PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui))))) {
                            if (stopThread)
                                return;
                            PBotUtils.sleep(10);
                        }
                        lblProg2.settext("Dropping seeds to inv");
                        Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                        if (slot != null) {
                            PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                            while (PBotUtils.getItemAtHand(ui) != null)
                                PBotUtils.sleep(50);
                        }
                    } catch (NullPointerException | Loading | Sprite.ResourceException q) {
                    }
                } else if (replant) {
                    try {
                        lblProg2.settext("Planting");

                        int retryplant = 0;
                        while (PBotUtils.findNearestStageCrop(ui, 5, 0, cropName) == null && harvestItem() != null) {
                            if (stopThread)
                                return;
                            if (retryplant > 50) {
                                gobs.remove(g);
                                continue crop;
                            }
                            retryplant++;
                            if (PBotUtils.getItemAtHand(ui) != null) {
                                if (inHandHarvestItem())
                                    PBotUtils.mapInteractClick(ui);
                                else {
                                    if (!inHandToInventory()) {
                                        System.out.println("Drop can not");
                                        if (PBotUtils.getItemAtHand(ui) != null)
                                            PBotUtils.dropItem(ui, 0);
                                    }
                                }
                            } else {
                                if (!takeSeed())
                                    continue;
                            }
                        }

                        lblProg2.settext("Dropping seeds");
                        if (!inHandToInventory()) {
                            System.out.println("Drop can not");
                            if (PBotUtils.getItemAtHand(ui) != null)
                                PBotUtils.dropItem(ui, 0);
                        }

//                            ui.gui.map.wdgmsg("itemact", Coord.z, PBotUtils.player(ui).rc.floor(posres), 0, 0, (int) PBotUtils.player(ui).id, PBotUtils.player(ui).rc.floor(posres), 0, -1);
                    } catch (NullPointerException | Loading | Sprite.ResourceException q) {
                    }
                } else if (replantcontainer) {
                    try {
                        retryharvest = 0;
                        while (PBotUtils.getItemAtHand(ui) == null) { // loops until successfully picked up seeds
                            if (stopThread)
                                return;
                            lblProg2.settext("Grabbing seeds2");
                            Inventory inv = ui.gui.maininv;
                            while (inv.getItemPartial("seed") == null) {
                                System.out.println(inv.getItemPartial("seed") + "");
                                PBotUtils.sleep(10);
                            }
                            WItem flax = inv.getItemPartial("seed");
                            GItem flax2 = flax.item;
                            List<WItem> items = inv.getIdenticalItems((flax2)); // acquires all seed stacks in inventory
                            sort(items); // sorts by quality
                            for (WItem seeds : items) {
                                GItem item = seeds.item;
                                if (PBotUtils.getAmount(item) >= 5) {
                                    PBotUtils.takeItem(ui, item, 1000);
                                    while (PBotUtils.getItemAtHand(ui) == null) { // just a double verification that we have successfully picked up seeds, should account for lag
                                        retryharvest++;
                                        if (retryharvest > 500) {
                                            retryharvest = 0;
                                            PBotUtils.takeItem(ui, item, 1000);
                                        }
                                        System.out.println(PBotUtils.getItemAtHand(ui) + " " + retryharvest);
                                        PBotUtils.sleep(10);
                                    }
                                    break;
                                }
                            }
                        }


                        // Plant the seed from hand
                        int amount = 0;
                        if (seedName.contains("seed"))
                            PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui)); // logs the seed count in your hand so it can use the count to verify it successfully planted
                        lblProg2.settext("Planting");
                        //PBotUtils.mapInteractClick();
                        ui.gui.map.wdgmsg("itemact", Coord.z, PBotUtils.player(ui).rc.floor(posres), 0, 0, (int) PBotUtils.player(ui).id, PBotUtils.player(ui).rc.floor(posres), 0, -1);
                        while (PBotUtils.findNearestStageCrop(ui, 5, 0, cropName) == null || (PBotUtils.getItemAtHand(ui) != null && (seedName.contains("seed") && amount == PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui))))) {
                            if (stopThread)
                                return;
                            PBotUtils.sleep(10);
                        }
                        // Merge seed from hand into inventory or put it in inventory
                        for (Widget w = ui.gui.maininv.child; w != null; w = w.next) {
                            if (w instanceof GItem && ((GItem) w).resource().name.equals(seedName)) {
                                GItem item = (GItem) w;
                                if (PBotUtils.getItemAtHand(ui) != null && PBotUtils.getAmount(item) < 50) {//finds other seeds in inventory with less than 50 count
                                    lblProg2.settext("Merging stacks");
                                    int handAmount = PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui));
                                    try {
                                        item.wdgmsg("itemact", 0);//merges
                                    } catch (Exception e) {
                                    }
                                    while (PBotUtils.getItemAtHand(ui) != null && PBotUtils.getAmount(PBotUtils.getGItemAtHand(ui)) == handAmount)//waits until the count changes to account for lag
                                        PBotUtils.sleep(50);
                                }
                            }
                        }
                        if (PBotUtils.getItemAtHand(ui) != null) {//still have seeds on cursor, dropping them in an empty inventory slot
                            lblProg2.settext("Dropping to inv");
                            Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                            if (slot != null) {
                                PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                while (PBotUtils.getItemAtHand(ui) != null)
                                    PBotUtils.sleep(50);
                            }
                        }
                        if (PBotUtils.invFreeSlots(ui) == 0) {//inv full, time to barrel
                            lblProg2.settext("Barreling");
                            for (int n = 0; true; n++) {
                                if (n >= 5) {
                                    PBotUtils.debugMsg(ui, "Barreling error, stopping.", Color.white);
                                    stopThread = true;
                                    stop();
                                    break;
                                }

                                if (PBotUtils.getItemAtHand(ui) != null)
                                    PBotUtils.dropItemFromHand(ui, 0, 1000);
                                PBotUtils.pfRightClick(ui, containers.get(0), 0);
                                String barrelname = containers.get(0).getres().basename().contains("barrel") ? "Barrel" : "Trough";
                                if (PBotUtils.waitForWindow(ui, barrelname, 1000))
                                    break;
                            }
                            GItem item = PBotUtils.getInventoryItemsByNames(ui.gui.maininv, Arrays.asList(seedName)).get(0).gitem;
                            PBotUtils.takeItem(ui, item, 1000);
                            while (PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName).size() > 0) {
                                if (stopThread)
                                    return;
                                if (PBotUtils.getItemAtHand(ui) == null) {
                                    System.out.println("Hand null, breaking");
                                    break;
                                }
                                List<PBotItem> list = PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName);
                                ui.gui.map.wdgmsg("itemact", Coord.z, containers.get(0).rc.floor(posres), 3, 0, (int) containers.get(0).id, containers.get(0).rc.floor(posres), 0, -1);
                                for (int i = 0; PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName).size() == list.size(); i++) {
                                    if (stopThread)
                                        break;
                                    if (containers.size() == 1 && i > 250) {
                                        PBotUtils.debugMsg(ui, "Only container in list appears to be full, stopping.", Color.white);
                                        stopThread = true;
                                        stop();
                                        break;
                                    } else if (i > 250) {
                                        PBotUtils.debugMsg(ui, "Container appears to be full, removing.", Color.white);
                                        Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                        PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                        PBotUtils.sleep(250);
                                        containers.remove(0);
                                        for (int n = 0; true; n++) {
                                            if (n >= 5) {
                                                PBotUtils.debugMsg(ui, "Barreling error, stopping.", Color.white);
                                                stopThread = true;
                                                stop();
                                                break;
                                            }

                                            if (PBotUtils.getItemAtHand(ui) != null)
                                                PBotUtils.dropItemFromHand(ui, 0, 1000);
                                            PBotUtils.pfRightClick(ui, containers.get(0), 0);
                                            String barrelname = containers.get(0).getres().basename().contains("barrel") ? "Barrel" : "Trough";
                                            if (PBotUtils.waitForWindow(ui, barrelname, 1000))
                                                break;
                                        }
                                        item = PBotUtils.getInventoryItemsByNames(ui.gui.maininv, Arrays.asList(seedName)).get(0).gitem;
                                        PBotUtils.takeItem(ui, item, 1000);
                                        break;
                                    }
                                    PBotUtils.sleep(10);
                                }
                            }
                            PBotUtils.sleep(250);
                            if (stopThread)
                                return;
                            if (PBotUtils.getItemAtHand(ui) != null) {//still have seeds on cursor, dropping them in an empty inventory slot
                                ui.gui.map.wdgmsg("itemact", Coord.z, containers.get(0).rc.floor(posres), 0, 0, (int) containers.get(0).id, containers.get(0).rc.floor(posres), 0, -1);
                            }
                            if (PBotUtils.getItemAtHand(ui) != null) {//still have seeds on cursor, dropping them in an empty inventory slot
                                lblProg2.settext("Dropping to inv");
                                Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                if (slot != null) {
                                    PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                    while (PBotUtils.getItemAtHand(ui) != null) {
                                        if (stopThread)
                                            return;
                                        PBotUtils.sleep(10);
                                    }
                                }
                            }
                        }
                    } catch (NullPointerException | Loading | Resource.LoadException x) {
                    }
                } else {
                    try {
                        if (containeronly && !containers.isEmpty()) { // Put items into container if inventory is full
                            GItem item;
                            if (PBotUtils.invFreeSlots(ui) == 0) {
                                lblProg2.settext("Barreling");
                                for (int n = 0; true; n++) {
                                    if (n >= 5) {
                                        PBotUtils.debugMsg(ui, "Barreling error, stopping.", Color.white);
                                        stopThread = true;
                                        stop();
                                        break;
                                    }

                                    if (PBotUtils.getItemAtHand(ui) != null)
                                        PBotUtils.dropItemFromHand(ui, 0, 1000);
                                    PBotUtils.pfRightClick(ui, containers.get(0), 0);
                                    String barrelname = containers.get(0).getres().basename().contains("barrel") ? "Barrel" : "Trough";
                                    if (PBotUtils.waitForWindow(ui, barrelname, 1000))
                                        break;
                                }
                                item = PBotUtils.getInventoryItemsByNames(ui.gui.maininv, Arrays.asList(seedName)).get(0).gitem;
                                PBotUtils.takeItem(ui, item, 1000);
                                while (PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName).size() > 0) {
                                    if (stopThread)
                                        return;
                                    if (PBotUtils.getItemAtHand(ui) == null) {
                                        System.out.println("Hand null, breaking");
                                        break;
                                    }
                                    List<PBotItem> list = PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName);
                                    ui.gui.map.wdgmsg("itemact", Coord.z, containers.get(0).rc.floor(posres), 3, 0, (int) containers.get(0).id, containers.get(0).rc.floor(posres), 0, -1);
                                    for (int i = 0; PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName).size() == list.size(); i++) {
                                        if (stopThread)
                                            return;
                                        if (containers.size() == 1 && i > 250) {
                                            PBotUtils.debugMsg(ui, "Only container in list appears to be full, stopping.", Color.white);
                                            stopThread = true;
                                            stop();
                                            break;
                                        } else if (i > 250) {
                                            PBotUtils.debugMsg(ui, "Container appears to be full, removing.", Color.white);
                                            Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                            PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                            PBotUtils.sleep(250);
                                            containers.remove(0);
                                            for (int n = 0; true; n++) {
                                                if (n >= 5) {
                                                    PBotUtils.debugMsg(ui, "Barreling error, stopping.", Color.white);
                                                    stopThread = true;
                                                    stop();
                                                    break;
                                                }

                                                if (PBotUtils.getItemAtHand(ui) != null)
                                                    PBotUtils.dropItemFromHand(ui, 0, 1000);
                                                PBotUtils.pfRightClick(ui, containers.get(0), 0);
                                                String barrelname = containers.get(0).getres().basename().contains("barrel") ? "Barrel" : "Trough";
                                                if (PBotUtils.waitForWindow(ui, barrelname, 1000))
                                                    break;
                                            }
                                            item = PBotUtils.getInventoryItemsByNames(ui.gui.maininv, Arrays.asList(seedName)).get(0).gitem;
                                            PBotUtils.takeItem(ui, item, 1000);
                                            PBotUtils.sleep(1000);
                                            break;
                                        }
                                        PBotUtils.sleep(10);
                                    }
                                }
                                PBotUtils.sleep(250);
                                if (PBotUtils.getItemAtHand(ui) != null) {//still have seeds on cursor, dropping them in an empty inventory slot
                                    ui.gui.map.wdgmsg("itemact", Coord.z, containers.get(0).rc.floor(posres), 0, 0, (int) containers.get(0).id, containers.get(0).rc.floor(posres), 0, -1);
                                }
                                if (PBotUtils.getItemAtHand(ui) != null) {//still have seeds on cursor, dropping them in an empty inventory slot
                                    lblProg2.settext("Dropping to inv");
                                    Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                    if (slot != null) {
                                        PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                        while (PBotUtils.getItemAtHand(ui) != null) {
                                            if (stopThread)
                                                return;
                                            PBotUtils.sleep(10);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NullPointerException | Loading | Resource.LoadException p) {
                    }
                }
                gobs.remove(g);
                cropsHarvested++;
                lblProg.settext(cropsHarvested + "/" + totalCrops);
            }
            boolean skip = cropName.contains("pipeweed") || cropName.contains("hemp") || cropName.contains("flax");
            if ((replantcontainer || containeronly) && !containers.isEmpty() && !skip) {
                Runnable collect = () -> {
                    List<PBotItem> items = PBotUtils.getInventoryItemsByNames(ui.gui.maininv, Arrays.asList(seedName));
                    if (items.isEmpty())
                        return;
                    lblProg2.settext("Barreling");
                    GItem item;
                    for (int n = 0; true; n++) {
                        if (n >= 5) {
                            PBotUtils.debugMsg(ui, "Barreling error, stopping.", Color.white);
                            stopThread = true;
                            stop();
                            break;
                        }

                        if (PBotUtils.getItemAtHand(ui) != null)
                            PBotUtils.dropItemFromHand(ui, 0, 1000);
                        PBotUtils.pfRightClick(ui, containers.get(0), 0);
                        String barrelname = containers.get(0).getres().basename().contains("barrel") ? "Barrel" : "Trough";
                        if (PBotUtils.waitForWindow(ui, barrelname, 1000))
                            break;
                    }
                    item = items.get(0).gitem;
                    PBotUtils.takeItem(ui, item, 1000);
                    while (PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName).size() > 0 || PBotUtils.getItemAtHand(ui) != null) {
                        if (stopThread)
                            return;
                        if (PBotUtils.getItemAtHand(ui) == null) {
                            System.out.println("Hand null, breaking");
                            break;
                        }
                        List<PBotItem> list = PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName);
                        ui.gui.map.wdgmsg("itemact", Coord.z, containers.get(0).rc.floor(posres), 3, 0, (int) containers.get(0).id, containers.get(0).rc.floor(posres), 0, -1);
                        for (int i = 0; PBotUtils.getInventoryItemsByName(ui.gui.maininv, seedName).size() == list.size(); i++) {
                            if (stopThread)
                                return;
                            if (containers.size() == 1 && i > 250) {
                                PBotUtils.debugMsg(ui, "Only container in list appears to be full, stopping.", Color.white);
                                stopThread = true;
                                stop();
                                break;
                            } else if (i > 250) {
                                PBotUtils.debugMsg(ui, "Container appears to be full, removing.", Color.white);
                                Coord slot = PBotUtils.getFreeInvSlot(ui.gui.maininv);
                                PBotUtils.dropItemToInventory(slot, ui.gui.maininv);
                                PBotUtils.sleep(250);
                                containers.remove(0);
                                for (int n = 0; true; n++) {
                                    if (n >= 5) {
                                        PBotUtils.debugMsg(ui, "Barreling error, stopping.", Color.white);
                                        stopThread = true;
                                        stop();
                                        break;
                                    }

                                    if (PBotUtils.getItemAtHand(ui) != null)
                                        PBotUtils.dropItemFromHand(ui, 0, 1000);
                                    PBotUtils.pfRightClick(ui, containers.get(0), 0);
                                    String barrelname = containers.get(0).getres().basename().contains("barrel") ? "Barrel" : "Trough";
                                    if (PBotUtils.waitForWindow(ui, barrelname, 1000))
                                        break;
                                }
                                item = PBotUtils.getInventoryItemsByNames(ui.gui.maininv, Arrays.asList(seedName)).get(0).gitem;
                                PBotUtils.takeItem(ui, item, 1000);
                                PBotUtils.sleep(1000);
                                break;
                            }
                            PBotUtils.sleep(10);
                        }
                    }
                };
                String groundname = ""; //most stuff goes from terobjs/plants/carrot to terobjs/items/carrot for example
                boolean isbarrel = containers.get(0).getres().basename().contains("barrel");
                if (cropName.contains("carrot") || cropName.contains("yellowonion") || cropName.contains("redonion") || cropName.contains("beet")
                        || cropName.contains("leek") || cropName.contains("turnip") || cropName.contains("pumpkin")) {
                    if ((!isbarrel) || (isbarrel && cropName.contains("seed")))
                        groundname = seedName.replace("invobjs", "terobjs/items");
                } else {
                    if (cropName.contains("pipeweed")) {
                        groundname = seedName.replace("invobjs", "terobjs/items");
                    } else if (cropName.contains("hemp")) {
                        groundname = seedName.replace("invobjs", "terobjs/items");
                    } else if (cropName.contains("flax")) {
                        groundname = seedName.replace("invobjs", "terobjs/items");
                    } else if (cropName.contains("poppy")) {
                        if (!isbarrel)
                            groundname = "gfx/terobjs/items/flower-poppy";
                        else
                            groundname = seedName.replace("invobjs", "terobjs/items");
                    } else if (cropName.contains("wheat") || cropName.contains("barley") || cropName.contains("millet")) {
                        if (!isbarrel)
                            groundname = "gfx/terobjs/items/straw";
                        else
                            groundname = seedName.replace("invobjs", "terobjs/items");
                    } else if (cropName.contains("pumpkin")) {
                        if (!isbarrel)
                            groundname = "gfx/terobjs/items/pumpkin";
                        else
                            groundname = seedName.replace("invobjs", "terobjs/items");
                    }
                }

                List<Gob> grounditems = new ArrayList<>();
                for (PBotGob g : PBotUtils.gobsInArea(ui, rc1, rc2))
                    if (g.getResname().equals(groundname))
                        grounditems.add(g.gob);
                collect:
                for (; grounditems.size() > 0; ) {
                    Gob grounditem = closestGob(grounditems);
                    if (stopThread)
                        return;
                    lblProg2.settext("Grabbing items");
                    PBotUtils.sysLogAppend(ui, "Grabbing stuff.", "white");
                    if (PBotGobAPI.findGobById(ui, grounditem.id) == null) {
                        grounditems.remove(grounditem);
                        continue;
                    }
                    if (!PBotUtils.pfRightClick(ui, grounditem, 0)) {
                        grounditems.remove(grounditem);
                        continue;
                    }

                    for (int i = 0, sleep = 10; PBotUtils.isMoving(ui) || PBotGobAPI.findGobById(ui, grounditem.id) != null; i += sleep) {
                        if (i >= 2000) {
                            grounditems.remove(grounditem);
                            continue collect;
                        }
                        PBotUtils.sleep(10);
                    }
                    grounditems.remove(grounditem);

                    System.out.println("inv free slots : " + PBotUtils.invFreeSlots(ui));

                    if (PBotUtils.invFreeSlots(ui) == 0 || grounditems.size() == 0)
                        collect.run();

                    grounditems.clear();
                    for (PBotGob g : PBotUtils.gobsInArea(ui, rc1, rc2))
                        if (g.getResname().equals(groundname))
                            grounditems.add(g.gob);
                }
                collect.run();
            }
            if (stockpile) {//new feature, when done farming stockpile the leftover materials
                lblProg2.settext("Stockpiling");
                List<Gob> stockpiles = new ArrayList<>();
                List<Coord> initcoordlist = stockpileLocs;//retain list so we can get all the new stockpiles from it later
                String groundname = null; //most stuff goes from terobjs/plants/carrot to terobjs/items/carrot for example
                List<String> invname = new ArrayList<>();

                //this attempts to resolve the inventory res and ground res of what you harvested
                if (cropName.contains("carrot") || cropName.contains("yellowonion") || cropName.contains("redonion") || cropName.contains("beet")
                        || cropName.contains("leek") || cropName.contains("turnip") || cropName.contains("pumpkin")) {
                    groundname = seedName.replace("invobjs", "terobjs/items");
                    invname.add(cropName.replaceAll("terobjs/plants", "invobjs"));
                } else {
                    if (cropName.contains("pipeweed")) {
                        groundname = "gfx/terobjs/items/tobacco-fresh";
                        invname.add("gfx/invobjs/tobacco-fresh");
                    } else if (cropName.contains("hemp")) {
                        groundname = "gfx/terobjs/items/hempfibre";
                        invname.add("gfx/invobjs/hempfibre");
                    } else if (cropName.contains("flax")) {
                        groundname = "gfx/terobjs/items/flaxfibre";
                        invname.add("gfx/invobjs/flaxfibre");
                    } else if (cropName.contains("poppy")) {
                        groundname = "gfx/terobjs/items/flower-poppy";
                        invname.add("gfx/invobjs/flower-poppy");
                    } else if (cropName.contains("wheat") || cropName.contains("barley") || cropName.contains("millet")) {
                        groundname = "gfx/terobjs/items/straw";
                        invname.add("gfx/invobjs/straw");
                    } else if (cropName.contains("pumpkin")) {
                        groundname = "gfx/terobjs/items/pumpkin";
                        invname.add("gfx/invobjs/pumpkin");
                    }
                }

                Runnable stocking = () -> {
                    boolean stop = false;
                    while (PBotUtils.getInventoryItemsByName(ui.gui.maininv, invname.get(0)).size() != 0 && !stop) {
                        if (stopThread)
                            return;
                        System.out.println("In stockpile loop");
                        PBotUtils.sleep(1000);
                        if (PBotUtils.getItemAtHand(ui) != null)
                            PBotUtils.dropItem(ui, 0);
                        if (stockpiles.isEmpty()) {
                            System.out.println("Stockpiles empty");
                            PBotUtils.debugMsg(ui, "All chosen stockpiles full!", Color.GREEN);
                            stop = true;
                            break;
                        }

                        if (PBotUtils.stockpileIsFull(PBotUtils.findObjectById(ui, stockpiles.get(0).id))) {
                            System.out.println("Stockpile full");
                            stockpiles.remove(0);
                            continue;
                        }
                        if (stop)
                            break;
                        if (stockpiles.size() == 0) {
                            PBotUtils.debugMsg(ui, "Stockpile list now empty, stopping.", Color.white);
                            stop = true;
                            stop();
                        }
                        PBotUtils.pfRightClick(ui, stockpiles.get(0), 0);
                        int retry = 0;
                        while (ui.gui.getwnd("Stockpile") == null) {
                            if (!PBotUtils.isMoving(ui))
                                retry++;
                            if (retry > 100) {
                                if (stop)
                                    break;
                                retry = 0;
                                System.out.println("Retry : " + retry);
                                PBotUtils.sysLogAppend(ui, "Retrying stockpile interaction", "white");
                                PBotUtils.dropItem(ui, 0);
                                PBotUtils.pfRightClick(ui, stockpiles.get(0), 0);
                            }
                            PBotUtils.sleep(10);
                        }
                        PBotUtils.sleep(1000);
                        System.out.println("clicking stockpile");
                        try {
                            while (PBotUtils.getItemAtHand(ui) == null) {
                                if (stopThread)
                                    return;
                                PBotUtils.playerInventory(ui).getInventoryItemByResnames(invname.get(0)).takeItem(1000);
                            }
                        } catch (NullPointerException q) {
                        }
                        int cnt = PBotUtils.invFreeSlots(ui);
                        try {
                            ui.gui.map.wdgmsg("itemact", Coord.z, stockpiles.get(0).rc.floor(posres), 3, 0, (int) stockpiles.get(0).id, stockpiles.get(0).rc.floor(posres), 0, -1);
                        } catch (IndexOutOfBoundsException lolindexes) {
                            PBotUtils.debugMsg(ui, "Critical error in stockpile list, stopping thread to prevent crash.", Color.white);
                            stop = true;
                            stop();
                        }
                    }
                };

                while (PBotUtils.getItemAtHand(ui) == null) {
                    if (stopThread)
                        return;
                    lblProg2.settext("Grabbing items");
                    if (PBotUtils.findObjectByNames(ui, 5000, groundname) == null) {
                        break;
                    }
                    PBotUtils.sysLogAppend(ui, "Grabbing stuff.", "white");
                    Gob g = PBotUtils.findObjectByNames(ui, 5000, groundname);
                    PBotUtils.pfRightClick(ui, g, 1);
//                    ui.gui.map.wdgmsg("click", g.sc, g.rc.floor(posres), 3, 1, 0, (int) g.id, g.rc.floor(posres), 0, -1);
                    PBotUtils.sleep(1000);

                    while (/*PBotUtils.getItemAtHand(ui) == null &
                            PBotUtils.findObjectByNames(ui, 5000, groundname) != null && */
                            PBotUtils.isMoving(ui)) {
//                        System.out.println("waiting for item on  hand");
                        PBotUtils.sleep(10);
                    }
                    System.out.println("inv free slots : " + PBotUtils.invFreeSlots(ui));
                }
                PBotUtils.dropItemFromHand(ui, 0);

                while (PBotUtils.getGItemAtHand(ui) != null) {//wait for hand to be droped
                    if (stopThread)
                        return;
                    PBotUtils.sleep(10);
                }


                List<PBotItem> items = PBotUtils.getInventoryItemsByNames(PBotUtils.playerInventory(ui).inv, invname);
                System.out.println("Stocklocs size : " + stockpileLocs.size() + " items size : " + items.size() + " " + invname);
                lblProg2.settext("Creating Stockpiles");
                while (stockpileLocs.size() > 0 && items.size() > 0) {//build stockpiles
                    if (stopThread)
                        return;
                    PBotItem item = items.get(0);
                    Coord location = stockpileLocs.get(0);
                    if (Stockpiles(location, location).isEmpty()) {
                        item.takeItem(1000);
                        while (PBotUtils.getItemAtHand(ui) == null)
                            PBotUtils.sleep(15);
                        PBotGobAPI.makePile(ui);
                        while (PBotUtils.getItemAtHand(ui) != null) {
                            PBotUtils.sleep(15);
                        }
                        if (!PBotUtils.pfLeftClick(ui, location.x + 11, location.y) && !PBotUtils.pfLeftClick(ui, location.x - 11, location.y) && !PBotUtils.pfLeftClick(ui, location.x, location.y + 11) && !PBotUtils.pfLeftClick(ui, location.x, location.y - 11)) { // Couldn't find path next to the stockpile that we want to make next
                            items.remove(item);
                            stockpileLocs.remove(location);
                            continue;
                        }
                        if (!PBotGobAPI.placeThing(ui, location.x, location.y, 1000))
                            if (PBotGobAPI.unplaceThing(ui, 1000))
                                if (!inHandToInventory()) {
                                    System.out.println("Drop can not");
                                    if (PBotUtils.getItemAtHand(ui) != null)
                                        PBotUtils.dropItem(ui, 0);
                                }
                    }
                    items = PBotUtils.getInventoryItemsByNames(PBotUtils.playerInventory(ui).inv, invname);
                    stockpileLocs.remove(location);
                    PBotUtils.sleep(1000);//putting in small delay, seems to miss creating the last stockpile
                }
                stockpiles.addAll(Stockpiles(PBotUtils.getSelectedAreaA(), PBotUtils.getSelectedAreaB()));
                boolean stop = false;
                lblProg2.settext("Stockpiling");
                stocking.run();
                while (PBotUtils.findObjectByNames(ui, 5000, groundname) != null) {
                    if (stopThread)
                        return;
                    System.out.println("In main loop");
                    if (stop)
                        break;
                    boolean pathfind = true;
                    while (PBotUtils.getItemAtHand(ui) == null) {
                        if (stopThread)
                            return;
                        if (PBotUtils.findObjectByNames(ui, 5000, groundname) == null) {
                            PBotUtils.sysLogAppend(ui, "Out of items to stockpile, finishing.", "white");
                            stop = true;
                            break;
                        }
                        PBotUtils.sysLogAppend(ui, "Grabbing stuff.", "white");
                        Gob g = PBotUtils.findObjectByNames(ui, 5000, groundname);
                        int retry = 0;
                        if (pathfind) {
                            pathfind = false;
                            PBotUtils.pfGobClick(ui, g, 1, 0);
                            while (g.rc.dist(ui.gui.map.player().rc) > 11) { //get within one tile of the target
                                if (stopThread)
                                    return;
                                lblProg2.settext("Moving to Pickup");
                                retry++;
                                while (PBotUtils.isMoving(ui))
                                    PBotUtils.sleep(10);//if we're moving, sleep and dont trigger unstucking
                                if (retry > 500) {
                                    retry = 0;
                                    lblProg2.settext("Unstucking");
                                    PBotUtils.sysLogAppend(ui, "Moving char to unstuck", "white");
                                    Gob player = ui.gui.map.player();
                                    Coord location = player.rc.floor(posres);
                                    int x = location.x + getrandom();
                                    int y = location.y + getrandom();
                                    Coord finalloc = new Coord(x, y);
                                    ui.gui.map.wdgmsg("click", Coord.z, finalloc, 1, 0);
                                    ui.gui.map.pllastcc = finalloc.mul(posres);
                                    PBotUtils.sleep(1000);
                                    PBotUtils.pfGobClick(ui, g, 1, 0);
                                }
                                PBotUtils.sleep(10);
                            }
                        }
                        //shift right click
                        ui.gui.map.wdgmsg("click", g.sc, g.rc.floor(posres), 3, 1, 0, (int) g.id, g.rc.floor(posres), 0, -1);
                        ui.gui.map.pllastcc = g.rc;
                        PBotUtils.sleep(2000);//wait 2 seconds to start moving
                        while (PBotUtils.getItemAtHand(ui) == null & PBotUtils.findObjectByNames(ui, 5000, groundname) != null && PBotUtils.isMoving(ui)) {
                            if (stopThread)
                                return;
                            System.out.println("waiting for item on  hand");
                            PBotUtils.sleep(10);
                        }
                        System.out.println("inv free slots : " + PBotUtils.invFreeSlots(ui));
                    }

                    PBotUtils.sysLogAppend(ui, "Done Grabbing stuff.", "white");
                    if (stop)
                        break;
                    stocking.run();
                    if (PBotUtils.findObjectByNames(ui, 5000, groundname) == null)
                        break;
                }
                if (PBotUtils.getInventoryItemsByNames(PBotUtils.playerInventory(ui).inv, invname).size() > 0) {
                    lblProg2.settext("Finishing Stockpiling");
                    System.out.println("In stockpile loop");
                    PBotUtils.sleep(1000);
                    if (PBotUtils.getItemAtHand(ui) != null)
                        PBotUtils.dropItem(ui, 0);
                    if (stockpiles.isEmpty()) {
                        System.out.println("Stockpiles empty");
                        PBotUtils.debugMsg(ui, "All chosen stockpiles full!", Color.GREEN);
                        return;
                    }
                    if (PBotUtils.stockpileIsFull(PBotUtils.findObjectById(ui, stockpiles.get(0).id))) {
                        System.out.println("Stockpile full");
                        stockpiles.remove(0);
                    }
                    if (stockpiles.size() == 0) {
                        PBotUtils.debugMsg(ui, "Stockpile list now empty, stopping.", Color.white);
                        stop = true;
                        stop();
                    }
                    PBotUtils.pfRightClick(ui, stockpiles.get(0), 0);
                    int retry = 0;
                    while (ui.gui.getwnd("Stockpile") == null) {
                        if (stopThread)
                            return;
                        if (!PBotUtils.isMoving(ui))
                            retry++;
                        if (retry > 100) {
                            if (stop)
                                break;
                            retry = 0;
                            System.out.println("Retry : " + retry);
                            PBotUtils.sysLogAppend(ui, "Retrying stockpile interaction", "white");
                            PBotUtils.dropItem(ui, 0);
                            PBotUtils.pfRightClick(ui, stockpiles.get(0), 0);
                        }
                        PBotUtils.sleep(10);
                    }
                    PBotUtils.sleep(1000);
                    System.out.println("clicking stockpile");
                    try {
                        while (PBotUtils.getItemAtHand(ui) == null) {
                            if (stopThread)
                                return;
                            PBotUtils.playerInventory(ui).getInventoryItemByResnames(invname.get(0)).takeItem(1000);
                        }
                    } catch (NullPointerException q) {
                    }
                    int cnt = PBotUtils.invFreeSlots(ui);
                    try {
                        ui.gui.map.wdgmsg("itemact", Coord.z, stockpiles.get(0).rc.floor(posres), 3, 0, (int) stockpiles.get(0).id, stockpiles.get(0).rc.floor(posres), 0, -1);
                    } catch (IndexOutOfBoundsException lolindexes) {
                        PBotUtils.debugMsg(ui, "Critical error in stockpile list, stopping thread to prevent crash.", Color.white);
                        stop();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        PBotUtils.debugMsg(ui, cropName.substring(cropName.lastIndexOf("/") + 1).substring(0, 1).toUpperCase()
                + cropName.substring(cropName.lastIndexOf("/") + 1).substring(1)
                + " Farmer finished!", Color.white);
        this.destroy();
    }

    public ArrayList<Gob> Crops() {
        // Initialises list of crops to harvest between the selected coordinates
        ArrayList<Gob> gobs = new ArrayList<Gob>();
        double bigX = Math.max(rc1.x, rc2.x);
        double smallX = Math.min(rc1.x, rc2.x);
        double bigY = Math.max(rc1.y, rc2.y);
        double smallY = Math.min(rc1.y, rc2.y);
        for (Gob gob : ui.sess.glob.oc.getallgobs()) {
            if (gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
                    && gob.rc.y >= smallY && gob.getres().name.contains(cropName) && gob.getStage() == stage) {
                gobs.add(gob);
            }
        }
        gobs.sort(new CoordSort());
        return gobs;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            stop();
            reqdestroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    // Sorts coordinate array to efficient sequence
    class CoordSort implements Comparator<Gob> {
        public int compare(Gob a, Gob b) {
            if (a.rc.floor().x == b.rc.floor().x) {
                if (a.rc.floor().x % 2 == 0)
                    return (a.rc.floor().y < b.rc.floor().y) ? 1 : (a.rc.floor().y > b.rc.floor().y) ? -1 : 0;
                else
                    return (a.rc.floor().y < b.rc.floor().y) ? -1 : (a.rc.floor().y > b.rc.floor().y) ? 1 : 0;
            } else
                return (a.rc.floor().x < b.rc.floor().x) ? -1 : (a.rc.floor().x > b.rc.floor().x) ? 1 : 0;
        }
    }

    public void sort(List<WItem> items) {
        Collections.sort(items, (a, b) -> {
            QBuff aq = a.item.quality();
            QBuff bq = b.item.quality();
            if (aq == null || bq == null)
                return 0;
            else if (aq.q == bq.q)
                return 0;
            else if (aq.q > bq.q)
                return -1;
            else
                return 1;
        });
    }

    public int getrandom() {
        Random r = new Random();
        int randomNumber = r.ints(1, -6000, 6000).findFirst().getAsInt();
        return randomNumber;
    }

    public void stop() {
        // Stops thread
        PBotUtils.debugMsg(ui, cropName.substring(cropName.lastIndexOf("/") + 1).substring(0, 1).toUpperCase()
                + cropName.substring(cropName.lastIndexOf("/") + 1).substring(1)
                + " Farmer stopped!", Color.white);
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        ui.gui.map.pllastcc = ui.gui.map.player().rc;
        stopThread = true;
        this.destroy();
    }

    public ArrayList<Gob> Stockpiles(Coord a, Coord b) {
        // Initialises list of crops to harvest between the selected coordinates
        ArrayList<Gob> gobs = new ArrayList<Gob>();
        double bigX = Math.max(a.x, b.x);
        double smallX = Math.min(a.x, b.x);
        double bigY = Math.max(a.y, b.y);
        double smallY = Math.min(a.y, b.y);
        for (Gob gob : ui.sess.glob.oc.getallgobs()) {
            if (gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
                    && gob.rc.y >= smallY && gob.getres().name.contains("stockpile")) {
                gobs.add(gob);
            }
        }
        gobs.sort(new CoordSort());
        return gobs;
    }

    public boolean harvest(Gob g) {
        if (PBotUtils.getItemAtHand(ui) != null)
            PBotUtils.dropItemFromHand(ui, 0, 1000);
        PBotUtils.doClick(ui, g, 3, 0);
        PBotUtils.waitForFlowerMenu(ui, 1000);
        if (PBotUtils.petalExists(ui)) {
            if (PBotUtils.choosePetal(ui, "Harvest")) {
                if (!PBotUtils.waitFlowermenuClose(ui, 1000)) {
                    PBotUtils.closeFlowermenu(ui, 1000);
                    return (false);
                }
                PBotUtils.waitForHourglass(ui, 1000);
                return (true);
            } else
                return (false);
        } else
            return (false);
    }

    public PBotItem harvestItem() {
        PBotInventory inv = PBotUtils.playerInventory(ui);
        List<PBotItem> items = inv.getInventoryItemsByResnames(seedName);
        for (PBotItem item : items) {
            if (!seedName.contains("seed") || item.getAmount() >= 5) {
                return (item);
            }
        }
        return (null);
    }

    public boolean takeSeed() {
        int retrytake = 0;
        while (PBotUtils.getItemAtHand(ui) == null && harvestItem() != null) {
            if (stopThread)
                break;
            if (retrytake > 50)
                return (false);
            retrytake++;
            lblProg2.settext("Grabbing seeds " + retrytake);
            if (harvestItem() == null || !harvestItem().takeItem(1000)) {
                System.out.println("Take can not");
                continue;
            }
        }
        return (true);
    }

    public boolean inHandHarvestItem() {
        if (PBotUtils.getItemAtHand(ui) != null) {
            while (PBotUtils.getItemAtHand(ui).getResname() == null) {
                PBotUtils.sleep(10);
            }
            if (PBotUtils.getItemAtHand(ui).getResname().equals(seedName))
                return (true);
        }
        return (false);
    }

    public boolean inHandToInventory() {
        if (PBotUtils.getItemAtHand(ui) != null) {
            PBotItem item = PBotUtils.getItemAtHand(ui);
            PBotInventory inv = PBotUtils.playerInventory(ui);
            Coord coord = inv.freeSpaceForItem(item);
            if (coord != null) {
                inv.dropItemToInventory(coord);
                int retrydrop = 0;
                while (PBotUtils.getItemAtHand(ui) == null) {
                    if (stopThread)
                        break;
                    lblProg2.settext("Dropping seeds " + retrydrop);
                    if (retrydrop > 50)
                        return (false);
                    retrydrop++;
                    PBotUtils.sleep(100);
                }
                return (true);
            } else
                return (false);
        }
        return (false);
    }

    public Gob closestGob(List<Gob> list) {
        double min = Double.MAX_VALUE;
        Gob gob = null;
        Coord2d pc = PBotGobAPI.player(ui).getRcCoords();
        for (Gob g : list) {
            if (pc.dist(g.rc) < min) {
                min = Math.min(min, pc.dist(g.rc));
                gob = g;
            }
        }
        return (gob);
    }
}