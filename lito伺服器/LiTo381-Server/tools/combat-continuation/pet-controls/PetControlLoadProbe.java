package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
/** Actual immutable configuration publication only; no SQL, sockets, actors or clocks. */
public final class PetControlLoadProbe {
 static int checks;
 static void need(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
 static Properties read(Path p)throws Exception{var v=new Properties();try(var r=Files.newBufferedReader(p)){v.load(r);}return v;}
 static void write(Path p,Properties v)throws Exception{try(var out=Files.newBufferedWriter(p)){v.store(out,"isolated read-only source configuration fixture");}}
 static ServerConfig source(ServerConfig old,Path p)throws Exception{var fields=ServerConfig.class.getRecordComponents();Class<?>[]types=new Class<?>[fields.length];Object[]args=new Object[fields.length];for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();args[i]=fields[i].getName().equals("source")?p:fields[i].getAccessor().invoke(old);}var ctor=ServerConfig.class.getDeclaredConstructor(types);ctor.setAccessible(true);return ctor.newInstance(args);}
 public static void main(String[]args)throws Exception{
  var server=Path.of(args[0]);var root=Path.of(args[1]);Files.createDirectories(root);var jar=Path.of(args[2]).toRealPath();for(var cls:List.of(CombatRules.class,NativePetControls.class))need(Path.of(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate configuration and consumer helper");
  var paths=read(server.getParent().resolve("combat.properties"));for(String key:List.of("source.skill.file","source.damage.file","source.character.file","source.map.directory")){paths.setProperty(key,server.getParent().resolve(paths.getProperty(key)).normalize().toString());}
  var character=read(Path.of(paths.getProperty("source.character.file")));Path fixture=root.resolve("server.properties"),chars=root.resolve("character.properties");Files.writeString(fixture,"# no database or listener opened\ndatabase.mode=memory\n");paths.setProperty("source.character.file",chars.toString());write(root.resolve("combat.properties"),paths);var cfg=ServerConfig.load(fixture);var current=CombatRules.class.getDeclaredField("current");current.setAccessible(true);
  for(int mode=0;mode<3;mode++){
   current.set(null,null);var p=new Properties();p.putAll(character);for(String key:NativePetControls.KEYS)p.remove(key);if(mode==1)for(int i=0;i<7;i++)p.setProperty(NativePetControls.KEYS.get(i),""+(i*3-5));if(mode==2)p.setProperty("ElfChaForPet","invalid");write(chars,p);
   if(mode==2){boolean rejected=false;try{CombatRules.load(cfg);}catch(java.io.IOException expected){rejected=true;}need(rejected&&current.get(null)==null,"invalid pet setting rejects whole snapshot before publication");p.setProperty("ElfChaForPet","19");write(chars,p);}
   CombatRules.load(cfg);var snapshot=(CombatRules.Snapshot)current.get(null);var expected=NativePetControls.parse(p);for(int i=0;i<7;i++){String key=NativePetControls.KEYS.get(i);need(snapshot.values().get(key)==expected.get(i).doubleValue(),"actual immutable snapshot key");need(NativePetControls.configuredBonus(i)==expected.get(i),"actual configured consumer reads key");need(snapshot.defaultedKeys().contains(key)==!p.containsKey(key),"default provenance retained");}
   need(NativePetControls.configuredBonus(-1)==0&&NativePetControls.configuredBonus(7)==0,"unknown class remains no bonus");
   try{snapshot.values().put("ElfChaForPet",999.0);throw new AssertionError("mutable snapshot");}catch(UnsupportedOperationException expectedError){checks++;}
   Object retained=current.get(null);p.setProperty("ElfChaForPet","222");write(chars,p);CombatRules.load(cfg);need(current.get(null)==retained&&NativePetControls.configuredBonus(2)==expected.get(2),"restart-only immutable snapshot; no per-cast file rereads");
  }
  Files.writeString(root.resolve("observations.json"),"{\"cases\":3,\"checks\":"+checks+",\"sql_connections\":0,\"listeners\":0,\"native_actor_tests\":false}\n");
 }
}
