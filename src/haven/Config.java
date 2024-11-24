/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.error.ErrorHandler;
import haven.purus.Iconfinder;
import haven.sloth.util.ObservableMap;
import modification.configuration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Config {
    public static String revVersion = "1.0";
    public static String prefspec = "hafen";
    public static final Properties jarprops = getjarprops();
    public final Properties localprops = getlocalprops();
    public static final File HOMEDIR = new File("").getAbsoluteFile();
    public static boolean dumpcode = Utils.getprop("haven.dumpcode", "off").equals("on");
    public static final boolean iswindows = System.getProperty("os.name").startsWith("Windows");
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static String resdir = Config.Variable.prop("haven.resdir", System.getenv("HAFEN_RESDIR")).get();
    public static String authcrt = Config.Variable.prop("haven.authcrt", System.getenv("HAFEN_AUTHCRT")).get();
    public static String authuser = null;
    public static String authserv = null;
    public static String defserv = Config.Variable.prop("haven.defserv", "game.havenandhearth.com").get();
    public static URI resurl = Config.Variable.propu("haven.resurl", "https://game.havenandhearth.com/hres/").get();
    public static URI newresurl = Config.Variable.propu("haven.newresurl", "https://game.havenandhearth.com/res/").get();
    public static boolean dbtext = false;
    public static boolean profile = false;
    public static boolean par = true;
    public static boolean simplemap = Utils.getprefb("simplemap", false);
    public static boolean rawrzmap = Utils.getprefb("rawrzmap", false);
    public static boolean trollexmap = Utils.getprefb("trollexmap", false);
    public static boolean disableBlackOutLinesOnMap = Utils.getprefb("disableBlackOutLinesOnMap", false);
    public static boolean mapscale = Utils.getprefb("mapscale", false);
    public static boolean profilegpu = false;
    public static boolean nopreload = false;
    public static int mainport = 1870;
    public static int authport = 1871;
    public static boolean skybox = Utils.getprefb("skybox", true);
    public static boolean showmetertext = Utils.getprefb("showmetertext", false);
    public static boolean savecutlery = Utils.getprefb("savecutlery", true);
    public static boolean lowerterraindistance = Utils.getprefb("lowerterraindistance", false);
    public static boolean noloadscreen = Utils.getprefb("noloadscreen", false);
    public static String mapperUrl = Utils.getpref("mapperUrl", Utils.getpref("navigationEndpoint", "http://example.com"));
    public static boolean mapperHashName = Utils.getprefb("mapperHashName", true);
    public static boolean mapperEnabled = Utils.getprefb("mapperEnabled", true);
    //    public static boolean vendanMapv4 = Utils.getprefb("vendan-mapv4", false);
//    public static boolean vendanGreenMarkers = Utils.getprefb("vendan-mapv4-green-markers", false);
//    public static boolean enableNavigationTracking = Utils.getprefb("enableNavigationTracking", false);
//    public static boolean sendCustomMarkers = Utils.getprefb("sendCustomMarkers", false);
    public static URI screenurl = Config.Variable.propu("haven.screenurl", "http://game.havenandhearth.com/mt/ss").get();
    public static boolean hideflocomplete = Utils.getprefb("hideflocomplete", false);
    public static boolean mapdrawparty = Utils.getprefb("mapdrawparty", false);
    public static boolean mapdrawquests = Utils.getprefb("mapdrawquests", true);
    public static boolean mapdrawflags = Utils.getprefb("mapdrawflags", false);
    public static boolean hideflovisual = Utils.getprefb("hideflovisual", false);
    public static boolean sessiondisplay = Utils.getprefb("sessiondisplay", true);
    public static boolean longtooltips = Utils.getprefb("longtooltips", true);
    public static boolean straightcavewall = Utils.getprefb("straightcavewall", false);
    public static boolean avatooltips = Utils.getprefb("avatooltips", false);
    public static boolean showkinnames = Utils.getprefb("showkinnames", true);
    public static boolean savemmap = Utils.getprefb("savemmap", false);
    public static boolean studylock = Utils.getprefb("studylock", false);
    public static boolean chatsave = Utils.getprefb("chatsave", false);
    public static boolean chattimestamp = Utils.getprefb("chattimestamp", true);
    public static boolean flatcupboards = Utils.getprefb("flatcupboards", true);
    public static boolean flatwalls = Utils.getprefb("flatwalls", false);
    public static boolean flatcaves = Utils.getprefb("flatcaves", false);
    public static boolean biganimals = Utils.getprefb("biganimals", false);
    public static boolean showquality = Utils.getprefb("showquality", true);
    public static boolean showroadendpoint = Utils.getprefb("showroadendpoint", true);
    public static boolean showroadmidpoint = Utils.getprefb("showroadmidpoint", false);
    public static boolean qualitywhole = Utils.getprefb("qualitywhole", true);
    public static int badcamsensitivity = Utils.getprefi("badcamsensitivity", 5);
    public static List<LoginData> logins = new ArrayList<>();
    public static boolean shooanimals = Utils.getprefb("shooanimals", false);
    public static boolean horseautorun = Utils.getprefb("horseautorun", true);
    public static boolean mapshowgrid = Utils.getprefb("mapshowgrid", false);
    public static boolean mapshowviewdist = Utils.getprefb("mapshowviewdist", false);
    public static boolean disabletiletrans = Utils.getprefb("disabletiletrans", false);
    public static boolean slothgrid = Utils.getprefb("slothgrid", false);
    public static int slothgridoffset = Utils.getprefi("slothgridoffset", 0);
    public static boolean itemmeterbar = Utils.getprefb("itemmeterbar", false);
    public static boolean showprogressperc = Utils.getprefb("showprogressperc", true);
    public static int fpsLimit = Utils.getprefi("fpsLimit", 200);
    public static int fpsBackgroundLimit = Utils.getprefi("fpsBackgroundLimit", 200);

    //Item Coloring
    public static boolean qualitycolor = Utils.getprefb("qualitycolor", false);
    public static boolean insaneitem = Utils.getprefb("insaneitem", false);
    public static boolean transferquality = Utils.getprefb("transferquality", false);
    public static boolean transfercolor = Utils.getprefb("transfercolor", false);
    public static boolean dropcolor = Utils.getprefb("dropcolor", false);
    public static Color uncommon = new Color(30, 255, 0);
    public static Color rare = new Color(0, 112, 221);
    public static Color epic = new Color(163, 53, 238);
    public static Color legendary = new Color(255, 128, 0);

    public static int uncommonq = Utils.getprefi("uncommonq", 40);
    public static int rareq = Utils.getprefi("rareq", 90);
    public static int epicq = Utils.getprefi("epicq", 160);
    public static int legendaryq = Utils.getprefi("legendaryq", 250);


    public static boolean quickslots = Utils.getprefb("quickslots", true);
    public static boolean disablequickslotdrop = Utils.getprefb("disablequickslotdrop", true);
    public static boolean quickbelt = Utils.getprefb("quickbelt", false);
    public static boolean statuswdgvisible = Utils.getprefb("statuswdgvisible", false);

    public static boolean errorsounds = Utils.getprefb("errorsounds", true);
    public static boolean cleavesound = Utils.getprefb("cleavesound", true);
    public static boolean chatsounds = Utils.getprefb("chatsounds", true);
    public static boolean discordsounds = Utils.getprefb("discordsounds", true);
    public static boolean realmchatalerts = Utils.getprefb("realmchatalerts", false);

    public static double sfxchipvol = Utils.getprefd("sfxchipvol", 0.9);
    public static double sfxquernvol = Utils.getprefd("sfxquernvol", 0.9);
    public static double sfxdoorvol = Utils.getprefd("sfxdoorvol", 0.9);
    public static double sfxfirevol = Utils.getprefd("sfxfirevol", 1.0);
    public static double sfxclapvol = Utils.getprefd("sfxclapvol", 1.0);
    public static double sfxbeehivevol = Utils.getprefd("sfxbeehivevol", 1.0);
    public static double sfxchatvol = Utils.getprefd("sfxchatvol", 1.0);
    public static double sfxcauldronvol = Utils.getprefd("sfxcauldronvol", 1.0);
    public static double sfxwhistlevol = Utils.getprefd("sfxwhistlevol", 1.0);
    public static double sfxdingvol = Utils.getprefd("sfxdingvol", 1.0);
    public static boolean showcraftcap = Utils.getprefb("showcraftcap", true);
    public static boolean showgobhp = Utils.getprefb("showgobhp", true);
    public static boolean showgobquality = Utils.getprefb("showgobquality", true);
    public static boolean showplantgrowstage = Utils.getprefb("showplantgrowstage", false);
    public static boolean showtreegrowstage = Utils.getprefb("showtreegrowstage", false);
    public static boolean showfreshcropstage = Utils.getprefb("showfreshcropstage", false);
    public static boolean notifykinonline = Utils.getprefb("notifykinonline", true);
    public static boolean autosortkinlist = Utils.getprefb("autosortkinlist", true);
    public static boolean showminerad = Utils.getprefb("showminerad", false);
    public static boolean showTroughrad = Utils.getprefb("showTroughrad", false);
    public static boolean showBeehiverad = Utils.getprefb("showBeehiverad", false);
    public static boolean showMoundbedrad = Utils.getprefb("showMoundbedrad", false);
    public static boolean showBarterrad = Utils.getprefb("showBarterrad", false);
    public static boolean showweather = Utils.getprefb("showweather", true);
    public static boolean simplecrops = Utils.getprefb("simplecrops", false);
    public static boolean escclosewindows = Utils.getprefb("escclosewindows", true);
    public static int afklogouttime = Utils.getprefi("afklogouttime", 0);
    public static int autodrinktime = Utils.getprefi("autodrinktime", 5);
    public static boolean simpleforage = Utils.getprefb("simpleforage", false);
    public static boolean showfps = Utils.getprefb("showfps", false);
    public static boolean autohearth = Utils.getprefb("autohearth", false);
    public static boolean runonlogin = Utils.getprefb("runonlogin", false);
    public static boolean autostudy = Utils.getprefb("autostudy", false);
    public static boolean showdmgop = Utils.getprefb("showdmgop", true);
    public static boolean showothercombatinfo = Utils.getprefb("showothercombatinfo", true);
    public static boolean hidegobs = Utils.getprefb("hidegobs", false);
    public static boolean hideuniquegobs = Utils.getprefb("hideuniquegobs", false);
    public static boolean qualitybg = Utils.getprefb("qualitybg", true);
    public static int qualitybgtransparency = Utils.getprefi("qualitybgtransparency", 5);
    public static boolean showwearbars = Utils.getprefb("showwearbars", true);
    public static boolean tilecenter = Utils.getprefb("tilecenter", false);
    public static boolean userazerty = Utils.getprefb("userazerty", false);
    public static boolean hlightcuropp = Utils.getprefb("hlightcuropp", false);
    public static boolean cRackmissing = Utils.getprefb("cRackmissing", false);
    public static boolean reversebadcamx = Utils.getprefb("reversebadcamx", false);
    public static boolean reversebadcamy = Utils.getprefb("reversebadcamy", false);
    public static boolean showservertime = Utils.getprefb("showservertime", false);
    public static boolean enabletracking = Utils.getprefb("enabletracking", false);
    public static boolean enableswimming = Utils.getprefb("enableswimming", false);
    public static boolean autoconnectdiscord = Utils.getprefb("autoconnectdiscord", false);
    public static boolean autoconnectarddiscord = Utils.getprefb("autoconnectarddiscord", false);
    public static boolean enablecrime = Utils.getprefb("enablecrime", false);
    public static boolean enablesiege = Utils.getprefb("enablesiege", true);
    public static boolean resinfo = Utils.getprefb("resinfo", true);
    public static boolean detailedresinfo = Utils.getprefb("detailedresinfo", false);
    public static boolean showanimalrad = Utils.getprefb("showanimalrad", true);
    public static boolean hwcursor = Utils.getprefb("hwcursor", false);
    public static boolean showboundingboxes = Utils.getprefb("showboundingboxes", false);
    public static boolean showgridlines = Utils.getprefb("showgridlines", false);
    public static boolean showcooldown = Utils.getprefb("showcooldown", false);
    public static boolean nodropping = Utils.getprefb("nodropping", false);
    public static boolean nodropping_all = Utils.getprefb("nodropping_all", false);
    public static boolean histbelt = Utils.getprefb("histbelt", false);
    public static boolean dropMinedStones = Utils.getprefb("dropMinedStones", true);
    public static boolean dropMinedOre = Utils.getprefb("dropMinedOre", true);
    public static boolean dropMinedOrePrecious = Utils.getprefb("dropMinedOrePrecious", true);
    public static boolean dropMinedCatGold = Utils.getprefb("dropMinedCatGold", false);
    public static boolean dropMinedSeaShells = Utils.getprefb("dropMinedSeaShells", false);
    public static boolean dropMinedCrystals = Utils.getprefb("dropMinedCrystals", false);
    public static boolean dropMinedQuarryquartz = Utils.getprefb("dropMinedQuarryquartz", false);
    public static boolean dropsmelterstones = Utils.getprefb("dropsmelterstones", true);
    public static boolean showdframestatus = Utils.getprefb("showdframestatus", true);
    public static boolean showcoopstatus = Utils.getprefb("showcoopstatus", true);
    public static boolean hideallicons = Utils.getprefb("hideallicons", false);
    public static boolean stopmapupdate = Utils.getprefb("stopmapupdate", false);
    public static boolean showhutchstatus = Utils.getprefb("showhutchstatus", true);
    public static boolean showrackstatus = Utils.getprefb("showrackstatus", true);
    public static boolean showcupboardstatus = Utils.getprefb("showcupboardstatus", true);
    public static boolean showbarrelstatus = Utils.getprefb("showbarrelstatus", true);
    public static boolean showbarreltext = Utils.getprefb("showbarreltext", false);
    public static boolean showsigntext = Utils.getprefb("showsigntext", false);
    public static boolean showpartialstoragestatus = Utils.getprefb("showpartialstoragestatus", false);
    public static boolean showshedstatus = Utils.getprefb("showshedstatus", true);
    public static boolean enableorthofullzoom = Utils.getprefb("enableorthofullzoom", false);
    public static boolean partycircles = Utils.getprefb("partycircles", false);
    public static boolean kincircles = Utils.getprefb("kincircles", false);
    public static boolean playercircle = Utils.getprefb("playercircle", false);
    public static boolean stranglevinecircle = Utils.getprefb("stranglevinecircle", false);
    public static boolean doubleradius = Utils.getprefb("doubleradius", false);
    public static boolean dungeonkeyalert = Utils.getprefb("dungeonkeyalert", true);
    public static double sfxwhipvol = Utils.getprefd("sfxwhipvol", 0.9);
    public static boolean showarchvector = Utils.getprefb("showarchvector", false);
    public static boolean disabledrinkhotkey = Utils.getprefb("disabledrinkhotkey", false);
    public static boolean disablegatekeybind = Utils.getprefb("disablegatekeybind", false);
    public static boolean disablevgatekeybind = Utils.getprefb("disablevgatekeybind", true);
    public static boolean disablecartkeybind = Utils.getprefb("disablecartkeybind", true);
    public static boolean autologout = Utils.getprefb("autologout", false);
    public static int combatkeys = Utils.getprefi("combatkeys", 0);
    public static boolean logcombatactions = Utils.getprefb("logcombatactions", false);
    public static boolean autopickmussels = Utils.getprefb("autopickmussels", false);
    public static boolean autopickclay = Utils.getprefb("autopickclay", true);
    public static boolean autopickbarnacles = Utils.getprefb("autopickbarnacles", false);
    public static boolean autopickcattails = Utils.getprefb("autopickcattails", false);
    public static boolean DivertPolityMessages = Utils.getprefb("DivertPolityMessages", false);
    public static boolean confirmmagic = Utils.getprefb("confirmmagic", true);
    public static boolean confirmclose = Utils.getprefb("confirmclose", false);
    public static boolean disablemagaicmenugrid = Utils.getprefb("disablemagaicmenugrid", false);
    public static boolean altfightui = Utils.getprefb("altfightui", false);
    public static boolean forcefightfocus = Utils.getprefb("forcefightfocus", false); //only forces focus if anything besides the chat box has focus
    public static boolean forcefightfocusharsh = Utils.getprefb("forcefightfocusharsh", false); //will force fightsess focus no matter what
    public static boolean combshowkeys = Utils.getprefb("combshowkeys", true);
    public static boolean combaltopenings = Utils.getprefb("combaltopenings", true);
    public static boolean studyhist = Utils.getprefb("studyhist", false);
    public static boolean studybuff = Utils.getprefb("studybuff", false);
    public static int zkey = Utils.getprefi("zkey", KeyEvent.VK_Z);
    public static boolean disableterrainsmooth = Utils.getprefb("disableterrainsmooth", false);
    public static boolean temporaryswimming = Utils.getprefb("temporaryswimming", false);
    public static boolean disableelev = Utils.getprefb("disableelev", false);
    public static boolean obviousridges = Utils.getprefb("obviousridges", false);
    //    public static String treeboxclr = Utils.getpref("treeboxclr", "D7FF00");
    public static String discordtoken = Utils.getpref("discordtoken", "Null");
    public static String discordchannel = Utils.getpref("discordchannel", "");
    public static String discordalertstring = Utils.getpref("discordalertstring", "Null");
    public static boolean discorduser = Utils.getprefb("discorduser", false);
    public static boolean discordrole = Utils.getprefb("discordrole", false);
    public static String charname = Utils.getpref("charname", "Null");
    public static String chatalert = Utils.getpref("chatalert", "Null");
    public static String AlertChannel = Utils.getpref("AlertChannel", "Null");
    public static boolean discordchat = Utils.getprefb("", false);//invoked in gameui once you have a char name
    //    public static String discordbotkey = Utils.getpref("discordbotkey", "Null");
    public static boolean highlightpots = Utils.getprefb("highlightpots", false);
    public static boolean abandonrightclick = Utils.getprefb("abandonrightclick", false);
    public static boolean DropEntrails = Utils.getprefb("DropEntrails", false);
    public static boolean DropIntestines = Utils.getprefb("DropIntestines", false);
    public static boolean StarveAlert = Utils.getprefb("StarveAlert", true);
    public static boolean stackwindows = Utils.getprefb("stackwindows", false);
    public static boolean autodrink = Utils.getprefb("autodrink", false);
    public static int autodrinkthreshold = Utils.getprefi("autodrinkthreshold", 80);
    public static boolean DropMeat = Utils.getprefb("DropMeat", false);
    public static boolean DropBones = Utils.getprefb("DropBones", false);
    public static boolean bonsai = Utils.getprefb("bonsai", false);
    public static boolean largetree = Utils.getprefb("largetree", false);
    public static boolean smallworld = Utils.getprefb("smallworld", false);
    public static boolean largetreeleaves = Utils.getprefb("largetree", false);
    public static int fontsizechat = Utils.getprefi("fontsizechat", 14);
    public static int curiotimetarget = Utils.getprefi("curiotimetarget", 1440);
    public static int statgainsize = Utils.getprefi("statgainsize", 1);
    public static int caveinduration = Utils.getprefi("caveinduration", 1);
    public static boolean colorfulcaveins = Utils.getprefb("colorfulcaveins", false);
    public static boolean fontaa = Utils.getprefb("fontaa", false);
    public static boolean usefont = Utils.getprefb("usefont", false);
    public static boolean largeqfont = Utils.getprefb("largeqfont", false);
    public static String font = Utils.getpref("font", "SansSerif");
    public static int fontadd = Utils.getprefi("fontadd", 0);
    public static boolean proximityaggro = Utils.getprefb("proximityaggro", false);
    public static boolean proximityaggropvp = Utils.getprefb("proximityaggropvp", false);
    public static boolean disablemenugrid = Utils.getprefb("disablemenugrid", false);
    public static boolean lockedmainmenu = Utils.getprefb("lockedmainmenu", true);
    public static boolean splitskills = Utils.getprefb("splitskills", true);
    public static boolean pf = false;
    public static String playerposfile;
    public static Double uiscale = getfloat("haven.uiscale", null);
    public static byte[] authck = null;
    //public static String version;
    public static String version = Utils.getpref("version", "1.0");
    public static String newversion;
    public static String Changelog;
    public static String[] Changelogarray;
    public static StringBuffer Changelogbuffer;
    public static String gitrev;
    public static boolean fepmeter = Utils.getprefb("fepmeter", true);
    public static boolean hungermeter = Utils.getprefb("hungermeter", true);
    public static boolean leechdrop = Utils.getprefb("leechdrop", false);
    public static boolean hideTrees = Utils.getprefb("hideTrees", true);
    public static boolean hideLogs = Utils.getprefb("hideLogs", true);
    public static boolean hideStumps = Utils.getprefb("hideStumps", true);
    //hideboulders
    public static boolean hideboulders = Utils.getprefb("hideboulders", false);
    public static boolean hideCrops = Utils.getprefb("hideCrops", true);
    public static boolean hideBushes = Utils.getprefb("hideBushes", true);
    public static boolean showoverlay = Utils.getprefb("showoverlay", true);
    public static boolean disableAllAnimations = Utils.getprefb("disableAllAnimations", false);
    public static boolean hidecalendar = Utils.getprefb("hidecalendar", false);
    public static int smatSupportsred = Utils.getprefi("smatSupportsred", 0);
    public static int smatSupportsgreen = Utils.getprefi("smatSupportsgreen", 255);
    public static int smatSupportsblue = Utils.getprefi("smatSupportsblue", 0);
    public static String confid = "ArdClient";
    public static boolean elitecombatanimal = Utils.getprefb("disableAllAnimations", true);
    public static final boolean isUpdate;
    private static String username, playername;
    public static boolean showPBot = Utils.getprefb("showPBot", true);
    //    public static boolean showPBotOld = Utils.getprefb("showPBotOld", true);
    public static double alertsvol = Utils.getprefd("alertsvol", 0.8);
    public static boolean chatalarm = Utils.getprefb("chatalarm", true);
    public static double chatalarmvol = Utils.getprefd("chatalarmvol", 0.8);
    public static boolean timersalarm = Utils.getprefb("timersalarm", false);
    public static boolean alarmonce = Utils.getprefb("alarmonce", false);
    public static boolean timersort = Utils.getprefb("timersort", false);
    public static double timersalarmvol = Utils.getprefd("timersalarmvol", 0.8);
    public static boolean discordplayeralert = Utils.getprefb("discordplayeralert", false);
    public static boolean discordalarmalert = Utils.getprefb("discordalarmalert", false);
    public static Map<String, String> alarmsfxlist = Collections.synchronizedMap(new HashMap<String, String>() {
        @Override
        public String put(final String key, final String value) {
            String ret = super.put(key, value);
            Utils.setpref(key + "sfx", value);
            return (ret);
        }

        @Override
        public String get(final Object key) {
            String ret = super.get(key);
            if (ret == null)
                ret = Utils.getpref(key + "sfx", "None");
            return (ret);
        }
    });
    static {
        alarmsfxlist.putAll(Utils.<String, String>map()
                .put("alarmunknown", Utils.getpref("alarmunknownsfx", "custom/sfx/omni/OhShitItsAGuy"))
                .put("alarmred", Utils.getpref("alarmredsfx", "custom/sfx/omni/Siren"))
                .put("alarmstudy", Utils.getpref("alarmstudysfx", "custom/sfx/omni/Study"))
                .put("alarmcleave", Utils.getpref("alarmcleavesfx", "custom/sfx/omni/oof"))
                .put("alarmattacked", Utils.getpref("alarmattackedsfx", "None"))
                .map()
        );
    }
    public static Map<String, Double> alarmvollist = Collections.synchronizedMap(new HashMap<String, Double>() {
        @Override
        public Double put(final String key, final Double value) {
            Double ret = super.put(key, value);
            Utils.setprefd(key + "vol", value);
            return (ret);
        }

        @Override
        public Double get(final Object key) {
            Double ret = super.get(key);
            if (ret == null)
                ret = Utils.getprefd(key + "vol", 0.8);
            return (ret);
        }
    });
    public static Map<String, Boolean> curioslist = null;
    public static ObservableMap<String, Boolean> autodroplist = null;

    public static final String chatfile = "chatlog.txt";
    public static PrintWriter chatlog = null;

    public static final Map<String, CheckListboxItem> boulders = new HashMap<>();
    public static final Map<String, CheckListboxItem> bushes = new HashMap<>();
    public static final Map<String, CheckListboxItem> trees = new HashMap<>();
    public static final Map<String, CheckListboxItem> icons = new HashMap<>();
    public static final Map<String, CheckListboxItem> oldicons = new HashMap<>();

    public static final ObservableMap<String, CheckListboxItem> flowermenus = new ObservableMap<>(new TreeMap<>());

//    public final static HashMap<String, CheckListboxItem> flowermenus = new HashMap<String, CheckListboxItem>(37) {{
//        put("Pick", new CheckListboxItem("Pick", Resource.BUNDLE_FLOWER));
//        put("Drink", new CheckListboxItem("Drink", Resource.BUNDLE_FLOWER));
//        put("Harvest", new CheckListboxItem("Harvest", Resource.BUNDLE_FLOWER));
//        put("Eat", new CheckListboxItem("Eat", Resource.BUNDLE_FLOWER));
//        put("Split", new CheckListboxItem("Split", Resource.BUNDLE_FLOWER));
//        put("Kill", new CheckListboxItem("Kill", Resource.BUNDLE_FLOWER));
//        put("Slice", new CheckListboxItem("Slice", Resource.BUNDLE_FLOWER));
//        put("Pluck", new CheckListboxItem("Pluck", Resource.BUNDLE_FLOWER));
//        put("Empty", new CheckListboxItem("Empty", Resource.BUNDLE_FLOWER));
//        put("Clean", new CheckListboxItem("Clean", Resource.BUNDLE_FLOWER));
//        put("Skin", new CheckListboxItem("Skin", Resource.BUNDLE_FLOWER));
//        put("Flay", new CheckListboxItem("Flay", Resource.BUNDLE_FLOWER));
//        put("Collect bones", new CheckListboxItem("Collect bones", Resource.BUNDLE_FLOWER));
//        put("Crumble", new CheckListboxItem("Crumble", Resource.BUNDLE_FLOWER));
//        put("Butcher", new CheckListboxItem("Butcher", Resource.BUNDLE_FLOWER));
//        put("Giddyup!", new CheckListboxItem("Giddyup!", Resource.BUNDLE_FLOWER));
//        put("Break", new CheckListboxItem("Break", Resource.BUNDLE_FLOWER));
//        put("Man the helm", new CheckListboxItem("Man the helm", Resource.BUNDLE_FLOWER));
//        put("Cargo", new CheckListboxItem("Cargo", Resource.BUNDLE_FLOWER));
//        put("Sleep", new CheckListboxItem("Sleep", Resource.BUNDLE_FLOWER));
//        put("Shear wool", new CheckListboxItem("Shear wool", Resource.BUNDLE_FLOWER));
//        put("Harvest wax", new CheckListboxItem("Harvest wax", Resource.BUNDLE_FLOWER));
//        put("Slice up", new CheckListboxItem("Slice up", Resource.BUNDLE_FLOWER));
//        put("Chip stone", new CheckListboxItem("Chip stone", Resource.BUNDLE_FLOWER));
//        put("Study", new CheckListboxItem("Study", Resource.BUNDLE_FLOWER));
//        put("Peer into", new CheckListboxItem("Peer into", Resource.BUNDLE_FLOWER));
//        put("Tether horse", new CheckListboxItem("Tether horse", Resource.BUNDLE_FLOWER));
//        put("Wring neck", new CheckListboxItem("Wring neck", Resource.BUNDLE_FLOWER));
//        put("Open", new CheckListboxItem("Open", Resource.BUNDLE_FLOWER));
//        put("Inspect", new CheckListboxItem("Inspect", Resource.BUNDLE_FLOWER));
//        put("Slaughter", new CheckListboxItem("Slaughter", Resource.BUNDLE_FLOWER));
//        put("Crack open", new CheckListboxItem("Crack Open", Resource.BUNDLE_FLOWER));
//        put("Collect coal", new CheckListboxItem("Collect Coal", Resource.BUNDLE_FLOWER));
//        put("Pick leaf", new CheckListboxItem("Pick Leaf", Resource.BUNDLE_FLOWER));
//        put("Ride", new CheckListboxItem("Ride", Resource.BUNDLE_FLOWER));
//        put("Scale", new CheckListboxItem("Scale", Resource.BUNDLE_FLOWER));
//        put("Pick mushrooms", new CheckListboxItem("Pick mushrooms", Resource.BUNDLE_FLOWER));
//    }};

    public static final Map<String, CheckListboxItem> autowindows = new HashMap<>();
    public static final Map<String, CheckListboxItem> autoclusters = new HashMap<>();

    public static final Map<String, CheckListboxItem> curiolist = new HashMap<>();
    public static final Map<String, Indir<Tex>> additonalicons = new HashMap<>();
    public static final Map<String, CheckListboxItem> alarmitems = new HashMap<>();
    public static final Map<String, String> defaultitems = new HashMap<>();
    public static final Set<String> locres = new HashSet<>(Arrays.asList(
            "gfx/terobjs/saltbasin",
            "gfx/terobjs/abyssalchasm",
            "gfx/terobjs/windthrow",
            "gfx/terobjs/icespire",
            "gfx/terobjs/woodheart",
            "gfx/terobjs/lilypadlotus",
            "gfx/terobjs/fairystone",
            "gfx/terobjs/jotunmussel",
            "gfx/terobjs/guanopile",
            "gfx/terobjs/geyser",
            "gfx/terobjs/claypit",
            "gfx/terobjs/caveorgan",
            "gfx/terobjs/crystalpatch"));

    public static final Set<String> mineablesStone = new HashSet<>(Arrays.asList(
            "gneiss",
            "basalt",
            "cinnabar",
            "dolomite",
            "feldspar",
            "flint",
            "granite",
            "hornblende",
            "limestone",
            "marble",
            "porphyry",
            "quartz",
            "sandstone",
            "schist",
            "blackcoal",
            "mica",
            "apatite",
            "sodalite",
            "gabbro",
            "kyanite",
            "zincspar",
            "fluorospar",
            "microlite",
            "olivine",
            "soapstone",
            "orthoclase",
            "alabaster",
            "corund",
            "diorite",
            "breccia",
            "diabase",
            "slate",
            "arkose",
            "eclogite",
            "jasper",
            "greenschist",
            "pegmatite",
//            "ilmenite",
            "rhyolite",
            "pumice",
            "sunstone",
            "chert",
            "graywacke",
            "serpentine"
    ));

    public final static Set<String> mineablesOre = new HashSet<String>(Arrays.asList(
            "cassiterite",
            "leadglance",
            "chalcopyrite",
            "malachite",
            "ilmenite",
            "limonite",
            "hematite",
            "magnetite",
            "leadglance",
            "peacockore",
            "cuprite"
    ));

    public final static Set<String> mineablesOrePrecious = new HashSet<>(Arrays.asList(
            "galena",
            "argentite",
            "hornsilver",
            "petzite",
            "sylvanite",
            "nagyagite"
    ));

    public final static Set<String> mineablesCurios = new HashSet<>(Arrays.asList(
            "catgold",
            "petrifiedshell",
            "strangecrystal",
            "quarryquartz"
    ));

    public static final Map<String, CheckListboxItem> disableanim = new HashMap<>();
    public static final Map<String, CheckListboxItem> disableshiftclick = new HashMap<>();
    public static final Map<String, String> alarms = new HashMap<>();
    public static final Map<String, String[]> cures = new HashMap<>();
    public static final Set<String> bigAnimals = new HashSet<>(Arrays.asList(
            "Spine01_R.001",
            "main.004_R",
            "tail0_R.001",
            "footfront.r",
            "ear.r",
            "frontleg4.r",
            "Bone_R.018",
            "main.002_L.002",
            "foot.l",
            "main.001_R.002",
            "frontleg4.r",
            "boneback01.l",
            "Spine3",
            "Tummy",
            "main.008_R",
            "Bone_L.001",
            "LegFront01.r",
            "foot.r",
            "Bone.001_L.002",
            "Spine00"
    ));

    static {
        boulders.put("alabaster", new CheckListboxItem("Alabaster"));
        boulders.put("basalt", new CheckListboxItem("Basalt"));
        boulders.put("schist", new CheckListboxItem("Schist"));
        boulders.put("dolomite", new CheckListboxItem("Dolomite"));
        boulders.put("gneiss", new CheckListboxItem("Gneiss"));
        boulders.put("granite", new CheckListboxItem("Granite"));
        boulders.put("porphyry", new CheckListboxItem("Porphyry"));
        boulders.put("quartz", new CheckListboxItem("Quartz"));
        boulders.put("limestone", new CheckListboxItem("Limestone"));
        boulders.put("sandstone", new CheckListboxItem("Sandstone"));
        boulders.put("cinnabar", new CheckListboxItem("Cinnabar"));
        boulders.put("feldspar", new CheckListboxItem("Feldspar"));
        boulders.put("marble", new CheckListboxItem("Marble"));
        boulders.put("flint", new CheckListboxItem("Flint"));
        boulders.put("hornblende", new CheckListboxItem("Hornblende"));
        boulders.put("olivine", new CheckListboxItem("Olivine"));
        boulders.put("apatite", new CheckListboxItem("Apatite"));
        boulders.put("corund", new CheckListboxItem("Korund"));
        boulders.put("gabbro", new CheckListboxItem("Gabbro"));
        boulders.put("arkose", new CheckListboxItem("Arkose"));
        boulders.put("breccia", new CheckListboxItem("Breccia"));

        bushes.put("arrowwood", new CheckListboxItem("Arrowwood"));
        bushes.put("crampbark", new CheckListboxItem("Crampbark"));
        bushes.put("sandthorn", new CheckListboxItem("Sandthorn"));
        bushes.put("blackberrybush", new CheckListboxItem("Blackberry"));
        bushes.put("dogrose", new CheckListboxItem("Dogrose"));
        bushes.put("spindlebush", new CheckListboxItem("Spindlebush"));
        bushes.put("blackcurrant", new CheckListboxItem("Blackcurrant"));
        bushes.put("elderberrybush", new CheckListboxItem("Elderberry"));
        bushes.put("teabush", new CheckListboxItem("Tea"));
        bushes.put("blackthorn", new CheckListboxItem("Blackthorn"));
        bushes.put("gooseberrybush", new CheckListboxItem("Gooseberry"));
        bushes.put("tibast", new CheckListboxItem("Tibast"));
        bushes.put("bogmyrtle", new CheckListboxItem("Bogmyrtle"));
        bushes.put("hawthorn", new CheckListboxItem("Hawthorn"));
        bushes.put("tundrarose", new CheckListboxItem("Tundrarose"));
        bushes.put("boxwood", new CheckListboxItem("Boxwood"));
        bushes.put("holly", new CheckListboxItem("Hollyberry"));
        bushes.put("woodbine", new CheckListboxItem("Fly Woodbine"));
        bushes.put("bsnightshade", new CheckListboxItem("Bittersweet Nightshade"));
        bushes.put("raspberrybush", new CheckListboxItem("Raspberry"));
        bushes.put("caprifole", new CheckListboxItem("Caprifole"));
        bushes.put("redcurrant", new CheckListboxItem("Redcurrant"));
        bushes.put("gorse", new CheckListboxItem("Gorse"));
        bushes.put("witherstand", new CheckListboxItem("Witherstand"));
        bushes.put("cavefern", new CheckListboxItem("Cave Fern"));
        bushes.put("mastic", new CheckListboxItem("Mastic"));
        bushes.put("poppycaps", new CheckListboxItem("Poppycaps"));
        bushes.put("ghostpipe", new CheckListboxItem("Ghostpipe"));
        bushes.put("hoarwithy", new CheckListboxItem("Hoarwithy"));

        trees.put("chastetree", new CheckListboxItem("Chaste Tree"));
        trees.put("dogwood", new CheckListboxItem("Dogwood"));
        trees.put("strawberrytree", new CheckListboxItem("Strawberry Tree"));
        trees.put("stonepine", new CheckListboxItem("Stone Pine"));
        trees.put("blackpine", new CheckListboxItem("Black Pine"));
        trees.put("silverfir", new CheckListboxItem("Silver Fir"));
        trees.put("treeheath", new CheckListboxItem("Heath Tree"));
        trees.put("sycamore", new CheckListboxItem("Sycamore"));
        trees.put("terebinth", new CheckListboxItem("Terebinth"));
        trees.put("lotetree", new CheckListboxItem("Lote Tree"));
        trees.put("sorbtree", new CheckListboxItem("Sorb Tree"));
        trees.put("alder", new CheckListboxItem("Alder"));
        trees.put("corkoak", new CheckListboxItem("Corkoak"));
        trees.put("plumtree", new CheckListboxItem("Plum Tree"));
        trees.put("juniper", new CheckListboxItem("Juniper"));
        trees.put("crabappletree", new CheckListboxItem("Crabapple"));
        trees.put("kingsoak", new CheckListboxItem("King's Oak"));
        trees.put("oak", new CheckListboxItem("Oak"));
        trees.put("walnuttree", new CheckListboxItem("Walnut Tree"));
        trees.put("birdcherrytree", new CheckListboxItem("Birdcherry Tree"));
        trees.put("larch", new CheckListboxItem("Larch"));
        trees.put("poplar", new CheckListboxItem("Poplar"));
        trees.put("whitebeam", new CheckListboxItem("Whitebeam"));
        trees.put("appletree", new CheckListboxItem("Apple Tree"));
        trees.put("cypress", new CheckListboxItem("Cypress"));
        trees.put("buckthorn", new CheckListboxItem("Buckthorn"));
        trees.put("laurel", new CheckListboxItem("Laurel"));
        trees.put("ash", new CheckListboxItem("Ash"));
        trees.put("elm", new CheckListboxItem("Elm"));
        trees.put("rowan", new CheckListboxItem("Rowan"));
        trees.put("willow", new CheckListboxItem("Willow"));
        trees.put("cedar", new CheckListboxItem("Cedar"));
        trees.put("linden", new CheckListboxItem("Linden"));
        trees.put("olivetree", new CheckListboxItem("Olive Tree"));
        trees.put("aspen", new CheckListboxItem("Aspen"));
        trees.put("fir", new CheckListboxItem("Fir"));
        trees.put("baywillow", new CheckListboxItem("Baywillow"));
        trees.put("goldenchain", new CheckListboxItem("Goldenchain"));
        trees.put("peartree", new CheckListboxItem("Pear Tree"));
        trees.put("sallow", new CheckListboxItem("Sallow"));
        trees.put("yew", new CheckListboxItem("Yew"));
        trees.put("cherry", new CheckListboxItem("Cherry"));
        trees.put("maple", new CheckListboxItem("Maple"));
        trees.put("beech", new CheckListboxItem("Beech"));
        trees.put("chestnuttree", new CheckListboxItem("Chestnut Tree"));
        trees.put("hazel", new CheckListboxItem("Hazel"));
        trees.put("spruce", new CheckListboxItem("Spruce"));
        trees.put("hornbeam", new CheckListboxItem("Hornbeam"));
        trees.put("oldtrunk", new CheckListboxItem("Mirkwood Log"));
        trees.put("conkertree", new CheckListboxItem("Conker Tree"));
        trees.put("mulberry", new CheckListboxItem("Mulberry"));
        trees.put("sweetgum", new CheckListboxItem("Sweetgum"));
        trees.put("pine", new CheckListboxItem("Pine"));
        trees.put("birch", new CheckListboxItem("Birch"));
        trees.put("planetree", new CheckListboxItem("Plane Tree"));
        trees.put("quincetree", new CheckListboxItem("Quince"));
        trees.put("almondtree", new CheckListboxItem("Almond"));
        trees.put("persimmontree", new CheckListboxItem("Persimmon"));
        trees.put("medlartree", new CheckListboxItem("Medlar"));
        trees.put("gnomeshat", new CheckListboxItem("Gnome's Hat"));
        trees.put("carobtree", new CheckListboxItem("Carob Tree"));
        trees.put("gloomcap", new CheckListboxItem("Gloomcap"));
        trees.put("trombonechantrelle", new CheckListboxItem("Trombone Chantrelle"));
        trees.put("osier", new CheckListboxItem("Osier"));
        trees.put("tamarisk", new CheckListboxItem("Tamarisk"));
        trees.put("checkertree", new CheckListboxItem("Checker Tree"));
        trees.put("wartybirch", new CheckListboxItem("Warty Birch"));
        trees.put("figtree", new CheckListboxItem("Fig Tree"));
        trees.put("dwarfpine", new CheckListboxItem("Dwarf Pine"));
        trees.put("mayflower", new CheckListboxItem("Mayflower"));

        icons.put("arrow", new CheckListboxItem("Arrow"));
        icons.put("rowboat", new CheckListboxItem("Rowboat"));
        icons.put("dugout", new CheckListboxItem("Dugout"));
        icons.put("knarr", new CheckListboxItem("Knarr"));
        icons.put("snekkja", new CheckListboxItem("Snekkja"));
        icons.put("wagon", new CheckListboxItem("Wagon"));
        icons.put("wheelbarrow", new CheckListboxItem("Wheelbarrow"));
        icons.put("cart", new CheckListboxItem("Cart"));
        icons.put("wball", new CheckListboxItem("Wrecking Ball"));
        icons.put("bram", new CheckListboxItem("Battering Ram"));
//        icons.put("irrbloss", new CheckListboxItem("Irrlight"));
        icons.put("opiumdragon", new CheckListboxItem("Opium Dragon"));
//        icons.put("moonmoth", new CheckListboxItem("Moonmoths"));
        icons.put("lobsterpot", new CheckListboxItem("Lobster Pot"));
//        icons.put("mandrakespirited", new CheckListboxItem("Spirited Mandrake"));
        icons.put("fishingnet", new CheckListboxItem("Fishing Net"));
        icons.put("mare", new CheckListboxItem("Tamed Mares"));
        icons.put("stallion", new CheckListboxItem("Tamed Stallions"));
        icons.put("boarspear", new CheckListboxItem("Boar Spear"));
//        icons.put("boostspeed", new CheckListboxItem("Speed Boost"));
//        icons.put("frog", new CheckListboxItem("Frog"));
//        icons.put("toad", new CheckListboxItem("Toad"));
        icons.put("stalagoomba", new CheckListboxItem("Stalagoomba"));
        icons.put("dryad", new CheckListboxItem("Dryad"));
        icons.put("ent", new CheckListboxItem("Ent"));
//old icons
//        icons.put("dandelion", new CheckListboxItem("Dandelion"));
//        icons.put("chantrelle", new CheckListboxItem("Chantrelle"));
//        icons.put("blueberry", new CheckListboxItem("Blueberry"));
//        icons.put("rat", new CheckListboxItem("Rat"));
//        icons.put("chicken", new CheckListboxItem("Chicken"));
//        icons.put("chick", new CheckListboxItem("Chick"));
//        icons.put("duskfern", new CheckListboxItem("Dusk Fern"));
//        icons.put("spindlytaproot", new CheckListboxItem("Spindly Taproot"));
//        icons.put("stingingnettle", new CheckListboxItem("Stinging Nettle"));
//        icons.put("dragonfly", new CheckListboxItem("Dragonfly"));
//        icons.put("magpie", new CheckListboxItem("Magpie"));
//        icons.put("mistletoe", new CheckListboxItem("Mistletoe"));
//        icons.put("firefly", new CheckListboxItem("Firefly"));
//        icons.put("cavemoth", new CheckListboxItem("Cave Moth"));
//        icons.put("windweed", new CheckListboxItem("Wild Windsown Weed"));
//        icons.put("mussels", new CheckListboxItem("Mussels"));
//        icons.put("mallard", new CheckListboxItem("Duck"));
//        icons.put("ladybug", new CheckListboxItem("Ladybug"));
//        icons.put("silkmoth", new CheckListboxItem("Silkmoth"));
//        icons.put("hedgehog", new CheckListboxItem("Hedgehog"));
//        icons.put("squirrel", new CheckListboxItem("Squirrel"));
//        icons.put("rabbit", new CheckListboxItem("Rabbit"));
//        icons.put("lingon", new CheckListboxItem("Lingonberries"));
//        icons.put("grub", new CheckListboxItem("Grub"));
//        icons.put("yellowfoot", new CheckListboxItem("Yellowfoot"));
//        icons.put("coltsfoot", new CheckListboxItem("Coltsfoot"));
//        icons.put("chives", new CheckListboxItem("Chives"));
//        icons.put("rustroot", new CheckListboxItem("Rustroot"));
//        icons.put("adder", new CheckListboxItem("Adder"));
//        icons.put("crab", new CheckListboxItem("Crab"));
//        icons.put("clover", new CheckListboxItem("Clover"));
//        icons.put("ladysmantle", new CheckListboxItem("Lady's Mantle"));
//        icons.put("grasshopper", new CheckListboxItem("Grasshopper"));
//        icons.put("snapdragon", new CheckListboxItem("Uncommon Snapdragon"));
//        icons.put("cattail", new CheckListboxItem("Cattail"));
//        icons.put("forestsnail", new CheckListboxItem("Forest Snail"));
//        icons.put("forestlizard", new CheckListboxItem("Forest Lizard"));
//        icons.put("greenkelp", new CheckListboxItem("Green Kelp"));
//        icons.put("waterstrider", new CheckListboxItem("Water Strider"));
//        icons.put("frogspawn", new CheckListboxItem("Frog Spawn"));
//        icons.put("oyster", new CheckListboxItem("Oysters"));
//        icons.put("jellyfish", new CheckListboxItem("Jellyfish"));
//        icons.put("clay-gray", new CheckListboxItem("Gray Clay"));
//        icons.put("bat", new CheckListboxItem("Bats"));
//        icons.put("stagbeetle", new CheckListboxItem("Stagbeetles"));
//        icons.put("monarchbutterfly", new CheckListboxItem("Monarch Butterfly"));
//        icons.put("cavecentipede", new CheckListboxItem("Cave Centipede"));
//        icons.put("mole", new CheckListboxItem("Moles"));
//        icons.put("lorchel", new CheckListboxItem("Morels"));
//        icons.put("frogscrown", new CheckListboxItem("Frog's Crown"));
//        icons.put("lampstalk", new CheckListboxItem("Lamp Stalks"));

        Utils.loadcollection("petalcol").forEach(petal -> flowermenus.put(petal, new CheckListboxItem(petal)));

        autowindows.put("Inventory", new CheckListboxItem("Inventory"));
//        autowindows.put("Belt", new CheckListboxItem("Belt"));
        autowindows.put("Quest Log", new CheckListboxItem("Quest Log"));
        autowindows.put("Study", new CheckListboxItem("Study"));
        autowindows.put("Equipment", new CheckListboxItem("Equipment"));
        autowindows.put("Timers", new CheckListboxItem("Timers"));
        autowindows.put("Kith & Kin", new CheckListboxItem("Kith & Kin"));
        autowindows.put("Character Sheet", new CheckListboxItem("Character Sheet"));
        autowindows.put("Search...", new CheckListboxItem("Search..."));
        autowindows.put("Craft window", new CheckListboxItem("Craft window"));
        autowindows.put("Chat", new CheckListboxItem("Chat Window - Reverse Logic, select to disable."));

        autoclusters.put("gfx/terobjs/herbs/mussels", new CheckListboxItem("Mussels", Resource.BUNDLE_FLOWER));
        autoclusters.put("gfx/terobjs/herbs/clay-gray", new CheckListboxItem("Gray Clay", Resource.BUNDLE_FLOWER));
        autoclusters.put("gfx/terobjs/herbs/oyster", new CheckListboxItem("Oysters", Resource.BUNDLE_FLOWER));
        autoclusters.put("gfx/terobjs/herbs/goosebarnacle", new CheckListboxItem("Gooseneck Barnacles", Resource.BUNDLE_FLOWER));
        autoclusters.put("gfx/terobjs/herbs/cattail", new CheckListboxItem("Cattails", Resource.BUNDLE_FLOWER));
        autoclusters.put("gfx/kritter/jellyfish/jellyfish", new CheckListboxItem("Jellyfish", Resource.BUNDLE_FLOWER));
        autoclusters.put("gfx/terobjs/herbs/lampstalk", new CheckListboxItem("Lamp Stalks", Resource.BUNDLE_FLOWER));

        curiolist.put("Bar of Soap", new CheckListboxItem("Bar of Soap"));
        curiolist.put("Barkboat", new CheckListboxItem("Barkboat"));
        curiolist.put("Batwing Necklace", new CheckListboxItem("Batwing Necklace"));
        curiolist.put("Beast Unborn", new CheckListboxItem("Beast Unborn"));
        curiolist.put("Blacksmith's Bauble", new CheckListboxItem("Blacksmith's Bauble"));
        curiolist.put("Chiming Bluebell", new CheckListboxItem("Chiming Bluebell"));
        curiolist.put("Bronze Steed", new CheckListboxItem("Bronze Steed"));
        curiolist.put("Cigar", new CheckListboxItem("Cigar"));
        curiolist.put("Deep Sea Atavism", new CheckListboxItem("Deep Sea Atavism"));
        curiolist.put("Feather Duster", new CheckListboxItem("Feather Duster"));
        curiolist.put("Feather Trinket", new CheckListboxItem("Feather Trinket"));
        curiolist.put("Bouquet of Flowers", new CheckListboxItem("Bouquet of Flowers"));
        curiolist.put("Fossil Collection", new CheckListboxItem("Fossil Collection"));
        curiolist.put("Glue Troll", new CheckListboxItem("Glue Troll"));
        curiolist.put("Golden Cat", new CheckListboxItem("Golden Cat"));
        curiolist.put("Golden Tooth", new CheckListboxItem("Golden Tooth"));
        curiolist.put("Grand Haruspex", new CheckListboxItem("Grand Haruspex"));
        curiolist.put("Great Wax Seal", new CheckListboxItem("Great Wax Seal"));
        curiolist.put("Ivory Figurine", new CheckListboxItem("Ivory Figurine"));
        curiolist.put("Mirkwood Offering", new CheckListboxItem("Mirkwood Offering"));
        curiolist.put("Onion Braid", new CheckListboxItem("Onion Braid"));
        curiolist.put("Porcelain Doll", new CheckListboxItem("Porcelain Doll"));
        curiolist.put("Seer's Bowl", new CheckListboxItem("Seer's Bowl"));
        curiolist.put("Seer's Bones", new CheckListboxItem("Seer's Bones"));
        curiolist.put("Seer's Spindle", new CheckListboxItem("Seer's Spindle"));
        curiolist.put("Seer's Stones", new CheckListboxItem("Seer's Stones"));
        curiolist.put("Seer's Tealeaves", new CheckListboxItem("Seer's Tealeaves"));
        curiolist.put("Shiny Marbles", new CheckListboxItem("Shiny Marbles"));
        curiolist.put("Silken Ribbon", new CheckListboxItem("Silken Ribbon"));
        curiolist.put("Silver Rose", new CheckListboxItem("Silver Rose"));
        curiolist.put("Snow Globe", new CheckListboxItem("Snow Globe"));
        curiolist.put("Stained Glass Heart", new CheckListboxItem("Stained Glass Heart"));
        curiolist.put("Straw Doll", new CheckListboxItem("Straw Doll"));
        curiolist.put("Stuffed Bear", new CheckListboxItem("Stuffed Bear"));
        curiolist.put("Tafl Board", new CheckListboxItem("Tafl Board"));
        curiolist.put("Tiny Abacus", new CheckListboxItem("Tiny Abacus"));
        curiolist.put("Uncrushed Husk", new CheckListboxItem("Uncrushed Husk"));
        curiolist.put("Easter Egg", new CheckListboxItem("Easter Egg"));

//        additonalicons.put("gfx/terobjs/items/mandrakespirited", Utils.cache(() -> Resource.loadtex("gfx/icons/mandrakespirited")));
        additonalicons.put("gfx/terobjs/vehicle/bram", Utils.cache(() -> Resource.loadtex("gfx/icons/bram")));
//        additonalicons.put("gfx/kritter/toad/toad", Utils.cache(() -> Resource.loadtex("gfx/icons/toad")));
        additonalicons.put("gfx/terobjs/vehicle/rowboat", Utils.cache(() -> Resource.loadtex("gfx/icons/rowboat")));
        additonalicons.put("gfx/terobjs/vehicle/dugout", Utils.cache(() -> Resource.loadtex("gfx/icons/dugout")));
        additonalicons.put("gfx/terobjs/vehicle/knarr", Utils.cache(() -> Resource.loadtex("gfx/icons/knarr")));
        additonalicons.put("gfx/terobjs/vehicle/snekkja", Utils.cache(() -> Resource.loadtex("gfx/icons/snekkja")));
//        additonalicons.put("gfx/kritter/chicken/chicken", Utils.cache(() -> Resource.loadtex("gfx/icons/deadhen")));
//        additonalicons.put("gfx/kritter/chicken/rooster", Utils.cache(() -> Resource.loadtex("gfx/icons/deadrooster")));
//        additonalicons.put("gfx/kritter/rabbit/rabbit", Utils.cache(() -> Resource.loadtex("gfx/icons/deadrabbit")));
//        additonalicons.put("gfx/kritter/hedgehog/hedgehog", Utils.cache(() -> Resource.loadtex("gfx/icons/deadhedgehog")));
//        additonalicons.put("gfx/kritter/squirrel/squirrel", Utils.cache(() -> Resource.loadtex("gfx/icons/deadsquirrel")));
        additonalicons.put("gfx/terobjs/items/arrow", Utils.cache(() -> Resource.loadtex("gfx/icons/arrow")));
        additonalicons.put("gfx/terobjs/items/boarspear", Utils.cache(() -> Resource.loadtex("gfx/icons/arrow")));
//        additonalicons.put("gfx/kritter/frog/frog", Utils.cache(() -> Resource.loadtex("gfx/icons/frog")));
        additonalicons.put("gfx/terobjs/vehicle/wagon", Utils.cache(() -> Resource.loadtex("gfx/icons/wagon")));
        additonalicons.put("gfx/terobjs/vehicle/wheelbarrow", Utils.cache(() -> Resource.loadtex("gfx/icons/wheelbarrow")));
        additonalicons.put("gfx/terobjs/vehicle/cart", Utils.cache(() -> Resource.loadtex("gfx/icons/cart")));
        additonalicons.put("gfx/terobjs/vehicle/wreckingball", Utils.cache(() -> Resource.loadtex("gfx/icons/wball")));
        additonalicons.put("gfx/kritter/nidbane/nidbane", Utils.cache(() -> Resource.loadtex("gfx/icons/spooky")));
        additonalicons.put("gfx/kritter/irrbloss/irrbloss", Utils.cache(() -> Resource.loadtex("gfx/icons/irrbloss")));
        additonalicons.put("gfx/kritter/opiumdragon/opiumdragon", Utils.cache(() -> Resource.loadtex("gfx/icons/opiumdragon")));
        additonalicons.put("gfx/terobjs/lobsterpot", Utils.cache(() -> Resource.loadtex("gfx/icons/lobsterpot")));
        additonalicons.put("gfx/terobjs/fishingnet", Utils.cache(() -> Resource.loadtex("gfx/icons/fishingnet")));
        additonalicons.put("gfx/kritter/horse/stallion", Utils.cache(() -> Resource.loadtex("gfx/icons/stallionicon")));
        additonalicons.put("gfx/kritter/horse/mare", Utils.cache(() -> Resource.loadtex("gfx/icons/mareicon")));
        additonalicons.put("gfx/kritter/stalagoomba/stalagoomba", Utils.cache(() -> configuration.imageToTex(configuration.modificationPath + "/gfx/icons/stalagoomba.png", Coord.of(20, 20))));
        additonalicons.put("gfx/kritter/dryad/dryad", Utils.cache(() -> configuration.imageToTex(configuration.modificationPath + "/gfx/icons/dryad.png")));
        additonalicons.put("gfx/kritter/ent/ent", Utils.cache(() -> configuration.imageToTex(configuration.modificationPath + "/gfx/icons/ent.png")));
        additonalicons.put("gfx/kritter/midgeswarm/midgeswarm", Utils.cache(() -> configuration.imageToTex(configuration.modificationPath + "/gfx/icons/midgeswarm.png")));

        alarmitems.put("gfx/terobjs/herbs/flotsam", new CheckListboxItem("Peculiar Flotsam"));
        alarmitems.put("gfx/terobjs/herbs/chimingbluebell", new CheckListboxItem("Chiming Bluebell"));
        alarmitems.put("gfx/terobjs/herbs/edelweiss", new CheckListboxItem("Edelweiß"));
        alarmitems.put("gfx/terobjs/herbs/bloatedbolete", new CheckListboxItem("Bloated Bolete"));
        alarmitems.put("gfx/terobjs/herbs/glimmermoss", new CheckListboxItem("Glimmermoss"));
        alarmitems.put("gfx/terobjs/herbs/camomile", new CheckListboxItem("Camomile"));
        alarmitems.put("gfx/terobjs/herbs/clay-cave", new CheckListboxItem("Cave Clay"));
        alarmitems.put("gfx/terobjs/herbs/mandrake", new CheckListboxItem("Mandrake Root"));
        alarmitems.put("gfx/terobjs/herbs/clay-gray", new CheckListboxItem("Gray Clay"));
        alarmitems.put("gfx/terobjs/herbs/dandelion", new CheckListboxItem("Dandelion"));
        alarmitems.put("gfx/terobjs/herbs/chantrelle", new CheckListboxItem("Chantrelle"));
        alarmitems.put("gfx/terobjs/herbs/blueberry", new CheckListboxItem("Blueberry"));
        alarmitems.put("gfx/terobjs/herbs/strawberry", new CheckListboxItem("Strawberry"));
        alarmitems.put("gfx/kritter/rat/rat", new CheckListboxItem("Rat"));
        alarmitems.put("gfx/kritter/spermwhale/spermwhale", new CheckListboxItem("Whale"));
        alarmitems.put("gfx/kritter/orca/orca", new CheckListboxItem("Orca"));
        alarmitems.put("gfx/kritter/chicken/chicken", new CheckListboxItem("Chicken"));
        alarmitems.put("gfx/kritter/chicken/chick", new CheckListboxItem("Chick"));
        alarmitems.put("gfx/terobjs/herbs/spindlytaproot", new CheckListboxItem("Spindly Taproot"));
        alarmitems.put("gfx/terobjs/herbs/stingingnettle", new CheckListboxItem("Stinging Nettle"));
        alarmitems.put("gfx/kritter/dragonfly/dragonfly", new CheckListboxItem("Dragonfly"));
        alarmitems.put("gfx/kritter/toad/toad", new CheckListboxItem("Toad"));
        alarmitems.put("gfx/kritter/frog/frog", new CheckListboxItem("Frog"));
        alarmitems.put("gfx/terobjs/herbs/windweed", new CheckListboxItem("Wild Windsown Weed"));
        alarmitems.put("gfx/terobjs/herbs/mussels", new CheckListboxItem("Mussels"));
        alarmitems.put("gfx/kritter/mallard/mallard", new CheckListboxItem("Duck"));
        alarmitems.put("gfx/kritter/ladybug/ladybug", new CheckListboxItem("Ladybug"));
        alarmitems.put("gfx/kritter/silkmoth/silkmoth", new CheckListboxItem("Silkmoth"));
        alarmitems.put("gfx/kritter/hedgehog/hedgehog", new CheckListboxItem("Hedgehog"));
        alarmitems.put("gfx/kritter/squirrel/squirrel", new CheckListboxItem("Squirrel"));
        alarmitems.put("gfx/kritter/rabbit/rabbit", new CheckListboxItem("Rabbit"));
        alarmitems.put("gfx/terobjs/herbs/lingon", new CheckListboxItem("Lingonberries"));
        alarmitems.put("gfx/kritter/grub/grub", new CheckListboxItem("Grub"));
        alarmitems.put("gfx/terobjs/herbs/yellowfoot", new CheckListboxItem("Yellowfoot"));
        alarmitems.put("gfx/terobjs/herbs/chives", new CheckListboxItem("Chives"));
        alarmitems.put("gfx/terobjs/herbs/rustroot", new CheckListboxItem("Rustroot"));
        alarmitems.put("gfx/kritter/crab/crab", new CheckListboxItem("Crab"));
        alarmitems.put("gfx/terobjs/herbs/clover", new CheckListboxItem("Clover"));
        alarmitems.put("gfx/terobjs/herbs/ladysmantle", new CheckListboxItem("Lady's Mantle"));
        alarmitems.put("gfx/kritter/grasshopper/grasshopper", new CheckListboxItem("Grasshopper"));
        alarmitems.put("gfx/kritter/irrbloss/irrbloss", new CheckListboxItem("Irrlight"));
        alarmitems.put("gfx/kritter/opiumdragon/opiumdragon", new CheckListboxItem("Opium Dragon"));
        alarmitems.put("gfx/terobjs/herbs/snapdragon", new CheckListboxItem("Uncommon Snapdragon"));
        alarmitems.put("gfx/terobjs/herbs/cattail", new CheckListboxItem("Cattail"));
        alarmitems.put("gfx/kritter/forestsnail/forestsnail", new CheckListboxItem("Forest Snail"));
        alarmitems.put("gfx/kritter/forestlizard/forestlizard", new CheckListboxItem("Forest Lizard"));
        alarmitems.put("gfx/terobjs/herbs/greenkelp", new CheckListboxItem("Green Kelp"));
        alarmitems.put("gfx/terobjs/herbs/yarrow", new CheckListboxItem("Yarrow"));
        alarmitems.put("gfx/terobjs/herbs/candleberry", new CheckListboxItem("Candleberry"));
        alarmitems.put("gfx/terobjs/herbs/oyster", new CheckListboxItem("Oysters"));
        alarmitems.put("gfx/kritter/jellyfish/jellyfish", new CheckListboxItem("Jellyfish"));
        alarmitems.put("gfx/terobjs/herbs/seashell", new CheckListboxItem("Rainbowshell"));
        alarmitems.put("gfx/terobjs/herbs/giantpuffball", new CheckListboxItem("Giant Puff Ball"));
        alarmitems.put("gfx/terobjs/herbs/ladysmantledew", new CheckListboxItem("Dewy Lady's Mantle"));

        defaultitems.put("gfx/terobjs/herbs/flotsam", "Peculiar Flotsam");
        defaultitems.put("gfx/terobjs/herbs/chimingbluebell", "Chiming Bluebell");
        defaultitems.put("gfx/terobjs/herbs/edelweiss", "Edelweiß");
        defaultitems.put("gfx/terobjs/herbs/bloatedbolete", "Bloated Bolete");
        defaultitems.put("gfx/terobjs/herbs/glimmermoss", "Glimmermoss");
        defaultitems.put("gfx/terobjs/herbs/camomile", "Camomile");
        defaultitems.put("gfx/terobjs/herbs/clay-cave", "Cave Clay");
        defaultitems.put("gfx/terobjs/herbs/mandrake", "Mandrake Root");
        defaultitems.put("gfx/terobjs/herbs/clay-gray", "Gray Clay");
        defaultitems.put("gfx/terobjs/herbs/dandelion", "Dandelion");
        defaultitems.put("gfx/terobjs/herbs/chantrelle", "Chantrelle");
        defaultitems.put("gfx/terobjs/herbs/blueberry", "Blueberry");
        defaultitems.put("gfx/terobjs/herbs/strawberry", "Strawberry");
        defaultitems.put("gfx/kritter/rat/rat", "Rat");
        defaultitems.put("gfx/terobs/villageidol", "Village Idol");
        defaultitems.put("gfx/kritter/chicken/chicken", "Chicken");
        defaultitems.put("gfx/kritter/chicken/chick", "Chick");
        defaultitems.put("gfx/terobjs/herbs/spindlytaproot", "Spindly Taproot");
        defaultitems.put("gfx/terobjs/herbs/stingingnettle", "Stinging Nettle");
        defaultitems.put("gfx/kritter/dragonfly/dragonfly", "Dragonfly");
        defaultitems.put("gfx/kritter/toad/toad", "Toad");
        defaultitems.put("gfx/kritter/frog/frog", "Frog");
        defaultitems.put("gfx/terobjs/herbs/windweed", "Wild Windsown Weed");
        defaultitems.put("gfx/terobjs/herbs/mussels", "Mussels");
        defaultitems.put("gfx/kritter/mallard/mallard", "Duck");
        defaultitems.put("gfx/kritter/ladybug/ladybug", "Ladybug");
        defaultitems.put("gfx/kritter/silkmoth/silkmoth", "Silkmoth");
        defaultitems.put("gfx/kritter/caveangler/caveangler", "Cave Angler");
        defaultitems.put("gfx/kritter/hedgehog/hedgehog", "Hedgehog");
        defaultitems.put("gfx/kritter/squirrel/squirrel", "Squirrel");
        defaultitems.put("gfx/kritter/rabbit/rabbit", "Rabbit");
        defaultitems.put("gfx/terobjs/herbs/lingon", "Lingonberries");
        defaultitems.put("gfx/kritter/grub/grub", "Grub");
        defaultitems.put("gfx/terobjs/herbs/yellowfoot", "Yellowfoot");
        defaultitems.put("gfx/terobjs/herbs/coltsfoot", "Coltsfoot");
        defaultitems.put("gfx/terobjs/herbs/chives", "Chives");
        defaultitems.put("gfx/terobjs/herbs/rustroot", "Rustroot");
        defaultitems.put("gfx/kritter/crab/crab", "Crab");
        defaultitems.put("gfx/terobjs/herbs/clover", "Clover");
        defaultitems.put("gfx/terobjs/herbs/ladysmantle", "Lady's Mantle");
        defaultitems.put("gfx/kritter/grasshopper/grasshopper", "Grasshopper");
        defaultitems.put("gfx/kritter/irrbloss/irrbloss", "Irrlight");
        defaultitems.put("gfx/kritter/opiumdragon/opiumdragon", "Opium Dragon");
        defaultitems.put("gfx/terobjs/herbs/snapdragon", "Uncommon Snapdragon");
        defaultitems.put("gfx/terobjs/herbs/cattail", "Cattail");
        defaultitems.put("gfx/kritter/forestsnail/forestsnail", "Forest Snail");
        defaultitems.put("gfx/kritter/forestlizard/forestlizard", "Forest Lizard");
        defaultitems.put("gfx/terobjs/herbs/greenkelp", "Green Kelp");
        defaultitems.put("gfx/terobjs/herbs/yarrow", "Yarrow");
        defaultitems.put("gfx/terobjs/herbs/candleberry", "Candleberry");
        defaultitems.put("gfx/terobjs/herbs/oyster", "Oysters");
        defaultitems.put("gfx/kritter/jellyfish/jellyfish", "Jellyfish");
        defaultitems.put("gfx/terobjs/herbs/seashell", "Rainbowshell");
        defaultitems.put("gfx/terobjs/herbs/giantpuffball", "Giant Puff Ball");
        defaultitems.put("gfx/terobjs/herbs/ladysmantledew", "Dewy Lady's Mantle");
        defaultitems.put("gfx/terobjs/saltbasin", "Salt Basin");
        defaultitems.put("gfx/terobjs/abyssalchasm", "Abyssal Chasm");
        defaultitems.put("gfx/terobjs/windthrow", "Wild Windthrow");
        defaultitems.put("gfx/terobjs/icespire", "Ice Spire");
        defaultitems.put("gfx/terobjs/woodheart", "Heartwood Tree");
        defaultitems.put("gfx/terobjs/lilypadlotus", "Lilypad Lotus");
        defaultitems.put("gfx/terobjs/fairystone", "Fairy Stone");
        defaultitems.put("gfx/terobjs/jotunmussel", "Jotun Mussel");
        defaultitems.put("gfx/terobjs/guanopile", "Guano Pile");
        defaultitems.put("gfx/terobjs/geyser", "Brimstone Geyser");
        defaultitems.put("gfx/terobjs/claypit", "Clay Pit");
        defaultitems.put("gfx/terobjs/caveorgan", "Cave Organ");
        defaultitems.put("gfx/terobjs/crystalpatch", "Rock Crystal");
        defaultitems.put("gfx/kritter/bear/bear", "Bear");
        defaultitems.put("gfx/kritter/orca/orca", "Orca");
        defaultitems.put("gfx/kritter/spermwhale/spermwhale", "Whale");
        defaultitems.put("gfx/kritter/adder/adder", "Snake");
        defaultitems.put("gfx/terobjs/vehicle/bram", "Battering Ram");
        defaultitems.put("gfx/terobjs/vehicle/catapult", "Catapult");
        defaultitems.put("gfx/terobjs/vehicle/wreckingball", "Wrecking Ball");
        defaultitems.put("gfx/kritter/lynx/lynx", "Lynx");
        defaultitems.put("gfx/kritter/walrus/walrus", "Walrus");
        defaultitems.put("gfx/kritter/seal/seal", "Seal");
        defaultitems.put("gfx/kritter/troll/troll", "Troll");
        defaultitems.put("gfx/kritter/mammoth/mammoth", "Mammoth");
        defaultitems.put("gfx/kritter/goldeneagle/golldeneagle", "Eagle");
        defaultitems.put("gfx/kritter/nidbane/nidbane", "Nidbane");
        defaultitems.put("gfx/kritter/horse/horse", "Wild Horse");
        defaultitems.put("gfx/kritter/moose/moose", "Moose");
        defaultitems.put("gfx/terobjs/beaverdam", "Beaver Dam");
        defaultitems.put("gfx/terobjs/dng/antdungeon", "Ant Dungeon");
        defaultitems.put("gfx/terobjs/dng/batcave", "Bat Dungeon");
        defaultitems.put("gfx/kritter/wolverine/wolverine", "Wolverine");
        defaultitems.put("gfx/kritter/badger/badger", "Badger");
        defaultitems.put("gfx/kritter/fox/fox", "Fox");
        defaultitems.put("gfx/kritter/wolf/wolf", "Wolves");
        defaultitems.put("gfx/kritter/mole/mole", "Moles");
        defaultitems.put("gfx/terobjs/herbs/lorchel", "Morels");
        defaultitems.put("gfx/terobjs/herbs/frogscrown", "Frog's Crown");
        defaultitems.put("gfx/terobjs/items/gems/gemstone", "Gemstones");
        defaultitems.put("gfx/kritter/boar/boar", "Boars");
        defaultitems.put("gfx/kritter/reddeer/reddeer", "Red Deer");
        defaultitems.put("gfx/kritter/reindeer/reindeer", "Reindeer");

        disableanim.put("gfx/terobjs/beehive", new CheckListboxItem("Beehives"));
        disableanim.put("gfx/terobjs/pow", new CheckListboxItem("Fires"));
        disableanim.put("gfx/terobjs/stockpile-trash", new CheckListboxItem("Full trash stockpiles"));
        disableanim.put("/idle", new CheckListboxItem("Idle animals"));
        disableanim.put("gfx/terobjs/steelcrucible", new CheckListboxItem("Steel Crucible"));
        disableanim.put("gfx/terobjs/cauldron", new CheckListboxItem("Cauldrons"));
        disableanim.put("gfx/terobjs/villageidol", new CheckListboxItem("Village Idol"));
        disableanim.put("gfx/terobjs/tarkiln", new CheckListboxItem("Tar Kilns"));
        disableanim.put("gfx/terobjs/oven", new CheckListboxItem("Ovens"));
        disableanim.put("gfx/terobjs/smelter", new CheckListboxItem("Smelters"));
        disableanim.put("gfx/terobjs/arch/visflag", new CheckListboxItem("Visitor Flags"));
        disableanim.put("gfx/terobjs/flagpole", new CheckListboxItem("Flag Poles"));
        disableanim.put("gfx/terobjs/herbs/chimingbluebell", new CheckListboxItem("Bluebells"));

        disableshiftclick.put("steelcrucible", new CheckListboxItem("Steel Crucibles"));
        disableshiftclick.put("ttub", new CheckListboxItem("Tanning Tub"));
        disableshiftclick.put("smelter", new CheckListboxItem("Smelters"));
        disableshiftclick.put("oven", new CheckListboxItem("Ovens"));
        disableshiftclick.put("kiln", new CheckListboxItem("Kilns"));
        disableshiftclick.put("htable", new CheckListboxItem("Herb Tables"));
        disableshiftclick.put("cupboard", new CheckListboxItem("Cupboards"));
        disableshiftclick.put("cauldron", new CheckListboxItem("Cauldrons"));
        disableshiftclick.put("primsmelter", new CheckListboxItem("Primitiry Smeltery"));

        alarms.put("None", "None");
        alarms.put("Pony Alarm", "sfx/alarmpony");
        alarms.put("Awwwwww Yeah", "sfx/awwyeah");
        alarms.put("Bear Roar", "sfx/BearRoar");
        alarms.put("Jcoles Beaver Dungeon", "sfx/BeaverDungeon");
        alarms.put("JColes Danger Noodle", "sfx/DangerNoodle");
        alarms.put("DaveyJones", "sfx/DaveyJones");
        alarms.put("Ding", "sfx/Ding");
        alarms.put("Doomed", "sfx/Doomed");
        alarms.put("EagleScreech", "sfx/EagleScreech");
        alarms.put("GhostBusters", "sfx/GhostBusters");
        alarms.put("Gold", "sfx/gold");
        alarms.put("Oof!", "sfx/oof");
        alarms.put("lynx", "sfx/lynx");
        alarms.put("mammoth", "sfx/mammoth");
        alarms.put("Oh Shit!", "sfx/OhShit");
        alarms.put("JColes OhFuckItsAGuy", "sfx/OhShitItsAGuy");
        alarms.put("Enemy Siren", "sfx/redenemy");
        alarms.put("Arf Arf", "sfx/seal");
        alarms.put("Siege Warning", "sfx/siege");
        alarms.put("Silver", "sfx/silver");
        alarms.put("Unknown Player Siren", "sfx/Siren");
        alarms.put("Female Scream", "sfx/Scream");
        alarms.put("Study Ding", "sfx/Study");
        alarms.put("Swag!", "sfx/Swag");
        alarms.put("Thank youuuuuuu", "sfx/thankyourick");
        alarms.put("Timer alarm", "sfx/timer");
        alarms.put("Troll in the dungeon!", "sfx/troll");
        alarms.put("JColes Wallllllrus", "sfx/Walrus");
        alarms.put("Wrecking Ball!", "sfx/WreckingBall");
        alarms.put("Zelda Secret", "sfx/Zelda");
        alarms.put("Trumpets", "sfx/trumpets");
        alarms.put("No Dick!", "sfx/nodick");
        alarms.put("Snek!", "sfx/snek");
        alarms.put("Noperope", "sfx/noperope");
        alarms.put("Bruh", "sfx/bruh");

        cures.put("paginae/wound/antburn", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        cures.put("paginae/wound/sandfleabites", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        cures.put("paginae/wound/blunttrauma", new String[]{
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/leech",
                "gfx/invobjs/gauze",
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/camomilecompress",
                "gfx/invobjs/opium"
        });
        cures.put("paginae/wound/bruise", new String[]{
                "gfx/invobjs/leech"
        });
        cures.put("paginae/wound/midgebite", new String[]{
                "gfx/invobjs/herbs/yarrow"
        });
        cures.put("paginae/wound/concussion", new String[]{
                "gfx/invobjs/coldcompress",
                "gfx/invobjs/opium"
        });
        cures.put("paginae/wound/cruelincision", new String[]{
                "gfx/invobjs/gauze",
                "gfx/invobjs/stitchpatch",
                "gfx/invobjs/rootfill"
        });
        cures.put("paginae/wound/deepcut", new String[]{
                "gfx/invobjs/gauze",
                "gfx/invobjs/stingingpoultice",
                "gfx/invobjs/rootfill",
                "gfx/invobjs/herbs/waybroad",
                "gfx/invobjs/honeybroadaid"
        });
        cures.put("paginae/wound/fellslash", new String[]{
                "gfx/invobjs/gauze"
        });
        cures.put("paginae/wound/nicksnknacks", new String[]{
                "gfx/invobjs/herbs/yarrow",
                "gfx/invobjs/honeybroadaid"
        });
        cures.put("paginae/wound/punchsore", new String[]{
                "gfx/invobjs/mudointment",
                "gfx/invobjs/opium"
        });
        cures.put("paginae/wound/scrapesncuts", new String[]{
                "gfx/invobjs/herbs/yarrow",
                "gfx/invobjs/mudointment",
                "gfx/invobjs/honeybroadaid"
        });
        cures.put("paginae/wound/severemauling", new String[]{
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/opium"
        });
        cures.put("paginae/wound/swollenbump", new String[]{
                "gfx/invobjs/coldcompress",
                "gfx/invobjs/leech",
                "gfx/invobjs/stingingpoultice"
        });
        cures.put("paginae/wound/unfaced", new String[]{
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/leech",
                "gfx/invobjs/mudointment",
                "gfx/invobjs/kelpcream"
        });
        cures.put("paginae/wound/wretchedgore", new String[]{
                "gfx/invobjs/stitchpatch"
        });
        cures.put("paginae/wound/blackeye", new String[]{
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/honeybroadaid",
                "gfx/invobjs/toadbutter",
                "gfx/invobjs/rootfill"
        });
        cures.put("paginae/wound/bladekiss", new String[]{
                "gfx/invobjs/gauze",
                "gfx/invobjs/toadbutter"
        });
        cures.put("paginae/wound/somethingbroken", new String[]{
                "gfx/invobjs/splint"
        });
        cures.put("paginae/wound/infectedsore", new String[]{
                "gfx/invobjs/camomilecompress",
                "gfx/invobjs/soapbar",
                "gfx/invobjs/opium",
                "gfx/invobjs/antpaste"
        });
        cures.put("paginae/wound/nastylaceration", new String[]{
                "gfx/invobjs/stitchpatch",
                "gfx/invobjs/toadbutter"
        });
        cures.put("paginae/wound/sealfinger", new String[]{
                "gfx/invobjs/hartshornsalve",
                "gfx/invobjs/kelpcream",
                "gfx/invobjs/antpaste"
        });
        cures.put("paginae/wound/coalcough", new String[]{
                "gfx/invobjs/opium"
        });
        cures.put("paginae/wound/beesting", new String[]{
                "gfx/invobjs/kelpcream",
                "gfx/invobjs/antpaste"
        });
        cures.put("paginae/wound/crabcaressed", new String[]{
                "gfx/invobjs/antpaste"
        });
        cures.put("paginae/wound/leechburns", new String[]{
                "gfx/invobjs/toadbutter"
        });
    }

    public static final Map<Long, Coord> gridIdsMap = new HashMap<>();

    static {
        Utils.loadprefchklist("disableanim", Config.disableanim);
        Utils.loadprefchklist("alarmitems", Config.alarmitems);
        Utils.loadprefchklist("disableshiftclick", Config.disableshiftclick);
        Utils.loadCurioList();
        Utils.loadAutodropList();
        String p;
        if ((p = Utils.getprop("haven.authck", null)) != null)
            authck = Utils.hex2byte(p);
//        try {
//            InputStream in = ErrorHandler.class.getResourceAsStream("/buildinfo");
//            try {
//                if (in != null) {
//                    java.util.Scanner s = new java.util.Scanner(in);
//                    String[] binfo = s.next().split(",");
//                    gitrev = binfo[0];
//                    version = binfo[1];
//                    System.out.println(gitrev + " " + version);
//                }
//            } finally {
//                in.close();
//            }
//        } catch (Exception e) {
//        }
        loadBuildVersion();

        isUpdate = (!version.equals(newversion)) || !getFile("CHANGELOG.txt").exists();
        if (isUpdate) {
            Config.version = newversion;
            Utils.setpref("version", newversion);
            Config.version = newversion;
        }


        try {
            InputStream in = ErrorHandler.class.getResourceAsStream("/CHANGELOG.txt");
            try {
                if (in != null) {
                    java.util.Scanner s = new java.util.Scanner(in);
                    Changelogbuffer = new StringBuffer();
                    while (s.hasNextLine()) {
                        Changelogbuffer.append("-");
                        Changelogbuffer.append(s.nextLine());
                    }
                    Changelog = Changelogbuffer.toString();
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
        }

        loadLogins();
        Iconfinder.loadConfig();
    }

    private static void loadBuildVersion() {
        InputStream in = Config.class.getResourceAsStream("/buildinfo");
        try {
            try {
                if (in != null) {
                    Properties info = new Properties();
                    info.load(in);
                    newversion = info.getProperty("version");
                    gitrev = info.getProperty("git-rev");
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw (new Error(e));
        }
    }

    public static File getFile(String name) {
        return new File(HOMEDIR, name);
    }

    public static String loadFile(String name) {
        InputStream inputStream = getFSStream(name);
        if (inputStream == null) {
            inputStream = getJarStream(name);
        }
        return getString(inputStream);
    }

    public static String loadJarFile(String name) {
        return getString(getJarStream(name));
    }

    public static String loadFSFile(String name) {
        return getString(getFSStream(name));
    }

    private static InputStream getFSStream(String name) {
        InputStream inputStream = null;
        File file = Config.getFile(name);
        if (file.exists() && file.canRead()) {
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException ignored) {
            }
        }
        return inputStream;
    }

    private static InputStream getJarStream(String name) {
        if (name.charAt(0) != '/') {
            name = '/' + name;
        }
        return Config.class.getResourceAsStream(name);
    }

    private static String getString(InputStream inputStream) {
        if (inputStream != null) {
            try {
                return Utils.stream2str(inputStream);
            } catch (Exception ignore) {
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static void saveFile(String name, String data) {
        File file = Config.getFile(name);
        boolean exists = file.exists();
        if (!exists) {
            try {
                //noinspection ResultOfMethodCallIgnored
                String parent = file.getParent();
                new File(parent).mkdirs();
                exists = file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        if (exists && file.canWrite()) {
            PrintWriter out = null;
            try {
                out = new PrintWriter(file);
                out.print(data);
            } catch (FileNotFoundException ignored) {
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private static int getint(String name, int def) {
        String val = Utils.getprop(name, null);
        if (val == null)
            return (def);
        return (Integer.parseInt(val));
    }

    private static URL geturl(String name, String def) {
        String val = Utils.getprop(name, def);
        if (val.equals(""))
            return (null);
        try {
            return (new URL(val));
        } catch (java.net.MalformedURLException e) {
            throw (new RuntimeException(e));
        }
    }

    public static void parsesvcaddr(String spec, Consumer<String> host, Consumer<Integer> port) {
        if ((spec.length() > 0) && (spec.charAt(0) == '[')) {
            int p = spec.indexOf(']');
            if (p > 0) {
                String hspec = spec.substring(1, p);
                if (spec.length() == p + 1) {
                    host.accept(hspec);
                    return;
                } else if ((spec.length() > p + 1) && (spec.charAt(p + 1) == ':')) {
                    host.accept(hspec);
                    port.accept(Integer.parseInt(spec.substring(p + 2)));
                    return;
                }
            }
        }
        int p = spec.indexOf(':');
        if (p >= 0) {
            host.accept(spec.substring(0, p));
            port.accept(Integer.parseInt(spec.substring(p + 1)));
            return;
        } else {
            host.accept(spec);
            return;
        }
    }

    private static Config global = null;

    public static Config get() {
        if (global != null)
            return (global);
        synchronized (Config.class) {
            if (global == null)
                global = new Config();
            return (global);
        }
    }

    private static Properties getjarprops() {
        Properties ret = new Properties();
        try (InputStream fp = Config.class.getResourceAsStream("boot-props")) {
            if (fp != null)
                ret.load(fp);
        } catch (Exception exc) {
            /* XXX? Catch all exceptions? It just seems dumb to
             * potentially crash here for unforeseen reasons. */
            new Warning(exc, "unexpected error occurred when loading local properties").issue();
        }
        return (ret);
    }

    private static Properties getlocalprops() {
        Properties ret = new Properties();
        try {
            Path jar = Utils.srcpath(Config.class);
            if (jar != null) {
                try (InputStream fp = Files.newInputStream(jar.resolveSibling("haven-config.properties"))) {
                    ret.load(fp);
                } catch (NoSuchFileException exc) {
                    /* That's quite alright. */
                }
            }
        } catch (Exception exc) {
            new Warning(exc, "unexpected error occurred when loading neighboring properties").issue();
        }
        return (ret);
    }

    public String getprop(String name, String def) {
        String ret;
        if ((ret = jarprops.getProperty(name)) != null)
            return (ret);
        if ((ret = localprops.getProperty(name)) != null)
            return (ret);
        return (Utils.getprop(name, def));
    }

    public static final Path parsepath(String p) {
        if ((p == null) || p.equals(""))
            return (null);
        return (Utils.path(p));
    }

    public static final URL parseurl(String url) {
        if ((url == null) || url.equals(""))
            return (null);
        try {
            return (new URL(url));
        } catch (java.net.MalformedURLException e) {
            throw (new RuntimeException(e));
        }
    }

    public static final URI parseuri(String url) {
        if((url == null) || url.equals(""))
            return(null);
        return(Utils.uri(url));
    }

    public static class Variable<T> {
        public final Function<Config, T> init;
        private boolean inited = false;
        private T val;

        private Variable(Function<Config, T> init) {
            this.init = init;
        }

        public T get() {
            if (!inited) {
                synchronized (this) {
                    if (!inited) {
                        val = init.apply(Config.get());
                        inited = true;
                    }
                }
            }
            return (val);
        }

        public void set(T val) {
            synchronized (this) {
                inited = true;
                this.val = val;
            }
        }

        public static <V> Variable<V> def(Supplier<V> defval) {
            return (new Variable<>(cfg -> defval.get()));
        }

        public static <V> Variable<V> prop(String name, Function<String, V> parse, Supplier<V> defval) {
            return (new Variable<>(cfg -> {
                String pv = cfg.getprop(name, null);
                if (pv == null) pv = Utils.getpref(name, null);
                return ((pv == null) ? defval.get() : parse.apply(pv));
            }));
        }

        public static Variable<String> prop(String name, String defval) {
            return (prop(name, Function.identity(), () -> defval));
        }

        public static Variable<Integer> propi(String name, int defval) {
            return (prop(name, Integer::parseInt, () -> defval));
        }

        public static Variable<Boolean> propb(String name, boolean defval) {
            return (prop(name, Utils::parsebool, () -> defval));
        }

        public static Variable<Double> propf(String name, Double defval) {
            return (prop(name, Double::parseDouble, () -> defval));
        }

        public static Variable<byte[]> propb(String name, byte[] defval) {
            return (prop(name, Utils::hex2byte, () -> defval));
        }

        public static Variable<URI> propu(String name, URI defval) {
            return (prop(name, Config::parseuri, () -> defval));
        }

        public static Variable<URI> propu(String name, String defval) {
            return (propu(name, parseuri(defval)));
        }

        public static Variable<Path> propp(String name, Path defval) {
            return (prop(name, Config::parsepath, () -> defval));
        }

        public static Variable<Path> propp(String name, String defval) {
            return (propp(name, parsepath(defval)));
        }
    }

    private static void loadLogins() {
        try {
            String loginsjson = Utils.getpref("logins", null);
            if (loginsjson == null)
                return;
            JSONArray larr = new JSONArray(loginsjson);
            for (int i = 0; i < larr.length(); i++) {
                JSONObject l = larr.getJSONObject(i);
                logins.add(new LoginData(l.get("name").toString(), l.get("pass").toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveLogins() {
        try {
            List<String> larr = new ArrayList<String>();
            for (LoginData ld : logins) {
                String ldjson = new JSONObject(ld, new String[]{"name", "pass"}).toString();
                larr.add(ldjson);
            }
            String jsonobjs = "";
            for (String s : larr)
                jsonobjs += s + ",";
            if (jsonobjs.length() > 0)
                jsonobjs = jsonobjs.substring(0, jsonobjs.length() - 1);
            Utils.setpref("logins", "[" + jsonobjs + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static URL geturl(String url) {
        if (url.equals(""))
            return null;
        try {
            return new URL(url);
        } catch (java.net.MalformedURLException e) {
            throw (new RuntimeException(e));
        }
    }

    public static class Services {
        public static final Variable<URI> directory = Config.Variable.propu("haven.svcdir", "");
        public final URI rel;
        public final Properties props;

        public Services(URI rel, Properties props) {
            this.rel = rel;
            this.props = props;
        }

        private static Services fetch(URI uri) {
            Properties props = new Properties();
            if(uri != null) {
                Object[] data;
                try {
                    try(InputStream fp = Http.fetch(uri.toURL())) {
                        data = new StreamMessage(fp).list();
                    }
                } catch(IOException exc) {
                    throw(new RuntimeException(exc));
                }
                for(Object d : data) {
                    Object[] p = (Object[])d;
                    props.put(p[0], p[1]);
                }
            }
            return(new Services(uri, props));
        }

        private static Services global = null;
        public static Services get() {
            if(global != null)
                return(global);
            synchronized(Services.class) {
                if(global == null)
                    global = fetch(directory.get());
                return(global);
            }
        }

        public URI geturi(String name) {
            String val = props.getProperty(name);
            if(val == null)
                return(null);
            return(rel.resolve(parseuri(val)));
        }

        public static Variable<URI> var(String name, String defval) {
            URI def = parseuri(defval);
            return new Variable<URI>(cfg -> {
                String pv = cfg.getprop("haven." + name, null);
                if(pv != null)
                    return(parseuri(pv));
                return(Services.get().geturi(name));
            });
        }
    }

    private static void usage(PrintStream out) {
        out.println("usage: haven.jar [OPTIONS] [SERVER[:PORT]]");
        out.println("Options include:");
        out.println("  -h                 Display this help");
        out.println("  -d                 Display debug text");
        out.println("  -P                 Enable profiling");
        out.println("  -G                 Enable GPU profiling");
        out.println("  -p FILE            Write player position to a memory mapped file");
        out.println("  -U URL             Use specified external resource URL");
        out.println("  -A AUTHSERV[:PORT] Use specified authentication server");
        out.println("  -u USER            Authenticate as USER (together with -C)");
        out.println("  -C HEXCOOKIE       Authenticate with specified hex-encoded cookie");
        out.println("  -p PREFSPEC        Use alternate preference prefix");
    }

    public static void cmdline(String[] args) {
        PosixArgs opt = PosixArgs.getopt(args, "hdPGU:r:A:u:C:p:");
        if (opt == null) {
            usage(System.err);
            System.exit(1);
        }
        for (char c : opt.parsed()) {
            switch (c) {
                case 'h':
                    usage(System.out);
                    System.exit(0);
                    break;
                case 'd':
                    dbtext = true;
                    break;
                case 'P':
                    profile = true;
                    break;
                case 'G':
                    profilegpu = true;
                    break;
                case 'A':
                    parsesvcaddr(opt.arg, s -> authserv = s, p -> authport = p);
                    break;
                case 'U':
                    resurl = parseuri(opt.arg);
                    break;
                case 'u':
                    authuser = opt.arg;
                    break;
                case 'C':
                    authck = Utils.hex2byte(opt.arg);
                    break;
                case 'p':
                    prefspec = opt.arg;
                    break;
            }
        }
        if (opt.rest.length > 0)
            parsesvcaddr(opt.rest[0], s -> defserv = s, p -> mainport = p);
        if (opt.rest.length > 0) {
            int p = opt.rest[0].indexOf(':');
            if (p >= 0) {
                defserv = opt.rest[0].substring(0, p);
                mainport = Integer.parseInt(opt.rest[0].substring(p + 1));
            } else {
                defserv = opt.rest[0];
            }
        }
    }

    public static void setUserName(String username) {
        Config.username = username;
        Config.playername = null;
    }

    public static void setPlayerName(String playername) {
        Config.playername = playername;
        Reactor.PLAYER.onNext(userpath());
    }

    public static String userpath() {
        return String.format("%s/%s", username, playername);
    }

    private static Double getfloat(String name, Double def) {
        String val = Utils.getprop(name, null);
        if (val == null)
            return (def);
        return (Double.parseDouble(val));
    }


    static {
        Console.setscmd("stats", (cons, args) -> dbtext = Utils.parsebool(args[1]));
        Console.setscmd("profile", (cons, args) -> {
            if (args[1].equals("none") || args[1].equals("off")) {
                profile = profilegpu = false;
            } else if (args[1].equals("cpu")) {
                profile = true;
            } else if (args[1].equals("gpu")) {
                profilegpu = true;
            } else if (args[1].equals("all")) {
                profile = profilegpu = true;
            }
        });
    }
}
