package a3.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Helpers.Duration;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Quaternion;
import ray.rml.Vector3;

import javax.vecmath.Quat4f;
import java.io.IOException;

public class Debris extends GameEntity {
    private Entity obj;
    private RigidBody body;
    private ConvexHullShape collisionShape;
    private Duration duration;

    public Debris(Vector3 location, Quaternion rotation, Vector3 velocity, String modelName, TextureState textureState, float maxDuration) throws IOException {
        super(true);

        this.duration = new Duration(maxDuration);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Debris" + modelName + unique;

        obj = sm.createEntity(name, modelName);
        //addResponsibility(obj); since this uses the player's texture, do not remove this obj! (TODO: check to see if we're polluting the game with extra models)
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);
        obj.setRenderState(textureState);

        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);
        node.setLocalPosition(location);

        initPhysics(location, rotation, velocity);
    }

    private void initPhysics(Vector3 location, Quaternion rotation, Vector3 velocity) {
        float mass = 100f;
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
        t.origin.x = location.x();
        t.origin.y = location.y();
        t.origin.z = location.z();

        Quat4f rot = new Quat4f();
        rot.w = rotation.w();
        rot.x = rotation.x();
        rot.y = rotation.y();
        rot.z = rotation.z();
        t.setRotation(rot);
        body.setWorldTransform(t);
        body.setLinearVelocity(velocity.toJavaX());
    }

    public SceneNode getNode() { return node; }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (duration.exceeded(delta)) {
            // ran out of life
            destroy();
            return;
        }

        // resize
        float scalar = 1f - (float)Math.pow(duration.progress(), 2f);
        node.setLocalScale(scalar, scalar, scalar);
        collisionShape.setLocalScaling(new javax.vecmath.Vector3f(scalar, scalar, scalar));
    }

}

