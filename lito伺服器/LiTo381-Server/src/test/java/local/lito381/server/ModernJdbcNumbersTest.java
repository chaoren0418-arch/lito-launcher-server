package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernJdbcNumbersTest {
    @Test void integersAndIntegralDecimalsKeepEverySignedI64BitWithoutRounding() throws Exception {
        for (int type : new int[]{Types.TINYINT,Types.SMALLINT,Types.INTEGER,Types.BIGINT,Types.DECIMAL,Types.NUMERIC}) {
            for (String literal : List.of("-9223372036854775808","9223372036854775807","4294967299.000","-0.000","1E+3")) {
                var fixture = new Fixture(type,new BigDecimal(literal));
                assertEquals(new BigDecimal(literal).longValueExact(),ModernJdbcNumbers.required(fixture.rows(),"number"));
                assertEquals(1,fixture.materializations);
            }
        }
    }

    @Test void bitAndBooleanMetadataPreserveRawTinyintValuesInsteadOfCoercingToTrue() throws Exception {
        for (int type : new int[]{Types.BIT,Types.BOOLEAN}) {
            for (long value : new long[]{0,1,2,-1,255}) {
                var fixture = new Fixture(type,BigDecimal.valueOf(value));
                assertEquals(value,ModernJdbcNumbers.required(fixture.rows(),"number"));
            }
        }
    }

    @Test void fractionsAndUnsignedOrSignedOverflowAreRejectedWithoutClampOrWrap() {
        for (String literal : List.of("0.1","-0.1","1.0000000000000000000000001","9223372036854775808",
                "18446744073709551615","-9223372036854775809","1E+1000","1E-1000")) {
            for (int type : new int[]{Types.BIGINT,Types.DECIMAL,Types.NUMERIC}) {
                assertSafeFailure(new Fixture(type,new BigDecimal(literal)));
            }
        }
    }

    @Test void approximateTextAndOtherTypesAreRejectedBeforeReadingEvenNullOrIntegralValues() {
        for (int type : new int[]{Types.FLOAT,Types.REAL,Types.DOUBLE,Types.CHAR,Types.VARCHAR,Types.LONGVARCHAR,
                Types.NVARCHAR,Types.BINARY,Types.VARBINARY,Types.DATE,Types.TIMESTAMP,Types.NULL,Types.OTHER}) {
            for (BigDecimal value : new BigDecimal[]{null,BigDecimal.ONE}) {
                var fixture = new Fixture(type,value);
                assertSafeFailure(fixture); assertEquals(0,fixture.materializations);
            }
        }
    }

    @Test void nullableOnlyReportsSqlNullAndRejectsInconsistentDriverNullFlags() throws Exception {
        var absent = new Fixture(Types.BIGINT,null);
        assertTrue(ModernJdbcNumbers.nullable(absent.rows(),"number").isEmpty());
        assertSafeFailure(new Fixture(Types.BIGINT,null));
        for (BigDecimal value : new BigDecimal[]{null,BigDecimal.ZERO}) {
            var mismatch = new Fixture(Types.DECIMAL,value); mismatch.inconsistentNull=true;
            assertSafeFailure(mismatch);
        }
        assertEquals(0,ModernJdbcNumbers.nullable(new Fixture(Types.INTEGER,BigDecimal.ZERO).rows(),"number").orElseThrow());
    }

    @Test void metadataAndReadFailuresCannotLeakDriverDetailsOrCauseChains() {
        for (String stage : List.of("findColumn","getMetaData","getColumnType","getBigDecimal","wasNull")) {
            var fixture = new Fixture(Types.INTEGER,BigDecimal.ONE); fixture.failureStage=stage;
            assertSafeFailure(fixture);
        }
    }

    private static void assertSafeFailure(Fixture fixture) {
        var failure=assertThrows(SQLException.class,()->ModernJdbcNumbers.required(fixture.rows(),"number"));
        assertEquals("modern exact integer unavailable",failure.getMessage());
        assertNull(failure.getCause()); assertNull(failure.getNextException()); assertEquals(0,failure.getSuppressed().length);
    }

    private static final class Fixture {
        final int type; final BigDecimal value;
        int materializations; boolean inconsistentNull; String failureStage;
        Fixture(int type,BigDecimal value) { this.type=type;this.value=value; }
        Object invoke(String method) throws SQLException {
            if (method.equals(failureStage)) throw new SQLException("SYNTHETIC secret SQL value",new Exception("SYNTHETIC private cause"));
            return switch(method) {
                case "findColumn" -> 1;
                case "getMetaData" -> Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSetMetaData.class},
                        (p,m,a)->invoke(m.getName()));
                case "getColumnType" -> type;
                case "getBigDecimal" -> { materializations++;yield value; }
                case "wasNull" -> inconsistentNull ? value!=null : value==null;
                default -> throw new AssertionError("unexpected numeric JDBC method: "+method);
            };
        }
        ResultSet rows() {
            return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)-> {
                if(m.getName().equals("getBigDecimal"))assertEquals(1,a.length,"rounding overload is forbidden");
                return invoke(m.getName());
            });
        }
    }

    /** Tiny adapter for the three existing map-backed JDBC fixtures, never production. */
    @FunctionalInterface interface Value { Object read(String column) throws SQLException; }
    static final class Columns {
        private final Value values;
        private final Map<String,Integer> types;
        private final List<String> labels=new ArrayList<>();
        Columns(Value values) { this(values,Map.of()); }
        Columns(Value values,Map<String,Integer> types) { this.values=values;this.types=types; }
        int find(String column) { if(!labels.contains(column))labels.add(column);return labels.indexOf(column)+1; }
        String name(Object index) { return index instanceof Integer number ? labels.get(number-1) : (String)index; }
        ResultSetMetaData metadata() {
            return (ResultSetMetaData)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSetMetaData.class},(p,m,a)-> {
                if(!m.getName().equals("getColumnType"))throw new AssertionError("unexpected fixture metadata method: "+m.getName());
                String column=name(a[0]);return types.getOrDefault(column,type(values.read(column)));
            });
        }
        static BigDecimal decimal(Object value) {
            if(value==null)return null;
            if(value instanceof BigDecimal decimal)return decimal;
            if(value instanceof BigInteger integer)return new BigDecimal(integer);
            if(value instanceof Boolean flag)return flag?BigDecimal.ONE:BigDecimal.ZERO;
            if(value instanceof Number number)return new BigDecimal(number.toString());
            throw new AssertionError("non-numeric value must be rejected before getBigDecimal");
        }
        private static int type(Object value) {
            if(value instanceof BigDecimal)return Types.DECIMAL;
            if(value instanceof Float)return Types.REAL;
            if(value instanceof Double)return Types.DOUBLE;
            if(value instanceof String)return Types.VARCHAR;
            if(value instanceof Boolean)return Types.BIT;
            if(value instanceof java.sql.Timestamp)return Types.TIMESTAMP;
            return Types.BIGINT;
        }
    }
}
