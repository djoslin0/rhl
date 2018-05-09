package a3.Actions;

import a3.GameEntities.Player;
import a3.InputSetup;
import myGameEngine.Helpers.HudText;
import myGameEngine.Singletons.EngineManager;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import ray.input.InputManager;
import ray.input.action.Action;

// This action is triggered when the start button is pressed and registers actions on a controller

public class ActionGamepadStart implements Action {
    private InputManager im;
    private Player player;
    private Controller controller;
    private HudText helpText;
    private boolean isSetup;

    public ActionGamepadStart(InputManager im, Player player, Controller controller, HudText helpText) {
        this.im = im;
        this.player = player;
        this.controller = controller;
        this.helpText = helpText;
    }

    @Override
    public void performAction(float time, Event event) {
        // only run setup once per gamepad
        if (isSetup) { return; }
        isSetup = true;

        // hide "press START" text
        if (helpText.text.contains("gamepad")) { helpText.text = "Score: 0"; }

        // register actions on controller
        InputSetup.setupController(im, player, controller);

        // begin
        EngineManager.setGameActive(true);
    }
}
