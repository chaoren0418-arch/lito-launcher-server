package local.lito381.server;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit dependency identity candidate. No image decoding or renderer claim. */
final class ModernNativeActorWorldProtocol {
    static final int REQUEST_KIND=92,RESULT_KIND=93,REQUEST_BYTES=348,IDENTITY_BYTES=952,MIN_RESULT_BYTES=428;
    static final String ZERO="0".repeat(64);
    static final class Identity {
        private final byte[] wire;
        Identity(byte[] bytes) {
            if(bytes==null||bytes.length!=IDENTITY_BYTES)throw bad();wire=bytes.clone();var b=ByteBuffer.wrap(wire);
            if(b.getShort()!=1||b.getShort()!=4||b.getShort()!=1||b.getShort()!=0||b.getInt()!=0||b.getInt()!=3225
                    ||b.get()!=0||b.get()!=0||b.getShort()!=3||b.getInt()!=1)throw bad();
            for(int i=0;i<3;i++)nonzero(b);ModernNativeActorWorldProtocol.catalog(b);
            long textIndex=pin(b,4,4*1024*1024),textPak=pin(b,1,256L*1024*1024);
            range(b,textPak,8*1024*1024);nonzero(b);long decoded=b.getLong();if(decoded<1||decoded>8*1024*1024)throw bad();nonzero(b);
            pin(b,4,4*1024*1024);long spritePak=pin(b,1,2L*1024*1024*1024);
            for(int shard:new int[]{4,5,12,13}){if(Short.toUnsignedInt(b.getShort())!=shard||b.getShort()!=0)throw bad();long length=b.getLong();if(length<1||length>2L*1024*1024*1024)throw bad();pin(b,4,4*1024*1024);}
            for(int image:new int[]{0,1,8,9}){int shard=("3225-"+image+".spr").chars().sum()%16;if(Short.toUnsignedInt(b.getShort())!=image||Byte.toUnsignedInt(b.get())!=shard||b.get()!=0)throw bad();range(b,spritePak,1024*1024);nonzero(b);}if(b.hasRemaining())throw bad();
        }
        byte[] bytes(){return wire.clone();}
        String digest(){return hash("LITO-NATIVE-ACTOR-DEPENDENCY-IDENTITY\0",wire);}
        ModernNativeCatalogWorldProtocol.Catalog catalog(){return ModernNativeActorWorldProtocol.catalog(ByteBuffer.wrap(wire,120,168).slice());}
        public boolean equals(Object other){return other instanceof Identity i&&Arrays.equals(wire,i.wire);}
        public int hashCode(){return Arrays.hashCode(wire);}
    }
    record Request(String expectedIdentity,String actorProof,ModernNativeCatalogWorldProtocol.Request catalogRequest) {
        Request {expectedIdentity=validHash(expectedIdentity);actorProof=validHash(actorProof);Objects.requireNonNull(catalogRequest);var q=catalogRequest.nativeRequest().world();if(ZERO.equals(expectedIdentity)&&(q.action()!=ModernBattleWorldProtocol.Action.READ||q.matchId()!=0)||(q.action()==ModernBattleWorldProtocol.Action.LOADED)==ZERO.equals(actorProof))throw bad();}
        int requestId(){return catalogRequest.requestId();}
    }
    record Result(Request echo,Identity identity,ModernNativeCatalogWorldProtocol.Result catalogResult) {
        Result {Objects.requireNonNull(echo);Objects.requireNonNull(catalogResult);if(!echo.catalogRequest().equals(catalogResult.echo())||(identity==null)!=(catalogResult.catalog()==null)||identity!=null&&(!identity.catalog().equals(catalogResult.catalog())||!ZERO.equals(echo.expectedIdentity())&&!identity.digest().equals(echo.expectedIdentity())))throw bad();}
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated peer){return ModernSessionExtensions.allowsNativeActor(peer)&&ModernNativeCatalogWorldProtocol.negotiated(peer);}
    static String loadProof(Identity identity,ModernNativeCanonicalWorldProtocol.SourceAttachment source,ModernBattleWorldProtocol.Request q){return hash("LITO-NATIVE-ACTOR-DEPENDENCY-LOAD-PROOF\0",ByteBuffer.allocate(64).put(hex(identity.digest())).put(hex(ModernNativeCatalogWorldProtocol.loadProof(identity.catalog(),source,q))).array());}
    static void authorize(Identity identity,ModernNativeCanonicalWorldProtocol.SourceAttachment source,Request r,ModernSessionBoundary.Negotiated peer){if(!negotiated(peer)||identity==null||!ZERO.equals(r.expectedIdentity())&&!identity.digest().equals(r.expectedIdentity()))throw new SecurityException("Native actor dependencies differ");ModernNativeCatalogWorldProtocol.authorize(identity.catalog(),source,r.catalogRequest(),peer);if(r.catalogRequest().nativeRequest().world().action()==ModernBattleWorldProtocol.Action.LOADED&&!loadProof(identity,source,r.catalogRequest().nativeRequest().world()).equals(r.actorProof()))throw new SecurityException("Native actor dependency proof differs");}
    static int maximumResult(ModernSessionBoundary.Negotiated peer){if(!negotiated(peer))throw bad();return 92+IDENTITY_BYTES+ModernNativeCatalogWorldProtocol.maximumResult(peer);}
    static byte[] encodeRequest(Request r,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {require(peer);var b=header(REQUEST_BYTES,r.requestId(),REQUEST_KIND);echo(b,r);b.put(ModernNativeCatalogWorldProtocol.encodeRequest(r.catalogRequest(),peer));return b.array();}
    static Request decodeRequest(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {require(peer);try{var b=ModernWorldResourceProtocol.checkedHeader(bytes,REQUEST_KIND,REQUEST_BYTES,REQUEST_BYTES);int id=b.getInt(8);if(b.getShort()!=1||b.getShort()!=4)throw bad();String expected=getHash(b),proof=getHash(b);byte[] nested=new byte[264];b.get(nested);var r=ModernNativeCatalogWorldProtocol.decodeRequest(nested,peer);if(r.requestId()!=id||b.hasRemaining())throw bad();return new Request(expected,proof,r);}catch(RuntimeException failure){throw new ModernApplicationProtocol.ProtocolException();}}
    static byte[] encodeResult(Result r,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {require(peer);byte[] nested=ModernNativeCatalogWorldProtocol.encodeResult(r.catalogResult(),peer);int size=r.identity()==null?0:IDENTITY_BYTES;var b=header(92+size+nested.length,r.echo().requestId(),RESULT_KIND);echo(b,r.echo());b.putInt(size).putInt(0);if(size>0)b.put(r.identity().bytes());b.put(nested);return b.array();}
    static Result decodeResultWithoutData(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException {require(peer);try{var b=ModernWorldResourceProtocol.checkedHeader(bytes,RESULT_KIND,MIN_RESULT_BYTES,maximumResult(peer));int id=b.getInt(8);if(b.getShort()!=1||b.getShort()!=4)throw bad();String expected=getHash(b),proof=getHash(b);int size=b.getInt();if(b.getInt()!=0||size!=0&&size!=IDENTITY_BYTES||b.remaining()<size+ModernNativeCatalogWorldProtocol.MIN_RESULT_BYTES)throw bad();Identity identity=null;if(size>0){byte[] raw=new byte[size];b.get(raw);identity=new Identity(raw);}byte[] nested=new byte[b.remaining()];b.get(nested);var actual=ModernNativeCatalogWorldProtocol.decodeResultWithoutData(nested,peer);if(actual.echo().requestId()!=id)throw bad();return new Result(new Request(expected,proof,actual.echo()),identity,actual);}catch(RuntimeException failure){throw new ModernApplicationProtocol.ProtocolException();}}
    /** Validate selected index MISS then unique base HIT; shard PAKs aren't read. */
    static void verifyMembers(Path root,Identity identity)throws Exception {
        byte[] raw=identity.bytes();var b=ByteBuffer.wrap(raw);byte[] sprite=index(root.resolve("Sprite.idx"),Arrays.copyOfRange(raw,456,496));long pak=b.getLong(496);
        byte[] evidence=readBounded(root.getParent().resolve("provenance/source-verification.json"),512*1024);if(!Arrays.equals(hex(hash("",evidence)),Arrays.copyOfRange(raw,56,88)))throw new SecurityException("Actor source provenance differs");
        index(root.resolve("Text.idx"),Arrays.copyOfRange(raw,288,328));byte[] decodedIndex=readBounded(root.resolve("decoded-Text.idx"),4*1024*1024);
        if(!new String(evidence,StandardCharsets.UTF_8).contains("\"decoded_index_sha256\": \""+hash("",decodedIndex)+"\""))throw new SecurityException("Decoded Text index provenance differs");
        long[] text=member(decodedIndex,b.getLong(328),"list.spr");if(text==null||text[0]!=b.getLong(368)||text[1]!=b.getLong(376))throw new SecurityException("Text member selection differs");
        byte[] packed=readBounded(root.resolve("packed-list.spr"),8*1024*1024),decoded=readBounded(root.resolve("decoded-list.spr"),8*1024*1024);
        if(packed.length!=text[1]||decoded.length!=b.getLong(416)||!Arrays.equals(hex(hash("",packed)),Arrays.copyOfRange(raw,384,416))||!Arrays.equals(hex(hash("",decoded)),Arrays.copyOfRange(raw,424,456)))throw new SecurityException("Text catalog member differs");
        for(int i=0;i<4;i++){int at=744+i*52;int image=Short.toUnsignedInt(b.getShort(at)),shard=Byte.toUnsignedInt(b.get(at+2));String name="3225-"+image+".spr";int role=Arrays.binarySearch(new int[]{4,5,12,13},shard),missAt=536+52*role;long missPak=b.getLong(missAt+4);byte[] miss=index(root.resolve(String.format(Locale.ROOT,"Sprite%02d.idx",shard)),Arrays.copyOfRange(raw,missAt+12,missAt+52));if(member(miss,missPak,name)!=null)throw new SecurityException("Selected shard override replaces base route");long[] found=member(sprite,pak,name);if(found==null||found[0]!=b.getLong(at+4)||found[1]!=b.getLong(at+12))throw new SecurityException("Base member range differs");byte[] selected=readBounded(root.resolve(name),1024*1024);if(selected.length!=found[1]||!Arrays.equals(hex(hash("",selected)),Arrays.copyOfRange(raw,at+20,at+52)))throw new SecurityException("Base member bytes differ");}
    }
    private static byte[] index(Path file,byte[] pin)throws Exception{var b=ByteBuffer.wrap(pin);long size=b.getLong();byte[] hash=new byte[32];b.get(hash);byte[] bytes=readBounded(file,4*1024*1024);if(bytes.length!=size||!Arrays.equals(hex(hash("",bytes)),hash))throw new SecurityException("Index identity differs");return bytes;}
    private static byte[] readBounded(Path file,int bound)throws Exception{long size=Files.size(file);if(size<1||size>bound)throw new SecurityException("Resource input overbound");try(var in=Files.newInputStream(file)){byte[] bytes=in.readNBytes((int)size+1);if(bytes.length!=size)throw new SecurityException("Resource input changed");return bytes;}}
    private static long[] member(byte[] index,long pak,String selected){var b=ByteBuffer.wrap(index).order(ByteOrder.LITTLE_ENDIAN);if(index.length<4)throw bad();long count=Integer.toUnsignedLong(b.getInt());if(count<1||count>250000||index.length!=4+count*28)throw bad();long[] found=null;for(int i=0;i<count;i++){long off=Integer.toUnsignedLong(b.getInt());byte[] name=new byte[20];b.get(name);long size=Integer.toUnsignedLong(b.getInt());int end=0;while(end<20&&name[end]!=0)end++;if(end==0||size>64*1024*1024||off>pak-size)throw bad();for(int j=end;j<20;j++)if(name[j]!=0)throw bad();if(end==selected.length()){boolean equal=true;for(int j=0;j<end;j++)if(Character.toLowerCase((char)(name[j]&255))!=selected.charAt(j)){equal=false;break;}if(equal){if(found!=null)throw new SecurityException("Duplicate selected member");found=new long[]{off,size};}}}return found;}
    private static ModernNativeCatalogWorldProtocol.Catalog catalog(ByteBuffer b){if(b.getShort()!=1||b.getShort()!=3)throw bad();long idx=b.getLong();String idxSha=getHash(b);long pak=b.getLong();String pakSha=getHash(b);if(b.getShort()!=1||b.getShort()!=0)throw bad();return new ModernNativeCatalogWorldProtocol.Catalog(idx,idxSha,pak,pakSha,b.getLong(),b.getLong(),getHash(b),getHash(b));}
    private static long pin(ByteBuffer b,long min,long max){long n=b.getLong();if(n<min||n>max)throw bad();nonzero(b);return n;}
    private static void range(ByteBuffer b,long pak,long bound){long off=b.getLong(),size=b.getLong();if(off<0||size<1||size>bound||off>pak-size)throw bad();}
    private static void nonzero(ByteBuffer b){if(ZERO.equals(getHash(b)))throw bad();}
    static String hash(String prefix,byte[] bytes){try{var h=MessageDigest.getInstance("SHA-256");h.update(prefix.getBytes(StandardCharsets.US_ASCII));return HexFormat.of().formatHex(h.digest(bytes));}catch(Exception e){throw new AssertionError(e);}}
    private static String validHash(String s){if(s==null||!s.matches("[0-9a-fA-F]{64}"))throw bad();return s.toLowerCase(Locale.ROOT);}
    private static byte[] hex(String s){return HexFormat.of().parseHex(s);}
    private static String getHash(ByteBuffer b){byte[] out=new byte[32];b.get(out);return HexFormat.of().formatHex(out);}
    private static ByteBuffer header(int n,int id,int kind){return ByteBuffer.allocate(n).putInt(0x4c544d31).putInt(n).putInt(id).putShort((short)kind).putShort((short)0);}
    private static void echo(ByteBuffer b,Request r){b.putShort((short)1).putShort((short)4).put(hex(r.expectedIdentity())).put(hex(r.actorProof()));}
    private static void require(ModernSessionBoundary.Negotiated peer)throws ModernApplicationProtocol.ProtocolException{if(!negotiated(peer))throw new ModernApplicationProtocol.ProtocolException();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("Invalid Native actor dependency identity");}
}
