package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSpellProtocol.*;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Independent v4 widths and negative cases; the old world/combat goldens remain immutable. */
class ModernSpellProtocolTest {
    private static final int ID = 0xfedcba98;
    private static final Context CONTEXT = new Context(7, 3333, -1);
    // Full u64 epoch, not a narrowed signed-positive counter. All fields are big endian.
    private static final String CONTEXT_HEX = "000000070000000000000d05ffffffffffffffff";
    static final String SKILLS_REQUEST_HEX = "4c544d3100000024fedcba9800180000" + CONTEXT_HEX;
    static final String SKILLS_RESULT_HEX = "4c544d310000004bfedcba9800190000" + CONTEXT_HEX
            + "00000000000100000000000500000000000000000000000a000000000000000a00052431343430";
    private static final String CAST_BODY_HEX = CONTEXT_HEX + "03000000"
            + "0000000000000009000000000000000600000000000000010000000500000000";
    static final String CAST_REQUEST_HEX = "4c544d3100000048fedcba98001a0000" + CAST_BODY_HEX;
    static final String CAST_NONE_HEX = "4c544d310000004cfedcba98001b0000" + CAST_BODY_HEX + "03010000";
    // Exactly 64 bytes: entity/request/count/revision/MP, skill, four coordinates, u32 policy milliseconds.
    static final String CAST_STATE_HEX = "000000000000000100000000000000010000000000000001"
            + "0000000000000002000000000000000a0000000500007fd40000801000007fd500008010000004b0";

