package Networking;

import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.Singletons.Settings;
import ray.networking.IGameConnection;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import a2.GameEntities.Player;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.InetAddress;

public class UDPServer extends GameConnectionServer<Byte> {
    private static UDPServer instance;
    public static int updateRate = 20;

    private long nextWorldState;
    private byte nextId = 1;
    private byte nextSide = 0;
    private static ConcurrentHashMap<String, Player> clientPlayers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ClientInfo> clientInfos = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ArrayList<Packet>> unreadPackets = new ConcurrentHashMap<>();

    private UDPServer(int localPort) throws IOException {
        super(localPort, IGameConnection.ProtocolType.UDP);
        System.out.println("server running on port: " + localPort);
    }

    public static void createServer(int localPort) throws IOException {
        instance = new UDPServer(localPort);
    }

    public static boolean hasServer() { return instance != null; }

    public static Player createPlayer(ClientInfo cli) {
        if (instance.clientPlayers.contains(cli.info())) {
            return instance.clientPlayers.get(cli.info());
        } else {
            Player player = new Player(instance.nextId, false, instance.nextSide, Settings.get().spawnPoint);
            instance.clientPlayers.put(cli.info(), player);
            try {
                IClientInfo ci = instance.getServerSocket().createClientInfo(cli.getIp(), cli.getPort());
                instance.addClient(ci, instance.nextId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            instance.nextId++;
            instance.nextSide = (byte) ((instance.nextSide + 1) % 2);
            return player;
        }
    }

    public static Player getPlayer(ClientInfo cli) {
        return instance.clientPlayers.get(cli.info());
    }

    public static void sendTo(ClientInfo cli, Packet packet) {
        try {
            Player player = clientPlayers.get(cli.info());
            if (player == null) {
                System.out.println("ERROR: could not identify player");
                return;
            }
            instance.sendPacket(packet.write(cli), player.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToAll(Packet packet) {
        try {
            for (ClientInfo cli : clientInfos.values()) {
                Player player = clientPlayers.get(cli.info());
                if (player == null) { continue; }
                instance.sendPacket(packet.write(cli), player.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort) {
        ClientInfo cli = new ClientInfo(senderIP, sndPort);
        clientInfos.put(cli.info(), cli);
        ByteBuffer buffer = ByteBuffer.wrap((byte[]) o);
        Packet packet = Packet.read(cli, buffer);
        if (packet.isReliable()) { packet.sendAck(cli); }

        ArrayList<Packet> packets = unreadPackets.get(cli.info());
        if (packets == null) {
            packets = new ArrayList<>();
            unreadPackets.put(cli.info(), packets);
        }
        synchronized (packets) {
            packets.add(packet);
        }
    }

    public static void update() {
        for (ClientInfo cli : clientInfos.values()) {
            ArrayList<Packet> packets = unreadPackets.get(cli.info());
            synchronized (packets) {
                for (Packet packet : packets) {
                    packet.receivedOnServer(cli);
                }
                packets.clear();
            }
        }
        long currentTime = java.lang.System.currentTimeMillis();
        if (currentTime >= instance.nextWorldState) {
            instance.nextWorldState = java.lang.System.currentTimeMillis() + 1000 / updateRate;
            sendToAll(new PacketWorldState());
        }
        Packet.resendUnackedPackets();
    }
}
