package local.lito381.server;
import java.sql.*;import java.util.*;

/** Immutable, qualified resource metadata. Loaded once; no SQL on combat threads. */
final class ResourceDatabase {
 record Key(String type,int skill,String field) {}
 record Snapshot(Map<Key,Integer> values,Map<Integer,Integer> selectors,
   Map<Integer,NativeLitoMapTableCatalog.Entry> maps,List<NativeLitoZoneCatalog.Zone> zones,
   List<NativeLitoZoneCatalog.Color> colors,Map<Integer,String> npcNames,Set<Integer> mapPages,
   Map<String,NativeLitoTownPortalCatalog.Route> portals,int arrow,int flightType) {}
 private static volatile Snapshot current;
 static boolean shared(){return "tw13081901-v1".equals(System.getProperty("lito.resourceProfile"));}
 static synchronized void load(ServerConfig config)throws Exception {
  NativeProcNpcCatalog.load(config);
  NativeProcEncounterCatalog.load(config);
  NativeProcSkillCatalog.load(config);
  NativeProcHealBonus.load(config);
  NativeArmorProc.load(config);
  if(current!=null)return;
  var p=UnifiedConfiguration.read(config.source());
  String db=SharedDatabase.identifier(p.getProperty("database.lito.database"));
  var values=new HashMap<Key,Integer>();var selectors=new HashMap<Integer,Integer>();
  var maps=new LinkedHashMap<Integer,NativeLitoMapTableCatalog.Entry>();
  var zones=new ArrayList<NativeLitoZoneCatalog.Zone>();var colors=new ArrayList<NativeLitoZoneCatalog.Color>();
  var names=new HashMap<Integer,String>();var pages=new HashSet<Integer>();var portals=new HashMap<String,NativeLitoTownPortalCatalog.Route>();
  int arrow=66,type=127;
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"));var s=c.createStatement()){
   c.setReadOnly(true);
   try(var r=s.executeQuery("SELECT qualified_version FROM lito_resource_release WHERE release_id=1")){
    if(!r.next()||!"0.6.61".equals(r.getString(1)))throw new SQLException("Unqualified resource release");
   }
   // The formal schema contains runtime resources and one consolidated notes table.
   // Source/provenance and research rows are archived, never runtime dependencies.
   var expectedTables=Set.of("skill_value","buff","projectile","map","zone","portal","npc_identity","map_page","release","pending");
   var actualTables=new HashSet<String>();
   try(var r=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND LEFT(table_name,14)='lito_resource_'")){
    while(r.next())actualTables.add(r.getString(1).substring(14));
   }
   if(!actualTables.equals(expectedTables))throw new SQLException("LiTo compact resource schema requires exactly 10 tables; run the explicit archived migration");
   try(var r=s.executeQuery("SELECT category,source_table,record_key,evidence_status,evidence_note,archive_ref FROM lito_resource_pending LIMIT 0")){}
   try(var r=s.executeQuery("SELECT record_type,skill_id,field_name,value FROM lito_resource_skill_value WHERE enabled=1 AND evidence_status='qualified'")){
    while(r.next()){
     String field=r.getString(3);int value=r.getInt(4);
     if(value<0 || value>=(field.equals("actionId")?256:30285))throw new SQLException("Resource field out of range");
     values.put(new Key(r.getString(1),r.getInt(2),field),value);
    }
   }
   try(var r=s.executeQuery("SELECT skill_id,packet_selector,packet_opcode FROM lito_resource_buff WHERE enabled=1 AND semantic_conflict=0 AND evidence_status='qualified'")){
    while(r.next()){int selector=r.getInt(2);if(selector<0||selector>255||r.getInt(3)!=109)throw new SQLException("Unsupported buff wire format");selectors.put(r.getInt(1),selector);}
   }
   try(var r=s.executeQuery("SELECT map_id,raw_slot,category,name FROM lito_resource_map WHERE enabled=1 AND evidence_status='qualified' ORDER BY raw_slot")){
    while(r.next())maps.put(r.getInt(1),new NativeLitoMapTableCatalog.Entry(r.getInt(2),r.getInt(1),r.getString(3),r.getString(4)));
   }
   try(var r=s.executeQuery("SELECT zone_key,color_index,map_id,min_x,min_y,max_x,max_y,red,green,blue FROM lito_resource_zone WHERE enabled=1 AND evidence_status='qualified' ORDER BY color_index")){
    while(r.next()){zones.add(new NativeLitoZoneCatalog.Zone(r.getString(1),r.getInt(2),r.getInt(3),r.getInt(4),r.getInt(5),r.getInt(6),r.getInt(7)));colors.add(new NativeLitoZoneCatalog.Color(r.getInt(2),r.getInt(8),r.getInt(9),r.getInt(10)));}
   }
   try(var r=s.executeQuery("SELECT resource_index,description FROM lito_resource_npc_identity WHERE enabled=1 AND evidence_status='qualified'")){while(r.next())names.put(r.getInt(1),r.getString(2));}
   try(var r=s.executeQuery("SELECT wire_page FROM lito_resource_map_page WHERE enabled=1 AND evidence_status='qualified'")){while(r.next())pages.add(r.getInt(1));}
   try(var r=s.executeQuery("SELECT source_map_id,source_x,source_y,target_map_id,target_x,target_y,raw_tag FROM lito_resource_portal WHERE enabled=1 AND evidence_status='qualified-existing-route'")){
    while(r.next()){
     int m=r.getInt(1),x=r.getInt(2),y=r.getInt(3),target=r.getInt(4);
     if((m!=500&&m!=560)||m!=target||r.getInt(7)!=100)throw new SQLException("Portal semantics not qualified");
     var existing=NativeLitoTownPortalCatalog.find(m,x,y);
     if(existing==null||existing.targetMapId()!=target||existing.target().x()!=r.getInt(5)||existing.target().y()!=r.getInt(6))throw new SQLException("Portal differs from qualified baseline");
     portals.put(m+":"+x+":"+y,new NativeLitoTownPortalCatalog.Route(m,new NativeLitoTownPortalCatalog.Point(x,y),target,new NativeLitoTownPortalCatalog.Point(r.getInt(5),r.getInt(6)),existing.slotName(),100));
    }
   }
   try(var r=s.executeQuery("SELECT projectile_gfx,flight_type,packet_opcode FROM lito_resource_projectile WHERE projectile_key='physical-arrow' AND enabled=1 AND evidence_status='qualified'")){
    if(!r.next())throw new SQLException("Missing qualified physical arrow");arrow=r.getInt(1);type=r.getInt(2);
    if(arrow!=66||type!=127||r.getInt(3)!=35)throw new SQLException("Physical arrow resource/format not qualified");
   }
  }
  GmTravel.load(config);
  CombatDatabase.load(config);
  ConsumableItemParameters.load(config); // T111: never throws; falls back to built-in values.
  NativeDeSprites.load(config);
  NativeDeFactoryControls.load(config);
  NativeDeNpcTemplateResources.load(config);
  NativeDeTerrainResources.load(config);
  NativeDeShopResources.load(config);
  NativeDeChatResources.load(config);
  current=new Snapshot(Map.copyOf(values),Map.copyOf(selectors),Collections.unmodifiableMap(maps),List.copyOf(zones),List.copyOf(colors),Map.copyOf(names),Set.copyOf(pages),Map.copyOf(portals),arrow,type);
  System.out.println("[LiTo] SQL resources loaded: skill-fields="+values.size()+" buffs="+selectors.size()+" maps="+maps.size()+" town-portals="+portals.size()+" npc-identities="+names.size()+"; unresolved fields remain disabled");
 }
 static int value(int fallback,int skill,String type,String field){
  // Numeric source fields never consult presentation mappings or allocate a Key.
  if(CombatDatabase.bound(field))return CombatDatabase.value(fallback,skill,type,field);
  var s=current;if(s==null||!shared())return CombatRules.intField(fallback,skill,field);
  if(field.equals("iconType")||field.equals("legacyIconType"))return s.selectors().getOrDefault(skill,fallback);
  return CombatRules.intField(s.values().getOrDefault(new Key(type,skill,field),fallback),skill,field);
 }
 static int arrow(int original){var s=current;return s!=null&&shared()&&original==66?s.arrow():original;}
 static int flightType(int original){var s=current;return s!=null&&shared()?s.flightType():original;}
 static List<NativeLitoMapTableCatalog.Entry> maps(List<NativeLitoMapTableCatalog.Entry> original){var s=current;return s==null?original:List.copyOf(s.maps().values());}
 static NativeLitoMapTableCatalog.Entry map(NativeLitoMapTableCatalog.Entry original,int id){var s=current;return s==null?original:s.maps().get(id);}
 static boolean mapActive(boolean original,int id){var s=current;return s==null?original:s.maps().containsKey(id);}
 static List<NativeLitoZoneCatalog.Zone> zones(List<NativeLitoZoneCatalog.Zone> original){var s=current;return s==null?original:s.zones();}
 static List<NativeLitoZoneCatalog.Color> colors(List<NativeLitoZoneCatalog.Color> original){var s=current;return s==null?original:s.colors();}
 static NativeLitoZoneCatalog.Zone zone(NativeLitoZoneCatalog.Zone original,int id){var s=current;return s==null?original:s.zones().stream().filter(z->z.mapId()==id).findFirst().orElse(null);}
 static String npcName(String original,int index){var s=current;return s==null?original:s.npcNames().getOrDefault(index,original);}
 static byte[] mapPage(byte[] packet,int page){var s=current;return s==null||s.mapPages().contains(page)?packet:Protocol.systemMessage(9,"此地圖頁面尚無原生圖資對應，暫不開放。");}
 static NativeLitoTownPortalCatalog.Route portal(NativeLitoTownPortalCatalog.Route original,int map,int x,int y){var s=current;return s==null?original:s.portals().get(map+":"+x+":"+y);}
 static List<NativeLitoTownPortalCatalog.Route> portals(List<NativeLitoTownPortalCatalog.Route> original,int map){var s=current;return s==null?original:s.portals().values().stream().filter(r->r.sourceMapId()==map).toList();}
 static int portalCount(int original){var s=current;return s==null?original:s.portals().size();}
}
