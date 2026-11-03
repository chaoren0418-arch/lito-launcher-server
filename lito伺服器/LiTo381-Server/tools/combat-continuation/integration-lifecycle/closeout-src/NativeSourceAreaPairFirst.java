package local.lito381.server;
import java.util.*;
/** 76/167 first primary identity before original roster locks, including natural BATTLE NPCs. */
final class NativeSourceAreaPairFirst {
 record First(int skill,NativeSourceSelfFinalFrame.First source,NativeSourceTargetNegativeLife.Proof proof,Map<ClientSession,Object> playerStatuses){First{playerStatuses=Collections.unmodifiableMap(new IdentityHashMap<>(playerStatuses));}}
 static Map<ClientSession,Object> players(NativeAreaMaterialAction.Context c){var values=new IdentityHashMap<ClientSession,Object>();if(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot)for(var entry:snapshot.members().entrySet()){var pc=entry.getKey();var m=entry.getValue();Object status=CombatAccess.field(pc,"timedStatus");if(status!=null&&NativeSourceTargetNegativeLife.raw(status)==m.life&&m.acceptedLife==m.life&&m.binding.equals(c.bindings.get(pc))&&m.binding.matchesFast(pc))values.put(pc,status);}return values;}
 static boolean secondaryPlayer(NativeAreaMaterialAction.Context c,ClientSession pc){if(!(c.nativeSourceAreaPairFirst instanceof First first)||first.skill()!=76)return true;Object status=first.playerStatuses().get(pc);return status!=null&&CombatAccess.field(pc,"timedStatus")==status;}
 static void capture(NativeAreaMaterialAction.Context c,NativeAreaMaterialAction.Npc n,ClientSession player,int skill){
  if(!NativeSourceTargetNegativeLife.enabled(c)||(player==null&&(n==null||n.kind()<1||n.kind()>3)))return;
  var binding=player==null?null:c.bindings.get(player);
  if(player!=null&&binding==null){c.nativeSourceAreaPairFirst=Boolean.FALSE;return;}
  long id=player==null?n.id():binding.object();
  var first=NativeSourceMassTeleportExit.capture(c.pc,c.caster).first();Object original=c.nativeSourceTargetNegativeProof;
  try{c.nativeSourceAreaPairFirst=first!=null&&original==null&&NativeSourceTargetNegativeLife.capture(c,id)?new First(skill,first,NativeSourceTargetNegativeLife.proof(c),players(c)):Boolean.FALSE;}
  finally{c.nativeSourceTargetNegativeProof=original;}
 }
 static void capture(NativeSourceMassSlowTargets.Aim a){capture(a.c,a.target,a.player,76);}
 static void capture(NativeSourceWindTargets.Aim a){capture(a.c,a.target,a.player,167);}
 static Iterator<?> secondaryIterator(Iterator<?> original){var c=NativeAreaMaterialAction.current.get();return c!=null&&c.nativeSourceAreaPairFirst instanceof First first&&first.skill()==76?new Cursor(original,c):original;}
 static final class Cursor implements Iterator<Object>{
  final Iterator<?> original;final NativeAreaMaterialAction.Context context;Object next;boolean ready;
  Cursor(Iterator<?> original,NativeAreaMaterialAction.Context context){this.original=original;this.context=context;}
  public boolean hasNext(){while(!ready&&original.hasNext()){Object value=original.next();if(!(value instanceof ClientSession pc))throw new IllegalStateException("verified Source76 PC iterator required");if(NativeSourceAreaPlayerProof.player(context,pc)){next=pc;ready=true;}}return ready;}
  public Object next(){if(!hasNext())throw new NoSuchElementException();Object value=next;next=null;ready=false;return value;}
 }
 static boolean current(NativeAreaMaterialAction.Context c){
  if(c==null||c.nativeSourceAreaPairFirst==null)return true;
  if(!(c.nativeSourceAreaPairFirst instanceof First first)||!NativeSourceSelfFinalFrame.current(first.source()))return false;
  var p=first.proof();if(p==null||p.life()!=first.source().lifetime()||!NativeSourceTargetNegativeLife.player(p.source(),p.sourceTimed()))return false;
  var m=p.member();if(m==null||p.peer()!=m.pc||p.peerTimed()==null||CombatAccess.field(m.pc,"timedStatus")!=p.peerTimed()||NativeSourceTargetNegativeLife.raw(p.peerTimed())!=m.life||m.acceptedLife!=m.life||!m.current(false)||NativeSourceRawDamage.number(m.pc,"playerHp")<=0)return false;
  var n=p.npc();if(n==null)return c.players.contains(p.peer())&&p.member().binding.equals(c.bindings.get(p.peer()))&&NativeSourceTargetNegativeLife.player(p.member(),p.peerTimed());
  if(n.kind()==1||n.kind()==2)return NativeSourceTargetNegativeLife.current(c,p);
  return n.kind()==3&&c.npcs.get(n.id())==n&&n.owner()==p.peer()&&n.actor() instanceof BattleCompanion pet&&p.actorTimed()!=null&&CombatAccess.field(pet,"timedStatus")==p.actorTimed()&&p.actorLife()!=null&&NativeSourceTargetNegativeLife.raw(p.actorTimed())==p.actorLife()&&NativeSourceNpcFirstLife.first(pet,n,c)==p.actorLife()&&NativeSourceControlledDisplay.attached(p.peer(),pet)&&NativeSourceRawDamage.number(pet,"hp")>0;
 }
 static boolean current(NativeSourceMassSlowTargets.Aim a){return current(a.c);}
 static boolean current(NativeSourceWindTargets.Aim a){return current(a.c);}
 static void close(NativeSourceMassSlowTargets.Aim a){a.c.nativeSourceAreaPairFirst=null;}
 static void close(NativeSourceWindTargets.Aim a){a.c.nativeSourceAreaPairFirst=null;}
 static boolean retry(NativePlayerCommit.Binding binding,ClientSession pc,NativeAreaMaterialAction.Context c){return current(c)&&NativeSourceNegativePairFirst.retry(binding,pc,c);}
 static boolean scene(NativeAreaMaterialAction.Context c){return current(c)&&NativeSourceNegativePairFirst.scene(c);}
 private NativeSourceAreaPairFirst(){}
}
