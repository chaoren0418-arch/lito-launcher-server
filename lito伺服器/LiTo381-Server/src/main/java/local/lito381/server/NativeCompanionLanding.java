package local.lito381.server;
/** Source CheckUtil NPC teleport search. Scene/actor ownership stays with its caller. */
final class NativeCompanionLanding {
 @FunctionalInterface interface Terrain {
  /** Revalidate and relocate under the walking destination lock; false must not mutate. */
  boolean acceptAndRelocate(int x,int y);
  default boolean isNull(){return false;}
 }
 static ArenaMapCatalog.Point find(Terrain map,int centerX,int centerY){
  try{
   if(map==null||map.isNull())return null;
   if(map.acceptAndRelocate(centerX,centerY))return new ArenaMapCatalog.Point(centerX,centerY);
   // Teleportation.teleport(L1NpcInstance,...) passes radius 5 to CheckUtil.
   for(int r=1;r<=5;r++)for(int dx=-r;dx<=r;dx++)for(int dy=-r;dy<=r;dy++){
    if(Math.abs(dx)!=r&&Math.abs(dy)!=r)continue;
    int x=centerX+dx,y=centerY+dy;
    if(map.acceptAndRelocate(x,y))return new ArenaMapCatalog.Point(x,y);
   }
  }catch(RuntimeException failure){NativeReuseLog.getLogger(NativeCompanionLanding.class).error("Source companion landing commit failed; relocation rejected",failure);}
  return null;
 }
 private NativeCompanionLanding(){}
}
