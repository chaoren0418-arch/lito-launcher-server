package local.lito381.server;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;
/** One readonly Source other.properties snapshot before the existing combat-rules publication. */
final class NativeSourceOtherControls {
 record Snapshot(NativeSourceDisplayPlan.Settings settings,Path file){}
 private static volatile Snapshot current;
 static CombatRules.Snapshot prepare(CombatRules.Snapshot rules)throws IOException{
  if(rules.characterFile()==null)throw new IOException("Source character settings path is required for other.properties");Path file=rules.characterFile().toAbsolutePath().normalize().resolveSibling("other.properties");var existing=current;if(existing!=null){if(!existing.file.equals(file))throw new IOException("Source other controls already belong to another startup");return rules;}
  var properties=new Properties();try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){properties.load(reader);}final NativeSourceDisplayPlan.Settings settings;try{settings=NativeSourceDisplayPlan.Settings.read(properties);}catch(NumberFormatException error){throw new IOException("Invalid Source presentation control in "+file,error);}current=new Snapshot(settings,file);return rules;
 }
 static Snapshot snapshot(){var value=current;if(value==null)throw new IllegalStateException("Source other controls not loaded");return value;}
 static NativeSourceDisplayPlan.Settings settings(){return snapshot().settings;}
 private NativeSourceOtherControls(){}
}
