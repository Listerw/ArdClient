package haven.automation;

import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.Coord2d;
import haven.FlowerMenu;
import haven.GameUI;
import haven.Gob;
import haven.Label;
import haven.MCache;
import haven.RichText;
import haven.UI;
import haven.Widget;
import haven.Window;
import haven.automation.helpers.TileStatic;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotItem;
import haven.purus.pbot.PBotUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;

public class TunnelerBot extends Window implements Runnable {
    private final Button mineButton;
    private boolean autoMineActive;
    private boolean autoRoadActive;

    private final CheckBox mineToTheLeftCheckbox;
    private boolean mineToTheLeft = true;
    private final CheckBox mineToTheRightCheckbox;
    private boolean mineToTheRight = true;

    private final Label miningDirectionLabel;
    private Coord direction = new Coord(0, -1);
    private Coord directionPerpendicular = new Coord(-1, 0);
    private int milestoneRot = 19115;

    private MCache map;
    private final GameUI gui;
    private boolean stop;
    private List<Gob> columns = new ArrayList<>();

    private int stage;
    private Coord2d currentAnchorColumn;

    public TunnelerBot(GameUI gui) {
        super(UI.scale(120, 185), "Auto Tunneler");
        this.gui = gui;
        stop = false;
        map = gui.map.glob.map;
        currentAnchorColumn = gui.map.player().rc;
        stage = 0;
        autoMineActive = false;

        Button northDirection = new Button(UI.scale(25), "N") {
            @Override
            public void click() {
                changeDirection(1);
            }
        };
        add(northDirection, UI.scale(45, 10));
        Button eastDirection = new Button(UI.scale(25), "E") {
            @Override
            public void click() {
                changeDirection(2);
            }
        };
        add(eastDirection, UI.scale(75, 35));
        Button southDirection = new Button(UI.scale(25), "S") {
            @Override
            public void click() {
                changeDirection(3);
            }
        };
        add(southDirection, UI.scale(45, 60));
        Button westDirection = new Button(UI.scale(25), "W") {
            @Override
            public void click() {
                changeDirection(4);
            }
        };
        add(westDirection, UI.scale(15, 35));

        miningDirectionLabel = new Label("N");
        add(miningDirectionLabel, UI.scale(52, 38));
        miningDirectionLabel.tooltip = RichText.render("Choose mining direction N-E-S-W (on map)", UI.scale(300));

        mineToTheLeftCheckbox = new CheckBox("Left") {
            {
                a = mineToTheLeft;
            }

            public void set(boolean val) {
                mineToTheLeft = val;
                a = val;
                resetParams();
            }
        };
        add(mineToTheLeftCheckbox, UI.scale(15, 95));
        mineToTheLeftCheckbox.tooltip = RichText.render("Mine left branch near every column.\nIf disabled but other option (right) is\nenabled still need to mine 1 tile this way.", UI.scale(300));

        mineToTheRightCheckbox = new CheckBox("Right") {
            {
                a = mineToTheRight;
            }

            public void set(boolean val) {
                mineToTheRight = val;
                a = val;
                resetParams();
            }
        };
        add(mineToTheRightCheckbox, UI.scale(60, 95));
        mineToTheRightCheckbox.tooltip = RichText.render("Mine right branch near every column.", UI.scale(300));

        CheckBox roadBox = new CheckBox("Autoroad") {
            {
                a = autoRoadActive;
            }

            public void set(boolean val) {
                autoRoadActive = val;
                a = val;
            }
        };
        add(roadBox, UI.scale(15, 125));

        mineButton = new Button(UI.scale(100), "Start Mining") {
            @Override
            public void click() {
                autoMineActive = !autoMineActive;
                if (autoMineActive) {
                    this.change("Stop Mining");
                } else {
                    this.change("Start Mining");
                }
            }
        };
        add(mineButton, UI.scale(10, 145));
    }

