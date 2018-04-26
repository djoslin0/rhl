package a2.GameEntities;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;

import a2.MyGame;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.io.IOException;

public class Box extends GameEntity {
    private Entity obj;

    public Box(Vector3 location, float scale) throws IOException {
        super(false);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Box" + unique;

        obj = sm.createEntity(name, MyGame.playMode ? "cube.obj" : "cubetilt.obj");
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
        float mass = 0f;
        MotionStateController motionState = new MotionStateController(this.node);
        ConvexHullShape collisionShape = BulletConvert.entityToConvexHullShape(obj);
        collisionShape.setLocalScaling(node.getLocalScale().toJavaX());

        RigidBody body = createBody(mass, motionState, collisionShape);
        body.setRestitution(0.1f);
        body.setFriction(0.9f);
        body.setDamping(0.05f, 0.05f);
    }

    public String listedName() { return "box"; }
    public SceneNode getNode() { return node; }
}

