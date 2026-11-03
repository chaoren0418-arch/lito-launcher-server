package local.lito381.server;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Full source-bound armor proc facts, NOT a live combat capability.
 * Preserve i64 SQL values; never reproduce classic getInt truncation. */
final class ModernArmorProcs {
    static final int MAX_ROWS=1024;
    static final List<String> COLUMNS=List.of("id","item_id","enchant_level","proc_group",
            "on_attack_chance","on_hurt_chance","flat_damage","rand_damage","heal_hp","heal_hp_rand",
            "heal_mp","heal_mp_rand","heal_mode","trigger_skill","skill_time_sec","skill_target","gfx_id","gfx_mode");

    record Rule(long id,long catalogId,long enchantment,long group,long attackChance,long hurtChance,
            long flatDamage,long randomDamage,long hp,long randomHp,long mp,long randomMp,long healMode,
            long skill,long seconds,long skillTarget,long graphics,long graphicsMode) {
        Rule {
            if(id<1||catalogId<1||enchantment< -1||group<0)throw invalid();
            // Effect values and selector modes are facts, not execution approval.
            // Unknown modes remain visible to a later strict feature qualifier.
        }
        int chance(boolean hurt) { return (int)Math.max(0,Math.min(100,hurt?hurtChance:attackChance)); }
        @Override public String toString(){return "ArmorProcRule[redacted]";}
    }
    private static final Comparator<Rule> ORDER=Comparator.comparingLong(Rule::enchantment)
            .thenComparingLong(Rule::group).thenComparingLong(Rule::id);

    /** Empty lists are certified absence only after the complete SELECT succeeds. */
    record Catalog(int sourceId,String database,Map<Long,List<Rule>> byItem) {
        Catalog {
            if(sourceId<1)throw invalid();ModernPhysicalCombat.identifier(database);Objects.requireNonNull(byItem);
            if(byItem.size()>ModernCharacterData.MAX_ITEMS)throw invalid();
            var copy=new TreeMap<Long,List<Rule>>();var unique=new HashSet<Long>();int count=0;
            for(var entry:byItem.entrySet()) {
                if(entry.getKey()==null||entry.getKey()<1||entry.getValue()==null)throw invalid();
                var rows=List.copyOf(entry.getValue());
                for(var rule:rows) {
                    if(rule.catalogId()!=entry.getKey()||!unique.add(rule.id())||++count>MAX_ROWS)throw invalid();
                }
                copy.put(entry.getKey(),rows.stream().sorted(ORDER).toList());
            }
            byItem=Map.copyOf(copy);
        }
        Set<Long> presentIds() {
            var result=new HashSet<Long>();byItem.forEach((id,rows)->{if(!rows.isEmpty())result.add(id);});
            return Set.copyOf(result);
        }
        /** EquipProcTable: ALL exact-stage rows, otherwise ALL wildcard-stage rows.
         * proc_group does not select one winner; zero-chance exact rows still suppress fallback. */
        List<Rule> selected(long catalogId,long equippedEnchantment) {
            var rows=byItem.get(catalogId);if(rows==null)throw invalid();
            if(equippedEnchantment>=0) {
                var exact=rows.stream().filter(row->row.enchantment()==equippedEnchantment).toList();
                if(!exact.isEmpty())return exact;
            }
            return rows.stream().filter(row->row.enchantment()== -1).toList();
        }
        @Override public String toString(){return "ArmorProcCatalog[redacted]";}
    }

    /** Classic percentage semantics, with exactly one bounded draw when chance > 0. */
    static boolean roll(Rule rule,boolean hurt,ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(rule);Objects.requireNonNull(random);int chance=rule.chance(hurt);
        if(chance==0)return false;long value=random.nextLong(100);
        if(value<0||value>=100)throw invalid();return value<chance;
    }
    /** Inclusive random range, widened rather than saturating to native int. */
    static long amount(long fixed,long randomMaximum,ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(random);long base=Math.max(0,fixed);
        if(randomMaximum<=0)return base;
        Math.addExact(base,randomMaximum); // The entire configured range must fit before drawing.
        long bound=Math.incrementExact(randomMaximum),draw=random.nextLong(bound);
        if(draw<0||draw>=bound)throw invalid();return Math.addExact(base,draw);
    }
    private ModernArmorProcs(){}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid qualified armor proc facts");}
}
