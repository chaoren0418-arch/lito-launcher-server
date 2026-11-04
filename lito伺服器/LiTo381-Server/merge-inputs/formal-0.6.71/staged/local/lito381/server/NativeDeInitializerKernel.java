// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeWeaponKernel.Target;

final class NativeDeInitializerKernel {
   private static final int[][] _class_list = new int[][]{{0, 61, 138, 734, 2786, 6658, 6671}, {1, 48, 37, 1186, 2796, 6661, 6650}};

   private NativeDeInitializerKernel() {
   }

   static void start(
      NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, NativeDeInitializerKernel.Name var2, boolean var3, String var4
   ) {
      init(var0, var1, var2, var3, var4);
      if (var1.getLevel() <= 0) {
         var1.setLevel(1);
      }

      if (var1.getInt() <= 0) {
         var1.setInt(10);
      }
   }

   static void companions(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, boolean var2) {
      companions(var0, var1, var2, true);
   }

   static void companions(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, boolean var2, boolean var3) {
      if (var2) {
         initDoll(var0, var1);
      }

      if (var3) {
         initPet(var0, var1);
      }
   }

   static void init(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, NativeDeInitializerKernel.Name var2) {
      init(var0, var1, var2, var1.isMorphEnable(), null);
   }

   static void init(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, NativeDeInitializerKernel.Name var2, boolean var3) {
      init(var0, var1, var2, var3, null);
   }

