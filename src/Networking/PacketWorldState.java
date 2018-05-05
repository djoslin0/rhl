package Networking;

import a2.GameEntities.Player;
import a2.GameEntities.Puck;
import com.bulletphysics.linearmath.Transform;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.TimeManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

public class PacketWorldState extends Packet {
    // write variables

    // read variables
    private short tick;
    private boolean puckFrozen;
    private boolean puckDunked;
    private Vector3 puckPosition;
    private javax.vecmath.Quat4f puckOrientation = new javax.vecmath.Quat4f();
    private javax.vecmath.Vector3f puckLinearVelocity = new javax.vecmath.Vector3f();
    private javax.vecmath.Vector3f puckAngularVelocity = new javax.vecmath.Vector3f();
    private ArrayList<PlayerState> playerStates;

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'w'; }

    @Override
    public ByteBuffer writeInfo() {
        Collection<Player> players = UDPServer.getPlayers();
        ByteBuffer buffer = ByteBuffer.allocate(30 + 20 * players.size());
        Puck puck = EntityManager.getPuck();
        puckPosition = puck.getNode().getWorldPosition();
        puck.getBody().getOrientation(puckOrientation);
        puck.getBody().getLinearVelocity(puckLinearVelocity);
        puck.getBody().getAngularVelocity(puckAngularVelocity);

        // tick
        buffer.putShort(TimeManager.getTick());

        // puck frozen
        if (puck.isFrozen()) {
            buffer.put(puck.wasDunked() ? (byte) 2 : (byte) 1);
        } else {
            buffer.put((byte)0);
        }

        // position
        buffer.putShort(NetworkFloat.encode(puckPosition.x()));
        buffer.putShort(NetworkFloat.encode(puckPosition.y()));
        buffer.putShort(NetworkFloat.encode(puckPosition.z()));

        // orientation
        buffer.putShort(NetworkFloat.encode(puckOrientation.w));
        buffer.putShort(NetworkFloat.encode(puckOrientation.x));
        buffer.putShort(NetworkFloat.encode(puckOrientation.y));
        buffer.putShort(NetworkFloat.encode(puckOrientation.z));

        // linear velocity
        buffer.putShort(NetworkFloat.encode(puckLinearVelocity.x));
        buffer.putShort(NetworkFloat.encode(puckLinearVelocity.y));
        buffer.putShort(NetworkFloat.encode(puckLinearVelocity.z));

        // angular velocity
        buffer.putShort(NetworkFloat.encode(puckAngularVelocity.x));
        buffer.putShort(NetworkFloat.encode(puckAngularVelocity.y));
        buffer.putShort(NetworkFloat.encode(puckAngularVelocity.z));

        // players
        buffer.put((byte)players.size());
        for (Player player : players) {
            buffer.put(player.getId());
            buffer.put((byte)player.getSide().ordinal());
            buffer.put(player.getHealth());
            buffer.put(player.getController().getControls());
            buffer.putShort(NetworkFloat.encode(player.getPitch() * 100f));
            buffer.putShort(NetworkFloat.encode(player.getYaw() * 100f));

            Vector3 position = player.getPosition();
            buffer.putShort(NetworkFloat.encode(position.x()));
            buffer.putShort(NetworkFloat.encode(position.y()));
            buffer.putShort(NetworkFloat.encode(position.z()));

            Vector3 velocity = player.getVelocity();
            buffer.putShort(NetworkFloat.encode(velocity.x()));
            buffer.putShort(NetworkFloat.encode(velocity.y()));
            buffer.putShort(NetworkFloat.encode(velocity.z()));
        }
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        tick = buffer.getShort();

        byte puckState = buffer.get();
        puckFrozen = (puckState > 0);
        puckDunked = (puckState == 2);

        puckPosition = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort())
        );

        puckOrientation.w = NetworkFloat.decode(buffer.getShort());
        puckOrientation.x = NetworkFloat.decode(buffer.getShort());
        puckOrientation.y = NetworkFloat.decode(buffer.getShort());
        puckOrientation.z = NetworkFloat.decode(buffer.getShort());

        puckLinearVelocity.x = NetworkFloat.decode(buffer.getShort());
        puckLinearVelocity.y = NetworkFloat.decode(buffer.getShort());
        puckLinearVelocity.z = NetworkFloat.decode(buffer.getShort());

        puckAngularVelocity.x = NetworkFloat.decode(buffer.getShort());
        puckAngularVelocity.y = NetworkFloat.decode(buffer.getShort());
        puckAngularVelocity.z = NetworkFloat.decode(buffer.getShort());

        playerStates = new ArrayList<>();
        byte playerCount = buffer.get();
        while(playerCount-- > 0) {
            byte id = buffer.get();
            byte side = buffer.get();
            byte health = buffer.get();
            byte controls = buffer.get();
            float pitch = NetworkFloat.decode(buffer.getShort()) / 100f;
            float yaw = NetworkFloat.decode(buffer.getShort()) / 100f;

            Vector3 position = Vector3f.createFrom(
                    NetworkFloat.decode(buffer.getShort()),
                    NetworkFloat.decode(buffer.getShort()),
                    NetworkFloat.decode(buffer.getShort()));

            Vector3 velocity = Vector3f.createFrom(
                    NetworkFloat.decode(buffer.getShort()),
                    NetworkFloat.decode(buffer.getShort()),
                    NetworkFloat.decode(buffer.getShort()));

            if (id != UDPClient.getPlayerId()) {
                playerStates.add(new PlayerState(id, side, health, controls, pitch, yaw, position, velocity));
            }
        }
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void receivedOnClient() {
        if (UDPClient.getPlayer() == null) { return; }

        // ignore past packets
        if (TimeManager.difference(tick, UDPClient.getLastReceivedTick()) <= 0) { return; }
        UDPClient.setLastReceivedTick(tick);

        TimeManager.setTick(tick);

        Puck puck = EntityManager.getPuck();

        // set frozen
        if (!puck.isFrozen() && puckFrozen) {
            puck.reset(puckDunked);
        } else if (puck.isFrozen() && !puckFrozen) {
            puck.unfreeze();
        }

        // set position
        Transform puckTransform = new Transform();
        puck.getBody().getWorldTransform(puckTransform);
        puckTransform.origin.set(puckPosition.x(), puckPosition.y(), puckPosition.z());

        // orientation
        puckTransform.setRotation(puckOrientation);
        puck.getBody().setWorldTransform(puckTransform);

        // velocities
        puck.getBody().setLinearVelocity(puckLinearVelocity);
        puck.getBody().setAngularVelocity(puckAngularVelocity);

        // players
        for (PlayerState playerState : playerStates) {
            playerState.apply();
        }
    }

    private class PlayerState {
        private byte id;
        private byte side;
        private byte health;
        private byte controls;
        private float pitch;
        private float yaw;
        private Vector3 position;
        private Vector3 velocity;

        public PlayerState(byte id, byte side, byte health, byte controls, float pitch, float yaw, Vector3 position, Vector3 velocity) {
            this.id = id;
            this.side = side;
            this.health = health;
            this.controls = controls;
            this.pitch = pitch;
            this.yaw = yaw;
            this.position = position;
            this.velocity = velocity;
        }

        public void apply() {
            Player player = UDPClient.getPlayer(id);

            if (player == null) {
                player = new Player(id, false, Player.Team.values()[side]);
                player.setPosition(position);
                UDPClient.addPlayer(player);
                System.out.println("NEW PLAYER: " + id);
            }

            player.getController().setControls(controls);
            player.getController().setControls(controls);
            player.setPitch(pitch);
            player.setYaw(yaw);
            player.setPosition(position);
            player.setVelocity(velocity);
            player.setHealth(health);
        }
    }
}
