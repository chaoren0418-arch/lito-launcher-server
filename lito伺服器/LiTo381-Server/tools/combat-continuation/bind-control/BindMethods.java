import java.nio.file.*;import java.util.*;import java.util.jar.*;import java.net.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class BindMethods implements Opcodes {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"CombatBindControl";
 static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new LinkedHashSet<MethodNode>();
  var reject=NativeSkillConsumptionMethods.method(c,"rejectWhileParalyzed","(Ljava/lang/String;)Z");var movement=new MethodNode(ASM9,reject.access,"nativeRejectMovementLocked",reject.desc,reject.signature,reject.exceptions.toArray(String[]::new));reject.accept(movement);c.methods.add(movement);changed.add(movement);
  var expected=Set.of("rejectWhileParalyzed","useTeleportSpell","centeredAreaCasterStillAdmitted","decayPotionCasterStillAdmitted","counterDetectionCasterStillAdmitted");var seen=new HashSet<String>();int moves=0;
  for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
   if(expected.contains(m.name)&&call.owner.equals(P+"BattleTimedStatus")&&call.name.equals("actionLocked")){call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+P+"BattleTimedStatus;J)Z";seen.add(m.name);changed.add(m);}
   if(m.name.equals("move")&&call.owner.equals(PC)&&call.name.equals("rejectWhileParalyzed")){call.name="nativeRejectMovementLocked";changed.add(m);moves++;}
  }
  if(!seen.equals(expected)||moves!=1)throw new AssertionError("action/movement census changed "+seen+" "+moves);
  var teleport=c.methods.stream().filter(m->m.name.equals("useTeleportSpell")).findFirst().orElseThrow();int flag=teleport.maxLocals++;var initialize=new InsnList();initialize.add(new InsnNode(ICONST_0));initialize.add(new VarInsnNode(ISTORE,flag));teleport.instructions.insert(initialize);
  MethodInsnNode clear=null,reveal=null;FieldInsnNode tail=null;
  for(var n:teleport.instructions.toArray()){
   if(n instanceof MethodInsnNode call&&call.owner.equals(PC)){if(call.name.equals("clearPendingAskCallbacks")){if(clear!=null)throw new AssertionError();clear=call;}if(call.name.equals("nativeSpellRevealControlled")){if(reveal!=null)throw new AssertionError();reveal=call;}}
   if(n instanceof FieldInsnNode field&&field.owner.equals(PC)&&field.name.equals("pendingClassicWorldPortal")&&field.getOpcode()==PUTFIELD){if(tail!=null)throw new AssertionError();tail=field;}
  }
  if(clear==null||reveal==null||tail==null||!(previous(clear) instanceof VarInsnNode v&&v.var==0&&v.getOpcode()==ALOAD))throw new AssertionError("teleport effect boundary changed");
  var afterPending=new LabelNode();teleport.instructions.insert(tail,afterPending);var check=new InsnList();check.add(new VarInsnNode(ALOAD,0));check.add(new MethodInsnNode(INVOKESTATIC,H,"blocked","(L"+PC+";)Z",false));check.add(new VarInsnNode(ISTORE,flag));check.add(new VarInsnNode(ILOAD,flag));check.add(new JumpInsnNode(IFNE,afterPending));teleport.instructions.insertBefore(previous(clear),check);
  var out=new InsnList();var allowed=new LabelNode();out.add(new VarInsnNode(ILOAD,flag));out.add(new JumpInsnNode(IFEQ,allowed));out.add(new VarInsnNode(ALOAD,0));out.add(new MethodInsnNode(INVOKESTATIC,H,"rejected","(L"+PC+";)V",false));out.add(new InsnNode(RETURN));out.add(allowed);teleport.instructions.insert(reveal,out);changed.add(teleport);
  var tree=c.methods.stream().filter(m->m.name.equals("useWorldTreeCallSpell")).findFirst().orElseThrow();int portals=0;
  for(var n:tree.instructions.toArray())if(n instanceof TypeInsnNode t&&t.getOpcode()==NEW&&t.desc.equals(P+"Protocol$PortalDestination")){
   var test=new InsnList();var yes=new LabelNode();test.add(new VarInsnNode(ALOAD,0));test.add(new MethodInsnNode(INVOKESTATIC,H,"blocked","(L"+PC+";)Z",false));test.add(new JumpInsnNode(IFEQ,yes));test.add(new VarInsnNode(ALOAD,0));test.add(new MethodInsnNode(INVOKESTATIC,H,"rejected","(L"+PC+";)V",false));test.add(new InsnNode(RETURN));test.add(yes);tree.instructions.insertBefore(n,test);portals++;
  }
  if(portals!=1)throw new AssertionError("world tree portal boundary changed "+portals);changed.add(tree);
  var framed=NativeSkillConsumptionMethods.frames(c,new ArrayList<>(changed),loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{
  try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
   var transformed=transform(old,loader);
   if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(transformed,false));return;}
   try(var next=new JarFile(args[2])){
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(transformed,true)))throw new AssertionError("transformation differs");var original=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());
    for(var m:actual.methods)if(Set.of("move","rejectWhileParalyzed","useTeleportSpell","useWorldTreeCallSpell","centeredAreaCasterStillAdmitted","decayPotionCasterStillAdmitted","counterDetectionCasterStillAdmitted").contains(m.name)){var before=NativeSkillConsumptionMethods.method(original,m.name,m.desc);actual.methods.set(actual.methods.indexOf(m),before);}actual.methods.removeIf(m->m.name.equals("nativeRejectMovementLocked"));if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(original,true)))throw new AssertionError("unrelated PC mutation");
    var name=P+"CombatRules.class";var before=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(name)).readAllBytes());var after=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(name)).readAllBytes());for(String method:List.of("<clinit>","snapshot")){var a=before.methods.stream().filter(m->m.name.equals(method)).findFirst().orElseThrow();var b=NativeSkillConsumptionMethods.method(after,a.name,a.desc);after.methods.set(after.methods.indexOf(b),a);}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(before,true),NativeSkillConsumptionMethods.bytes(after,true)))throw new AssertionError("unrelated control change");System.out.println("PASS seven PC methods, movement clone, parser only; NPC locks unchanged");
   }
  }
 }
}
