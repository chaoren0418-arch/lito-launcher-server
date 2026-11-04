// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

final class ResourceDatabase {
   private static volatile ResourceDatabase.Snapshot current;

   static boolean shared() {
      return "tw13081901-v1".equals(System.getProperty("lito.resourceProfile"));
   }

   static synchronized void load(ServerConfig var0) throws Exception {
      NativeProcNpcCatalog.load(var0);
      NativeProcEncounterCatalog.load(var0);
      NativeProcSkillCatalog.load(var0);
      NativeProcHealBonus.load(var0);
      NativeArmorProc.load(var0);
      if (current == null) {
         Properties var1 = UnifiedConfiguration.read(var0.source());
         String var2 = SharedDatabase.identifier(var1.getProperty("database.lito.database"));
         HashMap var3 = new HashMap();
         HashMap var4 = new HashMap();
         LinkedHashMap var5 = new LinkedHashMap();
         ArrayList var6 = new ArrayList();
         ArrayList var7 = new ArrayList();
         HashMap var8 = new HashMap();
         HashSet var9 = new HashSet();
         HashMap var10 = new HashMap();
         int var11 = 66;
         int var12 = 127;

         try (
            Connection var13 = DriverManager.getConnection(
               var1.getProperty("database.jdbc.base.url") + var2 + var1.getProperty("database.jdbc.options"),
               var1.getProperty("database.user"),
               var1.getProperty("database.password")
            );
            Statement var14 = var13.createStatement();
         ) {
            var13.setReadOnly(true);

            try (ResultSet var15 = var14.executeQuery("SELECT qualified_version FROM lito_resource_release WHERE release_id=1")) {
               if (!var15.next() || !"0.6.61".equals(var15.getString(1))) {
                  throw new SQLException("Unqualified resource release");
               }
            }

            Set var49 = Set.of("skill_value", "buff", "projectile", "map", "zone", "portal", "npc_identity", "map_page", "release", "pending");
            HashSet var16 = new HashSet();

            try (ResultSet var17 = var14.executeQuery(
                  "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND LEFT(table_name,14)='lito_resource_'"
               )) {
               while (var17.next()) {
                  var16.add(var17.getString(1).substring(14));
               }
            }

            if (!var16.equals(var49)) {
               throw new SQLException("LiTo compact resource schema requires exactly 10 tables; run the explicit archived migration");
            }

            ResultSet var50 = var14.executeQuery(
               "SELECT category,source_table,record_key,evidence_status,evidence_note,archive_ref FROM lito_resource_pending LIMIT 0"
            );
            if (var50 != null) {
               var50.close();
            }

            try (ResultSet var51 = var14.executeQuery(
                  "SELECT record_type,skill_id,field_name,value FROM lito_resource_skill_value WHERE enabled=1 AND evidence_status='qualified'"
               )) {
               while (var51.next()) {
                  String var18 = var51.getString(3);
                  int var19 = var51.getInt(4);
                  if (var19 < 0 || var19 >= (var18.equals("actionId") ? 256 : 30285)) {
                     throw new SQLException("Resource field out of range");
                  }

                  var3.put(new ResourceDatabase.Key(var51.getString(1), var51.getInt(2), var18), var19);
               }
            }

            try (ResultSet var52 = var14.executeQuery(
                  "SELECT skill_id,packet_selector,packet_opcode FROM lito_resource_buff WHERE enabled=1 AND semantic_conflict=0 AND evidence_status='qualified'"
               )) {
               while (var52.next()) {
                  int var59 = var52.getInt(2);
                  if (var59 < 0 || var59 > 255 || var52.getInt(3) != 109) {
                     throw new SQLException("Unsupported buff wire format");
                  }

                  var4.put(var52.getInt(1), var59);
               }
            }

            try (ResultSet var53 = var14.executeQuery(
                  "SELECT map_id,raw_slot,category,name FROM lito_resource_map WHERE enabled=1 AND evidence_status='qualified' ORDER BY raw_slot"
               )) {
               while (var53.next()) {
                  var5.put(var53.getInt(1), new NativeLitoMapTableCatalog.Entry(var53.getInt(2), var53.getInt(1), var53.getString(3), var53.getString(4)));
               }
            }

            try (ResultSet var54 = var14.executeQuery(
                  "SELECT zone_key,color_index,map_id,min_x,min_y,max_x,max_y,red,green,blue FROM lito_resource_zone WHERE enabled=1 AND evidence_status='qualified' ORDER BY color_index"
               )) {
               while (var54.next()) {
                  var6.add(
                     new NativeLitoZoneCatalog.Zone(
                        var54.getString(1), var54.getInt(2), var54.getInt(3), var54.getInt(4), var54.getInt(5), var54.getInt(6), var54.getInt(7)
                     )
                  );
                  var7.add(new NativeLitoZoneCatalog.Color(var54.getInt(2), var54.getInt(8), var54.getInt(9), var54.getInt(10)));
               }
            }

            try (ResultSet var55 = var14.executeQuery(
                  "SELECT resource_index,description FROM lito_resource_npc_identity WHERE enabled=1 AND evidence_status='qualified'"
               )) {
               while (var55.next()) {
                  var8.put(var55.getInt(1), var55.getString(2));
               }
            }

            try (ResultSet var56 = var14.executeQuery("SELECT wire_page FROM lito_resource_map_page WHERE enabled=1 AND evidence_status='qualified'")) {
               while (var56.next()) {
                  var9.add(var56.getInt(1));
               }
            }

            try (ResultSet var57 = var14.executeQuery(
                  "SELECT source_map_id,source_x,source_y,target_map_id,target_x,target_y,raw_tag FROM lito_resource_portal WHERE enabled=1 AND evidence_status='qualified-existing-route'"
               )) {
               while (var57.next()) {
                  int var60 = var57.getInt(1);
                  int var61 = var57.getInt(2);
                  int var20 = var57.getInt(3);
                  int var21 = var57.getInt(4);
                  if (var60 != 500 && var60 != 560 || var60 != var21 || var57.getInt(7) != 100) {
                     throw new SQLException("Portal semantics not qualified");
                  }

                  NativeLitoTownPortalCatalog.Route var22 = NativeLitoTownPortalCatalog.find(var60, var61, var20);
                  if (var22 == null || var22.targetMapId() != var21 || var22.target().x() != var57.getInt(5) || var22.target().y() != var57.getInt(6)) {
                     throw new SQLException("Portal differs from qualified baseline");
                  }

                  var10.put(
                     var60 + ":" + var61 + ":" + var20,
                     new NativeLitoTownPortalCatalog.Route(
                        var60,
                        new NativeLitoTownPortalCatalog.Point(var61, var20),
                        var21,
                        new NativeLitoTownPortalCatalog.Point(var57.getInt(5), var57.getInt(6)),
                        var22.slotName(),
                        100
                     )
                  );
               }
            }

            try (ResultSet var58 = var14.executeQuery(
                  "SELECT projectile_gfx,flight_type,packet_opcode FROM lito_resource_projectile WHERE projectile_key='physical-arrow' AND enabled=1 AND evidence_status='qualified'"
               )) {
               if (!var58.next()) {
                  throw new SQLException("Missing qualified physical arrow");
               }

               var11 = var58.getInt(1);
               var12 = var58.getInt(2);
               if (var11 != 66 || var12 != 127 || var58.getInt(3) != 35) {
                  throw new SQLException("Physical arrow resource/format not qualified");
               }
            }
         }

         GmTravel.load(var0);
         CombatDatabase.load(var0);
         NativeDeSprites.load(var0);
         NativeDeFactoryControls.load(var0);
         NativeDeNpcTemplateResources.load(var0);
         NativeDeTerrainResources.load(var0);
         NativeDeShopResources.load(var0);
         NativeDeChatResources.load(var0);
         current = new ResourceDatabase.Snapshot(
            Map.copyOf(var3),
            Map.copyOf(var4),
            Collections.unmodifiableMap(var5),
            List.copyOf(var6),
            List.copyOf(var7),
            Map.copyOf(var8),
            Set.copyOf(var9),
            Map.copyOf(var10),
            var11,
            var12
         );
         System.out
            .println(
               "[LiTo] SQL resources loaded: skill-fields="
                  + var3.size()
                  + " buffs="
                  + var4.size()
                  + " maps="
                  + var5.size()
                  + " town-portals="
                  + var10.size()
                  + " npc-identities="
                  + var8.size()
                  + "; unresolved fields remain disabled"
            );
      }
   }

