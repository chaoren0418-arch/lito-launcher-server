from pathlib import Path
R=Path(__file__).resolve().parent;P=R.parent/'resurrection-companion'
t=(P/'NativeResurrectionMaterialAction.java').read_text('utf-8')
t=t.replace('private static final ThreadLocal<Target> current=new ThreadLocal<>();','private static final ThreadLocal<Target> current=new ThreadLocal<>();\n private static final ThreadLocal<Target> ingress=new ThreadLocal<>();')
start=t.index(' private static void run(');end=t.index(' static boolean admit(',start)
t=t[:start]+''' static void dispatch(ClientSession pc,byte[] packet,NativeSelfCastAction.Task original)throws IOException{
  int skill=Protocol.spellSelector(packet).skillId();String phase=String.valueOf(CombatAccess.field(pc,"phase"));
  if((skill!=61&&skill!=75)||(!phase.equals("WORLD")&&!phase.equals("BATTLE"))||pc.selectedCharacterName().isBlank()){original.run();return;}
  var prior=ingress.get();ingress.set(target(pc,packet,phase.equals("WORLD")));
  try{original.run();}finally{if(prior==null)ingress.remove();else ingress.set(prior);}
 }
 private static void run(ClientSession pc,byte[] packet,boolean world,NativeSelfCastAction.Task body)throws IOException{
  var inherited=ingress.get();var target=inherited!=null&&inherited.caster==pc?inherited:target(pc,packet,world);
  // Dispatcher metadata must not escape into deferred output or nested callbacks.
  ingress.remove();
  try{NativeLightAction.commitPair(pc,target.owner,()->{
   if(!target.valid())return;
   var previous=current.get();current.set(target);
   try{body.run();}finally{if(previous==null)current.remove();else current.set(previous);}
  });}finally{if(inherited==null)ingress.remove();else ingress.set(inherited);}
 }
 static void battle(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,packet,false,()->pc.nativeMaterialResurrectionBody(packet,spell));
 }
 static void world(ClientSession pc,byte[] packet,BattleSpellCatalog.ResurrectionSpell spell)throws IOException{
  run(pc,packet,true,()->pc.nativeMaterialWorldResurrectionBody(packet,spell));
 }
'''+t[end:]
(R/'NativeResurrectionMaterialAction.java').write_text(t,encoding='utf-8')
t=(R.parent/'resurrection-material/build/inputs/sources/NativeLightAction.java').read_text('utf-8')
needle=' static void dispatch(ClientSession pc,byte[] packet)throws IOException{'
assert t.count(needle)==1
t=t.replace(needle,needle+'\n  NativeResurrectionMaterialAction.dispatch(pc,packet,()->dispatchResurrectionOriginal(pc,packet));\n }\n private static void dispatchResurrectionOriginal(ClientSession pc,byte[] packet)throws IOException{')
(R/'NativeLightAction.java').write_text(t,encoding='utf-8')
