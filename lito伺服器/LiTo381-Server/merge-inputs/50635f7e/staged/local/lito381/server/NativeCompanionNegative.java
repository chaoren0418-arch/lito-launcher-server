package local.lito381.server;
import java.io.*;import java.util.*;
/** Reuse the existing per-cast single-target actor/receipt/publication authority for WORLD negative skills. */
final class NativeCompanionNegative {
 static final String P="local/lito381/server/",STAGE="nativeWeaponBreakTarget",ID="nativeWeaponBreakTargetId",RECEIPT="nativeWeaponBreakPeerReceipt";
 static boolean owns(NativeAreaMaterialAction.Context c,ClientSession pc){return c!=null&&c.pc==pc&&c.phase.equals("WORLD")&&(int)CombatAccess.field(c,STAGE)!=0;}
 static ClientSession selected(NativeAreaMaterialAction.Context c,long id){
  for(var player:c.players){var binding=c.bindings.get(player);if(binding!=null&&binding.object()==id)return player;}
  var n=c.npcs.get(id);return n!=null&&n.kind()==3?(ClientSession)n.owner():c.pc;
 }
 static void world(ClientSession pc,byte[] packet,NativeSelfCastAction.Task body)throws IOException{
  if(!NativeCompanionPlacement.world(pc)){body.run();return;}
  long id=Protocol.attackSpellRequest(packet).targetObjectId();var c=new NativeAreaMaterialAction.Context(pc);var owner=selected(c,id);var receipt=owner==pc?null:NativePlayerVitalCompletions.reserve(owner);if(owner!=pc&&receipt==null)return;
  CombatAccess.set(c,STAGE,1);CombatAccess.set(c,ID,id);CombatAccess.set(c,RECEIPT,receipt);var prior=NativeAreaMaterialAction.current.get();NativeAreaMaterialAction.current.set(c);
  try{body.run();}finally{if(prior==null)NativeAreaMaterialAction.current.remove();else NativeAreaMaterialAction.current.set(prior);if(receipt!=null&&!receipt.released.get())receipt.close();}
 }
 static void body(ClientSession pc,NativeSelfCastAction.Task body)throws IOException{var c=NativeAreaMaterialAction.current.get();if(!owns(c,pc)){body.run();return;}int prior=(int)CombatAccess.field(c,STAGE);CombatAccess.set(c,STAGE,2);try{body.run();}finally{CombatAccess.set(c,STAGE,prior);}}
 static void entangleBody(ClientSession pc,byte[] packet,BattleSpellCatalog.EntangleSpell spell,int roll)throws IOException{body(pc,()->pc.nativeMaterialEntangleBody(packet,spell,roll));}
 static void eraseBody(ClientSession pc,byte[] packet,BattleSpellCatalog.EraseMagicSpell spell,int roll)throws IOException{body(pc,()->pc.nativeMaterialEraseBody(packet,spell,roll));}
 static ClientSession owner(ClientSession pc,long id){var c=NativeAreaMaterialAction.current.get();return owns(c,pc)&&(long)CombatAccess.field(c,ID)==id?selected(c,id):NativeNegativeMaterialAction.nativeSceneOriginalOwner(pc,id);}
 static boolean admit(ClientSession pc,ClientSession player,ClientSession companionOwner,int skill,int action,int reuse){var c=NativeAreaMaterialAction.current.get();if(owns(c,pc)){
  long id=(long)CombatAccess.field(c,ID);if(!c.scene())return false;
  if(player!=null){var binding=c.bindings.get(player);if(binding==null||binding.object()!=id||!c.player(player))return false;}
  else if(id!=pc.playerObjectId()){var n=c.npcs.get(id);if(n==null||!c.npc(id)||companionOwner!=null&&(n.kind()!=3||n.owner()!=companionOwner))return false;}
 }return NativeNegativeMaterialAction.nativeSceneOriginalAdmit(pc,player,companionOwner,skill,action,reuse);}
 static List<ClientSession> members(List<ClientSession> original,ClientSession pc){return NativeCompanionPlacement.world(pc)?NativeCompanionTargets.owners(original,pc):original;}
 static boolean objectPacket(int opcode){return opcode==Protocol.S_ACTION||opcode==Protocol.S_EFFECT||opcode==Protocol.S_POISON||opcode==Protocol.S_SPEED;}
 private NativeCompanionNegative(){}
}
