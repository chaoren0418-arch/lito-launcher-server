package local.lito381.server;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Two real TLS/session owners and a shared map. Source rows/catalog are explicit
 * synthetic fixtures; no SQL, production client, native HWND or settlement claim. */
public final class ModernGreaterHasteTestPeer {
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!args[2].equals("normal"))throw new IllegalArgumentException();
            int lifetime=Integer.parseInt(args[3]);if(lifetime<5000||lifetime>60000)throw new IllegalArgumentException();
            run(Path.of(args[1]),ModernWorldTestPeer.sharedMap(Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"))),lifetime);
        } catch(Exception failure) {
            System.err.println("MODERN_GREATER_HASTE_PEER_FAILED "+failure.getClass().getName());
            java.util.Arrays.stream(failure.getStackTrace()).limit(10).forEach(f->System.err.println("  at "+f));
            System.exit(2);
        }
    }
    private static void run(Path tls,ModernWorldMap map,int lifetime)throws Exception {
        Path directory=Files.createTempDirectory("modern-haste-peer-").toAbsolutePath().normalize();
        try(var store=new ModernEquipmentOverrideStore(directory)) {
            var fixture=new ModernApplicationTestBackend("auth",80);
            var physicalReads=new AtomicInteger();var catalogs=new AtomicInteger();var skills=new AtomicInteger();
            ModernEquipmentAuthority.PhysicalProvider physical=(principal,source,id)->{
                if(id!=3333||!principal.authorizesSource(source)||source!=7&&source!=42
                        ||!(source==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT).equals(principal.accountForSource(source)))throw new SecurityException();
                var row=fixture.characters.characters(principal,source).stream().filter(c->c.objectId()==id).findFirst().orElseThrow();
                var qualified=ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(ModernPhysicalCombatTest.fixtureSource(source,row,1000,10,0),12,0,0,0,300));
                physicalReads.incrementAndGet();
                byte[] binding=new byte[32];binding[0]=(byte)source;
                return new ModernEquipmentAuthority.PhysicalLoaded(qualified,new ModernEquipmentOverrideStore.Fingerprint(binding));
            };
            var equipment=new ModernEquipmentAuthority((p,s,id)->{
                var loaded=physical.read(p,s,id);return new ModernEquipmentAuthority.Loaded(loaded.source().input().equipment(),loaded.ownerBinding());
            },null,physical,store);
            ModernSkillsRepository learned=(p,s,id)->{
                var loaded=physical.read(p,s,id);skills.incrementAndGet();
                return Optional.of(new ModernSkills.Snapshot(s,loaded.source().input().equipment().source().character(),
                        s==7?List.of(new ModernSkills.Skill(54,"$1976",60,10,ModernSkills.Support.UNIMPLEMENTED)):List.of()));
            };
            ModernGreaterHasteRepository catalog=(p,s,id)->{
                physical.read(p,s,id);catalogs.incrementAndGet();
                return Optional.of(new ModernGreaterHasteCatalog.Snapshot(new TournamentPresence.CharacterKey(s,id),
                        s==7?ModernGreaterHasteCatalog.Status.QUALIFIED:ModernGreaterHasteCatalog.Status.UNLEARNED));
            };
            ModernShopCatalogRepository shop=(p,s,id)->Optional.of(new ModernShopCatalog.Snapshot(s,p.accountForSource(s),
                    physical.read(p,s,id).source().input().equipment().source().character(),List.of()));
            var presence=new TournamentPresence();var binding=new ModernWorldBinding(1,514,map);
            var rooms=new ModernRooms(Set.of(binding.key()));
            var mapping=new ModernWorldService.Mapping(binding,ModernWorldTestPeer.ANCHOR,200_000_000L,1_000_000_000L,
                    2_000_000_000L,false,false,true,true,true).withGreaterHaste(true);
            try(var worlds=new ModernWorldService(rooms,Map.of(binding.key(),mapping),
                    new ModernWorldService.Limits(4,2,2,3000,50,new ModernBattleEngine.Limits(30_000,200_000_000L,45_000)),
                    bound->bound-1,null,Map.of(7,100.0,42,100.0))) {
                var server=new ModernServer(new InetSocketAddress("127.0.0.1",0),ModernTlsTestMaterial.serverContext(tls,false),
                        new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,10_000),fixture.verifier,fixture.characters,presence,
                        (p,s,id)->Optional.of(physical.read(p,s,id).source().input().equipment().source()),equipment,store,rooms,worlds,43,
                        learned,ModernCharacterRecordRepository.unavailable(),null,ModernGmCommandRepository.unavailable(),shop,20000,null,false,catalog);
                try {
                    System.out.println("READY "+server.address().getPort());System.out.flush();
                    long until=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                    for(;;){var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                        if(state.accepted()>2||server.listenerFailed()||System.nanoTime()>=until)throw new IllegalStateException("TLS flow incomplete");Thread.sleep(10);}
                    if(fixture.authenticationCalls.get()!=4||physicalReads.get()<4||catalogs.get()<4||catalogs.get()!=skills.get()||server.snapshot().failed()!=0) {
                        System.err.println("HASTE_COUNTERS auth="+fixture.authenticationCalls.get()+" physical="+physicalReads.get()+" catalog="+catalogs.get()+" skills="+skills.get()+" failed="+server.snapshot().failed());
                        System.err.println("HASTE_FAILURE_COUNTS "+server.failureCounts());throw new IllegalStateException("qualification incomplete");
                    }
                } finally {
                    server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("server not retired");
                    worlds.close();if(!worlds.awaitTermination(5000)||!worlds.isQuiescent()||worlds.registrationCount()!=0
                            ||presence.ownerCount()!=0||presence.userCount()!=0||rooms.ownerCount()!=0||rooms.roomCount()!=0||rooms.memberCount()!=0||!store.isQuiescent())throw new IllegalStateException("owners not retired");
                }
            }
        } finally {
            // Only flat files in this invocation's fresh directory, after owners close.
            try(var files=Files.list(directory)){for(Path file:files.toList()){
                if(!file.toAbsolutePath().normalize().getParent().equals(directory)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("unexpected child");
                Files.delete(file);
            }}Files.delete(directory);
        }
    }
}
