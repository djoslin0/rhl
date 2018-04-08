package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.dynamics.RigidBody;
import a2.Contollers.CharacterController;
import myGameEngine.Controllers.PlayerMotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Camera;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.io.IOException;

public class Player extends GameEntity {
    private SceneNode cameraNode;
    private RigidBody body;
    private CharacterController controller;
    private byte playerId;
    private byte playerSide;

    public Player(byte playerId, boolean isClient, byte side, Vector3 location) {
        super(true);
        this.playerId = playerId;
        playerSide = side;
        SceneManager sm = EngineManager.getSceneManager();
        String name = "Player" + playerId;

        // load model
        Entity entity = null;
        try {
            entity = sm.createEntity(name, "cube.obj");
            entity.setPrimitive(Renderable.Primitive.TRIANGLES);
            addResponsibility(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        if (isClient) {
            Camera camera = EngineManager.getSceneManager().getCamera("MainCamera");
            cameraNode.attachObject(camera);
            camera.setMode('n');
        }

        initPhysics();
    }

    public byte getId() { return playerId; }
    public byte getSide(){
        return playerSide;
    }

    private void initPhysics() {
        float mass = 70;
        PlayerMotionStateController motionState = new PlayerMotionStateController(this.node);
        CapsuleShape collisionShape = new CapsuleShape(0.75f, 1.8f);

        body = createBody(mass, motionState, collisionShape);
        body.setRestitution(0f);
        body.setFriction(0.1f);
        body.setAngularFactor(0);
        body.setDamping(0.05f, 0f);
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);

        controller = new CharacterController(this);
    }

    @Override
    public boolean shouldRegisterCollision() { return true; }

    public String listedName() { return "player"; }
    public SceneNode getCameraNode() { return cameraNode; }
    public RigidBody getBody() { return body; }
    public CharacterController getController() { return controller; }

    @Override
    public void update(float delta) {
        super.update(delta);
    }
}
