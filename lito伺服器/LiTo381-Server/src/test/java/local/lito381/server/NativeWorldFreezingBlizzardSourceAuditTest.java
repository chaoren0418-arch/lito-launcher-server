package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill80 and its ordinary-WORLD multi-actor freeze lifecycle. */
final class NativeWorldFreezingBlizzardSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedTargetingStayExact() throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (80, 0, '冰雪颶風', 10, 7, 60, 0, 0, 0, 3000, 32, 'none', 3, 2, 10, 5, 0, 1, 4, 64, -10, 0, 10, 0, 128, '$1994', 18, 3933, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (80, '冰雪颶風-寒冰耐性', 52, 44, 36, 0, 0, 0.38, 1, 0, 1, 0.05, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (80, '冰雪颶風', 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (80, '冰雪颶風', NULL, NULL);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("sub_4CB5F0 proc near");
        int producerEnd = assembly.indexOf("sub_4CB5F0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        int routeStart = producer.indexOf("loc_4CBB81:");
        int routeEnd = producer.indexOf("loc_4CBB9A:", routeStart);
        String route = producer.substring(routeStart, routeEnd);
        assertTrue(route.contains(
                "jumptable 004CB66E cases 52,58,61,69,79"));
        assertTrue(route.contains("mov     al, byte_635A44[esi]"));
        assertTrue(route.contains("and     eax, 7"));
        assertTrue(route.contains("jmp     short loc_4CBBDA"));
        int tailStart = producer.indexOf("loc_4CBBDA:");
        String tail = producer.substring(tailStart, tailStart + 500);
        assertTrue(tail.contains("sar     esi, 3"));
        assertTrue(tail.contains("push    14h"));
        assertTrue(tail.contains("call    sub_4C6AE0"));

        String skillUse = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("if (_skill.getTarget().equals(\"none\"))"));
        assertTrue(skillUse.contains(
                "objects = World.get().getVisibleObjects(this._target, this._skill.getArea());"));
        assertTrue(skillUse.contains("case FREEZING_BLIZZARD"));
        assertTrue(skillUse.contains("S_RangeSkill.TYPE_NODIR"));
        assertTrue(skillUse.contains("freezeAfterAccepted = this._isFreeze"));
        assertTrue(skillUse.contains("new S_Poison(pc.getId(), 2)"));
        assertTrue(skillUse.contains(
                "new S_Paralysis(S_Paralysis.TYPE_FREEZE, true)"));

        String stop = LocalResources.readString(
                RETAINED.resolve("model/skill/L1SkillStop.java"),
                StandardCharsets.UTF_8);
        assertTrue(stop.contains("case FREEZING_BLIZZARD"));
        assertTrue(stop.contains("new S_Poison(pc.getId(), 0)"));
        assertTrue(stop.contains(
                "new S_Paralysis(S_Paralysis.TYPE_FREEZE, false)"));
    }

    @Test
    void worldDispatcherTargetsAllActorKindsAndPublishesTypeZeroLifecycle()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.FreezeAttackSpell worldFreeze", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldCenteredFreezeAttackSpell(payload, worldFreeze)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldCenteredFreezeAttackSpell(");
        int methodEnd = session.indexOf(
                "private void publishClassicWorldFreezePalette(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("candidate != this"));
        assertTrue(method.contains("owner == this"));
        assertTrue(method.contains("map.isNormalZone(originX, originY)"));
        assertTrue(method.contains("hub.classicWorldMonstersInScreen("));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("freezeActive(requestNow)"));
        assertTrue(method.contains("classicWorldMonsterFreezeActive("));
        assertTrue(method.contains("totalFreezeResistance()"));
        assertTrue(method.contains("consumeCounterMagic(castNow)"));
        assertTrue(method.contains("counterMirrorDecision("));
        assertTrue(method.contains("classicWorldCkewAttackAllowed("));
        assertTrue(method.contains("applySpellLawfulDelta("));
        assertTrue(method.contains("spell.effectGfx(), 0,"));
        assertTrue(method.contains("takeClassicWorldPlayerDamage("));
        assertTrue(method.contains("applyClassicWorldMonsterCompanionDamage("));
        assertTrue(method.contains("hub.damageClassicWorldMonster("));
        assertTrue(method.contains("target.timedStatus.activateFreeze("));
        assertTrue(method.contains("target.controlled().activateFreeze("));
        assertTrue(method.contains("hub.freezeClassicWorldMonster("));
        assertTrue(method.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.FREEZE_APPLY)"));
        assertTrue(method.contains("damage > 0"));

        String companion = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"),
                StandardCharsets.UTF_8);
        assertTrue(companion.contains("!timedStatus.freezeActive(nowNanos)"));
        assertTrue(companion.contains("boolean activateFreeze("));
        assertTrue(companion.contains("boolean expireFreeze("));
        assertTrue(companion.contains("boolean freeze = timedStatus.cancelFreeze()"));

        String population = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains(
                "actor.damagePoisonStatus.activateFreeze("));
        assertTrue(population.contains("private static boolean actionLocked("));
        assertTrue(population.contains(
                "actor.damagePoisonStatus.freezeActive(nowNanos)"));
        assertTrue(population.contains(
                "boolean thunderGrab, boolean horrorOfDeath, boolean windShackle"));

        int expiryStart = session.indexOf(
                "hub.expireClassicWorldMonsterFreezes(");
        int expiryEnd = session.indexOf(
                "hub.expireClassicWorldMonsterHastes(", expiryStart);
        String expiry = session.substring(expiryStart, expiryEnd);
        assertTrue(expiry.contains("Protocol.poison(expired.objectId(), palette)"));
        assertTrue(expiry.contains("reason=freeze-expired"));
        assertTrue(session.contains(
                "if (timedStatus.expireFreeze(nowNanos) && (inBattle || inClassicWorld))"));
        assertTrue(session.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.FREEZE_RELEASE)"));
    }
}
