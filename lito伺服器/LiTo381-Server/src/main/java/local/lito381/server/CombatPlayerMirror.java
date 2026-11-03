package local.lito381.server;

/** Source PC reflection decisions and signed recovery; all actor references are call-local. */
final class CombatPlayerMirror {
 private static final String P="local/lito381/server/",C="L"+P+"ClientSession;",D="L"+P+"CombatPlayerMirror$Decision;";
 record Decision(boolean active,boolean eligible,boolean triggered,int wisdom,int roll,int reflectedDamage){
  Decision{if(wisdom<0||wisdom>255||roll<0||roll>99||triggered&&(!active||!eligible||wisdom<roll)||!triggered&&reflectedDamage!=0)throw new IllegalArgumentException("invalid source PC reflection decision");}
  static Decision inactive(int wisdom,int roll){return new Decision(false,false,false,wisdom,roll,0);}
 }
 static Decision decide(ClientSession target,int damage,boolean eligible,int roll,long now,CombatMagicDamage.Attempt attempt){
  if(now<0||roll<0||roll>99)throw new IllegalArgumentException("invalid source reflection clock/roll");
  if(!CombatRules.loaded()||attempt==null||attempt.magicResistance()==-1){var d=(BattleCounterMirrorStatus.Decision)CombatAccess.call(target,"counterMirrorDecision","(IZIJ)L"+P+"BattleCounterMirrorStatus$Decision;",damage,eligible,roll,now);return new Decision(d.active(),d.eligible(),d.triggered(),d.wisdom(),d.roll(),d.reflectedDamage());}
  int wisdom=CombatAccess.integerCall(target,"effectiveWisdom","(J)I",now);
  if(attempt.magicResistance()==-2||CombatPlayerMagic.protectedZero(target,now))return Decision.inactive(wisdom,roll);
  var status=(BattleCounterMirrorStatus)CombatAccess.field(target,"counterMirrorStatus");
  synchronized(status){boolean active=status.active(now);boolean trigger=active&&eligible&&wisdom>=roll;if(trigger)status.cancel();return new Decision(active,eligible,trigger,wisdom,roll,trigger?damage:0);}
 }
 static void publish(ClientSession caster,ClientSession target,Decision d,String description){CombatAccess.call(caster,"publishSourcePlayerCounterMirrorTrigger","("+C+D+"Ljava/lang/String;)V",target,d,description);}
 static void receiveBattle(ClientSession caster,int damage,ClientSession defender,boolean earthBindBlocks){CombatPlayerRecovery.battle(caster,damage,defender,earthBindBlocks);}
 static void receiveWorld(ClientSession caster,int damage,ClientSession defender,boolean earthBindBlocks){CombatPlayerRecovery.world(caster,damage,defender,earthBindBlocks);}
}
