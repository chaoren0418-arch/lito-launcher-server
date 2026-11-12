package local.lito381.server;
import java.sql.*;import java.util.*;
/** MyISAM-safe ordered application: receipt, atomic counter+token write, receipt acknowledgement. */
final class SettlementSql {
 /** Durable character_lito ranking columns (T103b / #310 D3); added by db/lito_character_ranking.sql. */
 static final String RANKING_COLUMNS="ranking_points,disconnects,monthly_period,monthly_ranking_points,monthly_wins,monthly_losses";
 static final String COUNTER_COLUMNS="wins,losses,"+RANKING_COLUMNS;
 /** One character_lito row's counters. monthlyPeriod is the YYYYMM key of the last write; 0 means never. */
 record Counters(long wins,long losses,long rankingPoints,long disconnects,int monthlyPeriod,long monthlyRankingPoints,long monthlyWins,long monthlyLosses){
  static final Counters ZERO=new Counters(0,0,0,0,0,0,0,0);
  Counters{if(wins<0||losses<0||rankingPoints<0||disconnects<0||monthlyPeriod<0||monthlyRankingPoints<0||monthlyWins<0||monthlyLosses<0)throw new IllegalArgumentException("negative character_lito counter");}
  /** Reads {@link #COUNTER_COLUMNS} starting at column {@code first}. */
  static Counters read(ResultSet r,int first)throws SQLException{
   return new Counters(r.getLong(first),r.getLong(first+1),r.getLong(first+2),r.getLong(first+3),r.getInt(first+4),r.getLong(first+5),r.getLong(first+6),r.getLong(first+7));
  }
  /** Binds {@link #COUNTER_COLUMNS} starting at parameter {@code first}; returns the next parameter index. */
  int bind(PreparedStatement p,int first)throws SQLException{
   p.setLong(first,wins);p.setLong(first+1,losses);p.setLong(first+2,rankingPoints);p.setLong(first+3,disconnects);p.setInt(first+4,monthlyPeriod);p.setLong(first+5,monthlyRankingPoints);p.setLong(first+6,monthlyWins);p.setLong(first+7,monthlyLosses);return first+8;
  }
  static Counters of(GameStateStore.TournamentProfileState s){
   return new Counters(s.wins(),s.losses(),s.rankingPoints(),s.disconnects(),s.monthlyPeriod(),s.monthlyRankingPoints(),s.monthlyWins(),s.monthlyLosses());
  }
 }
 /**
  * The single durable scoring rule; identical to {@code GameStateStore.recordResult}:
  * reward = win or loss reward, a disconnect subtracts the loss reward instead, totals never drop below 0,
  * and the monthly accumulator restarts when the ranking month (05:00 Asia/Taipei boundary) changed.
  */
 static Counters next(Counters c,boolean won,boolean disconnected,int winReward,int lossReward,int period){
  if(won&&disconnected)throw new IllegalArgumentException("a disconnected result cannot be a win");
  if(period<=0)throw new IllegalArgumentException("invalid ranking period");
  boolean sameMonth=c.monthlyPeriod()==period;
  long reward=Math.max(0,won?winReward:lossReward),delta=disconnected?-reward:reward;
  long monthlyPoints=sameMonth?c.monthlyRankingPoints():0,monthlyWins=sameMonth?c.monthlyWins():0,monthlyLosses=sameMonth?c.monthlyLosses():0;
  return new Counters(Math.addExact(c.wins(),won?1:0),Math.addExact(c.losses(),won?0:1),Math.max(0,Math.addExact(c.rankingPoints(),delta)),
   Math.addExact(c.disconnects(),disconnected?1:0),period,Math.max(0,Math.addExact(monthlyPoints,delta)),
   Math.addExact(monthlyWins,won?1:0),Math.addExact(monthlyLosses,won?0:1));
 }
 /** Values the in-memory TournamentProfileState can hold (signed dwords, disconnects within losses). */
 static boolean representable(Counters c){
  return c.wins()+c.losses()<=Integer.MAX_VALUE&&c.disconnects()<=c.losses()&&c.monthlyWins()<=Integer.MAX_VALUE&&c.monthlyLosses()<=Integer.MAX_VALUE;
 }
 static SharedDatabase.Route route(String database)throws SQLException{synchronized(SharedDatabase.ROUTES){return SharedDatabase.ROUTES.values().stream().filter(r->r.database.equals(database)).findFirst().orElseThrow(()->new SQLException("Settlement source unavailable: "+database));}}
 static String table(SharedDatabase.Route r){return "`"+SharedDatabase.identifier(r.controls)+"`.`lito_battle_settlement`";}
 static void check(SharedDatabase.Route r)throws SQLException{try(var c=r.connection();var s=c.createStatement()){s.executeQuery("SELECT last_result_id FROM character_lito LIMIT 0").close();
  try{s.executeQuery("SELECT "+RANKING_COLUMNS+" FROM character_lito LIMIT 0").close();}catch(SQLException missing){throw new SQLException("character_lito in "+r.database+" lacks ranking columns; run db/lito_character_ranking.sql first",missing);}
  s.executeQuery("SELECT result_id,battle_id,source_database,char_obj_id,won,disconnected,applied FROM "+table(r)+" LIMIT 0").close();try(var p=c.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME='lito_battle_settlement'")){p.setString(1,r.controls);try(var rows=p.executeQuery()){if(!rows.next()||!"InnoDB".equalsIgnoreCase(rows.getString(1)))throw new SQLException("lito_battle_settlement must use InnoDB");}}}}
 static void apply(List<SettlementJournal.Entry> entries)throws Exception{
  if(entries.isEmpty())return;SharedDatabase.Route first=route(entries.get(0).database());
  for(var e:entries)if(!e.controls().equals(first.controls))throw new SQLException("mixed settlement controls database");
  receipts(first,entries);
  // Every receipt is frozen above; one character that cannot be drained must not leave the others of the same battle pending (T124).
  Exception failure=null;
  for(var e:entries)try{drain(route(e.database()),e.character());}catch(Exception ex){failure=collect(failure,ex);}
  if(failure!=null)throw failure;
 }
 static Exception collect(Exception first,Exception next){if(first==null)return next;if(first!=next)first.addSuppressed(next);return first;}
 /** Freezes receipts in the InnoDB control database; replaying the same result id is a no-op. */
 static void receipts(SharedDatabase.Route first,List<SettlementJournal.Entry> entries)throws Exception{
  try(var c=first.connection()){
   c.setAutoCommit(false);
   try{for(var e:entries){
    String sql="INSERT INTO "+table(first)+"(result_id,battle_id,source_database,source_name,char_obj_id,char_name,won,disconnected) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE result_id=result_id";
    try(var p=c.prepareStatement(sql)){p.setString(1,e.id());p.setString(2,e.battle());p.setString(3,e.database());p.setString(4,e.source());p.setLong(5,e.character());p.setString(6,e.name());p.setBoolean(7,e.won());p.setBoolean(8,e.disconnected());p.executeUpdate();}
    try(var p=c.prepareStatement("SELECT battle_id,source_database,char_obj_id,char_name,won,disconnected FROM "+table(first)+" WHERE result_id=?")){p.setString(1,e.id());try(var r=p.executeQuery()){if(!r.next()||!r.getString(1).equals(e.battle())||!r.getString(2).equals(e.database())||r.getLong(3)!=e.character()||!r.getString(4).equals(e.name())||r.getBoolean(5)!=e.won()||r.getBoolean(6)!=e.disconnected())throw new SQLException("Conflicting frozen settlement: "+e.id());}}
   }c.commit();}catch(Exception ex){c.rollback();throw ex;}
  }
 }
 /** Drains every pending receipt; a failing source or character is reported after the others were attempted (T124). */
 static void drainAll()throws Exception{
  List<SharedDatabase.Route> routes;synchronized(SharedDatabase.ROUTES){routes=List.copyOf(SharedDatabase.ROUTES.values());}
  Exception failure=null;
  for(var route:routes){List<Long> ids=new ArrayList<>();
   try(var c=route.connection();var p=c.prepareStatement("SELECT DISTINCT char_obj_id FROM "+table(route)+" WHERE source_database=? AND applied=0")){p.setString(1,route.database);try(var r=p.executeQuery()){while(r.next())ids.add(r.getLong(1));}}
   catch(Exception e){failure=collect(failure,e);continue;}
   for(long id:ids)try{drain(route,id);}catch(Exception e){failure=collect(failure,e);}
  }
  if(failure!=null)throw failure;
 }
 static void drain(SharedDatabase.Route route,long id)throws Exception{
  String lockName=SettlementJournal.Entry.key("00000000-0000-0000-0000-000000000000",route.controls+"_"+route.database,id);
  try(var c=route.connection()){
   c.setAutoCommit(true);try(var p=c.prepareStatement("SELECT GET_LOCK(?,5)")){p.setString(1,lockName);try(var r=p.executeQuery()){if(!r.next()||r.getInt(1)!=1)throw new SQLException("Settlement lock busy");}}
   try{
    List<String> pending=new ArrayList<>();try(var p=c.prepareStatement("SELECT result_id FROM "+table(route)+" WHERE source_database=? AND char_obj_id=? AND applied=0 ORDER BY sequence_id")){p.setString(1,route.database);p.setLong(2,id);try(var r=p.executeQuery()){while(r.next())pending.add(r.getString(1));}}
    for(String token:pending){String name;boolean won,disconnected;
     try(var p=c.prepareStatement("SELECT char_name,won,disconnected FROM "+table(route)+" WHERE result_id=? AND applied=0")){p.setString(1,token);try(var r=p.executeQuery()){if(!r.next())continue;name=r.getString(1);won=r.getBoolean(2);disconnected=r.getBoolean(3);}}
     try(var p=c.prepareStatement("SELECT char_name FROM characters WHERE objid=?")){p.setLong(1,id);try(var r=p.executeQuery()){if(!r.next()||!name.equals(r.getString(1)))throw new SQLException("Settlement character identity changed: "+route.database+"/"+id);}}
     Counters counters=Counters.ZERO;long version=0;String last="";boolean exists;
     try(var p=c.prepareStatement("SELECT version,last_result_id,"+COUNTER_COLUMNS+" FROM character_lito WHERE char_obj_id=?")){p.setLong(1,id);try(var r=p.executeQuery()){exists=r.next();if(exists){version=r.getLong(1);last=r.getString(2);counters=Counters.read(r,3);}}}
     if(!token.equals(last)){
      if(version>=0xFFFF_FFFFL || counters.wins()>=Integer.MAX_VALUE || counters.losses()>=Integer.MAX_VALUE)throw new SQLException("Settlement counters exhausted");
      // Points, disconnects and the monthly accumulator are derived from the same frozen receipt
      // (won/disconnected) and land in the same single-row write as the last_result_id marker.
      Counters updated=next(counters,won,disconnected&&!won,route.winReward,route.lossReward,GameStateStore.rankingPeriodKeyAt(System.currentTimeMillis()));
      if(!representable(updated))throw new SQLException("Settlement counters exhausted");
      if(exists){try(var p=c.prepareStatement("UPDATE character_lito SET char_name=?,wins=?,losses=?,ranking_points=?,disconnects=?,monthly_period=?,monthly_ranking_points=?,monthly_wins=?,monthly_losses=?,version=?,last_result_id=? WHERE char_obj_id=? AND version=? AND last_result_id=?")){p.setString(1,name);int i=updated.bind(p,2);p.setLong(i,version+1);p.setString(i+1,token);p.setLong(i+2,id);p.setLong(i+3,version);p.setString(i+4,last);if(p.executeUpdate()!=1)throw new SQLException("Settlement concurrent writer conflict");}}
      else{try(var p=c.prepareStatement("INSERT INTO character_lito(char_obj_id,char_name,"+COUNTER_COLUMNS+",version,last_result_id) VALUES(?,?,?,?,?,?,?,?,?,?,1,?)")){p.setLong(1,id);p.setString(2,name);int i=updated.bind(p,3);p.setString(i,token);p.executeUpdate();}}
     }
     // The marker is in the SAME MyISAM row update as the counters. If this acknowledgement fails, replay sees the marker and does not add twice.
     try(var p=c.prepareStatement("UPDATE "+table(route)+" SET applied=1,applied_at=CURRENT_TIMESTAMP WHERE result_id=?")){p.setString(1,token);p.executeUpdate();}
     synchronized(route){route.runtime.remove(id);}
    }
   }finally{try(var p=c.prepareStatement("SELECT RELEASE_LOCK(?)")){p.setString(1,lockName);p.executeQuery().close();}}
  }
 }
 static RepositoryTournamentProfile current(SettlementJournal.Entry e)throws Exception{return load(route(e.database()),e.character(),e.name());}
 /** Authoritative durable counters for one source character. */
 static RepositoryTournamentProfile load(SharedDatabase.Route route,long character,String name)throws Exception{
  try(var c=route.connection();var p=c.prepareStatement("SELECT version,"+COUNTER_COLUMNS+" FROM character_lito WHERE char_obj_id=?")){p.setLong(1,character);try(var r=p.executeQuery()){if(!r.next())throw new SQLException("Settlement counter missing");return route.profile(character,name,Counters.read(r,2),r.getLong(1));}}
 }
}
