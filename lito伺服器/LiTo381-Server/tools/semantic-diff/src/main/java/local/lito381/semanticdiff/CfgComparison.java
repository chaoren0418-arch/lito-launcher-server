package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import static local.lito381.semanticdiff.BlockExpressions.*;

/** Entry-rooted labelled graph isomorphism. No search by similar block contents. */
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
        List<Block> left=view(a,aj),right=view(b,bj);
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
                    ||!b.events().get(0).catches().isEmpty()||!b.copies().isEmpty()||b.edges().size()!=1)return id;
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
        return new Block(events,b.exit(),b.copies(),edges);
    }
}
