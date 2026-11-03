package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.Stat.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * NEW_MATCH_MELEE_V1, PC-versus-PC only. Server-owned immutable inputs and RNG;
 * no socket, SQL, clock, HP mutation, native int conversion or process globals.
 *
 * Rules explicitly select LiTo's traced Legacy equipment/VIP subset and AC/
 * defense core, with retained L1PcOriginal and seven-class level bonuses. Direct
 * REDUCTION is NOT reinterpreted as armor defense (nor a new outgoing bonus):
 * that preserves the traced LiTo loader output, not the divergent classic slot
 * switch. New matches import NO active skill/cooking/doll/talent/weight status,
 * no classic PvP configuration or elemental-affinity multiplier. This is a
 * named isolated ruleset, NOT all current classic combat or arbitrary proc.
 */
final class ModernPhysicalCombat {
    static final String RULESET = "NEW_MATCH_MELEE_V1";
    static final Set<Long> MELEE_TYPES = Set.of(1L,2L,3L,5L,6L,14L,15L);
    enum UnsupportedReason { NONE, WEAPON_UNQUALIFIED, RANGED, KIRINGKU, SPECIAL_WEAPON,
        ATTRIBUTE_ENCHANT, CUSTOM_EXECUTOR, EQUIPMENT_PROC, INVALID_MELEE_RANGE, ZERO_WEAPON_DIE, LIFETIME_UNQUALIFIED }
    enum Failure { UNSUPPORTED, INVALID_STATE, INVALID_RANDOM, ARITHMETIC_OVERFLOW }
    static final class CombatException extends IllegalArgumentException {
        private final Failure failure;
        CombatException(Failure failure){super("modern physical combat: "+failure);this.failure=failure;}
        Failure failure(){return failure;}
    }
    @FunctionalInterface interface BoundedRandom { long nextLong(long boundExclusive); }

