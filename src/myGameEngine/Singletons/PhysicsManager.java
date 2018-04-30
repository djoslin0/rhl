package myGameEngine.Singletons;

import com.bulletphysics.collision.broadphase.*;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.Updatable;

import javax.vecmath.Vector3f;
import java.util.ArrayList;

public class PhysicsManager extends InternalTickCallback implements Updatable {
    private static final PhysicsManager instance = new PhysicsManager();
    public static int tickRate = 144;

    // this is the most important class
    private static DiscreteDynamicsWorld world = null;

    // keep the collision shapes, for deletion/cleanup
    private static BroadphaseInterface broadphase;
    private static CollisionDispatcher dispatcher;
    private static ConstraintSolver solver;
    private static DefaultCollisionConfiguration collisionConfiguration;
    private ArrayList<InternalTickCallback> callbacks = new ArrayList<>();
    private ArrayList<RigidBody> registeredCollisions = new ArrayList<>();
    private ArrayList<RigidBody> rigidBodies = new ArrayList<>();

    public static short COL_LOCAL_PLAYER = (short)(1 << 2);
    public static short COL_WORLD = (short)(1 << 3);
    public static short COL_DEBRIS = (short)(1 << 4);
    public static short COL_PUCK = (short)(1 << 5);
    public static short COLLIDE_ALL = (short)(-1);
    public static short COLLIDE_DEFAULT = (short)(COLLIDE_ALL ^ COL_DEBRIS);
    public static short COLLIDE_WORLD = COL_WORLD;
    public static short COLLIDE_DEBRIS = (short)(COL_WORLD | COL_DEBRIS);
    public static short COLLIDE_IGNORE_LOCAL_PLAYER = (short)(COLLIDE_DEFAULT ^ COL_LOCAL_PLAYER);
    public static short COLLIDE_PUCK = COL_PUCK;

    public static void initPhysics() {

        // collision configuration contains default setup for memory, collision setup
        instance.collisionConfiguration = new DefaultCollisionConfiguration();

        // use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
        instance.dispatcher = new CollisionDispatcher(instance.collisionConfiguration);

        instance.broadphase = new DbvtBroadphase();

        // the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
        SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
        instance.solver = sol;

        // TODO: needed for SimpleDynamicsWorld
        //sol.setSolverMode(sol.getSolverMode() & ~SolverMode.SOLVER_CACHE_FRIENDLY.getMask());

        instance.world = new DiscreteDynamicsWorld(instance.dispatcher, instance.broadphase, instance.solver, instance.collisionConfiguration);

        instance.world.setGravity(new Vector3f(0f, -20f, 0f));
        instance.world.setInternalTickCallback(instance, null);

        UpdateManager.add(instance);
    }

    public static DiscreteDynamicsWorld getWorld() {
        return instance.world;
    }

    public static void addRigidBody(RigidBody rigidBody) {
        if (!instance.rigidBodies.contains(rigidBody)) {
            instance.rigidBodies.add(rigidBody);
        }
        instance.world.addRigidBody(rigidBody);
    }

    public static void addRigidBody(RigidBody rigidBody, short group, short mask) {
        if (!instance.rigidBodies.contains(rigidBody)) {
            instance.rigidBodies.add(rigidBody);
        }
        instance.world.addRigidBody(rigidBody, group, mask);
    }

    public static void removeRigidBody(RigidBody rigidBody) {
        instance.rigidBodies.remove(rigidBody);
        instance.world.removeRigidBody(rigidBody);
    }

    public static ArrayList<RigidBody> getRigidBodies() { return instance.rigidBodies; }

    @Override
    public void internalTick(DynamicsWorld world, float timeStep) {
        TimeManager.incrementTick();
        for(InternalTickCallback callback : (ArrayList<InternalTickCallback>)instance.callbacks.clone()) {
            callback.internalTick(world, timeStep);
        }
        checkCollisions();
    }

    private void checkCollisions() {
        Dispatcher dispatcher = PhysicsManager.getWorld().getDispatcher();
        int manifoldCount = dispatcher.getNumManifolds();
        for (int i = 0; i < manifoldCount; i++) {
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
            if (manifold == null) { continue; }
            RigidBody object1 = (RigidBody)manifold.getBody0();
            RigidBody object2 = (RigidBody)manifold.getBody1();
            if (!registeredCollisions.contains(object1)) { continue; }
            if (!registeredCollisions.contains(object2)) { continue; }

            for (int j = 0; j < manifold.getNumContacts(); j++) {
                ManifoldPoint contactPoint = manifold.getContactPoint(j);
                if (contactPoint.getDistance() < 0) {
                    ((GameEntity)object1.getUserPointer()).collision((GameEntity) object2.getUserPointer(), contactPoint, true);
                    ((GameEntity)object2.getUserPointer()).collision((GameEntity) object1.getUserPointer(), contactPoint, false);
                }
            }
        }
    }

    public static void addCallback(InternalTickCallback callback) {
        instance.callbacks.add(callback);
    }

    public static void registerCollision(RigidBody body) {
        instance.registeredCollisions.add(body);
    }

    public static void unregisterCollision(RigidBody body) {
        instance.registeredCollisions.remove(body);
    }

    @Override
    public void update(float delta) {
        stepSimulation(delta / 1000f);
    }

    public static void stepSimulation(float timeStep) {
        instance.world.stepSimulation(timeStep, 144, 1f / (float)PhysicsManager.tickRate);
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }

}