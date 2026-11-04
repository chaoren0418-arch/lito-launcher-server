// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/ground-material/NativeGroundMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;

/** Ground placement is planned without a live lease; materials commit before the live effect. */
final class NativeGroundMaterialAction {
 static final class Rejected extends RuntimeException {Rejected(){super(null,null,false,false);}}
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.GroundSpell spell)throws IOException{
  NativeLightAction.commitSelf(pc,()->pc.nativeMaterialGroundBody(packet,spell));
 }
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.GroundSpell spell)throws IOException{
  NativeLightAction.commitSelf(pc,()->pc.nativeMaterialWorldGroundBody(packet,spell));
 }
 static void admit(ClientSession pc,BattleSpellCatalog.GroundSpell spell){
  if(spell.skillId()!=58&&spell.skillId()!=63)throw new IllegalStateException("unreviewed ground skill");
  if(!NativeLightAction.admitSelf(pc,spell.skillId(),spell.actionId(),spell.reuseDelayMillis()))throw new Rejected();
 }
 static boolean prepare(ClientSession pc,BattleSpellCatalog.GroundSpell spell){
  if(!pc.nativePlayerActionLock().isHeldByCurrentThread())throw new IllegalStateException("ground requires player action lock");
  try{if(spell.itemConsumeId()!=0)NativeCastMaterialPolicy.gm(pc);return true;}
  catch(IOException failure){NativeReuseLog.getLogger(NativeGroundMaterialAction.class).error("ground material policy read failed",failure);NativeLightAction.materialOutput(pc,Protocol.messageCode(299),"S_MESSAGE_CODE ground-material-policy=299");return false;}
 }
 static BattleGroundEffects.CastResult placeBattle(GameHub hub,long room,ClientSession pc,BattleSpellCatalog.GroundSpell spell,int sx,int sy,int x,int y,long ignored,Runnable commit){
  if(!prepare(pc,spell))return BattleGroundEffects.CastResult.rejected();
  return hub.nativeMaterialPlaceGroundSpell(room,pc,spell,sx,sy,x,y,System.nanoTime(),commit);
 }
 static ClassicWorldGroundEffects.CastResult placeWorld(GameHub hub,int map,int instance,ClientSession pc,BattleSpellCatalog.GroundSpell spell,int sx,int sy,int x,int y,long ignored,Runnable commit){
  if(!prepare(pc,spell))return ClassicWorldGroundEffects.CastResult.rejected();
  return hub.nativeMaterialPlaceWorldGroundSpell(map,instance,pc,spell,sx,sy,x,y,System.nanoTime(),commit);
 }
 @SuppressWarnings("unchecked") static List<Object> leases(Object effects){return (List<Object>)CombatAccess.field(effects,"leases");}
 static BattleGroundEffects battleCopy(BattleGroundEffects live){var copy=new BattleGroundEffects();leases(copy).addAll(leases(live));return copy;}
 static ClassicWorldGroundEffects worldCopy(ClassicWorldGroundEffects live){var copy=new ClassicWorldGroundEffects();leases(copy).addAll(leases(live));return copy;}
 static void install(Object live,Object planned){var target=leases(live);target.clear();target.addAll(leases(planned));}
 static BattleGroundEffects.CastResult nativeSourceGroundPlacementOriginalBattle(BattleGroundEffects live,BattleSpellCatalog.GroundSpell spell,long caster,int sx,int sy,int x,int y,ArenaMapCatalog.Arena map,long ignored,GameHub hub,Runnable commit){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("ground placement requires original hub monitor");
  var plan=NativeSourceGroundPlacement.copy(live);var proposed=plan.cast(spell,caster,sx,sy,x,y,map,System.nanoTime());
  if(!proposed.accepted())return proposed;
  try{commit.run();}catch(Rejected rejected){return BattleGroundEffects.CastResult.rejected();}
  plan=NativeSourceGroundPlacement.copy(live);var result=plan.cast(spell,caster,sx,sy,x,y,map,System.nanoTime());
  if(!result.accepted())throw new IllegalStateException("ground eligibility changed under hub monitor");
  NativeSourceGroundPlacement.install(live,plan);return result;
 }
 static BattleGroundEffects.CastResult castBattle(BattleGroundEffects live,BattleSpellCatalog.GroundSpell spell,long caster,int sx,int sy,int x,int y,ArenaMapCatalog.Arena map,long ignored,GameHub hub,Runnable commit){
  return NativeSourceGroundPlacement.cast(live,spell,caster,sx,sy,x,y,map,ignored,hub,commit);
 }
 static ClassicWorldGroundEffects.CastResult castWorld(ClassicWorldGroundEffects live,BattleSpellCatalog.GroundSpell spell,long caster,int sx,int sy,int x,int y,ClassicWorldMapCatalog.ClassicMap map,long ignored,GameHub hub,Object key,Runnable commit){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("world ground placement requires original hub monitor");
  var plan=worldCopy(live);var proposed=plan.cast(spell,caster,sx,sy,x,y,map,System.nanoTime());
  if(!proposed.accepted()){install(live,plan);removeEmptyWorld(hub,key,live);return proposed;}
  try{commit.run();}catch(Rejected rejected){removeEmptyWorld(hub,key,live);return ClassicWorldGroundEffects.CastResult.rejected();}
  plan=worldCopy(live);var result=plan.cast(spell,caster,sx,sy,x,y,map,System.nanoTime());
  if(!result.accepted())throw new IllegalStateException("world ground eligibility changed under hub monitor");
  install(live,plan);return result;
 }
 @SuppressWarnings("unchecked") static void removeEmptyWorld(GameHub hub,Object key,ClassicWorldGroundEffects live){
  if(leases(live).isEmpty())((java.util.Map<Object,ClassicWorldGroundEffects>)CombatAccess.field(hub,"classicWorldGroundEffects")).remove(key,live);
 }
 private NativeGroundMaterialAction(){}
}
