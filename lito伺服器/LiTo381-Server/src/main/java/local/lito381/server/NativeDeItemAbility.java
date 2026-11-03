// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import local.lito381.server.NativeDeItemMetadata;
import local.lito381.server.NativeDeWorldLog;

public class NativeDeItemAbility {
    private static final NativeDeWorldLog _log = NativeDeItemMetadata.Logs.getLogger(NativeDeItemAbility.class);
    private volatile String _debugContext = "";
    private final Map<AbilityType, Integer> _abilities = new EnumMap<AbilityType, Integer>(AbilityType.class);

    public NativeDeItemAbility() {
        for (AbilityType abilityType : AbilityType.values()) {
            this._abilities.put(abilityType, 0);
        }
    }

    public synchronized void setDebugContext(String string) {
        this._debugContext = string != null ? string : "";
    }

    public synchronized String getDebugContext() {
        return this._debugContext;
    }

    public synchronized boolean auditIntegrity(String string) {
        int n = AbilityType.values().length;
        ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
        for (AbilityType abilityType : AbilityType.values()) {
            Integer n2 = this._abilities.get((Object)abilityType);
            if (n2 != null) continue;
            boolean bl = this._abilities.containsKey((Object)abilityType);
            arrayList.add((CallSite)((Object)(abilityType.name() + (bl ? "=null" : "=missing"))));
        }
        if (this._abilities.size() != n) {
            arrayList.add((CallSite)((Object)("mapSize=" + this._abilities.size() + " expected=" + n)));
        }
        if (!arrayList.isEmpty()) {
            _log.warn("[ItemAbility] \u80fd\u529b map \u7570\u5e38 phase={} issues=[{}] ctx={} thread={}", (Object)string, (Object)String.join((CharSequence)", ", arrayList), (Object)this._debugContext, (Object)Thread.currentThread().getName());
            return false;
        }
        return true;
    }

    private int resolveStored(AbilityType abilityType, String string, int n) {
        if (abilityType == null) {
            _log.warn("[ItemAbility] AbilityType \u70ba null op={} addDelta={} ctx={} thread={}", (Object)string, (Object)n, (Object)this._debugContext, (Object)Thread.currentThread().getName());
            return 0;
        }
        Integer n2 = this._abilities.get((Object)abilityType);
        if (n2 == null) {
            boolean bl = this._abilities.containsKey((Object)abilityType);
            _log.warn("[ItemAbility] \u80fd\u529b\u503c\u7570\u5e38(\u5c07\u4ee5 0 \u8a08\u7b97) op={} type={} containsKey={} mapSize={} enumCount={} addDelta={} ctx={} thread={} hint={}", (Object)string, (Object)abilityType, (Object)bl, (Object)this._abilities.size(), (Object)AbilityType.values().length, (Object)n, (Object)this._debugContext, (Object)Thread.currentThread().getName(), (Object)(bl ? "key\u5b58\u5728\u4f46value\u70banull" : "key\u4e0d\u5b58\u5728(\u53ef\u80fdclear\u8207add\u7af6\u722d\u6216\u672a\u521d\u59cb\u5316)"));
            return 0;
        }
        return n2;
    }

    public synchronized void add(AbilityType abilityType, int n) {
        int n2 = this.resolveStored(abilityType, "add", n);
        this._abilities.put(abilityType, n2 + n);
    }

    public synchronized void set(AbilityType abilityType, int n) {
        this._abilities.put(abilityType, n);
    }

    public synchronized int get(AbilityType abilityType) {
        return this.resolveStored(abilityType, "get", 0);
    }

    public synchronized boolean has(AbilityType abilityType) {
        return this.get(abilityType) != 0;
    }

    public synchronized void clear() {
        for (AbilityType abilityType : AbilityType.values()) {
            this._abilities.put(abilityType, 0);
        }
    }

    public synchronized Map<AbilityType, Integer> getAll() {
        return new EnumMap<AbilityType, Integer>(this._abilities);
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
