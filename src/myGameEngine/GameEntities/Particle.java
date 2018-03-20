package myGameEngine.GameEntities;

import myGameEngine.Helpers.Duration;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.awt.*;
import java.io.IOException;

public class Particle extends Billboard {
    private Duration duration;
    private float initialWidth;
    private float initialHeight;
    private Vector3 velocity;

    public Particle(float width, float height, Vector3 position, Vector3 velocity, String textureName, Color color, float maxDuration) throws IOException {
        super(null, width, height, textureName, color);
        this.duration = new Duration(maxDuration);
        this.velocity = velocity;
        initialWidth = width;
        initialHeight = height;
        this.node.setLocalPosition(position);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (duration.exceeded(delta)) {
            // ran out of life
            destroy();
            return;
        }

        // move with velocity
        node.setLocalPosition(node.getLocalPosition().add(velocity.mult(delta)));

        // resize
        float scalar = 1f - (float)Math.pow(duration.progress(), 2f);
        this.setSize(initialWidth * scalar, initialHeight * scalar);
    }

    public SceneNode getNode() {
        return this.node;
    }
}