   static void init(
      NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, NativeDeInitializerKernel.Name var2, boolean var3, String var4
   ) {
      try {
         NativeDeInitializerKernel.Setting var5 = null;
         ArrayList var37 = null;
         if (var4 != null && !var4.trim().isEmpty()) {
            try {
               String[] var38 = var4.split(",");
               ArrayList var41 = new ArrayList();

               for (String var12 : var38) {
                  var12 = var12.trim();
                  if (!var12.isEmpty()) {
                     try {
                        int var13 = Integer.parseInt(var12);
                        NativeDeInitializerKernel.Setting var14 = var0.getSettingById(var13);
                        if (var14 != null) {
                           var41.add(var14);
                        }
                     } catch (NumberFormatException var33) {
                     }
                  }
               }

               if (!var41.isEmpty()) {
                  var5 = (NativeDeInitializerKernel.Setting)var41.get(var0.nextInt(var41.size()));
                  var0.log().debug("DeInitializer: 使用精確指定模式 (Setting ID=" + var5.id + ", ClassId=" + var5.classId + ", WeaponType=" + var5.weaponType + ")");
               } else {
                  var37 = new ArrayList();

                  for (String var63 : var38) {
                     var63 = var63.trim();
                     if (!var63.isEmpty()) {
                        try {
                           int var70 = Integer.parseInt(var63);
                           var37.add(var70);
                        } catch (NumberFormatException var32) {
                           var0.log().warn("DeInitializer: classRestrict 格式錯誤 (部分): " + var63, (Throwable)var32);
                        }
                     }
                  }

                  if (!var37.isEmpty()) {
                     var0.log().debug("DeInitializer: 使用舊版兼容模式 (Class IDs: " + var37 + ")");
                  }
               }
            } catch (Exception var34) {
               var0.log().warn("DeInitializer: classRestrict 解析異常: " + var4, (Throwable)var34);
            }
         }

         if (var2 == null) {
            NativeDeInitializerKernel.Name[] var39 = var0.getDeNameList();
            if (var39.length > 0) {
               ArrayList var42 = new ArrayList();
               if (var5 != null) {
                  for (NativeDeInitializerKernel.Name var65 : var39) {
                     if (var65.get_classId() == var5.classId) {
                        var42.add(var65);
                     }
                  }
               } else if (var37 != null && !var37.isEmpty()) {
                  for (NativeDeInitializerKernel.Name var66 : var39) {
                     if (var37.contains(var66.get_classId())) {
                        var42.add(var66);
                     }
                  }
               }

               if (var42.isEmpty()) {
                  for (NativeDeInitializerKernel.Name var67 : var39) {
                     var42.add(var67);
                  }
               }

               if (var42.isEmpty()) {
                  var0.log().warn("DeInitializer: 找不到符合條件的假人名單");
               } else {
                  NativeDeInitializerKernel.Name var48 = (NativeDeInitializerKernel.Name)var42.get(var0.nextInt(var42.size()));
                  ArrayList var54 = var0.liveNames();
                  ReentrantLock var60 = var0.namesLock();
                  var60.lock();

                  try {
                     for (int var68 = 0; var54.contains(var48) && var68 < 10; var68++) {
                        var48 = (NativeDeInitializerKernel.Name)var42.get(var0.nextInt(var42.size()));
                     }
                  } finally {
                     var60.unlock();
                  }

                  var2 = var48;
               }
            }
         }

         if (var2 == null) {
            var0.log().warn("[假人名稱驗證][名單選取失敗] 找不到 DeName，npcObjectId={}；可能是假人名單為空、載入失敗或沒有可用名稱", var1.getId());
            var1.setInitFailed(true);
            return;
         }

         if (var2.get_name() != null && !var2.get_name().trim().isEmpty()) {
            var1.set_deName(var2);
            ArrayList var40 = var0.liveNames();
            ReentrantLock var43 = var0.namesLock();
            var43.lock();

            try {
               if (!var40.contains(var2)) {
                  var40.add(var2);
               }
            } finally {
               var43.unlock();
            }

            var1.setNameId(var2.get_name());
            if (var1.getNameId() != null && !var1.getNameId().trim().isEmpty() && var2.get_name().equals(var1.getNameId())) {
               int var49;
               if (var5 != null) {
                  var49 = var5.classId;
               } else {
                  var49 = var2.get_classId();
               }

               int var55 = var2.get_sex();
               if (var55 > 1) {
                  var55 = 1;
               }

               if (var49 > 6) {
                  var49 = 0;
               }

               int var61 = _class_list[var55][var49];
               NativeDeInitializerKernel.Ability var69 = var0.getAbility(var49);
               if (var69 != null) {
                  int var71 = var0.nextInt(Math.max(1, var69.lvlMax - var69.lvlMin + 1)) + var69.lvlMin;
                  var1.setLevel(var71);
                  int var73 = var0.nextInt(Math.max(1, var69.hpMax - var69.hpMin + 1)) + var69.hpMin;
                  int var15 = var0.nextInt(Math.max(1, var69.mpMax - var69.mpMin + 1)) + var69.mpMin;
                  var1.setMaxHp(var73);
                  var1.setCurrentHp(var73);
                  var1.setMaxMp(var15);
                  var1.setCurrentMp(var15);
                  int var16 = Math.abs(var69.acMax - var69.acMin);
                  int var17 = var69.acMin - var0.nextInt(var16 + 1);
                  int var18 = var0.nextInt(Math.max(1, var69.lawfulMax - var69.lawfulMin + 1)) + var69.lawfulMin;
                  var1.setAc(var17);
                  var1.setLawful(var18);
                  var1.setStr(var69.strAdd + 10);
                  var1.setDex(var69.dexAdd + 10);
                  var1.setCon(var69.conAdd + 10);
                  var1.setInt(var69.intAdd + 10);
                  var1.setWis(var69.wisAdd + 10);
                  var1.setCha(var69.chaAdd + 10);
                  var1.addSp(var69.spAdd);
                  var1.addDmgup(var69.dmgAdd);
                  var1.addHitup(var71 / 3);
               }

               int var72;
               if (var5 != null) {
                  var72 = var5.weaponType;
                  var0.log().debug("DeInitializer: 強制鎖定武器類型=" + var72 + " (來自 Setting ID=" + var5.id + ")");
               } else {
                  var72 = var2.get_weaponType();
                  if (var72 == 0) {
                     var72 = NativeDeWeaponKernel.random(var49, var0::nextBoolean);
                  }
               }

               int var74 = var61;
               if (var3) {
                  int var75 = var0.getRandomPoly(var49, var72);
                  if (var75 > 0) {
                     var74 = var75;
                  }
               }

               var1.setClassId(var49);
               var1.setTempCharGfx(var74);
               var1.setGfxId(var74);
               NativeDeWeaponKernel.apply(var1, var72);
               var1.setMoveSpeed(1);
               var1.setBraveSpeed(1);
               int var76 = var0.getAttackSpeed(var1.getTempCharGfx(), var1.getCurrentWeapon() + 1);
               int var77 = var0.getMoveSpeed(var1.getTempCharGfx(), var1.getCurrentWeapon());
               var1.setPassispeed(var77);
               var1.setAtkspeed(var76);
               if (var2.get_clanid() != 0) {
                  var1.set_clanid(var2.get_clanid());
                  NativeDeInitializerKernel.Clan var78 = var0.clan(var2.get_clanid());
                  if (var78 != null) {
                     var1.set_clanname(var78.getClanName());
                     NativeDeInitializerKernel.Emblem var80 = var0.emblem(var78.getClanId());
                     var1.set_emblem(var80);
                  }
               }

               String var79 = var2.getTitle();
               if (var79 != null && !var79.trim().isEmpty()) {
                  var1.setTitle(var79);
               } else {
                  var1.setTitle("");
               }

               var1.broadcastPacketAll(var0.visual(var1.getId(), var1.getCurrentWeapon()));
               return;
            }

            var0.log()
               .error(
                  "[假人名稱驗證][套用流程錯誤] deobjid={}, sourceName={}, npcNameId={}, npcObjectId={}",
                  var2.get_deobjid(),
                  "[" + var2.get_name() + "]",
                  var1.getNameId() == null ? "<NULL>" : "[" + var1.getNameId() + "]",
                  var1.getId()
               );
            var1.setInitFailed(true);
            var1.removeFromDeNameList();
            return;
         }

         var0.log()
            .warn(
               "[假人名稱驗證][DB名稱無效] deobjid={}, denamebig5={}, npcObjectId={}",
               var2.get_deobjid(),
               var2.get_name() == null ? "<NULL>" : "[" + var2.get_name() + "]",
               var1.getId()
            );
         var1.setInitFailed(true);
         return;
      } catch (Exception var36) {
         int var6 = var2 != null ? var2.get_deobjid() : 0;
         String var7 = var2 != null ? var2.get_name() : null;
         String var8 = var1 != null ? var1.getNameId() : null;
         var0.log()
            .error(
               "[假人名稱驗證][初始化例外] deobjid={}, sourceName={}, npcNameId={}, npcObjectId={}",
               var6,
               var7 == null ? "<NULL>" : "[" + var7 + "]",
               var8 == null ? "<NULL>" : "[" + var8 + "]",
               var1 != null ? var1.getId() : 0,
               var36
            );
         if (var1 != null) {
            var1.setInitFailed(true);
            var1.removeFromDeNameList();
         }
      }
   }

