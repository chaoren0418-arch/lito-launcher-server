// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import local.lito381.server.ArenaMapCatalog;
import local.lito381.server.BattleControlCatalog;
import local.lito381.server.BattleDoor;
import local.lito381.server.BattleGuardianTower;
import local.lito381.server.BattleJoyOfPainStatus;
import local.lito381.server.BattleQuestEncounter;
import local.lito381.server.BattleRules;
import local.lito381.server.BattleTimeAttackEncounter;
import local.lito381.server.NativeLitoSiegePolicy;

final class BattleGuardianTowers {
    private static final String DATABASE_PLACEMENT_SOURCE = "lito_static.lito_battle_object";
    static final String HONOR_RELIC_HEALING_RULE_SOURCE = "LineageCompendium-LiTo-Honor-relic-has-HP-and-can-be-healed";
    static final String GEOMETRY_DERIVED_PLACEMENT = "LTmap-S32-safe-anchor-provisional";
    static final String TIME_ATTACK_GATE_SOURCE = "GameMeca-LWC-2005-participant-start-tower+LTmap-540-feature-anchor-provisional";
    static final String SIEGE_RULE_SOURCE = "LiTo-2005-20min-random-sides-two-outer-towers-inner-gate-inner-tower";
    static final String SIEGE_PLACEMENT_SOURCE = "LTmap-S32-safe-anchor-provisional-not-Layer8-decoration";
    static final String SIEGE_GFX_SOURCE = "same-family-common-guardian-tower-set-provisional-role-assignment";
    static final String QUEST_ROOM_ADVANCE_RULE_SOURCE = "4Gamer-Tokyo-2005-08-29-kill-room-monsters-to-enter-next-room";
    static final String QUEST_FINAL_TOWER_PRESENTATION_SOURCE = "2005-JP-tournament-final-Guardian-Tower+selected-client-common-tower-sprite-family+event-hp-coordinate-provisional";
    private final BattleGuardianTower forceZero = new BattleGuardianTower();
    private final BattleGuardianTower forceOne = new BattleGuardianTower();
    private final BattleGuardianTower siegeOuterLeft = new BattleGuardianTower();
    private final BattleGuardianTower siegeOuterRight = new BattleGuardianTower();
    private final BattleGuardianTower siegeInner = new BattleGuardianTower();
    private final BattleGuardianTower[] questFinalTowers = new BattleGuardianTower[]{new BattleGuardianTower(), new BattleGuardianTower()};
    private BattleRules.Mode mode;
    private ArenaMapCatalog.Arena arena;
    private BattleControlCatalog controls = BattleControlCatalog.defaults();
    private int siegeAttackerForce = -1;
    private SiegePhase siegePhase = SiegePhase.INACTIVE;

    BattleGuardianTowers() {
    }

    synchronized void reset(BattleRules.RuleSet rules, ArenaMapCatalog.Arena arena) {
        this.reset(rules, arena, -1);
    }

    synchronized void reset(BattleRules.RuleSet rules, ArenaMapCatalog.Arena arena, int requestedSiegeAttackerForce) {
        this.reset(rules, arena, requestedSiegeAttackerForce, BattleControlCatalog.defaults());
    }

    synchronized void reset(BattleRules.RuleSet rules, ArenaMapCatalog.Arena arena, int requestedSiegeAttackerForce, BattleControlCatalog controls) {
        ArenaMapCatalog.Point blue;
        this.deactivate();
        if (rules == null || arena == null || rules.mode() != BattleRules.Mode.HONOR_MATCH && rules.mode() != BattleRules.Mode.QUEST_BATTLE && rules.mode() != BattleRules.Mode.TIME_ATTACK && rules.mode() != BattleRules.Mode.SIEGE_WAR) {
            return;
        }
        this.mode = rules.mode();
        this.arena = arena;
        BattleControlCatalog battleControlCatalog = this.controls = controls == null ? BattleControlCatalog.defaults() : controls;
        if (this.mode == BattleRules.Mode.QUEST_BATTLE) {
            return;
        }
        if (this.mode == BattleRules.Mode.TIME_ATTACK) {
            this.resetControlled(this.forceZero, 0, "time-start", arena.featurePoint(), 2665, "$2205", TIME_ATTACK_GATE_SOURCE);
            return;
        }
        if (this.mode == BattleRules.Mode.SIEGE_WAR) {
            this.siegeAttackerForce = NativeLitoSiegePolicy.resolveAttackerForce(requestedSiegeAttackerForce, () -> ThreadLocalRandom.current().nextInt(2));
            int defenderForce = 1 - this.siegeAttackerForce;
            this.siegePhase = SiegePhase.OUTER_TOWERS;
            this.resetControlled(this.siegeOuterLeft, defenderForce, "siege-outer-left", arena.spawn(4), 2665, "$2205", SIEGE_PLACEMENT_SOURCE);
            this.resetControlled(this.siegeOuterRight, defenderForce, "siege-outer-right", arena.spawn(5), 2669, "$2204", SIEGE_PLACEMENT_SOURCE);
            this.resetControlled(this.siegeInner, defenderForce, "siege-inner", arena.spawn(6), 2665, "$2205", SIEGE_PLACEMENT_SOURCE);
            return;
        }
        ArenaMapCatalog.Point red = BattleGuardianTowers.adjacentSafe(arena, arena.spawn(4), 1);
        if (red.equals(blue = BattleGuardianTowers.adjacentSafe(arena, arena.spawn(5), -1))) {
            throw new IllegalStateException("guardian towers share a placement: " + arena.mapId());
        }
        this.resetControlled(this.forceZero, 0, "honor-red", red, 2665, "$2205", GEOMETRY_DERIVED_PLACEMENT);
        this.resetControlled(this.forceOne, 1, "honor-blue", blue, 2669, "$2204", GEOMETRY_DERIVED_PLACEMENT);
    }

