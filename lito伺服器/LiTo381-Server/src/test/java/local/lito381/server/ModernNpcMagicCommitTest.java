package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernNpcMagicCommitTest {
    static ModernNpcRoster.Actor spawn(ModernNpcRoster roster,ModernNpcCatalog.Catalog catalog) {
        return roster.spawn(ModernNpcRosterTest.SCOPE,List.of(new ModernNpcRoster.Spawn(catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),
                0,1,ModernNpcRosterTest.cell(10,10),2)),Set.of()).get(0);
    }
    static ModernNpcMagic.Incoming plan(ModernNpcRoster roster,ModernNpcCatalog.Catalog catalog,ModernNpcRoster.Actor actor) {
        return ModernNpcMagic.prepareIncoming(ModernNpcMagicTest.caster(),catalog,roster.snapshot(),actor.id(),actor.life(),new ModernNpcMeleeTest.Rolls(9,99));
    }
    @Test void preparedMagicHasQualifiedCasterProofAndCommitsVictimExactlyOnce()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L),true,0);
        try(var roster=ModernNpcRosterTest.roster()) {
            var npc=spawn(roster,c);var before=roster.snapshot();var pc=ModernNpcMagicTest.caster();var plan=plan(roster,c,npc);
            assertEquals(before,roster.snapshot());assertEquals(pc.key(),plan.attacker());assertEquals(pc.profile().equipmentRevision(),plan.equipmentRevision());
            assertEquals(pc.profile().evaluatedAtMillis(),plan.evaluatedAtMillis());assertEquals(pc.profile().validUntilMillis(),plan.validUntilMillis());
            assertEquals(c.npcs().get(ModernNpcMeleeTest.NPC).key(),plan.catalogKey());assertEquals(3,plan.mpCost());
            var hit=roster.commitIncoming(plan);assertEquals(263,hit.hp());assertEquals(ModernNpcRoster.Action.HURT,hit.action());
            assertEquals(npc.nextMoveMillis(),hit.nextMoveMillis());assertEquals(npc.nextAttackMillis(),hit.nextAttackMillis());
            assertEquals(before.revision()+1,roster.snapshot().revision());
            ModernNpcRosterTest.rejectedUnchanged(roster,()->roster.commitIncoming(plan));
            assertEquals("NpcMagicIncoming[redacted]",plan.toString());
        }
    }
    @Test void fullyResistedCastConsumesPlanButDoesNotFabricateHurtAnimation()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,Long.MAX_VALUE),true,0);
        try(var roster=ModernNpcRosterTest.roster()) {
            var npc=spawn(roster,c);var before=roster.snapshot();var plan=plan(roster,c,npc);assertEquals(0,plan.outcome().appliedDamage());
            assertEquals(npc,roster.commitIncoming(plan));assertEquals(before.revision()+1,roster.snapshot().revision());
            ModernNpcRosterTest.rejectedUnchanged(roster,()->roster.commitIncoming(plan));
        }
    }
    @Test void targetChangeClockChangeOrScopeChangeRejectOldPlanWithoutPartialDamage()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L),true,0);
        try(var roster=ModernNpcRosterTest.roster()) {
            var npc=spawn(roster,c);var old=plan(roster,c,npc);roster.damage(old.scope(),npc.id(),1,1);
            ModernNpcRosterTest.rejectedUnchanged(roster,()->roster.commitIncoming(old));
            var stale=plan(roster,c,roster.snapshot().actors().get(0));roster.advance(old.scope(),1);
            ModernNpcRosterTest.rejectedUnchanged(roster,()->roster.commitIncoming(stale));
            for(var scope:List.of(new ModernNpcRoster.Scope(12,22,33),new ModernNpcRoster.Scope(11,23,33),new ModernNpcRoster.Scope(11,22,34)))
                ModernNpcMagicTest.failure(ModernMagicCombat.Failure.INVALID_STATE,()->ModernNpcMagic.prepareIncoming(ModernNpcMagicTest.caster(),c,roster.snapshot(),new ModernNpcRoster.Id(scope,1),1,b->0));
            ModernNpcMagicTest.failure(ModernMagicCombat.Failure.INVALID_STATE,()->ModernNpcMagic.prepareIncoming(ModernNpcMagicTest.caster(),c,roster.snapshot(),new ModernNpcRoster.Id(old.scope(),2),1,b->0));
        }
    }
    @Test void deathAndResurrectionKeepLifeBoundaryAndOldSpellCannotHitNewIncarnation()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L,HP,5L),true,0);
        try(var roster=ModernNpcRosterTest.roster()) {
            var npc=spawn(roster,c);var plan=plan(roster,c,npc);assertEquals(17,plan.outcome().rawDamage());assertEquals(5,plan.outcome().appliedDamage());
            var dead=roster.commitIncoming(plan);assertFalse(dead.alive());assertEquals(ModernNpcRoster.Action.DEATH,dead.action());assertEquals(3000,dead.corpseUntilMillis());
            assertEquals(0,dead.nextMoveMillis());assertEquals(0,dead.nextAttackMillis());
            ModernNpcMagicTest.failure(ModernMagicCombat.Failure.INVALID_STATE,()->plan(roster,c,dead));
            var revived=roster.resurrect(plan.scope(),npc.id(),1,5,0,Set.of());assertEquals(2,revived.life());
            ModernNpcRosterTest.rejectedUnchanged(roster,()->roster.commitIncoming(plan));
            ModernNpcMagicTest.failure(ModernMagicCombat.Failure.INVALID_STATE,()->plan(roster,c,npc));
            roster.commitIncoming(plan(roster,c,revived));var corpse=roster.snapshot().actors().get(0);roster.advance(plan.scope(),corpse.corpseUntilMillis());
            assertTrue(roster.snapshot().actors().isEmpty());
            ModernNpcMagicTest.failure(ModernMagicCombat.Failure.INVALID_STATE,()->plan(roster,c,revived));
        }
    }
    @Test void concurrentReuseOnlyOneVictimCommitSucceeds()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L),true,0);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try(var roster=ModernNpcRosterTest.roster()) {
            var npc=spawn(roster,c);var plan=plan(roster,c,npc);var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> commit=()->{start.await();try{roster.commitIncoming(plan);return true;}catch(IllegalArgumentException expected){return false;}};
            var a=executor.submit(commit);var b=executor.submit(commit);start.countDown();assertNotEquals(a.get(2,java.util.concurrent.TimeUnit.SECONDS),b.get(2,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(263,roster.snapshot().actors().get(0).hp());
        }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(2,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void realSharedGeometryMagicKillResurrectionAndCorpseRetirement()throws Exception {
        var map=ModernQuestMapQualificationTest.shared();var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,3);
        var c=ModernNpcMeleeTest.catalog(Map.of(MR,0L),true,0);var scope=ModernNpcRosterTest.SCOPE;
        try(var roster=new ModernNpcRoster(scope,map,List.of(cells.get(0),cells.get(1)),20,3000)) {
            var npc=roster.spawn(scope,List.of(new ModernNpcRoster.Spawn(c.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate(),0,1,cells.get(0),2)),Set.of(cells.get(2))).get(0);
            int casts=0;ModernNpcMagic.Incoming last=null;
            while(npc.alive()) {last=plan(roster,c,npc);npc=roster.commitIncoming(last);assertTrue(++casts<100);}
            assertEquals(17,casts);assertEquals(8,last.outcome().appliedDamage());assertTrue(map.standing(npc.cell()));
            var stale=last;var revived=roster.resurrect(scope,npc.id(),1,280,npc.mp(),Set.of(cells.get(2)));
            ModernNpcRosterTest.rejectedUnchanged(roster,()->roster.commitIncoming(stale));assertEquals(2,revived.life());
            roster.damage(scope,revived.id(),revived.life(),Long.MAX_VALUE);var corpse=roster.snapshot().actors().get(0);roster.advance(scope,corpse.corpseUntilMillis());
            assertTrue(roster.snapshot().actors().isEmpty());
        }
    }
}
