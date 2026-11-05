package local.lito381.server;
import java.io.*;import java.util.*;
/** Keep genuine player/BATTLE membership distinct from WORLD actor ownership. */
final class NativeCompanionArea {
 static List<ClientSession> owners(List<ClientSession> members,ClientSession caster){return NativeCompanionPlacement.world(caster)?NativeCompanionTargets.owners(members,caster):members;}
 static List<BattleCompanion.Snapshot> snapshots(ClientSession owner,ClientSession caster){return NativeCompanionPlacement.world(caster)?NativeCompanionTargets.snapshots(owner,caster):owner.classicWorldCompanionSnapshots();}
 static int map(ClientSession owner,ClientSession caster){return NativeCompanionTargets.map(owner,caster);}
 static int instance(ClientSession owner,ClientSession caster){return NativeCompanionTargets.instance(owner,caster);}
 static boolean same(ClientSession caster,ClientSession owner){return NativeCompanionTargets.same(caster,owner);}
 static void run(ClientSession pc,int skill,NativeSelfCastAction.Task body)throws IOException{
  if(!NativeCompanionPlacement.world(pc)||(skill!=66&&skill!=76)){NativeAreaMaterialAction.nativeCompanionAreaOriginalRun(pc,skill,body);return;}
  var c=new NativeAreaMaterialAction.Context(pc);
  try(var scope=new NativeCompanionTargets.Scope(c)){
   if(!scope.valid)return;CombatAccess.set(c,"nativeCompanionTargetScope",scope);CombatAccess.set(c,NativeCompanionNegative.STAGE,1);var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);
   try{NativeAreaMaterialAction.nativeCompanionAreaCapturedRun(pc,skill,()->{if(!c.scene()||NativeAreaMaterialAction.current.get()!=c)return;c.npcs.entrySet().removeIf(e->e.getValue().kind()==3&&scope.receipts.containsKey((ClientSession)e.getValue().owner())&&!scope.owns(c,(ClientSession)e.getValue().owner()));CombatAccess.set(c,NativeCompanionNegative.STAGE,0);try{body.run();}finally{CombatAccess.set(c,NativeCompanionNegative.STAGE,1);}},c);}
   finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);CombatAccess.set(c,"nativeCompanionTargetScope",null);}
  }
 }
 private NativeCompanionArea(){}
}
