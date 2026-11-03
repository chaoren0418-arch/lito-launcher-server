package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Static rows from the existing readonly startup snapshot only; no Hub/listener/player repository. */
public final class FreezeSourceMetadataProbe {
 public static void main(String[]a)throws Exception{ResourceDatabase.load(ServerConfig.load(Path.of(a[0]),"standard"));var out=new ArrayList<String>();for(int id:new int[]{17,50,80,194})out.add(id+"\t"+NativeSkillTargetMetadata.row(id));Files.write(Path.of(a[1]),out);System.out.println(String.join("\n",out));}
}
