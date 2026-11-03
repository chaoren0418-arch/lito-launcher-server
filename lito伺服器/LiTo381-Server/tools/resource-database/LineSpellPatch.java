import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Pinned native line blocks remain intact; declared branches may continue across target kinds. */
final class LineSpellPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatLineSpells",C="L"+P+"ClientSession;",S="L"+P+"BattleSpellCatalog$DirectionalLineSpell;",PRIMARY=H+"$Primary",BATCH=P+"CombatCompanionSpells$Batch";
    static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);}
    static List<AbstractInsnNode> between(AbstractInsnNode start,AbstractInsnNode end){var result=new ArrayList<AbstractInsnNode>();for(var n=start;n!=end;n=n.getNext()){if(n==null)throw new IllegalStateException("line region escaped");result.add(n);}return result;}
    static int continueCases(MethodNode m,List<AbstractInsnNode> region,List<JumpInsnNode> guards,int expected,String label) {
        var index=new IdentityHashMap<AbstractInsnNode,Integer>();for(int i=0;i<region.size();i++)index.put(region.get(i),i);var candidates=new IdentityHashMap<LabelNode,Integer>();
        for(var n:region)if(n instanceof JumpInsnNode j&&j.getOpcode()==GOTO&&index.containsKey(j.label)&&index.get(j.label)>index.get(n)&&index.get(j.label)>index.get(guards.get(guards.size()-1)))candidates.merge(j.label,1,Integer::sum);
        var ends=candidates.entrySet().stream().filter(e->e.getValue()==expected).map(Map.Entry::getKey).toList();if(ends.size()!=1)throw new IllegalStateException("ambiguous line "+label+" exit: "+candidates.values());var end=ends.get(0);int changed=0;
        for(var guard:guards) {
            int lo=index.get(guard),hi=index.getOrDefault(guard.label,region.size());
            var exits=region.subList(lo,hi).stream().filter(n->n instanceof JumpInsnNode j&&j.getOpcode()==GOTO&&j.label==end).map(n->(JumpInsnNode)n).toList();
            if(exits.size()!=1)throw new IllegalStateException("missing line "+label+" branch exit "+lo+".."+hi+": "+exits.size());
            var prefix=new InsnList();prefix.add(call("allKinds","()Z"));prefix.add(new JumpInsnNode(IFNE,guard.label));m.instructions.insertBefore(exits.get(0),prefix);changed++;
        }
        CombatPatch.require(changed,expected,"line "+label+" cross-kind exits");return changed;
    }
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        var m=CombatPatch.method(c,"useLineAttackSpell","(L"+P+"Protocol$AttackSpellRequest;"+S+")V");
        int primary=DragonPatch.local(m,"primary"),tower=DragonPatch.local(m,"primaryTower"),monster=DragonPatch.local(m,"primaryMonster"),time=DragonPatch.local(m,"primaryTimeBoss"),bot=DragonPatch.local(m,"botTarget"),tx=DragonPatch.local(m,"targetX"),ty=DragonPatch.local(m,"targetY"),line=DragonPatch.local(m,"line"),wire=DragonPatch.local(m,"wireTargets"),countered=DragonPatch.local(m,"counteredTargets"),cast=DragonPatch.local(m,"castNow"),recipients=DragonPatch.local(m,"attackRecipients");
        Set<Integer> primarySlots=Set.of(primary,tower,monster,time),targetLists=Set.of(DragonPatch.local(m,"towerTargets"),DragonPatch.local(m,"monsterTargets"),DragonPatch.local(m,"timeBossTargets")),damageMaps=Set.of(DragonPatch.local(m,"towerDamages"),DragonPatch.local(m,"monsterDamages"),DragonPatch.local(m,"timeBossDamages"));
        AbstractInsnNode geometry=null,caster=null,wireStart=null,calculationEnd=null,commitStart=null,commitEnd=null;JumpInsnNode unknown=null;
        for(var n:m.instructions.toArray()) {
            if(n instanceof MethodInsnNode v&&v.owner.equals(P+"BattleStraightLine")&&v.name.equals("points"))geometry=DragonPatch.next(n);
            if(geometry==null&&n instanceof JumpInsnNode j&&j.getOpcode()==IFNULL&&DragonPatch.prior(n) instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&l.var==time&&unknown==null)unknown=j;
            if(geometry!=null&&caster==null&&n instanceof MethodInsnNode v&&v.owner.equals(c.name)&&v.name.equals("selectedCharacterSummary"))caster=DragonPatch.prior(n);
            if(n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==wire)wireStart=n;
            if(wireStart!=null&&calculationEnd==null&&n instanceof MethodInsnNode v&&v.owner.equals("java/util/List")&&v.name.equals("iterator")&&DragonPatch.prior(n) instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&l.var==countered)calculationEnd=DragonPatch.prior(n);
            if(calculationEnd!=null&&commitStart==null&&n instanceof MethodInsnNode v&&v.owner.equals("java/util/Map")&&v.name.equals("isEmpty")&&DragonPatch.prior(n) instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&damageMaps.contains(l.var))commitStart=DragonPatch.prior(n);
            if(commitStart!=null&&commitEnd==null&&n instanceof MethodInsnNode v&&v.owner.equals(P+"Protocol$AttackSpellRequest")&&v.name.equals("x"))commitEnd=DragonPatch.prior(n);
        }
        if(!(geometry instanceof VarInsnNode g)||g.getOpcode()!=ASTORE||g.var!=line||caster==null||wireStart==null||calculationEnd==null||commitStart==null||commitEnd==null||unknown==null)throw new IllegalStateException("native line boundaries changed");
        List<AbstractInsnNode> collection=between(geometry.getNext(),caster),calculation=between(wireStart.getNext(),calculationEnd),commit=between(commitStart,commitEnd);var guards=new ArrayList<JumpInsnNode>();int added=0,ids=0,contains=0;
        for(var n:collection) {
            if(n instanceof JumpInsnNode j&&j.getOpcode()==IFNULL&&DragonPatch.prior(n) instanceof VarInsnNode load&&load.getOpcode()==ALOAD&&primarySlots.contains(load.var))guards.add(j);
            if(n instanceof MethodInsnNode v&&v.owner.equals("java/util/List")&&v.name.equals("add")&&DragonPatch.prior(n) instanceof VarInsnNode load&&load.getOpcode()==ALOAD&&primarySlots.contains(load.var)){m.instructions.set(n,call("addPrimary","(Ljava/util/List;Ljava/lang/Object;)Z"));added++;}
            if(n instanceof MethodInsnNode v&&v.name.equals("objectId")&&v.desc.equals("()J")&&DragonPatch.prior(n) instanceof VarInsnNode load&&load.getOpcode()==ALOAD&&Set.of(tower,monster,time).contains(load.var)){m.instructions.set(n,call("primaryId","(Ljava/lang/Object;)J"));ids++;}
            if(n instanceof MethodInsnNode v&&v.owner.equals(P+"BattleStraightLine")&&v.name.equals("contains")) {
                var y=DragonPatch.prior(n);var actor=DragonPatch.prior(y);if(!(actor instanceof VarInsnNode load)||load.getOpcode()!=ALOAD)throw new IllegalStateException("line candidate geometry provenance changed");var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,load.var));m.instructions.insertBefore(n,args);m.instructions.set(n,call("contains","(Ljava/util/List;II"+C+"Ljava/lang/Object;)Z"));contains++;
            }
        }
        CombatPatch.require(guards.size(),4,"line collection kind guards");CombatPatch.require(added,4,"line null-safe primary inserts");CombatPatch.require(ids,3,"line null-safe primary identity comparisons");CombatPatch.require(contains,4,"line per-target source geometry checks");
        // Collection has three else exits; the fourth kind naturally reaches the common join.
        int exits=continueCases(m,collection,guards.subList(0,3),3,"collection");for(var guard:guards){m.instructions.insertBefore(guard,call("collectKind","(Ljava/lang/Object;)Z"));guard.setOpcode(IFEQ);}
        var calcGuards=new ArrayList<JumpInsnNode>();for(var n:calculation)if(n instanceof JumpInsnNode j&&(j.getOpcode()==IFEQ&&DragonPatch.prior(n) instanceof VarInsnNode botLoad&&botLoad.getOpcode()==ILOAD&&botLoad.var==bot
                ||j.getOpcode()==IFNE&&DragonPatch.prior(n) instanceof MethodInsnNode v&&v.owner.equals("java/util/List")&&v.name.equals("isEmpty")&&DragonPatch.prior(v) instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&targetLists.contains(l.var)))calcGuards.add(j);
        CombatPatch.require(calcGuards.size(),4,"line damage kind guards");exits+=continueCases(m,calculation,calcGuards,4,"calculation");
        var commitGuards=new ArrayList<JumpInsnNode>();for(var n:commit)if(n instanceof JumpInsnNode j&&(j.getOpcode()==IFEQ&&DragonPatch.prior(n) instanceof VarInsnNode botLoad&&botLoad.getOpcode()==ILOAD&&botLoad.var==bot
                ||j.getOpcode()==IFNE&&DragonPatch.prior(n) instanceof MethodInsnNode v&&v.owner.equals("java/util/Map")&&v.name.equals("isEmpty")&&DragonPatch.prior(v) instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&damageMaps.contains(l.var)))commitGuards.add(j);
        CombatPatch.require(commitGuards.size(),4,"line commit kind guards");exits+=continueCases(m,commit,commitGuards,4,"commit");
        int primarySlot=MagicDamagePatch.appendLocal(c,m,PRIMARY),batchSlot=MagicDamagePatch.appendLocal(c,m,BATCH);VarInsnNode primaryInit=null;for(var n:m.instructions)if(n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==primarySlot){primaryInit=v;break;}if(primaryInit==null)throw new IllegalStateException("missing line primary initialization");
        var init=new InsnList();init.add(new VarInsnNode(ALOAD,0));init.add(new VarInsnNode(ALOAD,1));init.add(call("primary","("+C+"L"+P+"Protocol$AttackSpellRequest;)L"+PRIMARY+";"));init.add(new VarInsnNode(ASTORE,primarySlot));m.instructions.insert(primaryInit,init);
        var oldExit=DragonPatch.prior(unknown.label);if(!(oldExit instanceof JumpInsnNode join)||join.getOpcode()!=GOTO)throw new IllegalStateException("line unknown target branch changed");
        var originalLog=new LabelNode();var fallback=new InsnList();fallback.add(new VarInsnNode(ALOAD,primarySlot));fallback.add(new JumpInsnNode(IFNULL,originalLog));fallback.add(new VarInsnNode(ALOAD,primarySlot));fallback.add(new MethodInsnNode(INVOKEVIRTUAL,PRIMARY,"x","()I",false));fallback.add(new VarInsnNode(ISTORE,tx));fallback.add(new VarInsnNode(ALOAD,primarySlot));fallback.add(new MethodInsnNode(INVOKEVIRTUAL,PRIMARY,"y","()I",false));fallback.add(new VarInsnNode(ISTORE,ty));fallback.add(new JumpInsnNode(GOTO,join.label));fallback.add(originalLog);fallback.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insertBefore(DragonPatch.next(unknown.label),fallback);
        var admitted=new LabelNode();var admit=new InsnList();admit.add(new VarInsnNode(ALOAD,0));admit.add(new VarInsnNode(ILOAD,tx));admit.add(new VarInsnNode(ILOAD,ty));admit.add(new VarInsnNode(ALOAD,primary));admit.add(call("admit","("+C+"II"+C+")Z"));admit.add(new JumpInsnNode(IFNE,admitted));admit.add(new InsnNode(RETURN));admit.add(admitted);admit.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insertBefore(DragonPatch.next(join.label),admit);
        var capture=new InsnList();capture.add(new VarInsnNode(ALOAD,0));capture.add(new VarInsnNode(ALOAD,2));capture.add(new VarInsnNode(ALOAD,primarySlot));capture.add(new VarInsnNode(ALOAD,line));capture.add(call("capture","("+C+S+"L"+PRIMARY+";Ljava/util/List;)L"+BATCH+";"));capture.add(new VarInsnNode(ASTORE,batchSlot));m.instructions.insert(geometry,capture);
        int draws=0;for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals("java/util/List")&&v.name.equals("isEmpty")&&DragonPatch.prior(n) instanceof VarInsnNode l&&l.getOpcode()==ALOAD&&l.var==wire){var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,2));args.add(new VarInsnNode(ALOAD,batchSlot));args.add(new VarInsnNode(LLOAD,cast));args.add(new VarInsnNode(ALOAD,wire));args.add(call("draw","("+C+S+"L"+BATCH+";JLjava/util/List;)L"+BATCH+";"));args.add(new VarInsnNode(ASTORE,batchSlot));m.instructions.insertBefore(l,args);draws++;}
        CombatPatch.require(draws,1,"line owned wire preparation");var finish=new InsnList();finish.add(new VarInsnNode(ALOAD,0));finish.add(new VarInsnNode(ALOAD,2));finish.add(new VarInsnNode(ALOAD,batchSlot));finish.add(new VarInsnNode(LLOAD,cast));finish.add(new VarInsnNode(ALOAD,recipients));finish.add(call("commit","("+C+S+"L"+BATCH+";JLjava/util/List;)V"));m.instructions.insertBefore(commitStart,finish);
        hooks.put("source-line-cross-kind-branches",exits);hooks.put("source-line-kind-adapters",guards.size()+added+ids+contains);hooks.put("source-line-controlled-ingress",6);
    }
}
