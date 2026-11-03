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

/** Hash-pinned proof for retained ordinary-monster potion use and packets. */
final class NativeWorldMonsterItemUseSourceAuditTest {
    private static final Path PROJECT = Path.of("").toAbsolutePath().normalize();
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path MONSTER = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1MonsterInstance.java");
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path AI = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcAI.java");
    private static final Path HASTE_PACKET = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_SkillHaste.java");
    private static final Path EFFECT_PACKET = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_SkillSound.java");
    private static final Path ITEM_IDS = RETAINED.resolve(
            "src/com/lineage/server/model/item/L1ItemId.java");
    private static final Path NPC_PACK = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_NPCPack.java");
    private static final Path REMOVE_OBJECT = RETAINED.resolve(
            "src/com/lineage/server/serverpackets/S_RemoveObject.java");
    private static final Path PLAYER = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PcInstance.java");

    @Test
    void retainedSourcesRemainHashPinned() throws Exception {
        assertEquals("B6A8694D897FC28F8BD7779541E78778D99E8E13842DB6175D58CB57CA2351A4",
                sha256(MONSTER));
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("F31A67C15C4C8473CC236E4E74D79B7CBE625DA807F0E40BEE715AC7640EFA41",
                sha256(AI));
        assertEquals("365938971888140370DAFA5B219CE48DCBC12A8ED30C4BD53B551D09BADAC09C",
                sha256(HASTE_PACKET));
        assertEquals("D59DF752B4BACBD0158D9962AFED424CC9C876D348D1154F633AC0E38B986CCB",
                sha256(EFFECT_PACKET));
        assertEquals("6D2290557CC6EBA2F869029BC6AE9B17BED7E35FD0BB3BE2BBC7CB789EE30999",
                sha256(ITEM_IDS));
        assertEquals("DEE24B66CD725B3C70A110DA0DBFBB61F9E20D6A13B00A798DFF046E0251F0AA",
                sha256(NPC_PACK));
        assertEquals("A3E3E0D61401BF29E702DDFD8055A54ABDD170B064DA24381748C12763BEC5A1",
                sha256(REMOVE_OBJECT));
        assertEquals("D652D175C31ED3A7D1F6682792BD447BC36AB8C0211A2C7A56B070C0D630C19F",
                sha256(PLAYER));
    }

    @Test
    void aiAndMonsterRetainHasteThenDoppelThenLowHpHealingOrder()
            throws Exception {
        String ai = method(readLegacy(AI), "private boolean AIProcess()",
                "public static String getMonitorLine()");
        ordered(ai, "checkTarget()", "searchTarget()", "onItemUse()",
                "is_now_target() == null", "onTarget()");

        String itemUse = method(readLegacy(MONSTER),
                "public void onItemUse()", "public void onPerceive");
        ordered(itemUse, "getMaxHp() <= 0",
                "!isActived() && (_target != null)",
                "useItem(USEITEM_HASTE, 40)", "template.is_doppel()",
                "getCurrentHp() * 100 / getMaxHp() < 40",
                "useItem(USEITEM_HEAL, 50)");
    }

    @Test
    void retainedUseItemPinsInclusiveRollPriorityEffectsAndDurations()
            throws Exception {
        String npc = readLegacy(NPC);
        String heal = method(npc, "private void useHealPotion",
                "private void useHastePotion");
        ordered(heal, "new S_SkillSound(this.getId(), effectId)",
                "hasSkillEffect(POLLUTE_WATER)", "healHp = (healHp >> 1)",
                "hasSkillEffect(ADLV80_2_2)", "healHp = (healHp >> 1)",
                "hasSkillEffect(ADLV80_2_1)", "healHp *= -1",
                "adjustCurrentHp(healHp)");
        String haste = method(npc, "private void useHastePotion",
                "public static final int USEITEM_HEAL");
        ordered(haste, "new S_SkillHaste(this.getId(), 1, time)",
                "new S_SkillSound(this.getId(), 191)", "setMoveSpeed(1)",
                "setSkillEffect(STATUS_HASTE, time * 1000)");
        String use = method(npc, "public void useItem", "public synchronized void setDirectionMoveSrc");
        ordered(use, "is_decay_potion()", "_random.nextInt(100) > chance",
                "POTION_OF_GREATER_HEALING, 1", "useHealPotion(75, 197)",
                "POTION_OF_EXTRA_HEALING, 1", "useHealPotion(45, 194)",
                "POTION_OF_HEALING, 1", "useHealPotion(15, 189)",
                "hasSkillEffect(STATUS_HASTE)",
                "B_POTION_OF_GREATER_HASTE_SELF, 1", "useHastePotion(2100)",
                "POTION_OF_GREATER_HASTE_SELF, 1", "useHastePotion(1800)",
                "B_POTION_OF_HASTE_SELF, 1", "useHastePotion(350)",
                "POTION_OF_HASTE_SELF, 1", "useHastePotion(300)");

        ordered(readLegacy(HASTE_PACKET), "writeC(S_OPCODE_SKILLHASTE)",
                "writeD(objid)", "writeC(mode)", "writeH(time)");
        ordered(readLegacy(EFFECT_PACKET), "writeC(S_OPCODE_SKILLSOUNDGFX)",
                "writeD(objid)", "writeH(gfxid)");
    }

