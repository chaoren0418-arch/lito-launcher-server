package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** v28 immutable selected-catalog labels; never database names or player identities. */
final class ModernNpcNames {
    static final int VERSION = 28;
    record Name(int sourceId, int npcId, int graphic, String name, String nameId) {
        Name {
            if (sourceId < 1 || npcId < 1 || !ModernNpcRoster.supportedGraphic(graphic)) throw bad();
            ModernNpcCatalog.text(name, 256, false);
            ModernNpcCatalog.text(nameId, 256, true);
        }
        long key() { return ((long) sourceId << 32) | Integer.toUnsignedLong(npcId); }
    }
    record Table(List<Name> rows) {
        Table {
            rows = List.copyOf(rows);
            if (rows.isEmpty() || rows.size() > ModernNpcCatalog.MAX_NPCS) throw bad();
            long previous = 0;
            for (var row : rows) {
                if (row.key() <= previous) throw bad();
                previous = row.key();
            }
        }
        static Table from(ModernNpcCatalog.Catalog catalog) {
            return new Table(catalog.npcs().values().stream().map(n -> new Name(n.key().sourceId(),
                    n.key().npcId(), Math.toIntExact(n.get(ModernNpcCatalog.Field.GRAPHIC)), n.name(), n.nameId()))
                    .sorted(Comparator.comparingLong(Name::key)).toList());
        }
        void validate(List<ModernNpcProtocol.Actor> actors, ModernNpcProtocol.Resources resources) {
            Objects.requireNonNull(resources);
            if (!rows.stream().map(Name::graphic).distinct().sorted().toList().equals(resources.graphics())) throw bad();
            for (var actor : actors) {
                var row = rows.stream().filter(n -> n.sourceId() == actor.catalogSourceId() && n.npcId() == actor.npcId())
                        .findFirst().orElseThrow(ModernNpcNames::bad);
                if (row.graphic() != actor.graphic()) throw bad();
            }
        }
    }
    static void put(ByteBuffer out, Table table) throws ModernApplicationProtocol.ProtocolException {
        Objects.requireNonNull(table);
        out.putShort((short) 1).putShort((short) table.rows().size()).putInt(0);
        for (var row : table.rows()) {
            out.putInt(row.sourceId()).putInt(row.npcId()).putInt(row.graphic());
            ModernApplicationProtocol.putText(out, row.name(), 256, false);
            ModernApplicationProtocol.putText(out, row.nameId(), 256, true);
        }
    }
    static Table get(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        if (in.getShort() != 1) throw bad();
        int count = Short.toUnsignedInt(in.getShort());
        if (in.getInt() != 0 || count < 1 || count > ModernNpcCatalog.MAX_NPCS) throw bad();
        var rows = new ArrayList<Name>(count);
        for (int i = 0; i < count; i++) rows.add(new Name(in.getInt(), in.getInt(), in.getInt(),
                ModernApplicationProtocol.text(in, 256, false), ModernApplicationProtocol.text(in, 256, true)));
        return new Table(rows);
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid immutable NPC name table"); }
    private ModernNpcNames() {}
}
