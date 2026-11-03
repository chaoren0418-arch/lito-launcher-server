import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Test-only executable copies of current source arithmetic, with no source boot/SQL. */
public final class BuildNpcSourceOracle {
 static final String Q="com/lineage/server/",P="local/lito381/server/",O=P+"NpcSourceModeOracle",T=P+"SourceNpcTableOracle";
 static void need(boolean value,String why){if(!value)throw new AssertionError("source oracle: "+why);}
 static ClassNode read(Path p)throws Exception{var c=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(c,0);return c;}
 static MethodNode method(ClassNode c,String name,String desc){return c.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow();}
 static void clearDebug(MethodNode m){m.localVariables=null;m.tryCatchBlocks.clear();for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);}
 static String digest(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
 static String remap(String value){return value.replace(Q+"model/Instance/L1NpcInstance",O+"$Npc").replace(Q+"templates/L1Npc",O+"$Template").replace(Q+"model/L1AttackNpc",O);}
 public static void main(String[] args)throws Exception {
  Path source=Path.of(args[0]),tests=Path.of(args[1]),receipt=Path.of(args[2]);Path npc=source.resolve(Q+"model/L1AttackNpc.class"),list=source.resolve(Q+"model/L1AttackList.class");
  var mode=method(read(npc),"npcDmgMode","(D)D");mode.name="mode";mode.access=ACC_PUBLIC;var calls=new TreeMap<String,Integer>();int actorFields=0;
  for(var n:mode.instructions.toArray()){
   if(n instanceof FieldInsnNode f){need(f.getOpcode()==GETFIELD&&f.owner.equals(Q+"model/L1AttackNpc")&&f.name.equals("_npc"),"only original NPC field");f.owner=O;f.desc="L"+O+"$Npc;";actorFields++;}
   if(n instanceof MethodInsnNode c){calls.merge(c.name+c.desc,1,Integer::sum);
    if(c.owner.equals("java/util/concurrent/ThreadLocalRandom")){
     if(c.name.equals("current")&&c.desc.equals("()Ljava/util/concurrent/ThreadLocalRandom;"))mode.instructions.set(c,new VarInsnNode(ALOAD,0));
     else{need(c.name.equals("nextInt")&&c.desc.equals("(I)I"),"only source critical random bound");c.owner=O;c.name="nextInt";}
    }else{need(Set.of("getDmgup()I","isUndeadDamage()Z","getLeverage()I","getNpcTemplate()L"+Q+"templates/L1Npc;","isBoss()Z","getWeaponBreakStack()I").contains(c.name+c.desc),"unknown source mode call "+c.owner+"."+c.name+c.desc);c.owner=remap(c.owner);c.desc=remap(c.desc);}
   }
  }
  need(actorFields==3&&calls.values().stream().mapToInt(i->i).sum()==8&&calls.values().stream().allMatch(i->i==1),"exact source mode field/call shape");clearDebug(mode);
  var poison=method(read(npc),"addNpcPoisonAttack","(L"+Q+"model/L1Character;)V");poison.name="poison";poison.desc="(L"+O+"$Target;)V";poison.access=ACC_PUBLIC;int poisonFields=0,poisonCalls=0;
  for(var n:poison.instructions.toArray()){
   if(n instanceof FieldInsnNode f){need(f.getOpcode()==GETFIELD&&f.owner.equals(Q+"model/L1AttackNpc")&&f.name.equals("_npc"),"only source poison attacker field");f.owner=O;f.desc="L"+O+"$Npc;";poisonFields++;}
   if(n instanceof MethodInsnNode c){poisonCalls++;if(c.owner.equals("java/util/concurrent/ThreadLocalRandom")){if(c.name.equals("current")&&c.desc.equals("()Ljava/util/concurrent/ThreadLocalRandom;"))poison.instructions.set(c,new VarInsnNode(ALOAD,0));else{need(c.name.equals("nextInt")&&c.desc.equals("(I)I"),"source poison random bound only");c.owner=O;}}
    else if(c.owner.startsWith(Q+"model/poison/")){String name=c.owner.substring(c.owner.lastIndexOf('/')+1);need(c.name.equals("doInfection")&&Set.of("L1DamagePoison","L1SilencePoison","L1ParalysisPoison").contains(name),"only three source poison factories");c.owner=O+"$"+name.substring(2,name.length()-6);c.desc=c.desc.replace(Q+"model/L1Character",O+"$Target");}
    else{need(Set.of("getNpcTemplate()L"+Q+"templates/L1Npc;","get_poisonatk()I","get_paralysisatk()I").contains(c.name+c.desc),"only source poison NPC attributes");c.owner=remap(c.owner);c.desc=remap(c.desc);}
   }
  }
  need(poisonFields==3&&poisonCalls==13,"exact source poison field/call shape");clearDebug(poison);
  var commit=method(read(npc),"commitPc","()V");commit.name="commit";commit.access=ACC_PUBLIC;int commitFields=0,commitCalls=0;
  for(var n:commit.instructions.toArray()){
   if(n instanceof FieldInsnNode f){commitFields++;if(f.getOpcode()==GETSTATIC){need(f.owner.equals("com/lineage/config/ConfigSkill")&&f.name.equals("STRIKER_DMG2")&&f.desc.equals("D"),"only source Striker config");commit.instructions.insertBefore(f,new VarInsnNode(ALOAD,0));commit.instructions.set(f,new FieldInsnNode(GETFIELD,O,"striker","D"));}else{need(f.owner.equals(Q+"model/L1AttackNpc")&&Set.of("_isHit","_targetPc","_damage","_npc").contains(f.name),"only source commit actor/scalar fields");f.owner=O;f.desc=f.desc.replace(Q+"model/Instance/L1PcInstance",O+"$Pc").replace(Q+"model/Instance/L1NpcInstance",O+"$Npc");}}
   if(n instanceof MethodInsnNode c){commitCalls++;need(Set.of("hasSkillEffect(I)Z","isShortDistance()Z","receiveDamage(L"+Q+"model/L1Character;DZZ)V").contains(c.name+c.desc),"only source commit status/range/HP calls");c.owner=c.owner.replace(Q+"model/L1AttackNpc",O).replace(Q+"model/Instance/L1PcInstance",O+"$Pc");c.desc=c.desc.replace(Q+"model/L1Character",O+"$Target");}
  }
  need(commitFields==8&&commitCalls==3,"exact source commit field/call shape");clearDebug(commit);
  Path target=tests.resolve(O+".class");var stub=read(target);stub.methods.removeIf(m->m.name.equals("commit")&&m.desc.equals("()V")||m.name.equals("mode")&&m.desc.equals("(D)D")||m.name.equals("poison")&&m.desc.equals(poison.desc));stub.methods.add(commit);stub.methods.add(mode);stub.methods.add(poison);var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);stub.accept(writer);Files.write(target,writer.toByteArray());
  var original=read(list);var load=method(original,"load","()V");var starts=Arrays.stream(load.instructions.toArray()).filter(n->n instanceof IntInsnNode i&&i.getOpcode()==BIPUSH&&i.operand==-6).toList();need(starts.size()==1,"unique source STRD initial -6");var start=starts.get(0);while(load.instructions.getFirst()!=start)load.instructions.remove(load.instructions.getFirst());clearDebug(load);load.signature=null;
  for(var n:load.instructions){
   if(n instanceof FieldInsnNode f){need(f.owner.equals(Q+"model/L1AttackList")&&Set.of("STRD","DEXD").contains(f.name),"only source STRD/DEXD suffix");f.owner=T;}
   if(n instanceof MethodInsnNode c)need(c.owner.equals("java/lang/Integer")&&c.name.equals("valueOf")||c.owner.equals("java/util/HashMap")&&c.name.equals("put"),"no source boot or SQL call in arithmetic suffix");
  }
  var table=new ClassNode();table.version=V17;table.access=ACC_PUBLIC|ACC_FINAL;table.name=T;table.superName="java/lang/Object";table.fields.addAll(original.fields);var init=method(original,"<clinit>","()V");clearDebug(init);for(var n:init.instructions)if(n instanceof FieldInsnNode f){need(f.owner.equals(Q+"model/L1AttackList"),"original static map owner");f.owner=T;}else if(n instanceof MethodInsnNode c)need(c.owner.equals("java/util/HashMap")&&c.name.equals("<init>"),"map-only static initializer");table.methods.add(init);table.methods.add(load);writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);table.accept(writer);Path tablePath=tests.resolve(T+".class");Files.write(tablePath,writer.toByteArray());
  String json="{\"source_npc_class_sha256\":\""+digest(npc)+"\",\"source_table_class_sha256\":\""+digest(list)+"\",\"executable_mode_sha256\":\""+digest(target)+"\",\"executable_table_sha256\":\""+digest(tablePath)+"\",\"mode_calls\":8,\"mode_actor_fields\":3,\"poison_producer_calls\":13,\"poison_actor_fields\":3,\"commit_actor_fields\":8,\"commit_calls\":3,\"source_boot_or_sql_executed\":false}";Files.writeString(receipt,json+"\n");System.out.println("PASS source compiled npcDmgMode, poison producer and commitPc copied with typed scalar/factory/HP stubs; source STRD/DEXD loop suffix copied; source boot and SQL excluded "+json);
 }
}
