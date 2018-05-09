package Networking;

import a3.GameEntities.Player;
import myGameEngine.Singletons.EntityManager;
import ray.networking.IGameConnection;
import ray.networking.client.GameConnectionClient;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class UDPClient extends GameConnectionClient {
    private static UDPClient instance;
    public static int updateRate = 30;
    private long nextInputTime;
    private short lastReceivedTick;

    private ConcurrentHashMap<Byte, Player> players = new ConcurrentHashMap<>();
    private byte playerId;

    private UDPClient(InetAddress serverIp, int remotePort) throws IOException {
        super(serverIp, remotePort, IGameConnection.ProtocolType.UDP);
    }

    public static void createClient(InetAddress serverIp, int serverPort) throws IOException {
        instance =  new UDPClient(serverIp, serverPort);
    }


    public static Collection<Player> getPlayers() { return instance.players.values(); }
    public static Player getPlayer() {
        return instance.players.get(instance.playerId);
    }
    public static Player getPlayer(byte playerId) { return instance.players.get(playerId); }

    public static void send(Packet packet) {
        try {
            instance.sendPacket(packet.write(null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setLastReceivedTick(short tick) { instance.lastReceivedTick = tick; }
    public static short getLastReceivedTick() { return instance.lastReceivedTick; }

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
        if (o == null) { return; }
        ByteBuffer buffer = ByteBuffer.wrap((byte[])o);
        Packet packet = Packet.read(null, buffer);
        packet.receivedOnClient();
        if (packet.isReliable()) { packet.sendAck(null); }
    }

    private static void removeInactivePlayers() {
        ArrayList<Player> removePlayers = new ArrayList<>();
        for (Player player : instance.players.values()) {
            if (player == EntityManager.getLocalPlayer()) { continue; }
            if (player.lastMessageReceived == 0) {
                removePlayers.add(player);
            }
        }

        for (Player player : removePlayers) {
            removeInactivePlayer(player);
        }
    }

    private static void removeInactivePlayer(Player player) {
        instance.players.remove(player.getId());
        System.out.println("Removed due to inactivity: " + player.getId());
        player.destroy();
    }

    public static void update() {
        instance.processPackets();
        Packet.resendUnackedPackets();

        removeInactivePlayers();

        long currentTime = java.lang.System.currentTimeMillis();
        if (currentTime >= instance.nextInputTime) {
            instance.nextInputTime = java.lang.System.currentTimeMillis() + 1000 / updateRate;
            if (UDPClient.getPlayer() != null) {
                send(new PacketInput());
            }
        }
    }
}