    private static Request request() { return new Request(new ModernRooms.Scope(7, 3333, -1, 3), 9, 6, 1, 5); }
    private static ModernSkills.Skill teleport() { return new ModernSkills.Skill(5, "$1440", 10, 10, ModernSkills.Support.TELEPORT); }
    private static CastState cast() { return new CastState(1, 1, 1, 2, 10, 5, 32724, 32784, 32725, 32784, 1_200); }
    private static byte[] hex(String text) { return HexFormat.of().parseHex(text); }
    private static String hex(byte[] bytes) { return HexFormat.of().formatHex(bytes); }
    private static ModernApplicationProtocol.SkillsResult skills(List<ModernSkills.Skill> rows) {
        return new ModernApplicationProtocol.SkillsResult(ID, CONTEXT, new SkillsReply(Status.OK, rows));
    }
    private static ModernWorldProtocol.Frame world(CastState state, long revision, ModernWorldMap.Cell position) {
        var base = ModernWorldProtocolTest.frame();
        var actor = base.actors().get(0);
        var moved = new ModernBattleEngine.Actor(1, actor.member(), position, 2, true, 0, actor.stepIntervalNanos());
        return new ModernWorldProtocol.Frame(9, 6, revision, ModernWorldProtocol.Phase.ACTIVE,
                ModernWorldProtocol.AbortReason.NONE, base.room(), base.manifest(), List.of(moved), null, List.of(state));
    }
    private static ModernCombatProtocol.Vital vital(long mp, long maxMp) {
        return new ModernCombatProtocol.Vital(1, 20, 20, mp, maxMp, -40, 0, 0, 0, 0, 0, 0, 1_000_000_000);
    }
    private static ModernCombatProtocol.Frame successfulFrame() {
        return new ModernCombatProtocol.Frame(world(cast(), 2, new ModernWorldMap.Cell(32725, 32784)), List.of(vital(20, 30)));
    }
    private static ModernApplicationProtocol.CastResult success() {
        return new ModernApplicationProtocol.CastResult(ID, request(), new Reply(Status.OK, Reason.NONE, successfulFrame()));
    }
    private static byte[] encoded(ModernApplicationProtocol.Message message) throws Exception {
        return ModernApplicationProtocol.encode(message, 4);
    }
    private static void rejected(byte[] bytes) {
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes, 4));
    }

    @Test void literalKindsContextAndTargetlessCastWidthsRoundTrip() throws Exception {
        var read = new ModernApplicationProtocol.SkillsRequest(ID, CONTEXT);
        var rows = skills(List.of(teleport()));
        var command = new ModernApplicationProtocol.CastRequest(ID, request());
        var unavailable = new ModernApplicationProtocol.CastResult(ID, request(), new Reply(Status.UNAVAILABLE, Reason.NOT_ACTIVE, null));
        var messages = List.<ModernApplicationProtocol.Message>of(read, rows, command, unavailable);
        var goldens = List.of(SKILLS_REQUEST_HEX, SKILLS_RESULT_HEX, CAST_REQUEST_HEX, CAST_NONE_HEX);
        for (int i = 0; i < messages.size(); i++) {
            assertEquals(goldens.get(i), hex(encoded(messages.get(i))));
            assertEquals(messages.get(i), ModernApplicationProtocol.decode(hex(goldens.get(i)), 4));
        }
        assertEquals(20, hex(CONTEXT_HEX).length);
        assertEquals(56, hex(CAST_BODY_HEX).length);
        assertEquals(56, CAST_REQUEST_BYTES);
        assertEquals(72, encoded(command).length); // No client destination coordinate fields.
        assertEquals(-1, ((ModernApplicationProtocol.SkillsRequest) ModernApplicationProtocol.decode(hex(SKILLS_REQUEST_HEX), 4)).context().epoch());
        assertThrows(IllegalArgumentException.class, () -> new Context(7, 3333, 0));
        assertThrows(IllegalArgumentException.class, () -> new Context(0, 3333, 1));
        assertThrows(IllegalArgumentException.class, () -> new Context(7, Long.MIN_VALUE, 1));
    }

    @Test void versionsOneThroughThreeRejectAllNewKindsAndVersionFourKeepsOldBodies() throws Exception {
        var command = new ModernApplicationProtocol.CastRequest(ID, request());
        var unavailable = new ModernApplicationProtocol.CastResult(ID, request(), new Reply(Status.UNAVAILABLE, Reason.NOT_ACTIVE, null));
        for (var message : List.<ModernApplicationProtocol.Message>of(new ModernApplicationProtocol.SkillsRequest(ID, CONTEXT),
                skills(List.of(teleport())), command, unavailable, success())) {
            byte[] bytes = encoded(message);
            for (int version = 1; version <= 3; version++) {
                final int old = version;
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(message, old));
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes, old));
            }
        }
        var oldMessage = (ModernApplicationProtocol.WorldResult) ModernApplicationProtocol.decode(hex(ModernWorldProtocolTest.FRAME_HEX), 1);
        assertTrue(oldMessage.reply().frame().casts().isEmpty());
        for (int version : List.of(1, 2)) assertArrayEquals(hex(ModernWorldProtocolTest.FRAME_HEX), ModernApplicationProtocol.encode(oldMessage, version));
        byte[] v3 = ModernApplicationProtocol.encode(oldMessage, 3), v4 = encoded(oldMessage);
        byte[] expected = Arrays.copyOf(v3, v3.length + 4); // Explicit empty CastState list, not a changed old body.
        ByteBuffer.wrap(expected).putInt(4, expected.length);
        assertArrayEquals(expected, v4);
        assertEquals(oldMessage, ModernApplicationProtocol.decode(v4, 4));
        for (int version : List.of(1, 2)) {
            var oldCombat = ModernApplicationProtocol.decode(hex(ModernCombatProtocolTest.FRAME_HEX), version);
            assertArrayEquals(hex(ModernCombatProtocolTest.FRAME_HEX), ModernApplicationProtocol.encode(oldCombat, version));
        }
    }

    @Test void mixedLearnedHealArrowAndTeleportPreserveEveryFieldAcrossVersionsFourThroughThirteen() throws Exception {
        var heal = new ModernSkills.Skill(1, "$1436", 4, 10, ModernSkills.Support.BASIC_HEAL);
        var arrow = new ModernSkills.Skill(4, "$1439", 3, 10, ModernSkills.Support.LIGHT_ARROW);
        var message = skills(List.of(heal, arrow, teleport()));
        byte[] current = ModernApplicationProtocol.encode(message, 13);
        // Header16 + context20 + status4 + count4; each five-byte name
        // follows its 26-byte fixed row. Only the support byte may differ.
        assertEquals(137, current.length);
        assertEquals(4, current[48]);
        assertEquals(3, current[79]);
        assertEquals(0, current[110]);
        for (int version = 4; version <= 13; version++) {
            var healSupport = version < 13 ? ModernSkills.Support.UNIMPLEMENTED : ModernSkills.Support.BASIC_HEAL;
            var arrowSupport = version < 6 ? ModernSkills.Support.UNIMPLEMENTED : ModernSkills.Support.LIGHT_ARROW;
            var expected = skills(List.of(
                    new ModernSkills.Skill(1, "$1436", 4, 10, healSupport),
                    new ModernSkills.Skill(4, "$1439", 3, 10, arrowSupport), teleport()));
            byte[] bytes = ModernApplicationProtocol.encode(message, version);
            byte[] exact = current.clone();
            exact[48] = (byte) healSupport.ordinal();
            exact[79] = (byte) arrowSupport.ordinal();
            assertArrayEquals(exact, bytes, "only support is projected in version " + version);
            assertArrayEquals(bytes, ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes), version));
            var decoded = (ModernApplicationProtocol.SkillsResult) ModernApplicationProtocol.decode(bytes, version);
            assertEquals(expected, decoded, "version " + version);
            assertEquals(List.of(1, 4, 5), decoded.reply().skills().stream().map(ModernSkills.Skill::skillId).toList());
            assertArrayEquals(bytes, ModernApplicationProtocol.encode(decoded, version));
        }
        assertEquals(List.of(heal, arrow, teleport()), message.reply().skills(), "projection must not mutate source qualification");
    }

    @Test void unsupportedHealTemplateDowngradesForLegacyWithoutLosingSourceNameOrWideCosts() throws Exception {
        var invalidHeal = new ModernSkills.Skill(1, "來源治癒", Long.MAX_VALUE, Long.MAX_VALUE,
                ModernSkills.Support.UNSUPPORTED_TEMPLATE);
        var arrow = new ModernSkills.Skill(4, "$1439", 3, 10, ModernSkills.Support.LIGHT_ARROW);
        var message = skills(List.of(invalidHeal, arrow, teleport()));
        for (int version = 4; version <= 13; version++) {
            var expected = skills(List.of(new ModernSkills.Skill(1, invalidHeal.nameToken(), Long.MAX_VALUE, Long.MAX_VALUE,
                            version < 13 ? ModernSkills.Support.UNIMPLEMENTED : ModernSkills.Support.UNSUPPORTED_TEMPLATE),
                    new ModernSkills.Skill(4, "$1439", 3, 10,
                            version < 6 ? ModernSkills.Support.UNIMPLEMENTED : ModernSkills.Support.LIGHT_ARROW), teleport()));
            byte[] bytes = ModernApplicationProtocol.encode(message, version);
            var decoded = (ModernApplicationProtocol.SkillsResult) ModernApplicationProtocol.decode(bytes, version);
            assertEquals(expected, decoded, "version " + version);
            assertNotEquals(ModernSkills.Support.BASIC_HEAL, decoded.reply().skills().get(0).support());
            assertArrayEquals(bytes, ModernApplicationProtocol.encode(decoded, version));
        }
        assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE, invalidHeal.support());
    }

    @Test void versionsOneThroughThreeRejectMixedSkillListsAndAllAssociatedCommands() throws Exception {
        var rows = skills(List.of(new ModernSkills.Skill(1, "$1436", 4, 10, ModernSkills.Support.BASIC_HEAL),
                new ModernSkills.Skill(4, "$1439", 3, 10, ModernSkills.Support.LIGHT_ARROW), teleport()));
        var messages = List.<ModernApplicationProtocol.Message>of(
                new ModernApplicationProtocol.SkillsRequest(ID, CONTEXT), rows,
                new ModernApplicationProtocol.CastRequest(ID, request()),
                new ModernApplicationProtocol.CastRequest(ID, new Request(request().scope(), 9, 6, 1, 4, 2)),
                new ModernApplicationProtocol.CastRequest(ID, new Request(request().scope(), 9, 6, 1, 1, 3)));
        for (var message : messages) {
            byte[] bytes = ModernApplicationProtocol.encode(message, 13);
            for (int version = 1; version <= 3; version++) {
                final int old = version;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,
                        () -> ModernApplicationProtocol.encode(message, old), "encode version " + old);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,
                        () -> ModernApplicationProtocol.decode(bytes, old), "decode version " + old);
            }
        }
    }

    @Test void forgedLegacyHealOrdinalsAreRejectedEvenWhenEveryOtherMixedSkillFieldIsValid() throws Exception {
        var rows = skills(List.of(new ModernSkills.Skill(1, "$1436", 4, 10, ModernSkills.Support.BASIC_HEAL),
                new ModernSkills.Skill(4, "$1439", 3, 10, ModernSkills.Support.LIGHT_ARROW), teleport()));
        for (int version = 4; version <= 12; version++) {
            final int old = version;
            byte[] valid = ModernApplicationProtocol.encode(rows, old);
            assertEquals(ModernSkills.Support.UNIMPLEMENTED.ordinal(), valid[48]);
            assertDoesNotThrow(() -> ModernApplicationProtocol.decode(valid, old));
            for (int forgedSupport : new int[]{4, 2}) {
                byte[] forged = valid.clone();
                // Raw mutation bypasses the writer's intentional old-peer
                // projection: BASIC_HEAL4 and skill1/UNSUPPORTED_TEMPLATE2
                // are both new meanings, even though ordinal2 itself is old.
                forged[48] = (byte) forgedSupport;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,
                        () -> ModernApplicationProtocol.decode(forged, old),
                        "version " + old + " forged support " + forgedSupport);
            }
        }
        byte[] current = ModernApplicationProtocol.encode(rows, 13);
        assertEquals(rows, ModernApplicationProtocol.decode(current, 13));
        current[48] = 2;
        var unsupported = (ModernApplicationProtocol.SkillsResult) ModernApplicationProtocol.decode(current, 13);
        assertEquals(ModernSkills.Support.UNSUPPORTED_TEMPLATE, unsupported.reply().skills().get(0).support());
        assertEquals(rows.reply().skills().subList(1, 3), unsupported.reply().skills().subList(1, 3));
    }

    @Test void skillNamesStrictUtf8ByteBoundsAndFullNonnegativeI64ArePreserved() throws Exception {
        String boundary = "名".repeat(85) + "x"; // 256 UTF-8 bytes, not 256 UTF-16 units.
        var row = new ModernSkills.Skill(220, boundary, Long.MAX_VALUE, Long.MAX_VALUE, ModernSkills.Support.UNIMPLEMENTED);
        var message = skills(List.of(row));
        assertEquals(message, ModernApplicationProtocol.decode(encoded(message), 4));
        for (String invalid : List.of("名".repeat(86), "x".repeat(257), "", "\u00a0\u2007\u202f", "x\n", "\ud800")) {
            assertThrows(IllegalArgumentException.class, () -> new ModernSkills.Skill(1, invalid, 0, 0, ModernSkills.Support.UNIMPLEMENTED));
        }
        for (long[] values : new long[][]{{-1, 0}, {0, -1}}) assertThrows(IllegalArgumentException.class,
                () -> new ModernSkills.Skill(1, "N", values[0], values[1], ModernSkills.Support.UNIMPLEMENTED));
        byte[] malformed = encoded(skills(List.of(new ModernSkills.Skill(1, "x", 0, 0, ModernSkills.Support.UNIMPLEMENTED))));
        malformed[malformed.length - 1] = (byte) 0xc0;
        rejected(malformed);
        byte[] negativeMp = hex(SKILLS_RESULT_HEX); ByteBuffer.wrap(negativeMp).putLong(52, -1); rejected(negativeMp);
        byte[] negativeReuse = hex(SKILLS_RESULT_HEX); ByteBuffer.wrap(negativeReuse).putLong(60, -1); rejected(negativeReuse);
    }

    @Test void allTwoHundredTwentySkillsAreSortedUniqueAndSupportIsNotGuessed() throws Exception {
        var rows = new ArrayList<ModernSkills.Skill>();
        for (int id = 1; id <= 220; id++) rows.add(id == 5 ? teleport()
                : new ModernSkills.Skill(id, "$" + id, 0, Long.MAX_VALUE, ModernSkills.Support.UNIMPLEMENTED));
        var message = skills(rows); byte[] bytes = encoded(message);
        assertEquals(message, ModernApplicationProtocol.decode(bytes, 4));
        rows.clear(); assertEquals(220, message.reply().skills().size());
        assertThrows(UnsupportedOperationException.class, () -> message.reply().skills().clear());
        assertThrows(IllegalArgumentException.class, () -> skills(List.of(teleport(), teleport())));
        assertThrows(IllegalArgumentException.class, () -> skills(List.of(new ModernSkills.Skill(6, "N", 0, 0, ModernSkills.Support.UNIMPLEMENTED), teleport())));
        assertThrows(IllegalArgumentException.class, () -> new ModernSkills.Skill(221, "N", 0, 0, ModernSkills.Support.UNIMPLEMENTED));
        assertThrows(IllegalArgumentException.class, () -> new ModernSkills.Skill(4, "N", 10, 10, ModernSkills.Support.TELEPORT));
        assertThrows(IllegalArgumentException.class, () -> new ModernSkills.Skill(5, "N", 9, 10, ModernSkills.Support.TELEPORT));
        assertThrows(IllegalArgumentException.class, () -> new ModernSkills.Skill(5, "N", 10, 11, ModernSkills.Support.TELEPORT));
        assertThrows(IllegalArgumentException.class, () -> new ModernSkills.Skill(5, "N", 10, 10, ModernSkills.Support.UNIMPLEMENTED));
        assertDoesNotThrow(() -> new ModernSkills.Skill(5, "N", 999, 999, ModernSkills.Support.UNSUPPORTED_TEMPLATE));
        ByteBuffer.wrap(bytes).putShort(40, (short) 221); rejected(bytes);
        var denied = new ModernApplicationProtocol.SkillsResult(ID, CONTEXT, new SkillsReply(Status.DENIED, List.of()));
        assertEquals(40, encoded(denied).length);
        assertEquals(denied, ModernApplicationProtocol.decode(encoded(denied), 4));
        assertThrows(IllegalArgumentException.class, () -> new SkillsReply(Status.DENIED, List.of(teleport())));
    }

    @Test void everyTruncationTrailingCoordinateAndReservedByteIsRejected() throws Exception {
        for (byte[] good : List.of(hex(SKILLS_REQUEST_HEX), hex(SKILLS_RESULT_HEX), hex(CAST_REQUEST_HEX), hex(CAST_NONE_HEX), encoded(success()))) {
            for (int length = 0; length < good.length; length++) {
                byte[] cut = Arrays.copyOf(good, length);
                if (length >= 8) ByteBuffer.wrap(cut).putInt(4, length);
                rejected(cut);
            }
            byte[] extra = Arrays.copyOf(good, good.length + 8); ByteBuffer.wrap(extra).putInt(4, extra.length);
            rejected(extra); // Even plausible i32 destination coordinates must not be accepted.
            byte[] header = good.clone(); header[15] = 1; rejected(header);
        }
        for (int offset : new int[]{37, 38, 39, 68, 69, 70, 71}) {
            byte[] reserved = hex(CAST_REQUEST_HEX); reserved[offset] = 1; rejected(reserved);
        }
        for (int offset : new int[]{37, 38, 39, 42, 43, 49, 50, 51}) {
            byte[] reserved = hex(SKILLS_RESULT_HEX); reserved[offset] = 1; rejected(reserved);
        }
        for (int offset : new int[]{72, 73, 74, 75}) {
            byte[] invalid = hex(CAST_NONE_HEX); invalid[offset] = (byte) 255; rejected(invalid);
        }
        byte[] channel = hex(CAST_REQUEST_HEX); channel[36] = 4; rejected(channel);
        for (int offset : new int[]{40, 48, 56}) {
            byte[] zero = hex(CAST_REQUEST_HEX); ByteBuffer.wrap(zero).putLong(offset, 0); rejected(zero);
        }
    }

    @Test void castResultReusesExactAtomicCombatFrameAndRetainsCompleteEcho() throws Exception {
        var message = success(); byte[] bytes = encoded(message);
        assertEquals(27, Short.toUnsignedInt(ByteBuffer.wrap(bytes).getShort(12)));
        assertArrayEquals(hex(CAST_BODY_HEX), Arrays.copyOfRange(bytes, 16, 72));
        assertArrayEquals(new byte[]{0, 0, 1, 0}, Arrays.copyOfRange(bytes, 72, 76));
        ByteBuffer direct = ByteBuffer.allocate(ModernCombatProtocol.MAX_FRAME_BYTES);
        ModernCombatProtocol.putFrame(direct, successfulFrame(), 4);
        assertArrayEquals(Arrays.copyOf(direct.array(), direct.position()), Arrays.copyOfRange(bytes, 76, bytes.length));
        assertEquals(message, ModernApplicationProtocol.decode(bytes, 4));
        int castTail = bytes.length - 108 - 68; // One combat count+104B vital after one cast count+64B row.
        assertArrayEquals(new byte[]{0, 1, 0, 0}, Arrays.copyOfRange(bytes, castTail, castTail + 4));
        assertArrayEquals(hex(CAST_STATE_HEX), Arrays.copyOfRange(bytes, castTail + 4, castTail + 68));
        assertEquals(64, hex(CAST_STATE_HEX).length);
        assertEquals(1_200, ByteBuffer.wrap(bytes).getInt(castTail + 64));
        assertEquals(0, message.reply().frame().world().actors().get(0).lastMoveSequence()); // Teleport is not a MOVE.
        assertEquals(20, message.reply().frame().vitals().get(0).mp());
    }

    @Test void initialDeniedAndSuccessfulCastStatesHaveDistinctInvariants() throws Exception {
        ByteBuffer encoded = ByteBuffer.allocate(68); ModernSpellProtocol.putStates(encoded, List.of(CastState.initial(1, 1_200)));
        assertEquals(68, encoded.position());
        assertEquals("00010000" + "0000000000000001" + "00".repeat(52) + "000004b0", hex(encoded.array()));
        assertEquals(List.of(CastState.initial(1, 1_200)), ModernSpellProtocol.getStates(ByteBuffer.wrap(encoded.array()), 1));
        var deniedState = new CastState(1, 7, 0, 0, 0, 0, 0, 0, 0, 0, 1_200); // Denial consumes sequence, never a visual event/MP.
        var f = new ModernCombatProtocol.Frame(world(deniedState, 2, new ModernWorldMap.Cell(32724, 32784)), List.of(vital(30, 30)));
        var request = new Request(request().scope(), 9, 6, 7, 5);
        var denied = new ModernApplicationProtocol.CastResult(ID, request, new Reply(Status.DENIED, Reason.NO_MP, f));
        assertEquals(denied, ModernApplicationProtocol.decode(encoded(denied), 4));
        assertThrows(IllegalArgumentException.class, () -> new CastState(1, 0, 1, 2, 10, 5, 1, 2, 2, 2, 1_200));
        assertThrows(IllegalArgumentException.class, () -> new CastState(1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1_200));
        assertThrows(IllegalArgumentException.class, () -> new CastState(1, 1, 0, 0, 10, 0, 0, 0, 0, 0, 1_200));
        assertThrows(IllegalArgumentException.class, () -> new CastState(1, 1, 1, 2, 9, 5, 1, 2, 2, 2, 1_200));
        assertThrows(IllegalArgumentException.class, () -> new CastState(1, 1, 1, 2, 10, 5, 1, 2, 1, 2, 1_200));
        assertThrows(ArithmeticException.class, () -> new CastState(1, Long.MAX_VALUE, Long.MAX_VALUE, 2, 10, 5, 1, 2, 2, 2, 1_200));
        assertThrows(ArithmeticException.class, () -> new CastState(1, 1, 1, Long.MAX_VALUE, 10, 5, 1, 2, 2, 2, 1_200).wireRevision());
        assertEquals(3, cast().wireRevision().castRevision());
        assertEquals(1_200, cast().wireRevision().castIntervalMillis());
        assertEquals(0, CastState.initial(1, 1_200).wireRevision().castRevision());
        assertEquals(1_200, CastState.initial(1, 1_200).wireRevision().castIntervalMillis());
    }

    @Test void castCoverageReservedIdentityManifestAndRevisionAreStrict() throws Exception {
        byte[] good = encoded(success()); int start = good.length - 108 - 68;
        for (int offset : new int[]{start + 2, start + 3}) {
            byte[] invalid = good.clone(); invalid[offset] = 1; rejected(invalid);
        }
        for (int count : new int[]{2, 101}) {
            byte[] invalid = good.clone(); ByteBuffer.wrap(invalid).putShort(start, (short) count); rejected(invalid);
        }
        for (int offset : new int[]{0, 8, 16, 24, 32}) {
            byte[] invalid = good.clone(); ByteBuffer.wrap(invalid).putLong(start + 4 + offset, Long.MIN_VALUE); rejected(invalid);
        }
        byte[] wrongEntity = good.clone(); ByteBuffer.wrap(wrongEntity).putLong(start + 4, 2); rejected(wrongEntity);
        byte[] future = good.clone(); ByteBuffer.wrap(future).putLong(start + 4 + 24, 3); rejected(future);
        for (int offset : new int[]{44, 48, 52, 56}) {
            byte[] outside = good.clone(); ByteBuffer.wrap(outside).putInt(start + 4 + offset, Integer.MAX_VALUE); rejected(outside);
        }
        var base = ModernWorldProtocolTest.frame();
        assertThrows(IllegalArgumentException.class, () -> new ModernWorldProtocol.Frame(9, 6, 2, ModernWorldProtocol.Phase.PREPARING,
                ModernWorldProtocol.AbortReason.NONE, base.room(), base.manifest(), base.actors(), null, List.of(cast())));
        assertThrows(IllegalArgumentException.class, () -> new ModernWorldProtocol.Frame(9, 6, 2, base.phase(), base.abortReason(),
                base.room(), base.manifest(), base.actors(), null, List.of(CastState.initial(1, 1_200), CastState.initial(2, 1_700))));
    }

    @Test void mpCannotDivergeFromCumulativeCostEvenWhenEachNumberFitsI64() {
        var w = world(cast(), 2, new ModernWorldMap.Cell(32725, 32784));
        assertThrows(IllegalArgumentException.class, () -> new ModernCombatProtocol.Frame(w, List.of(vital(21, 30))));
        assertThrows(IllegalArgumentException.class, () -> new ModernCombatProtocol.Frame(w, List.of(vital(0, 9))));
        assertDoesNotThrow(() -> new ModernCombatProtocol.Frame(w, List.of(vital(Long.MAX_VALUE - 10, Long.MAX_VALUE))));
        var initial = world(CastState.initial(1, 1_200), 2, new ModernWorldMap.Cell(32724, 32784));
        assertThrows(IllegalArgumentException.class, () -> new ModernCombatProtocol.Frame(initial, List.of(vital(29, 30))));
    }

    @Test void successfulAckRequiresExactSelfSourceMatchGenerationSequenceRevisionSkillAndDestination() {
        var good = successfulFrame(); var original = request();
        for (Request wrong : List.of(new Request(new ModernRooms.Scope(42, 3333, -1, 3), 9, 6, 1, 5),
                new Request(original.scope(), 10, 6, 1, 5), new Request(original.scope(), 9, 7, 1, 5),
                new Request(original.scope(), 9, 6, 2, 5), new Request(original.scope(), 9, 6, 1, 6))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> encoded(new ModernApplicationProtocol.CastResult(ID, wrong, new Reply(Status.OK, Reason.NONE, good))));
        }
        var wrongPosition = new ModernCombatProtocol.Frame(world(cast(), 2, new ModernWorldMap.Cell(32724, 32784)), good.vitals());
        var staleRevision = new ModernCombatProtocol.Frame(world(cast(), 3, new ModernWorldMap.Cell(32725, 32784)), good.vitals());
        for (var wrong : List.of(wrongPosition, staleRevision)) assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> encoded(new ModernApplicationProtocol.CastResult(ID, original, new Reply(Status.OK, Reason.NONE, wrong))));
        for (Status status : Status.values()) for (Reason reason : Reason.values()) {
            if (!(status == Status.UNAVAILABLE && reason == Reason.NOT_ACTIVE))
                assertThrows(IllegalArgumentException.class, () -> new Reply(status, reason, null));
        }
    }

    @Test void finishedDenialFreezesCastHistoryWhileReceiptRevisionCanAdvance() throws Exception {
        var f = ModernSettlementProtocolTest.finished();
        var history = new CastState(1, 1, 1, 6, 10, 5, 32723, 32784, 32724, 32784, 1_200);
        var world = new ModernWorldProtocol.Frame(f.matchId(), f.generation(), f.revision(), f.phase(), f.abortReason(), f.room(),
                f.manifest(), f.actors(), f.completion(), List.of(history, CastState.initial(2, 1_700)));
        var vitals = List.of(new ModernCombatProtocol.Vital(1, 50, 50, 20, 30, -9, 0, 1, 8, 0, 0, 2, 1_000_000_000),
                new ModernCombatProtocol.Vital(2, 0, 50, 0, 0, -10, 0, 0, 0, 8, 8, 0, 1_000_000_000));
        var later = new Request(request().scope(), 9, 6, 2, 5);
        var denied = new ModernApplicationProtocol.CastResult(ID, later, new Reply(Status.DENIED, Reason.NOT_ACTIVE, new ModernCombatProtocol.Frame(world, vitals)));
        assertEquals(denied, ModernApplicationProtocol.decode(encoded(denied), 4)); // No sequence consumption after FINISHED.
        assertEquals(List.of(1_200, 1_700), denied.reply().frame().world().casts().stream().map(CastState::castIntervalMillis).toList());
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> encoded(new ModernApplicationProtocol.CastResult(ID, later,
                new Reply(Status.OK, Reason.NONE, new ModernCombatProtocol.Frame(world, vitals)))));
        var saved = new ModernWorldProtocol.Completion(f.completion().event(), ModernWorldProtocol.SettlementState.SAVED);
        assertDoesNotThrow(() -> new ModernWorldProtocol.Frame(9, 6, 9, f.phase(), f.abortReason(), f.room(), f.manifest(), f.actors(), saved,
                List.of(history, CastState.initial(2, 1_700))));
        var invented = new CastState(1, 2, 2, 9, 20, 5, 32723, 32784, 32724, 32784, 1_200);
        assertThrows(IllegalArgumentException.class, () -> new ModernWorldProtocol.Frame(9, 6, 9, f.phase(), f.abortReason(), f.room(), f.manifest(),
                f.actors(), saved, List.of(invented, CastState.initial(2, 1_700))));
    }

    @Test void castPaceIsAnExplicitBoundedU32OnInitialAndSuccessfulFrames() throws Exception {
        for (int invalid : new int[]{Integer.MIN_VALUE, -1, 0, 9, 10_001, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> CastState.initial(1, invalid));
            assertThrows(IllegalArgumentException.class, () -> new CastState(1, 1, 1, 2, 10, 5, 32724, 32784, 32725, 32784, invalid));
            byte[] bad = encoded(success());
            ByteBuffer.wrap(bad).putInt(bad.length - 108 - 4, invalid);
            rejected(bad);
        }
        for (int valid : new int[]{10, 1_200, 10_000}) {
            ByteBuffer bytes = ByteBuffer.allocate(68); ModernSpellProtocol.putStates(bytes, List.of(CastState.initial(1, valid)));
            assertEquals(valid, ByteBuffer.wrap(bytes.array()).getInt(64));
            assertEquals(List.of(CastState.initial(1, valid)), ModernSpellProtocol.getStates(ByteBuffer.wrap(bytes.array()), 1));
        }
    }

    @Test void kindSpecificInputBoundsRejectBeforeReadingOrAllocatingPayload() {
        for (int kind : new int[]{24, 25, 26, 27}) {
            int limit = kind == 27 ? ModernCombatProtocol.MAX_FRAME_BYTES : ModernApplicationProtocol.MAX_FRAME_BYTES;
            byte[] header = ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(limit + 1).putInt(ID).putShort((short) kind).putShort((short) 0).array();
            var stream = new ByteArrayInputStream(header);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.readFrame(stream));
            assertEquals(0, stream.available());
        }
    }
}
