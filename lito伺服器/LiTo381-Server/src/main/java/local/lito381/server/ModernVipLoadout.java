package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Complete current VIP contribution, not a combat/equip/visual permission.
 * Reconciles the exact owner/source/revision and does not mutate a character.
 * Saved total AC is deliberately NOT reconstructed by subtracting today's
 * template; its historical shape/applied template may differ after reload.
 * A future atomic lifecycle must retain these applied templates for removal.
 * The caller explicitly supplies the current VIP shape (zero means none),
 * never infer it from inventory order or stack every equipped VIP's shape. */
final class ModernVipLoadout {
    record Applied(long objectId, ModernVipEquipment.Vip template, ModernItemLifetime.View lifetime,
            ModernEquipmentOverrideStore.Fingerprint sourceItem) {
        Applied {
            if(objectId<1||template==null||lifetime==null||sourceItem==null||lifetime.itemObjectId()!=objectId||!lifetime.usable())throw invalid();
        }
        @Override public String toString(){return "AppliedVipCandidate[redacted]";}
    }
    private final TournamentPresence.CharacterKey owner;
    private final ModernPhysicalCombat.Source qualifiedSource;
    private final Optional<ModernEquipmentOverrideStore.Stored> qualifiedStored;
    private final ModernEquipmentReconciliation.ExpectedBinding binding;
    private final String database;
    private final long equipmentRevision,evaluatedAtMillis,validUntilMillis;
    private final List<Applied> items;
    private final ModernVipEquipment.Abilities vipAbilities,polymorphAbilities,total;
    private final ModernEquipmentFacts.Ability legacyAlreadyEvaluated;
    private final ModernVipEquipment.Polymorph shape;
    private final Set<Long> icons;
    private final Map<Long,Boolean> appliedFlags;
    private final Map<Long,ModernItemLifetime.View> pinnedLifetimes;

    private ModernVipLoadout(ModernPhysicalCombat.Source source,Optional<ModernEquipmentOverrideStore.Stored> stored,
            ModernEquipmentReconciliation.ExpectedBinding binding,long revision,long now,long validUntil,String database,List<Applied> items,
            ModernVipEquipment.Abilities vipAbilities,ModernVipEquipment.Polymorph shape,
            ModernEquipmentFacts.Ability legacyAlreadyEvaluated,Set<Long> icons) {
        this(source,stored,binding,revision,now,validUntil,database,items,vipAbilities,shape,legacyAlreadyEvaluated,icons,Map.of(),Map.of());
    }
    private ModernVipLoadout(ModernPhysicalCombat.Source source,Optional<ModernEquipmentOverrideStore.Stored> stored,
            ModernEquipmentReconciliation.ExpectedBinding binding,long revision,long now,long validUntil,String database,List<Applied> items,
            ModernVipEquipment.Abilities vipAbilities,ModernVipEquipment.Polymorph shape,
            ModernEquipmentFacts.Ability legacyAlreadyEvaluated,Set<Long> icons,Map<Long,Boolean> appliedFlags,
            Map<Long,ModernItemLifetime.View> pinnedLifetimes) {
        qualifiedSource=source;qualifiedStored=stored;this.owner=binding.key();
        this.binding=binding;
        this.database=database;equipmentRevision=revision;evaluatedAtMillis=now;
        validUntilMillis=validUntil;this.items=List.copyOf(items);this.vipAbilities=vipAbilities;
        this.shape=shape;polymorphAbilities=shape==null?ModernVipEquipment.Abilities.zero():shape.abilities();
        total=vipAbilities.plus(polymorphAbilities);this.legacyAlreadyEvaluated=legacyAlreadyEvaluated;this.icons=Set.copyOf(icons);
        this.appliedFlags=Map.copyOf(appliedFlags);this.pinnedLifetimes=Map.copyOf(pinnedLifetimes);
    }