   static int value(int var0, int var1, String var2, String var3) {
      if (CombatDatabase.bound(var3)) {
         return CombatDatabase.value(var0, var1, var2, var3);
      } else {
         ResourceDatabase.Snapshot var4 = current;
         if (var4 == null || !shared()) {
            return CombatRules.intField(var0, var1, var3);
         } else {
            return !var3.equals("iconType") && !var3.equals("legacyIconType")
               ? CombatRules.intField(var4.values().getOrDefault(new ResourceDatabase.Key(var2, var1, var3), var0), var1, var3)
               : var4.selectors().getOrDefault(var1, var0);
         }
      }
   }

   static int arrow(int var0) {
      ResourceDatabase.Snapshot var1 = current;
      return var1 != null && shared() && var0 == 66 ? var1.arrow() : var0;
   }

   static int flightType(int var0) {
      ResourceDatabase.Snapshot var1 = current;
      return var1 != null && shared() ? var1.flightType() : var0;
   }

   static List<NativeLitoMapTableCatalog.Entry> maps(List<NativeLitoMapTableCatalog.Entry> var0) {
      ResourceDatabase.Snapshot var1 = current;
      return var1 == null ? var0 : List.copyOf(var1.maps().values());
   }

   static NativeLitoMapTableCatalog.Entry map(NativeLitoMapTableCatalog.Entry var0, int var1) {
      ResourceDatabase.Snapshot var2 = current;
      return var2 == null ? var0 : var2.maps().get(var1);
   }

