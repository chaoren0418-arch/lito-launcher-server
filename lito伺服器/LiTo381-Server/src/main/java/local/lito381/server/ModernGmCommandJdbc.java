package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * One SELECT in the exact source DAO's owned connection and deadline. The source
 * accounts/characters and its explicitly configured catalog.commands are joined;
 * no fallback admin, schema writes, password reads, reflection or cached grants.
 */
final class ModernGmCommandJdbc {
    private ModernGmCommandJdbc() {}
    static String sql(String catalog) {
        if (catalog == null || !catalog.matches("[A-Za-z0-9_]{1,64}"))
            throw new IllegalArgumentException("explicit GM catalog identifier required");
        return "SELECT LEFT(a.login,65) AS canonical_account,OCTET_LENGTH(a.login) AS canonical_account_bytes,"
                + "a.access_level,a.banned AS account_banned,c.objid,"
                + "LEFT(c.char_name,257) AS char_name,OCTET_LENGTH(c.char_name) AS char_name_bytes,"
                + "c.Type AS character_type,c.Sex AS character_sex,c.level,c.Banned AS character_banned,"
                + "LEFT(cmd.name,33) AS command_name,OCTET_LENGTH(cmd.name) AS command_name_bytes,"
                + "cmd.access_level AS required_access,LEFT(cmd.class_name,129) AS executor,"
                + "OCTET_LENGTH(cmd.class_name) AS executor_bytes,cmd.`system` AS command_system"
                + " FROM accounts a JOIN characters c ON c.account_name=a.login"
                + " LEFT JOIN `" + catalog + "`.`commands` cmd ON cmd.name=?"
                + " WHERE a.login=? AND c.objid=? LIMIT 2";
    }
    static Optional<ModernGmCommands.Snapshot> read(Connection connection, ModernBackendScope scope,
            int sourceId, String canonicalAccount, long objectId, String catalog) throws Exception {
        if (sourceId <= 0 || objectId <= 0 || !ModernGmCommands.validText(canonicalAccount,64)) throw unavailable();
        String query = sql(catalog);
        scope.checkActive();
        try {
            Optional<ModernGmCommands.Snapshot> result;
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setMaxRows(2);
                statement.setQueryTimeout(Math.max(1,(scope.remainingMillis()+999)/1000));
                statement.setString(1,ModernGmCommands.NAME);
                statement.setString(2,canonicalAccount);
                statement.setLong(3,objectId);
                scope.checkActive();
                try (ResultSet rows = statement.executeQuery()) {
                    scope.checkActive();
                    if (!rows.next()) result = Optional.empty();
                    else {
                        scope.checkActive();
                        String owner = text(rows,"canonical_account",64);
                        long id = number(rows,"objid"), type = number(rows,"character_type"), sex = number(rows,"character_sex");
                        if (!canonicalAccount.equals(owner) || id != objectId || type < 0 || type > 65535 || sex < 0 || sex > 1)
                            throw unavailable();
                        var character = new ModernApplicationProtocol.CharacterRow(id,text(rows,"char_name",256),
                                (int)type,(int)sex,number(rows,"level"));
                        int access = access(rows,"access_level");
                        boolean accountBanned = flag(rows,"account_banned"), characterBanned = flag(rows,"character_banned");
                        ModernGmCommands.Definition definition = null;
                        String name = rows.getString("command_name");
                        if (name == null) {
                            // A LEFT JOIN miss is different from an unreadable table,
                            // a partially NULL command or malformed numeric schema.
                            if (ModernJdbcNumbers.nullable(rows,"command_name_bytes").isPresent()
                                    || ModernJdbcNumbers.nullable(rows,"required_access").isPresent()
                                    || rows.getString("executor") != null
                                    || ModernJdbcNumbers.nullable(rows,"executor_bytes").isPresent()
                                    || ModernJdbcNumbers.nullable(rows,"command_system").isPresent()) throw unavailable();
                        } else definition = new ModernGmCommands.Definition(text(rows,"command_name",32),
                                access(rows,"required_access"),text(rows,"executor",128),flag(rows,"command_system"));
                        var snapshot = new ModernGmCommands.Snapshot(sourceId,owner,character,access,accountBanned,characterBanned,definition);
                        if (rows.next()) throw unavailable(); // no LIMIT-1 winner among ambiguous accounts/commands/characters
                        result = Optional.of(snapshot);
                    }
                    scope.checkActive();
                }
            }
            scope.checkActive();
            return result;
        } catch (Exception failure) {
            scope.checkActive(); // cancellation/deadline takes priority over a source error
            throw unavailable(); // driver details and close/suppressed messages cannot escape
        }
    }
    private static long number(ResultSet rows,String name) throws SQLException { return ModernJdbcNumbers.required(rows,name); }
    private static int access(ResultSet rows,String name) throws SQLException {
        long value = number(rows,name);
        if (value < 0 || value > 200) throw unavailable();
        return (int)value; // never clamp an invalid permission into a grant
    }
    private static boolean flag(ResultSet rows,String name) throws SQLException {
        long value = number(rows,name); if (value != 0 && value != 1) throw unavailable(); return value == 1;
    }
    private static String text(ResultSet rows,String name,int max) throws SQLException {
        String value = rows.getString(name); long bytes = number(rows,name+"_bytes");
        if (!ModernGmCommands.validText(value,max) || bytes != value.getBytes(StandardCharsets.UTF_8).length) throw unavailable();
        return value;
    }
    private static SQLException unavailable() { return new SQLException("modern GM source unavailable"); }
}
