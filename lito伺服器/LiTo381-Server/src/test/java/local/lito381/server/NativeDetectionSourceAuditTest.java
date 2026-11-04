package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill13 Detection and its currently imported world boundary. */
final class NativeDetectionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void selectedRowNativeProducerAndRetainedRevealStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (13, 0, '無所遁形術', 2, 4, 15, 0, 0, 0, 500, 0, 'none', 3, 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 16, '$1445', 19, 749, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (13, '無所遁形術', '40495,40499,40505,88', '10,8,3,1');"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("loc_4CBBD4:");
        int producerEnd = assembly.indexOf("loc_4CBBED:", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("cases 1,2,12,13"));
        assertTrue(producer.contains("and     ecx, 7"));
        assertTrue(producer.contains("sar     esi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset off_5028CC"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("case DETECTION:// 無所遁形術"));
        assertTrue(skillUse.contains("checkEquipped(20015)"));
        assertTrue(skillUse.contains("this._mpConsume = this._mpConsume >> 1;"));
        assertTrue(skillUse.contains("if (this._skillId == DETECTION)"));
        assertTrue(skillUse.contains("HIDDEN_STATUS_SINK"));
        assertTrue(skillUse.contains("npc.appearOnGround(this._player);"));
        assertTrue(skillUse.contains("World.get().getVisiblePlayer(pc)"));
        assertTrue(skillUse.contains("!tgt.isGmInvis() && tgt.isInvisble()"));
        assertTrue(skillUse.contains("tgt.delInvis();"));
        assertTrue(skillUse.contains("WorldTrap.get().onDetection(pc);"));
        assertFalse(BattleAbsoluteBarrierRules.blocksIncomingSpell(
                true, BattleSpellCatalog.DETECTION));
    }

    @Test
    void retainedTrapEffectAndSelectedIvoryTowerNoEffectBoundaryStayExact()
            throws Exception {
        String worldTrap = Files.readString(
                RETAINED.resolve("world/WorldTrap.java"), StandardCharsets.UTF_8);
        assertTrue(worldTrap.contains("loc.isInScreen(trap.getLocation())"));
        assertTrue(worldTrap.contains("trap.tryDetection(pc);"));

        String trap = Files.readString(
                RETAINED.resolve("templates/L1Trap.java"), StandardCharsets.UTF_8);
        assertTrue(trap.contains("if (this._isDetectionable)"));
        assertTrue(trap.contains("new S_EffectLocation("));
        assertFalse(trap.substring(trap.indexOf("public void onDetection("),
                trap.indexOf("// TODO", trap.indexOf("public void onDetection(")))
                .contains("new S_Trap("));

        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        for (int trapId = 24; trapId <= 37; trapId++) {
            String prefix = "INSERT INTO `trap` VALUES (" + trapId + ",";
            int start = database.indexOf(prefix);
            int end = database.indexOf('\n', start);
            assertTrue(start >= 0 && end > start);
            String row = database.substring(start, end);
            assertTrue(row.contains("'L1MonsterTrap', 0, 0,"), row);
        }
        long ivorySpawns = database.lines()
                .filter(line -> line.startsWith("INSERT INTO `spawnlist_trap` VALUES ("))
                .filter(line -> line.matches(".*?, (78|79|80|81|82), .*"))
                .count();
        assertEquals(14, ivorySpawns);
    }

    @Test
    void bothDispatchersCommitCastEffectAndRevealWithoutDamage() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        assertEquals(3, occurrences(session,
                "BattleSpellCatalog.DetectionSpell "));
        assertEquals(2, occurrences(session,
                "useDetectionSpell(payload,"));

        int methodStart = session.indexOf("private void useDetectionSpell(");
        int methodEnd = session.indexOf(
                "private void useClassicWorldCounterDetectionSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("InventoryCatalog.STRENGTH_MAGIC_HELM"));
        assertTrue(method.contains("Protocol.objectAction("));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains("spell.effectGfx()"));
        assertTrue(method.contains("Protocol.manaPoint("));
        assertTrue(method.contains("target.cancelInvisibilityState()"));
        assertTrue(method.contains("target.publishInvisibilityReveal("));
        assertTrue(method.contains("applyClassicWorldDetectionContext("));
        assertTrue(method.contains("hidden-sink-reveals="));
        assertTrue(method.contains("trap-activations="));
        assertTrue(method.contains("trap-effects="));
        assertTrue(method.contains("damage-targets=0 counter-magic-exempt=true"));
        assertFalse(method.contains("Protocol.attackMany("));
        assertFalse(method.contains("takePlayerDamage("));
        assertFalse(method.contains("takeClassicWorldPlayerDamage("));
        assertFalse(method.contains("consumeCounterMagic("));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
