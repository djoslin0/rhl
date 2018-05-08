package Networking;

import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.Settings;
import ray.networking.IGameConnection;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import a2.GameEntities.Player;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.InetAddress;

public class UDPServer extends GameConnectionServer<Byte> {
    private static UDPServer instance;
    public static int updateRate = 30;

    private long nextWorldState;
    private byte nextId = 1;
    private static ConcurrentHashMap<Byte, Player> players = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Player> clientPlayers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ClientInfo> clientInfos = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ArrayList<Object>> unreadPackets = new ConcurrentHashMap<>();

    private UDPServer(int localPort) throws IOException {
        super(localPort, IGameConnection.ProtocolType.UDP);
        System.out.println("server running on port: " + localPort);
    }

    public static void createServer(int localPort) throws IOException {
        instance = new UDPServer(localPort);
    }

    public static boolean hasServer() { return instance != null; }

    private static Player.Team getTeamWithFewer() {
        // check how many players on each side
        int blueSide = 0;
        int orangeSide = 0;

        for (Player playerItr: players.values()) {
            if (playerItr.getSide() == Player.Team.Orange) {
                orangeSide++;
            } else {
                blueSide++;
            }
        }
        if (orangeSide > blueSide) {
            return Player.Team.Blue;
        } else {
            return Player.Team.Orange;
        }
    }

    public static Player createPlayer(ClientInfo cli, byte headId) {
        if (instance.clientPlayers.contains(cli.info())) {
            return instance.clientPlayers.get(cli.info());
        } else {
            Player player = new Player(instance.nextId, false, getTeamWithFewer(), headId);
            instance.clientPlayers.put(cli.info(), player);
            instance.players.put(player.getId(), player);
            try {
                IClientInfo ci = instance.getServerSocket().createClientInfo(cli.getIp(), cli.getPort());
                instance.addClient(ci, instance.nextId);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (instance.players.containsKey(instance.nextId)) {
                instance.nextId++;
                if (instance.nextId == 0 || instance.nextId == -1) {
                    instance.nextId = 2;
                }
            }

            return player;
        }
    }

    public static Player getPlayer(ClientInfo cli) { return instance.clientPlayers.get(cli.info()); }
    public static Player getPlayer(byte id) { return instance.players.get(id); }
    public static Collection<Player> getPlayers() { return instance.players.values(); }

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
        addUnreadPacket(cli, o);
    }

    private synchronized void addUnreadPacket(ClientInfo cli, Object o) {
        ArrayList<Object> packets = unreadPackets.get(cli.info());
        if (packets == null) {
            packets = new ArrayList<>();
            unreadPackets.put(cli.info(), packets);
        }
        packets.add(o);

        Player player = clientPlayers.get(cli.info());
        if (player != null) { player.lastMessageReceived = java.lang.System.currentTimeMillis(); }
    }

    private synchronized void processUnreadPackets() {
        for (ClientInfo cli : clientInfos.values()) {
            ArrayList<Object> packets = unreadPackets.get(cli.info());
            if (packets == null) { continue; }
            for (Object o : packets) {
                ByteBuffer buffer = ByteBuffer.wrap((byte[]) o);
                Packet packet = Packet.read(cli, buffer);
                if (packet.isReliable()) { packet.sendAck(cli); }
                packet.receivedOnServer(cli);
            }
            packets.clear();
        }
    }

    private static void removeInactivePlayers() {
        long now = java.lang.System.currentTimeMillis();
        ArrayList<Player> removePlayers = new ArrayList<>();
        for (Player player : players.values()) {
            if (player.isAi()) { continue; }
            if (now - player.lastMessageReceived > 5000) {
                removePlayers.add(player);
            }
        }

        for (Player player : removePlayers) {
            removeInactivePlayer(player);
        }
    }

    private static void removeInactivePlayer(Player player) {
        players.remove(player.getId());
        String strInfo = null;
        for (String clientInfo : Collections.list(clientPlayers.keys())) {
            if (clientPlayers.get(clientInfo) == player) {
                strInfo = clientInfo;
                break;
            }
        }
        if (strInfo != null) {
            clientPlayers.remove(strInfo);
            clientInfos.remove(strInfo);
            unreadPackets.remove(strInfo);
            Packet.unackedPackets.remove(strInfo);
        }
        System.out.println("Removed due to inactivity: " + player.getId());
        player.destroy();
    }

    public static void update() {
        instance.processUnreadPackets();
        removeInactivePlayers();
        long currentTime = java.lang.System.currentTimeMillis();
        if (currentTime >= instance.nextWorldState) {
            instance.nextWorldState = java.lang.System.currentTimeMillis() + 1000 / updateRate;
            sendToAll(new PacketWorldState());
        }
        Packet.resendUnackedPackets();
    }

    public static void addPlayer(Player player) {
        players.put(player.getId(), player);
    }
}
