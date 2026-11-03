from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;B=R.parent/'blind-material';pins=[]
for name in ['BlindWorldNpcProbe.java','test_world_npcs.py']:
 p=B/name;t=p.read_text('utf-8').replace('Blind','EarthBind').replace('darkEarthBindFixtureRoll','earthBindFixturePercentileRoll')
 if name.endswith('.py'):t=t.replace("['cases']==64","['cases']==32").replace("(8 if mode=='before' else 0)","(4 if mode=='before' else 0)")
 q=R/name.replace('Blind','EarthBind');assert not q.exists();q.write_text(t,encoding='utf-8');pins.append({'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(R/'npc-adaptation.json').write_text(json.dumps({'status':'prepared-not-qualified','inputs':pins,'scope':'actual world population, skill157 native earth-bind status and fixture; no production PC overlays'},ensure_ascii=False,indent=2),encoding='utf-8')
