package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class PacketAck extends Packet {

    // read/write variables
    private byte number;

    public PacketAck() { }

    public PacketAck(byte number) {
        this.number = number;
    }

    @Override
    public boolean isReliable() { return false; }

    @Override
    public byte getId() { return (byte)'a'; }

    @Override
    public ByteBuffer writeInfo() { return ByteBuffer.allocate(1).put(number); }

    @Override
    public void readInfo(ByteBuffer info) { number = info.get(); }

    @Override
    public void receivedOnServer(ClientInfo cli) {
        System.out.println("rx ack " + cli.info() + ": " + number);
        HashMap<Byte, Packet> map = Packet.unackedPackets.get(cli);
        if (map != null) { map.remove(number); }
    }

    @Override
    public void receivedOnClient() {
        System.out.println("rx ack: " + number);
        Packet.unackedPackets.get(null).remove(number);
    }
}
