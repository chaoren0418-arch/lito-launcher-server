"""Record completed narrow controls without claiming whole combat qualification."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
p=R/'control-gaps.json';v=read(p)
rows=v.get('implemented_controls',v.get('pending_controls'))
mapping={'SOLID_CARRIAGE_MODE':'solid-control','INVIS_CAST_BREAK':'invisibility-control','Triple_Arrow_Dmg':'triple-control','SLAY_BREAK_NOT_TELEPORT':'bind-control'}
for row in rows:
 d=R/mapping[row['key']];result=read(d/'build/tests/result.json');assert result['status']=='passed';assert sha(Path(result['runtime']['path']))==result['runtime']['sha256']
 row.update(status='implemented-targeted-tested-full-combat-release-pending',runtime=result['runtime'],tests=pin(d/'build/tests/result.json'),reproduction=pin(d/'build-repro/build.json'))
v.update(scope='Four Source controls implemented and tested; composed-candidate regression receipts are indexed by latest_status, and the full combat release remains open',pending_controls=[],implemented_controls=rows,latest_status=pin(R/'continuation-current.json'))
other=R/'build/control-inventory/other-controls-scope.json'
if other.exists():
 audit=read(other)
 v['additional_control_audit']=pin(other)
 v['additional_unqualified_controls']=[{'key':row['property'],'source_field':row['source_field'],'status':row['classification']} for row in audit['controls'] if row['classification']!='excluded-death-penalty-rule']
 v['excluded_controls']=[{'key':row['property'],'reason':'LiTo has no death equipment or experience penalty'} for row in audit['controls'] if row['classification']=='excluded-death-penalty-rule']
v['research_notes']['SOLID_CARRIAGE_MODE']='Shield/guarder handling, parser fallback, native equipment and source equipment snapshot paths tested. See exact result receipt.'
v['research_notes']['INVIS_CAST_BREAK']='Source Boolean parser and 72 actual cast variants passed; enabled packet output matched parent. Separate overlapping 60/97 timers candidate is not implied qualified by this control receipt.'
v['research_notes']['Triple_Arrow_Dmg']='Bow/gauntlet, three indexed attempts, same caster and scoped calculation only; maximum multiplier before defense. Nonfinite values deliberately rejected at startup instead of Source NaN/Infinity acceptance.'
v['research_notes']['SLAY_BREAK_NOT_TELEPORT']='PC action lock excludes thunder binding; movement retains old lock. Actual skill 5 consumes accepted resources before blocked message/unlock. NPC lock unchanged. Skill 131 remains production-disabled; enabled 131 branch has not been behaviorally qualified.'
p.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print('Updated four implemented control receipts; final qualification still open')
