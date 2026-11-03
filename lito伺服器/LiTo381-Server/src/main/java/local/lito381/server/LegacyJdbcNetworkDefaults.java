package local.lito381.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Missing Connector/J network defaults; pool admission is a separate timeout. */
final class LegacyJdbcNetworkDefaults {
    static final int CONNECT_MILLIS = 10_000;
    static final int SOCKET_MILLIS = 60_000;

    private LegacyJdbcNetworkDefaults() {}

    /** Keep explicit operator values, including deliberate zero, unchanged. */
    static String apply(String jdbcUrl) {
        if (jdbcUrl == null) throw new IllegalArgumentException("missing JDBC URL");
        String result = jdbcUrl;
        if (!hasQueryProperty(jdbcUrl, "connectTimeout")) {
            result = append(result, "connectTimeout", CONNECT_MILLIS);
        }
        if (!hasQueryProperty(jdbcUrl, "socketTimeout")) {
            result = append(result, "socketTimeout", SOCKET_MILLIS);
        }
        return result;
    }

    private static boolean hasQueryProperty(String url, String property) {
        int query = url.indexOf('?');
        if (query < 0) return false;
        for (String part : url.substring(query + 1).split("&", -1)) {
            int equals = part.indexOf('=');
            String key = equals < 0 ? part : part.substring(0, equals);
            try {
                if (property.equals(URLDecoder.decode(key, StandardCharsets.UTF_8))) return true;
            } catch (IllegalArgumentException malformedEncoding) {
                throw new IllegalArgumentException("malformed JDBC query property encoding");
            }
        }
        return false;
    }

    private static String append(String url, String key, int millis) {
        String delimiter = url.indexOf('?') < 0 ? "?"
                : url.endsWith("?") || url.endsWith("&") ? "" : "&";
        return url + delimiter + key + "=" + millis;
    }
}
