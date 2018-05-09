package myGameEngine.Singletons;

import Networking.UDPServer;
import a3.GameEntities.Player;
import a3.GameEntities.Puck;
import ray.rage.Engine;
import ray.rage.scene.SceneManager;

import java.util.ArrayList;
import java.util.HashMap;

// The EntityManager is responsible for allowing easy access to a list for every entity with a 'listedName' from anywhere
public class EntityManager {
    private static final EntityManager instance = new EntityManager();
    private HashMap<String, ArrayList<Object>> list = new HashMap<String, ArrayList<Object>>();
    private Puck puck;
    private Player localPlayer;

    public static void add(String key, Object object) {
        if (instance.list.get(key) == null) {
            instance.list.put(key, new ArrayList<>());
        }

        if (key.compareTo("puck") == 0) {
            instance.puck = (Puck)object;
        }

        instance.list.get(key).add(object);
    }

    public static void remove(String key, Object object) {
        if (instance.list.get(key) != null) {
            instance.list.get(key).remove(object);
        }
    }

    public static ArrayList<Object> get(String key) {
        return instance.list.get(key);
    }

    public static Puck getPuck() { return instance.puck; }
    public static Player getLocalPlayer() { return instance.localPlayer; }
    public static void setLocalPlayer(Player player) { instance.localPlayer = player; }
}
