package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skill58 Fire Wall producer, rows and atomic runtime contract. */
final class NativeWorldFireWallSkill58SourceAuditTest {
    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin + start.length());
        assertTrue(begin >= 0 && finish > begin, start + " -> " + end);
        return source.substring(begin, finish);
    }

    @Test
    void selectedClientProducerAndDatabaseRowStayExact() throws Exception {
        String assembly = read(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String route = between(assembly, "loc_4CBA2C:", "loc_4CBA92:");
        assertTrue(route.contains("jumptable 004CB66E cases 57,62"));
        assertTrue(route.contains("call    sub_4C9380"));
        String producer = between(
                assembly, "sub_4C9270 proc near", "sub_4C9270 endp");
        assertTrue(producer.contains("push    offset aCcchh"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        String database = read(Path.of("../1-伺服器/db/380_db.sql"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (58, 0, '火牢', 8, 1, 60, 0, 0, 0, 10, 10, 'attack', 3, 20, 0, 0, 0, 0, 2, 64, 0, 8, 0, 0, 2, '$1472', 18, 168, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (58, '火牢', NULL, NULL);"));
    }

    @Test
    void retainedEightSegmentLeaseAndPulseRulesStayConnected()
            throws Exception {
        Path retained = Path.of("../1-伺服器/src/com/lineage/server");
        String use = read(retained.resolve("model/skill/L1SkillUse.java"));
        assertTrue(use.contains("case FIRE_WALL:"));
        assertTrue(use.contains(
                "L1SpawnUtil.doSpawnFireWall(_user, _targetX, _targetY);"));
        String spawn = read(retained.resolve("utils/L1SpawnUtil.java"));
        assertTrue(spawn.contains("for (int i = 0; i < 8; i++)"));
        assertTrue(spawn.contains("World.get().getVisibleObjects(_cha)"));
        assertTrue(spawn.contains("if (tmp >= 24)"));
        assertTrue(spawn.contains("WorldEffect.get().isEffect(loc, npcid)"));
        assertTrue(spawn.contains("if (!map.isArrowPassable("));
        String timer = read(retained.resolve(
                "timecontroller/skill/EffectFirewallTimer.java"));
        assertTrue(timer.contains(
                "19 + _random.nextInt(Math.max(user.getInt() / 2, 1))"));
        assertTrue(timer.contains("tgpc.getClanid() == user.getClanid()"));
        assertTrue(timer.contains("tgpc.isSafetyZone()"));
        assertTrue(timer.contains("L1AttackList.SKM0.get(key)"));
        assertTrue(read(retained.resolve(
                "model/Instance/L1EffectInstance.java")).contains(
                "FW_DAMAGE_INTERVAL = 1650"));
    }

    @Test
    void battleAndWorldHandlersCommitLeaseAndResourcesAtomically()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String battle = between(session, "private void useGroundSpell(",
                "private void useClassicWorldGroundSpell(");
        String world = between(session,
                "private void useClassicWorldGroundSpell(",
                "private boolean admitClassicWorldSpellFamily(");
        for (String handler : new String[] {battle, world}) {
            assertTrue(handler.contains("synchronized (this)"));
            assertTrue(handler.contains("observer || playerHp <= 0"));
            assertTrue(handler.contains("BattleSpellCatalog.isInNativeScreen("));
            assertTrue(handler.contains("spellReadyNanos.put(spell.skillId()"));
            assertTrue(handler.contains("playerMp -= mpCost"));
            assertTrue(handler.contains("cancelMeditation("));
            assertTrue(handler.contains("cancelAbsoluteBarrier("));
            assertTrue(handler.contains("revealInvisibility("));
            assertFalse(handler.contains("commitSpellReady("));
        }
        assertTrue(battle.contains("hub.placeGroundSpell("));
        assertTrue(world.contains("hub.placeClassicWorldGroundSpell("));

        String hub = read(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        assertTrue(hub.contains("Runnable resourceCommit"));
        assertTrue(hub.contains("if (placed.accepted()) resourceCommit.run();"));
        assertTrue(hub.contains("arena.nativeStandingFloor(sourceX, sourceY)"));
        assertTrue(hub.contains("map.standingPassable(sourceX, sourceY)"));
    }

    @Test
    void catalogsKeepNativeFireWallGeometryAndDamageRules() throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains(
                "FIRE_WALL, new GroundSpell(FIRE_WALL, 7, 1, 18, 168, 8,"));
        assertTrue(catalog.contains(
                "60, 10, 10, 1_650, 1, 8, 0, 0, \"fire-wall\")"));
        String battle = read(Path.of(
                "src/main/java/local/lito381/server/BattleGroundEffects.java"));
        String world = read(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldGroundEffects.java"));
        for (String effects : new String[] {battle, world}) {
            assertTrue(effects.contains(
                    "MAX_FIRE_WALL_SEGMENTS_PER_CASTER = 24"));
            assertTrue(effects.contains("spell.segmentCount()"));
        }
        assertTrue(battle.contains("arena.nativeCanStep("));
        assertTrue(world.contains("map.attackPosition("));
        assertTrue(world.contains("map.arrowPassable("));
    }
}
