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

    public static void rewind(short rewindTo) {
        int amount = instance.states[instance.onState].tick - rewindTo + 1;
        if (amount > maxHistory) { amount = maxHistory; }
        if (amount <= 0) { return; }


        // apply rewound state
        HistoryState state = getState((rewindTo));
        state.bodies.apply();
        state.controllers.apply();

        // set time and onstate backward
        // subtract by one since these will both be incremented immediately
        TimeManager.setTick((short)(rewindTo - 1));
        instance.onState = getState((short)(rewindTo - 1)).index;

        // resimulate physics
        instance.rewriting = true;
        PhysicsManager.getWorld().stepSimulation((float)(amount / (1.0 * PhysicsManager.tickRate)), maxHistory, 1f / (float)PhysicsManager.tickRate);
        instance.rewriting = false;
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
            instance.states[instance.onState].controllers.applyInput();
        }

        instance.states[instance.onState] = new HistoryState(instance.onState);

        if (instance.rewriting || UDPClient.hasClient()) {
            for (Object p : EntityManager.get("player")) {
                Player player = (Player) p;
                if (((Player) p).getId() == 0) { continue; }
                Transform t = new Transform();
                player.getBody().getWorldTransform(t);
                System.out.println("Tick: " + TimeManager.getTick() + ", " + player.getController().getControls() + ", " + t.origin);
            }
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

        private HistoryRigidBodies() {
            bodies = new ArrayList<>();
            for (RigidBody rb : PhysicsManager.getRigidBodies()) {
                HistoryRigidBody hrb = new HistoryRigidBody();
                hrb.rb = rb;
                rb.getWorldTransform(hrb.worldTransform);
                rb.getMotionState().setWorldTransform(hrb.worldTransform);
                rb.getLinearVelocity(hrb.linearVelocity);
                rb.getAngularVelocity(hrb.angularVelocity);
                bodies.add(hrb);
            }
        }

        private void apply() {
            for (HistoryRigidBody hrb : bodies) {
                RigidBody rb = hrb.rb;
                //PhysicsManager.addRigidBody(rb);
                rb.setWorldTransform(hrb.worldTransform);
                rb.setLinearVelocity(hrb.linearVelocity);
                rb.setAngularVelocity(hrb.angularVelocity);
                rb.getMotionState().setWorldTransform(hrb.worldTransform);
            }
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
