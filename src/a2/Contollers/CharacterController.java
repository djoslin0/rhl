package a2.Contollers;

import Networking.PacketAttack;
import Networking.UDPClient;
import a2.Actions.ActionMove;
import a2.Actions.ActionRotate;
import a2.GameEntities.Attackable;
import a2.GameEntities.Player;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.GameEntities.Terrain;
import myGameEngine.Helpers.MathHelper;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.PhysicsManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;
import ray.rml.*;

public class CharacterController extends InternalTickCallback {

    // auto-set
    private Player player;
    private SceneNode node;
    private SceneNode cameraNode;
    private RigidBody body;
    private javax.vecmath.Vector3f linearVelocity = new javax.vecmath.Vector3f();
    private javax.vecmath.Vector3f angularVelocity = new javax.vecmath.Vector3f();
    private javax.vecmath.Vector3f gravity = new javax.vecmath.Vector3f();
    private Terrain terrain;
    private boolean onGround;
    private Vector3 normal;
    private float groundY;
    private String animation = "";

    // track in history
    private boolean wasOnGround;
    private boolean wasJumping;
    private boolean wasAttacking;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveForward;
    private boolean moveBackward;
    private boolean crouching;
    private boolean jumping;
    private boolean attacking;
    private boolean wrapYaw;
    private int jumpTicks;
    private int attackTicks;
    private Vector3 lastMovement = Vector3f.createZeroVector();
    private int knockbackTimeout = 0;
    private int ignoreKnockTimeout = 0;
    // note: also track cameraNode orientation

    // constants
    private final float groundAcceleration = 70;
    private final float crouchAcceleration = 40;
    private final float airAcceleration = 20;
    private final float maxSpeed = 20;
    private final float maxCrouchSpeed = 10;
    private final float groundFriction = 0.95f;
    private final float rotationSensititvity = 1;
    private final int jumpTickTimeout = 8;
    private final int attackTickTimeout = 40;

    public CharacterController(Player player) {
        this.player = player;
        this.node = player.getNode();
        this.cameraNode = player.getCameraNode();
        this.body = player.getBody();
        PhysicsManager.addCallback(this);

        for (Object o : EntityManager.get("terrain")) {
            terrain = (Terrain) o;
            break;
        }
    }

    public byte getControls() {
        byte controls = 0;
        controls |= (moveLeft ? 1 : 0) << 0;
        controls |= (moveRight ? 1 : 0) << 1;
        controls |= (moveForward ? 1 : 0) << 2;
        controls |= (moveBackward ? 1 : 0) << 3;
        controls |= (jumpTicks > 0 ? 1 : 0) << 4;
        controls |= (attackTicks > 0 ? 1 : 0) << 5;
        controls |= (crouching ? 1 : 0) << 6;
        controls |= (wrapYaw ? 1 : 0) << 7;
        return controls;
    }

    public void setControls(byte controls) {
        moveLeft = (controls & (1 << 0)) != 0;
        moveRight = (controls & (1 << 1)) != 0;
        moveForward = (controls & (1 << 2)) != 0;
        moveBackward = (controls & (1 << 3)) != 0;
        jumping = (controls & (1 << 4)) != 0;
        attacking = (controls & (1 << 5)) != 0;
        setCrouching((controls & (1 << 6)) != 0);
        wrapYaw = (controls & (1 << 7)) != 0;
    }

    public static boolean wrapYawFromControl(byte controls) {
        return (controls & (1 << 7)) != 0;
    }

    public void move(ActionMove.Direction direction, boolean value) {
        // remember movement directions, to be applied on physics tick
        switch (direction) {
            case FORWARD: moveForward = value; break;
            case BACKWARD: moveBackward = value; break;
            case LEFT: moveLeft = value; break;
            case RIGHT: moveRight = value; break;
        }
    }

