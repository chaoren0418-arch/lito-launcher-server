package local.lito381.server;
import java.lang.reflect.*;import java.util.*;import java.nio.charset.Charset;import java.io.IOException;
/** Live presentation over existing source-qualified state; no extra database writes. */
public final class LiveBattleStats {
 static Field field(Class<?> c,String n){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(Exception e){throw new IllegalStateException(e);}}
 static Object get(Object o,String n){try{return field(o.getClass(),n).get(o);}catch(Exception e){throw new IllegalStateException(e);}}
 static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{var m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);try{return m.invoke(o,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception x)throw x;throw e;}}
 static final Map<GameStateStore,Boolean> enabled=Collections.synchronizedMap(new WeakHashMap<>());
 public static void enable(ClientSession session){
  try{if(SharedDatabase.enabled((ServerConfig)get(session,"config")))enabled.put((GameStateStore)get(session,"state"),true);}
  catch(Exception e){throw new IllegalStateException("Cannot enable live results",e);}
 }
 public static void beforeSnapshot(GameStateStore state){
  if(!enabled.containsKey(state))return;
  try{
   boolean dirty=false;
   for(Object p:((Map<?,?>)get(state,"profiles")).values()){
    for(String n:List.of("wins","losses","rankingPoints","classicServerName","channel","sourceLevel","race","monthlyWins","monthlyLosses","monthlyRankingPoints","monthlyRankingPeriod")){
     String snapshot="snapshot"+Character.toUpperCase(n.charAt(0))+n.substring(1);
     if(!Objects.equals(get(p,n),get(p,snapshot))){dirty=true;break;}
    }
    if(dirty)break;
   }
   if(dirty)for(String n:List.of("rankingSnapshotBoundaryEpochSeconds","monthlyRankingSnapshotBoundaryEpochSeconds")){
    Field f=field(GameStateStore.class,n);long old=f.getLong(state);if(old>0)f.setLong(state,old-1);
   }
  }catch(Exception e){throw new IllegalStateException("Cannot refresh live rankings",e);}
 }
 public static boolean baselineEquals(Object cached,Object expected,ClientSession session){
  if(enabled.containsKey((GameStateStore)get(session,"state"))){
   var a=(GameStateStore.TournamentProfileState)cached;var b=(GameStateStore.TournamentProfileState)expected;
   return a.wins()==b.wins() && a.losses()==b.losses() && a.classicServerName().equals(b.classicServerName());
  }
  return cached.equals(expected);
 }
 public static void persistRankingRoster(ClientSession session)throws IOException{
  if(enabled.containsKey((GameStateStore)get(session,"state")))return; // Ranking reads must not replay stale per-session SQL caches.
  try{call(session,"persistSqlTournamentRoster",new Class<?>[0]);}catch(Exception e){throw new IOException(e);}
 }
 static String hex(String s){return HexFormat.of().formatHex(s.getBytes(Charset.forName("MS950")));}
 public static byte[] packet(String source,String raw,String display,GameStateStore.TournamentStatistics s,Protocol.LitoCharacterRankings r){
  String text="LITOSTAT1:"+hex(source)+":"+hex(raw)+":"+hex(display)+":"+s.winRateDenominator()+","+s.wins()+","+s.losses()+","+s.disconnects()+","+r.overall()+","+r.classicServer()+","+r.channel()+","+r.level()+","+r.race()+","+r.overallMovement()+","+r.classicServerMovement()+","+r.channelMovement()+","+r.levelMovement()+","+r.raceMovement();
  return new Protocol.Writer().c(0x0f).c(254).s(text).bytes();
 }
 public static void lobby(ClientSession session)throws IOException{
  enable(session);if(!enabled.containsKey((GameStateStore)get(session,"state")))return;
  if(!get(session,"phase").toString().equals("CHANNEL"))return;
  String raw=(String)get(session,"selectedCharacter");if(raw.isBlank())return;
  try{
   GameStateStore state=(GameStateStore)get(session,"state");String identity=(String)call(session,"stateCharacter",new Class<?>[0]);
   var stats=state.tournamentStatistics(identity);var ranks=state.characterRankings(identity);
   String display=(String)call(session,"litoDisplayName",new Class<?>[0]);
   call(session,"sendEncrypted",new Class<?>[]{byte[].class,String.class},packet((String)get(session,"selectedClassicServerName"),raw,display,stats,ranks),
       "S_LITO_LIVE_STATS player="+raw+" battles="+stats.winRateDenominator()+" wins="+stats.wins()+" losses="+stats.losses());
  }catch(Exception e){throw new IOException("Cannot publish live lobby statistics",e);}
 }
}
