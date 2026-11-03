package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.util.*;import java.sql.*;
import static local.lito381.server.LiToGmCommands.*;
public class GmRegression {
 static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
 static void phase(ClientSession s,String value)throws Exception{Class c=Class.forName("local.lito381.server.ClientSession$Phase");set(s,"phase",Enum.valueOf(c,value));}
 static List<byte[]> drain(Socket socket,Protocol.CipherState cipher)throws Exception{List<byte[]> out=new ArrayList<>();socket.setSoTimeout(80);try{while(true)out.add(cipher.decrypt(Protocol.readFrame(socket.getInputStream(),65535)));}catch(SocketTimeoutException done){}return out;}
 static void chat(ClientSession s,String value)throws Exception{call(s,"say",new Class[]{byte[].class,int.class},new Protocol.Writer().c(Protocol.C_SAY).c(0).s(value).bytes(),Protocol.C_SAY);}
 public static void main(String[] a)throws Exception{
  var config=ServerConfig.load(Path.of(a[0]));Path temp=Files.createTempDirectory("lito-gm-034-");var store=new GameStateStore(temp.resolve("state"),true);
  try(var accounts=AccountRepository.open(config);var hub=new GameHub(config);var listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var client=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var socket=listener.accept()){
   var s=new ClientSession(socket,ClientSession.Endpoint.GAME,config,accounts,store,new CaptureLog(temp.resolve("capture")),hub);
   set(s,"selectedClassicServerName",config.classicServerNames().get(0));set(s,"selectedCharacter","GmRegression");set(s,"account","gm-regression-nonexistent");set(s,"outgoing",new Protocol.CipherState(123));var cipher=new Protocol.CipherState(123);
   set(s,"characters",List.of(new CharacterSummary("GmRegression","",1,0,52,500,100,10,13,10,10,9,8,11,8,11,1,0,0,0,0,0,0,0)));
   set(s,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());set(s,"sourceInventoryWeightStats",SourceInventoryWeightStats.none(1.0));phase(s,"CHANNEL");var repo=(AccountRepository)call(s,"selectedSourceAccounts",new Class[]{});
   chat(s,".item 40743 100");var denied=drain(client,cipher);check(denied.size()==1&&(denied.get(0)[0]&255)==Protocol.S_MESSAGE,"unauthorized gets only visible denial");check(((RuntimeInventory)get(s,"inventory")).items().isEmpty(),"unauthorized no mutation");
   String gm=null;try(var c=connection(repo);var q=c.createStatement();var rows=q.executeQuery("SELECT login FROM accounts WHERE access_level>=200 AND banned=0 LIMIT 1")){if(rows.next())gm=rows.getString(1);}check(gm!=null,"existing GM test account required; no privileges modified");set(s,"account",gm);
   chat(s,".item 40743 100");var packets=drain(client,cipher);var inv=(RuntimeInventory)get(s,"inventory");check(inv.items().size()==1&&inv.items().get(0).quantity()==100,"native chat creates full arrow stack");check(packets.size()>=2,"inventory publication plus visible result");
   chat(s,".item 40743 99");drain(client,cipher);inv=(RuntimeInventory)get(s,"inventory");check(inv.items().get(0).quantity()==199,"second stack merges exactly");
   chat(s,".item 88889 2 7");drain(client,cipher);inv=(RuntimeInventory)get(s,"inventory");check(inv.items().stream().filter(i->i.definition().catalogId()==88889&&i.enchantLevel()==7&&i.identified()).count()==2,"two separate enchanted identified rings");check(inv.sourceEquipmentOnly().items().size()==3,"GM items survive lobby shop cleanup");
   int before=inv.items().size();for(String bad:List.of(".item 88889 11",".item 88889 -1",".item 88889 1 128",".item 99999999 1",".item 40743 0",".item 40743 2147483648",".item 40743 1 0 extra")){chat(s,bad);check(!drain(client,cipher).isEmpty(),"visible failure "+bad);check(((RuntimeInventory)get(s,"inventory")).items().size()==before,"failure no mutation "+bad);}
   createItem(s,repo,"箭",1,0);drain(client,cipher);check(((RuntimeInventory)get(s,"inventory")).items().get(0).quantity()==200,"exact source name resolution");
   hub.connect(s);hub.enterChannel(0,s);long roomId=hub.createRoom(0,new GameHub.RoomSettings("GM regression","",4,GameHub.RoomSettings.EQUAL_SIDES,BattleRules.Mode.DEATHMATCH.wireId(),0,0,0,0,List.of()));check(hub.tryJoinRoom(roomId,s).joined(),"room join");set(s,"currentRoomId",roomId);phase(s,"BATTLE");set(s,"playerHp",500);set(s,"playerMp",100);
   Object room=((Map<?,?>)get(hub,"rooms")).get(roomId);set(room,"battleStarted",true);((Collection<ClientSession>)get(room,"battleRoster")).add(s);
   var match=(BattleRules.MatchState)get(room,"match");match.begin(List.of(s.playerObjectId()),Map.of(s.playerObjectId(),0),System.nanoTime());
   var arena=ArenaMapCatalog.forMap(hub.roomRules(roomId).mapId());var p=arena.spawn(0);set(s,"positionX",p.x());set(s,"positionY",p.y());
   chat(s,".spawn 45161 2 3");var spawnPackets=drain(client,cipher);var monsters=hub.visibleTimeAttackActors(roomId,s);check(monsters.size()==2,"two actual NPCs in ordinary battle");check(monsters.stream().allMatch(n->n.npcId()==45161&&n.gfx()==145&&n.hp()==120),"SQL NPC identity graphics and HP");
   check(hub.roomRules(roomId).mode()==BattleRules.Mode.DEATHMATCH,"original battle mode unchanged");
   set(room,"pveAiPublishAfterNanos",0L);check(!hub.monsterAiTicks(System.nanoTime()+3_000_000_000L).isEmpty(),"ordinary-mode NPC AI produces movement/attack ticks");
   long otherRoom=hub.createRoom(0,new GameHub.RoomSettings("GM other room","",4,GameHub.RoomSettings.EQUAL_SIDES,BattleRules.Mode.DEATHMATCH.wireId(),0,0,0,0,List.of()));check(hub.timeAttackActor(otherRoom,monsters.get(0).objectId())==null,"NPC does not leak across room IDs");
   String identity=(String)call(s,"stateCharacter",new Class[]{});var reloaded=new GameStateStore(temp.resolve("state"),true);check(reloaded.economy(identity).inventory().get(40743L)==200,"durable state reload has exact created stack");
   var first=monsters.get(0);check(hub.canAttackTimeAttackActor(roomId,s,first.objectId()),"ordinary attack target admission");
   check(hub.hasteTimeAttackActor(roomId,s,first.objectId(),System.nanoTime(),30),"NPC buff target admission");
   var damage=hub.damageTimeAttackActor(roomId,s,first.objectId(),20);check(damage!=null&&hub.timeAttackActor(roomId,first.objectId()).hp()==100,"NPC combat HP mutation");
   check(hub.healTimeAttackActor(roomId,s,first.objectId(),10)!=null&&hub.timeAttackActor(roomId,first.objectId()).hp()==110,"NPC heal admission and effect");
   hub.damageTimeAttackActor(roomId,s,first.objectId(),1000);var last=monsters.get(1);var killed=hub.damageTimeAttackActor(roomId,s,last.objectId(),1000);check(killed!=null&&!killed.hit().finalBossesCleared(),"last GM death does not complete match");check(match.phase()==BattleRules.Phase.RUNNING,"match continues after GM deaths");
   chat(s,".spawn 45161");drain(client,cipher);check(hub.visibleTimeAttackActors(roomId,s).size()==1,"spawn again after all die");
   int npcCount=hub.visibleTimeAttackActors(roomId,s).size();for(String bad:List.of(".spawn 45161 101",".spawn 45161 1 21",".spawn 45001",".spawn 99999999")){chat(s,bad);check(!drain(client,cipher).isEmpty(),"visible NPC refusal");check(hub.visibleTimeAttackActors(roomId,s).size()==npcCount,"NPC refusal no mutation");}
   var encounter=(BattleTimeAttackEncounter)get(room,"timeAttackEncounter");encounter.deactivate();check(!LiToGmNpcs.active(encounter)&&encounter.snapshots().isEmpty(),"end/reset clears GM scope and actors");
   check(LiToGmNpcs.combatMode(hub.roomRules(roomId))==BattleRules.Mode.DEATHMATCH,"mode access returns original after clear");
   System.out.println("PASS: real SQL command policy and templates; native encrypted chat authorization/errors; exact stacks/name/enchant; durable provenance; ordinary-room NPC visibility/target/buff/heal/damage/death/reset; no match completion. Test state="+temp);
  }
 }
}
