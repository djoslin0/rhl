package myGameEngine.GameEntities;

import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.Settings;
import ray.rage.Engine;
import ray.rage.asset.material.Material;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.TextureState;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.Tessellation;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;

public class Terrain extends GameEntity {
    private Tessellation tessellation;

    public Terrain() {
        super(false);
        SceneManager sm = EngineManager.getSceneManager();
        tessellation = sm.createTessellation("tessellation", 0);
        tessellation.setSubdivisions(0f);

        SceneNode node = sm.getRootSceneNode().createChildSceneNode("tesselationNode");
        node.scale(400, 1800, 400);
        node.moveDown(3f);
        node.yaw(Degreef.createFrom(180f));
        node.attachObject(tessellation);

        Engine engine = EngineManager.getEngine();
        tessellation.setHeightMap(engine, "terrain.png");
        tessellation.setTexture(engine, "snow.png");

        Material mat = engine.getMaterialManager().createManualAsset("tessellationMaterial");
        mat.setEmissive(Settings.get().terrainEmissive);
        mat.setSpecular(Settings.get().terrainSpecular);
        tessellation.getTessellationBody().setMaterial(mat);
    }

    @Override
    public String listedName() { return "terrain"; }

    public float getHeight(Vector3 location) {
        return tessellation.getWorldHeight(location.x(), location.z());
    }

    public Vector3 getNormal(Vector3 location, Vector3 forward, Vector3 right) {
        Vector3 vOrigin = Vector3f.createFrom(location.x(), getHeight(location), location.z());
        Vector3 vForward = location.add(forward);
        vForward = Vector3f.createFrom(vForward.x(), getHeight(vForward), vForward.z());
        Vector3 vRight = location.add(right);
        vRight = Vector3f.createFrom(vRight.x(), getHeight(vRight), vRight.z());
        return vForward.sub(vOrigin).cross((Vector3)vRight.sub(vOrigin));
    }
}
