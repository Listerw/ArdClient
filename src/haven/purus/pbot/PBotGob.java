package haven.purus.pbot;

import haven.Composite;
import haven.Coord;
import haven.Coord2d;
import haven.Drawable;
import haven.GAttrib;
import haven.Gob;
import haven.GobHitbox;
import haven.Indir;
import haven.LinMove;
import haven.Loading;
import haven.MapView;
import haven.ResData;
import haven.ResDrawable;
import haven.Resource;
import haven.UI;
import haven.purus.gobText;
import haven.sloth.script.pathfinding.Hitbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static haven.OCache.posres;

public class PBotGob {

    public Gob gob;
    public final UI ui;

    PBotGob(Gob gob) {
        this.gob = gob;
        this.ui = gob.glob.ui.get();
    }


    /**
     * Check if stockpile is full
     *
     * @return True if stockpile is full, else false
     */
    public boolean stockpileIsFull() {
        if (gob.sdt() == 31)
            return true;
        else
            return false;
    }


    /**
     * Itemact with gob, to fill trough with item in hand for example
     *
     * @param mod 1 = shift, 2 = ctrl, 4 = alt, 3 = shift + ctrl (all)
     */
    public void itemClick(int mod) {
        ui.gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), mod, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    /**
     * Add cool hovering text above gob
     *
     * @param text   text to add
     * @param height height that the hext hovers at
     * @return Returns id of the text, which can be used to remove the text with removeGobText
     */
    public int addGobText(String text, int height) {
        Gob.Overlay ol = new Gob.Overlay(new gobText(text, height));
        gob.addol(ol);
        return ol.id;
    }

    /**
     * Remove an added hovering text from gob that was added with addGobText
     *
     * @param id Id of the gobtext
     */
    public void removeGobText(int id) {
        gob.ols.remove(gob.findol(id));
        ui.gui.map.glob.oc.changed(gob);
    }

    /**
     * Click the gob
     *
     * @param button 1 = Left click, 3 = Right click
     * @param mod    0 = no modifier, 1 = shift, 2 = ctrl, 4 = alt
     * @param meshId can be a door, roasting spit etc.
     * @param olid   gob overlay to click, for example roasting spit.
     */
    public void doClick(int button, int mod, int meshId, int olid) {//{pc, mc.floor(posres), clickb, modflags} + {0, (int) gob.id, gob.rc.floor(posres), 0, -1}
        ui.gui.map.wdgmsg("click", ui.mc.max(Coord.z), gob.rc.floor(posres), button, mod, olid != 0 ? 1 : 0, (int) gob.id, gob.rc.floor(posres), olid, meshId);
        ui.gui.map.pllastcc = gob.rc;
    }

    /**
     * Click the gob
     *
     * @param button 1 = Left click, 3 = Right click
     * @param mod    0 = no modifier, 1 = shift, 2 = ctrl, 4 = alt
     * @param meshId can be a door, roasting spit etc.
     */
    public void doClick(int button, int mod, int meshId) {
        doClick(button, mod, meshId, 0);
    }

    /**
     * Click the gob
     *
     * @param button 1 = Left click, 3 = Right click
     * @param mod    0 = no modifier, 1 = shift, 2 = ctrl, 4 = alt
     */
    public void doClick(int button, int mod) {
        doClick(button, mod, -1, 0);
    }

    /**
     * Get id of the gob
     *
     * @return Id of the gob
     */
    public long getGobId() {
        return gob.id;
    }

    /**
     * Get stage of the crop
     *
     * @return Stage of the crop
     */
    public int getCropStage() {
        return gob.getStage();
    }

    /**
     * Returns the name of the gobs resource file, or null if not found
     *
     * @return Name of the gob
     */
    public String getResname() {
        try {
            if (gob.getres() != null)
                return gob.getres().name;
        } catch (Loading l) {
        }
        return null;
    }

    /**
     * Get name of window for gob from gobWindowMap
     *
     * @param gob Gob to get inventory of
     * @return Inventory window name
     */
    public static String windowNameForGob(Gob gob) {
        if (gob.getres() == null)
            return "Window name for gob found!!";
        else
            return (String) PBotGobAPI.gobWindowMap.get(gob.getres().name);
    }

    /**
     * Returns rc-coords of the gob
     *
     * @return Coords of the gob
     */
    public Coord2d getRcCoords() {
        return gob.rc;
    }

    /**
     * Highlights the gob by Alt+Clicking it
     */
    public void highlightGob() {
        doClick(0, 4);
    }

    /**
     * Toggle whether the gob should be marked or not
     */
    public void toggleMarked() {
        if (MapView.markedGobs.contains(gob.id))
            MapView.markedGobs.remove(gob.id);
        else
            MapView.markedGobs.add(gob.id);
        ui.gui.map.glob.oc.changed(gob);
    }

