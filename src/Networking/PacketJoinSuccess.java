package Networking;

import a2.GameEntities.Player;
import a2.GameState;
import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.NetworkHelpers.NetworkFloat;
import myGameEngine.Singletons.TimeManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;

public class PacketJoinSuccess extends Packet {
    // write variables
    private Player player;

    // read variables
    private short tick;
    private byte id;
    private Player.Team side;
    private Vector3 position;
    private byte orange;
    private byte blue;
    private byte headId;

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
        ByteBuffer buffer = ByteBuffer.allocate(13);
        Vector3 position = player.getNode().getWorldPosition();
        buffer.putShort(TimeManager.getTick());
        buffer.put(player.getId());
        buffer.put((byte)player.getSide().ordinal());
        buffer.putShort(NetworkFloat.encode(position.x()));
        buffer.putShort(NetworkFloat.encode(position.y()));
        buffer.putShort(NetworkFloat.encode(position.z()));
        buffer.put((byte) GameState.getScore(Player.Team.Orange));
        buffer.put((byte) GameState.getScore(Player.Team.Blue));
        buffer.put(player.getHeadId());
        return buffer;
    }

    @Override
    public void readInfo(ByteBuffer buffer) {
        tick = buffer.getShort();
        id = buffer.get();
        if(buffer.get() == Player.Team.Orange.ordinal()){
            side = Player.Team.Orange;
        }else{
            side = Player.Team.Blue;
        }
        position = Vector3f.createFrom(
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort()),
                NetworkFloat.decode(buffer.getShort())
        );
        orange = buffer.get();
        blue = buffer.get();
        headId = buffer.get();
    }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        // should not happen
    }

    @Override
    public void receivedOnClient() {
        if (UDPClient.getPlayer(id) != null) { return; }
        UDPClient.setLastReceivedTick(tick);
        TimeManager.setTick(tick);
        UDPClient.setPlayerId(id);
        Player player = new Player(id, true, side, headId);
        UDPClient.addPlayer(player);
        player.setPosition(position);
        GameState.setScore(Player.Team.Orange, orange);
        GameState.setScore(Player.Team.Blue, blue);
        System.out.println("joined as player " + id);
    }
}
