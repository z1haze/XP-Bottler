package me.z1haze.xpbottler.util;

/**
 * Vanilla experience formulas, unchanged since 1.8.
 * Pure arithmetic with no Bukkit dependency so it can be unit tested.
 */
public final class XPMath {

    private XPMath() {
    }

    /**
     * Total points needed to reach {@code level} starting from zero.
     * Clamped to Integer.MAX_VALUE, which is already Bukkit's ceiling for a player's XP.
     */
    public static int pointsForLevel(int level) {
        if (level <= 0) return 0;

        long l = level;
        long points;

        if (level <= 16) {
            points = l * l + 6 * l;
        } else if (level <= 31) {
            // 2.5L² − 40.5L + 360, kept in integer math
            points = (5 * l * l - 81 * l) / 2 + 360;
        } else {
            // 4.5L² − 162.5L + 2220, kept in integer math
            points = (9 * l * l - 325 * l) / 2 + 2220;
        }

        return (int) Math.min(points, Integer.MAX_VALUE);
    }

    /**
     * Points needed to go from {@code level} to the next one. Matches Player#getExpToLevel().
     */
    public static int expToLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    /**
     * A player's exact total, from their level, progress bar (0.0 to 1.0) and the size of that bar.
     * Never derive this from Player#getTotalExperience(), which doesn't drop when the player enchants.
     */
    public static int totalPoints(int level, float progress, int expToLevel) {
        long total = (long) pointsForLevel(level) + Math.round(progress * expToLevel);
        return (int) Math.min(total, Integer.MAX_VALUE);
    }
}
