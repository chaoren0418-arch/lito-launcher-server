"""Promote only a reproduced candidate with a complete, SHA-matched scoped receipt set. No deployment."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];D=R/'build';B=S/'tools/resource-database/build';L=S.parents[1]/'lito登入器/LiTo381-Launcher'
read=lambda p:json.loads(p.read_text('utf-8'))
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for block in iter(lambda:f.read(1024*1024),b''):h.update(block)
 return h.hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(R/'current.json');assert parent['runtime']['sha256']=='9b8d9567da87634738b57b76d2f3e739808e1e6b328c1fd084dc3388d18eaf9c';assert sha(Path(parent['manifest']['path']))==parent['manifest']['sha256'];pm=read(Path(parent['manifest']['path']));assert pm['qualified_scope']['latest_same_SHA_cases']==1138
old_audit=read(D/'source-summon-lifetime-audit-current.json');old_progress=read(D/'Source-lifecycle-progress-verified-current.json')
for ref in old_progress['protected']+old_audit['Source']:assert sha(Path(ref['path']))==ref['sha256'],ref
relocation_index=read(D/'summon-relocation-contract-current.json');assert sha(Path(relocation_index['manifest']['path']))==relocation_index['manifest']['sha256'];relocation=read(Path(relocation_index['manifest']['path']))
for row in relocation['Source']:
 for ref in [row['original'],row['frozen']]:assert sha(Path(ref['path']))==ref['sha256'],ref
indexes={name:read(D/(name+'-current.json')) for name in ['summon-retirement-build','summon-retirement-suite','summon-retirement-reproduction','summon-retirement-lifetime','summon-retirement-retry-boundaries']};runtime=indexes['summon-retirement-build']['runtime'];assert sha(Path(runtime['path']))==runtime['sha256'];manifests={}
for name,index in indexes.items():
 assert index['runtime']==runtime and sha(Path(index['manifest']['path']))==index['manifest']['sha256'];manifests[name]=read(Path(index['manifest']['path']))
 if name!='summon-retirement-build':assert index['status'].startswith('passed')
bm=manifests['summon-retirement-build'];sm=manifests['summon-retirement-suite'];assert bm['base']==parent['runtime'] and len(bm['changed'])==8 and len(bm['fresh'])==4 and bm['new_workers_schedulers_ThreadLocals_static_registries']==0
assert sm['cases']==1206 and not sm['same_SHA_full_regression_passed'];assert manifests['summon-retirement-lifetime']['observations']['cases']==64 and manifests['summon-retirement-retry-boundaries']['observations']['cases']==4
for row in sm['results']:
 for ref in [row['receipt'],row['manifest'],row['log']]:assert sha(Path(ref['path']))==ref['sha256']
 assert read(Path(row['receipt']['path']))['runtime']==runtime
for ref in bm['inputs']:
 assert sha(Path(ref['path']))==ref['sha256'] and sha(R/Path(ref['path']).name)==ref['sha256']
run=D/'checkpoints'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);continuation=C/'continuation-current.json';ch=sha(continuation);ih=sha(R/'current.json');shutil.copyfile(R/'current.json',run/'previous-integration.json');shutil.copyfile(continuation,run/'previous-continuation.json');shutil.copyfile(D/'source-summon-lifetime-audit-current.json',run/'previous-audit.json');shutil.copyfile(D/'Source-lifecycle-progress-verified-current.json',run/'previous-progress.json');shutil.copyfile(__file__,run/Path(__file__).name);refs={}
shutil.copyfile(D/'summon-retirement-progress-current.json',run/'previous-retirement-progress.json');shutil.copyfile(D/'summon-relocation-contract-current.json',run/'summon-relocation-contract-current.json')
for name in indexes:
 p=run/(name+'-current.json');shutil.copyfile(D/p.name,p);refs[name]=pin(p)
remaining=[
 'Relocation with no companion landing remains open: Source Teleportation:404/752 and CheckUtil:632 preserve the live original NPC on failed placement, rather than death/deletion/item transfer. The two World cases of the six-case 9b baseline require original scene/instance/placement ownership and viewer semantics; see summon-relocation-contract-current.json. Do not wire rejected relocation into logout/ground retirement.',
 'Validate real socket logout and role selection and broader native Main lifetime with pending summon retirement; direct actual Hub.disconnect/shutdown/close fixtures and the existing Main 68/73 probes are scoped evidence.',
 'Complete every remaining material/class/target/Ctrl/area/effect/Source-control matrix and all offensive/NPC lethal producers; static census or four ordinary skill packet paths are not full coverage.',
 'Trimmed/leading-zero numeric summon commands, fake corpse types, actual follow/defense/extend/guard-home AI and full hate/path semantics remain open.',
 'Source NPC initial loot/death/drop provenance, unbounded inventory capacity/long counts, broader factories/executors/expiry and forced relocation/LOS; retain entire over-capacity tame rejection.',
 'Final same-SHA affected/composed/full regression, complete frozen chain rebuild and readonly Source startup/close, then the concentrated final human native-UI checklist; no deployment is authorized.'
]
scope=dict(latest_same_SHA_cases=1206,latest_same_SHA_known_checks=sm['known_checks'],latest_check_count_complete=False,latest_Main_skill68_cases=10,latest_Main_skill73_cases=8,parent_same_SHA_cases=1138,parent_same_SHA_known_checks=27270,retirement_cases=64,retry_boundary_cases=4,injected_owner_flush_faults=52,injected_nested_ground_faults=16,worker_repository_calls=0,new_workers_schedulers_ThreadLocals_static_registries=0,added_instance_fields=dict(BattleCompanion='one weak-scene canonical fallback Stage',NativePlayerLifetime='one lifetime-owned in-flight retirement work set',ReturnNatureTarget='one ephemeral deletion context marker'),qualification='Actual pre-disconnect owner-first return; canonical no-player-inventory ground worker; queue/timer rejection, real STOPPED/drain shutdown completion fence, role/scene/actor end and duplicate deletion. Existing paired/Ctrl/area, group, zombie, tame and Main probes replayed at this exact SHA. Full task and final regression remain open.')
v=dict(time_utc=datetime.now(timezone.utc).isoformat(),status='integrated-Source-summon-logout-ground-worker-lifetime-coverage-open',runtime=runtime,receipts=refs,parent_integration=pin(run/'previous-integration.json'),previous_continuation=pin(run/'previous-continuation.json'),protected=old_progress['protected'],Source_readonly_references=old_audit['Source'],qualified_scope=scope,remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,native_client_launched=False)
v['Source_relocation_references']=relocation['Source'];v['next_relocation_contract']=pin(run/'summon-relocation-contract-current.json');write(run/'manifest.json',v);assert sha(continuation)==ch and sha(R/'current.json')==ih,'concurrent continuation update';write(R/'current.json',dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False));prior=read(continuation);prior.update(time_utc=v['time_utc'],status=v['status'],integration_lifecycle=pin(R/'current.json'),integration_lifecycle_candidate=runtime,integration_lifecycle_candidate_full_regression_passed=False);write(continuation,prior)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';text=doc.read_text('utf-8').replace('目前檢查點 `9b8d9567','父層檢查點 `9b8d9567');text+='\n\n目前召喚物退休檢查點 `'+runtime['sha256'][:12]+'…` 同 SHA 通過 **1,206 案／已計數 '+format(sm['known_checks'],',')+' 項檢查**，含 Main 68／73 的 10＋8 案。新增 64 案真正 Hub 斷線／shutdown／close、主人優先返還、工作池及計時器拒絕、STOPPED、角色／場景／物件退役與重複清理，另 4 案驗證生命週期尚未安裝的早到時鐘及失敗後重新滿載的重試節流；實際注入 52 次主人提交失敗、16 次三層落地預約失敗，worker Repository 呼叫為零。前段 `1ad4cd9c…` 的 48 案僅為歷史未接線元件證據，未累加到本輪。\n\n斷線在原 synchronized Hub 移除主人的容器之前取得清理責任；能返還時沿用原子主人交易及地面後備，失敗交由既有工作池及計時器持有同一背包，沒有退役後玩家背包／JDBC 存取。Source 普通刪除保留真實 HP；死亡仍走既有 HP0 路徑。地面完成後只發布一次 Source169／移除，場景或物件世代終止則取消工作並釋放引用。新增欄位限原伴隨物、短期 Target 及既有 application lifetime 各一項；沒有新增玩家欄位、執行緒、排程器、ThreadLocal 或全域登錄表。精確八個既有／四個新增 class 差分與凍結重建同 SHA 已驗證。移位無落點、實際 socket／換角與更廣 Main 退休、其餘材料技能／Source／AI 與最終全回歸仍開放；競技、超容量整筆拒絕及 SQL／WAL 限制保留，未部署。[逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-retirement-suite-current.json)\n';doc.write_text(text,'utf-8')
doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md';lines=doc2.read_text('utf-8').splitlines();updates={
 '| 本輪增量候選 |':'| 本輪增量候選 | **`'+runtime['sha256'][:12]+'…`**：同 SHA 通過 **1,206 案／已計數 '+format(sm['known_checks'],',')+' 項檢查**，含 Main 68／73 的 10＋8 案；新增登出 owner-first／純落地 worker／佇列及計時器拒絕／STOPPED／shutdown／場景與世代清理。跨主人單體／直線／範圍、Ctrl、群組、造屍／馴服及成對回歸已重播。凍結重建及八個既有／四個新增 class 隔離通過；完整技能／Source 與最終全回歸仍開放。[索引](../../tools/combat-continuation/integration-lifecycle/current.json)。 |',
 '| 本次更新 |':'| 本次更新 | 新增退休 64 案與重試邊界 4 案，52 次主人交易、16 次三層落地故障、worker Repository 零呼叫。失敗保留原背包，既有生命週期持有工作，場景結束取消並釋放引用；Source 刪除不偽造 HP0。移位無落點、真實 socket／換角與更廣 Main 退休及其餘技能／外控矩陣仍開放。未部署、未啟動遊戲、未寫 Source／玩家 SQL／WAL。 |',
 '| 尚未接線的續作 |':'| 退休接線界線 | 歷史 `1ad4cd9c…` 元件 48 案未累算；登出與工作持有已在本輪接線。父版六個原始缺口中的 World 兩個無落點移位案例仍開放，不能將斷線證據當成傳送驗收。競技、131 停用及超容量整筆拒絕保全資料的規則保留。 |'}
for i,line in enumerate(lines):
 for prefix,new in updates.items():
  if line.startswith(prefix):lines[i]=new
doc2.write_text('\n'.join(lines)+'\n','utf-8')
write(D/'summon-retirement-progress-current.json',dict(status=v['status'],runtime=runtime,integrated_runtime=runtime,manifest=pin(run/'manifest.json'),previous=pin(run/'previous-retirement-progress.json'),full_goal_achieved=False))
audit=dict(old_audit);audit.update(time_utc=v['time_utc'],status=v['status'],runtime=runtime,checkpoint=pin(run/'manifest.json'),previous=pin(run/'previous-audit.json'),qualified_scope=scope,retirement_handoff_remaining=remaining,integrated_retirement_receipts=refs,current_Main_Source_review=next(row['manifest'] for row in sm['results'] if row['name']=='on-summon-retirement-source-drift-review'));write(D/'source-summon-lifetime-audit-current.json',audit)
proof=dict(old_progress);proof.update(status='passed-integrated-1206-summon-retirement-Source-and-protected-seal',runtime=runtime,checkpoint=pin(run/'manifest.json'),previous_progress=pin(run/'previous-progress.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),live_helpers=[pin(R/n) for n in ['NativeSummonOwnerLifecycle.java','NativeSummonTerminalLifecycle.java','NativeSummonGroundTransfer.java','NativeSummonRetirement.java']],docs=[pin(doc),pin(doc2)]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
print(json.dumps(dict(status=v['status'],runtime=runtime,cases=1206,known_checks=sm['known_checks'],full_goal_achieved=False)))
