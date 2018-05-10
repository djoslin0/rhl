
package a3.GameEntities;

import a3.Actions.ActionMove;
import a3.Contollers.AiController;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class AIPlayer extends Player{
    private AiController AIcontroller;
    public AIPlayer(byte playerId, boolean local, Player.Team side, byte headId) {
        super(playerId, local, side, headId);
        AIcontroller = new AiController(this);
    }
    public void update(float delta){
        super.update(delta);
        AIcontroller.desideCurrentAction();
        AIcontroller.executeCurrentAction();
    }
}
