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
    private float joe;
    private Integer id = 0;
    private int redSide = 0;
    private int blueSide = 0;
    private ConcurrentHashMap<Integer,Player> cliententities;
    private ConcurrentHashMap<String,Integer> clientmap;
    private static UDPServer server = null;
    private AlphabetHashTable switchVals;

    private UDPServer(int localPort, ProtocolType protocolType) throws IOException {
        super(localPort, protocolType);
        cliententities = new ConcurrentHashMap<>();
        clientmap = new ConcurrentHashMap<String, Integer>();
        switchVals = new AlphabetHashTable();

        System.out.println("server running on port:" + localPort);
    }
    public static UDPServer createServer(int localPort,ProtocolType protocolType) throws IOException {
        if(server ==null){
            return new UDPServer(localPort,protocolType);
        }else
            return server;
    }
    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort) {
        String message = (String) o;
        String[] msgTokens = message.split(",");
        IClientInfo ci = null;
        ClientInfo cli = new ClientInfo(senderIP.toString(),String.valueOf(sndPort));
        try {
            ci = getServerSocket().createClientInfo(senderIP,sndPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
        if(msgTokens.length>0){
            switch (switchVals.get(msgTokens[0])){
                //join
                case 26:
                        System.out.println(sndPort);
                        System.out.println("client join with credentials: " + senderIP + " " + sndPort);
                    if(blueSide>redSide){
                        try {
                            addClient(ci,id);
                            clientmap.put(cli.info(),id);
                            createClient(0,ci);
                            String packetInfo;
                            packetInfo = "Success";
                            packetInfo += "," + String.valueOf(cliententities.get(id).getNode().getLocalPosition().x());
                            packetInfo += "," + String.valueOf(cliententities.get(id).getNode().getLocalPosition().y());
                            packetInfo += "," + String.valueOf(cliententities.get(id).getNode().getLocalPosition().z());
                            packetInfo += "," + 0;
                            sendPacket(packetInfo,id);
                            id++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        try {
                            System.out.println(ci.toString());
                            addClient(ci,id);
                            clientmap.put(cli.info(),id);
                            createClient(1,ci);
                            String packetInfo;
                            packetInfo = "Success";
                            packetInfo += "," + String.valueOf(cliententities.get(id).getNode().getLocalPosition().x());
                            packetInfo += "," + String.valueOf(cliententities.get(id).getNode().getLocalPosition().y());
                            packetInfo += "," + String.valueOf(cliententities.get(id).getNode().getLocalPosition().z());
                            packetInfo += "," + 1;
                            sendPacket(packetInfo,id);
                            id++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    break;
                    //sendclientgamestate
                case 27:
                    String packetInfo;
                    packetInfo = "Players";
                    packetInfo += "," + String.valueOf(cliententities.values().size());
                    for(Object c : cliententities.values()){
                        packetInfo += "," + String.valueOf(((Player)c).getNode().getLocalPosition().x());
                        packetInfo += "," + String.valueOf(((Player)c).getNode().getLocalPosition().y());
                        packetInfo += "," + String.valueOf(((Player)c).getNode().getLocalPosition().z());
                        packetInfo += "," + String.valueOf(((Player)c).getside());
                    }
                    try {
                        sendPacket(packetInfo,clientmap.get(cli));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                    //forward
                case 22:
                    break;
                    //backward
                case 18:
                    break;
                    //left
                case 0:
                    break;
                    //right
                case 3:
                    break;
                    //jump
                case 9:
                    break;
                    //punch/fire
                case 15:
                    break;

            }

        }
    }
    private void createClient(int side,IClientInfo ci) throws IOException {
        Player newPlayer =  new Player(Spawn(side),side,id);
        cliententities.put(id,newPlayer);
    }

    private Vector3 Spawn(int side){
        if(side ==0){
            return Vector3f.createFrom(0f,0f,-50f);
        }else
            return Vector3f.createFrom(0f,0f,50f);
    }

    public void updateclients() throws IOException {
        String updatePacket;
        updatePacket = String.valueOf(id);
        for(Object o : cliententities.entrySet()){
            updatePacket +="," +  String.valueOf(((Player)o).getNode().getLocalPosition().x());
            updatePacket +="," +  String.valueOf(((Player)o).getNode().getLocalPosition().y());
            updatePacket +="," +  String.valueOf(((Player)o).getNode().getLocalPosition().z());
        }
        sendPacketToAll(updatePacket);
    }
}
