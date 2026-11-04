package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks selected LiTo skill69 and retained ordinary-WORLD teleport rules. */
final class NativeWorldMassTeleportSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_Attr.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/MASS_TELEPORT.java"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED_ROOT = Path.of("../1-伺服器");
    private static final Path RETAINED = RETAINED_ROOT.resolve(
            "src/com/lineage/server");
    private static final Path RECOVERED = Path.of(
            "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");

    @Test
    void selectedProducerDatabaseAndRetainedRulesStayExact() throws Exception {
        String database = read(RETAINED_ROOT.resolve("db/380_db.sql"));
        String secondary = read(RETAINED_ROOT.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        String selectedRow = "INSERT INTO `skills` VALUES (69, 0, '集體傳送術', "
                + "9, 4, 60, 0, 40318, 1, 10, 0, 'none', 4, 0, 0, 0, 0, 0, "
                + "0, 128, 0, 0, 2, 0, 16, '$1480', 19, 2236, 0, 0, 0, 0);";
        String secondaryRow = "INSERT INTO `skills` VALUES (69, '集體傳送術', "
                + "9, 4, 60, 0, 40318, 1, 0, 0, 'none', 4, 0, 0, 0, 0, 0, "
                + "0, 128, 0, 0, 2, 0, 16, '$1480', 19, 2236, 0, 0, 0, 0);";
        String itemRow = "INSERT INTO `skills_item` VALUES (69, '集體傳送術', NULL, NULL);";
        assertEquals(1, count(database, selectedRow));
        assertEquals(1, count(secondary, secondaryRow));
        assertEquals(1, count(database, itemRow));
        assertEquals(1, count(secondary, itemRow));
        assertTrue(database.contains(
                "INSERT INTO `mapids` VALUES (4, '亞丁大陸', 32448, 34303,"
                        + " 32064, 33599, 1, 1, 0, 1,"));

        String assembly = read(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        int routeStart = assembly.indexOf("loc_4CBA92:");
        String route = assembly.substring(routeStart, routeStart + 600);
        assertTrue(route.contains("jumptable 004CB66E cases 4,68,130"));
        assertTrue(route.contains("call    sub_4CAB70"));
        int producerStart = assembly.indexOf("sub_4CAB70 proc near");
        int producerEnd = assembly.indexOf("sub_4CAB70 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("loc_4CAD4C:"));
        assertTrue(producer.contains("push    offset aCcchhh"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("call    sub_4C6AE0"));
        assertArrayEquals("cccdhh\0".getBytes(StandardCharsets.US_ASCII),
                peBytes(0x00636028L, 7));
        assertArrayEquals(new byte[] {0}, peBytes(0x00635A44L + 68, 1));
        assertEquals(10, Short.toUnsignedInt(ByteBuffer.wrap(
                peBytes(0x00635774L + 68L * 2L, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort()));

        String retained = read(RETAINED.resolve(
                "model/skill/skillmode/MASS_TELEPORT.java"));
        assertTrue(retained.contains("pc.getMap().isEscapable()"));
        assertTrue(retained.contains("pc.getMap().isTeleportable()"));
        assertTrue(retained.contains("randomLocation(200, true)"));
        assertTrue(retained.contains("getTileLineDistance("));
        assertTrue(retained.contains("<= 3"));
        assertTrue(retained.contains("member.getClanid() == pc.getClanid()"));
        assertTrue(retained.contains("pc.getClanid() != 0"));
        assertTrue(retained.contains("!member.isPrivateShop()"));
        assertTrue(retained.contains("new S_Message_YN(748)"));
        assertTrue(retained.contains(
                "L1Teleport.teleport(pc, newX, newY, mapId, 5, true)"));

        String answer = read(RETAINED.resolve("clientpackets/C_Attr.java"));
        assertTrue(answer.contains("case 748:"));
        assertTrue(answer.contains("pc.getTeleportX()"));
        assertTrue(answer.contains("pc.getTeleportY()"));
        assertTrue(answer.contains("pc.getTeleportMapId()"));
        assertTrue(answer.contains(
                "L1Teleport.teleport(pc, newX, newY, mapId, 5, true)"));
    }

    @Test
    void ordinaryWorldDispatcherCapabilityAndEncryptedAcceptanceAreConnected()
            throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        int worldGround = session.indexOf(
                "BattleSpellCatalog.GroundSpell worldGround");
        int worldLine = session.indexOf(
                "BattleSpellCatalog.LineAttackSpell worldLineAttack", worldGround);
        String dispatcher = session.substring(worldGround, worldLine);
        assertTrue(dispatcher.contains(
                "BattleSpellCatalog.MassTeleportSpell worldMassTeleport"));
        assertTrue(dispatcher.contains(
                "BattleSpellCatalog.massTeleportSpell(selector.skillId())"));
        assertTrue(dispatcher.contains(
                "useMassTeleportSpell(payload, worldMassTeleport)"));

        String handler = between(session,
                "private void useMassTeleportSpell(",
                "private void useMovementBuffSpell(");
        assertTrue(handler.contains("originPhase == Phase.WORLD"));
        assertTrue(handler.contains("admitClassicWorldSpellFamily("));
        assertTrue(handler.contains(
                "resolveClassicWorldMassTeleportDestination("));
        assertTrue(handler.contains("sourceMap.allowsRandomTeleport()"));
        assertTrue(handler.contains("sourceMap.allowsEscape()"));
        assertTrue(handler.contains("standingPassable("));
        assertTrue(handler.contains("exactServerBookmark"));
        assertTrue(handler.contains("int originX = positionX"));
        assertTrue(handler.contains("positionX != originX"));
        assertTrue(handler.contains(
                "BattleAnimationCatalog.get().supportsTimedAction("));
        assertTrue(handler.contains(
                "hub.classicWorldMembers(sourceMapId, sourceInstanceId)"));
        assertTrue(handler.contains("sameNonemptyClan"));
        assertTrue(handler.contains("BattleMassTeleport.withinInviteRange("));
        assertTrue(handler.contains("pendingMassTeleport = new PendingMassTeleport("));
        assertTrue(handler.contains("Protocol.massTeleportAsk()"));
        assertTrue(handler.contains("Protocol.objectAction("));
        assertTrue(handler.contains("Protocol.effectObject("));
        assertTrue(handler.contains("Protocol.requestSummon("));
        assertTrue(handler.contains("answer.questionId() != 748"));
        assertTrue(handler.contains("pendingMassTeleport = null"));
        assertTrue(handler.contains("synchronized (invitee)"));
        assertTrue(handler.contains("synchronized (firstLock)"));
        assertTrue(handler.contains("synchronized (secondLock)"));
        assertTrue(handler.contains("reserveMassTeleportPortal("));
        assertTrue(handler.contains(
                "caster.classicWorldMapId == pending.sourceMapId()"));
        assertTrue(handler.contains("caster.classicWorldInstanceId")
                && handler.contains("== pending.sourceInstanceId()"));
        assertTrue(handler.contains("now - pending.expiresAtNanos() < 0"));
        assertTrue(handler.contains("\"Mass-Teleport-69\""));

        int destination = handler.indexOf(
                "resolveClassicWorldMassTeleportDestination(",
                handler.indexOf("synchronized (this)"));
        int inventory = handler.indexOf("inventory = nextInventory;", destination);
        int mp = handler.indexOf("playerMp -= mpCost;", inventory);
        int reuse = handler.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int casterPortal = handler.indexOf(
                "reserveMassTeleportPortal(", reuse);
        int meditation = handler.indexOf(
                "cancelMeditation(\"spell-\" + spell.skillId())", casterPortal);
        int barrier = handler.indexOf(
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())", meditation);
        int invisibility = handler.indexOf(
                "revealInvisibility(\"spell-\" + spell.skillId())", barrier);
        int invitation = handler.indexOf("synchronized (invitee)", invisibility);
        int publication = handler.indexOf("Protocol.objectAction(", invitation);
        List<Integer> order = List.of(destination, inventory, mp, reuse, casterPortal,
                meditation, barrier, invisibility, invitation, publication);
        assertTrue(order.stream().allMatch(value -> value >= 0), order.toString());
        assertEquals(order.stream().sorted().toList(), order);
        assertFalse(handler.contains("commitSpellReady("));
        assertFalse(handler.contains(
                "beginMassTeleportPortal(\n                portal, \"caster\""));
        int token = handler.indexOf("invitee.pendingMassTeleport =");
        int ask = handler.indexOf("invitee.sendEncrypted(Protocol.massTeleportAsk()", token);
        assertTrue(token >= 0 && ask > token);
        int answerLock = handler.indexOf("synchronized (this)",
                handler.indexOf("private void answerMassTeleport("));
        int consume = handler.indexOf("pendingMassTeleport = null", answerLock);
        assertTrue(answerLock >= 0 && consume > answerLock);

        String portal = between(session,
                "private void returnSummon(",
                "private void useMovementBuffSpell(");
        assertTrue(portal.contains("worldClassicPortal"));
        assertTrue(portal.contains("completeClassicWorldPortal("));
        assertTrue(portal.contains("hub.transitionClassicWorld("));
        assertTrue(portal.contains("sendClassicWorldRebuild("));

        String mapCatalog = read(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldMapCatalog.java"));
        assertTrue(mapCatalog.contains(
                "MARKABLE_MAP_IDS = Set.of(0, 4, 304, 6321)"));
        assertTrue(mapCatalog.contains("boolean allowsBookmark()"));

        String acceptance = read(Path.of(
                "tools/classic_world_mass_teleport_selftest.py"));
        assertTrue(acceptance.contains(
                "request = b\"\\x14\\x08\\x04\" + struct.pack("));
        assertTrue(acceptance.contains("QUESTION = 748"));
        assertTrue(acceptance.contains("caster_and_invitee_destination_equal"));
        assertTrue(acceptance.contains("replay_blocked"));
        assertTrue(acceptance.contains("encrypted_stream_aligned"));
        assertTrue(acceptance.contains("CLAN_TEAM_ROOM = \"LegacyEmblem\""));
        String battleAcceptance = read(Path.of(
                "tools/classic_mass_teleport_selftest.py"));
        assertTrue(battleAcceptance.contains(
                "CLAN_TEAM_ROOM = \"LegacyEmblem\""));
        for (String configName : List.of(
                "src/test/resources/server-config/root/server.mass-teleport-native-battle-selftest.properties",
                "src/test/resources/server-config/root/server.world-mass-teleport-selftest.properties")) {
            String config = read(Path.of(configName));
            assertTrue(config.contains("game.bind=127.0.0.2"));
            assertTrue(config.contains("port=32100"));
            assertTrue(config.contains("database.mode=memory"));
            assertTrue(config.contains("database.read.only=true"));
            assertTrue(config.contains("state.persistence.enabled=false"));
            assertTrue(config.contains("battle.ai.enabled=false"));
        }
        String worldConfig = read(Path.of(
                "src/test/resources/server-config/root/server.world-mass-teleport-selftest.properties"));
        assertTrue(worldConfig.contains(
                "classic.world.monster.ai.enabled=false"));
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static int count(String source, String value) {
        int result = 0;
        for (int offset = source.indexOf(value); offset >= 0;
                offset = source.indexOf(value, offset + value.length())) {
            result++;
        }
        return result;
    }

    private static byte[] peBytes(long virtualAddress, int length) throws Exception {
        byte[] data = LocalResources.readAllBytes(RECOVERED);
        ByteBuffer image = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = virtualAddress - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (sectionRva <= rva
                    && rva + length <= sectionRva + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                byte[] result = new byte[length];
                System.arraycopy(data, offset, result, 0, length);
                return result;
            }
        }
        throw new IllegalArgumentException(
                "unmapped PE address " + Long.toHexString(virtualAddress));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