    /** Rebuild from an opaque applied ledger, not today's reloaded VIP catalog.
     * The same pinned source supplies ordinary equipment/base attributes. */
    static ModernVipLoadout fromApplied(ModernPhysicalCombat.Source source,
            Optional<ModernEquipmentOverrideStore.Stored> stored,ModernVipLifecycle.State state,
            Map<Long,ModernItemLifetime.View> knownLifetimes,long now) {
        var q=source.input().equipment();var binding=state.binding();
        var approved=ModernEquipmentReconciliation.reconcile(q,binding,stored,now);
        if(now<state.observedMillis()||approved.expected().revision()!=state.equipmentRevision()
                ||!q.catalog().resourceDatabase().equals(state.database()))throw invalid();
        var flags=new TreeMap<Long,Boolean>();var lives=new TreeMap<Long,ModernItemLifetime.View>();
        for(var item:q.source().inventory())if(isVip(source,item.objectId())) {
            var known=knownLifetimes.get(item.objectId());if(known==null||known.itemObjectId()!=item.objectId())throw invalid();
            lives.put(item.objectId(),at(known,now));flags.put(item.objectId(),false);
        }
        if(!lives.keySet().equals(knownLifetimes.keySet()))throw invalid();
        var abilities=ModernVipEquipment.Abilities.zero();
        for(var item:state.items()) {
            var instance=q.items().get(item.objectId());var life=lives.get(item.objectId());
            if(instance==null||life==null||!life.usable()||!life.equals(item.lifetime())
                    ||item.template().catalogId()!=instance.source().template().catalogId()
                    ||!item.sourceItem().equals(approved.sourceFingerprints().get(item.objectId()))
                    ||!approved.overrides().getOrDefault(item.objectId(),instance.source().equipped()))throw invalid();
            flags.put(item.objectId(),true);abilities=abilities.plus(item.template().abilities());
        }
        long until=deadline(source,approved,flags,lives,now);
        return new ModernVipLoadout(source,stored,binding,state.equipmentRevision(),now,until,state.database(),state.items(),
                abilities,state.shape().orElse(null),legacyProjection(abilities),state.icons(),flags,lives);
    }

    /** Minimum leases survive unequip/expiry; a new SQL observation is not a
     * renewal permission for the same raw item lifetime facts. */
    ModernVipLoadout withLifetimeCeilings(Map<Long,ModernItemLifetime.View> ceilings) {
        var capped=new ArrayList<Applied>();long until=validUntilMillis;
        for(var item:items) {
            var old=ceilings.get(item.objectId());if(old==null)throw invalid();
            var life=earlier(old,item.lifetime(),evaluatedAtMillis);if(!life.usable())throw invalid();
            capped.add(new Applied(item.objectId(),item.template(),life,item.sourceItem()));
            if(life.state()==ModernItemLifetime.State.ACTIVE)until=minimum(until,life.expiresAtMillis());
        }
        return new ModernVipLoadout(qualifiedSource,qualifiedStored,binding,equipmentRevision,evaluatedAtMillis,until,database,
                capped,vipAbilities,shape,legacyAlreadyEvaluated,icons);
    }
    static ModernItemLifetime.View earlier(ModernItemLifetime.View a,ModernItemLifetime.View b,long now) {
        if(a.itemObjectId()!=b.itemObjectId())throw invalid();
        if(a.state()==ModernItemLifetime.State.PERMANENT&&b.state()==ModernItemLifetime.State.PERMANENT)return a;
        if(a.expiresAtMillis()<0||b.expiresAtMillis()<0)throw invalid();
        return at(new ModernItemLifetime.View(a.itemObjectId(),ModernItemLifetime.State.ACTIVE,
                Math.min(a.expiresAtMillis(),b.expiresAtMillis())),now);
    }
    private static ModernItemLifetime.View at(ModernItemLifetime.View life,long now) {
        if(life.expiresAtMillis()>=0)return new ModernItemLifetime.View(life.itemObjectId(),
                now>=life.expiresAtMillis()?ModernItemLifetime.State.EXPIRED:ModernItemLifetime.State.ACTIVE,life.expiresAtMillis());
        if(life.state()!=ModernItemLifetime.State.PERMANENT)throw invalid();
        return life;
    }
    static boolean isVip(ModernPhysicalCombat.Source source,long id) {
        var item=source.input().equipment().items().get(id);
        return item!=null&&item.source().template().kind()==ModernCharacterData.ItemKind.ETC
                &&source.templates().get(item.source().template().catalogId()).className().equals("add.VIP");
    }
    private static long deadline(ModernPhysicalCombat.Source source,ModernEquipmentReconciliation.Result approved,
            Map<Long,Boolean> flags,Map<Long,ModernItemLifetime.View> lives,long now) {
        long until=-1;var q=source.input().equipment();
        for(var item:approved.desiredLoadout().items())if(flags.getOrDefault(item.objectId(),item.equipped())) {
            var life=lives.getOrDefault(item.objectId(),q.lifetimes().view(item.objectId(),now));
            if(!life.usable())throw invalid();
            if(life.state()==ModernItemLifetime.State.ACTIVE)until=minimum(until,life.expiresAtMillis());
            var instance=q.items().get(item.objectId());var stone=instance.stone();
            if(instance.source().template().kind()!=ModernCharacterData.ItemKind.ETC&&stone.active(now)&&stone.expiresAtEpochMillis().isPresent())
                until=minimum(until,stone.expiresAtEpochMillis().getAsLong());
        }
        return until;
    }
    private static long minimum(long a,long b){return a<0?b:Math.min(a,b);}
    ModernPhysicalCombat.Source source(){return qualifiedSource;}
    Optional<ModernEquipmentOverrideStore.Stored> stored(){return qualifiedStored;}
    Map<Long,Boolean> effectiveOverrides(Map<Long,Boolean> reconciled) {
        var result=new TreeMap<>(reconciled);result.putAll(appliedFlags);return Map.copyOf(result);
    }
    ModernItemLifetime.View lifetime(long id,long now) {
        var life=pinnedLifetimes.get(id);return life==null?qualifiedSource.input().equipment().lifetimes().view(id,now):at(life,now);
    }

