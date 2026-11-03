package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleGuardianTowerTest {
    @Test
    void honorMatchCreatesTwoEnemyForceWorldObjectsFromSafeS32Anchors() {
        BattleGuardianTowers towers = new BattleGuardianTowers();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        towers.reset(rules(BattleRules.Mode.HONOR_MATCH), arena);

        List<BattleGuardianTower.Snapshot> snapshots = towers.snapshots();
        assertEquals(2, snapshots.size());
        BattleGuardianTower.Snapshot red = snapshots.get(0);
        BattleGuardianTower.Snapshot blue = snapshots.get(1);
        assertEquals(0, red.force());
        assertEquals(1, blue.force());
        assertEquals(BattleGuardianTower.GFX_FIRE, red.gfx());
        assertEquals(BattleGuardianTower.GFX_WATER, blue.gfx());
        assertEquals(BattleGuardianTower.MAX_HP, red.hp());
        assertEquals(BattleGuardianTower.ACTION_HEALTHY, red.action());
        assertEquals(BattleGuardianTowers.GEOMETRY_DERIVED_PLACEMENT,
                red.placementSource());
        assertEquals(BattleGuardianTower.ObjectiveKind.COMMON_GUARDIAN_TOWER,
                red.objectiveKind());
        assertNotEquals(new ArenaMapCatalog.Point(red.x(), red.y()),
                new ArenaMapCatalog.Point(blue.x(), blue.y()));
        assertTrue(arena.contains(red.x(), red.y()));
        assertTrue(arena.contains(blue.x(), blue.y()));
        assertTrue(towers.blocks(red.x(), red.y()));
    }

    @Test
    void friendlyTowerIsProtectedAndEnemyTowerAcceptsDamage() {
        BattleGuardianTowers towers = new BattleGuardianTowers();
        towers.reset(rules(BattleRules.Mode.HONOR_MATCH), ArenaMapCatalog.forMap(510));
        BattleGuardianTower.Snapshot forceZero = towers.snapshots().get(0);
        BattleGuardianTower.Snapshot forceOne = towers.snapshots().get(1);

        assertFalse(towers.canAttack(forceZero.objectId(), 0));
        assertTrue(towers.canAttack(forceOne.objectId(), 0));
        assertNull(towers.damage(forceZero.objectId(), 0, 10));
        BattleGuardianTower.Damage hit = towers.damage(
                forceOne.objectId(), 0, 10).damage();
        assertEquals(BattleGuardianTower.MAX_HP - 10, hit.after().hp());
        assertFalse(hit.actionChanged());
        assertFalse(hit.destroyedNow());
    }

    @Test
    void honorRelicHealingRequiresItsOwnForceAndRestoresCrackState() {
        BattleGuardianTowers towers = new BattleGuardianTowers();
        towers.reset(rules(BattleRules.Mode.HONOR_MATCH),
                ArenaMapCatalog.forMap(512));
        BattleGuardianTower.Snapshot red = towers.snapshots().get(0);

        BattleGuardianTower.Damage damaged = towers.damage(
                red.objectId(), 1, 751).damage();
        assertEquals(2_249, damaged.after().hp());
        assertEquals(BattleGuardianTower.ACTION_CRACK_1,
                damaged.after().action());
        assertTrue(towers.canHeal(red.objectId(), 0));
        assertFalse(towers.canHeal(red.objectId(), 1));
        assertNull(towers.heal(red.objectId(), 1, 1));

        BattleGuardianTower.Healing threshold = towers.heal(
                red.objectId(), 0, 1);
        assertEquals(2_250, threshold.after().hp());
        assertEquals(1, threshold.restoredHp());
        assertTrue(threshold.actionChanged());
        assertEquals(BattleGuardianTower.ACTION_HEALTHY,
                threshold.after().action());

        BattleGuardianTower.Healing clamped = towers.heal(
                red.objectId(), 0, BattleGuardianTower.MAX_HP);
        assertEquals(BattleGuardianTower.MAX_HP, clamped.after().hp());
        assertEquals(750, clamped.restoredHp());
        assertFalse(clamped.actionChanged());
    }

    @Test
    void relicHealingDoesNotAuthorizeOtherTowerModesOrResurrection() {
        for (BattleRules.Mode mode : List.of(
                BattleRules.Mode.TIME_ATTACK,
                BattleRules.Mode.SIEGE_WAR,
                BattleRules.Mode.QUEST_BATTLE)) {
            BattleGuardianTowers towers = new BattleGuardianTowers();
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                    mode.maps().get(0));
            towers.reset(rules(mode), arena, mode == BattleRules.Mode.SIEGE_WAR
                    ? 0 : -1);
            BattleGuardianTower.Snapshot tower = mode == BattleRules.Mode.QUEST_BATTLE
                    ? towers.revealQuestFinalTower(0, arena.spawn(7))
                    : towers.snapshots().get(0);
            assertFalse(towers.canHeal(tower.objectId(), tower.force()), mode.name());
            assertNull(towers.heal(tower.objectId(), tower.force(), 10), mode.name());
        }

        BattleGuardianTowers honor = new BattleGuardianTowers();
        honor.reset(rules(BattleRules.Mode.HONOR_MATCH),
                ArenaMapCatalog.forMap(512));
        BattleGuardianTower.Snapshot red = honor.snapshots().get(0);
        honor.damage(red.objectId(), 1, BattleGuardianTower.MAX_HP);
        assertFalse(honor.canHeal(red.objectId(), 0));
        assertNull(honor.heal(red.objectId(), 0, 10));
    }

    @Test
    void exactQuarterThresholdsDriveCrackAndTowerDeathActions() {
        BattleGuardianTower tower = new BattleGuardianTower();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.rectangularForTest(
                99, 100, 110, 100, 110);
        tower.reset(1, arena, new ArenaMapCatalog.Point(105, 105),
                BattleGuardianTower.GFX_WATER, BattleGuardianTower.NAME_WATER,
                "unit-test");

        BattleGuardianTower.Damage threshold = tower.damage(750);
        assertEquals(2_250, threshold.after().hp());
        assertEquals(BattleGuardianTower.ACTION_HEALTHY, threshold.after().action());
        assertFalse(threshold.actionChanged());

        BattleGuardianTower.Damage crackOne = tower.damage(1);
        assertEquals(BattleGuardianTower.ACTION_CRACK_1, crackOne.after().action());
        assertTrue(crackOne.actionChanged());

        BattleGuardianTower.Damage crackTwo = tower.damage(750);
        assertEquals(1_499, crackTwo.after().hp());
        assertEquals(BattleGuardianTower.ACTION_CRACK_2, crackTwo.after().action());

        BattleGuardianTower.Damage crackThree = tower.damage(750);
        assertEquals(749, crackThree.after().hp());
        assertEquals(BattleGuardianTower.ACTION_CRACK_3, crackThree.after().action());

        BattleGuardianTower.Damage death = tower.damage(749);
        assertEquals(0, death.after().hp());
        assertEquals(BattleGuardianTower.ACTION_DIE, death.after().action());
        assertTrue(death.destroyedNow());
        assertFalse(tower.blocks(105, 105));
        assertNull(tower.damage(10));
    }

    @Test
    void modesWithoutObjectiveOrStartTowersDoNotInventTowerActors() {
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            if (mode == BattleRules.Mode.HONOR_MATCH
                    || mode == BattleRules.Mode.TIME_ATTACK
                    || mode == BattleRules.Mode.SIEGE_WAR) continue;
            BattleGuardianTowers towers = new BattleGuardianTowers();
            towers.reset(rules(mode), ArenaMapCatalog.forMap(mode.maps().get(0)));
            assertTrue(towers.snapshots().isEmpty(), mode.name());
        }
    }

    @Test
    void timeAttackCreatesOneCommonStartTowerAtMapFeatureAnchor() {
        BattleGuardianTowers towers = new BattleGuardianTowers();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(540);
        towers.reset(rules(BattleRules.Mode.TIME_ATTACK), arena);

        List<BattleGuardianTower.Snapshot> snapshots = towers.snapshots();
        assertEquals(1, snapshots.size());
        BattleGuardianTower.Snapshot gate = snapshots.get(0);
        assertEquals(0, gate.force());
        assertEquals(arena.featurePoint().x(), gate.x());
        assertEquals(arena.featurePoint().y(), gate.y());
        assertEquals(BattleGuardianTowers.TIME_ATTACK_GATE_SOURCE,
                gate.placementSource());
        assertEquals(BattleGuardianTower.ObjectiveKind.COMMON_GUARDIAN_TOWER,
                gate.objectiveKind());
        assertTrue(towers.canAttack(gate.objectId(), 0));
        assertTrue(towers.canAttack(gate.objectId(), 1));
    }

    @Test
    void questSupportsMapSpecificSymbolAndFinalTowerRoles() {
        BattleGuardianTowers towers = new BattleGuardianTowers();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        towers.reset(rules(BattleRules.Mode.QUEST_BATTLE), arena);
        assertTrue(towers.snapshots().isEmpty());

        BattleGuardianTower.Snapshot finalTower = towers.revealQuestFinalTower(
                0, arena.spawn(7));
        assertEquals(BattleGuardianTower.ObjectiveKind.QUEST_FINAL_GUARDIAN_TOWER,
                finalTower.objectiveKind());
        assertEquals(BattleQuestEncounter.FINAL_TOWER_PRESENTATION_SOURCE,
                finalTower.placementSource());
        assertEquals(BattleGuardianTower.GFX_FIRE, finalTower.gfx());
        assertTrue(towers.isQuestFinalTower(finalTower.objectId()));
        assertTrue(towers.canAttack(finalTower.objectId(), 0));
        assertFalse(towers.canAttack(finalTower.objectId(), 1));
        assertTrue(towers.deactivateQuestFinalTower(finalTower.objectId()));
        assertTrue(towers.snapshots().isEmpty());

        BattleGuardianTower.Snapshot roomSymbol = towers.revealQuestObjective(
                0, arena.spawn(6),
                BattleGuardianTower.ObjectiveKind.QUEST_ROOM_SYMBOL,
                BattleQuestEncounter.PER_ROOM_SYMBOL_RULE_SOURCE);
        assertTrue(towers.isQuestObjective(roomSymbol.objectId()));
        assertFalse(towers.isQuestFinalTower(roomSymbol.objectId()));
        assertTrue(towers.deactivateQuestObjective(roomSymbol.objectId()));

        BattleGuardianTower.Snapshot finalSymbol = towers.revealQuestObjective(
                0, arena.spawn(7),
                BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL,
                BattleQuestEncounter.PER_ROOM_SYMBOL_RULE_SOURCE);
        assertTrue(towers.isQuestObjective(finalSymbol.objectId()));
        assertTrue(towers.isQuestFinalTower(finalSymbol.objectId()));
        assertEquals(BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL,
                finalSymbol.objectiveKind());
    }

    @Test
    void siegeRequiresBothOuterTowersBeforeGateAndInnerStronghold() {
        BattleGuardianTowers towers = new BattleGuardianTowers();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(510);
        towers.reset(rules(BattleRules.Mode.SIEGE_WAR), arena, 0);

        assertEquals(0, towers.siegeAttackerForce());
        assertEquals(1, towers.siegeDefenderForce());
        assertEquals(BattleGuardianTowers.SiegePhase.OUTER_TOWERS,
                towers.siegePhase());
        List<BattleGuardianTower.Snapshot> snapshots = towers.snapshots();
        assertEquals(3, snapshots.size());
        BattleGuardianTower.Snapshot left = snapshots.get(0);
        BattleGuardianTower.Snapshot right = snapshots.get(1);
        BattleGuardianTower.Snapshot inner = snapshots.get(2);
        assertEquals(1, left.force());
        assertEquals(BattleGuardianTowers.SIEGE_PLACEMENT_SOURCE,
                left.placementSource());
        assertTrue(towers.canAttack(left.objectId(), 0));
        assertTrue(towers.canAttack(right.objectId(), 0));
        assertFalse(towers.canAttack(inner.objectId(), 0));
        assertFalse(towers.canAttack(left.objectId(), 1));

        BattleGuardianTowers.DamageResult first = towers.damage(
                left.objectId(), 0, BattleGuardianTower.MAX_HP);
        assertTrue(first.damage().destroyedNow());
        assertFalse(first.siegeGateOpened());
        assertNull(towers.damage(inner.objectId(), 0, 1));

        BattleGuardianTowers.DamageResult second = towers.damage(
                right.objectId(), 0, BattleGuardianTower.MAX_HP);
        assertTrue(second.siegeGateOpened());
        assertFalse(second.siegeStrongholdDestroyed());
        assertEquals(BattleGuardianTowers.SiegePhase.INNER_TOWER,
                towers.siegePhase());
        assertTrue(towers.canAttack(inner.objectId(), 0));

        BattleGuardianTowers.DamageResult finalHit = towers.damage(
                inner.objectId(), 0, BattleGuardianTower.MAX_HP);
        assertTrue(finalHit.siegeStrongholdDestroyed());
        assertEquals(BattleGuardianTowers.SiegePhase.COMPLETE,
                towers.siegePhase());
        assertFalse(towers.blocks(inner.x(), inner.y()));
    }

    @Test
    void nativePutObjectCarriesTowerActionNameAndVisibleHpRatio() {
        BattleGuardianTower tower = new BattleGuardianTower();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.rectangularForTest(
                99, 100, 110, 100, 110);
        tower.reset(1, arena, new ArenaMapCatalog.Point(105, 105),
                BattleGuardianTower.GFX_WATER, BattleGuardianTower.NAME_WATER,
                "unit-test");
        tower.damage(751);
        BattleGuardianTower.Snapshot snapshot = tower.snapshot();

        Protocol.Reader packet = new Protocol.Reader(Protocol.guardianTower(snapshot));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(105, packet.h());
        assertEquals(105, packet.h());
        assertEquals(snapshot.objectId(), packet.d());
        assertEquals(BattleGuardianTower.GFX_WATER, packet.h());
        assertEquals(BattleGuardianTower.ACTION_CRACK_1, packet.c());
        packet.c(); // heading
        packet.c(); // light
        assertEquals(BattleTimedStatus.NORMAL, packet.c());
        packet.d();
        packet.h();
        assertEquals(BattleGuardianTower.NAME_WATER, packet.s());
        assertEquals("", packet.s());
        assertEquals(0, packet.c()); // NPC status
        packet.d();
        packet.s();
        packet.s();
        packet.c();
        assertEquals(Protocol.healthRatio(snapshot.hp(), snapshot.maxHp()), packet.c());
    }

    private static BattleRules.RuleSet rules(BattleRules.Mode mode) {
        return BattleRules.decode(new GameHub.RoomSettings(
                "mode", "", 8, mode.wireId(), 0, 0, 0, 0, List.of()));
    }
}
