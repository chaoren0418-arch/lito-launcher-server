package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Engine execution after genuinely qualified equipment changes. Installation
 * is fixture-only reflection: live advanced SET remains deliberately gated. */
class ModernBattleItemRuntimeTest {
    static final class Fixture implements AutoCloseable {
        final Map<Integer,ModernPhysicalCombat.Source> sources=new HashMap<>();
        final ModernBattleEngineHonorTest.Fixture game;
        long attackSequence;
        Fixture()throws Exception {
            game=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true,p->{
                if(p.scope().sourceId()!=7)return p;
                var source=ModernBattleEquipmentHistoryTest.source(p);sources.put(p.scope().sourceId(),source);
                var fighter=ModernPhysicalCombatTest.fighter(source);
                return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                        p.skills(),p.castIntervalNanos(),true,p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true,false,true,true);
            });
        }
        ModernBattleObservation.OwnerView view(){return game.engine.serviceObservation().forOwner(game.players.get(0).scope());}
        ModernBattleEngine.ItemResult use(long sequence,long item) {
            var p=game.players.get(0);return game.engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,
                    game.ticket.matchId(),game.ticket.generation(),sequence,item));
        }
        void injure(long deficit) {
            for(int n=0;n<64;n++) {
                var hp=view().world().actors().get(0).vital().values();assertTrue(hp.hp()>0,"test attacker must not kill potion user: "+hp);
                if(hp.maximumHp()-hp.hp()>=deficit)return;
                game.clock.addAndGet(1_000_000_000L);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,game.attack(1,++attackSequence,1).status());
            }
            fail("test attacker did not create the required healing deficit");
        }
        void equipped(boolean equipped)throws Exception {
            var p=game.players.get(0);assertThrows(IllegalStateException.class,()->game.engine.beginEquipmentChange(p.owner(),p.scope(),game.ticket.matchId(),game.ticket.generation()));
            var capture=game.engine.serviceObservation();var old=capture.actors().get(0);var history=capture.forOwner(p.scope()).owner().history().history();
            var source=sources.get(p.scope().sourceId());var q=source.input().equipment();
            var next=ModernPhysicalCombat.qualifyEquipmentChange(source,ModernCombatProfileTest.binding(q),
                    ModernCombatProfileTest.stored(q,history.profile().equipmentRevision()+1,Map.of(1L,equipped)),1000,
                    old.loadout().fighter(),history.resources().vitals().hp(),history.resources().vitals().mp());
            var loadout=old.loadout().replacement(next);var revised=history.reprofile(capture.revision()+1,loadout.historyProfile(history.spells().keySet(),true));
            assertTrue(ModernEquipmentHistory.followsReprofile(history,revised));
            synchronized(game.engine) {
                var actor=ModernBattleVitalBalanceTest.actor(game.engine,0);
                ModernBattleCommandTest.set(actor,"loadout",loadout);ModernBattleCommandTest.set(actor,"equipmentResources",revised.resources());
                ModernBattleCommandTest.set(actor,"spellHistory",revised.spells());ModernBattleCommandTest.set(actor,"targetHistory",revised.targets());ModernBattleCommandTest.set(actor,"itemHistory",revised.items());
                ModernBattleCommandTest.set(actor,"hp",revised.resources().vitals().hp());ModernBattleCommandTest.set(actor,"mp",revised.resources().vitals().mp());
                ModernBattleCommandTest.set(actor,"weight",loadout.reweight(old.supplies().stocks(),List.of(),old.weight().units()));
                ModernBattleCommandTest.set(game.engine,"revision",revised.worldRevision());
            }
        }
        @Override public void close(){game.close();}
    }
    @Test void removingQualifiedRecoveryGearThenTakingRealDamageUsesNewPotionBonus()throws Exception {
        try(var f=new Fixture()) {
            f.game.active();assertEquals(8,f.view().owner().supplies().history().fixedRecovery());f.equipped(false);
            f.injure(40);
            var before=f.view();var hp=before.world().actors().get(0).vital().values();assertTrue(hp.hp()>0,"injured potion user: "+hp);assertTrue(hp.maximumHp()-hp.hp()>28,"healing deficit: "+hp);
            assertEquals(0,before.owner().supplies().history().fixedRecovery());
            var used=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var after=used.observation().forOwner(f.game.players.get(0).scope());var receipt=after.owner().supplies().history().last();
            assertEquals(0,receipt.fixedRecovery());assertEquals(20,receipt.rolledBase());assertEquals(20,receipt.restored());
            assertEquals(hp.hp()+20,after.world().actors().get(0).vital().values().hp());
            assertEquals(before.owner().supplies().stocks().get(0).remaining()-1,after.owner().supplies().stocks().get(0).remaining());
        }
    }
    @Test void priorPotionAboveNewCapacitySurvivesDenialTimedUseAndAnotherPotion()throws Exception {
        try(var f=new Fixture()) {
            f.game.active();f.injure(40);var first=f.use(1,40010);assertEquals(ModernItemProtocol.Reason.NONE,first.reason());
            var old=f.view().owner().supplies().history();assertEquals(28,old.restored());assertTrue(old.last().hpAfter()>100);
            f.equipped(false);var changed=f.view();var oldStocks=changed.owner().supplies().stocks();
            assertEquals(old.last(),changed.owner().supplies().history().last());assertEquals(0,changed.owner().supplies().history().fixedRecovery());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(f.game.engine.serviceObservation()));
            var denied=f.use(2,99999);assertEquals(ModernItemProtocol.Reason.UNSUPPORTED,denied.reason());
            var afterDenied=f.view();assertEquals(oldStocks,afterDenied.owner().supplies().stocks());assertEquals(changed.owner().supplies().history(),afterDenied.owner().supplies().history());
            assertEquals(afterDenied.world().revision(),afterDenied.owner().supplies().requestRevision());
            var timed=f.use(3,40013);assertEquals(ModernItemProtocol.Reason.NONE,timed.reason());
            var afterTimed=f.view();assertEquals(28,afterTimed.owner().supplies().history().restored());assertEquals(0,afterTimed.owner().supplies().history().last().fixedRecovery());
            assertEquals(191,afterTimed.owner().supplies().effect());assertEquals(0,afterTimed.owner().supplies().delayMillis());
            assertTrue(afterTimed.world().actors().get(0).buffs().holds(ModernTimedEffects.Kind.HASTE));
            f.injure(40);var used=f.use(4,40010);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var after=f.view();assertEquals(48,after.owner().supplies().history().restored());assertEquals(3,after.owner().supplies().history().uses());
            assertEquals(0,after.owner().supplies().history().last().fixedRecovery());assertEquals(20,after.owner().supplies().history().last().restored());
            var capture=used.observation();var d=ModernBattlePublicationTest.honor(capture);
            d=new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.FIRE_STORM,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            var audience=new ModernBattlePublication(capture,d,null,null).audience(f.game.players.get(0).scope());
            assertEquals(after.owner().supplies(),audience.personal().supplies());assertEquals(after.owner().history().history(),audience.personal().history());
            assertEquals(28,old.restored());assertEquals(8,old.last().fixedRecovery());
        }
    }
    @Test void equippingRecoveryGearAgainDoesNotRestoreHpOrResetPotionCooldown()throws Exception {
        try(var f=new Fixture()) {
            f.game.active();f.equipped(false);f.injure(40);assertEquals(ModernItemProtocol.Reason.NONE,f.use(1,40010).reason());
            var old=f.view();f.equipped(true);var equipped=f.view();assertEquals(old.world().actors().get(0).vital().values().hp(),equipped.world().actors().get(0).vital().values().hp());
            assertEquals(190,equipped.world().actors().get(0).vital().values().maximumHp());assertEquals(8,equipped.owner().supplies().history().fixedRecovery());
            assertEquals(ModernItemProtocol.Reason.TOO_FAST,f.use(2,40010).reason());assertEquals(equipped.owner().supplies().stocks(),f.view().owner().supplies().stocks());
            f.game.clock.addAndGet(1_000_000_000L);var used=f.use(3,40010);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var after=f.view();assertEquals(28,after.owner().supplies().history().last().restored());assertEquals(48,after.owner().supplies().history().restored());
            assertEquals(8,after.owner().supplies().history().last().fixedRecovery());assertEquals(0,old.owner().supplies().history().last().fixedRecovery());
            assertThrows(SecurityException.class,()->f.use(3,40010));assertEquals(after,f.view());
            assertThrows(SecurityException.class,()->f.use(5,40010));assertEquals(after,f.view());
        }
    }
    @Test void repricedPotionNumericCancellationAndTimeoutCannotPartiallyCommit()throws Exception {
        for(int mode=0;mode<3;mode++)try(var f=new Fixture()) {
            f.game.active();f.equipped(false);f.injure(40);var before=f.view();
            if(mode==0)f.game.invalid=true;else if(mode==1)f.game.cancel=true;else f.game.jump=1_800_000_000_000L;
            var result=f.use(1,40010);assertEquals(mode==0?ModernItemProtocol.Reason.NUMERIC:ModernItemProtocol.Reason.NOT_ACTIVE,result.reason());
            var after=f.view();assertEquals(before.owner().supplies().history(),after.owner().supplies().history());assertEquals(before.owner().supplies().stocks(),after.owner().supplies().stocks());
            assertEquals(before.owner().weight(),after.owner().weight());assertEquals(before.world().actors().get(0).vital().values(),after.world().actors().get(0).vital().values());
            assertEquals(mode==0?1:0,after.owner().supplies().requestSequence());
            if(mode==0){f.game.invalid=false;assertEquals(ModernItemProtocol.Reason.NONE,f.use(2,40010).reason());}
        }
    }
    @Test void futureItemAckCannotBeJoinedToAnOlderOwnerWorld()throws Exception {
        try(var f=new Fixture()) {
            f.game.active();var old=f.view();var o=old.owner();var s=o.supplies();
            var future=new ModernBattleObservation.Supplies(1,old.world().revision()+1,s.history(),s.stocks(),s.effect(),s.delayMillis());
            assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.OwnerView(old.world(),new ModernBattleObservation.PrivateOwner(
                    o.scope(),o.entityId(),o.loadout(),o.skills(),o.history(),future,o.weight(),o.ammo(),o.regenClock())));
        }
    }
    @Test void exhaustedRevisionRetiresWithoutConsumingTheNextPotion()throws Exception {
        try(var f=new Fixture()) {
            f.game.active();var before=f.view().owner().supplies().stocks();
            ModernBattleCommandTest.set(f.game.engine,"revision",Long.MAX_VALUE);
            assertThrows(IllegalStateException.class,()->f.use(1,40010));assertTrue(f.game.engine.isClosed());
            var actor=ModernBattleVitalBalanceTest.actor(f.game.engine,0);var state=(ModernItemRuntime.State)ModernBattlePublicationTest.field(actor,"itemUse");
            assertEquals(before,state.stocks());assertEquals(0,state.requestSequence());
        }
    }
}
