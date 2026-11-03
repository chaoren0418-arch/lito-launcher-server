package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterMagicSkillCatalogTest {
    @Test
    void map4KeepsEverySelectedSourceType2MobskillAndPopulation() {
        List<ClassicWorldMonsterMagicSkillCatalog.Definition> definitions =
                ClassicWorldMonsterMagicSkillCatalog.definitions();
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.MAP4_MAGIC_NPC_TYPES,
                definitions.size());
        int population = ClassicWorldMonsterCatalog.definitions(4).stream()
                .filter(spawn -> ClassicWorldMonsterMagicSkillCatalog.forNpc(
                        spawn.template().npcId()) != null)
                .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count)
                .sum();
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.MAP4_MAGIC_POPULATION,
                population);

        ClassicWorldMonsterMagicSkillCatalog.Definition rabbit =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45049);
        ClassicWorldMonsterMagicSkillCatalog.Definition mage =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45224);
        ClassicWorldMonsterMagicSkillCatalog.Definition darkElf =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45265);
        assertNotNull(rabbit);
        assertNotNull(mage);
        assertNotNull(darkElf);
        assertEquals(BattleSpellCatalog.CONE_OF_COLD, rabbit.skillId());
        assertEquals(5, rabbit.leverage());
        assertEquals(1040, rabbit.actionDelayMillis());
        assertEquals(BattleSpellCatalog.CONE_OF_COLD, mage.skillId());
        assertEquals(10, mage.effectiveLeverage());
        assertEquals(1920, mage.actionDelayMillis());
        assertEquals(BattleSpellCatalog.TORNADO, darkElf.skillId());
        assertEquals(15, darkElf.effectiveLeverage());
        assertEquals(4, darkElf.areaRadius());
        assertEquals(1920, darkElf.actionDelayMillis());
    }

    @Test
    void retainedSignedDistanceAndStrictTriRndComparisonsAreExact() {
        ClassicWorldMonsterMagicSkillCatalog.Definition ice =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45224);
        assertTrue(ice.triggerDistance(0));
        assertTrue(ice.triggerDistance(6));
        assertFalse(ice.triggerDistance(7));
        assertTrue(ice.triggerRoll(1));
        assertTrue(ice.triggerRoll(99));
        assertFalse(ice.triggerRoll(100));

        ClassicWorldMonsterMagicSkillCatalog.Definition tornado =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45265);
        assertTrue(tornado.triggerDistance(4));
        assertFalse(tornado.triggerDistance(5));
        assertTrue(tornado.triggerRoll(79));
        assertFalse(tornado.triggerRoll(80));
        assertNull(ClassicWorldMonsterMagicSkillCatalog.forNpc(45009));
    }

    @Test
    void ckewKeepsAllSixteenType2RowsAndExactCustomSkillColumns() {
        List<ClassicWorldMonsterMagicSkillCatalog.Definition> definitions =
                ClassicWorldMonsterMagicSkillCatalog.ckewDefinitions();
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.CKEW_MAGIC_ROWS,
                definitions.size());
        assertEquals(ClassicWorldMonsterMagicSkillCatalog.CKEW_MAGIC_NPC_TYPES,
                definitions.stream().map(
                        ClassicWorldMonsterMagicSkillCatalog.Definition::npcId)
                        .distinct().count());

        for (int npcId = 87007; npcId <= 87010; npcId++) {
            List<ClassicWorldMonsterMagicSkillCatalog.Definition> rows =
                    ClassicWorldMonsterMagicSkillCatalog.forNpcAll(npcId);
            assertEquals(2, rows.size());
            ClassicWorldMonsterMagicSkillCatalog.Definition wave = rows.get(0);
            assertEquals(0, wave.actNo());
            assertEquals(30, wave.triggerChance());
            assertEquals(-6, wave.triggerRange());
            assertEquals(10038, wave.skillId());
            assertEquals(5, wave.mpCost());
            assertEquals(0, wave.reuseDelayMillis());
            assertEquals(100, wave.baseDamage());
            assertEquals(10, wave.diceCount());
            assertEquals(10, wave.dieSides());
            assertEquals(BattleSpellCatalog.ATTRIBUTE_EARTH, wave.attribute());
            assertEquals(4, wave.areaRadius());
            assertEquals(2510, wave.effectGfx());
            assertEquals(1760, wave.actionDelayMillis());
            assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED,
                    wave.targeting());

            ClassicWorldMonsterMagicSkillCatalog.Definition fireball = rows.get(1);
            assertEquals(1, fireball.actNo());
            assertEquals(BattleSpellCatalog.BURNING_FIREBALL, fireball.skillId());
            assertEquals(50, fireball.leverage());
            assertEquals(20, fireball.mpCost());
            assertEquals(1000, fireball.reuseDelayMillis());
            assertEquals(0, fireball.baseDamage());
            assertEquals(5, fireball.diceCount());
            assertEquals(9, fireball.dieSides());
            assertEquals(6, fireball.maximumRange());
            assertEquals(3, fireball.areaRadius());
            assertEquals(171, fireball.effectGfx());
            assertEquals(880, fireball.actionDelayMillis());
            assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.TARGET_CENTERED,
                    fireball.targeting());
        }

        for (int npcId = 87015; npcId <= 87018; npcId++) {
            List<ClassicWorldMonsterMagicSkillCatalog.Definition> rows =
                    ClassicWorldMonsterMagicSkillCatalog.forNpcAll(npcId);
            assertEquals(2, rows.size());
            ClassicWorldMonsterMagicSkillCatalog.Definition darkWave = rows.get(0);
            assertEquals(10010, darkWave.skillId());
            assertEquals(-15, darkWave.triggerRange());
            assertEquals(3, darkWave.areaRadius());
            assertEquals(1240, darkWave.actionDelayMillis());
            assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.CASTER_CENTERED,
                    darkWave.targeting());
            ClassicWorldMonsterMagicSkillCatalog.Definition arrow = rows.get(1);
            assertEquals(10061, arrow.skillId());
            assertEquals(-8, arrow.triggerRange());
            assertEquals(8, arrow.maximumRange());
            assertEquals(3, arrow.areaRadius());
            assertEquals(2546, arrow.effectGfx());
            assertEquals(1600, arrow.actionDelayMillis());
            assertEquals(ClassicWorldMonsterMagicSkillCatalog.Targeting.TARGET_CENTERED,
                    arrow.targeting());
        }
    }
}
