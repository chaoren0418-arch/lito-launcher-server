package local.lito381.server;
import java.sql.*;import java.util.*;
/** Source SkillCoexistSet read on a borrowed, already read-only transaction; no global policy cache. */
final class NativeSourceIcons1Policy {
 record Columns(String table,String eventClass,String eventStart){Columns{identifier(table);identifier(eventClass);identifier(eventStart);}}
 record Policy(String database,boolean enabled,String state){}
 static String identifier(String value){if(value==null||!value.matches("[\\p{L}\\p{N}_]+"))throw new IllegalArgumentException("invalid Source policy identifier");return "`"+value+"`";}
 static Policy read(Connection connection,String database){return read(connection,database,new Columns("server_event","eventclass","eventstart"));}
 static Policy read(Connection connection,String database,Columns columns){String db=identifier(database);try{
  if(!connection.isReadOnly()||connection.getAutoCommit())throw new SQLException("Source coexist policy requires borrowed read-only transaction");
  String sql="SELECT "+identifier(columns.eventStart())+" FROM "+db+"."+identifier(columns.table())+" WHERE "+identifier(columns.eventClass())+"=? LIMIT 1";
  try(var statement=connection.prepareStatement(sql)){statement.setString(1,"SkillCoexistSet");try(var rows=statement.executeQuery()){if(rows.next())return new Policy(database,rows.getInt(1)==1,"loaded");return new Policy(database,false,"absent");}}
 }catch(SQLException failure){return new Policy(database,false,"unavailable");}}
 static NativeProcSkillCatalog.Catalog catalog(String database,Map<Integer,NativeProcSkillCatalog.Skill> skills,Connection connection){return new NativeProcSkillCatalog.Catalog(database,skills,read(connection,database));}
 static Policy of(NativeProcSkillCatalog.Catalog catalog){Object value=catalog.nativeSourceIcons1Policy;return value instanceof Policy policy&&Objects.equals(policy.database(),catalog.database())?policy:new Policy(catalog.database(),false,"unavailable");}
 private NativeSourceIcons1Policy(){}
}
