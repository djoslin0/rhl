package a2;

import Networking.PacketScore;
import Networking.UDPServer;
import a2.Contollers.HudController;
import a2.GameEntities.Player;
import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Singletons.AudioManager;
import myGameEngine.Singletons.EntityManager;

public class GameState {
    private static GameState instance = new GameState();
    private int orangeScore = 0;
    private int blueScore = 0;

    public static int getScore(Player.Team team) {
        if (team == Player.Team.Orange) {
            return instance.orangeScore;
        } else {
            return instance.blueScore;
        }
    }

    public static void addScore(Player.Team team, int amount) {
        if (team == Player.Team.Orange) {
            instance.orangeScore += amount;
        } else {
            instance.blueScore += amount;
        }
        HudController.updateScore(team);
        if (UDPServer.hasServer()) { UDPServer.sendToAll(new PacketScore()); }

        if (amount > 0) {
            Player localPlayer = EntityManager.getLocalPlayer();
            if (localPlayer.getSide() == team) {
                AudioManager.get().goalWon.play();
            } else {
                AudioManager.get().goalLost.play();
            }
        }
    }

    public static void setScore(Player.Team team, int amount) {
        boolean update = false;
        if (team == Player.Team.Orange) {
            if (instance.orangeScore != amount) {
                instance.orangeScore = amount;
                HudController.updateScore(team);
                update = true;
            }
        } else {
            if (instance.blueScore != amount) {
                instance.blueScore = amount;
                HudController.updateScore(team);
                update = true;
            }
        }
        if (UDPServer.hasServer()) { UDPServer.sendToAll(new PacketScore()); }

        if (update && amount > 0) {
            Player localPlayer = EntityManager.getLocalPlayer();
            if (localPlayer.getSide() == team) {
                AudioManager.get().goalWon.play();
            } else {
                AudioManager.get().goalLost.play();
            }
        }
    }
}
