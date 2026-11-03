from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;W=R.parent/'water-life-material'
for name in ['Fault','Lifecycle']:
 t=(W/('WaterLife'+name+'Probe.java')).read_text('utf-8').replace('WaterLife','Negative')
 if name=='Fault':
  t=t.replace('skill=selectedSkill;','skill=selectedSkill;NegativeMaterialProbe.selectedSkill=skill;NegativeMaterialProbe.selectedMode=0;probability(skill);')
  t=t.replace('need(((BattleNegativeStatus)get(recipient,"waterLifeStatus")).active(System.nanoTime())==before,"target effect waits for material save success");','need(!active(recipient,skill),"original target effect follows successful material save");')
  t=t.replace('need(((BattleNegativeStatus)get(peer,"waterLifeStatus")).active(System.nanoTime())==before,"failed material cannot activate target status");','need(!active(peer,skill),"failed material cannot activate target status");')
  t=t.replace('faults==8','faults==24')
 else:
  t=t.replace('call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(21).c(1).d(target.playerObjectId()).bytes());','''var row=spell(selectedSkill);byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((selectedSkill-1)>>3).c((selectedSkill-1)&7).d(target.playerObjectId()).h((int)get(target,"positionX")).h((int)get(target,"positionY")).bytes();
  call(pc,selectedSkill==153?"useEraseMagicSpell":"useEntangleSpell",new Class[]{byte[].class,row.getClass(),int.class},packet,row,1);''')
  t=t.replace('var row=BattleSpellCatalog.waterLifeSpell(170);int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();','var row=spell(selectedSkill);probability(selectedSkill);int item=Math.toIntExact((long)call(row,"itemConsumeId",new Class[]{})),count=(int)call(row,"itemConsumeCount",new Class[]{});')
  t=t.replace('var status=(BattleNegativeStatus)get(peer,"waterLifeStatus");','')
  t=t.replace('status.active(System.nanoTime())','active(peer,selectedSkill)')
  t=t.replace('((BattleNegativeStatus)get(pc,"waterLifeStatus")).active(System.nanoTime())','active(pc,selectedSkill)')
  t=t.replace('row.effectGfx()','(int)call(row,"effectGfx",new Class[]{})')
  t=t.replace('170','selectedSkill')
  t=t.replace('for(int mode=0;mode<7;mode++){','for(int skill:SKILLS)for(int mode=0;mode<7;mode++){selectedSkill=skill;selectedMode=0;')
  t=t.replace('app+"-"+mode','app+"-"+skill+"-"+mode')
 (R/('Negative'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(W/'test_boundaries.py').read_text('utf-8').replace('WaterLife','Negative')
t=t.replace("('Fault','before',old,8),('Fault','after',jar,8),('Lifecycle','after',jar,56)","('Fault','before',old,24),('Fault','after',jar,24),('Lifecycle','after',jar,168)")
t=t.replace("row['actual_memory_faults']==8", "row['actual_memory_faults']==24").replace("(8 if mode=='before' else 0)","(24 if mode=='before' else 0)")
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'boundary-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[W/'WaterLifeFaultProbe.java',W/'WaterLifeLifecycleProbe.java',W/'test_boundaries.py',Path(__file__)]],scope='3 existing roots: real memory-save fault, ordered PC pair, role/scene, retained STOPPED receipt and output failure; no NPC/companion claim'),indent=2),encoding='utf-8')
