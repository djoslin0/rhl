package a2.GameEntities;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.io.IOException;

public class Puck extends GameEntity {
    private SceneNode node;
    private Entity obj;

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

        initPhysics();
    }

    private void initPhysics() {
        // NOTE: Re-using the same collision is better for memory usage and performance
        ConvexHullShape colShape = BulletConvert.entityToConvexHullShape(obj);
        colShape.setLocalScaling(node.getLocalScale().toJavaX());

        // Create Dynamic Object
        float mass = 1000f;

        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0, 0);
        colShape.calculateLocalInertia(mass, localInertia);

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        MotionStateController myMotionState = new MotionStateController(this.node);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setRestitution(0.6f);
        body.setFriction(0.2f);
        body.setDamping(0.05f, 0f);

        body.setUserPointer(this);
        PhysicsManager.getWorld().addRigidBody(body);
    }

    public String listedName() { return "puck"; }
    public SceneNode getNode() { return node; }

    @Override
    public void update(float delta) {
    }

}