    static ModernVipLoadout qualify(ModernPhysicalCombat.Source source,
            ModernEquipmentReconciliation.ExpectedBinding binding,Optional<ModernEquipmentOverrideStore.Stored> stored,
            long now,long currentVipShape) {
        return qualify(source,binding,stored,now,currentVipShape,false);
    }
    /** Explicit reconstructed-character path; cannot fail merely because a
     * saved opaque TOTAL AC or a partial old VIP subtotal cannot be reused. */
    static ModernVipLoadout qualifyForRebuild(ModernPhysicalCombat.Source source,
            ModernEquipmentReconciliation.ExpectedBinding binding,Optional<ModernEquipmentOverrideStore.Stored> stored,
            long now,long currentVipShape) {
        return qualify(source,binding,stored,now,currentVipShape,true);
    }
    /** Equipment proposals retain the earliest known VIP leases BEFORE
     * selecting flags. A later DB observation cannot resurrect an expired
     * sibling merely because its raw SQL equipped flag is still true. */
    static ModernVipLoadout qualifyForChange(ModernPhysicalCombat.Source source,
            ModernEquipmentReconciliation.ExpectedBinding binding,Optional<ModernEquipmentOverrideStore.Stored> stored,
            long now,long shape,Map<Long,ModernItemLifetime.View> ceilings) {
        var ids=new HashSet<Long>();
        for(var item:source.input().equipment().source().inventory())if(isVip(source,item.objectId()))ids.add(item.objectId());
        if(!ids.equals(ceilings.keySet()))throw invalid();
        return qualify(source,binding,stored,now,shape,true,ceilings);
    }
    private static ModernVipLoadout qualify(ModernPhysicalCombat.Source source,
            ModernEquipmentReconciliation.ExpectedBinding binding,Optional<ModernEquipmentOverrideStore.Stored> stored,
            long now,long currentVipShape,boolean rebuild) {
        return qualify(source,binding,stored,now,currentVipShape,rebuild,Map.of());
    }
    private static ModernVipLoadout qualify(ModernPhysicalCombat.Source source,
            ModernEquipmentReconciliation.ExpectedBinding binding,Optional<ModernEquipmentOverrideStore.Stored> stored,
            long now,long currentVipShape,boolean rebuild,Map<Long,ModernItemLifetime.View> ceilings) {
        Objects.requireNonNull(source);
        if(currentVipShape<0)throw invalid();
        var equipment=source.input().equipment();
        var lives=new TreeMap<Long,ModernItemLifetime.View>();
        for(var item:equipment.source().inventory()) {
            var life=equipment.lifetimes().view(item.objectId(),now);var ceiling=ceilings.get(item.objectId());
            lives.put(item.objectId(),ceiling==null?life:earlier(ceiling,life,now));
        }
        List<ModernEquipmentAuthority.Equipped> flags;long revision,validUntil;
        if(rebuild) {
            var reconciled=ModernEquipmentReconciliation.reconcile(equipment,binding,stored,now);
            flags=reconciled.desiredLoadout().items().stream().map(i->new ModernEquipmentAuthority.Equipped(i.objectId(),i.equipped()&&lives.get(i.objectId()).usable())).toList();
            revision=reconciled.expected().revision();validUntil=-1;
            for(var flag:flags)if(flag.equipped()) {
                var life=lives.get(flag.objectId());var stone=equipment.items().get(flag.objectId()).stone();
                if(life.state()==ModernItemLifetime.State.ACTIVE)validUntil=validUntil<0?life.expiresAtMillis():Math.min(validUntil,life.expiresAtMillis());
                if(stone.active(now)&&stone.expiresAtEpochMillis().isPresent()
                        &&equipment.items().get(flag.objectId()).source().template().kind()!=ModernCharacterData.ItemKind.ETC)
                    validUntil=validUntil<0?stone.expiresAtEpochMillis().getAsLong():Math.min(validUntil,stone.expiresAtEpochMillis().getAsLong());
            }
        } else {
            var profile=ModernCombatProfile.qualify(source.input(),binding,stored,now,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
            flags=profile.equipped();revision=profile.equipmentRevision();validUntil=profile.validUntilMillis();
        }
        var catalog=source.vipCatalog();
        // Every carried VIP needs complete facts, even when not yet equipped.
        for(var item:equipment.source().inventory()) {
            var policy=source.templates().get(item.template().catalogId());
            if(item.template().kind()==ModernCharacterData.ItemKind.ETC&&policy.className().equals("add.VIP")) {
                if(catalog==null||!catalog.vips().containsKey(item.template().catalogId()))throw invalid();
                var life=lives.get(item.objectId());
                if(!life.usable()&&life.state()!=ModernItemLifetime.State.EXPIRED)throw invalid();
            }
        }
        var applied=new ArrayList<Applied>();var types=new HashSet<Long>();var icons=new HashSet<Long>();
        var abilities=ModernVipEquipment.Abilities.zero();
        var legacy=ModernEquipmentFacts.Ability.zero();
        ModernVipEquipment.Polymorph shape=null;
        for(var flag:flags) {
            if(!flag.equipped())continue;
            var item=equipment.items().get(flag.objectId()).source();
            if(item.template().kind()!=ModernCharacterData.ItemKind.ETC
                    ||!source.templates().get(item.template().catalogId()).className().equals("add.VIP"))continue;
            var vip=catalog.vips().get(item.template().catalogId());
            if(!types.add(vip.exclusiveType()))throw invalid(); // Type zero is also exclusive in add.VIP.
            var projected=legacyProjection(vip.abilities());
            var oldRule=equipment.catalog().require(new ModernEquipmentFacts.RuleKey(ModernEquipmentFacts.RuleKind.VIP,
                    ModernCharacterData.ItemKind.ETC,item.template().catalogId(),0));
            if(!oldRule.present()||!projected.equals(projectLegacyRule(oldRule.raw())))throw invalid();
            // Separate SELECTs are not a SQL snapshot. Refuse observed overlap
            // disagreement rather than combining two generations of abilities.
            applied.add(new Applied(item.objectId(),vip,lives.get(item.objectId()),
                    ModernEquipmentReconciliation.fingerprint(equipment.items().get(item.objectId()),equipment.lifetimes().items().get(item.objectId()))));
            abilities=abilities.plus(vip.abilities());legacy=legacy.plus(projected);
            if(vip.iconId()!=0)icons.add(vip.iconId());
            if(currentVipShape!=0&&vip.polymorphId()==currentVipShape)shape=catalog.polymorphs().get(currentVipShape);
        }
        if(currentVipShape!=0&&shape==null)throw invalid();
        applied.sort(java.util.Comparator.comparingLong(Applied::objectId));
        return new ModernVipLoadout(source,stored,binding,revision,now,validUntil,equipment.catalog().resourceDatabase(),applied,abilities,shape,legacy,icons);
    }

    void require(ModernCombatProfile.Input input,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long now) {
        if(qualifiedSource.input()!=input||!this.binding.equals(binding)||!qualifiedStored.equals(stored)||now!=evaluatedAtMillis)throw invalid();
    }
    /** Common attributes only. Generic damage reduction, magic-only reduction,
     * EXP and ADENA rates remain distinct full fields, never guessed into an
     * unrelated legacy REDUCTION/MAGIC_DAMAGE statistic. */
    ModernEquipmentFacts.Ability commonAbilities() {
        var result=legacyProjection(total);
        return result.with(ModernEquipmentFacts.Stat.CHA,total.get(ModernVipEquipment.Field.CHA))
                .with(ModernEquipmentFacts.Stat.MAX_HP,total.get(ModernVipEquipment.Field.MAX_HP))
                .with(ModernEquipmentFacts.Stat.MAX_MP,total.get(ModernVipEquipment.Field.MAX_MP))
                .with(ModernEquipmentFacts.Stat.AC,total.get(ModernVipEquipment.Field.AC));
    }

    /** Exact overlap with the retained LegacyEquipmentMagicLoader evaluator.
     * Missing CHA/HP/MP/AC/reductions/rates are intentionally NOT silently mapped
     * into a similarly named combat stat. Full fields remain in total(). */
    static ModernEquipmentFacts.Ability legacyProjection(ModernVipEquipment.Abilities abilities) {
        var result=ModernEquipmentFacts.Ability.zero();
        for(var field:ModernVipEquipment.Field.values()) {
            ModernEquipmentFacts.Stat stat=switch(field) {
                case STR->ModernEquipmentFacts.Stat.STR;case DEX->ModernEquipmentFacts.Stat.DEX;
                case CON->ModernEquipmentFacts.Stat.CON;case INT->ModernEquipmentFacts.Stat.INT;case WIS->ModernEquipmentFacts.Stat.WIS;
                case HPR->ModernEquipmentFacts.Stat.HP_REGEN;case MPR->ModernEquipmentFacts.Stat.MP_REGEN;
                case MR->ModernEquipmentFacts.Stat.MR;case SP->ModernEquipmentFacts.Stat.SP;
                case MELEE_DAMAGE->ModernEquipmentFacts.Stat.MELEE_DAMAGE;case MELEE_HIT->ModernEquipmentFacts.Stat.MELEE_HIT;
                case RANGED_DAMAGE->ModernEquipmentFacts.Stat.RANGED_DAMAGE;case RANGED_HIT->ModernEquipmentFacts.Stat.RANGED_HIT;
                case FIRE->ModernEquipmentFacts.Stat.FIRE;case WIND->ModernEquipmentFacts.Stat.WIND;
                case EARTH->ModernEquipmentFacts.Stat.EARTH;case WATER->ModernEquipmentFacts.Stat.WATER;
                case STUN->ModernEquipmentFacts.Stat.STUN;case STONE->ModernEquipmentFacts.Stat.STONE;
                case SLEEP->ModernEquipmentFacts.Stat.SLEEP;case FREEZE->ModernEquipmentFacts.Stat.FREEZE;
                case SUSTAIN->ModernEquipmentFacts.Stat.SUSTAIN;case BLIND->ModernEquipmentFacts.Stat.BLIND;
                case CHA,AC,MAX_HP,MAX_MP,DAMAGE_REDUCTION,MAGIC_REDUCTION,EXP_RATE,ADENA_RATE->null;
            };
            if(stat!=null)result=result.with(stat,abilities.get(field));
        }
        return result;
    }
    private static ModernEquipmentFacts.Ability projectLegacyRule(ModernEquipmentFacts.Ability rule) {
        var result=ModernEquipmentFacts.Ability.zero();
        for(var stat:ModernEquipmentFacts.Stat.values()) {
            boolean retained=switch(stat) {
                case STR,DEX,CON,INT,WIS,HP_REGEN,MP_REGEN,MR,SP,MELEE_DAMAGE,MELEE_HIT,RANGED_DAMAGE,RANGED_HIT,
                        FIRE,WIND,EARTH,WATER,STUN,STONE,SLEEP,FREEZE,SUSTAIN,BLIND->true;
                default->false;
            };
            if(retained)result=result.with(stat,rule.get(stat));
        }
        return result;
    }
    TournamentPresence.CharacterKey owner(){return owner;}
    ModernEquipmentReconciliation.ExpectedBinding binding(){return binding;}
    String database(){return database;}
    long equipmentRevision(){return equipmentRevision;}
    long evaluatedAtMillis(){return evaluatedAtMillis;}
    long validUntilMillis(){return validUntilMillis;}
    List<Applied> items(){return items;}
    ModernVipEquipment.Abilities vipAbilities(){return vipAbilities;}
    ModernVipEquipment.Abilities polymorphAbilities(){return polymorphAbilities;}
    ModernVipEquipment.Abilities total(){return total;}
    ModernEquipmentFacts.Ability legacyAlreadyEvaluated(){return legacyAlreadyEvaluated;}
    Optional<ModernVipEquipment.Polymorph> shape(){return Optional.ofNullable(shape);}
    Set<Long> icons(){return icons;}
    @Override public String toString(){return "QualifiedVipLoadoutCandidate[redacted]";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid qualified VIP loadout");}
}
