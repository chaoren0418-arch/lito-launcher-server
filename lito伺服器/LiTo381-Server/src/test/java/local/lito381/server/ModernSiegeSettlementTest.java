package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernSiegeSettlementTest {
    @TempDir Path root;
    static long deadline(){return ModernSettlementMysqlFixture.deadline(5);}
    static Event clear(int attacker)throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(attacker,1,120_000,1000,0,true)) {
            f.active();assertNull(f.engine.siegeResult());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(attacker,1,ModernSiege.Slot.OUTER_LEFT).status());
            f.walk(attacker,ModernSiegeTest.cell(11,10));f.waitMillis(200);f.tower(attacker,2,ModernSiege.Slot.OUTER_RIGHT);
            f.walk(attacker,ModernSiegeTest.cell(29,10));f.walk(attacker,ModernSiegeTest.cell(29,9));f.walk(attacker,ModernSiegeTest.cell(31,9));f.waitMillis(200);
            f.epoch.set(9000);var world=f.tower(attacker,3,ModernSiege.Slot.INNER).snapshot();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());
            var event=f.engine.siegeResult();assertNotNull(event);assertEquals(9000,event.finishedAtMillis());
            assertEquals(world.revision(),event.engineRevision());assertEquals(ModernSiegeSettlement.configHash(f.config),event.siege().configSha256());
            assertEquals(world.manifest().digest(),event.siege().terrainSha256());assertEquals(world.siege().state().objectives(),event.siege().towers());
            f.epoch.set(99_000);f.waitMillis(500_000);f.tick();assertSame(event,f.engine.siegeResult());
            f.rooms.releasePreparation(f.ticket);f.tick();assertSame(event,f.engine.siegeResult());
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(world,world.revision()+1,null));return event;
        }
    }
    static Event timeout(int attacker,boolean openGate)throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(attacker,1,60_000,1000,0,true)) {
            f.active();if(openGate){f.tower(attacker,1,ModernSiege.Slot.OUTER_LEFT);f.walk(attacker,ModernSiegeTest.cell(11,10));f.waitMillis(200);f.tower(attacker,2,ModernSiege.Slot.OUTER_RIGHT);}
            f.epoch.set(12_345);f.waitMillis(60_000);var world=f.tick();var event=f.engine.siegeResult();assertNotNull(event);
            assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());assertEquals(60_000,event.siege().elapsedMillis());assertEquals(12_345,event.finishedAtMillis());return event;
        }
    }
    static ModernSiegeSettlement.Proof proof(){
        var towers=new ArrayList<ModernSiege.Objective>();for(int i=0;i<3;i++) {
            var t=new ModernSiege.Tower(ModernSiege.Slot.values()[i],new ModernWorldMap.Cell(10+i,9),2665,7);
            towers.add(ModernSiege.Objective.initial(t).damage(7,1,3+i));
        }
        return new ModernSiegeSettlement.Proof(17,4,"11".repeat(32),"22".repeat(32),0,0,ModernSiegeSettlement.End.INNER_DESTROYED,60_000,1000,2000,4,towers);
    }
    /** Synthetic all-human 15-v-15 receipt for JDBC-seam and byte tests only. */
    static Event golden(){
        var rows=new ArrayList<Participant>();for(int i=0;i<30;i++)rows.add(new Participant(i+1,ModernParticipantId.human(i<15?7:42,3333+i%15),i/15,i<15?Verdict.WIN:Verdict.LOSS));
        return new Event(ModernMatchSettlementTest.ID,7,11,5,3,547,123456789,rows,null,proof());
    }
    static Event withProof(Event e,ModernSiegeSettlement.Proof p){return new Event(e.resultId(),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants(),null,p);}
    static ModernSiegeSettlement.Proof replace(ModernSiegeSettlement.Proof p,String terrain,String policy,List<ModernSiege.Objective> towers){
        return new ModernSiegeSettlement.Proof(p.roomId(),p.physicalMapId(),terrain,policy,p.attacker(),p.winner(),p.end(),p.durationMillis(),p.respawnMillis(),p.elapsedMillis(),p.gateRevision(),towers);
    }
    static Event changedTerrain(Event e){return withProof(e,replace(e.siege(),"ab".repeat(32),e.siege().configSha256(),e.siege().towers()));}
    @Test void actualEngineBothAttackRolesAndBothTimeoutGateStatesCaptureOneFrozenRoster()throws Exception {
        for(int attacker:List.of(0,1)) {
            var e=clear(attacker);assertEquals(30,e.participants().size());assertTrue(e.hasBots());assertEquals(attacker,e.siege().winner());
            assertEquals(ModernSiegeSettlement.End.INNER_DESTROYED,e.siege().end());assertEquals(28,e.participants().stream().filter(p->!p.identity().isHuman()).count());
            for(var p:e.participants())assertEquals(p.team()==attacker?Verdict.WIN:Verdict.LOSS,p.verdict());
            for(boolean opened:List.of(false,true)) {var t=timeout(attacker,opened);assertEquals(1-attacker,t.siege().winner());assertEquals(opened,t.siege().gateRevision()>0);
                assertEquals(ModernSiegeSettlement.End.TIMEOUT,t.siege().end());assertTrue(t.siege().towers().get(2).hp()>0);}
        }
    }
    @Test void loadingActiveAbortAndRegressedTerminalUtcNeverMintResults()throws Exception {
        for(int kind=0;kind<4;kind++)try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            if(kind>0)f.active();assertNull(f.engine.siegeResult());
            if(kind==0)f.waitMillis(2000);else if(kind==1)f.engine.abort();else if(kind==2)f.rooms.releasePreparation(f.ticket);
            else {f.epoch.set(999);f.waitMillis(60_000);}
            var world=f.tick();assertEquals(ModernBattleEngine.Phase.ABORTED,world.phase());assertNull(f.engine.siegeResult());
            assertThrows(IllegalArgumentException.class,()->ModernSiegeSettlement.event(world,UUID.randomUUID(),1000));
        }
    }
    @Test void minMaxTeamRosterAndOutcomeProofCannotBeBorrowedByAnotherMode(){
        var e=golden();assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,5,3,547,0,e.participants()));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,5,1,514,0,e.participants(),null,e.siege()));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,5,3,547,0,e.participants().subList(0,29),null,e.siege()));
        var wrong=new ArrayList<>(e.participants());var p=wrong.get(0);wrong.set(0,new Participant(p.entityId(),p.identity(),p.team(),Verdict.LOSS));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,5,3,547,0,wrong,null,e.siege()));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,6,3,547,0,e.participants(),null,e.siege()));
        var rows=new ArrayList<Participant>();for(int i=0;i<100;i++)rows.add(new Participant(i+1,ModernParticipantId.human(7,i+1),i/50,i<50?Verdict.WIN:Verdict.LOSS));
        assertDoesNotThrow(()->new Event(e.resultId(),7,11,5,3,547,0,rows,null,e.siege()));
        rows.set(50,new Participant(51,ModernParticipantId.human(7,51),0,Verdict.WIN));
        assertThrows(IllegalArgumentException.class,()->new Event(e.resultId(),7,11,5,3,547,0,rows,null,e.siege()));
    }
    @Test void wrongHitTeamFutureDeathAndForgedGateOrderCannotCreateCreditableProof(){
        var e=golden();var original=e.siege();var rows=new ArrayList<>(original.towers());var t=rows.get(0);
        rows.set(0,new ModernSiege.Objective(t.tower(),0,7,1,3,3,16));
        var defenseHit=replace(original,original.terrainSha256(),original.configSha256(),rows);
        assertThrows(IllegalArgumentException.class,()->withProof(e,defenseHit));
        assertThrows(IllegalArgumentException.class,()->new ModernSiegeSettlement.Proof(17,4,original.terrainSha256(),original.configSha256(),0,0,original.end(),60_000,1000,2000,3,original.towers()));
        assertThrows(IllegalArgumentException.class,()->new ModernSiegeSettlement.Proof(17,4,original.terrainSha256(),original.configSha256(),0,1,ModernSiegeSettlement.End.TIMEOUT,60_000,1000,60_000,4,original.towers()));
        assertThrows(IllegalArgumentException.class,()->new ModernSiegeSettlement.Proof(17,4,original.terrainSha256(),original.configSha256(),0,0,original.end(),60_000,1000,60_000,4,original.towers()));
    }
    @Test void geometryPolicyHashIgnoresSetOrderButCoversCampsPortalTimingHpAndLogicalMap(){
        var c=ModernSiegeTest.config(547,7,60_000,1000);String baseline=ModernSiegeSettlement.configHash(c);
        var g=new ArrayList<>(c.gate());Collections.reverse(g);
        assertEquals(baseline,ModernSiegeSettlement.configHash(ModernSiegeTest.gate(c,new java.util.LinkedHashSet<>(g))));
        var reversed=new ArrayList<>(c.attackCamp());Collections.reverse(reversed);
        var towers=new ArrayList<>(c.towers());var t=towers.get(0);towers.set(0,new ModernSiege.Tower(t.slot(),t.cell(),t.gfx(),8));
        var variants=List.of(
                new ModernSiege.Config(c.key(),c.towers(),c.gate(),reversed,c.defenseCamp(),c.portal(),60_000,1000),
                new ModernSiege.Config(c.key(),towers,c.gate(),c.attackCamp(),c.defenseCamp(),c.portal(),60_000,1000),
                new ModernSiege.Config(c.key(),c.towers(),c.gate(),c.attackCamp(),c.defenseCamp(),c.portal(),60_001,1000),
                new ModernSiege.Config(c.key(),c.towers(),c.gate(),c.attackCamp(),c.defenseCamp(),c.portal(),60_000,1001),
                new ModernSiege.Config(c.key(),c.towers(),c.gate(),c.attackCamp(),c.defenseCamp(),new ModernSiege.Portal(c.portal().inside(),c.portal().outside()),60_000,1000),
                ModernSiegeTest.config(563,7,60_000,1000));
        for(var changed:variants)assertNotEquals(baseline,ModernSiegeSettlement.configHash(changed));
    }
    @Test void canonicalVersionFourHasFixedProofAndAlwaysTypedParticipantsEvenWithoutBots()throws Exception {
        var e=golden();var bytes=ModernMatchSettlementJdbc.canonical(e);assertEquals(4,ByteBuffer.wrap(bytes).getInt());
        assertEquals(64+30*23+308,bytes.length);int offset=64+30*23;
        String expected="00000004"+"11".repeat(32)+"22".repeat(32)+"0000000000000011000000000000ea60000003e800000000000007d00000000000000004";
        for(int i=0;i<3;i++)expected+=String.format(java.util.Locale.ROOT,"%08x%08x%08x%016x%016x%016x%016x%016x%016x%016x",10+i,9,2665,7L,0L,7L,1L,3L+i,3L+i,1L);
        assertEquals(expected,HexFormat.of().formatHex(Arrays.copyOfRange(bytes,offset,bytes.length)));
        assertEquals(e.siege(),ModernSiegeSettlement.read(ByteBuffer.wrap(bytes,offset,308)));
        for(int i=0;i<30;i++)assertEquals(0,bytes[64+23*i]);
        assertEquals(1,ByteBuffer.wrap(ModernMatchSettlementJdbc.canonical(ModernMatchSettlementTest.event())).getInt());
        assertEquals(2,ByteBuffer.wrap(ModernMatchSettlementJdbc.canonical(ModernBotSettlementTest.event(UUID.randomUUID()))).getInt());
        assertEquals(3,ByteBuffer.wrap(ModernMatchSettlementJdbc.canonical(ModernTimeAttackSettlementTest.golden())).getInt());
    }
    @Test void allFourPersistenceVariantsReopenTogetherWithFullTypedProofAndNoDuplicateCredit()throws Exception {
        var events=List.of(ModernMatchSettlementTest.event(),ModernBotSettlementTest.event(UUID.randomUUID()),ModernTimeAttackSettlementTest.clear(1,false),clear(0));
        try(var box=new ModernMatchOutbox(root,new byte[32])){for(var e:events){box.record(e,deadline());box.record(e,deadline());}assertEquals(4,box.size());}
        try(var box=new ModernMatchOutbox(root,new byte[32])){
            assertEquals(Set.copyOf(events),Set.copyOf(box.pending(deadline())));
            var changed=changedTerrain(events.get(3));assertEquals(ModernMatchOutbox.Failure.CONFLICT,assertThrows(ModernMatchOutbox.OutboxException.class,()->box.record(changed,deadline())).failure());
            for(var e:events)box.acknowledge(e,deadline());assertEquals(0,box.size());
        }
    }
    @Test void corruptTruncatedDowngradedAndWrongTeamProofsFailEvenWithValidFileChecksum()throws Exception {
        byte[] good=ModernMatchSettlementJdbc.canonical(golden());int start=64+30*23;var corrupt=new ArrayList<byte[]>();
        for(int offset:List.of(0,44,48,60,64,start,start+76,start+77,start+78,start+79,start+80,start+84,start+104+12)){
            var value=good.clone();value[offset]=(byte)255;corrupt.add(value);
        }
        var missing=Arrays.copyOf(good,start);ByteBuffer.wrap(missing).putInt(2);corrupt.add(missing);
        var order=good.clone();ByteBuffer.wrap(order).putLong(start+96,3);corrupt.add(order);
        var future=good.clone();ByteBuffer.wrap(future).putLong(start+104+2*68+44,6);corrupt.add(future);
        var wrongActor=good.clone();ByteBuffer.wrap(wrongActor).putLong(start+104+60,16);corrupt.add(wrongActor);
        corrupt.add(Arrays.copyOf(good,good.length-1));corrupt.add(Arrays.copyOf(good,good.length+1));
        for(var payload:corrupt){var file=raw(payload);Files.write(root.resolve("outbox.mso"),file);
            assertEquals(ModernMatchOutbox.Failure.CORRUPT,assertThrows(ModernMatchOutbox.OutboxException.class,()->new ModernMatchOutbox(root,new byte[32])).failure());
            assertArrayEquals(file,Files.readAllBytes(root.resolve("outbox.mso")));}
    }
    static byte[] raw(byte[] payload)throws Exception {
        var out=ByteBuffer.allocate(52+4+payload.length+32);out.putInt(0x4d534f31).putShort((short)1).putShort((short)0).put(new byte[32]).putLong(1).putInt(1).putInt(payload.length).put(payload);
        out.put(MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(out.array(),out.position())));return out.array();
    }
    @Test void humanLedgerSeamEnforcesTerrainConflictAtomicRollbackAndUncertainCommitRetry()throws Exception {
        var db=new ModernMatchSettlementJdbcTest.Database();var e=golden();db.persist(e);db.persist(e);
        assertArrayEquals(new long[]{1,0,1},db.total(7,3333));assertArrayEquals(new long[]{0,1,1},db.total(42,3333));assertEquals(30,db.data.totals.size());
        assertEquals(Failure.CONFLICT,assertThrows(SettlementException.class,()->db.persist(changedTerrain(e))).failure());
        var next=new Event(UUID.randomUUID(),e.matchId(),e.generation(),e.engineRevision(),3,547,0,e.participants(),null,e.siege());
        db.failMember=16;assertThrows(SettlementException.class,()->db.persist(next));assertEquals(1,db.data.ledgers.size());assertArrayEquals(new long[]{1,0,1},db.total(7,3333));
        db.failMember=0;db.commitFault="after";assertEquals(Failure.COMMIT_UNKNOWN,assertThrows(SettlementException.class,()->db.persist(next)).failure());
        db.commitFault="";db.persist(next);db.persist(next);assertArrayEquals(new long[]{2,0,2},db.total(7,3333));assertArrayEquals(new long[]{0,2,2},db.total(42,3333));db.retired();
    }
    @Test void siegeCoordinatorRetainsExactProofOnUnknownCommitAndRestartReplaysIt()throws Exception {
        var e=clear(0);var calls=new AtomicInteger();var seen=new AtomicReference<Event>();
        var coordinator=new ModernSettlementCoordinator(new ModernMatchOutbox(root,new byte[32]),(event,d)->{
            assertEquals(e,event);seen.set(event);if(calls.incrementAndGet()==1)throw new SettlementException(Failure.COMMIT_UNKNOWN);
        },2000,null);
        try {var slot=coordinator.reserve(e.matchId(),e.generation());slot.record(e,deadline());
            long end=deadline();while(slot.state()!=ReceiptState.SAVED&&System.nanoTime()<end)Thread.sleep(5);
            assertEquals(ReceiptState.SAVED,slot.state());assertEquals(2,calls.get());slot.record(e,deadline());assertEquals(e,seen.get());
        }finally{coordinator.close();assertTrue(coordinator.awaitTermination(3000));assertTrue(coordinator.isQuiescent());}
        try(var box=new ModernMatchOutbox(root,new byte[32])){assertEquals(0,box.size());box.record(e,deadline());}
        var reopened=new ModernMatchOutbox(root,new byte[32]);var replay=new ModernSettlementCoordinator(reopened,(event,d)->{assertEquals(e,event);calls.incrementAndGet();},2000,null);
        try {long end=deadline();while(reopened.size()!=0&&System.nanoTime()<end)Thread.sleep(5);assertEquals(0,reopened.size());assertEquals(3,calls.get());}
        finally{replay.close();assertTrue(replay.awaitTermination(3000));assertTrue(replay.isQuiescent());}
    }
}
