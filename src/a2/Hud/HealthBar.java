package a2.Hud;

import a2.Contollers.HudController;
import a2.GameEntities.Player;
import myGameEngine.GameEntities.HudElement;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector2f;

import java.awt.*;
import java.io.IOException;

public class HealthBar implements Updatable {
    private Player player;
    private HudElement bar;
    private HudElement bar2;
    private float bar2Width;

    public HealthBar(Player player, SceneNode parentNode) {
        this.player = player;
        try {
            bar = new HudElement(parentNode, 0.00185f, Vector2f.createFrom(-0.89f, -0.8f), Vector2f.createFrom(-1f, 0f), "bar.png", new Color(60, 255, 60));
            bar2 = new HudElement(parentNode, 0.001849f, Vector2f.createFrom(-0.89f, -0.8f), Vector2f.createFrom(-1f, 0f), "bar.png", new Color(255, 100, 100));
            new HudElement(parentNode, 0.002f, Vector2f.createFrom(-0.9f, -0.8f), Vector2f.createFrom(-1f, 0f), "battery.png", Color.WHITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        UpdateManager.add(this);
    }

    @Override
    public void update(float delta) {
        // update health bars
        float width = player.getHealth() / 100f * 59f;
        if (width < 0.001f) { width = 0.001f; }
        if (width > bar2Width) { bar2Width = width - 0.1f; }
        if (bar2Width > bar2Width - 0.1f) {
            bar2Width -= delta * 0.005f;
        }
        bar.getNode().setLocalScale(width, 1f, 1f);
        bar2.getNode().setLocalScale(bar2Width, 0.99f, 1f);

        float barHealthF = player.getHealth() / 100f - (bar2Width - width) / 50f;
        if (barHealthF < 0) { barHealthF = 0; }
        float barR = (float)Math.pow(1 - barHealthF, 1.5f) * 0.7f + 0.4f;
        float barG = (float)Math.sqrt(barHealthF * 0.8f) + 0.2f;
        if (barR > 1) { barR = 1; }
        if (barG > 1) { barG = 1; }
        Color healthBarColor = new Color(barR, barG, 0.2f);
        bar.getMaterial().setAmbient(healthBarColor);
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}
