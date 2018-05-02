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
            new PacketAck(),
            new PacketJoin(),
            new PacketJoinSuccess(),
            new PacketWorldState(),
            new PacketInput(),
            new PacketAttack(),
            new PacketScore()
    });

    public static HashMap<String, HashMap<Byte, Packet>> unackedPackets = new HashMap<>();
    public static HashMap<String, Byte> nextPacketNumber = new HashMap<>();
    private Byte number = null;
    private ClientInfo clientInfo;
    private long nextSendTime;
    private long sendTimeout = 250;

    public Byte getNumber() { return number; }
    public abstract boolean isReliable();
    public abstract byte getId();
    public abstract ByteBuffer writeInfo();
    public abstract void readInfo(ByteBuffer buffer);
    public abstract void receivedOnServer(ClientInfo cli);
    public abstract void receivedOnClient();

    private static HashMap<Byte, Packet> createPacketIds(Packet[] array) {
        HashMap<Byte, Packet> packets = new HashMap<>();
        for (byte i = 0; i < array.length; i++) {
            if (packets.containsKey(array[i].getId())) {
                System.out.println("Error: Duplicate packet ID!");
                System.exit(1);
            }
            packets.put(array[i].getId(), array[i]);
        }
        return packets;
    }

    public Serializable write(ClientInfo cli) {
        this.clientInfo = cli;
        this.nextSendTime = java.lang.System.currentTimeMillis() + sendTimeout;
        this.sendTimeout += 150;
        byte[] info = writeInfo().array();
        int headerSize = isReliable() ? 4 : 3;
        ByteBuffer buffer = ByteBuffer.allocate(headerSize + info.length);

        // id
        buffer.put(getId());

        // number
        if (isReliable()) {
            if (getNumber() == null) {
                String clientInfo = (cli == null) ? null : cli.info();
                if (!unackedPackets.containsKey(clientInfo)) {
                    // this is our first reliable packet to this destination
                    unackedPackets.put(clientInfo, new HashMap<>());
                    nextPacketNumber.put(clientInfo, (byte)0);
                }
                // store this packet as unacked
                number = nextPacketNumber.get(clientInfo);
                unackedPackets.get(clientInfo).put(number, this);
                // increment packet number
                nextPacketNumber.put(clientInfo, (byte)(number + 1));
            }
            buffer.put(getNumber());
        }

        // length
        buffer.putShort((short)info.length);

        // info
        for (byte b : info) { buffer.put(b); }

        return buffer.array();
    }

    public static Packet read(ClientInfo cli, ByteBuffer buffer) {
        // id
        byte id = buffer.get();

        // number
        Packet packet = Packet.packets.get(id);
        if (packet.isReliable()) {
            packet.number = buffer.get();
        }

        // length
        short length = buffer.getShort();

        // info
        byte[] packetInfo = new byte[length];
        if (length != 0) { buffer.get(packetInfo, 0, length); }
        packet.readInfo(ByteBuffer.wrap(packetInfo));

        return packet;
    }

    public void sendAck(ClientInfo cli) {
        System.out.println("tx ack " + (UDPClient.hasClient() ? "" : cli.info()) + ": " + number);
        PacketAck ack = new PacketAck(number);
        if (UDPClient.hasClient()) {
            UDPClient.send(ack);
        } else {
            UDPServer.sendTo(cli, ack);
        }
    }

    public static void resendUnackedPackets() {
        long currentTime = java.lang.System.currentTimeMillis();
        for (HashMap<Byte, Packet> map : unackedPackets.values()) {
            for (Packet packet : map.values()) {
                if (packet.nextSendTime > currentTime) { continue; }
                System.out.println("resending packet #" + packet.getNumber());
                if (UDPClient.hasClient()) {
                    UDPClient.send(packet);
                } else {
                    UDPServer.sendTo(packet.clientInfo, packet);
                }
            }
        }
    }

    public static String debugString(ByteBuffer buffer) {
        String debug = "";
        for (byte b : buffer.array()) {
            debug += b + ", ";
        }
        return debug;
    }
}