   static void initDoll(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1) {
      try {
         if (!var1.getDolls().isEmpty()) {
            return;
         }

         int var2 = var1.getCurrentWeapon();
         if (var2 == 0) {
            return;
         }

         int var3 = var1.getClassId();
         int var4 = var0.getRandomDoll(var3, var2);
         if (var4 <= 0) {
            return;
         }

         int var5 = 71082;
         NativeDeInitializerKernel.Template var6 = var0.template(var5);
         if (var6 == null) {
            return;
         }

         var0.doll(var6, var1, var4);
      } catch (Exception var7) {
         var0.log().error("假人娃娃生成異常", var7);
      }
   }

   static void initDoll(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1, int var2, int var3) {
      int var4 = var0.getRandomDoll(var2, var3);
      if (var4 > 0) {
         try {
            NativeDeInitializerKernel.Template var5 = var0.template(71082);
            if (var5 == null) {
               return;
            }

            var0.doll(var5, var1, var4);
         } catch (Exception var6) {
            var0.log().error("假人娃娃生成異常", var6);
         }
      }
   }

   static void initPet(NativeDeInitializerKernel.Services var0, NativeDeInitializerKernel.Actor var1) {
      try {
         int var2 = var1.getClassId();
         int var3 = var1.getCurrentWeapon();
         NativeDeInitializerKernel.PetInfo var4 = var0.getPetInfo(var2, var3);
         if (var4 == null || var4.npcId == 0 || var4.count <= 0) {
            return;
         }

         for (int var5 = 0; var5 < var4.count; var5++) {
            NativeDeInitializerKernel.Template var6 = var0.template(var4.npcId);
            if (var6 != null) {
               NativeDeInitializerKernel.Pet var7 = var0.summon(var6, var1);
               var7.setDePetType(var4.type);
               var7.setHomeX(var7.getX());
               var7.setHomeY(var7.getY());
               var7.setHeading(var1.getHeading());
            }
         }
      } catch (Exception var8) {
         var0.log().error("假人寵物 initPet 異常", var8);
      }
   }

