package myGameEngine.Controllers;

import a2.Actions.ActionMove;
import a2.Actions.ActionRotate;
import myGameEngine.Singletons.TimeManager;
import ray.rage.scene.ManualObject;
import ray.rage.scene.SceneNode;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Radianf;

public class NodeController extends AbstractController {
    private SceneNode node;

    public NodeController(SceneNode node) {
        this.node = node;
    }
    public void move(ActionMove.Direction direction, float speed) {
        float scaledSpeed = (float)(TimeManager.getDelta() / 200f) * speed;
        switch (direction) {
            case FORWARD:
                node.moveForward(scaledSpeed);
                break;
            case BACKWARD:
                node.moveBackward(scaledSpeed);
                break;
            case LEFT:
                // NOTE: reversed for some reason?
                node.moveRight(scaledSpeed);
                break;
            case RIGHT:
                // NOTE: reversed for some reason?
                node.moveLeft(scaledSpeed);
                break;
        }
    }

    public void rotate(ActionRotate.Direction direction, float speed) {
        float div = 700f;
        Radianf angle;
        switch (direction) {
            case X:
                angle = Radianf.createFrom((float)(TimeManager.getDelta() / -div) * speed);
                node.yaw(angle);
            case Y:
                angle = Radianf.createFrom((float)(TimeManager.getDelta() / div) * speed);
                node.pitch(angle);
                break;
        }
    }

    @Override
    protected void updateImpl(float v) { }
}
