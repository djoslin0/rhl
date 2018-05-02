package a2;

import Networking.PacketScore;
import Networking.UDPServer;
import a2.Contollers.HudController;
import a2.GameEntities.Player;

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
    }

    public static void setScore(Player.Team team, int amount) {
        if (team == Player.Team.Orange) {
            instance.orangeScore = amount;
        } else {
            instance.blueScore = amount;
        }
        HudController.updateScore(team);
        if (UDPServer.hasServer()) { UDPServer.sendToAll(new PacketScore()); }
    }
}
