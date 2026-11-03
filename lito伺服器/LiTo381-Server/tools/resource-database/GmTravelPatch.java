import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
final class GmTravelPatch implements Opcodes {
 static final String P="local/lito381/server/";
 static void apply(ClassNode c){
  wrap(c,"handleGmChatCommand","(Ljava/lang/String;)Z","command",IRETURN);
  wrap(c,"useInventoryItem","([B)V","useItem",RETURN);
 }
 static void wrap(ClassNode c,String name,String desc,String helper,int ret){
  var old=c.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow();old.name=name+"BeforeTravel";
  var m=new MethodNode(ACC_PRIVATE,name,desc,null,new String[]{"java/lang/Exception"});
  m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,1));
  m.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"GmTravel",helper,"(L"+P+"ClientSession;"+desc.substring(1),false));
  m.instructions.add(new InsnNode(ret));c.methods.add(m);
 }
}
