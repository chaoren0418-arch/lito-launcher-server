package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleGreaterHasteTest.*;
import static local.lito381.server.ModernGreaterHasteCatalog.Status.QUALIFIED;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernGreaterHasteEquipmentTest {
    @TempDir Path root;
    static ModernPhysicalCombat.Source enoughMp(ModernPhysicalCombat.Source s) {
        var changed=ModernMagicCombatTest.stats(s,12,17,0,15,500);
        return new ModernPhysicalCombat.Source(changed.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),
                s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    static ModernGreaterHasteCast.Request request(ModernBattleCanonicalEquipmentTest.Fixture f,long seq) {
        var a=f.capture().actors().get(0);var p=f.player();
        return new ModernGreaterHasteCast.Request(p.scope(),f.world.ticket.matchId(),f.world.ticket.generation(),seq,
                a.resources().equipmentRevision(),a.resources().life()+1,a.pose().position().x(),a.pose().position().y(),a.buffs().revision());
    }
    static void active(ModernBattleCanonicalEquipmentTest.Fixture f) {
        for(var p:f.world.players)f.world.engine.ackLoadedObservation(p.owner(),p.scope(),f.world.ticket.matchId(),f.world.ticket.generation(),f.world.map.digest());
        assertEquals(ModernBattleEngine.Phase.ACTIVE,f.capture().phase());
    }
    @Test void realDurableSetRepricesOnlyFutureCastsAndRetainsLeaseAndHistoricalCost()throws Exception {
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                p->enable(p,QUALIFIED),ModernGreaterHasteEquipmentTest::enoughMp)) {
            var gear=f.read();active(f);
            var first=f.world.engine.greaterHaste(f.player().owner(),request(f,1));assertEquals(ModernGreaterHasteCast.Reason.NONE,first.reason());
            var before=first.observation().actors().get(0);assertEquals(54,before.histories().get(LANE).current().spent());
            var stale=request(f,2);var changed=f.set(gear,false);assertEquals(ModernEquipmentAuthority.OK,changed.equipment().status());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());var after=changed.observation().actors().get(0);
            assertEquals(60,after.histories().get(LANE).current().cost());assertEquals(before.histories().get(LANE).last(),after.histories().get(LANE).last());
            assertEquals(before.buffs(),after.buffs());assertEquals(before.runtime(),after.runtime());assertEquals(1,after.loadout().greaterHaste().equipmentRevision());
            assertEquals(ModernGreaterHasteCast.Reason.STALE_STATE,f.world.engine.greaterHaste(f.player().owner(),stale).reason());
            f.world.elapsed(2_000_000_000L);var second=f.world.engine.greaterHaste(f.player().owner(),request(f,3));assertEquals(ModernGreaterHasteCast.Reason.NONE,second.reason());
            var owner=second.observation().forOwner(f.player().scope()).owner();var history=owner.history().history();
            assertEquals(114,history.spells().get(LANE).current().spent());assertEquals(2,history.spells().get(LANE).current().casts());
            assertEquals(54,history.spells().get(LANE).current().priorSpent());assertEquals(60,history.spells().get(LANE).last().cost());
            assertEquals(1,history.spells().get(LANE).last().before().equipmentRevision());assertEquals(114,history.resources().flows().spent());
            assertEquals(1,f.stored().orElseThrow().revision());assertEquals(ModernSkills.Support.UNIMPLEMENTED,owner.skills().skills().stream().filter(s->s.skillId()==54).findFirst().orElseThrow().support());
        }
    }
    @Test void deathRespawnClearsTheLeaseButKeepsItsCostAndRejectsAnOldLifeRequest()throws Exception {
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                p->enable(p,QUALIFIED),ModernGreaterHasteEquipmentTest::enoughMp)) {
            active(f);assertEquals(ModernGreaterHasteCast.Reason.NONE,f.world.engine.greaterHaste(f.player().owner(),request(f,1)).reason());
            var stale=request(f,2);var before=f.capture().actors().get(0);f.killAndRevive();var after=f.capture().actors().get(0);
            assertFalse(after.buffs().hasEffects());assertTrue(after.resources().life()>before.resources().life());
            assertEquals(before.histories().get(LANE),after.histories().get(LANE));
            assertEquals(ModernGreaterHasteCast.Reason.STALE_STATE,f.world.engine.greaterHaste(f.player().owner(),stale).reason());
        }
    }
}
