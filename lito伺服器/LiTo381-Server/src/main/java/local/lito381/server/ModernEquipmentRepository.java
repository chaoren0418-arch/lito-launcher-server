package local.lito381.server;

import java.util.Map;
import java.util.Objects;

/** Exact configured source/account/catalog routing. No SQL mutation or native fallback. */
final class ModernEquipmentRepository {
    record Source(ModernServerConfig.SourceConfig config, ModernJdbcBackend backend) {
        Source {
            Objects.requireNonNull(config); Objects.requireNonNull(backend);
            if (!backend.usesConfiguration(config.database())) throw new IllegalArgumentException("equipment DAO binding mismatch");
            ModernEquipmentSourceJdbc.sql(config.catalogDatabase());
        }
        @Override public String toString() { return "ModernEquipmentSourceBinding[redacted]"; }
    }
    private ModernEquipmentRepository() {}
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured) {
        return physicalJdbc(configured,false);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled) {
        return physicalJdbc(configured,magicEnabled,false);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,boolean bowEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,false);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,false);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,honorEnabled,false);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,false);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,false);
    }
    /** Explicit source/durable-equipment preparation. Per-world reads below
     * keep unselected worlds on their original, independently qualified path. */
    static ModernEquipmentAuthority.PhysicalProvider armorProtectionJdbc(Map<Integer,Source> configured,boolean magicEnabled,
            boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,true);
    }
    static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,
            boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean protectionEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,protectionEnabled,false);
    }
    /** Dedicated full-VIP transition source. Does not enable runtime/wire/UI
     * support and never changes the strict default/new-match source reader. */
    static ModernEquipmentAuthority.PhysicalProvider fullVipJdbc(Map<Integer,Source> configured,boolean magicEnabled,
            boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean protectionEnabled) {
        return physicalJdbc(configured,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,protectionEnabled,true);
    }
    private static ModernEquipmentAuthority.PhysicalProvider physicalJdbc(Map<Integer,Source> configured,boolean magicEnabled,
            boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean protectionEnabled,boolean retainedVip) {
        ModernPhysicalCombatSourceJdbc.requireHealingMagic(magicEnabled,playerHealingEnabled);
        if(configured==null||configured.isEmpty()||configured.size()>64
                ||configured.entrySet().stream().anyMatch(e->e.getKey()==null||e.getValue()==null
                ||e.getKey()!=e.getValue().config().id()))throw new IllegalArgumentException("invalid physical sources");
        var sources=Map.copyOf(configured);
        return new ModernEquipmentAuthority.PhysicalProvider() {
          @Override public boolean fullVipSource(int sourceId){return retainedVip&&sources.containsKey(sourceId);}
          @Override public boolean playerHealingSource(int sourceId){return playerHealingEnabled&&sources.containsKey(sourceId);}
          @Override public boolean armorProtectionSource(int sourceId){return protectionEnabled&&sources.containsKey(sourceId);}
          @Override public ModernEquipmentAuthority.PhysicalLoaded read(ModernAccountAuthentication.Principal principal,int sourceId,long objectId)throws Exception {
            return readSelected(principal,sourceId,objectId,protectionEnabled);
          }
          @Override public ModernEquipmentAuthority.PhysicalLoaded readForWorld(ModernAccountAuthentication.Principal principal,int sourceId,long objectId,boolean armorProtection)throws Exception {
            if(armorProtection&&!protectionEnabled)throw new IllegalStateException("armor source not enabled");
            return readSelected(principal,sourceId,objectId,armorProtection);
          }
          private ModernEquipmentAuthority.PhysicalLoaded readSelected(ModernAccountAuthentication.Principal principal,int sourceId,long objectId,boolean armorProtection)throws Exception {
            Objects.requireNonNull(principal);
            if(!principal.authorizesSource(sourceId)||objectId<=0)throw new SecurityException("unauthorized physical source");
            var source=sources.get(sourceId);
            if(source==null)throw new IllegalStateException("physical source unavailable");
            var owner=ModernEquipmentBinding.ownerBinding(principal,source.config());
            if(armorProtection) {
                var bound=source.backend().armorProtectionCharacter(sourceId,principal.accountForSource(sourceId),objectId,
                        source.config().catalogDatabase(),magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,retainedVip)
                        .orElseThrow(()->new IllegalStateException("physical character unavailable"));
                return new ModernEquipmentAuthority.PhysicalLoaded(bound.physical(),owner,bound);
            }
            var input=source.backend().physicalCharacter(sourceId,principal.accountForSource(sourceId),objectId,
                    source.config().catalogDatabase(),magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,retainedVip).orElseThrow(()->new IllegalStateException("physical character unavailable"));
            return new ModernEquipmentAuthority.PhysicalLoaded(input,owner);
          }
        };
    }
    static ModernEquipmentAuthority.CombatProvider combatJdbc(Map<Integer, Source> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null
                    || e.getKey() != e.getValue().config().id())) throw new IllegalArgumentException("invalid combat sources");
        var sources = Map.copyOf(configured);
        return (principal, sourceId, objectId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId) || objectId <= 0) throw new SecurityException("unauthorized combat source");
            var source = sources.get(sourceId);
            if (source == null) throw new IllegalStateException("combat source unavailable");
            var owner = ModernEquipmentBinding.ownerBinding(principal, source.config());
            var input = source.backend().combatCharacter(sourceId, principal.accountForSource(sourceId), objectId,
                    source.config().catalogDatabase()).orElseThrow(() -> new IllegalStateException("combat character unavailable"));
            // Same DAO identity guard and physical-close-before-publish contract
            // as equipment; no alternate connection or primary-source fallback.
            return new ModernEquipmentAuthority.CombatLoaded(input,owner);
        };
    }
    static ModernEquipmentAuthority.Provider jdbc(Map<Integer, Source> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null
                    || e.getKey() != e.getValue().config().id())) throw new IllegalArgumentException("invalid equipment sources");
        var sources = Map.copyOf(configured);
        return (principal, sourceId, objectId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId) || objectId <= 0) throw new SecurityException("unauthorized equipment source");
            var source = sources.get(sourceId);
            if (source == null) throw new IllegalStateException("equipment source unavailable");
            var owner = ModernEquipmentBinding.ownerBinding(principal, source.config());
            var qualified = source.backend().equipmentCharacter(sourceId, principal.accountForSource(sourceId), objectId,
                    source.config().catalogDatabase()).orElseThrow(() -> new IllegalStateException("equipment character unavailable"));
            // JDBC's owned physical connection AND raw socket have closed before
            // this immutable bundle can reach the durable override store.
            return new ModernEquipmentAuthority.Loaded(qualified, owner);
        };
    }
}
