package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ClassicWorldGuardianCatalogTest {
    @Test
    void selectedMapFourPreservesAllFiftyFourFixedGuardianRows() {
        assertEquals(54, ClassicWorldGuardianCatalog.all().size());
        assertEquals(Map.of(
                70846, 10L,
                70848, 19L,
                70850, 10L,
                70851, 14L,
                70852, 1L),
                ClassicWorldGuardianCatalog.all().stream().collect(
                        Collectors.groupingBy(
                                ClassicWorldNpcCatalog.StaticNpc::npcId,
                                Collectors.counting())));

        ClassicWorldNpcCatalog.StaticNpc first =
                ClassicWorldGuardianCatalog.all().get(0);
        assertEquals(796, first.spawnId());
        assertEquals(70846, first.npcId());
        assertEquals(33139, first.x());
        assertEquals(32262, first.y());
        assertEquals(5, first.heading());
        assertEquals(100, first.movementDistance());
        assertEquals(0x6400_031CL, first.objectId());
        assertEquals("$752", first.nameId());
        assertEquals("L1Guardian", first.implementation());

        ClassicWorldNpcCatalog.StaticNpc last =
                ClassicWorldGuardianCatalog.all().get(53);
        assertEquals(851, last.spawnId());
        assertEquals(70852, last.npcId());
        assertEquals(33157, last.x());
        assertEquals(32276, last.y());
        assertEquals(0x6400_0353L, last.objectId());

        assertFalse(ClassicWorldNpcCatalog.occupies(4, first.x(), first.y()),
                "promoted moving guardians must not leave static collision behind");
    }

    @Test
    void exactTemplatesDialogueAndTargetDispositionsMatchSelectedSource() {
        for (ClassicWorldMonsterCatalog.SpawnDefinition definition
                : ClassicWorldGuardianCatalog.definitions()) {
            ClassicWorldMonsterCatalog.MonsterTemplate npc = definition.template();
            assertTrue(npc.guardian());
            assertEquals(30, npc.level());
            assertEquals(3_000, npc.hp());
            assertEquals(300, npc.mp());
            assertEquals(0, npc.armorClass());
            assertEquals(30, npc.strength());
            assertEquals(30, npc.constitution());
            assertEquals(30, npc.dexterity());
            assertEquals(30, npc.wisdom());
            assertEquals(30, npc.intelligence());
            assertEquals(70, npc.magicResistance());
            assertEquals(0, npc.experience());
            assertEquals(0, npc.lawful());
            assertTrue(npc.aggressive());
            assertEquals(480, npc.passispeed());
            assertEquals(840, npc.atkspeed());
        }
        assertEquals("arachnee1",
                ClassicWorldGuardianCatalog.dialogueHtml(70846, -1_000));
        assertEquals("arachnem1",
                ClassicWorldGuardianCatalog.dialogueHtml(70846, -1_001));
        assertEquals("ente1", ClassicWorldGuardianCatalog.dialogueHtml(70848, 0));
        assertEquals("entm1", ClassicWorldGuardianCatalog.dialogueHtml(70848, -9_999));
        assertEquals("pane1", ClassicWorldGuardianCatalog.dialogueHtml(70850, 0));
        assertEquals("panm1", ClassicWorldGuardianCatalog.dialogueHtml(70850, -9_999));
        assertEquals("fairye1", ClassicWorldGuardianCatalog.dialogueHtml(70851, 0));
        assertEquals("fairym1", ClassicWorldGuardianCatalog.dialogueHtml(70851, -9_999));
        assertEquals("fairyqe1", ClassicWorldGuardianCatalog.dialogueHtml(70852, 0));
        assertEquals("fairyqm1", ClassicWorldGuardianCatalog.dialogueHtml(70852, -9_999));

        assertEquals(ClassicWorldGuardianCatalog.TargetDisposition.INTRUDER,
                ClassicWorldGuardianCatalog.disposition(false, false));
        assertEquals(ClassicWorldGuardianCatalog.TargetDisposition.ELF_MURDERER,
                ClassicWorldGuardianCatalog.disposition(true, true));
        assertEquals(ClassicWorldGuardianCatalog.TargetDisposition.NONE,
                ClassicWorldGuardianCatalog.disposition(true, false));
    }

    @Test
    void unarmedElfHarvestKeepsEveryOneBasedBoundaryAndExactMessageArgument() {
        assertHarvest(70846, 30, 40503, 1, "$752", "$769");
        assertNull(ClassicWorldGuardianCatalog.harvest(70846, 31));

        assertHarvest(70848, 10, 40506, 1, "$755", "$794");
        assertHarvest(70848, 11, 40507, 1, "$755", "$763");
        assertHarvest(70848, 60, 40507, 1, "$755", "$763");
        assertHarvest(70848, 61, 40505, 1, "$755", "$770");
        assertHarvest(70848, 70, 40505, 1, "$755", "$770");
        assertNull(ClassicWorldGuardianCatalog.harvest(70848, 71));

        assertHarvest(70850, 30, 40519, 5, "$753", "$760 (5)");
        assertNull(ClassicWorldGuardianCatalog.harvest(70850, 31));
        assertNull(ClassicWorldGuardianCatalog.harvest(70851, 1));
        assertNull(ClassicWorldGuardianCatalog.harvest(70852, 100));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldGuardianCatalog.harvest(70848, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ClassicWorldGuardianCatalog.harvest(99999, 1));
    }

    @Test
    void harvestedItemsAndNativePacketsKeepSelectedRowsAndReceiverWidths() {
        InventoryCatalog.Definition fruit = InventoryCatalog.require(40506);
        assertEquals(0xF9, fruit.useType());
        assertEquals(126, fruit.icon());
        assertEquals(79, fruit.groundGfx());
        assertEquals(InventoryCatalog.Effect.HEAL, fruit.effect());
        assertEquals(127, fruit.itemDescriptionId());
        assertEquals(8_000, fruit.unitWeight());
        assertArrayEquals(new int[] {30, 50}, InventoryCatalog.healingRange(40506));
        assertEquals(197, InventoryCatalog.healingEffectGfx(40506));

        InventoryCatalog.Definition branch = InventoryCatalog.require(40507);
        assertEquals(0, branch.useType());
        assertEquals(309, branch.icon());
        assertEquals(881, branch.groundGfx());
        assertEquals(InventoryCatalog.Effect.ANTIDOTE, branch.effect());
        assertEquals(127, branch.itemDescriptionId());
        assertEquals(1_000, branch.unitWeight());

        long objectId = 0x6400_031CL;
        assertArrayEquals(new Protocol.Writer().c(0x1C).d(objectId).c(2).bytes(),
                Protocol.changeDirection(objectId, 2));
        Protocol.Reader shout = new Protocol.Reader(Protocol.sayCode(
                2, objectId, "<$752> $804", 33139, 32262));
        assertEquals(0x2D, shout.c());
        assertEquals(2, shout.c());
        assertEquals(objectId, shout.d());
        assertEquals("<$752> $804", shout.s());
        assertEquals(33139, shout.h());
        assertEquals(32262, shout.h());
        assertEquals(0, shout.remaining());

        Protocol.Reader wanted = new Protocol.Reader(Protocol.sayCode(
                0, objectId, "$752: $815", 33139, 32262));
        assertEquals(0x2D, wanted.c());
        assertEquals(0, wanted.c());
        assertEquals(objectId, wanted.d());
        assertEquals("$752: $815", wanted.s());
    }

    @Test
    void dynamicPopulationUsesStableStaticIdsSupportsDamageAndTenSecondTalkRest() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION, population.size());
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION
                        + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION + 54
                        + ClassicWorldGuardCatalog.POPULATION,
                population.runtimeActorCount());

        long objectId = ClassicWorldNpcCatalog.objectId(796);
        ClassicWorldMonsterPopulation.Snapshot guardian =
                population.actor(4, objectId);
        assertNotNull(guardian);
        assertEquals(33139, guardian.x());
        assertEquals(32262, guardian.y());
        assertEquals(3_000, guardian.currentHp());
        assertTrue(ClassicWorldGuardianCatalog.isGuardian(guardian));

        ClassicWorldMonsterPopulation.Hit armedOrMagic =
                population.damage(4, objectId, 77, 125);
        assertNotNull(armedOrMagic);
        assertEquals(125, armedOrMagic.appliedDamage());
        assertEquals(2_875, armedOrMagic.after().currentHp());

        long now = 2_000_000_000L;
        ClassicWorldMonsterPopulation.Snapshot faced = population.faceAndRest(
                4, objectId, guardian.x() + 1, guardian.y(), now, 10);
        assertNotNull(faced);
        assertEquals(2, faced.heading());
        assertFalse(population.actionReady(4, objectId, now + 9_999_999_999L));
        assertTrue(population.actionReady(4, objectId, now + 10_000_000_000L));
    }

    private static void assertHarvest(
            int npcId, int roll, int itemId, int quantity,
            String giver, String argument) {
        ClassicWorldGuardianCatalog.Harvest result =
                ClassicWorldGuardianCatalog.harvest(npcId, roll);
        assertNotNull(result);
        assertEquals(itemId, result.itemId());
        assertEquals(quantity, result.quantity());
        assertEquals(giver, result.giverNameId());
        assertEquals(argument, result.itemMessageArgument());
    }
}
