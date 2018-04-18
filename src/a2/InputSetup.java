package a2;

import a2.Actions.*;
import a2.GameEntities.Player;
import myGameEngine.Helpers.HudText;
import net.java.games.input.Controller;
import ray.input.InputManager;
import static net.java.games.input.Component.Identifier.*;
import static ray.input.InputManager.INPUT_ACTION_TYPE;

public class InputSetup {
    private static final InputSetup instance = new InputSetup();

    ActionMove moveForwardAction;
    ActionMove moveBackwardAction;
    ActionMove moveLeftAction;
    ActionMove moveRightAction;

    ActionRotate yawAction;
    ActionRotate pitchAction;

    ActionJump jumpAction;
    ActionAttack attackAction;
    ActionCrouch crouchAction;

    private boolean actions;
    private boolean startedController;

    private void setupActions(Player player) {
        if (actions) { return; }
        actions = true;

        // Player 1

        // movement actions
        moveForwardAction = new ActionMove(player, ActionMove.Direction.FORWARD);
        moveBackwardAction = new ActionMove(player, ActionMove.Direction.BACKWARD);
        moveLeftAction = new ActionMove(player, ActionMove.Direction.LEFT);
        moveRightAction = new ActionMove(player, ActionMove.Direction.RIGHT);
        jumpAction = new ActionJump(player);
        attackAction = new ActionAttack(player);
        crouchAction = new ActionCrouch(player);

        // rotate actions
        yawAction = new ActionRotate(player, ActionRotate.Direction.X);
        pitchAction = new ActionRotate(player, ActionRotate.Direction.Y);

    }

    public static void setupKeyboard(InputManager im, Player player) {
        instance.setupActions(player);

        for (Controller c : im.getControllers()) {
            if (c.getType().equals(Controller.Type.KEYBOARD)) {
                // movement actions
                im.associateAction(c, Key.W, instance.moveForwardAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
                im.associateAction(c, Key.S, instance.moveBackwardAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
                im.associateAction(c, Key.A, instance.moveLeftAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
                im.associateAction(c, Key.D, instance.moveRightAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
                im.associateAction(c, Key.SPACE, instance.jumpAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
                im.associateAction(c, Key.LCONTROL, instance.crouchAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
            }
        }
    }

    public static void setupMouse(InputManager im, Player player) {
        instance.setupActions(player);

        for (Controller c : im.getControllers()) {
            if (c.getType().equals(Controller.Type.MOUSE)) {
                // camera actions
                im.associateAction(c, Axis.X, instance.yawAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
                im.associateAction(c, Axis.Y, instance.pitchAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
                im.associateAction(c, Button.LEFT, instance.attackAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
            }
        }
    }

    public static void setupController(InputManager im, Player player, Controller c) {
        instance.startedController = true;
        instance.setupActions(player);

        // movement / camera actions
        /*im.associateAction(c, Axis.X, instance.xAxisAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.Y, instance.yAxisAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.RX, instance.posAzimuthAction2, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.RY, instance.negElevationAction2, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.Z, instance.zAxisAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Button._0, instance.attachYawAction2, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);*/
    }

    public static void listenToControllers(InputManager im, Player player, HudText helpText) {
        for (Controller c : im.getControllers()) {
            if (c.getType().equals(Controller.Type.GAMEPAD) || c.getType().equals(Controller.Type.STICK)) {
                ActionGamepadStart gamepadStartAction = new ActionGamepadStart(im, player, c, helpText);
                im.associateAction(c, Button._7, gamepadStartAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
            }
        }
    }

    public static boolean hasStartedController() { return instance.startedController; }
}
