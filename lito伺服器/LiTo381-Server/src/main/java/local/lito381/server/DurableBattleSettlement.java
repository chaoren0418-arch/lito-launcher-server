package local.lito381.server;
import java.io.*;import java.lang.ref.WeakReference;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Freeze all outcomes before any result packet. Retry compact durable records, not player worlds. */
final class DurableBattleSettlement {
 static Object get(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}}
 static void set(Object o,String name,Object value){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}}
 static Object call(Object o,String name,Class<?>[] types,Object...args){try{var m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,args);}catch(InvocationTargetException e){throw new IllegalStateException(name,e.getCause());}catch(ReflectiveOperationException e){throw new IllegalStateException(name,e);}}
 static final class Ticket {final WeakReference<ClientSession> session;final Context context;volatile SettlementJournal.Entry entry;volatile GameStateStore.Progress progress;Ticket(ClientSession s,Context c){session=new WeakReference<>(s);context=c;}}
 static final class Context {final WeakReference<GameHub> hub;final long room,generation;final boolean testRoom;final String battle=UUID.randomUUID().toString();volatile BattleRules.Resolution terminal;volatile List<SettlementJournal.Entry> unsaved=List.of();boolean retryNotification;Context(GameHub h,long r,long g){hub=new WeakReference<>(h);room=r;generation=g;testRoom=NativeRoomRules.testRoom(h,r);}}
 private static final Map<ClientSession,Ticket> TICKETS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final Map<GameHub,Map<Long,Context>> CONTEXTS=Collections.synchronizedMap(new WeakHashMap<>());
 private static volatile SettlementJournal journal;private static volatile long lastError;
 private static final ScheduledExecutorService WORKER=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"lito-settlement-recovery");t.setDaemon(true);return t;});
 static void open(ServerConfig config)throws Exception{
  if(journal!=null)return;
  synchronized(DurableBattleSettlement.class){if(journal!=null)return;
   for(var route:List.copyOf(SharedDatabase.ROUTES.values()))SettlementSql.check(route);
   Path configBase=Path.of(SharedDatabase.properties(config.source()).getProperty("configuration.base.file",config.source().toAbsolutePath().toString()));
   Path path=configBase.getParent().getParent().resolve("runtime/battle-settlements");
   SettlementJournal opened=new SettlementJournal(path);
   try{opened.replay(SettlementSql::apply);SettlementSql.drainAll();journal=opened;}catch(Exception e){opened.close();throw e;}
   WORKER.scheduleWithFixedDelay(DurableBattleSettlement::retry,2,2,TimeUnit.SECONDS);
   Runtime.getRuntime().addShutdownHook(new Thread(()->{try{journal.close();}catch(IOException ignored){}},"settlement-journal-close"));
  }
 }
 static Context context(GameHub hub,long room){
  Object raw;long generation=0;synchronized(hub){raw=((Map<?,?>)get(hub,"rooms")).get(room);if(raw!=null)generation=(long)get(raw,"battleGeneration");}
  synchronized(CONTEXTS){var map=CONTEXTS.computeIfAbsent(hub,h->new HashMap<>());Context old=map.get(room);if(old!=null && (!old.unsaved.isEmpty()||old.generation==generation))return old;var c=new Context(hub,room,generation);map.put(room,c);return c;}
 }
 static void begin(ClientSession session){var hub=(GameHub)get(session,"hub");long room=(long)get(session,"currentRoomId");TICKETS.put(session,new Ticket(session,context(hub,room)));}
 private static Ticket ticket(ClientSession session){var t=TICKETS.get(session);if(t==null){begin(session);t=TICKETS.get(session);}return t;}
 static SharedDatabase.Route route(ClientSession session){Object repository=call(session,"selectedSourceAccounts",new Class[]{});return repository instanceof LegacyMySqlRepository legacy?SharedDatabase.ROUTES.get(legacy):null;}
 static BattleRules.Resolution prepare(ClientSession session,BattleRules.Resolution resolution)throws IOException{
  if(!get(session,"phase").toString().equals("BATTLE"))return resolution;
  GameHub hub=(GameHub)get(session,"hub");long room=(long)get(session,"currentRoomId");List<ClientSession> roster=hub.battleResultCombatants(room);
  Context c=ticket(session).context;BattleRules.Resolution frozen=freeze(c,roster,resolution,Set.of());
  for(var player:roster){long id=player.playerObjectId();if(frozen.winners().contains(id))player.serverRecordBattleResult(true);else if(frozen.losers().contains(id))player.serverRecordBattleResult(false);}
  return frozen;
 }
 static BattleRules.Resolution freeze(Context c,List<ClientSession> roster,BattleRules.Resolution proposed,Set<Long> disconnected)throws IOException{
  for(var player:roster)ticket(player);
  synchronized(c){
   BattleRules.Resolution effective=c.terminal!=null?c.terminal:proposed;if(effective.ended()&&c.terminal==null)c.terminal=effective;
   List<SettlementJournal.Entry> fresh=new ArrayList<>();
   for(var player:roster){if(c.testRoom||player.isObserver())continue;long id=player.playerObjectId();if(!effective.winners().contains(id)&&!effective.losers().contains(id))continue;
    Ticket t=ticket(player);if(t.entry!=null)continue;var route=route(player);if(route==null)continue;
    var ch=(CharacterSummary)call(player,"selectedCharacterSummary",new Class[]{});if(ch==null)throw new IOException("settlement character missing");boolean won=effective.winners().contains(id);
    t.entry=new SettlementJournal.Entry(SettlementJournal.Entry.key(c.battle,route.database,ch.objectId()),c.battle,route.database,route.name,route.controls,ch.objectId(),ch.name(),won,!won&&disconnected.contains(id));fresh.add(t.entry);
   }
   if(!fresh.isEmpty()){var all=new ArrayList<>(c.unsaved);all.addAll(fresh);c.unsaved=List.copyOf(all);}
   if(!c.unsaved.isEmpty()){
    c.retryNotification=effective.ended();
    if(journal==null)throw new IOException("durable settlement journal unavailable");
    journal.save(c.unsaved);c.unsaved=List.of();c.retryNotification=false;
   }
   return effective;
  }
 }
 static BattleRules.Resolution freezeDisconnect(GameHub hub,long room,List<ClientSession> roster,BattleRules.Resolution proposed,Set<Long> departed){
  Context c=roster.isEmpty()?context(hub,room):ticket(roster.get(0)).context;
  try{return freeze(c,roster,proposed,departed);}catch(IOException e){throw new IllegalStateException("cannot preserve disconnect settlement",e);}
 }
 static boolean cleanupAllowed(GameHub hub,long room){synchronized(CONTEXTS){var map=CONTEXTS.get(hub);Context c=map==null?null:map.get(room);return c==null||c.unsaved.isEmpty();}}
 static void requireDurable(GameHub hub,long room){if(!cleanupAllowed(hub,room))throw new IllegalStateException("room retained: settlement journal not durable");}
 static void forget(GameHub hub,long room){synchronized(CONTEXTS){var map=CONTEXTS.get(hub);if(map!=null&&cleanupAllowed(hub,room))map.remove(room);}}
 static GameStateStore.TournamentProfileState counters(GameStateStore.TournamentProfileState old,int wins,int losses,String source)throws IOException{
  try{var fields=old.getClass().getRecordComponents();Class<?>[] types=new Class<?>[fields.length];Object[] values=new Object[fields.length];for(int i=0;i<fields.length;i++){var f=fields[i];types[i]=f.getType();values[i]=switch(f.getName()){case "wins"->wins;case "losses"->losses;case "classicServerName"->source;default->f.getAccessor().invoke(old);};}return (GameStateStore.TournamentProfileState)old.getClass().getDeclaredConstructor(types).newInstance(values);}catch(ReflectiveOperationException e){throw new IOException(e);}
 }
 static boolean unranked(ClientSession s){Ticket t=TICKETS.get(s);return t!=null?t.context.testRoom:NativeRoomRules.testRoom((GameHub)get(s,"hub"),(long)get(s,"currentRoomId"));}
 static boolean won(ClientSession s,boolean fallback){Ticket t=TICKETS.get(s);return t!=null&&t.entry!=null?t.entry.won():fallback;}
 static boolean disconnected(ClientSession s,boolean fallback){Ticket t=TICKETS.get(s);return t!=null&&t.entry!=null?t.entry.disconnected():fallback;}
 static boolean matches(ClientSession s,SettlementJournal.Entry e){var ch=(CharacterSummary)call(s,"selectedCharacterSummary",new Class[]{});var source=route(s);return ch!=null&&source!=null&&source.database.equals(e.database())&&ch.objectId()==e.character()&&ch.name().equals(e.name());}
 static GameStateStore.Progress record(GameStateStore live,String character,int reward,int experience,ClientSession session)throws IOException{
  Ticket t=ticket(session);SettlementJournal.Entry e=t.entry;if(e==null)throw new IOException("result has no frozen settlement identity");
  if(!matches(session,e))throw new IOException("settlement belongs to a previous source/character; journal will complete it separately");
  synchronized(t){if(t.progress!=null)return t.progress;if(!t.context.unsaved.isEmpty())throw new IOException("settlement is not durable yet");
   try{
    SettlementSql.apply(List.of(e));var actual=SettlementSql.current(e);
    // Use a compact detached store after disconnect; never recreate a released player profile.
    boolean detached=get(session,"phase").toString().equals("CLOSED") || ((java.net.Socket)get(session,"socket")).isClosed();
    GameStateStore target=detached?new GameStateStore(Path.of("unused-settlement-memory"),false,true):live;
    synchronized(target){var old=target.tournamentProfileState(character);var baseline=counters(old,actual.state().wins()-(e.won()?1:0),actual.state().losses()-(e.won()?0:1),e.source());target.reconcileTournamentProfileStates(Map.of(character,baseline));
     t.progress=target.recordResult(character,e.won(),e.disconnected(),reward,experience,null);
     var updated=new RepositoryTournamentProfile(e.character(),e.name(),actual.snapshotVersion(),target.tournamentProfileState(character));
     ((Map<Long,RepositoryTournamentProfile>)get(session,"sqlTournamentProfiles")).put(e.character(),updated);var route=SettlementSql.route(e.database());synchronized(route){route.runtime.put(e.character(),updated);}
     if(detached)target.releaseSourceCharacter(character);
    }
    return t.progress;
   }catch(Exception failure){throw new IOException("durable settlement queued; result="+e.id(),failure);}
  }
 }
 static boolean hasProfile(GameStateStore state,String identity){Object key=get(call(state,"profileIdentity",new Class[]{String.class},identity),"storageKey");return ((Map<?,?>)get(state,"profiles")).containsKey(key);}
 static void reconcileRecovered(List<SettlementJournal.Entry> entries)throws Exception{
  List<GameHub> hubs;synchronized(CONTEXTS){hubs=List.copyOf(CONTEXTS.keySet());}
  for(var e:entries){var actual=SettlementSql.current(e);
   for(GameHub hub:hubs){List<ClientSession> sessions;synchronized(hub){sessions=List.copyOf((Set<ClientSession>)get(hub,"connectedSessions"));}
    for(ClientSession s:sessions)synchronized(s){
     if(((java.net.Socket)get(s,"socket")).isClosed())continue;var source=route(s);if(source==null||!source.database.equals(e.database()))continue;
     var selected=(CharacterSummary)call(s,"selectedCharacterSummary",new Class[]{});if(selected==null||selected.objectId()!=e.character()||!selected.name().equals(e.name()))continue;
     var state=(GameStateStore)get(s,"state");String identity=(String)call(s,"stateCharacter",new Class[]{});
     synchronized(state){if(!hasProfile(state,identity))continue;var old=state.tournamentProfileState(identity);
      if(old.wins()==actual.state().wins()&&old.losses()==actual.state().losses())continue;
      var updated=counters(old,actual.state().wins(),actual.state().losses(),source.name);state.reconcileTournamentProfileStates(Map.of(identity,updated));
      ((Map<Long,RepositoryTournamentProfile>)get(s,"sqlTournamentProfiles")).put(e.character(),new RepositoryTournamentProfile(e.character(),e.name(),actual.snapshotVersion(),updated));
     }
     try{LiveBattleStats.lobby(s);}catch(IOException ignored){/* The next lobby refresh resends the same authoritative totals. */}
    }
   }
  }
 }
 private static void retry(){
  try{
   List<Context> contexts=new ArrayList<>();synchronized(CONTEXTS){for(var map:CONTEXTS.values())contexts.addAll(map.values());}
   for(Context c:contexts){boolean notify=false;synchronized(c){if(!c.unsaved.isEmpty()){journal.save(c.unsaved);c.unsaved=List.of();notify=c.retryNotification;c.retryNotification=false;}}
    if(notify){GameHub hub=c.hub.get();if(hub!=null){for(ClientSession s:hub.battleResultCombatants(c.room))try{s.serverEndBattle(c.terminal);}catch(IOException ignored){}hub.endBattle(c.room);synchronized(hub){Object room=((Map<?,?>)get(hub,"rooms")).get(c.room);if(room!=null&&hub.roomMembers(c.room).isEmpty())call(hub,"resetOrRemove",new Class[]{room.getClass()},room);}}}
   }
   for(Context c:contexts){GameHub h=c.hub.get();if(h==null)continue;boolean absent;synchronized(h){absent=!((Map<?,?>)get(h,"rooms")).containsKey(c.room);}if(absent&&c.unsaved.isEmpty())forget(h,c.room);}
   journal.replay(entries->{SettlementSql.apply(entries);reconcileRecovered(entries);});SettlementSql.drainAll();
   List<Ticket> tickets;synchronized(TICKETS){tickets=List.copyOf(TICKETS.values());}
   for(Ticket t:tickets){ClientSession s=t.session.get();if(s!=null)synchronized(s){if(TICKETS.get(s)==t&&t.entry!=null&&t.progress==null&&matches(s,t.entry)&&cleanupAllowed((GameHub)get(s,"hub"),t.context.room)){s.serverRecordBattleResult(t.entry.won());if(!((java.net.Socket)get(s,"socket")).isClosed())LiveBattleStats.lobby(s);}}}
  }catch(Exception e){long now=System.nanoTime();if(now-lastError>30_000_000_000L){lastError=now;System.err.println("[LiTo] settlement recovery pending: "+e.getClass().getSimpleName()+" "+e.getMessage());}}
 }
}
