package a2.GameEntities;

import Networking.UDPClient;
import a2.Actions.ActionRotate;
import a2.Contollers.CharacterAnimationController;
import a2.Contollers.LocalCharacterAnimationController;
import a2.Contollers.RemoteCharacterAnimationController;
import a2.MyGame;
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
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.TimeManager;
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.*;
import ray.rml.*;

import javax.vecmath.Quat4f;
import java.awt.*;
import java.io.IOException;

public class Player extends GameEntity implements Attackable {
    private SceneNode cameraNode;
    private SceneNode handNode;
    private SceneNode headNode;
    private SceneNode leftEyeNode;
    private SceneNode rightEyeNode;
    private SceneNode roboNode;
    private SceneNode crosshairNode;
    private RigidBody body;
    private CharacterController controller;
    private CharacterAnimationController animationController;
    private SkeletalEntity robo;
    private Glove glove;
    private byte playerId;
    private byte playerSide;
    private boolean local;
    public short lastReceivedTick;
    private int health;
    private float absorbHurt;
    private Debris headDebris;
    private float respawnTimeout;
    private boolean dead;

    public static float height = 1.8f;
    public static float crouchHeight = 0.75f;
    public static float cameraHeight = 1.2f;
    public static float cameraCrouchHeight = -0.45f;
    public static float absorbHurtFalloff = 0.015f;
    public static float respawnSeconds = 5f;

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
        crosshairNode = cameraNode.createChildSceneNode(name + "crosshairNode");
        crosshairNode.moveForward(3);
        try {
            Billboard flare = new Billboard(crosshairNode, 0.2f, 0.2f, "flare2.png", Color.RED);
            addResponsibility(flare);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // store tick
        lastReceivedTick = TimeManager.getTick();

        // initialize physics
        initPhysics();

        // load character
        if (local) {
            loadLocalCharacter(name);
        } else {
            loadRemoteCharacter(name);
        }

        // attach glove to hands
        glove = new Glove(this, name, handNode);

        health = 100;
        absorbHurt = 0;
    }

    private void loadLocalCharacter(String name) {
        // right hand
        handNode = cameraNode.createChildSceneNode(name + "HandNode");
        handNode.setLocalPosition(0.2f, -0.1f, 0f);
        handNode.pitch(Degreef.createFrom(70f));

        // animation controller
        animationController = new LocalCharacterAnimationController(this, controller);
    }

