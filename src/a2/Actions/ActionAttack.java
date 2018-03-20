package a2.Actions;

import a2.GameEntities.Player;
import myGameEngine.Singletons.EngineManager;
import net.java.games.input.Event;
import ray.input.action.Action;

public class ActionAttack implements Action {
    private Player player;

    public ActionAttack(Player player) {
        this.player = player;
    }

    @Override
    public void performAction(float time, Event event) {
        if (!EngineManager.isGameActive()) { return; }
        if (player.isDestroyed()) { return; }
        player.getController().attack();
    }
}