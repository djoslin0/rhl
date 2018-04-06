package Networking;

import myGameEngine.NetworkHelpers.AlphabetHashTable;
import myGameEngine.NetworkHelpers.ClientInfo;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import a2.GameEntities.Player;
import ray.rml.Vector3;
import ray.rml.Vector3f;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.InetAddress;

public class UDPServer extends GameConnectionServer<Integer>{
    private Integer id = 1;
    private int redSide = 0;
    private int blueSide = 0;
    private ConcurrentHashMap<Integer,Player> clientEntities;
    private ConcurrentHashMap<String,Integer> clientMap;
    private static UDPServer server = null;
    private AlphabetHashTable switchVals;

    private UDPServer(int localPort, ProtocolType protocolType) throws IOException {
        super(localPort, protocolType);
        clientEntities = new ConcurrentHashMap<>();
        clientMap = new ConcurrentHashMap<>();
        switchVals = new AlphabetHashTable();

        System.out.println("server running on port:" + localPort);
    }

    public static UDPServer createServer(int localPort,ProtocolType protocolType) throws IOException {
        if(server == null) {
            return new UDPServer(localPort,protocolType);
        } else {
            return server;
        }
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort) {
        String message = (String) o;
        String[] msgTokens = message.split(",");
        IClientInfo ci = null;
        ClientInfo cli = new ClientInfo(senderIP.toString(), String.valueOf(sndPort));
        try {
            ci = getServerSocket().createClientInfo(senderIP,sndPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(msgTokens.length > 0) {
            switch (switchVals.get(msgTokens[0])) {
                //join
                case 26:
                    System.out.println(sndPort);
                    System.out.println("client join with credentials: " + senderIP + " " + sndPort);
                    try {
                        int side = ((blueSide > redSide) ? 0 : 1);
                        int cId;
                        if (clientMap.contains(ci)) {
                            cId = clientMap.get(id);
                        } else {
                            cId = id;
                            addClient(ci, cId);
                            clientMap.put(cli.info(), cId);
                            createClient(side, ci);
                            id++;
                            if(blueSide > redSide) {
                                redSide++;
                            } else {
                                blueSide++;
                            }
                        }
                        String packetInfo;
                        Player player = clientEntities.get(cId);
                        packetInfo = "Success";
                        packetInfo += "," + cId;
                        packetInfo += "," + String.valueOf(player.getNode().getLocalPosition().x());
                        packetInfo += "," + String.valueOf(player.getNode().getLocalPosition().y());
                        packetInfo += "," + String.valueOf(player.getNode().getLocalPosition().z());
                        packetInfo += "," + side;
                        sendPacket(packetInfo, cId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                    //sendclientgamestate
                case 27:
                    String packetInfo;
                    packetInfo = "Players";
                    packetInfo += "," + String.valueOf(clientEntities.values().size());
                    for(Object c : clientEntities.values()) {
                        packetInfo += "," + ((Player) c).getId();
                        packetInfo += "," + ((Player) c).getNode().getLocalPosition().x();
                        packetInfo += "," + ((Player) c).getNode().getLocalPosition().y();
                        packetInfo += "," + ((Player) c).getNode().getLocalPosition().z();
                        packetInfo += "," + ((Player) c).getSide();
                    }
                    System.out.println(packetInfo);
                    try {
                        cli = new ClientInfo(senderIP.toString(),String.valueOf(sndPort));
                        sendPacket(packetInfo, clientMap.get(cli.info()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                   //player has moved
                case 12:
                    Vector3 oldlocation = clientEntities.get(clientMap.get(cli.info())).getNode().getLocalPosition();
                    clientEntities.get(clientMap.get(cli.info())).getBody().translate( new javax.vecmath.Vector3f
                            (Float.valueOf(msgTokens[1])-oldlocation.x(),Float.valueOf(msgTokens[2])- oldlocation.y(),Float.valueOf(msgTokens[3])-oldlocation.z()));
                    break;
                    //backward
            }

        }
    }

    private void createClient(int side, IClientInfo ci) throws IOException {
        Player newPlayer =  new Player(id, null, spawn(side), side);
        newPlayer.getNode().setLocalPosition(0f, 1.5f,0f);
        clientEntities.put(id, newPlayer);
    }

    private Vector3 spawn(int side){
        if(side == 0){
            return Vector3f.createFrom(0f,0f,-50f);
        } else {
            return Vector3f.createFrom(0f, 0f, 50f);
        }
    }

    public void updateClients() throws IOException {
        String updatePacket;
        updatePacket = String.valueOf(id);
        for(Object o : clientEntities.entrySet()) {
            updatePacket += "," + String.valueOf(((Player)o).getNode().getLocalPosition().x());
            updatePacket += "," + String.valueOf(((Player)o).getNode().getLocalPosition().y());
            updatePacket += "," + String.valueOf(((Player)o).getNode().getLocalPosition().z());
        }
        sendPacketToAll(updatePacket);
    }
}
