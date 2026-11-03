package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/** Candidate codec only: wraps canonical66/67 DTOs, never a legacy WorldFrame. */
final class ModernNativeCanonicalWorldProtocol {
    static final int REQUEST_KIND=88,RESULT_KIND=89,REQUEST_BYTES=180,RESOURCE_BYTES=68;
    static final int MIN_RESULT_BYTES=244,MAX_ATTACHMENT_BYTES=2860;
    private static final int MAGIC=0x4c544d31,SCHEMA=1,NAMESPACE=2;
    private static final String ZERO="0".repeat(64);
    record Request(int requestId,int namespace,String expectedDigest,String loadProof,
                   ModernBattleWorldProtocol.Request world) {
        Request {
            if(requestId==0||namespace!=NAMESPACE||!hash(expectedDigest)||!hash(loadProof)||world==null)throw bad();
            expectedDigest=expectedDigest.toLowerCase(Locale.ROOT);loadProof=loadProof.toLowerCase(Locale.ROOT);
            if(ZERO.equals(expectedDigest)&&(world.action()!=ModernBattleWorldProtocol.Action.READ||world.matchId()!=0)
                    ||(world.action()==ModernBattleWorldProtocol.Action.LOADED)==ZERO.equals(loadProof))throw bad();
        }
    }
    record SourceAttachment(ModernWorldResourceProtocol.Plan plan,ModernWorldMap.Manifest manifest) {
        SourceAttachment {
            Objects.requireNonNull(plan);Objects.requireNonNull(manifest);
            if(plan.namespace()!=NAMESPACE||plan.mapId()!=manifest.mapId()||!plan.geometryDigest().equals(manifest.digest()))throw bad();
            for(var pin:plan.archives())if(pin.bytes()>(pin.role()%2==1?8L:256L)*1024*1024)throw bad();
            validateManifest(manifest);
        }
        int bytes(){return 300+40*manifest.sectors().size();}
    }
    record Result(Request echo,SourceAttachment source,ModernBattleWorldProtocol.Reply reply) {
        Result {
            Objects.requireNonNull(echo);Objects.requireNonNull(reply);
            ModernBattleWorldProtocol.validate(new ModernApplicationProtocol.BattleWorldResult(echo.requestId(),echo.world(),reply));
            if(source==null&&(reply.world()!=null||!ZERO.equals(echo.expectedDigest()))
                    ||source!=null&&!ZERO.equals(echo.expectedDigest())&&!echo.expectedDigest().equals(source.plan().digest()))throw bad();
            if(source!=null&&reply.view()!=null&&!source.manifest().equals(reply.view().world().manifest()))throw bad();
        }
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated peer){
        return ModernSessionExtensions.allowsNativeResources(peer)&&ModernBattleWorldProtocol.negotiated(peer);
    }
    static int maximumResult(ModernSessionBoundary.Negotiated peer){
        if(!negotiated(peer))throw bad();
        return Math.addExact(3104,ModernBattleWire.Encoding.selected(peer).maximum());
    }
    static byte[] encodeRequest(Request q,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);var canonical=ModernBattleWorldProtocol.encodeRequest(new ModernApplicationProtocol.BattleWorldRequest(q.requestId(),q.world()),peer.version());
        var out=header(REQUEST_BYTES,q.requestId(),REQUEST_KIND);putResource(out,q);out.put(canonical,16,96);return out.array();
    }
    static Request decodeRequest(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);try {
            var in=ModernWorldResourceProtocol.checkedHeader(bytes,REQUEST_KIND,REQUEST_BYTES,REQUEST_BYTES);int id=in.getInt(8);
            if(u16(in)!=SCHEMA)fail();int ns=u16(in);String expected=getHash(in),proof=getHash(in);
            var q=new Request(id,ns,expected,proof,ModernBattleWorldProtocol.getRequest(in));if(in.hasRemaining())fail();return q;
        }catch(ModernApplicationProtocol.ProtocolException failure){throw failure;}catch(RuntimeException failure){throw new ModernApplicationProtocol.ProtocolException();}
    }
    static byte[] encodeResult(Result r,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);if(r.source()!=null&&!ModernLargeMaps.supported(r.source().manifest(),peer))fail();
        var canonical=ModernBattleWorldProtocol.encodeResult(new ModernApplicationProtocol.BattleWorldResult(r.echo().requestId(),r.echo().world(),r.reply()),peer);
        int attachment=r.source()==null?0:r.source().bytes();int bytes=Math.addExact(canonical.length,76+attachment);
        if(bytes<MIN_RESULT_BYTES||bytes>maximumResult(peer))fail();
        var out=header(bytes,r.echo().requestId(),RESULT_KIND);putResource(out,r.echo());out.putInt(attachment).putInt(0);
        if(r.source()!=null)putSource(out,r.source());out.put(canonical,16,canonical.length-16);return out.array();
    }
    /** Offline diagnostic peer for pre-engine replies only. Server codecs are directional;
     * DATA decoding belongs to the consumer's existing canonical payload validator. */
    static Result decodeResultWithoutData(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {
        require(peer);try {
            var in=ModernWorldResourceProtocol.checkedHeader(bytes,RESULT_KIND,MIN_RESULT_BYTES,maximumResult(peer));int id=in.getInt(8);
            if(u16(in)!=SCHEMA)fail();int ns=u16(in);String expected=getHash(in),proof=getHash(in);int size=in.getInt();
            if(in.getInt()!=0||size<0||size>MAX_ATTACHMENT_BYTES||size>in.remaining()-152||size!=0&&size<340)fail();
            SourceAttachment source=null;
            if(size!=0){var slice=in.slice();slice.limit(size);source=getSource(slice);if(slice.hasRemaining()||!ModernLargeMaps.supported(source.manifest(),peer))fail();in.position(in.position()+size);}
            var q=ModernBattleWorldProtocol.getRequest(in);
            int status=Byte.toUnsignedInt(in.get()),reason=Byte.toUnsignedInt(in.get()),state=Byte.toUnsignedInt(in.get());
            if(in.get()!=0||status>=ModernWorldProtocol.Status.values().length||reason>ModernWorldProtocol.Reason.NOT_ACTIVE.ordinal()||state>1)fail();
            ModernTargetHistory.World world=null;ModernBattleLifecycle lifecycle=null;
            if(state==0){for(int i=0;i<48;i++)if(in.get()!=0)fail();}
            else {
                world=new ModernTargetHistory.World(in.getLong(),in.getLong(),in.getLong());long revision=in.getLong();
                int phase=Byte.toUnsignedInt(in.get()),abort=Byte.toUnsignedInt(in.get());
                if(phase>=ModernWorldProtocol.Phase.values().length||abort>=ModernWorldProtocol.AbortReason.values().length
                        ||in.get()!=0||in.get()!=0||in.getLong()!=0||in.getInt()!=0)fail();
                lifecycle=new ModernBattleLifecycle(revision,ModernWorldProtocol.Phase.values()[phase],ModernWorldProtocol.AbortReason.values()[abort],null,null,ModernPetRaceProtocol.Snapshot.disabled(),false,false);
            }
            if(in.getInt()!=0||in.hasRemaining())fail();
            var reply=new ModernBattleWorldProtocol.Reply(ModernWorldProtocol.Status.values()[status],ModernWorldProtocol.Reason.values()[reason],world,lifecycle,null);
            return new Result(new Request(id,ns,expected,proof,q),source,reply);
        }catch(ModernApplicationProtocol.ProtocolException failure){throw failure;}catch(RuntimeException failure){throw new ModernApplicationProtocol.ProtocolException();}
    }
    static void authorize(SourceAttachment source,Request request,ModernSessionBoundary.Negotiated peer){
        if(!negotiated(peer)||source==null||!ModernLargeMaps.supported(source.manifest(),peer)||request.namespace()!=source.plan().namespace()
                ||!ZERO.equals(request.expectedDigest())&&!source.plan().digest().equals(request.expectedDigest()))throw new SecurityException("Native canonical source proof differs");
        if(request.world().action()==ModernBattleWorldProtocol.Action.LOADED
                &&(!request.world().digest().equals(source.manifest().digest())||!request.loadProof().equals(loadProof(source,request.world()))))
            throw new SecurityException("Native canonical LOAD proof differs");
    }
    static String loadProof(SourceAttachment source,ModernBattleWorldProtocol.Request request){
        if(request.matchId()<1||request.generation()<1||request.roomId()<1)throw bad();
        byte[] prefix="LITO-NATIVE-LOAD-PROOF\0".getBytes(StandardCharsets.US_ASCII);var scope=request.scope();
        var out=ByteBuffer.allocate(prefix.length+112).put(prefix).put(HexFormat.of().parseHex(source.plan().digest())).put(HexFormat.of().parseHex(source.manifest().digest()));
        out.putInt(scope.sourceId()).putLong(scope.objectId()).putLong(scope.epoch()).putInt(scope.channelId());
        out.putLong(request.roomId()).putLong(request.matchId()).putLong(request.generation());
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(out.array()));}catch(Exception impossible){throw new AssertionError(impossible);}
    }
    private static void putResource(ByteBuffer out,Request q){out.putShort((short)SCHEMA).putShort((short)q.namespace()).put(HexFormat.of().parseHex(q.expectedDigest())).put(HexFormat.of().parseHex(q.loadProof()));}
    private static void putSource(ByteBuffer out,SourceAttachment source){
        var p=source.plan();var m=source.manifest();out.putShort((short)SCHEMA).putShort((short)NAMESPACE);
        out.putShort((short)SCHEMA).putShort((short)p.namespace()).put(HexFormat.of().parseHex(p.digest())).putInt(p.mapId()).put(HexFormat.of().parseHex(p.geometryDigest())).putShort((short)4).putShort((short)0);
        for(var pin:p.archives())out.putShort((short)pin.role()).putShort((short)pin.profile()).putLong(pin.bytes()).put(HexFormat.of().parseHex(pin.sha256()));
        out.putInt(40+40*m.sectors().size()).putShort((short)SCHEMA).putShort((short)m.sectors().size()).putInt(m.mapId()).put(HexFormat.of().parseHex(m.digest()));
        for(var sector:m.sectors())out.putShort((short)sector.key().x()).putShort((short)sector.key().y()).putInt(sector.byteLength()).put(HexFormat.of().parseHex(sector.rawSha256()));
    }
    private static SourceAttachment getSource(ByteBuffer in)throws ModernApplicationProtocol.ProtocolException {
        if(u16(in)!=SCHEMA||u16(in)!=NAMESPACE||u16(in)!=SCHEMA)fail();int ns=u16(in);String set=getHash(in);int map=in.getInt();String geometry=getHash(in);
        if(u16(in)!=4||u16(in)!=0)fail();var pins=new ArrayList<ModernWorldResourceProtocol.ArchivePin>();
        for(int i=0;i<4;i++)pins.add(new ModernWorldResourceProtocol.ArchivePin(u16(in),u16(in),in.getLong(),getHash(in)));
        var plan=new ModernWorldResourceProtocol.Plan(ns,map,geometry,pins);if(!plan.digest().equals(set))fail();
        int length=in.getInt();if(length!=in.remaining()||u16(in)!=SCHEMA)fail();int count=u16(in);
        if(count<1||count>64||length!=40+40*count)fail();int mapId=in.getInt();String canonical=getHash(in);var sectors=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int i=0;i<count;i++)sectors.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(u16(in),u16(in)),in.getInt(),getHash(in)));
        return new SourceAttachment(plan,new ModernWorldMap.Manifest(mapId,sectors,canonical));
    }
    private static void validateManifest(ModernWorldMap.Manifest manifest){
        if(manifest.mapId()<0||manifest.sectors().isEmpty()||manifest.sectors().size()>64||!hash(manifest.digest())||ZERO.equals(manifest.digest()))throw bad();
        int px=-1,py=-1;long total=0;
        for(var sector:manifest.sectors()){
            int x=sector.key().x(),y=sector.key().y();
            if(x<px||x==px&&y<=py||sector.byteLength()<49158||sector.byteLength()>8*1024*1024||!hash(sector.rawSha256())||ZERO.equals(sector.rawSha256()))throw bad();
            total+=sector.byteLength();if(total>32*1024*1024)throw bad();px=x;py=y;
        }
    }
    private static ByteBuffer header(int size,int id,int kind){return ByteBuffer.allocate(size).putInt(MAGIC).putInt(size).putInt(id).putShort((short)kind).putShort((short)0);}
    private static String getHash(ByteBuffer b){byte[] hash=new byte[32];b.get(hash);return HexFormat.of().formatHex(hash);}
    private static int u16(ByteBuffer b){return Short.toUnsignedInt(b.getShort());}
    private static boolean hash(String s){return s!=null&&s.matches("[0-9a-fA-F]{64}");}
    private static void require(ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException{if(!negotiated(peer))fail();}
    private static void fail()throws ModernApplicationProtocol.ProtocolException{throw new ModernApplicationProtocol.ProtocolException();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid Native canonical world codec");}
    private ModernNativeCanonicalWorldProtocol(){}
}
