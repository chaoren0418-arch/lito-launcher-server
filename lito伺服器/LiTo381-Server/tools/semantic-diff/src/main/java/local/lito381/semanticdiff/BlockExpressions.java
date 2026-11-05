package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Block-local symbolic execution. Stack exits preserve predecessor/value correlations.
 * Only loads and proven dead-at-exit temporary transport disappear; every other operation
 * is an ordered event, with its operands and ordered exceptional successors.
 */
final class BlockExpressions implements Opcodes {
    private BlockExpressions() {}
    record Expr(String kind, int id, int size) {}
    record Val(BasicValue type, Expr expr) implements Value {
        @Override public int getSize() { return type.getSize(); }
    }
    record Event(String text, List<Expr> args, List<String> catches) {}
    record Block(List<Event> events, List<Expr> entry, List<Expr> exit,
            Map<Expr,Integer> copies, List<Integer> edges) {}

    static boolean same(MethodNode a, MethodNode b, JarIndex aj, JarIndex bj) {
        if (!FourthNormalizations.enabled("blocks") || !FourthNormalizations.enabled("transport")) return false;
        if (direct(a,b,aj,bj)) return true;
        try {
            // Keep the earlier, independently proven local rewrites (not union provenance).
            return direct(LocalTransport.normalize(a),LocalTransport.normalize(b),aj,bj);
        } catch (AnalyzerException | RuntimeException unsupported) { return false; }
    }
    private static boolean direct(MethodNode a, MethodNode b, JarIndex aj, JarIndex bj) {
        try {
            List<Block> left = view(a,aj), right = view(b,bj);
            if (left.size() != right.size()) return false;
            Map<Integer,Integer> slots = new HashMap<>(), reverse = new HashMap<>();
            for (int i=0;i<LocalWebs.parameters(a);i++) { slots.put(i,i); reverse.put(i,i); }
            for (int i=0;i<left.size();i++) {
                Block l=left.get(i), r=right.get(i);
                if (!l.edges.equals(r.edges) || !l.entry.equals(r.entry) || l.events.size()!=r.events.size()) return false;
                for (int k=0;k<l.events.size();k++) {
                    Event x=l.events.get(k), y=r.events.get(k);
                    if (!x.text.equals(y.text) || !x.catches.equals(y.catches)
                            || !match(x.args,y.args,slots,reverse)) return false;
                }
                if (!match(l.exit,r.exit,slots,reverse) || l.copies.size()!=r.copies.size()) return false;
                Map<Expr,Integer> mapped=new HashMap<>();
                for (var e:l.copies.entrySet()) {
                    Expr x=e.getKey();
                    if (x.kind.equals("local")) {
                        Integer slot=slots.get(x.id); if(slot==null)return false;
                        x=new Expr("local",slot,x.size);
                    }
                    mapped.merge(x,e.getValue(),Integer::sum);
                }
                if(!mapped.equals(r.copies))return false;
            }
            return true;
        } catch (AnalyzerException | RuntimeException unsupported) { return false; }
    }
    static boolean match(List<Expr> a,List<Expr> b,Map<Integer,Integer> slots,Map<Integer,Integer> reverse) {
        if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++) {
            Expr x=a.get(i),y=b.get(i);
            if(!x.kind.equals(y.kind)||x.size!=y.size)return false;
            if(!x.kind.equals("local")) { if(x.id!=y.id)return false; continue; }
            for(int w=0;w<x.size;w++) {
                int l=x.id+w,r=y.id+w;
                if(slots.containsKey(l)) { if(slots.get(l)!=r)return false; }
                else { if(reverse.containsKey(r))return false; slots.put(l,r);reverse.put(r,l); }
            }
        }
        return true;
    }
    static List<Block> view(MethodNode m,JarIndex jar) throws AnalyzerException {
        return view(m,jar,false);
    }
    static List<Block> view(MethodNode m,JarIndex jar,boolean storedValues) throws AnalyzerException {
        return view(m,jar,storedValues,false);
    }
    static List<Block> view(MethodNode m,JarIndex jar,boolean storedValues,boolean tryBoundaries) throws AnalyzerException {
        return view(m,jar,storedValues,tryBoundaries,false);
    }
    static List<Block> view(MethodNode m,JarIndex jar,boolean storedValues,boolean tryBoundaries,boolean emptyBoundaries) throws AnalyzerException {
        if(m.instructions.size()==0 || m.visibleLocalVariableAnnotations!=null || m.invisibleLocalVariableAnnotations!=null)
            throw new IllegalArgumentException("unsupported block metadata");
        List<AbstractInsnNode> code=new ArrayList<>(); Map<AbstractInsnNode,Integer> pos=new IdentityHashMap<>();
        for(AbstractInsnNode n:m.instructions) { pos.put(n,code.size());if(n.getOpcode()>=0)code.add(n); }
        Frame<BasicValue>[] frames=new Analyzer<>(new BasicInterpreter()).analyze("Blocks",m);
        TreeSet<Integer> leaders=new TreeSet<>();leaders.add(0);
        if(emptyBoundaries)for(int i=0;i<code.size();i++) {
            Frame<BasicValue> frame=frames[m.instructions.indexOf(code.get(i))];
            if(frame!=null&&frame.getStackSize()==0)leaders.add(i);
        }
        for(int i=0;i<code.size();i++) {
            AbstractInsnNode n=code.get(i);int op=n.getOpcode();
            if(op==JSR||op==RET||!FourthNormalizations.enabled("value-flow")
                    && (op==DUP_X1||op==DUP_X2||op==DUP2_X1||op==DUP2_X2||op==SWAP))
                throw new IllegalArgumentException("unsupported shuffle/subroutine");
            if (op >= DUP && op <= SWAP && FourthNormalizations.annotated(n))
                throw new IllegalArgumentException("annotated shuffle");
            if(n instanceof JumpInsnNode j)leaders.add(pos.get(j.label));
            if(n instanceof TableSwitchInsnNode s){leaders.add(pos.get(s.dflt));for(LabelNode l:s.labels)leaders.add(pos.get(l));}
            if(n instanceof LookupSwitchInsnNode s){leaders.add(pos.get(s.dflt));for(LabelNode l:s.labels)leaders.add(pos.get(l));}
            if(n instanceof JumpInsnNode||n instanceof TableSwitchInsnNode||n instanceof LookupSwitchInsnNode
                    ||op==ATHROW||op>=IRETURN&&op<=RETURN)leaders.add(i+1);
        }
        for(TryCatchBlockNode t:m.tryCatchBlocks) {
            leaders.add(pos.get(t.handler));
            // Split a cleanup suffix only at an empty-stack boundary. Keeping
            // expression evaluation intact avoids artificial stack transport blocks.
            if(tryBoundaries)for(LabelNode boundary:List.of(t.start,t.end)) {
                Frame<BasicValue> frame=frames[m.instructions.indexOf(boundary)];
                if(frame!=null&&frame.getStackSize()==0)leaders.add(pos.get(boundary));
            }
        }
        leaders.remove(code.size());
        List<Integer> starts=new ArrayList<>(leaders);int[] block=new int[code.size()];
        for(int i=0;i<starts.size();i++)Arrays.fill(block,starts.get(i),i+1<starts.size()?starts.get(i+1):code.size(),i);
        Map<Integer,List<VarInsnNode>> uses=new HashMap<>();Set<Integer> blocked=new HashSet<>();
        Map<Integer,Integer> widths=new HashMap<>();
        for(AbstractInsnNode n:code) {
            if(n instanceof VarInsnNode v) {
                uses.computeIfAbsent(v.var,k->new ArrayList<>()).add(v);
                int width=LocalWebs.width(v.getOpcode());
                Integer old=widths.putIfAbsent(v.var,width);
                if(old!=null&&old!=width)throw new IllegalArgumentException("overlapping local types");
                if(FourthNormalizations.annotated(n))blocked.add(v.var);
            }
            if(n instanceof IincInsnNode inc)blocked.add(inc.var);
        }
        for(var e:widths.entrySet())if(e.getValue()==2&&(widths.containsKey(e.getKey()+1)||blocked.contains(e.getKey()+1)))
            throw new IllegalArgumentException("overlapping wide local");
        Set<Integer> temps=new HashSet<>(); Map<Integer,Integer> reads=new HashMap<>();
        for(var e:uses.entrySet()) {
            if(e.getKey()<LocalWebs.parameters(m)||blocked.contains(e.getKey()))continue;
            List<VarInsnNode> stores=e.getValue().stream().filter(v->LocalWebs.store(v.getOpcode())).toList();
            if(stores.size()!=1)continue;
            VarInsnNode store=stores.get(0);int p=pos.get(store);boolean ok=true;int count=0;
            for(VarInsnNode v:e.getValue())if(v!=store) {
                if(!LocalWebs.load(v.getOpcode())||v.getOpcode()!=store.getOpcode()-(ISTORE-ILOAD)
                        ||pos.get(v)<=p||block[pos.get(v)]!=block[p])ok=false;
                count++;
            }
            // No reads in any other block (including handlers), hence not live at the exit.
            if(ok&&count>0){temps.add(e.getKey());reads.put(e.getKey(),count);}
        }
        Set<Integer> deferred = new HashSet<>();
        if (FourthNormalizations.enabled("value-flow")) {
            deferred.addAll(TemporaryValues.eligible(m));
            deferred.removeAll(temps);
        }
        List<Block> result=new ArrayList<>();
        boolean monitor=code.stream().anyMatch(n->n.getOpcode()==MONITORENTER);
        for(int k=0;k<starts.size();k++) {
            int start=starts.get(k),end=k+1<starts.size()?starts.get(k+1):code.size();
            Frame<BasicValue> entry=frames[m.instructions.indexOf(code.get(start))];
            if(entry==null)throw new IllegalArgumentException("unreachable block");
            Evaluator eval=new Evaluator(m,jar,temps,reads,pos,block,monitor);
            eval.deferred = deferred;
            eval.storedValues=storedValues;
            Frame<Val> f=new Frame<>(m.maxLocals,m.maxStack);
            f.setReturn(eval.newValue(Type.getReturnType(m.desc)));
            for(int i=0;i<m.maxLocals;i++) { BasicValue v=entry.getLocal(i);f.setLocal(i,new Val(v,new Expr("local",i,v.getSize()))); }
            List<Expr> incoming=new ArrayList<>();
            for(int i=0;i<entry.getStackSize();i++) {
                BasicValue v=entry.getStack(i);Expr value=new Expr("stack",i,v.getSize());
                incoming.add(value);f.push(new Val(v,value));
            }
            for(int i=start;i<end;i++) {
                AbstractInsnNode n=code.get(i);int op=n.getOpcode();
                if(op==DUP||op==DUP2) {
                    if(FourthNormalizations.annotated(n))throw new IllegalArgumentException("annotated copy");
                    if (!FourthNormalizations.enabled("value-flow")) {
                        Val v=f.getStack(f.getStackSize()-1);eval.copy(v.expr,1);
                        if(op==DUP2&&v.getSize()==1)eval.copy(f.getStack(f.getStackSize()-2).expr,1);
                    }
                } else if(op==POP||op==POP2) {
                    List<Val> args=new ArrayList<>();Val v=f.getStack(f.getStackSize()-1);args.add(v);
                    if(op==POP2&&v.getSize()==1)args.add(0,f.getStack(f.getStackSize()-2));
                    eval.event(n,args,null);
                } else if(op==GOTO||op==RETURN||op==NOP)eval.event(n,List.of(),null);
                f.execute(n,eval);
            }
            eval.finish();
            List<Expr> exit=new ArrayList<>();for(int i=0;i<f.getStackSize();i++)exit.add(f.getStack(i).expr);
            AbstractInsnNode last=code.get(end-1);int op=last.getOpcode();List<Integer> edges=new ArrayList<>();
            if(last instanceof JumpInsnNode j){edges.add(block[pos.get(j.label)]);if(op!=GOTO)edges.add(k+1);}
            else if(last instanceof TableSwitchInsnNode s){edges.add(block[pos.get(s.dflt)]);for(LabelNode l:s.labels)edges.add(block[pos.get(l)]);}
            else if(last instanceof LookupSwitchInsnNode s){edges.add(block[pos.get(s.dflt)]);for(LabelNode l:s.labels)edges.add(block[pos.get(l)]);}
            else if(op!=ATHROW&&!(op>=IRETURN&&op<=RETURN))edges.add(k+1);
            result.add(new Block(eval.events,incoming,exit,eval.copies,edges));
        }
        return result;
    }
    static final class Evaluator extends Interpreter<Val> {
        final BasicInterpreter types=new BasicInterpreter();final List<Event> events=new ArrayList<>();
        final Map<Expr,Integer> copies=new HashMap<>();final MethodNode m;final JarIndex jar;
        final Set<Integer> temps;final Map<Integer,Integer> reads;final Map<AbstractInsnNode,Integer> pos;final int[] blocks;final boolean monitor;
        boolean storedValues;
        Set<Integer> deferred = Set.of();
        final List<Event> bindings = new ArrayList<>();
        Evaluator(MethodNode m,JarIndex jar,Set<Integer> temps,Map<Integer,Integer> reads,Map<AbstractInsnNode,Integer> pos,int[] blocks,boolean monitor){
            super(ASM9);this.m=m;this.jar=jar;this.temps=temps;this.reads=reads;this.pos=pos;this.blocks=blocks;this.monitor=monitor;
        }
        void copy(Expr value,int count){if(count>0)copies.merge(value,count,Integer::sum);}
        Val event(AbstractInsnNode n,List<Val> values,BasicValue type){
            List<Expr> args=new ArrayList<>();for(Val v:values)args.add(v.expr);
            if(n instanceof VarInsnNode v && LocalWebs.store(n.getOpcode()))args.add(0,new Expr("local",v.var,LocalWebs.width(n.getOpcode())));
            if(n instanceof IincInsnNode v)args.add(0,new Expr("local",v.var,1));
            List<String> catches=new ArrayList<>();int p=pos.get(n);
            if(!Normalizations.harmless(n,monitor))for(TryCatchBlockNode t:m.tryCatchBlocks)
                if(p>=pos.get(t.start)&&p<pos.get(t.end))catches.add(blocks[pos.get(t.handler)]+":"+ClassComparer.graphCatch(t,jar));
            int id=events.size();events.add(new Event(ClassComparer.graphInsn(n,jar),args,catches));
            return type==null?null:new Val(type,new Expr("event",id,type.getSize()));
        }
        @Override public Val newValue(Type type){BasicValue v=types.newValue(type);return v==null?null:new Val(v,new Expr("unset",0,v.getSize()));}
        @Override public Val newOperation(AbstractInsnNode n)throws AnalyzerException{
            BasicValue type = types.newOperation(n);
            if (FourthNormalizations.enabled("value-flow") && !FourthNormalizations.annotated(n)
                    && DiscardedLoads.literal(n))
                return new Val(type,new Expr("literal:"+ClassComparer.graphInsn(n,jar),0,type.getSize()));
            return event(n,List.of(),type);
        }
        @Override public Val copyOperation(AbstractInsnNode n,Val v)throws AnalyzerException{
            types.copyOperation(n,v.type);
            if(n instanceof VarInsnNode x) {
                if(LocalWebs.store(n.getOpcode())) {
                    if(temps.contains(x.var)){
                        if (!FourthNormalizations.enabled("value-flow")) copy(v.expr,reads.get(x.var)-1);
                        return v;
                    }
                    if (deferred.contains(x.var)) {
                        bindings.add(new Event(ClassComparer.graphInsn(n,jar),
                                List.of(new Expr("local",x.var,v.getSize()),v.expr),List.of()));
                        return v;
                    }
                    Val stored=event(n,List.of(v),v.type);
                    // STORE is still an ordered, slot-labelled event. Its local now
                    // holds exactly the input value: DUP;STORE and STORE;LOAD agree.
                    return storedValues ? v : stored;
                }
                if(FourthNormalizations.annotated(n))return event(n,List.of(v),v.type);
            }
            return v;
        }
        void finish() {
            if (bindings.isEmpty()) return;
            // These locals have one dominating definition and cannot be observed by
            // a handler. Bind their exact values at the normal block exit, ordered
            // by producer identity rather than the physical STORE shuffle.
            bindings.sort(Comparator.comparing((Event e) -> e.args.get(1).kind)
                    .thenComparingInt(e -> e.args.get(1).id).thenComparingInt(e -> e.args.get(0).id));
            int at = events.size();
            if (at > 0) {
                String last = events.get(at-1).text;
                if (last.startsWith("IF") || last.startsWith("GOTO") || last.startsWith("TABLESWITCH")
                        || last.startsWith("LOOKUPSWITCH") || last.endsWith("RETURN") || last.equals("ATHROW")) at--;
            }
            events.addAll(at,bindings);
        }
        @Override public Val unaryOperation(AbstractInsnNode n,Val v)throws AnalyzerException{return event(n,List.of(v),types.unaryOperation(n,v.type));}
        @Override public Val binaryOperation(AbstractInsnNode n,Val a,Val b)throws AnalyzerException{return event(n,List.of(a,b),types.binaryOperation(n,a.type,b.type));}
        @Override public Val ternaryOperation(AbstractInsnNode n,Val a,Val b,Val c)throws AnalyzerException{return event(n,List.of(a,b,c),types.ternaryOperation(n,a.type,b.type,c.type));}
        @Override public Val naryOperation(AbstractInsnNode n,List<? extends Val> values)throws AnalyzerException{
            List<BasicValue> args=new ArrayList<>();for(Val v:values)args.add(v.type);
            return event(n,new ArrayList<>(values),types.naryOperation(n,args));
        }
        @Override public void returnOperation(AbstractInsnNode n,Val value,Val expected){}
        @Override public Val merge(Val a,Val b){throw new IllegalStateException("no cross-block expression merging");}
    }
}
