package myGameEngine;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.BulletStats;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.linearmath.CProfileManager;
import com.bulletphysics.linearmath.ScalarUtil;
import myGameEngine.Helpers.PreInternalTickCallback;
import myGameEngine.Singletons.PhysicsManager;

public class PhysicsWorld extends DiscreteDynamicsWorld {
    protected PreInternalTickCallback preInternalTickCallback;

    public PhysicsWorld(Dispatcher dispatcher, BroadphaseInterface pairCache, ConstraintSolver constraintSolver, CollisionConfiguration collisionConfiguration) {
        super(dispatcher, pairCache, constraintSolver, collisionConfiguration);
    }

    public float getLocalTime() { return this.localTime; }
    public void setLocalTime(float localTime) { this.localTime = localTime; }

    @Override
    public void internalSingleStepSimulation(float timeStep) {
        if (preInternalTickCallback != null) {
            preInternalTickCallback.preInternalTick(this, timeStep);
        }
        super.internalSingleStepSimulation(timeStep);
    }

    public void setPreInternalTickCallback(PreInternalTickCallback cb) {
        this.preInternalTickCallback = cb;
    }

    public void synchronizeMotionStates() {
        super.synchronizeMotionStates();
    }
}
