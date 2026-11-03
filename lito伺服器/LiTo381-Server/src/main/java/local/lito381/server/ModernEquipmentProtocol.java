package local.lito381.server;

import static local.lito381.server.ModernEquipmentAuthority.*;
import static local.lito381.server.ModernEquipmentFacts.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;

/**
 * Modern equipment kinds 16/17. Only the result gets the 320 KiB frame bound.
 * The source is a COMPLETE, independently validated kind-15 frame, not a new
 * interpretation of saved/base values as live/effective character statistics.
 */
final class ModernEquipmentProtocol {
    static final int MAX_FRAME_BYTES = 320 * 1024;
    static final int REQUEST_BYTES = 72;
    static final int RESULT_PREFIX_BYTES = REQUEST_BYTES + 16;
    static final int SNAPSHOT_PREFIX_BYTES = 416;
    private static final int MODIFIER_COUNT = 39;

    private ModernEquipmentProtocol() {}

    /** Authority pre-commit gate: exercises the real bounded writer and wipes its output. */
    static void validateSnapshot(Snapshot snapshot) throws ModernApplicationProtocol.ProtocolException {
        if (snapshot == null) fail();
        var source = snapshot.source();
        var request = new Request(source.sourceId(), source.character().objectId(), 1, READ, false, 0, 0, ZERO);
        byte[] frame = ModernApplicationProtocol.encode(new ModernApplicationProtocol.EquipmentResult(1, request,
                new Result(OK, false, 0, 0, snapshot)), ModernSessionProtocol.MAX_VERSION);
        Arrays.fill(frame, (byte) 0);
    }

    static void putRequest(ByteBuffer out, Request request) {
        out.putInt(request.sourceId()).putLong(request.objectId()).putLong(request.epoch());
        out.put((byte) request.action()).put((byte) (request.desired() ? 1 : 0)).putShort((short) 0);
        out.putLong(request.itemObjectId()).putLong(request.expectedRevision()).put(request.expectedToken().bytes());
    }

