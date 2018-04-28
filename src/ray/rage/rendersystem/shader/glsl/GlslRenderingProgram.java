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

package ray.rage.rendersystem.shader.glsl;

import java.awt.*;
import java.nio.*;
import java.util.List;

import com.jogamp.opengl.awt.*;

import ray.rage.asset.material.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.shader.*;
import ray.rage.scene.*;
import ray.rage.util.*;
import ray.rml.*;

/**
 * Concrete implementation of a {@link GpuShaderProgram shader-program} to
 * process {@link Renderable renderables}.
 * <p>
 * This implementation contains, and submits, all the input data for the
 * <code>renderables.vert</code> vertex shader. Changes in the inputs of that
 * program will require changes to this implementation.
 *
 * @author Raymond L. Rivera
 *
 */
class GlslRenderingProgram extends AbstractGlslProgram {

    private boolean                        initialized = false;

    private GlslProgramAttributeBufferVec3 positionsBuffer;
    private GlslProgramAttributeBufferVec2 texcoordsBuffer;
    private GlslProgramAttributeBufferVec3 normalsBuffer;
    private GlslProgramIndexBuffer         indexBuffer;

    private GlslProgramStorageBufferFloat  lightsBuffer;

    private GlslProgramUniformMat4         modelViewMatrix;
    private GlslProgramUniformMat4         projMatrix;
    private GlslProgramUniformMat4         normalMatrix;

    private GlslProgramUniformVec4         ambientLightIntensity;

    private GlslProgramUniformVec4         materialAmbient;
    private GlslProgramUniformVec4         materialDiffuse;
    private GlslProgramUniformVec4         materialSpecular;
    private GlslProgramUniformVec4         materialEmissive;
    private GlslProgramUniformFloat        materialShininess;

    public GlslRenderingProgram(GLCanvas canvas) {
        super(canvas);
    }

    @Override
    public Type getType() {
        return Type.RENDERING;
    }

    @Override
    public void fetchImpl(Context ctx) {
        if (!initialized)
            init();

        final Renderable r = ctx.getRenderable();
        final Matrix4 model = r.getWorldTransformMatrix();
        final Matrix4 view = ctx.getViewMatrix();
        final Matrix4 proj = ctx.getProjectionMatrix();

        setRenderable(r);
        setGlobalAmbientLight(ctx.getAmbientLight());
        setLocalLights(ctx.getLightsList(), view);
        setMaterial(r.getMaterial());
        setMatrixUniforms(model, view, proj);
    }

    private void setRenderable(Renderable r) {
        FloatBuffer fb = r.getVertexBuffer();
        if (canSubmitBuffer(fb))
            positionsBuffer.set(fb);

        fb = r.getTextureCoordsBuffer();
        if (canSubmitBuffer(fb))
            texcoordsBuffer.set(fb);

        fb = r.getNormalsBuffer();
        if (canSubmitBuffer(fb))
            normalsBuffer.set(fb);

        IntBuffer ib = r.getIndexBuffer();
        if (canSubmitBuffer(ib))
            indexBuffer.set(ib);
    }

    private void setGlobalAmbientLight(AmbientLight ambientLight) {
        if (ambientLight == null)
            return;

        ambientLightIntensity.set(toVec4(ambientLight.getIntensity()));
    }

    private void setLocalLights(List<Light> lights, Matrix4 view) {
        if (lights == null || lights.size() == 0)
            return;

        // XXX: See renderables.frag, struct light_t member definition,
        // specifically the total size in bytes for the structure.
        //
        // If the number of fields and/or total byte size changes,
        // then the code below must be adjusted to match, including alignment.
        //
        // 25 floats + 3 floats of padding for alignment
        final int shaderStructFieldCount = 25 + 3;

        FloatBuffer ssbo = BufferUtil.directFloatBuffer(shaderStructFieldCount * lights.size());
        for (Light l : lights) {
            ssbo.put(l.getAmbient().getColorComponents(null));
            ssbo.put(1f);

            ssbo.put(l.getDiffuse().getColorComponents(null));
            ssbo.put(1f);

            ssbo.put(l.getSpecular().getColorComponents(null));
            ssbo.put(1f);

            final float w = l.getType() == Light.Type.DIRECTIONAL ? 0 : 1;
            Vector4 pos = view.mult(Vector4f.createFrom(l.getParentNode().getWorldPosition(), w));
            ssbo.put(pos.toFloatArray());

            ssbo.put(l.getConstantAttenuation());
            ssbo.put(l.getLinearAttenuation());
            ssbo.put(l.getQuadraticAttenuation());
            ssbo.put(l.getRange());

            Vector3 dir = view.toMatrix3().mult(l.getParentNode().getWorldForwardAxis()).normalize();
            ssbo.put(dir.toFloatArray());

            ssbo.put(l.getConeCutoffAngle().valueRadians());
            ssbo.put(l.getFalloffExponent());

            // padding required for 16-byte alignment in SSBO
            ssbo.put(0);
            ssbo.put(0);
            ssbo.put(0);
        }
        ssbo.rewind();
        lightsBuffer.set(ssbo);
    }

