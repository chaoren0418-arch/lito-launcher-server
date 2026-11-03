import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceDollSlowMethods {
 static final String P="local/lito381/server/",N=P+"NativeSourceDollSlow",PC=P+"ClientSession",SK=P+"NativeSourceDollSkills",TS=P+"BattleTimedStatus";
 static final String SPEED=P+"NativeSourceDollOutput$Speed",EPOCH="nativeSourceDollSlowEpoch";
 static final List<String>TIMERS=List.of(TS,P+"BattleCompanion",P+"BattleQuestEncounter$Monster",P+"BattleTimeAttackEncounter$Monster");
 static final List<String>TARGETS=List.of(PC,SK,TS,TIMERS.get(1),TIMERS.get(2),TIMERS.get(3),SPEED);
 static void patch(ClassNode c){int count=0;
  if(TIMERS.contains(c.name))c.fields.add(new FieldNode(ACC_VOLATILE,EPOCH,"Ljava/lang/Object;",null,null));
  if(c.name.equals(SPEED)){
   c.fields.add(new FieldNode(ACC_FINAL,EPOCH,"Ljava/lang/Object;",null,null));int ctors=0,returns=0;
   for(var m:c.methods){
    if(m.name.equals("<init>"))for(var node:m.instructions.toArray())if(node.getOpcode()==RETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,N,"epoch","(L"+SPEED+";)Ljava/lang/Object;",false));x.add(new FieldInsnNode(PUTFIELD,SPEED,EPOCH,"Ljava/lang/Object;"));m.instructions.insertBefore(node,x);ctors++;}
    if(m.name.equals("current"))for(var node:m.instructions.toArray())if(node.getOpcode()==IRETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,N,"sameEpoch","(L"+SPEED+";)Z",false));x.add(new InsnNode(IAND));m.instructions.insertBefore(node,x);returns++;}
   }if(ctors!=1||returns!=1)throw new AssertionError("exact detached speed epoch capture/return");return;
  }
  if(TIMERS.contains(c.name)&&!c.name.equals(TS))return;
  for(var m:c.methods){
   if(c.name.equals(TS)&&m.name.equals("speed")&&m.desc.equals("(J)L"+TS+"$Speed;")){
    var pass=new LabelNode();var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(LLOAD,1));x.add(new MethodInsnNode(INVOKESTATIC,N,"extendedSpeed","(L"+TS+";J)L"+TS+"$Speed;",false));x.add(new InsnNode(DUP));x.add(new JumpInsnNode(IFNULL,pass));x.add(new InsnNode(ARETURN));x.add(pass);x.add(new InsnNode(POP));m.instructions.insert(x);count++;
   }
   for(var node:m.instructions.toArray())if(node instanceof MethodInsnNode call){
    if(c.name.equals(PC)&&m.name.equals("nativeStaticOriginalFrame")&&call.owner.equals(P+"NativeSourceDollOutput")&&call.name.equals("allowFrame")){call.owner=N;count++;}
    if(c.name.equals(SK)&&m.name.equals("apply")&&call.owner.equals(SK)&&call.name.equals("slow")){call.owner=N;count++;}
   }
  }if(count!=1)throw new AssertionError("one explicit port in "+c.name+": "+count);
 }
 public static void main(String[]a)throws Exception{var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var f=Files.list(Path.of(a[3]))){for(var p:f.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}try(var hierarchy=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=hierarchy;for(var name:TARGETS){var c=FreezeAcceptedMethods.read(base,name);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var next=new JarFile(a[2])){if(!Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(next,name),false)))throw new AssertionError(name);}}}System.out.println("PASS bounded Source slow duration/innate-haste/frame and detached lease epoch ports");}
}
