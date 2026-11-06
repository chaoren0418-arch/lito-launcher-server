package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import static local.lito381.semanticdiff.BlockExpressions.*;

/** Entry-rooted labelled graph isomorphism after proven identical-block coalescing. */
final class CfgComparison implements Opcodes {
    private CfgComparison() {}
    record Result(boolean same, String reason) {}
    static Result compare(MethodNode a, MethodNode b, JarIndex aj, JarIndex bj) {
        if (!FourthNormalizations.enabled("cfg")) return new Result(false,"CFG disabled");
        try {
            if (direct(a,b,aj,bj)) return new Result(true,"entry-rooted labelled CFGs equal");
            if (FourthNormalizations.enabled("transport")
                    && direct(CfgTransport.normalize(a),CfgTransport.normalize(b),aj,bj))
                return new Result(true,"entry-rooted labelled CFGs equal after local transport");
            return new Result(false,"labelled CFGs differ");
        } catch (org.objectweb.asm.tree.analysis.AnalyzerException | RuntimeException ex) {
            return new Result(false,"CFG rejected: " + ex.getMessage());
        }
    }

    private static boolean direct(MethodNode a,MethodNode b,JarIndex aj,JarIndex bj)
            throws org.objectweb.asm.tree.analysis.AnalyzerException {
        List<Block> left=view(a,aj,true),right=view(b,bj,true);
        if(isomorphic(a,left,right))return true;
        if(!FourthNormalizations.enabled("cfg-merge"))return false;
        if(isomorphic(a,coalesce(left),coalesce(right)))return true;
        // javac can inline the final cleanup after the last protected operation.
        // Respect try boundaries as block leaders to expose that shared suffix.
        if(isomorphic(a,coalesce(view(a,aj,true,true)),coalesce(view(b,bj,true,true))))return true;
        // A shared cleanup can start inside an otherwise straight-line block
        // (e.g. STORE; LOAD; IF). Empty-stack cuts expose the same basic blocks
        // on both sides without moving any operation or changing exception edges.
        return isomorphic(a,coalesce(view(a,aj,true,true,true)),coalesce(view(b,bj,true,true,true)));
    }

    private static boolean isomorphic(MethodNode a,List<Block> left,List<Block> right) {
        Map<Integer,Integer> blocks=new HashMap<>(),back=new HashMap<>();
        Map<Integer,Integer> slots=new HashMap<>(),reverse=new HashMap<>();
        for(int i=0;i<LocalWebs.parameters(a);i++){slots.put(i,i);reverse.put(i,i);}
        ArrayDeque<int[]> pending=new ArrayDeque<>();pending.add(new int[]{0,0});
        while(!pending.isEmpty()) {
            int[] pair=pending.removeFirst();int li=target(left,pair[0]),ri=target(right,pair[1]);
            if(blocks.containsKey(li)){if(blocks.get(li)!=ri)return false;continue;}
            if(back.containsKey(ri))return false;
            blocks.put(li,ri);back.put(ri,li);
            Block l=canonical(left.get(li)),r=canonical(right.get(ri));
            if(!l.entry().equals(r.entry()))return mismatch(li,ri,l.entry(),r.entry());
            if(l.events().size()!=r.events().size()||l.edges().size()!=r.edges().size())return mismatch(li,ri,l,r);
            for(int i=0;i<l.events().size();i++) {
                Event x=l.events().get(i),y=r.events().get(i);
                if(!x.text().equals(y.text())||!match(x.args(),y.args(),slots,reverse)
                        ||x.catches().size()!=y.catches().size())return mismatch(li,ri,x,y);
                // Event position and handler priority are edge labels, not unordered sets.
                for(int j=0;j<x.catches().size();j++) {
                    String xc=x.catches().get(j),yc=y.catches().get(j);
                    int xp=xc.indexOf(':'),yp=yc.indexOf(':');
                    if(!xc.substring(xp).equals(yc.substring(yp)))return false;
                    pending.add(new int[]{Integer.parseInt(xc.substring(0,xp)),Integer.parseInt(yc.substring(0,yp))});
                }
            }
            // DUP and reloading an unchanged local are transport only. Exact operand
            // origins, ordered stores/events and exit stack already account for every use.
            if(!match(l.exit(),r.exit(),slots,reverse))return mismatch(li,ri,l,r);
            // Canonical true/false, switch keys/default, or the sole fallthrough edge.
            for(int i=0;i<l.edges().size();i++)pending.add(new int[]{l.edges().get(i),r.edges().get(i)});
        }
        // Refuse disconnected components; do not invent an arbitrary correspondence.
        for(int i=0;i<left.size();i++)if(!blocks.containsKey(target(left,i)))return false;
        for(int i=0;i<right.size();i++)if(!back.containsKey(target(right,i)))return false;
        return boundaries(left,right,blocks,slots,reverse);
    }

