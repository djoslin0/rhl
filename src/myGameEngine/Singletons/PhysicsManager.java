package myGameEngine.Singletons;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import myGameEngine.Controllers.CharacterController;

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
        for(InternalTickCallback callback : instance.callbacks) {
            callback.internalTick(dynamicsWorld, timeStep);
        }
    }

    public static void addCallback(InternalTickCallback callback) {
        instance.callbacks.add(callback);
    }
}