package a2.Hud;

import a2.GameEntities.Player;
import a2.GameState;
import myGameEngine.GameEntities.HudElement;
import ray.rage.scene.SceneNode;
import ray.rml.Vector2f;

import java.awt.*;
import java.io.IOException;

public class ScoreBoard {
    private HudNumber orange;
    private HudNumber blue;
    private HudElement container;

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
    }

    public void updateScore() {
        orange.update(GameState.getScore(Player.Team.Orange));
        blue.update(GameState.getScore(Player.Team.Blue));
    }
}