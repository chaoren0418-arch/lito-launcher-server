package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Real siege engine -> owned InnoDB -> actual process restart -> production
 * outbox/coordinator/record-reader. No live/source SQL or native client use. */
class ModernSiegeSettlementRealMysqlTest {
    @Test @Timeout(150) void siegeWinsTimeoutsHumanBotRollbackConcurrentRetryAndRealRestartAreExactlyOnce()throws Exception {
        String installation=System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(installation!=null&&!installation.isBlank(),"explicit isolated MySQL opt-in only");
        var attack=ModernSiegeSettlementTest.clear(0);var defense=ModernSiegeSettlementTest.timeout(0,true);
        var swapped=ModernSiegeSettlementTest.clear(1);Event full;
        try(var f=new ModernBattleEngineSiegeTest.Fixture(1,1,50,1000,0,true,50)){f.active();f.waitMillis(50);f.tick();full=f.engine.siegeResult();}
        assertEquals(100,full.participants().size());assertEquals(2672,ModernMatchSettlementJdbc.canonical(full).length);
        try(var fixture=ModernSettlementMysqlFixture.open(Path.of(installation))) {
            assertNotEquals(3306,fixture.port());fixture.verifyInnoDb();
            fixture.store().persist(attack,ModernSettlementMysqlFixture.deadline(10));fixture.store().persist(attack,ModernSettlementMysqlFixture.deadline(10));
            assertCounts(fixture,1,28);assertTotals(fixture,1,0,0,1,1);assertStored(fixture,attack);
            assertEquals(Failure.CONFLICT,assertThrows(SettlementException.class,()->fixture.store().persist(ModernSiegeSettlementTest.changedTerrain(attack),ModernSettlementMysqlFixture.deadline(10))).failure());

            var humanWrites=new AtomicInteger();var failHuman=new ModernMatchSettlementJdbc(d->ModernMatchSettlementRealMysqlTest.faultAfterFirstParticipant(fixture.openConnection(d),humanWrites),ModernSettlementMysqlFixture.DATABASE);
            assertEquals(Failure.UNAVAILABLE,assertThrows(SettlementException.class,()->failHuman.persist(defense,ModernSettlementMysqlFixture.deadline(10))).failure());
            assertEquals(1,humanWrites.get());assertCounts(fixture,1,28);assertTotals(fixture,1,0,0,1,1);
            var botWrites=new AtomicInteger();var failBot=new ModernMatchSettlementJdbc(d->failAfterBot(fixture.openConnection(d),botWrites),ModernSettlementMysqlFixture.DATABASE);
            assertEquals(Failure.UNAVAILABLE,assertThrows(SettlementException.class,()->failBot.persist(defense,ModernSettlementMysqlFixture.deadline(10))).failure());
            assertEquals(1,botWrites.get());assertCounts(fixture,1,28);assertTotals(fixture,1,0,0,1,1);
            fixture.store().persist(defense,ModernSettlementMysqlFixture.deadline(10));assertCounts(fixture,2,56);assertTotals(fixture,1,1,1,1,2);assertStored(fixture,defense);

            ModernMatchSettlementRealMysqlTest.proveRealConcurrentDuplicate(fixture,swapped);
            assertCounts(fixture,3,84);assertTotals(fixture,1,2,2,1,3);
            var directory=Files.createDirectory(fixture.artifactDirectory().resolve("siege-outbox"));byte[] binding=new byte[32];binding[0]=83;
            try(var box=new ModernMatchOutbox(directory,binding)) {
                box.record(full,ModernSettlementMysqlFixture.deadline(5));
                var uncertain=new ModernMatchSettlementJdbc(d->commitThenFail(fixture.openConnection(d)),ModernSettlementMysqlFixture.DATABASE);
                assertEquals(Failure.COMMIT_UNKNOWN,assertThrows(SettlementException.class,()->uncertain.persist(full,ModernSettlementMysqlFixture.deadline(10))).failure());
                assertEquals(List.of(full),box.pending(ModernSettlementMysqlFixture.deadline(5)));
                assertCounts(fixture,4,182);assertTotals(fixture,2,2,2,2,4);assertStored(fixture,full);
            }
            assertEquals(0,fixture.activeConnections());fixture.restart();assertEquals(2,fixture.starts());assertFalse(fixture.forcedTermination());fixture.verifyInnoDb();
            var reopened=new ModernMatchOutbox(directory,binding);ModernSettlementCoordinator coordinator=null;
            try {
                assertEquals(List.of(full),reopened.pending(ModernSettlementMysqlFixture.deadline(5)));
                coordinator=new ModernSettlementCoordinator(reopened,fixture.store(),3000,null);
                long deadline=ModernSettlementMysqlFixture.deadline(8);while(reopened.size()!=0&&System.nanoTime()<deadline)Thread.sleep(10);
                assertEquals(0,reopened.size());assertTrue(reopened.isHealthy());
            }finally{if(coordinator==null)reopened.close();else{coordinator.close();assertTrue(coordinator.awaitTermination(8000));assertTrue(coordinator.isQuiescent());}}
            for(var e:List.of(attack,defense,swapped,full)){fixture.store().persist(e,ModernSettlementMysqlFixture.deadline(10));assertStored(fixture,e);}
            assertCounts(fixture,4,182);assertTotals(fixture,2,2,2,2,4);
            ModernMatchSettlementRealMysqlTest.assertRecord(fixture,7,3334,false,0,0,0);
            assertEquals(0,fixture.activeConnections());fixture.successful();
        }
    }
    private static void assertCounts(ModernSettlementMysqlFixture f,long matches,long bots)throws Exception {
        assertArrayEquals(new long[]{matches,matches*2,2},f.counts());assertEquals(bots,ModernBotSettlementTest.botCount(f));
    }
    private static void assertTotals(ModernSettlementMysqlFixture f,long firstWins,long firstLosses,long secondWins,long secondLosses,long revision)throws Exception {
        ModernMatchSettlementRealMysqlTest.assertRecord(f,7,3333,true,firstWins,firstLosses,revision);
        ModernMatchSettlementRealMysqlTest.assertRecord(f,42,3333,true,secondWins,secondLosses,revision);
    }
    private static void assertStored(ModernSettlementMysqlFixture f,Event event)throws Exception {
        try(var c=f.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement("SELECT mode,map_id,event_body,event_hash FROM `"+ModernSettlementMysqlFixture.DATABASE+"`.`"+ModernMatchSettlementJdbc.MATCH+"` WHERE result_id=?")) {
            s.setQueryTimeout(2);s.setMaxRows(2);s.setBytes(1,ModernMatchSettlementJdbc.uuid(event));
            try(var rows=s.executeQuery()){assertTrue(rows.next());assertEquals(3,rows.getInt("mode"));assertEquals(547,rows.getInt("map_id"));
                var bytes=ModernMatchSettlementJdbc.canonical(event);assertArrayEquals(bytes,rows.getBytes("event_body"));
                assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes),rows.getBytes("event_hash"));assertFalse(rows.next());}
        }
    }
    private static Connection failAfterBot(Connection owned,AtomicInteger written){
        return (Connection)Proxy.newProxyInstance(ModernSiegeSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            Object result=invoke(owned,method,args);
            if(method.getName().equals("prepareStatement")&&args[0] instanceof String sql&&sql.startsWith("INSERT INTO `"+ModernSettlementMysqlFixture.DATABASE+"`.`"+ModernMatchSettlementJdbc.BOT+"`")) {
                return Proxy.newProxyInstance(ModernSiegeSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{
                    Object value=invoke(result,m,a);if(m.getName().equals("executeUpdate")){written.incrementAndGet();throw new SQLException("synthetic after real bot insert","HY000",90002);}return value;
                });
            }return result;
        });
    }
    private static Connection commitThenFail(Connection owned){
        return (Connection)Proxy.newProxyInstance(ModernSiegeSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            Object result=invoke(owned,method,args);if(method.getName().equals("commit"))throw new SQLException("synthetic lost commit acknowledgement","08007",90003);return result;
        });
    }
    private static Object invoke(Object value,java.lang.reflect.Method method,Object[] args)throws Throwable {
        try{return method.invoke(value,args);}catch(InvocationTargetException failure){throw failure.getCause();}
    }
}
