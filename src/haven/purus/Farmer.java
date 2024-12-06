package haven.purus;

import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.Gob;
import haven.Label;
import haven.MCache;
import haven.UI;
import haven.Widget;
import haven.WidgetVerticalAppender;
import haven.Window;
import haven.automation.AreaSelectCallback;
import haven.automation.GobSelectCallback;
import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.util.ArrayList;

public class Farmer extends Window implements AreaSelectCallback, GobSelectCallback {

    private Coord ca, cb;
    private boolean containeronly = false, replant = false, replantcontainer = true, stockpile = false;
    private CheckBox replantChkbox, fillContainerChkbox, replantBarrelChkbox, stockpileChkbox;
    private Gob barrel, chest;
    private ArrayList<Gob> containers = new ArrayList<>();
    private ArrayList<Coord> stockpileLocs = new ArrayList<>();
    private Button stockpilearea;
    private Thread selectingarea;
    private boolean areaselected = false;

    public Farmer() {
        super(UI.scale(350, 410), "Farming Bots", "Farming Bots");
    }

    public void added() {
        int btnszx = UI.scale(140);
        PBotUtils.debugMsg(ui, "Hold alt and left click containers to select them.", Color.white);
        Button carrotBtn = new Button(btnszx, "Carrot") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    ui.gui.map.unregisterAreaSelect();
                    // Start carrot farmer and close this window
                    SeedCropFarmer SCF = new SeedCropFarmer(cb, ca, "gfx/terobjs/plants/carrot", "gfx/invobjs/carrot", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);
                    ui.gui.add(SCF,
                            new Coord(ui.gui.sz.x / 2 - SCF.sz.x / 2, ui.gui.sz.y / 2 - SCF.sz.y / 2 - 200));
                    new Thread(SCF).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button carrotseedBtn = new Button(btnszx, "Carrot Seeds") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    SeedCropFarmer bf =
                            new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/carrot", "gfx/invobjs/seed-carrot", 3, replant, containeronly, replantcontainer, containers, false, null);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button beetBtn = new Button(btnszx, "Beetroot") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start beetroot onion farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/beet", "gfx/invobjs/beet", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button turnipBtn = new Button(btnszx, "Turnip") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start beetroot onion farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/turnip", "gfx/invobjs/turnip", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button turnipseedBtn = new Button(btnszx, "Turnip Seeds") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/turnip", "gfx/invobjs/seed-turnip", 1, replant, containeronly, replantcontainer, containers, false, null);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button turnip2Btn = new Button(btnszx, "Turnip Stage 3") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start beetroot onion farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/turnip", "gfx/invobjs/turnip", 2, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button onionBtn = new Button(btnszx, "Yellow Onion") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    SeedCropFarmer bf =
                            new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/yellowonion", "gfx/invobjs/yellowonion", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button redOnionBtn = new Button(btnszx, "Red Onion") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    SeedCropFarmer bf =
                            new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/redonion", "gfx/invobjs/redonion", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button leekBtn = new Button(btnszx, "Leeks") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start beetroot onion farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/leek", "gfx/invobjs/leek", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button pumpkinBtn = new Button(btnszx, "Pumpkin") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/pumpkin", "gfx/invobjs/seed-pumpkin", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);
                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button barleyBtn = new Button(btnszx, "Barley") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start barley farmer and close this window
                    SeedCropFarmer bf =
                            new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/barley", "gfx/invobjs/seed-barley", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button wheatBtn = new Button(btnszx, "Wheat") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    SeedCropFarmer bf =
                            new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/wheat", "gfx/invobjs/seed-wheat", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button milletBtn = new Button(btnszx, "Millet") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    SeedCropFarmer bf =
                            new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/millet", "gfx/invobjs/seed-millet", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button flaxBtn = new Button(btnszx, "Flax") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start flax farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/flax", "gfx/invobjs/seed-flax", 3, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button hempBtn = new Button(btnszx, "Hemp") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start hemp farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/hemp", "gfx/invobjs/seed-hemp", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
            }
        };
        Button poppyBtn = new Button(btnszx, "Poppy") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start poppy farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/poppy", "gfx/invobjs/seed-poppy", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button pipeBtn = new Button(btnszx, "Pipeweed") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start hemp farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/pipeweed", "gfx/invobjs/seed-pipeweed", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
            }
        };
        Button lettuceBtn = new Button(btnszx, "Lettuce") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start hemp farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/lettuce", "gfx/invobjs/seed-lettuce", 4, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
            }
        };
        Button kaleBtn = new Button(btnszx, "Green Kale") {
            @Override
            public void click() {
                if (replantcontainer && containers.size() == 0 || containeronly && containers.size() == 0) {
                    PBotUtils.debugMsg(ui, "Please select a container by holding alt and clicking it before starting if using barrel or replantbarrel.", Color.white);
                } else if (ca != null && cb != null) {
                    // Start hemp farmer and close this window
                    SeedCropFarmer bf = new SeedCropFarmer(ca, cb, "gfx/terobjs/plants/greenkale", "gfx/invobjs/seed-greenkale", 5, replant, containeronly, replantcontainer, containers, stockpile, stockpileLocs);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
            }
        };
        Button trelHarBtn = new Button(btnszx, "Trellis harvest") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    TrellisFarmer bf = new TrellisFarmer(ca, cb, true, false, false, chest);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button trelDesBtn = new Button(btnszx, "Trellis destroy") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    TrellisFarmer bf = new TrellisFarmer(ca, cb, false, true, false, chest);

                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button trelPlantBtn = new Button(btnszx, "Trellis plant") {
            @Override
            public void click() {
                if (ca != null && cb != null) {
                    // Start yellow onion farmer and close this window
                    TrellisFarmer bf = new TrellisFarmer(ca, cb, false, false, true, chest);
                    ui.gui.add(bf, new Coord(ui.gui.sz.x / 2 - bf.sz.x / 2, ui.gui.sz.y / 2 - bf.sz.y / 2 - 200));
                    new Thread(bf).start();
                    this.parent.destroy();
                } else {
                    PBotUtils.debugMsg(ui, "Area not selected!", Color.WHITE);
                }
            }
        };
        Button areaSelBtn = new Button(btnszx, "Select Area") {
            @Override
            public void click() {
                PBotUtils.debugMsg(ui, "Drag area over crops", Color.WHITE);
                ui.gui.map.farmSelect = true;
            }
        };
        stockpilearea = new Button(btnszx, "Stockpile Area") {
            @Override
            public void click() {
                if (ca == null && cb == null) {
                    PBotUtils.debugMsg(ui, "Please select your crop area first before your stockpiles", Color.white);
                } else {
                    PBotUtils.debugMsg(ui, "Click and Drag over 2 wide area for stockpiles", Color.WHITE);
                    selectingarea = new Thread(new Farmer.selectingarea(), "Farming Bots");
                    selectingarea.start();
                }
            }
        };

        replantChkbox = new CheckBox("Replant") {
            {
                a = replant;
            }

            public void set(boolean val) {
                a = val;
                replant = val;
                containeronly = !val;
                replantcontainer = !val;

                fillContainerChkbox.a = !val;
                replantBarrelChkbox.a = !val;
            }
        };
        replantBarrelChkbox = new CheckBox("Plant+Barrel") {
            {
                a = replantcontainer;
            }

            public void set(boolean val) {
                a = val;
                replantcontainer = val;
                replant = !val;
                containeronly = !val;

                replantChkbox.a = !val;
                fillContainerChkbox.a = !val;
            }
        };
        fillContainerChkbox = new CheckBox("Barrel") {
            {
                a = containeronly;
            }

            public void set(boolean val) {
                a = val;
                containeronly = val;
                replant = !val;
                replantcontainer = !val;

                replantBarrelChkbox.a = !val;
                replantChkbox.a = !val;
            }
        };
        stockpileChkbox = new CheckBox("Stockpile") {
            {
                a = stockpile;
            }

            public void set(boolean val) {
                a = val;
                stockpile = val;
                if (a)
                    PBotUtils.debugMsg(ui, "Currently only objects with smaller hitboxes work with stockpile, such as flax/hemp/poppy/leeks/pipeweed.");
            }
        };

        final WidgetVerticalAppender appender = new WidgetVerticalAppender(this);
        appender.addRow(areaSelBtn, stockpilearea, new Button(btnszx, "Water Settings") {
            @Override
            public void click() {
                ui.gui.toggleWaterSettings();
            }
        });
        appender.addRow(replantChkbox, replantBarrelChkbox, fillContainerChkbox, stockpileChkbox);
        appender.add(new Label(""));
        appender.addRow(carrotBtn, carrotseedBtn);
        appender.add(beetBtn);
        appender.addRow(turnipBtn, turnipseedBtn, turnip2Btn);
        appender.addRow(onionBtn, redOnionBtn, leekBtn);
        appender.add(pumpkinBtn);
        appender.addRow(barleyBtn, wheatBtn, milletBtn);
        appender.addRow(flaxBtn, hempBtn);
        appender.add(poppyBtn);
        appender.add(pipeBtn);
        appender.addRow(lettuceBtn, kaleBtn);
        appender.addRow(trelHarBtn, trelDesBtn, trelPlantBtn);

        pack();
    }

    private class selectingarea implements Runnable {
        @Override
        public void run() {
            PBotUtils.selectArea(ui);
            Coord aPnt = PBotUtils.getSelectedAreaA();
            Coord bPnt = PBotUtils.getSelectedAreaB();
            if (Math.abs(aPnt.x - bPnt.x) > 22 && Math.abs(aPnt.y - bPnt.y) > 22) {
                PBotUtils.debugMsg(ui, "Please select an area at least 2 tiles wide - try again.");
            } else {
                for (int i = Math.min(aPnt.x, bPnt.x) + 11 / 2; i < Math.max(aPnt.x, bPnt.x); i += 11) {
                    for (int j = Math.min(aPnt.y, bPnt.y) + 11 / 2; j < Math.max(aPnt.y, bPnt.y); j += 11) {
                        stockpileLocs.add(new Coord(i, j));
                    }
                }
            }
        }
    }

    private void registerGobSelect() {
        synchronized (GobSelectCallback.class) {
            ui.gui.map.registerGobSelect(this);
        }
    }

    public void areaselect(Coord a, Coord b) {
        this.ca = a.mul(MCache.tilesz2);
        this.cb = b.mul(MCache.tilesz2).add(11, 11);
        PBotUtils.debugMsg(ui, "Area selected!", Color.WHITE);
        ui.gui.map.unregisterAreaSelect();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            ui.gui.map.unregisterGobSelect();
            this.destroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    @Override
    public void gobselect(Gob gob) {
        if (gob.getres().basename().contains("barrel") || gob.getres().basename().contains("trough") || gob.getres().basename().contains("cistern")) {
            if (!containers.contains(gob)) {
                containers.add(gob);
                PBotUtils.debugMsg(ui, "Barrel/Trough/Cistern added! Total : " + containers.size(), Color.WHITE);
            }
        } else if (gob.getres().basename().contains("chest")) {
            chest = gob;
            PBotUtils.debugMsg(ui, "Chest selected!", Color.WHITE);
        } else
            PBotUtils.debugMsg(ui, "Please select only a barrel, trough, or chest!", Color.WHITE);
        //ui.gui.map.unregisterGobSelect();
    }
}
