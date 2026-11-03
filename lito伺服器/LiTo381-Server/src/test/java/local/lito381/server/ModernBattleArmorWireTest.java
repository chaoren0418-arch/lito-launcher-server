package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import static local.lito381.server.ModernBattleEngineArmorProtectionTest.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real engine captures; candidate syntax proof, not live transport/UI admission. */
class ModernBattleArmorWireTest {
    @TempDir Path root;
    interface Sink {void send(String label,ModernBattleAudience.View view)throws Exception;}
    static ModernPhysicalCombat.Source source(ModernPhysicalCombat.Source seed) {
        var s=ModernArmorProtectionEquipmentTest.source(seed);var policies=new HashMap<Long,ModernPhysicalCombat.TemplatePolicy>();
        s.templates().forEach((id,p)->policies.put(id,new ModernPhysicalCombat.TemplatePolicy(p.kind(),p.className(),p.criticalMultiplier(),
                p.kind()==ModernCharacterData.ItemKind.ETC?OptionalLong.empty():OptionalLong.of(0))));
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),policies,s.procCatalogIds(),s.magicGrowth(),s.magicBows(),
                s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),s.vipCatalog(),s.armorProcs(),s.weaponProcCatalogIds());
    }
    static ModernBattlePublication publication(ModernBattleObservation.Capture c) {
        var old=ModernArmorProtectionObservationTest.descriptor(c);var f=c.features();
        var d=new ModernMatchDescriptor(old.ruleset(),old.features()|(f.ammo()?AMMO:0)|(f.greaterHaste()?GREATER_HASTE:0)
                |(f.haste()?HASTE:0)|(f.slow()?SLOW:0)|(f.npcArrow()?NPC_LIGHT_ARROW:0)|(f.npcFire()?NPC_FIRE_STORM:0)|(f.npcDisintegrate()?NPC_DISINTEGRATE:0),
                old.preparationTimeoutMillis(),old.loadTimeoutMillis(),old.countdownNanos(),old.activeTimeoutMillis());
        var names=c.npcs()==null?null:new ModernNpcNames.Table(c.npcs().roster().actors().stream().map(a->
                new ModernNpcNames.Name(a.template().catalogSourceId(),a.template().npcId(),a.template().graphic(),"fixture","$1")).distinct().toList());
        var resources=names==null?null:new ModernNpcProtocol.Resources(names.rows().stream().map(ModernNpcNames.Name::graphic).distinct().sorted().toList());
        return ModernBattlePublication.armorProtection(c,d,resources,names);
    }
    static Map<Integer,byte[]> sections(byte[] raw) {
        assertTrue(raw.length<=ModernBattleArmorWire.MAX_BYTES);var b=ByteBuffer.wrap(raw);
        assertEquals(ModernBattleWire.MAGIC,b.getInt());assertEquals(8,b.getShort());assertEquals(0,b.getShort());assertEquals(raw.length,b.getInt());assertEquals(16,b.getShort());assertEquals(0,b.getShort());
        var parts=new LinkedHashMap<Integer,byte[]>();
        for(int id=1;id<=16;id++) {
            assertEquals(id,b.getShort());assertEquals(0,b.getShort());int size=b.getInt();
            int limit=id==16?ModernBattleArmorWire.LIMIT:id==15?ModernBattleArmorWire.HEALING_LIMIT:id==14?ModernBattleHealingWire.STATUS_LIMIT:ModernBattleWire.sectionLimit(id);
            assertTrue(size>=0&&size<=limit);var part=new byte[size];b.get(part);parts.put(id,part);
        }
        assertFalse(b.hasRemaining());return parts;
    }
    static void check(ModernBattleAudience.View v) {
        var raw=ModernBattleWire.encodeArmorProtection(v);var parts=sections(raw);var b=ByteBuffer.wrap(parts.get(16));
        assertEquals(v.world().actors().size(),b.getInt());
        for(var a:v.world().actors()) {
            assertEquals(a.pose().entityId(),b.getLong());var s=a.armorProtection();assertEquals(s==null?0:1,b.get());
            if(s==null)continue;
            assertEquals(s.entityId(),b.getLong());assertEquals(s.life(),b.getLong());assertEquals(s.sequence(),b.getLong());assertEquals(s.revision(),b.getLong());
            assertEquals(s.observedNanos(),b.getLong());assertEquals(s.untilNanos(),b.getLong());assertEquals(s.effects().size(),b.getInt());
            for(var e:s.effects()){assertEquals(e.graphics(),b.getLong());assertEquals(e.graphicsMode(),b.getLong());assertEquals(e.appliedProtection()?1:0,b.get());assertEquals(e.durationSeconds(),b.getLong());}
        }
        assertFalse(b.hasRemaining());assertArrayEquals(raw,ModernBattleWire.encodeArmorProtection(v));
        assertEquals(v.world().descriptor().has(PLAYER_HEALING)?1:0,parts.get(15)[0]);
        assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,8));
        assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodePlayerHealing(v));
        for(int version=1;version<=47;version++) {
            int selected=version;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(v,selected));
        }
        var peer=new ModernSessionBoundary.Negotiated(1,47,1,Integer.MAX_VALUE,3);
        assertFalse(v.world().descriptor().supportedBy(peer));
        assertThrows(IllegalArgumentException.class,()->ModernBattleWire.Encoding.selected(peer).payload(v));
    }
    static void send(Sink sink,String label,ModernBattleObservation.Capture c,List<ModernBattleEngineBotTest.Player> players)throws Exception {
        var p=publication(c);for(int i=0;i<players.size();i++)sink.send(label+"-"+i,p.audience(players.get(i).scope()));
    }
    static void trace(Path root,Sink sink)throws Exception {
        try(var f=fixture(10000,false)) {
            send(sink,"loading",f.engine.serviceObservation(),f.players);active(f);
            send(sink,"active",f.engine.serviceObservation(),f.players);f.attack(0,1,2);
            send(sink,"first",f.engine.serviceObservation(),f.players);f.elapsed(SECOND);f.attack(0,2,2);
            send(sink,"second",f.engine.serviceObservation(),f.players);f.elapsed(3*SECOND);
            send(sink,"expired",f.engine.serviceObservation(),f.players);send(sink,"aborted",f.engine.abortObservation(),f.players);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,1000,SECOND,2,false,true,ModernBattleEngineArmorProtectionTest::protectHonor)) {
            active(f);send(sink,"bots",f.engine.serviceObservation(),f.players);
        }
        try(var f=quest(10000)) {
            active(f);f.elapsed(100);f.step(living(f.engine,0));send(sink,"quest",f.engine.serviceObservation(),f.players);
        }
        try(var f=fixture(1,false)) {
            active(f);f.cast(0,1,4,2);send(sink,"dead",f.engine.serviceObservation(),f.players);
            f.elapsed(5*SECOND);send(sink,"revived",f.engine.serviceObservation(),f.players);
        }
        for(int kind=0;kind<=2;kind++)for(boolean healing:new boolean[]{false,true}) {
            final int status=kind;
            var owned=java.nio.file.Files.createDirectory(root.resolve("status"+kind+"-"+healing));
            try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(owned,new ModernEquipmentOverrideStore.FileOps(){},p->{
                if(status>0)p=ModernBattleHasteTest.enable(p,ModernHasteCatalog.Status.QUALIFIED);
                if(status==2)p=ModernBattleSlowTest.enable(p,5);
                return healing?p.withPlayerHealing():p;
            },ModernBattleArmorWireTest::source,true)) {
                var gear=f.read();ModernArmorProtectionEquipmentTest.active(f);ModernArmorProtectionEquipmentTest.hit(f,1);
                String label="status"+kind+"-"+healing;
                send(sink,label+"-hit",f.capture(),f.world.players);
                if(healing)assertEquals(ModernPlayerHealingCast.Reason.NONE,ModernBattlePlayerHealingTest.heal(f.world,0,1,0).reason());
                send(sink,label+"-healed",f.capture(),f.world.players);
                f.set(gear,false);send(sink,label+"-unequipped",f.capture(),f.world.players);
                if(kind>0) {
                    f.world.elapsed(2*SECOND);var c=f.capture();
                    var haste=f.world.engine.haste(f.world.players.get(0).owner(),ModernBattleHasteTest.request(c,0,healing?2:1,ModernBattleHasteTest.target(c,0)));
                    assertEquals(ModernHasteCast.Reason.NONE,haste.reason());send(sink,label+"-cast-haste",haste.observation(),f.world.players);
                }
                if(kind==2) {
                    var c=f.capture();var slow=f.world.engine.slow(f.world.players.get(1).owner(),ModernBattleSlowTest.request(c,1,1,ModernBattleSlowTest.target(c,1)));
                    assertEquals(ModernSlowCast.Reason.NONE,slow.reason());send(sink,label+"-cast-slow",slow.observation(),f.world.players);
                }
            }
        }
    }
    @Test void completeRealCapturesAreBoundedAndNeverNegotiatedAsAnOldFormat()throws Exception {
        var labels=new ArrayList<String>();trace(root,(label,v)->{check(v);labels.add(label);});assertEquals(68,labels.size());
    }
    @Test void bothOwnersSharePublicArmorButNotPrivateEquipmentOrSourceScope()throws Exception {
        try(var f=fixture(10000,false)) {
            active(f);f.attack(0,1,2);var p=publication(f.engine.serviceObservation());
            var a=sections(ModernBattleWire.encodeArmorProtection(p.audience(f.players.get(0).scope())));
            var b=sections(ModernBattleWire.encodeArmorProtection(p.audience(f.players.get(1).scope())));
            for(int id=1;id<=16;id++)if(id!=11)assertArrayEquals(a.get(id),b.get(id));
            assertFalse(Arrays.equals(a.get(11),b.get(11)));
        }
    }
    @Test void armorEntryBoundExactlyIncludesAllOneThousandTwentyFourEffects() {
        assertEquals(25652,ModernBattleArmorWire.STATE_LIMIT);assertEquals(2566104,ModernBattleArmorWire.LIMIT);
        assertEquals(ModernBattleHealingWire.MAX_BYTES+1+8+ModernBattleArmorWire.LIMIT,ModernBattleArmorWire.MAX_BYTES);
        assertEquals(3,ModernSessionExtensions.HEALING_MASK);assertEquals(3,ModernSessionExtensions.HEALING.supported());
        assertEquals(3,ModernSessionExtensions.HEALING.required());assertEquals(7,ModernSessionExtensions.HEALING_ARMOR.required());
    }
}
