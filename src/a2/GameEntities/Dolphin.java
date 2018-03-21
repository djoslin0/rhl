package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.RigidBody;
import myGameEngine.Controllers.NodeController;
import myGameEngine.Controllers.OrbitCameraController;
import myGameEngine.GameEntities.*;
import myGameEngine.Helpers.HudText;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.TimeManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Camera;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class Dolphin extends GameEntity {
    private TronTrail tron;
    private OrbitCameraController cameraController;
    private NodeController nodeController;
    private int score;
    private boolean falling = false;
    private float fallVelocity = 0f;
    private Color color;
    private HudText hud;
    private Camera camera;
    private boolean winning = false;
    private Vector3 lastWorldPosition;

    public Dolphin(Camera camera, Vector3 location, Color color, String objName, HudText hud) throws IOException {
        super(true);
        SceneManager sm = EngineManager.getSceneManager();
        String name = "Dolphin" + UniqueCounter.next();
        this.color = color;
        this.hud = hud;
        this.camera = camera;

        // load dolphin model
        Entity entity = sm.createEntity(name, objName);
        addResponsibility(entity);
        entity.setPrimitive(Renderable.Primitive.TRIANGLES);

        // create entity's base node, and move to desired location
        node = sm.getRootSceneNode().createChildSceneNode(entity.getName() + "Node");
        addResponsibility(node);
        node.attachObject(entity);
        node.setLocalPosition(location);

        // allow the dolphin to be easily moved around
        nodeController = new NodeController(node);

        // create a trail for each flipper
        SceneNode flipper1 = node.createChildSceneNode(name + "Flipper1Node");
        addResponsibility(flipper1);
        flipper1.setLocalPosition(Vector3f.createFrom(0.1f, -0.09f, 0.4f));
        addResponsibility(new Trail(flipper1, Vector3f.createFrom(0.07f, -0.05f ,-0.07f),5, 1, Color.WHITE));

        SceneNode flipper2 = node.createChildSceneNode(name + "Flipper2Node");
        addResponsibility(flipper2);
        flipper2.setLocalPosition(Vector3f.createFrom(-0.1f, -0.09f, 0.4f));
        addResponsibility(new Trail(flipper2, Vector3f.createFrom(-0.07f, -0.05f ,-0.07f), 5, 1, Color.WHITE));

        tron = new TronTrail(node, color);
        addResponsibility(tron);

        cameraController = new OrbitCameraController(camera, this.node);

        lastWorldPosition = node.getWorldPosition();
    }

    public String listedName() { return "dolphin"; }

    private void checkPrizes() {
        //if (dead) { return; }
        // check to see if we collided with any prizes
        Vector3 toPosition = node.getWorldPosition().add(node.getWorldForwardAxis());
        javax.vecmath.Vector3f from = node.getWorldPosition().toJavaX();
        javax.vecmath.Vector3f to = toPosition.toJavaX();
        CollisionWorld.ClosestRayResultCallback closest = new CollisionWorld.ClosestRayResultCallback(from, to);
        PhysicsManager.getWorld().rayTest(from, to, closest);
        if (closest.hasHit()) {
            if (closest.collisionObject instanceof RigidBody) {
                RigidBody rb = (RigidBody) closest.collisionObject;
                javax.vecmath.Vector3f force = node.getWorldForwardAxis().mult(100f).toJavaX();
                rb.activate();
                rb.applyCentralForce(force);
            }
        }
    }

    private void showWinLose(boolean won) {
        try {
            SceneNode noteNode = node.createChildSceneNode("DeathNode" + UniqueCounter.next());
            noteNode.setLocalPosition(Vector3f.createFrom(0,0.55f,0));
            new Billboard(noteNode, 1f, 1f, won ? "won.png" : "lost.png", Color.white, camera);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        // create death  particle effect
        Random rnd = new Random((long) TimeManager.getElapsed());
        for (int i = 0; i < 5; i++) {
            float particleSize = 0.2f + rnd.nextFloat() * 2f;
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

        if (!winning) { showWinLose(false); }

        super.destroy();

        //EngineManager.setGameActive(false);
    }

    public SceneNode getNode() { return node; }
    public NodeController getNodeController() { return nodeController; }
    public OrbitCameraController getCameraController() { return cameraController; }
    public TronTrail getTron() { return tron; }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (isDestroyed()) { return; }
        if (!EngineManager.isGameActive()) { return; }

        checkPrizes();
        cameraController.updateCameraPosition();
        lastWorldPosition = node.getWorldPosition().add(0,0,0);
    }
}