    record GrowthRow(long id,long hit,long damage) { GrowthRow {if(id<1||id>127)throw invalid();} }
    record Growth(int sourceId,String database,List<GrowthRow> strength,List<GrowthRow> dexterity) {
        Growth {
            if(sourceId<1)throw invalid();identifier(database);
            strength=table(strength);dexterity=table(dexterity);
        }
        private static List<GrowthRow> table(List<GrowthRow> rows) {
            if(rows==null||rows.size()!=127)throw invalid();rows=List.copyOf(rows);
            for(int i=0;i<127;i++)if(rows.get(i).id()!=i+1)throw invalid();return rows;
        }
        // Explicit retained 1..127 lookup saturation. The original i64 ability
        // remains intact in Profile; only this bounded table index is an int.
        GrowthRow at(boolean ranged,long ability){return (ranged?dexterity:strength).get((int)Math.max(1,Math.min(127,ability))-1);}
        @Override public String toString(){return "PhysicalGrowth[redacted]";}
    }
    /** Exact bounded decimal multiplier; integer damage is truncated toward zero. */
    record Rational(long numerator,long denominator) {
        Rational {if(numerator<0||denominator<1||denominator>1_000_000)throw invalid();}
        static Rational decimal(BigDecimal number) {
            if(number==null||number.signum()<0)throw invalid();
            var value=number.stripTrailingZeros();int scale=Math.max(0,value.scale());
            if(scale>6)throw invalid();long denominator=1;
            for(int i=0;i<scale;i++)denominator=Math.multiplyExact(denominator,10);
            return new Rational(value.movePointRight(scale).longValueExact(),denominator);
        }
        long applyAtLeastOne(long damage) {
            if(damage<0)throw invalid();
            return numerator<=denominator?damage:Math.multiplyExact(damage,numerator)/denominator;
        }
        double asDouble(){return (double)numerator/denominator;}
    }
    record TemplatePolicy(ModernCharacterData.ItemKind kind,String className,Rational criticalMultiplier,
            OptionalLong hasteItem) {
        TemplatePolicy(ModernCharacterData.ItemKind kind,String className,Rational criticalMultiplier) {
            this(kind,className,criticalMultiplier,OptionalLong.empty());
        }
        TemplatePolicy {
            Objects.requireNonNull(kind);Objects.requireNonNull(hasteItem);
            if(kind==ModernCharacterData.ItemKind.ETC&&hasteItem.isPresent())throw invalid();
            if(!ModernCharacterData.validText(className)||className.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>256
                    ||(kind==ModernCharacterData.ItemKind.WEAPON)!=(criticalMultiplier!=null))throw invalid();
        }
        boolean custom(){return !className.equals("0");}
        @Override public String toString(){return "PhysicalTemplatePolicy[redacted]";}
    }
    record Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,
            ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows,Map<Long,Long> weaponArrowSprites,ModernNaturalRegen.Growth regenGrowth,ModernHonorHealing.Growth healingGrowth,
            ModernVipEquipment.Catalog vipCatalog,ModernArmorProcs.Catalog armorProcs,Set<Long> weaponProcCatalogIds) {
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,
                ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows,Map<Long,Long> weaponArrowSprites,ModernNaturalRegen.Growth regenGrowth,ModernHonorHealing.Growth healingGrowth,
                ModernVipEquipment.Catalog vipCatalog,ModernArmorProcs.Catalog armorProcs) {
            this(input,growth,templates,procCatalogIds,magicGrowth,magicBows,weaponArrowSprites,regenGrowth,healingGrowth,vipCatalog,armorProcs,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,
                ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows,Map<Long,Long> weaponArrowSprites,ModernNaturalRegen.Growth regenGrowth,ModernHonorHealing.Growth healingGrowth,
                ModernVipEquipment.Catalog vipCatalog) {
            this(input,growth,templates,procCatalogIds,magicGrowth,magicBows,weaponArrowSprites,regenGrowth,healingGrowth,vipCatalog,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,
                ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows,Map<Long,Long> weaponArrowSprites,ModernNaturalRegen.Growth regenGrowth,ModernHonorHealing.Growth healingGrowth) {
            this(input,growth,templates,procCatalogIds,magicGrowth,magicBows,weaponArrowSprites,regenGrowth,healingGrowth,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,
                ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows,Map<Long,Long> weaponArrowSprites,ModernNaturalRegen.Growth regenGrowth){
            this(input,growth,templates,procCatalogIds,magicGrowth,magicBows,weaponArrowSprites,regenGrowth,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,
                ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows,Map<Long,Long> weaponArrowSprites) {
            this(input,growth,templates,procCatalogIds,magicGrowth,magicBows,weaponArrowSprites,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,ModernMagicCombat.Growth magicGrowth,Map<Long,Long> magicBows) {
            this(input,growth,templates,procCatalogIds,magicGrowth,magicBows,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds,ModernMagicCombat.Growth magicGrowth) {
            this(input,growth,templates,procCatalogIds,magicGrowth,null);
        }
        Source(ModernCombatProfile.Input input,Growth growth,Map<Long,TemplatePolicy> templates,Set<Long> procCatalogIds) {
            this(input,growth,templates,procCatalogIds,null);
        }
        Source {
            Objects.requireNonNull(input);Objects.requireNonNull(growth);Objects.requireNonNull(templates);Objects.requireNonNull(procCatalogIds);
            var equipment=input.equipment();
            if(armorProcs!=null) {
                if(armorProcs.sourceId()!=equipment.source().sourceId()||!armorProcs.database().equals(equipment.catalog().resourceDatabase()))throw invalid();
                var expectedArmor=new java.util.HashSet<Long>();
                for(var item:equipment.source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.ARMOR
                        &&procCatalogIds.contains(item.template().catalogId()))expectedArmor.add(item.template().catalogId());
                if(!expectedArmor.equals(armorProcs.byItem().keySet()))throw invalid();
            }
            if(vipCatalog!=null) {
                if(vipCatalog.sourceId()!=equipment.source().sourceId()||!vipCatalog.database().equals(equipment.catalog().resourceDatabase()))throw invalid();
                var expectedVips=new java.util.HashSet<Long>();
                for(var item:equipment.source().inventory()) {
                    var policy=templates.get(item.template().catalogId());
                    if(item.template().kind()==ModernCharacterData.ItemKind.ETC
                            &&policy!=null&&policy.className().equals("add.VIP"))expectedVips.add(item.template().catalogId());
                }
                var observedVips=new java.util.HashSet<Long>(vipCatalog.vips().keySet());observedVips.addAll(vipCatalog.absentVips());
                if(!expectedVips.equals(observedVips))throw invalid();
            }
            if(healingGrowth!=null&&(healingGrowth.sourceId()!=equipment.source().sourceId()||!healingGrowth.database().equals(equipment.catalog().resourceDatabase())))throw invalid();
            if(equipment.source().sourceId()!=growth.sourceId()||!equipment.catalog().resourceDatabase().equals(growth.database())
                    ||templates.size()>512||procCatalogIds.size()>512)throw invalid();
            templates=Map.copyOf(templates);procCatalogIds=Set.copyOf(procCatalogIds);
            if(weaponProcCatalogIds!=null) {
                weaponProcCatalogIds=Set.copyOf(weaponProcCatalogIds);
                if(!procCatalogIds.containsAll(weaponProcCatalogIds))throw invalid();
            }
            var expected=new java.util.HashSet<Long>();
            for(var item:equipment.source().inventory()) {
                long catalog=item.template().catalogId();expected.add(catalog);var policy=templates.get(catalog);
                if(policy==null||policy.kind()!=item.template().kind())throw invalid();
                if(policy.kind()==ModernCharacterData.ItemKind.WEAPON&&input.weaponsQualified()
                        &&Double.compare(policy.criticalMultiplier().asDouble(),input.weapons().get(item.objectId()).criticalMultiplier())!=0)throw invalid();
            }
            if(!expected.equals(templates.keySet())||!expected.containsAll(procCatalogIds))throw invalid();
            if(magicGrowth!=null&&(magicGrowth.sourceId()!=equipment.source().sourceId()
                    ||!magicGrowth.database().equals(equipment.catalog().resourceDatabase())))throw invalid();
            if(magicBows!=null) {
                magicBows=Map.copyOf(magicBows);var weaponIds=new java.util.HashSet<Long>();
                for(var item:equipment.source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.WEAPON)weaponIds.add(item.template().catalogId());
                if(!magicBows.keySet().equals(weaponIds)||magicBows.values().stream().anyMatch(v->v<0))throw invalid();
                if(weaponArrowSprites!=null){weaponArrowSprites=Map.copyOf(weaponArrowSprites);if(!weaponArrowSprites.keySet().equals(weaponIds)||weaponArrowSprites.values().stream().anyMatch(v->v< -1))throw invalid();}
            }
            if(magicBows==null&&weaponArrowSprites!=null)throw invalid();
            if(regenGrowth!=null&&(regenGrowth.sourceId()!=equipment.source().sourceId()
                    ||!regenGrowth.database().equals(equipment.catalog().resourceDatabase())))throw invalid();
        }
        @Override public String toString(){return "PhysicalSource[redacted]";}
    }
    record Offense(long level,long classHit,long growthHit,long originalHit,long equipmentHit,long enchantHit,long totalHit,
                   long classDamage,long growthDamage,long originalDamage,long itemDamage,long templateDamage,long enchantDamage,
                   long ignoreReduction) {}
    record Defense(long armorClass,long hitDefenseMaximum,long acMaximum,long armorReduction,long directionalReduction,long pvpReduction) {}
    /** Identity-neutral calculator input; no character, inventory owner, SQL row or grant. */
    record Values(boolean supported,long maximumHp,Offense offense,Defense defense,
                  boolean unarmed,ModernCombatProfile.WeaponFacts weapon,Rational multiplier) {
        Values {
            Objects.requireNonNull(offense);Objects.requireNonNull(defense);Objects.requireNonNull(multiplier);
            if(maximumHp<1 || unarmed && weapon!=null || supported && !unarmed && weapon==null)throw invalid();
        }
    }
    static final class Fighter {
        private final String catalogDatabase;
        private final ModernCombatProfile profile;private final Offense offense;private final Defense defense;
        private final UnsupportedReason unsupported;private final long range;private final Rational multiplier;
        private final ModernMagicCombat.Fighter magic;
        private final ModernRangedCombat.Fighter ranged;
        private final ModernNaturalRegen.Profile regen;
        private final OptionalLong hasteItemCount;
        private ModernHonorHealing.Profile healing;
        private ModernArmorProtectionLoadout armorProtection;
        private ModernVipCombatProfile vip;
        private Fighter(String catalogDatabase,ModernCombatProfile p,Offense o,Defense d,UnsupportedReason u,long range,Rational multiplier,
                ModernMagicCombat.Fighter magic,ModernRangedCombat.Fighter ranged,ModernNaturalRegen.Profile regen,OptionalLong hasteItemCount) {
            this.catalogDatabase=catalogDatabase;
            profile=p;offense=o;defense=d;unsupported=u;this.range=range;this.multiplier=multiplier;this.magic=magic;
            this.ranged=ranged;this.regen=regen;this.hasteItemCount=Objects.requireNonNull(hasteItemCount);
        }
        ModernCombatProfile profile(){return profile;}
        String catalogDatabase(){return catalogDatabase;}
        TournamentPresence.CharacterKey key(){return new TournamentPresence.CharacterKey(profile.source().sourceId(),profile.source().character().objectId());}
        Offense offense(){return offense;}Defense defense(){return defense;}
        long attackRange(){return range;}UnsupportedReason unsupportedReason(){return unsupported;}
        boolean supported(){return unsupported==UnsupportedReason.NONE;}
        /** Null means the source did not opt in to magic qualification, never zero MR. */
        ModernMagicCombat.Fighter magic(){return magic;}
        ModernRangedCombat.Fighter ranged(){return ranged;}
        ModernNaturalRegen.Profile regen(){return regen;}
        ModernHonorHealing.Profile healing(){return healing;}
        /** Internal engine opt-in, not a resource/wire capability. */
        ModernArmorProtectionLoadout armorProtection(){return armorProtection;}
        ModernVipCombatProfile vip(){return vip;}
        /** Only weapon/armor haste_item, not dolls, astrolabe, buffs or a cast grant. */
        OptionalLong hasteItemCount(){return hasteItemCount;}
        Values values(){return new Values(supported(),profile.effective().maximumHp(),offense,defense,
                profile.weapon().objectId()==0,profile.weapon().facts(),multiplier);}
        @Override public String toString(){return "ModernPhysicalFighter[redacted]";}
    }
    record Outcome(boolean hit,long rawDamage,long appliedDamage,long remainingHp,boolean death,
                   boolean maximumTriggered,boolean criticalTriggered) {
        Outcome {if(rawDamage<0||appliedDamage<0||appliedDamage>rawDamage||remainingHp<0
                ||(!hit&&(rawDamage!=0||appliedDamage!=0||death||maximumTriggered||criticalTriggered))
                ||(death&&(appliedDamage==0||remainingHp!=0)))throw invalid();}
    }
    private ModernPhysicalCombat(){}

    static Fighter qualify(Source source,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long now) {
        Objects.requireNonNull(source);
        try {
            var profile=ModernCombatProfile.qualify(source.input(),binding,stored,now,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
            return qualifyProfile(source,profile,now);
        } catch(ArithmeticException failure){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }

    /** Candidate only: does not mutate the live engine, store, clocks or skill receipts. */
    static Fighter qualifyEquipmentChange(Source source,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> replacement,long now,Fighter previous,long liveHp,long liveMp) {
        Objects.requireNonNull(source);Objects.requireNonNull(previous);
        if(previous.armorProtection()!=null)throw invalid(); // Require a fresh, source-bound skill catalog too.
        if(!previous.catalogDatabase().equals(source.growth().database()))throw invalid();
        try {
            var candidate=ModernCombatProfile.qualify(source.input(),binding,replacement,now,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
            var retained=candidate.retainMatchVitals(previous.profile(),liveHp,liveMp);
            return qualifyProfile(source,retained,now);
        } catch(ArithmeticException failure){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }

    private static Fighter qualifyProfile(Source source,ModernCombatProfile profile,long now) {
        return qualifyProfile(source,profile,now,null);
    }
    static Fighter qualifyProtection(ModernArmorProtectionCatalog.BoundSource bound,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long now) {
        var source=Objects.requireNonNull(bound).physical();
        var profile=ModernCombatProfile.qualify(source.input(),binding,stored,now,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        return qualifyProfile(source,profile,now,ModernArmorProtectionLoadout.qualify(bound,profile));
    }
    static Fighter qualifyProtectionEquipmentChange(ModernArmorProtectionCatalog.BoundSource bound,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long now,Fighter previous,long hp,long mp) {
        Objects.requireNonNull(previous);var source=Objects.requireNonNull(bound).physical();
        if(previous.armorProtection()==null||!previous.catalogDatabase().equals(source.growth().database()))throw invalid();
        var next=ModernCombatProfile.qualify(source.input(),binding,stored,now,ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH)
                .retainMatchVitals(previous.profile(),hp,mp);
        return qualifyProfile(source,next,now,ModernArmorProtectionLoadout.qualify(bound,next));
    }
    private static Fighter qualifyProfile(Source source,ModernCombatProfile profile,long now,ModernArmorProtectionLoadout protection) {
        return qualifyProfile(source,profile,now,protection,null);
    }
    /** Complete numeric preparation only. Existing engine admission rejects
     * this until frame/CAS/expiry/resource publication is integrated. */
    static Fighter qualifyVip(ModernVipCharacterState state,long now) {
        var vip=ModernVipCombatProfile.qualify(state,now);
        return qualifyProfile(vip.source(),vip.character(),now,null,vip);
    }
    static Fighter qualifyVipRuntime(ModernVipCharacterState state,long now,long abilityRevision,ModernArmorProtectionLoadout protection) {
        var vip=ModernVipCombatProfile.qualify(state,now,abilityRevision);
        return qualifyProfile(vip.source(),vip.character(),now,
                protection==null?null:protection.rebindVip(vip.source(),vip.character()),vip);
    }
    static Fighter qualifyVipProtection(ModernVipCharacterState state,long now,ModernArmorProtectionCatalog.BoundSource bound) {
        var vip=ModernVipCombatProfile.qualify(state,now);
        if(bound.physical()!=vip.source())throw invalid();
        return qualifyProfile(vip.source(),vip.character(),now,ModernArmorProtectionLoadout.qualify(bound,vip.character()),vip);
    }
    private static Fighter qualifyProfile(Source source,ModernCombatProfile profile,long now,ModernArmorProtectionLoadout protection,ModernVipCombatProfile vip) {
        if(vip!=null)vip.require(source,profile);
        if((profile.armorPolicy()==ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP)!=(vip!=null))throw invalid();
        try {
            var e=profile.equipmentEvaluation();var weapon=profile.weapon();var row=profile.source().character();int type=row.characterClass();
            long level=row.level(),classHit=level/HIT_DIVISORS[type],classDamage=level/DAMAGE_DIVISORS[type];
            var growth=source.growth().at(false,profile.effective().strength());
            long original=profile.source().persisted().originalStrength();
            long originalHit=originalHit(type,original),originalDamage=originalDamage(type,original);
            long equipmentHit=add(e.total(MELEE_HIT),e.armorTemplatePhysical().meleeHit(),e.weaponTemplatePhysical().meleeHit());
            long enchantHit=weapon.objectId()==0?0:threeFifths(weapon.enchantment());
            long hit=add(level,classHit,growth.hit(),originalHit,equipmentHit,enchantHit);
            // Validate the complete future subtotal before entering the engine.
            long itemDamage=e.total(MELEE_DAMAGE),templateDamage=add(e.armorTemplatePhysical().meleeDamage(),e.weaponTemplatePhysical().meleeDamage());
            long enchantDamage=weapon.objectId()==0?0:Math.subtractExact(weapon.enchantment(),weapon.durability());
            add(growth.damage(),originalDamage,itemDamage,templateDamage,enchantDamage,classDamage);
            var offense=new Offense(level,classHit,growth.hit(),originalHit,equipmentHit,enchantHit,hit,
                    classDamage,growth.damage(),originalDamage,itemDamage,templateDamage,enchantDamage,e.total(IGNORE_REDUCTION));
            long ac=profile.effective().armorClass();
            long hitMaximum=ac>=0?0:Math.addExact(Math.negateExact(ac),Math.negateExact(ac)/2);
            long acMaximum=Math.max(0,Math.subtractExact(10,ac))/AC_DIVISORS[type];
            var defense=new Defense(ac,hitMaximum,acMaximum,Math.addExact(e.magicReductionBase(),vip==null?0:vip.genericReduction()),Math.max(0,e.total(MELEE_REDUCTION)),Math.max(0,e.total(PVP_REDUCTION)));
            UnsupportedReason reason=UnsupportedReason.NONE;
            for(var flag:profile.equipped()) {
                var item=source.input().equipment().items().get(flag.objectId()).source();long catalog=item.template().catalogId();
                // Saved total AC may retain custom/poly effects from SQL-equipped
                // items. Expiring or overriding the current mask does not qualify
                // that unresolved source baseline and must not unblock combat.
                if(!flag.equipped() && !item.equipped())continue;
                var life=source.input().equipment().lifetimes().view(item.objectId(),now);
                if(!life.usable() && life.state()!=ModernItemLifetime.State.EXPIRED)reason=UnsupportedReason.LIFETIME_UNQUALIFIED;
                if((vip==null||!vip.permits(item,flag.equipped()))&&ModernVipEquipment.unresolvedCustom(source,item,flag.equipped()))reason=UnsupportedReason.CUSTOM_EXECUTOR;
                if(source.procCatalogIds().contains(catalog)&&(protection==null||!protection.permits(item.objectId())))reason=UnsupportedReason.EQUIPMENT_PROC;
            }
            long range=1;Rational multiplier=new Rational(1,1);
            if(weapon.state()==ModernCombatProfile.WeaponState.UNQUALIFIED)reason=UnsupportedReason.WEAPON_UNQUALIFIED;
            else if(weapon.facts()!=null) {
                var facts=weapon.facts();long weaponType=facts.retainedType();
                if(facts.ranged())reason=UnsupportedReason.RANGED;
                else if(weaponType==17)reason=UnsupportedReason.KIRINGKU;
                else if(!MELEE_TYPES.contains(weaponType)||facts.catalogId()==247||facts.catalogId()==248||facts.catalogId()==249)
                    reason=UnsupportedReason.SPECIAL_WEAPON; // Retained trial weapons explicitly cannot hit (L1AttackPc:261).
                else if(facts.attributeKind()!=0)reason=UnsupportedReason.ATTRIBUTE_ENCHANT;
                else if(facts.range()<0||facts.range()>16)reason=UnsupportedReason.INVALID_MELEE_RANGE;
                else if(facts.damageSmall()<1)reason=UnsupportedReason.ZERO_WEAPON_DIE;
                range=Math.max(1,facts.range());multiplier=source.templates().get(facts.catalogId()).criticalMultiplier();
            }
            var magic=source.magicGrowth()==null?null:ModernMagicCombat.qualify(source,profile,protection,vip);
            var ranged=ModernRangedCombat.qualify(source,profile,defense,now,protection,vip);
            var regen=source.regenGrowth()==null?null:ModernNaturalRegen.qualify(source,profile,source.regenGrowth(),protection,vip);
            var result=new Fighter(source.growth().database(),profile,offense,defense,reason,range,multiplier,magic,ranged,regen,hasteItemCount(source,profile));
            result.healing=ModernHonorHealing.qualify(source,profile,magic);
            result.armorProtection=protection;
            result.vip=vip;
            return result;
        } catch(ArithmeticException failure){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }

    private static OptionalLong hasteItemCount(Source source,ModernCombatProfile profile) {
        // Require the complete carried-gear catalog too: an unobserved flag is
        // unknown, even when its item is not equipped in this revision.
        for(var policy:source.templates().values())
            if(policy.kind()!=ModernCharacterData.ItemKind.ETC&&policy.hasteItem().isEmpty())
                return OptionalLong.empty();
        long count=0;
        for(var flag:profile.equipped()) {
            if(!flag.equipped())continue;
            var item=source.input().equipment().items().get(flag.objectId());
            var policy=source.templates().get(item.source().template().catalogId());
            // ItemTable uses != 0. Preserve the full signed i64 before testing;
            // 2, negative values and 2^32 must never narrow to false.
            if(policy.hasteItem().isPresent()&&policy.hasteItem().getAsLong()!=0)count++;
        }
        return OptionalLong.of(count);
    }

    /** Caller validates source/match/team/position/pace and commits HP only AFTER this returns. */
    static Outcome resolve(Fighter attacker,Fighter defender,long currentHp,BoundedRandom random) {
        Objects.requireNonNull(attacker);Objects.requireNonNull(defender);Objects.requireNonNull(random);
        if(!attacker.supported()||!defender.supported())throw new CombatException(Failure.UNSUPPORTED);
        if(attacker.key().equals(defender.key())||currentHp<1||currentHp>defender.profile().effective().maximumHp())throw new CombatException(Failure.INVALID_STATE);
        return resolveValues(attacker.values(),defender.values(),currentHp,random);
    }
    /** Trusted typed combatant adapter proves distinct identity before calling this pure core. */
    static Outcome resolveValues(Values attacker,Values defender,long currentHp,BoundedRandom random) {
        return resolveValues(attacker,defender,currentHp,random,0);
    }
    /** TOURNAMENT_WEIGHT_V1 opts into the classic carrying hit subtotal penalty. */
    static Outcome resolveValues(Values attacker,Values defender,long currentHp,BoundedRandom random,int weightHitPenalty) {
        return resolveProtected(attacker,defender,currentHp,random,weightHitPenalty,ModernIncomingDamage.NONE).outcome();
    }
    static ModernIncomingDamage.Resolved<Outcome> resolveProtected(Values attacker,Values defender,long currentHp,
            BoundedRandom random,int weightHitPenalty,ModernIncomingDamage.Protection protection) {
        Objects.requireNonNull(attacker);Objects.requireNonNull(defender);Objects.requireNonNull(random);
        Objects.requireNonNull(protection);
        if(weightHitPenalty!=0&&weightHitPenalty!=1&&weightHitPenalty!=3&&weightHitPenalty!=5)throw new CombatException(Failure.INVALID_STATE);
        if(!attacker.supported()||!defender.supported())throw new CombatException(Failure.UNSUPPORTED);
        if(currentHp<1||currentHp>defender.maximumHp())throw new CombatException(Failure.INVALID_STATE);
        try {
            long hitRate=Math.subtractExact(attacker.offense.totalHit(),weightHitPenalty);
            long attackerDice=add(draw(random,20),2,hitRate,-10);
            long defenderDice=defender.defense.armorClass()>=0?Math.subtractExact(10,defender.defense.armorClass())
                    :add(10,oneThrough(random,defender.defense.hitDefenseMaximum()));
            long fumble=Math.subtractExact(hitRate,9),critical=Math.addExact(hitRate,10);
            int chance=attackerDice<=fumble?15:attackerDice>=critical?100:attackerDice>defenderDice?100:15;
            if(oneThrough(random,100)>chance)return new ModernIncomingDamage.Resolved<>(
                    new Outcome(false,0,0,currentHp,false,false,false),protection.apply(0,currentHp));
            long base;boolean maximumTriggered=false,criticalTriggered=false;
            if(attacker.unarmed()) {
                // L1AttackPc.weaponDamage2 replaces the earlier STR/equip sum;
                // only the later classDamage is included in this clean state.
                base=add((draw(random,5)+4)>>2,attacker.offense.classDamage());
            } else {
                var facts=attacker.weapon();long max=facts.damageSmall();
                maximumTriggered=facts.maximumDamageChance()>0&&oneThrough(random,100)<=facts.maximumDamageChance();
                long die=maximumTriggered?max:oneThrough(random,facts.maximumDamageChance()>0&&max>1?max-1:max);
                var o=attacker.offense;
                base=Math.max(0,add(die,o.growthDamage(),o.originalDamage(),o.itemDamage(),o.templateDamage(),o.enchantDamage(),o.classDamage()));
                criticalTriggered=facts.criticalChance()>0&&oneThrough(random,100)<=facts.criticalChance();
                if(criticalTriggered)base=attacker.multiplier.applyAtLeastOne(base);
            }
            var d=defender.defense;
            long acReduction=d.acMaximum()==0?0:add(draw(random,d.acMaximum()),Math.max(1,d.acMaximum()>>3));
            long armor=d.armorReduction()<=10?d.armorReduction():add(11,draw(random,Math.subtractExact(d.armorReduction(),10)));
            long effectiveArmor=Math.max(0,Math.subtractExact(armor,attacker.offense.ignoreReduction()));
            long reduction=add(acReduction,effectiveArmor,d.directionalReduction(),d.pvpReduction());
            long damage=Math.max(0,Math.subtractExact(base,reduction));
            var impact=protection.apply(damage,currentHp);
            return new ModernIncomingDamage.Resolved<>(new Outcome(true,base,impact.applied(),impact.remaining(),impact.death(),
                    maximumTriggered,criticalTriggered),impact);
        } catch(ArithmeticException failure){throw new CombatException(Failure.ARITHMETIC_OVERFLOW);}
    }
    private static long oneThrough(BoundedRandom random,long maximum){return Math.addExact(draw(random,maximum),1);}
    private static long draw(BoundedRandom random,long bound) {
        if(bound<1)throw new CombatException(Failure.INVALID_RANDOM);
        long value=random.nextLong(bound);if(value<0||value>=bound)throw new CombatException(Failure.INVALID_RANDOM);return value;
    }
    private static long add(long... values){long result=0;for(long value:values)result=Math.addExact(result,value);return result;}
    private static long threeFifths(long value){return Math.addExact(Math.multiplyExact(value/5,3),Math.multiplyExact(value%5,3)/5);}
    private static final int[] HIT_DIVISORS={15,10,30,40,30,25,40},DAMAGE_DIVISORS={10,6,12,20,16,13,16},AC_DIVISORS={3,2,3,5,4,3,4};
    // Exact retained switch thresholds, including original==0 taking default.
    static long originalHit(int type,long value) {
        if(type<0||type>6||value<0)throw invalid();
        return switch(type) {
            case 0 -> tier(value,15,18,1,2);case 1 -> tier(value,16,18,2,4);
            case 2 -> tier(value,12,14,1,2);case 3 -> tier(value,10,12,1,2);
            case 4 -> tier(value,14,17,1,2);case 5 -> tier(value,13,16,1,3);
            default -> value>=1&&value<=11?0:value==12||value==13?1:value==14||value==15?2:value==16?3:4;
        };
    }
    static long originalDamage(int type,long value) {
        if(type<0||type>6||value<0)throw invalid();
        return switch(type) {
            case 0 -> tier(value,14,17,1,2);case 1 -> tier(value,17,19,2,4);
            case 2,6 -> tier(value,11,13,1,2);case 3 -> tier(value,9,11,1,2);
            case 4 -> tier(value,13,17,1,2);default -> tier(value,14,17,1,3);
        };
    }
    private static long tier(long value,int low,int middle,int middleBonus,int upperBonus){return value>=1&&value<=low?0:value>low&&value<=middle?middleBonus:upperBonus;}
    static void identifier(String value){if(value==null||!value.matches("[A-Za-z0-9_]{1,64}"))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid qualified physical input");}
}