    /**
     * Click a gob with pathfinder, with given button, wait until pathfinder is finished
     *
     * @param btn    1 = left click, 3 = right click
     * @param mod    1 = shift, 2 = ctrl, 4 = alt
     * @param meshId meshid to click
     * @return True if path was found, or false if not
     */
    public boolean pfClick(int btn, int mod, int meshId) {
        ui.gui.map.purusPfRightClick(gob, meshId, btn, mod, null);
        try {
            ui.gui.map.pastaPathfinder.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (ui.gui.map) {
            return ui.gui.map.foundPath;
        }
    }

    /**
     * Click a gob with pathfinder, with given button, wait until pathfinder is finished
     *
     * @param btn 1 = left click, 3 = right click
     * @param mod 1 = shift, 2 = ctrl, 4 = alt
     * @return True if path was found, or false if not
     */
    public boolean pfClick(int btn, int mod) {
        return pfClick(btn, mod, -1);
    }

    /**
     * Check if the object is moving
     *
     * @return Returns true if gob is moving, false otherwise
     */
    public boolean isMoving() {
        if (gob.getv() == 0)
            return false;
        else
            return true;
    }

    /**
     * Get speed of this gob if it is moving
     *
     * @return Speed of the gob, -1 if not moving object
     */
    public double getSpeed() {
        LinMove lm = gob.getLinMove();
        if (lm == null)
            return -1;
        else
            return lm.getv();
    }

    /**
     * Returns resnames of poses of this gob if it has any poses
     *
     * @return Resnames of poses
     */
    public ArrayList<String> getPoses() {
        ArrayList<String> ret = new ArrayList<>();
        Drawable d = gob.getattr(Drawable.class);

        if (d instanceof Composite) {
            Composite comp = (Composite) d;
            Collection<ResData> poses = comp.prevposes;
            if (poses != null)
                for (ResData rd : poses) {
                    try {
                        ret.add(rd.res.get().name);
                    } catch (Loading l) {

                    }
                }
        }
        return ret;
    }

    /**
     * Get overlays of the gob, get meshId with getOverlyId
     *
     * @return List containing resnames of the overlays
     */
    public List<String> getOverlayNames() {
        List<String> ret = new ArrayList<>();
        for (Gob.Overlay ol : gob.ols) {
            try {
                Indir<Resource> olires = ol.res;
                String name = null;
                if (olires != null) {
                    name = olires.get().name;
                } else if (ol.spr != null && ol.spr.res != null) {
                    name = ol.spr.res.name;
                }
                if (name != null)
                    ret.add(name);
            } catch (Loading l) {
            }
        }
        return ret;
    }

    public List<Gob.Overlay> getOverlays() {
        return (new ArrayList<>(gob.ols));
    }

    /**
     * Return meshId of the overlay with given resname
     *
     * @param overlayName Exact match
     * @return Meshid of the overlay -1 if not found
     */
    public int getOverlayId(String overlayName) {
        for (Gob.Overlay ol : gob.ols) {
            try {
                Indir<Resource> olires = ol.res;
                if (olires.get().name.equals(overlayName)) {
                    return ol.id;
                }
            } catch (Loading l) {
            }
        }
        return -1;
    }

    /**
     * Sdt may tell information about things such as tanning tub state, crop stage etc.
     *
     * @return sdt of the gob, -1 if not found
     */
    public int getSdt() {
        try {
            Resource res = gob.getres();
            if (res != null) {
                GAttrib rd = gob.getattr(ResDrawable.class);
                return ((ResDrawable) rd).sdt.peekrbuf(0);
            }
        } catch (Loading l) {
        }
        return -1;
    }

    /**
     * Is the gob knocked out/dead
     *
     * @return True if animal is knocked out, false if not
     */
    public boolean isKnocked() {
        return gob.isDead();
    }

    /**
     * Repeat shift + itemact until all similar items from the inventory have been clicked insinde trough etc.
     * May result in valuable items accidentally being put in troughs etc...
     */
    public void itemClickAll() {
        Object[] args = {Coord.z, gob.rc.floor(posres), 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1};
        ui.gui.map.lastItemactClickArgs = args;
        ui.gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    /**
     * Get distance between gobs
     *
     * @return double number
     */
    public double dist(final PBotGob pgob) {
        return (getRcCoords().dist(pgob.getRcCoords()));
    }

    /**
     * Get hitboxes
     *
     * @return array of hitboxes
     */
    public GobHitbox.BBox[] getHitboxes() throws Loading {
        return (GobHitbox.getBBox(gob));
    }

    /**
     * Get first hitbox
     *
     * @return first hitbox of gob
     */
    public GobHitbox.BBox getHitbox() throws Loading {
        GobHitbox.BBox[] bboxes = getHitboxes();
        return (bboxes.length > 0 ? bboxes[0] : null);
    }

    /**
     * Get first hitbox
     *
     * @return first hitbox of gob
     */
    public Hitbox getHitbox_() {
        Hitbox[] bboxes = getHitboxes_();
        return (bboxes.length > 0 ? bboxes[0] : null);
    }

    /**
     * Get hitboxes
     *
     * @return array of hitboxes
     */
    public Hitbox[] getHitboxes_() {
        return (Hitbox.hbfor(gob));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PBotGob pBotGob = (PBotGob) o;
        return Objects.equals(gob, pBotGob.gob) && Objects.equals(ui, pBotGob.ui);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gob, ui);
    }

    public static PBotGob of(Gob gob) {
        return (new PBotGob(gob));
    }
}