    public void rotate(ActionRotate.Direction direction, float speed) {
        // perform rotation on proper node immediately
        double mult = rotationSensititvity / 500f;
        double currentPitch = cameraNode.getLocalRotation().getPitch();
        Radianf angle = Radianf.createFrom(0);
        switch (direction) {
            case X:
                angle = Radianf.createFrom((float)mult * -speed);
                node.yaw(angle);
                break;
            case Y:
                angle = Radianf.createFrom((float)mult * speed);
                // restrict pitch
                if (currentPitch < Math.PI / -2f && angle.valueRadians() < 0) { return; }
                if (currentPitch > Math.PI / 2f && angle.valueRadians() > 0) { return; }
                cameraNode.pitch(angle);
                break;
        }
    }

    public void attack() {
        // player requested attack
        attacking = true;
    }

    public void jump() {
        // player requested jump
        jumping = true;
    }

    public void knockback(Vector3 vec) {
        if (ignoreKnockTimeout > 0) { return; }
        ignoreKnockTimeout = 10;
        knockbackTimeout = (vec != null) ? (int)(vec.length() / 10f) : 10;
        if (knockbackTimeout > 100) { knockbackTimeout = 100; }

        if (vec == null) { return; }
        body.applyCentralImpulse(vec.toJavaX());
    }

