package haven.res.lib.monochrome;

import haven.GLState;
import haven.GLState.Slot.Type;
import haven.GOut;
import haven.glsl.Cons;
import haven.glsl.Function;
import haven.glsl.Return;
import haven.glsl.ShaderMacro;
import haven.glsl.Uniform;
import haven.glsl.Variable;

import java.awt.Color;

public class Monochrome extends GLState {
    public static final GLState.Slot<Monochrome> slot = new GLState.Slot(Type.DRAW, Monochrome.class);
    public static final Uniform ccol = new Uniform(haven.glsl.Type.VEC3);
    public static final Uniform cstr = new Uniform(haven.glsl.Type.FLOAT);
    public final Color col;
    public final float str;

    public Monochrome(Color col, float str) {
        this.col = col;
        this.str = str;
    }

    public void reapply(GOut out) {
        out.gl.glUniform3f(out.st.prog.uniform(ccol), (float) col.getRed() / 255, (float) col.getGreen() / 255, (float) col.getBlue() / 255);
        out.gl.glUniform1f(out.st.prog.uniform(cstr), str);
    }

    public void apply(GOut out) {
        reapply(out);
    }

    public void unapply(GOut out) {
    }

    public void prep(GLState.Buffer buf) {
        buf.put(slot, this);
    }

    private static final ShaderMacro shader = (ctx) -> {
        Function.Def mono = new Function.Def(haven.glsl.Type.VEC4) {
            {
                Variable.Ref in = param(PDir.IN, haven.glsl.Type.VEC4).ref();
                code.add(new Return(Cons.vec4(Cons.mix(Cons.pick(in, "rgb"), Cons.mul(Monochrome.ccol.ref(), Cons.dot(Cons.pick(in, "rgb"), Cons.vec3(Cons.l(1.0 / 3.0)))), Monochrome.cstr.ref()), Cons.pick(in, "a"))));
            }
        };
        ctx.fctx.fragcol.mod(mono::call, 4000);
    };

    public ShaderMacro shader() {
        return (shader);
    }
}
