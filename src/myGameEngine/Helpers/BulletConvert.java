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

    public static Matrix3 jxMatrix4fToMatrix3(javax.vecmath.Matrix4f mat4) {
        float[] rot = new float[] {
                mat4.m00, mat4.m10, mat4.m20,
                mat4.m01, mat4.m11, mat4.m21,
                mat4.m02, mat4.m12, mat4.m22
        };
        return Matrix3f.createFrom(rot);
    }
}
