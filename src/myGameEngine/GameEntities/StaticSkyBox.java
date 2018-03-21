package myGameEngine.GameEntities;

import myGameEngine.Singletons.EngineManager;
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

public class StaticSkyBox extends GameEntity {
    public StaticSkyBox(SceneNode parentNode) throws IOException {
        super(false);
        // create sides
        float size = 500f;
        CreateSide(parentNode,"front",
                new float[] {
                        -size, -size, -size, // BL
                        -size, size, -size,  // BR
                        size, -size, -size,  // TL
                        size, size, -size    // TR
                },
                new float[] {
                        0, 0, 1,
                        0, 0, 1,
                        0, 0, 1,
                        0, 0, 1
                });

        CreateSide(parentNode,"back",
                new float[] {
                        size, -size, size,   // BL
                        size, size, size,    // BR
                        -size, -size, size,  // TL
                        -size, size, size    // TR
                },
                new float[] {
                        0, 0, -1,
                        0, 0, -1,
                        0, 0, -1,
                        0, 0, -1
                });

        CreateSide(parentNode,"right",
                new float[] {
                        -size, -size, size,   // BL
                        -size, size, size,    // BR
                        -size, -size, -size,  // TL
                        -size, size, -size    // TR
                },
                new float[] {
                        1, 0, 0,
                        1, 0, 0,
                        1, 0, 0,
                        1, 0, 0
                });

        CreateSide(parentNode,"left",
                new float[] {
                        size, -size, -size, // BL
                        size, size, -size,  // BR
                        size, -size, size,  // TL
                        size, size, size    // TR
                },
                new float[] {
                        -1, 0, 0,
                        -1, 0, 0,
                        -1, 0, 0,
                        -1, 0, 0
                });

        CreateSide(parentNode,"top",
                new float[] {
                        -size, size, -size, // BL
                        -size, size, size,  // BR
                        size, size, -size,  // TL
                        size, size, size    // TR
                },
                new float[] {
                        0, -1, 0,
                        0, -1, 0,
                        0, -1, 0,
                        0, -1, 0
                });

        CreateSide(parentNode,"bottom",
                new float[] {
                        size, -size, -size,   // BL
                        size, -size, size,    // BR
                        -size, -size, -size,  // TL
                        -size, -size, size    // TR
                },
                new float[] {
                        0, 1, 0,
                        0, 1, 0,
                        0, 1, 0,
                        0, 1, 0
                });
    }

    private void CreateSide(SceneNode parentNode, String side, float[] vertices, float[] normals) throws IOException {
        Engine engine = EngineManager.getEngine();
        SceneManager sm = EngineManager.getSceneManager();

        // setup side manual object
        ManualObject obj = sm.createManualObject("SkyBox" + side);
        addResponsibility(obj);
        obj.createManualSection("SkyBoxSection" + side);
        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));
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

        // create and load trail texture
        Material mat = engine.getMaterialManager().createManualAsset("SkyBoxMat" + side);
        addResponsibility(mat);
        mat.setEmissive(Color.WHITE);
        Texture tex = engine.getTextureManager().getAssetByPath(side + ".png");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);

        // finish manual object setup
        obj.setMaterial(mat);
        obj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        obj.setRenderState(texState);

        // attach
        parentNode.attachObject(obj);
    }

}
