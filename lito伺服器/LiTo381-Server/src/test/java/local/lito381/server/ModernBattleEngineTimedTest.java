package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernTimedEffects.Kind.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Genuine room capture and qualified source fixtures. No client buff clock or client supplied effect. */
class ModernBattleEngineTimedTest {
    static final long MS=1_000_000L,SECOND=1_000*MS,STEP=200*MS,ATTACK=700*MS,CAST=1200*MS;
    static final ModernBattleEngine.Limits LIMITS=new ModernBattleEngine.Limits(2000,0,1_800_000);

    @Test void initialEffectsAreAbsentUntilBothRealOwnersAcknowledgeTheirManifest()throws Exception {
        try(var f=new Fixture()) {
            var before=f.engine.tick();assertEquals(List.of(ModernTimedEffects.initial(1),ModernTimedEffects.initial(2)),before.buffs());
            assertEquals(7,before.items().get(0).stocks().size());assertEquals(2,before.weights().size());
            assertEquals(ModernBattleEngine.Phase.LOADING,f.ack(0).phase());
            assertEquals(ModernItemProtocol.Reason.NOT_ACTIVE,f.use(0,HASTE.itemId()).reason());
            assertEquals(0,f.engine.tick().items().get(0).useCount());assertEquals(before.buffs(),f.engine.tick().buffs());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.ack(1).phase());
        }
    }
    @Test void useCommitsStockWeightAndEffectTogetherWithoutHealingOrRandomness()throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();var result=f.use(0,HASTE.itemId());var after=result.snapshot();
            assertEquals(ModernItemProtocol.Reason.NONE,result.reason());assertEquals(before.revision()+1,after.revision());
            assertEquals(0,f.draws.get());assertEquals(before.vitals(),after.vitals());
            var item=after.items().get(0);var buff=after.buffs().get(0);
            assertEquals(after.revision(),item.useRevision());assertEquals(after.revision(),buff.revision());
            assertEquals(0,item.restored());assertEquals(1000,item.hpBefore());assertEquals(item.hpBefore(),item.hpAfter());
            assertEquals(191,item.effect());assertEquals(0,item.delayMillis());assertEquals(300*SECOND,buff.hasteUntilNanos());
            assertEquals(3,f.stock(after,0,HASTE.itemId()).remaining());
            assertTrue(after.weights().get(0).snapshot().view().suppliesWeight()<before.weights().get(0).snapshot().view().suppliesWeight());
            assertEquals(STEP,after.actors().get(0).stepIntervalNanos());assertEquals(ATTACK,after.vitals().get(0).attackIntervalNanos());
            assertEquals(after,f.engine.tick(),"ordinary polling cannot resample a same-revision effect clock");
        }
    }
    @Test void hasteRefreshUsesServerElapsedAndExpiresExactlyAtTheRealThreeHundredSeconds()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());f.elapsed(100*SECOND);var refresh=f.use(0,HASTE.itemId()).snapshot();
            assertEquals(400*SECOND,refresh.buffs().get(0).hasteUntilNanos());
            f.elapsed(400*SECOND-1);assertEquals(refresh,f.engine.tick());
            f.elapsed(400*SECOND);var expired=f.engine.tick();
            assertEquals(refresh.revision()+1,expired.revision());assertFalse(expired.buffs().get(0).hasEffects());
            assertEquals(400*SECOND,expired.buffs().get(0).observedNanos());assertEquals(refresh.items(),expired.items());
            assertEquals(expired,f.engine.tick());
        }
    }
    @Test void braveFamilyReplacesWithoutDroppingHasteAndElfKeepsFourHundredEightySeconds()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(1,HASTE.itemId());f.use(1,BRAVE.itemId());f.elapsed(10*SECOND);
            var elf=f.use(1,ELF_BRAVE.itemId()).snapshot().buffs().get(1);
            assertEquals(3,elf.braveKind());assertEquals(490*SECOND,elf.braveUntilNanos());assertEquals(300*SECOND,elf.hasteUntilNanos());
            f.elapsed(300*SECOND);var hasteExpired=f.engine.tick().buffs().get(1);
            assertEquals(0,hasteExpired.hasteUntilNanos());assertTrue(hasteExpired.holds(ELF_BRAVE));
            f.elapsed(490*SECOND-1);assertEquals(hasteExpired,f.engine.tick().buffs().get(1));
            f.elapsed(490*SECOND);assertFalse(f.engine.tick().buffs().get(1).hasEffects());
            f.use(1,ELF_BRAVE.itemId());var replaced=f.use(1,BRAVE.itemId()).snapshot().buffs().get(1);
            assertEquals(1,replaced.braveKind());assertEquals(790*SECOND,replaced.braveUntilNanos());
        }
    }
    @Test void classRejectionConsumesOnlySequenceAndNeverStockWeightHpOrRandom()throws Exception {
        try(var f=new Fixture()) {
            f.active();var before=f.engine.tick();var denied=f.use(0,ELF_BRAVE.itemId());
            assertEquals(ModernItemProtocol.Reason.CLASS_RESTRICTED,denied.reason());
            assertEquals(before.buffs(),denied.snapshot().buffs());assertEquals(before.weights(),denied.snapshot().weights());
            assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.items().get(0).stocks(),denied.snapshot().items().get(0).stocks());
            assertEquals(1,denied.snapshot().items().get(0).requestSequence());assertEquals(0,denied.snapshot().items().get(0).useCount());
            assertEquals(ModernItemProtocol.Reason.CLASS_RESTRICTED,f.use(1,WISDOM.itemId()).reason());assertEquals(0,f.draws.get());
        }
    }
    @Test void healingDelaySurvivesInterleavedZeroDelayTimedUses()throws Exception {
        try(var f=new Fixture()) {
            f.active();assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40010).reason());
            f.elapsed(250*MS);f.use(0,HASTE.itemId());var before=f.engine.tick();
            assertEquals(ModernItemProtocol.Reason.TOO_FAST,f.use(0,40010).reason());assertEquals(1,f.draws.get());
            assertEquals(before.items().get(0).stocks(),f.engine.tick().items().get(0).stocks());
            f.elapsed(500*MS);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40010).reason());
            assertEquals(2,f.draws.get());assertTrue(f.engine.tick().buffs().get(0).holds(HASTE));
        }
    }
    @Test void hasteChangesTheRealMovementGateButDoesNotMutateTheBaseInterval()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,0).status());
            f.elapsed(150*MS);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,2,0).status());
            f.elapsed(151*MS);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,3,0).status());
            assertEquals(STEP,f.engine.tick().actors().get(0).stepIntervalNanos());
        }
    }
    @Test void hasteAndBraveChangeMeleeGateWithSequentialNativeMillisecondTruncation()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());f.use(0,BRAVE.itemId());assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1).status());
            long effective=ModernTimedEffects.intervalNanos(ATTACK,ModernTimedEffects.Action.ATTACK,f.engine.tick().buffs().get(0));
            assertEquals(398*MS,effective);f.elapsed(effective-1);assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,2).status());
            f.elapsed(effective);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,3).status());
            assertEquals(ATTACK,f.engine.tick().vitals().get(0).attackIntervalNanos());
        }
    }
    @Test void spellCooldownAndCrossActionChecksUseTheSameEffectivePace()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());f.use(0,BRAVE.itemId());assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4).reason());
            long effective=ModernTimedEffects.intervalNanos(CAST,ModernTimedEffects.Action.SPELL,f.engine.tick().buffs().get(0));
            f.elapsed(effective-1);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,0).status());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,1).status());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,5).reason());
            f.elapsed(effective);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,4).reason());
            assertEquals(CAST/MS,f.engine.tick().casts().get(0).castIntervalMillis());
        }
    }
    @Test void wisdomAddsTwoToQualifiedIntelligenceTermAndActuallyChangesDamageOnly()throws Exception {
        try(var f=new Fixture()) {
            f.active();var a=ModernCombatant.human(f.prepared.get(0).fighter());var b=ModernCombatant.human(f.prepared.get(1).fighter());
            var baseline=ModernCombatant.resolveLightArrow(a,b,1000,bound->bound-1);
            var boosted=ModernCombatant.resolveLightArrow(a,b,1000,bound->bound-1,2);
            assertTrue(boosted.appliedDamage()>baseline.appliedDamage());var original=f.prepared.get(0).fighter().magic().offense();
            f.use(0,WISDOM.itemId());var cast=f.cast(0,1,4);assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());
            assertEquals(boosted.remainingHp(),cast.snapshot().vitals().get(1).hp());assertEquals(97,cast.snapshot().vitals().get(0).mp());
            assertEquals(original,f.prepared.get(0).fighter().magic().offense());assertEquals(2,cast.snapshot().buffs().get(0).spBonus());
            assertEquals(0,cast.snapshot().vitals().get(0).attackRevision());assertEquals(0,cast.snapshot().casts().get(0).castCount());
        }
    }
    @Test void validAndInvalidLateRandomCannotCommitAfterTheCasterEffectExpired()throws Exception {
        for(int action=0;action<3;action++)for(int invalid=0;invalid<3;invalid++)try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());f.elapsed(300*SECOND-1);var before=f.engine.tick();
            f.jumpOnRandom=1;f.randomMode=invalid;
            ModernBattleEngine.Snapshot after;
            if(action==0){var result=f.attack(0,1);assertEquals(ModernBattleEngine.AttackStatus.STALE_EFFECT,result.status());after=result.snapshot();}
            else {var result=f.cast(0,1,action==1?5:4);assertEquals(ModernSpellProtocol.Reason.STALE_EFFECT,result.reason());after=result.snapshot();}
            assertEquals(before.revision()+2,after.revision());assertFalse(after.buffs().get(0).hasEffects());
            assertEquals(before.vitals().get(1),after.vitals().get(1));assertEquals(before.vitals().get(0).hp(),after.vitals().get(0).hp());
            assertEquals(before.vitals().get(0).mp(),after.vitals().get(0).mp());assertEquals(before.actors(),after.actors());
            assertEquals(before.items(),after.items());assertEquals(before.damages(),after.damages());
            assertEquals(0,after.vitals().get(0).attackRevision());assertEquals(0,after.casts().get(0).castCount());
            assertEquals(1,action==0?after.vitals().get(0).attackSequence():after.casts().get(0).requestSequence());
        }
    }
    @Test void anotherActorsExpiryDoesNotRejectMagicAndEventUsesTheFinalCommitRevision()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(1,HASTE.itemId());f.elapsed(300*SECOND-1);var before=f.engine.tick();f.jumpOnRandom=1;
            var cast=f.cast(0,1,4);var after=cast.snapshot();assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());
            assertEquals(before.revision()+2,after.revision());assertEquals(after.revision(),after.damages().get(0).castRevision());
            assertEquals(after.revision(),after.vitals().get(1).hurtRevision());assertFalse(after.buffs().get(1).hasEffects());
        }
    }
    @Test void sharedExpiryTickBumpsOnceAndTerminalClearCannotRetainAnActiveLease()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());f.use(1,HASTE.itemId());var before=f.engine.tick();f.elapsed(300*SECOND);
            var after=f.engine.tick();assertEquals(before.revision()+1,after.revision());
            assertTrue(after.buffs().stream().allMatch(s->s.revision()==after.revision()&&!s.hasEffects()));
            f.use(0,BRAVE.itemId());f.use(1,ELF_BRAVE.itemId());f.elapsed(2_000*SECOND);
            var terminal=f.engine.tick();assertEquals(ModernBattleEngine.Phase.ABORTED,terminal.phase());
            assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,terminal.abortReason());
            assertTrue(terminal.buffs().stream().allMatch(s->!s.hasEffects()&&s.observedNanos()<=ModernTimedEffects.MAX_OBSERVED_NANOS));
            assertEquals(terminal,f.engine.tick());
        }
    }
    @Test void lethalCommitClearsBothWinnerAndDeadLoserAtTheResultRevision()throws Exception {
        try(var f=new Fixture(3,2,1000,1,true)) {
            f.active();f.use(0,WISDOM.itemId());f.use(1,ELF_BRAVE.itemId());var result=f.cast(0,1,4).snapshot();
            assertEquals(ModernBattleEngine.Phase.FINISHED,result.phase());assertEquals(0,result.vitals().get(1).hp());
            assertEquals(result.revision(),result.result().engineRevision());
            assertTrue(result.buffs().stream().allMatch(s->!s.hasEffects()&&s.revision()==result.revision()));
            assertEquals(result,f.engine.tick());assertEquals(result,f.use(0,HASTE.itemId()).snapshot());
        }
    }
    @Test void disconnectClearsEffectsWithoutRestoringConsumedStocks()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.use(0,HASTE.itemId());f.use(1,ELF_BRAVE.itemId());var before=f.engine.tick();
            f.engine.disconnect(f.players.get(1).owner());var terminal=f.engine.tick();
            assertEquals(ModernBattleEngine.Phase.ABORTED,terminal.phase());assertEquals(before.items(),terminal.items());
            assertTrue(terminal.buffs().stream().noneMatch(ModernTimedEffects.State::hasEffects));assertEquals(before.weights(),terminal.weights());
        }
    }
    @Test void legacyWeightedConstructorHasNoBuffStateOrTimedItemAdmission()throws Exception {
        try(var f=new ModernBattleEngineWeightTest.Fixture(0,true)) {
            f.active();assertTrue(f.engine.tick().buffs().isEmpty());
            var p=f.prepared.get(0);var rejected=f.engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,
                    f.ticket.matchId(),f.ticket.generation(),1,HASTE.itemId()));
            assertEquals(ModernItemProtocol.Reason.UNSUPPORTED,rejected.reason());assertTrue(rejected.snapshot().buffs().isEmpty());
        }
    }

    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();final ModernWorldMap map;final ModernWorldBinding binding;
        final ModernRooms rooms;final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final List<ModernBattleEngine.PreparedMember> prepared;
        final ModernBattleEngine engine;final AtomicLong clock=new AtomicLong(SECOND),epoch=new AtomicLong(1000);final AtomicInteger draws=new AtomicInteger();
        final long[] itemSequences=new long[2];long jumpOnRandom;int randomMode;Runnable onRandom=()->{},onEpoch=()->{};
        Fixture()throws Exception{this(3,2,1000,1000,false);}
        Fixture(int firstClass,int secondClass,long firstHp,long secondHp,boolean settle)throws Exception {
            this(firstClass,secondClass,firstHp,secondHp,settle,java.util.function.UnaryOperator.identity());
        }
        Fixture(int firstClass,int secondClass,long firstHp,long secondHp,boolean settle,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> configure)throws Exception {
            map=ModernWorldMap.fromSectorBytes(514,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
            binding=new ModernWorldBinding(1,514,map);rooms=new ModernRooms(Set.of(binding.key()));
            for(int i=0;i<2;i++) {
                int source=i==0?7:42;var owner=presence.connect();var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture-"+i,i==0?firstClass:secondClass,0,80);
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,row.objectId()),"source-"+source,row.name(),row.level(),row.characterClass(),row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var p=new ModernBattleEngineBotTest.Player(owner,new ModernRooms.Scope(source,row.objectId(),1,0),row);players.add(p);
                var prior=i==0?null:view().summary();ModernRoomsTest.accepted(rooms.execute(owner,i==0
                        ?new ModernRooms.Command(p.scope(),ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(2,1,514,0),"")
                        :new ModernRooms.Command(p.scope(),ModernRooms.Action.JOIN,prior.id(),prior.revision(),0,null,"")));
            }
            for(var p:players){var s=view().summary();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}
            var owner=players.get(0);var s=view().summary();var captured=rooms.prepareWorld(owner.owner(),owner.scope(),s.id(),s.revision());
            ModernRoomsTest.accepted(captured.result());ticket=captured.ticket();var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<2;i++) {
                var p=players.get(i);var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.row(),i==0?firstHp:secondHp,100,12,0);
                var stocks=ModernShopTimedTest.catalog(p.row().characterClass()).offers().stream()
                        .sorted(java.util.Comparator.comparingLong(ModernShopCatalog.Offer::itemId)).map(o->new ModernShopAuthority.Stack(o,4)).toList();
                members.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.row(),ModernBattleEngineBotTest.cell(10+i,10),STEP,fighter,ATTACK,
                        new ModernSkills.Snapshot(p.scope().sourceId(),p.row(),ModernBattleEngineBotTest.learned()),CAST,true,stocks,
                        ModernInventoryWeight.qualify(fighter.profile(),100),true));
            }
            prepared=members.stream().map(configure).toList();engine=new ModernBattleEngine(ticket,binding,prepared,LIMITS,clock::get,()->{onEpoch.run();return epoch.get();},
                    bound->{draws.incrementAndGet();onRandom.run();if(jumpOnRandom!=0){clock.addAndGet(jumpOnRandom);jumpOnRandom=0;}return randomMode==1?-1:randomMode==2?bound:randomMode==3?0:bound-1;},settle);
        }
        ModernRooms.RoomSnapshot view(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        void elapsed(long nanos){clock.set(SECOND+nanos);}
        ModernBattleEngine.Snapshot ack(int i){var p=players.get(i);return engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());}
        ModernBattleEngine.Snapshot active(){ack(0);return ack(1);}
        ModernBattleEngine.ItemResult use(int i,long id){var p=players.get(i);return engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,ticket.matchId(),ticket.generation(),++itemSequences[i],id));}
        ModernBattleEngine.CastResult cast(int i,long seq,int skill){var p=players.get(i);return engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),seq,skill,skill==5?0:i==0?2:1));}
        ModernBattleEngine.AttackResult attack(int i,long seq){var p=players.get(i);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,i==0?2:1);}
        ModernBattleEngine.MoveResult move(int i,long seq,int heading){var p=players.get(i);var at=engine.tick().actors().get(i).position();return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,at.x(),at.y(),heading);}
        ModernItemProtocol.Stock stock(ModernBattleEngine.Snapshot s,int actor,long item){return s.items().get(actor).stocks().stream().filter(x->x.itemId()==item).findFirst().orElseThrow();}
        @Override public void close(){engine.close();rooms.releasePreparation(ticket);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}assertEquals(0,rooms.ownerCount());}
    }
}
