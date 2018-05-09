package a3.Contollers;

import a3.GameEntities.Player;
import a3.GameState;
import a3.Hud.HealthBar;
import a3.Hud.Peripheral;
import a3.Hud.ScoreBoard;
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
    private HudElement winLose;
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
            winLose = new HudElement(hudNode, 0.003f, Vector2f.createFrom(0, 0.6f), Vector2f.createZeroVector(), "win.png", Color.WHITE);
            winLose.hide();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showPeripheral(Color color, float duration) {
        peripheral.show(color, duration);
    }

    public static void showWinLose(boolean win) {
        instance.winLose.updateTexture(win ? "win.png" : "lose.png");
        instance.winLose.show();
    }

    public static void hideWinLose() {
        instance.winLose.hide();
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