    synchronized void deactivate() {
        this.forceZero.deactivate();
        this.forceOne.deactivate();
        this.siegeOuterLeft.deactivate();
        this.siegeOuterRight.deactivate();
        this.siegeInner.deactivate();
        for (BattleGuardianTower tower : this.questFinalTowers) {
            tower.deactivate();
        }
        this.mode = null;
        this.arena = null;
        this.controls = BattleControlCatalog.defaults();
        this.siegeAttackerForce = -1;
        this.siegePhase = SiegePhase.INACTIVE;
    }

    synchronized BattleGuardianTower.Snapshot revealQuestObjective(int force, ArenaMapCatalog.Point point, BattleGuardianTower.ObjectiveKind objectiveKind, String placementSource) {
        ArenaMapCatalog.Point selectedPoint;
        if (this.mode != BattleRules.Mode.QUEST_BATTLE || this.arena == null || force < 0 || force > 1 || point == null || objectiveKind == null || objectiveKind != BattleGuardianTower.ObjectiveKind.QUEST_ROOM_SYMBOL && objectiveKind != BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL && objectiveKind != BattleGuardianTower.ObjectiveKind.QUEST_FINAL_GUARDIAN_TOWER || placementSource == null || placementSource.isBlank()) {
            return null;
        }
        BattleGuardianTower target = this.questFinalTowers[force];
        BattleGuardianTower.Snapshot existing = target.snapshot();
        if (existing != null) {
            return existing;
        }
        BattleControlCatalog.ObjectPlacement configured = this.controls.object(this.mode, this.arena.mapId(), "quest-force-" + force);
        ArenaMapCatalog.Point point2 = selectedPoint = configured == null ? point : new ArenaMapCatalog.Point(configured.x(), configured.y());
        int selectedGfx = configured == null ? (force == 0 ? 2665 : 2669) : configured.gfx();
        int selectedMaximumHp = configured == null ? 3000 : configured.maximumHp();
        target.reset(force, this.arena, selectedPoint, selectedGfx, force == 0 ? "$2205" : "$2204", configured == null ? placementSource : DATABASE_PLACEMENT_SOURCE, objectiveKind, selectedMaximumHp);
        return target.snapshot();
    }

    private void resetControlled(BattleGuardianTower tower, int force, String role, ArenaMapCatalog.Point fallback, int fallbackGfx, String name, String fallbackSource) {
        BattleControlCatalog.ObjectPlacement configured = this.controls.object(this.mode, this.arena.mapId(), role);
        ArenaMapCatalog.Point point = configured == null ? fallback : new ArenaMapCatalog.Point(configured.x(), configured.y());
        tower.reset(force, this.arena, point, configured == null ? fallbackGfx : configured.gfx(), name, configured == null ? fallbackSource : DATABASE_PLACEMENT_SOURCE, BattleGuardianTower.ObjectiveKind.COMMON_GUARDIAN_TOWER, configured == null ? 3000 : configured.maximumHp());
    }

    synchronized BattleGuardianTower.Snapshot revealQuestFinalTower(int force, ArenaMapCatalog.Point point) {
        return this.revealQuestObjective(force, point, BattleGuardianTower.ObjectiveKind.QUEST_FINAL_GUARDIAN_TOWER, QUEST_FINAL_TOWER_PRESENTATION_SOURCE);
    }

    synchronized boolean isQuestObjective(long objectId) {
        if (this.mode != BattleRules.Mode.QUEST_BATTLE) {
            return false;
        }
        for (BattleGuardianTower tower : this.questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = tower.snapshot();
            if (snapshot == null || snapshot.objectId() != objectId) continue;
            return true;
        }
        return false;
    }

