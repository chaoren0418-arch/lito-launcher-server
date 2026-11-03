import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;
final class VerifyPoisonAtomic {
 static final String P="local/lito381/server/",C=P+"ClientSession",T=P+"BattleTimedStatus",H=P+"CombatPoisonAtomic",D=H+"$NpcState",K=P+"ClassicWorldMonsterPoisonAttack$Kind",ARGS="(JIIIJLjava/lang/String;)";
 static MethodNode state;
 static void need(boolean b,String message){if(!b)throw new AssertionError("poison atomic "+message);}static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}static int local(AbstractInsnNode n,int opcode){need(n instanceof VarInsnNode v&&v.getOpcode()==opcode,"typed original operand");return ((VarInsnNode)n).var;}
 static MethodNode copy(MethodNode m){var r=new MethodNode(m.access,m.name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(r);return r;}
 static void prepare(ClassNode formal,ClassNode candidate){
  state=candidate.methods.stream().filter(m->m.name.equals("sourceNpcPoisonState")&&m.desc.equals(ARGS+"L"+D+";")).findFirst().orElseThrow();need(state.access==ACC_PRIVATE,"one private typed state clone, no actor fields");
  var nativeMethod=formal.methods.stream().filter(m->m.name.equals("applyClassicWorldNpcPoison")&&m.desc.equals(ARGS+"V")).findFirst().orElseThrow();AbstractInsnNode first=null,last=null;JumpInsnNode guard=null;
  for(var n:nativeMethod.instructions)if(n instanceof MethodInsnNode c&&c.owner.equals(P+"ServerConfig")&&c.name.equals("classicWorldMonsterPoisonFixtureRoll"))first=before(before(n));else if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)&&f.name.equals("classicWorldPoisonKind"))last=n;
  need(first!=null&&last!=null,"formal state range");for(var n=last;n!=first;n=n.getPrevious())if(n instanceof JumpInsnNode j&&j.getOpcode()==IFEQ&&before(j) instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==14){guard=j;break;}need(guard!=null,"formal applied branch");
  var labels=new HashMap<LabelNode,LabelNode>();for(var n:nativeMethod.instructions)if(n instanceof LabelNode l)labels.put(l,new LabelNode());var result=new LabelNode();var expected=new MethodNode(ACC_PRIVATE,"sourceNpcPoisonState",ARGS+"L"+D+";",null,null);
  for(var n=first;;n=n.getNext()){if(!(n instanceof FrameNode)&&!(n instanceof LineNumberNode)){var v=n.clone(labels);if(n==guard)((JumpInsnNode)v).label=result;expected.instructions.add(v);}if(n==last)break;}
  expected.instructions.add(result);expected.instructions.add(new TypeInsnNode(NEW,D));expected.instructions.add(new InsnNode(DUP));expected.instructions.add(new VarInsnNode(ALOAD,11));for(int i:new int[]{10,12,13,14})expected.instructions.add(new VarInsnNode(ILOAD,i));expected.instructions.add(new MethodInsnNode(INVOKESPECIAL,D,"<init>","(L"+K+";IZZZ)V",false));expected.instructions.add(new InsnNode(ARETURN));
  need(VerifyCombatPreservation.semantic(state).equals(VerifyCombatPreservation.semantic(expected)),"clone is the exact formal RNG/admission/state/provenance prefix plus five scalar/enum results");
  for(var n:state.instructions)if(n instanceof MethodInsnNode c){need(!c.owner.equals(C)&&!c.owner.equals(P+"Protocol")&&!Set.of("sendEncrypted","broadcastPoisonState","takePlayerDamage","takeClassicWorldMonsterDamage").contains(c.name),"no network, HP or publisher in status transaction");}
 }
 static MethodNode normalize(MethodNode original){var m=copy(original);if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode c&&c.owner.equals(H)))return m;m.maxStack+=64;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(C,m);}catch(AnalyzerException e){throw new AssertionError(e);}var nodes=m.instructions.toArray();
  for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode c&&c.owner.equals(H)){
   if(c.name.equals("npc")){
    need(m.name.equals("applyClassicWorldNpcPoison")&&c.desc.equals("(L"+C+";JIIIJLjava/lang/String;)L"+D+";"),"only native NPC installer uses state clone");var args=new ArrayList<AbstractInsnNode>();var n=before(c);int[] ops={ALOAD,LLOAD,ILOAD,ILOAD,ILOAD,LLOAD,ALOAD};int[] slots={0,1,3,4,5,6,8};for(int i=6;i>=0;i--){need(local(n,ops[i])==slots[i],"exact original NPC parameters");args.add(n);n=before(n);}
    String[] names={"kind","roll","resistance","existing","applied"};int[] stores={11,10,12,13,14};String[] types={"L"+K+";","I","Z","Z","Z"};var remove=new ArrayList<AbstractInsnNode>(args);remove.add(c);n=after(c);
    for(int i=0;i<5;i++){if(i<4){need(n.getOpcode()==DUP,"one scalar decision snapshot");remove.add(n);n=after(n);}need(n instanceof MethodInsnNode get&&get.owner.equals(D)&&get.name.equals(names[i])&&get.desc.equals("()"+types[i]),"exact native local result binding");remove.add(n);n=after(n);need(local(n,i==0?ASTORE:ISTORE)==stores[i],"unchanged native state local");remove.add(n);n=after(n);}
    need(local(n,ILOAD)==14&&after(n) instanceof JumpInsnNode j&&j.getOpcode()==IFEQ,"original applied publisher gate");var outside=((JumpInsnNode)after(n)).label;remove.add(n);remove.add(after(n));
    var labels=new HashMap<LabelNode,LabelNode>();for(var x:state.instructions)if(x instanceof LabelNode l)labels.put(l,new LabelNode());AbstractInsnNode end=null;for(var x:state.instructions)if(x instanceof TypeInsnNode t&&t.getOpcode()==NEW&&t.desc.equals(D)){end=x;break;}need(end!=null,"typed state return");var returnLabel=before(end).getNext();while(returnLabel!=end&&!(returnLabel instanceof LabelNode))returnLabel=returnLabel.getNext();need(returnLabel instanceof LabelNode,"single return merge label");labels.put((LabelNode)returnLabel,outside);
    var prefix=new InsnList();for(var x=state.instructions.getFirst();x!=returnLabel;x=x.getNext())if(!(x instanceof FrameNode)&&!(x instanceof LineNumberNode))prefix.add(x.clone(labels));m.instructions.insertBefore(args.get(args.size()-1),prefix);for(var x:remove)m.instructions.remove(x);
   }else if(c.name.equals("player")){
    need(c.desc.equals("(L"+T+";JIIL"+C+";L"+C+";)Z"),"PC poison bridge descriptor");var caster=before(c);var pc=before(caster);need(local(caster,ALOAD)==0,"original poison caster");var f=frames[at];var receiver=f.getStack(f.getStackSize()-6);need(receiver.insns.size()==1&&receiver.insns.iterator().next() instanceof FieldInsnNode field&&field.owner.equals(C)&&field.name.equals("timedStatus")&&local(before(field),ALOAD)==local(pc,ALOAD),"exact existing PC status receiver");m.instructions.remove(caster);m.instructions.remove(pc);m.instructions.set(c,new MethodInsnNode(INVOKEVIRTUAL,T,"activatePoison","(JII)Z",false));
   }else if(c.name.equals("poll")){
    need(m.name.equals("serverExpireTimedStatuses")&&c.desc.equals("(L"+T+";JIL"+C+";)L"+T+"$PoisonPoll;")&&local(before(c),ALOAD)==0,"same original poison poll receiver");m.instructions.remove(before(c));m.instructions.set(c,new MethodInsnNode(INVOKEVIRTUAL,T,"pollPoison","(JI)L"+T+"$PoisonPoll;",false));
   }else {
    String field,type;if(c.name.equals("attacker")){field="poisonAttacker";type="L"+C+";";}else if(c.name.equals("object")){field="classicWorldPoisonAttackerObjectId";type="J";}else {need(c.name.equals("kind"),"known provenance field");field="classicWorldPoisonKind";type="L"+K+";";}need(c.desc.equals("(L"+C+";"+type+")V"),"unchanged original field stack operands");m.instructions.set(c,new FieldInsnNode(PUTFIELD,C,field,type));
   }
  }
  return m;
 }
}
