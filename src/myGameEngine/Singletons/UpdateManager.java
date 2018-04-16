package myGameEngine.Singletons;

import myGameEngine.Helpers.Updatable;

import java.util.ArrayList;

// The UpdateManager is responsible for allowing entities to add and remove themselves from the update list
public class UpdateManager {
    private static final UpdateManager instance = new UpdateManager();
    private ArrayList<Updatable> updateList;
    private boolean paused = false;

    public UpdateManager() {
        updateList = new ArrayList<>();
    }

    public static void pause(boolean paused) { instance.paused = paused; }
    public static void add(Updatable updatable) {
        if (!instance.updateList.contains(updatable)) {
            instance.updateList.add(updatable);
        }
    }
    public static void remove(Updatable updatable) {
        instance.updateList.remove(updatable);
    }

    public static void update(float delta) {
        if (instance.paused) { return; }
        Updatable[] savedList = new Updatable[instance.updateList.size()];
        savedList = instance.updateList.toArray(savedList);
        for (Updatable updatable : savedList) {
            if (updatable.blockUpdates()) { continue; }
            updatable.update(delta);
        }
    }

    public static void clear() {
        instance.updateList.clear();
    }
}
