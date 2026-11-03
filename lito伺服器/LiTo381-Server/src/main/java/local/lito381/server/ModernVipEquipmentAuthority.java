package local.lito381.server;

import static local.lito381.server.ModernEquipmentOverrideStore.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Full-VIP selected-worker authority. SQL is read-only; desired overrides go
 * only to the separate source/object store. Main requires explicit map/source
 * opt-in; this authority never uses the legacy gear DTO. Callers must own a current navigation Operation
 * and a reservation from the registered full-VIP engine. One immutable provider
 * read per call does not claim that multiple MyISAM queries are atomic. */
final class ModernVipEquipmentAuthority {
    record Quote(ModernBattleVipObservation.View view,Fingerprint token) {
        Quote {Objects.requireNonNull(view);Objects.requireNonNull(token);}
        ModernBattleVipCommandState.Proof proof(){return ModernBattleVipCommandState.from(view);}
        @Override public String toString(){return "VipEquipmentQuote[redacted]";}
    }
    record Request(ModernBattleVipCommandState.Proof proof,long itemObjectId,boolean desired,Fingerprint token) {
        Request {Objects.requireNonNull(proof);Objects.requireNonNull(token);if(itemObjectId<1)throw invalid();}
        @Override public String toString(){return "VipEquipmentRequest[redacted]";}
    }
    /** Private engine capture, never a wire DTO. Enables exact inspection
     * receipts without doing a later unrelated engine READ. */
    record Inspection(Quote quote,ModernBattleObservation.Capture observation) {
        Inspection {Objects.requireNonNull(quote);Objects.requireNonNull(observation);}
        @Override public String toString(){return "VipEquipmentInspection[redacted]";}
    }
    enum Status { UNCHANGED, APPLIED, PERSISTED_RETIRED, CONFLICT, DENIED }
    record Result(Status status,EquipmentRules.Reason denial,long blockingItem,Quote quote,
            ModernBattleEngine.EquipmentPublication publication,ModernBattleObservation.Capture observation) {
        Result {
            Objects.requireNonNull(status);Objects.requireNonNull(observation);
            if((status==Status.DENIED)!=(denial!=null)||blockingItem<0||status!=Status.DENIED&&blockingItem!=0)throw invalid();
            boolean published=status==Status.UNCHANGED||status==Status.APPLIED||status==Status.PERSISTED_RETIRED;
            if(published!=(publication!=null)||published&& !publication.disposition().name().equals(status.name())
                    ||(status==Status.APPLIED||status==Status.UNCHANGED)&&(quote==null
                        ||quote.view().personal().equipment().versions().savedEquipmentRevision()!=publication.equipmentRevision())
                    ||status==Status.PERSISTED_RETIRED&&quote!=null)throw invalid();
        }
        @Override public String toString(){return "VipEquipmentAuthorityResult[redacted]";}
    }
    private record Input(ModernEquipmentAuthority.PhysicalLoaded loaded,ModernEquipmentReconciliation.ExpectedBinding binding,
            Optional<Stored> stored,ModernVipCharacterState state,long now) {}
    private final ModernEquipmentAuthority.PhysicalProvider provider;
    private final ModernEquipmentOverrideStore store;
    private final LongSupplier clock;
    private final AtomicLong highWater=new AtomicLong(-1);
    private static final EquipmentRules.Policy POLICY=new EquipmentRules.Policy(EquipmentRules.SealedItemPolicy.RETAINED_ALLOW);