    @Override
    public void run() {
        try {
            sleep(2000);
            miningLoop:
            while (!stop) {
                if (gui.fv != null && gui.fv.current != null) {
                    fleeInOppositeDirection();
                    stage = -1;
                } else if (autoMineActive) {
                    if (gui.getmeter("stam", 0).a < 0.40) {
                        clearhand();
                        AUtils.drinkTillFull(gui, 0.99, 0.99);
                    }
                    clearhand();

                    List<Gob> looseRocks = AUtils.getGobsPartial("looserock", gui);
                    for (Gob rock : looseRocks) {
                        if (rock.rc.dist(gui.map.player().rc) < 125) {
                            ui.root.wdgmsg("gk", 27);
                            resetParams();
                            gui.msg("Loose rock in dangerous distance. Mining stopped.");
                        }
                    }

                    List<Gob> boulders = AUtils.getGobsPartial("bumlings", gui);
                    for (Gob boulder : boulders) {
                        if (boulder.rc.dist(gui.map.player().rc) < 20) {
                            clearhand();
                            AUtils.rightClickGobAndSelectOption(gui, boulder, 0);
                            AUtils.clickUiButton("paginae/act/mine", gui);
                            sleep(2000);
                            continue miningLoop;
                        }
                    }

                    debug("stage " + stage);
                    if (stage == 0) {
                        columns = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/column").stream().map(p -> p.gob).collect(Collectors.toList());
                        if (columns.isEmpty()) {
                            gui.error("No column nearby.");
                            resetParams();
                            continue;
                        }

                        Gob centerColumn = AUtils.closestGob(columns, gui.map.player().rc);
                        if (centerColumn == null) {
                            continue;
                        }
                        currentAnchorColumn = centerColumn.rc.add(direction.add(directionPerpendicular).mul(11));

                        //Check lines from column
                        int nextLine = checkLinesMined();
                        debug("next line " + nextLine);
                        switch (nextLine) {
                            case 0:
                                stage = 4;
                                break;
                            case 1:
                                stage = 1;
                                break;
                            case 2:
                                stage = 2;
                                break;
                            case 3:
                                stage = 3;
                                break;
                        }

                        if (stage != 4 && !goToNearestColumn()) {
                            sleep(1000);
                            continue;
                        }

                    } else if (stage == 1) {
                        //mine forward
                        if (mineLine(currentAnchorColumn, direction, 10, true))
                            stage = 0;
                    } else if (stage == 2) {
                        //mine to the side
                        if (mineLine(currentAnchorColumn, directionPerpendicular, mineToTheLeft ? 10 : 1, false))
                            stage = 0;
                    } else if (stage == 3) {
                        //mine to the other side
                        if (mineLine(currentAnchorColumn, directionPerpendicular.inv(), 12, false))
                            stage = 0;
                    } else if (stage == 4) {
                        //building phase

                        //check if we need to build milestone
                        List<Gob> milestones = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/road/milestone-stone-m").stream().map(p -> p.gob).collect(Collectors.toList());
                        Coord2d playercood = gui.map.player().rc;
                        Gob closestMilestone = AUtils.closestGob(milestones, playercood);
                        if (autoRoadActive && closestMilestone != null && closestMilestone.rc.dist(currentAnchorColumn) > 19 * 11) {
                            stage = 5;
                        } else {
                            Coord nextColumnAdd = direction.mul(11).mul(10);
                            Coord2d nextColumnPos = currentAnchorColumn.add(nextColumnAdd);
                            if (checkForNearbyColumn(nextColumnPos)) {
                                pfL(nextColumnPos);
                                stage = 0;
                            } else {
//                                System.out.println("build next");
                                buildNextColumn(currentAnchorColumn);
                            }
                        }
                    } else if (stage == 5) {
                        buildMilestone();
                    } else if (stage == -1) {
                        fleeInOppositeDirection();
                    }
                }
                sleep(500);
            }
        } catch (InterruptedException e) {
//            System.out.println("Tunneler interrupted..");
        }

    }

