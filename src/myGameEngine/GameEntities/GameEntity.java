package myGameEngine.GameEntities;

import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rage.asset.material.Material;
import ray.rage.asset.mesh.Mesh;
import ray.rage.scene.*;

import java.util.ArrayList;

public class GameEntity {
    private boolean destroyed = false;

    // keep track of entities that need to be cleaned up / destroyed
    private ArrayList<SceneNode> nodeResponsibility = new ArrayList<>();
    private ArrayList<SceneObject> objectResponsibility = new ArrayList<>();
    private ArrayList<Material> materialResponsibility = new ArrayList<>();
    private ArrayList<GameEntity> gameEntityResponsibility = new ArrayList<>();
    private ArrayList<Light> lightResponsibility = new ArrayList<>();

    public GameEntity() {
        if (listedName() != null) {
            EntityManager.add(listedName(), this);
        }
    }

    public String listedName() { return null; }

    // keep track of entities that need to be cleaned up / destroyed
    public void addResponsibility(SceneNode node) { nodeResponsibility.add(node); }
    public void addResponsibility(SceneObject obj) { objectResponsibility.add(obj); }
    public void addResponsibility(Material material) { materialResponsibility.add(material); }
    public void addResponsibility(GameEntity gameEntity) { gameEntityResponsibility.add(gameEntity); }
    public void addResponsibility(Light light) { lightResponsibility.add(light); }

    public boolean isDestroyed() { return destroyed; }

    // destroy the entities this one is responsible for
    public void destroy() {
        if (destroyed) { return; }
        destroyed = true;

        SceneManager sm = EngineManager.getSceneManager();

        for (GameEntity gameEntity : gameEntityResponsibility) {
            gameEntity.destroy();
        }
        gameEntityResponsibility.clear();

        for (Material material : materialResponsibility) {
            sm.getMaterialManager().removeAssetByName(material.getName());
        }
        materialResponsibility.clear();

        for (SceneObject object : objectResponsibility) {
            object.detachFromParent();
            if (object instanceof ManualObject) {
                sm.destroyManualObject((ManualObject)object);
                sm.getMeshManager().removeAssetByName(object.getName() + Mesh.class.getSimpleName());
            } else if (object instanceof Entity) {
                sm.destroyEntity((Entity)object);
            } else {
                System.out.println("UNSUPPORTED SCENEOBJECT!");
            }
        }
        objectResponsibility.clear();

        for (Light light : lightResponsibility) {
            light.detachFromParent();
            sm.destroyLight(light);
        }
        lightResponsibility.clear();

        for (SceneNode node : nodeResponsibility) {
            if (node.getParent() != null) {
                node.getParent().detachChild(node);
            }
            sm.destroySceneNode(node);
        }
        nodeResponsibility.clear();

        if (listedName() != null) {
            EntityManager.remove(listedName(), this);
        }
    }
}
