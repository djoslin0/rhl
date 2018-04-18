package a2.GameEntities;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.Billboard;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.GameEntities.Particle;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.TimeManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.asset.material.Material;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class Prize extends GameEntity {
    private Entity obj;

    public Prize(Vector3 location) throws IOException {
        super(true);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Gem" + unique;

        obj = sm.createEntity(name, "gem.obj");
        addResponsibility(obj);
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);

        Color color = new Color(0.1f, 0.1f, 0.9f);
        Color color2 = new Color(0.9f, 0.1f, 0.1f);
        Color color3 = new Color(0.3f, 0.1f, 0.3f);

        Material mat = sm.getMaterialManager().createManualAsset(name + "-alpha");
        addResponsibility(mat);
        mat.setEmissive(color);
        mat.setDiffuse(color2);
        mat.setAmbient(color2);
        obj.setMaterial(mat);

        Random rnd = new Random(unique * 7);

        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);
        node.setLocalPosition(location);
        float scale = 5f;
        node.setLocalScale(scale, scale, scale);

        Billboard flare = new Billboard(node, 1.5f * scale, 1.5f * scale, "flare2.png", color3);
        addResponsibility(flare);

        initPhysics();
    }

    private void initPhysics() {
        // Create Dynamic Object
        float mass = 100f;
        MotionStateController motionState = new MotionStateController(this.node);
        ConvexHullShape collisionShape = BulletConvert.entityToConvexHullShape(obj);
        collisionShape.setLocalScaling(node.getLocalScale().toJavaX());

        RigidBody body = createBody(mass, motionState, collisionShape);
        body.setRestitution(0.9f);
        body.setFriction(0.1f);
        body.setDamping(0.05f, 0.05f);
    }

    public String listedName() { return "prize"; }
    public SceneNode getNode() { return node; }

    @Override
    public void update(float delta) { }

    public void particleEffect() {
        // create collection particle effect
        Random rnd = new Random((long) TimeManager.getElapsed());
        for (int i = 0; i < 5; i++) {
            float particleSize = 0.1f + rnd.nextFloat() * 0.6f;
            Vector3 position = node.getWorldPosition();
            Vector3 velocity = Vector3f.createFrom(
                    (rnd.nextFloat() - 0.5f) * 0.004f,
                    (rnd.nextFloat() - 0.5f) * 0.004f,
                    (rnd.nextFloat() - 0.5f) * 0.004f);
            Color color = new Color(0.1f + rnd.nextFloat() * 0.4f, 0.1f + rnd.nextFloat() * 0.4f, 0.3f + rnd.nextFloat() * 0.4f);
            try {
                new Particle(particleSize, particleSize, position, velocity, "flare2.png", color, 500f + rnd.nextFloat() * 1500f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

