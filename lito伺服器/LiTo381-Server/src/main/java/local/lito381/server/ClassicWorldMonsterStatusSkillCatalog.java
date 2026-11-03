package local.lito381.server;

import java.util.List;

/** Retained map-2000 Type2 probability/status mobskill rows. */
final class ClassicWorldMonsterStatusSkillCatalog {
    static final String RETAINED_DATABASE_SHA256 =
            "3BF9B04B6E1D788E361534A79F3AA49CEEEF84FAE5CE04471EDF1EC9764CA423";
    static final String RETAINED_CURSE_PARALYSIS_SHA256 =
            "CD3D30B87611CFE082C4B75C26485604A5B6D99D14D55A1F7E721B6BB06A8999";
    static final String RETAINED_CURSE_MODE_SHA256 =
            "A90F2CC61A31680DD7843F7EA7152815D8039B14AF3F6A28B2D19D387F73E959";
    static final int CURSE_PARALYZE = 33;
    static final int CKEW_STATUS_ROWS = 1;
    static final int CKEW_STATUS_POPULATION = 23;

    record Definition(
            int npcId, int actNo, int triggerChance, int triggerRange,
            int leverage, int skillId, int mpCost, int reuseDelayMillis,
            int maximumRange, int actionId, int effectGfx,
            int actionDelayMillis, int warningDelayMillis,
            int paralysisDurationMillis, int failureMessage,
            int probabilityHigh, int probabilityEqual, int probabilityLow,
            double intelligenceMultiplier, double magicResistanceMultiplier,
            boolean includeMagicLevel, boolean includeMagicHit) {
        Definition {
            if (npcId <= 0 || actNo < 0 || triggerChance <= 0
                    || triggerChance > 100 || triggerRange == 0 || leverage < 0
                    || skillId <= 0 || mpCost < 0 || reuseDelayMillis < 0
                    || maximumRange <= 0 || actionId < 0 || actionId > 0xFF
                    || effectGfx <= 0 || effectGfx > 0xFFFF
                    || actionDelayMillis <= 0 || warningDelayMillis <= 0
                    || paralysisDurationMillis <= 0 || failureMessage <= 0
                    || failureMessage > 0xFFFF
                    || probabilityHigh < 0 || probabilityHigh > 100
                    || probabilityEqual < 0 || probabilityEqual > 100
                    || probabilityLow < 0 || probabilityLow > 100
                    || !Double.isFinite(intelligenceMultiplier)
                    || intelligenceMultiplier < 0.0D
                    || !Double.isFinite(magicResistanceMultiplier)
                    || magicResistanceMultiplier < 0.0D) {
                throw new IllegalArgumentException(
                        "invalid classic monster status skill row");
            }
        }

        boolean triggerDistance(int distance) {
            if (distance < 0) {
                throw new IllegalArgumentException("negative mob target distance");
            }
            return triggerRange < 0
                    ? distance <= Math.abs(triggerRange)
                    : distance >= triggerRange;
        }

        boolean triggerRoll(int oneBasedRoll) {
            if (oneBasedRoll < 1 || oneBasedRoll > 100) {
                throw new IllegalArgumentException(
                        "mob status roll outside percentile range");
            }
            return oneBasedRoll < triggerChance;
        }

        /** Retained negative-skill table followed by the native outer 90% cap. */
        int probability(
                int casterLevel, int casterIntelligence,
                int targetLevel, int targetMagicResistance,
                int targetStoneResistance) {
            if (casterLevel <= 0 || casterIntelligence < 0 || targetLevel <= 0
                    || targetMagicResistance < 0
                    || targetStoneResistance < -128
                    || targetStoneResistance > 127) {
                throw new IllegalArgumentException(
                        "invalid classic monster status probability inputs");
            }
            double result = casterLevel > targetLevel ? probabilityHigh
                    : casterLevel == targetLevel
                            ? probabilityEqual : probabilityLow;
            result += casterIntelligence * intelligenceMultiplier;
            if (includeMagicHit) {
                result += BattleTurnUndead.magicBonus(casterIntelligence);
            }
            if (includeMagicLevel) result += casterLevel >> 2;
            result -= targetMagicResistance * magicResistanceMultiplier;
            result -= targetStoneResistance;
            result = Math.max(1.0D, Math.min(100.0D, result));
            return Math.max(1, Math.min(90, (int) result));
        }
    }

    private static final List<Definition> DEFINITIONS = List.of(
            // mobskill87005/0 plus retained skill33 and NPC87005 clocks.
            new Definition(87005, 0, 20, -2, 1, CURSE_PARALYZE,
                    150, 5_000, 2, 19, 746, 1_120,
                    5_000, 10_000, 280,
                    50, 40, 30, 0.4D, 0.05D,
                    false, true));

    private ClassicWorldMonsterStatusSkillCatalog() {}

    static List<Definition> definitions() {
        return DEFINITIONS;
    }

    static List<Definition> forNpc(int npcId) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.npcId() == npcId)
                .toList();
    }
}
