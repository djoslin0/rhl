package myGameEngine.GameEntities;

import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.UpdateManager;

public class GameEntityUpdatable extends GameEntity implements Updatable {

    public GameEntityUpdatable() {
        // enable update calls
        UpdateManager.add(this);
    }

    @Override
    public void destroy() {
        super.destroy();
        // disable update calls
        UpdateManager.remove(this);
    }

    @Override
    public void update(float delta) { }

    @Override
    public boolean blockUpdates() {
        // do not update when we are destroyed
        return isDestroyed();
    }
}
