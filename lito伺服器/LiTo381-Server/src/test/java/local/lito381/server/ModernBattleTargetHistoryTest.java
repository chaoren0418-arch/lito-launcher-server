package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernBattleTargetHistoryTest {
    static ModernTargetHistory.History history(ModernBattleEngine engine,ModernRooms.FrozenPreparation ticket,ModernBattleEngineBotTest.Player player,ModernUseHistory.Lane lane) {
        return ModernBattleUseHistoryTest.read(engine,ticket,player).targets().get(lane);
    }
    @Test void actualAoeSeparatesHumanBotAndHonorObjectivesAndKeepsEventCapacities() throws Exception {
        for(boolean honor:List.of(false,true))try(var f=ModernBattleEngineFireStormTest.fixture(honor,honor?0:2,1000,40)) {
            var before=f.active();var reply=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());
            var h=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM);
            assertEquals(reply.snapshot().revision(),h.last().use().revision());assertEquals(2,h.last().targets().size());
            var pc=h.last().targets().get(0);var other=h.last().targets().get(1);
            assertEquals(before.vitals().get(1).maxHp(),pc.before().maximumHp());assertEquals(40,pc.before().hp());assertTrue(pc.death());
            var id=assertInstanceOf(ModernTargetHistory.Participant.class,pc.before().target());assertEquals(before.actors().get(1).member().identity(),id.identity());
            if(honor) {assertInstanceOf(ModernTargetHistory.Honor.class,other.before().target());assertEquals(1000,other.before().maximumHp());}
            else assertFalse(assertInstanceOf(ModernTargetHistory.Participant.class,other.before().target()).identity().isHuman());
            assertThrows(UnsupportedOperationException.class,()->h.last().targets().clear());
        }
    }
    @Test void deadAndRevivedVictimDoesNotRewriteOldReceiptAndNextHitNamesNextLife() throws Exception {
        try(var f=ModernBattleEngineFireStormTest.fixture(true,0,1000,40)) {
            f.active();f.cast(0,1,70,0);var first=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM);
            f.elapsed(5_000_000_000L);var revived=f.engine.tick();assertEquals(40,revived.vitals().get(1).hp());
            assertEquals(first,history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM));
            f.cast(0,2,70,0);var next=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM);
            assertEquals(1,first.last().targets().get(0).before().life());assertEquals(2,next.last().targets().get(0).before().life());
            assertEquals(40,first.last().targets().get(0).before().maximumHp());assertTrue(ModernTargetHistory.follows(first,next));
        }
    }
    @Test void fullHpHonorHealingHasRealRawHealButZeroAppliedAndOtherLaneIsIndependent() throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());
            var heal=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.HEALING).last().targets().get(0);
            assertEquals(ModernTargetHistory.Kind.HEAL,heal.kind());assertTrue(heal.raw()>0);assertEquals(0,heal.applied());assertEquals(100,heal.hpAfter());
            f.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,4,4).reason());
            assertEquals(heal,history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.HEALING).last().targets().get(0));
            var damage=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.LIGHT_ARROW).last().targets().get(0);
            assertEquals(ModernTargetHistory.Kind.DAMAGE,damage.kind());assertTrue(damage.applied()>0);assertEquals(100,damage.before().maximumHp());
        }
    }
    @Test void invalidFinalVictimRollCannotPublishPartialTargetHistory() throws Exception {
        try(var f=ModernBattleEngineFireStormTest.fixture(true,0,1000,1000)) {
            var before=f.active();f.invalidAt=10;
            assertEquals(ModernSpellProtocol.Reason.NUMERIC,f.cast(0,1,70,0).reason());
            assertNull(history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM));assertEquals(before.vitals(),f.engine.tick().vitals());
            f.invalidAt=0;assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,2,70,0).reason());
            var h=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM);assertEquals(2,h.last().use().sequence());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,3,70,0).reason());assertEquals(h,history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM));
        }
    }
    @Test void actualNpcArrowFireAndDisintegrateKeepTypedLifeAndCatalogCapacity() throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,1,n).reason());
            f.elapsed(1200);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernBattleEngineNpcFireStormTest.cast(f,2).reason());
            f.elapsed(2400);assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n).reason());
            for(var lane:List.of(ModernUseHistory.Lane.NPC_LIGHT_ARROW,ModernUseHistory.Lane.NPC_FIRE_STORM,ModernUseHistory.Lane.NPC_DISINTEGRATE)) {
                var h=history(f.engine,f.ticket,f.players.get(0),lane);var hit=h.last().targets().get(0);
                assertEquals(n.id(),assertInstanceOf(ModernTargetHistory.Npc.class,hit.before().target()).id());
                assertEquals(n.life(),hit.before().life());assertEquals(10000,hit.before().maximumHp());assertEquals(0,hit.before().equipmentRevision());
                assertEquals(h.last().use(),ModernBattleUseHistoryTest.read(f.engine,f.ticket,f.players.get(0)).spells().get(lane).last());
            }
        }
    }
    @Test void actualQuestStructureProofRemainsAfterNextNpcOnlyWave() throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,1).reason());
            f.elapsed(2000);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,2).reason());
            var first=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.NPC_FIRE_STORM);assertNotNull(first.structure());
            var target=first.structure().targets().get(0);assertInstanceOf(ModernTargetHistory.Quest.class,target.before().target());assertTrue(target.death());
            f.elapsed(4000);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,3).reason());
            var next=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.NPC_FIRE_STORM);
            assertEquals(first.structure(),next.structure());assertTrue(next.last().targets().stream().allMatch(t->t.before().target() instanceof ModernTargetHistory.Npc));
        }
    }
    @Test void actualTimeTowerProofRemainsAfterWaveStarts() throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,1)) {
            f.active();assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,1).reason());
            var first=history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.NPC_FIRE_STORM);
            assertNotNull(first.structure());assertInstanceOf(ModernTargetHistory.TimeTower.class,first.structure().targets().get(0).before().target());
            f.elapsed(2000);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,2).reason());
            assertEquals(first.structure(),history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.NPC_FIRE_STORM).structure());
        }
    }
}
