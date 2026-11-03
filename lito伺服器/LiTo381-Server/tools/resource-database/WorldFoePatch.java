import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Two narrowly invertible bridges; all original Foe admission, strikes, effects and costs remain. */
final class WorldFoePatch {
 static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatWorldFoe",A=H+"$Pending";
 static void apply(ClassNode c,Map<String,Integer> hooks){
  var m=c.methods.stream().filter(x->x.name.equals("useFoeSlayerSpell")).findFirst().orElseThrow();int target=DragonPatch.local(m,"target"),pending=MagicDamagePatch.appendLocal(c,m,A),captured=0,finished=0;
  for(var n:m.instructions.toArray()){
   if(n instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("resolveFoeSlayerTarget")){
    var store=DragonPatch.next(call);if(!(store instanceof VarInsnNode v)||v.getOpcode()!=ASTORE||v.var!=target)throw new IllegalStateException("original Foe target store");
    var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,target));add.add(new MethodInsnNode(INVOKESTATIC,H,"capture","(L"+C+";Ljava/lang/Object;)L"+A+";",false));add.add(new VarInsnNode(ASTORE,pending));m.instructions.insert(store,add);captured++;
   }else if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatDragon")&&call.name.equals("finishFoe")){
    if(!call.desc.equals("(L"+C+";Ljava/lang/Object;Z)V"))throw new IllegalStateException("original Foe helper descriptor");
    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,pending));m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"finish","(L"+C+";Ljava/lang/Object;ZL"+A+";)V",false));finished++;
   }
  }
  if(captured!=1||finished!=1)throw new IllegalStateException("Foe cast bridge counts "+captured+"/"+finished);
  var subset=new ClassNode();subset.version=c.version;subset.access=ACC_PUBLIC;subset.name=c.name;subset.superName=c.superName;subset.methods.addAll(List.of(m));var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){protected ClassLoader getClassLoader(){return PoisonLifecyclePatch.frameLoader;}};subset.accept(writer);var framed=new ClassNode();new ClassReader(writer.toByteArray()).accept(framed,0);for(var fixed:framed.methods)for(int i=0;i<c.methods.size();i++)if(c.methods.get(i).name.equals(fixed.name)&&c.methods.get(i).desc.equals(fixed.desc)){c.methods.set(i,fixed);break;}
  hooks.put("source-WORLD-Foe-capture-finish",2);
 }
}
