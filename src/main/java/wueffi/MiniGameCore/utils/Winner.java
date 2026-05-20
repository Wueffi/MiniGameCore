package wueffi.MiniGameCore.utils;

import org.bukkit.entity.Player;

import java.util.Collection;

public sealed interface Winner
        permits Winner.PlayerWinner, Winner.TeamWinner, Winner.TieWinner, Winner.Aborted {

    record PlayerWinner(Player player) implements Winner {
    }

    record TeamWinner(Team team) implements Winner {
    }

    record TieWinner(Collection<Player> playerList) implements Winner {
    }

    record Aborted() implements Winner {
    }
}
