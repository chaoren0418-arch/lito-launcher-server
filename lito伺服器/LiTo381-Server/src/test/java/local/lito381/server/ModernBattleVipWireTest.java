package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModernBattleVipWireTest {
    static ModernMatchDescriptor descriptor(ModernBattleObservation.Capture c,boolean settlement) {
        var f=c.features();var l=c.limits();
        int flags=FULL_VIP|(f.combat()?COMBAT:0)|(f.spells()?SPELLS:0)|(f.arrow()?LIGHT_ARROW:0)|(f.items()?ITEMS:0)
                |(f.weight()?WEIGHT:0)|(f.timed()?TIMED:0)|(f.ammo()?AMMO:0)|(f.regen()?REGEN:0)|(f.fire()?FIRE_STORM:0)
                |(f.disintegrate()?DISINTEGRATE:0)|(f.greaterHaste()?GREATER_HASTE:0)|(f.haste()?HASTE:0)|(f.slow()?SLOW:0)
                |(f.playerHealing()?PLAYER_HEALING:0)|(f.armorProtection()?ARMOR_PROTECTION:0);
        return new ModernMatchDescriptor(settlement?Ruleset.DEATHMATCH_ELIMINATION:Ruleset.PVP_SKIRMISH,flags,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
    }
    static byte[] section(byte[] bytes,int id) {
        var b=ByteBuffer.wrap(bytes);assertEquals(ModernBattleWire.MAGIC,b.getInt());assertEquals(10,b.getShort());assertEquals(0,b.getShort());
        assertEquals(bytes.length,b.getInt());assertEquals(18,b.getShort());assertEquals(0,b.getShort());byte[] found=null;
        for(int n=1;n<=18;n++){assertEquals(n,b.getShort());assertEquals(0,b.getShort());int length=b.getInt();var value=new byte[length];b.get(value);if(n==id)found=value;}
        assertFalse(b.hasRemaining());return Objects.requireNonNull(found);
    }
    @Test void fullFramesHaveAllEighteenSectionsAndIndependentOptionalFeatures()throws Exception {
        var frames=ModernBattleVipWirePeer.frames();assertEquals(64,frames.size());
        for(var f:frames){var bytes=ModernBattleVipWire.encode(f.payload());assertTrue(bytes.length<ModernBattleVipWire.MAX_BYTES);var v=f.payload().view();
            for(int id=1;id<=18;id++)assertNotNull(section(bytes,id));
            assertEquals(v.world().features().armorProtection()?1:0,section(bytes,17)[0]);
            assertEquals(v.world().features().playerHealing()?1:0,section(bytes,15)[0]);
            assertArrayEquals(bytes,ModernBattleVipWire.encode(f.payload()));
        }
    }
    @Test void privateOwnerSectionIsBoundToExactSourceAndAllThreeVersions()throws Exception {
        try(var f=ModernBattleEngineVipHealingTest.fixture(4,5,true,0,b->b-1)) {
            f.active();ModernBattleEngineVipAdvancedTest.elapsed(f,6);var c=f.view();
            for(var p:f.players){var payload=ModernBattleVipWire.candidate(c,descriptor(c,false),p.scope());
                var b=ByteBuffer.wrap(section(ModernBattleVipWire.encode(payload),18));
                assertEquals(c.identity().roomId(),b.getLong());assertEquals(c.identity().matchId(),b.getLong());assertEquals(c.identity().generation(),b.getLong());assertEquals(c.revision(),b.getLong());
                assertEquals(p.scope().sourceId(),b.getInt());assertEquals(3333,b.getLong());assertEquals(p.scope().epoch(),b.getLong());assertEquals(p.scope().channelId(),b.getInt());
                assertEquals(payload.view().personal().entityId(),b.getLong());assertEquals(payload.view().personal().history().resources().life(),b.getLong());
                assertEquals(0,b.getLong());assertEquals(2,b.getLong());assertEquals(2,b.getLong());assertEquals(0,b.getInt());
            }
        }
    }
    @Test void descriptorOwnerAndLimitsCannotBeSubstitutedOrDowngraded()throws Exception {
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            var c=f.active();var d=descriptor(c,false);var p=f.players.get(0).scope();
            for(int flags:new int[]{d.features()&~FULL_VIP,d.features()|ARMOR_PROTECTION,d.features()|ITEMS}) {
                var wrong=new ModernMatchDescriptor(d.ruleset(),flags,2000,d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
                assertThrows(IllegalArgumentException.class,()->ModernBattleVipWire.candidate(c,wrong,p));
            }
            var wrong=new ModernMatchDescriptor(d.ruleset(),d.features(),2000,d.loadTimeoutMillis()+1,d.countdownNanos(),d.activeTimeoutMillis());
            assertThrows(IllegalArgumentException.class,()->ModernBattleVipWire.candidate(c,wrong,p));
            assertThrows(SecurityException.class,()->ModernBattleVipWire.candidate(c,d,new ModernRooms.Scope(p.sourceId(),p.objectId(),p.epoch()+1,p.channelId())));
            for(int v=1;v<=ModernSessionProtocol.MAX_VERSION;v++)assertFalse(d.supportedBy(v,-1));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.put(ByteBuffer.allocate(ModernMatchDescriptor.BYTES),d));
        }
    }
    @Test void allThirtyOneAbilityOrdinalsAndWideSignedValuesArePinned()throws Exception {
        assertEquals("STR,DEX,CON,INT,WIS,CHA,AC,MAX_HP,MAX_MP,HPR,MPR,MR,SP,MELEE_DAMAGE,MELEE_HIT,RANGED_DAMAGE,RANGED_HIT,DAMAGE_REDUCTION,MAGIC_REDUCTION,FIRE,WIND,EARTH,WATER,STUN,STONE,SLEEP,FREEZE,SUSTAIN,BLIND,EXP_RATE,ADENA_RATE",
                String.join(",",Arrays.stream(ModernVipEquipment.Field.values()).map(Enum::name).toList()));
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,5_000_000_000L,MAX_MP,6_000_000_000L,EXP_RATE,7_000_000_000L,ADENA_RATE,-5_000_000_000L))) {
            var c=f.active();var v=ModernBattleVipWire.candidate(c,descriptor(c,false),f.players.get(0).scope());
            var b=ByteBuffer.wrap(section(ModernBattleVipWire.encode(v),18));b.position(96);assertEquals(1,b.getInt());b.position(b.position()+40);
            for(var field:ModernVipEquipment.Field.values())assertEquals(v.view().personal().equipment().vipItems().get(0).applied().abilities().get(field),b.getLong());
            assertEquals(5_000_000_100L,v.view().personal().equipment().effective().maximumHp());
        }
    }
    @Test void unrelatedCaptureAndWrongTerminalPolicyCannotEnterVipWire()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var c=f.engine.serviceObservation();assertThrows(IllegalArgumentException.class,()->ModernBattleVipWire.candidate(c,descriptor(c,false),f.players.get(0).scope()));
        }
        try(var f=ModernBattleEngineVipAdvancedTest.fixture(4,2,true,1,1000,500,225,b->0)) {
            f.active();var c=ModernBattleEngineVipAdvancedTest.cast(f,0,1,77).observation();
            assertThrows(IllegalArgumentException.class,()->ModernBattleVipWire.candidate(c,descriptor(c,false),f.players.get(0).scope()));
            assertNotNull(ModernBattleVipWire.candidate(c,descriptor(c,true),f.players.get(0).scope()));
        }
    }
}
