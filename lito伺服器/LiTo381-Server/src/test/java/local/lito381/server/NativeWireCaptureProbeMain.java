package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;
public final class NativeWireCaptureProbeMain {
    public static void main(String[] args)throws Exception {
        String zero="0".repeat(64),pin="1".repeat(64);var scope=new ModernRooms.Scope(7,10275,Long.MIN_VALUE,0);
        int caps=ModernSessionExtensions.NATIVE_DEPENDENCIES|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE;
        var peer=new ModernSessionBoundary.Negotiated(1,47,1,caps,768);
        var first=new ModernNativeCanonicalWorldProtocol.Request(12,2,zero,zero,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.READ,0,0,0,0,zero));
        var later=new ModernNativeCanonicalWorldProtocol.Request(12,2,pin,zero,new ModernBattleWorldProtocol.Request(scope,ModernBattleWorldProtocol.Action.READ,1,0,1,5,zero));
        byte[] a=ModernNativeCatalogWorldProtocol.encodeRequest(new ModernNativeCatalogWorldProtocol.Request(zero,zero,first),peer),b=ModernNativeCatalogWorldProtocol.encodeRequest(new ModernNativeCatalogWorldProtocol.Request(pin,zero,later),peer);
        var old=new LinkedHashMap<String,String>();String oldKey="7_READ_12_request90_hex";old.put(oldKey,HexFormat.of().formatHex(a));String overwritten=old.put(oldKey,HexFormat.of().formatHex(b));require(old.size()==1&&overwritten.equals(HexFormat.of().formatHex(a)),"old exporter drops first event");
        var capture=new NativeWireCapture();long firstSession=capture.session(),nextSession=capture.session();capture.append(firstSession,"SYNTHETIC_MAIN_DISCOVERY_NOT_TRANSMITTED",scope,"READ",a,null,"NOT_TRANSMITTED_MINIMAL_REPRO");capture.append(nextSession,"SYNTHETIC_NEGATIVE_LOADING_NOT_TRANSMITTED",scope,"READ",b,null,"NOT_TRANSMITTED_MINIMAL_REPRO");require(capture.count()==2,"new recorder keeps both reset request IDs");capture.write(Path.of(args[0]),"collision-minimal-repro");Files.writeString(Path.of(args[0]).resolve("collision-minimal-result.json"),NativeTerrainProbeMain.json(Map.of("state","PASSED_MINIMAL_KEY_COLLISION_REPRO","old_map_events",1,"new_capture_events",2,"same_source_action_request_id",true,"different_client_sessions",true,"synthetic_not_original_capture",true,"lost_historical_bytes_reconstructed",false)));System.out.println("Minimal collision reproduced; append-only capture retains both events");
    }
}
