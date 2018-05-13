package a3.Contollers;

import a3.Actions.ActionMove;
import a3.GameEntities.AIPlayer;
import a3.GameEntities.CollisionBox;
import a3.GameEntities.Player;
import myGameEngine.Helpers.MathHelper;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.EntityManager;
import ray.rml.Quaternion;
import ray.rml.Vector2f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class AiController {
    private AIPlayer aiPlayer;
    private States currentState = States.InSpawn;

    private float accuracy;

    private Vector3 goalPosition;
    private Vector3 enemyGoalPosition;
    private Vector3 desiredLocation;
    private Vector3 target;

    private boolean dunnking = false;

    private static int numBots = 0;
    private int botNum;
    private int dunkTicks = 0;


    private static  CollisionBox blueBox = new CollisionBox(8f,16f,16f, Vector3f.createFrom(83f,5.9f,0.0f));;
    private static CollisionBox orangeBox = new CollisionBox(8f,16f,16f, Vector3f.createFrom(-83f,3.9f,0.0f));
    private static CollisionBox inSpawn = new CollisionBox(150f,14f,4f,Vector3f.createFrom(0f,7f,-54f));

    public AiController(AIPlayer aiPlayer){

        numBots++;
        botNum = numBots;
        this.aiPlayer = aiPlayer;
        target = Vector3f.createFrom(0f,0f,0f);
        desiredLocation = Vector3f.createFrom(0f,0f,0f);

        //get players goal size
        if(aiPlayer.getSide() == Player.Team.Orange){
            goalPosition = EngineManager.getSceneManager().getSceneNode("Goalbox0").getWorldPosition();
            enemyGoalPosition = EngineManager.getSceneManager().getSceneNode("Goalbox1").getWorldPosition();
        }else{
            goalPosition = EngineManager.getSceneManager().getSceneNode("Goalbox1").getWorldPosition();
            enemyGoalPosition = EngineManager.getSceneManager().getSceneNode("Goalbox0").getWorldPosition();
        }
    }
    static enum States{
        Thinking, Shooting , InSpawn, Reset, InGaol, BehindGoal, Defending,Dunking,MakingPlay,Goalie
    }

    public void desideCurrentAction(){
       Vector3 puckPos = EntityManager.getPuck().getNode().getWorldPosition();
       Vector3 optPos =  Vector3f.createFrom(0f,0f,0f);
       javax.vecmath.Vector3f puckVelocity = new javax.vecmath.Vector3f();
       EntityManager.getPuck().getBody().getLinearVelocity(puckVelocity);
        if(inSpawn(aiPlayer.getPosition())){
            currentState = States.InSpawn;
        }
       switch (currentState){
           // in spawn

           case Thinking:
               if(inSpawn(aiPlayer.getPosition())){
                   currentState = States.InSpawn;
                   break;
               }
               // reset
               if(EntityManager.getPuck().isFrozen() && !aiPlayer.isDead()){
                   currentState = States.Reset;
                   break;
               }
               // make a play on attack
               if(getFurthestFreindly() == true && Math.abs(puckPos.sub(goalPosition).length()) < 60f) {
            	   currentState = States.MakingPlay;
            	   break;
               }
               // goalie state
               if(getFurthestFreindly() == true && Math.abs(puckPos.sub(enemyGoalPosition).length()) < 60f){
                   currentState = States.Goalie;
                   break;
               }
               // in goal
               if(Math.abs(desiredLocation.z()) > 49f && currentState != States.InSpawn){
                   accuracy = 1f;
                   if(desiredLocation.z() < 0){
                       desiredLocation = Vector3f.createFrom(desiredLocation.x(),aiPlayer.getPosition().y(),-49f);
                   }else{
                       desiredLocation = Vector3f.createFrom(desiredLocation.x(),aiPlayer.getPosition().y(),49f);
                   }
                   target = puckPos;
                   break;
               }
               if(Math.abs(desiredLocation.x()) > 109f && currentState != States.InSpawn){
                   accuracy = 4f;
                   if(desiredLocation.x() < 0f){
                       desiredLocation = Vector3f.createFrom(-109f,aiPlayer.getPosition().y(),desiredLocation.z());
                   }else{
                       desiredLocation = Vector3f.createFrom(109f,aiPlayer.getPosition().y(),desiredLocation.z());
                   }
                   break;
               }
               if( Math.abs(desiredLocation.x()) > 94f && Math.abs(desiredLocation.z()) > 32f && currentState != States.InSpawn){
                   accuracy = 5f;
                   desiredLocation = Vector3f.createFrom(puckPos.x(),aiPlayer.getPosition().y(),puckPos.z());
                   target = puckPos;
                   break;
               }

               // in a goal get out
               if(inGoal(aiPlayer.getPosition())){
                   currentState = States.InGaol;
                   break;
               }

               // stuck behind goal get back
                if(stuckBehindGoal()){
                   currentState = States.BehindGoal;
                   break;
                }
               // behind goal then dunk
               if(behindGoal()){
                   currentState = States.Dunking;
                   break;
               }

               // shooting or defending
               if(goalPosition.x() < 0f){
                   if(puckPos.x() <= 0){
                       currentState = States.Shooting;
                   }else if(puckPos.x() > 0f){
                       if(getClosestEnemy()!= null && aiPlayer.getPosition().sub(puckPos).length() > getClosestEnemy().length() ){
                           currentState = States.Defending;
                       }else{
                           currentState = States.Shooting;
                       }
                   }
               }else if(goalPosition.x() > 0){
                   if(puckPos.x() > 0f){
                       currentState = States.Shooting;
                   }else {
                       if(getClosestEnemy() !=null && aiPlayer.getPosition().sub(puckPos).length() > getClosestEnemy().length() ){
                           currentState = States.Defending;
                       }else{
                           currentState = States.Shooting;
                       }
                   }
               }

               break;
               //dunk
           case Dunking:
               accuracy = 0.5f;
               target = Vector3f.createFrom(puckPos.x(),puckPos.y() + 2.3f, puckPos.z());
               optPos = puckPos.sub(goalPosition).normalize().mult(4.3f);
               desiredLocation = puckPos.add(Vector3f.createFrom(optPos.x(),aiPlayer.getPosition().y(),optPos.z()));
               if(aiPlayer.getPosition().sub(desiredLocation).length() < 2f)
               {
                   aiPlayer.getController().setCrouching(true);
               }else {
                   aiPlayer.getController().setCrouching(false);
               }
               if(dunnking == true || !behindGoal() || Math.abs(aiPlayer.getPosition().x())< (Math.abs(goalPosition.x())-3f)){
                   dunnking = false;
                   currentState = States.Thinking;
                   aiPlayer.getController().setCrouching(false);
               }

               break;
               // shoot
           case Shooting:
               target = puckPos;
               if(goalPosition.x()<0){
                   accuracy = (puckPos.x() < -0.4f)? 0.25f : 1;
                   optPos = bestShot();

               }else{
                   accuracy = (puckPos.x() > 0.4f)? 0.25f : 1;
                   optPos = bestShot();
               }

                desiredLocation = puckPos.add(Vector3f.createFrom(optPos.x(),aiPlayer.getPosition().y(),optPos.z()));
                currentState = States.Thinking;
               break;
           case Defending:
               accuracy = 1f;
               target = puckPos;
               optPos = puckPos.sub(goalPosition).mult(0.5f);
               desiredLocation = Vector3f.createFrom(optPos.x(),aiPlayer.getPosition().y(),optPos.z());
               currentState = States.Thinking;
               break;
               // get out of spawn
           case InSpawn:
               accuracy = 4f;
               target = Vector3f.createFrom(0.0f,aiPlayer.getPosition().y(),-35f);
               desiredLocation = Vector3f.createFrom(0.0f,aiPlayer.getPosition().y(),-35f);
               if(aiPlayer.getPosition().sub(desiredLocation).length() < accuracy){
                   currentState = States.Thinking;
               }
               break;
           case InGaol:
               accuracy = 2f;
                if(aiPlayer.getPosition().x() < 0f){
                    if(puckPos.z() < 0f){
                        desiredLocation = Vector3f.createFrom(-70f,aiPlayer.getPosition().y(),-14.0f);
                    }else{
                        desiredLocation = Vector3f.createFrom(-70f,aiPlayer.getPosition().y(),14.0f);
                    }
                }else{
                    if(puckPos.z() < 0f){
                        desiredLocation = Vector3f.createFrom(70f,aiPlayer.getPosition().y(),-14.0f);
                    }else{
                        desiredLocation = Vector3f.createFrom(70f,aiPlayer.getPosition().y(),14.0f);
                    }
                }
                if(aiPlayer.getPosition().sub(desiredLocation).length() < accuracy || aiPlayer.getPosition().sub(puckPos).length()<5f || aiPlayer.isDead()){
                    currentState = States.Thinking;
                }
               break;
           case BehindGoal:
               target = puckPos;
               accuracy = 2f;
               if(aiPlayer.getPosition().x() < 0f){
                   if(puckPos.z() < 0f){
                       desiredLocation = Vector3f.createFrom(-83f,aiPlayer.getPosition().y(),-14.0f);
                   }else{
                       desiredLocation = Vector3f.createFrom(-83f,aiPlayer.getPosition().y(),14.0f);
                   }
               }else{
                   if(puckPos.z() < 0f){
                       desiredLocation = Vector3f.createFrom(83f,aiPlayer.getPosition().y(),-14.0f);
                   }else{
                       desiredLocation = Vector3f.createFrom(83f,aiPlayer.getPosition().y(),14.0f);
                   }
               }
               if(aiPlayer.getPosition().sub(desiredLocation).length() < 4f || aiPlayer.isDead()){
                   currentState = States.Thinking;
               }
               break;
               // get to your side
           case Reset:
               int pos = (botNum % 2 == 0)? -botNum : botNum;
               target = puckPos;
               if(aiPlayer.getSide() == Player.Team.Orange){
                   desiredLocation = enemyGoalPosition .add(Vector3f.createFrom(65f,0f,pos * 5f));
               }else{
                   desiredLocation = enemyGoalPosition .add(Vector3f.createFrom(-65f,0f,pos * 5f));
               }

               if(!EntityManager.getPuck().isFrozen() || aiPlayer.isDead() || inSpawn(aiPlayer.getPosition())){
                   currentState = States.Thinking;
               }
               break;
               // make a play on attack
           case MakingPlay:
               accuracy = 1f;
               aiPlayer.getController().jump();
               accuracy = 1f;
               int scaler = -1;
               if(goalPosition.x() < 0){
                   scaler = 1;
               }
               desiredLocation = Vector3f.createFrom(goalPosition.add(scaler * 50f,0f,0f).x(),aiPlayer.getPosition().y(),puckPos.z() + bestShot().z());
               target = puckPos;
               if(aiPlayer.getPosition().sub(puckPos).length() < 8f || puckPos.sub(goalPosition).length() >= 65f){
                   currentState = States.Shooting;
               }

        	   break;
        	   // play as the goalie
           case Goalie:
               accuracy = 1f;
               if(goalPosition.x() < 0){
                   scaler = 1;
               }else{
                   scaler = -1;
               }
               desiredLocation = Vector3f.createFrom(scaler *78f,aiPlayer.getPosition().y(),puckPos.z()/49f * 8f);
               target = puckPos;
               currentState = States.Thinking;
               break;
       }


    }
    public void executeCurrentAction(){
        if(currentState == States.MakingPlay){
            System.out.println(desiredLocation);
        }
        Vector3 puckPos = EntityManager.getPuck().getNode().getWorldPosition();
        desiredLocation = Vector3f.createFrom(desiredLocation.x(),aiPlayer.getPosition().y(),desiredLocation.z());
        if(aiPlayer.getPosition().sub(desiredLocation).length()>accuracy){
            aiPlayer.lookAt(desiredLocation);
            aiPlayer.getController().move(ActionMove.Direction.FORWARD,true);
        }else if(aiPlayer.getPosition().sub(desiredLocation).length() < accuracy && currentState != States.Shooting && currentState != States.Dunking ){
            aiPlayer.getController().move(ActionMove.Direction.FORWARD,false);
            aiPlayer.lookAt(puckPos);
            if(aiPlayer.getPosition().sub(puckPos).length() < 7f){
                aiPlayer.getController().attack();
            }
        }else if(currentState == States.Shooting || currentState == States.Dunking){

            aiPlayer.getController().move(ActionMove.Direction.FORWARD,false);
            aiPlayer.lookAt(target);
            aiPlayer.getController().attack();
            if(currentState == States.Dunking)
            {
                dunkTicks++;
                if(dunkTicks >= 10 || aiPlayer.getPosition().sub(desiredLocation).length() > 2f){
                    dunnking = true;
                    dunkTicks = 0;
                }
            }
        }
    }

    private Vector3 bestShot(){
        Vector3 puckPos = EntityManager.getPuck().getNode().getWorldPosition();
        return puckPos.sub(goalPosition).normalize().mult(7f);
    }
    private boolean stuckBehindGoal(){
        Vector3 puckPos  = EntityManager.getPuck().getNode().getWorldPosition();
        if(Math.abs(aiPlayer.getPosition().z()) < 8f && Math.abs(aiPlayer.getPosition().x()) > 85f  && Math.abs(puckPos.x()) < 79f){
            return true;
        }else{
            return  false;
        }

    }
    private boolean behindGoal(){
        Vector3 puckPos = EntityManager.getPuck().getNode().getWorldPosition();
        if(goalPosition.x() < 0f){
            if(puckPos.x() < goalPosition.x()){
                return true;
            }else{
               return false;
            }

        }else{
             if(goalPosition.x() > 0f){
                 if(puckPos.x() > goalPosition.x()){
                     return  true;
                 }else{
                     return  false;
                 }
             }
        }
            return false;
    }
    public static boolean inSpawn(Vector3 position){ return inSpawn.contains(position); }
    public  static boolean inGoal(Vector3 position){return (blueBox.contains(position)|| orangeBox.contains(position));}
    public void lookAt(Vector3 target){
        Quaternion origin = aiPlayer.getNode().getLocalRotation().toQuaternion();
        aiPlayer.lookAt(target);
        Quaternion end = aiPlayer.getNode().getLocalRotation().toQuaternion();
        MathHelper.slerp(origin,end,0.1f);

    }

    private Vector3 getClosestFreindly(){
    	Vector3 puckPos = EntityManager.getPuck().getNode().getLocalPosition();
        Vector3 closestsFreindly = null;
        for(Object o : EntityManager.get("player")){
            Player player = (Player)o;
            if(player.isDead()){continue;}
            if(player == null) {continue;}
            if(player.getSide() != aiPlayer.getSide()) {continue;}
            if(player == aiPlayer) {continue;}
            if(closestsFreindly == null || closestsFreindly.sub(puckPos).length() > player.getPosition().sub(puckPos).length()){
                closestsFreindly = player.getPosition();
            }
        }
        return closestsFreindly;
    }
    private boolean getFurthestFreindly(){
        boolean friends = false;
        boolean meFurthest = true;
        Vector3 puckPos = EntityManager.getPuck().getNode().getLocalPosition();
        for(Object o : EntityManager.get("player")){
            Player player = (Player)o;
            if(player == null) { continue;}
            if(player.getSide() != aiPlayer.getSide()) {continue;}
            if(player.getSide() == aiPlayer.getSide()) { friends = true;}
            if(player.isDead()){ meFurthest = false; continue;}
            if(player == aiPlayer) {continue;}
            if(Math.abs(aiPlayer.getPosition().sub(puckPos).length()) < Math.abs(player.getPosition().sub(puckPos).length())){
                meFurthest = false;
            }
        }
        if(friends == false){
            return  false;
        }else{
            return meFurthest;
        }

    }
    
    private Vector3 getClosestEnemy(){
        Vector3 closestEnemy = null;
        for(Object o : EntityManager.get("player")){
            Player player = (Player)o;
            if(aiPlayer.getSide()!= player.getSide() && (closestEnemy == null || closestEnemy.sub(EntityManager.getPuck().getNode().getLocalPosition()).length() > player.getPosition().sub(EntityManager.getPuck().getNode().getLocalPosition()).length())){
                closestEnemy = player.getPosition();
            }
        }
        return closestEnemy;
    }
}
