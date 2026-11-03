import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class Patch implements Opcodes {
 static final String P="local/lito381/server/",H=P+"ResourceDatabase";
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 public static void main(String[] a)throws Exception{
  Path base=Path.of(a[0]),classes=Path.of(a[1]),out=Path.of(a[2]);
  if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base))).equals("9719f45db324206617726ce06d1a9e2184e9a3b3aefab3d50f3ebbdfbf09cfac"))throw new IllegalStateException("0.6.61 baseline mismatch");
  Set<String> fields=new HashSet<>(Files.readAllLines(Path.of(a[3])));Map<String,Integer> hooks=new TreeMap<>();
  ProbabilityPatch.load(Path.of(a[4]));PoisonLifecyclePatch.frameLoader=new java.net.URLClassLoader(new java.net.URL[]{classes.toUri().toURL(),base.toUri().toURL()},ClassLoader.getPlatformClassLoader());
  List<Path> generated;try(var f=Files.walk(classes.resolve(P))){generated=f.filter(p->p.toString().endsWith(".class")).toList();}
  Set<String> replace=new HashSet<>();for(var p:generated)replace.add(classes.relativize(p).toString().replace('\\','/'));
  try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))){
   for(var it=z.entries();it.hasMoreElements();){var e=it.nextElement();if(replace.contains(e.getName()))continue;byte[] bytes=z.getInputStream(e).readAllBytes();
    if(e.getName().startsWith(P)&&e.getName().endsWith(".class")&&
      (e.getName().contains("BattleSpellCatalog$")||CombatPatch.CLASSES.contains(e.getName())||PatiencePatch.accepts(e.getName())||BuffPatch.accepts(e.getName())||SpecialBuffPatch.accepts(e.getName())||BlessingNpcPatch.accepts(e.getName())||Set.of(P+"BattleFreezeEffects.class",P+"BattleAbilityStatus$IllusionAvatar.class",P+"BattleCompanion.class",P+"ClassicWorldMonsterPopulation.class",P+"BattleTimedStatus.class",P+"GameHub.class",P+"ClientSession.class",P+"Protocol.class",P+"NativeLitoMapTableCatalog.class",P+"NativeLitoZoneCatalog.class",P+"NativeLitoTownPortalCatalog.class",P+"NativeLitoRentalTownNpcCatalog$Npc.class").contains(e.getName()))){
     var c=new ClassNode();new ClassReader(bytes).accept(c,0);boolean changed=false;
     if(c.name.equals(P+"BattleTimedStatus")){PoisonParalysisPatch.timed(c,hooks);PoisonLifecyclePatch.timed(c,hooks);changed=true;}
     if(c.name.equals(P+"BattleAbilityStatus$IllusionAvatar")){PlayerMirrorPatch.avatar(c,hooks);changed=true;}
     if(c.name.equals(P+"BattleCompanion")){NpcDebuffPatch.companion(c,hooks);changed=true;}
     if(c.name.equals(P+"ClassicWorldMonsterPopulation")){MonsterParalysisPatch.apply(c,hooks);MonsterSilencePatch.apply(c,hooks);MonsterDamagePatch.apply(c,hooks);MonsterPoisonParalysisPatch.apply(c,hooks);WorldCommitPatch.population(c,hooks);WorldFreezePatch.population(c,hooks);changed=true;}
     if(c.name.equals(P+"BattleSpellCatalog$HealingSpell")){NpcDebuffPatch.healingCatalog(c,hooks);changed=true;}
     if(PatiencePatch.accepts(e.getName())){PatiencePatch.apply(c,hooks);changed=true;}
     int priorHooks=hooks.size();CombatPatch.apply(c,hooks);if(hooks.size()!=priorHooks)changed=true;
     priorHooks=hooks.size();BuffPatch.apply(c,hooks);if(hooks.size()!=priorHooks)changed=true;
     if(c.name.equals(P+"ClientSession")){WeaponBuffPatch.apply(c,hooks);DeathPenaltyPatch.apply(c,hooks);changed=true;}
     priorHooks=hooks.size();BlessingNpcPatch.apply(c,hooks);if(hooks.size()!=priorHooks)changed=true;
     priorHooks=hooks.size();SpecialBuffPatch.apply(c,hooks);if(hooks.size()!=priorHooks)changed=true;
     if(c.name.equals(P+"ClientSession")){MagicDamagePatch.apply(c,hooks);ClassDamagePatch.apply(c,hooks);KiringkuPatch.apply(c,hooks);CompanionDamagePatch.apply(c,hooks);RangedDamagePatch.apply(c,hooks);CompanionSpellPatch.apply(c,hooks);ReturnNaturePatch.apply(c,hooks);ProbabilityPatch.apply(c,hooks);DragonPatch.apply(c,hooks);AwakenPatch.apply(c,hooks);LineSpellPatch.apply(c,hooks);WorldCompanionPhysicalPatch.apply(c,hooks);NpcDebuffPatch.session(c,hooks);HealingPatch.apply(c,hooks);NpcMagicPatch.apply(c,hooks);CounterDetectionPatch.apply(c,hooks);PlayerMagicPatch.apply(c,hooks);PlayerMirrorPatch.apply(c,hooks);PlayerDamagePatch.apply(c,hooks);NpcPlayerPhysicalPatch.apply(c,hooks);NpcPoisonPatch.apply(c,hooks);IncomingSourcePatch.apply(c,hooks);PoisonAtomicPatch.apply(c,hooks);PoisonParalysisPatch.session(c,hooks);hooks.put("equipment-transitions",EquipmentPatch.apply(c));MovementLockPatch.session(c);ForcedCombatPatch.session(c);GmTravelPatch.apply(c);DeathCancellationPatch.apply(c,hooks);PoisonLifecyclePatch.session(c,hooks);WorldCommitPatch.session(c,hooks);WorldMagicPatch.apply(c,hooks);WorldMultiMagicPatch.apply(c,hooks);WorldFreezePatch.session(c,hooks);WorldFoePatch.apply(c,hooks);hooks.put("gm-travel",1);hooks.put("forced-combat-dispatch",1);hooks.put("movement-lock-release",1);changed=true;}
     if(c.name.equals(P+"BattleFreezeEffects")){WorldFreezePatch.effects(c,hooks);changed=true;}
     if(c.name.equals(P+"GameHub")){var close=c.methods.stream().filter(m->m.name.equals("close")&&m.desc.equals("()V")).findFirst().orElseThrow();var head=new InsnList();head.add(new VarInsnNode(ALOAD,0));head.add(new MethodInsnNode(INVOKESTATIC,P+"CombatMonsterCurseLifecycle","closeHub","(L"+P+"GameHub;)V",false));close.instructions.insert(head);hooks.put("source-NPC-damage-hub-close",1);ForcedCombatPatch.hub(c);hooks.put("forced-combat-target",1);changed=true;}
     if(c.name.equals(P+"Protocol")){MovementLockPatch.protocol(c);changed=true;}
     for(var m:c.methods){
      if(c.name.contains("BattleSpellCatalog$")&&(m.access&ACC_STATIC)==0&&!m.name.equals("<init>")){
       String type=c.name.substring(c.name.indexOf('$')+1);
       boolean hasField=c.fields.stream().anyMatch(f->f.name.equals("skillId")&&f.desc.equals("I"));
       boolean hasSkill=hasField||c.methods.stream().anyMatch(x->x.name.equals("skillId")&&x.desc.equals("()I"));
       if(hasSkill)for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(c.name)){
        if(f.desc.equals("I")&&(fields.contains(type+"\t"+f.name)||CombatPatch.NUMERIC_FIELDS.contains(f.name)||CombatPatch.CONTROL_FIELDS.contains(f.name)||type.equals("FoeSlayerSpell")&&Set.of("hitCount","exposureDamageStep").contains(f.name)||f.name.equals("iconType")||f.name.equals("legacyIconType"))){
         var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(hasField?new FieldInsnNode(GETFIELD,c.name,"skillId","I"):new MethodInsnNode(INVOKEVIRTUAL,c.name,"skillId","()I",false));i.add(new LdcInsnNode(type));i.add(new LdcInsnNode(f.name));i.add(call("value","(IILjava/lang/String;Ljava/lang/String;)I"));m.instructions.insert(n,i);hooks.merge("skill-field",1,Integer::sum);changed=true;
        }
        if(f.desc.equals("D")&&f.name.equals("damageMultiplier")&&type.equals("CounterBarrierSpell")) {
         var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new FieldInsnNode(GETFIELD,c.name,"skillId","I"));i.add(new LdcInsnNode(f.name));i.add(CombatPatch.call("doubleField","(DILjava/lang/String;)D"));m.instructions.insert(n,i);hooks.merge("combat-double-field",1,Integer::sum);changed=true;
        }
        if(f.desc.equals("J")&&f.name.equals("itemConsumeId")) {
         var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(hasField?new FieldInsnNode(GETFIELD,c.name,"skillId","I"):new MethodInsnNode(INVOKEVIRTUAL,c.name,"skillId","()I",false));i.add(new LdcInsnNode(f.name));i.add(new MethodInsnNode(INVOKESTATIC,P+"CombatDatabase","longValue","(JILjava/lang/String;)J",false));m.instructions.insert(n,i);hooks.merge("combat-material-long",1,Integer::sum);changed=true;
        }
       }
      }
      if(c.name.equals(P+"Protocol")&&m.name.equals("rangedPhysicalAttack")){
       var i=new InsnList();i.add(new VarInsnNode(ILOAD,9));i.add(call("arrow","(I)I"));i.add(new VarInsnNode(ISTORE,9));m.instructions.insert(i);hooks.merge("arrow",1,Integer::sum);changed=true;
       for(var n:m.instructions.toArray())if(n instanceof IntInsnNode v&&v.operand==127){m.instructions.insert(n,call("flightType","(I)I"));hooks.merge("flight-type",1,Integer::sum);}
      }
      for(var n:m.instructions.toArray()){
       if(n.getOpcode()!=ARETURN&&n.getOpcode()!=IRETURN)continue;var i=new InsnList();String label=null;
       if(c.name.equals(P+"NativeLitoMapTableCatalog"))switch(m.name){
        case "active":i.add(call("maps","(Ljava/util/List;)Ljava/util/List;"));label="maps";break;
        case "forMap":i.add(new VarInsnNode(ILOAD,0));i.add(call("map","(L"+P+"NativeLitoMapTableCatalog$Entry;I)L"+P+"NativeLitoMapTableCatalog$Entry;"));label="map";break;
        case "isActive":i.add(new VarInsnNode(ILOAD,0));i.add(call("mapActive","(ZI)Z"));label="map-active";break;
       }
       if(c.name.equals(P+"NativeLitoZoneCatalog"))switch(m.name){
        case "zones":i.add(call("zones","(Ljava/util/List;)Ljava/util/List;"));label="zones";break;
        case "colors":i.add(call("colors","(Ljava/util/List;)Ljava/util/List;"));label="colors";break;
        case "forMap":i.add(new VarInsnNode(ILOAD,0));i.add(call("zone","(L"+P+"NativeLitoZoneCatalog$Zone;I)L"+P+"NativeLitoZoneCatalog$Zone;"));label="zone";break;
       }
       if(c.name.equals(P+"NativeLitoTownPortalCatalog"))switch(m.name){
        case "find":for(int slot=0;slot<3;slot++)i.add(new VarInsnNode(ILOAD,slot));i.add(call("portal","(L"+P+"NativeLitoTownPortalCatalog$Route;III)L"+P+"NativeLitoTownPortalCatalog$Route;"));label="portal";break;
        case "routesForMap":i.add(new VarInsnNode(ILOAD,0));i.add(call("portals","(Ljava/util/List;I)Ljava/util/List;"));label="portals";break;
        case "routeCount":i.add(call("portalCount","(I)I"));label="portal-count";break;
       }
       if(c.name.equals(P+"NativeLitoRentalTownNpcCatalog$Npc")&&m.name.equals("name")){
        i.add(new VarInsnNode(ALOAD,0));i.add(new FieldInsnNode(GETFIELD,c.name,"resourceIndex","I"));i.add(call("npcName","(Ljava/lang/String;I)Ljava/lang/String;"));label="npc-name";
       }
       if(c.name.equals(P+"Protocol")&&m.name.equals("showMap")){
        i.add(new VarInsnNode(ILOAD,2));i.add(call("mapPage","([BI)[B"));label="map-page";
       }
       if(label!=null){m.instructions.insertBefore(n,i);hooks.merge(label,1,Integer::sum);changed=true;}
      }
     }
     if(changed){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);bytes=w.toByteArray();}
    }
    var entry=new ZipEntry(e.getName());entry.setTime(0);o.putNextEntry(entry);o.write(bytes);o.closeEntry();
   }
   for(var p:generated.stream().sorted().toList()){var entry=new ZipEntry(classes.relativize(p).toString().replace('\\','/'));entry.setTime(0);o.putNextEntry(entry);o.write(Files.readAllBytes(p));o.closeEntry();}
  }
  CombatPatch.require(hooks.getOrDefault("source-weapon-type-adapters",0),2,"source physical weapon subtype adapters");
  System.out.println(hooks);
  for(String k:List.of("skill-field","arrow","flight-type","maps","map","map-active","zones","colors","zone","portal","portals","portal-count","npc-name","map-page"))if(!hooks.containsKey(k))throw new IllegalStateException("missing hook "+k);
  for(String k:List.of("combat-extra-ac","source-nature-extra-after-status","source-group-water-life","source-group-owned-healing","combat-pet-to-pc",
    "combat-proc-burningSpirit","combat-proc-doubleBrake","combat-proc-elementalFire",
    "combat-soul-of-flame","combat-striker","combat-immune-to-harm","combat-joy",
    "combat-double-field","combat-material-long","combat-material-consumers","combat-teleport-values","combat-magic-class-types",
    "combat-probability-targets","combat-boss-gates"))if(!hooks.containsKey(k))throw new IllegalStateException("missing combat hook "+k);
 }
}
