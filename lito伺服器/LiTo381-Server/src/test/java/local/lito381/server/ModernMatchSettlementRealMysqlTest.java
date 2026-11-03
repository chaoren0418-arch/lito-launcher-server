package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;
import static local.lito381.server.ModernMatchSettlementTest.event;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Opt-in integration: genuine isolated mysqld + InnoDB, never the native service. */
class ModernMatchSettlementRealMysqlTest {
    @Test
    @Timeout(value=150, unit=TimeUnit.SECONDS)
    void isolatedRealInnoDbRollbackUniqueKeyConcurrencyAndRestartPersistence() throws Exception {
        String configured=System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(configured!=null&&!configured.isBlank(),
                "isolated MySQL integration requires explicit LITO_MODERN_MYSQL_HOME; no database was started");
        try(var fixture=ModernSettlementMysqlFixture.open(Path.of(configured))) {
            assertNotEquals(3306,fixture.port());fixture.verifyInnoDb();
            assertArrayEquals(new long[]{0,0,0},fixture.counts());
            assertRecord(fixture,7,3333,false,0,0,0);
            var original=event();fixture.store().persist(original,ModernSettlementMysqlFixture.deadline(10));
            fixture.store().persist(original,ModernSettlementMysqlFixture.deadline(10));
            assertArrayEquals(new long[]{1,2,2},fixture.counts());
            assertArrayEquals(new long[]{1,0,1},fixture.total(7,3333));
            assertArrayEquals(new long[]{0,1,1},fixture.total(42,3333));
            assertRecord(fixture,7,3333,true,1,0,1);
            assertRecord(fixture,42,3333,true,0,1,1); // Same object ID, different source, no fallback.
            assertRecord(fixture,7,3334,false,0,0,0);

            // Inject only a driver-style exception after the first *real*
            // participant insert. The genuine server must roll back its ledger,
            // first participant and both counter mutations, not a map mock.
            AtomicInteger written=new AtomicInteger();
            var rollback=new ModernMatchSettlementJdbc(deadline->faultAfterFirstParticipant(
                    fixture.openConnection(deadline),written),ModernSettlementMysqlFixture.DATABASE);
            var rolledBack=event(UUID.randomUUID());
            var failure=assertThrows(SettlementException.class,()->rollback.persist(rolledBack,ModernSettlementMysqlFixture.deadline(10)));
            assertEquals(Failure.UNAVAILABLE,failure.failure());assertEquals(1,written.get());
            assertArrayEquals(new long[]{1,2,2},fixture.counts());
            assertArrayEquals(new long[]{1,0,1},fixture.total(7,3333));
            assertArrayEquals(new long[]{0,1,1},fixture.total(42,3333));
            assertRecord(fixture,7,3333,true,1,0,1); // A real rollback cannot leak estimated totals.
            fixture.store().persist(rolledBack,ModernSettlementMysqlFixture.deadline(10));
            assertArrayEquals(new long[]{2,4,2},fixture.counts());

            proveRealConcurrentDuplicate(fixture,event(UUID.randomUUID()));
            assertArrayEquals(new long[]{3,6,2},fixture.counts());
            assertArrayEquals(new long[]{3,0,3},fixture.total(7,3333));
            assertArrayEquals(new long[]{0,3,3},fixture.total(42,3333));
            assertRecord(fixture,7,3333,true,3,0,3);
            assertRecord(fixture,42,3333,true,0,3,3);
            assertEquals(0,fixture.activeConnections());

            // Actual owned mysqld shutdown/restart using the same fresh data
            // directory, not merely construction of another Java Store object.
            fixture.restart();assertEquals(2,fixture.starts());assertFalse(fixture.forcedTermination());
            fixture.verifyInnoDb();fixture.store().persist(original,ModernSettlementMysqlFixture.deadline(10));
            assertArrayEquals(new long[]{3,6,2},fixture.counts());
            assertArrayEquals(new long[]{3,0,3},fixture.total(7,3333));
            assertArrayEquals(new long[]{0,3,3},fixture.total(42,3333));
            assertRecord(fixture,7,3333,true,3,0,3); // New reader after actual mysqld restart and duplicate replay.
            assertRecord(fixture,42,3333,true,0,3,3);
            assertEquals(0,fixture.activeConnections());

            // SQL committed before local outbox acknowledgement. Reopening
            // replays the exact event through the real coordinator/JDBC writer;
            // this is a reopen boundary, not a claim of process-kill coverage.
            proveCommittedBeforeOutboxAcknowledgement(fixture,event(UUID.randomUUID()));
            assertArrayEquals(new long[]{4,8,2},fixture.counts());
            assertArrayEquals(new long[]{4,0,4},fixture.total(7,3333));
            assertArrayEquals(new long[]{0,4,4},fixture.total(42,3333));
            assertRecord(fixture,7,3333,true,4,0,4); // Durable outbox replay did not count the committed UUID twice.
            assertRecord(fixture,42,3333,true,0,4,4);
            assertEquals(0,fixture.activeConnections());fixture.successful();
        }
    }

