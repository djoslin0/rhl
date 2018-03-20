package a2.Actions;

import a2.GameEntities.Dolphin;
import a2.GameEntities.Player;
import myGameEngine.Singletons.EngineManager;
import net.java.games.input.Event;
import ray.input.action.Action;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.Viewport;

import java.awt.*;

// This action passed a rotation direction to the player
public class ActionRotate implements Action {
    private Player player;
    private Direction direction;
    private Robot robot;
    public enum Direction { X, Y }

    public ActionRotate(Player player, Direction direction) {
        this.player = player;
        this.direction = direction;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.out.println("Could not create robot!");
        }
    }

    @Override
    public void performAction(float time, Event event) {
        if (!EngineManager.isGameActive()) { return; }
        if (player.isDestroyed()) { return; }
        player.getController().rotate(direction, event.getValue());
        recenterMouse();
    }

    private void recenterMouse() {
        RenderWindow rw = EngineManager.getRenderWindow();
        Viewport v = rw.getViewport(0);
        int x = rw.getLocationLeft() + v.getActualScissorWidth() / 2;
        int y = rw.getLocationTop() + v.getActualScissorHeight() / 2;
        robot.mouseMove(x, y);
    }
}
