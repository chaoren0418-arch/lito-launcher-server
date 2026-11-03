package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Authoritative Honor Match guardian-tower world object.
 *
 * <p>The native client owns the four tower sprites in the ordinary
 * {@code Sprite.idx/Sprite.pak}, not in {@code LiTo/LTsprite}.  Same-family
 * 3.80 server data corroborates gfx 2665/2667/2669/2671, 3000 HP and the
 * dedicated crack/death action sequence 32/33/34/35.  The exact LiTo client
 * consumes those states through the ordinary S_PUT_OBJECT, S_HIT_RATIO and
 * S_ACTION world-object receivers; there is no tower-specific wire packet.</p>
 *
 * <p>Coordinates are server-owned and therefore cannot be recovered from the
 * client executable.  Until an official LiTo server capture supplies them,
 * {@link BattleGuardianTowers} selects two distinct, S32-probed safe anchors
 * and exposes that placement source explicitly instead of claiming them as
 * native coordinates.</p>
 */
final class BattleGuardianTower {
    static final int MAX_HP = 3_000;
    static final int ACTION_HEALTHY = 0;
    static final int ACTION_CRACK_1 = 32;
    static final int ACTION_CRACK_2 = 33;
    static final int ACTION_CRACK_3 = 34;
    static final int ACTION_DIE = 35;

    static final int GFX_FIRE = 2665;
    static final int GFX_EARTH = 2667;
    static final int GFX_WATER = 2669;
    static final int GFX_WIND = 2671;

    static final String NAME_FIRE = "$2205";
    static final String NAME_WATER = "$2204";

    /**
     * Evidence role of a tower-shaped actor.  This is server metadata only;
     * every role still uses the native common world-object packets.
     *
     * <p>The selected client retains both the 2003 Hellgate/Terrible Quest
     * family, whose rules require one symbol after every room, and later
     * Battle-GP maps that advance directly after the first three armies and
     * expose one terminal Guardian Tower.  The role is therefore map-specific
     * even though every objective uses the same recovered native world-object
     * packets.</p>
     */
    enum ObjectiveKind {
        COMMON_GUARDIAN_TOWER("guardian-tower"),
        QUEST_ROOM_SYMBOL("quest-room-symbol"),
        QUEST_FINAL_SYMBOL("quest-final-symbol"),
        QUEST_FINAL_GUARDIAN_TOWER("quest-final-guardian-tower");

        private final String wireLabel;

        ObjectiveKind(String wireLabel) {
            this.wireLabel = wireLabel;
        }

        String wireLabel() {
            return wireLabel;
        }
    }

    // 0x63 is already the server-owned repair-NPC namespace.  Keep the
    // objective actors in their own range so a room id can never alias a
    // guardian tower and make the native client's object table overwrite one
    // of the two entries.
    private static final long FIRST_OBJECT_ID = 0x6B00_0000L;
    private static final AtomicLong NEXT_OBJECT_ID = new AtomicLong(FIRST_OBJECT_ID);

    record Snapshot(long objectId, int force, int x, int y, int gfx, String name,
                    int hp, int maxHp, int action, boolean destroyed,
                    String placementSource, ObjectiveKind objectiveKind) {}

    record Damage(Snapshot before, Snapshot after, boolean actionChanged,
                  boolean destroyedNow) {}

    record Healing(Snapshot before, Snapshot after, int restoredHp,
                   boolean actionChanged) {}

    private final long objectId = NEXT_OBJECT_ID.getAndUpdate(current ->
            current >= 0x6BFF_FFFFL ? FIRST_OBJECT_ID : current + 1);
    private int force;
    private int x;
    private int y;
    private int gfx;
    private String name = "";
    private String placementSource = "";
    private ObjectiveKind objectiveKind = ObjectiveKind.COMMON_GUARDIAN_TOWER;
    private int maximumHp = MAX_HP;
    private int hp;
    private int action;
    private boolean active;
    private final BattleJoyOfPainStatus joyOfPainStatus =
            new BattleJoyOfPainStatus();

