import java.nio.file.*;import java.util.*;import java.util.jar.*;import java.net.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class TripleMethods implements Opcodes {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"CombatTripleArrow",SCOPE=H+"$Scope",DESC="(L"+P+"Protocol$AttackRequest;L"+PC+"$AttackEnvelope;)V";
 static MethodNode wrapper(){
  var m=new MethodNode(ASM9,ACC_PRIVATE,"nativeTripleArrowAttempt",DESC.replace(")V","I)V"),null,new String[]{"java/io/IOException"});var x=m.instructions;
  x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ILOAD,3));x.add(new MethodInsnNode(INVOKESTATIC,H,"enter","(L"+PC+";I)L"+SCOPE+";",false));x.add(new VarInsnNode(ASTORE,4));
  var start=new LabelNode();var end=new LabelNode();var fail=new LabelNode();x.add(start);x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,1));x.add(new VarInsnNode(ALOAD,2));x.add(new MethodInsnNode(INVOKEVIRTUAL,PC,"attack",DESC,false));x.add(end);x.add(new VarInsnNode(ALOAD,4));x.add(new MethodInsnNode(INVOKESTATIC,H,"leave","(L"+SCOPE+";)V",false));x.add(new InsnNode(RETURN));
  x.add(fail);x.add(new VarInsnNode(ASTORE,5));x.add(new VarInsnNode(ALOAD,4));x.add(new MethodInsnNode(INVOKESTATIC,H,"leave","(L"+SCOPE+";)V",false));x.add(new VarInsnNode(ALOAD,5));x.add(new InsnNode(ATHROW));m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,fail,null));return m;
 }
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new LinkedHashSet<MethodNode>();var names=new TreeSet<String>();int attacks=0;
  for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
   if(m.name.equals("useTripleArrowSpell")&&call.owner.equals(PC)&&call.name.equals("attack")&&call.desc.equals(DESC)){
    // Pinned original loop has index local 22, bounded by TripleArrowSpell.hitCount.
    boolean increment=false;for(var q:m.instructions.toArray())if(q instanceof IincInsnNode i&&i.var==22&&i.incr==1)increment=true;if(!increment)throw new AssertionError("triple loop changed");
    m.instructions.insertBefore(n,new VarInsnNode(ILOAD,22));call.name="nativeTripleArrowAttempt";call.desc=DESC.replace(")V","I)V");attacks++;changed.add(m);
   }
   if(call.owner.equals(P+"BattleWeaponCritical")&&call.name.equals("apply")&&call.desc.equals("(L"+P+"SourceWeaponPhysicalStats;IID)L"+P+"BattleWeaponCritical$DamageOutcome;")){
    m.instructions.insertBefore(n,new VarInsnNode(ALOAD,0));call.owner=H;call.desc=call.desc.replace("IID)","IIDL"+PC+";)");names.add(m.name);changed.add(m);
   }
  }
  if(attacks!=1||!names.equals(Set.of("attack","attackClassicWorld","attackClassicWorldPlayer")))throw new AssertionError("call census changed "+attacks+" "+names);
  var wrapper=wrapper();c.methods.add(wrapper);changed.add(wrapper);var framed=NativeSkillConsumptionMethods.frames(c,new ArrayList<>(changed),loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   var transformed=transform(old,loader);
   if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(transformed,false));return;}
   try(var next=new JarFile(args[2])){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(transformed,true)))throw new AssertionError("transformation differs");var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());
    for(var m:actual.methods)if(Set.of("useTripleArrowSpell","attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name)){
     var before=NativeSkillConsumptionMethods.method(original,m.name,m.desc);actual.methods.set(actual.methods.indexOf(m),before);
    }
    actual.methods.removeIf(m->m.name.equals("nativeTripleArrowAttempt"));if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(original,true)))throw new AssertionError("unrelated PC mutation");
    var name=P+"CombatRules.class";var before=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(name)).readAllBytes());var after=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(name)).readAllBytes());for(String method:List.of("<clinit>","snapshot")){var a=before.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow();var b=NativeSkillConsumptionMethods.method(after,a.name,a.desc);after.methods.set(after.methods.indexOf(b),a);}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(before,true),NativeSkillConsumptionMethods.bytes(after,true)))throw new AssertionError("unrelated control change");System.out.println("PASS inverse: bounded triple loop, three pre-defense multiplier sites, parser only");
   }
  }
 }
}
