package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for selected setLink/serchLink and mobgroup semantics. */
final class NativeWorldMonsterFamilyLinkSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path MONSTER = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1MonsterInstance.java");
    private static final Path GROUP_INFO = RETAINED.resolve(
            "src/com/lineage/server/model/L1MobGroupInfo.java");
    private static final Path GROUP_SPAWN = RETAINED.resolve(
            "src/com/lineage/server/model/L1MobGroupSpawn.java");

    @Test
    void retainedSourcesDatabaseAndGeneratedResourcesRemainPinned() throws Exception {
        assertEquals(ClassicWorldMobGroupCatalog.RETAINED_NPC_SOURCE_SHA256,
                sha256(NPC));
        assertEquals(ClassicWorldMonsterAggressionCatalog.RETAINED_MONSTER_SOURCE_SHA256,
                sha256(MONSTER));
        assertEquals(ClassicWorldMobGroupCatalog.RETAINED_GROUP_INFO_SOURCE_SHA256,
                sha256(GROUP_INFO));
        assertEquals(ClassicWorldMobGroupCatalog.RETAINED_GROUP_SPAWN_SOURCE_SHA256,
                sha256(GROUP_SPAWN));
        assertEquals(ClassicWorldMobGroupCatalog.SELECTED_DATABASE_SHA256,
                sha256(RETAINED.resolve("db/380_db.sql")));
        assertEquals(ClassicWorldMobGroupCatalog.RESOURCE_SHA256,
                sha256(Path.of("src/main/resources/world/classic-world-mob-groups.lmg.gz")));
        assertEquals(ClassicWorldMonsterAggressionCatalog.RESOURCE_SHA256,
                sha256(Path.of("src/main/resources/world/classic-world-monster-aggression.lma.gz")));
    }

    @Test
    void retainedFamilyLinkWaitsForPursuitAndOnlyLinksVisibleIdleAllies()
            throws Exception {
        String monster = readLegacy(MONSTER);
        String setLink = method(monster, "public void setLink(final L1Character cha)",
                "public L1MonsterInstance");
        ordered(setLink, "cha == null", "get_showId() != cha.get_showId()",
                "this._hateList.isEmpty()", "this._hateList.add(cha, 0)");

        String npc = readLegacy(NPC);
        String search = method(npc, "public void serchLink(",
                "protected void deferFamilyLinkFromPcAttacker");
        ordered(search, "FAMILY_LINK_LOS_REQUIRED_BEYOND_TILES",
                "targetPlayer.getKnownObjects()", "this.get_showId() != npc.get_showId()",
                "get_agrofamily() > 0", "get_agrofamily() == 1",
                "get_family() == family", "this.getMobGroupInfo()",
                "this.getMobGroupId() == npc.getMobGroupId()");
        assertTrue(npc.contains("ally.glanceCheck(targetPlayer.getX(), targetPlayer.getY())"));

        String pursuit = method(npc, "private void attack(L1Character target)",
                "private static boolean isPetOrSummon");
        assertEquals(3, occurrences(pursuit, "tryFlushDeferredFamilyLink();"));
        ordered(pursuit, "isAttackPosition", "tryFlushDeferredFamilyLink();",
                "_mobSkill.skillUse(target, true)", "tryFlushDeferredFamilyLink();",
                "_npcMove.moveDirection", "tryFlushDeferredFamilyLink();",
                "_npcMove.setDirectionMove");
    }

    @Test
    void retainedGroupSpawnAndLeaderRemovalRulesRemainDistinct() throws Exception {
        String spawn = readLegacy(GROUP_SPAWN);
        ordered(spawn, "MobGroupTable.get().getTemplate(groupId)",
                "mobGroupInfo.setRemoveGroup", "mobGroupInfo.addMember(leader)",
                "mobGroup.getMinions()", "leader.getX() + _random.nextInt(5) - 2",
                "mob.setX(leader.getX())", "mob.setreSpawn(false)",
                "mob.set_showId(leader.get_showId())");

        String info = readLegacy(GROUP_INFO);
        ordered(info, "this.setLeader(npc)", "this._membersList.add(npc)",
                "npc.setMobGroupInfo(this)", "npc.setMobGroupId(this._leader.getId())");
        String removal = method(info, "public synchronized int removeMember",
                "public int getNumOfMembers");
        ordered(removal, "this.isLeader(npc)", "this.isRemoveGroup()",
                "minion.setMobGroupInfo(null)", "minion.setreSpawn(false)",
                "this.setLeader(this._membersList.get(0))");
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int position = text.indexOf(needle); position >= 0;
             position = text.indexOf(needle, position + needle.length())) count++;
        return count;
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String method(String source, String startText, String endText) {
        int start = source.indexOf(startText);
        int end = source.indexOf(endText, start + startText.length());
        assertTrue(start >= 0 && end > start, "missing method boundary " + startText);
        return source.substring(start, end);
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // Try the next retained source encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source " + path);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
