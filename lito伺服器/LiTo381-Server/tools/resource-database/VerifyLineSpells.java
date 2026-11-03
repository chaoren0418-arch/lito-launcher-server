import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Independent inverse of the declared line adapters; all remaining opcodes compare to formal. */
final class VerifyLineSpells {
    static final String P="local/lito381/server/",H=P+"CombatLineSpells",C="L"+P+"ClientSession;",S="L"+P+"BattleSpellCatalog$DirectionalLineSpell;",PRIMARY=H+"$Primary",BATCH=P+"CombatCompanionSpells$Batch";
    static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
    static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
    static VarInsnNode local(AbstractInsnNode n,int opcode){if(!(n instanceof VarInsnNode v)||v.getOpcode()!=opcode)throw new AssertionError("line local opcode changed");return v;}
    static int slot(MethodNode m,String name){var ids=new HashSet<Integer>();for(var v:m.localVariables)if(v.name.equals(name))ids.add(v.index);if(ids.size()!=1)throw new AssertionError("line local identity missing="+name);return ids.iterator().next();}
    static void erase(MethodNode m,AbstractInsnNode... nodes){for(var n:nodes)m.instructions.remove(n);}
    static MethodNode normalize(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode v&&v.owner.equals(H)))return m;
        if(!m.name.equals("useLineAttackSpell")||!m.desc.equals("(L"+P+"Protocol$AttackSpellRequest;"+S+")V"))throw new AssertionError("unexpected source line actor hook");
        int ctx=-1,batch=-1;var counts=new TreeMap<String,Integer>();var primaries=new HashMap<Integer,String>();var lists=new HashMap<Integer,Integer>();
        for(var pair:List.of(new String[]{"primary","playerTargets"},new String[]{"primaryTower","towerTargets"},new String[]{"primaryMonster","monsterTargets"},new String[]{"primaryTimeBoss","timeBossTargets"})) {
            int id=slot(m,pair[0]);var types=new HashSet<String>();for(var v:m.localVariables)if(v.index==id)types.add(v.desc);if(types.size()!=1)throw new AssertionError("ambiguous native line primary type");primaries.put(id,types.iterator().next());lists.put(id,slot(m,pair[1]));
        }
        int primary=slot(m,"primary"),x=slot(m,"targetX"),y=slot(m,"targetY"),line=slot(m,"line"),wire=slot(m,"wireTargets"),cast=slot(m,"castNow"),recipients=slot(m,"attackRecipients");
        for(var node:m.instructions.toArray()) {
            if(!m.instructions.contains(node)||!(node instanceof MethodInsnNode call))continue;
            if(call.owner.equals(PRIMARY)) {
                if(!call.name.equals("x")||!call.desc.equals("()I"))throw new AssertionError("unexpected line primary projection");
                var load=before(call);var branch=before(load);var first=before(branch);var sx=after(call);var second=after(sx);var getY=after(second);var sy=after(getY);var jump=after(sy);
                if(local(first,ALOAD).var!=ctx||local(load,ALOAD).var!=ctx||!(branch instanceof JumpInsnNode b)||b.getOpcode()!=IFNULL||local(sx,ISTORE).var!=x||local(second,ALOAD).var!=ctx
                        ||!(getY instanceof MethodInsnNode gy)||!gy.owner.equals(PRIMARY)||!gy.name.equals("y")||!gy.desc.equals("()I")||local(sy,ISTORE).var!=y||!(jump instanceof JumpInsnNode j)||j.getOpcode()!=GOTO||jump.getNext()!=b.label)throw new AssertionError("invalid native line unknown-target fallback");
                erase(m,first,branch,load,call,sx,second,getY,sy,jump);counts.merge("fallback",1,Integer::sum);continue;
            }
            if(!call.owner.equals(H))continue;counts.merge(call.name,1,Integer::sum);
            switch(call.name) {
                case "primary" -> {
                    var request=before(call);var caster=before(request);var store=after(call);if(!call.desc.equals("("+C+"L"+P+"Protocol$AttackSpellRequest;)L"+PRIMARY+";")||local(caster,ALOAD).var!=0||local(request,ALOAD).var!=1)throw new AssertionError("invalid line primary resolution");ctx=local(store,ASTORE).var;erase(m,caster,request,call,store);
                }
                case "admit" -> {
                    var actor=before(call);var iy=before(actor);var ix=before(iy);var caster=before(ix);var branch=after(call);var ret=after(branch);
                    if(!call.desc.equals("("+C+"II"+C+")Z")||local(caster,ALOAD).var!=0||local(ix,ILOAD).var!=x||local(iy,ILOAD).var!=y||local(actor,ALOAD).var!=primary||!(branch instanceof JumpInsnNode b)||b.getOpcode()!=IFNE||ret.getOpcode()!=RETURN||ret.getNext()!=b.label)throw new AssertionError("invalid line primary map/LOS guard");erase(m,caster,ix,iy,actor,call,branch,ret);
                }
                case "collectKind" -> {
                    var actor=local(before(call),ALOAD);var branch=after(call);if(!call.desc.equals("(Ljava/lang/Object;)Z")||!primaries.containsKey(actor.var)||!(branch instanceof JumpInsnNode b)||b.getOpcode()!=IFEQ)throw new AssertionError("invalid line target-kind guard");b.setOpcode(IFNULL);m.instructions.remove(call);
                }
                case "addPrimary" -> {
                    var actor=local(before(call),ALOAD);var list=local(before(actor),ALOAD);if(!call.desc.equals("(Ljava/util/List;Ljava/lang/Object;)Z")||!Objects.equals(lists.get(actor.var),list.var))throw new AssertionError("invalid line null-safe primary insertion");m.instructions.set(call,new MethodInsnNode(INVOKEINTERFACE,"java/util/List","add","(Ljava/lang/Object;)Z",true));
                }
                case "primaryId" -> {
                    var actor=local(before(call),ALOAD);String desc=primaries.get(actor.var);if(!call.desc.equals("(Ljava/lang/Object;)J")||desc==null||actor.var==primary)throw new AssertionError("invalid line primary identity");m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,desc.substring(1,desc.length()-1),"objectId","()J",false));
                }
                case "contains" -> {
                    var actor=local(before(call),ALOAD);var caster=local(before(actor),ALOAD);var coordinate=before(caster);var original=local(before(coordinate),ALOAD);
                    if(!call.desc.equals("(Ljava/util/List;II"+C+"Ljava/lang/Object;)Z")||caster.var!=0||actor.var!=original.var||!(coordinate instanceof FieldInsnNode f&&f.name.equals("positionY")||coordinate instanceof MethodInsnNode v&&v.name.equals("y")))throw new AssertionError("invalid native line candidate geometry");erase(m,caster,actor);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"BattleStraightLine","contains","(Ljava/util/List;II)Z",false));
                }
                case "allKinds" -> {
                    var branch=after(call);var old=after(branch);if(!call.desc.equals("()Z")||!(branch instanceof JumpInsnNode b)||b.getOpcode()!=IFNE||!(old instanceof JumpInsnNode o)||o.getOpcode()!=GOTO||after(o)!=after(b.label))throw new AssertionError("invalid line cross-kind continuation");erase(m,call,branch);
                }
                case "capture" -> {
                    var ray=before(call);var context=before(ray);var spell=before(context);var caster=before(spell);var store=after(call);
                    if(!call.desc.equals("("+C+S+"L"+PRIMARY+";Ljava/util/List;)L"+BATCH+";")||local(caster,ALOAD).var!=0||local(spell,ALOAD).var!=2||local(context,ALOAD).var!=ctx||local(ray,ALOAD).var!=line||local(before(caster),ASTORE).var!=line)throw new AssertionError("invalid line controlled batch capture");batch=local(store,ASTORE).var;erase(m,caster,spell,context,ray,call,store);
                }
                case "draw","commit" -> {
                    var list=before(call);var when=before(list);var context=before(when);var spell=before(context);var caster=before(spell);boolean draw=call.name.equals("draw");
                    if(!call.desc.equals("("+C+S+"L"+BATCH+";JLjava/util/List;)"+(draw?"L"+BATCH+";":"V"))||local(caster,ALOAD).var!=0||local(spell,ALOAD).var!=2||local(context,ALOAD).var!=batch||local(when,LLOAD).var!=cast||local(list,ALOAD).var!=(draw?wire:recipients))throw new AssertionError("invalid native line owned batch phase");
                    var next=after(call);if(draw){if(local(next,ASTORE).var!=batch||local(after(next),ALOAD).var!=wire||!(after(after(next)) instanceof MethodInsnNode v)||!v.name.equals("isEmpty"))throw new AssertionError("line wire batch moved");m.instructions.remove(next);}else if(local(next,ALOAD).var!=slot(m,"towerDamages")||!(after(next) instanceof MethodInsnNode v)||!v.name.equals("isEmpty"))throw new AssertionError("line commit batch moved");erase(m,caster,spell,context,when,list,call);
                }
                default -> throw new AssertionError("unknown line hook="+call.name);
            }
        }
        if(!counts.equals(new TreeMap<>(Map.ofEntries(Map.entry("primary",1),Map.entry("fallback",1),Map.entry("admit",1),Map.entry("capture",1),Map.entry("draw",1),Map.entry("commit",1),Map.entry("collectKind",4),Map.entry("addPrimary",4),Map.entry("primaryId",3),Map.entry("contains",4),Map.entry("allKinds",11)))))throw new AssertionError("line actor hooks drift="+counts);
        var slots=new HashSet<>(Set.of(ctx,batch));for(int i=0;i<2;i++){var n=m.instructions.getFirst();while(n.getOpcode()<0)n=n.getNext();var store=after(n);if(n.getOpcode()!=ACONST_NULL||!slots.remove(local(store,ASTORE).var))throw new AssertionError("invalid line method-local initialization");erase(m,n,store);}if(!slots.isEmpty())throw new AssertionError("line local escaped normalization");return m;
    }
}
