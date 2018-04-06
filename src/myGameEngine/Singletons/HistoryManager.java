package myGameEngine.Singletons;

import a2.GameEntities.Player;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.Controllers.CharacterController;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.*;

public class HistoryManager {
    private static final HistoryManager instance = new HistoryManager();
    private static final int maxHistory = 144;
    private int[] historyTick = new int[maxHistory];
    private HistoryRigidBodies[] historyRigidBodies = new HistoryRigidBodies[maxHistory];
    private HistoryCharacterControllers[] historyCharacterControllers = new HistoryCharacterControllers[maxHistory];
    private int onState = 0;
    private int rewriteCount = 0;
    private Player rewritingPlayerInput = null;

    public static void rewind(Player player, int amount) {
        if (amount > maxHistory) { amount = maxHistory; }

        // find rewound state index
        int wasOnState = instance.onState;
        int rewoundState = (maxHistory + wasOnState - amount) % maxHistory;

        // forget history of rigid bodies
        for (int i = instance.onState; i != rewoundState; i = (maxHistory + i - 1) % maxHistory) {
            instance.historyRigidBodies[i] = null;
        }

        // set to rewound state index
        instance.onState = rewoundState;
        HistoryRigidBodies hp = instance.historyRigidBodies[instance.onState];
        if (hp == null) { return; }

        // remove all current rigid bodies from world
        ArrayList<RigidBody> rigidBodies = (ArrayList<RigidBody>)PhysicsManager.getRigidBodies().clone();
        for (RigidBody rb : rigidBodies) {
            PhysicsManager.removeRigidBody(rb);
        }

        // apply rewound rigid bodies
        hp.apply();
n
        // apply rewound character controllers
        HistoryCharacterControllers hcc = instance.historyCharacterControllers[instance.onState];
        hcc.fullApply();

        // resimulate physics
        instance.rewritingPlayerInput = player;
        instance.rewriteCount = 0;
        PhysicsManager.getWorld().stepSimulation(amount / (1f * PhysicsManager.tickRate), maxHistory, 1f / (float)PhysicsManager.tickRate);
        System.out.println(amount + " : " + instance.rewriteCount + ", " + instance.onState + " : " + wasOnState);
        instance.rewritingPlayerInput = null;
    }

    public static void internalTick(float timeStep) {
        // increment on state index
        instance.onState = (instance.onState + 1) % maxHistory;

        // remember tick
        instance.historyTick[instance.onState] = 0;  // TODO: track time properly

        // remember rigid bodies
        instance.historyRigidBodies[instance.onState] = new HistoryRigidBodies();

        // apply history of player inputs other than the player being rewrote
        if (instance.rewritingPlayerInput != null) {
            instance.rewriteCount++;
            instance.historyCharacterControllers[instance.onState].inputApply(instance.rewritingPlayerInput);
        }

        // remember character controllers
        instance.historyCharacterControllers[instance.onState] = new HistoryCharacterControllers();
    }

    private static class HistoryRigidBodies {
        private ArrayList<HistoryRigidBody> historyRigidBodies;

        private HistoryRigidBodies() {
            historyRigidBodies = new ArrayList<>();
            for (RigidBody rb : PhysicsManager.getRigidBodies()) {
                HistoryRigidBody hrb = new HistoryRigidBody();
                hrb.rb = rb;
                rb.getWorldTransform(hrb.worldTransform);
                rb.getLinearVelocity(hrb.linearVelocity);
                rb.getAngularVelocity(hrb.angularVelocity);
                historyRigidBodies.add(hrb);
            }
        }

        private void apply() {
            for (HistoryRigidBody hrb: historyRigidBodies) {
                RigidBody rb = hrb.rb;
                PhysicsManager.addRigidBody(rb);
                rb.setWorldTransform(hrb.worldTransform);
                rb.setLinearVelocity(hrb.linearVelocity);
                rb.setAngularVelocity(hrb.angularVelocity);
            }
        }

        private class HistoryRigidBody {
            public RigidBody rb = null;
            public Transform worldTransform = new Transform();
            public Vector3f linearVelocity = new Vector3f();
            public Vector3f angularVelocity = new Vector3f();
        }
    }

    private static class HistoryCharacterControllers {
        private HashMap<Player, CharacterController.HistoryCharacterController> historyCharacterControllers = new HashMap<>();

        private HistoryCharacterControllers() {
            for (Object p : EntityManager.get("player")) {
                Player player = (Player) p;
                historyCharacterControllers.put(player, player.getController().remember());
            }
        }

        private void fullApply() {
            for (CharacterController.HistoryCharacterController hcc : historyCharacterControllers.values()) {
                hcc.fullApply();
            }
        }

        private void inputApply(Player ignorePlayer) {
            for (CharacterController.HistoryCharacterController hcc : historyCharacterControllers.values()) {
                if (ignorePlayer == hcc.getPlayer()) { continue; }
                hcc.inputApply();
            }
        }
    }
}
