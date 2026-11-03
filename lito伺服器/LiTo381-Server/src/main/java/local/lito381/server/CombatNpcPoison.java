package local.lito381.server;
import java.util.List;

/** Source NPC poison uses existing PC status/provenance; no new actor storage. */
final class CombatNpcPoison {
 private static final String P="local/lito381/server/",C="L"+P+"ClientSession;";
 private static ClassicWorldMonsterPoisonAttack.Kind kind(ClientSession pc){return (ClassicWorldMonsterPoisonAttack.Kind)CombatAccess.field(pc,"classicWorldPoisonKind");}
 private static void clear(ClientSession pc,ClassicWorldMonsterPoisonAttack.Kind expected){if(CombatRules.loaded()&&(expected==null||kind(pc)==expected)){CombatAccess.set(pc,"classicWorldPoisonAttackerObjectId",0L);CombatAccess.set(pc,"classicWorldPoisonKind",ClassicWorldMonsterPoisonAttack.Kind.NONE);}}
 static void apply(ClientSession target,CombatNpcPlayerDamage.Attempt attempt,long now){
  if(attempt==null||!attempt.source()||attempt.poisonAttack()!=1&&attempt.poisonAttack()!=2&&attempt.poisonAttack()!=4)return;
  CombatAccess.call(target,"applyClassicWorldNpcPoison","(JIIIJLjava/lang/String;)V",attempt.npcObjectId(),attempt.npcId(),attempt.poisonAttack(),attempt.paralysisAttack(),now,"source-companion");
 }
 static void legacyWorld(ClientSession target,long objectId,int npcId,int poison,int paralysis,long now,String source,CombatNpcPlayerDamage.Attempt attempt){if(attempt==null||!attempt.source())CombatAccess.call(target,"applyClassicWorldNpcPoison","(JIIIJLjava/lang/String;)V",objectId,npcId,poison,paralysis,now,source);}
 static boolean worldFlag(boolean legacy,ClientSession pc){return legacy||CombatRules.loaded()&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&kind(pc)!=ClassicWorldMonsterPoisonAttack.Kind.NONE;}
 static void battleTick(ClientSession target,int legacy,ClientSession caster){boolean npc=CombatRules.loaded()&&kind(target)==ClassicWorldMonsterPoisonAttack.Kind.DAMAGE;CombatAccess.call(target,"takePlayerPoisonDamage","(I"+C+")V",npc?ClassicWorldMonsterPoisonAttack.DAMAGE_PER_TICK:legacy,npc?null:caster);}
 static boolean nativeCure(BattleTimedStatus timed,ClientSession pc){synchronized(timed){boolean result=timed.curePoison();if(result){clear(pc,ClassicWorldMonsterPoisonAttack.Kind.DAMAGE);if(CombatRules.loaded())CombatAccess.set(pc,"poisonAttacker",null);}return result;}}
 static boolean nativeSilence(BattleTimedStatus timed,ClientSession pc){synchronized(timed){boolean result=timed.curePoisonSilence();if(result)clear(pc,ClassicWorldMonsterPoisonAttack.Kind.SILENCE);return result;}}
 static BattleTimedStatus.ParalysisCancellation nativeCancel(BattleTimedStatus timed,ClientSession pc){synchronized(timed){var result=CombatPoisonParalysis.cancelAll(timed);if(result.present())clear(pc,ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS);return result;}}
 static void nativeClear(BattleTimedStatus timed,ClientSession pc){synchronized(timed){timed.clear();clear(pc,null);if(CombatRules.loaded())CombatAccess.set(pc,"poisonAttacker",null);}}
 static BattleTimedStatus.CurseRemoval nativeCurse(BattleTimedStatus timed,ClientSession pc){synchronized(timed){var result=timed.removeCurse();if(CombatRules.loaded()){var poison=CombatPoisonParalysis.cancelPoison(timed);result=new BattleTimedStatus.CurseRemoval(result.poisonCured()||poison.present(),CombatPoisonParalysis.merge(result.paralysisCancellation(),poison));}clear(pc,null);if(CombatRules.loaded()&&result.poisonCured())CombatAccess.set(pc,"poisonAttacker",null);return result;}}
 static BattleTimedStatus.MagicCancellation nativeMagic(BattleTimedStatus timed,ClientSession pc){synchronized(timed){var result=timed.cancelMagicEffects();if(CombatRules.loaded())result=CombatPoisonParalysis.magic(result,CombatPoisonParalysis.cancelPoison(timed));clear(pc,null);if(CombatRules.loaded()&&result.poison())CombatAccess.set(pc,"poisonAttacker",null);return result;}}
 static BattleTimedStatus.ParalysisCancellation nativeNpcParalysis(ClientSession pc){var timed=(BattleTimedStatus)CombatAccess.field(pc,"timedStatus");synchronized(timed){if(!CombatRules.loaded()||kind(pc)!=ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS)return new BattleTimedStatus.ParalysisCancellation(false,false);var result=CombatPoisonParalysis.cancelPoison(timed);if(result.present())clear(pc,ClassicWorldMonsterPoisonAttack.Kind.PARALYSIS);return result;}}
 static boolean cure(BattleTimedStatus t,ClientSession pc){return CombatPoisonLifecycle.cure(t,pc,null);}
 static boolean cureSilence(BattleTimedStatus t,ClientSession pc){return CombatPoisonLifecycle.silence(t,pc,null);}
 static BattleTimedStatus.ParalysisCancellation cancelParalysis(BattleTimedStatus t,ClientSession pc){return CombatPoisonLifecycle.cancel(t,pc,null);}
 static void clearTimed(BattleTimedStatus t,ClientSession pc){CombatPoisonLifecycle.clear(t,pc,null);}
 static BattleTimedStatus.CurseRemoval removeCurse(BattleTimedStatus t,ClientSession pc){return CombatPoisonLifecycle.curse(t,pc,null);}
 static BattleTimedStatus.MagicCancellation cancelMagic(BattleTimedStatus t,ClientSession pc){return CombatPoisonLifecycle.magic(t,pc,null);}
 static BattleTimedStatus.ParalysisCancellation cureNpcParalysis(ClientSession pc){return CombatPoisonLifecycle.npcParalysis(pc,null);}
 static Boolean silencePending(ClientSession pc){return CombatRules.loaded()&&kind(pc)==ClassicWorldMonsterPoisonAttack.Kind.SILENCE&&((BattleTimedStatus)CombatAccess.field(pc,"timedStatus")).poisonSilenceActive();}
 private static void endSilence(ClientSession pc,Boolean pending){if(Boolean.TRUE.equals(pending)&&!CombatPoisonLifecycle.closing(pc))CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",Protocol.messageCode(311),"S_MESSAGE_CODE source-npc-silence-poison-cured=311");}
 static void poisonPalette(ClientSession pc,int palette,String reason,Boolean pending){CombatAccess.call(pc,"broadcastPoisonState","(ILjava/lang/String;)V",palette,reason);endSilence(pc,pending);}
 static void cancellationPublish(ClientSession caster,ClientSession target,Object cancellation,List<ClientSession> recipients,long now,Boolean pending){CombatAccess.call(caster,"publishPlayerMagicCancellation","("+C+"L"+P+"ClientSession$PlayerMagicCancellation;Ljava/util/List;J)V",target,cancellation,recipients,now);endSilence(target,pending);}
}
