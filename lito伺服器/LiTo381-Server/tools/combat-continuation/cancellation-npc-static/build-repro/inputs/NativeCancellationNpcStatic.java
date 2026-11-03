package local.lito381.server;
import java.io.IOException;import java.util.*;

/** Scene-owned L1Quest appearance state. Source SQL remains a startup-only input. */
final class NativeCancellationNpcStatic {
 static final String P="local/lito381/server/",FIELD="nativeCancellationStaticAppearance";
 record Appearance(String name,int speed){}
 static final class State {final Map<Long,Appearance> actors=new HashMap<>();}
 private static State state(GameHub hub,boolean create){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("static NPC appearance requires hub ownership");
  State s=(State)CombatAccess.field(hub,FIELD);if(s==null&&create){s=new State();CombatAccess.set(hub,FIELD,s);}return s;
 }
 static Appearance appearance(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc){
  synchronized(hub){State s=state(hub,false);return s==null?new Appearance(npc.nameId(),npc.passispeed()):s.actors.getOrDefault(npc.objectId(),new Appearance(npc.nameId(),npc.passispeed()));}
 }
 static boolean restored(boolean ok,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc){
  if(ok)synchronized(hub){State s=state(hub,false);if(s!=null){s.actors.remove(npc.objectId());if(s.actors.isEmpty())CombatAccess.set(hub,FIELD,null);}}return ok;
 }
 static byte[] putObject(ClassicWorldNpcCatalog.StaticNpc npc,int heading,int gfx,ClientSession receiver){
  var hub=(GameHub)CombatAccess.field(receiver,"hub");var a=appearance(hub,npc);
  var projection=new ClassicWorldNpcCatalog.StaticNpc(npc.mapId(),npc.spawnId(),npc.objectId(),npc.npcId(),npc.x(),npc.y(),npc.heading(),npc.movementDistance(),a.name(),npc.implementation(),npc.gfxId(),npc.level(),npc.hp(),npc.mp(),npc.experience(),npc.lawful(),npc.lightSize(),a.speed(),npc.atkspeed(),npc.changeHead(),npc.normalAction(),npc.chaoticAction());
  return Protocol.classicStaticNpc(projection,heading,gfx);
 }
 static final class Target implements NativeCancellationNpcTransform.Target {
  final GameHub hub;final ClassicWorldNpcCatalog.StaticNpc npc;final List<NativeCancellationNpcTransform.Notice> notices=new ArrayList<>();
  Target(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc){this.hub=hub;this.npc=npc;}
  public long getId(){return npc.objectId();}public int getTempCharGfx(){return hub.classicWorldNpcGfx(npc);}public int getGfxId(){return npc.gfxId();}
  public int getX(){return npc.x();}public int getY(){return npc.y();}public int getMapId(){return npc.mapId();}public int getHeading(){return hub.classicWorldNpcHeading(npc);}
  public void setTempCharGfx(int gfx){if(!hub.transformClassicWorldNpcGfx(npc,npc.gfxId(),gfx))throw new IllegalStateException("static NPC appearance changed under owned hub");}
  public void setPassispeed(int speed){var a=appearance(hub,npc);state(hub,true).actors.put(npc.objectId(),new Appearance(a.name(),speed));}
  public void setNameId(String name){var a=appearance(hub,npc);state(hub,true).actors.put(npc.objectId(),new Appearance(name,a.speed()));}
  public void broadcastPacketAll(NativeCancellationNpcTransform.Notice notice){notices.add(notice);}
  public void deleteMe(){throw new IllegalStateException("static NPC replacement is not handled by the appearance adapter");}
 }
 static boolean route(ClientSession pc,byte[] packet,BattleSpellCatalog.CancellationSpell spell)throws IOException {
  if(!CombatRules.loaded())return false;
  var request=Protocol.objectSpellRequest(packet);int map=(int)CombatAccess.field(pc,"classicWorldMapId");
  var npc=ClassicWorldNpcCatalog.npc(map,request.targetObjectId());if(npc==null||!npc.implementation().equals("L1Quest"))return false;
  var rule=NativeCancellationNpcRules.lookup(npc.npcId());if(rule!=null&&rule.type()!=1)return false;
  var binding=NativePlayerCommit.Binding.capture(pc);var hub=(GameHub)CombatAccess.field(pc,"hub");
  NativeLightAction.commitPair(pc,pc,()->NativeWorldMaterialPolicy.run(pc,true,44,()->{synchronized(hub){
   if(binding.matches(pc)&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED))cast(pc,hub,npc,request,spell,rule);
  }}));return true;
 }
 private static void cast(ClientSession pc,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,Protocol.ObjectSpellRequest request,BattleSpellCatalog.CancellationSpell spell,NativeCancellationNpcRules.Rule rule)throws IOException {
  if(!String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")||(int)CombatAccess.field(pc,"classicWorldMapId")!=npc.mapId()||request.skillId()!=44||spell.skillId()!=44)return;
  int x=(int)CombatAccess.field(pc,"positionX"),y=(int)CombatAccess.field(pc,"positionY");
  if(Math.max(Math.abs(x-npc.x()),Math.abs(y-npc.y()))>spell.maximumRange()||!BattleSpellCatalog.isInNativeScreen(x,y,npc.x(),npc.y())||!BattleAnimationCatalog.get().supportsTimedAction(CombatAccess.integerCall(pc,"battleGfx","()I"),spell.actionId()))return;
  long now=System.nanoTime();var timed=(BattleTimedStatus)CombatAccess.field(pc,"timedStatus");var form=((BattlePolymorphStatus)CombatAccess.field(pc,"polymorphStatus")).active(now);
  if((form!=null&&!form.skillUsable())||timed.silenceActive(now)){send(pc,Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK),"S_PARALYSE static-Cancellation-rejected");send(pc,Protocol.messageCode(285),"S_MESSAGE_CODE static-Cancellation-rejected");return;}
  if(CombatAccess.integerCall(pc,"currentWeight240","(J)I",now)>=BattleInventoryWeight.BUFFED_REGEN_LIMIT){send(pc,Protocol.messageCode(316),"S_MESSAGE_CODE static-Cancellation-overweight");return;}
  if(!((Set<?>)CombatAccess.field(pc,"learnedSkills")).contains(44)||(boolean)CombatAccess.field(pc,"observer")||(int)CombatAccess.field(pc,"playerHp")<=0||CombatAccess.field(pc,"pendingPortal")!=null||hub.hasPrivateShop(pc))return;
  var summary=(CharacterSummary)CombatAccess.call(pc,"selectedCharacterSummary","()L"+P+"CharacterSummary;");var equipment=(SourceEquipmentMagicStats)CombatAccess.call(pc,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;");
  int intelligence=equipment.effectiveIntelligence(CombatAccess.integerCall(pc,"baseIntelligence","()I"));int reduction=BattleSpellCatalog.originalMagicConsumeReduction(summary==null?0:summary.type(),summary==null?0:Math.max(0,summary.originalIntelligence()));
  int cost=NativeSkillConsumptionBridges.CancellationSpell_effectiveMpCost_III(spell,intelligence,reduction);
  if(now<((Map<Integer,Long>)CombatAccess.field(pc,"spellReadyNanos")).getOrDefault(44,0L)||(int)CombatAccess.field(pc,"playerMp")<cost)return;
  if(rule!=null&&(rule.targetId()>0xFFFF||rule.targetId()<=0))return;
  if(!NativeLightAction.admitPair(pc,pc,44,spell.actionId(),spell.reuseDelayMillis()))return;
  var target=new Target(hub,npc);if(rule!=null)NativeCancellationNpcTransform.apply(target,rule,(id,sx,sy,sm,sh,duration)->null);
  CombatAccess.call(pc,"cancelMeditation","(Ljava/lang/String;)V","spell-44");
  CombatAccess.set(pc,"playerMp",NativeSkillConsumption.boundMp(pc,(int)CombatAccess.field(pc,"playerMp")-cost));
  CombatAccess.call(pc,"commitSpellReady","(IJ)V",44,System.nanoTime()+spell.reuseDelayMillis()*1_000_000L);
  CombatAccess.call(pc,"cancelAbsoluteBarrier","(Ljava/lang/String;)V","spell-44");
  var members=(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",npc.mapId());
  for(var member:members){
   if(!(boolean)CombatAccess.call(pc,"sameClassicWorldMap","(L"+P+"ClientSession;)Z",member)||!BattleSpellCatalog.isInNativeScreen((int)CombatAccess.field(member,"positionX"),(int)CombatAccess.field(member,"positionY"),npc.x(),npc.y()))continue;
   if(!(boolean)CombatAccess.field(pc,"invisible")||member==pc)sendViewer(member,Protocol.objectAction(pc.playerObjectId(),spell.actionId()),"S_ACTION static-Cancellation");
   sendNpc(member,hub,npc,Protocol.effectObject(npc.objectId(),spell.effectGfx()),"S_EFFECT static-Cancellation");
   for(var notice:target.notices){
    if(notice instanceof NativeCancellationNpcTransform.Name n)sendNpc(member,hub,npc,Protocol.changeDescription(n.objectId(),n.name()),"S_CHANGE_NAME static-Cancellation");
    else if(notice instanceof NativeCancellationNpcTransform.Shape n)sendNpc(member,hub,npc,Protocol.classicStaticNpcShape(n.objectId(),n.gfx(),0),"S_POLYMORPH static-Cancellation");
   }
  }
  CombatAccess.call(pc,"sendInventoryDelta","(L"+P+"RuntimeInventory;L"+P+"RuntimeInventory;)V",NativeCastMaterial.previous(pc),NativeCastMaterial.next(pc));
  send(pc,Protocol.manaPoint((int)CombatAccess.field(pc,"playerMp"),(int)CombatAccess.field(pc,"playerMaxMp")),"S_MANA_POINT static-Cancellation");
 }
 private static void sendViewer(ClientSession receiver,byte[] packet,String note){
  var binding=NativePlayerCommit.Binding.capture(receiver);byte[] frozen=packet.clone();
  NativeLightAction.deferScoped(()->{if(binding.matches(receiver))receiver.nativeSelfOriginalSend(frozen,note);});
 }
 private static void sendNpc(ClientSession receiver,GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,byte[] packet,String note){
  var binding=NativePlayerCommit.Binding.capture(receiver);int gfx=hub.classicWorldNpcGfx(npc);var expected=appearance(hub,npc);byte[] frozen=packet.clone();
  NativeLightAction.deferScoped(()->{
   synchronized(hub){if(!binding.matches(receiver)||ClassicWorldNpcCatalog.npc(npc.mapId(),npc.objectId())!=npc||hub.classicWorldNpcGfx(npc)!=gfx||!appearance(hub,npc).equals(expected))return;}
   receiver.nativeSelfOriginalSend(frozen,note);
  });
 }
 private static void send(ClientSession pc,byte[] packet,String note)throws IOException{NativeCancellationProbability.send(pc,packet,note);}
 private NativeCancellationNpcStatic(){}
}
