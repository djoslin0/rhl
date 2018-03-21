package myGameEngine.GameEntities;

import myGameEngine.Helpers.*;
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
import ray.rml.Vector3;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class Trail extends GameEntity {
    public ManualObject obj;
    private float[] vertices;
    private SceneNode[] trailNodes;
    private float speed;
    private float size;

    public Trail(SceneNode trailNode, Vector3 line, int segments, float speed, Color color) throws IOException {
        super(true);
        Engine engine = EngineManager.getEngine();
        SceneManager sm = EngineManager.getSceneManager();

        this.speed = speed;
        this.size = 1;

        String name = "Trail" + UniqueCounter.next();
        obj = sm.createManualObject(name);
        addResponsibility(obj);

        obj.createManualSection(name + "Section");
        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

        // build initial arrays
        FloatBuilder vertices = new FloatBuilder((segments + 1) * 2 * 3);
        FloatBuilder texcoords  = new FloatBuilder((segments + 1) * 2 * 2);
        FloatBuilder normals = new FloatBuilder((segments + 1) * 2 * 3);
        IntBuilder indices = new IntBuilder(segments * 4 * 3);

        // create the nodes that are used for trail drawing (get location)
        SceneNode trailNode2 = trailNode.createChildSceneNode(name + "Node2");
        addResponsibility(trailNode2);
        trailNode2.setLocalPosition(line);
        trailNodes = new SceneNode[] { trailNode, trailNode2};

        float midX = trailNode.getWorldPosition().x() + line.x() / 2f;
        float midY = trailNode.getWorldPosition().y() + line.y() / 2f;
        float midZ = trailNode.getWorldPosition().z() + line.z() / 2f;

        Vector3 wp = trailNodes[0].getWorldPosition();
        Vector3 wp2 = trailNodes[1].getWorldPosition();

        for (int i = 0; i < (segments + 1); i++) {
            // create initial empty vertices / normals
            /*vertices.add(midX, midY, midZ);
            vertices.add(midX, midY, midZ);*/
            vertices.add(wp.x(), wp.y(), wp.z());
            vertices.add(wp2.x(), wp2.y(), wp2.z());
            normals.add(0, 1f, 0);
            normals.add(0, 1f, 0);

            // create y-mirroring tex coordinates
            texcoords.add(0, i % 2);
            texcoords.add(1, i % 2);

            if (i < segments) {
                // create both faces for triangle 1
                indices.add(i * 2 + 0, i * 2 + 1, i * 2 + 2);
                indices.add(i * 2 + 0, i * 2 + 2, i * 2 + 1);

                // create both faces for triangle 2
                indices.add(i * 2 + 1, i * 2 + 2, i * 2 + 3);
                indices.add(i * 2 + 1, i * 2 + 3, i * 2 + 2);

            }
        }

        // Convert array builders to object
        obj.setVertexBuffer(BufferUtil.directFloatBuffer(vertices.array));
        obj.setTextureCoordBuffer(BufferUtil.directFloatBuffer(texcoords.array));
        obj.setNormalsBuffer(BufferUtil.directFloatBuffer(normals.array));
        obj.setIndexBuffer(BufferUtil.directIntBuffer(indices.array));

        // create and load trail texture
        Material mat = engine.getMaterialManager().createManualAsset("trail-alpha" +  name);
        addResponsibility(mat);
        mat.setEmissive(Color.WHITE);
        mat.setAmbient(color);
        Texture tex = engine.getTextureManager().getAssetByPath("trail.png");
        TextureState texState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        texState.setTexture(tex);
        obj.setDataSource(Renderable.DataSource.INDEX_BUFFER);
        obj.setRenderState(texState);
        obj.setMaterial(mat);

        // remember vertices in order to adjust them later
        this.vertices = vertices.array;

        // create dummy node and attach our obj
        SceneNode dummyNode = sm.getRootSceneNode().createChildSceneNode(name + "DummyNode");
        addResponsibility(dummyNode);
        dummyNode.attachObject(obj);
    }

    public void setSize(float size) {
        this.size =  size;
    }

    @Override
    public void update(float delta) {
        for (int i = 0; i < vertices.length; i+= 6) {
            double scalar = speed * delta / 200f;
            if (i + 11 >= vertices.length) {
                // snap tip of trail to the nodes
                Vector3 wp = trailNodes[0].getWorldPosition();
                Vector3 wp2 = trailNodes[1].getWorldPosition();
                vertices[i + 0] = wp.x();
                vertices[i + 1] = wp.y();
                vertices[i + 2] = wp.z();
                vertices[i + 3] = wp2.x();
                vertices[i + 4] = wp2.y();
                vertices[i + 5] = wp2.z();
            } else {
                // move trail
                vertices[i + 0] = MathHelper.lerp(vertices[i + 0], vertices[i + 6], scalar);
                vertices[i + 1] = MathHelper.lerp(vertices[i + 1], vertices[i + 7], scalar);
                vertices[i + 2] = MathHelper.lerp(vertices[i + 2], vertices[i + 8], scalar);
                vertices[i + 3] = MathHelper.lerp(vertices[i + 3], vertices[i + 9], scalar);
                vertices[i + 4] = MathHelper.lerp(vertices[i + 4], vertices[i + 10], scalar);
                vertices[i + 5] = MathHelper.lerp(vertices[i + 5], vertices[i + 11], scalar);
            }
            // taper trail
            scalar = ((1.0 - (i / (double)vertices.length)) / 10.0);
            scalar = MathHelper.lerp(0.5f, (float)scalar, size);
            float[] saved = Arrays.copyOfRange(vertices, i, i + 6);
            vertices[i + 0] = MathHelper.lerp(saved[0], saved[3], scalar);
            vertices[i + 1] = MathHelper.lerp(saved[1], saved[4], scalar);
            vertices[i + 2] = MathHelper.lerp(saved[2], saved[5], scalar);
            vertices[i + 3] = MathHelper.lerp(saved[3], saved[0], scalar);
            vertices[i + 4] = MathHelper.lerp(saved[4], saved[1], scalar);
            vertices[i + 5] = MathHelper.lerp(saved[5], saved[2], scalar);
        }

        FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
        obj.setVertexBuffer(vertBuf);
    }
}
