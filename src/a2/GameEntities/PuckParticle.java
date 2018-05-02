package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Helpers.Duration;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.*;

import javax.vecmath.Quat4f;
import java.io.IOException;

public class PuckParticle extends GameEntity{
    private Entity obj;
    private RigidBody body;
    private ConvexHullShape collisionShape;
    private Duration duration = new Duration(4000f);
    private boolean physics = false;
    private int rotate;

    public PuckParticle(int rotate) throws IOException {
        super(true);

        this.rotate = rotate;

        String name = "puckParticle" + UniqueCounter.next();

        obj = EngineManager.getSceneManager().createEntity(name,"puckpartical.obj");
        addResponsibility(obj);

        node = EngineManager.getSceneManager().getRootSceneNode().createChildSceneNode(name);
        addResponsibility(node);
        node.attachObject(obj);

        initPhysics();
        setState();

        // make it tiny (invisible) to start with
        node.setLocalScale(0.001f, 0.001f, 0.001f);
        collisionShape.setLocalScaling(new javax.vecmath.Vector3f(0.001f, 0.001f, 0.001f));
    }

    private void setState() {
        collisionShape.setLocalScaling(new javax.vecmath.Vector3f(1, 1, 1));
        node.setLocalScale(1, 1, 1);
        node.setLocalRotation(Matrix3f.createIdentityMatrix());
        node.rotate(Degreef.createFrom(45f * rotate), Vector3f.createUnitVectorY());

        Transform t = new Transform();
        Vector3 worldPosition = EntityManager.getPuck().getNode().getWorldPosition();
        t.origin.x = worldPosition.x();
        t.origin.y = worldPosition.y();
        t.origin.z = worldPosition.z();

        Quat4f rot = new Quat4f();
        Quaternion nodeQ = node.getWorldRotation().toQuaternion();
        rot.w = nodeQ.w();
        rot.x = nodeQ.x();
        rot.y = nodeQ.y();
        rot.z = nodeQ.z();
        t.setRotation(rot);
        body.setWorldTransform(t);
        body.setLinearVelocity(node.getWorldForwardAxis().mult(60f).toJavaX());
    }

    public void startPhysics() {
        physics = true;
        setState();
    }

    private void initPhysics() {
        float mass = 1000f;
        MotionStateController motionState = new MotionStateController(this.node);
        collisionShape = BulletConvert.entityToConvexHullShape(obj);
        collisionShape.setLocalScaling(node.getLocalScale().toJavaX());

        body = createBody(mass, motionState, collisionShape, PhysicsManager.COL_DEBRIS, PhysicsManager.COLLIDE_DEBRIS);
        body.setRestitution(0.6f);
        body.setFriction(0.2f);
        body.setDamping(0.05f, 0f);
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
    }

    public void update(float delta) {
        if(physics) {
            super.update(delta);
            if(duration.exceeded(delta)){
                physics = false;
                duration = new Duration(4000f);
                return;
            }
            float scalar = 1f - (float)Math.pow(duration.progress(), 2f);
            node.setLocalScale(scalar, scalar, scalar);
            collisionShape.setLocalScaling(new javax.vecmath.Vector3f(scalar, scalar, scalar));
        }
    }
}
