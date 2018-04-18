package myGameEngine.Singletons;

import java.io.IOException;
import java.util.ArrayList;

// The TimeManager is responsible for allowing easy access to the delta, elapsed time, and fps from anywhere
public class TimeManager {
    private static final TimeManager instance = new TimeManager();

    // store current tick
    private short tick;

    // store elapsed time and delta time
    private double elapsed;
    private double delta;

    // store values for FPS tracking
    private int displayFps;
    private int fps;
    private double fpsElapsed;

    private TimeManager(){}

    public static double getDelta() {
        return instance.delta;
    }
    public static int getFps() {
        return instance.displayFps;
    }
    public static double getElapsed() { return instance.elapsed; }
    public static short getTick() { return instance.tick; }
    public static void setTick(short tick) { instance.tick = tick; }
    public static void incrementTick() { instance.tick++; }

    public static void update(float delta) {
        instance.elapsed += delta;
        instance.fpsElapsed += delta;
        instance.delta = delta;

        // update fps
        if (instance.fpsElapsed >= 1000) {
            instance.displayFps = instance.fps;
            instance.fps = 0;
            instance.fpsElapsed -= 1000;
        }
        instance.fps++;
    }

    public static int difference(short b, short a) {
        ArrayList<Integer> c = new ArrayList<>();
        c.add(b - a);
        c.add((b + Short.MAX_VALUE - Short.MIN_VALUE + 1) - a);
        c.add(b - (a + Short.MAX_VALUE - Short.MIN_VALUE + 1));

        int smallest = c.get(0);
        for (Integer i : c) {
            if (Math.abs(i) < Math.abs(smallest)) {
                smallest = i;
            }
        }

        return smallest;
    }

    public static void main(String[] args) throws Exception {
        short a = Short.MAX_VALUE;
        short b = (short)(a + 1);
        int diff = difference(b, a);
        if (diff != 1) { throw new Exception("" + diff); }

        diff = difference(a, b);
        if (diff != -1) { throw new Exception("" + diff); }

        a = -5;
        b = (short)(a + 10);
        diff = difference(b, a);
        if (diff != 10) { throw new Exception("" + diff); }
    }
}
