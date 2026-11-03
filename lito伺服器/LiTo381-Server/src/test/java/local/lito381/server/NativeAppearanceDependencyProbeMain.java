package local.lito381.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static local.lito381.server.NativeAppearanceDependencyResolver.*;

public final class NativeAppearanceDependencyProbeMain {
    interface Checked {void run()throws Exception;}
    static int negatives;
    static void reject(Checked f)throws Exception {try{f.run();}catch(IOException|SecurityException|IllegalArgumentException e){negatives++;return;}throw new IllegalStateException("rejection missing");}
    static void require(boolean v)throws IOException {if(!v)throw new IOException("appearance dependency proof");}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]).toRealPath();
        require(Path.of(NativeAppearanceDependencyResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(root.resolve("classes").toRealPath()));
        require(Path.of(NativeAppearanceDependencyProbeMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(root.resolve("test-classes").toRealPath()));
        byte[] nad=Files.readAllBytes(root.resolve("resources/appearance-dependencies.nad"));
        var disabled=new NativeAppearanceDependencyResolver(nad);var resolver=new NativeAppearanceDependencyResolver(nad,true);
        byte[] nab=Files.readAllBytes(root.resolve("evidence/appearance-bindings.nab"));require(nab.length<=4096);
        byte[] body=Arrays.copyOf(nab,nab.length-32);var hash=MessageDigest.getInstance("SHA-256");
        hash.update("LITO-NATIVE-APPEARANCE-BINDING-EVIDENCE\0".getBytes(StandardCharsets.US_ASCII));hash.update(body);
        require(MessageDigest.isEqual(hash.digest(),Arrays.copyOfRange(nab,nab.length-32,nab.length)));
        var in=new DataInputStream(new ByteArrayInputStream(body));require(in.readInt()==0x4e414231&&in.readUnsignedShort()==1&&in.readUnsignedShort()==4);
        var inputs=new ArrayList<Input>();
        for(int i=0;i<4;i++) {
            var b=new Binding(in.readInt(),in.readLong(),in.readLong(),in.readInt(),in.readLong(),in.readLong(),in.readLong(),HexFormat.of().formatHex(in.readNBytes(32)),HexFormat.of().formatHex(in.readNBytes(32)));
            inputs.add(new Input(b,in.readUnsignedShort(),in.readUnsignedByte(),in.readInt(),in.readLong(),in.readUnsignedShort(),in.readUnsignedByte()==1));
        }
        require(in.available()==0);var result=new ArrayList<Map<String,Object>>();int planned=0;
        for(var input:inputs) {
            var b=input.binding();int walk=input.effectiveWeaponEquipped()?(input.retainedWeaponType()==3?50:46):0;
            for(var state:State.values())for(int heading=0;heading<8;heading++) {
                Plan plan=resolver.inspectBase(input,b,b.observedAt(),state,heading);
                require(!plan.rendererReady()&&!plan.runtimeAppearanceQualified()&&plan.gameWireProfile()==0);
                require(plan.logical()==input.persistedClass()&&plan.graphics()==(plan.logical()==0?3225:3476));
                int expected=switch(state){case WALK->walk;case ATTACK->walk+1;case DAMAGE->walk+2;case IDLE->walk+3;case DEATH->8;};
                require(plan.action()==expected);
                for(var frame:plan.frames()) {
                    byte[] member=Files.readAllBytes(root.resolve("resources").resolve(frame.member().name()));
                    require(member.length==frame.member().bytes()&&HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(member)).equals(frame.member().sha256()));
                    require(frame.subframe()<frame.member().frames());planned++;
                }
            }
            var attack=resolver.inspectBase(input,b,b.observedAt(),State.ATTACK,4);
            result.add(Map.of("source",b.source(),"object",b.object(),"epoch_u64_hex",Long.toUnsignedString(b.epoch(),16),"revision",b.revision(),"persisted_Class",input.persistedClass(),"graphics",attack.graphics(),"walk_action",walk,"attack_action",attack.action()));
        }
        Input a=inputs.get(0),off=inputs.get(1),other=inputs.get(2),selected=inputs.get(3);var b=a.binding();
        require(a.weaponObject()==10304&&a.retainedWeaponType()==3&&off.weaponObject()==0&&off.binding().revision()==1);
        require(selected.weaponObject()==10252&&selected.retainedWeaponType()==2&&selected.persistedClass()==734);
        reject(()->disabled.inspectBase(a,b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(a,other.binding(),b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(a,off.binding(),b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(a,selected.binding(),b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(a,b,b.observedAt()-1,State.WALK,0));
        reject(()->resolver.inspectBase(a,b,b.observedAt(),State.WALK,8));
        reject(()->resolver.inspectBase(new Input(b,0,0,null,10304,3,true),b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(new Input(b,0,1,0,10304,3,true),b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(new Input(b,0,0,734,10304,3,true),b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(new Input(b,0,0,0,0,3,true),b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(new Input(b,0,0,0,10304,3,false),b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(new Input(b,0,0,0,10304,1,true),b,b.observedAt(),State.WALK,0));
        reject(()->resolver.inspectBase(new Input(b,3,0,734,10304,3,true),b,b.observedAt(),State.WALK,0));
        var cut=new Binding(b.source(),b.object(),b.epoch(),b.channel(),b.revision(),b.observedAt(),b.observedAt()+1,b.ownerHash(),b.catalogHash());
        reject(()->resolver.inspectBase(new Input(cut,0,0,0,10304,3,true),cut,cut.validUntil(),State.WALK,0));
        byte[] corrupt=nad.clone();corrupt[20]^=1;reject(()->new NativeAppearanceDependencyResolver(corrupt,true));
        reject(()->new NativeAppearanceDependencyResolver(Arrays.copyOf(nad,nad.length-1),true));
        reject(()->new NativeAppearanceDependencyResolver(new byte[MAX_BYTES+1],true));
        // Recomputed digest still must not admit a forged format header.
        byte[] forged=nad.clone();forged[0]^=1;var d=MessageDigest.getInstance("SHA-256");d.update("LITO-NATIVE-APPEARANCE-DEPENDENCIES\0".getBytes(StandardCharsets.US_ASCII));d.update(forged,0,forged.length-32);System.arraycopy(d.digest(),0,forged,forged.length-32,32);
        reject(()->new NativeAppearanceDependencyResolver(forged,true));
        var report=new LinkedHashMap<String,Object>();report.put("state","PASSED_DEFAULT_OFF_BASE_DEPENDENCY_INSPECTION");report.put("inputs",result);
        report.put("plans",160);report.put("frame_member_checks",planned);report.put("negative_cases",negatives);
        report.put("default_off",true);report.put("live_selection_authority",false);report.put("renderer_LOADED",false);report.put("new_game_wire_profile",false);
        Path output=args.length>1?Path.of(args[1]):root.resolve("appearance-dependency-probe.json");
        Files.writeString(output,NativeTerrainProbeMain.json(report),StandardCharsets.UTF_8);
        System.out.println("Passed 160 base dependency plans, "+planned+" frame member checks, "+negatives+" rejections; runtime appearance remains unqualified");
    }
}
