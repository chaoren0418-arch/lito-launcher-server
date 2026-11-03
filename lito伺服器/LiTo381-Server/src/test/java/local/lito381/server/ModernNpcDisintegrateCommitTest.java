package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Victim CAS only. Complete engine MP/lawful/sequence publication and the
 * NPC skill77 wire/renderer are separate, mandatory integration work. */
class ModernNpcDisintegrateCommitTest {
    static ModernNpcDisintegrate.Incoming plan(ModernNpcRoster r,ModernNpcCatalog.Catalog c,ModernNpcRoster.Actor n) {
        return ModernNpcDisintegrate.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),n.id(),n.life(),b->b==15?0:99,0);
    }
    @Test void preparedSkill77ProofIsImmutableAndDistinctFromLightArrowOrPlayerTargets()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L,HP,1000L),true,0);
        var pc=ModernNpcDisintegrateTest.caster();
        try(var r=ModernNpcRosterTest.roster()) {
            var n=ModernNpcMagicCommitTest.spawn(r,c);var before=r.snapshot();var hit=plan(r,c,n);
            assertEquals(before,r.snapshot());assertEquals(pc.key(),hit.attacker());
            assertEquals(pc.profile().equipmentRevision(),hit.equipmentRevision());
            assertEquals(pc.profile().evaluatedAtMillis(),hit.evaluatedAtMillis());
            assertEquals(pc.profile().validUntilMillis(),hit.validUntilMillis());
            assertEquals(ModernDisintegrate.qualify(pc).mpCost(),hit.mpCost());
            assertEquals(c.npcs().get(ModernNpcMeleeTest.NPC).key(),hit.catalogKey());
            assertEquals(216,hit.outcome().rawDamage());assertEquals(11746,hit.outcome().effect());
            var after=r.commitIncoming(hit);assertEquals(784,after.hp());assertEquals(ModernNpcRoster.Action.HURT,after.action());
            assertEquals(n.nextAttackMillis(),after.nextAttackMillis());assertEquals(n.nextMoveMillis(),after.nextMoveMillis());
            assertEquals(before.revision()+1,r.snapshot().revision());
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(hit));
            assertEquals("NpcDisintegrateIncoming[redacted]",hit.toString());
            assertEquals(300,pc.profile().effective().initialMp());
            assertEquals(500,pc.profile().source().persisted().lawful(),"victim CAS cannot debit or advance caster data");
        }
    }
    @Test void fullyResistedOutcomeConsumesOnePlanWithoutInventingNpcHurt()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,Long.MAX_VALUE),true,0);
        try(var r=ModernNpcRosterTest.roster()) {
            var n=ModernNpcMagicCommitTest.spawn(r,c);var hit=plan(r,c,n);long revision=r.snapshot().revision();
            assertEquals(0,hit.outcome().appliedDamage());assertEquals(n,r.commitIncoming(hit));
            assertEquals(revision+1,r.snapshot().revision());
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(hit));
        }
    }
    @Test void changedTargetClockOrMapGenerationRejectsThePreparedPlan()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L,HP,1000L),true,0);
        try(var r=ModernNpcRosterTest.roster()) {
            var n=ModernNpcMagicCommitTest.spawn(r,c);var hit=plan(r,c,n);r.damage(hit.scope(),n.id(),n.life(),1);
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(hit));
            var newer=plan(r,c,r.snapshot().actors().get(0));r.advance(hit.scope(),1);
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(newer));
            for(var scope:List.of(new ModernNpcRoster.Scope(12,22,33),new ModernNpcRoster.Scope(11,23,33),new ModernNpcRoster.Scope(11,22,34)))
                ModernNpcDisintegrateTest.failure(ModernDisintegrate.Failure.INVALID_STATE,()->ModernNpcDisintegrate.prepareIncoming(
                        ModernNpcDisintegrateTest.caster(),c,r.snapshot(),new ModernNpcRoster.Id(scope,1),1,b->{fail("scope checked before RNG");return 0;},0));
            ModernNpcDisintegrateTest.failure(ModernDisintegrate.Failure.INVALID_STATE,()->ModernNpcDisintegrate.prepareIncoming(
                    ModernNpcDisintegrateTest.caster(),c,r.snapshot(),new ModernNpcRoster.Id(hit.scope(),999),1,b->0,0));
        }
    }
    @Test void criticalDeathCorpseResurrectionAndRetirementCannotApplyOldLifeTwice()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L,HP,300L),true,0);
        try(var r=ModernNpcRosterTest.roster()) {
            var n=ModernNpcMagicCommitTest.spawn(r,c);
            var hit=ModernNpcDisintegrate.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),n.id(),1,b->0,0);
            assertEquals(432,hit.outcome().rawDamage());assertEquals(300,hit.outcome().appliedDamage());assertEquals(11748,hit.outcome().effect());
            var dead=r.commitIncoming(hit);assertEquals(0,dead.hp());assertEquals(ModernNpcRoster.Action.DEATH,dead.action());
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(hit));
            ModernNpcDisintegrateTest.failure(ModernDisintegrate.Failure.INVALID_STATE,()->plan(r,c,dead));
            var revived=r.resurrect(hit.scope(),dead.id(),1,300,dead.mp(),Set.of());assertEquals(2,revived.life());
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(hit));
            ModernNpcDisintegrateTest.failure(ModernDisintegrate.Failure.INVALID_STATE,()->ModernNpcDisintegrate.prepareIncoming(
                    ModernNpcDisintegrateTest.caster(),c,r.snapshot(),revived.id(),1,b->0,0));
            var fresh=ModernNpcDisintegrate.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),revived.id(),2,b->0,0);
            var finalCorpse=r.commitIncoming(fresh);r.advance(hit.scope(),finalCorpse.corpseUntilMillis());
            assertTrue(r.snapshot().actors().isEmpty());
            ModernNpcRosterTest.rejectedUnchanged(r,()->r.commitIncoming(fresh));
        }
    }
    @Test void concurrentDuplicatePlanHasExactlyOneVictimCommit()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L,HP,1000L),true,0);
        var executor=Executors.newFixedThreadPool(2);
        try(var r=ModernNpcRosterTest.roster()) {
            var n=ModernNpcMagicCommitTest.spawn(r,c);var hit=plan(r,c,n);var start=new CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> commit=()->{start.await();try{r.commitIncoming(hit);return true;}catch(IllegalArgumentException stale){return false;}};
            var a=executor.submit(commit);var b=executor.submit(commit);start.countDown();
            assertNotEquals(a.get(2,TimeUnit.SECONDS),b.get(2,TimeUnit.SECONDS));
            assertEquals(784,r.snapshot().actors().get(0).hp());
        }finally {executor.shutdownNow();assertTrue(executor.awaitTermination(2,TimeUnit.SECONDS));}
    }
    @Test void actualSharedMapDeathKeepsTypedNpcIdentityAndNoSourceOrMapMutation()throws Exception {
        var map=ModernQuestMapQualificationTest.shared();var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,3);
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L,HP,300L),true,0);var scope=ModernNpcRosterTest.SCOPE;
        try(var r=new ModernNpcRoster(scope,map,List.of(cells.get(0),cells.get(1)),20,3000)) {
            var n=r.spawn(scope,List.of(new ModernNpcRoster.Spawn(c.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),0,1,cells.get(0),2)),Set.of(cells.get(2))).get(0);
            var hit=ModernNpcDisintegrate.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),n.id(),1,b->0,0);
            var dead=r.commitIncoming(hit);assertEquals(n.id(),dead.id());assertEquals(n.template(),dead.template());
            assertTrue(map.standing(dead.cell()));assertEquals(ModernNpcRoster.Action.DEATH,dead.action());
            assertEquals(300,c.npcs().get(ModernNpcMeleeTest.NPC).get(HP));
        }
    }
}
