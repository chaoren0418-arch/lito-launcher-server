package local.lito381.server;

import com.sun.nio.file.ExtendedOpenOption;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Separate fixture admission: an explicit pinned Native source, never a Shared lookup fallback. */
public final class NativeWorldResourceCandidate {
    static final String NAMESPACE="NATIVE_LITO_LTMAP_V1";
    static final String ID="native-dm564-geometry.20261004.1";
    record Candidate(String id,String namespace,ModernWorldBinding binding,ModernWorldMap.Cell anchor,
                     List<ModernWorldMap.Cell> originalSpawns,int modernCapacity) {}
    static Properties config(Path path)throws Exception {
        var p=new Properties();try(var r=Files.newBufferedReader(path,StandardCharsets.UTF_8)){p.load(r);}return p;
    }
    static Candidate load(Properties p,BooleanSupplier cancelled)throws Exception {
        require(NAMESPACE.equals(p.getProperty("source.namespace")),"NAMESPACE");
        require(ID.equals(p.getProperty("candidate.id")),"CANDIDATE_ID");
        require("564".equals(p.getProperty("map.id"))&&"1".equals(p.getProperty("mode.id")),"BINDING");
        require("7fff8000".equals(p.getProperty("sectors")),"SELECTION");
        require("100".equals(p.getProperty("modern.capacity")),"MODERN_POLICY");
        Path core=Path.of(ModernWorldMap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        require(Files.isRegularFile(core)&&NativeTerrainProbeMain.hash(Files.readAllBytes(core)).equalsIgnoreCase(p.getProperty("parser.jar.sha256")),"PARSER_ARTIFACT");
        Path root=Path.of(p.getProperty("source.root")).toAbsolutePath().normalize();
        require(root.getFileName().toString().equalsIgnoreCase("LTmap")&&root.getParent().getFileName().toString().equalsIgnoreCase("LiTo"),"SOURCE_LAYOUT");
        require(root.toRealPath().equals(root),"SOURCE_ALIAS");
        var key=new ModernWorldMap.SectorKey(0x7fff,0x8000);
        Path file=root.resolve("564").resolve(key.fileName());
        long expected=Long.parseLong(p.getProperty("sector.7fff8000.bytes"));
        require(expected>0&&expected<=ModernWorldMap.MAX_SECTOR_BYTES,"SIZE_BUDGET");
        ModernServerConfig.checkedLocalPath(file,true);
        require(file.toRealPath().equals(file),"SOURCE_ALIAS");
        BasicFileAttributes before=Files.readAttributes(file,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        require(before.isRegularFile()&&before.size()==expected,"PINNED_SIZE");
        Set<OpenOption> options=new HashSet<>(Set.of(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS));
        boolean windows="\\".equals(file.getFileSystem().getSeparator());
        if(windows){options.add(ExtendedOpenOption.NOSHARE_WRITE);options.add(ExtendedOpenOption.NOSHARE_DELETE);}
        byte[] data=new byte[(int)expected];
        try(var ch=FileChannel.open(file,options)) {
            require(ch.size()==expected,"CHANGED_FILE");var buf=ByteBuffer.wrap(data);
            while(buf.hasRemaining()){active(cancelled);require(ch.read(buf)>0,"CHANGED_FILE");}
            active(cancelled);require(ch.size()==expected&&ch.read(ByteBuffer.allocate(1))==-1,"CHANGED_FILE");
            ModernServerConfig.checkedLocalPath(file,true);
            var after=Files.readAttributes(file,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            require((windows||before.fileKey()!=null)&&Objects.equals(before.fileKey(),after.fileKey())
                    &&before.size()==after.size()&&before.lastModifiedTime().equals(after.lastModifiedTime()),"CHANGED_FILE");
        }
        require(NativeTerrainProbeMain.hash(data).equalsIgnoreCase(p.getProperty("sector.7fff8000.sha256")),"RAW_PIN");
        var map=ModernWorldMap.fromSectorBytes(564,Map.of(key,data),cancelled);
        require(map.digest().equalsIgnoreCase(p.getProperty("canonical.sha256")),"CANONICAL_PIN");
        var original=ArenaMapCatalog.forMap(564).spawns().stream().map(s->new ModernWorldMap.Cell(s.x(),s.y())).toList();
        map.validateSpawns(original);map.validateSpawns(map.serverChosenSpawns(original.get(0),100));
        return new Candidate(ID,NAMESPACE,new ModernWorldBinding(1,564,map),original.get(0),original,100);
    }
    private static void active(BooleanSupplier cancelled){require(!cancelled.getAsBoolean()&&!Thread.currentThread().isInterrupted(),"CANCELLED");}
    private static void require(boolean pass,String failure){if(!pass)throw new IllegalArgumentException(failure);}
}
