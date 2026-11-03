import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Bridges live target operands, rather than reparsing the primary packet target. */
final class ProbabilityPatch implements Opcodes {
    static final String P="local/lito381/server/";
    static Map<String,String> parameters;
    static void load(Path file)throws Exception {
        var result=new HashMap<String,String>();
        for(String line:Files.readAllLines(file)) {
            var cells=line.split("\t",-1);if(cells.length!=4)throw new IllegalStateException("invalid probability metadata");
            if(result.put(cells[0]+"."+cells[1]+cells[2],cells[3])!=null)throw new IllegalStateException("duplicate probability metadata");
        }
        parameters=Map.copyOf(result);
    }
    static void apply(ClassNode c,Map<String,Integer> hooks)throws Exception {
        int sites=0,missing=0,bossGates=0;
        for(var m:List.copyOf(c.methods)) {
            if(Arrays.stream(m.instructions.toArray()).noneMatch(n->n instanceof MethodInsnNode call&&ProbeProbabilityTargets.probability(call)))continue;
            var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
            var types=new Analyzer<>(new BasicInterpreter()).analyze(c.name,m);
            var nodes=m.instructions.toArray();
            var trace=new ProbeProbabilityTargets.Trace(m,frames);
            for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode call&&ProbeProbabilityTargets.probability(call)) {
                trace.targets.clear();trace.visited.clear();
                var frame=frames[i];int argc=Type.getArgumentTypes(call.desc).length;
                for(int j=frame.getStackSize()-argc;j<frame.getStackSize();j++)trace.scalar(frame.getStack(j),0);
                final int position=i;
                trace.targets.entrySet().removeIf(e->e.getKey()>=types[position].getLocals()||!types[position].getLocal(e.getKey()).isReference());
                if(trace.targets.isEmpty()) {
                    if(!m.name.equals("rollNpcThunderGrabDuration"))throw new IllegalStateException("unresolved probability target: "+m.name);
                    missing++;continue;
                }
                String record=call.owner.substring(call.owner.indexOf('$')+1);
                String names=parameters.get(record+"."+call.name+call.desc);
                if(names==null)throw new IllegalStateException("missing source operand semantics: "+record+"."+call.name+call.desc);
                if(call.getOpcode()!=INVOKEVIRTUAL&&call.getOpcode()!=INVOKEINTERFACE)throw new IllegalStateException("unexpected probability call opcode");
                var args=Type.getArgumentTypes(call.desc);var signature=new Type[args.length+3];
                signature[0]=Type.getObjectType(call.owner);System.arraycopy(args,0,signature,1,args.length);
                signature[signature.length-2]=Type.getObjectType(P+"ClientSession");signature[signature.length-1]=Type.getType("[Ljava/lang/Object;");
                String descriptor=Type.getMethodDescriptor(Type.INT_TYPE,signature),name="combatProbability$"+sites;
                var wrapper=new MethodNode(ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,name,descriptor,null,null);
                wrapper.instructions.add(new VarInsnNode(ALOAD,0));wrapper.instructions.add(new LdcInsnNode(call.name));
                wrapper.instructions.add(new LdcInsnNode(call.desc));wrapper.instructions.add(new LdcInsnNode(names));
                wrapper.instructions.add(new LdcInsnNode(args.length));wrapper.instructions.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));
                int local=1;
                for(int j=0;j<args.length;j++) {
                    wrapper.instructions.add(new InsnNode(DUP));wrapper.instructions.add(new LdcInsnNode(j));
                    wrapper.instructions.add(new VarInsnNode(args[j].getOpcode(ILOAD),local));box(wrapper.instructions,args[j]);
                    wrapper.instructions.add(new InsnNode(AASTORE));local+=args[j].getSize();
                }
                wrapper.instructions.add(new VarInsnNode(ALOAD,local));wrapper.instructions.add(new VarInsnNode(ALOAD,local+1));
                wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,P+"CombatProbability","dispatch",
                        "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;L"+P+"ClientSession;[Ljava/lang/Object;)I",false));
                wrapper.instructions.add(new InsnNode(IRETURN));wrapper.maxLocals=local+2;c.methods.add(wrapper);
                var extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));extra.add(new LdcInsnNode(trace.targets.size()));
                extra.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));int at=0;
                for(int targetLocal:trace.targets.keySet()) {
                    extra.add(new InsnNode(DUP));extra.add(new LdcInsnNode(at++));extra.add(new VarInsnNode(ALOAD,targetLocal));extra.add(new InsnNode(AASTORE));
                }
                m.instructions.insertBefore(call,extra);
                m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,c.name,name,descriptor,false));sites++;
            }
            int spellLocal=-1,local=(m.access&ACC_STATIC)==0?1:0;
            for(var argument:Type.getArgumentTypes(m.desc)) {
                if(argument.getSort()==Type.OBJECT&&argument.getInternalName().startsWith(P+"BattleSpellCatalog$")) {
                    if(spellLocal!=-1)throw new IllegalStateException("ambiguous source spell context: "+m.name);
                    spellLocal=local;
                }
                local+=argument.getSize();
            }
            if(spellLocal>=0)for(var n:nodes)if(n instanceof MethodInsnNode call&&call.desc.equals("()Z")
                    &&(call.name.equals("boss")||call.name.equals("finalBoss"))
                    &&call.owner.startsWith(P)&&!call.owner.startsWith(P+"BattleSpellCatalog$")) {
                var gate=new InsnList();gate.add(new VarInsnNode(ALOAD,spellLocal));
                gate.add(new MethodInsnNode(INVOKESTATIC,P+"CombatProbability","bossGate","(ZLjava/lang/Object;)Z",false));
                m.instructions.insert(call,gate);bossGates++;
            }
        }
        if(sites!=59||missing!=1)throw new IllegalStateException("probability sites drift: installed="+sites+" unresolved="+missing);
        hooks.put("combat-probability-targets",sites);hooks.put("combat-probability-npc-thunder-outer-adapter",missing);
        if(bossGates==0)throw new IllegalStateException("missing source boss gates");hooks.put("combat-boss-gates",bossGates);
        thunder(c,hooks);
        confusion(c,hooks);
        counter(c,hooks);
        mortal(c,hooks);
    }
    static void mortal(ClassNode c,Map<String,Integer> hooks)throws Exception {
        int direct=0,outer=0;
        for(var m:List.copyOf(c.methods)) {
            if(m.name.equals("mortalBodyNpcDecision"))continue; // fallback only; all three live callers are adapted below
            var nodes=m.instructions.toArray();
            if(Arrays.stream(nodes).noneMatch(n->n instanceof MethodInsnNode call&&
                    (call.owner.equals(P+"BattleMortalBody")&&call.name.equals("decide")||call.owner.equals(c.name)&&call.name.equals("mortalBodyNpcDecision"))))continue;
            var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
            var types=new Analyzer<>(new BasicInterpreter()).analyze(c.name,m);var trace=new ProbeProbabilityTargets.Trace(m,frames);
            for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode call&&
                    (call.owner.equals(P+"BattleMortalBody")&&call.name.equals("decide")||call.owner.equals(c.name)&&call.name.equals("mortalBodyNpcDecision"))) {
                boolean npc=call.owner.equals(c.name);
                String decision="L"+P+"BattleMortalBody$Decision;",spell="L"+P+"BattleSpellCatalog$MortalBodySpell;";
                if(!call.desc.equals(npc?"(IZ)"+decision:"("+spell+"ZIIZIZIIII)"+decision))throw new IllegalStateException("mortal signature drift: "+call.desc);
                trace.targets.clear();trace.visited.clear();var frame=frames[i];int argc=Type.getArgumentTypes(call.desc).length;
                for(int j=frame.getStackSize()-argc;j<frame.getStackSize();j++)trace.scalar(frame.getStack(j),0);
                int slot=1;
                for(var argument:Type.getArgumentTypes(m.desc)) {
                    if(argument.getSort()==Type.OBJECT&&(ProbeProbabilityTargets.targetOwner(argument.getInternalName())
                            ||argument.getInternalName().equals(P+"BattleCompanion")))trace.targets.computeIfAbsent(slot,k->new TreeSet<>()).add(argument.getInternalName());
                    slot+=argument.getSize();
                }
                if(m.localVariables!=null)for(var local:m.localVariables)
                    if(Set.of("questAttacker","timeAttacker").contains(local.name)&&m.instructions.indexOf(local.start)<=i&&m.instructions.indexOf(local.end)>i)
                        trace.targets.computeIfAbsent(local.index,k->new TreeSet<>()).add(local.desc);
                final int position=i;
                trace.targets.entrySet().removeIf(e->e.getKey()>=types[position].getLocals()||!types[position].getLocal(e.getKey()).isReference());
                if(trace.targets.isEmpty()&&!m.name.equals("tryMortalBodyArenaBotReflection"))throw new IllegalStateException("mortal attacker context missing: "+m.name);
                var extra=new InsnList();if(!npc)extra.add(new VarInsnNode(ALOAD,0));
                extra.add(new LdcInsnNode(trace.targets.size()));extra.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));int at=0;
                for(int target:trace.targets.keySet()) {
                    extra.add(new InsnNode(DUP));extra.add(new LdcInsnNode(at++));extra.add(new VarInsnNode(ALOAD,target));extra.add(new InsnNode(AASTORE));
                }
                m.instructions.insertBefore(call,extra);
                m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatProbability",npc?"npcMortalDecision":"mortalDecision",
                        npc?"(L"+P+"ClientSession;IZ[Ljava/lang/Object;)"+decision
                           :"("+spell+"ZIIZIZIIIIL"+P+"ClientSession;[Ljava/lang/Object;)"+decision,false));
                if(npc)outer++;else direct++;
            }
        }
        if(direct!=2||outer!=3)throw new IllegalStateException("mortal consumers drift: direct="+direct+" NPC="+outer);
        hooks.put("combat-mortal-direct-targets",direct);hooks.put("combat-mortal-npc-targets",outer);
    }
    static void counter(ClassNode c,Map<String,Integer> hooks)throws Exception {
        int sites=0;
        for(var m:List.copyOf(c.methods)) {
            var nodes=m.instructions.toArray();
            if(Arrays.stream(nodes).noneMatch(n->n instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("counterBarrierDecision")))continue;
            var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
            var types=new Analyzer<>(new BasicInterpreter()).analyze(c.name,m);var trace=new ProbeProbabilityTargets.Trace(m,frames);
            for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("counterBarrierDecision")) {
                if(!call.desc.equals("(IIZIJ)L"+P+"BattleCounterBarrier$Decision;"))throw new IllegalStateException("counter signature drift");
                trace.targets.clear();trace.visited.clear();var frame=frames[i];int argc=Type.getArgumentTypes(call.desc).length;
                for(int j=frame.getStackSize()-argc;j<frame.getStackSize();j++)trace.scalar(frame.getStack(j),0);
                final int position=i;
                trace.targets.entrySet().removeIf(e->e.getKey()>=types[position].getLocals()||!types[position].getLocal(e.getKey()).isReference());
                if(trace.targets.isEmpty()&&!Set.of("attack","attackClassicWorldPlayer","serverBotTick").contains(m.name))
                    throw new IllegalStateException("counter attacker context missing: "+m.name);
                var extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));extra.add(new LdcInsnNode(trace.targets.size()));
                extra.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));int at=0;
                for(int target:trace.targets.keySet()) {
                    extra.add(new InsnNode(DUP));extra.add(new LdcInsnNode(at++));extra.add(new VarInsnNode(ALOAD,target));extra.add(new InsnNode(AASTORE));
                }
                m.instructions.insertBefore(call,extra);
                m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatProbability","counterDecision",
                        "(L"+P+"ClientSession;IIZIJL"+P+"ClientSession;[Ljava/lang/Object;)L"+P+"BattleCounterBarrier$Decision;",false));sites++;
            }
        }
        if(sites!=8)throw new IllegalStateException("counter consumers drift: "+sites);
        hooks.put("combat-counter-attacker-targets",sites);
    }
    static void confusion(ClassNode c,Map<String,Integer> hooks)throws Exception {
        int success=0,chance=0;
        for(var m:List.copyOf(c.methods)) {
            var calls=new ArrayList<MethodInsnNode>();
            for(var node:m.instructions)if(node instanceof MethodInsnNode call
                    &&call.owner.equals(P+"BattleSpellCatalog$ConfusionSpell")&&Set.of("chancePercent","succeeds").contains(call.name))calls.add(call);
            if(calls.isEmpty())continue;
            if(!Set.of("useAttackSpell","useClassicWorldAttackSpell").contains(m.name))throw new IllegalStateException("unexpected confusion caller: "+m.name);
            var targets=new HashMap<MethodInsnNode,Map<Integer,String>>();
            for(var call:calls)targets.put(call,ProbeProbabilityTargets.confusionLocals(m,call));
            var types=new Analyzer<>(new BasicInterpreter()).analyze(c.name,m);
            var indexes=new IdentityHashMap<AbstractInsnNode,Integer>();var nodes=m.instructions.toArray();for(int i=0;i<nodes.length;i++)indexes.put(nodes[i],i);
            for(var call:calls) {
                var locals=targets.get(call);
                if(locals.size()!=5)throw new IllegalStateException("confusion target context drift: "+m.name+" "+locals);
                var extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));extra.add(new LdcInsnNode(locals.size()));
                extra.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));int at=0;
                for(int target:locals.keySet()) {
                    if(!types[indexes.get(call)].getLocal(target).isReference())throw new IllegalStateException("uninitialized confusion target reference");
                    extra.add(new InsnNode(DUP));extra.add(new LdcInsnNode(at++));extra.add(new VarInsnNode(ALOAD,target));extra.add(new InsnNode(AASTORE));
                }
                boolean succeeds=call.name.equals("succeeds");
                if(!call.desc.equals(succeeds?"(ZI)Z":"(Z)I"))throw new IllegalStateException("confusion signature drift");
                m.instructions.insertBefore(call,extra);
                m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatProbability",succeeds?"confusionSucceeds":"confusionChance",
                        "(L"+P+"BattleSpellCatalog$ConfusionSpell;Z"+(succeeds?"I":"")+"L"+P+"ClientSession;[Ljava/lang/Object;)"+(succeeds?"Z":"I"),false));
                if(succeeds)success++;else chance++;
            }
        }
        if(success!=2||chance!=2)throw new IllegalStateException("confusion consumers drift: success="+success+" chance="+chance);
        hooks.put("combat-confusion-success",success);hooks.put("combat-confusion-chance",chance);
    }
    static void thunder(ClassNode c,Map<String,Integer> hooks)throws Exception {
        int players=0,npcs=0;
        for(var m:List.copyOf(c.methods)) {
            var nodes=m.instructions.toArray();
            boolean npcCall=Arrays.stream(nodes).anyMatch(n->n instanceof MethodInsnNode call
                    &&call.owner.equals(c.name)&&call.name.equals("rollNpcThunderGrabDuration"));
            Frame<SourceValue>[] frames=npcCall?new Analyzer<>(new SourceInterpreter()).analyze(c.name,m):null;
            Frame<BasicValue>[] types=npcCall?new Analyzer<>(new BasicInterpreter()).analyze(c.name,m):null;
            var trace=npcCall?new ProbeProbabilityTargets.Trace(m,frames):null;
            for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode call) {
                if(m.name.equals("applyPlayerThunderGrab")&&call.owner.equals(P+"BattleSpellCatalog$ThunderGrabSpell")
                        &&call.name.equals("succeeds")&&call.desc.equals("(IIIZI)Z")) {
                    var extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));extra.add(new VarInsnNode(ALOAD,1));
                    m.instructions.insertBefore(call,extra);
                    m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatProbability","thunderSucceeds",
                            "(L"+P+"BattleSpellCatalog$ThunderGrabSpell;IIIZIL"+P+"ClientSession;L"+P+"ClientSession;)Z",false));players++;
                } else if(call.owner.equals(c.name)&&call.name.equals("rollNpcThunderGrabDuration")) {
                    if(!call.desc.equals("(L"+P+"BattleSpellCatalog$ThunderGrabSpell;IIZIIJLjava/lang/String;)I"))throw new IllegalStateException("NPC thunder signature drift");
                    trace.targets.clear();trace.visited.clear();var frame=frames[i];int argc=Type.getArgumentTypes(call.desc).length;
                    for(int j=frame.getStackSize()-argc;j<frame.getStackSize();j++)trace.scalar(frame.getStack(j),0);
                    final int position=i;
                    trace.targets.entrySet().removeIf(e->e.getKey()>=types[position].getLocals()||!types[position].getLocal(e.getKey()).isReference());
                    if(trace.targets.isEmpty())throw new IllegalStateException("NPC thunder has no live target: "+m.name);
                    var extra=new InsnList();extra.add(new LdcInsnNode(trace.targets.size()));extra.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));int at=0;
                    for(int target:trace.targets.keySet()) {
                        extra.add(new InsnNode(DUP));extra.add(new LdcInsnNode(at++));extra.add(new VarInsnNode(ALOAD,target));extra.add(new InsnNode(AASTORE));
                    }
                    m.instructions.insertBefore(call,extra);
                    m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatProbability","npcThunderDuration",
                            "(L"+P+"ClientSession;L"+P+"BattleSpellCatalog$ThunderGrabSpell;IIZIIJLjava/lang/String;[Ljava/lang/Object;)I",false));npcs++;
                }
            }
        }
        if(players!=1||npcs!=6)throw new IllegalStateException("thunder consumers drift: players="+players+" NPCs="+npcs);
        hooks.put("combat-thunder-success",players);hooks.put("combat-thunder-npc-targets",npcs);
    }
    static void box(InsnList code,Type type) {
        String owner=switch(type.getSort()) {
            case Type.BOOLEAN -> "Boolean";case Type.BYTE -> "Byte";case Type.CHAR -> "Character";
            case Type.SHORT -> "Short";case Type.INT -> "Integer";case Type.LONG -> "Long";
            case Type.FLOAT -> "Float";case Type.DOUBLE -> "Double";default -> null;
        };
        if(owner!=null)code.add(new MethodInsnNode(INVOKESTATIC,"java/lang/"+owner,"valueOf",
                "("+type.getDescriptor()+")Ljava/lang/"+owner+";",false));
    }
}
