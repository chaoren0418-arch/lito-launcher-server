package local.lito381.server;

import java.nio.file.*;
import java.util.*;

/** Run identically with baseline and wire-candidate JARs; emits exact old v47 bytes. */
public final class NativeWireLegacyCompatibilityMain {
    public static void main(String[] args)throws Exception {
        var rows=new LinkedHashMap<String,Object>();int caps=ModernSessionProtocol.CAP_EXPLICIT_I64_STATS|ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES;
        var hello=new ModernSessionProtocol.Hello(0x12345678,47,47,caps,caps,1);
        var policy=new ModernSessionBoundary.Policy(caps,caps,47);var boundary=new ModernSessionBoundary(policy);
        var bytes=ModernSessionProtocol.encode(hello);rows.put("plain_hello",HexFormat.of().formatHex(bytes));rows.put("plain_ack",HexFormat.of().formatHex(boundary.acceptHello(bytes)));
        var all=new ModernSessionProtocol.Hello(1,47,47,ModernSessionExtensions.DEPENDENCIES,ModernSessionExtensions.DEPENDENCIES,1);
        var ext=new ModernSessionProtocol.ExtendedHello(all,new ModernSessionExtensions.Offer(255,255));
        var extPolicy=new ModernSessionBoundary.Policy(ModernSessionExtensions.DEPENDENCIES,ModernSessionExtensions.DEPENDENCIES,47);
        rows.put("existing_extension_hello",HexFormat.of().formatHex(ModernSessionProtocol.encode(ext)));
        rows.put("existing_extension_ack",HexFormat.of().formatHex(new ModernSessionBoundary(extPolicy,new ModernSessionExtensions.Offer(255,255)).acceptHello(ModernSessionProtocol.encode(ext))));
        var scope=new ModernRooms.Scope(7,Long.MAX_VALUE,1,0);
        var read=new ModernWorldProtocol.Request(scope,ModernWorldProtocol.Action.READ,0,0,0,0,0,0,0,0,"0".repeat(64));
        rows.put("shared_world_read",HexFormat.of().formatHex(ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldRequest(3,read),47)));
        rows.put("shared_world_null_result",HexFormat.of().formatHex(ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(3,read,new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,null)),47)));
        Files.writeString(Path.of(args[0]),NativeTerrainProbeMain.json(rows));System.out.println("[native-wire-compatibility] exact legacy v47 vectors emitted");
    }
}
