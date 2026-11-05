package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Transactional, bottom-up proof for position-corresponding metafactory sites. */
final class LambdaCallsites {
    private LambdaCallsites() {}
    private record Pair(MethodNode left,MethodNode right) {}
    static void pair(ClassNode left,ClassNode right,JarIndex lj,JarIndex rj) {
        if(!FourthNormalizations.enabled("callsites"))return;
        Proof proof=new Proof(left,right,lj,rj);
        Map<String,String> oldLeft=new TreeMap<>(lj.callsiteIds),oldRight=new TreeMap<>(rj.callsiteIds);
        Map<String,JarIndex.MethodResult> results=new TreeMap<>(lj.methodResults.getOrDefault(left.name,Map.of()));
        boolean success=false;
        try {
            for(MethodNode m:left.methods)if(!m.name.startsWith("lambda$")) {
                MethodNode r=find(right,m.name,m.desc);
                if(r!=null&&!proof.collect(m,r))return;
            }
            // Shared targets require every site to participate in this same bijection.
            if(!proof.allSites(left,proof.leftSites,proof.forward.keySet())
                    ||!proof.allSites(right,proof.rightSites,proof.reverse.keySet()))return;
            for(Pair p:proof.ordered) {
                MethodNode a=p.left,b=p.right;
                String aid=lj.remapMethod(left.name,a.name,a.desc),bid=rj.remapMethod(right.name,b.name,b.desc);
                if(aid.equals(bid))continue;
                if(lj.exact(left.name,a)||rj.exact(right.name,b))return;
                if(ClassComparer.compareMethod(a.name+a.desc,a,b,lj,rj,false)!=null)return;
                String detail=lj.methodResults.getOrDefault(left.name,Map.of()).getOrDefault(a.name+a.desc,
                        new JarIndex.MethodResult("SAME","linear/normalized body equal")).toString();
                String id="lambda$callsite$"+a.name+"$"+b.name;
                lj.callsiteIds.put(left.name+"#"+a.name+a.desc,id);
                rj.callsiteIds.put(right.name+"#"+b.name+b.desc,id);
                lj.clearLambdaIds();rj.clearLambdaIds();
                lj.result(a,"LAMBDA_PAIRED_BY_CALLSITE",b.name+b.desc+"; "+detail);
            }
            success=true;
        } finally {
            if(!success) {
                lj.callsiteIds.clear();lj.callsiteIds.putAll(oldLeft);
                rj.callsiteIds.clear();rj.callsiteIds.putAll(oldRight);
                lj.methodResults.put(left.name,results);
                lj.clearLambdaIds();rj.clearLambdaIds();
            }
        }
    }
    private static MethodNode find(ClassNode c,String name,String desc) {
        List<MethodNode> found=c.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).toList();
        return found.size()==1?found.get(0):null;
    }
    private static List<InvokeDynamicInsnNode> sites(MethodNode m) {
        List<InvokeDynamicInsnNode> out=new ArrayList<>();
        for(AbstractInsnNode n:m.instructions)if(n instanceof InvokeDynamicInsnNode i)out.add(i);
        return out;
    }
    private static MethodNode target(ClassNode c,InvokeDynamicInsnNode i) {
        if(!FourthNormalizations.lambda(i))return null;
        Handle h=(Handle)i.bsmArgs[1];
        if(!h.getOwner().equals(c.name)||!h.getName().startsWith("lambda$"))return null;
        MethodNode m=find(c,h.getName(),h.getDesc());
        return m!=null&&(m.access&Opcodes.ACC_PRIVATE)!=0?m:null;
    }
    private static boolean compatible(InvokeDynamicInsnNode a,InvokeDynamicInsnNode b) {
        if(!a.name.equals(b.name)||!a.desc.equals(b.desc)||!a.bsm.equals(b.bsm)
                ||a.bsmArgs.length!=b.bsmArgs.length)return false;
        for(int i=0;i<a.bsmArgs.length;i++)if(i!=1&&!Objects.equals(a.bsmArgs[i],b.bsmArgs[i]))return false;
        Handle x=(Handle)a.bsmArgs[1],y=(Handle)b.bsmArgs[1];
        // Normalizations.prepare already canonicalizes private H5/H7 handles.
        return x.getTag()==y.getTag()&&x.isInterface()==y.isInterface()
                &&x.getOwner().equals(y.getOwner())&&x.getDesc().equals(y.getDesc());
    }
    private static final class Proof {
        final ClassNode left,right;final JarIndex lj,rj;
        final Map<MethodNode,MethodNode> forward=new IdentityHashMap<>(),reverse=new IdentityHashMap<>();
        final Set<MethodNode> active=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<InvokeDynamicInsnNode> leftSites=Collections.newSetFromMap(new IdentityHashMap<>()),
                rightSites=Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Pair> ordered=new ArrayList<>();
        Proof(ClassNode l,ClassNode r,JarIndex a,JarIndex b){left=l;right=r;lj=a;rj=b;}
        boolean collect(MethodNode a,MethodNode b) {
            if(active.contains(a))return false;
            List<InvokeDynamicInsnNode> as=sites(a),bs=sites(b);
            if(as.size()!=bs.size())return as.stream().noneMatch(i->target(left,i)!=null)
                    &&bs.stream().noneMatch(i->target(right,i)!=null);
            active.add(a);
            for(int i=0;i<as.size();i++) {
                InvokeDynamicInsnNode x=as.get(i),y=bs.get(i);
                MethodNode lm=target(left,x),rm=target(right,y);
                if(lm==null&&rm==null)continue;
                if(lm==null||rm==null||!compatible(x,y))return false;
                // Content identity remains the first rule. Only unmatched identities
                // enter the stricter callsite bijection (javac may share equal bodies).
                if(lj.remapMethod(left.name,lm.name,lm.desc).equals(rj.remapMethod(right.name,rm.name,rm.desc)))continue;
                leftSites.add(x);rightSites.add(y);
                if(forward.containsKey(lm)) {if(forward.get(lm)!=rm||active.contains(lm))return false;continue;}
                if(reverse.containsKey(rm))return false;
                forward.put(lm,rm);reverse.put(rm,lm);
                if(!collect(lm,rm))return false;
                ordered.add(new Pair(lm,rm));
            }
            active.remove(a);return true;
        }
        boolean allSites(ClassNode c,Set<InvokeDynamicInsnNode> visited,Set<MethodNode> targets) {
            for(MethodNode m:c.methods)for(InvokeDynamicInsnNode i:sites(m))
                if(targets.contains(target(c,i))&&!visited.contains(i))return false;
            return true;
        }
    }
}
