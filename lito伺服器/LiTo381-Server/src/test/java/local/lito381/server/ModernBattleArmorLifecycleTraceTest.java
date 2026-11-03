package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineArmorProtectionTest.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual engine + durable equipment transitions with an explicit gfx0 catalog.
 * Uses synthetic terrain/source data; never changes the ordinary11101 fixture. */
class ModernBattleArmorLifecycleTraceTest {
    @TempDir Path root;

    static ModernPhysicalCombat.Source graphics(ModernPhysicalCombat.Source s) {
        var rules=new HashMap<Long,List<ModernArmorProcs.Rule>>();
        for(var item:s.input().equipment().source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.ARMOR)
            rules.put(item.template().catalogId(),List.of(new ModernArmorProcs.Rule(item.template().catalogId(),
                    item.template().catalogId(),-1,88,0,100,0,0,0,0,0,0,0,68,3,1,0,0)));
        return ModernArmorProtectionLoadoutTest.withRules(s,Set.of(),rules);
    }
    static ModernBattleEngine.PreparedMember member(ModernBattleEngine.PreparedMember p) {
        var v=p.fighter().profile().source().persisted();
        var fighter=ModernArmorProtectionLoadoutTest.qualify(graphics(ModernBattleEngineHonorTest.source(
                p.scope().sourceId(),p.character(),v.maxHp(),v.maxMp(),0)));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,
                p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),
                ModernInventoryWeight.qualify(fighter.profile(),p.weightBasis().sourceRate()),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),
                p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate(),p.greaterHaste(),p.haste(),p.slow(),p.playerHealing());
    }
    static void emit(ModernArmorSelectedProtocolTest.Sink sink,String label,ModernBattleObservation.Capture c,
            List<ModernBattleEngineBotTest.Player> players)throws Exception {
        ModernBattleArmorWireTest.send((name,v)->{
            ModernBattleArmorPresentationWireTest.check(v);
            var peer=ModernArmorSelectedProtocolTest.handshake(15).selected();
            var q=new ModernBattleReadProtocol.Request(v.personal().scope(),v.world().identity());
            var reply=new ModernApplicationProtocol.BattleReadResult(2,q,
                    new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v));
            sink.send(name,15,ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadRequest(2,q),peer),
                    ModernApplicationProtocol.encode(reply,peer));
        },label,c,players);
    }
    static void trace(Path root,ModernArmorSelectedProtocolTest.Sink sink)throws Exception {
        try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                ModernBattleEngine.PreparedMember::withPlayerHealing,s->graphics(ModernBattleArmorWireTest.source(s)),true)) {
            var gear=f.read();emit(sink,"gear-loading",f.capture(),f.world.players);
            ModernArmorProtectionEquipmentTest.active(f);emit(sink,"gear-active",f.capture(),f.world.players);
            ModernArmorProtectionEquipmentTest.hit(f,1);var initial=f.capture();emit(sink,"gear-hit",initial,f.world.players);
            var original=ModernArmorPresentationTest.own(initial,0);
            var off=f.set(gear,false);assertEquals(ModernEquipmentAuthority.OK,off.equipment().status());
            assertEquals(original.batch(),ModernArmorPresentationTest.own(off.observation(),0).batch());
            emit(sink,"gear-off",off.observation(),f.world.players);
            var heal=ModernBattlePlayerHealingTest.heal(f.world,0,1,0);assertEquals(ModernPlayerHealingCast.Reason.NONE,heal.reason());
            assertEquals(original.batch(),ModernArmorPresentationTest.own(heal.observation(),0).batch());
            emit(sink,"gear-heal",heal.observation(),f.world.players);
            f.world.elapsed(SECOND);ModernArmorProtectionEquipmentTest.hit(f,2);
            assertNull(ModernArmorPresentationTest.own(f.capture(),0).batch());
            assertEquals(3*SECOND,ModernArmorProtectionEquipmentTest.state(f).state().untilNanos());
            emit(sink,"gear-offhit",f.capture(),f.world.players);
            var on=f.set(off.equipment().snapshot(),true);assertEquals(ModernEquipmentAuthority.OK,on.equipment().status());
            assertEquals(3*SECOND,ModernArmorProtectionEquipmentTest.state(f).state().untilNanos());
            emit(sink,"gear-on",on.observation(),f.world.players);
            f.world.elapsed(3*SECOND);emit(sink,"gear-expired",f.capture(),f.world.players);
            ModernArmorProtectionEquipmentTest.hit(f,3);assertEquals(6*SECOND,ModernArmorProtectionEquipmentTest.state(f).state().untilNanos());
            emit(sink,"gear-again",f.capture(),f.world.players);
            assertEquals(2,f.stored().orElseThrow().revision());
            emit(sink,"gear-abort",f.world.engine.abortObservation(),f.world.players);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1,100,1000,SECOND,0,false,true,
                ModernBattleArmorLifecycleTraceTest::member)) {
            emit(sink,"life-loading",f.engine.serviceObservation(),f.players);active(f);
            emit(sink,"life-active",f.engine.serviceObservation(),f.players);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
            assertTrue(hp(f.engine.serviceObservation().world())>0);
            assertNotNull(ModernArmorPresentationTest.own(f.engine.serviceObservation(),1).batch());
            emit(sink,"life-hit",f.engine.serviceObservation(),f.players);
            long cast=0;
            while(hp(f.engine.serviceObservation().world())>0&&cast<20) {
                cast++;f.elapsed(cast*2*SECOND);var hit=f.cast(0,cast,4,2);
                assertEquals(ModernSpellProtocol.Reason.NONE,hit.reason());
                emit(sink,"life-damage"+cast,hit.observation(),f.players);
            }
            var dead=f.engine.serviceObservation();assertEquals(0,hp(dead.world()));
            assertEquals(ModernBattleEngine.Phase.ACTIVE,dead.phase());
            assertNull(ModernArmorPresentationTest.own(dead,1).batch());
            emit(sink,"life-dead",dead,f.players);
            long revivedAt=(cast*2+5)*SECOND;f.elapsed(revivedAt);
            var revived=f.engine.serviceObservation();assertEquals(100,hp(revived.world()));
            assertEquals(1,ModernArmorPresentationTest.own(revived,1).life());
            assertNull(ModernArmorPresentationTest.own(revived,1).batch());
            emit(sink,"life-revived",revived,f.players);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,2,2).status());
            assertNotNull(ModernArmorPresentationTest.own(f.engine.serviceObservation(),1).batch());
            emit(sink,"life-again",f.engine.serviceObservation(),f.players);
            f.elapsed(revivedAt+2*SECOND);var finish=f.cast(0,cast+1,4,4);
            assertEquals(ModernSpellProtocol.Reason.NONE,finish.reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,finish.observation().phase());
            assertNull(ModernArmorPresentationTest.own(finish.observation(),1).batch());
            emit(sink,"life-finished",finish.observation(),f.players);
        }
    }
    @Test void durableGearAndHonorDeathRespawnFinishAreActualEngineTransitions()throws Exception {
        var labels=new HashSet<String>();trace(root,(label,ext,q,r)->assertTrue(labels.add(label)));
        assertTrue(labels.containsAll(List.of("gear-heal-0","gear-again-0","life-dead-1","life-revived-1","life-finished-1")));
        System.out.println("ARMOR_LIFECYCLE_FRAMES="+labels.size());
    }
}
