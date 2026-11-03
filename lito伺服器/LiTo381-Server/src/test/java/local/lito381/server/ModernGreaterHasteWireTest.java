package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleGreaterHasteTest.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Schema3 is data-only; it cannot authorize an application operation. */
class ModernGreaterHasteWireTest {
    static ModernBattlePublication publication(ModernBattleObservation.Capture c) {
        var f=c.features();var m=c.modes();
        int flags=(f.combat()?COMBAT:0)|(f.spells()?SPELLS:0)|(f.arrow()?LIGHT_ARROW:0)|(f.items()?ITEMS:0)
                |(f.weight()?WEIGHT:0)|(f.timed()?TIMED:0)|(f.ammo()?AMMO:0)|(f.regen()?REGEN:0)
                |(f.fire()?FIRE_STORM:0)|(f.disintegrate()?DISINTEGRATE:0)|(f.greaterHaste()?GREATER_HASTE:0)
                |(m.honor().enabled()?HONOR:0);
        var rule=m.honor().enabled()?Ruleset.HONOR_TOWERS:c.result()!=null?Ruleset.DEATHMATCH_ELIMINATION:Ruleset.PVP_SKIRMISH;
        var l=c.limits();return new ModernBattlePublication(c,new ModernMatchDescriptor(rule,flags,2000,
                l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis()),null,null);
    }
    static Map<Integer,byte[]> sections(byte[] bytes) {
        var b=ByteBuffer.wrap(bytes);assertEquals(ModernBattleWire.MAGIC,b.getInt());assertEquals(3,b.getShort());assertEquals(0,b.getShort());
        assertEquals(bytes.length,b.getInt());assertEquals(13,b.getShort());assertEquals(0,b.getShort());var result=new LinkedHashMap<Integer,byte[]>();
        for(int id=1;id<=13;id++){assertEquals(id,b.getShort());assertEquals(0,b.getShort());int length=b.getInt();assertTrue(length>=0&&length<=ModernBattleWire.sectionLimit(id));
            var part=new byte[length];b.get(part);result.put(id,part);}assertFalse(b.hasRemaining());return result;
    }
    @Test void exactNewHeaderAndPrivateSourceAreBoundWithoutChangingOtherOwnersPublicData()throws Exception {
        try(var f=fixture()) {
            active(f);var p=publication(cast(f,0,1).observation());var a=p.audience(f.players.get(0).scope());var b=p.audience(f.players.get(1).scope());
            var encoded=ModernBattleWire.encode(a,3);var sa=sections(encoded);var sb=sections(ModernBattleWire.encode(b,3));
            assertArrayEquals(encoded,ModernBattleWire.encode(a,3));
            for(int id=1;id<=13;id++)if(id!=ModernBattleWire.PERSONAL)assertArrayEquals(sa.get(id),sb.get(id));
            assertFalse(Arrays.equals(sa.get(11),sb.get(11)));assertEquals(7,ByteBuffer.wrap(sa.get(11)).getInt());assertEquals(42,ByteBuffer.wrap(sb.get(11)).getInt());
            assertEquals(60,a.personal().history().resources().flows().spent());assertEquals(0,b.personal().history().resources().flows().spent());
            assertEquals(2,ByteBuffer.wrap(sa.get(13)).getInt());
        }
    }
    @Test void historySchema2RetainsLane8AndExactLifeGearAndSpending()throws Exception {
        try(var f=fixture()) {
            active(f);var c=cast(f,0,1).observation();var history=publication(c).audience(f.players.get(0).scope()).personal().history();
            var bytes=ModernEquipmentHistoryWire.encode(history,2);assertEquals(2,ByteBuffer.wrap(bytes).getShort(4));
            var decoded=ModernEquipmentHistoryWire.decode(bytes,2);assertEquals(history,decoded);
            assertArrayEquals(bytes,ModernEquipmentHistoryWire.encode(decoded,2));assertEquals(60,decoded.spells().get(LANE).last().cost());
            assertEquals(history.identity(),decoded.identity());assertTrue(decoded.targets().get(LANE).last().targets().isEmpty());
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(bytes));
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(history));
            for(int schema:new int[]{0,1,3})assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(bytes,schema));
        }
    }
    @Test void newHistoryRejectsTruncationTrailingBytesUnknownSchemaAndTooManyLanes()throws Exception {
        try(var f=fixture()) {
            var c=active(f);var h=publication(c).audience(f.players.get(0).scope()).personal().history();var bytes=ModernEquipmentHistoryWire.encode(h,2);
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(Arrays.copyOf(bytes,bytes.length-1),2));
            assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(Arrays.copyOf(bytes,bytes.length+1),2));
            var changed=bytes.clone();changed[5]=3;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(changed,2));
            var count=bytes.clone();count[252]=10;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(count,2));
        }
    }
    @Test void oldBattleSchemasAndOldApplicationDescriptorRefuseNewFeatureEvenBeforeFirstCast()throws Exception {
        try(var f=fixture()) {
            var p=publication(active(f));var v=p.audience(f.players.get(0).scope());
            for(int schema:new int[]{0,1,2,4})assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,schema));
            assertFalse(p.descriptor().supportedBy(41,ModernSessionProtocol.KNOWN_CAPABILITIES));
            assertFalse(p.descriptor().supportedBy(42,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_GREATER_HASTE));
            assertTrue(p.descriptor().supportedBy(42,ModernSessionProtocol.KNOWN_CAPABILITIES));
            var out=ByteBuffer.allocate(ModernMatchDescriptor.BYTES);
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.put(out,p.descriptor()));assertEquals(0,out.position());
        }
        try(var old=new ModernBattleEngineTimedTest.Fixture()) {
            old.active();var v=ModernBattleWireTest.publication(old.engine.serviceObservation(),null).audience(old.players.get(0).scope());
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,3));
            assertDoesNotThrow(()->ModernBattleWire.encode(v,1));assertDoesNotThrow(()->ModernBattleWire.encode(v,2));
        }
    }
    @Test void featureRequiresTimedAndSpellsAndDoesNotRelaxNpcOrSiegePolicies() {
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,COMBAT|GREATER_HASTE,2000,2000,0,1800000));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,COMBAT|SPELLS|GREATER_HASTE,2000,2000,0,1800000));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.SIEGE_OBJECTIVES,63|SIEGE|GREATER_HASTE,2000,2000,0,1800000));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.NPC_ENCOUNTER,63|NPC|GREATER_HASTE,2000,2000,0,1800000));
        assertEquals(8,LANE.ordinal());assertEquals(47,ModernSessionProtocol.MAX_VERSION);
    }
    static ModernBattleObservation.Capture shiftedReceipt(ModernBattleObservation.Capture c) {
        var actors=new java.util.ArrayList<>(c.actors());var a=actors.get(0);var r=a.runtime();
        var receipts=new java.util.HashMap<>(r.receipts());var old=receipts.get(LANE);var detail=(ModernSpellRuntime.GreaterHaste)old.detail();
        receipts.put(LANE,new ModernSpellRuntime.Receipt(old.batch(),old.casts(),old.spent(),old.atNanos(),
                new ModernSpellRuntime.GreaterHaste(detail.activeElapsedNanos()+1,detail.hasteUntilNanos()+1)));
        var changed=new ModernSpellRuntime.State(r.identity(),r.lanes(),r.birthRevision(),r.sequence(),r.requestRevision(),r.initialLawful(),r.lawful(),receipts,r.structure());
        actors.set(0,new ModernBattleObservation.CapturedActor(a.pose(),a.scope(),a.loadout(),a.skills(),a.vital(),a.resources(),a.castIntervalNanos(),
                a.histories(),a.targets(),changed,a.supplies(),a.weight(),a.buffs(),a.ammo(),a.regeneration(),a.regenClock(),a.presentation()));
        return new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),c.limits(),
                c.loadingOriginNanos(),actors,c.modes(),c.npcs(),c.result());
    }
    @Test void retainedReceiptClockCannotChangeAfterPotionOverwriteOrAbort()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1);f.elapsed(1_000_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());
            var potion=f.engine.serviceObservation();assertDoesNotThrow(()->publication(potion));
            assertThrows(IllegalArgumentException.class,()->publication(shiftedReceipt(potion)));
            f.rooms.releasePreparation(f.ticket);var aborted=f.engine.serviceObservation();assertDoesNotThrow(()->publication(aborted));
            assertThrows(IllegalArgumentException.class,()->publication(shiftedReceipt(aborted)));
        }
    }
}
