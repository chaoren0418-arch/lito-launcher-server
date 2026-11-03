package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Real room/engine transitions, not invented wire frames or desktop acceptance. */
class ModernBattleObservationTest {
    static ModernBattleObservation.OwnerView read(ModernBattleEngine e,ModernRooms.FrozenPreparation t,ModernBattleEngineBotTest.Player p) {
        return e.observation(p.owner(),p.scope(),t.matchId(),t.generation());
    }
    static ModernBattleObservation.Capture withActor(ModernBattleObservation.Capture c,int index,ModernBattleObservation.CapturedActor a,long revision) {
        var actors=new ArrayList<>(c.actors());actors.set(index,a);
        return new ModernBattleObservation.Capture(c.identity(),revision,c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),c.limits(),
                c.loadingOriginNanos(),actors,c.modes(),c.npcs(),c.result());
    }
    static void parity(ModernBattleEngine e) {
        var capture=e.serviceObservation();assertEquals(e.tick(),ModernLegacyBattleProjection.from(capture));
        assertEquals(capture.revision(),capture.world().revision());
    }
    @Test void loadingAndActiveOwnersSharePublicFactsButNotPrivateSourceOrEpoch()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var p=f.players.get(0);var q=f.players.get(1);var loading=read(f.engine,f.ticket,p);
            assertEquals(-1,loading.world().clock().activeElapsedNanos());assertEquals(ModernBattleEngine.Phase.LOADING,loading.world().phase());
            f.active();var left=read(f.engine,f.ticket,p);var right=read(f.engine,f.ticket,q);
            assertEquals(left.world(),right.world());assertEquals(0,left.world().clock().activeElapsedNanos());
            assertEquals(p.scope(),left.owner().scope());assertEquals(q.scope(),right.owner().scope());
            assertNotEquals(left.owner().loadout().fighter().profile().source(),right.owner().loadout().fighter().profile().source());
            assertEquals(left.world().revision(),left.owner().history().history().worldRevision());
            assertEquals(left.owner().supplies().history(),left.owner().history().history().items());parity(f.engine);
            assertThrows(SecurityException.class,()->f.engine.observation(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.observation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1));
            var foreign=new ModernRooms.Scope(p.scope().sourceId(),p.scope().objectId(),p.scope().epoch()+1,p.scope().channelId());
            assertThrows(SecurityException.class,()->f.engine.serviceObservation().forOwner(foreign));
            assertEquals(left,read(f.engine,f.ticket,p));
        }
    }
    @Test void publicRecordGraphContainsNoPrivateOwnerScopeLoadoutOrRuntime() {
        var forbidden=java.util.Set.of(ModernRooms.Scope.class,ModernBattleLoadout.class,ModernCharacterData.Snapshot.class,
                ModernEquipmentHistory.Identity.class,ModernBattleObservation.CapturedActor.class,ModernSpellRuntime.State.class);
        var seen=new HashSet<Class<?>>();var pending=new ArrayList<java.lang.reflect.Type>();pending.add(ModernBattleObservation.World.class);
        while(!pending.isEmpty()) {
            var type=pending.remove(pending.size()-1);
            if(type instanceof java.lang.reflect.ParameterizedType p){pending.add(p.getRawType());pending.addAll(List.of(p.getActualTypeArguments()));}
            else if(type instanceof Class<?> c) {
                assertFalse(forbidden.contains(c),"private type in public graph: "+c.getSimpleName());
                if(!seen.add(c))continue;
                if(c.isRecord())for(var field:c.getRecordComponents())pending.add(field.getGenericType());
                if(c.isSealed())pending.addAll(List.of(c.getPermittedSubclasses()));
            }
        }
        assertTrue(seen.contains(ModernTargetHistory.Effect.class));
    }
    @Test void immutableCaptureRetainsExactCriticalSpellReceiptAndRejectsLaterOwnerData()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1000,1000,500,true)) {
            f.active();var p=f.players.get(0);var before=f.engine.serviceObservation();var old=before.forOwner(p.scope());
            assertEquals(ModernDisintegrateState.Reason.NONE,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());var after=read(f.engine,f.ticket,p);
            assertEquals(old,before.forOwner(p.scope()));assertNotEquals(old.world(),after.world());
            var visible=after.world().actors().get(0).spells().receipts().get(ModernUseHistory.Lane.DISINTEGRATE);
            var privateReceipt=after.owner().history().spells().receipts().get(ModernUseHistory.Lane.DISINTEGRATE);
            assertEquals(ModernBattleObservation.Impact.from(privateReceipt.batch()),visible.impact());assertEquals(privateReceipt.detail(),visible.detail());
            assertEquals(0,visible.loadingElapsedNanos());assertInstanceOf(ModernSpellRuntime.Disintegrate.class,visible.detail());
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(old.world(),after.owner()));
            assertThrows(UnsupportedOperationException.class,()->before.actors().clear());
            assertThrows(UnsupportedOperationException.class,()->after.world().actors().get(0).spells().receipts().clear());parity(f.engine);
        }
    }
    @Test void relativeClocksSurviveSignedNanoTimeWrapAndRetainCastAge()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1000,500,Long.MAX_VALUE-1_000_000_000L)) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());f.elapsed(2_000_000_000L);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,1,3).reason());var v=read(f.engine,f.ticket,f.players.get(0));
            assertEquals(2_000_000_000L,v.world().clock().loadingElapsedNanos());assertEquals(2_000_000_000L,v.world().clock().activeElapsedNanos());
            assertEquals(2_000_000_000L,v.world().actors().get(0).spells().receipts().get(ModernUseHistory.Lane.HEALING).loadingElapsedNanos());parity(f.engine);
        }
    }
    @Test void potionAndNaturalRegenUseTheSameResourceAndPrivateClockObservation()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();f.cast(1,1,4);f.heal(0);f.elapsed(16_000_000_000L);var v=read(f.engine,f.ticket,f.room.players.get(0));
            var a=v.world().actors().get(0);assertNotNull(v.owner().regenClock());
            assertEquals(v.owner().supplies().history().restored(),a.resources().flows().restored());
            assertEquals(a.resources().flows().hpGain(),a.regeneration().hp().gain());
            assertEquals(v.owner().loadout().reweight(v.owner().supplies().stocks(),List.of(),v.owner().weight().units()),v.owner().weight());parity(f.engine);
        }
    }
    @Test void arrowWithoutSpellCapabilityStillBindsStockWeightAndAttackToOneOwner()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var before=read(f.engine,f.ticket,f.room.players.get(0));assertNull(before.owner().skills());
            assertTrue(before.owner().history().history().spells().isEmpty());assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1).status());
            var after=read(f.engine,f.ticket,f.room.players.get(0));assertEquals(1,after.owner().ammo().shotCount());
            assertEquals(after.world().revision(),after.owner().ammo().shotRevision());
            assertEquals(after.world().revision(),after.world().actors().get(0).vital().attackRevision());
            assertEquals(0,after.owner().supplies().history().uses());assertEquals(before.owner().ammo().selected().remaining()-1,after.owner().ammo().selected().remaining());
            assertEquals(after.owner().loadout().reweight(after.owner().supplies().stocks(),after.owner().ammo().stocks(),after.owner().weight().units()),after.owner().weight());parity(f.engine);
        }
    }
    @Test void noSkillNoSupplyMeleeAndLoadingAbortStillHaveValidOwnerObservations()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,100,30,1_000_000_000L,0,false,false,p->
                new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos()))) {
            var p=f.players.get(0);var initial=read(f.engine,f.ticket,p);assertNotNull(initial.owner().history());
            assertNull(initial.owner().skills());assertNull(initial.owner().supplies());assertNull(initial.owner().weight());
            f.elapsed(2_000_000_000L);var expired=read(f.engine,f.ticket,p);
            assertEquals(ModernBattleEngine.Phase.ABORTED,expired.world().phase());assertEquals(-1,expired.world().clock().activeElapsedNanos());
            assertEquals(ModernBattleEngine.AbortReason.LOAD_TIMEOUT,expired.world().abortReason());parity(f.engine);
        }
    }
    @Test void sameNumericBotAndHumanRemainTypedAndSettlementIsStable()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture(1,100,100,true,0)) {
            f.active();for(int n=1;n<=20&&f.engine.tick().phase()==ModernBattleEngine.Phase.ACTIVE;n++){f.attack(n);f.clock.addAndGet(1_000_000_000L);}
            var c=f.engine.serviceObservation();var v=c.forOwner(f.room.players.get(0).scope());assertNotNull(v.world().result());
            assertTrue(v.world().actors().get(0).pose().member().identity().isHuman());assertFalse(v.world().actors().get(1).pose().member().identity().isHuman());
            assertEquals(v.world().actors().get(0).pose().member().identity().id(),v.world().actors().get(1).pose().member().identity().id());
            assertEquals(v.world().result(),f.engine.serviceObservation().world().result());assertNull(c.actors().get(1).scope());parity(f.engine);
        }
    }
    @Test void ownerEnvelopeRejectsForeignSkillsAndAlteredVisibleCriticalMetadata()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1000,1000,500,true)) {
            f.active();assertEquals(ModernDisintegrateState.Reason.NONE,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());
            var c=f.engine.serviceObservation();var v=c.forOwner(f.players.get(0).scope());var o=v.owner();
            var foreign=c.forOwner(f.players.get(1).scope()).owner();
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(v.world(),new ModernBattleObservation.PrivateOwner(
                    o.scope(),o.entityId(),o.loadout(),foreign.skills(),o.history(),o.supplies(),o.weight(),o.ammo(),o.regenClock())));
            var a=v.world().actors().get(0);var s=a.spells();var receipts=new java.util.EnumMap<ModernUseHistory.Lane,ModernBattleObservation.SpellReceipt>(ModernUseHistory.Lane.class);
            receipts.putAll(s.receipts());var lane=ModernUseHistory.Lane.DISINTEGRATE;
            var old=receipts.get(lane);var d=(ModernSpellRuntime.Disintegrate)old.detail();
            receipts.put(lane,new ModernBattleObservation.SpellReceipt(old.impact(),old.casts(),old.spent(),old.loadingElapsedNanos(),
                    new ModernSpellRuntime.Disintegrate(!d.critical(),d.lawfulBefore(),d.lawfulAfter(),d.damageLossAfter())));
            var spells=new ModernBattleObservation.Spells(s.sequence(),s.requestRevision(),s.initialLawful(),s.lawful(),s.histories(),receipts,s.structure());
            var actors=new ArrayList<>(v.world().actors());actors.set(0,new ModernBattleObservation.Actor(a.pose(),a.vital(),a.resources(),spells,a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation()));
            var w=v.world();var forged=new ModernBattleObservation.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.features(),w.clock(),actors,w.modes(),w.npcs(),w.result());
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(forged,o));
        }
    }
    @Test void respawnAdvancesLifeWithoutRewritingHistoricalCastOrPublicResult()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();f.cast(1,1,1,4);f.cast(0,1,4,2);var dead=read(f.engine,f.ticket,f.players.get(1));
            f.elapsed(5_000_000_000L);var alive=read(f.engine,f.ticket,f.players.get(1));
            assertEquals(0,dead.world().actors().get(1).vital().values().hp());assertEquals(1,alive.world().actors().get(1).resources().life());
            assertEquals(dead.world().actors().get(1).spells(),alive.world().actors().get(1).spells());parity(f.engine);
        }
    }
    @Test void npcAndRetainedQuestStructureComeFromOneCaptureAcrossNewWave()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);
            var before=f.engine.serviceObservation();var owner=before.forOwner(f.players.get(0).scope());assertNotNull(owner.world().actors().get(0).spells().structure());
            f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);var after=f.engine.serviceObservation();
            assertEquals(owner.world().actors().get(0).spells().structure(),after.world().actors().get(0).spells().structure());
            assertEquals(owner,before.forOwner(f.players.get(0).scope()));assertEquals(f.view(),ModernLegacyBattleProjection.npcs(after));
            assertEquals(after.identity().matchId(),after.world().npcs().roster().scope().matchId());
        }
    }
    @Test void everyQuestLogicalRulePublishesTerminalObjectivesNpcAndOwnerTogether()throws Exception {
        for(int logical:BattleRules.Mode.QUEST_BATTLE.maps())try(var f=ModernNpcFireStormStructuresTest.quest(logical,1)) {
            f.active();long sequence=0,elapsed=0;boolean symbols=logical==513||logical==527;
            for(int stage=1;stage<=4;stage++) {
                f.elapsed(elapsed);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,++sequence).reason());
                f.view();
                if(symbols||stage==4){f.elapsed(elapsed+=2000);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,++sequence).reason());}
                elapsed+=2000;
            }
            var c=f.engine.serviceObservation();var v=c.forOwner(f.players.get(0).scope());assertEquals(ModernBattleEngine.Phase.FINISHED,v.world().phase());
            assertEquals(logical,v.world().result().mapId());assertEquals(v.world().revision(),v.world().result().engineRevision());
            assertNotNull(v.world().actors().get(0).spells().structure());assertEquals(f.view(),ModernLegacyBattleProjection.npcs(c));
        }
    }
    @Test void timeAttackCanonicalSettlementMatchesLegacyProofAndCachesOneIdentity()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);int waves=f.view().world().timeAttack().state().plan().waves().size();
            for(int wave=0;wave<waves;wave++){f.elapsed((wave+1)*2000L);ModernNpcFireStormStructuresTest.cast(f,0,wave+2);}
            var c=f.engine.serviceObservation();var v=c.forOwner(f.players.get(0).scope());assertEquals(ModernBattleEngine.Phase.FINISHED,v.world().phase());
            var event=v.world().modes().timeAttackResult();assertNotNull(event);
            assertEquals(event,ModernTimeAttackSettlement.event(ModernLegacyBattleProjection.from(c),event.resultId(),event.finishedAtMillis()));
            assertEquals(event,ModernTimeAttackSettlement.event(v.world(),event.resultId(),event.finishedAtMillis()));
            assertEquals(event,f.engine.serviceObservation().world().modes().timeAttackResult());parity(f.engine);
        }
    }
    @Test void siegeTimeoutCanonicalSettlementIncludesAllTypedBotsAndSameProof()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();f.waitMillis(60_000);var c=f.engine.serviceObservation();var v=c.forOwner(f.players.get(0).scope());
            var event=v.world().modes().siegeResult();assertNotNull(event);assertEquals(30,event.participants().size());
            assertEquals(28,event.participants().stream().filter(p->!p.identity().isHuman()).count());
            assertEquals(event,ModernSiegeSettlement.event(ModernLegacyBattleProjection.from(c),event.resultId(),event.finishedAtMillis()));
            assertEquals(event,ModernSiegeSettlement.event(v.world(),event.resultId(),event.finishedAtMillis()));
            assertEquals(event,f.engine.serviceObservation().world().modes().siegeResult());parity(f.engine);
        }
    }
    @Test void raceHasNoInventedCombatInventoryAndKeepsEngineRevisionUntilLegacyOutput()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture(30_000,1_000_000_000L)) {
            var start=f.engine.observation(f.a.owner(),f.a.scope(),f.ticket.matchId(),f.ticket.generation());
            assertNull(start.owner().history());assertNull(start.owner().supplies());assertFalse(start.world().features().combat());
            try(var donor=new ModernBattleEngineHonorTest.Fixture()) {
                var foreign=read(donor.engine,donor.ticket,donor.players.get(0)).owner();var o=start.owner();
                assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(start.world(),new ModernBattleObservation.PrivateOwner(
                        o.scope(),o.entityId(),foreign.loadout(),o.skills(),o.history(),o.supplies(),o.weight(),o.ammo(),o.regenClock())));
            }
            f.active();var first=f.finish(0);f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var c=f.engine.serviceObservation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,c.world().phase());assertNotNull(c.world().modes().raceResultId());
            assertEquals(c.world().modes().race().revision()+1,ModernLegacyBattleProjection.from(c).race().terminalRevision());parity(f.engine);
        }
    }
    @Test void qualifiedCapacityAndPriceChangeCanBeObservedWithoutLegacyFrameOrLiveSet()throws Exception {
        var sources=new java.util.HashMap<Integer,ModernPhysicalCombat.Source>();
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true,p->{
            var s=ModernBattleEquipmentHistoryTest.source(p);sources.put(p.scope().sourceId(),s);var fighter=ModernPhysicalCombatTest.fighter(s);
            return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),true,p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true,false,true,true);
        })) {
            f.active();f.cast(0,1,1,3);f.elapsed(2_000_000_000L);f.cast(0,2,70,0);
            var c=f.engine.serviceObservation();var p=f.players.get(0);var before=c.forOwner(p.scope());var old=c.actors().get(0);var h=before.owner().history().history();
            var source=sources.get(7);var q=source.input().equipment();
            var next=ModernPhysicalCombat.qualifyEquipmentChange(source,ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,1,Map.of(1L,false)),
                    1000,old.loadout().fighter(),h.resources().vitals().hp(),h.resources().vitals().mp());
            var loadout=old.loadout().replacement(next);var revised=h.reprofile(c.revision()+1,loadout.historyProfile(h.spells().keySet(),true));
            var oldVital=old.vital();var combat=loadout.combatant();
            var vital=new ModernBattleObservation.Vital(revised.resources().vitals(),combat.armorClass(),combat.weaponType(),oldVital.attackSequence(),oldVital.attackRevision(),
                    oldVital.hurtRevision(),oldVital.deathRevision(),oldVital.target(),oldVital.attackIntervalNanos());
            var supplies=new ModernBattleObservation.Supplies(old.supplies().requestSequence(),old.supplies().requestRevision(),revised.items(),old.supplies().stocks(),old.supplies().effect(),old.supplies().delayMillis());
            var candidate=new ModernBattleObservation.CapturedActor(old.pose(),old.scope(),loadout,old.skills(),vital,revised.resources(),old.castIntervalNanos(),
                    revised.spells(),revised.targets(),old.runtime(),supplies,loadout.reweight(supplies.stocks(),List.of(),old.weight().units()),old.buffs(),old.ammo(),old.regeneration(),old.regenClock(),old.presentation());
            var changed=withActor(c,0,candidate,revised.worldRevision());var observed=changed.forOwner(p.scope());
            assertEquals(100,observed.world().actors().get(0).vital().values().maximumHp());assertEquals(50,observed.world().actors().get(0).vital().values().maximumMp());
            assertEquals(before.world().actors().get(0).spells().receipts(),observed.world().actors().get(0).spells().receipts());
            assertNotEquals(h.profile(),observed.owner().history().history().profile());
            var d=ModernBattlePublicationTest.honor(changed);
            d=new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.FIRE_STORM,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            var audience=new ModernBattlePublication(changed,d,null,null).audience(p.scope());
            assertEquals(observed.owner().history().history(),audience.personal().history());
            assertEquals(loadout.fighter().profile().equipped(),audience.personal().equipment().equipped());
            assertEquals(50,audience.world().actors().get(0).vital().values().maximumMp());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(changed));
            assertEquals(before,read(f.engine,f.ticket,p));assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
}