    private float twoDimensionalLength(javax.vecmath.Vector3f vec) {
        // find length along x/z only
        return (float)Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2));
    }

    private Vector3 getMovementDirection() {
        // convert forward/right requested movement into vector
        Vector3 forward = node.getWorldForwardAxis().mult((moveForward ? 1 : 0) - (moveBackward ? 1 : 0));
        Vector3 right = node.getWorldRightAxis().mult((moveLeft ? 1 : 0) - (moveRight ? 1 : 0));
        Vector3 movement = forward.add(right);
        return movement.length() <= 1 ? movement : movement.normalize();
    }

    private void groundTrace() {
        // make many raycasts around the player's position in order to determine if they are on the ground
        float stepSize = 0.1f; // defines how far from the origin to check
        normal = null;
        groundY = node.getWorldPosition().y();
        onGround = false;

        // trace 9 times
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                // setup trace
                Vector3 from = node.getWorldPosition().add(x * stepSize, 0, z * stepSize);
                Vector3 to = node.getWorldPosition().sub(0, 2, 0);
                CollisionWorld.ClosestRayResultCallback trace = new CollisionWorld.ClosestRayResultCallback(from.toJavaX(), to.toJavaX());
                PhysicsManager.getWorld().rayTest(from.toJavaX(), to.toJavaX(), trace);

                // detect collision
                if (trace.hasHit()) {
                    onGround = true; // we found ground
                    Vector3 traceNormal = Vector3f.createFrom(trace.hitNormalWorld).normalize();
                    if (normal == null || traceNormal.y() > normal.y()) {
                        // remember the least extreme normal (and the groundY for that trace)
                        normal = traceNormal;
                        groundY = trace.hitPointWorld.y;
                    }
                }
            }
        }

        // check terrain
        if (terrain != null) {
            float terrainHeight = terrain.getHeight(node.getWorldPosition());
            if ((!onGround || terrainHeight > groundY) && node.getWorldPosition().y() - 2 < terrainHeight) {
                onGround = true;
                groundY = terrain.getHeight(node.getWorldPosition());
                normal = terrain.getNormal(node.getWorldPosition(), node.getWorldForwardAxis(), node.getWorldRightAxis());
            }
        }

        if (normal == null || normal.y() < 0.5f) {
            // if our normal is too extreme, override to say we aren't on the ground
            normal = Vector3f.createUnitVectorY();
            onGround = false;
        }
    }

    @Override
    public void internalTick(DynamicsWorld dynamicsWorld, float timeStep) {
        // restrict angular velocity to 0
        angularVelocity.set(0, 0, 0);
        body.setAngularVelocity(angularVelocity);

        // keep linearVelocity up to date
        body.getLinearVelocity(linearVelocity);

        // track whether or not yaw should wrap around
        wrapYaw = node.getLocalRotation().getRoll() != 0;

        cameraUpdate();

        groundTrace();

        checkAttack();

        checkJump();

        if (knockbackTimeout > 0) { --knockbackTimeout; }
        if (ignoreKnockTimeout > 0) { --ignoreKnockTimeout; }

        // do ground or air movement depending on onGround status
        // store current movement in order to undo it on next tick
        if (onGround) {
            lastMovement = groundMove();
        } else {
            lastMovement = airMove();
        }

        // keep linearVelocity up to date
        body.setLinearVelocity(linearVelocity);

        checkAnimation();

        wasOnGround = onGround;
    }

    private void cameraUpdate() {
        float height = cameraNode.getLocalPosition().y();
        if (crouching && height > Player.cameraCrouchHeight) {
            height -= 0.1f;
            if (height < Player.cameraCrouchHeight) { height = Player.cameraCrouchHeight; }
            cameraNode.setLocalPosition(0, height, 0);
            cameraNode.update();
        } else if (!crouching && height < Player.cameraHeight) {
            height += 0.1f;
            if (height > Player.cameraHeight) { height = Player.cameraHeight; }
            cameraNode.setLocalPosition(0, height, 0);
            cameraNode.update();
        }
    }

    private Vector3 groundMove() {
        float acceleration = crouching ? crouchAcceleration : groundAcceleration;
        Vector3 movement = getMovementDirection().mult(acceleration);

        // apply friction
        float friction = MathHelper.lerp(groundFriction, 1, knockbackTimeout / 100f);
        if (friction > 1) { friction = 1; }
        linearVelocity.scale(friction);

        if (movement.length() >= 0.2f) {
            // undo the previous movement's changes to velocity
            // this is done so that we do not feel like we're skating on ice
            body.applyCentralImpulse(lastMovement.mult(-0.99f).toJavaX());

            float speed = movement.length();
            try {
                // adjust desired movement to align along ground plane
                movement = clipVelocity(movement, normal).normalize().mult(speed);
            } catch (Exception ex) {}

            // apply new movement to velocity
            float previousSpeed = linearVelocity.length();
            body.setLinearVelocity(linearVelocity);
            body.applyCentralImpulse(movement.toJavaX());
            body.getLinearVelocity(linearVelocity);
            float currentSpeed = linearVelocity.length();

            // cap movement speed so we do not constantly gain more momentum
            float speedCap = crouching ? maxCrouchSpeed : maxSpeed;
            if (currentSpeed > previousSpeed && currentSpeed > speedCap) {
                linearVelocity.normalize();
                linearVelocity.scale(Math.max(previousSpeed, speedCap));
            }
        }

        // remove gravity
        gravity.y = 0;
        body.setGravity(gravity);

        // hover character slightly above the ground
        // this is so we don't bounce off ramps and changing elevations
        com.bulletphysics.linearmath.Transform t = new com.bulletphysics.linearmath.Transform();
        body.getWorldTransform(t);
        t.origin.y = groundY + 1.85f;
        body.proceedToTransform(t);

        return movement;
    }

    private Vector3 airMove() {
        float acceleration = airAcceleration;
        Vector3 movement = getMovementDirection().mult(acceleration);

        if (movement.length() >= 0.2f) {
            // undo the previous movement's changes to velocity
            // this is done so that we do not feel like we're skating on ice
            if (wasOnGround == onGround) {
                body.applyCentralImpulse(lastMovement.mult(-0.99f).toJavaX());
            }

            // apply new movement to velocity
            float previousSpeed = twoDimensionalLength(linearVelocity);
            body.setLinearVelocity(linearVelocity);
            body.applyCentralImpulse(movement.toJavaX());
            body.getLinearVelocity(linearVelocity);
            float currentSpeed = twoDimensionalLength(linearVelocity);

            // cap movement speed so we do not constantly gain more momentum
            if (currentSpeed > previousSpeed && currentSpeed > maxSpeed) {
                float oldY = linearVelocity.y;
                linearVelocity.y = 0;
                linearVelocity.normalize();
                linearVelocity.scale(Math.max(previousSpeed, maxSpeed));
                linearVelocity.y = oldY;
            }
        }

        // apply gravity
        PhysicsManager.getWorld().getGravity(gravity);
        body.setGravity(gravity);

        return movement;
    }

    private Attackable getAttackable(CollisionObject object) {
        if (object == EntityManager.getPuck().getBody()) {
            return EntityManager.getPuck();
        }

        // lookup player
        for (Object o : EntityManager.get("player")) {
            Player player = (Player)o;
            if (object == player.getBody()) {
                return player;
            }
        }

        return null;
    }

    private void checkAttack() {
        wasAttacking = attacking;

        if (attackTicks > 0) {
            // prevent onGround status for a few ticks after starting attack
            attackTicks--;
            return;
        }

        if (!attacking) { return; }
        attacking = false;

        attackTicks = attackTickTimeout;

        if (!player.isLocal()) { return; }

        Vector3 toPosition = cameraNode.getWorldPosition().add(cameraNode.getWorldForwardAxis().mult(5f));
        javax.vecmath.Vector3f from = cameraNode.getWorldPosition().toJavaX();
        javax.vecmath.Vector3f to = toPosition.toJavaX();
        CollisionWorld.ClosestRayResultCallback closest = new CollisionWorld.ClosestRayResultCallback(from, to);
        closest.collisionFilterMask = PhysicsManager.COLLIDE_IGNORE_LOCAL_PLAYER;

        PhysicsManager.getWorld().rayTest(from, to, closest);

        if (!closest.hasHit()) { return; }
        if (!(closest.collisionObject instanceof RigidBody)) { return; }

        // figure out which id/attackable is being attacked
        Attackable attackable = getAttackable(closest.collisionObject);
        if (attackable == null) {
            //UDPClient.send(new PacketAttack((byte)-2, cameraNode.getWorldForwardAxis(), Vector3f.createZeroVector()));
            return;
        }

        Transform t = new Transform();
        RigidBody rb = (RigidBody) closest.collisionObject;
        rb.getWorldTransform(t);

        Vector3 hitPoint = Vector3f.createFrom((javax.vecmath.Vector3f)closest.hitPointWorld.clone());
        Vector3 origin = Vector3f.createFrom(t.origin);

        Vector3 aim = cameraNode.getWorldForwardAxis();
        Vector3 relative = hitPoint.sub(origin);
        attackable.attacked(aim, relative);

        if (UDPClient.hasClient()) {
            UDPClient.send(new PacketAttack(attackable.getId(), aim, relative));
        }
    }

    private void checkJump() {
        wasJumping = jumping;

        if (jumpTicks > 0) {
            // prevent onGround status for a few ticks after starting jump
            jumpTicks--;
            onGround = false;
            normal = Vector3f.createUnitVectorY();
            return;
        }

        // make sure we are pressing jump
        if (!jumping) { return; }
        jumping = false;

        // make sure we are on the ground
        if (!onGround) { return; }

        // jump off the ground
        if (player.isLocal()) {
            linearVelocity.add(normal.mult(12).toJavaX());
            onGround = false;
            normal = Vector3f.createUnitVectorY();
            jumpTicks = jumpTickTimeout;
        }
    }

    private void checkAnimation() {

        SkeletalEntity robo = player.getRobo();
        if (robo != null) {
            // align head to camera pitch
            robo.addRotationOverride("head", cameraNode.getLocalRotation().toQuaternion());

            // align arm to camera pitch
            float pitch_upper = (float) (cameraNode.getLocalRotation().getPitch() / 2f - Math.PI / 3.5f);
            float pitch_lower = (float) (cameraNode.getLocalRotation().getPitch() / 1.9f + Math.PI / -2.5f);

            if (attackTicks > 0) {
                float attackScale = (attackTicks / (float)attackTickTimeout);
                attackScale *= attackScale;
                attackScale = 1f - attackScale;
                pitch_upper += Math.sin(attackScale * Math.PI) * 2f;
                pitch_lower -= Math.sin(attackScale * Math.PI) * 0.5f;
            }
            Matrix3 m = Matrix3f.createFrom(0, 0, pitch_upper);
            robo.addRotationOverride("arm_upper_R", m.toQuaternion());

            Matrix3 m2 = Matrix3f.createFrom(pitch_lower, 0, 0);
            robo.addRotationOverride("arm_lower_R", m2.toQuaternion());
        }

        if (wasJumping) {
            if (animation != "jump") { player.animate("jump", 0.04f, SkeletalEntity.EndType.NONE, false); }
            animation = "jump";
            return;
        }

        if (!onGround) {
            if (animation != "falling") { player.animate("falling", 0.025f, SkeletalEntity.EndType.LOOP, true); }
            animation = "falling";
            return;
        }

        if (onGround && !wasOnGround) {
            if (animation != "landing") { player.animate("land", 0.025f, SkeletalEntity.EndType.NONE, false); }
            animation = "landing";
            return;
        }

        if (crouching) {
            if (moveForward && !moveBackward) {
                if (animation != "crouch_walk_forward") { player.animate("crouch_walk", 0.1f, SkeletalEntity.EndType.LOOP, true); }
                animation = "crouch_walk_forward";
            } else if (moveBackward && !moveForward) {
                if (animation != "crouch_walk_backward") { player.animate("crouch_walk", -0.1f, SkeletalEntity.EndType.LOOP, true); }
                animation = "crouch_walk_backward";
            } else if (moveRight && !moveLeft) {
                if (animation != "crouch_sidestep_right") { player.animate("crouch_sidestep", 0.12f, SkeletalEntity.EndType.LOOP, true); }
                animation = "crouch_sidestep_right";
            } else if (moveLeft && !moveRight) {
                if (animation != "crouch_sidestep_left") { player.animate("crouch_sidestep", -0.12f, SkeletalEntity.EndType.LOOP, true); }
                animation = "crouch_sidestep_left";
            } else {
                if (animation != "crouch_idle") { player.animate("crouch_idle", 0.04f, SkeletalEntity.EndType.LOOP, true); }
                animation = "crouch_idle";
            }
            return;
        }

        if (moveForward && !moveBackward) {
            if (animation != "runforward") { player.animate("run", 0.095f, SkeletalEntity.EndType.LOOP, true); }
            animation = "runforward";
        } else if (moveBackward && !moveForward) {
            if (animation != "runback") { player.animate("run", -0.095f, SkeletalEntity.EndType.LOOP, true); }
            animation = "runback";
        } else if (moveRight && !moveLeft) {
            if (animation != "sidestep_right") { player.animate("sidestep", 0.12f, SkeletalEntity.EndType.LOOP, true); }
            animation = "sidestep_right";
        } else if (moveLeft && !moveRight) {
            if (animation != "sidestep_left") { player.animate("sidestep", -0.12f, SkeletalEntity.EndType.LOOP, true); }
            animation = "sidestep_left";
        } else {
            if (animation != "idle") { player.animate("idle", 0.04f, SkeletalEntity.EndType.LOOP, true); }
            animation = "idle";
        }
    }

    private Vector3 clipVelocity(Vector3 in, Vector3 normal) {
        // magic to convert a flat movement vector into one based on the ground plane
        // shamelessly stolen from Quake 3
        // https://github.com/id-Software/Quake-III-Arena/blob/master/code/game/bg_pmove.c
        float overbounce = 1.001f;
        float backoff = in.dot(normal);
        if (backoff < 0) {
            backoff *= overbounce;
        } else {
            backoff /= overbounce;
        }
        return in.sub(normal.mult(backoff));
    }

    public void setCrouching(boolean crouching) {
        if (crouching == this.crouching) { return; }
        this.crouching = crouching;
        this.body = player.createBody(crouching);
    }
}