   static final class Ability {
      final int lvlMin;
      final int lvlMax;
      final int lawfulMin;
      final int lawfulMax;
      final int hpMin;
      final int hpMax;
      final int mpMin;
      final int mpMax;
      final int acMin;
      final int acMax;
      final int strAdd;
      final int dexAdd;
      final int conAdd;
      final int intAdd;
      final int wisAdd;
      final int chaAdd;
      final int spAdd;
      final int dmgAdd;

      Ability(int... var1) {
         if (var1.length != 18) {
            throw new IllegalArgumentException("18 explicit source ability values required");
         } else {
            this.lvlMin = var1[0];
            this.lvlMax = var1[1];
            this.lawfulMin = var1[2];
            this.lawfulMax = var1[3];
            this.hpMin = var1[4];
            this.hpMax = var1[5];
            this.mpMin = var1[6];
            this.mpMax = var1[7];
            this.acMin = var1[8];
            this.acMax = var1[9];
            this.strAdd = var1[10];
            this.dexAdd = var1[11];
            this.conAdd = var1[12];
            this.intAdd = var1[13];
            this.wisAdd = var1[14];
            this.chaAdd = var1[15];
            this.spAdd = var1[16];
            this.dmgAdd = var1[17];
         }
      }
   }

   interface Actor extends Target {
      boolean isMorphEnable();

      int getId();

      void setInitFailed(boolean var1);

      void set_deName(NativeDeInitializerKernel.Name var1);

      void removeFromDeNameList();

      void setNameId(String var1);

      String getNameId();

      void setLevel(int var1);

      int getLevel();

      void setMaxHp(int var1);

      void setCurrentHp(int var1);

      void setMaxMp(int var1);

      void setCurrentMp(int var1);

      void setAc(int var1);

      void setLawful(int var1);

      void setStr(int var1);

      void setDex(int var1);

      void setCon(int var1);

      void setInt(int var1);

      short getInt();

      void setWis(int var1);

      void setCha(int var1);

      void addSp(int var1);

      void addDmgup(int var1);

      void addHitup(int var1);

      void setClassId(int var1);

      int getClassId();

      void setTempCharGfx(int var1);

      int getTempCharGfx();

      void setGfxId(int var1);

      void setCurrentWeapon(int var1);

      int getCurrentWeapon();

      void setStatus(int var1);

      void set_ranged(int var1);

      void setBowActId(int var1);

      void setMoveSpeed(int var1);

      void setBraveSpeed(int var1);

      void setPassispeed(int var1);

      void setAtkspeed(int var1);

      void set_clanid(int var1);

      void set_clanname(String var1);

      void set_emblem(NativeDeInitializerKernel.Emblem var1);

