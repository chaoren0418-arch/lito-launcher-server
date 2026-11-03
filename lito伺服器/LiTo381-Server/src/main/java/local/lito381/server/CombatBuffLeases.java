package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Owned only by one BattleAbilityStatus. Bounded skill IDs, no actor references. */
final class CombatBuffLeases {
    private static final List<String> WEAPON_GROUPS=List.of("WEAPON","FANG");
    private static final Map<Integer,String> GROUPS=Map.ofEntries(Map.entry(3,"AC"),Map.entry(99,"AC"),Map.entry(151,"AC"),
            Map.entry(42,"STR"),Map.entry(109,"STR"),Map.entry(26,"DEX"),Map.entry(110,"DEX"),Map.entry(114,"AURA"),Map.entry(115,"AURA"),
            Map.entry(8,"WEAPON"),Map.entry(12,"WEAPON"),Map.entry(48,"WEAPON"),Map.entry(107,"FANG"),Map.entry(21,"ARMOR"));
    private final Map<Integer,Long> deadlines=new LinkedHashMap<>();
    static boolean loaded(){return CombatDatabase.controlsOrNull()!=null;}
    static boolean validArmDuration(int seconds){return loaded()?seconds>0&&seconds<=65535:seconds==12;}
    static int value(int legacy,int skill,String type) {
        var source=CombatDatabase.controlsOrNull();
        return source==null?legacy:SourceSkillControls.ability(source,skill,type);
    }
    static BattleAbilityStatus.ArmorProtection replacement(BattleAbilityStatus.ArmorProtection old) {
        var source=CombatDatabase.controlsOrNull();
        return source!=null&&source.skillCoexist()?new BattleAbilityStatus.ArmorProtection(0,0,0):old;
    }
    static CombatBuffLeases owned(BattleAbilityStatus status) {
        return (CombatBuffLeases)CombatAccess.field(status,"sourceBuffLeases");
    }
    static BattleAbilityStatus.ArmorProtection autoArmorProtection(BattleAbilityStatus status,long now) {
        var source=CombatDatabase.controlsOrNull();var leases=owned(status);
        if(source!=null&&source.skillCoexist()&&leases.contains(3,now))return new BattleAbilityStatus.ArmorProtection(3,0,1);
        return status.armorProtection(now);
    }
    static int autoStrengthSkillId(BattleAbilityStatus status,long now) {
        var source=CombatDatabase.controlsOrNull();
        return source!=null&&source.skillCoexist()&&owned(status).contains(42,now)?42:status.strengthSkillId(now);
    }
    static int autoDexteritySkillId(BattleAbilityStatus status,long now) {
        var source=CombatDatabase.controlsOrNull();
        return source!=null&&source.skillCoexist()&&owned(status).contains(26,now)?26:status.dexteritySkillId(now);
    }
    static BattleAwakenStatus.Snapshot onDeath(BattleAwakenStatus.Snapshot previous,ClientSession caster) {
        if(!loaded())return previous;
        // Source Death invokes CANCELLATION with isDead=true; that removes
        // player skills even when they normally resist Cancellation. Port the
        // owned ability family and the scalar BUFF families handled here.
        ((BattleAbilityStatus)CombatAccess.field(caster,"abilityStatus")).clear();
        var timed=(BattleTimedStatus)CombatAccess.field(caster,"timedStatus");timed.expireWeakness(Long.MAX_VALUE);timed.expireDisease(Long.MAX_VALUE);
        for(String field:new String[]{"earthBlessStatus","ironSkinStatus","clearMindStatus","fireWeaponStatus",
                "fireBlessStatus","windShotStatus","stormEyeStatus","stormShotStatus","burningWeaponStatus"})
            CombatAccess.call(CombatAccess.field(caster,field),"clear","()V");
        return previous;
    }
    synchronized boolean contains(int skill,long now){prune(now);return deadlines.containsKey(skill);}
    synchronized void activate(int skill,long now,int seconds) {
        var source=CombatDatabase.controlsOrNull();if(source==null)return;
        String group=GROUPS.get(skill);
        if(group==null||now<0||seconds<1||seconds>65535)throw new IllegalArgumentException("invalid source buff lease="+skill);
        prune(now);
        if(!source.skillCoexist())deadlines.keySet().removeIf(id->GROUPS.get(id).equals(group));
        deadlines.remove(skill); // refresh the one ID; insertion order is presentation identity.
        deadlines.put(skill,Math.addExact(now,TimeUnit.SECONDS.toNanos(seconds)));
    }
    synchronized int replace(int legacy,int skill,long now,int seconds) {
        activate(skill,now,seconds);
        var source=CombatDatabase.controlsOrNull();return source!=null&&source.skillCoexist()?0:legacy;
    }
    synchronized int put(int skill,long now,int seconds) {
        String group=GROUPS.get(skill);if(group==null)throw new IllegalArgumentException("unsupported source buff="+skill);
        int previous=skill(group,now);return replace(previous,skill,now,seconds);
    }
    synchronized boolean remove(boolean legacy,int skill) {return deadlines.remove(skill)!=null||legacy;}
    synchronized void clear(){deadlines.clear();}
    synchronized void cancel(){var source=CombatDatabase.controlsOrNull();if(source==null)deadlines.clear();else deadlines.keySet().removeIf(id->!source.cancellationImmune().contains(id));}
    synchronized BattleWeaponBuffStatus.Modifier weapon(long now) {
        prune(now);int id=0,remaining=0,hit=0,damage=0,bowHit=0,bowDamage=0;
        for(String group:WEAPON_GROUPS) {
            int selected=skill(group,now);if(selected!=0)id=selected;
            remaining=Math.max(remaining,remaining(group,now));
            hit=Math.addExact(hit,amount(group,"HIT"));damage=Math.addExact(damage,amount(group,"DMG"));
            bowHit=Math.addExact(bowHit,amount(group,"BOW_HIT"));bowDamage=Math.addExact(bowDamage,amount(group,"BOW_DMG"));
        }
        return new BattleWeaponBuffStatus.Modifier(id,hit,damage,bowHit,bowDamage,remaining);
    }
    synchronized int armor(long now){prune(now);return amount("ARMOR","AC");}
    synchronized BattleItemArmorEnchantStatus.Modifier armorModifier(long now,long item) {
        int id=skill("ARMOR",now);return new BattleItemArmorEnchantStatus.Modifier(id,item,amount("ARMOR","AC"),remaining("ARMOR",now));
    }
    private void prune(long now){if(now<0)throw new IllegalArgumentException("negative source buff time");deadlines.values().removeIf(deadline->now>=deadline);}
    private int skill(String group,long now) {
        prune(now);int result=0;for(int id:deadlines.keySet())if(GROUPS.get(id).equals(group))result=id;return result;
    }
    private int remaining(String group,long now) {
        long deadline=0;for(var e:deadlines.entrySet())if(GROUPS.get(e.getKey()).equals(group))deadline=Math.max(deadline,e.getValue());
        return deadline<=now?0:(int)Math.min(65535,Math.max(1,(deadline-now+999_999_999L)/1_000_000_000L));
    }
    private int amount(String group,String type) {
        int amount=0;var source=CombatDatabase.controlsOrNull();
        for(int id:deadlines.keySet())if(GROUPS.get(id).equals(group))amount=Math.addExact(amount,SourceSkillControls.ability(source,id,type));
        return amount;
    }
    synchronized BattleAbilityStatus.Modifier shield(long now){return modifier("AC",now);}
    synchronized BattleAbilityStatus.Modifier strength(long now){return modifier("STR",now);}
    synchronized BattleAbilityStatus.Modifier dexterity(long now){return modifier("DEX",now);}
    private BattleAbilityStatus.Modifier modifier(String group,long now) {
        prune(now);return new BattleAbilityStatus.Modifier(amount(group,group),remaining(group,now));
    }
    synchronized int strengthSkillId(long now){return skill("STR",now);}
    synchronized int dexteritySkillId(long now){return skill("DEX",now);}
    synchronized BattleAbilityStatus.ArmorProtection armorProtection(long now) {
        int id=skill("AC",now);return new BattleAbilityStatus.ArmorProtection(id,amount("AC","AC"),remaining("AC",now));
    }
    synchronized BattleAbilityStatus.RoyalAura royalAura(long now) {
        int id=skill("AURA",now);
        // GLOWING_AURA retains a direct +20 MR in L1SkillUse; it is additional
        // to table-controlled HIT/BOW_HIT, including when both auras coexist.
        return new BattleAbilityStatus.RoyalAura(id,amount("AURA","AC"),amount("AURA","HIT"),amount("AURA","BOW_HIT"),
                deadlines.containsKey(114)?20:0,remaining("AURA",now));
    }
}
