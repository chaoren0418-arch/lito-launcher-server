package local.lito381.server;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unchecked")
public final class NativeWorldWireIdLifecycleMain extends NativeWorldEqualObjectIdWireMain {
 NativeWorldWireIdLifecycleMain(Path root,Path work){super(root,work);}
 @Override void seed(ModernSettlementMysqlFixture mysql,Path profile)throws Exception{
  super.seed(mysql,profile);
  try(var routes=AccountRepository.open(ServerConfig.load(profile))){require(routes.createAccount("WireMate",PASSWORD)==AccountMutationResult.SUCCESS,"own legal third account");}
  sql(mysql,B,"INSERT INTO characters(account_name,char_name,objid,level,HighLevel,MaxHp,CurHp,MaxMp,CurMp,Ac,Str,Con,Dex,Cha,Intel,Wis,Class,Sex,Type,Food,OriginalStr,OriginalCon,OriginalDex,OriginalCha,OriginalInt,OriginalWis) VALUES('WireMate','MateB',10002,52,52,200,200,100,100,10,18,18,13,12,8,9,61,0,1,100,18,18,13,12,8,9)");
  sql(mysql,B,"INSERT INTO character_other(char_obj_id) VALUES(10002)");
  sql(mysql,B,"UPDATE characters SET ClanID=70001,Clanname='OwnedRunClanB',ClanRank=2 WHERE objid=10002");
  sql(mysql,B,"INSERT INTO lito_character_origin(char_obj_id,account_name,char_name,created_at) VALUES(10002,'WireMate','MateB',NOW())");
 }
 long wire(Map<String,Object> row){return ((Number)((Map<String,Object>)row.get("self")).get("id")).longValue();}
 void direction(Peer p,long id)throws Exception{p.send(new Protocol.Writer().c(Protocol.C_CHANGE_DIRECTION).c(0).bytes());require(Arrays.equals(p.await(Protocol.S_CHANGE_DIRECTION),new Protocol.Writer().c(Protocol.S_CHANGE_DIRECTION).d(id).c(0).bytes()),"normal C09 uses stable observed wire ID");sync(p);}
 byte[] move(Peer p,Peer witness,long id,int x,int y)throws Exception{sync(witness);p.send(new Protocol.Writer().c(Protocol.C_MOVE).h(x).h(y).c(0).bytes());byte[] result=witness.await(Protocol.S_MOVE_OBJECT);require(Arrays.equals(result,new Protocol.Writer().c(Protocol.S_MOVE_OBJECT).d(id).h(x).h(y-1).c(0).bytes()),"normal C0A witnessed with assigned wire ID");sync(p);return result;}
 @Override void portalFlow(ModernSettlementMysqlFixture mysql)throws Exception{
  start();snapshot(mysql,"before-T25-lifecycle-authentication");
  try(Peer old=select("LifecycleOldA",port,"WireSend",SUN,"SenderA");Peer b=select("LifecycleMappedB",port,"WireRecv",MOON,"RecvB")){
   beginBattle(old,b,"LifecycleInitialLegalEntry");
   long oldId=wire(portalObserved(old,"WireSend(WireSun)",33051,32337,true,"oldA-legal131-entry"));
   long bid=wire(portalObserved(b,"WireRecv(WireMoon)",33051,32337,true,"mappedB-legal131-entry"));require(oldId==10001&&bid==0x40000000L,"initial physical10001 collision mapped only B");
   portalObserved(b,"WireRecv(WireMoon)",33055,32333,false,"mappedB-holds-distinct-WORLD-point");direction(old,oldId);evidence.put("old_A_move_seen_B",HexFormat.of().formatHex(move(old,b,oldId,33051,32337)));snapshot(mysql,"both-WORLD-before-oldA-QUIT");
   old.close();byte[] removal=b.await(Protocol.S_REMOVE_OBJECT);require(Arrays.equals(removal,Protocol.removeObject(oldId)),"old A normalQUIT removes only old wire ID");int oldRemovalAt=b.received.size()-1;evidence.put("old_A_remove_hex",HexFormat.of().formatHex(removal));evidence.put("old_A_remove_B_received_index",oldRemovalAt);waitPosition(mysql,A,33051,32336,0);snapshot(mysql,"oldA-QUIT-only-sourceA-row-saved");sync(b);int rejoinFrom=b.received.size();
   try(Peer again=select("LifecycleNewA",port,"WireSend",SUN,"SenderA");Peer mate=select("LifecycleEntryMateB",port,"WireMate",MOON,"MateB")){
    require(again.received.stream().noneMatch(x->{int op=x[0]&255;return op==Protocol.S_WORLD||op==Protocol.S_PORTAL||op==Protocol.S_PUT_OBJECT;}),"new LOGIN/GAME/C9 remains lobby; saved SQL is not automatic WORLD resume");snapshot(mysql,"newA-C9-lobby-while-mappedB-still-WORLD");
    beginBattle(again,mate,"LifecycleReentryLegalBattle");long newId=wire(portalObserved(again,"WireSend(WireSun)",33051,32337,true,"newA-legal-BattleSTART-131-WORLD-reentry"));require(newId==oldId&&newId!=bid,"rejoining A reuses released preferred ID without colliding mapped B");sync(b);int newActorAt=-1;for(int i=rejoinFrom;i<b.received.size();i++){byte[] x=b.received.get(i);if((x[0]&255)==Protocol.S_PUT_OBJECT&&actor(x).id()==newId&&actor(x).name().equals("WireSend(WireSun)"))newActorAt=i;}require(newActorAt>oldRemovalAt,"old removal ordered before newly joined actor");evidence.put("new_A_actor_B_received_index",newActorAt);mate.close();
    sync(b);int reportalFrom=b.received.size();for(int i=newActorAt+1;i<reportalFrom;i++)require(!Arrays.equals(b.received.get(i),Protocol.removeObject(newId)),"no late old removal after reentry before new session normal teleport");
    portalObserved(again,"WireSend(WireSun)",33055,32332,false,"rejoinedA-normal-WORLD-portal");sync(b);int reportalRemove=-1,reportalActor=-1,removeCount=0;for(int i=reportalFrom;i<b.received.size();i++){byte[] x=b.received.get(i);if(Arrays.equals(x,Protocol.removeObject(newId))){reportalRemove=i;removeCount++;}if((x[0]&255)==Protocol.S_PUT_OBJECT&&actor(x).id()==newId&&actor(x).x()==33055&&actor(x).y()==32332)reportalActor=i;}require(removeCount==1&&reportalFrom<=reportalRemove&&reportalRemove<reportalActor,"new session normal C2E/C08 intentionally removes then reannounces its current actor");evidence.put("new_A_reportal_begin_B_received_index",reportalFrom);evidence.put("new_A_normal_reportal_remove_B_received_index",reportalRemove);evidence.put("new_A_normal_reportal_actor_B_received_index",reportalActor);
    direction(again,newId);evidence.put("new_A_move_seen_B",HexFormat.of().formatHex(move(again,b,newId,33055,32332)));direction(b,bid);evidence.put("mapped_B_move_seen_new_A",HexFormat.of().formatHex(move(b,again,bid,33055,32333)));sync(b);sync(again);
    int beforeNewQuit=b.received.size();for(int i=reportalActor+1;i<beforeNewQuit;i++)require(!Arrays.equals(b.received.get(i),Protocol.removeObject(newId)),"no late old removal after intentional current-session reannouncement and two normal movements");evidence.put("new_A_B_observation_end_before_new_QUIT",beforeNewQuit);evidence.put("mapped_B_wire_ID_retained",bid);evidence.put("new_A_wire_ID",newId);snapshot(mysql,"rejoinedA-and-retainedB-operate-no-late-old-removal");
    again.close();byte[] newRemoval=b.await(Protocol.S_REMOVE_OBJECT);require(Arrays.equals(newRemoval,Protocol.removeObject(newId)),"actual new A QUIT removal after completed live operation segment");evidence.put("new_A_normal_QUIT_remove_hex",HexFormat.of().formatHex(newRemoval));waitPosition(mysql,A,33055,32331,0);snapshot(mysql,"newA-normal-QUIT-sourceA-saved-again");
   }
   b.close();waitPosition(mysql,B,33055,32332,0);snapshot(mysql,"mappedB-normal-QUIT-sourceB-physical10001-saved");
  }
  stop();snapshot(mysql,"after-owned-normal-stop");evidence.put("Java_generations",1);evidence.put("old_TTL_or_clan_matrix_executed",false);evidence.put("product_clock_or_private_phase_changed",false);evidence.put("actual_native_BIN_or_GUI_executed",false);save();
 }
 @Override void run()throws Exception{super.run();evidence.put("state","PASSED_T25_NORMAL_RELOGIN_C9_LEGAL_WORLD_REENTRY_STABLE_MAPPED_SURVIVOR_SQL");save();}
 public static void main(String[] args)throws Exception{new NativeWorldWireIdLifecycleMain(Path.of(args[0]),Path.of(args[1])).run();}
}
