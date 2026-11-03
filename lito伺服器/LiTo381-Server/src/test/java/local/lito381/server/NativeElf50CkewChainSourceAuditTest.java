package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Hash-pinned retained-source, selected-DB and native-client chain evidence. */
final class NativeElf50CkewChainSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SOURCE = RETAINED.resolve(
            "其他src參考用/381n8src/com/lineage/data");
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)");

    @Test
    void everyAuthoritativeInputRemainsByteExact() throws Exception {
        assertEquals(ClassicWorldCkewCatalog.DICARDING_BRIDGE_SOURCE_SHA256,
                hash(SOURCE.resolve("npc/quest/Npc_Dicarding.java")));
        assertEquals(ClassicWorldCkewCatalog.ENTRANCE_SOURCE_SHA256,
                hash(SOURCE.resolve("npc/quest/Npc_CKEW50.java")));
        assertEquals(ClassicWorldCkewCatalog.QUEST_SOURCE_SHA256,
                hash(SOURCE.resolve("quest/CKEWLv50_1.java")));
        assertEquals("F6084A4AECEA3149AB6402B90B72000DABB18317FEC9438E96D0A38ECD1CCFD1",
                hash(SOURCE.resolve("quest/ElfLv50_1.java")));
        assertEquals(ClassicWorldCkewCatalog.SELECTED_DATABASE_SHA256,
                hash(RETAINED.resolve("db/380_db.sql")));
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                hash(CLIENT.resolve("LiTo.bin")));
        assertEquals("B23C985CDAD807808E996FED31DBCB174D920B17EEE65025535EE92BF76FEA82",
                hash(CLIENT.resolve("Text.idx")));
        assertEquals("CE8FA5ADE2E69525F781B8DFF5681229FD4CFE8AC27496574E8F281182227439",
                hash(CLIENT.resolve("Text.pak")));
    }

    @Test
    void retainedExecutorPinsReportPartyEntryAndQuestLifecycle() throws Exception {
        String dicarding = text(SOURCE.resolve("npc/quest/Npc_Dicarding.java"));
        assertTrue(dicarding.contains("cmd.equalsIgnoreCase(\"p\")"));
        assertTrue(dicarding.contains("checkItemX(49163, 1)"));
        assertTrue(dicarding.contains(
                "QuestClass.get().endQuest(pc, ElfLv50_1.QUEST.get_id())"));
        assertTrue(dicarding.contains(
                "QuestClass.get().startQuest(pc, CKEWLv50_1.QUEST.get_id())"));
        assertTrue(dicarding.contains("cmd.equalsIgnoreCase(\"y\")"));
        assertTrue(dicarding.contains("cmd.equalsIgnoreCase(\"s\")"));
        assertTrue(dicarding.contains("CreateNewItem.getQuestItem(pc, npc, 184, 1)"));
        assertTrue(dicarding.contains("CreateNewItem.getQuestItem(pc, npc, 50, 1)"));

        String entrance = text(SOURCE.resolve("npc/quest/Npc_CKEW50.java"));
        assertTrue(entrance.contains("i != CKEWLv50_1.USER"));
        assertTrue(entrance.contains("cmd.equalsIgnoreCase(\"ent\")"));
        assertTrue(entrance.contains("quest.set_outStop(true)"));
        assertTrue(entrance.contains("L1Teleport.teleport(pc, 32725, 32940"));
        assertTrue(entrance.contains(
                "pc.getQuest().set_step(CKEWLv50_1.QUEST.get_id(), 2)"));

        String quest = text(SOURCE.resolve("quest/CKEWLv50_1.java"));
        assertTrue(quest.contains("public static final int MAPID = 2000"));
        assertTrue(quest.contains("public static final int USER = 15"));
        assertTrue(quest.contains("pc.getQuest().set_step(QUEST.get_id(), 1)"));
        assertTrue(quest.contains("pc.getQuest().set_end(QUEST.get_id())"));
    }

    @Test
    void selectedStoryRowsMatchFourReportsFourEntriesAndFiveRewards() {
        Map<Integer, List<Integer>> reports = Map.of(
                40000132, List.of(0, 10402),
                40000171, List.of(1, 10903),
                40000262, List.of(2, 11604),
                40000295, List.of(3, 12102));
        for (Map.Entry<Integer, List<Integer>> expected : reports.entrySet()) {
            ClassicWorldStoryQuestCatalog.Event row = event(expected.getKey());
            assertEquals(expected.getValue().get(1).intValue(), row.questId());
            assertEquals(ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID,
                    row.chainedQuestId());
            assertEquals(1 << expected.getValue().get(0), row.classMask());
            assertEquals(1, row.setStep());
        }
        assertEquals(List.of(40000302, 40000303, 40000304, 40000305),
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(row -> row.questId()
                                == ClassicWorldCkewCatalog.SELECTED_ENTRY_QUEST_ID)
                        .filter(row -> row.action().equals("ent"))
                        .map(ClassicWorldStoryQuestCatalog.Event::id).toList());
        assertEquals(List.of(40000306, 40000307, 40000308, 40000309,
                        40000310, 40000311, 40000312, 40000313,
                        40000314, 40000315),
                ClassicWorldStoryQuestCatalog.all().stream()
                        .filter(row -> row.questId()
                                == ClassicWorldCkewCatalog.SELECTED_COMPLETION_QUEST_ID)
                        .filter(row -> row.eventType()
                                == ClassicWorldStoryQuestCatalog.EventType.NPC_ACTION)
                        .map(ClassicWorldStoryQuestCatalog.Event::id).toList());
    }

    private static ClassicWorldStoryQuestCatalog.Event event(int id) {
        return ClassicWorldStoryQuestCatalog.all().stream()
                .filter(row -> row.id() == id).findFirst().orElseThrow();
    }

    private static String text(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
