package a2.GameEntities;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;

public class Goal extends GameEntity {
    private Entity obj;
    private RigidBody body;

    public Goal(int side) throws IOException {
        super(false);
        //insantiate nodes
        SceneManager sm = EngineManager.getSceneManager();
        node = sm.getRootSceneNode().createChildSceneNode("Goal" + side);
        node.scale(Vector3f.createFrom(2f,4f,2f));
        //        //load model
        obj = sm.createEntity("Goalentity"+side, "goal.obj");
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);
        //attach object to goals
        node.attachObject(obj);
        // wich side are you creating the goal on?
        if(side ==0){
            node.setLocalPosition(50f,0f,0f);
        }else{
            node.setLocalPosition(-50f,0f,0f);
        }


        // set initial locations
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
    public boolean registerCollisions() { return true; }
}
