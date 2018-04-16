package myGameEngine.Singletons;

import Networking.UDPClient;
import Networking.UDPServer;
import a2.GameEntities.Player;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import a2.Contollers.CharacterController;

import javax.vecmath.Vector3f;
import java.util.*;

public class HistoryManager {
    private static final HistoryManager instance = new HistoryManager();
    private static final int maxHistory = 144;
    private HistoryState[] states = new HistoryState[maxHistory];
    private int onState = 0;
    private boolean rewriting = false;
    private int DEBUGREMOVEME = 0;

    public static void rewrite(short rewindTo) {
        if (instance.states[instance.onState] == null) { return; }
        int amount = instance.states[instance.onState].tick - rewindTo + 1;
        if (amount > maxHistory) { amount = maxHistory; }
        if (amount <= 0) { return; }
        if (getState((short)(rewindTo - 1)) == null) { return; }

        // apply rewound state
        HistoryState state = getState(rewindTo);
        state.bodies.apply();
        state.controllers.apply();

        // set time and onstate backward
        // subtract by one since these will both be incremented immediately
        TimeManager.setTick((short)(rewindTo - 1));
        instance.onState = getState((short)(rewindTo - 1)).index;

        // resimulate physics
        instance.rewriting = true;
        instance.DEBUGREMOVEME = 1;
        //System.out.println("1: " + state.bodies.bodies.get(1).worldTransform.origin);
        PhysicsManager.stepSimulation((float)(amount / (1.0 * PhysicsManager.tickRate)));
        instance.rewriting = false;
    }

    public static void rewind(short rewindTo) {
        if (instance.states[instance.onState] == null) { return; }
        int amount = instance.states[instance.onState].tick - rewindTo;
        if (amount > maxHistory) { amount = maxHistory; }
        if (amount <= 0) { return; }

        // apply rewound state
        HistoryState state = getState(rewindTo);
        if (state == null) { return; }
        state.bodies.apply();
        state.controllers.apply();

        // set time and onstate backward
        TimeManager.setTick(rewindTo);
        instance.onState = state.index;
    }

    public static void fastForward(short fastForwardTo, boolean rewrite) {
        if (instance.states[instance.onState] == null) { return; }
        int amount = fastForwardTo - instance.states[instance.onState].tick;
        if (amount > maxHistory) { amount = maxHistory; }
        if (amount <= 0) { return; }

        if (rewrite) { instance.rewriting = true; }
        System.out.println("FF AMT: " + amount);
        PhysicsManager.stepSimulation((float)(amount / (1.0 * PhysicsManager.tickRate)));
        if (rewrite) { instance.rewriting = false; }
    }

    public static HistoryState getState(short atTick) {
        HistoryState state = instance.states[instance.onState];
        if (state == null) { return null; }

        short onTick = state.tick;

        int dist = atTick - onTick;
        if (Math.abs(dist) >= maxHistory) { return null; }

        state = instance.states[(maxHistory + instance.onState + dist) % maxHistory];
        if (state != null && state.tick != atTick) {
            System.out.println("TIX MISMATCH: " + atTick + " != " + state.tick + ", DIST: " + dist + ", ONTICK: " + onTick);
            return null;
        }

        return state;
    }

