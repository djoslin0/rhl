package myGameEngine.GameEntities;

import myGameEngine.Helpers.*;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.TimeManager;
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
import ray.rml.Vector3f;

import java.awt.*;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class TronTrail extends GameEntity {
    public ManualObject obj;
    private float[] vertices;
    private SceneNode trailNode;
    private float speed;
    private float size;
    private Color color;
    private ArrayList<Vector3> segments = new ArrayList();

    public TronTrail(SceneNode trailNode, Color color) throws IOException {
        super(true);

        Engine engine = EngineManager.getEngine();
        SceneManager sm = EngineManager.getSceneManager();

        this.speed = speed;
        this.size = 1;
        this.trailNode = trailNode;
        this.color = color;

        String name = "Trail" + UniqueCounter.next();
        obj = sm.createManualObject(name);
        addResponsibility(obj);

        obj.createManualSection(name + "Section");
        obj.setGpuShaderProgram(sm.getRenderSystem().getGpuShaderProgram(GpuShaderProgram.Type.RENDERING));

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

        addSegment();
        addSegment();

        // create dummy node and attach our obj
        SceneNode dummyNode = sm.getRootSceneNode().createChildSceneNode(name + "DummyNode");
        addResponsibility(dummyNode);
        dummyNode.attachObject(obj);
    }

    public void addSegment() {
        segments.add(trailNode.getWorldPosition().add(0,0,0));

        int segmentCount = segments.size();
        Vector3 wp = trailNode.getWorldPosition();

        // build initial arrays
        FloatBuilder vertices = new FloatBuilder((segmentCount) * 2 * 3);
        FloatBuilder texcoords  = new FloatBuilder((segmentCount) * 2 * 2);
        FloatBuilder normals = new FloatBuilder((segmentCount) * 2 * 3);
        IntBuilder indices = new IntBuilder((segmentCount-1) * 4 * 3);

        for (int i = 0; i < (segmentCount); i++) {
            vertices.add(wp.x(), wp.y(), wp.z());
            vertices.add(wp.x(), wp.y(), wp.z());
            normals.add(0, 1f, 0);
            normals.add(0, 1f, 0);

            // create y-mirroring tex coordinates
            texcoords.add(0, i % 2);
            texcoords.add(1, i % 2);

            if (i < (segmentCount - 1)) {
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

        // remember vertices in order to adjust them later
        this.vertices = vertices.array;
    }

    @Override
    public void update(float delta) {

        float moveLength = trailNode.getWorldPosition().sub(segments.get(segments.size() - 1)).length();
        if (moveLength > 1f) {
            segments.remove(0);
            addSegment();
            moveLength = 0;
        }

        for (int i = 0; i < vertices.length; i+= 6) {
            double scalar = moveLength;
            if (i + 11 >= vertices.length) {
                // snap tip of trail to the nodes
                Vector3 wp = trailNode.getWorldPosition();
                vertices[i + 0] = wp.x();
                vertices[i + 1] = wp.y() - 0.15f;
                vertices[i + 2] = wp.z();
                vertices[i + 3] = wp.x();
                vertices[i + 4] = wp.y() + 0.25f;
                vertices[i + 5] = wp.z();
            } else {
                // move trail
                Vector3 s1 = segments.get(i / 6);
                Vector3 s2 = segments.get(i / 6 + 1);
                vertices[i + 0] = MathHelper.lerp(s1.x(), s2.x(), scalar);
                vertices[i + 1] = MathHelper.lerp(s1.y(), s2.y(), scalar);
                vertices[i + 2] = MathHelper.lerp(s1.z(), s2.z(), scalar);
                vertices[i + 3] = MathHelper.lerp(s1.x(), s2.x(), scalar);
                vertices[i + 4] = MathHelper.lerp(s1.y() + 0.5f, s2.y() + 0.5f, scalar);
                vertices[i + 5] = MathHelper.lerp(s1.z(), s2.z(), scalar);
            }
        }

        FloatBuffer vertBuf = BufferUtil.directFloatBuffer(vertices);
        obj.setVertexBuffer(vertBuf);
    }

    @Override
    public void destroy() {
        // create death  particle effect
        Random rnd = new Random((long) TimeManager.getElapsed());
        for (Vector3 segment : segments) {
            float particleSize = 0.2f + rnd.nextFloat() * 2f;
            Vector3 position = segment;
            Vector3 velocity = Vector3f.createFrom(
                    (rnd.nextFloat() - 0.5f) * 0.004f,
                    (rnd.nextFloat() - 0.5f) * 0.004f,
                    (rnd.nextFloat() - 0.5f) * 0.004f);
            try {
                new Particle(particleSize, particleSize, position, velocity, "flare2.png", color, 500f + rnd.nextFloat() * 1500f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.destroy();
    }

    public boolean intersect(Vector3 posA, Vector3 posB, boolean skipLast) {
        Line2D line1 = new Line2D.Float(posA.x(), posA.z(), posB.x(), posB.z());

        for (int i = 0; i < vertices.length; i+= 6) {
            if (skipLast && i + 11 + 6 + 6 >= vertices.length) { break; }
            if (i + 11 >= vertices.length) {
                Vector3 wp = trailNode.getWorldPosition();
                Vector3 sp = segments.get(segments.size() - 1);
                Line2D line2 = new Line2D.Float(wp.x(), wp.z(), sp.x(), sp.z());
                if (line1.intersectsLine(line2)) { return true; }
            } else {
                Line2D line2 = new Line2D.Float(vertices[i + 0], vertices[i + 2], vertices[i + 6], vertices[i + 8]);
                if (line1.intersectsLine(line2)) { return true; }
            }
        }

        return false;
    }
}