    synchronized void reset(int force, ArenaMapCatalog.Arena arena,
                            ArenaMapCatalog.Point point, int gfx, String name,
                            String placementSource) {
        reset(force, arena, point, gfx, name, placementSource,
                ObjectiveKind.COMMON_GUARDIAN_TOWER);
    }

    synchronized void reset(int force, ArenaMapCatalog.Arena arena,
                            ArenaMapCatalog.Point point, int gfx, String name,
                            String placementSource, ObjectiveKind objectiveKind) {
        reset(force, arena, point, gfx, name, placementSource,
                objectiveKind, MAX_HP);
    }

    synchronized void reset(int force, ArenaMapCatalog.Arena arena,
                            ArenaMapCatalog.Point point, int gfx, String name,
                            String placementSource, ObjectiveKind objectiveKind,
                            int maximumHp) {
        if (force < 0 || force > 1 || arena == null || point == null
                || !arena.contains(point.x(), point.y())
                || !supportedGfx(gfx) || name == null || name.isBlank()
                || placementSource == null || placementSource.isBlank()
                || objectiveKind == null || maximumHp < 1
                || maximumHp > 10_000_000) {
            throw new IllegalArgumentException("invalid guardian-tower reset");
        }
        this.force = force;
        x = point.x();
        y = point.y();
        this.gfx = gfx;
        this.name = name;
        this.placementSource = placementSource;
        this.objectiveKind = objectiveKind;
        this.maximumHp = maximumHp;
        hp = maximumHp;
        action = ACTION_HEALTHY;
        active = true;
        joyOfPainStatus.clear();
    }

    synchronized void deactivate() {
        active = false;
        hp = 0;
        action = ACTION_HEALTHY;
        joyOfPainStatus.clear();
    }

    synchronized Snapshot snapshot() {
        return active ? snapshotUnsafe() : null;
    }

    synchronized Damage damage(int amount) {
        if (!active || hp <= 0 || amount <= 0) return null;
        Snapshot before = snapshotUnsafe();
        hp = Math.max(0, hp - amount);
        int nextAction = actionForHp(hp, maximumHp);
        boolean changed = nextAction != action;
        action = nextAction;
        if (hp == 0) joyOfPainStatus.clear();
        Snapshot after = snapshotUnsafe();
        return new Damage(before, after, changed,
                before.hp() > 0 && after.hp() == 0);
    }

    synchronized Healing heal(int amount) {
        if (!active || hp <= 0 || amount <= 0) return null;
        Snapshot before = snapshotUnsafe();
        hp = Math.min(maximumHp, Math.addExact(hp, amount));
        int nextAction = actionForHp(hp, maximumHp);
        boolean changed = nextAction != action;
        action = nextAction;
        Snapshot after = snapshotUnsafe();
        return new Healing(before, after, after.hp() - before.hp(), changed);
    }

    synchronized boolean blocks(int targetX, int targetY) {
        return active && hp > 0 && targetX == x && targetY == y;
    }

    synchronized BattleJoyOfPainStatus.Activation activateJoyOfPain(
            long nowNanos, int durationSeconds) {
        return !active || hp <= 0 ? null
                : joyOfPainStatus.activateOrRefresh(nowNanos, durationSeconds);
    }

    private Snapshot snapshotUnsafe() {
        return new Snapshot(objectId, force, x, y, gfx, name, hp, maximumHp,
                action, hp == 0, placementSource, objectiveKind);
    }

    private static int actionForHp(int hp, int maximumHp) {
        if (hp <= 0) return ACTION_DIE;
        if (hp < maximumHp / 4) return ACTION_CRACK_3;
        if (hp < maximumHp / 2) return ACTION_CRACK_2;
        if (hp < maximumHp * 3 / 4) return ACTION_CRACK_1;
        return ACTION_HEALTHY;
    }

    private static boolean supportedGfx(int gfx) {
        return gfx == GFX_FIRE || gfx == GFX_EARTH
                || gfx == GFX_WATER || gfx == GFX_WIND;
    }

