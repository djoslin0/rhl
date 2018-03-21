package a2.GameEntities;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import myGameEngine.Controllers.CollectController;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.Billboard;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.GameEntities.Particle;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
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
    private SceneNode node;
    private Entity obj;
    private CollectController collectController;

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

        collectController = new CollectController();
        collectController.addNode(node);
        sm.addController(collectController);

        initPhysics();
    }

    private void initPhysics() {
        // NOTE: Re-using the same collision is better for memory usage and performance
        ConvexHullShape colShape = BulletConvert.entityToConvexHullShape(obj);
        colShape.setLocalScaling(node.getLocalScale().toJavaX());

        // Create Dynamic Object
        float mass = 100f;

        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0, 0);
        colShape.calculateLocalInertia(mass, localInertia);

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        MotionStateController myMotionState = new MotionStateController(this.node);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setRestitution(0.9f);
        body.setFriction(0.1f);
        body.setDamping(0.05f, 0.05f);

        body.setUserPointer(this);
        PhysicsManager.getWorld().addRigidBody(body);
    }

    public String listedName() { return "prize"; }
    public SceneNode getNode() { return node; }
    public CollectController getCollectController() { return collectController; }

    @Override
    public void update(float delta) {
        if (getCollectController().isCollected()) {
            Random rnd = new Random((long) TimeManager.getElapsed());
            Vector3 pLocation = Vector3f.createFrom((rnd.nextFloat() - 0.5f) * 28f, 0f, (rnd.nextFloat() - 0.5f) * 28f);
            node.setLocalPosition(pLocation);
            getCollectController().respawn();
            particleEffect();
        }
    }

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

