package local.lito381.server;
/** Actor-owned immutable scalar motion state; snapshots never retain an actor or Context. */
final class NativeSourceNpcMotion {
 record Motion(int heading,boolean escaped){}
 record Saved(NativeSourceNpcFactoryType.Stamp stamp,Motion motion){}
 static boolean natural(Object actor){return actor!=null&&(actor.getClass().getName().equals("local.lito381.server.BattleQuestEncounter$Monster")||actor.getClass().getName().equals("local.lito381.server.BattleTimeAttackEncounter$Monster"));}
 static boolean known(NativeSourceNpcFactoryType.Stamp p){return p!=null&&p.nativeSourceNpcMoveInterval!=null&&p.nativeSourceNpcAttackInterval!=null;}
 static Motion motion(Object actor){var value=CombatAccess.field(actor,"nativeSourceNpcMotion");return value instanceof Motion m&&m.heading()>=0&&m.heading()<8?m:new Motion(0,false);}
 static int direction(int heading,int x,int y,int tx,int ty){float dx=Math.abs(x-tx),dy=Math.abs(y-ty),distance=Math.max(dx,dy);if(distance==0)return heading;int ax=(int)Math.floor(dx/distance+0.59f),ay=(int)Math.floor(dy/distance+0.59f);int sx=Integer.compare(tx,x),sy=Integer.compare(ty,y);if(ax==0)sx=0;if(ay==0)sy=0;if(sx<0)return sy<0?7:sy>0?5:6;if(sx>0)return sy<0?1:sy>0?3:2;return sy<0?0:sy>0?4:heading;}
 static int moving(int x,Object actor,int y){if(natural(actor)&&known(NativeSourceNpcFactoryType.stamp(actor))){var m=motion(actor);int h=direction(m.heading(),(int)CombatAccess.field(actor,"x"),(int)CombatAccess.field(actor,"y"),x,y);CombatAccess.set(actor,"nativeSourceNpcMotion",new Motion(h,m.escaped()));}return x;}
 static void facing(Object actor,int x,int y){if(natural(actor)&&known(NativeSourceNpcFactoryType.stamp(actor))){var m=motion(actor);int h=direction(m.heading(),(int)CombatAccess.field(actor,"x"),(int)CombatAccess.field(actor,"y"),x,y);CombatAccess.set(actor,"nativeSourceNpcMotion",new Motion(h,m.escaped()));}}
 static int onTarget(int distance,Object actor){var p=NativeSourceNpcFactoryType.stamp(actor);if(natural(actor)&&known(p)&&p.nativeSourceNpcAttackInterval==0&&p.nativeSourceNpcMoveInterval>0){var m=motion(actor);if(!m.escaped())CombatAccess.set(actor,"nativeSourceNpcMotion",new Motion(m.heading(),true));}return distance;}
 static Object wrap(Object original,Object actor){if(!(original instanceof NativeSourceNpcFactoryType.Stamp p)||!natural(actor)||!known(p))return original;var m=motion(actor);return m.heading()!=0||m.escaped()?new Saved(p,m):original;}
 static Object unwrap(Object value){return value instanceof Saved saved?saved.stamp():value;}
 static Object raw(Object snapshot){return snapshot instanceof BattleQuestEncounter.Snapshot s?s.nativeSourceNpcFactoryMemory:snapshot instanceof BattleTimeAttackEncounter.Snapshot s?s.nativeSourceNpcFactoryMemory:null;}
 static Object restored(Object original,Object actor,Object snapshot){if(original instanceof NativeSourceNpcFactoryType.Stamp p&&known(p)&&natural(actor)&&raw(snapshot) instanceof Saved saved&&saved.stamp()==p&&saved.motion()!=null&&saved.motion().heading()>=0&&saved.motion().heading()<8)CombatAccess.set(actor,"nativeSourceNpcMotion",saved.motion());return original;}
 private NativeSourceNpcMotion(){}
}
