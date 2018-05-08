
package a2.GameEntities;

import a2.Actions.ActionMove;
import a2.Contollers.AiController;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class AIPlayer extends Player{
    private AiController controller;
    public AIPlayer(byte playerId, boolean local, Player.Team side, byte headId) {
        super(playerId, local, side, headId);
        controller = new AiController(this);
    }
    public void update(float delta){
        super.update(delta);
        controller.desideCurrentAction();
        controller.executeCurrentAction();
    }
}
