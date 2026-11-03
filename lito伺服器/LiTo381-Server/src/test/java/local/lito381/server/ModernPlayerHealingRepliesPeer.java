package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernApplicationProtocol.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

/** Real engine/service replies for all eight existing canonical families.
 * Not a network, SQL or interactive client test. Revival is a loading denial;
 * equipment is an actual durable file-store SET, not an invented success. */
final class ModernPlayerHealingRepliesPeer {
    @FunctionalInterface interface Sink {void send(String label,byte[] request,byte[] reply)throws Exception;}
    private static void send(Sink out,String label,Message request,Message reply)throws Exception {
        out.send(label,ModernApplicationProtocol.encode(request,ModernPlayerHealingApplicationTest.peer(3)),
                ModernApplicationProtocol.encode(reply,ModernPlayerHealingApplicationTest.peer(3)));
    }
    static void trace(Sink out)throws Exception {
        Path root=Files.createTempDirectory("lito-healing-replies-").toAbsolutePath().normalize();
        try {
            ModernHealingSceneTraceTest.trace(Files.createDirectory(root.resolve("scene")),(label,capture,catalog)->{
                var p=ModernBattleHealingWireTest.publication(capture,catalog);
                for(var owner:capture.ownerViews(capture.world()).keySet()) {
                    var v=p.audience(owner);var w=v.world().identity();String name=label+"-"+owner.sourceId();
                    var rq=new ModernBattleReadProtocol.Request(owner,w);
                    send(out,"read-"+name,new BattleReadRequest(2,rq),new BattleReadResult(2,rq,
                            new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v)));
                    var wq=new ModernBattleWorldProtocol.Request(owner,ModernBattleWorldProtocol.Action.READ,w.roomId(),0,w.matchId(),w.generation(),ModernWorldProtocol.ZERO_DIGEST);
                    send(out,"world-"+name,new BattleWorldRequest(3,wq),new BattleWorldResult(3,wq,
                            new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,w,ModernBattleLifecycle.from(p),v)));
                    if(label.equals("npc-loading")) {
                        var a=v.world().actors().get(0);
                        var proof=new ModernBattleCommandLife.Proof(owner,w,a.pose().entityId(),a.resources().life()+1,v.world().revision());
                        var q=new ModernBattleRevivalProtocol.Request(proof,proof.revision());
                        send(out,"revival-denied-"+name,new BattleRevivalRequest(4,q),new BattleRevivalResult(4,q,
                                new ModernBattleRevivalProtocol.Reply(ModernBattleRevivalProtocol.Reason.NOT_ACTIVE,v,ModernBattleLifecycle.from(p))));
                    }
                }
            });
            try(var f=new ModernPlayerHealingServiceTest.Fixture()) {
                f.active();assertEquals(ModernPlayerHealingCast.Reason.NONE,f.service.playerHeal(f.player(0).owner(),f.intent(0,1,1)).reason());
                var result=f.move(0,1);
                send(out,"operation-move",new BattleOperationRequest(result.requestId(),result.echo()),result);
            }
            try(var f=ModernBattleHealingWireTest.statusFixture(2)) {
                var before=ModernBattlePlayerHealingTest.active(f);
                var q=ModernHasteProtocolTest.intent(before,0,1,ModernBattleHasteTest.target(before,1));
                var cast=f.engine.canonicalHaste(f.players.get(0).owner(),q);assertEquals(ModernHasteCast.Reason.NONE,cast.reason());
                var p=ModernBattleHealingWireTest.publication(cast.observation(),null);
                send(out,"haste-cast",new HasteRequest(q),new HasteResult(ModernHasteProtocolTest.reply(q,cast,p)));
                f.elapsed(2_000_000_000L);p=ModernBattleHealingWireTest.publication(f.engine.serviceObservation(),null);
                var sq=ModernSlowProtocolTest.intent(p.audience(f.players.get(0).scope()),1,2);
                var slow=f.engine.canonicalSlow(f.players.get(0).owner(),sq);assertEquals(ModernSlowCast.Reason.NONE,slow.reason());
                var next=ModernBattleHealingWireTest.publication(slow.observation(),null);
                send(out,"slow-cast",new SlowRequest(sq),new SlowResult(new ModernSlowProtocol.Reply(sq,slow.reason(),next.audience(sq.proof().player()),ModernBattleLifecycle.from(next))));
            }
            try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,300,1_000_000_000L,1,true,true,
                    p->ModernBattleGreaterHasteTest.enable(p,ModernGreaterHasteCatalog.Status.QUALIFIED).withPlayerHealing())) {
                var q=ModernGreaterHasteProtocolTest.intent(ModernBattlePlayerHealingTest.active(f),0,1);
                var cast=f.engine.canonicalGreaterHaste(f.players.get(0).owner(),q);assertEquals(ModernGreaterHasteCast.Reason.NONE,cast.reason());
                var p=ModernBattleHealingWireTest.publication(cast.observation(),null);
                send(out,"greater-cast",new GreaterHasteRequest(q),new GreaterHasteResult(new ModernGreaterHasteProtocol.Reply(q,cast.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p))));
            }
            equipment(Files.createDirectory(root.resolve("gear")),out);
        } finally {
            // Only the exact directory allocated above belongs to this peer.
            try(var paths=Files.walk(root)) {for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");
                Files.delete(path);
            }}
        }
    }
    private static void equipment(Path root,Sink out)throws Exception {
        try(var f=new ModernPlayerHealingServiceTest.Fixture();var store=new ModernEquipmentOverrideStore(root)) {
            var source=ModernGreaterHasteEquipmentTest.enoughMp(ModernBattleLifecycleServiceTest.Fixture.source(f.player(0)));
            var binding=ModernCombatProfileTest.binding(source.input().equipment());
            var gear=new ModernEquipmentAuthority((p,s,o)->new ModernEquipmentAuthority.Loaded(source.input().equipment(),binding.fingerprint()),null,
                    (p,s,o)->new ModernEquipmentAuthority.PhysicalLoaded(source,binding.fingerprint()),store,System::currentTimeMillis);
            var principal=new ModernAccountAuthentication.Principal("healing-replies-test",Set.of(new ModernAccountAuthentication.Source(7,"source-7")));
            f.register(0,ModernPlayerHealingServiceTest.peer(3),true,d->ModernPhysicalCombatTest.fighter(source));f.active();
            assertEquals(ModernPlayerHealingCast.Reason.NONE,f.service.playerHeal(f.player(0).owner(),f.intent(0,1,0)).reason());
            var scope=ModernEquipmentOverrideStoreTest.scope();
            var lease=new ModernEquipmentLease.Issuer().open(f.player(0).owner(),binding.key(),1,()->{});
            try(var op=lease.operation(scope)) {
                ModernBackendScope.bind(scope);
                try {
                    var owned=gear.execute(principal,f.player(0).row(),op,new ModernEquipmentAuthority.Request(7,1,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO)).snapshot();
                    var set=new ModernEquipmentAuthority.Request(7,1,1,ModernEquipmentAuthority.SET,false,1,owned.revision(),owned.viewToken());
                    var request=new BattleEquipmentRequest(21,new ModernBattleEquipmentProtocol.Request(f.proof(0,f.read(0).view()),set));
                    var changed=f.service.equipment(f.player(0).owner(),request,gear,principal,f.player(0).row(),op);
                    assertEquals(ModernBattleEquipmentProtocol.Disposition.APPLIED,changed.reply().disposition());
                    assertEquals(1,store.read(op.key(),op.scope()).orElseThrow().revision());
                    send(out,"equipment-durable",request,changed);
                } finally {ModernBackendScope.unbind();}
            } finally {lease.invalidate();scope.finish();}
        }
    }
}
