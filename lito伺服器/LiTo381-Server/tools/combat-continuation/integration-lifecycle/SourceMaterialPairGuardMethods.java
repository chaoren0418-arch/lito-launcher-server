import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Shared first-value task/matches ports and the exact erroneous interface opcode. */
public final class SourceMaterialPairGuardMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",LIGHT=P+"NativeLightAction",PAIR=P+"NativeSourceLightPairLife",H=P+"NativeSourceMaterialPairGuard",TASK=P+"NativeSelfCastAction$Task",SP=P+"BattleSpellCatalog$TargetNegativeStatusSpell";
 static final List<String> OWNERS=List.of(PC,LIGHT,PAIR);
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void patch(ClassNode c){
  if(c.name.equals(PAIR)){
   var old=c.methods.stream().filter(m->m.name.equals("task")).findFirst().orElseThrow();old.name="nativeSourceMaterialOriginalTask";
   var m=new MethodNode(old.access,"task",old.desc,old.signature,old.exceptions.toArray(String[]::new));for(int i=0;i<3;i++)m.instructions.add(new VarInsnNode(ALOAD,i));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"task",old.desc,false));m.instructions.add(new InsnNode(ARETURN));c.methods.add(m);return;
  }
  if(c.name.equals(LIGHT)){
   var m=c.methods.stream().filter(x->x.name.equals("lambda$commitPair$9")).findFirst().orElseThrow();int sites=0;
   need(m.desc.endsWith("L"+TASK+";)V"),"original shared pair Task argument");
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceWeaponPlayerCounter")&&call.name.equals("matches")){
    need(call.desc.equals("(L"+P+"NativePlayerCommit$Binding;L"+PC+";)Z"),"two original matched operands");m.instructions.insertBefore(call,new VarInsnNode(ALOAD,5));call.owner=H;call.desc="(L"+P+"NativePlayerCommit$Binding;L"+PC+";L"+TASK+";)Z";sites++;
   }need(sites==2,"two original post-pair identity sites");return;
  }
  var m=c.methods.stream().filter(x->x.name.equals("nativeMaterialTargetNegativeBody")).findFirst().orElseThrow();int sites=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(SP)&&call.getOpcode()==INVOKEVIRTUAL){need(call.name.equals("skillId")&&call.desc.equals("()I")&&!call.itf,"one Source-none request injected interface getter");call.setOpcode(INVOKEINTERFACE);call.itf=true;sites++;}
  need(sites==2,"two erroneous native material interface opcodes");
 }
 public static void main(String[]a)throws Exception{
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var files=Files.list(Path.of(a[3]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=loader;
   for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(base,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);
    if(a[0].equals("splice")){var path=Path.of(a[2]).resolve(owner+".class");Files.createDirectories(path.getParent());Files.write(path,bytes);}else try(var jar=new JarFile(a[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)),"exact shared material pair splice "+owner);}
   }
  }if(a[0].equals("splice"))Files.write(Path.of(a[2]).getParent().resolve("material-pair-guard-ports.tsv"),List.of("owner\tmethod\tphase",PAIR+"\ttask\tfirst-role-life-status-task-values; full original Task fallback sibling",LIGHT+"\tlambda$commitPair$9\ttwo explicit Task first-identity guards before old Binding.matches",PC+"\tnativeMaterialTargetNegativeBody\ttwo interface ABI opcode corrections at Source-none and LOS getter ports"));
  System.out.println("PASS Source material shared pair:2 explicit Task identity ports,1 complete original Task sibling,2 interface opcodes; no receipt/cost/RNG/status/body changes");
 }
}