    static Request getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        int source = in.getInt(); long object = in.getLong(), epoch = in.getLong();
        int action = Byte.toUnsignedInt(in.get()); boolean desired = bool(in);
        if (in.getShort() != 0) fail();
        long item = in.getLong(), revision = in.getLong();
        return new Request(source, object, epoch, action, desired, item, revision, fingerprint(in));
    }

    /** Includes semantic checks not expressible by the individual domain records. */
    static void validate(ModernApplicationProtocol.EquipmentResult message)
            throws ModernApplicationProtocol.ProtocolException {
        Request request = message.echo(); Result result = message.result();
        if (request == null || result == null || Stat.values().length != MODIFIER_COUNT) fail();
        if (request.action() == READ && (result.changed() || result.status() == DENIED || result.status() == CONFLICT)) fail();
        Snapshot snapshot = result.snapshot();
        if (snapshot == null) return;
        ModernWeightProtocol.validateEquipment(snapshot);
        if (snapshot.source().sourceId() != request.sourceId()
                || snapshot.source().character().objectId() != request.objectId()) fail();
        if (request.action() == SET && result.status() == OK) {
            long expected = result.changed() ? Math.addExact(request.expectedRevision(), 1) : request.expectedRevision();
            if (snapshot.revision() != expected) fail();
            boolean found = false;
            for (Equipped item : snapshot.equipped()) {
                if (item.objectId() == request.itemObjectId()) {
                    if (item.equipped() != request.desired()) fail();
                    found = true;
                }
            }
            if (!found) fail();
        }
    }

    static void putResult(ByteBuffer out, ModernApplicationProtocol.EquipmentResult message)
            throws ModernApplicationProtocol.ProtocolException {
        putResult(out, message, ModernSessionProtocol.VERSION);
    }

    static void putResult(ByteBuffer out, ModernApplicationProtocol.EquipmentResult message, int version)
            throws ModernApplicationProtocol.ProtocolException {
        validateVersion(version);
        validate(message);
        Request request = message.echo(); Result result = message.result();
        putRequest(out, request);
        out.put((byte) result.status()).put((byte) (result.changed() ? 1 : 0)).putShort((short) result.reason())
                .putLong(result.blockingItem()).put((byte) (result.snapshot() == null ? 0 : 1)).put((byte) 0).putShort((short) 0);
        Snapshot snapshot = result.snapshot();
        if (snapshot == null) return;
        if (version < 9 && snapshot.weight() != null) fail();
        if (version >= 2) validateLifetimes(snapshot);
        byte[] source = ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterDataResult(
                message.requestId(), request.sourceId(), request.objectId(), request.epoch(), 0, snapshot.source()));
        try {
            out.putLong(snapshot.revision()).put(snapshot.viewToken().bytes()).put(snapshot.catalogGeneration().bytes())
                    .putLong(snapshot.evaluatedAtMillis()).putLong(snapshot.nextStoneExpiry());
            for (Stat stat : Stat.values()) out.putLong(snapshot.modifiers().get(stat));
            out.putLong(snapshot.magicReductionBase());
            out.putShort((short) snapshot.equipped().size()).putShort((short) snapshot.stale().size()).putInt(source.length);
            out.put(source);
            for (Equipped item : snapshot.equipped()) {
                out.putLong(item.objectId()).put((byte) (item.equipped() ? 1 : 0)); zeroes(out, 7);
            }
            for (var item : snapshot.stale()) {
                out.putLong(item.itemObjectId()).putLong(item.catalogId()).put((byte) staleCode(item.reason())); zeroes(out, 7);
            }
            if (version >= 2) {
                out.putShort((short) snapshot.lifetimes().size()).putShort((short) 0);
                for (var item : snapshot.lifetimes()) {
                    out.putLong(item.itemObjectId()).put((byte) lifetimeCode(item.state())); zeroes(out, 7);
                    out.putLong(item.expiresAtMillis());
                }
            }
            if (version >= 9) ModernWeightProtocol.putOptional(out, snapshot.weight());
        } finally { Arrays.fill(source, (byte) 0); }
    }

    static ModernApplicationProtocol.EquipmentResult getResult(ByteBuffer in, int requestId)
            throws ModernApplicationProtocol.ProtocolException {
        return getResult(in, requestId, ModernSessionProtocol.VERSION);
    }

    static ModernApplicationProtocol.EquipmentResult getResult(ByteBuffer in, int requestId, int version)
            throws ModernApplicationProtocol.ProtocolException {
        validateVersion(version);
        Request request = getRequest(in);
        int status = Byte.toUnsignedInt(in.get()); boolean changed = bool(in);
        int reason = Short.toUnsignedInt(in.getShort()); long blocking = in.getLong();
        boolean present = bool(in); reserved(in, 3);
        Snapshot snapshot = null;
        if (present) {
            long revision = in.getLong();
            var token = fingerprint(in); var generation = fingerprint(in);
            long evaluatedAt = in.getLong(), expiry = in.getLong();
            if (Stat.values().length != MODIFIER_COUNT) fail();
            var values = new EnumMap<Stat, Long>(Stat.class);
            for (Stat stat : Stat.values()) values.put(stat, in.getLong());
            long magicBase = in.getLong();
            int flagCount = Short.toUnsignedInt(in.getShort()), staleCount = Short.toUnsignedInt(in.getShort());
            long sourceLength = Integer.toUnsignedLong(in.getInt());
            if (flagCount > ModernCharacterData.MAX_ITEMS || staleCount > ModernCharacterData.MAX_ITEMS
                    || sourceLength < ModernApplicationProtocol.HEADER_BYTES
                    || sourceLength > ModernCharacterDataProtocol.MAX_FRAME_BYTES
                    || (version == 1 && sourceLength + 16L * flagCount + 24L * staleCount != in.remaining())
                    || (version >= 2 && sourceLength + 16L * flagCount + 24L * staleCount + 4 > in.remaining())) fail();
            // Reject recursion/reflection BEFORE calling the general decoder. It
            // must never recurse through an attacker-provided nested gear result.
            int start = in.position();
            if (Short.toUnsignedInt(in.getShort(start + 12)) != 15) fail();
            byte[] sourceBytes = new byte[(int) sourceLength]; in.get(sourceBytes);
            ModernCharacterData.Snapshot source;
            try {
                var decoded = ModernApplicationProtocol.decode(sourceBytes);
                if (!(decoded instanceof ModernApplicationProtocol.CharacterDataResult data)
                        || data.requestId() != requestId || data.sourceId() != request.sourceId()
                        || data.objectId() != request.objectId() || data.selectionEpoch() != request.epoch()
                        || data.status() != 0 || data.snapshot() == null) {
                    decoded.close(); fail();
                }
                source = ((ModernApplicationProtocol.CharacterDataResult) decoded).snapshot();
            } finally { Arrays.fill(sourceBytes, (byte) 0); }
            var flags = new ArrayList<Equipped>(flagCount);
            for (int index = 0; index < flagCount; index++) {
                long object = in.getLong(); boolean equipped = bool(in); reserved(in, 7);
                flags.add(new Equipped(object, equipped));
            }
            var stale = new ArrayList<ModernEquipmentReconciliation.StaleItem>(staleCount);
            for (int index = 0; index < staleCount; index++) {
                long object = in.getLong(), catalog = in.getLong();
                var staleReason = staleReason(Byte.toUnsignedInt(in.get())); reserved(in, 7);
                stale.add(new ModernEquipmentReconciliation.StaleItem(object, catalog, staleReason));
            }
            var lifetimes = new ArrayList<ModernItemLifetime.View>();
            if (version >= 2) {
                int count = Short.toUnsignedInt(in.getShort()); reserved(in, 2);
                if (count != source.inventory().size() || count > ModernCharacterData.MAX_ITEMS
                        || (version < 9 ? 24L * count != in.remaining() : 24L * count + 8 > in.remaining())) fail();
                for (int index = 0; index < count; index++) {
                    long item = in.getLong(); var state = lifetimeState(Byte.toUnsignedInt(in.get())); reserved(in, 7);
                    lifetimes.add(new ModernItemLifetime.View(item, state, in.getLong()));
                }
            }
            snapshot = new Snapshot(source, revision, token, generation, evaluatedAt, expiry,
                    new Ability(values), magicBase, flags, stale, lifetimes,
                    version >= 9 ? ModernWeightProtocol.getOptional(in) : null);
            if (version >= 2) validateLifetimes(snapshot);
            ModernWeightProtocol.validateEquipment(snapshot);
        }
        return new ModernApplicationProtocol.EquipmentResult(requestId, request,
                new Result(status, changed, reason, blocking, snapshot));
    }

    private static ModernEquipmentOverrideStore.Fingerprint fingerprint(ByteBuffer in) {
        byte[] bytes = new byte[32];
        try { in.get(bytes); return new ModernEquipmentOverrideStore.Fingerprint(bytes); }
        finally { Arrays.fill(bytes, (byte) 0); }
    }
    private static void validateVersion(int version) throws ModernApplicationProtocol.ProtocolException {
        if (version < 1 || version > ModernSessionProtocol.MAX_VERSION) fail();
    }
    private static void validateLifetimes(Snapshot snapshot) throws ModernApplicationProtocol.ProtocolException {
        if (snapshot.lifetimes().size() != snapshot.source().inventory().size()
                || snapshot.nextStoneExpiry() >= 0 && snapshot.nextStoneExpiry() <= snapshot.evaluatedAtMillis()) fail();
        for (int i = 0; i < snapshot.lifetimes().size(); i++) {
            var row = snapshot.lifetimes().get(i);
            if (row.itemObjectId() != snapshot.source().inventory().get(i).objectId()) fail();
            boolean dated = row.state() == ModernItemLifetime.State.ACTIVE || row.state() == ModernItemLifetime.State.EXPIRED;
            if ((dated && row.expiresAtMillis() < 0) || (!dated && row.expiresAtMillis() != -1)
                    || (!row.usable() && snapshot.equipped().get(i).equipped())) fail();
            if (row.state() == ModernItemLifetime.State.ACTIVE
                    && (row.expiresAtMillis() <= snapshot.evaluatedAtMillis() || snapshot.nextStoneExpiry() < 0
                        || snapshot.nextStoneExpiry() > row.expiresAtMillis())) fail();
            if (row.state() == ModernItemLifetime.State.EXPIRED
                    && row.expiresAtMillis() > snapshot.evaluatedAtMillis()) fail();
        }
    }
    private static int lifetimeCode(ModernItemLifetime.State state) {
        return switch (state) {
            case UNQUALIFIED -> 0; case PERMANENT -> 1; case ACTIVE -> 2; case EXPIRED -> 3;
            case UNSTARTED_OR_MISSING -> 4; case RELATIVE_UNQUALIFIED -> 5; case LIGHTING_UNQUALIFIED -> 6;
        };
    }
    private static ModernItemLifetime.State lifetimeState(int code) throws ModernApplicationProtocol.ProtocolException {
        return switch (code) {
            case 0 -> ModernItemLifetime.State.UNQUALIFIED; case 1 -> ModernItemLifetime.State.PERMANENT;
            case 2 -> ModernItemLifetime.State.ACTIVE; case 3 -> ModernItemLifetime.State.EXPIRED;
            case 4 -> ModernItemLifetime.State.UNSTARTED_OR_MISSING; case 5 -> ModernItemLifetime.State.RELATIVE_UNQUALIFIED;
            case 6 -> ModernItemLifetime.State.LIGHTING_UNQUALIFIED;
            default -> throw new ModernApplicationProtocol.ProtocolException();
        };
    }
    private static boolean bool(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        int value = Byte.toUnsignedInt(in.get()); if (value > 1) fail(); return value != 0;
    }
    private static void zeroes(ByteBuffer out, int count) { for (int i = 0; i < count; i++) out.put((byte) 0); }
    private static void reserved(ByteBuffer in, int count) throws ModernApplicationProtocol.ProtocolException {
        for (int i = 0; i < count; i++) if (in.get() != 0) fail();
    }
    private static int staleCode(ModernEquipmentReconciliation.StaleReason reason) {
        return switch (reason) {
            case SOURCE_ITEM_MISSING -> 1;
            case SOURCE_CATALOG_CHANGED -> 2;
            case SOURCE_FACTS_CHANGED -> 3;
        };
    }
    private static ModernEquipmentReconciliation.StaleReason staleReason(int code)
            throws ModernApplicationProtocol.ProtocolException {
        return switch (code) {
            case 1 -> ModernEquipmentReconciliation.StaleReason.SOURCE_ITEM_MISSING;
            case 2 -> ModernEquipmentReconciliation.StaleReason.SOURCE_CATALOG_CHANGED;
            case 3 -> ModernEquipmentReconciliation.StaleReason.SOURCE_FACTS_CHANGED;
            default -> throw new ModernApplicationProtocol.ProtocolException();
        };
    }
    private static void fail() throws ModernApplicationProtocol.ProtocolException {
        throw new ModernApplicationProtocol.ProtocolException();
    }
}
