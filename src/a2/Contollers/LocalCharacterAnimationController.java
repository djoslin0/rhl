package a2.Contollers;

import a2.GameEntities.Player;
import myGameEngine.Helpers.MathHelper;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.rendersystem.Viewport;
import ray.rml.*;

public class LocalCharacterAnimationController implements Updatable, CharacterAnimationController {
    private Player player;
    private CharacterController controller;
    private Vector3 velocityOffset = Vector3f.createZeroVector();
    private Vector3 lastForwardVector = Vector3f.createZeroVector();
    private Vector3 rotationOffset = Vector3f.createZeroVector();

    private float bob;
    private static final float bobSpeed = 0.00075f;
    private static final float bobOffset = 0.015f;

    public LocalCharacterAnimationController(Player player, CharacterController controller) {
        this.player = player;
        this.controller = controller;
        UpdateManager.add(this);
    }

    @Override
    public void knock(Vector3 vec, Vector3 relative) {}

    @Override
    public void update(float delta) {
        // get default position according to aspect ratio
        Viewport vp = EngineManager.getRenderWindow().getViewport(0);
        float aspectRatio = (float)vp.getActualWidth() / (float)vp.getActualHeight();
        Vector3 offset = Vector3f.createFrom(-0.34f * aspectRatio, -0.5f, 0.65f);

        if (controller.isOnGround()) {
            if (controller.isMovingForward()
                    || controller.isMovingBackward()
                    || controller.isMovingLeft()
                    || controller.isMovingRight()) {
                bob += player.getVelocity().length() * bobSpeed * delta;
                if (bob > Math.PI * 2) { bob -= Math.PI * 2; }
            }
        }

        // add headbob offset
        offset = offset.add(0f, (float)Math.sin(bob) * bobOffset, 0);

        // grab velocity, rotate to camera local, multiply down
        Vector3 velocityTempOffset = player.getVelocity().rotate(Radianf.createFrom(-player.getYaw()), Vector3f.createUnitVectorY()).mult(-0.004f);
        if (velocityTempOffset.length() > 0.08f) {
            // limit velocity offset
            velocityTempOffset = velocityTempOffset.normalize().mult(0.08f);
        }
        // smooth velocity offset
        velocityOffset = MathHelper.lerp(velocityOffset, velocityTempOffset, 0.4f);
        offset = offset.add(velocityOffset);

        // limit offset
        float ox = offset.x();
        float oy = offset.y();
        float oz = offset.z();
        if (oy > -0.48f) { oy = -0.48f; }
        if (oz > 0.7f) { oz = 0.7f; }

        // set location offset
        player.getHandNode().setLocalPosition(ox, oy, oz);

        // grab difference in forward vectors to detect rotation
        Vector3 forwardDiff = player.getCameraNode().getWorldForwardAxis().sub(lastForwardVector);
        lastForwardVector = player.getCameraNode().getWorldForwardAxis();
        float xzDiff = (forwardDiff.x() + forwardDiff.z()) / 2f;
        // generate rotation vector based on difference in forward vectors
        Vector3 rot = Vector3f.createFrom(forwardDiff.y() * 6f, xzDiff * 4f, xzDiff * 2.4f);
        // smooth rotation vector
        rotationOffset = rotationOffset.lerp(rot, 0.2f);

        // set rotation
        player.getHandNode().setLocalRotation(Matrix3f.createFrom(70f + rotationOffset.x(), rotationOffset.y(), rotationOffset.z()));

    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}
