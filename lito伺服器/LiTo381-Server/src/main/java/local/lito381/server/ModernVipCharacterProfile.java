package local.lito381.server;

import java.util.Objects;
import java.util.Optional;

/** Complete VIP attributes plus the traced birth/level and qualified equipment
 * character layers. This is NOT a fully imported classic character: persisted
 * quest/talent/astrolabe/spell layers require their own explicit contracts.
 * Nor does it grant combat, a visual shape/resource, or a durable equipment SET.
 * Keep unmapped reduction/rate fields typed; existing fighters must not silently
 * consume a common-attribute profile and lose those full fields. */
final class ModernVipCharacterProfile {
    private final ModernCombatProfile character;
    private final ModernVipLoadout vip;
    private ModernVipCharacterProfile(ModernCombatProfile character,ModernVipLoadout vip) {
        this.character=Objects.requireNonNull(character);this.vip=Objects.requireNonNull(vip);
        if(character.equipmentRevision()!=vip.equipmentRevision()||character.validUntilMillis()!=vip.validUntilMillis())throw new IllegalArgumentException("VIP profile boundary mismatch");
    }
    static ModernVipCharacterProfile qualify(ModernPhysicalCombat.Source source,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> stored,long now,long currentVipShape) {
        var vip=ModernVipLoadout.qualifyForRebuild(source,binding,stored,now,currentVipShape);
        var character=ModernCombatProfile.qualifyRebuiltVip(source.input(),binding,stored,now,vip);
        return new ModernVipCharacterProfile(character,vip);
    }
    static ModernVipCharacterProfile fromApplied(ModernPhysicalCombat.Source source,
            Optional<ModernEquipmentOverrideStore.Stored> stored,ModernVipLifecycle.State state,
            java.util.Map<Long,ModernItemLifetime.View> lifetimes,long now) {
        var vip=ModernVipLoadout.fromApplied(source,stored,state,lifetimes,now);
        return new ModernVipCharacterProfile(ModernCombatProfile.qualifyRebuiltVip(source.input(),state.binding(),stored,now,vip),vip);
    }
    ModernVipCharacterProfile retainVitals(ModernVipCharacterProfile previous,long hp,long mp) {
        return new ModernVipCharacterProfile(character.retainVipStateVitals(previous.character,hp,mp),vip);
    }
    ModernCombatProfile character(){return character;}
    ModernVipLoadout vip(){return vip;}
    long genericDamageReduction(){return vip.total().get(ModernVipEquipment.Field.DAMAGE_REDUCTION);}
    long magicOnlyReduction(){return vip.total().get(ModernVipEquipment.Field.MAGIC_REDUCTION);}
    long experienceRate(){return vip.total().get(ModernVipEquipment.Field.EXP_RATE);}
    long adenaRate(){return vip.total().get(ModernVipEquipment.Field.ADENA_RATE);}
    @Override public String toString(){return "RebuiltVipCharacterProfile[redacted]";}
}