    static boolean isStateAction(int action) {
        return action == ACTION_HEALTHY || action == ACTION_CRACK_1
                || action == ACTION_CRACK_2 || action == ACTION_CRACK_3
                || action == ACTION_DIE;
    }
}

/** Force-owned objectives plus the cooperative Time Attack start tower. */
final class BattleGuardianTowers {
    private static final String DATABASE_PLACEMENT_SOURCE =
            "lito_static.lito_battle_object";
    static final String HONOR_RELIC_HEALING_RULE_SOURCE =
            "LineageCompendium-LiTo-Honor-relic-has-HP-and-can-be-healed";
    static final String GEOMETRY_DERIVED_PLACEMENT =
            "LTmap-S32-safe-anchor-provisional";
    static final String TIME_ATTACK_GATE_SOURCE =
            "GameMeca-LWC-2005-participant-start-tower+"
                    + "LTmap-540-feature-anchor-provisional";
    static final String SIEGE_RULE_SOURCE =
            "LiTo-2005-20min-random-sides-two-outer-towers-inner-gate-inner-tower";
    static final String SIEGE_PLACEMENT_SOURCE =
            "LTmap-S32-safe-anchor-provisional-not-Layer8-decoration";
    static final String SIEGE_GFX_SOURCE =
            "same-family-common-guardian-tower-set-provisional-role-assignment";
    static final String QUEST_ROOM_ADVANCE_RULE_SOURCE =
            BattleQuestEncounter.ROOM_ADVANCE_RULE_SOURCE;
    static final String QUEST_FINAL_TOWER_PRESENTATION_SOURCE =
            BattleQuestEncounter.FINAL_TOWER_PRESENTATION_SOURCE;

    enum SiegePhase { INACTIVE, OUTER_TOWERS, INNER_TOWER, COMPLETE }

    record DamageResult(BattleGuardianTower.Damage damage,
                        boolean siegeGateOpened,
                        boolean siegeStrongholdDestroyed) {}

    record Hit(BattleGuardianTower.Damage damage,
               BattleRules.Resolution resolution,
               List<BattleTimeAttackEncounter.Snapshot> spawnedTimeAttackActors,
               BattleDoor.Snapshot openedSiegeGate,
               List<BattleQuestEncounter.Snapshot> spawnedQuestActors,
               int completedQuestRoom,
               String timeAttackStartRole) {
        Hit {
            spawnedTimeAttackActors = List.copyOf(spawnedTimeAttackActors);
            spawnedQuestActors = List.copyOf(spawnedQuestActors);
            if (completedQuestRoom < 0 || completedQuestRoom > 4) {
                throw new IllegalArgumentException("invalid completed Quest room");
            }
            if (timeAttackStartRole == null
                    || !(timeAttackStartRole.isEmpty()
                        || timeAttackStartRole.equals("leader")
                        || timeAttackStartRole.equals("member"))) {
                throw new IllegalArgumentException("invalid Time Attack start role");
            }
        }
    }

    private final BattleGuardianTower forceZero = new BattleGuardianTower();
    private final BattleGuardianTower forceOne = new BattleGuardianTower();
    private final BattleGuardianTower siegeOuterLeft = new BattleGuardianTower();
    private final BattleGuardianTower siegeOuterRight = new BattleGuardianTower();
    private final BattleGuardianTower siegeInner = new BattleGuardianTower();
    private final BattleGuardianTower[] questFinalTowers = {
            new BattleGuardianTower(), new BattleGuardianTower()
    };
    private BattleRules.Mode mode;
    private ArenaMapCatalog.Arena arena;
    private BattleControlCatalog controls = BattleControlCatalog.defaults();
    private int siegeAttackerForce = -1;
    private SiegePhase siegePhase = SiegePhase.INACTIVE;

    synchronized void reset(BattleRules.RuleSet rules, ArenaMapCatalog.Arena arena) {
        reset(rules, arena, -1);
    }

