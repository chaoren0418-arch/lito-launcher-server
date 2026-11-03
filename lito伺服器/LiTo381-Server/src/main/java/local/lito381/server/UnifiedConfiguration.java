// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import local.lito381.server.SharedDatabase;

public final class UnifiedConfiguration {
    static final Set<String> KEYS = Set.of("bind", "game.bind", "port", "server.name", "database.jdbc.base.url", "database.jdbc.options", "database.user", "database.password", "database.static.database", "database.lito.database", "weight.rate", "capture.directory", "battle.ai.enabled", "battle.physical.hit.enabled", "battle.natural.mp.regen.enabled", "native.siege.roster.enforced", "native.tournament.allowance", "memory.initial", "memory.maximum", "cpu.limit", "logs.directory", "startup.timeout.seconds");

    static Properties raw(Path path) throws Exception {
        Properties properties = new Properties(){

            @Override
            public synchronized Object put(Object object, Object object2) {
                if (this.containsKey(object)) {
                    throw new IllegalArgumentException("Duplicate setting: " + String.valueOf(object));
                }
                return super.put(object, object2);
            }
        };
        try (Object object = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            properties.load((Reader)object);
        }
        object = new Properties();
        ((Properties)object).putAll((Map<?, ?>)properties);
        return object;
    }

    static Properties read(Path path) throws Exception {
        Properties object = UnifiedConfiguration.raw(path);
        if (object.containsKey("configuration.base.file")) {
            for (String object22 : object.stringPropertyNames()) {
                if (Set.of("configuration.base.file", "capture.directory").contains(object22)) continue;
                throw new IllegalArgumentException("Unsupported runtime override: " + object22);
            }
            Path path2 = path.getParent().resolve(object.getProperty("configuration.base.file")).normalize();
            Properties properties = UnifiedConfiguration.raw(path2);
            UnifiedConfiguration.validate(properties);
            if (object.containsKey("capture.directory")) {
                properties.setProperty("capture.directory", object.getProperty("capture.directory"));
            }
            properties.setProperty("configuration.base.file", path2.toAbsolutePath().toString());
            object = properties;
        } else if (object.containsKey("database.jdbc.base.url")) {
            UnifiedConfiguration.validate(object);
            object.setProperty("configuration.base.file", path.toAbsolutePath().toString());
        }
        UnifiedConfiguration.aliases(object);
        return object;
    }

    static void validate(Properties properties) {
        for (String string : properties.stringPropertyNames()) {
            if (KEYS.contains(string)) continue;
            throw new IllegalArgumentException("Unknown/retired setting: " + string);
        }
        for (String string : KEYS) {
            if (properties.containsKey(string)) continue;
            throw new IllegalArgumentException("Missing setting: " + string);
        }
        if (!properties.getProperty("database.jdbc.base.url").startsWith("jdbc:mysql://") || !properties.getProperty("database.jdbc.base.url").endsWith("/")) {
            throw new IllegalArgumentException("Invalid database.jdbc.base.url");
        }
        if (!properties.getProperty("database.jdbc.options").startsWith("?")) {
            throw new IllegalArgumentException("Invalid database.jdbc.options");
        }
        for (String string : List.of("database.static.database", "database.lito.database")) {
            SharedDatabase.identifier(properties.getProperty(string));
        }
        if (properties.getProperty("database.user").isBlank()) {
            throw new IllegalArgumentException("Empty database.user");
        }
        double d = Double.parseDouble(properties.getProperty("weight.rate"));
        if (!Double.isFinite(d) || d <= 0.0 || d > 1000.0) {
            throw new IllegalArgumentException("weight.rate must be >0 and <=1000");
        }
        for (String string : List.of("battle.ai.enabled", "battle.physical.hit.enabled", "battle.natural.mp.regen.enabled", "native.siege.roster.enforced")) {
            if (Set.of("true", "false").contains(properties.getProperty(string))) continue;
            throw new IllegalArgumentException("Invalid boolean: " + string);
        }
        UnifiedConfiguration.number(properties, "port", 1024, 65527);
        UnifiedConfiguration.number(properties, "cpu.limit", 1, 256);
        UnifiedConfiguration.number(properties, "startup.timeout.seconds", 5, 300);
        UnifiedConfiguration.number(properties, "native.tournament.allowance", 0, 1000000);
        if (UnifiedConfiguration.memory(properties.getProperty("memory.initial")) > UnifiedConfiguration.memory(properties.getProperty("memory.maximum"))) {
            throw new IllegalArgumentException("Initial heap exceeds maximum");
        }
    }

    static long memory(String string) {
        if (!string.matches("[1-9][0-9]*[mMgG]")) {
            throw new IllegalArgumentException("Invalid heap size");
        }
        long l = Long.parseLong(string.substring(0, string.length() - 1));
        return Math.multiplyExact(l, string.toLowerCase(Locale.ROOT).endsWith("g") ? 1024L : 1L);
    }

