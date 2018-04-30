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
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.Settings;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;

public class Goal extends GameEntity {
    private Entity obj;
    private RigidBody body;
    private SceneNode backCollisionBox;
    private SceneNode lSideCollisionBox;
    private SceneNode rSideCollisionBox;
    private SceneNode tRailCollisionBox;
    private SceneNode goalBox;

    public Goal(int side) throws IOException {
        super(false);
        // instantiate nodes
        SceneManager sm = EngineManager.getSceneManager();
        node = sm.getRootSceneNode().createChildSceneNode("Goal" + side);

        // load model
        obj = sm.createEntity("GoalEntity" + side, "goal.obj");
        node.rotate(Degreef.createFrom(90),Vector3f.createUnitVectorY());
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);

        // attach object to goals
        node.attachObject(obj);
        backCollisionBox = sm.getRootSceneNode().createChildSceneNode("Back" +side);
        lSideCollisionBox = sm.getRootSceneNode().createChildSceneNode("Left" +side);
        rSideCollisionBox = sm.getRootSceneNode().createChildSceneNode("Right" +side);
        tRailCollisionBox = sm.getRootSceneNode().createChildSceneNode("Rail" +side);
        goalBox = sm.getRootSceneNode().createChildSceneNode("Goalbox" + side);
        // which side are you creating the goal on?
        float goalDistance = Settings.get().goalDistance.floatValue();
        if(side == 0){
            backCollisionBox.setLocalPosition(goalDistance+2.0f,2.5f,0f);
            lSideCollisionBox.setLocalPosition(goalDistance-2.2f,2.5f,8f);
            rSideCollisionBox.setLocalPosition(goalDistance-2.2f,2.5f,-8f);
            tRailCollisionBox.setLocalPosition(goalDistance-6f,4.7f,0f);
            goalBox.setLocalPosition(goalDistance,0f,0f);
            node.setLocalPosition(goalDistance,0f,0f);
        }else{
            backCollisionBox.setLocalPosition(-goalDistance-2.0f,2.5f,0f);
            lSideCollisionBox.setLocalPosition(-goalDistance+2.2f,2.5f,8f);
            rSideCollisionBox.setLocalPosition(-goalDistance+2.2f,2.5f,-8f);
            tRailCollisionBox.setLocalPosition(-goalDistance+6f,4.7f,0f);
            goalBox.setLocalPosition(-goalDistance,0f,0f);
            node.setLocalPosition(-goalDistance,0f,0f);
            node.rotate(Degreef.createFrom(180f),Vector3f.createUnitVectorY());
        }

        // create physics
        initPhysics();

    }

    private void initPhysics() {
        float mass = 0f;

        // motion states for collision boxes
        MotionStateController backMotionState = new MotionStateController(backCollisionBox);
        MotionStateController leftMotionState = new MotionStateController(lSideCollisionBox);
        MotionStateController rightMotionState = new MotionStateController(rSideCollisionBox);
        MotionStateController railMotionState = new MotionStateController(tRailCollisionBox);
        MotionStateController goalBoxMotionState = new MotionStateController(goalBox);

        // collision shapes
        BoxShape backCollisionShape = new BoxShape(new javax.vecmath.Vector3f(0.2f,2.6f,+8.2f));
        BoxShape leftCollisionShape = new BoxShape(new javax.vecmath.Vector3f(4.0f,2.6f,+0.2f));
        BoxShape rightCollisionShape = new BoxShape(new javax.vecmath.Vector3f(4.0f,2.6f,+0.2f));
        BoxShape tRailCollisionShape = new BoxShape(new javax.vecmath.Vector3f(0.2f,0.2f,+8.2f));
        BoxShape goalBoxCollisionShape = new BoxShape(new javax.vecmath.Vector3f(0.9f,1.5f,+7.9f));

        // rigid bodies
        RigidBody backBody = createBody(mass, backMotionState, backCollisionShape, PhysicsManager.COL_WORLD, PhysicsManager.COLLIDE_ALL);
        RigidBody leftBody = createBody(mass, leftMotionState, leftCollisionShape, PhysicsManager.COL_WORLD, PhysicsManager.COLLIDE_ALL);
        RigidBody rightBody = createBody(mass, rightMotionState, rightCollisionShape, PhysicsManager.COL_WORLD, PhysicsManager.COLLIDE_ALL);
        RigidBody railBody= createBody(mass, railMotionState, tRailCollisionShape, PhysicsManager.COL_WORLD, PhysicsManager.COLLIDE_ALL);
        RigidBody goalBoxBody= createBody(mass, goalBoxMotionState, goalBoxCollisionShape, PhysicsManager.COL_WORLD, PhysicsManager.COLLIDE_ALL);


        // unregister collisions
        PhysicsManager.unregisterCollision(backBody);
        PhysicsManager.unregisterCollision(leftBody);
        PhysicsManager.unregisterCollision(rightBody);
        PhysicsManager.unregisterCollision(railBody);

        // body physics vars
        backBody.setRestitution(0.1f);
        backBody.setFriction(0.9f);
        backBody.setDamping(0.05f, 0.05f);

        leftBody.setRestitution(0.1f);
        leftBody.setFriction(0.9f);
        leftBody.setDamping(0.05f, 0.05f);

        rightBody.setRestitution(0.1f);
        rightBody.setFriction(0.9f);
        rightBody.setDamping(0.05f, 0.05f);

        railBody.setRestitution(0.1f);
        railBody.setFriction(0.9f);
        railBody.setDamping(0.05f, 0.05f);

    }

    public boolean shouldRegisterCollision() { return true; }
}
