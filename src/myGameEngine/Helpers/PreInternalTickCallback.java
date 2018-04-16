package myGameEngine.Helpers;

import com.bulletphysics.dynamics.DynamicsWorld;

public interface PreInternalTickCallback {
    void preInternalTick(DynamicsWorld world, float timeStep);
}