    static int number(Properties properties, String string, int n, int n2) {
        int n3 = Integer.parseInt(properties.getProperty(string));
        if (n3 < n || n3 > n2) {
            throw new IllegalArgumentException("Out of range: " + string);
        }
        return n3;
    }

    public static void aliases(Properties properties) {
        if (!properties.containsKey("database.jdbc.base.url")) {
            return;
        }
        for (String string : List.of("URL1_LOGIN", "URL1")) {
            properties.setProperty(string, properties.getProperty("database.jdbc.base.url"));
        }
        for (String string : List.of("URL3_LOGIN", "URL3")) {
            properties.setProperty(string, properties.getProperty("database.jdbc.options"));
        }
        for (String string : List.of("Login_LOGIN", "Login")) {
            properties.setProperty(string, properties.getProperty("database.user"));
        }
        for (String string : List.of("Password_LOGIN", "Password")) {
            properties.setProperty(string, properties.getProperty("database.password"));
        }
        properties.setProperty("login.source.port.base", properties.getProperty("port"));
        properties.setProperty("database.persistence.policy", "source-readonly-stats-only");
        properties.setProperty("database.lito.character.table", "character_lito");
    }

    static List<Source> sources(Properties properties) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        ArrayList<Source> arrayList = new ArrayList<Source>();
        try (Connection connection = DriverManager.getConnection(properties.getProperty("database.jdbc.base.url") + SharedDatabase.identifier(properties.getProperty("database.lito.database")) + properties.getProperty("database.jdbc.options"), properties.getProperty("database.user"), properties.getProperty("database.password"));
             Statement statement = connection.createStatement();){
            connection.setReadOnly(true);
            statement.setQueryTimeout(10);
            try (ResultSet resultSet = statement.executeQuery("SELECT source_id,display_name,character_database,enabled FROM lito_source_server ORDER BY source_id");){
                while (resultSet.next()) {
                    int n = resultSet.getInt(4);
                    if (resultSet.wasNull() || n < 0 || n > 1) {
                        throw new IllegalArgumentException("Invalid source enabled flag");
                    }
                    arrayList.add(new Source(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), n == 1));
                }
            }
        }
        UnifiedConfiguration.validateSources(arrayList);
        return arrayList.stream().filter(Source::enabled).toList();
    }

    static void validateSources(List<Source> list) {
        HashSet<Integer> hashSet = new HashSet<Integer>();
        HashSet<String> hashSet2 = new HashSet<String>();
        HashSet<String> hashSet3 = new HashSet<String>();
        int n = 0;
        for (Source source : list) {
            if (source.id < 1 || source.id > 8 || !hashSet.add(source.id)) {
                throw new IllegalArgumentException("Source ID must be unique 1..8");
            }
            if (!source.enabled) continue;
            ++n;
            if (source.name == null || source.name.isBlank() || source.name.contains(",") || source.name.getBytes(Charset.forName("MS950")).length > 63 || !Charset.forName("MS950").newEncoder().canEncode(source.name) || source.name.chars().anyMatch(Character::isISOControl) || !hashSet2.add(source.name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Invalid/duplicate source name");
            }
            if (hashSet3.add(SharedDatabase.identifier(source.database).toLowerCase(Locale.ROOT))) continue;
            throw new IllegalArgumentException("Duplicate source database");
        }
        if (n == 0) {
            throw new IllegalArgumentException("No enabled source servers");
        }
    }

    public static void loaded(Properties properties, Path path) {
        if (properties.containsKey("database.jdbc.base.url") && !properties.containsKey("configuration.base.file")) {
            properties.setProperty("configuration.base.file", path.toAbsolutePath().normalize().toString());
        }
    }

    static void apply(Properties properties, Path path) throws Exception {
        Path path2 = properties.containsKey("configuration.base.file") ? path.resolve(properties.getProperty("configuration.base.file")) : path.resolve("server.properties");
        Properties properties2 = UnifiedConfiguration.read(path2);
        if (properties.containsKey("configuration.base.file") && properties.containsKey("capture.directory")) {
            properties2.setProperty("capture.directory", properties.getProperty("capture.directory"));
        }
        List<Source> list = UnifiedConfiguration.sources(properties2);
        String string = properties2.getProperty("configuration.base.file", path2.toAbsolutePath().toString());
        properties.clear();
        properties.putAll((Map<?, ?>)properties2);
        properties.setProperty("database.connection.properties", string.replace('\\', '/'));
        properties.setProperty("database.mode", "lito_mysql");
        properties.setProperty("database.read.only", "true");
        properties.setProperty("state.persistence.enabled", "false");
        properties.setProperty("classic.world.compatibility.enabled", "false");
        properties.setProperty("classic.world.monster.ai.enabled", "false");
        properties.setProperty("battle.compatibility.service.actors.enabled", "false");
        properties.setProperty("native.tournament.economy.enabled", "true");
        properties.setProperty("lito.rental.town.directory.enabled", "false");
        properties.setProperty("lito.rental.teleport.routes", "");
        properties.setProperty("lobby.server.select.response.delay.ms", "0");
        properties.setProperty("lobby.enter.channel.response.delay.ms", "0");
        properties.setProperty("login.bootstrap.sequence", "login_check,classic_server_list,lito_server_list");
        properties.setProperty("login.bootstrap.step.delay.ms", "250");
        properties.setProperty("lobby.character.list.enabled", "true");
        properties.setProperty("origin.source.id", "lito-shared");
        properties.setProperty("origin.source.name", properties.getProperty("server.name"));
        properties.setProperty("advertised.host", properties.getProperty("game.bind"));
        properties.setProperty("advertised.port", properties.getProperty("port"));
        properties.setProperty("login.source.port.base", properties.getProperty("port"));
        properties.setProperty("state.file", Path.of(string, new String[0]).getParent().resolve("../runtime/shared-database-memory/server.state").normalize().toString().replace('\\', '/'));
        for (String string2 : List.of("capture.directory", "logs.directory")) {
            properties.setProperty(string2, Path.of(string, new String[0]).getParent().resolve(properties.getProperty(string2)).normalize().toString().replace('\\', '/'));
        }
        properties.setProperty("login.classic.server.names", String.join((CharSequence)",", list.stream().map(Source::name).toList()));
        properties.setProperty("login.classic.server.ids", String.join((CharSequence)",", list.stream().map(source -> Integer.toString(source.id)).toList()));
        properties.setProperty("database.dynamic.databases", String.join((CharSequence)",", list.stream().map(Source::database).toList()));
        properties.setProperty("database.source.weight.rates", String.join((CharSequence)",", Collections.nCopies(list.size(), properties.getProperty("weight.rate"))));
    }

    static String quote(String string) {
        StringBuilder stringBuilder = new StringBuilder("\"");
        for (char c : string.toCharArray()) {
            if (c == '\"' || c == '\\') {
                stringBuilder.append('\\').append(c);
                continue;
            }
            if (c < ' ') {
                stringBuilder.append(String.format("\\u%04x", c));
                continue;
            }
            stringBuilder.append(c);
        }
        return stringBuilder.append('\"').toString();
    }

    public static void main(String[] stringArray) {
        try {
            Path path = Path.of(stringArray[0], new String[0]).toAbsolutePath();
            Properties properties = UnifiedConfiguration.raw(path);
            UnifiedConfiguration.loaded(properties, path);
            UnifiedConfiguration.apply(properties, path.getParent());
            StringJoiner stringJoiner = new StringJoiner(",", "{", "}");
            for (String stringArray22 : new TreeSet<String>(properties.stringPropertyNames())) {
                if (stringArray22.startsWith("database.jdbc.") || stringArray22.equals("database.password") || stringArray22.equals("database.user") || stringArray22.startsWith("Password") || stringArray22.startsWith("Login") || stringArray22.startsWith("URL")) continue;
                stringJoiner.add(UnifiedConfiguration.quote(stringArray22) + ":" + UnifiedConfiguration.quote(properties.getProperty(stringArray22)));
            }
            String[] stringArray3 = properties.getProperty("login.classic.server.ids").split(",");
            String[] stringArray2 = properties.getProperty("login.classic.server.names").split(",");
            String[] stringArray4 = properties.getProperty("database.dynamic.databases").split(",");
            StringJoiner stringJoiner2 = new StringJoiner(",", "[", "]");
            for (int i = 0; i < stringArray3.length; ++i) {
                stringJoiner2.add("{\"id\":" + stringArray3[i] + ",\"name\":" + UnifiedConfiguration.quote(stringArray2[i]) + ",\"database\":" + UnifiedConfiguration.quote(stringArray4[i]) + ",\"port\":" + (Integer.parseInt(properties.getProperty("port")) + Integer.parseInt(stringArray3[i])) + "}");
            }
            System.out.println("{\"settings\":" + String.valueOf(stringJoiner) + ",\"routes\":" + String.valueOf(stringJoiner2) + "}");
        }
        catch (Exception exception) {
            System.err.println("LiTo configuration unavailable: " + exception.getClass().getSimpleName() + (String)(exception instanceof IllegalArgumentException ? " " + exception.getMessage() : "; check database connectivity, permissions and lito_source_server"));
            System.exit(1);
        }
    }

    record Source(int id, String name, String database, boolean enabled) {
    }
}
