package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketJoin extends Packet {

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'j'; }

    @Override
    public ByteBuffer writeInfo() { return ByteBuffer.allocate(0); }

    @Override
    public void readInfo(ByteBuffer buffer) { }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        System.out.println("client join with credentials: " + cli.getIp() + ":" + cli.getPort());
        Player player = UDPServer.getPlayer(cli);
        if (player == null) { player = UDPServer.createPlayer(cli); }
        UDPServer.sendTo(cli, new PacketJoinSuccess(player));
    }

    @Override
    public void receivedOnClient() {
        // should not happen
    }
}
