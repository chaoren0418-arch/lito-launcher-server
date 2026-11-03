package local.lito381.server;

import static local.lito381.server.ModernRooms.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernRoomBotsTest {
    static final class Fixture {
        final TournamentPresence presence=new TournamentPresence();
        final ModernRooms rooms=new ModernRooms(Set.of(new ModernWorldBinding.Key(1,514)),Set.of(new ModernWorldBinding.Key(1,514)));
        final TournamentPresence.Owner owner=presence.connect();
        final Scope scope=new Scope(7,1,1,0);
        final TournamentPresence.Selection selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,1),"Sun","GM",80,6,1);
        Fixture(int capacity) {
            presence.select(owner,selection);rooms.enterLobby(owner,selection,1,0,true);
            accepted(rooms.execute(owner,new Command(scope,Action.CREATE,0,0,0,new Draft("test","",capacity,1,514,0),"")));
        }
        View view(){return rooms.execute(owner,new Command(scope,Action.READ,0,0,0,null,"")).view();}
        ModernGmCommands.Snapshot grant(){return new ModernGmCommands.Snapshot(7,"owner",new ModernApplicationProtocol.CharacterRow(1,"GM",6,1,80),200,false,false,
                new ModernGmCommands.Definition("de",100,"L1SpawnDe",false));}
        BotResult spawn(int count){var s=view().joined().summary();return rooms.spawnBots(owner,scope,s.id(),s.revision(),count,grant());}
        void ready(){var s=view().joined().summary();accepted(rooms.execute(owner,new Command(scope,Action.READY,s.id(),s.revision(),1,null,"")));}
    }
    static void accepted(Result result){assertEquals(Status.OK,result.status());}

    @Test void twentyAddsClipToCapacityAutoBalanceReadyAndKeepSourceWithoutHumanIdentityCollision() {
        var f=new Fixture(10);var before=f.view();var result=f.spawn(20);accepted(result.result());
        assertEquals(9,result.added());assertEquals(0,result.removed());var room=result.result().view().joined();
        assertEquals(10,room.members().size());assertEquals(5,room.members().stream().filter(m->m.team()==0).count());
        assertEquals(9,room.members().stream().filter(Member::ready).count());assertFalse(room.summary().readySatisfied());
        assertEquals(before.joined().summary().revision()+1,room.summary().revision());
        assertEquals(1,f.rooms.ownerCount());assertEquals(10,f.rooms.memberCount());
        assertEquals(ModernParticipantId.human(7,1),room.members().get(0).identity());
        assertEquals(ModernParticipantId.bot(7,1),room.members().get(1).identity());
        for(var bot:room.members().subList(1,10)){assertEquals("Sun",bot.sourceName());assertTrue(bot.ready());assertEquals(80,bot.level());assertThrows(IllegalStateException.class,bot::key);}
        assertEquals(0,f.spawn(20).added());assertEquals(room,f.view().joined());
        f.ready();var snapshot=f.view().joined();assertTrue(snapshot.summary().readySatisfied());
        var preparation=f.rooms.prepareWorld(f.owner,f.scope,snapshot.summary().id(),snapshot.summary().revision());
        assertNotNull(preparation.ticket());assertEquals(1,preparation.ticket().participants().size());assertEquals(9,preparation.ticket().bots().size());
        assertEquals(Reason.INVALID_STATE,f.spawn(0).result().reason());
    }
    @Test void clearIsAtomicIdempotentAndNewBotIdentifiersAreNeverReused() {
        var f=new Fixture(4);f.spawn(20);long revision=f.view().joined().summary().revision();
        assertEquals(3,f.spawn(0).removed());assertEquals(1,f.rooms.memberCount());
        assertEquals(revision+1,f.view().joined().summary().revision());assertEquals(0,f.spawn(0).removed());
        assertEquals(revision+1,f.view().joined().summary().revision());f.spawn(1);
        assertEquals(4,f.view().joined().members().get(1).identity().id());
        f.rooms.disconnect(f.owner);assertEquals(0,f.rooms.roomCount());assertEquals(0,f.rooms.memberCount());assertEquals(0,f.rooms.ownerCount());
    }
    @Test void unauthorizedStaleSourceAndRevisionNeverMutateRoom() {
        var f=new Fixture(4);var before=f.view();var s=before.joined().summary();
        assertEquals(Reason.GM_NOT_AUTHORIZED,f.rooms.spawnBots(f.owner,f.scope,s.id(),s.revision(),2,null).result().reason());
        assertEquals(Reason.REVISION_MISMATCH,f.rooms.spawnBots(f.owner,f.scope,s.id(),s.revision()+1,2,f.grant()).result().reason());
        var wrong=new ModernGmCommands.Snapshot(42,"owner",f.grant().character(),200,false,false,f.grant().definition());
        assertThrows(SecurityException.class,()->f.rooms.spawnBots(f.owner,f.scope,s.id(),s.revision(),2,wrong));
        assertEquals(before,f.view());
    }
    @Test void oldProtocolCannotReceiveBotsAndBlocksSpawningWhileItIsInRoom() {
        var f=new Fixture(4);var second=f.presence.connect();var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(42,2),"Moon","player",80,0,0);
        f.presence.select(second,selection);f.rooms.enterLobby(second,selection,1,0);var scope=new Scope(42,2,1,0);
        f.spawn(1);var s=f.view().joined().summary();
        assertEquals(Reason.INVALID_STATE,f.rooms.execute(second,new Command(scope,Action.JOIN,s.id(),s.revision(),0,null,"")).reason());
        f.spawn(0);s=f.view().joined().summary();accepted(f.rooms.execute(second,new Command(scope,Action.JOIN,s.id(),s.revision(),0,null,"")));
        assertEquals(Reason.INVALID_STATE,f.spawn(2).result().reason());assertEquals(2,f.rooms.memberCount());
    }
    @Test void v7RosterHasTaggedIdentityAndOlderCodecRejectsIt() throws Exception {
        var f=new Fixture(4);f.spawn(2);var value=new ModernApplicationProtocol.RoomResult(1,new ModernRoomProtocol.Echo(f.scope,Action.READ,0,0,0),new Result(Status.OK,Reason.NONE,f.view()));
        byte[] bytes=ModernApplicationProtocol.encode(value,7);assertEquals(value,ModernApplicationProtocol.decode(bytes,7));
        for(int version=1;version<=6;version++){final int v=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(value,v));}
    }
}
