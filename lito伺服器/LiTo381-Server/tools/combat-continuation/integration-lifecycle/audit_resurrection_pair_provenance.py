"""Correct a legacy evidence path alias without rewriting receipts or rerunning/changing gameplay."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,shutil,copy
R=Path(__file__).resolve().parent;D=R/'build';B=R.parents[2]/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
g=read(D/'companion-resurrection-gates-regressions-current.json');verify(g);gates=read(Path(g['manifest']['path']));verify(gates)
row=next(r for r in gates['results'] if Path(r['original']['path']).name=='test_pair.py');p=Path(row['manifest']['path']);m=read(p)
assert m['runtime']==g['runtime'] and m['status'].startswith('passed') and g['runtime']['sha256']=='63f38c850e280837d4deead387117de82f16cbb1c6a6a241ab2c659d4719e8db'
declared=m['fixture_adapter']['original'];adapted=m['fixture_adapter']['adapted'];assert declared['path']==adapted['path'] and declared['sha256']!=adapted['sha256']
original=B/'NativePairMaterialBoundaryProbe.java';assert sha(original)==declared['sha256'];verify(adapted)
before=original.read_text('utf-8');after=Path(adapted['path']).read_text('utf-8');assert before.count('NativePairCastAction.class')==1 and after==before.replace('NativePairCastAction.class','NativePairedCastSafety.class')
run=D/'companion-resurrection-pair-provenance'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
frozen=inputs/original.name;shutil.copyfile(original,frozen);shutil.copyfile(__file__,inputs/Path(__file__).name)
normalized=copy.deepcopy(m);normalized['fixture_adapter']['original']=pin(frozen);verify(normalized)
assert {k:v for k,v in normalized.items() if k!='fixture_adapter'}=={k:v for k,v in m.items() if k!='fixture_adapter'}
write(run/'normalized-pair-manifest.json',normalized)
v=dict(status='passed-exact-legacy-pair-source-path-alias-correction-no-behavior-change',runtime=g['runtime'],gates=g['manifest'],historical_pair_manifest=row['manifest'],normalized_pair_manifest=pin(run/'normalized-pair-manifest.json'),original_source=pin(original),frozen_original=pin(frozen),actual_compiled_input=adapted,declared_aliased_path=declared['path'],declared_original_sha256=declared['sha256'],inputs=[pin(p) for p in inputs.iterdir()],cases_added=0,explanation='The original driver pinned its temporary boundary source before replacing one obsolete runtime-origin class literal, then reused the same path. Frozen compiled input, exact one-literal adaptation, compiled classes, observations and all other references are verified; historical manifests and results remain unchanged.',full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
verify(v);write(run/'manifest.json',v);write(D/'companion-resurrection-pair-provenance-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases_added=0)))
