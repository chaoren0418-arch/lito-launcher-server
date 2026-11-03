package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.EOFException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Real Main/TLS/two selected sources/GM BOT/journal/InnoDB. Synthetic shared
 * physical4 layout, NOT a renderer or original native547 geometry acceptance. */
class ModernSiegeMainTest {
    @TempDir Path temporary;
    static final class Client implements AutoCloseable {
        // Keep this legacy v26 peer frozen: later protocol features must not leak
        // into its HELLO merely because the production capability set expands.
        static final int V26_CAPABILITIES = (ModernSessionProtocol.CAP_NPC << 1) - 1;
        final SSLSocket socket;int id=2;ModernRooms.Scope scope;
        Client(ModernServer server,Path tls)throws Exception {int port=server.address().getPort();
            socket=(SSLSocket)ModernTlsTestMaterial.clientContext(tls).getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
            var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(8000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,26,26,V26_CAPABILITIES,7,1)));socket.getOutputStream().flush();
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));assertEquals(26,ack.version());assertTrue((ack.capabilities()&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)!=0);
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message message)throws Exception {
            var bytes=ModernApplicationProtocol.encode(message,26);try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);message.close();}
            var reply=ModernApplicationProtocol.readFrame(socket.getInputStream(),26);if(reply==null)throw new EOFException("Main closed before siege reply");
            try{var result=ModernApplicationProtocol.decode(reply,26);assertEquals(message.requestId(),result.requestId());return result;}finally{Arrays.fill(reply,(byte)0);}
        }
        void enter(int source)throws Exception {assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(new ModernApplicationProtocol.Auth(id++,ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()))).status());
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(id++,source,1))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(id++,source,3333,1))).phase());
            assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(id++,source,3333,1,0))).phase());scope=new ModernRooms.Scope(source,3333,1,0);
        }
        ModernRooms.Result room(ModernRooms.Action action,ModernRooms.Summary s,int value,ModernRooms.Draft draft)throws Exception {
            var reply=((ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(id++,new ModernRooms.Command(scope,action,s==null?0:s.id(),s==null?0:s.revision(),value,draft,"")))).result();assertEquals(ModernRooms.Status.OK,reply.status());return reply;}
        ModernRooms.Summary room()throws Exception{return room(ModernRooms.Action.READ,null,0,null).view().joined().summary();}
        ModernWorldProtocol.Reply world(ModernWorldProtocol.Action action,ModernWorldProtocol.Frame w,ModernRooms.Summary s)throws Exception {
            return ((ModernApplicationProtocol.WorldResult)exchange(new ModernApplicationProtocol.WorldRequest(id++,new ModernWorldProtocol.Request(scope,action,0,s==null?0:s.id(),s==null?0:s.revision(),w==null?0:w.matchId(),w==null?0:w.generation(),0,0,0,action==ModernWorldProtocol.Action.LOADED?w.manifest().digest():ModernWorldProtocol.ZERO_DIGEST)))).reply();}
        ModernWorldProtocol.Frame world()throws Exception{return world(ModernWorldProtocol.Action.READ,null,null).frame();}
        ModernCombatProtocol.Frame combat(ModernWorldProtocol.Frame w)throws Exception{return ((ModernApplicationProtocol.CombatResult)exchange(new ModernApplicationProtocol.CombatRequest(id++,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,w.matchId(),w.generation(),0,0)))).reply().frame();}
        ModernSiegeProtocol.Reply tower(ModernWorldProtocol.Frame w,ModernSiege.Slot slot)throws Exception {long sequence=combat(w).vitals().stream().filter(v->w.actors().get((int)v.entityId()-1).member().identity().equals(ModernParticipantId.human(scope.sourceId(),3333))).findFirst().orElseThrow().attackSequence()+1;
            return ((ModernApplicationProtocol.SiegeResult)exchange(new ModernApplicationProtocol.SiegeRequest(id++,new ModernSiegeProtocol.Request(scope,w.siege().snapshot().state().scope(),ModernSiegeProtocol.Action.ATTACK_TOWER,sequence,slot)))).reply();}
        void walk(ModernWorldMap.Cell target)throws Exception {
            for(int i=0;i<100;i++){var w=world();var a=w.actors().stream().filter(p->p.member().identity().equals(ModernParticipantId.human(scope.sourceId(),3333))).findFirst().orElseThrow();var from=a.position();if(from.equals(target))return;Thread.sleep(2);
                int dx=Integer.compare(target.x(),from.x()),dy=Integer.compare(target.y(),from.y());int heading=dx==0?(dy<0?0:4):dy==0?(dx>0?2:6):dx>0?(dy<0?1:3):(dy<0?7:5);
                var r=((ModernApplicationProtocol.WorldResult)exchange(new ModernApplicationProtocol.WorldRequest(id++,new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.MOVE,heading,0,0,w.matchId(),w.generation(),a.lastMoveSequence()+1,from.x(),from.y(),ModernWorldProtocol.ZERO_DIGEST)))).reply();assertEquals(ModernWorldProtocol.Status.OK,r.status());}
            fail("bounded Main walk exhausted");
        }
        void destroy(ModernSiege.Slot slot)throws Exception {long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(world().siege().snapshot().state().objectives().get(slot.ordinal()).hp()>0){assertTrue(System.nanoTime()<deadline);Thread.sleep(3);assertEquals(ModernSiegeProtocol.Reason.NONE,tower(world(),slot).reason());}}
        ModernCharacterRecord.Snapshot record()throws Exception {var r=((ModernApplicationProtocol.RecordResult)exchange(new ModernApplicationProtocol.RecordRequest(id++,new ModernCharacterRecordProtocol.Context(scope.sourceId(),3333,scope.epoch())))).result();assertEquals(ModernCharacterRecordProtocol.Status.OK,r.status());return r.snapshot();}
        void back(ModernWorldProtocol.Frame w)throws Exception {assertEquals(ModernWorldProtocol.Status.OK,world(ModernWorldProtocol.Action.LEAVE,w,null).status());room(ModernRooms.Action.LEAVE,room(),0,null);
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(id++,scope.sourceId(),3333,scope.epoch()))).phase());
            assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(id++,scope.sourceId(),3333,scope.epoch()))).phase());}
        @Override public void close()throws Exception{socket.close();}
    }
    @Test @Timeout(value=120,unit=TimeUnit.SECONDS)
    void twoTlsSourcesAndGmBotsCompleteSiegeSaveSeparateRecordsAndReturnNormally()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);Assumptions.assumeTrue(mysql!=null);var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        var p=ModernSiegeConfigurationTest.world(temporary,60000);
        try(var fixture=ModernQuestSqlFixture.openSiege(Path.of(mysql),tls,p)){var server=ModernMain.start(fixture.config);int attacker;
            try {try(var a=new Client(server,tls);var b=new Client(server,tls)){
                a.enter(7);b.enter(42);assertFalse(a.record().rowPresent());assertFalse(b.record().rowPresent());
                a.room(ModernRooms.Action.CREATE,null,0,ModernRoomsTest.draft(100,3,547,0));b.room(ModernRooms.Action.JOIN,a.room(),0,null);
                var s=a.room();var gm=((ModernApplicationProtocol.GmResult)a.exchange(new ModernApplicationProtocol.GmRequest(a.id++,new ModernGmCommandProtocol.Request(a.scope,s.id(),s.revision(),28)))).reply();
                assertEquals(ModernGmCommandProtocol.Delivery.RESULT,gm.delivery());assertEquals(28,gm.result().added());
                a.room(ModernRooms.Action.READY,a.room(),1,null);b.room(ModernRooms.Action.READY,b.room(),1,null);
                var prepared=a.world(ModernWorldProtocol.Action.PREPARE,null,a.room());assertEquals(ModernWorldProtocol.Status.OK,prepared.status());
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);var w=a.world();while(w.phase()==ModernWorldProtocol.Phase.PREPARING){assertTrue(System.nanoTime()<deadline);Thread.sleep(10);w=a.world();}
                assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase());assertEquals(30,w.actors().size());assertEquals(4,w.manifest().mapId());
                assertEquals(ModernWorldProtocol.Phase.LOADING,a.world(ModernWorldProtocol.Action.LOADED,w,null).frame().phase());
                assertEquals(ModernWorldProtocol.Phase.ACTIVE,b.world(ModernWorldProtocol.Action.LOADED,w,null).frame().phase());
                w=a.world();attacker=w.siege().snapshot().state().attacker();var attack=attacker==0?a:b;
                attack.destroy(ModernSiege.Slot.OUTER_LEFT);assertFalse(a.world().siege().snapshot().state().open());
                attack.walk(ModernSiegeTest.cell(11,10));attack.destroy(ModernSiege.Slot.OUTER_RIGHT);assertTrue(b.world().siege().snapshot().state().open());
                attack.walk(ModernSiegeTest.cell(29,10));attack.walk(ModernSiegeTest.cell(29,9));attack.walk(ModernSiegeTest.cell(31,9));attack.destroy(ModernSiege.Slot.INNER);
                var done=a.world();assertEquals(ModernWorldProtocol.Phase.FINISHED,done.phase());assertEquals(done.siege().event(done),b.world().siege().event(b.world()));
                deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);while(done.siege().receipt().state()==ModernWorldProtocol.SettlementState.PENDING){assertTrue(System.nanoTime()<deadline);Thread.sleep(20);done=a.world();}
                assertEquals(ModernWorldProtocol.SettlementState.SAVED,done.siege().receipt().state());assertEquals(attacker==0?1:0,a.record().wins());assertEquals(attacker==1?1:0,b.record().wins());
                a.back(done);b.back(done);
            }ModernNpcServiceTest.await(()->server.snapshot().sessions()==0);assertEquals(0,server.snapshot().failed());}
            finally{server.close();assertTrue(server.awaitTermination(5000));}
            assertArrayEquals(new long[]{1,2,2},fixture.mysql.counts());assertArrayEquals(new long[]{attacker==0?1:0,attacker==0?0:1,1},fixture.mysql.total(7,3333));
            assertArrayEquals(new long[]{attacker==1?1:0,attacker==1?0:1,1},fixture.mysql.total(42,3333));assertEquals(0,fixture.mysql.activeConnections());fixture.successful();
        }
    }
}
