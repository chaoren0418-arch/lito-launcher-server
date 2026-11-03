"""Prepare two exact material roots; NPC replacement races are a separate gate."""
from pathlib import Path
R=Path(__file__).resolve().parent
T=R/'pet-material';T.mkdir(exist_ok=True)
def replace_once(s,a,b):
 assert s.count(a)==1,(a,s.count(a))
 return s.replace(a,b)
s=(R/'earth-bind-material/EarthBindMaterialMethods.java').read_text('utf-8')
s=s.replace('EarthBindMaterialMethods','PetMaterialMethods').replace('NativeEarthBindMaterialAction','NativePetMaterialAction')
start=s.index(' static final List<Spec> SPECS=');end=s.index('\n',start)
s=s[:start]+' static final List<Spec> SPECS=List.of(new Spec("useTamingSpell","nativeMaterialTamingBody","taming","TamingSpell",37,40,32,35,0,5),new Spec("useCreateZombieSpell","nativeMaterialZombieBody","zombie","CreateZombieSpell",34,39,29,32,0,5));'+s[end:]
s=s.replace('two negative roots inverse','two pet material roots inverse')
s=replace_once(s,'int monitors=0;','int monitors=0,retainedMonitors=0;')
s=replace_once(s,'if(!(prev(n) instanceof VarInsnNode v)||(v.var!=s.monitor&&v.var!=s.monitor+1))throw new AssertionError("unknown monitor");m.instructions.set(n,new InsnNode(POP));monitors++;','if(!(prev(n) instanceof VarInsnNode v))throw new AssertionError("unknown monitor operand");if(s.name.equals("useCreateZombieSpell")&&v.var==40){retainedMonitors++;}else{if(v.var!=s.monitor)throw new AssertionError("unknown monitor "+s.name+" local="+v.var);m.instructions.set(n,new InsnNode(POP));monitors++;}')
s=replace_once(s,'if(monitors!=s.monitorCount||','if(retainedMonitors!=(s.name.equals("useCreateZombieSpell")?3:0)||monitors!=s.monitorCount||')
(T/'PetMaterialMethods.java').write_text(s,encoding='utf-8')
s=(R/'earth-bind-material/build.py').read_text('utf-8').replace('EarthBindMaterialMethods','PetMaterialMethods').replace('NativeEarthBindMaterialAction','NativePetMaterialAction')
s=replace_once(s,"jar=R.parent/'poison-publication/build/candidate-poison-publication.jar';assert sha(jar)=='a53513d544124cbd1aac543f34302a15067f2a98422ed14576bcaaa6c991d4a3'","jar=R.parent/'polymorph-source/build/candidate-polymorph-source.jar';assert sha(jar)=='32d88c6b3b82926b035831fd8f6c186d662dc311f88826b06badda8f53853465'")
s=replace_once(s,"R.parent/'poison-publication/build/inputs/sources/NativeLightAction.java'","R.parent/'return-nature-transfer/build/inputs/sources/NativeLightAction.java'")
s=replace_once(s,"needle+'&&skill!=157'","needle+'&&skill!=36&&skill!=41'")
s=replace_once(s,"needle='note.contains(\"Striker Gale target=\")';assert t.count(needle)==1\nt=t.replace(needle,'('+needle+'||note.contains(\"Earth Bind target=\"))')\n",'')
s=s.replace('candidate-earth-bind-material.jar','candidate-pet-material.jar')
(T/'build.py').write_text(s,encoding='utf-8')