   static boolean mapActive(boolean var0, int var1) {
      ResourceDatabase.Snapshot var2 = current;
      return var2 == null ? var0 : var2.maps().containsKey(var1);
   }

   static List<NativeLitoZoneCatalog.Zone> zones(List<NativeLitoZoneCatalog.Zone> var0) {
      ResourceDatabase.Snapshot var1 = current;
      return var1 == null ? var0 : var1.zones();
   }

   static List<NativeLitoZoneCatalog.Color> colors(List<NativeLitoZoneCatalog.Color> var0) {
      ResourceDatabase.Snapshot var1 = current;
      return var1 == null ? var0 : var1.colors();
   }

   static NativeLitoZoneCatalog.Zone zone(NativeLitoZoneCatalog.Zone var0, int var1) {
      ResourceDatabase.Snapshot var2 = current;
      return var2 == null ? var0 : var2.zones().stream().filter(var1x -> var1x.mapId() == var1).findFirst().orElse(null);
   }

   static String npcName(String var0, int var1) {
      ResourceDatabase.Snapshot var2 = current;
      return var2 == null ? var0 : var2.npcNames().getOrDefault(var1, var0);
   }

   static byte[] mapPage(byte[] var0, int var1) {
      ResourceDatabase.Snapshot var2 = current;
      return var2 != null && !var2.mapPages().contains(var1) ? Protocol.systemMessage(9, "此地圖頁面尚無原生圖資對應，暫不開放。") : var0;
   }

   static NativeLitoTownPortalCatalog.Route portal(NativeLitoTownPortalCatalog.Route var0, int var1, int var2, int var3) {
      ResourceDatabase.Snapshot var4 = current;
      return var4 == null ? var0 : var4.portals().get(var1 + ":" + var2 + ":" + var3);
   }

   static List<NativeLitoTownPortalCatalog.Route> portals(List<NativeLitoTownPortalCatalog.Route> var0, int var1) {
      ResourceDatabase.Snapshot var2 = current;
      return var2 == null ? var0 : var2.portals().values().stream().filter(var1x -> var1x.sourceMapId() == var1).toList();
   }

   static int portalCount(int var0) {
      ResourceDatabase.Snapshot var1 = current;
      return var1 == null ? var0 : var1.portals().size();
   }

   record Key(String type, int skill, String field) {
   }

   record Snapshot(
      Map<ResourceDatabase.Key, Integer> values,
      Map<Integer, Integer> selectors,
      Map<Integer, NativeLitoMapTableCatalog.Entry> maps,
      List<NativeLitoZoneCatalog.Zone> zones,
      List<NativeLitoZoneCatalog.Color> colors,
      Map<Integer, String> npcNames,
      Set<Integer> mapPages,
      Map<String, NativeLitoTownPortalCatalog.Route> portals,
      int arrow,
      int flightType
   ) {
   }
}
