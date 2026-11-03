package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class ClassicWorldMobGroupCatalogTest {
    @Test
    void selectedCatalogRetainsEveryReferencedGroupAndCompleteMinionTemplate() {
        assertEquals(21, ClassicWorldMobGroupCatalog.size());
        assertEquals(28, ClassicWorldMobGroupCatalog.ROW_COUNT);
        assertEquals(449, ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION);

        ClassicWorldMobGroupCatalog.Group orcs =
                ClassicWorldMobGroupCatalog.forId(8);
        assertNotNull(orcs);
        assertFalse(orcs.removeIfLeaderDies());
        assertEquals(45082, orcs.leaderNpcId());
        assertEquals(4, orcs.minionsPerLeader());
        Map<Integer, ClassicWorldMobGroupCatalog.Minion> byNpc = orcs.minions()
                .stream().collect(Collectors.toMap(
                        ClassicWorldMobGroupCatalog.Minion::npcId,
                        Function.identity()));
        assertEquals(2, byNpc.get(45009).count());
        assertEquals(2, byNpc.get(45019).count());
        assertEquals(1880, byNpc.get(45019).template().atkspeed());

        ClassicWorldMobGroupCatalog.Group necromancer =
                ClassicWorldMobGroupCatalog.forId(15);
        assertTrue(necromancer.removeIfLeaderDies());
        assertEquals(6, necromancer.minionsPerLeader());
        assertEquals("$317", necromancer.minions().get(0).template().nameId());
    }

    @Test
    void productionPopulationAppendsOneIndependentRuntimeGroupPerLeader() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION
                        + ClassicWorldMobGroupCatalog.PROJECTED_MINION_POPULATION
                        + ClassicWorldGuardianCatalog.POPULATION
                        + ClassicWorldGuardCatalog.POPULATION,
                population.runtimeActorCount());

        ClassicWorldMonsterPopulation.Snapshot leader =
                population.mobGroupLeaders(4, 8).get(0);
        var members = population.mobGroupMembers(4, leader.objectId());
        assertEquals(5, members.size());
        assertTrue(members.stream().allMatch(member ->
                population.mobGroupLeaderObjectId(4, member.objectId())
                        == leader.objectId()));
        assertEquals(1, members.stream().filter(member ->
                member.objectId() == leader.objectId()).count());
        assertEquals(2, members.stream().filter(member -> member.npcId() == 45009).count());
        assertEquals(2, members.stream().filter(member -> member.npcId() == 45019).count());
    }

    @Test
    void leaderRemovalAndRespawnRetainSelectedDissolveSemantics() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation(21L);
        ClassicWorldMonsterPopulation.Snapshot leader =
                population.mobGroupLeaders(12, 15).get(0);
        var oldMinions = population.mobGroupMembers(12, leader.objectId()).stream()
                .filter(member -> member.objectId() != leader.objectId()).toList();
        assertEquals(6, oldMinions.size());

        var lethal = population.damage(
                12, leader.objectId(), 0x6600_0001L, leader.maximumHp());
        assertTrue(lethal.diedNow());
        assertNotNull(population.removeCorpse(12, leader.objectId()));
        assertTrue(oldMinions.stream().allMatch(member ->
                population.mobGroupLeaderObjectId(12, member.objectId()) == 0));

        ClassicWorldMonsterPopulation.Respawn respawn =
                population.respawn(12, leader.objectId());
        assertNotNull(respawn);
        assertEquals(6, respawn.groupMembers().size());
        assertTrue(respawn.groupMembers().stream().allMatch(member ->
                population.mobGroupLeaderObjectId(12, member.objectId())
                        == leader.objectId()));
        assertEquals(ClassicWorldMonsterCatalog.TOTAL_POPULATION,
                population.size());
    }
}
