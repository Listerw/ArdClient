package haven.automation;


import haven.Audio;
import haven.Button;
import haven.CheckBox;
import haven.Coord;
import haven.Coord2d;
import haven.Glob;
import haven.Gob;
import haven.GobCustomSprite;
import haven.GobHealth;
import haven.GobHighlight;
import haven.Label;
import haven.Loading;
import haven.MCache;
import haven.Resource;
import haven.Text;
import haven.UI;
import haven.Utils;
import haven.Widget;
import haven.Window;
import haven.purus.pbot.PBotGobAPI;
import haven.purus.pbot.PBotUtils;
import haven.sloth.gob.Mark;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MinerAlert extends Window {
    private int countiron, countgold, countsilver, countcopper, counttin, countlead, countleadglance, countbasaslt, countcinnabar, countdolomite, countfeldspar, countflint, countgneiss, countgranite, counthornblende;
    private int countlimestone, countmarble, countporphyry, countquartz, countsandstone, countschist;
    private int countcassiterite, countchalcopyrite, countmalachite, countilmenite, countlimonite, counthematite, countmagnetite, countgalena, countargentite;
    private int countpetzite, countsylvanite, countnagyagite, counthornsilver, countslimes, countslimestotal;

    private static final int TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;
    private Thread runner;
    public Boolean terminate = false;
    public static int delay = 5000, maxmarks = 50;
    public Gob gob;
    private Button runbtn, stopbtn, mutebtn;
    private final Label labelcountiron, labelcountgold, labelcountcinnabar, labelcountsilver, labelcounttin, labelcountlead, labelcountcopper, labelcountmagnetite, labelcounthematite, labelcountslimes, labelcountslimestotal;
    private static final Text.Foundry infof = new Text.Foundry(Text.sans, UI.scale(10)).aa(true);
    private double lasterror = 0;
    public List<Gob> slimecount = new ArrayList<>();
    private static final Resource goldsfx = Resource.local().loadwait("sfx/Zelda");
    private static final Resource silversfx = Resource.local().loadwait("sfx/gold");
    private static final Resource supportalertsfx = Resource.local().loadwait("custom/sfx/omni/Z_OOT_Navi_WatchOut");
    private Boolean audiomute; // quarter is 25% damage, half is 50% damage
    private CheckBox SupportsQuarter, SupportsHalf, SupplortsLoose, MarkTiles;// quarter is 25% damage, half is 50% damage
    private List<String> reslist = Arrays.asList("gfx/tiles/rocks/cassiterite", "gfx/tiles/rocks/chalcopyrite", "gfx/tiles/rocks/malachite", "gfx/tiles/rocks/ilmenite", "gfx/tiles/rocks/limonite",
            "gfx/tiles/rocks/hematite", "gfx/tiles/rocks/magnetite", "gfx/tiles/rocks/galena", "gfx/tiles/rocks/argentite", "gfx/tiles/rocks/hornsilver", "gfx/tiles/rocks/petzite", "gfx/tiles/rocks/sylvanite",
            "gfx/tiles/rocks/nagyagite", "gfx/tiles/rocks/cinnabar", "gfx/tiles/rocks/leadglance");
    private String looserock = "gfx/terobjs/looserock";
    private Set<Long> ignoredloose = Collections.synchronizedSet(new HashSet<>());

    private final HashMap<String, String> smeltchance = new HashMap<String, String>(15) {{
        put("cassiterite", "30% Tin");
        put("chalcopyrite", "8% Copper 4% Iron");
        put("cinnabar", "12% Quicksilver");
        put("malachite", "20% Copper");
        put("peacockore", "30% Copper");
        put("ilmenite", "6% Iron");
        put("limonite", "12% Iron");
        put("hematite", "20% Iron");
        put("magnetite", "30% Iron");
        put("galena", "10% Silver");
        put("argentite", "20% Silver");
        put("hornsilver", "30% Silver");
        put("petzite", "10% Gold");
        put("sylvanite", "20% Gold");
        put("nagyagite", "25% Gold");
    }};

    private final Map<String, GobCustomSprite> cachedSpriteList = new HashMap<>();

    private GobCustomSprite getCachedSprite(String text, int time) {
        GobCustomSprite gcs = cachedSpriteList.get(text);
        if (gcs == null) {
            gcs = new GobCustomSprite(text, time);
        } else {
            gcs.setLife(time);
        }
        return (gcs);
    }

    public MinerAlert() {
        super(UI.scale(220, 320), "Miner Alert", "Miner Alert");
        int yvalue = UI.scale(17);
        int yvalue2 = UI.scale(8);
        int yy = UI.scale(20);
        audiomute = false;

        final Label labeliron = new Label("Number of Iron tiles visible.", infof);
        add(labeliron, new Coord(UI.scale(10), yvalue2));
        labelcountiron = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountiron, new Coord(UI.scale(65), yvalue));

        final Label labeltin = new Label("Number of Tin tiles visible.", infof);
        add(labeltin, new Coord(UI.scale(10), yvalue2 += yy));
        labelcounttin = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcounttin, new Coord(UI.scale(65), yvalue += yy));

        final Label labellead = new Label("Number of Lead tiles visible.", infof);
        add(labellead, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountlead = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountlead, new Coord(UI.scale(65), yvalue += yy));

        final Label labelcopper = new Label("Number of Copper tiles visible.", infof);
        add(labelcopper, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountcopper = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountcopper, new Coord(UI.scale(65), yvalue += yy));

        final Label labelgold = new Label("Number of Gold tiles visible.", infof);
        add(labelgold, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountgold = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountgold, new Coord(UI.scale(65), yvalue += yy));

        final Label labelsilver = new Label("Number of Silver Tiles visible.", infof);
        add(labelsilver, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountsilver = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountsilver, new Coord(UI.scale(65), yvalue += yy));

        final Label labelcinnabar = new Label("Number of Cinnabar Tiles visible.", infof);
        add(labelcinnabar, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountcinnabar = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountcinnabar, new Coord(UI.scale(65), yvalue += yy));

        final Label labelmagnetite = new Label("Number of Black Ore Tiles visible.", infof);
        add(labelmagnetite, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountmagnetite = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountmagnetite, new Coord(UI.scale(65), yvalue += yy));

        final Label labelhematite = new Label("Number of Bloodstone Tiles visible.", infof);
        add(labelhematite, new Coord(UI.scale(10), yvalue2 += yy));
        labelcounthematite = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcounthematite, new Coord(UI.scale(65), yvalue += yy));

        final Label labelslimes = new Label("Number of Slimes Visible", infof);
        add(labelslimes, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountslimes = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountslimes, new Coord(UI.scale(65), yvalue += yy));

        final Label labelslimestotal = new Label("Number of Slimes Total", infof);
        add(labelslimestotal, new Coord(UI.scale(10), yvalue2 += yy));
        labelcountslimestotal = new Label("0", Text.num12boldFnd, Color.WHITE);
        add(labelcountslimestotal, new Coord(UI.scale(65), yvalue += yy));

        SupportsQuarter = new CheckBox("Stop Mining at <25% HP Supports");
        add(SupportsQuarter, UI.scale(10), yvalue += yy);
        SupportsHalf = new CheckBox("Stop Mining at <50% HP Supports");
        add(SupportsHalf, UI.scale(10), yvalue += yy);
        SupplortsLoose = new CheckBox("Stop Mining at discover a loose rock");
        add(SupplortsLoose, UI.scale(10), yvalue += yy);
        MarkTiles = new CheckBox("Mark ore tiles with arrows above them.");
        MarkTiles.set(true);
        add(MarkTiles, UI.scale(10), yvalue += yy);
        runbtn = new Button(UI.scale(100), "Run") {
            @Override
            public void click() {
                terminate = false;
                runner = new Thread(new runner(), "Miner Alert");
                runner.start();
            }
        };

        stopbtn = new Button(UI.scale(100), "Stop") {
            @Override
            public void click() {
                cbtn.show();
                terminate = true;
            }
        };

        mutebtn = new Button(UI.scale(100), "Mute") {
            @Override
            public void click() {
                audiomute = !audiomute;
                PBotUtils.debugMsg(ui, "Mute status : " + audiomute, Color.white);
            }
        };
        add(mutebtn, new Coord(UI.scale(35), yvalue += yy));
        runbtn.click();
        pack();
    }

    private class runner implements Runnable {
        @Override
        public void run() {
            long lasttick5 = 0;
            long lasttick500 = 0;
            while (ui.gui != null && ui.gui.getwnd("Miner Alert") != null) {
                long curtick = System.currentTimeMillis();
                //PBotUtils.sleep(5000);//sleep 5 seconds every iteration, no reason to update more than once every 5 seconds.
                try {
                    if (ui == null || ui.gui == null)
                        break;
                    if (curtick - lasttick500 > 500) {
                        lasttick500 = curtick;

                        maxmarks = 50;
                        countiron = 0;
                        countgold = 0;
                        countsilver = 0;
                        Glob g = ui.gui.map.glob;
                        Gob player = ui.gui.map.player();
                        List<Gob> allGobs = PBotUtils.getGobs(ui);
                        List<Gob> list = new ArrayList<>();
                        List<Gob> supportlist = new ArrayList<>();
                        List<Gob> looses = new ArrayList<>();

                        for (Gob allGob : allGobs) {
                            try {
                                if (allGob.type.toString().contains("SUPPORT"))
                                    supportlist.add(allGob);
                                Resource res = allGob.getres();
                                if (res.name.endsWith("greenooze") && !allGob.isDead()) {
                                    list.add(allGob);
                                    if (!slimecount.contains(allGob))
                                        slimecount.add(allGob);
                                }
                                if (res.name.equals(looserock)) {
                                    looses.add(allGob);
                                }
                            } catch (NullPointerException | Loading e) {
                            }
                        }

                        if (curtick - lasttick5 > 5000) {
                            lasttick5 = curtick;

                            countslimes = list.size();
                            while (PBotUtils.player(ui) == null)
                                PBotUtils.sleep(10); //sleep if player is null, teleporting through a road?
                            Coord pltc = new Coord((int) player.getc().x / 11, (int) player.getc().y / 11);

                            if (SupportsHalf.a || SupportsQuarter.a) {//if support alerts toggled, resolve mine supports and HP
                                for (Gob support : supportlist) {
                                    double distFromPlayer = support.rc.dist(PBotUtils.player(ui).rc);
                                    if (distFromPlayer <= 13 * 11) {    //support is less than or equal to 13 tiles from current player position, check it's HP
                                        if (support.getattr(GobHealth.class) != null && support.getattr(GobHealth.class).hp <= 2f / 4 && SupportsHalf.a) {
                                            PBotUtils.debugMsg(ui, "Detected mine support at 50% or less HP", Color.ORANGE);
                                            support.addol(new Mark(4500));
                                            support.delattr(GobHighlight.class);
                                            support.setattr(new GobHighlight(support));
                                            if (PBotGobAPI.player(ui).getPoses().contains("gfx/borka/choppan") || PBotGobAPI.player(ui).getPoses().contains("gfx/borka/pickan")) {
                                                ui.root.wdgmsg("gk", 27);
                                                Audio.play(supportalertsfx);
                                            }
                                        } else if (support.getattr(GobHealth.class) != null && support.getattr(GobHealth.class).hp <= 1f / 4 && SupportsQuarter.a) {
                                            PBotUtils.debugMsg(ui, "Detected mine support at 25% or less HP less than 13 tiles away", Color.RED);
                                            support.addol(new Mark(4500));
                                            support.delattr(GobHighlight.class);
                                            support.setattr(new GobHighlight(support));
                                            if (PBotGobAPI.player(ui).getPoses().contains("gfx/borka/choppan") || PBotGobAPI.player(ui).getPoses().contains("gfx/borka/pickan")) {
                                                ui.root.wdgmsg("gk", 27);
                                                Audio.play(supportalertsfx);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int x = -44; x < 44; x++) {
                                for (int y = -44; y < 44; y++) {
                                    int t = g.map.gettile(pltc.sub(x, y));
                                    Resource res = g.map.tilesetr(t);

                                    if (res == null)
                                        continue;
                                    String name = res.name;

                                    if (MarkTiles.a && reslist.contains(name) && maxmarks != 0) {
                                        final Coord2d mc = player.rc.sub(new Coord2d((x - 1) * 11, (y - 1) * 11)); //no clue why i have to subtract 1 tile here to get it to line up.
                                        final Coord tc = mc.floor(MCache.tilesz);
                                        final Coord2d tcd = mc.div(MCache.tilesz);

                                        ui.sess.glob.map.getgridto(tc).ifPresent(grid -> {
                                            final Coord2d offset = tcd.sub(new Coord2d(grid.ul));
                                            ui.sess.glob.map.getgrido(grid.id).ifPresent(grid2 -> {
                                                final Coord2d mc2 = new Coord2d(grid2.ul).add(offset.x, offset.y).mul(MCache.tilesz);
                                                maxmarks--;
                                                final Gob g2 = ui.sess.glob.oc.new ModdedGob(mc2, 0);
                                                g2.addol(new Mark(4500));
                                                g2.addol(getCachedSprite(res.basename().substring(0, 1).toUpperCase() + res.basename().substring(1) + " " + smeltchance.get(res.basename()), 4000));
                                            });
                                        });
                                    }

                                    if (name.equals("gfx/tiles/rocks/leadglance")) {
                                        countlead++;
                                        countleadglance++;
                                    }
                                    if (name.equals("gfx/tiles/rocks/cassiterite")) {
                                        counttin = counttin + 1;
                                        countcassiterite = countcassiterite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/chalcopyrite")) {
                                        countiron = countiron + 1;
                                        countcopper = countcopper + 1;
                                        countchalcopyrite = countchalcopyrite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/malachite")) {
                                        countcopper = countcopper + 1;
                                        countmalachite = countmalachite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/ilmenite")) {
                                        countiron = countiron + 1;
                                        countilmenite = countilmenite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/limonite")) {
                                        countiron = countiron + 1;
                                        countlimonite = countlimonite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/hematite")) {
                                        countiron = countiron + 1;
                                        counthematite = counthematite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/magnetite")) {
                                        countiron = countiron + 1;
                                        countmagnetite = countmagnetite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/galena")) {
                                        countsilver = countsilver + 1;
                                        countgalena = countgalena + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/argentite")) {
                                        countsilver = countsilver + 1;
                                        countargentite = countargentite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/hornsilver")) {
                                        countsilver = countsilver + 1;
                                        counthornsilver = counthornsilver + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/petzite")) {
                                        countgold = countgold + 1;
                                        countpetzite = countpetzite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/sylvanite")) {
                                        countgold = countgold + 1;
                                        countsylvanite = countsylvanite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/nagyagite")) {
                                        countgold = countgold + 1;
                                        countnagyagite = countnagyagite + 1;
                                    }
                                    if (name.equals("gfx/tiles/rocks/cinnabar")) {
                                        countcinnabar = countcinnabar + 1;
                                    }

                                }
                            }
                            labelcountiron.settext(countiron + "");
                            labelcountcopper.settext(countcopper + "");
                            labelcounttin.settext(counttin + "");
                            labelcountlead.settext(countlead + "");
                            labelcountgold.settext(countgold + "");
                            labelcountsilver.settext(countsilver + "");
                            labelcountmagnetite.settext(countmagnetite + "");
                            labelcounthematite.settext(counthematite + "");
                            labelcountslimes.settext(countslimes + "");
                            labelcountslimestotal.settext(slimecount.size() + "");
                            labelcountcinnabar.settext(countcinnabar + "");
                            if (countgold > 0) {
                                double now = Utils.rtime();
                                if (now - lasterror > 45) {
                                    lasterror = now;
                                    PBotUtils.debugMsg(ui, "Gold Visible on screen!!", Color.green);
                                    if (!audiomute)
                                        Audio.play(goldsfx);
                                }
                            }
                            if (countcinnabar > 0) {
                                double now = Utils.rtime();
                                if (now - lasterror > 45) {
                                    PBotUtils.debugMsg(ui, "Cinnabar visible on screen!!", Color.green);
                                    lasterror = now;
                                }
                            }
                            if (countsilver > 0) {
                                double now = Utils.rtime();
                                if (now - lasterror > 15) {
                                    PBotUtils.debugMsg(ui, "Silver visible on screen!!", Color.green);
                                    if (!audiomute) {
                                        Audio.play(silversfx);
                                    }
                                    lasterror = now;
                                }
                            }
                            if (countslimes > 0) {
                                double now = Utils.rtime();
                                if (now - lasterror > 15) {
                                    PBotUtils.sysLogAppend(ui, "Slime number spawned : " + list.size(), "white");
                                    lasterror = now;
                                }
                            }
                            countiron = 0;
                            counttin = 0;
                            countlead = 0;
                            countcopper = 0;
                            countgold = 0;
                            countsilver = 0;
                            counthematite = 0;
                            countmagnetite = 0;
                            countcinnabar = 0;
                            countslimes = 0;
                        }

                        if (SupplortsLoose.a) {
                            boolean already = false;
                            for (Gob loose : looses) {
                                if (!ignoredloose.contains(loose.id)) {
                                    PBotUtils.debugMsg(ui, "Detected loose rock. Beware!", Color.RED);
                                    loose.addol(new Mark(4500));
                                    loose.delattr(GobHighlight.class);
                                    loose.setattr(new GobHighlight(loose));
                                    ignoredloose.add(loose.id);
                                    if (!already) {
                                        if (PBotGobAPI.player(ui).getPoses().contains("gfx/borka/choppan") || PBotGobAPI.player(ui).getPoses().contains("gfx/borka/pickan")) {
                                            ui.root.wdgmsg("gk", 27);
                                            Audio.play(supportalertsfx);
                                        }
                                        already = true;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception lolloadingerrors) {
                    lolloadingerrors.printStackTrace();
                }
                PBotUtils.sleep(100);
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn)
            reqdestroy();
        else
            super.wdgmsg(sender, msg, args);
    }

    public void close() {
        reqdestroy();
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        if (key == 27) {//ignore escape key
            return true;
        }
        return super.type(key, ev);
    }

}

