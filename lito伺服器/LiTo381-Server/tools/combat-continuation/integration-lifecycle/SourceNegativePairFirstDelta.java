import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceNegativePairFirstDelta {
 static final String P="local/lito381/server/",AREA=P+"NativeAreaMaterialAction",AIM=P+"NativeSourceNegativeTargets$Aim",H=P+"NativeSourceNegativePairFirst";
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void load(MethodNode m,AbstractInsnNode n,int slot){var p=FreezeAcceptedMethods.prev(n);need(p instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"bounded original operand "+slot);m.instructions.remove(p);}
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[0]);var jar=new JarFile(args[1])){
   var names=new HashSet<String>();var changed=new HashSet<String>();for(var e:Collections.list(old.entries())){names.add(e.getName());need(jar.getJarEntry(e.getName())!=null,"retain original archive entry");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(jar.getJarEntry(e.getName())).readAllBytes()))changed.add(e.getName());}
   need(changed.equals(Set.of(AREA+".class",AIM+".class")),"only two reviewed original owners changed");var fresh=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!names.contains(e.getName()))fresh.add(e.getName());need(fresh.equals(Set.of(H+".class",H+"$First.class")),"one fieldless helper and bounded immutable first value only");int methods=0,ports=0;
   for(var owner:List.of(AREA,AIM)){
    var before=FreezeSourceTargetDelta.read(old,owner);var after=FreezeSourceTargetDelta.read(jar,owner);
    if(owner.equals(AIM)){var field=after.fields.stream().filter(f->f.name.equals("nativeSourceNegativePairFirst")).findFirst().orElseThrow();need(field.access==ACC_TRANSIENT&&field.desc.equals("Ljava/lang/Object;"),"one transient instance first-proof slot");after.fields.remove(field);}
    need(before.fields.size()==after.fields.size()&&before.methods.size()==after.methods.size(),"all original schema and methods retained");for(int i=0;i<before.fields.size();i++){var a=before.fields.get(i);var b=after.fields.get(i);need(a.name.equals(b.name)&&a.desc.equals(b.desc)&&a.access==b.access&&Objects.equals(a.signature,b.signature)&&Objects.equals(a.value,b.value),"original field exact");}
    for(var original:before.methods){var m=after.methods.stream().filter(x->x.name.equals(original.name)&&x.desc.equals(original.desc)).findFirst().orElseThrow();for(var node:m.instructions.toArray())if(m.instructions.contains(node)&&node instanceof MethodInsnNode call&&call.owner.equals(H)){
     switch(call.name){
      case "capture","close"->{need(owner.equals(AIM)&&m.name.equals(call.name.equals("capture")?"<init>":"close"),"original first/cleanup method only");load(m,call,0);m.instructions.remove(call);}
      case "current"->{need(owner.equals(AIM)&&m.name.equals("valid"),"original Aim validation only");load(m,call,0);var branch=FreezeSourceTargetDelta.next(call);var value=FreezeSourceTargetDelta.next(branch);var ret=FreezeSourceTargetDelta.next(value);need(branch.getOpcode()==IFNE&&value.getOpcode()==ICONST_0&&ret.getOpcode()==IRETURN,"false proof returns through original valid contract");for(var n:List.of(call,branch,value,ret))m.instructions.remove(n);}
      case "retry"->{need(owner.equals(AREA)&&m.name.equals("nativeCompanionAreaCapturedRun"),"original retry only");load(m,call,3);call.owner=P+"NativePlayerCommit$Binding";call.name="matches";call.desc="(L"+P+"ClientSession;)Z";call.setOpcode(INVOKEVIRTUAL);call.itf=false;}
      case "scene"->{need(owner.equals(AREA)&&m.name.equals("nativeCompanionAreaCapturedRun"),"original precommit scene only");call.owner=P+"NativeSourceAreaPlayerProof";}
      default->throw new AssertionError("unexpected first-proof port "+call.name);
     }ports++;
    }need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,m,original.name)),"complete original method inverse "+owner+"."+original.name);methods++;}
   }need(ports==5,"five explicit bounded ports");System.out.println("PASS independent negative first inverse: "+methods+" complete original methods,2 original owners,5 explicit ports,1 transient first slot,1 fieldless helper; original costs/RNG/locks/handlers unchanged");
  }
 }
}
