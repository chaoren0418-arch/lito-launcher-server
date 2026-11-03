package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernModeratedDirectChatTest {
    static ModernDirectChatProtocol.Request request(ModernSpeechAuthorityTest.Fixture f,ModernChatTest.Player p,
            ModernDirectChat.Action action,long sequence) {
        var token=f.read(p).token();
        return new ModernDirectChatProtocol.Request(p.scope(),token.roomId(),token.membership(),sequence,0,action,
                action==ModernDirectChat.Action.SEND?new ModernDirectChatProtocol.Target(42,"角色3333"):null,
                action==ModernDirectChat.Action.PREFERENCES?new ModernDirectChat.Preferences(false,false,false):null,
                action==ModernDirectChat.Action.SEND?"私訊":"");
    }
    @Test void namedSilenceBlocksEvenAdministratorConsumesSequenceButNotRateOrDelivery() {
        try(var f=new ModernSpeechAuthorityTest.Fixture()) {
            var a=f.player(7,3333);var b=f.player(42,3333);f.mute(a,20,false);
            var admin=ModernSpeechAuthorityTest.principal("shown","SunGM","MoonGM");
            var send=request(f,a,ModernDirectChat.Action.SEND,1);
            var denied=f.rooms.moderatedDirectChat(a.owner(),send,admin);
            assertEquals(ModernDirectChat.Status.SILENCED,denied.status());assertEquals(1,denied.snapshot().requestSequence());
            assertTrue(denied.snapshot().messages().isEmpty());
            assertTrue(f.rooms.moderatedDirectChat(b.owner(),request(f,b,ModernDirectChat.Action.READ,0),admin).snapshot().messages().isEmpty());
            assertThrows(SecurityException.class,()->f.rooms.moderatedDirectChat(a.owner(),send,admin));
            assertEquals(ModernDirectChat.Status.OK,f.rooms.moderatedDirectChat(a.owner(),request(f,a,ModernDirectChat.Action.PREFERENCES,2),admin).status());
            f.mute(a,0,false);
            for(int i=3;i<=6;i++)assertEquals(ModernDirectChat.Status.OK,f.rooms.moderatedDirectChat(a.owner(),request(f,a,ModernDirectChat.Action.SEND,i),admin).status());
            assertEquals(ModernDirectChat.Status.RATE_LIMITED,f.rooms.moderatedDirectChat(a.owner(),request(f,a,ModernDirectChat.Action.SEND,7),admin).status());
            assertEquals(4,f.rooms.moderatedDirectChat(b.owner(),request(f,b,ModernDirectChat.Action.READ,0),admin).snapshot().messages().size());
        }
    }
    @Test void timedAndRoomSilenceDoNotBlockPrivateAndLegacyCannotBypassQualifiedPath() {
        try(var f=new ModernSpeechAuthorityTest.Fixture()) {
            var a=f.player(7,3333);f.player(42,3333);f.create(a);f.mute(a,20,true);
            f.rooms.silenceRoom(a.owner(),f.read(a).token(),true);
            var send=request(f,a,ModernDirectChat.Action.SEND,1);
            assertEquals(ModernDirectChat.Status.OK,f.rooms.moderatedDirectChat(a.owner(),send,ModernSpeechAuthorityTest.USER).status());
            assertEquals(ModernFilteredChat.Status.SILENCED,f.say(a,ModernSpeechAuthorityTest.USER).status());
            for(var action:List.of(ModernDirectChat.Action.READ,ModernDirectChat.Action.SEND)) {
                var old=request(f,a,action,action==ModernDirectChat.Action.READ?0:2);
                assertThrows(SecurityException.class,()->f.rooms.directChat(a.owner(),old.scope(),old.roomId(),old.membership(),old.command()));
            }
            f.mute(a,20,false);
            assertEquals(ModernDirectChat.Status.SILENCED,f.rooms.moderatedDirectChat(a.owner(),request(f,a,ModernDirectChat.Action.SEND,2),ModernSpeechAuthorityTest.USER).status());
        }
    }
    @Test void wrongGrantAndRetiredAdmissionCannotConsumePrivateSequence() {
        try(var f=new ModernSpeechAuthorityTest.Fixture()) {
            var a=f.player(7,3333);f.player(42,3333);var send=request(f,a,ModernDirectChat.Action.SEND,1);
            assertThrows(SecurityException.class,()->f.rooms.moderatedDirectChat(a.owner(),send,null));
            var other=new ModernAccountAuthentication.Principal("shown",java.util.Set.of(new ModernAccountAuthentication.Source(42,"月亮神")));
            assertThrows(SecurityException.class,()->f.rooms.moderatedDirectChat(a.owner(),send,other));
            f.create(a);
            assertThrows(SecurityException.class,()->f.rooms.moderatedDirectChat(a.owner(),send,ModernSpeechAuthorityTest.USER));
            assertEquals(ModernDirectChat.Status.OK,f.rooms.moderatedDirectChat(a.owner(),request(f,a,ModernDirectChat.Action.SEND,1),ModernSpeechAuthorityTest.USER).status());
        }
    }
    @Test void explicitKindsVersionAndSilencedActionAreStrictlyIsolated() throws Exception {
        var r=ModernDirectChatProtocolTest.read();
        var send=new ModernDirectChatProtocol.Request(r.scope(),0,1,1,0,ModernDirectChat.Action.SEND,new ModernDirectChatProtocol.Target(42,"Moon Mage"),null,"私訊");
        var denied=new ModernDirectChat.Result(ModernDirectChat.Status.SILENCED,ModernDirectChatProtocolTest.result(send,List.of(),List.of()).snapshot());
        var message=new ModernApplicationProtocol.ModeratedDirectResult(1,send,denied);
        var bytes=ModernApplicationProtocol.encode(message,33);
        assertEquals(57,ByteBuffer.wrap(bytes).getShort(12));assertEquals(message,ModernApplicationProtocol.decode(bytes,33));
        assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),33));
        var request=new ModernApplicationProtocol.ModeratedDirectRequest(1,r);
        var requestBytes=ModernApplicationProtocol.encode(request,33);assertEquals(56,ByteBuffer.wrap(requestBytes).getShort(12));
        assertEquals(request,ModernApplicationProtocol.decode(requestBytes,33));
        for(int v=1;v<33;v++){final int version=v;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,version));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),version));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(request,version));}
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.DirectChatResult(1,send,denied),33));
        var old=bytes.clone();ByteBuffer.wrap(old).putShort(12,(short)53);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(old,33));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernDirectChatProtocol.validate(r,new ModernDirectChat.Result(ModernDirectChat.Status.SILENCED,ModernDirectChatProtocolTest.result(r,List.of(),List.of()).snapshot()),true));
        for(int size=0;size<bytes.length;size++){var truncated=java.util.Arrays.copyOf(bytes,size);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,33));}
        var header=java.util.Arrays.copyOf(bytes,16);ByteBuffer.wrap(header).putInt(4,ModernDirectChatProtocol.MAX_FRAME_BYTES+1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(header),33));
    }
}
