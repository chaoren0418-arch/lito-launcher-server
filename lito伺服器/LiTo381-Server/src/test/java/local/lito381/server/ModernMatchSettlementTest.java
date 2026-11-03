package local.lito381.server;

import static local.lito381.server.ModernMatchSettlement.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernMatchSettlementTest {
    static final UUID ID=UUID.fromString("12345678-1234-5678-9abc-123456789abc");
    static Participant member(long entity,int source,long object,int team,Verdict verdict) {
        return new Participant(entity,new TournamentPresence.CharacterKey(source,object),team,verdict);
    }
    static Event event() {return event(ID);}
    static Event event(UUID id) {return new Event(id,7,11,19,1,514,123456789,List.of(
            member(1,7,3333,0,Verdict.WIN),member(2,42,3333,1,Verdict.LOSS)));}
    private static Event with(List<Participant> rows) {return new Event(ID,7,11,19,1,514,123456789,rows);}

    @Test void immutableFullEventPreservesSameObjectIdAcrossSourcesAndFullI64() throws Exception {
        var rows=new ArrayList<>(List.of(member(1,7,Long.MAX_VALUE,0,Verdict.LOSS),member(2,Integer.MAX_VALUE,Long.MAX_VALUE,1,Verdict.WIN)));
        var event=new Event(ID,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,1,514,Long.MAX_VALUE,rows);
        rows.clear();assertEquals(2,event.participants().size());assertThrows(UnsupportedOperationException.class,()->event.participants().clear());
        assertEquals(Long.MAX_VALUE,event.participants().get(1).key().objectId());assertEquals(Long.MAX_VALUE,event.finishedAtMillis());
        assertEquals(event,new Event(ID,event.matchId(),event.generation(),event.engineRevision(),1,514,event.finishedAtMillis(),event.participants()));
    }
    @Test void everyMemberOfWinningTeamGetsWinIncludingEarlierDeadMembers() {
        var event=with(List.of(member(1,7,1,0,Verdict.WIN),member(2,7,2,1,Verdict.LOSS),member(3,42,1,0,Verdict.WIN)));
        assertEquals(2,event.participants().stream().filter(p->p.verdict()==Verdict.WIN).count());
    }
    @Test void participantRejectsInvalidHandleTeamIdentityAndVerdict() {
        for(long entity:new long[]{Long.MIN_VALUE,0,101,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->member(entity,1,1,0,Verdict.WIN));
        for(int team:new int[]{-1,2,Integer.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->member(1,1,1,team,Verdict.WIN));
        assertThrows(NullPointerException.class,()->new Participant(1,(TournamentPresence.CharacterKey)null,0,Verdict.WIN));
        assertThrows(NullPointerException.class,()->member(1,1,1,0,null));
    }
    @Test void zeroUuidAndNonpositiveMatchContextAreRejected() {
        var rows=event().participants();assertThrows(IllegalArgumentException.class,()->new Event(new UUID(0,0),1,1,1,1,514,0,rows));
        for(int field=0;field<3;field++)for(long value:new long[]{0,-1,Long.MIN_VALUE}) {
            long match=field==0?value:1,generation=field==1?value:1,revision=field==2?value:1;
            assertThrows(IllegalArgumentException.class,()->new Event(ID,match,generation,revision,1,514,0,rows));
        }
        assertThrows(IllegalArgumentException.class,()->new Event(ID,1,1,1,1,514,-1,rows));
    }
    @Test void onlyNativeLegalDeathmatchPairsAreAccepted() {
        for(int mode:new int[]{0,2,3,4,5,6})assertThrows(IllegalArgumentException.class,()->new Event(ID,1,1,1,mode,514,0,event().participants()));
        assertThrows(IllegalArgumentException.class,()->new Event(ID,1,1,1,1,0,0,event().participants()));
    }
    @Test void participantCountsMustBeBetweenTwoAndOneHundred() {
        assertThrows(IllegalArgumentException.class,()->with(List.of()));
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,1,1,0,Verdict.WIN))));
        var rows=new ArrayList<Participant>();for(int i=1;i<=100;i++)rows.add(member(i,7,i,i%2,i%2==0?Verdict.WIN:Verdict.LOSS));
        assertEquals(100,with(rows).participants().size());rows.add(rows.get(0));assertThrows(IllegalArgumentException.class,()->with(rows));
    }
    @Test void entitiesAreExactOneThroughNAndKeysAreUniqueSourceOrdered() {
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(2,7,1,0,Verdict.WIN),member(1,42,1,1,Verdict.LOSS))));
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,42,1,0,Verdict.WIN),member(2,7,1,1,Verdict.LOSS))));
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,7,2,0,Verdict.WIN),member(2,7,1,1,Verdict.LOSS))));
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,7,1,0,Verdict.WIN),member(2,7,1,1,Verdict.LOSS))));
    }
    @Test void bothTeamsMustExistWithExactlyOneConsistentWinningTeam() {
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,7,1,0,Verdict.WIN),member(2,7,2,0,Verdict.WIN))));
        for(var verdict:Verdict.values())assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,7,1,0,verdict),member(2,7,2,1,verdict))));
        assertThrows(IllegalArgumentException.class,()->with(List.of(member(1,7,1,0,Verdict.WIN),member(2,7,2,1,Verdict.LOSS),member(3,7,3,0,Verdict.LOSS))));
    }
    @Test void canonicalEventIncludesAllFieldsAndHasStableIndependentBytes() throws Exception {
        String actual=java.util.HexFormat.of().formatHex(ModernMatchSettlementJdbc.canonical(event()));
        String expected="0000000112345678123456789abc123456789abc0000000000000007000000000000000b0000000000000013"
                +"000000010000020200000000075bcd1500000002"
                +"0000000000000001000000070000000000000d050000"
                +"00000000000000020000002a0000000000000d050101";
        assertEquals(expected,actual);assertEquals(108,ModernMatchSettlementJdbc.canonical(event()).length);
        assertFalse(java.util.Arrays.equals(ModernMatchSettlementJdbc.canonical(event()),ModernMatchSettlementJdbc.canonical(event(UUID.randomUUID()))));
    }
    @Test void maximumCanonicalFrameIsBoundedAndErrorsAreRedacted() throws Exception {
        var rows=new ArrayList<Participant>();for(int i=1;i<=100;i++)rows.add(member(i,7,i,i%2,i%2==0?Verdict.WIN:Verdict.LOSS));
        assertEquals(2264,ModernMatchSettlementJdbc.canonical(with(rows)).length);
        assertTrue(ModernMatchSettlementJdbc.canonical(with(rows)).length<=ModernMatchSettlementJdbc.MAX_EVENT_BYTES);
        assertEquals("MatchSettlementEvent[redacted]",event().toString());
        assertEquals("SettlementParticipant[redacted]",event().participants().get(0).toString());
        for(var failure:Failure.values()) {var error=new SettlementException(failure);assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);}
    }
}
