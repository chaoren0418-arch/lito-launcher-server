package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class BattleQuestEncounterTest {
    @Test
    void cubeHarmonyRecoversOnlyInitializedMonsterManaAndCapsAtSourceMaximum() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        assertEquals(0, encounter.recoverMana(monster.objectId(), 5));
        BattleQuestEncounter.ManaDrain drained = encounter.drainMana(
                monster.objectId(), 0, monster.maximumMp(), 5);
        assertEquals(5, drained.afterMp());
        assertEquals(5, encounter.recoverMana(monster.objectId(), 5));
        assertEquals(10, encounter.drainMana(
                monster.objectId(), 0, monster.maximumMp(), 100).drained());
    }

    @Test
    void ordinaryQuestNpcOwnsCancelableHorrorOfDeathLease() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = 1_000_000_000L;

        assertFalse(encounter.horrorOfDeath(
                monster.objectId(), 0, now, 60));
        assertTrue(encounter.horrorOfDeathActive(monster.objectId(), now));
        assertTrue(encounter.horrorOfDeath(
                monster.objectId(), 0, now + 1, 60));
        assertTrue(encounter.expireHorrorOfDeaths(
                now + TimeUnit.SECONDS.toNanos(59)).isEmpty());
        assertEquals(1, encounter.expireHorrorOfDeaths(
                now + TimeUnit.SECONDS.toNanos(60)).size());
        assertFalse(encounter.horrorOfDeathActive(
                monster.objectId(), now + TimeUnit.SECONDS.toNanos(60)));
        assertFalse(encounter.horrorOfDeath(monster.objectId(), 0,
                now + TimeUnit.SECONDS.toNanos(61), 60));
        BattleQuestEncounter.MagicCancellation cancelled =
                encounter.cancelMagicEffects(monster.objectId(), 0);
        assertTrue(cancelled.horrorOfDeath());
        assertFalse(encounter.horrorOfDeathActive(monster.objectId(), now + 2));
    }

    @Test
    void preservesSpeciesOrderAndPhotoVisibleRoomTotalLowerBounds() {
        List<List<BattleQuestEncounter.Template>> waves =
                BattleQuestEncounter.waves();
        assertEquals(4, waves.size());
        assertEquals(List.of(45262, 45262), npcIds(waves.get(0)));
        assertEquals(List.of(45207, 45289, 45207, 45289,
                45207, 45289, 45207), npcIds(waves.get(1)));
        assertEquals(List.of(45259, 45278, 45322,
                45259, 45278, 45322,
                45259, 45278, 45322), npcIds(waves.get(2)));
        assertEquals(List.of(45390, 45380, 45601,
                45390, 45380, 45601, 45390), npcIds(waves.get(3)));
        assertTrue(BattleQuestEncounter.COUNT_SOURCE.contains(
                "visible-total-lower-bound"));
        assertTrue(BattleQuestEncounter.COUNT_SOURCE.contains(
                "room1-two-visible-native-gfx1649-lower-bound"));
        assertEquals(2, BattleQuestEncounter.ROOM_ONE_PHOTO_LOWER_BOUND);
        assertTrue(BattleQuestEncounter.PLACEMENT_SOURCE.contains(
                "connected-local-envelope-provisional"));
    }

    @Test
    void everyQuestMapPlacesEachPhotoBoundedWaveOnUniqueNativeFloor() {
        List<Integer> expectedTotals = List.of(2, 7, 9, 7);
        for (int mapId : BattleRules.Mode.QUEST_BATTLE.maps()) {
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
            BattleQuestEncounter encounter = new BattleQuestEncounter();
            encounter.reset(questRules(mapId), arena);
            for (int room = 1; room <= 4; room++) {
                List<BattleQuestEncounter.Snapshot> wave = encounter.snapshots(0);
                assertEquals(expectedTotals.get(room - 1), wave.size(),
                        "map=" + mapId + " room=" + room);
                assertEquals(wave.size(), wave.stream()
                        .map(actor -> new ArenaMapCatalog.Point(actor.x(), actor.y()))
                        .distinct().count(), "map=" + mapId + " room=" + room);
                for (BattleQuestEncounter.Snapshot actor : wave) {
                    assertTrue(arena.nativeStandingFloor(actor.x(), actor.y()),
                            "map=" + mapId + " room=" + room);
                    assertEquals(BattleQuestEncounter.PLACEMENT_SOURCE,
                            actor.placementSource());
                }
                for (BattleQuestEncounter.Snapshot actor : wave) {
                    encounter.damage(actor.objectId(), 0, actor.maxHp());
                }
                if (BattleQuestEncounter.usesPerRoomSymbols(mapId)) {
                    BattleQuestEncounter.ObjectiveCompletion completion =
                            encounter.destroyObjective(0);
                    assertNotNull(completion);
                    assertEquals(room == 4, completion.matchCompleted());
                } else if (room == 4) {
                    assertNotNull(encounter.destroyFinalTower(0));
                }
            }
        }
    }

    @Test
    void hellgateAndTerribleQuestRequireEachRoomSymbolBeforeNextArmy() {
        for (int mapId : List.of(513, 527)) {
            BattleQuestEncounter encounter = new BattleQuestEncounter();
            encounter.reset(questRules(mapId), ArenaMapCatalog.forMap(mapId));
            assertTrue(encounter.usesPerRoomSymbols());
            for (int room = 1; room <= 4; room++) {
                List<BattleQuestEncounter.Snapshot> wave = encounter.snapshots(0);
                assertFalse(wave.isEmpty());
                BattleQuestEncounter.Hit last = null;
                for (BattleQuestEncounter.Snapshot actor : wave) {
                    last = encounter.damage(actor.objectId(), 0, actor.maxHp());
                }
                assertNotNull(last);
                assertTrue(last.objectiveUnlocked());
                assertTrue(last.spawned().isEmpty());
                assertTrue(encounter.snapshots(0).isEmpty());
                assertNotNull(encounter.pendingObjectivePoint(0));
                assertEquals(room == 4
                                ? BattleGuardianTower.ObjectiveKind.QUEST_FINAL_SYMBOL
                                : BattleGuardianTower.ObjectiveKind.QUEST_ROOM_SYMBOL,
                        encounter.pendingObjectiveKind(0));

                BattleQuestEncounter.ObjectiveCompletion completion =
                        encounter.destroyObjective(0);
                assertNotNull(completion);
                assertEquals(room, completion.completedRoom());
                assertEquals(room == 4, completion.matchCompleted());
                assertEquals(room == 4, completion.spawned().isEmpty());
            }
        }
    }

    @Test
    void questOgreExecutesExactSelectedAreaPhysicalMobskillRow() {
        BattleQuestEncounter encounter = encounterAtRoom(3);
        BattleQuestEncounter.Snapshot ogre = actor(encounter, 45278);

        BattleQuestEncounter.AiAttack attack = encounter.aiAttackForActNoFixture(
                ogre.objectId(), ogre.x() + 2, ogre.y(), 0, 0, 0);

        assertNotNull(attack);
        assertFalse(attack.magic());
        assertFalse(attack.summon());
        assertEquals(1, attack.triggerRoll());
        BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physical =
                attack.physicalSkill();
        assertNotNull(physical);
        assertEquals(45278, physical.npcId());
        assertEquals(0, physical.actNo());
        assertEquals(2, physical.attackRange());
        assertEquals(20, physical.leverage());
        assertEquals(BattleActionCodes.ATTACK, physical.effectiveActionId());
        assertEquals(1, physical.areaWidth());
        assertEquals(2, physical.areaHeight());
        assertTrue(physical.areaContains(
                ogre.x(), ogre.y(), 2, ogre.x() + 2, ogre.y()));
        assertFalse(physical.areaContains(
                ogre.x(), ogre.y(), 2, ogre.x(), ogre.y() + 2));
        assertTrue(attack.spawned().isEmpty());
    }

    @Test
    void questDeathKnightExecutesBothExactSelectedMagicActNumbersAndMpCosts() {
        BattleQuestEncounter vampireEncounter = encounterAtRoom(4);
        BattleQuestEncounter.Snapshot vampireCaster = actor(vampireEncounter, 45601);
        BattleQuestEncounter.AiAttack vampire =
                vampireEncounter.aiAttackForActNoFixture(
                        vampireCaster.objectId(), vampireCaster.x() + 3,
                        vampireCaster.y(), 0, 0, 0);
        assertNotNull(vampire);
        assertTrue(vampire.magic());
        assertEquals(0, vampire.magicSkill().actNo());
        assertEquals(28, vampire.magicSkill().skillId());
        assertEquals(238, vampire.actor().currentMp());
        assertEquals(250, vampire.actor().maximumMp());
        assertEquals(19, vampire.actor().intelligence());

        BattleQuestEncounter earthquakeEncounter = encounterAtRoom(4);
        BattleQuestEncounter.Snapshot earthquakeCaster =
                actor(earthquakeEncounter, 45601);
        BattleQuestEncounter.AiAttack earthquake =
                earthquakeEncounter.aiAttackForActNoFixture(
                        earthquakeCaster.objectId(), earthquakeCaster.x() + 6,
                        earthquakeCaster.y(), 0, 1, 0);
        assertNotNull(earthquake);
        assertTrue(earthquake.magic());
        assertEquals(1, earthquake.magicSkill().actNo());
        assertEquals(10056, earthquake.magicSkill().skillId());
        assertEquals(BattleTimeAttackMagicSkillCatalog.Targeting.CASTER_RADIUS,
                earthquake.magicSkill().targeting());
        assertEquals(245, earthquake.actor().currentMp());
    }

    @Test
    void questDeathKnightSummonsOneToThreeSpartoiAtMostTwice() {
        BattleQuestEncounter encounter = encounterAtRoom(4);
        BattleQuestEncounter.Snapshot deathKnight = actor(encounter, 45601);

        BattleQuestEncounter.AiAttack first = encounter.aiAttackForActNoFixture(
                deathKnight.objectId(), deathKnight.x() + 3,
                deathKnight.y(), 0, 2, 2);
        assertNotNull(first);
        assertTrue(first.summon());
        assertEquals(2, first.summonSkill().actNo());
        assertEquals(3, first.spawned().size());
        for (BattleQuestEncounter.Snapshot spartoi : first.spawned()) {
            assertEquals(45161, spartoi.npcId());
            assertEquals(145, spartoi.gfx());
            assertEquals(120, spartoi.maxHp());
            assertEquals(30, spartoi.currentMp());
            assertEquals(BattleQuestEncounter.DEATH_KNIGHT_SUMMON_SOURCE,
                    spartoi.speciesSource());
            assertEquals("selected-mobskill-summon-count", spartoi.countSource());
            assertEquals(BattleQuestEncounter.SUMMON_PLACEMENT_SOURCE,
                    spartoi.placementSource());
        }

        long secondReady = TimeUnit.MILLISECONDS.toNanos(
                first.summonSkill().actionDelayMillis());
        BattleQuestEncounter.AiAttack second = encounter.aiAttackForActNoFixture(
                deathKnight.objectId(), deathKnight.x() + 3,
                deathKnight.y(), secondReady, 2, 0);
        assertNotNull(second);
        assertEquals(1, second.spawned().size());
        assertNull(encounter.aiAttackForActNoFixture(
                deathKnight.objectId(), deathKnight.x() + 3,
                deathKnight.y(), secondReady * 2, 2, 1));
    }

    @Test
    void questDeathKnightActThreeRemainsExactAdjacentPhysicalFallbackRow() {
        BattleQuestEncounter encounter = encounterAtRoom(4);
        BattleQuestEncounter.Snapshot deathKnight = actor(encounter, 45601);

        BattleQuestEncounter.AiAttack attack = encounter.aiAttackForActNoFixture(
                deathKnight.objectId(), deathKnight.x() + 1,
                deathKnight.y(), 0, 3, 0);

        assertNotNull(attack);
        assertFalse(attack.magic());
        assertFalse(attack.summon());
        assertEquals(3, attack.physicalSkill().actNo());
        assertEquals(100, attack.physicalSkill().triggerChance());
        assertEquals(1, attack.physicalSkill().attackRange());
        assertEquals(20, attack.physicalSkill().leverage());
        assertEquals(BattleActionCodes.ATTACK,
                attack.physicalSkill().effectiveActionId());
    }

    @Test
    void formatsHistoricallyVisibleForceArmyAnnouncements() {
        assertEquals("Force Red: 第2軍登場!",
                BattleQuestEncounter.armyAnnouncement(0, 2));
        assertEquals("Force Blue: 第3軍登場!",
                BattleQuestEncounter.armyAnnouncement(1, 3));
        assertEquals("Force Red: 第4軍登場!",
                BattleQuestEncounter.armyAnnouncement(0, 4));
        assertThrows(IllegalArgumentException.class,
                () -> BattleQuestEncounter.armyAnnouncement(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> BattleQuestEncounter.armyAnnouncement(2, 2));
    }

    @Test
    void createsIndependentForceLanesAndRejectsCrossLaneDamage() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));

        BattleQuestEncounter.Snapshot red = encounter.snapshots(0).get(0);
        BattleQuestEncounter.Snapshot blue = encounter.snapshots(1).get(0);
        assertNotEquals(red.objectId(), blue.objectId());
        assertEquals(red.npcId(), blue.npcId());
        ArenaMapCatalog.Point redAnchor = ArenaMapCatalog.forMap(554).spawn(4);
        ArenaMapCatalog.Point blueAnchor = ArenaMapCatalog.forMap(554).spawn(4);
        assertEquals(redAnchor.x(), red.x());
        assertEquals(redAnchor.y(), red.y());
        assertEquals(blueAnchor.x(), blue.x());
        assertEquals(blueAnchor.y(), blue.y());
        assertEquals(BattleQuestEncounter.COUNT_SOURCE, red.countSource());
        assertTrue(encounter.canAttack(red.objectId(), 0));
        assertFalse(encounter.canAttack(red.objectId(), 1));
        assertNull(encounter.damage(red.objectId(), 1, red.maxHp()));
        assertEquals(1, encounter.roomNumber(0));
        assertEquals(1, encounter.roomNumber(1));
    }

    @Test
    void nativeHealingRestoresOnlyTheVisibleLivingForceActorAndCapsAtMaxHp() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot red = encounter.snapshots(0).get(0);

        BattleQuestEncounter.Hit hit = encounter.damage(red.objectId(), 0, 25);
        assertEquals(red.maxHp() - 25, hit.damage().after().hp());
        assertNull(encounter.heal(red.objectId(), 1, 10));
        BattleQuestEncounter.Healing healing = encounter.heal(red.objectId(), 0, 40);
        assertEquals(red.maxHp() - 25, healing.before().hp());
        assertEquals(red.maxHp(), healing.after().hp());
        assertEquals(25, healing.restoredHp());
        assertEquals(0, encounter.heal(red.objectId(), 0, 0).restoredHp());
    }

    @Test
    void weaponBreakStacksAreForceIsolatedAndResetOnDeath() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot red = encounter.snapshots(0).get(0);
        assertEquals(-1, encounter.addWeaponBreakStack(red.objectId(), 1));
        assertEquals(1, encounter.addWeaponBreakStack(red.objectId(), 0));
        assertEquals(1, encounter.weaponBreakStacks(red.objectId()));
        encounter.damage(red.objectId(), 0, red.maxHp());
        assertEquals(0, encounter.weaponBreakStacks(red.objectId()));
    }

    @Test void elementalFallDownIsForceIsolatedRefreshesAndCancellationRestores() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot red = encounter.snapshots(0).get(0);
        long now = 1_000_000_000L;
        assertFalse(encounter.elementalFallDown(
                red.objectId(), 1, now, 32, 1));
        assertTrue(encounter.elementalFallDown(
                red.objectId(), 0, now, 32, 1));
        assertEquals(-50, encounter.elementalFallDownModifier(
                red.objectId(), 1, now));
        assertTrue(encounter.elementalFallDown(
                red.objectId(), 0, now + TimeUnit.SECONDS.toNanos(10), 32, 2));
        assertEquals(-50, encounter.elementalFallDownModifier(
                red.objectId(), 1, now + TimeUnit.SECONDS.toNanos(20)));
        assertEquals(0, encounter.elementalFallDownModifier(
                red.objectId(), 2, now + TimeUnit.SECONDS.toNanos(20)));
        BattleQuestEncounter.MagicCancellation cancelled =
                encounter.cancelMagicEffects(red.objectId(), 0);
        assertTrue(cancelled.elementalFallDown().present());
        assertEquals(1, cancelled.elementalFallDown().attributeKind());
        assertEquals(0, encounter.elementalFallDownModifier(
                red.objectId(), 1, now + TimeUnit.SECONDS.toNanos(20)));
    }

    @Test void eraseMagicQuartersMrConsumesExactLeaseAndCancellationClears() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot target = encounter.snapshots(0).get(0);
        long now = 1_500_000_000L;
        assertFalse(encounter.eraseMagic(target.objectId(), 1, now, 32));
        assertTrue(encounter.eraseMagic(target.objectId(), 0, now, 32));
        assertEquals(25, encounter.effectiveMagicResistance(
                target.objectId(), 103, now));
        BattleEraseMagicStatus.Lease stale =
                encounter.captureEraseMagic(target.objectId(), now);
        assertTrue(encounter.eraseMagic(target.objectId(), 0, now + 1, 32));
        assertFalse(encounter.consumeEraseMagic(target.objectId(), stale));
        BattleQuestEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(target.objectId(), 0);
        assertTrue(cancellation.eraseMagic());
        assertEquals(103, encounter.effectiveMagicResistance(
                target.objectId(), 103, now + 2));
    }

    @Test
    void manaDrainInitializesFromSourceMpAndPersistsPerQuestActor() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot red = encounter.snapshots(0).get(0);

        assertNull(encounter.drainMana(red.objectId(), 1, 10, 9));
        BattleQuestEncounter.ManaDrain first =
                encounter.drainMana(red.objectId(), 0, 10, 9);
        assertEquals(10, first.maximumMp());
        assertEquals(10, first.beforeMp());
        assertEquals(1, first.afterMp());
        assertEquals(9, first.drained());
        BattleQuestEncounter.ManaDrain second =
                encounter.drainMana(red.objectId(), 0, 10, 18);
        assertEquals(1, second.beforeMp());
        assertEquals(0, second.afterMp());
        assertEquals(1, second.drained());
        assertThrows(IllegalStateException.class,
                () -> encounter.drainMana(red.objectId(), 0, 11, 1));
    }

    @Test
    void mindBreakStrictManaDebitRequiresMoreThanRequested() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot red = encounter.snapshots(0).get(0);
        BattleQuestEncounter.ManaDrain first =
                encounter.drainManaIfStrictlyAbove(red.objectId(), 0, 10, 5);
        assertEquals(10, first.beforeMp());
        assertEquals(5, first.afterMp());
        assertEquals(5, first.drained());
        BattleQuestEncounter.ManaDrain equal =
                encounter.drainManaIfStrictlyAbove(red.objectId(), 0, 10, 5);
        assertEquals(5, equal.beforeMp());
        assertEquals(5, equal.afterMp());
        assertEquals(0, equal.drained());
    }

    @Test
    void clearingEachWaveAdvancesDirectlyAndRoomFourUnlocksOnlyFinalTower() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));

        for (int expectedRoom = 1; expectedRoom <= 4; expectedRoom++) {
            List<BattleQuestEncounter.Snapshot> wave = encounter.snapshots(0);
            assertEquals(expectedRoom, encounter.roomNumber(0));
            assertEquals(expectedRoom, wave.get(0).roomNumber());
            BattleQuestEncounter.Hit last = null;
            for (BattleQuestEncounter.Snapshot monster : wave) {
                last = encounter.damage(monster.objectId(), 0, monster.maxHp());
            }
            assertTrue(last.waveCleared());
            assertEquals(expectedRoom, last.clearedRoom());
            assertEquals(expectedRoom == 4, last.finalTowerUnlocked());
            if (expectedRoom < 4) {
                assertEquals(expectedRoom + 1, encounter.roomNumber(0));
                assertFalse(last.spawned().isEmpty());
                assertFalse(encounter.finalTowerUnlocked(0));
            } else {
                assertTrue(last.spawned().isEmpty());
                assertNotNull(encounter.finalTowerPoint(0));
                assertNull(encounter.destroyFinalTower(1));
                BattleQuestEncounter.FinalTowerCompletion completion =
                        encounter.destroyFinalTower(0);
                assertEquals(4, completion.completedRoom());
                assertFalse(encounter.finalTowerUnlocked(0));
                assertFalse(encounter.finalTowerUnlocked(1));
            }
        }
    }

    @Test
    void captureRemovesActorWithoutDeathAndAdvancesOnlyAfterLaneClears() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));

        List<BattleQuestEncounter.Snapshot> turtles = encounter.snapshots(0);
        BattleQuestEncounter.Capture partialRoomOne = encounter.capture(
                turtles.get(0).objectId(), 0);
        assertEquals(turtles.get(0), partialRoomOne.captured());
        assertFalse(partialRoomOne.waveCleared());
        assertTrue(partialRoomOne.spawned().isEmpty());
        BattleQuestEncounter.Capture first = encounter.capture(
                turtles.get(1).objectId(), 0);
        assertEquals(turtles.get(1), first.captured());
        assertTrue(first.waveCleared());
        assertEquals(1, first.clearedRoom());
        assertFalse(first.finalTowerUnlocked());
        assertEquals(7, first.spawned().size());
        assertNull(encounter.find(turtles.get(0).objectId()));
        assertNull(encounter.find(turtles.get(1).objectId()));
        assertNull(encounter.capture(
                encounter.snapshots(1).get(0).objectId(), 0));

        BattleQuestEncounter.Capture partial = encounter.capture(
                first.spawned().get(0).objectId(), 0);
        assertFalse(partial.waveCleared());
        assertTrue(partial.spawned().isEmpty());
        assertEquals(6, encounter.snapshots(0).size());
        BattleQuestEncounter.Capture cleared = null;
        for (BattleQuestEncounter.Snapshot actor : encounter.snapshots(0)) {
            cleared = encounter.capture(actor.objectId(), 0);
        }
        assertNotNull(cleared);
        assertTrue(cleared.waveCleared());
        assertEquals(2, cleared.clearedRoom());
        assertEquals(9, cleared.spawned().size());
    }

    @Test
    void deadQuestActorRemainsAForceScopedConsumableCorpseAcrossWaveAdvance() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        List<BattleQuestEncounter.Snapshot> turtles = encounter.snapshots(0);
        BattleQuestEncounter.Snapshot turtle = turtles.get(1);
        encounter.damage(turtles.get(0).objectId(), 0, turtles.get(0).maxHp());

        BattleQuestEncounter.Hit hit = encounter.damage(
                turtle.objectId(), 0, turtle.maxHp());
        assertTrue(hit.damage().diedNow());
        assertTrue(hit.waveCleared());
        long now = System.nanoTime();
        BattleQuestEncounter.Snapshot corpse = encounter.findCorpse(
                turtle.objectId(), 0, now);
        assertEquals(turtle.objectId(), corpse.objectId());
        assertFalse(corpse.alive());
        assertNull(encounter.findCorpse(turtle.objectId(), 1, now));
        assertEquals(corpse, encounter.consumeCorpse(turtle.objectId(), 0, now));
        assertNull(encounter.consumeCorpse(turtle.objectId(), 0, now));
    }

    @Test
    void returnToNatureLiberatesTamedQuestActorAsFreshWildObject() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot source = encounter.snapshots(0).get(0);
        BattleQuestEncounter.Capture capture =
                encounter.capture(source.objectId(), source.force());
        assertNotNull(capture);
        BattleQuestEncounter.Snapshot captured = capture.captured();
        BattleCompanion tamed = BattleCompanion.tamed(
                0x6810_0145L, captured.npcId(), captured.gfx(), captured.name(),
                captured.level(), Math.max(1, captured.hp() - 1), captured.maxHp(),
                20, 0, 12, 12, captured.x() + 1, captured.y());
        assertTrue(tamed.receiveItem(InventoryCatalog.require(
                InventoryCatalog.EXTRA_HEALING_POTION), 2));

        BattleQuestEncounter.Snapshot released = encounter.releaseTamed(
                captured, tamed.snapshot(), tamed.carriedItems());
        assertNotNull(released);
        assertTrue(released.alive());
        assertEquals(captured.npcId(), released.npcId());
        assertEquals(captured.force(), released.force());
        assertEquals(tamed.snapshot().x(), released.x());
        assertEquals(tamed.snapshot().y(), released.y());
        assertEquals(tamed.snapshot().hp(), released.hp());
        assertTrue(released.objectId() != captured.objectId());
        assertTrue(released.objectId() != tamed.snapshot().objectId());
        assertEquals(released, encounter.find(released.objectId()));
        assertEquals(2, encounter.releasedInventory(released.objectId())
                .catalogCounts().getOrDefault(
                        InventoryCatalog.EXTRA_HEALING_POTION, 0));
    }

    @Test
    void resurrectionRestoresEligibleQuestCorpseAtQuarterHpAcrossWaveAdvance() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        List<BattleQuestEncounter.Snapshot> turtles = encounter.snapshots(0);
        BattleQuestEncounter.Snapshot turtle = turtles.get(1);
        encounter.damage(turtles.get(0).objectId(), 0, turtles.get(0).maxHp());
        encounter.damage(turtle.objectId(), 0, turtle.maxHp());

        BattleQuestEncounter.Resurrection result = encounter.resurrectCorpse(
                turtle.objectId(), 0, System.nanoTime());
        assertNotNull(result);
        assertFalse(result.before().alive());
        assertEquals(turtle.objectId(), result.after().objectId());
        assertTrue(result.after().alive());
        assertEquals(turtle.maxHp() / 4, result.after().hp());
        assertEquals(turtle.objectId(), encounter.find(turtle.objectId()).objectId());
        assertNull(encounter.findCorpse(turtle.objectId(), 0, System.nanoTime()));
        assertNull(encounter.resurrectCorpse(turtle.objectId(), 0, System.nanoTime()));
    }

    @Test
    void callOfNatureRestoresEligibleQuestCorpseAtFullHp() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot turtle = encounter.snapshots(0).get(0);
        encounter.damage(turtle.objectId(), 0, turtle.maxHp());

        BattleQuestEncounter.Resurrection result = encounter.resurrectCorpse(
                turtle.objectId(), 0, System.nanoTime(), 1);
        assertNotNull(result);
        assertFalse(result.before().alive());
        assertTrue(result.after().alive());
        assertEquals(turtle.maxHp(), result.after().hp());
        assertEquals(turtle.maxHp(), result.after().maxHp());
    }

    @Test
    void questDeathKnightCantResurrectSourceFlagIsEnforced() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        for (int room = 1; room < 4; room++) {
            for (BattleQuestEncounter.Snapshot actor : encounter.snapshots(0)) {
                encounter.damage(actor.objectId(), 0, actor.maxHp());
            }
        }
        BattleQuestEncounter.Snapshot deathKnight = encounter.snapshots(0).stream()
                .filter(actor -> actor.npcId() == 45601).findFirst().orElseThrow();
        encounter.damage(deathKnight.objectId(), 0, deathKnight.maxHp());
        assertNull(encounter.resurrectCorpse(
                deathKnight.objectId(), 0, System.nanoTime()));
    }

    @Test
    void questCorpseExpiresAtTheSelectedThreeSecondDeletionBoundary() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot turtle = encounter.snapshots(0).get(0);
        encounter.damage(turtle.objectId(), 0, turtle.maxHp());
        assertNull(encounter.findCorpse(turtle.objectId(), 0, Long.MAX_VALUE));
    }

    @Test
    void questFinalTowerAppearsOnlyAfterFourthRoomAndIsForceOwnedObjective() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Hit clear = null;
        for (int room = 1; room <= 4; room++) {
            for (BattleQuestEncounter.Snapshot actor : encounter.snapshots(0)) {
                clear = encounter.damage(actor.objectId(), 0, actor.maxHp());
            }
            assertEquals(room == 4, clear.finalTowerUnlocked());
        }

        BattleGuardianTowers towers = new BattleGuardianTowers();
        towers.reset(rules(), ArenaMapCatalog.forMap(554));
        assertTrue(towers.snapshots().isEmpty());

        BattleGuardianTower.Snapshot red = towers.revealQuestFinalTower(
                0, encounter.finalTowerPoint(0));
        assertEquals(1, towers.snapshots().size());
        assertEquals(0, red.force());
        assertTrue(towers.isQuestFinalTower(red.objectId()));
        assertTrue(towers.canAttack(red.objectId(), 0));
        assertFalse(towers.canAttack(red.objectId(), 1));
        assertNull(towers.damage(red.objectId(), 1, 10));
        assertEquals(BattleGuardianTower.MAX_HP - 10,
                towers.damage(red.objectId(), 0, 10).damage().after().hp());
        towers.damage(red.objectId(), 0, BattleGuardianTower.MAX_HP);
        assertTrue(towers.deactivateQuestFinalTower(red.objectId()));
        assertTrue(towers.snapshots().isEmpty());
    }

    @Test
    void nativePutObjectCarriesQuestMonsterIdentityAndHp() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);

        Protocol.Reader packet = new Protocol.Reader(Protocol.questMonster(monster));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(monster.x(), packet.h());
        assertEquals(monster.y(), packet.h());
        assertEquals(monster.objectId(), packet.d());
        assertEquals(monster.gfx(), packet.h());
        assertEquals(BattleActionCodes.WALK, packet.c());
        packet.c();
        packet.c();
        assertEquals(BattleTimedStatus.NORMAL, packet.c());
        packet.d();
        packet.h();
        assertEquals(monster.name(), packet.s());
        assertEquals("", packet.s());
        assertEquals(0, packet.c());
        packet.d();
        packet.s();
        packet.s();
        packet.c();
        assertEquals(Protocol.healthRatio(monster.hp(), monster.maxHp()), packet.c());
        packet.c();
        assertEquals(monster.level(), packet.c());
    }

    @Test
    void everyNativeQuestSelectionUsesTheSingleRoomSelectedMapForBothForces() {
        for (int selected : BattleRules.Mode.QUEST_BATTLE.maps()) {
            BattleRules.RuleSet selectedRules = new BattleRules.RuleSet(
                    BattleRules.Mode.QUEST_BATTLE, selected, List.of(selected),
                    600, 1, 2, true, false,
                    "clear-four-monster-rooms+destroy-final-guardian-tower");
            assertEquals(selected, BattleRules.arenaMapForForce(selectedRules, 0));
            assertEquals(selected, BattleRules.arenaMapForForce(selectedRules, 1));
        }
    }

    @Test
    void everyNativeQuestMapKeepsAllFourWavesOnItsNativeStandingFloor() {
        for (int selected : BattleRules.Mode.QUEST_BATTLE.maps()) {
            BattleRules.RuleSet selectedRules = questRules(selected);
            BattleQuestEncounter encounter = new BattleQuestEncounter();
            encounter.reset(selectedRules, ArenaMapCatalog.forMap(selected));
            for (int expectedRoom = 1; expectedRoom <= 4; expectedRoom++) {
                int roomNumber = expectedRoom;
                for (int force = 0; force <= 1; force++) {
                    int mapId = BattleRules.arenaMapForForce(selectedRules, force);
                    ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(mapId);
                    List<BattleQuestEncounter.Snapshot> wave = encounter.snapshots(force);
                    assertFalse(wave.isEmpty(),
                            "missing Quest wave map=" + mapId + " room=" + expectedRoom);
                    assertTrue(wave.stream().allMatch(monster ->
                                    monster.roomNumber() == roomNumber
                                    && arena.nativeStandingFloor(monster.x(), monster.y())),
                            "Quest wave left native floor map=" + mapId
                                    + " room=" + expectedRoom + " wave=" + wave);
                    for (BattleQuestEncounter.Snapshot monster : wave) {
                        BattleQuestEncounter.Hit hit = encounter.damage(
                                monster.objectId(), force, monster.maxHp());
                        assertTrue(hit.damage().diedNow());
                    }
                    if (BattleQuestEncounter.usesPerRoomSymbols(selected)) {
                        assertNotNull(encounter.destroyObjective(force));
                    } else if (expectedRoom == 4) {
                        assertNotNull(encounter.destroyFinalTower(force));
                    }
                }
            }
        }
    }

    @Test
    void aiMovesOnlyOntoConservativeFloorAndHonorsNativeActionDuration() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());

        BattleQuestEncounter.Snapshot moved = encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), 0);
        assertEquals(destination.x(), moved.x());
        assertEquals(destination.y(), moved.y());
        assertNull(encounter.attackForAi(monster.objectId(), 0));
        assertEquals(monster.objectId(), encounter.attackForAi(
                monster.objectId(), TimeUnit.SECONDS.toNanos(10)).objectId());
        assertNull(encounter.moveForAi(monster.objectId(),
                arena.minX() - 1, arena.minY() - 1,
                TimeUnit.SECONDS.toNanos(20)));
    }

    @Test
    void hasteShortensQuestNpcAuthoritativeMoveAndAttackIntervals() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());
        int moveMillis = BattleActionInterval.npcMillis(
                monster.gfx(), BattleActionCodes.WALK, true);
        int attackMillis = BattleActionInterval.npcMillis(
                monster.gfx(), BattleActionCodes.ATTACK, true);

        assertTrue(encounter.haste(monster.objectId(), 0, 0, 1_200));
        assertNotNull(encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), 0));
        assertNull(encounter.attackForAi(
                monster.objectId(), TimeUnit.MILLISECONDS.toNanos(moveMillis) - 1));
        assertNotNull(encounter.attackForAi(
                monster.objectId(), TimeUnit.MILLISECONDS.toNanos(moveMillis)));
        long attackReady = TimeUnit.MILLISECONDS.toNanos(moveMillis + attackMillis);
        assertNull(encounter.attackForAi(monster.objectId(), attackReady - 1));
        assertNotNull(encounter.attackForAi(monster.objectId(), attackReady));
    }

    @Test
    void darknessSupportsThirtyTwoSecondLeaseAndKeepsAdjacentAttackAvailable() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());

        assertFalse(encounter.blind(monster.objectId(), 1, 0, 32));
        assertTrue(encounter.blind(monster.objectId(), 0, 0, 32));
        assertTrue(encounter.blindActive(
                monster.objectId(), TimeUnit.SECONDS.toNanos(31)));
        assertNull(encounter.moveForAi(monster.objectId(), destination.x(), destination.y(),
                TimeUnit.SECONDS.toNanos(1)));
        assertEquals(monster.objectId(), encounter.attackForAi(
                monster.objectId(), TimeUnit.SECONDS.toNanos(1)).objectId());
        assertFalse(encounter.blindActive(
                monster.objectId(), TimeUnit.SECONDS.toNanos(32)));
        assertEquals(destination.x(), encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(),
                TimeUnit.SECONDS.toNanos(33)).x());
    }

    @Test
    void sleepBlocksQuestMovementAndAttackUntilPositiveDamageWakesTarget() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());

        assertFalse(encounter.sleep(monster.objectId(), 1, 0, 32));
        assertTrue(encounter.sleep(monster.objectId(), 0, 0, 32));
        assertTrue(encounter.sleepActive(monster.objectId(), 1));
        assertNull(encounter.moveForAi(monster.objectId(), destination.x(), destination.y(), 1));
        assertNull(encounter.attackForAi(monster.objectId(), 1));
        assertFalse(encounter.damage(monster.objectId(), 0, 1).damage().diedNow());
        assertFalse(encounter.sleepActive(monster.objectId(), 2));
        assertEquals(destination.x(), encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), 2).x());
    }

    @Test
    void freezeBlocksQuestAiSurvivesDamageAndPublishesOneExactExpiry() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());
        long expiry = TimeUnit.SECONDS.toNanos(16);

        assertFalse(encounter.freeze(monster.objectId(), 1, 0, 16));
        assertTrue(encounter.freeze(monster.objectId(), 0, 0, 16));
        assertFalse(encounter.freeze(monster.objectId(), 0, 1, 16));
        assertTrue(encounter.freezeActive(monster.objectId(), 1));
        assertNull(encounter.moveForAi(monster.objectId(),
                destination.x(), destination.y(), 1));
        assertNull(encounter.attackForAi(monster.objectId(), 1));
        assertFalse(encounter.damage(monster.objectId(), 0, 1).damage().diedNow());
        assertTrue(encounter.freezeActive(monster.objectId(), 2));
        assertTrue(encounter.expireFreezes(expiry - 1).isEmpty());
        assertEquals(List.of(monster.objectId()), encounter.expireFreezes(expiry)
                .stream().map(BattleQuestEncounter.Snapshot::objectId).toList());
        assertTrue(encounter.expireFreezes(expiry).isEmpty());
        assertEquals(destination.x(), encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), expiry).x());
    }

    @Test
    void curseParalysisImmediatelyBlocksQuestAiAndPublishesOneExactExpiry() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());
        long start = TimeUnit.SECONDS.toNanos(3);
        long expiry = start + TimeUnit.SECONDS.toNanos(16);

        assertFalse(encounter.paralysis(monster.objectId(), 1, start, 16_000));
        assertTrue(encounter.paralysis(monster.objectId(), 0, start, 16_000));
        assertFalse(encounter.paralysis(monster.objectId(), 0, start + 1, 16_000));
        assertTrue(encounter.paralysisActive(monster.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(monster.objectId(), start + 1));
        assertFalse(encounter.damage(monster.objectId(), 0, 1).damage().diedNow());
        assertTrue(encounter.paralysisActive(monster.objectId(), start + 2));
        assertTrue(encounter.cancelParalysis(monster.objectId()));
        assertFalse(encounter.cancelParalysis(monster.objectId()));
        assertFalse(encounter.paralysisActive(monster.objectId(), start + 3));
        assertEquals(monster.objectId(), encounter.attackForAi(
                monster.objectId(), start + 3).objectId());

        assertTrue(encounter.paralysis(monster.objectId(), 0, start + 4, 16_000));
        assertTrue(encounter.expireParalyses(expiry - 1).isEmpty());
        assertEquals(List.of(monster.objectId()), encounter.expireParalyses(expiry + 4)
                .stream().map(BattleQuestEncounter.Snapshot::objectId).toList());
        assertTrue(encounter.expireParalyses(expiry + 4).isEmpty());
        assertEquals(destination.x(), encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), expiry + 4).x());
    }

    @Test
    void earthBindBlocksQuestAiAndDamageUntilOneExactExpiry() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(arena, monster.x(), monster.y());
        long start = System.nanoTime();
        long expiry = start + TimeUnit.SECONDS.toNanos(16);

        assertFalse(encounter.earthBind(monster.objectId(), 1, start, 16));
        assertTrue(encounter.earthBind(monster.objectId(), 0, start, 16));
        assertFalse(encounter.earthBind(monster.objectId(), 0, start + 1, 16));
        assertTrue(encounter.earthBindActive(monster.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(monster.objectId(), start + 1));
        assertNull(encounter.damage(monster.objectId(), 0, 1).damage());
        assertTrue(encounter.expireEarthBinds(expiry - 1).isEmpty());
        assertEquals(List.of(monster.objectId()), encounter.expireEarthBinds(expiry)
                .stream().map(BattleQuestEncounter.Snapshot::objectId).toList());
        assertTrue(encounter.expireEarthBinds(expiry).isEmpty());
        assertFalse(encounter.damage(monster.objectId(), 0, 1).damage().diedNow());
    }

    @Test
    void shockStunIsForceScopedBlocksQuestAiSurvivesDamageAndCancellation() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(
                arena, monster.x(), monster.y());
        long start = TimeUnit.SECONDS.toNanos(3);
        long expiry = start + TimeUnit.SECONDS.toNanos(4);

        assertFalse(encounter.shockStun(monster.objectId(), 1, start, 4));
        assertTrue(encounter.shockStun(monster.objectId(), 0, start, 4));
        assertTrue(encounter.shockStunActive(monster.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(monster.objectId(), start + 1));
        assertFalse(encounter.damage(monster.objectId(), 0, 1).damage().diedNow());
        assertNotNull(encounter.cancelMagicEffects(monster.objectId(), 0));
        assertTrue(encounter.shockStunActive(monster.objectId(), start + 2));
        assertFalse(encounter.shockStunActive(monster.objectId(), expiry));
        assertEquals(destination.x(), encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), expiry).x());
    }

    @Test
    void thunderGrabIsForceScopedBlocksQuestAiAndCancellationRemovesIt() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(
                arena, monster.x(), monster.y());
        long start = TimeUnit.SECONDS.toNanos(5);

        assertFalse(encounter.thunderGrab(monster.objectId(), 1, start, 4));
        assertTrue(encounter.thunderGrab(monster.objectId(), 0, start, 4));
        assertTrue(encounter.thunderGrabActive(monster.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), start + 1));
        BattleQuestEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(monster.objectId(), 0);
        assertNotNull(cancellation);
        assertTrue(cancellation.thunderGrab());
        assertFalse(encounter.thunderGrabActive(monster.objectId(), start + 2));
    }

    @Test
    void boneBreakIsForceScopedNonRefreshingAndCancellationUnlocksQuestAi() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(554);
        encounter.reset(rules(), arena);
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        ArenaMapCatalog.Point destination = adjacentFloor(
                arena, monster.x(), monster.y());
        long start = TimeUnit.SECONDS.toNanos(7);

        assertFalse(encounter.boneBreak(monster.objectId(), 1, start, 2));
        assertTrue(encounter.boneBreak(monster.objectId(), 0, start, 2));
        assertFalse(encounter.boneBreak(monster.objectId(), 0, start + 1, 2));
        assertTrue(encounter.boneBreakActive(monster.objectId(), start + 1));
        assertNull(encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), start + 1));
        assertNull(encounter.attackForAi(monster.objectId(), start + 1));
        BattleQuestEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(monster.objectId(), 0);
        assertNotNull(cancellation);
        assertTrue(cancellation.boneBreak());
        assertFalse(encounter.boneBreakActive(monster.objectId(), start + 2));
        assertEquals(destination.x(), encounter.moveForAi(
                monster.objectId(), destination.x(), destination.y(), start + 2).x());
    }

    @Test
    void hasteIsForceScopedRefreshableAndPublishesOneExactExpiry() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long start = TimeUnit.SECONDS.toNanos(3);
        long refresh = start + TimeUnit.SECONDS.toNanos(100);
        long firstExpiry = start + TimeUnit.SECONDS.toNanos(1_200);
        long refreshedExpiry = refresh + TimeUnit.SECONDS.toNanos(1_200);

        assertFalse(encounter.haste(monster.objectId(), 1, start, 1_200));
        assertTrue(encounter.haste(monster.objectId(), 0, start, 1_200));
        assertTrue(encounter.hasteActive(monster.objectId(), start + 1));
        assertTrue(encounter.haste(monster.objectId(), 0, refresh, 1_200));
        assertTrue(encounter.expireHastes(firstExpiry).isEmpty());
        assertTrue(encounter.hasteActive(monster.objectId(), firstExpiry));
        assertEquals(List.of(monster.objectId()), encounter.expireHastes(refreshedExpiry)
                .stream().map(BattleQuestEncounter.Snapshot::objectId).toList());
        assertFalse(encounter.hasteActive(monster.objectId(), refreshedExpiry));
        assertTrue(encounter.expireHastes(refreshedExpiry).isEmpty());
    }

    @Test
    void massSlowIsForceScopedNeutralizesHasteAndPublishesOneExpiry() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long start = TimeUnit.SECONDS.toNanos(3);

        assertFalse(encounter.slow(monster.objectId(), 1, start, 64));
        assertTrue(encounter.haste(monster.objectId(), 0, start, 1_200));
        assertTrue(encounter.slow(monster.objectId(), 0, start + 1, 64));
        assertFalse(encounter.hasteActive(monster.objectId(), start + 1));
        assertFalse(encounter.slowActive(monster.objectId(), start + 1));
        assertTrue(encounter.slow(monster.objectId(), 0, start + 2, 64));
        assertTrue(encounter.slowActive(monster.objectId(), start + 2));
        long expiry = start + 2 + TimeUnit.SECONDS.toNanos(64);
        assertTrue(encounter.expireSlows(expiry - 1).isEmpty());
        assertEquals(List.of(monster.objectId()), encounter.expireSlows(expiry)
                .stream().map(BattleQuestEncounter.Snapshot::objectId).toList());
        assertFalse(encounter.slowActive(monster.objectId(), expiry));
        assertTrue(encounter.expireSlows(expiry).isEmpty());

        assertTrue(encounter.slow(monster.objectId(), 0, expiry + 1, 64));
        BattleQuestEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(monster.objectId(), 0);
        assertTrue(cancellation.slow());
        assertFalse(cancellation.haste());
        assertFalse(encounter.slowActive(monster.objectId(), expiry + 2));
    }

    @Test
    void cancellationIsForceScopedAndClearsEveryImplementedQuestNpcMagicEffect() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = TimeUnit.SECONDS.toNanos(3);

        assertTrue(encounter.haste(monster.objectId(), 0, now, 1_200));
        assertTrue(encounter.blind(monster.objectId(), 0, now, 32));
        assertTrue(encounter.weakness(monster.objectId(), 0, now, 64));
        assertTrue(encounter.disease(monster.objectId(), 0, now, 64));
        assertTrue(encounter.silence(monster.objectId(), 0, now, 16));
        assertTrue(encounter.decayPotion(monster.objectId(), 0, now, 16));
        assertTrue(encounter.polluteWater(monster.objectId(), 0, now, 192));
        assertTrue(encounter.sleep(monster.objectId(), 0, now, 32));
        assertTrue(encounter.paralysis(monster.objectId(), 0, now, 16_000));
        assertTrue(encounter.freeze(monster.objectId(), 0, now, 16));
        assertTrue(encounter.earthBind(monster.objectId(), 0, now, 16));
        assertTrue(encounter.windShackle(monster.objectId(), 0, now, 16));
        assertFalse(encounter.windShackle(
                monster.objectId(), 0, now + 1, 16));
        assertNull(encounter.cancelMagicEffects(monster.objectId(), 1));

        BattleQuestEncounter.MagicCancellation result =
                encounter.cancelMagicEffects(monster.objectId(), 0);
        assertNotNull(result);
        assertEquals(monster.objectId(), result.target().objectId());
        assertTrue(result.haste());
        assertFalse(result.slow());
        assertTrue(result.blind());
        assertTrue(result.weakness());
        assertTrue(result.disease());
        assertTrue(result.silence());
        assertTrue(result.decayPotion());
        assertTrue(result.polluteWater());
        assertTrue(result.sleep());
        assertTrue(result.paralysis());
        assertTrue(result.freeze());
        assertTrue(result.earthBind());
        assertTrue(result.windShackle());
        assertFalse(encounter.hasteActive(monster.objectId(), now + 1));
        assertFalse(encounter.blindActive(monster.objectId(), now + 1));
        assertFalse(encounter.weaknessActive(monster.objectId(), now + 1));
        assertFalse(encounter.diseaseActive(monster.objectId(), now + 1));
        assertFalse(encounter.silenceActive(monster.objectId(), now + 1));
        assertFalse(encounter.decayPotionActive(monster.objectId(), now + 1));
        assertFalse(encounter.polluteWaterActive(monster.objectId(), now + 1));
        assertFalse(encounter.sleepActive(monster.objectId(), now + 1));
        assertFalse(encounter.paralysisActive(monster.objectId(), now + 1));
        assertFalse(encounter.freezeActive(monster.objectId(), now + 1));
        assertFalse(encounter.earthBindActive(monster.objectId(), now + 1));
        assertFalse(encounter.windShackleActive(monster.objectId(), now + 1));
    }

    @Test
    void questNpcSilenceIsConsumedOnlyByItsNextSpellAttempt() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.silence(monster.objectId(), 0, now, 16));
        assertTrue(encounter.silenceActive(monster.objectId(), now + 1));
        assertTrue(encounter.consumeSilenceForSpell(monster.objectId(), now + 1));
        assertFalse(encounter.silenceActive(monster.objectId(), now + 1));
        assertFalse(encounter.consumeSilenceForSpell(monster.objectId(), now + 1));
    }

    @Test
    void questNpcAreaSilenceRejectsEverySpellAttemptForItsFullLease() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertTrue(encounter.areaSilence(monster.objectId(), 0, now, 16));
        assertTrue(encounter.consumeSilenceForSpell(monster.objectId(), now + 1));
        assertTrue(encounter.consumeSilenceForSpell(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(15)));
        assertTrue(encounter.silenceActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(16) - 1));
        assertFalse(encounter.consumeSilenceForSpell(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(16)));
        assertFalse(encounter.silenceActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(16)));
    }

    @Test
    void questNpcDecayPotionIsForceScopedAndDoesNotRefresh() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertFalse(encounter.decayPotion(monster.objectId(), 1, now, 16));
        assertTrue(encounter.decayPotion(monster.objectId(), 0, now, 16));
        assertTrue(encounter.decayPotion(monster.objectId(), 0,
                now + TimeUnit.SECONDS.toNanos(5), 16));
        assertTrue(encounter.decayPotionActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(15)));
        assertFalse(encounter.decayPotionActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(16)));
    }

    @Test
    void questNpcPolluteWaterIsForceScopedRefreshesAndExpires() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertFalse(encounter.polluteWater(monster.objectId(), 1, now, 192));
        assertTrue(encounter.polluteWater(monster.objectId(), 0, now, 192));
        assertTrue(encounter.polluteWater(monster.objectId(), 0,
                now + TimeUnit.SECONDS.toNanos(100), 192));
        assertTrue(encounter.polluteWaterActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(291)));
        assertFalse(encounter.polluteWaterActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(292)));
    }

    @Test
    void questNpcStrikerGaleIsForceScopedRefreshesExpiresAndCancels() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long now = TimeUnit.SECONDS.toNanos(3);
        assertFalse(encounter.strikerGale(monster.objectId(), 1, now, 192));
        assertTrue(encounter.strikerGale(monster.objectId(), 0, now, 192));
        assertTrue(encounter.strikerGale(monster.objectId(), 0,
                now + TimeUnit.SECONDS.toNanos(100), 192));
        assertTrue(encounter.strikerGaleActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(291)));
        BattleQuestEncounter.MagicCancellation cancellation =
                encounter.cancelMagicEffects(monster.objectId(), 0);
        assertNotNull(cancellation);
        assertTrue(cancellation.strikerGale());
        assertFalse(encounter.strikerGaleActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(291)));
        assertTrue(encounter.strikerGale(monster.objectId(), 0, now, 192));
        assertFalse(encounter.strikerGaleActive(monster.objectId(),
                now + TimeUnit.SECONDS.toNanos(192)));
    }

    @Test
    void retainedDamagePoisonTicksThroughEarthBindExpiresAndCanBeCured() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        BattleQuestEncounter.Snapshot monster = encounter.snapshots(0).get(0);
        long start = System.nanoTime();

        assertFalse(encounter.poison(monster.objectId(), 1, 15L,
                start, 30, 3_000));
        assertTrue(encounter.poison(monster.objectId(), 0, 15L,
                start, 30, 3_000));
        assertFalse(encounter.poison(monster.objectId(), 0, 16L,
                start + 1, 30, 3_000));
        assertTrue(encounter.earthBind(monster.objectId(), 0, start + 1, 16));
        assertNull(encounter.damage(monster.objectId(), 0, 1).damage());

        List<BattleQuestEncounter.PoisonUpdate> firstTick = encounter.pollPoisons(
                start + TimeUnit.SECONDS.toNanos(3), 3_000, 5);
        assertEquals(1, firstTick.size());
        assertEquals(15L, firstTick.get(0).attackerObjectId());
        assertEquals(1, firstTick.get(0).hits().size());
        assertEquals(monster.maxHp() - 5,
                firstTick.get(0).hits().get(0).damage().after().hp());
        assertTrue(encounter.poisonActive(monster.objectId(),
                start + TimeUnit.SECONDS.toNanos(29)));

        List<BattleQuestEncounter.PoisonUpdate> expired = encounter.pollPoisons(
                start + TimeUnit.SECONDS.toNanos(30), 3_000, 5);
        assertEquals(1, expired.size());
        assertTrue(expired.get(0).expired());
        assertTrue(expired.get(0).hits().isEmpty());
        assertFalse(encounter.poisonActive(monster.objectId(),
                start + TimeUnit.SECONDS.toNanos(30)));

        long secondStart = start + TimeUnit.SECONDS.toNanos(31);
        assertTrue(encounter.poison(monster.objectId(), 0, 15L,
                secondStart, 30, 3_000));
        assertTrue(encounter.curePoison(monster.objectId()));
        assertFalse(encounter.curePoison(monster.objectId()));
        assertTrue(encounter.pollPoisons(
                secondStart + TimeUnit.SECONDS.toNanos(3), 3_000, 5).isEmpty());
    }

    @Test
    void retainedDamagePoisonKillAdvancesDirectlyToNextQuestRoom() {
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        List<BattleQuestEncounter.Snapshot> turtles = encounter.snapshots(0);
        BattleQuestEncounter.Snapshot monster = turtles.get(1);
        encounter.damage(turtles.get(0).objectId(), 0, turtles.get(0).maxHp());
        BattleQuestEncounter.Hit setup = encounter.damage(
                monster.objectId(), 0, monster.maxHp() - 5);
        assertFalse(setup.damage().diedNow());
        long start = System.nanoTime();
        assertTrue(encounter.poison(monster.objectId(), 0, 15L,
                start, 30, 3_000));

        BattleQuestEncounter.PoisonUpdate update = encounter.pollPoisons(
                start + TimeUnit.SECONDS.toNanos(3), 3_000, 5).get(0);
        BattleQuestEncounter.Hit lethal = update.hits().get(0);
        assertTrue(lethal.damage().diedNow());
        assertTrue(lethal.waveCleared());
        assertEquals(1, lethal.clearedRoom());
        assertFalse(lethal.finalTowerUnlocked());
        assertEquals(7, lethal.spawned().size());
        assertEquals(2, encounter.roomNumber(0));
        assertFalse(encounter.poisonActive(monster.objectId(),
                start + TimeUnit.SECONDS.toNanos(3)));
    }

    private static List<Integer> npcIds(
            List<BattleQuestEncounter.Template> wave) {
        return wave.stream().map(BattleQuestEncounter.Template::npcId).toList();
    }

    private static BattleQuestEncounter encounterAtRoom(int roomNumber) {
        if (roomNumber < 1 || roomNumber > 4) {
            throw new IllegalArgumentException("invalid Quest room");
        }
        BattleQuestEncounter encounter = new BattleQuestEncounter();
        encounter.reset(rules(), ArenaMapCatalog.forMap(554));
        for (int room = 1; room < roomNumber; room++) {
            for (BattleQuestEncounter.Snapshot actor : encounter.snapshots(0)) {
                encounter.damage(actor.objectId(), 0, actor.maxHp());
            }
        }
        assertEquals(roomNumber, encounter.roomNumber(0));
        return encounter;
    }

    private static BattleQuestEncounter.Snapshot actor(
            BattleQuestEncounter encounter, int npcId) {
        return encounter.snapshots(0).stream()
                .filter(candidate -> candidate.npcId() == npcId)
                .findFirst().orElseThrow();
    }

    private static ArenaMapCatalog.Point adjacentFloor(
            ArenaMapCatalog.Arena arena, int x, int y) {
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < 8; heading++) {
            int candidateX = x + dx[heading];
            int candidateY = y + dy[heading];
            if (arena.nativeCanStep(x, y, candidateX, candidateY)) {
                return new ArenaMapCatalog.Point(candidateX, candidateY);
            }
        }
        throw new AssertionError("native safe anchor has no adjacent legal step");
    }

    private static BattleRules.RuleSet rules() {
        return BattleRules.decode(new GameHub.RoomSettings(
                "quest", "", 8, BattleRules.Mode.QUEST_BATTLE.wireId(),
                0, 0, 0, 0, List.of()));
    }

    private static BattleRules.RuleSet questRules(int mapId) {
        return new BattleRules.RuleSet(
                BattleRules.Mode.QUEST_BATTLE, mapId, List.of(mapId),
                600, 1, 2, true, false,
                "clear-four-monster-rooms+destroy-final-guardian-tower");
    }
}
