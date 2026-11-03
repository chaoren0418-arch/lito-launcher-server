package local.lito381.server;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.io.*;
public final class CombatConfigOriginProbe {
 static int checks;static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
 static Path write(Path path,String text)throws Exception{Files.createDirectories(path.getParent());Files.writeString(path,text,StandardCharsets.UTF_8);return path;}
 static void rejected(Path p)throws Exception{try{NativeCombatConfigOrigin.entry(p);throw new AssertionError("accepted invalid origin");}catch(IOException expected){checks++;}}
 public static void main(String[]args)throws Exception{
  Path base=Path.of(args[0]).toRealPath(),dir=Path.of(args[1]).toAbsolutePath().normalize(),jar=Path.of(args[2]).toRealPath(),want=base.getParent().resolve("combat.properties");Files.createDirectories(dir);
  check(Path.of(NativeCombatConfigOrigin.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"candidate helper authority");check(NativeCombatConfigOrigin.entry(base).equals(want),"direct formal config same path");
  var abs=write(dir.resolve("中文目錄/absolute.properties"),"configuration.base.file="+base.toString().replace('\\','/')+"\ncapture.directory=outputs/captures\n");check(NativeCombatConfigOrigin.entry(abs).equals(want),"absolute base Unicode path");
  var rel=write(dir.resolve("relative.properties"),"configuration.base.file="+dir.relativize(base).toString().replace('\\','/')+"\n");check(NativeCombatConfigOrigin.entry(rel).equals(want),"relative existing base contract");
  var local=write(dir.resolve("legacy.properties"),"# legacy fixture without SQL keys\n");check(NativeCombatConfigOrigin.entry(local).equals(dir.resolve("combat.properties")),"legacy config keeps sibling path");
  rejected(dir.resolve("missing.properties"));rejected(write(dir.resolve("missing-base.properties"),"configuration.base.file=not-found.properties\n"));rejected(write(dir.resolve("invalid-overlay.properties"),"configuration.base.file="+base.toString().replace('\\','/')+"\nport=1\n"));rejected(write(dir.resolve("cycle.properties"),"configuration.base.file=cycle.properties\n"));
  for(int i=0;i<10;i++)check(NativeCombatConfigOrigin.entry(rel).equals(want),"stable origin does not mutate source");
  var cfg=ServerConfig.load(abs,"standard");CombatRules.load(cfg);check(CombatRules.loaded(),"actual combat loader accepts launcher overlay with formal SQL source list");
  String result="{\"status\":\"passed\",\"checks\":"+checks+",\"source_database_modified\":false}";Files.writeString(dir.resolve("observations.json"),result);System.out.println(result);
 }
}
