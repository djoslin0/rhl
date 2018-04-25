package a2.Contollers;

import a2.GameEntities.Player;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.rendersystem.Viewport;
import ray.rml.Radianf;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class LocalCharacterAnimationController implements Updatable, CharacterAnimationController {
    private Player player;
    private CharacterController controller;
    private Vector3 offset;

    private float bob;
    private static final float bobSpeed = 0.00075f;
    private static final float bobOffset = 0.015f;

    public LocalCharacterAnimationController(Player player, CharacterController controller) {
        this.player = player;
        this.controller = controller;
        UpdateManager.add(this);
    }

    @Override
    public void jump() {}

    @Override
    public void knock(Vector3 vec) {}

    @Override
    public void update(float delta) {
        //Settings.runScript();
        // get default position according to aspect ratio
        Viewport vp = EngineManager.getRenderWindow().getViewport(0);
        float aspectRatio = (float)vp.getActualWidth() / (float)vp.getActualHeight();
        Vector3 offset = Vector3f.createFrom(-0.35f * aspectRatio, -0.5f, 0.7f);

        if (controller.isOnGround()) {
            if (controller.isMovingForward()
                    || controller.isMovingBackward()
                    || controller.isMovingLeft()
                    || controller.isMovingRight()) {
                bob += player.getVelocity().length() * bobSpeed * delta;
                if (bob > Math.PI * 2) { bob -= Math.PI * 2; }
            }
        } else {

        }
        offset = offset.add(0f, (float)Math.sin(bob) * bobOffset, 0);
        //offset = offset.add(player.getVelocity().sub(player.getCameraNode().getWorldForwardAxis()).mult(Settings.get().debug1.floatValue()));
        player.getHandNode().setLocalPosition(offset);
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}
