package myGameEngine.Controllers;

import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Helpers.BulletConvert;
import ray.rage.scene.Node;
import ray.rml.*;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;

public class MotionStateController extends DefaultMotionState {
    protected Node node;

    public MotionStateController(Node node) {
        this.node = node;
        Vector3 wp = node.getWorldPosition();
        graphicsWorldTrans.origin.set(wp.x(), wp.y(), wp.z());
        startWorldTrans.origin.set(wp.x(), wp.y(), wp.z());
    }

    @Override
    public void setWorldTransform(Transform transform) {
        super.setWorldTransform(transform);
        node.setLocalPosition(transform.origin.x, transform.origin.y, transform.origin.z);

        // rotation
        Quat4f jq = new Quat4f();
        transform.getRotation(jq);
        Quaternion q = Quaternionf.createFrom(jq.w, jq.x, jq.y, jq.z);
        node.setLocalRotation(q.toMatrix3());

        node.update(true, true);
    }
}
