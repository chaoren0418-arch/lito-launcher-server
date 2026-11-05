package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** CFG isomorphism with ordered normal/exception edges and a global bijection of local slots.
 * Instruction-sized blocks are a refinement of basic blocks; transparent GOTO edges are contracted.
 */
final class LayoutComparison implements Opcodes {
    record Edge(String kind, int target) {}
    record Vertex(AbstractInsnNode insn, String text, List<Edge> edges) {}
    record Graph(List<Vertex> nodes, int entry, Map<Integer,List<Set<Integer>>> inputs,
            Map<Set<Integer>,Integer> copies) {}
    private LayoutComparison() {}

    static boolean same(MethodNode left, MethodNode right, JarIndex lj, JarIndex rj) {
        return difference(left, right, lj, rj) == null;
    }

    static boolean transportSame(TransportComparison.View left, TransportComparison.View right,
            JarIndex lj, JarIndex rj, boolean layout) {
        Graph a = graph(left.method(),lj,left,layout), b = graph(right.method(),rj,right,layout);
        if (a.nodes.size() != b.nodes.size()) return false;
        State s = new State();
        for (int i=0; i<LocalWebs.parameters(left.method()); i++) { s.slots.put(i,i); s.reverseSlots.put(i,i); }
        if (!layout) {
            for (int i=0; i<a.nodes.size(); i++) {
                if (!match(a,b,i,i,s)) return false;
                for (var e : s.nodes.entrySet()) if (!e.getKey().equals(e.getValue())) return false;
            }
            return dependencies(a,b,s);
        }
        if (a.nodes.isEmpty()) return true;
        return match(a,b,a.entry,b.entry,s) && complete(a,b,s,new int[]{100000});
    }

    private static boolean dependencies(Graph a, Graph b, State s) {
        Map<Set<Integer>,Integer> copies=new HashMap<>();
        for (var entry:a.copies.entrySet()) {
            Set<Integer> mapped=mapSources(entry.getKey(),s);
            if(mapped==null)return false;
            copies.merge(mapped,entry.getValue(),Integer::sum);
        }
        if(!copies.equals(b.copies))return false;
        for (int i=0;i<a.nodes.size();i++) {
            List<Set<Integer>> left=a.inputs.getOrDefault(i,List.of());
            List<Set<Integer>> right=b.inputs.getOrDefault(s.nodes.get(i),List.of());
            if (left.size()!=right.size()) return false;
            for(int k=0;k<left.size();k++) {
                Set<Integer> mapped=mapSources(left.get(k),s);
                if(mapped==null || !mapped.equals(right.get(k)))return false;
            }
        }
        return true;
    }
    private static Set<Integer> mapSources(Set<Integer> source,State s) {
        Set<Integer> mapped=new HashSet<>();
        for(int value:source) {
            Integer target=s.nodes.get(value>=0?value:-value-2);
            if(target==null)return null;
            mapped.add(value>=0?target:-target-2);
        }
        return mapped;
    }

    static String difference(MethodNode left, MethodNode right, JarIndex lj, JarIndex rj) {
        Graph a = graph(left, lj), b = graph(right, rj);
        if (a.nodes.size() != b.nodes.size()) return "graph node count left=" + a.nodes.size() + " right=" + b.nodes.size();
        State s = new State();
        int parameters = (left.access & ACC_STATIC) == 0 ? 1 : 0;
        for (Type t : Type.getArgumentTypes(left.desc)) parameters += t.getSize();
        for (int i=0;i<parameters;i++) { s.slots.put(i,i); s.reverseSlots.put(i,i); }
        if (a.nodes.isEmpty()) return null;
        if (!match(a, b, a.entry, b.entry, s)) return s.reason;
        return complete(a, b, s, new int[]{100000}) ? null : "unreachable graph components differ";
    }

