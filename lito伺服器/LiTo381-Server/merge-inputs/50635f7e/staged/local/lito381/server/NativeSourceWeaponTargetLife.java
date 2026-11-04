package local.lito381.server;
import java.io.IOException;import java.util.*;
/** A single bounded first Context precedes original weapon-break target selection and peer reservation. */
final class NativeSourceWeaponTargetLife {
 static boolean marked(NativeAreaMaterialAction.Context c){return c!=null&&c.nativeSourceWeaponFirstLife instanceof Protocol.AttackSpellRequest&&"BATTLE".equals(c.phase);}
 static boolean current(NativeAreaMaterialAction.Context c){return !marked(c)||CombatRules.loaded()&&NativeSourceTargetNegativeLife.current(c);}
 static void dispatch(ClientSession pc,byte[] payload,NativeSelfCastAction.Task task)throws IOException{
  if(!NativeSourcePairedScope.enabled(pc)||Protocol.spellSelector(payload).skillId()!=27||pc.selectedCharacterName().isBlank()){NativeWeaponBreakTargetLifecycle.nativeSourceWeaponOriginalDispatch(pc,payload,task);return;}
  var c=new NativeAreaMaterialAction.Context(pc);var request=NativeSourceTargetNegativeLife.request(Protocol.attackSpellRequest(payload),c);if(!NativeSourceTargetNegativeLife.capture(c,request.targetObjectId()))return;c.nativeSourceWeaponFirstLife=request;
  try{NativeWeaponBreakTargetLifecycle.nativeSourceWeaponFirstDispatch(pc,payload,task,c,request);}finally{NativeSourceTargetNegativeLife.close(c);}
 }
 static String phase(String original,NativeAreaMaterialAction.Context c){return c.phase;}
 static Protocol.AttackSpellRequest firstRequest(byte[] payload,NativeAreaMaterialAction.Context c,Protocol.AttackSpellRequest first){return first;}
 static NativeAreaMaterialAction.Context context(NativeAreaMaterialAction.Context c){return c;}
 static List<ClientSession> members(GameHub hub,long originalRoom,NativeAreaMaterialAction.Context c){if(!current(c))return List.of();var original=hub.roomCombatants(c.room);if(!current(c))return List.of();var values=new ArrayList<ClientSession>();for(var pc:original){var binding=c.bindings.get(pc);if(binding!=null){var member=NativeSourceFirstPlayers.member(pc,binding,c);if(member!=null&&member.current(true))values.add(pc);}}return values;}
 static void next(ClientSession pc,byte[] payload,NativeSelfCastAction.Task task,NativeAreaMaterialAction.Context c)throws IOException{if(current(c))NativeCancellationNpcMaterialAction.dispatch(pc,payload,task);}
 static NativePlayerCommit.Binding binding(ClientSession pc,NativeAreaMaterialAction.Context c){if(!marked(c))return NativePlayerCommit.Binding.capture(pc);return pc==c.pc?c.caster:c.bindings.get(pc);}
 static NativePlayerVitalCompletions.Receipt reserve(ClientSession pc,NativeAreaMaterialAction.Context c){if(!marked(c))return NativePlayerVitalCompletions.reserve(pc);var p=NativeSourceTargetNegativeLife.proof(c);return NativeSourceTargetNegativeReserve.initial(pc,c,p)?NativePlayerVitalCompletions.nativeSourceTargetNegativeReserve(pc,c,p):null;}
 static NativePlayerVitalCompletions.Receipt reuse(NativePlayerVitalCompletions.Receipt r,ClientSession pc,NativeAreaMaterialAction.Context c){if(!marked(c))return r;var p=NativeSourceTargetNegativeLife.proof(c);if(NativeSourceTargetNegativeReserve.initial(pc,c,p)&&r.life==p.life()&&r.app==p.life().application)return r;r.close();return null;}
 static Protocol.AttackSpellRequest runRequest(Protocol.AttackSpellRequest request,ClientSession pc){var c=NativeAreaMaterialAction.current.get();return marked(c)&&c.pc==pc&&c.nativeSourceWeaponFirstLife instanceof Protocol.AttackSpellRequest first&&request.skillId()==first.skillId()?first:request;}
 static boolean admit(ClientSession pc,ClientSession target,int skill){var c=NativeAreaMaterialAction.current.get();if(skill!=27||c==null||c.pc!=pc||!marked(c))return true;var p=NativeSourceTargetNegativeLife.proof(c);return p!=null&&current(c)&&(target!=null?target==p.peer():p.npc()!=null&&c.nativeWeaponBreakTargetId==p.npc().id());}
 static SourceNpcPhysicalStats prepare(ClientSession pc,BattleCompanion pet,NativeAreaMaterialAction.Context c){if(!marked(c))return NativeWeaponBreakBattleCompanion.prepare(pc,pet);return current(c)?NativeWeaponBreakBattleCompanion.nativeSourceWeaponFirstPrepare(pc,pet,c):null;}
 static BattleCompanion.Snapshot snapshot(BattleCompanion pet,NativeAreaMaterialAction.Context c){synchronized(pet){var p=NativeSourceTargetNegativeLife.proof(c);if(p==null||p.npc()==null||p.npc().actor()!=pet||!current(c))return null;return pet.snapshot();}}
 static boolean scene(NativeAreaMaterialAction.Context c){return current(c)&&c.scene()&&current(c);}
 private NativeSourceWeaponTargetLife(){}
}
