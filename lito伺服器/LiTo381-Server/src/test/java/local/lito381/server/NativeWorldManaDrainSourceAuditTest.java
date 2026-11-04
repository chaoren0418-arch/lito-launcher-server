package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill39 ordinary-WORLD probability and MP-transfer fidelity. */
final class NativeWorldManaDrainSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/combat/MagicHitCommitPlan.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");
    private static final Path RETAINED = Path.of("../1-伺服器/src/com/lineage/server");

    @Test
    void selectedRowsNativeProducerAndRetainedTransferRuleStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (39, 0, '魔力奪取', 5, 6, 1, 50, "
                + "0, 0, 500, 0, 'buff', 3, 0, 0, 0, 0, 12, 0, 1, 0, 4, "
                + "0, 0, 64, '$1982', 19, 2171, 0, 690, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (39, '魔力奪取', 5);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (39, '魔力奪取', NULL, NULL);"));
        String secondary = LocalResources.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (39, '魔力奪取', 5, 6, 1, 50, "
                + "0, 0, 400, 0, 'buff', 3, 0, 0, 0, 0, 12, 0, 1, 0, 4, "
                + "0, 0, 64, '$1982', 19, 2171, 0, 690, 0, 280);"));
        String npc45065 = database.lines()
                .filter(line -> line.startsWith(
                        "INSERT INTO `npc` VALUES (45065, '人形殭屍', '$57'"))
                .findFirst().orElseThrow();
        assertTrue(npc45065.contains(
                "'L1Monster', 52, 6, 45, 12, 8, 14, 16, 6, 5, 2, 5, 37, -8"));
        SourceNpcPhysicalStats source =
                SourceNpcPhysicalStats.memoryFixture(45065);
        assertEquals(12, source.maxMp());
        assertTrue(source.eraseEligible());
        assertTrue(BattleManaDrain.eligibleNpc(source));

        String assembly = LocalResources.readString(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB8F1:");
        String nativeCase = assembly.substring(dispatch, dispatch + 800);
        assertTrue(nativeCase.contains("jumptable 004CB66E case 38"));
        assertTrue(nativeCase.contains("mov     dword ptr [edi+68h], 26h"));
        assertTrue(nativeCase.contains("mov     edx, 4"));
        assertTrue(nativeCase.contains("call    sub_4CA6E0"));
        String producer = assembly.substring(
                assembly.indexOf("sub_4CA6E0 proc near"),
                assembly.indexOf("sub_4CA6E0 endp"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("offset aCccdhh"));
        byte[] recovered = LocalResources.readAllBytes(Path.of(
                "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
        assertEquals(500, peWordAtVirtualAddress(
                recovered, 0x635774 + 38 * 2));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("this._skillId == MANA_DRAIN"));
        assertTrue(skillUse.contains("random.nextInt(10) + 5"));
        assertTrue(skillUse.contains("chance + (this._user.getInt() / 2)"));
        assertTrue(skillUse.contains("cha.getCurrentMp() < drainMana"));
        assertTrue(skillUse.contains(
                "(this._skillId == MANA_DRAIN) && (isManaDrain == false)"));
        String commit = LocalResources.readString(
                RETAINED.resolve("model/combat/MagicHitCommitPlan.java"),
                StandardCharsets.UTF_8);
        assertTrue(commit.contains(
                "npc.commitManaDamageAfterAcceptedHit(attacker, requestedManaDrain)"));
        assertTrue(commit.contains("attacker.adjustCurrentMp(actual)"));
    }

    @Test
    void worldDispatcherUsesAtomicMonsterMpAndPreservesAdmissionOrder()
            throws Exception {
        String source = LocalResources.readString(SESSION, StandardCharsets.UTF_8);
        int worldStart = source.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = source.indexOf(
                "BattleSpellCatalog.ManaDrainSpell worldManaDrain", worldStart);
        int unsupported = source.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(source.substring(dispatch, unsupported).contains(
                "useClassicWorldManaDrainSpell(payload, worldManaDrain)"));

        int methodStart = source.indexOf(
                "private void useClassicWorldManaDrainSpell(");
        int methodEnd = source.indexOf(
                "private void useManaDrainSpell(", methodStart);
        String method = source.substring(methodStart, methodEnd);
        assertTrue(method.contains("BattleManaDrain.eligibleNpc(npcStats)"));
        assertTrue(method.contains("config.manaDrainFixtureDieRoll()"));
        assertTrue(method.contains("config.manaDrainFixturePercentileRoll()"));
        assertTrue(method.contains("config.manaDrainFixtureTransferRoll()"));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("drainClassicWorldMonsterMana("));
        assertTrue(method.contains("result.actualDrain()"));
        assertTrue(method.contains("provokeClassicWorldMonster("));
        assertTrue(method.contains("playerHp <= spell.hpCost()"));
        assertTrue(method.contains("Math.min(playerMaxMp, playerMp + actualDrain)"));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains("Protocol.messageCode(spell.failureMessage())"));

        int battleStart = source.indexOf("private void useManaDrainSpell(");
        int battleEnd = source.indexOf(
                "private void useBlindSpell(", battleStart);
        String battle = source.substring(battleStart, battleEnd);
        assertTrue(battle.contains(
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId());"));
        assertTrue(battle.contains(
                "revealInvisibility(\"spell-\" + spell.skillId());"));
        assertTrue(battle.contains("config.manaDrainFixtureDieRoll()"));
        assertTrue(battle.contains("config.manaDrainFixturePercentileRoll()"));
        assertTrue(battle.contains("config.manaDrainFixtureTransferRoll()"));

        String population = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains(
                "synchronized ManaDrain drainMana(int mapId, long objectId"));
        assertTrue(population.contains(
                "int actualDrain = Math.min(actor.currentMp, requestedDrain)"));
        assertTrue(population.contains("actor.currentMp -= actualDrain"));
    }

    private static int peWordAtVirtualAddress(byte[] data, int address) {
        ByteBuffer bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pe = bytes.getInt(0x3C);
        int sections = Short.toUnsignedInt(bytes.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(bytes.getShort(pe + 20));
        int optional = pe + 24;
        int imageBase = bytes.getInt(optional + 28);
        int rva = address - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            int virtualSize = bytes.getInt(section + 8);
            int sectionRva = bytes.getInt(section + 12);
            int rawSize = bytes.getInt(section + 16);
            int rawOffset = bytes.getInt(section + 20);
            long upper = Integer.toUnsignedLong(sectionRva)
                    + Math.max(Integer.toUnsignedLong(virtualSize),
                            Integer.toUnsignedLong(rawSize));
            if (Integer.toUnsignedLong(rva) >= Integer.toUnsignedLong(sectionRva)
                    && Integer.toUnsignedLong(rva) + 2 <= upper) {
                return Short.toUnsignedInt(bytes.getShort(
                        rawOffset + rva - sectionRva));
            }
        }
        throw new IllegalArgumentException("unmapped PE address " + address);
    }
}
