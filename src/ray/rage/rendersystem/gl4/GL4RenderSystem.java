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
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.gl2.GLUT;
import java.awt.Canvas;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import myGameEngine.Helpers.HudText;
import myGameEngine.Helpers.SortableRenderable;
import myGameEngine.Helpers.SortableSecondary;
import ray.rage.rendersystem.RenderQueue;
import ray.rage.rendersystem.RenderSystem;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.Viewport;
import ray.rage.rendersystem.RenderSystem.API;
import ray.rage.rendersystem.RenderSystem.Capabilities;
import ray.rage.rendersystem.Renderable.DataSource;
import ray.rage.rendersystem.Renderable.Primitive;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.shader.GpuShaderProgramFactory;
import ray.rage.rendersystem.shader.GpuShaderProgram.Context;
import ray.rage.rendersystem.shader.GpuShaderProgram.Type;
import ray.rage.rendersystem.shader.glsl.GlslProgramFactory;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.AmbientLight;
import ray.rage.scene.Light;
import ray.rage.scene.TessellationBody;
import ray.rml.Matrix4;
import ray.rml.Matrix4f;
import ray.rml.Vector3;

public final class GL4RenderSystem implements RenderSystem, GLEventListener {
    private static final Logger logger = Logger.getLogger(GL4RenderSystem.class.getName());
    private static final int INVALID_ID = -1;
    private int vertexArrayObjId = -1;
    private Map<Type, GpuShaderProgram> gpuProgramMap = new HashMap();
    private GpuShaderProgramFactory gpuProgramFactory = new GlslProgramFactory();
    private RenderQueue renderQueue;
    private Matrix4 viewMatrix = Matrix4f.createIdentityMatrix();
    private Matrix4 projMatrix = Matrix4f.createIdentityMatrix();
    private RenderWindow window;
    private GLCanvas canvas;
    private AtomicBoolean updateRequested = new AtomicBoolean(false);
    private AtomicBoolean contextInitialized = new AtomicBoolean(false);
    private Capabilities capabilities;
    private List<Light> lightsList;
    private AmbientLight ambientLight;
    private GLUT glut = new GLUT(); /* MyChange: COPIED FROM DISTRIBUTION */
    private String HUDstring = ""; /* MyChange: COPIED FROM DISTRIBUTION */
    private String HUDstring2 = ""; /* MyChange: COPIED FROM DISTRIBUTION */
    private int HUDfont = 5; /* MyChange: COPIED FROM DISTRIBUTION */
    private int HUDx = 5; /* MyChange: COPIED FROM DISTRIBUTION */
    private int HUDy = 5; /* MyChange: COPIED FROM DISTRIBUTION */
    private int HUDx2 = 5; /* MyChange: COPIED FROM DISTRIBUTION */
    private int HUDy2 = 20; /* MyChange: COPIED FROM DISTRIBUTION */

    private ArrayList<HudText> hudTexts = new ArrayList<HudText>(); /* MyChange: CHANGED HUD TO ARRAY */

