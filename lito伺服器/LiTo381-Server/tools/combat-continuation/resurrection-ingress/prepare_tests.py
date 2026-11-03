from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent/'resurrection-companion';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
names=['ResurrectionMaterialProbe.java','ResurrectionFaultProbe.java','ResurrectionLifecycleProbe.java','ResurrectionNumericProbe.java','ResurrectionAnswerLifetimeProbe.java','ResurrectionAnswerRaceProbe.java','ResurrectionWorldNpcProbe.java','ResurrectionCompanionProbe.java','ResurrectionCompanionAuditProbe.java','test.py','test_boundaries.py','test_numeric.py','test_answers.py','test_races.py','test_npc.py','test_companions.py','test_companion_lifetime.py']
for name in names:(R/name).write_bytes((P/name).read_bytes())
p=R/'test_companion_lifetime.py';t=p.read_text('utf-8').replace("(16 if mode=='before' else 0)","(12 if mode=='before' else 0)");p.write_text(t,encoding='utf-8')
p=R/'ResurrectionCompanionAuditProbe.java';t=p.read_text('utf-8').replace('idle&=((ThreadLocal<?>)f.get(null)).get()==null;', 'idle&=((ThreadLocal<?>)f.get(null)).get()==null;f=NativeResurrectionMaterialAction.class.getDeclaredField("ingress");f.setAccessible(true);idle&=((ThreadLocal<?>)f.get(null)).get()==null;');p.write_text(t,encoding='utf-8')
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(P/n),sha256=sha(P/n))for n in names]+[dict(path=str(Path(__file__)),sha256=sha(Path(__file__)))],scope='20 queued native ingress cases capture target before first caster wait; retained material/answer/NPC/pet behavior; no full-release claim'),indent=2),encoding='utf-8')
