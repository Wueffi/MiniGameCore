package wueffi.MiniGameCore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import wueffi.MiniGameCore.managers.LobbyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScoreBoard {

    private static final List<String> animations = Arrays.asList("§b§l» Lobbies", "§b§l» Stats");
    private static final String TITLE = "§6§lMiniGameCore";
    private static int animationIndex = 0;

    public static void createGameBoard(Player player, List<Player> alive) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("game", "dummy", TITLE);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int count = alive != null ? alive.size() : 0;

        obj.getScore("§fAlive: §a" + count + "/" + LobbyManager.getLobbyByPlayer(player).getPlayers().size()).setScore(count + 4);
        obj.getScore("§f───────────────").setScore(count + 3);

        if (alive == null || alive.isEmpty()) {
            obj.getScore("§cNo players alive!").setScore(count + 2);
        } else {
            for (int i = 0; i < alive.size(); i++) {
                obj.getScore("§f- " + alive.get(i).getName()).setScore(count - i + 1);
            }
        }

        obj.getScore("§r§f").setScore(2);
        obj.getScore("§7─────────────────").setScore(1);
        obj.getScore("§7Made with ❤ by Waffle").setScore(0);

        player.setScoreboard(board);
    }

    public static void createLobbyBoard(Player player, Lobby lobby) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("lobby", "dummy", TITLE);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<Player> players = new ArrayList<>(lobby.getPlayers());
        int maxPlayers = lobby.getMaxPlayers();

        obj.getScore("§r").setScore(players.size() + 6);
        obj.getScore("§fPlayers: §a" + players.size() + "/" + maxPlayers + " | Ready: " + lobby.getReadyPlayers().size() + "/" + lobby.getPlayers().size()).setScore(players.size() + 5);
        obj.getScore("§f───────────────").setScore(players.size() + 4);

        if (players.isEmpty()) {
            obj.getScore("§cNo players yet!").setScore(3);
        } else {
            for (int i = 0; i < players.size(); i++) {
                if (!lobby.getReadyPlayers().contains(player)) {
                    obj.getScore("§f- " + players.get(i).getName()).setScore(players.size() - i + 2);
                } else {
                    obj.getScore("§f- §a" + players.get(i).getName()).setScore(players.size() - i + 2);
                }
            }
        }

        obj.getScore("§r§f").setScore(2);
        obj.getScore("§7─────────────────").setScore(1);
        obj.getScore("§7Made with ❤ by Waffle").setScore(0);

        player.setScoreboard(board);
    }

    public static void createIdleBoard(Player player, String mostPlayed, int played, int won, int lost, List<Lobby> openLobbies, List<Lobby> closedLobbies) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = animations.get(animationIndex);
        Objective obj = board.registerNewObjective("idle", "dummy", TITLE);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (animationIndex == 0) {
            if (openLobbies.isEmpty() && closedLobbies.isEmpty()) {
                obj.getScore(title).setScore(10);
                obj.getScore("§l").setScore(9);
                obj.getScore("§cNo active Lobbies!").setScore(8);
                obj.getScore("§1").setScore(7);
                obj.getScore("§2").setScore(6);
                obj.getScore("§3").setScore(5);
                obj.getScore("§4").setScore(4);
                obj.getScore("§5").setScore(3);
            } else {
                int score = 10 + Math.max(0, openLobbies.size() - 3) + Math.max(0, closedLobbies.size() - 3); // crazy calculations ikr
                obj.getScore(title).setScore(score--);
                obj.getScore("§2§lOpen Lobbies:").setScore(score--);
                for (Lobby lobby : openLobbies) {
                    if (openLobbies.size() <= 3) {
                        obj.getScore("§6- " + lobby.getLobbyId()).setScore(score--);
                    }
                    if (openLobbies.size() <= 2) {
                        obj.getScore("§c").setScore(score--);
                    }
                    if (openLobbies.size() <= 1) {
                        obj.getScore("§d").setScore(score--);
                    } //sheesh
                }

                obj.getScore("§4§lRunning Games:").setScore(score--);
                for (Lobby lobby : closedLobbies) {
                    if (closedLobbies.size() <= 3) {
                        obj.getScore("§6- " + lobby.getLobbyId()).setScore(score--);
                    }
                    if (closedLobbies.size() <= 2) {
                        obj.getScore("§e").setScore(score--);
                    }
                    if (closedLobbies.size() <= 1) {
                        obj.getScore("§f").setScore(score--);
                    } // more sizing (atleast 10 lines)
                }
            }
        } else {
            obj.getScore(title).setScore(10);
            obj.getScore("§r").setScore(9);
            obj.getScore("§f§lYour Stats:").setScore(8);
            obj.getScore("§fMost played Game: §3" + mostPlayed).setScore(7);
            obj.getScore("§fTotal played: §2" + played).setScore(6);
            obj.getScore("§fWon: §6" + won).setScore(5);
            obj.getScore("§fLost: §c" + lost).setScore(4);

            float winrate = played > 0 ? ((float) won / played) * 100 : 0;
            winrate = Math.round(winrate * 10) / 10.0f;
            obj.getScore("§fWinrate: §5" + winrate + "%").setScore(3);
        }

        obj.getScore("§r§f").setScore(2);
        obj.getScore("§7─────────────────").setScore(1);
        obj.getScore("§7Made with ❤ by Waffle").setScore(0);

        player.setScoreboard(board);
    }

    public static void tickAnimation() {
        animationIndex = (animationIndex + 1) % animations.size();
    }

    public static int getAnimationIndex() {
        return animationIndex;
    }
}
