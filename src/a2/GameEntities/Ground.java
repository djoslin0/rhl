package a2.GameEntities;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import ray.rage.Engine;
import ray.rage.asset.material.Material;
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.ManualObject;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.util.BufferUtil;

import java.awt.*;
import java.io.IOException;

public class Ground extends GameEntity {
    private ManualObject obj;
    protected SceneNode node;

    public Ground() {
        Engine engine = EngineManager.getEngine();
        SceneManager sm = EngineManager.getSceneManager();

        String name = "Ground";
        obj = sm.createManualObject( name);
        addResponsibility(obj);
        obj.createManualSection(name + "Section");
        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        float size = 100f;

        // populate arrays
        float[] vertices = new float[] {
                -size, -0.3f, -size, // BL
                -size, -0.3f, size,  // BR
                size, -0.3f, -size,  // TL
                size, -0.3f, size    // TR
        };
        float[] normals = new float[]{
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1
        };
        float[] texcoords = new float[] {
                0, 0, // BL
                0, size * 2f, // BR
                size * 2f, 0, // TL
                size * 2f, size * 2f, // TR
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
        Material mat = engine.getMaterialManager().createManualAsset("ground-alpha" +  name);
        mat.setEmissive(Color.GREEN);
        addResponsibility(mat);
        try {
            Texture tex = engine.getTextureManager().getAssetByPath("grid.png");
            TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
            texState.setTexture(tex);
            texState.setWrapMode(TextureState.WrapMode.REPEAT);
            obj.setRenderState(texState);
        } catch (IOException e) {
            e.printStackTrace();
        }
        obj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        obj.setMaterial(mat);

        // create dummy node and attach our obj
        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);

        initPhysics();
    }

    private void initPhysics() {
        CollisionShape groundShape = new BoxShape(new javax.vecmath.Vector3f(100f, 2f, 100f));
        //CollisionShape groundShape = new StaticPlaneShape(new javax.vecmath.Vector3f(0, 1, 0), 1);

        Transform groundTransform = new Transform();
        groundTransform.setIdentity();
        groundTransform.origin.set(0, -2.3f, 0);

        float mass = 0f;
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0f, 0);

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        DefaultMotionState myMotionState = new DefaultMotionState(groundTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, groundShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        //body.setFriction(1f);

        // add the body to the dynamics world
        PhysicsManager.getWorld().addRigidBody(body);
    }
}
