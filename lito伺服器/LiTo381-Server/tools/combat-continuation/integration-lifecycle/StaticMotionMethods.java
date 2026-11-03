import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Narrow ports; retain canonical catalog references at every identity/authority gate. */
public final class StaticMotionMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",HUB=P+"GameHub",NPC=P+"ClassicWorldNpcCatalog$StaticNpc",H=P+"NativeStaticNpcMotion";
 static final List<String> TYPES=List.of("ClientSession","GameHub","NativeStaticNpcScene","NativeStaticNpcScene$State","ClassicWorldMonsterPopulation","NativePlayerCommit","NativeCancellationNpcStatic","NativeCancellationNpcStatic$Target","CombatSpecialBuffs");
 static ClassNode read(JarFile jar,String name)throws Exception{var c=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(P+name+".class"))).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
 static void field(ClassNode c,int access,String name,String desc){if(c.fields.stream().anyMatch(f->f.name.equals(name)))throw new AssertionError("duplicate "+name);c.fields.add(new FieldNode(access,name,desc,null,null));}
 static void call(InsnList i,String name,String desc){i.add(new MethodInsnNode(INVOKESTATIC,H,name,desc,false));}
 static void pc(InsnList i,int local){i.add(new VarInsnNode(ALOAD,local));}
 static void wrapper(ClassNode c,String name,String renamed,String helper,String desc){var old=method(c,name);old.name=renamed;old.access=old.access&~ACC_PRIVATE;var m=new MethodNode(ASM9,old.access&~(ACC_SYNCHRONIZED|ACC_SYNTHETIC),name,old.desc,old.signature,old.exceptions.toArray(String[]::new));m.instructions.add(new VarInsnNode(ALOAD,0));int local=1;for(var type:Type.getArgumentTypes(old.desc)){m.instructions.add(new VarInsnNode(type.getOpcode(ILOAD),local));local+=type.getSize();}call(m.instructions,helper,desc);m.instructions.add(new InsnNode(RETURN));c.methods.add(m);}
 static void patch(ClassNode c){int count=0;
  if(c.name.equals(PC)){
   wrapper(c,"sendEncryptedFrame","nativeStaticOriginalFrame","frame","(L"+PC+";[BLjava/lang/String;)V");
   wrapper(c,"moveClassicWorld","nativeStaticOriginalMove","moved","(L"+PC+";IIIII)V");
   wrapper(c,"openClassicWorldHypertext","nativeStaticOriginalDialog","dialog","(L"+PC+";J)V");
   int companionMoves=0;for(var m:c.methods)for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call){
    if(call.owner.equals(P+"BattleCompanion")&&call.name.equals("moveOneTile")){if((m.access&ACC_STATIC)!=0||!call.desc.equals("(IIIJ)L"+P+"BattleCompanion$Movement;"))throw new AssertionError("unbound companion landing "+m.name);m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.setOpcode(INVOKESTATIC);call.owner=H;call.name="companionMove";call.desc="(L"+P+"BattleCompanion;IIIJL"+PC+";)L"+P+"BattleCompanion$Movement;";companionMoves++;}
    if(call.owner.equals(NPC)&&Set.of("x","y").contains(call.name)&&(m.access&ACC_STATIC)==0){m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+NPC+";L"+PC+";)I";count++;}
    if(call.owner.equals(P+"ClassicWorldNpcCatalog")&&Set.of("inScreen","occupies").contains(call.name)&&(m.access&ACC_STATIC)==0){m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.owner=H;call.desc=call.desc.replace(")","L"+PC+";)");count++;}
    if(m.name.equals("callClanDestinationOccupied")&&call.owner.equals(P+"ClassicWorldNpcCatalog")&&call.name.equals("all")){m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.owner=H;call.name="projectedAll";call.desc="(IL"+PC+";)Ljava/util/List;";count++;}
   }
   var dialog=method(c,"nativeStaticOriginalDialog");int talks=0;for(var i:dialog.instructions.toArray())if(i instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.name.equals("lastClassicNpcActionObjectId")){
    // Only the canonical static-NPC assignment has StaticNpc.objectId immediately before it.
    var prev=i.getPrevious();while(prev!=null&&prev.getOpcode()<0)prev=prev.getPrevious();if(prev instanceof MethodInsnNode id&&id.owner.equals(NPC)&&id.name.equals("objectId")){
     var load=prev.getPrevious();while(load.getOpcode()<0)load=load.getPrevious();if(!(load instanceof VarInsnNode v)||v.getOpcode()!=ALOAD)throw new AssertionError("canonical dialog local");var add=new InsnList();pc(add,0);pc(add,v.var);call(add,"talk","(L"+PC+";L"+NPC+";)V");dialog.instructions.insert(i,add);talks++;
    }
   }if(talks!=1||count!=144||companionMoves!=2)throw new AssertionError("PC motion ports "+count+" talk="+talks+" companion landings="+companionMoves);
  }else if(c.name.equals(HUB)){
   field(c,ACC_PRIVATE|ACC_VOLATILE,"nativeStaticMotionClosed","Z");
   field(c,ACC_PRIVATE,"nativeStaticMotionRestTick","J");
   var state=c.fields.stream().filter(f->f.name.equals("nativeStaticNpcScenes")).findFirst().orElseThrow();state.access|=ACC_VOLATILE;
   var tick=method(c,"companionTickSafely");var add=new InsnList();pc(add,0);call(add,"tickSafely","(L"+HUB+";)V");tick.instructions.insert(add);
   var population=method(c,"classicWorldMonsterPopulation");for(var i:population.instructions.toArray())if(i.getOpcode()==ARETURN){add=new InsnList();pc(add,0);add.add(new VarInsnNode(ILOAD,1));add.add(new VarInsnNode(ILOAD,2));call(add,"bound","(L"+P+"ClassicWorldMonsterPopulation;L"+HUB+";II)L"+P+"ClassicWorldMonsterPopulation;");population.instructions.insertBefore(i,add);count++;}if(count!=3)throw new AssertionError("population scene bindings "+count);
  }else if(c.name.equals(P+"NativeStaticNpcScene$State")){field(c,ACC_VOLATILE,"nativeStaticMotion","Ljava/lang/Object;");
  }else if(c.name.equals(P+"NativeStaticNpcScene")){
   for(var m:c.methods){
    if(m.name.equals("prune")||m.name.equals("close")){var add=new InsnList();pc(add,0);call(add,m.name.equals("close")?"stop":"prune","(L"+HUB+";)V");m.instructions.insert(add);}
    for(var i:m.instructions.toArray()){
     if(i instanceof MethodInsnNode call&&call.owner.equals("java/util/Map")&&call.name.equals("isEmpty")&&Set.of("trim","prune").contains(m.name)){m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.setOpcode(INVOKESTATIC);call.owner=H;call.name="empty";call.desc="(Ljava/util/Map;L"+HUB+";)Z";call.itf=false;count++;}
     if(i.getOpcode()==IRETURN&&m.name.equals("heading")){var add=new InsnList();pc(add,0);pc(add,1);pc(add,2);call(add,"heading","(IL"+HUB+";L"+NPC+";L"+PC+";)I");m.instructions.insertBefore(i,add);}
     if(i.getOpcode()==IRETURN&&m.name.equals("inScene")){var add=new InsnList();pc(add,1);call(add,"alive","(ZL"+HUB+";)Z");m.instructions.insertBefore(i,add);}
     if(i.getOpcode()==IRETURN&&m.name.equals("turn")){var add=new InsnList();pc(add,0);pc(add,1);add.add(new VarInsnNode(ILOAD,2));pc(add,3);call(add,"turned","(ZL"+HUB+";L"+NPC+";IL"+PC+";)Z");m.instructions.insertBefore(i,add);}
     if((i.getOpcode()==IRETURN||i.getOpcode()==RETURN)&&Set.of("transform","restore","appearance").contains(m.name)){var add=new InsnList();pc(add,0);call(add,"changed","(L"+HUB+";)V");m.instructions.insertBefore(i,add);}
    }
   }if(count!=2)throw new AssertionError("scene retention "+count);
  }else if(c.name.equals(P+"ClassicWorldMonsterPopulation")){
   field(c,ACC_VOLATILE,"nativeStaticMotionHub","Ljava/lang/ref/WeakReference;");field(c,ACC_VOLATILE,"nativeStaticMotionInstance","I");
   for(var m:c.methods)for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(P+"ClassicWorldNpcCatalog")&&call.name.equals("occupies")){if((m.access&ACC_STATIC)!=0)throw new AssertionError("unbound NPC consumer");m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.owner=H;call.desc="(IIIL"+P+"ClassicWorldMonsterPopulation;)Z";count++;}if(count!=9)throw new AssertionError("NPC occupancy edges "+count);
  }else if(c.name.equals(P+"NativePlayerCommit")){
   var old=method(c,"advance");old.name="nativeStaticOriginalAdvance";var m=new MethodNode(ASM9,ACC_STATIC,"advance",old.desc,null,null);pc(m.instructions,0);call(m.instructions,"advance",old.desc);m.instructions.add(new InsnNode(RETURN));c.methods.add(m);
  }else if(c.name.equals(P+"NativeCancellationNpcStatic")){
   for(var m:c.methods){
    if(m.name.equals("putObject")){var add=new InsnList();pc(add,0);pc(add,3);call(add,"touch","(L"+NPC+";L"+PC+";)V");m.instructions.insert(add);}
    for(var i:m.instructions.toArray()){
     if(i instanceof MethodInsnNode call&&call.owner.equals(NPC)&&Set.of("x","y").contains(call.name)){int local=m.name.equals("putObject")?3:m.name.equals("cast")?0:-1;if(local<0)throw new AssertionError("static cast coordinates "+m.name);m.instructions.insertBefore(i,new VarInsnNode(ALOAD,local));call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+NPC+";L"+PC+";)I";count++;}
     if(i.getOpcode()==IRETURN&&m.name.equals("restored")){var add=new InsnList();pc(add,1);call(add,"changed","(L"+HUB+";)V");m.instructions.insertBefore(i,add);}
    }
   }if(count!=8)throw new AssertionError("static cancellation coords "+count);
  }else if(c.name.equals(P+"NativeCancellationNpcStatic$Target")){
   for(var m:c.methods)for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(NPC)&&Set.of("x","y").contains(call.name)){var add=new InsnList();pc(add,0);add.add(new FieldInsnNode(GETFIELD,c.name,"pc","L"+PC+";"));m.instructions.insertBefore(i,add);call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+NPC+";L"+PC+";)I";count++;}if(count!=2)throw new AssertionError("Source static target coords "+count);
  }else if(c.name.equals(P+"CombatSpecialBuffs")){
   for(var m:c.methods)if(m.name.equals("tryTalk"))for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(NPC)&&Set.of("x","y").contains(call.name)){m.instructions.insertBefore(i,new VarInsnNode(ALOAD,0));call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+NPC+";L"+PC+";)I";count++;}if(count!=2)throw new AssertionError("special talk coordinates "+count);
  }else throw new AssertionError(c.name);
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1])){for(var name:TYPES){var c=read(old,name);patch(c);var expected=bytes(c);if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(P+name+".class");Files.createDirectories(out.getParent());Files.write(out,expected);}else try(var now=new JarFile(args[2])){if(!Arrays.equals(expected,bytes(read(now,name))))throw new AssertionError("selective motion regeneration "+name);}}}System.out.println("PASS selective static motion regeneration; no PC fields or new timers");}
}
