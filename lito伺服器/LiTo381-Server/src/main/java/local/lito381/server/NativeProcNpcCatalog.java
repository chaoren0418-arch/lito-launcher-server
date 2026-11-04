// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import local.lito381.server.BattleCompanion;
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeProcNpcCatalog {
    static final String RESOURCE = "/combat/proc-npc-kinds.tsv";
    static final String QUERY = "SELECT npcid,impl,undead FROM npc ORDER BY npcid";
    private static volatile Catalog current;

    private NativeProcNpcCatalog() {
    }

    static Map<String, NativeProcHealingKernel.Kind> readKinds(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new FileNotFoundException("missing frozen Proc NPC ancestry /combat/proc-npc-kinds.tsv");
        }
        LinkedHashMap<String, NativeProcHealingKernel.Kind> linkedHashMap = new LinkedHashMap<String, NativeProcHealingKernel.Kind>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
            String string;
            if (!"# source-npc-kinds-v1".equals(bufferedReader.readLine())) {
                throw new IOException("invalid Proc NPC ancestry version");
            }
            while ((string = bufferedReader.readLine()) != null) {
                NativeProcHealingKernel.Kind kind;
                String[] stringArray = string.split("\t", -1);
                if (stringArray.length != 2 || stringArray[0].isEmpty()) {
                    throw new IOException("invalid Proc NPC ancestry row");
                }
                try {
                    kind = NativeProcHealingKernel.Kind.valueOf(stringArray[1]);
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    throw new IOException("invalid Proc NPC ancestry kind", illegalArgumentException);
                }
                if (kind != NativeProcHealingKernel.Kind.PLAYER && linkedHashMap.putIfAbsent(stringArray[0], kind) == null) continue;
                throw new IOException("invalid or duplicate Proc NPC ancestry=" + stringArray[0]);
            }
        }
        if (linkedHashMap.isEmpty()) {
            throw new IOException("empty Proc NPC ancestry");
        }
        return Map.copyOf(linkedHashMap);
    }

    static Catalog read(Connection connection, String string, Map<String, NativeProcHealingKernel.Kind> map) throws SQLException {
        LinkedHashMap<Integer, Npc> linkedHashMap = new LinkedHashMap<Integer, Npc>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(QUERY);){
            while (resultSet.next()) {
                Npc npc = new Npc(resultSet.getInt(1), resultSet.getString(2), resultSet.getInt(3));
                if (linkedHashMap.putIfAbsent(npc.id(), npc) == null) continue;
                throw new SQLException("duplicate Proc npcid=" + npc.id());
            }
        }
        if (linkedHashMap.isEmpty()) {
            throw new SQLException("empty Proc NPC catalog");
        }
        return new Catalog(string, linkedHashMap, map);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static synchronized void load(ServerConfig serverConfig) throws Exception {
        Catalog catalog;
        if (current != null) {
            return;
        }
        Map<String, NativeProcHealingKernel.Kind> map = NativeProcNpcCatalog.readKinds(NativeProcNpcCatalog.class.getResourceAsStream(RESOURCE));
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(4);
            try {
                catalog = NativeProcNpcCatalog.read(connection, string, map);
            }
            finally {
                connection.rollback();
            }
        }
        current = catalog;
    }

    static Catalog catalog() {
        Catalog catalog = current;
        if (catalog == null) {
            throw new IllegalStateException("Proc NPC catalog not loaded at startup");
        }
        return catalog;
    }

    static NativeProcHealingKernel.Kind companionKind(BattleCompanion.Kind kind) {
        return switch (Objects.requireNonNull(kind)) {
            default -> throw new IncompatibleClassChangeError();
            case PET -> NativeProcHealingKernel.Kind.PET;
            case SUMMON, TAMED -> NativeProcHealingKernel.Kind.SUMMON;
            case ARENA -> NativeProcHealingKernel.Kind.NPC;
        };
    }

    record Npc(int id, String implementation, int undead) {
    }

    record Catalog(String database, Map<Integer, Npc> npcs, Map<String, NativeProcHealingKernel.Kind> implementations) {
        Catalog {
            npcs = Map.copyOf(npcs);
            implementations = Map.copyOf(implementations);
        }

        Npc find(int n) {
            return this.npcs.get(n);
        }

        NativeProcHealingKernel.Kind kind(int n) {
            NativeProcHealingKernel.Kind kind;
            Npc npc = this.find(n);
            if (npc == null) {
                throw new IllegalArgumentException("missing Proc NPC template=" + n);
            }
            NativeProcHealingKernel.Kind kind2 = kind = npc.implementation() == null ? null : this.implementations.get(npc.implementation());
            if (kind == null) {
                throw new IllegalArgumentException("unqualified Proc NPC implementation id=" + n + " impl=" + npc.implementation());
            }
            return kind;
        }
    }
}
