/**
 * Copyright (C) 2016 Raymond L. Rivera <ray.l.rivera@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ray.rage.rendersystem.gl4;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;

import ray.rage.rendersystem.states.*;

/**
 * A concrete implementation of the {@link ZBufferState z-buffer-state}
 * interface for a {@link GL4RenderSystem}.
 *
 * @author Raymond L. Rivera
 *
 */
final class GL4ZBufferState extends GL4AbstractRenderState implements ZBufferState {

    private boolean      testEnabled    = true;
    private boolean      bufferWritable = true;
    private TestFunction testFunction   = TestFunction.LESS_OR_EQUAL;
    private boolean secondaryStage      = false; /* My Change: added a secondary depth stage */

    GL4ZBufferState(GLCanvas canvas) {
        super(canvas);
    }

    @Override
    public Type getType() {
        return Type.ZBUFFER;
    }

    @Override
    public void setTestFunction(TestFunction function) {
        if (function == null)
            throw new NullPointerException("Null function");

        testFunction = function;
    }

    @Override
    public TestFunction getTestFunction() {
        return testFunction;
    }

    @Override
    public void setTestEnabled(boolean enabled) {
        testEnabled = enabled;
    }

    @Override
    public boolean hasTestEnabled() {
        return testEnabled;
    }

    @Override
    public void setWritable(boolean writable) {
        bufferWritable = writable;
    }

    @Override
    public boolean isWritable() {
        return bufferWritable;
    }

    @Override
    public void notifyDispose() {
        testEnabled = false;
        bufferWritable = false;
    }

    @Override
    protected void applyImpl(GL4 gl) {
        if (testEnabled) {
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glDepthMask(bufferWritable);
            gl.glDepthFunc(getGLTestFunction(testFunction));
        } else {
            // XXX: glDepthFunc(GL_ALWAYS) might be more efficient and would
            // behave exactly the same
            gl.glDisable(GL4.GL_DEPTH_TEST);
        }
    }

    @Override
    protected void disposeImpl(GL4 gl) {}

    private static int getGLTestFunction(TestFunction func) {
        switch (func) {
            case ALWAYS_FAIL:
                return GL4.GL_NEVER;
            case ALWAYS_PASS:
                return GL4.GL_ALWAYS;
            case EQUAL:
                return GL4.GL_EQUAL;
            case NOT_EQUAL:
                return GL4.GL_NOTEQUAL;
            case LESS:
                return GL4.GL_LESS;
            case LESS_OR_EQUAL:
                return GL4.GL_LEQUAL;
            case GREATER:
                return GL4.GL_GREATER;
            case GREATER_OR_EQUAL:
                return GL4.GL_GEQUAL;
            default:
                throw new IllegalArgumentException("Unknown function: " + func);
        }
    }

    @Override
    public boolean isSecondaryStage() { return secondaryStage; } /* My Change: added a secondary depth stage */
    public void setSecondaryStage(boolean secondaryStage) { this.secondaryStage = secondaryStage; } /* My Change: added a secondary depth stage */

}
