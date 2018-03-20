package myGameEngine.Singletons;

// The TimeManager is responsible for allowing easy access to the delta, elapsed time, and fps from anywhere
public class TimeManager {
    private static final TimeManager instance = new TimeManager();

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

}
