package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill44 WORLD NPC/pet/summon target fidelity. */
final class NativeWorldCancellationSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");
    private static final Path MONSTERS = Path.of(
            "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java");
    private static final Path COMPANION = Path.of(
            "src/main/java/local/lito381/server/BattleCompanion.java");
    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server/model/skill");
    private static final Path CATALOG = Path.of(
            "src/main/java/local/lito381/server/BattleSpellCatalog.java");

    @Test
    void selectedRowAndRetainedTargetGateIncludeEveryNpcFamily()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (44, 0, '魔法相消術', 6, 3, 40, "
                + "0, 40318, 2, 500, 0, 'buff', 3, 0, 0, 0, 0, 3, 8, 1, "
                + "0, -1, 0, 0, 8, '$1464', 19, 870, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (44, '魔法相消術', NULL, NULL);"));
        String secondary = Files.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (44, '魔法相消術', 6, 3, 40, "
                + "0, 40318, 2, 0, 0, 'buff', 3, 0, 0, 0, 0, 3, 8, 1, "
                + "0, -1, 0, 0, 8, '$1464', 19, 870, 0, 0, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (44, '魔法相消術', NULL, NULL);"));
        String skillUse = Files.readString(
                RETAINED.resolve("L1SkillUse.java"), StandardCharsets.UTF_8);
        int gate = skillUse.indexOf("&& ((cha instanceof L1MonsterInstance)");
        String npcGate = skillUse.substring(gate, gate + 360);
        assertTrue(npcGate.contains("cha instanceof L1NpcInstance"));
        assertTrue(npcGate.contains("cha instanceof L1SummonInstance"));
        assertTrue(npcGate.contains("cha instanceof L1PetInstance"));

        String cancellation = Files.readString(
                RETAINED.resolve("skillmode/CANCELLATION.java"),
                StandardCharsets.UTF_8);
        assertTrue(cancellation.contains("cha.curePoison();"));
        assertTrue(cancellation.contains("cha.cureParalaysis();"));
        assertTrue(cancellation.contains("tgnpc.setMoveSpeed(0);"));
        assertTrue(cancellation.contains("tgnpc.setBraveSpeed(0);"));
        assertTrue(cancellation.contains("tgnpc.setWeaponBreakStack(0);"));
        assertTrue(cancellation.contains(
                "new S_SkillHaste(cha.getId(), 0, 0)"));
        assertTrue(cancellation.contains(
                "new S_SkillBrave(cha.getId(), 0, 0)"));
        String normalCast = skillUse.substring(
                skillUse.indexOf("case TYPE_NORMAL ->"),
                skillUse.indexOf("case TYPE_LOGIN ->"));
        assertTrue(normalCast.indexOf("commitMaterialAndAcquireCastLocks()")
                < normalCast.indexOf("breakInterruptedCastEffects()"));
        assertTrue(normalCast.indexOf("breakInterruptedCastEffects()")
                < normalCast.indexOf("breakInvisibilityOnCast()"));

        String catalog = Files.readString(CATALOG, StandardCharsets.UTF_8);
        assertTrue(catalog.contains(
                "CANCELLATION, new CancellationSpell(CANCELLATION, 5, 3, 19, 870,"));
        assertTrue(catalog.contains(
                "40, 500, 3, 280, InventoryCatalog.MAGIC_GEM, 2,"));
    }

    @Test
    void worldDispatcherResolvesControlledAndOrdinaryActorsBeforeStaticRoi()
            throws Exception {
        String source = Files.readString(SESSION, StandardCharsets.UTF_8);
        int start = source.indexOf("private void useWorldRoiCancellation(");
        int end = source.indexOf("private void useWorldNpcCancellation(", start);
        String dispatcher = source.substring(start, end);
        int companions = dispatcher.indexOf("classicWorldCompanionSnapshots()");
        int monsters = dispatcher.indexOf("hub.classicWorldMonster(");
        int ordinary = dispatcher.indexOf("useWorldNpcCancellation(");
        int staticRoi = dispatcher.indexOf("ClassicWorldNpcCatalog.npc(");
        assertTrue(companions >= 0 && monsters > companions
                && ordinary > monsters && staticRoi > ordinary);

        String method = source.substring(end,
                source.indexOf("private void useWorldPlayerCancellation(", end));
        assertTrue(method.contains("controlledCompanion.cancelMagicEffects()"));
        assertTrue(method.contains("hub.cancelClassicWorldMonsterMagic("));
        assertTrue(method.contains("removeCatalog("));
        assertTrue(method.contains("publishNpcMagicCancellation("));
        assertTrue(method.contains("classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("cancelAbsoluteBarrier(\"spell-\" + spell.skillId())"));

        int playerStart = source.indexOf("private void useWorldPlayerCancellation(");
        int battleStart = source.indexOf("private void useCancellationSpell(", playerStart);
        String playerMethod = source.substring(playerStart, battleStart);
        assertTrue(playerMethod.contains(
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())"));
        String battleMethod = source.substring(battleStart,
                source.indexOf("private void publishNpcMagicCancellation(",
                        battleStart));
        assertTrue(battleMethod.contains(
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())"));

        String monstersSource = Files.readString(
                MONSTERS, StandardCharsets.UTF_8);
        assertTrue(monstersSource.contains(
                "synchronized MagicCancellation cancelMagicEffects("));
        assertTrue(monstersSource.contains("actor.damagePoisonStatus.cancelMagicEffects()"));
        assertTrue(monstersSource.contains("actor.weaponBreakStacks = 0;"));
        String companionSource = Files.readString(
                COMPANION, StandardCharsets.UTF_8);
        assertTrue(companionSource.contains(
                "synchronized MagicCancellation cancelMagicEffects()"));
        assertTrue(companionSource.contains("boolean haste = cancelHaste() != 0;"));
        assertTrue(companionSource.contains(
                "boolean eraseMagic = cancelEraseMagic();"));
    }
}
