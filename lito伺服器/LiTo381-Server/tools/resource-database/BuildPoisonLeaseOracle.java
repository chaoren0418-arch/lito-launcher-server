import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.lang.reflect.InvocationTargetException;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Executes current source CAS/callback bodies in a platform-only isolated fixture loader. */
public class BuildPoisonLeaseOracle {
 static final String S="com/lineage/server/model/",P="local/lito381/server/",T=P+"SourceLeaseTarget",A=P+"SourceLeasePoison",B=P+"SourceLeaseParalysis";
 static final Map<String,String> TYPES=Map.of(S+"L1Character",T,S+"L1Character$PoisonTransition",T+"$PoisonTransition",S+"L1Character$ParalysisTransition",T+"$ParalysisTransition",S+"poison/L1Poison",A,S+"poison/L1Poison$CleanupPlan",A+"$CleanupPlan",S+"L1Paralysis",B,S+"L1Paralysis$CleanupPlan",B+"$CleanupPlan");
 static int checks;
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static String map(String s){if(s==null)return null;for(var entry:TYPES.entrySet())s=s.replace(entry.getKey(),entry.getValue());return s;}
 static ClassNode read(Path root,String name)throws Exception{var c=new ClassNode();new ClassReader(Files.readAllBytes(root.resolve(name+".class"))).accept(c,0);return c;}
 static void write(Path root,ClassNode c)throws Exception{
  c.name=map(c.name);c.superName=map(c.superName);c.signature=map(c.signature);c.outerClass=map(c.outerClass);c.outerMethodDesc=map(c.outerMethodDesc);c.nestHostClass=map(c.nestHostClass);if(c.nestMembers!=null)c.nestMembers=c.nestMembers.stream().map(BuildPoisonLeaseOracle::map).toList();c.innerClasses.clear();
  for(var f:c.fields){f.desc=map(f.desc);f.signature=map(f.signature);}
  for(var m:c.methods){m.desc=map(m.desc);m.signature=map(m.signature);if(m.localVariables!=null)for(var v:m.localVariables){v.desc=map(v.desc);v.signature=map(v.signature);}
   for(var n:m.instructions){if(n instanceof MethodInsnNode x){x.owner=map(x.owner);x.desc=map(x.desc);check(!x.owner.startsWith("com/lineage/"),"source boot/dependency excluded: "+x.owner);}
    else if(n instanceof FieldInsnNode x){x.owner=map(x.owner);x.desc=map(x.desc);check(!x.owner.startsWith("com/lineage/"),"source field dependency excluded");}
    else if(n instanceof TypeInsnNode x)x.desc=map(x.desc);
    else if(n instanceof LdcInsnNode x&&x.cst instanceof Type t)x.cst=Type.getType(map(t.getDescriptor()));
    else if(n instanceof FrameNode x){if(x.local!=null)for(int i=0;i<x.local.size();i++)if(x.local.get(i) instanceof String s)x.local.set(i,map(s));if(x.stack!=null)for(int i=0;i<x.stack.size();i++)if(x.stack.get(i) instanceof String s)x.stack.set(i,map(s));}
    else if(n instanceof InvokeDynamicInsnNode)throw new AssertionError("unexpected dynamic callback body; explicit adapter required");
   }
  }
  var out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);Path path=root.resolve(c.name+".class");Files.createDirectories(path.getParent());Files.write(path,out.toByteArray());
 }
 static void simple(ClassNode c,String name,String desc,int access,Consumer<MethodVisitor> body){var m=new MethodNode(access,name,desc,null,null);m.visitCode();body.accept(m);m.visitMaxs(12,12);m.visitEnd();c.methods.add(m);}
 static void fixture(Path out,String base)throws Exception{
  var c=new ClassNode();c.version=V17;c.access=ACC_PUBLIC|ACC_FINAL;c.name=base+"Fixture";c.superName=base;c.fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL,"target","L"+T+";",null,null));c.fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL,"cleanup","Ljava/lang/Runnable;",null,null));
  simple(c,"<init>","(L"+T+";Ljava/lang/Runnable;)V",ACC_PUBLIC,m->{m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,base,"<init>","()V",false);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitFieldInsn(PUTFIELD,c.name,"target","L"+T+";");m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,2);m.visitFieldInsn(PUTFIELD,c.name,"cleanup","Ljava/lang/Runnable;");m.visitInsn(RETURN);});
  simple(c,"getEffectId","()I",ACC_PUBLIC,m->{m.visitInsn(ICONST_1);m.visitInsn(IRETURN);});
  simple(c,"cure","()V",ACC_PUBLIC,m->{m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,c.name,"target","L"+T+";");m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,c.name,"cleanup","Ljava/lang/Runnable;");m.visitMethodInsn(INVOKEVIRTUAL,base,"cureIfCurrent","(L"+T+";Ljava/lang/Runnable;)Z",false);m.visitInsn(POP);m.visitInsn(RETURN);});
  simple(c,"run","(Ljava/lang/Runnable;)Z",ACC_PUBLIC,m->{m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,c.name,"target","L"+T+";");m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,base,"runIfCurrent","(L"+T+";Ljava/lang/Runnable;)Z",false);m.visitInsn(IRETURN);});write(out,c);
 }
 static Object invoke(Object target,String name,Object...args)throws Exception{for(Class<?> owner=target instanceof Class<?> k?k:target.getClass();owner!=null;owner=owner.getSuperclass())for(var m:owner.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);try{return m.invoke(target instanceof Class<?>?null:target,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception cause)throw cause;throw e;}}throw new NoSuchMethodException(name);}
 static boolean install(Class<?> base,Object target,Object lease,BooleanSupplier start)throws Exception{return (boolean)invoke(base,"installIfAbsent",target,lease,start);}
 static void proof(Path out)throws Exception{
  try(var loader=new URLClassLoader(new URL[]{out.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   Class<?> target=loader.loadClass(T.replace('/','.')),poison=loader.loadClass(A.replace('/','.')),paralysis=loader.loadClass(B.replace('/','.'));Object actor=target.getConstructor().newInstance();
   var bases=List.of(poison,paralysis);var names=List.of("Poison","Paralysis");
   for(int i=0;i<2;i++){
    Class<?> base=bases.get(i),concrete=loader.loadClass(base.getName()+"Fixture");var constructor=concrete.getConstructor(target,Runnable.class);var cleaned=new AtomicInteger();Object lease=constructor.newInstance(actor,(Runnable)cleaned::incrementAndGet);
    check(install(base,actor,lease,()->true),"source install admits first lease");var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var error=new AtomicReference<Throwable>();Thread callback=new Thread(()->{try{invoke(lease,"run",(Runnable)()->{entered.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new AssertionError("callback release timeout");}catch(InterruptedException e){throw new AssertionError(e);}});}catch(Throwable e){error.set(e);}},"source-lease-callback");callback.start();check(entered.await(3,TimeUnit.SECONDS),"actual source callback entered");invoke(lease,"cure");check(cleaned.get()==0,"source cure defers cleanup until callback exit");Object next=constructor.newInstance(actor,(Runnable)()->{});check(!install(base,actor,next,()->true),"source closing transition blocks replacement");release.countDown();callback.join(3000);check(!callback.isAlive()&&error.get()==null&&cleaned.get()==1,"source last callback retires transition exactly once");check(install(base,actor,next,()->true),"replacement admitted after cleanup");invoke(lease,"cure");check((boolean)invoke(actor,"isCurrent"+names.get(i),next),"old cure cannot clear new source lease");check(!(boolean)invoke(lease,"run",(Runnable)()->{throw new AssertionError("stale callback");}),"stale callback rejected");invoke(next,"cure");
    Object failed=constructor.newInstance(actor,(Runnable)cleaned::incrementAndGet);check(!install(base,actor,failed,()->false),"source rejected starter rolls back");Object throwing=constructor.newInstance(actor,(Runnable)cleaned::incrementAndGet);try{install(base,actor,throwing,()->{throw new IllegalArgumentException("fixture");});throw new AssertionError("starter exception lost");}catch(IllegalArgumentException expected){check(cleaned.get()==3,"source throwing starter rolls back in finally");}
    Object self=constructor.newInstance(actor,(Runnable)cleaned::incrementAndGet);check(install(base,actor,self,()->true),"source reentrant callback setup");check((boolean)invoke(self,"run",(Runnable)()->{try{invoke(self,"cure");check(cleaned.get()==3,"reentrant cure returns without waiting on self");}catch(Exception e){throw new AssertionError(e);}})&&cleaned.get()==4,"reentrant cleanup executes after callback finally");
   }
   var p=loader.loadClass(poison.getName()+"Fixture").getConstructor(target,Runnable.class);var q=loader.loadClass(paralysis.getName()+"Fixture").getConstructor(target,Runnable.class);
   for(boolean first:new boolean[]{false,true}){Object a=p.newInstance(actor,(Runnable)()->{}),b=q.newInstance(actor,(Runnable)()->{});if(first){check(install(poison,actor,a,()->true),"poison first");check(install(paralysis,actor,b,()->true),"paralysis coexists");}else{check(install(paralysis,actor,b,()->true),"paralysis first");check(install(poison,actor,a,()->true),"poison coexists");}invoke(a,"cure");check((boolean)invoke(actor,"isCurrentParalysis",b),"source poison cleanup preserves independent curse slot");invoke(b,"cure");}
  }
 }
 public static void main(String[] args)throws Exception{
  Path source=Path.of(args[0]),out=Path.of(args[1]);var character=read(source,S+"L1Character");
  check(read(source,S+"L1CurseParalysis").superName.equals(S+"L1Paralysis")&&read(source,S+"poison/L1ParalysisPoison").superName.equals(S+"poison/L1Poison"),"source factories use distinct base lease families");
  var target=new ClassNode();target.version=V17;target.access=ACC_PUBLIC;target.name=T;target.superName="java/lang/Object";target.nestMembers=new ArrayList<>(List.of(T+"$PoisonTransition",T+"$ParalysisTransition"));
  for(var f:character.fields)if(Set.of("_poison","_paralysis","POISON_UPDATER","PARALYSIS_UPDATER").contains(f.name)){target.fields.add(f);if(f.name.startsWith("_"))check((f.access&ACC_VOLATILE)!=0,"source independent lease field is volatile");}check(target.fields.size()==4,"two distinct source lease fields/updaters");
  for(var m:character.methods)if(Set.of("tryInstallPoison","isCurrentPoison","claimPoisonRemoval","completePoisonRemoval","tryInstallParalysis","isCurrentParalysis","claimParalysisRemoval","completeParalysisRemoval").contains(m.name))target.methods.add(m);check(target.methods.size()==8,"eight exact source identity/CAS methods");
  simple(target,"<init>","()V",ACC_PUBLIC,m->{m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(RETURN);});
  simple(target,"<clinit>","()V",ACC_STATIC,m->{for(var pair:List.of(new String[]{A,"_poison","POISON_UPDATER"},new String[]{B,"_paralysis","PARALYSIS_UPDATER"})){m.visitLdcInsn(Type.getObjectType(T));m.visitLdcInsn(Type.getObjectType(pair[0]));m.visitLdcInsn(pair[1]);m.visitMethodInsn(INVOKESTATIC,"java/util/concurrent/atomic/AtomicReferenceFieldUpdater","newUpdater","(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater;",false);m.visitFieldInsn(PUTSTATIC,T,pair[2],"Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater;");}m.visitInsn(RETURN);});write(out,target);
  for(String name:List.of(S+"poison/L1Poison",S+"poison/L1Poison$CleanupPlan",S+"L1Paralysis",S+"L1Paralysis$CleanupPlan",S+"L1Character$PoisonTransition",S+"L1Character$ParalysisTransition")){var c=read(source,name);c.methods.removeIf(m->Set.of("isValidTarget","sendMessageIfPlayer").contains(m.name));write(out,c);}
  fixture(out,A);fixture(out,B);proof(out);
  var damage=read(source,S+"poison/L1DamagePoison");var origin=damage.fields.stream().filter(f->f.name.equals("_attacker")).findFirst().orElseThrow();if((origin.access&(ACC_PRIVATE|ACC_FINAL))!=(ACC_PRIVATE|ACC_FINAL)||!origin.desc.equals("L"+S+"L1Character;"))throw new AssertionError("source poison attacker is a final character reference");int originWrites=0,originReads=0,pcReceives=0,mobReceives=0;for(var method:damage.methods)for(var n:method.instructions){if(n instanceof FieldInsnNode f&&f.owner.equals(damage.name)&&f.name.equals("_attacker")){if(f.getOpcode()==PUTFIELD){if(!method.name.equals("<init>"))throw new AssertionError("source poison attacker mutation");originWrites++;}if(f.getOpcode()==GETFIELD&&method.name.equals("poisonTickCurrent"))originReads++;}if(method.name.equals("poisonTickCurrent")&&n instanceof MethodInsnNode call&&call.name.equals("receiveDamage")){if(call.owner.equals(S+"Instance/L1PcInstance"))pcReceives++;else if(call.owner.equals(S+"Instance/L1MonsterInstance"))mobReceives++;else throw new AssertionError("unknown source poison receiver");}}if(originWrites!=1||originReads!=2||pcReceives!=1||mobReceives!=1)throw new AssertionError("source captured poison attacker is reused by PC and monster callbacks");
  var silence=read(source,S+"poison/L1SilencePoison");int startShape=0,endShape=0;for(var method:silence.methods){var names=new ArrayList<String>();int message=0;for(var n:method.instructions)if(n instanceof MethodInsnNode call){names.add(call.name);if(call.name.equals("sendMessageIfPlayer")){var code=n.getPrevious();while(code!=null&&code.getOpcode()<0)code=code.getPrevious();if(!(code instanceof IntInsnNode value))throw new AssertionError("source silence message constant");message=value.operand;}}if(message==310){if(!names.equals(List.of("setSkillEffect","setPoisonEffect","sendMessageIfPlayer")))throw new AssertionError("source silence initial publication order");startShape++;}if(message==311){if(!names.equals(List.of("killSkillEffectTimer","setPoisonEffect","sendMessageIfPlayer")))throw new AssertionError("source silence cleanup publication order");endShape++;}}if(startShape!=1||endShape!=1)throw new AssertionError("unique source silence initial/cleanup callbacks");
  Files.writeString(Path.of(args[2]),"{\"source_CAS_callback_bodies_executed\":true,\"source_character_CAS_methods\":8,\"source_lease_bases\":2,\"generated_source_lease_classes\":9,\"source_damage_poison_final_attacker_constructor_and_both_callback_reads_verified\":true,\"source_silence_compiled_initial_palette310_and_cleanup_palette311_order_verified\":true,\"source_boot_or_SQL_executed\":false,\"checks\":"+checks+"}\n");System.out.println("PASS actual source poison/paralysis lease CAS/closing/callback bodies; checks="+checks);
 }
}
