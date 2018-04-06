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
    private int id = -1;

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
        int id = 0;
        if(msgTokens.length > 0){
            switch (switchVals.get(msgTokens[0])) {
                case 28:
                    //Success
                    float pos[] = new float[3];
                    int side;
                    id = Integer.parseInt(msgTokens[1]);
                    pos[0] = Float.parseFloat(msgTokens[2]);
                    pos[1] = Float.parseFloat(msgTokens[3]);
                    pos[2] = Float.parseFloat(msgTokens[4]);
                    side = Integer.parseInt(msgTokens[5]);
                    if (this.id == -1) {
                        try {
                                player = new Player(id, camera, Vector3f.createFrom(pos[0], pos[1], pos[2]), side);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    this.id = id;
                    try {
                        requestPlayers();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case 29:
                    //Players
                    int playerCount = Integer.parseInt(msgTokens[1]);
                    for(int i = 0; i < playerCount; i++){
                        System.out.println(i);
                        id = Integer.parseInt(msgTokens[i*5 + 2]);
                        float x = Float.parseFloat(msgTokens[i*5 + 3]);
                        float y = Float.parseFloat(msgTokens[i*5 + 4]);
                        float z = Float.parseFloat(msgTokens[i*5 + 5]);
                        side = Integer.parseInt(msgTokens[i*5 + 6]);
                        if (id == this.id) { continue; }

                        if(otherPlayers.get(id) != null) {
                            Vector3 oldPosition = otherPlayers.get(id).getNode().getLocalPosition();
                            otherPlayers.get(id).getBody().translate(new javax.vecmath.Vector3f(x - oldPosition.x(), y - oldPosition.y(),z - oldPosition.z()));
                        } else if (id != this.id) {
                            System.out.println("Player created with id: "+ id);
                            Vector3 location = Vector3f.createFrom(x, y, z);
                            try {
                                otherPlayers.put(id, new Player(id, null, location, side));
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
