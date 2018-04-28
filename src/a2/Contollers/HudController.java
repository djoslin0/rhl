package a2.Contollers;

import a2.GameEntities.Player;
import myGameEngine.GameEntities.HudElement;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector2f;

import java.awt.*;
import java.io.IOException;

public class HudController implements Updatable {
    private Player player;
    private HudElement peripheral;
    private HudElement healthBar;
    private HudElement healthBar2;
    private HudElement crosshair;
    private float healthBar2Width;
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
            SceneNode hudNode = cameraNode.createChildSceneNode(name + "Node");
            hudNode.moveForward(0.05f);

            // create peripheral
            peripheral = new HudElement(hudNode, 1f, Vector2f.createZeroVector(), Vector2f.createZeroVector(), 0, "peripheral.png", peripheralColor);
            peripheral.fullscreen = true;
            player.addResponsibility(peripheral);

            // create battery
            HudElement battery = new HudElement(hudNode, 0.002f, Vector2f.createFrom(-0.9f, -0.8f), Vector2f.createFrom(-1f, 0f), 4, "battery.png", Color.WHITE);
            player.addResponsibility(battery);

            // create healthbar
            healthBar = new HudElement(hudNode, 0.00185f, Vector2f.createFrom(-0.89f, -0.8f), Vector2f.createFrom(-1f, 0f), 2, "bar.png", new Color(60, 255, 60));
            player.addResponsibility(healthBar);

            healthBar2 = new HudElement(hudNode, 0.001849f, Vector2f.createFrom(-0.89f, -0.8f), Vector2f.createFrom(-1f, 0f), 3, "bar.png", new Color(255, 100, 100));
            player.addResponsibility(healthBar2);

            // create crosshair
            crosshair = new HudElement(hudNode, 0.0007f, Vector2f.createZeroVector(), Vector2f.createZeroVector(), 1, "flare2.png", Color.RED);
            player.addResponsibility(crosshair);

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
        crosshair.getNode().setLocalScale(0.001f, 0.001f, 0.001f);
    }

    public void showLivingHud() {
        hidingLivingHud = false;
        crosshair.getNode().setLocalScale(1f, 1f, 1f);
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
        // update peripheral
        if (peripheralDuration > 0) {
            peripheralDuration -= delta;
            if (peripheralDuration < 0) { peripheralDuration = 0; }
        }
        peripheral.getMaterial().setAmbient(calculatePeripheralColor());

        // update health bars
        float barWidth = player.getHealth() / 100f * 59f;
        if (barWidth < 0.001f) { barWidth = 0.001f; }
        if (barWidth > healthBar2Width) { healthBar2Width = barWidth - 0.1f; }
        if (healthBar2Width > barWidth - 0.1f) {
            healthBar2Width -= delta * 0.005f;
        }
        healthBar.getNode().setLocalScale(barWidth, 1f, 1f);
        healthBar2.getNode().setLocalScale(healthBar2Width, 0.99f, 1f);

        float barHealthF = player.getHealth() / 100f - (healthBar2Width - barWidth) / 50f;
        if (barHealthF < 0) { barHealthF = 0; }
        float barR = (float)Math.pow(1 - barHealthF, 1.5f) * 0.7f + 0.4f;
        float barG = (float)Math.sqrt(barHealthF * 0.8f) + 0.2f;
        if (barR > 1) { barR = 1; }
        if (barG > 1) { barG = 1; }
        Color healthBarColor = new Color(barR, barG, 0.2f);
        healthBar.getMaterial().setAmbient(healthBarColor);
    }

    @Override
    public boolean blockUpdates() { return false; }
}
