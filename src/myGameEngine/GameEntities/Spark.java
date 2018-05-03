package myGameEngine.GameEntities;

import myGameEngine.Helpers.Duration;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;

public class Spark extends GameEntity {
    private Duration duration;
    private Vector3 velocity;

    public Spark(Vector3 position, Vector3 velocity, float maxDuration) {
        super(true);
        this.duration = new Duration(maxDuration);
        node = EngineManager.getSceneManager().getRootSceneNode().createChildSceneNode("Spark" + UniqueCounter.next());
        node.setLocalPosition(position);
        try {
            addResponsibility(new Trail(node, maxDuration, 4, Color.WHITE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.velocity = velocity;
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (duration.exceeded(delta)) {
            // ran out of life
            destroy();
            return;
        }
        velocity = velocity.sub(0, 0.001f * delta, 0);
        node.translate(velocity.mult(delta * 0.05f));
    }


}
