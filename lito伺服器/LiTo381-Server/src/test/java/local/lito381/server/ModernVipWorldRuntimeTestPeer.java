package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.Comparator;

/** Real TLS listener, controlled two-source rows and file CAS. Stdout contains
 * only endpoint/cleanup, never observations or LOAD proofs. No SQL/UI claim. */
public final class ModernVipWorldRuntimeTestPeer {
    public static void main(String[] args)throws Exception {
        if(args.length!=4||!args[0].equals("serve")||!args[2].equals("two"))throw new IllegalArgumentException("fixture arguments");
        int lifetime=Integer.parseInt(args[3]);if(lifetime<1000||lifetime>60000)throw new IllegalArgumentException("lifetime");
        ModernPlayerHealingTlsTest.serverContext=ModernTlsTestMaterial.serverContext(Path.of(args[1]),false);
        Path root=Files.createTempDirectory("lito-vip-world-runtime-").toAbsolutePath().normalize();
        try(var f=new ModernVipWorldTlsTest.Fixture(root)) {
            System.out.println("READY "+f.server.address().getPort());System.out.flush();
            long deadline=System.nanoTime()+lifetime*1_000_000L;
            while(System.nanoTime()<deadline) {
                if(f.backend.authenticationCalls.get()>0&&f.server.snapshot().sessions()==0)break;
                Thread.sleep(5);
            }
            assertEquals(4,f.backend.authenticationCalls.get());assertEquals(0,f.server.snapshot().sessions());
            // Two actors in each of two preparations, plus one fresh source
            // bundle for INSPECT and one for SET on the first match.
            assertEquals(0,f.server.snapshot().failed());assertEquals(6,f.vipReads.get());
            assertEquals(0,f.worlds.registrationCount());assertEquals(0,f.worlds.matchCount());
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()){
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");
                Files.delete(path);
            }}
        }
        System.out.println("CLOSED two");System.out.flush();
    }
}