    // A stack input is NOT an unqualified union of producer values. It is a phi
    // with one ordered vector per labelled predecessor. Each vector position is
    // (predecessor block, edge label, stack position) -> block-local expression.
    // "event" expressions reference the ordered expression DAG compared above;
    // "stack" expressions reference that predecessor's own input position.
    // Comparing every equation under the entry-rooted bijection preserves these
    // correlations even on backedges, without recursively expanding a loop.
    private record Predecessor(int block,int event,int arm) {}

    /** Redirect identical blocks to a deterministic representative, then repeat:
     * merging a tail can make its predecessors identical on the next pass.
     * Local identities are exact within one method; block-local dead temporaries
     * have already disappeared in #174's expression view. Never alpha-rename live locals.
     */
    static List<Block> coalesce(List<Block> source) {
        List<Block> graph=compact(source);
        boolean changed;
        do {
            changed=false;
            var inputs=incoming(graph);
            Map<StackPosition,Origin> origins=new HashMap<>();
            List<List<Origin>> stacks=new ArrayList<>();
            for(int i=0;i<graph.size();i++)stacks.add(stackOrigins(graph,inputs,i,origins));
            int[] representatives=new int[graph.size()];
            Map<MergeKey,Integer> groups=new HashMap<>();
            for(int i=0;i<graph.size();i++) {
                representatives[i]=i;
                if(stacks.get(i)==null)continue;
                Integer first=groups.putIfAbsent(new MergeKey(graph.get(i),stacks.get(i)),i);
                if(first!=null){representatives[i]=first;changed=true;}
            }
            if(changed)graph=redirect(graph,representatives);
        } while(changed);
        return graph;
    }

    // An event/local is qualified by its producer block, not merely its position
    // in a block. Stack transport is followed only if EVERY incoming edge proves
    // the same origin. Mixed phis or cycles are unknown, never an unordered union.
    private record Origin(int block,Expr value,String literal) {}
    private record StackPosition(int block,int slot) {}
    private record MergeKey(Block block,List<Origin> stack) {}
    private static List<Origin> stackOrigins(List<Block> graph,
            Map<Integer,Map<Predecessor,List<Expr>>> inputs,int id,Map<StackPosition,Origin> cache) {
        List<Origin> values=new ArrayList<>();
        for(int slot=0;slot<graph.get(id).entry().size();slot++) {
            Origin origin=origin(graph,inputs,id,new Expr("stack",slot,graph.get(id).entry().get(slot).size()),new HashSet<>(),cache);
            if(origin==null)return null;
            values.add(origin);
        }
        return values;
    }
    private static Origin origin(List<Block> graph,Map<Integer,Map<Predecessor,List<Expr>>> inputs,
            int id,Expr value,Set<StackPosition> visiting,Map<StackPosition,Origin> cache) {
        if (value.kind().startsWith("literal:")) return new Origin(-1,value,value.kind());
        if(value.kind().equals("event")) {
            Event e=graph.get(id).events().get(value.id());
            if(e.args().isEmpty()&&e.catches().isEmpty() && (e.text().equals("CONST null")
                    ||List.of("CONST Integer:","CONST Long:","CONST Float:","CONST Double:","CONST str:")
                        .stream().anyMatch(e.text()::startsWith)))
                return new Origin(-1,new Expr("literal",0,value.size()),e.text());
        }
        if(!value.kind().equals("stack"))return new Origin(id,value,null);
        StackPosition key=new StackPosition(id,value.id());
        if(cache.containsKey(key))return cache.get(key);
        if(visiting.size()>=256)return null; // conservative resource bound for transport chains
        if(!visiting.add(key))return null;
        Origin common=null;
        for(var edge:inputs.get(id).entrySet()) {
            Origin current=origin(graph,inputs,edge.getKey().block(),edge.getValue().get(value.id()),visiting,cache);
            if(current==null || common!=null&&!common.equals(current)){visiting.remove(key);cache.put(key,null);return null;}
            common=current;
        }
        visiting.remove(key);cache.put(key,common);return common;
    }

