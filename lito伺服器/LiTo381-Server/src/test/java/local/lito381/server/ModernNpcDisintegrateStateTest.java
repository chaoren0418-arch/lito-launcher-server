package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernNpcDisintegrateStateTest {
    static ModernNpcDisintegrateState.Hit hit() {
        return new ModernNpcDisintegrateState.Hit(new ModernNpcRoster.Id(ModernNpcRosterTest.SCOPE,1),1,0,1,
                ModernNpcRosterTest.cell(10,10),ModernNpcRosterTest.cell(11,10),1000,new ModernDisintegrate.Outcome(216,216,784,false,false));
    }
    static ModernBattleEngine.Snapshot replace(ModernBattleEngine.Snapshot w,List<ModernNpcDisintegrateState.State> states) {
        return new ModernBattleEngine.Snapshot(w.matchId(),w.generation(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),
                w.vitals(),w.result(),w.casts(),w.damages(),w.items(),w.weights(),w.buffs(),w.ammo(),w.regen(),w.honor(),w.fireStorms(),w.race(),
                w.isolatedForces(),w.quest(),w.npcSpells(),w.timeAttack(),w.siege(),w.disintegrations(),states);
    }
    static ModernNpcDisintegrateState.State event(ModernNpcDisintegrateState.State s,long revision,long sequence,ModernNpcDisintegrateState.Hit h) {
        return new ModernNpcDisintegrateState.State(s.entityId(),s.castCount(),revision,sequence,s.mpSpent(),s.mpCost(),s.initialLawful(),s.lawful(),h);
    }
    @Test void ledgerHasExactLawfulManaAndMonotonicSequenceWithoutPlayerTargetRestrictions() {
        var zero=ModernNpcDisintegrateState.State.initial(1,70,500);var s=zero.cast(3,2,hit());
        assertEquals(1,s.entityId());assertEquals(1,s.hit().target().serial());assertEquals(501,s.lawful());assertEquals(70,s.mpSpent());assertEquals(5000,s.reuseMillis());
        assertEquals(502,s.cast(4,3,hit()).lawful());
        assertThrows(IllegalArgumentException.class,()->s.cast(3,3,hit()));assertThrows(IllegalArgumentException.class,()->s.cast(4,2,hit()));
        assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateState.State.initial(1,70,499).cast(3,1,hit()));
        assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateState.State.initial(1,70,Long.MAX_VALUE).cast(3,1,hit()));
        assertEquals(0,ModernNpcDisintegrateState.State.initial(2,0,0).reuseMillis());
        assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateState.State.initial(2,0,500));
        for(int cost:List.of(-1,1,58,71))assertThrows(IllegalArgumentException.class,()->ModernNpcDisintegrateState.State.initial(1,cost,500));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.State(1,1,3,1,69,70,500,501,hit()));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.State(1,1,3,1,70,70,500,502,hit()));
        assertThrows(ArithmeticException.class,()->new ModernNpcDisintegrateState.State(1,Long.MAX_VALUE,3,Long.MAX_VALUE,1,70,500,501,hit()));
    }
    @Test void receiptCannotForgeHpDeltaGeometryLaneOrLife() {
        var h=hit();
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.Hit(h.target(),1,0,1,h.from(),h.to(),999,h.outcome()));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.Hit(h.target(),1,0,1,h.from(),h.from(),1000,h.outcome()));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.Hit(h.target(),1,0,1,h.from(),ModernNpcRosterTest.cell(21,10),1000,h.outcome()));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.Hit(h.target(),0,0,1,h.from(),h.to(),1000,h.outcome()));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.Hit(h.target(),1,0,0,h.from(),h.to(),1000,h.outcome()));
        for(int lane:List.of(-1,2))assertThrows(IllegalArgumentException.class,()->new ModernNpcDisintegrateState.Hit(h.target(),1,lane,1,h.from(),h.to(),1000,h.outcome()));
    }
    @Test void combinedManaValidationCannotDropTheNewNpcLedgerOrTreatItAsTeleport()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);var w=ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,npc).frame().world();
            assertThrows(IllegalArgumentException.class,()->replace(w,List.of()));
            var v=w.vitals().get(0);var r=w.regen().get(0);
            ModernRegenProtocol.validateTotals(r,v,0,0,0,0,0,0,0,70);
            assertThrows(IllegalArgumentException.class,()->ModernRegenProtocol.validateTotals(r,v,0,0,0,0,0,0,0,0));
            assertThrows(IllegalArgumentException.class,()->ModernRegenProtocol.validateTotals(r,v,0,0,0,0,0,0,0,-1));
            assertEquals(0,w.casts().get(0).mpSpent());assertEquals(0,w.npcSpells().get(0).mpSpent());
        }
    }
    @Test void snapshotRejectsCrossGenerationFutureLifeSequenceRevisionAndWrongLane()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);var w=ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,npc).frame().world();var s=w.npcDisintegrations().get(0);var h=s.hit();
            var scope=h.target().scope();var alien=new ModernNpcRoster.Id(new ModernNpcRoster.Scope(scope.roomId(),scope.matchId(),scope.generation()+1),1);
            var variants=List.of(
                    event(s,s.revision()+1,s.lastSequence(),h),event(s,s.revision(),s.lastSequence()+1,h),
                    event(s,s.revision(),s.lastSequence(),new ModernNpcDisintegrateState.Hit(alien,1,0,1,h.from(),h.to(),h.hpBefore(),h.outcome())),
                    event(s,s.revision(),s.lastSequence(),new ModernNpcDisintegrateState.Hit(h.target(),1,1,1,h.from(),h.to(),h.hpBefore(),h.outcome())),
                    event(s,s.revision(),s.lastSequence(),new ModernNpcDisintegrateState.Hit(h.target(),1,0,2,h.from(),h.to(),h.hpBefore(),h.outcome())));
            for(var invalid:variants){var states=new ArrayList<>(w.npcDisintegrations());states.set(0,invalid);assertThrows(IllegalArgumentException.class,()->replace(w,states));}
            assertThrows(IllegalArgumentException.class,()->replace(w,List.of(s)));
        }
    }
    @Test void npcFrameRequiresActualLifeHpCellAndSpawnedSerialRatherThanJustAValidWorld()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);var frame=ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,npc).frame();var w=frame.world();var s=w.npcDisintegrations().get(0);var h=s.hit();
            var variants=List.of(
                    new ModernNpcDisintegrateState.Hit(new ModernNpcRoster.Id(h.target().scope(),2),1,0,1,h.from(),h.to(),h.hpBefore(),h.outcome()),
                    new ModernNpcDisintegrateState.Hit(h.target(),2,0,1,h.from(),h.to(),h.hpBefore(),h.outcome()),
                    new ModernNpcDisintegrateState.Hit(h.target(),1,0,1,h.from(),new ModernWorldMap.Cell(h.to().x()+1,h.to().y()),h.hpBefore(),h.outcome()),
                    new ModernNpcDisintegrateState.Hit(h.target(),1,0,1,h.from(),h.to(),10000,new ModernDisintegrate.Outcome(1,1,9999,false,false)));
            for(var receipt:variants) {
                var states=new ArrayList<>(w.npcDisintegrations());states.set(0,event(s,s.revision(),s.lastSequence(),receipt));
                var changed=replace(w,states);assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Frame(changed,frame.npcs(),frame.exchanges()));
            }
        }
    }
    @Test void anotherCastersArrowRevisionCannotBeReusedForAnNpcSkill77Receipt()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var first=f.spawn(0,11,10);var second=f.spawn(1,21,10);
            ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,first);
            assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,1,1,second).reason());
            var w=f.view().world();var s=w.npcDisintegrations().get(0);var states=new ArrayList<>(w.npcDisintegrations());
            states.set(0,event(s,w.npcSpells().get(1).revision(),s.lastSequence(),s.hit()));
            assertThrows(IllegalArgumentException.class,()->replace(w,states));
        }
    }
}
