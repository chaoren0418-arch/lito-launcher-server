package local.lito381.server;

import java.util.List;

/** Exact active-physical rows used by the selected-source map-4 population. */
final class ClassicWorldMonsterPhysicalSkillCatalog {
    static final String RETAINED_SQL_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";
    static final String RETAINED_MOB_SKILL_USE_SHA256 =
            "7167D0899729DBBA7A12E1EDCEC5D8DDE0570B94DA7539D06A1417C9465C90B7";
    static final String RETAINED_WORLD_SHA256 =
            "0592D0D5D13AD5502DE4EE2DA8ACF4F90E319DAFE3BA52DE3F526B348A618DBF";
    static final int MAP4_PHYSICAL_ROWS = 5;
    static final int MAP4_PHYSICAL_NPC_TYPES = 4;
    static final int MAP4_PHYSICAL_POPULATION = 232;

    /** One retained {@code mobskill} Type=1 row. */
    record Definition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int triggerCount, int range, int areaWidth, int areaHeight,
            int leverage, int gfxId, int actionId) {
        Definition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0
                    || triggerCount < 0 || range <= 0 || areaWidth < 0
                    || areaHeight < 0 || leverage < 0 || gfxId < 0
                    || actionId < 0) {
                throw new IllegalArgumentException("invalid classic mob physical row");
            }
        }

        int effectiveLeverage() {
            return leverage > 0 ? leverage : 10;
        }

        boolean triggerDistance(int distance) {
            if (distance < 0) throw new IllegalArgumentException("negative mob target distance");
            return triggerRange < 0
                    ? distance <= Math.abs(triggerRange)
                    : distance >= triggerRange;
        }

        boolean triggerRoll(int oneBasedRoll) {
            if (oneBasedRoll < 1 || oneBasedRoll > 100) {
                throw new IllegalArgumentException("mob skill roll outside percentile range");
            }
            // Retained L1MobSkillUse uses the strict comparison chance < TriRnd.
            return oneBasedRoll < triggerChance;
        }

        boolean hasArea() {
            return areaHeight > 0;
        }

        /** Retained World.getVisibleBoxObjects rotated-rectangle membership. */
        boolean containsBoxTarget(
                int attackerX, int attackerY, int heading,
                int targetX, int targetY) {
            if (!hasArea() || heading < 0 || heading > 7) return false;
            int distance = Math.max(Math.abs(targetX - attackerX),
                    Math.abs(targetY - attackerY));
            if (distance > areaHeight && distance > areaWidth) return false;
            int rotatedHeading = (heading + 6) & 7;
            double angle = rotatedHeading * Math.PI / 4.0;
            double cosine = Math.cos(angle);
            double sine = Math.sin(angle);
            int x = targetX - attackerX;
            int y = targetY - attackerY;
            long rotatedX = Math.round(x * cosine + y * sine);
            long rotatedY = Math.round(-x * sine + y * cosine);
            return rotatedX > 0 && distance <= areaHeight
                    && rotatedY >= -areaWidth && rotatedY <= areaWidth;
        }
    }

    private static final List<Definition> DEFINITIONS = List.of(
            // Ogre: forward box 1x2, double leverage, special action 30.
            new Definition(45278, 0, 30, -2, 0,
                    2, 1, 2, 20, 0, 30),
            new Definition(45284, 0, 100, 2, 0,
                    2, 0, 0, 15, 0, 30),
            new Definition(45291, 0, 100, -1, 0,
                    1, 0, 0, 15, 0, 30),
            // Fire Beast: act 0 is limited to two successful uses per life.
            new Definition(45341, 0, 100, 2, 2,
                    2, 0, 0, 0, 954, 3),
            new Definition(45341, 1, 100, -1, 0,
                    1, 0, 0, 0, 0, 30));

    private ClassicWorldMonsterPhysicalSkillCatalog() {}

    static List<Definition> definitions() {
        return DEFINITIONS;
    }

    static List<Definition> forNpc(int npcId) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.npcId() == npcId)
                .toList();
    }
}