    synchronized boolean isQuestFinalTower(long objectId) {
        BattleGuardianTower.Snapshot objective = this.find(objectId);
        return objective != null && (objective.objectiveKind() == BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL || objective.objectiveKind() == BattleGuardianTower.ObjectiveKind.QUEST_FINAL_GUARDIAN_TOWER);
    }

    synchronized boolean deactivateQuestObjective(long objectId) {
        if (this.mode != BattleRules.Mode.QUEST_BATTLE) {
            return false;
        }
        for (BattleGuardianTower tower : this.questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = tower.snapshot();
            if (snapshot == null || snapshot.objectId() != objectId) continue;
            tower.deactivate();
            return true;
        }
        return false;
    }

    synchronized boolean deactivateQuestFinalTower(long objectId) {
        return this.isQuestFinalTower(objectId) && this.deactivateQuestObjective(objectId);
    }

    synchronized List<BattleGuardianTower.Snapshot> snapshots() {
        ArrayList<BattleGuardianTower.Snapshot> result = new ArrayList<BattleGuardianTower.Snapshot>(2);
        BattleGuardianTower.Snapshot zero = this.forceZero.snapshot();
        BattleGuardianTower.Snapshot one = this.forceOne.snapshot();
        if (zero != null) {
            result.add(zero);
        }
        if (one != null) {
            result.add(one);
        }
        BattleGuardianTower.Snapshot outerLeft = this.siegeOuterLeft.snapshot();
        BattleGuardianTower.Snapshot outerRight = this.siegeOuterRight.snapshot();
        BattleGuardianTower.Snapshot inner = this.siegeInner.snapshot();
        if (outerLeft != null) {
            result.add(outerLeft);
        }
        if (outerRight != null) {
            result.add(outerRight);
        }
        if (inner != null) {
            result.add(inner);
        }
        for (BattleGuardianTower tower : this.questFinalTowers) {
            BattleGuardianTower.Snapshot snapshot = tower.snapshot();
            if (snapshot == null) continue;
            result.add(snapshot);
        }
        return List.copyOf(result);
    }

    synchronized BattleGuardianTower.Snapshot find(long objectId) {
        for (BattleGuardianTower.Snapshot tower : this.snapshots()) {
            if (tower.objectId() != objectId) continue;
            return tower;
        }
        return null;
    }

    synchronized boolean canAttack(long objectId, int attackerForce) {
        BattleGuardianTower.Snapshot tower = this.find(objectId);
        if (tower == null || tower.destroyed() || attackerForce < 0 || attackerForce > 1) {
            return false;
        }
        if (this.mode == BattleRules.Mode.SIEGE_WAR) {
            if (attackerForce != this.siegeAttackerForce) {
                return false;
            }
            boolean outer = this.isSiegeOuter(objectId);
            boolean inner = this.isSiegeInner(objectId);
            return this.siegePhase == SiegePhase.OUTER_TOWERS && outer || this.siegePhase == SiegePhase.INNER_TOWER && inner;
        }
        return this.mode == BattleRules.Mode.HONOR_MATCH && tower.force() != attackerForce || this.mode == BattleRules.Mode.QUEST_BATTLE && tower.force() == attackerForce || this.mode == BattleRules.Mode.TIME_ATTACK;
    }

    synchronized boolean canHeal(long objectId, int healerForce) {
        BattleGuardianTower.Snapshot tower = this.find(objectId);
        return this.mode == BattleRules.Mode.HONOR_MATCH && tower != null && !tower.destroyed() && healerForce >= 0 && healerForce <= 1 && tower.force() == healerForce;
    }

    synchronized BattleGuardianTower.Healing heal(long objectId, int healerForce, int amount) {
        if (amount <= 0 || !this.canHeal(objectId, healerForce)) {
            return null;
        }
        BattleGuardianTower target = this.target(objectId);
        return target == null ? null : target.heal(amount);
    }