    synchronized void reset(BattleRules.RuleSet rules, ArenaMapCatalog.Arena arena,
                            int requestedSiegeAttackerForce) {
        reset(rules, arena, requestedSiegeAttackerForce,
                BattleControlCatalog.defaults());
    }

    synchronized void reset(BattleRules.RuleSet rules, ArenaMapCatalog.Arena arena,
                            int requestedSiegeAttackerForce,
                            BattleControlCatalog controls) {
        deactivate();
        if (rules == null || arena == null
                || (rules.mode() != BattleRules.Mode.HONOR_MATCH
                    && rules.mode() != BattleRules.Mode.QUEST_BATTLE
                    && rules.mode() != BattleRules.Mode.TIME_ATTACK
                    && rules.mode() != BattleRules.Mode.SIEGE_WAR)) return;
        mode = rules.mode();
        this.arena = arena;
        this.controls = controls == null
                ? BattleControlCatalog.defaults() : controls;
        // Quest objectives are revealed by the encounter after the applicable
        // map-specific monster-clear gate has been satisfied.
        if (mode == BattleRules.Mode.QUEST_BATTLE) return;

        // Contemporary Korean play instructions identify one guardian tower
        // that starts Time Attack. They explicitly warn that another participant
        // can destroy it, although this often suppresses the later dragon. The
        // client and retained database do not expose its exact element, HP or coordinate;
        // use the proven common tower actor at the map's S32 feature anchor and
        // keep that provisional boundary in every snapshot.
        if (mode == BattleRules.Mode.TIME_ATTACK) {
            resetControlled(forceZero, 0, "time-start", arena.featurePoint(),
                    BattleGuardianTower.GFX_FIRE,
                    BattleGuardianTower.NAME_FIRE,
                    TIME_ATTACK_GATE_SOURCE);
            return;
        }

        if (mode == BattleRules.Mode.SIEGE_WAR) {
            siegeAttackerForce = NativeLitoSiegePolicy.resolveAttackerForce(
                    requestedSiegeAttackerForce,
                    () -> ThreadLocalRandom.current().nextInt(2));
            int defenderForce = 1 - siegeAttackerForce;
            siegePhase = SiegePhase.OUTER_TOWERS;

            // The executable/resource set does not retain original server
            // object coordinates.  Use three distinct S32-probed anchors and
            // retain the provisional source on every actor.  Layer8 positions
            // are decorative effects (flags, circles, fire and castle art),
            // not authoritative attackable-object placements.
            resetControlled(siegeOuterLeft, defenderForce, "siege-outer-left", arena.spawn(4),
                    BattleGuardianTower.GFX_FIRE,
                    BattleGuardianTower.NAME_FIRE,
                    SIEGE_PLACEMENT_SOURCE);
            resetControlled(siegeOuterRight, defenderForce, "siege-outer-right", arena.spawn(5),
                    BattleGuardianTower.GFX_WATER,
                    BattleGuardianTower.NAME_WATER,
                    SIEGE_PLACEMENT_SOURCE);
            resetControlled(siegeInner, defenderForce, "siege-inner", arena.spawn(6),
                    BattleGuardianTower.GFX_FIRE,
                    BattleGuardianTower.NAME_FIRE,
                    SIEGE_PLACEMENT_SOURCE);
            return;
        }

        // Anchors 4/5 are independently S32-probed 3x3 safe interiors.  They
        // keep the objective off the first player start points while exact
        // official server coordinates remain unavailable from client-only
        // evidence.  This source is logged and serialized in each snapshot.
        ArenaMapCatalog.Point red = adjacentSafe(arena, arena.spawn(4), 1);
        ArenaMapCatalog.Point blue = adjacentSafe(arena, arena.spawn(5), -1);
        if (red.equals(blue)) {
            throw new IllegalStateException("guardian towers share a placement: "
                    + arena.mapId());
        }
        resetControlled(forceZero, 0, "honor-red", red, BattleGuardianTower.GFX_FIRE,
                BattleGuardianTower.NAME_FIRE, GEOMETRY_DERIVED_PLACEMENT);
        resetControlled(forceOne, 1, "honor-blue", blue, BattleGuardianTower.GFX_WATER,
                BattleGuardianTower.NAME_WATER, GEOMETRY_DERIVED_PLACEMENT);
    }