    static void assertRecord(ModernSettlementMysqlFixture fixture,int sourceId,long objectId,
                                     boolean present,long wins,long losses,long revision) throws Exception {
        var backend=new ModernBackendExecutor(1,1);
        try(var gate=backend.openSession()) {
            var principal=new ModernAccountAuthentication.Principal("sun-owner",java.util.Set.of(
                    new ModernAccountAuthentication.Source(7,"Sun"),new ModernAccountAuthentication.Source(42,"Moon")),
                    java.util.Map.of(7,"sun-owner",42,"moon-owner"));
            var repository=ModernCharacterRecordRepository.jdbc(fixture::openConnection,ModernSettlementMysqlFixture.DATABASE);
            var result=gate.call(5000,scope->repository.character(principal,sourceId,objectId));
            assertEquals(new ModernCharacterRecord.Snapshot(new TournamentPresence.CharacterKey(sourceId,objectId),present,wins,losses,revision),result);
            assertEquals(0,fixture.activeConnections(),"record reader must actually retire its SQL connection before returning");
        }finally{backend.close();assertTrue(backend.awaitTermination(5000),"record reader workers must actually retire");}
    }

    @Test
    @Timeout(value=150,unit=TimeUnit.SECONDS)
    void cooperativeEngineResultsRollbackConcurrentDuplicatesRestartAndJournalReplayInRealMysql()throws Exception {
        String configured=System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(configured!=null&&!configured.isBlank(),"explicit owned MySQL integration only");
        var mixed=ModernTimeAttackSettlementTest.clear(2,true);
        var timeout=ModernTimeAttackSettlementTest.timeout();
        var allWin=ModernTimeAttackSettlementTest.clear(2,false);
        var solo=ModernTimeAttackSettlementTest.clear(1,false);
        try(var fixture=ModernSettlementMysqlFixture.open(Path.of(configured))) {
            assertNotEquals(3306,fixture.port());fixture.verifyInnoDb();
            fixture.store().persist(mixed,ModernSettlementMysqlFixture.deadline(10));fixture.store().persist(mixed,ModernSettlementMysqlFixture.deadline(10));
            assertArrayEquals(new long[]{1,2,2},fixture.counts());
            assertRecord(fixture,7,3333,true,0,1,1);assertRecord(fixture,42,3333,true,1,0,1);
            AtomicInteger written=new AtomicInteger();var rollback=new ModernMatchSettlementJdbc(deadline->faultAfterFirstParticipant(fixture.openConnection(deadline),written),ModernSettlementMysqlFixture.DATABASE);
            assertEquals(Failure.UNAVAILABLE,assertThrows(SettlementException.class,()->rollback.persist(timeout,ModernSettlementMysqlFixture.deadline(10))).failure());
            assertEquals(1,written.get());assertArrayEquals(new long[]{1,2,2},fixture.counts());
            assertRecord(fixture,7,3333,true,0,1,1);assertRecord(fixture,42,3333,true,1,0,1);
            fixture.store().persist(timeout,ModernSettlementMysqlFixture.deadline(10));
            proveRealConcurrentDuplicate(fixture,allWin);assertArrayEquals(new long[]{3,6,2},fixture.counts());
            assertRecord(fixture,7,3333,true,1,2,3);assertRecord(fixture,42,3333,true,2,1,3);

            Path directory=Files.createDirectory(fixture.artifactDirectory().resolve("cooperative-outbox"));byte[] binding=new byte[32];binding[0]=67;
            try(var box=new ModernMatchOutbox(directory,binding)) {
                box.record(solo,ModernSettlementMysqlFixture.deadline(5));fixture.store().persist(solo,ModernSettlementMysqlFixture.deadline(10));
                assertEquals(1,box.size()); // SQL committed; deliberately no local acknowledgement.
            }
            assertArrayEquals(new long[]{4,7,2},fixture.counts());
            fixture.restart();assertEquals(2,fixture.starts());assertFalse(fixture.forcedTermination());fixture.verifyInnoDb();
            var reopened=new ModernMatchOutbox(directory,binding);ModernSettlementCoordinator coordinator=null;
            try {
                assertEquals(java.util.List.of(solo),reopened.pending(ModernSettlementMysqlFixture.deadline(5)));
                coordinator=new ModernSettlementCoordinator(reopened,fixture.store(),3000,null);
                long until=ModernSettlementMysqlFixture.deadline(8);while(reopened.size()!=0&&System.nanoTime()<until)Thread.sleep(20);
                assertEquals(0,reopened.size());assertTrue(reopened.isHealthy());
            }finally {
                if(coordinator==null)reopened.close();else {coordinator.close();assertTrue(coordinator.awaitTermination(8000));assertTrue(coordinator.isQuiescent());}
            }
            fixture.store().persist(mixed,ModernSettlementMysqlFixture.deadline(10));fixture.store().persist(solo,ModernSettlementMysqlFixture.deadline(10));
            assertEquals(Failure.CONFLICT,assertThrows(SettlementException.class,()->fixture.store().persist(ModernTimeAttackSettlementTest.changedTerrain(mixed),ModernSettlementMysqlFixture.deadline(10))).failure());
            assertArrayEquals(new long[]{4,7,2},fixture.counts());assertRecord(fixture,7,3333,true,2,2,4);assertRecord(fixture,42,3333,true,2,1,3);
            assertRecord(fixture,7,3334,false,0,0,0);assertEquals(0,fixture.activeConnections());fixture.successful();
        }
    }

