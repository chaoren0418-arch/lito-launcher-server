import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Source skill BUFF ownership at the native admission and damage boundaries. */
final class WeaponBuffPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatWeaponBuffs",C=P+"ClientSession";
    static final String W=P+"BattleWeaponBuffStatus",I=P+"BattleItemWeaponEnchantStatus",A=P+"BattleItemArmorEnchantStatus";
    static void replace(MethodNode method,MethodInsnNode old,String helper,boolean caster,int actor) {
        if(caster)method.instructions.insertBefore(old,new VarInsnNode(ALOAD,actor));
        String descriptor="(L"+old.owner+";"+old.desc.substring(1);
        if(caster){int at=descriptor.indexOf(')');descriptor=descriptor.substring(0,at)+"L"+C+";"+descriptor.substring(at);}
        method.instructions.set(old,new MethodInsnNode(INVOKESTATIC,H,helper,descriptor,false));
    }
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        var counts=new TreeMap<String,Integer>();
        for(var m:c.methods) {
            boolean itemWeapon=m.name.equals("useItemWeaponEnchantSpell"),itemArmor=m.name.equals("useItemArmorEnchantSpell");
            boolean activatesItem=Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode call&&call.owner.equals(I)&&call.name.equals("activate"));
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call) {
                if(call.owner.equals(P+"BattleSpellCatalog$WeaknessSpell")) {
                    String extra=Set.of("attack","attackClassicWorldPlayer").contains(m.name)&&Set.of("meleeDamageModifier","meleeHitModifier").contains(call.name)?"legacyWeakness"
                            :m.name.equals("battleArmorClass")&&call.name.equals("armorClassModifier")?"diseaseArmor":null;
                    if(extra!=null){m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,H,extra,"(I)I",false));counts.merge(extra,1,Integer::sum);}
                }
                String helper=null;boolean actor=true;int index=0;
                if(call.owner.equals(W)&&call.name.equals("activate")) {
                    helper="activateWeapon";
                    if(m.name.equals("useWeaponBuffSpell"))index=DragonPatch.local(m,"playerTarget");
                    else if(!m.name.equals("maintainNativeMagicDollAutoSkills"))throw new IllegalStateException("unknown weapon BUFF owner: "+m.name);
                } else if(call.owner.equals(I)&&call.name.equals("activate"))helper="activateItemWeapon";
                else if(call.owner.equals(A)&&call.name.equals("activate"))helper="activateArmor";
                else if(call.owner.equals(W)&&call.name.equals("modifier"))helper=m.name.equals("maintainNativeMagicDollAutoSkills")?"autoWeaponModifier":"weaponModifier";
                else if(call.owner.equals(I)&&call.name.equals("modifier"))helper="itemWeaponModifier";
                else if(call.owner.equals(A)&&call.name.equals("modifier"))helper="itemArmorModifier";
                else if(call.owner.equals(W)&&call.name.equals("cancelMagicEffects")&&activatesItem){helper="keepWeaponBuffs";actor=false;}
                else if(call.owner.equals(I)&&call.name.equals("remove")&&m.name.equals("useWeaponBuffSpell")){helper="keepItemBuff";actor=false;}
                else if(call.owner.equals(W)&&call.name.equals("removeSkill"))helper="removeWeaponSkill";
                else if(call.owner.equals(A)&&call.name.equals("removeSkill"))helper="removeArmorSkill";
                else if(itemWeapon&&call.owner.equals(P+"RuntimeInventory$Item")&&call.name.equals("objectId")){helper="itemObjectId";actor=false;}
                else if(itemWeapon&&call.owner.equals(P+"RuntimeInventory$Item")&&call.name.equals("nativeLogName"))helper="itemLogName";
                else if(itemArmor&&call.owner.equals(P+"RuntimeInventory")&&call.name.equals("item"))helper="bodyArmor";
                if(helper!=null){replace(m,call,helper,actor,index);counts.merge(helper,1,Integer::sum);}
            }
            if(itemWeapon) {
                int slot=DragonPatch.local(m,"validWeapon");
                for(var n:m.instructions.toArray())if(n instanceof VarInsnNode store&&store.getOpcode()==ISTORE&&store.var==slot) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,H,"equippedWeapon","(ZL"+C+";)Z",false));m.instructions.insertBefore(n,i);counts.merge("equippedWeapon",1,Integer::sum);
                }
            }
            if(m.name.equals("activeItemArmorEnchantModifier"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN) {
                var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(LLOAD,1));i.add(new MethodInsnNode(INVOKESTATIC,H,"armor","(IL"+C+";J)I",false));m.instructions.insertBefore(n,i);counts.merge("armor",1,Integer::sum);
            }
        }
        var expected=Map.ofEntries(Map.entry("activateWeapon",2),Map.entry("activateItemWeapon",2),Map.entry("activateArmor",3),
                Map.entry("weaponModifier",3),Map.entry("autoWeaponModifier",1),Map.entry("itemWeaponModifier",3),Map.entry("itemArmorModifier",2),
                Map.entry("keepWeaponBuffs",2),Map.entry("keepItemBuff",1),Map.entry("removeWeaponSkill",1),Map.entry("removeArmorSkill",1),
                Map.entry("itemObjectId",3),Map.entry("itemLogName",3),Map.entry("bodyArmor",1),Map.entry("equippedWeapon",1),Map.entry("armor",1),Map.entry("legacyWeakness",8),Map.entry("diseaseArmor",1));
        if(!counts.equals(expected))throw new IllegalStateException("source weapon BUFF call-site drift: "+counts);
        hooks.put("source-player-weapon-buffs",counts.values().stream().mapToInt(Integer::intValue).sum());
    }
}
