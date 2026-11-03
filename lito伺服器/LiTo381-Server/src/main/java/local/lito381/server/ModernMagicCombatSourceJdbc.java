package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/** One opt-in, bounded SELECT on the physical bundle's same owned read-only connection. */
final class ModernMagicCombatSourceJdbc {
    private ModernMagicCombatSourceJdbc() {}
    static ModernMagicCombat.Growth read(Connection connection, ModernBackendScope scope,
            int sourceId, String catalogDatabase) throws Exception {
        ModernPhysicalCombat.identifier(catalogDatabase);
        if(sourceId<1 || connection==null || scope==null || ModernBackendScope.current()!=scope)throw unavailable();
        scope.checkActive();
        try {
            if(!connection.isReadOnly())throw unavailable();
            var result=new ArrayList<ModernMagicCombat.WisdomRow>(50);
            String sql="SELECT id,`魔防` AS magic_resistance FROM `"+catalogDatabase+"`.`角色_精神成長` ORDER BY id LIMIT 51";
            try(var statement=connection.prepareStatement(sql)) {
                statement.setMaxRows(51);
                statement.setQueryTimeout((int)Math.max(1,((long)scope.remainingMillis()+999L)/1000L));
                scope.checkActive();
                try(var rows=statement.executeQuery()) {
                    while(rows.next()) {
                        scope.checkActive();
                        if(result.size()==50)throw unavailable();
                        long id=ModernJdbcNumbers.required(rows,"id"),mr=ModernJdbcNumbers.required(rows,"magic_resistance");
                        if(id!=result.size()+1 || mr<0)throw unavailable();
                        result.add(new ModernMagicCombat.WisdomRow(id,mr));
                    }
                }
            }
            scope.checkActive();
            return new ModernMagicCombat.Growth(sourceId,catalogDatabase,result);
        } catch(Exception failure) {
            scope.checkActive();
            throw unavailable(); // No driver SQL, endpoint, account or partial source data.
        }
    }
    private static SQLException unavailable(){return new SQLException("modern magic growth unavailable");}
}