    @Test void relativeOrParentAliasedInstallationNeverStartsProcess() {
        assertThrows(Exception.class,()->ModernSettlementMysqlFixture.checkedHome(Path.of("relative-mysql")));
        assertThrows(Exception.class,()->ModernSettlementMysqlFixture.checkedHome(Path.of("C:/placeholder/../mysql")));
    }

    @Test void temporaryRootRejectsRelativeParentAliasAndNonAsciiBeforeCreatingAnything() {
        assertThrows(IOException.class,()->ModernSettlementMysqlFixture.checkedTemporaryRoot(Path.of("relative-temp")));
        assertThrows(IOException.class,()->ModernSettlementMysqlFixture.checkedTemporaryRoot(Path.of("C:/placeholder/../temp")));
        assertThrows(IOException.class,()->ModernSettlementMysqlFixture.checkedTemporaryRoot(Path.of("C:/中文暫存")));
    }

    @Test void connectionDiagnosticsExposeOnlyFixedStageSqlstateAndNumericCode() {
        String secret="synthetic-secret-url-and-database-path";
        var valid=ModernSettlementMysqlFixture.connectionFailure(ModernSettlementMysqlFixture.ConnectionStage.CONNECT,
                new SQLException(secret,"28000",1045));
        assertEquals("stage=CONNECT; sqlstate=28000; vendor=1045",valid.diagnostic);
        assertFalse(valid.getMessage().contains(secret));assertNull(valid.getCause());
        var invalid=ModernSettlementMysqlFixture.connectionFailure(ModernSettlementMysqlFixture.ConnectionStage.DATADIR,
                new SQLException(secret,secret,-1));
        assertEquals("stage=DATADIR; sqlstate=NONE; vendor=-1",invalid.diagnostic);
        assertFalse(invalid.getMessage().contains(secret));assertNull(invalid.getCause());
        var pathFailure=ModernSettlementMysqlFixture.connectionFailure(ModernSettlementMysqlFixture.ConnectionStage.DATADIR,
                new IOException(secret));
        assertEquals("stage=DATADIR; sqlstate=NONE; vendor=0",pathFailure.diagnostic);
        assertNull(pathFailure.getCause());
    }

