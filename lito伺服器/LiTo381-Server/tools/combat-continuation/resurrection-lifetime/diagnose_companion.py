from pathlib import Path
R=Path(__file__).resolve().parent
t=(R/'ResurrectionCompanionProbe.java').read_text('utf-8').replace('ResurrectionCompanionProbe','ResurrectionCompanionDiagnosticProbe')
needle='   need((int)get(pc,"playerMp")==Math.max'
pos=t.index(needle)
diag='''   System.out.println("DIAGNOSTIC native mp="+get(pc,"playerMp")+" pet="+pet.snapshot()+" frames="+collect(wire));
   byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).bytes();
   call(pc,"useClassicWorldResurrectionSpell",new Class[]{byte[].class,row.getClass()},packet,row);
   System.out.println("DIAGNOSTIC root mp="+get(pc,"playerMp")+" pet="+pet.snapshot()+" frames="+collect(wire));
'''
t=t[:pos]+diag+t[pos:]
t=t.replace('new boolean[]{false,true}','new boolean[]{false}').replace('for(int skill:SKILLS)','for(int skill:new int[]{61})').replace('mode<9','mode<1')
(R/'ResurrectionCompanionDiagnosticProbe.java').write_text(t,encoding='utf-8')
d=(R/'test_companions.py').read_text('utf-8').replace('build/companions','build/companion-diagnostic').replace('ResurrectionCompanionProbe','ResurrectionCompanionDiagnosticProbe').replace("v[mode]['cases']==72","v[mode]['cases']==1").replace("status='passed'","status='diagnostic-not-qualified'")
(R/'test_companion_diagnostic.py').write_text(d,encoding='utf-8')
