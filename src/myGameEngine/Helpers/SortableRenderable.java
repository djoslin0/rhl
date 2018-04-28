package myGameEngine.Helpers;

import myGameEngine.Singletons.EngineManager;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Camera;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.FloatBuffer;

// SortableRenderable allows the RenderManager to draw transparent objects in the correct order
public class SortableRenderable implements Comparable<SortableRenderable> {
    public Renderable renderable;
    public float dist;
    public SortableRenderable(Renderable renderable) {
        this.renderable = renderable;

        for (Camera c : EngineManager.getSceneManager().getCameras()) {
            Vector3 vPosition = renderable.getWorldTransformMatrix().column(3).toVector3();
            dist = c.getParentSceneNode().getWorldPosition().sub(vPosition).lengthSquared();
            break;
        }

    }
    public int compareTo(SortableRenderable sr) {
        return (this.dist > sr.dist) ? -1 : 1;
    }
}
