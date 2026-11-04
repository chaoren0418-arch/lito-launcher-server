package local.lito381.server;
import java.io.*;
/** Earth Bind borrows the existing single-target capture and owner receipt; no new cast state. */
final class NativeCompanionEarth {
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.BindSpell spell)throws IOException{NativeCompanionNegative.world(pc,packet,()->NativeEarthBindMaterialAction.nativeSceneOriginalWorld(pc,packet,spell));}
 static void body(ClientSession pc,byte[] packet,BattleSpellCatalog.BindSpell spell)throws IOException{NativeCompanionNegative.body(pc,()->pc.nativeMaterialWorldEarthBindBody(packet,spell));}
 static ClientSession target(ClientSession pc,byte[] packet,boolean world){var c=NativeAreaMaterialAction.current.get();return world&&NativeCompanionNegative.owns(c,pc)?NativeCompanionNegative.selected(c,Protocol.attackSpellRequest(packet).targetObjectId()):NativeEarthBindMaterialAction.nativeSceneOriginalTarget(pc,packet,world);}
 static boolean admit(ClientSession pc,ClientSession player,int skill,int action,int reuse){var c=NativeAreaMaterialAction.current.get();if(!NativeCompanionNegative.owns(c,pc))return NativeEarthBindMaterialAction.nativeSceneOriginalAdmit(pc,player,skill,action,reuse);
  long id=(long)CombatAccess.field(c,NativeCompanionNegative.ID);if(!c.scene())return false;
  if(player!=null){var binding=c.bindings.get(player);if(binding==null||binding.object()!=id||!c.player(player))return false;}
  else {var n=c.npcs.get(id);if(n==null||!c.npc(id))return false;if(n.kind()==3)player=(ClientSession)n.owner();}
  return NativeEarthBindMaterialAction.nativeSceneOriginalAdmit(pc,player,skill,action,reuse);
 }
 private NativeCompanionEarth(){}
}
