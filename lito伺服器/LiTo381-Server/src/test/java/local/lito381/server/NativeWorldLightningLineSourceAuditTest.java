package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected LiTo skill17 and its retained ordinary-WORLD line rules. */
final class NativeWorldLightningLineSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void selectedProducerDatabaseAndRetainedBresenhamRulesStayExact()
            throws Exception {
        String database = read(Path.of("../1-伺服器/db/380_db.sql"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (17, 0, '極光雷電', 3, 0,"
                        + " 20, 0, 0, 0, 500, 0, 'attack', 3, 0, 1, 4, 0,"
                        + " 0, 8, 64, 0, 6, 3, 0, 1, '$1446', 18, 170,"
                        + " 0, 0, 0, 0);"));

        String assembly = read(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        int routeStart = assembly.indexOf("loc_4CBA5C:");
        String route = assembly.substring(routeStart, routeStart + 700);
        assertTrue(route.contains(
                "jumptable 004CB66E cases 16,21,24,29"));
        assertTrue(route.contains("mov     edx, 6"));
        assertTrue(route.contains("call    sub_4CA6E0"));
        int producerStart = assembly.indexOf("sub_4CA6E0 proc near");
        int producerEnd = assembly.indexOf(
                "sub_4CA6E0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("push    offset aCccdhh"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        String skillUse = read(RETAINED.resolve(
                "model/skill/L1SkillUse.java"));
        assertTrue(skillUse.contains("case LIGHTNING:"));
        assertTrue(skillUse.contains("case FREEZING_BREATH:"));
        assertTrue(skillUse.contains(
                "World.get().getVisibleLineObjects(this._user, this._target)"));
        assertTrue(skillUse.contains("if (this.isTarget(cha) == false)"));
        assertTrue(skillUse.contains(
                "this._targetList.add(new TargetStatus(cha))"));

        String world = read(RETAINED.resolve("world/World.java"));
        assertTrue(world.contains(
                "private HashMap<Integer, Integer> createLineMap"));
        assertTrue(world.contains("E = -dx"));
        assertTrue(world.contains("for (i = 0; i <= dx; i++)"));
        assertTrue(world.contains("E = -dy"));
        assertTrue(world.contains("for (i = 0; i <= dy; i++)"));
        assertTrue(world.contains("getVisibleLineObjects"));
        assertTrue(world.contains("src.get_showId() != element.get_showId()"));
        assertTrue(world.contains("lineMap.containsKey(key)"));
    }

    @Test
    void ordinaryWorldDispatcherLineTargetsAndDamageConsumersAreConnected()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        int worldGroundDispatch = session.indexOf(
                "BattleSpellCatalog.GroundSpell worldGround");
        int worldAttackDispatch = session.indexOf(
                "BattleSpellCatalog.AttackSpell worldAttack",
                worldGroundDispatch);
        String worldDispatcher = session.substring(
                worldGroundDispatch, worldAttackDispatch);
        assertTrue(worldDispatcher.contains(
                "BattleSpellCatalog.LineAttackSpell worldLineAttack"));
        assertTrue(worldDispatcher.contains(
                "BattleSpellCatalog.lineAttackSpell(selector.skillId())"));
        assertTrue(worldDispatcher.contains(
                "useClassicWorldLineAttackSpell(payload, worldLineAttack)"));

        int dispatch = session.indexOf(
                "BattleSpellCatalog.LineAttackSpell lineAttackSpell");
        int areaDispatch = session.indexOf(
                "BattleSpellCatalog.AreaAttackSpell areaAttackSpell",
                dispatch);
        String dispatcher = session.substring(dispatch, areaDispatch);
        assertTrue(dispatcher.contains("if (phase == Phase.WORLD)"));
        assertTrue(dispatcher.contains(
                "useClassicWorldLineAttackSpell(payload, lineAttackSpell)"));
        assertTrue(dispatcher.contains(
                "useLineAttackSpell(payload, lineAttackSpell)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldLineAttackSpell(");
        int methodEnd = session.indexOf(
                "private void useClassicWorldLineFreezeAttackSpell(",
                methodStart);
        assertTrue(methodStart > 0 && methodEnd > methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.attackSpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("BattleStraightLine.points("));
        assertTrue(method.contains("BattleStraightLine.contains("));
        assertTrue(method.contains("map.attackPosition("));
        assertTrue(method.contains("classicWorldInstanceId"));
        assertTrue(method.contains("primaryCount != 1"));
        assertTrue(method.contains("spell.effectiveMpCost("));
        assertTrue(method.contains("target.timedStatus.consumeCounterMagic("));
        assertTrue(method.contains("target.eraseMagicStatus.capture("));
        assertTrue(method.contains("target.combatDamageAfterCooking("));
        assertTrue(method.contains("target.earthBindProtectedCombatDamage("));
        assertTrue(method.contains("target.controlled().earthBindActive("));
        assertTrue(method.contains(
                "hub.classicWorldMonsterEarthBindActive("));
        assertTrue(method.contains(
                "ClassicWorldMonsterMagicDamage"));
        assertTrue(method.contains(
                "hub.classicWorldMonsterElementalFallDownModifier("));
        assertTrue(method.contains("classicWorldCkewAttackAllowed("));
        assertTrue(method.contains("wireTargets.sort("));
        assertTrue(method.contains("Protocol.attackMany("));
        assertTrue(method.contains(
                "publishClassicWorldAreaAttackSpellPacket("));
        assertTrue(method.contains("takeClassicWorldPlayerDamage("));
        assertTrue(method.contains(
                "applyClassicWorldMonsterCompanionDamage("));
        assertTrue(method.contains("hub.damageClassicWorldMonster("));
        assertTrue(method.contains(
                "publishClassicWorldSpellMonsterDamage("));
        assertFalse(method.contains("hub.roomMembers("));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
