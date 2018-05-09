package Networking;

import a3.Contollers.CharacterController;
import a3.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.TimeManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketInput extends Packet {
    // read variables
    private short tick;
    private byte controls;
    private byte health;
    private float pitch;
    private float yaw;
    public Vector3 position;
    private Vector3 velocity;

    public PacketInput() { }

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'i'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(20);

        short onTick = TimeManager.getTick();
        buffer.putShort(onTick);

        Player player = Networking.UDPClient.getPlayer();
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

        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        tick = buffer.getShort();
        health = buffer.get();
        controls = buffer.get();
        pitch = NetworkFloat.decode(buffer.getShort()) / 100f;
        yaw = NetworkFloat.decode(buffer.getShort()) / 100f;

        position = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));

        velocity = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));

    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        Player player = UDPServer.getPlayer(cli);
        if (player == null) { return; }

        // ignore past packets
        if (TimeManager.difference(tick, player.lastReceivedTick) <= 0) { return; }
        player.lastReceivedTick = tick;

        CharacterController controller = player.getController();
        controller.setControls(controls);

        player.setPitch(pitch);
        player.setYaw(yaw);
        player.setPosition(position);
        player.setVelocity(velocity);

        player.setHealth(health);
    }

    @Override
    public void receivedOnClient() {
        // should not happen
    }
}