    private static List<Block> compact(List<Block> graph) {
        int[] representatives=new int[graph.size()];
        for(int i=0;i<graph.size();i++)representatives[i]=target(graph,i);
        return redirect(graph,representatives);
    }
    private static List<Block> redirect(List<Block> graph,int[] representatives) {
        Map<Integer,Integer> ids=new LinkedHashMap<>();
        // Keep the method entry at zero, including an initial GOTO trampoline.
        for(int i=0;i<graph.size();i++)ids.computeIfAbsent(representatives[i],k->ids.size());
        List<Block> result=new ArrayList<>();
        for(int representative:ids.keySet()) {
            Block b=canonical(graph.get(representative));
            List<Event> events=new ArrayList<>();
            for(Event e:b.events()) {
                List<String> catches=new ArrayList<>();
                for(String edge:e.catches()) {
                    int p=edge.indexOf(':');
                    catches.add(ids.get(representatives[Integer.parseInt(edge.substring(0,p))])+edge.substring(p));
                }
                events.add(new Event(e.text(),e.args(),catches));
            }
            List<Integer> edges=b.edges().stream().map(i->ids.get(representatives[i])).toList();
            result.add(new Block(events,b.entry(),b.exit(),b.copies(),edges));
        }
        return result;
    }
    private static Map<Integer,Map<Predecessor,List<Expr>>> incoming(List<Block> graph) {
        Map<Integer,Map<Predecessor,List<Expr>>> inputs=new HashMap<>();
        for(int id=0;id<graph.size();id++)if(target(graph,id)==id)inputs.put(id,new HashMap<>());
        if(!graph.get(target(graph,0)).entry().isEmpty())
            throw new IllegalArgumentException("nonempty method entry stack");
        for(int id:inputs.keySet()) {
            Block b=canonical(graph.get(id));
            for(int arm=0;arm<b.edges().size();arm++)
                addInput(graph,inputs,b.edges().get(arm),new Predecessor(id,-1,arm),b.exit());
            for(int event=0;event<b.events().size();event++) {
                List<String> catches=b.events().get(event).catches();
                for(int arm=0;arm<catches.size();arm++) {
                    String edge=catches.get(arm);int end=edge.indexOf(':');
                    // JVM exceptional transfer discards the normal stack and
                    // supplies exactly this instruction's thrown object.
                    addInput(graph,inputs,Integer.parseInt(edge.substring(0,end)),
                            new Predecessor(id,event,arm),List.of(new Expr("exception",event,1)));
                }
            }
        }
        return inputs;
    }
    private static void addInput(List<Block> graph,Map<Integer,Map<Predecessor,List<Expr>>> inputs,
            int destination,Predecessor source,List<Expr> values) {
        int id=target(graph,destination);List<Expr> entry=graph.get(id).entry();
        if(values.size()!=entry.size())throw new IllegalArgumentException("edge stack depth mismatch");
        for(int i=0;i<entry.size();i++)if(values.get(i).size()!=entry.get(i).size())
            throw new IllegalArgumentException("edge stack category mismatch");
        if(inputs.get(id).put(source,values)!=null)throw new IllegalArgumentException("duplicate edge");
    }
    private static boolean boundaries(List<Block> left,List<Block> right,Map<Integer,Integer> blocks,
            Map<Integer,Integer> slots,Map<Integer,Integer> reverse) {
        var a=incoming(left);var b=incoming(right);
        for(var pair:blocks.entrySet()) {
            var l=a.get(pair.getKey());var r=b.get(pair.getValue());
            if(l.size()!=r.size())return false;
            for(var edge:l.entrySet()) {
                Predecessor p=edge.getKey();Integer source=blocks.get(p.block());
                if(source==null)return false;
                List<Expr> values=r.get(new Predecessor(source,p.event(),p.arm()));
                if(values==null||!match(edge.getValue(),values,slots,reverse))
                    return mismatch(pair.getKey(),pair.getValue(),edge,values);
            }
        }
        return true;
    }

    private static boolean mismatch(int left,int right,Object a,Object b) {
        if(Boolean.getBoolean("semanticdiff.traceCfg"))System.err.println("CFG block "+left+"/"+right+"\nLEFT "+a+"\nRIGHT "+b);
        return false;
    }

    private static int target(List<Block> graph,int id) {
        Set<Integer> seen=new HashSet<>();
        while(true) {
            if(!seen.add(id))throw new IllegalArgumentException("GOTO-only cycle");
            Block b=graph.get(id);
            if(b.events().size()!=1||!b.events().get(0).text().equals("GOTO L0")
                    ||!b.events().get(0).catches().isEmpty()||!b.copies().isEmpty()||b.edges().size()!=1
                    ||!b.entry().equals(b.exit()))return id;
            id=b.edges().get(0);
        }
    }

    private static Block canonical(Block b) {
        List<Event> events=new ArrayList<>(b.events());List<Integer> edges=new ArrayList<>(b.edges());
        if(!events.isEmpty()) {
            Event last=events.get(events.size()-1);
            if(last.text().equals("GOTO L0")&&last.catches().isEmpty())events.remove(events.size()-1);
            else {
                // Odd/even opcode adjacency is not uniform across the JVM branch families.
                int[] from={IFEQ,IFLT,IFGT,IF_ICMPEQ,IF_ICMPLT,IF_ICMPGT,IF_ACMPEQ,IFNULL};
                int[] to={IFNE,IFGE,IFLE,IF_ICMPNE,IF_ICMPGE,IF_ICMPLE,IF_ACMPNE,IFNONNULL};
                for(int i=0;i<from.length;i++)if(last.text().equals(ClassComparer.OPCODES[from[i]]+" L0")) {
                    if(edges.size()!=2)throw new IllegalArgumentException("conditional successors");
                    events.set(events.size()-1,new Event(ClassComparer.OPCODES[to[i]]+" L0",last.args(),last.catches()));
                    Collections.swap(edges,0,1);break;
                }
            }
        }
        return new Block(events,b.entry(),b.exit(),b.copies(),edges);
    }
}
