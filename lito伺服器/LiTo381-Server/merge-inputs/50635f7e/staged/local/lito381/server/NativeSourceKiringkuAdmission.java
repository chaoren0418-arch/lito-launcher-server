package local.lito381.server;
import java.io.*;
/** Pre-output identity capture, revalidated at each original PC mutation under the existing ordered pair. */
final class NativeSourceKiringkuAdmission {
 static final String P="local/lito381/server/";
 record Aim(NativeAreaMaterialAction.Context c,ClientSession target,Object casterLife,Object targetLife,int cx,int cy,int tx,int ty,NativePlayerLifetime lifetime){}
 static int n(ClientSession pc,String key){return(int)CombatAccess.field(pc,key);}
 static Object life(ClientSession pc){return NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(pc,"timedStatus"),true);}
 static Aim capture(ClientSession pc,Protocol.AttackRequest request){
  if(!CombatRules.loaded()||!CombatAccess.field(pc,"phase").toString().equals("BATTLE")||(boolean)CombatAccess.field(pc,"observer")||n(pc,"playerHp")<=0)return null;
  var weapon=(SourceWeaponPhysicalStats)CombatAccess.call(pc,"activePhysicalWeapon","()L"+P+"SourceWeaponPhysicalStats;");if(!weapon.present()||weapon.weaponType()!=17)return null;
  var c=new NativeAreaMaterialAction.Context(pc);for(var target:c.players)if(c.bindings.get(target).object()==request.targetObjectId())return new Aim(c,target,life(pc),life(target),n(pc,"positionX"),n(pc,"positionY"),n(target,"positionX"),n(target,"positionY"),NativePlayerLifetime.of(c.hub));return null;
 }
 static boolean valid(Aim a,ClientSession caster,ClientSession target){return a.c.pc==caster&&a.target==target&&a.lifetime.mode==NativePlayerLifetime.Mode.OPEN&&!(boolean)CombatAccess.field(a.c.hub,"nativeStaticMotionClosed")&&a.c.scene()&&a.c.player(target)&&!(boolean)CombatAccess.field(caster,"observer")&&!(boolean)CombatAccess.field(target,"observer")&&n(caster,"playerHp")>0&&n(target,"playerHp")>0&&n(caster,"positionX")==a.cx&&n(caster,"positionY")==a.cy&&n(target,"positionX")==a.tx&&n(target,"positionY")==a.ty&&life(caster)==a.casterLife&&life(target)==a.targetLife;}
 static void owned(Aim aim,ClientSession caster,ClientSession target,NativeSelfCastAction.Task task)throws IOException{
  if(aim==null){task.run();return;}try{NativeSourceOuterPair.run(caster,target,()->{synchronized(aim.lifetime.admission){if(!valid(aim,caster,target))return;try{NativeLightAction.commitPair(caster,target,task);}catch(IOException error){throw new UncheckedIOException(error);}}});}catch(UncheckedIOException error){throw error.getCause();}
 }
 static void damage(ClientSession target,int damage,ClientSession caster,Aim aim)throws IOException{owned(aim,caster,target,()->CombatAccess.call(target,"takePlayerDamage","(IL"+P+"ClientSession;)V",damage,caster));}
 static void bounce(ClientSession target,ClientSession caster,SourceWeaponPhysicalStats active,SourceWeaponPhysicalStats source,boolean sourceActive,long object,Aim aim)throws IOException{owned(aim,caster,target,()->CombatAccess.call(target,"applyBounceAttackWeaponDamage","(L"+P+"ClientSession;L"+P+"SourceWeaponPhysicalStats;L"+P+"SourceWeaponPhysicalStats;ZJ)V",caster,active,source,sourceActive,object));}
 static void venom(ClientSession caster,ClientSession target,BattleQuestEncounter.Snapshot quest,BattleTimeAttackEncounter.Snapshot time,ClassicWorldMonsterPopulation.Snapshot world,boolean weapon,boolean hit,long object,Aim aim)throws IOException{owned(aim,caster,target,()->CombatAccess.call(caster,"attemptEnchantVenomPoison","(L"+P+"ClientSession;L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;L"+P+"ClassicWorldMonsterPopulation$Snapshot;ZZJ)V",target,quest,time,world,weapon,hit,object));}
 static void doll(ClientSession caster,NativeMagicDollEffects.AttackProc proc,long object,ClientSession target,BattleQuestEncounter.Snapshot quest,BattleTimeAttackEncounter.Snapshot time,SourceNpcPhysicalStats stats,Aim aim)throws IOException{owned(aim,caster,target,()->CombatAccess.call(caster,"applyNativeMagicDollAttackProcs","(L"+P+"NativeMagicDollEffects$AttackProc;JL"+P+"ClientSession;L"+P+"BattleQuestEncounter$Snapshot;L"+P+"BattleTimeAttackEncounter$Snapshot;L"+P+"SourceNpcPhysicalStats;)V",proc,object,target,quest,time,stats));}
 private NativeSourceKiringkuAdmission(){}
}
