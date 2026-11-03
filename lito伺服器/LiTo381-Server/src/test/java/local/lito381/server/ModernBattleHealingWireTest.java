package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import static local.lito381.server.ModernBattlePlayerHealingTest.*;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Complete candidate only; no HELLO, network request or UI enablement. */
class ModernBattleHealingWireTest {
    static ModernBattlePublication publication(ModernBattleObservation.Capture c,ModernNpcCatalog.Catalog catalog) {
        var f=c.features();var m=c.modes();
        int flags=(f.combat()?COMBAT:0)|(f.spells()?SPELLS:0)|(f.arrow()?LIGHT_ARROW:0)|(f.items()?ITEMS:0)|(f.weight()?WEIGHT:0)
                |(f.timed()?TIMED:0)|(f.ammo()?AMMO:0)|(f.regen()?REGEN:0)|(f.fire()?FIRE_STORM:0)|(f.disintegrate()?DISINTEGRATE:0)
                |(f.greaterHaste()?GREATER_HASTE:0)|(f.haste()?HASTE:0)|(f.slow()?SLOW:0)|PLAYER_HEALING
                |(m.honor().enabled()?HONOR:0)|(c.npcs()!=null?NPC:0)|(m.isolatedForces()?ISOLATED_FORCES:0)
                |(m.quest().progress()!=null&&m.quest().progress().plan().objectives()!=null?QUEST_OBJECTIVES:0)|(!m.quest().lives().isEmpty()?QUEST_LIVES:0)
                |(f.npcArrow()?NPC_LIGHT_ARROW:0)|(f.npcDisintegrate()?NPC_DISINTEGRATE:0)|(f.npcFire()?NPC_FIRE_STORM:0)|(m.timeAttack().enabled()?TIME_ATTACK:0)|(m.siege().enabled()?SIEGE:0);
        var rule=m.honor().enabled()?Ruleset.HONOR_TOWERS:m.siege().enabled()?Ruleset.SIEGE_OBJECTIVES:m.timeAttack().enabled()?Ruleset.COOPERATIVE_TIME_ATTACK:
                c.npcs()!=null?Ruleset.NPC_ENCOUNTER:c.result()!=null?Ruleset.DEATHMATCH_ELIMINATION:Ruleset.PVP_SKIRMISH;
        var l=c.limits();var d=new ModernMatchDescriptor(rule,flags,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
        return ModernBattlePublication.playerHealing(c,d,catalog==null?null:ModernNpcProtocol.Resources.from(catalog),catalog==null?null:ModernNpcNames.Table.from(catalog));
    }
    static Map<Integer,byte[]> sections(byte[] raw) {
        assertTrue(raw.length<=ModernBattleHealingWire.MAX_BYTES);var b=ByteBuffer.wrap(raw);
        assertEquals(ModernBattleWire.MAGIC,b.getInt());assertEquals(7,b.getShort());assertEquals(0,b.getShort());assertEquals(raw.length,b.getInt());assertEquals(15,b.getShort());assertEquals(0,b.getShort());
        var out=new LinkedHashMap<Integer,byte[]>();for(int id=1;id<=15;id++) {
            assertEquals(id,b.getShort());assertEquals(0,b.getShort());int n=b.getInt();
            assertTrue(n>=0&&n<=(id==15?ModernBattleHealingWire.LIMIT:id==14?ModernBattleHealingWire.STATUS_LIMIT:ModernBattleWire.sectionLimit(id)));
            var part=new byte[n];b.get(part);out.put(id,part);
        }assertFalse(b.hasRemaining());return out;
    }
    static byte[] check(ModernBattleAudience.View v) {
        var raw=ModernBattleWire.encodePlayerHealing(v);var parts=sections(raw);
        assertEquals(v.world().actors().size(),ByteBuffer.wrap(parts.get(15)).getInt());
        assertArrayEquals(raw,ModernBattleWire.encodePlayerHealing(v));return raw;
    }
    @Test void completeBothOwnersKeepSamePublicSectionsAndDistinctPrivateLhi5()throws Exception {
        try(var f=fixture(1,100,100)) {
            var c=healAfterActive(f);var p=publication(c,null);var a=p.audience(f.players.get(0).scope());var b=p.audience(f.players.get(1).scope());
            var pa=sections(check(a));var pb=sections(check(b));
            for(int id=1;id<=15;id++)if(id!=11)assertArrayEquals(pa.get(id),pb.get(id));
            assertFalse(Arrays.equals(pa.get(11),pb.get(11)));assertEquals(0,pa.get(14)[0]);assertEquals(0,pa.get(14)[1]);
            assertNotNull(a.personal().history().healing());assertEquals(1,a.personal().history().healing().outgoing().spending().current().casts());
            var frozen=check(a);f.elapsed(2*SECOND);heal(f,0,2,1);assertArrayEquals(frozen,check(a));
        }
    }
    static ModernBattleObservation.Capture healAfterActive(ModernBattleEngineHonorTest.Fixture f){active(f);return heal(f,0,1,2).observation();}
    @Test void oldConstructorsAllOldEncodersAndEveryNegotiatedVersionRejectHealing()throws Exception {
        try(var f=fixture(0,100,100)) {
            var c=active(f);var p=publication(c,null);var v=p.audience(f.players.get(0).scope());
            assertThrows(IllegalArgumentException.class,()->new ModernBattlePublication(c,p.descriptor(),null,null));
            for(int n=1;n<=7;n++){int schema=n;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,schema));}
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeSpeed(v));assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeHaste(v));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeNpcMotion(ModernNpcMotion.Payload.selected(v)));
            for(int n=1;n<=ModernSessionProtocol.MAX_VERSION;n++){int version=n;assertFalse(p.descriptor().supportedBy(version,-1));assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(v,version));}
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.put(ByteBuffer.allocate(100),p.descriptor()));assertEquals(47,ModernSessionProtocol.MAX_VERSION);
            assertThrows(SecurityException.class,()->p.audience(new ModernRooms.Scope(7,3333,2,0)));
        }
    }
    static ModernBattleEngineHonorTest.Fixture statusFixture(int kind)throws Exception {
        return new ModernBattleEngineHonorTest.Fixture(1000,100,300,SECOND,1,true,true,p->{
            var s=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.character(),100,300,225);
            var policies=new HashMap<Long,ModernPhysicalCombat.TemplatePolicy>();
            s.templates().forEach((id,v)->policies.put(id,new ModernPhysicalCombat.TemplatePolicy(v.kind(),v.className(),v.criticalMultiplier(),OptionalLong.of(0))));
            var fighter=ModernPhysicalCombatTest.fighter(new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,s.procCatalogIds(),
                    s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth()));
            var next=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true,p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled());
            if(kind>=1)next=ModernBattleHasteTest.enable(next,ModernHasteCatalog.Status.QUALIFIED);
            if(kind==2)next=ModernBattleSlowTest.enable(next,5);
            return next.withPlayerHealing();
        });
    }
    @Test void timedOrdinaryHasteAndSlowAreNotDroppedFromCompleteCandidate()throws Exception {
        for(int kind=0;kind<3;kind++)try(var f=statusFixture(kind)) {
            var c=active(f);check(publication(c,null).audience(f.players.get(0).scope()));
            if(kind>0) {
                var r=f.engine.haste(f.players.get(0).owner(),ModernBattleHasteTest.request(c,0,1,ModernBattleHasteTest.target(c,1)));
                assertEquals(ModernHasteCast.Reason.NONE,r.reason());c=r.observation();
                check(publication(c,null).audience(f.players.get(0).scope()));f.elapsed(2*SECOND);
            }
            c=heal(f,0,kind==0?1:2,1).observation();var v=publication(c,null).audience(f.players.get(0).scope());
            assertEquals(kind,sections(check(v)).get(14)[0]);
            assertEquals(kind==2,v.personal().materials()!=null);
        }
    }
    @Test void missingFeatureAndUnsupportedDependenciesCannotBePublished()throws Exception {
        try(var f=fixture(0,100,100)) {
            var c=active(f);var p=publication(c,null);var d=p.descriptor();
            var old=new ModernMatchDescriptor(d.ruleset(),d.features()&~PLAYER_HEALING,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->ModernBattlePublication.playerHealing(c,old,null,null));
            assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,COMBAT|SPELLS|PLAYER_HEALING,2000,2000,0,1000));
        }
    }
}
