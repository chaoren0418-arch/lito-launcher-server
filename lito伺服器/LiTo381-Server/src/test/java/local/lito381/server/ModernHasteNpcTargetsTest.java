package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import static local.lito381.server.ModernHasteCast.Reason.NONE;
import static local.lito381.server.ModernBattleNpcHasteTest.active;
import static local.lito381.server.ModernBattleNpcHasteTest.spawn;
import static local.lito381.server.ModernBattleNpcHasteTest.npc;
import static local.lito381.server.ModernBattleNpcHasteTest.cast;
import static local.lito381.server.ModernHasteTargetsTest.receipt;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernHasteNpcTargetsTest {
    static ModernBattleEngineNpcTest.Fixture fixture(long npcHp)throws Exception {
        return fixture(npcHp,false);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(long npcHp,boolean quest)throws Exception {
        return fixture(npcHp,quest,false);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(long npcHp,boolean quest,boolean fireStorm)throws Exception {
        return fixture(npcHp,quest,fireStorm,300);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(long npcHp,boolean quest,boolean fireStorm,long mp)throws Exception {
        var catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,npcHp),true,0);
        var f=new ModernBattleEngineNpcTest.Fixture(2,0,fireStorm,1000,0,s->{
            var changed=ModernMagicCombatTest.stats(s,12,17,0,15,mp);
            return new ModernPhysicalCombat.Source(changed.input(),s.growth(),s.templates(),s.procCatalogIds(),ModernMagicCombatTest.growth(s.growth().sourceId()),
                    s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
        },catalog,false);
        try {
            var skills=fireStorm?List.of(ModernSkillsTest.teleport(),new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM)):List.of(ModernSkillsTest.teleport());
            var members=f.prepared.stream().map(p->enable(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),1_200_000_000L,false,List.of(),ModernInventoryWeight.qualify(p.fighter().profile(),100),true,false,fireStorm),ModernHasteCatalog.Status.QUALIFIED)).toList();
            var base=ModernQuestProgressTest.plan(f.binding.key(),catalog);
            var waves=base.waves().stream().map(w->!fireStorm||w.lane()==0?w:new ModernQuestProgress.Wave(w.lane(),w.stage(),
                    w.spawns().stream().map(s->new ModernNpcRoster.Spawn(s.template(),s.lane(),s.stage(),ModernQuestProgressTest.cell(24,10),s.heading())).toList())).toList();
            var config=quest?new ModernNpcBattle.Config(f.binding.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,
                    new ModernQuestProgress.Plan(base.key(),waves,ModernQuestObjectivesTest.objectives(554,1,false))):f.config;
            f.engine=new ModernBattleEngine(f.ticket,f.binding,members,List.of(),new ModernBattleEngine.Limits(2000,0,1_800_000),f.clock::get,f.epoch::get,
                    b->{f.draws.incrementAndGet();return Math.min(f.randomValue,b-1);},quest,null,()->false,null,config.withFireStorm(fireStorm));
            return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernHasteTargets.Entry entry(ModernBattleObservation.Capture c,ModernNpcRoster.Id id) {
        return c.hasteTargets().targets().get(new ModernHasteTargets.Npc(id));
    }
    @Test void refreshPreservesPreviousWalkProofEvenAfterCasterTargetsSomeoneElse()throws Exception {
        try(var f=fixture(280)) {
            active(f);var n=spawn(f,0,14,10);var first=cast(f,0,1,n).observation();var old=receipt(first,0);
            f.elapsed(100);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var moved=f.engine.npcTickObservation();assertEquals(old,entry(moved,n.id()).walk());assertEquals(old,entry(moved,n.id()).active());
            f.elapsed(2000);var refreshed=cast(f,0,2,npc(f.engine.serviceObservation())).observation();var latest=receipt(refreshed,0);
            assertEquals(latest,entry(refreshed,n.id()).active());assertEquals(old,entry(refreshed,n.id()).walk());assertNotEquals(old,latest);
            f.elapsed(4000);var c=f.engine.serviceObservation();var other=f.engine.haste(f.players.get(0).owner(),request(c,0,3,ModernBattleHasteTest.target(c,0))).observation();
            assertEquals(2,other.hasteTargets().targets().size());assertEquals(entry(refreshed,n.id()),entry(other,n.id()));
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));var next=f.engine.npcTickObservation();
            assertEquals(latest,entry(next,n.id()).walk());assertEquals(latest,entry(next,n.id()).active());
            assertEquals(75,npc(next).walk().durationMillis());assertDoesNotThrow(()->next.hasteTargets().validate(next));
        }
    }
    @Test void expiryKeepsAcceleratedWalkEvidenceUntilARealNormalStepReplacesIt()throws Exception {
        try(var f=fixture(280)) {
            active(f);var n=spawn(f,0,14,10);var r=receipt(cast(f,0,1,n).observation(),0);
            f.elapsed(100);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(1_200_000);var expired=f.engine.serviceObservation();assertFalse(npc(expired).speed().hasted());
            assertNull(entry(expired,n.id()).active());assertEquals(r,entry(expired,n.id()).walk());assertEquals(75,npc(expired).walk().durationMillis());
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));var normal=f.engine.npcTickObservation();
            assertEquals(100,npc(normal).walk().durationMillis());assertTrue(normal.hasteTargets().targets().isEmpty());
            assertEquals(40,normal.actors().get(0).resources().flows().spent());
        }
    }
    @Test void staleClientCommandAfterUnpublishedSchedulerStepCannotDropTheWalkGrant()throws Exception {
        try(var f=fixture(280)) {
            active(f);var n=spawn(f,0,14,10);var c=cast(f,0,1,n).observation();var current=npc(c);
            var oldIntent=request(c,0,2,ModernBattleNpcHasteTest.target(current));f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var rejected=f.engine.haste(f.players.get(0).owner(),oldIntent);assertEquals(ModernHasteCast.Reason.STALE_TARGET,rejected.reason());
            assertEquals(receipt(c,0),entry(rejected.observation(),n.id()).walk());assertEquals(40,rejected.observation().actors().get(0).resources().flows().spent());
        }
    }
    @Test void actualNpcDeathKeepsOnlyLastWalkAndCorpseRetirementDropsTheOldSerial()throws Exception {
        try(var f=fixture(1)) {
            active(f);var n=spawn(f,0,12,10);var first=cast(f,0,1,n).observation();var r=receipt(first,0);f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(2000);var p=f.players.get(0);f.randomValue=19;
            var killed=f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,n.id(),1);assertEquals(ModernNpcBattle.Reason.ATTACKED,killed.reason());
            var dead=killed.observation();assertFalse(npc(dead).alive());assertNull(entry(dead,n.id()).active());assertEquals(r,entry(dead,n.id()).walk());
            f.elapsed(npc(dead).corpseUntilMillis());var retired=f.engine.serviceObservation();assertTrue(retired.npcs().roster().actors().isEmpty());assertTrue(retired.hasteTargets().targets().isEmpty());
            var newborn=spawn(f,0,12,10);assertNotEquals(n.id(),newborn.id());assertFalse(newborn.speed().hasted());
            assertTrue(f.engine.serviceObservation().hasteTargets().targets().isEmpty());
        }
    }
    @Test void missingWalkProofAndUsingOldWalkAsCurrentSpeedCannotEnterSnapshot()throws Exception {
        try(var f=fixture(280)) {
            active(f);var n=spawn(f,0,14,10);var first=cast(f,0,1,n).observation();f.elapsed(100);
            f.engine.stepNpcUnpublished(n.id(),1,4096,false);f.elapsed(2000);var c=cast(f,0,2,npc(f.engine.serviceObservation())).observation();
            var e=entry(c,n.id());var key=new ModernHasteTargets.Npc(n.id());
            var missingWalk=new ModernHasteTargets.State(c.identity(),Map.of(key,new ModernHasteTargets.Entry(e.active(),null)));
            assertThrows(IllegalArgumentException.class,()->ModernHasteTargetsTest.with(c,missingWalk));
            var wrongActive=new ModernHasteTargets.State(c.identity(),Map.of(key,new ModernHasteTargets.Entry(e.walk(),e.walk())));
            assertThrows(IllegalArgumentException.class,()->ModernHasteTargetsTest.with(c,wrongActive));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteTargets.Entry(e.walk(),e.active()));
        }
    }
    @Test void closedRoomRetiresBothPlayerAndNpcEvidenceWithoutClearingOldImmutableCaptures()throws Exception {
        try(var f=fixture(280)) {
            active(f);var n=spawn(f,0,14,10);var first=cast(f,0,1,n).observation();f.elapsed(2000);var c=f.engine.serviceObservation();
            var both=f.engine.haste(f.players.get(0).owner(),request(c,0,2,ModernBattleHasteTest.target(c,1))).observation();
            assertEquals(2,both.hasteTargets().targets().size());var ended=f.engine.abortObservation();
            assertEquals(ModernBattleEngine.Phase.ABORTED,ended.phase());assertTrue(ended.hasteTargets().targets().isEmpty());
            assertTrue(ended.npcs().roster().closed());assertDoesNotThrow(()->both.hasteTargets().validate(both));
            assertEquals(1,first.hasteTargets().targets().size());
        }
    }
    @Test void questFinalHitClearsPlayerAndSurvivingNpcGrantsAtSettlement()throws Exception {
        try(var f=fixture(1,true)) {
            var c=active(f);assertEquals(NONE,f.engine.haste(f.players.get(0).owner(),request(c,0,1,ModernBattleHasteTest.target(c,0))).reason());
            var other=c.npcs().roster().actors().stream().filter(n->n.lane()==1).findFirst().orElseThrow();assertEquals(NONE,cast(f,1,1,other).reason());
            f.randomValue=19;var p=f.players.get(0);
            for(int stage=1;stage<=4;stage++) {
                f.elapsed(stage*2000);var n=f.engine.serviceObservation().npcs().roster().actors().stream().filter(a->a.lane()==0&&a.alive()).findFirst().orElseThrow();
                assertEquals(stage,n.stage());assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),stage,n.id(),n.life()).reason());
            }
            var before=f.engine.serviceObservation();var objective=before.modes().quest().objectives().stream().filter(a->a.id().lane()==0).findFirst().orElseThrow();
            assertEquals(2,before.hasteTargets().targets().size());f.elapsed(8700);
            var won=f.engine.attackQuestObjective(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),5,objective.id()).observation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,won.phase());assertEquals(won.revision(),won.result().engineRevision());
            assertTrue(won.hasteTargets().targets().isEmpty());assertTrue(won.actors().stream().noneMatch(a->a.buffs().hasEffects()));
            assertTrue(won.npcs().roster().actors().stream().noneMatch(a->a.speed().hasted()));
            assertEquals(40,won.actors().get(0).resources().flows().spent());assertEquals(40,won.actors().get(1).resources().flows().spent());
            assertEquals(won,f.engine.serviceObservation());
            var publication=ModernBattleWireTest.publication(won,f.config.catalog());
            for(var player:f.players)assertTrue(publication.audience(player.scope()).world().hasteEffects().targets().isEmpty());
        }
    }
    @Test void questFinalFireStormClearsActiveEffectsButKeepsTheSurvivorsCommittedWalkProof()throws Exception {
        try(var f=fixture(1,true,true)) {
            var c=active(f);var player=f.players.get(0);
            assertEquals(NONE,f.engine.haste(player.owner(),request(c,0,1,ModernBattleHasteTest.target(c,0))).reason());
            var other=c.npcs().roster().actors().stream().filter(n->n.lane()==1).findFirst().orElseThrow();
            var granted=cast(f,1,1,other).observation();var walkGrant=receipt(granted,1);
            f.elapsed(100);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(other.id(),1,4096,false));
            var walked=f.engine.npcTickObservation().npcs().roster().actors().stream().filter(n->n.id().equals(other.id())).findFirst().orElseThrow();
            assertEquals(75,walked.walk().durationMillis());f.randomValue=19;
            for(int stage=1;stage<=4;stage++) {
                f.elapsed(stage*2000);var n=f.engine.serviceObservation().npcs().roster().actors().stream().filter(a->a.lane()==0&&a.alive()).findFirst().orElseThrow();
                assertEquals(stage,n.stage());
                assertEquals(ModernNpcFireStormState.Reason.NONE,f.engine.fireStormNpc(player.owner(),new ModernNpcFireStormState.Request(player.scope(),f.scope,stage+1,1)).reason());
            }
            var before=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.ACTIVE,before.phase());
            assertEquals(2,before.hasteTargets().targets().size());f.elapsed(10_000);
            var result=f.engine.fireStormNpc(player.owner(),new ModernNpcFireStormState.Request(player.scope(),f.scope,6,1));
            assertEquals(ModernNpcFireStormState.Reason.NONE,result.reason());var won=result.observation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,won.phase());assertEquals(won.revision(),won.result().engineRevision());
            assertTrue(won.actors().stream().noneMatch(a->a.buffs().hasEffects()));assertTrue(won.npcs().roster().actors().stream().noneMatch(a->a.speed().hasted()));
            assertEquals(1,won.hasteTargets().targets().size());assertNull(entry(won,other.id()).active());assertEquals(walkGrant,entry(won,other.id()).walk());
            var survivor=won.npcs().roster().actors().stream().filter(n->n.id().equals(other.id())).findFirst().orElseThrow();
            assertEquals(walked.walk(),survivor.walk());assertEquals(walked.walkSpeed(),survivor.walkSpeed());assertEquals(walked.hp(),survivor.hp());
            assertEquals(280,won.actors().get(0).resources().flows().spent());assertEquals(40,won.actors().get(1).resources().flows().spent());
            assertEquals(5,won.actors().get(0).histories().get(ModernUseHistory.Lane.NPC_FIRE_STORM).current().casts());
            assertEquals(won,f.engine.serviceObservation());assertDoesNotThrow(()->before.hasteTargets().validate(before));
            var publication=ModernBattleWireTest.publication(won,f.config.catalog());
            assertTrue(publication.audience(f.players.get(0).scope()).world().hasteEffects().targets().isEmpty());
            var retained=publication.audience(f.players.get(1).scope()).world().hasteEffects().targets().get(0);
            assertNull(retained.active());assertEquals(ModernHasteEffects.grant(walkGrant,won.loadingOriginNanos()),retained.walk());
        }
    }
}
