package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill18 ordinary-WORLD target, probability and death fidelity. */
final class NativeWorldTurnUndeadSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1MagicPc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path SESSION = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java");
    private static final Path RETAINED = Path.of("../1-伺服器/src/com/lineage/server");

    @Test
    void selectedRowsNativeProducerAndRetainedDeathRuleStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (18, 0, '起死回生術', 3, 1, 20, "
                + "0, 0, 0, 500, 0, 'attack', 2, 0, 0, 0, 0, 7, 1, 4, 0, "
                + "10, 0, 0, 2, '$1970', 19, 754, 0, 0, 0, 280);"));
        String npc45065 = database.lines()
                .filter(line -> line.startsWith(
                        "INSERT INTO `npc` VALUES (45065, '人形殭屍', '$57'"))
                .findFirst().orElseThrow();
        assertTrue(npc45065.contains(
                "'L1Monster', 52, 6, 45, 12, 8, 14, 16, 6, 5, 2, 5, 37, -8"));
        assertTrue(npc45065.contains(
                "'zombie', 1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0"));
        assertTrue(BattleTurnUndead.eligible(
                SourceNpcPhysicalStats.memoryFixture(45065)));

        String assembly = LocalResources.readString(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB755:");
        String nativeCase = assembly.substring(dispatch, dispatch + 700);
        assertTrue(nativeCase.contains("push    12h"));
        assertTrue(nativeCase.contains("mov     edx, 0Ah"));
        assertTrue(nativeCase.contains("call    sub_4CA6E0"));
        String producer = assembly.substring(
                assembly.indexOf("sub_4CA6E0 proc near"),
                assembly.indexOf("sub_4CA6E0 endp"));
        assertTrue(producer.contains("offset aCccdhh"));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("this._skillId == TURN_UNDEAD"));
        assertTrue(skillUse.contains("this._dmg = cha.getCurrentHp();"));
        assertTrue(skillUse.contains("getNpcTemplate().get_IsTU()"));
        assertTrue(skillUse.contains("(undeadType == 0) || (undeadType == 2)"));
        String magic = LocalResources.readString(
                RETAINED.resolve("model/L1MagicPc.java"), StandardCharsets.UTF_8);
        assertTrue(magic.contains(
                "diceCount2 = getMagicBonus() + getMagicLevel() + 1"));
        assertTrue(magic.contains(
                "diceCount2 = getMagicBonus() + getMagicLevel() - 1"));
        assertTrue(magic.contains("probability += (_pc.getOriginalMagicHit() << 1)"));
        assertTrue(magic.contains("probability += _pc.getMagicHit()"));
        assertTrue(magic.contains("probability -= getTargetMr()"));
    }

    @Test
    void worldDispatcherPaysOnceAndUsesOrdinaryMonsterDeathLifecycle()
            throws Exception {
        String source = LocalResources.readString(SESSION, StandardCharsets.UTF_8);
        int worldStart = source.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = source.indexOf(
                "BattleSpellCatalog.TurnUndeadSpell worldTurnUndead", worldStart);
        int unsupported = source.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(source.substring(dispatch, unsupported).contains(
                "useClassicWorldTurnUndeadSpell(payload, worldTurnUndead)"));

        int methodStart = source.indexOf(
                "private void useClassicWorldTurnUndeadSpell(");
        int methodEnd = source.indexOf(
                "private void useClassicWorldManaDrainSpell(", methodStart);
        String method = source.substring(methodStart, methodEnd);
        assertTrue(method.contains("BattleTurnUndead.eligible(npcStats)"));
        assertTrue(method.contains("map.isSafetyZone(positionX, positionY)"));
        assertTrue(method.contains("config.turnUndeadFixtureDieRoll()"));
        assertTrue(method.contains("config.turnUndeadFixturePercentileRoll()"));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("monsterTarget.currentHp()"));
        assertTrue(method.contains(
                "publishClassicWorldSpellMonsterDamage(spell.skillId(), transition)"));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains("Protocol.messageCode(spell.failureMessage())"));
        assertTrue(method.contains("classicWorldInstanceId != originInstanceId"));

        String config = LocalResources.readString(
                Path.of("src/main/java/local/lito381/server/ServerConfig.java"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains(
                "integer(p, \"turn.undead.fixture.die.roll\", 0, 0, 7)"));
        assertTrue(config.contains(
                "integer(p, \"turn.undead.fixture.percentile.roll\", 0, 0, 100)"));
    }
}
