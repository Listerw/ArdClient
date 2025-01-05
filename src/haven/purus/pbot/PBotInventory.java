package haven.purus.pbot;

import haven.Coord;
import haven.Inventory;
import haven.UI;
import haven.WItem;
import haven.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PBotInventory {

    public Inventory inv;
    public final UI ui;

    private final int invSize = UI.scale(33);

    public PBotInventory(Inventory inv) {
        this.inv = inv;
        this.ui = inv.ui;
    }

    public int inventoryID() {
        return (inv.wdgid());
    }

    /**
     * Return all items that the inventory contains
     *
     * @return List of items in the inventory
     */
    public List<PBotItem> getInventoryContents() {
        List<PBotItem> items = new ArrayList<>();
        for (Widget witm = inv.child; witm != null; witm = witm.next) {
            synchronized (witm) {
                if (witm instanceof WItem) {
                    WItem witem = (WItem) witm;
                    items.add(new PBotItem(witem));
                }
            }
        }
        return items;
    }

    public List<PBotItem> getInventoryContentsDontStack() {
        List<PBotItem> items = new ArrayList<>();
        for (Widget witm = inv.child; witm != null; witm = witm.next) {
            synchronized (witm) {
                if (witm instanceof WItem) {
                    WItem witem = (WItem) witm;

                    PBotItem item = new PBotItem(witem);
                    if (item.isStack()) {
                        items.addAll(item.getStackContents());
                    } else {
                        items.add(item);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Returns a list of items with specific regex pattern(s) from the inventory
     *
     * @param pattern Regex pattern(s) matching item names
     * @return List of items with name matching at least one of the given patterns
     */
    public List<PBotItem> getInventoryItemsByNames(String... pattern) {
        List<PBotItem> items = new ArrayList<>();
        List<Pattern> patterns = Arrays.stream(pattern).map(Pattern::compile).collect(Collectors.toList());
        for (PBotItem item : getInventoryContents()) {
            String name = item.getName();
            if (name == null)
                continue;
            for (Pattern p : patterns) {
                if (p.matcher(name).matches())
                    items.add(item);
            }
        }
        return items;
    }

    public List<PBotItem> getInventoryItemsByNames(List<String> pattern) {
        return (getInventoryItemsByNames(pattern.toArray(new String[0])));
    }

    public PBotItem getInventoryItemByNames(String... pattern) {
        List<Pattern> patterns = Arrays.stream(pattern).map(Pattern::compile).collect(Collectors.toList());
        for (PBotItem item : getInventoryContents()) {
            String name = item.getName();
            if (name == null)
                continue;
            for (Pattern p : patterns) {
                if (p.matcher(name).matches())
                    return (item);
            }
        }
        return (null);
    }

    public PBotItem getInventoryItemByNames(List<String> pattern) {
        return (getInventoryItemByNames(pattern.toArray(new String[0])));
    }

    /**
     * @param pattern Regex pattern(s) matching item resnames
     * @return List of items with resname matching at least one of the given patterns
     */
    public List<PBotItem> getInventoryItemsByResnames(String... pattern) {
        List<PBotItem> items = new ArrayList<>();
        List<Pattern> patterns = Arrays.stream(pattern).map(Pattern::compile).collect(Collectors.toList());
        for (PBotItem item : getInventoryContents()) {
            String name = item.getResname();
            for (Pattern p : patterns) {
                if (p.matcher(name).matches())
                    items.add(item);
            }
        }
        return items;
    }

    public List<PBotItem> getInventoryItemsByResnames(List<String> pattern) {
        return (getInventoryItemsByResnames(pattern.toArray(new String[0])));
    }

    public PBotItem getInventoryItemByResnames(String... pattern) {
        List<Pattern> patterns = Arrays.stream(pattern).map(Pattern::compile).collect(Collectors.toList());
        for (PBotItem item : getInventoryContents()) {
            String name = item.getResname();
            for (Pattern p : patterns) {
                if (p.matcher(name).matches())
                    return (item);
            }
        }
        return (null);
    }

    public PBotItem getInventoryItemByResnames(List<String> pattern) {
        return (getInventoryItemByResnames(pattern.toArray(new String[0])));
    }

    public List<PBotItem> getInventoryContainsResnames(List<String> resnames) {
        List<PBotItem> items = new ArrayList<>();
        for (PBotItem item : getInventoryContents()) {
            String name = item.getResname();
            for (String s : resnames) {
                if (name != null && name.contains(s))
                    items.add(item);
            }
        }
        return items;
    }

    /**
     * Finds an item with certain location from the inventory
     *
     * @param xLoc x-coordinate of the item location in inventory
     * @param yLoc y-coordinate of the item location in inventory
     * @return Null if not found
     */
    public PBotItem getItemFromInventoryAtLocation(int xLoc, int yLoc) {
        for (Widget w = inv.child; w != null; w = w.next) {
            if (w instanceof WItem) {
                WItem itm = (WItem) w;
                if (itm.c.div(invSize).x == xLoc && itm.c.div(invSize).y == yLoc)
                    return new PBotItem(itm);
            }
        }
        return null;
    }


    // Returns coordinates for placement if the given inventory matrix has space for item, 1 = grid reserved, 0 = grid free O(n*m) where n and m dimensions of matrix, null if no space
    public Coord freeSpaceForItem(PBotItem itm) {
        return (freeSpaceForItem(itm.gitem.size()));
    }

    public Coord freeSpaceForItem(Coord size) {
        short[][] inventoryMatrix = containerMatrix();
        int[][] d = new int[inventoryMatrix.length][];
        {
            for (int i = 0; i < d.length; i++) {
                d[i] = new int[inventoryMatrix[i].length];
            }
        }

        int sizeX = size.x;
        int sizeY = size.y;
        for (int i = 0; i < inventoryMatrix.length; i++) {
            for (int j = 0; j < inventoryMatrix[i].length; j++) {
                if (inventoryMatrix[i][j] != 0)
                    d[i][j] = 0;
                else
                    d[i][j] = (j == 0 ? 1 : d[i][j - 1] + 1);
            }
        }

        for (int i = 0; i < inventoryMatrix[0].length; i++) {
            int curLen = 0;
            for (int j = 0; j < inventoryMatrix.length; j++) {
                if (d[j][i] >= sizeY)
                    curLen++;
                else
                    curLen = 0;
                if (curLen >= sizeX)
                    return (new Coord(j, i));
            }
        }

        return (null);
    }

    public Coord freeSpaceForItemAlt(PBotItem itm) {
        return (freeSpaceForItemAlt(itm.gitem.size()));
    }

    public Coord freeSpaceForItemAlt(Coord size) {
        short[][] inventoryMatrix = containerMatrix();

        int sizeX = size.x;
        int sizeY = size.y;

        for (int row = 0; row < inventoryMatrix.length; row++)
            for (int collumn = 0; collumn < inventoryMatrix[row].length; collumn++) {
                boolean ok = true;
                next:
                for (int y = 0; y < sizeY; y++)
                    for (int x = 0; x < sizeX; x++)
                        if (row + sizeX - 1 >= inventoryMatrix.length || collumn + sizeY - 1 >= inventoryMatrix[row].length || inventoryMatrix[row + x][collumn + y] == 1) {
                            ok = false;
                            break next;
                        }
                if (ok) {
                    return (Coord.of(row, collumn));
                }
            }

        return (null);
    }

    public int itemCountAtFreeSpace(PBotItem itm) {
        return (itemCountAtFreeSpace(itm.gitem.size()));
    }

    public int itemCountAtFreeSpace(Coord size) {
        int count = 0;
        short[][] inventoryMatrix = containerMatrix();

        int sizeX = size.x;
        int sizeY = size.y;

        for (int row = 0; row < inventoryMatrix.length; row++)
            for (int collumn = 0; collumn < inventoryMatrix[row].length; collumn++) {
                boolean ok = true;
                next:
                for (int y = 0; y < sizeY; y++)
                    for (int x = 0; x < sizeX; x++)
                        if (row + sizeX - 1 >= inventoryMatrix.length || collumn + sizeY - 1 >= inventoryMatrix[row].length || inventoryMatrix[row + x][collumn + y] == 1) {
                            ok = false;
                            break next;
                        }
                if (ok) {
                    count++;
                    for (int x = 0; x < sizeX; x++)
                        for (int y = 0; y < sizeY; y++)
                            inventoryMatrix[row + x][collumn + y] = 1;
                }
            }

        return (count);
    }


    // Returns a matrix representing the container and items inside, 1 = item in this grid, 0 = free grid, -1 = invalid
    public short[][] containerMatrix() {
        short[][] ret = new short[inv.isz.x][inv.isz.y];
        for (PBotItem item : getInventoryContents()) {
            int xSize = item.gitem.size().x;
            int ySize = item.gitem.size().y;
            int xLoc = item.witem.c.div(invSize).x;
            int yLoc = item.witem.c.div(invSize).y;

            for (int i = 0; i < xSize; i++) {
                for (int j = 0; j < ySize; j++) {
                    ret[i + xLoc][j + yLoc] = 1;
                }
            }
        }
        int mo = 0;
        for (int i = 0; i < inv.isz.y; i++) {
            for (int j = 0; j < inv.isz.x; j++) {
                if ((inv.sqmask != null) && inv.sqmask[mo++]) ret[j][i] = -1;
            }
        }
        return (ret);
    }

    public boolean isFreeSlot(Coord coord) {
        return (containerMatrix()[coord.x][coord.y] == 0);
    }

    /**
     * Drop item from the hand to given slot in inventory
     *
     * @param coord Slot to drop the item to
     */
    public void dropItemToInventory(Coord coord) {
        inv.wdgmsg("drop", coord);
    }

    public boolean dropItemToInventory(Coord coord, int limit) {
        int cycles = 0;
        int sleeptime = 25;
        inv.wdgmsg("drop", coord);
        while (PBotUtils.getItemAtHand(ui) != null) {
            if (cycles == limit) {
                return (false);
            } else {
                PBotUtils.sleep(sleeptime);
                cycles += sleeptime;
            }
        }
        return (true);
    }

    public void transferLastItemFromInventoryToPlayerInventory() {
        this.inv.wdgmsg("invxf", PBotUtils.playerInventory(ui).inventoryID(), 1);
    }

    public void transferLastItemToInventoryFromPlayerInventory() {
        PBotUtils.playerInventory(ui).inv.wdgmsg("invxf", inventoryID(), 1);
    }

    /**
     * Amount of free slots in the inventory
     *
     * @return Amount of free inventory slots
     */
    public int freeSlotsInv() {
        int takenSlots = 0;
        for (Widget i = inv.child; i != null; i = i.next) {
            if (i instanceof WItem) {
                WItem buf = (WItem) i;
                takenSlots += buf.size().x * buf.size().y;
            }
        }
        int allSlots = inv.getMaxSlots();
        return allSlots - takenSlots;
    }

    public Coord getFreeSlot() {
        return inv.getFreeSlot();
    }

    public void wdgmsg(String msg, Object... args) {
        inv.wdgmsg(msg, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PBotInventory that = (PBotInventory) o;
        return Objects.equals(inv, that.inv) && Objects.equals(ui, that.ui);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inv, ui);
    }
}
