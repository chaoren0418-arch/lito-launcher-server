// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import local.lito381.server.NativeDeInitializerKernel;

final class NativeDeControlCatalog {
    static final String NAMES = "SELECT * FROM `\u5047\u4eba_\u4eba\u7269\u6e05\u55ae`";
    static final String ABILITIES = "SELECT * FROM \u5047\u4eba_\u80fd\u529b\u8a2d\u7f6e";
    static final String SETTINGS = "SELECT * FROM `\u5047\u4eba_\u8a2d\u7f6e\u72c0\u614b`";
    static final List<String> ABILITY_COLUMNS = List.of("lvl_min", "lvl_max", "lawful_min", "lawful_max", "hp_min", "hp_max", "mp_min", "mp_max", "ac_min", "ac_max", "str_add", "dex_add", "con_add", "int_add", "wis_add", "cha_add", "sp_add", "dmg_add");

    private NativeDeControlCatalog() {
    }

    static Names emptyNames() {
        return new Names(Map.of(), Map.of(), List.of(), List.of(), 0, 0, List.of(), false);
    }

    static Abilities emptyAbilities() {
        return new Abilities(Map.of(), 0, false);
    }

    static Settings emptySettings() {
        return new Settings(Map.of(), Map.of(), 0, false);
    }

    static Catalog empty() {
        return new Catalog(NativeDeControlCatalog.emptyNames(), NativeDeControlCatalog.emptyAbilities(), NativeDeControlCatalog.emptySettings());
    }

   static NativeDeControlCatalog.Names names(Connection var0, NativeDeControlCatalog.Names var1, NativeDeControlCatalog.Reporter var2) {
      ConcurrentHashMap var3 = new ConcurrentHashMap();
      ConcurrentHashMap var4 = new ConcurrentHashMap();
      ArrayList<NativeDeInitializerKernel.Name> var5 = new ArrayList<>(var1.history());

      for (NativeDeInitializerKernel.Name var7 : var5) {
         var3.put(var7.name, var7);
         var4.put(var7.id, var7);
      }

      int var27 = 0;
      int var28 = 0;
      ArrayList var8 = new ArrayList();
      boolean var9 = false;

      try (
         PreparedStatement var10 = var0.prepareStatement("SELECT * FROM `假人_人物清單`");
         ResultSet var11 = var10.executeQuery();
      ) {
         while (var11.next()) {
            var27++;
            int var12 = var11.getInt("deobjid");
            String var13 = var11.getString("denamebig5");
            String var14 = var11.getString("detitlebig5");
            if (var14 == null) {
               var14 = "";
            }

            String var15 = var11.getString("deshopbig5");
            if (var15 == null) {
               var15 = "";
            }

            if (var13 == null || var13.trim().isEmpty()) {
               var28++;
               var2.event("invalid-name", var12, null);
               continue;
            }
            {
               int var16 = var11.getInt("class_id");
               int var17 = var11.getInt("sex");
               int var18 = var11.getInt("clanid");
               int var19 = 0;

               try {
                  var19 = var11.getInt("weapon_type");
               } catch (SQLException var23) {
                  var19 = 0;
               }

               NativeDeInitializerKernel.Name var20 = new NativeDeInitializerKernel.Name(var12, var13, var16, var17, var18, var19, var14, var15);
               if (var3.get(var13) == null) {
                  var3.put(var13, var20);
                  var4.put(var12, var20);
                  var5.add(var20);
                  var2.registered(var20);
               } else {
                  var8.add(var12);
                  var2.event("duplicate-name-write-suppressed", var12, null);
               }
            }
         }
      } catch (SQLException var26) {
         var9 = true;
         var2.event("names-sql", 0, var26);
      }

      return new NativeDeControlCatalog.Names(
         var3, var4, var5, Arrays.asList((NativeDeInitializerKernel.Name[])var3.values().toArray(new NativeDeInitializerKernel.Name[var3.size()])), var27, var28, var8, var9
      );
   }

