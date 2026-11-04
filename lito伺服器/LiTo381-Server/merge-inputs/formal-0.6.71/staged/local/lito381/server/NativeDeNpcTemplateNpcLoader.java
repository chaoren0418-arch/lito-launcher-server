// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class NativeDeNpcTemplateNpcLoader {
   static void load() {
      NativeDeNpcTemplateLoadScope.context().families = sourceBuildFamily();
      new NativeDeNpcTemplateNpcLoader().sourceLoadNpcData();
   }

   private void registerConstructorCache(String var1) {
      NativeDeNpcTemplateLoadScope.context().requiredImplementations.add(Objects.toString(var1, "<null>"));
   }

   private void sourceLoadNpcData() {
      NativeDeNpcTemplateLoadScope.npcs().clear();

      try (
         Connection var1 = NativeDeNpcTemplateLoadScope.Database.get().getConnection();
         PreparedStatement var2 = var1.prepareStatement("SELECT * FROM `npc`");
         ResultSet var3 = var2.executeQuery();
      ) {
         while (var3.next()) {
            int var4 = -1;

            try {
               var4 = var3.getInt("npcid");
               NativeDeNpcTemplate var5 = new NativeDeNpcTemplate();
               var5.set_npcId(var4);
               var5.set_name(var3.getString("name"));
               var5.set_nameid(var3.getString("nameid"));
               String var6 = var3.getString("classname");
               var5.set_classname(var6);
               var5.setNpcExecutor(this.addClass(var4, var6));
               var5.setImpl(var3.getString("impl"));
               var5.set_gfxid(var3.getInt("gfxid"));
               var5.set_level(var3.getInt("lvl"));
               var5.set_hp(var3.getInt("hp"));
               var5.set_mp(var3.getInt("mp"));
               var5.set_ac(var3.getInt("ac"));
               var5.set_str(var3.getByte("str"));
               var5.set_con(var3.getByte("con"));
               var5.set_dex(var3.getByte("dex"));
               var5.set_wis(var3.getByte("wis"));
               var5.set_int(var3.getByte("intel"));
               var5.set_mr(var3.getInt("mr"));
               var5.set_exp(var3.getInt("exp"));
               var5.set_lawful(var3.getInt("lawful"));
               var5.set_size(var3.getString("size"));
               var5.set_weakAttr(var3.getInt("weakAttr"));
               var5.set_ranged(var3.getInt("ranged"));
               var5.setTamable(var3.getBoolean("tamable"));
               var5.set_passispeed(var3.getInt("passispeed"));
               var5.set_atkspeed(var3.getInt("atkspeed"));
               var5.setAtkMagicSpeed(var3.getInt("atk_magic_speed"));
               var5.setSubMagicSpeed(var3.getInt("sub_magic_speed"));
               var5.set_undead(var3.getInt("undead"));
               var5.set_poisonatk(var3.getInt("poison_atk"));
               var5.set_paralysisatk(var3.getInt("paralysis_atk"));
               var5.set_agro(var3.getBoolean("agro"));
               var5.set_agrososc(var3.getBoolean("agrososc"));
               var5.set_agrocoi(var3.getBoolean("agrocoi"));
               Integer var7 = NativeDeNpcTemplateLoadScope.families().get(var3.getString("family"));
               if (var7 == null) {
                  var5.set_family(0);
               } else {
                  var5.set_family(var7);
               }

               int var8 = var3.getInt("agrofamily");
               if (var5.get_family() == 0 && var8 == 1) {
                  var5.set_agrofamily(0);
               } else {
                  var5.set_agrofamily(var8);
               }

               var5.set_agrogfxid1(var3.getInt("agrogfxid1"));
               var5.set_agrogfxid2(var3.getInt("agrogfxid2"));
               var5.set_picupitem(var3.getBoolean("picupitem"));
               var5.set_digestitem(var3.getInt("digestitem"));
               var5.set_bravespeed(var3.getBoolean("bravespeed"));
               var5.set_hprinterval(var3.getInt("hprinterval"));
               var5.set_hpr(var3.getInt("hpr"));
               var5.set_mprinterval(var3.getInt("mprinterval"));
               var5.set_mpr(var3.getInt("mpr"));
               var5.set_teleport(var3.getBoolean("teleport"));
               var5.set_randomlevel(var3.getInt("randomlevel"));
               var5.set_randomhp(var3.getInt("randomhp"));
               var5.set_randommp(var3.getInt("randommp"));
               var5.set_randomac(var3.getInt("randomac"));
               var5.set_randomexp(var3.getInt("randomexp"));
               var5.set_randomlawful(var3.getInt("randomlawful"));
               var5.set_damagereduction(var3.getInt("damage_reduction"));
               var5.set_hard(var3.getBoolean("hard"));
               var5.set_doppel(var3.getBoolean("doppel"));
               var5.set_IsTU(var3.getBoolean("IsTU"));
               var5.set_IsErase(var3.getBoolean("IsErase"));
               var5.setBowActId(var3.getInt("bowActId"));
               var5.setKarma(var3.getInt("karma"));
               var5.setTransformId(var3.getInt("transform_id"));
               var5.setTransformGfxId(var3.getInt("transform_gfxid"));
               var5.setLightSize(var3.getInt("light_size"));
               var5.setAmountFixed(var3.getBoolean("amount_fixed"));
               var5.setChangeHead(var3.getBoolean("change_head"));
               var5.setCantResurrect(var3.getBoolean("cant_resurrect"));
               var5.setBoss(var3.getBoolean("boss"));
               this.registerConstructorCache(var5.getImpl());
               NativeDeNpcTemplateLoadScope.npcs().put(var4, var5);
            } catch (Exception var12) {
               NativeDeNpcTemplateLoadScope.log().error("載入資料失敗，已略過該筆 (npcid: " + var4 + ")", (Throwable)var12);
            }
         }
      } catch (SQLException var16) {
         NativeDeNpcTemplateLoadScope.log().error(var16.getLocalizedMessage(), (Throwable)var16);
      }
   }

   private NativeDeNpcExecutor addClass(int var1, String var2) {
      try {
         if (!var2.equals("0")) {
            String var3 = var2;
            String[] var11 = null;
            if (var2.indexOf(" ") != -1) {
               var11 = var2.split(" ");

               try {
                  var3 = var11[0];
               } catch (Exception var8) {
               }
            }

            StringBuilder var5 = new StringBuilder();
            var5.append("com.lineage.data.npc.");
            var5.append(var3);
            Class var6 = NativeDeNpcTemplateLoadScope.resolveClass(var5.toString());
            NativeDeNpcExecutor var7 = (NativeDeNpcExecutor)var6.getMethod("get").invoke(null);
            if (var11 != null) {
               var7.set_set(var11);
            }

            return var7;
         }
      } catch (ClassNotFoundException var9) {
         String var4 = "發生[NPC檔案]錯誤, 檢查檔案是否存在:" + var2 + " NpcId:" + var1;
         NativeDeNpcTemplateLoadScope.log().error(var4);
      } catch (Exception var10) {
         NativeDeNpcTemplateLoadScope.log().error(var10.getLocalizedMessage(), (Throwable)var10);
      }

      return null;
   }

   private static Map<String, Integer> sourceBuildFamily() {
      HashMap var0 = new HashMap();

      try (
         Connection var1 = NativeDeNpcTemplateLoadScope.Database.get().getConnection();
         PreparedStatement var2 = var1.prepareStatement("select distinct(family) as family from npc WHERE NOT trim(family) =''");
         ResultSet var3 = var2.executeQuery();
      ) {
         int var4 = 1;

         while (var3.next()) {
            String var5 = "";

            try {
               var5 = var3.getString("family");
               int var6 = var4++;
               if (var5.equalsIgnoreCase("orc")) {
                  NativeDeNpcTemplateLoadScope.orc(var6);
               }

               var0.put(var5, var6);
            } catch (Exception var10) {
               NativeDeNpcTemplateLoadScope.log().error("載入資料失敗，已略過該筆 (family: " + var5 + ", id: " + var4 + ")", (Throwable)var10);
            }
         }
      } catch (SQLException var14) {
         NativeDeNpcTemplateLoadScope.log().error(var14.getLocalizedMessage(), (Throwable)var14);
      }

      return var0;
   }
}
