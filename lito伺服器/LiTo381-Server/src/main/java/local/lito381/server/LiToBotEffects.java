// ORIGINAL SOURCE moved from lito登入器/LiTo381-Launcher/server-patches/repair-032/LiToBotEffects.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.lang.reflect.*;import java.util.*;
/** Status lifetime follows the actual room generation, never the reusable wire id 2. */
final class LiToBotEffects {
 private static final Map<Object,State> states=Collections.synchronizedMap(new WeakHashMap<>());
 private static final Field ROOMS;
 static {try{ROOMS=GameHub.class.getDeclaredField("rooms");ROOMS.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 static final class State {long generation,blind,weakness,disease;int mp=100;State(long g){generation=g;}}
 static Object field(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new IllegalStateException(e);}}
 static State state(GameHub h,long id){synchronized(h){try{Object room=((Map<?,?>)ROOMS.get(h)).get(id);if(room==null || !(boolean)field(room,"battleStarted") || (int)field(room,"botHp")<=0)return null;
 long g=((Number)field(room,"battleGeneration")).longValue();synchronized(states){State old=states.get(room);if(old==null||old.generation!=g){old=new State(g);states.put(room,old);}return old;}
 }catch(IllegalAccessException e){throw new IllegalStateException(e);}}}
 static boolean eligible(GameHub h,long room){var mode=h.roomRules(room).mode();return h.roomCombatantCount(room)==1 && mode!=BattleRules.Mode.PET_RACE && mode!=BattleRules.Mode.QUEST_BATTLE && mode!=BattleRules.Mode.TIME_ATTACK && state(h,room)!=null;}
 static boolean apply(GameHub h,long room,int skill,long now,int seconds){State s=state(h,room);if(s==null)return false;long until=Math.addExact(now,Math.multiplyExact(seconds,1_000_000_000L));synchronized(s){if(skill==BattleSpellCatalog.WEAKNESS)s.weakness=until;else if(skill==BattleSpellCatalog.DISEASE)s.disease=until;else s.blind=until;}return true;}
 static boolean blind(GameHub h,long room,long now){State s=state(h,room);if(s==null)return false;synchronized(s){return now<s.blind;}}
 static int damageModifier(GameHub h,long room,long now){State s=state(h,room);if(s==null)return 0;synchronized(s){return (now<s.weakness?BattleSpellCatalog.weaknessSpell(BattleSpellCatalog.WEAKNESS).meleeDamageModifier():0)+(now<s.disease?BattleSpellCatalog.weaknessSpell(BattleSpellCatalog.DISEASE).meleeDamageModifier():0);}}
 static int mp(GameHub h,long room){State s=state(h,room);if(s==null)return 0;synchronized(s){return s.mp;}}
 static int drain(GameHub h,long room,int amount){State s=state(h,room);if(s==null)return 0;synchronized(s){int n=Math.min(s.mp,Math.max(0,amount));s.mp-=n;return n;}}
}
