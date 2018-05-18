package a3.Hud;

import a3.GameEntities.Player;
import a3.GameState;
import myGameEngine.GameEntities.HudElement;
import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.AudioManager;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
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
    private HudNumber minutes;
    private HudNumber seconds;
    private HudNumber timer;
    private HudElement colon;
    private HudElement overtime;
    private HudElement container;

    private SoundGroup timerSound;
    private SoundGroup startSound;

    private float timeToStart = 5000f;
    private float orangeChangeTime = 0;
    private float blueChangeTime = 0;

    private int timerNumber = 0;

    public ScoreBoard(SceneNode parentNode) {
        try {
            // timer
            SceneNode timerNode = parentNode.createChildSceneNode("timer");
            timerNode.setLocalPosition(0f,0,0f);
            timer = new HudNumber(timerNode, 1, 0.0025f, Vector2f.createFrom(0f,0.5f), Color.WHITE, false);

            timer.hide();
            // score hud
            container = new HudElement(parentNode,0.002f, Vector2f.createFrom(0f,.90f), Vector2f.createZeroVector(),"scorehud.png", Color.WHITE);

            // orange side score board
            SceneNode orangeNode = parentNode.createChildSceneNode("orangeNode");
            orangeNode.setLocalPosition(0.0059f, 0, 0);
            orange = new HudNumber(orangeNode, 2, 0.0015f, Vector2f.createFrom(0f,.90f), Color.WHITE, false);

            // blue side score board
            SceneNode blueNode = parentNode.createChildSceneNode("blueNode");
            blueNode.setLocalPosition(-0.0059f, 0, 0);
            blue = new HudNumber(blueNode, 2, 0.0015f, Vector2f.createFrom(0f,.90f), Color.WHITE, false);

            // minutes
            SceneNode minutesNode = parentNode.createChildSceneNode("minutesNode");
            minutesNode.setLocalPosition(0.0015f, 0, 0);
            minutes = new HudNumber(minutesNode, 2, 0.0008f, Vector2f.createFrom(0f,.90f), Color.WHITE, true);

            // seconds
            SceneNode secondsNode = parentNode.createChildSceneNode("secondsNode");
            secondsNode.setLocalPosition(-0.0015f, 0, 0);
            seconds = new HudNumber(secondsNode, 2, 0.0008f, Vector2f.createFrom(0f,.90f), Color.WHITE, true);

            // colon
            colon = new HudElement(parentNode, 0.0008f, Vector2f.createFrom(0f,.90f), Vector2f.createZeroVector(), "colon.png", Color.WHITE);

            // overtime
            overtime = new HudElement(parentNode, 0.0008f, Vector2f.createFrom(0f,.90f), Vector2f.createZeroVector(), "overtime.png", Color.WHITE);

            startSound = AudioManager.get().start.clone(parentNode);
            timerSound = AudioManager.get().timer.clone(parentNode);
            startSound.setPitch(1f);
            timerSound.setPitch(1f);



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
            blue.update(GameState.getScore(Player.Team.Blue));
        }
    }

    @Override
    public void update(float delta) {
        // start timer
        if(EntityManager.getPuck().isFrozen() && GameState.getSecondsLeft() >= 1){
            float strength = (float) Math.pow(timeToStart/1000f/timer.getNumber(),4);
            timeToStart -= delta;
            timer.setScale(1 + strength * 1.25f);
            timer.update((int)(timeToStart/1000f)+1);
            if(timer.getNumber() <= 3){
                timer.show();
                if(timerNumber != timer.getNumber()){
                    timerSound.play();
                    timerNumber = timer.getNumber();
                }
                if(timeToStart/1000f < .02){
                    startSound.play();
                }
            }

        }else{
            timer.hide();
            timeToStart = 5000f;
        }
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

        // update time
        if (GameState.getSecondsLeft() >= 0) {
            overtime.hide();
            seconds.update((int)GameState.getSecondsLeft() % 60);
            seconds.show();
            minutes.update((int)(GameState.getSecondsLeft() / 60));
            minutes.show();
            colon.show();
        } else {
            overtime.show();
            seconds.hide();
            minutes.hide();
            colon.hide();
        }
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}