    @Test
    void retainedDoppelCopiesBaseClassAndRebuildsRecognizedObject()
            throws Exception {
        String itemUse = method(readLegacy(MONSTER),
                "public void onItemUse()", "public void onPerceive");
        ordered(itemUse, "useItem(USEITEM_HASTE, 40)",
                "template.is_doppel()",
                "_target instanceof L1PcInstance targetPc",
                "setName(_target.getName())", "setNameId(_target.getName())",
                "setTitle(_target.getTitle())",
                "setTempLawful(_target.getLawful())",
                "setTempCharGfx(targetPc.getClassId())",
                "setGfxId(targetPc.getClassId())", "setPassispeed(640)",
                "setAtkspeed(900)", "new S_RemoveObject(this)",
                "pc.removeKnownObject(this)", "pc.updateObject()",
                "useItem(USEITEM_HEAL, 50)");
        assertTrue(readLegacy(PLAYER).contains("int getClassId()"));

        String pack = readLegacy(NPC_PACK);
        ordered(pack, "getTempCharGfx()", "getGfxId()",
                "is_doppel() && (npc.getGfxId() != 31)", "writeC(0x04)",
                "getMoveSpeed()", "getTempLawful()", "getNameId()",
                "getTitle()");
        assertTrue(pack.contains("status |= STATUS_PC"));
        ordered(readLegacy(REMOVE_OBJECT), "S_OPCODE_REMOVE_OBJECT",
                "writeD(obj.getId())");
    }

    @Test
    void productionSchedulerAndWriterPreserveOnItemUsePacketOrder()
            throws Exception {
        String hub = Files.readString(PROJECT.resolve(
                "src/main/java/local/lito381/server/GameHub.java"));
        String tick = method(hub, "private void classicWorldMonsterTick(",
                "private ClassicMonsterGroundPickup");
        ordered(tick, "population.actionReady(", "population.itemUseHaste(",
                "member.serverClassicWorldMonsterUsedItem(hasteUse)",
                "target.player()", "population.doppelTransform(",
                "member.serverClassicWorldMonsterDoppelTransformed(doppel)",
                "population.itemUseHealing(",
                "member.serverClassicWorldMonsterUsedItem(healingUse)",
                "population.beginTargetAction(",
                "actor.atkspeed() <= 0", "population.attack(",
                "population.moveToward(");

        String session = Files.readString(PROJECT.resolve(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String writer = method(session,
                "void serverClassicWorldMonsterUsedItem(",
                "void serverClassicWorldMonsterHomeReset(");
        ordered(writer, "ItemUseKind.HASTE", "Protocol.speed(",
                "Protocol.SpeedMode.HASTE", "Protocol.effectObject(");

        String doppelWriter = method(session,
                "void serverClassicWorldMonsterDoppelTransformed(",
                "void serverClassicWorldMonsterRevealed(");
        ordered(doppelWriter, "Protocol.removeObject(",
                "sendClassicWorldMonster(", "doppel-transform");

        String protocol = Files.readString(PROJECT.resolve(
                "src/main/java/local/lito381/server/Protocol.java"));
        String pack = method(protocol,
                "static byte[] classicWorldMonster(\n"
                        + "            ClassicWorldMonsterPopulation.Snapshot monster, int moveSpeed)",
                "/** Retained S_NPCPack_F/S_NPCPack actor");
        ordered(pack, "monster.gfxId()", "actorStatus", "moveSpeed",
                "monster.lawful()", "monster.nameId()", "monster.title()",
                "objectStatus");
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
