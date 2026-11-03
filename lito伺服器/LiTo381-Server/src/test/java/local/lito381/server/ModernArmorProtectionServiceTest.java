package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingServiceTest.peer;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real service preparation, owner routing and durable equipment; synthetic
 * qualified source, not a certificate for shared 11101 or player rendering. */
class ModernArmorProtectionServiceTest {
    @TempDir Path root;
    static ModernPlayerHealingServiceTest.Fixture fixture(boolean healing)throws Exception {
        return new ModernPlayerHealingServiceTest.Fixture(1,null,true,healing);
    }
    static void complete(ModernBattleAudience.View view,int ext)throws Exception {
        ModernBattleArmorWireTest.check(view);
        assertArrayEquals(ModernBattleWire.encodeArmorProtection(view),ModernBattleWire.Encoding.selected(peer(ext)).payload(view));
        assertTrue(view.world().descriptor().has(ModernMatchDescriptor.ARMOR_PROTECTION));
        assertTrue(view.world().actors().stream().filter(a->a.pose().member().identity().isHuman()).allMatch(a->a.armorProtection()!=null));
        assertTrue(view.world().actors().stream().filter(a->!a.pose().member().identity().isHuman()).allMatch(a->a.armorProtection()==null));
    }
    @Test void listenerOffersOnlyConfiguredBitsAndHealingDoesNotSilentlyAdvertiseArmor()throws Exception {
        assertEquals(0,ModernServer.extensionsFor(null).supported());
        for(boolean armor:new boolean[]{false,true})for(boolean healing:new boolean[]{false,true}) {
            try(var f=new ModernPlayerHealingServiceTest.Fixture(0,null,armor,healing)) {
                var offer=ModernServer.extensionsFor(f.service);assertEquals((armor?12:0)|(healing?3:0),offer.supported());assertEquals(0,offer.required());
                if(!armor) {
                    var boundary=new ModernSessionBoundary(ModernSessionExtensionsTest.policy(),offer);
                    var raw=ModernSessionProtocol.encode(new ModernSessionProtocol.ExtendedHello(ModernSessionExtensionsTest.base(),ModernSessionExtensions.ARMOR));
                    assertThrows(ModernSessionProtocol.ProtocolException.class,()->boundary.acceptHello(raw));
                }
            }
        }
    }
    @Test void preparationNeedsBothTrustedSourceAndSelectedDataBeforeAnyRead()throws Exception {
        try(var f=fixture(true)) {
            assertThrows(IllegalArgumentException.class,()->f.register(0,peer(7),true,false,null));
            assertThrows(IllegalArgumentException.class,()->f.register(0,peer(3),true,true,null));
            f.register(1,peer(3),true,false,null);
            assertNotEquals(ModernWorldProtocol.Status.OK,f.prepare().status());assertEquals(0,f.reads.get());assertEquals(0,f.service.matchCount());
        }
    }
    @Test void missingOrUnsolicitedFreshArmorAbortsWithoutPartialPublication()throws Exception {
        for(boolean armor:new boolean[]{false,true})try(var f=new ModernPlayerHealingServiceTest.Fixture(0,null,armor,true)) {
            f.register(0,peer(armor?7:3),true,d->{
                var p=f.player(0);var s=ModernBattleEngineHonorTest.source(p.scope().sourceId(),p.row(),1000,300);
                return armor?ModernPhysicalCombatTest.fighter(s):ModernArmorProtectionLoadoutTest.qualify(ModernBattleArmorWireTest.source(s));
            });
            assertEquals(ModernWorldProtocol.Status.OK,f.prepare().status());
            ModernNpcServiceTest.await(()->f.read(1).lifecycle().phase()==ModernWorldProtocol.Phase.ABORTED);
            assertNull(f.read(1).view());f.leave(0);f.leave(1);assertEquals(0,f.service.matchCount());
        }
    }
    @Test void actualServicePublishesDamageProtectionHealingAndDepartureWithoutLosingHistory()throws Exception {
        for(boolean healing:new boolean[]{false,true})try(var f=fixture(healing)) {
            int ext=healing?7:4;f.active();complete(f.read(0).view(),ext);
            var hit=f.attack(1,0,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,((ModernBattleEngine.AttackResult)hit.receipt().outcome()).status());
            var view=hit.receipt().view();complete(view,ext);var armor=view.world().actors().get(0).armorProtection();
            assertEquals(1,armor.sequence());assertTrue(armor.effects().stream().anyMatch(e->e.appliedProtection()));
            assertTrue(armor.untilNanos()>armor.observedNanos());
            var retained=ModernBattleWire.encodeArmorProtection(view);
            if(healing) {
                var q=f.intent(0,1,0);assertThrows(SecurityException.class,()->f.service.playerHeal(f.player(1).owner(),q));
                var result=f.service.playerHeal(f.player(0).owner(),q);assertEquals(ModernPlayerHealingCast.Reason.NONE,result.reason());
                complete(result.view(),ext);assertEquals(armor,result.view().world().actors().get(0).armorProtection());
                var encoded=ModernApplicationProtocol.encode(new ModernApplicationProtocol.PlayerHealingResult(result),peer(ext));
                assertArrayEquals(ModernBattleWire.encodeArmorProtection(result.view()),Arrays.copyOfRange(encoded,192,encoded.length));
            }
            complete(f.move(0,1).receipt().view(),ext);
            assertArrayEquals(retained,ModernBattleWire.encodeArmorProtection(view));
            assertNotEquals(f.read(0).view().personal().scope(),f.read(1).view().personal().scope());
            f.leave(0);complete(f.read(1).view(),ext);f.leave(1);assertEquals(0,f.service.matchCount());
        }
    }
    @Test void completeServiceDurableSetRetainsProtectionAndHealingState()throws Exception {
        try(var f=fixture(true);var store=new ModernEquipmentOverrideStore(root)) {
            var source=f.source(0);var binding=ModernCombatProfileTest.binding(source.input().equipment());
            var bound=ModernArmorProtectionLoadoutTest.bound(source);
            var gear=new ModernEquipmentAuthority((p,s,o)->new ModernEquipmentAuthority.Loaded(source.input().equipment(),binding.fingerprint()),null,
                    (p,s,o)->new ModernEquipmentAuthority.PhysicalLoaded(source,binding.fingerprint(),bound),store,System::currentTimeMillis);
            var principal=new ModernAccountAuthentication.Principal("armor-service",Set.of(new ModernAccountAuthentication.Source(7,"source-7")));
            f.active();f.attack(1,0,1);var before=f.read(0).view();
            var scope=ModernEquipmentOverrideStoreTest.scope();var lease=new ModernEquipmentLease.Issuer().open(f.player(0).owner(),binding.key(),1,()->{});
            try(var op=lease.operation(scope)) {
                ModernBackendScope.bind(scope);
                try {
                    var owned=gear.execute(principal,f.player(0).row(),op,new ModernEquipmentAuthority.Request(7,1,1,ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO)).snapshot();
                    long item=source.input().equipment().source().inventory().stream().filter(i->i.template().kind()==ModernCharacterData.ItemKind.ARMOR).findFirst().orElseThrow().objectId();
                    var set=new ModernEquipmentAuthority.Request(7,1,1,ModernEquipmentAuthority.SET,false,item,owned.revision(),owned.viewToken());
                    var request=new ModernApplicationProtocol.BattleEquipmentRequest(21,new ModernBattleEquipmentProtocol.Request(f.proof(0,before),set));
                    var changed=f.service.equipment(f.player(0).owner(),request,gear,principal,f.player(0).row(),op);
                    assertEquals(ModernBattleEquipmentProtocol.Disposition.APPLIED,changed.reply().disposition());complete(changed.reply().view(),7);
                    assertEquals(before.world().actors().get(0).armorProtection(),changed.reply().view().world().actors().get(0).armorProtection());
                    assertEquals(before.world().actors().get(0).playerHealing().incoming(),changed.reply().view().world().actors().get(0).playerHealing().incoming());
                    assertEquals(1,store.read(op.key(),op.scope()).orElseThrow().revision());
                    var bytes=ModernApplicationProtocol.encode(changed,peer(7));assertTrue(bytes.length>ModernBattleEquipmentProtocol.RESULT_PREFIX_BYTES);
                    assertThrows(IllegalArgumentException.class,()->ModernApplicationProtocol.encode(changed,peer(3)));
                }finally{ModernBackendScope.unbind();}
            }finally{lease.invalidate();scope.finish();}
        }
    }
}
