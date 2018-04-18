package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketAttack extends Packet {
    // read/write variables
    private byte id;
    private Vector3 aim;
    private Vector3 relative;

    public PacketAttack() {}

    public PacketAttack(byte id, Vector3 aim, Vector3 relative) {
        this.id = id;
        this.aim = aim;
        this.relative = relative;
    }

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'t'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.put(id);
        buffer.putShort(NetworkFloat.encode(aim.x()));
        buffer.putShort(NetworkFloat.encode(aim.y()));
        buffer.putShort(NetworkFloat.encode(aim.z()));
        buffer.putShort(NetworkFloat.encode(relative.x()));
        buffer.putShort(NetworkFloat.encode(relative.y()));
        buffer.putShort(NetworkFloat.encode(relative.z()));
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        id = buffer.get();

        aim = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));

        relative = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()));
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        if (id == -1) {
            EntityManager.getPuck().attacked(aim, relative);
        } else {
            Player player = UDPServer.getPlayer(id);
            player.attacked(aim, relative);
        }

        // broadcast
        PacketAttack attack = new PacketAttack(id, aim, relative);
        UDPServer.sendToAll(attack);
    }

    @Override
    public void receivedOnClient() {
        Player player = UDPClient.getPlayer(id);
        if (player == null) { return; }

        player.attacked(aim, relative);
    }
}
