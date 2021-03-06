package myGameEngine.Singletons;

import ray.rml.Vector3;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Settings {
    private static final Settings instance = new Settings();

    private ScriptEngine jsEngine;
    private File scriptFile;

    public boolean verticalSync;
    public int fov;

    public Color ambientColor;
    public Color diffuseColor;
    public Color specularColor;
    public Vector3 lightDirection;

    public Vector3 puckSpawnPoint;

    public Color terrainEmissive;
    public Color terrainSpecular;

    public Double goalDistance;

    public Integer serverBotCount;
    public Integer localBotCount;

    public Integer matchSeconds;
    public Integer intermissionSeconds;

    public Double debug1;
    public Double debug2;
    public Double debug3;
    public Double debug4;
    public Double debug5;
    public Vector3 debugPosition;
    public Vector3 debugScale;
    public Vector3 debugRotation;


    public static void initScript() {
        ScriptEngineManager factory = new ScriptEngineManager();
        instance.jsEngine = factory.getEngineByName("js");
        instance.scriptFile = new File("assets/scripts/Settings.js");
        instance.runScript();
    }

    public static void runScript() {
        try {
            FileReader fileReader = new FileReader(instance.scriptFile);
            instance.jsEngine.eval(fileReader);
            fileReader.close();

            instance.verticalSync = (Boolean) (instance.jsEngine.eval("verticalSync"));
            instance.fov = (Integer) (instance.jsEngine.eval("fov"));

            instance.ambientColor = (Color)(instance.jsEngine.eval("ambientColor"));
            instance.diffuseColor = (Color)(instance.jsEngine.eval("diffuseColor"));
            instance.specularColor = (Color)(instance.jsEngine.eval("specularColor"));
            instance.lightDirection = (Vector3)(instance.jsEngine.eval("lightDirection"));

            instance.puckSpawnPoint = (Vector3)(instance.jsEngine.eval("puckSpawnPoint"));

            instance.terrainEmissive = (Color)(instance.jsEngine.eval("terrainEmissive"));
            instance.terrainSpecular = (Color)(instance.jsEngine.eval("terrainSpecular"));

            instance.serverBotCount = (Integer) (instance.jsEngine.eval("serverBotCount"));
            instance.localBotCount = (Integer) (instance.jsEngine.eval("localBotCount"));

            instance.goalDistance = (Double)(instance.jsEngine.eval("goalDistance"));

            instance.matchSeconds = (Integer) (instance.jsEngine.eval("matchSeconds"));
            instance.intermissionSeconds = (Integer) (instance.jsEngine.eval("intermissionSeconds"));

            instance.debug1 = (Double)(instance.jsEngine.eval("debug1"));
            instance.debug2 = (Double)(instance.jsEngine.eval("debug2"));
            instance.debug3 = (Double)(instance.jsEngine.eval("debug3"));
            instance.debug4 = (Double)(instance.jsEngine.eval("debug4"));
            instance.debug5 = (Double)(instance.jsEngine.eval("debug5"));

            instance.debugPosition =  (Vector3)(instance.jsEngine.eval("debugPosition"));
            instance.debugScale =  (Vector3)(instance.jsEngine.eval("debugScale"));
            instance.debugRotation =  (Vector3)(instance.jsEngine.eval("debugRotation"));

        } catch (FileNotFoundException e) {
            System.out.println(instance.scriptFile + " not found " + e);
        } catch (IOException e) {
            System.out.println("IO problem with " + instance.scriptFile + ": " + e);
        } catch (ScriptException e) {
            System.out.println("Settings Exception in " + instance.scriptFile + ": " + e);
        } catch (NullPointerException e) {
            System.out.println("Null pointer exception reading " + instance.scriptFile + ": " + e);
        }
    }

    public static Settings get() { return instance; }
}
