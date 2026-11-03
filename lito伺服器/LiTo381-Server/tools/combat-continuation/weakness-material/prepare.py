from pathlib import Path
R=Path(__file__).resolve().parent;N=R.parent/'negative-material'
t=(N/'NegativeMaterialMethods.java').read_text('utf-8').replace('NegativeMaterialMethods','WeaknessMaterialMethods').replace('NativeNegativeMaterialAction','NativeWeaknessMaterialAction')
t=t.replace('int monitor,int copy,int previous,int now','int monitor,int copy,int previous,int now,int target,int monitorCount')
a=t.index(' static final List<Spec> SPECS=');b=t.index('\n static AbstractInsnNode',a)
t=t[:a]+' static final List<Spec> SPECS=List.of(new Spec("useWeaknessSpell","nativeMaterialWeaknessBody","battle","WeaknessSpell",48,56,42,44,14,12),new Spec("useClassicWorldWeaknessSpell","nativeMaterialWorldWeaknessBody","world","WeaknessSpell",47,55,41,43,12,10));'+t[b:]
t=t.replace('v.var!=s.monitor','(v.var!=s.monitor&&v.var!=s.monitor+1)').replace('monitors!=5','monitors!=s.monitorCount')
t=t.replace('gate.add(new VarInsnNode(ALOAD,8));gate.add(new VarInsnNode(ALOAD,9));','gate.add(new VarInsnNode(ALOAD,s.target));')
t=t.replace('"(L"+PC+";L"+PC+";L"+PC+";III)Z"','"(L"+PC+";L"+PC+";III)Z"')
t=t.replace('wrapper.instructions.add(new VarInsnNode(ILOAD,3));','')
(R/'WeaknessMaterialMethods.java').write_text(t,encoding='utf-8')
t=(N/'build.py').read_text('utf-8').replace('armor-state/build/candidate-armor-state.jar','negative-material/build/candidate-negative-material.jar').replace('c7bae84c7a0d260186e38763c1af7dba16db643a77477d096d6540fa747a01b8','18fbe46a414e29974bdb4d625b33b6aca9b3271cac0eaf9994a14ed7801f244e').replace('armor-material/build/inputs/sources/NativeLightAction.java','negative-material/build/inputs/sources/NativeLightAction.java')
t=t.replace("needle='&&skill!=112'","needle='&&skill!=153'").replace("needle+'&&skill!=29&&skill!=152&&skill!=153'","needle+'&&skill!=47&&skill!=56'")
a=t.index("needle='note.startsWith(");b=t.index('(src/light.name)',a)
effect="needle='note.contains(\"Erase Magic target=\")';assert t.count(needle)==1\nt=t.replace(needle,'('+needle+'||note.contains(\"Weakness target=\")||note.contains(\"Disease target=\"))')\n"
t=t[:a]+effect+t[b:]
t=t.replace('NegativeMaterialMethods','WeaknessMaterialMethods').replace('NativeNegativeMaterialAction','NativeWeaknessMaterialAction')
t=t.replace("candidate=D/'candidate-negative-material.jar'","candidate=D/'candidate-weakness-material.jar'")
(R/'build.py').write_text(t,encoding='utf-8')
