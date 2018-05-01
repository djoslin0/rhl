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
    private HudElement orangeSideScore0;
    private HudElement blueSideScore0;
    private HudElement orangeSideScore1 = null;
    private HudElement blueSideScore1 = null;
    private HudElement scoreHud;
    private SceneNode hudNode;
    private int blueScore = 0;
    private int orangeScore = 0;
    private float healthBar2Width;
    private float peripheralDuration;
    private float peripheralMaxDuration;
    private static HudController hudController = null;
    private Color peripheralColor = new Color(1f, 1f, 1f, 0f);
    private boolean hidingLivingHud = false;

    // initial instantiation of singleton hud controller
    public static HudController getHudController(Player player){
        if(hudController == null){
            hudController = new HudController(player);
            return hudController;
        }else
            return hudController;
    }

    //get hug controller after instantiation
    public static HudController getHudController() {
        return hudController;
    }

    private HudController(Player player) {
        this.player = player;

        SceneManager sm = EngineManager.getSceneManager();

        String name = "Player" + player.getId() + "Hud";
        SceneNode cameraNode = player.getCameraNode();

        try {
            hudNode = cameraNode.createChildSceneNode(name + "Node");
            hudNode.moveForward(0.05f);

            // create peripheral
            peripheral = new HudElement(hudNode, 1f, Vector2f.createZeroVector(), Vector2f.createZeroVector(), 0, "peripheral.png", peripheralColor);
            peripheral.fullscreen = true;
            player.addResponsibility(peripheral);

            // create healthbar
            healthBar = new HudElement(hudNode, 0.00185f, Vector2f.createFrom(-0.89f, -0.8f), Vector2f.createFrom(-1f, 0f), 0, "bar.png", new Color(60, 255, 60));
            player.addResponsibility(healthBar);

            healthBar2 = new HudElement(hudNode, 0.001849f, Vector2f.createFrom(-0.89f, -0.8f), Vector2f.createFrom(-1f, 0f), 0, "bar.png", new Color(255, 100, 100));
            player.addResponsibility(healthBar2);

            // create battery
            HudElement battery = new HudElement(hudNode, 0.002f, Vector2f.createFrom(-0.9f, -0.8f), Vector2f.createFrom(-1f, 0f), 0, "battery.png", Color.WHITE);
            player.addResponsibility(battery);

            // create crosshair
            crosshair = new HudElement(hudNode, 0.0007f, Vector2f.createZeroVector(), Vector2f.createZeroVector(), 0, "flare2.png", Color.RED);
            player.addResponsibility(crosshair);

            SceneNode leftHudNode = hudNode.createChildSceneNode("leftHudNode");
            leftHudNode.setLocalPosition(-0.007f, 0, 0);
            // orange side score board
            orangeSideScore0 = new HudElement(leftHudNode,0.002f,Vector2f.createFrom(0f,.60f),Vector2f.createZeroVector(),0,"0.png",Color.WHITE);
            player.addResponsibility(orangeSideScore0);
            //blue side score board

            //score container
            scoreHud = new HudElement(hudNode,0.002f,Vector2f.createFrom(0f,.60f),Vector2f.createZeroVector(),0,"scorehud.png",Color.WHITE);
            player.addResponsibility(scoreHud);

            blueSideScore0 = new HudElement(hudNode,0.002f,Vector2f.createFrom(.40f,.85f),Vector2f.createZeroVector(),0,"0.png",Color.WHITE);
            player.addResponsibility(blueSideScore0);



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
    public void updateScore(Player.Team side, int num){
        if(side == Player.Team.Orange){
            orangeScore = orangeScore + num;
        }else{
            blueScore = blueScore +num;
        }
        if( side == Player.Team.Orange && orangeScore % 10 != 0 ){
            orangeSideScore0.updateTexture(orangeScore % 10 +".png");
        }else if(side == Player.Team.Orange && orangeSideScore1 != null){
            orangeSideScore0.updateTexture("0.png");
            orangeSideScore1.updateTexture(orangeScore / 10 + ".png" );
        }else if(side == Player.Team.Orange){
                orangeSideScore0.destroy();
            try {
                orangeSideScore0 = new HudElement(hudNode,0.002f,Vector2f.createFrom(-.34f,.85f),Vector2f.createZeroVector(),5,"0.png",Color.WHITE);
                orangeSideScore1 = new HudElement(hudNode,0.002f,Vector2f.createFrom(-.40f,.85f),Vector2f.createZeroVector(),5,"1.png",Color.WHITE);
                player.addResponsibility(orangeSideScore1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(side == Player.Team.Blue && blueScore % 10 != 0){
            blueSideScore0.updateTexture(blueScore % 10 +".png");
        }else if(side == Player.Team.Blue && blueSideScore1 != null){
            blueSideScore0.updateTexture("0.png");
            blueSideScore1.updateTexture(blueScore / 10 + ".png" );
        }else if(side == Player.Team.Blue){
            blueSideScore0.updateTexture("0.png");
            try {
                blueSideScore1 = new HudElement(hudNode,0.002f,Vector2f.createFrom(0.34f,.85f),Vector2f.createZeroVector(),5,"1.png",Color.WHITE);
                player.addResponsibility(blueSideScore1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
