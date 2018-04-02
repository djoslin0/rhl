package Networking;

import com.sun.glass.ui.SystemClipboard;
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
        clientmap = new ConcurrentHashMap<>();
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
                            redSide++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        try {
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
                            blueSide++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    break;
                    //sendclientgamestate
                case 27:
                    String packetInfo;
                    packetInfo = "Players";
                    packetInfo += "," + String.valueOf(cliententities.values().size()-1);
                    for(Object c : cliententities.values()){
                        if(((Player)c).getId() != clientmap.get(cli.info())) {
                            packetInfo += "," + String.valueOf(((Player) c).getNode().getLocalPosition().x());
                            packetInfo += "," + String.valueOf(((Player) c).getNode().getLocalPosition().y());
                            packetInfo += "," + String.valueOf(((Player) c).getNode().getLocalPosition().z());
                            packetInfo += "," + String.valueOf(((Player) c).getside());
                        }
                    }
                    try {
                        cli = new ClientInfo(senderIP.toString(),String.valueOf(sndPort));
                        sendPacket(packetInfo,clientmap.get(cli.info()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                   //player has moved
                case 12:
                    Vector3 oldlocation = cliententities.get(clientmap.get(cli.info())).getNode().getLocalPosition();
                    cliententities.get(clientmap.get(cli.info())).getBody().translate( new javax.vecmath.Vector3f
                            (Float.valueOf(msgTokens[1])-oldlocation.x(),Float.valueOf(msgTokens[2])- oldlocation.y(),Float.valueOf(msgTokens[3])-oldlocation.z()));
                    break;
                    //backward
            }

        }
    }
    private void createClient(int side,IClientInfo ci) throws IOException {
        Player newPlayer =  new Player(Spawn(side),side,id);
        newPlayer.getNode().setLocalPosition(0f,1.5f,0f);
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
