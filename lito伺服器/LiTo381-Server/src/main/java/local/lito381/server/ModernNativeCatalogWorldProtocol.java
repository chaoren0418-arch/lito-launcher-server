package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Opt-in Native catalog envelope. Existing 88/89 and DATA bytes stay intact. */
final class ModernNativeCatalogWorldProtocol {
    static final int REQUEST_KIND=90,RESULT_KIND=91,REQUEST_BYTES=264,CATALOG_BYTES=168,MIN_RESULT_BYTES=336;
    static final String ZERO="0".repeat(64);
    /** Profile1 is the traced protected-old28 / per-entry L1 LTlist.spr route.
     * The decoded member pin is provenance, not renderer completion. */
    record Catalog(long indexBytes,String indexSha,long pakBytes,String pakSha,
                   long memberOffset,long memberBytes,String packedSha,String decodedSha) {
        Catalog {
            indexSha=hash(indexSha);pakSha=hash(pakSha);packedSha=hash(packedSha);decodedSha=hash(decodedSha);
            if(indexBytes<32||indexBytes>8*1024*1024||pakBytes<1||pakBytes>256L*1024*1024
                    ||memberOffset<0||memberBytes<1||memberBytes>1024*1024||memberOffset>pakBytes-memberBytes)throw bad();
        }
        byte[] bytes(){return ByteBuffer.allocate(CATALOG_BYTES).putShort((short)1).putShort((short)3)
                .putLong(indexBytes).put(hex(indexSha)).putLong(pakBytes).put(hex(pakSha))
                .putShort((short)1).putShort((short)0).putLong(memberOffset).putLong(memberBytes).put(hex(packedSha)).put(hex(decodedSha)).array();}
        String digest(){return ModernNativeCatalogWorldProtocol.digest("LITO-NATIVE-CATALOG-IDENTITY\0",bytes());}
    }
    record Request(String expectedCatalog,String catalogProof,ModernNativeCanonicalWorldProtocol.Request nativeRequest) {
        Request {
            expectedCatalog=hashOrZero(expectedCatalog);catalogProof=hashOrZero(catalogProof);Objects.requireNonNull(nativeRequest);
            var q=nativeRequest.world();
            if(ZERO.equals(expectedCatalog)&&(q.action()!=ModernBattleWorldProtocol.Action.READ||q.matchId()!=0)
                    ||(q.action()==ModernBattleWorldProtocol.Action.LOADED)==ZERO.equals(catalogProof))throw bad();
        }
        int requestId(){return nativeRequest.requestId();}
    }
    record Result(Request echo,Catalog catalog,ModernNativeCanonicalWorldProtocol.Result nativeResult) {
        Result {
            Objects.requireNonNull(echo);Objects.requireNonNull(nativeResult);
            if(!echo.nativeRequest().equals(nativeResult.echo())||(catalog==null)!=(nativeResult.source()==null)
                    ||catalog!=null&&!ZERO.equals(echo.expectedCatalog())&&!catalog.digest().equals(echo.expectedCatalog()))throw bad();
        }
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated peer) {
        return ModernSessionExtensions.allowsNativeCatalog(peer)&&ModernNativeCanonicalWorldProtocol.negotiated(peer);
    }
    static int maximumResult(ModernSessionBoundary.Negotiated peer){if(!negotiated(peer))throw bad();return 260+ModernNativeCanonicalWorldProtocol.maximumResult(peer);}
    static String loadProof(Catalog catalog,ModernNativeCanonicalWorldProtocol.SourceAttachment source,ModernBattleWorldProtocol.Request request) {
        return digest("LITO-NATIVE-CATALOG-LOAD-PROOF\0",ByteBuffer.allocate(64).put(hex(catalog.digest())).put(hex(ModernNativeCanonicalWorldProtocol.loadProof(source,request))).array());
    }
    static void authorize(Catalog catalog,ModernNativeCanonicalWorldProtocol.SourceAttachment source,Request request,ModernSessionBoundary.Negotiated peer) {
        if(!negotiated(peer)||catalog==null||!ZERO.equals(request.expectedCatalog())&&!catalog.digest().equals(request.expectedCatalog()))throw new SecurityException("Native catalog identity differs");
        ModernNativeCanonicalWorldProtocol.authorize(source,request.nativeRequest(),peer);
        if(request.nativeRequest().world().action()==ModernBattleWorldProtocol.Action.LOADED
                &&!loadProof(catalog,source,request.nativeRequest().world()).equals(request.catalogProof()))throw new SecurityException("Native catalog LOAD proof differs");
    }
    static byte[] encodeRequest(Request request,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);byte[] nested=ModernNativeCanonicalWorldProtocol.encodeRequest(request.nativeRequest(),peer);
        var out=header(REQUEST_BYTES,request.requestId(),REQUEST_KIND);resource(out,request);out.put(nested);return out.array();
    }
    static Request decodeRequest(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);try {
            var in=ModernWorldResourceProtocol.checkedHeader(bytes,REQUEST_KIND,REQUEST_BYTES,REQUEST_BYTES);int id=in.getInt(8);
            if(in.getShort()!=1||in.getShort()!=2)fail();String expected=getHash(in),proof=getHash(in);
            byte[] nested=new byte[ModernNativeCanonicalWorldProtocol.REQUEST_BYTES];in.get(nested);var nativeRequest=ModernNativeCanonicalWorldProtocol.decodeRequest(nested,peer);
            if(nativeRequest.requestId()!=id||in.hasRemaining())fail();return new Request(expected,proof,nativeRequest);
        }catch(ModernApplicationProtocol.ProtocolException failure){throw failure;}catch(RuntimeException failure){throw new ModernApplicationProtocol.ProtocolException();}
    }
    static byte[] encodeResult(Result result,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);byte[] nested=ModernNativeCanonicalWorldProtocol.encodeResult(result.nativeResult(),peer);int size=result.catalog()==null?0:CATALOG_BYTES;
        var out=header(92+size+nested.length,result.echo().requestId(),RESULT_KIND);resource(out,result.echo());out.putInt(size).putInt(0);if(size!=0)out.put(result.catalog().bytes());out.put(nested);return out.array();
    }
    /** Diagnostic pre-engine consumer only; canonical DATA stays consumer-owned. */
    static Result decodeResultWithoutData(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);try {
            var in=ModernWorldResourceProtocol.checkedHeader(bytes,RESULT_KIND,MIN_RESULT_BYTES,maximumResult(peer));int id=in.getInt(8);
            if(in.getShort()!=1||in.getShort()!=2)fail();String expected=getHash(in),proof=getHash(in);int size=in.getInt();if(in.getInt()!=0||size!=0&&size!=CATALOG_BYTES||in.remaining()<size+ModernNativeCanonicalWorldProtocol.MIN_RESULT_BYTES)fail();
            Catalog catalog=null;if(size!=0){if(in.getShort()!=1||in.getShort()!=3)fail();long index=in.getLong();String indexSha=getHash(in);long pak=in.getLong();String pakSha=getHash(in);if(in.getShort()!=1||in.getShort()!=0)fail();catalog=new Catalog(index,indexSha,pak,pakSha,in.getLong(),in.getLong(),getHash(in),getHash(in));}
            var nested=new byte[in.remaining()];in.get(nested);var nativeResult=ModernNativeCanonicalWorldProtocol.decodeResultWithoutData(nested,peer);if(nativeResult.echo().requestId()!=id)fail();return new Result(new Request(expected,proof,nativeResult.echo()),catalog,nativeResult);
        }catch(ModernApplicationProtocol.ProtocolException failure){throw failure;}catch(RuntimeException failure){throw new ModernApplicationProtocol.ProtocolException();}
    }
    private static void resource(ByteBuffer out,Request r){out.putShort((short)1).putShort((short)2).put(hex(r.expectedCatalog())).put(hex(r.catalogProof()));}
    private static ByteBuffer header(int size,int id,int kind){return ByteBuffer.allocate(size).putInt(0x4c544d31).putInt(size).putInt(id).putShort((short)kind).putShort((short)0);}
    private static String hash(String s){s=hashOrZero(s);if(ZERO.equals(s))throw bad();return s;}
    private static String hashOrZero(String s){if(s==null||!s.matches("[0-9a-fA-F]{64}"))throw bad();return s.toLowerCase(Locale.ROOT);}
    private static byte[] hex(String s){return HexFormat.of().parseHex(s);}
    private static String getHash(ByteBuffer b){byte[] bytes=new byte[32];b.get(bytes);return HexFormat.of().formatHex(bytes);}
    private static String digest(String prefix,byte[] bytes){try{var hash=MessageDigest.getInstance("SHA-256");hash.update(prefix.getBytes(StandardCharsets.US_ASCII));return HexFormat.of().formatHex(hash.digest(bytes));}catch(Exception impossible){throw new AssertionError(impossible);}}
    private static void require(ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {if(!negotiated(peer))fail();}
    private static void fail()throws ModernApplicationProtocol.ProtocolException {throw new ModernApplicationProtocol.ProtocolException();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid Native catalog envelope");}
    private ModernNativeCatalogWorldProtocol(){}
}
