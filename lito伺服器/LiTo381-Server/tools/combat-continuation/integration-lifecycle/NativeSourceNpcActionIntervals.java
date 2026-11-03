package local.lito381.server;
/** Immutable Source NPC brave metadata and Source status-multiplier order on original AI schedules. */
final class NativeSourceNpcActionIntervals {
 static Boolean brave(Object actor){var stamp=NativeSourceNpcFactoryType.stamp(actor);return stamp==null?null:stamp.nativeSourceNpcBraveSpeed;}
 static int base(int original,Object actor){return brave(actor)==null?original:original<=0?960:original;}
 static int brave(int original,Object actor){return Boolean.TRUE.equals(brave(actor))?(int)(original*0.755):original;}
 static int calculate(int base,int type,boolean haste,boolean slow,boolean brave,boolean wind){int value=base<=0?960:base;if(slow)value=(int)(value*2.0);else if(haste)value=(int)(value*0.755);if(brave)value=(int)(value*0.755);if(wind&&type!=0)value=(int)(value*1.25);return Math.max(1,value);}
 static int npcMillis(int gfx,int action,boolean haste,boolean slow,boolean wind,Object actor){var brave=brave(actor);if(brave==null)return BattleActionInterval.npcMillis(gfx,action,haste,slow,wind);int base=Math.toIntExact(BattleAnimationCatalog.get().requireTimedAction(gfx,action).durationMillis());return calculate(base,action==0?0:1,haste,slow,brave,wind);}
 private NativeSourceNpcActionIntervals(){}
}
