package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual TLS and bounded lease pipeline, with explicitly synthetic read-only source providers. */
class ModernWeightNavigationTest {
    @TempDir Path directory;
    @Test void shopFreshlyReadsEquipmentThenCatalogOnOneOwnedScopeAndChangedBasisConflicts()throws Exception {
        try(var peer=new Peer(directory);var client=new Client(peer)) {
            client.enter(7,1);var opened=client.shop(client.read());var initial=opened.outcome().snapshot();
            assertNotNull(initial.weight());assertEquals(List.of("gear","shop"),peer.order());
            var bought=client.shop(client.buy(initial,2));assertEquals(19920,bought.outcome().snapshot().balance());
            assertEquals(7,bought.outcome().snapshot().weight().view().suppliesWeight());
            peer.strength.set(1);var denied=client.shop(client.buy(bought.outcome().snapshot(),1));
            assertEquals(ModernShopAuthority.Status.CONFLICT,denied.outcome().status());
            assertEquals(bought.outcome().snapshot().balance(),denied.outcome().snapshot().balance());
            assertEquals(bought.outcome().snapshot().revision()+1,denied.outcome().snapshot().revision());
            assertNotEquals(initial.weight().view().maximumWeight(),denied.outcome().snapshot().weight().view().maximumWeight());
        }
    }
    @Test void equipmentReceiptAndShopSourceBasisAgreeAndSourceSwitchUsesItsExplicitRate()throws Exception {
        try(var peer=new Peer(directory);var client=new Client(peer)) {
            client.enter(7,1);var gear=(ModernApplicationProtocol.EquipmentResult)client.exchange(new ModernApplicationProtocol.EquipmentRequest(client.next(),
                    new ModernEquipmentAuthority.Request(7,3333,1,0,false,0,0,ModernEquipmentAuthority.ZERO)));
            var shop=client.shop(client.read()).outcome().snapshot();assertNotNull(gear.result().snapshot().weight());
            assertEquals(gear.result().snapshot().weight(),shop.weight());
            client.back();client.enter(42,2);var moon=client.shop(client.read()).outcome().snapshot();
            var moonGear=(ModernApplicationProtocol.EquipmentResult)client.exchange(new ModernApplicationProtocol.EquipmentRequest(client.next(),
                    new ModernEquipmentAuthority.Request(42,3333,2,0,false,0,0,ModernEquipmentAuthority.ZERO)));
            // Source 42 has a different class/original-stat bonus as well as its
            // explicit rate; scaling source 7's final capacity is not equivalent.
            assertEquals(ModernInventoryWeight.qualify(moonGear.result().snapshot(),0.002).maximumWeight(),moon.weight().view().maximumWeight());
            assertEquals(moonGear.result().snapshot().weight(),moon.weight());
            assertNotEquals(shop.weight().view().maximumWeight()*2,moon.weight().view().maximumWeight());
            assertEquals(20000,moon.balance());assertTrue(moon.stacks().isEmpty());
        }
    }
    @Test void cancellationDuringEquipmentReadCannotContinueToCatalogOrPublishWeight()throws Exception {
        try(var peer=new Peer(directory);var client=new Client(peer)) {
            client.enter(7,1);peer.pause=true;client.send(new ModernApplicationProtocol.ShopRequest(client.next(),client.read()));
            assertTrue(peer.entered.await(2,TimeUnit.SECONDS));peer.server.close();
            assertEquals(1,peer.server.snapshot().backendActive());assertFalse(peer.server.awaitTermination(20));
            peer.release.countDown();assertTrue(peer.server.awaitTermination(5000));
            assertEquals(List.of("gear"),peer.order());
        }
    }
    static final class Peer implements AutoCloseable {
        final ModernApplicationTestBackend fixture=new ModernApplicationTestBackend("auth");
        final AtomicLong strength=new AtomicLong(200);final List<String> calls=new ArrayList<>();
        final AtomicReference<ModernBackendScope> lastScope=new AtomicReference<>();
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        final ModernServer server;final javax.net.ssl.SSLContext clientTls;volatile boolean pause;
        Peer(Path directory)throws Exception {
            var material=ModernTlsTestMaterial.defaultDirectory();ModernTlsTestMaterial.prepare(material);
            clientTls=ModernTlsTestMaterial.clientContext(material);
            var store=new ModernEquipmentOverrideStore(directory);
            var authority=new ModernEquipmentAuthority((principal,source,object)->{
                var scope=ModernBackendScope.current();scope.checkActive();lastScope.set(scope);synchronized(calls){calls.add("gear");}
                if(pause){entered.countDown();ModernShopNavigationTest.await(release);}
                var row=fixture.characters.characters(principal,source).stream().filter(c->c.objectId()==object).findFirst().orElseThrow();
                assertEquals(source==7?"sun-owner":"moon-owner",principal.accountForSource(source));
                var q=ModernEquipmentEvaluatorTest.qualified(source,row.characterClass(),List.of(),Map.of(),false,List.of(),List.of());
                var p=q.source().persisted();
                var values=new ModernCharacterData.PersistedValues(p.maxHp(),p.maxMp(),p.savedHp(),p.savedMp(),p.armorClass(),strength.get(),p.dexterity(),p.constitution(),
                        p.intelligence(),p.wisdom(),p.charisma(),p.experience(),p.lawful(),p.originalStrength(),p.originalDexterity(),p.originalConstitution(),p.originalIntelligence(),p.originalWisdom(),p.food225());
                var snapshot=new ModernCharacterData.Snapshot(source,row,values,List.of());
                q=new ModernEquipmentFacts.Qualified(snapshot,q.items(),q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
                return new ModernEquipmentAuthority.Loaded(q,ModernCombatProfileTest.BINDING);
            },store);
            ModernShopCatalogRepository shop=(principal,source,object)->{
                assertSame(lastScope.get(),ModernBackendScope.current());ModernBackendScope.current().checkActive();
                synchronized(calls){calls.add("shop");}
                var row=fixture.characters.characters(principal,source).stream().filter(c->c.objectId()==object).findFirst().orElseThrow();
                return Optional.of(new ModernShopCatalog.Snapshot(source,principal.accountForSource(source),row,List.of(ModernShopAuthorityTest.offer(40,40))));
            };
            var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x7fff,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var binding=new ModernWorldBinding(1,514,map);var rooms=new ModernRooms(Set.of(binding.key()));
            var worlds=new ModernWorldService(rooms,Map.of(binding.key(),new ModernWorldService.Mapping(binding,new ModernWorldMap.Cell(32724,32784),
                    50_000_000,1_000_000_000L,0,false,false,true,true)),
                    new ModernWorldService.Limits(2,1,1,2000,50,new ModernBattleEngine.Limits(2000,0,10000)),bound->0,null,Map.of(7,0.001,42,0.002));
            server=new ModernServer(new InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(material,false),
                    new ModernServer.Limits(2,2,60,2,1,1,2000,2000,2000,10000),fixture.verifier,fixture.characters,
                    new TournamentPresence(),ModernCharacterDataRepository.unavailable(),authority,store,rooms,worlds,9,
                    ModernSkillsRepository.unavailable(),ModernCharacterRecordRepository.unavailable(),null,
                    ModernGmCommandRepository.unavailable(),shop,20000);
        }
        List<String> order(){synchronized(calls){return List.copyOf(calls);}}
        @Override public void close()throws Exception {release.countDown();server.close();assertTrue(server.awaitTermination(5000));}
    }
    static final class Client implements AutoCloseable {
        final SSLSocket socket;int id=2;boolean authenticated;ModernRooms.Scope scope;
        Client(Peer peer)throws Exception {
            socket=(SSLSocket)peer.clientTls.getSocketFactory().createSocket(new Socket("127.0.0.1",peer.server.address().getPort()),"localhost",peer.server.address().getPort(),true);
            var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.setSoTimeout(5000);socket.startHandshake();
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,9,9,2047,7,1)));socket.getOutputStream().flush();
            assertEquals(2047,((ModernSessionProtocol.Ack)ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()))).capabilities());
        }
        int next(){return id++;}
        void enter(int source,long epoch)throws Exception {
            if(!authenticated){try(var auth=new ModernApplicationProtocol.Auth(next(),ModernApplicationTestBackend.ACCOUNT,ModernApplicationTestBackend.PASSWORD.toCharArray())){assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(auth)).status());}authenticated=true;}
            assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(next(),source,epoch))).status());
            assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(next(),source,3333,epoch))).phase());
            assertEquals(2,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.ChannelEnter(next(),source,3333,epoch,0))).phase());scope=new ModernRooms.Scope(source,3333,epoch,0);
        }
        ModernShopProtocol.Request read(){return ModernShopNavigationTest.readRequest(scope,null);}
        ModernShopProtocol.Request buy(ModernShopAuthority.Snapshot s,long qty){return new ModernShopProtocol.Request(scope,ModernShopAuthority.Action.BUY,0,0,s.roundId(),s.revision(),s.token(),40010,qty);}
        ModernShopProtocol.Reply shop(ModernShopProtocol.Request request)throws Exception{return ((ModernApplicationProtocol.ShopResult)exchange(new ModernApplicationProtocol.ShopRequest(next(),request))).reply();}
        void back()throws Exception{assertEquals(1,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.LobbyLeave(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterReturn(next(),scope.sourceId(),scope.objectId(),scope.epoch()))).phase());}
        void send(ModernApplicationProtocol.Message request)throws Exception{socket.getOutputStream().write(ModernApplicationProtocol.encode(request,9));socket.getOutputStream().flush();}
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request)throws Exception{send(request);return ModernApplicationProtocol.decode(ModernApplicationProtocol.readFrame(socket.getInputStream()),9);}
        @Override public void close()throws IOException{socket.close();}
    }
}
