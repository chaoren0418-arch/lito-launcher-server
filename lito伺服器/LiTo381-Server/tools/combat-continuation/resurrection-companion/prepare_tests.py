from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;P=R.parent/'resurrection-lifetime';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
names=['ResurrectionMaterialProbe.java','ResurrectionFaultProbe.java','ResurrectionLifecycleProbe.java','ResurrectionNumericProbe.java','ResurrectionAnswerLifetimeProbe.java','ResurrectionAnswerRaceProbe.java','ResurrectionWorldNpcProbe.java','ResurrectionCompanionProbe.java','ResurrectionCompanionAuditProbe.java','test.py','test_boundaries.py','test_numeric.py','test_answers.py','test_races.py','test_npc.py','test_companions.py','audit_companion_lifetime.py']
for name in names:(R/name).write_bytes((P/name).read_bytes())
p=R/'test_answers.py';t=p.read_text('utf-8').replace("(16 if mode=='before' else 0)",'0').replace("'before'","'parent'");p.write_text(t,encoding='utf-8')
p=R/'ResurrectionCompanionAuditProbe.java';t=p.read_text('utf-8').replace('mode<4','mode<5')
t=t.replace('else if(mode==3)set(peer,NativeDeItemApplication.DISCONNECTED,true);','else if(mode==3)set(peer,NativeDeItemApplication.DISCONNECTED,true);\n    else if(mode==4){need(pet.resurrect(pet.snapshot().maxHp()),"actual intervening pet resurrection");pet.applyDamage(Integer.MAX_VALUE);}')
t=t.replace('clean.set(NativeLightStoppedProbe.idleContext());','boolean idle=NativeLightStoppedProbe.idleContext();if(!before){var f=NativeResurrectionMaterialAction.class.getDeclaredField("current");f.setAccessible(true);idle&=((ThreadLocal<?>)f.get(null)).get()==null;}clean.set(idle);')
t=t.replace('else if(revived)gaps++;','else if(revived)gaps++;\n   if(!before&&mode>0)need(!revived&&(int)get(pc,"playerMp")==1000&&(item==0||pc.nativeDeInventory(0).countCatalog(item)==count+3)&&!((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)&&wire.raw.size()==0&&other.raw.size()==0,"stale owner/entity/death cycle rejected before costs, reuse and publication");')
t=t.replace('if(mode==1)NativePlayerCommit.selected', 'if(mode==1)System.out.println("QUEUED before="+before+" "+Arrays.toString(worker.getStackTrace()));\n    if(mode==1)NativePlayerCommit.selected')
p.write_text(t,encoding='utf-8')
t=(P/'audit_companion_lifetime.py').read_text('utf-8').replace("[('after',b['runtime'])]","[('before',b['base']),('after',b['runtime'])]").replace("v[mode]['cases']==16", "v[mode]['cases']==20 and v[mode]['stale_companion_acceptances']==(16 if mode=='before' else 0)").replace("status='observed-not-qualified'","status='passed'")
(R/'test_companion_lifetime.py').write_text(t,encoding='utf-8')
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(P/n),sha256=sha(P/n))for n in names]+[dict(path=str(Path(__file__)),sha256=sha(Path(__file__)))],scope='new20 before/after queued companion owner/identity/death-cycle cases; replay accepted material/answer/NPC controls; no full-release claim'),indent=2),encoding='utf-8')
