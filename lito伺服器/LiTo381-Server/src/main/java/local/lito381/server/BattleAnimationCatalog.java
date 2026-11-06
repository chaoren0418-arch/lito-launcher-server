package local.lito381.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Server-side copy of the 3.80/3.81 spr_action table.
 *
 * <p>The client owns the actual frames in Sprite.idx/Sprite.pak.  This table
 * is the matching server contract: for each gfx it records the timed action
 * ids and their frame counts/rates.  Object spawn packets must select their
 * weapon stance from this contract instead of inventing a numeric byte.</p>
 */
final class BattleAnimationCatalog {
    static final String DEFAULT_RESOURCE = "/battle/spr-action-380.csv";

    private final String source;
    private final Map<Integer, Map<Integer, Animation>> sprites;
    private final int entryCount;

    private BattleAnimationCatalog(String source,
                                   Map<Integer, Map<Integer, Animation>> sprites,
                                   int entryCount) {
        this.source = source;
        this.sprites = sprites;
        this.entryCount = entryCount;
    }

    static BattleAnimationCatalog get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final BattleAnimationCatalog INSTANCE =
                load(SharedClientResources.animationResource(DEFAULT_RESOURCE));
    }

    static BattleAnimationCatalog load(String resource) {
        InputStream input = BattleAnimationCatalog.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalStateException("Required battle animation resource is missing: " + resource);
        }
        Map<Integer, Map<Integer, Animation>> mutable = new HashMap<>();
        int entries = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#") || value.startsWith("spr_id,")) continue;
                String[] fields = value.split(",", -1);
                if (fields.length != 4) {
                    throw new IllegalStateException(resource + ":" + lineNumber
                            + " expected four CSV fields, got " + fields.length);
                }
                Animation animation;
                try {
                    animation = new Animation(
                            Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
                            Math.max(0, Integer.parseInt(fields[2])),
                            Math.max(0, Integer.parseInt(fields[3])));
                } catch (NumberFormatException error) {
                    throw new IllegalStateException(resource + ":" + lineNumber
                            + " contains a non-numeric animation field", error);
                }
                Animation previous = mutable
                        .computeIfAbsent(animation.spriteId(), ignored -> new HashMap<>())
                        .put(animation.actionId(), animation);
                if (previous != null) {
                    throw new IllegalStateException(resource + ":" + lineNumber
                            + " duplicates gfx=" + animation.spriteId()
                            + " action=" + animation.actionId());
                }
                entries++;
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read battle animation resource: " + resource, error);
        }
        Map<Integer, Map<Integer, Animation>> immutable = new HashMap<>();
        mutable.forEach((sprite, actions) -> immutable.put(sprite, Map.copyOf(actions)));
        BattleAnimationCatalog catalog = new BattleAnimationCatalog(
                resource, Map.copyOf(immutable), entries);
        if (catalog.entryCount == 0) {
            throw new IllegalStateException("Battle animation resource is empty: " + resource);
        }
        return catalog;
    }

    int requireWalkAction(int spriteId, int actionId) {
        if (!BattleActionCodes.isWalkAction(actionId)) {
            throw new IllegalArgumentException("Action " + actionId
                    + " is not a weapon walk/stance action for gfx " + spriteId);
        }
        requireTimedAction(spriteId, actionId);
        return actionId;
    }

    /**
     * The 3.81 server's {@code L1PcInstance.getWeaponType} falls back from
     * chainsword stance 83 to spear stance 24 when the active sprite has no
     * chainsword action family.  Native Dragon Knight gfx 6658/6661 take
     * that branch in the selected spr_action resource.
     */
    int resolveEquippedWalkAction(int spriteId, int requestedActionId) {
        if (requestedActionId == BattleActionCodes.CHAINSWORD_WALK
                && !supportsTimedAction(spriteId, requestedActionId)) {
            return requireWalkAction(spriteId, BattleActionCodes.SPEAR_WALK);
        }
        return requireWalkAction(spriteId, requestedActionId);
    }

    int requireAttackAction(int spriteId, int walkAction) {
        requireWalkAction(spriteId, walkAction);
        int attackAction = BattleActionCodes.attackForWalk(walkAction);
        requireTimedAction(spriteId, attackAction);
        return attackAction;
    }

    Animation requireTimedAction(int spriteId, int actionId) {
        Animation animation = sprites.getOrDefault(spriteId, Map.of()).get(actionId);
        if (animation == null) {
            throw new IllegalArgumentException("Battle animation is unavailable: gfx="
                    + spriteId + " action=" + actionId + " source=" + source);
        }
        return animation;
    }

    boolean supportsTimedAction(int spriteId, int actionId) {
        return sprites.getOrDefault(spriteId, Map.of()).containsKey(actionId);
    }

    void requireBattleCapableSprite(int spriteId, int walkAction) {
        requireWalkAction(spriteId, walkAction);
        requireAttackAction(spriteId, walkAction);
    }

    String source() { return source; }
    int spriteCount() { return sprites.size(); }
    int entryCount() { return entryCount; }

    record Animation(int spriteId, int actionId, int frameCount, int frameRate) {
        long durationMillis() {
            if (frameCount <= 0 || frameRate <= 0) {
                long zero = 0L;
                return SharedClientResources.duration(spriteId, actionId, zero);
            }
            long computed = Math.round(frameCount * 40.0 * (24.0 / frameRate));
            return SharedClientResources.duration(spriteId, actionId, computed);
        }
    }
}

