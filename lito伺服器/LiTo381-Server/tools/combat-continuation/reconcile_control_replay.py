"""Retain completed behavioral results; rebase only the historical formal-file guard."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/'build/control-regression'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
p=D/'replays/20261018T150530069906Z/manifest.json';v=read(p);build=read(D/'build-current.json');assert v['runtime']==build['runtime'];assert sha(Path(v['runtime']['path']))==v['runtime']['sha256']
formal=S.parent.parent/'lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar';assert v['status']=='failed' and v['error']==str(formal)
rows=v['completed_results'];assert len(rows)==48 and sum(x['cases'] for x in rows)==1728
for r in rows:
 for key in ['observations','log']:
  assert sha(Path(r[key]['path']))==r[key]['sha256']
 obs=read(Path(r['observations']['path']));assert obs.get('cases',0)==r['cases'] and obs['checks']==r['checks']
for r in v['outputs']:assert sha(Path(r['path']))==r['sha256']
for r in v['references']:assert sha(Path(r['path']))==r['sha256']
guards=read(B/'native-proc-healing-world-current.json')['protected_files'];current=[];changes=[]
for r in guards:
 if Path(r['path'])==formal:
  assert r['sha256']=='c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539'
  assert sha(formal)=='75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'
  changes.append({'historical':r,'current_accepted_0_6_69':pin(formal)})
 else:assert sha(Path(r['path']))==r['sha256']
 current.append(pin(Path(r['path'])))
assert len(changes)==1
result=dict(status='passed-behavior-and-current-formal-guard',runtime=build['runtime'],original_failed_manifest=pin(p),cases=sum(r['cases'] for r in rows),checks=sum(r['checks'] for r in rows),results=rows,protected_files=current,guard_rebase=changes,behavior_assertions_or_timeouts_changed=False,formal_deployment_changed=False,full_goal_achieved=False)
out=D/'replay-reconciliation.json';out.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps({'status':result['status'],'cases':result['cases'],'checks':result['checks']}))
qualified=dict(result,commands=v['commands'],outputs=v['outputs'],references=v['references'],counts={'cases':result['cases'],'checks':result['checks']})
manifest=D/'replay-qualified-current-formal.json';manifest.write_text(json.dumps(qualified,ensure_ascii=False,indent=2),encoding='utf-8')
(D/'replay-current.json').write_text(json.dumps({'status':qualified['status'],'manifest':pin(manifest),'counts':qualified['counts']},ensure_ascii=False,indent=2),encoding='utf-8')
