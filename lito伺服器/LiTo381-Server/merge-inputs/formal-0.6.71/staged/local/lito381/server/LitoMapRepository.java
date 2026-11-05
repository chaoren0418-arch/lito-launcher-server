package local.lito381.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional classic-world adapter that joins {@code mapids} to retained 380
 * text maps.  Native LiTo modes use the embedded LTMap-derived terrain and do
 * not open this repository.
 * Embedded collision maps remain authoritative for already-audited map ids.
 * Every other row is loaded lazily from its 380 text map and exposed through
 * {@link ClassicWorldMapCatalog}, so movement/collision can use the full mapids
 * directory without loading all 86 MB at process startup.
 */
final class LitoMapRepository implements AutoCloseable {
    record Definition(int mapId, int physicalMapId, int startX, int endX,
                      int startY, int endY, boolean markable,
                      boolean teleportable, boolean escapable,
                      boolean deathPenalty, boolean recallPets, Path file) {
        int width() { return endX - startX + 1; }
        int height() { return endY - startY + 1; }
    }

    private final boolean enabled;
    private final Path mapDirectory;
    private final String url;
    private final String login;
    private final String password;
    private final String staticDatabase;
    private Map<Integer, Definition> definitions = Map.of();
    private final Map<Integer, ClassicWorldMapCatalog.ClassicMap> loadedMaps =
            new ConcurrentHashMap<>();
    private int physicalFileCount;
    private int runtimeExternalMapCount;
    private int copiedMapCount;
    private int correctedBoundaryCount;

    private LitoMapRepository() {
        enabled = false;
        mapDirectory = null;
        url = login = password = staticDatabase = "";
    }

    private LitoMapRepository(ServerConfig config) throws IOException, ClassNotFoundException {
        enabled = true;
        mapDirectory = config.mapDirectory().toAbsolutePath().normalize();
        staticDatabase = config.legacySourceResourceDatabases().get(0);
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(
                config.legacySqlProperties(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String prefix = required(properties, "URL1_LOGIN", required(properties, "URL1", null));
        String suffix = properties.getProperty("URL3_LOGIN",
                properties.getProperty("URL3", "")).trim();
        url = prefix + staticDatabase + suffix;
        login = required(properties, "Login_LOGIN", required(properties, "Login", null));
        password = properties.getProperty("Password_LOGIN",
                properties.getProperty("Password", ""));
        Class.forName(properties.getProperty(
                "Driver_LOGIN", "com.mysql.cj.jdbc.Driver").trim());
    }

    static LitoMapRepository open(ServerConfig config) throws Exception {
        return config.databaseMode().equals("lito_mysql")
                && config.classicWorldCompatibilityEnabled()
                ? new LitoMapRepository(config) : new LitoMapRepository();
    }

    void healthCheck() throws SQLException, IOException {
        if (!enabled) return;
        if (!Files.isDirectory(mapDirectory)) {
            throw new IOException("LiTo map directory is missing: " + mapDirectory);
        }
        String sql = "SELECT mapid,startX,endX,startY,endY,CopyMapId,"
                + "markable,teleportable,escapable,penalty,recall_pets FROM "
                + staticDatabase + ".mapids ORDER BY mapid";
        Map<Integer, Definition> loaded = new LinkedHashMap<>();
        Set<Integer> physicalMaps = new LinkedHashSet<>();
        try (Connection connection = DriverManager.getConnection(url, login, password)) {
            connection.setReadOnly(true);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    int mapId = row.getInt("mapid");
                    int copyMapId = row.getInt("CopyMapId");
                    int physicalMapId = copyMapId > 0 ? copyMapId : mapId;
                    Path file = mapDirectory.resolve(physicalMapId + ".txt");
                    Definition definition = new Definition(
                            mapId, physicalMapId,
                            row.getInt("startX"), row.getInt("endX"),
                            row.getInt("startY"), row.getInt("endY"),
                            row.getBoolean("markable"),
                            row.getBoolean("teleportable"),
                            row.getBoolean("escapable"),
                            row.getBoolean("penalty"),
                            row.getBoolean("recall_pets"), file);
                    if (definition.width() <= 0 || definition.height() <= 0) {
                        throw new SQLException("invalid mapids boundary for map " + mapId);
                    }
                    if (loaded.putIfAbsent(mapId, definition) != null) {
                        throw new SQLException("duplicate mapids row " + mapId);
                    }
                    physicalMaps.add(physicalMapId);
                }
            }
        }
        if (loaded.isEmpty()) throw new SQLException("lito_static.mapids is empty");
        for (int physicalMapId : physicalMaps) {
            Path file = mapDirectory.resolve(physicalMapId + ".txt");
            if (!Files.isRegularFile(file) || Files.size(file) == 0) {
                throw new IOException("LiTo physical map is missing: " + file);
            }
        }
        Map<Integer, Definition> normalized = new LinkedHashMap<>();
        for (Definition definition : loaded.values()) {
            if (definition.physicalMapId() != definition.mapId()) {
                Definition physical = loaded.get(definition.physicalMapId());
                if (physical == null) {
                    throw new SQLException("CopyMapId target is missing for map "
                            + definition.mapId() + ": " + definition.physicalMapId());
                }
                definition = new Definition(
                        definition.mapId(), definition.physicalMapId(),
                        physical.startX(), physical.endX(), physical.startY(), physical.endY(),
                        definition.markable(), definition.teleportable(), definition.escapable(),
                        definition.deathPenalty(), definition.recallPets(), physical.file());
                copiedMapCount++;
            }
            normalized.put(definition.mapId(), definition);
        }
        definitions = Map.copyOf(normalized);
        physicalFileCount = physicalMaps.size();
        for (Definition definition : definitions.values()) {
            ClassicWorldMapCatalog.ClassicMap map = loadTextMap(definition);
            if (map.width() != definition.width() || map.height() != definition.height()) {
                correctedBoundaryCount++;
            }
            if (!ClassicWorldMapCatalog.embeddedSupports(definition.mapId())) {
                loadedMaps.put(definition.mapId(), map);
            }
        }
        runtimeExternalMapCount = loadedMaps.size();
        ClassicWorldMapCatalog.installExternalMaps(this);
    }

