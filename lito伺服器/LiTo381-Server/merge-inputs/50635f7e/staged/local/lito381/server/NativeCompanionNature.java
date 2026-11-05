package local.lito381.server;
import java.io.*;import java.util.*;
/** Retain the original Nature retirement/ground publication; freeze WORLD actor admission before waits. */
final class NativeCompanionNature {
 static void dispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  if(Protocol.spellSelector(packet).skillId()==145&&NativeCompanionPlacement.world(pc)&&!pc.selectedCharacterName().isBlank())NativeCompanionNegative.world(pc,packet,()->NativeReturnNatureMaterialAction.nativeSceneOriginalDispatch(pc,packet,original));
  else NativeReturnNatureMaterialAction.nativeSceneOriginalDispatch(pc,packet,original);
 }
 static void cast(ClientSession pc,byte[] packet,BattleSpellCatalog.ReturnToNatureSpell spell,int roll)throws IOException{
  if(NativeCompanionPlacement.world(pc)&&!NativeCompanionNegative.owns(NativeAreaMaterialAction.current.get(),pc))NativeCompanionNegative.world(pc,packet,()->NativeReturnNatureMaterialAction.nativeSceneOriginalCast(pc,packet,spell,roll));
  else NativeReturnNatureMaterialAction.nativeSceneOriginalCast(pc,packet,spell,roll);
 }
 static boolean admit(ClientSession pc,ClientSession owner,int skill,int action,int reuse){var c=NativeAreaMaterialAction.current.get();if(NativeCompanionNegative.owns(c,pc)){
  long id=(long)CombatAccess.field(c,NativeCompanionNegative.ID);var n=c.npcs.get(id);if(!c.scene()||n==null||n.kind()!=3||n.owner()!=owner||!c.npc(id))return false;
 }return NativeReturnNatureMaterialAction.nativeSceneOriginalAdmit(pc,owner,skill,action,reuse);}
 static List<ClientSession> deletionOwners(List<ClientSession> original,NativeSummonGroundTransfer.Stage stage){var h=stage.hub.get();if(h==null)return original;var result=new ArrayList<>(original);for(var owner:NativeCompanionPlacement.owners(h,stage.map,stage.instance))if(!result.contains(owner))result.add(owner);return List.copyOf(result);}
 private NativeCompanionNature(){}
}
