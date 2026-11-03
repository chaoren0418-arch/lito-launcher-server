package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSlowCast.Reason.*;
import static local.lito381.server.ModernBattleEngineTimedTest.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/** Actual monitor-owned engine operations, not transaction-candidate tests.
 * No registered wire/UI capability is claimed by these private captures. */
class ModernBattleSlowTest {
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,int materials) {
        return enable(p,materials,0);
    }
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,int materials,long access) {
        var skills=new ArrayList<>(p.skills().skills());skills.add(new ModernSkills.Skill(29,"$1455",20,500,ModernSkills.Support.UNIMPLEMENTED));
        skills.sort(Comparator.comparingInt(ModernSkills.Skill::skillId));
        var copy=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),
                true,p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate(),p.greaterHaste(),p.haste());
        var stocks=new ArrayList<>(p.matchItems());
        if(materials>0)stocks.add(new ModernShopAuthority.Stack(ModernSpellMaterialsTest.offer(2000),materials));
        stocks.sort(Comparator.comparingLong(x->x.offer().itemId()));
        return copy.withSlow(new ModernSlowCatalog.Snapshot(p.fighter().key(),ModernSlowCatalog.Status.QUALIFIED,OptionalLong.of(access)),stocks);
    }
    static Fixture fixture()throws Exception{return fixture(4,100,1000,false);}
    static Fixture fixture(int materials,long mp,long targetHp,boolean settle)throws Exception {
        return fixture(materials,mp,targetHp,settle,12);
    }
    static Fixture fixture(int materials,long mp,long targetHp,boolean settle,int intelligence)throws Exception {
        return new Fixture(3,2,1000,targetHp,settle,p->{
            var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),p.scope().sourceId()==7?1000:targetHp,mp,intelligence,0);
            return enable(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true),materials);
        });
    }
    static ModernBattleObservation.Capture active(Fixture f){return ModernBattleHasteTest.active(f);}
    static ModernSlowCast.Player target(ModernBattleObservation.Capture c,int n) {
        var a=c.actors().get(n);return new ModernSlowCast.Player(new ModernTargetHistory.Participant(a.pose().entityId(),a.pose().member().identity()),
                a.resources().life()+1,a.resources().equipmentRevision(),a.pose().position(),a.buffs().revision());
    }
    static ModernSlowCast.Request request(ModernBattleObservation.Capture c,int n,long seq,ModernSlowCast.Target target) {
        var a=c.actors().get(n);return new ModernSlowCast.Request(a.scope(),c.identity().matchId(),c.identity().generation(),seq,
                a.resources().equipmentRevision(),a.resources().life()+1,a.pose().position(),a.buffs().revision(),target);
    }
    static ModernBattleEngine.SlowResult cast(Fixture f,int caster,long seq,int victim) {
        var c=f.engine.serviceObservation();return f.engine.slow(f.players.get(caster).owner(),request(c,caster,seq,target(c,victim)));
    }
    static void consistent(ModernBattleObservation.Capture c) {
        var world=c.world();for(var a:c.actors())if(a.scope()!=null)assertDoesNotThrow(()->c.forOwner(a.scope()));
        assertThrows(IllegalArgumentException.class,()->ModernBattleWireTest.publication(c,null));
        assertTrue(world.features().slow());
    }
    static Fixture dualSpeedFixture()throws Exception {
        return new Fixture(3,2,1000,1000,false,p->{
            var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),1000,300,12,0);
            var base=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true);
            return enable(ModernBattleHasteTest.enable(ModernBattleGreaterHasteTest.enable(base,ModernGreaterHasteCatalog.Status.QUALIFIED),
                    ModernHasteCatalog.Status.QUALIFIED),6);
        });
    }
    @Test void actualGreaterHasteAndTargetedHasteNeutralizeSlowWithTypedPaidReceipts()throws Exception {
        try(var f=dualSpeedFixture()) {
            active(f);f.randomMode=3;assertEquals(NONE,cast(f,0,1,1).reason());
            var greater=ModernBattleGreaterHasteTest.cast(f,1,1);assertEquals(ModernGreaterHasteCast.Reason.NONE,greater.reason());
            var neutral=greater.observation();assertFalse(neutral.actors().get(1).buffs().hasEffects());
            assertInstanceOf(ModernSpellRuntime.GreaterHasteNeutralized.class,neutral.actors().get(1).runtime().receipts().get(ModernUseHistory.Lane.GREATER_HASTE).detail());
            assertEquals(60,neutral.actors().get(1).resources().flows().spent());consistent(neutral);
            f.elapsed(2*SECOND);assertEquals(ModernGreaterHasteCast.Reason.NONE,ModernBattleGreaterHasteTest.cast(f,1,2).reason());
            assertEquals(NONE,cast(f,0,2,1).reason());assertFalse(f.engine.serviceObservation().actors().get(1).buffs().hasEffects());
            f.elapsed(4*SECOND);assertEquals(NONE,cast(f,0,3,1).reason());
            var c=f.engine.serviceObservation();var haste=f.engine.haste(f.players.get(1).owner(),ModernBattleHasteTest.request(c,1,3,ModernBattleHasteTest.target(c,1)));
            assertEquals(ModernHasteCast.Reason.TOO_FAST,haste.reason());f.elapsed(4400*MS);c=f.engine.serviceObservation();
            haste=f.engine.haste(f.players.get(1).owner(),ModernBattleHasteTest.request(c,1,4,ModernBattleHasteTest.target(c,1)));
            assertEquals(ModernHasteCast.Reason.NONE,haste.reason());assertFalse(haste.observation().actors().get(1).buffs().hasEffects());consistent(haste.observation());
        }
    }
    @Test void reentrantSameSequenceCommitsOnlyInnerTransactionAndTargetChangeRejectsOuter()throws Exception {
        try(var f=fixture()) {
            active(f);f.randomMode=3;f.onRandom=()->{f.onRandom=()->{};assertEquals(NONE,cast(f,0,1,1).reason());};
            assertThrows(SecurityException.class,()->cast(f,0,1,1));
            var c=f.engine.serviceObservation();assertEquals(1,c.actors().get(0).materials().uses());assertEquals(20,c.actors().get(0).resources().flows().spent());consistent(c);
        }
        try(var f=dualSpeedFixture()) {
            active(f);f.randomMode=3;f.onRandom=()->{f.onRandom=()->{};assertEquals(ModernGreaterHasteCast.Reason.NONE,ModernBattleGreaterHasteTest.cast(f,1,1).reason());};
            var result=cast(f,0,1,1);assertEquals(STALE_TARGET,result.reason());var c=result.observation();
            assertEquals(0,c.actors().get(0).materials().uses());assertEquals(0,c.actors().get(0).resources().flows().spent());consistent(c);
        }
    }
    @Test void gmProbabilityIsExactlyOnePercentAndSourceAccessIsNotBorrowedFromCaster()throws Exception {
        for(long access:List.of(199L,200L,1L<<32))try(var f=new Fixture(3,2,1000,1000,false,p->enable(p,4,p.scope().sourceId()==42?access:0))) {
            active(f);var miss=cast(f,0,1,1);assertEquals(NONE,miss.reason());
            var detail=(ModernSlowReceipt)miss.observation().actors().get(0).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail();
            if(access>=200)assertEquals(1,detail.probability());else assertTrue(detail.probability()>1);
            assertEquals(ModernSlowReceipt.Outcome.MISS,detail.outcome());
            f.elapsed(2*SECOND);f.randomMode=3;var hit=cast(f,0,2,1);assertEquals(NONE,hit.reason());
            assertEquals(ModernSlowReceipt.Outcome.APPLIED,((ModernSlowReceipt)hit.observation().actors().get(0).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail()).outcome());
            assertEquals(2,hit.observation().actors().get(0).materials().uses());consistent(hit.observation());
        }
    }
    @Test void epochCallbackCannotCommitUsingAnExpiredTargetOrAnAlreadyEndedWorld()throws Exception {
        for(long jump:new long[]{64*SECOND,1800*SECOND})try(var f=fixture()) {
            active(f);f.randomMode=3;assertEquals(NONE,cast(f,0,1,1).reason());f.elapsed(2*SECOND);
            // The final post-RNG equipment check can be slow/reentrant.
            // Temporal transitions must use the time AFTER it returns.
            var calls=new java.util.concurrent.atomic.AtomicInteger();
            f.onRandom=()->{f.onRandom=()->{};f.onEpoch=()->{calls.incrementAndGet();f.onEpoch=()->{};f.elapsed(jump);};};
            var r=cast(f,0,2,1);assertEquals(1,calls.get());
            assertEquals(jump==64*SECOND?STALE_TARGET:NOT_ACTIVE,r.reason());
            assertEquals(1,r.observation().actors().get(0).materials().uses());assertEquals(20,r.observation().actors().get(0).resources().flows().spent());
            consistent(r.observation());
        }
    }
    @Test void loadingThenHitCommitsOnlyCasterMpMaterialWeightAndVictimSpeed()throws Exception {
        try(var f=fixture()) {
            assertEquals(NOT_ACTIVE,cast(f,0,1,1).reason());var before=active(f);f.randomMode=3;
            var result=cast(f,0,2,1);assertEquals(NONE,result.reason());var c=result.observation();consistent(c);
            var caster=c.actors().get(0);var victim=c.actors().get(1);
            assertEquals(80,caster.resources().vitals().mp());assertEquals(1,caster.materials().uses());assertEquals(3,caster.materials().stocks().get(0).remaining());
            assertEquals(before.actors().get(0).weight().view().totalWeight()-2,caster.weight().view().totalWeight());
            assertEquals(before.actors().get(1).resources(),victim.resources());assertEquals(64*SECOND,victim.buffs().slowUntilNanos());
            assertEquals(7,caster.materials().scope().sourceId());assertEquals(42,victim.pose().member().identity().sourceId());assertEquals(1,f.draws.get());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,1).status());f.elapsed(399*MS);
            assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,move(f,1,2).status());f.elapsed(400*MS);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,3).status());
            f.elapsed(64*SECOND);var expired=f.engine.serviceObservation();assertEquals(0,expired.actors().get(1).buffs().slowUntilNanos());consistent(expired);
        }
    }
    static ModernBattleEngine.MoveResult move(Fixture f,int i,long seq) {
        var c=f.engine.serviceObservation();var p=f.players.get(i);var at=c.actors().get(i).pose().position();
        return f.engine.move(p.owner(),p.scope(),c.identity().matchId(),c.identity().generation(),seq,at.x(),at.y(),0);
    }
    @Test void missIsPaidAndReplayCooldownAndExhaustedMaterialsCannotChargeAgain()throws Exception {
        try(var f=fixture(1,100,1000,false)) {
            active(f);var first=cast(f,0,1,1);assertEquals(NONE,first.reason());
            var a=first.observation().actors().get(0);assertEquals(ModernSlowReceipt.Outcome.MISS,((ModernSlowReceipt)a.runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail()).outcome());
            assertEquals(80,a.resources().vitals().mp());assertFalse(a.materials().hasSlowMaterial());assertEquals(0,first.observation().actors().get(1).buffs().slowUntilNanos());
            assertThrows(SecurityException.class,()->cast(f,0,1,1));assertEquals(TOO_FAST,cast(f,0,2,1).reason());
            f.elapsed(2*SECOND);assertEquals(NO_MATERIAL,cast(f,0,3,1).reason());assertEquals(1,f.draws.get());consistent(f.engine.serviceObservation());
        }
        try(var f=fixture(1,19,1000,false)){active(f);assertEquals(NO_MP,cast(f,0,1,1).reason());assertEquals(0,f.draws.get());assertEquals(0,f.engine.serviceObservation().actors().get(0).materials().uses());}
    }
    @Test void forgedNamespaceAndStaleTargetDoNotDrawOrDebit()throws Exception {
        try(var f=fixture()) {
            var c=active(f);var t=target(c,1);var stale=new ModernSlowCast.Player(t.participant(),t.life()+1,t.equipmentRevision(),t.cell(),t.buffRevision());
            assertEquals(STALE_TARGET,f.engine.slow(f.players.get(0).owner(),request(c,0,1,stale)).reason());
            var forged=new ModernSlowCast.Player(new ModernTargetHistory.Participant(2,ModernParticipantId.human(7,3333)),1,0,t.cell(),0);
            assertThrows(SecurityException.class,()->f.engine.slow(f.players.get(0).owner(),request(c,0,2,forged)));
            assertEquals(0,f.draws.get());assertEquals(0,f.engine.serviceObservation().actors().get(0).materials().uses());
        }
    }
    @Test void rngClockExpiryAndInvalidDrawRejectBeforeAnyAdditionalDebit()throws Exception {
        try(var f=fixture()) {
            active(f);f.randomMode=3;assertEquals(NONE,cast(f,0,1,1).reason());f.elapsed(2*SECOND);
            f.jumpOnRandom=64*SECOND;var denied=cast(f,0,2,1);assertEquals(STALE_TARGET,denied.reason());
            assertEquals(1,denied.observation().actors().get(0).materials().uses());assertEquals(80,denied.observation().actors().get(0).resources().vitals().mp());
            consistent(denied.observation());
        }
        for(int mode:new int[]{1,2})try(var f=fixture()) {active(f);f.randomMode=mode;assertEquals(NUMERIC,cast(f,0,1,1).reason());assertEquals(0,f.engine.serviceObservation().actors().get(0).materials().uses());}
    }
    @Test void sameSequenceConcurrentCommandsCanCommitOnlyOnePaidAttempt()throws Exception {
        try(var f=fixture()) {
            var c=active(f);f.randomMode=3;var req=request(c,0,1,target(c,1));var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                var jobs=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++)jobs.add(pool.submit(()->{start.await();try{return f.engine.slow(f.players.get(0).owner(),req).reason()==NONE;}catch(SecurityException duplicate){return false;}}));
                start.countDown();int successes=0;for(var j:jobs)if(j.get(5,TimeUnit.SECONDS))successes++;
                assertEquals(1,successes);assertEquals(1,f.draws.get());assertEquals(1,f.engine.serviceObservation().actors().get(0).materials().uses());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void potionUsePreservesMaterialsAndTerminalKillClearsSpeedWithoutErasingCosts()throws Exception {
        try(var f=fixture(4,100,1,true)) {
            active(f);f.randomMode=3;assertEquals(NONE,cast(f,0,1,1).reason());
            assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());var afterItem=f.engine.serviceObservation();consistent(afterItem);
            assertEquals(3,afterItem.actors().get(0).materials().stocks().get(0).remaining());f.elapsed(2*SECOND);
            var kill=f.cast(0,2,4);assertEquals(ModernSpellProtocol.Reason.NONE,kill.reason());var ended=kill.observation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,ended.phase());assertEquals(0,ended.actors().get(1).buffs().slowUntilNanos());
            assertEquals(1,ended.actors().get(0).materials().uses());consistent(ended);
        }
    }
}
