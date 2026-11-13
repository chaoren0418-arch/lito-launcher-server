package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * T120 (Issue #339): item bless consistency found out of scope in #318.
 *
 * <ul>
 *   <li>(3) illusion weapons (380_db weapon 祝福狀態=3, 幻象之劍／幻象之弓) must load.</li>
 *   <li>(1) the blessed-weapon d4 follows the weapon template bless like
 *       1-伺服器 L1AttackPc.java:113 (`_weapon.getItem().getBless()`).</li>
 * </ul>
 */
final class ItemBlessConsistencyT120Test {

    // ---------------------------------------------------------------- (3)

    @Test void illusionWeaponTemplateBlessLoadsInsteadOfThrowing() {
        // 380_db weapon rows 幻象之劍／幻象之弓 store 祝福狀態=3 (1-伺服器 BLESS_ILLUSION);
        // nativeBlessingFromSource keeps 3 unchanged.
        int illusion = BattlePhysicalOffense.nativeBlessingFromSource(3);
        assertEquals(3, illusion);
        SourceWeaponCombatTraits traits = assertDoesNotThrow(
                () -> new SourceWeaponCombatTraits(1, illusion, 0, 0, 0,
                        0, 0.0D, 0, false, false, 0));
        assertEquals(3, traits.blessing());
        SourceWeaponPhysicalStats sword = new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 0, 0, 0, 0).withCombatTraits(traits);
        assertEquals(3, sword.attackBlessing());
    }

    @Test void illusionProjectileAndBowLoad() {
        SourceProjectilePhysicalStats arrow = assertDoesNotThrow(
                () -> new SourceProjectilePhysicalStats(
                        0x1234L, 40743, -2, 5, 7, 0, 3, 66, true, 20));
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 4, 0, 0, 15, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(1, 3, 0, 0, 0))
                .withProjectile(arrow);
        assertEquals(3, bow.attackBlessing());
    }

    @Test void illusionWeaponDoesNotRollTheBlessedD4() {
        // 1-伺服器 L1AttackPc.java:1365/1373/1381 only adds the d4 for `_weaponBless == 1`.
        SourceWeaponPhysicalStats illusion = new SourceWeaponPhysicalStats(
                24, 1, 6, 8, 1, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(1, 3, 0, 0, 0));
        for (int undead = 0; undead <= 5; undead++) {
            assertEquals(0, BattlePhysicalOffense.blessingMaximum(illusion, npc(undead)),
                    "undead=" + undead);
        }
    }

    @Test void blessingOutsideTheNativeRangeIsStillRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SourceWeaponCombatTraits(1, 4, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceWeaponCombatTraits(1, -1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceProjectilePhysicalStats(
                        0x1234L, 40743, -2, 5, 7, 0, 4, 66, true, 20));
    }

    // ---------------------------------------------------------------- (1)

    @Test void rentalWeaponUsesTheConvertedTemplateBless() {
        // rental-item-definitions-380.tsv weapon 84 (and all 13 rental weapons) store
        // bless=0, the 1-伺服器 internal "normal" value.
        RentalItemDefinitionCatalog.SourceTemplate source = RentalItemDefinitionCatalog.source(84);
        assertEquals(0, source.integer("bless"));
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(84L, 1));
        RuntimeInventory.Item item = inventory.items().get(0);
        inventory.toggleWeapon(item.objectId());
        SourceWeaponPhysicalStats weapon = inventory.equippedPhysicalWeapon();
        assertEquals(RuntimeInventory.BLESSING_NORMAL, weapon.combatTraits().blessing());
        for (int undead = 0; undead <= 5; undead++) {
            assertEquals(0, BattlePhysicalOffense.blessingMaximum(weapon, npc(undead)),
                    "an ordinary rental weapon must not roll the blessed d4; undead=" + undead);
        }
    }

    @Test void everyRentalWeaponTemplateBlessConvertsToACombatBlessing() {
        for (RentalItemDefinitionCatalog.SourceTemplate source : RentalItemDefinitionCatalog.all()) {
            if (!"weapon".equals(source.kind())) continue;
            int converted = BattlePhysicalOffense.nativeBlessingFromSource(source.integer("bless"));
            assertTrue(converted >= 0 && converted <= 3, "item " + source.definition().catalogId());
        }
    }

    @Test void nativeDeInventoryNoLongerOverwritesCombatBlessWithTheInstanceBless() throws Exception {
        // NativeDeItemInventory.updatePhysical used to rebuild the combat traits from the
        // item instance bless; it must keep the loader's converted template bless.
        String source = Files.readString(
                Path.of("src/main/java/local/lito381/server/NativeDeItemInventory.java"),
                StandardCharsets.UTF_8);
        int start = source.indexOf("private void updatePhysical(");
        assertTrue(start >= 0);
        int end = source.indexOf("public Item remove(", start);
        assertTrue(end > start);
        String body = source.substring(start, end);
        assertFalse(body.contains("new SourceWeaponCombatTraits("), body);
        assertFalse(body.contains(".blessing()"), body);
    }

    private static SourceNpcPhysicalStats npc(int undead) {
        return new SourceNpcPhysicalStats(1, 10, false, 0, false, undead, 0, 0);
    }
}
