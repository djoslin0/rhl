package myGameEngine.GameEntities;

import myGameEngine.Helpers.Duration;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.scene.Light;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;

import java.awt.*;

public class LightFade extends GameEntityUpdatable {
    private Light light;
    private Duration duration;
    private float range;

    public LightFade(SceneNode parentNode, Color color, float range, float attenuation, float maxDuration) {
        SceneManager sm = EngineManager.getSceneManager();

        this.range = range;
        this.duration = new Duration(maxDuration);

        long unique = UniqueCounter.next();
        light = sm.createLight(parentNode.getName() + "Light" + unique, Light.Type.POINT);
        addResponsibility(light);
        light.setAmbient(Color.black);
        light.setDiffuse(color);
        light.setSpecular(color);
        light.setConstantAttenuation(0);
        light.setQuadraticAttenuation(0);
        light.setLinearAttenuation(attenuation);
        light.setRange(range);

        SceneNode node = parentNode.createChildSceneNode(parentNode.getName() + "LightNode" + unique);
        addResponsibility(node);
        node.attachObject(light);
    }

    @Override
    public void update(float delta) {
        if (duration.exceeded(delta)) {
            // ran out of life
            destroy();
            return;
        }

        // fade light
        float scalar = duration.progress();
        light.setConstantAttenuation(scalar);
        light.setRange(range * (1f - scalar));
    }
}
