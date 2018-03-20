package myGameEngine.Controllers;

import ray.rage.scene.Node;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class CollectController extends AbstractController {
    private boolean collected;
    private boolean collecting;
    private float life;

    public void collect() {
        if (collecting) { return; }
        collecting = true;
        collected = false;
        life = 250f;
    }

    public void respawn() {
        collecting = false;
        collected = false;
        for (Node n : super.controlledNodesList) {
            Vector3 curScale = Vector3f.createFrom(1, 1, 1);
            n.setLocalScale(curScale);
        }
    }

    public boolean isCollected() { return collected; }
    public boolean isCollecting() { return collecting; }

    @Override
    protected void updateImpl(float time) {
        if (!collecting) { return; }
        life -= time;
        if (life <= 0) {
            collecting = false;
            collected = true;
            return;
        }
        float scaleAmt = life / 250f;
        for (Node n : super.controlledNodesList) {
            Vector3 curScale = Vector3f.createFrom(scaleAmt, scaleAmt, scaleAmt);
            n.setLocalScale(curScale);
        }
    }
}
