package myGameEngine.Controllers;

import myGameEngine.Singletons.TimeManager;
import ray.rage.scene.Camera;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.*;

public class CameraController extends AbstractController {
    private Camera camera;

    public CameraController(Camera camera) {
        this.camera = camera;
    }

    private void move(float speed, Vector3 direction) {
        Vector3 position = camera.getPo();
        float moveSpeed = (float)(TimeManager.getDelta() / 200f) * speed;
        Vector3f movement = (Vector3f) Vector3f.createFrom(moveSpeed * direction.x(), moveSpeed * direction.y(), moveSpeed * direction.z());
        Vector3f finalPosition = (Vector3f) position.add(movement);
        camera.setPo((Vector3f) Vector3f.createFrom(finalPosition.x(), finalPosition.y(), finalPosition.z()));
    }

    public void moveForward(float speed) {
        move(speed, camera.getFd());
    }
    public void moveBackward(float speed) {
        move(speed, camera.getFd().mult(-1));
    }
    public void moveRight(float speed) {
        move(speed, camera.getRt());
    }
    public void moveLeft(float speed) {
        move(speed, camera.getRt().mult(-1));
    }

    public void rotateLeft(float speed) {
        float rotateSpeed = (float)(TimeManager.getDelta() / 700f) * speed;
        Matrix4f rotationYaw = (Matrix4f) Matrix4f.createRotationFrom(Radianf.createFrom(rotateSpeed), camera.getUp());
        Vector4f scalar = (Vector4f) Vector4f.createNormalizedFrom(camera.getFd());
        Vector4f scaledRotationYaw = (Vector4f) rotationYaw.mult(scalar);
        camera.setFd((Vector3f) Vector3f.createFrom(scaledRotationYaw.x(), scaledRotationYaw.y(), scaledRotationYaw.z()));

        Vector4f scalar2 = (Vector4f) Vector4f.createNormalizedFrom(camera.getRt());
        Vector4f scaledRotationYaw2 = (Vector4f) rotationYaw.mult(scalar2);
        camera.setRt((Vector3f) Vector3f.createFrom(scaledRotationYaw2.x(), scaledRotationYaw2.y(), scaledRotationYaw2.z()));
    }
    public void rotateRight(float speed) {
        rotateLeft(-speed);
    }

    public void rotateUp (float speed) {
        float rotateSpeed = (float)(TimeManager.getDelta() / 700f) * speed;
        Matrix4f rotationPitch = (Matrix4f) Matrix4f.createRotationFrom(Radianf.createFrom(rotateSpeed), camera.getRt());
        Vector4f scalar =(Vector4f) Vector4f.createNormalizedFrom(camera.getUp());
        Vector4f scaledRotationPitch = (Vector4f) rotationPitch.mult(scalar);
        camera.setUp((Vector3f) Vector3f.createFrom(scaledRotationPitch.x(), scaledRotationPitch.y(), scaledRotationPitch.z()));

        Vector4f scalar2 = (Vector4f) Vector4f.createNormalizedFrom(camera.getFd());
        Vector4f scaledRotationPitch2 = (Vector4f) rotationPitch.mult(scalar2);
        camera.setFd((Vector3f) Vector3f.createFrom(scaledRotationPitch2.x(), scaledRotationPitch2.y(), scaledRotationPitch2.z()));
    }
    public void rotateDown (float speed) {
        rotateUp(-speed);
    }


    public void rollRight(float speed) {
        float rotateSpeed = (float)(TimeManager.getDelta() / 700f) * speed;
        Matrix4f rotationRoll = (Matrix4f) Matrix4f.createRotationFrom(Radianf.createFrom(rotateSpeed), camera.getFd());
        Vector4f scalar = (Vector4f) Vector4f.createNormalizedFrom(camera.getRt());
        Vector4f scaledRotationRoll = (Vector4f) rotationRoll.mult(scalar);
        camera.setRt((Vector3f) Vector3f.createFrom(scaledRotationRoll.x(), scaledRotationRoll.y(), scaledRotationRoll.z()));

        Vector4f scalar2 = (Vector4f) Vector4f.createNormalizedFrom(camera.getUp());
        Vector4f scaledRotationRoll2 = (Vector4f) rotationRoll.mult(scalar2);
        camera.setUp((Vector3f) Vector3f.createFrom(scaledRotationRoll2.x(), scaledRotationRoll2.y(), scaledRotationRoll2.z()));
    }
    public void rollLeft(float speed) {
        rollRight(-speed);
    }

    public void setPosition(Vector3f position) {
        camera.setPo(position);
    }
    public void setRotation(Matrix3f rotation) {
        camera.setFd((Vector3f) rotation.column(0));
        camera.setUp((Vector3f) rotation.column(1));
        camera.setRt((Vector3f) rotation.column(2));

        Matrix4f rotationYaw = (Matrix4f) Matrix4f.createRotationFrom(Degreef.createFrom(-90), camera.getUp());
        Vector4f scalar = (Vector4f) Vector4f.createNormalizedFrom(camera.getFd());
        Vector4f scaledRotationYaw = (Vector4f) rotationYaw.mult(scalar);
        camera.setFd((Vector3f) Vector3f.createFrom(scaledRotationYaw.x(), scaledRotationYaw.y(), scaledRotationYaw.z()));

        Vector4f scalar2 = (Vector4f) Vector4f.createNormalizedFrom(camera.getRt());
        Vector4f scaledRotationPitch = (Vector4f) rotationYaw.mult(scalar2);
        camera.setRt((Vector3f) Vector3f.createFrom(scaledRotationPitch.x(), scaledRotationPitch.y(), scaledRotationPitch.z()));
    }

    @Override
    protected void updateImpl(float v) {

    }
}
