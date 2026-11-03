package local.lito381.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/** One bounded full-row SELECT on the caller's owned read-only connection.
 * Missing/denied/NULL/non-integral fields never become no proc or zero effects. */
final class ModernArmorProcsJdbc {
    private ModernArmorProcsJdbc(){}
    static ModernArmorProcs.Catalog read(Connection connection,ModernBackendScope scope,int sourceId,
            String database,List<Long> requested)throws Exception {
        if(connection==null||scope==null||scope!=ModernBackendScope.current()||sourceId<1)throw unavailable();
        ModernPhysicalCombat.identifier(database);scope.checkActive();
        try {
            if(!connection.isReadOnly()||requested==null||requested.size()>ModernCharacterData.MAX_ITEMS)throw unavailable();
            var ids=new TreeSet<Long>();
            for(var id:requested)if(id==null||id<1||!ids.add(id))throw unavailable();
            var result=new TreeMap<Long,List<ModernArmorProcs.Rule>>();
            for(var id:ids)result.put(id,new ArrayList<>());
            if(ids.isEmpty())return new ModernArmorProcs.Catalog(sourceId,database,result);
            String columns=String.join(",",ModernArmorProcs.COLUMNS.stream().map(name->"`"+name+"`").toList());
            String sql="SELECT "+columns+" FROM `"+database+"`.`裝備_armor_魔法` WHERE item_id IN ("
                    +String.join(",",java.util.Collections.nCopies(ids.size(),"?"))+") ORDER BY id LIMIT 1025";
            try(var statement=connection.prepareStatement(sql)) {
                statement.setMaxRows(ModernArmorProcs.MAX_ROWS+1);
                statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999)/1000));
                int index=1;for(var id:ids)statement.setLong(index++,id);
                scope.checkActive();
                try(var rows=statement.executeQuery()) {
                    int count=0;long previous=0;
                    while(rows.next()) {
                        scope.checkActive();var values=new long[ModernArmorProcs.COLUMNS.size()];
                        for(int i=0;i<values.length;i++)values[i]=ModernJdbcNumbers.required(rows,ModernArmorProcs.COLUMNS.get(i));
                        if(++count>ModernArmorProcs.MAX_ROWS||values[0]<=previous||!ids.contains(values[1]))throw unavailable();
                        previous=values[0];
                        result.get(values[1]).add(new ModernArmorProcs.Rule(values[0],values[1],values[2],values[3],values[4],values[5],
                                values[6],values[7],values[8],values[9],values[10],values[11],values[12],values[13],values[14],values[15],values[16],values[17]));
                    }
                }
            }
            scope.checkActive();return new ModernArmorProcs.Catalog(sourceId,database,result);
        }catch(Exception failure){scope.checkActive();throw unavailable();}
    }
    private static SQLException unavailable(){return new SQLException("modern armor proc source unavailable");}
}
