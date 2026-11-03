import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Complete the native S_BLINK -> C_BLINK -> S_PARALYSE(17) handshake. */
final class MovementLockPatch implements Opcodes {
 static final String P="local/lito381/server/";
 static void protocol(ClassNode c){
  if(c.methods.stream().anyMatch(m->m.name.equals("movementLockRelease")))throw new IllegalStateException("duplicate movement release");
  var m=new MethodNode(ACC_STATIC,"movementLockRelease","()[B",null,null);
  m.instructions.add(new InsnNode(ICONST_2));m.instructions.add(new IntInsnNode(NEWARRAY,T_BYTE));
  m.instructions.add(new InsnNode(DUP));m.instructions.add(new InsnNode(ICONST_0));m.instructions.add(new IntInsnNode(BIPUSH,0x25));m.instructions.add(new InsnNode(BASTORE));
  m.instructions.add(new InsnNode(DUP));m.instructions.add(new InsnNode(ICONST_1));m.instructions.add(new IntInsnNode(BIPUSH,17));m.instructions.add(new InsnNode(BASTORE));m.instructions.add(new InsnNode(ARETURN));c.methods.add(m);
 }
 static void session(ClassNode c){
  int count=0;
  for(var m:c.methods)if(m.name.equals("movementUnlock")&&m.desc.equals("([B)V"))for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(c.name)&&f.name.equals("movementLockPending")){
   // Insert before clearing the pending flag; failed writes retain pending state.
   var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,P+"Protocol","movementLockRelease","()[B",false));i.add(new LdcInsnNode("S_PARALYSE movement-lock-release mode=17"));i.add(new MethodInsnNode(INVOKEVIRTUAL,c.name,"sendEncrypted","([BLjava/lang/String;)V",false));m.instructions.insertBefore(n,i);count++;
  }
  if(count!=1)throw new IllegalStateException("movement unlock hook drift: "+count);
 }
}
