package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill41 and its ordinary-WORLD dead-monster conversion. */
final class NativeWorldCreateZombieSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1SummonInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedZombieRulesStayExact() throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (41, 0, '造屍術', 6, 0, 5, 80, 40318, 1, 10, 0, 'buff', 2, 0, 0, 0, 0, 0, 1, 32, -10, -1, 0, 0, 1, '$1461', 19, 226, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (41, '造屍術', NULL, NULL);"));
        String secondary = LocalResources.readString(Path.of(
                "../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (41, '造屍術', 6, 0, 5, 80, 40318, 1, 0, 0, 'buff', 2, 0, 0, 0, 0, 0, 1, 32, -10, -1, 0, 0, 1, '$1461', 19, 226, 0, 0, 0, 280);"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills_item` VALUES (41, '造屍術', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `npc` VALUES (45065, '人形殭屍', '$57', '0', '普通', 'L1Monster', 52, 6, 45, 12,"));
        assertTrue(database.contains(
                "INSERT INTO `npc` VALUES (81188, '人形殭屍', '$57', '0', '造屍術', 'L1Monster', 52, 20, 800, 12,"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CB8A0:");
        int routeEnd = assembly.indexOf("loc_4CB8B4:", routeStart);
        String route = assembly.substring(routeStart, routeEnd);
        assertTrue(route.contains("cases 40,60,74"));
        assertTrue(route.contains("call    sub_4CA4F0"));
        int producerStart = assembly.indexOf("sub_4CA4F0 proc near");
        int producerEnd = assembly.indexOf("sub_4CA4F0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("and     eax, 7"));
        assertTrue(producer.contains("sar     edi, 3"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("\"cccd\""));

        String skillUse = LocalResources.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("targetNpc.isDead()"));
        assertTrue(skillUse.contains(
                "World.get().findObject(targetNpc.getId()) != targetNpc"));
        assertTrue(skillUse.contains("player.canTakePet(6)"));
        assertTrue(skillUse.contains("new L1SummonInstance("));
        assertTrue(skillUse.contains("new S_ServerMessage(319)"));
        assertTrue(skillUse.contains("new S_ServerMessage(280)"));

        String summon = LocalResources.readString(RETAINED.resolve(
                "model/Instance/L1SummonInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(summon.contains("if (isCreateZombie)"));
        assertTrue(summon.contains("npcId = 81183"));
        assertTrue(summon.contains("npcId = 81188"));
        assertTrue(summon.contains("this.setPetcost(6)"));
        assertTrue(summon.contains("target.deleteMe()"));
        assertTrue(summon.contains("master.addPet(this)"));
    }

    @Test
    void worldDispatcherConsumesCorpseAndPublishesControlledReplacement()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int dispatchStart = session.indexOf(
                "BattleSpellCatalog.WeightSpell worldWeight");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.AquaProtectSpell worldAquaProtect",
                dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.CreateZombieSpell worldCreateZombie"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.createZombieSpell(selector.skillId())"));
        assertTrue(dispatch.contains("admitClassicWorldSpellFamily("));
        assertTrue(dispatch.contains(
                "useCreateZombieSpell(payload, worldCreateZombie)"));

        int handlerStart = session.indexOf(
                "private void useCreateZombieSpell(");
        int handlerEnd = session.indexOf(
                "private void useClassicWorldResurrectionSpell(", handlerStart);
        String handler = session.substring(handlerStart, handlerEnd);
        assertTrue(handler.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(handler.contains("hub.classicWorldMonster("));
        assertTrue(handler.contains("worldCorpse.present() && !worldCorpse.alive()"));
        assertTrue(handler.contains("phase == originPhase && !observer"));
        assertTrue(handler.contains("classicWorldMapId == originMapId"));
        assertTrue(handler.contains("classicWorldInstanceId == originInstanceId"));
        assertTrue(handler.contains("hub.consumeClassicWorldMonsterCorpse("));
        assertTrue(handler.contains("BattleCompanion.zombie("));
        assertTrue(handler.contains("tamedCompanions.add(companion)"));
        assertTrue(handler.contains("publishCreatedZombie(companion"));
        assertTrue(handler.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(handler.contains("cancelAbsoluteBarrier("));
        assertTrue(handler.contains("revealInvisibility("));
        assertTrue(handler.contains("commitSpellReady("));
        assertTrue(handler.contains("canTakeControlledCompanion(spell.petCost())"));
        assertTrue(handler.contains("Protocol.messageCode(319)"));
        assertTrue(handler.contains("Protocol.messageCode(spell.failureMessage())"));

        String hub = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        int consumeStart = hub.indexOf(
                "consumeClassicWorldMonsterCorpse(");
        int consumeEnd = hub.indexOf(
                "ClassicWorldMonsterPopulation.Snapshot reserveReleasedPet(",
                consumeStart);
        String consume = hub.substring(consumeStart, consumeEnd);
        assertTrue(consume.contains("population.removeCorpse(mapId, objectId)"));
        assertTrue(consume.contains("population.nextRespawnDelaySeconds("));
        assertTrue(consume.contains("respawnClassicWorldMonsterSafely("));
    }
}
