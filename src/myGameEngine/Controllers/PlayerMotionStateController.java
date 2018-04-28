package myGameEngine.Controllers;

import a2.GameEntities.Player;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Helpers.BulletConvert;
import ray.rage.scene.Node;
import ray.rml.Matrix3f;
import ray.rml.Vector3;

import javax.vecmath.Matrix4f;

public class PlayerMotionStateController extends DefaultMotionState {
    private Player player;
    protected Node node;

    public PlayerMotionStateController(Player player, Node node) {
        this.player = player;
        this.node = node;
        Vector3 wp = node.getWorldPosition();
        graphicsWorldTrans.origin.set(wp.x(), wp.y(), wp.z());
        startWorldTrans.origin.set(wp.x(), wp.y(), wp.z());
    }

    @Override
    public void setWorldTransform(Transform transform) {
        super.setWorldTransform(transform);
        if (player.isDead()) { return; }
        node.setLocalPosition(transform.origin.x, transform.origin.y, transform.origin.z);
        node.update(true, true);
    }
}