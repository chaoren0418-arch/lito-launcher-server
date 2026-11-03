package local.lito381.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Startup-only dedicated SQL layout facts. Never grants a map, resource or gameplay capability. */
final class ModernWorldPolicy {
    static final int SCHEMA_VERSION = 1, MAX_BINDINGS = 40;
    private static final Comparator<Row> ORDER = Comparator.comparingInt((Row r) -> r.key().mode())
            .thenComparingInt(r -> r.key().logicalMapId());
    record Pin(String namespaceSha256, long revision, String contentSha256) {
        Pin { hash(namespaceSha256); hash(contentSha256); if (revision < 1) throw invalid(); }
        @Override public String toString() { return "ModernWorldPolicyPin[redacted]"; }
    }
    record Row(ModernWorldBinding.Key key, int physicalMapId, String manifestSha256,
               ModernWorldMap.Cell anchor, List<ModernWorldMap.Cell> teamCamps,
               ModernWorldService.ClockOverrides clocks) {
        Row {
            Objects.requireNonNull(key); Objects.requireNonNull(anchor); hash(manifestSha256);
            TournamentRoomPolicy.rules(key.mode(), key.logicalMapId());
            if (physicalMapId < 0 || physicalMapId > 65535) throw invalid();
            teamCamps = List.copyOf(teamCamps);
            if(clocks!=null&&clocks.loadTimeoutMillis()==null&&clocks.countdownNanos()==null&&clocks.activeTimeoutMillis()==null)clocks=null;
            if (!teamCamps.isEmpty() && (teamCamps.size() != 2 || teamCamps.get(0).equals(teamCamps.get(1))
                    || !BattleRules.Mode.fromWire(key.mode()).teamBased())) throw invalid();
        }
        @Override public String toString() { return "ModernWorldPolicyRow[redacted]"; }
    }
    record Snapshot(Pin pin, List<Row> rows) {
        Snapshot {
            Objects.requireNonNull(pin); rows = List.copyOf(rows);
            if (rows.isEmpty() || rows.size() > MAX_BINDINGS) throw invalid();
            for (int i = 1; i < rows.size(); i++) if (ORDER.compare(rows.get(i - 1), rows.get(i)) >= 0) throw invalid();
            if (!pin.contentSha256().equals(contentHash(pin.namespaceSha256(), pin.revision(), rows))) throw invalid();
        }
        Map<ModernWorldBinding.Key, Row> byBinding() {
            return rows.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Row::key, r -> r));
        }
        @Override public String toString() { return "ModernWorldPolicySnapshot[redacted]"; }
    }
    private ModernWorldPolicy() {}

    /** Canonical v1: big-endian integers; explicit nullable flags; sorted complete binding set. */
    static String contentHash(String namespace, long revision, List<Row> rows) {
        hash(namespace); if (revision < 1 || rows == null || rows.isEmpty() || rows.size() > MAX_BINDINGS) throw invalid();
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                out.writeUTF("LITO_MODERN_WORLD_POLICY_V1"); out.writeInt(SCHEMA_VERSION);
                out.write(HexFormat.of().parseHex(namespace)); out.writeLong(revision); out.writeInt(rows.size());
                Row prior = null;
                for (var row : rows) {
                    if (row == null || prior != null && ORDER.compare(prior, row) >= 0) throw invalid();
                    out.writeInt(row.key().mode()); out.writeInt(row.key().logicalMapId()); out.writeInt(row.physicalMapId());
                    out.write(HexFormat.of().parseHex(row.manifestSha256())); out.writeInt(row.anchor().x()); out.writeInt(row.anchor().y());
                    out.writeBoolean(!row.teamCamps().isEmpty());
                    for (var camp : row.teamCamps()) { out.writeInt(camp.x()); out.writeInt(camp.y()); }
                    var clocks = row.clocks();
                    integer(out, clocks == null ? null : clocks.loadTimeoutMillis());
                    Long countdown = clocks == null ? null : clocks.countdownNanos();
                    out.writeBoolean(countdown != null); if (countdown != null) out.writeLong(countdown);
                    integer(out, clocks == null ? null : clocks.activeTimeoutMillis()); prior = row;
                }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (java.security.NoSuchAlgorithmException | IOException impossible) { throw new IllegalStateException("world policy hash unavailable"); }
    }
    private static void integer(DataOutputStream out, Integer value) throws IOException {
        out.writeBoolean(value != null); if (value != null) out.writeInt(value);
    }
    /** SQL NULL retains the local override; only then does the existing global fallback resolve. */
    static ModernWorldService.ClockOverrides clocks(ModernWorldService.ClockOverrides local, ModernWorldService.ClockOverrides sql) {
        if (sql == null) return local;
        return new ModernWorldService.ClockOverrides(sql.loadTimeoutMillis() != null ? sql.loadTimeoutMillis() : local == null ? null : local.loadTimeoutMillis(),
                sql.countdownNanos() != null ? sql.countdownNanos() : local == null ? null : local.countdownNanos(),
                sql.activeTimeoutMillis() != null ? sql.activeTimeoutMillis() : local == null ? null : local.activeTimeoutMillis());
    }
    /** Representative maximum-capacity layouts; every actual typed roster is still allocated and requalified at PREPARE. */
    static void qualifyLayout(ModernWorldService.Mapping mapping, int localCapacity, BooleanSupplier cancelled) throws ModernWorldMap.MapException {
        if (cancelled.getAsBoolean()) throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CANCELLED);
        int capacity = Math.min(localCapacity, TournamentRoomPolicy.combatantCapacity(100, BattleRules.Mode.fromWire(mapping.binding().key().mode())));
        if (capacity < TournamentRoomPolicy.rules(mapping.binding().key().mode(), mapping.binding().key().logicalMapId()).minimumPlayers()) throw invalid();
        Set<ModernWorldMap.Cell> objectives = mapping.honor() == null ? Set.of() : mapping.honor().towers().stream()
                .map(ModernHonor.Tower::cell).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if(mapping.npc()!=null)objectives=mapping.npc().reservedCells();
        if(!ModernQuestSpace.isolated(mapping)) {
            if (objectives.contains(mapping.anchor())) throw invalid();
            for (var camp : mapping.spawnCamps()) if (objectives.contains(camp)) throw invalid();
        }
        for (int variant = 0; variant < (mapping.serverBots() ? 2 : 1); variant++) {
            var roster = new ArrayList<ModernRooms.Member>(capacity);
            for (int i = 0; i < capacity; i++) {
                var identity = variant == 1 && i > 0 ? ModernParticipantId.bot(1, i + 1L) : ModernParticipantId.human(1, i + 1L);
                roster.add(new ModernRooms.Member(identity, "qualification", "qualification", 80, 1, 0, mapping.timeAttack()!=null?0:i % 2, true));
            }
            var positions = ModernQuestSpace.allocate(mapping, roster, objectives, cancelled);
            if(!ModernQuestSpace.isolated(mapping))ModernWorldSpawns.qualify(mapping.binding().map(), roster, positions, objectives, cancelled);
        }
        if (cancelled.getAsBoolean()) throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CANCELLED);
    }
    static void hash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}") || value.equals("0".repeat(64))) throw invalid();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid dedicated world policy"); }
}
