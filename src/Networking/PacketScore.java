package Networking;

import a3.GameEntities.Player;
import a3.GameState;
import myGameEngine.NetworkHelpers.ClientInfo;

import java.nio.ByteBuffer;

public class PacketScore extends Packet {
    // read variables
    private byte orange;
    private byte blue;

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'r'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte)GameState.getScore(Player.Team.Orange));
        buffer.put((byte)GameState.getScore(Player.Team.Blue));
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        orange = buffer.get();
        blue = buffer.get();
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void receivedOnClient() {
        GameState.setScore(Player.Team.Orange, orange);
        GameState.setScore(Player.Team.Blue, blue);
    }
}
