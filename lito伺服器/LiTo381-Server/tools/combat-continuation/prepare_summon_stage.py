"""Prepare a separate unqualified Summon Monster stage; preserve all earlier evidence."""
from pathlib import Path
R=Path(__file__).resolve().parent; E=R/'elemental-material'; N=R/'summon-material';N.mkdir(exist_ok=True)
t=(E/'ElementalMethods.java').read_text('utf-8')
t=t.replace('ElementalMethods','SummonMethods').replace('NativeElementalCastAction','NativeSummonCastAction').replace('ElementalSummon','Summon')
start=t.index('   if(n.getOpcode()==MONITORENTER')
end=t.index('\n   if(n instanceof MethodInsnNode',start)
t=t[:start]+'''   if(n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT){
    var previous=prev(n);if(!(previous instanceof VarInsnNode v))throw new AssertionError("unknown monitor");
    if(v.var==23){m.instructions.set(n,new InsnNode(POP));pcMonitors++;}
    else if(v.var==37)companionMonitors++;else throw new AssertionError("unknown monitor local "+v.var);
   }'''+t[end:]
t=t.replace('new VarInsnNode(LSTORE,11)','new VarInsnNode(LSTORE,21)')
t=t.replace('savedSlot.var==9','savedSlot.var==18').replace('new VarInsnNode(ASTORE,9)','new VarInsnNode(ASTORE,18)')
anchor='  var wrapper=new MethodNode'
assert t.count(anchor)==1
t=t.replace(anchor,'''  int clocks=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode factory&&factory.owner.equals(P+"BattleCompanion")&&factory.name.equals("summoned")){
   var time=prev(n);if(!(time instanceof VarInsnNode v)||v.getOpcode()!=LLOAD||v.var!=21)throw new AssertionError("summon construction time");
   m.instructions.set(time,new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false));clocks++;
  }
  if(clocks!=1)throw new AssertionError("one summoned factory");
'''+anchor)
(N/'SummonMethods.java').write_text(t,encoding='utf-8')
t=(E/'NativeElementalCastAction.java').read_text('utf-8').replace('NativeElementalCastAction','NativeSummonCastAction').replace('ElementalSummon','Summon')
t=t.replace('spell.skillId()!=154&&spell.skillId()!=162','spell.skillId()!=51')
(N/'NativeSummonCastAction.java').write_text(t,encoding='utf-8')
t=(E/'build.py').read_text('utf-8')
t=t.replace("R.parent/'invisibility-leases/build/candidate-invisibility-leases.jar'","R.parent/'elemental-lifetime/build/candidate-elemental-lifetime.jar'")
t=t.replace('d4bc643fca4ab18ea4b6b84e9f685bbc80bd26d806b98970a6a0d98d28ae3a81','b0362026f3ec4ec105a63ed1f1da87cdb1192f69e546f86f16f88d220b6d582c')
t=t.replace("R.parent/'build/inputs/sources/NativeLightAction.java'","R.parent/'elemental-material/build/inputs/sources/NativeLightAction.java'")
t=t.replace("needle+'&&skill!=154&&skill!=162'","needle+'&&skill!=51'")
t=t.replace('NativeElementalCastAction','NativeSummonCastAction').replace('ElementalMethods','SummonMethods')
t=t.replace("['LESSER_ELEMENTAL','GREATER_ELEMENTAL']","['SUMMON_MONSTER']")
t=t.replace('candidate-elemental-material.jar','candidate-summon-material.jar')
(N/'build.py').write_text(t,encoding='utf-8')
print('Prepared unqualified isolated skill51 stage')
