package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real frozen-room engine and isolated durable store; no public VIP protocol
 * or native rendering claim is made by these internal qualification tests. */
class ModernBattleEngineVipTest {
    @TempDir Path directory;

    @Test void bindsSameObjectIdToDistinctSourcesAndRejectsOldProjections() throws Exception {
        try(var f=new Fixture(false,Map.of(MAX_HP,4_294_967_299L,MAX_MP,4_294_967_307L))) {
            var c=f.active();assertTrue(c.hasVip());assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());
            var first=state(c,0).ledger().actor();var second=state(c,1).ledger().actor();
            assertEquals(first.context(),second.context());assertNotEquals(first.owner(),second.owner());
            assertEquals(3333,first.owner().objectId());assertEquals(3333,second.owner().objectId());
            assertEquals(1,first.entity());assertEquals(2,second.entity());assertEquals(0,first.life());
            assertEquals(4_294_967_399L,c.actors().get(0).resources().vitals().maximumHp());
            assertEquals(4_294_967_357L,c.actors().get(0).resources().vitals().maximumMp());
            assertThrows(IllegalArgumentException.class,c::world);
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            var p=f.players.get(1);
            assertThrows(SecurityException.class,()->f.engine.beginEquipmentChange(p.owner(),f.players.get(0).scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
    @Test void expiryIsAtomicAcrossActorsAndChangesAbilityButNotSavedRevision() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,100L,MAX_MP,80L,DAMAGE_REDUCTION,10L,MAGIC_REDUCTION,20L,HPR,9L))) {
            var before=f.active();f.epoch.set(2000);var after=f.view();
            assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());assertEquals(before.revision()+1,after.revision());
            for(var a:after.actors()) {
                var vip=a.loadout().fighter().vip();var v=a.resources();
                assertEquals(0,vip.state().ledger().equipmentRevision());assertTrue(vip.state().stored().isEmpty());
                assertEquals(1,v.equipmentRevision());assertEquals(100,v.vitals().hp());assertEquals(50,v.vitals().mp());
                assertEquals(100,v.flows().hpClipped());assertEquals(80,v.flows().mpClipped());
                assertEquals(0,a.loadout().fighter().defense().armorReduction());
                assertEquals(0,a.loadout().magic().defense().magicOnlyReduction());
                assertEquals(0,a.loadout().regen().values().equipmentHpRegen());
            }
            assertEquals(after,f.view()); // Expiry is not repeatedly applied.
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
        }
    }
    @Test void expiryDuringRandomCalculationDiscardsOldDamageAndLeavesWorldRetryable() throws Exception {
        var epoch=new AtomicLong(1000);var calls=new AtomicLong();
        try(var f=new Fixture(true,Map.of(MAX_HP,100L),epoch,b->{calls.incrementAndGet();epoch.set(2000);return 0;})) {
            f.active();var out=f.attack(0,1,2);assertTrue(calls.get()>0);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,out.status());
            var c=out.observation();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());
            assertEquals(0,c.actors().get(0).vital().attackSequence());
            assertEquals(100,c.actors().get(1).resources().vitals().hp());
            assertEquals(0,c.actors().get(1).resources().flows().damaged());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
        }
    }
    @Test void actualCasRebuildsFromDamageDuringRenameWithoutHealing() throws Exception {
        var during=new AtomicReference<ModernBattleObservation.Capture>();
        try(var f=new Fixture(false,Map.of(MAX_HP,100L));var change=f.activeReservation(0)) {
            var before=change.vipState();var candidate=ModernVipCharacterState.prepareRetainedUnequip(before,1,1000);
            change.prepareVip(candidate,1000);
            try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                @Override public void move(Path from,Path to)throws IOException {
                    var out=f.attack(1,1,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,out.status());during.set(out.observation());
                    ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                }
            })) {
                var commit=commit(store,change,candidate);assertEquals(1,commit.stored().revision());
                long hp=during.get().actors().get(0).resources().vitals().hp();assertTrue(hp<200);
                var published=change.publish(commit);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,published.disposition());
                var a=f.view().actors().get(0);assertEquals(Math.min(100,hp),a.resources().vitals().hp());
                assertEquals(a.resources().vitals().hp(),a.loadout().fighter().profile().effective().initialHp());
                assertEquals(during.get().actors().get(0).resources().flows().damaged(),a.resources().flows().damaged());
                assertEquals(commit.stored(),state(f.view(),0).stored().orElseThrow());
                assertEquals(0,state(f.view(),1).ledger().equipmentRevision());
                assertThrows(IllegalStateException.class,()->change.publish(commit));
            }
            try(var store=new ModernEquipmentOverrideStore(directory);var call=new ModernEquipmentOverrideStoreTest.Call(before.ledger().actor().owner())) {
                var saved=store.read(before.ledger().actor().owner(),call.scope).orElseThrow();assertEquals(1,saved.revision());
                assertFalse(saved.value().overrides().get(0).equipped());
                assertTrue(store.read(state(f.view(),1).ledger().actor().owner(),call.scope).isEmpty());
            }
        }
    }
    @Test void postExpiryEquipmentUsesIndependentSavedAndRuntimeVersions() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();f.epoch.set(2000);var c=f.view();assertEquals(1,c.actors().get(0).resources().equipmentRevision());
            try(var change=f.reserve(0);var store=new ModernEquipmentOverrideStore(directory)) {
                var before=change.vipState();var q=f.sources.get(0).input().equipment();
                var fresh=ModernVipLoadout.qualifyForRebuild(f.sources.get(0),ModernCombatProfileTest.binding(q),
                        ModernCombatProfileTest.stored(q,1,Map.of(3L,true)),2000,0);
                var candidate=ModernVipCharacterState.prepareOrdinaryEquipment(before,fresh,2000);
                change.prepareVip(candidate,2000);var commit=commit(store,change,candidate);var pub=change.publish(commit);
                assertEquals(1,pub.equipmentRevision());assertEquals(2,pub.resources().balance().equipmentRevision());
                var after=f.view().actors().get(0);assertEquals(100,after.resources().vitals().hp());assertEquals(140,after.resources().vitals().maximumHp());
                assertEquals(1,state(f.view(),0).ledger().equipmentRevision());assertEquals(2,after.loadout().fighter().profile().equipmentRevision());
                assertEquals(1,state(f.view(),0).profile().character().equipmentRevision());
            }
        }
    }
    @Test void expiryBeforeAdmissionRejectsDiskWriteWithoutAbortingActiveWorld() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,100L));var change=f.activeReservation(0);var store=new ModernEquipmentOverrideStore(directory)) {
            var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
            f.epoch.set(2000);
            assertEquals(ModernEquipmentOverrideStore.Failure.INVALID_OPERATION,assertThrows(ModernEquipmentOverrideStore.StoreException.class,
                    ()->commit(store,change,candidate)).failure());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().phase());
            try(var call=new ModernEquipmentOverrideStoreTest.Call(candidate.nextStore().orElseThrow().key())) {
                assertTrue(store.read(call.lease.key(),call.scope).isEmpty());
            }
        }
    }
    @Test void expiryAfterDurableAdmissionRetainsDiskEvidenceAndRetiresWorld() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,100L));var change=f.activeReservation(0)) {
            var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
            try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                @Override public void move(Path from,Path to)throws IOException {
                    f.epoch.set(2000);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().phase());
                    ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                }
            })) {
                var saved=commit(store,change,candidate);var out=change.publish(saved);
                assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,out.disposition());assertNull(out.resources());
                assertEquals(ModernBattleEngine.Phase.ABORTED,f.view().phase());
                try(var call=new ModernEquipmentOverrideStoreTest.Call(saved.stored().key())) {assertEquals(saved.stored(),store.read(saved.stored().key(),call.scope).orElseThrow());}
            }
        }
    }
    @Test void candidateFromAnotherMatchOrSourceCannotPublish() throws Exception {
        try(var f=new Fixture(false,Map.of(MAX_HP,100L));var other=new Fixture(false,Map.of(MAX_HP,100L));
                var a=f.activeReservation(0);var b=other.activeReservation(0);var sibling=f.reserve(1)) {
            for(var before:List.of(b.vipState(),sibling.vipState())) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(before,1,1000);
                assertThrows(IllegalArgumentException.class,()->a.prepareVip(candidate,1000));
            }
            assertEquals(0,state(f.view(),0).ledger().equipmentRevision());
        }
    }
    @Test void reentrantEquipmentPublicationInvalidatesInFlightAttack() throws Exception {
        var action=new AtomicReference<Runnable>();
        try(var f=new Fixture(false,Map.of(MAX_HP,100L),new AtomicLong(1000),b->{var r=action.getAndSet(null);if(r!=null)r.run();return 0;});
                var change=f.activeReservation(1);var store=new ModernEquipmentOverrideStore(directory)) {
            var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
            action.set(()->{try {assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(commit(store,change,candidate)).disposition());}
                catch(Exception e){throw new AssertionError(e);}});
            var out=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,out.status());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,out.observation().phase());
            assertEquals(0,out.observation().actors().get(0).vital().attackSequence());
            assertEquals(100,out.observation().actors().get(1).resources().vitals().hp());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
        }
    }
    @Test void expiryNoOpReceiptDoesNotPretendToHaveWrittenEquipment() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();f.epoch.set(2000);f.view();
            try(var change=f.reserve(0)) {
                var out=change.confirmUnchanged();assertEquals(ModernBattleEngine.EquipmentDisposition.UNCHANGED,out.disposition());
                assertEquals(0,out.equipmentRevision());assertEquals(1,out.resources().balance().equipmentRevision());
                assertTrue(state(f.view(),0).stored().isEmpty());
            }
        }
    }
    @Test void deathDuringRenameCannotBeUndoneByEquipmentPublication() throws Exception {
        // A signed VIP modifier leaves one HP. Retained unarmed combat does
        // not use equipment melee damage; do not fake a lethal equipment hit.
        try(var f=new Fixture(false,Map.of(MAX_HP,-99L));var change=f.activeReservation(0)) {
            var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
            try(var store=new ModernEquipmentOverrideStore(directory,new ModernEquipmentOverrideStore.FileOps(){
                @Override public void move(Path from,Path to)throws IOException {
                    var out=f.attack(1,1,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,out.status());
                    assertEquals(0,out.observation().actors().get(0).resources().vitals().hp());
                    ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                }
            })) {
                var out=change.publish(commit(store,change,candidate));assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,out.disposition());
                var a=f.view().actors().get(0);assertEquals(0,a.resources().vitals().hp());assertTrue(a.vital().deathRevision()>0);
                assertEquals(0,a.loadout().fighter().profile().effective().initialHp());assertEquals(0,a.resources().life());
                assertEquals(1,a.resources().flows().damaged());assertEquals(0,a.resources().flows().hpClipped());
            }
        }
    }
    @Test void expiryWhileDeadRetainsDeathAndClipsNoPhantomHp() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,-99L))) {
            f.active();var killed=f.attack(0,1,2).observation();assertEquals(0,killed.actors().get(1).resources().vitals().hp());
            f.epoch.set(2000);var after=f.view().actors().get(1);
            assertEquals(0,after.resources().vitals().hp());assertEquals(killed.actors().get(1).vital().deathRevision(),after.vital().deathRevision());
            assertEquals(0,after.resources().flows().hpClipped());assertEquals(1,after.resources().equipmentRevision());
        }
    }
    @Test void backwardsEpochStillAbortsInsteadOfExtendingVipLease() throws Exception {
        try(var f=new Fixture(true,Map.of(MAX_HP,100L))) {
            var before=f.active();f.epoch.set(999);var after=f.view();
            assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,after.abortReason());
            assertEquals(before.actors(),after.actors());
        }
    }
    @Test void ordinaryFeaturesAndQualifiedProtectionPermitInternalVipAdmission() throws Exception {
        try(var f=new Fixture(false,Map.of(MAX_HP,100L))) {
            var s=f.sources.get(0);var q=s.input().equipment();var row=q.source().character();var p=f.players.get(0);
            var old=ModernPhysicalCombat.qualify(s,ModernCombatProfileTest.binding(q),Optional.empty(),1000);
            var complete=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(s,0),1000);
            var cell=new ModernWorldMap.Cell(32770,32770);var skills=new ModernSkills.Snapshot(7,row,List.of());
            var ordinary=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,cell,200_000_000,old,700_000_000,skills,100_000_000);
            assertDoesNotThrow(()->ordinary.withVip(complete));
            var weighted=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,cell,200_000_000,old,700_000_000,
                    null,0,false,List.of(),ModernInventoryWeight.qualify(old.profile(),1));
            assertDoesNotThrow(()->weighted.withVip(complete));
            var protectionSource=ModernArmorProtectionLoadoutTest.withRules(s,Set.of(),Map.of());
            var protection=ModernPhysicalCombat.qualifyVipProtection(ModernVipCharacterStateTest.start(protectionSource,0),1000,
                    ModernArmorProtectionLoadoutTest.bound(protectionSource));
            var basic=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,cell,200_000_000,old,700_000_000);
            assertDoesNotThrow(()->basic.withVip(protection));
        }
    }
    static ModernEquipmentOverrideStore.Commit commit(ModernEquipmentOverrideStore store,ModernBattleEngine.EquipmentChange change,
            ModernVipCharacterState.Candidate candidate)throws Exception {
        var next=candidate.nextStore().orElseThrow();
        try(var call=new ModernEquipmentOverrideStoreTest.Call(next.key())) {
            return store.compareAndSet(next.key(),candidate.expectedStore().map(ModernEquipmentOverrideStore.Expected::of).orElseGet(ModernEquipmentOverrideStore.Expected::absent),
                    next.value(),call.scope,call.operation,change::admit);
        }
    }
    private static ModernVipCharacterState state(ModernBattleObservation.Capture c,int index){return c.actors().get(index).loadout().fighter().vip().state();}
    static ModernPhysicalCombat.Source source(int id,ModernApplicationProtocol.CharacterRow row,Map<ModernVipEquipment.Field,Long> fields,boolean timed) {
        var base=ModernVipCharacterStateTest.withArmor(ModernVipCombatProfileTest.source(fields,false),false);
        var q=base.input().equipment();var c=q.catalog();var life=q.lifetimes();
        var snapshot=new ModernCharacterData.Snapshot(id,row,q.source().persisted(),q.source().inventory());
        var qualified=new ModernEquipmentFacts.Qualified(snapshot,q.items(),new ModernEquipmentFacts.Catalog(id,c.resourceDatabase(),c.lookups(),c.verifiedSetIds(),c.sets(),c.combos(),c.vipTableState()),
                new ModernItemLifetime.Observation(id,row.objectId(),life.dbNowLocal(),life.queryStartedEpochMillis(),life.queryStartedNanos(),life.items(),life.dbClockUncertaintyMillis()));
        var result=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,base.input().weapons()),ModernPhysicalCombatTest.growth(id,1000,0),base.templates(),base.procCatalogIds(),
                ModernMagicCombatTest.growth(id),Map.of(),Map.of(),ModernNaturalRegenTest.growth(id,"catalog",5),null,
                new ModernVipEquipment.Catalog(id,"catalog",base.vipCatalog().vips(),base.vipCatalog().polymorphs()));
        return timed?ModernVipLifecycleTest.timed(result,1000,"2026-10-01 00:00:00",1):result;
    }
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final List<ModernPhysicalCombat.Source> sources=new ArrayList<>();
        final AtomicLong clock=new AtomicLong(1_000_000_000L),epoch;
        final ModernWorldMap map;final ModernRooms.FrozenPreparation ticket;final ModernBattleEngine engine;
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields)throws Exception {this(timed,fields,new AtomicLong(1000),b->0);}
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random)throws Exception {
            this(timed,fields,epoch,random,java.util.function.UnaryOperator.identity(),java.util.function.UnaryOperator.identity());
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare)throws Exception {
            this(timed,fields,epoch,random,sourceTransform,prepare,java.util.function.UnaryOperator.identity(),1_000_000_000L);
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> complete,long nanoOrigin)throws Exception {
            this(timed,fields,epoch,random,sourceTransform,prepare,complete,nanoOrigin,0);
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> complete,long nanoOrigin,long countdown)throws Exception {
            this(timed,fields,epoch,random,sourceTransform,prepare,complete,nanoOrigin,countdown,2,false);
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> complete,long nanoOrigin,long countdown,
                int humans,boolean settlement)throws Exception {
            this(timed,fields,epoch,random,sourceTransform,prepare,complete,nanoOrigin,countdown,humans,settlement,null,60000);
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> complete,long nanoOrigin,long countdown,
                int humans,boolean settlement,int[] classes,int activeTimeoutMillis)throws Exception {
            this(timed,fields,epoch,random,sourceTransform,prepare,complete,nanoOrigin,countdown,humans,settlement,classes,activeTimeoutMillis,s->0);
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> complete,long nanoOrigin,long countdown,
                int humans,boolean settlement,int[] classes,int activeTimeoutMillis,java.util.function.ToLongFunction<ModernPhysicalCombat.Source> initialShape)throws Exception {
            this(timed,fields,epoch,random,sourceTransform,prepare,complete,nanoOrigin,countdown,
                    humans,settlement,classes,activeTimeoutMillis,initialShape,null,null);
        }
        Fixture(boolean timed,Map<ModernVipEquipment.Field,Long> fields,AtomicLong epoch,ModernPhysicalCombat.BoundedRandom random,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> complete,long nanoOrigin,long countdown,
                int humans,boolean settlement,int[] classes,int activeTimeoutMillis,java.util.function.ToLongFunction<ModernPhysicalCombat.Source> initialShape,
                ModernWorldMap mapOverride,List<ModernWorldMap.Cell> spawnOverride)throws Exception {
            clock.set(nanoOrigin);
            this.epoch=epoch;
            if((mapOverride==null)!=(spawnOverride==null))throw new IllegalArgumentException("map/spawns must be supplied together");
            map=mapOverride!=null?mapOverride:ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            if(humans!=2&&humans!=4)throw new IllegalArgumentException("test roster size");
            if(classes!=null&&classes.length!=humans)throw new IllegalArgumentException("test classes");
            if(spawnOverride!=null){if(spawnOverride.size()!=humans)throw new IllegalArgumentException("test spawn count");map.validateSpawns(spawnOverride);}
            for(int source:List.of(7,42,51,63).subList(0,humans)) {
                if(classes==null){players.add(rooms.player(source,3333,30,0));continue;}
                var owner=rooms.presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),
                        "source-"+source,"player-3333",30,classes[players.size()],1);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,rooms.presence.select(owner,selection));
                rooms.rooms.enterLobby(owner,selection,1,0);players.add(new ModernRoomsTest.Player(owner,new ModernRooms.Scope(source,3333,1,0)));
            }
            ModernRoomsTest.accepted(rooms.create(players.get(0),ModernRoomsTest.draft(100,1,514,0)));
            for(int i=1;i<players.size();i++)ModernRoomsTest.accepted(rooms.join(players.get(i),rooms.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var summary=rooms.view(players.get(0)).joined().summary();var prep=rooms.rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(prep.result());ticket=prep.ticket();var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<ticket.participants().size();i++) {
                var p=ticket.participants().get(i);var m=p.member();var row=new ModernApplicationProtocol.CharacterRow(m.key().objectId(),m.name(),m.characterClass(),m.sex(),m.level());
                var s=sourceTransform.apply(source(m.key().sourceId(),row,fields,timed));sources.add(s);var binding=ModernCombatProfileTest.binding(s.input().equipment());
                var old=ModernPhysicalCombat.qualify(s,binding,Optional.empty(),1000);
                var state=ModernVipCharacterState.start(new ModernVipLifecycle.Actor(UUID.randomUUID(),m.key(),1,0),s,binding,Optional.empty(),1000,initialShape.applyAsLong(s));
                var origin=new ModernWorldMap.SectorKey(0x8000,0x8000).origin();
                var cell=spawnOverride!=null?spawnOverride.get(i):new ModernWorldMap.Cell(origin.x()+2+i,origin.y()+2);
                members.add(complete.apply(prepare.apply(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,cell,200_000_000,old,700_000_000))
                        .withVip(ModernPhysicalCombat.qualifyVip(state,1000))));
            }
            engine=new ModernBattleEngine(ticket,new ModernWorldBinding(1,514,map),members,new ModernBattleEngine.Limits(10000,countdown,activeTimeoutMillis),clock::get,epoch::get,random,settlement);
        }
        ModernBattleObservation.Capture active(){for(var p:players)engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());return view();}
        ModernBattleObservation.Capture view(){return engine.serviceObservation();}
        ModernBattleEngine.EquipmentChange activeReservation(int index){active();return reserve(index);}
        ModernBattleEngine.EquipmentChange reserve(int index){var p=players.get(index);return engine.beginEquipmentChange(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        ModernBattleEngine.AttackResult attack(int index,long sequence,long target){var p=players.get(index);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),sequence,target);}
        @Override public void close(){engine.close();rooms.rooms.releasePreparation(ticket);for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
    }
}
