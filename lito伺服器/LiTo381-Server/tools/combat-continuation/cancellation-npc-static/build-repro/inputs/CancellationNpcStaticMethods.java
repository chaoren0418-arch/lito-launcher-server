import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Three named methods and one hub-owned field. No inventory, character or unrelated NPC rewrite. */
public final class CancellationNpcStaticMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",HUB=P+"GameHub",H=P+"NativeCancellationNpcStatic",NPC=P+"ClassicWorldNpcCatalog$StaticNpc",FIELD="nativeCancellationStaticAppearance";
 static Map<String,ClassNode> transform(JarFile jar,ClassLoader loader)throws Exception {
  var pc=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());
  var route=pc.methods.stream().filter(m->m.name.equals("useWorldRoiCancellation")).findFirst().orElseThrow();var code=new InsnList();var fallback=new LabelNode();
  for(int i=0;i<3;i++)code.add(new VarInsnNode(ALOAD,i));code.add(new MethodInsnNode(INVOKESTATIC,H,"route","(L"+PC+";[BL"+P+"BattleSpellCatalog$CancellationSpell;)Z",false));code.add(new JumpInsnNode(IFEQ,fallback));code.add(new InsnNode(RETURN));code.add(fallback);route.instructions.insert(code);
  var perceive=pc.methods.stream().filter(m->m.name.equals("sendClassicWorldStaticNpc")).findFirst().orElseThrow();int projections=0;
  for(var n:perceive.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"Protocol")&&call.name.equals("classicStaticNpc")){
   if(!call.desc.equals("(L"+NPC+";II)[B"))throw new AssertionError("static perception contract");
   perceive.instructions.insertBefore(call,new VarInsnNode(ALOAD,0));call.owner=H;call.name="putObject";call.desc="(L"+NPC+";IIL"+PC+";)[B";projections++;
  }
  if(projections!=1)throw new AssertionError("static perception count "+projections);
  var framed=NativeSkillConsumptionMethods.frames(pc,List.of(route,perceive),loader);for(var m:List.of(route,perceive))pc.methods.set(pc.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));
  var hub=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(HUB+".class")).readAllBytes());if(hub.fields.stream().anyMatch(f->f.name.equals(FIELD)))throw new AssertionError("duplicate appearance field");hub.fields.add(new FieldNode(ASM9,ACC_PRIVATE,FIELD,"Ljava/lang/Object;",null,null));
  var restore=hub.methods.stream().filter(m->m.name.equals("restoreClassicWorldNpcGfx")).findFirst().orElseThrow();int returns=0;
  for(var n:restore.instructions.toArray())if(n.getOpcode()==IRETURN){var hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new VarInsnNode(ALOAD,1));hook.add(new MethodInsnNode(INVOKESTATIC,H,"restored","(ZL"+HUB+";L"+NPC+";)Z",false));restore.instructions.insertBefore(n,hook);returns++;}
  if(returns!=3)throw new AssertionError("static restore return count "+returns);
  framed=NativeSkillConsumptionMethods.frames(hub,List.of(restore),loader);hub.methods.set(hub.methods.indexOf(restore),NativeSkillConsumptionMethods.method(framed,restore.name,restore.desc));return Map.of(PC,pc,HUB,hub);
 }
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
   var expected=transform(old,loader);
   if(args[0].equals("splice")){for(var e:expected.entrySet()){var path=Path.of(args[2]).resolve(e.getKey()+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(e.getValue(),false));}return;}
   try(var next=new JarFile(args[2])){for(var e:expected.entrySet()){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(e.getKey()+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(e.getValue(),true)))throw new AssertionError("unexpected class changes");
    var prior=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(e.getKey()+".class")).readAllBytes());
    for(String name:e.getKey().equals(PC)?List.of("useWorldRoiCancellation","sendClassicWorldStaticNpc"):List.of("restoreClassicWorldNpcGfx")){
     var m=prior.methods.stream().filter(x->x.name.equals(name)).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(actual.methods.stream().filter(x->x.name.equals(name)).findFirst().orElseThrow()),m);
    }
    if(e.getKey().equals(HUB))actual.fields.removeIf(f->f.name.equals(FIELD));
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(prior,true)))throw new AssertionError("unrelated methods/fields changed");
   }System.out.println("PASS exact three method and one field inverse");}
  }
 }
}
