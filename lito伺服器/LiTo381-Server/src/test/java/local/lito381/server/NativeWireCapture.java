package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;

/** Bounded append-only test evidence. Never records authentication payloads. */
final class NativeWireCapture {
    private static final int MAX_RECORDS=256,MAX_BYTES=12*1024*1024;
    private final List<Map<String,Object>> entries=new ArrayList<>();
    private final Map<String,Object> vectors=new LinkedHashMap<>();
    private long nextSession;private int totalBytes;
    synchronized long session(){return ++nextSession;}
    synchronized void append(long session,String phase,ModernRooms.Scope scope,String action,byte[] request,byte[] response,String outcome) {
        require(entries.size()<MAX_RECORDS&&request.length<=MAX_BYTES-totalBytes,"capture record/byte budget");int incoming=response==null?0:response.length;require(incoming<=MAX_BYTES-totalBytes-request.length,"capture response budget");totalBytes+=request.length+incoming;
        int sequence=entries.size()+1,kind=Short.toUnsignedInt(ByteBuffer.wrap(request).getShort(12)),id=ByteBuffer.wrap(request).getInt(8);
        String key=String.format(Locale.ROOT,"capture_%04d_session_%03d_source_%d_request_%d",sequence,session,scope.sourceId(),id);
        require(!vectors.containsKey(key+"_request"+kind+"_hex"),"unique capture key");
        var row=new LinkedHashMap<String,Object>();row.put("sequence",sequence);row.put("session",session);row.put("phase",phase);row.put("source_id",scope.sourceId());row.put("object_id",scope.objectId());row.put("epoch_u64_hex",String.format(Locale.ROOT,"%016x",scope.epoch()));row.put("channel",scope.channelId());row.put("request_id",id);row.put("request_kind",kind);row.put("action",action);row.put("outcome",outcome);row.put("key",key);row.put("request_hex",HexFormat.of().formatHex(request));vectors.put(key+"_request"+kind+"_hex",row.get("request_hex"));
        if(response!=null){int replyKind=Short.toUnsignedInt(ByteBuffer.wrap(response).getShort(12));require(ByteBuffer.wrap(response).getInt(8)==id,"captured echo request ID");row.put("response_kind",replyKind);row.put("response_hex",HexFormat.of().formatHex(response));vectors.put(key+"_result"+replyKind+"_hex",row.get("response_hex"));
            if(kind==90||kind==92){int q=kind==90?84:168;require(request.length>=q+180,"captured nested88");var requested=ByteBuffer.wrap(request);row.put("requested_world",Map.of("room",requested.getLong(q+116),"match",requested.getLong(q+132),"generation",requested.getLong(q+140)));row.put("wire_scope",Map.of("source",requested.getInt(q+84),"object",requested.getLong(q+88),"epoch_u64_hex",String.format(Locale.ROOT,"%016x",requested.getLong(q+96)),"channel",requested.getInt(q+104)));
                int nested=0;if(replyKind==93){int length=ByteBuffer.wrap(response).getInt(84);require(length==952,"capture actor identity length");nested=92+length;}require(Short.toUnsignedInt(ByteBuffer.wrap(response).getShort(nested+12))==91,"captured nested91");nested+=92+ByteBuffer.wrap(response).getInt(nested+84);byte[] inner=Arrays.copyOfRange(response,nested,response.length);require(Short.toUnsignedInt(ByteBuffer.wrap(inner).getShort(12))==89&&ByteBuffer.wrap(inner).getInt(8)==id,"captured nested89 ID/kind");row.put("nested89_hex",HexFormat.of().formatHex(inner));vectors.put(key+"_nested89_hex",row.get("nested89_hex"));int metadata=92+ByteBuffer.wrap(inner).getInt(84)+96;require(inner.length>=metadata+56,"capture89 metadata bound");row.put("observed_world",Map.of("room",ByteBuffer.wrap(inner).getLong(metadata+4),"match",ByteBuffer.wrap(inner).getLong(metadata+12),"generation",ByteBuffer.wrap(inner).getLong(metadata+20)));row.put("state",Byte.toUnsignedInt(inner[metadata+2]));
            }
        }
        entries.add(Collections.unmodifiableMap(row));
    }
    synchronized int count(){return entries.size();}
    synchronized void write(Path output,String stem)throws Exception {
        Files.writeString(output.resolve(stem+"-capture.json"),NativeTerrainProbeMain.json(Map.of("schema",1,"append_only",true,"max_records",MAX_RECORDS,"max_wire_bytes",MAX_BYTES,"wire_bytes",totalBytes,"session_count",nextSession,"entries",entries)));
        Files.writeString(output.resolve(stem+"-vectors.json"),NativeTerrainProbeMain.json(vectors));
    }
}
