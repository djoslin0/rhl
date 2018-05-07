package myGameEngine.GameEntities;

import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.InternalTickCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;
import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UpdateManager;
import ray.rage.asset.material.Material;
import ray.rage.asset.mesh.Mesh;
import ray.rage.scene.*;

import java.util.ArrayList;

public class GameEntity implements Updatable {
    protected SceneNode node;
    private boolean destroyed = false;

    // keep track of entities that need to be cleaned up / destroyed
    protected ArrayList<SceneNode> nodeResponsibility = new ArrayList<>();
    protected ArrayList<SceneObject> objectResponsibility = new ArrayList<>();
    protected ArrayList<Material> materialResponsibility = new ArrayList<>();
    protected ArrayList<GameEntity> gameEntityResponsibility = new ArrayList<>();
    protected ArrayList<Light> lightResponsibility = new ArrayList<>();
    protected ArrayList<RigidBody> bodyResponsibility = new ArrayList<>();
    protected ArrayList<SoundGroup> soundResponsibility = new ArrayList<>();
    protected ArrayList<Updatable> updatableResponsibility = new ArrayList<>();
    protected ArrayList<InternalTickCallback> tickResponsibility = new ArrayList<>();

    public GameEntity(boolean updatable) {
        if (updatable) {
            UpdateManager.add(this);
        }
        if (listedName() != null) {
            EntityManager.add(listedName(), this);
        }
    }

    public String listedName() { return null; }
    public boolean shouldRegisterCollision() { return false; }

    public SceneNode getNode() { return node; }

    // keep track of entities that need to be cleaned up / destroyed
    public void addResponsibility(SceneNode node) { nodeResponsibility.add(node); }
    public void addResponsibility(SceneObject obj) { objectResponsibility.add(obj); }
    public void addResponsibility(Material material) { materialResponsibility.add(material); }
    public void addResponsibility(GameEntity gameEntity) { gameEntityResponsibility.add(gameEntity); }
    public void addResponsibility(Light light) { lightResponsibility.add(light); }
    public void addResponsibility(RigidBody body) { bodyResponsibility.add(body); }
    public void addResponsibility(SoundGroup sound) { soundResponsibility.add(sound); }
    public void addResponsibility(Updatable updatable) { updatableResponsibility.add(updatable); }
    public void addResponsibility(InternalTickCallback updatable) { tickResponsibility.add(updatable); }

    public boolean isDestroyed() { return destroyed; }

    // destroy the entities this one is responsible for
    public void destroy() {
        if (destroyed) { return; }
        destroyed = true;

        // disable update calls
        UpdateManager.remove(this);

        for (Updatable updatable : updatableResponsibility) {
            UpdateManager.remove(updatable);
        }

        for (InternalTickCallback c : tickResponsibility) {
            PhysicsManager.removeCallback(c);
        }

        // clean up responsibilities
        SceneManager sm = EngineManager.getSceneManager();

        // game entities
        for (GameEntity gameEntity : gameEntityResponsibility) {
            gameEntity.destroy();
        }
        gameEntityResponsibility.clear();

        // materials
        for (Material material : materialResponsibility) {
            sm.getMaterialManager().removeAssetByName(material.getName());
        }
        materialResponsibility.clear();

        // objects
        for (SceneObject object : objectResponsibility) {
            object.detachFromParent();
            if (object instanceof ManualObject) {
                sm.destroyManualObject((ManualObject)object);
                sm.getMeshManager().removeAssetByName(object.getName() + Mesh.class.getSimpleName());
            } else if (object instanceof Entity) {
                try {
                    sm.destroyEntity((Entity) object);
                } catch (Exception ex) {}
            } else {
                System.out.println("UNSUPPORTED SCENEOBJECT!");
            }
        }
        objectResponsibility.clear();

        // bodies
        for (RigidBody body : bodyResponsibility) {
            PhysicsManager.unregisterCollision(body);
            PhysicsManager.removeRigidBody(body);
            body.destroy();
        }
        bodyResponsibility.clear();

        // lights
        for (Light light : lightResponsibility) {
            light.detachFromParent();
            sm.destroyLight(light);
        }
        lightResponsibility.clear();

        // nodes
        for (SceneNode node : nodeResponsibility) {
            if (node.getParent() != null) {
                node.getParent().detachChild(node);
            }
            sm.destroySceneNode(node);
        }
        nodeResponsibility.clear();

        // sounds
        for (SoundGroup sound : soundResponsibility) {
            sound.destroy();
        }
        soundResponsibility.clear();

        // have entitiy manager forget about this entity
        if (listedName() != null) {
            EntityManager.remove(listedName(), this);
        }
    }

    protected RigidBody createBody(float mass, MotionState motionState, CollisionShape collisionShape, short group, short mask) {
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0f, 0);
        if (mass > 0) {
            collisionShape.calculateLocalInertia(mass, localInertia);
        }

        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia);
        RigidBody body = new RigidBody(rbInfo);
        body.setUserPointer(this);

        if (group == -1 && mask == -1) {
            PhysicsManager.addRigidBody(body);
        } else {
            PhysicsManager.addRigidBody(body, group, mask);
        }
        addResponsibility(body);

        if (shouldRegisterCollision()) {
            PhysicsManager.registerCollision(body);
        }

        return body;
    }

    protected RigidBody createBody(float mass, MotionState motionState, CollisionShape collisionShape) {
        return createBody(mass, motionState, collisionShape, (short)-1, (short)-1);
    }

    public void collision(GameEntity entity, ManifoldPoint contactPoint, boolean isA) { }

    @Override
    public void update(float delta) { }

    @Override
    public boolean blockUpdates() { return false; }

}
