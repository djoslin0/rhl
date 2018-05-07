package a2;

import Networking.PacketMatch;
import Networking.PacketScore;
import Networking.UDPClient;
import Networking.UDPServer;
import a2.Contollers.HudController;
import a2.GameEntities.Player;
import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Helpers.Updatable;
import myGameEngine.Singletons.AudioManager;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.Settings;
import myGameEngine.Singletons.UpdateManager;

import java.util.ArrayList;

public class GameState implements Updatable {
    private static GameState instance = new GameState();
    private int orangeScore = 0;
    private int blueScore = 0;
    private float secondsLeft = Settings.get().matchSeconds.intValue();
    private float matchOverSecondsLeft = 0;
    private boolean matchOver = false;

    public GameState() {
        UpdateManager.add(this);
    }

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

    public static void endMatch(Player.Team winningTeam) {
        instance.matchOver = true;
        instance.secondsLeft = 0;
        instance.matchOverSecondsLeft = Settings.get().intermissionSeconds.intValue();
        if (UDPClient.hasClient()) {
            instance.matchOverSecondsLeft = 60;
        } else {
            EntityManager.getPuck().reset(false, false);
        }
        if (UDPServer.hasServer()) { UDPServer.sendToAll(new PacketMatch()); }

        HudController.showWinLose(EntityManager.getLocalPlayer().getSide() == winningTeam);
    }

    public static void resetMatch() {
        instance.matchOver = false;
        instance.secondsLeft = Settings.get().matchSeconds.intValue();
        instance.matchOverSecondsLeft = 0;
        if (UDPServer.hasServer()) { UDPServer.sendToAll(new PacketMatch()); }

        // reset players
        for (Object o : EntityManager.get("player")) {
            Player player = (Player) o;
            if (player.getId() == 0) { continue; }
            player.die();
            player.respawn();
        }

        setScore(Player.Team.Orange, 0);
        setScore(Player.Team.Blue, 0);
        HudController.hideWinLose();
    }

    @Override
    public void update(float delta) {
        if (UDPServer.hasServer() && UDPServer.getPlayers().size() < 1) {
            return;
        }
        if (UDPClient.hasClient()) {
            return;
        }

        if (matchOver) {
            matchOverSecondsLeft -= delta / 1000f;
            if (matchOverSecondsLeft <= 0) {
                resetMatch();
            }
            return;
        }

        if (!EntityManager.getPuck().isFrozen()) {
            secondsLeft -= delta / 1000f;
        }
        if (secondsLeft <= 0) {
            if (orangeScore != blueScore) {
                endMatch(orangeScore > blueScore ? Player.Team.Orange : Player.Team.Blue);
            }
        }
        if (secondsLeft <= -1) {
            secondsLeft = -1;
        }
    }

    @Override
    public boolean blockUpdates() { return false; }
    public static float getSecondsLeft() { return instance.secondsLeft; }
    public static void setSecondsLeft(float secondsLeft) { instance.secondsLeft = secondsLeft; }
    public static boolean isMatchOver() { return instance.matchOver; }
}
