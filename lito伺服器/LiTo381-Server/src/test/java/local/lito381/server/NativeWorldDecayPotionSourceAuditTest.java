package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill71 and its ordinary-WORLD non-refreshing semantics. */
final class NativeWorldDecayPotionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerSelectedRowsBarrierAndPotionGateStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (71, 0, '藥水霜化術', 9, 6, 80, 0, 40318, 2, 500, 16, 'buff', 3, 0, 0, 0, 0, 8, 0, 1, 0, 3, 0, 0, 64, '$1990', 19, 2232, 0, 0, 0, 280);"));
        assertFalse(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (71,"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (71, '藥水腐壞術', 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (71, '藥水霜化術', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatch = assembly.indexOf("loc_4CB938:");
        String nativeCases = assembly.substring(dispatch, dispatch + 420);
        assertTrue(nativeCases.contains(
                "jumptable 004CB66E cases 10,19,27,39,46,55,63,70,102"));
        assertTrue(nativeCases.contains("mov     edx, 3"));
        assertTrue(nativeCases.contains("call    sub_4CA6E0"));
        String producer = assembly.substring(
                assembly.indexOf("sub_4CA6E0 proc near"),
                assembly.indexOf("sub_4CA6E0 endp"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("offset aCccdhh"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        int barrier = skillUse.indexOf(
                "if (pc.hasSkillEffect(ABSOLUTE_BARRIER))");
        assertTrue(skillUse.substring(barrier, barrier + 1_100)
                .contains("case DECAY_POTION:"));
        String mode = Files.readString(RETAINED.resolve(
                "model/skill/skillmode/DECAY_POTION.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains(
                "if (cha.hasSkillEffect(L1SkillId.DECAY_POTION))"));
        assertTrue(mode.contains("cha.set_decay_potion(true)"));
        assertTrue(mode.contains("cha.set_decay_potion(false)"));
        String buff = Files.readString(
                RETAINED.resolve("model/skill/L1BuffUtil.java"),
                StandardCharsets.UTF_8);
        assertTrue(buff.contains("if (pc.is_decay_potion())"));
        assertTrue(buff.contains("new S_ServerMessage(698)"));
    }

    @Test
    void worldDispatcherProbabilityStatusItemGateAndCancellationStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.DecayPotionSpell worldDecayPotion",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldDecayPotionSpell(payload, worldDecayPotion)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldDecayPotionSpell(");
        int methodEnd = session.indexOf(
                "private void useDecayPotionSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains(
                "BattleAbsoluteBarrierRules.blocksIncomingSpell("));
        assertTrue(method.contains("spell.probability("));
        assertTrue(method.contains("config.decayPotionFixtureDieRoll()"));
        assertTrue(method.contains(
                "config.decayPotionFixturePercentileRoll()"));
        assertTrue(method.contains("classicWorldCkewAttackAllowed("));
        assertTrue(method.contains("consumeEraseMagicAfterProbability("));
        assertTrue(method.contains("consumeCounterMagic(castNow)"));
        assertTrue(method.contains(
                "playerTarget.timedStatus.activateDecayPotion("));
        assertTrue(method.contains(
                "hub.decayPotionClassicWorldMonster("));
        assertTrue(method.contains("hub.provokeClassicWorldMonster("));
        assertTrue(method.contains("Protocol.effectObject("));
        assertTrue(method.contains(
                "Protocol.messageCode(spell.failureMessage())"));

        assertTrue(session.contains(
                "(phase == Phase.BATTLE || phase == Phase.WORLD)"));
        assertTrue(session.contains(
                "InventoryCatalog.blockedByDecayPotion(definition)"));
        assertTrue(session.contains("Protocol.messageCode(698)"));
        assertTrue(session.contains("monsterCancellation.decayPotion()"));
        int haste = session.indexOf(
                "definition.effect() == InventoryCatalog.Effect.HASTE");
        String hasteExecutor = session.substring(haste, session.indexOf(
                "definition.effect() == InventoryCatalog.Effect.BRAVE", haste));
        assertTrue(hasteExecutor.contains(
                "phase != Phase.BATTLE && phase != Phase.WORLD"));
        assertTrue(hasteExecutor.contains(
                "classicWorldMembers(classicWorldMapId)"));

        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains(
                "synchronized boolean decayPotion("));
        assertTrue(population.contains(
                "activateDecayPotion("));
        assertTrue(population.contains(
                "synchronized boolean decayPotionActive("));
        assertTrue(population.contains("timed.decayPotion()"));
    }
}
