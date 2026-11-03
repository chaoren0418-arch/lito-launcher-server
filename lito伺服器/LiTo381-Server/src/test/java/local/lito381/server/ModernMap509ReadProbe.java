package local.lito381.server;

import java.nio.file.Path;
import java.util.Set;

/** Read-only actual shared map exploration; no SQL, listener, LT fallback or UI. */
public final class ModernMap509ReadProbe {
    private ModernMap509ReadProbe() {}
    static ModernWorldMap read(Path root) throws Exception {
        var key = new ModernWorldMap.SectorKey(0x7fff, 0x8000);
        var map = ModernWorldMap.load(root, 509, Set.of(key), () -> false);
        var source = map.manifest().sectors().get(0);
        if (source.byteLength() != 106355 || !source.rawSha256().equals(
                "8580337a4ad3593d697a416062c4da063f8fd6c2ba311f0b694fcfc59710cb9d"))
            throw new IllegalStateException("shared 509 raw witness changed");
        return map;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !Path.of(args[0]).isAbsolute()) throw new IllegalArgumentException();
        var map = read(Path.of(args[0]));
        int standing = map.teleportCells().size();
        var origin = map.manifest().sectors().get(0).key().origin();
        var masks = new byte[4096];
        for (int y=0;y<64;y++) for (int x=0;x<64;x++)
            masks[y*64+x]=(byte)map.directionMask(new ModernWorldMap.Cell(origin.x()+x,origin.y()+y));
        var anchor = new ModernWorldMap.Cell(32740,32796);
        var spawns = map.serverChosenSpawns(anchor,100);
        map.validateSpawns(spawns);
        System.out.println("map="+map.mapId()+" bytes=106355 canonical="+map.digest()+" standing="+standing
                +" masks="+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(masks))
                +" anchor="+anchor+" mask="+map.directionMask(anchor)+" second="+spawns.get(1)+" spawns="+spawns.size());
    }
}
