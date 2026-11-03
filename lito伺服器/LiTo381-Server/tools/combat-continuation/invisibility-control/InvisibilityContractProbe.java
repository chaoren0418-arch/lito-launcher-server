package local.lito381.server;
import java.nio.file.*;import java.util.*;
public final class InvisibilityContractProbe extends NativeMagicalWeaponProbe {
 public static void main(String[] args)throws Exception {
  var cfg=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(cfg);var f=CombatRules.class.getDeclaredField("current");f.setAccessible(true);var original=(CombatRules.Snapshot)f.get(null);int parsers=0,toggles=0;
  var skill=new Properties();skill.putAll(CombatRules.read(original.skillFile()));var damage=CombatRules.read(original.damageFile());
  for(String raw:new String[]{null,""," ","true","TRUE"," true ","false","FALSE","0","1","yes","invalid"}){
   if(raw==null)skill.remove("INVIS_CAST_BREAK");else skill.setProperty("INVIS_CAST_BREAK",raw);var next=CombatRules.snapshot(skill,damage,original.skillFile(),original.damageFile());
   need(next.values().get("INVIS_CAST_BREAK")== (SourceInvisibilityReference.enabled(skill)?1.0:0.0),"compiled Source boolean parser");need(next.defaultedKeys().contains("INVIS_CAST_BREAK")== (raw==null||raw.isBlank()),"default provenance");parsers++;
  }
  var root=Path.of(args[1]);Files.createDirectories(root);var state=new GameStateStore(root.resolve("must-not-write"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var fixture=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   var pc=fixture.session;
   for(boolean enabled:new boolean[]{false,true})for(int id:new int[]{0,20077,120077,20062,20000})for(boolean equipped:new boolean[]{false,true})for(boolean same:new boolean[]{false,true}){
    var values=new HashMap<>(original.values());values.put("INVIS_CAST_BREAK",enabled?1.0:0.0);f.set(null,copy(original,Map.of("values",values)));var inv=new RuntimeInventory();
    if(id!=0){var def=copy(InventoryCatalog.require(InventoryCatalog.SHORT_SWORD),Map.of("catalogId",(long)id));need(inv.add(new RuntimeInventory.Item(800001,def,1,equipped),1),"isolated equipment identity fixture");}set(pc,"inventory",inv);
    boolean source=enabled&&SourceInvisibilityReference.shouldToggleOffInvisibilityRecast(60,same,false,equipped&&(id==20077||id==120077||id==20062));need(CombatInvisibilityRules.toggle(same,pc)==source,"actual equipment toggle vs compiled Source rule");toggles++;
   }
  }finally{f.set(null,original);NativePlayerCommit.closeFixture();}
  need(NativePlayerLifetime.allIdle(),"released lifetimes");need(!Files.exists(root.resolve("must-not-write")),"memory-only");String result="{\"parser_cases\":"+parsers+",\"toggle_cases\":"+toggles+",\"checks\":"+ownChecks+"}";Files.writeString(Path.of(args[2]),result);System.out.println(result);
 }
}
