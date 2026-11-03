import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;
/** Execute the actual compiled receiveDamage equipment/Avatar tail without source boot. */
public final class BuildIncomingSourceOracle {
 static final String Q="com/lineage/server/",O="local/lito381/server/SourceIncomingOracle";
 static void need(boolean b,String why){if(!b)throw new AssertionError("incoming source oracle: "+why);}
 static ClassNode read(Path p)throws Exception{var c=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(c,0);return c;}
 static AbstractInsnNode prior(AbstractInsnNode n){do{n=n.getPrevious();}while(n.getOpcode()<0);return n;}
 static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n.getOpcode()<0);return n;}
 static String sha(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
 public static void main(String[] args)throws Exception{
  Path source=Path.of(args[0]),target=Path.of(args[1]).resolve(O+".class"),receipt=Path.of(args[2]);var pc=read(source);var method=pc.methods.stream().filter(m->m.name.equals("receiveDamageInternal")).findFirst().orElseThrow();AbstractInsnNode start=null,end=null;int slot=-1;
  for(var n:method.instructions)if(n instanceof MethodInsnNode c&&c.name.equals("checkEquipped")&&prior(n) instanceof IntInsnNode number&&number.operand==145){need(start==null,"unique source equipment145 tail");start=prior(prior(prior(n)));need(start instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"source receiver before inventory145");}
  for(var n:method.instructions)if(n instanceof FieldInsnNode f&&f.getOpcode()==GETSTATIC&&f.owner.equals("com/lineage/config/ConfigSkill")&&f.name.equals("ILLUSION_AVATAR_DAMAGE_RATE")){var mul=next(n);var store=next(mul);need(mul.getOpcode()==DMUL&&store instanceof VarInsnNode v&&v.getOpcode()==DSTORE,"source Avatar double multiplication");slot=((VarInsnNode)store).var;for(var p=n.getPrevious();p!=null;p=p.getPrevious())if(p instanceof JumpInsnNode j&&j.getOpcode()==IFEQ){end=j.label;break;}}
  need(start!=null&&end!=null&&slot>=0,"complete original equipment/Avatar branch range");var labels=new HashMap<LabelNode,LabelNode>();for(var n:method.instructions)if(n instanceof LabelNode l)labels.put(l,new LabelNode());var tail=new MethodNode(ACC_PUBLIC,"tail","(D)I",null,null);int calls=0,fields=0;
  for(var n=start;;n=n.getNext()){
   if(!(n instanceof FrameNode)&&!(n instanceof LineNumberNode)){var copy=n.clone(labels);if(copy instanceof VarInsnNode v&&Set.of(DLOAD,DSTORE).contains(v.getOpcode())){need(v.var==slot,"single source damage double local");v.var=1;}
    if(copy instanceof MethodInsnNode c){calls++;need(c.owner.equals(Q+"model/Instance/L1PcInstance")&&(c.name.equals("getInventory")&&c.desc.equals("()L"+Q+"model/L1PcInventory;")||c.name.equals("hasSkillEffect")&&c.desc.equals("(I)Z"))||c.owner.equals(Q+"model/L1PcInventory")&&c.name.equals("checkEquipped")&&c.desc.equals("(I)Z"),"only original typed inventory/status reads");c.owner=O;c.desc=c.desc.replace(Q+"model/L1PcInventory",O);}
    if(copy instanceof FieldInsnNode f){need(f.getOpcode()==GETSTATIC&&f.owner.equals("com/lineage/config/ConfigSkill")&&f.name.equals("ILLUSION_AVATAR_DAMAGE_RATE")&&f.desc.equals("D"),"only Avatar config field");fields++;tail.instructions.add(new VarInsnNode(ALOAD,0));copy=new FieldInsnNode(GETFIELD,O,"rate","D");}
    tail.instructions.add(copy);
   }
   if(n==end)break;
  }
  need(calls==5&&fields==1,"exact original tail typed bindings");boolean cast=false;for(var n=end.getNext();n!=null;n=n.getNext())if(n instanceof VarInsnNode v&&v.getOpcode()==DLOAD&&v.var==slot&&next(n).getOpcode()==D2I){tail.instructions.add(new VarInsnNode(DLOAD,1));tail.instructions.add(next(n).clone(labels));cast=true;break;}need(cast,"actual source DamageEvent final int cast");tail.instructions.add(new InsnNode(IRETURN));var fixture=read(target);fixture.methods.removeIf(m->m.name.equals("tail"));fixture.methods.add(tail);var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);fixture.accept(writer);Files.write(target,writer.toByteArray());
  String result="{\"source_class_sha256\":\""+sha(source)+"\",\"executable_class_sha256\":\""+sha(target)+"\",\"tail_calls\":5,\"tail_config_fields\":1,\"source_boot_or_SQL_executed\":false}";Files.writeString(receipt,result+"\n");System.out.println("PASS executable source equipment145/149 OR, double1.5, conditional Avatar and DamageEvent int tail "+result);
 }
}
