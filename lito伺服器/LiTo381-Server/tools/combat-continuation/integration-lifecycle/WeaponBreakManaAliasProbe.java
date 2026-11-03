package local.lito381.server;
import java.nio.file.*;import java.util.*;import static local.lito381.server.WeaponBreakNpcEpochProbe.*;
/** Valid MP/maxMP bytes can equal an NPC ID; MP is not an object publication. */
public final class WeaponBreakManaAliasProbe extends ReturnNatureMaterialProbe {
 static int missing;
 static void exercise(Target t)throws Exception{
  var pc=t.pc();var peer=t.peer();var spell=BattleSpellCatalog.weaponBreakSpell(27);set(pc,"learnedSkills",new HashSet<>(Set.of(27)));WeaponBreakNpcEpochProbe.probability();int after=(int)(t.id()&65535),maximum=(int)(t.id()>>>16);need(after>0&&after<=maximum&&maximum<=65535,"valid Source MP values encode actual target ID");set(pc,"playerMaxMp",maximum);int cost=cost(pc,27,spell);set(pc,"playerMp",after+cost);int item=Math.toIntExact(spell.itemConsumeId()),amount=spell.itemConsumeCount();NativeDeItemPublication.grant(pc,item,amount+3);long initial=pc.nativeDeInventory(0).countCatalog(item);
  if(t.population()!=null)NativeCancellationNpcMaterialAction.epoch(t.actor(),true);else NativeCombatAttacker.token((BattleTimedStatus)get(t.actor(),"timedStatus"),true);
  try(var a=new SourceActionPairProbe.Wire(pc,peer);var b=new SourceActionPairProbe.Wire(peer,pc)){
   byte[] raw=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(3).c(2).d(t.id()).h(t.x()).h(t.y()).bytes();NativeSourcePlayerActionLock.runPairOrdered(pc,peer,()->{try{call(pc,"handle",new Class[]{byte[].class},ctrl?forced(raw):raw);advance(t);}catch(Exception e){throw new RuntimeException(e);}});
   var self=collect(a);var other=collect(b);String effect=HexFormat.of().formatHex(Protocol.effectObject(t.id(),spell.effectGfx())),mana=HexFormat.of().formatHex(Protocol.manaPoint(after,maximum));need(!self.contains(effect)&&!other.contains(effect),"retired NPC effects remain suppressed");if(!self.contains(mana))missing++;if(!WeaponBreakNpcEpochProbe.before)need(self.contains(mana),"numeric MP payload alias must keep current caster acknowledgement");need((int)get(pc,"playerMp")==after&&pc.nativeDeInventory(0).countCatalog(item)==initial-amount,"actual MP/material accepted once");need(a.locked+b.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativeLightStoppedProbe.idleContext(),"alias case drains receipts/scopes with unlocked output");WeaponBreakNpcEpochProbe.checked++;
  }
 }
 public static void main(String[]args)throws Exception{targetHook=WeaponBreakManaAliasProbe::exercise;try{WeaponBreakNpcEpochProbe.main(args);}finally{targetHook=null;}if(WeaponBreakNpcEpochProbe.before)need(missing==10,"parent numeric-payload alias reproduced for five target kinds and two app flags");Files.writeString(Path.of(args[2]),"{\"cases\":10,\"checks\":"+ownChecks+",\"missing_current_caster_MP\":"+missing+",\"full_goal_achieved\":false}\n");}
}
