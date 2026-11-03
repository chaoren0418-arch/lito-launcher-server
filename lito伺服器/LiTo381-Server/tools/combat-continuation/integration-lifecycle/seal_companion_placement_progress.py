"""Seal actual portal gaps and an unwired Source search increment; never promote partial scene semantics."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'))
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for block in iter(lambda:f.read(1024*1024),b''):h.update(block)
 return h.hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
current=read(R/'current.json');ih=sha(R/'current.json');continuation=C/'continuation-current.json';ch=sha(continuation);assert current['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df';assert sha(Path(current['manifest']['path']))==current['manifest']['sha256'];checkpoint=read(Path(current['manifest']['path']));assert checkpoint['qualified_scope']['latest_same_SHA_cases']==1206
audit=read(D/'source-summon-lifetime-audit-current.json');proof=read(D/'Source-lifecycle-progress-verified-current.json')
for ref in proof['protected']+audit['Source']+proof['docs']+proof['live_helpers']:assert sha(Path(ref['path']))==ref['sha256'],ref
indexes={name:read(D/(name+'-current.json')) for name in ['companion-landing-build','companion-landing-search','companion-landing-reproduction','companion-portal-before']};component=indexes['companion-landing-build']['runtime'];manifests={}
for name,index in indexes.items():
 assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];assert index['runtime']==(current['runtime'] if name=='companion-portal-before' else component);manifests[name]=read(Path(index['manifest']['path']))
assert indexes['companion-landing-search']['status'].startswith('passed') and indexes['companion-landing-reproduction']['status'].startswith('passed');bm=manifests['companion-landing-build'];search=manifests['companion-landing-search']['observations'];portal=manifests['companion-portal-before']['observations'];reproduction=manifests['companion-landing-reproduction'];assert bm['base']==current['runtime'] and bm['changed']==[] and len(bm['fresh'])==2 and not bm['production_dispatch_wired'];assert search['cases']==16518 and search['Source_commit_failures']==8 and portal['cases']==12 and portal['blocked_landing_inactive_actors']==8 and portal['portal_owner_tokens_changed']==12
for row in bm['Source']:
 for ref in row.values():assert sha(Path(ref['path']))==ref['sha256']
for ref in bm['inputs']:assert sha(Path(ref['path']))==ref['sha256'] and sha(R/Path(ref['path']).name)==ref['sha256']
run=D/'companion-placement-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in indexes:
 p=run/(name+'-current.json');shutil.copyfile(D/p.name,p);refs[name]=pin(p)
for source,name in [(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(D/'Source-lifecycle-progress-verified-current.json','previous-progress.json'),(continuation,'previous-continuation.json'),(Path(__file__),Path(__file__).name)]:shutil.copyfile(source,run/name)
remaining=[
 'Attach authoritative actor map/instance/population identity before the first completeClassicWorldPortal NativePlayerCommit.scene write; all three scene writes advance the owner action token even when map/instance values are unchanged.',
 'Keep rejected relocation alive at its original position and scene with canonical inventory. Do not treat no landing as death, logout or an item-transfer request. The full blocked radius is covered separately from the original sixteen-cell search gap.',
 'Revalidate and commit landing under existing actor/Hub movement ownership with Source passability, at least one walk exit, live-actor/NPC/door occupancy, original scene generation and same-ID replacement checks. Terrain acceptAndRelocate is a port, not a qualified native adapter.',
 'Rewrite scene-specific snapshot, occupancy, target, publication, lifetime, loot/death and old-scene ownership consumers together. The exact 61 snapshot callsites and four single-snapshot readers require review; the census is not proof of complete field/scene coverage.',
 'Do not bind a persistent owner solely to NativePlayerCommit.Binding.owner(): legitimate portal changes it. Integrate a verified before/after relocation authority while still rejecting actual role/connection retirement and stale callbacks, without player references in detached jobs or a new global registry.',
 'Qualify original/destination observers, stale role output, actual Source null/failed landing, retries, simultaneous walking/teleporting and final Main shutdown; retain all remaining skill/Source/AI, capacity/provenance and full regression gates.'
]
v=dict(status='passed-unwired-Source-landing-search-with-12-actual-portal-baseline-scene-integration-open',integrated_runtime=current['runtime'],integrated_checkpoint=current['manifest'],unwired_runtime=component,receipts=refs,Source=bm['Source'],observations=dict(portal=portal,search=search),scene_consumer_census=reproduction['scene_consumer_census'],scene_consumer_counts=reproduction['scene_consumer_counts'],readonly_runtime_methods=[pin(D/'companion-placement-ports.txt'),pin(D/'companion-placement-identity-ports.txt')],remaining=remaining,production_dispatch_wired=False,scene_consumer_rewrite_complete=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);assert sha(R/'current.json')==ih and sha(continuation)==ch,'concurrent integration/continuation change'
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';text=doc.read_text('utf-8');text+='\n\n傳送續作已用現行 `3bda9d5d…` 的真正 `C_GOTO_PORTAL` 重現 **12 案／4,198 檢查**：兩種物品套用模式、同圖／跨圖、原 16 格受阻、整個 Source 搜尋範圍受阻及正常落點。8 案留下被停用的原召喚物與同一背包；正常 4 案保留活體。12 案都更新主人動作 token，即使地圖／副本不變也一樣；只有停用的 8 案更新伴隨物 token。未放寬副本編號限制，跨圖案例使用真正支援的 4→0 地圖。最初以不相容的高 HP／MP 測試初始化及普通地圖的非合法副本編號造成的失敗，均保留原始紀錄；正式案例沿用原生重建生命值並通過原有地圖規則。\n\nSource `CheckUtil.findNearestLivingClearLanding` 的完整方法體另抽成獨立參考，只投影原子落點提交入口及日誌；未重寫中心／外圈順序、半徑或例外邊界。尚未接線的 `'+component['sha256'][:12]+'…` 搜尋核心通過 **16,518 組／68,125 檢查**（8 次提交失敗），包含每格唯一可用、所有格受阻、半徑外格、連續多物件占格及完整候選座標軌跡。父版全部封存項目不變，只有兩個無欄位 class，沒有玩家／Hub／SQL／檔案／執行緒／排程／ThreadLocal／全域登錄表依賴；凍結輸入可重建相同 SHA。此為未接線搜尋端口，原生地形、占格、場景持有與並發原子提交尚未驗收。\n\n實際 JAR 已盤出 61 個伴隨物快照呼叫、4 個單一快照讀取、4 個占格呼叫與唯一 portal 移位入口；包括跨角色／範圍材料、復活、回歸自然、治癒、普攻、視野、NPC 目標、期限與掉落等消費端。必須一併處理原場景物件位置與主人合法傳送收據，不能僅略過 deactivate 讓舊物件被新圖查詢。上述 12 案差異及搜尋核心案例均未併入整合版 1,206 案，也未宣告傳送或完整 Source 已完成。[獨立收據及消費端清單](../../tools/combat-continuation/integration-lifecycle/build/companion-placement-progress-current.json)\n';doc.write_text(text,'utf-8')
doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md';lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| 退休接線界線 |'));lines.insert(at+1,'| 傳送續作／未接線核心 | 現行候選真正 portal 12 案重現 8 個停用原物件／背包缺口，12 案均更新主人收據；另 `'+component['sha256'][:12]+'…` Source 搜尋核心通過 16,518 組／68,125 檢查與同 SHA 重建。尚須整合原場景、占格、視野及 61 個快照呼叫等消費端，未改整合檢查點、未併入 1,206 案。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-placement-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
progress=dict(status=v['status'],integrated_runtime=current['runtime'],unwired_runtime=component,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-placement-progress-current.json',progress)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_placement=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-unwired-Source-landing-evidence',previous_progress=pin(run/'previous-progress.json'),pending_companion_placement=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
continuing=read(continuation);continuing['companion_placement_progress']=pin(D/'companion-placement-progress-current.json');write(continuation,continuing);assert sha(R/'current.json')==ih,'integrated checkpoint changed';print(json.dumps(dict(status=v['status'],integrated_SHA=current['runtime']['sha256'],unwired_SHA=component['sha256'],portal_cases=12,Source_search_cases=16518,full_goal_achieved=False)))
