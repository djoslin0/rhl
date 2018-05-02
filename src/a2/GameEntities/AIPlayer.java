package a2.GameEntities;

import a2.Actions.ActionMove;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class AIPlayer extends Player{
    private Vector3 goalPosition;
    private Vector3 puckPosition;
    public AIPlayer(byte playerId, boolean local, Player.Team side) {
        super(playerId, local, side);
    }
    public void update(float delta){
        super.update(delta);
        puckPosition = EntityManager.getPuck().getNode().getWorldPosition();
        if(this.getSide() == Team.Orange){
           goalPosition = EngineManager.getSceneManager().getSceneNode("Goalbox0").getWorldPosition();
        }else{
            goalPosition = EngineManager.getSceneManager().getSceneNode("Goalbox1").getWorldPosition();
        }
        this.lookAt(puckPosition);
        Vector3 attack = getPosition().sub(puckPosition);
        Vector3 goalPuckDif = puckPosition.sub(goalPosition);
        Vector3 diff = goalPuckDif.normalize().mult(4f);
        Vector3 desiredLocation = goalPuckDif.add(diff).add(goalPosition);
        // lookup player
        Vector3 least = getPosition().sub(puckPosition);
        boolean me = true;
        for (Object o : EntityManager.get("player")) {
            Player player = (Player)o;
            if(player.getPosition().sub(puckPosition).length()<least.length() && !(player == this)){
                me = false;
            }

        }
        if (me == false) {
            desiredLocation = puckPosition.sub(goalPosition).mult(.5f);
        }
        this.lookAt(desiredLocation);
        if(getPosition().sub(desiredLocation).length()<3f && me==true){
            this.lookAt(puckPosition);
            getController().move(ActionMove.Direction.FORWARD,true);
            getController().attack();
        }else{
            if(getPosition().sub(desiredLocation).length()<2f){
                getController().move(ActionMove.Direction.FORWARD,false);
                lookAt(puckPosition);
            }else{
                getController().move(ActionMove.Direction.FORWARD,true);
            }

        }

    }
}
