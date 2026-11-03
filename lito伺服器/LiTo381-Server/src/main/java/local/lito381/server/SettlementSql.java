package local.lito381.server;
import java.sql.*;import java.util.*;
/** MyISAM-safe ordered application: receipt, atomic counter+token write, receipt acknowledgement. */
final class SettlementSql {
 static SharedDatabase.Route route(String database)throws SQLException{synchronized(SharedDatabase.ROUTES){return SharedDatabase.ROUTES.values().stream().filter(r->r.database.equals(database)).findFirst().orElseThrow(()->new SQLException("Settlement source unavailable: "+database));}}
 static String table(SharedDatabase.Route r){return "`"+SharedDatabase.identifier(r.controls)+"`.`lito_battle_settlement`";}
 static void check(SharedDatabase.Route r)throws SQLException{try(var c=r.connection();var s=c.createStatement()){s.executeQuery("SELECT last_result_id FROM character_lito LIMIT 0").close();s.executeQuery("SELECT result_id,battle_id,source_database,char_obj_id,won,disconnected,applied FROM "+table(r)+" LIMIT 0").close();try(var p=c.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME='lito_battle_settlement'")){p.setString(1,r.controls);try(var rows=p.executeQuery()){if(!rows.next()||!"InnoDB".equalsIgnoreCase(rows.getString(1)))throw new SQLException("lito_battle_settlement must use InnoDB");}}}}
 static void apply(List<SettlementJournal.Entry> entries)throws Exception{
  if(entries.isEmpty())return;SharedDatabase.Route first=route(entries.get(0).database());
  for(var e:entries)if(!e.controls().equals(first.controls))throw new SQLException("mixed settlement controls database");
  try(var c=first.connection()){
   c.setAutoCommit(false);
   try{for(var e:entries){
    String sql="INSERT INTO "+table(first)+"(result_id,battle_id,source_database,source_name,char_obj_id,char_name,won,disconnected) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE result_id=result_id";
    try(var p=c.prepareStatement(sql)){p.setString(1,e.id());p.setString(2,e.battle());p.setString(3,e.database());p.setString(4,e.source());p.setLong(5,e.character());p.setString(6,e.name());p.setBoolean(7,e.won());p.setBoolean(8,e.disconnected());p.executeUpdate();}
    try(var p=c.prepareStatement("SELECT battle_id,source_database,char_obj_id,char_name,won,disconnected FROM "+table(first)+" WHERE result_id=?")){p.setString(1,e.id());try(var r=p.executeQuery()){if(!r.next()||!r.getString(1).equals(e.battle())||!r.getString(2).equals(e.database())||r.getLong(3)!=e.character()||!r.getString(4).equals(e.name())||r.getBoolean(5)!=e.won()||r.getBoolean(6)!=e.disconnected())throw new SQLException("Conflicting frozen settlement: "+e.id());}}
   }c.commit();}catch(Exception ex){c.rollback();throw ex;}
  }
  for(var e:entries)drain(route(e.database()),e.character());
 }
 static void drainAll()throws Exception{
  List<SharedDatabase.Route> routes;synchronized(SharedDatabase.ROUTES){routes=List.copyOf(SharedDatabase.ROUTES.values());}
  for(var route:routes){List<Long> ids=new ArrayList<>();try(var c=route.connection();var p=c.prepareStatement("SELECT DISTINCT char_obj_id FROM "+table(route)+" WHERE source_database=? AND applied=0")){p.setString(1,route.database);try(var r=p.executeQuery()){while(r.next())ids.add(r.getLong(1));}}for(long id:ids)drain(route,id);}
 }
 static void drain(SharedDatabase.Route route,long id)throws Exception{
  String lockName=SettlementJournal.Entry.key("00000000-0000-0000-0000-000000000000",route.controls+"_"+route.database,id);
  try(var c=route.connection()){
   c.setAutoCommit(true);try(var p=c.prepareStatement("SELECT GET_LOCK(?,5)")){p.setString(1,lockName);try(var r=p.executeQuery()){if(!r.next()||r.getInt(1)!=1)throw new SQLException("Settlement lock busy");}}
   try{
    List<String> pending=new ArrayList<>();try(var p=c.prepareStatement("SELECT result_id FROM "+table(route)+" WHERE source_database=? AND char_obj_id=? AND applied=0 ORDER BY sequence_id")){p.setString(1,route.database);p.setLong(2,id);try(var r=p.executeQuery()){while(r.next())pending.add(r.getString(1));}}
    for(String token:pending){String name;boolean won;
     try(var p=c.prepareStatement("SELECT char_name,won FROM "+table(route)+" WHERE result_id=? AND applied=0")){p.setString(1,token);try(var r=p.executeQuery()){if(!r.next())continue;name=r.getString(1);won=r.getBoolean(2);}}
     try(var p=c.prepareStatement("SELECT char_name FROM characters WHERE objid=?")){p.setLong(1,id);try(var r=p.executeQuery()){if(!r.next()||!name.equals(r.getString(1)))throw new SQLException("Settlement character identity changed: "+route.database+"/"+id);}}
     long wins=0,losses=0,version=0;String last="";boolean exists;
     try(var p=c.prepareStatement("SELECT wins,losses,version,last_result_id FROM character_lito WHERE char_obj_id=?")){p.setLong(1,id);try(var r=p.executeQuery()){exists=r.next();if(exists){wins=r.getLong(1);losses=r.getLong(2);version=r.getLong(3);last=r.getString(4);}}}
     if(!token.equals(last)){
      if(version>=0xFFFF_FFFFL || wins>=Integer.MAX_VALUE || losses>=Integer.MAX_VALUE)throw new SQLException("Settlement counters exhausted");
      if(exists){try(var p=c.prepareStatement("UPDATE character_lito SET char_name=?,wins=?,losses=?,version=?,last_result_id=? WHERE char_obj_id=? AND version=? AND last_result_id=?")){p.setString(1,name);p.setLong(2,wins+(won?1:0));p.setLong(3,losses+(won?0:1));p.setLong(4,version+1);p.setString(5,token);p.setLong(6,id);p.setLong(7,version);p.setString(8,last);if(p.executeUpdate()!=1)throw new SQLException("Settlement concurrent writer conflict");}}
      else{try(var p=c.prepareStatement("INSERT INTO character_lito(char_obj_id,char_name,wins,losses,version,last_result_id) VALUES(?,?,?,?,1,?)")){p.setLong(1,id);p.setString(2,name);p.setInt(3,won?1:0);p.setInt(4,won?0:1);p.setString(5,token);p.executeUpdate();}}
     }
     // The marker is in the SAME MyISAM row update as the counters. If this acknowledgement fails, replay sees the marker and does not add twice.
     try(var p=c.prepareStatement("UPDATE "+table(route)+" SET applied=1,applied_at=CURRENT_TIMESTAMP WHERE result_id=?")){p.setString(1,token);p.executeUpdate();}
     synchronized(route){route.runtime.remove(id);}
    }
   }finally{try(var p=c.prepareStatement("SELECT RELEASE_LOCK(?)")){p.setString(1,lockName);p.executeQuery().close();}}
  }
 }
 static RepositoryTournamentProfile current(SettlementJournal.Entry e)throws Exception{
  var route=route(e.database());try(var c=route.connection();var p=c.prepareStatement("SELECT wins,losses,version FROM character_lito WHERE char_obj_id=?")){p.setLong(1,e.character());try(var r=p.executeQuery()){if(!r.next())throw new SQLException("Settlement counter missing");return route.profile(e.character(),e.name(),r.getInt(1),r.getInt(2),r.getLong(3));}}
 }
}
