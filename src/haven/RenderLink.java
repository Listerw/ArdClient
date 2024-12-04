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

import java.util.ArrayList;

public interface RenderLink {
    Rendered make(Sprite.Owner owner);

    default Rendered make() {
        return (make(null));
    }

    class MeshMat implements RenderLink {
        public final Resource srcres;
        public final Indir<Resource> mesh, mat;
        public final int meshid, matid;
        private Rendered res = null;

        public MeshMat(Resource srcres, Indir<Resource> mesh, int meshid, Indir<Resource> mat, int matid) {
            this.srcres = srcres;
            this.mesh = mesh;
            this.meshid = meshid;
            this.mat = mat;
            this.matid = matid;
        }

        public static MeshMat parse(Resource res, Message buf) {
            String meshnm = buf.string();
            int meshver = buf.uint16();
            int meshid = buf.int16();
            String matnm = buf.string();
            int matver = buf.uint16();
            int matid = buf.int16();
            Indir<Resource> mesh = meshnm.equals("") ? res.indir() : res.pool.load(meshnm, meshver);
            Indir<Resource> mat = matnm.equals("") ? res.indir() : res.pool.load(matnm, matver);
            return (new MeshMat(res, mesh, meshid, mat, matid));
        }

        public Rendered make(Sprite.Owner owner) {
            if (res == null) {
                FastMesh m = null;
                for (FastMesh.MeshRes mr : mesh.get().layers(FastMesh.MeshRes.class)) {
                    if ((meshid < 0) || (mr.id == meshid)) {
                        m = mr.m;
                        break;
                    }
                }
                Material M = null;
                for (Material.Res mr : mat.get().layers(Material.Res.class)) {
                    if ((matid < 0) || (mr.id == matid)) {
                        M = mr.get();
                        break;
                    }
                }
                if (m == null)
                    throw (new Sprite.ResourceException("Could not find specified mesh by ID " + meshid, srcres));
                if (M == null)
                    throw (new Sprite.ResourceException("Could not find specified material by ID " + matid, srcres));
                res = M.apply(m);
            }
            return (res);
        }
    }

    class AmbientLink implements RenderLink {
        public final Indir<Resource> res;

        public AmbientLink(Indir<Resource> res) {
            this.res = res;
        }

        public static AmbientLink parse(Resource res, Message buf) {
            String nm = buf.string();
            int ver = buf.uint16();
            return (new AmbientLink(res.pool.load(nm, ver)));
        }

        public Rendered make(Sprite.Owner owner) {
            return (new ActAudio.Ambience(res.get()));
        }
    }

    class Collect implements RenderLink {
        public final Indir<Resource> from;
        public final int meshid, meshmask;
        private Rendered res;

        public Collect(Indir<Resource> from, int meshid, int meshmask) {
            this.from = from;
            this.meshid = meshid;
            this.meshmask = meshmask;
        }

        public static Collect parse(Resource res, Message buf) {
            String nm = buf.string();
            int ver = buf.uint16();
            Indir<Resource> lres = res.pool.load(nm, ver);
            int meshid = buf.int16();
            int meshmask = buf.eom() ? -1 : buf.int16();
            return (new Collect(lres, meshid, meshmask));
        }

        public Rendered make(Sprite.Owner owner) {
            if (res == null) {
                ArrayList<Rendered> cl = new ArrayList<>();
                for (FastMesh.MeshRes mr : from.get().layers(FastMesh.MeshRes.class)) {
                    if (((meshid >= 0) && (mr.id < 0)) || ((mr.id & meshmask) == meshid))
                        cl.add(mr.mat.get().apply(mr.m));
                }
                final Rendered[] ca = cl.toArray(new Rendered[0]);
                res = new Rendered() {
                    public void draw(GOut g) {
                    }

                    public boolean setup(RenderList ls) {
                        for (Rendered r : ca)
                            ls.add(r, null);
                        return (false);
                    }
                };
            }
            return (res);
        }
    }

    class ResSprite implements RenderLink {
        public final Indir<Resource> res;

        public ResSprite(Indir<Resource> res) {
            this.res = res;
        }

        public static ResSprite parse(Resource res, Message buf) {
            String nm = buf.string();
            int ver = buf.uint16();
            return (new ResSprite(res.pool.load(nm, ver)));
        }

        public Rendered make(Sprite.Owner owner) {
            return (Sprite.create(owner, res.get(), Message.nil));
        }
    }

    class Parameters implements RenderLink {
        public final Resource from;
        public final Indir<Resource> res;
        public final Object[] args;
        private Resource lres;
        private RenderLink link = null;

        public Parameters(Resource from, Indir<Resource> res, Object[] args) {
            this.from = from;
            this.res = res;
            this.args = args;
        }

        public static Parameters parse(Resource res, Message buf) {
            String nm = buf.string();
            int ver = buf.uint16();
            Object[] args = buf.list(new Resource.PoolMapper(res.pool));
            return (new Parameters(res, res.pool.load(nm, ver), args));
        }

        public Rendered make(Sprite.Owner owner) {
            if (link == null) {
                synchronized (this) {
                    if (link == null) {
                        if (lres == null)
                            lres = res.get();
                        link = lres.getcode(ArgLink.class, true).parse(from, args);
                    }
                }
            }
            return (link.make(owner));
        }
    }