    @Test void windowsOwnerPinsFreshNamespaceUntilReleasedWithoutStartingMysql() throws Exception {
        Assumptions.assumeTrue("\\".equals(Path.of(".").getFileSystem().getSeparator()));
        Path parent=Path.of("target").toRealPath();
        Path directory=Files.createTempDirectory(parent,"mysql-owner-").toRealPath();
        Path renamed=directory.resolveSibling(directory.getFileName()+"-renamed");
        boolean[] moved={false};
        try(var owner=new ModernSettlementMysqlFixture.DirectoryOwner(directory)) {
            owner.validate();
            assertThrows(IOException.class,()->Files.delete(owner.lockPath));
            assertThrows(IOException.class,()->{Files.move(directory,renamed);moved[0]=true;});
            owner.validate();
        } finally {
            Path owned=moved[0]?renamed:directory;
            assertEquals(parent,owned.getParent());assertEquals(owned,owned.toRealPath());
            Files.deleteIfExists(owned.resolve(".isolated-mysql-owner.lock"));
            Files.delete(owned); // Only the now-empty, exactly created root.
        }
    }

    private static void proveCommittedBeforeOutboxAcknowledgement(ModernSettlementMysqlFixture fixture,Event value) throws Exception {
        Path directory=Files.createDirectory(fixture.artifactDirectory().resolve("settlement-outbox"));
        byte[] binding=new byte[32];binding[0]=91; // Synthetic isolated binding, no source configuration.
        try(var original=new ModernMatchOutbox(directory,binding)) {
            original.record(value,ModernSettlementMysqlFixture.deadline(5));
            fixture.store().persist(value,ModernSettlementMysqlFixture.deadline(10));
            assertEquals(1,original.size());
            assertArrayEquals(new long[]{4,8,2},fixture.counts());
            // Deliberately no acknowledge() before close/reopen.
        }
        var reopened=new ModernMatchOutbox(directory,binding);
        ModernSettlementCoordinator coordinator=null;
        try {
            assertEquals(java.util.List.of(value),reopened.pending(ModernSettlementMysqlFixture.deadline(5)));
            coordinator=new ModernSettlementCoordinator(reopened,fixture.store(),3000,null);
            long until=ModernSettlementMysqlFixture.deadline(8);
            while(reopened.size()!=0&&System.nanoTime()<until)Thread.sleep(20);
            assertTrue(reopened.isHealthy());assertEquals(0,reopened.size(),"real SQL duplicate replay must retire the outbox record");
        } finally {
            if(coordinator==null)reopened.close();
            else {
                coordinator.close();assertTrue(coordinator.awaitTermination(8000),"owned SQL replay workers must actually retire");
                assertTrue(coordinator.isQuiescent());
            }
        }
        try(var confirmed=new ModernMatchOutbox(directory,binding)) {assertEquals(0,confirmed.size());}
    }

