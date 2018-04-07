package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketJoinSuccess extends Packet {
    // write variables
    private Player player;

    // read variables
    private byte id;
    private byte side;
    private Vector3 position;

    public PacketJoinSuccess() { }

    public PacketJoinSuccess(Player player) {
        this.player = player;
    }

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'s'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        Vector3 position = player.getNode().getWorldPosition();
        buffer.put(player.getId());
        buffer.put(player.getSide());
        buffer.putShort(NetworkFloat.encode(position.x()));
        buffer.putShort(NetworkFloat.encode(position.y()));
        buffer.putShort(NetworkFloat.encode(position.z()));
        return buffer;
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void readInfo(ByteBuffer info) {
        id = info.get();
        side = info.get();
        position = Vector3f.createFrom(
                NetworkFloat.decode(info.getShort()),
                NetworkFloat.decode(info.getShort()),
                NetworkFloat.decode(info.getShort())
        );
    }

    @Override
    public void receivedOnClient() {
        UDPClient.setPlayerId(id);
        if (UDPClient.getPlayer(id) == null) {
            UDPClient.addPlayer(new Player(id, true, side, position));
        }
        System.out.println("joined.");
    }
}
