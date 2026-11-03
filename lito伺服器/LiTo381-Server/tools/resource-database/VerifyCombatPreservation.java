import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Compare bytecode semantics to the formal release, excluding debug/constant-pool layout. */
public class VerifyCombatPreservation {
    static final String P="local/lito381/server/";
    record ProbabilityCall(String owner,String name,String descriptor,boolean isInterface) {}
    static final Map<String,Set<String>> ALLOWED=Map.of(
        P+"ClientSession",Set.of("companionPhysicalDamage","useTeleportSpell"),
        P+"ResourceDatabase",Set.of("load","value"),
        P+"BattlePhysicalDefense",Set.of("againstPlayer"),
        P+"BattlePhysicalProcBuff",Set.of("burningSpirit","elementalFire","doubleBrake","soulOfFlameMultiplier"),
        P+"BattleStrikerGale",Set.of("damageAfterStatus"),
        P+"BattleAbilityStatus$Protection",Set.of("applyTo"),
        P+"BattleJoyOfPain",Set.of("feedbackDamage"),
        P+"BattleTurnUndead",Set.of("magicLevel","probability"),
        P+"BattleChainswordWeakness",Set.of("attack","consumeFoeSlayer","snapshot"));
    static ClassNode read(byte[] bytes) {
        var c=new ClassNode();new ClassReader(bytes).accept(c,0);return c;
    }
    static AbstractInsnNode prior(AbstractInsnNode node) {
        var p=node.getPrevious();while(p!=null&&p.getOpcode()<0)p=p.getPrevious();return p;
    }
    static AbstractInsnNode following(AbstractInsnNode node){var n=node.getNext();while(n!=null&&n.getOpcode()<0)n=n.getNext();return n;}
    static Map<String,ProbabilityCall> wrappers(ZipFile old,ClassNode candidate)throws Exception {
        candidate=VerifyPoisonLifecycle.prepare(read(old.getInputStream(old.getEntry(P+"ClientSession.class")).readAllBytes()),candidate);
        var result=new HashMap<String,ProbabilityCall>();
        var dragon=new HashMap<String,Integer>();
        for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatDragon"))dragon.merge(call.name,1,Integer::sum);
        if(!dragon.equals(Map.of("normalAttack",3,"consumeOnHit",3,"multiplier",3,"finishFoe",1,"weaknessAttack",3)))throw new AssertionError("dragon consumer count drift: "+dragon);
        var awake=new HashMap<String,Integer>();
        for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatAwaken"))awake.merge(call.name,1,Integer::sum);
        if(!awake.equals(Map.of("classicActive",11,"pollMpDrainTicks",1,"clearOnToggle",1,"ability",8,"sameCasterContext",1)))throw new AssertionError("awakening consumer count drift: "+awake);
        var buffs=new HashMap<String,Integer>();
        for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatBuffLeases"))buffs.merge(call.name,1,Integer::sum);
        if(!buffs.equals(Map.of("onDeath",2,"replacement",1,"autoArmorProtection",1,"autoStrengthSkillId",1,"autoDexteritySkillId",1)))throw new AssertionError("source BUFF actor hook drift: "+buffs);
        var weaponBuffs=new HashMap<String,Integer>();
        for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatWeaponBuffs"))weaponBuffs.merge(call.name,1,Integer::sum);
        if(!weaponBuffs.equals(Map.ofEntries(Map.entry("activateWeapon",2),Map.entry("activateItemWeapon",2),Map.entry("activateArmor",3),
                Map.entry("weaponModifier",3),Map.entry("autoWeaponModifier",1),Map.entry("itemWeaponModifier",3),Map.entry("itemArmorModifier",2),
                Map.entry("keepWeaponBuffs",2),Map.entry("keepItemBuff",1),Map.entry("removeWeaponSkill",1),Map.entry("removeArmorSkill",1),
                Map.entry("itemObjectId",3),Map.entry("itemLogName",3),Map.entry("bodyArmor",1),Map.entry("equippedWeapon",1),Map.entry("armor",1),Map.entry("legacyWeakness",8),Map.entry("diseaseArmor",1))))throw new AssertionError("source player weapon BUFF consumer drift: "+weaponBuffs);
        var classDamage=new HashMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatClassDamage"))classDamage.merge(call.name,1,Integer::sum);
        if(!classDamage.equals(Map.of("hit",3)))throw new AssertionError("source class damage consumer drift: "+classDamage);
        var kiringku=new HashMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatKiringku"))kiringku.merge(call.name,1,Integer::sum);
        if(!kiringku.equals(Map.of("player",2,"npc",2,"finish",1,"hit",3)))throw new AssertionError("source Kiringku consumer drift: "+kiringku);
        var counterDetection=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatCounterDetection"))counterDetection.merge(call.name,1,Integer::sum);if(!counterDetection.equals(new TreeMap<>(Map.of("prepare",2,"finish",2))))throw new AssertionError("native72 stage counts="+counterDetection);
        VerifyPoisonAtomic.prepare(read(old.getInputStream(old.getEntry(P+"ClientSession.class")).readAllBytes()),VerifyPoisonParalysis.session(candidate));
        var paralysis=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatPoisonParalysis"))paralysis.merge(call.name,1,Integer::sum);if(!paralysis.equals(new TreeMap<>(Map.of("activate",1,"poisonVisual",1,"curseVisual",2,"poll",1,"note",1,"paletteFirst",1,"release",5,"releaseAt",1,"cancelNpc",2))))throw new AssertionError("independent paralysis hooks="+paralysis);
        var atomicPoison=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatPoisonAtomic"))atomicPoison.merge(call.name,1,Integer::sum);if(!atomicPoison.equals(new TreeMap<>(Map.of("npc",1,"player",3,"poll",1,"attacker",12,"object",7,"kind",7))))throw new AssertionError("poison atomic field/producer/poll hooks="+atomicPoison);
        var incoming=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatIncomingSource"))incoming.merge(call.name,1,Integer::sum);if(!incoming.equals(new TreeMap<>(Map.of("battle",1,"worldBefore",1,"world",1))))throw new AssertionError("source incoming factor/order hooks="+incoming);
        var npcPoison=new TreeMap<String,Integer>();for(var method:VerifyPoisonParalysis.session(candidate).methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatNpcPoison"))npcPoison.merge(call.name,1,Integer::sum);if(!npcPoison.equals(new TreeMap<>(Map.ofEntries(Map.entry("legacyWorld",1),Map.entry("worldFlag",3),Map.entry("battleTick",1),Map.entry("cure",6),Map.entry("cureSilence",5),Map.entry("cancelParalysis",3),Map.entry("clearTimed",4),Map.entry("removeCurse",2),Map.entry("cancelMagic",3),Map.entry("cureNpcParalysis",1),Map.entry("silencePending",7),Map.entry("poisonPalette",2),Map.entry("cancellationPublish",3)))))throw new AssertionError("source NPC poison exact PC lifecycle/notice hooks="+npcPoison);
        var npcPlayer=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatNpcPlayerDamage"))npcPlayer.merge(call.name,1,Integer::sum);if(!npcPlayer.equals(new TreeMap<>(Map.of("prepare",2,"finish",2,"commit",2))))throw new AssertionError("owned NPC PC source stage count="+npcPlayer);
        var playerDamage=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatPlayerDamage"))playerDamage.merge(call.name,1,Integer::sum);if(!playerDamage.equals(new TreeMap<>(Map.of("battle",7,"battleTerminal",1,"world",9,"worldGate",8,"mindMultiplier",4,"dollGate",1,"shockWire",2))))throw new AssertionError("signed native PC HP boundaries="+playerDamage);
        var playerMirror=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatPlayerMirror"))playerMirror.merge(call.name,1,Integer::sum);if(!playerMirror.equals(new TreeMap<>(Map.of("decide",17,"receiveWorld",1,"receiveBattle",1))))throw new AssertionError("source PC reflection bridges="+playerMirror);
        var playerMagic=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatPlayerMagic"))playerMagic.merge(call.name,1,Integer::sum);if(!playerMagic.equals(new TreeMap<>(Map.of("reductions",17,"protect",21,"signedPreMr",1))))throw new AssertionError("PC scalar mandatory bridge counts="+playerMagic);
        var npcMagic=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatNpcMagic"))npcMagic.merge(call.name,1,Integer::sum);if(!npcMagic.equals(new TreeMap<>(Map.of("line",5,"area",16,"ordinaryComplete",6,"attempt",1,"signedPreMr",1))))throw new AssertionError("NPC magic mandatory scalar/clone hooks="+npcMagic);
        var companionDamage=new HashMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatCompanionDamage"))companionDamage.merge(call.name,1,Integer::sum);
        if(!companionDamage.equals(Map.of("ordinary",11,"finalBurn",2,"beforeCritical",2,"physical",1,"physicalAttempt",1,"area",4,"line",1,"ordinaryComplete",2)))throw new AssertionError("source companion damage consumers changed: "+companionDamage);
        var rangedDamage=new HashMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatRules")&&Set.of("strikerBeforeClass","strikerAfterClass").contains(call.name))rangedDamage.merge(call.name,1,Integer::sum);else if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatWorldCommit")&&call.name.equals("prepare"))rangedDamage.merge("strikerAfterClass",1,Integer::sum);
        if(!rangedDamage.equals(Map.of("strikerBeforeClass",3,"strikerAfterClass",3)))throw new AssertionError("source ranged commit consumer drift: "+rangedDamage);
        var companionSpells=new HashMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatCompanionSpells")){if(!(method.name.equals("useShockStunSpell")&&call.name.equals("shock")||method.name.equals("useCenteredFreezeAttackSpell")&&Set.of("captureCenteredFreeze","drawCenteredFreeze","commitCenteredFreeze").contains(call.name)))throw new AssertionError("unexpected companion spell hook");companionSpells.merge(call.name,1,Integer::sum);}if(!companionSpells.equals(Map.of("shock",1,"captureCenteredFreeze",1,"drawCenteredFreeze",1,"commitCenteredFreeze",1)))throw new AssertionError("controlled spell ingress drift="+companionSpells);
        var healing=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatHealing"))healing.merge(call.name,1,Integer::sum);if(!healing.equals(new TreeMap<>(Map.of("blessingPlayer",1,"groupPlayerBase",1,"owned",2))))throw new AssertionError("source group healing hooks="+healing);
        var npcDebuffs=new TreeMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatNpcDebuffs"))npcDebuffs.merge(call.name,1,Integer::sum);if(!npcDebuffs.equals(new TreeMap<>(Map.of("negative",1,"armorTarget",1,"strikerActive",2,"armorActive",2,"healingBattle",1,"healingWorld",1))))throw new AssertionError("NPC debuff ingress/consumer hooks="+npcDebuffs);
        var magicDamage=new HashMap<String,Integer>();for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatMagicDamage"))magicDamage.merge(call.name,1,Integer::sum);
        if(!magicDamage.equals(Map.ofEntries(Map.entry("ordinary",5),Map.entry("line",2),Map.entry("area",4),Map.entry("finish",17),Map.entry("direct",3),Map.entry("reduceMindBreak",2),Map.entry("cookingAfterMode",2),Map.entry("deferMr",2),Map.entry("attempt",1))))throw new AssertionError("source magic damage consumer drift: "+magicDamage);
        var specialBuffs=new HashMap<String,Integer>();
        for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatSpecialBuffs"))specialBuffs.merge(call.name,1,Integer::sum);
        if(!specialBuffs.equals(Map.ofEntries(Map.entry("legacy",9),Map.entry("add",8),Map.entry("element",3),Map.entry("evasion",1),Map.entry("weight",1),
                Map.entry("eyeRecipients",1),Map.entry("eyePhase",1),Map.entry("replace",3),Map.entry("addScalar",3),Map.entry("expire",1),Map.entry("blessingNpc",1),Map.entry("tryTalk",1))))throw new AssertionError("special buff consumer drift: "+specialBuffs);
        var special=new HashMap<String,Integer>();
        for(var method:candidate.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call
                &&call.owner.equals(P+"CombatProbability")&&Set.of("thunderSucceeds","npcThunderDuration","confusionChance","confusionSucceeds","counterDecision","mortalDecision","npcMortalDecision").contains(call.name))
            special.merge(call.name,1,Integer::sum);
        if(!special.equals(Map.of("thunderSucceeds",1,"npcThunderDuration",6,"confusionChance",2,"confusionSucceeds",2,"counterDecision",8,"mortalDecision",2,"npcMortalDecision",3)))
            throw new AssertionError("special probability consumer count drift: "+special);
        for(var method:candidate.methods)if(method.name.startsWith("combatProbability$")) {
            var arguments=Type.getArgumentTypes(method.desc);
            if(arguments.length<3||!arguments[0].getInternalName().startsWith(P+"BattleSpellCatalog$"))throw new AssertionError("bad probability receiver");
            var strings=new ArrayList<String>();int dispatch=0;
            for(var n:method.instructions) {
                if(n.getOpcode()<0)continue;
                if(n instanceof LdcInsnNode literal&&literal.cst instanceof String s)strings.add(s);
                if(n instanceof MethodInsnNode call) {
                    if(call.owner.equals(P+"CombatProbability")&&call.name.equals("dispatch"))dispatch++;
                    else if(!call.owner.startsWith("java/lang/")||!call.name.equals("valueOf"))throw new AssertionError("unexpected probability wrapper call");
                }
                if(!Set.of(Opcodes.ALOAD,Opcodes.ILOAD,Opcodes.LLOAD,Opcodes.FLOAD,Opcodes.DLOAD,
                        Opcodes.DUP,Opcodes.LDC,Opcodes.ANEWARRAY,Opcodes.AASTORE,Opcodes.INVOKESTATIC,Opcodes.IRETURN).contains(n.getOpcode()))
                    throw new AssertionError("unexpected probability wrapper operation");
            }
            if(strings.size()!=3||dispatch!=1)throw new AssertionError("probability wrapper contract drift");
            var oldArgs=Arrays.copyOfRange(arguments,1,arguments.length-2);
            String descriptor=Type.getMethodDescriptor(Type.INT_TYPE,oldArgs),owner=arguments[0].getInternalName();
            if(!strings.get(1).equals(descriptor)||strings.get(2).split(",").length!=oldArgs.length)throw new AssertionError("probability parameter mismatch");
            var entry=old.getEntry(owner+".class");if(entry==null)throw new AssertionError("unknown original probability owner");
            var original=read(old.getInputStream(entry).readAllBytes());
            if(original.methods.stream().noneMatch(m->m.name.equals(strings.get(0))&&m.desc.equals(descriptor)))throw new AssertionError("unknown original probability method");
            result.put(method.name+method.desc,new ProbabilityCall(owner,strings.get(0),descriptor,(original.access&Opcodes.ACC_INTERFACE)!=0));
        }
        if(result.size()!=59)throw new AssertionError("declared probability wrapper count drift: "+result.size());
        return result;
    }
    static MethodNode normalizeProbability(MethodNode source,Map<String,ProbabilityCall> wrappers) {
        var method=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(method);
        for(var n:method.instructions.toArray())if(n instanceof MethodInsnNode call) {
            if(call.owner.equals(P+"ClientSession")&&call.name.startsWith("combatProbability$")) {
                var specification=wrappers.get(call.name+call.desc);if(specification==null)throw new AssertionError("unknown probability wrapper");
                var array=prior(call);
                while(array!=null&&!(array instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.ANEWARRAY&&type.desc.equals("java/lang/Object")))array=prior(array);
                if(array==null||!(prior(array) instanceof LdcInsnNode count)||!(count.cst instanceof Integer size)||size<1||size>16)
                    throw new AssertionError("bad target reference array");
                var start=prior(count);if(!(start instanceof VarInsnNode caster)||caster.getOpcode()!=Opcodes.ALOAD||caster.var!=0)throw new AssertionError("bad probability caster context");
                var remove=new ArrayList<AbstractInsnNode>();remove.add(start);remove.add(count);remove.add(array);
                var cursor=array;
                for(int i=0;i<size;i++) {
                    cursor=cursor.getNext();if(cursor.getOpcode()!=Opcodes.DUP)throw new AssertionError("bad probability reference dup");remove.add(cursor);
                    cursor=cursor.getNext();if(!(cursor instanceof LdcInsnNode index)||!Integer.valueOf(i).equals(index.cst))throw new AssertionError("bad probability reference index");remove.add(cursor);
                    cursor=cursor.getNext();if(!(cursor instanceof VarInsnNode local)||cursor.getOpcode()!=Opcodes.ALOAD)throw new AssertionError("bad live probability target");remove.add(cursor);
                    cursor=cursor.getNext();if(cursor.getOpcode()!=Opcodes.AASTORE)throw new AssertionError("bad probability reference store");remove.add(cursor);
                }
                if(cursor.getNext()!=call)throw new AssertionError("unexplained probability context instructions");
                for(var extra:remove)method.instructions.remove(extra);
                method.instructions.set(call,new MethodInsnNode(specification.isInterface()?Opcodes.INVOKEINTERFACE:Opcodes.INVOKEVIRTUAL,
                        specification.owner(),specification.name(),specification.descriptor(),specification.isInterface()));
            } else if(call.owner.equals(P+"CombatProbability")&&call.name.equals("thunderSucceeds")) {
                if(!method.name.equals("applyPlayerThunderGrab"))throw new AssertionError("unexpected thunder success caller");
                var target=prior(call);var caster=prior(target);
                if(!(target instanceof VarInsnNode t)||t.getOpcode()!=Opcodes.ALOAD||t.var!=1
                        ||!(caster instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ALOAD||s.var!=0)
                    throw new AssertionError("bad thunder player context");
                method.instructions.remove(target);method.instructions.remove(caster);
                method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"BattleSpellCatalog$ThunderGrabSpell","succeeds","(IIIZI)Z",false));
            } else if(call.owner.equals(P+"CombatProbability")&&Set.of("npcThunderDuration","confusionChance","confusionSucceeds","counterDecision","mortalDecision","npcMortalDecision").contains(call.name)) {
                boolean confusion=call.name.startsWith("confusion"),succeeds=call.name.equals("confusionSucceeds"),counter=call.name.equals("counterDecision");
                boolean mortal=call.name.equals("mortalDecision"),npcMortal=call.name.equals("npcMortalDecision");
                var array=prior(call);
                while(array!=null&&!(array instanceof TypeInsnNode type&&type.getOpcode()==Opcodes.ANEWARRAY&&type.desc.equals("java/lang/Object")))array=prior(array);
                if(array==null||!(prior(array) instanceof LdcInsnNode count)||!(count.cst instanceof Integer size)||size<(counter||npcMortal?0:1)||size>16)
                    throw new AssertionError("bad NPC thunder reference array");
                var remove=new ArrayList<AbstractInsnNode>();remove.add(count);remove.add(array);var cursor=array;
                if(confusion||counter||mortal) {
                    if(confusion&&!Set.of("useAttackSpell","useClassicWorldAttackSpell").contains(method.name))throw new AssertionError("unexpected confusion caller");
                    var caster=prior(count);
                    if(!(caster instanceof VarInsnNode local)||local.getOpcode()!=Opcodes.ALOAD||local.var!=0)throw new AssertionError("bad confusion caster");
                    remove.add(caster);
                }
                for(int i=0;i<size;i++) {
                    cursor=cursor.getNext();if(cursor.getOpcode()!=Opcodes.DUP)throw new AssertionError("bad NPC thunder dup");remove.add(cursor);
                    cursor=cursor.getNext();if(!(cursor instanceof LdcInsnNode index)||!Integer.valueOf(i).equals(index.cst))throw new AssertionError("bad NPC thunder index");remove.add(cursor);
                    cursor=cursor.getNext();if(!(cursor instanceof VarInsnNode local)||cursor.getOpcode()!=Opcodes.ALOAD)throw new AssertionError("bad NPC thunder reference");remove.add(cursor);
                    cursor=cursor.getNext();if(cursor.getOpcode()!=Opcodes.AASTORE)throw new AssertionError("bad NPC thunder store");remove.add(cursor);
                }
                if(cursor.getNext()!=call)throw new AssertionError("unexplained NPC thunder context");
                for(var extra:remove)method.instructions.remove(extra);
                method.instructions.set(call,mortal
                        ?new MethodInsnNode(Opcodes.INVOKESTATIC,P+"BattleMortalBody","decide","(L"+P+"BattleSpellCatalog$MortalBodySpell;ZIIZIZIIII)L"+P+"BattleMortalBody$Decision;",false)
                        :npcMortal?new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"ClientSession","mortalBodyNpcDecision","(IZ)L"+P+"BattleMortalBody$Decision;",false)
                        :counter
                        ?new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"ClientSession","counterBarrierDecision","(IIZIJ)L"+P+"BattleCounterBarrier$Decision;",false)
                        :confusion
                        ?new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"BattleSpellCatalog$ConfusionSpell",succeeds?"succeeds":"chancePercent",succeeds?"(ZI)Z":"(Z)I",false)
                        :new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"ClientSession","rollNpcThunderGrabDuration",
                        "(L"+P+"BattleSpellCatalog$ThunderGrabSpell;IIZIIJLjava/lang/String;)I",false));
            } else if(call.owner.equals(P+"CombatProbability")&&call.name.equals("bossGate")) {
                var argument=prior(call);var original=prior(argument);
                if(!(argument instanceof VarInsnNode local)||local.getOpcode()!=Opcodes.ALOAD
                        ||!(original instanceof MethodInsnNode getter)||!getter.desc.equals("()Z")
                        ||!Set.of("boss","finalBoss").contains(getter.name))throw new AssertionError("unexplained boss gate transform");
                method.instructions.remove(argument);method.instructions.remove(call);
            }
        }
        return method;
    }
    static MethodNode normalizeDragon(MethodNode source) {
        var method=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(method);
        boolean cast=method.name.equals("useFoeSlayerSpell");int hp=-1,mp=-1;LabelNode accepted=null;
        if(cast)for(var local:method.localVariables) {
            if(local.name.equals("remainingHp"))hp=local.index;
            if(local.name.equals("remainingMp"))mp=local.index;
            if(local.name.equals("targetStillEligible"))accepted=local.start;
        }
        int hpReads=0,mpReads=0;
        for(var n:method.instructions.toArray()) {
            if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatDragon")) {
                var b=prior(call);var a=prior(b);
                if(call.name.equals("normalAttack")) {
                    if(!(b instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ALOAD||v.var!=0)throw new AssertionError("bad normal attack context");
                    method.instructions.remove(b);method.instructions.remove(call);
                } else if(call.name.equals("consumeOnHit")) {
                    var targetEnd=b;var targetStart=targetEnd;
                    if(targetEnd instanceof MethodInsnNode getter&&getter.desc.equals("()J")&&Set.of("objectId","playerObjectId").contains(getter.name)) {
                        targetStart=prior(targetEnd);
                        if(!(targetStart instanceof VarInsnNode ref)||ref.getOpcode()!=Opcodes.ALOAD)throw new AssertionError("bad Foe target receiver");
                    } else if(!(targetEnd instanceof VarInsnNode id)||id.getOpcode()!=Opcodes.LLOAD)throw new AssertionError("bad Foe target ID");
                    b=prior(targetStart);a=prior(b);
                    var strike=prior(a);var store=call.getNext();while(store!=null&&store.getOpcode()<0)store=store.getNext();
                    if(!(a instanceof VarInsnNode caster)||caster.getOpcode()!=Opcodes.ALOAD||caster.var!=0
                            ||!(strike instanceof VarInsnNode load)||load.getOpcode()!=Opcodes.ALOAD
                            ||!(store instanceof VarInsnNode write)||write.getOpcode()!=Opcodes.ASTORE||write.var!=load.var
                            ||!(b instanceof VarInsnNode hit)||hit.getOpcode()!=Opcodes.ILOAD)throw new AssertionError("bad Foe hit context");
                    if(targetStart!=targetEnd)method.instructions.remove(targetStart);method.instructions.remove(targetEnd);
                    method.instructions.remove(strike);method.instructions.remove(a);method.instructions.remove(b);method.instructions.remove(call);method.instructions.remove(store);
                } else if(call.name.equals("weaknessAttack")) {
                    if(!(b instanceof VarInsnNode caster)||caster.getOpcode()!=Opcodes.ALOAD||caster.var!=0)throw new AssertionError("bad weakness caster");
                    method.instructions.remove(b);
                    method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"BattleChainswordWeakness","attack","(IIJJI)L"+P+"BattleChainswordWeakness$AttackOutcome;",false));
                } else if(call.name.equals("multiplier")) {
                    if(!(b instanceof VarInsnNode strike)||strike.getOpcode()!=Opcodes.ALOAD)throw new AssertionError("bad Foe multiplier context");
                    method.instructions.remove(b);method.instructions.remove(call);
                } else if(call.name.equals("finishFoe")) {
                    var caster=prior(a);
                    if(!cast||!(caster instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ALOAD||s.var!=0
                            ||!(a instanceof VarInsnNode t)||t.getOpcode()!=Opcodes.ALOAD
                            ||!(b instanceof VarInsnNode eligible)||eligible.getOpcode()!=Opcodes.ILOAD)throw new AssertionError("bad Foe commit context");
                    method.instructions.remove(caster);method.instructions.remove(a);method.instructions.remove(b);method.instructions.remove(call);
                } else throw new AssertionError("unknown Dragon hook");
            } else if(cast&&n instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.GETFIELD&&field.owner.equals(P+"ClientSession")
                    &&Set.of("playerHp","playerMp").contains(field.name)&&accepted!=null&&method.instructions.indexOf(n)>method.instructions.indexOf(accepted)) {
                var caster=prior(n);
                if(!(caster instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ALOAD||v.var!=0)throw new AssertionError("bad current Foe vitals");
                boolean health=field.name.equals("playerHp");int slot=health?hp:mp;if(slot<0)throw new AssertionError("missing old Foe vitals slot");
                method.instructions.remove(caster);method.instructions.set(n,new VarInsnNode(Opcodes.ILOAD,slot));if(health)hpReads++;else mpReads++;
            } else if(cast&&n instanceof InvokeDynamicInsnNode concat)for(int i=0;i<concat.bsmArgs.length;i++)
                if(concat.bsmArgs[i] instanceof String text)concat.bsmArgs[i]=text.replace(" exposure-preconsumed="," exposure-consumed=");
        }
        if(cast&&(hpReads!=3||mpReads!=3))throw new AssertionError("current Foe vitals hook count drift");
        return method;
    }
    static MethodNode normalizeAwaken(MethodNode source) {
        var method=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(method);
        var active=Map.of("useTeleportSpell",5,"useSpellBeforeDragonTargets",1,"admitClassicWorldSpellFamily",1,
                "centeredAreaCasterStillAdmitted",1,"decayPotionCasterStillAdmitted",1,"counterDetectionCasterStillAdmitted",1,"battleGfx",1);
        var stats=Map.of("effectiveCharisma","CHA","baseStrength","STR","effectiveConstitution","CON",
                "baseDexterity","DEX","baseIntelligence","INT","effectiveWisdom","WIS");
        for(var n:method.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatAwaken")) {
            String owner=P+"BattleAwakenStatus$Snapshot",name=call.name,desc="()Z";
            if(name.equals("sameCasterContext")) {
                var spell=prior(call);var caster=prior(spell);var context=prior(caster);int slot=-1;
                if(method.localVariables!=null)for(var v:method.localVariables)if(v.name.equals("sameCasterContext"))slot=v.index;
                if(!method.name.equals("useAwakenSpell")||!(spell instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ALOAD||s.var!=2
                        ||!(caster instanceof VarInsnNode t)||t.getOpcode()!=Opcodes.ALOAD||t.var!=0
                        ||!(context instanceof VarInsnNode b)||b.getOpcode()!=Opcodes.ILOAD||b.var!=slot)throw new AssertionError("bad awakening exclusion context");
                method.instructions.remove(spell);method.instructions.remove(caster);method.instructions.remove(call);continue;
            } else if(name.equals("classicActive")) {
                if(!active.containsKey(method.name))throw new AssertionError("unknown awakening admission caller");name="active";
            } else if(name.equals("pollMpDrainTicks")) {
                if(!method.name.equals("serverAwakeningMpDrain"))throw new AssertionError("unknown awakening drain caller");
                owner=P+"BattleAwakenStatus";desc="(J)I";
            } else if(name.equals("clearOnToggle")) {
                var toggle=prior(call);var spell=prior(toggle);int slot=-1;
                if(method.localVariables!=null)for(var v:method.localVariables)if(v.name.equals("toggle"))slot=v.index;
                if(!method.name.equals("useAwakenSpell")||!(spell instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ALOAD||s.var!=2
                        ||!(toggle instanceof VarInsnNode t)||t.getOpcode()!=Opcodes.ALOAD||t.var!=slot)throw new AssertionError("bad awakening toggle context");
                method.instructions.remove(spell);method.instructions.remove(toggle);owner=P+"BattlePolymorphStatus";name="clear";desc="()L"+P+"BattlePolymorphCatalog$Form;";
            } else if(name.equals("ability")) {
                if(!(prior(call) instanceof LdcInsnNode type)||!(type.cst instanceof String stat))throw new AssertionError("bad awakening ability binding");
                if(stat.equals("HP")) {if(!method.name.equals("awakeningAdjustedMaximumHp"))throw new AssertionError("bad awakening HP caller");name="maximumHpBonus";}
                else if(stat.equals("AC")) {if(!method.name.equals("battleArmorClass"))throw new AssertionError("bad awakening AC caller");name="armorClassModifier";}
                else {if(!stat.equals(stats.get(method.name)))throw new AssertionError("bad awakening stat caller");name="allAbilityScoreBonus";}
                method.instructions.remove(type);desc="()I";
            } else throw new AssertionError("unknown awakening hook");
            method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,owner,name,desc,false));
        }
        return method;
    }
    static MethodNode normalizeBuff(MethodNode source,String owner) {
        String helper=P+"CombatBuffLeases",ability=P+"BattleAbilityStatus",field="sourceBuffLeases";
        var method=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(method);
        var getters=Set.of("shield","strength","dexterity","strengthSkillId","dexteritySkillId","armorProtection","royalAura");
        if(owner.equals(ability)&&getters.contains(method.name)) {
            var ops=Arrays.stream(method.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
            if(ops.size()<7||!(ops.get(0) instanceof MethodInsnNode load)||!load.owner.equals(helper)||!load.name.equals("loaded")
                    ||!(ops.get(1) instanceof JumpInsnNode jump)||jump.getOpcode()!=Opcodes.IFEQ
                    ||!(ops.get(2) instanceof VarInsnNode actor)||actor.getOpcode()!=Opcodes.ALOAD||actor.var!=0
                    ||!(ops.get(3) instanceof FieldInsnNode own)||own.getOpcode()!=Opcodes.GETFIELD||!own.owner.equals(ability)||!own.name.equals(field)
                    ||!(ops.get(4) instanceof VarInsnNode now)||now.getOpcode()!=Opcodes.LLOAD||now.var!=1
                    ||!(ops.get(5) instanceof MethodInsnNode call)||!call.owner.equals(helper)||!call.name.equals(method.name)||!call.desc.equals(method.desc)
                    ||!Set.of(Opcodes.ARETURN,Opcodes.IRETURN).contains(ops.get(6).getOpcode()))throw new AssertionError("bad source buff getter entry: "+method.name);
            var last=ops.get(6);var end=jump.label;var cursor=ops.get(0);
            while(cursor!=end) {var next=cursor.getNext();method.instructions.remove(cursor);cursor=next;}
            var next=end.getNext();method.instructions.remove(end);if(next instanceof FrameNode frame&&frame.type==Opcodes.F_SAME)method.instructions.remove(frame);
        }
        for(var n:method.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)) {
            if(call.name.equals("validArmDuration")) {
                var next=call.getNext();while(next!=null&&next.getOpcode()<0)next=next.getNext();
                if(!owner.equals(P+"BattleArmBreakerStatus")||!method.name.equals("activateIfAbsent")
                        ||!(next instanceof JumpInsnNode jump)||jump.getOpcode()!=Opcodes.IFNE)throw new AssertionError("bad Arm Breaker duration binding");
                method.instructions.set(call,new IntInsnNode(Opcodes.BIPUSH,12));jump.setOpcode(Opcodes.IF_ICMPEQ);
            } else if(call.name.equals("onDeath")) {
                if(!Set.of("takePlayerDamage","takeClassicWorldDirectDamage").contains(method.name)
                        ||!(prior(call) instanceof VarInsnNode actor)||actor.getOpcode()!=Opcodes.ALOAD||actor.var!=0
                        ||!(prior(actor) instanceof MethodInsnNode clear)||!clear.owner.equals(P+"BattleAwakenStatus")||!clear.name.equals("clear"))throw new AssertionError("bad source BUFF death boundary");
                method.instructions.remove(actor);method.instructions.remove(call);
            } else if(call.name.equals("replacement")) {
                if(!method.name.equals("useShieldSpell")||!(prior(call) instanceof MethodInsnNode old)||!old.owner.equals(ability)||!old.name.equals("armorProtection"))throw new AssertionError("bad armor replacement icon hook");
                method.instructions.remove(call);
            } else if(call.name.startsWith("auto")) {
                if(!method.name.equals("maintainNativeMagicDollAutoSkills"))throw new AssertionError("bad source auto-skill caller");
                String old=switch(call.name){case "autoArmorProtection"->"armorProtection";case "autoStrengthSkillId"->"strengthSkillId";case "autoDexteritySkillId"->"dexteritySkillId";default->throw new AssertionError("unknown auto-skill hook");};
                int split=call.desc.indexOf(';');String desc="("+call.desc.substring(split+1);
                method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,ability,old,desc,false));
            } else if(call.name.equals("value")) {
                var type=prior(call);var id=prior(type);var read=prior(id);
                boolean fieldRead=read instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETFIELD&&f.owner.equals(owner)&&f.desc.equals("I");
                boolean alias=owner.equals(P+"BattleArmBreakerStatus")&&method.name.equals("meleeDamageModifier")&&read instanceof IntInsnNode c&&c.getOpcode()==Opcodes.BIPUSH&&c.operand==-2
                        &&id instanceof LdcInsnNode skill&&Integer.valueOf(213).equals(skill.cst)&&type instanceof LdcInsnNode stat&&"DMG".equals(stat.cst);
                if(!call.desc.equals("(IILjava/lang/String;)I")||!(type instanceof LdcInsnNode t)||!(t.cst instanceof String)
                        ||!(id instanceof LdcInsnNode i)||!(i.cst instanceof Integer)
                        ||!fieldRead&&!alias)throw new AssertionError("bad source buff value transform");
                method.instructions.remove(type);method.instructions.remove(id);method.instructions.remove(call);
            } else if(call.name.equals("<init>")) {
                var dup=prior(call);var create=prior(dup);var actor=prior(create);var write=call.getNext();
                if(!owner.equals(ability)||!method.name.equals("<init>")||dup.getOpcode()!=Opcodes.DUP
                        ||!(create instanceof TypeInsnNode c)||c.getOpcode()!=Opcodes.NEW||!c.desc.equals(helper)
                        ||!(actor instanceof VarInsnNode a)||a.getOpcode()!=Opcodes.ALOAD||a.var!=0
                        ||!(write instanceof FieldInsnNode f)||f.getOpcode()!=Opcodes.PUTFIELD||!f.owner.equals(ability)||!f.name.equals(field))throw new AssertionError("bad source buff state construction");
                for(var extra:List.of(actor,create,dup,call,write))method.instructions.remove(extra);
            } else {
                var remove=new ArrayList<AbstractInsnNode>();remove.add(call);var cursor=prior(call);
                if(call.name.equals("activate")||call.name.equals("replace")) {
                    if(!Set.of("activateArmorProtection","activateStrength","activateDexterity","activateRoyalAura").contains(method.name))throw new AssertionError("unknown source buff activation");
                    for(int[] expected:new int[][]{{Opcodes.ILOAD,4},{Opcodes.LLOAD,2},{Opcodes.ILOAD,1}}) {
                        if(!(cursor instanceof VarInsnNode v)||v.getOpcode()!=expected[0]||v.var!=expected[1])throw new AssertionError("bad source buff activation operands");remove.add(cursor);cursor=prior(cursor);
                    }
                    if(call.name.equals("replace")){if(cursor.getOpcode()!=Opcodes.SWAP)throw new AssertionError("bad source buff replacement stack");remove.add(cursor);cursor=prior(cursor);}
                } else if(call.name.equals("remove")) {
                    if(!method.name.equals("removeSkill")||!(cursor instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ILOAD||v.var!=1)throw new AssertionError("bad source buff removal");
                    remove.add(cursor);cursor=prior(cursor);if(cursor.getOpcode()!=Opcodes.SWAP)throw new AssertionError("bad source buff removal stack");remove.add(cursor);cursor=prior(cursor);
                } else if(!(call.name.equals("cancel")&&method.name.equals("clearCancelable")||call.name.equals("clear")&&method.name.equals("clear")))throw new AssertionError("unknown source buff hook="+call.name);
                if(!(cursor instanceof FieldInsnNode f)||f.getOpcode()!=Opcodes.GETFIELD||!f.owner.equals(ability)||!f.name.equals(field)
                        ||!(prior(cursor) instanceof VarInsnNode a)||a.getOpcode()!=Opcodes.ALOAD||a.var!=0)throw new AssertionError("bad source buff state receiver");
                remove.add(cursor);remove.add(prior(cursor));for(var extra:remove)method.instructions.remove(extra);
            }
        }
        return method;
    }
    static MethodNode normalizeWeaponBuff(MethodNode source) {
        var method=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(method);
        String helper=P+"CombatWeaponBuffs",w=P+"BattleWeaponBuffStatus",i=P+"BattleItemWeaponEnchantStatus",a=P+"BattleItemArmorEnchantStatus";
        var specs=Map.ofEntries(Map.entry("activateWeapon",w+":activate"),Map.entry("activateItemWeapon",i+":activate"),Map.entry("activateArmor",a+":activate"),
                Map.entry("weaponModifier",w+":modifier"),Map.entry("autoWeaponModifier",w+":modifier"),Map.entry("itemWeaponModifier",i+":modifier"),Map.entry("itemArmorModifier",a+":modifier"),
                Map.entry("keepWeaponBuffs",w+":cancelMagicEffects"),Map.entry("keepItemBuff",i+":remove"),Map.entry("removeWeaponSkill",w+":removeSkill"),Map.entry("removeArmorSkill",a+":removeSkill"),
                Map.entry("itemObjectId",P+"RuntimeInventory$Item:objectId"),Map.entry("itemLogName",P+"RuntimeInventory$Item:nativeLogName"),Map.entry("bodyArmor",P+"RuntimeInventory:item"));
        var noCaster=Set.of("keepWeaponBuffs","keepItemBuff","itemObjectId");
        for(var n:method.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)) {
            if(Set.of("legacyWeakness","diseaseArmor").contains(call.name)) {
                if(!(prior(call) instanceof MethodInsnNode old)||!old.owner.equals(P+"BattleSpellCatalog$WeaknessSpell")
                        ||call.name.equals("legacyWeakness")&&(!Set.of("attack","attackClassicWorldPlayer").contains(method.name)||!Set.of("meleeDamageModifier","meleeHitModifier").contains(old.name))
                        ||call.name.equals("diseaseArmor")&&(!method.name.equals("battleArmorClass")||!old.name.equals("armorClassModifier")))throw new AssertionError("bad SQL Weakness/Disease consumer");
                method.instructions.remove(call);continue;
            }
            if(call.name.equals("equippedWeapon")) {
                if(!method.name.equals("useItemWeaponEnchantSpell")||!(prior(call) instanceof VarInsnNode actor)||actor.getOpcode()!=Opcodes.ALOAD||actor.var!=0)throw new AssertionError("bad equipped weapon admission context");
                method.instructions.remove(actor);method.instructions.remove(call);continue;
            }
            if(call.name.equals("armor")) {
                var now=prior(call);var actor=prior(now);
                if(!method.name.equals("activeItemArmorEnchantModifier")||!(now instanceof VarInsnNode time)||time.getOpcode()!=Opcodes.LLOAD||time.var!=1
                        ||!(actor instanceof VarInsnNode player)||player.getOpcode()!=Opcodes.ALOAD||player.var!=0)throw new AssertionError("bad player armor aggregate context");
                method.instructions.remove(now);method.instructions.remove(actor);method.instructions.remove(call);continue;
            }
            String spec=specs.get(call.name);if(spec==null)throw new AssertionError("unknown player weapon BUFF hook="+call.name);
            var args=Type.getArgumentTypes(call.desc);int end=args.length;
            if(!noCaster.contains(call.name)) {
                int slot=0;if(call.name.equals("activateWeapon")&&method.name.equals("useWeaponBuffSpell")) {
                    slot=-1;for(var v:method.localVariables)if(v.name.equals("playerTarget"))slot=v.index;
                }
                if(!(prior(call) instanceof VarInsnNode actor)||actor.getOpcode()!=Opcodes.ALOAD||actor.var!=slot
                        ||!args[end-1].getDescriptor().equals("L"+P+"ClientSession;"))throw new AssertionError("bad player weapon BUFF owner context");
                method.instructions.remove(actor);end--;
            }
            String owner=spec.substring(0,spec.indexOf(':')),name=spec.substring(spec.indexOf(':')+1);
            if(!args[0].getDescriptor().equals("L"+owner+";"))throw new AssertionError("bad player weapon BUFF legacy receiver");
            String desc=Type.getMethodDescriptor(Type.getReturnType(call.desc),Arrays.copyOfRange(args,1,end));
            method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,owner,name,desc,false));
        }
        return method;
    }
    static MethodNode normalizeSpecialBuff(MethodNode source,String owner) {
        var method=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(method);
        if(owner.equals(P+"ClientSession")&&method.name.equals("takeClassicWorldDirectDamage")) {
            int sites=0;
            for(var n:method.instructions.toArray())if(n.getOpcode()==Opcodes.POP2&&n.getNext()!=null&&n.getNext().getOpcode()==Opcodes.POP
                    &&prior(n) instanceof VarInsnNode time&&time.getOpcode()==Opcodes.LLOAD&&time.var==3
                    &&prior(time) instanceof VarInsnNode actor&&actor.getOpcode()==Opcodes.ALOAD&&actor.var==0) {
                method.instructions.remove(n.getNext());method.instructions.set(n,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"ClientSession","applyClassicWorldDeathExperiencePenalty","(J)V",false));sites++;
            }
            if(sites!=1)throw new AssertionError("world no-penalty boundary drift="+sites);
        }
        for(var n:method.instructions.toArray()) {
            if(owner.equals(P+"ClientSession")&&method.name.equals("applyClassicWorldWaterDragonSaellBlessing")&&n instanceof IntInsnNode value&&value.operand==4016)value.operand=4018;
            if(!(n instanceof MethodInsnNode call)||!call.owner.equals(P+"CombatSpecialBuffs"))continue;
            if(call.name.equals("sourceMap")) {
                var load=prior(call);var dup=following(call);var jump=following(dup);var ret=following(jump);var pop=following(ret);
                if(!owner.equals(P+"ClassicWorldMapCatalog")||!method.name.equals("forMap")||!(load instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ILOAD||v.var!=0
                        ||dup.getOpcode()!=Opcodes.DUP||jump.getOpcode()!=Opcodes.IFNULL||ret.getOpcode()!=Opcodes.ARETURN||pop.getOpcode()!=Opcodes.POP)throw new AssertionError("invalid source map boundary");
                for(var extra:List.of(load,call,dup,jump,ret,pop))method.instructions.remove(extra);continue;
            }
            if(Set.of("npcs","mapSupported","mapIds").contains(call.name)) {
                if(!Set.of(P+"ClassicWorldMapCatalog",P+"ClassicWorldNpcCatalog").contains(owner))throw new AssertionError("bad source world metadata owner");
                if(!call.name.equals("mapIds")) {
                    var load=prior(call);if(!(load instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ILOAD||v.var!=0)throw new AssertionError("bad source metadata map context");method.instructions.remove(load);
                }
                method.instructions.remove(call);continue;
            }
            if(call.name.equals("blessingNpc")) {
                var load=prior(call);if(!owner.equals(P+"ClientSession")||!method.name.equals("openClassicWorldHypertext")||!(load instanceof VarInsnNode v)||v.getOpcode()!=Opcodes.ALOAD)throw new AssertionError("bad blessing NPC classifier");
                method.instructions.remove(load);method.instructions.remove(call);continue;
            }
            if(call.name.equals("tryTalk")) {
                var npc=prior(call);var actor=prior(npc);var jump=following(call);var ret=following(jump);
                if(!owner.equals(P+"ClientSession")||!method.name.equals("applyClassicWorldWaterDragonSaellBlessing")||!(npc instanceof VarInsnNode nr)||nr.getOpcode()!=Opcodes.ALOAD||nr.var!=1
                        ||!(actor instanceof VarInsnNode ar)||ar.getOpcode()!=Opcodes.ALOAD||ar.var!=0||jump.getOpcode()!=Opcodes.IFEQ||ret.getOpcode()!=Opcodes.RETURN)throw new AssertionError("bad source blessing talk boundary");
                for(var extra:List.of(npc,actor,call,jump,ret))method.instructions.remove(extra);continue;
            }
            if(Set.of("eyeField","eyeText").contains(call.name)) {
                var literal=prior(call);var actor=prior(literal);
                if(!owner.equals(P+"BattleDragonEyeCatalog$Eye")||!(literal instanceof LdcInsnNode text)||!method.name.equals(text.cst)
                        ||!(actor instanceof VarInsnNode ref)||ref.getOpcode()!=Opcodes.ALOAD||ref.var!=0)throw new AssertionError("bad eye getter projection");
                method.instructions.remove(literal);method.instructions.remove(actor);method.instructions.remove(call);continue;
            }
            if(!owner.equals(P+"ClientSession"))throw new AssertionError("special buff on unexpected owner");
            if(call.name.equals("legacy")){if(!call.desc.equals("(I)I"))throw new AssertionError("bad legacy buff signature");method.instructions.remove(call);continue;}
            if(call.name.equals("eyePhase")) {
                var actor=prior(call);var battle=prior(actor);var cast=call.getNext();
                if(!method.name.equals("useDragonEye")||!(actor instanceof VarInsnNode ref)||ref.var!=0||ref.getOpcode()!=Opcodes.ALOAD
                        ||!(battle instanceof FieldInsnNode field)||field.getOpcode()!=Opcodes.GETSTATIC||!field.name.equals("BATTLE")||!field.owner.equals(P+"ClientSession$Phase")
                        ||!(cast instanceof TypeInsnNode type)||type.getOpcode()!=Opcodes.CHECKCAST||!type.desc.equals(P+"ClientSession$Phase"))throw new AssertionError("bad eye world phase projection");
                method.instructions.remove(actor);method.instructions.remove(battle);method.instructions.remove(cast);method.instructions.remove(call);continue;
            }
            int extras=switch(call.name){case "add"->2;case "replace","addScalar","element"->3;case "evasion","weight","expire"->2;case "eyeRecipients"->1;default->throw new AssertionError("unknown special buff projection="+call.name);};
            var removed=new ArrayList<AbstractInsnNode>();var cursor=prior(call);
            for(int i=0;i<extras;i++){removed.add(cursor);cursor=prior(cursor);}
            var actor=removed.get(removed.size()-1);
            if(!(actor instanceof VarInsnNode ref)||ref.getOpcode()!=Opcodes.ALOAD||ref.var!=0)throw new AssertionError("bad special buff actor context");
            if(Set.of("add","replace","addScalar").contains(call.name)&&!(removed.get(0) instanceof LdcInsnNode text&&text.cst instanceof String))throw new AssertionError("bad special buff ability literal");
            for(var extra:removed)method.instructions.remove(extra);
            if(call.name.equals("eyeRecipients"))method.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"GameHub","roomMembers","(J)Ljava/util/List;",false));
            else method.instructions.remove(call);
        }
        return method;
    }
    static MethodNode normalizeMagicDamage(MethodNode source) {
        String helper=P+"CombatMagicDamage",attempt=helper+"$Attempt",actor=P+"ClientSession";int slot=-1;
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)) {
            if(Set.of("ordinary","line","area","finalBurn").contains(call.name)) {
                var target=prior(call);var dup=following(call);var store=following(dup);var getter=following(store);
                if(!(target instanceof VarInsnNode t)||t.getOpcode()!=ALOAD||dup.getOpcode()!=DUP||!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE
                        ||!(getter instanceof MethodInsnNode g)||!g.owner.equals(attempt)||!g.name.equals(call.name.equals("finalBurn")?"finalBurn":"damage"))throw new AssertionError("invalid source magic preparation");
                if(slot!=-1&&slot!=s.var)throw new AssertionError("multiple source magic attempt locals");slot=s.var;
                var types=Type.getArgumentTypes(call.desc);if(!types[types.length-1].getInternalName().equals(actor))throw new AssertionError("missing source magic live target");
                boolean burn=call.name.equals("finalBurn");var oldTypes=Arrays.copyOfRange(types,burn?0:1,types.length-1);
                String name=switch(call.name){case "ordinary"->"ordinaryMagicDamage";case "line"->"lineSpellDamage";case "area"->"areaSpellDamage";default->"resolve";};
                m.instructions.set(call,new MethodInsnNode(burn?INVOKESTATIC:INVOKEVIRTUAL,burn?P+"BattleFinalBurn":actor,name,Type.getMethodDescriptor(burn?Type.getObjectType(P+"BattleFinalBurn$Result"):Type.INT_TYPE,oldTypes),false));
                for(var extra:List.of(target,dup,store,getter))m.instructions.remove(extra);
            } else if(call.name.equals("beforeCritical")) {
                var target=prior(call);if(!(target instanceof VarInsnNode t)||t.getOpcode()!=ALOAD)throw new AssertionError("invalid deferred critical target");m.instructions.remove(target);
                m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$AttackSpell","damageAfterSpecialCritical","(IZ)I",false));
            } else if(call.name.equals("finish")) {
                var crit=prior(call);var caster=prior(crit);var context=prior(caster);var damage=prior(context);var store=following(call);var mirror=prior(damage);
                if(!(context instanceof VarInsnNode a)||a.getOpcode()!=ALOAD||a.var!=slot||!(caster instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||c.var!=0
                        ||crit.getOpcode()!=ICONST_0&&crit.getOpcode()!=ILOAD||!(damage instanceof VarInsnNode d)||d.getOpcode()!=ILOAD||!(store instanceof VarInsnNode s)||s.getOpcode()!=ISTORE||s.var!=d.var
                        ||!(mirror instanceof MethodInsnNode mi)||!mi.owner.equals(actor)||!mi.name.equals("counterMirrorDecision"))throw new AssertionError("invalid post-mirror source magic boundary");
                for(var extra:List.of(crit,caster,context,damage,call,store))m.instructions.remove(extra);
            } else if(call.name.equals("reduceMindBreak")) {
                var target=prior(call);var damage=prior(target);var dup=following(call);var store=following(dup);
                if(!(target instanceof VarInsnNode t)||t.getOpcode()!=ALOAD||!(damage instanceof VarInsnNode d)||d.getOpcode()!=ILOAD||dup.getOpcode()!=DUP||!(store instanceof VarInsnNode s)||s.getOpcode()!=ISTORE||s.var!=d.var)throw new AssertionError("invalid Mind Break reduction boundary");
                for(var extra:List.of(target,call,dup,store))m.instructions.remove(extra);
            } else if(call.name.equals("cookingAfterMode")) {
                var mind=prior(call);if(!(mind instanceof VarInsnNode v)||v.getOpcode()!=ALOAD)throw new AssertionError("invalid Mind Break reduction skip");m.instructions.remove(mind);
                m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,actor,"combatDamageAfterCooking","(ILjava/lang/String;)I",false));
            } else if(call.name.equals("direct")) {
                var cooking=prior(call);var target=prior(cooking);var caster=prior(target);var context=prior(caster);
                if(!(context instanceof VarInsnNode a)||a.getOpcode()!=ALOAD||a.var!=slot||!(caster instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||c.var!=0
                        ||!(target instanceof VarInsnNode t)||t.getOpcode()!=ALOAD||!Set.of("useShockStunSpell","useClassicWorldShockStunSpell","nativeMagicDollTriggeredSkillDamage").contains(m.name)
                        ||cooking.getOpcode()!=(m.name.equals("nativeMagicDollTriggeredSkillDamage")?ICONST_1:ICONST_0))throw new AssertionError("invalid shock/doll source magic boundary");
                for(var extra:List.of(cooking,target,caster,context,call))m.instructions.remove(extra);
            } else throw new AssertionError("unrecognized source magic boundary: "+call.name);
        }
        if(slot>=0) {var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var store=following(first);
            if(first.getOpcode()!=ACONST_NULL||!(store instanceof VarInsnNode v)||v.getOpcode()!=ASTORE||v.var!=slot)throw new AssertionError("invalid source magic attempt initialization");m.instructions.remove(first);m.instructions.remove(store);}
        return m;
    }
    static void verifyMagicClone(MethodNode original,MethodNode clone) {
        String helper=P+"CombatMagicDamage";var m=new MethodNode(clone.access,clone.name,clone.desc,clone.signature,clone.exceptions.toArray(String[]::new));clone.accept(m);m.name=original.name;m.desc=original.desc;int calls=0,returns=0,signed=0;
        for(var node:m.instructions.toArray()) {
            if(node instanceof MethodInsnNode call&&call.owner.equals(helper)&&call.name.equals("deferMr")){m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"BattleMagicResistance","damageAfterResistance","(III)I",false));calls++;}
            if(node instanceof MethodInsnNode call&&call.owner.equals(P+"CombatPlayerMagic")&&call.name.equals("signedPreMr")){if(!call.desc.equals("(II)I")||prior(call).getOpcode()!=ICONST_0||prior(prior(call)).getOpcode()!=ISUB)throw new AssertionError("PC signed armor scalar boundary");m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,"java/lang/Math","max","(II)I",false));signed++;}
            if(node.getOpcode()==ARETURN) {
                var factory=prior(node);var hit=prior(factory);var caster=prior(hit);var mr=prior(caster);
                if(!(factory instanceof MethodInsnNode f)||!f.owner.equals(helper)||!f.name.equals("attempt")||!(hit instanceof MethodInsnNode h)||!h.owner.equals(P+"ClientSession")||!h.name.equals("originalMagicHit")
                        ||!(caster instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||c.var!=0||!(mr instanceof VarInsnNode r)||r.getOpcode()!=ILOAD||r.var!=5)throw new AssertionError("invalid source magic clone return");
                for(var extra:List.of(factory,hit,caster,mr))m.instructions.remove(extra);m.instructions.set(node,new InsnNode(IRETURN));returns++;
            }
        }
        if(calls!=1||returns!=1||signed!=1||!semantic(original).equals(semantic(m)))throw new AssertionError("source magic pre-MR clone changed unrelated instructions");
    }
    static MethodNode normalizeCompanionSpell(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);int count=0,slot=-1,draws=0,commits=0,time=-1;
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatCompanionSpells")) {
            if(!call.name.equals("shock")) {
                if(!m.name.equals("useCenteredFreezeAttackSpell"))throw new AssertionError("unexpected centered companion batch owner");var store=following(call);
                if(call.name.equals("captureCenteredFreeze")) {
                    var when=prior(call);var spell=prior(when);var caster=prior(spell);var original=prior(caster);
                    if(!call.desc.equals("(L"+P+"ClientSession;L"+P+"BattleSpellCatalog$FreezeAttackSpell;J)L"+P+"CombatCompanionSpells$Batch;")||!(when instanceof VarInsnNode w)||w.getOpcode()!=LLOAD||!(original instanceof VarInsnNode o)||o.getOpcode()!=LSTORE||o.var!=w.var
                            ||!(spell instanceof VarInsnNode s)||s.getOpcode()!=ALOAD||s.var!=2||!(caster instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||c.var!=0||!(store instanceof VarInsnNode b)||b.getOpcode()!=ASTORE)throw new AssertionError("invalid controlled freeze capture");slot=b.var;for(var extra:List.of(caster,spell,when,call,store))m.instructions.remove(extra);count++;continue;
                }
                var list=prior(call);var when=prior(list);var batch=prior(when);var spell=prior(batch);var caster=prior(spell);
                if(!(list instanceof VarInsnNode l)||l.getOpcode()!=ALOAD||!(when instanceof VarInsnNode w)||w.getOpcode()!=LLOAD||!(batch instanceof VarInsnNode b)||b.getOpcode()!=ALOAD||b.var!=slot||!(spell instanceof VarInsnNode s)||s.getOpcode()!=ALOAD||s.var!=2||!(caster instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||c.var!=0)throw new AssertionError("invalid controlled freeze batch arguments");
                String prefix="(L"+P+"ClientSession;L"+P+"BattleSpellCatalog$FreezeAttackSpell;L"+P+"CombatCompanionSpells$Batch;JLjava/util/List;)";
                if(call.name.equals("drawCenteredFreeze")) {
                    var wire=following(store);var empty=following(wire);
                    if(!call.desc.equals(prefix+"L"+P+"CombatCompanionSpells$Batch;")||!(store instanceof VarInsnNode a)||a.getOpcode()!=ASTORE||a.var!=slot||!(wire instanceof VarInsnNode v)||v.getOpcode()!=ALOAD||v.var!=l.var||!(empty instanceof MethodInsnNode e)||!e.owner.equals("java/util/List")||!e.name.equals("isEmpty"))throw new AssertionError("invalid controlled freeze wire merge");time=w.var;draws++;
                } else if(call.name.equals("commitCenteredFreeze")) {
                    var original=prior(caster);
                    if(!call.desc.equals(prefix+"I")||w.var!=time||!(store instanceof VarInsnNode a)||a.getOpcode()!=ISTORE||!(original instanceof VarInsnNode o)||o.getOpcode()!=ISTORE||o.var!=a.var||prior(original).getOpcode()!=ICONST_0)throw new AssertionError("invalid controlled freeze commit phase");commits++;
                } else throw new AssertionError("unexpected companion freeze stage");
                for(var extra:List.of(caster,spell,batch,when,list,call,store))m.instructions.remove(extra);continue;
            }
            var spell=prior(call);var payload=prior(spell);var caster=prior(payload);var branch=following(call);var ret=following(branch);
            if(!m.name.equals("useShockStunSpell")||!call.name.equals("shock")||!call.desc.equals("(L"+P+"ClientSession;[BL"+P+"BattleSpellCatalog$ShockStunSpell;)Z")
                    ||!(caster instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||c.var!=0||!(payload instanceof VarInsnNode p)||p.getOpcode()!=ALOAD||p.var!=1||!(spell instanceof VarInsnNode s)||s.getOpcode()!=ALOAD||s.var!=2
                    ||!(branch instanceof JumpInsnNode jump)||jump.getOpcode()!=IFEQ||ret.getOpcode()!=RETURN||ret.getNext()!=jump.label)throw new AssertionError("invalid controlled Shock Stun prefix");
            for(var extra:List.of(caster,payload,spell,call,branch,ret))m.instructions.remove(extra);count++;
        }
        if(count>1)throw new AssertionError("duplicate controlled spell prefix");
        if(slot>=0){var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var init=following(first);if(draws!=1||commits!=1||first.getOpcode()!=ACONST_NULL||!(init instanceof VarInsnNode i)||i.getOpcode()!=ASTORE||i.var!=slot)throw new AssertionError("invalid controlled freeze batch initialization");m.instructions.remove(first);m.instructions.remove(init);}return m;
    }
    static MethodNode normalizeRangedDamage(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);int slot=-1,before=0,after=0;
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatRules")&&Set.of("strikerBeforeClass","strikerAfterClass").contains(call.name)) {
            if(!Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name))throw new AssertionError("unexpected source Striker caller");
            if(call.name.equals("strikerBeforeClass")) {
                var store=prior(call);var and=prior(store);var dup=prior(and);
                if(!call.desc.equals("(IZZ)I")||!(store instanceof VarInsnNode s)||s.getOpcode()!=ISTORE||and.getOpcode()!=IAND||dup.getOpcode()!=DUP2)throw new AssertionError("invalid source ranged status capture");slot=s.var;
                for(var extra:List.of(dup,and,store))m.instructions.remove(extra);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"BattleStrikerGale","damageAfterStatus","(IZZ)I",false));before++;
            } else {
                var load=prior(call);var finish=prior(load);
                if(!call.desc.equals("(IZ)I")||!(load instanceof VarInsnNode l)||l.getOpcode()!=ILOAD||slot!=l.var||!(finish instanceof MethodInsnNode f)
                        ||!(f.owner.equals(P+"CombatKiringku")&&f.name.equals("finish")||f.owner.equals(P+"CombatCompanionDamage")&&f.name.equals("physical")))throw new AssertionError("invalid source ranged final class position");
                m.instructions.remove(load);m.instructions.remove(call);after++;
            }
        }
        if(slot>=0) {var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var store=following(first);
            if(before!=1||after!=1||first.getOpcode()!=ICONST_0||!(store instanceof VarInsnNode s)||s.getOpcode()!=ISTORE||s.var!=slot)throw new AssertionError("invalid source ranged flag initialization");m.instructions.remove(first);m.instructions.remove(store);
        }
        return m;
    }
    static boolean weaponTypeAdapterOnly(MethodNode original,MethodNode candidate) {
        var m=new MethodNode(candidate.access,candidate.name,candidate.desc,candidate.signature,candidate.exceptions.toArray(String[]::new));candidate.accept(m);
        var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var call=following(first);var store=following(call);
        if(!(first instanceof VarInsnNode l)||l.getOpcode()!=ILOAD||l.var!=0||!(call instanceof MethodInsnNode c)||!c.owner.equals(P+"CombatRules")||!c.name.equals("weaponActionType")||!c.desc.equals("(I)I")
                ||!(store instanceof VarInsnNode s)||s.getOpcode()!=ISTORE||s.var!=0)return false;
        for(var extra:List.of(first,call,store))m.instructions.remove(extra);return semantic(original).equals(semantic(m));
    }
    static boolean shockEffectBoundOnly(MethodNode original,MethodNode candidate) {
        var m=new MethodNode(candidate.access,candidate.name,candidate.desc,candidate.signature,candidate.exceptions.toArray(String[]::new));candidate.accept(m);int count=0;
        for(var n:m.instructions.toArray())if(n instanceof LdcInsnNode v&&Integer.valueOf(65535).equals(v.cst)&&prior(n) instanceof VarInsnNode local&&local.getOpcode()==ILOAD&&local.var==7&&following(n) instanceof JumpInsnNode jump&&Set.of(IF_ICMPLE,IF_ICMPGT).contains(jump.getOpcode())){m.instructions.set(n,new InsnNode(ICONST_4));count++;}
        return count==1&&semantic(original).equals(semantic(m));
    }
    static MethodNode normalizeCompanionDamage(MethodNode source) {
        String helper=P+"CombatCompanionDamage",magic=P+"CombatMagicDamage",physical=helper+"$Physical";int slot=-1;
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)) {
            if(Set.of("ordinary","finalBurn").contains(call.name)) {
                var complete=prior(call);var context=prior(complete);boolean attack=Set.of("useAttackSpell","useClassicWorldAttackSpell").contains(m.name);
                if(!(context instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||complete.getOpcode()!=(attack?ICONST_0:ICONST_1))throw new AssertionError("invalid companion magic context");
                var args=Type.getArgumentTypes(call.desc);m.instructions.remove(context);m.instructions.remove(complete);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,magic,call.name,Type.getMethodDescriptor(Type.getReturnType(call.desc),Arrays.copyOf(args,args.length-2)),false));
            } else if(call.name.equals("beforeCritical")) {
                var attempt=prior(call);if(!(attempt instanceof VarInsnNode v)||v.getOpcode()!=ALOAD)throw new AssertionError("invalid companion critical attempt");m.instructions.remove(attempt);
                var args=Type.getArgumentTypes(call.desc);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,magic,"beforeCritical",Type.getMethodDescriptor(Type.INT_TYPE,Arrays.copyOf(args,args.length-1)),false));
            } else if(Set.of("area","line","ordinaryComplete").contains(call.name)) {
                var context=prior(call);if(!(context instanceof VarInsnNode c)||c.getOpcode()!=ALOAD)throw new AssertionError("invalid companion area context");m.instructions.remove(context);
                var args=Type.getArgumentTypes(call.desc);String name=call.name.equals("area")?"areaSpellDamage":call.name.equals("line")?"lineSpellDamage":"ordinaryMagicDamage";
                m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,P+"ClientSession",name,Type.getMethodDescriptor(Type.INT_TYPE,Arrays.copyOfRange(args,1,args.length-1)),false));
            } else if(call.name.equals("physicalAttempt")) {
                var context=prior(call);var critical=prior(context);var pre=prior(critical);var raw=prior(pre);var dup=prior(raw);var store=following(call);var defense=prior(dup);
                if(!(context instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||!(critical instanceof VarInsnNode cr)||cr.getOpcode()!=ALOAD||!(pre instanceof VarInsnNode p)||p.getOpcode()!=ILOAD||!(raw instanceof VarInsnNode r)||r.getOpcode()!=ILOAD||dup.getOpcode()!=DUP||!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE||!(defense instanceof MethodInsnNode d)||!d.owner.equals(P+"BattlePhysicalDefense")||!d.name.equals("againstNpc"))throw new AssertionError("invalid companion original physical projection");
                slot=s.var;for(var extra:List.of(context,critical,pre,raw,dup,call,store))m.instructions.remove(extra);
            } else if(call.name.equals("physical")) {
                var context=prior(call);var attempt=prior(context);if(!(context instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||!(attempt instanceof VarInsnNode a)||a.getOpcode()!=ALOAD||a.var!=slot)throw new AssertionError("invalid companion physical commit");m.instructions.remove(context);m.instructions.remove(attempt);
                var args=Type.getArgumentTypes(call.desc);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatKiringku","finish",Type.getMethodDescriptor(Type.INT_TYPE,Arrays.copyOf(args,args.length-2)),false));
            } else throw new AssertionError("unknown companion damage hook="+call.name);
        }
        if(slot>=0){var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var store=following(first);if(first.getOpcode()!=ACONST_NULL||!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE||s.var!=slot)throw new AssertionError("invalid companion damage local initialization");m.instructions.remove(first);m.instructions.remove(store);}
        return m;
    }
    static MethodNode normalizeKiringku(MethodNode source) {
        String helper=P+"CombatKiringku";int slot=-1,weaponSlot=-1;
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)) {
            if(!Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name))throw new AssertionError("unrecognized Kiringku caller");
            if(Set.of("player","npc").contains(call.name)) {
                var weapon=prior(call);boolean player=call.name.equals("player");var ac=player?prior(weapon):null;var dup=prior(player?ac:weapon);var defense=prior(dup);var store=following(call);
                if(!(weapon instanceof VarInsnNode w)||w.getOpcode()!=ALOAD||player&&(!(ac instanceof VarInsnNode v)||v.getOpcode()!=ILOAD)||dup.getOpcode()!=DUP||!(defense instanceof MethodInsnNode d)||!d.owner.equals(P+"BattlePhysicalDefense")||!d.name.equals(player?"againstPlayer":"againstNpc")||!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE)throw new AssertionError("invalid source Kiringku defense projection");
                if(slot!=-1&&slot!=s.var||weaponSlot!=-1&&weaponSlot!=w.var)throw new AssertionError("Kiringku projection context drift");slot=s.var;weaponSlot=w.var;
                var extras=new ArrayList<AbstractInsnNode>(List.of(weapon,dup,call,store));if(ac!=null)extras.add(ac);for(var extra:extras)m.instructions.remove(extra);
            } else if(call.name.equals("hit")) {
                var weapon=prior(call);var hit=prior(weapon);var store=following(call);
                if(!(weapon instanceof VarInsnNode w)||w.getOpcode()!=ALOAD||!(hit instanceof VarInsnNode h)||h.getOpcode()!=ILOAD||!(store instanceof VarInsnNode s)||s.getOpcode()!=ISTORE||s.var!=h.var)throw new AssertionError("invalid source Kiringku automatic hit");
                if(weaponSlot!=-1&&weaponSlot!=w.var)throw new AssertionError("Kiringku automatic hit weapon drift");weaponSlot=w.var;for(var extra:List.of(weapon,hit,call,store))m.instructions.remove(extra);
            } else if(call.name.equals("finish")) {
                var armor=prior(call);var hit=prior(armor);var context=prior(hit);var crit=prior(context);var weapon=prior(crit);
                if(!(weapon instanceof VarInsnNode w)||w.getOpcode()!=ALOAD||w.var!=weaponSlot||!(crit instanceof VarInsnNode c)||c.getOpcode()!=ALOAD||!(context instanceof VarInsnNode a)||a.getOpcode()!=ALOAD||a.var!=slot||!(hit instanceof VarInsnNode h)||h.getOpcode()!=ILOAD||armor.getOpcode()!=(m.name.equals("attackClassicWorldPlayer")?ICONST_0:ILOAD))throw new AssertionError("invalid source Kiringku final context");
                for(var extra:List.of(armor,hit,context,crit,weapon))m.instructions.remove(extra);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatClassDamage","physical","(IL"+P+"ClientSession;L"+P+"ClientSession;)I",false));
            } else throw new AssertionError("unknown source Kiringku hook="+call.name);
        }
        if(slot>=0){var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var store=following(first);if(first.getOpcode()!=ACONST_NULL||!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE||s.var!=slot)throw new AssertionError("invalid Kiringku local initialization");m.instructions.remove(first);m.instructions.remove(store);}
        return m;
    }
    static MethodNode normalizeClassDamage(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatClassDamage")&&call.name.equals("physical")) {
            var target=prior(call);var caster=prior(target);var dup=following(call);var hit=following(dup);var swap=following(hit);var qualify=following(swap);var store=following(qualify);
            if(!Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name)||!(caster instanceof VarInsnNode c)||c.getOpcode()!=Opcodes.ALOAD||c.var!=0
                    ||target.getOpcode()!=Opcodes.ACONST_NULL&&target.getOpcode()!=Opcodes.ALOAD||dup.getOpcode()!=Opcodes.DUP||!(hit instanceof VarInsnNode h)||h.getOpcode()!=Opcodes.ILOAD
                    ||swap.getOpcode()!=Opcodes.SWAP||!(qualify instanceof MethodInsnNode q)||!q.owner.equals(P+"CombatClassDamage")||!q.name.equals("hit")||!q.desc.equals("(ZI)Z")
                    ||!(store instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ISTORE||s.var!=h.var)throw new AssertionError("invalid source class physical boundary");
            for(var extra:List.of(target,caster,call,dup,hit,swap,qualify,store))m.instructions.remove(extra);
        }return m;
    }
    static MethodNode normalizeReturnNature(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        if(!m.name.equals("useReturnToNatureSpell"))return m;
        int slot=-1,prepared=0,committed=0;String helper=P+"CombatReturnNature",attempt=helper+"$Attempt";
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)&&call.name.equals("prepare")) {
            var now=prior(call);var controlled=prior(now);var owner=prior(controlled);var actor=prior(owner);
            if(!(actor instanceof VarInsnNode a)||a.getOpcode()!=Opcodes.ALOAD||a.var!=0||!(owner instanceof VarInsnNode o)||o.getOpcode()!=Opcodes.ALOAD
                    ||!(controlled instanceof VarInsnNode c)||c.getOpcode()!=Opcodes.ALOAD||!(now instanceof VarInsnNode t)||t.getOpcode()!=Opcodes.LLOAD)throw new AssertionError("invalid Return Nature live context");
            var dup=following(call);var store=following(dup);var secondDup=following(store);var mrGetter=following(secondDup);var mrStore=following(mrGetter);var getter=following(mrStore);
            if(dup.getOpcode()!=Opcodes.DUP||!(store instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ASTORE||secondDup.getOpcode()!=Opcodes.DUP
                    ||!(mrGetter instanceof MethodInsnNode mg)||!mg.owner.equals(attempt)||!mg.name.equals("magicResistance")||!mg.desc.equals("()I")
                    ||!(mrStore instanceof VarInsnNode ms)||ms.getOpcode()!=Opcodes.ISTORE||!(getter instanceof MethodInsnNode g)||!g.owner.equals(attempt)||!g.name.equals("probability")||!g.desc.equals("()I"))throw new AssertionError("invalid Return Nature proposal local");slot=s.var;
            for(var extra:List.of(now,controlled,owner,actor,dup,store,secondDup,mrGetter,mrStore,getter))m.instructions.remove(extra);
            m.instructions.set(call,new MethodInsnNode(Opcodes.INVOKEVIRTUAL,P+"BattleSpellCatalog$ReturnToNatureSpell","probability","(IIIIII)I",false));prepared++;
        }
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)&&call.name.equals("commit")) {
            var controlled=prior(call);var proposal=prior(controlled);
            if(!(proposal instanceof VarInsnNode p)||p.getOpcode()!=Opcodes.ALOAD||p.var!=slot||!(controlled instanceof VarInsnNode c)||c.getOpcode()!=Opcodes.ALOAD)throw new AssertionError("invalid Return Nature commit context");
            m.instructions.remove(controlled);m.instructions.remove(proposal);m.instructions.remove(call);committed++;
        }
        var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var store=following(first);
        if(prepared!=1||committed!=1||first.getOpcode()!=Opcodes.ACONST_NULL||!(store instanceof VarInsnNode s)||s.getOpcode()!=Opcodes.ASTORE||s.var!=slot)throw new AssertionError("invalid Return Nature initialization/sites");
        m.instructions.remove(first);m.instructions.remove(store);return m;
    }
    static String semantic(MethodNode m) { return semantic(m,false); }
    static boolean zeroMortalBaseOnly(MethodNode original,MethodNode candidate) {
        var fixed=new MethodNode(candidate.access,candidate.name,candidate.desc,candidate.signature,candidate.exceptions.toArray(String[]::new));candidate.accept(fixed);
        var old=Arrays.stream(original.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
        var changed=Arrays.stream(fixed.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
        if(old.size()!=changed.size())return false;int sites=0;
        for(int i=1;i<old.size();i++)if(old.get(i) instanceof JumpInsnNode before&&before.getOpcode()==Opcodes.IFLE
                &&changed.get(i) instanceof JumpInsnNode after&&after.getOpcode()==Opcodes.IFLT
                &&old.get(i-1) instanceof VarInsnNode local&&local.getOpcode()==Opcodes.ILOAD&&local.var==5) {
            after.setOpcode(Opcodes.IFLE);sites++;
        }
        return sites==1&&semantic(original).equals(semantic(fixed));
    }
    static String semantic(MethodNode m,boolean normalizeMaterials) {
        var index=new IdentityHashMap<AbstractInsnNode,Integer>();int i=0;
        for(var n:m.instructions) { index.put(n,i);if(n.getOpcode()>=0)i++; }
        var lines=new ArrayList<String>();
        lines.add(m.access+":"+m.name+":"+m.desc+":"+m.signature+":"+m.exceptions);
        for(var n:m.instructions) {
            if(n.getOpcode()<0)continue;String v="";int opcode=n.getOpcode();
            if(n instanceof IntInsnNode x)v=""+x.operand;
            else if(n instanceof VarInsnNode x)v=""+x.var;
            else if(n instanceof TypeInsnNode x)v=x.desc;
            else if(n instanceof FieldInsnNode x)v=x.owner+"."+x.name+x.desc;
            else if(n instanceof MethodInsnNode x) {
                v=x.owner+"."+x.name+x.desc+":"+x.itf;
                if(normalizeMaterials&&x.owner.equals(P+"CombatDatabase")&&x.name.equals("consumeMaterial")) {
                    opcode=Opcodes.INVOKEVIRTUAL;v=P+"RuntimeInventory.removeCatalog(JJ)Z:false";
                }
            }
            else if(n instanceof InvokeDynamicInsnNode x)v=x.name+x.desc+":"+x.bsm+":"+Arrays.deepToString(x.bsmArgs);
            else if(n instanceof JumpInsnNode x)v=""+index.get(x.label);
            else if(n instanceof LdcInsnNode x)v=x.cst.getClass().getName()+":"+x.cst;
            else if(n instanceof IincInsnNode x)v=x.var+":"+x.incr;
            else if(n instanceof TableSwitchInsnNode x)v=x.min+":"+x.max+":"+index.get(x.dflt)+":"+x.labels.stream().map(index::get).toList();
            else if(n instanceof LookupSwitchInsnNode x)v=x.keys+":"+index.get(x.dflt)+":"+x.labels.stream().map(index::get).toList();
            else if(n instanceof MultiANewArrayInsnNode x)v=x.desc+":"+x.dims;
            lines.add(opcode+":"+v);
        }
        for(var t:m.tryCatchBlocks)lines.add("try:"+index.get(t.start)+":"+index.get(t.end)+":"+index.get(t.handler)+":"+t.type);
        return String.join("\n",lines);
    }
    public static void main(String[] args) throws Exception {
        int untouchedClasses=0,unchangedMethods=0,changedMethods=0,newClasses=0;
        var changes=new ArrayList<String>();
        try(var old=new ZipFile(args[0]);var candidate=new ZipFile(args[1])) {
            var probabilities=wrappers(old,VerifyWorldMagic.normalize(VerifyWorldMultiMagic.normalize(VerifyWorldFreeze.normalize(VerifyWorldFoe.normalize(read(candidate.getInputStream(candidate.getEntry(P+"ClientSession.class")).readAllBytes()))))));
            for(String name:List.of("BattlePatienceStatus","BattleSpellCatalog$PatienceSpell")){var type=read(candidate.getInputStream(candidate.getEntry(P+name+".class")).readAllBytes());int bridges=0;for(var method:type.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatSourceReductions"))bridges++;if(bridges!=1)throw new AssertionError("mandatory Patience scalar="+name);}
            var nature=read(candidate.getInputStream(candidate.getEntry(P+"BattleSpellCatalog$NaturesBlessingSpell.class")).readAllBytes());for(var method:nature.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatRules")&&call.name.equals("naturesBlessing"))throw new AssertionError("Nature extra must not precede target modifiers in lawful catalog");
            var companion=read(candidate.getInputStream(candidate.getEntry(P+"BattleCompanion.class")).readAllBytes());int npcCancel=0;for(var method:companion.methods)for(var n:method.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatNpcDebuffs")){if(!method.name.equals("cancelMagicEffects")||!call.name.equals("cancel"))throw new AssertionError("unexpected NPC lifecycle hook");npcCancel++;}if(npcCancel!=1)throw new AssertionError("NPC Striker cancellation hook="+npcCancel);
            for(var it=old.entries();it.hasMoreElements();) {
                var e=it.nextElement();var n=candidate.getEntry(e.getName());
                if(n==null)throw new AssertionError("lost formal entry: "+e.getName());
                byte[] before=old.getInputStream(e).readAllBytes(),after=candidate.getInputStream(n).readAllBytes();
                if(!e.getName().endsWith(".class")) {
                    if(!Arrays.equals(before,after))throw new AssertionError("changed formal resource: "+e.getName());
                    continue;
                }
                if(Arrays.equals(before,after)){untouchedClasses++;continue;}
                var a=read(before);var b=VerifyWorldMagic.normalize(VerifyWorldMultiMagic.normalize(VerifyWorldFreeze.normalize(VerifyWorldFoe.normalize(read(after)))));var worldCommitExtras=a.name.equals(P+"ClientSession")?VerifyWorldCommit.raw(b):Set.<String>of();if(a.name.equals(P+"ClassicWorldMonsterPopulation")){VerifyMonsterParalysis.raw(b);VerifyMonsterSilence.raw(b);VerifyMonsterDamage.raw(b);VerifyMonsterPoisonParalysis.raw(b);}
                if(a.access!=b.access || !Objects.equals(a.superName,b.superName) || !a.interfaces.equals(b.interfaces))
                    throw new AssertionError("changed class structure: "+a.name);
                var af=a.fields.stream().map(f->f.access+":"+f.name+":"+f.desc+":"+f.signature+":"+f.value).toList();
                var bf=b.fields.stream().map(f->f.access+":"+f.name+":"+f.desc+":"+f.signature+":"+f.value).toList();
                if(a.name.equals(P+"BattleAbilityStatus")) {
                    String extra=(Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL)+":sourceBuffLeases:L"+P+"CombatBuffLeases;:null:null";
                    if(!bf.contains(extra)||bf.size()!=af.size()+1)throw new AssertionError("invalid added source buff state");
                    bf=new ArrayList<>(bf);bf.remove(extra);
                }
                if(a.name.equals(P+"BattleTimedStatus")){String extra=Opcodes.ACC_PRIVATE+":sourcePoisonParalysis:L"+P+"BattleTimedStatus;:null:null";if(!bf.contains(extra)||bf.size()!=af.size()+2)throw new AssertionError("invalid independent poison paralysis slot");bf=new ArrayList<>(bf);bf.remove(extra);}
                if(a.name.equals(P+"BattleTimedStatus")){String extra=Opcodes.ACC_PRIVATE+":sourcePoisonLifecycle:L"+P+"CombatPoisonLifecycle$State;:null:null";if(!bf.contains(extra))throw new AssertionError("missing damage poison lifecycle field");bf=new ArrayList<>(bf);bf.remove(extra);}
                if(!af.equals(bf))throw new AssertionError("changed formal state fields: "+a.name);
                var bm=new HashMap<String,MethodNode>();for(var m:b.methods)bm.put(m.name+m.desc,m);
                for(var m:a.methods) {
                    var peer=bm.remove(m.name+m.desc);
                    if(peer==null)throw new AssertionError("lost formal method: "+a.name+"."+m.name+m.desc);
                    if(semantic(m).equals(semantic(peer))){unchangedMethods++;continue;}
                    boolean allowed=a.name.equals(P+"BattleTimedStatus")&&semantic(m).equals(semantic(VerifyPoisonParalysis.normalize(peer,a.name)))||ALLOWED.getOrDefault(a.name,Set.of()).contains(m.name)
                            || a.name.startsWith(P+"BattleSpellCatalog$")&&!m.name.startsWith("<");
                    if(a.name.equals(P+"ClientSession")&&m.name.startsWith("use")&&m.desc.contains("BattleSpellCatalog$"))
                        allowed|=semantic(m).equals(semantic(peer,true));
                    if(a.name.equals(P+"ClientSession"))allowed|=semantic(m).equals(semantic(normalizeProbability(normalizeReturnNature(normalizeMagicDamage(normalizeClassDamage(normalizeKiringku(normalizeCompanionDamage(normalizeRangedDamage(normalizeDragon(normalizeAwaken(normalizeBuff(normalizeWeaponBuff(normalizeSpecialBuff(normalizeCompanionSpell(VerifyWorldPhysical.normalize(VerifyCounterDetection.normalize(VerifyPlayerMagic.normalize(VerifyPlayerMirror.normalize(VerifyPlayerDamage.normalize(VerifyNpcPlayerPhysical.normalize(VerifyNpcMagic.normalize(VerifyHealing.normalize(VerifyNpcDebuffs.normalize(VerifyLineSpells.normalize(VerifyNpcPoison.normalize(VerifyIncomingSource.normalize(VerifyPoisonAtomic.normalize(VerifyPoisonParalysis.normalize(VerifyWorldCommit.normalize(VerifyPoisonLifecycle.nativeMethod(peer)),a.name))))))))))))))),a.name)),a.name))))))))),probabilities),true));
                    if(a.name.equals(P+"BattleSpellCatalog$HealingSpell")&&m.name.equals("npcHpDelta")&&!semantic(m).equals(semantic(VerifyNpcDebuffs.signedHealing(peer))))throw new AssertionError("NPC signed healing changed native undead/validation rules");
                    if(a.name.equals(P+"BattleCompanion"))allowed|=semantic(m).equals(semantic(VerifyNpcDebuffs.normalize(peer)));
                    if(a.name.equals(P+"GameHub")&&m.name.equals("close")&&m.desc.equals("()V"))allowed|=semantic(m).equals(semantic(VerifyMonsterDamage.hubClose(peer)));
                    if(a.name.equals(P+"ClassicWorldMonsterPopulation"))allowed|=semantic(m).equals(semantic(VerifyMonsterParalysis.normalize(peer)));
                    if(a.name.equals(P+"BattleMortalBody$Decision")&&m.name.equals("<init>"))allowed|=zeroMortalBaseOnly(m,peer);
                    if(a.name.equals(P+"BattleShockStunEffects")&&m.name.equals("place"))allowed|=shockEffectBoundOnly(m,peer);
                    if(a.name.equals(P+"BattleStrikerGale")&&m.name.equals("playerWeaponEligible")||a.name.equals(P+"BattleArmorBreak")&&m.name.equals("supportsWeaponType"))allowed|=weaponTypeAdapterOnly(m,peer);
                    allowed|=semantic(m).equals(semantic(normalizeBuff(peer,a.name)));
                    if(Set.of(P+"BattleDragonEyeCatalog$Eye",P+"ClassicWorldNpcCatalog",P+"ClassicWorldMapCatalog").contains(a.name))allowed|=semantic(m).equals(semantic(normalizeSpecialBuff(peer,a.name)));
                    if(a.name.equals(P+"BattleAbilityStatus$IllusionAvatar"))allowed|=semantic(m).equals(semantic(VerifyPlayerMirror.avatar(peer,a.name)));
                    if(Set.of(P+"BattlePatienceStatus",P+"BattleSpellCatalog$PatienceSpell").contains(a.name)){boolean same=semantic(m).equals(semantic(VerifyPatience.normalize(peer,a.name)));if((m.name.equals("physicalReduction")||m.name.equals("physicalDamageReduction"))&&!same)throw new AssertionError("Patience changed native activation/lease/scalar body");allowed|=same;}
                    if(!allowed)throw new AssertionError("unexpected formal change: "+a.name+"."+m.name+m.desc);
                    changedMethods++;changes.add(a.name+"."+m.name+m.desc);
                }
                if(a.name.equals(P+"ClientSession")){bm.keySet().removeAll(worldCommitExtras);var atomicState=bm.remove("sourceNpcPoisonState(JIIIJLjava/lang/String;)L"+P+"CombatPoisonAtomic$NpcState;");if(atomicState==null||!semantic(VerifyPoisonParalysis.normalize(VerifyPoisonLifecycle.nativeMethod(atomicState),a.name)).equals(semantic(VerifyPoisonAtomic.state)))throw new AssertionError("missing exact NPC poison state clone");bm.keySet().removeAll(VerifyPoisonLifecycle.extraMethods);bm.keySet().removeAll(probabilities.keySet());var clone=bm.remove("sourceMagicDamageBeforeResistance(IIIIIILjava/lang/String;)L"+P+"CombatMagicDamage$Attempt;");if(clone==null)throw new AssertionError("missing pre-MR source magic method");verifyMagicClone(a.methods.stream().filter(v->v.name.equals("ordinaryMagicDamage")).findFirst().orElseThrow(),clone);var npcClone=bm.remove("sourceNpcMagicBeforeResistance(IIIIIILjava/lang/String;)L"+P+"CombatMagicDamage$Attempt;");if(npcClone==null)throw new AssertionError("missing signed NPC magic clone");VerifyNpcMagic.cloneOnly(a.methods.stream().filter(v->v.name.equals("ordinaryMagicDamage")).findFirst().orElseThrow(),npcClone);var mirror=bm.remove("publishSourcePlayerCounterMirrorTrigger(L"+P+"ClientSession;L"+P+"CombatPlayerMirror$Decision;Ljava/lang/String;)V");if(mirror==null)throw new AssertionError("missing typed source PC mirror publisher");VerifyPlayerMirror.cloneOnly(a.methods.stream().filter(v->v.name.equals("publishPlayerCounterMirrorTrigger")).findFirst().orElseThrow(),mirror);}
                if(a.name.equals(P+"BattleChainswordWeakness"))bm.keySet().removeAll(Set.of(
                        "sourceAttack(IIJJI)L"+P+"BattleChainswordWeakness$AttackOutcome;","sourceNormalAttack(J)V","sourceConsumeOnHit()I"));
                if(a.name.equals(P+"ForcedCombat")) {
                    var target=bm.remove("canAttackTarget(L"+P+"GameHub;JL"+P+"ClientSession;L"+P+"ClientSession;J)Z");
                    if(target==null||target.access!=ACC_STATIC)throw new AssertionError("missing object-specific Ctrl helper");
                    // Pin the reviewed new helper, including try/finally intent restoration.
                    String digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(semantic(target).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    if(!digest.equals("df36d71ca3e33882d9239c9e957851bdf0cac04653e2c074817ea0d6c1aa2f93"))throw new AssertionError("object-specific Ctrl helper changed="+digest);
                }
                if(!bm.isEmpty())throw new AssertionError("unexpected added methods: "+a.name+" "+bm.keySet());
            }
            for(var it=candidate.entries();it.hasMoreElements();) {
                var e=it.nextElement();if(old.getEntry(e.getName())!=null)continue;
                if(!e.getName().startsWith(P+"CombatRules")&&!e.getName().startsWith(P+"CombatDatabase")
                        &&!e.getName().startsWith(P+"SourceSkillControls")&&!e.getName().startsWith(P+"CombatAccess")
                        &&!e.getName().startsWith(P+"CombatProbability")&&!e.getName().startsWith(P+"CombatDragon")&&!e.getName().startsWith(P+"CombatAwaken")&&!e.getName().startsWith(P+"CombatBuffLeases")&&!e.getName().startsWith(P+"CombatWeaponBuffs")&&!e.getName().startsWith(P+"SourceEyeControls")&&!e.getName().startsWith(P+"SourceBlessingControls")&&!e.getName().startsWith(P+"CombatSpecialBuffs")&&!e.getName().startsWith(P+"CombatReturnNature")&&!e.getName().startsWith(P+"CombatClassDamage")&&!e.getName().startsWith(P+"CombatMagicDamage")&&!e.getName().startsWith(P+"CombatKiringku")&&!e.getName().startsWith(P+"CombatCompanionDamage")&&!e.getName().startsWith(P+"CombatCompanionSpells")&&!e.getName().startsWith(P+"CombatLineSpells")&&!e.getName().startsWith(P+"CombatWorldPhysical")&&!e.getName().startsWith(P+"CombatNpcDebuffs")&&!e.getName().startsWith(P+"CombatHealing")&&!e.getName().startsWith(P+"CombatNpcMagic")&&!e.getName().startsWith(P+"CombatCounterDetection")&&!e.getName().startsWith(P+"CombatPlayerMagic")&&!e.getName().startsWith(P+"CombatPlayerMirror")&&!e.getName().startsWith(P+"CombatPlayerDamage")&&!e.getName().startsWith(P+"CombatPlayerRecovery")&&!e.getName().startsWith(P+"CombatSourceReductions")&&!e.getName().startsWith(P+"CombatNpcPlayerDamage")&&!e.getName().startsWith(P+"CombatNpcPhysicalSource")&&!e.getName().startsWith(P+"CombatNpcPoison")&&!e.getName().startsWith(P+"CombatIncomingSource")&&!e.getName().startsWith(P+"CombatPoisonAtomic")&&!e.getName().startsWith(P+"CombatPoisonParalysis")&&!e.getName().startsWith(P+"CombatPoisonLifecycle")&&!Set.of(P+"CombatMonsterParalysis.class",P+"CombatMonsterParalysis$Poll.class").contains(e.getName())&&!Set.of(P+"CombatMonsterCurseLifecycle.class",P+"CombatMonsterCurseLifecycle$Curse.class",P+"CombatMonsterCurseLifecycle$Actor.class",P+"CombatMonsterCurseLifecycle$Recipient.class",P+"CombatMonsterCurseLifecycle$Context.class",P+"CombatMonsterCurseLifecycle$Token.class",P+"CombatMonsterCurseLifecycle$Delivery.class",P+"CombatMonsterCurseLifecycle$Scope.class",P+"CombatMonsterCurseLifecycle$Removal.class",P+"CombatMonsterCurseLifecycle$Damage.class",P+"CombatMonsterCurseLifecycle$DamageTask.class",P+"CombatMonsterCurseLifecycle$DamageUpdates.class",P+"CombatMonsterCurseLifecycle$Paralysis.class",P+"CombatMonsterCurseLifecycle$DamageDispatch.class").contains(e.getName())&&!Set.of(P+"CombatWorldCommit.class",P+"CombatWorldCommit$Attempt.class",P+"CombatWorldCommit$Plan.class",P+"CombatWorldCommit$Physical.class",P+"CombatWorldCommit$Doll.class").contains(e.getName())&&!Set.of(P+"CombatWorldMagic.class",P+"CombatWorldMagic$Spell.class",P+"CombatWorldMagic$Attempt.class").contains(e.getName())&&!Set.of(P+"CombatWorldMultiMagic.class",P+"CombatWorldMultiMagic$Multi.class",P+"CombatWorldMultiMagic$Pending.class",P+"CombatWorldMultiMagic$Batch.class").contains(e.getName())&&!Set.of(P+"CombatWorldFreeze.class",P+"CombatWorldFreeze$Frozen.class",P+"CombatWorldFreeze$Pending.class",P+"CombatWorldFreeze$Batch.class",P+"CombatWorldFoe.class",P+"CombatWorldFoe$Pending.class").contains(e.getName())&&!Set.of(P+"CombatNpcDamageQueue.class",P+"CombatNpcDamageQueue$Event.class",P+"CombatNpcDamageQueue$Queue.class").contains(e.getName())&&!e.getName().equals(P+"CombatDeathCancellation.class")&&!Set.of("CombatCurseLifecycle","CombatCurseLifecycle$Context","CombatCurseLifecycle$Curse","CombatCurseLifecycle$Palette","CombatCurseLifecycle$Release","CombatCurseLifecycle$Removal","CombatCurseLifecycle$Scope","CombatCurseLifecycle$Snapshot","CombatCurseLifecycle$Target").stream().anyMatch(n->e.getName().equals(P+n+".class")))
                    throw new AssertionError("unexpected candidate entry: "+e.getName());
                newClasses++;
            }
        }
        Files.write(Path.of(args[2]),changes);
        System.out.println("PASS formal bytecode preservation: identical classes="+untouchedClasses
                +"; equivalent methods in changed classes="+unchangedMethods+"; declared changed methods="+changedMethods
                +"; new combat classes="+newClasses+"; native admission/packets preserved outside declared source combat transforms");
    }
}
