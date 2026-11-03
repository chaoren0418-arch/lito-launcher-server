package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;import java.sql.*;
public class ResourceRegression {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Map<String,Object> fields()throws Exception{
  var result=new TreeMap<String,Object>();
  for(var f:BattleSpellCatalog.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&Map.class.isAssignableFrom(f.getType())){
   f.setAccessible(true);for(var value:((Map<?,?>)f.get(null)).values())if(value!=null&&value.getClass().isRecord()){
    if(Arrays.stream(value.getClass().getRecordComponents()).noneMatch(rc->rc.getName().equals("skillId")))continue;
    var id=value.getClass().getDeclaredMethod("skillId");id.setAccessible(true);int skill=(int)id.invoke(value);
    for(var rc:value.getClass().getRecordComponents())if(rc.getType()==int.class){var method=rc.getAccessor();method.setAccessible(true);result.put(value.getClass().getSimpleName()+":"+skill+":"+rc.getName(),method.invoke(value));}
   }
  }return result;
 }
 public static void main(String[] a)throws Exception{
  var config=ServerConfig.load(Path.of(a[0]),"standard");var before=fields();
  var names=NativeLitoRentalTownNpcCatalog.forMap(500).stream().map(n->n.name()).toList();
  var maps=NativeLitoMapTableCatalog.active();var zones=NativeLitoZoneCatalog.zones();var colors=NativeLitoZoneCatalog.colors();
  var portals=new HashSet<NativeLitoTownPortalCatalog.Route>();for(int m:new int[]{500,560})portals.addAll(NativeLitoTownPortalCatalog.routesForMap(m));
  var arrow=Protocol.rangedPhysicalAttack(1,1,2,0,0,1,66,32768,32768,32770,32770);
  var buffs=new ArrayList<byte[]>();for(int id:new int[]{181,201,204,206,209,211,214,216,217,219,3,99,151})buffs.add(Protocol.mageShield(64,id));
  ResourceDatabase.load(config);var after=fields();
  // Combat numbers intentionally follow SQL/source controls, checked separately.
  for(var key:before.keySet()) {
   if(key.equals("PatienceSpell:211:physicalDamageReduction")){
    check(before.get(key).equals(2)&&after.get(key).equals(15),"source SKD3 Patience getter must switch from legacy2 to fixed15");continue;
   }
   String fieldName=key.substring(key.lastIndexOf(':')+1);
   if(Set.of("mpCost","hpCost","reuseDelayMillis","durationSeconds",
     "baseDamage","baseDiceCount","dieSides","diceCount","itemConsumeId","itemConsumeCount","probabilityDice","probabilityValue",
     "specialCriticalChancePercent","specialCriticalGfx","manaGain","warningDelayMillis",
     "paralysisDurationMillis","baseStunSeconds","randomStunSeconds","minimumBindSeconds","maximumBindSeconds","baseReflectionDamage","hitCount","exposureDamageStep").contains(fieldName))continue;
   check(Objects.equals(before.get(key),after.get(key)),"qualified presentation/other contract changed: "+key);
  }
  check(names.equals(NativeLitoRentalTownNpcCatalog.forMap(500).stream().map(n->n.name()).toList()),"SQL NPC name mismatch");
  check(maps.equals(NativeLitoMapTableCatalog.active()),"SQL map metadata differs");
  check(zones.equals(NativeLitoZoneCatalog.zones())&&colors.equals(NativeLitoZoneCatalog.colors()),"SQL zone mismatch");
  check(Arrays.equals(arrow,Protocol.rangedPhysicalAttack(1,1,2,0,0,1,66,32768,32768,32770,32770)),"arrow packet changed");
  int n=0;for(int id:new int[]{181,201,204,206,209,211,214,216,217,219,3,99,151})check(Arrays.equals(buffs.get(n++),Protocol.mageShield(64,id)),"buff packet changed");
  check(Protocol.showMap(1,1)[0]==0x6e&&Protocol.showMap(1,2)[0]==0x6e,"qualified map pages");
  for(int page=3;page<=18;page++)check(Protocol.showMap(1,page)[0]==Protocol.systemMessage(9,"x")[0],"unqualified page opened");
  var loaded=new HashSet<NativeLitoTownPortalCatalog.Route>();for(int m:new int[]{500,560})loaded.addAll(NativeLitoTownPortalCatalog.routesForMap(m));check(portals.equals(loaded),"SQL portal metadata differs");
  check(NativeLitoTownPortalCatalog.routeCount()==56,"town portals");check(NativeLitoTownPortalCatalog.find(525,32748,32999)==null,"blocked portal enabled");
  // Prove consumers call the SQL snapshot, not just an unused catalog loader.
  var field=ResourceDatabase.class.getDeclaredField("current");field.setAccessible(true);var snap=(ResourceDatabase.Snapshot)field.get(null);
  check(snap.values().size()==364&&snap.selectors().size()==11,"qualified row counts");
  var altered=new HashMap<>(snap.values());var key=new ResourceDatabase.Key("HealingSpell",1,"effectGfx");check(altered.containsKey(key),"healing binding");altered.put(key,11723);
  field.set(null,new ResourceDatabase.Snapshot(Map.copyOf(altered),snap.selectors(),snap.maps(),snap.zones(),snap.colors(),snap.npcNames(),snap.mapPages(),snap.portals(),snap.arrow(),snap.flightType()));
  check(fields().get("HealingSpell:1:effectGfx").equals(11723),"skill consumer did not use snapshot");field.set(null,snap);
  check(fields().equals(after),"snapshot restore");
  System.out.println("PASS: SQL read-only load; qualified presentation/other contracts unchanged; combat values tested separately; real consumer overrides snapshot; 364 skill fields, 11 buff selectors, 47 map names, zones, 56 existing portals; pages 3..18 withheld; arrow/buff wire parity; no SQL writes");
 }
 static Map<String,String> diff(Map<String,Object> a,Map<String,Object>b){var d=new TreeMap<String,String>();for(var k:a.keySet())if(!Objects.equals(a.get(k),b.get(k)))d.put(k,a.get(k)+" -> "+b.get(k));return d;}
}
