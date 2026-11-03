package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernLargeMapsTest {
    static final int BASE=7|ModernSessionProtocol.CAP_CHARACTER_DATA|ModernSessionProtocol.CAP_ROOMS|ModernSessionProtocol.CAP_WORLD|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
    static ModernSessionBoundary.Negotiated peer(boolean enabled){return new ModernSessionBoundary.Negotiated(1,34,1,BASE|(enabled?ModernSessionProtocol.CAP_LARGE_MAPS:0));}
    @Test void independentOptInNegotiatesDowngradesAndPreservesRequiredDependencies()throws Exception {
        var hello=new ModernSessionProtocol.Hello(1,1,34,BASE|ModernSessionProtocol.CAP_LARGE_MAPS,7,1);
        for(int maximum:List.of(19,33,34)) {
            var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(BASE,7,maximum).withLargeMaps());
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(hello)));
            assertEquals(maximum,ack.version());assertEquals(maximum==34,(ack.capabilities()&ModernSessionProtocol.CAP_LARGE_MAPS)!=0);
        }
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,33,hello.supportedCapabilities(),7,1)));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(7|ModernSessionProtocol.CAP_LARGE_MAPS,7));
        assertFalse(ModernLargeMaps.negotiated(null));assertFalse(ModernLargeMaps.negotiated(peer(false)));assertTrue(ModernLargeMaps.negotiated(peer(true)));
        assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_LARGE_MAPS);
    }
    static ModernWorldProtocol.Frame frame(int count) {
        var base=ModernWorldProtocolTest.frame();var first=base.manifest().sectors().get(0);var sectors=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int i=0;i<count;i++)sectors.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(first.key().x(),first.key().y()+i),49170,first.rawSha256()));
        return new ModernWorldProtocol.Frame(base.matchId(),base.generation(),base.revision(),base.phase(),base.abortReason(),base.room(),new ModernWorldMap.Manifest(514,sectors,base.manifest().digest()),base.actors());
    }
    @Test void oldWireCountRemainsSixteenAndPeerCannotReceiveUnnegotiatedLargeFrames()throws Exception {
        for(int count:List.of(16,17,64)) {
            var frame=frame(count);var reply=new ModernApplicationProtocol.WorldResult(1,ModernWorldProtocolTest.read(),new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,frame));
            assertEquals(count<=16,ModernLargeMaps.supported(frame.manifest(),peer(false)));
            assertTrue(ModernLargeMaps.supported(frame.manifest(),peer(true)));
            byte[] encoded=ModernApplicationProtocol.encode(reply,34);
            assertEquals(reply,ModernApplicationProtocol.decode(encoded,34));
            if(count>16) {
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(reply,33));
                assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(reply,peer(false)));
            }
        }
        assertThrows(IllegalArgumentException.class,()->frame(65));
    }
    @Test void roomOptionsCreateJoinAndMapChangesRespectEveryParticipantsCapability() {
        var f=new ModernRoomsTest.Fixture();var large=new ModernWorldBinding.Key(1,516);f.rooms.configureLargeWorlds(Set.of(large));
        var modern=f.player(7,1,30,0);var legacy=f.player(42,2,30,0);
        f.rooms.enableLargeMaps(modern.owner(),modern.scope());
        assertFalse(f.view(legacy).options().stream().filter(o->o.mode()==1&&o.mapId()==516).findFirst().orElseThrow().worldAvailable());
        var denied=f.create(legacy,ModernRoomsTest.draft(2,1,516,0));assertEquals(ModernRooms.Status.DENIED,denied.status());assertNull(f.view(legacy).joined());
        ModernRoomsTest.accepted(f.create(modern,ModernRoomsTest.draft(2,1,516,0)));
        var room=f.view(modern).joined().summary();assertEquals(ModernRooms.Status.DENIED,f.join(legacy,room,"").status());
        ModernRoomsTest.accepted(f.act(modern,ModernRooms.Action.LEAVE,0));
        ModernRoomsTest.accepted(f.create(modern,ModernRoomsTest.draft(2,1,514,0)));ModernRoomsTest.accepted(f.join(legacy,f.view(modern).joined().summary(),""));
        room=f.view(modern).joined().summary();
        var changed=f.rooms.execute(modern.owner(),new ModernRooms.Command(modern.scope(),ModernRooms.Action.CONFIGURE,room.id(),room.revision(),0,ModernRoomsTest.draft(2,1,516,0),""));
        assertEquals(ModernRooms.Status.DENIED,changed.status());assertEquals(514,f.view(modern).joined().summary().config().mapId());
        for(var p:List.of(modern,legacy)){f.rooms.disconnect(p.owner());f.presence.disconnect(p.owner());}
        assertEquals(0,f.rooms.memberCount());
    }
    @Test void mixedCapabilityRandomMatchmakingSelectsOnlyACommonLoadableMap() {
        var large=new ModernWorldBinding.Key(1,516);var small=new ModernWorldBinding.Key(1,531);
        try(var f=new ModernMatchmakingTest.Fixture(Set.of(large,small),java.util.Map.of())) {
            f.rooms.configureLargeWorlds(Set.of(large));var modern=f.player();var old=f.player();
            f.rooms.enableLargeMaps(modern.owner(),modern.scope());
            assertTrue(f.view(modern).options().stream().filter(o->o.mode()==1&&o.mapId()==516).findFirst().orElseThrow().worldAvailable());
            assertEquals(ModernRooms.Status.DENIED,f.start(old,new ModernMatchmaking.Criteria(1,516,2),0,0).rooms().status());
            var c=new ModernMatchmaking.Criteria(1,0,2);var first=f.start(modern,c,0,0);f.start(old,c,0,0);
            var joined=f.read(modern,first.ticketId());assertEquals(ModernMatchmaking.State.MATCHED,joined.state());
            assertEquals(531,joined.rooms().view().joined().summary().config().mapId());
            assertEquals(531,f.view(old).joined().summary().config().mapId());
        }
    }
}
