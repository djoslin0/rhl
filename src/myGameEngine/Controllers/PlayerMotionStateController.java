package myGameEngine.Controllers;

import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Helpers.BulletConvert;
import ray.rage.scene.Node;
import ray.rml.Matrix3f;
import ray.rml.Vector3;

import javax.vecmath.Matrix4f;

public class PlayerMotionStateController extends DefaultMotionState {
    protected Node node;

    public PlayerMotionStateController(Node node) {
        this.node = node;
        Vector3 wp = node.getWorldPosition();
        graphicsWorldTrans.origin.set(wp.x(), wp.y(), wp.z());
        startWorldTrans.origin.set(wp.x(), wp.y(), wp.z());
    }

    @Override
    public void setWorldTransform(Transform transform) {
        super.setWorldTransform(transform);
        node.setLocalPosition(transform.origin.x, transform.origin.y, transform.origin.z);
        node.update(true, true);
    }
}