package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldCastleWarFieldTest {
    @Test
    void retainsAllEightExactWarRectanglesAndPerimeterCounts() {
        assertEquals(new ClassicWorldCastleWarField.Area(
                1, 33089, 33219, 32717, 32827, 4),
                ClassicWorldCastleWarField.area(1));
        assertEquals(new ClassicWorldCastleWarField.Area(
                6, 32755, 32870, 32790, 32920, 66),
                ClassicWorldCastleWarField.area(6));
        assertEquals(new ClassicWorldCastleWarField.Area(
                8, 32888, 33070, 32839, 32953, 320),
                ClassicWorldCastleWarField.area(8));
        assertEquals(List.of(62, 52, 66, 68, 76, 64, 82, 76),
                java.util.stream.IntStream.rangeClosed(1, 8)
                        .map(ClassicWorldCastleWarField::flagCount)
                        .boxed().toList());
        assertEquals(546, java.util.stream.IntStream.rangeClosed(1, 8)
                .map(ClassicWorldCastleWarField::flagCount).sum());
    }

    @Test
    void beginReplacesTrebuchetsAndEndRestoresThatCastlesTower() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        ClassicWorldCastleWarField.Transition kent = field.begin("Sun", 1);
        assertEquals(4 + 62, kent.spawned().size());
        assertEquals(4, kent.spawned().stream().filter(object ->
                object.kind() == ClassicWorldCastleWarField.Kind.TREBUCHET).count());
        assertEquals(2, kent.spawned().stream().filter(object ->
                object.npcId() == ClassicWorldCastleWarField.DEFENCE_TREBUCHET_NPC_ID)
                .count());
        assertEquals(2, kent.spawned().stream().filter(object ->
                object.npcId() == ClassicWorldCastleWarField.ATTACK_TREBUCHET_NPC_ID)
                .count());

        List<Long> firstTrebuchets = kent.spawned().stream()
                .filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TREBUCHET)
                .map(ClassicWorldCastleWarField.WarObject::objectId).toList();
        ClassicWorldCastleWarField.Transition dwarf = field.begin("SUN", 6);
        assertEquals(4, dwarf.removed().stream().filter(object ->
                object.kind() == ClassicWorldCastleWarField.Kind.TREBUCHET).count());
        List<Long> replacementTrebuchets = dwarf.spawned().stream()
                .filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TREBUCHET)
                .map(ClassicWorldCastleWarField.WarObject::objectId).toList();
        assertNotEquals(firstTrebuchets, replacementTrebuchets);
        assertEquals(62 + 64 + 4, field.objects("sun").size());

        ClassicWorldCastleWarField.Transition ended = field.end("Sun", 6);
        assertEquals(64, ended.removed().size());
        assertTrue(ended.removed().stream().allMatch(object ->
                object.kind() == ClassicWorldCastleWarField.Kind.FLAG
                        && object.castleId() == 6));
        assertEquals(1, ended.spawned().size());
        assertEquals(ClassicWorldCastleWarField.Kind.TOWER,
                ended.spawned().get(0).kind());
        assertEquals(6, ended.spawned().get(0).castleId());
        assertEquals(66, ended.spawned().get(0).mapId());
        assertEquals(62 + 4 + 1, field.objects("Sun").size());
    }

    @Test
    void retainsAllTwelveExactNativeTowerDefinitionsPerSource() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        ClassicWorldCastleWarField.Transition first = field.ensureTowers("Sun");
        assertEquals(12, first.spawned().size());
        assertEquals(List.of(1, 1, 1, 1, 1, 1, 5, 1),
                java.util.stream.IntStream.rangeClosed(1, 8)
                        .map(ClassicWorldCastleWarField::towerCount)
                        .boxed().toList());

        ClassicWorldCastleWarField.WarObject kent = first.spawned().stream()
                .filter(object -> object.castleId() == 1).findFirst().orElseThrow();
        assertEquals(81111, kent.npcId());
        assertEquals(1672, kent.gfxId());
        assertEquals("$1435", kent.nameId());
        assertEquals(33139, kent.x());
        assertEquals(32768, kent.y());
        assertEquals(4, kent.mapId());
        assertEquals(10_000, kent.maximumHp());

        List<ClassicWorldCastleWarField.WarObject> aden = first.spawned().stream()
                .filter(object -> object.castleId() == 7).toList();
        assertEquals(List.of(81189, 81190, 81191, 81192, 81193),
                aden.stream().map(ClassicWorldCastleWarField.WarObject::npcId).toList());
        assertEquals(List.of(10_000, 3_000, 3_000, 3_000, 3_000),
                aden.stream().map(ClassicWorldCastleWarField.WarObject::maximumHp).toList());
        assertTrue(field.ensureTowers("SUN").spawned().isEmpty());
        assertEquals(12, field.objects("sun").size());
        assertEquals(12, field.ensureTowers("Moon").spawned().size());
        assertEquals(12, field.objects("Moon").size());
    }

    @Test
    void nativeTowerCracksDiesSpawnsCrownAndClaimRestoresTower() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        ClassicWorldCastleWarField.WarObject tower = field.ensureTowers("Sun")
                .spawned().stream().filter(object -> object.castleId() == 1)
                .findFirst().orElseThrow();

        ClassicWorldCastleWarField.TowerHit crack1 = field.damageTower(
                "sun", 4, tower.objectId(), 2_501);
        assertEquals(7_499, crack1.after().currentHp());
        assertEquals(32, crack1.action());
        ClassicWorldCastleWarField.TowerHit crack2 = field.damageTower(
                "SUN", 4, tower.objectId(), 2_500);
        assertEquals(4_999, crack2.after().currentHp());
        assertEquals(33, crack2.action());
        ClassicWorldCastleWarField.TowerHit crack3 = field.damageTower(
                "Sun", 4, tower.objectId(), 2_500);
        assertEquals(2_499, crack3.after().currentHp());
        assertEquals(34, crack3.action());
        ClassicWorldCastleWarField.TowerHit death = field.damageTower(
                "Sun", 4, tower.objectId(), 99_999);
        assertTrue(death.diedNow());
        assertEquals(35, death.action());
        assertEquals(ClassicWorldCastleWarField.Kind.CROWN,
                death.spawnedCrown().kind());
        assertEquals(81125, death.spawnedCrown().npcId());
        assertEquals(1482, death.spawnedCrown().gfxId());
        assertEquals("$413", death.spawnedCrown().nameId());

        ClassicWorldCastleWarField.Transition claim = field.claimCrown(
                "Sun", 4, death.spawnedCrown().objectId());
        assertEquals(2, claim.removed().size());
        assertEquals(1, claim.spawned().size());
        assertEquals(10_000, claim.spawned().get(0).currentHp());
        assertNotEquals(tower.objectId(), claim.spawned().get(0).objectId());
    }

    @Test
    void trebuchetUsesExactMonsterTemplateAndOrdinaryDeathLifecycle() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        ClassicWorldCastleWarField.WarObject trebuchet = field.begin("Sun", 1)
                .spawned().stream().filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TREBUCHET
                        && object.npcId()
                            == ClassicWorldCastleWarField.ATTACK_TREBUCHET_NPC_ID)
                .findFirst().orElseThrow();

        ClassicWorldMonsterPopulation.Snapshot combat =
                field.trebuchetCombatSnapshot("SUN", 4, trebuchet.objectId());
        assertEquals("L1Monster", combat.template().implementation());
        assertEquals(70, combat.template().level());
        assertEquals(30_000, combat.maximumHp());
        assertEquals(20, combat.maximumMp());
        assertEquals(-10, combat.template().armorClass());
        assertTrue(combat.template().large());

        ClassicWorldCastleWarField.TrebuchetHit first = field.damageTrebuchet(
                "sun", 4, trebuchet.objectId(), 1_234);
        assertEquals(1_234, first.appliedDamage());
        assertEquals(28_766, first.after().currentHp());
        assertTrue(!first.diedNow());

        ClassicWorldCastleWarField.TrebuchetHit death = field.damageTrebuchet(
                "Sun", 4, trebuchet.objectId(), 99_999);
        assertTrue(death.diedNow());
        assertEquals(0, death.after().currentHp());
        assertEquals(null, field.trebuchetCombatSnapshot(
                "Sun", 4, trebuchet.objectId()));
        assertEquals(death.after(), field.removeTrebuchetCorpse(
                "Sun", 4, trebuchet.objectId()));
        assertEquals(null, field.object("Sun", 4, trebuchet.objectId()));
        assertEquals(null, field.removeTrebuchetCorpse(
                "Sun", 4, trebuchet.objectId()));
    }

    @Test
    void trebuchetOwnsStrictMpAndNativeMonsterStatusLeases() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        ClassicWorldCastleWarField.WarObject trebuchet = field.begin("Sun", 1)
                .spawned().stream().filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TREBUCHET)
                .findFirst().orElseThrow();
        long now = 1_000_000L;

        ClassicWorldMonsterPopulation.ManaDrain equal =
                field.drainTrebuchetManaIfStrictlyAbove(
                        "Sun", 4, trebuchet.objectId(), 20);
        assertEquals(0, equal.actualDrain(),
                "retained Mind Break requires current MP > requested drain");
        assertEquals(20, equal.after().currentMp());
        ClassicWorldMonsterPopulation.ManaDrain accepted =
                field.drainTrebuchetManaIfStrictlyAbove(
                        "Sun", 4, trebuchet.objectId(), 5);
        assertEquals(5, accepted.actualDrain());
        assertEquals(15, accepted.after().currentMp());

        assertTrue(field.confusionSilenceTrebuchet(
                "Sun", 4, trebuchet.objectId(), now, 8));
        assertTrue(!field.confusionSilenceTrebuchet(
                "Sun", 4, trebuchet.objectId(), now + 1, 8));
        assertTrue(field.trebuchetSilenceActive(
                "Sun", 4, trebuchet.objectId(), now + 1));
        assertTrue(field.thunderGrabTrebuchet(
                "Sun", 4, trebuchet.objectId(), now, 4));
        assertTrue(field.trebuchetThunderGrabActive(
                "Sun", 4, trebuchet.objectId(), now + 1));
        assertTrue(field.boneBreakTrebuchet(
                "Sun", 4, trebuchet.objectId(), now, 2));
        assertTrue(field.trebuchetBoneBreakActive(
                "Sun", 4, trebuchet.objectId(), now + 1));
        BattleArmBreakerStatus.Activation arm = field.armBreakerTrebuchet(
                "Sun", 4, trebuchet.objectId(), now,
                BattleArmBreakerStatus.DURATION_SECONDS);
        assertTrue(arm.accepted());
        assertTrue(!arm.alreadyActive());
        assertTrue(field.trebuchetArmBreakerActive(
                "Sun", 4, trebuchet.objectId(), now + 1));

        ClassicWorldCastleWarField.Transition replacement = field.begin("Sun", 1);
        assertEquals(null, field.trebuchetCombatSnapshot(
                "Sun", 4, trebuchet.objectId()));
        ClassicWorldCastleWarField.WarObject fresh = replacement.spawned().stream()
                .filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TREBUCHET)
                .findFirst().orElseThrow();
        assertEquals(20, field.trebuchetCombatSnapshot(
                "Sun", 4, fresh.objectId()).currentMp());
        assertTrue(!field.trebuchetSilenceActive(
                "Sun", 4, fresh.objectId(), now + 1));
        assertTrue(!field.trebuchetThunderGrabActive(
                "Sun", 4, fresh.objectId(), now + 1));
        assertTrue(!field.trebuchetBoneBreakActive(
                "Sun", 4, fresh.objectId(), now + 1));
        assertTrue(!field.trebuchetArmBreakerActive(
                "Sun", 4, fresh.objectId(), now + 1));
    }

    @Test
    void adenMainTowerCannotBeDamagedUntilAllFourSubTowersDie() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        List<ClassicWorldCastleWarField.WarObject> aden = field.ensureTowers("Sun")
                .spawned().stream().filter(object -> object.castleId() == 7).toList();
        ClassicWorldCastleWarField.WarObject main = aden.stream()
                .filter(object -> object.npcId() == 81189).findFirst().orElseThrow();
        ClassicWorldCastleWarField.TowerHit blocked = field.damageTower(
                "Sun", 4, main.objectId(), 10_000);
        assertEquals(0, blocked.appliedDamage());
        assertEquals(10_000, blocked.after().currentHp());

        for (ClassicWorldCastleWarField.WarObject sub : aden.stream()
                .filter(object -> object.npcId() >= 81190).toList()) {
            ClassicWorldCastleWarField.TowerHit death = field.damageTower(
                    "Sun", 4, sub.objectId(), 3_000);
            assertTrue(death.diedNow());
            assertEquals(35, death.action());
            assertEquals(null, death.spawnedCrown());
        }
        ClassicWorldCastleWarField.TowerHit mainDeath = field.damageTower(
                "Sun", 4, main.objectId(), 10_000);
        assertTrue(mainDeath.diedNow());
        assertEquals(81125, mainDeath.spawnedCrown().npcId());
    }

    @Test
    void retainsExactTownAndDiadReturnSets() {
        assertEquals(List.of(
                new ClassicWorldCastleWarField.Point(33048, 32750, 4),
                new ClassicWorldCastleWarField.Point(33059, 32768, 4),
                new ClassicWorldCastleWarField.Point(33047, 32761, 4),
                new ClassicWorldCastleWarField.Point(33059, 32759, 4),
                new ClassicWorldCastleWarField.Point(33051, 32775, 4),
                new ClassicWorldCastleWarField.Point(33048, 32778, 4),
                new ClassicWorldCastleWarField.Point(33064, 32773, 4),
                new ClassicWorldCastleWarField.Point(33057, 32748, 4)),
                ClassicWorldCastleWarField.returnPoints(1));
        assertEquals(List.of(
                new ClassicWorldCastleWarField.Point(32792, 32807, 310),
                new ClassicWorldCastleWarField.Point(32816, 32820, 310),
                new ClassicWorldCastleWarField.Point(32823, 32797, 310)),
                ClassicWorldCastleWarField.returnPoints(8));
        assertEquals(8, ClassicWorldCastleWarField.castleIdAt(
                320, 33033, 32895));
        assertEquals(0, ClassicWorldCastleWarField.castleIdAt(
                4, 33033, 32895));
    }

    @Test
    void serializesRetainedFieldAndMonsterActorIdentity() {
        ClassicWorldCastleWarField field = new ClassicWorldCastleWarField();
        ClassicWorldCastleWarField.Transition transition = field.begin("Sun", 1);
        ClassicWorldCastleWarField.WarObject flag = transition.spawned().stream()
                .filter(object -> object.kind() == ClassicWorldCastleWarField.Kind.FLAG)
                .findFirst().orElseThrow();
        ClassicWorldCastleWarField.WarObject trebuchet = transition.spawned().stream()
                .filter(object -> object.kind()
                        == ClassicWorldCastleWarField.Kind.TREBUCHET)
                .findFirst().orElseThrow();

        assertActor(flag, 0);
        assertActor(trebuchet, ClassicWorldCastleWarField.TREBUCHET_LEVEL);
    }

    private static void assertActor(
            ClassicWorldCastleWarField.WarObject object, int expectedLevel) {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.classicCastleWarObject(object));
        assertEquals(Protocol.S_PUT_OBJECT, packet.c());
        assertEquals(object.x(), packet.h());
        assertEquals(object.y(), packet.h());
        assertEquals(object.objectId(), packet.d());
        assertEquals(object.gfxId(), packet.h());
        assertEquals(0, packet.c());
        assertEquals(0, packet.c());
        assertEquals(0, packet.c());
        assertEquals(BattleTimedStatus.NORMAL, packet.c());
        assertEquals(0, packet.d());
        assertEquals(0, packet.h());
        assertEquals(object.nameId(), packet.s());
        assertEquals("", packet.s());
        assertEquals(0, packet.c());
        assertEquals(0, packet.d());
        assertEquals("", packet.s());
        assertEquals("", packet.s());
        assertEquals(0, packet.c());
        assertEquals(0xFF, packet.c());
        assertEquals(0, packet.c());
        assertEquals(expectedLevel, packet.c());
    }
}
