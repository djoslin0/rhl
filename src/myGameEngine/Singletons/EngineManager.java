package myGameEngine.Singletons;

import ray.rage.Engine;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.scene.SceneManager;

import java.util.ArrayList;
import java.util.HashMap;

// The EngineManager is responsible for allowing easy access to the engine and scenemanager from anywhere
public class EngineManager {
    private static final EngineManager instance = new EngineManager();
    private Engine engine;
    private RenderWindow renderWindow;
    private boolean gameActive = true;
    private boolean gameStarted = true;

    private EngineManager(){}

    public static void init(Engine engine) { instance.engine = engine; }
    public static Engine getEngine(){ return instance.engine; }
    public static SceneManager getSceneManager() { return instance.engine.getSceneManager(); }
    public static boolean hasGameStarted() { return instance.gameStarted; }
    public static boolean isGameActive() { return instance.gameActive; }
    public static void setGameActive(boolean gameActive) {
        if (gameActive) { instance.gameStarted = true; }
        instance.gameActive = gameActive;
    }

    public static RenderWindow getRenderWindow() {
        return instance.renderWindow;
    }

    public static void setRenderWindow(RenderWindow rw) {
        instance.renderWindow = rw;
    }
}
