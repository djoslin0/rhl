package myGameEngine.GameEntities;

import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UniqueCounter;
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

// The WorldAxes is an entity that displays a line for the X, Y, and Z axes
public class WorldAxes extends GameEntity {
    public WorldAxes() {
        // Create each axis
        CreateAxis("X", Color.RED, new float[] {
                0, 0, 0,
                1, 0, 0
        });
        CreateAxis("Y", Color.GREEN, new float[] {
                0, 0, 0,
                0, 1, 0
        });
        CreateAxis("Z", Color.BLUE, new float[] {
                0, 0, 0,
                0, 0, 1
        });
    }

    private void CreateAxis(String name, Color color, float[] vertices) {
        SceneManager sm = EngineManager.getSceneManager();

        // setup side manual object
        ManualObject obj = sm.createManualObject("WorldAxis" + name);
        addResponsibility(obj);
        obj.createManualSection("WorldAxisSection" + name);

        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
        float[] normals = new float[] {
                0, 1, 0,
                0, 1, 0
        };
        float[] texcoords = new float[] {
                0, 0,
                0, 1
        };
        int[] indices = new int[] {
                0, 1
        };

        // Convert array builders to object
        obj.setVertexBuffer(BufferUtil.directFloatBuffer(vertices));
        obj.setTextureCoordBuffer(BufferUtil.directFloatBuffer(texcoords));
        obj.setNormalsBuffer(BufferUtil.directFloatBuffer(normals));
        obj.setIndexBuffer(BufferUtil.directIntBuffer(indices));
        obj.setPrimitive(Renderable.Primitive.LINES);

        // apply color material
        Material mat = sm.getMaterialManager().createManualAsset("WorldAxisMat" + name);
        addResponsibility(mat);
        mat.setEmissive(color);
        Texture tex = null;
        try {
            tex = sm.getTextureManager().getAssetByPath("default.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextureState tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        tstate.setTexture(tex);
        obj.setRenderState(tstate);
        obj.setMaterial(mat);

        // attach
        sm.getRootSceneNode().attachObject(obj);
    }
}
