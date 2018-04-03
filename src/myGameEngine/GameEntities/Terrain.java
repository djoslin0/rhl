package myGameEngine.GameEntities;

import myGameEngine.Singletons.EngineManager;
import ray.rage.Engine;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.Tessellation;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class Terrain extends GameEntity {
    private Tessellation tessellation;

    public Terrain() {
        super(false);
        SceneManager sm = EngineManager.getSceneManager();
        tessellation = sm.createTessellation("tessellation", 0);
        tessellation.setSubdivisions(0f);

        SceneNode node = sm.getRootSceneNode().createChildSceneNode("tesselationNode");
        node.scale(300, 1500, 300);
        node.moveDown(3f);
        node.attachObject(tessellation);

        Engine engine = EngineManager.getEngine();
        tessellation.setHeightMap(engine, "terrain.png");
        tessellation.setTexture(engine, "terrain.png");
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