      void setTitle(String var1);

      void broadcastPacketAll(NativeDeInitializerKernel.Packet var1);

      Map<?, ?> getDolls();

      int getHeading();

      @Override
      default void weapon(int var1) {
         this.setCurrentWeapon(var1);
      }

      @Override
      default void status(int var1) {
         this.setStatus(var1);
      }

      @Override
      default void range(int var1) {
         this.set_ranged(var1);
      }

      @Override
      default void bow(int var1) {
         this.setBowActId(var1);
      }
   }

   interface Clan {
      String getClanName();

      int getClanId();
   }

   record Emblem(int id) {
   }

   interface Log {
      void debug(String var1);

      void warn(String var1);

      void warn(String var1, Throwable var2);

      void warn(String var1, Object var2);

      void warn(String var1, Object var2, Object var3, Object var4);

      void error(String var1, Throwable var2);

      void error(String var1, Object var2, Object var3, Object var4, Object var5);

      void error(String var1, Object var2, Object var3, Object var4, Object var5, Object var6);
   }

   static class Name {
      final int id;
      final int classId;
      final int sex;
      final int clanId;
      final int weapon;
      final String name;
      final String title;
      final String shop;

      Name(int var1, String var2, int var3, int var4, int var5, int var6, String var7, String var8) {
         this.id = var1;
         this.name = var2;
         this.classId = var3;
         this.sex = var4;
         this.clanId = var5;
         this.weapon = var6;
         this.title = var7;
         this.shop = var8;
      }

      int get_deobjid() {
         return this.id;
      }

      String get_name() {
         return this.name;
      }

      int get_classId() {
         return this.classId;
      }

      int get_sex() {
         return this.sex;
      }

      int get_clanid() {
         return this.clanId;
      }

      int get_weaponType() {
         return this.weapon;
      }

      String getTitle() {
         return this.title;
      }

      String getDeShop() {
         return this.shop;
      }

      public void set_deobjid(int var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void set_name(String var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void set_classId(int var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public int get_type() {
         return this.get_classId();
      }

      public void set_type(int var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void set_sex(int var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void set_clanid(int var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void set_weaponType(int var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void setTitle(String var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }

      public void setDeShop(String var1) {
         throw new UnsupportedOperationException("shared source-name definition is immutable");
      }
   }

   interface Packet {
   }

   interface Pet {
      void setDePetType(int var1);

      int getX();

      int getY();

      void setHomeX(int var1);

      void setHomeY(int var1);

      void setHeading(int var1);
   }

   record PetInfo(int type, int npcId, int count) {
   }

   interface Services {
      NativeDeInitializerKernel.Log log();

      int nextInt(int var1);

      boolean nextBoolean();

      NativeDeInitializerKernel.Setting getSettingById(int var1);

      NativeDeInitializerKernel.Name[] getDeNameList();

      ArrayList<NativeDeInitializerKernel.Name> liveNames();

      ReentrantLock namesLock();

      NativeDeInitializerKernel.Ability getAbility(int var1);

      int getRandomPoly(int var1, int var2);

      int getRandomDoll(int var1, int var2);

      NativeDeInitializerKernel.PetInfo getPetInfo(int var1, int var2);

      int getAttackSpeed(int var1, int var2);

      int getMoveSpeed(int var1, int var2);

      NativeDeInitializerKernel.Clan clan(int var1);

      NativeDeInitializerKernel.Emblem emblem(int var1);

      NativeDeInitializerKernel.Template template(int var1);

      void doll(NativeDeInitializerKernel.Template var1, NativeDeInitializerKernel.Actor var2, int var3);

      NativeDeInitializerKernel.Pet summon(NativeDeInitializerKernel.Template var1, NativeDeInitializerKernel.Actor var2);

      NativeDeInitializerKernel.Packet visual(int var1, int var2);
   }

   record Setting(int id, int classId, int weaponType) {
   }

   record Template(int id) {
   }
}
