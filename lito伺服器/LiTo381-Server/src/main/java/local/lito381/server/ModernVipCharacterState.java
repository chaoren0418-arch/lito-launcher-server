package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** One immutable character/profile/VIP frame. These prepare/accept calculations
 * do not authorize equipment, persist an override, or publish a battle actor.
 * The engine must own the selection, equipment CAS and live-vital lock before
 * atomically replacing its frame. A template reload never rewrites applied VIP
 * abilities; a later SQL clock never renews the same observed raw lifetime. */
final class ModernVipCharacterState {
    private final ModernPhysicalCombat.Source basis;
    private final Optional<ModernEquipmentOverrideStore.Stored> stored;
    private final ModernVipLifecycle.State ledger;
    private final ModernVipCharacterProfile profile;
    private final Map<Long,ModernItemLifetime.View> leases;

    private ModernVipCharacterState(ModernPhysicalCombat.Source basis,Optional<ModernEquipmentOverrideStore.Stored> stored,
            ModernVipLifecycle.State ledger,ModernVipCharacterProfile profile,Map<Long,ModernItemLifetime.View> leases) {
        this.basis=basis;this.stored=stored;this.ledger=ledger;this.profile=profile;this.leases=Map.copyOf(leases);
        if(ledger.equipmentRevision()!=profile.character().equipmentRevision()||!ledger.total().equals(profile.vip().total()))throw invalid();
    }
    static ModernVipCharacterState start(ModernVipLifecycle.Actor actor,ModernPhysicalCombat.Source source,
            ModernEquipmentReconciliation.ExpectedBinding binding,Optional<ModernEquipmentOverrideStore.Stored> stored,long now,long shape) {
        var profile=ModernVipCharacterProfile.qualify(source,binding,stored,now,shape);
        var ledger=ModernVipLifecycle.start(actor,profile.vip(),now);
        return new ModernVipCharacterState(source,stored,ledger,profile,observe(source,now));
    }
    static final class Candidate {
        private final ModernVipCharacterState before;
        private final ModernVipLifecycle.Candidate vip;
        private final Optional<ModernEquipmentOverrideStore.Stored> nextStored;
        private final Map<Long,ModernItemLifetime.View> leases;
        private final ModernVipCharacterProfile pending;
        private Candidate(ModernVipCharacterState before,ModernVipLifecycle.Candidate vip,
                Optional<ModernEquipmentOverrideStore.Stored> nextStored,Map<Long,ModernItemLifetime.View> leases,long now) {
            this.before=before;this.vip=vip;this.nextStored=nextStored;this.leases=Map.copyOf(leases);
            pending=ModernVipCharacterProfile.fromApplied(before.basis,nextStored,vip.after(),leases,now);
        }
        ModernVipLifecycle.Candidate vip(){return vip;}
        Optional<ModernEquipmentOverrideStore.Stored> expectedStore(){return before.stored;}
        Optional<ModernEquipmentOverrideStore.Stored> nextStore(){return nextStored;}
        ModernVipCharacterProfile pending(){return pending;}
        @Override public String toString(){return "VipCharacterCandidate[redacted]";}
    }
    static Candidate prepareEquip(ModernVipCharacterState before,ModernVipLoadout fresh,long id,long now) {
        return equipment(before,fresh,id,ModernVipLifecycle.Operation.EQUIP,now);
    }
    static Candidate prepareUnequip(ModernVipCharacterState before,ModernVipLoadout fresh,long id,long now) {
        return equipment(before,fresh,id,ModernVipLifecycle.Operation.UNEQUIP,now);
    }
    static Candidate prepareOrdinaryEquipment(ModernVipCharacterState before,ModernVipLoadout fresh,long now) {
        return equipment(before,fresh,0,ModernVipLifecycle.Operation.EQUIPMENT_REVISION,now);
    }
    private static Candidate equipment(ModernVipCharacterState before,ModernVipLoadout fresh,long id,ModernVipLifecycle.Operation op,long now) {
        current(before,now);Objects.requireNonNull(fresh);
        if(fresh.evaluatedAtMillis()!=now)throw invalid();
        sameSource(before.basis,fresh.source());
        var nextLeases=new TreeMap<>(before.leases);var observation=observe(fresh.source(),now);
        for(var e:observation.entrySet())nextLeases.put(e.getKey(),ModernVipLoadout.earlier(before.leases.get(e.getKey()),e.getValue(),now));
        var bounded=fresh.withLifetimeCeilings(nextLeases);
        var candidate=switch(op) {
            case EQUIP->ModernVipLifecycle.prepareEquip(before.ledger,bounded,id,now);
            case UNEQUIP->ModernVipLifecycle.prepareUnequip(before.ledger,bounded,id,now);
            case EQUIPMENT_REVISION->ModernVipLifecycle.prepareEquipmentRevision(before.ledger,bounded,now);
            default->throw invalid();
        };
        // Persistent entries outside this operation are not silently discarded,
        // including stale/unrelated entries hidden by reconciliation.
        var previous=entries(before.stored);var next=entries(fresh.stored());
        if(op==ModernVipLifecycle.Operation.EQUIPMENT_REVISION) {
            if(previous.equals(next))throw invalid();
            for(long vipId:before.leases.keySet())if(!Objects.equals(previous.get(vipId),next.get(vipId)))throw invalid();
            var changes=new java.util.HashSet<>(previous.keySet());changes.addAll(next.keySet());
            boolean effectiveChange=false;
            var oldFlags=flags(before.profile.character());
            var reconciled=ModernEquipmentReconciliation.reconcile(before.basis.input().equipment(),before.ledger.binding(),fresh.stored(),now);
            for(long itemId:changes)if(!Objects.equals(previous.get(itemId),next.get(itemId))) {
                if(!before.basis.input().equipment().items().containsKey(itemId)||ModernVipLoadout.isVip(before.basis,itemId))throw invalid();
                var entry=next.get(itemId);var instance=before.basis.input().equipment().items().get(itemId);
                if(entry!=null&&(entry.catalogId()!=instance.source().template().catalogId()
                        ||!entry.sourceItem().equals(reconciled.sourceFingerprints().get(itemId))))throw invalid();
            }
            for(var flag:reconciled.desiredLoadout().items())if(!ModernVipLoadout.isVip(before.basis,flag.objectId())
                    &&oldFlags.get(flag.objectId())!=flag.equipped())effectiveChange=true;
            if(!effectiveChange)throw invalid();
        } else {
            previous.remove(id);next.remove(id);if(!previous.equals(next))throw invalid();
        }
        return new Candidate(before,candidate,fresh.stored(),nextLeases,now);
    }
    /** Uses the actual applied template even if today's catalog no longer has it.
     * nextStore is a proposal only; the caller must perform an authorized CAS. */
    static Candidate prepareRetainedUnequip(ModernVipCharacterState before,long id,long now) {
        current(before,now);var vip=ModernVipLifecycle.prepareRetainedUnequip(before.ledger,id,now);
        var entries=entries(before.stored);var q=before.basis.input().equipment();var item=q.items().get(id);
        entries.put(id,new ModernEquipmentOverrideStore.ItemOverride(id,item.source().template().catalogId(),
                ModernEquipmentReconciliation.fingerprint(item,q.lifetimes().items().get(id)),false));
        var next=new ModernEquipmentOverrideStore.Stored(before.ledger.actor().owner(),vip.after().equipmentRevision(),
                new ModernEquipmentOverrideStore.Replacement(before.ledger.binding().fingerprint(),java.util.List.copyOf(entries.values())));
        return new Candidate(before,vip,Optional.of(next),before.leases,now);
    }
    static Optional<Candidate> prepareClearShape(ModernVipCharacterState before,long now) {
        current(before,now);
        return ModernVipLifecycle.prepareClearShape(before.ledger,now).map(v->new Candidate(before,v,before.stored,before.leases,now));
    }
    static Optional<Candidate> prepareExpiry(ModernVipCharacterState before,long now) {
        if(now<before.ledger.observedMillis())throw invalid();
        var vip=ModernVipLifecycle.prepareExpiry(before.ledger,now);
        if(vip.isEmpty()) {
            long deadline=before.profile.character().validUntilMillis();
            // Unequipped source gear still contributes carrying weight. Its
            // retained lease can expire without changing equipped abilities.
            boolean sourceGearExpired=before.profile.character().sourceLifetimes().stream().anyMatch(v->
                    v.state()==ModernItemLifetime.State.ACTIVE&&v.expiresAtMillis()<=now
                    &&before.basis.input().equipment().items().get(v.itemObjectId()).source().template().kind()!=ModernCharacterData.ItemKind.ETC);
            if((deadline<0||now<deadline)&&!sourceGearExpired)return Optional.empty();
            vip=Optional.of(ModernVipLifecycle.prepareTimeRefresh(before.ledger,now));
        }
        return Optional.of(new Candidate(before,vip.orElseThrow(),before.stored,before.leases,now));
    }
    /** Pass CURRENT live HP/MP while holding the engine's lock, not values
     * captured during preparation. No increase heals, and zero HP stays zero. */
    static ModernVipCharacterState acceptAt(Candidate candidate,ModernVipCharacterState current,ModernVipLifecycle.Actor actor,
            long equipmentRevision,long now,long liveHp,long liveMp) {
        Objects.requireNonNull(candidate);if(candidate.before!=current)throw invalid();
        long until=candidate.pending.character().validUntilMillis();
        if(until>=0&&now>=until)throw invalid();
        // Even removal of the expiring item must be re-prepared at the boundary.
        long oldUntil=current.profile.character().validUntilMillis();
        if(oldUntil>=0&&candidate.pending.character().evaluatedAtMillis()<oldUntil&&now>=oldUntil)throw invalid();
        var ledger=ModernVipLifecycle.acceptAt(candidate.vip,actor,current.ledger,equipmentRevision,now);
        var profile=ModernVipCharacterProfile.fromApplied(current.basis,candidate.nextStored,ledger,candidate.leases,now)
                .retainVitals(current.profile,liveHp,liveMp);
        return new ModernVipCharacterState(current.basis,candidate.nextStored,ledger,profile,candidate.leases);
    }
    private static void current(ModernVipCharacterState before,long now) {
        if(now<before.ledger.observedMillis()||before.profile.character().validUntilMillis()>=0
                &&now>=before.profile.character().validUntilMillis())throw invalid();
    }
    private static Map<Long,ModernItemLifetime.View> observe(ModernPhysicalCombat.Source source,long now) {
        var result=new TreeMap<Long,ModernItemLifetime.View>();var q=source.input().equipment();
        for(var item:q.source().inventory())if(ModernVipLoadout.isVip(source,item.objectId())) {
            var life=q.lifetimes().view(item.objectId(),now);
            if(!life.usable()&&life.state()!=ModernItemLifetime.State.EXPIRED)throw invalid();
            result.put(item.objectId(),life);
        }
        return Map.copyOf(result);
    }
    private static TreeMap<Long,ModernEquipmentOverrideStore.ItemOverride> entries(Optional<ModernEquipmentOverrideStore.Stored> stored) {
        var result=new TreeMap<Long,ModernEquipmentOverrideStore.ItemOverride>();
        stored.ifPresent(s->s.value().overrides().forEach(e->result.put(e.itemObjectId(),e)));return result;
    }
    private static Map<Long,Boolean> flags(ModernCombatProfile profile) {
        var result=new TreeMap<Long,Boolean>();for(var flag:profile.equipped())result.put(flag.objectId(),flag.equipped());return result;
    }
    /** Non-VIP/base/instance changes need re-admission, not a hybrid snapshot.
     * Only VIP catalog rows and observation clocks may differ in this route. */
    private static void sameSource(ModernPhysicalCombat.Source a,ModernPhysicalCombat.Source b) {
        var x=a.input().equipment();var y=b.input().equipment();var c=x.catalog();var d=y.catalog();
        if(!x.source().equals(y.source())||!x.items().equals(y.items())||!x.lifetimes().items().equals(y.lifetimes().items())
                ||a.input().weaponsQualified()!=b.input().weaponsQualified()||!a.input().weapons().equals(b.input().weapons())
                ||c.sourceId()!=d.sourceId()||!c.resourceDatabase().equals(d.resourceDatabase())||c.vipTableState()!=d.vipTableState()
                ||!c.verifiedSetIds().equals(d.verifiedSetIds())||!c.sets().equals(d.sets())||!c.combos().equals(d.combos())
                ||!nonVipRules(c).equals(nonVipRules(d)))throw invalid();
        var normalized=new ModernPhysicalCombat.Source(a.input(),b.growth(),b.templates(),b.procCatalogIds(),b.magicGrowth(),
                b.magicBows(),b.weaponArrowSprites(),b.regenGrowth(),b.healingGrowth(),a.vipCatalog(),b.armorProcs(),b.weaponProcCatalogIds());
        if(!a.equals(normalized))throw invalid();
    }
    private static Map<ModernEquipmentFacts.RuleKey,ModernEquipmentFacts.RuleValue> nonVipRules(ModernEquipmentFacts.Catalog catalog) {
        var result=new java.util.HashMap<>(catalog.lookups());result.keySet().removeIf(k->k.kind()==ModernEquipmentFacts.RuleKind.VIP);return result;
    }
    ModernVipCharacterProfile profile(){return profile;}
    /** Fresh ownership/store/source proof without replaying today's VIP catalog
     * over already-applied templates. A mismatch needs re-admission, not a SET
     * on a hybrid character. This check neither grants a lease nor performs IO. */
    void requireSource(ModernPhysicalCombat.Source fresh,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<ModernEquipmentOverrideStore.Stored> currentStored,long now) {
        current(this,now);
        if(!ledger.binding().equals(binding)||!stored.equals(currentStored))throw invalid();
        sameSource(basis,Objects.requireNonNull(fresh));
    }
    ModernVipCharacterState rebind(ModernVipLifecycle.Actor actor) {
        return new ModernVipCharacterState(basis,stored,ModernVipLifecycle.rebind(ledger,actor),profile,leases);
    }
    ModernVipLifecycle.State ledger(){return ledger;}
    Optional<ModernEquipmentOverrideStore.Stored> stored(){return stored;}
    Map<Long,ModernItemLifetime.View> leases(){return leases;}
    @Override public String toString(){return "VipCharacterState[redacted]";}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid VIP character state");}
}