    Definition require(int mapId) {
        Definition definition = definitions.get(mapId);
        if (definition == null) throw new IllegalArgumentException("unknown LiTo map " + mapId);
        return definition;
    }

    boolean supports(int mapId) {
        return enabled && definitions.containsKey(mapId);
    }

    ClassicWorldMapCatalog.ClassicMap load(int mapId) {
        if (!enabled) throw new IllegalArgumentException("external LiTo maps are disabled");
        try {
            return loadedMaps.computeIfAbsent(mapId, id -> {
                try {
                    return loadTextMap(require(id));
                } catch (IOException error) {
                    throw new MapLoadException(error);
                }
            });
        } catch (MapLoadException error) {
            throw new IllegalStateException("cannot load LiTo map " + mapId, error.getCause());
        }
    }

    static ClassicWorldMapCatalog.ClassicMap loadTextMap(Definition definition)
            throws IOException {
        int width = -1;
        List<byte[]> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(
                definition.file(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                if (width < 0) width = values.length;
                if (values.length != width) {
                    throw new IOException("map " + definition.mapId() + " row " + rows.size()
                            + " has " + values.length + " cells; expected " + width);
                }
                byte[] row = new byte[width];
                for (int column = 0; column < width; column++) {
                    int value;
                    try {
                        value = Integer.parseInt(values[column]);
                    } catch (NumberFormatException error) {
                        throw new IOException("invalid cell in map " + definition.mapId()
                                + " row=" + rows.size() + " column=" + column, error);
                    }
                    if (value < 0 || value > 255) {
                        throw new IOException("cell outside byte range in map "
                                + definition.mapId() + ": " + value);
                    }
                    row[column] = (byte) value;
                }
                rows.add(row);
            }
        }
        if (width <= 0 || rows.isEmpty()) {
            throw new IOException("map " + definition.mapId() + " is empty");
        }
        int height = rows.size();
        byte[] cells = new byte[Math.multiplyExact(width, height)];
        for (int index = 0; index < height; index++) {
            System.arraycopy(rows.get(index), 0, cells, index * width, width);
        }
        Definition effective = new Definition(
                definition.mapId(), definition.physicalMapId(),
                definition.startX(), Math.addExact(definition.startX(), width - 1),
                definition.startY(), Math.addExact(definition.startY(), height - 1),
                definition.markable(), definition.teleportable(), definition.escapable(),
                definition.deathPenalty(), definition.recallPets(), definition.file());
        String sha256;
        try {
            sha256 = HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(definition.file())));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        return ClassicWorldMapCatalog.externalMap(effective, sha256, cells);
    }

    String description() {
        if (!enabled) return "native-litmap-embedded(active=47, resource-directories=62)";
        return "lito_static.mapids(rows=" + definitions.size()
                + ", physical-files=" + physicalFileCount
                + ", copied-maps=" + copiedMapCount
                + ", corrected-boundaries=" + correctedBoundaryCount
                + ", runtime-external-maps=" + runtimeExternalMapCount
                + ", directory=" + mapDirectory + ")";
    }

    @Override public void close() {
        if (enabled) ClassicWorldMapCatalog.removeExternalMaps(this);
        loadedMaps.clear();
    }

    private static final class MapLoadException extends RuntimeException {
        private MapLoadException(IOException cause) { super(cause); }
    }

    private static String required(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            if (fallback != null) return fallback;
            throw new IllegalArgumentException("Missing SQL property: " + key);
        }
        return value.trim();
    }
}