    synchronized void deactivate() {
        forceZero.deactivate();
        forceOne.deactivate();
        siegeOuterLeft.deactivate();
        siegeOuterRight.deactivate();
        siegeInner.deactivate();
        for (BattleGuardianTower tower : questFinalTowers) tower.deactivate();
        mode = null;
        arena = null;
        controls = BattleControlCatalog.defaults();
        siegeAttackerForce = -1;
        siegePhase = SiegePhase.INACTIVE;
    }

    synchronized BattleGuardianTower.Snapshot revealQuestObjective(
            int force, ArenaMapCatalog.Point point,
            BattleGuardianTower.ObjectiveKind objectiveKind,
            String placementSource) {
        if (mode != BattleRules.Mode.QUEST_BATTLE || arena == null
                || force < 0 || force > 1 || point == null
                || objectiveKind == null
                || (objectiveKind != BattleGuardianTower.ObjectiveKind.QUEST_ROOM_SYMBOL
                    && objectiveKind
                        != BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL
                    && objectiveKind
                        != BattleGuardianTower.ObjectiveKind
                                .QUEST_FINAL_GUARDIAN_TOWER)
                || placementSource == null || placementSource.isBlank()) return null;
        BattleGuardianTower target = questFinalTowers[force];
        BattleGuardianTower.Snapshot existing = target.snapshot();
        if (existing != null) return existing;
        BattleControlCatalog.ObjectPlacement configured = controls.object(
                mode, arena.mapId(), "quest-force-" + force);
        ArenaMapCatalog.Point selectedPoint = configured == null ? point
                : new ArenaMapCatalog.Point(configured.x(), configured.y());
        int selectedGfx = configured == null
                ? (force == 0 ? BattleGuardianTower.GFX_FIRE
                        : BattleGuardianTower.GFX_WATER)
                : configured.gfx();
        int selectedMaximumHp = configured == null
                ? BattleGuardianTower.MAX_HP : configured.maximumHp();
        target.reset(force, arena, selectedPoint,
                selectedGfx,
                force == 0 ? BattleGuardianTower.NAME_FIRE
                        : BattleGuardianTower.NAME_WATER,
                configured == null ? placementSource : DATABASE_PLACEMENT_SOURCE,
                objectiveKind, selectedMaximumHp);
        return target.snapshot();
    }

    private void resetControlled(BattleGuardianTower tower, int force, String role,
                                 ArenaMapCatalog.Point fallback, int fallbackGfx,
                                 String name, String fallbackSource) {
        BattleControlCatalog.ObjectPlacement configured =
                controls.object(mode, arena.mapId(), role);
        ArenaMapCatalog.Point point = configured == null ? fallback
                : new ArenaMapCatalog.Point(configured.x(), configured.y());
        tower.reset(force, arena, point,
                configured == null ? fallbackGfx : configured.gfx(), name,
                configured == null ? fallbackSource : DATABASE_PLACEMENT_SOURCE,
                BattleGuardianTower.ObjectiveKind.COMMON_GUARDIAN_TOWER,
                configured == null ? BattleGuardianTower.MAX_HP
                        : configured.maximumHp());
    }

    synchronized BattleGuardianTower.Snapshot revealQuestFinalTower(
            int force, ArenaMapCatalog.Point point) {
        return revealQuestObjective(force, point,
                BattleGuardianTower.ObjectiveKind.QUEST_FINAL_GUARDIAN_TOWER,
                QUEST_FINAL_TOWER_PRESENTATION_SOURCE);
    }

    synchronized boolean isQuestObjective(long objectId) {
        if (mode != BattleRules.Mode.QUEST_BATTLE) return false;
        for (BattleGuardianTower tower : questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = tower.snapshot();
            if (snapshot != null && snapshot.objectId() == objectId) return true;
        }
        return false;
    }

