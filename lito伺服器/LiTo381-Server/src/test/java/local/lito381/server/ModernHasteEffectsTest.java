package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import static local.lito381.server.ModernBattleEngineTimedTest.SECOND;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Real engine -> checked publication -> exact owner audience. Not a wire,
 * SQL admission, shared-map qualification or UI acceptance claim. */
class ModernHasteEffectsTest {
    @Test void latestSuccessfulPlayerCastCannotLoseItsResultInTheSameRevision()throws Exception {
        try(var f=fixture()) {
            active(f);var w=publish(cast(f,0,1,1).observation()).world();var s=w.hasteEffects();
            var rows=new ArrayList<>(w.actors());var a=rows.get(1);var b=a.buffs();
            var cleared=new ModernTimedEffects.State(b.entityId(),b.revision(),b.observedNanos(),0,b.braveUntilNanos(),b.wisdomUntilNanos(),b.braveKind());
            rows.set(1,new ModernBattleObservation.Actor(a.pose(),a.vital(),a.resources(),a.spells(),a.castIntervalNanos(),cleared,a.regeneration(),a.presentation()));
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(s.world(),s.revision(),List.of(),null),rows));
        }
    }
    @Test void latestSuccessfulNpcCastCannotLoseItsResultInTheSameRevision()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(280)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);ModernBattleNpcHasteTest.cast(f,0,1,n);
            var w=publish(f).audience(f.players.get(0).scope()).world();var s=w.hasteEffects();var roster=s.npcs();var m=roster.actors().get(0);
            var cleared=new ModernNpcSpeed.State(m.speed().revision(),m.speed().observedMillis(),0);
            var motion=new ModernHasteEffects.Motion(m.actor(),m.life(),m.base(),cleared,m.walkSpeed(),m.walk());
            var state=new ModernHasteEffects.State(s.world(),s.revision(),List.of(),new ModernHasteEffects.Npcs(roster.scope(),roster.revision(),roster.elapsedMillis(),List.of(motion)));
            assertThrows(IllegalArgumentException.class,()->with(w,state));
        }
    }
    static ModernBattlePublication publish(ModernBattleObservation.Capture c){return ModernBattleWireTest.publication(c,null);}
    static ModernBattlePublication publish(ModernBattleEngineNpcTest.Fixture f){return ModernBattleWireTest.publication(f.engine.serviceObservation(),f.config.catalog());}
    static ModernBattleObservation.World with(ModernBattleObservation.World w,ModernHasteEffects.State state) {
        return with(w,state,w.actors());
    }
    static ModernBattleObservation.World with(ModernBattleObservation.World w,ModernHasteEffects.State state,List<ModernBattleObservation.Actor> actors) {
        return new ModernBattleObservation.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.features(),w.clock(),actors,w.modes(),w.npcs(),w.result(),state);
    }
    static ModernBattleAudience.World with(ModernBattleAudience.World w,ModernHasteEffects.State state) {
        return new ModernBattleAudience.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.descriptor(),w.clock(),w.lane(),w.actors(),w.modes(),w.npcs(),w.result(),state);
    }
    static ModernBattleObservation.World receipt(ModernBattleObservation.World w,int who,ModernBattleObservation.SpellReceipt r) {
        var rows=new ArrayList<>(w.actors());var a=rows.get(who);var s=a.spells();var map=new java.util.EnumMap<>(s.receipts());map.put(LANE,r);
        var changed=new ModernBattleObservation.Spells(s.sequence(),s.requestRevision(),s.initialLawful(),s.lawful(),s.histories(),map,s.structure());
        rows.set(who,new ModernBattleObservation.Actor(a.pose(),a.vital(),a.resources(),changed,a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation()));
        return with(w,w.hasteEffects(),rows);
    }
    @Test void twoVictimsRemainPublicAfterCasterMovesOnAndOwnerPrivateScopesStaySeparate()throws Exception {
        try(var f=fixture()) {
            active(f);var first=publish(cast(f,0,1,1).observation());f.elapsed(2*SECOND);var p=publish(cast(f,0,2,0).observation());
            var a=p.audience(f.players.get(0).scope());var b=p.audience(f.players.get(1).scope());
            assertSame(a.world(),b.world());assertNotEquals(a.personal().scope(),b.personal().scope());
            assertEquals(2,a.world().hasteEffects().targets().size());var old=first.world().hasteEffects().targets().get(0).active();
            assertEquals(old,a.world().hasteEffects().targets().get(1).active());assertEquals(40,old.receipt().spent());
            assertEquals(80,a.world().actors().get(0).spells().receipts().get(LANE).spent());
            assertEquals(0,old.receipt().loadingElapsedNanos());assertEquals(2*SECOND,a.world().hasteEffects().targets().get(0).active().receipt().loadingElapsedNanos());
            assertThrows(UnsupportedOperationException.class,()->a.world().hasteEffects().targets().clear());
            assertEquals(1,first.world().hasteEffects().targets().size());
            assertThrows(SecurityException.class,()->p.audience(new ModernRooms.Scope(7,3333,999,0)));
        }
    }
    @Test void checkedPublicationDoesNotGrantAnyExistingVersionOrCodecPermission()throws Exception {
        try(var f=fixture()) {
            var loading=publish(f.engine.serviceObservation());assertEquals(ModernBattleEngine.Phase.LOADING,loading.world().phase());
            assertTrue(loading.world().hasteEffects().targets().isEmpty());
            var p=publish(active(f));var v=p.audience(f.players.get(0).scope());assertTrue(p.descriptor().has(ModernMatchDescriptor.HASTE));
            assertTrue(v.world().hasteEffects().targets().isEmpty());assertNull(v.npcMotion());
            for(int version=1;version<44;version++)assertFalse(p.descriptor().supportedBy(version,Integer.MAX_VALUE));
            assertTrue(p.descriptor().supportedBy(44,ModernSessionProtocol.KNOWN_CAPABILITIES));
            assertFalse(p.descriptor().supportedBy(44,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_HASTE));
            for(int schema=1;schema<=4;schema++){int selected=schema;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,selected));}
            for(int version=35;version<=43;version++){int selected=version;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(v,selected));}
            var bytes=ByteBuffer.allocate(64);assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.put(bytes,p.descriptor()));assertEquals(0,bytes.position());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(p.capture()));
            var d=p.descriptor();var noFeature=new ModernMatchDescriptor(d.ruleset(),d.features()&~ModernMatchDescriptor.HASTE,2000,d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(p.capture(),noFeature,null,null));
        }
    }
    @Test void missingForeignReorderedAndExtraEvidenceCannotConstructTheCheckedWorld()throws Exception {
        try(var f=fixture()) {
            var empty=publish(active(f)).world();var w=publish(cast(f,0,1,1).observation()).world();var s=w.hasteEffects();
            assertThrows(IllegalArgumentException.class,()->with(w,null));
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(s.world(),s.revision(),List.of(),null)));
            assertThrows(IllegalArgumentException.class,()->with(empty,s));
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(new ModernTargetHistory.World(s.world().roomId(),s.world().matchId(),s.world().generation()+1),s.revision(),s.targets(),null)));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteEffects.State(s.world(),s.revision(),List.of(s.targets().get(0),s.targets().get(0)),null));
            f.elapsed(2*SECOND);var both=publish(cast(f,0,2,0).observation()).world().hasteEffects();
            assertThrows(IllegalArgumentException.class,()->new ModernHasteEffects.State(both.world(),both.revision(),List.of(both.targets().get(1),both.targets().get(0)),null));
        }
    }
    @Test void publicReceiptCannotBorrowAnotherSourceClockOrSpendCount()throws Exception {
        try(var f=fixture()) {
            active(f);var w=publish(cast(f,0,1,1).observation()).world();var s=w.hasteEffects();var e=s.targets().get(0);var g=e.active();var r=g.receipt();
            var foreign=new ModernHasteEffects.Grant(new ModernTargetHistory.Participant(1,ModernParticipantId.human(42,3333)),r);
            var later=new ModernHasteEffects.Grant(g.caster(),new ModernBattleObservation.SpellReceipt(r.impact(),r.casts(),r.spent(),r.loadingElapsedNanos()+1,r.detail()));
            for(var wrong:List.of(foreign,later))assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(s.world(),s.revision(),List.of(new ModernHasteEffects.Entry(e.target(),wrong,null)),null)));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteEffects.Grant(g.caster(),new ModernBattleObservation.SpellReceipt(r.impact(),r.casts(),r.spent()+1,r.loadingElapsedNanos(),r.detail())));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteEffects.Entry(e.target(),null,g));
        }
    }
    @Test void sameClockRefreshCannotUseTheOtherCastersSupersededGrant()throws Exception {
        try(var f=fixture()) {
            active(f);var old=publish(cast(f,0,1,1).observation()).world().hasteEffects();var w=publish(cast(f,1,1,1).observation()).world();
            assertEquals(old.targets().get(0).active().detail().activeElapsedNanos(),w.hasteEffects().targets().get(0).active().detail().activeElapsedNanos());
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(w.identity(),w.revision(),old.targets(),null)));
        }
    }
    @Test void npcCurrentPaceAndCommittedWalkHaveSeparateFactsAcrossRefreshAndExpiry()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(280)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(150);ModernBattleNpcHasteTest.cast(f,0,1,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation()));
            var initial=publish(f);var m=initial.world().hasteEffects().npcs().actors().get(0);
            assertEquals(75,m.currentPace().walkMillis());assertEquals(100,m.walk().durationMillis());assertFalse(m.walkSpeed().hasted());
            f.elapsed(200);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var stepped=publish(f).world().hasteEffects();var previous=stepped.targets().get(0).walk();assertNotNull(previous);
            f.elapsed(2000);ModernBattleNpcHasteTest.cast(f,0,2,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation()));
            var refreshed=publish(f).world().hasteEffects();assertEquals(previous,refreshed.targets().get(0).walk());assertNotEquals(previous,refreshed.targets().get(0).active());
            f.elapsed(1_202_000);var expired=publish(f);var status=expired.audience(f.players.get(0).scope()).world().hasteEffects();
            assertNull(status.targets().get(0).active());assertEquals(previous,status.targets().get(0).walk());assertEquals(100,status.npcs().actors().get(0).currentPace().walkMillis());
            assertEquals(75,status.npcs().actors().get(0).walk().durationMillis());assertNull(expired.audience(f.players.get(0).scope()).npcMotion());
            assertThrows(IllegalArgumentException.class,()->ModernNpcMotion.View.from(expired.capture().npcs().roster().actors().get(0)));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(expired.audience(f.players.get(0).scope()),43));
        }
    }
    @Test void questAudienceIncludesOnlyOwnLaneGrantsAndStatusWithoutRenumbering()throws Exception {
        try(var f=ModernBattleNpcHasteTest.fixture(true,2,0,300)) {
            var initial=ModernBattleNpcHasteTest.active(f);var ns=initial.npcs().roster().actors();
            for(int who=0;who<2;who++)assertEquals(ModernHasteCast.Reason.NONE,ModernBattleNpcHasteTest.cast(f,who,1,ns.get(who)).reason());
            f.elapsed(2000);var c=f.engine.serviceObservation();assertEquals(ModernHasteCast.Reason.NONE,f.engine.haste(f.players.get(0).owner(),request(c,0,2,target(c,0))).reason());
            var p=publish(f);assertEquals(3,p.world().hasteEffects().targets().size());
            for(int who=0;who<2;who++) {
                var w=p.audience(f.players.get(who).scope()).world();var state=w.hasteEffects();
                assertEquals(who+1,w.actors().get(0).pose().entityId());assertEquals(who==0?2:1,state.targets().size());assertEquals(1,state.npcs().actors().size());
                assertEquals(ns.get(who).id(),state.npcs().actors().get(0).actor());
                int owner=who;assertTrue(state.targets().stream().flatMap(e->e.grants().stream()).allMatch(g->g.caster().entityId()==owner+1));
                assertThrows(IllegalArgumentException.class,()->with(w,p.world().hasteEffects()));
            }
        }
    }
    @Test void retiredNpcReceiptStillCarriesItsOriginalLaneAndRejectsCrossLaneForgery()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(1,true)) {
            var initial=ModernBattleNpcHasteTest.active(f);var n=initial.npcs().roster().actors().get(1);
            ModernBattleNpcHasteTest.cast(f,1,1,n);f.elapsed(2000);f.randomValue=19;var p=f.players.get(1);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,n.id(),1).reason());
            f.elapsed(4000);var w=publish(f).world();assertTrue(w.npcs().roster().actors().stream().noneMatch(a->a.id().equals(n.id())));assertTrue(w.hasteEffects().targets().isEmpty());
            var r=w.actors().get(1).spells().receipts().get(LANE);var d=(ModernHasteReceipt)r.detail();var t=(ModernHasteReceipt.Npc)d.target();assertEquals(1,t.lane());
            var forged=new ModernHasteReceipt(d.activeElapsedNanos(),new ModernHasteReceipt.Npc(t.id(),t.life(),0,t.cell(),t.rosterRevision(),t.before(),t.after()));
            var changed=new ModernBattleObservation.SpellReceipt(r.impact(),r.casts(),r.spent(),r.loadingElapsedNanos(),forged);
            assertThrows(IllegalArgumentException.class,()->receipt(w,1,changed));
        }
    }
    @Test void changedOrMissingNpcMotionCannotBorrowAnOtherwiseValidGrant()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(280)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);ModernBattleNpcHasteTest.cast(f,0,1,n);
            var w=publish(f).world();var s=w.hasteEffects();var npcs=s.npcs();var m=npcs.actors().get(0);
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(s.world(),s.revision(),s.targets(),null)));
            var forged=new ModernHasteEffects.Motion(m.actor(),m.life(),new ModernNpcMotion.Pace(101,750),m.speed(),m.walkSpeed(),m.walk());
            var changed=new ModernHasteEffects.Npcs(npcs.scope(),npcs.revision(),npcs.elapsedMillis(),List.of(forged));
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernHasteEffects.State(s.world(),s.revision(),s.targets(),changed)));
        }
    }
    @Test void publicObjectGraphCannotReachPrivateSourceEpochCatalogOrRawReceipt() {
        var graph=ModernBattleAudienceTest.graph(ModernHasteEffects.State.class,Set.of(ModernRooms.Scope.class,ModernTargetHistory.Batch.class,
                ModernSpellRuntime.Receipt.class,ModernSpellRuntime.State.class,ModernHasteTargets.State.class,ModernHasteTargets.Entry.class,
                ModernNpcRoster.Template.class,ModernBattleObservation.Capture.class,ModernBattleLoadout.class,ModernCharacterData.Snapshot.class));
        assertTrue(graph.contains(ModernHasteEffects.Motion.class));assertTrue(graph.contains(ModernHasteReceipt.Npc.class));
    }
    @Test void audienceRosterClockCannotLeadWorldButTimeoutMayCloseAnOlderRoster()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(280)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);
            ModernBattleNpcHasteTest.cast(f,0,1,n);var w=publish(f).audience(f.players.get(0).scope()).world();
            var s=w.hasteEffects();var roster=s.npcs();var body=w.npcs();
            var future=new ModernHasteEffects.Npcs(roster.scope(),roster.revision(),roster.elapsedMillis()+1,roster.actors());
            var changed=new ModernHasteEffects.State(s.world(),s.revision(),s.targets(),future);
            var futureBody=new ModernBattleAudience.Npcs(body.revision(),body.elapsedMillis()+1,body.spawned(),body.closed(),body.actors(),body.exchanges(),body.resources(),body.names());
            assertThrows(IllegalArgumentException.class,()->new ModernBattleAudience.World(w.identity(),w.revision(),w.phase(),w.abortReason(),
                    w.room(),w.manifest(),w.descriptor(),w.clock(),w.lane(),w.actors(),w.modes(),futureBody,w.result(),changed));
            f.elapsed(f.engine.serviceObservation().limits().activeTimeoutMillis());var terminal=publish(f).world();
            assertEquals(ModernBattleEngine.Phase.ABORTED,terminal.phase());
            assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,terminal.abortReason());
            assertTrue(terminal.npcs().roster().elapsedMillis()<terminal.clock().activeElapsedNanos()/1_000_000L);
            assertTrue(terminal.npcs().roster().closed());assertTrue(terminal.hasteEffects().targets().isEmpty());
            assertTrue(terminal.hasteEffects().npcs().actors().isEmpty());
        }
    }
    @Test void potionOverwriteAndAbortRemoveOnlyLiveProofWithoutReconstructingHistory()throws Exception {
        try(var f=fixture()) {
            active(f);var first=publish(cast(f,0,1,1).observation());var old=first.world().hasteEffects().targets().get(0).active();
            f.elapsed(SECOND);assertEquals(ModernItemProtocol.Reason.NONE,f.use(1,40013).reason());
            var potion=publish(f.engine.serviceObservation());assertTrue(potion.world().hasteEffects().targets().isEmpty());
            assertEquals(old.receipt(),potion.world().actors().get(0).spells().receipts().get(LANE));
            var aborted=publish(f.engine.abortObservation());assertEquals(ModernBattleEngine.Phase.ABORTED,aborted.world().phase());
            assertTrue(aborted.world().hasteEffects().targets().isEmpty());assertEquals(old.receipt(),aborted.world().actors().get(0).spells().receipts().get(LANE));
        }
    }
    @Test void all128ResidentNpcsRetainDistinctEffectsFromOneCasterInBoundedPublicData()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(280,false,false,10_000)) {
            ModernBattleNpcHasteTest.active(f);var template=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var spawns=new ArrayList<ModernNpcRoster.Spawn>();for(int i=0;i<128;i++)spawns.add(new ModernNpcRoster.Spawn(template,0,1,ModernBattleEngineNpcTest.cell(2+i%16,2+i/16),2));
            var born=f.engine.spawnNpcsObservation(f.scope,spawns).npcs().roster().actors();
            for(int i=0;i<128;i++){f.elapsed(i*2000);assertEquals(ModernHasteCast.Reason.NONE,ModernBattleNpcHasteTest.cast(f,0,i+1,born.get(i)).reason());}
            var p=publish(f);var s=p.audience(f.players.get(0).scope()).world().hasteEffects();assertEquals(128,s.targets().size());assertEquals(128,s.npcs().actors().size());
            assertEquals(5120,p.world().actors().get(0).resources().flows().spent());
            assertEquals(1,s.targets().get(0).active().receipt().casts());assertEquals(128,s.targets().get(127).active().receipt().casts());
            var tooMany=new ArrayList<>(s.npcs().actors());tooMany.add(s.npcs().actors().get(127));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteEffects.Npcs(s.npcs().scope(),s.npcs().revision(),s.npcs().elapsedMillis(),tooMany));
        }
    }
    @Test void hundredRealRoomParticipantsKeepDistinctSelfGrantsAndOwnerEnvelopes()throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(100,0)) {
            var ticket=room.capture();var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.prepared(ticket)) {
                var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),p.fighter().profile().effective().maximumHp(),100,12,0);
                int i=prepared.size();prepared.add(enable(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),
                        ModernBattleEngineBotTest.cell(10+i%10*2,10+i/10*2),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),
                        List.of(),ModernInventoryWeight.qualify(fighter.profile(),100),true),ModernHasteCatalog.Status.QUALIFIED));
            }
            try(var e=new ModernBattleEngine(ticket,room.binding,prepared,List.of(),ModernBattleEngineBotTest.LIMITS,()->SECOND,()->1000,bound->0,false)) {
                for(var p:room.players)e.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());
                for(int i=0;i<100;i++){var c=e.serviceObservation();assertEquals(ModernHasteCast.Reason.NONE,e.haste(room.players.get(i).owner(),request(c,i,1,target(c,i))).reason());}
                var p=publish(e.serviceObservation());assertEquals(100,p.world().hasteEffects().targets().size());
                for(int i=0;i<100;i++){var v=p.audience(room.players.get(i).scope());assertEquals(i+1,v.personal().entityId());assertSame(p.audience(room.players.get(0).scope()).world(),v.world());}
            }
        }
    }
    @Test void botTargetRemainsPublicWithoutInventingPrivateOwnerOrCasterHistory()throws Exception {
        try(var f=ModernBattleNpcHasteTest.fixture(false,1,1,300)) {
            var c=ModernBattleNpcHasteTest.active(f);var target=target(c,1);
            assertEquals(ModernHasteCast.Reason.NONE,f.engine.haste(f.players.get(0).owner(),request(c,0,1,target)).reason());
            var p=publish(f);var v=p.audience(f.players.get(0).scope());var bot=v.world().actors().get(1);
            assertFalse(bot.pose().member().identity().isHuman());assertEquals(0,bot.spells().sequence());assertTrue(bot.spells().receipts().isEmpty());
            assertEquals(new ModernHasteEffects.Player(target.participant()),v.world().hasteEffects().targets().get(0).target());
            assertEquals(1,v.world().hasteEffects().targets().get(0).active().caster().entityId());
            // BOT and HUMAN namespaces may share numeric source/object IDs.
            // A trusted scope lookup is not a connection authentication API.
            assertNotEquals(bot.pose().member().identity(),v.world().actors().get(0).pose().member().identity());
            assertNull(p.capture().actors().get(1).scope());
            assertEquals(Set.of(f.players.get(0).scope()),p.capture().ownerViews(p.world()).keySet());
            var owner=f.players.get(0).scope();
            assertThrows(SecurityException.class,()->p.audience(new ModernRooms.Scope(owner.sourceId(),owner.objectId(),owner.epoch()+1,owner.channelId())));
        }
    }
    @Test void typedTargetOrderingDoesNotOffsetOrNarrowAFullWidthNpcSerial()throws Exception {
        try(var f=ModernHasteNpcTargetsTest.fixture(280)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);ModernBattleNpcHasteTest.cast(f,0,1,n);
            var w=publish(f).world();var original=w.hasteEffects().targets().get(0).active();var r=original.receipt();var d=original.detail();var t=(ModernHasteReceipt.Npc)d.target();
            // Record-boundary test only; an actual world must also prove that
            // this serial was spawned. No fake spawn or authority is created.
            for(long serial:new long[]{Long.MAX_VALUE-1,Long.MAX_VALUE}) {
                var id=new ModernNpcRoster.Id(t.id().scope(),serial);var detail=new ModernHasteReceipt(d.activeElapsedNanos(),
                        new ModernHasteReceipt.Npc(id,t.life(),t.lane(),t.cell(),t.rosterRevision(),t.before(),t.after()));
                var grant=new ModernHasteEffects.Grant(original.caster(),new ModernBattleObservation.SpellReceipt(r.impact(),r.casts(),r.spent(),r.loadingElapsedNanos(),detail));
                var candidate=new ModernHasteEffects.State(w.identity(),w.revision(),List.of(new ModernHasteEffects.Entry(new ModernHasteEffects.Npc(id),grant,null)),w.hasteEffects().npcs());
                assertEquals(serial,((ModernHasteEffects.Npc)candidate.targets().get(0).target()).id().serial());
                assertThrows(IllegalArgumentException.class,()->with(w,candidate));
            }
        }
    }
}
