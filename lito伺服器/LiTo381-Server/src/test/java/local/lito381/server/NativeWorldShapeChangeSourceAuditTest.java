package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill67, monlist ownership and ordinary-WORLD polymorph consumers. */
final class NativeWorldShapeChangeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseMenuAndRetainedRulesStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (67, 0, '變形術', 9, 2, 80, 0, 40318, 1, 10, 7200, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 4, '$1478', 19, 230, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (67, '變形術', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        String dispatch = between(assembly, "loc_4CB112:", "loc_4CB11C:");
        assertTrue(dispatch.contains("cases 0,7,8,18,22,34,36,42,43,47,54,56,66"));
        assertTrue(dispatch.contains("jmp     sub_4C9880"));
        String producer = between(
                assembly, "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("push    offset aCccd"));

        String skillUse = retained("model/skill/L1SkillUse.java");
        assertTrue(skillUse.contains("pc.getInventory().checkEquipped(20281)"));
        assertTrue(skillUse.contains("new S_ShowPolyList(pc.getId())"));
        assertTrue(skillUse.contains("pc.setShapeChange(true)"));
        assertTrue(skillUse.contains("WandPolyTable.get().getRandomPolyId()"));
        String actionPc = retained("model/L1ActionPc.java");
        assertTrue(actionPc.contains("if (_pc.isShapeChange())"));
        assertTrue(actionPc.contains("L1PolyMorph.handleCommands(_pc, cmd)"));
        assertTrue(actionPc.contains("_pc.setShapeChange(false)"));
        String polymorph = retained("model/L1PolyMorph.java");
        assertTrue(polymorph.contains("weaponTakeoff"));
        assertTrue(polymorph.contains("pc.removeSkillEffect(L1SkillId.SHAPE_CHANGE)"));
        assertTrue(polymorph.contains("doPoly(pc, poly.getPolyId(), 7200"));

        assertEquals(96, BattlePolymorphCatalog.standardMenuForms().size());
        assertEquals(21, BattlePolymorphCatalog.allForms().stream()
                .filter(form -> form.randomWeight() > 0).count());
        assertEquals(1_000_000, BattlePolymorphCatalog.allForms().stream()
                .mapToInt(BattlePolymorphCatalog.Form::randomWeight).sum());
        assertEquals(3, BattlePolymorphCatalog.standardMenuForms().stream()
                .filter(form -> !form.skillUsable()).count());
    }

    @Test
    void worldDispatcherTokenContextRoiAndActionConsumersStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        String dispatch = between(session,
                "BattleSpellCatalog.SleepSpell worldSleep",
                "BattleSpellCatalog.PoisonSpell worldPoison");
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.PolymorphSpell worldPolymorph"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.polymorphSpell(selector.skillId())"));
        assertTrue(dispatch.contains(
                "admitClassicWorldSpellFamily("));
        assertTrue(dispatch.contains(
                "usePolymorphSpell(payload, worldPolymorph)"));

        String hypertext = between(session,
                "private void hypertextAction(",
                "private void handleClassicCastleTrebuchetAction(");
        assertTrue(hypertext.contains(
                "PendingShapeChange shapeChange = pendingShapeChange"));
        assertTrue(hypertext.indexOf("handleShapeChangeAction(request, shapeChange)")
                < hypertext.indexOf("if (phase == Phase.WORLD)"));
        String callback = between(session,
                "private void handleShapeChangeAction(",
                "private void requestPlayerTrade(");
        assertTrue(callback.contains("pending.originPhase() == Phase.WORLD"));
        assertTrue(callback.contains("classicWorldMapId == pending.mapId()"));
        assertTrue(callback.contains("classicWorldInstanceId == pending.instanceId()"));
        assertTrue(callback.contains("pendingShapeChange = null"));
        assertTrue(callback.contains("BattlePolymorphCatalog.find(request.action())"));
        assertTrue(callback.contains("preparePolymorphEquipment(form)"));

        String handler = between(session,
                "private void usePolymorphSpell(",
                "private void useLightSpell(");
        assertTrue(handler.contains("originPhase == Phase.WORLD"));
        assertTrue(handler.contains("classicWorldMembers(originMapId)"));
        assertTrue(handler.contains("target.classicWorldInstanceId == originInstanceId"));
        assertTrue(handler.contains("positionX == originX"));
        assertTrue(handler.contains("target.positionX == targetX"));
        assertTrue(handler.contains("new PendingShapeChange("));
        assertTrue(handler.contains("BattlePolymorphCatalog.randomForm("));
        assertTrue(handler.contains("cancelAbsoluteBarrier("));
        assertTrue(handler.contains("revealInvisibility("));
        assertTrue(handler.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(session.contains(
                "if ((phase != Phase.BATTLE && phase != Phase.WORLD)"));
        assertTrue(session.contains(
                "inventory.controlAbilityEnabled(abilityType)"));
        assertTrue(session.contains(
                "state.saveEconomyState(stateCharacter(), shopPoints"));

        String appearance = between(session,
                "private void broadcastPolymorphAppearance(",
                "private PolymorphEquipmentChange preparePolymorphEquipment(");
        assertTrue(appearance.contains("originPhase == Phase.WORLD"));
        assertTrue(appearance.contains("classicWorldMembers(originMapId)"));
        assertTrue(appearance.contains("Protocol.polymorph("));
        assertTrue(session.contains("polymorphStatus.expire(nowNanos)"));
        assertTrue(session.contains("!active.skillUsable()"));
        assertTrue(session.contains(
                "BattleActionCodes.attackForWalk(battleWalkAction())"));

        String acceptance = Files.readString(Path.of(
                "tools/classic_world_shape_change_selftest.py"),
                StandardCharsets.UTF_8);
        assertTrue(acceptance.contains("random_target"));
        assertTrue(acceptance.contains("one_shot_world_token"));
        assertTrue(acceptance.contains("world_attack_action"));
        assertTrue(acceptance.contains("blocked_message"));
        assertTrue(acceptance.contains("encrypted_stream_aligned"));
        String config = Files.readString(Path.of(
                "src/test/resources/server-config/root/server.world-shape-change-selftest.properties"),
                StandardCharsets.UTF_8);
        assertTrue(config.contains("database.read.only=true"));
        assertTrue(config.contains("classic.world.monster.ai.enabled=false"));
        assertTrue(config.contains("uncanny.dodge.hit.fixture.d20=20"));
        assertTrue(config.contains("uncanny.dodge.hit.fixture.d100=1"));
        assertTrue(config.contains("control.ring.fixture.enabled=true"));
    }

    private static String retained(String relative) throws Exception {
        return Files.readString(RETAINED.resolve(relative).normalize(),
                StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
