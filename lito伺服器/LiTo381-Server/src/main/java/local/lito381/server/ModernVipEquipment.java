package local.lito381.server;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Full current VIP/polymorph catalog facts. Not saved-character provenance,
 * equipment permission, an applied visual, or a combat-support grant. */
final class ModernVipEquipment {
    enum Field {
        STR("力量","addStr"), DEX("敏捷","addDex"), CON("體質","addCon"),
        INT("智力","addInt"), WIS("精神","addWis"), CHA("魅力","addCha"),
        AC("防禦","addAc"), MAX_HP("血量","addMaxHp"), MAX_MP("魔量","addMaxMp"),
        HPR("回血","addHpr"), MPR("回魔","addMpr"), MR("抗魔","addMr"), SP("魔攻","addSp"),
        MELEE_DAMAGE("近戰傷害","addDmg"), MELEE_HIT("近戰命中","addHit"),
        RANGED_DAMAGE("遠攻傷害","addBowDmg"), RANGED_HIT("遠攻命中","addBowHit"),
        DAMAGE_REDUCTION("傷害減免","reduction_dmg"), MAGIC_REDUCTION("魔法傷害減免","reduction_magic_dmg"),
        FIRE("火屬性防禦","addFire"), WIND("風屬性防禦","addWind"),
        EARTH("地屬性防禦","addEarth"), WATER("水屬性防禦","addWater"),
        STUN("昏迷耐性",null), STONE("石化耐性",null), SLEEP("睡眠耐性",null),
        FREEZE("寒冰耐性",null), SUSTAIN("支撐耐性",null), BLIND("暗黑耐性",null),
        EXP_RATE("經驗獲取率","addExpRate"), ADENA_RATE("金幣獲取率",null);
        final String vipColumn,polyColumn;
        Field(String vipColumn,String polyColumn) { this.vipColumn=vipColumn;this.polyColumn=polyColumn; }
    }

    /** Applied attribute signs: VIP AC is direct; PolyAbilityTable negates addAc.
     * Complete i64 values, independent of native byte/int truncation. */
    record Abilities(Map<Field,Long> values) {
        Abilities {
            Objects.requireNonNull(values);
            if(values.size()!=Field.values().length)throw invalid();
            var copy=new EnumMap<Field,Long>(Field.class);
            for(var field:Field.values())copy.put(field,Objects.requireNonNull(values.get(field)));
            values=Map.copyOf(copy);
        }
        long get(Field field) { return values.get(Objects.requireNonNull(field)); }
        Abilities plus(Abilities other) {
            Objects.requireNonNull(other);
            var result=new EnumMap<Field,Long>(Field.class);
            for(var field:Field.values())result.put(field,Math.addExact(get(field),other.get(field)));
            return new Abilities(result);
        }
        Abilities minus(Abilities other) {
            Objects.requireNonNull(other);
            var result=new EnumMap<Field,Long>(Field.class);
            for(var field:Field.values())result.put(field,Math.subtractExact(get(field),other.get(field)));
            return new Abilities(result);
        }
        static Abilities zero() {
            var values=new EnumMap<Field,Long>(Field.class);
            for(var field:Field.values())values.put(field,0L);
            return new Abilities(values);
        }
    }
    record Vip(long catalogId,long exclusiveType,long iconId,long polymorphId,Abilities abilities) {
        Vip {
            if(catalogId<1||exclusiveType<0||iconId<0||polymorphId<0)throw invalid();
            Objects.requireNonNull(abilities);
        }
        @Override public String toString(){return "VipCatalogEntry[redacted]";}
    }
    record Polymorph(long graphicsId,boolean present,boolean noDeathExpLoss,
                     boolean retainsShapeOnCancellation,Abilities abilities) {
        Polymorph {
            if(graphicsId<1)throw invalid();Objects.requireNonNull(abilities);
            if(!present&&(noDeathExpLoss||retainsShapeOnCancellation||!abilities.equals(Abilities.zero())))throw invalid();
        }
        /** Only after a successful complete query proves absence in the table. */
        static Polymorph absent(long graphicsId){return new Polymorph(graphicsId,false,false,false,Abilities.zero());}
        @Override public String toString(){return "VipPolymorphEntry[redacted]";}
    }
    record Catalog(int sourceId,String database,Map<Long,Vip> vips,Map<Long,Polymorph> polymorphs,java.util.Set<Long> absentVips) {
        Catalog(int sourceId,String database,Map<Long,Vip> vips,Map<Long,Polymorph> polymorphs) {
            this(sourceId,database,vips,polymorphs,java.util.Set.of());
        }
        Catalog {
            if(sourceId<1)throw invalid();ModernPhysicalCombat.identifier(database);
            Objects.requireNonNull(vips);Objects.requireNonNull(polymorphs);Objects.requireNonNull(absentVips);
            if(vips.size()+absentVips.size()>ModernCharacterData.MAX_ITEMS||polymorphs.size()>vips.size())throw invalid();
            for(var id:absentVips)if(id==null||id<1||vips.containsKey(id))throw invalid();
            var expected=new java.util.HashSet<Long>();
            for(var entry:vips.entrySet()) {
                if(entry.getKey()==null||entry.getValue()==null||entry.getKey()!=entry.getValue().catalogId())throw invalid();
                if(entry.getValue().polymorphId()>0)expected.add(entry.getValue().polymorphId());
            }
            if(!expected.equals(polymorphs.keySet()))throw invalid();
            for(var entry:polymorphs.entrySet())if(entry.getValue()==null||entry.getKey()!=entry.getValue().graphicsId())throw invalid();
            vips=Map.copyOf(new TreeMap<>(vips));polymorphs=Map.copyOf(new TreeMap<>(polymorphs));absentVips=java.util.Set.copyOf(absentVips);
        }
        /** Current configuration only. Never subtract this from persisted AC:
         * saved shape may differ, and an already-applied template can predate reload. */
        long configuredArmorContribution(long catalogId) {
            var vip=Objects.requireNonNull(vips.get(catalogId));
            long poly=vip.polymorphId()==0?0:polymorphs.get(vip.polymorphId()).abilities().get(Field.AC);
            return Math.addExact(vip.abilities().get(Field.AC),poly);
        }
        @Override public String toString(){return "VipSourceCatalog[redacted]";}
    }
    /** The existing import contract preserves saved total AC and reconstructs
     * current six abilities/maxima from saved BASE values. A known, inactive
     * VIP needs no active executor/shape and is covered only when BOTH of its
     * currently configured AC layers are independently zero. Do not accept
     * cancellation between VIP AC and a polymorph which may never have applied.
     * This retains saved AC verbatim; it does not reconstruct historical buffs
     * or subtract a guessed pre-reload template. Active VIPs still require the
     * separate full attribute/visual/equipment-transition implementation. */
    static boolean unresolvedCustom(ModernPhysicalCombat.Source source,ModernCharacterData.InventoryItem item,
                                    boolean effectiveEquipped) {
        var policy=source.templates().get(item.template().catalogId());
        if(!policy.custom())return false;
        if(effectiveEquipped||!item.equipped()||item.template().kind()!=ModernCharacterData.ItemKind.ETC
                ||!policy.className().equals("add.VIP")||source.vipCatalog()==null)return true;
        var vip=source.vipCatalog().vips().get(item.template().catalogId());
        if(vip==null||vip.abilities().get(Field.AC)!=0)return true;
        return vip.polymorphId()!=0&&source.vipCatalog().polymorphs().get(vip.polymorphId()).abilities().get(Field.AC)!=0;
    }
    private ModernVipEquipment(){}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid qualified VIP source");}
}
