package local.lito381.server;
import java.sql.*;import java.nio.file.*;import java.util.*;
/** Diagnostic only: explain existing record rejections, without changing Source rows. */
public final class NpcPhysicalInvalidAudit {
 static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
 static String json(Map<String,Integer> m){var s=new StringJoiner(",","{","}");m.forEach((k,v)->s.add(q(k)+":"+v));return s.toString();}
 public static void main(String[]args)throws Exception{
  var cfg=ServerConfig.load(Path.of(args[0]),"standard");var p=UnifiedConfiguration.read(cfg.source());String db=SharedDatabase.identifier(p.getProperty("database.static.database"));
  var sizes=new TreeMap<String,Integer>();var types=new TreeMap<String,Integer>();var bounds=new TreeMap<String,Integer>();int total=0,invalid=0;
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
   try{var snapshot=NativeNpcPhysicalSource.read(c,cfg);invalid=snapshot.invalid().size();
    try(var st=c.prepareStatement("SELECT npcid,size,impl,damage_reduction,undead,lawful,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk,weakAttr FROM npc");var row=st.executeQuery()){
     while(row.next()){total++;if(!snapshot.invalid().contains(row.getInt("npcid")))continue;
      sizes.merge(String.valueOf(row.getString("size")),1,Integer::sum);types.merge(String.valueOf(row.getString("impl")),1,Integer::sum);
      String size=row.getString("size");if(size==null||!Set.of("small","large").contains(size.trim().toLowerCase(Locale.ROOT)))bounds.merge("unsupported_size",1,Integer::sum);
      for(String field:List.of("damage_reduction","undead","mp","str","dex","ranged","bowActId","poison_atk","paralysis_atk"))if(row.getInt(field)<0)bounds.merge(field+"_negative",1,Integer::sum);
      int lawful=row.getInt("lawful");if(lawful<Short.MIN_VALUE||lawful>Short.MAX_VALUE)bounds.merge("lawful_outside_signed_short",1,Integer::sum);
      if(row.getInt("bowActId")>65535)bounds.merge("bowActId_above_unsigned_short",1,Integer::sum);
      if(!Set.of(-8,-4,-2,-1,0,1,2,4,8).contains(row.getInt("weakAttr")))bounds.merge("weakAttr_not_supported",1,Integer::sum);
      if(row.getInt("npcid")<=0)bounds.merge("npcid_nonpositive",1,Integer::sum);
     }
    }
   }finally{c.rollback();}
  }
  Files.writeString(Path.of(args[1]),"{\"status\":\"diagnostic-only-existing-record-rejections\",\"rows\":"+total+",\"invalid\":"+invalid+",\"sizes\":"+json(sizes)+",\"implementation_types\":"+json(types)+",\"validation_causes\":"+json(bounds)+",\"source_database_mutated\":false}\n");
 }
}
