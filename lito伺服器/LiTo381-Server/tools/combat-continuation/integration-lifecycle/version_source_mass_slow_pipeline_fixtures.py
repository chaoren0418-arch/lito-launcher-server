"""Version only Source76 visual expectations proved by the complete Source pipeline reference."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
reference=read(D/'source-mass-slow-pipeline-oracle-current.json');assert sha(Path(reference['manifest']['path']))==reference['manifest']['sha256'];oracle=read(Path(reference['manifest']['path']));assert oracle['observations']['Source_pipeline_reference_cases']==36
for row in oracle['Source']:assert sha(Path(row['original']['path']))==row['original']['sha256']
changes={}
old='if(!before)need(!self.contains(fx)&&!other.contains(fx),"Source failed NPC is removed before effects");'
new='if(!before)need(Collections.frequency(self,fx)==1&&Collections.frequency(other,fx)==1,"Source buff area retains caster entry and sends one primary visual while NPC state effects fail");'
for name in ['SourceMassSlowControlledProbe.java','SourceMassSlowStateProbe.java']:changes[name]=[(old,new)]
for name,count in [('SourceMassSlowMixedProbe.java','1'),('SourceMassSlowCtrlAreaProbe.java','(ctrl==1?1:0)')]:changes[name]=[
 ('Collections.frequency(self,fx)==(applied?1:0)','Collections.frequency(self,fx)=='+count),
 ('Collections.frequency(other,fx)==(applied?1:0)','Collections.frequency(other,fx)=='+count),
 ('Source buff sendGrfx keeps one primary-target visual when a real player remains in targetList; no NPC state effect','Source buff sendGrfx retains caster entry independently of player probability success; no NPC state effect')]
def project(folder,items,basis=None):
 folder.mkdir(exist_ok=True);rows=[]
 for source,patches in items:
  old=source.read_text('utf-8');text=old
  for a,b in patches:assert text.count(a)==1,(source.name,a);text=text.replace(a,b)
  inverse=text
  for a,b in reversed(patches):assert inverse.count(b)==1;inverse=inverse.replace(b,a)
  assert inverse==old
  target=folder/source.name;target.write_text(text,'utf-8');rows.append(dict(original=pin(source),versioned=pin(target),changes=patches,whole_file_inverse_matches=True))
 write(folder/'manifest.json',dict(Source_pipeline_reference=reference['manifest'],scope_legacy_basis=basis,Source76_visual_expectations_only=True,all_cost_role_state_lifecycle_and_skill66_assertions_retained=True,rows=rows,full_goal_achieved=False))
project(R/'source76-pipeline-fixtures',[(R/name,patches)for name,patches in changes.items()])
basis=R/'source76-legacy-fixtures/manifest.json';legacy=read(basis);items=[]
for row in legacy['rows']:
 source=Path(row['versioned']['path']);assert sha(source)==row['versioned']['sha256'];patches=[]
 if source.name in ['CompanionAreaSceneProbe.java','CompanionAreaLifecycleProbe.java']:patches=[('self.contains(fx)==(accepted&&skill==66)','self.contains(fx)==accepted')]
 items.append((source,patches))
project(R/'source76-pipeline-legacy-fixtures',items,pin(basis))
print(json.dumps(dict(status='versioned-Source76-visual-only-with-exact-whole-file-inverses',direct_fixtures=4,legacy_fixtures=5)))
