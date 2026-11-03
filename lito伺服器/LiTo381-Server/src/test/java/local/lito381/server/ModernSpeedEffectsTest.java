package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleSlowTest.*;
import static local.lito381.server.ModernBattleEngineTimedTest.SECOND;
import java.util.*;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ModernSpeedEffectsTest {
    static ModernBattlePublication publish(ModernBattleObservation.Capture c,ModernNpcCatalog.Catalog catalog) {
        var f=c.features();var m=c.modes();
        int flags=(f.combat()?1:0)|(f.spells()?2:0)|(f.arrow()?4:0)|(f.items()?8:0)|(f.weight()?16:0)|(f.timed()?32:0)|(f.ammo()?64:0)|(f.regen()?128:0)
                |(m.honor().enabled()?256:0)|(f.fire()?512:0)|(m.race()!=null?1024:0)|(c.npcs()!=null?2048:0)|(m.isolatedForces()?4096:0)
                |(m.quest().progress()!=null&&m.quest().progress().plan().objectives()!=null?8192:0)|(!m.quest().lives().isEmpty()?16384:0)
                |(f.npcArrow()?32768:0)|(m.timeAttack().enabled()?65536:0)|(m.siege().enabled()?131072:0)|(f.disintegrate()?262144:0)|(f.npcDisintegrate()?524288:0)|(f.npcFire()?1048576:0)
                |(f.haste()?ModernMatchDescriptor.HASTE:0)|(f.greaterHaste()?ModernMatchDescriptor.GREATER_HASTE:0)|(f.slow()?ModernMatchDescriptor.SLOW:0);
        var rule=m.honor().enabled()?ModernMatchDescriptor.Ruleset.HONOR_TOWERS:m.siege().enabled()?ModernMatchDescriptor.Ruleset.SIEGE_OBJECTIVES:
                m.timeAttack().enabled()?ModernMatchDescriptor.Ruleset.COOPERATIVE_TIME_ATTACK:c.npcs()!=null?ModernMatchDescriptor.Ruleset.NPC_ENCOUNTER:
                c.result()!=null?ModernMatchDescriptor.Ruleset.DEATHMATCH_ELIMINATION:ModernMatchDescriptor.Ruleset.PVP_SKIRMISH;
        var l=c.limits();var d=new ModernMatchDescriptor(rule,flags,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
        return new ModernBattlePublication(c,d,catalog==null?null:ModernNpcProtocol.Resources.from(catalog),catalog==null?null:ModernNpcNames.Table.from(catalog));
    }
    static ModernBattleObservation.World with(ModernBattleObservation.World w,ModernSpeedEffects.State state,List<ModernBattleObservation.Actor> actors) {
        return new ModernBattleObservation.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.features(),w.clock(),actors,w.modes(),w.npcs(),w.result(),null,state);
    }
    static ModernBattleObservation.World with(ModernBattleObservation.World w,ModernSpeedEffects.State state){return with(w,state,w.actors());}
    @Test void completePublicationKeepsBothSourcesSeparateAndOldTransportsCannotDropMaterialsOrSpeed()throws Exception {
        try(var f=dualSpeedFixture()) {
            active(f);f.randomMode=3;var c=cast(f,0,1,1).observation();var p=publish(c,null);
            var a=p.audience(f.players.get(0).scope());var b=p.audience(f.players.get(1).scope());
            assertSame(a.world(),b.world());assertNotEquals(a.personal().materials().scope(),b.personal().materials().scope());
            assertEquals(1,a.personal().materials().uses());assertEquals(0,b.personal().materials().uses());
            assertEquals(c.actors().get(0).materials(),a.personal().materials());assertNotNull(a.world().speedEffects());assertNull(a.world().hasteEffects());
            for(int version=1;version<ModernBattleSpeedWire.VERSION;version++)assertFalse(p.descriptor().supportedBy(version,ModernSessionProtocol.KNOWN_CAPABILITIES));
            for(int schema=1;schema<=6;schema++){int n=schema;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(a,n));}
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeHaste(a));
            for(int version=35;version<=45;version++){int n=version;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(a,n));}
            var bytes=ModernBattleWire.encodeSpeed(a);assertArrayEquals(bytes,ModernBattleWire.encodeSpeed(a));
            assertEquals(6,ByteBuffer.wrap(bytes).getShort(4));assertEquals(14,ByteBuffer.wrap(bytes).getShort(12));
            var history=a.personal().history();assertEquals(history,ModernEquipmentHistoryWire.decode(ModernEquipmentHistoryWire.encode(history,4),4));
            for(int schema=1;schema<=3;schema++){int n=schema;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(history,n));}
            assertThrows(SecurityException.class,()->p.audience(new ModernRooms.Scope(7,3333,999,0)));
        }
    }
    @Test void oneNpcCanCarryOldSlowWalkAndNewHasteFromTheOtherLaneWithoutRewritingEither()throws Exception {
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0)) {
            ModernBattleNpcHasteTest.active(f);var npc=ModernBattleNpcHasteTest.spawn(f,0,14,10);
            ModernBattleNpcSlowTest.cast(f,0,1,npc);f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(npc.id(),1,4096,false));
            var walked=publish(f.engine.serviceObservation(),f.config.catalog()).world().speedEffects();var old=walked.targets().get(0).walk();
            assertNotNull(old);assertEquals(ModernUseHistory.Lane.SLOW,old.lane());
            f.elapsed(2000);ModernBattleNpcHasteTest.cast(f,0,2,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation()));
            f.elapsed(4000);ModernBattleNpcHasteTest.cast(f,0,3,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation()));
            var p=publish(f.engine.serviceObservation(),f.config.catalog());var s=p.world().speedEffects();var entry=s.targets().get(0);
            assertEquals(old,entry.walk());assertEquals(ModernUseHistory.Lane.HASTE,entry.active().lane());
            assertEquals(75,s.npcs().actors().get(0).currentPace().walkMillis());assertEquals(200,s.npcs().actors().get(0).walk().durationMillis());
            var noWalk=new ModernSpeedEffects.State(s.world(),s.revision(),List.of(new ModernSpeedEffects.Entry(entry.target(),entry.active(),null)),s.npcs());
            assertThrows(IllegalArgumentException.class,()->with(p.world(),noWalk));
            assertDoesNotThrow(()->ModernBattleWire.encodeSpeed(p.audience(f.players.get(0).scope())));
        }
    }
    @Test void latestMissKeepsEarlierGrantButCannotBeSubstitutedForTheActiveProof()throws Exception {
        try(var f=fixture()) {
            active(f);f.randomMode=3;cast(f,0,1,1);f.elapsed(2*SECOND);f.randomMode=0;
            var p=publish(cast(f,0,2,1).observation(),null);var w=p.world();var s=w.speedEffects();assertEquals(1,s.targets().size());
            var a=w.actors().get(0);var r=a.spells().receipts().get(ModernUseHistory.Lane.SLOW);
            assertEquals(ModernSlowReceipt.Outcome.MISS,((ModernSlowReceipt)r.detail()).outcome());
            var fake=new ModernSpeedEffects.Grant(new ModernTargetHistory.Participant(a.pose().entityId(),a.pose().member().identity()),r);
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernSpeedEffects.State(s.world(),s.revision(),
                    List.of(new ModernSpeedEffects.Entry(s.targets().get(0).target(),fake,null)),null)));
            assertEquals(1,s.targets().get(0).active().receipt().casts());assertEquals(2,p.audience(f.players.get(0).scope()).personal().materials().uses());
        }
    }
    @Test void missingForeignClockAndReorderedProofCannotConstructACompleteWorld()throws Exception {
        try(var f=fixture()) {
            active(f);f.randomMode=3;cast(f,0,1,1);f.elapsed(2*SECOND);var w=publish(cast(f,0,2,0).observation(),null).world();var s=w.speedEffects();
            assertEquals(2,s.targets().size());assertThrows(IllegalArgumentException.class,()->with(w,null));
            assertThrows(IllegalArgumentException.class,()->with(w,new ModernSpeedEffects.State(s.world(),s.revision(),List.of(),null)));
            assertThrows(IllegalArgumentException.class,()->new ModernSpeedEffects.State(s.world(),s.revision(),List.of(s.targets().get(1),s.targets().get(0)),null));
            var e=s.targets().get(0);var g=e.active();var r=g.receipt();
            for(var wrong:List.of(new ModernSpeedEffects.Grant(new ModernTargetHistory.Participant(1,ModernParticipantId.human(42,3333)),r),
                    new ModernSpeedEffects.Grant(g.caster(),new ModernBattleObservation.SpellReceipt(r.impact(),r.casts(),r.spent(),r.loadingElapsedNanos()+1,r.detail())))) {
                var targets=new ArrayList<>(s.targets());targets.set(0,new ModernSpeedEffects.Entry(e.target(),wrong,null));
                assertThrows(IllegalArgumentException.class,()->with(w,new ModernSpeedEffects.State(s.world(),s.revision(),targets,null)));
            }
        }
    }
    @Test void retainedGrantCannotInventThreeCastsInsideOneNewRequest()throws Exception {
        try(var f=fixture(6,1000,1000,false,15)) {
            active(f);f.randomMode=3;
            for(int seq=1;seq<=3;seq++){f.elapsed((seq-1)*2_000_000_000L);assertEquals(ModernSlowCast.Reason.NONE,cast(f,0,seq,1).reason());}
            f.elapsed(6_000_000_000L);f.randomMode=0;
            var w=publish(cast(f,0,4,1).observation(),null).world();var s=w.speedEffects();
            var entry=s.targets().get(0);var g=entry.active();var r=g.receipt();
            assertEquals(3,r.casts());assertEquals(3,r.impact().use().sequence());
            var rewritten=new ModernSpeedEffects.Grant(g.caster(),new ModernBattleObservation.SpellReceipt(
                r.impact(),1,r.impact().use().cost(),r.loadingElapsedNanos(),r.detail()));
            var bad=new ModernSpeedEffects.State(s.world(),s.revision(),
                List.of(new ModernSpeedEffects.Entry(entry.target(),rewritten,entry.walk())),s.npcs());
            assertThrows(IllegalArgumentException.class,()->with(w,bad));
            assertEquals(17,r.impact().use().cost());
            var repriced=new ModernSpeedEffects.Grant(g.caster(),new ModernBattleObservation.SpellReceipt(
                r.impact(),r.casts(),r.spent()+1,r.loadingElapsedNanos(),r.detail()));
            var wrongCost=new ModernSpeedEffects.State(s.world(),s.revision(),
                List.of(new ModernSpeedEffects.Entry(entry.target(),repriced,entry.walk())),s.npcs());
            assertThrows(IllegalArgumentException.class,()->with(w,wrongCost));
        }
    }
    @Test void paidNativeFailureCarriesItsProofWithoutInventingAnActiveGrant()throws Exception {
        try(var f=ModernBattleNpcSlowTest.fixture(false,2,0,ModernSlowNativeRulesTest.catalog(45912))) {
            ModernBattleNpcHasteTest.active(f);var n=ModernSlowNativeRulesTest.spawn(f,45912,11);
            var p=publish(ModernBattleNpcSlowTest.cast(f,0,1,n).observation(),f.config.catalog());assertTrue(p.world().speedEffects().targets().isEmpty());
            var d=(ModernSlowReceipt)p.world().actors().get(0).spells().receipts().get(ModernUseHistory.Lane.SLOW).detail();
            assertEquals(ModernSlowReceipt.Outcome.NATIVE_IMMUNE,d.outcome());assertNotNull(d.missingEffect());
            assertDoesNotThrow(()->ModernBattleWire.encodeSpeed(p.audience(f.players.get(0).scope())));
            var terminal=publish(f.engine.abortObservation(),f.config.catalog());assertTrue(terminal.world().speedEffects().npcs().actors().isEmpty());
            assertEquals(1,terminal.audience(f.players.get(0).scope()).personal().materials().uses());
        }
    }
}
