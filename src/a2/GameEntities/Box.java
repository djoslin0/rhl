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

public class Box extends GameEntity {
    private SceneNode node;
    private Entity obj;

    public Box(Vector3 location, float scale) throws IOException {
        super(false);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Box" + unique;

        obj = sm.createEntity(name, "cubetilt.obj");
        addResponsibility(obj);
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);

        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);
        node.setLocalPosition(location);
        node.setLocalScale(scale, scale, scale);

        initPhysics();
    }

    private void initPhysics() {
        // NOTE: Re-using the same collision is better for memory usage and performance
        ConvexHullShape colShape = BulletConvert.entityToConvexHullShape(obj);
        colShape.setLocalScaling(node.getLocalScale().toJavaX());

        // Create Dynamic Object
        /*float mass = 100f;// * node.getLocalScale().x();
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0, 0);
        colShape.calculateLocalInertia(mass, localInertia);*/

        float mass = 0f;
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0f, 0);

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
        MotionStateController myMotionState = new MotionStateController(this.node);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setRestitution(0.1f);
        body.setFriction(0.9f);
        body.setDamping(0.05f, 0.05f);

        PhysicsManager.getWorld().addRigidBody(body);
        //body.setActivationState(RigidBody.ISLAND_SLEEPING);
    }

    public String listedName() { return "box"; }
    public SceneNode getNode() { return node; }
}

