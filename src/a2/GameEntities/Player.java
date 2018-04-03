package a2.GameEntities;

import Networking.UDPClient;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.dynamics.RigidBody;
import myGameEngine.Controllers.CharacterController;
import myGameEngine.Controllers.PlayerMotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Camera;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.io.IOException;
import java.util.UUID;

public class Player extends GameEntity {
    private SceneNode cameraNode;
    private RigidBody body;
    private CharacterController controller;
    private Integer playerID;
    private int playerSide;

    public Player (Vector3 location,int side,Integer playerID) throws IOException{
        super(true);
        this.playerID = playerID;
        playerSide = side;
        SceneManager sm = EngineManager.getSceneManager();
        String name = "Player" + playerID;
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
        initPhysics();
    }

    public Player(Camera camera, Vector3 location,int side) throws IOException {
        super(true);
        System.out.println(location.toString());
        this.playerSide = side;
        SceneManager sm = EngineManager.getSceneManager();
        String name = "User Character" + side;

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

    public int getId() { return playerID; }
    public int getSide(){
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

        controller = new CharacterController(node, cameraNode, body);
    }

    public boolean registerCollisions() { return true; }

    public String listedName() { return "player"; }
    public SceneNode getCameraNode() { return cameraNode; }
    public RigidBody getBody() { return body; }
    public CharacterController getController() { return controller; }

    @Override
    public void update(float delta) {
        super.update(delta);
    }
}
