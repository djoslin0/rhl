package myGameEngine.Actions;

import net.java.games.input.Event;
import ray.input.action.Action;

public class ActionScale implements Action {
    private Action action;
    private float scalar;

    public ActionScale(Action action, float scalar) {
        this.action = action;
        this.scalar = scalar;
    }

    @Override
    public void performAction(float v, Event event) {
        if (Math.abs(event.getValue()) < 0.2f) { return; }
        Event scaled = new Event();
        scaled.set(event.getComponent(), event.getValue() * scalar, event.getNanos());
        action.performAction(v, scaled);
    }
}
