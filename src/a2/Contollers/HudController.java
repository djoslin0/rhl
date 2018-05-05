package a2.Contollers;

import a2.GameEntities.Player;
import a2.GameState;
import a2.Hud.HealthBar;
import a2.Hud.Peripheral;
import a2.Hud.ScoreBoard;
import myGameEngine.GameEntities.HudElement;
import myGameEngine.Helpers.SoundGroup;
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

public class HudController {
    private Peripheral peripheral;
    private HealthBar healthBar;
    private ScoreBoard scoreBoard;
    private HudElement crosshair;
    private SceneNode hudNode;
    private static HudController instance = null;

    // initial instantiation of singleton hud controller
    public static HudController get(Player player){
        if (instance == null) { instance = new HudController(player); }
        return instance;
    }

    private HudController(Player player) {
        String name = "Player" + player.getId() + "Hud";
        SceneNode cameraNode = player.getCameraNode();

        try {
            hudNode = cameraNode.createChildSceneNode(name + "Node");
            hudNode.moveForward(0.05f);

            peripheral = new Peripheral(player, hudNode);
            healthBar = new HealthBar(player, hudNode);
            scoreBoard = new ScoreBoard(hudNode);
            crosshair = new HudElement(hudNode, 0.0007f, Vector2f.createZeroVector(), Vector2f.createZeroVector(), "flare2.png", Color.RED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showPeripheral(Color color, float duration) {
        peripheral.show(color, duration);
    }

    public void hideLivingHud() {
        crosshair.getNode().setLocalScale(0.001f, 0.001f, 0.001f);
    }

    public void showLivingHud() {
        crosshair.getNode().setLocalScale(1f, 1f, 1f);
    }

    public static void updateScore(Player.Team team){
        if (instance.scoreBoard == null) { return; }
        instance.scoreBoard.updateScore(team);
        instance.peripheral.updateScore(team);
    }
}
