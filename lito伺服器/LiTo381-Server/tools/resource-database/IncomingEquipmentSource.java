package local.lito381.server;
import java.nio.file.*;
import java.util.*;
/** Exact read-only shared weapon rows; only instance state and evaluations are fixtures. */
final class IncomingEquipmentSource {
 record Template(Map<String,String> row,ClassicWorldDropCatalog.ItemWire wire){
  int integer(String key){return Integer.parseInt(row.get(key));}
  SourceInventoryItem source(long object){return new SourceInventoryItem(object,integer("item_id"),1,false,0,0,true,integer("bless"),false,wire);}
  SourceWeaponPhysicalStats physical()throws Exception{
   var field=LegacyEquipmentMagicLoader.class.getDeclaredField("WEAPON_TYPES");field.setAccessible(true);var types=(Map<String,Integer>)field.get(null);Integer type=types.get(row.get("type"));if(type==null)throw new AssertionError("native source weapon type mapping absent");
   return new SourceWeaponPhysicalStats(integer("item_id"),type,integer("dmg_small"),integer("dmg_large"),integer("range"),integer("hitmodifier"),integer("dmgmodifier"),0,0).withNameId(row.get("name_id"));
  }
 }
 static Map<Integer,Template> load(Path file)throws Exception{
  var lines=Files.readAllLines(file);String[] header=lines.get(0).split("\t");var result=new LinkedHashMap<Integer,Template>();
  for(String line:lines.subList(1,lines.size())){String[] values=line.split("\t",-1);if(values.length!=header.length)throw new AssertionError("shared template columns");var row=new LinkedHashMap<String,String>();for(int i=0;i<header.length;i++)row.put(header[i],values[i]);int id=Integer.parseInt(row.get("item_id"));if(id!=145&&id!=149)throw new AssertionError("unexpected shared weapon id");int mask=0;String[] classes={"royal","knight","elf","mage","darkelf","dragonknight","illusionist"};for(int type=0;type<7;type++)if(Integer.parseInt(row.get("use_"+classes[type]))!=0)mask|=1<<type;
   var wire=new ClassicWorldDropCatalog.ItemWire(id,"weapon",row.get("name_id"),"weapon",Integer.parseInt(row.get("invgfx")),Integer.parseInt(row.get("grdgfx")),Integer.parseInt(row.get("bless")),false,Integer.parseInt(row.get("trade"))==0,Integer.parseInt(row.get("weight")),Integer.parseInt(row.get("itemdesc_id")),row.get("type"),Integer.parseInt(row.get("dmg_small")),Integer.parseInt(row.get("dmg_large")),row.get("material"),Integer.parseInt(row.get("safenchant")),mask,Integer.parseInt(row.get("cant_delete"))!=0);
   var template=new Template(Map.copyOf(row),wire);if(result.putIfAbsent(id,template)!=null)throw new AssertionError("shared static templates must have one authoritative database per id");
  }
  if(!result.keySet().equals(Set.of(145,149)))throw new AssertionError("shared weapon145/149 rows missing");return Map.copyOf(result);
 }
}
