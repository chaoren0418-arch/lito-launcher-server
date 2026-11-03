package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSlowCast.Reason.*;
import static local.lito381.server.ModernBattleNpcHasteTest.active;
import static local.lito381.server.ModernBattleNpcHasteTest.spawn;
import static local.lito381.server.ModernBattleNpcHasteTest.npc;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Real engine NPC CAS, pursuit and BOT targets; no public Slow wire is enabled. */
class ModernBattleNpcSlowTest {
    static ModernPhysicalCombat.Source flags(ModernPhysicalCombat.Source s) {
        var templates=new HashMap<Long,ModernPhysicalCombat.TemplatePolicy>();
        s.templates().forEach((id,p)->templates.put(id,new ModernPhysicalCombat.TemplatePolicy(p.kind(),p.className(),p.criticalMultiplier(),OptionalLong.of(0))));
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),templates,s.procCatalogIds(),s.magicGrowth(),
                s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    static ModernBattleEngineNpcTest.Fixture fixture(boolean quest,int humans,int bots)throws Exception {
        return fixture(quest,humans,bots,ModernNpcMeleeTest.catalog());
    }
    static ModernBattleEngineNpcTest.Fixture fixture(boolean quest,int humans,int bots,ModernNpcCatalog.Catalog catalog)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(humans,bots,false,1000,0,s->{
            var changed=ModernMagicCombatTest.stats(s,12,17,0,15,300);
            return flags(new ModernPhysicalCombat.Source(changed.input(),s.growth(),s.templates(),s.procCatalogIds(),
                    ModernMagicCombatTest.growth(s.growth().sourceId()),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth()));
        },catalog,false);
        try {
            var members=f.prepared.stream().map(p->{
                var base=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                        new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),List.of(ModernSkillsTest.teleport())),1200_000_000L,false,List.of(),
                        ModernInventoryWeight.qualify(p.fighter().profile(),100),true);
                return ModernBattleSlowTest.enable(ModernBattleHasteTest.enable(base,ModernHasteCatalog.Status.QUALIFIED),6);
            }).toList();
            var config=new ModernNpcBattle.Config(f.config.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,
                    quest?ModernQuestProgressTest.plan(f.binding.key(),catalog):null);
            f.engine=f.build(config,false,members);return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernSlowCast.Npc target(ModernNpcRoster.Actor n){return new ModernSlowCast.Npc(n.id(),n.life(),n.changedRevision(),n.cell());}
    static ModernBattleEngine.SlowResult cast(ModernBattleEngineNpcTest.Fixture f,int who,long seq,ModernNpcRoster.Actor n) {
        return f.engine.slow(f.players.get(who).owner(),ModernBattleSlowTest.request(f.engine.serviceObservation(),who,seq,target(n)));
    }
    @Test void npcSpeedAndCasterCostsCommitTogetherAndPursuitRetainsItsAlreadyScheduledStep()throws Exception {
        try(var f=fixture(false,2,0)) {
            active(f);var n=spawn(f,0,14,10);f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var first=npc(f.engine.serviceObservation());f.elapsed(150);var before=f.engine.serviceObservation();
            var result=cast(f,0,1,first);assertEquals(NONE,result.reason());var c=result.observation();ModernBattleSlowTest.consistent(c);
            var slowed=npc(c);assertEquals(before.npcs().roster().revision()+1,c.npcs().roster().revision());
            assertEquals(new ModernNpcMotion.Pace(200,1500),slowed.pace());assertEquals(64150,slowed.speed().slowUntilMillis());
            assertEquals(20,c.actors().get(0).resources().flows().spent());assertEquals(1,c.actors().get(0).materials().uses());
            assertEquals(first.walk(),slowed.walk());assertEquals(200,slowed.nextMoveMillis());
            f.elapsed(199);assertEquals(ModernNpcBattle.Reason.WAIT,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(200);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var moved=npc(f.engine.serviceObservation());assertEquals(200,moved.walk().durationMillis());assertEquals(400,moved.nextMoveMillis());
            long elapsed=400;boolean attacked=false;
            for(int i=0;i<12;i++){f.elapsed(elapsed);var reason=f.engine.stepNpcUnpublished(n.id(),1,4096,false);
                if(reason==ModernNpcBattle.Reason.ATTACKED){attacked=true;break;}assertEquals(ModernNpcBattle.Reason.MOVED,reason);elapsed+=200;}
            assertTrue(attacked);assertEquals(elapsed+1500,npc(f.engine.serviceObservation()).nextAttackMillis());
            f.elapsed(elapsed+1499);assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(elapsed+1500);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            ModernBattleSlowTest.consistent(f.engine.serviceObservation());
        }
    }
    @Test void slowHasteNeutralizationPreservesHistoricalSlowWalkAndLaterHasteCanStart()throws Exception {
        try(var f=fixture(false,2,0)) {
            active(f);var n=spawn(f,0,14,10);assertEquals(NONE,cast(f,0,1,n).reason());f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));var old=npc(f.engine.serviceObservation());
            assertTrue(old.walkSpeed().slowed());f.elapsed(2000);
            assertEquals(ModernHasteCast.Reason.NONE,ModernBattleNpcHasteTest.cast(f,0,2,npc(f.engine.serviceObservation())).reason());
            var neutral=f.engine.serviceObservation();assertFalse(npc(neutral).speed().slowed());assertFalse(npc(neutral).speed().hasted());
            assertEquals(old.walk(),npc(neutral).walk());assertTrue(npc(neutral).walkSpeed().slowed());ModernBattleSlowTest.consistent(neutral);
            f.elapsed(4000);assertEquals(ModernHasteCast.Reason.NONE,ModernBattleNpcHasteTest.cast(f,0,3,npc(f.engine.serviceObservation())).reason());
            assertTrue(npc(f.engine.serviceObservation()).speed().hasted());f.elapsed(6000);
            assertEquals(NONE,cast(f,0,4,npc(f.engine.serviceObservation())).reason());var back=f.engine.serviceObservation();
            assertFalse(npc(back).speed().hasted());assertFalse(npc(back).speed().slowed());assertEquals(2,back.actors().get(0).materials().uses());
            assertEquals(ModernSlowReceipt.Outcome.NEUTRALIZED,((ModernSlowReceipt)back.actors().get(0).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail()).outcome());
            ModernBattleSlowTest.consistent(back);
        }
    }
    @Test void reentrantNpcMovementDuringRandomRejectsWithoutAnyCasterDebitOrSpeedCommit()throws Exception {
        try(var f=fixture(false,2,0)) {
            active(f);var n=spawn(f,0,14,10);f.elapsed(100);
            f.duringRandom=()->assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var result=cast(f,0,1,n);assertEquals(STALE_TARGET,result.reason());var c=result.observation();
            assertEquals(0,c.actors().get(0).materials().uses());assertEquals(0,c.actors().get(0).resources().flows().spent());
            assertFalse(npc(c).speed().slowed());assertNotNull(npc(c).walk());ModernBattleSlowTest.consistent(c);
        }
    }
    @Test void cancellationInsideRandomHasNoHalfCommitAndClearsEarlierTarget()throws Exception {
        try(var f=fixture(false,2,0)) {
            active(f);var n=spawn(f,0,14,10);assertEquals(NONE,cast(f,0,1,n).reason());f.elapsed(2000);
            f.duringRandom=()->f.rooms.releasePreparation(f.ticket);
            var result=cast(f,0,2,npc(f.engine.serviceObservation()));assertEquals(NOT_ACTIVE,result.reason());var c=result.observation();
            assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,c.abortReason());assertTrue(c.npcs().roster().actors().isEmpty());
            assertEquals(1,c.actors().get(0).materials().uses());assertEquals(20,c.actors().get(0).resources().flows().spent());
            ModernBattleSlowTest.consistent(c);
        }
    }
    @Test void botIsAQualifiedParticipantWithoutInventingSourceInventoryOrRuntime()throws Exception {
        try(var f=fixture(false,1,1)) {
            var before=active(f);var result=f.engine.slow(f.players.get(0).owner(),
                    ModernBattleSlowTest.request(before,0,1,ModernBattleSlowTest.target(before,1)));
            assertEquals(NONE,result.reason());var c=result.observation();var bot=c.actors().get(1);
            assertNull(bot.scope());assertNull(bot.runtime());assertNull(bot.materials());assertTrue(bot.buffs().slowUntilNanos()>0);
            assertEquals(before.actors().get(1).resources(),bot.resources());ModernBattleSlowTest.consistent(c);
        }
    }
    @Test void questIsolationRejectsOtherNpcLaneAndOwnLaneRemainsPlayable()throws Exception {
        try(var f=fixture(true,2,0)) {
            var c=active(f);var foreign=c.npcs().roster().actors().stream().filter(n->n.lane()==1).findFirst().orElseThrow();
            assertEquals(WRONG_INSTANCE,cast(f,0,1,foreign).reason());assertEquals(0,f.draws.get());
            var own=c.npcs().roster().actors().stream().filter(n->n.lane()==0).findFirst().orElseThrow();assertEquals(NONE,cast(f,0,2,own).reason());ModernBattleSlowTest.consistent(f.engine.serviceObservation());
        }
    }
}
