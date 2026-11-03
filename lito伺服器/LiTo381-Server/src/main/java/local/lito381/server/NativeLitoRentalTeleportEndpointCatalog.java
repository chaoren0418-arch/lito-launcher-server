package local.lito381.server;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server-owned endpoint metadata for the selected client's TeleportMe pages.
 *
 * <p>The client resources prove the page/action labels and the protected
 * binary proves that A..D select S_F3 table indices 0..3.  They do not contain
 * the historical host/port catalogue, so no endpoint is enabled implicitly.
 * Each configured row is qualified by both HTML page and action string; this
 * is required because the three retained page variants reuse action letters
 * for different displayed destinations.</p>
 */
final class NativeLitoRentalTeleportEndpointCatalog {
    static final String PROPERTY = "lito.rental.teleport.routes";

    record Endpoint(String html, String action, int serverIndex, String name,
                    String host, long ipv4, int port) {
        Endpoint {
            if (!NativeLitoRentalTownNpcCatalog.TELEPORT_ACTIONS_BY_HTML
                        .getOrDefault(html, java.util.Set.of()).contains(action)
                    || serverIndex != indexForAction(action)
                    || name == null || name.isBlank()
                    || host == null || host.isBlank()
                    || ipv4 < 0 || ipv4 > 0xFFFF_FFFFL
                    || port < 1 || port > 0xFFFF) {
                throw new IllegalArgumentException("invalid rental teleport endpoint");
            }
        }

        String key() {
            return NativeLitoRentalTeleportEndpointCatalog.key(html, action);
        }
    }

    private NativeLitoRentalTeleportEndpointCatalog() {}

    /**
     * Parses comma-separated {@code html|action|name|IPv4|port} rows.
     * Empty input deliberately means that historical endpoint routing is off.
     */
    static List<Endpoint> parse(String configured) {
        if (configured == null || configured.isBlank()) return List.of();
        Map<String, Endpoint> endpoints = new LinkedHashMap<>();
        for (String rawRow : configured.split(",", -1)) {
            String row = rawRow.trim();
            if (row.isEmpty()) {
                throw new IllegalArgumentException(PROPERTY + " contains an empty row");
            }
            String[] fields = row.split("\\|", -1);
            if (fields.length != 5) {
                throw new IllegalArgumentException(PROPERTY
                        + " row must be html|action|name|IPv4|port: " + row);
            }
            String html = fields[0].trim();
            String action = fields[1].trim().toUpperCase(Locale.ROOT);
            String name = fields[2].trim();
            String host = fields[3].trim();
            int port;
            try {
                port = Integer.parseInt(fields[4].trim());
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException(PROPERTY
                        + " has an invalid port: " + row, error);
            }
            long ipv4 = ipv4LittleEndian(host);
            Endpoint endpoint = new Endpoint(
                    html, action, indexForAction(action), name, host, ipv4, port);
            if (endpoints.putIfAbsent(endpoint.key(), endpoint) != null) {
                throw new IllegalArgumentException(PROPERTY
                        + " contains duplicate page/action: " + endpoint.key());
            }
        }
        return List.copyOf(new ArrayList<>(endpoints.values()));
    }

    static Endpoint find(List<Endpoint> endpoints, String html, String action) {
        if (endpoints == null || html == null || action == null) return null;
        String wanted = key(html, action);
        return endpoints.stream().filter(endpoint -> endpoint.key().equals(wanted))
                .findFirst().orElse(null);
    }

    static Protocol.LitoVsServerAvailability[] statusTable(
            List<Endpoint> endpoints, String html) {
        int maximum = -1;
        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                if (endpoint.html().equals(html)) {
                    maximum = Math.max(maximum, endpoint.serverIndex());
                }
            }
        }
        if (maximum < 0) return new Protocol.LitoVsServerAvailability[0];
        Protocol.LitoVsServerAvailability[] statuses =
                new Protocol.LitoVsServerAvailability[maximum + 1];
        java.util.Arrays.fill(statuses, Protocol.LitoVsServerAvailability.UNAVAILABLE);
        for (Endpoint endpoint : endpoints) {
            if (endpoint.html().equals(html)) {
                statuses[endpoint.serverIndex()] =
                        Protocol.LitoVsServerAvailability.AVAILABLE;
            }
        }
        return statuses;
    }

    static int indexForAction(String action) {
        if (action == null || !action.startsWith("LT_TELEPORT_")
                || action.length() != "LT_TELEPORT_A".length()) {
            throw new IllegalArgumentException("invalid rental teleport action");
        }
        int index = action.charAt(action.length() - 1) - 'A';
        if (index < 0 || index > 3) {
            throw new IllegalArgumentException("invalid rental teleport action");
        }
        return index;
    }

    private static String key(String html, String action) {
        return html + '\u0000' + action.toUpperCase(Locale.ROOT);
    }

    private static long ipv4LittleEndian(String host) {
        try {
            InetAddress resolved = InetAddress.getByName(host);
            if (!(resolved instanceof Inet4Address)) {
                throw new IllegalArgumentException(PROPERTY + " requires an IPv4 address");
            }
            byte[] bytes = resolved.getAddress();
            return (bytes[0] & 0xFFL)
                    | (bytes[1] & 0xFFL) << 8
                    | (bytes[2] & 0xFFL) << 16
                    | (bytes[3] & 0xFFL) << 24;
        } catch (java.net.UnknownHostException error) {
            throw new IllegalArgumentException(PROPERTY
                    + " cannot resolve IPv4 host: " + host, error);
        }
    }
}
