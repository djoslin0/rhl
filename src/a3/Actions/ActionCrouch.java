package a3.Actions;

import a3.GameEntities.Player;
import myGameEngine.Singletons.EngineManager;
import net.java.games.input.Event;
import ray.input.action.Action;

public class ActionCrouch implements Action {
    private Player player;

    public ActionCrouch(Player player) {
        this.player = player;
    }

    @Override
    public void performAction(float time, Event event) {
        if (!EngineManager.isGameActive()) { return; }
        if (player.isDestroyed()) { return; }
        player.getController().setCrouching(event.getValue() != 0);
    }
}