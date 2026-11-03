import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Exact selective BATTLE tame factory, release and existing material boundary edits. */
public final class BattleTamedMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativeBattleTamedLifecycle";
 static final List<String> TYPES=List.of("ClientSession","BattleCompanion","NativePetMaterialAction","BattleQuestEncounter$Snapshot","BattleTimeAttackEncounter$Snapshot","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster","Protocol","GameHub");
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(P+n+".class"))).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode previous(AbstractInsnNode i){do{i=i.getPrevious();}while(i.getOpcode()<0);return i;}
 static AbstractInsnNode next(AbstractInsnNode i){do{i=i.getNext();}while(i.getOpcode()<0);return i;}
 static void patch(ClassNode c){int count=0;
  if(c.name.equals(PC)){
   for(var m:c.methods)if(m.name.equals("createTamedCompanion")&&!m.desc.contains("ClassicWorldMonsterPopulation")){
    int kind=m.desc.contains("BattleQuestEncounter")?1:2;m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
    m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new VarInsnNode(ALOAD,2));m.instructions.add(new InsnNode(kind==1?ICONST_1:ICONST_2));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"complete","(L"+PC+";Ljava/lang/Object;L"+P+"SourceNpcPhysicalStats;I)L"+P+"BattleCompanion;",false));m.instructions.add(new InsnNode(ARETURN));count++;
   }
   var body=c.methods.stream().filter(m->m.name.equals("nativeMaterialTamingBody")).findFirst().orElseThrow();int stats=-1;
   for(var i:body.instructions.toArray())if(i instanceof MethodInsnNode call&&call.name.equals("npcPhysicalStats")){if(stats!=-1||!(next(i) instanceof VarInsnNode v)||v.getOpcode()!=ASTORE)throw new AssertionError("Source NPC stats binding");stats=v.var;}
   if(stats<0)throw new AssertionError("missing Source stats");
   for(var i:body.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(P+"GameHub")&&Set.of("captureQuestMonster","captureTimeAttackActor").contains(call.name)){
    body.instructions.insertBefore(call,new VarInsnNode(ALOAD,stats));call.setOpcode(INVOKESTATIC);call.owner=H;call.name=call.name.equals("captureQuestMonster")?"captureQuest":"captureTime";call.desc="(L"+P+"GameHub;"+call.desc.substring(1,call.desc.indexOf(')'))+"L"+P+"SourceNpcPhysicalStats;"+call.desc.substring(call.desc.indexOf(')'));count++;
   }
   var release=c.methods.stream().filter(m->m.name.equals("releaseCompanionToNature")).findFirst().orElseThrow();
   for(var i:release.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(P+"GameHub")&&Set.of("releaseQuestTamedCompanion","releaseTimeAttackTamedCompanion").contains(call.name)){
    release.instructions.insertBefore(call,new VarInsnNode(ALOAD,1));call.setOpcode(INVOKESTATIC);call.owner=H;call.name=call.name.equals("releaseQuestTamedCompanion")?"releaseQuest":"releaseTime";call.desc="(L"+P+"GameHub;"+call.desc.substring(1,call.desc.indexOf(')'))+"L"+P+"BattleCompanion;"+call.desc.substring(call.desc.indexOf(')'));count++;
   }if(count!=6)throw new AssertionError("PC BATTLE factory/capture/release edges "+count);
   int hit=0,ratio=0;var snapshotTypes=Set.of(P+"BattleCompanion$Snapshot",P+"BattleQuestEncounter$Snapshot",P+"BattleTimeAttackEncounter$Snapshot",P+"ClassicWorldMonsterPopulation$Snapshot");
   for(var m:c.methods)for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(P+"Protocol")&&Set.of("hitRatio","healthRatio").contains(call.name)&&previous(i) instanceof MethodInsnNode maximum&&snapshotTypes.contains(maximum.owner)&&Set.of("maxHp","maximumHp").contains(maximum.name)){
    if(call.name.equals("hitRatio")){call.name="visualHitRatio";hit++;}else{call.name="visualHealthRatio";ratio++;}call.owner=H;
   }if(hit!=31||ratio!=6)throw new AssertionError("NPC-only HP display ports "+hit+"/"+ratio);
  }else if(c.name.equals(P+"BattleCompanion")){
   for(var m:c.methods)if(Set.of("tamedFromQuest","tamedFromTimeAttack").contains(m.name)){
    for(var i:m.instructions.toArray()){
     if(i instanceof MethodInsnNode call&&call.owner.endsWith("$Snapshot")&&call.name.equals("hp")){call.setOpcode(INVOKESTATIC);call.owner=H;call.name="initialHp";call.desc="(Ljava/lang/Object;)I";count++;}
     if(i instanceof MethodInsnNode call&&call.owner.equals(P+"SourceNpcPhysicalStats")&&call.name.equals("maxMp")){
      if(!(previous(i) instanceof VarInsnNode v)||v.var!=3)throw new AssertionError("MP template argument");v.var=2;call.owner=P+(m.name.equals("tamedFromQuest")?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Snapshot";call.name="maximumMp";count++;
     }
     if(i.getOpcode()==ARETURN){var add=new InsnList();add.add(new VarInsnNode(ALOAD,2));add.add(new MethodInsnNode(INVOKESTATIC,H,"vitals","(L"+P+"BattleCompanion;Ljava/lang/Object;)L"+P+"BattleCompanion;",false));m.instructions.insertBefore(i,add);count++;}
    }
   }if(count!=6)throw new AssertionError("direct BATTLE vitals and unpublished initialization "+count);
  }else if(c.name.equals(P+"GameHub")){
   for(var m:c.methods)if(m.name.equals("releaseTimeAttackTamedCompanion"))for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(P+"BattleRules$RuleSet")&&call.name.equals("mode")){call.setOpcode(INVOKESTATIC);call.owner=P+"LiToGmNpcs";call.name="combatMode";call.desc="(L"+P+"BattleRules$RuleSet;)L"+P+"BattleRules$Mode;";count++;}
   if(count!=1)throw new AssertionError("existing GM release mode edge "+count);
  }else if(c.name.equals(P+"Protocol")){
   for(var m:c.methods)if(m.name.equals("questMonster")||m.name.equals("timeAttackActor"))for(var i:m.instructions.toArray()){
    if(i instanceof JumpInsnNode jump&&jump.getOpcode()==IF_ICMPLE&&previous(i) instanceof MethodInsnNode limit&&limit.name.equals("maxHp")){m.instructions.insertBefore(i,new InsnNode(POP2));jump.setOpcode(GOTO);count++;}
    if(i instanceof MethodInsnNode call&&call.owner.equals(P+"Protocol")&&call.name.equals("healthRatio")){call.owner=H;call.name="visualHealthRatio";count++;}
   }if(count!=4)throw new AssertionError("two NPC packet guards and visual ratios "+count);
  }else if(c.name.equals(P+"NativePetMaterialAction")){
   for(var m:c.methods)if(m.name.equals("taming")||m.name.equals("zombie"))for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call&&call.owner.equals(P+"NativePetWorldLifetime")&&call.name.equals("run")){
    m.instructions.insertBefore(i,new LdcInsnNode(m.name.equals("taming")?36:41));call.owner=H;call.desc=call.desc.replace(")V","I)V");count++;
   }if(count!=2)throw new AssertionError("existing pet boundaries "+count);
  }else if(c.name.endsWith("$Snapshot")){if(c.fields.stream().anyMatch(f->f.name.equals("nativeBattleTamedCompanion")))throw new AssertionError("duplicate preparation");c.fields.add(new FieldNode(ACC_PRIVATE|ACC_VOLATILE,"nativeBattleTamedCompanion","Ljava/lang/Object;",null,null));
  }else if(c.name.endsWith("$Monster")){var bag=c.fields.stream().filter(f->f.name.equals("inventory")&&f.desc.equals("L"+P+"RuntimeInventory;")).findFirst().orElseThrow();if((bag.access&ACC_FINAL)==0)throw new AssertionError("expected original final NPC bag");bag.access&=~ACC_FINAL;
  }else throw new AssertionError(c.name);
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1])){for(var n:TYPES){var c=read(old,n);patch(c);var expected=bytes(c);if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(P+n+".class");Files.createDirectories(out.getParent());Files.write(out,expected);}else try(var now=new JarFile(args[2])){if(!Arrays.equals(expected,bytes(read(now,n))))throw new AssertionError("exact BATTLE regeneration "+n);}}}System.out.println("PASS exact existing BATTLE tame, release and material regeneration; no PC fields or new worker/context registry");}
}
