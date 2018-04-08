package Networking;

import myGameEngine.NetworkHelpers.ClientInfo;
import myGameEngine.Singletons.Settings;
import ray.networking.IGameConnection;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import a2.GameEntities.Player;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.InetAddress;

public class UDPServer extends GameConnectionServer<Byte> {
    private static UDPServer instance;

    private byte nextId = 1;
    private byte nextSide = 0;
    private static ConcurrentHashMap<ClientInfo, Player> clientPlayers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ClientInfo> clientInfos = new ConcurrentHashMap<>();

    private UDPServer(int localPort) throws IOException {
        super(localPort, IGameConnection.ProtocolType.UDP);
        System.out.println("server running on port: " + localPort);
    }

    public static void createServer(int localPort) throws IOException {
        instance = new UDPServer(localPort);
    }

    public static Player createPlayer(ClientInfo cli) {
        if (instance.clientPlayers.contains(cli.toString())) {
            return instance.clientPlayers.get(cli.toString());
        } else {
            Player player = new Player(instance.nextId, false, instance.nextSide, Settings.get().spawnPoint);
            instance.clientPlayers.put(cli, player);
            try {
                IClientInfo ci = instance.getServerSocket().createClientInfo(cli.getIp(), cli.getPort());
                instance.addClient(ci, instance.nextId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            instance.nextId++;
            instance.nextSide = (byte)((instance.nextSide + 1) % 2);
            return player;
        }
    }

    public static Player getPlayer(ClientInfo cli) {
        return instance.clientPlayers.get(cli.info());
    }

    public static void sendTo(ClientInfo cli, Packet packet) {
        System.out.println("sending to " + cli.info() + "...");
        try {
            Player player = clientPlayers.get(cli);
            System.out.println(">> " + cli.info() + ", " + (player != null) + ", " + (packet != null));
            instance.sendPacket(packet.write(cli), player.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToAll(Packet packet) {
        try {
            for (ClientInfo cli : clientInfos.values()) {
                Player player = clientPlayers.get(cli);
                instance.sendPacket(packet.write(cli), player.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort) {
        ClientInfo cli = new ClientInfo(senderIP, sndPort);
        if (clientInfos.contains(cli.info())) {
            cli = clientInfos.get(cli.info());
        } else {
            clientInfos.put(cli.info(), cli);
        }
        ByteBuffer buffer = ByteBuffer.wrap((byte[])o);
        Packet packet = Packet.read(cli, buffer);
        packet.receivedOnServer(cli);
        if (packet.isReliable()) { packet.sendAck(cli); }
    }
}
