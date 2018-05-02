package myGameEngine.Helpers;

import myGameEngine.Singletons.EngineManager;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.Camera;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.FloatBuffer;

// SortableRenderable allows the RenderManager to draw transparent objects in the correct order
public class SortableSecondary implements Comparable<SortableSecondary> {
    public Renderable renderable;
    public int layer = 0;

    public SortableSecondary(Renderable renderable) {
        this.renderable = renderable;
        ZBufferState zbs = (ZBufferState)renderable.getRenderState(RenderState.Type.ZBUFFER);
        if (zbs != null) {
            layer = zbs.getSecondaryStage();
        }
    }
    public int compareTo(SortableSecondary sr) {
        return (this.layer < sr.layer) ? -1 : 1;
    }
}
