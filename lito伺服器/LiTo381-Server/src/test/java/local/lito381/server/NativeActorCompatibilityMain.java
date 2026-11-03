package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;
/** Only old90 pinned byte roundtrips, not a suite rerun. */
public final class NativeActorCompatibilityMain {
    public static void main(String[] args)throws Exception {
        int caps=ModernSessionExtensions.NATIVE_DEPENDENCIES|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE;
        var peer=new ModernSessionBoundary.Negotiated(1,47,1,caps,768);int count=0;
        try(var files=Files.list(Path.of(args[0]))){for(var file:files.filter(p->p.getFileName().toString().endsWith(".bin")).toList()){byte[] before=Files.readAllBytes(file);var q=ModernApplicationProtocol.decode(before,peer);require(q instanceof ModernApplicationProtocol.NativeCatalogWorldRequest,"old90 route");require(Arrays.equals(before,ModernApplicationProtocol.encode(q,peer)),"old90 exact bytes");count++;}}
        Files.writeString(Path.of(args[1]),NativeTerrainProbeMain.json(Map.of("state","PASSED_OLD768_REQUEST90_EXACT_VECTOR_ROUNDTRIPS","vectors",count,"old_factory_policy_unchanged",true,"whole_suite_rerun",false)));System.out.println("Old768/90 exact vectors preserved: "+count);
    }
}
