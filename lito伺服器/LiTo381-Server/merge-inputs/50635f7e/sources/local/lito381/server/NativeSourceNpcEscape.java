package local.lito381.server;
import java.util.*;
/** Source Monster escape at the existing Hub tick boundary; no queued actor or new scheduler. */
final class NativeSourceNpcEscape {
 static boolean monster(Object a){var p=NativeSourceNpcFactoryType.stamp(a);return NativeSourceNpcMotion.natural(a)&&NativeSourceNpcMotion.known(p)&&p.kind()==1;}
 static boolean blind(boolean original,Object actor){return original&&!(monster(actor)&&NativeSourceNpcMotion.motion(actor).escaped());}
 static boolean deferDoor(boolean original){return false;}
 static int reverse(int h,int x,int y,int tx,int ty){return(NativeSourceNpcMotion.direction(h,x,y,tx,ty)+4)&7;}
 static int choose(int heading,int allowed){for(int delta:new int[]{0,-1,1,2,-2,-3,3,4}){int d=(heading+delta)&7;if((allowed&(1<<d))!=0)return d;}return-1;}
 static boolean blocked(Object actor,long now){if(!(boolean)CombatAccess.call(actor,"ready","(J)Z",now))return true;for(String name:List.of("sleepActive","paralysisActive","shockStunActive","boneBreakActive","thunderGrabActive","freezeActive","earthBindActive"))if((boolean)CombatAccess.call(actor,name,"(J)Z",now))return true;return false;}
 static boolean phase(Object e,Object actor,boolean quest){if(quest)return(boolean)CombatAccess.field(e,"active");String p=String.valueOf(CombatAccess.field(e,"phase"));return(p.equals("ORDINARY_DOCUMENTED_FRAGMENT")||p.equals("FINAL_BOSSES"))&&(boolean)CombatAccess.field(actor,"activeAi");}
 static boolean door(Object room,int x,int y){return(boolean)CombatAccess.call(CombatAccess.field(room,"door"),"blocks","(II)Z",x,y)||(boolean)CombatAccess.call(CombatAccess.field(room,"siegeGate"),"blocks","(II)Z",x,y);}
 static boolean handled(GameHub hub,List<GameHub.MonsterAiTick> out,Object room,BattleRules.Mode mode,long id,int force,int sx,int sy,ClientSession target,long now,int tx,int ty,String source){
  if(!Thread.holdsLock(hub)||(mode!=BattleRules.Mode.QUEST_BATTLE&&mode!=BattleRules.Mode.TIME_ATTACK))return false;
  boolean quest=mode==BattleRules.Mode.QUEST_BATTLE;Object e=CombatAccess.field(room,quest?"questEncounter":"timeAttackEncounter");if(e==null)return false;int nx=sx,ny=sy,heading=-1;
  Object a,life,generation,motion;int segment;NativeSourceNpcFactoryType.Stamp p;ArenaMapCatalog.Arena arena;int distance,preferred;
  synchronized(e){
   a=CombatAccess.call(e,"monster","(J)Llocal/lito381/server/"+(quest?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);
   if(a==null||!monster(a))return false;p=NativeSourceNpcFactoryType.stamp(a);boolean escape=NativeSourceNpcMotion.motion(a).escaped()||(p.nativeSourceNpcAttackInterval==0&&p.nativeSourceNpcMoveInterval>0);if(!escape)return false;
   if(!phase(e,a,quest)||blocked(a,now)||(int)CombatAccess.field(a,"x")!=sx||(int)CombatAccess.field(a,"y")!=sy||(quest&&(int)CombatAccess.field(a,"force")!=force))return true;
   distance=Math.max(Math.abs(sx-tx),Math.abs(sy-ty));NativeSourceNpcMotion.onTarget(distance,a);if(!NativeSourceNpcMotion.motion(a).escaped())return false;
   if(distance>((boolean)CombatAccess.call(a,"blindActive","(J)Z",now)?1:17)||p.nativeSourceNpcMoveInterval<=0)return true;
   arena=quest?((ArenaMapCatalog.Arena[])CombatAccess.field(e,"arenas"))[force]:(ArenaMapCatalog.Arena)CombatAccess.field(e,"arena");if(arena==null)return true;
   preferred=reverse(NativeSourceNpcMotion.motion(a).heading(),sx,sy,tx,ty);life=NativeSourceMonsterHp.life(a);generation=CombatAccess.field(e,"sourceProcEncounterGeneration");motion=CombatAccess.field(a,"nativeSourceNpcMotion");segment=quest?(int)CombatAccess.field(a,"roomNumber"):0;
  }
  // Keep original PC/companion footprint discovery outside the encounter monitor.
  int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};
  try{
   var occupied=GameHub.class.getDeclaredMethod("nativeSourceNpcEscapeOccupied",room.getClass(),BattleRules.Mode.class,int.class,long.class,int.class,int.class);occupied.setAccessible(true);
   for(int delta:new int[]{0,-1,1,2,-2,-3,3,4}){int d=(preferred+delta)&7;int x=sx+dx[d],y=sy+dy[d];if(arena.nativeCanStep(sx,sy,x,y)&&(!quest||!BattleQuestBoundary.blocksMap(arena.mapId(),(int)CombatAccess.field(a,"roomNumber"),sx,sy,x,y))&&!(boolean)occupied.invoke(null,room,mode,force,id,x,y)){heading=d;nx=x;ny=y;break;}}
  }catch(ReflectiveOperationException error){throw new IllegalStateException("Source escape original occupancy",error);}
  synchronized(e){
   Object current=CombatAccess.call(e,"monster","(J)Llocal/lito381/server/"+(quest?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);
   if(current!=a||NativeSourceNpcFactoryType.stamp(a)!=p||NativeSourceMonsterHp.life(a)!=life||CombatAccess.field(e,"sourceProcEncounterGeneration")!=generation||CombatAccess.field(a,"nativeSourceNpcMotion")!=motion||(quest?((ArenaMapCatalog.Arena[])CombatAccess.field(e,"arenas"))[force]:(ArenaMapCatalog.Arena)CombatAccess.field(e,"arena"))!=arena||(quest&&((int)CombatAccess.field(a,"force")!=force||(int)CombatAccess.field(a,"roomNumber")!=segment))||!phase(e,a,quest)||blocked(a,now)||(int)CombatAccess.field(a,"x")!=sx||(int)CombatAccess.field(a,"y")!=sy)return true;
   if((boolean)CombatAccess.call(a,"blindActive","(J)Z",now)&&distance>1)return true;
   if(heading<0||door(room,nx,ny)){CombatAccess.call(a,"schedule","(JI)V",now,0);return true;}
   Object moved=CombatAccess.call(e,"nativeSourceEscapeMoveForAi","(JIIJ)Llocal/lito381/server/"+(quest?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot;",id,nx,ny,now);if(moved==null)return true;
  }
  // Constructor origin/discovery stays outside the encounter monitor, as in the original Hub path.
  out.add(new GameHub.MonsterAiTick((long)CombatAccess.field(room,"id"),mode,id,force,sx,sy,nx,ny,heading,target,0,false,null,null,null,List.of(),0,source));return true;
 }
 private NativeSourceNpcEscape(){}
}