    ModernVipEquipmentAuthority(ModernEquipmentAuthority.PhysicalProvider provider,ModernEquipmentOverrideStore store,LongSupplier clock) {
        this.provider=Objects.requireNonNull(provider);this.store=Objects.requireNonNull(store);this.clock=Objects.requireNonNull(clock);
    }
    boolean sourceEnabled(int source){return provider.fullVipSource(source);}
    boolean playerHealingSource(int source){return sourceEnabled(source)&&provider.playerHealingSource(source);}
    boolean armorProtectionSource(int source){return sourceEnabled(source)&&provider.armorProtectionSource(source);}
    /** Authenticated new-world read, using the actual selected backend lease.
     * Shape is explicit trusted state, not inferred from inventory order. The
     * Navigation entry currently starts without an active transformation. */
    ModernPhysicalCombat.Fighter prepareWorld(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow selected,ModernEquipmentLease.Operation operation,
            boolean armorProtection,long currentVipShape)throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(operation);
        var key=operation.key();var scope=operation.scope();
        if(!principal.authorizesSource(key.sourceId())||selected.objectId()!=key.objectId()
                ||ModernBackendScope.current()!=scope||!sourceEnabled(key.sourceId()))throw new SecurityException("invalid VIP world authority scope");
        scope.checkActive();var loaded=provider.readForWorld(principal,key.sourceId(),key.objectId(),armorProtection);var fresh=loaded.source();
        if(!fresh.input().weaponsQualified()||fresh.input().equipment().source().sourceId()!=key.sourceId()
                ||!fresh.input().equipment().source().character().equals(selected)||(loaded.protection()!=null)!=armorProtection)
            throw new IOException("VIP world source changed during preparation");
        scope.checkActive();var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,loaded.ownerBinding());var stored=store.read(key,scope);
        if(stored.isPresent()&&!stored.orElseThrow().value().binding().equals(binding.fingerprint()))throw new IOException("VIP owner binding requires reconciliation");
        long at=now();var state=ModernVipCharacterState.start(new ModernVipLifecycle.Actor(java.util.UUID.randomUUID(),key,1,0),fresh,binding,stored,at,currentVipShape);
        var fighter=loaded.protection()==null?ModernPhysicalCombat.qualifyVip(state,at):ModernPhysicalCombat.qualifyVipProtection(state,at,loaded.protection());
        operation.restrictCommit(at,fighter.profile().validUntilMillis(),this::commitNow);
        scope.checkActive();if(!operation.tryCommit())throw new IOException("VIP world preparation retired before publication");
        return fighter;
    }
    Quote inspect(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation,ModernBattleVipCommandState.Proof proof,ModernBattleEngine.EquipmentChange change)throws Exception {
        return inspectCaptured(principal,selected,operation,proof,change).quote();
    }
    Inspection inspectCaptured(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation,ModernBattleVipCommandState.Proof proof,ModernBattleEngine.EquipmentChange change)throws Exception {
        try(change) {
            var input=load(principal,selected,operation,proof,change);
            var token=token(input,proof,input.stored());
            operation.restrictCommit(input.now(),input.state().profile().character().validUntilMillis(),this::commitNow);
            if(!operation.tryCommit())throw new IOException("VIP equipment inspection retired");
            change.captureNoMutation();var view=view(change.observation(),proof);
            requireSameState(proof,view);operation.scope().checkActive();
            return new Inspection(new Quote(view,token),change.observation());
        }
    }
    Result execute(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation,Request request,ModernBattleEngine.EquipmentChange change)throws Exception {
        Objects.requireNonNull(request);
        try(change) {
            var input=load(principal,selected,operation,request.proof(),change);
            if(!token(input,request.proof(),input.stored()).equals(request.token()))
                return rejected(Status.CONFLICT,null,0,request.proof(),change);
            var source=input.loaded().source();var q=source.input().equipment();var item=q.items().get(request.itemObjectId());
            if(item==null)return rejected(Status.DENIED,EquipmentRules.Reason.UNKNOWN_ITEM,request.itemObjectId(),request.proof(),change);
            var before=input.state();var flags=new TreeMap<Long,Boolean>();
            before.profile().character().equipped().forEach(f->flags.put(f.objectId(),f.equipped()));
            boolean vip=ModernVipLoadout.isVip(source,request.itemObjectId());
            ModernVipCharacterState.Candidate candidate=null;
            if(vip) {
                // A no-op is still source/lease/version checked, but does not
                // renew a template or create an artificial saved tombstone.
                if(flags.get(request.itemObjectId())!=request.desired()) {
                    if(!request.desired())candidate=ModernVipCharacterState.prepareRetainedUnequip(before,request.itemObjectId(),input.now());
                    else {
                        var life=ModernVipLoadout.earlier(before.leases().get(request.itemObjectId()),q.lifetimes().view(request.itemObjectId(),input.now()),input.now());
                        if(!life.usable())return rejected(Status.DENIED,EquipmentRules.Reason.UNQUALIFIED_ITEM,request.itemObjectId(),request.proof(),change);
                        var template=source.vipCatalog()==null?null:source.vipCatalog().vips().get(item.source().template().catalogId());
                        if(template==null)return rejected(Status.DENIED,EquipmentRules.Reason.UNQUALIFIED_ITEM,request.itemObjectId(),request.proof(),change);
                        for(var applied:before.ledger().items())if(applied.template().exclusiveType()==template.exclusiveType())
                            return rejected(Status.DENIED,EquipmentRules.Reason.SLOT_OCCUPIED,applied.objectId(),request.proof(),change);
                        var next=replace(input,List.of(new EquipmentRules.Change(request.itemObjectId(),true)));
                        long shape=template.polymorphId()!=0?template.polymorphId():before.ledger().shape().map(ModernVipEquipment.Polymorph::graphicsId).orElse(0L);
                        var fresh=ModernVipLoadout.qualifyForChange(proposalSource(input,request.itemObjectId()),input.binding(),Optional.of(next),input.now(),shape,before.leases());
                        candidate=ModernVipCharacterState.prepareEquip(before,fresh,request.itemObjectId(),input.now());
                    }
                }
            } else {
                // Current engine flags retain expired VIP leases and applied
                // templates; fresh SQL flags must not resurrect those layers.
                var loadout=new EquipmentRules.Loadout(q.ruleLoadout().items().stream().map(i->i.withEquipped(flags.get(i.objectId()))).toList());
                var actor=new EquipmentRules.Actor(EquipmentRules.CharacterClass.values()[selected.characterClass()],selected.level());
                var decision=EquipmentRules.setEquipped(actor,loadout,request.itemObjectId(),request.desired(),POLICY);
                if(decision instanceof EquipmentRules.Rejection denied)
                    return rejected(Status.DENIED,denied.reason(),denied.blockingObjectId(),request.proof(),change);
                var transition=(EquipmentRules.Transition)decision;
                if(transition.changed()) {
                    if(request.desired()&&!before.profile().character().sourceLifetimes().stream().filter(l->l.itemObjectId()==request.itemObjectId()).findFirst().orElseThrow().usable())
                        return rejected(Status.DENIED,EquipmentRules.Reason.UNQUALIFIED_ITEM,request.itemObjectId(),request.proof(),change);
                    var next=replace(input,transition.changes());
                    var fresh=ModernVipLoadout.qualifyForChange(proposalSource(input,0),input.binding(),Optional.of(next),input.now(),
                            before.ledger().shape().map(ModernVipEquipment.Polymorph::graphicsId).orElse(0L),before.leases());
                    candidate=ModernVipCharacterState.prepareOrdinaryEquipment(before,fresh,input.now());
                }
            }
            if(candidate==null) {
                operation.restrictCommit(input.now(),before.profile().character().validUntilMillis(),this::commitNow);
                if(!operation.tryCommit())throw new IOException("VIP equipment no-op retired");
                var publication=change.confirmUnchanged();var capture=change.observation();var view=view(capture,request.proof());
                requireSameState(request.proof(),view);operation.scope().checkActive();
                return new Result(Status.UNCHANGED,null,0,new Quote(view,request.token()),publication,capture);
            }
            change.prepareVip(candidate,input.now());
            var next=candidate.nextStore().orElseThrow();
            var oldVersions=request.proof().versions();var nextProof=new ModernBattleVipCommandState.Proof(request.proof().life(),
                    new ModernBattleVipObservation.Versions(next.revision(),Math.incrementExact(oldVersions.runtimeAbilityRevision()),
                        Math.incrementExact(oldVersions.lifecycleSequence())));
            // Bounded serialization/hashing is completed before irreversible IO.
            var afterToken=token(input,nextProof,Optional.of(next));
            operation.restrictCommit(input.now(),minimum(before.profile().character().validUntilMillis(),candidate.pending().character().validUntilMillis()),this::commitNow);
            Commit committed;
            try {committed=store.compareAndSet(input.binding().key(),input.stored().map(Expected::of).orElseGet(Expected::absent),
                    next.value(),operation.scope(),operation,change::admit);}
            catch(StoreException failure) {
                if(failure.failure()!=Failure.CONFLICT)throw failure;
                return rejected(Status.CONFLICT,null,0,request.proof(),change);
            }
            if(!committed.changed()||!committed.stored().equals(next))throw new IOException("VIP durable outcome requires reconciliation");
            // Always resolve admitted durable state, even if cancellation means
            // no response can be sent. Never synthesize rollback after rename.
            var publication=change.publish(committed);var capture=change.observation();
            operation.scope().checkActive();
            if(capture==null)throw new IOException("VIP world closed after durable save");
            if(publication.disposition()==ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED)
                return new Result(Status.PERSISTED_RETIRED,null,0,null,publication,capture);
            var view=view(capture,request.proof());
            if(!view.personal().equipment().versions().equals(nextProof.versions()))throw new IOException("VIP publication version mismatch");
            return new Result(Status.APPLIED,null,0,new Quote(view,afterToken),publication,capture);
        }
    }
    private Input load(ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation,ModernBattleVipCommandState.Proof proof,ModernBattleEngine.EquipmentChange change)throws Exception {
        Objects.requireNonNull(principal);Objects.requireNonNull(selected);Objects.requireNonNull(operation);Objects.requireNonNull(change);
        var key=operation.key();var player=proof.life().player();
        if(!principal.authorizesSource(key.sourceId())||selected.objectId()!=key.objectId()
                ||key.sourceId()!=player.sourceId()||key.objectId()!=player.objectId()||ModernBackendScope.current()!=operation.scope())
            throw new SecurityException("invalid VIP equipment authority scope");
        change.requireVipRequest(proof,operation);operation.scope().checkActive();
        var loaded=provider.readForWorld(principal,key.sourceId(),key.objectId(),change.armorProtectionEnabled());
        var q=loaded.source().input().equipment();
        if(!loaded.source().input().weaponsQualified()||loaded.source().vipCatalog()==null
                ||q.source().sourceId()!=key.sourceId()||!q.source().character().equals(selected))
            throw new IOException("VIP selected source changed");
        var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,loaded.ownerBinding());
        var stored=store.read(key,operation.scope());long now=now();
        change.requireVipSource(loaded,binding,stored,now);operation.scope().checkActive();
        return new Input(loaded,binding,stored,change.vipState(),now);
    }
    /** Proposal-only overlay of already-applied templates. The token and source
     * check use the actual fresh input, never this overlay. Current clocks and
     * ordinary facts remain fresh; expiry ceilings are applied by the loadout.
     * Newly equipped VIPs use today's complete row, not a pinned sibling row. */
    private static ModernPhysicalCombat.Source proposalSource(Input input,long newlyEquippedVip) {
        var fresh=input.loaded().source();var old=input.state().profile().vip().source();
        long newShape=newlyEquippedVip==0?0:fresh.vipCatalog().vips().get(fresh.input().equipment().items().get(newlyEquippedVip).source().template().catalogId()).polymorphId();
        var vips=new TreeMap<>(old.vipCatalog().vips());vips.putAll(fresh.vipCatalog().vips());
        var q=fresh.input().equipment();var rules=new java.util.HashMap<>(old.input().equipment().catalog().lookups());rules.putAll(q.catalog().lookups());
        for(var applied:input.state().ledger().items()) {
            var row=applied.template();vips.put(row.catalogId(),row);
            rules.put(new ModernEquipmentFacts.RuleKey(ModernEquipmentFacts.RuleKind.VIP,ModernCharacterData.ItemKind.ETC,row.catalogId(),0),
                    ModernEquipmentFacts.RuleValue.present(ModernVipLoadout.legacyProjection(row.abilities())));
        }
        var polys=new TreeMap<Long,ModernVipEquipment.Polymorph>();
        for(var vip:vips.values())if(vip.polymorphId()!=0) {
            long id=vip.polymorphId();var current=input.state().ledger().shape().filter(s->s.graphicsId()==id);
            var row=id==newShape?fresh.vipCatalog().polymorphs().get(id)
                    :current.orElseGet(()->fresh.vipCatalog().polymorphs().getOrDefault(id,old.vipCatalog().polymorphs().get(id)));
            if(row==null)throw invalid();polys.put(id,row);
        }
        var c=q.catalog();var catalog=new ModernEquipmentFacts.Catalog(c.sourceId(),c.resourceDatabase(),rules,c.verifiedSetIds(),c.sets(),c.combos(),c.vipTableState());
        var equipment=new ModernEquipmentFacts.Qualified(q.source(),q.items(),catalog,q.lifetimes());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(equipment,fresh.input().weaponsQualified(),fresh.input().weapons()),
                fresh.growth(),fresh.templates(),fresh.procCatalogIds(),fresh.magicGrowth(),fresh.magicBows(),fresh.weaponArrowSprites(),fresh.regenGrowth(),fresh.healingGrowth(),
                new ModernVipEquipment.Catalog(fresh.vipCatalog().sourceId(),fresh.vipCatalog().database(),vips,polys),fresh.armorProcs(),fresh.weaponProcCatalogIds());
    }
    private static Stored replace(Input input,List<EquipmentRules.Change> changes) {
        var rows=new TreeMap<Long,ItemOverride>();input.stored().ifPresent(s->s.value().overrides().forEach(r->rows.put(r.itemObjectId(),r)));
        var q=input.loaded().source().input().equipment();
        for(var change:changes) {
            var item=q.items().get(change.objectId());
            if(change.equipped()==item.source().equipped())rows.remove(change.objectId());
            else rows.put(change.objectId(),new ItemOverride(change.objectId(),item.source().template().catalogId(),
                    ModernEquipmentReconciliation.fingerprint(item,q.lifetimes().items().get(change.objectId())),change.equipped()));
        }
        return new Stored(input.binding().key(),Math.incrementExact(input.state().ledger().equipmentRevision()),
                new Replacement(input.binding().fingerprint(),List.copyOf(rows.values())));
    }
    private static Result rejected(Status status,EquipmentRules.Reason denial,long blocking,ModernBattleVipCommandState.Proof proof,
            ModernBattleEngine.EquipmentChange change) {
        change.captureNoMutation();return new Result(status,denial,blocking,null,null,change.observation());
    }
    private static ModernBattleVipObservation.View view(ModernBattleObservation.Capture capture,ModernBattleVipCommandState.Proof proof) {
        return ModernBattleVipObservation.from(capture).forOwner(proof.life().player());
    }
    private static void requireSameState(ModernBattleVipCommandState.Proof proof,ModernBattleVipObservation.View view)throws IOException {
        var current=ModernBattleVipCommandState.from(view);
        if(view.world().phase()!=ModernBattleEngine.Phase.ACTIVE||view.personal().history().resources().vitals().hp()==0
                ||!proof.life().world().equals(current.life().world())||proof.life().entity()!=current.life().entity()
                ||proof.life().life()!=current.life().life()||!proof.versions().equals(current.versions()))
            throw new IOException("VIP inspection changed before publication");
    }
    /** Domain separated, full-width, sorted current-source token. Non-VIP
     * growth/proc/instance facts are additionally pinned by requireVipSource.
     * Observation clocks and world revision are intentionally not generations;
     * earliest leases and the original life/state proof are checked separately. */
    private static Fingerprint token(Input input,ModernBattleVipCommandState.Proof proof,Optional<Stored> stored)throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            out.writeUTF("LiTo381.FullVip.Equipment.v1");out.write(input.binding().fingerprint().bytes());
            var p=proof.life();out.writeInt(p.player().sourceId());out.writeLong(p.player().objectId());out.writeLong(p.player().epoch());out.writeInt(p.player().channelId());
            out.writeLong(p.world().roomId());out.writeLong(p.world().matchId());out.writeLong(p.world().generation());out.writeLong(p.entity());out.writeLong(p.life());
            out.writeLong(proof.versions().savedEquipmentRevision());out.writeLong(proof.versions().runtimeAbilityRevision());out.writeLong(proof.versions().lifecycleSequence());
            var q=input.loaded().source().input().equipment();out.write(ModernEquipmentBinding.catalogGeneration(q.catalog()).fingerprint().bytes());
            out.writeInt(q.source().inventory().size());
            for(var item:q.source().inventory())out.write(ModernEquipmentReconciliation.fingerprint(q.items().get(item.objectId()),q.lifetimes().items().get(item.objectId())).bytes());
            var vip=input.loaded().source().vipCatalog();out.writeBoolean(vip!=null);
            if(vip!=null) {
                out.writeInt(vip.absentVips().size());
                for(long id:new java.util.TreeSet<>(vip.absentVips()))out.writeLong(id);
                out.writeInt(vip.sourceId());out.writeUTF(vip.database());out.writeInt(vip.vips().size());
                for(var row:new TreeMap<>(vip.vips()).values()) {
                    out.writeLong(row.catalogId());out.writeLong(row.exclusiveType());out.writeLong(row.iconId());out.writeLong(row.polymorphId());
                    for(var field:ModernVipEquipment.Field.values())out.writeLong(row.abilities().get(field));
                }
                out.writeInt(vip.polymorphs().size());
                for(var row:new TreeMap<>(vip.polymorphs()).values()) {
                    out.writeLong(row.graphicsId());out.writeBoolean(row.present());out.writeBoolean(row.noDeathExpLoss());out.writeBoolean(row.retainsShapeOnCancellation());
                    for(var field:ModernVipEquipment.Field.values())out.writeLong(row.abilities().get(field));
                }
            }
            out.writeBoolean(stored.isPresent());
            if(stored.isPresent()) {
                var s=stored.orElseThrow();out.writeLong(s.revision());out.write(s.value().binding().bytes());out.writeInt(s.value().overrides().size());
                for(var row:s.value().overrides()){out.writeLong(row.itemObjectId());out.writeLong(row.catalogId());out.write(row.sourceItem().bytes());out.writeBoolean(row.equipped());}
            }
        }
        return new Fingerprint(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    }
    private long commitNow(){long now=clock.getAsLong();if(now<0)return -1;long prior=highWater.getAndAccumulate(now,Math::max);return now<prior?-1:now;}
    private long now()throws IOException {long now=commitNow();if(now<0)throw new IOException("VIP equipment clock unavailable");return now;}
    private static long minimum(long a,long b){return a<0?b:b<0?a:Math.min(a,b);}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid full VIP equipment authority input");}
}
