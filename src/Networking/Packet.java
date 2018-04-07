package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.ClientInfo;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public abstract class Packet {
    public static HashMap<Byte, Packet> packets = createPacketIds(new Packet[] {
            // list all packets here
            new PacketJoin(),
            new PacketJoinSuccess()
    });

    public abstract boolean isReliable();
    public abstract byte getId();
    public abstract ByteBuffer writeInfo();
    public abstract void readInfo(ByteBuffer info);
    public abstract void receivedOnServer(ClientInfo cli);
    public abstract void receivedOnClient();

    private static HashMap<Byte, Packet> createPacketIds(Packet[] array) {
        HashMap<Byte, Packet> packets = new HashMap<>();
        for (byte i = 0; i < array.length; i++) {
            packets.put(array[i].getId(), array[i]);
        }
        return packets;
    }

    public Serializable write() {
        ByteBuffer info = writeInfo();
        ByteBuffer packet = ByteBuffer.allocate(3 + info.position());
        packet.put(getId());
        packet.putShort((short)info.position());
        for (byte b : info.array()) { packet.put(b); }
        System.out.println("writing packet: " + debugString(packet));
        return packet.array();
    }

    public static Packet read(ClientInfo cli, ByteBuffer buffer) {
        System.out.println("reading packet: " + debugString(buffer));
        byte id = buffer.get();
        short length = buffer.getShort();
        byte[] packetInfo = new byte[length];
        System.out.println("  ID: " + (char)id + " LENGTH: " + length + " POS: " + buffer.position());
        if (length != 0) {
            buffer.get(packetInfo, 0, length);
        }
        Packet.packets.get(id).readInfo(ByteBuffer.wrap(packetInfo));
        return Packet.packets.get(id);
    }

    public static String debugString(ByteBuffer buffer) {
        String debug = "";
        for (byte b : buffer.array()) {
            debug += b + ", ";
        }
        return debug;
    }
}
