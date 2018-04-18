package a2.GameEntities;

import Networking.UDPClient;
import Networking.UDPServer;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.dynamics.RigidBody;
import a2.Contollers.CharacterController;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.PlayerMotionStateController;
import myGameEngine.GameEntities.Billboard;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.TimeManager;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Camera;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;

public class Player extends GameEntity implements Attackable {
    private SceneNode cameraNode;
    private RigidBody body;
    private CharacterController controller;
    private byte playerId;
    private byte playerSide;
    private boolean local;
    public short lastReceivedTick;

    public static float height = 1.8f;
    public static float crouchHeight = 0.75f;

    public Player(byte playerId, boolean local, byte side, Vector3 location) {
        super(true);
        System.out.println("CREATE PLAYER " + playerId);
        this.playerId = playerId;
        this.local = local;
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

        if (local) {
            Camera camera = EngineManager.getSceneManager().getCamera("MainCamera");
            cameraNode.attachObject(camera);
            camera.setMode('n');
        }

        SceneNode flareNode = cameraNode.createChildSceneNode(name + "flareNode");
        flareNode.moveForward(3);
        try {
            Billboard flare = new Billboard(flareNode, 0.2f, 0.2f, "flare2.png", Color.RED);
            addResponsibility(flare);
        } catch (IOException e) {
            e.printStackTrace();
        }

        lastReceivedTick = TimeManager.getTick();

        initPhysics();
    }

    public byte getId() { return playerId; }
    public byte getSide(){
        return playerSide;
    }
    public boolean isLocal() { return local; }

    private void initPhysics() {
        createBody(false);
        controller = new CharacterController(this);
    }

    public RigidBody createBody(boolean crouching) {
        javax.vecmath.Vector3f velocity = new javax.vecmath.Vector3f();

        if (body != null) {
            body.getLinearVelocity(velocity);
            PhysicsManager.removeRigidBody(body);
            bodyResponsibility.remove(body);
        }

        float height = crouching ? crouchHeight : this.height;
        float mass = 70;
        PlayerMotionStateController motionState = new PlayerMotionStateController(this.node);
        CapsuleShape collisionShape = new CapsuleShape(0.75f, height);

        if (local) {
            body = createBody(mass, motionState, collisionShape, PhysicsManager.COL_LOCAL_PLAYER, PhysicsManager.COLLIDE_ALL);
        } else {
            body = createBody(mass, motionState, collisionShape);
        }
        body.setRestitution(0f);
        body.setFriction(0.1f);
        body.setAngularFactor(0);
        body.setDamping(0.05f, 0f);
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
        body.setLinearVelocity(velocity);

        return body;
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

    public float getPitch() { return (float)cameraNode.getLocalRotation().getPitch(); }
    public float getYaw() { return (float)node.getLocalRotation().getYaw(); }

    public void setPitch(float pitch) {
        Matrix3 cameraNodeRotation = Matrix3f.createFrom(pitch, 0, 0);
        cameraNode.setLocalRotation(cameraNodeRotation);

    }

    public void setYaw(byte controls, float yaw) {
        double wrapYawValue = CharacterController.wrapYawFromControl(controls) ? Math.PI : 0;
        Matrix3 nodeRotation = Matrix3f.createFrom(wrapYawValue, yaw, wrapYawValue);
        node.setLocalRotation(nodeRotation);
    }

    public void setPosition(Vector3 position) {
        Transform t = new Transform();
        body.getWorldTransform(t);
        t.origin.x = position.x();
        t.origin.y = position.y();
        t.origin.z = position.z();
        body.proceedToTransform(t);
        body.getMotionState().setWorldTransform(t);
    }

    public Vector3 getPosition() {
        Transform t = new Transform();
        body.getWorldTransform(t);
        return Vector3f.createFrom(t.origin);
    }

    public Vector3 getVelocity() {
        javax.vecmath.Vector3f vel = new javax.vecmath.Vector3f();
        body.getLinearVelocity(vel);
        return Vector3f.createFrom(vel);
    }

    public void setVelocity(Vector3 velocity) {
        javax.vecmath.Vector3f vel = velocity.toJavaX();
        body.setLinearVelocity(vel);
    }

    public void attacked(Vector3 aim, Vector3 relative) {
        if (local || (!UDPClient.hasClient() && !UDPServer.hasServer())) {
            javax.vecmath.Vector3f force = aim.mult(6000f).toJavaX();
            body.applyImpulse(force, relative.toJavaX());
            body.activate();
        }
        controller.knockback(null);
    }

}
