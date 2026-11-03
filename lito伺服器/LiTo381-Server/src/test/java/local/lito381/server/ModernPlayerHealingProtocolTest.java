package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattlePlayerHealingTest.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Explicit candidate bytes/engine admission; no listener or new version enabled. */
class ModernPlayerHealingProtocolTest {
    @TempDir Path root;
    static ModernPlayerHealingProtocol.Request intent(ModernBattleObservation.Capture c,int who,long seq,int target) {
        var a=c.actors().get(who);var request=request(c,who,seq,target);
        return new ModernPlayerHealingProtocol.Request(-1,new ModernBattleCommandLife.Proof(a.scope(),c.identity(),a.pose().entityId(),request.life(),c.revision()),
                seq,request.equipmentRevision(),request.cell().x(),request.cell().y(),request.target());
    }
    static ModernPlayerHealingProtocol.Reply reply(ModernPlayerHealingProtocol.Request q,ModernBattleEngine.PlayerHealingResult r,ModernNpcCatalog.Catalog catalog) {
        var p=ModernBattleHealingWireTest.publication(r.observation(),catalog);
        return new ModernPlayerHealingProtocol.Reply(q,r.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p));
    }
    @Test void fixedIntentPreservesOpaqueIdsBotIdentityAndRejectsOldVersionAdmission()throws Exception {
        try(var f=fixture(1,100,100)) {
            var c=active(f);
            for(int target=0;target<3;target++) {
                var q=intent(c,0,1,target);var b=ModernPlayerHealingProtocol.encodeCandidateRequest(q);
                assertEquals(160,b.length);assertEquals(78,ByteBuffer.wrap(b).getShort(12));assertEquals(-1,ByteBuffer.wrap(b).getInt(8));
                assertEquals(q,ModernPlayerHealingProtocol.decodeCandidateRequest(b));
                var stream=new ByteArrayInputStream(ByteBuffer.allocate(320).put(b).put(b).array());
                assertEquals(q,ModernPlayerHealingProtocol.readCandidateRequest(stream));assertEquals(160,stream.available());
                for(int version=1;version<=ModernSessionProtocol.MAX_VERSION;version++){int v=version;
                    assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(b,v));}
                for(int n=0;n<160;n++){var prefix=Arrays.copyOf(b,n);assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingProtocol.decodeCandidateRequest(prefix));}
                var trailing=Arrays.copyOf(b,161);ByteBuffer.wrap(trailing).putInt(4,161);assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingProtocol.decodeCandidateRequest(trailing));
                for(int offset:new int[]{0,4,13,15,120}){var bad=b.clone();bad[offset]^=0x7f;assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingProtocol.decodeCandidateRequest(bad));}
                for(int size:new int[]{0,16,159,161,Integer.MAX_VALUE}){var bad=b.clone();ByteBuffer.wrap(bad).putInt(4,size);var input=new ByteArrayInputStream(bad);
                    assertThrows(IllegalArgumentException.class,()->ModernPlayerHealingProtocol.readCandidateRequest(input));assertEquals(144,input.available());}
                assertEquals("PlayerHealingIntent[redacted]",q.toString());
            }
            assertEquals(47,ModernSessionProtocol.MAX_VERSION);
        }
    }
    @Test void canonicalIdentityProofAndReplayCannotBorrowAnotherOwnerOrWorld()throws Exception {
        try(var f=fixture(0,100,100)) {
            var c=active(f);var q=intent(c,0,1,1);var p=q.proof();
            assertThrows(SecurityException.class,()->f.engine.canonicalPlayerHeal(f.players.get(1).owner(),q));
            for(var proof:List.of(new ModernBattleCommandLife.Proof(p.player(),p.world(),3,p.life(),p.revision()),
                    new ModernBattleCommandLife.Proof(p.player(),new ModernTargetHistory.World(p.world().roomId()+1,p.world().matchId(),p.world().generation()),p.entity(),p.life(),p.revision()),
                    new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life(),Long.MAX_VALUE))) {
                var bad=new ModernPlayerHealingProtocol.Request(q.requestId(),proof,q.sequence(),q.equipmentRevision(),q.x(),q.y(),q.target());
                assertThrows(SecurityException.class,()->f.engine.canonicalPlayerHeal(f.players.get(0).owner(),bad));
            }
            assertEquals(0,f.draws);var result=f.engine.canonicalPlayerHeal(f.players.get(0).owner(),q);assertEquals(NONE,result.reason());
            var r=reply(q,result,null);var bytes=ModernPlayerHealingProtocol.encodeCandidateReply(r);
            assertEquals(79,ByteBuffer.wrap(bytes).getShort(12));assertEquals(7,ByteBuffer.wrap(bytes).getShort(196));
            assertEquals(bytes.length-192,ByteBuffer.wrap(bytes).getInt(188));
            assertThrows(SecurityException.class,()->f.engine.canonicalPlayerHeal(f.players.get(0).owner(),q));
            assertEquals(1,f.engine.serviceObservation().actors().get(0).playerHealing().outgoing().spending().current().casts());
            for(var bad:List.of(intent(c,0,1,0),intent(c,0,2,1),intent(c,1,1,0)))
                assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingProtocol.Reply(bad,NONE,r.view(),r.lifecycle()));
            assertThrows(IllegalArgumentException.class,()->new ModernPlayerHealingProtocol.Reply(q,NO_MP,r.view(),r.lifecycle()));
        }
    }
    @FunctionalInterface interface Sink {
        default byte[] request(String label,ModernBattleAudience.View before,ModernPlayerHealingProtocol.Request q)throws Exception {
            return ModernPlayerHealingProtocol.encodeCandidateRequest(q);
        }
        void accept(String label,ModernBattleAudience.View before,ModernPlayerHealingProtocol.Request q,ModernPlayerHealingProtocol.Reply reply)throws Exception;
    }
    static void send(String label,ModernBattleEngineHonorTest.Fixture f,int who,int target,ModernPlayerHealingCast.Reason expected,Runnable between,Sink sink)throws Exception {
        var c=f.engine.serviceObservation();var a=c.actors().get(who);var q=intent(c,who,a.runtime().sequence()+1,target);
        var before=ModernBattleHealingWireTest.publication(c,null).audience(q.proof().player());
        var bytes=sink.request(label,before,q);var decoded=ModernPlayerHealingProtocol.readCandidateRequest(new ByteArrayInputStream(bytes));assertEquals(q,decoded);
        between.run();var r=reply(decoded,f.engine.canonicalPlayerHeal(f.players.get(who).owner(),decoded),null);assertEquals(expected,r.reason());
        sink.accept(label,before,q,r);
    }
    static void trace(Path root,Sink sink)throws Exception {
        try(var f=fixture(1,100,100)) {
            active(f);send("bot",f,0,2,NONE,()->{},sink);send("cooldown",f,0,1,TOO_FAST,()->{},sink);
            f.elapsed(2*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,1,4,1).reason());send("self-hurt",f,0,0,NONE,()->{},sink);
            f.elapsed(4*SECOND);send("cross",f,0,1,NONE,()->{},sink);
            send("abort",f,0,0,NOT_ACTIVE,()->f.rooms.releasePreparation(f.ticket),sink);
        }
        var factory=new ModernPlayerHealingEquipmentTest();factory.root=Files.createDirectory(root.resolve("gear"));
        try(var f=factory.fixture()) {
            active(f.world);var gear=f.read();send("gear-initial",f.world,0,0,NONE,()->{},sink);
            assertEquals(2,f.capture().actors().get(0).playerHealing().outgoing().spending().current().spent());
            send("stale-gear",f.world,0,1,STALE_STATE,()->assertDoesNotThrow(()->f.set(gear,false)),sink);
            f.world.elapsed(2*SECOND);send("repriced",f.world,0,1,NONE,()->{},sink);
            send("old-life",f.world,0,0,STALE_STATE,f::killAndRevive,sink);
            send("new-life",f.world,0,0,NONE,()->{},sink);
        }
        try(var f=fixture(0,100,3)){active(f);send("no-mp",f,0,1,NO_MP,()->{},sink);}
        try(var f=fixture(0,100,100)){active(f);f.invalid=true;send("numeric",f,0,1,NUMERIC,()->{},sink);}
        try(var f=fixture(0,100,100)) {
            active(f);send("concurrent-target-heal",f,0,1,NONE,()->assertEquals(NONE,heal(f,1,1,1).reason()),sink);
        }
        try(var f=fixture(0,100,100)) {
            active(f);f.randomOverride=bound->{f.randomOverride=null;assertEquals(NONE,heal(f,1,1,0).reason());return bound-1;};
            send("concurrent-caster-healed",f,0,1,STALE_STATE,()->{},sink);
        }
        try(var f=ModernBattleHealingWireTest.statusFixture(2)) {
            var c=active(f);assertEquals(ModernHasteCast.Reason.NONE,f.engine.haste(f.players.get(0).owner(),ModernBattleHasteTest.request(c,0,1,ModernBattleHasteTest.target(c,1))).reason());
            f.elapsed(2*SECOND);send("with-haste-slow-data",f,0,1,NONE,()->{},sink);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,
                p->place(p,p.scope().sourceId()==7?10:29,false))) {
            active(f);send("out-of-range",f,0,1,OUT_OF_RANGE,()->{},sink);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,
                ModernBattleEngine.PreparedMember::withPlayerHealing,
                bytes->{ModernWorldMapTest.setAttributes(bytes,10,10,(short)0,(short)1);return bytes;})) {
            active(f);send("blocked",f,0,1,BLOCKED,()->{},sink);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,
                p->place(p,p.scope().sourceId()==7?10:11,true))) {
            active(f);send("too-heavy",f,0,0,TOO_HEAVY,()->{},sink);
        }
        try(var f=fixture(0,100,100)) {
            var c=active(f);var position=c.actors().get(1).pose().position();
            send("stale-target",f,0,1,STALE_TARGET,()->assertEquals(ModernBattleEngine.MoveStatus.MOVED,
                    f.engine.move(f.players.get(1).owner(),f.players.get(1).scope(),f.ticket.matchId(),f.ticket.generation(),1,position.x(),position.y(),2).status()),sink);
        }
        try(var f=fixture(0,1,100)) {
            active(f);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());
            send("target-dead",f,0,1,TARGET_DEAD,()->{},sink);
        }
        try(var f=fixture(0,100,500)) {
            active(f);send("caster-died-pending",f,0,1,DEAD,()->{
                for(int sequence=1;sequence<=100&&f.engine.serviceObservation().actors().get(0).resources().vitals().hp()>0;sequence++) {
                    f.elapsed((sequence-1)*2*SECOND);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,sequence,4,1).reason());
                }
                assertEquals(0,f.engine.serviceObservation().actors().get(0).resources().vitals().hp());
            },sink);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,100,SECOND,0,false,true,p->new ModernBattleEngine.PreparedMember(
                p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),p.skills().skills().stream().filter(s->s.skillId()!=1).toList()),
                p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled()).withPlayerHealing())) {
            active(f);send("unlearned",f,0,1,UNLEARNED,()->{},sink);
        }
    }
    @Test void completeEngineTraceIncludesDenialsRepricingLivesAndConcurrentOtherPlayers()throws Exception {
        var count=new int[1];trace(root,(label,before,q,r)->{
            ModernBattleHealingWireTest.check(before);var bytes=ModernPlayerHealingProtocol.encodeCandidateReply(r);assertTrue(bytes.length<=ModernPlayerHealingProtocol.MAX_RESULT_BYTES);
            for(int v=1;v<=47;v++){int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,version));}
            count[0]++;
        });assertEquals(22,count[0]);
    }
}
