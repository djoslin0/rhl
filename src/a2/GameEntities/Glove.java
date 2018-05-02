package a2.GameEntities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.GameEntities.LightFade;
import myGameEngine.GameEntities.Particle;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Helpers.MathHelper;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.asset.texture.Texture;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Degreef;
import ray.rml.Radianf;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;

public class Glove extends GameEntity {
    private Entity gloveObj;
    private Entity springObj;
    private Player player;
    private Vector3 target;
    private Vector3 offset;
    private float time;
    private SceneNode node;
    private SceneNode handNode;
    private SceneNode springNode;
    private boolean hit;
    private boolean createPow;
    private boolean wasDead;

    private static float speed = 0.003f;

    public Glove(Player player, String playerName, TextureState textureState, SceneNode handNode) {
        super(true);

        SceneManager sm = EngineManager.getSceneManager();

        try {
            gloveObj = sm.createEntity(playerName + "Glove", "glove.obj");
            addResponsibility(gloveObj);
            gloveObj.setPrimitive(Renderable.Primitive.TRIANGLES);
            gloveObj.setRenderState(textureState);

            springObj = sm.createEntity(playerName + "Spring", "spring.obj");
            addResponsibility(springObj);
            springObj.setPrimitive(Renderable.Primitive.TRIANGLES);

            if (player.isLocal()) {
                ZBufferState zBufferStateGlove = (ZBufferState) sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
                zBufferStateGlove.setSecondaryStage(1);
                gloveObj.setRenderState(zBufferStateGlove);

                ZBufferState zBufferStateSpring = (ZBufferState) sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
                zBufferStateSpring.setSecondaryStage(2);
                springObj.setRenderState(zBufferStateSpring);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        node = sm.getRootSceneNode().createChildSceneNode(playerName + "GloveObjNode");
        addResponsibility(node);

        springNode = sm.getRootSceneNode().createChildSceneNode(playerName + "SpringObjNode");
        addResponsibility(springNode);
        springNode.attachObject(springObj);
        springNode.setLocalScale(0.001f, 0.001f, 0.001f); // hide spring

        this.player = player;
        this.handNode = handNode;
        handNode.attachObject(gloveObj);
    }

    public boolean hasTarget() { return target != null; }
    public float getTime() { return time; }

    public void attack(boolean hit, Vector3 target) {
        boolean hadTarget = (this.target != null);

        this.hit = hit;

        this.target = target;
        this.offset = target.sub(player.getCameraNode().getWorldPosition());

        if (hit) {
            createPow = true;
            if (this.offset.length() > 2f) {
                this.target = target.sub(player.getCameraNode().getWorldForwardAxis().mult(0.25f));
                this.offset = target.sub(player.getCameraNode().getWorldPosition());
            } else {
                this.target = target.sub(player.getCameraNode().getWorldForwardAxis().mult(-0.25f));
                this.offset = target.sub(player.getCameraNode().getWorldPosition());
            }
        }

        time = 0;

        if (!hadTarget) {
            handNode.detachObject(gloveObj);
            node.attachObject(gloveObj);
        }

        node.setLocalPosition(handNode.getWorldPosition());
        node.setLocalRotation(handNode.getWorldRotation());
    }

    @Override
    public void update(float delta) {
        if (player.isDead()) {
            // player head, hide glove
            if (gloveObj.getParentSceneNode() != null) {
                gloveObj.getParentSceneNode().detachObject(gloveObj);
            }
            springNode.setLocalScale(0.001f, 0.001f, 0.001f);
            time = 0;
            target = null;
            wasDead = true;
            return;
        } else if (wasDead) {
            // player respawn, show glove
            wasDead = false;
            handNode.attachObject(gloveObj);
        }

        if (target == null) { return; }

        time += speed * delta;

        if (time > 1) {
            // reattach glove to hand
            target = null;
            node.setLocalPosition(0, 0, 0);
            node.detachObject(gloveObj);
            handNode.attachObject(gloveObj);
            springNode.setLocalScale(0.001f, 0.001f, 0.001f); // hide spring
            return;
        }

        double theta = (1f - Math.pow(1f - time, 4)) * Math.PI;

        Vector3 smoothedTarget = target;
        if (!hit) { smoothedTarget = player.getCameraNode().getWorldPosition().add(offset); }

        double halfPi = Math.PI / 2f;
        if (theta > halfPi) {
            if (hit) {
                float toTargetLerp = (float) ((halfPi - (theta - halfPi)) / halfPi);
                toTargetLerp *= toTargetLerp;
                toTargetLerp *= toTargetLerp;
                smoothedTarget = player.getCameraNode().getWorldPosition().add(offset).lerp(target, toTargetLerp);
            }
            if (createPow) {
                createPow = false;
                try {
                    Particle pow = new Particle(1.5f, 1.5f, target, Vector3f.createZeroVector(), "pow.png", Color.WHITE, 120f);
                    new LightFade(pow.getNode(), new Color(240, 240, 140), 50f, 0.2f, 120f);
                    if (player.isLocal()) {
                        SceneManager sm = EngineManager.getSceneManager();
                        ZBufferState zBufferState = (ZBufferState) sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
                        zBufferState.setWritable(false);
                        zBufferState.setSecondaryStage(3);
                        pow.getManualObject().setRenderState(zBufferState);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        float scalar = (float)Math.sin(theta);
        node.setLocalPosition(MathHelper.lerp(handNode.getWorldPosition(), smoothedTarget, scalar));
        try {
            node.lookAt(smoothedTarget);
            node.pitch(Degreef.createFrom(90));
            node.setLocalRotation(MathHelper.slerp(node.getWorldRotation().toQuaternion(), handNode.getWorldRotation().toQuaternion(), time).toMatrix3());

            springNode.setLocalPosition(node.getWorldPosition());
            springNode.lookAt(handNode);
            springNode.pitch(Degreef.createFrom(90));
            float springDist = springNode.getWorldPosition().sub(handNode.getWorldPosition()).length();
            springNode.setLocalScale(1f, springDist / 1.026404f, 1f);

        } catch (Exception ex) {}

        float size = hit ? (float)(1f + 2f * Math.pow(scalar, 4)) : 1;
        node.setLocalScale(size, size, size);
    }
}

