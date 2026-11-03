from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;D=R.parent/'decay-material';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
names=['DecayPotionMaterialProbe.java','DecayPotionFaultProbe.java','DecayPotionLifecycleProbe.java','DecayPotionWorldNpcProbe.java','test.py','test_boundaries.py','test_world_npcs.py']
for name in names:
 t=(D/name).read_text('utf-8').replace('DecayPotion','Blind').replace('decayPotion','blind').replace('blindFixturePercentileRoll','darkBlindFixtureRoll')
 if name=='DecayPotionMaterialProbe.java':
  t=t.replace('Native skill71 packet','Native skills40/103 packets').replace('SKILLS={71}','SKILLS={40,103}').replace('Set.of(2,71)','Set.of(2,40,103)').replace('"type",3','"type",selectedSkill==103?4:3')
  t=t.replace('mode<12','mode<13')
  t=t.replace('var self=collect(wire);var peers=collect(other);','''if(mode==12){
    var status=(BattleTimedStatus)get(peer,"timedStatus");long end=(long)get(status,"blindExpiresAtNanos");
    need(!status.expireBlind(end-1)&&active(peer,skill),"blind retained before stored deadline");
    need(status.expireBlind(end)&&!active(peer,skill),"blind expires at stored deadline");
   }
   var self=collect(wire);var peers=collect(other);
   if(mode==12)need(peers.contains(HexFormat.of().formatHex(Protocol.blind(1))),"actual blind mode1 packet reaches affected player");''')
 if name=='test.py':t=t.replace("v[mode]['cases']==96","v[mode]['cases']==208").replace("(8 if mode=='before' else 0)","(16 if mode=='before' else 0)")
 if name=='DecayPotionFaultProbe.java':t=t.replace('faults==8','faults==16')
 if name=='test_boundaries.py':
  t=t.replace("('Fault','before',old,8),('Fault','after',jar,8),('Lifecycle','after',jar,56)","('Fault','before',old,16),('Fault','after',jar,16),('Lifecycle','after',jar,112)").replace("row['actual_memory_faults']==8","row['actual_memory_faults']==16").replace("(8 if mode=='before' else 0)","(16 if mode=='before' else 0)")
 if name=='test_world_npcs.py':t=t.replace("v[mode]['cases']==32","v[mode]['cases']==64").replace("(4 if mode=='before' else 0)","(8 if mode=='before' else 0)")
 (R/name.replace('DecayPotion','Blind')).write_text(t,encoding='utf-8')
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(D/n),sha256=sha(D/n))for n in names]+[dict(path=str(Path(__file__)),sha256=sha(Path(__file__)))],scope='both native Blind skills; original deterministic percentile fixture; actual blind packet and stored expiry; no SQL writes'),indent=2),encoding='utf-8')
