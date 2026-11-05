package local.lito381.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
