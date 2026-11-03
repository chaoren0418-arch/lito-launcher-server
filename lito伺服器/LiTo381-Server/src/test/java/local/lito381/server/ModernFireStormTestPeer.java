package local.lito381.server;

/** New v14 fixture modes only. Old peer versions, source SQL and shared files remain untouched. */
public final class ModernFireStormTestPeer {
    private ModernFireStormTestPeer(){}
    static ModernPhysicalCombat.Source fireSource(ModernPhysicalCombat.Source source,boolean sun){
        var old=source.input().equipment();var item=old.items().get(7000L);var t=item.template();
        var replacement=new ModernEquipmentFacts.Instance(item.source(),new ModernEquipmentFacts.Template(t.metadata(),t.rules(),
                t.retainedType(),t.safeEnchant(),t.setIds(),t.base().with(ModernEquipmentFacts.Stat.MAGIC_DAMAGE,sun?1000:0),t.physical()),
                item.greater(),item.dark(),item.cards(),item.stone());
        var instances=new java.util.HashMap<>(old.items());instances.put(7000L,replacement);
        var q=new ModernEquipmentFacts.Qualified(old.source(),instances,old.catalog(),old.lifetimes());
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,source.input().weaponsQualified(),source.input().weapons()),
                source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth());
    }
    public static void main(String[] args){
        if(args.length!=4||!java.util.Set.of("fire","fire-honor","life-fire").contains(args[2]))throw new IllegalArgumentException();
        ModernCombatTestPeer.main(args);
    }
}
