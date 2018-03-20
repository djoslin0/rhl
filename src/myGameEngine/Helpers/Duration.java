package myGameEngine.Helpers;

// The Duration class represents a period of time that is currently passing
public class Duration {
    private float value;
    private float max;

    public Duration(float max) {
        this.max = max;
    }

    public boolean exceeded(float delta) {
        value += delta;
        if (value >= max) {
            value = max;
            return true;
        } else {
            return false;
        }
    }

    public float progress() {
        return value / max;
    }
    public float max() { return max; }
}
