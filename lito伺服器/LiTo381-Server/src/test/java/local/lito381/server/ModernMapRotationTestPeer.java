package local.lito381.server;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Test-classpath only; existing owned combat peer, two explicitly pinned shared maps. */
public final class ModernMapRotationTestPeer {
    static final ModernWorldMap.Cell ANCHOR509 = new ModernWorldMap.Cell(32740,32796);
    private ModernMapRotationTestPeer() {}
    static ModernWorldMapQualification.Expected expected(int id, int length, String raw, String canonical,
                                                        ModernWorldMap.Cell anchor) {
        return new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(1,id),
                new ModernWorldMap.Manifest(id,List.of(new ModernWorldMap.SectorManifest(
                        new ModernWorldMap.SectorKey(0x7fff,0x8000),length,raw)),canonical),anchor,100);
    }
    static ModernWorldMap qualified509(Path root) throws Exception {
        return ModernWorldMapQualification.load(root,expected(509,106355,
                "8580337a4ad3593d697a416062c4da063f8fd6c2ba311f0b694fcfc59710cb9d",
                "1178261c2d6a968251f2eadfb2029bd0febade95b7151c15872f355784df87f8",ANCHOR509),()->false).map();
    }
    static void qualify514(ModernWorldMap map) throws Exception {
        ModernWorldMapQualification.qualify(map,expected(514,137784,ModernWorldTestPeer.RAW_SHA,
                ModernWorldTestPeer.MANIFEST_SHA,ModernWorldTestPeer.ANCHOR));
    }
    static Map<ModernWorldBinding.Key,ModernWorldService.Mapping> sharedArenas(Path root)throws Exception {
        return sharedArenas(root,"rotation-shared");
    }
    /** Independent test BFS, not the production teleport candidate builder. */
    static List<ModernWorldMap.Cell> arenaDestinations(ModernWorldMap map,List<ModernWorldMap.Cell> spawns) {
        var arena=ModernDeathmatchSharedMapTest.components(map).stream().filter(c->c.contains(spawns.get(0))).findFirst().orElseThrow();
        return map.teleportCells().stream().filter(c->arena.contains(c)&&!spawns.contains(c)).toList();
    }
    static int adjacentChoice(ModernWorldMap map,List<ModernWorldMap.Cell> spawns,List<ModernWorldMap.Cell> destinations) {
        for(int i=0;i<destinations.size();i++)if(map.canStep(spawns.get(0),destinations.get(i))&&map.canStep(destinations.get(i),spawns.get(0)))return i;
        throw new IllegalStateException("fixture has no reversible adjacent teleport");
    }
    static List<Integer> sharedIds(String mode) {
        return switch(mode) {
            case "rotation-shared" -> List.of(551,552);
            case "rotation-shared-508-531" -> List.of(508,531);
            case "rotation-shared-531-534" -> List.of(531,534);
            case "rotation-shared-516-531" -> List.of(531,516);
            case "rotation-shared-504-531" -> List.of(531,504);
            case "rotation-shared-505-531" -> List.of(531,505);
            case "rotation-shared-506-531" -> List.of(531,506);
            default -> throw new IllegalArgumentException("unselected shared rotation");
        };
    }
    static boolean isSharedRotation(String mode) {
        return java.util.Set.of("rotation-shared","rotation-shared-508-531","rotation-shared-531-534","rotation-shared-516-531","rotation-shared-504-531","rotation-shared-505-531","rotation-shared-506-531").contains(mode);
    }
    static Map<ModernWorldBinding.Key,ModernWorldService.Mapping> sharedArenas(Path root,String mode)throws Exception {
        var ids=sharedIds(mode);var p=ModernSharedDeathmatchPresetTest.worlds(root,ids);
        for(int id:ids){String prefix=ModernSharedDeathmatchPreset.prefix(id);
            p.remove(prefix+"botRules");p.setProperty(prefix+"attackMillis","1000");
            p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        }
        return ModernWorldConfiguration.parse(p).load();
    }
    public static void main(String[] args) {
        if (args.length!=4 || !(args[2].equals("rotation")||isSharedRotation(args[2]))) throw new IllegalArgumentException();
        ModernCombatTestPeer.main(args);
    }
}
