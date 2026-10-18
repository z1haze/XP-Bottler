package me.z1haze.xpbottler.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XPMathTest {

    @Test
    void pointsForLevelMatchesVanillaAtFormulaBoundaries() {
        assertEquals(0, XPMath.pointsForLevel(0));
        assertEquals(7, XPMath.pointsForLevel(1));
        assertEquals(315, XPMath.pointsForLevel(15));
        assertEquals(352, XPMath.pointsForLevel(16));
        assertEquals(394, XPMath.pointsForLevel(17));
        assertEquals(1395, XPMath.pointsForLevel(30));
        assertEquals(1507, XPMath.pointsForLevel(31));
        assertEquals(1628, XPMath.pointsForLevel(32));
    }

    @Test
    void pointsForLevelMatchesVanillaAtHighLevels() {
        assertEquals(5345, XPMath.pointsForLevel(50));
        assertEquals(30970, XPMath.pointsForLevel(100));
        assertEquals(4_339_720, XPMath.pointsForLevel(1000));
    }

    @Test
    void pointsForLevelNeverGoesNegative() {
        assertEquals(0, XPMath.pointsForLevel(-5));
        assertEquals(Integer.MAX_VALUE, XPMath.pointsForLevel(100_000));
        assertTrue(XPMath.pointsForLevel(Integer.MAX_VALUE) > 0);
    }

    @Test
    void expToLevelMatchesVanillaAtFormulaBoundaries() {
        assertEquals(7, XPMath.expToLevel(0));
        assertEquals(37, XPMath.expToLevel(15));
        assertEquals(42, XPMath.expToLevel(16));
        assertEquals(112, XPMath.expToLevel(30));
        assertEquals(121, XPMath.expToLevel(31));
    }

    @Test
    void levelTableAndPerLevelCostAgree() {
        // The quadratic totals and the linear per-level cost are two views of the same curve
        for (int level = 0; level < 5000; level++) {
            assertEquals(XPMath.pointsForLevel(level + 1) - XPMath.pointsForLevel(level), XPMath.expToLevel(level),
                    "level " + level);
        }
    }

    @Test
    void totalPointsRoundTripsThroughLevelAndProgress() {
        for (int level = 0; level < 500; level++) {
            int bar = XPMath.expToLevel(level);

            // an empty bar is exactly the level threshold
            assertEquals(XPMath.pointsForLevel(level), XPMath.totalPoints(level, 0f, bar));

            // a full bar is exactly the next level threshold
            assertEquals(XPMath.pointsForLevel(level + 1), XPMath.totalPoints(level, 1f, bar));

            // every whole point inside the bar survives the float conversion
            for (int point = 0; point < bar; point++) {
                float progress = (float) point / bar;
                assertEquals(XPMath.pointsForLevel(level) + point, XPMath.totalPoints(level, progress, bar),
                        "level " + level + " point " + point);
            }
        }
    }

    @Test
    void totalPointsClampsInsteadOfOverflowing() {
        assertEquals(Integer.MAX_VALUE, XPMath.totalPoints(100_000, 0.5f, XPMath.expToLevel(100_000)));
    }
}