    class ArgMaker extends Resource.PublishedCode.Instancer.Chain<ArgLink> {
        public ArgMaker() {
            super(ArgLink.class);
            add(new Direct<>(ArgLink.class));
            add(new StaticCall<>(ArgLink.class, "mkrlink", RenderLink.class, new Class<?>[]{Resource.class, Object[].class},
                    (make) -> new ArgLink() {
                        @Override
                        public RenderLink parse(Resource res, Object... args) {
                            return (make.apply(new Object[]{res, args}));
                        }

                        public Rendered create(Sprite.Owner owner, Resource res, Object... args) {
                            throw (new RuntimeException("unimplemented ArgLink.create() called"));
                        }
                    }));
            add(new Construct<>(ArgLink.class, RenderLink.class, new Class<?>[]{Resource.class, Object[].class},
                    (cons) -> new ArgLink() {
                        @Override
                        public RenderLink parse(Resource res, Object... args) {
                            return (cons.apply(new Object[]{res, args}));
                        }

                        public Rendered create(Sprite.Owner owner, Resource res, Object... args) {
                            throw (new RuntimeException("unimplemented ArgLink.create() called"));
                        }
                    }));
            add(new StaticCall<>(ArgLink.class, "mkrlink", Rendered.class, new Class<?>[]{Sprite.Owner.class, Resource.class, Object[].class}, (make) -> (owner, res, args) -> make.apply(new Object[]{owner, res, args})));
            add(new Construct<>(ArgLink.class, Rendered.class, new Class<?>[]{Sprite.Owner.class, Resource.class, Object[].class}, (cons) -> (owner, res, args) -> cons.apply(new Object[]{owner, res, args})));
        }
    }

    @Resource.PublishedCode(name = "rlink", instancer = ArgMaker.class)
    interface ArgLink {
        default RenderLink parse(Resource res, Object... args) {
            return (owner -> this.create(owner, res, args));
        }

        @Deprecated
        Rendered create(Sprite.Owner owner, Resource res, Object... args);
    }

    @Resource.LayerName("rlink")
    class Res extends Resource.Layer implements Resource.IDLayer<Integer> {
        public transient final RenderLink l;
        public final int id;

        public Res(Resource res, Message buf) {
            res.super();
            int lver = buf.uint8();
            int t;
            if (lver < 3) {
                t = lver;
                id = -1;
            } else if (lver == 3) {
                id = buf.int16();
                t = buf.uint8();
            } else {
                throw (new Resource.LoadException("Invalid renderlink version: " + lver, res));
            }
            if (t == 0) {
                l = MeshMat.parse(res, buf);
            } else if (t == 1) {
                l = AmbientLink.parse(res, buf);
            } else if (t == 2) {
                l = Collect.parse(res, buf);
            } else if (t == 3) {
                l = Parameters.parse(res, buf);
            } else if (t == 4) {
                l = ResSprite.parse(res, buf);
            } else {
                throw (new Resource.LoadException("Invalid renderlink type: " + t, res));
            }
        }

        public void init() {}

        public Integer layerid() {
            return (id);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<rlink ");
            sb.append(id).append(" ");
            sb.append("Layers: ");
            if (l instanceof MeshMat) {
                MeshMat mm = (MeshMat) l;
                sb.append("[(MeshMat) ");
                if (mm.srcres != null) {
                    sb.append("((srcres):(").append(mm.srcres).append(") ");
//                    for (Resource.Layer layer : mm.srcres.layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                if (mm.mesh != null) {
                    sb.append("((mesh):(").append(mm.mesh).append("(").append(mm.meshid).append(")").append(") ");
//                    for (Resource.Layer layer : mm.mesh.get().layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                if (mm.mat != null) {
                    sb.append("((mat):(").append(mm.mat).append("(").append(mm.matid).append(")").append(") ");
//                    for (Resource.Layer layer : mm.mat.get().layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                sb.append("]");
            }
            if (l instanceof AmbientLink) {
                AmbientLink al = (AmbientLink) l;
                sb.append("[(AmbientLink) ");
                if (al.res != null) {
                    sb.append("((res):(").append(al.res).append(") ");
//                    for (Resource.Layer layer : mm.srcres.layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                sb.append("]");
            }
            if (l instanceof Collect) {
                Collect cl = (Collect) l;
                sb.append("[(Collect) ");
                if (cl.from != null) {
                    sb.append("((from):(").append(cl.from).append(") ");
//                    for (Resource.Layer layer : mm.srcres.layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                sb.append("(").append(cl.meshid).append(")").append("(").append(cl.meshmask).append(")");
                sb.append("]");
            }
            if (l instanceof Parameters) {
                Parameters pl = (Parameters) l;
                sb.append("[(Parameters) ");
                if (pl.res != null) {
                    sb.append("((res):(").append(pl.res).append(") ");
//                    for (Resource.Layer layer : mm.srcres.layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                if (pl.lres != null) {
                    sb.append("((lres):(").append(pl.lres).append(") ");
//                    for (Resource.Layer layer : mm.mesh.get().layers()) {
//                        sb.append("[").append(layer).append("]");
//                    }
                    sb.append(")");
                }
                sb.append("]");
            }
            sb.append(">");
            return sb.toString();
        }
    }
}
