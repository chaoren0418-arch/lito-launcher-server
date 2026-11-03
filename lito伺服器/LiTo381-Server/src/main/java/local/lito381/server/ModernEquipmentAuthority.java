package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modern-only equipment authority. Called exclusively through Navigation.equipment,
 * whose actual Work owns the opaque selection Operation. SQL remains read-only;
 * only the separate source/object-keyed store receives desired-equipped overrides.
 * Each action pins ONE immutable qualified input bundle and content generation.
 * This does not claim its provider's multiple MyISAM queries form an atomic snapshot.
 */
final class ModernEquipmentAuthority {
    static final int OK=0, DENIED=1, BACKEND_UNAVAILABLE=2, BUSY=3, CONFLICT=4, BINDING_MISMATCH=5;
    static final int READ=0, SET=1;
    static final Fingerprint ZERO = new Fingerprint(new byte[32]);
    record Request(int sourceId,long objectId,long epoch,int action,boolean desired,
                   long itemObjectId,long expectedRevision,Fingerprint expectedToken) {
        Request {
            if(sourceId<=0 || objectId<=0 || epoch==0 || action<READ || action>SET || expectedRevision<0
                    || expectedToken==null || (action==READ && (desired || itemObjectId!=0 || expectedRevision!=0 || !ZERO.equals(expectedToken)))
                    || (action==SET && itemObjectId<=0)) throw invalid();
        }
    }
    record Loaded(Qualified qualified,Fingerprint ownerBinding) {
        Loaded { Objects.requireNonNull(qualified); Objects.requireNonNull(ownerBinding); }
        @Override public String toString(){return "EquipmentAuthorityInput[redacted]";}
    }
    @FunctionalInterface interface Provider {
        Loaded read(ModernAccountAuthentication.Principal principal,int sourceId,long objectId) throws Exception;
    }
    record CombatLoaded(ModernCombatProfile.Input input, Fingerprint ownerBinding) {
        CombatLoaded { Objects.requireNonNull(input); Objects.requireNonNull(ownerBinding); }
        @Override public String toString(){return "CombatAuthorityInput[redacted]";}
    }
    @FunctionalInterface interface CombatProvider {
        CombatLoaded read(ModernAccountAuthentication.Principal principal,int sourceId,long objectId) throws Exception;
    }
    record PhysicalLoaded(ModernPhysicalCombat.Source source,Fingerprint ownerBinding,
            ModernArmorProtectionCatalog.BoundSource protection) {
        PhysicalLoaded(ModernPhysicalCombat.Source source,Fingerprint ownerBinding){this(source,ownerBinding,null);}
        PhysicalLoaded {
            Objects.requireNonNull(source);Objects.requireNonNull(ownerBinding);
            // Never splice a skill catalog onto a separately read physical bundle.
            if(protection!=null&&protection.physical()!=source)throw invalid();
        }
        @Override public String toString(){return "PhysicalAuthorityInput[redacted]";}
    }
    @FunctionalInterface interface PhysicalProvider {
        PhysicalLoaded read(ModernAccountAuthentication.Principal principal,int sourceId,long objectId) throws Exception;
        /** Configuration opt-in, not a learned grant or an authorization check. */
        default boolean playerHealingSource(int sourceId){return false;}
        default boolean armorProtectionSource(int sourceId){return false;}
        default boolean fullVipSource(int sourceId){return false;}
        /** The immutable selected world/reservation chooses the contract, not
         * a packet flag or a capability alone. Never strip an already read bundle. */
        default PhysicalLoaded readForWorld(ModernAccountAuthentication.Principal principal,int sourceId,long objectId,boolean armorProtection)throws Exception {
            var loaded=read(principal,sourceId,objectId);
            if((loaded.protection()!=null)!=armorProtection)throw new IllegalStateException("physical source differs from selected world");
            return loaded;
        }
    }
    record Equipped(long objectId,boolean equipped) {
        Equipped { if(objectId<=0)throw invalid(); }
    }
    record Snapshot(ModernCharacterData.Snapshot source,long revision,Fingerprint viewToken,Fingerprint catalogGeneration,
                    long evaluatedAtMillis,long nextStoneExpiry,Ability modifiers,long magicReductionBase,
                    List<Equipped> equipped,List<ModernEquipmentReconciliation.StaleItem> stale,
                    List<ModernItemLifetime.View> lifetimes, ModernInventoryWeight.Snapshot weight) {
        Snapshot(ModernCharacterData.Snapshot source,long revision,Fingerprint viewToken,Fingerprint catalogGeneration,
                    long evaluatedAtMillis,long nextStoneExpiry,Ability modifiers,long magicReductionBase,
                    List<Equipped> equipped,List<ModernEquipmentReconciliation.StaleItem> stale,
                    List<ModernItemLifetime.View> lifetimes) {
            this(source,revision,viewToken,catalogGeneration,evaluatedAtMillis,nextStoneExpiry,modifiers,magicReductionBase,equipped,stale,lifetimes,null);
        }
        Snapshot(ModernCharacterData.Snapshot source,long revision,Fingerprint viewToken,Fingerprint catalogGeneration,
                    long evaluatedAtMillis,long nextStoneExpiry,Ability modifiers,long magicReductionBase,
                    List<Equipped> equipped,List<ModernEquipmentReconciliation.StaleItem> stale) {
            this(source,revision,viewToken,catalogGeneration,evaluatedAtMillis,nextStoneExpiry,modifiers,magicReductionBase,equipped,stale,List.of());
        }
        Snapshot {
            Objects.requireNonNull(source);Objects.requireNonNull(viewToken);Objects.requireNonNull(catalogGeneration);Objects.requireNonNull(modifiers);
            equipped=List.copyOf(equipped);stale=List.copyOf(stale);lifetimes=List.copyOf(lifetimes);
            if(revision<0 || evaluatedAtMillis<0 || nextStoneExpiry< -1 || equipped.size()!=source.inventory().size()
                    || stale.size()>ModernCharacterData.MAX_ITEMS)throw invalid();
            for(int i=0;i<equipped.size();i++)if(equipped.get(i).objectId()!=source.inventory().get(i).objectId())throw invalid();
            if(!lifetimes.isEmpty()) {
                if(lifetimes.size()!=equipped.size())throw invalid();
                for(int i=0;i<lifetimes.size();i++)if(lifetimes.get(i).itemObjectId()!=equipped.get(i).objectId()
                        || !lifetimes.get(i).usable() && equipped.get(i).equipped())throw invalid();
            }
            long previous=0;
            for(var row:stale){if(row.itemObjectId()<=previous)throw invalid();previous=row.itemObjectId();}
        }
        Snapshot withWeight(ModernInventoryWeight.Snapshot weight) {
            var result = new Snapshot(source,revision,viewToken,catalogGeneration,evaluatedAtMillis,nextStoneExpiry,
                    modifiers,magicReductionBase,equipped,stale,lifetimes,weight);
            ModernWeightProtocol.validateEquipment(result); return result;
        }
        @Override public String toString(){return "EquipmentAuthoritySnapshot[redacted]";}
    }
    record Result(int status,boolean changed,int reason,long blockingItem,Snapshot snapshot) {
        Result {
            if(status<OK || status>BINDING_MISMATCH || blockingItem<0 || reason<0 || reason>EquipmentRules.Reason.values().length
                    || ((status==OK || status==DENIED || status==CONFLICT)!=(snapshot!=null))
                    || (changed && status!=OK) || ((status==DENIED)!=(reason!=0))
                    || (status!=DENIED && blockingItem!=0))throw invalid();
        }
        static Result unavailable(int status){return new Result(status,false,0,0,null);}
    }
    private final Provider provider;
    private final CombatProvider combatProvider;
    private final PhysicalProvider physicalProvider;
    private final ModernEquipmentOverrideStore store;
    private final LongSupplier clock;
    private final AtomicLong lastObservedTime = new AtomicLong(-1);
    private final EquipmentRules.Policy policy = new EquipmentRules.Policy(EquipmentRules.SealedItemPolicy.RETAINED_ALLOW);

