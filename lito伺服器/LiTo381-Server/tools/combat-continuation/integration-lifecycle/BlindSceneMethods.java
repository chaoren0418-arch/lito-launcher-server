import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Bounded ports on the exact candidate. No whole ClientSession compilation. */
public final class BlindSceneMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",B=P+"BattleCompanion",T=P+"BattleTimedStatus",H=P+"NativeBlindSceneAction",S=P+"NativeBlindSourceState",OLD=P+"NativeBlindMaterialAction";
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(n+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static InsnList skill(){var x=new InsnList();x.add(new VarInsnNode(ALOAD,2));x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$BlindSpell","skillId","()I",false));return x;}
 static void patch(ClassNode c){
  if(c.name.equals(B)){int ports=0;for(var m:c.methods)if(m.name.equals("cancelMagicEffects"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals(P+"NativeCompanionWindState")&&v.name.equals("cancel")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,S,"cancel","(L"+B+";)V",false));m.instructions.insert(n,x);ports++;}if(ports!=1)throw new AssertionError("NPC cancel port");return;}
  if(c.name.equals(T)){int reports=0,clears=0;for(var m:c.methods)if(m.name.equals("cancelMagicEffects"))for(var n:m.instructions.toArray()){
    if(n instanceof VarInsnNode v&&v.getOpcode()==ISTORE&&v.var==4){m.instructions.insertBefore(n,new MethodInsnNode(INVOKESTATIC,S,"removed","(Z)Z",false));reports++;}
    if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.name.equals("blindExpiresAtNanos")){var zero=prev(n);if(zero.getOpcode()!=LCONST_0)throw new AssertionError("blind clear shape");var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,T,"blindExpiresAtNanos","J"));x.add(new MethodInsnNode(INVOKESTATIC,S,"cleared","(J)J",false));m.instructions.insertBefore(zero,x);m.instructions.remove(zero);clears++;}
   }if(reports!=1||clears!=1)throw new AssertionError("PC blind cancellation ports");return;}
  int roots=0,chance=0,admit=0,erase=0,sleep=0,counter=0,alias=0;for(var m:new ArrayList<>(c.methods)){
   if(m.name.equals("combatProbability$57")){var q=new MethodNode(ACC_STATIC,"nativeCompanionBlindChance",m.desc,null,m.exceptions.toArray(String[]::new));m.accept(q);c.methods.add(q);alias++;}
   if(m.name.equals("useBlindSpell")||m.name.equals("useClassicWorldBlindSpell"))for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(OLD)&&(v.name.equals("battle")||v.name.equals("world"))){v.owner=H;roots++;}
   if(!m.name.equals("nativeMaterialBlindBody")&&!m.name.equals("nativeMaterialWorldBlindBody"))continue;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v){
    if(v.owner.equals(PC)&&(v.name.equals("combatProbability$57")||v.name.equals("combatProbability$5"))){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(skill());x.add(new MethodInsnNode(INVOKESTATIC,H,"chance","(IL"+PC+";I)I",false));m.instructions.insert(n,x);chance++;}
    if(v.owner.equals(OLD)&&v.name.equals("admit")){v.owner=H;admit++;}
    if(v.owner.equals(PC)&&v.name.equals("consumeEraseMagicAfterProbability")){v.setOpcode(INVOKESTATIC);v.owner=H;v.name="erase";v.desc="(L"+PC+";"+v.desc.substring(1);erase++;}
    if(v.owner.equals(PC)&&v.name.equals("breakSleepForStatusSpell")){m.instructions.insertBefore(n,skill());v.setOpcode(INVOKESTATIC);v.owner=H;v.name="sleep";v.desc="(L"+PC+";"+v.desc.substring(1).replace(")V","I)V");sleep++;}
    if(v.owner.equals(PC)&&v.name.equals("cancelSleep")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(skill());m.instructions.insertBefore(n,x);v.setOpcode(INVOKESTATIC);v.owner=H;v.name="sleep";v.desc="(L"+PC+";Ljava/lang/String;L"+PC+";I)V";sleep++;}
    if(v.owner.equals(T)&&v.name.equals("consumeCounterMagic")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(skill());m.instructions.insertBefore(n,x);v.setOpcode(INVOKESTATIC);v.owner=H;v.name="counter";v.desc="(L"+T+";JL"+PC+";I)Z";counter++;}
   }
  }if(roots!=2||chance!=2||admit!=2||erase!=2||sleep!=2||counter!=2||alias!=1)throw new AssertionError("two blind body ports "+List.of(roots,chance,admit,erase,sleep,counter,alias));
 }
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){for(String name:List.of(PC,B,T)){var c=read(j,name);patch(c);var result=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,result);}else try(var next=new JarFile(a[2])){if(!Arrays.equals(result,bytes(read(next,name))))throw new AssertionError(name);}}}System.out.println("PASS exact two blind roots, Source target/probability/erase/sleep/counter ports and shared state40 cancellation");}
}
