package myGameEngine.GameEntities;

import myGameEngine.Singletons.UniqueCounter;
import myGameEngine.Singletons.EngineManager;
import ray.rage.Engine;
import ray.rage.asset.material.Material;
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.Viewport;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.*;
import ray.rage.util.BufferUtil;
import ray.rml.Vector2;
import ray.rml.Vector3;

import java.awt.*;
import java.io.IOException;

public class HudElement extends GameEntity implements Camera.Listener {
    private ManualObject obj;
    private Material mat;
    private TextureState texState;
    private Texture tex;
    private Vector2 screenLocation;

    public HudElement(SceneNode parentNode, float scale, Vector2 screenLocation, Vector2 origin, String textureName, Color color) throws IOException {
        super(false);

        this.screenLocation = screenLocation;

        Engine engine = EngineManager.getEngine();
        SceneManager sm = EngineManager.getSceneManager();

        String name = "HudElement" + UniqueCounter.next();
        obj = sm.createManualObject(name);
        addResponsibility(obj);
        obj.createManualSection(name + "Section");

        texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setMinificationFilter(TextureState.MinificationFilter.BILINEAR_NO_MIPMAPS);
        tex = engine.getTextureManager().getAssetByPath(textureName);
        texState.setTexture(tex);

        // divide dimensions due to how we set up vertices
        float aspectRatio = tex.getImage().getWidth() / (float)tex.getImage().getHeight();
        float width = aspectRatio * scale;
        float height = scale;

        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        // populate arrays
        float[] vertices = new float[] {
                -width + origin.x() * width, -height + origin.y() * height, 0, // BL
                -width + origin.x() * width, height + origin.y() * height, 0,  // BR
                width + origin.x() * width, -height + origin.y() * height, 0,  // TL
                width + origin.x() * width, height + origin.y() * height, 0    // TR
        };
        float[] normals = new float[]{
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1
        };
        float[] texcoords = new float[] {
                1, 0, // BL
                1, 1, // BR
                0, 0, // TL
                0, 1, //TR
        };
        int[] indices = new int[] {
                0, 1, 2,
                2, 1, 3
        };

        // Convert array builders to object
        obj.setVertexBuffer(BufferUtil.directFloatBuffer(vertices));
        obj.setTextureCoordBuffer(BufferUtil.directFloatBuffer(texcoords));
        obj.setNormalsBuffer(BufferUtil.directFloatBuffer(normals));
        obj.setIndexBuffer(BufferUtil.directIntBuffer(indices));

        // create and load billboard texture & material
        mat = engine.getMaterialManager().createManualAsset("hud-element-alpha" +  name);
        addResponsibility(mat);
        mat.setEmissive(Color.WHITE);
        mat.setAmbient(color);

        obj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        obj.setRenderState(texState);
        obj.setMaterial(mat);

        // set no depth testing
        ZBufferState zBufferState = (ZBufferState) sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
        zBufferState.setSecondaryStage((int)UniqueCounter.next() + 10);
        zBufferState.setWritable(false);
        obj.setRenderState(zBufferState);

        // create dummy node and attach our obj
        node = parentNode.createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);

        // remember camera because we have to point to it every frame
        for (Camera camera : sm.getCameras()) {
            camera.addListener(this);
        }
    }

    public ManualObject getManualObject() { return obj; }
    public Material getMaterial() { return mat; }

    public void updateTexture(String textureName){
        Texture texture = null;
        try {
            texture = EngineManager.getSceneManager().getTextureManager().getAssetByPath(textureName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextureState textureState = (TextureState)EngineManager.getSceneManager().getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        textureState.setTexture(texture);
        obj.setRenderState(textureState);
    }

    @Override
    public void onCameraPreRenderScene(Camera camera) {

        // get default position according to aspect ratio
        Viewport vp = EngineManager.getRenderWindow().getViewport(0);
        float aspectRatio = (float)vp.getActualWidth() / (float)vp.getActualHeight();

        float h = 0.0577434f;
        float w = h * aspectRatio;
        node.setLocalPosition((w / -2f) * screenLocation.x(), (h / 2f) * screenLocation.y(), 0);
    }

    @Override
    public void onCameraPostRenderScene(Camera camera) { }
}
