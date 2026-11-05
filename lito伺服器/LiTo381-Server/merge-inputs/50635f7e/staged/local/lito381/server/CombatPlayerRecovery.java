// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
package local.lito381.server;

/** Shared signed PC recovery with retained positive dispatch; no owned actor state. */
final class CombatPlayerRecovery{
 private static final String P="local/lito381/server/",C="L"+P+"ClientSession;";
 static void battle(ClientSession caster,int damage,ClientSession defender,boolean earthBindBlocks){receive(caster,damage,defender,earthBindBlocks,false);}
 static void world(ClientSession caster,int damage,ClientSession defender,boolean earthBindBlocks){receive(caster,damage,defender,earthBindBlocks,true);}
 private static int field(ClientSession pc,String name){return ((Number)CombatAccess.field(pc,name)).intValue();}
 static void nativeSourceSignedOriginalReceive(ClientSession caster,int damage,ClientSession defender,boolean earthBindBlocks,boolean world){
  if(damage>=0||!CombatRules.loaded()){CombatAccess.call(caster,world?"takeClassicWorldPlayerDamage":"takePlayerDamage","(I"+C+"Z)V",damage,defender,earthBindBlocks);return;}
  int after,maximum;
  synchronized(caster){
   if(!String.valueOf(CombatAccess.field(caster,"phase")).equals(world?"WORLD":"BATTLE")||field(caster,"playerHp")<=0||defender==null)return;
   if(world){if(!(Boolean)CombatAccess.call(caster,"sameClassicWorldMap","("+C+")Z",defender))return;}
   else if(!CombatAccess.field(caster,"currentRoomId").equals(CombatAccess.field(defender,"currentRoomId")))return;
   double signed=damage;var inventory=(RuntimeInventory)CombatAccess.field(caster,"inventory");if(inventory.hasEquippedCatalog(145)||inventory.hasEquippedCatalog(149))signed*=1.5;
   signed*=((BattleAbilityStatus)CombatAccess.field(caster,"abilityStatus")).illusionAvatar(System.nanoTime()).incomingDamageRate();
   int committed=(int)signed;if(committed>0)throw new IllegalStateException("source negative incoming value requires signed fatal-commit support");
   maximum=field(caster,"playerMaxHp");after=(int)Math.min((long)maximum,(long)field(caster,"playerHp")-(long)committed);CombatAccess.set(caster,"playerHp",after);
  }
  CombatAccess.call(caster,"sendEncrypted","([BLjava/lang/String;)V",new Protocol.Writer().c(Protocol.S_HIT_POINT).h(after).h(maximum).bytes(),"S_HIT_POINT source-signed-recovery="+after+"/"+maximum);
  CombatAccess.call(caster,"broadcastOwnHitRatio","(Ljava/lang/String;)V","source-signed-recovery");
 }

    private static void receive(ClientSession clientSession, int n, ClientSession clientSession2, boolean bl, boolean bl2) {
        NativeSourceSignedRecovery.receive(clientSession, n, clientSession2, bl, bl2);
    }
}
