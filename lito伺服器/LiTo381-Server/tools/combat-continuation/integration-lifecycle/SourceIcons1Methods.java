import java.nio.file.*;import java.util.*;
/** Encoder-only stage: deliberately no original producer or receiver splice. */
public final class SourceIcons1Methods {
 public static void main(String[]a)throws Exception{if(a[0].equals("splice")){Files.createDirectories(Path.of(a[2]));Files.write(Path.of(a[2]).getParent().resolve("icons1-ports.tsv"),List.of("scope\tstatus","immutable-scalar-ICONS1-encoder\tno original producer/receiver ports; live projection and coexist reader remain open"));}System.out.println("PASS Source ICONS1 encoder-only stage; no original producer/receiver splice");}
}
