package a2.GameEntities;

import a2.Contollers.HudController;
import a2.GameState;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.GameEntities.LightFade;
import myGameEngine.GameEntities.Particle;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.*;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Quaternionf;
import ray.rml.Radianf;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import java.awt.*;
import java.io.IOException;

public class Puck extends GameEntity implements Attackable {
    private Entity obj;
    private RigidBody body;
    private SceneNode angularTestNode;

    private float angularPushScale = 400f;
    private float linearPushScale = 200f;
    private float mass = 1000f;

    private float freezeTime = 0;

    public Puck(Vector3 location) throws IOException {
        super(true);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Puck" + unique;

        obj = sm.createEntity(name, "puck.obj");
        addResponsibility(obj);
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);

        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);
        node.setLocalPosition(location);

        angularTestNode = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "TestNode");
        addResponsibility(angularTestNode);

        initPhysics();
    }

    private void initPhysics() {
        MotionStateController motionState = new MotionStateController(this.node);
        ConvexHullShape collisionShape = BulletConvert.entityToConvexHullShape(obj);
        collisionShape.setLocalScaling(node.getLocalScale().toJavaX());

        body = createBody(mass, motionState, collisionShape, PhysicsManager.COL_PUCK, PhysicsManager.COLLIDE_ALL);
        body.setRestitution(0.6f);
        body.setFriction(0.2f);
        body.setDamping(0.05f, 0f);
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
    }

    @Override
    public boolean shouldRegisterCollision() { return true; }

    public void reset() {
        body.setLinearVelocity(new javax.vecmath.Vector3f());
        body.setAngularVelocity(new javax.vecmath.Vector3f());
        Transform t = new Transform();
        t.origin.x = 0;
        t.origin.y = 25f;
        t.origin.z = 0;

        body.setWorldTransform(t);
        body.clearForces();

        try {
            new Particle(10f, 10f, Vector3f.createFrom(0, 25f, 0), Vector3f.createZeroVector(), "pow2.png", Color.WHITE, 300f);
        } catch (IOException e) {
            e.printStackTrace();
        }

        freeze();
    }

    private void freeze() {
        freezeTime = 5000f;
        body.setMassProps(0, new javax.vecmath.Vector3f(0, 0f, 0));
        Transform t = new Transform();
        body.getWorldTransform(t);
        body.getMotionState().setWorldTransform(t);
    }

    public void unfreeze() {
        freezeTime = 0f;
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0f, 0);
        body.getCollisionShape().calculateLocalInertia(mass, localInertia);
        body.setMassProps(mass, localInertia);

    }

    public void goalCollision(Player.Team team) {
        try{
            Color powColor = (team == Player.Team.Blue) ? new Color(255, 230, 170) : new Color(170, 170, 255);
            Particle pow = new Particle(10f, 10f, EntityManager.getPuck().getNode().getWorldPosition(), Vector3f.createZeroVector(), "pow2.png", powColor, 300f);
            new LightFade(pow.getNode(), powColor, 100f, 0.01f, 300f);
            for(int i=0; i<8; i++) {
                new PuckPartical(i);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        GameState.addScore(team, 1);

        reset();
    }

    public void playerCollision(GameEntity entity, ManifoldPoint contactPoint, boolean isA) {
        Player player = (Player) entity;
        Vector3 entityPosition = entity.getNode().getWorldPosition();
        Vector3 thisPosition = node.getWorldPosition();

        // get linear velocity
        javax.vecmath.Vector3f linearVelocityJavaX = new javax.vecmath.Vector3f();
        body.getLinearVelocity(linearVelocityJavaX);
        Vector3 linearVelocity = Vector3f.createFrom(linearVelocityJavaX);

        // get angular velocity
        javax.vecmath.Vector3f angularVelocityJavaX = new javax.vecmath.Vector3f();
        body.getAngularVelocity(angularVelocityJavaX);
        Vector3 angularVelocity = Vector3f.createFrom(angularVelocityJavaX);

        // calculate angular push vector
        float dist = entityPosition.sub(thisPosition).length();
        angularTestNode.setLocalPosition(thisPosition);
        angularTestNode.lookAt(entity.getNode());
        angularTestNode.pitch(Radianf.createFrom(angularVelocity.x() / 5f));
        angularTestNode.yaw(Radianf.createFrom(angularVelocity.y() / 5f));
        angularTestNode.roll(Radianf.createFrom(angularVelocity.z() / 5f));
        angularTestNode.moveForward(dist);
        Vector3 angularPush = angularTestNode.getWorldPosition().sub(entityPosition).mult(angularPushScale);

        // calculate linear push vector
        Vector3 linearPush = Vector3f.createZeroVector();
        Vector3 diff = entityPosition.sub(thisPosition);
        if (linearVelocity.lengthSquared() > 0) {
            float dot = linearVelocity.normalize().dot(diff.normalize());
            if (dot > 0.5f) { dot = 0.5f; }
            dot = dot * 2f;
            if (dot > 0) {
                linearPush = linearVelocity.mult(linearPushScale * dot);
            }
        }

        // push player
        Vector3 push = angularPush.add(linearPush);
        Vector3 collisionPoint = Vector3f.createFrom(contactPoint.positionWorldOnA).add(Vector3f.createFrom(contactPoint.positionWorldOnB)).div(2f);
        player.getController().knockback(push, collisionPoint.sub(player.getPosition()));

        // calculate player hurt/squeeze
        float linearDot = linearPush.dot(player.getVelocity()) / 45000;
        int hurtAmount = (int)(linearDot + angularVelocity.length() * 1.5f);
        if (hurtAmount > 0) {
            // squeezekill?

            // figure out if we should be squishing downward
            float yScalar = 1;
            Vector3 playerLocalPoint = isA ? Vector3f.createFrom(contactPoint.localPointB) : Vector3f.createFrom(contactPoint.localPointA);
            if (playerLocalPoint.y() > 1f && linearVelocity.y() < -5) {
                yScalar = Player.height * 0.9f;
            } else {
                yScalar = 0;
            }

            // create collision direction vector
            Vector3 diff2 = Vector3f.createFrom(contactPoint.positionWorldOnA).sub(Vector3f.createFrom(contactPoint.positionWorldOnB)).normalize().mult(2.5f);
            if (!isA) { diff2 = diff2.mult(-1f); }
            diff2 = Vector3f.createFrom(diff2.x(), diff2.y() * yScalar, diff2.z());

            // create start/end vectors for ray trace
            javax.vecmath.Vector3f start = isA ? Vector3f.createFrom(contactPoint.positionWorldOnB).toJavaX() : Vector3f.createFrom(contactPoint.positionWorldOnA).toJavaX();
            javax.vecmath.Vector3f end = Vector3f.createFrom(start).add(diff2).toJavaX();

            // raytrace squeezekill
            CollisionWorld.ClosestRayResultCallback squeezeKill = new CollisionWorld.ClosestRayResultCallback(start, end);
            squeezeKill.collisionFilterMask = PhysicsManager.COLLIDE_WORLD;
            PhysicsManager.getWorld().rayTest(start, end, squeezeKill);

            if (squeezeKill.hasHit() && hurtAmount > 4) {
                // squeezekill
                player.hurt(100);
            } else {
                // hurt player
                player.hurt(hurtAmount);
            }
        }
    }

    public void collision(GameEntity entity, ManifoldPoint contactPoint, boolean isA) {
        if (entity instanceof Goal) {
            javax.vecmath.Vector3f point = new javax.vecmath.Vector3f();
            contactPoint.getPositionWorldOnA(point);
            goalCollision((point.x < 0) ? Player.Team.Blue : Player.Team.Orange);
        } else if (entity instanceof Player) {
            playerCollision(entity, contactPoint, isA);
        }
    }

    public void attacked(Vector3 aim, Vector3 relative) {
        javax.vecmath.Vector3f jvel = new javax.vecmath.Vector3f();
        body.getLinearVelocity(jvel);
        Vector3 velocity = Vector3f.createFrom(jvel);
        float dot = velocity.dot(aim.normalize()) * 1.45f;
        float rally = 0;
        if (dot < 0) {
            rally = -dot;
            if (rally > 50f) { rally = 50f; }
        }
        javax.vecmath.Vector3f force = aim.mult(18000f + rally * 1000f).toJavaX();
        body.applyImpulse(force, relative.toJavaX());
        body.activate();
    }

    @Override
    public byte getId() {
        return 0;
    }

    public String listedName() { return "puck"; }
    public SceneNode getNode() { return node; }
    public RigidBody getBody() { return body; }
    public boolean isFrozen() { return freezeTime > 0; }

    @Override
    public void update(float delta) {
        if (freezeTime > 0) {
            freezeTime -= delta;
            if (freezeTime <= 0) {
                unfreeze();
            }
        }
    }

}

