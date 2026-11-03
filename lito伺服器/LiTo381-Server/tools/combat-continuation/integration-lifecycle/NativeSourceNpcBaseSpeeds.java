package local.lito381.server;
/** Source template movement/attack intervals frozen at the existing NPC birth. */
final class NativeSourceNpcBaseSpeeds {
 static int npcMillis(int gfx,int action,boolean haste,boolean slow,boolean wind,Object actor){
  var stamp=NativeSourceNpcFactoryType.stamp(actor);
  if(stamp==null||stamp.nativeSourceNpcMoveInterval==null||stamp.nativeSourceNpcAttackInterval==null||stamp.nativeSourceNpcBraveSpeed==null)return NativeSourceNpcActionIntervals.npcMillis(gfx,action,haste,slow,wind,actor);
  int type=action==0?0:1;
  int base=type==0?stamp.nativeSourceNpcMoveInterval:stamp.nativeSourceNpcAttackInterval;
  return NativeSourceNpcActionIntervals.calculate(base,type,haste,slow,stamp.nativeSourceNpcBraveSpeed,wind);
 }
 private NativeSourceNpcBaseSpeeds(){}
}
