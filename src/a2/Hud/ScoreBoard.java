package a2.Hud;

import a2.GameEntities.Player;
import a2.GameState;
import myGameEngine.GameEntities.HudElement;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector2f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;

public class ScoreBoard implements Updatable {
    private HudNumber orange;
    private HudNumber blue;
    private HudElement container;

    private float orangeChangeTime = 0;
    private float blueChangeTime = 0;

    public ScoreBoard(SceneNode parentNode) {
        try {
            container = new HudElement(parentNode,0.002f, Vector2f.createFrom(0f,.90f), Vector2f.createZeroVector(),"scorehud.png", Color.WHITE);

            // orange side score board
            SceneNode orangeNode = parentNode.createChildSceneNode("orangeNode");
            orangeNode.setLocalPosition(0.0059f, 0, 0);
            orange = new HudNumber(orangeNode, 2, 0.0015f, Vector2f.createFrom(0f,.90f), Color.WHITE);

            // blue side score board
            SceneNode blueNode = parentNode.createChildSceneNode("blueNode");
            blueNode.setLocalPosition(-0.0059f, 0, 0);
            blue = new HudNumber(blueNode, 2, 0.0015f, Vector2f.createFrom(0f,.90f), Color.WHITE);

        } catch (IOException e) {
            e.printStackTrace();
        }
        UpdateManager.add(this);
    }

    public void updateScore(Player.Team team) {
        if (team == Player.Team.Orange) {
            orangeChangeTime = 1000f;
            orange.update(GameState.getScore(Player.Team.Orange));
        }
        else if (team == Player.Team.Blue) {
            blueChangeTime = 1000f;
        }
    }

    @Override
    public void update(float delta) {
        // scale and flash number if it changed
        if (orangeChangeTime > 0) {
            orangeChangeTime -= delta;
            if (orangeChangeTime < 0) { orangeChangeTime = 0; }
            float strength = (float)Math.pow(orangeChangeTime / 1000f, 4);
            orange.setScale(1 + strength * 1.75f);
            float r = 1f;
            float g = 1f - Math.abs((float)Math.sin(orangeChangeTime / 60f) * 0.15f * strength);
            float b = 1f - Math.abs((float)Math.sin(orangeChangeTime / 60f) * 0.3f * strength);
            orange.setColor(new Color(r, g, b));
        }
        
        if (blueChangeTime > 0) {
            blueChangeTime -= delta;
            if (blueChangeTime < 0) { blueChangeTime = 0; }
            float strength = (float)Math.pow(blueChangeTime / 1000f, 4);
            blue.setScale(1 + strength * 1.75f);
            float c = 1f - Math.abs((float)Math.sin(blueChangeTime / 60f) * 0.3f * strength);
            blue.setColor(new Color(c, c, 1f));
        }
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}
