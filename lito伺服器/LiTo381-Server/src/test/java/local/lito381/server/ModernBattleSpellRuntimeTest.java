package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernBattleSpellRuntimeTest {
    static ModernSpellRuntime.Snapshot read(ModernBattleEngine e,ModernRooms.FrozenPreparation t,ModernBattleEngineBotTest.Player p) {
        return e.spellRuntime(p.owner(),p.scope(),t.matchId(),t.generation());
    }
    @Test void actualHonorFiveLanesShareDenialsCooldownLawfulAndTeleportDestination()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,10000,1000,500,false)) {
            f.active();assertEquals(ModernDisintegrateState.Reason.NONE,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());
            var first=read(f.engine,f.ticket,f.players.get(0));
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,4,2).reason());
            var denied=read(f.engine,f.ticket,f.players.get(0));assertEquals(2,denied.spells().sequence());assertEquals(first.spells().receipts(),denied.spells().receipts());
            assertEquals(first.history().resources(),denied.history().resources());
            f.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,4,2).reason());
            f.elapsed(4_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,4,70,0).reason());
            f.elapsed(6_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,5,1,3).reason());
            f.elapsed(8_000_000_000L);var teleport=f.cast(0,6,5,0);assertEquals(ModernSpellProtocol.Reason.NONE,teleport.reason());
            var state=read(f.engine,f.ticket,f.players.get(0));assertEquals(6,state.spells().sequence());assertEquals(501,state.spells().lawful());
            assertEquals(5,state.spells().receipts().size());var receipt=state.spells().receipts().get(ModernUseHistory.Lane.TELEPORT);
            assertEquals(teleport.snapshot().actors().get(0).position(),((ModernSpellRuntime.Teleport)receipt.detail()).destination());
            assertEquals(state.spells().requestRevision(),state.history().worldRevision());
            assertThrows(SecurityException.class,()->f.cast(0,6,5,0));assertEquals(state,read(f.engine,f.ticket,f.players.get(0)));
            // Existing negotiated ceiling is v43 (NPC motion); internal skill43
            // must not itself add another network version or legacy SET route.
            assertEquals(47,ModernSessionProtocol.MAX_VERSION);
            assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(f.players.get(0).owner(),f.players.get(0).scope(),f.ticket.matchId(),f.ticket.generation()));
        }
    }
    @Test void actualPlayerAndNpcCriticalVariantsSurviveWithoutReadingLegacySerializers()throws Exception {
        for(boolean critical:new boolean[]{false,true}) {
            try(var f=ModernBattleEngineDisintegrateTest.fixture(false,0,100,300,500,false)) {
                f.active();f.randomOverride=b->b==100?(critical?0:99):0;
                var result=ModernBattleEngineDisintegrateTest.cast(f,1,2);assertEquals(ModernDisintegrateState.Reason.NONE,result.reason());
                var state=read(f.engine,f.ticket,f.players.get(0));var d=(ModernSpellRuntime.Disintegrate)state.spells().receipts().get(ModernUseHistory.Lane.DISINTEGRATE).detail();
                assertEquals(critical,d.critical());assertEquals(500,d.lawfulBefore());assertEquals(501,d.lawfulAfter());
                assertEquals(0,state.history().targets().get(ModernUseHistory.Lane.DISINTEGRATE).last().targets().get(0).hpAfter());
                assertEquals(ModernBattleEngine.Phase.FINISHED,result.snapshot().phase());
            }
            try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
                f.active();var n=f.spawn(0,11,10);f.randomValue=critical?0:99;
                assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,n).reason());
                var state=read(f.engine,f.ticket,f.players.get(0));var receipt=state.spells().receipts().get(ModernUseHistory.Lane.NPC_DISINTEGRATE);
                assertEquals(critical,((ModernSpellRuntime.Disintegrate)receipt.detail()).critical());
                assertTrue(receipt.batch().targets().get(0).before().target() instanceof ModernTargetHistory.Npc);
                assertEquals(501,state.spells().lawful());
            }
        }
    }
    @Test void actualNpcThreeLanesAndStructureRetentionUseTheSameOwnerProjection()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);ModernBattleEngineNpcMagicTest.cast(f,0,1,n);f.elapsed(1200);ModernBattleEngineNpcFireStormTest.cast(f,2);
            f.elapsed(2400);ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n);var state=read(f.engine,f.ticket,f.players.get(0));
            assertEquals(Set.of(ModernUseHistory.Lane.NPC_LIGHT_ARROW,ModernUseHistory.Lane.NPC_FIRE_STORM,ModernUseHistory.Lane.NPC_DISINTEGRATE),state.spells().receipts().keySet());
            assertEquals(3,state.spells().sequence());
        }
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);
            var before=read(f.engine,f.ticket,f.players.get(0));f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);
            var after=read(f.engine,f.ticket,f.players.get(0));var lane=ModernUseHistory.Lane.NPC_FIRE_STORM;
            assertEquals(before.history().targets().get(lane).structure(),after.history().targets().get(lane).structure());
            assertEquals(after.history().targets().get(lane).last(),after.spells().receipts().get(lane).batch());
        }
    }
    @Test void ownerReadRejectsForeignScopeOrRetiredWorldAndCannotMutateHistory()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var p=f.players.get(0);var q=f.players.get(1);var before=read(f.engine,f.ticket,p);
            assertThrows(SecurityException.class,()->f.engine.spellRuntime(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.spellRuntime(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1));
            assertEquals(before,read(f.engine,f.ticket,p));
            f.engine.close();assertThrows(IllegalStateException.class,()->read(f.engine,f.ticket,p));
        }
    }
}
