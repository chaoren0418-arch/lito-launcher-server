package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchSettlement.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class ModernBotSettlementTest {
    @TempDir Path directory;
    static Event event(UUID id) {
        return new Event(id,1,2,3,1,514,100,List.of(
                new Participant(1,ModernParticipantId.human(7,3333),0,Verdict.WIN),
                new Participant(2,ModernParticipantId.bot(7,3333),1,Verdict.LOSS)));
    }
    @Test void mixedIdentityEventUsesCanonicalV2AndReopensEntireRosterWithoutFakeHumanKey() throws Exception {
        var e=event(UUID.randomUUID());assertTrue(e.hasBots());
        assertEquals(2,ByteBuffer.wrap(ModernMatchSettlementJdbc.canonical(e)).getInt());
        assertEquals(110,ModernMatchSettlementJdbc.canonical(e).length);
        assertThrows(IllegalStateException.class,e.participants().get(1)::key);
        assertEquals(1,ByteBuffer.wrap(ModernMatchSettlementJdbc.canonical(ModernMatchSettlementTest.event())).getInt());
        byte[] binding=new byte[32];
        try(var outbox=new ModernMatchOutbox(directory,binding)){outbox.record(e,ModernSettlementMysqlFixture.deadline(5));}
        try(var outbox=new ModernMatchOutbox(directory,binding)){
            assertEquals(List.of(e),outbox.pending(ModernSettlementMysqlFixture.deadline(5)));
            outbox.acknowledge(e,ModernSettlementMysqlFixture.deadline(5));assertEquals(0,outbox.size());
        }
    }
    @Test void duplicateTypedIdentityAndBotOnlyOutcomeAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Event(UUID.randomUUID(),1,2,3,1,514,100,List.of(
                new Participant(1,ModernParticipantId.bot(7,1),0,Verdict.WIN),new Participant(2,ModernParticipantId.bot(7,2),1,Verdict.LOSS))));
        assertThrows(IllegalArgumentException.class,()->new Event(UUID.randomUUID(),1,2,3,1,514,100,List.of(
                new Participant(1,ModernParticipantId.human(7,1),0,Verdict.WIN),new Participant(2,ModernParticipantId.human(7,1),1,Verdict.LOSS))));
    }
    @Test @Timeout(120) void genuineMysqlSeparatesCollidingBotIdFromHumanTotalsAndAtomicallyRollsBackBotInsert() throws Exception {
        String home=System.getenv(ModernSettlementMysqlFixture.ENV);
        Assumptions.assumeTrue(home!=null&&!home.isBlank(),"isolated MySQL opt-in only");
        try(var fixture=ModernSettlementMysqlFixture.open(Path.of(home))) {
            fixture.verifyInnoDb();var first=event(UUID.randomUUID());
            fixture.store().persist(first,ModernSettlementMysqlFixture.deadline(10));fixture.store().persist(first,ModernSettlementMysqlFixture.deadline(10));
            assertArrayEquals(new long[]{1,1,1},fixture.counts());assertArrayEquals(new long[]{1,0,1},fixture.total(7,3333));assertEquals(1,botCount(fixture));
            var second=event(UUID.randomUUID());
            var failing=new ModernMatchSettlementJdbc(deadline->failBotWrite(fixture.openConnection(deadline)),ModernSettlementMysqlFixture.DATABASE);
            assertThrows(SettlementException.class,()->failing.persist(second,ModernSettlementMysqlFixture.deadline(10)));
            assertArrayEquals(new long[]{1,1,1},fixture.counts());assertArrayEquals(new long[]{1,0,1},fixture.total(7,3333));assertEquals(1,botCount(fixture));
            Path outboxPath=fixture.artifactDirectory().resolve("bot-outbox");java.nio.file.Files.createDirectory(outboxPath);
            byte[] binding=new byte[32];
            try(var outbox=new ModernMatchOutbox(outboxPath,binding)){outbox.record(second,ModernSettlementMysqlFixture.deadline(5));fixture.store().persist(second,ModernSettlementMysqlFixture.deadline(10));}
            fixture.restart();
            try(var outbox=new ModernMatchOutbox(outboxPath,binding)){
                assertEquals(List.of(second),outbox.pending(ModernSettlementMysqlFixture.deadline(5)));
                fixture.store().persist(second,ModernSettlementMysqlFixture.deadline(10));outbox.acknowledge(second,ModernSettlementMysqlFixture.deadline(5));
            }
            assertArrayEquals(new long[]{2,2,1},fixture.counts());assertArrayEquals(new long[]{2,0,2},fixture.total(7,3333));assertEquals(2,botCount(fixture));
            assertEquals(0,fixture.activeConnections());fixture.successful();
        }
    }
    static long botCount(ModernSettlementMysqlFixture fixture) throws Exception {
        try(var c=fixture.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.prepareStatement("SELECT COUNT(*) FROM `"+ModernSettlementMysqlFixture.DATABASE+"`.`"+ModernMatchSettlementJdbc.BOT+"`")) {
            s.setQueryTimeout(2);s.setMaxRows(2);try(var rows=s.executeQuery()){assertTrue(rows.next());long count=rows.getLong(1);assertFalse(rows.next());return count;}
        }
    }
    private static Connection failBotWrite(Connection owned) {
        return (Connection)Proxy.newProxyInstance(ModernBotSettlementTest.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            Object result=invoke(owned,method,args);
            if(method.getName().equals("prepareStatement")&&args[0] instanceof String sql&&sql.startsWith("INSERT INTO `"+ModernSettlementMysqlFixture.DATABASE+"`.`"+ModernMatchSettlementJdbc.BOT+"`")) {
                return Proxy.newProxyInstance(ModernBotSettlementTest.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{
                    if(m.getName().equals("executeUpdate"))throw new SQLException("synthetic bot write failure","HY000",90002);
                    return invoke(result,m,a);
                });
            }
            return result;
        });
    }
    private static Object invoke(Object target,java.lang.reflect.Method method,Object[] args)throws Throwable {
        try{return method.invoke(target,args);}catch(InvocationTargetException failure){throw failure.getCause();}
    }
}
