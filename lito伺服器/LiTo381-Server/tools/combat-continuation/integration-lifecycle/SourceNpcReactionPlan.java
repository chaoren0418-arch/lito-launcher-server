package local.lito381.server;
import java.lang.reflect.*;
/** Actual immutable native Ref/snapshot ABI only; no PC/Hub/game/socket/database allocation. */
public final class SourceNpcReactionPlan {
 static int cases;static void check(boolean v,String why){if(!v)throw new AssertionError(why);cases++;}
 static Object snapshot(Class<?>type,int gfx)throws Exception{var parts=type.getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];for(int i=0;i<parts.length;i++){var p=parts[i];types[i]=p.getType();values[i]=types[i]==int.class?(p.getName().equals("gfx")?gfx:1):types[i]==long.class?7L:types[i]==boolean.class?false:types[i]==String.class?"":types[i].isEnum()?types[i].getEnumConstants()[0]:null;}var c=type.getDeclaredConstructor(types);c.setAccessible(true);return c.newInstance(values);}
 static NativeCombatAttacker.Ref ref(int kind,long id){return new NativeCombatAttacker.Ref(kind,id,new Object(),0,null,5,0,0);}
 public static void main(String[]args)throws Exception{
  check(NativeSourceNpcReaction.scope(null,7)==-1,"unscoped original fallback");check(NativeSourceNpcReaction.scope(ref(1,7),7)==-1,"WORLD original fallback");check(NativeSourceNpcReaction.scope(ref(5,7),7)==-1,"unsupported kind original fallback");
  check(NativeSourceNpcReaction.scope(ref(2,7),7)==1,"original companion Ref");check(NativeSourceNpcReaction.scope(ref(3,7),7)==1,"original Quest Ref");check(NativeSourceNpcReaction.scope(ref(4,7),7)==1,"original Time Ref");check(NativeSourceNpcReaction.scope(ref(3,7),8)==0,"scoped mismatched id rejects instead of fresh lookup");check(NativeSourceNpcReaction.scope(ref(2,0),0)==0,"zero id cannot become an NPC target");check(NativeSourceNpcReaction.scope(ref(0,7),7)==-1,"unsupported zero kind fallback");
  var q=snapshot(BattleQuestEncounter.Snapshot.class,53);var t=snapshot(BattleTimeAttackEncounter.Snapshot.class,222);var c=snapshot(BattleCompanion.Snapshot.class,333);
  check(NativeSourceNpcReaction.gfx(3,q)==53,"actual Quest snapshot gfx");check(NativeSourceNpcReaction.gfx(4,t)==222,"actual Time snapshot gfx");check(NativeSourceNpcReaction.gfx(2,c)==333,"actual companion snapshot gfx");check(NativeSourceNpcReaction.gfx(3,t)==null&&NativeSourceNpcReaction.gfx(4,q)==null,"same id cannot cross typed encounter snapshot");check(NativeSourceNpcReaction.gfx(2,new Object())==null,"arbitrary/PC object cannot supply NPC gfx");check(NativeSourceNpcReaction.gfx(3,null)==null,"missing original Live snapshot rejects");check(NativeSourceNpcReaction.gfx(3,snapshot(BattleQuestEncounter.Snapshot.class,0))==0,"Source nonpositive gfx membership is not filtered");
  if(cases!=16)throw new AssertionError(cases);System.out.println("PASS16 actual native NPC Ref scope and typed snapshot gfx contracts; no bound reflection/HP/receiver/Main/game/MySQL qualification");
 }
}
