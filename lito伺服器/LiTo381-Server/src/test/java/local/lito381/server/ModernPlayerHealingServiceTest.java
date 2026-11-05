package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingCast.Reason.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real Rooms -> preparation -> engine -> service publication. Transport and
 * player UI are deliberately not claimed by these tests. */
class ModernPlayerHealingServiceTest {
    @TempDir Path root;
    static final int CAPS=ModernSessionBoundary.Policy.world(true,true,true,true,true,false,true).withBattleActions().supportedCapabilities();
    static ModernSessionBoundary.Negotiated peer(int extensions)throws Exception {
        var offer=new ModernSessionExtensions.Offer(extensions,extensions);
        var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(CAPS,ModernSessionExtensions.DEPENDENCIES,47),offer);
        var hello=new ModernSessionProtocol.Hello(1,47,47,CAPS,ModernSessionExtensions.DEPENDENCIES,1);
        boundary.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedHello(hello,offer)));
        return boundary.negotiated().orElseThrow();
    }
    static void complete(ModernBattleAudience.View view) {
        assertTrue(view.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING));
        byte[] bytes=ModernBattleWire.encodePlayerHealing(view);
        assertEquals(7,ByteBuffer.wrap(bytes).getShort(4));
        assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(view,47));
    }
    @Test void serviceReadMovementAndDepartureRetainExactHealingAndPrivateOwnerScope()throws Exception {
        try(var f=new Fixture()) {
            f.active();assertEquals(2,f.reads.get());
            var attack=f.attack(1,0,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,((ModernBattleEngine.AttackResult)attack.receipt().outcome()).status());
            var q=f.intent(0,1,0);var success=f.service.playerHeal(f.player(0).owner(),q);
            assertEquals(NONE,success.reason());complete(success.view());
            var healed=success.view().world().actors().get(0).playerHealing();
            assertEquals(1,healed.incoming().received());assertTrue(healed.incoming().restored()>0);
            assertEquals(4,healed.outgoing().current().spent());
            assertEquals(296,success.view().world().actors().get(0).vital().values().mp());
            var denied=f.service.playerHeal(f.player(0).owner(),f.intent(0,2,1));
            assertEquals(TOO_FAST,denied.reason());assertEquals(healed,denied.view().world().actors().get(0).playerHealing());
            var old=success.view();var firstRead=f.read(0).view();long clock=firstRead.world().clock().loadingElapsedNanos();
            Thread.sleep(30);var read=f.read(0).view();complete(read);
            assertTrue(read.world().clock().loadingElapsedNanos()>clock);
            assertEquals(healed,read.world().actors().get(0).playerHealing());
            var direct=f.service.readBattle(f.player(1).owner(),new ModernBattleReadProtocol.Request(f.player(1).scope(),f.world));
            complete(direct.view());assertEquals(f.player(1).scope(),direct.view().personal().scope());
            assertEquals(f.player(0).scope().objectId(),direct.view().personal().scope().objectId());
            assertNotEquals(f.player(0).scope().sourceId(),direct.view().personal().scope().sourceId());
            var cooling=f.move(0,1);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,((ModernBattleEngine.MoveResult)cooling.receipt().outcome()).status());
            Thread.sleep(2100); // Real shared action cooldown; never bypass its clock.
            var move=f.move(0,2);complete(move.receipt().view());
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,((ModernBattleEngine.MoveResult)move.receipt().outcome()).status());
            assertEquals(healed,move.receipt().view().world().actors().get(0).playerHealing());
            f.leave(0);var remaining=f.read(1);complete(remaining.view());
            assertEquals(ModernWorldProtocol.Phase.ABORTED,remaining.lifecycle().phase());
            assertEquals(healed,remaining.view().world().actors().get(0).playerHealing());
            assertEquals(NOT_ACTIVE,f.service.playerHeal(f.player(1).owner(),f.intent(1,1,1)).reason());
            assertThrows(SecurityException.class,()->f.service.playerHeal(f.player(0).owner(),q));
            assertEquals(ModernBattleEngine.Phase.ACTIVE,old.world().phase());assertEquals(healed,old.world().actors().get(0).playerHealing());
            f.leave(1);ModernWorldServiceTest.awaitNoMatches(f.service);assertEquals(2,f.terminals.get());
        }
    }
    @Test void botReceiptCarriesTypedSourceIdentityAndCrossSourceUnlearnedDoesNotSpend()throws Exception {
        try(var f=new Fixture()) {
            f.active();var bot=f.read(0).view().world().actors().get(2);
            assertFalse(bot.pose().member().identity().isHuman());
            var success=f.service.playerHeal(f.player(0).owner(),f.intent(0,1,2));assertEquals(NONE,success.reason());
            var receipt=success.view().world().actors().get(2).playerHealing().incoming().last();
            assertEquals(bot.pose().member().identity(),receipt.recipient().identity());
            assertEquals(7,receipt.recipient().identity().sourceId());complete(success.view());
            var foreign=f.service.playerHeal(f.player(1).owner(),f.intent(1,1,0));
            assertEquals(UNLEARNED,foreign.reason());assertEquals(300,foreign.view().world().actors().get(1).vital().values().mp());
            assertEquals(0,foreign.view().world().actors().get(1).playerHealing().outgoing().current().casts());
        }
    }
    @Test void dataOnlyCannotCastAndOldPeerCannotPrepareNewWorld()throws Exception {
        try(var f=new Fixture()) {
            f.register(0,peer(1),true,null);f.active();var before=f.read(0).view();
            assertThrows(SecurityException.class,()->f.service.playerHeal(f.player(0).owner(),f.intent(0,1,0)));
            assertEquals(before.personal().history().resources(),f.read(0).view().personal().history().resources());
        }
        try(var f=new Fixture()) {
            f.register(1,new ModernSessionBoundary.Negotiated(1,47,1,CAPS),false,null);
            assertNotEquals(ModernWorldProtocol.Status.OK,f.prepare().status());assertEquals(0,f.reads.get());assertEquals(0,f.service.matchCount());
        }
    }
    @Test void handshakeDoesNotSubstituteForSourceOptInOrSourceQualifiedArithmetic()throws Exception {
        try(var f=new Fixture()) {
            assertThrows(IllegalArgumentException.class,()->f.register(0,peer(3),false,null));
            assertThrows(IllegalArgumentException.class,()->f.register(0,new ModernSessionBoundary.Negotiated(1,47,1,CAPS),true,null));
            f.register(0,peer(3),true,d->ModernMagicCombatTest.createFighter(7,f.player(0).row(),1000,300,12,0));
            assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());
            ModernNpcServiceTest.await(()->f.read(1).lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED);
            assertNull(f.read(1).view());f.leave(0);f.leave(1);
        }
    }
    @Test void ownerWorldEntityFutureRevisionAndStaleGearCannotBorrowAuthority()throws Exception {
        try(var f=new Fixture()) {
            f.active();var q=f.intent(0,1,1);var p=q.proof();
            assertThrows(SecurityException.class,()->f.service.playerHeal(f.player(1).owner(),q));
            for(var proof:List.of(new ModernBattleCommandLife.Proof(p.player(),p.world(),3,p.life(),p.revision()),
                    new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life(),Long.MAX_VALUE),
                    new ModernBattleCommandLife.Proof(p.player(),new ModernTargetHistory.World(p.world().roomId()+1,p.world().matchId(),p.world().generation()),p.entity(),p.life(),p.revision()))) {
                var wrong=new ModernPlayerHealingProtocol.Request(1,proof,1,q.equipmentRevision(),q.x(),q.y(),q.target());
                assertThrows(SecurityException.class,()->f.service.playerHeal(f.player(0).owner(),wrong));
            }
            var stale=new ModernPlayerHealingProtocol.Request(1,p,1,q.equipmentRevision()+1,q.x(),q.y(),q.target());
            var denied=f.service.playerHeal(f.player(0).owner(),stale);assertEquals(STALE_STATE,denied.reason());
            assertEquals(0,denied.view().world().actors().get(0).playerHealing().outgoing().current().spent());
            assertEquals(NONE,f.service.playerHeal(f.player(0).owner(),f.intent(0,2,1)).reason());
            assertThrows(SecurityException.class,()->f.service.playerHeal(f.player(0).owner(),q));
        }
    }
    @Test void cancelledFreshSourceCannotPublishOrReadFollowingSource()throws Exception {
        try(var f=new Fixture()) {
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            f.register(0,peer(3),true,d->{
                entered.countDown();boolean interrupted=false;
                for(;;)try{if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("provider release deadline");break;}catch(InterruptedException e){interrupted=true;}
                if(interrupted)Thread.currentThread().interrupt();return f.fighter(0);
            });
            try {
                assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());assertTrue(entered.await(2,TimeUnit.SECONDS));
                f.leave(0);release.countDown();
                ModernNpcServiceTest.await(()->f.read(1).lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED);
                assertNull(f.read(1).view());assertEquals(0,f.reads.get());f.leave(1);
            }finally{release.countDown();}
        }
    }
    @Test void mappingCopiesKeepExplicitHealingAndOldDescriptorAdmissionStaysClosed()throws Exception {
        try(var f=new Fixture()) {
            var m=f.mapping;
            for(var copy:List.of(m.withClockOverrides(null),m.withRace(null),m.withDisintegrate(false),m.withGreaterHaste(false),m.withHaste(false),m.withSlow(false))) {
                assertTrue(copy.playerHealingEnabled());var d=ModernMatchDescriptor.of(copy,f.limits,false);
                assertFalse(d.supportedBy(47,CAPS));assertTrue(d.supportedBy(peer(1)));
                assertFalse(d.supportedBy(new ModernSessionBoundary.Negotiated(1,47,1,CAPS)));
            }
            assertFalse(m.withPlayerHealing(false).playerHealingEnabled());
            var invalid=new ModernWorldService.Mapping(f.room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000);
            assertThrows(IllegalArgumentException.class,()->invalid.withPlayerHealing(true));
        }
    }
    @Test void durableEquipmentServiceRepricesOnlyFutureCastsAndKeepsHealingReceipts()throws Exception {
        try(var f=new Fixture();var store=new ModernEquipmentOverrideStore(root)) {
            var source=ModernGreaterHasteEquipmentTest.enoughMp(ModernBattleLifecycleServiceTest.Fixture.source(f.player(0)));
            var binding=ModernCombatProfileTest.binding(source.input().equipment());
            var gear=new ModernEquipmentAuthority((p,s,o)->new ModernEquipmentAuthority.Loaded(source.input().equipment(),binding.fingerprint()),null,
                    (p,s,o)->new ModernEquipmentAuthority.PhysicalLoaded(source,binding.fingerprint()),store,System::currentTimeMillis);
            var principal=new ModernAccountAuthentication.Principal("healing-service-test",Set.of(new ModernAccountAuthentication.Source(7,"source-7")));
            f.register(0,peer(3),true,d->ModernPhysicalCombatTest.fighter(source));f.active();
            var first=f.service.playerHeal(f.player(0).owner(),f.intent(0,1,0));assertEquals(NONE,first.reason());
            var before=first.view().world().actors().get(0).playerHealing();var stale=f.intent(0,2,1);
            var scope=ModernEquipmentOverrideStoreTest.scope();
            var lease=new ModernEquipmentLease.Issuer().open(f.player(0).owner(),binding.key(),1,()->{});
            try(var op=lease.operation(scope)) {
                ModernBackendScope.bind(scope);
                try {
                    var owned=gear.execute(principal,f.player(0).row(),op,new ModernEquipmentAuthority.Request(7,1,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO)).snapshot();
                    var set=new ModernEquipmentAuthority.Request(7,1,1,ModernEquipmentAuthority.SET,false,1,owned.revision(),owned.viewToken());
                    var request=new ModernApplicationProtocol.BattleEquipmentRequest(21,new ModernBattleEquipmentProtocol.Request(f.proof(0,f.read(0).view()),set));
                    var changed=f.service.equipment(f.player(0).owner(),request,gear,principal,f.player(0).row(),op);
                    ModernBattleEquipmentProtocol.validate(changed);complete(changed.reply().view());
                    var encoded=ModernApplicationProtocol.encode(changed,peer(3));
                    int equipmentBytes=java.nio.ByteBuffer.wrap(encoded).getInt(ModernBattleEquipmentProtocol.RESULT_PREFIX_BYTES-8);
                    int payloadBytes=java.nio.ByteBuffer.wrap(encoded).getInt(ModernBattleEquipmentProtocol.RESULT_PREFIX_BYTES-4);
                    assertArrayEquals(ModernBattleWire.encodePlayerHealing(changed.reply().view()),java.util.Arrays.copyOfRange(encoded,
                            ModernBattleEquipmentProtocol.RESULT_PREFIX_BYTES+equipmentBytes,encoded.length));
                    assertEquals(payloadBytes,encoded.length-ModernBattleEquipmentProtocol.RESULT_PREFIX_BYTES-equipmentBytes);
                    assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(changed,peer(0)));
                    assertEquals(ModernBattleEquipmentProtocol.Disposition.APPLIED,changed.reply().disposition());
                    var after=changed.reply().view().world().actors().get(0).playerHealing();
                    assertEquals(before.incoming(),after.incoming());assertEquals(before.receipt(),after.receipt());
                    assertEquals(before.outgoing().current().spent(),after.outgoing().current().spent());
                    assertTrue(after.outgoing().current().cost()>before.outgoing().current().cost());
                    assertEquals(1,store.read(op.key(),op.scope()).orElseThrow().revision());
                    assertEquals(STALE_STATE,f.service.playerHeal(f.player(0).owner(),stale).reason());
                    Thread.sleep(2100);var second=f.service.playerHeal(f.player(0).owner(),f.intent(0,3,1));assertEquals(NONE,second.reason());complete(second.view());
                    assertEquals(before.outgoing().current().spent()+after.outgoing().current().cost(),second.view().world().actors().get(0).playerHealing().outgoing().current().spent());
                }finally{ModernBackendScope.unbind();}
            }finally{lease.invalidate();scope.finish();}
        }
    }
    @Test void terminalVictoryAndSavedResultKeepHealingUntilEachOwnerLeaves()throws Exception {
        var events=new java.util.concurrent.CopyOnWriteArrayList<ModernMatchSettlement.Event>();
        try(var f=new Fixture(0,(event,deadline)->events.add(event))) {
            f.active();var first=f.service.playerHeal(f.player(0).owner(),f.intent(0,1,0));assertEquals(NONE,first.reason());
            var retained=first.view();var healing=retained.world().actors().get(0).playerHealing();
            boolean won=false;
            for(int sequence=1;sequence<=200;sequence++) {
                Thread.sleep(2);var hit=f.attack(1,0,sequence);complete(hit.receipt().view());
                assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,((ModernBattleEngine.AttackResult)hit.receipt().outcome()).status());
                if(hit.receipt().view().world().phase()==ModernBattleEngine.Phase.FINISHED){won=true;break;}
            }
            assertTrue(won);ModernNpcServiceTest.await(()->f.read(1).lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
            var finished=f.read(1);complete(finished.view());assertEquals(1,events.size());
            assertEquals(events.get(0),finished.lifecycle().result());assertEquals(List.of(7,42),events.get(0).participants().stream().map(p->p.key().sourceId()).toList());
            assertEquals(healing,finished.view().world().actors().get(0).playerHealing());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,retained.world().phase());
            f.leave(0);complete(f.read(1).view());f.leave(1);ModernWorldServiceTest.awaitNoMatches(f.service);assertEquals(1,events.size());
        }
    }
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineBotTest.RoomFixture room;
        final ModernWorldService.Mapping mapping;
        final ModernWorldService.Limits limits=new ModernWorldService.Limits(2,1,0,4000,10,new ModernBattleEngine.Limits(4000,0,30000));
        final ModernWorldService service;final AtomicInteger reads=new AtomicInteger(),terminals=new AtomicInteger();ModernTargetHistory.World world;
        final boolean armor,healing;
        Fixture()throws Exception {this(1,null);}
        Fixture(int bots,ModernMatchSettlement.Store journal)throws Exception {
            this(bots,journal,false,true);
        }
        Fixture(int bots,ModernMatchSettlement.Store journal,boolean armor,boolean healing)throws Exception {
            this.armor=armor;this.healing=healing;
            room=new ModernBattleEngineBotTest.RoomFixture(2,bots);
            mapping=new ModernWorldService.Mapping(room.binding,ModernBattleEngineBotTest.cell(10,10),1_000_000,1_000_000,2_000_000_000L,false,true,true,true,true,false,true,null,false).withPlayerHealing(healing).withArmorProtection(armor);
            service=new ModernWorldService(room.rooms,Map.of(room.binding.key(),mapping),limits,b->b-1,journal,Map.of(7,100.0,42,100.0));
            for(int i=0;i<2;i++)register(i,peer((healing?3:0)|(armor?4:0)),healing,null);
        }
        ModernBattleEngineBotTest.Player player(int i){return room.players.get(i);}
        ModernPhysicalCombat.Source source(int i){var p=player(i);var seed=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.row(),1000,300);return armor?ModernBattleArmorWireTest.source(seed):seed;}
        ModernPhysicalCombat.Fighter fighter(int i){return armor?ModernArmorProtectionLoadoutTest.qualify(source(i)):ModernPhysicalCombatTest.fighter(source(i));}
        void register(int i,ModernSessionBoundary.Negotiated protocol,boolean source,ModernWorldService.PhysicalReader physical) {
            register(i,protocol,source,armor,physical);
        }
        void register(int i,ModernSessionBoundary.Negotiated protocol,boolean source,boolean protection,ModernWorldService.PhysicalReader physical) {
            var p=player(i);
            service.register(p.owner(),p.scope(),d->p.row(),physical!=null?physical:d->{assertTrue(d-System.nanoTime()>0);reads.incrementAndGet();return fighter(i);},true,
                    d->new ModernSkills.Snapshot(p.scope().sourceId(),p.row(),i==0?List.of(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL)):List.of()),false,
                    new ModernWorldService.MatchItems(){public List<ModernShopAuthority.Stack> capture(long m,long g,long d){throw new AssertionError();}
                        public List<ModernShopAuthority.Stack> capture(long m,long g,long d,ModernInventoryWeight.Basis b){assertNotNull(b);return List.of();}
                        public void terminal(long m,long g){terminals.incrementAndGet();}},
                    true,true,false,true,false,false,protocol,null,null,null,null,source,protection);
        }
        ModernBattleWorldProtocol.Reply prepare(){var s=room.view().summary();var r=service.battleWorld(player(0).owner(),new ModernBattleWorldProtocol.Request(player(0).scope(),ModernBattleWorldProtocol.Action.PREPARE,s.id(),s.revision(),0,0,ModernWorldProtocol.ZERO_DIGEST));if(r.world()!=null)world=r.world();return r;}
        ModernBattleWorldProtocol.Request worldRequest(int i,ModernBattleWorldProtocol.Action action){return new ModernBattleWorldProtocol.Request(player(i).scope(),action,world.roomId(),0,world.matchId(),world.generation(),action==ModernBattleWorldProtocol.Action.LOADED?room.map.digest():ModernWorldProtocol.ZERO_DIGEST);}
        ModernBattleWorldProtocol.Reply read(int i){return service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.READ));}
        void active()throws Exception {
            assertEquals(ModernWorldProtocol.Status.OK,prepare().status());
            ModernNpcServiceTest.await(()->{var r=read(0);assertNotEquals(ModernWorldProtocol.Phase.ABORTED,r.lifecycle().phase());return r.view()!=null;});
            for(int i=0;i<2;i++)assertEquals(ModernWorldProtocol.Status.OK,service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.LOADED)).status());
            assertEquals(ModernWorldProtocol.Phase.ACTIVE,read(0).lifecycle().phase());
        }
        ModernBattleCommandLife.Proof proof(int i,ModernBattleAudience.View v){var a=v.world().actors().get(i);return new ModernBattleCommandLife.Proof(player(i).scope(),world,a.pose().entityId(),a.resources().life()+1,v.world().revision());}
        ModernPlayerHealingProtocol.Request intent(int i,long sequence,int target){var v=read(i).view();var a=v.world().actors().get(i);var t=v.world().actors().get(target);return new ModernPlayerHealingProtocol.Request(1,proof(i,v),sequence,a.resources().equipmentRevision(),a.pose().position().x(),a.pose().position().y(),
                new ModernPlayerHealingCast.Target(new ModernTargetHistory.Participant(t.pose().entityId(),t.pose().member().identity()),t.resources().life()+1,t.resources().equipmentRevision(),t.pose().position()));}
        ModernApplicationProtocol.BattleOperationResult move(int i,long sequence){var v=read(i).view();var a=v.world().actors().get(i);var command=new ModernApplicationProtocol.WorldRequest(9,new ModernWorldProtocol.Request(player(i).scope(),ModernWorldProtocol.Action.MOVE,4,0,0,world.matchId(),world.generation(),sequence,a.pose().position().x(),a.pose().position().y(),ModernWorldProtocol.ZERO_DIGEST));
            return service.operation(player(i).owner(),new ModernApplicationProtocol.BattleOperationRequest(9,new ModernBattleActionProtocol.Request(proof(i,v),ModernBattleActionProtocol.NO_TARGET,command)));}
        ModernApplicationProtocol.BattleOperationResult attack(int i,int target,long sequence){var v=read(i).view();var t=v.world().actors().get(target);var command=new ModernApplicationProtocol.CombatRequest(10,new ModernCombatProtocol.Request(player(i).scope(),ModernCombatProtocol.Action.ATTACK,world.matchId(),world.generation(),sequence,t.pose().entityId()));
            return service.operation(player(i).owner(),new ModernApplicationProtocol.BattleOperationRequest(10,new ModernBattleActionProtocol.Request(proof(i,v),new ModernBattleActionProtocol.Target(ModernBattleActionProtocol.Kind.PLAYER,t.pose().entityId(),t.resources().life()+1),command)));}
        void leave(int i){assertEquals(ModernWorldProtocol.Status.OK,service.battleWorld(player(i).owner(),worldRequest(i,ModernBattleWorldProtocol.Action.LEAVE)).status());}
        public void close()throws Exception{service.close();assertTrue(service.awaitTermination(5000));room.close();}
    }
}
