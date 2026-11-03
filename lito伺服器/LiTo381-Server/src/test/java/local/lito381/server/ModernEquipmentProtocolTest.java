package local.lito381.server;

import static local.lito381.server.ModernEquipmentAuthority.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernEquipmentProtocolTest {
    // Shared verbatim with Rust. Bytes are independent of the Java writer.
    static final String SET_HEX = "4c544d3100000058fedcba9800100000000000070000000000000d05ffffffffffffffff01010000"
            + "00000000000000630000000000000008000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    static final String RESULT_PREFIX_HEX = "4c544d3100000363fedcba9800110000000000070000000000000d05ffffffffffffffff01010000"
            + "00000000000000630000000000000008000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
            + "000100000000000000000000010000000000000000000009202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f"
            + "404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f00000000075bcd15ffffffffffffffff"
            + "7fffffffffffffff80000000000000000000000000000003fffffffffffffffc0000000000000005fffffffffffffffa"
            + "0000000000000007fffffffffffffff80000000000000009fffffffffffffff6000000000000000bfffffffffffffff4"
            + "000000000000000dfffffffffffffff2000000000000000ffffffffffffffff00000000000000011ffffffffffffffee"
            + "0000000000000013ffffffffffffffec0000000000000015ffffffffffffffea0000000000000017ffffffffffffffe8"
            + "0000000000000019ffffffffffffffe6000000000000001bffffffffffffffe4000000000000001dffffffffffffffe2"
            + "000000000000001fffffffffffffffe00000000000000021ffffffffffffffde0000000000000023ffffffffffffffdc"
            + "0000000000000025ffffffffffffffda000000000000002780000000000000000001000100000133";
    static final String RESULT_HEX = RESULT_PREFIX_HEX + ModernCharacterDataProtocolTest.DATA_HEX
            + "00000000000000630100000000000000000000000000009600000000000000c80200000000000000";
    private static final int ID = 0xfedcba98;

    static ModernEquipmentOverrideStore.Fingerprint fingerprint(int start) {
        byte[] bytes = new byte[32]; for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (start + i);
        return new ModernEquipmentOverrideStore.Fingerprint(bytes);
    }
    static Request set() { return new Request(7, 3333, -1, SET, true, 99, 8, fingerprint(0)); }
    static Request read() { return new Request(7, 3333, -1, READ, false, 0, 0, ZERO); }
    static Snapshot snapshot() {
        var values = new EnumMap<Stat, Long>(Stat.class);
        for (Stat stat : Stat.values()) {
            int index = stat.ordinal();
            values.put(stat, index == 0 ? Long.MAX_VALUE : index == 1 ? Long.MIN_VALUE
                    : (index + 1L) * (index % 2 == 0 ? 1 : -1));
        }
        return new Snapshot(ModernCharacterDataProtocolTest.snapshot(7, 3333), 9, fingerprint(32), fingerprint(64),
                123456789, -1, new Ability(values), Long.MIN_VALUE, List.of(new Equipped(99, true)),
                List.of(new ModernEquipmentReconciliation.StaleItem(150, 200,
                        ModernEquipmentReconciliation.StaleReason.SOURCE_CATALOG_CHANGED)));
    }
    private static ModernApplicationProtocol.EquipmentResult result(Request request, Result result) {
        return new ModernApplicationProtocol.EquipmentResult(ID, request, result);
    }
    private static ModernApplicationProtocol.EquipmentResult success() { return result(set(), new Result(OK, true, 0, 0, snapshot())); }
    private static byte[] bytes(String hex) { return HexFormat.of().parseHex(hex); }
    private static void rejected(byte[] frame) {
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(frame));
    }
    private static Snapshot changedSnapshot(Snapshot input, long revision, List<Equipped> flags) {
        return new Snapshot(input.source(), revision, input.viewToken(), input.catalogGeneration(), input.evaluatedAtMillis(),
                input.nextStoneExpiry(), input.modifiers(), input.magicReductionBase(), flags, input.stale());
    }

    @Test void crossLanguageGoldenSetAndCompleteResultPreserveAll39SignedModifiers() throws Exception {
        var request = new ModernApplicationProtocol.EquipmentRequest(ID, set());
        assertEquals(88, bytes(SET_HEX).length); assertEquals(520, bytes(RESULT_PREFIX_HEX).length);
        assertEquals(867, bytes(RESULT_HEX).length); assertEquals(39, Stat.values().length);
        assertArrayEquals(bytes(SET_HEX), ModernApplicationProtocol.encode(request));
        assertEquals(request, ModernApplicationProtocol.decode(bytes(SET_HEX)));
        assertArrayEquals(bytes(RESULT_HEX), ModernApplicationProtocol.encode(success()));
        assertEquals(success(), ModernApplicationProtocol.decode(bytes(RESULT_HEX)));
        assertEquals(Long.MIN_VALUE, success().result().snapshot().modifiers().get(Stat.DEX));
        assertEquals(-34, success().result().snapshot().modifiers().get(Stat.AC));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernEquipmentProtocol.validateSnapshot(snapshot()));
        assertDoesNotThrow(() -> ModernEquipmentProtocol.validateSnapshot(withLifetime(snapshot(), ModernItemLifetime.State.PERMANENT, -1)));
        assertFalse(request.toString().contains("3333")); assertFalse(success().toString().contains("3333"));
    }

    @Test void readAndEveryLegalStatusRoundTripWithoutInventingSnapshotsOrSuccess() throws Exception {
        var request = new ModernApplicationProtocol.EquipmentRequest(-1, read());
        assertEquals(request, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(request)));
        byte[] readFrame = ModernApplicationProtocol.encode(request);
        assertArrayEquals(new byte[52], Arrays.copyOfRange(readFrame, 36, 88));
        for (Request operation : List.of(read(), set())) {
            for (int status : new int[] {BACKEND_UNAVAILABLE, BUSY, BINDING_MISMATCH}) {
                var message = result(operation, Result.unavailable(status));
                byte[] frame = ModernApplicationProtocol.encode(message);
                assertEquals(104, frame.length); assertEquals(message, ModernApplicationProtocol.decode(frame));
            }
        }
        for (Result result : List.of(new Result(OK, false, 0, 0, snapshot()),
                new Result(DENIED, false, EquipmentRules.Reason.CURSED.ordinal() + 1, Long.MAX_VALUE, snapshot()),
                new Result(CONFLICT, false, 0, 0, snapshot()))) {
            var message = result(result.status() == OK ? read() : set(), result);
            assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        }
        var noChange = result(set(), new Result(OK, false, 0, 0, changedSnapshot(snapshot(), 8, snapshot().equipped())));
        assertEquals(noChange, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(noChange)));
    }

    private static Snapshot withLifetime(Snapshot source, ModernItemLifetime.State state, long expiry) {
        return new Snapshot(source.source(), source.revision(), source.viewToken(), source.catalogGeneration(),
                source.evaluatedAtMillis(), source.nextStoneExpiry(), source.modifiers(), source.magicReductionBase(),
                source.equipped(), source.stale(), List.of(new ModernItemLifetime.View(99, state, expiry)));
    }
    private static Snapshot withPermanentLifetimes(Snapshot source) {
        return new Snapshot(source.source(), source.revision(), source.viewToken(), source.catalogGeneration(),
                source.evaluatedAtMillis(), source.nextStoneExpiry(), source.modifiers(), source.magicReductionBase(),
                source.equipped(), source.stale(), source.source().inventory().stream()
                .map(item -> new ModernItemLifetime.View(item.objectId(), ModernItemLifetime.State.PERMANENT, -1)).toList());
    }
    private static Snapshot withTimes(Snapshot source, long evaluatedAt, long nextExpiry) {
        return new Snapshot(source.source(), source.revision(), source.viewToken(), source.catalogGeneration(),
                evaluatedAt, nextExpiry, source.modifiers(), source.magicReductionBase(),
                source.equipped(), source.stale(), source.lifetimes());
    }
    private static byte[] v2Golden() {
        byte[] legacy = bytes(RESULT_HEX);
        byte[] suffix = bytes("0001000000000000000000630100000000000000ffffffffffffffff");
        byte[] frame = Arrays.copyOf(legacy, legacy.length + suffix.length);
        System.arraycopy(suffix, 0, frame, legacy.length, suffix.length);
        ByteBuffer.wrap(frame).putInt(4, frame.length);
        return frame;
    }
    @Test void versionTwoLifetimeGoldenIsExplicitAndOldOverloadsRemainStrictVersionOne() throws Exception {
        var message = result(set(), new Result(OK, true, 0, 0, withLifetime(snapshot(), ModernItemLifetime.State.PERMANENT, -1)));
        byte[] encoded = ModernApplicationProtocol.encode(message, 2);
        assertEquals(895, encoded.length);
        assertArrayEquals(v2Golden(), encoded);
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 2));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(encoded));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(bytes(RESULT_HEX), 2));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(success(), 2));
        assertArrayEquals(bytes(RESULT_HEX), ModernApplicationProtocol.encode(message));
        assertTrue(((ModernApplicationProtocol.EquipmentResult) ModernApplicationProtocol.decode(bytes(RESULT_HEX))).result().snapshot().lifetimes().isEmpty());
        assertArrayEquals(encoded, ModernApplicationProtocol.encode(message, 3));
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 3));
        assertArrayEquals(encoded, ModernApplicationProtocol.encode(message, 4));
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 4));
        assertArrayEquals(encoded, ModernApplicationProtocol.encode(message, 5));
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 5));
        assertArrayEquals(encoded, ModernApplicationProtocol.encode(message, 6));
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 6));
        assertArrayEquals(encoded, ModernApplicationProtocol.encode(message, 7));
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 7));
        assertArrayEquals(encoded, ModernApplicationProtocol.encode(message, 8));
        assertEquals(message, ModernApplicationProtocol.decode(encoded, 8));
        byte[] v9 = ModernApplicationProtocol.encode(message, 9);
        assertEquals(encoded.length + 8, v9.length);
        assertArrayEquals(new byte[8], Arrays.copyOfRange(v9, encoded.length, v9.length));
        assertEquals(message, ModernApplicationProtocol.decode(v9, 9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(message, ModernSessionProtocol.MAX_VERSION + 1));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(encoded, 9));
    }
    @Test void allLifetimeStatesPreserveSourceInventoryAndDateShapeButNeverFakeEffectiveEquip() throws Exception {
        var base = changedSnapshot(snapshot(), 9, List.of(new Equipped(99, false)));
        assertTrue(base.source().inventory().get(0).equipped());
        for (var state : ModernItemLifetime.State.values()) {
            long expiry = state == ModernItemLifetime.State.ACTIVE ? Long.MAX_VALUE
                    : state == ModernItemLifetime.State.EXPIRED ? base.evaluatedAtMillis() : -1;
            var snap = withTimes(withLifetime(base, state, expiry), base.evaluatedAtMillis(),
                    state == ModernItemLifetime.State.ACTIVE ? expiry : -1);
            var message = result(read(), new Result(OK, false, 0, 0, snap));
            byte[] encoded = ModernApplicationProtocol.encode(message, 2);
            assertEquals(message, ModernApplicationProtocol.decode(encoded, 2));
            assertTrue(snap.source().inventory().get(0).equipped());
            assertFalse(snap.equipped().get(0).equipped());
            assertEquals(1, snap.source().inventory().size());
            assertThrows(IllegalArgumentException.class, () -> new ModernItemLifetime.View(99, state, expiry == -1 ? 0 : -1));
            if (!state.usable()) assertThrows(IllegalArgumentException.class, () -> withLifetime(snapshot(), state, expiry));
        }
    }
    @Test void versionTwoRejectsLifetimeUnknownDuplicateCountReservedAndTrailingFields() throws Exception {
        byte[] original = v2Golden();
        for (int[] mutation : new int[][] {{867,1},{870,1},{879,7},{880,1}}) {
            byte[] changed = original.clone(); changed[mutation[0]] = (byte) mutation[1];
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(changed, 2));
        }
        byte[] wrongId=original.clone(); ByteBuffer.wrap(wrongId).putLong(871,100);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(wrongId,2));
        byte[] expiredEquipped=original.clone(); expiredEquipped[879]=3; ByteBuffer.wrap(expiredEquipped).putLong(887,0);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(expiredEquipped,2));
        byte[] truncated=Arrays.copyOf(original,original.length-1); ByteBuffer.wrap(truncated).putInt(4,truncated.length);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(truncated,2));
        byte[] trailing=Arrays.copyOf(original,original.length+1); ByteBuffer.wrap(trailing).putInt(4,trailing.length);
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(trailing,2));
    }
    @Test void versionTwoLifetimeStatesAndNextDeadlineAgreeWithServerEvaluationTime() throws Exception {
        var base = changedSnapshot(snapshot(), 9, List.of(new Equipped(99, false)));
        for (long[] row : new long[][] {
                {2,2_000,1_000,1_500}, {2,2_000,1_000,2_000}, {3,1_000,1_000,-1}, {3,0,0,-1},
                {1,-1,1_000,1_001}, {1,-1,Long.MAX_VALUE,-1}, {2,Long.MAX_VALUE,Long.MAX_VALUE-1,Long.MAX_VALUE}}) {
            var state = ModernItemLifetime.State.values()[(int)row[0]];
            var snap = withTimes(withLifetime(base, state, row[1]), row[2], row[3]);
            var message = result(read(), new Result(OK, false, 0, 0, snap));
            assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message, 2), 2));
        }
        var valid = withTimes(withLifetime(base, ModernItemLifetime.State.ACTIVE, 2_000), 1_000, 1_500);
        byte[] wire = ModernApplicationProtocol.encode(result(read(), new Result(OK, false, 0, 0, valid)), 2);
        for (long[] row : new long[][] {{2,999,1_500},{2,1_000,1_001},{2,2_000,-1},{2,2_000,999},
                {2,2_000,1_000},{2,2_000,2_001},{3,1_001,-1},{1,-1,1_000}}) {
            var state = ModernItemLifetime.State.values()[(int)row[0]];
            var invalid = withTimes(withLifetime(base, state, row[1]), 1_000, row[2]);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(
                    result(read(), new Result(OK, false, 0, 0, invalid)), 2));
            byte[] changed = wire.clone(); changed[879] = (byte)row[0];
            ByteBuffer.wrap(changed).putLong(887, row[1]).putLong(184, row[2]);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(changed, 2));
        }
        // v1 has no lifetime-qualified timestamp relationship; its original contract is unchanged.
        var legacy = withTimes(base, 1_000, 1_000);
        var message = result(read(), new Result(OK, false, 0, 0, legacy));
        assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(
                result(read(), new Result(OK, false, 0, 0, withPermanentLifetimes(legacy))), 2));
    }

    @Test void allDenialReasonsAndAllStaleReasonsHaveFixedNonzeroWireValues() throws Exception {
        assertArrayEquals(new String[] {"INVALID_REQUEST", "UNKNOWN_ITEM", "UNQUALIFIED_ITEM", "NOT_EQUIPMENT",
                "UNQUALIFIED_EQUIPPED_ITEM", "INVALID_LOADOUT", "CLASS_NOT_ALLOWED", "LEVEL_TOO_LOW", "LEVEL_TOO_HIGH",
                "CURSED", "SEALED", "SLOT_OCCUPIED", "RING_LIMIT", "AID_LIMIT", "DUPLICATE_AID_CATALOG", "TALISMAN_LIMIT",
                "SHIELD_GUARDER_CONFLICT", "TWO_HANDED_SHIELD_CONFLICT", "LAYER_DEPENDENCY"},
                Arrays.stream(EquipmentRules.Reason.values()).map(Enum::name).toArray(String[]::new));
        for (var reason : EquipmentRules.Reason.values()) {
            var message = result(set(), new Result(DENIED, false, reason.ordinal() + 1, 0, snapshot()));
            byte[] encoded = ModernApplicationProtocol.encode(message);
            assertEquals(reason.ordinal() + 1, Short.toUnsignedInt(ByteBuffer.wrap(encoded).getShort(90)));
            assertEquals(message, ModernApplicationProtocol.decode(encoded));
        }
        for (var reason : ModernEquipmentReconciliation.StaleReason.values()) {
            var before = snapshot();
            var snapshot = new Snapshot(before.source(), 9, before.viewToken(), before.catalogGeneration(), 0, 0,
                    before.modifiers(), 0, before.equipped(), List.of(new ModernEquipmentReconciliation.StaleItem(1, 1, reason)));
            var message = result(read(), new Result(OK, false, 0, 0, snapshot));
            byte[] encoded = ModernApplicationProtocol.encode(message);
            assertEquals(reason.ordinal() + 1, Byte.toUnsignedInt(encoded[859]));
            assertEquals(message, ModernApplicationProtocol.decode(encoded));
        }
    }

    @Test void requestRejectsInvalidActionsBoolsReservedReadFieldsAndUnsignedOverflow() {
        byte[] original = bytes(SET_HEX);
        for (int offset : new int[] {14,15,38,39}) {
            byte[] bad = original.clone(); bad[offset] = 1; rejected(bad);
        }
        for (int[] change : new int[][] {{36,2},{36,255},{37,2},{37,255}}) {
            byte[] bad = original.clone(); bad[change[0]] = (byte) change[1]; rejected(bad);
        }
        for (int[] range : new int[][] {{8,12},{16,20},{20,28},{28,36},{40,48}}) {
            byte[] bad = original.clone(); Arrays.fill(bad, range[0], range[1], (byte) 0); rejected(bad);
        }
        for (int offset : new int[] {20,40,48}) {
            byte[] bad = original.clone(); ByteBuffer.wrap(bad).putLong(offset, Long.MIN_VALUE); rejected(bad);
        }
        byte[] negativeSource = original.clone(); ByteBuffer.wrap(negativeSource).putInt(16, -1); rejected(negativeSource);
        byte[] read = original.clone(); Arrays.fill(read, 36, 88, (byte) 0);
        assertDoesNotThrow(() -> ModernApplicationProtocol.decode(read));
        for (int offset : new int[] {37,47,55,56,87}) {
            byte[] bad = read.clone(); bad[offset] = 1; rejected(bad);
        }
    }

    @Test void requestSupportsPositiveI64ItemsAndFullUnsignedEpochAndOpaqueToken() throws Exception {
        for (long epoch : new long[] {1, Long.MAX_VALUE, Long.MIN_VALUE, -1}) {
            var request = new Request(Integer.MAX_VALUE, Long.MAX_VALUE, epoch, SET, false, Long.MAX_VALUE,
                    Long.MAX_VALUE, fingerprint(224));
            var message = new ModernApplicationProtocol.EquipmentRequest(-1, request);
            assertEquals(message, ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(message)));
        }
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(new ModernApplicationProtocol.EquipmentRequest(0, set())));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(new ModernApplicationProtocol.EquipmentRequest(1, null)));
    }

    @Test void resultRejectsStatusPresenceReasonBlockingAndChangedContradictions() {
        byte[] original = bytes(RESULT_HEX);
        for (int[] change : new int[][] {{88,6},{88,255},{89,2},{89,255},{90,1},{91,1},{99,1},{100,0},{100,2}}) {
            byte[] bad = original.clone(); bad[change[0]] = (byte) change[1]; rejected(bad);
        }
        for (int status : new int[] {1,2,3,4,5}) {
            byte[] bad = original.clone(); bad[88] = (byte) status; rejected(bad); // changed cannot accompany any failure.
        }
        for (int offset : new int[] {101,102,103}) {
            byte[] bad = original.clone(); bad[offset] = 1; rejected(bad);
        }
        for (int status : new int[] {2,3,5}) {
            byte[] bad = original.clone(); bad[88] = (byte) status; bad[89] = 0; rejected(bad); // forbidden snapshot.
        }
        for (int status : new int[] {0,1,4}) {
            byte[] bad = Arrays.copyOf(original, 104); ByteBuffer.wrap(bad).putInt(4, 104);
            bad[88] = (byte) status; bad[89] = 0; bad[100] = 0; rejected(bad);
        }
        byte[] denied = original.clone(); denied[88] = 1; denied[89] = 0;
        rejected(denied); // Missing nonzero reason.
        ByteBuffer.wrap(denied).putShort(90, (short) (EquipmentRules.Reason.values().length + 1)); rejected(denied);
        ByteBuffer.wrap(denied).putShort(90, (short) 1).putLong(92, -1); rejected(denied);
    }

    @Test void readCannotMutateBeDeniedOrConflictAndSetSuccessMustMatchRequestedFlagAndRevision() throws Exception {
        for (Request wrongContext : List.of(new Request(42, 3333, -1, READ, false, 0, 0, ZERO),
                new Request(7, 3334, -1, READ, false, 0, 0, ZERO))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(
                    result(wrongContext, new Result(OK, false, 0, 0, snapshot()))));
        }
        for (Result illegal : List.of(new Result(OK, true, 0, 0, snapshot()),
                new Result(DENIED, false, 1, 0, snapshot()), new Result(CONFLICT, false, 0, 0, snapshot()))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(result(read(), illegal)));
        }
        for (Snapshot illegal : List.of(changedSnapshot(snapshot(), 8, snapshot().equipped()),
                changedSnapshot(snapshot(), 10, snapshot().equipped()),
                changedSnapshot(snapshot(), 9, List.of(new Equipped(99, false))))) {
            assertThrows(ModernApplicationProtocol.ProtocolException.class,
                    () -> ModernApplicationProtocol.encode(result(set(), new Result(OK, true, 0, 0, illegal))));
        }
        var missingItem = new Request(7, 3333, -1, SET, true, 100, 8, fingerprint(0));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(result(missingItem, new Result(OK, true, 0, 0, snapshot()))));
        var exhausted = new Request(7, 3333, -1, SET, true, 99, Long.MAX_VALUE, fingerprint(0));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,
                () -> ModernApplicationProtocol.encode(result(exhausted, new Result(OK, true, 0, 0, snapshot()))));
        byte[] wrongRevision = bytes(RESULT_HEX); ByteBuffer.wrap(wrongRevision).putLong(104, 8); rejected(wrongRevision);
        byte[] wrongFlag = bytes(RESULT_HEX); wrongFlag[835] = 0; rejected(wrongFlag);
        byte[] noChange = bytes(RESULT_HEX); noChange[89] = 0; rejected(noChange);
        var readFrame = bytes(RESULT_HEX); Arrays.fill(readFrame, 36, 88, (byte) 0); rejected(readFrame);
    }

    @Test void nestedDataMustBeCompleteStatusZeroSameRequestSourceObjectAndEpoch() {
        for (int[] range : new int[][] {{528,532},{536,540},{540,548},{548,556}}) {
            byte[] bad = bytes(RESULT_HEX); bad[range[1] - 1] ^= 1; rejected(bad);
        }
        for (int offset : new int[] {534,535,557,558,559}) {
            byte[] bad = bytes(RESULT_HEX); bad[offset] = 1; rejected(bad);
        }
        for (int status : new int[] {1,2,3,4}) {
            byte[] bad = bytes(RESULT_HEX); bad[556] = (byte) status; rejected(bad);
        }
        byte[] wrongCharacter = bytes(RESULT_HEX); ByteBuffer.wrap(wrongCharacter).putLong(560, 3334); rejected(wrongCharacter);
        byte[] malformedLength = bytes(RESULT_HEX); ByteBuffer.wrap(malformedLength).putInt(524, 306); rejected(malformedLength);
        for (int kind : new int[] {0,1,3,14,16,17,65535}) {
            byte[] bad = bytes(RESULT_HEX); ByteBuffer.wrap(bad).putShort(532, (short) kind); rejected(bad);
        }
    }

    @Test void noNestedRecursionEvenWhenAttackerSuppliesACompleteEquipmentResult() {
        byte[] child = bytes(RESULT_HEX), original = bytes(RESULT_HEX);
        byte[] outer = new byte[520 + child.length + 40];
        System.arraycopy(original, 0, outer, 0, 520);
        System.arraycopy(child, 0, outer, 520, child.length);
        System.arraycopy(original, 827, outer, 520 + child.length, 40);
        ByteBuffer.wrap(outer).putInt(4, outer.length).putInt(516, child.length);
        rejected(outer);
    }

    @Test void snapshotBoundsCountsIdentifiersExpiryFlagsAndStaleReservedFailClosed() {
        for (int offset : new int[] {104,176}) {
            byte[] bad = bytes(RESULT_HEX); ByteBuffer.wrap(bad).putLong(offset, -1); rejected(bad);
        }
        byte[] badExpiry = bytes(RESULT_HEX); ByteBuffer.wrap(badExpiry).putLong(184, -2); rejected(badExpiry);
        for (int offset : new int[] {512,514}) for (int count : new int[] {0,2,513,65535}) {
            byte[] bad = bytes(RESULT_HEX); ByteBuffer.wrap(bad).putShort(offset, (short) count); rejected(bad);
        }
        for (int length : new int[] {0,15,306,308,262145,-1}) {
            byte[] bad = bytes(RESULT_HEX); ByteBuffer.wrap(bad).putInt(516, length); rejected(bad);
        }
        for (int offset : new int[] {827,843,851}) for (long value : new long[] {0,-1}) {
            byte[] bad = bytes(RESULT_HEX); ByteBuffer.wrap(bad).putLong(offset, value); rejected(bad);
        }
        byte[] wrongItem = bytes(RESULT_HEX); ByteBuffer.wrap(wrongItem).putLong(827, 100); rejected(wrongItem);
        for (int offset : new int[] {836,837,838,839,840,841,842,860,861,862,863,864,865,866}) {
            byte[] bad = bytes(RESULT_HEX); bad[offset] = 1; rejected(bad);
        }
        for (int[] change : new int[][] {{835,2},{835,255},{859,0},{859,4},{859,255}}) {
            byte[] bad = bytes(RESULT_HEX); bad[change[0]] = (byte) change[1]; rejected(bad);
        }
    }

    @Test void full512FlagsAndStaleWideFrameRoundTripWithNoI64Narrowing() throws Exception {
        var template = new ModernCharacterData.ItemTemplate(Long.MAX_VALUE, ModernCharacterData.ItemKind.ARMOR,
                "n".repeat(256), Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        var items = new ArrayList<ModernCharacterData.InventoryItem>(); var flags = new ArrayList<Equipped>();
        var stale = new ArrayList<ModernEquipmentReconciliation.StaleItem>();
        for (int index = 1; index <= 512; index++) {
            long id = 4_294_967_296L + index;
            items.add(new ModernCharacterData.InventoryItem(id, template, Long.MAX_VALUE, false, Long.MIN_VALUE,
                    Long.MAX_VALUE, true, 255, true, Long.MAX_VALUE));
            flags.add(new Equipped(id, index % 2 == 0));
            stale.add(new ModernEquipmentReconciliation.StaleItem(id, Long.MAX_VALUE,
                    ModernEquipmentReconciliation.StaleReason.SOURCE_FACTS_CHANGED));
        }
        var source = new ModernCharacterData.Snapshot(Integer.MAX_VALUE,
                new ModernApplicationProtocol.CharacterRow(Long.MAX_VALUE, "n".repeat(256), 65535, 1, Long.MAX_VALUE),
                ModernCharacterDataProtocolTest.persisted(), items);
        var snapshot = new Snapshot(source, Long.MAX_VALUE, fingerprint(0), fingerprint(32), Long.MAX_VALUE, Long.MAX_VALUE,
                snapshot().modifiers(), Long.MAX_VALUE, flags, stale);
        var request = new Request(Integer.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, READ, false, 0, 0, ZERO);
        var message = result(request, new Result(OK, false, 0, 0, snapshot));
        byte[] encoded = ModernApplicationProtocol.encode(message);
        assertTrue(encoded.length > ModernApplicationProtocol.MAX_FRAME_BYTES);
        assertTrue(encoded.length < ModernEquipmentProtocol.MAX_FRAME_BYTES);
        assertEquals(message, ModernApplicationProtocol.decode(encoded));
        assertArrayEquals(encoded, ModernApplicationProtocol.readFrame(new ByteArrayInputStream(encoded)));
        // No future deadline can exist at Long.MAX_VALUE; retain the old v1 bound
        // fixture above, but explicitly qualify the v2 permanent case with none.
        var qualifiedVersionTwo = withTimes(withPermanentLifetimes(snapshot), Long.MAX_VALUE, -1);
        assertDoesNotThrow(() -> ModernEquipmentProtocol.validateSnapshot(qualifiedVersionTwo));
        var versionTwo = result(request, new Result(OK, false, 0, 0, qualifiedVersionTwo));
        byte[] v2 = ModernApplicationProtocol.encode(versionTwo, 2);
        assertTrue(v2.length <= ModernEquipmentProtocol.MAX_FRAME_BYTES);
        assertEquals(versionTwo, ModernApplicationProtocol.decode(v2, 2));
        int flagsStart = 520 + ByteBuffer.wrap(encoded).getInt(516), staleStart = flagsStart + 512 * 16;
        for (int second : new int[] {flagsStart + 16, staleStart + 24}) {
            byte[] bad = encoded.clone(); ByteBuffer.wrap(bad).putLong(second, 4_294_967_297L); rejected(bad);
        }
        byte[] unorderedFlags = encoded.clone(); ByteBuffer.wrap(unorderedFlags).putLong(flagsStart, Long.MAX_VALUE); rejected(unorderedFlags);
        byte[] unorderedStale = encoded.clone(); ByteBuffer.wrap(unorderedStale).putLong(staleStart, Long.MAX_VALUE); rejected(unorderedStale);
    }

    @Test void emptyInventoryUsesZeroFlagsAndImmutableSnapshotsDoNotAliasCollectionsOrTokens() throws Exception {
        var base = snapshot();
        var source = new ModernCharacterData.Snapshot(7, base.source().character(), base.source().persisted(), List.of());
        var flags = new ArrayList<Equipped>(); var stale = new ArrayList<ModernEquipmentReconciliation.StaleItem>();
        var snapshot = new Snapshot(source, 0, ZERO, ZERO, 0, -1, Ability.zero(), 0, flags, stale);
        flags.add(new Equipped(1, true)); stale.add(new ModernEquipmentReconciliation.StaleItem(1, 1,
                ModernEquipmentReconciliation.StaleReason.SOURCE_ITEM_MISSING));
        var message = result(read(), new Result(OK, false, 0, 0, snapshot));
        byte[] frame = ModernApplicationProtocol.encode(message);
        assertEquals(739, frame.length); assertEquals(message, ModernApplicationProtocol.decode(frame));
        byte[] token = snapshot.viewToken().bytes(); token[0] = 1;
        assertArrayEquals(new byte[32], snapshot.viewToken().bytes());
        assertThrows(IllegalArgumentException.class, () -> new Snapshot(base.source(), 0, ZERO, ZERO, 0, -1,
                Ability.zero(), 0, List.of(), List.of()));
    }

    @Test void everyTruncationTrailingByteAndUnknownKindFailsClosed() {
        for (String hex : List.of(SET_HEX, RESULT_HEX)) {
            byte[] original = bytes(hex);
            for (int length = 0; length < original.length; length++) {
                byte[] shortFrame = Arrays.copyOf(original, length);
                if (length >= 16) ByteBuffer.wrap(shortFrame).putInt(4, length);
                rejected(shortFrame);
            }
            byte[] trailing = Arrays.copyOf(original, original.length + 1); ByteBuffer.wrap(trailing).putInt(4, trailing.length); rejected(trailing);
            for (int kind : new int[] {0,1,2,18,65535}) {
                byte[] bad = original.clone(); ByteBuffer.wrap(bad).putShort(12, (short) kind); rejected(bad);
            }
        }
        byte[] nullId = bytes(RESULT_HEX); Arrays.fill(nullId, 8, 12, (byte) 0); rejected(nullId);
    }

    @Test void kindSpecificBoundsAreCheckedBeforeAllocationOrReadingBody() throws Exception {
        for (int kind : new int[] {3,7,14,15,16,17}) {
            int maximum = kind == 17 ? 327680 : kind == 15 ? 262144 : 65536;
            byte[] header = Arrays.copyOf(bytes(RESULT_HEX), 16);
            ByteBuffer.wrap(header).putShort(12, (short) kind).putInt(4, maximum + 1);
            var input = new ByteArrayInputStream(header);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.readFrame(input));
            assertEquals(0, input.available());
            ByteBuffer.wrap(header).putInt(4, maximum);
            assertThrows(EOFException.class, () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header)));
        }
        assertEquals(65536, ModernApplicationProtocol.MAX_FRAME_BYTES);
        assertEquals(262144, ModernCharacterDataProtocol.MAX_FRAME_BYTES);
        assertEquals(327680, ModernEquipmentProtocol.MAX_FRAME_BYTES);
        assertEquals(40, ModernSessionProtocol.MAX_FRAME_BYTES);
        // The independent extension HELLO is 40 bytes; it does not enlarge
        // legacy HELLO/ACK or allow equipment frames through the session reader.
        var hello = new ModernSessionProtocol.Hello(1, 47, 47, ModernSessionProtocol.KNOWN_CAPABILITIES,
                ModernSessionExtensions.DEPENDENCIES, 1);
        var ack = new ModernSessionProtocol.Ack(1, 47, 1, ModernSessionProtocol.KNOWN_CAPABILITIES);
        var handshakes = List.of(ModernSessionProtocol.encode(hello), ModernSessionProtocol.encode(ack),
                ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedHello(hello, ModernSessionExtensions.HEALING)),
                ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedAck(ack, 3)));
        int[] lengths = {32, 24, 40, 28};
        for (int i = 0; i < handshakes.size(); i++) {
            byte[] frame = handshakes.get(i);
            assertEquals(lengths[i], frame.length);
            assertArrayEquals(frame, ModernSessionProtocol.readFrame(new ByteArrayInputStream(frame)));
            byte[] wrong = Arrays.copyOf(frame, frame.length + 1);
            ByteBuffer.wrap(wrong).putInt(4, wrong.length);
            var input = new ByteArrayInputStream(wrong);
            assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.readFrame(input));
            assertEquals(wrong.length - 16, input.available());
        }
        assertThrows(ModernSessionProtocol.ProtocolException.class, () -> ModernSessionProtocol.decode(bytes(SET_HEX)));
        for (int length : new int[] {15, -1, Integer.MAX_VALUE}) {
            byte[] header = Arrays.copyOf(bytes(RESULT_HEX), 16); ByteBuffer.wrap(header).putInt(4, length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header)));
        }
    }

    @Test void fragmentedAndCoalescedFramesPreserveBoundariesAndPartialFramesNeverBecomeEof() throws Exception {
        byte[] first = bytes(SET_HEX), second = bytes(RESULT_HEX);
        byte[] joined = Arrays.copyOf(first, first.length + second.length); System.arraycopy(second, 0, joined, first.length, second.length);
        var input = new ByteArrayInputStream(joined);
        InputStream fragmented = new InputStream() {
            boolean zero;
            @Override public int read() { return input.read(); }
            @Override public int read(byte[] out, int offset, int count) {
                zero = !zero; return zero ? 0 : input.read(out, offset, Math.min(3, count));
            }
        };
        assertArrayEquals(first, ModernApplicationProtocol.readFrame(fragmented)); assertEquals(second.length, input.available());
        assertArrayEquals(second, ModernApplicationProtocol.readFrame(fragmented)); assertNull(ModernApplicationProtocol.readFrame(fragmented));
        assertThrows(EOFException.class, () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(Arrays.copyOf(second, second.length - 1))));
    }
}
