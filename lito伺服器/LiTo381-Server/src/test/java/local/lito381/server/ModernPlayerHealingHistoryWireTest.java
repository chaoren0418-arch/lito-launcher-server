package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingHistoryTest.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Owner section only, not a negotiated world, command or UI capability. */
class ModernPlayerHealingHistoryWireTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;
    static final int SCHEMA=ModernEquipmentHistoryWire.PLAYER_HEALING_SCHEMA;
    static byte[] encode(ModernEquipmentHistory.State s){return ModernEquipmentHistoryWire.encode(s,SCHEMA);}
    static ModernEquipmentHistory.State decode(byte[] b){return ModernEquipmentHistoryWire.decode(b,SCHEMA);}
    static ModernEquipmentHistory.State check(ModernEquipmentHistory.State s) {
        var bytes=encode(s);var read=ModernEquipmentHistoryWire.decodeBound(bytes,s.identity(),s.worldRevision(),SCHEMA);
        assertEquals(s,read);assertArrayEquals(bytes,encode(read));return read;
    }
    static ModernEquipmentHistory.State enabled(ModernEquipmentHistory.State s) {
        long profile=s.spells().values().stream().findFirst().map(v->v.current().profileRevision()).orElse(s.items()==null?s.worldRevision():s.items().profileRevision());
        return healing(s,new ModernPlayerHealingHistory.Entry(ModernPlayerHealingHistory.Incoming.initial(s.identity().world(),s.identity().actor()),
                ModernPlayerHealingHistory.Outgoing.initial(s.resources().equipmentRevision(),profile,4)));
    }
    @Test void emptyAndPopulatedNewTailAreMandatoryAndNeverImplicitlyNegotiated() {
        var initial=initial(7,1,true);var after=cast(initial,initial,1).caster();
        for(var s:List.of(initial,after,initial(42,2,false))) {
            check(s);
            for(int schema=1;schema<=4;schema++){int old=schema;
                assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(s,old));
                assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(encode(s),old));}
        }
        var old=ModernEquipmentHistoryWireTest.empty(1,100);
        assertThrows(IllegalArgumentException.class,()->encode(old));
        var missing=ModernEquipmentHistoryWire.encode(old,4);ByteBuffer.wrap(missing).putShort(4,(short)SCHEMA);
        assertThrows(IllegalArgumentException.class,()->decode(missing));
        assertEquals(47,ModernSessionProtocol.MAX_VERSION);
    }
    @Test void crossSourceSameObjidIncomingDoesNotImportTheForeignEpoch() {
        var a=initial(7,1,true);var b=initial(42,2,false);var pair=cast(a,b,1);
        var received=check(pair.target());assertNull(received.healing().outgoing());
        assertEquals(991,received.identity().source().epoch());assertEquals(7,received.healing().incoming().last().caster().identity().sourceId());
        var id=a.identity();var scoped=new ModernEquipmentHistory.Identity(id.world(),new ModernRooms.Scope(7,3333,Long.MIN_VALUE,0),id.actor());
        var otherEpoch=new ModernEquipmentHistory.State(scoped,a.worldRevision(),a.resources(),a.spells(),a.items(),a.targets(),a.healing());
        var changed=cast(otherEpoch,b,1);
        assertArrayEquals(encode(pair.target()),encode(changed.target()));
        assertFalse(Arrays.equals(encode(pair.caster()),encode(changed.caster())));
        assertEquals(Long.MIN_VALUE,check(changed.caster()).identity().source().epoch());
    }
    @Test void fullHpFreeCastsRetainCountsAndMixedPriceHistoryDoesNotRewriteThePast() {
        var a=enabled(ModernEquipmentHistoryWireTest.empty(-1,Long.MAX_VALUE));
        check(a); // Exact signed maxima and opaque epoch bits survive.
        a=initial(7,1,true);a=cast(a,a,1).caster();check(a);
        var profile=new ModernEquipmentHistory.Profile(1,100,100,java.util.Map.of(ModernUseHistory.Lane.TELEPORT,2),0L);
        a=check(a.reprofile(3,profile,0));var past=a.healing().outgoing().last();
        a=resources(a,a.resources().regenerate(4,100-a.resources().vitals().hp(),0,0,0));
        a=check(cast(a,a,2).caster());
        assertEquals(2,a.healing().incoming().received());assertEquals(8,a.healing().incoming().restored());
        assertEquals(2,a.healing().outgoing().spending().current().casts());assertEquals(4,a.resources().flows().spent());
        assertEquals(0,a.healing().outgoing().last().target().applied());assertEquals(0,a.healing().outgoing().last().use().cost());
        assertEquals(4,past.use().cost());assertEquals(4,a.healing().outgoing().spending().current().priorSpent());
    }
    @Test void allOrdinaryLanesAndMaximumRetainedTargetsStillFitBesideHealing() {
        check(enabled(ModernEquipmentHistoryWireTest.maximum()));
        var s=initial(7,1,true);var all=new java.util.EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
        for(var lane:ModernUseHistory.Lane.values())all.put(lane,ModernUseHistory.Spell.initial(0,1,0));
        s=new ModernEquipmentHistory.State(s.identity(),s.worldRevision(),s.resources(),all,s.items(),s.targets(),s.healing());
        assertEquals(11,check(s).spells().size());
    }
    @Test void prefixesAppendedBytesPaddingFlagsAndAccountTotalsRejectOrRoundTripCanonically() {
        var a=initial(7,1,true);var raw=encode(cast(a,a,1).caster());
        for(int n=0;n<raw.length;n++){var b=Arrays.copyOf(raw,n);if(n>=12)ByteBuffer.wrap(b).putInt(8,n);assertThrows(IllegalArgumentException.class,()->decode(b),"prefix "+n);}
        var extra=Arrays.copyOf(raw,raw.length+1);ByteBuffer.wrap(extra).putInt(8,extra.length);assertThrows(IllegalArgumentException.class,()->decode(extra));
        assertThrows(IllegalArgumentException.class,()->decode(new byte[ModernEquipmentHistoryWire.MAX_BYTES+1]));
        int rejected=0,accepted=0;
        for(int n=0;n<raw.length;n++) {
            var b=raw.clone();b[n]^=1;
            try{var s=decode(b);assertArrayEquals(b,encode(s),"mutation "+n);accepted++;}catch(IllegalArgumentException expected){rejected++;}
        }
        assertTrue(rejected>500);assertTrue(accepted>0);
        // Initial layout: 256 header + one 80-byte spell + 44-byte items.
        var empty=encode(a);assertEquals(528,empty.length);
        for(int offset:new int[]{444,445,448,449,524,525}) {var b=empty.clone();b[offset]=(byte)0x80;assertThrows(IllegalArgumentException.class,()->decode(b),"flag/padding "+offset);}
    }
    @Test void explicitOwnerBindingRejectsOtherWorldActorSourceEpochChannelAndRevision() {
        var a=initial(7,1,true);var after=cast(a,a,1).caster();var raw=encode(after);var id=after.identity();
        for(var wrong:List.of(new ModernEquipmentHistory.Identity(new ModernTargetHistory.World(1,2,4),id.source(),id.actor()),
                new ModernEquipmentHistory.Identity(id.world(),new ModernRooms.Scope(7,3333,99,0),id.actor()),
                new ModernEquipmentHistory.Identity(id.world(),new ModernRooms.Scope(7,3333,1,1),id.actor()),
                new ModernEquipmentHistory.Identity(id.world(),id.source(),new ModernTargetHistory.Participant(2,id.actor().identity())),
                initial(42,2,false).identity()))
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decodeBound(raw,wrong,after.worldRevision(),SCHEMA));
        assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decodeBound(raw,id,after.worldRevision()+1,SCHEMA));
    }
    @Test void actualEnginePrivateCapturesKeepDenialsCrossSourceAndBot()throws Exception {
        try(var f=ModernBattlePlayerHealingTest.fixture(1,100,100)) {
            frames(ModernBattlePlayerHealingTest.active(f));
            frames(ModernBattlePlayerHealingTest.heal(f,0,1,2).observation());
            frames(ModernBattlePlayerHealingTest.heal(f,0,2,1).observation()); // TOO_FAST preserves last success.
            f.elapsed(2*ModernBattlePlayerHealingTest.SECOND);
            frames(ModernBattlePlayerHealingTest.heal(f,0,3,1).observation());
        }
    }
    @Test void actualDurableGearAndRespawnPreserveEncodedHistory()throws Exception {
        var setup=new ModernPlayerHealingEquipmentTest();setup.root=root;
        try(var f=setup.fixture()) {
            frames(ModernBattlePlayerHealingTest.active(f.world));var gear=f.read();
            frames(ModernBattlePlayerHealingTest.heal(f.world,0,1,0).observation());
            frames(f.set(gear,false).observation());f.killAndRevive();frames(f.capture());
            frames(ModernBattlePlayerHealingTest.heal(f.world,0,2,0).observation());
        }
    }
    static void frames(ModernBattleObservation.Capture capture) {
        for(var v:capture.ownerViews(capture.world()).values())check(v.owner().history().history());
    }
}
