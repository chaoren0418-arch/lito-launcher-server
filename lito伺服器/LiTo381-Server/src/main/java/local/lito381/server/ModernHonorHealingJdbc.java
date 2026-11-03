package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/** Opt-in, one bounded source-owned SELECT. Missing healing bonus is not a manufactured fallback. */
final class ModernHonorHealingJdbc {
    static ModernHonorHealing.Growth read(Connection connection,ModernBackendScope scope,int source,String database)throws Exception{
        ModernPhysicalCombat.identifier(database);scope.checkActive();
        try {
        if(connection==null||!connection.isReadOnly()||ModernBackendScope.current()!=scope)throw new SQLException("honor healing unavailable");
        var rows=new ArrayList<ModernHonorHealing.Row>();
        try(var statement=connection.prepareStatement("SELECT id,`治癒骰加成` AS healing_bonus FROM `"+database+"`.`角色_智力成長` ORDER BY id LIMIT 128")){
            statement.setMaxRows(128);statement.setQueryTimeout((int)Math.max(1L,(scope.remainingMillis()+999L)/1000));scope.checkActive();
            try(var result=statement.executeQuery()){
                while(result.next()){scope.checkActive();if(rows.size()==127)throw new SQLException("honor healing unavailable");rows.add(new ModernHonorHealing.Row(ModernJdbcNumbers.required(result,"id"),ModernJdbcNumbers.required(result,"healing_bonus")));}
            }
        }
        scope.checkActive();return new ModernHonorHealing.Growth(source,database,rows);
        }catch(Exception failure){scope.checkActive();throw new SQLException("honor healing unavailable");}
    }
    private ModernHonorHealingJdbc(){}
}