    synchronized boolean isQuestFinalTower(long objectId) {
        BattleGuardianTower.Snapshot objective = find(objectId);
        return objective != null
                && (objective.objectiveKind()
                        == BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL
                    || objective.objectiveKind()
                        == BattleGuardianTower.ObjectiveKind
                                .QUEST_FINAL_GUARDIAN_TOWER);
    }

    synchronized boolean deactivateQuestObjective(long objectId) {
        if (mode != BattleRules.Mode.QUEST_BATTLE) return false;
        for (BattleGuardianTower tower : questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = tower.snapshot();
            if (snapshot != null && snapshot.objectId() == objectId) {
                tower.deactivate();
                return true;
            }
        }
        return false;
    }

    synchronized boolean deactivateQuestFinalTower(long objectId) {
        return isQuestFinalTower(objectId) && deactivateQuestObjective(objectId);
    }

    synchronized List<BattleGuardianTower.Snapshot> snapshots() {
        List<BattleGuardianTower.Snapshot> result = new ArrayList<>(2);
        BattleGuardianTower.Snapshot zero = forceZero.snapshot();
        BattleGuardianTower.Snapshot one = forceOne.snapshot();
        if (zero != null) result.add(zero);
        if (one != null) result.add(one);
        BattleGuardianTower.Snapshot outerLeft = siegeOuterLeft.snapshot();
        BattleGuardianTower.Snapshot outerRight = siegeOuterRight.snapshot();
        BattleGuardianTower.Snapshot inner = siegeInner.snapshot();
        if (outerLeft != null) result.add(outerLeft);
        if (outerRight != null) result.add(outerRight);
        if (inner != null) result.add(inner);
        for (BattleGuardianTower tower : questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = tower.snapshot();
            if (snapshot != null) result.add(snapshot);
        }
        return List.copyOf(result);
    }

    synchronized BattleGuardianTower.Snapshot find(long objectId) {
        for (BattleGuardianTower.Snapshot tower : snapshots()) {
            if (tower.objectId() == objectId) return tower;
        }
        return null;
    }

    synchronized boolean canAttack(long objectId, int attackerForce) {
        BattleGuardianTower.Snapshot tower = find(objectId);
        if (tower == null || tower.destroyed()
                || attackerForce < 0 || attackerForce > 1) return false;
        if (mode == BattleRules.Mode.SIEGE_WAR) {
            if (attackerForce != siegeAttackerForce) return false;
            boolean outer = isSiegeOuter(objectId);
            boolean inner = isSiegeInner(objectId);
            return (siegePhase == SiegePhase.OUTER_TOWERS && outer)
                    || (siegePhase == SiegePhase.INNER_TOWER && inner);
        }
        return (mode == BattleRules.Mode.HONOR_MATCH
                        && tower.force() != attackerForce)
                    || (mode == BattleRules.Mode.QUEST_BATTLE
                        && tower.force() == attackerForce)
                    || mode == BattleRules.Mode.TIME_ATTACK;
    }

    /**
     * The retained LiTo Honor description explicitly gives each team a relic
     * with HP that can be healed.  Keep that rule narrow: it authorizes a
     * living team's own Honor relic, not Quest symbols, the Time Attack start
     * tower, or Siege objectives whose healing semantics are not sourced.
     */
    synchronized boolean canHeal(long objectId, int healerForce) {
        BattleGuardianTower.Snapshot tower = find(objectId);
        return mode == BattleRules.Mode.HONOR_MATCH
                && tower != null && !tower.destroyed()
                && healerForce >= 0 && healerForce <= 1
                && tower.force() == healerForce;
    }

    synchronized BattleGuardianTower.Healing heal(
            long objectId, int healerForce, int amount) {
        if (amount <= 0 || !canHeal(objectId, healerForce)) return null;
        BattleGuardianTower target = target(objectId);
        return target == null ? null : target.heal(amount);
    }