    private void buildMilestone() throws InterruptedException {
        List<Gob> milestones = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/road/milestone-stone-m").stream().map(p -> p.gob).collect(Collectors.toList());
        Coord2d playercood = gui.map.player().rc;
        Gob closestMilestone = AUtils.closestGob(milestones, playercood);
        Coord addcoord = new Coord(0, 0).sub(directionPerpendicular).mul(11);
        Coord2d newMilestonePos = currentAnchorColumn.add(addcoord);

        if (closestMilestone.rc.dist(currentAnchorColumn) < 5 * 11) {
            stage = 0;
        }
        //mine tile where we put milestone
        else if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(newMilestonePos, map))) {
            pfL(currentAnchorColumn.add(direction.mul(11)));
            //Mine spot
            AUtils.clickUiButton("paginae/act/mine", gui);
            gui.map.wdgmsg("sel", newMilestonePos.floor(MCache.tilesz), newMilestonePos.floor(MCache.tilesz), 0);
            int timeout = 0;
            while (timeout < 100 && !TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(newMilestonePos, map))) {
                timeout++;
                sleep(100);
            }
        }
        //find rocks
        else if (!hasRocksInInv(5)) {
            findRocks();
        } else if (isClearPath(playercood, closestMilestone.rc)) {
            gui.map.wdgmsg("click", Coord.z, closestMilestone.rc.floor(posres), 3, 0, 0, (int) closestMilestone.id, closestMilestone.rc.floor(posres), 0, -1);
            sleep(500);
            Button extendButton = null;
            try {
                Window milestoneWindow = gui.getwnd("Milestone");
                if (milestoneWindow != null) {
                    for (Widget wi = milestoneWindow.lchild; wi != null; wi = wi.prev) {
                        if (wi instanceof Button) {
                            if (((Button) wi).text.text.equals("Extend"))
                                extendButton = (Button) wi;
                        }
                    }
                }
            } catch (NullPointerException e) {
            }
            if (extendButton != null) {
                extendButton.click();
                sleep(500);

                //Walk to CCC with milestone on cursor
                gui.map.wdgmsg("place", currentAnchorColumn.floor(posres), milestoneRot, 1, 2);
                int timeout = 0;
                while (gui.map.player().rc.dist(currentAnchorColumn) > 11 && timeout < 100) {
                    timeout++;
                    sleep(100);
                }

                Coord2d buildPos = new Coord2d(newMilestonePos.x, newMilestonePos.y);

                gui.map.wdgmsg("place", buildPos.floor(posres), milestoneRot, 1, 0);
                sleep(1000);
                AUtils.activateSign("Milestone", gui);
                waitBuildingConstruction("gfx/terobjs/road/milestone-stone-m");
                gui.map.wdgmsg("click", Coord.z, playercood.floor(posres), 3, 0);

            } else {
                gui.error("error when trying to extend road, the closest milestone cannot be extended!");
            }


        } else {
            Coord2d milestonevision = closestMilestone.rc.add(directionPerpendicular.x * 11, directionPerpendicular.y * 11);
            pfL(milestonevision);
            AUtils.leftClick(gui, milestonevision);
            Thread.sleep(100);
            while (gui.map.player().getv() > 0 && !isClearPath(playercood, closestMilestone.rc.add(new Coord2d(directionPerpendicular).mul(5)))) {
                Thread.sleep(100);
            }
        }
    }

    private void clearhand() {
        if (!gui.hand.isEmpty()) {
            if (gui.vhand != null) {
                gui.vhand.item.wdgmsg("drop", Coord.z);
            }
        }
        AUtils.rightClick(gui);
    }

    private boolean checkForNearbyColumn(Coord2d pos) {
        columns = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/column").stream().map(p -> p.gob).collect(Collectors.toList());
        for (Gob gob : columns) {
            if (gob.rc.dist(pos) < 44) {
                return true;
            }
        }
        return false;
    }

    private void buildNextColumn(Coord2d fromCenter) throws InterruptedException {
        debug("building column");
        AUtils.rightClick(gui);
        Coord addCoord = direction.mul(11).mul(10);
        Coord2d columnCoord = fromCenter.add(addCoord);
        Coord columnOffset = directionPerpendicular.inv().mul(11);
        List<Gob> constructions = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/consobj").stream().map(p -> p.gob).collect(Collectors.toList());
        try {
            constructions.sort((gob1, gob2) -> (int) (gob1.rc.dist(gui.map.player().rc) - gob2.rc.dist(gui.map.player().rc)));
        } catch (Exception ignored) {}

        if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(columnCoord.add(columnOffset), map))) {
            debug("mine for construction");
            pfL(columnCoord);
            AUtils.clickUiButton("paginae/act/mine", gui);
            gui.map.wdgmsg("sel", columnCoord.add(columnOffset).floor(MCache.tilesz), columnCoord.add(columnOffset).floor(MCache.tilesz), 0);
            int timeout = 0;
            while (timeout < 100 && !TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(columnCoord.add(columnOffset), map))) {
                timeout++;
                sleep(100);
            }
        } else if (!hasRocksInInv(30)) {
            findRocks();
        } else if (!constructions.isEmpty()) {
            debug("continue construction");
            if (!AUtils.hasWnd("Stone Column", gui)) {
                pfL(columnCoord);
                Gob closeConstr = constructions.get(0);
                gui.map.wdgmsg("click", Coord.z, closeConstr.rc.floor(posres), 3, 0, 0, (int) closeConstr.id, closeConstr.rc.floor(posres), 0, -1);
                sleep(1000);

                // Check if window interrupted
                int attempts = 0;
                while (!AUtils.hasWnd("Stone Column", gui) && attempts < 5) {
                    sleep(3000);
                    gui.map.wdgmsg("click", Coord.z, closeConstr.rc.floor(posres), 3, 0, 0, (int) closeConstr.id, closeConstr.rc.floor(posres), 0, -1);
                    attempts++;
                }
            }
            AUtils.activateSign("Stone Column", gui);
            waitBuildingConstruction("gfx/terobjs/column");
        } else {
            debug("new construction");
            pfL(columnCoord);
            AUtils.clickUiButton("paginae/bld/column", gui);
            sleep(300);
            Coord2d buildPos = columnCoord.add(columnOffset).floord(MCache.tilesz).mul(MCache.tilesz).add(MCache.tilesz.div(2));
            gui.map.wdgmsg("place", buildPos.floor(posres), 0, 1, 0);
            sleep(1000);
            AUtils.activateSign("Stone Column", gui);
            waitBuildingConstruction("gfx/terobjs/column");
        }
    }

    private void waitBuildingConstruction(String name) throws InterruptedException {
        debug("waitBuildingConstruction " + name);
        boolean constructionComplete = false;
        int timeout = 0;
        int previousRockCount = -1;
        int stuckCounter = 0;

        while (timeout < 50 && !constructionComplete) {
            int currentRocks = getStoneCount();  // Get current stone count
            if (!hasRocksInInv(0)) {
                debug("Out of rocks during construction");
                gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
                sleep(1000);
                return;
            }

            // Check if stones are actually being used
            if (currentRocks == previousRockCount) {
                stuckCounter++;
                if (stuckCounter > 5) {  // If stuck for too long, close window and retry
                    debug("Construction appears stuck, closing window");
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
                    sleep(1000);
                    return;
                }
            } else {
                stuckCounter = 0;
            }

            previousRockCount = currentRocks;

            if (checkIfConstructed(name)) {
                constructionComplete = true;
            }

            sleep(1000);  // Longer sleep to allow construction to progress
            timeout++;
        }

        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
        sleep(1000);
    }

    private int getStoneCount() {
        int count = 0;
        for (PBotItem wItem : PBotUtils.playerInventory(ui).getInventoryContentsDontStack()) {
            if (TileStatic.SUPPORT_MATERIALS.contains(wItem.gitem.getres().basename())) {
                count++;
            }
        }
        return count;
    }

    private boolean checkIfConstructed(String name) {
        debug("check " + name);
        List<Gob> colmns = PBotGobAPI.findObjectsByNames(gui.ui, name).stream().map(r -> r.gob).collect(Collectors.toList());
        return AUtils.closestGob(colmns, gui.map.player().rc).rc.dist(gui.map.player().rc) < 20;
    }

    private void findRocks() throws InterruptedException {
        debug("finding rocks");
        List<Gob> gobs = AUtils.getAllGobs(gui);
        Coord2d playerC = gui.map.player().rc;
        Coord2d crossSectionCenter = currentAnchorColumn;  // Center point reference
        Coord2d supportTile = currentAnchorColumn.add(direction.mul(11).mul(10)).add(directionPerpendicular.inv().mul(11));

        try {
            gobs.sort(Comparator.comparingDouble(gob -> gob.rc.dist(playerC)));
        } catch (Exception e) {}

        debug("pathing to cross section at " + crossSectionCenter);
        // Path to cross-section first, but only try once
        if (!pathAndWait(crossSectionCenter)) {
            debug("failed to path to cross section");
            return;
        }
        sleep(1000);  // Wait a bit after reaching cross-section

        // Try stones near main line first
        debug("searching for rocks in main line area");
        if (tryRocksInRange(gobs, crossSectionCenter, 44)) {
            debug("found rocks in main line, returning via cross-section");
            sleep(500);
            pathAndWait(crossSectionCenter);
            sleep(500);
            pathAndWait(supportTile);
            return;
        }

        // Check side areas if main line had no rocks
        debug("searching for rocks in side areas");
        if (tryRocksInRange(gobs, crossSectionCenter, 132)) {
            debug("found rocks in side area, returning via cross-section");
            sleep(500);
            pathAndWait(crossSectionCenter);
            sleep(500);
            pathAndWait(supportTile);
            return;
        }

        debug("no rocks found in range, returning to support");
        // If no rocks found, return to support tile
        pathAndWait(crossSectionCenter);
        sleep(500);
        pathAndWait(supportTile);
    }

    private boolean tryRocksInRange(List<Gob> gobs, Coord2d position, double range) throws InterruptedException {
        List<Gob> rocksInRange = new ArrayList<>();

        // Pre-filter valid rocks
        for (Gob gob : gobs) {
            if (TileStatic.SUPPORT_MATERIALS.contains(gob.getres().basename())) {
                Coord2d tc = new Coord2d(gob.rc.floor(MCache.tilesz)).mul(MCache.tilesz).add(MCache.tilesz.div(2, 2));
                if (position.dist(tc) < range) {
                    rocksInRange.add(gob);
                }
            }
        }

        rocksInRange.sort((a, b) -> (int)(position.dist(a.rc) - position.dist(b.rc)));

        for (Gob gob : rocksInRange) {
            Coord2d tc = new Coord2d(gob.rc.floor(MCache.tilesz)).mul(MCache.tilesz).add(MCache.tilesz.div(2, 2));
            debug("attempting to collect rock at " + tc);

            // Verify rock still exists before attempting collection
            if (gob.getres() == null || !TileStatic.SUPPORT_MATERIALS.contains(gob.getres().basename())) {
                debug("rock no longer exists, skipping");
                continue;
            }

            if (pathAndWait(tc)) {
                if (gui.map.player().rc.dist(tc) < 11) {
                    debug("collecting rock");
                    gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                    sleep(3000);

                    // Verify collection was successful
                    if (hasRocksInInv(0)) {
                        return true;
                    }
                }
            }
            sleep(1000);
        }
        return false;
    }

    private boolean hasRocksInInv(int num) {
        int rocksAmount = 0;
        for (PBotItem wItem : PBotUtils.playerInventory(ui).getInventoryContentsDontStack()) {
            if (TileStatic.SUPPORT_MATERIALS.contains(wItem.gitem.getres().basename())) {
                rocksAmount++;
            }
        }
        debug("rocks " + rocksAmount + " of " + num);
        return rocksAmount > num || gui.maininv.getFreeSpace() == 0;
    }

    private Integer checkLinesMined() {
        debug("checkLinesMined");
        Coord dir1 = direction; //forward
        Coord dir2 = directionPerpendicular; //to the side
        Coord dir3 = directionPerpendicular.inv(); //to the other side

        if ((!checkLineMined(currentAnchorColumn, dir2, mineToTheLeft ? 10 : 1)) && (mineToTheRight || mineToTheLeft)) {
            return 2;
        } else if ((!checkLineMined(currentAnchorColumn, dir3, 12)) && mineToTheRight) {
            return 3;
        } else if (!checkLineMined(currentAnchorColumn, dir1, 10)) {
            return 1;
        }

        return 0;
    }

    private boolean checkLineMined(Coord2d place, Coord dir, int length) {
        debug("checkLineMined " + place + " " + dir + " " + length);
        for (int i = 0; i <= length; i++) {
            Coord dirmul = dir.mul(11).mul(i);
            if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(place.add(dirmul), map))) {
                return false;
            }
        }
        return true;
    }

    private boolean mineLine(Coord2d place, Coord dir, int length, boolean last) throws InterruptedException {
        debug("mineline " + place + " " + dir + " " + length + " " + last);
        Coord end = dir.mul(11).mul(length);
        Coord dirmul;
        Coord2d mineplace = new Coord2d(0, 0);
        int tilesToMine = 0;
        for (int i = 0; i <= length; i++) {
            dirmul = dir.mul(11).mul(i);
            mineplace = place.add(dirmul);
            if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(mineplace, map))) {
                tilesToMine++;
            }
        }
        debug("tilesToMine " + tilesToMine);
        if (tilesToMine > 0) {
            AUtils.clickUiButton("paginae/act/mine", gui);
            if (!((PBotGobAPI.player(ui).getPoses().contains("gfx/borka/pickan") || PBotGobAPI.player(ui).getPoses().contains("gfx/borka/choppan")) || PBotGobAPI.player(ui).getPoses().contains("gfx/borka/drinkan"))) {
                gui.map.wdgmsg("sel", place.floor(MCache.tilesz), place.add(end).floor(MCache.tilesz), 0);
            }
            sleep(4000);
            return false;
        } else {
            if (!last) {
                pfL(currentAnchorColumn);
            }
            return true;
        }
    }

    private boolean isClearPath(Coord2d fromd, Coord2d tod) {
        debug("isClearPath " + fromd + " " + tod);
        Coord2d direction = tod.sub(fromd);
        double dirLen = fromd.dist(tod);
        if (dirLen < 21) {
            return true;
        }
        Coord2d directionNorm = direction.div(dirLen);
        for (int i = 1; i < dirLen / 11; i++) {
            Coord2d addCoord = directionNorm.mul(11).mul(i);
            if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(fromd.add(addCoord), map))) {
                return false;
            }
        }
        return true;
    }

    private boolean goToNearestColumn() throws InterruptedException {
        if (TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(currentAnchorColumn, map))) {
            pfL(currentAnchorColumn);
            return true;
        } else if (currentAnchorColumn.dist(gui.map.player().rc) < 22) {
            AUtils.clickUiButton("paginae/act/mine", gui);
            gui.map.wdgmsg("sel", gui.map.player().rc.floor(MCache.tilesz), currentAnchorColumn.floor(MCache.tilesz), 0);
            sleep(500);
            return true;
        } else {
            gui.error("cannot not walk to nearest column, try to mine around it");
            return false;
        }
    }

    private void fleeInOppositeDirection() {
        try {
            columns = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/column").stream().map(p -> p.gob).collect(Collectors.toList());
            Gob centerColumn = AUtils.closestGob(columns, gui.map.player().rc);
            currentAnchorColumn = centerColumn.rc.add(new Coord(direction).add(directionPerpendicular).mul(11));
            if (TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(currentAnchorColumn, map))) {
                Thread.sleep(500);
                pfL(currentAnchorColumn.sub(direction.mul(2 * 11)));
                Coord addDirection = direction.inv().mul(11).mul(12);

                centerColumn = AUtils.closestGob(columns, currentAnchorColumn.add(addDirection));

                currentAnchorColumn = centerColumn.rc.add(new Coord(direction).add(directionPerpendicular).mul(11));
                pfL(currentAnchorColumn.sub(direction.mul(2 * 11)));
            } else {
                gui.error("PANIC! Cannot find a path to flee.");
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void changeDirection(int dir) {
        debug("changedir " + dir);
        if (dir == 1) {
            resetParams();
            miningDirectionLabel.settext("N");
            direction = new Coord(0, -1);
            directionPerpendicular = new Coord(-1, 0);
            milestoneRot = 19115;
        } else if (dir == 2) {
            resetParams();
            miningDirectionLabel.settext("E");
            direction = new Coord(1, 0);
            directionPerpendicular = new Coord(0, -1);
            milestoneRot = -30037;
        } else if (dir == 3) {
            resetParams();
            miningDirectionLabel.settext("S");
            direction = new Coord(0, 1);
            directionPerpendicular = new Coord(1, 0);
            milestoneRot = -13653;
        } else if (dir == 4) {
            resetParams();
            miningDirectionLabel.settext("W");
            direction = new Coord(-1, 0);
            directionPerpendicular = new Coord(0, 1);
            milestoneRot = 2731;
        }
    }

    private void resetParams() {
        debug("resetparam");
        map = gui.map.glob.map;
        stage = 0;
        columns = PBotGobAPI.findObjectsByNames(gui.ui, "gfx/terobjs/column").stream().map(p -> p.gob).collect(Collectors.toList());
        Gob centerColumn = AUtils.closestGob(columns, gui.map.player().rc);
        if (centerColumn != null) {
            currentAnchorColumn = centerColumn.rc.add(new Coord(direction).add(directionPerpendicular).mul(11));
        }
        mineButton.change("Start Mining");
        autoMineActive = false;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.tunnelerBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        if (gui.tunnelerBotThread != null) {
            gui.tunnelerBotThread.interrupt();
            gui.tunnelerBotThread = null;
        }
        this.destroy();
    }

    private void debug(String str) {
        gui.debuglog.append(str, Color.WHITE);
    }

    private void pfL(Coord2d c) throws InterruptedException {
        FlowerMenu.setNextSelection();
        gui.map.showSpecialMenu(c);
        debug("pf to " + c);
        PBotUtils.pfLeftClick(gui.ui, c.x, c.y);
        // Wait for pathfinding to initialize
        sleep(500);

        // Wait for movement to complete or timeout
        int timeout = 0;
        while (gui.map.player().getv() > 0 && timeout < 100) {
            sleep(100);
            timeout++;
        }
    }

    private boolean waitForMovement() throws InterruptedException {
        int timeout = 0;
        while (gui.map.player().getv() > 0 && timeout < 50) {
            sleep(100);
            timeout++;
        }
        return timeout < 50;
    }

    private boolean pathAndWait(Coord2d target) throws InterruptedException {
        int attempts = 0;
        while (attempts < 3) {
            try {
                pfL(target);
                int timeout = 0;
                while (gui.map.player().getv() > 0 && timeout < 50) {
                    sleep(100);
                    timeout++;
                }
                if (gui.map.player().rc.dist(target) < 11) {
                    return true;
                }
            } catch (Exception e) {
                debug("pathfinding failed: " + e.getMessage());
            }
            attempts++;
            sleep(1000);
        }
        return false;
    }
}