    private void setMaterial(Material mat) {
        if (mat == null)
            return;

        materialAmbient.set(toVec4(mat.getAmbient()));
        materialDiffuse.set(toVec4(mat.getDiffuse()));
        materialSpecular.set(toVec4(mat.getSpecular()));
        materialEmissive.set(toVec4(mat.getEmissive()));
        materialShininess.set(mat.getShininess());
    }

    private void setMatrixUniforms(Matrix4 model, Matrix4 view, Matrix4 proj) {
        final Matrix4 modelView = view.mult(model);
        modelViewMatrix.set(modelView);
        projMatrix.set(proj);
        normalMatrix.set(modelView.inverse().transpose());
    }

    private void init() {
        final GLCanvas canvas = getCanvas();

        positionsBuffer = new GlslProgramAttributeBufferVec3(this, canvas, "vertex_position");
        texcoordsBuffer = new GlslProgramAttributeBufferVec2(this, canvas, "vertex_texcoord");
        normalsBuffer = new GlslProgramAttributeBufferVec3(this, canvas, "vertex_normal");
        indexBuffer = new GlslProgramIndexBuffer(this, canvas);

        ambientLightIntensity = new GlslProgramUniformVec4(this, canvas, "global_light.intensity");
        lightsBuffer = new GlslProgramStorageBufferFloat(this, canvas);

        materialAmbient = new GlslProgramUniformVec4(this, canvas, "material.ambient");
        materialDiffuse = new GlslProgramUniformVec4(this, canvas, "material.diffuse");
        materialSpecular = new GlslProgramUniformVec4(this, canvas, "material.specular");
        materialEmissive = new GlslProgramUniformVec4(this, canvas, "material.emissive");
        materialShininess = new GlslProgramUniformFloat(this, canvas, "material.shininess");

        modelViewMatrix = new GlslProgramUniformMat4(this, canvas, "matrix.model_view");
        projMatrix = new GlslProgramUniformMat4(this, canvas, "matrix.projection");
        normalMatrix = new GlslProgramUniformMat4(this, canvas, "matrix.normal");

        initialized = true;
    }

    @Override
    public void notifyDispose() {
        if (initialized) {
            positionsBuffer.notifyDispose();
            texcoordsBuffer.notifyDispose();
            normalsBuffer.notifyDispose();
            indexBuffer.notifyDispose();

            ambientLightIntensity.notifyDispose();
            lightsBuffer.notifyDispose();

            materialAmbient.notifyDispose();
            materialDiffuse.notifyDispose();
            materialSpecular.notifyDispose();
            materialEmissive.notifyDispose();
            materialShininess.notifyDispose();

            modelViewMatrix.notifyDispose();
            projMatrix.notifyDispose();
            normalMatrix.notifyDispose();

            positionsBuffer = null;
            texcoordsBuffer = null;
            normalsBuffer = null;
            indexBuffer = null;

            ambientLightIntensity = null;
            lightsBuffer = null;

            materialAmbient = null;
            materialDiffuse = null;
            materialSpecular = null;
            materialEmissive = null;
            materialShininess = null;

            modelViewMatrix = null;
            projMatrix = null;
            normalMatrix = null;

            initialized = false;
        }
        super.notifyDispose();
    }

    private static Vector4 toVec4(Color c) {
        final float[] rgba = c.getComponents(null); /* MyChange: added alpha support */
        return Vector4f.createFrom(rgba[0], rgba[1], rgba[2], rgba[3]); /* MyChange: added alpha support */
    }

    private static boolean canSubmitBuffer(Buffer buff) {
        return buff != null && buff.capacity() > 0;
    }

}
