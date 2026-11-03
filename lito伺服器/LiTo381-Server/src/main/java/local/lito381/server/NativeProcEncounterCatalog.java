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
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.NativeProcNpcCatalog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeProcEncounterCatalog {
    static final String RESOURCE = "/combat/proc-npc-receivers.tsv";
    static final String QUERY = "SELECT npcid,impl,undead,lawful FROM npc ORDER BY npcid";
    private static volatile Catalog current;

    private NativeProcEncounterCatalog() {
    }

    static Map<String, Type> readTypes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new FileNotFoundException(RESOURCE);
        }
        LinkedHashMap<String, Type> linkedHashMap = new LinkedHashMap<String, Type>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
            String string;
            if (!"# source-npc-receivers-v1".equals(bufferedReader.readLine())) {
                throw new IOException("invalid NPC receiver resource version");
            }
            while ((string = bufferedReader.readLine()) != null) {
                Type type;
                String[] stringArray = string.split("\t", -1);
                if (stringArray.length != 4 || stringArray[0].isEmpty() || !stringArray[2].startsWith("com/lineage/server/model/Instance/")) {
                    throw new IOException("invalid NPC receiver resource row");
                }
                try {
                    type = new Type(NativeProcHealingKernel.Kind.valueOf(stringArray[1]), stringArray[2], Receiver.valueOf(stringArray[3]));
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    throw new IOException("invalid NPC receiver kind", illegalArgumentException);
                }
                if (type.kind() != NativeProcHealingKernel.Kind.PLAYER && linkedHashMap.putIfAbsent(stringArray[0], type) == null) continue;
                throw new IOException("duplicate/invalid NPC receiver implementation");
            }
        }
        if (linkedHashMap.isEmpty()) {
            throw new IOException("empty NPC receiver resource");
        }
        return Map.copyOf(linkedHashMap);
    }

    static Catalog read(Connection connection, String string, Map<String, Type> map) throws SQLException {
        LinkedHashMap<Integer, Npc> linkedHashMap = new LinkedHashMap<Integer, Npc>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(QUERY);){
            while (resultSet.next()) {
                String string2 = resultSet.getString(2);
                Npc npc = new Npc(resultSet.getInt(1), string2, resultSet.getInt(3), resultSet.getInt(4), string2 == null ? null : map.get(string2));
                if (linkedHashMap.putIfAbsent(npc.id(), npc) == null) continue;
                throw new SQLException("duplicate encounter NPC=" + npc.id());
            }
        }
        if (linkedHashMap.isEmpty()) {
            throw new SQLException("empty encounter NPC catalog");
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
        Map<String, Type> map = NativeProcEncounterCatalog.readTypes(NativeProcEncounterCatalog.class.getResourceAsStream(RESOURCE));
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        try (Object object = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));){
            object.setReadOnly(true);
            object.setAutoCommit(false);
            object.setTransactionIsolation(4);
            try {
                catalog = NativeProcEncounterCatalog.read((Connection)object, string, map);
            }
            finally {
                object.rollback();
            }
        }
        object = NativeProcNpcCatalog.catalog();
        if (((NativeProcNpcCatalog.Catalog)object).npcs().size() != catalog.npcs().size()) {
            throw new SQLException("NPC static snapshots changed during startup");
        }
        for (Npc npc : catalog.npcs().values()) {
            NativeProcNpcCatalog.Npc npc2 = ((NativeProcNpcCatalog.Catalog)object).find(npc.id());
            if (npc2 == null || npc2.undead() != npc.undead() || !Objects.equals(npc2.implementation(), npc.implementation())) {
                throw new SQLException("NPC static metadata changed during startup id=" + npc.id());
            }
            if (npc.type() == null || ((NativeProcNpcCatalog.Catalog)object).implementations().get(npc.implementation()) == npc.kind()) continue;
            throw new SQLException("source NPC hierarchy changed without kind qualification");
        }
        current = catalog;
    }

    static Catalog catalog() {
        Catalog catalog = current;
        if (catalog == null) {
            throw new IllegalStateException("encounter Proc NPC catalog not loaded");
        }
        return catalog;
    }

    record Type(NativeProcHealingKernel.Kind kind, String owner, Receiver receiver) {
    }

    static enum Receiver {
        MONSTER,
        PET,
        SUMMON,
        GUARD,
        POSITIVE_ONLY,
        NONE,
        UNSUPPORTED,
        FOLLOWER;

    }

    record Npc(int id, String implementation, int undead, int lawful, Type type) {
        NativeProcHealingKernel.Kind kind() {
            return this.type.kind();
        }

        Receiver receiver() {
            return this.type.receiver();
        }
    }

    record Catalog(String database, Map<Integer, Npc> npcs, Map<String, Type> types) {
        Catalog {
            map = Map.copyOf(map);
            map2 = Map.copyOf(map2);
        }

        Npc find(int n) {
            return this.npcs.get(n);
        }

        Npc require(int n) {
            Npc npc = this.find(n);
            if (npc == null || npc.type() == null) {
                throw new IllegalArgumentException("unqualified encounter NPC=" + n);
            }
            if (npc.receiver() == Receiver.UNSUPPORTED && npc.kind() != NativeProcHealingKernel.Kind.TOWER && npc.kind() != NativeProcHealingKernel.Kind.DOOR) {
                throw new IllegalArgumentException("unported encounter NPC receiver=" + npc.implementation() + " owner=" + npc.type().owner());
            }
            return npc;
        }
    }
}
