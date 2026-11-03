package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.util.*;import java.lang.reflect.*;
public class Regression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void set(Object o,String n,Object v)throws Exception{LiveBattleStats.field(o.getClass(),n).set(o,v);}
 public static void main(String[] a)throws Exception{
  ServerConfig config=ServerConfig.load(Path.of(a[0]));Path root=Path.of(a[1]);Files.createDirectories(root);
  var state=new GameStateStore(root.resolve("unused.state"),false,true);
  try(var accounts=new MemoryAccountRepository("qatest","testpass","",0,1.0,40,52);var hub=new GameHub(config);var listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var client=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var socket=listener.accept();var capture=new CaptureLog(root.resolve("capture"))){
   var s=new ClientSession(socket,ClientSession.Endpoint.GAME,config,accounts,state,capture,hub);String source=config.classicServerNames().get(1);
   set(s,"selectedClassicServerName",source);set(s,"selectedCharacter","LiveTester");set(s,"account","qatest");set(s,"outgoing",new Protocol.CipherState(123));
   Class phase=Class.forName("local.lito381.server.ClientSession$Phase");set(s,"phase",Enum.valueOf(phase,"CHANNEL"));
   set(s,"currentChannelId",0);LiveBattleStats.enable(s);String id=GameStateStore.sourceCharacter(source,"LiveTester"),other=GameStateStore.sourceCharacter(config.classicServerNames().get(0),"LiveTester");
   state.bindRankingIdentity(id,source,0,52,1);state.bindRankingIdentity(other,config.classicServerNames().get(0),0,52,1);
   var query=new GameStateStore.RankingQuery(source,0,52,1);
   check(state.ranking(Protocol.LitoRankingScope.OVERALL,query,100).isEmpty(),"20-match eligibility preserved");
   for(int i=0;i<20;i++)state.recordResult(id,true,250);
   var ranks=state.ranking(Protocol.LitoRankingScope.OVERALL,query,100);check(ranks.size()==1,"immediate eligibility after 20 results");
   check(ranks.get(0).wins()==20,"live overall wins");
   var cached=state.tournamentProfileState(id);state.recordResult(id,false,100);var expected=state.tournamentProfileState(id);
   ranks=state.ranking(Protocol.LitoRankingScope.OVERALL,query,100);check(ranks.get(0).losses()==1,"immediate loss update");
   check(state.ranking(Protocol.LitoRankingScope.MONTHLY,query,100).get(0).losses()==1,"live monthly loss");
   check(state.ranking(Protocol.LitoRankingScope.CLASSIC_SERVER,new GameStateStore.RankingQuery(config.classicServerNames().get(0),0,52,1),100).isEmpty(),"source scope cannot leak peer");
   check(LiveBattleStats.baselineEquals(expected,state.tournamentProfileState(id),s),"snapshot refresh not false SQL conflict");
   check(!LiveBattleStats.baselineEquals(cached,state.tournamentProfileState(id),s),"actual stale result still rejected");
   for(int i=0;i<21;i++)state.recordResult(other,true,500);
   ranks=state.ranking(Protocol.LitoRankingScope.OVERALL,query,100);check(ranks.size()==2&&ranks.get(0).wins()==21,"rank order changes immediately");
   check(state.characterRankings(id).overall()==2,"personal rank agrees");
   LiveBattleStats.call(s,"sendRoomList",new Class<?>[0]);
   client.setSoTimeout(2000);var cipher=new Protocol.CipherState(123);byte[] packet=cipher.decrypt(Protocol.readFrame(client.getInputStream(),65535));
   check((packet[0]&255)==15&&(packet[1]&255)==254,"live packet from actual sendRoomList hook");
   String text=new String(packet,2,packet.length-3,java.nio.charset.StandardCharsets.US_ASCII);check(text.startsWith("LITOSTAT1:")&&text.contains(":21,20,1,0,2,"),"qualified current results encoded");
   Files.write(root.resolve("live-packet.bin"),packet);
   LiveBattleStats.persistRankingRoster(s); // Memory repository cannot persist; shared ranking read must skip mutations.
   check(state.tournamentStatistics(id).wins()==20&&state.tournamentStatistics(other).wins()==21,"refresh never awards a result");
   System.out.println("PASS actual packet hook; live overall/monthly/source ranking; 20-match threshold; rank movement; source isolation; repeated result baseline; zero SQL");
  }
 }
}
