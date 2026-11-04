// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/pet-material/NativePetMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
/** Preserve native target/capacity/factory tails; own the caster material commit. */
final class NativePetMaterialAction {
 static void taming(ClientSession pc,byte[] packet,BattleSpellCatalog.TamingSpell spell)throws IOException {
  NativeBattleTamedLifecycle.run(pc,packet,()->pc.nativeMaterialTamingBody(packet,spell),36);
 }
 static void zombie(ClientSession pc,byte[] packet,BattleSpellCatalog.CreateZombieSpell spell)throws IOException {
  NativeBattleTamedLifecycle.run(pc,packet,()->pc.nativeMaterialZombieBody(packet,spell),41);
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(target!=pc||(skill!=36&&skill!=41))throw new IllegalStateException("unreviewed pet material root");
  return NativeLightAction.admitPair(pc,pc,skill,action,reuse);
 }
 private NativePetMaterialAction(){}
}
