package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernCharacterRecordProtocol.*;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Literal v5 record wire; an unavailable read must never be rendered as confirmed zero totals. */
class ModernCharacterRecordProtocolTest {
    private static final int REQUEST_ID = 0xfedcba98;
    private static final Context CONTEXT = new Context(7, 3333, -1);
    private static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(7, 3333);
    private static final long WINS = 0x1_0000_0001L, LOSSES = 0x2_0000_0002L, REVISION = 0x3_0000_0003L;
    private static final String CONTEXT_HEX = "000000070000000000000d05ffffffffffffffff";
    static final String REQUEST_HEX = "4c544d3100000024fedcba98001c0000" + CONTEXT_HEX;
    static final String STORED_HEX = "4c544d3100000040fedcba98001d0000" + CONTEXT_HEX
            + "00010000000000010000000100000002000000020000000300000003";
    static final String ABSENT_HEX = "4c544d3100000040fedcba98001d0000" + CONTEXT_HEX
            + "00000000000000000000000000000000000000000000000000000000";
    static final String DENIED_HEX = "4c544d3100000040fedcba98001d0000" + CONTEXT_HEX
            + "01000000000000000000000000000000000000000000000000000000";

    private static ModernCharacterRecord.Snapshot stored() {
        return new ModernCharacterRecord.Snapshot(KEY, true, WINS, LOSSES, REVISION);
    }
    private static ModernApplicationProtocol.RecordResult result(Status status, ModernCharacterRecord.Snapshot snapshot) {
        return new ModernApplicationProtocol.RecordResult(REQUEST_ID, new Reply(CONTEXT, status, snapshot));
    }
    private static byte[] hex(String text) { return HexFormat.of().parseHex(text); }
    private static byte[] encode(ModernApplicationProtocol.Message value) throws Exception { return ModernApplicationProtocol.encode(value, 5); }
    private static void rejected(byte[] bytes) {
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes, 5));
    }

    @Test void completeLiteralRequestStoredAbsentAndDeniedGoldensMatchBothDirections() throws Exception {
        var messages = List.<ModernApplicationProtocol.Message>of(new ModernApplicationProtocol.RecordRequest(REQUEST_ID, CONTEXT),
                result(Status.OK, stored()), result(Status.OK, ModernCharacterRecord.Snapshot.absent(KEY)), result(Status.DENIED, null));
        var goldens = List.of(REQUEST_HEX, STORED_HEX, ABSENT_HEX, DENIED_HEX);
        for (int i = 0; i < messages.size(); i++) {
            byte[] literal = hex(goldens.get(i));
            assertArrayEquals(literal, encode(messages.get(i)));
            assertEquals(messages.get(i), ModernApplicationProtocol.decode(literal, 5));
            assertEquals(Integer.toUnsignedLong(REQUEST_ID), Integer.toUnsignedLong(ByteBuffer.wrap(literal).getInt(8)));
            assertArrayEquals(hex(CONTEXT_HEX), Arrays.copyOfRange(literal, 16, 36));
        }
        assertEquals(36, hex(REQUEST_HEX).length); assertEquals(64, hex(STORED_HEX).length);
        assertEquals(20, hex(CONTEXT_HEX).length); assertEquals(48, hex(DENIED_HEX).length - 16);
        assertEquals(28, Short.toUnsignedInt(ByteBuffer.wrap(hex(REQUEST_HEX)).getShort(12)));
        assertEquals(29, Short.toUnsignedInt(ByteBuffer.wrap(hex(STORED_HEX)).getShort(12)));
        assertEquals(-1, ((ModernApplicationProtocol.RecordRequest) ModernApplicationProtocol.decode(hex(REQUEST_HEX), 5)).context().epoch());
    }

    @Test void absentStoredZeroAndUnavailableAreThreeDifferentTypedResults() throws Exception {
        var absent = result(Status.OK, ModernCharacterRecord.Snapshot.absent(KEY));
        var zero = result(Status.OK, new ModernCharacterRecord.Snapshot(KEY, true, 0, 0, 0));
        assertNotEquals(absent, zero); assertFalse(absent.result().snapshot().rowPresent()); assertTrue(zero.result().snapshot().rowPresent());
        byte[] expected = hex(ABSENT_HEX); expected[37] = 1;
        assertArrayEquals(expected, encode(zero)); assertEquals(zero, ModernApplicationProtocol.decode(expected, 5));
        for (Status status : List.of(Status.DENIED, Status.BUSY, Status.UNAVAILABLE)) {
            var unavailable = result(status, null); byte[] bytes = encode(unavailable);
            assertEquals(64, bytes.length); assertEquals(status.ordinal(), Byte.toUnsignedInt(bytes[36]));
            assertEquals(0, bytes[37]); assertArrayEquals(new byte[24], Arrays.copyOfRange(bytes, 40, 64));
            var parsed = (ModernApplicationProtocol.RecordResult) ModernApplicationProtocol.decode(bytes, 5);
            assertEquals(unavailable, parsed); assertNull(parsed.result().snapshot());
            assertNotEquals(absent, parsed); // No snapshot is NOT evidence of zero wins/losses.
        }
        assertThrows(IllegalArgumentException.class, () -> new Reply(CONTEXT, Status.OK, null));
        for (Status status : List.of(Status.DENIED, Status.BUSY, Status.UNAVAILABLE)) {
            assertThrows(IllegalArgumentException.class, () -> new Reply(CONTEXT, status, stored()));
            assertThrows(IllegalArgumentException.class, () -> new Reply(CONTEXT, status, ModernCharacterRecord.Snapshot.absent(KEY)));
        }
    }

    @Test void fullPositiveI64CountsAndNonzeroU64EpochSurviveWithoutNativeNarrowing() throws Exception {
        for (long epoch : new long[]{1, Long.MAX_VALUE, Long.MIN_VALUE, -1}) {
            var context = new Context(Integer.MAX_VALUE, Long.MAX_VALUE, epoch);
            var key = new TournamentPresence.CharacterKey(context.sourceId(), context.objectId());
            var snapshot = new ModernCharacterRecord.Snapshot(key, true, Long.MAX_VALUE, 0, Long.MAX_VALUE);
            var message = new ModernApplicationProtocol.RecordResult(0x80000000, new Reply(context, Status.OK, snapshot));
            var parsed = (ModernApplicationProtocol.RecordResult) ModernApplicationProtocol.decode(encode(message), 5);
            assertEquals(message, parsed); assertEquals(Long.MAX_VALUE, parsed.result().snapshot().wins());
            assertEquals(Long.MAX_VALUE, parsed.result().snapshot().revision()); assertEquals(epoch, parsed.result().context().epoch());
            assertEquals(0x80000000L, Integer.toUnsignedLong(parsed.requestId()));
        }
        assertTrue(stored().wins() > 0xffffffffL && stored().losses() > 0xffffffffL);
        assertThrows(IllegalArgumentException.class, () -> new Context(0, 3333, 1));
        assertThrows(IllegalArgumentException.class, () -> new Context(-1, 3333, 1));
        assertThrows(IllegalArgumentException.class, () -> new Context(7, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Context(7, Long.MIN_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> new Context(7, 3333, 0));
        byte[] zeroId = hex(REQUEST_HEX); ByteBuffer.wrap(zeroId).putInt(8, 0); rejected(zeroId);
    }

    @Test void everyTruncationTrailingByteAndReservedFieldIsRejected() {
        for (String golden : List.of(REQUEST_HEX, STORED_HEX, ABSENT_HEX, DENIED_HEX)) {
            byte[] good = hex(golden);
            for (int length = 0; length < good.length; length++) {
                byte[] cut = Arrays.copyOf(good, length); if (length >= 8) ByteBuffer.wrap(cut).putInt(4, length);
                rejected(cut);
            }
            byte[] trailing = Arrays.copyOf(good, good.length + 1); ByteBuffer.wrap(trailing).putInt(4, trailing.length); rejected(trailing);
            for (int offset : new int[]{14, 15}) { byte[] reserved = good.clone(); reserved[offset] = 1; rejected(reserved); }
        }
        for (int offset : new int[]{38, 39}) { byte[] reserved = hex(STORED_HEX); reserved[offset] = 1; rejected(reserved); }
        for (int value : new int[]{2, 255}) { byte[] bad = hex(STORED_HEX); bad[37] = (byte) value; rejected(bad); }
        for (int value : new int[]{4, 255}) { byte[] bad = hex(DENIED_HEX); bad[36] = (byte) value; rejected(bad); }
        for (int kind : new int[]{0, 27, 30, 65535}) {
            byte[] bad = hex(REQUEST_HEX); ByteBuffer.wrap(bad).putShort(12, (short) kind); rejected(bad);
        }
    }

    @Test void corruptCountersNegativeValuesOverflowOrRevisionMismatchNeverDecodeAsTotals() {
        for (int offset : new int[]{40, 48, 56}) {
            for (long value : new long[]{-1, Long.MIN_VALUE}) {
                byte[] bad = hex(STORED_HEX); ByteBuffer.wrap(bad).putLong(offset, value); rejected(bad);
            }
        }
        byte[] sum = hex(STORED_HEX); ByteBuffer.wrap(sum).putLong(56, REVISION + 1); rejected(sum);
        byte[] overflow = hex(STORED_HEX);
        ByteBuffer.wrap(overflow).putLong(40, Long.MAX_VALUE).putLong(48, 1).putLong(56, 0); rejected(overflow);
        byte[] unconfirmed = hex(STORED_HEX); unconfirmed[37] = 0; rejected(unconfirmed);
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, 0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, 1, 1, 3));
        assertThrows(IllegalArgumentException.class, () -> new ModernCharacterRecord.Snapshot(KEY, false, 1, 0, 1));
        assertThrows(ArithmeticException.class, () -> new ModernCharacterRecord.Snapshot(KEY, true, Long.MAX_VALUE, 1, 0));
    }

    @Test void failuresCannotSmuggleStoredFlagOrNumbersAndSnapshotKeyMustMatchSourceAndObject() {
        for (Status status : List.of(Status.DENIED, Status.BUSY, Status.UNAVAILABLE)) {
            byte[] stored = hex(STORED_HEX); stored[36] = (byte) status.ordinal(); rejected(stored);
            byte[] storedZero = hex(DENIED_HEX); storedZero[36] = (byte) status.ordinal(); storedZero[37] = 1; rejected(storedZero);
            for (int offset : new int[]{40, 48, 56}) {
                byte[] counts = hex(DENIED_HEX); counts[36] = (byte) status.ordinal(); ByteBuffer.wrap(counts).putLong(offset, 1); rejected(counts);
            }
        }
        for (var wrong : List.of(new TournamentPresence.CharacterKey(42, 3333), new TournamentPresence.CharacterKey(7, 3334))) {
            var snapshot = new ModernCharacterRecord.Snapshot(wrong, true, WINS, LOSSES, REVISION);
            assertThrows(IllegalArgumentException.class, () -> new Reply(CONTEXT, Status.OK, snapshot));
            assertThrows(IllegalArgumentException.class, () -> new Reply(CONTEXT, Status.OK, ModernCharacterRecord.Snapshot.absent(wrong)));
        }
        for (String golden : List.of(REQUEST_HEX, STORED_HEX)) {
            byte[] source = hex(golden); ByteBuffer.wrap(source).putInt(16, -1); rejected(source);
            byte[] object = hex(golden); ByteBuffer.wrap(object).putLong(20, -1); rejected(object);
            byte[] epoch = hex(golden); ByteBuffer.wrap(epoch).putLong(28, 0); rejected(epoch);
        }
    }

    @Test void versionsOneThroughFourRejectNewKindsWithoutAddingCapabilityBits() throws Exception {
        for (var message : List.<ModernApplicationProtocol.Message>of(new ModernApplicationProtocol.RecordRequest(REQUEST_ID, CONTEXT),
                result(Status.OK, stored()), result(Status.OK, ModernCharacterRecord.Snapshot.absent(KEY)), result(Status.DENIED, null))) {
            byte[] bytes = encode(message);
            for (int version = 1; version <= 4; version++) {
                final int old = version;
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(message, old));
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes, old));
            }
            assertArrayEquals(bytes, ModernApplicationProtocol.encode(message, 6));
            assertEquals(message, ModernApplicationProtocol.decode(bytes, 6));
            assertArrayEquals(bytes, ModernApplicationProtocol.encode(message, 7));
            assertEquals(message, ModernApplicationProtocol.decode(bytes, 7));
            assertArrayEquals(bytes, ModernApplicationProtocol.encode(message, 8));
            assertEquals(message, ModernApplicationProtocol.decode(bytes, 8));
            assertArrayEquals(bytes, ModernApplicationProtocol.encode(message, 9));
            assertEquals(message, ModernApplicationProtocol.decode(bytes, 9));
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(message, ModernSessionProtocol.MAX_VERSION + 1));
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes, ModernSessionProtocol.MAX_VERSION + 1));
        }
        assertEquals(2097151, ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT));
        var hello = new ModernSessionProtocol.Hello(REQUEST_ID, 1, 5, 255, 7, 1);
        for (int maximum : List.of(1, 2, 3, 4, 5)) {
            var boundary = new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true, true).withMaxVersion(maximum));
            var ack = (ModernSessionProtocol.Ack) ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(hello)));
            assertEquals(maximum, ack.version()); assertEquals(255, ack.capabilities());
        }
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.validateCapabilities(
                ModernSessionProtocol.KNOWN_CAPABILITIES & ~ModernSessionProtocol.CAP_TIMED, 7)); // All 32 bits are assigned; reject a missing dependency.
    }

    @Test void versionFiveDoesNotChangeExistingV4WorldSkillsCastOrFullGearPayload() throws Exception {
        var world = (ModernApplicationProtocol.WorldResult) ModernApplicationProtocol.decode(hex(ModernWorldProtocolTest.FRAME_HEX), 1);
        var f = world.reply().frame();
        var castWorld = new ModernWorldProtocol.Frame(f.matchId(), f.generation(), f.revision(), f.phase(), f.abortReason(),
                f.room(), f.manifest(), f.actors(), f.completion(), List.of(ModernSpellProtocol.CastState.initial(1, 1_200)));
        var worldWithCasts = new ModernApplicationProtocol.WorldResult(world.requestId(), world.echo(),
                new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK, ModernWorldProtocol.Reason.NONE, castWorld));
        var skills = ModernApplicationProtocol.decode(hex(ModernSpellProtocolTest.SKILLS_RESULT_HEX), 4);
        var cast = ModernApplicationProtocol.decode(hex(ModernSpellProtocolTest.CAST_REQUEST_HEX), 4);
        var none = ModernApplicationProtocol.decode(hex(ModernSpellProtocolTest.CAST_NONE_HEX), 4);
        var base = ModernEquipmentProtocolTest.snapshot();
        var gear = new ModernEquipmentAuthority.Snapshot(base.source(), base.revision(), base.viewToken(), base.catalogGeneration(),
                base.evaluatedAtMillis(), base.nextStoneExpiry(), base.modifiers(), base.magicReductionBase(), base.equipped(), base.stale(),
                List.of(new ModernItemLifetime.View(99, ModernItemLifetime.State.PERMANENT, -1)));
        var equipment = new ModernApplicationProtocol.EquipmentResult(REQUEST_ID, ModernEquipmentProtocolTest.set(),
                new ModernEquipmentAuthority.Result(ModernEquipmentAuthority.OK, true, 0, 0, gear));
        for (var message : List.<ModernApplicationProtocol.Message>of(world, worldWithCasts, skills, cast, none, equipment)) {
            byte[] original = ModernApplicationProtocol.encode(message, 4);
            assertArrayEquals(original, encode(message));
            assertEquals(message, ModernApplicationProtocol.decode(original, 4));
            assertEquals(message, ModernApplicationProtocol.decode(original, 5));
        }
    }

    @Test void recordsKeepGeneralFrameBudgetAndOversizeHeaderRejectsBeforeBodyAllocation() {
        for (int kind : new int[]{28, 29}) {
            int exact = kind == 28 ? 36 : 64;
            for (int length : new int[]{exact - 1, exact + 1, ModernApplicationProtocol.MAX_FRAME_BYTES + 1}) {
                byte[] header = ByteBuffer.allocate(16).putInt(0x4c544d31).putInt(length)
                        .putInt(REQUEST_ID).putShort((short) kind).putShort((short) 0).array();
                var input = new ByteArrayInputStream(header);
                assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.readFrame(input));
                assertEquals(0, input.available());
            }
        }
    }
}
