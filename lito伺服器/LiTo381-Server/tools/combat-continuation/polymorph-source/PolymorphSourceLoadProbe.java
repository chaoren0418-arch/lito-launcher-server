package local.lito381.server;
import java.sql.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
/** Actual shared SQL is read-only. Fault/empty views affect this connection's result objects only. */
public final class PolymorphSourceLoadProbe {
 static int checks,cases,rolls;
 static void need(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static Object published()throws Exception{var f=NativePolymorphSource.class.getDeclaredField("current");f.setAccessible(true);return f.get(null);}
 static Object invoke(Object target,Method method,Object[]args)throws Throwable{try{return method.invoke(target,args);}catch(InvocationTargetException failure){throw failure.getCause();}}
 static Connection view(Connection real,int mode){return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
  if(method.getName().equals("prepareStatement")){
   String sql=(String)args[0];boolean weights=sql.contains("道具_變形控制魔杖");need(sql.startsWith("SELECT "),"read-only query");
   if(mode==1&&weights||mode==2&&!weights)throw new SQLException("fixture prepare failure "+mode);
   PreparedStatement statement=(PreparedStatement)invoke(real,method,args);
   return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,m,a)->{
    if(m.getName().equals("executeQuery")){
     var result=(ResultSet)invoke(statement,m,a);if(!weights||mode<3)return result;int[]seen={0};
     return Proxy.newProxyInstance(ResultSet.class.getClassLoader(),new Class[]{ResultSet.class},(rp,rm,ra)->{
      if(rm.getName().equals("next")){if(mode==3)return false;if(mode==4&&seen[0]++>0)return false;}
      if(rm.getName().equals("getInt")&&"chance".equals(ra[0])&&mode==4)return 1;
      if(rm.getName().equals("getInt")&&"poly_id".equals(ra[0])&&mode==5)return 999999;
      return invoke(result,rm,ra);
     });
    }return invoke(statement,m,a);
   });
  }return invoke(real,method,args);
 });}
 public static void main(String[]args)throws Exception{
  var jar=Path.of(args[2]).toRealPath();for(var type:List.of(CombatDatabase.class,BattlePolymorphCatalog.class,NativePolymorphSource.class,NativePolymorphWeights.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate consumers");
  var cfg=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(cfg);var snapshot=(NativePolymorphSource.Snapshot)published();need(snapshot!=null,"actual CombatDatabase load published shared snapshot");
  ResourceDatabase.load(cfg);need(published()==snapshot,"repeat startup does not replace immutable snapshot");
  var boundaries=new TreeSet<Integer>(Set.of(1,1000000));int sum=0;for(var row:snapshot.weights()){sum+=row.chance();for(int n:new int[]{sum-1,sum,sum+1})if(n>=1&&n<=1000000)boundaries.add(n);}
  var random=new Random(0x67145);for(int i=0;i<1000;i++)boundaries.add(1+random.nextInt(1000000));
  for(int roll:boundaries){int expected=SourceWandPolyReference.select(snapshot.weights(),roll);need(BattlePolymorphCatalog.randomForm(roll).gfx()==expected,"actual selection follows Source SQL snapshot");rolls++;}
  var p=UnifiedConfiguration.read(cfg.source());String database=SharedDatabase.identifier(p.getProperty("database.static.database"));
  for(int mode=0;mode<6;mode++){
   NativePolymorphSource.Snapshot prepared=null;boolean failed=false;
   try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+database+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){
    c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    try{prepared=NativePolymorphSource.read(view(c,mode));}catch(SQLException expected){failed=true;}finally{c.rollback();}
   }
   need(failed==(mode==1||mode==2||mode==5),"bounded SQL/metadata failure mode="+mode);need(published()==snapshot,"preparation cannot partially publish mode="+mode);
   if(!failed){
    need(prepared.forms().containsKey(29),"fallback native resource validated");
    try{NativePolymorphSource.publish(prepared);
     if(mode==3){need(prepared.weights().isEmpty(),"empty query result");need(BattlePolymorphCatalog.randomForm(1).gfx()==29&&BattlePolymorphCatalog.randomForm(1000000).gfx()==29,"empty Source table falls back to skeleton");}
     if(mode==4){need(prepared.weights().size()==1,"partial weighting view");need(BattlePolymorphCatalog.randomForm(1).gfx()==prepared.weights().get(0).polyId()&&BattlePolymorphCatalog.randomForm(2).gfx()==29,"unassigned probability falls back without normalization");}
    }finally{NativePolymorphSource.publish(snapshot);}
   }cases++;
  }
  need(published()==snapshot,"temporary test generations released");Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"actual_sql_weight_rows\":"+snapshot.weights().size()+",\"native_rolls\":"+rolls+",\"source_database_mutated\":false,\"full_goal_achieved\":false}\n");
 }
}