    public GL4RenderSystem() {
        try {
            final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL4bc)); /* MyChange: COPIED GL4bc FROM DISTRIBUTION */
            caps.setBackgroundOpaque(true);
            caps.setDoubleBuffered(true);
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(8);
            this.canvas = new GLCanvas(caps);
            this.canvas.addGLEventListener(this);
            this.canvas.setAutoSwapBufferMode(false);
        } catch (GLException var2) {
            throw new RuntimeException("Could not create OpenGL 4 context. Check your hardware or drivers", var2);
        }
    }

    public API getAPI() {
        return API.OPENGL_4;
    }

    public Canvas getCanvas() {
        return this.canvas;
    }

    public RenderWindow createRenderWindow(DisplayMode displayMode, boolean fullScreen) {
        if (this.window == null) {
            this.window = new GL4RenderWindow(this.canvas, displayMode, fullScreen);
        }

        return this.window;
    }

    public RenderWindow createRenderWindow(boolean fullScreen) {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return this.createRenderWindow(gd.getDisplayMode(), fullScreen);
    }

    public RenderQueue createRenderQueue() {
        return new GL4RenderQueue();
    }

    public boolean isInitialized() {
        return this.contextInitialized.get();
    }

    public void processRenderQueue(RenderQueue rq, Matrix4 view, Matrix4 proj) {
        if (rq == null) {
            throw new NullPointerException("Null " + RenderQueue.class.getSimpleName());
        } else if (view == null) {
            throw new NullPointerException("Null view matrix");
        } else if (proj == null) {
            throw new NullPointerException("Null projection matrix");
        } else {
            this.renderQueue = rq;
            this.viewMatrix = view;
            this.projMatrix = proj;
            this.updateRequested.set(true);
            this.canvas.display();
            this.updateRequested.set(false);
            this.renderQueue = null;
            this.viewMatrix = null;
            this.projMatrix = null;
        }
    }

    public void clearViewport(Viewport vp) {
        if (vp == null) {
            throw new NullPointerException("Null viewport");
        } else {
            GLContext ctx = this.canvas.getContext();

            try {
                ctx.makeCurrent();
            } catch (GLException var4) {
                throw new RuntimeException(var4);
            }

            GL4 gl = ctx.getGL().getGL4();
            gl.glViewport(vp.getActualLeft(), vp.getActualBottom(), vp.getActualWidth(), vp.getActualHeight());
            gl.glScissor(vp.getActualScissorLeft(), vp.getActualScissorBottom(), vp.getActualScissorWidth(), vp.getActualScissorHeight());
            gl.glClearBufferfv(6144, 0, vp.getClearColorBuffer());
            gl.glClearBufferfv(6145, 0, vp.getClearDepthBuffer());
            ctx.release();
        }
    }

    public void swapBuffers() {
        this.canvas.swapBuffers();
    }

    /* MyChange: ADDED FUNCTION */
    public void addHud(HudText hudText) {
        hudTexts.add(hudText);
    }

    /* MyChange: CHANGED FUNCTION FROM DISTRIBUTION */
    @Override
    public void setHUD(String string, int x, int y) {
    }

    /* MyChange: CHANGED FUNCTION FROM DISTRIBUTION */
    @Override
    public void setHUD(String string) {
    }

    /* MyChange: CHANGED FUNCTION FROM DISTRIBUTION */
    public void setHUD2(String string, int x, int y) {
    }

    /* MyChange: CHANGED FUNCTION FROM DISTRIBUTION */
    public void setHUD2(String string) {
    }

    public void init(GLAutoDrawable glad) {
        GL4 gl = (GL4) glad.getGL();

        capabilities = new GL4RenderSystemCaps(gl);

        // TODO: turn all remaining enable/disable/etc. calls to render states
        // at some point to allow changes after this initialization(?)

        gl.glEnable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthFunc(GL4.GL_LEQUAL);
        gl.glEnable(GL4.GL_SCISSOR_TEST);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glEnable(GL4.GL_TEXTURE_CUBE_MAP_SEAMLESS);
        gl.glEnable(GL.GL_BLEND);  /* MyChange: ADDED LINE */
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); /* MyChange: ADDED LINE */
        gl.setSwapInterval(0); /* MyChange: disables VSync */

        int[] vaos = new int[1];
        gl.glGenVertexArrays(vaos.length, vaos, 0);
        vertexArrayObjId = vaos[0];

        gl.glBindVertexArray(vertexArrayObjId);

        contextInitialized.set(true);
    }

    /* MyChange: CREATED FUNCTION */
    private void doRender(GL4 gl, Renderable r) {
        GpuShaderProgram program = r.getGpuShaderProgram();
        if (program == null) {
            logger.severe(Renderable.class.getSimpleName() + " skipped. No "
                    + GpuShaderProgram.class.getSimpleName() + " set");
            return;
        }
        gl.glDepthMask(true);
        setRenderStates(r);
        final Context ctx = program.createContext();
        ctx.setRenderable(r);
        ctx.setViewMatrix(viewMatrix);
        ctx.setProjectionMatrix(projMatrix);
        ctx.setLightsList(lightsList);
        ctx.setAmbientLight(ambientLight);

        program.bind();
        program.fetch(ctx);
        drawRenderable(gl, r);
        program.unbind();

        ctx.notifyDispose();
    }

    /* MyChange: REWROTE FUNCTION */
    @Override
    public void display(GLAutoDrawable glad) {
        // this prevents automatic display invocations during window/context
        // creation from proceeding, which will result in a crash b/c the other
        // assets/dependencies will not be ready/setup before this is
        // auto-invoked the first time; it also prevents attempts to draw before
        // the context has been initialized in its own separate thread
        if (!updateRequested.get() || !contextInitialized.get())
            return;

        GL4 gl = (GL4) glad.getGL();

        // render opaque, gather transparencies (alphas)
        PriorityQueue<SortableRenderable> alphas = new PriorityQueue<>();
        PriorityQueue<SortableSecondary> secondStage = new PriorityQueue<>();
        for (Renderable r : renderQueue) {
            try {
                // detect secondary stage
                ZBufferState zbs = (ZBufferState)r.getRenderState(RenderState.Type.ZBUFFER);
                if (zbs.getSecondaryStage() > 0) {
                    secondStage.add(new SortableSecondary(r));
                    continue;
                }
            } catch (RuntimeException ex) { }
            // detect alphas
            if (r.getMaterial() != null && r.getMaterial().getName().contains("alpha")) { alphas.add(new SortableRenderable(r)); continue; }
            doRender(gl, r);
        }

        // render transparencies (alphas)
        while (alphas.size() > 0) {
            doRender(gl, alphas.poll().renderable);
        }

        // draw secondary stage
        gl.glDepthMask(true);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        while (secondStage.size() > 0) {
            doRender(gl, secondStage.poll().renderable);
        }

        // draw hud
        GL4bc gl4bc = (GL4bc)gl;
        for (HudText hudText : hudTexts) {
            gl4bc.glColor3f(hudText.color.getRed() / 255f, hudText.color.getGreen() / 255f, hudText.color.getBlue() / 255f);
            gl4bc.glWindowPos2d(hudText.renderX(canvas), hudText.renderY(canvas));
            this.glut.glutBitmapString(hudText.font, hudText.text);
        }
    }

    private void setRenderStates(Renderable r) {
        Iterator var3 = r.getRenderStates().iterator();

        while(var3.hasNext()) {
            RenderState state = (RenderState)var3.next();
            if (state.isEnabled()) {
                state.apply();
            }
        }

    }

    private void drawRenderable(GL4 gl, Renderable r) {
        final DataSource source = r.getDataSource();
        final int primitive = getGLPrimitive(r.getPrimitive());
        switch (source) {
            case INDEX_BUFFER:
                gl.glDrawElements(primitive, r.getIndexBuffer().capacity(), GL4.GL_UNSIGNED_INT, 0);
                break;
            case VERTEX_BUFFER:
                gl.glDrawArrays(primitive, 0, r.getVertexBuffer().capacity());
                break;
            case TESS_VERT_BUFFER:
                int quality = ((TessellationBody)r).getQualityTotal();
                gl.glPatchParameteri(36466, 4);
                gl.glDrawArraysInstanced(14, 0, 4, quality * quality);
                break;
            default:
                logger.severe("Draw call skipped. Invalid " + DataSource.class.getName() + ": " + source);
                break;
        }
    }

    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        Iterator var7 = this.window.getViewports().iterator();

        while(var7.hasNext()) {
            Viewport vp = (Viewport)var7.next();
            vp.notifyDimensionsChanged();
        }

    }

    public void dispose(GLAutoDrawable glad) {
        GL4 gl = (GL4)glad.getGL();
        gl.glBindVertexArray(0);
        Iterator var4 = this.gpuProgramMap.values().iterator();

        while(var4.hasNext()) {
            GpuShaderProgram program = (GpuShaderProgram)var4.next();
            program.notifyDispose();
        }

        this.gpuProgramMap.clear();
        int[] vaos = new int[]{this.vertexArrayObjId};
        gl.glDeleteVertexArrays(vaos.length, vaos, 0);
        this.vertexArrayObjId = -1;
        this.contextInitialized = null;
        this.capabilities = null;
        this.gpuProgramMap = null;
        this.gpuProgramFactory = null;
        this.renderQueue = null;
        this.projMatrix = null;
        this.viewMatrix = null;
        this.window = null;
        this.lightsList = null;
        this.ambientLight = null;
    }

    public RenderWindow getRenderWindow() {
        return this.window;
    }

    public Capabilities getCapabilities() {
        return this.capabilities;
    }

    @Override
    public RenderState createRenderState(RenderState.Type type) {
        if (type == null)
            throw new NullPointerException("Null " + RenderState.Type.class.getSimpleName());

        switch (type) {
            case ZBUFFER:
                return new GL4ZBufferState(canvas);
            case TEXTURE:
                // TODO: Add TBO ref to validate buffer IDs, etc
                return new GL4TextureState(capabilities, canvas);
            case FRONT_FACE:
                return new GL4FrontFaceState(canvas);
            default:
                throw new RuntimeException("Unimplemented " + RenderState.Type.class.getSimpleName() + ": " + type);
        }
    }

    public GpuShaderProgram createGpuShaderProgram(Type type) {
        if (this.gpuProgramMap.containsKey(type)) {
            throw new RuntimeException(GpuShaderProgram.class.getSimpleName() + " already exists: " + type);
        } else {
            GpuShaderProgram program = this.gpuProgramFactory.createInstance(this, type);
            this.gpuProgramMap.put(type, program);
            return program;
        }
    }

    public GpuShaderProgram getGpuShaderProgram(Type type) {
        GpuShaderProgram program = (GpuShaderProgram)this.gpuProgramMap.get(type);
        if (program == null) {
            throw new RuntimeException(GpuShaderProgram.class.getSimpleName() + " does not exist: " + type);
        } else {
            return program;
        }
    }

    public void setActiveLights(List<Light> lights) {
        this.lightsList = lights;
    }

    public void setAmbientLight(AmbientLight ambient) {
        this.ambientLight = ambient;
    }

    public void notifyDispose() {
        this.window.notifyDispose();
        this.canvas.disposeGLEventListener(this, false);
    }

    private static int getGLPrimitive(Primitive primitive) {
        switch (primitive) {
            case TRIANGLES:
                return GL4.GL_TRIANGLES;
            case TRIANGLE_STRIP:
                return GL4.GL_TRIANGLE_STRIP;
            case LINES:
                return GL4.GL_LINES;
            case POINTS:
                return GL4.GL_POINTS;
            default:
                logger.severe("Unimplemented primitive: " + primitive + ". Using " + Primitive.TRIANGLES);
                return GL4.GL_TRIANGLES;
        }
    }
}
