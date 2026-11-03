"""Seal only the 161 NPC cancellation-control increment, without promotion."""
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
names=['companion-forest-cancellation-build','companion-forest-cancellation-reproduction','companion-forest-cancellation-before','companion-forest-cancellation-after','companion-forest-cancel-regressions-after'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]];assert runtime['sha256']=='f213c87f8ee21f89037f4d316b61555bff394f244e56662cd5dcdaab5074a96a'
assert build['base']['sha256']=='c6611ff7b0099854d4641eb67782fd2095759cb4fbe86078e0fbaf6963c633ea'
for name in names:assert indices[name]['runtime']==(build['base'] if name.endswith('-before') else runtime),name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert build['changed']==['local/lito381/server/BattleCompanion.class'] and build['fresh']==['local/lito381/server/NativeCompanionForestCancellation.class']
before=manifests['companion-forest-cancellation-before']['observations'];after=manifests['companion-forest-cancellation-after']['observations'];legacy=manifests['companion-forest-cancel-regressions-after']['observations']
assert before['cases']==after['cases']==48 and before['unprotected_area_silence_left']==24 and after['unprotected_area_silence_left']==0 and after['actual_kinds']==3
assert legacy['cases']==64 and legacy['locked_writes']==0
count=after['cases']+legacy['cases'];checks=after['checks']+legacy['checks']
run=D/'companion-forest-cancellation-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining'])+['On f213c87f, actual raw skill44 now removes unprotected actor skill161, preserving configured161 immunity. This closes that specific NPC cancellation flag gap. Other cancellation/Source controls and NPC spell/AI consumers, refresh/expiry/reset/factories, skill167 and the remaining material matrices plus final exact-SHA full regression remain open. The parent c6611ff7 985 cases and this 112-case increment are separate SHA scopes and are not summed.']
v=dict(status='passed-scoped-area161-NPC-cancellation-immunity-and-Ctrl-Source-coverage-open',runtime=runtime,parent=build['base'],integrated_runtime=integrated['runtime'],receipts=refs,qualified_scope=dict(cases=count,known_checks=checks,check_count_complete=True),observations=dict(before=before,after=after,original_companion_cancellation=legacy),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,all_Source_semantics_verified=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\n技能161 NPC相消外控續作 `'+runtime['sha256'][:12]+'…` 同SHA **112案／'+format(checks,',')+'項檢查**。真正PET／SUMMON／TAMED、自己／他人、正常／Ctrl與161相消免疫開／關的48案以原始技能44封包對照：父層24次未受保護的161殘留，新候選全部清除，免疫開啟者保留；原相消仍取消非免疫加速並只扣一次材料／MP。另64案凍結原NPC相消回歸通過，鎖內送包0。\n\n只把BattleCompanion原cancelMagicEffects的一個既有呼叫接至無欄位helper，先執行原173／174處理，再依既有cancellationImmune清單清除161；不新增時鐘、註冊表、狀態欄位或執行緒。父層其餘JAR及全部原方法／欄位保留，獨立逆向稽核與凍結重建同SHA。父層c6611ff7的985案含Main68／73，與此112案分開記錄。161其餘消費端／狀態與出生矩陣、167及其餘材料／完整Source外控仍開放。整合檢查點及正式檔不動，競技／記憶體／131／容量與資料限制保留；未部署，Source／玩家SQL／WAL寫入0。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-cancellation-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,l in enumerate(lines) if l.startswith('| 技能161 NPC／Ctrl成對生命週期 |'));lines.insert(at+1,'| 技能161 NPC相消外控 | **`'+runtime['sha256'][:12]+'…`** 同SHA **112案**；原始44正常／Ctrl於三種真正NPC與自己／他人遵守161免疫設定，修復24次非免疫殘留。其餘Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-cancellation-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
write(D/'companion-forest-cancellation-progress-current.json',dict(status=v['status'],runtime=runtime,integrated_runtime=integrated['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False))
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_forest_cancellation=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-112-area161-cancellation-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_forest_cancellation=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/n) for n in ['NativeCompanionForestCancellation.java','CompanionForestCancellationMethods.java','CompanionForestCancellationDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['companion_forest_cancellation_progress']=pin(D/'companion-forest-cancellation-progress-current.json');write(continuation,following);assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_forest_cancellation_progress'}==previous
write(D/'companion-forest-cancellation-wip-current.json',dict(status='sealed-scoped-all-native-fixtures-terminal',runtime=runtime,progress=pin(D/'companion-forest-cancellation-progress-current.json'),remaining=remaining,full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],cases=count,known_checks=checks,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
