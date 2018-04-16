package a2.Contollers;

import a2.Actions.ActionMove;
import a2.Actions.ActionRotate;
import a2.GameEntities.Player;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.GameEntities.Terrain;
import myGameEngine.Helpers.MathHelper;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.TimeManager;
import ray.rage.scene.SceneNode;
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
    private boolean moveLeft;
    private boolean moveRight;
    private boolean moveForward;
    private boolean moveBackward;
    private boolean jumping;
    private boolean attacking;
    private boolean wrapYaw;
    private int jumpTicks;
    private Vector3 lastMovement = Vector3f.createZeroVector();
    private int knockbackTimeout = 0;
    private int ignoreKnockTimeout = 0;
    // note: also track cameraNode orientation

    // constants
    private final float groundAcceleration = 70;
    private final float airAcceleration = 20;
    private final float maxSpeed = 14;
    private final float groundFriction = 0.95f;
    private final float rotationSensititvity = 1;

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
        controls |= (jumping ? 1 : 0) << 4;
        controls |= (attacking ? 1 : 0) << 5;
        controls |= (wrapYaw ? 1 : 0) << 6;
        return controls;
    }

    public void setControls(byte controls) {
        moveLeft = (controls & (1 << 0)) != 0;
        moveRight = (controls & (1 << 1)) != 0;
        moveForward = (controls & (1 << 2)) != 0;
        moveBackward = (controls & (1 << 3)) != 0;
        jumping = (controls & (1 << 4)) != 0;
        attacking = (controls & (1 << 5)) != 0;
        wrapYaw = (controls & (1 << 6)) != 0;
    }

    private boolean wrapYawFromControl(byte controls) {
        return (controls & (1 << 6)) != 0;
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
        Radianf angle = Radianf.createFrom(0);
        switch (direction) {
            case X:
                angle = Radianf.createFrom((float)mult * -speed);
                node.yaw(angle);
                break;
            case Y:
                angle = Radianf.createFrom((float)mult * speed);
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
        knockbackTimeout = (int)(vec.length() / 10f);
        if (knockbackTimeout > 100) { knockbackTimeout = 100; }
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

        groundTrace();

        if (!onGround) {
            System.out.println("NOT ON GROUND!");
        }

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

        wasOnGround = onGround;
    }

    private Vector3 groundMove() {
        float acceleration = groundAcceleration;
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
            if (currentSpeed > previousSpeed && currentSpeed > maxSpeed) {
                linearVelocity.normalize();
                linearVelocity.scale(Math.max(previousSpeed, maxSpeed));
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

    private void checkAttack() {
        if (!attacking) { return; }
        attacking = false;

        Vector3 toPosition = cameraNode.getWorldPosition().add(cameraNode.getWorldForwardAxis().mult(5f));
        javax.vecmath.Vector3f from = cameraNode.getWorldPosition().toJavaX();
        javax.vecmath.Vector3f to = toPosition.toJavaX();
        CollisionWorld.ClosestRayResultCallback closest = new CollisionWorld.ClosestRayResultCallback(from, to);
        PhysicsManager.getWorld().rayTest(from, to, closest);
        if (closest.hasHit()) {
            if (closest.collisionObject instanceof RigidBody) {
                RigidBody rb = (RigidBody) closest.collisionObject;
                javax.vecmath.Vector3f force = cameraNode.getWorldForwardAxis().mult(20000f).toJavaX();

                Transform t = new Transform();
                rb.getWorldTransform(t);
                javax.vecmath.Vector3f relative = (javax.vecmath.Vector3f)closest.hitPointWorld.clone();
                relative.sub(t.origin);
                rb.applyImpulse(force, relative);

                rb.activate();

                javax.vecmath.Vector3f vel = new javax.vecmath.Vector3f();
                rb.getLinearVelocity(vel);
                System.out.println("Hit: " + closest.hasHit() + " @ " + TimeManager.getTick() + ", " + t.origin);
            }
        }
    }

    private void checkJump() {
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
        linearVelocity.add(normal.mult(12).toJavaX());
        onGround = false;
        normal = Vector3f.createUnitVectorY();
        jumpTicks = 8;
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

    public HistoryCharacterController remember() {
        return new HistoryCharacterController(player);
    }

    public class HistoryCharacterController {
        private Player player;
        private CharacterController cc;
        private boolean wasOnGround;
        private byte controls;
        private int jumpTicks;
        private Vector3 lastMovement;
        private int knockbackTimeout;
        private int ignoreKnockTimeout;
        private Matrix3 nodeRotation;
        private Matrix3 cameraNodeRotation;

        private HistoryCharacterController(Player player) {
            this.player = player;
            this.cc = player.getController();
            wasOnGround = cc.wasOnGround;
            controls = cc.getControls();
            jumpTicks = cc.jumpTicks;
            lastMovement = cc.lastMovement.add(0, 0, 0);
            knockbackTimeout = cc.knockbackTimeout;
            ignoreKnockTimeout = cc.ignoreKnockTimeout;
            nodeRotation = cc.node.getLocalRotation();
            cameraNodeRotation = cc.cameraNode.getLocalRotation();
        }

        public void apply() {
            cc.wasOnGround = wasOnGround;
            cc.jumpTicks = jumpTicks;
            cc.lastMovement = lastMovement;
            cc.knockbackTimeout = knockbackTimeout;
            cc.ignoreKnockTimeout = ignoreKnockTimeout;
            applyInput();
        }

        public void applyInput() {
            cc.setControls(controls);
            cc.cameraNode.setLocalRotation(cameraNodeRotation);
            cc.node.setLocalRotation(nodeRotation);
        }

        public void overwriteInput(byte controls, float pitch, float yaw) {
            if (player.getId() == 0) { return; }
            this.controls = controls;
            double wrapYawValue = cc.wrapYawFromControl(controls) ? Math.PI : 0;
            cameraNodeRotation = Matrix3f.createFrom(pitch, 0, 0);
            nodeRotation = Matrix3f.createFrom(wrapYawValue, yaw, wrapYawValue);
        }

        public byte getControls() { return this.controls; }

        public Player getPlayer() { return player; }
    }
}
