package myGameEngine;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;

public class PhysicsWorld extends DiscreteDynamicsWorld {
    protected InternalTickCallback preInternalTickCallback;

    public PhysicsWorld(Dispatcher dispatcher, BroadphaseInterface pairCache, ConstraintSolver constraintSolver, CollisionConfiguration collisionConfiguration) {
        super(dispatcher, pairCache, constraintSolver, collisionConfiguration);
    }

    @Override
    protected void internalSingleStepSimulation(float timeStep) {
        if (preInternalTickCallback != null) {
            preInternalTickCallback.internalTick(this, timeStep);
        }
        super.internalSingleStepSimulation(timeStep);
    }

    public void setPreInternalTickCallback(InternalTickCallback cb) {
        this.preInternalTickCallback = cb;
    }
}