    synchronized DamageResult damage(long objectId, int attackerForce,
                                     int amount) {
        if (!canAttack(objectId, attackerForce)) return null;
        BattleGuardianTower target = target(objectId);
        if (target == null) return null;
        BattleGuardianTower.Damage damage = target.damage(amount);
        if (damage == null || mode != BattleRules.Mode.SIEGE_WAR) {
            return damage == null ? null : new DamageResult(damage, false, false);
        }

        boolean gateOpened = false;
        boolean strongholdDestroyed = false;
        if (damage.destroyedNow() && siegePhase == SiegePhase.OUTER_TOWERS
                && siegeOuterLeft.snapshot().destroyed()
                && siegeOuterRight.snapshot().destroyed()) {
            siegePhase = SiegePhase.INNER_TOWER;
            gateOpened = true;
        } else if (damage.destroyedNow()
                && siegePhase == SiegePhase.INNER_TOWER
                && isSiegeInner(objectId)) {
            siegePhase = SiegePhase.COMPLETE;
            strongholdDestroyed = true;
        }
        return new DamageResult(damage, gateOpened, strongholdDestroyed);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPain(
            long objectId, int casterForce, long nowNanos,
            int durationSeconds) {
        if (!canAttack(objectId, casterForce)) return null;
        BattleGuardianTower target = target(objectId);
        return target == null ? null
                : target.activateJoyOfPain(nowNanos, durationSeconds);
    }

    synchronized boolean blocks(int x, int y) {
        if (forceZero.blocks(x, y) || forceOne.blocks(x, y)
                || siegeOuterLeft.blocks(x, y)
                || siegeOuterRight.blocks(x, y)
                || siegeInner.blocks(x, y)) return true;
        for (BattleGuardianTower tower : questFinalTowers) {
            if (tower.blocks(x, y)) return true;
        }
        return false;
    }

    synchronized int siegeAttackerForce() {
        return mode == BattleRules.Mode.SIEGE_WAR ? siegeAttackerForce : -1;
    }

    synchronized int siegeDefenderForce() {
        return mode == BattleRules.Mode.SIEGE_WAR ? 1 - siegeAttackerForce : -1;
    }

    synchronized SiegePhase siegePhase() {
        return siegePhase;
    }

    synchronized boolean siegePortalAllowed(int force) {
        return mode != BattleRules.Mode.SIEGE_WAR || force == siegeDefenderForce();
    }

    private BattleGuardianTower target(long objectId) {
        BattleGuardianTower[] candidates = {
                forceZero, forceOne, siegeOuterLeft, siegeOuterRight, siegeInner
        };
        for (BattleGuardianTower candidate : candidates) {
            BattleGuardianTower.Snapshot snapshot = candidate.snapshot();
            if (snapshot != null && snapshot.objectId() == objectId) return candidate;
        }
        for (BattleGuardianTower candidate : questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = candidate.snapshot();
            if (snapshot != null && snapshot.objectId() == objectId) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSiegeOuter(long objectId) {
        BattleGuardianTower.Snapshot left = siegeOuterLeft.snapshot();
        BattleGuardianTower.Snapshot right = siegeOuterRight.snapshot();
        return (left != null && left.objectId() == objectId)
                || (right != null && right.objectId() == objectId);
    }

    private boolean isSiegeInner(long objectId) {
        BattleGuardianTower.Snapshot inner = siegeInner.snapshot();
        return inner != null && inner.objectId() == objectId;
    }

    private static ArenaMapCatalog.Point adjacentSafe(ArenaMapCatalog.Arena arena,
                                                       ArenaMapCatalog.Point anchor,
                                                       int deltaX) {
        ArenaMapCatalog.Point candidate = new ArenaMapCatalog.Point(
                anchor.x() + deltaX, anchor.y());
        return arena.nativeCanStep(anchor.x(), anchor.y(), candidate.x(), candidate.y())
                ? candidate : anchor;
    }
}
