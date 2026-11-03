import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceDollAudienceMethods {
 static final String P="local/lito381/server/",N=P+"NativeSourceDollAudience",PC=P+"ClientSession",CTX=P+"NativeAreaMaterialAction$Context",VIEW=P+"NativeSourceDollSkills$View",OUT=P+"NativeSourceDollOutput",REC=OUT+"$Receiver",FIELD="nativeSourceDollAudienceProof";
 static final List<String> OWNERS=List.of(CTX,VIEW,OUT,REC);static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 static MethodNode ctor(MethodNode old){var m=new MethodNode(old.access,"<init>",SourceNpcFirstViewMethods.extra(old.desc),null,null);m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new VarInsnNode(ALOAD,2));m.instructions.add(new MethodInsnNode(INVOKESPECIAL,REC,"<init>",old.desc,false));m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,3));m.instructions.add(new FieldInsnNode(PUTFIELD,REC,FIELD,"Ljava/lang/Object;"));m.instructions.add(new InsnNode(RETURN));m.maxLocals=4;return m;}
 static void patch(ClassNode c){
  if(c.name.equals(REC)){var old=c.methods.stream().filter(m->m.name.equals("<init>")).findFirst().orElseThrow();c.fields.add(new FieldNode(ACC_FINAL|ACC_TRANSIENT,FIELD,"Ljava/lang/Object;",null,null));c.methods.add(ctor(old));return;}
  int capture=0,hub=0,copies=0,select=0,current=0;
  for(var m:c.methods){
   if(c.name.equals(CTX)&&m.name.equals("<init>")&&m.desc.equals("(L"+PC+";)V"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceFirstPlayers")&&call.name.equals("capture")){call.owner=N;capture++;}
   if(c.name.equals(VIEW)&&m.name.equals("<init>"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
    if(call.owner.equals(P+"GameHub")&&call.name.equals("roomMembers")){m.instructions.insertBefore(call,new VarInsnNode(ALOAD,1));call.setOpcode(INVOKESTATIC);call.owner=N;call.name="members";call.desc="(L"+P+"GameHub;JL"+PC+";)Ljava/util/List;";call.itf=false;hub++;}
    else if(call.owner.equals("java/util/List")&&call.name.equals("copyOf")){m.instructions.insertBefore(call,new VarInsnNode(ALOAD,6));m.instructions.insertBefore(call,new VarInsnNode(ALOAD,1));call.owner=N;call.name="copy";call.desc="(Ljava/util/Collection;L"+CTX+";L"+PC+";)Ljava/util/List;";call.itf=false;copies++;}
   }
   if(c.name.equals(OUT)&&m.name.equals("snapshot"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(REC)&&call.name.equals("<init>")){
    AbstractInsnNode create=call;while((create=FreezeAcceptedMethods.prev(create))!=null)if(create instanceof TypeInsnNode t&&t.getOpcode()==NEW&&t.desc.equals(REC))break;
    need(create!=null&&FreezeSourceTargetDelta.next(create).getOpcode()==DUP,"exact original Receiver allocation");m.instructions.remove(FreezeSourceTargetDelta.next(create));m.instructions.remove(create);m.instructions.insertBefore(call,new VarInsnNode(ALOAD,0));call.setOpcode(INVOKESTATIC);call.owner=N;call.name="select";call.desc="(L"+PC+";L"+P+"NativePlayerCommit$Binding;L"+VIEW+";)L"+REC+";";select++;
   }
   if(c.name.equals(OUT)&&m.name.equals("current")){var ok=new LabelNode();var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,N,"current","(L"+OUT+"$Output;)Z",false));x.add(new JumpInsnNode(IFNE,ok));x.add(new InsnNode(ICONST_0));x.add(new InsnNode(IRETURN));x.add(ok);m.instructions.insert(x);current++;}
  }
  need(c.name.equals(CTX)?capture==1:c.name.equals(VIEW)?hub==1&&copies==2:select==1&&current==1,"all exact original audience data ports "+c.name);
 }
 public static void main(String[]a)throws Exception{var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var f=Files.list(Path.of(a[3]))){for(var p:f.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var old=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=loader;for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(old,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(owner+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var jar=new JarFile(a[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)),"same frozen audience splice");}}
   if(a[0].equals("splice"))Files.write(Path.of(a[2]).getParent().resolve("doll-audience-ports.tsv"),List.of("owner\tmethod\tphase",CTX+"\t<init>\toriginal under-Hub first observer/life/binding",VIEW+"\t<init>\toriginal room-members and two immutable-list branch ports",OUT+"\tsnapshot/current\tselect original receiver and read-only final life/observer proof",REC+"\t<init>\texact original delegating ctor; final transient noncanonical proof"));
  }System.out.println("PASS original first doll audience and detached final receiver life/observer proof");
 }
}