    synchronized DamageResult damage(long objectId, int attackerForce, int amount) {
        if (!this.canAttack(objectId, attackerForce)) {
            return null;
        }
        BattleGuardianTower target = this.target(objectId);
        if (target == null) {
            return null;
        }
        BattleGuardianTower.Damage damage = target.damage(amount);
        if (damage == null || this.mode != BattleRules.Mode.SIEGE_WAR) {
            return damage == null ? null : new DamageResult(damage, false, false);
        }
        boolean gateOpened = false;
        boolean strongholdDestroyed = false;
        if (damage.destroyedNow() && this.siegePhase == SiegePhase.OUTER_TOWERS && this.siegeOuterLeft.snapshot().destroyed() && this.siegeOuterRight.snapshot().destroyed()) {
            this.siegePhase = SiegePhase.INNER_TOWER;
            gateOpened = true;
        } else if (damage.destroyedNow() && this.siegePhase == SiegePhase.INNER_TOWER && this.isSiegeInner(objectId)) {
            this.siegePhase = SiegePhase.COMPLETE;
            strongholdDestroyed = true;
        }
        return new DamageResult(damage, gateOpened, strongholdDestroyed);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPain(long objectId, int casterForce, long nowNanos, int durationSeconds) {
        if (!this.canAttack(objectId, casterForce)) {
            return null;
        }
        BattleGuardianTower target = this.target(objectId);
        return target == null ? null : target.activateJoyOfPain(nowNanos, durationSeconds);
    }

    synchronized boolean blocks(int x, int y) {
        if (this.forceZero.blocks(x, y) || this.forceOne.blocks(x, y) || this.siegeOuterLeft.blocks(x, y) || this.siegeOuterRight.blocks(x, y) || this.siegeInner.blocks(x, y)) {
            return true;
        }
        for (BattleGuardianTower tower : this.questFinalTowers) {
            if (!tower.blocks(x, y)) continue;
            return true;
        }
        return false;
    }

    synchronized int siegeAttackerForce() {
        return this.mode == BattleRules.Mode.SIEGE_WAR ? this.siegeAttackerForce : -1;
    }

    synchronized int siegeDefenderForce() {
        return this.mode == BattleRules.Mode.SIEGE_WAR ? 1 - this.siegeAttackerForce : -1;
    }

    synchronized SiegePhase siegePhase() {
        return this.siegePhase;
    }

    synchronized boolean siegePortalAllowed(int force) {
        return this.mode != BattleRules.Mode.SIEGE_WAR || force == this.siegeDefenderForce();
    }

    private BattleGuardianTower target(long objectId) {
        BattleGuardianTower.Snapshot snapshot;
        BattleGuardianTower[] candidates;
        for (BattleGuardianTower candidate : candidates = new BattleGuardianTower[]{this.forceZero, this.forceOne, this.siegeOuterLeft, this.siegeOuterRight, this.siegeInner}) {
            snapshot = candidate.snapshot();
            if (snapshot == null || snapshot.objectId() != objectId) continue;
            return candidate;
        }
        for (BattleGuardianTower candidate : this.questFinalTowers) {
            snapshot = candidate.snapshot();
            if (snapshot == null || snapshot.objectId() != objectId) continue;
            return candidate;
        }
        return null;
    }

    private boolean isSiegeOuter(long objectId) {
        BattleGuardianTower.Snapshot left = this.siegeOuterLeft.snapshot();
        BattleGuardianTower.Snapshot right = this.siegeOuterRight.snapshot();
        return left != null && left.objectId() == objectId || right != null && right.objectId() == objectId;
    }

    private boolean isSiegeInner(long objectId) {
        BattleGuardianTower.Snapshot inner = this.siegeInner.snapshot();
        return inner != null && inner.objectId() == objectId;
    }

    private static ArenaMapCatalog.Point adjacentSafe(ArenaMapCatalog.Arena arena, ArenaMapCatalog.Point anchor, int deltaX) {
        ArenaMapCatalog.Point candidate = new ArenaMapCatalog.Point(anchor.x() + deltaX, anchor.y());
        return arena.nativeCanStep(anchor.x(), anchor.y(), candidate.x(), candidate.y()) ? candidate : anchor;
    }

    static enum SiegePhase {
        INACTIVE,
        OUTER_TOWERS,
        INNER_TOWER,
        COMPLETE;

    }

    record DamageResult(BattleGuardianTower.Damage damage, boolean siegeGateOpened, boolean siegeStrongholdDestroyed) {
    }

    record Hit(BattleGuardianTower.Damage damage, BattleRules.Resolution resolution, List<BattleTimeAttackEncounter.Snapshot> spawnedTimeAttackActors, BattleDoor.Snapshot openedSiegeGate, List<BattleQuestEncounter.Snapshot> spawnedQuestActors, int completedQuestRoom, String timeAttackStartRole) {
        Hit {
            spawnedTimeAttackActors = List.copyOf(spawnedTimeAttackActors);
            spawnedQuestActors = List.copyOf(spawnedQuestActors);
            if (completedQuestRoom < 0 || completedQuestRoom > 4) {
                throw new IllegalArgumentException("invalid completed Quest room");
            }
            if (timeAttackStartRole == null || !timeAttackStartRole.isEmpty() && !timeAttackStartRole.equals("leader") && !timeAttackStartRole.equals("member")) {
                throw new IllegalArgumentException("invalid Time Attack start role");
            }
        }
    }
}
