package a2.GameEntities;

import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
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
        Entity box = EngineManager.getSceneManager().createEntity("sizeEntity","goalsize.obj");
        size.attachObject(box);
        size.scale(Settings.get().scaleX.floatValue(), (Float) Settings.get().scaleY.floatValue(),(Float) Settings.get().scaleZ.floatValue());
        size.setLocalPosition(Settings.get().goalLocation);

    }
    public void setSize(){
        Settings.runScript();
        size.setLocalPosition(Settings.get().goalLocation);
    }
    public void setScale(){
        Settings.runScript();
        size.setLocalScale(Settings.get().scaleX.floatValue(), (Float) Settings.get().scaleY.floatValue(),(Float) Settings.get().scaleZ.floatValue());
    }
}
