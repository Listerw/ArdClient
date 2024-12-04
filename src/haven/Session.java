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

import haven.sloth.script.SessionDetails;
import modification.dev;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class Session implements Resource.Resolver {
    public static final int PVER = 29;

    public static final int MSG_SESS = 0;
    public static final int MSG_REL = 1;
    public static final int MSG_ACK = 2;
    public static final int MSG_BEAT = 3;
    public static final int MSG_MAPREQ = 4;
    public static final int MSG_MAPDATA = 5;
    public static final int MSG_OBJDATA = 6;
    public static final int MSG_OBJACK = 7;
    public static final int MSG_CLOSE = 8;
    public static final int SESSERR_AUTH = 1;
    public static final int SESSERR_BUSY = 2;
    public static final int SESSERR_CONN = 3;
    public static final int SESSERR_PVER = 4;
    public static final int SESSERR_EXPR = 5;
    public static final int SESSERR_MESG = 6;
    public final CharacterInfo character;
    static final int ackthresh = 30;

    public final Connection conn;

    private static final String[] LOCAL_CACHED = new String[]{
            "gfx/hud/chr/custom/ahard",
            "gfx/hud/chr/custom/asoft"
    };

    DatagramSocket sk;
    Thread ticker;
    public int connfailed = 0;
    public String connerror = null;
    int tseq = 0, rseq = 0;
    int ackseq;
    long acktime = -1;
    final LinkedList<PMessage> uimsgs = new LinkedList<>();
    final LinkedList<RMessage> pending = new LinkedList<>();
    public final SessionDetails details;
    public String username;
    final Map<Integer, CachedRes> rescache = new TreeMap<>();
    public final Glob glob;
    public byte[] sesskey;
    private int localCacheId = -1;
    long sent = 0, recv = 0, pend = 0, retran = 0;
    private boolean closed = false;

    @SuppressWarnings("serial")
    public static class MessageException extends RuntimeException {
        public Message msg;

        public MessageException(String text, Message msg) {
            super(text);
            this.msg = msg;
        }
    }

    public static class LoadingIndir extends Loading {
        public final int resid;
        private transient final CachedRes res;

        private LoadingIndir(CachedRes res) {
            super("Waiting to resolve resource reference " + res.resid + "...");
            this.res = res;
            this.resid = res.resid;
        }

        public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
            synchronized (res) {
                if (res.resnm != null) {
                    reg.accept(Waiting.dummy);
                    callback.run();
                } else {
                    reg.accept(res.wq.add(callback));
                }
            }
        }

        public void waitfor() throws InterruptedException {
            synchronized (res) {
                while (res.resnm == null)
                    res.wait(1000);
            }
        }

        public boolean boostprio(int prio) {
            res.boostprio(prio);
            return (true);
        }

        public boolean canwait() {
            return (true);
        }
    }

    private static class CachedRes {
        private final Waitable.Queue wq = new Waitable.Queue();
        private final int resid;
        private String resnm = null;
        private int resver;
        private Reference<Ref> ind;
        private int prio = -6;

        private CachedRes(int id) {
            resid = id;
        }

        private class Ref implements Indir<Resource> {
            public Resource res;

            public Resource get() {
                if (resnm == null)
                    throw (new LoadingIndir(CachedRes.this));
                if (res == null) {
                    try {
                        res = Resource.remote().load(resnm, resver, prio).get();
                    } catch (Loading l) {
                        throw (l);
                    } catch (RuntimeException e) {
                        dev.simpleLog(e);
                        res = new Resource.FakeResource(resnm, resver);
                    }
                }
                return (res);
            }

            public String toString() {
                if (res == null) {
                    return ("<" + resid + (resnm != null ? ":" + resnm + "(" + resver + ")" : "") + ">");
                } else {
                    return ("<" + resid + ":" + res + ">");
                }
            }

            private void reset() {
                res = null;
            }
        }

        private class SRef extends Ref {
            public SRef(Resource res) {
                this.res = res;
            }
        }

        private Ref get() {
            Ref ind = (this.ind == null) ? null : (this.ind.get());
            if (ind == null)
                this.ind = new WeakReference<>(ind = new Ref());
            return (ind);
        }

        public void boostprio(int prio) {
            if (this.prio < prio)
                this.prio = prio;
        }

        public void set(String nm, int ver) {
            Resource.remote().load(nm, ver, -10);
            synchronized (this) {
                this.resnm = nm;
                this.resver = ver;
                get().reset();
                wq.wnotify();
                notifyAll();
            }
        }

        public void set(Resource res) {
            synchronized (this) {
                this.resnm = res.name;
                this.resver = res.ver;
                ind = new WeakReference<>(new SRef(res));
                wq.wnotify();
                notifyAll();
            }
        }
    }

    private CachedRes cachedres(int id) {
        synchronized (rescache) {
            CachedRes ret = rescache.get(id);
            if (ret != null)
                return (ret);
            ret = new CachedRes(id);
            rescache.put(id, ret);
            return (ret);
        }
    }

    public void allCache() {
        for (Map.Entry<Integer, CachedRes> entry : rescache.entrySet()) {
            System.out.println("Resource [" + entry.getKey() + " : " + entry.getValue().get() + "]");
        }
    }

    private int cacheres(String resname) {
        return cacheres(Resource.remote().loadwait(resname));
    }

    private int cacheres(Resource res) {
        cachedres(--localCacheId).set(res);
        return localCacheId;
    }

    public Indir<Resource> getres(int id, int prio) {
        CachedRes res = cachedres(id);
        res.boostprio(prio);
        return (res.get());
    }

    public Indir<Resource> getres(int id) {
        return (getres(id, 0));
    }

    public Indir<Resource> dynres(UID uid) {
        return (Resource.remote().dynres(uid));
    }

    public int getresid(Resource res) {
        synchronized (rescache) {
            for (Map.Entry<Integer, CachedRes> entry : rescache.entrySet()) {
                try {
                    if (entry.getValue().get().get() == res) {
                        return entry.getKey();
                    }
                } catch (Loading ignored) {
                }
            }
        }
        return -1;
    }

    public int getresidf(Resource res) {
        int id = getresid(res);
        if (id == -1) {
            id = cacheres(res);
        }
        return id;
    }

    private class ObjAck {
        long id;
        int frame;
        long recv;
        long sent;

        public ObjAck(long id, int frame, long recv) {
            this.id = id;
            this.frame = frame;
            this.recv = recv;
            this.sent = 0;
        }
    }

    private class Ticker extends HackThread {
        public Ticker() {
            super("Server time ticker");
            setDaemon(true);
        }

        public void run() {
            try {
                while (true) {
                    long now, then;
                    then = System.currentTimeMillis();
                    glob.oc.tick();
                    now = System.currentTimeMillis();
                    if (now - then < 70)
                        Thread.sleep(70 - (now - then));
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private void handlerel(PMessage msg) {
        if ((msg.type == RMessage.RMSG_NEWWDG) || (msg.type == RMessage.RMSG_WDGMSG) ||
                (msg.type == RMessage.RMSG_DSTWDG) || (msg.type == RMessage.RMSG_ADDWDG) || (msg.type == RMessage.RMSG_WDGBAR)) {
            postuimsg(msg);
        } else if (msg.type == RMessage.RMSG_MAPIV) {
            glob.map.invalblob(msg);
        } else if (msg.type == RMessage.RMSG_GLOBLOB) {
            glob.blob(msg);
        } else if (msg.type == RMessage.RMSG_RESID) {
            int resid = msg.uint16();
            String resname = msg.string();
            int resver = msg.uint16();
            cachedres(resid).set(resname, resver);
            dev.simpleLog(String.format("Resource %d:%s(v%d)", resid, resname, resver));
        } else if (msg.type == RMessage.RMSG_SFX) {
            Indir<Resource> res = getres(msg.uint16());
            double vol = ((double) msg.uint16()) / 256.0;
            double spd = ((double) msg.uint16()) / 256.0;
//                Audio.play(res);
            glob.loader.defer(() -> {
                Audio.CS clip = Audio.fromres(res.get());
                if (spd != 1.0)
                    clip = new Audio.Resampler(clip).sp(spd);
                if (vol != 1.0)
                    clip = new Audio.VolAdjust(clip, vol);
                Audio.play(clip);
            }, null);
            ;
        } else if (msg.type == RMessage.RMSG_MUSIC) {
            String resnm = msg.string();
            int resver = msg.uint16();
            boolean loop = !msg.eom() && (msg.uint8() != 0);
            if (Music.enabled) {
                if (resnm.equals(""))
                    Music.play(null, false);
                else
                    Music.play(Resource.remote().load(resnm, resver), loop);
            }
        } else if (msg.type == RMessage.RMSG_SESSKEY) {
            sesskey = msg.bytes();
        } else {
            throw (new MessageException("Unknown rmsg type: " + msg.type, msg));
        }
    }

    private final Connection.Callback conncb = new Connection.Callback() {
        public void closed() {
            synchronized (uimsgs) {
                closed = true;
                uimsgs.notifyAll();
            }
            synchronized (Session.this) {
                Session.this.notifyAll();
            }
        }

        public void handle(PMessage msg) {
            handlerel(msg);
        }

        public void handle(OCache.ObjDelta delta) {
            glob.oc.receive(delta);
        }

        public void mapdata(Message msg) {
            glob.map.mapdata(msg);
        }
    };

    public Session(SocketAddress server, String username, byte[] cookie, Object... args) throws InterruptedException {
        this.conn = new Connection(server, username);
        this.details = new SessionDetails(this);
        this.username = username;
        glob = new Glob(this);
        conn.add(conncb);
        conn.connect(cookie, args);
        character = new CharacterInfo();
        try {
            sk = new DatagramSocket();
//            sk = DatagramChannel.open();
//            sk.configureBlocking(true);
        } catch (IOException e) {
            throw (new RuntimeException(e));
        }
        ticker = new Ticker();
        ticker.start();
        Arrays.stream(LOCAL_CACHED).forEach(this::cacheres);
        Config.setUserName(username);
    }

    public void close() {
        conn.close();
        ticker.interrupt();
    }

    public synchronized boolean alive() {
        return (conn.alive());
    }

    public void queuemsg(PMessage pmsg) {
        conn.queuemsg(pmsg);
    }

    public void postuimsg(PMessage msg) {
        synchronized (uimsgs) {
            uimsgs.add(msg);
            uimsgs.notifyAll();
        }
    }

    public PMessage getuimsg() throws InterruptedException {
        synchronized (uimsgs) {
            while (true) {
                if (!uimsgs.isEmpty())
                    return (uimsgs.remove());
                if (closed)
                    return (null);
                uimsgs.wait();
            }
        }
    }

    public void sendmsg(PMessage msg) {
        conn.send(msg);
    }

    public void sendmsg(byte[] msg) {
        conn.send(ByteBuffer.wrap(msg));
    }
}