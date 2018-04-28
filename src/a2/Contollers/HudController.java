package a2.Contollers;

import a2.GameEntities.Player;
import myGameEngine.GameEntities.Billboard;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.TimeManager;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.rendersystem.Viewport;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;

import java.awt.*;
import java.io.IOException;

public class HudController implements Updatable {
    private Player player;
    private SceneNode crosshairNode;
    private Billboard peripheral;
    private SceneNode peripheralNode;
    private float peripheralDuration;
    private float peripheralMaxDuration;
    private Color peripheralColor = new Color(1f, 1f, 1f, 0f);
    private boolean hidingLivingHud = false;

    public HudController(Player player) {
        this.player = player;

        SceneManager sm = EngineManager.getSceneManager();

        String name = "Player" + player.getId() + "Hud";
        SceneNode cameraNode = player.getCameraNode();

        try {
            // create crosshair
            crosshairNode = cameraNode.createChildSceneNode(name + "crosshairNode");
            player.addResponsibility(crosshairNode);
            crosshairNode.moveForward(3);
            Billboard crosshair = new Billboard(crosshairNode, 0.2f, 0.2f, "flare2.png", Color.RED);
            player.addResponsibility(crosshair);

            // create peripheral
            peripheralNode = cameraNode.createChildSceneNode(name + "peripheralNode");
            player.addResponsibility(peripheralNode);
            peripheralNode.moveForward(0.05f);
            peripheral = new Billboard(peripheralNode, 1f, 1f, "peripheral.png", peripheralColor);

            // set no depth testing
            ZBufferState zBufferState = (ZBufferState) sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
            zBufferState.setSecondaryStage(true);
            crosshair.getManualObject().setRenderState(zBufferState);
            peripheral.getManualObject().setRenderState(zBufferState);

        } catch (IOException e) {
            e.printStackTrace();
        }

        UpdateManager.add(this);
    }

    public void showPeripheral(Color color, float duration) {
        peripheralColor = color;
        peripheralDuration = duration;
        peripheralMaxDuration = duration;
    }

    public void hideLivingHud() {
        hidingLivingHud = true;
        crosshairNode.setLocalScale(0.001f, 0.001f, 0.001f);
    }

    public void showLivingHud() {
        hidingLivingHud = false;
        crosshairNode.setLocalScale(1f, 1f, 1f);
    }

    private Color calculatePeripheralColor() {
        int red = peripheralColor.getRed();
        int green = peripheralColor.getGreen();
        int blue = peripheralColor.getBlue();
        int alpha = peripheralColor.getAlpha();

        if (!player.isDead() && player.getHealth() < 30) {
            // fade to black as we are close to death
            float intensity = 1f - (player.getHealth() / 30f);
            float duration = peripheralDuration / peripheralMaxDuration;
            float inverseDuration = 1f - duration;
            red = (int)(red * duration);
            green = (int)(green * duration);
            blue = (int)(blue * duration);
            alpha = (int)(alpha * duration + 255 * inverseDuration * intensity);
        } else {
            alpha = (int)(alpha * peripheralDuration / peripheralMaxDuration);
        }

        return new Color(red, green, blue, alpha);
    }

    @Override
    public void update(float delta) {

        // get default position according to aspect ratio
        Viewport vp = EngineManager.getRenderWindow().getViewport(0);
        float aspectRatio = (float)vp.getActualWidth() / (float)vp.getActualHeight();

        float h = 0.0577434f;
        float w = h * aspectRatio;

        if (peripheralDuration > 0) {
            peripheralDuration -= delta;
            if (peripheralDuration < 0) { peripheralDuration = 0; }
        }

        peripheral.getMaterial().setAmbient(calculatePeripheralColor());
        peripheralNode.setLocalScale(w, h, 1);
    }

    @Override
    public boolean blockUpdates() { return false; }
}
