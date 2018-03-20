package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.OrbitCameraController;
import myGameEngine.Controllers.PlayerMotionStateController;
import myGameEngine.Controllers.CharacterController;
import myGameEngine.GameEntities.GameEntityUpdatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Camera;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;

public class Player extends GameEntityUpdatable {
    private SceneNode node;
    private SceneNode cameraNode;
    private RigidBody body;
    private CharacterController controller;

    public Player(Camera camera, Vector3 location) throws IOException {
        super();
        SceneManager sm = EngineManager.getSceneManager();
        String name = "Player" + UniqueCounter.next();

        // load model
        Entity entity = sm.createEntity(name, "cube.obj");
        entity.setPrimitive(Renderable.Primitive.TRIANGLES);
        addResponsibility(entity);

        // create entity's base node, and move to desired location
        node = sm.getRootSceneNode().createChildSceneNode(name + "Node");
        node.setLocalPosition(location);
        addResponsibility(node);

        SceneNode objNode = node.createChildSceneNode(name + "ObjNode");
        objNode.attachObject(entity);
        objNode.setLocalScale(0.75f, 1.8f, 0.75f);
        addResponsibility(objNode);

        // create entity's camera node
        cameraNode = node.createChildSceneNode(name + "CameraNode");
        cameraNode.setLocalPosition(0, 1.5f, 0);
        addResponsibility(cameraNode);
        cameraNode.attachObject(camera);
        camera.setMode('n');

        initPhysics();
    }

    private void initPhysics() {
        // NOTE: Re-using the same collision is better for memory usage and performance
        CapsuleShape colShape = new CapsuleShape(0.75f, 1.8f);

        // Create Dynamic Object
        float mass = 70;

        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0, 0);
        colShape.calculateLocalInertia(mass, localInertia);

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        PlayerMotionStateController myMotionState = new PlayerMotionStateController(this.node);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape, localInertia);
        body = new RigidBody(rbInfo);
        body.setRestitution(0f);
        body.setFriction(0.3f);
        body.setAngularFactor(0);
        body.setDamping(0.05f, 0f);

        PhysicsManager.getWorld().addRigidBody(body);

        controller = new CharacterController(node, cameraNode, body);
    }

    public String listedName() { return "player"; }

    public SceneNode getNode() { return node; }
    public SceneNode getCameraNode() { return cameraNode; }
    public RigidBody getBody() { return body; }
    public CharacterController getController() { return controller; }

    @Override
    public void update(float delta) {
        super.update(delta);
    }
}
