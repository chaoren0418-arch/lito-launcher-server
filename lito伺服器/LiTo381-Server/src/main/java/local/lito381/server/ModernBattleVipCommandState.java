package local.lito381.server;

import java.util.Objects;

/** Full-VIP command preconditions, not authentication or a wire capability.
 * All three versions come from the SAME qualified owner observation. Saved
 * equipment CAS, runtime ability rebuilds and VIP lifecycle events are separate
 * clocks; neither their equality nor a newer world revision substitutes for an
 * exact comparison. The service must still authorize owner/selection and the
 * equipment authority must qualify source facts and perform durable CAS.
 */
final class ModernBattleVipCommandState {
    record Proof(ModernBattleCommandLife.Proof life, ModernBattleVipObservation.Versions versions) {
        Proof { Objects.requireNonNull(life); Objects.requireNonNull(versions); }
        @Override public String toString() { return "VipCommandState[redacted]"; }
    }
    static Proof from(ModernBattleVipObservation.View view) {
        Objects.requireNonNull(view);
        var personal=view.personal();var world=view.world();
        return new Proof(new ModernBattleCommandLife.Proof(personal.scope(),world.identity(),personal.entityId(),
                Math.incrementExact(personal.history().resources().life()),world.revision()),personal.equipment().versions());
    }
    enum Rejection {
        NONE, NOT_ACTIVE, STALE_LIFE, DEAD, UNAVAILABLE, BUSY,
        STALE_SAVED_EQUIPMENT, STALE_RUNTIME_ABILITY, STALE_LIFECYCLE
    }
    /** PRIVATE engine capture. A later service must project only the authorized
     * owner with the full VIP schema, never the legacy equipment reply. */
    record EquipmentAdmission(Rejection rejection, ModernBattleEngine.EquipmentChange change,
                              ModernBattleObservation.Capture observation) {
        EquipmentAdmission {
            Objects.requireNonNull(rejection); Objects.requireNonNull(observation);
            if((rejection==Rejection.NONE)!=(change!=null))throw new IllegalArgumentException("invalid VIP admission");
        }
        @Override public String toString() { return "VipEquipmentAdmission[redacted]"; }
    }
    static Rejection compare(ModernBattleVipObservation.Versions seen, ModernBattleVipObservation.Versions current) {
        Objects.requireNonNull(seen); Objects.requireNonNull(current);
        if(seen.savedEquipmentRevision()!=current.savedEquipmentRevision())return Rejection.STALE_SAVED_EQUIPMENT;
        if(seen.runtimeAbilityRevision()!=current.runtimeAbilityRevision())return Rejection.STALE_RUNTIME_ABILITY;
        if(seen.lifecycleSequence()!=current.lifecycleSequence())return Rejection.STALE_LIFECYCLE;
        return Rejection.NONE;
    }
    private ModernBattleVipCommandState() {}
}
