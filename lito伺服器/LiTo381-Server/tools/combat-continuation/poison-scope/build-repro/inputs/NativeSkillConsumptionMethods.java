import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Typed call-site correction, preserving original validation and every original cost method. */
public final class NativeSkillConsumptionMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",CAT=P+"BattleSpellCatalog$",H=P+"NativeSkillConsumption",M=P+"NativeSourceSkillConsumptionMath",BR=P+"NativeSkillConsumptionBridges";
 static final Set<String> OWNERS=Set.of(PC,P+"CombatCompanionSpells",P+"CombatNpcDebuffs");
 static ClassNode read(byte[]b){var c=new ClassNode();new ClassReader(b).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c,boolean normalized){if(normalized)for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static MethodNode method(ClassNode c,String name,String desc){return c.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static boolean cost(MethodInsnNode c){return c.owner.startsWith(CAT)&&(c.name.equals("mpCost")&&c.desc.equals("()I")||c.name.equals("effectiveMpCost"));}
 static boolean extra(MethodInsnNode c){return c.name.equals("mpCost")||c.desc.equals("()I")||c.desc.equals("(I)I");}
 static String bridgeName(MethodInsnNode c){return c.owner.substring(CAT.length())+"_"+c.name+"_"+c.desc.replaceAll("[^IZ]","");}
 static String bridgeDesc(MethodInsnNode c){return "(L"+c.owner+";"+c.desc.substring(1,c.desc.indexOf(')'))+(extra(c)?c.desc.equals("(I)I")?"I":"II":"")+")I";}
 static ClassNode bridgeClass(){var c=new ClassNode();c.version=V17;c.access=ACC_FINAL;c.name=BR;c.superName="java/lang/Object";return c;}
 static void bridge(ClassNode target,MethodInsnNode call){
  String name=bridgeName(call),desc=bridgeDesc(call);if(target.methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals(desc)))return;
  var m=new MethodNode(ACC_STATIC,name,desc,null,null);m.instructions.add(new VarInsnNode(ALOAD,0));int slot=1;for(var t:Type.getArgumentTypes(call.desc)){m.instructions.add(new VarInsnNode(t.getOpcode(ILOAD),slot));slot+=t.getSize();}
  m.instructions.add(new MethodInsnNode(call.getOpcode(),call.owner,call.name,call.desc,call.itf));m.instructions.add(new InsnNode(POP)); // Retain exact original validation and exception identity.
  m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"skillId","(Ljava/lang/Object;)I",false));m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"baseMp","(Ljava/lang/Object;)I",false));
  int intelligence,original,helmet=-1;
  if(call.name.equals("mpCost")||call.desc.equals("()I")){intelligence=slot;original=slot+1;}
  else if(call.desc.equals("(I)I")){intelligence=slot;original=1;}
  else if(call.desc.equals("(II)I")||call.desc.equals("(IIZ)I")){intelligence=1;original=2;if(call.desc.equals("(IIZ)I"))helmet=3;}
  else throw new AssertionError("unmapped cost signature "+call.owner+call.desc);
  m.instructions.add(new VarInsnNode(ILOAD,intelligence));m.instructions.add(new VarInsnNode(ILOAD,original));m.instructions.add(helmet<0?new InsnNode(ICONST_0):new VarInsnNode(ILOAD,helmet));m.instructions.add(new MethodInsnNode(INVOKESTATIC,M,"mpCost","(IIIIZ)I",false));m.instructions.add(new InsnNode(IRETURN));target.methods.add(m);
 }
 static void getters(ClassNode pc){
  var intelligence=new MethodNode(0,"nativeSkillConsumptionIntelligence","()I",null,null);intelligence.instructions.add(new VarInsnNode(ALOAD,0));intelligence.instructions.add(new MethodInsnNode(INVOKESPECIAL,PC,"requiredEquipmentMagicStats","()L"+P+"SourceEquipmentMagicStats;",false));intelligence.instructions.add(new VarInsnNode(ALOAD,0));intelligence.instructions.add(new MethodInsnNode(INVOKESPECIAL,PC,"baseIntelligence","()I",false));intelligence.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,P+"SourceEquipmentMagicStats","effectiveIntelligence","(I)I",false));intelligence.instructions.add(new InsnNode(IRETURN));pc.methods.add(intelligence);
  var original=new MethodNode(0,"nativeSkillConsumptionOriginalReduction","()I",null,null);original.instructions.add(new VarInsnNode(ALOAD,0));original.instructions.add(new MethodInsnNode(INVOKESPECIAL,PC,"selectedCharacterSummary","()L"+P+"CharacterSummary;",false));original.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"originalReduction","(L"+P+"CharacterSummary;)I",false));original.instructions.add(new InsnNode(IRETURN));pc.methods.add(original);
 }
 static ClassNode frames(ClassNode owner,List<MethodNode>methods,ClassLoader loader){var shell=new ClassNode();shell.version=owner.version;shell.access=owner.access;shell.name=owner.name;shell.superName=owner.superName;shell.interfaces=owner.interfaces;shell.methods.addAll(methods);for(var m:methods)for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);var w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){protected String getCommonSuperClass(String a,String b){try{Class<?>x=Class.forName(a.replace('/','.'),false,loader),y=Class.forName(b.replace('/','.'),false,loader);if(x.isAssignableFrom(y))return a;if(y.isAssignableFrom(x))return b;if(x.isInterface()||y.isInterface())return "java/lang/Object";do{x=x.getSuperclass();}while(!x.isAssignableFrom(y));return x.getName().replace('.','/');}catch(Exception e){throw new IllegalStateException(a+" / "+b,e);}}};shell.accept(w);return read(w.toByteArray());}
 static Map<String,ClassNode> patch(JarFile jar,ClassLoader loader)throws Exception{
  var result=new TreeMap<String,ClassNode>();var bridges=bridgeClass();int calls=0,writes=0,reflectWrites=0,extraMethods=0;var summary=new ArrayList<String>();
  for(var name:new TreeSet<>(OWNERS)){
   var c=read(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
   for(var m:c.methods){var costs=new ArrayList<MethodInsnNode>();for(var n:m.instructions)if(n instanceof MethodInsnNode call&&cost(call))costs.add(call);if(costs.isEmpty())continue;
    if((m.access&ACC_STATIC)!=0&&name.equals(PC))throw new AssertionError("static PC cost caller");int caster=name.equals(PC)?0:0;boolean needs=costs.stream().anyMatch(NativeSkillConsumptionMethods::extra);int intelligence=-1,original=-1;
    if(needs){intelligence=m.maxLocals++;original=m.maxLocals++;AbstractInsnNode first=costs.stream().filter(NativeSkillConsumptionMethods::extra).findFirst().orElseThrow();AbstractInsnNode monitor=null;for(var n:m.instructions){if(n==first)break;if(n.getOpcode()==MONITORENTER){monitor=n;break;}}
     var code=new InsnList();code.add(new VarInsnNode(ALOAD,caster));code.add(new MethodInsnNode(INVOKEVIRTUAL,PC,"nativeSkillConsumptionIntelligence","()I",false));code.add(new VarInsnNode(ISTORE,intelligence));code.add(new VarInsnNode(ALOAD,caster));code.add(new MethodInsnNode(INVOKEVIRTUAL,PC,"nativeSkillConsumptionOriginalReduction","()I",false));code.add(new VarInsnNode(ISTORE,original));m.instructions.insertBefore(monitor==null?first:monitor,code);extraMethods++;
    }
    for(var call:costs){bridge(bridges,call);if(extra(call)){var code=new InsnList();code.add(new VarInsnNode(ILOAD,intelligence));if(!call.desc.equals("(I)I"))code.add(new VarInsnNode(ILOAD,original));m.instructions.insertBefore(call,code);}m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,BR,bridgeName(call),bridgeDesc(call),false));calls++;}
    for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(PC)&&f.name.equals("playerMp")){
     int previous=prev(n).getOpcode();if(!Set.of(ISUB,INVOKEVIRTUAL,INVOKESTATIC,ICONST_0).contains(previous))throw new AssertionError("unreviewed MP writer alias pattern "+m.name+" "+previous);
     var code=new InsnList();code.add(new InsnNode(DUP2));code.add(new MethodInsnNode(INVOKESTATIC,H,"boundMp","(L"+PC+";I)I",false));code.add(new InsnNode(SWAP));code.add(new InsnNode(POP));m.instructions.insertBefore(n,code);writes++;
    }
    if(!name.equals(PC))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatAccess")&&call.name.equals("set")){
     var box=prev(n);if(!(box instanceof MethodInsnNode b)||!b.owner.equals("java/lang/Integer")||!b.name.equals("valueOf"))continue;var value=prev(box);var key=prev(value);var receiver=prev(key);if(!(key instanceof LdcInsnNode k)||!"playerMp".equals(k.cst))continue;
     if(!(value instanceof VarInsnNode v)||v.getOpcode()!=ILOAD||!(receiver instanceof VarInsnNode pc)||pc.getOpcode()!=ALOAD||pc.var!=caster)throw new AssertionError("exact reflected MP local/receiver");var code=new InsnList();code.add(new VarInsnNode(ALOAD,caster));code.add(new VarInsnNode(ILOAD,v.var));code.add(new MethodInsnNode(INVOKESTATIC,H,"boundMp","(L"+PC+";I)I",false));code.add(new VarInsnNode(ISTORE,v.var));m.instructions.insertBefore(value,code);reflectWrites++;
    }
    changed.add(m);summary.add(name+"\t"+m.name+m.desc+"\t"+costs.size());
   }
   if(name.equals(PC))getters(c);var recomputed=frames(c,changed,loader);for(var old:changed)c.methods.set(c.methods.indexOf(old),method(recomputed,old.name,old.desc));result.put(name,c);
  }
  result.put(BR,bridges);System.out.println("COUNTS calls="+calls+" actualPCMpWrites="+writes+" reflectedMpWrites="+reflectWrites+" extraSnapshotMethods="+extraMethods+" bridges="+bridges.methods.size());for(var line:summary)System.out.println("PATCHED\t"+line);return result;
 }
 public static void main(String[]args)throws Exception{try(var jar=new JarFile(args[1])){var urls=new ArrayList<URL>();urls.add(Path.of(args[1]).toUri().toURL());try(var files=Files.list(Path.of(args[4]))){for(var p:files.filter(p->p.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}try(var loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader())){var expected=patch(jar,loader);
  if(args[0].equals("splice")){for(var e:expected.entrySet()){var p=Path.of(args[2]).resolve(e.getKey()+".class");Files.createDirectories(p.getParent());Files.write(p,bytes(e.getValue(),false));}}
  else try(var candidate=new JarFile(args[2])){
   for(var e:jar.stream().toList()){var old=jar.getInputStream(e).readAllBytes();var actual=candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes();String name=e.getName().replaceFirst("\\.class$","");if(expected.containsKey(name)){if(!Arrays.equals(bytes(expected.get(name),true),bytes(read(actual),true)))throw new AssertionError("complete projected consumer class inverse "+name);}else if(!Arrays.equals(old,actual))throw new AssertionError("unapproved parent entry "+e.getName());}
   for(var e:candidate.stream().toList())if(jar.getJarEntry(e.getName())==null){byte[]expectedBytes;if(e.getName().equals(BR+".class"))expectedBytes=bytes(expected.get(BR),false);else{if(!Set.of(H+".class",M+".class").contains(e.getName()))throw new AssertionError("unexpected new entry "+e.getName());expectedBytes=Files.readAllBytes(Path.of(args[3]).resolve(e.getName()));}if(!Arrays.equals(expectedBytes,candidate.getInputStream(e).readAllBytes()))throw new AssertionError("unfrozen helper "+e.getName());}
   System.out.println("PASSED complete PC and two actual caster-helper class inverse; original catalog methods/validation preserved; exact typed bridges and canonical MP bounds; every other parent entry byte-identical");
  }
 }}}
}
