package Networking;

import a2.GameEntities.Player;
import ray.networking.IGameConnection;
import ray.networking.client.GameConnectionClient;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class UDPClient extends GameConnectionClient {
    private static UDPClient instance;
    public static int updateRate = 20;
    private long nextInputTime;

    private ConcurrentHashMap<Byte, Player> players = new ConcurrentHashMap<>();
    private byte playerId;

    private UDPClient(InetAddress serverIp, int remotePort) throws IOException {
        super(serverIp, remotePort, IGameConnection.ProtocolType.UDP);
    }

    public static void createClient(InetAddress serverIp, int serverPort) throws IOException {
        instance =  new UDPClient(serverIp, serverPort);
    }

    public static Player getPlayer(byte playerId) {
        return instance.players.get(playerId);
    }

    public static void send(Packet packet) {
        try {
            instance.sendPacket(packet.write(null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPlayerId(byte playerId) { instance.playerId = playerId; }
    public static byte getPlayerId() { return instance.playerId; }
    public static boolean hasClient() { return instance != null; }

    public static void addPlayer(Player player) {
        if (!instance.players.contains(player)) {
            instance.players.put(player.getId(), player);
        }
    }

    public void processPacket(Object o)
    {
        ByteBuffer buffer = ByteBuffer.wrap((byte[])o);
        Packet packet = Packet.read(null, buffer);
        packet.receivedOnClient();
        if (packet.isReliable()) { packet.sendAck(null); }
    }

    public static void update() {
        instance.processPackets();
        Packet.resendUnackedPackets();

        long currentTime = java.lang.System.currentTimeMillis();
        if (currentTime >= instance.nextInputTime) {
            instance.nextInputTime = java.lang.System.currentTimeMillis() + 1000 / updateRate;
            send(new PacketInput(getPlayer(instance.playerId)));
        }
    }
}
