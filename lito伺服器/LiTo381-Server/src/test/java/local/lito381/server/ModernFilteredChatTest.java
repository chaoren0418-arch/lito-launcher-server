package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernFilteredChatTest {
    static final class Fixture implements AutoCloseable {
        final ModernDirectChatTest.Fixture direct = new ModernDirectChatTest.Fixture(new ModernSpeechAuthority(ModernSpeechAuthorityTest.sources()));
        final ModernChatTest.Fixture base = direct.base;
        ModernChatTest.Player player(int source,long object,int channel) {
            var p=direct.player(source,object,channel); base.rooms.enableFilteredChat(p.owner(),p.scope()); return p;
        }
        ModernFilteredChat.Snapshot read(ModernChatTest.Player p) {
            return base.rooms.filteredChat(p.owner(),new ModernFilteredChat.Request(
                    new ModernFilteredChat.Token(p.scope(),base.room(p),0),ModernFilteredChat.Action.READ,
                    ModernFilteredChat.Kind.NORMAL,0,0,""),null).snapshot();
        }
        ModernFilteredChat.Result say(ModernChatTest.Player p,ModernFilteredChat.Kind kind,String text) {
            return say(p,kind,text,false,false,false,false);
        }
        ModernFilteredChat.Result say(ModernChatTest.Player p,ModernFilteredChat.Kind kind,String text,
                boolean named,boolean timed,boolean room,boolean admin) {
            var before=read(p);
            return base.rooms.filteredChat(p.owner(),new ModernFilteredChat.Request(before.token(),
                    ModernFilteredChat.Action.SEND,kind,before.sequence()+1,before.cursor(),text),
                    new ModernFilteredChat.SpeechPolicy(before.token(),named,timed,room,admin));
        }
        ModernDirectChat.Snapshot privateRead(ModernChatTest.Player p) {
            return base.rooms.moderatedDirectChat(p.owner(),new ModernDirectChatProtocol.Request(p.scope(),base.room(p),read(p).token().membership(),
                    0,0,ModernDirectChat.Action.READ,null,null,""),ModernSpeechAuthorityTest.USER).snapshot();
        }
        ModernDirectChat.Result change(ModernChatTest.Player p,ModernDirectChat.Action action,
                ModernDirectChat.Address address,ModernDirectChat.Preferences prefs,String text) {
            var before=privateRead(p);
            return base.rooms.moderatedDirectChat(p.owner(),new ModernDirectChatProtocol.Request(p.scope(),base.room(p),read(p).token().membership(),
                    before.requestSequence()+1,before.cursor(),action,address==null?null:new ModernDirectChatProtocol.Target(address.sourceId(),address.name()),prefs,text),ModernSpeechAuthorityTest.USER);
        }
        void block(ModernChatTest.Player receiver,ModernChatTest.Player sender) {
            assertEquals(ModernDirectChat.Status.OK,change(receiver,ModernDirectChat.Action.BLOCK,direct.address(sender),null,"").status());
        }
        void prefs(ModernChatTest.Player p,boolean whispers,boolean world,boolean trade) {
            assertEquals(ModernDirectChat.Status.OK,change(p,ModernDirectChat.Action.PREFERENCES,null,
                    new ModernDirectChat.Preferences(whispers,world,trade),"").status());
        }
        List<String> text(ModernChatTest.Player p) { return read(p).messages().stream().map(ModernFilteredChat.Message::text).toList(); }
        public void close() { direct.close(); assertEquals(0,base.rooms.filteredChatOwnerCount()); }
    }

    @Test void qualifiedRejectsApplyToAllPublicKindsButNotTheOtherSourceNamesake() {
        try(var f=new Fixture()) {
            var sun=f.player(7,3333,0);var moon=f.player(42,3333,0);var receiver=f.player(7,9,0);
            f.block(receiver,sun);
            for(var kind:ModernFilteredChat.Kind.values()) {
                assertEquals(ModernFilteredChat.Status.OK,f.say(sun,kind,"不可收"+kind).status());
                assertEquals(ModernFilteredChat.Status.OK,f.say(moon,kind,"月亮"+kind).status());
            }
            var received=f.read(receiver);assertEquals(3,received.cursor());assertFalse(received.truncated());
            assertEquals(List.of(1L,2L,3L),received.messages().stream().map(ModernFilteredChat.Message::id).toList());
            assertTrue(received.messages().stream().allMatch(m->m.sender().key().equals(moon.selection().key())));
            assertEquals(List.of("月亮NORMAL","月亮WORLD","月亮TRADE"),f.text(receiver));
            assertTrue(f.privateRead(receiver).messages().isEmpty());
        }
    }
    @Test void categoryPreferencesAreIndependentAndSendReceiptDoesNotPretendSelfDelivery() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);
            f.prefs(a,false,false,false);f.prefs(b,false,false,true);
            var sent=f.say(a,ModernFilteredChat.Kind.WORLD,"已送但雙方不接世界");
            assertEquals(ModernFilteredChat.Status.OK,sent.status());assertNotNull(sent.receipt());
            assertEquals(a.selection().key(),sent.receipt().sender().key());
            assertEquals(sent.snapshot().sequence(),sent.receipt().sequence());
            assertEquals("已送但雙方不接世界",sent.receipt().text());assertEquals(0,sent.snapshot().cursor());
            assertTrue(sent.snapshot().messages().isEmpty());assertTrue(f.text(b).isEmpty());
            f.say(a,ModernFilteredChat.Kind.TRADE,"交易");f.say(a,ModernFilteredChat.Kind.NORMAL,"一般");
            assertEquals(List.of("一般"),f.text(a));assertEquals(List.of("交易","一般"),f.text(b));
            assertEquals(List.of(1L,2L),f.read(b).messages().stream().map(ModernFilteredChat.Message::id).toList());
            assertFalse(sent.toString().contains("已送"));assertFalse(sent.receipt().toString().contains("已送"));
        }
    }
    @Test void changesAffectOnlySubsequentDeliveryAndUnblockDoesNotResurrectSuppressedMessages() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);
            f.say(a,ModernFilteredChat.Kind.NORMAL,"已交付");var before=f.read(b);
            f.block(b,a);f.say(a,ModernFilteredChat.Kind.NORMAL,"隱藏");
            assertEquals(before,f.read(b)); // No global cursor/timing-count leak.
            f.change(b,ModernDirectChat.Action.UNBLOCK,f.direct.address(a),null,"");
            f.say(a,ModernFilteredChat.Kind.NORMAL,"解除後");
            assertEquals(List.of("已交付","解除後"),f.text(b));
            assertEquals(List.of(1L,2L),f.read(b).messages().stream().map(ModernFilteredChat.Message::id).toList());
            assertEquals(List.of("已交付"),before.messages().stream().map(ModernFilteredChat.Message::text).toList());
            assertThrows(UnsupportedOperationException.class,()->before.messages().clear());
            f.prefs(b,true,false,true);f.base.now.addAndGet(ModernChat.REFILL_NANOS);
            f.say(a,ModernFilteredChat.Kind.WORLD,"隱藏世界");
            f.prefs(b,true,true,true);assertEquals(List.of("已交付","解除後"),f.text(b));
        }
    }
    @Test void legacyNormalTrafficBridgesBothWaysButCannotBypassFilteredEnrollmentOrLoseCategory() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.base.player(42,2,0);var excluded=f.player(42,3,0);
            f.block(excluded,b);
            assertEquals(ModernChat.Status.OK,f.base.say(b,"舊版一般").status());
            assertEquals(List.of("舊版一般"),f.text(a));assertTrue(f.text(excluded).isEmpty());
            f.say(a,ModernFilteredChat.Kind.NORMAL,"新版一般");
            f.say(a,ModernFilteredChat.Kind.WORLD,"新版世界");f.say(a,ModernFilteredChat.Kind.TRADE,"新版交易");
            assertEquals(List.of("舊版一般","新版一般"),f.base.read(b).messages().stream().map(ModernChat.Message::text).toList());
            assertThrows(SecurityException.class,()->f.base.read(a));
            assertThrows(SecurityException.class,()->f.base.rooms.chatSend(a.owner(),a.scope(),0,
                    f.read(a).token().membership(),1,0,"繞過過濾"));
            assertThrows(SecurityException.class,()->f.base.rooms.enableFilteredChat(b.owner(),b.scope()));
            assertEquals(2,f.base.rooms.filteredChatOwnerCount());
        }
    }
    @Test void privateRoomAndChannelAdmissionNeverReplaysOldHistoryAndStaleTokensCannotRedirect() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);var lobby=f.player(7,3,0);var other=f.player(42,4,1);
            f.base.create(a,"secret");f.say(a,ModernFilteredChat.Kind.NORMAL,"加入前");
            f.base.join(b,a,"secret");assertTrue(f.text(b).isEmpty());
            f.say(a,ModernFilteredChat.Kind.WORLD,"同房世界");assertEquals(List.of("同房世界"),f.text(b));
            assertTrue(f.text(lobby).isEmpty());assertTrue(f.text(other).isEmpty());
            var old=f.read(b);f.base.leave(b);assertTrue(f.text(b).isEmpty());
            f.base.join(b,a,"secret");var next=f.read(b);
            assertTrue(next.token().membership()>old.token().membership());assertEquals(old.sequence(),next.sequence());
            var request=new ModernFilteredChat.Request(old.token(),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,1,old.cursor(),"過期");
            assertThrows(SecurityException.class,()->f.base.rooms.filteredChat(b.owner(),request,
                    new ModernFilteredChat.SpeechPolicy(old.token(),false,false,false,false)));
            assertEquals(next,f.read(b));assertTrue(f.text(lobby).isEmpty());
            f.base.rooms.enableFilteredChat(b.owner(),b.scope());assertEquals(next,f.read(b));
        }
    }
    @Test void sourceReentryClearsHistoryButPreservesConnectionSequenceAndSharedRate() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);
            for(int i=0;i<4;i++)f.say(a,ModernFilteredChat.Kind.NORMAL,"額度"+i);
            var before=f.read(a);f.base.rooms.leaveLobby(a.owner());
            var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(42,1),"月亮神","角色1",50,6,1);
            var scope=new ModernRooms.Scope(42,1,2,1);var p=new ModernChatTest.Player(a.owner(),selected,scope);
            f.base.rooms.enterLobby(a.owner(),selected,2,1);f.base.rooms.enableDirectChat(a.owner(),scope);
            f.base.rooms.enableFilteredChat(a.owner(),scope);
            assertEquals(4,f.read(p).sequence());assertTrue(f.text(p).isEmpty());
            assertEquals(ModernFilteredChat.Status.RATE_LIMITED,f.say(p,ModernFilteredChat.Kind.WORLD,"不能洗額度").status());
            assertThrows(SecurityException.class,()->f.read(a));
            f.base.now.addAndGet(ModernChat.REFILL_NANOS);
            assertEquals(ModernFilteredChat.Status.OK,f.say(p,ModernFilteredChat.Kind.WORLD,"新來源").status());
            assertEquals(6,f.read(p).sequence());assertEquals(1,f.read(p).cursor());
            assertNotEquals(before.token(),f.read(p).token());assertEquals(42,f.read(p).owner().key().sourceId());
        }
    }
    @Test void filteredAndPrivateTrafficShareBudgetWhileCommandsAndSilenceDoNotSpendIt() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);
            assertEquals(ModernFilteredChat.Status.COMMAND_REQUIRED,f.say(a,ModernFilteredChat.Kind.TRADE,".de 20",true,true,true,false).status());
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(a,ModernFilteredChat.Kind.NORMAL,"被禁言",true,false,false,true).status());
            for(int i=0;i<4;i++)assertEquals(ModernDirectChat.Status.OK,f.change(a,ModernDirectChat.Action.SEND,f.direct.address(b),null,"私訊"+i).status());
            assertEquals(ModernFilteredChat.Status.RATE_LIMITED,f.say(a,ModernFilteredChat.Kind.NORMAL,"公聊共用額度").status());
            assertTrue(f.text(b).isEmpty());assertEquals(4,f.privateRead(b).messages().size());
            f.base.now.addAndGet(ModernChat.REFILL_NANOS);
            assertEquals(ModernFilteredChat.Status.OK,f.say(a,ModernFilteredChat.Kind.NORMAL,"公聊").status());
            assertEquals(ModernDirectChat.Status.RATE_LIMITED,f.change(a,ModernDirectChat.Action.SEND,f.direct.address(b),null,"私訊仍限速").status());
        }
    }
    @Test void nativeSilenceMatrixUsesNamedForEveryoneTimedAndRoomOnlyForNonAdmins() {
        for(boolean named:List.of(false,true))for(boolean timed:List.of(false,true))
            for(boolean room:List.of(false,true))for(boolean admin:List.of(false,true))try(var f=new Fixture()) {
                var a=f.player(7,1,0);var b=f.player(42,1,0);
                var policy=new ModernFilteredChat.SpeechPolicy(f.read(a).token(),named,timed,room,admin);
                assertEquals(named,policy.blocksPrivate()); // Private adapter remains explicitly unwired.
                var result=f.say(a,ModernFilteredChat.Kind.NORMAL,"權威檢查",named,timed,room,admin);
                boolean blocked=named||!admin&&(timed||room);
                assertEquals(blocked?ModernFilteredChat.Status.SILENCED:ModernFilteredChat.Status.OK,result.status());
                assertEquals(blocked?0:1,f.text(b).size());
                assertEquals(blocked,result.receipt()==null);
                assertEquals(ModernFilteredChat.Status.OK,f.say(b,ModernFilteredChat.Kind.NORMAL,"同obj不同來源未禁言").status());
            }
    }
    @Test void missingStaleAuthorityForgedScopesReplayedSequenceAndFutureCursorCannotMutate() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,1,0);var before=f.read(a);
            var valid=new ModernFilteredChat.Request(before.token(),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,1,0,"測試");
            assertThrows(SecurityException.class,()->f.base.rooms.filteredChat(a.owner(),valid,null));
            var foreign=new ModernFilteredChat.SpeechPolicy(f.read(b).token(),false,false,false,true);
            assertThrows(SecurityException.class,()->f.base.rooms.filteredChat(a.owner(),valid,foreign));
            for(var token:List.of(new ModernFilteredChat.Token(b.scope(),0,before.token().membership()),
                    new ModernFilteredChat.Token(new ModernRooms.Scope(7,1,2,0),0,before.token().membership()),
                    new ModernFilteredChat.Token(new ModernRooms.Scope(7,1,1,1),0,before.token().membership()),
                    new ModernFilteredChat.Token(a.scope(),99,before.token().membership()),
                    new ModernFilteredChat.Token(a.scope(),0,before.token().membership()+1))) {
                var r=new ModernFilteredChat.Request(token,ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,1,0,"偽造");
                assertThrows(SecurityException.class,()->f.base.rooms.filteredChat(a.owner(),r,new ModernFilteredChat.SpeechPolicy(token,false,false,false,true)));
            }
            assertEquals(before,f.read(a));assertTrue(f.text(b).isEmpty());
            assertEquals(ModernFilteredChat.Status.OK,f.say(a,ModernFilteredChat.Kind.NORMAL,"有效").status());
            var saved=f.read(a);
            assertThrows(SecurityException.class,()->f.base.rooms.filteredChat(a.owner(),valid,new ModernFilteredChat.SpeechPolicy(before.token(),false,false,false,false)));
            var future=new ModernFilteredChat.Request(saved.token(),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,2,2,"超前");
            assertThrows(IllegalArgumentException.class,()->f.base.rooms.filteredChat(a.owner(),future,new ModernFilteredChat.SpeechPolicy(saved.token(),false,false,false,false)));
            assertEquals(saved,f.read(a));
        }
    }
    @Test void boundsRingGapAndSnapshotValidationDistinguishDroppedDeliveredMessagesFromFilteredOnes() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);f.prefs(b,true,false,true);
            for(int i=0;i<70;i++) {
                f.base.now.addAndGet(ModernChat.REFILL_NANOS);f.say(a,ModernFilteredChat.Kind.WORLD,"不收"+i);
                f.base.now.addAndGet(ModernChat.REFILL_NANOS);f.say(a,ModernFilteredChat.Kind.NORMAL,"收"+i);
            }
            var view=f.read(b);assertEquals(70,view.cursor());assertEquals(64,view.messages().size());assertTrue(view.truncated());
            assertEquals(7,view.messages().get(0).id());assertEquals(70,view.messages().get(63).id());
            var caughtUp=f.base.rooms.filteredChat(b.owner(),new ModernFilteredChat.Request(view.token(),ModernFilteredChat.Action.READ,
                    ModernFilteredChat.Kind.NORMAL,0,view.cursor(),""),null).snapshot();
            assertFalse(caughtUp.truncated());assertTrue(caughtUp.messages().isEmpty());
            assertThrows(IllegalArgumentException.class,()->new ModernFilteredChat.Snapshot(view.token(),view.owner(),0,70,true,false,
                    List.of(view.messages().get(0),view.messages().get(63))));
            assertThrows(IllegalArgumentException.class,()->new ModernFilteredChat.Request(view.token(),ModernFilteredChat.Action.SEND,
                    ModernFilteredChat.Kind.NORMAL,1,0,"\n"));
            assertFalse(view.toString().contains("角色"));
        }
    }
    @Test void concurrentBlockAndDeliveryAreOneAtomicDecisionAndReadNeverRewritesHistory() throws Exception {
        for(int i=0;i<20;i++)try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);var gate=new CountDownLatch(1);var workers=Executors.newFixedThreadPool(2);
            try {
                var send=workers.submit(()->{gate.await();return f.say(a,ModernFilteredChat.Kind.NORMAL,"競態");});
                var block=workers.submit(()->{gate.await();f.block(b,a);return true;});
                gate.countDown();assertEquals(ModernFilteredChat.Status.OK,send.get(2,TimeUnit.SECONDS).status());
                assertTrue(block.get(2,TimeUnit.SECONDS));var result=f.read(b);assertTrue(result.cursor()==0||result.cursor()==1);
                assertEquals(result,f.read(b));f.base.now.addAndGet(ModernChat.REFILL_NANOS);
                f.say(a,ModernFilteredChat.Kind.NORMAL,"拒收後");
                assertEquals(result,f.read(b));
            } finally { workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS)); }
        }
    }
    @Test void concurrentLeaveCannotRedirectRoomTrafficOrRetainOldRoomHistory() throws Exception {
        for(int i=0;i<20;i++)try(var f=new Fixture()) {
            var host=f.player(7,1,0);var guest=f.player(42,2,0);var lobby=f.player(7,3,0);
            f.base.create(host,"");f.base.join(guest,host,"");var before=f.read(guest);
            var request=new ModernFilteredChat.Request(before.token(),ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,1,0,"房間競態");
            var authority=new ModernFilteredChat.SpeechPolicy(before.token(),false,false,false,false);
            var gate=new CountDownLatch(1);var workers=Executors.newFixedThreadPool(2);
            try {
                var send=workers.submit(()->{gate.await();try{return f.base.rooms.filteredChat(guest.owner(),request,authority).status()==ModernFilteredChat.Status.OK;}
                    catch(SecurityException stale){return false;}});
                var leave=workers.submit(()->{gate.await();f.base.leave(guest);return true;});gate.countDown();
                boolean accepted=send.get(2,TimeUnit.SECONDS);assertTrue(leave.get(2,TimeUnit.SECONDS));
                assertEquals(accepted?1:0,f.text(host).size());assertTrue(f.text(guest).isEmpty());assertTrue(f.text(lobby).isEmpty());
                assertEquals(accepted?1:0,f.read(guest).sequence());
            } finally {workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }
    @Test void unicodeCanonicalBlockingUsesTheSameServerAuthorityAsPrivateMessages() {
        try(var f=new Fixture()) {
            var receiver=f.player(7,9,0);
            for(int source:List.of(7,42)) {
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),source==7?"太陽神":"月亮神","İD",80,6,1);
                var owner=f.base.presence.connect();
                var p=new ModernChatTest.Player(owner,selection,new ModernRooms.Scope(source,3333,1,0));
                f.base.players.add(p);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,f.base.presence.select(owner,selection));
                f.base.rooms.enterLobby(owner,selection,1,0);
                f.base.rooms.enableDirectChat(owner,p.scope());f.base.rooms.enableFilteredChat(owner,p.scope());
                f.change(receiver,ModernDirectChat.Action.BLOCK,new ModernDirectChat.Address(7," i\u0307d "),null,"");
                f.say(p,ModernFilteredChat.Kind.NORMAL,"來源"+source);
            }
            assertEquals(List.of("來源42"),f.text(receiver));assertEquals(1,f.read(receiver).cursor());
            assertEquals("i\u0307d",f.privateRead(receiver).blocked().get(0).name());
        }
    }
    private static Object field(Object target,String name) throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);
    }
    private static void counter(Object target,String name,long value) throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.setLong(target,value);
    }
    private static Object ledger(Fixture f,ModernChatTest.Player p) throws Exception {
        return ((java.util.Map<?,?>)field(field(f.base.rooms,"filteredChat"),"ledgers")).get(p.owner());
    }
    @Test void exhaustedRecipientAndLastLegalSequenceCannotWrapOrBreakOtherRecipients() throws Exception {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);var c=f.player(42,3,0);
            counter(ledger(f,b),"cursor",Long.MAX_VALUE);counter(ledger(f,a),"sequence",Long.MAX_VALUE-1);
            var sent=f.say(a,ModernFilteredChat.Kind.WORLD,"最後序號");
            assertEquals(ModernFilteredChat.Status.OK,sent.status());assertEquals(Long.MAX_VALUE,sent.receipt().sequence());
            assertFalse(sent.snapshot().available());assertTrue(f.text(b).isEmpty());assertEquals(List.of("最後序號"),f.text(c));
            var token=sent.snapshot().token();
            var replay=new ModernFilteredChat.Request(token,ModernFilteredChat.Action.SEND,ModernFilteredChat.Kind.NORMAL,Long.MAX_VALUE,1,"不回繞");
            assertThrows(SecurityException.class,()->f.base.rooms.filteredChat(a.owner(),replay,new ModernFilteredChat.SpeechPolicy(token,false,false,false,false)));
            assertEquals(ModernFilteredChat.Status.UNAVAILABLE,f.base.rooms.filteredChat(a.owner(),new ModernFilteredChat.Request(token,
                    ModernFilteredChat.Action.READ,ModernFilteredChat.Kind.NORMAL,0,1,""),null).status());
            assertEquals(1,f.text(c).size());assertFalse(f.read(b).available());
        }
    }
    @Test void saturatedLegacySinkDeniesNormalWithoutPartialBroadcastButTypedTrafficRemainsIndependent() throws Exception {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);
            var binding=((java.util.Map<?,?>)field(f.base.rooms,"bindings")).get(a.owner());
            var member=(ModernChat.Membership)field(binding,"chatMembership");counter(member.log(),"last",Long.MAX_VALUE);
            var denied=f.say(a,ModernFilteredChat.Kind.NORMAL,"不能部分廣播");
            assertEquals(ModernFilteredChat.Status.UNAVAILABLE,denied.status());assertNull(denied.receipt());
            assertEquals(1,denied.snapshot().sequence());assertTrue(f.text(a).isEmpty());assertTrue(f.text(b).isEmpty());
            assertEquals(ModernFilteredChat.Status.OK,f.say(a,ModernFilteredChat.Kind.WORLD,"獨立類型").status());
            assertEquals(List.of("獨立類型"),f.text(b));
        }
    }
}
