package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import static local.lito381.server.ModernHasteCast.Reason.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernHasteEquipmentTest {
    @TempDir Path root;
    ModernBattleCanonicalEquipmentTest.Fixture fixture()throws Exception {
        return new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                p->enable(p,ModernHasteCatalog.Status.QUALIFIED),ModernGreaterHasteEquipmentTest::enoughMp);
    }
    static void active(ModernBattleCanonicalEquipmentTest.Fixture f) {ModernGreaterHasteEquipmentTest.active(f);}
    static ModernHasteCast.Request intent(ModernBattleCanonicalEquipmentTest.Fixture f,long seq) {
        var c=f.capture();return request(c,0,seq,target(c,1));
    }
    @Test void durableEquipmentSetRepricesFutureSkill43WithoutRewritingVictimOrHistoricalSpend()throws Exception {
        try(var f=fixture()) {
            var gear=f.read();active(f);var first=f.world.engine.haste(f.player().owner(),intent(f,1));assertEquals(NONE,first.reason());
            var before=first.observation();var caster=before.actors().get(0);assertEquals(35,caster.histories().get(LANE).current().spent());
            var stale=intent(f,2);var proof=new ModernBattleCommandLife.Proof(f.player().scope(),before.identity(),1,caster.resources().life()+1,before.revision());
            var changed=f.call(op->f.authority.executeInMatch(f.principal,f.player().row(),op,
                    new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.SET,false,1,gear.revision(),gear.viewToken()),f.reserve(proof)));
            assertEquals(ModernEquipmentAuthority.OK,changed.equipment().status());assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
            var after=changed.observation();var a=after.actors().get(0);assertEquals(40,a.histories().get(LANE).current().cost());
            assertEquals(caster.histories().get(LANE).last(),a.histories().get(LANE).last());assertEquals(caster.runtime(),a.runtime());
            assertEquals(before.actors().get(1).buffs(),after.actors().get(1).buffs());assertEquals(1,a.loadout().haste().equipmentRevision());
            assertEquals(STALE_STATE,f.world.engine.haste(f.player().owner(),stale).reason());f.world.elapsed(2_000_000_000L);
            var next=f.world.engine.haste(f.player().owner(),intent(f,3));assertEquals(NONE,next.reason());var h=history(next.observation(),0);
            assertEquals(75,h.spells().get(LANE).current().spent());assertEquals(35,h.spells().get(LANE).current().priorSpent());
            assertEquals(40,h.spells().get(LANE).last().cost());assertEquals(1,h.spells().get(LANE).last().before().equipmentRevision());
            assertEquals(75,h.resources().flows().spent());assertEquals(1,f.stored().orElseThrow().revision());
        }
    }
    @Test void targetDeathRespawnClearsLeaseAndOldTargetLifeCannotBeReused()throws Exception {
        try(var f=fixture()) {
            active(f);var c=f.capture();assertEquals(NONE,f.world.engine.haste(f.player().owner(),request(c,0,1,target(c,0))).reason());
            var old=f.capture();var staleTarget=target(old,0);var cost=old.actors().get(0).resources().flows().spent();
            f.killAndRevive();var after=f.capture();assertFalse(after.actors().get(0).buffs().hasEffects());
            assertTrue(after.actors().get(0).resources().life()>old.actors().get(0).resources().life());assertEquals(cost,after.actors().get(0).resources().flows().spent());
            var enemy=f.world.players.get(1);var seq=after.actors().get(1).runtime().sequence()+1;
            assertEquals(STALE_TARGET,f.world.engine.haste(enemy.owner(),request(after,1,seq,staleTarget)).reason());
        }
    }
}
