import java.util.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Independent inverse: twelve narrow bridges, all original instructions retained. */
final class VerifyWorldMultiMagic {
 static final String P="local/lito381/server/",C=P+"ClientSession",W=P+"ClassicWorldMonsterPopulation",H=P+"CombatWorldMultiMagic",B=H+"$Batch",SNAP="L"+W+"$Snapshot;",LEASE="L"+P+"BattleEraseMagicStatus$Lease;";
 static void need(boolean value,String why){if(!value)throw new AssertionError("WORLD multi magic "+why);}
 static AbstractInsnNode prior(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode next(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static int scoped(MethodNode m,String name,String desc,AbstractInsnNode at){int index=m.instructions.indexOf(at);var found=m.localVariables.stream().filter(v->v.name.equals(name)&&(desc==null||v.desc.equals(desc))&&m.instructions.indexOf(v.start)<=index&&m.instructions.indexOf(v.end)>index).toList();need(found.size()==1,"one original scoped "+name);return found.get(0).index;}
 static void removeLoads(MethodNode m,AbstractInsnNode n,int...slots){var q=prior(n);for(int i=slots.length-1;i>=0;i--){need(q instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slots[i],"same typed original operand");var before=prior(q);m.instructions.remove(q);q=before;}}
 static ClassNode normalize(ClassNode source){
  if(!source.name.equals(C))return source;var c=new ClassNode();source.accept(c);var seen=new TreeSet<String>();int total=0;
  for(var m:c.methods){var calls=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode x&&x.owner.equals(H)).map(n->(MethodInsnNode)n).toList();if(calls.isEmpty())continue;
   need(Set.of("useClassicWorldLineAttackSpell","useClassicWorldCenteredAreaAttackSpell","useClassicWorldAreaAttackSpell").contains(m.name),"only declared native ingress");need(seen.add(m.name)&&calls.size()==4,"four bridges in one overload");
   var counts=new TreeMap<String,Integer>();for(var call:calls)counts.merge(call.name,1,Integer::sum);String scalar=m.name.equals("useClassicWorldLineAttackSpell")?"line":"area";need(counts.equals(new TreeMap<>(Map.of("create",1,scalar,1,"capturePut",1,"enqueue",1))),"exact bridge identities");
   var created=calls.stream().filter(x->x.name.equals("create")).findFirst().orElseThrow();need(created.desc.equals("()L"+B+";"),"typed cast-stack batch");var init=next(created);need(init instanceof VarInsnNode v&&v.getOpcode()==ASTORE,"batch initialized once at method entry");int batch=((VarInsnNode)init).var;need(prior(created)==null,"batch create is first instruction");
   var calc=calls.stream().filter(x->x.name.equals(scalar)).findFirst().orElseThrow();String type=scalar.equals("line")?"DirectionalLineSpell":"AreaAttackSpell";String original="(L"+C+";L"+P+"BattleSpellCatalog$"+type+";IIIILjava/lang/String;)I";need(calc.desc.equals(original.substring(0,original.indexOf(')'))+SNAP+")I"),"exact signed scalar type");int current=scoped(m,"current",SNAP,calc);removeLoads(m,calc,current);m.instructions.set(calc,new MethodInsnNode(INVOKESTATIC,P+"CombatNpcMagic",scalar,original,false));
   var put=calls.stream().filter(x->x.name.equals("capturePut")).findFirst().orElseThrow();need(put.desc.equals("(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/Object;L"+B+";L"+C+";"+SNAP+LEASE+")Ljava/lang/Object;"),"exact native map-put signature");need(scoped(m,"current",SNAP,put)==current,"capture same calculated NPC");removeLoads(m,put,batch,0,current,scoped(m,"eraseLease",LEASE,put));
   // The native key/damage/map operand producers are left intact and checked by whole-method comparison.
   m.instructions.set(put,new MethodInsnNode(INVOKEINTERFACE,"java/util/Map","put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",true));
   var enqueued=calls.stream().filter(x->x.name.equals("enqueue")).findFirst().orElseThrow();need(enqueued.desc.equals("(L"+B+";L"+C+";Ljava/util/Map$Entry;I)Z"),"exact target-lane signature");var jump=next(enqueued);need(jump instanceof JumpInsnNode j&&j.getOpcode()==IFNE,"handled condition retains fallback");
   var hp=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode x&&x.owner.equals(P+"GameHub")&&x.name.equals("damageClassicWorldMonster")).findFirst().orElseThrow();int hit=((VarInsnNode)next(hp)).var;var transition=m.localVariables.stream().filter(v->v.index==hit&&v.name.equals("transition")&&v.desc.equals("L"+W+"$Hit;")).findFirst().orElseThrow();need(((JumpInsnNode)jump).label==transition.end,"skip only original monster HP/effect loop body");need(next(jump).getOpcode()==ACONST_NULL&&next(next(jump)) instanceof VarInsnNode store&&store.getOpcode()==ASTORE&&store.var==hit,"native nullable transition and entire fallback retained");
   var skill=prior(enqueued);need(skill instanceof MethodInsnNode get&&get.getOpcode()==INVOKEVIRTUAL&&get.owner.equals(P+"BattleSpellCatalog$"+(scalar.equals("line")?"LineAttackSpell":"AreaAttackSpell"))&&get.name.equals("skillId")&&get.desc.equals("()I"),"same original spell skill accessor");removeLoads(m,skill,batch,0,scoped(m,"entry","Ljava/util/Map$Entry;",hp),VerifyLineSpells.slot(m,"spell"));m.instructions.remove(skill);m.instructions.remove(jump);m.instructions.remove(enqueued);
   m.instructions.remove(init);m.instructions.remove(created);need(Arrays.stream(m.instructions.toArray()).noneMatch(n->n instanceof VarInsnNode v&&v.var==batch),"cast-stack batch local has no other uses");total+=4;
  }
  need(seen.size()==3&&total==12,"exact three family/twelve bridge coverage");return c;
 }
}