    static Abilities abilities(Connection connection, Abilities abilities, boolean bl, Reporter reporter) {
        HashMap<Integer, NativeDeInitializerKernel.Ability> hashMap = bl ? new HashMap<Integer, NativeDeInitializerKernel.Ability>() : new HashMap<Integer, NativeDeInitializerKernel.Ability>(abilities.values());
        int n = 0;
        boolean bl2 = false;
        try (PreparedStatement preparedStatement = connection.prepareStatement(ABILITIES);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                ++n;
                int n2 = resultSet.getInt("class_id");
                int[] nArray = new int[18];
                for (int i = 0; i < nArray.length; ++i) {
                    nArray[i] = resultSet.getInt(ABILITY_COLUMNS.get(i));
                }
                hashMap.put(n2, new NativeDeInitializerKernel.Ability(nArray));
            }
            reporter.event("abilities-info", hashMap.size(), null);
        }
        catch (SQLException sQLException) {
            bl2 = true;
            reporter.event("abilities-sql", 0, sQLException);
        }
        return new Abilities(hashMap, n, bl2);
    }

    static List<Integer> tolerant(String string, List<Integer> list) {
        if (string == null || string.isEmpty()) {
            return list;
        }
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (String string2 : string.split(",")) {
            try {
                arrayList.add(Integer.parseInt(string2.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                arrayList.add(0);
            }
        }
        return arrayList;
    }

    static List<Integer> strictTokens(String string, List<Integer> list) {
        if (string == null || string.isEmpty()) {
            return list;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(string, ",");
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        while (stringTokenizer.hasMoreTokens()) {
            arrayList.add(Integer.parseInt(stringTokenizer.nextToken().trim()));
        }
        return arrayList;
    }

   static NativeDeControlCatalog.Settings settings(Connection var0, NativeDeControlCatalog.Reporter var1) {
      HashMap var2 = new HashMap();
      HashMap var3 = new HashMap();
      int var4 = 0;
      boolean var5 = false;

      try (
         PreparedStatement var6 = var0.prepareStatement("SELECT * FROM `假人_設置狀態`");
         ResultSet var7 = var6.executeQuery();
      ) {
         while (var7.next()) {
            var4++;
            int var8 = var7.getInt("id");
            int var9 = var7.getInt("class_id");
            int var10 = var7.getInt("weapon_type");
            String var11 = var7.getString("poly_list");
            List var12 = tolerant(var11, List.of(0));
            String var13 = var7.getString("doll_list");
            List var14 = tolerant(var13, List.of());
            List var15 = strictTokens(var7.getString("use_skill"), null);
            List var16 = strictTokens(var7.getString("use_item_Gfx"), List.of());
            String var17 = var7.getString("De_pet");
            ArrayList var18 = new ArrayList();
            if (var17 != null && !var17.equals("0") && !var17.isEmpty()) {
               try {
                  String var19 = var17.replaceAll("\\s+", "");
                  if (!var19.isEmpty()) {
                     StringTokenizer var20 = new StringTokenizer(var19, ",");
                     int var21 = 0;

                     while (var20.hasMoreTokens()) {
                        String var22 = var20.nextToken();
                        if (var22 == null || var22.isEmpty()) {
                           continue;
                        }
                        {
                           StringTokenizer var23 = new StringTokenizer(var22, ";");
                           if (var23.countTokens() >= 3) {
                              try {
                                 int var24 = Integer.parseInt(var23.nextToken().trim());
                                 int var25 = Integer.parseInt(var23.nextToken().trim());
                                 int var26 = Integer.parseInt(var23.nextToken().trim());
                                 if (var24 > 0 && var25 > 0 && var26 > 0) {
                                    var18.add(new NativeDeInitializerKernel.PetInfo(var24, var25, var26));
                                    var21++;
                                 }
                              } catch (NumberFormatException var30) {
                                 var1.event("pet-number", 0, var30);
                              }
                           } else {
                              var1.event("pet-short", 0, null);
                           }
                        }
                     }

                     if (var21 > 0) {
                        var1.event("pet-info", var21, null);
                     }
                  }
               } catch (Exception var31) {
                  var1.event("pet-error", 0, var31);
               }
            }

            String var35 = var7.getString("akk_skill");
            List<Integer> var36;
            if (var35 != null && !var35.isEmpty()) {
               StringTokenizer var37 = new StringTokenizer(var35, ",");
               var36 = new ArrayList();

               while (var37.hasMoreTokens()) {
                  try {
                     var36.add(Integer.parseInt(var37.nextToken().trim()));
                  } catch (NumberFormatException var29) {
                     var36.add(0);
                  }
               }
            } else {
               var36 = List.of(0);
            }

            NativeDeControlCatalog.Definition var38 = new NativeDeControlCatalog.Definition(
               new NativeDeInitializerKernel.Setting(var8, var9, var10), var11, var12, var13, var14, var15, var16, var18, var36
            );
            var2.put(var9 + "_" + var10, var38);
            if (var8 > 0) {
               var3.put(var8, var38);
            }
         }

         var1.event("settings-info", var2.size(), null);
      } catch (Exception var34) {
         var5 = true;
         var1.event("settings-error", 0, var34);
      }

      return new NativeDeControlCatalog.Settings(var2, var3, var4, var5);
   }

    record Names(Map<String, NativeDeInitializerKernel.Name> byName, Map<Integer, NativeDeInitializerKernel.Name> byId, List<NativeDeInitializerKernel.Name> history, List<NativeDeInitializerKernel.Name> selection, int rows, int invalid, List<Integer> duplicates, boolean sqlFailure) {Names {
            byName = Map.copyOf(byName);
            byId = Map.copyOf(byId);
            history = List.copyOf(history);
            selection = List.copyOf(selection);
            duplicates = List.copyOf(duplicates);
        }
    }

    record Abilities(Map<Integer, NativeDeInitializerKernel.Ability> values, int rows, boolean sqlFailure) {Abilities {
            values = Map.copyOf(values);
        }
    }

    record Settings(Map<String, Definition> values, Map<Integer, Definition> byId, int rows, boolean failure) {Settings {
            values = Map.copyOf(values);
            byId = Map.copyOf(byId);
        }

        Definition find(int n, int n2) {
            return this.values.get(n + "_" + n2);
        }

        int poly(int n, int n2, IntUnaryOperator intUnaryOperator) {
            Definition definition = this.find(n, n2);
            return definition == null || definition.polyIds().isEmpty() ? 0 : definition.polyIds().get(intUnaryOperator.applyAsInt(definition.polyIds().size()));
        }

        int doll(int n, int n2, IntUnaryOperator intUnaryOperator) {
            Definition definition = this.find(n, n2);
            return definition == null || definition.dollIds().isEmpty() ? 0 : definition.dollIds().get(intUnaryOperator.applyAsInt(definition.dollIds().size()));
        }

        NativeDeInitializerKernel.PetInfo pet(int n, int n2, IntUnaryOperator intUnaryOperator) {
            Definition definition = this.find(n, n2);
            return definition == null || definition.pets().isEmpty() ? null : definition.pets().get(intUnaryOperator.applyAsInt(definition.pets().size()));
        }
    }

    record Catalog(Names names, Abilities abilities, Settings settings) {
    }

    static interface Reporter {
        public void event(String var1, int var2, Exception var3);

        default public void registered(NativeDeInitializerKernel.Name name) {
        }
    }

    record Definition(NativeDeInitializerKernel.Setting key, String polyList, List<Integer> polyIds, String dollList, List<Integer> dollIds, List<Integer> useSkills, List<Integer> itemGfxs, List<NativeDeInitializerKernel.PetInfo> pets, List<Integer> attackSkills) {
        Definition {
            polyIds = List.copyOf(polyIds);
            dollIds = List.copyOf(dollIds);
            useSkills = useSkills == null ? null : List.copyOf(useSkills);
            itemGfxs = List.copyOf(itemGfxs);
            pets = List.copyOf(pets);
            attackSkills = List.copyOf(attackSkills);
        }
    }
}