    private static final class State {
        final Map<Integer,Integer> nodes = new HashMap<>(), reverse = new HashMap<>();
        final Map<Integer,Integer> slots = new HashMap<>(), reverseSlots = new HashMap<>();
        String reason = "control-flow graph differs";
        State copy() { State c = new State(); c.nodes.putAll(nodes); c.reverse.putAll(reverse);
            c.slots.putAll(slots); c.reverseSlots.putAll(reverseSlots); return c; }
    }
    private static boolean bind(Map<Integer,Integer> a, Map<Integer,Integer> b, int x, int y) {
        if (a.containsKey(x)) return a.get(x) == y;
        if (b.containsKey(y)) return false;
        a.put(x,y); b.put(y,x); return true;
    }
    private static boolean match(Graph a, Graph b, int x, int y, State s) {
        Deque<int[]> work = new ArrayDeque<>(); work.add(new int[]{x,y});
        while (!work.isEmpty()) {
            int[] p = work.remove(); x=p[0]; y=p[1];
            if (x < 0 || y < 0) { if (x != y) return false; else continue; }
            if (s.nodes.containsKey(x)) { if (s.nodes.get(x) != y) return false; else continue; }
            if (!bind(s.nodes,s.reverse,x,y)) return false;
            Vertex l=a.nodes.get(x), r=b.nodes.get(y);
            s.reason = "graph vertices left[" + x + "]=" + l.text + " right[" + y + "]=" + r.text
                    + " edges=" + l.edges.size() + "/" + r.edges.size();
            if (!l.text.equals(r.text) || l.edges.size()!=r.edges.size()) return false;
            if (l.insn instanceof VarInsnNode v) {
                int w=((VarInsnNode)r.insn).var;
                if (!bind(s.slots,s.reverseSlots,v.var,w)) { s.reason += " local slots=" + v.var + "/" + w + " bindings=" + s.slots; return false; }
                int op=v.getOpcode();
                if ((op==LLOAD || op==DLOAD || op==LSTORE || op==DSTORE)
                        && !bind(s.slots,s.reverseSlots,v.var+1,w+1)) return false;
            } else if (l.insn instanceof IincInsnNode v && !bind(s.slots,s.reverseSlots,v.var,((IincInsnNode)r.insn).var)) return false;
            for (int i=0;i<l.edges.size();i++) {
                Edge le=l.edges.get(i), re=r.edges.get(i);
                if (!le.kind.equals(re.kind)) return false;
                work.add(new int[]{le.target,re.target});
            }
        }
        return true;
    }
    private static boolean complete(Graph a, Graph b, State s, int[] budget) {
        if (s.nodes.size()==a.nodes.size()) return dependencies(a,b,s);
        int x=0; while(s.nodes.containsKey(x)) x++;
        for(int y=0;y<b.nodes.size();y++) if(!s.reverse.containsKey(y)) {
            if (--budget[0]<0) throw new IllegalArgumentException("layout graph search budget exceeded");
            State trial=s.copy();
            if(match(a,b,x,y,trial) && complete(a,b,trial,budget)) return true;
        }
        return false;
    }
    private static boolean transparent(AbstractInsnNode n) {
        return n.getOpcode()<0 || n.getOpcode()==GOTO && n.visibleTypeAnnotations==null && n.invisibleTypeAnnotations==null;
    }
    private static AbstractInsnNode resolve(AbstractInsnNode n) {
        Set<AbstractInsnNode> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        while(n!=null && transparent(n)) {
            if(!seen.add(n)) return n; // Retain a pure jump cycle rather than erase an infinite loop.
            n=n instanceof JumpInsnNode j ? j.label : n.getNext();
        }
        return n;
    }
    private static Graph graph(MethodNode m, JarIndex jar) {
        return graph(m,jar,null,true);
    }
    private static Graph graph(MethodNode m, JarIndex jar, TransportComparison.View transport, boolean collapseGoto) {
        for (AbstractInsnNode n : m.instructions) if (n.getOpcode() == JSR || n.getOpcode() == RET)
            throw new IllegalArgumentException("layout does not support legacy JSR/RET subroutines");
        Map<AbstractInsnNode,Integer> ids=new IdentityHashMap<>(), positions=new IdentityHashMap<>();
        List<AbstractInsnNode> code=new ArrayList<>(); int pos=0;
        for(AbstractInsnNode n:m.instructions) { positions.put(n,pos++); AbstractInsnNode r=collapseGoto?resolve(n):FourthNormalizations.next(n);
            if(r!=null && !ids.containsKey(r)) { ids.put(r,code.size()); code.add(r); } }
        boolean monitor=code.stream().anyMatch(n->n.getOpcode()==MONITORENTER);
        List<Vertex> vertices=new ArrayList<>();
        for(AbstractInsnNode n:code) {
            List<Edge> edges=new ArrayList<>(); int op=n.getOpcode();
            if(n instanceof JumpInsnNode j) {
                edges.add(edge("jump",j.label,ids,collapseGoto));
                if(op!=GOTO) edges.add(edge("fall",n.getNext(),ids,collapseGoto));
            } else if(n instanceof TableSwitchInsnNode t) {
                edges.add(edge("default",t.dflt,ids,collapseGoto));
                for(int i=0;i<t.labels.size();i++) edges.add(edge("case:"+(t.min+i),t.labels.get(i),ids,collapseGoto));
            } else if(n instanceof LookupSwitchInsnNode t) {
                edges.add(edge("default",t.dflt,ids,collapseGoto));
                for(int i=0;i<t.labels.size();i++) edges.add(edge("case:"+t.keys.get(i),t.labels.get(i),ids,collapseGoto));
            } else if(op!=ATHROW && !(op>=IRETURN && op<=RETURN)) edges.add(edge("fall",n.getNext(),ids,collapseGoto));
            if(!Normalizations.harmless(n,monitor)) {
                for(TryCatchBlockNode t:m.tryCatchBlocks) if(positions.get(n)>=positions.get(t.start) && positions.get(n)<positions.get(t.end))
                    edges.add(edge("catch:"+ClassComparer.graphCatch(t,jar),t.handler,ids,collapseGoto));
            }
            vertices.add(new Vertex(n,ClassComparer.graphInsn(n,jar),edges));
        }
        Map<Integer,List<Set<Integer>>> inputs = new HashMap<>();
        if (transport != null) for (AbstractInsnNode n : code) {
            List<Set<Integer>> operands=new ArrayList<>();
            for (var value : transport.inputs().getOrDefault(n,List.of())) {
                // Check before resolving labels: mapping must never collapse ambiguous origins.
                if(value.insns.size()!=1) throw new IllegalArgumentException("ambiguous or unresolved transport operand");
                Set<Integer> producers=new HashSet<>();
                for(AbstractInsnNode producer:value.insns) {
                    AbstractInsnNode resolved=producer instanceof LabelNode ? (collapseGoto?resolve(producer):FourthNormalizations.next(producer)) : producer;
                    Integer id=ids.get(resolved);
                    if(id==null) throw new IllegalArgumentException("removed transport producer");
                    producers.add(producer instanceof LabelNode ? -id-2 : id);
                }
                operands.add(producers);
            }
            inputs.put(ids.get(n),operands);
        }
        Map<Set<Integer>,Integer> copies=new HashMap<>();
        if(transport!=null)for(var entry:transport.copyCounts().entrySet()) {
            if(entry.getKey().size()!=1)throw new IllegalArgumentException("ambiguous or unresolved copy producer");
            Set<Integer> producers=new HashSet<>();
            for(AbstractInsnNode producer:entry.getKey()) {
                AbstractInsnNode resolved=producer instanceof LabelNode ? (collapseGoto?resolve(producer):FourthNormalizations.next(producer)) : producer;
                Integer id=ids.get(resolved);
                if(id==null)throw new IllegalArgumentException("unresolved copy producer");
                producers.add(producer instanceof LabelNode ? -id-2 : id);
            }
            if(producers.isEmpty())throw new IllegalArgumentException("unknown copy producer");
            copies.merge(producers,entry.getValue(),Integer::sum);
        }
        return new Graph(vertices,code.isEmpty() ? -1 : ids.get(collapseGoto?resolve(m.instructions.getFirst()):FourthNormalizations.next(m.instructions.getFirst())),inputs,copies);
    }
    private static Edge edge(String kind, AbstractInsnNode n, Map<AbstractInsnNode,Integer> ids) {
        return edge(kind,n,ids,true);
    }
    private static Edge edge(String kind, AbstractInsnNode n, Map<AbstractInsnNode,Integer> ids, boolean collapseGoto) {
        AbstractInsnNode r=collapseGoto?resolve(n):FourthNormalizations.next(n); return new Edge(kind,r==null ? -1 : ids.get(r));
    }
}
