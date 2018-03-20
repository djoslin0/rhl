package a2.Actions;

import a2.GameEntities.Dolphin;
import a2.GameEntities.Player;
import myGameEngine.Singletons.EngineManager;
import net.java.games.input.Event;
import ray.input.action.Action;

// This action passed a move direction to the player
public class ActionMove implements Action {
    private Player player;
    private Direction direction;
    public enum Direction { FORWARD, BACKWARD, LEFT, RIGHT };

    public ActionMove(Player player, Direction direction) {
        this.player = player;
        this.direction = direction;
    }

    @Override
    public void performAction(float time, Event event) {
        if (!EngineManager.isGameActive()) { return; }
        if (player.isDestroyed()) { return; }
        player.getController().move(direction, Math.abs(event.getValue()));
    }
}
