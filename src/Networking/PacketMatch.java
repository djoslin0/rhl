package Networking;

import a2.GameEntities.Player;
import a2.GameState;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.Singletons.TimeManager;

import java.nio.ByteBuffer;

public class PacketMatch extends Packet {
    // read variables
    private byte state;
    private byte team;

    @Override
    public boolean isReliable() { return true; }

    @Override
    public byte getId() { return (byte)'m'; }

    @Override
    public ByteBuffer writeInfo() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(GameState.isMatchOver() ? (byte) 1 : (byte)0);
        buffer.put(GameState.getScore(Player.Team.Orange) > GameState.getScore(Player.Team.Blue) ? (byte) 0 : (byte)1);
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        state = buffer.get();
        team = buffer.get();
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void receivedOnClient() {
        System.out.println("MATCH: " + state + ", " + team);
        if (!GameState.isMatchOver() && state == 1) {
            GameState.endMatch(team == (byte)0 ? Player.Team.Orange : Player.Team.Blue);
            System.out.println("MATCH: OVER");
        } else if (GameState.isMatchOver() && state == 0) {
            GameState.resetMatch();
            System.out.println("MATCH: RESET");
        }
    }
}
