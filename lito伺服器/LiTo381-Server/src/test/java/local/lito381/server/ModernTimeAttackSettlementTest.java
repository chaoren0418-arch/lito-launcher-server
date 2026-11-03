package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernTimeAttackSettlementTest {
    @TempDir Path root;
    static long deadline(){return System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);}
    static ModernTimeAttackSettlement.Proof proof(long alive,long left) {
        return new ModernTimeAttackSettlement.Proof(11,554,"11".repeat(32),"22".repeat(32),ModernTimeAttack.End.CLEARED,2,3,1,20_000,1000,4000,alive,left,5);
    }
    static Event golden() {
        return new Event(ModernMatchSettlementTest.ID,7,11,19,4,540,123456789,List.of(
                ModernMatchSettlementTest.member(1,7,3333,0,Verdict.WIN),ModernMatchSettlementTest.member(2,42,3333,0,Verdict.LOSS)),proof(1,0));
    }
    static Event clear(int humans,boolean ownerLeaves)throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(humans,8,100,false)) {
            f.active();assertNull(f.engine.timeAttackResult());f.tower(0,1);
            if(ownerLeaves)f.engine.withdrawTimeAttack(f.players.get(0).owner());
            for(int wave=0;wave<f.plan.waves().size();wave++){
                f.elapsed(1000+1000*wave);f.epoch.set(9000+wave);
                assertEquals(ModernNpcBattle.Reason.ATTACKED,f.hit(ownerLeaves?1:0,wave+(ownerLeaves?1:2),f.living()).reason());
            }
            var world=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());
            var event=f.engine.timeAttackResult();assertNotNull(event);assertEquals(world.timeAttack().outcomes(world),event.participants());
            assertEquals(9002,event.finishedAtMillis());assertEquals(ModernTimeAttackSettlement.planHash(f.plan),event.timeAttack().planSha256());
            f.epoch.set(99_999);f.elapsed(500_000);f.view();assertSame(event,f.engine.timeAttackResult());
            f.engine.withdrawTimeAttack(f.players.get(ownerLeaves?1:0).owner());assertSame(event,f.engine.timeAttackResult());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(world,world.revision()+1,null));
            return event;
        }
    }
    static Event timeout()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,false)) {
            f.active();f.tower(0,1);f.epoch.set(12345);f.elapsed(20_000);f.view();
            var event=f.engine.timeAttackResult();assertEquals(12345,event.finishedAtMillis());assertEquals(ModernTimeAttack.End.TIMEOUT,event.timeAttack().end());return event;
        }
    }
    @Test void actualSoloPartyAndIndividualDepartureKeepOneImmutableCooperativeAuthority()throws Exception {
        var solo=clear(1,false);assertEquals(1,solo.participants().size());assertEquals(Verdict.WIN,solo.participants().get(0).verdict());
        var party=clear(2,false);assertTrue(party.participants().stream().allMatch(p->p.verdict()==Verdict.WIN&&p.team()==0));
        var left=clear(2,true);assertEquals(List.of(Verdict.LOSS,Verdict.WIN),left.participants().stream().map(Participant::verdict).toList());
        assertEquals(1,left.timeAttack().departedMask());assertEquals(2,left.timeAttack().leader());assertNotEquals(party.resultId(),left.resultId());
        var timeout=timeout();assertTrue(timeout.participants().stream().allMatch(p->p.verdict()==Verdict.LOSS));
    }
    @Test void actualLeaderDeathAndLastDepartureRetainTheirDifferentTerminalCauses()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,1,false)) {
            f.active();f.tower(0,1);var npc=f.living();f.elapsed(1000);f.epoch.set(5000);f.engine.stepNpc(npc.id(),npc.life(),4096,false);
            var e=f.engine.timeAttackResult();assertEquals(ModernTimeAttack.End.LEADER_DEFEATED,e.timeAttack().end());assertEquals(5000,e.finishedAtMillis());
            assertEquals(2,e.timeAttack().aliveMask());assertTrue(e.participants().stream().allMatch(p->p.verdict()==Verdict.LOSS));
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();f.epoch.set(6000);f.engine.withdrawTimeAttack(f.players.get(0).owner());var e=f.engine.timeAttackResult();
            assertEquals(ModernTimeAttack.End.PARTY_DEFEATED,e.timeAttack().end());assertEquals(-1,e.timeAttack().startedMillis());assertEquals(6000,e.finishedAtMillis());
            assertEquals(1,e.timeAttack().aliveMask());assertEquals(1,e.timeAttack().departedMask());assertEquals(Verdict.LOSS,e.participants().get(0).verdict());
        }
    }
    @Test void waitingCancelAndRegressedTerminalUtcCannotCreateCreditableResults()throws Exception {
        for(int kind=0;kind<3;kind++)try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,100,false)) {
            f.active();
            if(kind==0)f.elapsed(60_000);
            else if(kind==1){f.tower(0,1);f.epoch.set(999);f.elapsed(20_000);}
            else {f.epoch.set(999);var w=f.engine.withdrawTimeAttack(f.players.get(0).owner());assertEquals(ModernBattleEngine.Phase.ABORTED,w.phase());assertEquals(0,f.engine.ownerCount());}
            if(kind!=2)assertEquals(ModernBattleEngine.Phase.ABORTED,f.view().world().phase());
            assertNull(f.engine.timeAttackResult());
        }
    }
    @Test void cooperativeVariantNeverWeakensOldTeamIdentityOrRosterBounds()throws Exception {
        var e=golden();
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,19,4,540,0,e.participants()));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,19,1,514,0,ModernMatchSettlementTest.event().participants(),e.timeAttack()));
        var wrong=new ArrayList<>(e.participants());wrong.set(1,ModernMatchSettlementTest.member(2,42,3333,1,Verdict.LOSS));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,19,4,540,0,wrong,e.timeAttack()));
        wrong.set(1,ModernMatchSettlementTest.member(2,42,3333,0,Verdict.WIN));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,19,4,540,0,wrong,e.timeAttack()));
        var rows=new ArrayList<Participant>();for(int n=1;n<=60;n++)rows.add(ModernMatchSettlementTest.member(n,7,n,0,Verdict.WIN));
        var many=new Event(UUID.randomUUID(),7,11,19,4,540,0,rows,proof((1L<<60)-1,0));assertEquals(1508,ModernMatchSettlementJdbc.canonical(many).length);
        rows.add(ModernMatchSettlementTest.member(61,7,61,0,Verdict.WIN));assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,19,4,540,0,rows,many.timeAttack()));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,19,4,540,0,e.participants(),proof(5,0)));
        assertThrows(IllegalArgumentException.class,()->proof(1,1));
        assertThrows(IllegalArgumentException.class,()->new ModernTimeAttackSettlement.Proof(11,554,"11".repeat(32),"22".repeat(32),ModernTimeAttack.End.PARTY_DEFEATED,0,3,1,20_000,-1,0,0,0,0));
    }
    @Test void versionThreeBytesHaveIndependentFixedProofAndOldVersionOneIsUnchanged()throws Exception {
        var e=golden();String expected="00000003"+"12345678123456789abc123456789abc"+"0000000000000007000000000000000b0000000000000013"
                +"000000040000021c00000000075bcd1500000002"
                +"0000000000000001000000070000000000000d050000"+"00000000000000020000002a0000000000000d050001"
                +"0000022a"+"11".repeat(32)+"22".repeat(32)+"000000000000000b0102030100004e20"
                +"00000000000003e80000000000000fa0000000000000000100000000000000000000000000000005";
        byte[] bytes=ModernMatchSettlementJdbc.canonical(e);assertEquals(expected,HexFormat.of().formatHex(bytes));assertEquals(232,bytes.length);
        assertEquals(e.timeAttack(),ModernTimeAttackSettlement.read(ByteBuffer.wrap(Arrays.copyOfRange(bytes,108,bytes.length))));
        assertEquals(1,ByteBuffer.wrap(ModernMatchSettlementJdbc.canonical(ModernMatchSettlementTest.event())).getInt());
    }
    @Test void mixedLegacySoloAndCooperativeOutboxReopenPreservesProofAndSameIdConflicts()throws Exception {
        var values=List.of(ModernMatchSettlementTest.event(),clear(1,false),clear(2,true));byte[] binding=new byte[32];binding[0]=67;
        try(var box=new ModernMatchOutbox(root,binding)) {
            for(var e:values){box.record(e,deadline());box.record(e,deadline());}assertEquals(3,box.size());
        }
        try(var box=new ModernMatchOutbox(root,binding)) {
            assertEquals(java.util.Set.copyOf(values),java.util.Set.copyOf(box.pending(deadline())));
            var e=values.get(2);var changed=changedTerrain(e);
            assertEquals(ModernMatchOutbox.Failure.CONFLICT,assertThrows(ModernMatchOutbox.OutboxException.class,()->box.record(changed,deadline())).failure());
            for(var value:values)box.acknowledge(value,deadline());assertEquals(0,box.size());
        }
    }
    static Event changedTerrain(Event e) {
        var p=e.timeAttack();var changed=new ModernTimeAttackSettlement.Proof(p.roomId(),p.physicalMapId(),"ab".repeat(32),p.planSha256(),p.end(),p.wave(),p.waveCount(),p.leader(),p.durationMillis(),p.startedMillis(),p.observedMillis(),p.aliveMask(),p.departedMask(),p.towerDeathRevision());
        return new Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants(),changed);
    }
    @Test void corruptedOrDowngradedProofFailsClosedEvenWithRecomputedOuterChecksum()throws Exception {
        byte[] good=ModernMatchSettlementJdbc.canonical(golden());var broken=new ArrayList<byte[]>();
        for(int offset:List.of(0,44,48,60,108,184,186,187)) {var b=good.clone();b[offset]=(byte)0xff;broken.add(b);}
        var unproven=Arrays.copyOf(good,108);ByteBuffer.wrap(unproven).putInt(1);broken.add(unproven);
        var falseAlive=good.clone();ByteBuffer.wrap(falseAlive).putLong(208,3);broken.add(falseAlive);
        for(long revision:List.of(19L,20L)){var futureRevision=good.clone();ByteBuffer.wrap(futureRevision).putLong(224,revision);broken.add(futureRevision);}
        broken.add(Arrays.copyOf(good,good.length-1));broken.add(Arrays.copyOf(good,good.length+1));
        for(var payload:broken) {
            byte[] file=raw(payload);Files.write(root.resolve("outbox.mso"),file);
            assertEquals(ModernMatchOutbox.Failure.CORRUPT,assertThrows(ModernMatchOutbox.OutboxException.class,()->new ModernMatchOutbox(root,new byte[32])).failure());
            assertArrayEquals(file,Files.readAllBytes(root.resolve("outbox.mso")));
        }
    }
    private static byte[] raw(byte[] payload)throws Exception {
        var out=ByteBuffer.allocate(52+4+payload.length+32);out.putInt(0x4d534f31).putShort((short)1).putShort((short)0).put(new byte[32]).putLong(1).putInt(1).putInt(payload.length).put(payload);
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(out.array(),out.position()));out.put(hash);return out.array();
    }
    @Test void jdbcUsesOneSourceQualifiedLedgerAndExactProofForRollbackAndDuplicateCredit()throws Exception {
        var db=new ModernMatchSettlementJdbcTest.Database();var e=clear(2,true);db.persist(e);db.persist(e);
        assertArrayEquals(new long[]{0,1,1},db.total(7,3333));assertArrayEquals(new long[]{1,0,1},db.total(42,3333));
        assertEquals(Failure.CONFLICT,assertThrows(SettlementException.class,()->db.persist(changedTerrain(e))).failure());
        var loss=timeout();db.failMember=2;assertThrows(SettlementException.class,()->db.persist(loss));
        assertArrayEquals(new long[]{0,1,1},db.total(7,3333));assertArrayEquals(new long[]{1,0,1},db.total(42,3333));
        db.failMember=0;db.persist(loss);assertArrayEquals(new long[]{0,2,2},db.total(7,3333));assertArrayEquals(new long[]{1,1,2},db.total(42,3333));db.retired();
    }
    @Test void uncertainCommitRetainsSameCooperativeIdentityForOneCredit()throws Exception {
        for(String when:List.of("before","after")) {
            var db=new ModernMatchSettlementJdbcTest.Database();var e=clear(1,false);db.commitFault=when;
            assertEquals(Failure.COMMIT_UNKNOWN,assertThrows(SettlementException.class,()->db.persist(e)).failure());assertEquals(0,db.rollbacks);
            db.commitFault="";db.persist(e);db.persist(e);assertArrayEquals(new long[]{1,0,1},db.total(7,3333));assertEquals(1,db.data.ledgers.size());db.retired();
        }
    }
}
