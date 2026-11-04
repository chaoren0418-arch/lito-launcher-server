package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected LiTo skills 58/63 and their retained ordinary-WORLD rules. */
final class NativeWorldGroundSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY);
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void selectedProducerDatabaseAndRetainedEffectRulesStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (58, 0, '火牢', 8, 1, 60, 0, 0, 0, 10, 10, 'attack', 3, 20, 0, 0, 0, 0, 2, 64, 0, 8, 0, 0, 2, '$1472', 18, 168, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (63, 0, '治癒能量風暴', 8, 6, 20, 0, 40318, 1, 10, 320, 'buff', 32, 0, 0, 0, 0, 0, 0, 2, 0, -1, 3, 0, 64, '$1988', 19, 2231, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (58, '火牢', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (63, '治癒能量風暴', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CBA2C:");
        String route = assembly.substring(routeStart, routeStart + 500);
        assertTrue(route.contains("cases 57,62"));
        assertTrue(route.contains("call    sub_4C9380"));
        int producerStart = assembly.indexOf("sub_4C9270 proc near");
        int producerEnd = assembly.indexOf("sub_4C9270 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("push    offset aCcchh"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        String skillUse = Files.readString(
                RETAINED.resolve("model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("case LIFE_STREAM:"));
        assertTrue(skillUse.contains("L1SpawnUtil.spawnEffect(81169,"));
        assertTrue(skillUse.contains("case FIRE_WALL:"));
        assertTrue(skillUse.contains(
                "L1SpawnUtil.doSpawnFireWall(_user, _targetX, _targetY);"));

        String spawn = Files.readString(
                RETAINED.resolve("utils/L1SpawnUtil.java"),
                StandardCharsets.UTF_8);
        assertTrue(spawn.contains("for (int i = 0; i < 8; i++)"));
        assertTrue(spawn.contains("World.get().getVisibleObjects(_cha)"));
        assertTrue(spawn.contains("if (tmp >= 24)"));
        assertTrue(spawn.contains("if (WorldEffect.get().isEffect(loc, npcid))"));
        assertTrue(spawn.contains("if (!map.isArrowPassable("));

        String firewall = Files.readString(RETAINED.resolve(
                "timecontroller/skill/EffectFirewallTimer.java"),
                StandardCharsets.UTF_8);
        assertTrue(firewall.contains(
                "WorldEffect.get().getFirewall(effect)"));
        assertTrue(firewall.contains(
                "effect.get_showId() != object.get_showId()"));
        assertTrue(firewall.contains("object instanceof L1PcInstance"));
        assertTrue(firewall.contains("object instanceof L1MonsterInstance"));
        assertTrue(firewall.contains("tgpc.getClanid() == user.getClanid()"));
        assertTrue(firewall.contains("tgpc.isSafetyZone()"));
        assertTrue(firewall.contains(
                "19 + _random.nextInt(Math.max(user.getInt() / 2, 1))"));
        assertTrue(firewall.contains("L1AttackList.SKM0.get(key)"));

        String effect = Files.readString(RETAINED.resolve(
                "model/Instance/L1EffectInstance.java"),
                StandardCharsets.UTF_8);
        assertTrue(effect.contains("FW_DAMAGE_INTERVAL = 1650"));

        String regen = Files.readString(RETAINED.resolve(
                "timecontroller/pc/PcRegenExecutor.java"),
                StandardCharsets.UTF_8);
        assertTrue(regen.contains("effect.getNpcId() == 81169"));
        assertTrue(regen.contains(
                "effect.getLocation().getTileLineDistance(pc.getLocation()) < 4"));
        assertTrue(regen.contains("hpDelta += 3"));
        assertTrue(regen.contains("if (hpVal < 0 && inLifeStream)"));
    }

    @Test
    void ordinaryWorldDispatcherResourcesPulsesAndRegenAreConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.GroundSpell worldGround", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useClassicWorldGroundSpell(payload, worldGround)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldGroundSpell(");
        int methodEnd = session.indexOf(
                "private boolean admitClassicWorldSpellFamily(", methodStart);
        assertTrue(methodStart > 0 && methodEnd > methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.groundSpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("spell.effectiveMpCost("));
        assertTrue(method.contains("nextInventory.removeCatalog("));
        assertTrue(method.contains("hub.placeClassicWorldGroundSpell("));
        assertTrue(method.contains("spellReadyNanos.put(spell.skillId()"));
        assertFalse(method.contains("commitSpellReady("));
        assertTrue(method.contains("Protocol.changeDirection("));
        assertTrue(method.contains("Protocol.effectLocation("));
        assertTrue(method.contains("state.saveEconomyState("));
        assertFalse(method.contains("hub.roomMembers("));

        assertTrue(session.contains(
                "void serverClassicWorldGroundPlayerPulse("));
        assertTrue(session.contains(
                "void serverClassicWorldGroundMonsterPulse("));
        assertTrue(session.contains(
                "hub.inClassicWorldLifeStream("));

        String effects = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldGroundEffects.java"),
                StandardCharsets.UTF_8);
        assertTrue(effects.contains(
                "MAX_FIRE_WALL_SEGMENTS_PER_CASTER = 24"));
        assertTrue(effects.contains(
                "BattleSpellCatalog.isInNativeScreen("));
        assertTrue(effects.contains(
                "ClassicWorldGroundEffects.ActorKind".replace(
                        "ClassicWorldGroundEffects.", "")));

        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        assertTrue(hub.contains("placeClassicWorldGroundSpell("));
        assertTrue(hub.contains("classicWorldGroundTicks(nowNanos)"));
        assertTrue(hub.contains(
                "serverClassicWorldGroundPlayerPulse("));
        assertTrue(hub.contains(
                "serverClassicWorldGroundMonsterPulse("));
    }
}
