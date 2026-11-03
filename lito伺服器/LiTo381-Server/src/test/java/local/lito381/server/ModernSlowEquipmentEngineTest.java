package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSlowCast.Reason.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual durable equipment authority plus a Slow-enabled Honor engine. */
class ModernSlowEquipmentEngineTest {
    @TempDir Path root;
    static ModernPhysicalCombat.Source immune(ModernPhysicalCombat.Source s) {
        var policies=new HashMap<>(s.templates());
        policies.replaceAll((id,p)->new ModernPhysicalCombat.TemplatePolicy(p.kind(),p.className(),p.criticalMultiplier(),OptionalLong.of(1)));
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,s.procCatalogIds(),s.magicGrowth(),
                s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    static ModernBattleCanonicalEquipmentTest.Fixture fixture(Path root)throws Exception {
        return new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                p->ModernBattleSlowTest.enable(p,6),ModernSlowEquipmentEngineTest::immune);
    }
    static void active(ModernBattleCanonicalEquipmentTest.Fixture f) {
        for(var p:f.world.players)f.world.engine.ackLoadedObservation(p.owner(),p.scope(),f.world.ticket.matchId(),f.world.ticket.generation(),f.world.map.digest());
        assertEquals(ModernBattleEngine.Phase.ACTIVE,f.capture().phase());f.world.randomOverride=b->0;
    }
    static ModernBattleEngine.SlowResult cast(ModernBattleCanonicalEquipmentTest.Fixture f,int caster,int target) {
        var c=f.capture();var result=f.world.engine.slow(f.world.players.get(caster).owner(),
                ModernBattleSlowTest.request(c,caster,c.actors().get(caster).runtime().sequence()+1,ModernBattleSlowTest.target(c,target)));
        assertEquals(NONE,result.reason());ModernBattleSlowTest.consistent(result.observation());return result;
    }
    static ModernSlowReceipt detail(ModernBattleObservation.Capture c,int caster) {
        return (ModernSlowReceipt)c.actors().get(caster).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail();
    }
    @Test void durableUnequipRepricesWithoutLosingMaterialsAndReequipCannotEraseAnEarlierSlow()throws Exception {
        try(var f=fixture(root)) {
            var gear=f.read();active(f);var first=cast(f,0,0).observation();
            assertEquals(ModernSlowReceipt.Outcome.EQUIPMENT_IMMUNE,detail(first,0).outcome());
            assertFalse(first.actors().get(0).buffs().hasEffects());assertEquals(1,first.actors().get(0).materials().uses());
            long cost=first.actors().get(0).histories().get(ModernUseHistory.Lane.SLOW).current().cost();
            var changed=f.set(gear,false);assertEquals(ModernEquipmentAuthority.OK,changed.equipment().status());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
            var after=changed.observation();ModernBattleSlowTest.consistent(after);
            assertEquals(first.actors().get(0).materials(),after.actors().get(0).materials());
            assertEquals(first.actors().get(0).weight().view().totalWeight(),after.actors().get(0).weight().view().totalWeight());
            assertEquals(cost,after.actors().get(0).resources().flows().spent());
            assertTrue(after.actors().get(0).histories().get(ModernUseHistory.Lane.SLOW).current().cost()>cost);
            f.world.elapsed(2_000_000_000L);var applied=cast(f,0,0).observation();assertTrue(applied.actors().get(0).buffs().slowUntilNanos()>0);
            assertEquals(2,applied.actors().get(0).materials().uses());
            var returned=f.set(f.read(),true);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,returned.publication().disposition());
            assertEquals(applied.actors().get(0).buffs(),returned.observation().actors().get(0).buffs());
            var immune=cast(f,1,0).observation();assertEquals(ModernSlowReceipt.Outcome.EQUIPMENT_IMMUNE,detail(immune,1).outcome());
            assertEquals(applied.actors().get(0).buffs(),immune.actors().get(0).buffs());
            assertEquals(2,f.stored().orElseThrow().revision());ModernBattleSlowTest.consistent(immune);
        }
    }
    @Test void equipmentChangeInsideRandomRejectsOldCasterProofWithoutMaterialOrMpCharge()throws Exception {
        try(var f=fixture(root)) {
            var gear=f.read();active(f);f.world.randomOverride=b->{
                f.world.randomOverride=x->0;
                try{assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,f.set(gear,false).publication().disposition());}
                catch(Exception failure){throw new AssertionError(failure);}return 0;
            };
            var before=f.capture();var result=f.world.engine.slow(f.player().owner(),
                    ModernBattleSlowTest.request(before,0,1,ModernBattleSlowTest.target(before,1)));
            assertEquals(STALE_STATE,result.reason());var c=result.observation();assertEquals(0,c.actors().get(0).materials().uses());
            assertEquals(0,c.actors().get(0).resources().flows().spent());assertEquals(1,c.actors().get(0).resources().equipmentRevision());ModernBattleSlowTest.consistent(c);
        }
    }
}
