package local.lito381.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Offline base-body inspection only. Always requires explicit persisted Class,
 * never supplies session authority, a game wire profile, or renderer readiness. */
final class NativeAppearanceDependencyResolver {
    static final int MAX_BYTES=128*1024;
    record Binding(int source,long object,long epoch,int channel,long revision,
                   long observedAt,long validUntil,String ownerHash,String catalogHash) {
        Binding {
            if(source<=0||object<=0||channel<0||revision<0||observedAt<0||validUntil< -1
                ||!isHash(ownerHash)||!isHash(catalogHash))throw new IllegalArgumentException("appearance binding");
        }
    }
    record Input(Binding binding,int characterType,int sex,Integer persistedClass,
                 long weaponObject,int retainedWeaponType,boolean effectiveWeaponEquipped) {}
    record Frame(int image,int subframe,int ticks,byte[] opaqueEvents) {
        Frame {opaqueEvents=opaqueEvents.clone();}
        @Override public byte[] opaqueEvents(){return opaqueEvents.clone();}
    }
    record Action(int id,boolean eightWay,int fps,List<Frame> frames) {
        Action {frames=List.copyOf(frames);}
    }
    record Row(int logical,int graphics,Map<Integer,Action> actions) {
        Row {actions=Map.copyOf(actions);}
    }
    record Member(String name,int shard,boolean baseArchive,long offset,long bytes,int frames,String sha256) {}
    record PlannedFrame(Member member,int subframe,int ticks,int fps,byte[] opaqueEvents) {
        PlannedFrame {opaqueEvents=opaqueEvents.clone();}
        @Override public byte[] opaqueEvents(){return opaqueEvents.clone();}
    }
    record Plan(Binding binding,int logical,int graphics,int action,int heading,List<PlannedFrame> frames) {
        Plan {frames=List.copyOf(frames);}
        boolean runtimeAppearanceQualified(){return false;}
        boolean rendererReady(){return false;}
        int gameWireProfile(){return 0;}
    }
    enum State {WALK,ATTACK,DAMAGE,IDLE,DEATH}
    private final boolean enabled;
    private final Map<Integer,Row> rows;
    private final Map<String,Member> members;
    NativeAppearanceDependencyResolver(byte[] bytes) throws Exception {this(bytes,false);}
    NativeAppearanceDependencyResolver(byte[] bytes,boolean inspectionEnabled) throws Exception {
        if(bytes.length<40||bytes.length>MAX_BYTES)throw new IOException("appearance dependency budget");
        byte[] body=Arrays.copyOf(bytes,bytes.length-32);
        var digest=MessageDigest.getInstance("SHA-256");digest.update("LITO-NATIVE-APPEARANCE-DEPENDENCIES\0".getBytes(StandardCharsets.US_ASCII));digest.update(body);
        if(!MessageDigest.isEqual(digest.digest(),Arrays.copyOfRange(bytes,bytes.length-32,bytes.length)))throw new IOException("appearance dependency digest");
        var in=new DataInputStream(new ByteArrayInputStream(body));
        if(in.readInt()!=0x4e414431||in.readUnsignedShort()!=1)throw new IOException("appearance dependency schema");
        int count=in.readUnsignedShort();if(count<1||count>16)throw new IOException("appearance row budget");
        var rr=new HashMap<Integer,Row>();
        for(int i=0;i<count;i++) {
            int logical=in.readInt(),gfx=in.readInt(),n=in.readUnsignedShort();
            if(logical<0||gfx<0||n<1||n>96)throw new IOException("appearance row");
            var actions=new HashMap<Integer,Action>();
            for(int j=0;j<n;j++) {
                int id=in.readUnsignedShort(),dir=in.readUnsignedByte(),fps=in.readUnsignedByte(),length=in.readUnsignedShort();
                if(id>=100||dir>1||fps<1||fps>120||length<1||length>128)throw new IOException("appearance action");
                var frames=new ArrayList<Frame>();
                for(int k=0;k<length;k++) {
                    int image=in.readInt(),sub=in.readInt(),ticks=in.readUnsignedShort(),ev=in.readUnsignedShort();
                    if(image<0||image>65528||sub<0||sub>127||ev>128)throw new IOException("appearance frame");
                    byte[] events=in.readNBytes(ev);if(events.length!=ev)throw new EOFException();
                    frames.add(new Frame(image,sub,ticks,events));
                }
                if(actions.put(id,new Action(id,dir==1,fps,frames))!=null)throw new IOException("duplicate action");
            }
            if(rr.put(logical,new Row(logical,gfx,actions))!=null)throw new IOException("duplicate logical sprite");
        }
        count=in.readUnsignedShort();if(count<1||count>1024)throw new IOException("appearance member budget");
        var mm=new HashMap<String,Member>();
        for(int i=0;i<count;i++) {
            int n=in.readUnsignedByte();if(n<1||n>20)throw new IOException("appearance member name");
            byte[] nameBytes=in.readNBytes(n);if(nameBytes.length!=n)throw new EOFException();
            String name=new String(nameBytes,StandardCharsets.US_ASCII);
            int shard=in.readUnsignedByte(),base=in.readUnsignedByte();long off=in.readLong(),size=in.readLong();int frames=in.readUnsignedShort();
            byte[] hash=in.readNBytes(32);
            if(!name.matches("[0-9]+-[0-9]+\\.spr")||shard>15||base>1||off<0||size<1||size>1048576||frames<1||frames>127||hash.length!=32
                ||name.chars().sum()%16!=shard)throw new IOException("appearance member");
            if(mm.put(name,new Member(name,shard,base==1,off,size,frames,HexFormat.of().formatHex(hash)))!=null)throw new IOException("duplicate member");
        }
        if(in.available()!=0)throw new IOException("appearance dependency trailing bytes");
        rows=Map.copyOf(rr);members=Map.copyOf(mm);enabled=inspectionEnabled;
        for(var row:rows.values())for(var a:row.actions().values())for(var f:a.frames())for(int h=0;h<(a.eightWay()?8:1);h++) {
            var m=members.get(row.graphics()+"-"+(f.image()+h)+".spr");
            if(m==null||f.subframe()>=m.frames())throw new IOException("appearance subframe dependency missing");
        }
    }
    Plan inspectBase(Input input,Binding expected,long at,State state,int heading) throws Exception {
        if(!enabled)throw new IOException("appearance dependency inspection disabled");
        Objects.requireNonNull(input);Objects.requireNonNull(expected);Objects.requireNonNull(state);
        Binding b=input.binding();
        if(!b.equals(expected))throw new SecurityException("appearance owner/epoch/revision/catalog differs");
        if(at<b.observedAt()||(b.validUntil()>=0&&at>=b.validUntil()))throw new IOException("appearance observation lifetime");
        if(heading<0||heading>7)throw new IOException("appearance heading");
        if(input.persistedClass()==null)throw new IOException("persisted SQL Class required");
        // Limited base study, with SQL Class independently observed. Type/sex
        // are consistency checks, not a substitute for that field or live gfx.
        int logical=input.persistedClass();
        if(!((input.characterType()==0&&input.sex()==0&&logical==0)
            ||(input.characterType()==3&&input.sex()==0&&logical==734)))throw new IOException("unqualified base appearance");
        int walk;
        if(input.effectiveWeaponEquipped()) {
            if(input.weaponObject()<=0)throw new IOException("effective weapon object required");
            walk=switch(input.retainedWeaponType()){case 2->46;case 3->50;default->throw new IOException("unqualified weapon stance");};
        } else {
            if(input.weaponObject()!=0||input.retainedWeaponType()!=0)throw new IOException("unequipped weapon must not select stance");
            walk=0;
        }
        int action=switch(state){case WALK->walk;case ATTACK->walk+1;case DAMAGE->walk+2;case IDLE->walk+3;case DEATH->8;};
        Row row=rows.get(logical);if(row==null)throw new IOException("appearance logical row absent");
        Action a=row.actions().get(action);if(a==null)throw new IOException("appearance action absent");
        var out=new ArrayList<PlannedFrame>();
        for(var f:a.frames()) {
            int image=Math.addExact(f.image(),a.eightWay()?heading:0);
            Member m=members.get(row.graphics()+"-"+image+".spr");
            if(m==null||f.subframe()>=m.frames())throw new IOException("appearance frame absent");
            out.add(new PlannedFrame(m,f.subframe(),f.ticks(),a.fps(),f.opaqueEvents()));
        }
        return new Plan(b,logical,row.graphics(),action,heading,out);
    }
    private static boolean isHash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
}
