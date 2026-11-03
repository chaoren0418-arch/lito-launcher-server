package local.lito381.server;

/** Source awakening policy and SQL abilities; no retained actor state. */
final class CombatAwaken {
    private CombatAwaken() { }
    static boolean classicMode() {return CombatRules.number(CombatRules.AWAKE_CLASSIC_MODE,1)!=0;}
    static boolean classicActive(BattleAwakenStatus.Snapshot awakening) {
        return classicMode()&&awakening.active();
    }
    static int pollMpDrainTicks(BattleAwakenStatus status,long now) {
        return classicMode()?status.pollMpDrainTicks(now):0;
    }
    static boolean sameCasterContext(boolean admitted,ClientSession caster,BattleSpellCatalog.AwakenSpell spell) {
        if(!admitted)return false;
        var status=(BattleAwakenStatus)CombatAccess.field(caster,"awakenStatus");
        int active=status.snapshot().skillId();
        return active==0||active==spell.skillId();
    }
    static BattlePolymorphCatalog.Form clearOnToggle(BattlePolymorphStatus status,
            BattleSpellCatalog.AwakenSpell spell,BattleAwakenStatus.Toggle toggle) {
        if(classicMode())return status.clear();
        // The relaxed source only undoes a genuine dragon form on stopSkill;
        // an unrelated item polymorph survives both activation and deactivation.
        var form=status.active(System.nanoTime());
        return toggle.deactivated()&&form!=null&&form.gfx()==spell.dragonGfx()?status.clear():null;
    }
    static int ability(BattleAwakenStatus.Snapshot awakening,String type) {
        if(!awakening.active())return 0;
        var controls=CombatDatabase.controlsOrNull();
        if(controls!=null)return SourceSkillControls.ability(controls,awakening.skillId(),type);
        return switch(type) {
            case "AC" -> awakening.armorClassModifier();
            case "HP" -> awakening.maximumHpBonus();
            case "STR","DEX","CON","INT","WIS","CHA" -> awakening.allAbilityScoreBonus();
            default -> throw new IllegalArgumentException("unbound awakening ability="+type);
        };
    }
}
