package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javax.net.ssl.SSLSocket;

/** Binding/transport smoke only; the separate rotation peer retains full SQL assertions. */
public final class ModernMapV47StartupProbeMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("qualified properties and owned evidence directory required");
        Path root=Path.of(args[1]).toRealPath();var properties=new Properties();
        try(var reader=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){properties.load(reader);}
        var mappings=ModernWorldConfiguration.parse(properties).load();
        Path tls=root.resolve("tls");ModernTlsTestMaterial.prepare(tls);
        var backend=new ModernApplicationTestBackend("normal");var presence=new TournamentPresence();
        var equipmentReads=new java.util.concurrent.atomic.AtomicInteger();
        var store=new ModernEquipmentOverrideStore(Files.createDirectory(root.resolve("equipment")));
        var equipment=new ModernEquipmentAuthority((principal,source,id)->{equipmentReads.incrementAndGet();throw new IllegalStateException("Transport probe has no source equipment authority");},store);
        var rooms=new ModernRooms(mappings.keySet(),java.util.Set.of(),mappings.entrySet().stream().collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey,e->e.getValue().playerCapacity())));
        var worlds=new ModernWorldService(rooms,mappings,new ModernWorldService.Limits(2,1,1,3000,50,new ModernBattleEngine.Limits(15000,3000000000L,45000)));
        var server=new ModernServer(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),ModernTlsTestMaterial.serverContext(tls,false),new ModernServer.Limits(2,2,600,2,1,4,2000,2000,2000,10000),backend.verifier,backend.characters,presence,ModernCharacterDataRepository.unavailable(),equipment,store,rooms,worlds,47);
        int port=server.address().getPort();int version;
        try(var socket=(SSLSocket)ModernTlsTestMaterial.clientContext(tls).getSocketFactory().createSocket()) {
            var parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);
            socket.setSoTimeout(3000);socket.connect(server.address(),1000);socket.startHandshake();
            int caps=ModernSessionProtocol.CAP_EXPLICIT_I64_STATS|ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES;
            socket.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,47,47,caps,caps,ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1)));socket.getOutputStream().flush();
            var ack=ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(socket.getInputStream()));
            if(!(ack instanceof ModernSessionProtocol.Ack selected)||selected.version()!=47)throw new IllegalStateException("v47 negotiation rejected");
            version=selected.version();
        } finally {server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("Server retained workers");worlds.close();if(!worlds.awaitTermination(5000))throw new IllegalStateException("World retained workers");store.close();}
        if(backend.authenticationCalls.get()!=0||equipmentReads.get()!=0||presence.ownerCount()!=0||rooms.roomCount()!=0||!worlds.isQuiescent())throw new IllegalStateException("Transport probe leaked authority/state");
        Files.writeString(root.resolve("v47-startup-probe.json"),"{\"status\":\"passed\",\"qualified_dm_bindings\":"+mappings.size()+",\"tls_negotiated_version\":"+version+",\"port\":"+port+",\"workers_stopped\":true,\"authentication_calls\":0,\"full_rotation_or_gui_acceptance\":false}",StandardCharsets.UTF_8);
        System.out.println("[map-v47-startup] bindings="+mappings.size()+"; TLS HELLO v47; workers stopped");
    }
}
