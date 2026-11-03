import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Preserve the native handler except its material/item transaction boundary. */
public final class MagicalWeaponMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativeCastMaterial",A=P+"NativeMagicalWeaponAction";
 static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
 static void remove(MethodNode m,AbstractInsnNode first,AbstractInsnNode end){for(var n=first;n!=end;){if(n==null)throw new AssertionError("bounded slice");var next=n.getNext();m.instructions.remove(n);n=next;}}
 static MethodNode clone(MethodNode m){var n=new MethodNode(ASM9,m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static void call(InsnList x,String owner,String name,String desc){x.add(new MethodInsnNode(INVOKESTATIC,owner,name,desc,false));}
 static ClassNode patch(JarFile jar,ClassLoader loader)throws Exception {
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());
  var original=c.methods.stream().filter(m->m.name.equals("useMagicalWeaponSpell")).findFirst().orElseThrow();
  var body=clone(original);body.name="nativeMaterialMagicalWeaponBody";body.access&=~ACC_PRIVATE;
  MethodInsnNode copy=null,replace=null,save=null,delta=null;int enters=0,exits=0;
  for(var n:body.instructions.toArray()) {
   if(n.getOpcode()==MONITORENTER){body.instructions.set(n,new InsnNode(POP));enters++;}
   else if(n.getOpcode()==MONITOREXIT){body.instructions.set(n,new InsnNode(POP));exits++;}
   if(n instanceof MethodInsnNode call){
    if(call.owner.equals(P+"RuntimeInventory")&&call.name.equals("copy"))copy=call;
    if(call.owner.equals(P+"NativeDeItemApplication")&&call.name.equals("replace"))replace=call;
    if(call.owner.equals(P+"GameStateStore")&&call.name.equals("saveEconomyState"))save=call;
    if(call.owner.equals(PC)&&call.name.equals("sendInventoryDelta"))delta=call;
   }
  }
  if(enters!=1||exits!=4||copy==null||replace==null||save==null||delta==null)throw new AssertionError("reviewed body shape "+enters+"/"+exits+" replace="+replace);
  var start=prev(prev(copy));var end=replace.getNext();
  if(!(start instanceof VarInsnNode v)||v.getOpcode()!=ALOAD||v.var!=0)throw new AssertionError("copy start");
  // Locals are verified against the retained method before replacing the slice.
  int[] stores={16,17,18,20,26};for(int slot:stores){boolean found=false;for(var n=start;n!=end;n=n.getNext())if(n instanceof VarInsnNode x&&(x.getOpcode()==ASTORE||x.getOpcode()==ISTORE)&&x.var==slot)found=true;if(!found)throw new AssertionError("missing local "+slot);}
  remove(body,start,end);var x=new InsnList();var accepted=new LabelNode();
  x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,2));x.add(new VarInsnNode(ALOAD,3));x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"Protocol$ObjectSpellRequest","targetObjectId","()J",false));
  call(x,A,"admit","(L"+PC+";L"+P+"BattleSpellCatalog$MagicalWeaponSpell;J)Z");x.add(new JumpInsnNode(IFNE,accepted));x.add(new InsnNode(RETURN));x.add(accepted);
  call(x,"java/lang/System","nanoTime","()J");x.add(new VarInsnNode(LSTORE,21));
  x.add(new VarInsnNode(ALOAD,0));call(x,H,"previous","(L"+PC+";)L"+P+"RuntimeInventory;");x.add(new VarInsnNode(ASTORE,16));
  for(int slot:new int[]{17,18}){
   if(slot==17)x.add(new VarInsnNode(ALOAD,16));else{x.add(new VarInsnNode(ALOAD,0));call(x,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;");}
   x.add(new VarInsnNode(ALOAD,3));x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"Protocol$ObjectSpellRequest","targetObjectId","()J",false));
   x.add(new MethodInsnNode(INVOKEVIRTUAL,P+"RuntimeInventory","item","(J)L"+P+"RuntimeInventory$Item;",false));x.add(new VarInsnNode(ASTORE,slot));
  }
  x.add(new VarInsnNode(ALOAD,0));call(x,A,"enchanted","(L"+PC+";)Z");x.add(new VarInsnNode(ISTORE,20));body.instructions.insertBefore(end,x);
  AbstractInsnNode saveStart=save;while((saveStart=prev(saveStart))!=null)if(saveStart instanceof FieldInsnNode f&&f.owner.equals(PC)&&f.name.equals("state")){saveStart=prev(saveStart);break;}
  if(!(saveStart instanceof VarInsnNode q)||q.getOpcode()!=ALOAD||q.var!=0)throw new AssertionError("save expression");remove(body,saveStart,save.getNext());
  var field=prev(prev(delta));if(!(field instanceof FieldInsnNode f)||!f.name.equals("inventory"))throw new AssertionError("delta inventory");body.instructions.set(field,new MethodInsnNode(INVOKESTATIC,H,"next","(L"+PC+";)L"+P+"RuntimeInventory;",false));
  body.tryCatchBlocks.removeIf(t->body.instructions.indexOf(t.start)<0||body.instructions.indexOf(t.end)<0||body.instructions.indexOf(t.handler)<0);
  var wrapper=new MethodNode(ASM9,original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));for(int i=0;i<3;i++)wrapper.instructions.add(new VarInsnNode(ALOAD,i));call(wrapper.instructions,A,"cast","(L"+PC+";"+original.desc.substring(1));wrapper.instructions.add(new InsnNode(RETURN));
  c.methods.set(c.methods.indexOf(original),wrapper);c.methods.add(body);var methods=List.of(body,wrapper);var framed=NativeSkillConsumptionMethods.frames(c,methods,loader);for(var m:methods)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[] args)throws Exception {
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(patch(old,loader),false));return;}
   try(var candidate=new JarFile(args[2])){
    var actual=NativeSkillConsumptionMethods.read(candidate.getInputStream(candidate.getJarEntry(PC+".class")).readAllBytes());
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(patch(old,loader),true)))throw new AssertionError("transformed PC mismatch");
    var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());var original=parent.methods.stream().filter(m->m.name.equals("useMagicalWeaponSpell")).findFirst().orElseThrow();var wrapper=actual.methods.stream().filter(m->m.name.equals(original.name)).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(wrapper),clone(original));if(!actual.methods.removeIf(m->m.name.equals("nativeMaterialMagicalWeaponBody")))throw new AssertionError("missing alias");
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC changes");System.out.println("PASS independent PC inverse: one handler, no fields, original output tail retained");
   }
  }
 }
}
