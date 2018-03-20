package a2.GameEntities;

import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.controllers.RotationController;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;
import java.util.Random;

public class Asteroid extends GameEntity {
    private SceneNode node;
    private float scale;
    private float speed;
    private float size;
    private boolean outOfBounds = false;
    private long uniqueCount;

    public Asteroid(SceneNode parent, Vector3 location, float speed, float size) throws IOException {
        SceneManager sm = EngineManager.getSceneManager();
        uniqueCount = UniqueCounter.next();
        String name = "Asteroid" + uniqueCount;
        Random rnd = new Random(uniqueCount);
        this.speed = rnd.nextFloat() + speed;
        this.size = size;

        // create model of random asteroid [1, 2, 3]
        Entity obj = sm.createEntity(name, "asteroid" + (rnd.nextInt(3) + 1) +".obj");
        addResponsibility(obj);
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);

        // create base asteroid node
        node = parent.createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.setLocalPosition(location);

        // random orientation for movement
        node.rotate(Degreef.createFrom(rnd.nextFloat() * 360f), Vector3f.createUnitVectorX());
        node.rotate(Degreef.createFrom(rnd.nextFloat() * 360f), Vector3f.createUnitVectorY());
        node.rotate(Degreef.createFrom(rnd.nextFloat() * 360f), Vector3f.createUnitVectorZ());
        node.moveForward(10f * size);

        // rotate around a random axis
        SceneNode rotationNode = node.createChildSceneNode(obj.getName() + "RotationNode");
        addResponsibility(rotationNode);
        rotationNode.attachObject(obj);
        Vector3 rotationAxis = Vector3f.createFrom(rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f).normalize();
        RotationController rc = new RotationController(rotationAxis, rnd.nextFloat() * 0.02f + 0.01f);
        rc.addNode(rotationNode);
        sm.addController(rc);

        scale = (rnd.nextFloat() * 20f + 10f) * size;
        rotationNode.setLocalScale(scale, scale, scale);
    }

    // listedName causes EntityManager to keep track of this entity in a named list
    public String listedName() { return "asteroid"; }

}
