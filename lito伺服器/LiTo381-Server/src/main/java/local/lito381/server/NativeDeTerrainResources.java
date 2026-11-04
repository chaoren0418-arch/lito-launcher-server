// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import local.lito381.server.NativeDeNpcTemplateResources;
import local.lito381.server.NativeDeTerrainData;
import local.lito381.server.NativeDeTerrainScope;
import local.lito381.server.NativeDeTerrainTable;
import local.lito381.server.NativeDeTerrainText;
import local.lito381.server.NativeDeTerrainV1;
import local.lito381.server.NativeDeWorldContext;
import local.lito381.server.NativeDeWorldLog;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;
import local.lito381.server.UnifiedConfiguration;

final class NativeDeTerrainResources {
    private static volatile Snapshot current;
    private static final Map<Integer, String> assets;

    private static Map<Integer, String> assets() {
        TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();
        try (InputStream inputStream = NativeDeTerrainResources.class.getResourceAsStream("/combat/terrain/source-index.tsv");){
            if (inputStream == null) {
                throw new IllegalStateException("source terrain index missing");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
                String string;
                while ((string = bufferedReader.readLine()) != null) {
                    String[] stringArray = string.split("\t");
                    if (stringArray.length == 2 && treeMap.put(Integer.parseInt(stringArray[0]), stringArray[1]) == null) continue;
                    throw new IllegalStateException("invalid source terrain index");
                }
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
        return Map.copyOf(treeMap);
    }

    static Reader asset(String string) throws IOException {
        if (!string.matches("\\./maps/-?[0-9]+\\.txt")) {
            throw new FileNotFoundException("source terrain path rejected");
        }
        int n = Integer.parseInt(string.substring(7, string.length() - 4));
        String string2 = assets.get(n);
        if (string2 == null) {
            throw new FileNotFoundException("unindexed source terrain " + n);
        }
        InputStream inputStream = NativeDeTerrainResources.class.getResourceAsStream("/combat/terrain/source/" + n + ".txt.gz");
        if (inputStream == null) {
            throw new FileNotFoundException("source terrain asset missing " + n);
        }
        return NativeDeTerrainResources.verifiedAsset(inputStream, n, string2);
    }

    static Reader verifiedAsset(InputStream inputStream, final int n, final String string) throws IOException {
        GZIPInputStream gZIPInputStream;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new AssertionError((Object)noSuchAlgorithmException);
        }
        try {
            gZIPInputStream = new GZIPInputStream(inputStream);
        }
        catch (Throwable throwable) {
            try {
                inputStream.close();
            }
            catch (Throwable throwable2) {
                throwable.addSuppressed(throwable2);
            }
            if (throwable instanceof IOException) {
                IOException iOException = (IOException)throwable;
                throw iOException;
            }
            throw NativeDeWorldContext.failure(throwable);
        }
        final DigestInputStream digestInputStream = new DigestInputStream(gZIPInputStream, messageDigest);
        return new FilterReader(new InputStreamReader((InputStream)digestInputStream, Charset.defaultCharset())){
            boolean closed;

            @Override
            public void close() throws IOException {
                if (this.closed) {
                    return;
                }
                this.closed = true;
                Throwable throwable = null;
                try {
                    digestInputStream.transferTo(OutputStream.nullOutputStream());
                    if (!HexFormat.of().formatHex(messageDigest.digest()).equals(string)) {
                        throw new IOException("source terrain digest mismatch " + n);
                    }
                }
                catch (Throwable throwable2) {
                    throwable = throwable2;
                }
                try {
                    super.close();
                }
                catch (Throwable throwable3) {
                    if (throwable == null) {
                        throwable = throwable3;
                    }
                    throwable.addSuppressed(throwable3);
                }
                if (throwable instanceof IOException) {
                    IOException iOException = (IOException)throwable;
                    throw iOException;
                }
                if (throwable != null) {
                    throw NativeDeWorldContext.failure(throwable);
                }
            }
        };
    }

    static Snapshot read(String string3, NativeDeTerrainScope.Sql sql, NativeDeNpcTemplateResources.Counts counts, NativeDeWorldLog nativeDeWorldLog) throws SQLException {
        AtomicLong atomicLong = new AtomicLong();
        NativeDeTerrainScope.Context context = new NativeDeTerrainScope.Context(sql, NativeDeTerrainResources::asset, (string, string2, objectArray) -> {
            if (string.equals("error")) {
                atomicLong.incrementAndGet();
            }
            nativeDeWorldLog.emit(string, string2, objectArray);
        });
        NativeDeTerrainScope.call(context, () -> {
            NativeDeTerrainTable.get().load();
            return null;
        });
        if (atomicLong.get() != 0L || counts.lifecycleFailures.get() != 0L || counts.opened.get() != counts.rolledBack.get() || counts.opened.get() != counts.closed.get() || context.maps.isEmpty()) {
            throw new SQLException("source map metadata lifecycle failed; publication refused");
        }
        TreeMap<Integer, Definition> treeMap = new TreeMap<Integer, Definition>();
        for (Map.Entry<Integer, NativeDeTerrainData> entry : context.maps.entrySet()) {
            Definition definition = Definition.of(entry.getValue(), context.names.get(entry.getKey()));
            if (!assets.containsKey(definition.physical())) {
                throw new SQLException("source map physical asset not packaged: " + definition.id() + " -> " + definition.physical());
            }
            treeMap.put(entry.getKey(), definition);
        }
        return new Snapshot(string3, treeMap, counts.opened.get(), counts.rolledBack.get(), counts.closed.get(), atomicLong.get());
    }

    static synchronized void load(ServerConfig serverConfig) throws Exception {
        Snapshot snapshot;
        if (current != null) {
            return;
        }
        Properties properties = UnifiedConfiguration.read(serverConfig.source());
        String string3 = SharedDatabase.identifier(properties.getProperty("database.static.database"));
        NativeDeNpcTemplateResources.Counts counts = new NativeDeNpcTemplateResources.Counts();
        current = snapshot = NativeDeTerrainResources.read(string3, () -> NativeDeNpcTemplateResources.owned(DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + string3 + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password")), counts), counts, (string, string2, objectArray) -> System.getLogger(NativeDeTerrainResources.class.getName()).log(string.equals("error") ? System.Logger.Level.ERROR : System.Logger.Level.INFO, string2 + " " + Arrays.toString(objectArray)));
    }

    static void addMaps(Set<Integer> set) {
        set.addAll(NativeDeTerrainResources.snapshot().maps().keySet());
    }

    static Snapshot snapshot() {
        return Objects.requireNonNull(current, "source terrain resources not loaded");
    }

    static NativeDeTerrainV1 create(Definition definition, NativeDeWorldLog nativeDeWorldLog) {
        NativeDeTerrainScope.Context context = new NativeDeTerrainScope.Context(null, NativeDeTerrainResources::asset, nativeDeWorldLog);
        return NativeDeTerrainScope.call(context, () -> definition.create(NativeDeTerrainText.readOne(definition.id(), definition.copy(), definition.width(), definition.height())));
    }

    private NativeDeTerrainResources() {
    }

    static {
        assets = NativeDeTerrainResources.assets();
    }

    record Definition(int id, int startX, int endX, int startY, int endY, double monsters, double drop, boolean underwater, boolean markable, boolean teleportable, boolean escapable, boolean resurrection, boolean painwand, boolean penalty, boolean pets, boolean recall, boolean items, boolean skills, boolean guaji, int shop, boolean arrows, int copy, String name) {
        static Definition of(NativeDeTerrainData nativeDeTerrainData, String string) {
            return new Definition(nativeDeTerrainData.mapId, nativeDeTerrainData.startX, nativeDeTerrainData.endX, nativeDeTerrainData.startY, nativeDeTerrainData.endY, nativeDeTerrainData.monster_amount, nativeDeTerrainData.dropRate, nativeDeTerrainData.isUnderwater, nativeDeTerrainData.markable, nativeDeTerrainData.teleportable, nativeDeTerrainData.escapable, nativeDeTerrainData.isUseResurrection, nativeDeTerrainData.isUsePainwand, nativeDeTerrainData.isEnabledDeathPenalty, nativeDeTerrainData.isTakePets, nativeDeTerrainData.isRecallPets, nativeDeTerrainData.isUsableItem, nativeDeTerrainData.isUsableSkill, nativeDeTerrainData.isGuaji, nativeDeTerrainData.isUsableShop, nativeDeTerrainData.isArrows, nativeDeTerrainData.copyMapId, string);
        }

        int physical() {
            return this.copy > 0 ? this.copy : this.id;
        }

        int width() {
            return this.endX - this.startX + 1;
        }

        int height() {
            return this.endY - this.startY + 1;
        }

        NativeDeTerrainV1 create(byte[][] byArray) {
            return new NativeDeTerrainV1((short)this.id, byArray, this.startX, this.endX, this.startY, this.endY, this.underwater, this.markable, this.teleportable, this.escapable, this.resurrection, this.painwand, this.penalty, this.pets, this.recall, this.items, this.skills, this.guaji, this.shop);
        }
    }

    record Snapshot(String database, Map<Integer, Definition> maps, long opened, long rolledBack, long closed, long errors) {
        Snapshot {
            maps = Map.copyOf(maps);
        }
    }
}
