package me.z1haze.xpbottler.util;

import org.bukkit.entity.Player;

/**
 * Reads and writes a player's exact XP in points, on top of {@link XPMath}.
 */
public final class PlayerXp {

    private PlayerXp() {
    }

    public static int getTotal(Player player) {
        return XPMath.totalPoints(player.getLevel(), player.getExp(), player.getExpToLevel());
    }

    /**
     * Replaces the player's XP with an exact number of points.
     * setTotalExperience is reset too so the score stays in step with what the player sees.
     */
    public static void set(Player player, int points) {
        player.setLevel(0);
        player.setExp(0f);
        player.setTotalExperience(0);

        if (points > 0) {
            player.giveExp(points);
        }
    }
}
