from pathlib import Path
R=Path(__file__).resolve().parent;P=R.parent
base=(P/'decay-material/DecayPotionLifecycleProbe.java').read_text('utf-8')
main=base[base.index(' public static void main('):].replace('mode<7','mode<8').replace('var jar=Path.of(args[3]).toRealPath();','before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();')
main=main.replace('"+ownChecks+",\\"actual_frames','"+ownChecks+",\\"world_heal_gaps\\":"+gaps+",\\"actual_frames')
head='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Original native item dispatch, expired Decay Potion and actual registered scene boundaries. */
public final class WorldPotionProbe extends DecayPotionMaterialProbe {
 static int gaps;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);selectedSkill=71;selectedMode=0;probability(71);var row=BattleSpellCatalog.decayPotionSpell(71);
  int potion=mode==6?40012:40010;NativeDeItemPublication.grant(peer,potion,3);var drink=peer.nativeDeInventory(0).items().stream().filter(i->i.definition().catalogId()==potion).findFirst().orElseThrow();
  need(InventoryCatalog.blockedByDecayPotion(drink.definition()),"actual potion eligibility");
  if(mode==0||mode==5)NativeDeItemPublication.grant(pc,Math.toIntExact(row.itemConsumeId()),row.itemConsumeCount()+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==0||mode==5){cast(pc,71,row);need(active(peer,71),"real targeted Decay Potion applied before drinking");}
   if(mode==2)set(peer,"observer",true);if(mode==3)set(peer,"playerHp",0);
   if(mode==4){if(world)set(peer,"classicWorldMapId",(int)get(peer,"classicWorldMapId")+1);else set(peer,"phase",Enum.valueOf((Class<Enum>)get(peer,"phase").getClass(),"LOBBY"));}
   int hp=(int)get(peer,"playerHp");long quantity=peer.nativeDeInventory(0).countCatalog(potion);
   var payload=new Protocol.Writer().c(Protocol.C_USE_ITEM).d(mode==7?0x7feeeeeeL:drink.objectId());if(drink.definition().useType()==10)payload.d(12345);else payload.s("");var packet=payload.bytes();
   call(peer,"handle",new Class[]{byte[].class},packet);
   if(mode==0){
    need((int)get(peer,"playerHp")==hp&&peer.nativeDeInventory(0).countCatalog(potion)==quantity,"active status denies healing and consumption");
    var status=(BattleTimedStatus)get(peer,"timedStatus");need(status.expireDecayPotion((long)get(status,"decayPotionExpiresAtNanos"))&&!active(peer,71),"status expires at actual deadline");call(peer,"handle",new Class[]{byte[].class},packet);
   }
   long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))!=0&&System.nanoTime()<limit)Thread.sleep(1);
   boolean valid=mode==0||mode==1||mode==6,healed=valid&&(!world||!before);
   need(((int)get(peer,"playerHp")>hp)==healed&&peer.nativeDeInventory(0).countCatalog(potion)==quantity-(healed?1:0),"native heal/quantity follows legitimate phase, status and live scene mode="+mode+" world="+world+" before="+before);
   if(valid&&world&&before)gaps++;
   collect(wire);var packets=collect(other);if(mode==0||mode==5)need(packets.contains(HexFormat.of().formatHex(Protocol.messageCode(698))),"real visible potion rejection698");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"outputs unlocked and item lifetime drained");
  }cases++;
 }
'''
(R/'WorldPotionProbe.java').write_text(head+main,encoding='utf-8')
