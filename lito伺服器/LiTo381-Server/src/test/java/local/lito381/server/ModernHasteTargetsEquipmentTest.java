package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import static local.lito381.server.ModernHasteTargetsTest.receipt;
import static local.lito381.server.ModernHasteTargetsTest.key;
import static local.lito381.server.ModernHasteCast.Reason.NONE;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernHasteTargetsEquipmentTest {
    @TempDir Path root;
    ModernBattleCanonicalEquipmentTest.Fixture fixture()throws Exception {
        return new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                p->enable(p,ModernHasteCatalog.Status.QUALIFIED),ModernGreaterHasteEquipmentTest::enoughMp);
    }
    @Test void previousVictimsProofSurvivesCasterRepricingAndLaterCastAtTheNewPrice()throws Exception {
        try(var f=fixture()) {
            var gear=f.read();ModernHasteEquipmentTest.active(f);var c=f.capture();
            var first=f.world.engine.haste(f.player().owner(),request(c,0,1,target(c,1))).observation();var old=receipt(first,0);
            assertEquals(35,old.spent());var proof=new ModernBattleCommandLife.Proof(f.player().scope(),first.identity(),1,first.actors().get(0).resources().life()+1,first.revision());
            var changed=f.call(op->f.authority.executeInMatch(f.principal,f.player().row(),op,
                    new ModernEquipmentAuthority.Request(7,3333,1,ModernEquipmentAuthority.SET,false,1,gear.revision(),gear.viewToken()),f.reserve(proof)));
            assertEquals(ModernEquipmentAuthority.OK,changed.equipment().status());assertEquals(first.hasteTargets(),changed.observation().hasteTargets());
            f.world.elapsed(2_000_000_000L);c=f.capture();var second=f.world.engine.haste(f.player().owner(),request(c,0,2,target(c,0))).observation();
            assertEquals(old,second.hasteTargets().targets().get(key(second,1)).active());assertEquals(40,receipt(second,0).batch().use().cost());
            assertEquals(75,second.actors().get(0).resources().flows().spent());assertEquals(2,second.hasteTargets().targets().size());
            assertEquals(2,ModernBattleWireTest.publication(second,null).world().hasteEffects().targets().size());
        }
    }
    @Test void casterDeathAndNewIncarnationDoNotEraseOtherLivingVictimsGrant()throws Exception {
        try(var f=fixture()) {
            ModernHasteEquipmentTest.active(f);var c=f.capture();var first=f.world.engine.haste(f.player().owner(),request(c,0,1,target(c,1))).observation();
            var old=first.hasteTargets();f.killAndRevive();var revived=f.capture();
            assertTrue(revived.actors().get(0).resources().life()>first.actors().get(0).resources().life());assertEquals(old,revived.hasteTargets());
            assertTrue(revived.actors().get(1).buffs().hasEffects());assertEquals(35,revived.actors().get(0).resources().flows().spent());
            assertDoesNotThrow(()->old.validate(revived));
            assertEquals(1,ModernBattleWireTest.publication(revived,null).world().hasteEffects().targets().size());
        }
    }
}
