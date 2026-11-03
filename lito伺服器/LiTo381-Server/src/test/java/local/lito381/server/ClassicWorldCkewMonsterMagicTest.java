package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldCkewMonsterMagicTest {
    private static final int MAP_ID = ClassicWorldCkewCatalog.MAP_ID;
    private static final long OBJECT_BASE = 0x6600_0000L;
    private static final long RELEASED_BASE = 0x6600_1000L;

    @Test
    void fighterUsesIndependentRowsPursuitChanceRuleMpAndActionClocks() {
        ClassicWorldMonsterPopulation closePopulation = population();
        ClassicWorldMonsterPopulation.Snapshot close = fighter(closePopulation);
        ClassicWorldMonsterPopulation.GridPoint distance2 =
                targetAtDistance(close, 2);
        assertNotNull(distance2);
        long targetId = 0x7000_0001L;
        long now = 10_000_000_000L;
        assertTrue(closePopulation.provoke(MAP_ID, close.objectId(), targetId));
        assertNull(closePopulation.magicAttack(
                MAP_ID, close.objectId(), targetId,
                distance2.x(), distance2.y(), now, 1, 30, 1));
        ClassicWorldMonsterPopulation.MagicAttack strict =
                closePopulation.magicAttack(
                        MAP_ID, close.objectId(), targetId,
                        distance2.x(), distance2.y(), now, 1, 29, 1);
        assertNotNull(strict);
        assertEquals(10038, strict.skill().skillId());
        assertTrue(strict.triggerChanceApplied());
        assertEquals(29, strict.triggerRoll());
        assertEquals(195, strict.attacker().currentMp());

        ClassicWorldMonsterPopulation wavePopulation = population();
        ClassicWorldMonsterPopulation.Snapshot waveFighter = fighter(wavePopulation);
        ClassicWorldMonsterPopulation.GridPoint distance4 =
                targetAtDistance(waveFighter, 4);
        assertNotNull(distance4);
        assertTrue(wavePopulation.provoke(
                MAP_ID, waveFighter.objectId(), targetId));
        ClassicWorldMonsterPopulation.MagicAttack wave =
                wavePopulation.magicAttack(
                        MAP_ID, waveFighter.objectId(), targetId,
                        distance4.x(), distance4.y(), now,
                        2, 100, 1);
        assertNotNull(wave);
        assertEquals(10038, wave.skill().skillId());
        assertFalse(wave.triggerChanceApplied());
        assertEquals(0, wave.triggerRoll());
        assertFalse(wavePopulation.actionReady(MAP_ID, waveFighter.objectId(),
                now + 1_759_999_999L));
        assertTrue(wavePopulation.actionReady(MAP_ID, waveFighter.objectId(),
                now + 1_760_000_000L));

        ClassicWorldMonsterPopulation firePopulation = population();
        ClassicWorldMonsterPopulation.Snapshot fireFighter = fighter(firePopulation);
        ClassicWorldMonsterPopulation.GridPoint distance6 =
                targetAtDistance(fireFighter, 6);
        assertNotNull(distance6);
        assertTrue(firePopulation.provoke(
                MAP_ID, fireFighter.objectId(), targetId));
        ClassicWorldMonsterPopulation.MagicAttack fireball =
                firePopulation.magicAttack(
                        MAP_ID, fireFighter.objectId(), targetId,
                        distance6.x(), distance6.y(), now,
                        2, 100, 2);
        assertNotNull(fireball);
        assertEquals(BattleSpellCatalog.BURNING_FIREBALL,
                fireball.skill().skillId());
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.TARGET_CENTERED,
                fireball.skill().targeting());
        assertEquals(180, fireball.attacker().currentMp());
        assertFalse(firePopulation.actionReady(MAP_ID, fireFighter.objectId(),
                now + 879_999_999L));
        assertTrue(firePopulation.actionReady(MAP_ID, fireFighter.objectId(),
                now + 880_000_000L));
        assertNull(firePopulation.magicAttack(
                MAP_ID, fireFighter.objectId(), targetId,
                distance6.x(), distance6.y(), now + 880_000_000L,
                2, 100, 2));
        assertNotNull(firePopulation.magicAttack(
                MAP_ID, fireFighter.objectId(), targetId,
                distance6.x(), distance6.y(), now + 1_000_000_000L,
                2, 100, 2));
    }

    @Test
    void chinoRejectsEmptyCasterAreaAndSupportsBothCustomAreaCenters() {
        long targetId = 0x7000_0002L;
        long now = 20_000_000_000L;

        ClassicWorldMonsterPopulation emptyAreaPopulation = chinoPopulation();
        ClassicWorldMonsterPopulation.Snapshot farChino = chino(emptyAreaPopulation);
        ClassicWorldMonsterPopulation.GridPoint distance4 =
                targetAtDistance(farChino, 4);
        assertNotNull(distance4);
        assertTrue(emptyAreaPopulation.provoke(
                MAP_ID, farChino.objectId(), targetId));
        assertNull(emptyAreaPopulation.magicAttack(
                MAP_ID, farChino.objectId(), targetId,
                distance4.x(), distance4.y(), now,
                2, 100, 1));
        ClassicWorldMonsterPopulation.Snapshot unchanged =
                emptyAreaPopulation.actor(MAP_ID, farChino.objectId());
        assertNotNull(unchanged);
        assertEquals(500, unchanged.currentMp());
        assertTrue(emptyAreaPopulation.actionReady(
                MAP_ID, farChino.objectId(), now));

        ClassicWorldMonsterPopulation wavePopulation = chinoPopulation();
        ClassicWorldMonsterPopulation.Snapshot waveChino = chino(wavePopulation);
        ClassicWorldMonsterPopulation.GridPoint distance3 =
                targetAtDistance(waveChino, 3);
        assertNotNull(distance3);
        assertTrue(wavePopulation.provoke(MAP_ID, waveChino.objectId(), targetId));
        ClassicWorldMonsterPopulation.MagicAttack wave =
                wavePopulation.magicAttack(
                        MAP_ID, waveChino.objectId(), targetId,
                        distance3.x(), distance3.y(), now,
                        2, 100, 1);
        assertNotNull(wave);
        assertEquals(10010, wave.skill().skillId());
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED,
                wave.skill().targeting());
        assertEquals(495, wave.attacker().currentMp());
        assertFalse(wavePopulation.actionReady(MAP_ID, waveChino.objectId(),
                now + 1_239_999_999L));
        assertTrue(wavePopulation.actionReady(MAP_ID, waveChino.objectId(),
                now + 1_240_000_000L));

        ClassicWorldMonsterPopulation arrowPopulation = chinoPopulation();
        ClassicWorldMonsterPopulation.Snapshot arrowChino = chino(arrowPopulation);
        ClassicWorldMonsterPopulation.GridPoint distance8 =
                targetAtDistance(arrowChino, 8);
        assertNotNull(distance8);
        assertTrue(arrowPopulation.provoke(
                MAP_ID, arrowChino.objectId(), targetId));
        ClassicWorldMonsterPopulation.MagicAttack arrow =
                arrowPopulation.magicAttack(
                        MAP_ID, arrowChino.objectId(), targetId,
                        distance8.x(), distance8.y(), now,
                        2, 100, 2);
        assertNotNull(arrow);
        assertEquals(10061, arrow.skill().skillId());
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.TARGET_CENTERED,
                arrow.skill().targeting());
        assertEquals(495, arrow.attacker().currentMp());
        assertFalse(arrowPopulation.actionReady(MAP_ID, arrowChino.objectId(),
                now + 1_599_999_999L));
        assertTrue(arrowPopulation.actionReady(MAP_ID, arrowChino.objectId(),
                now + 1_600_000_000L));
    }

    @Test
    void customDamageRowsUseOneHundredPlusTenD10BeforeLeverage() {
        ClassicWorldMonsterMagicSkillCatalog.Definition wave =
                ClassicWorldMonsterMagicSkillCatalog.forNpcAll(87015).get(0);
        assertEquals(110, ClassicWorldMonsterMagicDamage.rollSourceDamage(
                wave, sides -> 1));
        assertEquals(200, ClassicWorldMonsterMagicDamage.rollSourceDamage(
                wave, sides -> sides));
        ClassicWorldMonsterMagicSkillCatalog.Definition fireball =
                ClassicWorldMonsterMagicSkillCatalog.forNpcAll(87007).get(1);
        assertEquals(5, ClassicWorldMonsterMagicDamage.rollSourceDamage(
                fireball, sides -> 1));
        assertEquals(45, ClassicWorldMonsterMagicDamage.rollSourceDamage(
                fireball, sides -> sides));
    }

    @Test
    void unifiedFighterPoolPreservesActOrderAcrossType2AndType1() {
        long targetId = 0x7000_0003L;
        long now = 30_000_000_000L;

        ClassicWorldMonsterPopulation firstPopulation = population();
        ClassicWorldMonsterPopulation.Snapshot first = fighter(firstPopulation);
        ClassicWorldMonsterPopulation.GridPoint firstTarget =
                targetAtDistance(first, 2);
        assertNotNull(firstTarget);
        assertTrue(firstPopulation.provoke(MAP_ID, first.objectId(), targetId));
        ClassicWorldMonsterPopulation.CkewSkillAttack firstChoice =
                firstPopulation.ckewSkillAttack(
                        MAP_ID, first.objectId(), targetId,
                        firstTarget.x(), firstTarget.y(), now,
                        java.util.Set.of(firstTarget), 1, 29, 1);
        assertNotNull(firstChoice);
        assertNull(firstChoice.physical());
        assertEquals(10038, firstChoice.magic().skill().skillId());
        assertEquals(0, firstChoice.magic().skill().actNo());

        ClassicWorldMonsterPopulation secondPopulation = population();
        ClassicWorldMonsterPopulation.Snapshot second = fighter(secondPopulation);
        ClassicWorldMonsterPopulation.GridPoint secondTarget =
                targetAtDistance(second, 2);
        assertNotNull(secondTarget);
        assertTrue(secondPopulation.provoke(MAP_ID, second.objectId(), targetId));
        ClassicWorldMonsterPopulation.CkewSkillAttack secondChoice =
                secondPopulation.ckewSkillAttack(
                        MAP_ID, second.objectId(), targetId,
                        secondTarget.x(), secondTarget.y(), now,
                        java.util.Set.of(secondTarget), 1, 29, 2);
        assertNotNull(secondChoice);
        assertNull(secondChoice.physical());
        assertEquals(BattleSpellCatalog.BURNING_FIREBALL,
                secondChoice.magic().skill().skillId());
        assertEquals(1, secondChoice.magic().skill().actNo());

        ClassicWorldMonsterPopulation thirdPopulation = population();
        ClassicWorldMonsterPopulation.Snapshot third = fighter(thirdPopulation);
        ClassicWorldMonsterPopulation.GridPoint thirdTarget =
                targetAtDistance(third, 2);
        assertNotNull(thirdTarget);
        assertTrue(thirdPopulation.provoke(MAP_ID, third.objectId(), targetId));
        ClassicWorldMonsterPopulation.CkewSkillAttack thirdChoice =
                thirdPopulation.ckewSkillAttack(
                        MAP_ID, third.objectId(), targetId,
                        thirdTarget.x(), thirdTarget.y(), now,
                        java.util.Set.of(thirdTarget), 1, 29, 3);
        assertNotNull(thirdChoice);
        assertNull(thirdChoice.magic());
        assertEquals(2, thirdChoice.physical().skill().actNo());
        assertTrue(thirdChoice.physical().triggerChanceApplied());
        assertEquals(29, thirdChoice.physical().triggerRoll());
    }

    @Test
    void entangleDoublesUnifiedCkewMagicAndPhysicalActionClocks() {
        long targetId = 0x7000_0005L;
        long now = 35_000_000_000L;

        ClassicWorldMonsterPopulation magicPopulation = population();
        ClassicWorldMonsterPopulation.Snapshot magicFighter = fighter(magicPopulation);
        ClassicWorldMonsterPopulation.GridPoint magicTarget =
                targetAtDistance(magicFighter, 2);
        assertNotNull(magicTarget);
        assertTrue(magicPopulation.provoke(
                MAP_ID, magicFighter.objectId(), targetId));
        assertTrue(magicPopulation.slow(
                MAP_ID, magicFighter.objectId(), now, 64));
        ClassicWorldMonsterPopulation.CkewSkillAttack magic =
                magicPopulation.ckewSkillAttack(
                        MAP_ID, magicFighter.objectId(), targetId,
                        magicTarget.x(), magicTarget.y(), now,
                        java.util.Set.of(magicTarget), 1, 29, 1);
        assertNotNull(magic);
        assertNotNull(magic.magic());
        long magicDelayNanos = magic.magic().skill().actionDelayMillis()
                * 2L * 1_000_000L;
        assertFalse(magicPopulation.actionReady(
                MAP_ID, magicFighter.objectId(), now + magicDelayNanos - 1));
        assertTrue(magicPopulation.actionReady(
                MAP_ID, magicFighter.objectId(), now + magicDelayNanos));

        ClassicWorldMonsterPopulation physicalPopulation = population();
        ClassicWorldMonsterPopulation.Snapshot physicalFighter =
                fighter(physicalPopulation);
        ClassicWorldMonsterPopulation.GridPoint physicalTarget =
                targetAtDistance(physicalFighter, 2);
        assertNotNull(physicalTarget);
        assertTrue(physicalPopulation.provoke(
                MAP_ID, physicalFighter.objectId(), targetId));
        assertTrue(physicalPopulation.slow(
                MAP_ID, physicalFighter.objectId(), now, 64));
        ClassicWorldMonsterPopulation.CkewSkillAttack physical =
                physicalPopulation.ckewSkillAttack(
                        MAP_ID, physicalFighter.objectId(), targetId,
                        physicalTarget.x(), physicalTarget.y(), now,
                        java.util.Set.of(physicalTarget), 1, 29, 3);
        assertNotNull(physical);
        assertNotNull(physical.physical());
        long physicalDelayNanos = Math.max(
                100, physicalFighter.template().atkspeed())
                * 2L * 1_000_000L;
        assertFalse(physicalPopulation.actionReady(
                MAP_ID, physicalFighter.objectId(), now + physicalDelayNanos - 1));
        assertTrue(physicalPopulation.actionReady(
                MAP_ID, physicalFighter.objectId(), now + physicalDelayNanos));
    }

    @Test
    void unifiedPursuitIgnoresChanceAndSelectedFailureHasNoFallback() {
        long targetId = 0x7000_0004L;
        long now = 40_000_000_000L;

        ClassicWorldMonsterPopulation pursuitPopulation = population();
        ClassicWorldMonsterPopulation.Snapshot pursuit = fighter(pursuitPopulation);
        ClassicWorldMonsterPopulation.GridPoint distance3 =
                targetAtDistance(pursuit, 3);
        assertNotNull(distance3);
        assertTrue(pursuitPopulation.provoke(
                MAP_ID, pursuit.objectId(), targetId));
        ClassicWorldMonsterPopulation.CkewSkillAttack pursuitChoice =
                pursuitPopulation.ckewSkillAttack(
                        MAP_ID, pursuit.objectId(), targetId,
                        distance3.x(), distance3.y(), now,
                        java.util.Set.of(distance3), 2, 100, 3);
        assertNotNull(pursuitChoice);
        assertNotNull(pursuitChoice.physical());
        assertFalse(pursuitChoice.physical().triggerChanceApplied());
        assertEquals(0, pursuitChoice.physical().triggerRoll());

        ClassicWorldMonsterPopulation failurePopulation = population();
        ClassicWorldMonsterPopulation.Snapshot failure = fighter(failurePopulation);
        ClassicWorldMonsterPopulation.GridPoint distance6 =
                targetAtDistance(failure, 6);
        assertNotNull(distance6);
        assertTrue(failurePopulation.provoke(
                MAP_ID, failure.objectId(), targetId));
        assertNull(failurePopulation.ckewSkillAttack(
                MAP_ID, failure.objectId(), targetId,
                distance6.x(), distance6.y(), now,
                java.util.Set.of(distance6), 2, 100, 1));
        ClassicWorldMonsterPopulation.Snapshot unchanged =
                failurePopulation.actor(MAP_ID, failure.objectId());
        assertNotNull(unchanged);
        assertEquals(200, unchanged.currentMp());
        assertTrue(failurePopulation.actionReady(
                MAP_ID, failure.objectId(), now));

        ClassicWorldMonsterPopulation.CkewSkillAttack alternative =
                failurePopulation.ckewSkillAttack(
                        MAP_ID, failure.objectId(), targetId,
                        distance6.x(), distance6.y(), now,
                        java.util.Set.of(distance6), 2, 100, 2);
        assertNotNull(alternative);
        assertEquals(BattleSpellCatalog.BURNING_FIREBALL,
                alternative.magic().skill().skillId());
    }

    private static ClassicWorldMonsterPopulation population() {
        return ClassicWorldMonsterPopulation.questInstance(
                356L, OBJECT_BASE, RELEASED_BASE,
                ClassicWorldCkewMonsterCatalog.definitions());
    }

    private static ClassicWorldMonsterPopulation.Snapshot fighter(
            ClassicWorldMonsterPopulation population) {
        ClassicWorldMonsterPopulation.Snapshot result = population.nearest(
                MAP_ID, 32880, 32900, 87007);
        assertNotNull(result);
        return result;
    }

    private static ClassicWorldMonsterPopulation chinoPopulation() {
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldCkewMonsterCatalog.templateForNpc(87015);
        assertNotNull(template);
        ClassicWorldMonsterPopulation.GridPoint actorCell = openActorTile(8);
        assertNotNull(actorCell);
        ClassicWorldMonsterCatalog.SpawnDefinition definition =
                new ClassicWorldMonsterCatalog.SpawnDefinition(
                        MAP_ID, 900001, "CKEW-Chino-magic-test", 1, 0,
                        actorCell.x(), actorCell.y(), 0, 0,
                        0, 0, 0, 0, 2,
                        0, 0, false, 0, false, 0, template);
        return ClassicWorldMonsterPopulation.questInstance(
                357L, OBJECT_BASE, RELEASED_BASE, List.of(definition));
    }

    private static ClassicWorldMonsterPopulation.Snapshot chino(
            ClassicWorldMonsterPopulation population) {
        ClassicWorldMonsterPopulation.Snapshot result = population.nearest(
                MAP_ID, 32800, 32800, 87015);
        assertNotNull(result);
        return result;
    }

    private static ClassicWorldMonsterPopulation.GridPoint targetAtDistance(
            ClassicWorldMonsterPopulation.Snapshot actor, int distance) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(actor.mapId());
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int heading = 0; heading < dx.length; heading++) {
            int x = actor.x() + dx[heading] * distance;
            int y = actor.y() + dy[heading] * distance;
            if (map.attackPosition(actor.x(), actor.y(), x, y, distance)) {
                return new ClassicWorldMonsterPopulation.GridPoint(x, y);
            }
        }
        return null;
    }

    private static ClassicWorldMonsterPopulation.GridPoint openActorTile(
            int distance) {
        ClassicWorldMapCatalog.ClassicMap map =
                ClassicWorldMapCatalog.forMap(MAP_ID);
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        for (int y = map.startY(); y <= map.endY(); y++) {
            for (int x = map.startX(); x <= map.endX(); x++) {
                if (!map.standingPassable(x, y)) continue;
                for (int heading = 0; heading < dx.length; heading++) {
                    int targetX = x + dx[heading] * distance;
                    int targetY = y + dy[heading] * distance;
                    if (map.attackPosition(x, y, targetX, targetY, distance)) {
                        return new ClassicWorldMonsterPopulation.GridPoint(x, y);
                    }
                }
            }
        }
        return null;
    }
}
