package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/** Optional complete source CON-regeneration table; same owned read-only connection and deadline. */
final class ModernNaturalRegenSourceJdbc {
    static ModernNaturalRegen.Growth read(Connection connection,ModernBackendScope scope,int sourceId,String catalogDatabase) throws Exception {
        ModernPhysicalCombat.identifier(catalogDatabase);
        if(sourceId<1||connection==null||scope==null||ModernBackendScope.current()!=scope)throw unavailable();
        scope.checkActive();
        try {
            if(!connection.isReadOnly())throw unavailable();
            var values=new ArrayList<ModernNaturalRegen.ConRow>(50);
            String sql="SELECT id,`回血` AS hp_regen_maximum FROM `"+catalogDatabase+"`.`角色_體質成長` ORDER BY id LIMIT 51";
            try(var statement=connection.prepareStatement(sql)) {
                statement.setMaxRows(51);statement.setQueryTimeout((int)Math.max(1,((long)scope.remainingMillis()+999L)/1000L));
                scope.checkActive();
                try(var rows=statement.executeQuery()) {
                    while(rows.next()) {
                        scope.checkActive();if(values.size()==50)throw unavailable();
                        long id=ModernJdbcNumbers.required(rows,"id"),maximum=ModernJdbcNumbers.required(rows,"hp_regen_maximum");
                        if(id!=values.size()+1||maximum<1)throw unavailable();values.add(new ModernNaturalRegen.ConRow(id,maximum));
                    }
                }
            }
            scope.checkActive();return new ModernNaturalRegen.Growth(sourceId,catalogDatabase,values);
        } catch(Exception failure){scope.checkActive();throw unavailable();}
    }
    private static SQLException unavailable(){return new SQLException("modern natural regeneration growth unavailable");}
    private ModernNaturalRegenSourceJdbc(){}
}