    public static void internalTick(float timeStep) {
        // increment on state index
        instance.onState = (instance.onState + 1) % maxHistory;

        // apply history of player inputs
        if (instance.rewriting) {
            if (instance.states[instance.onState] != null) {
                instance.states[instance.onState].controllers.applyInput();
            }
        }

        HistoryState oldState = instance.states[instance.onState];
        instance.states[instance.onState] = new HistoryState(instance.onState);

        if (instance.DEBUGREMOVEME > 0) {
            if (--instance.DEBUGREMOVEME == 0) {
                Vector3f o1 = oldState.bodies.bodies.get(1).worldTransform.origin;
                Vector3f o2 = instance.states[instance.onState].bodies.bodies.get(1).worldTransform.origin;
                if (o1.x != o2.x || o1.y != o2.y || o1.z != o2.z) {
                    System.out.println("2: " + oldState.bodies.bodies.get(1).worldTransform.origin);
                    System.out.println("3: " + instance.states[instance.onState].bodies.bodies.get(1).worldTransform.origin);
                }
            }
        }

        if (instance.rewriting || UDPClient.hasClient()) {
            /*System.out.println("-------");
            System.out.println(oldState.tick + ": " + oldState.bodies.bodies.get(1).worldTransform.origin);
            System.out.println(instance.states[instance.onState].tick + ": " + instance.states[instance.onState].bodies.bodies.get(1).worldTransform.origin);

            for (Object p : EntityManager.get("player")) {
                Player player = (Player) p;
                if (((Player) p).getId() == 0) { continue; }
                Transform t = new Transform();
                player.getBody().getWorldTransform(t);
                System.out.println("Tick: " + TimeManager.getTick() + ", " + player.getController().getControls() + ", " + t.origin);
            }*/
        }
    }

    public static class HistoryState {
        public int index;
        public short tick = TimeManager.getTick();
        public HistoryRigidBodies bodies = new HistoryRigidBodies();
        public HistoryCharacterControllers controllers = new HistoryCharacterControllers();
        public HistoryState(int index) { this.index = index; }
    }

    private static class HistoryRigidBodies {
        private ArrayList<HistoryRigidBody> bodies;
        //private float localTime;

        private HistoryRigidBodies() {
            bodies = new ArrayList<>();
            //localTime = PhysicsManager.getWorld().getLocalTime();
            for (RigidBody rb : PhysicsManager.getRigidBodies()) {
                HistoryRigidBody hrb = new HistoryRigidBody();
                hrb.rb = rb;
                rb.getWorldTransform(hrb.worldTransform);
                rb.getLinearVelocity(hrb.linearVelocity);
                rb.getAngularVelocity(hrb.angularVelocity);
                bodies.add(hrb);
            }
        }

        private void apply() {
            PhysicsManager.resetWorld();
            //PhysicsManager.getWorld().setLocalTime(localTime);
            /*for (HistoryRigidBody hrb : bodies) {
                PhysicsManager.removeRigidBody(hrb.rb);
            }*/

            for (HistoryRigidBody hrb : bodies) {
                RigidBody rb = hrb.rb;


                Transform worldTransform = new Transform();
                worldTransform.set(hrb.worldTransform);
                //rb.setWorldTransform(worldTransform);
                rb.proceedToTransform(worldTransform);

                Vector3f linearVelocity = new Vector3f();
                linearVelocity.set(hrb.linearVelocity);
                rb.setLinearVelocity(linearVelocity);

                Vector3f angularVelocity = new Vector3f();
                angularVelocity.set(hrb.angularVelocity);
                rb.setAngularVelocity(angularVelocity);

                rb.getMotionState().setWorldTransform(worldTransform);
            }
            PhysicsManager.getWorld().synchronizeMotionStates();
            /*for (HistoryRigidBody hrb : bodies) {
                PhysicsManager.addRigidBody(hrb.rb);
            }*/
        }

        private class HistoryRigidBody {
            public RigidBody rb = null;
            public Transform worldTransform = new Transform();
            public Vector3f linearVelocity = new Vector3f();
            public Vector3f angularVelocity = new Vector3f();
        }
    }

    public static class HistoryCharacterControllers {
        private HashMap<Player, CharacterController.HistoryCharacterController> controllers = new HashMap<>();

        private HistoryCharacterControllers() {
            for (Object p : EntityManager.get("player")) {
                Player player = (Player) p;
                controllers.put(player, player.getController().remember());
            }
        }

        public CharacterController.HistoryCharacterController getHistory(Player player) {
            return controllers.get(player);
        }

        private void apply() {
            for (CharacterController.HistoryCharacterController hcc : controllers.values()) {
                hcc.apply();
            }
        }

        private void applyInput() {
            for (CharacterController.HistoryCharacterController hcc : controllers.values()) {
                hcc.applyInput();
            }
        }
    }
}
