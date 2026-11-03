package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class RoomRulesRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Object get(Object o,String s)throws Exception{return NativeDirectoryRegression.get(o,s);}
 static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{return NativeDirectoryRegression.call(o,n,t,a);}
 static void ready(ClientSession s)throws Exception{call(s,"setReady",new Class[]{byte[].class},new byte[]{(byte)0xd2,1});}
 static boolean bot(List<byte[]> rows){return rows.stream().anyMatch(p->new String(p,StandardCharsets.ISO_8859_1).contains("ArenaBot"));}
 static void join(ClientSession s,long room)throws Exception{call(s,"joinRoom",new Class[]{byte[].class,String.class},new Protocol.Writer().c(0xcf).d(room).s("").bytes(),"rule-test");}
 static String phase(ClientSession s)throws Exception{return get(s,"phase").toString();}
 static void battle(ClientSession...players)throws Exception{long end=System.nanoTime()+12_000_000_000L;while(System.nanoTime()<end){boolean b=true;for(var p:players)b &= phase(p).equals("BATTLE");if(b)return;Thread.sleep(100);}throw new AssertionError("no battle");}
 static Object room(GameHub h,long id)throws Exception{return ((Map<?,?>)get(h,"rooms")).get(id);}
 @SuppressWarnings("unchecked") static List<ClientSession> players(Object room)throws Exception{return (List<ClientSession>)call(room,"combatants",new Class[]{});}
 public static void main(String[] args)throws Exception{
  var config=ServerConfig.load(Path.of(args[0]),"standard");check(NativeRoomRules.read(config).equals(new NativeRoomRules.Policy(true,true)),"live SQL policy unreadable");var root=Path.of(args[1]);Files.createDirectories(root);
  var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var a=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var b=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME)){
   NativeDirectoryRegression.init(a,"Owner",0,config.classicServerNames().get(0));NativeDirectoryRegression.init(b,"Peer",0,config.classicServerNames().get(1));
   var ac=new Protocol.CipherState(0x37110001);var bc=new Protocol.CipherState(0x37110001);
   long test=hub.roomSnapshots(0).get(0).id();
   call(b.session,"sendRoomList",new Class[]{});NativeDirectoryRegression.drain(b,bc);
   check(NativeDirectoryRegression.lists.get(b).get(test).users()==1,"empty test room must display bot");
   check(hub.roomCombatantCount(test)==0,"display count leaked to admission");
   join(a.session,test);
   check(bot(NativeDirectoryRegression.drain(a,ac)),"test room missing roster bot");NativeDirectoryRegression.drain(b,bc);
   check(NativeDirectoryRegression.lists.get(b).get(test).users()==2,"single human plus bot display");
   check(hub.roomCombatantCount(test)==1,"single human admission count changed");
   hub.setForce(test,a.session,1);check(!NativeRoomRules.rosterReady(hub,room(hub,test),players(room(hub,test))),"test human on bot team");hub.setForce(test,a.session,0);
   join(b.session,test);check(hub.roomCombatantCount(test)==2,"test second player");
   var snap=hub.roomSnapshots(0).stream().filter(x->x.id()==test).findFirst().orElseThrow();
   NativeRoomDirectoryRow.send(a.session,snap,"fixture-two-humans");
   NativeDirectoryRegression.drain(a,ac);NativeDirectoryRegression.drain(b,bc);
   check(NativeDirectoryRegression.lists.get(a).get(test).users()==2,"two humans must not display extra bot");
   call(b.session,"exitRoom",new Class[]{});check(bot(NativeDirectoryRegression.drain(a,ac)),"bot roster not restored after peer leave");NativeDirectoryRegression.drain(b,bc);
   ready(a.session);battle(a.session);check(hub.botHp(test)==100,"test world bot HP");check(bot(NativeDirectoryRegression.drain(a,ac)),"test world missing bot");
   check(DurableBattleSettlement.unranked(a.session),"test room ranked");
   var context=DurableBattleSettlement.context(hub,test);check(context.testRoom,"test marker not frozen");
   var result=hub.forceWinner(test,0);a.session.serverEndBattle(result);hub.endBattle(test);
   check(context.unsaved.isEmpty(),"test generated durable writes");check(!(boolean)get(a.session,"resultRecorded"),"test recorded result");
   call(a.session,"quitFight",new Class[]{});call(a.session,"exitRoom",new Class[]{});NativeDirectoryRegression.drain(a,ac);NativeDirectoryRegression.drain(b,bc);
   check(phase(a.session).equals("CHANNEL"),"test result return");check(hub.roomMembers(test).isEmpty(),"test roster not cleared");
   // A matching display name must not acquire the default room's exception.
   call(a.session,"createRoom",new Class[]{byte[].class},NativeDirectoryRegression.settings(0xcc,"Local Lobby"));long id=(long)get(a.session,"currentRoomId");
   check(id!=test&&!NativeRoomRules.testRoom(hub,id),"name spoofed test room");check(!bot(NativeDirectoryRegression.drain(a,ac)),"player room bot");NativeDirectoryRegression.drain(b,bc);
   ready(a.session);Thread.sleep(4100);check(phase(a.session).equals("ROOM"),"solo player started");var denied=NativeDirectoryRegression.drain(a,ac);check(denied.stream().anyMatch(p->p[0]==Protocol.systemMessage(9,"x")[0]),"no visible refusal");
   join(b.session,id);NativeDirectoryRegression.drain(a,ac);NativeDirectoryRegression.drain(b,bc);
   hub.setForce(id,a.session,0);hub.setForce(id,b.session,0);ready(b.session);
   check(!NativeRoomRules.rosterReady(hub,room(hub,id),players(room(hub,id))),"same team allowed");
   hub.setForce(id,b.session,1);hub.setReady(id,a.session,false);ready(a.session);
   var match=(BattleRules.MatchState)get(room(hub,id),"match");check(match.phase()==BattleRules.Phase.COUNTDOWN,"valid teams no countdown");
   hub.setForce(id,b.session,0);check(match.phase()==BattleRules.Phase.WAITING,"team change did not cancel");
   hub.setForce(id,b.session,1);hub.setReady(id,a.session,false);ready(a.session);battle(a.session,b.session);
   check(!DurableBattleSettlement.unranked(a.session),"old test ticket leaked to ordinary battle");check(hub.botHp(id)==0,"player room hidden bot");
   check(!bot(NativeDirectoryRegression.drain(a,ac))&&!bot(NativeDirectoryRegression.drain(b,bc)),"normal bootstrap bot");
   // No-winner cleanup avoids touching live SQL while exercising both result returns.
   result=hub.forceWinner(id,-1);result=new BattleRules.Resolution(true,false,Set.of(),Set.of(),"fixture-no-winner");
   a.session.serverEndBattle(result);b.session.serverEndBattle(result);hub.endBattle(id);
   call(a.session,"quitFight",new Class[]{});call(a.session,"exitRoom",new Class[]{});call(b.session,"quitFight",new Class[]{});call(b.session,"exitRoom",new Class[]{});
   check(room(hub,id)==null,"player room not removed");
   var policyField=NativeRoomRules.class.getDeclaredField("POLICIES");policyField.setAccessible(true);
   @SuppressWarnings("unchecked") var policies=(Map<GameHub,NativeRoomRules.Policy>)policyField.get(null);
   policies.put(hub,new NativeRoomRules.Policy(false,false));
   check(hub.roomSnapshots(1).isEmpty(),"disabled default room created");
   policies.put(hub,new NativeRoomRules.Policy(true,false));
   long disabledBot=hub.roomSnapshots(1).get(0).id();check(!NativeRoomRules.bot(BattleRules.Mode.DEATHMATCH,hub,disabledBot),"disabled bot active");
   call(b.session,"sendRoomList",new Class[]{});NativeDirectoryRegression.drain(b,bc);
   check(NativeDirectoryRegression.lists.get(b).get(test).users()==0,"disabled bot counted");
   check(!NativeRoomRules.bot(BattleRules.Mode.HONOR_MATCH,hub,test),"bot leaked into other mode");
   policies.put(hub,new NativeRoomRules.Policy(true,true));
   System.out.println("PASS test roster/world/HP, unranked settlement, cleanup, name spoof, solo refusal packet, teams, countdown cancellation, two-human start, no bot leakage");
  }
 }
}
