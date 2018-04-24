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
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.*;
import ray.rml.*;

import java.awt.*;
import java.io.IOException;

public class Player extends GameEntity implements Attackable {
    private SceneNode cameraNode;
    private SceneNode handNode;
    private SceneNode headNode;
    private RigidBody body;
    private CharacterController controller;
    private Glove glove;
    private byte playerId;
    private byte playerSide;
    private boolean local;
    public short lastReceivedTick;


    private SkeletalEntity robo;

    public static float height = 1.6f;
    public static float crouchHeight = 0.75f;
    public static float cameraHeight = 1.2f;
    public static float cameraCrouchHeight = -0.45f;


    public Player(byte playerId, boolean local, byte side, Vector3 location) {
        super(true);
        System.out.println("CREATE PLAYER " + playerId);
        this.playerId = playerId;
        this.local = local;
        playerSide = side;
        SceneManager sm = EngineManager.getSceneManager();
        String name = "Player" + playerId;

        // create entity's base node, and move to desired location
        node = sm.getRootSceneNode().createChildSceneNode(name + "Node");
        node.setLocalPosition(location);
        addResponsibility(node);

        // create entity's camera node
        cameraNode = node.createChildSceneNode(name + "CameraNode");
        cameraNode.setLocalPosition(0, 1.5f, 0);
        addResponsibility(cameraNode);

        // attach camera
        if (local) {
            Camera camera = EngineManager.getSceneManager().getCamera("MainCamera");
            cameraNode.attachObject(camera);
            camera.setMode('n');
        }

        // create aiming flare
        SceneNode flareNode = cameraNode.createChildSceneNode(name + "flareNode");
        flareNode.moveForward(3);
        try {
            Billboard flare = new Billboard(flareNode, 0.2f, 0.2f, "flare2.png", Color.RED);
            addResponsibility(flare);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load character
        if (!local) { loadCharacter(name); }

        // store tick
        lastReceivedTick = TimeManager.getTick();

        // initialize physics
        initPhysics();
    }

    private void loadCharacter(String name) {
        try {
            SceneManager sm = EngineManager.getSceneManager();

            // load skeletal model
            robo = sm.createSkeletalEntity("robo" + name, "robo.rkm", "robo.rks");

            // load animations
            robo.loadAnimation("idle", "idle.rka");
            robo.loadAnimation("run", "run.rka");
            robo.loadAnimation("sidestep", "sidestep.rka");
            robo.loadAnimation("jump", "jump.rka");
            robo.loadAnimation("falling", "falling.rka");
            robo.loadAnimation("land", "land.rka");
            robo.loadAnimation("crouch_idle", "crouch_idle.rka");
            robo.loadAnimation("crouch_walk", "crouch_walk.rka");
            robo.loadAnimation("crouch_sidestep", "crouch_sidestep.rka");

            // load texture
            Texture texture = sm.getTextureManager().getAssetByPath("robo_uv.png");
            TextureState textureState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
            textureState.setTexture(texture);
            robo.setRenderState(textureState);

            // setup character node
            SceneNode roboNode = node.createChildSceneNode(name + "RoboNode");
            roboNode.setLocalPosition(0, -height, 0);
            addResponsibility(roboNode);
            roboNode.attachObject(robo);

            // track right hand
            handNode = roboNode.createChildSceneNode(name + "HandNode");
            handNode.setLocalScale(100f, 100f, 100f);

            // attach glove to hand
            glove = new Glove(this, name, handNode);

            // hide normal glove
            robo.addScaleOverride("hand_R", Vector3f.createFrom(0.01f, 0.01f, 0.01f));

            // track head for glowing eyes
            headNode = roboNode.createChildSceneNode(name + "HeadNode");

            // create eyes
            float eyeSpacing = 0.225f;
            float distFromHead = 0.74f;
            float eyeHeight = 0.6f;
            float eyeSize = 0.2f;

            // left eye
            SceneNode leftEyeNode = headNode.createChildSceneNode(name + "EyeLNode");
            leftEyeNode.setLocalPosition(eyeSpacing, eyeHeight, distFromHead);
            Billboard leftEyeFlare = new Billboard(leftEyeNode, eyeSize, eyeSize, "flare1.png", Color.YELLOW);
            addResponsibility(leftEyeFlare);

            // right eye
            SceneNode rightEyeNode = headNode.createChildSceneNode(name + "EyeRNode");
            rightEyeNode.setLocalPosition(-eyeSpacing, eyeHeight, distFromHead);
            Billboard rightEyeFlare = new Billboard(rightEyeNode, eyeSize, eyeSize, "flare1.png", Color.YELLOW);
            addResponsibility(rightEyeFlare);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initPhysics() {
        createBody(false);
        controller = new CharacterController(this);
    }

    public RigidBody createBody(boolean crouching) {
        javax.vecmath.Vector3f velocity = new javax.vecmath.Vector3f();
        if (body != null) {
            // we're replacing our previous body, remember velocity
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
    public SkeletalEntity getRobo() { return robo; }
    public byte getId() { return playerId; }
    public byte getSide(){ return playerSide; }
    public boolean isLocal() { return local; }
    public Glove getGlove() { return glove; }

    public float getPitch() { return (float)cameraNode.getLocalRotation().getPitch(); }

    public void setPitch(float pitch) {
        Matrix3 cameraNodeRotation = Matrix3f.createFrom(pitch, 0, 0);
        cameraNode.setLocalRotation(cameraNodeRotation);

    }

    public float getYaw() { return (float)node.getLocalRotation().getYaw(); }

    public void setYaw(byte controls, float yaw) {
        double wrapYawValue = CharacterController.wrapYawFromControl(controls) ? Math.PI : 0;
        Matrix3 nodeRotation = Matrix3f.createFrom(wrapYawValue, yaw, wrapYawValue);
        node.setLocalRotation(nodeRotation);
    }

    public Vector3 getPosition() {
        Transform t = new Transform();
        body.getWorldTransform(t);
        return Vector3f.createFrom(t.origin);
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
        controller.knockback(aim.mult(2500f));
    }

    public void animate(String animName, float animSpeed, SkeletalEntity.EndType endType, boolean interruptable) {
        if (robo == null) { return; }
        robo.playAnimation(animName, animSpeed, endType, 0, interruptable);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (robo != null) {
            // update animations
            robo.update(delta);

            // update tracked hand
            Matrix4 handTransform = robo.getBoneModelTransform("hand_R");
            handNode.setLocalPosition(handTransform.column(3).toVector3());
            handNode.setLocalRotation(handTransform.toMatrix3());

            // update tracked head
            Matrix4 headTransform = robo.getBoneModelTransform("head");
            headNode.setLocalPosition(headTransform.column(3).toVector3());
            headNode.setLocalRotation(headTransform.toMatrix3());
        }
    }

}
