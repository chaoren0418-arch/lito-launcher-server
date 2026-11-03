package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Server-only, checked-i64 equipment-effective input. Not an attack permission,
 * full buff/passive evaluation, STR/DEX growth table, or a persisted HP update.
 * CharacterTable.captureCharacterSnapshotLocked saves BASE maxima/six abilities
 * but getAc() saves TOTAL AC. Keep that distinction and every unresolved saved
 * AC layer on the retained import route. The explicit rebuilt-VIP route uses
 * the traced birth/level AC plus current qualified equipment and VIP instead.
 * Never substitute OriginalStr/OriginalDex for the six base ability fields.
 */
final class ModernCombatProfile {
    enum VitalPolicy { FULL_NEW_MATCH, RETAINED_SAVED, RETAINED_MATCH }
    enum ArmorPolicy { PRESERVED_SOURCE_TOTAL, REBUILT_CHARACTER_EQUIPMENT_VIP }
    enum WeaponState { UNARMED, QUALIFIED, UNQUALIFIED }

    /**
     * One source item's real catalog/instance facts. Integer fields never narrow.
     * The multiplier is deliberately the retained getDouble semantic, not a
     * claim that an SQL FLOAT has exact decimal/rational precision. Attributes
     * are retained IDs only: their passive/proc table is NOT qualified here.
     */
    record WeaponFacts(long objectId, long catalogId, long retainedType,
            long damageSmall, long damageLarge, long range, int material, long templateBlessing,
            long maximumDamageChance, long criticalChance, double criticalMultiplier,
            long attributeKind, long attributeLevel) {
        WeaponFacts {
            if (objectId < 1 || catalogId < 1 || retainedType < 1 || retainedType > 18
                    || damageSmall < 0 || damageLarge < 0 || range < -1 || material < 0 || material > 22
                    || templateBlessing < 0 || templateBlessing > 3
                    || maximumDamageChance < 0 || maximumDamageChance > 100
                    || criticalChance < 0 || criticalChance > 100
                    || !Double.isFinite(criticalMultiplier) || criticalMultiplier < 0
                    || attributeKind < 0 || attributeLevel < 0
                    || (attributeKind == 0) != (attributeLevel == 0)) throw invalid();
        }
        boolean ranged() { return retainedType == 4 || retainedType == 10 || retainedType == 13; }
        @Override public String toString() { return "CombatWeaponFacts[redacted]"; }
    }

