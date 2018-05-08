package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketJoin extends Packet {

    // read/write variables
    private byte headId;

    public PacketJoin() {}

    public PacketJoin(byte headId) {
        this.headId = headId;
    }

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'j'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(headId);
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        this.headId = buffer.get();
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        System.out.println("client join with credentials: " + cli.getIp() + ":" + cli.getPort());
        Player player = UDPServer.getPlayer(cli);
        if (player == null) { player = UDPServer.createPlayer(cli, headId); }
        UDPServer.sendTo(cli, new PacketJoinSuccess(player));
    }

    @Override
    public void receivedOnClient() {
        // should not happen
    }
}
