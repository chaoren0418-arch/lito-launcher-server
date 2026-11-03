package local.lito381.server;
import java.io.IOException;
/** Preserve native target/capacity/factory tails; own the caster material commit. */
final class NativePetMaterialAction {
 static void taming(ClientSession pc,byte[] packet,BattleSpellCatalog.TamingSpell spell)throws IOException {
  NativePetWorldLifetime.run(pc,packet,()->pc.nativeMaterialTamingBody(packet,spell));
 }
 static void zombie(ClientSession pc,byte[] packet,BattleSpellCatalog.CreateZombieSpell spell)throws IOException {
  NativePetWorldLifetime.run(pc,packet,()->pc.nativeMaterialZombieBody(packet,spell));
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(target!=pc||(skill!=36&&skill!=41))throw new IllegalStateException("unreviewed pet material root");
  return NativeLightAction.admitPair(pc,pc,skill,action,reuse);
 }
 private NativePetMaterialAction(){}
}
