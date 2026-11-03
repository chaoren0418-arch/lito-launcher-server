package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModernSpeechAuthorityTest {
    static Map<Integer,ModernSpeechAuthority.Source> sources() {
        return Map.of(7,new ModernSpeechAuthority.Source("太陽神",Set.of("SunGM")),
                42,new ModernSpeechAuthority.Source("月亮神",Set.of("MoonGM")));
    }
    static ModernAccountAuthentication.Principal principal(String display,String sun,String moon) {
        return new ModernAccountAuthentication.Principal(display,Set.of(
                new ModernAccountAuthentication.Source(7,"太陽神"),new ModernAccountAuthentication.Source(42,"月亮神")),Map.of(7,sun,42,moon));
    }
    static final ModernAccountAuthentication.Principal USER=principal("SunGM","regular","regular");
    static final class Fixture implements AutoCloseable {
        final AtomicLong now=new AtomicLong();
        final TournamentPresence presence=new TournamentPresence();
        final ModernSpeechAuthority speech=new ModernSpeechAuthority(sources(),now::get);
        final ModernRooms rooms=new ModernRooms(Set.of(),Set.of(),Map.of(),new ModernChat(now::get),speech);
        final List<TournamentPresence.Owner> owners=new ArrayList<>();
        ModernChatTest.Player player(int source,long id) {
            var owner=presence.connect();owners.add(owner);return enter(owner,source,id,1,0);
        }
        ModernChatTest.Player enter(TournamentPresence.Owner owner,int source,long id,long epoch,int channel) {
            var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,id),
                    source==7?"太陽神":"月亮神","角色"+id,80,6,1);
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));
            var p=new ModernChatTest.Player(owner,selection,new ModernRooms.Scope(source,id,epoch,channel));
            rooms.enterLobby(owner,selection,epoch,channel);rooms.enableDirectChat(owner,p.scope());rooms.enableFilteredChat(owner,p.scope());return p;
        }
        ModernRooms.View view(ModernChatTest.Player p) {
            return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view();
        }
        ModernFilteredChat.Snapshot read(ModernChatTest.Player p) {
            var joined=view(p).joined();long room=joined==null?0:joined.summary().id();
            return rooms.filteredChatAuthorized(p.owner(),new ModernFilteredChat.Request(new ModernFilteredChat.Token(p.scope(),room,0),
                    ModernFilteredChat.Action.READ,ModernFilteredChat.Kind.NORMAL,0,0,""),USER).snapshot();
        }
        ModernFilteredChat.Request request(ModernChatTest.Player p,String text) {
            var s=read(p);return new ModernFilteredChat.Request(s.token(),ModernFilteredChat.Action.SEND,
                    ModernFilteredChat.Kind.NORMAL,s.sequence()+1,s.cursor(),text);
        }
        ModernFilteredChat.Result say(ModernChatTest.Player p,ModernAccountAuthentication.Principal principal) {
            return rooms.filteredChatAuthorized(p.owner(),request(p,"訊息"),principal);
        }
        void mute(ModernChatTest.Player p,int seconds,boolean timed) {rooms.silenceCharacter(p.owner(),read(p).token(),seconds,timed);}
        void create(ModernChatTest.Player p) {
            assertEquals(ModernRooms.Status.OK,rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.CREATE,0,0,0,
                    new ModernRooms.Draft("房間","",4,1,564,0),"")).status());
        }
        void leaveRoom(ModernChatTest.Player p) {
            var s=view(p).joined().summary();
            assertEquals(ModernRooms.Status.OK,rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.LEAVE,s.id(),s.revision(),0,null,"")).status());
        }
        public void close() {
            for(var owner:owners){rooms.disconnect(owner);presence.disconnect(owner);}
            assertEquals(0,rooms.ownerCount());assertEquals(0,rooms.filteredChatOwnerCount());assertEquals(0,rooms.speechOwnerCount());assertEquals(0,presence.ownerCount());
        }
    }
    @Test void onlyCurrentSourcesCanonicalAccountCanBypassTimedSilence() {
        try(var f=new Fixture()) {
            var sun=f.player(7,3333);var moon=f.player(42,3333);f.mute(sun,20,true);f.mute(moon,20,true);
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(sun,USER).status()); // Display SunGM is not authority.
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(sun,principal("shown","MoonGM","SunGM")).status());
            assertEquals(ModernFilteredChat.Status.OK,f.say(sun,principal("shown","SUNGm","regular")).status());
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(moon,principal("shown","SunGM","regular")).status());
            assertEquals(ModernFilteredChat.Status.OK,f.say(moon,principal("shown","regular","MOONGM")).status());
        }
    }
    @Test void missingOrWrongSourceAuthorityFailsBeforeSequenceRateAndPublication() {
        try(var f=new Fixture()) {
            var p=f.player(7,1);var request=f.request(p,"secret");var before=f.read(p);
            var other=new ModernAccountAuthentication.Principal("SunGM",Set.of(new ModernAccountAuthentication.Source(42,"月亮神")));
            var renamed=new ModernAccountAuthentication.Principal("SunGM",Set.of(new ModernAccountAuthentication.Source(7,"偽來源")));
            for(var grant:new ModernAccountAuthentication.Principal[]{null,other,renamed})
                assertThrows(SecurityException.class,()->f.rooms.filteredChatAuthorized(p.owner(),request,grant));
            assertEquals(before,f.read(p));
            assertEquals(ModernFilteredChat.Status.OK,f.rooms.filteredChatAuthorized(p.owner(),request,USER).status());
            for(int i=0;i<3;i++)assertEquals(ModernFilteredChat.Status.OK,f.say(p,USER).status());
            assertEquals(ModernFilteredChat.Status.RATE_LIMITED,f.say(p,USER).status());
        }
    }
    @Test void unconfiguredAuthorityNeverDefaultsToPermission() {
        try(var f=new Fixture()) {
            var p=f.player(7,1);var empty=new ModernSpeechAuthority(Map.of(),f.now::get);
            assertThrows(SecurityException.class,()->empty.policy(p.owner(),USER,p.selection(),f.read(p).token(),false));
            assertEquals(0,empty.ownerCount());
        }
    }
    @Test void namedSilenceIncludesAdministratorsAndIsIsolatedBySourceAndObject() {
        try(var f=new Fixture()) {
            var a=f.player(7,3333);var b=f.player(42,3333);var c=f.player(7,2);
            f.mute(a,20,false);
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(a,principal("shown","SunGM","MoonGM")).status());
            assertEquals(ModernFilteredChat.Status.OK,f.say(b,USER).status());assertEquals(ModernFilteredChat.Status.OK,f.say(c,USER).status());
            var policy=f.speech.policy(a.owner(),USER,a.selection(),f.read(a).token(),false);assertTrue(policy.blocksPrivate());
            f.mute(a,0,false);f.mute(a,20,true);
            assertFalse(f.speech.policy(a.owner(),USER,a.selection(),f.read(a).token(),false).blocksPrivate());
        }
    }
    @Test void roomSilenceFollowsRoomAndNotItsOriginalOwnerOrLobby() {
        try(var f=new Fixture()) {
            var a=f.player(7,1);var b=f.player(42,2);f.create(a);
            var summary=f.view(a).joined().summary();
            assertEquals(ModernRooms.Status.OK,f.rooms.execute(b.owner(),new ModernRooms.Command(b.scope(),ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"")).status());
            f.rooms.silenceRoom(a.owner(),f.read(a).token(),true);
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(a,USER).status());
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(b,USER).status());
            assertEquals(ModernFilteredChat.Status.OK,f.say(a,principal("shown","SunGM","regular")).status());
            f.leaveRoom(a);assertEquals(ModernFilteredChat.Status.OK,f.say(a,USER).status());
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(b,USER).status());
            f.leaveRoom(b);f.create(b);assertEquals(ModernFilteredChat.Status.OK,f.say(b,USER).status());
            assertThrows(SecurityException.class,()->f.rooms.silenceRoom(a.owner(),f.read(a).token(),true));
        }
    }
    @Test void staleAdmissionsCannotMuteOrSendAfterRoomTransition() {
        try(var f=new Fixture()) {
            var p=f.player(7,1);var stale=f.request(p,"old");f.create(p);var before=f.read(p);
            assertThrows(SecurityException.class,()->f.rooms.silenceCharacter(p.owner(),stale.token(),20,false));
            assertThrows(SecurityException.class,()->f.rooms.silenceRoom(p.owner(),stale.token(),true));
            assertThrows(SecurityException.class,()->f.rooms.filteredChatAuthorized(p.owner(),stale,USER));
            assertEquals(before,f.read(p));assertEquals(0,f.rooms.speechOwnerCount());
        }
    }
    @Test void roleAndChannelSwitchCannotEraseActiveSilenceButDisconnectDoes() {
        try(var f=new Fixture()) {
            var p=f.player(7,3333);f.mute(p,20,false);f.rooms.leaveLobby(p.owner());
            var other=f.enter(p.owner(),42,3333,2,1);assertEquals(ModernFilteredChat.Status.OK,f.say(other,USER).status());
            f.rooms.leaveLobby(p.owner());var back=f.enter(p.owner(),7,3333,3,1);
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(back,USER).status());
            f.rooms.disconnect(back.owner());assertEquals(0,f.rooms.speechOwnerCount());
        }
    }
    @Test void exactExpiryAndIndependentClearingAlsoWorkAcrossSignedClockWrap() {
        for(long start:new long[]{0,Long.MAX_VALUE-500_000_000L,-1_000_000_000L})try(var f=new Fixture()) {
            f.now.set(start);var p=f.player(7,1);f.mute(p,1,false);f.mute(p,2,true);
            f.now.set(start+999_999_999L);assertTrue(f.speech.policy(p.owner(),USER,p.selection(),f.read(p).token(),false).namedSilence());
            f.now.set(start+1_000_000_000L);var at=f.speech.policy(p.owner(),USER,p.selection(),f.read(p).token(),false);
            assertFalse(at.namedSilence());assertTrue(at.timedSilence());
            f.now.set(start+2_000_000_000L);assertFalse(f.speech.policy(p.owner(),USER,p.selection(),f.read(p).token(),false).blocksPublic());
            assertEquals(0,f.rooms.speechOwnerCount());f.mute(p,2,false);f.mute(p,2,true);f.mute(p,0,true);
            assertTrue(f.speech.policy(p.owner(),USER,p.selection(),f.read(p).token(),false).namedSilence());
        }
    }
    @Test void invalidDurationAndFullStateFailWithoutDiscardingExistingRestrictions() {
        try(var f=new Fixture()) {
            var p=f.player(7,1);f.mute(p,ModernSpeechAuthority.MAX_SECONDS,false);
            for(int seconds:new int[]{-1,ModernSpeechAuthority.MAX_SECONDS+1})assertThrows(IllegalArgumentException.class,()->f.mute(p,seconds,false));
            for(int i=2;i<=64;i++)f.speech.silence(p.owner(),new TournamentPresence.CharacterKey(7,i),1,false);
            assertThrows(IllegalStateException.class,()->f.speech.silence(p.owner(),new TournamentPresence.CharacterKey(7,65),1,false));
            assertTrue(f.speech.policy(p.owner(),USER,p.selection(),f.read(p).token(),false).namedSilence());
            f.now.addAndGet(TimeUnit.SECONDS.toNanos(1));f.speech.silence(p.owner(),new TournamentPresence.CharacterKey(7,65),1,false);
            f.mute(p,0,false);assertFalse(f.speech.policy(p.owner(),USER,p.selection(),f.read(p).token(),false).blocksPublic());
        }
    }
    @Test void reservedCommandsDoNotExecuteAndSilencedAttemptsDoNotConsumeRateBudget() {
        try(var f=new Fixture()) {
            var p=f.player(7,1);f.mute(p,20,false);
            assertEquals(ModernFilteredChat.Status.COMMAND_REQUIRED,f.rooms.filteredChatAuthorized(p.owner(),f.request(p,".de 20"),USER).status());
            for(int i=0;i<5;i++)assertEquals(ModernFilteredChat.Status.SILENCED,f.say(p,USER).status());
            assertEquals(0,f.read(p).cursor());f.mute(p,0,false);
            for(int i=0;i<4;i++)assertEquals(ModernFilteredChat.Status.OK,f.say(p,USER).status());
            assertEquals(ModernFilteredChat.Status.RATE_LIMITED,f.say(p,USER).status());
        }
    }
    @Test void administratorListsAreImmutableBoundedNormalizedAndRedacted() {
        assertEquals(Set.of("sungm","管理員"),ModernSpeechAuthority.parseAdministrators(" SunGM , 管理員 "));
        assertEquals(Set.of(),ModernSpeechAuthority.parseAdministrators(""));
        for(String value:List.of(" ","a,","a,,b","a,A","a\u0001","界".repeat(22),"İ".repeat(22),"\ud800",String.join(",",java.util.stream.IntStream.range(0,65).mapToObj(i->"a"+i).toList())))
            assertThrows(IllegalArgumentException.class,()->ModernSpeechAuthority.parseAdministrators(value));
        var source=new ModernSpeechAuthority.Source("太陽神",Set.of("SunGM"));
        assertThrows(UnsupportedOperationException.class,()->source.administrators().clear());assertFalse(source.toString().contains("sungm"));
    }
    @Test void moderationAndSendAreOneAtomicAuthorityDecision() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for(int attempt=0;attempt<20;attempt++)try(var f=new Fixture()) {
                var p=f.player(7,1);var request=f.request(p,"race");var start=new java.util.concurrent.CountDownLatch(1);
                var muted=executor.submit(()->{start.await();f.rooms.silenceCharacter(p.owner(),request.token(),20,false);return true;});
                var sent=executor.submit(()->{start.await();return f.rooms.filteredChatAuthorized(p.owner(),request,USER);});
                start.countDown();assertTrue(muted.get(5,TimeUnit.SECONDS));var result=sent.get(5,TimeUnit.SECONDS);
                assertTrue(result.status()==ModernFilteredChat.Status.OK||result.status()==ModernFilteredChat.Status.SILENCED);
                assertEquals(result.status()==ModernFilteredChat.Status.OK?1:0,f.read(p).cursor());
                assertEquals(result.status()==ModernFilteredChat.Status.OK,result.receipt()!=null);
                assertEquals(ModernFilteredChat.Status.SILENCED,f.say(p,USER).status());
                assertEquals(2,f.read(p).sequence());
            }
        } finally {executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
    }
}
