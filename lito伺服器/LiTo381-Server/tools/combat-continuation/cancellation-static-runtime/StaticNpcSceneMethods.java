import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Redirect only five static NPC presentation APIs, and add owned lifecycle cleanup. */
public final class StaticNpcSceneMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",HUB=P+"GameHub",H=P+"NativeStaticNpcScene";
 static final Map<String,String> CALLS=Map.of("classicWorldNpcGfx","gfx","classicWorldNpcHeading","heading","transformClassicWorldNpcGfx","transform","restoreClassicWorldNpcGfx","restore","changeClassicWorldNpcHeading","turn");
 static final Set<String> PC_METHODS=Set.of("openClassicWorldHypertext","handleClassicWorldStoryAction","handleClassicWorldRescueStaticAction","handleClassicWorldDspyStaticAction","handleClassicWorldRoiStaticStart","sendClassicWorldStaticNpc","useWorldRoiCancellation","attackClassicWorld","resolveClassicWorldScarecrowHit");
 static final Set<String> HUB_METHODS=Set.of("transitionClassicWorld","leaveClassicWorld","close");
 static Map<String,ClassNode> transform(JarFile jar,ClassLoader loader)throws Exception {
  var pc=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var modified=new ArrayList<MethodNode>();int count=0;
  for(var m:pc.methods){
   boolean changed=false;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(HUB)&&CALLS.containsKey(call.name)){
    if(!PC_METHODS.contains(m.name)||(m.access&ACC_STATIC)!=0||call.getOpcode()!=INVOKEVIRTUAL)throw new AssertionError("unexpected static NPC callsite "+m.name);
    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,0));call.owner=H;call.name=CALLS.get(call.name);call.desc="(L"+HUB+";"+call.desc.substring(1).replace(")","L"+PC+";)");call.setOpcode(INVOKESTATIC);changed=true;count++;
   }
   if(changed)modified.add(m);
  }
  if(count!=26||!modified.stream().map(m->m.name).collect(java.util.stream.Collectors.toSet()).equals(PC_METHODS))throw new AssertionError("static NPC callsite count "+count);
  var framed=NativeSkillConsumptionMethods.frames(pc,modified,loader);for(var m:modified)pc.methods.set(pc.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));
  var hub=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(HUB+".class")).readAllBytes());
  if(hub.fields.stream().anyMatch(f->f.name.equals("nativeStaticNpcScenes")))throw new AssertionError("duplicate scene state");
  hub.fields.add(new FieldNode(ASM9,ACC_PRIVATE,"nativeStaticNpcScenes","Ljava/lang/Object;",null,null));modified.clear();
  for(var m:hub.methods)if(HUB_METHODS.contains(m.name)){
   if(m.name.equals("close")){
    var hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new MethodInsnNode(INVOKESTATIC,H,"close","(L"+HUB+";)V",false));m.instructions.insert(hook);
   }else for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN||n.getOpcode()==ARETURN){
    var hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new MethodInsnNode(INVOKESTATIC,H,"prune","(L"+HUB+";)V",false));m.instructions.insertBefore(n,hook);
   }
   modified.add(m);
  }
  if(modified.size()!=3)throw new AssertionError("lifecycle contract");
  framed=NativeSkillConsumptionMethods.frames(hub,modified,loader);for(var m:modified)hub.methods.set(hub.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));
  return Map.of(PC,pc,HUB,hub);
 }
 public static void main(String[]args)throws Exception{
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   var expected=transform(old,loader);
   if(args[0].equals("splice")){for(var e:expected.entrySet()){var p=Path.of(args[2]).resolve(e.getKey()+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(e.getValue(),false));}return;}
   try(var next=new JarFile(args[2])){for(var e:expected.entrySet()){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(e.getKey()+".class")).readAllBytes());
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(e.getValue(),true)))throw new AssertionError("unexpected changes");
    var prior=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(e.getKey()+".class")).readAllBytes());
    for(var m:prior.methods)if((e.getKey().equals(PC)?PC_METHODS:HUB_METHODS).contains(m.name))actual.methods.set(actual.methods.indexOf(NativeSkillConsumptionMethods.method(actual,m.name,m.desc)),m);
    if(e.getKey().equals(HUB))actual.fields.removeIf(f->f.name.equals("nativeStaticNpcScenes"));
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(prior,true)))throw new AssertionError("unrelated methods changed");
   }System.out.println("PASS exact 26 callsites, 3 lifecycle methods and one field inverse");}
  }
 }
}
