// DECOMPILED from base-0.6.71.jar, TODO review
// Decompiled with Vineflower 1.11.2; reviewed against formal bytecode.
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import local.lito381.server.NativeDeItemMetadata.Logs;

public class NativeDeItemAbility {
   private static final NativeDeWorldLog _log = Logs.getLogger(NativeDeItemAbility.class);
   private volatile String _debugContext = "";
   private final Map<NativeDeItemAbility.AbilityType, Integer> _abilities = new EnumMap<>(NativeDeItemAbility.AbilityType.class);

   public NativeDeItemAbility() {
      for (NativeDeItemAbility.AbilityType var4 : NativeDeItemAbility.AbilityType.values()) {
         this._abilities.put(var4, 0);
      }
   }

   public synchronized void setDebugContext(String var1) {
      this._debugContext = var1 != null ? var1 : "";
   }

   public synchronized String getDebugContext() {
      return this._debugContext;
   }

   public synchronized boolean auditIntegrity(String var1) {
      int var2 = NativeDeItemAbility.AbilityType.values().length;
      List var3 = new ArrayList();

      for (NativeDeItemAbility.AbilityType var7 : NativeDeItemAbility.AbilityType.values()) {
         Integer var8 = this._abilities.get(var7);
         if (var8 == null) {
            boolean var9 = this._abilities.containsKey(var7);
            var3.add(var7.name() + (var9 ? "=null" : "=missing"));
         }
      }

      if (this._abilities.size() != var2) {
         var3.add("mapSize=" + this._abilities.size() + " expected=" + var2);
      }

      if (!var3.isEmpty()) {
         _log.warn(
            "[ItemAbility] 能力 map 異常 phase={} issues=[{}] ctx={} thread={}",
            var1,
            String.join(", ", var3),
            this._debugContext,
            Thread.currentThread().getName()
         );
         return false;
      } else {
         return true;
      }
   }

   private int resolveStored(NativeDeItemAbility.AbilityType var1, String var2, int var3) {
      if (var1 == null) {
         _log.warn("[ItemAbility] AbilityType 為 null op={} addDelta={} ctx={} thread={}", var2, var3, this._debugContext, Thread.currentThread().getName());
         return 0;
      } else {
         Integer var4 = this._abilities.get(var1);
         if (var4 == null) {
            boolean var5 = this._abilities.containsKey(var1);
            _log.warn(
               "[ItemAbility] 能力值異常(將以 0 計算) op={} type={} containsKey={} mapSize={} enumCount={} addDelta={} ctx={} thread={} hint={}",
               var2,
               var1,
               var5,
               this._abilities.size(),
               NativeDeItemAbility.AbilityType.values().length,
               var3,
               this._debugContext,
               Thread.currentThread().getName(),
               var5 ? "key存在但value為null" : "key不存在(可能clear與add競爭或未初始化)"
            );
            return 0;
         } else {
            return var4;
         }
      }
   }

   public synchronized void add(NativeDeItemAbility.AbilityType var1, int var2) {
      int var3 = this.resolveStored(var1, "add", var2);
      this._abilities.put(var1, var3 + var2);
   }

   public synchronized void set(NativeDeItemAbility.AbilityType var1, int var2) {
      this._abilities.put(var1, var2);
   }

   public synchronized int get(NativeDeItemAbility.AbilityType var1) {
      return this.resolveStored(var1, "get", 0);
   }

   public synchronized boolean has(NativeDeItemAbility.AbilityType var1) {
      return this.get(var1) != 0;
   }

   public synchronized void clear() {
      for (NativeDeItemAbility.AbilityType var4 : NativeDeItemAbility.AbilityType.values()) {
         this._abilities.put(var4, 0);
      }
   }

   public synchronized Map<NativeDeItemAbility.AbilityType, Integer> getAll() {
      return new EnumMap<>(this._abilities);
   }

   public static enum AbilityType {
      STR,
      DEX,
      CON,
      INT,
      WIS,
      CHA,
      HP,
      MP,
      MR,
      SP,
      HIT,
      DMG,
      BOW_HIT,
      BOW_DMG,
      PIERCE,
      MAGIC_BOW,
      ARROW_SPR,
      BASE_DMG_SMALL,
      BASE_DMG_LARGE,
      AC,
      DMG_REDUX,
      DODGE,
      ER,
      DEF_FIRE,
      DEF_WATER,
      DEF_WIND,
      DEF_EARTH,
      REGIST_FREEZE,
      REGIST_STONE,
      REGIST_SLEEP,
      REGIST_BLIND,
      REGIST_STUN,
      REGIST_SUSTAIN,
      STUN_SUCCESS,
      MAGIC_DMG_MODIFIER,
      MAGIC_HIT,
      POTION_RECOVERY,
      POISON_RESIST,
      PVP_DMG,
      PVP_REDUX,
      DMG_BOSS,
      BOSS_REDUX,
      IGNORE_REDUCTION,
      MELEE_REDUX,
      BOW_REDUX,
      ADENA_RATE,
      DROP_RATE,
      EXP_RATE,
      ATTR_ENCHANT_KIND,
      ATTR_ENCHANT_LEVEL,
      ATTR_PASSIVE_DMG,
      ATTR_PROBABILITY,
      ATTR_MAGIC_DMG_MIN,
      ATTR_MAGIC_DMG_MAX,
      ATTR_DRAIN_HP_MIN,
      ATTR_DRAIN_HP_MAX,
      ATTR_DRAIN_MP_MIN,
      ATTR_DRAIN_MP_MAX,
      ATTR_DMG_UP_RATE,
      ATTR_RANGE_RADIUS,
      ATTR_RANGE_DMG,
      ATTR_GFX_ID,
      HP_REGEN,
      MP_REGEN,
      MAGIC_DMG_REDUX,
      WEIGHT_REDUCTION,
      HAS_WEAPON_PROC,
      HAS_MAGIC_STONE,
      MAGIC_STONE_ID,
      MAGIC_STONE_LEVEL,
      WEIGHT_LIMIT_RATE,
      HP_REGEN_TIMED_AMOUNT,
      HP_REGEN_TIMED_INTERVAL,
      MP_REGEN_TIMED_AMOUNT,
      MP_REGEN_TIMED_INTERVAL,
      DOLL_GET_ITEM_ID,
      DOLL_GET_ITEM_COUNT,
      DOLL_GET_ITEM_INTERVAL,
      STATUS_UNDERWATER_BREATH,
      STATUS_HASTE,
      AUTO_SKILL_ID;
   }
}
