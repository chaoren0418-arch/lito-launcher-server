package local.lito381.server;
import java.nio.file.*;import java.util.*;
public class EquipmentRegression {
 static Object get(Object o,String n)throws Exception{return NativeDirectoryRegression.get(o,n);}
 static void set(Object o,String n,Object v)throws Exception{NativeDirectoryRegression.set(o,n,v);}
 static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{return NativeDirectoryRegression.call(o,n,t,a);}
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static void phase(ClientSession s,String n)throws Exception{set(s,"phase",Enum.valueOf((Class)get(s,"phase").getClass(),n));}
 static void select(ClientSession s,String name)throws Exception{phase(s,"LOBBY");set(s,"litoCharacterSelectionConsumed",false);call(s,"characterSelect",new Class[]{byte[].class},new Protocol.Writer().c(0xc9).s(name).bytes());}
 static AccountRepository readOnlyAccounts(ServerConfig c)throws Exception {
  var routes=new LinkedHashMap<String,AccountRepository>();var settings=SharedDatabase.properties(c.source());var credentials=SharedDatabase.properties(c.legacySqlProperties());
  for(int i=0;i<c.classicServerNames().size();i++){
   String name=c.classicServerNames().get(i),db=c.legacySourceDatabases().get(i);
   var repo=new LegacyMySqlRepository(c.legacySqlProperties(),name,c.classicServerIds().get(i),db,c.legacySourceResourceDatabases().get(i),c.legacySourceWeightRates().get(i),true,"legacy_mysql");
   SharedDatabase.ROUTES.put(repo,new SharedDatabase.Route(name,db,settings.getProperty("database.lito.database"),credentials));AccountRepository traced=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(EquipmentRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(proxy,method,args)->{
    if(method.getName().equals("repositoryForClassicSource"))return proxy;
    if(method.getName().equals("equipmentEvaluation")&&args.length==2&&failEvaluation.get())throw new java.sql.SQLException("injected evaluation failure");
    try{return method.invoke(repo,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
   });routes.put(name,traced);
  }
  return new SourceRoutedAccountRepository(routes);
 }
 static final java.util.concurrent.atomic.AtomicBoolean failEvaluation=new java.util.concurrent.atomic.AtomicBoolean();
 static List<byte[]> use(SourceLoginRegression.Fixture f,Protocol.CipherState cipher,long id)throws Exception{
  call(f.session,"useInventoryItem",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_ITEM).d(id).s("").bytes());return NativeDirectoryRegression.drain(f,cipher);
 }
 static void marker(List<byte[]> frames,RuntimeInventory.Item item)throws Exception{
  byte[] expected=Protocol.changeInventoryDescription(item.objectId(),item.nativeViewName());var descriptions=frames.stream().filter(b->b[0]==expected[0]&&new Protocol.Reader(java.util.Arrays.copyOfRange(b,1,b.length)).d()==item.objectId()).toList();
  check(descriptions.size()==1,"expected one final equipment marker, got "+descriptions.size());check(Arrays.equals(expected,descriptions.get(0)),"published speculative equipment state");
 }
 public static void main(String[] a)throws Exception{
  var config=ServerConfig.load(Path.of(a[0]),"standard");var root=Path.of(a[1]);Files.createDirectories(root);
  try(var accounts=readOnlyAccounts(config);var capture=new CaptureLog(root.resolve("captures"))){
   var repo=accounts.repositoryForClassicSource(config.classicServerNames().get(0));var chars=repo.characters("3333");
   var ch=chars.stream().filter(c->c.name().equals("3333")).findFirst().orElseThrow();var original=repo.sourceInventory(ch.objectId());
   var state=new GameStateStore(root.resolve("unused"),false,true);
   try(var hub=new GameHub(config,state,accounts);var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var s=f.session;set(s,"account","3333");set(s,"characters",chars);set(s,"originalCharacterCount",chars.size());set(s,"selectedClassicServerName",config.classicServerNames().get(0));f.client.setSoTimeout(50);var cipher=new Protocol.CipherState(0x37110001);
    select(s,ch.name());NativeDirectoryRegression.drain(f,cipher);
    var bindings=Map.copyOf((Map<?,?>)get(s,"sourceEquipmentObjects"));check(!bindings.isEmpty(),"first selection has no equipment bindings");
    System.out.println("First selection mappings="+bindings.size());
    select(s,ch.name());NativeDirectoryRegression.drain(f,cipher);
    var again=(Map<?,?>)get(s,"sourceEquipmentObjects");System.out.println("Same-character selection mappings="+again.size());
    var inv=(RuntimeInventory)get(s,"inventory");var weapon=inv.items().stream().filter(i->i.sourceBound()&&i.definition().weapon()&&i.equipped()).findFirst().orElseThrow();
    phase(s,"CHANNEL");var frames=use(f,cipher,weapon.objectId());
    var live=((RuntimeInventory)get(s,"inventory")).item(weapon.objectId());System.out.println("After one unequip request equipped="+live.equipped()+" frames="+frames.size());
    check(bindings.equals(again),"same-character selection erased source equipment bindings");check(!live.equipped(),"one unequip request rolled back");
    marker(frames,live);
    // Repeated selection must also retain current evaluated stats after taking gear off.
    var stats=get(s,"sourceEquipmentMagicStats");select(s,ch.name());NativeDirectoryRegression.drain(f,cipher);check(stats.equals(get(s,"sourceEquipmentMagicStats")),"reselect restored original equipped stats");phase(s,"CHANNEL");
    for(int i=0;i<4;i++){boolean before=((RuntimeInventory)get(s,"inventory")).item(weapon.objectId()).equipped();var packets=use(f,cipher,weapon.objectId());var after=((RuntimeInventory)get(s,"inventory")).item(weapon.objectId());check(after.equipped()!=before,"weapon toggle rejected");marker(packets,after);}
    var armor=((RuntimeInventory)get(s,"inventory")).items().stream().filter(i->i.sourceBound()&&i.definition().sourceArmor()&&i.equipped()&&i.definition().sourceArmorStatus().armorType()==4).findFirst().orElseThrow();
    for(int i=0;i<4;i++){boolean before=((RuntimeInventory)get(s,"inventory")).item(armor.objectId()).equipped();var packets=use(f,cipher,armor.objectId());var after=((RuntimeInventory)get(s,"inventory")).item(armor.objectId());check(after.equipped()!=before,"armor toggle rejected");marker(packets,after);}
    failEvaluation.set(true);
    for(long id:new long[]{weapon.objectId(),armor.objectId()}){var before=(RuntimeInventory)get(s,"inventory");var packets=use(f,cipher,id);check(before==get(s,"inventory"),"failed evaluation committed inventory");marker(packets,before.item(id));}
    failEvaluation.set(false);
    // Different characters must get their own source object mappings, never the retained first character's.
    var other=chars.stream().filter(c->!c.name().equals(ch.name())).findFirst().orElseThrow();select(s,other.name());NativeDirectoryRegression.drain(f,cipher);var otherBindings=(Map<?,?>)get(s,"sourceEquipmentObjects");check(!otherBindings.equals(bindings),"character switch reused previous bindings");
    select(s,ch.name());NativeDirectoryRegression.drain(f,cipher);check(!((Map<?,?>)get(s,"sourceEquipmentObjects")).isEmpty(),"switch back lost bindings");
    check(original.equals(repo.sourceInventory(ch.objectId())),"source database inventory mutated");
    System.out.println("PASS: real C_USE_ITEM weapon/armor toggles; exactly one final marker per request; same-character selection preserves bindings and evaluated stats; other-character isolation; injected SQL failures never publish candidate state; source inventory unchanged");
   }
  }
 }
}
