package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Real room tickets and engine commands; no dispatcher, wire or UI claim. */
class ModernBattlePlayerHealingTest {
    static final long SECOND=1_000_000_000L;
    static ModernBattleEngineHonorTest.Fixture fixture(int bots,long secondHp,long mp)throws Exception {
        return new ModernBattleEngineHonorTest.Fixture(1000,secondHp,mp,SECOND,bots,false,true,ModernBattleEngine.PreparedMember::withPlayerHealing);
    }
    static ModernBattleObservation.Capture active(ModernBattleEngineHonorTest.Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
        var c=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());verify(c);return c;
    }
    static void verify(ModernBattleObservation.Capture c) {
        var world=c.world();assertTrue(world.features().playerHealing());assertEquals(2,c.ownerViews(world).size());
        for(var a:world.actors())assertNotNull(a.playerHealing());
    }
    static ModernPlayerHealingCast.Request request(ModernBattleObservation.Capture c,int who,long sequence,int target) {
        var a=c.actors().get(who);var t=c.actors().get(target);
        return new ModernPlayerHealingCast.Request(a.scope(),c.identity().matchId(),c.identity().generation(),sequence,
                a.resources().equipmentRevision(),a.resources().life()+1,a.pose().position(),new ModernPlayerHealingCast.Target(
                        new ModernTargetHistory.Participant(t.pose().entityId(),t.pose().member().identity()),t.resources().life()+1,
                        t.resources().equipmentRevision(),t.pose().position()));
    }
    static ModernBattleEngine.PlayerHealingResult heal(ModernBattleEngineHonorTest.Fixture f,int who,long sequence,int target) {
        var r=f.engine.playerHeal(f.players.get(who).owner(),request(f.engine.serviceObservation(),who,sequence,target));verify(r.observation());return r;
    }
    @Test void engineSelfHealUpdatesResourcesHistoriesAndBothPrivateViewsWithoutCallingLegacySnapshot()throws Exception {
        try(var f=fixture(0,100,100)) {
            active(f);var hit=f.cast(1,1,4,1);assertEquals(ModernSpellProtocol.Reason.NONE,hit.reason());
            var before=hit.observation().actors().get(0).resources();assertTrue(before.vitals().hp()>0&&before.vitals().hp()<100);
            var result=heal(f,0,1,0);assertEquals(NONE,result.reason());var a=result.observation().actors().get(0);
            assertEquals(Math.min(100,before.vitals().hp()+8),a.resources().vitals().hp());assertEquals(96,a.resources().vitals().mp());
            assertEquals(Math.min(8,100-before.vitals().hp()),a.playerHealing().incoming().restored());assertEquals(0,a.regeneration().itemGain());
            assertEquals(1,a.runtime().playerHealing().casts());assertEquals(a.playerHealing().outgoing().last().impact(),a.playerHealing().incoming().last());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,4,2).reason());
            assertThrows(SecurityException.class,()->heal(f,0,2,0));
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(result.observation()));
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(result.observation().forOwner(f.players.get(0).scope()).owner().history().history()));
            f.elapsed(2*SECOND);assertEquals(NONE,heal(f,0,3,1).reason());
        }
    }
    @Test void crossSourceSameObjidAndOppositeTeamHealPaysOnlyTheCasterAndKeepsPublicEpochPrivate()throws Exception {
        try(var f=fixture(0,100,100)) {
            var initial=active(f);assertNotEquals(initial.actors().get(0).pose().member().team(),initial.actors().get(1).pose().member().team());
            f.cast(0,1,4,2);f.elapsed(2*SECOND);var before=f.engine.serviceObservation();
            var result=heal(f,0,2,1);assertEquals(NONE,result.reason());var c=result.observation();var a=c.actors().get(0);var b=c.actors().get(1);
            assertEquals(before.actors().get(0).resources().vitals().mp()-4,a.resources().vitals().mp());
            assertEquals(before.actors().get(1).resources().vitals().mp(),b.resources().vitals().mp());
            assertEquals(Math.min(100,before.actors().get(1).resources().vitals().hp()+8),b.resources().vitals().hp());
            assertEquals(0,a.playerHealing().incoming().received());assertEquals(1,b.playerHealing().incoming().received());
            assertEquals(7,b.playerHealing().incoming().last().caster().identity().sourceId());assertEquals(42,b.playerHealing().incoming().recipient().identity().sourceId());
            assertEquals(a.playerHealing().outgoing().last().impact(),c.world().actors().get(0).playerHealing().receipt().impact());
        }
    }
    @Test void fullHpAndServerBotTargetsStillReceiveEventsAndNoDatabaseCharacterAlias()throws Exception {
        try(var f=fixture(1,100,100)) {
            active(f);var result=heal(f,0,1,2);assertEquals(NONE,result.reason());var bot=result.observation().actors().get(2);
            assertFalse(bot.playerHealing().incoming().recipient().identity().isHuman());assertNull(bot.playerHealing().outgoing());
            assertEquals(1,bot.playerHealing().incoming().received());assertEquals(0,bot.playerHealing().incoming().restored());
            assertEquals(40,bot.resources().vitals().hp());assertEquals(4,result.observation().actors().get(0).resources().flows().spent());
            assertEquals(0,bot.resources().flows().spent());assertEquals(2,f.engine.ownerCount());
        }
    }
    @Test void rngFailureTimeoutCancellationAndChangedWorldCannotPartiallyChargeOrHeal()throws Exception {
        for(int mode=0;mode<4;mode++)try(var f=fixture(0,100,100)) {
            var before=active(f);if(mode==0)f.invalid=true;if(mode==1)f.jump=1800*SECOND;if(mode==2)f.cancel=true;
            if(mode==3)f.randomOverride=bound->{f.randomOverride=null;var pos=before.actors().get(1).pose().position();
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(f.players.get(1).owner(),f.players.get(1).scope(),f.ticket.matchId(),f.ticket.generation(),1,pos.x(),pos.y(),2).status());return bound-1;};
            var result=heal(f,0,1,1);
            assertEquals(mode==0?NUMERIC:mode==3?STALE_TARGET:NOT_ACTIVE,result.reason());
            for(int i=0;i<2;i++) {
                var a=result.observation().actors().get(i);assertEquals(before.actors().get(i).resources().flows().spent(),a.resources().flows().spent());
                assertEquals(before.actors().get(i).playerHealing().incoming(),a.playerHealing().incoming());
            }
            assertNull(result.observation().actors().get(0).runtime().playerHealing());
        }
    }
    @Test void oldTargetLifeAndForeignParticipantIdentityAreRejectedBeforeRng()throws Exception {
        try(var f=fixture(0,1,100)) {
            var initial=active(f);var old=request(initial,0,2,1);f.cast(0,1,4,2);f.elapsed(5*SECOND);var now=f.engine.serviceObservation();
            assertEquals(1,now.actors().get(1).resources().life());int draws=f.draws;
            var result=f.engine.playerHeal(f.players.get(0).owner(),old);assertEquals(STALE_TARGET,result.reason());assertEquals(draws,f.draws);
            var current=request(result.observation(),0,3,1);var t=current.target();
            var alias=new ModernPlayerHealingCast.Request(current.scope(),current.matchId(),current.generation(),current.sequence(),current.equipmentRevision(),current.life(),current.cell(),
                    new ModernPlayerHealingCast.Target(new ModernTargetHistory.Participant(t.participant().entityId(),ModernParticipantId.human(7,3333)),t.life(),t.equipmentRevision(),t.cell()));
            assertThrows(SecurityException.class,()->f.engine.playerHeal(f.players.get(0).owner(),alias));assertEquals(draws,f.draws);
            assertEquals(NONE,f.engine.playerHeal(f.players.get(0).owner(),current).reason());
        }
    }
    @Test void unlearnedAndExhaustedMpFailBeforeRandomWithoutInventingHealing()throws Exception {
        try(var f=fixture(0,100,3)) {
            active(f);int draws=f.draws;var result=heal(f,0,1,1);assertEquals(NO_MP,result.reason());assertEquals(draws,f.draws);
            assertEquals(3,result.observation().actors().get(0).resources().vitals().mp());
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,p->new ModernBattleEngine.PreparedMember(
                p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),p.skills().skills().stream().filter(s->s.skillId()!=1).toList()),
                p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled()).withPlayerHealing())) {
            active(f);int draws=f.draws;assertEquals(UNLEARNED,heal(f,0,1,1).reason());assertEquals(draws,f.draws);
        }
    }
    @Test void duplicateRequestsIncludingReentrantRngCanCommitAtMostOnce()throws Exception {
        try(var f=fixture(0,100,100)) {
            var start=active(f);var request=request(start,0,1,1);var owner=f.players.get(0).owner();
            f.randomOverride=bound->{f.randomOverride=null;assertEquals(NONE,f.engine.playerHeal(owner,request).reason());return bound-1;};
            assertThrows(SecurityException.class,()->f.engine.playerHeal(owner,request));var after=f.engine.serviceObservation();verify(after);
            assertEquals(1,after.actors().get(0).runtime().playerHealing().casts());assertEquals(4,after.actors().get(0).resources().flows().spent());
            assertEquals(1,after.actors().get(1).playerHealing().incoming().received());
        }
        try(var f=fixture(0,100,100)) {
            var request=request(active(f),0,1,1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Callable<Boolean> cast=()->{try{return f.engine.playerHeal(f.players.get(0).owner(),request).reason()==NONE;}catch(SecurityException duplicate){return false;}};
                var first=pool.submit(cast);var second=pool.submit(cast);
                assertEquals(1,(first.get(5,java.util.concurrent.TimeUnit.SECONDS)?1:0)+(second.get(5,java.util.concurrent.TimeUnit.SECONDS)?1:0));
                var after=f.engine.serviceObservation();verify(after);assertEquals(1,after.actors().get(0).runtime().playerHealing().casts());
                assertEquals(4,after.actors().get(0).resources().flows().spent());assertEquals(1,after.actors().get(1).playerHealing().incoming().received());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));}
        }
    }
    static ModernBattleEngine.PreparedMember place(ModernBattleEngine.PreparedMember p,int x,boolean heavy) {
        var supplies=heavy?p.matchItems().stream().map(s->new ModernShopAuthority.Stack(s.offer(),ModernShopAuthority.MAX_QUANTITY)).toList():p.matchItems();
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),ModernBattleEngineHonorTest.cell(x,10),p.stepIntervalNanos(),
                p.fighter(),p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),supplies,p.weightBasis(),
                p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled()).withPlayerHealing();
    }
    @Test void nativeScreenBoundaryIsNotTruncatedToOffensiveSixteenCellLineLimit()throws Exception {
        for(int[] pair:List.of(new int[]{10,28},new int[]{30,11},new int[]{10,29})) {
            boolean expected=ModernHonor.inScreen(pair[0],10,pair[1],10);
            try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,
                    p->place(p,p.scope().sourceId()==7?pair[0]:pair[1],false))) {
                active(f);var result=heal(f,0,1,1);assertEquals(expected?NONE:OUT_OF_RANGE,result.reason());
                assertEquals(expected?4:0,result.observation().actors().get(0).resources().flows().spent());
                if(!expected)assertEquals(0,f.draws);
            }
        }
    }
    @Test void realBlockedEdgeAndQualifiedOverweightDenyBeforeRandomOrSpend()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,
                ModernBattleEngine.PreparedMember::withPlayerHealing,
                bytes->{ModernWorldMapTest.setAttributes(bytes,10,10,(short)0,(short)1);return bytes;})) {
            active(f);assertEquals(BLOCKED,heal(f,0,1,1).reason());assertEquals(0,f.draws);
            assertEquals(0,f.engine.serviceObservation().actors().get(0).resources().flows().spent());
            assertEquals(NONE,heal(f,0,2,0).reason());
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,
                p->place(p,p.scope().sourceId()==7?10:11,true))) {
            active(f);assertFalse(f.engine.serviceObservation().actors().get(0).weight().view().permitsAttackCast());
            assertEquals(TOO_HEAVY,heal(f,0,1,0).reason());assertEquals(0,f.draws);
            assertEquals(0,f.engine.serviceObservation().actors().get(0).resources().flows().spent());
        }
    }
    @Test void woundedBotReceivesPositiveHealingWithoutPotionOrDatabaseOwner()throws Exception {
        try(var f=fixture(1,100,100)) {
            var initial=active(f);int attacker=initial.actors().get(0).pose().member().team()==initial.actors().get(2).pose().member().team()?1:0;
            f.randomOverride=bound->bound==100?99:0;
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(attacker,1,4,3).reason());var hurt=f.engine.serviceObservation();
            var bot=hurt.actors().get(2);assertTrue(bot.resources().vitals().hp()>0&&bot.resources().vitals().hp()<40);
            f.randomOverride=null;int healer=1-attacker;var healed=heal(f,healer,1,2);assertEquals(NONE,healed.reason());
            var after=healed.observation().actors().get(2);long gained=after.resources().vitals().hp()-bot.resources().vitals().hp();
            assertTrue(gained>0);assertEquals(gained,after.playerHealing().incoming().restored());assertEquals(gained,after.resources().flows().restored());
            assertEquals(0,after.regeneration().itemGain());assertNull(after.playerHealing().outgoing());assertNull(after.scope());
            assertEquals(2,healed.observation().ownerViews(healed.observation().world()).size());assertEquals(2,f.engine.ownerCount());
        }
    }
}
