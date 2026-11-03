package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Explicit Native resource proof around unmodified v47 navigation bodies. */
final class ModernWorldResourceProtocol {
    static final int SCHEMA=1,NAMESPACE=2,REQUEST_KIND=86,RESULT_KIND=87,REQUEST_BYTES=156,PLAN_BYTES=252,RESULT_PREFIX_BYTES=288;
    static final int MAX_RESULT_BYTES=ModernWorldProtocol.MAX_FRAME_BYTES+RESULT_PREFIX_BYTES;
    static final String ZERO="0".repeat(64);
    record ArchivePin(int role,int profile,long bytes,String sha256) {
        ArchivePin {if(role<1||role>4||profile!=1||bytes<1||bytes>(role%2==1?16L:512L)*1024*1024||!hash(sha256)||ZERO.equals(sha256))throw invalid();sha256=sha256.toLowerCase(Locale.ROOT);}
    }
    record Plan(int namespace,int mapId,String geometryDigest,List<ArchivePin> archives) {
        Plan {
            if(namespace!=NAMESPACE||mapId<0||!hash(geometryDigest)||ZERO.equals(geometryDigest)||archives==null||archives.size()!=4)throw invalid();
            archives=List.copyOf(archives);for(int i=0;i<4;i++)if(archives.get(i).role()!=i+1)throw invalid();geometryDigest=geometryDigest.toLowerCase(Locale.ROOT);
        }
        String digest(){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identityBytes(this)));}catch(Exception impossible){throw new AssertionError(impossible);}}
    }
    record Request(int requestId,int namespace,String expectedDigest,ModernWorldProtocol.Request world) {
        Request {
            if(requestId==0||namespace!=NAMESPACE||!hash(expectedDigest)||world==null||ZERO.equals(expectedDigest)
                    &&(world.action()!=ModernWorldProtocol.Action.READ||world.matchId()!=0))throw invalid();expectedDigest=expectedDigest.toLowerCase(Locale.ROOT);
        }
    }
    record Result(Request echo,Plan plan,ModernWorldProtocol.Reply reply) {
        Result {
            if(echo==null||plan==null||reply==null||echo.namespace()!=plan.namespace()
                    ||!ZERO.equals(echo.expectedDigest())&&!echo.expectedDigest().equals(plan.digest()))throw invalid();
            var f=reply.frame();if(f!=null&&(f.manifest().mapId()!=plan.mapId()||!f.manifest().digest().equals(plan.geometryDigest())))throw invalid();
        }
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated n){return ModernSessionExtensions.allowsNativeResources(n);}
    static byte[] encodeRequest(Request r,ModernSessionBoundary.Negotiated n)throws ModernApplicationProtocol.ProtocolException {
        require(n);var out=header(REQUEST_BYTES,r.requestId(),REQUEST_KIND);putEcho(out,r);ModernWorldProtocol.putRequest(out,r.world());return out.array();
    }
    static Request decodeRequest(byte[] bytes,ModernSessionBoundary.Negotiated n)throws ModernApplicationProtocol.ProtocolException {
        require(n);try{var in=checkedHeader(bytes,REQUEST_KIND,REQUEST_BYTES,REQUEST_BYTES);int id=in.getInt(8);checkSchema(in);int ns=u16(in);String digest=getHash(in);var r=new Request(id,ns,digest,ModernWorldProtocol.getRequest(in));if(in.hasRemaining())fail();return r;}
        catch(ModernApplicationProtocol.ProtocolException e){throw e;}catch(RuntimeException e){throw new ModernApplicationProtocol.ProtocolException();}
    }
    static byte[] encodeResult(Result r,ModernSessionBoundary.Negotiated n)throws ModernApplicationProtocol.ProtocolException {
        require(n);var legacy=ModernApplicationProtocol.encode(new ModernApplicationProtocol.WorldResult(r.echo().requestId(),r.echo().world(),r.reply()),47);
        int size=legacy.length+RESULT_PREFIX_BYTES;if(size>MAX_RESULT_BYTES)fail();var out=header(size,r.echo().requestId(),RESULT_KIND);
        putEcho(out,r.echo());putPlan(out,r.plan());out.put(legacy,16,legacy.length-16);return out.array();
    }
    static Result decodeResult(byte[] bytes,ModernSessionBoundary.Negotiated n)throws ModernApplicationProtocol.ProtocolException {
        require(n);try{var in=checkedHeader(bytes,RESULT_KIND,16+RESULT_PREFIX_BYTES+108,MAX_RESULT_BYTES);int id=in.getInt(8);
            checkSchema(in);int ns=u16(in);String expected=getHash(in);Plan p=getPlan(in);var body=new byte[16+in.remaining()];
            var legacy=ByteBuffer.wrap(body);legacy.putInt(0x4c544d31).putInt(body.length).putInt(id).putShort((short)21).putShort((short)0);legacy.put(in);
            var r=(ModernApplicationProtocol.WorldResult)ModernApplicationProtocol.decode(body,47);
            return new Result(new Request(id,ns,expected,r.echo()),p,r.reply());
        }catch(ModernApplicationProtocol.ProtocolException e){throw e;}catch(RuntimeException e){throw new ModernApplicationProtocol.ProtocolException();}
    }
    static void authorize(Plan p,Request r,ModernSessionBoundary.Negotiated n) {
        if(!negotiated(n)||p==null||r.namespace()!=p.namespace()||!ZERO.equals(r.expectedDigest())&&!p.digest().equals(r.expectedDigest()))throw new SecurityException("Native resource namespace/proof not negotiated or mismatched");
        if(r.world().action()==ModernWorldProtocol.Action.LOADED&&!r.world().manifestDigest().equals(p.geometryDigest()))throw new SecurityException("Native canonical geometry proof mismatch");
    }
    static byte[] identityBytes(Plan p) {
        var prefix="LITO-WORLD-RESOURCES\0".getBytes(StandardCharsets.US_ASCII);var out=ByteBuffer.allocate(prefix.length+44+4*44);
        out.put(prefix).putShort((short)SCHEMA).putShort((short)p.namespace()).putInt(p.mapId()).put(HexFormat.of().parseHex(p.geometryDigest())).putShort((short)4).putShort((short)0);
        for(var a:p.archives())putArchive(out,a);return out.array();
    }
    private static void putPlan(ByteBuffer b,Plan p){b.putShort((short)SCHEMA).putShort((short)p.namespace()).put(HexFormat.of().parseHex(p.digest())).putInt(p.mapId()).put(HexFormat.of().parseHex(p.geometryDigest())).putShort((short)4).putShort((short)0);for(var a:p.archives())putArchive(b,a);}
    private static Plan getPlan(ByteBuffer b)throws ModernApplicationProtocol.ProtocolException {
        checkSchema(b);int ns=u16(b);String expected=getHash(b);int map=b.getInt();String geometry=getHash(b);if(u16(b)!=4||u16(b)!=0)fail();
        var archives=new ArrayList<ArchivePin>();for(int i=0;i<4;i++)archives.add(new ArchivePin(u16(b),u16(b),b.getLong(),getHash(b)));
        var p=new Plan(ns,map,geometry,archives);if(!p.digest().equals(expected))fail();return p;
    }
    private static void putArchive(ByteBuffer b,ArchivePin a){b.putShort((short)a.role()).putShort((short)a.profile()).putLong(a.bytes()).put(HexFormat.of().parseHex(a.sha256()));}
    private static void putEcho(ByteBuffer b,Request r){b.putShort((short)SCHEMA).putShort((short)r.namespace()).put(HexFormat.of().parseHex(r.expectedDigest()));}
    private static ByteBuffer header(int size,int id,int kind){return ByteBuffer.allocate(size).putInt(0x4c544d31).putInt(size).putInt(id).putShort((short)kind).putShort((short)0);}
    static ByteBuffer checkedHeader(byte[] bytes,int kind,int min,int max)throws ModernApplicationProtocol.ProtocolException {
        if(bytes==null||bytes.length<min||bytes.length>max)fail();var in=ByteBuffer.wrap(bytes);
        if(in.getInt()!=0x4c544d31||in.getInt()!=bytes.length||in.getInt()==0||u16(in)!=kind||u16(in)!=0)fail();return in;
    }
    private static void checkSchema(ByteBuffer b)throws ModernApplicationProtocol.ProtocolException{if(u16(b)!=SCHEMA)fail();}
    private static String getHash(ByteBuffer b){byte[] a=new byte[32];b.get(a);return HexFormat.of().formatHex(a);}
    private static int u16(ByteBuffer b){return Short.toUnsignedInt(b.getShort());}
    private static boolean hash(String s){return s!=null&&s.matches("[0-9a-fA-F]{64}");}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid Native resource wire contract");}
    private static void require(ModernSessionBoundary.Negotiated n)throws ModernApplicationProtocol.ProtocolException{if(!negotiated(n))fail();}
    private static void fail()throws ModernApplicationProtocol.ProtocolException{throw new ModernApplicationProtocol.ProtocolException();}
}
