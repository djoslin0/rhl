package myGameEngine.Actions;

import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;

// This class will call one of two actions after an axis exceeds the active zone.
// Once triggered, it will not be able to call the action until the axis is below the inactive zone.

public class ActionAxisToggle implements Action {
    private Action negative;
    private Action positive;
    private float activeZone;
    private float inactiveZone;
    private boolean isToggled;

    public ActionAxisToggle(Action negative, Action positive, float activeZone, float inactiveZone) {
        this.negative = negative;
        this.positive = positive;
        this.activeZone = activeZone;
        this.inactiveZone = inactiveZone;
    }

    @Override
    public void performAction(float v, Event event) {
        // when toggled, stay toggled until axis is below the inactive zone
        if (isToggled) {
            if (Math.abs(event.getValue()) < inactiveZone) {
                isToggled = false;
            } else {
                return;
            }
        }
        if (event.getValue() < -activeZone) {
            isToggled = true;
            negative.performAction(v, event);
        } else if (event.getValue() > activeZone) {
            isToggled = true;
            positive.performAction(v, event);
        }
    }
}
