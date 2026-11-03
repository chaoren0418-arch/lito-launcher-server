package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import static local.lito381.server.ModernBattlePlayerHealingTest.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual durable equipment authority and one live engine, not a wire/UI test. */
class ModernPlayerHealingEquipmentTest {
    @TempDir Path root;
    ModernBattleCanonicalEquipmentTest.Fixture fixture()throws Exception {
        return new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                ModernBattleEngine.PreparedMember::withPlayerHealing,ModernGreaterHasteEquipmentTest::enoughMp);
    }
    @Test void durableGearRepricesFutureHealingWithoutRewritingOldCostsOrReceipts()throws Exception {
        try(var f=fixture()) {
            var gear=f.read();active(f.world);assertEquals(NONE,heal(f.world,0,1,0).reason());
            var before=f.capture();var old=before.actors().get(0);var stale=request(before,0,2,0);
            var changed=f.set(gear,false);assertEquals(ModernEquipmentAuthority.OK,changed.equipment().status());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());verify(changed.observation());
            var after=changed.observation().actors().get(0);var first=old.playerHealing().outgoing().spending();var repriced=after.playerHealing().outgoing().spending();
            assertTrue(repriced.current().cost()>first.current().cost());
            assertEquals(first.last(),repriced.last());assertEquals(first.current().spent(),repriced.current().spent());
            assertEquals(old.playerHealing().incoming(),after.playerHealing().incoming());assertEquals(old.runtime(),after.runtime());
            assertEquals(STALE_STATE,f.world.engine.playerHeal(f.player().owner(),stale).reason());
            f.world.elapsed(2*SECOND);var second=heal(f.world,0,3,1);assertEquals(NONE,second.reason());
            var sent=second.observation().actors().get(0).playerHealing().outgoing().spending();
            assertEquals(2,sent.current().casts());assertEquals(first.current().spent()+repriced.current().cost(),sent.current().spent());
            assertEquals(first.current().spent(),sent.current().priorSpent());assertEquals(1,sent.last().before().equipmentRevision());
            assertEquals(1,f.stored().orElseThrow().revision());
        }
    }
    @Test void deathAndRespawnRetainHistoricalHealingButOldLifeCannotCastAgain()throws Exception {
        try(var f=fixture()) {
            active(f.world);assertEquals(NONE,heal(f.world,0,1,0).reason());
            var before=f.capture();var stale=request(before,0,2,0);f.killAndRevive();var after=f.capture();verify(after);
            assertTrue(after.actors().get(0).resources().life()>before.actors().get(0).resources().life());
            assertEquals(before.actors().get(0).playerHealing(),after.actors().get(0).playerHealing());
            assertEquals(before.actors().get(0).runtime().playerHealing(),after.actors().get(0).runtime().playerHealing());
            assertEquals(STALE_STATE,f.world.engine.playerHeal(f.player().owner(),stale).reason());
            assertEquals(NONE,heal(f.world,0,3,0).reason());assertEquals(2,f.capture().actors().get(0).playerHealing().incoming().received());
        }
    }
    @Test void potionsNaturalRegenAndHealingRemainSeparateAfterActualDamageAndGearCommit()throws Exception {
        try(var f=fixture()) {
            var gear=f.read();active(f.world);assertEquals(ModernSpellProtocol.Reason.NONE,f.world.cast(1,1,4,1).reason());
            assertEquals(NONE,heal(f.world,0,1,0).reason());var healed=f.capture().actors().get(0).playerHealing();
            assertTrue(healed.incoming().restored()>0);var potion=f.use(1);assertEquals(ModernItemProtocol.Reason.NONE,potion.reason());
            var consumed=potion.observation().actors().get(0);assertTrue(consumed.regeneration().itemGain()>0);
            assertEquals(consumed.regeneration().itemGain()+healed.incoming().restored(),consumed.resources().flows().restored());
            assertEquals(healed,consumed.playerHealing());f.world.elapsed(60*SECOND);var regenerated=f.capture();verify(regenerated);
            var live=regenerated.actors().get(0);assertTrue(live.resources().flows().mpGain()>0);
            assertEquals(healed,live.playerHealing());assertEquals(consumed.regeneration().itemGain(),live.regeneration().itemGain());
            var committed=f.set(gear,false);verify(committed.observation());var finalActor=committed.observation().actors().get(0);
            assertEquals(live.playerHealing().incoming(),finalActor.playerHealing().incoming());
            assertEquals(live.resources().flows().restored(),finalActor.resources().flows().restored());
            assertEquals(live.resources().flows().mpGain(),finalActor.resources().flows().mpGain());
        }
    }
    @Test void targetDurableGearCommitDuringRandomIsRetainedAndRejectsTheStaleHeal()throws Exception {
        try(var f=fixture()) {
            var gear=f.read();active(f.world);var before=f.capture();var request=request(before,1,1,0);
            var committed=new java.util.concurrent.atomic.AtomicReference<ModernBattleObservation.Capture>();
            f.world.randomOverride=bound->{f.world.randomOverride=null;var set=assertDoesNotThrow(()->f.set(gear,false));
                assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,set.publication().disposition());committed.set(set.observation());return bound-1;};
            var result=f.world.engine.playerHeal(f.world.players.get(1).owner(),request);assertEquals(STALE_TARGET,result.reason());verify(result.observation());
            assertNotNull(committed.get());var target=result.observation().actors().get(0);var caster=result.observation().actors().get(1);
            assertEquals(committed.get().actors().get(0).resources(),target.resources());
            assertEquals(committed.get().actors().get(0).playerHealing(),target.playerHealing());assertEquals(1,target.resources().equipmentRevision());
            assertEquals(before.actors().get(1).resources().flows().spent(),caster.resources().flows().spent());assertNull(caster.runtime().playerHealing());
            assertEquals(1,f.stored().orElseThrow().revision());assertEquals(NONE,heal(f.world,1,2,0).reason());
        }
    }
}
