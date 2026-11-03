package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Initially run against the engine's independent stored <=34 DTOs before
 * removing them. Thereafter the existing packet/mode tests guard this boundary. */
class ModernLegacySpellProjectionTest {
    static Object field(Object actor,String name)throws Exception {
        var f=actor.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(actor);
    }
    @SuppressWarnings("unchecked")
    static void matches(ModernBattleEngine engine,ModernBattleEngine.Snapshot s)throws Exception {
        for(int i=0;i<s.actors().size();i++) {
            var raw=ModernBattleVitalBalanceTest.actor(engine,i);var actor=s.actors().get(i);
            var runtime=(ModernSpellRuntime.State)field(raw,"spellRuntime");
            var history=(Map<ModernUseHistory.Lane,ModernUseHistory.Spell>)field(raw,"spellHistory");
            int interval=s.casts().isEmpty()?0:s.casts().get(i).castIntervalMillis();
            var p=ModernLegacySpellProjection.from(actor.entityId(),actor.member().team(),interval,runtime,history);
            assertEquals(at(s.casts(),i),p.teleport());assertEquals(at(s.damages(),i),p.arrow());
            assertEquals(at(s.honor().heals(),i),p.healing());assertEquals(at(s.fireStorms(),i),p.fireStorm());
            assertEquals(at(s.disintegrations(),i),p.disintegrate());assertEquals(at(s.npcSpells(),i),p.npcArrow());
            assertEquals(at(s.npcFireStorms(),i),p.npcFireStorm());assertEquals(at(s.npcDisintegrations(),i),p.npcDisintegrate());
        }
    }
    static Object at(List<?> values,int index){return values.isEmpty()?null:values.get(index);}
    @Test void playerHonorFiveLanesIncludeDeniedSequenceAndHistoricalVictimLoss()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,10000,1000,500,false)) {
            f.active();matches(f.engine,f.engine.tick());
            matches(f.engine,ModernBattleEngineDisintegrateTest.cast(f,1,2).snapshot());
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,4,2).reason());matches(f.engine,f.engine.tick());
            int[] skills={4,70,1,4,5};long[] targets={2,0,3,4,0};
            for(int i=0;i<skills.length;i++) {
                f.elapsed((i+1)*2_000_000_000L);var reply=f.cast(0,i+3,skills[i],targets[i]);
                assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());matches(f.engine,reply.snapshot());
            }
        }
    }
    @Test void bothCriticalVariantsAndBotEmptyLanesAreRepresentedExactly()throws Exception {
        for(boolean critical:new boolean[]{false,true}) {
            try(var f=ModernBattleEngineDisintegrateTest.fixture(false,2,10000,1000,500,false)) {
                f.active();matches(f.engine,f.engine.tick());f.randomOverride=b->b==100?(critical?0:99):0;
                matches(f.engine,ModernBattleEngineDisintegrateTest.cast(f,1,2).snapshot());
                f.elapsed(2_000_000_000L);matches(f.engine,f.cast(0,2,70,0).snapshot());
            }
            try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
                f.active();matches(f.engine,f.view().world());var n=f.spawn(0,11,10);f.randomValue=critical?0:99;
                matches(f.engine,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,n).frame().world());
            }
        }
    }
    @Test void allNpcSpellLanesAndArrowCriticalAreRetained()throws Exception {
        for(boolean critical:new boolean[]{false,true})try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();matches(f.engine,f.view().world());var n=f.spawn(0,11,10);f.randomValue=critical?0:99;
            matches(f.engine,ModernBattleEngineNpcMagicTest.cast(f,0,1,n).frame().world());
            f.elapsed(1200);f.randomValue=0;matches(f.engine,ModernBattleEngineNpcFireStormTest.cast(f,2).frame().world());
            f.elapsed(2400);matches(f.engine,ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n).frame().world());
        }
    }
    @Test void questStructureRetainsOriginalCountKindAndLifeAfterRevival()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1,1,false)) {
            f.active();matches(f.engine,ModernNpcFireStormStructuresTest.cast(f,0,1).frame().world());
            f.elapsed(2000);matches(f.engine,ModernNpcFireStormStructuresTest.cast(f,0,2).frame().world());
            // A post-command world read advances the newly unlocked wave;
            // only then can its NPC attack after the configured 100ms birth delay.
            matches(f.engine,f.view().world());
            f.randomValue=19;var dead=ModernQuestLifeTest.die(f,2100);matches(f.engine,dead.world());
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            matches(f.engine,f.view().world());f.elapsed(4000);f.randomValue=0;
            matches(f.engine,ModernNpcFireStormStructuresTest.cast(f,0,3).frame().world());
        }
    }
    @Test void towerRetainsProofAcrossAllWavesAndFinalSettlement()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.time(1,1)) {
            f.active();matches(f.engine,ModernNpcFireStormStructuresTest.cast(f,0,1).frame().world());
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed((wave+1)*2000L);matches(f.engine,ModernNpcFireStormStructuresTest.cast(f,0,wave+2).frame().world());
            }
        }
    }
    @Test void repricingRetainsRealSpendButCannotBeHiddenInsideAnOldFixedPricePacket()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.cast(0,1,1,3);var original=ModernBattleSpellRuntimeTest.read(f.engine,f.ticket,f.players.get(0));
            var h=original.history();var runtime=original.spells();var lane=ModernUseHistory.Lane.HEALING;var profile=h.profile();
            var costs=new java.util.EnumMap<>(profile.costs());costs.put(lane,2);
            var repriced=h.reprofile(h.worldRevision()+1,new ModernEquipmentHistory.Profile(profile.equipmentRevision()+1,
                    profile.maximumHp(),profile.maximumMp(),costs,profile.fixedRecovery()));
            runtime.validate(repriced);
            assertThrows(IllegalArgumentException.class,()->ModernLegacySpellProjection.from(1,0,1000,runtime,repriced.spells()));
            var receipt=runtime.receipts().get(lane);var anchor=receipt.batch().use().before();
            var recast=repriced.cast(lane,repriced.worldRevision()+1,2,new ModernWorldMap.Cell(anchor.x(),anchor.y()),receipt.batch().targets());
            var next=runtime.cast(repriced,recast,lane,receipt.atNanos()+2_000_000_000L,ModernSpellRuntime.Ordinary.INSTANCE);
            assertEquals(6,next.receipts().get(lane).spent());assertEquals(2,next.receipts().get(lane).casts());
            assertThrows(IllegalArgumentException.class,()->ModernLegacySpellProjection.from(1,0,1000,next,recast.spells()));
            // Even a new current price whose count*price accidentally equals
            // total historical spending must not erase the epoch boundary.
            costs.put(lane,3);
            var coincidental=recast.reprofile(recast.worldRevision()+1,new ModernEquipmentHistory.Profile(profile.equipmentRevision()+2,
                    profile.maximumHp(),profile.maximumMp(),costs,profile.fixedRecovery()));
            next.validate(coincidental);assertEquals(6,coincidental.spells().get(lane).current().casts()*3);
            assertThrows(IllegalArgumentException.class,()->ModernLegacySpellProjection.from(1,0,1000,next,coincidental.spells()));
            assertEquals(original,ModernBattleSpellRuntimeTest.read(f.engine,f.ticket,f.players.get(0)),"pure mixed-price candidate is not live SET authority");
        }
    }
    @Test void actorNoLongerStoresAnyLegacySpellDtoOrDuplicateTeleportLedger()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var actor=ModernBattleVitalBalanceTest.actor(f.engine,0);
            var types=java.util.Set.of(ModernSpellProtocol.CastState.class,ModernDamageSpellProtocol.State.class,ModernHonor.Heal.class,
                    ModernFireStormProtocol.State.class,ModernDisintegrateState.State.class,ModernNpcMagic.CastState.class,
                    ModernNpcFireStormState.State.class,ModernNpcDisintegrateState.State.class);
            for(var field:actor.getClass().getDeclaredFields())assertFalse(types.contains(field.getType()),field.getName());
            for(var name:List.of("castCount","castRevision","mpSpent","fromX","fromY","toX","toY"))
                assertThrows(NoSuchFieldException.class,()->actor.getClass().getDeclaredField(name));
        }
    }
}
