package myGameEngine.GameEntities;

import myGameEngine.Singletons.UniqueCounter;
import myGameEngine.Singletons.EngineManager;
import ray.rage.Engine;
import ray.rage.asset.material.Material;
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.*;
import ray.rage.util.BufferUtil;

import java.awt.*;
import java.io.IOException;

public class Billboard extends GameEntity implements Camera.Listener {
    private ManualObject obj;
    private SceneNode parentNode;
    private Material mat;
    private TextureState texState;
    private Texture tex;

    public Billboard(SceneNode parentNode, float width, float height, String textureName, Color color) throws IOException {
        super(true);

        Engine engine = EngineManager.getEngine();
        SceneManager sm = EngineManager.getSceneManager();

        String name = "Billboard" + UniqueCounter.next();
        obj = sm.createManualObject( name);
        addResponsibility(obj);
        obj.createManualSection(name + "Section");

        // divide dimensions due to how we set up vertices
        width = width / 2f;
        height = height / 2f;

        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        // populate arrays
        float[] vertices = new float[] {
            -width, -height, 0, // BL
            -width, height, 0,  // BR
            width, -height, 0,  // TL
            width, height, 0    // TR
        };
        float[] normals = new float[]{
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1
        };
        float[] texcoords = new float[] {
                0, 0, // BL
                0, 1, // BR
                1, 0, // TL
                1, 1, //TR
        };
        int[] indices = new int[] {
                1, 0, 2,
                1, 2, 3
        };

        // Convert array builders to object
        obj.setVertexBuffer(BufferUtil.directFloatBuffer(vertices));
        obj.setTextureCoordBuffer(BufferUtil.directFloatBuffer(texcoords));
        obj.setNormalsBuffer(BufferUtil.directFloatBuffer(normals));
        obj.setIndexBuffer(BufferUtil.directIntBuffer(indices));

        // create and load billboard texture & material
        mat = engine.getMaterialManager().createManualAsset("billboard-alpha" +  name);
        addResponsibility(mat);
        mat.setEmissive(Color.WHITE);
        mat.setAmbient(color);
        texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setMinificationFilter(TextureState.MinificationFilter.BILINEAR_NO_MIPMAPS);
        tex = engine.getTextureManager().getAssetByPath(textureName);
        texState.setTexture(tex);

        obj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        obj.setRenderState(texState);
        obj.setMaterial(mat);

        // create dummy node and attach our obj
        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);

        // remember parent node because we have to move there every frame
        this.parentNode = parentNode;

        // remember camera because we have to point to it every frame
        for (Camera camera : sm.getCameras()) {
            camera.addListener(this);
        }

    }

    public ManualObject getManualObject() { return obj; }
    public Material getMaterial() { return mat; }

    public void setSize(float width, float height) {
        float[] vertices = new float[] {
                -width, -height, 0, // BL
                -width, height, 0,  // BR
                width, -height, 0,  // TL
                width, height, 0    // TR
        };
        obj.setVertexBuffer(BufferUtil.directFloatBuffer(vertices));
    }

    @Override
    public void onCameraPreRenderScene(Camera camera) {
        // snap to parent node, look at camera
        if (this.parentNode != null) {
            node.setLocalPosition(parentNode.getWorldPosition());
            node.setLocalScale(parentNode.getWorldScale());
        }

        if (node == null || camera.getParentSceneNode() == null) { return; }
        try {
            node.lookAt(camera.getParentSceneNode().getWorldPosition(), camera.getParentSceneNode().getWorldUpAxis());
        } catch (Exception ex) {

        }

    }

    @Override
    public void onCameraPostRenderScene(Camera camera) { }
}