final class BattleActionCodes {
    static final int WALK = 0;
    static final int ATTACK = 1;
    static final int DAMAGE = 2;
    static final int IDLE = 3;
    /** Retained NPC ACTION_Appear; shares numeric id with sword walking. */
    static final int APPEAR = 4;
    static final int SWORD_WALK = 4;
    static final int SWORD_ATTACK = 5;
    static final int AXE_WALK = 11;
    /** Retained NPC ACTION_Hide; shares numeric id with axe walking. */
    static final int HIDE = 11;
    static final int AXE_ATTACK = 12;
    static final int DIE = 8;
    static final int PICKUP = 15;
    static final int WAND = 17;
    static final int SPELL_CAST = 19;
    static final int BOW_WALK = 20;
    static final int BOW_ATTACK = 21;
    static final int SPEAR_WALK = 24;
    static final int SPEAR_ATTACK = 25;
    static final int STAFF_WALK = 40;
    static final int STAFF_ATTACK = 41;
    /** Retained flying-monster ACTION_Moveup. */
    static final int MOVE_UP = 44;
    /** Retained flying-monster ACTION_Movedown. */
    static final int MOVE_DOWN = 45;
    static final int DAGGER_WALK = 46;
    static final int DAGGER_ATTACK = 47;
    static final int TWO_HAND_SWORD_WALK = 50;
    static final int TWO_HAND_SWORD_ATTACK = 51;
    static final int EDORYU_WALK = 54;
    static final int EDORYU_ATTACK = 55;
    static final int CLAW_WALK = 58;
    static final int CLAW_ATTACK = 59;
    static final int THROWING_KNIFE_WALK = 62;
    static final int THROWING_KNIFE_ATTACK = 63;
    static final int CHAINSWORD_WALK = 83;
    static final int CHAINSWORD_ATTACK = 84;
    static final int DOOR_CRACK_1 = 32;
    static final int DOOR_CRACK_2 = 33;
    static final int DOOR_CRACK_3 = 34;
    static final int DOOR_CRACK_4 = 35;
    static final int DOOR_CRACK_5 = 36;
    static final int DOOR_DIE = 37;

    private static final Set<Integer> WALK_ACTIONS = Set.of(
            WALK, SWORD_WALK, AXE_WALK, BOW_WALK, SPEAR_WALK, STAFF_WALK,
            DAGGER_WALK, TWO_HAND_SWORD_WALK, EDORYU_WALK, CLAW_WALK,
            THROWING_KNIFE_WALK, CHAINSWORD_WALK);

    private BattleActionCodes() {}

    static boolean isWalkAction(int actionId) {
        return WALK_ACTIONS.contains(actionId);
    }

    static int attackForWalk(int walkAction) {
        return switch (walkAction) {
            case WALK -> ATTACK;
            case SWORD_WALK -> SWORD_ATTACK;
            case AXE_WALK -> AXE_ATTACK;
            case BOW_WALK -> BOW_ATTACK;
            case SPEAR_WALK -> SPEAR_ATTACK;
            case STAFF_WALK -> STAFF_ATTACK;
            case DAGGER_WALK -> DAGGER_ATTACK;
            case TWO_HAND_SWORD_WALK -> TWO_HAND_SWORD_ATTACK;
            case EDORYU_WALK -> EDORYU_ATTACK;
            case CLAW_WALK -> CLAW_ATTACK;
            case THROWING_KNIFE_WALK -> THROWING_KNIFE_ATTACK;
            case CHAINSWORD_WALK -> CHAINSWORD_ATTACK;
            default -> throw new IllegalArgumentException(
                    "No attack action is mapped for walk action " + walkAction);
        };
    }

    static boolean isUntimedCoreAction(int actionId) {
        return actionId == DAMAGE || actionId == IDLE || actionId == DIE;
    }
}
