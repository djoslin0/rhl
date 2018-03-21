package myGameEngine.Singletons;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.broadphase.Dispatcher;
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
import myGameEngine.Controllers.CharacterController;
import myGameEngine.GameEntities.GameEntity;
import ray.rml.Vector3;

import javax.vecmath.Vector3f;
import java.util.ArrayList;

public class PhysicsManager extends InternalTickCallback {
    private static final PhysicsManager instance = new PhysicsManager();

    // this is the most important class
    private static DynamicsWorld world = null;

    // keep the collision shapes, for deletion/cleanup
    private static BroadphaseInterface broadphase;
    private static CollisionDispatcher dispatcher;
    private static ConstraintSolver solver;
    private static DefaultCollisionConfiguration collisionConfiguration;
    private ArrayList<InternalTickCallback> callbacks = new ArrayList<>();
    private ArrayList<RigidBody> registeredCollisions = new ArrayList<>();

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
    }

    public static DynamicsWorld getWorld() {
        return instance.world;
    }

    @Override
    public void internalTick(DynamicsWorld dynamicsWorld, float timeStep) {
        checkCollisions();
        for(InternalTickCallback callback : instance.callbacks) {
            callback.internalTick(dynamicsWorld, timeStep);
        }
    }

    private void checkCollisions() {
        Dispatcher dispatcher = PhysicsManager.getWorld().getDispatcher();
        int manifoldCount = dispatcher.getNumManifolds();
        for (int i = 0; i < manifoldCount; i++) {
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
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

}