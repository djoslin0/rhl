package myGameEngine.Actions;

import myGameEngine.Controllers.OrbitCameraController;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class ActionOrbitAround extends AbstractInputAction {
    private OrbitCameraController occ;
    private OrbitCameraController.OrbitAxis axis;
    private float scalar;

    public ActionOrbitAround(OrbitCameraController occ, OrbitCameraController.OrbitAxis axis, float scalar) {
        this.occ = occ;
        this.axis = axis;
        this.scalar = scalar;
    }

    @Override
    public void performAction(float time, Event event) {
        if (Math.abs(event.getValue()) < 0.2) { return; }
        occ.input(axis, event.getValue() * scalar, true);
    }
}
