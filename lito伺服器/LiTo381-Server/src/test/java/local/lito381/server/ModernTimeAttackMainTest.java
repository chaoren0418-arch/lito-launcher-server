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

/** Headless v25 client through real Main/TLS/SQL/outbox. Shared physical554
 * is an explicit test mapping for logical540, not native540 or UI evidence. */
class ModernTimeAttackMainTest {
    static final class Client implements AutoCloseable {
        final SSLSocket socket;int id=2;ModernRooms.Scope scope;
        Client(ModernServer server,Path tls)throws Exception {
            int port=server.address().getPort();socket=(SSLSocket)ModernTlsTestMaterial.clientContext(tls).getSocketFactory()
                    .createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
            var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(8000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,25,25,ModernNpcStartupTest.LEGACY_NPC_CAPABILITIES,7,1)));
            socket.getOutputStream().flush();var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));
            assertEquals(25,ack.version());assertTrue((ack.capabilities()&ModernSessionProtocol.CAP_NPC)!=0);
            assertEquals(0,ack.capabilities()&~ModernNpcStartupTest.LEGACY_NPC_CAPABILITIES);
        }
        int next(){return id++;}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message m)throws Exception {
            byte[] bytes=ModernApplicationProtocol.encode(m,25);
            try{socket.getOutputStream().write(bytes);socket.getOutputStream().flush();}finally{Arrays.fill(bytes,(byte)0);m.close();}
            var reply=ModernApplicationProtocol.readFrame(socket.getInputStream(),25);if(reply==null)throw new EOFException("Main closed before reply");
            try{var r=ModernApplicationProtocol.decode(reply,25);assertEquals(m.requestId(),r.requestId());return r;}finally{Arrays.fill(reply,(byte)0);}
        }
        void enter(int source,long epoch)throws Exception {
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch))).phase());
            assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),source,3333,epoch,0))).phase());
            scope=new ModernRooms.Scope(source,3333,epoch,0);
        }
        ModernRooms.Result room(ModernRooms.Action action,ModernRooms.Summary s,int value,ModernRooms.Draft draft)throws Exception {
            var result=((ModernApplicationProtocol.RoomResult)exchange(new ModernApplicationProtocol.RoomRequest(next(),new ModernRooms.Command(scope,action,s==null?0:s.id(),s==null?0:s.revision(),value,draft,"")))).result();
            assertEquals(ModernRooms.Status.OK,result.status());return result;
        }
        ModernRooms.Summary room()throws Exception{return room(ModernRooms.Action.READ,null,0,null).view().joined().summary();}
        ModernWorldProtocol.Reply world(ModernWorldProtocol.Action action,ModernWorldProtocol.Frame w,ModernRooms.Summary s)throws Exception {
            return ((ModernApplicationProtocol.WorldResult)exchange(new ModernApplicationProtocol.WorldRequest(next(),new ModernWorldProtocol.Request(scope,action,0,
                    s==null?0:s.id(),s==null?0:s.revision(),w==null?0:w.matchId(),w==null?0:w.generation(),0,0,0,
                    action==ModernWorldProtocol.Action.LOADED?w.manifest().digest():ModernWorldProtocol.ZERO_DIGEST)))).reply();
        }
        ModernNpcProtocol.Reply npc(ModernWorldProtocol.Frame w,ModernNpcProtocol.Action action,long seq,long serial,long life)throws Exception {
            return ((ModernApplicationProtocol.NpcResult)exchange(new ModernApplicationProtocol.NpcRequest(next(),new ModernNpcProtocol.Request(scope,action,
                    new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation()),seq,serial,life)))).reply();
        }
        ModernWorldProtocol.Frame active()throws Exception {
            room(ModernRooms.Action.CREATE,null,0,ModernRoomsTest.draft(4,4,540,0));room(ModernRooms.Action.READY,room(),1,null);
            var r=world(ModernWorldProtocol.Action.PREPARE,null,room());assertEquals(ModernWorldProtocol.Status.OK,r.status());
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);var w=r.frame();
            while(w.phase()==ModernWorldProtocol.Phase.PREPARING){assertTrue(System.nanoTime()<deadline);Thread.sleep(10);w=world(ModernWorldProtocol.Action.READ,null,null).frame();}
            assertEquals(ModernWorldProtocol.Phase.LOADING,w.phase());assertEquals(554,w.manifest().mapId());assertNotNull(w.timeAttack());
            assertFalse(npc(w,ModernNpcProtocol.Action.READ,0,0,0).frame().resources().graphics().isEmpty());
            var loaded=world(ModernWorldProtocol.Action.LOADED,w,null);assertEquals(ModernWorldProtocol.Status.OK,loaded.status());assertEquals(ModernWorldProtocol.Phase.ACTIVE,loaded.frame().phase());return loaded.frame();
        }
        void back(ModernWorldProtocol.Frame w)throws Exception {
            assertEquals(ModernWorldProtocol.Status.OK,world(ModernWorldProtocol.Action.LEAVE,w,null).status());room(ModernRooms.Action.LEAVE,room(),0,null);
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(next(),scope.sourceId(),3333,scope.epoch()))).phase());
            assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),3333,scope.epoch()))).phase());
        }
        ModernCharacterRecord.Snapshot record()throws Exception {
            var r=((ModernApplicationProtocol.RecordResult)exchange(new ModernApplicationProtocol.RecordRequest(next(),new ModernCharacterRecordProtocol.Context(scope.sourceId(),3333,scope.epoch())))).result();
            assertEquals(ModernCharacterRecordProtocol.Status.OK,r.status());return r.snapshot();
        }
        @Override public void close()throws Exception{socket.close();}
    }
    @Test @Timeout(value=150,unit=TimeUnit.SECONDS)
    void mainReadsSharedGeometryAndSqlThenTlsClientCompletesWavesReturnsAndSwitchesSameObjectSource()throws Exception {
        var mysql=System.getenv(ModernSettlementMysqlFixture.ENV);var shared=System.getenv("LITO_SHARED_RESOURCE_ROOT");Assumptions.assumeTrue(mysql!=null&&shared!=null);
        var tls=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(tls);
        try(var fixture=ModernQuestSqlFixture.openTimeAttack(Path.of(mysql),Path.of(shared),tls)) {
            var server=ModernMain.start(fixture.config);
            try {try(var c=new Client(server,tls)) {
                assertEquals(0,((ModernApplicationProtocol.AuthResult)c.exchange(new ModernApplicationProtocol.Auth(c.next(),ModernQuestSqlFixture.ACCOUNT,ModernQuestSqlFixture.PASSWORD.toCharArray()))).status());
                c.enter(7,1);assertFalse(c.record().rowPresent());var w=c.active();
                var tower=c.npc(w,ModernNpcProtocol.Action.ATTACK_TOWER,1,0,0);assertEquals(ModernNpcProtocol.Status.OK,tower.status());
                long sequence=1,deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(18);ModernNpcProtocol.Frame state=tower.frame();
                while(state.combat().world().phase()!=ModernWorldProtocol.Phase.FINISHED) {
                    assertTrue(System.nanoTime()<deadline,"bounded real RNG gameplay");Thread.sleep(710);
                    state=c.npc(w,ModernNpcProtocol.Action.READ,0,0,0).frame();
                    if(state.combat().world().phase()==ModernWorldProtocol.Phase.FINISHED)break;
                    var target=state.actors().stream().filter(a->a.hp()>0).findFirst();if(target.isEmpty())continue;
                    var a=target.orElseThrow();var hit=c.npc(w,ModernNpcProtocol.Action.ATTACK,++sequence,a.serial(),a.life());assertEquals(ModernNpcProtocol.Status.OK,hit.status());state=hit.frame();
                }
                var finished=state.combat().world();assertEquals(ModernTimeAttack.End.CLEARED,finished.timeAttack().end());var event=finished.timeAttack().event(finished);
                deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
                while(finished.timeAttack().receipt().state()==ModernWorldProtocol.SettlementState.PENDING){assertTrue(System.nanoTime()<deadline);Thread.sleep(20);finished=c.world(ModernWorldProtocol.Action.READ,w,null).frame();}
                assertEquals(ModernWorldProtocol.SettlementState.SAVED,finished.timeAttack().receipt().state());assertEquals(event,finished.timeAttack().event(finished));
                assertEquals(1,c.record().wins());assertEquals(0,c.record().losses());c.back(w);
                c.enter(42,2);assertFalse(c.record().rowPresent());var second=c.active();c.back(second); // Last participant withdrawal, before tower, is a separate loss.
                c.enter(42,3);deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);var r=c.record();
                while(!r.rowPresent()){assertTrue(System.nanoTime()<deadline);Thread.sleep(20);r=c.record();}
                assertEquals(0,r.wins());assertEquals(1,r.losses());
                assertEquals(1,((ModernApplicationProtocol.NavigationResult)c.exchange(new ModernApplicationProtocol.LobbyLeave(c.next(),42,3333,3))).phase());
                assertEquals(0,((ModernApplicationProtocol.NavigationResult)c.exchange(new ModernApplicationProtocol.CharacterReturn(c.next(),42,3333,3))).phase());
            }
                ModernNpcServiceTest.await(()->server.snapshot().sessions()==0);assertEquals(0,server.snapshot().failed());
            }finally{server.close();assertTrue(server.awaitTermination(5000));}
            assertArrayEquals(new long[]{2,2,2},fixture.mysql.counts());assertArrayEquals(new long[]{1,0,1},fixture.mysql.total(7,3333));assertArrayEquals(new long[]{0,1,1},fixture.mysql.total(42,3333));
            try(var connection=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                for(int source:new int[]{7,42})try(var query=connection.prepareStatement("SELECT count,is_equipped FROM `"+ModernQuestSqlFixture.source(source)+"`.character_items WHERE id=1002")) {
                    query.setQueryTimeout(2);try(var rows=query.executeQuery()){assertTrue(rows.next());assertEquals(source==7?2:5,rows.getLong(1));assertEquals(0,rows.getLong(2));assertFalse(rows.next());}
                }
            }
            assertEquals(0,fixture.mysql.activeConnections());fixture.successful();
        }
    }
}
