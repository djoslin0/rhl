package a3;

import a3.Actions.*;
import a3.GameEntities.Player;
import myGameEngine.Actions.ActionAxis;
import myGameEngine.Actions.ActionAxisToggle;
import myGameEngine.Actions.ActionScale;
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

    ActionAxis xAction;
    ActionAxis yAction;
    ActionAxisToggle zAction;
    ActionScale rxAction;
    ActionScale ryAction;

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

        // controller actions
        xAction = new ActionAxis(moveLeftAction, moveRightAction);
        yAction = new ActionAxis(moveForwardAction, moveBackwardAction);
        zAction = new ActionAxisToggle(attackAction, attackAction, 0.6f, 0.4f);
        rxAction = new ActionScale(yawAction, 8f);
        ryAction = new ActionScale(pitchAction, 8f);
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
        im.associateAction(c, Axis.X, instance.xAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.Y, instance.yAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.RX, instance.rxAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.RY, instance.ryAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Axis.Z, instance.zAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(c, Button._0, instance.jumpAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateAction(c, Button._1, instance.crouchAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
        im.associateAction(c, Button._5, instance.jumpAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
        im.associateAction(c, Button._4, instance.crouchAction, INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
    }

    public static void listenToControllers(InputManager im, Player player) {
        for (Controller c : im.getControllers()) {
            if (c.getType().equals(Controller.Type.GAMEPAD) || c.getType().equals(Controller.Type.STICK)) {
                ActionGamepadStart gamepadStartAction = new ActionGamepadStart(im, player, c);
                im.associateAction(c, Button._7, gamepadStartAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
            }
        }
    }

    public static boolean hasStartedController() { return instance.startedController; }
}
