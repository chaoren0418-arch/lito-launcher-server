package local.lito381.server;

/** v27 TLS fixture: synthetic authenticated source qualification, shared Lineage
 * map bytes, real application session and isolated SQL match settlement. */
public final class ModernDisintegrateTestPeer {
    private ModernDisintegrateTestPeer(){}
    static ModernPhysicalCombat.Source lawfulSource(ModernPhysicalCombat.Source s){
        var q=s.input().equipment();var old=q.source().persisted();
        var values=new ModernCharacterData.PersistedValues(old.maxHp(),old.maxMp(),old.savedHp(),old.savedMp(),old.armorClass(),
                old.strength(),old.dexterity(),old.constitution(),old.intelligence(),old.wisdom(),old.charisma(),old.experience(),500,
                old.originalStrength(),old.originalDexterity(),old.originalConstitution(),old.originalIntelligence(),old.originalWisdom(),old.food225());
        var row=new ModernCharacterData.Snapshot(q.source().sourceId(),q.source().character(),values,q.source().inventory());
        var qualified=new ModernEquipmentFacts.Qualified(row,q.items(),q.catalog(),q.lifetimes());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,s.input().weaponsQualified(),s.input().weapons()),
                s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth());
    }
    public static void main(String[] args){
        if(args.length!=4||!(args[2].equals("disintegrate")||args[2].equals("life-disintegrate")||args[2].equals("canonical-disintegrate")))throw new IllegalArgumentException();
        ModernCombatTestPeer.main(args);
    }
}
