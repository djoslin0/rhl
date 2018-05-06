
package a2.GameEntities;

import a2.Actions.ActionMove;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class AIPlayer extends Player{
    private Vector3 goalPosition;
    private Vector3 puckPosition;
    private boolean attacking;
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
        Vector3 diff = goalPuckDif.normalize().mult(6f);
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
        if (me == false && puckPosition.x() < 0) {
            desiredLocation = puckPosition.sub(goalPosition).mult(.5f);
        }
        desiredLocation = Vector3f.createFrom(desiredLocation.x(), getPosition().y(), desiredLocation.z());
        this.lookAt(desiredLocation);

        float accuracy = (puckPosition.x() > -0.4) ? 0.25f : 1;
        if (attacking) {
            this.lookAt(puckPosition);
            getController().move(ActionMove.Direction.FORWARD,false);
            attacking = false;
        } else if(getPosition().sub(desiredLocation).length()<accuracy && me==true){
            this.lookAt(puckPosition);
            getController().attack();
            attacking = true;
        }else{
            getController().move(ActionMove.Direction.FORWARD,true);
        }

    }
}
