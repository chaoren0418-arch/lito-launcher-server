// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/area-sleep-slow/NativeAreaMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.locks.*;

/** Freeze membership before waiting; all player locks precede the hub/population. */
final class NativeAreaMaterialAction {
 static final String P="local/lito381/server/";
 static final ThreadLocal<Context> current=new ThreadLocal<>();
 private static final ThreadLocal<Object[]> massUnlock=new ThreadLocal<>();
 record Npc(int kind,long id,Object owner,Object actor,Object epoch){}
 @AsmFinal({"players","bindings","population","nativeSourceMagicSnapshot"})
 static final class Context {
  final ClientSession pc;final GameHub hub;final NativePlayerCommit.Binding caster;
  final String phase;final int map,instance;final long room;
  List<ClientSession> players;
  IdentityHashMap<ClientSession,NativePlayerCommit.Binding> bindings;
  final Map<Long,Npc> npcs;
  ClassicWorldMonsterPopulation population;

        int nativeWeaponBreakTarget;
        long nativeWeaponBreakTargetId;
        NativePlayerVitalCompletions.Receipt nativeWeaponBreakPeerReceipt;
        Object nativeCompanionTargetScope;
        boolean nativeCorpseTargetScope;
        Object nativeShockPlayerScope;
        Object nativeStaticRespawnTarget;
        Object nativeAcceptedFreezeBatch;
        volatile Object nativeBattleControlledCancellationScope;
        volatile Object nativeSourceMassSlowAim;
        volatile Object nativeSourceWindAim;
        volatile Object nativeSourceNegativeAim;
        volatile Object nativeSourceLightningAim;
        volatile Object nativeSourceWeaponBuffAim;
        volatile Object nativeSourceRemainingAim;
        volatile Object nativeSourceControlledAction;
        volatile Object nativeSourceRawAction;
        volatile Object nativeSourceArenaPoisonOutput;
        volatile Object nativeSourceDollNpcScope;
        volatile Object nativeSourceDollAction;
        volatile Object nativeSourceDollOutput;
        volatile Object nativeSourceDollDefenseSnapshot;
        volatile Object nativeSourceCubePlayerOutput;
        volatile Object nativeSourceCubeNpcOutput;
        volatile Object nativeSourcePoisonNpcOutput;
        volatile Object nativeSourcePhysicalNpcOutput;
        volatile Object nativeSourcePhysicalNpcCommit;
        Object nativeSourceMagicSnapshot;
        volatile Object nativeSourceMagicAdmission;
        volatile Object nativeSourceSingleNpcOutput;
        volatile Object nativeSourceSpecialNpc;
        volatile Object nativeSourceGroundProof;
        volatile Object nativeSourceCubeStatusProof;
        volatile Object nativeSourceHealingCast;
        volatile Object nativeSourceFirstPlayers;
        volatile transient Object nativeSourceTargetNegativeProof;
        volatile transient Object nativeSourceWeaponFirstLife;
        transient Object nativeSourceControlledNegativeFinal;

