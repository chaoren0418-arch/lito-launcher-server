package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import org.junit.jupiter.api.Test;

class ModernHasteReceiptTest {
    static ModernSpellRuntime.Receipt replace(ModernSpellRuntime.Receipt r,ModernSpellRuntime.Detail d) {
        return new ModernSpellRuntime.Receipt(r.batch(),r.casts(),r.spent(),r.atNanos(),d);
    }
    @Test void truncatedDurationWrongTargetIdAndGenericReceiptCannotImpersonateSkill43()throws Exception {
        try(var f=fixture()) {
            active(f);var c=cast(f,0,1,1).observation();var r=c.actors().get(0).runtime().receipts().get(LANE);
            var d=(ModernHasteReceipt)r.detail();var p=(ModernHasteReceipt.Player)d.target();var a=p.after();
            var shortLease=new ModernTimedEffects.State(a.entityId(),a.revision(),a.observedNanos(),a.hasteUntilNanos()-1,a.braveUntilNanos(),a.wisdomUntilNanos(),a.braveKind(),a.hasteOrigin());
            assertThrows(IllegalArgumentException.class,()->new ModernHasteReceipt(d.activeElapsedNanos(),new ModernHasteReceipt.Player(p.participant(),p.life(),p.equipmentRevision(),p.cell(),p.before(),shortLease)));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteReceipt.Player(r.batch().caster(),p.life(),p.equipmentRevision(),p.cell(),p.before(),p.after()));
            assertThrows(IllegalArgumentException.class,()->replace(r,ModernSpellRuntime.Ordinary.INSTANCE));
            assertThrows(IllegalArgumentException.class,()->replace(r,new ModernSpellRuntime.GreaterHaste(0,2400_000_000_000L)));
        }
    }
    @Test void receiptCannotClaimSelfUnderAnotherIdentityOrDifferentLife()throws Exception {
        try(var f=fixture()) {
            active(f);var c=cast(f,0,1,0).observation();var r=c.actors().get(0).runtime().receipts().get(LANE);
            var d=(ModernHasteReceipt)r.detail();var p=(ModernHasteReceipt.Player)d.target();
            var wrongIdentity=new ModernTargetHistory.Participant(1,ModernParticipantId.human(42,3333));
            for(var participant:java.util.List.of(p.participant(),wrongIdentity)) {
                var changed=new ModernHasteReceipt(0,new ModernHasteReceipt.Player(participant,p.life()+1,p.equipmentRevision(),p.cell(),p.before(),p.after()));
                assertThrows(IllegalArgumentException.class,()->replace(r,changed));
            }
        }
    }
    @Test void npcReceiptCannotBorrowAnotherWorldOrALaterEngineRevision()throws Exception {
        try(var f=ModernBattleNpcHasteTest.fixture(false,2,0,300)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,11,10);
            var c=ModernBattleNpcHasteTest.cast(f,0,1,n).observation();var r=c.actors().get(0).runtime().receipts().get(LANE);
            var d=(ModernHasteReceipt)r.detail();var p=(ModernHasteReceipt.Npc)d.target();var s=p.id().scope();
            var foreign=new ModernNpcRoster.Id(new ModernNpcRoster.Scope(s.roomId(),s.matchId(),s.generation()+1),p.id().serial());
            assertThrows(IllegalArgumentException.class,()->replace(r,new ModernHasteReceipt(0,new ModernHasteReceipt.Npc(foreign,p.life(),p.lane(),p.cell(),p.rosterRevision(),p.before(),p.after()))));
            var later=ModernNpcSpeed.haste(p.before(),0,r.batch().use().revision()+1);
            assertThrows(IllegalArgumentException.class,()->replace(r,new ModernHasteReceipt(0,new ModernHasteReceipt.Npc(p.id(),p.life(),p.lane(),p.cell(),r.batch().use().revision(),p.before(),later))));
        }
    }
}
