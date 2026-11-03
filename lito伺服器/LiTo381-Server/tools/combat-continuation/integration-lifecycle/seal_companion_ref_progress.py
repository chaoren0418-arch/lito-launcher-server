"""Seal common WORLD Ref evidence without promoting the integrated candidate or full Source coverage."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';continuation=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json');integrated=read(R/'current.json');ih=sha(R/'current.json');previous=read(continuation)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert integrated['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df'
names=['companion-ref-scene-build','companion-ref-scene-reproduction','companion-ref-lifecycle-before','companion-ref-lifecycle-after','companion-ref-feedback-before','companion-ref-feedback-after','companion-ref-regressions','companion-ref-scene-consumers','companion-ref-origin'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed','located')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]]
assert runtime['sha256']=='718abe6b7a490079a67775707e6ed1b455398593be1b9812cfa24669f4e8206a'
for name in names:
 if not name.endswith('-before'):assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert len(build['changed'])==2 and len(build['fresh'])==1 and build['new_player_fields_workers_schedulers_ThreadLocals_global_registries']==0
old=manifests['companion-ref-lifecycle-before']['observations'];new=manifests['companion-ref-lifecycle-after']['observations'];fold=manifests['companion-ref-feedback-before']['observations'];fnew=manifests['companion-ref-feedback-after']['observations'];reg=manifests['companion-ref-regressions']
for before,after in [(old,new),(fold,fnew)]:
 assert before['cases']==after['cases']==10 and before['master_scene_Refs']==10 and after['master_scene_Refs']==0 and before['valid_actor_Refs_lost']==2 and after['valid_actor_Refs_lost']==0
assert fold['accepted_frames']==fnew['accepted_frames']==fold['queued_native_PC_events']==fnew['queued_native_PC_events']==10 and fold['valid_feedback']==0 and fnew['valid_feedback']==2 and reg['cases']==3440
for row in reg['results']:
 m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
 if 'gates-regressions' in row['manifest']['path'] or 'stage-regressions' in row['manifest']['path']:
  for child in m['results']:verify(read(Path(child['manifest']['path'])))
for name in ['companion-ref-lifecycle-after','companion-ref-feedback-after']:
 text=Path(manifests[name]['log']['path']).read_text('utf-8');assert 'Vital completion failed' not in text and 'role publication capture requires actual mutation lock' not in text
count=new['cases']+fnew['cases']+reg['cases'];assert count==3460
known=new['checks']+fnew['checks']+reg['known_checks']
run=D/'companion-ref-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[3]='Finish exact Source landing/terrain/door projection, real cross-instance showId failure semantics and remaining concurrent portal consumers. Common WORLD Ref now records actor map/instance and resolves same-role canonical actors through the existing roster; this qualifies current scene/life lookup and the tested queued Joy feedback path only. Specialized ProcHealingCompanion find/registered/sameScene, resurrection/Nature/ground owner stamps, AI/hate/poison, debit/damage mutation/publication races and all remaining factories/material/control matrices remain open. Do not infer permanent scene-ABA invalidation or every Ref consumer from current map/instance fencing; keep player/BATTLE action fences strict.'
v=dict(status='passed-scoped-WORLD-common-Ref-actor-scene-and-native-feedback-coverage-open',integrated_runtime=integrated['runtime'],runtime=runtime,parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(lifecycle_before=old,lifecycle_after=new,feedback_before=fold,feedback_after=fnew),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\n共用WORLD隨從Ref續作 `'+runtime['sha256'][:12]+'…` 同SHA **3,460案／已計數'+format(known,',')+'項檢查**。原Ref使用主人場景，主人真正傳送後又以嚴格玩家動作Binding尋找，10案重現10個場景錯記與2次原場景有效Ref遺失；新10案皆歸0。現在只對WORLD kind2沿用既有連線名冊解析同角色主人，重新核對原物件、生命token、canonical custody及物件目前場景／弱Hub與population；正常主人換圖保留原場景物件，真正換角、生命更新、成功跨場景移動與同ID替換拒絕舊Ref。場景檢查限目前map／instance，未宣稱永久場景ABA失效。\n\n另10案先在兩個原生提交worker的受控屏障前，經原生Calc與enqueueWorld送入真實PC傷害，再完成真正主人傳送／生命變動後放行；原10筆傷害皆恰提交一次並由原drain建立accepted Frame，不注入假的Frame。舊版2次有效Joy反饋被取消，新版恢復；其餘角色／生命／跨場景／替換案例不消耗舊Joy且不傷害新物件，按原抑制表核對DAMAGE action僅送物件原場景，遠端主人不收到場景動作；主人私有HP比例封包仍依原契約。所有收據／工作／Context清空，無鎖內IO、背包或主人HP／MP改寫。此為10筆佇列事件，不混稱為施法成對鎖等待。\n\n只改NativeCombatAttacker與NativePlayerCommit兩個class，新增無欄位helper。NativePlayerCommit所有原有方法的正規化bytecode逐一一致；僅另增隨從解析分支。Ref record、ClientSession、玩家Binding、NativePairedCastSafety、NativePlayerLifetime逐位元不變；WORLD live查詢維持Hub在pet之前，BATTLE沿用原分支，無新玩家欄位／工作池／時鐘／ThreadLocal／全域名冊／SQL／檔案／玩家庫存依賴；凍結輸入重建相同SHA。另對兩個實際新fixture classpath各核對23個遊戲class皆來自此JAR，唯讀載入不啟動伺服器、不另計行為案例。\n\n另3,440案原角色／傳送、跨角色、Ctrl、範圍、復活、負向、地縛、自然／背包、STOPPED及Main68／73回歸在此同一SHA通過，與歷史候選不混加。唯讀普查定位18個class、167個方法／建構／欄位入口，非額外行為案例。ProcHealingCompanion等特化主人stamp、AI／仇恨／毒傷、MP扣除及傷害提交／送包競態，連同完整Source／外控／其餘材料仍待補；131停用、超容量馴服整筆拒絕及競技／記憶體／資料限制保留。整合檢查點仍3bda9d5d的1,206案，正式檔與固定基準不變，未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-ref-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| WORLD角色生命續作 |'));lines.insert(at+1,'| WORLD共用Ref續作 | **`'+runtime['sha256'][:12]+'…`** 同SHA **3,460案**；10個主人場景錯記及2次有效Ref／Joy反饋取消歸0。10筆真實PC佇列各提交一次，角色／生命／跨場景／同ID替換拒絕舊反饋；3,440案原跨角色／Ctrl／範圍／Main回歸通過。特化Proc／AI／毒傷等完整Source餘項仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-ref-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],integrated_runtime=integrated['runtime'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-ref-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_ref_scene=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-3460-WORLD-common-Ref-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_ref_scene=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionRefScene.java','CompanionRefSceneMethods.java','CompanionRefSceneDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['companion_ref_progress']=pin(D/'companion-ref-progress-current.json');write(continuation,following)
assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_ref_progress'}==previous
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
