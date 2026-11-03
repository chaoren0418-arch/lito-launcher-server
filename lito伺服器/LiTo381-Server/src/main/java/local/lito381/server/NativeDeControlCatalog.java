// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
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

    static Names names(Connection connection, Names names, Reporter reporter) {
        ConcurrentHashMap<String, NativeDeInitializerKernel.Name> concurrentHashMap = new ConcurrentHashMap<String, NativeDeInitializerKernel.Name>();
        ConcurrentHashMap<Integer, NativeDeInitializerKernel.Name> concurrentHashMap2 = new ConcurrentHashMap<Integer, NativeDeInitializerKernel.Name>();
        ArrayList<NativeDeInitializerKernel.Name> arrayList = new ArrayList<NativeDeInitializerKernel.Name>(names.history());
        for (NativeDeInitializerKernel.Name name : arrayList) {
            concurrentHashMap.put(name.name, name);
            concurrentHashMap2.put(name.id, name);
        }
        int n = 0;
        int n2 = 0;
        ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
        boolean bl = false;
        try (PreparedStatement preparedStatement = connection.prepareStatement(NAMES);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                String string;
                ++n;
                int n3 = resultSet.getInt("deobjid");
                String string2 = resultSet.getString("denamebig5");
                String string3 = resultSet.getString("detitlebig5");
                if (string3 == null) {
                    string3 = "";
                }
                if ((string = resultSet.getString("deshopbig5")) == null) {
                    string = "";
                }
                if (string2 == null || string2.trim().isEmpty()) {
                    ++n2;
                    reporter.event("invalid-name", n3, null);
                    continue;
                }
                int n4 = resultSet.getInt("class_id");
                int n5 = resultSet.getInt("sex");
                int n6 = resultSet.getInt("clanid");
                int n7 = 0;
                try {
                    n7 = resultSet.getInt("weapon_type");
                }
                catch (SQLException sQLException) {
                    n7 = 0;
                }
                NativeDeInitializerKernel.Name name = new NativeDeInitializerKernel.Name(n3, string2, n4, n5, n6, n7, string3, string);
                if (concurrentHashMap.get(string2) == null) {
                    concurrentHashMap.put(string2, name);
                    concurrentHashMap2.put(n3, name);
                    arrayList.add(name);
                    reporter.registered(name);
                    continue;
                }
                arrayList2.add(n3);
                reporter.event("duplicate-name-write-suppressed", n3, null);
            }
        }
        catch (SQLException sQLException) {
            bl = true;
            reporter.event("names-sql", 0, sQLException);
        }
        return new Names(concurrentHashMap, concurrentHashMap2, arrayList, Arrays.asList(concurrentHashMap.values().toArray(new NativeDeInitializerKernel.Name[concurrentHashMap.size()])), n, n2, arrayList2, bl);
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

    static Settings settings(Connection connection, Reporter reporter) {
        HashMap<String, Definition> hashMap = new HashMap<String, Definition>();
        HashMap<Integer, Definition> hashMap2 = new HashMap<Integer, Definition>();
        int n = 0;
        boolean bl = false;
        try (PreparedStatement preparedStatement = connection.prepareStatement(SETTINGS);
             ResultSet resultSet = preparedStatement.executeQuery();){
            while (resultSet.next()) {
                Object object;
                String string;
                ++n;
                int n2 = resultSet.getInt("id");
                int n3 = resultSet.getInt("class_id");
                int n4 = resultSet.getInt("weapon_type");
                String string2 = resultSet.getString("poly_list");
                List<Integer> list = NativeDeControlCatalog.tolerant(string2, List.of(Integer.valueOf(0)));
                String string3 = resultSet.getString("doll_list");
                List<Integer> list2 = NativeDeControlCatalog.tolerant(string3, List.of());
                List<Integer> list3 = NativeDeControlCatalog.strictTokens(resultSet.getString("use_skill"), null);
                List<Integer> list4 = NativeDeControlCatalog.strictTokens(resultSet.getString("use_item_Gfx"), List.of());
                String string4 = resultSet.getString("De_pet");
                ArrayList<NativeDeInitializerKernel.PetInfo> arrayList = new ArrayList<NativeDeInitializerKernel.PetInfo>();
                if (string4 != null && !string4.equals("0") && !string4.isEmpty()) {
                    try {
                        string = string4.replaceAll("\\s+", "");
                        if (!string.isEmpty()) {
                            object = new StringTokenizer(string, ",");
                            int n5 = 0;
                            while (((StringTokenizer)object).hasMoreTokens()) {
                                String string5 = ((StringTokenizer)object).nextToken();
                                if (string5 == null || string5.isEmpty()) continue;
                                StringTokenizer stringTokenizer = new StringTokenizer(string5, ";");
                                if (stringTokenizer.countTokens() >= 3) {
                                    try {
                                        int n6 = Integer.parseInt(stringTokenizer.nextToken().trim());
                                        int n7 = Integer.parseInt(stringTokenizer.nextToken().trim());
                                        int n8 = Integer.parseInt(stringTokenizer.nextToken().trim());
                                        if (n6 <= 0 || n7 <= 0 || n8 <= 0) continue;
                                        arrayList.add(new NativeDeInitializerKernel.PetInfo(n6, n7, n8));
                                        ++n5;
                                    }
                                    catch (NumberFormatException numberFormatException) {
                                        reporter.event("pet-number", 0, numberFormatException);
                                    }
                                    continue;
                                }
                                reporter.event("pet-short", 0, null);
                            }
                            if (n5 > 0) {
                                reporter.event("pet-info", n5, null);
                            }
                        }
                    }
                    catch (Exception exception) {
                        reporter.event("pet-error", 0, exception);
                    }
                }
                if ((string = resultSet.getString("akk_skill")) != null && !string.isEmpty()) {
                    StringTokenizer stringTokenizer = new StringTokenizer(string, ",");
                    object = new ArrayList<Integer>();
                    while (stringTokenizer.hasMoreTokens()) {
                        try {
                            object.add(Integer.parseInt(stringTokenizer.nextToken().trim()));
                        }
                        catch (NumberFormatException numberFormatException) {
                            object.add(0);
                        }
                    }
                } else {
                    object = List.of(Integer.valueOf(0));
                }
                Definition definition = new Definition(new NativeDeInitializerKernel.Setting(n2, n3, n4), string2, list, string3, list2, list3, list4, arrayList, (List<Integer>)object);
                hashMap.put(n3 + "_" + n4, definition);
                if (n2 <= 0) continue;
                hashMap2.put(n2, definition);
            }
            reporter.event("settings-info", hashMap.size(), null);
        }
        catch (Exception exception) {
            bl = true;
            reporter.event("settings-error", 0, exception);
        }
        return new Settings(hashMap, hashMap2, n, bl);
    }

    record Names(Map<String, NativeDeInitializerKernel.Name> byName, Map<Integer, NativeDeInitializerKernel.Name> byId, List<NativeDeInitializerKernel.Name> history, List<NativeDeInitializerKernel.Name> selection, int rows, int invalid, List<Integer> duplicates, boolean sqlFailure) {
        Names {
            map = Map.copyOf(map);
            map2 = Map.copyOf(map2);
            list = List.copyOf(list);
            list2 = List.copyOf(list2);
            list3 = List.copyOf(list3);
        }
    }

    record Abilities(Map<Integer, NativeDeInitializerKernel.Ability> values, int rows, boolean sqlFailure) {
        Abilities {
            map = Map.copyOf(map);
        }
    }

    record Settings(Map<String, Definition> values, Map<Integer, Definition> byId, int rows, boolean failure) {
        Settings {
            map = Map.copyOf(map);
            map2 = Map.copyOf(map2);
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
            list = List.copyOf(list);
            list2 = List.copyOf(list2);
            list3 = list3 == null ? null : List.copyOf(list3);
            list4 = List.copyOf(list4);
            list5 = List.copyOf(list5);
            list6 = List.copyOf(list6);
        }
    }
}
