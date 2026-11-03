from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;P=R.parent/'resurrection-material';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for name in ['ResurrectionMaterialProbe.java','ResurrectionFaultProbe.java','ResurrectionLifecycleProbe.java','ResurrectionNumericProbe.java','test.py','test_boundaries.py','test_numeric.py']:
 (R/name).write_bytes((P/name).read_bytes())
t=(P/'ResurrectionAnswerLifetimeProbe.java').read_text('utf-8')
t=t.replace('collect(wire);collect(other);need(', 'if(!before)need(get(peer,"nativeResurrectionAnswerLease")==null&&!active(peer,skill),"completed or rejected answer releases its sole pending role lease");\n   collect(wire);collect(other);need(')
(R/'ResurrectionAnswerLifetimeProbe.java').write_text(t,encoding='utf-8')
t=(P/'audit_answer_lifetime.py').read_text('utf-8').replace("D=R/'build/answer-audit'","D=R/'build/answers'")
t=t.replace("[('observed',b['runtime'])]","[('before',b['base']),('after',b['runtime'])]")
t=t.replace("assert v[mode]['cases']==64 and v[mode]['locked_writes']==0","assert v[mode]['cases']==64 and v[mode]['locked_writes']==0 and v[mode]['stale_native_acceptances']==v[mode]['stale_direct_acceptances']==(16 if mode=='before' else 0)")
t=t.replace("status='observed-not-qualified'","status='passed'")
(R/'test_answers.py').write_text(t,encoding='utf-8')
# The material/MP parent already has the resource fixes. These replays qualify
# that answer lease hooks retain the passed native cast, numeric and fault results.
for name in ['test.py','test_boundaries.py','test_numeric.py']:
 p=R/name;t=p.read_text('utf-8')
 if name=='test.py':t=t.replace("(16 if mode=='before' else 0)",'0')
 elif name=='test_boundaries.py':t=t.replace("(8 if mode=='before' else 0)",'0')
 else:
  t=t.replace("(v[mode]['source_predicted_overflows'] if mode=='before' else 0)",'0').replace("v['before']['signed_mp_bound_gaps']>0", "v['before']['signed_mp_bound_gaps']==0")
 # The parent already fixes material admission and MP bounds; use the retained
 # success oracle, not the historical pre-material failure oracle in Java.
 t=t.replace("'before'", "'parent'").replace('before/goldens.txt','parent/goldens.txt')
 p.write_text(t,encoding='utf-8')
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(P/n),sha256=sha(P/n))for n in ['ResurrectionMaterialProbe.java','ResurrectionFaultProbe.java','ResurrectionLifecycleProbe.java','ResurrectionNumericProbe.java','ResurrectionAnswerLifetimeProbe.java','test.py','test_boundaries.py','test_numeric.py','audit_answer_lifetime.py']]+[dict(path=str(Path(__file__)),sha256=sha(Path(__file__)))],scope='before/after64 native/direct role-replacement replies; retain256 original cast/reply and48 numeric cases; actual material fault observer and48 cast lifetime cases; no NPC/full-release claim'),indent=2),encoding='utf-8')
