package local.lito381.server;
import java.sql.*;import java.lang.reflect.*;import java.util.*;
/** Fake-JDBC schema/atomic-publication scenarios: no driver, real connection, player or Hub initialization. */
public final class SourceDollSkillsPlan {
 static int cases;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);cases++;}
 static Integer[] row(int id,int level){return new Integer[]{id,level,0,900+id,0,64,0,0,0};}
 static final class Fake implements InvocationHandler {
  final List<Integer[]> data;final boolean readonly,autocommit;int index=-1,queries,closed;boolean wasNull;
  Fake(List<Integer[]> data,boolean readonly,boolean autocommit){this.data=data;this.readonly=readonly;this.autocommit=autocommit;}
  Object proxy(Class<?> type){return Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{type},this);}
  public Object invoke(Object proxy,Method method,Object[] args)throws Throwable{return switch(method.getName()){
   case "isReadOnly"->readonly;case "getAutoCommit"->autocommit;case "createStatement"->proxy(Statement.class);
   case "executeQuery"->{if(!NativeSourceDollSkillData.SELECT.equals(args[0]))throw new AssertionError("unreviewed SQL");queries++;yield proxy(ResultSet.class);}
   case "next"->++index<data.size();case "getInt"->{Integer value=data.get(index)[(int)args[0]-1];wasNull=value==null;yield value==null?0:value;}
   case "wasNull"->wasNull;case "close"->{closed++;yield null;}default->throw new AssertionError("unowned JDBC authority: "+method.getName());};}
  void read()throws SQLException{NativeSourceDollSkillData.read((Connection)proxy(Connection.class));}
 }
 static void rejected(Fake fake,String name){try{fake.read();throw new AssertionError(name);}catch(SQLException expected){cases++;}}
 public static void main(String[]args)throws Exception{
  var valid=new Fake(Arrays.asList(row(29,4),row(74,10),row(205,0)),true,false);valid.read();check(valid.queries==1&&valid.closed==2,"one SELECT and owned statement/result only");
  check(NativeSourceDollSkillData.row(29).gfx()==929&&NativeSourceDollSkillData.row(29).duration()==64&&NativeSourceDollSkillData.row(74).level()==10,"actual skill level/gfx/duration not catalog arithmetic");
  check(NativeSourceDollSkillData.level(205)==1,"Source level0 retains <=6 critical eligibility");
  var writable=new Fake(Collections.singletonList(row(29,4)),false,false);rejected(writable,"writable transaction rejected");if(writable.queries!=0)throw new AssertionError();
  var auto=new Fake(Collections.singletonList(row(29,4)),true,true);rejected(auto,"autocommit rejected");if(auto.queries!=0)throw new AssertionError();
  rejected(new Fake(Arrays.asList(row(29,4),row(29,7)),true,false),"duplicate IDs");
  rejected(new Fake(List.of(),true,false),"empty snapshot");
  var nullCell=row(29,4);nullCell[3]=null;rejected(new Fake(Collections.singletonList(nullCell),true,false),"null scalar");
  var negative=row(29,-1);rejected(new Fake(Collections.singletonList(negative),true,false),"invalid Source metadata");
  var invalidDice=row(29,4);invalidDice[8]=1;rejected(new Fake(Collections.singletonList(invalidDice),true,false),"positive dice count requires die sides");
  check(NativeSourceDollSkillData.row(29).level()==4&&NativeSourceDollSkillData.row(74).gfx()==974,"failed reload never publishes partial replacement");
  try{NativeSourceDollSkillData.row(999);throw new AssertionError();}catch(IllegalStateException expected){cases++;}
  var status=new BattleTimedStatus();var slow=NativeSourceDollSkills.slowChange(status,1000000000L,10);check(slow.type()==BattleTimedStatus.SLOW,"normal speed gains slow");
  check(NativeSourceDollSkills.slowChange(status,5000000000L,10)==null&&status.speed(5000000000L).remainingSeconds()==6,"already slow keeps original expiry");
  var haste=new BattleTimedStatus();haste.activateHaste(1000000000L,20,43);var cancelled=NativeSourceDollSkills.slowChange(haste,2000000000L,10);check(cancelled.type()==BattleTimedStatus.NORMAL&&cancelled.stoppedSkillId()==43&&haste.speed(3000000000L).type()==BattleTimedStatus.NORMAL,"haste is cancelled without adding slow");
  check(NativeSourceDollSkills.procVisible(true,10,10,0,0),"Source proc target radius10 boundary");check(!NativeSourceDollSkills.procVisible(true,11,0,0,0),"outside Source proc radius");check(!NativeSourceDollSkills.procVisible(false,0,0,0,0),"proc preference disabled");
  if(cases!=18)throw new AssertionError(cases);System.out.println("PASS18 pure readonly metadata/atomic-publication/Source-slow/proc-view scenarios, no real connection/player/Hub/socket");
 }
}
