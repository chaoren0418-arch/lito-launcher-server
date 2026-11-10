package local.lito381.server;
import java.io.*;import java.lang.ref.WeakReference;
/** Real PC target, role and position are frozen before the ordered Source pair wait. */
final class NativeShockPlayerAction {
 static final String FIELD="nativeShockPlayerScope";
 static final class Scope {
  final WeakReference<ClientSession> target;final Object life;final int x,y,casterX,casterY,impact;boolean admitted;volatile Object deathLife;
  final java.util.ArrayDeque<Runnable> outputs=new java.util.ArrayDeque<>();boolean linked,finished,draining;Object nativeAcceptedShockPlan;
  Scope(ClientSession pc,ClientSession target,int impact){this.target=new WeakReference<>(target);this.impact=impact;life=NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(target,"timedStatus"),true);x=integer(target,"positionX");y=integer(target,"positionY");casterX=integer(pc,"positionX");casterY=integer(pc,"positionY");}
 }
 /** A per-event immutable view preserves the original defense values and carries only this bounded scope. */
 static final class Linked extends java.util.AbstractList<Object> implements java.util.RandomAccess {
  final java.util.List<?> values;final Scope scope;
  Linked(java.util.List<?> values,Scope scope){this.values=values;this.scope=scope;}
  public Object get(int at){return values.get(at);}public int size(){return values.size();}
 }
 static java.util.List<?> link(java.util.List<?> values,NativePlayerCommit.Binding target,NativePlayerCommit.Binding caster){var c=NativeAreaMaterialAction.current.get();var s=scope(c);var actor=s==null?null:s.target.get();if(values==null||s==null||!s.admitted||actor==null||NativeLightAction.currentMaterialProducer()!=c.pc||!target.equals(c.bindings.get(actor))||caster!=null&&!caster.equals(c.caster))return values;synchronized(s){if(s.linked)throw new IllegalStateException("Shock Stun linked twice");s.linked=true;}return new Linked(values,s);}
 static void deathReset(BattleTimedStatus timed){var frame=NativePlayerCommit.frame();if(frame==null||!frame.accepted||!frame.fatal||frame.after!=0||CombatAccess.field(frame.pc,"timedStatus")!=timed||!((Object)frame.event.defense()instanceof Linked linked))return;var s=linked.scope;var target=s.target.get();if(target==frame.pc&&frame.event.target().matches(target)&&integer(target,"playerHp")==0)s.deathLife=NativeCombatAttacker.token(timed,true);}
 // The existing HP completion owns the handoff; no waiting thread or additional coordinator is needed.
 static void completed(NativePlayerCommit.Frame frame){if(!((Object)frame.event.defense()instanceof Linked linked))return;var s=linked.scope;synchronized(s){s.finished=true;}drain(s);}
 static void afterDamage(Scope s,Runnable output){synchronized(s){s.outputs.addLast(output);if(!s.linked)s.finished=true;}drain(s);}
 static void drain(Scope s){synchronized(s){if(!s.finished||s.draining)return;s.draining=true;}try{while(true){Runnable output;synchronized(s){output=s.outputs.pollFirst();if(output==null){s.draining=false;return;}}output.run();}}catch(RuntimeException|Error failure){synchronized(s){s.outputs.clear();s.draining=false;}throw failure;}}

 static int integer(Object pc,String name){return (int)CombatAccess.field(pc,name);}
 static Scope scope(NativeAreaMaterialAction.Context c){return c==null?null:(Scope)CombatAccess.field(c,FIELD);}
 static void nativeSourceTargetOriginalBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell)throws IOException{run(pc,packet,spell,false);}
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell)throws IOException{run(pc,packet,spell,true);}
 static void original(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell,boolean world)throws IOException{if(world)pc.nativeShockPlayerOriginalWorld(packet,spell);else pc.nativeShockPlayerOriginalBattle(packet,spell);}
 static void run(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell,boolean world)throws IOException{
  var request=Protocol.attackSpellRequest(packet);if(spell.skillId()!=87||request.skillId()!=87)return;var c=new NativeAreaMaterialAction.Context(pc);var target=c.players.stream().filter(p->c.bindings.get(p).object()==request.targetObjectId()).findFirst().orElse(null);if(target==null){original(pc,packet,spell,world);return;}if(!c.phase.equals(world?"WORLD":"BATTLE"))return;
  var s=new Scope(pc,target,spell.impactGfx());var receipt=target==pc?null:NativePlayerVitalCompletions.reserve(target);if(target!=pc&&receipt==null)return;CombatAccess.set(c,FIELD,s);CombatAccess.set(c,NativeCompanionNegative.STAGE,1);CombatAccess.set(c,NativeCompanionNegative.ID,request.targetObjectId());CombatAccess.set(c,NativeCompanionNegative.RECEIPT,receipt);
  try{NativeAreaMaterialAction.owned(c,()->NativeLightAction.commitPair(pc,target,()->{if(!valid(c,s,false))return;NativeWorldMaterialPolicy.run(pc,true,87,()->{synchronized(c.hub){var timed=(BattleTimedStatus)CombatAccess.field(target,"timedStatus");synchronized(timed){synchronized(NativePlayerLifetime.of(c.hub).admission){
   if(!valid(c,s,false))return;CombatAccess.set(c,NativeCompanionNegative.STAGE,4);try{if(world)pc.nativeShockPlayerWorldBody(packet,spell);else pc.nativeShockPlayerBattleBody(packet,spell);}finally{CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}
  }}}});}));}finally{CombatAccess.set(c,FIELD,null);if(receipt!=null&&!receipt.released.get())receipt.close();}
 }
 static boolean valid(NativeAreaMaterialAction.Context c,Scope s,boolean publication){
  // A reserved role pair is not a damage admission. A stopped coordinator cannot accept the later PC HP event.
  if(!publication&&NativePlayerLifetime.of(c.hub).mode!=NativePlayerLifetime.Mode.OPEN)return false;
  var target=s==null?null:s.target.get();if(target==null||!c.scene()||!c.player(target)||(boolean)CombatAccess.field(target,"sourceItemDisconnected")||(boolean)CombatAccess.field(c.hub,"nativeStaticMotionClosed")||integer(target,"positionX")!=s.x||integer(target,"positionY")!=s.y||integer(c.pc,"positionX")!=s.casterX||integer(c.pc,"positionY")!=s.casterY)return false;
  if(publication&&integer(target,"playerHp")==0)return s.deathLife!=null&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(target,"timedStatus"),false)==s.deathLife;return integer(target,"playerHp")>0&&NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(target,"timedStatus"),false)==s.life;
 }
 static boolean admit(ClientSession pc,BattleSpellCatalog.ShockStunSpell spell){
  var c=NativeAreaMaterialAction.current.get();var s=scope(c);if(c==null||c.pc!=pc||s==null||!valid(c,s,false))return false;var target=s.target.get();int hpCost=CombatDatabase.value(0,87,"ShockStunSpell","hpCost");if(integer(pc,"playerHp")<=hpCost||!NativeLightAction.admitPair(pc,target,87,BattleActionCodes.attackForWalk(pc.battleWalkAction()),spell.reuseDelayMillis()))return false;
  s.admitted=true;if(hpCost!=0){int hp=Math.subtractExact(integer(pc,"playerHp"),hpCost);CombatAccess.set(pc,"playerHp",hp);CombatCompanionSpells.send(pc,new Protocol.Writer().c(Protocol.S_HIT_POINT).h(hp).h(integer(pc,"playerMaxHp")).bytes(),"S_HIT_POINT Shock Stun configured cost");}CombatAccess.call(pc,"sendInventoryDelta","(Llocal/lito381/server/RuntimeInventory;Llocal/lito381/server/RuntimeInventory;)V",NativeCastMaterial.previous(pc),NativeCastMaterial.next(pc));return true;
 }
 static long dword(byte[] p,int at){return Integer.toUnsignedLong((p[at]&255)|(p[at+1]&255)<<8|(p[at+2]&255)<<16|(p[at+3]&255)<<24);}
 static boolean deathPacket(NativeAreaMaterialAction.Context c,Scope s,ClientSession receiver,byte[] packet){var target=s.target.get();if(target==null)return false;int op=packet[0]&255;long id=c.bindings.get(target).object();return op==Protocol.S_ATTACK&&packet.length>=10&&dword(packet,2)==c.caster.object()&&dword(packet,6)==id||java.util.Arrays.equals(packet,Protocol.effectObject(id,s.impact))||receiver==c.pc&&(op==Protocol.S_MANA_POINT||op==Protocol.S_HIT_POINT||op==Protocol.S_CHANGE_DIRECTION);}
 static boolean captureOutput(ClientSession receiver,byte[] packet,String note) throws IOException{
  if(NativeResurrectionQueueLife.captureMana(receiver,packet,note))return true;
  var c=NativeAreaMaterialAction.current.get();var s=scope(c);if(s==null||(int)CombatAccess.field(c,NativeCompanionNegative.STAGE)!=4)return NativeAreaMaterialAction.nativeShockPlayerOriginalCaptureOutput(receiver,packet,note);var binding=c.bindings.get(receiver);if(binding==null)return true;byte[] frozen=packet.clone();
  NativeLightAction.deferWeaponTargetOutput(()->afterDamage(s,()->{var completion=new Runnable[1];NativeSourcePlayerActionLock.run(receiver,()->{if(binding.matches(receiver))completion[0]=NativeRolePublication.completion(receiver,()->{boolean allowed;synchronized(c.hub){var target=s.target.get();allowed=binding.matches(receiver)&&c.player(receiver)&&valid(c,s,true)&&target!=null&&(integer(target,"playerHp")>0||deathPacket(c,s,receiver,frozen));}if(allowed)try{receiver.nativeSelfOriginalSend(frozen,note);}catch(IOException failure){throw new UncheckedIOException(failure);}});});if(completion[0]!=null)completion[0].run();}));return true;
 }
 private NativeShockPlayerAction(){}

 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.ShockStunSpell spell)throws IOException{NativeSourceTargetNpc.shockEnter(pc,packet,spell);}

}
