// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/combat-config-origin/build-repro/inputs/NativeCombatConfigOrigin.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;import java.nio.file.Path;import java.util.Properties;
/** Reuse the existing single-base configuration contract; no SQL or fallback search. */
final class NativeCombatConfigOrigin {
 static Path entry(ServerConfig config)throws IOException{return entry(config.source());}
 static Path entry(Path source)throws IOException{
  Path supplied=source.toAbsolutePath().normalize();final Properties properties;
  try{properties=UnifiedConfiguration.read(supplied);}catch(IOException error){throw error;}catch(Exception error){throw new IOException("Cannot resolve combat configuration origin: "+supplied,error);}
  String base=properties.getProperty("configuration.base.file");Path owner=base==null?supplied:supplied.getParent().resolve(base).normalize();
  return owner.getParent().resolve("combat.properties");
 }
 private NativeCombatConfigOrigin(){}
}
