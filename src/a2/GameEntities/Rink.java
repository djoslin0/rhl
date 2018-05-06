package a2.GameEntities;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;

import a2.MyGame;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3f;
import ray.rml.Quaternion;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.vecmath.Quat4f;
import java.io.IOException;

public class Rink extends GameEntity {
    public Rink() throws IOException {
        super(false);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Rink" + unique;

        node = sm.getRootSceneNode().createChildSceneNode(name + "Node");
        addResponsibility(node);

        Entity rinkGlass = sm.createEntity(name + "Glass", "rink_glass.obj");
        addResponsibility(rinkGlass);
        rinkGlass.setPrimitive(Renderable.Primitive.TRIANGLES);
        ZBufferState zbs = (ZBufferState)sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
        zbs.setWritable(false);
        rinkGlass.setRenderState(zbs);
        node.attachObject(rinkGlass);

        Entity rink = sm.createEntity(name, "rink.obj");
        addResponsibility(rink);
        rink.setPrimitive(Renderable.Primitive.TRIANGLES);
        node.attachObject(rink);

        Entity rinkLines = sm.createEntity(name + "Lines", "rink_lines.obj");
        addResponsibility(rinkLines);
        rinkLines.setPrimitive(Renderable.Primitive.TRIANGLES);
        node.attachObject(rinkLines);

        // add collision boxes
        addSymmetricCollision(Vector3f.createFrom(-30f,0.72f,-50.2f), Vector3f.createFrom(12.0f, 1.6f, 0.25f));
        addSymmetricCollision(Vector3f.createFrom(-26f,0.1f, -57f), Vector3f.createFrom(16f, 1f, 1f));
        addSymmetricCollision(Vector3f.createFrom(-5f, 50.0f, -50.2f), Vector3f.createFrom(5.0f,  50.0f,  0.25f));
        addSymmetricCollision(Vector3f.createFrom(-9.8f, 50.0f, -54f), Vector3f.createFrom(0.25f,  50.0f,  4.0f));
        addSymmetricCollision(Vector3f.createFrom(-26f, 50.0f, -58.2f), Vector3f.createFrom(17f,  50.0f,  0.25f));
        addSymmetricCollision(Vector3f.createFrom(-41.8f, 50.0f, -54f), Vector3f.createFrom(0.25f,  50.0f,  4.0f));
        addSymmetricCollision(Vector3f.createFrom(-66f, 50.0f, -50.2f), Vector3f.createFrom(24.4f,  50.0f,  0.25f));
        addSymmetricCollision(Vector3f.createFrom(-110.2f, 50f, 0f), Vector3f.createFrom(0.25f,  100f,  30f));
        addSymmetricCollision(Vector3f.createFrom(-45.9f, 50f, 50.2f), Vector3f.createFrom(44.5f,  100f,  0.25f));
        addSymmetricCollision(Vector3f.createFrom(-95f, 50f, 48.7f), Vector3f.createFrom(0.25f,  100f,  5.7f), Vector3f.createFrom(0f,  1.29f,  0f));
        addSymmetricCollision(Vector3f.createFrom(-103.6f, 50f, 43.5f), Vector3f.createFrom(0.25f,  100f,  5.5f), Vector3f.createFrom(0f,  0.79f,  0f));
        addSymmetricCollision(Vector3f.createFrom(-108.6f, 50f, 35.0f), Vector3f.createFrom(0.25f,  100f,  6.5f), Vector3f.createFrom(0f,  0.3f,  0f));
        addSymmetricCollision(Vector3f.createFrom(-95f, 50f, -48.7f), Vector3f.createFrom(0.25f,  100f,  5.7f), Vector3f.createFrom(0f,  -1.29f,  0f));
        addSymmetricCollision(Vector3f.createFrom(-103.6f, 50f, -43.5f), Vector3f.createFrom(0.25f,  100f,  5.5f), Vector3f.createFrom(0f,  -0.79f,  0f));
        addSymmetricCollision(Vector3f.createFrom(-108.6f, 50f, -35.0f), Vector3f.createFrom(0.25f,  100f,  6.5f), Vector3f.createFrom(0f,  -0.3f,  0f));

        addCollision(Vector3f.createFrom(0f, -1f, 0f), Vector3f.createFrom(130f, 1f, 60f), Vector3f.createZeroVector());
        addCollision(Vector3f.createFrom(0f, 99f, 0f), Vector3f.createFrom(130f, 1f, 60f), Vector3f.createZeroVector());

        addCollision(Vector3f.createFrom(0f, 50f, 50.2f), Vector3f.createFrom(2f, 50f, 0.25f), Vector3f.createZeroVector(), PhysicsManager.COLLIDE_PUCK);
    }

    @Override
    public boolean shouldRegisterCollision() { return true; }

    private void addSymmetricCollision(Vector3 location, Vector3 scale) {
        addCollision(location, scale, Vector3f.createZeroVector());
        addCollision(location.sub(location.x() * 2, 0, 0), scale, Vector3f.createZeroVector());
    }

    private void addSymmetricCollision(Vector3 location, Vector3 scale, Vector3 rotation) {
        addCollision(location, scale, rotation);
        addCollision(location.sub(location.x() * 2, 0, 0), scale, rotation.sub(0, rotation.y() * 2, 0));
    }

    private void addCollision(Vector3 location, Vector3 scale, Vector3 rotation) {
        addCollision(location, scale, rotation, PhysicsManager.COLLIDE_ALL);
    }
    private void addCollision(Vector3 location, Vector3 scale, Vector3 rotation, short mask) {
        float mass = 0f;

        Transform transform = new Transform();
        transform.setIdentity();
        transform.origin.set(location.toJavaX());
        Quaternion q = Matrix3f.createFrom(rotation.x(), rotation.y(), rotation.z()).toQuaternion();
        Quat4f bulletQ = new Quat4f();
        bulletQ.w = q.w();
        bulletQ.x = q.x();
        bulletQ.y = q.y();
        bulletQ.z = q.z();
        transform.setRotation(bulletQ);
        DefaultMotionState motionState = new DefaultMotionState(transform);

        CollisionShape collisionShape = new BoxShape(scale.toJavaX());
        createBody(mass, motionState, collisionShape, PhysicsManager.COL_WORLD, mask);
    }

    public SceneNode getNode() { return node; }
}

