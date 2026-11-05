// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class NativeDeNpcTemplateMobLoader {
   static void load() {
      new NativeDeNpcTemplateMobLoader().sourceLoadMobSkillData();
   }

   private void sourceLoadMobSkillData() {
      NativeDeNpcTemplateLoadScope.Timer var1 = new NativeDeNpcTemplateLoadScope.Timer();

      try (
         Connection var2 = NativeDeNpcTemplateLoadScope.Database.get().getConnection();
         PreparedStatement var3 = var2.prepareStatement("SELECT mobid,count(*) as cnt FROM mobskill group by mobid");
         PreparedStatement var4 = var2.prepareStatement("SELECT * FROM mobskill where mobid = ? order by mobid,actNo");
         ResultSet var5 = var3.executeQuery();
      ) {
         int var6 = 0;
         int var7 = 0;
         int var8 = 0;

         while (var5.next()) {
            var7 = var5.getInt("mobid");
            var6 = var5.getInt("cnt");

            try {
               var4.setInt(1, var7);
               NativeDeNpcMobSkill var9 = new NativeDeNpcMobSkill(var6);
               var9.set_mobid(var7);

               try (ResultSet var10 = var4.executeQuery()) {
                  while (var10.next()) {
                     var8 = var10.getInt("actNo");
                     String var11 = var10.getString("mobname");
                     int var12 = var10.getInt("type");
                     int var13 = var10.getInt("SkillId");
                     var9.setMobName(isBlank(var11) ? buildMobSkillName(var7, var13, var12) : var11);
                     var9.setType(var8, var10.getInt("type"));
                     var9.setTriggerRandom(var8, var10.getInt("TriRnd"));
                     var9.setTriggerHp(var8, var10.getInt("TriHp"));
                     var9.setTriggerCompanionHp(var8, var10.getInt("TriCompanionHp"));
                     var9.setTriggerRange(var8, var10.getInt("TriRange"));
                     var9.setTriggerCount(var8, var10.getInt("TriCount"));
                     var9.setChangeTarget(var8, var10.getInt("ChangeTarget"));
                     var9.setRange(var8, var10.getInt("Range"));
                     var9.setAreaWidth(var8, var10.getInt("AreaWidth"));
                     var9.setAreaHeight(var8, var10.getInt("AreaHeight"));
                     var9.setLeverage(var8, var10.getInt("Leverage"));
                     var9.setSkillId(var8, var10.getInt("SkillId"));
                     var9.setGfxid(var8, var10.getInt("Gfxid"));
                     var9.setActid(var8, var10.getInt("Actid"));
                     var9.setSummon(var8, var10.getInt("SummonId"));
                     var9.setSummonMin(var8, var10.getInt("SummonMin"));
                     var9.setSummonMax(var8, var10.getInt("SummonMax"));
                     var9.setPolyId(var8, var10.getInt("PolyId"));
                     var9.setReuseDelay(var8, var10.getInt("reuseDelay"));
                  }
               }

               NativeDeNpcTemplateLoadScope.mobSkills().put(var7, var9);
            } catch (SQLException var20) {
               NativeDeNpcTemplateLoadScope.log().error(var20.getLocalizedMessage(), (Throwable)var20);
            }
         }

         NativeDeNpcTemplateLoadScope.log().info("載入MOB技能資料數量: " + NativeDeNpcTemplateLoadScope.mobSkills().size() + "(" + var1.get() + "ms)");
      } catch (SQLException var25) {
         NativeDeNpcTemplateLoadScope.log().error(var25.getLocalizedMessage(), (Throwable)var25);
      }

      NativeDeNpcTemplateLoadScope.suppressedBlankNames(this);
   }

   private static String buildMobSkillName(int var0, int var1, int var2) {
      NativeDeNpcTemplate var3 = NativeDeNpcTemplateLoadScope.Npcs.get().getTemplate(var0);
      if (var3 != null && !isBlank(var3.get_name())) {
         String var4 = null;
         if (var1 > 0) {
            NativeDeNpcTemplateLoadScope.Skill var5 = NativeDeNpcTemplateLoadScope.Skills.get().getTemplate(var1);
            if (var5 != null) {
               var4 = var5.getName();
            }
         }

         if (isBlank(var4)) {
            var4 = fallbackSkillName(var2, var1);
         }

         if (isBlank(var4)) {
            return null;
         } else {
            String var6 = var3.get_name() + ">" + var4;
            return var6.length() <= 45 ? var6 : var6.substring(0, 45);
         }
      } else {
         return null;
      }
   }

   private static String fallbackSkillName(int var0, int var1) {
      switch (var0) {
         case 1:
            return "物理攻擊";
         case 2:
            return var1 > 0 ? "技能" + var1 : "魔法攻擊";
         case 3:
            return "召喚";
         case 4:
            return "變身";
         case 5:
            return "衝暈";
         case 6:
            return "魔法相消";
         case 7:
            return "武器破壞";
         default:
            return null;
      }
   }

   private static boolean isBlank(String var0) {
      return var0 == null || var0.trim().isEmpty();
   }
}
