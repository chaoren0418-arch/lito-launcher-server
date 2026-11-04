package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill107 and its ordinary-WORLD item-local damage wiring. */
final class NativeWorldShadowFangSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"));
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void retainedDatabaseKeepsTheExactItemLocalContract() throws Exception {
        String database = LocalResources.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (107, 0, '暗影之牙', 14, 2, 30, 0, 40321, 1, 0, 192, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 4, '$2514', 19, 2951, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (107, '暗影之牙', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (107, '暗影之牙');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (107, '暗影之牙', 'DMG', 5);"));

        String status = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleItemWeaponEnchantStatus.java"),
                StandardCharsets.UTF_8);
        assertTrue(status.contains("LinkedHashMap<Long, Lease> leases"));
        assertTrue(status.contains("leases.put(itemObjectId, new Lease("));
        assertTrue(status.contains("Modifier modifier(long nowNanos, long itemObjectId)"));
    }

    @Test
    void worldDispatchCastRoiAndBothPlayerAttackPathsStayConnected()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.ItemWeaponEnchantSpell worldItemWeaponEnchant",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        String worldRoute = session.substring(dispatch, unsupported);
        assertTrue(worldRoute.contains(
                "BattleSpellCatalog.itemWeaponEnchantSpell(selector.skillId())"));
        assertTrue(worldRoute.contains(
                "useItemWeaponEnchantSpell(payload, worldItemWeaponEnchant)"));

        int castStart = session.indexOf("private void useItemWeaponEnchantSpell(");
        int castEnd = session.indexOf("private void useMagicalWeaponSpell(", castStart);
        String cast = session.substring(castStart, castEnd);
        assertTrue(cast.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(cast.contains("String spellName = spell.displayName()"));
        assertTrue(cast.contains(
                "spell.skillId(), spell.actionId(), spellName"));
        assertTrue(cast.contains("phase != originPhase"));
        assertTrue(cast.contains("classicWorldMapId != originMapId"));
        assertTrue(cast.contains("classicWorldInstanceId != originInstanceId"));
        assertTrue(cast.contains("itemWeaponEnchantStatus.activate("));
        assertTrue(cast.contains("commitSpellReady(spell.skillId()"));
        assertTrue(cast.contains("classicWorldMembers(originMapId)"));
        assertTrue(cast.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(cast.contains("phase=\" + originPhase"));

        int monsterStart = session.indexOf("private void attackClassicWorld(");
        int monsterEnd = session.indexOf(
                "private void attackClassicWorldPlayer(", monsterStart);
        String monster = session.substring(monsterStart, monsterEnd);
        assertTrue(monster.contains(
                "itemWeaponEnchantStatus.modifier(\n"
                        + "                        physicalAttackNowNanos, runtimeWeaponObjectId)"));
        assertTrue(monster.contains(
                "equipmentDamage, itemWeaponEnchant.damage(ranged)"));
        assertTrue(monster.contains(
                "config.shadowFangDamageFixtureWeaponRoll() > 0 ? 0"));
        assertTrue(monster.contains("item-weapon-enchant-damage="));

        int playerStart = monsterEnd;
        int playerEnd = session.indexOf(
                "private ClassicWorldProjectileDebit", playerStart);
        String player = session.substring(playerStart, playerEnd);
        assertTrue(player.contains(
                "itemWeaponEnchantStatus.modifier(nowNanos, runtimeWeaponObjectId)"));
        assertTrue(player.contains("itemEnchant.damage(ranged)"));
        assertTrue(player.contains("item-weapon-enchant-damage="));
    }

    @Test
    void cancellationStillHasNoItemWeaponEnchantRemovalPath() throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int cancellation = session.indexOf("private void useCancellationSpell(");
        int nextMethod = session.indexOf("\n    private void ", cancellation + 20);
        String method = session.substring(cancellation, nextMethod);
        assertTrue(!method.contains("itemWeaponEnchantStatus"));
    }
}
