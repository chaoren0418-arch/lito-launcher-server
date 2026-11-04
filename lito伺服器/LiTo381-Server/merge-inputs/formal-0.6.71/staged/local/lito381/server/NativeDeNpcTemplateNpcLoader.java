// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import local.lito381.server.NativeDeNpcExecutor;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplateLoadScope;

final class NativeDeNpcTemplateNpcLoader {
    NativeDeNpcTemplateNpcLoader() {
    }

    static void load() {
        NativeDeNpcTemplateLoadScope.context().families = NativeDeNpcTemplateNpcLoader.sourceBuildFamily();
        new NativeDeNpcTemplateNpcLoader().sourceLoadNpcData();
    }

    private void registerConstructorCache(String string) {
        NativeDeNpcTemplateLoadScope.context().requiredImplementations.add(Objects.toString(string, "<null>"));
    }

    private void sourceLoadNpcData() {
        block28: {
            NativeDeNpcTemplateLoadScope.npcs().clear();
            try (Connection connection = NativeDeNpcTemplateLoadScope.Database.get().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `npc`");){
                ResultSet resultSet = preparedStatement.executeQuery();
                try {
                    while (resultSet.next()) {
                        int n = -1;
                        try {
                            n = resultSet.getInt("npcid");
                            NativeDeNpcTemplate nativeDeNpcTemplate = new NativeDeNpcTemplate();
                            nativeDeNpcTemplate.set_npcId(n);
                            nativeDeNpcTemplate.set_name(resultSet.getString("name"));
                            nativeDeNpcTemplate.set_nameid(resultSet.getString("nameid"));
                            String string = resultSet.getString("classname");
                            nativeDeNpcTemplate.set_classname(string);
                            nativeDeNpcTemplate.setNpcExecutor(this.addClass(n, string));
                            nativeDeNpcTemplate.setImpl(resultSet.getString("impl"));
                            nativeDeNpcTemplate.set_gfxid(resultSet.getInt("gfxid"));
                            nativeDeNpcTemplate.set_level(resultSet.getInt("lvl"));
                            nativeDeNpcTemplate.set_hp(resultSet.getInt("hp"));
                            nativeDeNpcTemplate.set_mp(resultSet.getInt("mp"));
                            nativeDeNpcTemplate.set_ac(resultSet.getInt("ac"));
                            nativeDeNpcTemplate.set_str(resultSet.getByte("str"));
                            nativeDeNpcTemplate.set_con(resultSet.getByte("con"));
                            nativeDeNpcTemplate.set_dex(resultSet.getByte("dex"));
                            nativeDeNpcTemplate.set_wis(resultSet.getByte("wis"));
                            nativeDeNpcTemplate.set_int(resultSet.getByte("intel"));
                            nativeDeNpcTemplate.set_mr(resultSet.getInt("mr"));
                            nativeDeNpcTemplate.set_exp(resultSet.getInt("exp"));
                            nativeDeNpcTemplate.set_lawful(resultSet.getInt("lawful"));
                            nativeDeNpcTemplate.set_size(resultSet.getString("size"));
                            nativeDeNpcTemplate.set_weakAttr(resultSet.getInt("weakAttr"));
                            nativeDeNpcTemplate.set_ranged(resultSet.getInt("ranged"));
                            nativeDeNpcTemplate.setTamable(resultSet.getBoolean("tamable"));
                            nativeDeNpcTemplate.set_passispeed(resultSet.getInt("passispeed"));
                            nativeDeNpcTemplate.set_atkspeed(resultSet.getInt("atkspeed"));
                            nativeDeNpcTemplate.setAtkMagicSpeed(resultSet.getInt("atk_magic_speed"));
                            nativeDeNpcTemplate.setSubMagicSpeed(resultSet.getInt("sub_magic_speed"));
                            nativeDeNpcTemplate.set_undead(resultSet.getInt("undead"));
                            nativeDeNpcTemplate.set_poisonatk(resultSet.getInt("poison_atk"));
                            nativeDeNpcTemplate.set_paralysisatk(resultSet.getInt("paralysis_atk"));
                            nativeDeNpcTemplate.set_agro(resultSet.getBoolean("agro"));
                            nativeDeNpcTemplate.set_agrososc(resultSet.getBoolean("agrososc"));
                            nativeDeNpcTemplate.set_agrocoi(resultSet.getBoolean("agrocoi"));
                            Integer n2 = NativeDeNpcTemplateLoadScope.families().get(resultSet.getString("family"));
                            if (n2 == null) {
                                nativeDeNpcTemplate.set_family(0);
                            } else {
                                nativeDeNpcTemplate.set_family(n2);
                            }
                            int n3 = resultSet.getInt("agrofamily");
                            if (nativeDeNpcTemplate.get_family() == 0 && n3 == 1) {
                                nativeDeNpcTemplate.set_agrofamily(0);
                            } else {
                                nativeDeNpcTemplate.set_agrofamily(n3);
                            }
                            nativeDeNpcTemplate.set_agrogfxid1(resultSet.getInt("agrogfxid1"));
                            nativeDeNpcTemplate.set_agrogfxid2(resultSet.getInt("agrogfxid2"));
                            nativeDeNpcTemplate.set_picupitem(resultSet.getBoolean("picupitem"));
                            nativeDeNpcTemplate.set_digestitem(resultSet.getInt("digestitem"));
                            nativeDeNpcTemplate.set_bravespeed(resultSet.getBoolean("bravespeed"));
                            nativeDeNpcTemplate.set_hprinterval(resultSet.getInt("hprinterval"));
                            nativeDeNpcTemplate.set_hpr(resultSet.getInt("hpr"));
                            nativeDeNpcTemplate.set_mprinterval(resultSet.getInt("mprinterval"));
                            nativeDeNpcTemplate.set_mpr(resultSet.getInt("mpr"));
                            nativeDeNpcTemplate.set_teleport(resultSet.getBoolean("teleport"));
                            nativeDeNpcTemplate.set_randomlevel(resultSet.getInt("randomlevel"));
                            nativeDeNpcTemplate.set_randomhp(resultSet.getInt("randomhp"));
                            nativeDeNpcTemplate.set_randommp(resultSet.getInt("randommp"));
                            nativeDeNpcTemplate.set_randomac(resultSet.getInt("randomac"));
                            nativeDeNpcTemplate.set_randomexp(resultSet.getInt("randomexp"));
                            nativeDeNpcTemplate.set_randomlawful(resultSet.getInt("randomlawful"));
                            nativeDeNpcTemplate.set_damagereduction(resultSet.getInt("damage_reduction"));
                            nativeDeNpcTemplate.set_hard(resultSet.getBoolean("hard"));
                            nativeDeNpcTemplate.set_doppel(resultSet.getBoolean("doppel"));
                            nativeDeNpcTemplate.set_IsTU(resultSet.getBoolean("IsTU"));
                            nativeDeNpcTemplate.set_IsErase(resultSet.getBoolean("IsErase"));
                            nativeDeNpcTemplate.setBowActId(resultSet.getInt("bowActId"));
                            nativeDeNpcTemplate.setKarma(resultSet.getInt("karma"));
                            nativeDeNpcTemplate.setTransformId(resultSet.getInt("transform_id"));
                            nativeDeNpcTemplate.setTransformGfxId(resultSet.getInt("transform_gfxid"));
                            nativeDeNpcTemplate.setLightSize(resultSet.getInt("light_size"));
                            nativeDeNpcTemplate.setAmountFixed(resultSet.getBoolean("amount_fixed"));
                            nativeDeNpcTemplate.setChangeHead(resultSet.getBoolean("change_head"));
                            nativeDeNpcTemplate.setCantResurrect(resultSet.getBoolean("cant_resurrect"));
                            nativeDeNpcTemplate.setBoss(resultSet.getBoolean("boss"));
                            this.registerConstructorCache(nativeDeNpcTemplate.getImpl());
                            NativeDeNpcTemplateLoadScope.npcs().put(n, nativeDeNpcTemplate);
                            continue;
                        }
                        catch (Exception exception) {
                            NativeDeNpcTemplateLoadScope.log().error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (npcid: " + n + ")", exception);
                        }
                    }
                }
                finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            catch (SQLException sQLException) {
                NativeDeNpcTemplateLoadScope.log().error(sQLException.getLocalizedMessage(), sQLException);
            }
        }
    }

    private NativeDeNpcExecutor addClass(int n, String string) {
        block7: {
            try {
                if (string.equals("0")) break block7;
                String string2 = string;
                String[] stringArray = null;
                if (string.indexOf(" ") != -1) {
                    stringArray = string.split(" ");
                    try {
                        string2 = stringArray[0];
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("com.lineage.data.npc.");
                stringBuilder.append(string2);
                Class<?> clazz = NativeDeNpcTemplateLoadScope.resolveClass(stringBuilder.toString());
                NativeDeNpcExecutor nativeDeNpcExecutor = (NativeDeNpcExecutor)clazz.getMethod("get", new Class[0]).invoke(null, new Object[0]);
                if (stringArray != null) {
                    nativeDeNpcExecutor.set_set(stringArray);
                }
                return nativeDeNpcExecutor;
            }
            catch (ClassNotFoundException classNotFoundException) {
                String string3 = "\u767c\u751f[NPC\u6a94\u6848]\u932f\u8aa4, \u6aa2\u67e5\u6a94\u6848\u662f\u5426\u5b58\u5728:" + string + " NpcId:" + n;
                NativeDeNpcTemplateLoadScope.log().error(string3);
            }
            catch (Exception exception) {
                NativeDeNpcTemplateLoadScope.log().error(exception.getLocalizedMessage(), exception);
            }
        }
        return null;
    }

    private static Map<String, Integer> sourceBuildFamily() {
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        try (Connection connection = NativeDeNpcTemplateLoadScope.Database.get().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select distinct(family) as family from npc WHERE NOT trim(family) =''");
             ResultSet resultSet = preparedStatement.executeQuery();){
            int n = 1;
            while (resultSet.next()) {
                String string = "";
                try {
                    string = resultSet.getString("family");
                    int n2 = n++;
                    if (string.equalsIgnoreCase("orc")) {
                        NativeDeNpcTemplateLoadScope.orc(n2);
                    }
                    hashMap.put(string, n2);
                }
                catch (Exception exception) {
                    NativeDeNpcTemplateLoadScope.log().error("\u8f09\u5165\u8cc7\u6599\u5931\u6557\uff0c\u5df2\u7565\u904e\u8a72\u7b46 (family: " + string + ", id: " + n + ")", exception);
                }
            }
        }
        catch (SQLException sQLException) {
            NativeDeNpcTemplateLoadScope.log().error(sQLException.getLocalizedMessage(), sQLException);
        }
        return hashMap;
    }
}
