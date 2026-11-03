package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact retained database contract for the seven data-driven Dragon Eye buffs. */
final class BattleDragonEyeCatalog {
    static final long EARTH_DRAGON_EYE = 42_518L;
    static final long WATER_DRAGON_EYE = 42_519L;
    static final long WIND_DRAGON_EYE = 42_520L;
    static final long FIRE_DRAGON_EYE = 42_521L;
    static final long BIRTH_DRAGON_EYE = 42_522L;
    static final long IMAGE_DRAGON_EYE = 42_523L;
    static final long LIFE_DRAGON_EYE = 42_524L;

    static final int EARTH_DRAGON_EYE_BUFF = 6_684;
    static final int WATER_DRAGON_EYE_BUFF = 6_685;
    static final int WIND_DRAGON_EYE_BUFF = 6_686;
    static final int FIRE_DRAGON_EYE_BUFF = 6_683;
    static final int BIRTH_DRAGON_EYE_BUFF = 6_688;
    static final int IMAGE_DRAGON_EYE_BUFF = 6_689;
    static final int LIFE_DRAGON_EYE_BUFF = 6_687;

    private static final Ability NONE = new Ability(0, 0, 0, 0, 0, 0, 0, 0);
    private static final List<Eye> ORDERED_EYES = List.of(
            eye(EARTH_DRAGON_EYE, "$7885", 3_642, EARTH_DRAGON_EYE_BUFF,
                    7_671, 46, new Ability(0, 0, 0, 10, 3, 0, 0, 0),
                    "地龍之魔眼效果消失了。"),
            eye(WATER_DRAGON_EYE, "$7886", 3_641, WATER_DRAGON_EYE_BUFF,
                    7_671, 47, new Ability(0, 0, 3, 0, 0, 0, 3, 0),
                    "水龍之魔眼效果消失了。"),
            eye(WIND_DRAGON_EYE, "$7887", 3_643, WIND_DRAGON_EYE_BUFF,
                    7_673, 48, new Ability(0, 3, 0, 0, 0, 3, 0, 0),
                    "風龍之魔眼效果消失了。"),
            eye(FIRE_DRAGON_EYE, "$7888", 3_644, FIRE_DRAGON_EYE_BUFF,
                    7_674, 49, new Ability(2, 0, 0, 0, 0, 0, 0, 0),
                    "火龍之魔眼效果消失了。"),
            eye(BIRTH_DRAGON_EYE, "$7889", 3_663, BIRTH_DRAGON_EYE_BUFF,
                    7_675, 50, new Ability(0, 0, 50, 10, 0, 0, 0, 0),
                    "誕生之魔眼效果消失了。"),
            eye(IMAGE_DRAGON_EYE, "$7890", 3_666, IMAGE_DRAGON_EYE_BUFF,
                    7_676, 51, new Ability(0, 1, 50, 10, 0, 0, 0, 0),
                    "形象之魔眼效果消失了。"),
            eye(LIFE_DRAGON_EYE, "$7891", 3_645, LIFE_DRAGON_EYE_BUFF,
                    7_678, 52, new Ability(2, 1, 50, 10, 0, 0, 0, 1),
                    "生命之魔眼效果消失了。"));
    private static final Map<Long, Eye> EYES = index(ORDERED_EYES);

    private BattleDragonEyeCatalog() {}

    static List<Eye> all() {
        return ORDERED_EYES;
    }

    static Eye find(long itemId) {
        return EYES.get(itemId);
    }

    static Eye require(long itemId) {
        Eye eye = find(itemId);
        if (eye == null) throw new IllegalArgumentException("unknown Dragon Eye item " + itemId);
        return eye;
    }

    private static Eye eye(long itemId, String nameId, int inventoryGfx, int buffId,
                           int castGfx, int statusIcon, Ability ability,
                           String endMessage) {
        return new Eye(itemId, nameId, 0, inventoryGfx, 3_963,
                buffId, 600, castGfx, statusIcon,
                8, 600, 60_000, "DRAGON_EYE", false,
                ability == null ? NONE : ability, endMessage);
    }

    private static Map<Long, Eye> index(List<Eye> eyes) {
        Map<Long, Eye> indexed = new LinkedHashMap<>();
        for (Eye eye : eyes) {
            if (indexed.put(eye.itemId(), eye) != null) {
                throw new IllegalArgumentException("duplicate Dragon Eye item " + eye.itemId());
            }
        }
        return Map.copyOf(indexed);
    }

    record Ability(
            int meleeDamage, int magicHit, int damageReduction, int evasionRate,
            int stoneResistance, int sleepResistance, int freezeResistance,
            int poisonResistance) {
        Ability {
            if (meleeDamage < 0 || magicHit < 0 || damageReduction < 0
                    || evasionRate < 0 || stoneResistance < 0 || sleepResistance < 0
                    || freezeResistance < 0 || poisonResistance < 0) {
                throw new IllegalArgumentException("invalid Dragon Eye ability");
            }
        }

        boolean any() {
            return meleeDamage != 0 || magicHit != 0 || damageReduction != 0
                    || evasionRate != 0 || stoneResistance != 0
                    || sleepResistance != 0 || freezeResistance != 0
                    || poisonResistance != 0;
        }
    }

    record Eye(
            long itemId, String nameId, int useType, int inventoryGfx, int groundGfx,
            int buffId, int durationSeconds, int castGfx, int statusIcon,
            int sharedDelayId, int sharedDelayMillis, int itemDelayMillis,
            String group, boolean canOverwrite,
            Ability ability, String endMessage) {
        Eye {
            if (itemId <= 0 || itemId > 0xFFFF_FFFFL
                    || nameId == null || nameId.isBlank()
                    || useType < 0 || useType > 0xFF
                    || inventoryGfx < 0 || inventoryGfx > 0xFFFF
                    || groundGfx < 0 || groundGfx > 0xFFFF
                    || buffId <= 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                    || castGfx <= 0 || castGfx > 0xFFFF
                    || statusIcon < 0 || statusIcon > 0xFFFF
                    || sharedDelayId <= 0 || sharedDelayMillis <= 0
                    || itemDelayMillis <= 0 || group == null || group.isBlank()
                    || ability == null || !ability.any()
                    || endMessage == null || endMessage.isBlank()) {
                throw new IllegalArgumentException("invalid Dragon Eye source metadata");
            }
        }

        int meleeDamage() { return ability.meleeDamage(); }
        int magicHit() { return ability.magicHit(); }
        int damageReduction() { return ability.damageReduction(); }
        int evasionRate() { return ability.evasionRate(); }
        int stoneResistance() { return ability.stoneResistance(); }
        int sleepResistance() { return ability.sleepResistance(); }
        int freezeResistance() { return ability.freezeResistance(); }
        int poisonResistance() { return ability.poisonResistance(); }
    }
}
