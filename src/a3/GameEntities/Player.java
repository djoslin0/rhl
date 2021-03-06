package a3.GameEntities;

import Networking.UDPClient;
import Networking.UDPServer;
import a3.Contollers.*;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.PlayerMotionStateController;
import myGameEngine.GameEntities.Billboard;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.GameEntities.LightFade;
import myGameEngine.GameEntities.Particle;
import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.*;
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
    private SceneNode leftEyeNode;
    private SceneNode rightEyeNode;
    private SceneNode roboNode;

    private RigidBody body;

    private CharacterController controller;
    private CharacterAnimationController animationController;
    private HudController hudController;

    private SkeletalEntity robo;
    private Entity headObj;
    private Glove glove;

    private TextureState textureState;
    private TextureState headState;

    private byte playerId;
    private byte headId;
    private Player.Team playerSide;
    private boolean local;
    public short lastReceivedTick;
    private byte health;
    private float absorbHurt;
    private float invulnerability;
    private Debris headDebris;
    private float respawnTimeout;
    private boolean dead;

    public static float height = 1.8f;
    public static float crouchHeight = 0.75f;
    public static float cameraHeight = 1.2f;
    public static float cameraCrouchHeight = -0.45f;
    public static float absorbHurtFalloff = 0.015f;
    public static float respawnSeconds = 5f;

    private float nextStepTime;
    private float hurtSoundTimeout;
    private SoundGroup stepSound;
    private SoundGroup deathSound;
    private SoundGroup nearDeathSound;
    private SoundGroup respawnSound;
    private SoundGroup glitchSound;
    private SoundGroup jumpSound;
    private SoundGroup landSound;

    public long lastMessageReceived = java.lang.System.currentTimeMillis();

    // side definition
    public static enum Team{ Orange, Blue }

    public Player(byte playerId, boolean local, Player.Team side, byte headId) {
        super(true);
        System.out.println("CREATE PLAYER " + playerId);
        this.playerId = playerId;
        this.local = local;
        this.playerSide = side;
        this.headId = headId;

        SceneManager sm = EngineManager.getSceneManager();
        String name = "Player" + playerId;

        // create entity's base node, and move to desired location
        node = sm.getRootSceneNode().createChildSceneNode(name + "Node");
        node.setLocalPosition(getSpawnPosition());
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
            EntityManager.setLocalPlayer(this);
        }

        // store tick
        lastReceivedTick = TimeManager.getTick();

        // initialize physics
        initPhysics();

        // load texture
        try {
            String textureName = (side == Team.Orange) ? "robo_orange.png" : "robo_blue.png";
            Texture texture = sm.getTextureManager().getAssetByPath(textureName);
            textureState = (TextureState)sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
            textureState.setTexture(texture);

            if (headId == 2) {
                String headTextureName = (side == Team.Orange) ? "head2_orange.png" : "head2_blue.png";
                Texture headTexture = sm.getTextureManager().getAssetByPath(headTextureName);
                headState = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
                headState.setTexture(headTexture);
            } else {
                headState = textureState;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load character
        if (local) {
            loadLocalCharacter(name);
        } else {
            loadRemoteCharacter(name);
        }

        // attach glove to hands
        glove = new Glove(this, name, textureState, handNode);
        addResponsibility(glove);

        // create hud
        if (local) {
            hudController = HudController.get(this);
            AudioManager.setEar(cameraNode);
        }

        // sounds
        stepSound = AudioManager.get().step.clone(node);
        addResponsibility(stepSound);
        deathSound = AudioManager.get().death.clone(node);
        addResponsibility(deathSound);
        nearDeathSound = AudioManager.get().nearDeath.clone(node);
        addResponsibility(nearDeathSound);
        respawnSound = AudioManager.get().respawn.clone(node);
        addResponsibility(respawnSound);
        glitchSound = AudioManager.get().glitch.clone(node);
        addResponsibility(glitchSound);
        jumpSound = AudioManager.get().jump.clone(node);
        addResponsibility(jumpSound);
        landSound = AudioManager.get().land.clone(node);
        addResponsibility(landSound);

        health = 100;
        absorbHurt = 0;
    }

    public byte getHeadId() { return headId; }
    public void playJumpSound() { jumpSound.play(); }
    public void playLandSound() { landSound.play(); }

    private void loadLocalCharacter(String name) {
        // right hand
        handNode = cameraNode.createChildSceneNode(name + "HandNode");
        handNode.setLocalPosition(0.2f, -0.1f, 0f);
        handNode.pitch(Degreef.createFrom(70f));
        addResponsibility(handNode);

        // animation controller
        animationController = new LocalCharacterAnimationController(this, controller);
        addResponsibility((Updatable)animationController);
    }

    private void loadRemoteCharacter(String name) {
        try {
            SceneManager sm = EngineManager.getSceneManager();

            // load skeletal model
            robo = sm.createSkeletalEntity("robo" + name, "robo.rkm", "robo.rks");
            addResponsibility(robo);

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
            robo.setRenderState(textureState);

            // setup character node
            roboNode = node.createChildSceneNode(name + "RoboNode");
            roboNode.setLocalPosition(0, -height, -0.3f);
            addResponsibility(roboNode);
            roboNode.attachObject(robo);

            // track right hand
            handNode = roboNode.createChildSceneNode(name + "HandNode");
            addResponsibility(handNode);

            // track head for glowing eyes
            headNode = roboNode.createChildSceneNode(name + "HeadNode");
            addResponsibility(headNode);

            // create head
            headObj = sm.createEntity(name + "Head", (headId == 2) ? "head2.obj" : "head.obj");
            addResponsibility(headObj);
            headObj.setPrimitive(Renderable.Primitive.TRIANGLES);
            headObj.setRenderState(headState);
            headNode.attachObject(headObj);

            // create eyes
            float eyeSpacing = 0.225f;
            float distFromHead = 0.74f;
            float eyeHeight = 0.6f;
            float eyeSize = 0.2f;

            Color eyeColor = (playerSide == Team.Orange) ? Color.YELLOW : new Color(200, 150, 255);

            if (headId == 2) {
                eyeSpacing = 0;
                eyeHeight = 0.55f;
                distFromHead = 0.73f;
            }

            // left eye
            leftEyeNode = headNode.createChildSceneNode(name + "EyeLNode");
            addResponsibility(leftEyeNode);
            leftEyeNode.setLocalPosition(eyeSpacing, eyeHeight, distFromHead);
            Billboard leftEyeFlare = new Billboard(leftEyeNode, eyeSize, eyeSize, "flare1.png", eyeColor);
            addResponsibility(leftEyeFlare);

            // right eye
            if (headId != 2) {
                rightEyeNode = headNode.createChildSceneNode(name + "EyeRNode");
                addResponsibility(rightEyeNode);
                rightEyeNode.setLocalPosition(-eyeSpacing, eyeHeight, distFromHead);
                Billboard rightEyeFlare = new Billboard(rightEyeNode, eyeSize, eyeSize, "flare1.png", eyeColor);
                addResponsibility(rightEyeFlare);
            }

            // animation controller
            animationController = new RemoteCharacterAnimationController(this, controller);
            addResponsibility((Updatable)animationController);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initPhysics() {
        createBody(false);
        controller = new CharacterController(this);
        addResponsibility(controller);
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
    public Team getSide(){ return playerSide; }
    public boolean isLocal() { return local; }
    public Glove getGlove() { return glove; }
    public CharacterAnimationController getAnimationController() { return animationController; }
    public SceneNode getHandNode() { return handNode; }
    public boolean isDead() { return dead; }
    public boolean isAi() { return (this instanceof AIPlayer); }

    public byte getHealth() { return health; }

    public void setHealth(byte health) {
        if (!dead && health <= 0) { die(); }
        else if (dead && health > 0) { respawn(); }
        if (health - this.health <= -5) {
            glitchSound.play();
        }
        if (this.health >= 20 && health < 20) {
            nearDeathSound.play();
        }
        this.health = health;
    }

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

        if (dead && (local || (!UDPServer.hasServer() && !UDPClient.hasClient()) || isAi())) {
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

        if (invulnerability > 0) {
            invulnerability -= delta;
            if (invulnerability < 0) { invulnerability = 0; }
        }

        if (controller.isMoving() & controller.isOnGround()) {
            nextStepTime -= delta;
            if (nextStepTime <= 0) {
                int volume = local ? 40 : 60;
                if (controller.isCrouching()) { volume /= 2; }
                stepSound.play(volume);
                nextStepTime = 400;
            }
        } else {
            nextStepTime = 200;
        }

        if (hurtSoundTimeout > 0) { hurtSoundTimeout -= delta; }

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

    public boolean willHurt(int value) {
        return (value > absorbHurt && invulnerability <= 0);
    }

    public void hurt(int value) {
        if (!local && (UDPServer.hasServer() || UDPClient.hasClient()) && !isAi()) { return; }
        if (invulnerability > 0) { return; }
        if (value <= absorbHurt) { return; }
        int applyHurt = (int)(value - absorbHurt);
        int oldHealth = health;
        absorbHurt = value;
        health -= applyHurt;
        if (health <= 0) {
            health = 0;
            die();
        }

        // play sounds
        if (hurtSoundTimeout <= 0 && value > 5) {
            glitchSound.play();
            hurtSoundTimeout = 250;
        }
        if (oldHealth >= 20 && health < 20) {
            nearDeathSound.play();
        }

        if (local) {
            if (!dead) {
                float alpha = value / 20f + 0.1f;
                if (alpha > 1) { alpha = 1; }
                hudController.showPeripheral(new Color(1f, 0.1f, 0.1f, alpha), 800f * (alpha));
            }

        }
    }

    public void die() {
        if (dead) { return; }

        Color lightColor = (playerSide == Team.Orange) ? new Color(255, 200, 80) : new Color(100, 100, 255);
        new LightFade(node, lightColor, 100f, 0.01f, 300f);

        try {
            new Particle(4f, 4f, node.getWorldPosition(), Vector3f.createZeroVector(), "pow2.png", lightColor, 250f);
        } catch (IOException e) {
            e.printStackTrace();
        }

        respawnTimeout = respawnSeconds * 1000f;

        health = 0;
        absorbHurt = 0;
        invulnerability = 0;

        headDebris = createDebrisPart("head");
        createDebrisPart("torso");
        createDebrisPart("arm_upper_L");
        createDebrisPart("arm_upper_R");
        createDebrisPart("leg_upper_L");
        createDebrisPart("leg_upper_R");

        controller.setCrouching(false);

        if (local && hudController != null) {
            hudController.hideLivingHud();
            hudController.showPeripheral(Color.BLACK, respawnSeconds * 10000f);
        }

        PhysicsManager.removeRigidBody(body);

        if (roboNode != null) {
            node.detachChild(roboNode);
            leftEyeNode.setLocalScale(0.001f, 0.001f, 0.001f);
            if (rightEyeNode != null) { rightEyeNode.setLocalScale(0.001f, 0.001f, 0.001f); }
        }

        dead = true;

        deathSound.play();
    }

    private Vector3 getSpawnPosition() {
        float xPosition = 20 + 20 * (float)Math.random();
        if (getSide() == Team.Orange) { xPosition *= -1; }
        return Vector3f.createFrom(xPosition, 1.9f, -53.5f);
    }

    public void respawn() {
        if (!dead) { return; }

        dead = false;
        respawnTimeout = 0;
        absorbHurt = 0;

        if (local || (!UDPClient.hasClient() && !UDPServer.hasServer()) || isAi()) {
            health = 100;
            invulnerability = 3000;
            setPosition(getSpawnPosition());
            setVelocity(Vector3f.createZeroVector());
            setPitch(0);
            setYaw(0);
        }

        if (hudController != null) {
            hudController.showLivingHud();
            hudController.showPeripheral(Color.WHITE, 1000f);
        }

        PhysicsManager.addRigidBody(body);

        if (roboNode != null) {
            node.attachChild(roboNode);
            leftEyeNode.setLocalScale(1f, 1f, 1f);
            if (rightEyeNode != null) { rightEyeNode.setLocalScale(1f, 1f, 1f); }
        }

        respawnSound.play();
    }

    private Debris createDebrisPart(String boneName) {
        float duration = 10000f;
        try {
            String modelName = boneName + ".obj";
            TextureState textureState = this.textureState;
            if (boneName.equals("head") && headId == 2) {
                modelName = "head2.obj";
                textureState = this.headState;
            }
            if (local) {
                return new Debris(node.getWorldPosition(), cameraNode.getWorldRotation().toQuaternion(), getVelocity(), modelName, textureState, duration);
            } else {
                Matrix4 matrix = roboNode.getWorldTransform().mult(robo.getBoneModelTransform(boneName));
                Vector3 location = matrix.column(3).toVector3();
                Quaternion rotation = matrix.toQuaternion();
                Debris debris = new Debris(location, rotation, getVelocity(), modelName, textureState, duration);
                return debris;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