    ModernEquipmentAuthority(Provider provider,ModernEquipmentOverrideStore store){this(provider,store,System::currentTimeMillis);}
    ModernEquipmentAuthority(Provider provider,ModernEquipmentOverrideStore store,LongSupplier clock){
        this(provider,null,store,clock);
    }
    ModernEquipmentAuthority(Provider provider,CombatProvider combatProvider,ModernEquipmentOverrideStore store){
        this(provider,combatProvider,store,System::currentTimeMillis);
    }
    ModernEquipmentAuthority(Provider provider,CombatProvider combatProvider,ModernEquipmentOverrideStore store,LongSupplier clock){
        this(provider,combatProvider,null,store,clock);
    }
    ModernEquipmentAuthority(Provider provider,CombatProvider combatProvider,PhysicalProvider physicalProvider,ModernEquipmentOverrideStore store){
        this(provider,combatProvider,physicalProvider,store,System::currentTimeMillis);
    }
    ModernEquipmentAuthority(Provider provider,CombatProvider combatProvider,PhysicalProvider physicalProvider,ModernEquipmentOverrideStore store,LongSupplier clock){
        this.provider=Objects.requireNonNull(provider);this.store=Objects.requireNonNull(store);this.clock=Objects.requireNonNull(clock);
        this.combatProvider=combatProvider;this.physicalProvider=physicalProvider;
    }

