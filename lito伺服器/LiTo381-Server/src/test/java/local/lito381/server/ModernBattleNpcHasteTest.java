package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import static local.lito381.server.ModernHasteCast.Reason.*;
import static local.lito381.server.ModernBattleEngineNpcTest.cell;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Skill43 MP/target commit exercised through the same engine that owns NPC
 * pursuit and melee, not just a standalone roster mutation. */
class ModernBattleNpcHasteTest {
    static ModernBattleEngineNpcTest.Fixture fixture(boolean quest,int humans,int bots,long mp)throws Exception {
        return fixture(quest,humans,bots,mp,1000);
    }
    static ModernBattleEngineNpcTest.Fixture fixture(boolean quest,int humans,int bots,long mp,long hp)throws Exception {
        var catalog=ModernNpcMeleeTest.catalog();
        var f=new ModernBattleEngineNpcTest.Fixture(humans,bots,false,hp,0,s->{
            var changed=ModernMagicCombatTest.stats(s,12,17,0,15,mp);
            return new ModernPhysicalCombat.Source(changed.input(),s.growth(),s.templates(),s.procCatalogIds(),ModernMagicCombatTest.growth(s.growth().sourceId()),
                    s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
        },catalog,false);
        try {
            var members=f.prepared.stream().map(p->enable(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),List.of(ModernSkillsTest.teleport())),1200_000_000L,false,
                    List.of(),ModernInventoryWeight.qualify(p.fighter().profile(),100),true),ModernHasteCatalog.Status.QUALIFIED)).toList();
            var config=new ModernNpcBattle.Config(f.config.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,
                    quest?ModernQuestProgressTest.plan(f.binding.key(),catalog):null);
            f.engine=f.build(config,false,members);return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    static ModernBattleObservation.Capture active(ModernBattleEngineNpcTest.Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
        var c=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());return c;
    }
    static ModernNpcRoster.Actor npc(ModernBattleObservation.Capture c) {return c.npcs().roster().actors().get(0);}
    static ModernNpcRoster.Actor spawn(ModernBattleEngineNpcTest.Fixture f,int lane,int x,int y) {
        var template=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
        var c=f.engine.spawnNpcsObservation(f.scope,List.of(new ModernNpcRoster.Spawn(template,lane,1,cell(x,y),2)));
        return c.npcs().roster().actors().get(c.npcs().roster().actors().size()-1);
    }
    static ModernHasteCast.Npc target(ModernNpcRoster.Actor a) {return new ModernHasteCast.Npc(a.id(),a.life(),a.changedRevision(),a.cell());}
    static ModernBattleEngine.HasteResult cast(ModernBattleEngineNpcTest.Fixture f,int caster,long seq,ModernNpcRoster.Actor target) {
        return f.engine.haste(f.players.get(caster).owner(),request(f.engine.serviceObservation(),caster,seq,target(target)));
    }
    @Test void npcSerialIsNotAPlayerHandleAndMpAndStatusCommitInOneEngineRevision()throws Exception {
        try(var f=fixture(false,2,0,300)) {
            active(f);var n=spawn(f,0,11,10);var before=f.engine.serviceObservation();assertEquals(1,n.id().serial());
            var r=cast(f,0,1,n);assertEquals(NONE,r.reason());var c=r.observation();var after=npc(c);var caster=c.actors().get(0);
            assertEquals(before.revision()+1,c.revision());assertEquals(before.npcs().roster().revision()+1,c.npcs().roster().revision());
            assertEquals(40,caster.resources().flows().spent());assertEquals(260,caster.vital().values().mp());
            assertEquals(n.hp(),after.hp());assertEquals(n.mp(),after.mp());assertEquals(n.life(),after.life());
            assertEquals(1_200_000,after.speed().hasteUntilMillis());assertEquals(new ModernNpcMotion.Pace(75,566),after.pace());
            assertTrue(c.actors().stream().noneMatch(a->a.buffs().hasEffects()));assertTrue(c.npcs().exchanges().isEmpty());assertEquals(0,f.draws.get());
            var receipt=caster.runtime().receipts().get(LANE);var detail=(ModernHasteReceipt)receipt.detail();var effect=(ModernHasteReceipt.Npc)detail.target();
            assertEquals(n.id(),effect.id());assertEquals(n.speed(),effect.before());assertEquals(after.speed(),effect.after());assertTrue(receipt.batch().targets().isEmpty());
            assertDoesNotThrow(()->new ModernSpellRuntime.Snapshot(history(c,0),caster.runtime()));
            assertThrows(IllegalArgumentException.class,()->ModernNpcProtocol.Actor.from(after));assertNotNull(c.world().hasteEffects());
        }
    }
    @Test void actualPursuitAndAttackUse75And566msWithoutRewritingAlreadyScheduledActions()throws Exception {
        try(var f=fixture(false,2,0,300)) {
            active(f);var n=spawn(f,0,14,10);f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var first=npc(f.engine.serviceObservation());assertEquals(100,first.walk().durationMillis());
            f.elapsed(150);assertEquals(NONE,cast(f,0,1,first).reason());var fast=npc(f.engine.serviceObservation());
            assertEquals(first.walk(),fast.walk());assertEquals(200,fast.nextMoveMillis());
            f.elapsed(199);assertEquals(ModernNpcBattle.Reason.WAIT,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(200);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var moved=npc(f.engine.serviceObservation());assertEquals(75,moved.walk().durationMillis());assertEquals(275,moved.nextMoveMillis());
            long elapsed=275;boolean attacked=false;
            for(int attempt=0;attempt<12;attempt++) {
                f.elapsed(elapsed);var reason=f.engine.stepNpcUnpublished(n.id(),1,4096,false);
                if(reason==ModernNpcBattle.Reason.ATTACKED){attacked=true;break;}
                assertEquals(ModernNpcBattle.Reason.MOVED,reason);elapsed+=75;
            }
            assertTrue(attacked);var hit=npc(f.engine.serviceObservation());assertEquals(elapsed+566,hit.nextAttackMillis());
            f.elapsed(elapsed+565);assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            f.elapsed(elapsed+566);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
        }
    }
    @Test void staleNpcLifeRevisionPositionAndRemovedSerialDoNotPartiallyDebit()throws Exception {
        try(var f=fixture(false,2,0,300)) {
            active(f);var n=spawn(f,0,11,10);var c=f.engine.serviceObservation();
            var targets=List.of(new ModernHasteCast.Npc(n.id(),2,n.changedRevision(),n.cell()),
                    new ModernHasteCast.Npc(n.id(),1,n.changedRevision()+1,n.cell()),
                    new ModernHasteCast.Npc(n.id(),1,n.changedRevision(),cell(12,10)),
                    new ModernHasteCast.Npc(new ModernNpcRoster.Id(n.id().scope(),99),1,n.changedRevision(),n.cell()));
            long seq=0;for(var t:targets) {
                var r=f.engine.haste(f.players.get(0).owner(),request(c,0,++seq,t));assertEquals(STALE_TARGET,r.reason());
                unchanged(c.actors().get(0),r.observation().actors().get(0));assertEquals(c.npcs(),r.observation().npcs());
            }
            var scope=n.id().scope();var foreign=new ModernHasteCast.Npc(new ModernNpcRoster.Id(new ModernNpcRoster.Scope(scope.roomId(),scope.matchId(),scope.generation()+1),1),1,n.changedRevision(),n.cell());
            assertThrows(SecurityException.class,()->f.engine.haste(f.players.get(0).owner(),request(c,0,5,foreign)));
            assertEquals(NONE,cast(f,0,5,n).reason());
        }
    }
    @Test void insufficientMpLeavesNpcUnchangedAndRealDistantTargetIsOutOfRange()throws Exception {
        try(var f=fixture(false,2,0,39)) {
            active(f);var n=spawn(f,0,11,10);var before=f.engine.serviceObservation();
            assertEquals(NO_MP,cast(f,0,1,n).reason());var after=f.engine.serviceObservation();
            unchanged(before.actors().get(0),after.actors().get(0));assertEquals(before.npcs(),after.npcs());
        }
        try(var f=fixture(false,2,0,300)) {
            active(f);var distant=spawn(f,0,35,10);assertEquals(OUT_OF_RANGE,cast(f,0,1,distant).reason());
            assertEquals(0,f.engine.serviceObservation().actors().get(0).resources().flows().spent());
        }
    }
    @Test void questInstancesDenyBothOtherForcePlayerAndOtherForceNpc()throws Exception {
        try(var f=fixture(true,2,0,300)) {
            var c=active(f);assertTrue(c.modes().isolatedForces());
            assertEquals(WRONG_INSTANCE,f.engine.haste(f.players.get(0).owner(),request(c,0,1,ModernBattleHasteTest.target(c,1))).reason());
            var other=c.npcs().roster().actors().stream().filter(a->a.lane()==1).findFirst().orElseThrow();
            assertEquals(WRONG_INSTANCE,cast(f,0,2,other).reason());
            var own=c.npcs().roster().actors().stream().filter(a->a.lane()==0).findFirst().orElseThrow();
            assertEquals(NONE,cast(f,0,3,own).reason());assertEquals(40,f.engine.serviceObservation().actors().get(0).resources().flows().spent());
        }
    }
    @Test void serverBotReceivesTypedParticipantStatusWithoutBecomingACaster()throws Exception {
        try(var f=fixture(false,1,1,300)) {
            var c=active(f);assertFalse(c.actors().get(1).pose().member().identity().isHuman());
            var r=f.engine.haste(f.players.get(0).owner(),request(c,0,1,ModernBattleHasteTest.target(c,1)));assertEquals(NONE,r.reason());
            var a=r.observation().actors().get(1);assertTrue(a.buffs().hasEffects());assertNull(a.runtime());assertNull(a.scope());
            assertEquals(c.actors().get(1).resources(),a.resources());
        }
    }
    @Test void concurrentNpcTargetCompareAndSwapChargesExactlyOneCaster()throws Exception {
        try(var f=fixture(false,2,0,300)) {
            active(f);var n=spawn(f,0,15,10);var c=f.engine.serviceObservation();var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                var a=pool.submit(()->{gate.await();return f.engine.haste(f.players.get(0).owner(),request(c,0,1,target(n)));});
                var b=pool.submit(()->{gate.await();return f.engine.haste(f.players.get(1).owner(),request(c,1,1,target(n)));});gate.countDown();
                var reasons=List.of(a.get(5,TimeUnit.SECONDS).reason(),b.get(5,TimeUnit.SECONDS).reason());
                assertEquals(1,reasons.stream().filter(r->r==NONE).count());assertEquals(1,reasons.stream().filter(r->r==STALE_TARGET).count());
                var after=f.engine.serviceObservation();assertEquals(40,after.actors().stream().mapToLong(x->x.resources().flows().spent()).sum());
                assertEquals(c.npcs().roster().revision()+1,after.npcs().roster().revision());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void lethalNpcAttackClearsPlayersHasteAtTheDamageCommitNotALaterTick()throws Exception {
        try(var f=fixture(false,2,0,300,1)) {
            var c=active(f);var n=spawn(f,0,11,10);c=f.engine.serviceObservation();
            assertEquals(NONE,f.engine.haste(f.players.get(0).owner(),request(c,0,1,ModernBattleHasteTest.target(c,0))).reason());
            f.randomValue=19;f.elapsed(100);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            var dead=f.engine.npcTickObservation();var victim=dead.actors().get(0);assertEquals(0,victim.vital().values().hp());
            assertFalse(victim.buffs().hasEffects());assertEquals(dead.revision(),victim.buffs().revision());
            assertEquals(40,victim.resources().flows().spent());assertEquals(1,victim.runtime().receipts().get(LANE).casts());
            assertEquals(TARGET_DEAD,f.engine.haste(f.players.get(1).owner(),request(dead,1,1,ModernBattleHasteTest.target(dead,0))).reason());
            assertEquals(DEAD,f.engine.haste(f.players.get(0).owner(),request(dead,0,2,ModernBattleHasteTest.target(dead,1))).reason());
        }
    }
    @Test void internalTimeAttackCandidateCannotSilentlyUseTheOldNpcProjection()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,1000,false,1,false,true,false,1,true)) {
            var c=f.engine.serviceObservation();assertDoesNotThrow(c::world);
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            ModernHasteTimeAttackTest.active(f);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.engine.serviceObservation().phase());
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(1,8,1000,false,1,false,true)) {
            f.active();assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());
        }
    }
}
