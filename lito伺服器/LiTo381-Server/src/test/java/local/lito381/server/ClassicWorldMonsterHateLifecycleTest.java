package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Retained L1NpcInstance combat/drop hate-list lifetime. */
final class ClassicWorldMonsterHateLifecycleTest {
    private static final int MAP_ID = 4;
    private static final long PLAYER_ONE = 0x4200_0775L;
    private static final long PLAYER_TWO = 0x4200_0776L;
    private static final long SUMMON = 0x6000_0775L;

    @Test
    void acceptedDamageImmediatelySelectsMaximumHateAndFallsBackAfterInvalidation() {
        Fixture fixture = fixture();
        int firstAttackBonus = fixture.actor().maximumHp() / 10;

        assertNotNull(fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_ONE, 2));
        assertEquals(PLAYER_ONE, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        int secondDamage = firstAttackBonus + 3;
        assertNotNull(fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_TWO, secondDamage));
        assertEquals(PLAYER_TWO, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        assertNotNull(fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_ONE, 4));
        assertEquals(PLAYER_ONE, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        fixture.population().clearTarget(
                MAP_ID, fixture.actor().objectId(), PLAYER_ONE);
        assertEquals(PLAYER_TWO, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        ClassicWorldMonsterPopulation.Hit death = fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), Integer.MAX_VALUE);
        assertTrue(death.diedNow());
        assertEquals(List.of(
                new ClassicWorldMonsterPopulation.Contribution(
                        PLAYER_ONE, firstAttackBonus + 6L),
                new ClassicWorldMonsterPopulation.Contribution(
                        PLAYER_TWO, secondDamage)), death.contributions());
    }

    @Test
    void departingTargetIsRemovedFromCombatAndDropHate() {
        Fixture fixture = fixture();
        int firstAttackBonus = fixture.actor().maximumHp() / 10;
        fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_ONE, 1);
        fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_TWO,
                firstAttackBonus + 2);
        assertEquals(PLAYER_TWO, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        fixture.population().clearTargetsForPlayer(PLAYER_TWO);
        assertEquals(PLAYER_ONE, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        ClassicWorldMonsterPopulation.Hit death = fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), Integer.MAX_VALUE);
        assertEquals(List.of(new ClassicWorldMonsterPopulation.Contribution(
                        PLAYER_ONE, firstAttackBonus + 1L)),
                death.contributions());
    }

    @Test
    void petOrSummonHateTransfersToOwnerForCombatAndDrop() {
        Fixture fixture = fixture();
        int firstAttackBonus = fixture.actor().maximumHp() / 10;
        fixture.population().damage(
                MAP_ID, fixture.actor().objectId(),
                ClassicWorldMonsterPopulation.ContributorKind.SUMMON,
                SUMMON, PLAYER_ONE, 2);
        fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_TWO,
                firstAttackBonus + 3);
        assertEquals(PLAYER_TWO, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        fixture.population().transferTargets(SUMMON, PLAYER_ONE);
        fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), PLAYER_ONE, 4);
        assertEquals(PLAYER_ONE, fixture.population().targetObjectId(
                MAP_ID, fixture.actor().objectId()));

        ClassicWorldMonsterPopulation.Hit death = fixture.population().damage(
                MAP_ID, fixture.actor().objectId(), Integer.MAX_VALUE);
        assertEquals(List.of(
                new ClassicWorldMonsterPopulation.Contribution(
                        PLAYER_TWO, firstAttackBonus + 3L),
                new ClassicWorldMonsterPopulation.Contribution(
                        PLAYER_ONE, firstAttackBonus + 6L)),
                death.contributions());
    }

    private static Fixture fixture() {
        ClassicWorldMonsterPopulation population =
                new ClassicWorldMonsterPopulation();
        ClassicWorldMonsterPopulation.Snapshot actor = population.nearest(
                MAP_ID, ClassicWorldMapCatalog.WORLD_TREE_X,
                ClassicWorldMapCatalog.WORLD_TREE_Y, 45270);
        assertNotNull(actor);
        return new Fixture(population, actor);
    }

    private record Fixture(
            ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot actor) {}
}
