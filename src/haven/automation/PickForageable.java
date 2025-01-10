package haven.automation;


import haven.CheckListboxItem;
import haven.Config;
import haven.Coord2d;
import haven.FlowerMenu;
import haven.GameUI;
import haven.Gob;
import haven.Loading;
import haven.Resource;
import haven.Utils;
import haven.sloth.gob.HeldBy;
import modification.configuration;
import modification.resources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import static haven.OCache.posres;

public class PickForageable implements Runnable {
    private GameUI gui;
    public static final HashSet<String> gates = new HashSet(Arrays.asList("brickwallgate", "brickgate", "brickbiggate", "drystonewallgate", "drystonewallbiggate", "palisadegate", "palisadebiggate", "polegate", "polebiggate"));

    public PickForageable(GameUI gui) {
        this.gui = gui;
    }

    private Coord2d center;

    public PickForageable(GameUI gui, Coord2d center) {
        this.gui = gui;
        this.center = center;
    }

    @Override
    public void run() {
        Gob herb = null;
            Gob player = gui.map.player();
            if (player == null)
                return;//player is null, possibly taking a road, don't bother trying to do all of the below.
            HeldBy held = player.getattr(HeldBy.class);
//            List<PBotGob> gobs = new ArrayList<>();
//            gobs.stream().filter(gob -> gob.getResname().contains("stockpile") && gob.getResname().endsWith("wblock")).collect(Collectors.toList());
            for (Gob gob : gui.map.glob.oc.getallgobs()) {
                if (player == gob)
                    continue;
                if (held != null && held.holder == gob.id)
                    continue; //don't evaluate tamed horses
                Resource res = null;
                boolean gate = false;
//                boolean cart = false;
                try {
                    res = gob.getres();
                } catch (Loading l) {
                }
                if (res != null) {
//                    CheckListboxItem itm = Config.icons.get(res.basename());
//                    Boolean hidden = Boolean.FALSE;
                    if (!Config.disablegatekeybind)
                        gate = gates.contains(res.basename());
//                    if (!Config.disablecartkeybind)
//                        cart = res.basename().equals("cart");
//                    if (itm == null) {
//                        itm = Config.oldicons.get(res.basename());
//                        if (itm == null) {
//                            hidden = null;
//                        }
//                    }
//                    else if (itm.selected)
//                        hidden = Boolean.TRUE;

                    try {
                        if (gate) {
                            for (Gob.Overlay ol : gob.ols) {
                                if (ol.sdt != null) {
                                    String resname = (this.gui.map.glob.sess.getres(Utils.uint16d(ol.sdt.rbuf, 0)).get()).basename();
                                    if (Config.disablevgatekeybind && resname.equals("visflag")) {
                                        gate = false;
                                    }
                                }
                            }
                        }
                    } catch (Exception fucknulls) {
                        fucknulls.printStackTrace();
                    }

                    boolean matches = false;
                    try {
                        for (String act : resources.customQuickActions.keySet()) {
                            if (resources.customQuickActions.get(act)) {
                                Pattern pattern = Pattern.compile(act);
                                if (pattern.matcher(res.name).matches()) {
                                    matches = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (matches || gate) {
                        double distFromPlayer = gob.rc.dist(center != null ? center : gui.map.player().rc);
                        if (distFromPlayer <= configuration.quickradius * 11 && (herb == null || distFromPlayer < herb.rc.dist(center != null ? center : gui.map.player().rc)))
                            herb = gob;
                    }
//                    if (gob.type == Type.SMALLANIMAL) {
//                        double distFromPlayer = gob.rc.dist(gui.map.player().rc);
//                        if (distFromPlayer <= 20 * 11 && (herb == null || distFromPlayer < herb.rc.dist(gui.map.player().rc)))
//                            herb = gob;
//                    }
                }
            }
        if (herb == null)
            return;
        if (configuration.quickactionauto)
            FlowerMenu.setNextSelection();
        gui.map.wdgmsg("click", herb.sc, herb.rc.floor(posres), 3, 0, 0, (int) herb.id, herb.rc.floor(posres), 0, -1);
        gui.map.pllastcc = herb.rc;

        if (herb.getres() != null) {
            CheckListboxItem itm = Config.autoclusters.get(herb.getres().name);
            if (itm != null && itm.selected)
                gui.map.startMusselsPicker(herb);

           /* if ((herb.getres().basename().contains("mussel") || herb.getres().basename().contains("oyster")) && Config.autopickmussels)
                gui.map.startMusselsPicker(herb);
            if (herb.getres().basename().contains("clay-gray") && Config.autopickclay)
                gui.map.startMusselsPicker(herb);
            if (herb.getres().basename().contains("goosebarnacle") && Config.autopickbarnacles)
                gui.map.startMusselsPicker(herb);
            if (herb.getres().basename().contains("cattail") && Config.autopickcattails)
                gui.map.startMusselsPicker(herb);*/
        }
    }
}
