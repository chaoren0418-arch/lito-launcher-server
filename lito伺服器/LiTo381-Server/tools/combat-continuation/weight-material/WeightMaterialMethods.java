import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Correct only the second material debit in an already-admitted self cast. */
public final class WeightMaterialMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",KEY=PC+".class",NAME="nativeSelfWeightBody",H=P+"NativeCastMaterial";
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static void remove(MethodNode m,AbstractInsnNode begin,AbstractInsnNode end){for(var n=begin;n!=end;){if(n==null)throw new AssertionError("unbounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(KEY)).readAllBytes());var m=c.methods.stream().filter(x->x.name.equals(NAME)).findFirst().orElseThrow();
  MethodInsnNode consume=null,replace=null,save=null,delta=null,copy=null;int admission=0;
  for(var n:m.instructions)if(n instanceof MethodInsnNode call){
   if(call.owner.equals(P+"CombatDatabase")&&call.name.equals("consumeMaterial")){if(consume!=null)throw new AssertionError();consume=call;}
   if(call.owner.equals(P+"NativeSelfCastAction")&&call.name.equals("admit")){admission++;call.owner=P+"NativeWeightMaterialAction";call.desc="(L"+PC+";L"+P+"BattleSpellCatalog$WeightSpell;)Z";}
   if(call.owner.equals(P+"NativeDeItemApplication")&&call.name.equals("replace")){if(replace!=null)throw new AssertionError();replace=call;}
   if(call.owner.equals(P+"GameStateStore")&&call.name.equals("saveEconomyState")){if(save!=null)throw new AssertionError();save=call;}
   if(call.owner.equals(PC)&&call.name.equals("sendInventoryDelta")){if(delta!=null)throw new AssertionError();delta=call;}
   if(call.owner.equals(P+"RuntimeInventory")&&call.name.equals("copy")){if(copy!=null)throw new AssertionError();copy=call;}
  }
  if(admission!=1||consume==null||replace==null||save==null||delta==null||copy==null)throw new AssertionError("unexpected weight material shape");
  var start=(AbstractInsnNode)consume;for(int i=0;i<6;i++)start=prev(start);
  if(!(start instanceof VarInsnNode v)||v.var!=15||v.getOpcode()!=ALOAD)throw new AssertionError("material args");
  m.instructions.insertBefore(start,new InsnNode(ICONST_1));remove(m,start,consume.getNext());
  var inventory=prev(copy);if(!(inventory instanceof FieldInsnNode f)||!f.name.equals("inventory"))throw new AssertionError("copy input");
  m.instructions.set(inventory,new MethodInsnNode(INVOKESTATIC,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));m.instructions.remove(copy);
  start=replace;for(int i=0;i<3;i++)start=prev(start);
  var previousStore=prev(start);if(!(previousStore instanceof VarInsnNode s)||s.getOpcode()!=ASTORE||s.var!=14)throw new AssertionError("previous store");
  var previousField=prev(previousStore);if(!(previousField instanceof FieldInsnNode pf)||!pf.name.equals("inventory"))throw new AssertionError("previous inventory");
  m.instructions.set(previousField,new MethodInsnNode(INVOKESTATIC,H,"previous","(L"+PC+";)L"+P+"RuntimeInventory;",false));remove(m,start,replace.getNext());
  inventory=prev(delta);if(!(inventory instanceof FieldInsnNode df)||!df.name.equals("inventory"))throw new AssertionError("delta inventory");
  m.instructions.set(inventory,new MethodInsnNode(INVOKESTATIC,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));
  start=save;while((start=prev(start))!=null)if(start instanceof FieldInsnNode sf&&sf.owner.equals(PC)&&sf.name.equals("state")){start=prev(start);break;}
  if(!(start instanceof VarInsnNode sv)||sv.getOpcode()!=ALOAD||sv.var!=0)throw new AssertionError("save receiver");remove(m,start,save.getNext());
  var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var projected=transform(old,loader);
  if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(KEY);Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(projected,false));return;}
  try(var next=new JarFile(args[2])){
   var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(KEY)).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(projected,true)))throw new AssertionError("projection mismatch");
   var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(KEY)).readAllBytes());var original=parent.methods.stream().filter(m->m.name.equals(NAME)).findFirst().orElseThrow();var changed=NativeSkillConsumptionMethods.method(actual,NAME,original.desc);actual.methods.set(actual.methods.indexOf(changed),original);
   if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated method changed");
   if(next.size()!=old.size()+1||next.getJarEntry(P+"NativeWeightMaterialAction.class")==null)throw new AssertionError("added entries");for(var e:old.stream().toList())if(!e.getName().equals(KEY)&&!Arrays.equals(old.getInputStream(e).readAllBytes(),next.getInputStream(next.getJarEntry(e.getName())).readAllBytes()))throw new AssertionError("unrelated entry");
  }System.out.println("PASS single weight-body inverse; admission, MP/HP, validation and other entries preserved");
 }}
}
