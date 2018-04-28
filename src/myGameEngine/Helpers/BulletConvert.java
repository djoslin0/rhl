package myGameEngine.Helpers;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.util.ObjectArrayList;
import ray.rage.asset.mesh.SubMesh;
import ray.rage.scene.Entity;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Vector3;

import javax.vecmath.Vector3f;
import java.nio.FloatBuffer;

public class BulletConvert {

    public static ConvexHullShape entityToConvexHullShape(Entity obj) {
        ObjectArrayList<javax.vecmath.Vector3f> vertices = new ObjectArrayList<>();
        for(SubMesh submesh : obj.getMesh().getSubMeshes()) {
            FloatBuffer vb = submesh.getVertexBuffer();
            for (int i = 0; i < vb.limit(); i += 3) {
                vertices.add(new javax.vecmath.Vector3f(vb.get(i), vb.get(i+1), vb.get(i+2)));
            }
        }
        return new ConvexHullShape(vertices);
    }
}
