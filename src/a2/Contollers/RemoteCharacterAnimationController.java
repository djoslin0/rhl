package a2.Contollers;

import a2.GameEntities.Player;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;
import ray.rml.*;

public class RemoteCharacterAnimationController implements Updatable, CharacterAnimationController {
    private Player player;
    private CharacterController controller;
    private String lastAnimationTag;
    private Vector3 knockDirection = null;
    private float knockAmount = 0;
    private boolean knockReached = true;

    public RemoteCharacterAnimationController(Player player, CharacterController controller) {
        this.player = player;
        this.controller = controller;
        UpdateManager.add(this);
    }

    private void animate(String tag, String animName, float animSpeed, SkeletalEntity.EndType endType, boolean interruptable) {
        if (lastAnimationTag == tag) { return; }
        lastAnimationTag = tag;
        SkeletalEntity robo = player.getRobo();
        robo.playAnimation(animName, animSpeed, endType, 0, interruptable);
    }

    public void knock(Vector3 vec, Vector3 relative) {
        vec = Vector3f.createFrom(vec.x(), 0, vec.z());
        if (vec.length() == 0) { vec = Vector3f.createFrom(1, 0, 0); }
        knockDirection = vec.normalize().rotate(Radianf.createFrom(-player.getYaw()), Vector3f.createUnitVectorY());
        if (relative.y() >= 0.5f) { knockDirection = knockDirection.mult(-1f); }
        knockReached = false;
    }

    private void updateKnockPitch(float delta) {
        // add knockback
        SkeletalEntity robo = player.getRobo();
        robo.removeRotationAdditive("torso");
        robo.removeRotationOverride("shoulder_L");
        robo.removeRotationOverride("shoulder_R");
        robo.removeRotationOverride("hip_R");
        robo.removeRotationOverride("hip_L");
        robo.removeLocationAdditive("torso");

        float speedForward = 0.015f;
        float speedBackward = 0.002f;

        if (knockReached) {
            // we reached our peak and have begun decreasing
            boolean knockAmountSign = (knockAmount >= 0);
            knockAmount -= speedBackward * delta;
            if ((knockAmount >= 0) != knockAmountSign) {
                // we crossed knockAmount = 0
                knockDirection = null;
                return;
            }
        } else {
            // we are approaching our peak
            knockAmount += speedForward * delta;
            if (Math.abs(knockAmount) >= 1f) {
                // we have reached our peak, decrease afterward
                knockReached = true;
            }
        }

        // get foot floor location
        Vector3 originalFloor = getFloorOffset();

        float roll = 0.5f * knockAmount * knockDirection.x();
        float pitch = knockAmount * knockDirection.z();

        // rotations
        Matrix3 pz = Matrix3f.createFrom2(roll, roll, pitch);
        robo.addRotationAdditive("torso", pz.toQuaternion());

        Matrix3 pny = Matrix3f.createFrom(roll, -pitch, roll);
        robo.addRotationOverride("shoulder_L", pny.toQuaternion());
        robo.addRotationOverride("shoulder_R", pny.toQuaternion());
        robo.addRotationOverride("hip_R", pny.toQuaternion());

        Matrix3 py = Matrix3f.createFrom(-roll, pitch, -roll);
        robo.addRotationOverride("hip_L", py.toQuaternion());

        // apply new offset
        Vector3 newFloor = getFloorOffset();
        robo.addLocationAdditive("torso", originalFloor.sub(newFloor));

    }

    private Vector3 getFloorOffset() {
        SkeletalEntity robo = player.getRobo();
        Vector3 torsoLocation = robo.getBoneModelTransform("torso").column(3).toVector3();
        Vector3 footLocationL = robo.getBoneModelTransform("foot_L").column(3).toVector3();
        Vector3 footLocationR = robo.getBoneModelTransform("foot_R").column(3).toVector3();
        return footLocationL.add(footLocationR).div(2f).sub(torsoLocation);
    }

