package myGameEngine.Controllers;

import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.TimeManager;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Radianf;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class OrbitCameraController extends AbstractController implements Camera.Listener {
    private Camera camera;
    private SceneNode cameraNode;
    private SceneNode targetNode;
    private float azimuth;
    private float elevation;
    private float radius;
    private Vector3 worldUp;
    public boolean attachYaw = true;

    public enum OrbitAxis { AZIMUTH, ELEVATION, RADIUS };

    public OrbitCameraController(Camera camera, SceneNode targetNode) {
        this.camera = camera;
        this.targetNode = targetNode;
        azimuth = 180f;
        elevation = 20.0f;
        radius = 6.0f;
        worldUp = Vector3f.createFrom(0.0f, 1.0f, 0.0f);

        SceneNode rootNode = EngineManager.getSceneManager().getRootSceneNode();
        cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
        cameraNode.attachObject(camera);
        camera.setMode('n');
        camera.addListener(this);
    }

    public void input(OrbitAxis axis, float amount, boolean timeScale) {
        float div = 10f;
        if (timeScale) {
            amount = (float) TimeManager.getDelta() / div * amount;
        }
        switch (axis) {
            case AZIMUTH:
                azimuth = (azimuth + amount) % 360;
                break;
            case ELEVATION:
                elevation = (elevation + amount) % 360;
                if (elevation > 89.9f) { elevation = 89.9f; }
                if (elevation < -89.9f) { elevation = -89.9f; }
                break;
            case RADIUS:
                radius += amount;
                if (radius > 10) { radius = 10f; }
                if (radius < 1) { radius = 1f; }
                break;
        }

        updateCameraPosition();
    }

    public void updateCameraPosition() {
        double theta = Math.toRadians(azimuth);
        double phi = Math.toRadians(elevation);
        double x = radius * Math.cos(phi) * Math.sin(theta);
        double y = radius * Math.sin(phi);
        double z = radius * Math.cos(phi) * Math.cos(theta);
        cameraNode.setLocalPosition(Vector3f.createFrom((float)x, (float)y, (float)z).add(targetNode.getWorldPosition()));
        cameraNode.lookAt(targetNode, worldUp);
    }

    @Override
    public void onCameraPreRenderScene(Camera camera) {
        updateCameraPosition();
    }

    @Override
    protected void updateImpl(float v) {}

    @Override
    public void onCameraPostRenderScene(Camera camera) { }

}