  Context(ClientSession pc){
   bindings=new IdentityHashMap<>();npcs=new HashMap<>();
   this.pc=pc;hub=(GameHub)CombatAccess.field(pc,"hub");caster=NativeSourcePlayerFactories.caster(pc,this);
   phase=String.valueOf(CombatAccess.field(pc,"phase"));map=(int)CombatAccess.field(pc,"classicWorldMapId");instance=(int)CombatAccess.field(pc,"classicWorldInstanceId");room=(long)CombatAccess.field(pc,"currentRoomId");
   synchronized(hub){
    var members=new ArrayList<>(phase.equals("WORLD")?NativeCompanionTargets.owners(hub.classicWorldMembers(map,instance),pc):hub.roomMembers(room));if(!members.contains(pc))members.add(pc);
    players=members.stream().distinct().sorted(Comparator.comparingInt(NativeSourcePlayerActionLock::signedId).thenComparingInt(System::identityHashCode)).toList();
    for(var member:players){bindings.put(member,NativeSourceDollAudience.capture(member,this));
     for(var snap:phase.equals("WORLD")?member.classicWorldCompanionSnapshots():member.controlledCompanionSnapshots()){
      var companion=(BattleCompanion)CombatAccess.call(member,"ownedCompanion","(J)L"+P+"BattleCompanion;",snap.objectId());
      if(companion!=null&&snap.active()&&snap.hp()>0)npcs.put(snap.objectId(),new Npc(3,snap.objectId(),member,companion,NativeCombatAttacker.companion(member,companion)));
     }
    }
    population=phase.equals("WORLD")?NativeCancellationNpcMaterialAction.population(hub,map,instance):null;
    if(population!=null)synchronized(population){for(var n:population.inScreen(map,(int)CombatAccess.field(pc,"positionX"),(int)CombatAccess.field(pc,"positionY"))){Object actor=NativeCancellationNpcMaterialAction.actor(population,n.objectId());npcs.put(n.objectId(),new Npc(0,n.objectId(),population,actor,NativeCancellationNpcMaterialAction.epoch(actor,true)));}}
    if(phase.equals("BATTLE")){
     for(var n:hub.visibleQuestMonsters(room,pc))captureBattle(n.objectId(),1);
     for(var n:hub.visibleTimeAttackActors(room,pc))captureBattle(n.objectId(),2);
    }
   }
              NativeCompanionTargets.capture(this);
            NativeSourceControlledDisplay.inherit(this);
            NativeSourceRawDamage.inherit(this);
            NativeSourceArenaPoison.inherit(this);
            NativeSourceDollAction.inherit(this);
            NativeSourceDollOutput.inherit(this);
            this.nativeSourceDollDefenseSnapshot = NativeSourceDollDefense.snapshot(this);
            NativeSourceCubePlayer.inherit(this);
            NativeSourceCubeNpc.inherit(this);
            NativeSourcePoisonNpc.inherit(this);
            NativeSourcePhysicalNpc.inherit(this);
            Context destination=this;Context snapshotOwner=this;
            NativeSourceFirstPlayers.seal(this);
            destination.nativeSourceMagicSnapshot = NativeSourceMagicNpc.snapshot(snapshotOwner);
            NativeSourceSingleNpc.inherit(this);
            NativeSourceCubeStatus.inherit(this);
            NativeSourceFirstPlayers.seal(this);
        }
  Object encounter(int kind){var roomObject=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(room);return roomObject==null?null:CombatAccess.field(roomObject,kind==1?"questEncounter":"timeAttackEncounter");}
  Object monster(Object owner,int kind,long id){return owner==null?null:CombatAccess.call(owner,"monster","(J)L"+P+(kind==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);}
  void captureBattle(long id,int kind){Object owner=encounter(kind),actor=monster(owner,kind,id);if(actor!=null)npcs.put(id,new Npc(kind,id,owner,actor,NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor,"damagePoison"),true)));}
  boolean player(ClientSession target){var binding=bindings.get(target);return binding!=null&&NativeSourceControlledRoleFrame.current(binding,target)&&String.valueOf(CombatAccess.field(target,"phase")).equals(phase)
   &&(phase.equals("WORLD")?(int)CombatAccess.field(target,"classicWorldMapId")==map&&(int)CombatAccess.field(target,"classicWorldInstanceId")==instance:(long)CombatAccess.field(target,"currentRoomId")==room);}
  boolean nativeCompanionOriginalNpc(long id){var n=npcs.get(id);if(n==null)return false;
   if(n.kind==3){var owner=(ClientSession)n.owner;return player(owner)&&CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",id)==n.actor&&NativeCombatAttacker.alive(owner,(NativeCombatAttacker.Ref)n.epoch);}
   if(n.kind==0){return NativeCancellationNpcMaterialAction.population(hub,map,instance)==population&&NativeCancellationNpcMaterialAction.actor(population,id)==n.actor&&NativeCancellationNpcMaterialAction.epoch(n.actor,false)==n.epoch&&(boolean)CombatAccess.field(n.actor,"alive")&&(boolean)CombatAccess.field(n.actor,"present");}
   return encounter(n.kind)==n.owner&&monster(n.owner,n.kind,id)==n.actor&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(n.actor,"damagePoison"),false)==n.epoch&&(int)CombatAccess.field(n.actor,"hp")>0;
  }
  boolean scene(){return NativeSourceControlledRoleFrame.current(caster,pc)&&player(pc)&&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED);}

    boolean npc(long l) {
            return NativeCompanionTargets.npc(this, l);
        }

    Context(Context context) {
            this.pc = context.pc;
            this.hub = context.hub;
            this.caster = context.caster;
            this.phase = context.phase;
            this.map = context.map;
            this.instance = context.instance;
            this.room = context.room;
            this.players = context.players;
            this.bindings = context.bindings;
            this.npcs = context.npcs;
            this.population = context.population;
            this.nativeWeaponBreakTarget = context.nativeWeaponBreakTarget;
            this.nativeWeaponBreakTargetId = context.nativeWeaponBreakTargetId;
            this.nativeWeaponBreakPeerReceipt = context.nativeWeaponBreakPeerReceipt;
            this.nativeCompanionTargetScope = context.nativeCompanionTargetScope;
            this.nativeCorpseTargetScope = context.nativeCorpseTargetScope;
            this.nativeShockPlayerScope = context.nativeShockPlayerScope;
            this.nativeStaticRespawnTarget = context.nativeStaticRespawnTarget;
            this.nativeAcceptedFreezeBatch = null;
            NativeSourceControlledDisplay.inherit(this, context);
            NativeSourceRawDamage.inherit(this, context);
            NativeSourceArenaPoison.inherit(this, context);
            NativeSourceDollAction.inherit(this, context);
            NativeSourceDollOutput.inherit(this, context);
            this.nativeSourceDollDefenseSnapshot = NativeSourceDollDefense.snapshot(this);
            NativeSourceCubePlayer.inherit(this, context);
            NativeSourceCubeNpc.inherit(this, context);
            NativeSourcePoisonNpc.inherit(this, context);
            NativeSourcePhysicalNpc.inherit(this, context);
            Context destination=this;Context snapshotOwner=this;
            NativeSourceFirstPlayers.inherit(this, context);
            destination.nativeSourceMagicSnapshot = NativeSourceMagicNpc.snapshot(snapshotOwner);
            NativeSourceSingleNpc.inherit(this, context);
            NativeSourceCubeStatus.inherit(this, context);
            NativeSourceFirstPlayers.inherit(this, context);
        }

    Context(ClientSession clientSession, NativeSourcePhysicalNpc.Commit commit) {
            this(clientSession);
            this.players = commit.members;
            this.bindings = new IdentityHashMap<ClientSession, NativePlayerCommit.Binding>(commit.viewers);
            NativeSourcePhysicalNpc.bind(this, commit);
            Context destination=this;Context snapshotOwner=this;
            NativeSourceFirstPlayers.seal(this);
            destination.nativeSourceMagicSnapshot = NativeSourceMagicNpc.snapshot(snapshotOwner);
            NativeSourceSingleNpc.inherit(this);
            NativeSourceCubeStatus.inherit(this);
        }

    Context(ClientSession clientSession, NativeSourcePhysicalNpc.Output output) {
            this.pc = clientSession;
            this.hub = output.commit().origin.scene().hub();
            this.caster = output.binding();
            this.phase = "BATTLE";
            this.room = output.binding().room();
            this.map = output.binding().map();
            this.instance = output.binding().instance();
            this.players = List.of(clientSession);
            this.bindings = new IdentityHashMap();
            this.bindings.put(clientSession, output.binding());
            this.npcs = new HashMap<Long, Npc>();
            Context context = current.get();
            NativeCompanionTargets.capture(this);
            NativeSourceControlledDisplay.inherit(this);
            NativeSourceRawDamage.inherit(this);
            NativeSourceArenaPoison.inherit(this);
            NativeSourceDollAction.inherit(this);
            NativeSourceDollOutput.inherit(this);
            if (NativeSourceDollCommit.enabled(clientSession) && context != null && context.pc == clientSession && context.nativeSourceDollDefenseSnapshot instanceof NativeSourceDollDefense.Snapshot) {
                this.nativeSourceDollDefenseSnapshot = context.nativeSourceDollDefenseSnapshot;
            }
            NativeSourceCubePlayer.inherit(this);
            NativeSourceCubeNpc.inherit(this);
            NativeSourcePoisonNpc.inherit(this);
            NativeSourcePhysicalNpc.inherit(this);
            if (context != null && context.pc == clientSession && context.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot) {
                this.nativeSourceMagicSnapshot = context.nativeSourceMagicSnapshot;
            }
            NativeSourceSingleNpc.inherit(this);
            NativeSourceCubeStatus.inherit(this);
            NativeSourceFirstPlayers.seal(this);
        }

    Context(ClientSession clientSession, NativePlayerCommit.Binding binding, GameHub gameHub) {
            this.pc = clientSession;
            this.hub = gameHub;
            this.caster = binding;
            this.phase = "BATTLE";
            this.room = binding.room();
            this.map = binding.map();
            this.instance = binding.instance();
            this.players = List.of(clientSession);
            this.bindings = new IdentityHashMap();
            this.bindings.put(clientSession, binding);
            this.npcs = new HashMap<Long, Npc>();
            Context context = current.get();
            NativeCompanionTargets.capture(this);
            NativeSourceControlledDisplay.inherit(this);
            NativeSourceRawDamage.inherit(this);
            NativeSourceArenaPoison.inherit(this);
            NativeSourceDollAction.inherit(this);
            NativeSourceDollOutput.inherit(this);
            if (NativeSourceDollCommit.enabled(clientSession) && context != null && context.pc == clientSession && context.nativeSourceDollDefenseSnapshot instanceof NativeSourceDollDefense.Snapshot) {
                this.nativeSourceDollDefenseSnapshot = context.nativeSourceDollDefenseSnapshot;
            }
            NativeSourceCubePlayer.inherit(this);
            NativeSourceCubeNpc.inherit(this);
            NativeSourcePoisonNpc.inherit(this);
            NativeSourcePhysicalNpc.inherit(this);
            if (context != null && context.pc == clientSession && context.nativeSourceMagicSnapshot instanceof NativeSourceMagicNpc.Snapshot) {
                this.nativeSourceMagicSnapshot = context.nativeSourceMagicSnapshot;
            }
            NativeSourceSingleNpc.inherit(this);
            NativeSourceCubeStatus.inherit(this);
            NativeSourceFirstPlayers.seal(this);
        }

    Context(ClientSession clientSession, NativePlayerCommit.Binding binding, GameHub gameHub, NativeSourceElementTransport.Inherited inherited) {
            this(clientSession, binding, gameHub);
            this.nativeCompanionTargetScope = inherited.nativeCompanionTargetScope();
            this.nativeSourceControlledAction = inherited.nativeSourceControlledAction();
            this.nativeSourceRawAction = inherited.nativeSourceRawAction();
            this.nativeSourceArenaPoisonOutput = inherited.nativeSourceArenaPoisonOutput();
            this.nativeSourceDollAction = inherited.nativeSourceDollAction();
            this.nativeSourceDollOutput = inherited.nativeSourceDollOutput();
            this.nativeSourceCubePlayerOutput = inherited.nativeSourceCubePlayerOutput();
            this.nativeSourceCubeNpcOutput = inherited.nativeSourceCubeNpcOutput();
            this.nativeSourcePoisonNpcOutput = inherited.nativeSourcePoisonNpcOutput();
            this.nativeSourcePhysicalNpcOutput = inherited.nativeSourcePhysicalNpcOutput();
            this.nativeSourcePhysicalNpcCommit = inherited.nativeSourcePhysicalNpcCommit();
            this.nativeSourceSingleNpcOutput = inherited.nativeSourceSingleNpcOutput();
            this.nativeSourceCubeStatusProof = inherited.nativeSourceCubeStatusProof();
            this.nativeSourceDollDefenseSnapshot = inherited.nativeSourceDollDefenseSnapshot();
            this.nativeSourceMagicSnapshot = inherited.nativeSourceMagicSnapshot();
            this.nativeSourceFirstPlayers = inherited.nativeSourceFirstPlayers();
        }
}
 // Source L1SkillUse MASS_SLOW excludes only caster and caster-owned companions, not other pets.
 static boolean companionSlowEligible(SourceNpcPhysicalStats stats){return stats!=null;}
 static void worldSleep(ClientSession pc,byte[] packet,BattleSpellCatalog.SleepSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaWorldSleepBody(packet,spell));}
 static void battleSleep(ClientSession pc,byte[] packet,BattleSpellCatalog.SleepSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaBattleSleepBody(packet,spell));}
 static void worldSlow(ClientSession pc,byte[] packet,BattleSpellCatalog.MassSlowSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaWorldSlowBody(packet,spell));}
 static void battleSlow(ClientSession pc,byte[] packet,BattleSpellCatalog.MassSlowSpell spell)throws IOException{run(pc,spell.skillId(),()->pc.nativeAreaBattleSlowBody(packet,spell));}
 static List<ClientSession> roomCombatants(GameHub hub,long room){return filter(hub.roomCombatants(room));}
 static ClassicWorldMonsterPopulation.Snapshot worldNpc(GameHub hub,int map,int instance,long id){var c=current.get();return c.npc(id)?hub.classicWorldMonster(map,instance,id):null;}
 static BattleQuestEncounter.Snapshot questNpc(GameHub hub,long room,long id){var c=current.get();return c.npc(id)?hub.questMonster(room,id):null;}
 static BattleTimeAttackEncounter.Snapshot timeNpc(GameHub hub,long room,long id){var c=current.get();return c.npc(id)?hub.timeAttackActor(room,id):null;}
 static List<BattleCompanion.Snapshot> companions(ClientSession owner){var c=current.get();return NativeCompanionTargets.areaSnapshots(owner).stream().filter(s->c.npc(s.objectId())).toList();}
 static BattleCompanion companion(ClientSession owner,long id){var c=current.get();var n=c.npcs.get(id);return n!=null&&n.kind==3&&n.owner==owner&&c.npc(id)?(BattleCompanion)n.actor:null;}
 @SuppressWarnings("unchecked") static Map<Long,Object> phantasms(ClientSession pc,Object phase,long room,int map,int instance,int x,int y,int range,long now){
  var values=(Map<Long,Object>)CombatAccess.call(pc,"nativeCompanionAreaPhantasms","(L"+P+"ClientSession$Phase;JIIIIIJ)Ljava/util/Map;",phase,room,map,instance,x,y,range,now);
  if(values==null)return null;var c=current.get();var result=new LinkedHashMap<Long,Object>();
  for(var entry:values.entrySet()){
   Object value=entry.getValue();var player=(ClientSession)CombatAccess.call(value,"player","()L"+P+"ClientSession;");
   String kind=String.valueOf(CombatAccess.call(value,"kind","()L"+P+"BattleTrueTarget$Kind;"));
   if(kind.equals("PLAYER")?player!=null&&c.player(player):c.npc(entry.getKey()))result.put(entry.getKey(),value);
  }return result;
 }
 static void silence(ClientSession pc,byte[] packet,BattleSpellCatalog.AreaSilenceSpell spell,int fixture)throws IOException{run(pc,spell.skillId(),()->pc.nativeMaterialAreaSilenceBody(packet,spell,fixture));}
 static void wind(ClientSession pc,byte[] packet,BattleSpellCatalog.WindShackleSpell spell,int fixture)throws IOException{run(pc,spell.skillId(),()->pc.nativeMaterialAreaWindBody(packet,spell,fixture));}
 static void nativeCompanionAreaOriginalRun(ClientSession pc,int skill,NativeSelfCastAction.Task body)throws IOException{
  var context=new Context(pc);
  if(!context.phase.equals("WORLD")&&!context.phase.equals("BATTLE"))return;
  // Nonblocking acquisition prevents inversion with an existing two-player action or a changing character ID.
  var locks=context.players.stream().map(ClientSession::nativePlayerActionLock).distinct().toList();int acquired=0;
  for(int attempt=0;attempt<32;attempt++){
   for(;acquired<locks.size();acquired++)if(!locks.get(acquired).tryLock())break;
   if(acquired==locks.size())break;
   while(acquired>0)locks.get(--acquired).unlock();
   if(Thread.currentThread().isInterrupted()||!context.caster.matches(pc))return;
   LockSupport.parkNanos(1_000_000L);
  }
  if(acquired!=locks.size())return;
  var scope=NativePlayerActionScope.enter(context.players.toArray(ClientSession[]::new));
  try{
   if(!context.scene())return;
   NativeLightAction.commitPair(pc,pc,()->NativeWorldMaterialPolicy.run(pc,context.phase.equals("WORLD"),skill,()->{
    synchronized(context.hub){
     if(!NativeSourceAreaPlayerProof.scene(context))return;
     if(context.population!=null)synchronized(context.population){owned(context,body);}else owned(context,body);
    }
   }));
  }finally{
   while(acquired>0)locks.get(--acquired).unlock();
   for(var member:context.players)NativePlayerActionScope.unlocked(member);
   scope.close();
  }
 }
 static void owned(Context context,NativeSelfCastAction.Task body)throws IOException{var prior=current.get();current.set(context);try{body.run();}finally{if(prior==null)current.remove();else current.set(prior);}}
 static List<ClientSession> filter(List<ClientSession> candidates){var c=current.get();if(c==null)throw new IllegalStateException("area membership outside cast");var stream=candidates.stream();java.util.Objects.requireNonNull(c);return stream.filter(p->NativeSourceAreaPlayerProof.player(c,p)).toList();}
 static List<ClientSession> worldMembers(ClientSession pc,int map){var c=current.get();return filter(c.hub.classicWorldMembers(map,c.instance));}
 static List<ClientSession> battleMembers(GameHub hub,long room){return filter(hub.roomMembers(room));}
 static List<ClientSession> battleCombatants(GameHub hub,long room,ClientSession pc){return filter(hub.visibleBattleCombatants(room,pc));}
 static List<ClassicWorldMonsterPopulation.Snapshot> worldNpcs(GameHub hub,int map,int instance,int x,int y){var c=current.get();return hub.classicWorldMonstersInScreen(map,instance,x,y).stream().filter(n->c.npc(n.objectId())).toList();}
 static List<BattleQuestEncounter.Snapshot> questNpcs(GameHub hub,long room,ClientSession pc){var c=current.get();return hub.visibleQuestMonsters(room,pc).stream().filter(n->c.npc(n.objectId())).toList();}
 static List<BattleTimeAttackEncounter.Snapshot> timeNpcs(GameHub hub,long room,ClientSession pc){var c=current.get();return hub.visibleTimeAttackActors(room,pc).stream().filter(n->c.npc(n.objectId())).toList();}
 static boolean admit(ClientSession clientSession, int n, int n2, int n3) {
        if (n != 161 && n != 167 && n != 66 && n != 76 && n != 69) {
            throw new IllegalStateException("unreviewed area skill");
        }
        boolean bl = NativeLightAction.admitPair(clientSession, clientSession, n, n2, n3);
        if (!bl && n == 69) {
            Object[] objectArray = massUnlock.get();
            if (objectArray != null) {
                objectArray[1] = Boolean.TRUE;
            }
            NativeLightAction.deferScoped(() -> clientSession.nativeSelfOriginalSend(Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE mass-teleport material-unlock"));
        }
        return bl;
    }
 static boolean nativeTargetOriginalCaptureOutput(ClientSession receiver,byte[] plain,String note){
  Object[] unlock=massUnlock.get();if(unlock!=null&&unlock[0]==receiver&&Arrays.equals(plain,Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK)))unlock[1]=Boolean.TRUE;
  var context=current.get();if(context==null){current.remove();return false;}
  var binding=context.bindings.get(receiver);if(binding==null)return true;
  byte[] frozen=plain.clone();long id=frozen.length<5?-1:((long)(frozen[1]&255)|((long)(frozen[2]&255)<<8)|((long)(frozen[3]&255)<<16)|((long)(frozen[4]&255)<<24));
  var targets=context.players.stream().filter(pc->pc.playerObjectId()==id).toList();boolean npc=context.npcs.containsKey(id);
  Runnable output=NativeRolePublication.completion(receiver,()->{
   boolean valid;
   synchronized (context.hub) {
    // context 已成功取用 hub、population，這兩條路徑皆已確認非 null。
    if (context.population != null) {
     synchronized (context.population) {
      valid = binding.matches(receiver) && context.player(receiver)
        && targets.stream().allMatch(p -> NativeSourceIndirectPlayerProof.player(context, p))
        && (!npc || NativeCompanionTargets.publication(context, id));
     }
    } else {
     valid = binding.matches(receiver) && context.player(receiver)
       && targets.stream().allMatch(p -> NativeSourceIndirectPlayerProof.player(context, p))
       && (!npc || NativeCompanionTargets.publication(context, id));
    }
   }
   if(valid)try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException e){throw new UncheckedIOException(e);}
  });
  NativeLightAction.finishScoped((published,failure)->output.run());return true;
 }
 private NativeAreaMaterialAction(){}

    static void massTeleport(ClientSession clientSession, byte[] byArray, BattleSpellCatalog.MassTeleportSpell massTeleportSpell) throws IOException {
        NativePlayerCommit.Binding binding = NativePlayerCommit.Binding.capture(clientSession);
        NativeSourceMassTeleportExit.Proof proof = NativeSourceMassTeleportExit.capture(clientSession, binding);
        Object[] objectArray = massUnlock.get();
        Object[] objectArray2 = new Object[]{clientSession, Boolean.FALSE};
        massUnlock.set(objectArray2);
        try {
            NativeAreaMaterialAction.run(clientSession, massTeleportSpell.skillId(), () -> clientSession.nativeMaterialMassTeleportBody(byArray, massTeleportSpell));
        }
        finally {
            try {
                if (!((Boolean)objectArray2[1]).booleanValue() && NativeSourceMassTeleportExit.matches(binding, clientSession, proof) && CombatAccess.field(clientSession, "pendingPortal") == null) {
                    NativeSourceMassTeleportExit.completion(clientSession, () -> nativeSourceMassTeleportExitOriginalUnlock(binding,clientSession,proof), proof).run();
                }
            }
            finally {
                if (objectArray == null) {
                    massUnlock.remove();
                } else {
                    massUnlock.set(objectArray);
                }
            }
        }
    }

    static List<ClientSession> hubWorldMembers(GameHub gameHub, int n, int n2) {
        return NativeAreaMaterialAction.filter(gameHub.classicWorldMembers(n, n2));
    }

    static void nativeCompanionAreaCapturedRun(ClientSession clientSession, int n, NativeSelfCastAction.Task task, Context capturedContext) throws IOException {
        Context context=capturedContext;
        if (!context.phase.equals("WORLD") && !context.phase.equals("BATTLE")) {
            return;
        }
        List<ReentrantLock> list = context.players.stream().map(ClientSession::nativePlayerActionLock).distinct().toList();
        int n2 = 0;
        for (int i = 0; i < 32; ++i) {
            for(;n2<list.size();n2++)if(!list.get(n2).tryLock())break;
            if (n2 == list.size()) break;
            while (n2 > 0) {
                list.get(--n2).unlock();
            }
            if (Thread.currentThread().isInterrupted() || !NativeSourceNegativePairFirst.retry(context.caster, clientSession, context)) {
                return;
            }
            LockSupport.parkNanos(1000000L);
        }
        if (n2 != list.size()) {
            return;
        }
        NativePlayerActionScope.Ticket ticket = NativePlayerActionScope.enter((ClientSession[])context.players.toArray(ClientSession[]::new));
        try {
            if (!NativeSourceNegativePairFirst.scene(context)) {
                return;
            }
            NativeLightAction.commitPair(clientSession, clientSession, () -> NativeWorldMaterialPolicy.run(clientSession, context.phase.equals("WORLD"), n, () -> {
                synchronized (context.hub) {
                    if (!NativeSourceAreaPlayerProof.scene(context)) {
                        return;
                    }
                    if (context.population != null) {
                        synchronized (context.population) {
                            NativeAreaMaterialAction.owned(context, task);
                        }
                    } else {
                        NativeAreaMaterialAction.owned(context, task);
                    }
                }
            }));
        }
        finally {
            while (n2 > 0) {
                list.get(--n2).unlock();
            }
            for (ClientSession clientSession2 : context.players) {
                NativePlayerActionScope.unlocked(clientSession2);
            }
            ticket.close();
        }
    }

    static void run(ClientSession clientSession, int n, NativeSelfCastAction.Task task) throws IOException {
        NativeCompanionWindAction.run(clientSession, n, task);
    }

    static boolean nativeShockOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        return NativeWeaponBreakTargetLifecycle.captureOutput(clientSession, byArray, string);
    }

    static boolean nativeShockPlayerOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        return NativeShockCompanionAction.captureOutput(clientSession, byArray, string);
    }

    static boolean nativeAcceptedShockOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        return NativeShockPlayerAction.captureOutput(clientSession, byArray, string);
    }

    static boolean nativeStaticRespawnOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        return NativeAcceptedShockMutation.captureOutput(clientSession, byArray, string);
    }

    static boolean nativeFreezeAcceptedOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        return NativeStaticNpcRespawn.captureOutput(clientSession, byArray, string);
    }

    static boolean nativeBattleControlledOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        return NativeAcceptedFreezeMutation.captureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceMassSlowOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeBattleControlledCancellation.active()) {
            return NativeBattleControlledCancellation.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeBattleControlledOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceWindOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceMassSlowTargets.active()) {
            return NativeSourceMassSlowTargets.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeSourceMassSlowOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceNegativeOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceWindTargets.active()) {
            return NativeSourceWindTargets.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeSourceWindOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceLightningOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceNegativeTargets.active()) {
            return NativeSourceNegativeTargets.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeSourceNegativeOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceWeaponBuffOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceLightningLifetime.active()) {
            return NativeSourceLightningLifetime.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeSourceLightningOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceRemainingOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceWeaponBuffTargets.active()) {
            return NativeSourceWeaponBuffTargets.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeSourceWeaponBuffOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeSourceControlledOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceRemainingMaterials.active()) {
            return NativeSourceRemainingMaterials.captureOutput(clientSession, byArray, string);
        }
        return NativeAreaMaterialAction.nativeSourceRemainingOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean nativeRawOriginalCaptureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceControlledDisplay.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        return NativeAreaMaterialAction.nativeSourceControlledOriginalCaptureOutput(clientSession, byArray, string);
    }

    static boolean captureOutput(ClientSession clientSession, byte[] byArray, String string) throws IOException {
        if (NativeSourceRawDamage.captureOutput(clientSession, byArray, string)) {
            return true;
        }
        return NativeAreaMaterialAction.nativeRawOriginalCaptureOutput(clientSession, byArray, string);
    }

    private static /* synthetic */ void lambda$retainedMassUnlock$0(NativePlayerCommit.Binding binding, ClientSession clientSession) {
        if (binding.matches(clientSession)) {
            try {
                clientSession.nativeSelfOriginalSend(Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE mass-teleport admission-unlock");
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
        }
    }

    private static /* synthetic */ void nativeSourceMassTeleportExitOriginalUnlock(NativePlayerCommit.Binding binding, ClientSession clientSession, NativeSourceMassTeleportExit.Proof proof) {
        if (NativeSourceMassTeleportExit.matches(binding, clientSession, proof)) {
            try {
                NativeSourceMassTeleportExit.send(clientSession, Protocol.paralysis(Protocol.ParalysisMode.TELEPORT_UNLOCK), "S_PARALYSE mass-teleport admission-unlock", proof);
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
        }
    }
}