    private void updateBones(float delta) {
        SceneNode cameraNode = player.getCameraNode();
        SkeletalEntity robo = player.getRobo();

        if (robo != null) {
            // align head to camera pitch
            robo.addRotationOverride("head", cameraNode.getLocalRotation().toQuaternion());

            // align arm to camera pitch
            float pitch_upper = (float) (cameraNode.getLocalRotation().getPitch() / 2f - Math.PI / 3.5f);
            float pitch_lower = (float) (cameraNode.getLocalRotation().getPitch() / 1.9f + Math.PI / -2.5f);

            if (player.getGlove().hasTarget()) {
                float attackScale = 1f - player.getGlove().getTime();
                attackScale *= attackScale;
                attackScale = 1f - attackScale;
                pitch_upper += Math.sin(attackScale * Math.PI) * 2f;
                pitch_lower -= Math.sin(attackScale * Math.PI) * 0.5f;
            }
            Matrix3 armUpper = Matrix3f.createFrom(0, 0, pitch_upper);
            robo.addRotationOverride("arm_upper_R", armUpper.toQuaternion());

            Matrix3 armLower = Matrix3f.createFrom(pitch_lower, 0, 0);
            robo.addRotationOverride("arm_lower_R", armLower.toQuaternion());

            if (knockDirection != null) {
                updateKnockPitch(delta);
            }
        }
    }

    private void updateAnimations(float delta) {
        SkeletalEntity robo = player.getRobo();

        if (controller.clearJumpQueue()) {
            animate("jump", "jump", 0.04f, SkeletalEntity.EndType.NONE, false);
            return;
        }

        if (!controller.isOnGround()) {
            animate("falling", "falling", 0.025f, SkeletalEntity.EndType.LOOP, true);
            return;
        }

        if (controller.isOnGround() && !controller.wasOnGround()) {
            animate("land", "land", 0.025f, SkeletalEntity.EndType.NONE, false);
            return;
        }

        if (controller.isCrouching()) {
            if (controller.isMovingForward() && !controller.isMovingBackward()) {
                animate("crouch_walk_forward", "crouch_walk", 0.1f, SkeletalEntity.EndType.LOOP, true);
            } else if (controller.isMovingBackward() && !controller.isMovingForward()) {
                animate("crouch_walk_backward", "crouch_walk", -0.1f, SkeletalEntity.EndType.LOOP, true);
            } else if (controller.isMovingRight() && !controller.isMovingLeft()) {
                animate("crouch_sidestep_right", "crouch_sidestep", 0.12f, SkeletalEntity.EndType.LOOP, true);
            } else if (controller.isMovingLeft() && !controller.isMovingRight()) {
                animate("crouch_sidestep_left", "crouch_sidestep", -0.12f, SkeletalEntity.EndType.LOOP, true);
            } else {
                animate("crouch_idle", "crouch_idle", 0.04f, SkeletalEntity.EndType.LOOP, true);
            }
            return;
        }

        if (controller.isMovingForward() && !controller.isMovingBackward()) {
            animate("run_forward", "run", 0.095f, SkeletalEntity.EndType.LOOP, true);
        } else if (controller.isMovingBackward() && !controller.isMovingForward()) {
            animate("run_back", "run", -0.095f, SkeletalEntity.EndType.LOOP, true);
        } else if (controller.isMovingRight() && !controller.isMovingLeft()) {
            animate("sidestep_right", "sidestep", 0.12f, SkeletalEntity.EndType.LOOP, true);
        } else if (controller.isMovingLeft() && !controller.isMovingRight()) {
            animate("sidestep_left", "sidestep", -0.12f, SkeletalEntity.EndType.LOOP, true);
        } else {
            animate("idle", "idle", 0.04f, SkeletalEntity.EndType.LOOP, true);
        }
    }

    public void update(float delta) {
        updateBones(delta);
        updateAnimations(delta);
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}
