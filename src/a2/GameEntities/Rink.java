package a2.GameEntities;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;

import a2.MyGame;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.rendersystem.Renderable;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.rendersystem.states.ZBufferState;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.io.IOException;

public class Rink extends GameEntity {
    public Rink() throws IOException {
        super(false);

        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Rink" + unique;

        node = sm.getRootSceneNode().createChildSceneNode(name + "Node");
        addResponsibility(node);

        Entity rinkGlass = sm.createEntity(name + "Glass", "rink_glass.obj");
        addResponsibility(rinkGlass);
        rinkGlass.setPrimitive(Renderable.Primitive.TRIANGLES);
        ZBufferState zbs = (ZBufferState)sm.getRenderSystem().createRenderState(RenderState.Type.ZBUFFER);
        zbs.setWritable(false);
        rinkGlass.setRenderState(zbs);
        node.attachObject(rinkGlass);

        Entity rink = sm.createEntity(name, "rink.obj");
        addResponsibility(rink);
        rink.setPrimitive(Renderable.Primitive.TRIANGLES);
        node.attachObject(rink);

        Entity rinkLines = sm.createEntity(name + "Lines", "rink_lines.obj");
        addResponsibility(rinkLines);
        rinkLines.setPrimitive(Renderable.Primitive.TRIANGLES);
        node.attachObject(rinkLines);

    }

    public SceneNode getNode() { return node; }
}

