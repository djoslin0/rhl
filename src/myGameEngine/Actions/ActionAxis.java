package myGameEngine.Actions;

import net.java.games.input.Event;
import ray.input.action.Action;

// This class will call one of two actions depending on if the axis is positive or negative.
public class ActionAxis implements Action {
    private Action negative;
    private Action positive;
    public ActionAxis(Action negative, Action positive) {
        // save actions to call based on positive or negative axis input
        this.negative = negative;
        this.positive = positive;
    }

    @Override
    public void performAction(float v, Event event) {
        // perform negative or positive action based on axis input
        float value = event.getValue();
        if (value < -0f) {
            negative.performAction(v, event);
        } else if (value > 0f) {
            positive.performAction(v, event);
        }
    }
}
