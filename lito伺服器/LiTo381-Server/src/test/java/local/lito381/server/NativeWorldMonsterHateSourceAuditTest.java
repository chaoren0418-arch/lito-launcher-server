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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for retained combat/drop hate selection and transfer. */
final class NativeWorldMonsterHateSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1MonsterInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/NpcAI.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1HateList.java"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path MONSTER = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1MonsterInstance.java");
    private static final Path HATE = RETAINED.resolve(
            "src/com/lineage/server/model/L1HateList.java");
    private static final Path AI = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcAI.java");

    @Test
    void retainedHateSourcesRemainHashPinned() throws Exception {
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("B6A8694D897FC28F8BD7779541E78778D99E8E13842DB6175D58CB57CA2351A4",
                sha256(MONSTER));
        assertEquals("85284B78C1A3DF1D3C20C3214BBA4942ED9429D9A5339C115071B3D012A10CAC",
                sha256(HATE));
        assertEquals("F31A67C15C4C8473CC236E4E74D79B7CBE625DA807F0E40BEE715AC7640EFA41",
                sha256(AI));
    }

    @Test
    void retainedSetCheckAndTransferUseSeparateCombatAndDropHate() throws Exception {
        String npc = readLegacy(NPC);
        String setHate = method(npc, "public void setHate(",
                "public void setLink(");
        ordered(setHate, "_hateList.add(cha, hate)",
                "_dropHateList.add(cha, hate)",
                "_target = _hateList.getMaxHateCharacter()", "checkTarget()");

        String check = method(npc, "public void checkTarget()",
                "private void clearTagert()");
        ordered(check, "_hateList.removeInvalidCharacter(this)",
                "_dropHateList.removeInvalidCharacter(this)",
                "_target.getMapId() != getMapId()", "_target.getCurrentHp() <= 0",
                "_target.isDead()", "get_showId() != _target.get_showId()",
                "distance > 30", "clearTagert()");
        String clear = method(npc, "private void clearTagert()",
                "public boolean isHate(");
        ordered(clear, "tagertClear()", "!_hateList.isEmpty()",
                "_target = _hateList.getMaxHateCharacter()", "checkTarget()");

        String transfer = method(npc, "public void transferHateFromPetToMaster(",
                "protected void notifyNearbyNpcsTransferPetHateToMaster(");
        ordered(transfer, "final int combat = _hateList.get(pet)",
                "final int drop = _dropHateList.get(pet)", "targetRemove(pet)",
                "_hateList.add(master, combat)", "_dropHateList.add(master, drop)",
                "_target = _hateList.getMaxHateCharacter()", "checkTarget()");

        String hate = method(readLegacy(HATE),
                "public L1Character getMaxHateCharacter()", "public void removeInvalidCharacter");
        ordered(hate, "int maxHate = Integer.MIN_VALUE",
                "for (final Map.Entry", "maxHate < val", "cha = key",
                "maxHate = val");
    }

    @Test
    void implementationPromotesMaximumAndRevalidatesFallbackBeforeSearch()
            throws Exception {
        Path main = Path.of("src", "main", "java", "local", "lito381", "server");
        String population = LocalResources.readString(
                main.resolve("ClassicWorldMonsterPopulation.java"));
        ordered(population, "actor.contributionHate.merge(key, hate",
                "actor.combatHate.merge(",
                "actor.targetObjectId = maxCombatHateTarget(actor)");
        ordered(population, "synchronized void clearTarget(",
                "actor.combatHate.remove(clearedObjectId)",
                "actor.targetObjectId = maxCombatHateTarget(actor)");
        ordered(population, "synchronized void transferTargets(",
                "actor.combatHate.remove(sourceObjectId)",
                "actor.combatHate.merge(",
                "entry.getKey().sourceObjectId() != sourceObjectId",
                "ContributorKind.PLAYER", "maxCombatHateTarget(actor)");

        String hub = LocalResources.readString(main.resolve("GameHub.java"));
        ordered(hub, "findClassicWorldMonsterTarget(",
                "while (target == null && retainedTargetId != 0)",
                "population.clearTarget(", "population.targetObjectId(",
                "target = findClassicWorldMonsterTarget(",
                "nearestClassicWorldMonsterTarget(");
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
        assertTrue(start >= 0 && end > start,
                "missing method boundary " + startText);
        return source.substring(start, end);
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
                        .digest(LocalResources.readAllBytes(path)));
    }
}
