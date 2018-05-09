package a3.GameEntities;

import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Matrix3f;
import ray.rml.Quaternion;
import ray.rml.Vector3;

import java.io.IOException;

public class GoalSize {
    private SceneNode size;
    private static GoalSize goal = null;
    public static GoalSize GetGoalSize() throws IOException {
        if(goal == null){
            return new GoalSize();
        }else
            return goal;
    }
    private GoalSize() throws IOException {
        size = EngineManager.getSceneManager().getRootSceneNode().createChildSceneNode("size");
        Entity box = EngineManager.getSceneManager().createEntity("sizeEntity","cube.obj");
        size.attachObject(box);
        update();
    }

    public void update(){
        Settings.runScript();
        size.setLocalPosition(Settings.get().debugPosition);
        size.setLocalScale(Settings.get().debugScale);
        size.setLocalRotation(Matrix3f.createFrom(Settings.get().debugRotation.x(), Settings.get().debugRotation.y(), Settings.get().debugRotation.z()));
    }
}
