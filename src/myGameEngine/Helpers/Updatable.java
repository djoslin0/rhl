package myGameEngine.Helpers;

// Updatable entities can be automatically called from the UpdateManager
public interface Updatable {
    void update(float delta);
    boolean blockUpdates();
}
