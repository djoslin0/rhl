package Networking;

import a2.GameEntities.Player;
import myGameEngine.NetworkHelpers.AlphabetHashTable;
import ray.networking.client.GameConnectionClient;
import ray.rage.scene.Camera;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Hashtable;

public class UDPClient extends GameConnectionClient {
    private Player player;
    private Camera camera;
    private Hashtable<Integer,Player> otherPlayers;
    private AlphabetHashTable switchVals;
    private static UDPClient client = null;
    private int id = 0;

    private UDPClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, Camera camera) throws IOException {
        super(remoteAddr, remotePort, protocolType);
        player = null;
        this.camera = camera;
        otherPlayers = new Hashtable<>();
        switchVals = new AlphabetHashTable();
    }

    public static UDPClient getClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, Camera camera) throws IOException {
        if(client == null){
            client =  new UDPClient(remoteAddr, remotePort, protocolType, camera);
            return client;
        } else {
            return client;
        }
    }

    public static UDPClient getClient() {
        return client;
    }

    public void processPacket(Object msg)
    {
        String message = (String) msg;
        String[] msgTokens = message.split(",");
        //System.out.println(message);
        if(msgTokens.length > 0){
            switch (switchVals.get(msgTokens[0])) {
                case 28:
                    float pos[] = new float[3];
                    int side;
                    pos[0] = Float.parseFloat(msgTokens[1]);
                    pos[1] = Float.parseFloat(msgTokens[2]);
                    pos[2] = Float.parseFloat(msgTokens[3]);
                    side = Integer.parseInt(msgTokens[4]);
                    try {
                        player = new Player(camera, Vector3f.createFrom(pos[0], pos[1], pos[2]), side);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        requestPlayers();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 29:
                    //System.out.println(message);
                    for(int i =2; i < (Integer.parseInt(msgTokens[1])*4)+2; i+=4){
                        //aaSystem.out.println(i);
                        if(otherPlayers.get((i-2)/4) != null){
                            Vector3 oldPosition = otherPlayers.get((i-2)/4).getNode().getLocalPosition();
                            otherPlayers.get((i-2)/4).getBody().translate(new javax.vecmath.Vector3f(Float.parseFloat(msgTokens[i])-oldPosition.x(), Float.parseFloat(msgTokens[i+1])-oldPosition.y(),Float.parseFloat(msgTokens[i+2])-oldPosition.z()));
                        }else if(msgTokens.length > 2) {
                            System.out.println("Player created with id:"+ id);
                            Vector3 location = Vector3f.createFrom(Float.parseFloat(msgTokens[i]),Float.parseFloat(msgTokens[i+1]),Float.parseFloat(msgTokens[i+2]));
                            try {
                                otherPlayers.put(id,new Player(location,Integer.parseInt(msgTokens[i+3]),id));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            id++;
                        }
                    }
                    break;
            }
        }
    }

    public void requestJoin() throws IOException {
        sendPacket("join");
        System.out.println("attepmting connection");
    }

    public void requestPlayers() throws IOException {
        sendPacket("getPlayers");
    }

    public void sendPositionInfo(Player player) throws IOException {
        String message = "M";
        message += "," + String.valueOf(player.getNode().getLocalPosition().x());
        message += "," + String.valueOf(player.getNode().getLocalPosition().y());
        message += "," + String.valueOf(player.getNode().getLocalPosition().z());
        sendPacket(message);
    }

    public Player getPlayer(){
        return player;
    }
}