    static Connection faultAfterFirstParticipant(Connection owned,AtomicInteger written) {
        return wrapConnection(owned,(sql,statement)-> {
            if(!sql.startsWith("INSERT INTO `"+ModernSettlementMysqlFixture.DATABASE+"`.`"+ModernMatchSettlementJdbc.MEMBER+"`"))return statement;
            return (PreparedStatement)Proxy.newProxyInstance(ModernMatchSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(proxy,method,args)->{
                if(method.getName().equals("executeUpdate")) {
                    if(written.get()==1)throw new SQLException("synthetic participant interruption","HY000",90001);
                    Object value=invoke(statement,method,args);written.incrementAndGet();return value;
                }
                return invoke(statement,method,args);
            });
        });
    }

    static void proveRealConcurrentDuplicate(ModernSettlementMysqlFixture fixture,Event value) throws Exception {
        var inserted=new CountDownLatch(1);var release=new CountDownLatch(1);var secondInsert=new CountDownLatch(1);
        var workers=Executors.newFixedThreadPool(2);
        String prefix="INSERT INTO `"+ModernSettlementMysqlFixture.DATABASE+"`.`"+ModernMatchSettlementJdbc.MATCH+"`";
        var firstStore=new ModernMatchSettlementJdbc(deadline->wrapConnection(fixture.openConnection(deadline),(sql,statement)->{
            if(!sql.startsWith(prefix))return statement;
            return (PreparedStatement)Proxy.newProxyInstance(ModernMatchSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(proxy,method,args)->{
                Object result=invoke(statement,method,args);
                if(method.getName().equals("executeUpdate")) {
                    inserted.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new SQLException("synthetic transaction barrier deadline");
                }return result;
            });
        }),ModernSettlementMysqlFixture.DATABASE);
        var secondStore=new ModernMatchSettlementJdbc(deadline->wrapConnection(fixture.openConnection(deadline),(sql,statement)->{
            if(!sql.startsWith(prefix))return statement;
            return (PreparedStatement)Proxy.newProxyInstance(ModernMatchSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(proxy,method,args)->{
                if(method.getName().equals("executeUpdate"))secondInsert.countDown();
                return invoke(statement,method,args);
            });
        }),ModernSettlementMysqlFixture.DATABASE);
        try {
            var first=workers.submit(()->{firstStore.persist(value,ModernSettlementMysqlFixture.deadline(10));return true;});
            assertTrue(inserted.await(5,TimeUnit.SECONDS),"first physical InnoDB INSERT must execute before concurrency check");
            var second=workers.submit(()->{secondStore.persist(value,ModernSettlementMysqlFixture.deadline(10));return true;});
            assertTrue(secondInsert.await(5,TimeUnit.SECONDS),"second real connection must attempt same unique key");
            assertThrows(TimeoutException.class,()->second.get(150,TimeUnit.MILLISECONDS),
                    "duplicate INSERT cannot complete while first InnoDB transaction owns the unique key");
            release.countDown();assertTrue(first.get(10,TimeUnit.SECONDS));assertTrue(second.get(10,TimeUnit.SECONDS));
        } finally {
            release.countDown();workers.shutdownNow();assertTrue(ModernSettlementMysqlFixture.awaitExecutor(workers,12),"owned SQL workers must retire");
        }
    }
    @FunctionalInterface private interface Statements {PreparedStatement wrap(String sql,PreparedStatement statement);}
    private static Connection wrapConnection(Connection owned,Statements statements) {
        return (Connection)Proxy.newProxyInstance(ModernMatchSettlementRealMysqlTest.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            Object result=invoke(owned,method,args);
            if(method.getName().equals("prepareStatement")&&args!=null&&args.length>0&&args[0] instanceof String sql)
                return statements.wrap(sql,(PreparedStatement)result);
            return result;
        });
    }
    private static Object invoke(Object target,java.lang.reflect.Method method,Object[] args) throws Throwable {
        try{return method.invoke(target,args);}catch(InvocationTargetException failure){throw failure.getCause();}
    }
}
