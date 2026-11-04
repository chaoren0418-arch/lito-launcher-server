package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill87 and its ordinary-WORLD player/NPC stun lifecycle. */
final class NativeWorldShockStunSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedRulesStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (87, 0, '衝擊之暈', 11, 6, 15, 0, 0, 0, 8000, 6, 'attack', 3, 10, 10, 0, 30, 0, 0, 64, 0, 1, 0, 0, 64, '', 19, 4434, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (87, '衝擊之暈-昏迷耐性', 60, 45, 30, 0, 0, 0, 5, 0, 1, 0.05, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (87, '衝擊之暈', 10);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (87, '衝擊之暈');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_魔屏無效技能` VALUES (87, '衝擊之暈');"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("sub_4CB5F0 proc near");
        int producerEnd = assembly.indexOf("sub_4CB5F0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        int routeStart = producer.indexOf("loc_4CB9A3:");
        int routeEnd = producer.indexOf("loc_4CBA1E:", routeStart);
        String route = producer.substring(routeStart, routeEnd);
        assertTrue(route.contains("jumptable 004CB66E case 86"));
        assertTrue(route.contains("cmp     eax, 32h"));
        assertTrue(route.contains("cmp     eax, 36h"));
        assertTrue(route.contains("mov     ecx, 56h"));
        assertTrue(route.contains("mov     edx, 1"));
        assertTrue(route.contains("call    sub_4CA6E0"));
        int senderStart = assembly.indexOf("sub_4CA6E0 proc near");
        int senderEnd = assembly.indexOf("sub_4CA6E0 endp", senderStart);
        String sender = assembly.substring(senderStart, senderEnd);
        assertTrue(sender.contains("push    offset aCccdhh"));
        assertTrue(sender.contains("and     ecx, 7"));
        assertTrue(sender.contains("sar     ebx, 3"));
        assertTrue(sender.contains("push    14h"));

        String shock = Files.readString(RETAINED.resolve(
                "model/skill/skillmode/SHOCK_STUN.java"),
                StandardCharsets.UTF_8);
        assertTrue(shock.contains("random.nextInt(randomSec + 1)"));
        assertTrue(shock.contains("L1SpawnUtil.spawnEffect(81162, shock"));
        assertTrue(shock.contains(
                "new S_Paralysis(S_Paralysis.TYPE_STUN, true)"));
        assertTrue(shock.contains(
                "new S_Paralysis(S_Paralysis.TYPE_STUN, false)"));
    }

    @Test
    void worldDispatcherTargetsAllActorKindsAndExpiresNativeEffect()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.ShockStunSpell worldShockStun", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldShockStunSpell(payload, worldShockStun)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldShockStunSpell(");
        int methodEnd = session.indexOf(
                "private void publishClassicWorldShockStunPacket(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(method.contains("spell.supportsWalkAction(walkAction)"));
        assertTrue(method.contains("BattleActionCodes.attackForWalk(walkAction)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("candidate != this"));
        assertTrue(method.contains("classicWorldCompanionSnapshots()"));
        assertTrue(method.contains("hub.classicWorldMonster("));
        assertTrue(method.contains("map.attackPosition("));
        assertTrue(method.contains("totalStunResistance()"));
        assertTrue(method.contains("totalStunSuccess()"));
        assertTrue(method.contains("classicWorldCkewAttackAllowed("));
        assertTrue(method.contains("counter-magic-consumed=false"));
        assertTrue(method.contains("takeClassicWorldPlayerDamage("));
        assertTrue(method.contains("applyClassicWorldMonsterCompanionDamage("));
        assertTrue(method.contains("hub.damageClassicWorldMonster("));
        assertTrue(method.contains("activateShockStun("));
        assertTrue(method.contains("hub.shockStunClassicWorldMonster("));
        assertTrue(method.contains("hub.placeClassicWorldShockStunEffect("));
        assertTrue(method.contains("Protocol.shockStunEffect("));
        assertTrue(method.contains(
                "Protocol.paralysis(Protocol.ParalysisMode.STUN_APPLY)"));

        String companion = Files.readString(Path.of(
                "src/main/java/local/lito381/server/BattleCompanion.java"),
                StandardCharsets.UTF_8);
        assertTrue(companion.contains(
                "!timedStatus.shockStunActive(nowNanos)"));
        assertTrue(companion.contains("activateShockStun("));
        String population = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        assertTrue(population.contains("synchronized BattleTimedStatus.ShockStunActivation shockStun("));
        assertTrue(population.contains(
                "actor.damagePoisonStatus.shockStunActive(nowNanos)"));
        assertTrue(session.contains("expireClassicWorldShockStunEffects("));
        assertTrue(session.contains(
                "&& (inBattle || inClassicWorld)) {\n            sendEncrypted(Protocol.paralysis(Protocol.ParalysisMode.STUN_RELEASE)"));
    }
}
