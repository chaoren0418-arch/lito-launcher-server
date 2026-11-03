package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernMatchmakingTest {
    record Player(TournamentPresence.Owner owner, TournamentPresence.Selection selection, ModernRooms.Scope scope) {}
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence = new TournamentPresence();
        final ModernRooms rooms;
        final List<Player> players = new ArrayList<>();
        long next = 1;
        Fixture() { this(allOptions(), Map.of()); }
        Fixture(Set<ModernWorldBinding.Key> worlds, Map<ModernWorldBinding.Key,Integer> capacities) {
            rooms = new ModernRooms(worlds, Set.of(), capacities);
        }
        Player player() { return player(7, next++, 80, 0); }
        Player player(int source, long object, long level, int channel) {
            var owner = presence.connect();
            var selected = new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source, object),
                    "來源" + source, "角色" + object, level, 1, 0);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED, presence.select(owner, selected));
            rooms.enterLobby(owner, selected, 1, channel, true);
            var player = new Player(owner, selected, new ModernRooms.Scope(source, object, 1, channel));
            players.add(player); return player;
        }
        ModernMatchmaking.Result start(Player p, ModernMatchmaking.Criteria c, long wins, long losses) {
            return rooms.startMatchmaking(p.owner(), p.scope(), c, standing(p.selection().key(), wins, losses));
        }
        ModernMatchmaking.Result read(Player p, long id) { return rooms.readMatchmaking(p.owner(), p.scope(), id); }
        ModernMatchmaking.Result cancel(Player p, long id) { return rooms.cancelMatchmaking(p.owner(), p.scope(), id); }
        ModernRooms.View view(Player p) {
            return rooms.execute(p.owner(), new ModernRooms.Command(p.scope(), ModernRooms.Action.READ,0,0,0,null,"")).view();
        }
        ModernRooms.Result create(Player p, String password) {
            return rooms.execute(p.owner(), new ModernRooms.Command(p.scope(), ModernRooms.Action.CREATE,0,0,0,
                    new ModernRooms.Draft("手動房",password,2,1,514,0),""));
        }
        ModernRooms.Result leave(Player p) {
            var s = view(p).joined().summary();
            return rooms.execute(p.owner(), new ModernRooms.Command(p.scope(),ModernRooms.Action.LEAVE,s.id(),s.revision(),0,null,""));
        }
        public void close() {
            for (var p : players) { rooms.disconnect(p.owner()); presence.disconnect(p.owner()); }
            assertEquals(0, rooms.queuedMatchmakingCount()); assertEquals(0,rooms.ownerCount());
            assertEquals(0, rooms.memberCount()); assertEquals(0,rooms.roomCount());
            assertEquals(0, rooms.chatOwnerCount()); assertEquals(0,rooms.directChatOwnerCount());
            assertEquals(0, rooms.filteredChatOwnerCount());
        }
    }
    // Synthetic qualification proves grouping policy only, not these 40 worlds'
    // assets or gameplay. Production options still require verified bindings.
    static Set<ModernWorldBinding.Key> allOptions() {
        var keys = new HashSet<ModernWorldBinding.Key>();
        for (var mode : BattleRules.Mode.values()) if (mode.isBattleScene())
            for (int map : mode.maps()) keys.add(new ModernWorldBinding.Key(mode.wireId(),map));
        return Set.copyOf(keys);
    }
    static ModernMatchmaking.Criteria pair() { return new ModernMatchmaking.Criteria(1,514,2); }
    static ModernCharacterRecord.Snapshot standing(TournamentPresence.CharacterKey key,long wins,long losses) {
        return new ModernCharacterRecord.Snapshot(key,true,wins,losses,Math.addExact(wins,losses));
    }
    static void waiting(ModernMatchmaking.Result r) {
        assertEquals(ModernRooms.Status.OK,r.rooms().status()); assertEquals(ModernMatchmaking.State.WAITING,r.state());
        assertTrue(r.ticketId()>0); assertNull(r.rooms().view().joined());
    }
    static void set(Object target, String field, Object value) throws Exception {
        var member=target.getClass().getDeclaredField(field); member.setAccessible(true); member.set(target,value);
    }

    @Test void admissionIsNotMembershipAndCommitPublishesBothSourcesWithUnreadyBalancedTeams() {
        try(var f=new Fixture()) {
            var a=f.player(7,3333,80,0); var b=f.player(42,3333,80,0);
            var ar=f.start(a,pair(),4,6); var br=f.start(b,pair(),8,12); waiting(ar); waiting(br);
            assertEquals(0,f.rooms.roomCount()); assertEquals(0,f.rooms.memberCount());
            waiting(f.read(a,0)); // Bootstrap cannot perform the queued mutation.
            var result=f.read(b,br.ticketId()); assertEquals(ModernMatchmaking.State.MATCHED,result.state());
            var joined=result.rooms().view().joined(); assertEquals(a.selection().key(),joined.owner());
            assertEquals(joined,f.read(a,ar.ticketId()).rooms().view().joined());
            assertEquals(List.of(7,42),joined.members().stream().map(m->m.key().sourceId()).toList());
            assertEquals(List.of("來源7","來源42"),joined.members().stream().map(ModernRooms.Member::sourceName).toList());
            assertEquals(Set.of(0,1),new HashSet<>(joined.members().stream().map(ModernRooms.Member::team).toList()));
            assertTrue(joined.members().stream().noneMatch(ModernRooms.Member::ready)); assertFalse(joined.summary().readySatisfied());
            assertEquals(1,joined.summary().revision()); assertEquals(2,result.rooms().view().listRevision());
            assertEquals(0,f.rooms.queuedMatchmakingCount()); assertFalse(f.rooms.isPreparing(a.owner()));
        }
    }

    @Test void earliestWaiterAnchorsNearestWinRateAndFastPollerCannotJumpTheQueue() {
        try(var f=new Fixture()) {
            var oldest=f.player();var distant=f.player();var closest=f.player();var tied=f.player();
            var first=f.start(oldest,pair(),5,5);f.start(distant,pair(),0,10);
            f.start(closest,pair(),4,6);var last=f.start(tied,pair(),6,4);
            // The fastest/newest poller is not selected: equal 0.1 distance is FIFO.
            waiting(f.read(tied,last.ticketId()));
            var matched=f.read(oldest,first.ticketId());
            assertEquals(Set.of(oldest.selection().key(),closest.selection().key()),new HashSet<>(matched.rooms().view().joined()
                    .members().stream().map(ModernRooms.Member::key).toList()));
            assertEquals(2,f.rooms.queuedMatchmakingCount());
            assertEquals(ModernMatchmaking.State.MATCHED,f.read(tied,last.ticketId()).state());
            assertEquals(2,f.rooms.roomCount());
        }
    }

    @Test void exactLongRationalDistanceDoesNotCollapseIntoDoubleTieAndZeroHistoryIsExplicit() {
        var key=new TournamentPresence.CharacterKey(7,3333);
        var request=standing(key,Long.MAX_VALUE-1,1);
        var closer=standing(key,Long.MAX_VALUE-2,2);var farther=standing(key,Long.MAX_VALUE-3,3);
        assertTrue(ModernMatchmaking.compare(request,closer,9,farther,1)<0);
        assertTrue(ModernMatchmaking.compare(request,farther,1,closer,9)>0);
        var empty=ModernCharacterRecord.Snapshot.absent(key);
        assertTrue(ModernMatchmaking.compare(empty,empty,9,closer,1)<0);
        assertTrue(ModernMatchmaking.compare(empty,closer,9,farther,1)>0);
        assertTrue(ModernMatchmaking.compare(standing(key,1,2),standing(key,2,4),2,standing(key,3,6),1)>0);
        assertThrows(IllegalArgumentException.class,()->ModernMatchmaking.compare(empty,empty,0,empty,1));
    }

    @Test void criteriaRejectUnknownModesMapsCountsAndNativeBandGuessing() {
        for(int[] row:new int[][]{{6,500,2},{-1,0,2},{1,540,2},{1,-1,2},{1,514,1},{1,514,3},{1,514,102},
                {0,565,11},{4,540,61},{3,547,BattleRules.SIEGE_MINIMUM_PLAYERS-2}})
            assertThrows(IllegalArgumentException.class,()->new ModernMatchmaking.Criteria(row[0],row[1],row[2]));
        assertDoesNotThrow(()->new ModernMatchmaking.Criteria(4,540,1));
        assertDoesNotThrow(()->new ModernMatchmaking.Criteria(1,0,100));
    }

    @Test void onlyQualifiedMapsAndTheirConfiguredCapacityCanAcceptAdmission() {
        var key=new ModernWorldBinding.Key(1,514);
        try(var f=new Fixture(Set.of(key),Map.of(key,4))) {
            var p=f.player();var original=f.view(p);
            for(var c:List.of(new ModernMatchmaking.Criteria(1,509,2),new ModernMatchmaking.Criteria(1,514,6),
                    new ModernMatchmaking.Criteria(2,554,2))) {
                var result=f.start(p,c,0,0); assertEquals(ModernRooms.Reason.INVALID_CONFIGURATION,result.rooms().reason());
                assertEquals(ModernMatchmaking.State.IDLE,result.state());assertEquals(original,f.view(p));
            }
            waiting(f.start(p,new ModernMatchmaking.Criteria(1,0,4),0,0));
            assertEquals(1,f.rooms.queuedMatchmakingCount());
        }
    }

    @Test void differentModesMapsCountsAndChannelsNeverSilentlyCoalesce() {
        try(var f=new Fixture()) {
            var original=f.player();var start=f.start(original,pair(),0,0);
            for(var criteria:List.of(new ModernMatchmaking.Criteria(1,0,2),new ModernMatchmaking.Criteria(1,509,2),
                    new ModernMatchmaking.Criteria(1,514,4),new ModernMatchmaking.Criteria(2,554,2)))
                waiting(f.start(f.player(),criteria,0,0));
            waiting(f.start(f.player(42,99,50,1),pair(),0,0));
            waiting(f.read(original,start.ticketId()));assertEquals(0,f.rooms.roomCount());
            assertEquals(6,f.rooms.queuedMatchmakingCount());
        }
    }

    @Test void allSixModeAndFortyPairPoliciesCreateOnlyCompleteGroupsAndRespectReadyBarrier() {
        for(var key:allOptions()) try(var f=new Fixture()) {
            var rules=TournamentRoomPolicy.rules(key.mode(),key.logicalMapId());
            var criteria=new ModernMatchmaking.Criteria(key.mode(),key.logicalMapId(),rules.minimumPlayers());
            var entrants=new ArrayList<Player>();var tickets=new ArrayList<Long>();
            for(int i=0;i<criteria.players();i++) {
                var p=f.player();entrants.add(p);var result=f.start(p,criteria,0,0);waiting(result);tickets.add(result.ticketId());
            }
            var result=f.read(entrants.get(0),tickets.get(0));assertEquals(ModernMatchmaking.State.MATCHED,result.state());
            var room=result.rooms().view().joined();assertEquals(criteria.players(),room.members().size());
            assertEquals(Math.max(2,criteria.players()),room.summary().config().capacity());
            assertEquals(key.logicalMapId(),room.summary().config().mapId());
            assertFalse(room.summary().readySatisfied());
            if(rules.teamBased())assertEquals(criteria.players()/2,room.members().stream().filter(m->m.team()==0).count());
            var first=entrants.get(0);
            assertEquals(ModernRooms.Status.DENIED,f.rooms.prepareWorld(first.owner(),first.scope(),room.summary().id(),room.summary().revision()).result().status());
        }
    }

    @Test void automaticMapChoiceRotatesOnlyAcrossQualifiedCapacitySuitableWorlds() {
        var worlds=Set.of(new ModernWorldBinding.Key(1,509),new ModernWorldBinding.Key(1,514),new ModernWorldBinding.Key(1,564));
        try(var f=new Fixture(worlds,Map.of(new ModernWorldBinding.Key(1,564),2))) {
            var c=new ModernMatchmaking.Criteria(1,0,4);var selected=new ArrayList<Integer>();
            for(int group=0;group<4;group++) {
                var first=f.player();var ticket=f.start(first,c,0,0).ticketId();
                for(int i=1;i<4;i++)f.start(f.player(),c,0,0);
                selected.add(f.read(first,ticket).rooms().view().joined().summary().config().mapId());
            }
            assertEquals(List.of(509,514,509,514),selected);
        }
    }

    @Test void recordOwnerScopeEpochAndLevelAreRecheckedBeforeAdmission() {
        try(var f=new Fixture()) {
            var p=f.player();var score=standing(p.selection().key(),1,1);
            assertThrows(SecurityException.class,()->f.rooms.startMatchmaking(p.owner(),p.scope(),pair(),standing(new TournamentPresence.CharacterKey(42,p.scope().objectId()),1,1)));
            for(var scope:List.of(new ModernRooms.Scope(42,p.scope().objectId(),1,0),new ModernRooms.Scope(7,p.scope().objectId(),2,0),
                    new ModernRooms.Scope(7,p.scope().objectId(),1,1)))
                assertThrows(SecurityException.class,()->f.rooms.startMatchmaking(p.owner(),scope,pair(),score));
            var low=f.player(7,999,14,0);assertEquals(ModernRooms.Reason.LEVEL_RESTRICTED,f.start(low,pair(),0,0).rooms().reason());
            var high=f.player(7,998,80,3);assertEquals(ModernRooms.Reason.LEVEL_RESTRICTED,f.start(high,pair(),0,0).rooms().reason());
            assertEquals(0,f.rooms.queuedMatchmakingCount());
            var admitted=f.start(p,pair(),1,1);waiting(admitted);
            assertEquals(ModernRooms.Reason.INVALID_STATE,f.start(p,pair(),99,1).rooms().reason());
            assertEquals(admitted,f.read(p,0)); // Repeat START cannot refresh rank or FIFO.
        }
    }

    @Test void cancelIsIdempotentAndStaleCancelOrPollCannotRetireOrAdvanceANewAdmission() {
        try(var f=new Fixture()) {
            var p=f.player();var old=f.start(p,pair(),0,0);waiting(old);
            assertEquals(ModernMatchmaking.State.IDLE,f.cancel(p,old.ticketId()).state());
            assertEquals(ModernMatchmaking.State.IDLE,f.cancel(p,old.ticketId()).state());
            var fresh=f.start(p,pair(),0,0);assertTrue(fresh.ticketId()>old.ticketId());
            var other=f.player();f.start(other,pair(),0,0);
            for(var result:List.of(f.cancel(p,old.ticketId()),f.read(p,old.ticketId()))) {
                assertEquals(ModernRooms.Status.CONFLICT,result.rooms().status());assertEquals(fresh.ticketId(),result.ticketId());
            }
            assertEquals(0,f.rooms.roomCount());assertEquals(2,f.rooms.queuedMatchmakingCount());
            assertEquals(ModernMatchmaking.State.MATCHED,f.read(p,fresh.ticketId()).state());
            assertEquals(ModernMatchmaking.State.MATCHED,f.cancel(p,fresh.ticketId()).state());
            assertTrue(f.rooms.hasRoom(p.owner())); // Late cancel never becomes leave.
            assertEquals(ModernRooms.Status.OK,f.leave(p).status());assertEquals(ModernMatchmaking.State.IDLE,f.read(p,0).state());
        }
    }

    @Test void leaveDisconnectAndSameOwnerReentryRetireQueueBeforeNewScopeCanMatch() {
        for(int operation=0;operation<3;operation++)try(var f=new Fixture()) {
            var p=f.player();var ticket=f.start(p,pair(),0,0);
            if(operation==0)f.rooms.leaveLobby(p.owner());
            else if(operation==1)f.rooms.disconnect(p.owner());
            else f.rooms.enterLobby(p.owner(),p.selection(),2,0,true);
            assertEquals(0,f.rooms.queuedMatchmakingCount());
            assertThrows(SecurityException.class,()->f.read(p,ticket.ticketId()));
            var other=f.player();var waiting=f.start(other,pair(),0,0);waiting(f.read(other,waiting.ticketId()));
            assertEquals(0,f.rooms.roomCount());
        }
    }

    @Test void successfulManualCreateOrJoinRetiresQueueButRejectedJoinKeepsIt() {
        try(var f=new Fixture()) {
            var creator=f.player();var created=f.create(creator,"secret").view().joined().summary();
            var joiner=f.player();var admission=f.start(joiner,pair(),0,0);
            var denied=f.rooms.execute(joiner.owner(),new ModernRooms.Command(joiner.scope(),ModernRooms.Action.JOIN,
                    created.id(),created.revision(),0,null,"wrong"));
            assertEquals(ModernRooms.Reason.BAD_PASSWORD,denied.reason());waiting(f.read(joiner,0));
            assertEquals(ModernRooms.Status.OK,f.rooms.execute(joiner.owner(),new ModernRooms.Command(joiner.scope(),ModernRooms.Action.JOIN,
                    created.id(),created.revision(),0,null,"secret")).status());
            assertEquals(ModernMatchmaking.State.IDLE,f.cancel(joiner,admission.ticketId()).state());assertTrue(f.rooms.hasRoom(joiner.owner()));
            var manual=f.player();f.start(manual,pair(),0,0);assertEquals(ModernRooms.Status.OK,f.create(manual,"").status());
            assertEquals(0,f.rooms.queuedMatchmakingCount());assertEquals(ModernMatchmaking.State.IDLE,f.read(manual,0).state());
        }
    }

    @Test void fullRoomRegistryCannotPartiallyCommitAndQueuedGroupCanResumeAfterSlotReleased() {
        try(var f=new Fixture()) {
            var a=f.player();var ar=f.start(a,pair(),0,0);var b=f.player();f.start(b,pair(),0,0);
            Player last=null;for(int i=0;i<ModernRooms.MAX_ROOMS;i++) {last=f.player();assertEquals(ModernRooms.Status.OK,f.create(last,"").status());}
            var denied=f.read(a,ar.ticketId());assertEquals(ModernRooms.Status.FULL,denied.rooms().status());
            assertEquals(ModernMatchmaking.State.WAITING,denied.state());assertNull(f.view(b).joined());assertEquals(2,f.rooms.queuedMatchmakingCount());
            assertEquals(ModernRooms.Status.FULL,f.start(f.player(),pair(),0,0).rooms().status());
            assertEquals(ModernRooms.Status.OK,f.leave(last).status());
            assertEquals(ModernMatchmaking.State.MATCHED,f.read(a,ar.ticketId()).state());assertEquals(100,f.rooms.roomCount());
        }
    }

    @Test void revisionAndAdmissionExhaustionStayFailClosedButCancellationStillReleasesQueue() throws Exception {
        try(var f=new Fixture()) {
            var p=f.player();set(f.rooms,"nextMatchmakingId",Long.MAX_VALUE);
            assertEquals(ModernRooms.Reason.REVISION_EXHAUSTED,f.start(p,pair(),0,0).rooms().reason());
            assertEquals(0,f.rooms.queuedMatchmakingCount());set(f.rooms,"nextMatchmakingId",1L);
            var a=f.start(p,pair(),0,0);var other=f.player();f.start(other,pair(),0,0);
            set(f.rooms,"nextRoomId",Long.MAX_VALUE);
            assertEquals(ModernRooms.Reason.REVISION_EXHAUSTED,f.read(p,a.ticketId()).rooms().reason());
            assertEquals(ModernMatchmaking.State.IDLE,f.cancel(p,a.ticketId()).state());assertEquals(0,f.rooms.roomCount());
        }
    }

    @Test void queueHasAHardGlobalBoundAndFreedAdmissionCanBeReusedWithoutIdReuse() {
        try(var f=new Fixture()) {
            var first=f.player();var ticket=f.start(first,pair(),0,0);long latest=ticket.ticketId();
            for(int i=1;i<ModernMatchmaking.MAX_WAITERS;i++)latest=f.start(f.player(),pair(),0,0).ticketId();
            assertEquals(ModernMatchmaking.MAX_WAITERS,f.rooms.queuedMatchmakingCount());
            var overflow=f.player();var denied=f.start(overflow,pair(),0,0);
            assertEquals(ModernRooms.Status.FULL,denied.rooms().status());assertEquals(ModernMatchmaking.State.IDLE,denied.state());
            f.cancel(first,ticket.ticketId());var admitted=f.start(overflow,pair(),0,0);waiting(admitted);
            assertTrue(admitted.ticketId()>latest);assertEquals(ModernMatchmaking.MAX_WAITERS,f.rooms.queuedMatchmakingCount());
        }
    }

    @Test void concurrentPollersCommitEachPlayerAtMostOnceWithoutReadyOrDuplicateRooms() throws Exception {
        try(var f=new Fixture()) {
            var players=new ArrayList<Player>();var ids=new ArrayList<Long>();
            for(int i=0;i<32;i++){var p=f.player();players.add(p);ids.add(f.start(p,pair(),i,32-i).ticketId());}
            var executor=Executors.newFixedThreadPool(8);var gate=new CountDownLatch(1);
            try {
                var jobs=new ArrayList<java.util.concurrent.Future<ModernMatchmaking.Result>>();
                for(int i=0;i<32;i++){final int n=i;jobs.add(executor.submit(()->{
                    gate.await();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
                    while(true){var result=f.read(players.get(n),ids.get(n));
                        if(result.state()==ModernMatchmaking.State.MATCHED)return result;
                        assertTrue(System.nanoTime()<deadline,"waiting owner must keep polling until its own cohort commits");
                        Thread.sleep(1);
                    }
                }));}
                gate.countDown();for(var job:jobs)assertEquals(ModernRooms.Status.OK,job.get(5,TimeUnit.SECONDS).rooms().status());
                assertEquals(16,f.rooms.roomCount());assertEquals(32,f.rooms.memberCount());assertEquals(0,f.rooms.queuedMatchmakingCount());
                var unique=new HashSet<TournamentPresence.CharacterKey>();var roomIds=new HashSet<Long>();
                for(var p:players){var room=f.view(p).joined();assertNotNull(room);assertFalse(room.summary().readySatisfied());
                    if(roomIds.add(room.summary().id()))for(var m:room.members())assertTrue(unique.add(m.key()));}
                assertEquals(32,unique.size());
            } finally {gate.countDown();executor.shutdownNow();assertTrue(executor.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }

    @Test void cancellationRacingCommitNeverReturnsIdleForAnActuallyJoinedAdmission() throws Exception {
        for(int attempt=0;attempt<24;attempt++)try(var f=new Fixture()) {
            var a=f.player();var b=f.player();var ar=f.start(a,pair(),0,0);var br=f.start(b,pair(),0,0);
            var workers=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
            try {
                var cancel=workers.submit(()->{gate.await();return f.cancel(a,ar.ticketId());});
                var match=workers.submit(()->{gate.await();return f.read(b,br.ticketId());});gate.countDown();
                var cancelled=cancel.get(2,TimeUnit.SECONDS);match.get(2,TimeUnit.SECONDS);
                if(cancelled.state()==ModernMatchmaking.State.IDLE){assertFalse(f.rooms.hasRoom(a.owner()));assertEquals(0,f.rooms.roomCount());}
                else {assertEquals(ModernMatchmaking.State.MATCHED,cancelled.state());assertEquals(2,f.rooms.memberCount());}
            } finally {gate.countDown();workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }

    @Test void aPollCanMatchOtherWaitersButDoesNotPretendItsOwnAdmissionSucceeded() {
        try(var f=new Fixture()) {
            var a=f.player();var b=f.player();var c=f.player();var d=f.player();var e=f.player();var other=f.player();
            var ar=f.start(a,pair(),0,100);var br=f.start(b,pair(),50,50);
            var cr=f.start(c,pair(),25,75);var dr=f.start(d,pair(),75,25);
            var er=f.start(e,pair(),49,51);var last=f.start(other,pair(),1,99);
            waiting(f.read(c,cr.ticketId())); // Oldest A selects F.
            waiting(f.read(d,dr.ticketId())); // Next oldest B selects E.
            for(var row:List.of(Map.entry(a,ar),Map.entry(b,br),Map.entry(e,er),Map.entry(other,last)))
                assertEquals(ModernMatchmaking.State.MATCHED,f.read(row.getKey(),row.getValue().ticketId()).state());
            assertEquals(2,f.rooms.queuedMatchmakingCount());assertNull(f.view(c).joined());assertNull(f.view(d).joined());
            assertEquals(ModernMatchmaking.State.MATCHED,f.read(c,cr.ticketId()).state());
            assertEquals(3,f.rooms.roomCount());assertEquals(0,f.rooms.queuedMatchmakingCount());
        }
    }

    @Test void matchedRoomRetiresLobbyChatAdmissionAndPreservesSourceQualifiedSenders() {
        try(var f=new Fixture()) {
            var a=f.player(7,3333,80,0);var b=f.player(42,3333,80,0);
            for(var p:List.of(a,b)){f.rooms.enableDirectChat(p.owner(),p.scope());f.rooms.enableFilteredChat(p.owner(),p.scope());}
            java.util.function.Function<Player,ModernFilteredChat.Snapshot> read=p->f.rooms.filteredChat(p.owner(),
                    new ModernFilteredChat.Request(new ModernFilteredChat.Token(p.scope(),f.view(p).joined()==null?0:f.view(p).joined().summary().id(),0),
                    ModernFilteredChat.Action.READ,ModernFilteredChat.Kind.NORMAL,0,0,""),null).snapshot();
            var lobby=read.apply(a);var ar=f.start(a,pair(),0,0);f.start(b,pair(),0,0);
            f.rooms.filteredChat(a.owner(),new ModernFilteredChat.Request(lobby.token(),ModernFilteredChat.Action.SEND,
                    ModernFilteredChat.Kind.NORMAL,1,0,"等候中的大廳訊息"),new ModernFilteredChat.SpeechPolicy(lobby.token(),false,false,false,false));
            assertEquals(1,read.apply(b).messages().size());f.read(a,ar.ticketId());
            var room=read.apply(a);assertTrue(room.token().membership()>lobby.token().membership());
            assertTrue(room.messages().isEmpty());assertTrue(read.apply(b).messages().isEmpty());
            assertThrows(SecurityException.class,()->f.rooms.filteredChat(a.owner(),new ModernFilteredChat.Request(lobby.token(),
                    ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,2,1,"不能跨入場"),
                    new ModernFilteredChat.SpeechPolicy(lobby.token(),false,false,false,false)));
            f.rooms.filteredChat(a.owner(),new ModernFilteredChat.Request(room.token(),ModernFilteredChat.Action.SEND,
                    ModernFilteredChat.Kind.NORMAL,room.sequence()+1,room.cursor(),"配對房間訊息"),
                    new ModernFilteredChat.SpeechPolicy(room.token(),false,false,false,false));
            var message=read.apply(b).messages().get(0);assertEquals(a.selection().key(),message.sender().key());
            assertEquals("來源7",message.sender().sourceName());assertEquals("配對房間訊息",message.text());
        }
    }

    @Test void matchedPlayersMustExplicitlyReadyBeforeExistingWorldPreparationAcceptsTheExactRoster() {
        try(var f=new Fixture()) {
            var a=f.player();var b=f.player();var ar=f.start(a,pair(),0,0);f.start(b,pair(),0,0);f.read(a,ar.ticketId());
            for(var p:List.of(a,b)) {
                var s=f.view(p).joined().summary();
                assertEquals(ModernRooms.Status.OK,f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,
                        s.id(),s.revision(),1,null,"")).status());
            }
            var summary=f.view(a).joined().summary();assertTrue(summary.readySatisfied());
            var prepared=f.rooms.prepareWorld(a.owner(),a.scope(),summary.id(),summary.revision());
            assertEquals(ModernRooms.Status.OK,prepared.result().status());assertNotNull(prepared.ticket());
            assertEquals(List.of(a.owner(),b.owner()),prepared.ticket().participants().stream().map(ModernRooms.Participant::owner).toList());
            assertEquals(ModernMatchmaking.State.MATCHED,f.cancel(a,ar.ticketId()).state());assertTrue(prepared.ticket().isLive());
            f.rooms.disconnect(b.owner());assertFalse(prepared.ticket().isLive());assertFalse(f.rooms.isPreparing(a.owner()));
        }
    }

    @Test void listRevisionExhaustionDoesNotConsumeOrPublishHalfACohort() throws Exception {
        try(var f=new Fixture()) {
            var a=f.player();var b=f.player();var ar=f.start(a,pair(),0,0);var br=f.start(b,pair(),0,0);
            var field=ModernRooms.class.getDeclaredField("listRevisions");field.setAccessible(true);((long[])field.get(f.rooms))[0]=Long.MAX_VALUE;
            var result=f.read(a,ar.ticketId());assertEquals(ModernRooms.Reason.REVISION_EXHAUSTED,result.rooms().reason());
            assertEquals(2,f.rooms.queuedMatchmakingCount());assertEquals(0,f.rooms.memberCount());assertEquals(0,f.rooms.roomCount());
            f.cancel(a,ar.ticketId());f.cancel(b,br.ticketId());assertEquals(0,f.rooms.queuedMatchmakingCount());
        }
    }
}
