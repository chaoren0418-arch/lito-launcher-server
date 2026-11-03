package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Synthetic shared S32 geometry, NOT a claim that LT's original siege
 * coordinates, HP, sprite actions or physical maps have been recovered. */
class ModernSiegeTest {
    static ModernWorldMap.Cell cell(int x,int y){return ModernBattleEngineBotTest.cell(x,y);}
    static ModernWorldBinding binding(int logical)throws Exception {
        return new ModernWorldBinding(3,logical,ModernWorldMap.fromSectorBytes(4,
                Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false));
    }
    static ModernSiege.Config config(int logical,long hp,long duration,long respawn){
        var gate=new HashSet<ModernWorldMap.Cell>();for(int y=0;y<64;y++)gate.add(cell(30,y));
        return new ModernSiege.Config(new ModernWorldBinding.Key(3,logical),List.of(
                new ModernSiege.Tower(ModernSiege.Slot.OUTER_LEFT,cell(10,9),2665,hp),
                new ModernSiege.Tower(ModernSiege.Slot.OUTER_RIGHT,cell(12,9),2667,hp),
                new ModernSiege.Tower(ModernSiege.Slot.INNER,cell(32,9),2669,hp)),gate,
                List.of(cell(10,10),cell(11,10)),List.of(cell(31,10),cell(32,10)),
                new ModernSiege.Portal(cell(29,10),cell(31,11)),duration,respawn);
    }
    static ModernSiege.Config gate(ModernSiege.Config c,Set<ModernWorldMap.Cell> gate){return new ModernSiege.Config(c.key(),c.towers(),gate,c.attackCamp(),c.defenseCamp(),c.portal(),c.durationMillis(),c.respawnMillis());}
    @Test void threeLogicalMapsRequireExplicitPhysicalBindingAndRealCutset()throws Exception {
        for(int logical:List.of(547,563,573)) {
            var b=binding(logical);var c=config(logical,100,1_200_000,3500);var space=c.qualify(b,()->false);
            assertEquals(4,space.map().mapId());assertFalse(space.outside().contains(cell(31,10)));assertTrue(space.inside().contains(cell(31,10)));
            assertFalse(space.step(cell(29,10),cell(30,10),false));assertTrue(space.step(cell(29,10),cell(30,10),true));
            assertFalse(space.teleport(cell(10,10),cell(31,10),false));assertTrue(space.teleport(cell(10,10),cell(31,10),true));
            assertFalse(space.teleport(cell(10,10),cell(32,9),true));assertFalse(space.step(cell(31,9),cell(32,9),true));
            assertTrue(space.rayStep(cell(31,9),cell(32,9),true,cell(32,9)));
        }
    }
    @Test void ornamentalOneCellDoorOrAnyGateGapIsRejected()throws Exception {
        var b=binding(547);var c=config(547,100,60_000,1000);
        assertThrows(IllegalArgumentException.class,()->gate(c,Set.of(cell(30,10))).qualify(b,()->false));
        for(int y:List.of(0,1,10,31,62,63)){var g=new HashSet<>(c.gate());g.remove(cell(30,y));
            assertThrows(IllegalArgumentException.class,()->gate(c,g).qualify(b,()->false));}
    }
    @Test void diagonalRequiresOneWholeStaticAndDynamicCardinalRoute()throws Exception {
        var c=config(547,100,60_000,1000);var space=c.qualify(binding(547),()->false);
        assertFalse(space.step(cell(29,9),cell(30,10),false));assertTrue(space.step(cell(29,9),cell(30,10),true));
        assertFalse(space.rayStep(cell(29,9),cell(30,10),false,cell(31,11)));
        assertTrue(space.step(cell(9,9),cell(10,10),false)); // One clear route around an outer tower.
    }
    @Test void invalidRolesOverlapUnknownHpAndCancellationFailBeforePublishing()throws Exception {
        var c=config(547,100,60_000,1000);var b=binding(547);
        assertThrows(IllegalArgumentException.class,()->c.qualify(binding(563),()->false));
        assertThrows(IllegalArgumentException.class,()->new ModernSiege.Tower(ModernSiege.Slot.INNER,cell(1,1),2665,0));
        assertThrows(IllegalArgumentException.class,()->new ModernSiege.Config(c.key(),c.towers(),c.gate(),c.attackCamp(),c.attackCamp(),c.portal(),60_000,1000));
        assertThrows(IllegalStateException.class,()->c.qualify(b,()->true));
        var calls=new AtomicInteger();assertThrows(IllegalStateException.class,()->c.qualify(b,()->calls.incrementAndGet()>10));
        assertTrue(calls.get()<=12);assertEquals(0,ModernSiege.assignAttacker(()->0));assertEquals(1,ModernSiege.assignAttacker(()->1));
        assertThrows(IllegalArgumentException.class,()->ModernSiege.assignAttacker(()->2));
    }
    static final ModernSiege.Scope SCOPE=new ModernSiege.Scope(17,41,3);
    static ModernSiege.Target target(ModernSiege.Slot slot){return new ModernSiege.Target(SCOPE,slot);}
    @Test void exactTwoOuterThenInnerOrderIndependentOfWhichTeamWasAssignedAttacker(){
        for(int attacker=0;attacker<2;attacker++) {
            int team=attacker;var c=config(547,7,60_000,1000);var start=ModernSiege.initial(SCOPE,c,attacker).activate(2);
            assertFalse(start.attackable(1-attacker,ModernSiege.Slot.OUTER_LEFT));assertFalse(start.attackable(attacker,ModernSiege.Slot.INNER));
            assertThrows(IllegalArgumentException.class,()->start.damage(target(ModernSiege.Slot.INNER),team,1,7,1,3));
            var first=start.damage(target(ModernSiege.Slot.OUTER_RIGHT),attacker,1,7,10,3);assertFalse(first.open());
            var second=first.damage(target(ModernSiege.Slot.OUTER_LEFT),attacker,1,7,20,4);assertTrue(second.open());assertEquals(4,second.gateRevision());
            var done=second.damage(target(ModernSiege.Slot.INNER),attacker,1,7,30,5);
            assertTrue(done.finished());assertEquals(attacker,done.winner());assertEquals(5,done.finishRevision());assertEquals(30,done.finishedAtMillis());
            assertSame(done,done.timeout(60_000,6));assertSame(done,done.abort(6));
        }
    }
    @Test void deadlineHasPriorityOverSameTimeLethalHitAndNeverAwardsDefenderOnAbort(){
        var start=ModernSiege.initial(SCOPE,config(547,1,50,1000),1).activate(2);
        assertSame(start,start.timeout(49,3));var timed=start.timeout(50,3);
        assertTrue(timed.finished());assertEquals(0,timed.winner());assertEquals(50,timed.finishedAtMillis());
        assertThrows(IllegalArgumentException.class,()->start.damage(target(ModernSiege.Slot.OUTER_LEFT),1,1,1,50,3));
        var aborted=start.abort(3);assertEquals(ModernSiege.Phase.ABORTED,aborted.phase());assertEquals(-1,aborted.winner());
        assertSame(aborted,aborted.timeout(100,4));
    }
    @Test void wideHpDamageAndRespawnLedgersHaveNoNarrowCastOrSaturatingCredit(){
        var start=ModernSiege.initial(SCOPE,config(547,Long.MAX_VALUE,60_000,1000),0).activate(2);
        var hit=start.damage(target(ModernSiege.Slot.OUTER_LEFT),0,1,Long.MAX_VALUE-2,1,3);
        assertEquals(2,hit.objectives().get(0).hp());assertEquals(Long.MAX_VALUE-2,hit.objectives().get(0).damageLoss());
        var dead=hit.damage(target(ModernSiege.Slot.OUTER_LEFT),0,1,2,2,4);assertEquals(Long.MAX_VALUE,dead.objectives().get(0).damageLoss());
        assertThrows(IllegalArgumentException.class,()->dead.damage(target(ModernSiege.Slot.OUTER_LEFT),0,1,0,3,5));
        var life=ModernSiege.Life.initial(1).died(3,100,1200);assertEquals(1300,life.dueMillis());
        var restored=life.respawn(4,Long.MAX_VALUE);assertEquals(Long.MAX_VALUE,restored.hpGain());
        assertThrows(ArithmeticException.class,()->restored.died(5,2000,1200).respawn(6,1));
        assertEquals(0,life.terminal().dueMillis());assertThrows(IllegalArgumentException.class,()->life.terminal().respawn(4,1));
    }
    @Test void crossRoomGenerationAndStaleObjectiveRevisionAreRejected(){
        var start=ModernSiege.initial(SCOPE,config(547,1,60_000,1000),0).activate(2);
        assertThrows(IllegalArgumentException.class,()->start.damage(new ModernSiege.Target(new ModernSiege.Scope(17,41,4),ModernSiege.Slot.OUTER_LEFT),0,1,1,1,3));
        assertThrows(IllegalArgumentException.class,()->start.damage(target(ModernSiege.Slot.OUTER_LEFT),0,1,1,1,2));
        assertThrows(IllegalArgumentException.class,()->start.damage(target(ModernSiege.Slot.OUTER_LEFT),1,1,1,1,3));
    }
}
