package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Helpers.MathHelper;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Degreef;
import ray.rml.Radianf;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;

public class Glove extends GameEntity {
    private Entity obj;
    private Player player;
    private Vector3 target;
    private Vector3 offset;
    private float time;
    private SceneNode node;
    private SceneNode handNode;
    private boolean hit;

    private static float speed = 0.003f;

    public Glove(Player player, String playerName, SceneNode handNode) {
        super(true);

        SceneManager sm = EngineManager.getSceneManager();

        try {
            obj = sm.createEntity(playerName + "Glove", "glove.obj");
            addResponsibility(obj);
            obj.setPrimitive(Renderable.Primitive.TRIANGLES);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        node = sm.getRootSceneNode().createChildSceneNode(playerName + "GloveObjNode");
        addResponsibility(node);

        this.player = player;
        this.handNode = handNode;
        handNode.attachObject(obj);
    }

    public boolean hasTarget() { return target != null; }
    public float getTime() { return time; }

    public void attack(boolean hit, Vector3 target) {
        boolean hadTarget = (this.target != null);

        this.hit = hit;

        if (hit) {
            this.offset = target.sub(player.getPosition());
            this.target = target.sub(offset.normalize().mult(4f));
            this.offset = target.sub(player.getPosition());
        } else {
            this.target = target;
            this.offset = target.sub(player.getPosition());
        }

        time = 0;

        if (!hadTarget) {
            handNode.detachObject(obj);
            node.attachObject(obj);
        }

        node.setLocalPosition(handNode.getWorldPosition());
        node.setLocalRotation(handNode.getWorldRotation());
    }

    @Override
    public void update(float delta) {
        if (target == null) { return; }
        time += speed * delta;

        if (time > 1) {
            target = null;
            node.setLocalPosition(0, 0, 0);
            node.detachObject(obj);
            handNode.attachObject(obj);
            return;
        }

        double theta = (1f - Math.pow(1f - time, 4)) * Math.PI;
        float scalar = (float)Math.sin(theta);
        //node.setLocalPosition(MathHelper.lerp(handNode.getWorldPosition(), target, scalar));
        Vector3 smoothedTarget = player.getPosition().add(offset).lerp(target, 0.2f);
        node.setLocalPosition(MathHelper.lerp(handNode.getWorldPosition(), smoothedTarget, scalar));
        try {
            node.lookAt(smoothedTarget);
            node.pitch(Degreef.createFrom(90));
            node.setLocalRotation(MathHelper.slerp(node.getWorldRotation().toQuaternion(), handNode.getWorldRotation().toQuaternion(), time).toMatrix3());
        } catch (Exception ex) {}

        float size = hit ? (float)(1f + 2f * Math.pow(scalar, 4)) : 1;
        node.setLocalScale(size, size, size);
    }

}

