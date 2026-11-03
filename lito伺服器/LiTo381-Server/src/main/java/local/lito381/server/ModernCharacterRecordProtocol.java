package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Version-five selected-character totals; no fabricated result or source-table data. */
final class ModernCharacterRecordProtocol {
    private ModernCharacterRecordProtocol() {}

    record Context(int sourceId, long objectId, long epoch) {
        Context {
            if (sourceId <= 0 || objectId <= 0 || epoch == 0)
                throw new IllegalArgumentException("invalid record context");
        }
        TournamentPresence.CharacterKey key() { return new TournamentPresence.CharacterKey(sourceId, objectId); }
    }
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    record Reply(Context context, Status status, ModernCharacterRecord.Snapshot snapshot) {
        Reply {
            Objects.requireNonNull(context); Objects.requireNonNull(status);
            if ((status == Status.OK) != (snapshot != null)
                    || snapshot != null && !snapshot.key().equals(context.key()))
                throw new IllegalArgumentException("invalid record reply");
        }
    }

    static void putContext(ByteBuffer out, Context context) {
        out.putInt(context.sourceId()).putLong(context.objectId()).putLong(context.epoch());
    }
    static Context getContext(ByteBuffer in) { return new Context(in.getInt(), in.getLong(), in.getLong()); }
    static void putReply(ByteBuffer out, Reply reply) {
        putContext(out, reply.context());
        var row = reply.snapshot();
        out.put((byte) reply.status().ordinal()).put((byte) (row != null && row.rowPresent() ? 1 : 0)).putShort((short) 0);
        out.putLong(row == null ? 0 : row.wins()).putLong(row == null ? 0 : row.losses()).putLong(row == null ? 0 : row.revision());
    }
    static Reply getReply(ByteBuffer in) {
        var context = getContext(in);
        int status = Byte.toUnsignedInt(in.get()), stored = Byte.toUnsignedInt(in.get());
        if (status >= Status.values().length || stored > 1 || in.getShort() != 0)
            throw new IllegalArgumentException("invalid record header");
        long wins = in.getLong(), losses = in.getLong(), revision = in.getLong();
        if (status != 0) {
            if (stored != 0 || wins != 0 || losses != 0 || revision != 0)
                throw new IllegalArgumentException("non-success record has totals");
            return new Reply(context, Status.values()[status], null);
        }
        return new Reply(context, Status.OK, new ModernCharacterRecord.Snapshot(context.key(), stored == 1, wins, losses, revision));
    }
}
