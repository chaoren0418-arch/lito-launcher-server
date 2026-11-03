package local.lito381.server;
import java.sql.*;import java.util.*;
/** Read-only shared weights and selected polymorph metadata, published only after startup validation. */
final class NativePolymorphSource {
 record Snapshot(List<NativePolymorphWeights.Row> weights,Map<Integer,BattlePolymorphCatalog.Form> forms){
  Snapshot{weights=List.copyOf(weights);forms=Map.copyOf(forms);}
 }
 private record Row(String name,int gfx,int level,int weapons,int armor,boolean skills,int cause){}
 private static volatile Snapshot current;
 static Snapshot read(Connection connection)throws SQLException{
  var weights=new ArrayList<NativePolymorphWeights.Row>();
  // Preserve Source's SQL order, including equal-weight rows; never renormalize.
  try(var statement=connection.prepareStatement("SELECT * FROM `道具_變形控制魔杖` WHERE `chance` > 0 ORDER BY `chance` ASC");var result=statement.executeQuery()){
   while(result.next())weights.add(new NativePolymorphWeights.Row(result.getInt("poly_id"),result.getInt("chance")));
  }
  var selected=new HashSet<Integer>();selected.add(29);for(var row:weights)selected.add(row.polyId());
  var rows=new HashMap<Integer,Row>();
  // PolyTable uses the last row encountered for a repeated polyid.
  try(var statement=connection.prepareStatement("SELECT * FROM polymorphs");var result=statement.executeQuery()){
   while(result.next()){
    int gfx=result.getInt("polyid");if(!selected.contains(gfx))continue;
    rows.put(gfx,new Row(result.getString("name"),gfx,result.getInt("minlevel"),result.getInt("weaponequip"),result.getInt("armorequip"),result.getBoolean("isSkillUse"),result.getInt("cause")));
   }
  }
  var forms=new HashMap<Integer,BattlePolymorphCatalog.Form>();
  for(int gfx:selected){
   var row=rows.get(gfx);if(row==null)throw new SQLException("polymorphs: missing random/fallback polyid="+gfx);
   // Reject unsupported native causes before opening game listeners; never silently apply an incompatible form.
   if((row.cause&1)==0)throw new SQLException("polymorphs: random polyid="+gfx+" excludes item/magic cause; native no-change response requires qualification");
   try{forms.put(gfx,new BattlePolymorphCatalog.Form(row.name,row.gfx,Math.max(1,row.level),row.weapons,row.armor,row.skills,row.cause,false,0));}
   catch(RuntimeException failure){throw new SQLException("polymorphs: random/fallback polyid="+gfx+" exceeds native metadata/animation contract",failure);}
  }
  return new Snapshot(weights,forms);
 }
 static void publish(Snapshot prepared){current=Objects.requireNonNull(prepared);}
 static BattlePolymorphCatalog.Form selectOrNull(int roll){
  var data=current;if(data==null)return null;
  return data.forms.get(NativePolymorphWeights.select(data.weights,roll));
 }
 private NativePolymorphSource(){}
}
