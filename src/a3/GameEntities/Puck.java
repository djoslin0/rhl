package a3.GameEntities;

import Networking.UDPClient;
import a3.GameState;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.MotionStateController;
import myGameEngine.GameEntities.GameEntity;
import myGameEngine.GameEntities.LightFade;
import myGameEngine.GameEntities.Particle;
import myGameEngine.Helpers.BulletConvert;
import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Singletons.*;
import ray.rage.rendersystem.Renderable;
import ray.rage.scene.Entity;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rml.*;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class Puck extends GameEntity implements Attackable {
    private Entity obj;
    private RigidBody body;
    private SceneNode angularTestNode;
    private PuckParticle[] particles = new PuckParticle[8];
    private Vector3 averageLinearVelocity = Vector3f.createZeroVector();
    private int blockGoal = 0;

    private boolean dunk = false;
    private CollisionBox dunkBox1;
    private  CollisionBox dunkBox2;
    private boolean dunked = false;

    private float angularPushScale = 400f;
    private float linearPushScale = 200f;
    private float mass = 1000f;

    private float freezeTime = 0;
    private float rinkSlideTimeout = 0;
    private float iceImpactTimeout = 0;
    private float rinkImpactTimeout = 0;
    private ArrayList<Player> playerImpact = new ArrayList();
    private ArrayList<Float> playerImpactTimeout = new ArrayList();

    private SoundGroup explosionSound;
    private SoundGroup cheerSound;
    private SoundGroup iceSound;
    private SoundGroup rinkSound;
    private SoundGroup slideSound;
    private SoundGroup spinSound;

    public Puck(Vector3 location) throws IOException {
        super(true);
        SceneManager sm = EngineManager.getSceneManager();

        long unique = UniqueCounter.next();
        String name = "Puck" + unique;

        obj = sm.createEntity(name, "puck.obj");
        addResponsibility(obj);
        obj.setPrimitive(Renderable.Primitive.TRIANGLES);

        node = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "Node");
        addResponsibility(node);
        node.attachObject(obj);
        node.setLocalPosition(location);

        angularTestNode = sm.getRootSceneNode().createChildSceneNode(obj.getName() + "TestNode");
        addResponsibility(angularTestNode);
        dunkBox1 = new CollisionBox(8f,2f,16f, Vector3f.createFrom(83f,5.9f,0.0f));
        dunkBox2 = new CollisionBox(8f,2f,16f, Vector3f.createFrom(-83f,5.9f,0.0f));
        initPhysics();
        for(int i =0; i<8; i++){
            particles[i] = new PuckParticle(i);
        }

        explosionSound = AudioManager.get().explosion.clone(node);
        addResponsibility(explosionSound);

        cheerSound = AudioManager.get().cheer.clone(null);
        addResponsibility(cheerSound);

        iceSound = AudioManager.get().ice.clone(node);
        addResponsibility(iceSound);

        rinkSound = AudioManager.get().rink.clone(node);
        addResponsibility(rinkSound);

        slideSound = AudioManager.get().slide.clone(node);
        addResponsibility(slideSound);
        slideSound.play(false);

        spinSound = AudioManager.get().spin.clone(node);
        addResponsibility(spinSound);
        spinSound.play(false);

        // set puck position on start
        body.setLinearVelocity(new javax.vecmath.Vector3f());
        body.setAngularVelocity(new javax.vecmath.Vector3f());
        Transform t = new Transform();
        t.origin.x = 0f;
        t.origin.y = 25f;
        t.origin.z = 0;
        node.setLocalPosition(0, 25, 0);
        body.setWorldTransform(t);
        body.clearForces();
        freeze();
    }

    private void initPhysics() {
        MotionStateController motionState = new MotionStateController(this.node);
        ConvexHullShape collisionShape = BulletConvert.entityToConvexHullShape(obj);
        collisionShape.setLocalScaling(node.getLocalScale().toJavaX());

        body = createBody(mass, motionState, collisionShape, PhysicsManager.COL_PUCK, PhysicsManager.COLLIDE_ALL);
        body.setRestitution(0.6f);
        body.setFriction(0.2f);
        body.setDamping(0.05f, 0f);
        body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
    }

    @Override
    public boolean shouldRegisterCollision() { return true; }

    public void reset(boolean scored, boolean dunk) {
        Player.Team team = (node.getWorldPosition().x() < 0) ? Player.Team.Blue : Player.Team.Orange;

        dunked = dunk;
        try {
            Color powColor = (team == Player.Team.Blue) ? new Color(255, 230, 170) : new Color(170, 170, 255);
            if (!scored) { powColor = Color.WHITE; }

            Particle pow = new Particle(10f, 10f, node.getWorldPosition(), Vector3f.createZeroVector(), "pow2.png", powColor, 300f);
            if (scored) { new GoalText(team, dunk); }

            new LightFade(pow.getNode(), powColor, 100f, 0.01f, 300f);
            if (!isFrozen()) {
                for (int i = 0; i < 8; i++) {
                    particles[i].startPhysics();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        body.setLinearVelocity(new javax.vecmath.Vector3f());
        body.setAngularVelocity(new javax.vecmath.Vector3f());
        Transform t = new Transform();
        t.origin.x = 0f;
        t.origin.y = 25f;
        t.origin.z = 0;
        node.setLocalPosition(0, 25, 0);

        body.setWorldTransform(t);
        body.clearForces();

        try {
            new Particle(10f, 10f, Vector3f.createFrom(0, 25f, 0), Vector3f.createZeroVector(), "pow2.png", Color.WHITE, 300f);
        } catch (IOException e) {
            e.printStackTrace();
        }

        explosionSound.play(false);
        if (scored && dunk) {
            cheerSound.play(false);
            cheerSound.setPitch(EntityManager.getLocalPlayer().getSide() == team ? 1 : 0.7f);
        }

        freeze();
    }

    private void freeze() {
        freezeTime = 5000f;
        if (UDPClient.hasClient()) { freezeTime *= 2; }
        body.setMassProps(0, new javax.vecmath.Vector3f(0, 0f, 0));
        Transform t = new Transform();
        body.getWorldTransform(t);
        body.getMotionState().setWorldTransform(t);
    }

    public void unfreeze() {
        freezeTime = 0f;
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0f, 0);
        body.getCollisionShape().calculateLocalInertia(mass, localInertia);
        body.setMassProps(mass, localInertia);

    }

    public void goalCollision(Player.Team team) {
        if (!UDPClient.hasClient() && !isFrozen() && !GameState.isMatchOver() && blockGoal == 0) {
            if (dunk) {
                GameState.addScore(team, 3);
            } else {
                GameState.addScore(team, 1);
            }
            blockGoal = 10;
        }

        reset(true, dunk);
    }

    private void showImpactParticle(Vector3 point, Vector3 velocity, float size) {
        try {
            new Particle(size, size, point, velocity, "pow2.png", Color.white, 200f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playerCollision(GameEntity entity, ManifoldPoint contactPoint, boolean isA) {

        Player player = (Player) entity;
        Vector3 entityPosition = entity.getNode().getWorldPosition();
        Vector3 thisPosition = node.getWorldPosition();

        // get linear velocity
        javax.vecmath.Vector3f linearVelocityJavaX = new javax.vecmath.Vector3f();
        body.getLinearVelocity(linearVelocityJavaX);
        Vector3 linearVelocity = Vector3f.createFrom(linearVelocityJavaX);

        // get angular velocity
        javax.vecmath.Vector3f angularVelocityJavaX = new javax.vecmath.Vector3f();
        body.getAngularVelocity(angularVelocityJavaX);
        Vector3 angularVelocity = Vector3f.createFrom(angularVelocityJavaX);

        // calculate angular push vector
        float dist = entityPosition.sub(thisPosition).length();
        angularTestNode.setLocalPosition(thisPosition);
        angularTestNode.lookAt(entity.getNode());
        angularTestNode.pitch(Radianf.createFrom(angularVelocity.x() / 5f));
        angularTestNode.yaw(Radianf.createFrom(angularVelocity.y() / 5f));
        angularTestNode.roll(Radianf.createFrom(angularVelocity.z() / 5f));
        angularTestNode.moveForward(dist);
        Vector3 angularPush = angularTestNode.getWorldPosition().sub(entityPosition).mult(angularPushScale);

        // calculate linear push vector
        Vector3 linearPush = Vector3f.createZeroVector();
        Vector3 diff = entityPosition.sub(thisPosition);
        if (linearVelocity.lengthSquared() > 0) {
            float dot = linearVelocity.normalize().dot(diff.normalize());
            if (dot > 0.5f) { dot = 0.5f; }
            dot = dot * 2f;
            if (dot > 0) {
                linearPush = linearVelocity.mult(linearPushScale * dot);
            }
        }

        // push player
        Vector3 push = angularPush.add(linearPush);
        Vector3 collisionPoint = Vector3f.createFrom(contactPoint.positionWorldOnA).add(Vector3f.createFrom(contactPoint.positionWorldOnB)).div(2f);
        player.getController().knockback(push, collisionPoint.sub(player.getPosition()));

        // calculate player hurt/squeeze
        float linearDot = linearPush.dot(player.getVelocity()) / 45000;
        int hurtAmount = (int)(linearDot + angularVelocity.length() * 2f);

        // create small pow particle
        if (!playerImpact.contains(player) && hurtAmount > 5 && player.willHurt(hurtAmount)) {
            playerImpact.add(player);
            playerImpactTimeout.add(250f);
            float size = 1 + hurtAmount / 15f;
            if (size > 2) { size = 2; }
            showImpactParticle(Vector3f.createFrom(contactPoint.positionWorldOnB), push.mult(0.000014f), size);
        }

        if (hurtAmount > 0) {
            // squeezekill?

            // figure out if we should be squishing downward
            float yScalar = 1;
            Vector3 playerLocalPoint = isA ? Vector3f.createFrom(contactPoint.localPointB) : Vector3f.createFrom(contactPoint.localPointA);
            if (playerLocalPoint.y() > 1f && linearVelocity.y() < -5) {
                yScalar = Player.height * 0.9f;
            } else {
                yScalar = 0;
            }

            // create collision direction vector
            Vector3 diff2 = Vector3f.createFrom(contactPoint.positionWorldOnA).sub(Vector3f.createFrom(contactPoint.positionWorldOnB));
            if (diff2.length() < 0.01) { diff2 = node.getWorldPosition().sub(player.getNode().getWorldPosition()); }
            diff2 = diff2.normalize().mult(2.5f);
            if (!isA) { diff2 = diff2.mult(-1f); }
            diff2 = Vector3f.createFrom(diff2.x(), diff2.y() * yScalar, diff2.z());

            // create start/end vectors for ray trace
            javax.vecmath.Vector3f start = isA ? Vector3f.createFrom(contactPoint.positionWorldOnB).toJavaX() : Vector3f.createFrom(contactPoint.positionWorldOnA).toJavaX();
            javax.vecmath.Vector3f end = Vector3f.createFrom(start).add(diff2).toJavaX();

            // raytrace squeezekill
            CollisionWorld.ClosestRayResultCallback squeezeKill = new CollisionWorld.ClosestRayResultCallback(start, end);
            squeezeKill.collisionFilterMask = PhysicsManager.COLLIDE_WORLD;
            PhysicsManager.getWorld().rayTest(start, end, squeezeKill);

            if (squeezeKill.hasHit() && hurtAmount > 4) {
                // squeezekill
                player.hurt(100);
            } else {
                // hurt player
                player.hurt(hurtAmount);
            }
        }
    }

    private void rinkCollision(GameEntity entity, ManifoldPoint contactPoint, boolean isA) {
        Vector3 rinkPoint = isA ? Vector3f.createFrom(contactPoint.positionWorldOnB) : Vector3f.createFrom(contactPoint.positionWorldOnA);
        if (rinkPoint.y() <= 1f) {
            if (iceImpactTimeout <= 0) {
                int volume = (int) ((averageLinearVelocity.length() / 30f) * 100f);
                if (volume > 100) { volume = 100; }
                iceSound.play(volume,false);
                showImpactParticle(rinkPoint, Vector3f.createZeroVector(), volume / 110f + 0.15f);
            }
            iceImpactTimeout = 500;
        } else {
            if (rinkImpactTimeout <= 0) {
                int volume = (int) ((averageLinearVelocity.length() / 60f) * 100f);
                if (volume > 100) { volume = 100; }
                rinkSound.play(volume,false);
                showImpactParticle(rinkPoint, Vector3f.createZeroVector(), volume / 70f + 0.15f);
            }
            rinkImpactTimeout = 500;
        }

        rinkSlideTimeout = 50;
    }

    public void collision(GameEntity entity, ManifoldPoint contactPoint, boolean isA) {
        if (isFrozen()) { return; }
        if (entity instanceof Goal && !UDPClient.hasClient()) {
            goalCollision((node.getWorldPosition().x() < 0) ? Player.Team.Blue : Player.Team.Orange);
        } else if (entity instanceof Player) {
            playerCollision(entity, contactPoint, isA);
        } else if (entity instanceof Rink) {
            rinkCollision(entity, contactPoint, isA);
        }
    }

    public void attacked(Vector3 aim, Vector3 relative) {
        javax.vecmath.Vector3f jvel = new javax.vecmath.Vector3f();
        body.getLinearVelocity(jvel);
        Vector3 velocity = Vector3f.createFrom(jvel);
        float dot = velocity.dot(aim.normalize()) * 1.45f;
        float rally = 0;
        if (dot < 0) {
            rally = -dot;
            if (rally > 50f) { rally = 50f; }
        }
        javax.vecmath.Vector3f force = aim.mult(18000f + rally * 1000f).toJavaX();
        body.applyImpulse(force, relative.toJavaX());
        body.activate();
    }

    @Override
    public byte getId() {
        return 0;
    }

    public String listedName() { return "puck"; }
    public SceneNode getNode() { return node; }
    public RigidBody getBody() { return body; }
    public boolean isFrozen() { return freezeTime > 0; }
    public boolean wasDunked() { return dunked; }

    public Vector3 getLinearVelocity() {
        javax.vecmath.Vector3f linearVelocity = new javax.vecmath.Vector3f();
        body.getLinearVelocity(linearVelocity);
        return Vector3f.createFrom(linearVelocity);
    }

    @Override
    public void update(float delta) {
        // dunk detection
        Vector2 contained = Vector2f.createFrom(node.getLocalPosition().x(),node.getLocalPosition().z());
        if (dunkBox1.contains(node.getLocalPosition()) || dunkBox2.contains(node.getLocalPosition())) {
            dunk = true;
        } else if (dunkBox1.Contains2d(contained) || dunkBox2.Contains2d(contained)){
            if (dunk && (dunkBox1.below(node.getWorldPosition()) || dunkBox2.below(node.getWorldPosition()))) {
                goalCollision((node.getWorldPosition().x() < 0) ? Player.Team.Blue : Player.Team.Orange);
            }
        } else {
            dunk = false;
        }

        // freeze puck
        if (!GameState.isMatchOver() && freezeTime > 0) {
            freezeTime -= delta;
            if (freezeTime <= 0) {
                unfreeze();
            }
        }

        // slide sound
        Vector3 linearVelocity = getLinearVelocity();
        int slideVolume = (int)((linearVelocity.length() / 30f - 0.1f) * 60);
        if (slideVolume < 0) { slideVolume = 0; }
        if (slideVolume > 60) { slideVolume = 60; }
        if (rinkSlideTimeout <= 0) {
            rinkSlideTimeout = 0;
            slideVolume = 0;
        } else {
            rinkSlideTimeout -= delta;
        }
        float slidePitch = Math.min(linearVelocity.length() / 30f + 0.5f, 1f);
        slidePitch += (float)Math.sin(TimeManager.getElapsed() / 100f) * 0.1f;
        slideSound.setVolume(slideVolume);
        slideSound.setPitch(slidePitch);

        // spin sound
        javax.vecmath.Vector3f angularVelocity = new javax.vecmath.Vector3f();
        body.getAngularVelocity(angularVelocity);
        int spinVolume = (int)(Math.abs(angularVelocity.y) * 1f + Math.abs(angularVelocity.x) * 2.5f + Math.abs(angularVelocity.z) * 2.5f - 3f);
        if (spinVolume < 0) { spinVolume = 0; }
        if (spinVolume > 30) { spinVolume = 30; }
        float spinPitch = 0.25f + Math.abs(angularVelocity.y) / 20f + Math.abs(angularVelocity.x) / 20f + Math.abs(angularVelocity.z) / 20f;
        if (spinPitch > 20f) { spinPitch = 20f; }
        spinSound.setVolume(spinVolume);
        spinSound.setPitch(spinPitch);

        // deal with timeouts
        if (iceImpactTimeout <= 0) {
            iceImpactTimeout = 0;
        } else {
            iceImpactTimeout -= delta;
        }

        if (rinkImpactTimeout <= 0) {
            rinkImpactTimeout = 0;
        } else {
            rinkImpactTimeout -= delta;
        }

        if (blockGoal > 0) { blockGoal--; }

        ArrayList<Integer> removePlayerImpact = new ArrayList<>();
        for (int i = 0; i < playerImpact.size(); i++) {
            float timeout = playerImpactTimeout.get(i) - delta;
            playerImpactTimeout.set(i, timeout);
            if (timeout <= 0) { removePlayerImpact.add(i); }
        }
        for (int i = removePlayerImpact.size() - 1; i >= 0; i--) {
            int index = removePlayerImpact.get(i);
            playerImpact.remove(index);
            playerImpactTimeout.remove(index);
        }

        // update average velocity
        averageLinearVelocity = averageLinearVelocity.lerp(linearVelocity, 0.2f);
    }

}

