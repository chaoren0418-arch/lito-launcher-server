package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModernChatTest {
    record Player(TournamentPresence.Owner owner, TournamentPresence.Selection selection, ModernRooms.Scope scope) {}
    static final class Fixture implements AutoCloseable {
        final AtomicLong now = new AtomicLong();
        final TournamentPresence presence = new TournamentPresence();
        final ModernChat chat = new ModernChat(now::get);
        final ModernRooms rooms;
        Fixture(){this(new ModernSpeechAuthority(Map.of()));}
        Fixture(ModernSpeechAuthority speech){rooms=new ModernRooms(Set.of(),Set.of(),Map.of(),chat,speech);}
        final List<Player> players = new ArrayList<>();
        Player player(int source,long object,int channel) {
            var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,object),
                    source==7?"太陽神":"月亮神","角色"+object,channel==0?80:50,6,1);
            var owner=presence.connect();
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));
            var player=new Player(owner,selection,new ModernRooms.Scope(source,object,1,channel));
            rooms.enterLobby(owner,selection,1,channel); players.add(player); return player;
        }
        ModernRooms.View roomView(Player p) {
            return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view();
        }
        long room(Player p) {var joined=roomView(p).joined();return joined==null?0:joined.summary().id();}
        ModernChat.Snapshot read(Player p) {return rooms.chatRead(p.owner(),p.scope(),room(p),0,0);}
        ModernChat.Result say(Player p,String text) {
            var before=read(p);
            return rooms.chatSend(p.owner(),p.scope(),room(p),before.membership(),before.requestSequence()+1,before.cursor(),text);
        }
        ModernRooms.Summary create(Player p,String password) {
            var result=rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.CREATE,0,0,0,
                    new ModernRooms.Draft("測試房",password,4,1,564,0),""));
            assertEquals(ModernRooms.Status.OK,result.status());return result.view().joined().summary();
        }
        void join(Player p,Player host,String password) {
            var summary=roomView(host).joined().summary();
            assertEquals(ModernRooms.Status.OK,rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.JOIN,
                    summary.id(),summary.revision(),0,null,password)).status());
        }
        void leave(Player p) {
            var summary=roomView(p).joined().summary();
            assertEquals(ModernRooms.Status.OK,rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.LEAVE,
                    summary.id(),summary.revision(),0,null,"")).status());
        }
        public void close() {
            for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}
            assertEquals(0,rooms.chatOwnerCount());assertEquals(0,rooms.ownerCount());
            assertEquals(0,rooms.roomCount());assertEquals(0,presence.ownerCount());
        }
    }
    @Test void lobbyRoutesAcrossSourcesWithinOneChannelButNeverByObjectIdOrDisplayName() {
        try(var f=new Fixture()) {
            var sun=f.player(7,3333,0);var moon=f.player(42,3333,0);var elsewhere=f.player(7,4444,1);
            var roomsBefore=f.roomView(sun);
            assertEquals(ModernChat.Status.OK,f.say(sun,"你好，準備好了 👋").status());
            assertEquals(ModernChat.Status.OK,f.say(moon,"月亮神收到").status());
            var messages=f.read(sun).messages();assertEquals(messages,f.read(moon).messages());assertEquals(2,messages.size());
            assertEquals(sun.selection().key(),messages.get(0).sender());assertEquals(moon.selection().key(),messages.get(1).sender());
            assertEquals("角色3333(太陽神)",messages.get(0).displayName());assertEquals("角色3333(月亮神)",messages.get(1).displayName());
            assertTrue(f.read(elsewhere).messages().isEmpty());assertEquals(roomsBefore,f.roomView(sun));
            assertFalse(messages.toString().contains("你好"));assertFalse(f.read(sun).toString().contains("3333"));
        }
    }
    @Test void privateRoomAndLobbyAreDisjointAndLateJoinCannotReadEarlierPrivateMessages() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);var lobby=f.player(7,3,0);var other=f.player(42,4,0);
            var created=f.create(a,"secret");f.create(other,"");
            f.say(a,"入房前訊息");f.say(lobby,"大廳訊息");f.say(other,"另一房訊息");
            assertThrows(SecurityException.class,()->f.rooms.chatRead(b.owner(),b.scope(),created.id(),0,0));
            f.join(b,a,"secret");assertTrue(f.read(b).messages().isEmpty());
            f.say(a,"同房訊息");assertEquals(List.of("同房訊息"),f.read(b).messages().stream().map(ModernChat.Message::text).toList());
            assertEquals(List.of("大廳訊息"),f.read(lobby).messages().stream().map(ModernChat.Message::text).toList());
            assertEquals(List.of("另一房訊息"),f.read(other).messages().stream().map(ModernChat.Message::text).toList());
            f.leave(b);assertTrue(f.read(b).messages().isEmpty());
            f.say(lobby,"新的大廳訊息");assertEquals(List.of("新的大廳訊息"),f.read(b).messages().stream().map(ModernChat.Message::text).toList());
        }
    }
    @Test void leavingAndRejoiningSameRoomInvalidatesOldMembershipWithoutResettingRateOrSendSequence() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);f.create(a,"");f.join(b,a,"");
            for(int i=0;i<ModernChat.BURST;i++)assertEquals(ModernChat.Status.OK,f.say(b,"訊息"+i).status());
            var old=f.read(b);long room=f.room(b);f.leave(b);f.join(b,a,"");var next=f.read(b);
            assertTrue(next.membership()>old.membership());assertEquals(old.requestSequence(),next.requestSequence());assertTrue(next.messages().isEmpty());
            assertThrows(SecurityException.class,()->f.rooms.chatSend(b.owner(),b.scope(),room,old.membership(),old.requestSequence()+1,old.cursor(),"過期訊息"));
            assertThrows(SecurityException.class,()->f.rooms.chatRead(b.owner(),b.scope(),room,old.membership(),old.cursor()));
            assertEquals(ModernChat.Status.RATE_LIMITED,f.say(b,"不能靠重入洗速率").status());
            f.now.addAndGet(ModernChat.REFILL_NANOS);assertEquals(ModernChat.Status.OK,f.say(b,"一個補充額度").status());
            assertEquals(ModernChat.Status.RATE_LIMITED,f.say(b,"不會補滿").status());
        }
    }
    @Test void channelAndSourceReentryPreserveOwnerBudgetAndDisconnectReleasesLedgerEvenWithoutBinding() {
        try(var f=new Fixture()) {
            var p=f.player(7,1,0);for(int i=0;i<4;i++)f.say(p,"訊息"+i);
            var old=f.read(p);f.rooms.leaveLobby(p.owner());assertEquals(1,f.rooms.chatOwnerCount());
            var nextSelection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(42,1),"月亮神","角色1",50,6,1);
            var nextScope=new ModernRooms.Scope(42,1,2,1);f.rooms.enterLobby(p.owner(),nextSelection,2,1);
            var view=f.rooms.chatRead(p.owner(),nextScope,0,0,0);assertTrue(view.membership()>old.membership());
            assertThrows(SecurityException.class,()->f.rooms.chatRead(p.owner(),p.scope(),0,0,0));
            assertEquals(ModernChat.Status.RATE_LIMITED,f.rooms.chatSend(p.owner(),nextScope,0,view.membership(),5,0,"不跨來源洗速率").status());
            f.rooms.leaveLobby(p.owner());f.rooms.disconnect(p.owner());assertEquals(0,f.rooms.chatOwnerCount());
            assertThrows(SecurityException.class,()->f.rooms.chatRead(p.owner(),nextScope,0,0,0));
        }
    }
    @Test void sequenceAndCursorRejectionDoNotPublishOrConsumeAValidSend() {
        try(var f=new Fixture()) {
            var p=f.player(7,1,0);var view=f.read(p);
            assertThrows(SecurityException.class,()->f.rooms.chatSend(p.owner(),p.scope(),0,view.membership(),2,0,"skip"));
            assertThrows(IllegalArgumentException.class,()->f.rooms.chatSend(p.owner(),p.scope(),0,view.membership(),1,1,"future cursor"));
            assertEquals(view,f.read(p));assertEquals(ModernChat.Status.OK,f.say(p,"first").status());
            var saved=f.read(p);
            assertThrows(SecurityException.class,()->f.rooms.chatSend(p.owner(),p.scope(),0,view.membership(),1,0,"first"));
            assertEquals(saved,f.read(p));assertThrows(UnsupportedOperationException.class,()->saved.messages().clear());
        }
    }
    @Test void ringHasAnExplicitGapCursorAndLateMembersCannotRequestPreJoinHistory() {
        try(var f=new Fixture()) {
            var sender=f.player(7,1,0);var reader=f.player(42,2,0);
            for(int i=0;i<70;i++){f.now.addAndGet(ModernChat.REFILL_NANOS);f.say(sender,"訊息"+i);}
            var view=f.read(reader);assertEquals(70,view.cursor());assertTrue(view.truncated());assertEquals(64,view.messages().size());
            assertEquals(7,view.messages().get(0).id());
            var caughtUp=f.rooms.chatRead(reader.owner(),reader.scope(),0,view.membership(),view.cursor());
            assertFalse(caughtUp.truncated());assertTrue(caughtUp.messages().isEmpty());
            var late=f.player(7,3,0);assertTrue(f.read(late).messages().isEmpty());assertFalse(f.read(late).truncated());
            f.now.addAndGet(ModernChat.REFILL_NANOS);f.say(sender,"新訊息");assertEquals(1,f.read(late).messages().size());
        }
    }
    @Test void unicodeIsPreservedButControlBidiMalformedOversizeAndEmptyTextCannotBecomeMessages() {
        for(String value:List.of("", " \u00a0\u3000", "\u200d", "a\nb", "a\tb", "a\0b", "\u001b[1m", "\u202Eevil", "a\u2028b", "a\u2066b", "\uD800", "\uDC00", "x".repeat(257), "中".repeat(171), "😀".repeat(129)))
            assertThrows(IllegalArgumentException.class,()->ModernChat.validateText(value));
        assertThrows(IllegalArgumentException.class,()->ModernChat.validateText(null));
        for(String text:List.of("中文 IME", "🙂", "👩‍👩‍👧", "e\u0301", "x".repeat(256), "中".repeat(170), "😀".repeat(128), "<script>not markup</script>"))
            assertDoesNotThrow(()->ModernChat.validateText(text));
        try(var f=new Fixture()) {
            var p=f.player(7,1,0);var before=f.read(p);
            assertThrows(IllegalArgumentException.class,()->f.say(p,"\u202Ehidden"));assertEquals(before,f.read(p));
        }
    }
    @Test void gmTextIsNotBroadcastOrExecutedAndDenialsCarryAnAdvancedRequestSequence() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);
            for(String command:List.of(".de 20"," .DE 0",".de"))assertEquals(ModernChat.Status.COMMAND_REQUIRED,f.say(a,command).status());
            assertEquals(3,f.read(a).requestSequence());assertTrue(f.read(b).messages().isEmpty());assertEquals(0,f.rooms.roomCount());
            assertEquals(ModernChat.Status.OK,f.say(a,".delta 是普通文字").status());assertEquals(1,f.read(b).messages().size());
        }
    }
    @Test void clockRollbackDoesNotMintTokensAndNanoTimeWrapStillRefills() {
        for(boolean wrap:new boolean[]{false,true})try(var f=new Fixture()) {
            f.now.set(wrap?Long.MAX_VALUE-100:1000);var p=f.player(7,1,0);
            for(int i=0;i<4;i++)f.say(p,"burst");
            f.now.addAndGet(-1);assertEquals(ModernChat.Status.RATE_LIMITED,f.say(p,"rollback").status());
            f.now.addAndGet(ModernChat.REFILL_NANOS+1);assertEquals(ModernChat.Status.OK,f.say(p,"refill").status());
            assertEquals(ModernChat.Status.RATE_LIMITED,f.say(p,"single token").status());
        }
    }
    @Test void concurrentLeaveCannotRedirectAnAdmittedRoomSendIntoTheLobby() throws Exception {
        for(int iteration=0;iteration<20;iteration++)try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);var lobby=f.player(7,3,0);f.create(a,"");f.join(b,a,"");
            var before=f.read(b);long room=f.room(b);var gate=new CountDownLatch(1);var workers=Executors.newFixedThreadPool(2);
            try {
                var send=workers.submit(()->{gate.await();try{return f.rooms.chatSend(b.owner(),b.scope(),room,before.membership(),1,0,"race").status()==ModernChat.Status.OK;}catch(SecurityException stale){return false;}});
                var leave=workers.submit(()->{gate.await();f.leave(b);return true;});gate.countDown();
                boolean accepted=send.get(2,TimeUnit.SECONDS);assertTrue(leave.get(2,TimeUnit.SECONDS));
                assertTrue(f.read(lobby).messages().isEmpty());assertTrue(f.read(b).messages().isEmpty());
                assertEquals(accepted?1:0,f.read(a).messages().size());
            } finally {workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }
    @Test void chattingDoesNotUnreadyMembersCancelPreparationOrChangeTheFrozenRoster() {
        try(var f=new Fixture()) {
            var a=f.player(7,1,0);var b=f.player(42,2,0);f.create(a,"");f.join(b,a,"");
            for(var p:List.of(a,b)) {
                var room=f.roomView(p).joined().summary();
                assertEquals(ModernRooms.Status.OK,f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),
                        ModernRooms.Action.READY,room.id(),room.revision(),1,null,"")).status());
            }
            var room=f.roomView(a).joined().summary();assertTrue(room.readySatisfied());
            var prepared=f.rooms.prepareWorld(a.owner(),a.scope(),room.id(),room.revision());
            assertEquals(ModernRooms.Status.OK,prepared.result().status());var frozen=f.roomView(a);
            assertEquals(ModernChat.Status.OK,f.say(a,"倒數仍可對話").status());
            assertEquals(ModernChat.Status.OK,f.say(b,"同房收到").status());
            assertEquals(frozen,f.roomView(a));assertTrue(prepared.ticket().isLive());assertEquals(2,f.read(b).messages().size());
            f.rooms.disconnect(a.owner());assertFalse(prepared.ticket().isLive());assertEquals(1,f.rooms.chatOwnerCount());
        }
    }
    private static void setLong(Object target,String name,long value)throws Exception {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.setLong(target,value);
    }
    @Test void chatCounterExhaustionFailsOnlyChatWithoutWrappingOrBlockingRoomOperations() throws Exception {
        try(var f=new Fixture()) {
            var p=f.player(7,1,0);var ledger=f.chat.register(p.owner());
            setLong(ledger,"requestSequence",Long.MAX_VALUE-1);
            var finalSend=f.say(p,"最後一則");assertEquals(ModernChat.Status.OK,finalSend.status());
            assertFalse(finalSend.snapshot().available());assertEquals(Long.MAX_VALUE,finalSend.snapshot().requestSequence());
            assertThrows(SecurityException.class,()->f.rooms.chatSend(p.owner(),p.scope(),0,finalSend.snapshot().membership(),1,0,"wrap"));
            assertEquals(finalSend.snapshot(),f.read(p));
            setLong(ledger,"generation",Long.MAX_VALUE);f.create(p,"");
            var unavailable=f.read(p);assertFalse(unavailable.available());assertEquals(0,unavailable.membership());
            f.leave(p);assertFalse(f.read(p).available());assertEquals(0,f.rooms.roomCount());
        }
        var chat=new ModernChat(()->0);var presence=new TournamentPresence();var owner=presence.connect();
        var ledger=chat.register(owner);var log=new ModernChat.Log();var member=chat.join(ledger,log);
        setLong(log,"last",Long.MAX_VALUE);
        var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,1),"太陽神","角色",80,6,1);
        var result=chat.send(new ModernChat.Audience(0,0),ledger,member,member.generation(),1,0,selection,"不會溢位");
        assertEquals(ModernChat.Status.UNAVAILABLE,result.status());assertEquals(Long.MAX_VALUE,result.snapshot().cursor());
        assertTrue(result.snapshot().messages().isEmpty());chat.disconnect(owner);presence.disconnect(owner);
        assertEquals(0,chat.ownerCount());
    }
}
