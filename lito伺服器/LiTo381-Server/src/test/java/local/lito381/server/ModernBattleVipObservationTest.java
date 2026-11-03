package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineVipAdvancedTest.elapsed;
import static local.lito381.server.ModernBattleEngineVipHealingTest.heal;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Engine/durable-store projection tests. No wire, resource or UI admission. */
class ModernBattleVipObservationTest {
    @TempDir Path directory;
    @Test void sameObjectIdInTwoSourcesNeverSharesPrivateEquipmentOrSelection()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(-1,-1,true,0,b->b-1)) {
            var c=f.active();var publication=ModernBattleVipObservation.from(c);
            var a=publication.forOwner(f.players.get(0).scope());var b=publication.forOwner(f.players.get(1).scope());
            assertSame(publication.world(),a.world());assertSame(a.world(),b.world());
            assertEquals(7,a.personal().equipment().source().sourceId());assertEquals(42,b.personal().equipment().source().sourceId());
            assertEquals(3333,a.personal().scope().objectId());assertEquals(3333,b.personal().scope().objectId());
            assertNotEquals(a.personal().scope(),b.personal().scope());assertNotEquals(a.personal().history().identity(),b.personal().history().identity());
            var scope=a.personal().scope();
            for(var foreign:List.of(new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch()+1,scope.channelId()),
                    new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch(),1),new ModernRooms.Scope(51,3333,1,0)))
                assertThrows(SecurityException.class,()->publication.forOwner(foreign));
            assertEquals(c,f.view()); // Projection performs no engine mutation.
        }
    }
    @Test void expiryChangesRuntimeVersionWithoutInventingDurableEquipmentWrites()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(4,5,true,0,b->b-1)) {
            var before=ModernBattleVipObservation.from(f.active());var old=before.forOwner(f.players.get(0).scope());
            assertEquals(new ModernBattleVipObservation.Versions(0,0,0),old.personal().equipment().versions());
            assertEquals(1,old.personal().equipment().vipItems().size());
            assertEquals(List.of(505L),old.personal().equipment().iconIds());assertNull(old.personal().equipment().configuredShape());
            elapsed(f,6);var after=ModernBattleVipObservation.from(f.view()).forOwner(f.players.get(0).scope());
            var versions=after.personal().equipment().versions();assertEquals(0,versions.savedEquipmentRevision());
            assertEquals(2,versions.runtimeAbilityRevision());assertEquals(2,versions.lifecycleSequence());
            assertEquals(2,after.world().actors().get(0).resources().equipmentRevision());
            assertEquals(2,after.personal().history().resources().equipmentRevision());
            assertTrue(after.personal().equipment().vipItems().isEmpty());assertEquals(ModernVipEquipment.Abilities.zero(),after.personal().equipment().vipAbilities());
            assertTrue(after.personal().equipment().iconIds().isEmpty());
            assertEquals(1,old.personal().equipment().vipItems().size());assertEquals(0,old.world().actors().get(0).resources().equipmentRevision());
            assertEquals(before.world(),old.world());
        }
    }
    @Test void postExpiryDurableEquipKeepsSavedOneAndRuntimeTwo()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();f.epoch.set(2000);f.view();
            try(var change=f.reserve(0);var store=new ModernEquipmentOverrideStore(directory)) {
                var q=f.sources.get(0).input().equipment();
                var fresh=ModernVipLoadout.qualifyForRebuild(f.sources.get(0),ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,1,Map.of(3L,true)),2000,0);
                var candidate=ModernVipCharacterState.prepareOrdinaryEquipment(change.vipState(),fresh,2000);change.prepareVip(candidate,2000);
                var commit=ModernBattleEngineVipTest.commit(store,change,candidate);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(commit).disposition());
                var pub=ModernBattleVipObservation.from(f.view());var a=pub.forOwner(f.players.get(0).scope());var b=pub.forOwner(f.players.get(1).scope());
                assertEquals(new ModernBattleVipObservation.Versions(1,2,2),a.personal().equipment().versions());
                assertEquals(new ModernBattleVipObservation.Versions(0,1,1),b.personal().equipment().versions());
                assertEquals(commit.stored().revision(),a.personal().equipment().versions().savedEquipmentRevision());
                assertEquals(140,a.personal().equipment().effective().maximumHp());assertEquals(100,a.world().actors().get(0).resources().vitals().hp());
                assertTrue(a.personal().equipment().equipped().stream().anyMatch(i->i.objectId()==3&&i.equipped()));
            }
        }
    }
    @Test void paidHealingAndCombatReceiptsKeepTheirOriginalVersionAcrossExpiry()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(4,5,true,0,b->b-1)) {
            f.active();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            elapsed(f,2);
            assertEquals(ModernPlayerHealingCast.Reason.NONE,heal(f,0,1,1).reason());
            var old=ModernBattleVipObservation.from(f.view()).forOwner(f.players.get(0).scope());
            elapsed(f,6);var after=ModernBattleVipObservation.from(f.view()).forOwner(f.players.get(0).scope());
            assertEquals(old.world().actors().get(0).playerHealing().receipt(),after.world().actors().get(0).playerHealing().receipt());
            assertEquals(0,after.world().actors().get(0).playerHealing().receipt().impact().use().before().equipmentRevision());
            assertEquals(2,after.personal().equipment().versions().runtimeAbilityRevision());
            assertEquals(old.personal().history().healing().outgoing().last(),after.personal().history().healing().outgoing().last());
            assertEquals(old.personal().ammo().stocks(),after.personal().ammo().stocks());
            assertEquals(old.personal().materials().stocks(),after.personal().materials().stocks());
            assertEquals(ModernPlayerHealingCast.Reason.NONE,heal(f,0,2,1).reason());
            var later=ModernBattleVipObservation.from(f.view()).forOwner(f.players.get(0).scope());
            assertEquals(2,later.world().actors().get(0).playerHealing().receipt().impact().use().before().equipmentRevision());
        }
    }
    @Test void loadingActiveAbortAndFinishedAreProjectedWithoutExtraTicks()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            assertEquals(ModernBattleEngine.Phase.LOADING,ModernBattleVipObservation.from(f.view()).world().phase());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,ModernBattleVipObservation.from(f.active()).world().phase());
            var c=f.engine.abortObservation();var out=ModernBattleVipObservation.from(c);assertEquals(ModernBattleEngine.Phase.ABORTED,out.world().phase());
            assertEquals(c.abortReason(),out.world().abortReason());assertEquals(c.revision(),out.world().revision());
        }
        try(var f=ModernBattleEngineVipAdvancedTest.fixture(4,2,true,1,1000,500,225,b->0)) {
            f.active();var out=ModernBattleEngineVipAdvancedTest.cast(f,0,1,77).observation();assertEquals(ModernBattleEngine.Phase.FINISHED,out.phase());
            var pub=ModernBattleVipObservation.from(out);assertEquals(out.result(),pub.world().result());assertEquals(0,pub.world().actors().get(1).resources().vitals().hp());
        }
    }
    @Test void legacyWorldOwnerAudienceAndWireCannotSilentlyDropVipVersions()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(-1,-1,true,0,b->b-1)) {
            var c=f.active();assertThrows(IllegalArgumentException.class,c::world);
            assertThrows(IllegalArgumentException.class,()->c.forOwner(f.players.get(0).scope()));
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            var w=c.vipWorld();assertTrue(w.vip());assertThrows(IllegalArgumentException.class,()->c.ownerViews(w));
            var owners=c.vipOwnerViews(w);assertThrows(IllegalArgumentException.class,()->ModernBattleAudience.project(w,null,null,null,owners,c));
            var downgraded=new ModernBattleObservation.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.features(),w.clock(),w.actors(),w.modes(),w.npcs(),w.result(),w.hasteEffects(),w.speedEffects());
            assertThrows(IllegalArgumentException.class,()->c.vipOwnerViews(downgraded));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(downgraded,owners.values().iterator().next().owner()));
            assertFalse(ModernBattleAudience.View.class.isAssignableFrom(ModernBattleVipObservation.View.class));
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var c=f.engine.serviceObservation();assertFalse(c.world().vip());
            assertThrows(IllegalArgumentException.class,()->ModernBattleVipObservation.from(c));
        }
    }
    @Test void projectionCollectionsAreFrozenAndDiagnosticStringsDoNotExposePrivateInventory()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(-1,-1,true,0,b->b-1)) {
            var pub=ModernBattleVipObservation.from(f.active());var view=pub.forOwner(f.players.get(0).scope());var p=view.personal();var e=p.equipment();
            assertThrows(UnsupportedOperationException.class,()->view.world().actors().clear());
            assertThrows(UnsupportedOperationException.class,()->e.equipped().clear());assertThrows(UnsupportedOperationException.class,()->e.lifetimes().clear());
            assertThrows(UnsupportedOperationException.class,()->e.vipItems().clear());assertThrows(UnsupportedOperationException.class,()->e.iconIds().clear());
            assertThrows(UnsupportedOperationException.class,()->e.vipAbilities().values().clear());assertThrows(UnsupportedOperationException.class,()->p.skills().clear());
            for(var value:List.of(pub,view,view.world(),p,e))assertTrue(value.toString().endsWith("[redacted]"));
        }
    }
    @Test void publicAndPersonalTypeGraphsContainNoSourceEvaluatorBindingOrPrivateVipContext() {
        var privateTypes=Set.<Class<?>>of(ModernRooms.Scope.class,ModernCharacterData.Snapshot.class,ModernEquipmentHistory.Identity.class,
                ModernBattleVipObservation.Versions.class,ModernBattleVipObservation.Equipment.class);
        var forbidden=Set.<Class<?>>of(ModernPhysicalCombat.Source.class,ModernBattleLoadout.class,ModernVipLifecycle.Actor.class,
                ModernVipLifecycle.State.class,ModernVipCharacterState.class,ModernVipEquipment.Catalog.class,ModernVipLoadout.Applied.class,
                ModernEquipmentReconciliation.ExpectedBinding.class,ModernEquipmentOverrideStore.Fingerprint.class,ModernBattleObservation.CapturedActor.class);
        for(var root:List.of(ModernBattleVipObservation.World.class,ModernBattleVipObservation.Personal.class)) {
            var pending=new ArrayList<java.lang.reflect.Type>();pending.add(root);var seen=new HashSet<Class<?>>();
            while(!pending.isEmpty()) {
                var type=pending.remove(pending.size()-1);
                if(type instanceof java.lang.reflect.ParameterizedType p){pending.add(p.getRawType());pending.addAll(List.of(p.getActualTypeArguments()));}
                else if(type instanceof Class<?> c) {
                    assertFalse(forbidden.contains(c),"source implementation leak: "+c.getName());
                    if(root==ModernBattleVipObservation.World.class)assertFalse(privateTypes.contains(c),"private owner leak: "+c.getName());
                    if(!seen.add(c))continue;
                    if(c.isRecord())for(var field:c.getRecordComponents())pending.add(field.getGenericType());
                    if(c.isSealed())pending.addAll(List.of(c.getPermittedSubclasses()));
                }
            }
        }
    }
    @Test void versionValidationRejectsSavedRuntimeAndLifecycleContradictionsWithoutI32Narrowing() {
        assertThrows(IllegalArgumentException.class,()->new ModernBattleVipObservation.Versions(-1,0,0));
        assertThrows(IllegalArgumentException.class,()->new ModernBattleVipObservation.Versions(2,1,0));
        assertEquals(3,new ModernBattleVipObservation.Versions(0,2,3).lifecycleSequence());
        assertThrows(IllegalArgumentException.class,()->new ModernBattleVipObservation.Versions(0,0,-1));
        assertEquals(5_000_000_000L,new ModernBattleVipObservation.Versions(4_000_000_000L,5_000_000_000L,3_000_000_000L).runtimeAbilityRevision());
    }
    @Test void completeSigned64BitAbilitiesAreRetainedWithoutInventingRewardOrVisualEffects()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,5_000_000_000L,MAX_MP,6_000_000_000L,
                MAGIC_REDUCTION,4_000_000_000L,EXP_RATE,7_000_000_000L,ADENA_RATE,-5_000_000_000L))) {
            var c=f.active();var e=ModernBattleVipObservation.from(c).forOwner(f.players.get(0).scope()).personal().equipment();
            assertEquals(5_000_000_100L,e.effective().maximumHp());assertEquals(6_000_000_050L,e.effective().maximumMp());
            assertEquals(4_000_000_000L,e.vipAbilities().get(MAGIC_REDUCTION));assertEquals(7_000_000_000L,e.vipAbilities().get(EXP_RATE));
            assertEquals(-5_000_000_000L,e.vipAbilities().get(ADENA_RATE));
            assertEquals(c.actors().get(0).loadout().fighter().vip().state().ledger().total(),e.vipAbilities());
            assertNull(e.configuredShape());
        }
    }
    @Test void ownerValidationRejectsLaterHistoryAndAnotherSourceWithSameObjectId()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(4,5,true,0,b->b-1)) {
            var c=f.active();var old=c.vipWorld();var owners=c.vipOwnerViews(old);elapsed(f,6);
            var after=f.view();var w=after.vipWorld();var later=after.vipOwnerViews(w);
            var scope=f.players.get(0).scope();assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(old,later.get(scope).owner()));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(w,owners.get(scope).owner()));
            var own=later.get(scope).owner();var sibling=later.get(f.players.get(1).scope()).owner();
            var wrong=new ModernBattleObservation.PrivateOwner(own.scope(),own.entityId(),sibling.loadout(),sibling.skills(),sibling.history(),
                    sibling.supplies(),sibling.weight(),sibling.ammo(),sibling.regenClock(),sibling.materials());
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(w,wrong));
        }
    }
    @Test void retainedExpiryObservationDoesNotGrantNewMatchAdmission()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            f.active();f.epoch.set(2000);var c=f.view();var a=c.actors().get(0);var p=f.players.get(0);
            var e=ModernBattleVipObservation.from(c).forOwner(p.scope()).personal().equipment();
            assertEquals(new ModernBattleVipObservation.Versions(0,1,1),e.versions());assertTrue(e.vipItems().isEmpty());
            var source=f.sources.get(0);var old=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000);
            var prepared=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),e.source().character(),a.pose().position(),200_000_000,old,700_000_000);
            assertEquals(ModernCombatProfile.VitalPolicy.RETAINED_MATCH,a.loadout().fighter().profile().vitalPolicy());
            assertThrows(IllegalArgumentException.class,()->prepared.withVip(a.loadout().fighter()));
            assertEquals(c,f.view());
        }
    }
    @Test void activeSlowHasteAndMaterialHistoriesRemainBoundToOneCaptureAcrossExpiry()throws Exception {
        for(int skill:List.of(43,54))try(var f=ModernBattleEngineVipHealingTest.fixture(4,5,true,0,b->0)) {
            f.active();assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,0,1,1).reason());
            var slowed=f.view();var old=ModernBattleVipObservation.from(slowed).forOwner(f.players.get(0).scope());
            assertNotNull(old.world().speedEffects());assertEquals(1,old.personal().materials().uses());
            assertEquals("NONE",ModernBattleEngineVipSpeedTest.speed(f,1,1,skill,1).reason());
            var neutralized=ModernBattleVipObservation.from(f.view());
            assertNotEquals(old.world().speedEffects(),neutralized.world().speedEffects());
            elapsed(f,6);var c=f.view();var after=ModernBattleVipObservation.from(c).forOwner(f.players.get(0).scope());
            assertEquals(old.personal().materials().stocks(),after.personal().materials().stocks());
            assertEquals(old.world().actors().get(0).spells().receipts().get(ModernUseHistory.Lane.SLOW),
                    after.world().actors().get(0).spells().receipts().get(ModernUseHistory.Lane.SLOW));
            assertEquals(2,after.personal().equipment().versions().runtimeAbilityRevision());
            assertEquals(c.revision(),after.personal().history().worldRevision());
        }
    }
}
