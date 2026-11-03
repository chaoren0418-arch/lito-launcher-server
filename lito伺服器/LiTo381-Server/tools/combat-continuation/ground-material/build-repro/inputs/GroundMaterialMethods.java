import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Exact native roots plus private placement copies; all unrelated methods remain unchanged. */
public final class GroundMaterialMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",HUB=P+"GameHub",A=P+"NativeGroundMaterialAction",M=P+"NativeCastMaterial";
 record Spec(String root,String body,String helper,String place,String placed,String effects,int monitor,int previous,boolean world){}
 static final List<Spec>SPECS=List.of(
  new Spec("useGroundSpell","nativeMaterialGroundBody","battle","placeGroundSpell","nativeMaterialPlaceGroundSpell","BattleGroundEffects",18,10,false),
  new Spec("useClassicWorldGroundSpell","nativeMaterialWorldGroundBody","world","placeClassicWorldGroundSpell","nativeMaterialPlaceWorldGroundSpell","ClassicWorldGroundEffects",21,15,true));
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static void remove(MethodNode m,AbstractInsnNode first,AbstractInsnNode end){for(var n=first;n!=end;){if(n==null)throw new AssertionError("unbounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static MethodNode named(ClassNode c,String n){return c.methods.stream().filter(m->m.name.equals(n)).findFirst().orElseThrow();}
 static void framed(ClassNode c,List<MethodNode> changed,ClassLoader loader){var f=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(f,m.name,m.desc));}
 static Map<String,ClassNode> transform(JarFile jar,ClassLoader loader)throws Exception{
  var pc=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var hub=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(HUB+".class")).readAllBytes());var pcs=new ArrayList<MethodNode>();var hubs=new ArrayList<MethodNode>();
  for(var s:SPECS){
   var old=named(pc,s.root);var body=clone(old);body.name=s.body;body.access&=~ACC_PRIVATE;
   MethodInsnNode consume=null,delta=null,save=null,place=null;InvokeDynamicInsnNode factory=null;Handle handle=null;int monitors=0;
   for(var n:body.instructions.toArray()){
    if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){if(!(prev(n) instanceof VarInsnNode v)||v.var!=s.monitor)throw new AssertionError("unknown monitor");body.instructions.set(n,new InsnNode(POP));monitors++;}
    if(n instanceof MethodInsnNode c){if(c.owner.equals(P+"CombatDatabase")&&c.name.equals("consumeMaterial"))consume=c;if(c.owner.equals(PC)&&c.name.equals("sendInventoryDelta"))delta=c;if(c.owner.equals(P+"GameStateStore")&&c.name.equals("saveEconomyState"))save=c;if(c.owner.equals(HUB)&&c.name.equals(s.place))place=c;}
    if(n instanceof InvokeDynamicInsnNode d)for(var arg:d.bsmArgs)if(arg instanceof Handle h&&h.getOwner().equals(PC)&&h.getName().startsWith("lambda$"+s.root+"$")){if(factory!=null)throw new AssertionError("multiple ground commits");factory=d;handle=h;}
   }
   if(monitors!=6||consume==null||delta==null||save==null||place==null||factory==null)throw new AssertionError("ground root shape "+s.root+" monitors="+monitors);
   var start=(AbstractInsnNode)consume;for(int i=0;i<6;i++)start=prev(start);if(!(start instanceof VarInsnNode copyReceiver)||copyReceiver.var!=25||copyReceiver.getOpcode()!=ALOAD)throw new AssertionError("ground material slice");var end=consume.getNext();remove(body,start,end);body.instructions.insertBefore(end,new InsnNode(ICONST_1));
   var next=prev(delta);if(!(next instanceof FieldInsnNode nextField)||!nextField.name.equals("inventory"))throw new AssertionError("ground delta next");var previous=prev(prev(next));if(!(previous instanceof VarInsnNode previousReceiver)||previousReceiver.getOpcode()!=ALOAD||previousReceiver.var!=s.previous)throw new AssertionError("ground delta previous");
   body.instructions.set(next,new MethodInsnNode(INVOKESTATIC,M,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));var wire=new InsnList();wire.add(new VarInsnNode(ALOAD,0));wire.add(new MethodInsnNode(INVOKESTATIC,M,"previous","(L"+PC+";)L"+P+"RuntimeInventory;",false));body.instructions.insertBefore(previous,wire);body.instructions.remove(previous);
   start=save;while((start=prev(start))!=null)if(start instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("state")){start=prev(start);break;}if(!(start instanceof VarInsnNode stateReceiver)||stateReceiver.var!=0)throw new AssertionError("ground save slice");remove(body,start,save.getNext());
   body.instructions.set(place,new MethodInsnNode(INVOKESTATIC,A,s.world?"placeWorld":"placeBattle","(L"+HUB+";"+place.desc.substring(1),false));
   var lambda=clone(NativeSkillConsumptionMethods.method(pc,handle.getName(),handle.getDesc()));lambda.name=s.body+"Commit";
   int replaced=0,bounded=0;
   for(var n:lambda.instructions.toArray()){
    if(n instanceof MethodInsnNode c&&c.owner.equals(P+"NativeDeItemApplication")&&c.name.equals("replace")){start=n;for(int i=0;i<3;i++)start=prev(start);remove(lambda,start,n.getNext());replaced++;}
    if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(PC)&&f.name.equals("playerMp")){
     if(prev(n).getOpcode()!=ISUB)throw new AssertionError("ground signed MP shape");var bounds=new InsnList();bounds.add(new InsnNode(DUP2));bounds.add(new MethodInsnNode(INVOKESTATIC,P+"NativeSkillConsumption","boundMp","(L"+PC+";I)I",false));bounds.add(new InsnNode(SWAP));bounds.add(new InsnNode(POP));lambda.instructions.insertBefore(n,bounds);bounded++;
    }
   }
   if(replaced!=1||bounded!=1)throw new AssertionError("ground callback shape");
   var admission=new InsnList();admission.add(new VarInsnNode(ALOAD,0));admission.add(new VarInsnNode(ALOAD,3));admission.add(new MethodInsnNode(INVOKESTATIC,A,"admit","(L"+PC+";L"+P+"BattleSpellCatalog$GroundSpell;)V",false));admission.add(new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false));admission.add(new VarInsnNode(LSTORE,4));lambda.instructions.insert(admission);
   for(int i=0;i<factory.bsmArgs.length;i++)if(factory.bsmArgs[i]==handle)factory.bsmArgs[i]=new Handle(handle.getTag(),handle.getOwner(),lambda.name,handle.getDesc(),handle.isInterface());
   var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));for(int i=0;i<3;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,s.helper,"(L"+PC+";"+old.desc.substring(1),false));wrapper.instructions.add(new InsnNode(RETURN));pc.methods.set(pc.methods.indexOf(old),wrapper);pc.methods.add(body);pc.methods.add(lambda);pcs.add(wrapper);pcs.add(body);pcs.add(lambda);
   var original=hub.methods.stream().filter(m->m.name.equals(s.place)&&m.desc.contains("Ljava/lang/Runnable;")).findFirst().orElseThrow();var placement=clone(original);placement.name=s.placed;placement.access&=~ACC_PRIVATE;if((placement.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("original hub placement lock missing");int casts=0,runs=0;
   for(var n:placement.instructions.toArray())if(n instanceof MethodInsnNode c){
    if(c.owner.equals(P+s.effects)&&c.name.equals("cast")){
     var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));if(s.world)args.add(new VarInsnNode(ALOAD,14));args.add(new VarInsnNode(ALOAD,11));placement.instructions.insertBefore(n,args);
     String descriptor="(L"+P+s.effects+";"+c.desc.substring(1,c.desc.indexOf(')'))+"L"+HUB+";"+(s.world?"Ljava/lang/Object;":"")+"Ljava/lang/Runnable;"+c.desc.substring(c.desc.indexOf(')'));
     placement.instructions.set(n,new MethodInsnNode(INVOKESTATIC,A,s.world?"castWorld":"castBattle",descriptor,false));casts++;
    }
    if(c.owner.equals("java/lang/Runnable")&&c.name.equals("run")){var receiver=prev(n);if(!(receiver instanceof VarInsnNode v)||v.var!=11)throw new AssertionError("hub callback receiver");remove(placement,receiver,n.getNext());runs++;}
   }
   if(casts!=1||runs!=1)throw new AssertionError("hub placement shape");hub.methods.add(placement);hubs.add(placement);
  }
  framed(pc,pcs,loader);framed(hub,hubs,loader);return Map.of(PC,pc,HUB,hub);
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var patches=transform(old,loader);
  if(args[0].equals("splice")){for(var e:patches.entrySet()){var path=Path.of(args[2]).resolve(e.getKey()+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(e.getValue(),false));}return;}
  try(var next=new JarFile(args[2])){for(var e:patches.entrySet()){
   var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(e.getKey()+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(e.getValue(),true)))throw new AssertionError("ground transform mismatch");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(e.getKey()+".class")).readAllBytes());
   for(var s:SPECS)if(e.getKey().equals(PC)){var original=named(parent,s.root);var wrapper=named(actual,s.root);actual.methods.set(actual.methods.indexOf(wrapper),original);if(!actual.methods.removeIf(m->m.name.equals(s.body))||!actual.methods.removeIf(m->m.name.equals(s.body+"Commit")))throw new AssertionError();}else if(!actual.methods.removeIf(m->m.name.equals(s.placed)))throw new AssertionError();
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC/hub changed");
  }System.out.println("PASS ground roots inverse; original lambdas/hub methods, target gates and unrelated methods unchanged");}
 }}
}
