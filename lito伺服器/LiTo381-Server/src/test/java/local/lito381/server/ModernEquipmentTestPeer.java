package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-classpath only: production TLS/listener/session/authority with real owned durable storage. */
public final class ModernEquipmentTestPeer {
    private ModernEquipmentTestPeer() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 4 || !args[0].equals("serve") || !Set.of("normal","reconnect","uncertain").contains(args[2])) {
            throw new IllegalArgumentException("serve <fixture-dir> <normal|reconnect|uncertain> <lifetime-ms>");
        }
        int lifetime = Integer.parseInt(args[3]);
        if (lifetime < 1000 || lifetime > 30000) throw new IllegalArgumentException("bounded lifetime required");
        Path fixture = Path.of(args[1]);
        Path directory = Files.createTempDirectory("modern-equipment-peer-");
        var backend = new ModernApplicationTestBackend("auth");
        var reads = new AtomicInteger(); var moves = new AtomicInteger();
        String configText = "schema=1\nbind.port=0\ntls.pkcs12=" + fixture.resolve("server.p12").toString().replace('\\','/')
                + "\ntls.password.env=T\nsources=7,42\n";
        for (int id : List.of(7,42)) configText += "source."+id+".name=Source"+id+"\nsource."+id+".mysql.host=127.0.0.1\n"
                + "source."+id+".mysql.port=3306\nsource."+id+".mysql.database=fixture_"+id+"\nsource."+id+".catalog.database=catalog\n"
                + "source."+id+".mysql.username.env=U\nsource."+id+".mysql.password.env=P\nsource."+id+".mysql.passwordScheme=plain\nsource."+id+".mysql.tls=disabled\n";
        try (var config = ModernServerConfig.parse(configText.getBytes(StandardCharsets.UTF_8), name -> "fixture-only")) {
            var store = new ModernEquipmentOverrideStore(directory, new ModernEquipmentOverrideStore.FileOps() {
                @Override public void move(Path from, Path to) throws IOException {
                    ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                    moves.incrementAndGet();
                    if (args[2].equals("uncertain")) throw new IOException("injected post-rename uncertainty");
                }
            });
            var authority = new ModernEquipmentAuthority((principal, sourceId, objectId) -> {
                if (!principal.authorizesSource(sourceId) || objectId != 3333) throw new SecurityException("fixture owner required");
                var source = config.sources().stream().filter(row -> row.id()==sourceId).findFirst().orElseThrow();
                String owner = sourceId == 7 ? ModernApplicationTestBackend.SUN_ACCOUNT : ModernApplicationTestBackend.MOON_ACCOUNT;
                if (!owner.equals(principal.accountForSource(sourceId))) throw new SecurityException("fixture canonical owner required");
                reads.incrementAndGet();
                return new ModernEquipmentAuthority.Loaded(qualified(sourceId), ModernEquipmentBinding.ownerBinding(principal,source));
            }, store, () -> 1000L);
            var presence = new TournamentPresence();
            var limits = new ModernServer.Limits(4,2,600,4,2,4,2000,2000,2000,5000);
            var server = new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}),0),
                    ModernTlsTestMaterial.serverContext(fixture,false), limits, backend.verifier, backend.characters, presence,
                    (principal,sourceId,objectId) -> {
                        if (!principal.authorizesSource(sourceId) || objectId != 3333) return Optional.empty();
                        return Optional.of(qualified(sourceId).source());
                    }, authority, store);
            try {
                System.out.println("READY " + server.address().getPort()); System.out.flush();
                long deadline = System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(lifetime);
                int connections = args[2].equals("reconnect") ? 2 : 1;
                for (;;) {
                    var state=server.snapshot();
                    if (state.accepted()==connections && state.sessions()==0) break;
                    if (server.listenerFailed() || System.nanoTime()>=deadline) throw new IllegalStateException("equipment peer incomplete");
                    Thread.sleep(10);
                }
                if (reads.get()==0 || moves.get()==0 || backend.authenticationCalls.get()!=connections*2
                        || server.snapshot().failed()!=(args[2].equals("uncertain")?1:0)) throw new IllegalStateException("equipment was not exercised");
            } finally {
                server.close();
                if (!server.awaitTermination(3000) || !store.isQuiescent() || presence.ownerCount()!=0 || presence.userCount()!=0) {
                    throw new IllegalStateException("equipment ownership did not retire");
                }
            }
            // Reopen proves a SET reached disk even if its response was never sent.
            try (var reopened=new ModernEquipmentOverrideStore(directory);var executor=new ModernBackendExecutor(1,0);var gate=executor.openSession()) {
                var saved=gate.call(2000,scope->reopened.read(new TournamentPresence.CharacterKey(7,3333),scope));
                if (saved.isEmpty() || saved.orElseThrow().revision()<1) throw new IllegalStateException("equipment persistence missing");
                executor.close();
                if (!executor.awaitTermination(3000)) throw new IllegalStateException("reopen read did not retire");
            }
        } finally {
            // Only our fresh flat test directory; never enumerate/delete a workspace.
            try (var children=Files.list(directory)) {
                for (Path child:children.toList()) {
                    if (!Files.isRegularFile(child,java.nio.file.LinkOption.NOFOLLOW_LINKS)) throw new IOException("unexpected test child");
                    Files.delete(child);
                }
            }
            Files.delete(directory);
        }
    }
    static Qualified qualified(int sourceId) {
        var original=ModernCharacterDataTestFixtures.snapshot(sourceId,false);
        var items=List.of(item(5_000_000_001L,true,false,11,5_000_000_007L),item(5_000_000_002L,false,false,22,5_000_000_042L),
                item(5_000_000_003L,false,true,0,0));
        var snapshot=new ModernCharacterData.Snapshot(sourceId,original.character(),original.persisted(),items.stream().map(Instance::source).toList());
        var rules=new HashMap<RuleKey,RuleValue>();
        for (var key:requiredLookups(items)) if (key.kind()!=RuleKind.VIP) rules.put(key,RuleValue.absent());
        return new Qualified(snapshot,Map.of(items.get(0).source().objectId(),items.get(0),items.get(1).source().objectId(),items.get(1),
                items.get(2).source().objectId(),items.get(2)),new Catalog(sourceId,"catalog",rules,Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT),
                ModernItemLifetime.Observation.verifiedPermanent(snapshot));
    }
    private static Instance item(long id,boolean equipped,boolean etc,long str,long hp) {
        var metadata=new ModernCharacterData.ItemTemplate(id+1_000_000_000L,etc?ModernCharacterData.ItemKind.ETC:ModernCharacterData.ItemKind.WEAPON,"$"+id,1,2,1000);
        var source=new ModernCharacterData.InventoryItem(id,metadata,1,equipped,0,0,true,1,false,0);
        EquipmentRules.Facts rules=etc?EquipmentRules.NonEquipmentFacts.INSTANCE:new EquipmentRules.WeaponFacts(127,new EquipmentRules.LevelRange(1,Long.MAX_VALUE),false);
        var bonus=Ability.zero().with(Stat.STR,str).with(Stat.MAX_HP,hp);
        return new Instance(source,new Template(metadata,rules,etc?0:1,etc?0:6,List.of(),bonus,TemplatePhysical.zero()),0,
                RuleValue.present(Ability.zero()),List.of(0L,0L,0L,0L,0L),Stone.none());
    }
}
