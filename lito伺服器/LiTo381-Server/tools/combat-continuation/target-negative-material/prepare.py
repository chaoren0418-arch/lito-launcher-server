"""Preserve the private native kind and exact shared interface-root behavior."""
from pathlib import Path
import re,json,hashlib
R=Path(__file__).resolve().parent;P=R.parent;I=P/'silence-material';D=P/'decay-material'
t=(I/'SilenceMaterialMethods.java').read_text('utf-8').replace('SilenceMaterialMethods','TargetNegativeMaterialMethods').replace('NativeSilenceMaterialAction','NativeTargetNegativeMaterialAction')
t=re.sub(r'static final List<Spec> SPECS=List\.of\([^\n]+', 'static final List<Spec> SPECS=List.of(new Spec("useTargetNegativeStatusSpell","nativeMaterialTargetNegativeBody","cast","TargetNegativeStatusSpell",47,52,43,45,15,5));',t)
t=t.replace('new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$"+s.spell,accessor,"()I",false)','new MethodInsnNode(INVOKEINTERFACE,P+"BattleSpellCatalog$"+s.spell,accessor,"()I",true)')
start=t.index('   var wrapper=new MethodNode');end=t.index('\n  }',start)
t=t[:start]+'''   var wrapper=new MethodNode(ASM9,old.access,old.name,old.desc,old.signature,old.exceptions.toArray(String[]::new));
   wrapper.instructions.add(new VarInsnNode(ALOAD,0));wrapper.instructions.add(new VarInsnNode(ALOAD,1));
   for(int i=0;i<5;i++)wrapper.instructions.add(new VarInsnNode(i==3?ILOAD:ALOAD,i));
   String task=P+"NativeSelfCastAction$Task",capture="(L"+PC+";"+old.desc.substring(1,old.desc.length()-1)+"L"+task+";";
   var bootstrap=new Handle(H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory","(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false);
   wrapper.instructions.add(new InvokeDynamicInsnNode("run",capture,bootstrap,Type.getMethodType("()V"),new Handle(H_INVOKEVIRTUAL,PC,s.body,old.desc,false),Type.getMethodType("()V")));
   wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,A,"cast","(L"+PC+";[BL"+task+";)V",false));wrapper.instructions.add(new InsnNode(RETURN));c.methods.set(c.methods.indexOf(old),wrapper);c.methods.add(m);changed.add(wrapper);changed.add(m);'''+t[end:]
t=t.replace('PASS two negative roots inverse','PASS one shared target-negative interface root inverse')
(R/'TargetNegativeMaterialMethods.java').write_text(t,encoding='utf-8')
t=(I/'build.py').read_text('utf-8').replace('Silence','TargetNegative').replace('silence-material','target-negative-material')
t=t.replace('ground-material/build/candidate-ground-material.jar','world-potion/build/candidate-world-potion.jar').replace('9c5e60af1702d4e99d6a25da0fad1ce4ad66b1eeeb62db6637423b8145d7572d','91095e7a3d425994935e3c87210d2aafeea0cb36cebf2bd084bab4ab812afac5').replace('ground-material/build/inputs/sources/NativeLightAction.java','decay-material/build/inputs/sources/NativeLightAction.java')
t=t.replace("needle='&&skill!=63'","needle='&&skill!=71'").replace("needle+'&&skill!=64'","needle+'&&skill!=173&&skill!=174'")
t=t.replace('note.contains("Disease target=")','note.contains("Decay Potion target=")').replace('note.contains("TargetNegative target=")','note.contains("Pollute Water target=")||note.contains("Striker Gale target=")')
(R/'build.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[I/'SilenceMaterialMethods.java',I/'build.py',D/'build/inputs/sources/NativeLightAction.java',P/'build/target-negative-audit/inputs.json',Path(__file__)]],scope='173/174 shared original kind/interface body; target resolution and original side effects unchanged; no consumer or target coverage claimed before testing'),indent=2),encoding='utf-8')
