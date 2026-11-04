package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Source- and acceptance-pinned evidence for atomic native CKEW admission. */
final class NativeCkewAdmissionAtomicitySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/data/npc/quest/Npc_CKEW50.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/data/quest/CKEWLv50_1.java"),
                Path.of("../1-伺服器/其他src參考用/381n8src/com/lineage/server/templates/L1QuestUser.java"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path SOURCE = RETAINED.resolve(
            "其他src參考用/381n8src/com/lineage/data");

    @Test
    void nativeQuestAndSelectedCompatibilityInputsRemainByteExact()
            throws Exception {
        assertEquals(ClassicWorldCkewCatalog.QUEST_SOURCE_SHA256,
                hash(SOURCE.resolve("quest/CKEWLv50_1.java")));
        assertEquals(ClassicWorldCkewCatalog.ENTRANCE_SOURCE_SHA256,
                hash(SOURCE.resolve("npc/quest/Npc_CKEW50.java")));
        assertEquals(ClassicWorldCkewCatalog.SELECTED_DATABASE_SHA256,
                hash(RETAINED.resolve("db/380_db.sql")));
        assertEquals("504CCA865633AD8B78F4AD95D722FB6BEB5F0D3EE55B1A35E2C08F4535F69191",
                hash(RETAINED.resolve(
                        "其他src參考用/381n8src/com/lineage/server/templates/"
                                + "L1QuestUser.java")));
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                hash(Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin")));
    }

    @Test
    void livePublicationAndPendingAckInvalidationAreBothSerialized()
            throws Exception {
        String instances = text(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldCkewInstances.java"));
        int publication = instances.indexOf("synchronized boolean publishAdmission(");
        int liveCheck = instances.indexOf(
                "if (instance == null || !instance.active", publication);
        int joinedCheck = instances.indexOf(
                "!instance.joinedIdentities.contains(caller)", liveCheck);
        int callback = instances.indexOf("publication.publish();", joinedCheck);
        assertTrue(publication >= 0 && liveCheck > publication);
        assertTrue(joinedCheck > liveCheck && callback > joinedCheck,
                "S_PORTAL publication must remain inside the live-show monitor");

        String session = text(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        int entry = session.indexOf("private void handleClassicWorldCkewEntry");
        int commit = session.indexOf(
                "hub.commitClassicWorldCkewAdmission(", entry);
        int publish = session.indexOf(
                "hub.publishClassicWorldCkewAdmission(", commit);
        int portal = session.indexOf(
                "sendEncrypted(Protocol.portal(destination.mapId()", publish);
        assertTrue(entry >= 0 && commit > entry && publish > commit);
        assertTrue(portal > publish,
                "native S_PORTAL must be emitted only by live admission publication");

        int complete = session.indexOf("private void completePortal(byte[] payload)");
        int lifecycleLock = session.indexOf(
                "synchronized (classicWorldPortalLifecycleLock)", complete);
        int revokedCheck = session.indexOf(
                "pendingClassicWorldPortal != classicWorldPortal", lifecycleLock);
        int ended = session.indexOf("void serverClassicWorldCkewEnded(");
        int pendingEntry = session.indexOf("boolean pendingEntry", ended);
        int invalidate = session.indexOf(
                "invalidatePendingClassicWorldCkewEntry(ended.showId())", pendingEntry);
        assertTrue(lifecycleLock > complete && revokedCheck > lifecycleLock);
        assertTrue(ended > complete && pendingEntry > ended && invalidate > pendingEntry,
                "timeout must revoke a published but unacknowledged CKEW entry");
    }

    @Test
    void durableQuestSaveIsInsideTheLiveAdmissionCriticalSection()
            throws Exception {
        String instances = text(Path.of(
                "src/main/java/local/lito381/server/ClassicWorldCkewInstances.java"));
        int liveCheck = instances.indexOf("if (instance == null || !instance.active");
        int reservationCheck = instances.indexOf(
                "if (!instance.admittingIdentities.contains(caller)) return false;");
        int durable = instances.indexOf("durableCommit.commit();", reservationCheck);
        int membership = instances.indexOf(
                "instance.admittingIdentities.remove(caller)", durable);
        assertTrue(liveCheck >= 0 && reservationCheck > liveCheck);
        assertTrue(durable > reservationCheck);
        assertTrue(membership > durable,
                "durable quest save must precede reservation-to-membership mutation");

        String session = text(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        int pending = session.indexOf(
                "pendingClassicWorldPortal = new PendingClassicWorldPortal(",
                session.indexOf("private void handleClassicWorldCkewEntry"));
        int atomicCommit = session.indexOf(
                "hub.commitClassicWorldCkewAdmission(", pending);
        int stateSave = session.indexOf(
                "() -> state.saveStoryQuestSteps(stateCharacter(), next)",
                atomicCommit);
        int rollback = session.indexOf(
                "hub.rollbackClassicWorldCkewAdmission(this, admission)",
                stateSave);
        assertTrue(pending >= 0 && atomicCommit > pending);
        assertTrue(stateSave > atomicCommit && rollback > stateSave);
    }

    @Test
    void acceptancePinsInvalidationFailureAndEncryptedPortalSuppression()
            throws Exception {
        String instancesTest = text(Path.of(
                "src/test/java/local/lito381/server/ClassicWorldCkewInstancesTest.java"));
        for (String token : new String[] {
                "invalidatedReservationNeverPersistsEnteredQuestState",
                "durableFailureLeavesReservationRollbackableAndNoJoinedMember",
                "timeoutCannotSplitDurableQuestMutationFromMembershipCommit",
                "expiry must wait until durable save and join are indivisible",
                "invalidatedCommittedAdmissionCannotPublishPortal",
                "timeoutCannotSplitLiveCheckFromPortalPublication",
                "expiry must wait for the complete effect plus S_PORTAL publish"
        }) {
            assertTrue(instancesTest.contains(token), token);
        }
        String entryTest = text(Path.of(
                "src/test/java/local/lito381/server/ClientSessionWorldCkewEntryTest.java"));
        for (String token : new String[] {
                "failedDurableEntryCannotPublishPortalOrEnteredQuestState",
                "endedShowInvalidatesPublishedEntryBeforeGotoPortal",
                "entry-atomic-state.properties",
                "pendingClassicWorldCkewEntryShowId()",
                "Protocol.S_PORTAL"
        }) {
            assertTrue(entryTest.contains(token), token);
        }
    }

    private static String text(Path path) throws Exception {
        return LocalResources.readString(path, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(LocalResources.readAllBytes(path)));
    }
}
