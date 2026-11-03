package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattlePublicationTest.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class ModernBattleAudienceTest {
    @TempDir Path temporary;
    static ModernBattlePublication published(ModernWorldService s,TournamentPresence.Owner p)throws Exception {return cache(s,entry(s,p)).authority();}
    static Set<Class<?>> graph(Class<?> root,Set<Class<?>> forbidden) {
        var seen=new HashSet<Class<?>>();var pending=new ArrayList<Type>();pending.add(root);
        while(!pending.isEmpty()) {
            var t=pending.remove(pending.size()-1);
            if(t instanceof java.lang.reflect.ParameterizedType p){pending.add(p.getRawType());pending.addAll(List.of(p.getActualTypeArguments()));}
            else if(t instanceof Class<?> c) {
                assertFalse(forbidden.contains(c),"forbidden reachable type: "+c.getSimpleName());
                if(!seen.add(c)||c.isEnum())continue;
                if(c.getPackageName().equals("local.lito381.server"))for(var f:c.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.isSynthetic()) {
                    assertFalse(f.getName().toLowerCase().contains("database"));assertFalse(f.getName().equals("loadingOriginNanos"));pending.add(f.getGenericType());
                }
                if(c.isSealed())pending.addAll(List.of(c.getPermittedSubclasses()));
            }
        }
        return seen;
    }
    @Test void completeOutboundGraphHasNoServerCatalogPlansMapOrRawRuntimeAndPublicHasNoPrivateScope() {
        var forbidden=Set.of(ModernNpcRoster.Template.class,ModernQuestProgress.Plan.class,ModernTimeAttack.Plan.class,
                ModernPetRace.Course.class,ModernWorldMap.class,ModernBattleLoadout.class,ModernCombatProfile.class,ModernPhysicalCombat.Fighter.class,
                ModernSpellRuntime.State.class,ModernSpellRuntime.Receipt.class,ModernBattleObservation.Capture.class,ModernBattleObservation.PrivateOwner.class);
        var all=graph(ModernBattleAudience.View.class,forbidden);assertTrue(all.contains(ModernCharacterData.Snapshot.class));
        assertTrue(all.contains(ModernTargetHistory.Effect.class));assertTrue(all.contains(ModernBattleAudience.TimeAttack.class));
        var publicForbidden=new HashSet<>(forbidden);publicForbidden.addAll(Set.of(ModernRooms.Scope.class,ModernCharacterData.Snapshot.class,
                ModernEquipmentHistory.State.class,ModernTargetHistory.Batch.class,ModernBattleAudience.Personal.class));
        graph(ModernBattleAudience.World.class,publicForbidden);
        var motion=graph(ModernNpcMotion.View.class,publicForbidden);
        assertTrue(motion.contains(ModernNpcMotion.Pace.class));assertTrue(motion.contains(ModernNpcMotion.Walk.class));
        assertTrue(all.contains(ModernNpcMotion.View.class),"schema4 now encodes the explicitly versioned motion extension");
        graph(ModernNpcMotion.State.class,publicForbidden);
    }
    @Test void oneSharedPublicWorldRetainsDistinctExactPrivateSourcesAndRelativeSpellClock()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.cast(0,1,1,3);var c=f.engine.serviceObservation();var p=new ModernBattlePublication(c,honor(c),null,null);
            var a=p.audience(f.players.get(0).scope());var b=p.audience(f.players.get(1).scope());
            assertSame(a.world(),b.world());assertEquals(-1,a.world().lane());assertEquals(2,a.world().actors().size());
            assertNotEquals(a.personal().scope(),b.personal().scope());assertNotEquals(a.personal().equipment().source(),b.personal().equipment().source());
            assertEquals(p.owner(a.personal().scope()).owner().history().history(),a.personal().history());
            assertEquals(0,a.world().actors().get(0).spells().receipts().get(ModernUseHistory.Lane.HEALING).loadingElapsedNanos());
            assertThrows(SecurityException.class,()->p.audience(new ModernRooms.Scope(a.personal().scope().sourceId(),a.personal().scope().objectId(),2,0)));
            f.elapsed(2_000_000_000L);f.cast(0,2,1,3);assertSame(a,p.audience(a.personal().scope()));
            assertEquals(1,a.personal().history().spells().get(ModernUseHistory.Lane.HEALING).current().casts());
        }
    }
    @Test void actualServiceBuildsSparseLaneViewsWithGlobalIdsAndFullRoomMembership()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true,true,new ModernQuestServiceTest.Journal(),513)) {
            var frame=f.active();stopScheduler(f.service);var p=published(f.service,f.a.owner());var a=p.audience(f.a.scope());var b=p.audience(f.b.scope());
            assertEquals(0,a.world().lane());assertEquals(1,b.world().lane());assertNotSame(a.world(),b.world());
            for(var v:List.of(a,b)) {
                int lane=v.world().lane();assertEquals(2,v.world().room().members().size());assertEquals(1,v.world().actors().size());
                assertEquals(lane+1,v.world().actors().get(0).pose().entityId());assertEquals(lane,v.world().actors().get(0).pose().member().team());
                assertEquals(1,v.world().modes().quest().lanes().size());assertEquals(4,v.world().modes().quest().targets().size());
                assertTrue(v.world().modes().quest().targets().stream().allMatch(t->t.lane()==lane));
                assertTrue(v.world().npcs().actors().stream().allMatch(n->n.lane()==lane));
                var motion=ModernNpcMotion.project(p.world().npcs().roster(),lane);
                assertEquals(v.world().npcs().actors().stream().map(ModernBattleAudience.Npc::serial).toList(),
                        motion.stream().map(m->m.actor().serial()).toList());
                assertThrows(IllegalArgumentException.class,()->ModernNpcMotion.project(p.world().npcs().roster(),-1));
                assertEquals(v.personal().scope().sourceId(),v.personal().equipment().source().sourceId());assertEquals(3333,v.personal().scope().objectId());
            }
            assertEquals(a.world().actors().get(0).pose().position(),b.world().actors().get(0).pose().position());
            var legacy=f.read(f.b,frame.combat().world()).frame();var latest=published(f.service,f.b.owner()).audience(f.b.scope());
            assertEquals(legacy.actors().stream().map(ModernNpcProtocol.Actor::serial).toList(),latest.world().npcs().actors().stream().map(ModernBattleAudience.Npc::serial).toList());
            assertEquals(legacy.names(),latest.world().npcs().names());assertEquals(legacy.resources(),latest.world().npcs().resources());
        }
    }
    @Test void allQuestTerminalFlowsKeepOpposingWinnerPublicButNotOpposingObjectivesOrReceipts()throws Exception {
        for(int map:List.of(554,513,527))try(var f=new ModernNpcServiceTest.Fixture(100,true,false,new ModernQuestServiceTest.Journal(),map)) {
            var strike=ModernQuestServiceTest.clearToLast(f);f.service.npc(f.a.owner(),strike);stopScheduler(f.service);var p=published(f.service,f.a.owner());
            var win=p.audience(f.a.scope());var loss=p.audience(f.b.scope());assertSame(win.world().result(),loss.world().result());
            assertEquals(ModernBattleEngine.Phase.FINISHED,loss.world().phase());assertEquals(0,loss.world().modes().quest().winningLane());
            assertEquals(win.world().revision(),loss.world().modes().quest().deathRevision());assertTrue(loss.world().modes().quest().objectives().isEmpty());
            assertTrue(loss.world().modes().quest().exchanges().isEmpty());assertEquals(2,loss.world().result().participants().size());
            assertTrue(win.world().modes().quest().exchanges().stream().allMatch(e->e.participant().sourceId()==7));
        }
    }
    @Test void actualServiceNpcProjectionHasOnlySelectedPresentationMetadata()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture()) {
            f.active();stopScheduler(f.service);var p=published(f.service,f.a.owner());var view=p.audience(f.a.scope());
            assertEquals(p.world().npcs().roster().actors().size(),view.world().npcs().actors().size());
            var raw=p.world().npcs().roster().actors().get(0);var safe=view.world().npcs().actors().get(0);
            assertEquals(raw.id().serial(),safe.serial());assertEquals(raw.template().npcId(),safe.npcId());assertEquals(raw.hp(),safe.hp());
            assertEquals(raw.nextAttackMillis(),safe.nextAttackMillis());assertEquals(p.world().npcs().exchanges(),view.world().npcs().exchanges());
            var motion=ModernNpcMotion.project(p.world().npcs().roster(),view.world().lane());
            assertEquals(view.world().npcs().actors().stream().map(ModernBattleAudience.Npc::serial).toList(),
                    motion.stream().map(m->m.actor().serial()).toList());
            assertEquals(ModernNpcMotion.Pace.from(raw.template()),motion.get(0).pace());assertEquals(raw.walk(),motion.get(0).walk());
        }
    }
    @Test void timeAttackRemovesAllFutureSpawnTemplatesButRetainsFullResultProof()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new ModernTimeAttackServiceTest.Fixture(temporary,1,journal)) {
            var w=f.active();stopScheduler(f.service);var player=f.players.get(0);f.tower(player,w);for(int n=0;n<3;n++)f.hit(player,w,n+2);
            var p=published(f.service,player.owner());var v=p.audience(player.scope());var raw=p.world().modes().timeAttack().state();var time=v.world().modes().timeAttack();
            assertEquals(journal.event,v.world().result());assertEquals(raw.plan().waves().stream().map(ModernTimeAttack.Wave::kind).toList(),time.waves());
            assertEquals(ModernTimeAttackSettlement.planHash(raw.plan()),time.planSha256());assertEquals(raw.towerDeathRevision(),time.towerDeathRevision());
            assertEquals(raw.revision(),time.revision());assertEquals(raw.living().stream().map(ModernNpcRoster.Id::serial).sorted().toList(),time.living());
        }
    }
    @Test void siegeKeepsAllTypedParticipantsAndResultWithoutPrivateBotProfiles()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new ModernSiegeServiceTest.Fixture(0,500,journal)) {
            f.active();stopScheduler(f.service);ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.FINISHED);
            var p=published(f.service,f.players.get(0).owner());var v=p.audience(f.players.get(0).scope());assertEquals(30,v.world().actors().size());
            assertEquals(journal.event,v.world().result());assertEquals(p.world().modes().siege(),v.world().modes().siege());
            assertEquals(f.players.get(0).scope(),v.personal().scope());assertEquals(-1,v.world().lane());
        }
    }
    @Test void raceKeepsEngineRevisionAndCourseDigestWithoutPhysicalMapOrFakeInventory()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture(30_000,1_000_000_000L)) {
            f.active();var first=f.finish(0);f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var c=f.engine.serviceObservation();var l=c.limits();
            var d=new ModernMatchDescriptor(Ruleset.PET_RACE,PET_RACE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
            var v=new ModernBattlePublication(c,d,null,null).audience(f.a.scope());var race=v.world().modes().race();
            assertEquals(c.modes().race().terminalRevision(),race.terminalRevision());assertEquals(c.modes().raceResultId(),race.resultId());
            assertEquals(c.modes().race().course().digest(),race.courseSha256());assertEquals(c.modes().race().runners(),race.runners());
            assertNull(v.personal().equipment());assertNull(v.personal().history());assertNull(v.personal().supplies());assertNull(v.world().npcs());
        }
    }
    @Test void forgedCrossLaneMembershipAndMixedOwnersCannotConstructAnAudience()throws Exception {
        try(var f=new ModernNpcServiceTest.Fixture(100,true)) {
            f.active();stopScheduler(f.service);var p=published(f.service,f.a.owner());var v=p.audience(f.a.scope());var w=v.world();
            assertThrows(IllegalArgumentException.class,()->new ModernBattleAudience.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),
                    w.descriptor(),w.clock(),w.lane(),p.world().actors(),w.modes(),w.npcs(),w.result()));
            assertThrows(IllegalArgumentException.class,()->ModernBattleAudience.project(p.world(),p.descriptor(),p.resources(),p.names(),Map.of(f.a.scope(),p.owner(f.b.scope())),p.capture()));
            assertThrows(UnsupportedOperationException.class,()->v.world().actors().clear());assertThrows(UnsupportedOperationException.class,()->v.personal().equipment().equipped().clear());
        }
    }
}
