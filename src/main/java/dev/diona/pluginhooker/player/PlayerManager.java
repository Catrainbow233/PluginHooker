package dev.diona.pluginhooker.player;

import cn.nukkit.Player;
import lombok.Getter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
public class PlayerManager {

    @Getter
    private final Set<DionaPlayer> players = new HashSet<>();

    public void addPlayer(Player player) {
        players.add(new DionaPlayer(player));
    }

    public void removePlayer(Player player) {
        players.removeIf(dionaPlayer -> dionaPlayer.getPlayer() == player);
    }

    public DionaPlayer getDionaPlayer(Player player) {
        if (player == null) return null;
        for (DionaPlayer dionaPlayer : players) {
            if (dionaPlayer.getPlayer().equals(player)) {
                return dionaPlayer;
            }
        }
        return null;
    }

    public void removeAllPlayerCachedListener() {
        Collections.unmodifiableSet(players).forEach(DionaPlayer::removeCachedListener);
    }
}
