package local.lito381.server;
import java.sql.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
/** Real shared SQL is read-only. Error and invalid-row cases are ResultSet views, never UPDATEs. */
public final class CancellationNpcLoadProbe {
 static int cases,checks,queries,lookups;
 static void need(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static Object invoke(Object o,Method m,Object[] a)throws Throwable{try{return m.invoke(o,a);}catch(InvocationTargetException e){throw e.getCause();}}
 static NativeCancellationNpcRules.Snapshot current()throws Exception{var f=NativeCancellationNpcRules.class.getDeclaredField("current");f.setAccessible(true);return (NativeCancellationNpcRules.Snapshot)f.get(null);}
 static Connection view(Connection real,int mode) {
  return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
   if(m.getName().equals("prepareStatement")) {
    need(((String)a[0]).startsWith("SELECT "),"only SELECT");queries++;
    if(mode==1)throw new SQLException("fixture query failure");
    var st=(PreparedStatement)invoke(real,m,a);
    return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(sp,sm,sa)->{
     if(sm.getName().equals("executeQuery")) {
      var rs=(ResultSet)invoke(st,sm,sa);int[] seen={0};
      return Proxy.newProxyInstance(ResultSet.class.getClassLoader(),new Class[]{ResultSet.class},(rp,rm,ra)->{
       String n=rm.getName();
       if(n.equals("next")){if(mode==2)return false;if(mode==4&&seen[0]++==1)throw new SQLException("fixture partial query failure");}
       if(n.equals("getInt")) {
        String col=(String)ra[0];
        if(col.equals("npc_id")&&mode==5)return 1;
        if(col.equals("duration_seconds")&&mode==3)return -1;
        if(col.equals("transform_type")&&mode==7)return 3;
        if(col.equals("target_id")&&mode==8)return 0;
        if(col.equals("transform_type")&&(mode==9||mode==10))return 2;
        if(col.equals("target_id")&&mode==9)return rs.getInt("npc_id");
       }
       if(n.equals("getObject")&&((mode==6&&ra[0].equals("source_exists"))||(mode==10&&ra[0].equals("target_exists"))))return null;
       if(n.equals("getString")&&ra[0].equals("new_name_id")&&mode==11)return "  $fixture  ";
       return invoke(rs,rm,ra);
      });
     }
     return invoke(st,sm,sa);
    });
   }
   return invoke(real,m,a);
  });
 }
 static ServerConfig mismatch(ServerConfig cfg)throws Exception {
  var parts=ServerConfig.class.getRecordComponents();var types=new Class<?>[parts.length];var values=new Object[parts.length];
  for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();values[i]=parts[i].getName().equals("legacySourceResourceDatabases")?List.of("fixture_wrong_static"):parts[i].getAccessor().invoke(cfg);}
  var ctor=ServerConfig.class.getDeclaredConstructor(types);ctor.setAccessible(true);return ctor.newInstance(values);
 }
 public static void main(String[] args)throws Exception {
  Path jar=Path.of(args[2]).toRealPath();
  for(var c:List.of(CombatDatabase.class,NativeCancellationNpcRules.class))need(Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate class");
  need(NativeCancellationNpcRules.lookup(71092)==null,"unloaded compatibility");
  var cfg=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(cfg);
  var original=current();need(original!=null&&!original.rows().isEmpty(),"real startup reader");
  NativeDeNpcTemplateResources.load(cfg);
  for(int id:List.of(71092,80013,81209,91123,45641)){var template=NativeDeNpcTemplateResources.snapshot().npcs().get(id);System.out.println("TRANSFORM_TEMPLATE id="+id+" impl="+template.getImpl()+" gfx="+template.get_gfxid()+" hp="+template.get_hp()+" size="+template.get_size());}
  ResourceDatabase.load(cfg);need(current()==original,"idempotent startup");
  var props=UnifiedConfiguration.read(cfg.source());String db=SharedDatabase.identifier(props.getProperty("database.static.database"));
  need(original.table().equals(db+".skills_相消怪物轉換"),"configured static source");
  for(int mode=0;mode<=11;mode++) {
   NativeCancellationNpcRules.Snapshot prepared=null;boolean failed=false;
   try(var c=DriverManager.getConnection(props.getProperty("database.jdbc.base.url")+db+props.getProperty("database.jdbc.options"),props.getProperty("database.user"),props.getProperty("database.password"))) {
    c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    try{prepared=NativeCancellationNpcRules.read(view(c,mode));}catch(SQLException expected){failed=true;}finally{c.rollback();}
   }
   need(failed==(mode==1||mode==4||mode==5),"query fault mode "+mode);need(current()==original,"no partial publication");
   if(!failed)try {
    NativeCancellationNpcRules.publish(prepared);
    if(mode==0)need(prepared.equals(original),"real SQL snapshot stable");
    if(mode==2)need(prepared.rows().isEmpty()&&prepared.invalid().isEmpty(),"empty rules allowed");
    if(Set.of(3,6,7,8,9,10).contains(mode))need(prepared.rows().isEmpty()&&prepared.invalid().size()==original.rows().size()+original.invalid().size(),"invalid rules quarantined");
    if(mode==11)for(var rule:prepared.rows().values())need(rule.nameId().equals("$fixture"),"source trim");
   }finally{NativeCancellationNpcRules.publish(original);}
   cases++;
  }
  try(var c=DriverManager.getConnection(props.getProperty("database.jdbc.base.url")+db+props.getProperty("database.jdbc.options"),props.getProperty("database.user"),props.getProperty("database.password"))) {
   c.setReadOnly(true);c.setAutoCommit(false);int before=queries;boolean rejected=false;
   try{NativeCancellationNpcRules.read(view(c,0),mismatch(cfg));}catch(SQLException expected){rejected=true;}finally{c.rollback();}
   need(rejected&&queries==before,"mismatched source rejected before SQL");need(current()==original,"source mismatch retains snapshot");cases++;
  }
  int before=queries;
  for(int i=0;i<10000;i++)for(var rule:original.rows().values()){need(NativeCancellationNpcRules.lookup(rule.npcId())==rule,"exact immutable lookup");lookups++;}
  need(queries==before,"no cast lookup SQL");need(NativeCancellationNpcRules.lookup(Integer.MAX_VALUE)==null,"unconfigured NPC fallback");
  boolean immutable=false;try{original.rows().clear();}catch(UnsupportedOperationException expected){immutable=true;}need(immutable,"immutable rows");cases++;
  var r=new NativeCancellationNpcRules.Rule(1,1,2,0,null,0);need(r.nameId().isEmpty(),"source null name");
  need(NativeCancellationNpcRules.invalidReason(r,true,false)==null,"GFX is not an NPC id");
  need(!r.changesAppearance(1,2)&&!r.changesAppearance(1,3)&&r.changesAppearance(1,1),"source original-gfx gate");
  need(!new NativeCancellationNpcRules.Rule(1,2,2,1,"",0).changesAppearance(1,1),"type2 not appearance");cases++;
  Files.writeString(Path.of(args[1]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"sql_queries_observed\":"+queries+",\"memory_lookups\":"+lookups+",\"valid_sql_rows\":"+original.rows().size()+",\"invalid_sql_rows\":"+original.invalid().size()+",\"source_database_mutated\":false,\"runtime_consumers_complete\":false}\n");
 }
}
