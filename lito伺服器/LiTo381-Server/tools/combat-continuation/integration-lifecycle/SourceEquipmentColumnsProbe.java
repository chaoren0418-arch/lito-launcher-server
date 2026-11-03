package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.lang.reflect.*;import java.util.*;
/** No connection is opened: original query whitelist and exact prepare forwarding contract only. */
public final class SourceEquipmentColumnsProbe {
 static int checks,prepares;static void need(boolean v,String text){checks++;if(!v)throw new AssertionError(text);}
 public static void main(String[]args)throws Exception{
  need(NativeSourceEquipmentColumns.QUERIES.size()==17,"exact existing equipment query count");var all=new HashSet<>(NativeDeItemDefinitionsSql.QUERIES);all.addAll(NativeDeItemDefinitionsSql.TEMPLATE_QUERIES);need(all.size()==18,"existing15 definition and3 template queries only");
  for(var q:all){String expected=NativeSourceEquipmentColumns.QUERIES.getOrDefault(q,q);need(NativeSourceEquipmentColumns.translate(q).equals(expected),"exact immutable Source name projection");var connection=(Connection)Proxy.newProxyInstance(SourceEquipmentColumnsProbe.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{need(m.getName().equals("prepareStatement")&&a.length==1&&a[0].equals(expected),"only one original prepared SELECT with no new transaction or session calls");prepares++;return null;});need(NativeSourceEquipmentColumns.prepare(connection,q)==null,"original prepared statement return is forwarded unchanged");}
  for(var q:List.of("SELECT 1","SELECT * FROM characters","UPDATE weapon SET name='x'","DELETE FROM weapon","ALTER TABLE weapon ADD value int","SELECT * FROM `weapon`; SELECT 1")){boolean denied=false;try{NativeSourceEquipmentColumns.translate(q);}catch(SQLException expected){denied=true;}need(denied,"no new query/SQL-write authority");}
  boolean immutable=false;try{NativeSourceEquipmentColumns.QUERIES.put("x","y");}catch(UnsupportedOperationException expected){immutable=true;}need(immutable&&prepares==18,"read-only projection map, one forwarding per existing read");Files.writeString(Path.of(args[0]),"{\"cases\":24,\"checks\":"+checks+",\"simulated_existing_prepare_calls\":"+prepares+",\"actual_SQL_connections\":0,\"created_Hub\":false,\"native_runtime_cases\":0}\n");
 }
}
