package a2.Contollers;

import a2.GameEntities.Player;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class RemoteCharacterAnimationController implements Updatable, CharacterAnimationController {
    private Player player;
    private CharacterController controller;
    private String lastAnimationTag;
    private int knockDirection = 0;
    private float knockPitch = 0;
    private boolean knockPitchReached = true;

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
    
    public void jump() {
        SkeletalEntity robo = player.getRobo();
        animate("jump", "jump", 0.04f, SkeletalEntity.EndType.NONE, false);
    }

    public void knock(Vector3 vec) {
        knockDirection = vec.normalize().dot(player.getCameraNode().getWorldForwardAxis()) > 0 ? -1 : 1;
        knockPitchReached = false;
    }

    private void updateKnockPitch(float delta) {
        // add knockback
        SkeletalEntity robo = player.getRobo();
        if (knockPitchReached) {
            // we reached our peak and have begun decreasing
            boolean knockPitchSign = (knockPitch >= 0);
            knockPitch -= knockDirection * 0.0028f * delta;
            if ((knockPitch >= 0) != knockPitchSign) {
                // we crossed knockPitch = 0, stop applying pitch
                knockDirection = 0;
                robo.removeRotationAdditive("torso");
                robo.removeRotationOverride("shoulder_L");
                robo.removeRotationOverride("shoulder_R");
                robo.removeRotationOverride("hip_R");
                robo.removeRotationOverride("hip_L");
                robo.removeLocationAdditive("torso");
                return;
            }
        } else {
            // we are approaching our peak
            knockPitch += knockDirection * 0.007f * delta;
            if (Math.abs(knockPitch) >= 0.8f) {
                // we have reached our peak, decrease afterward
                knockPitchReached = true;
            }
        }

        // rotations
        Matrix3 pz = Matrix3f.createFrom(0, 0, knockPitch);
        robo.addRotationAdditive("torso", pz.toQuaternion());

        Matrix3 pny = Matrix3f.createFrom(0, -knockPitch, 0);
        robo.addRotationOverride("shoulder_L", pny.toQuaternion());
        robo.addRotationOverride("shoulder_R", pny.toQuaternion());
        robo.addRotationOverride("hip_R", pny.toQuaternion());

        Matrix3 py = Matrix3f.createFrom(0, knockPitch, 0);
        robo.addRotationOverride("hip_L", py.toQuaternion());

        Matrix3 px = Matrix3f.createFrom(knockPitch, 0, 0);

        // torso location offset
        if (knockPitch > 0) {
            if (controller.isCrouching()) {
                robo.addLocationAdditive("torso", Vector3f.createFrom(-1.0f, -0.4f, 0.0f).mult(knockPitch * 0.4f));
            } else {
                robo.addLocationAdditive("torso", Vector3f.createFrom(-0.3f, -0.6f, 0.0f).mult(knockPitch * 1.0f));
            }
        } else {
            if (controller.isCrouching()) {
                robo.addLocationAdditive("torso", Vector3f.createFrom(-0.6f, 0.2f, 0.0f).mult(knockPitch * 1.0f));
            } else {
                robo.addLocationAdditive("torso", Vector3f.createFrom(-0.6f, -0.1f, 0.0f).mult(knockPitch * 1.0f));
            }
        }
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

            if (controller.getAttackTicks() > 0) {
                float attackScale = (controller.getAttackTicks() / (float)CharacterController.attackTickTimeout);
                attackScale *= attackScale;
                attackScale = 1f - attackScale;
                pitch_upper += Math.sin(attackScale * Math.PI) * 2f;
                pitch_lower -= Math.sin(attackScale * Math.PI) * 0.5f;
            }
            Matrix3 armUpper = Matrix3f.createFrom(0, 0, pitch_upper);
            robo.addRotationOverride("arm_upper_R", armUpper.toQuaternion());

            Matrix3 armLower = Matrix3f.createFrom(pitch_lower, 0, 0);
            robo.addRotationOverride("arm_lower_R", armLower.toQuaternion());

            if (knockDirection != 0) {
                updateKnockPitch(delta);
            }
        }
    }

    private void updateAnimations(float delta) {
        SkeletalEntity robo = player.getRobo();
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
