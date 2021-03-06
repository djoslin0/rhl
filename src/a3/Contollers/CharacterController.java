package a3.Contollers;

import Networking.PacketAttack;
import Networking.UDPClient;
import Networking.UDPServer;
import a3.GameEntities.AIPlayer;
import a3.MyGame;
import a3.Actions.ActionMove;
import a3.Actions.ActionRotate;
import a3.GameEntities.Attackable;
import a3.GameEntities.Player;
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
import myGameEngine.Singletons.Settings;
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

    // track in history
    private boolean wasOnGround;
    private boolean wasAttacking;
    private boolean wasJumping;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveForward;
    private boolean moveBackward;
    private boolean crouching;
    private boolean jumping;
    private boolean attacking;
    private int jumpTicks;
    private int attackTicks;
    private Vector3 lastMovement = Vector3f.createZeroVector();
    private int knockbackTimeout = 0;
    private int ignoreKnockTimeout = 0;
    private boolean jumpQueued;
    // note: also track cameraNode orientation

    // constants
    public static final float groundAcceleration = 80;
    public static final float crouchAcceleration = 45;
    public static final float airAcceleration = 25;
    public static final float maxSpeed = 25;
    public static final float maxCrouchSpeed = 15;
    public static final float groundFriction = 0.95f;
    public static final float rotationSensititvity = 1;
    public static final int jumpTickTimeout = 50;
    public static final int attackTickTimeout = 50;

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

    public boolean isCrouching() { return crouching; }
    public boolean isOnGround() { return onGround; }
    public boolean wasOnGround() { return wasOnGround; }
    public boolean isMovingForward() { return moveForward; }
    public boolean isMovingBackward() { return moveBackward; }
    public boolean isMovingRight() { return moveRight; }
    public boolean isMovingLeft() { return moveLeft; }

    public byte getControls() {
        byte controls = 0;
        controls |= (moveLeft ? 1 : 0) << 0;
        controls |= (moveRight ? 1 : 0) << 1;
        controls |= (moveForward ? 1 : 0) << 2;
        controls |= (moveBackward ? 1 : 0) << 3;
        controls |= (jumpTicks > 0 ? 1 : 0) << 4;
        controls |= (attackTicks > 0 ? 1 : 0) << 5;
        controls |= (crouching ? 1 : 0) << 6;
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

    public boolean clearJumpQueue() {
        boolean wasJumpQueued = jumpQueued;
        jumpQueued = false;
        return wasJumpQueued;
    }

    public void knockback(Vector3 vec, Vector3 relative) {
        if (ignoreKnockTimeout > 0) { return; }
        ignoreKnockTimeout = 10;
        knockbackTimeout = (vec != null) ? (int)(vec.length() / 10f) : 10;
        if (knockbackTimeout > 100) { knockbackTimeout = 100; }

        if (vec.length() >= 1000f) {
            player.getAnimationController().knock(vec, relative);
        }

        if (player.isLocal() || player.isAi() || (!UDPClient.hasClient() && !UDPServer.hasServer())) {
            javax.vecmath.Vector3f finalVec = vec.toJavaX();
            finalVec.y *= 0.8f;
            body.applyCentralImpulse(finalVec);
        }
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
        float downDist = 2.1f;
        normal = null;
        groundY = node.getWorldPosition().y();
        onGround = false;

        if (linearVelocity.y > 4 && ignoreKnockTimeout > 0) {
            // if we're knocked upward we can not be on the ground
            normal = Vector3f.createUnitVectorY();
            onGround = false;
            return;
        }

        Vector3 worldPosition = node.getWorldPosition();

        // trace 9 times
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                // setup trace
                Vector3 from = worldPosition.add(x * stepSize, 0, z * stepSize);
                Vector3 to = worldPosition.sub(0, downDist, 0);
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
            float terrainHeight = terrain.getHeight(worldPosition);
            if ((!onGround || terrainHeight > groundY) && node.getWorldPosition().y() - downDist < terrainHeight) {
                onGround = true;
                groundY = terrain.getHeight(worldPosition);
                normal = terrain.getNormal(worldPosition, node.getWorldForwardAxis(), node.getWorldRightAxis());
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
        if (player.isDead()) {
            jumping = false;
            wasJumping = false;
            attacking = false;
            wasAttacking = false;
            jumpTicks = 0;
            attackTicks = 0;
            return;
        }

        // restrict angular velocity to 0
        angularVelocity.set(0, 0, 0);
        body.setAngularVelocity(angularVelocity);

        // keep linearVelocity up to date
        body.getLinearVelocity(linearVelocity);

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

        if (!wasOnGround && onGround) {
            player.playLandSound();
        }

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
        float friction = MathHelper.lerp(groundFriction, 1, Math.pow(knockbackTimeout / 100f, 0.5f));
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

        // zero velocity with respect to the ground normal
        Vector3 lv = Vector3f.createFrom(linearVelocity);
        linearVelocity = lv.sub(normal.mult(lv.dot(normal))).toJavaX();

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

        if (!player.isLocal() && !player.isAi()) { return; }

        Vector3 toPosition = cameraNode.getWorldPosition().add(cameraNode.getWorldForwardAxis().mult(6f));
        javax.vecmath.Vector3f from = cameraNode.getWorldPosition().toJavaX();
        javax.vecmath.Vector3f to = toPosition.toJavaX();
        CollisionWorld.ClosestRayResultCallback closest = new CollisionWorld.ClosestRayResultCallback(from, to);
        closest.collisionFilterMask = PhysicsManager.COLLIDE_IGNORE_LOCAL_PLAYER;

        PhysicsManager.getWorld().rayTest(from, to, closest);

        // figure out which id/attackable is being attacked
        Attackable attackable = getAttackable(closest.collisionObject);

        Vector3 rayEnd = closest.hasHit() ? Vector3f.createFrom(closest.hitPointWorld) : Vector3f.createFrom(closest.rayToWorld);
        if (closest.hasHit()) {
            player.getGlove().attack(attackable, rayEnd);
        } else {
            player.getGlove().attack(attackable, rayEnd.sub(player.getCameraNode().getWorldForwardAxis().mult(3f)));
        }

        if (!closest.hasHit()
            || !(closest.collisionObject instanceof RigidBody)
            || attackable == null) {
            if (UDPClient.hasClient()) {
                UDPClient.send(new PacketAttack(player.getId(), (byte) -1, cameraNode.getWorldForwardAxis(), rayEnd));
            } else if (UDPServer.hasServer() && player.isAi()) {
                UDPServer.sendToAll(new PacketAttack(player.getId(), (byte) -1, cameraNode.getWorldForwardAxis(), rayEnd));
            }
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
            UDPClient.send(new PacketAttack(player.getId(), attackable.getId(), aim, relative));
        } else if (UDPServer.hasServer() && player.isAi()) {
            UDPServer.sendToAll(new PacketAttack(player.getId(), attackable.getId(), aim, relative));
        }
    }

    private void checkJump() {
        if (!player.isLocal() && !wasJumping && jumping) {
            // queue up a jump animation event
            jumpQueued = true;
            player.playJumpSound();
        }

        // remember last jumping state
        wasJumping = jumping;

        if (!player.isLocal()) {
            // do not actually perform any jump stuff for remote players
            jumpTicks = jumping ? 1 : 0;
            return;
        }

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
        wasJumping = true;

        // make sure we are on the ground
        if (!onGround) { return; }

        // jump off the ground
        if (player.isLocal()) {
            linearVelocity.add(normal.mult(12).toJavaX());
            onGround = false;
            normal = Vector3f.createUnitVectorY();
            jumpTicks = jumpTickTimeout;
            player.playJumpSound();
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
        if (player.isDead()) { return; }
        this.crouching = crouching;
        this.body = player.createBody(crouching);
    }

    public boolean isMoving() {
        return moveBackward || moveForward || moveRight || moveLeft;
    }
}
