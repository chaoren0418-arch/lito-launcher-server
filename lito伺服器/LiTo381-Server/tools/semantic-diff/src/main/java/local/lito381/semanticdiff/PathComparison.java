package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** A bounded, fail-closed path proof. Never merges frames at a join. */
final class PathComparison implements Opcodes {
    record Result(boolean same, String reason) {}
    record Expr(String op, List<Expr> args) {
        Expr {
            // Bound expanded trees as well as path count (DUP can create an exponential DAG).
            long size=op.length();for(Expr e:args)size+=e.toString().length();
            if(size>32768)throw new IllegalArgumentException("expression size limit 32768 exceeded");
        }
        Expr(String op) { this(op, List.of()); }
    }
    record Val(BasicValue type, Expr expr) implements Value {
        public int getSize() { return type.getSize(); }
    }
    record Event(String op, List<Expr> args) {}
    record Edge(int target, String guard) {}
    static Result compare(MethodNode a, MethodNode b, JarIndex aj, JarIndex bj) {
        if (!FourthNormalizations.enabled("paths")) return new Result(false,"paths disabled");
        try {
            Map<List<Event>,Integer> x = new Proof(a,aj).paths(), y = new Proof(b,bj).paths();
            return new Result(x.equals(y), x.equals(y) ? "path multisets equal (" + x.values().stream().mapToInt(i->i).sum() + ")"
                    : "path event multisets differ (left=" + x.values().stream().mapToInt(i->i).sum()
                    + ", right=" + y.values().stream().mapToInt(i->i).sum() + ")");
        } catch (AnalyzerException | RuntimeException ex) {
            return new Result(false,"paths rejected: " + ex.getMessage());
        } catch (StackOverflowError limit) {
            return new Result(false,"paths rejected: recursion limit");
        }
    }
    static final class State {
        Frame<Val> frame;
        List<Event> events = new ArrayList<>();
        Set<Expr> nonnull = new HashSet<>();
        List<Expr> locks = new ArrayList<>();
        Map<Expr,Integer> allocations = new HashMap<>();
        int epoch;
        State(Frame<Val> frame) { this.frame=frame; }
        State(State s) { frame=new Frame<>(s.frame); events=new ArrayList<>(s.events);
            nonnull=new HashSet<>(s.nonnull); locks=new ArrayList<>(s.locks); allocations=new HashMap<>(s.allocations);epoch=s.epoch; }
    }
    static final class Proof {
        final MethodNode m; final JarIndex jar;
        final List<AbstractInsnNode> code=new ArrayList<>();
        final Map<AbstractInsnNode,Integer> pos=new IdentityHashMap<>();
        final Map<List<Event>,Integer> result=new HashMap<>();
        final BitSet reached=new BitSet();
        int exits, steps;
        Proof(MethodNode method,JarIndex jar) throws AnalyzerException {
            this.jar=jar; m=jar.beforeWebs.getOrDefault(method,method);
            if(m.instructions.size()==0 || m.visibleLocalVariableAnnotations!=null || m.invisibleLocalVariableAnnotations!=null)
                throw new IllegalArgumentException("empty body / local annotations");
            for(AbstractInsnNode n:m.instructions) {
                pos.put(n,code.size()); if(n.getOpcode()>=0)code.add(n);
                if(FourthNormalizations.annotated(n))throw new IllegalArgumentException("instruction annotations");
                if(n.getOpcode()==JSR||n.getOpcode()==RET)throw new IllegalArgumentException("JSR/RET");
            }
            for(TryCatchBlockNode t:m.tryCatchBlocks)
                if(t.visibleTypeAnnotations!=null||t.invisibleTypeAnnotations!=null)throw new IllegalArgumentException("handler annotations");
            // Check all normal components, including unreachable bytecode.
            int[] color=new int[code.size()];
            for(int i=0;i<code.size();i++) cycle(i,color);
            new Analyzer<>(new BasicVerifier()).analyze("Paths",m);
        }
        List<Integer> successors(int p) {
            AbstractInsnNode n=code.get(p); int op=n.getOpcode();
            if(n instanceof JumpInsnNode j)return op==GOTO?List.of(pos.get(j.label)):List.of(pos.get(j.label),p+1);
            if(n instanceof TableSwitchInsnNode s){List<Integer> a=new ArrayList<>();a.add(pos.get(s.dflt));for(LabelNode l:s.labels)a.add(pos.get(l));return a;}
            if(n instanceof LookupSwitchInsnNode s){List<Integer> a=new ArrayList<>();a.add(pos.get(s.dflt));for(LabelNode l:s.labels)a.add(pos.get(l));return a;}
            return op==ATHROW||op>=IRETURN&&op<=RETURN?List.of():List.of(p+1);
        }
        void cycle(int p,int[] color) {
            if(p<0||p>=code.size())throw new IllegalArgumentException("invalid normal successor");
            if(color[p]==1)throw new IllegalArgumentException("normal control-flow cycle");
            if(color[p]==2)return;
            color[p]=1;for(int q:successors(p))cycle(q,color);color[p]=2;
        }
        Map<List<Event>,Integer> paths() throws AnalyzerException {
            Frame<Val> f=new Frame<>(m.maxLocals,m.maxStack); BasicInterpreter types=new BasicInterpreter();
            for(int i=0;i<m.maxLocals;i++)f.setLocal(i,new Val(BasicValue.UNINITIALIZED_VALUE,new Expr("unset")));
            f.setReturn(value(types.newValue(Type.getReturnType(m.desc)),new Expr("expected")));
            int slot=0;
            if((m.access&ACC_STATIC)==0)f.setLocal(slot++,new Val(BasicValue.REFERENCE_VALUE,new Expr("this")));
            for(Type t:Type.getArgumentTypes(m.desc)){f.setLocal(slot,new Val(types.newValue(t),new Expr("arg:"+slot+":"+t)));slot+=t.getSize();}
            State s=new State(f);s.nonnull.add(new Expr("this"));
            walk(0,s,new BitSet());
            if(reached.cardinality()!=code.size())throw new IllegalArgumentException("unreachable instructions / shadowed handlers");
            return result;
        }
        void end(State s) {
            if(++exits>4096)throw new IllegalArgumentException("path limit 4096 exceeded");
            result.merge(List.copyOf(s.events),1,Integer::sum);
        }
        void event(State s,String op,List<Expr> args){s.events.add(new Event(op,List.copyOf(args)));}
        boolean standardSelf(int p,TryCatchBlockNode t,State s) {
            int h=pos.get(t.handler);
            if(t.type!=null&&!t.type.equals("java/lang/Throwable")||h+4>=code.size()||p!=h+2)return false;
            if(!(code.get(h) instanceof VarInsnNode store)||store.getOpcode()!=ASTORE
                    ||!(code.get(h+1) instanceof VarInsnNode lock)||lock.getOpcode()!=ALOAD
                    ||code.get(h+2).getOpcode()!=MONITOREXIT
                    ||!(code.get(h+3) instanceof VarInsnNode load)||load.getOpcode()!=ALOAD||load.var!=store.var
                    ||code.get(h+4).getOpcode()!=ATHROW)return false;
            Expr receiver=s.frame.getStack(s.frame.getStackSize()-1).expr;
            return !s.locks.isEmpty()&&s.locks.get(s.locks.size()-1).equals(receiver);
        }
        void walk(int p,State s,BitSet visited) throws AnalyzerException {
            if(++steps>500000)throw new IllegalArgumentException("path work limit 500000 exceeded");
            if(visited.get(p))throw new IllegalArgumentException("exception control-flow cycle / nonstandard monitor handler");
            visited=(BitSet)visited.clone();visited.set(p);
            reached.set(p);
            AbstractInsnNode n=code.get(p);int op=n.getOpcode();
            State before=new State(s);Eval eval=new Eval(this,s);
            if(op==MONITOREXIT)for(TryCatchBlockNode t:m.tryCatchBlocks)
                if(p>=pos.get(t.start)&&p<pos.get(t.end)&&visited.get(pos.get(t.handler))
                        && !standardSelf(p,t,before))throw new IllegalArgumentException("nonstandard self-covering monitor handler");
            if(op==RETURN)event(s,"RETURN",List.of());
            s.frame.execute(n,eval);
            boolean canThrow=eval.canThrow;
            if(canThrow) {
                List<String> coverage=new ArrayList<>();
                for(TryCatchBlockNode t:m.tryCatchBlocks)if(p>=pos.get(t.start)&&p<pos.get(t.end)&&!standardSelf(p,t,before))
                    coverage.add(t.type==null?"java/lang/Throwable":t.type);
                List<String> earlier=new ArrayList<>(); boolean catchAll=false;
                for(TryCatchBlockNode t:m.tryCatchBlocks)if(p>=pos.get(t.start)&&p<pos.get(t.end)) {
                    if(standardSelf(p,t,before))continue;
                    String type=t.type==null?"java/lang/Throwable":t.type;
                    State exceptional=new State(before);
                    event(exceptional,"EXCEPTION "+eval.operation+" coverage="+coverage+" catch="+type+" excluding="+earlier,eval.operands);
                    exceptional.epoch++;
                    exceptional.frame.clearStack();
                    exceptional.frame.push(new Val(BasicValue.REFERENCE_VALUE,new Expr("exception:"+exceptional.events.size())));
                    walk(pos.get(t.handler),exceptional,visited);
                    earlier.add(type);
                    if(type.equals("java/lang/Throwable")){catchAll=true;break;}
                }
                if(!catchAll) {
                    State exceptional=new State(before);
                    event(exceptional,"THROW "+eval.operation+" excluding="+earlier,eval.operands);end(exceptional);
                }
            }
            if(op==ATHROW)return;
            if(op>=IRETURN&&op<=RETURN){if(!s.locks.isEmpty())throw new IllegalArgumentException("unbalanced monitors");end(s);return;}
            if(n instanceof JumpInsnNode j && op!=GOTO) {
                for(boolean taken:new boolean[]{true,false}) {
                    State branch=new State(s);event(branch,"BRANCH",List.of(condition(op,eval.operands,taken)));
                    walk(taken?pos.get(j.label):p+1,branch,visited);
                }
            } else if(n instanceof TableSwitchInsnNode t) {
                for(int i=0;i<t.labels.size();i++){State b=new State(s);event(b,"CASE "+(t.min+i),eval.operands);walk(pos.get(t.labels.get(i)),b,visited);}
                List<Integer> keys=new ArrayList<>();for(int k=t.min;k<=t.max;k++)keys.add(k);
                event(s,"DEFAULT "+keys,eval.operands);walk(pos.get(t.dflt),s,visited);
            } else if(n instanceof LookupSwitchInsnNode t) {
                for(int i=0;i<t.labels.size();i++){State b=new State(s);event(b,"CASE "+t.keys.get(i),eval.operands);walk(pos.get(t.labels.get(i)),b,visited);}
                event(s,"DEFAULT "+t.keys,eval.operands);walk(pos.get(t.dflt),s,visited);
            } else walk(successors(p).get(0),s,visited);
        }
    }
    static Val value(BasicValue t,Expr e){return t==null?null:new Val(t,e);}
    static Expr condition(int op,List<Expr> values,boolean taken) {
        Expr a=values.get(0), b=values.size()==2?values.get(1):new Expr(op==IFNULL||op==IFNONNULL?"CONST null":"CONST Integer:0");
        String relation=switch(op) {
            case IFEQ,IF_ICMPEQ,IF_ACMPEQ,IFNULL -> "eq";
            case IFNE,IF_ICMPNE,IF_ACMPNE,IFNONNULL -> "ne";
            case IFLT,IF_ICMPLT -> "lt";case IFGE,IF_ICMPGE -> "ge";
            case IFGT,IF_ICMPGT -> "gt";case IFLE,IF_ICMPLE -> "le";
            default -> throw new IllegalArgumentException("unsupported branch");
        };
        if(!taken)relation=switch(relation){case "eq"->"ne";case "ne"->"eq";case "lt"->"ge";case "ge"->"lt";case "gt"->"le";default->"gt";};
        if(relation.equals("gt")||relation.equals("le")){Expr x=a;a=b;b=x;relation=relation.equals("gt")?"lt":"ge";}
        if((relation.equals("eq")||relation.equals("ne"))&&a.toString().compareTo(b.toString())>0){Expr x=a;a=b;b=x;}
        return new Expr(relation,List.of(a,b));
    }
    static final class Eval extends Interpreter<Val> {
        final BasicInterpreter types=new BasicInterpreter();final Proof proof;final State s;
        boolean canThrow;String operation;List<Expr> operands=List.of();
        Eval(Proof proof,State s){super(ASM9);this.proof=proof;this.s=s;}
        Val operation(AbstractInsnNode n,List<Val> args,BasicValue type) {
            int op=n.getOpcode();operands=args.stream().map(Val::expr).toList();operation=ClassComparer.graphInsn(n,proof.jar);
            canThrow=!Normalizations.harmless(n,false);
            boolean observable=n instanceof MethodInsnNode||n instanceof InvokeDynamicInsnNode
                    ||op==PUTFIELD||op==PUTSTATIC||op>=IASTORE&&op<=SASTORE||op==MONITORENTER||op==MONITOREXIT
                    ||op==NEW||op==NEWARRAY||op==ANEWARRAY||op==MULTIANEWARRAY;
            Expr receiver=null;
            if(op==GETFIELD||op==PUTFIELD||op==ARRAYLENGTH||op>=IALOAD&&op<=SALOAD||op>=IASTORE&&op<=SASTORE
                    ||op==MONITORENTER||op==MONITOREXIT||n instanceof MethodInsnNode && op!=INVOKESTATIC)receiver=operands.get(0);
            if(n instanceof FieldInsnNode f&&(op==GETFIELD||op==GETSTATIC)) {
                Integer access=proof.jar.fieldAccess(f);
                if(access==null)throw new IllegalArgumentException("unresolved field volatility: "+f.owner+"."+f.name);
                observable=(access&ACC_VOLATILE)!=0;
                // Known non-null instance field access has no ordinary runtime failure.
                if(op==GETFIELD&&s.nonnull.contains(receiver))canThrow=false;
            }
            if(op==MONITOREXIT) {
                if(s.locks.isEmpty()||!s.locks.get(s.locks.size()-1).equals(receiver))
                    throw new IllegalArgumentException("monitor exit lacks matching acquisition");
                s.locks.remove(s.locks.size()-1);canThrow=false;
            }
            if(op==MONITORENTER)s.locks.add(receiver);
            Expr expr=new Expr(operation+" @state="+s.epoch,operands);
            if(observable) {
                if(n instanceof MethodInsnNode call && call.name.equals("<init>") && s.allocations.containsKey(receiver)) {
                    int allocation=s.allocations.remove(receiver);
                    Event start=s.events.get(allocation);
                    // One allocation/constructor event, anchored at NEW. The completion boundary
                    // retains every intervening call/check/write's order relative to initialization.
                    s.events.set(allocation,new Event(start.op+" INIT "+operation+" completion="+s.events.size(),operands));
                } else proof.event(s,operation,operands);
                s.epoch++;
                expr=new Expr("result:"+s.events.size()+":"+operation,operands);
                if(op==NEW)s.allocations.put(expr,s.events.size()-1);
            } else if(canThrow&&op!=ATHROW) {
                // Retain the successful check too: dropping unused division/cast/array reads is unsafe.
                proof.event(s,"CHECK "+operation,operands);
                // Class initialization / a condy bootstrap can mutate arbitrary application state.
                if(op==GETSTATIC||n instanceof LdcInsnNode ldc && ldc.cst instanceof ConstantDynamic)s.epoch++;
            }
            if(op>=IRETURN&&op<=ARETURN)proof.event(s,ClassComparer.OPCODES[op],operands);
            if(receiver!=null)s.nonnull.add(receiver);
            if(n instanceof FieldInsnNode||op>=IALOAD&&op<=SALOAD)return value(type,expr);
            if(observable)return value(type,expr);
            return value(type,new Expr(operation,operands));
        }
        public Val newValue(Type t){return value(types.newValue(t),new Expr("unset"));}
        public Val newOperation(AbstractInsnNode n)throws AnalyzerException{return operation(n,List.of(),types.newOperation(n));}
        public Val copyOperation(AbstractInsnNode n,Val v)throws AnalyzerException{types.copyOperation(n,v.type);return v;}
        public Val unaryOperation(AbstractInsnNode n,Val v)throws AnalyzerException{return operation(n,List.of(v),types.unaryOperation(n,v.type));}
        public Val binaryOperation(AbstractInsnNode n,Val a,Val b)throws AnalyzerException{return operation(n,List.of(a,b),types.binaryOperation(n,a.type,b.type));}
        public Val ternaryOperation(AbstractInsnNode n,Val a,Val b,Val c)throws AnalyzerException{return operation(n,List.of(a,b,c),types.ternaryOperation(n,a.type,b.type,c.type));}
        public Val naryOperation(AbstractInsnNode n,List<? extends Val> args)throws AnalyzerException{return operation(n,new ArrayList<>(args),types.naryOperation(n,args.stream().map(Val::type).toList()));}
        public void returnOperation(AbstractInsnNode n,Val a,Val b){}
        public Val merge(Val a,Val b){throw new IllegalArgumentException("path frames must not merge");}
    }
}