    private void loadRemoteCharacter(String name) {
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
            roboNode = node.createChildSceneNode(name + "RoboNode");
            roboNode.setLocalPosition(0, -height, -0.3f);
            addResponsibility(roboNode);
            roboNode.attachObject(robo);

            // track right hand
            handNode = roboNode.createChildSceneNode(name + "HandNode");

            // track head for glowing eyes
            headNode = roboNode.createChildSceneNode(name + "HeadNode");

            // create eyes
            float eyeSpacing = 0.225f;
            float distFromHead = 0.74f;
            float eyeHeight = 0.6f;
            float eyeSize = 0.2f;

            // left eye
            leftEyeNode = headNode.createChildSceneNode(name + "EyeLNode");
            leftEyeNode.setLocalPosition(eyeSpacing, eyeHeight, distFromHead);
            Billboard leftEyeFlare = new Billboard(leftEyeNode, eyeSize, eyeSize, "flare1.png", Color.YELLOW);
            addResponsibility(leftEyeFlare);

            // right eye
            rightEyeNode = headNode.createChildSceneNode(name + "EyeRNode");
            rightEyeNode.setLocalPosition(-eyeSpacing, eyeHeight, distFromHead);
            Billboard rightEyeFlare = new Billboard(rightEyeNode, eyeSize, eyeSize, "flare1.png", Color.YELLOW);
            addResponsibility(rightEyeFlare);

            // animation controller
            animationController = new RemoteCharacterAnimationController(this, controller);
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

        PlayerMotionStateController motionState = new PlayerMotionStateController(this, this.node);
        CapsuleShape collisionShape = new CapsuleShape(0.85f, height);

        if (local) {
            body = createBody(mass, motionState, collisionShape, PhysicsManager.COL_LOCAL_PLAYER, PhysicsManager.COLLIDE_DEFAULT);
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
    public CharacterAnimationController getAnimationController() { return animationController; }
    public SceneNode getHandNode() { return handNode; }
    public boolean isDead() { return dead; }

    public float getPitch() { return (float)cameraNode.getLocalRotation().getPitch(); }

    public void setPitch(double pitch) {
        Matrix3 cameraNodeRotation = Matrix3f.createFrom(pitch, 0, 0);
        cameraNode.setLocalRotation(cameraNodeRotation);

    }

    public float getYaw() {
        double yaw = node.getLocalRotation().getYaw();
        if (node.getWorldRotation().getRoll() != 0) {
            if (yaw > 0) {
                yaw = Math.PI - yaw;
            } else {
                yaw = -Math.PI - yaw;
            }
        }
        return (float)yaw;
    }

    public void setYaw(double yaw) {
        double wrapYawValue = 0;
        if (Math.abs(yaw) > Math.PI / 2f) {
            if (yaw > 0) {
                yaw = Math.PI - yaw;
            } else {
                yaw = -Math.PI - yaw;
            }
            wrapYawValue = Math.PI;
        }
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
        float dot = getVelocity().dot(aim.normalize()) * 1.5f;
        float rally = 0;
        if (dot < 0) {
            rally = -dot;
            if (rally > 25f) { rally = 25f; }
        }

        controller.knockback(aim.mult(3500f + 100f * rally), relative);
    }

    public void lookAt(Vector3 target) {
        try {
            Matrix3 m = Matrix4f.createLookAtMatrix(cameraNode.getWorldPosition(), target, Vector3f.createUnitVectorY()).toMatrix3();
            if (m.getRoll() > 0) {
                setPitch(Math.PI + m.getPitch());
                setYaw(Math.PI - m.getYaw());
            } else {
                setPitch(m.getPitch());
                setYaw(m.getYaw());
            }
        } catch (ArithmeticException ex) { }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (dead) {
            lookAt(headDebris.getNode().getWorldPosition());
            if (respawnTimeout > 0) {
                respawnTimeout -= delta;
                if (respawnTimeout <= 0) {
                    respawn();
                }
            }
            return;
        }

        if (absorbHurt > 0) {
            absorbHurt -= absorbHurtFalloff * delta;
            if (absorbHurt < 0) { absorbHurt = 0; }
        }

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

    public void hurt(int value) {
        if (UDPClient.hasClient()) { return; }
        if (value < absorbHurt) { return; }
        int applyHurt = (int)(value - absorbHurt);
        absorbHurt = value;
        health -= applyHurt;
        if (health <= 0) {
            health = 0;
            die();
        }
        if (local) {
            MyGame.healthText.text = "Health: " + health;
        }
    }

    public void die() {
        if (dead) { return; }
        respawnTimeout = respawnSeconds * 1000f;
        dead = true;

        health = 0;
        if (local) {
            MyGame.healthText.text = "Health: " + health;
        }
        absorbHurt = 0;

        headDebris = createDebrisPart("head");
        createDebrisPart("torso");
        createDebrisPart("arm_upper_L");
        createDebrisPart("arm_upper_R");
        createDebrisPart("leg_upper_L");
        createDebrisPart("leg_upper_R");

        crosshairNode.setLocalScale(0.001f, 0.001f, 0.001f);
        PhysicsManager.removeRigidBody(body);

        if (roboNode != null) {
            node.detachChild(roboNode);
            leftEyeNode.setLocalScale(0.001f, 0.001f, 0.001f);
            rightEyeNode.setLocalScale(0.001f, 0.001f, 0.001f);
        }
    }

    public void respawn() {
        dead = false;
        respawnTimeout = 0;
        health = 100;
        absorbHurt = 0;
        setPosition(Settings.get().spawnPoint);
        setVelocity(Vector3f.createZeroVector());

        crosshairNode.setLocalScale(1f, 1f, 1f);
        PhysicsManager.addRigidBody(body);

        if (roboNode != null) {
            node.attachChild(roboNode);
            leftEyeNode.setLocalScale(1f, 1f, 1f);
            rightEyeNode.setLocalScale(1f, 1f, 1f);
        }
    }

    private Debris createDebrisPart(String boneName) {
        try {
            if (local) {
                return new Debris(node.getWorldPosition(), cameraNode.getWorldRotation().toQuaternion(), getVelocity(), boneName + ".obj");
            } else {
                Matrix4 matrix = roboNode.getWorldTransform().mult(robo.getBoneModelTransform(boneName));
                Vector3 location = matrix.column(3).toVector3();
                Quaternion rotation = matrix.toQuaternion();
                return new Debris(location, rotation, getVelocity(), boneName + ".obj");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
