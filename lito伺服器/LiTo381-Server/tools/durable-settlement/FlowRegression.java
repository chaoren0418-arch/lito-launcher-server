package local.lito381.server;
import java.nio.file.*;import java.util.*;
public class FlowRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Object field(Object o,String s)throws Exception{return NativeDirectoryRegression.get(o,s);}
 static void put(Object o,String n,Object v)throws Exception{NativeDirectoryRegression.set(o,n,v);}
 static Object room(GameHub h,long id)throws Exception{return ((Map<?,?>)field(h,"rooms")).get(id);}
 static long setup(GameHub h,ClientSession... players)throws Exception{
  long id=h.createRoom(0,new GameHub.RoomSettings("Settlement","",8,1,0,0,0,0));Object room=room(h,id);var forces=(Map<ClientSession,Integer>)field(room,"forces");var battleForces=(Map<ClientSession,Integer>)field(room,"battleForces");
  for(int i=0;i<players.length;i++){var s=players[i];check(h.tryJoinRoom(id,s).joined(),"join");put(s,"currentRoomId",id);forces.put(s,i%2);battleForces.put(s,i%2);Object phase=field(s,"phase");put(s,"phase",Enum.valueOf((Class)phase.getClass(),"BATTLE"));put(s,"resultRecorded",false);}
  ((Collection<ClientSession>)field(room,"battleRoster")).addAll(List.of(players));put(room,"battleStarted",true);put(room,"battleGeneration",1L);
  var ids=new ArrayList<Long>();var assignments=new HashMap<Long,Integer>();for(int i=0;i<players.length;i++){ids.add(players[i].playerObjectId());assignments.put(players[i].playerObjectId(),i%2);}
  ((BattleRules.MatchState)field(room,"match")).begin(ids,assignments,players[0].playerObjectId(),System.nanoTime());for(var s:players)DurableBattleSettlement.begin(s);return id;
 }
 static void disconnect(GameHub h,long id,ClientSession s)throws Exception{check(h.registerBattleDisconnect(id,s),"register disconnect");((Set<?>)field(room(h,id),"members")).remove(s);}
 static void resolve(GameHub h,long id)throws Exception{NativeDirectoryRegression.call(h,"resolveBattleDisconnectSafely",new Class[]{long.class,long.class},id,1L);}
 static String key(ClientSession s)throws Exception{return (String)NativeDirectoryRegression.call(s,"stateCharacter",new Class[]{});}
 public static void main(String[] args)throws Exception{
  var config=ServerConfig.load(Path.of(args[0]),"standard");Path root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var h=new GameHub(config,state,accounts);
      var a=new SourceLoginRegression.Fixture(config,accounts,state,capture,h,0,ClientSession.Endpoint.GAME);
      var b=new SourceLoginRegression.Fixture(config,accounts,state,capture,h,1,ClientSession.Endpoint.GAME);
      var c=new SourceLoginRegression.Fixture(config,accounts,state,capture,h,0,ClientSession.Endpoint.GAME);
      var d=new SourceLoginRegression.Fixture(config,accounts,state,capture,h,1,ClientSession.Endpoint.GAME)){
   var fixtures=List.of(a,b,c,d);String[] names={"Owner","Peer","Waiting","Fourth"};for(int i=0;i<4;i++)NativeDirectoryRegression.init(fixtures.get(i),names[i],0,config.classicServerNames().get(i%2));
   long id=setup(h,a.session,b.session,c.session,d.session);
   disconnect(h,id,a.session);resolve(h,id);check((boolean)field(room(h,id),"battleStarted"),"one teammate disconnect ended whole match");check(state.tournamentProfileState(key(a.session)).losses()==1,"departed player loss missing");check(state.tournamentProfileState(key(b.session)).wins()==0,"opponent prematurely won");
   disconnect(h,id,c.session);resolve(h,id);check(!(boolean)field(room(h,id),"battleStarted"),"last teammate departure did not settle");check(state.tournamentProfileState(key(b.session)).wins()==1&&state.tournamentProfileState(key(d.session)).wins()==1,"winning force result missing");check(state.tournamentProfileState(key(a.session)).losses()==1,"first departure counted twice");
   // Packet send failure must occur AFTER both outcomes are recorded.
   h.leaveRoom(id,b.session);h.leaveRoom(id,d.session);long next=setup(h,a.session,b.session);a.server.close();
   var verdict=new BattleRules.Resolution(true,false,Set.of(a.session.playerObjectId()),Set.of(b.session.playerObjectId()),"fixture-normal-end");
   try{a.session.serverEndBattle(verdict);throw new AssertionError("closed socket ignored");}catch(java.io.IOException expected){}
   check(state.tournamentProfileState(key(a.session)).wins()==1,"winner lost on closed socket");check(state.tournamentProfileState(key(b.session)).losses()==1,"other result not saved before first packet");
   a.session.serverRecordBattleDisconnect();check(state.tournamentProfileState(key(a.session)).losses()==1,"late disconnect changed frozen win");
   h.endBattle(next);h.leaveRoom(next,a.session);h.leaveRoom(next,b.session);
   long invalid=setup(h,c.session,d.session);int cw=state.tournamentProfileState(key(c.session)).wins(),cl=state.tournamentProfileState(key(c.session)).losses(),dw=state.tournamentProfileState(key(d.session)).wins(),dl=state.tournamentProfileState(key(d.session)).losses();
   disconnect(h,invalid,c.session);disconnect(h,invalid,d.session);resolve(h,invalid);
   check(state.tournamentProfileState(key(c.session)).wins()==cw&&state.tournamentProfileState(key(c.session)).losses()==cl&&state.tournamentProfileState(key(d.session)).wins()==dw&&state.tournamentProfileState(key(d.session)).losses()==dl,"both disconnected match counted");
   check(!GameHub.resolveBattleDisconnect(Set.of(99L),Set.of(99L)).winners().contains(2L),"phantom bot winner returned");
   System.out.println("PASS: 2v2 teammate disconnect continues; last opposing teammate leaves -> surviving team wins; earlier forfeit exactly once; closed result socket cannot prevent either save; late disconnect cannot overturn recorded result; simultaneous total disconnect invalid; no phantom bot");
  }
 }
}
