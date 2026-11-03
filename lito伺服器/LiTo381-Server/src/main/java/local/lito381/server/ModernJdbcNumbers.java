package local.lito381.server;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.OptionalLong;

/** Exact i64 boundary for the modern readers; never rounds, clamps or parses text. */
final class ModernJdbcNumbers {
    private ModernJdbcNumbers() {}

    static long required(ResultSet rows, String column) throws SQLException {
        OptionalLong value = nullable(rows,column);
        if (value.isEmpty()) throw unavailable();
        return value.getAsLong();
    }

    /**
     * SQL NULL is only reported, not defaulted: each caller retains its explicit
     * outer-join/nullable-extension policy. Metadata is obtained from this result,
     * not a separate schema query or cache that could outlive a result set.
     * Connector/J maps tinyint(1) to BIT/BOOLEAN. Its no-scale BigDecimal reader
     * preserves the underlying integer (including 2), unlike getObject(Boolean).
     * Existing flag/domain validation must therefore still run after this read.
     */
    static OptionalLong nullable(ResultSet rows, String column) throws SQLException {
        try {
            if (rows == null || column == null) throw unavailable();
            int index = rows.findColumn(column);
            var metadata = rows.getMetaData();
            if (index < 1 || metadata == null) throw unavailable();
            int type = metadata.getColumnType(index);
            // Reject approximate and character types BEFORE materializing their
            // values. getBigDecimal alone could coerce FLOAT/DOUBLE or text.
            switch (type) {
                case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                        Types.DECIMAL, Types.NUMERIC, Types.BIT, Types.BOOLEAN -> { }
                default -> throw unavailable();
            }
            BigDecimal value = rows.getBigDecimal(index); // Never the scale/rounding overload.
            boolean wasNull = rows.wasNull();
            if ((value == null) != wasNull) throw unavailable();
            return value == null ? OptionalLong.empty() : OptionalLong.of(value.longValueExact());
        } catch (SQLException | ArithmeticException failure) {
            // Driver details may include SQL, values or connection information.
            throw unavailable();
        }
    }

    private static SQLException unavailable() { return new SQLException("modern exact integer unavailable"); }
}
