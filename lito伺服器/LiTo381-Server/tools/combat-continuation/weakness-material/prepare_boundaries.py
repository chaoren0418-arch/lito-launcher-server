from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;N=R.parent/'negative-material'
for name in ['Fault','Lifecycle']:
 t=(N/('Negative'+name+'Probe.java')).read_text('utf-8').replace('Negative','Weakness')
 t=t.replace('faults==24','faults==16')
 t=t.replace('call(pc,selectedSkill==153?"useEraseMagicSpell":"useEntangleSpell",new Class[]{byte[].class,row.getClass(),int.class},packet,row,1);','call(pc,"handle",new Class[]{byte[].class},packet);')
 t=t.replace('Map.of("battleNaturalHpRegenEnabled",false,','Map.of("weaknessFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,')
 (R/('Weakness'+name+'Probe.java')).write_text(t,encoding='utf-8')
t=(N/'test_boundaries.py').read_text('utf-8').replace('Negative','Weakness')
t=t.replace("('Fault','before',old,24),('Fault','after',jar,24),('Lifecycle','after',jar,168)","('Fault','before',old,16),('Fault','after',jar,16),('Lifecycle','after',jar,112)")
t=t.replace("row['actual_memory_faults']==24","row['actual_memory_faults']==16").replace("(24 if mode=='before' else 0)","(16 if mode=='before' else 0)")
(R/'test_boundaries.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'boundary-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[N/'NegativeFaultProbe.java',N/'NegativeLifecycleProbe.java',N/'test_boundaries.py',Path(__file__)]],scope='actual native47/56 packet producers, material save failure, both PC roles, scene/STOPPED/output; existing percentile fixture'),indent=2),encoding='utf-8')