    /** Immutable bundle from ONE source data statement plus its bounded catalog reads. */
    record Input(Qualified equipment, boolean weaponsQualified, Map<Long, WeaponFacts> weapons) {
        Input {
            Objects.requireNonNull(equipment); Objects.requireNonNull(weapons);
            if (weapons.size() > ModernCharacterData.MAX_ITEMS) throw invalid();
            var copy = new TreeMap<Long, WeaponFacts>();
            for (var entry : weapons.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null
                        || entry.getKey().longValue() != entry.getValue().objectId()) throw invalid();
                copy.put(entry.getKey(), entry.getValue());
            }
            if (!weaponsQualified && !copy.isEmpty()) throw invalid();
            int expected = 0;
            for (var item : equipment.source().inventory()) {
                if (item.template().kind() != ModernCharacterData.ItemKind.WEAPON) continue;
                expected++;
                var facts = copy.get(item.objectId());
                if (weaponsQualified && (facts == null || facts.catalogId() != item.template().catalogId()
                        || facts.retainedType() != equipment.items().get(item.objectId()).template().retainedType())) throw invalid();
            }
            if (weaponsQualified && copy.size() != expected) throw invalid();
            weapons = Map.copyOf(copy);
        }
        static Input unqualified(Qualified equipment) { return new Input(equipment, false, Map.of()); }
        @Override public String toString() { return "CombatSourceInput[redacted]"; }
    }

    record Effective(long maximumHp, long maximumMp, long initialHp, long initialMp,
            long armorClass, long strength, long dexterity, long constitution,
            long intelligence, long wisdom, long charisma) {
        Effective {
            if (maximumHp < 1 || maximumMp < 0 || initialHp < 0 || initialHp > maximumHp
                    || initialMp < 0 || initialMp > maximumMp || strength < 0 || dexterity < 0
                    || constitution < 0 || intelligence < 0 || wisdom < 0 || charisma < 0) throw invalid();
        }
    }
    record Weapon(WeaponState state, long objectId, WeaponFacts facts,
            long enchantment, long durability, TemplatePhysical templateModifiers, Ability itemModifiers) {
        Weapon {
            Objects.requireNonNull(state); Objects.requireNonNull(templateModifiers); Objects.requireNonNull(itemModifiers);
            if (objectId < 0 || durability < 0 || (state == WeaponState.UNARMED) != (objectId == 0)
                    || (state == WeaponState.QUALIFIED) != (facts != null)
                    || facts != null && facts.objectId() != objectId
                    || objectId == 0 && (enchantment != 0 || durability != 0
                        || !templateModifiers.equals(TemplatePhysical.zero()) || !itemModifiers.equals(Ability.zero()))) throw invalid();
        }
        @Override public String toString() { return "CombatWeapon[redacted]"; }
    }

    private final ModernCharacterData.Snapshot source;
    private final long equipmentRevision;
    private final long evaluatedAtMillis;
    private final long validUntilMillis;
    private final VitalPolicy vitalPolicy;
    private final ArmorPolicy armorPolicy;
    private final Effective effective;
    private final ModernEquipmentEvaluator.Evaluation baseline;
    private final ModernEquipmentEvaluator.Evaluation current;
    private final Weapon weapon;
    private final List<ModernEquipmentAuthority.Equipped> equipped;
    private final List<ModernItemLifetime.View> sourceLifetimes;

    private ModernCombatProfile(ModernCharacterData.Snapshot source, long equipmentRevision, long now,
            VitalPolicy policy, Effective effective, ModernEquipmentEvaluator.Evaluation baseline,
            ModernEquipmentEvaluator.Evaluation current, Weapon weapon, List<ModernEquipmentAuthority.Equipped> equipped,
            long validUntilMillis, List<ModernItemLifetime.View> sourceLifetimes,ArmorPolicy armorPolicy) {
        this.source = source; this.equipmentRevision = equipmentRevision; this.evaluatedAtMillis = now;
        this.validUntilMillis = validUntilMillis;
        this.vitalPolicy = policy; this.effective = effective; this.baseline = baseline;
        this.current = current; this.weapon = weapon; this.equipped = List.copyOf(equipped);
        this.sourceLifetimes = List.copyOf(sourceLifetimes);
        this.armorPolicy=Objects.requireNonNull(armorPolicy);
    }

    static ModernCombatProfile qualify(Input input, ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored, long nowEpochMillis, VitalPolicy policy) {
        return qualify(input,binding,stored,nowEpochMillis,policy,null);
    }
    /** Explicit new character-domain route, never a fallback for a failed
     * saved-total import. Complete VIP source/shape qualification is mandatory. */
    static ModernCombatProfile qualifyRebuiltVip(Input input,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long now,ModernVipLoadout vip) {
        Objects.requireNonNull(vip).require(input,binding,stored,now);
        return qualify(input,binding,stored,now,VitalPolicy.FULL_NEW_MATCH,vip);
    }
    private static ModernCombatProfile qualify(Input input,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long nowEpochMillis,VitalPolicy policy,ModernVipLoadout vip) {
        Objects.requireNonNull(input); Objects.requireNonNull(binding); Objects.requireNonNull(stored); Objects.requireNonNull(policy);
        if (nowEpochMillis < 0 || policy == VitalPolicy.RETAINED_MATCH) throw invalid();
        Qualified fresh = input.equipment();
        // Reconcile THIS bundle here. A Result from another source with equal
        // numeric item IDs/fingerprints is not a source/owner capability.
        var approved = ModernEquipmentReconciliation.reconcile(fresh, binding, stored, nowEpochMillis);
        var overrides=vip==null?approved.overrides():vip.effectiveOverrides(approved.overrides());
        var baseline = vip==null?ModernEquipmentEvaluator.evaluateSourceBaseline(fresh, nowEpochMillis):null;
        var current = vip==null?ModernEquipmentEvaluator.evaluate(fresh, approved.overrides(), nowEpochMillis)
                :ModernEquipmentEvaluator.evaluateCompleteVip(fresh,overrides,nowEpochMillis,vip.commonAbilities());
        if(vip!=null&&current.vipRows()!=vip.items().size())throw invalid(); // No unqualified ETC VIP row can disappear.
        var saved = fresh.source().persisted();
        long maximumHp = Math.addExact(saved.maxHp(), current.total(MAX_HP));
        long maximumMp = Math.addExact(saved.maxMp(), current.total(MAX_MP));
        // Old import: same fresh clock/catalog on both sides of the override
        // delta. Explicit rebuild: no saved-total subtraction or inferred old
        // VIP template; the traced intrinsic layer plus current modifiers.
        long ac = vip==null?Math.addExact(saved.armorClass(), Math.subtractExact(current.total(AC), baseline.total(AC)))
                :Math.addExact(ModernCharacterArmor.intrinsic(fresh.source().character().characterClass(),
                        fresh.source().character().level(),saved.originalDexterity()),current.total(AC));
        long hp = policy == VitalPolicy.FULL_NEW_MATCH ? maximumHp : saved.savedHp();
        long mp = policy == VitalPolicy.FULL_NEW_MATCH ? maximumMp : saved.savedMp();
        // RETAINED_SAVED rejects an impossible live range rather than silently
        // healing, clipping, or adding equipment to an already saved live value.
        var effective = new Effective(maximumHp, maximumMp, hp, mp, ac,
                Math.addExact(saved.strength(), current.total(STR)), Math.addExact(saved.dexterity(), current.total(DEX)),
                Math.addExact(saved.constitution(), current.total(CON)), Math.addExact(saved.intelligence(), current.total(INT)),
                Math.addExact(saved.wisdom(), current.total(WIS)), Math.addExact(saved.charisma(), current.total(CHA)));
        long weaponId = current.weaponObjectId();
        Weapon weapon;
        if (weaponId == 0) weapon = new Weapon(WeaponState.UNARMED, 0, null, 0, 0, TemplatePhysical.zero(), Ability.zero());
        else {
            var item = fresh.items().get(weaponId).source();
            WeaponFacts facts = input.weapons().get(weaponId);
            weapon = new Weapon(facts == null ? WeaponState.UNQUALIFIED : WeaponState.QUALIFIED, weaponId, facts,
                    item.enchantment(), item.durability(), current.weaponTemplatePhysical(), current.weaponItem());
        }
        var flags = approved.desiredLoadout().items().stream().map(item ->
                new ModernEquipmentAuthority.Equipped(item.objectId(), overrides.getOrDefault(item.objectId(),item.equipped()))).toList();
        long validUntil = current.nextStoneExpiry().orElse(-1);
        for (var flag : flags) if (flag.equipped()) {
            var life = vip==null?fresh.lifetimes().view(flag.objectId(), nowEpochMillis):vip.lifetime(flag.objectId(),nowEpochMillis);
            if (life.state() == ModernItemLifetime.State.ACTIVE)
                validUntil = validUntil < 0 ? life.expiresAtMillis() : Math.min(validUntil, life.expiresAtMillis());
        }
        return new ModernCombatProfile(fresh.source(), approved.expected().revision(), nowEpochMillis,
                policy, effective, baseline, current, weapon, flags, validUntil,
                vip==null?fresh.lifetimes().views(fresh.source(), nowEpochMillis):fresh.source().inventory().stream()
                        .map(i->vip.lifetime(i.objectId(),nowEpochMillis)).toList(),vip==null?ArmorPolicy.PRESERVED_SOURCE_TOTAL:ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP);
    }

    /**
     * Convert a fully qualified next-revision candidate to a live-match profile.
     * The caller still must bind owner/selection/match/life and publish it with
     * the durable equipment result atomically. Nothing is mutated here, and a
     * new-match PreparedMember intentionally rejects the returned policy.
     */
    ModernCombatProfile retainMatchVitals(ModernCombatProfile previous, long hp, long mp) {
        if(previous.equipmentRevision==Long.MAX_VALUE||equipmentRevision!=previous.equipmentRevision+1)throw invalid();
        return retainVitals(previous,hp,mp,false);
    }
    /** Only the combined VIP state owns same-revision expiry/shape transitions.
     * This remains a calculation, never an admission or store write. */
    ModernCombatProfile retainVipStateVitals(ModernCombatProfile previous,long hp,long mp) {
        return retainVitals(previous,hp,mp,true);
    }
    private ModernCombatProfile retainVitals(ModernCombatProfile previous,long hp,long mp,boolean vipState) {
        Objects.requireNonNull(previous);
        if (vitalPolicy != VitalPolicy.FULL_NEW_MATCH
                || armorPolicy!=previous.armorPolicy
                || previous.vitalPolicy == VitalPolicy.RETAINED_SAVED
                || !source.equals(previous.source)
                || vipState&&armorPolicy!=ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP
                || equipmentRevision!=previous.equipmentRevision
                    &&(previous.equipmentRevision==Long.MAX_VALUE||equipmentRevision!=previous.equipmentRevision+1)
                || evaluatedAtMillis < previous.evaluatedAtMillis) throw invalid();
        var live = new ModernEquipmentVitals.Vitals(hp, previous.effective.maximumHp(),
                mp, previous.effective.maximumMp());
        var retained = ModernEquipmentVitals.retain(live, effective.maximumHp(), effective.maximumMp());
        var next = new Effective(effective.maximumHp(), effective.maximumMp(), retained.hp(), retained.mp(),
                effective.armorClass(), effective.strength(), effective.dexterity(), effective.constitution(),
                effective.intelligence(), effective.wisdom(), effective.charisma());
        return new ModernCombatProfile(source, equipmentRevision, evaluatedAtMillis, VitalPolicy.RETAINED_MATCH,
                next, baseline, current, weapon, equipped, validUntilMillis, sourceLifetimes,armorPolicy);
    }

    ModernCharacterData.Snapshot source() { return source; }
    long equipmentRevision() { return equipmentRevision; }
    long evaluatedAtMillis() { return evaluatedAtMillis; }
    /** Frozen effective maxima/AC/weapon must not outlive their source equipment. */
    long validUntilMillis() { return validUntilMillis; }
    VitalPolicy vitalPolicy() { return vitalPolicy; }
    ArmorPolicy armorPolicy(){return armorPolicy;}
    /** VIP battle ability version, separate from the immutable state's saved
     * equipment version. Expiry changes abilities without inventing a SQL SET.
     * Only a complete VIP proof may pass this projection into combat. */
    ModernCombatProfile withVipRuntimeRevision(long revision) {
        if(armorPolicy!=ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP||revision<equipmentRevision)throw invalid();
        if(revision==equipmentRevision)return this;
        return new ModernCombatProfile(source,revision,evaluatedAtMillis,vitalPolicy,effective,baseline,current,weapon,equipped,
                validUntilMillis,sourceLifetimes,armorPolicy);
    }
    Effective effective() { return effective; }
    ModernEquipmentEvaluator.Evaluation sourceBaseline() {
        if(baseline==null)throw new IllegalStateException("rebuilt character has no saved-total equipment baseline");
        return baseline;
    }
    ModernEquipmentEvaluator.Evaluation equipmentEvaluation() { return current; }
    Weapon weapon() { return weapon; }
    List<ModernEquipmentAuthority.Equipped> equipped() { return equipped; }
    /** Same qualified source and clock, including carried but unequipped gear. */
    List<ModernItemLifetime.View> sourceLifetimes() { return sourceLifetimes; }
    /** No range/hit/damage/skill claim follows merely from having catalog facts. */
    boolean attackImplemented() { return false; }
    @Override public String toString() { return "ModernCombatProfile[redacted]"; }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid qualified combat input"); }
}