    boolean playerHealingSource(int sourceId){return physicalProvider!=null&&physicalProvider.playerHealingSource(sourceId);}
    boolean armorProtectionSource(int sourceId){return physicalProvider!=null&&physicalProvider.armorProtectionSource(sourceId);}

    /** Explicit selected-owner character rebuild. ONE physical source read and
     * owner-bound store revision, with the same cancellation/deadline/publication
     * lease as world preparation. Not wired to old gear packets or combat; the
     * trusted caller must supply a qualified shape choice (zero means none). */
    ModernVipCharacterProfile prepareVipCharacter(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow selected,ModernEquipmentLease.Operation operation,long currentVipShape)throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(operation);
        var key=operation.key();
        if(!principal.authorizesSource(key.sourceId())||selected.objectId()!=key.objectId()
                ||ModernBackendScope.current()!=operation.scope())throw new SecurityException("invalid VIP character authority scope");
        if(currentVipShape<0)throw invalid();
        var scope=operation.scope();scope.checkActive();
        if(physicalProvider==null)throw new IOException("VIP character source unavailable");
        var loaded=physicalProvider.read(principal,key.sourceId(),key.objectId());var fresh=loaded.source();
        if(!fresh.input().weaponsQualified()||fresh.input().equipment().source().sourceId()!=key.sourceId()
                ||!fresh.input().equipment().source().character().equals(selected))throw new IOException("VIP character changed during preparation");
        scope.checkActive();var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,loaded.ownerBinding());var stored=store.read(key,scope);
        if(bindingMismatch(stored,binding))throw new IOException("VIP owner binding requires reconciliation");
        var result=ModernVipCharacterProfile.qualify(fresh,binding,stored,now(),currentVipShape);
        operation.restrictCommit(result.character().evaluatedAtMillis(),result.character().validUntilMillis(),this::commitNow);
        scope.checkActive();if(!operation.tryCommit())throw new IOException("VIP character preparation retired before publication");
        return result;
    }

    /** Fresh source+catalog+growth+proc proof, FULL_NEW_MATCH only; no HP/store mutation. */
    ModernPhysicalCombat.Fighter preparePhysical(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow selected,ModernEquipmentLease.Operation operation) throws Exception {
        return preparePhysicalSelected(principal,selected,operation,null);
    }
    ModernPhysicalCombat.Fighter preparePhysicalForWorld(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow selected,ModernEquipmentLease.Operation operation,boolean armorProtection)throws Exception {
        return preparePhysicalSelected(principal,selected,operation,armorProtection);
    }
    private ModernPhysicalCombat.Fighter preparePhysicalSelected(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow selected,ModernEquipmentLease.Operation operation,Boolean armorProtection)throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(operation);
        var key=operation.key();
        if(!principal.authorizesSource(key.sourceId())||selected.objectId()!=key.objectId()
                ||ModernBackendScope.current()!=operation.scope())throw new SecurityException("invalid physical authority scope");
        var scope=operation.scope();scope.checkActive();
        if(physicalProvider==null)throw new IOException("physical source qualification unavailable");
        var loaded=armorProtection==null?physicalProvider.read(principal,key.sourceId(),key.objectId())
                :physicalProvider.readForWorld(principal,key.sourceId(),key.objectId(),armorProtection);var fresh=loaded.source();
        if(!fresh.input().weaponsQualified()||fresh.input().equipment().source().sourceId()!=key.sourceId()
                ||!fresh.input().equipment().source().character().equals(selected))throw new IOException("physical character changed during preparation");
        scope.checkActive();var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,loaded.ownerBinding());
        var stored=store.read(key,scope);
        if(bindingMismatch(stored,binding))throw new IOException("physical owner binding requires reconciliation");
        var result=loaded.protection()==null?ModernPhysicalCombat.qualify(fresh,binding,stored,now())
                :ModernPhysicalCombat.qualifyProtection(loaded.protection(),binding,stored,now());
        operation.restrictCommit(result.profile().evaluatedAtMillis(),result.profile().validUntilMillis(),this::commitNow);
        scope.checkActive();if(!operation.tryCommit())throw new IOException("physical preparation retired before publication");
        return result;
    }

    /**
     * Read-only world-preparation input; NOT wired into attacks or the public gear
     * packet. The trusted caller must obtain this operation through Navigation's
     * selected owner/epoch gate and retain it until its actual Work.close.
     * Reuses exactly one fresh source+catalog bundle and the current numeric-key
     * override revision; never calls the old int/global native equipment path.
     */
    ModernCombatProfile prepareCombat(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow selected, ModernEquipmentLease.Operation operation,
            ModernCombatProfile.VitalPolicy vitalPolicy) throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(operation);Objects.requireNonNull(vitalPolicy);
        var key=operation.key();
        if(!principal.authorizesSource(key.sourceId()) || selected.objectId()!=key.objectId()
                || ModernBackendScope.current()!=operation.scope())throw new SecurityException("invalid combat authority scope");
        var scope=operation.scope();scope.checkActive();
        if(combatProvider==null)throw new IOException("combat source qualification unavailable");
        var loaded=combatProvider.read(principal,key.sourceId(),key.objectId());
        var fresh=loaded.input();
        if(!fresh.weaponsQualified() || fresh.equipment().source().sourceId()!=key.sourceId()
                || !fresh.equipment().source().character().equals(selected))throw new IOException("combat character changed during preparation");
        scope.checkActive();
        var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,loaded.ownerBinding());
        var stored=store.read(key,scope);
        if(bindingMismatch(stored,binding))throw new IOException("combat owner binding requires reconciliation");
        var result=ModernCombatProfile.qualify(fresh,binding,stored,now(),vitalPolicy);
        operation.restrictCommit(result.evaluatedAtMillis(),result.validUntilMillis(),this::commitNow);
        scope.checkActive();
        // This is a read-publication admission, not a store write. It gives
        // lease invalidation the same linearization as existing no-op gear SET,
        // and pins the selection until the owner's actual Work.close.
        if(!operation.tryCommit())throw new IOException("combat preparation retired before publication");
        return result;
    }

    Result execute(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
                   ModernEquipmentLease.Operation operation,Request request) throws Exception {
        return execute(principal,selected,operation,request,null);
    }
    Result execute(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
                   ModernEquipmentLease.Operation operation,Request request,Double weightRate) throws Exception {
        return execute(principal,selected,operation,request,weightRate,null);
    }
    record MatchResult(Result equipment, ModernBattleEngine.EquipmentPublication publication,
            ModernBattleObservation.Capture observation) {}

    /** Selected-owner worker seam. The v41 service adds a negotiated, exact
     * live-equipment receipt; legacy SET cannot enter here. The caller obtains
     * the opaque reservation from its exact registered engine. */
    MatchResult executeInMatch(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation,Request request,ModernBattleEngine.EquipmentChange change) throws Exception {
        Objects.requireNonNull(change);
        try (change) {
            change.requireRequest(request,operation);
            var result=execute(principal,selected,operation,request,null,change);
            var publication=change.publication();
            if(publication==null)change.captureNoMutation();
            // Persisted-for-next-match is NOT an active-world success ACK. Its
            // explicit durable receipt survives, so callers must reconcile and
            // may not interpret BACKEND_UNAVAILABLE as a rolled-back SET.
            if(publication!=null && publication.disposition()==ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED)
                result=Result.unavailable(BACKEND_UNAVAILABLE);
            operation.scope().checkActive();
            return new MatchResult(result,publication,change.observation());
        }
    }
    private Result execute(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
                   ModernEquipmentLease.Operation operation,Request request,Double weightRate,
                   ModernBattleEngine.EquipmentChange change) throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(operation);Objects.requireNonNull(request);
        var key = new TournamentPresence.CharacterKey(request.sourceId(),request.objectId());
        if(!principal.authorizesSource(request.sourceId()) || selected.objectId()!=request.objectId() || !operation.matchesKey(key)
                || ModernBackendScope.current()!=operation.scope())throw new SecurityException("invalid equipment authority scope");
        var scope=operation.scope();scope.checkActive();
        try {
            PhysicalLoaded physical=null;
            if(change!=null) {
                if(physicalProvider==null)throw new IOException("live equipment physical source unavailable");
                physical=physicalProvider.readForWorld(principal,request.sourceId(),request.objectId(),change.armorProtectionEnabled());
            }
            // ONE immutable physical/equipment bundle, never two SQL reads
            // whose inventory, catalog or owner binding may disagree.
            Loaded input=physical==null?provider.read(principal,request.sourceId(),request.objectId())
                    :new Loaded(physical.source().input().equipment(),physical.ownerBinding());
            Qualified fresh=input.qualified();
            var currentCharacter=fresh.source().character();
            if(fresh.source().sourceId()!=key.sourceId() || currentCharacter.objectId()!=selected.objectId()
                    || !currentCharacter.name().equals(selected.name()) || currentCharacter.characterClass()!=selected.characterClass()
                    || currentCharacter.sex()!=selected.sex()) {
                return Result.unavailable(BACKEND_UNAVAILABLE);
            }
            var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,input.ownerBinding());
            Optional<Stored> current=store.read(key,scope);
            if(bindingMismatch(current,binding))return Result.unavailable(BINDING_MISMATCH);
            long now=now();
            if(change!=null)change.requireSource(physical.source(),binding,current,now,physical.protection());
            var reconciled=ModernEquipmentReconciliation.reconcile(fresh,binding,current,now);
            Fingerprint generation=ModernEquipmentBinding.catalogGeneration(fresh.catalog()).fingerprint();
            Snapshot before=weighted(snapshot(input,generation,current,reconciled,now),weightRate);
            if(request.action()==READ){scope.checkActive();requireCurrent(before);return new Result(OK,false,0,0,before);}
            if(before.revision()!=request.expectedRevision() || !before.viewToken().equals(request.expectedToken())) {
                requireCurrent(before);
                return new Result(CONFLICT,false,0,0,before);
            }
            if(request.desired() && fresh.items().containsKey(request.itemObjectId())
                    && !fresh.lifetimes().view(request.itemObjectId(),now).usable()) {
                requireCurrent(before);
                return new Result(DENIED,false,EquipmentRules.Reason.UNQUALIFIED_ITEM.ordinal()+1,request.itemObjectId(),before);
            }
            var actor=new EquipmentRules.Actor(EquipmentRules.CharacterClass.values()[currentCharacter.characterClass()],currentCharacter.level());
            var decision=EquipmentRules.setEquipped(actor,reconciled.desiredLoadout(),request.itemObjectId(),request.desired(),policy);
            if(decision instanceof EquipmentRules.Rejection rejected) {
                requireCurrent(before);
                return new Result(DENIED,false,rejected.reason().ordinal()+1,rejected.blockingObjectId(),before);
            }
            var transition=(EquipmentRules.Transition)decision;
            // Idempotent intent does not create a revision-1 tombstone merely
            // because the currently confirmed state came directly from SQL.
            if(!transition.changed()) {
                operation.restrictCommit(now,before.nextStoneExpiry(),this::commitNow);
                if(!operation.tryCommit())throw new IOException("equipment operation retired before acknowledgement");
                if(change!=null)change.confirmUnchanged();
                requireCurrent(before);
                return new Result(OK,false,0,0,before);
            }
            var replacements=new TreeMap<Long,ItemOverride>();
            current.ifPresent(value -> value.value().overrides().forEach(row -> replacements.put(row.itemObjectId(),row)));
            // Preserve unrelated stale records. Only the explicit requested
            // transition (including a replaced weapon) may change its entries.
            for(var itemChange:transition.changes()) {
                var source=fresh.items().get(itemChange.objectId()).source();
                if(itemChange.equipped()==source.equipped())replacements.remove(itemChange.objectId());
                else replacements.put(itemChange.objectId(),new ItemOverride(itemChange.objectId(),source.template().catalogId(),
                        reconciled.sourceFingerprints().get(itemChange.objectId()),itemChange.equipped()));
            }
            // Stale records remain recoverable. Never silently prune them to
            // fit a new item, and reject before a permit/file write rather than
            // turning a full store into an uncertain-commit disconnection.
            if(replacements.size()>ModernCharacterData.MAX_ITEMS)return Result.unavailable(BACKEND_UNAVAILABLE);
            var replacement=new Replacement(binding.fingerprint(),List.copyOf(replacements.values()));
            long nextRevision=Math.addExact(before.revision(),1);
            var prospective=Optional.of(new Stored(key,nextRevision,replacement));
            var candidate=ModernEquipmentReconciliation.reconcile(fresh,binding,prospective,now);
            if(!candidate.desiredLoadout().equals(transition.candidate()))throw invalid();
            // Eagerly validate every modifier/token/output BEFORE the permit and
            // atomic rename. No new JDBC read or fallible evaluation follows it.
            Snapshot after=weighted(snapshot(input,generation,prospective,candidate,now),weightRate);
            if(change!=null)change.prepare(physical.source(),binding,prospective.orElseThrow(),now,physical.protection());
            long boundary=before.nextStoneExpiry()<0?after.nextStoneExpiry():after.nextStoneExpiry()<0?before.nextStoneExpiry()
                    :Math.min(before.nextStoneExpiry(),after.nextStoneExpiry());
            operation.restrictCommit(now,boundary,this::commitNow);
            Commit committed;
            try { committed=store.compareAndSet(key,reconciled.expected(),replacement,scope,operation,
                    change==null?()->true:change::admit); }
            catch(StoreException conflict) {
                if(conflict.failure()!=Failure.CONFLICT)throw conflict;
                var latest=store.read(key,scope);
                if(bindingMismatch(latest,binding))return Result.unavailable(BINDING_MISMATCH);
                Snapshot conflictView=weighted(snapshot(input,generation,latest,
                        ModernEquipmentReconciliation.reconcile(fresh,binding,latest,now),now),weightRate);
                requireCurrent(conflictView);
                return new Result(CONFLICT,false,0,0,conflictView);
            }
            if(!committed.changed() || committed.stored().revision()!=after.revision()) {
                // This would contradict our prevalidated replacement; a success
                // must never be synthesized with a different durable revision.
                throw new IOException("equipment durable result requires reconciliation");
            }
            // Resolve the irreversible durable outcome even if cancellation or
            // expiry now prevents a network ACK. Never leave an admitted live
            // reservation behind merely because the client went away.
            if(change!=null)change.publish(committed);
            // A rename may already have committed. Expiry after admission does
            // not undo it; withhold the obsolete active ACK and require READ.
            requireCurrent(after);
            return new Result(OK,true,0,0,after);
        } catch(StoreException failure) {
            if(failure.failure()==Failure.COMMIT_UNCERTAIN || failure.failure()==Failure.INVALID_OPERATION)throw failure;
            scope.checkActive();return Result.unavailable(failure.failure()==Failure.BUSY?BUSY:BACKEND_UNAVAILABLE);
        }
    }
    private static boolean bindingMismatch(Optional<Stored> stored,ModernEquipmentReconciliation.ExpectedBinding binding){
        return stored.isPresent() && !stored.orElseThrow().value().binding().equals(binding.fingerprint());
    }
    private static Snapshot weighted(Snapshot snapshot,Double rate)throws Exception {
        if(rate==null)return snapshot;
        var basis=ModernInventoryWeight.qualify(snapshot,rate);
        var result=snapshot.withWeight(ModernInventoryWeight.evaluate(basis,List.of()));
        ModernEquipmentProtocol.validateSnapshot(result);
        return result;
    }
    private static Snapshot snapshot(Loaded input,Fingerprint generation,Optional<Stored> stored,
            ModernEquipmentReconciliation.Result reconciled,long now)throws Exception {
        var evaluation=ModernEquipmentEvaluator.evaluate(input.qualified(),reconciled.overrides(),now);
        Ability modifiers=Ability.zero();
        for(Stat stat:Stat.values())modifiers=modifiers.with(stat,evaluation.total(stat));
        var flags=reconciled.desiredLoadout().items().stream().map(row->new Equipped(row.objectId(),row.equipped())).toList();
        var lifetimes=input.qualified().lifetimes().views(input.qualified().source(),now);
        long nextExpiry=evaluation.nextStoneExpiry().orElse(-1);
        for(var life:lifetimes)if(life.state()==ModernItemLifetime.State.ACTIVE)
            nextExpiry=nextExpiry<0?life.expiresAtMillis():Math.min(nextExpiry,life.expiresAtMillis());
        var result=new Snapshot(input.qualified().source(),reconciled.expected().revision(),
                token(input,generation,stored,reconciled,lifetimes),generation,now,nextExpiry,
                modifiers,evaluation.magicReductionBase(),flags,reconciled.staleItems(),lifetimes);
        ModernEquipmentProtocol.validateSnapshot(result);
        return result;
    }
    private static Fingerprint token(Loaded input,Fingerprint generation,Optional<Stored> stored,
            ModernEquipmentReconciliation.Result reconciled,List<ModernItemLifetime.View> lifetimes)throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            out.writeUTF("LiTo381.ModernEquipment.View.v1");out.write(input.ownerBinding().bytes());out.write(generation.bytes());
            out.writeLong(reconciled.expected().revision());
            var source=input.qualified().source();
            byte[] encoded=ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterDataResult(1,source.sourceId(),
                    source.character().objectId(),1,0,source));
            out.writeInt(encoded.length);out.write(encoded);java.util.Arrays.fill(encoded,(byte)0);
            for(var item:source.inventory())out.write(reconciled.sourceFingerprints().get(item.objectId()).bytes());
            for(var life:lifetimes) {out.writeLong(life.itemObjectId());out.writeByte(life.state().ordinal());}
            List<ItemOverride> saved=stored.map(value->value.value().overrides()).orElse(List.of());
            out.writeInt(saved.size());
            for(var row:saved){out.writeLong(row.itemObjectId());out.writeLong(row.catalogId());out.write(row.sourceItem().bytes());out.writeBoolean(row.equipped());}
        }
        return new Fingerprint(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    }
    private long now() throws IOException {
        long value=commitNow();
        if(value<0)throw new IOException("equipment clock unavailable or moved backwards");
        return value;
    }
    /** Same high-water clock at actual store admission; constant-time, no I/O. */
    private long commitNow() {
        long value=clock.getAsLong();
        if(value<0)return -1;
        long prior=lastObservedTime.getAndAccumulate(value,Math::max);
        return value<prior?-1:value;
    }
    private void requireCurrent(Snapshot snapshot) throws IOException {
        long value=now();
        if(value<snapshot.evaluatedAtMillis() || snapshot.nextStoneExpiry()>=0 && value>=snapshot.nextStoneExpiry())
            throw new IOException("equipment lifetime changed before publication");
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid equipment authority data");}
}
