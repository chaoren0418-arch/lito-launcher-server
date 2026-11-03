package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

/** An actual ModernServer owner. Rust performs all auth/navigation/gameplay over
 * verified TLS; stdout carries only the endpoint and final cleanup evidence. */
public final class ModernPlayerHealingRuntimeTestPeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!Set.of("data","commands","two").contains(args[2]))throw new IllegalArgumentException("fixture arguments");
        int lifetime=Integer.parseInt(args[3]);if(lifetime<1000||lifetime>60000)throw new IllegalArgumentException("lifetime");
        ModernPlayerHealingTlsTest.serverContext=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
        Path root=Files.createTempDirectory("lito-healing-runtime-").toAbsolutePath().normalize();
        // The Rust owner performs actual map/SPR/Tile preload, unlike the
        // protocol-only Java client. Keep the engine's normal bounded limit.
        // The two-owner case uses minimum legal dice to make one real melee
        // hit deterministic. Damage/healing still go through the real engine.
        try(var f=new ModernPlayerHealingTlsTest.Fixture(root,30000,b->args[2].equals("two")?0:b-1)) {
            System.out.println("READY "+f.server.address().getPort());System.out.flush();
            long deadline=System.nanoTime()+lifetime*1_000_000L;
            while(System.nanoTime()<deadline) {
                if(f.backend.authenticationCalls.get()>0&&f.server.snapshot().sessions()==0)break;
                Thread.sleep(5);
            }
            // One login checks each of the two independently bound sources.
            assertEquals(args[2].equals("two")?4:2,f.backend.authenticationCalls.get());assertEquals(0,f.server.snapshot().sessions());
            assertEquals(0,f.server.snapshot().failed());
            assertEquals(0,f.worlds.registrationCount());assertEquals(0,f.worlds.matchCount());
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()){
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");
                Files.delete(path);
            }}
        }
        System.out.println("CLOSED "+args[2]);System.out.flush();
    }
}
