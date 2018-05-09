package a3.GameEntities;

import myGameEngine.GameEntities.Billboard;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.Duration;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.TimeManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.scene.Light;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;

import java.awt.*;
import java.io.IOException;

public class GoalText extends GameEntity {
    private Duration duration = new Duration(5000f);
    private Light light;
    private Billboard[] letters;
    private SceneNode[] nodes;
    private boolean dunk;
    private float cr;
    private float cg;
    private float cb;

    public GoalText(Player.Team team, boolean dunk) {
        super(true);
        this.dunk = dunk;
        SceneManager sm = EngineManager.getSceneManager();

        if (team == Player.Team.Orange) {
            cr = 1.0f;
            cg = 0.9f;
            cb = 0.6f;
        } else {
            cr = 0.6f;
            cg = 0.6f;
            cb = 1.0f;
        }

        node = sm.getRootSceneNode().createChildSceneNode("GoalTextNode" + UniqueCounter.next());
        float goalDistance = Settings.get().goalDistance.floatValue();
        goalDistance = goalDistance * ((team == Player.Team.Orange) ? 1 : -1);
        node.setLocalPosition(goalDistance, 2.5f, 0f);

        String text = dunk ? "dunk" : "goal";

        letters = new Billboard[4];
        nodes = new SceneNode[4];
        try {
            for (int i = 0; i < nodes.length; i++) {
                int k = (team == Player.Team.Orange) ? i : nodes.length - i - 1;
                nodes[i] = node.createChildSceneNode("GoalTextSubNode" + UniqueCounter.next());
                letters[i] = new Billboard(nodes[i], 3f, 3f, text.charAt(k) + ".png", Color.WHITE);
                addResponsibility(letters[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        light = sm.createLight(node.getName() + "Light" + UniqueCounter.next(), Light.Type.POINT);
        addResponsibility(light);
        light.setAmbient(Color.black);
        light.setConstantAttenuation(0);
        light.setQuadraticAttenuation(0);
        light.setLinearAttenuation(0.1f);
        light.setRange(60f);
        node.attachObject(light);
    }

    public void update(float delta) {
        if (duration.exceeded(delta)) {
            this.destroy();
            return;
        }

        float scalar = 1f - (float)Math.pow(duration.progress(), 2f);

        for (int i = 0; i < letters.length; i++) {
            float theta = TimeManager.getTick() * 0.05f;
            float y = (float)Math.sin(theta + i * 1f * scalar) * 1.5f * scalar;
            nodes[i].setLocalPosition(0f, y, (i - 1.5f) * 3f);
            nodes[i].setLocalScale(scalar, scalar, scalar);
            Color color;
            Color lightColor;
            if (dunk) {
                float r = 0.5f + (float)Math.sin(theta * 2f + i * 1f * scalar) * 0.5f * scalar;
                float b = 0.5f + (float)Math.cos(theta * 2f + i * 1f * scalar) * 0.5f * scalar;
                if (r > 0.5) { r = 1f; }
                if (b > 0.5) { b = 1f; }
                color = new Color(r, 0, b);
                lightColor = color;
            } else {
                float v = 0.9f + (float)Math.sin(theta * 2 + i) * 0.1f;
                color = new Color(cr * v, cg * v, cb * v);
                lightColor = new Color((float)Math.pow(cr, 3) * (float)Math.pow(v, 4), (float)Math.pow(cg, 3) * (float)Math.pow(v, 4), (float)Math.pow(cb, 3) * (float)Math.pow(v, 4));
            }
            letters[i].getMaterial().setAmbient(color);
            if (i == 0) {
                light.setDiffuse(lightColor);
                light.setSpecular(lightColor);
                light.setRange(60f * (float)Math.pow(scalar, 2));
            }
        }
    }
}
