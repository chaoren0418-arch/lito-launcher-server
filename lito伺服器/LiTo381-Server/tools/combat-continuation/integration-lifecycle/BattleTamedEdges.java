import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Read actual archived classes; source mirrors are not the runtime authority. */
public final class BattleTamedEdges {
 public static void main(String[]args)throws Exception{
  var lines=new ArrayList<String>();try(var jar=new JarFile(args[0])){for(String name:List.of("ClientSession","BattleCompanion","GameHub","BattleQuestEncounter","BattleTimeAttackEncounter","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster")){
   var c=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("local/lito381/server/"+name+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   if(!name.equals("ClientSession")&&!name.equals("GameHub"))for(var f:c.fields)lines.add("FIELD "+name+" "+f.name+" "+f.desc+" "+f.access);
   for(var m:c.methods){
    if(name.equals("ClientSession"))for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals("local/lito381/server/Protocol")&&Set.of("hitRatio","healthRatio").contains(call.name)){lines.add("HIT_RATIO "+call.name+" "+m.name+m.desc);for(var n=i.getPrevious(); n!=null;){if(n instanceof MethodInsnNode prior)lines.add(" ARG "+prior.owner+"."+prior.name+prior.desc);n=n.getPrevious();if(n==null||n instanceof LabelNode)break;}}
    boolean selected=name.equals("ClientSession")?m.name.equals("createTamedCompanion")||m.name.equals("releaseCompanionToNature")||m.name.contains("Taming")||m.name.contains("taming"):name.equals("GameHub")?m.name.contains("capture")||m.name.contains("release")&&m.name.contains("Tamed"):name.equals("BattleCompanion")?m.name.startsWith("tamedFrom"):name.endsWith("$Monster")?m.name.equals("<init>")&&m.desc.contains("BattleCompanion"):m.name.equals("capture")||m.name.equals("releaseTamed");if(!selected)continue;
    lines.add("METHOD "+name+" "+m.name+m.desc+" access="+m.access);
    for(var i:m.instructions){if(i instanceof MethodInsnNode call)lines.add(" CALL "+call.owner+"."+call.name+call.desc);else if(i instanceof FieldInsnNode f)lines.add(" FIELD "+i.getOpcode()+" "+f.owner+"."+f.name+" "+f.desc);else if(i instanceof VarInsnNode v)lines.add(" VAR "+i.getOpcode()+" "+v.var);else if(i instanceof InsnNode)lines.add(" OP "+i.getOpcode());}
   }
  }}Files.write(Path.of(args[1]),lines);
 }
}
