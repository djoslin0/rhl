package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.ConvexConcaveCollisionAlgorithm;
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
import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.Degreef;
import ray.rml.Quaternion;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.vecmath.Quat4f;
import java.io.IOException;

public class PuckPartical extends GameEntity{
    private Entity obj;
    private RigidBody body;
    private ConvexHullShape collisionShape;
    private Duration duration = new Duration(3000f);
    public PuckPartical(String name,int rotate) throws IOException {
        super(true);
            obj = EngineManager.getSceneManager().createEntity(name,"puckpartical.obj");
            node = EngineManager.getSceneManager().getRootSceneNode().createChildSceneNode(name);
            node.setLocalPosition(EntityManager.getPuck().getNode().getWorldPosition());
            node.setLocalRotation(EntityManager.getPuck().getNode().getWorldRotation());
            node.rotate(Degreef.createFrom(45f*rotate), Vector3f.createUnitVectorY());
            node.attachObject(obj);
            addResponsibility(node);
            addResponsibility(obj);
            initPhysics();
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

        Transform t = new Transform();
        body.getWorldTransform(t);
        t.origin.x = EntityManager.getPuck().getNode().getWorldPosition().x();
        t.origin.y = EntityManager.getPuck().getNode().getWorldPosition().y();
        t.origin.z = EntityManager.getPuck().getNode().getWorldPosition().z();
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
    public void update(float delta){
        super.update(delta);
        if(duration.exceeded(delta)){
            destroy();
            return;
        }
    }
}
