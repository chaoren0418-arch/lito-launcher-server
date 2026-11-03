package local.lito381.server;

import java.util.*;

/** Actual original material, runtime weapon and source-overlay IOException paths in the TEST JAR. */
public final class SourceWeaponRollbackPlan {
    static int checks,attempts;
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    static void attempt(String mode)throws Throwable{
        boolean source=mode.startsWith("source");
        var overlay=source?SourceWeaponOverlayPlan.fixture("normal"):null;
        var f=source?overlay.nativeFixture():SourceWeaponPlayerFullPathPlan.fixture("normal");var b=f.base().first().base().base();
        var state=(GameStateStore)CombatAccess.field(b.target(),"state");
        String key=(String)CombatAccess.call(b.target(),"stateCharacter","()Ljava/lang/String;");
        Object profile=CombatAccess.call(state,"profile","(Ljava/lang/String;)Llocal/lito381/server/GameStateStore$MutableProfile;",key);
        if(mode.equals("source-reconcile"))SourceWeaponFullPathPlan.put(profile,"sourceWeaponDurability",null);
        var oldOverlay=CombatAccess.field(profile,"sourceWeaponDurability");var oldEconomy=state.economy(key);
        var inventory=(RuntimeInventory)CombatAccess.field(b.target(),"inventory");var casterInventory=(RuntimeInventory)CombatAccess.field(b.caster(),"inventory");
        String casterKey=(String)CombatAccess.call(b.caster(),"stateCharacter","()Ljava/lang/String;");var casterEconomy=state.economy(casterKey);
        boolean material=mode.equals("material");int failAt=material?1:2;Throwable error=null;
        try{
            WeaponStateFault.arm(state,failAt);
            try{SourceWeaponPlayerFullPathPlan.execute(f);}catch(Throwable t){error=t;}
            check(material?error==null:error!=null,"original material handler rejects locally; later target save failure preserves its caller outcome "+mode);
            if(!material){Throwable root=error;while(root.getCause()!=null)root=root.getCause();check(root instanceof java.io.IOException&&root.getMessage().equals("detached-weapon-state-save-failure"),"exact detached original target-save IOException retained "+mode);}else check(WeaponStateFault.failures==1,"original material handler observed the injected IOException and returned rejection");
            check(WeaponStateFault.failedOrdinal==failAt&&WeaponStateFault.calls>=failAt&&WeaponStateFault.failures==1,"selected original save boundary fails once; ordinary rollback saves remain allowed "+mode);
            check(CombatAccess.field(b.target(),"inventory")==inventory&&state.economy(key).equals(oldEconomy),"original target runtime inventory/economy rollback exact "+mode);
            check(CombatAccess.field(profile,"sourceWeaponDurability")==oldOverlay,"original source-overlay/reconciliation rollback restores exact previous reference "+mode);
            check((int)CombatAccess.field(b.caster(),"playerMp")== (material?100:71),"original MP boundary remains before/after admitted material failure "+mode);
            check(((RuntimeInventory)CombatAccess.field(b.caster(),"inventory")).countCatalog(40318)==(material?3:2),"original caster material boundary retained "+mode);
            if(material)check(CombatAccess.field(b.caster(),"inventory")==casterInventory&&state.economy(casterKey).equals(casterEconomy),"failed original material transaction restores exact caster inventory and economy");
            else check(state.economy(casterKey).inventory().get(40318L)==2,"accepted original caster material remains committed when later weapon save fails");
            if(source)check(overlay.weapon().durability()==0&&CombatAccess.field(b.target(),"sourceEquipmentMagicStats")==overlay.equipment(),"failed source save never mutates read-only Source stats");
            attempts++;
        }finally{
            WeaponStateFault.clear();check(WeaponStateFault.armed==null&&WeaponStateFault.failAt==0,"test fault releases its state reference");SourceWeaponFullPathPlan.cleanup(f);
        }
    }
    public static void main(String[]args)throws Throwable{
        SourceWeaponOverlayPlan.environment(()->{for(String mode:List.of("material","runtime","source","source-reconcile"))attempt(mode);});
        System.out.println("PASS"+checks+" TEST-JAR original-material/runtime/source/reconcile-save rollback contracts; original-attempts="+attempts+"; test SHA is not candidate SHA; backend and real IO remain unqualified");
    }
}
