package local.lito381.server;

import java.sql.*;
import java.util.*;

/** Read-only compatibility for each selected source's retained BUFF schema. */
final class SourceBuffRead {
    private SourceBuffRead() {}

    static boolean versioned(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (PreparedStatement q = connection.prepareStatement("SELECT * FROM character_buff LIMIT 0");
             ResultSet rows = q.executeQuery()) {
            ResultSetMetaData metadata = rows.getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                columns.add(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT));
            }
        }
        if (!columns.containsAll(Set.of("char_obj_id", "skill_id", "remaining_time", "poly_id"))) {
            throw new SQLException("Unsupported selected-source character_buff columns");
        }
        if (!columns.contains("snapshot_version")) return false;
        // A version column without its completion marker is not a single-table schema.
        try (PreparedStatement q = connection.prepareStatement(
                "SELECT char_obj_id,completed_version FROM character_buff_snapshot LIMIT 0");
             ResultSet ignored = q.executeQuery()) {
            return true;
        }
    }

    static PreparedStatement healthStatement(Connection connection, String legacyProjection)
            throws SQLException {
        return connection.prepareStatement(versioned(connection) ? legacyProjection
                : "SELECT char_obj_id,skill_id,remaining_time,poly_id FROM character_buff WHERE char_obj_id=? LIMIT 0");
    }

    static RepositoryBuffSnapshot load(Connection connection, long characterObjectId)
            throws SQLException {
        boolean versioned = versioned(connection);
        long completedVersion = 0;
        if (versioned) {
            try (PreparedStatement marker = connection.prepareStatement(
                    "SELECT completed_version FROM character_buff_snapshot WHERE char_obj_id=?")) {
                marker.setLong(1, characterObjectId);
                try (ResultSet row = marker.executeQuery()) {
                    if (row.next()) completedVersion = row.getLong(1);
                }
            }
        }
        List<RepositoryBuff> buffs = new ArrayList<>();
        String sql = "SELECT skill_id,remaining_time,poly_id FROM character_buff WHERE char_obj_id=?"
                + (versioned ? " AND snapshot_version=?" : "") + " ORDER BY skill_id";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setLong(1, characterObjectId);
            if (versioned) query.setLong(2, completedVersion);
            try (ResultSet row = query.executeQuery()) {
                while (row.next()) {
                    try {
                        buffs.add(new RepositoryBuff(row.getInt("skill_id"),
                                row.getInt("remaining_time"), row.getInt("poly_id")));
                    } catch (IllegalArgumentException invalid) {
                        throw new SQLException("invalid selected-source buff row for character: "
                                + characterObjectId, invalid);
                    }
                }
            }
        }
        try {
            return new RepositoryBuffSnapshot(true, completedVersion, buffs);
        } catch (IllegalArgumentException invalid) {
            throw new SQLException("invalid selected-source buff snapshot for character: "
                    + characterObjectId, invalid);
        }
    }
}
