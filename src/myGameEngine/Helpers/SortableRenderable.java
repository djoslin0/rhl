package myGameEngine.Helpers;

import ray.rage.rendersystem.Renderable;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.FloatBuffer;

// SortableRenderable allows the RenderManager to draw transparent objects in the correct order
public class SortableRenderable implements Comparable<SortableRenderable> {
    public Renderable renderable;
    public float dist;
    public SortableRenderable(Renderable renderable, Vector3 cameraPosition) {
        this.renderable = renderable;

        FloatBuffer vBuffer = renderable.getVertexBuffer();
        dist = Float.MAX_VALUE;
        for (int i = 0; i < vBuffer.limit(); i += 3) {
            Vector3 vPosition = Vector3f.createFrom(vBuffer.get(i + 0), vBuffer.get(i + 1), vBuffer.get(i + 2));
            dist = Math.max(dist, cameraPosition.sub(vPosition).length());
        }
    }
    public int compareTo(SortableRenderable sr) {
        return (this.dist < sr.dist) ? -1 : 1;
    }
}
