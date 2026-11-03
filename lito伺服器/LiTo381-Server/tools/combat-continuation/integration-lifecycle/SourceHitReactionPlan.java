package local.lito381.server;
import java.sql.*;import java.lang.reflect.*;import java.util.*;
/** JDBC contract proxies only: no DriverManager, database, Hub or ClientSession allocation. */
public final class SourceHitReactionPlan {
 static int cases;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);cases++;}
 static final class Jdbc implements InvocationHandler {
  boolean readonly=true,auto=false,prepareFailure,closeFailure;int failAt=-1,index=-1,prepares,reads,statementCloses,resultCloses;Integer[] values;Connection connection;
  Jdbc(Integer...v){values=v;connection=(Connection)Proxy.newProxyInstance(SourceHitReactionPlan.class.getClassLoader(),new Class<?>[]{Connection.class},this);}
  Object proxy(Class<?> type){return Proxy.newProxyInstance(SourceHitReactionPlan.class.getClassLoader(),new Class<?>[]{type},this);}
  public Object invoke(Object proxy,Method method,Object[]args)throws Throwable{
   return switch(method.getName()){
    case "isReadOnly"->readonly;case "getAutoCommit"->auto;
    case "prepareStatement"->{prepares++;if(!NativeSourceHitReaction.SELECT.equals(args[0])||args.length!=1)throw new AssertionError("fixed Source SELECT only");if(prepareFailure)throw new SQLException("fixture prepare failure");yield proxy(PreparedStatement.class);}
    case "executeQuery"->{if(args!=null&&args.length!=0)throw new AssertionError("prepared SELECT only");yield proxy(ResultSet.class);}
    case "next"->{if(index+1==failAt)throw new SQLException("fixture cursor failure");yield ++index<values.length;}
    case "getInt"->{if(!"gfx_id".equals(args[0]))throw new AssertionError("Source gfx_id column");reads++;yield values[index]==null?0:values[index];}
    case "close"->{if(proxy instanceof Connection)throw new AssertionError("borrowed connection cannot close");if(proxy instanceof PreparedStatement)statementCloses++;else resultCloses++;if(closeFailure)throw new SQLException("fixture close failure");yield null;}
    default->throw new AssertionError("unexpected JDBC operation "+method.getName());
   };
  }
 }
 static void denied(Jdbc jdbc)throws Exception{try{NativeSourceHitReaction.read(jdbc.connection);throw new AssertionError("must reject writable/autocommit");}catch(SQLException expected){if(jdbc.prepares!=0)throw new AssertionError("guard before SQL");}}
 public static void main(String[]a)throws Exception{
  check(!BattleHitReactionSuppressCatalog.shouldSendDamageAction(53)&&BattleHitReactionSuppressCatalog.shouldSendDamageAction(0),"original unloaded fallback");
  var writable=new Jdbc(222);writable.readonly=false;denied(writable);check(!BattleHitReactionSuppressCatalog.shouldSendDamageAction(53),"writable rejected without publication");
  var autocommit=new Jdbc(222);autocommit.auto=true;denied(autocommit);check(!BattleHitReactionSuppressCatalog.shouldSendDamageAction(53),"autocommit rejected without publication");
  var normal=new Jdbc(222,333);NativeSourceHitReaction.read(normal.connection);check(!BattleHitReactionSuppressCatalog.shouldSendDamageAction(222)&&BattleHitReactionSuppressCatalog.shouldSendDamageAction(53),"table replaces fixed snapshot");
  check(normal.prepares==1&&normal.reads==2&&normal.statementCloses==1&&normal.resultCloses==1,"owned SELECT closes resources without owning transaction");
  NativeSourceHitReaction.read(new Jdbc(9,9,10).connection);check(BattleHitReactionSuppressCatalog.suppressedGfx().equals(Set.of(9,10)),"Source duplicate set semantics");
  NativeSourceHitReaction.read(new Jdbc(0,-7,Integer.MAX_VALUE,Integer.MIN_VALUE).connection);check(!BattleHitReactionSuppressCatalog.shouldSendDamageAction(0)&&!BattleHitReactionSuppressCatalog.shouldSendDamageAction(-7)&&!BattleHitReactionSuppressCatalog.shouldSendDamageAction(Integer.MIN_VALUE)&&!BattleHitReactionSuppressCatalog.shouldSendDamageAction(Integer.MAX_VALUE),"Source int membership including nonpositive gfx");
  NativeSourceHitReaction.read(new Jdbc((Integer)null,19).connection);check(BattleHitReactionSuppressCatalog.suppressedGfx().equals(Set.of(0,19)),"Source getInt NULL is zero");
  var retained=BattleHitReactionSuppressCatalog.suppressedGfx();try{retained.add(24);throw new AssertionError("immutable");}catch(UnsupportedOperationException expected){}check(retained.equals(Set.of(0,19)),"immutable snapshot");
  NativeSourceHitReaction.read(new Jdbc().connection);check(BattleHitReactionSuppressCatalog.suppressedGfx().isEmpty()&&retained.equals(Set.of(0,19)),"empty table clears; prior snapshot detached");
  NativeSourceHitReaction.read(new Jdbc(9).connection);var failed=new Jdbc(27);failed.prepareFailure=true;NativeSourceHitReaction.read(failed.connection);check(BattleHitReactionSuppressCatalog.suppressedGfx().isEmpty(),"Source SQL failure clears stale control and continues");
  var partial=new Jdbc(41,42);partial.failAt=1;NativeSourceHitReaction.read(partial.connection);check(BattleHitReactionSuppressCatalog.suppressedGfx().equals(Set.of(41))&&partial.statementCloses==1&&partial.resultCloses==1,"Source partial cursor failure keeps read rows and closes");
  var close=new Jdbc(65);close.closeFailure=true;NativeSourceHitReaction.read(close.connection);check(BattleHitReactionSuppressCatalog.suppressedGfx().equals(Set.of(65))&&close.statementCloses==1&&close.resultCloses==1,"Source close exception retains read rows");
  denied(writable);denied(autocommit);check(BattleHitReactionSuppressCatalog.suppressedGfx().equals(Set.of(65)),"transaction guard failures do not replace loaded control");
  if(cases!=14)throw new AssertionError(cases);System.out.println("PASS14 hit reaction Source membership/failure and owned-readonly JDBC contracts; no actual database/game/lifecycle cases");
 }
}
