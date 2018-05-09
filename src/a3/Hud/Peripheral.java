package a3.Hud;

import a3.GameEntities.Player;
import a3.GameState;
import myGameEngine.GameEntities.HudElement;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.rendersystem.Viewport;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Vector2f;

import java.awt.*;
import java.io.IOException;

public class Peripheral extends HudElement implements Updatable {
    private Player player;
    private Color color = new Color(1f, 1f, 1f, 0f);
    private float duration = 1;
    private float maxDuration = 1;

    public Peripheral(Player player, SceneNode parentNode) throws IOException {
        super(parentNode, 1f, Vector2f.createZeroVector(), Vector2f.createZeroVector(), "peripheral.png", new Color(1f, 1f, 1f, 0f));
        this.player = player;
        UpdateManager.add(this);
    }

    private Color calculateColor() {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = color.getAlpha();

        if (!player.isDead() && player.getHealth() < 30) {
            // fade to black as we are close to death
            float intensity = 1f - (player.getHealth() / 30f);
            float scalar = duration / maxDuration;
            float inverseScalar = 1f - scalar;
            red = (int) (red * scalar);
            green = (int) (green * scalar);
            blue = (int) (blue * scalar);
            alpha = (int) (alpha * scalar + 255 * inverseScalar * intensity);
        } else {
            alpha = (int) (alpha * duration / maxDuration);
        }

        return new Color(red, green, blue, alpha);
    }

    public void show(Color color, float duration) {
        this.color = color;
        this.duration = duration;
        this.maxDuration = duration;
    }

    private void scoreAdded(Player.Team team) {
    }

    public void updateScore(Player.Team team) {
        if (team != player.getSide()) {
            show(Color.BLACK, 1000f);
        }
    }

    @Override
    public void onCameraPreRenderScene(Camera camera) {
        super.onCameraPreRenderScene(camera);

        // get default position according to aspect ratio
        Viewport vp = EngineManager.getRenderWindow().getViewport(0);
        float aspectRatio = (float)vp.getActualWidth() / (float)vp.getActualHeight();

        float h = 0.0577434f;
        float w = h * aspectRatio;
        node.setLocalScale(w / 2f, h / 2f, 1);
    }


    @Override
    public void update(float delta) {
        // update peripheral
        if (duration > 0) {
            duration -= delta;
            if (duration < 0) { duration = 0; }
        }
        getMaterial().setAmbient(calculateColor());
    }
}
