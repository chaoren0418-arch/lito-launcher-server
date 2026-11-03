"""Bounded existing-flow regression of the armor candidate; not the full release suite."""
from pathlib import Path
import hashlib,json,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build/armor-regression';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(R/'armor-state/build/build.json');runtime=build['runtime'];assert sha(Path(runtime['path']))==runtime['sha256']
seed=read(R/'build/integration-regression/preparation.json')
if not (D/'build-current.json').exists():
 write(D/'preparation.json',dict(status='bounded-armor-candidate-under-test',runtime=runtime,parent_build=pin(R/'armor-state/build/build.json'),asm=seed['asm'],formal_deployment_changed=False,full_goal_achieved=False))
 write(D/'build-current.json',dict(status='candidate-under-test',runtime=runtime,manifest=pin(D/'preparation.json')))
assert read(D/'build-current.json')['runtime']==runtime
original=R/'full_regression.py';t=original.read_text('utf-8')
lines=t.splitlines();line=next(x for x in lines if x.startswith('S=Path('));start=line.index(';D=Path(')
t=t.replace(line,line[:start]+';D=Path('+repr(str(D))+')')
selected={0,2,7,12,13,15,16,17}
t=t.replace(' for index,name in enumerate(names):',' for index,name in enumerate(names):\n  if index not in '+repr(selected)+':continue')
# Preserve original indices for driver categories. Reuse matching names, not list positions.
t=t.replace("previous=prior_data['results']","previous={row['name']:row for row in prior_data['results']}")
t=t.replace('previous=[]','previous={}')
t=t.replace("if index<len(previous) and previous[index]['exit_code']==0 and previous[index]['adapted_driver']['sha256']==sha(adapted):","if name in previous and previous[name]['exit_code']==0 and previous[name]['adapted_driver']['sha256']==sha(adapted):")
t=t.replace('old=previous[index]','old=previous[name]')
t=t.replace('passed-twelve-native-regression-drivers-and-eight-authority-item-Main-replays','passed-eight-selected-native-state-and-authority-regressions-not-full-release-suite')
adapter=D/'bounded_existing_regression.py';adapter.write_text(t,encoding='utf-8')
write(D/'adaptation.json',dict(original=pin(original),adapted=pin(adapter),runtime=runtime,selected_original_indices=sorted(selected),change='Select eight original drivers, preserve driver category indices, evidence destinations and resume by name. Assertions and deadlines unchanged.',full_suite=False))
subprocess.run([sys.executable,'-B','-X','utf8',str(adapter)],cwd=S,check=True)
