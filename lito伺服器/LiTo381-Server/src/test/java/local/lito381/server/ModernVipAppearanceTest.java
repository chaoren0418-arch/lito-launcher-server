package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Actual engine/CAS observations. Graphics remain metadata, never a LOAD ack. */
public final class ModernVipAppearanceTest {
    record Frame(String label,ModernBattleVipWire.Payload payload){}
    static long shape(ModernPhysicalCombat.Source s){return s.vipCatalog().sourceId()==7?5641:4_294_967_301L;}
    static ModernPhysicalCombat.Source shapes(ModernPhysicalCombat.Source s) {
        return shapes(s,shape(s));
    }
    static ModernPhysicalCombat.Source shapes(ModernPhysicalCombat.Source s,long graphics) {
        var old=s.vipCatalog();var vips=new HashMap<Long,ModernVipEquipment.Vip>();
        old.vips().forEach((id,v)->vips.put(id,new ModernVipEquipment.Vip(id,v.exclusiveType(),v.iconId(),graphics,v.abilities())));
        // Absence of a PolyAbility row is not absence of the chosen graphics.
        return ModernVipLoadoutTest.withCatalog(s,new ModernVipEquipment.Catalog(old.sourceId(),old.database(),vips,
                Map.of(graphics,ModernVipEquipment.Polymorph.absent(graphics))));
    }
    static ModernBattleEngineVipTest.Fixture fixture(boolean timed,long hp)throws Exception {
        return fixture(timed,hp,false);
    }
    static ModernBattleEngineVipTest.Fixture fixture(boolean timed,long hp,boolean shared)throws Exception {
        return new ModernBattleEngineVipTest.Fixture(timed,Map.of(MAX_HP,hp),new AtomicLong(1000),b->0,
                s->shapes(s,shared?5641:shape(s)),UnaryOperator.identity(),UnaryOperator.identity(),1_000_000_000L,0,2,false,null,60000,s->shared?5641:shape(s));
    }
    static void add(List<Frame> frames,String label,ModernBattleEngineVipTest.Fixture f,ModernBattleObservation.Capture c) {
        var publication=ModernBattlePublication.fullVip(c,ModernBattleVipWireTest.descriptor(c,false));
        for(var p:f.players)frames.add(new Frame(label+"-"+p.scope().sourceId(),publication.vip(p.scope())));
    }
    static List<Frame> frames()throws Exception {
        return frames(false);
    }
    static List<Frame> frames(boolean shared)throws Exception {
        var frames=new ArrayList<Frame>();
        Path root=Files.createTempDirectory("modern-vip-appearance-").toRealPath();
        try {
            try(var f=fixture(false,100,shared);var store=new ModernEquipmentOverrideStore(root)) {
                add(frames,"gear",f,f.view());add(frames,"gear",f,f.active());
                try(var change=f.reserve(0)) {
                    var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                }
                add(frames,"gear",f,f.view());
                try(var change=f.reserve(0)) {
                    var s=f.sources.get(0);var q=s.input().equipment();
                    var next=ModernVipLoadout.qualifyForRebuild(s,ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,2,Map.of(1L,true)),1000,shared?5641:shape(s));
                    var candidate=ModernVipCharacterState.prepareEquip(change.vipState(),next,1,1000);change.prepareVip(candidate,1000);
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                }
                add(frames,"gear",f,f.view());add(frames,"gear",f,f.engine.abortObservation());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!p.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("owned fixture scope");Files.delete(p);
            }}
        }
        try(var f=fixture(true,100,shared)) {
            add(frames,"expiry",f,f.active());f.epoch.set(2000);add(frames,"expiry",f,f.view());
        }
        try(var f=fixture(false,-99,shared)) {
            add(frames,"death",f,f.active());add(frames,"death",f,f.attack(0,1,2).observation());
        }
        return List.copyOf(frames);
    }
    /** Actual shared Lineage 514 sector and server-authored manifest, not a synthetic map. */
    static List<Frame> realMapFrames(Path root)throws Exception {
        if(!root.isAbsolute())throw new IllegalArgumentException("absolute shared root required");
        var map=ModernWorldMap.load(root,514,Set.of(new ModernWorldMap.SectorKey(0x7fff,0x8000)),()->false);
        if(!map.digest().equals("2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8"))
            throw new IllegalStateException("shared map candidate changed");
        var spawns=List.of(new ModernWorldMap.Cell(32724,32784),new ModernWorldMap.Cell(32725,32784));
        map.validateSpawns(spawns);
        var frames=new ArrayList<Frame>();
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L),new AtomicLong(1000),b->0,
                s->shapes(s,5641),UnaryOperator.identity(),UnaryOperator.identity(),1_000_000_000L,0,2,false,null,60000,
                s->5641,map,spawns)) {
            add(frames,"real-map-pre",f,f.view());
            add(frames,"real-map-active",f,f.active());
        }
        return List.copyOf(frames);
    }
    @Test void publicGraphicsFollowCommittedEquipmentExpiryDeathAndFrozenSnapshots()throws Exception {
        var frames=frames();assertEquals(18,frames.size());
        var start=frames.get(0).payload().view().world().appearances();
        assertEquals(5641,start.get(0).graphicsId());assertEquals(4_294_967_301L,start.get(1).graphicsId());
        assertEquals(3333,start.get(0).objectId());assertEquals(3333,start.get(1).objectId());
        assertEquals(7,start.get(0).sourceId());assertEquals(42,start.get(1).sourceId());
        assertThrows(UnsupportedOperationException.class,()->start.clear());
        var removed=frames.get(4).payload().view().world().appearances();
        assertEquals(0,removed.get(0).graphicsId());assertEquals(1,removed.get(0).runtimeAbilityRevision());
        assertEquals(1,removed.get(0).lifecycleSequence());assertEquals(start.get(1),removed.get(1));
        assertEquals(5641,frames.get(6).payload().view().world().appearances().get(0).graphicsId());
        assertEquals(2,frames.get(6).payload().view().world().appearances().get(0).lifecycleSequence());
        for(var a:frames.get(12).payload().view().world().appearances()){assertEquals(0,a.graphicsId());assertEquals(1,a.runtimeAbilityRevision());}
        var dead=frames.get(16).payload().view();assertEquals(0,dead.world().actors().get(1).resources().vitals().hp());
        assertEquals(4_294_967_301L,dead.world().appearances().get(1).graphicsId());
        for(int i=0;i<frames.size();i+=2)assertEquals(frames.get(i).payload().view().world().appearances(),frames.get(i+1).payload().view().world().appearances());
        assertEquals(5641,start.get(0).graphicsId()); // Later mutations never rewrite old observations.
    }
    @Test void appearanceIsExplicitlySelectedAndEveryOldSectionIsByteIdentical()throws Exception {
        var old=ModernVipSelectedProtocolTest.handshake(112).peer();var selected=ModernVipSelectedProtocolTest.handshake(240).peer();
        assertEquals(ModernBattleVipWire.MAX_APPEARANCE_BYTES,ModernBattleWire.Encoding.selected(selected).maximum());
        for(var f:frames()) {
            var p=f.payload();var before=ModernBattleVipWire.encode(p);assertArrayEquals(before,ModernBattleVipWire.encodeSelected(p,old));
            var bytes=ModernBattleVipWire.encodeSelected(p,selected);var b=ByteBuffer.wrap(bytes);
            assertEquals(11,b.getShort(4));assertEquals(19,b.getShort(12));
            var prefix=Arrays.copyOf(bytes,before.length);ByteBuffer.wrap(prefix).putShort(4,(short)10).putInt(8,prefix.length).putShort(12,(short)18);
            assertArrayEquals(before,prefix);
            b.position(before.length);assertEquals(19,b.getShort());assertEquals(0,b.getShort());assertEquals(108,b.getInt());assertEquals(2,b.getInt());
            for(var a:p.view().world().appearances()) {
                assertEquals(a.entityId(),b.getLong());assertEquals(a.sourceId(),b.getInt());assertEquals(a.objectId(),b.getLong());
                assertEquals(a.life(),b.getLong());assertEquals(a.runtimeAbilityRevision(),b.getLong());assertEquals(a.lifecycleSequence(),b.getLong());assertEquals(a.graphicsId(),b.getLong());
            }
            assertFalse(b.hasRemaining());
            var response=ModernVipSelectedProtocolTest.result(1,p);var wire=ModernApplicationProtocol.encode(response,selected);
            assertArrayEquals(bytes,Arrays.copyOfRange(wire,72,wire.length));
        }
    }
    @Test void selectionAndPublicTypeCannotLeakPrivateLedgerOrSilentlySelectOldPeer()throws Exception {
        assertThrows(IllegalArgumentException.class,()->new ModernSessionExtensions.Offer(128,0));
        var h=ModernVipSelectedProtocolTest.handshake(240);
        var old=new ModernSessionBoundary(new ModernSessionBoundary.Policy(ModernSessionProtocol.KNOWN_CAPABILITIES,ModernSessionExtensions.DEPENDENCIES,47),new ModernSessionExtensions.Offer(112,112));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->old.acceptHello(h.hello()));
        for(var f:ModernBattleVipObservation.Appearance.class.getRecordComponents())assertTrue(f.getType()==long.class||f.getType()==int.class);
        assertEquals(List.of("entityId","sourceId","objectId","life","runtimeAbilityRevision","lifecycleSequence","graphicsId"),
                Arrays.stream(ModernBattleVipObservation.Appearance.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList());
    }
    @Test void sharedReferenceUsesExplicitFormsWithoutRewritingOriginalWideFixture()throws Exception {
        var shared=frames(true);assertEquals(18,shared.size());
        for(var frame:shared)for(var a:frame.payload().view().world().appearances())assertTrue(a.graphicsId()==0||a.graphicsId()==5641);
        assertEquals(5641,shared.get(0).payload().view().world().appearances().get(1).graphicsId());
        assertEquals(0,shared.get(4).payload().view().world().appearances().get(0).graphicsId());
        assertEquals(5641,shared.get(6).payload().view().world().appearances().get(0).graphicsId());
        assertTrue(shared.get(12).payload().view().world().appearances().stream().allMatch(a->a.graphicsId()==0));
        assertEquals(0,shared.get(16).payload().view().world().actors().get(1).resources().vitals().hp());
        assertEquals(4_294_967_301L,frames().get(0).payload().view().world().appearances().get(1).graphicsId());
    }
    static List<Frame> walkingFrames()throws Exception {
        var frames=new ArrayList<Frame>();
        Path root=Files.createTempDirectory("modern-vip-walking-").toRealPath();
        try(var f=fixture(false,100,true);var store=new ModernEquipmentOverrideStore(root)) {
            add(frames,"walk",f,f.active());
            for(int who=0;who<2;who++)for(int heading=0;heading<8;heading++) {
                f.clock.addAndGet(250_000_000);
                var out=ModernBattleEngineVipSpeedTest.move(f,who,heading+1,heading);
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,out.status());add(frames,"walk",f,out.observation());
            }
            var denied=ModernBattleEngineVipSpeedTest.move(f,1,9,0);
            assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,denied.status());add(frames,"walk",f,denied.observation());
            f.clock.addAndGet(250_000_000);
            var moved=ModernBattleEngineVipSpeedTest.move(f,1,10,0);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,moved.status());add(frames,"walk",f,moved.observation());
            try(var change=f.reserve(1)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
            }
            add(frames,"walk",f,f.view());
            f.clock.addAndGet(250_000_000);
            var base=ModernBattleEngineVipSpeedTest.move(f,1,11,0);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,base.status());add(frames,"walk",f,base.observation());
            add(frames,"walk",f,f.engine.abortObservation());
        } finally {
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!p.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("owned walking fixture scope");Files.delete(p);
            }}
        }
        return List.copyOf(frames);
    }
    @Test void sharedWalksCoverBothSourcesAllDirectionsDenialAndCommittedFormReset()throws Exception {
        var frames=walkingFrames();assertEquals(44,frames.size());
        var before=frames.get(32).payload().view().world().actors().get(1);
        var denied=frames.get(34).payload().view().world().actors().get(1);
        assertEquals(before.pose().position(),denied.pose().position());
        assertEquals(9,denied.pose().lastMoveSequence());
        assertEquals(0,frames.get(38).payload().view().world().appearances().get(1).graphicsId());
        assertEquals(before.vital().weaponType(),frames.get(38).payload().view().world().actors().get(1).vital().weaponType());
    }
    static List<Frame> bodyFrames()throws Exception {
        var frames=new ArrayList<Frame>();
        Path root=Files.createTempDirectory("modern-vip-body-").toRealPath();
        try(var f=fixture(false,100,true);var store=new ModernEquipmentOverrideStore(root)) {
            add(frames,"body",f,f.active());
            var hit=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,hit.status());
            assertTrue(hit.observation().actors().get(1).resources().vitals().hp()>0);
            assertTrue(hit.observation().actors().get(1).vital().hurtRevision()>0);
            add(frames,"body",f,hit.observation());add(frames,"body",f,f.view());
            try(var change=f.reserve(0)) {
                var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
            }
            add(frames,"body",f,f.view());
            f.clock.addAndGet(250_000_000);
            var response=f.attack(1,1,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,response.status());
            add(frames,"body",f,response.observation());add(frames,"body",f,f.engine.abortObservation());
        } finally {
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!p.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("owned body fixture scope");Files.delete(p);
            }}
        }
        try(var f=fixture(false,-99,true)) {
            add(frames,"death",f,f.active());add(frames,"death",f,f.attack(0,1,2).observation());
        }
        return List.copyOf(frames);
    }
    @Test void sharedBodyEventsRetainSourceIdentityAndResetAppearanceWithoutResettingWeapon()throws Exception {
        var frames=bodyFrames();assertEquals(16,frames.size());
        for(var frame:frames)for(var actor:frame.payload().view().world().appearances()) {
            assertEquals(3333,actor.objectId());assertTrue(actor.sourceId()==7||actor.sourceId()==42);
        }
        var selected=ModernVipSelectedProtocolTest.handshake(240).peer();
        assertArrayEquals(ModernBattleVipWire.encodeSelected(frames.get(2).payload(),selected),
                ModernBattleVipWire.encodeSelected(frames.get(4).payload(),selected));
        var before=frames.get(2).payload().view().world();var after=frames.get(6).payload().view().world();
        assertEquals(5641,before.appearances().get(0).graphicsId());assertEquals(0,after.appearances().get(0).graphicsId());
        assertEquals(before.actors().get(0).vital().weaponType(),after.actors().get(0).vital().weaponType());
        assertEquals(0,frames.get(14).payload().view().world().actors().get(1).resources().vitals().hp());
    }
    static List<Frame> castFrames()throws Exception {
        var frames=new ArrayList<Frame>();
        Path root=Files.createTempDirectory("modern-vip-casting-").toRealPath();
        try {
            for(int skill:List.of(4,70,77))try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L),new AtomicLong(1000),b->0,
                    s->shapes(ModernBattleEngineVipAdvancedTest.source(s,-1,10000,1000,500,0),5641),
                    p->ModernBattleEngineVipSuppliesTest.supplies(p,-1),ModernBattleEngineVipAdvancedTest::advanced,
                    1_000_000_000L,0,2,false,null,60000,s->5641);var store=new ModernEquipmentOverrideStore(Files.createDirectory(root.resolve("skill-"+skill)))) {
                String label="cast"+skill;
                add(frames,label,f,f.active());
                var cast=ModernBattleEngineVipAdvancedTest.cast(f,0,1,skill);assertEquals("NONE",cast.reason());
                add(frames,label,f,cast.observation());add(frames,label,f,f.view());
                try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                    var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                }
                add(frames,label,f,f.view());add(frames,label,f,f.engine.abortObservation());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!p.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("owned cast fixture scope");Files.delete(p);
            }}
        }
        return List.copyOf(frames);
    }
    @Test void sharedCasterReceiptsRemainAuthoritativeAcrossDuplicatesAndAppearanceChanges()throws Exception {
        var frames=castFrames();assertEquals(30,frames.size());
        var selected=ModernVipSelectedProtocolTest.handshake(240).peer();
        for(int offset=0;offset<frames.size();offset+=10) {
            assertArrayEquals(ModernBattleVipWire.encodeSelected(frames.get(offset+2).payload(),selected),
                    ModernBattleVipWire.encodeSelected(frames.get(offset+4).payload(),selected));
            var cast=frames.get(offset+2).payload().view().world();var gear=frames.get(offset+6).payload().view().world();
            assertEquals(5641,cast.appearances().get(0).graphicsId());assertEquals(0,gear.appearances().get(0).graphicsId());
            assertTrue(cast.actors().get(0).resources().flows().spent()>0);
            for(var a:cast.appearances()){assertEquals(3333,a.objectId());assertTrue(a.sourceId()==7||a.sourceId()==42);}
        }
    }
    static List<Frame> utilityFrames()throws Exception {
        var frames=new ArrayList<Frame>();
        Path root=Files.createTempDirectory("modern-vip-utility-").toRealPath();
        try {
            for(int skill:List.of(5,1,43,54,29))try(var f=ModernBattleEngineVipHealingTest.fixture(-1,-1,false,0,b->0,
                    s->shapes(s,5641),UnaryOperator.identity(),new int[]{2,3},s->5641);
                    var store=new ModernEquipmentOverrideStore(Files.createDirectory(root.resolve("skill-"+skill)))) {
                String label="utility"+skill;
                add(frames,label,f,f.active());
                ModernBattleObservation.Capture cast;
                if(skill==1) {
                    var r=ModernBattleEngineVipHealingTest.heal(f,0,1,1);assertEquals(ModernPlayerHealingCast.Reason.NONE,r.reason());cast=r.observation();
                } else if(skill==5) {
                    var r=ModernBattleEngineVipSuppliesTest.cast(f,0,1,5);assertEquals(ModernSpellProtocol.Reason.NONE,r.reason());cast=r.observation();
                } else if(skill==29) {
                    var r=ModernBattleEngineVipSlowTest.slow(f,0,1,1);assertEquals(ModernSlowCast.Reason.NONE,r.reason());cast=r.observation();
                } else {
                    var r=ModernBattleEngineVipSpeedTest.speed(f,0,1,skill,1);assertEquals("NONE",r.reason());cast=r.observation();
                }
                add(frames,label,f,cast);add(frames,label,f,f.view());
                try(var change=ModernBattleEngineVipSuppliesTest.reserve(f,0)) {
                    var candidate=ModernVipCharacterState.prepareRetainedUnequip(change.vipState(),1,1000);change.prepareVip(candidate,1000);
                    assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(ModernBattleEngineVipTest.commit(store,change,candidate)).disposition());
                }
                add(frames,label,f,f.view());
                var moving=f.view().actors().get(0);
                f.clock.addAndGet(ModernTimedEffects.intervalNanos(moving.castIntervalNanos(),ModernTimedEffects.Action.SPELL,moving.buffs()));
                var positions=f.view().actors().stream().map(a->a.pose().position()).toList();
                int[] dx={0,1,1,1,0,-1,-1,-1},dy={-1,-1,0,1,1,1,0,-1};int heading=-1;
                for(int h=0;h<8;h++) {
                    var from=moving.pose().position();var to=new ModernWorldMap.Cell(from.x()+dx[h],from.y()+dy[h]);
                    if(f.map.canStep(from,to)&&!positions.contains(to)){heading=h;break;}
                }
                assertTrue(heading>=0,"fixture must offer a free legal step");
                var move=ModernBattleEngineVipSpeedTest.move(f,0,1,heading);
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,move.status());add(frames,label,f,move.observation());
                add(frames,label,f,f.engine.abortObservation());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!p.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("owned utility fixture scope");Files.delete(p);
            }}
        }
        return List.copyOf(frames);
    }
    @Test void sharedUtilityCastsKeepDualSourceReceiptsAndAllowMovementAfterTeleport()throws Exception {
        var frames=utilityFrames();assertEquals(60,frames.size());
        var selected=ModernVipSelectedProtocolTest.handshake(241).peer();
        assertThrows(IllegalArgumentException.class,()->ModernBattleVipWire.encodeSelected(frames.get(0).payload(),ModernVipSelectedProtocolTest.handshake(240).peer()));
        for(int offset=0;offset<frames.size();offset+=12) {
            assertArrayEquals(ModernBattleVipWire.encodeSelected(frames.get(offset+2).payload(),selected),
                    ModernBattleVipWire.encodeSelected(frames.get(offset+4).payload(),selected));
            var cast=frames.get(offset+2).payload().view().world();var gear=frames.get(offset+6).payload().view().world();
            assertEquals(5641,cast.appearances().get(0).graphicsId());assertEquals(0,gear.appearances().get(0).graphicsId());
            assertTrue(cast.actors().get(0).resources().flows().spent()>0);
            for(var a:cast.appearances()){assertEquals(3333,a.objectId());assertTrue(a.sourceId()==7||a.sourceId()==42);}
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length>1||(args.length==1&&!Set.of("shared","walk","body","cast","utility","real-map").contains(args[0])))throw new IllegalArgumentException("appearance fixture mode");
        int extensions=args.length==1&&args[0].equals("utility")?241:240;
        var h=ModernVipSelectedProtocolTest.handshake(extensions);var input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));
        System.out.println("SESSION:"+extensions+":"+HexFormat.of().formatHex(h.hello())+":"+HexFormat.of().formatHex(h.ack()));int count=0;
        for(var f:args.length==1&&args[0].equals("real-map")?realMapFrames(Path.of(Objects.requireNonNull(System.getenv("LITO_SHARED_RESOURCE_ROOT"),"explicit shared root"))):
                args.length==1&&args[0].equals("utility")?utilityFrames():args.length==1&&args[0].equals("cast")?castFrames():args.length==1&&args[0].equals("body")?bodyFrames():args.length==1&&args[0].equals("walk")?walkingFrames():frames(args.length==1)) {
            var r=ModernVipSelectedProtocolTest.result(++count,f.payload());
            var q=ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadRequest(r.requestId(),r.echo()),47);
            var bytes=ModernApplicationProtocol.encode(r,h.peer());
            System.out.println("REPLY:"+f.label()+":"+HexFormat.of().formatHex(q)+":"+HexFormat.of().formatHex(bytes));System.out.flush();
            var digest=new StringBuilder();int ch;
            while((ch=input.read())!=-1&&ch!='\n'){if(ch=='\r')continue;if(digest.length()>=64)throw new IllegalArgumentException("oversized digest");digest.append((char)ch);}
            assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),digest.toString());
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
}
