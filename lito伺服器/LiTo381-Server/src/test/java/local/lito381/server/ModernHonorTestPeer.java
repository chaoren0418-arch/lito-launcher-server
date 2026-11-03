package local.lito381.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Owned v13 Honor TLS peer. Real shared532 geometry; deliberately compact
 * fixture camps, NOT claimed as recovered native tower/spawn coordinates. */
public final class ModernHonorTestPeer {
    static final ModernWorldMap.Cell ANCHOR=new ModernWorldMap.Cell(32796,32839);
    static final List<ModernWorldMap.Cell> SPAWNS=List.of(new ModernWorldMap.Cell(32792,32843),new ModernWorldMap.Cell(32798,32843));
    private ModernHonorTestPeer(){}
    static ModernHonor.Config config(){return new ModernHonor.Config(List.of(
        new ModernHonor.Tower(0,new ModernWorldMap.Cell(32792,32842),2665,3000),
        new ModernHonor.Tower(1,new ModernWorldMap.Cell(32798,32842),2669,3000)),SPAWNS);}
    static ModernWorldMap qualified532(Path root)throws Exception{
        var sectors=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int i=0;i<6;i++)sectors.add(new ModernWorldMap.SectorManifest(
            new ModernWorldMap.SectorKey(0x7fff+i/2,0x8000+i%2),ModernMap532ReadProbe.LENGTHS[i],ModernMap532ReadProbe.HASHES[i]));
        var expected=new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(5,532),
            new ModernWorldMap.Manifest(532,sectors,"b483dc9c8fefefcdbd515680317fc5e44090046f6490e8d165f5f989bf6d5fdc"),ANCHOR,100);
        var map=ModernWorldMapQualification.load(root,expected,()->false).map();
        config().validate(new ModernWorldBinding(5,532,map));return map;
    }
    /** Actual qualified form, explicit synthetic INT table; does not query a source DB. */
    static ModernPhysicalCombat.Source healingSource(ModernPhysicalCombat.Source source){
        // Deliberately qualified synthetic +100 ranged armor bonus keeps the
        // 3000-HP objective run bounded WITHOUT accelerating server cadence.
        // This is not a claim about real-source gear or production balance.
        if(source.input().equipment().source().sourceId()==7){
            var old=source.input().equipment();var item=old.items().get(7000L);var t=item.template();
            var replacement=new ModernEquipmentFacts.Instance(item.source(),new ModernEquipmentFacts.Template(
                t.metadata(),t.rules(),t.retainedType(),t.safeEnchant(),t.setIds(),
                t.base().with(ModernEquipmentFacts.Stat.RANGED_DAMAGE,100),t.physical()),
                item.greater(),item.dark(),item.cards(),item.stone());
            var instances=new java.util.HashMap<>(old.items());instances.put(7000L,replacement);
            var q=new ModernEquipmentFacts.Qualified(old.source(),instances,old.catalog(),old.lifetimes());
            source=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,source.input().weaponsQualified(),source.input().weapons()),
                source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth());
        }
        var rows=new ArrayList<ModernHonorHealing.Row>();for(int i=1;i<=127;i++)rows.add(new ModernHonorHealing.Row(i,0));
        var growth=new ModernHonorHealing.Growth(source.input().equipment().source().sourceId(),
            source.input().equipment().catalog().resourceDatabase(),rows);
        return new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
            source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),growth);
    }
    public static void main(String[] args){
        if(args.length!=4||!(args[2].equals("honor")||args[2].equals("life-honor")||args[2].equals("canonical-honor")))throw new IllegalArgumentException();
        ModernCombatTestPeer.main(args);
    }
}
