package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Actual engine/ticket transitions, synthetic immutable catalogs. Internal opt-in only:
 * these tests do not qualify 11101 resources, legacy packets, SQL or player UI. */
class ModernBattleEngineArmorProtectionTest {
    static final long SECOND=1_000_000_000L;
    static ModernBattleObservation.World active(ModernBattleEngineHonorTest.Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
        return f.engine.serviceObservation().world();
    }
    static ModernBattleObservation.World active(ModernBattleEngineNpcTest.Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
        f.clock.addAndGet(f.limits.countdownNanos());return f.engine.serviceObservation().world();
    }
    static ModernBattleObservation.World active(ModernTimeAttackEngineTest.Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.binding.map().digest());
        assertEquals(ModernBattleEngine.Phase.COUNTDOWN,f.engine.serviceObservation().phase());
        f.elapsed(0);var w=f.engine.serviceObservation().world();assertEquals(ModernBattleEngine.Phase.ACTIVE,w.phase());return w;
    }
    static ModernNpcRoster.Actor living(ModernBattleEngine e,int lane) {
        return e.serviceObservation().npcs().roster().actors().stream().filter(a->a.alive()&&a.lane()==lane).findFirst().orElseThrow();
    }
    static ModernNpcRoster.Actor spawn(ModernBattleEngineNpcTest.Fixture f,int lane,int x,int y) {
        var t=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
        var roster=f.engine.spawnNpcsObservation(f.scope,List.of(new ModernNpcRoster.Spawn(t,lane,1,ModernBattleEngineNpcTest.cell(x,y),2))).npcs().roster();
        return roster.actors().get(roster.actors().size()-1);
    }
    static ModernBattleEngine.MoveResult move(ModernBattleEngineHonorTest.Fixture f,int who,long seq,int heading) {
        var p=f.players.get(who);var pos=f.engine.serviceObservation().actors().get(who).pose().position();
        return f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),seq,pos.x(),pos.y(),heading);
    }
    static void clear(ModernBattleEngineNpcTest.Fixture f,int lane,int stage,long seq,long millis) {
        f.elapsed(millis);var npc=living(f.engine,lane);assertEquals(stage,npc.stage());
        assertEquals(ModernNpcBattle.Reason.ATTACKED,f.attack(lane,seq,npc).reason());
    }
    static ModernBattleObservation.World die(ModernBattleEngineNpcTest.Fixture f,long millis) {
        f.elapsed(millis);var hit=f.step(living(f.engine,0));assertEquals(ModernNpcBattle.Reason.ATTACKED,hit.reason());
        var w=hit.observation().world();assertEquals(0,w.actors().get(0).vital().values().hp());assertTrue(w.modes().quest().lives().get(0).dead());return w;
    }

    static ModernBattleEngine.PreparedMember protect(ModernBattleEngine.PreparedMember p,ModernPhysicalCombat.Source seed) {
        var fighter=ModernArmorProtectionLoadoutTest.qualify(ModernArmorProtectionLoadoutTest.protection(seed));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,
                p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),
                p.weightBasis()==null?null:ModernInventoryWeight.qualify(fighter.profile(),p.weightBasis().sourceRate()),
                p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate(),
                p.greaterHaste(),p.haste(),p.slow(),p.playerHealing());
    }
    static ModernBattleEngine.PreparedMember protectHonor(ModernBattleEngine.PreparedMember p) {
        var original=p.fighter().profile().source();
        var seed=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.character(),original.persisted().maxHp(),original.persisted().maxMp(),0);
        var q=seed.input().equipment();
        var snapshot=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),original.persisted(),q.source().inventory());
        var qualified=new ModernEquipmentFacts.Qualified(snapshot,q.items(),q.catalog(),q.lifetimes());
        var s=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,seed.input().weapons()),seed.growth(),seed.templates(),
                seed.procCatalogIds(),seed.magicGrowth(),seed.magicBows(),seed.weaponArrowSprites(),seed.regenGrowth(),seed.healingGrowth());
        return protect(p,s);
    }
    static ModernBattleEngineHonorTest.Fixture fixture(long hp,boolean disintegrate)throws Exception {
        return new ModernBattleEngineHonorTest.Fixture(10000,hp,1000,SECOND,0,true,true,p->protectHonor(
                ModernBattleEngineDisintegrateTest.upgrade(p,500,12,false,disintegrate?ModernDisintegrateCatalog.Status.QUALIFIED:ModernDisintegrateCatalog.Status.UNLEARNED)));
    }
    static ModernBattleEngine.ArmorProtectionView armor(ModernBattleEngineHonorTest.Fixture f){return f.engine.armorProtectionSnapshot().get(2L);}
    static long hp(ModernBattleObservation.World s){return s.actors().get(1).vital().values().hp();}
    static void committedAnchor(ModernBattleObservation.Capture c,int who) {
        var a=c.actors().get(who);var v=a.armorProtection();assertFalse(v.triggers().isEmpty());
        assertNotNull(v.anchor());assertEquals(c.revision(),v.revision());
        assertEquals(c.clock().activeElapsedNanos(),v.state().observedNanos());
        assertEquals(a.pose().lastMoveSequence(),v.anchor().moveSequence());assertEquals(a.pose().position(),v.anchor().cell());
    }
    @Test void firstMeleeHitDoesNotProtectItselfSecondHitHalvesAndWinningProcDoesNotRefresh()throws Exception {
        try(var f=fixture(10000,false)) {
            var before=active(f);var first=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,first.status());
            long full=hp(before)-hp(first.observation().world());assertTrue(full>0);
            var initial=armor(f);assertEquals(3*SECOND,initial.state().untilNanos());assertEquals(first.observation().world().revision(),initial.revision());
            assertTrue(initial.triggers().get(0).appliedProtection());
            committedAnchor(first.observation(),1);
            f.elapsed(SECOND);var second=f.attack(0,2,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,second.status());
            assertEquals(full/2,hp(first.observation().world())-hp(second.observation().world()));
            assertEquals(3*SECOND,armor(f).state().untilNanos());assertFalse(armor(f).triggers().get(0).appliedProtection());
            f.elapsed(3*SECOND);assertEquals(0,armor(f).state().untilNanos());assertTrue(armor(f).triggers().isEmpty());
            var third=f.attack(0,3,2);assertEquals(full,hp(second.observation().world())-hp(third.observation().world()));assertEquals(6*SECOND,armor(f).state().untilNanos());
        }
    }
    @Test void arrowFirestormAndDisintegrateUseOldProtectionAtTheirActualEngineCommit()throws Exception {
        for(int lane:new int[]{4,70,77})try(var normal=fixture(10000,true);var protectedWorld=fixture(10000,true)) {
            active(normal);active(protectedWorld);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,protectedWorld.attack(0,1,2).status());
            normal.elapsed(2*SECOND);protectedWorld.elapsed(2*SECOND);
            long beforeNormal=hp(normal.engine.serviceObservation().world()),beforeProtected=hp(protectedWorld.engine.serviceObservation().world());
            ModernBattleObservation.World n,p;
            if(lane==77) {
                var x=ModernBattleEngineDisintegrateTest.cast(normal,1,2);var y=ModernBattleEngineDisintegrateTest.cast(protectedWorld,1,2);
                assertEquals(ModernDisintegrateState.Reason.NONE,x.reason());assertEquals(x.reason(),y.reason());n=x.observation().world();p=y.observation().world();
                committedAnchor(x.observation(),1);committedAnchor(y.observation(),1);
                assertEquals(n.actors().get(0).spells().histories().get(ModernUseHistory.Lane.DISINTEGRATE).current().spent(),p.actors().get(0).spells().histories().get(ModernUseHistory.Lane.DISINTEGRATE).current().spent());
            }else {
                var x=normal.cast(0,1,lane,lane==70?0:2);var y=protectedWorld.cast(0,1,lane,lane==70?0:2);
                assertEquals(ModernSpellProtocol.Reason.NONE,x.reason());assertEquals(x.reason(),y.reason());n=x.observation().world();p=y.observation().world();
                committedAnchor(x.observation(),1);committedAnchor(y.observation(),1);
            }
            assertTrue(beforeNormal>hp(n));assertEquals((beforeNormal-hp(n))/2,beforeProtected-hp(p),"lane "+lane);
            assertEquals(5*SECOND,armor(normal).state().untilNanos());assertEquals(3*SECOND,armor(protectedWorld).state().untilNanos());
            assertEquals(p.revision(),armor(protectedWorld).revision());
        }
    }
    @Test void expiredProtectionDuringRandomRejectsWithoutHpMpOrProcCommitAndWithoutReroll()throws Exception {
        for(int lane:new int[]{0,4,70,77})try(var f=fixture(10000,true)) {
            active(f);f.attack(0,1,2);f.elapsed(2*SECOND);var before=f.engine.serviceObservation().world();int calls=f.draws;
            f.jump=SECOND;
            if(lane==0)assertEquals(ModernBattleEngine.AttackStatus.STALE_EFFECT,f.attack(0,2,2).status());
            else if(lane==77)assertEquals(ModernDisintegrateState.Reason.STALE_EFFECT,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());
            else assertEquals(ModernSpellProtocol.Reason.STALE_EFFECT,f.cast(0,1,lane,lane==70?0:2).reason());
            var after=f.engine.serviceObservation().world();assertEquals(hp(before),hp(after));assertEquals(before.actors().get(0).vital().values().mp(),after.actors().get(0).vital().values().mp());
            assertEquals(0,armor(f).state().untilNanos());assertTrue(armor(f).triggers().isEmpty());
            assertTrue(f.draws>calls);int done=f.draws;f.engine.serviceObservation().world();armor(f);assertEquals(done,f.draws);
        }
    }
    @Test void newProcLeaseStartsAtAcceptedTimeRatherThanBeforeSlowRandom()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);f.jump=SECOND;assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            assertEquals(4*SECOND,armor(f).state().untilNanos());assertEquals(SECOND,armor(f).state().observedNanos());
        }
    }
    @Test void actorMutationDuringRandomRejectsWholeStormIncludingObjectiveAndMana()throws Exception {
        try(var f=fixture(10000,false)) {
            var before=active(f);var once=new AtomicBoolean();
            f.randomOverride=b->{if(once.compareAndSet(false,true))assertEquals(ModernBattleEngine.MoveStatus.MOVED,move(f,1,1,2).status());return b-1;};
            var cast=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.STALE_REGEN,cast.reason());
            assertEquals(hp(before),hp(cast.observation().world()));assertEquals(before.actors().get(0).vital().values().mp(),cast.observation().world().actors().get(0).vital().values().mp());
            assertEquals(before.modes().honor().objectives(),cast.observation().world().modes().honor().objectives());assertEquals(0,armor(f).state().sequence());
        }
    }
    @Test void lethalHitNeverGrantsProtectionAndRespawnGetsANewLifeWithoutOldEvents()throws Exception {
        try(var f=fixture(1,false)) {
            active(f);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
            var dead=armor(f);assertEquals(0,dead.state().untilNanos());assertTrue(dead.triggers().isEmpty());
            f.elapsed(5*SECOND);assertEquals(1,hp(f.engine.serviceObservation().world()));var revived=armor(f);
            assertEquals(dead.state().actor().life()+1,revived.state().actor().life());assertEquals(0,revived.state().sequence());
            assertEquals(dead.state().actor().match(),revived.state().actor().match());
        }
    }
    @Test void abortClearsActiveLeaseAndCloseCannotExposeOldWorldState()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);f.attack(0,1,2);assertTrue(armor(f).state().active(0));f.engine.abortObservation();
            assertEquals(0,armor(f).state().untilNanos());assertTrue(armor(f).triggers().isEmpty());
            f.engine.close();assertThrows(IllegalStateException.class,f.engine::armorProtectionSnapshot);
        }
    }
    @Test void npcIncomingDamageUsesSamePlayerStateAndIndependentHpLedger()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,10000,0,java.util.function.UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false)) {
            var members=f.prepared.stream().map(p->protect(p,ModernNaturalRegenTest.source(p.scope().sourceId(),p.character(),10000,30,0,0,0))).toList();
            f.engine=f.build(f.config,false,members);active(f);f.randomValue=Long.MAX_VALUE;
            var npc=spawn(f,0,11,10);f.elapsed(100);var first=f.step(npc);assertEquals(ModernNpcBattle.Reason.ATTACKED,first.reason());
            committedAnchor(first.observation(),0);
            long full=10000-first.observation().world().actors().get(0).vital().values().hp();assertTrue(full>0);
            assertEquals(3*SECOND+100_000_000,f.engine.armorProtectionSnapshot().get(1L).state().untilNanos());
            f.elapsed(1000);var second=f.step(npc);assertEquals(ModernNpcBattle.Reason.ATTACKED,second.reason());
            assertEquals(full/2,first.observation().world().actors().get(0).vital().values().hp()-second.observation().world().actors().get(0).vital().values().hp());
        }
    }
    static ModernPhysicalCombat.Source equipArmor(ModernPhysicalCombat.Source s) {
        var q=s.input().equipment();var items=new HashMap<>(q.items());
        for(var entry:List.copyOf(items.values()))if(entry.source().template().kind()==ModernCharacterData.ItemKind.ARMOR) {
            var old=entry.source();var next=new ModernCharacterData.InventoryItem(old.objectId(),old.template(),old.quantity(),true,
                    old.enchantment(),old.durability(),old.identified(),old.blessing(),old.sealed(),old.chargeCount());
            items.put(old.objectId(),new ModernEquipmentFacts.Instance(next,entry.template(),entry.greater(),entry.dark(),entry.cards(),entry.stone()));
        }
        var snapshot=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),q.source().persisted(),
                items.values().stream().map(ModernEquipmentFacts.Instance::source).sorted(Comparator.comparingLong(ModernCharacterData.InventoryItem::objectId)).toList());
        var qualified=new ModernEquipmentFacts.Qualified(snapshot,items,q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,s.input().weapons()),s.growth(),s.templates(),
                s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    @Test void bowCommitsExactlyOneArrowWithProtectedHpAndNoExtraRecovery()throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(2,0)) {
            var ticket=room.capture();var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.players) {
                boolean self=p.scope().sourceId()==7;
                var base=ModernPhysicalCombatTest.fixtureSource(p.scope().sourceId(),p.row(),10000,30,0);
                var source=equipArmor(ModernCombatTestPeer.ammoSource(ModernCombatTestPeer.weightSource(base),self));
                var fighter=ModernPhysicalCombatTest.fighter(source);
                var stocks=self?List.of(new ModernShopAuthority.Stack(ModernCombatTestPeer.arrow(40744,0,20),3),
                        new ModernShopAuthority.Stack(ModernCombatTestPeer.arrow(40746,1,30),2)):List.<ModernShopAuthority.Stack>of();
                var member=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.row(),ModernBattleEngineBotTest.cell(10+members.size(),10),
                        200_000_000L,fighter,SECOND,null,0,false,stocks,ModernInventoryWeight.qualify(fighter.profile(),100),true,true);
                members.add(protect(member,source));
            }
            var clock=new java.util.concurrent.atomic.AtomicLong(SECOND);
            var duringRandom=new java.util.concurrent.atomic.AtomicReference<Runnable>();
            try(var engine=new ModernBattleEngine(ticket,room.binding,members,List.of(),new ModernBattleEngine.Limits(2000,0,60000),clock::get,()->1000,
                    b->{var callback=duringRandom.getAndSet(null);if(callback!=null)callback.run();return b-1;},false)) {
                for(var p:members)engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());
                var p=members.get(0);var first=engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),1,2);
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,first.status());long full=10000-hp(first.observation().world());assertTrue(full>0);
                committedAnchor(first.observation(),1);
                assertEquals(2,first.observation().actors().get(0).ammo().selected().remaining());clock.addAndGet(SECOND);
                var second=engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),2,2);
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,second.status());assertEquals(full/2,hp(first.observation().world())-hp(second.observation().world()));
                assertEquals(1,second.observation().actors().get(0).ammo().selected().remaining());assertEquals(3*SECOND,engine.armorProtectionSnapshot().get(2L).state().untilNanos());
                assertEquals(first.observation().actors().stream().map(ModernBattleObservation.CapturedActor::supplies).toList(),second.observation().actors().stream().map(ModernBattleObservation.CapturedActor::supplies).toList());
                var beforeArmor=engine.armorProtectionSnapshot();clock.addAndGet(SECOND);
                duringRandom.set(()->assertEquals(ModernAmmoProtocol.Reason.NONE,engine.ammo(p.owner(),new ModernAmmoProtocol.Request(
                        p.scope(),ModernAmmoProtocol.Action.SELECT,ticket.matchId(),ticket.generation(),1,40746)).reason()));
                var stale=engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),3,2);
                assertEquals(ModernBattleEngine.AttackStatus.STALE_EFFECT,stale.status());assertEquals(hp(second.observation().world()),hp(stale.observation().world()));
                assertEquals(2,stale.observation().actors().get(0).ammo().shotCount());assertEquals(40746,stale.observation().actors().get(0).ammo().selected().itemId());
                assertEquals(2,stale.observation().actors().get(0).ammo().selected().remaining());assertEquals(beforeArmor,engine.armorProtectionSnapshot());
            }finally{room.rooms.releasePreparation(ticket);}
        }
    }
    @Test void timeAttackDepartureClearsOnlyDepartingLeaseAndLastExitClearsTheWorld()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,10000,false,1,false,true,false,1,false,false,20000,
                ModernBattleEngineArmorProtectionTest::protectHonor)) {
            active(f);f.tower(0,1);var npc=living(f.engine,0);f.elapsed(1000);f.random=99;
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpc(npc.id(),npc.life(),4096,false).reason());
            assertTrue(f.engine.armorProtectionSnapshot().get(1L).state().active(SECOND));
            var departed=f.engine.withdrawTimeAttackObservation(f.players.get(0).owner());assertEquals(ModernBattleEngine.Phase.ACTIVE,departed.phase());
            assertEquals(0,f.engine.armorProtectionSnapshot().get(1L).state().untilNanos());
            assertTrue(f.engine.armorProtectionSnapshot().get(1L).triggers().isEmpty());
            f.elapsed(2000);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpc(npc.id(),npc.life(),4096,false).reason());
            assertTrue(f.engine.armorProtectionSnapshot().get(2L).state().active(2*SECOND));
            assertEquals(ModernBattleEngine.Phase.FINISHED,f.engine.withdrawTimeAttackObservation(f.players.get(1).owner()).phase());
            assertTrue(f.engine.armorProtectionSnapshot().values().stream().allMatch(v->v.state().untilNanos()==0&&v.triggers().isEmpty()));
        }
    }
    @Test void badLateProcRandomCannotPartiallyCommitAnyMagicLane()throws Exception {
        for(int lane:new int[]{0,4,70,77})try(var f=fixture(10000,true)) {
            var before=active(f);var armorBefore=f.engine.armorProtectionSnapshot();
            // These synthetic unarmed/zero-armor fixtures each draw one hit or
            // critical 100-bound roll before the PC's independent proc roll.
            var counts=new java.util.concurrent.atomic.AtomicInteger();
            f.randomOverride=b->{if(b==100&&counts.incrementAndGet()==2)return -1;return b-1;};
            if(lane==0)assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,f.attack(0,1,2).status());
            else if(lane==77)assertEquals(ModernDisintegrateState.Reason.NUMERIC,ModernBattleEngineDisintegrateTest.cast(f,1,2).reason());
            else assertEquals(ModernSpellProtocol.Reason.NUMERIC,f.cast(0,1,lane,lane==70?0:2).reason());
            var after=f.engine.serviceObservation().world();assertEquals(hp(before),hp(after));assertEquals(before.actors().get(0).vital().values().mp(),after.actors().get(0).vital().values().mp());
            assertEquals(before.modes().honor().objectives(),after.modes().honor().objectives());assertEquals(armorBefore,f.engine.armorProtectionSnapshot());
            assertEquals(2,counts.get());
        }
    }
    static ModernBattleEngineNpcTest.Fixture quest(long hp)throws Exception {
        var f=new ModernBattleEngineNpcTest.Fixture(2,0,true,hp,0,java.util.function.UnaryOperator.identity(),
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),false);
        try {
            var base=ModernQuestProgressTest.plan(f.binding.key(),f.config.catalog());
            var plan=new ModernQuestProgress.Plan(base.key(),base.waves(),ModernQuestObjectivesTest.objectives(554,1,false)).withParticipantLives();
            var members=f.prepared.stream().map(p->protect(p,ModernNaturalRegenTest.source(p.scope().sourceId(),p.character(),hp,30,0,0,0))).toList();
            f.engine=f.build(new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000,plan),true,members);
            f.randomValue=19;return f;
        }catch(Throwable failure){f.close();throw failure;}
    }
    @Test void questGuardianVictoryClearsAnActiveLeaseBeforePublishingTheTerminalWorld()throws Exception {
        try(var f=quest(10000)) {
            active(f);for(int stage=1;stage<=3;stage++)clear(f,0,stage,stage,stage*1000L);
            var npc=living(f.engine,0);f.elapsed(3900);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.step(npc).reason());
            assertTrue(f.engine.armorProtectionSnapshot().get(1L).state().active(3_900_000_000L));
            clear(f,0,4,4,4000);var target=f.engine.serviceObservation().modes().quest().objectives().stream().filter(a->a.id().lane()==0).findFirst().orElseThrow();f.elapsed(4700);
            var done=ModernQuestObjectivesTest.strike(f,0,5,target.id());assertEquals(ModernNpcBattle.Reason.ATTACKED,done.reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,done.observation().world().phase());
            assertTrue(f.engine.armorProtectionSnapshot().values().stream().allMatch(v->v.state().untilNanos()==0&&v.triggers().isEmpty()));
        }
    }
    @Test void explicitQuestResurrectionGetsANewArmorLifeWithoutInheritingADeadReceipt()throws Exception {
        try(var f=quest(1)) {
            active(f);var dead=die(f,100);var prior=f.engine.armorProtectionSnapshot().get(1L);
            assertEquals(0,prior.state().untilNanos());assertTrue(prior.triggers().isEmpty());
            var result=ModernQuestLifeTest.confirm(f,0,dead.modes().quest().lives().get(0));assertEquals(ModernQuestLife.Reason.REVIVED,result.reason());
            var next=f.engine.armorProtectionSnapshot().get(1L);assertEquals(prior.state().actor().life()+1,next.state().actor().life());
            assertEquals(0,next.state().sequence());assertEquals(0,next.state().untilNanos());assertTrue(next.triggers().isEmpty());
        }
    }
}
