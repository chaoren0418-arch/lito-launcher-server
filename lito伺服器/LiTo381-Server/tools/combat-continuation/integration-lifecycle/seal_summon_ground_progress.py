"""Record an unwired worker-transfer capability; never promote it to integrated retirement qualification."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
current=read(R/'current.json');current_hash=sha(R/'current.json');continuation=R.parent/'continuation-current.json';continuation_hash=sha(continuation);assert current['runtime']['sha256']=='9b8d9567da87634738b57b76d2f3e739808e1e6b328c1fd084dc3388d18eaf9c';assert sha(Path(current['runtime']['path']))==current['runtime']['sha256'];checkpoint=read(Path(current['manifest']['path']));assert checkpoint['qualified_scope']['latest_same_SHA_cases']==1138
old=read(D/'source-summon-lifetime-audit-current.json');proof=read(D/'Source-lifecycle-progress-verified-current.json')
for ref in old['Source']+proof['protected']+proof['docs']:assert sha(Path(ref['path']))==ref['sha256']
build=read(D/'summon-ground-build-current.json');runtime=build['runtime'];assert sha(Path(runtime['path']))==runtime['sha256'];bm=read(Path(build['manifest']['path']));assert bm['base']==current['runtime'] and bm['changed']==[] and len(bm['fresh'])==2 and bm['production_entrypoints_changed']==0 and not bm['retirement_dispatch_wired']
for ref in bm['inputs']:
 assert sha(Path(ref['path']))==ref['sha256'];assert sha(R/Path(ref['path']).name)==ref['sha256']
run=D/'summon-retirement-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name);shutil.copyfile(D/'source-summon-lifetime-audit-current.json',run/'previous-audit.json');shutil.copyfile(D/'Source-lifecycle-progress-verified-current.json',run/'previous-progress.json');refs={};results={}
for name in ['summon-ground-build','summon-ground-capability','summon-ground-fault-lifetime','summon-ground-reproduction','summon-retirement-before','summon-retirement-audit']:
 p=D/(name+'-current.json');v=read(p);assert sha(Path(v['manifest']['path']))==v['manifest']['sha256'];assert v['runtime']==(current['runtime'] if name.startswith('summon-retirement-') else runtime);q=run/p.name;shutil.copyfile(p,q);refs[name]=pin(q);m=read(Path(v['manifest']['path']))
 if 'observations' in m:results[name]=m['observations']
 if name in ['summon-ground-capability','summon-ground-fault-lifetime','summon-ground-reproduction']:assert v['status'].startswith('passed')
assert results['summon-ground-capability']['cases']==4 and results['summon-ground-fault-lifetime']['cases']==44 and results['summon-ground-fault-lifetime']['repository_calls']==0 and results['summon-ground-fault-lifetime']['injected_reservation_faults']==12;assert results['summon-retirement-before']['cases']==6 and results['summon-retirement-before']['orphaned_expiry_bags']==6
checks=sum(results[name]['checks'] for name in ['summon-ground-capability','summon-ground-fault-lifetime']);assert checks==692
remaining=[
 'Wire authoritative deletion/retirement claims and owner-first logout before profile retirement; the new worker primitive only moves canonical inventory to ground and intentionally leaves actor registration/retirement to its owner.',
 'Reserve and retain cleanup ownership before removing the original actor/master containers. Resolve enqueue/retry rejection, pending nonempty bags and scene-end/shutdown behavior through existing lifecycle infrastructure; no new global actor registry, player SQL/WAL or silent item discard.',
 'Do not allow original cleanupClassicWorldTransientCompanionsOnDisconnect to advance the captured generation again or remove failed SUMMON bags before handoff. Actual GameHub.disconnect releases source item owner and ephemeral role/account profiles.',
 'Preserve BATTLE room/match/rules identity and World population identity; no late old-role deposit into replacement scenes. Integrate no-landing relocation with the correct pre-transition origin/destination contract.',
 'Verify real socket logout, role switch, last member/room reset, World instance close, retained STOPPED work, queue rejection and final Main close; component tests and static caller inventory are not end-to-end qualification.',
 'All remaining material/class/target/Ctrl/area/effect/Source-control matrices, fake types, AI consumers, NPC provenance/capacity and final full verification remain open.'
]
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';text=doc.read_text('utf-8');text+='''

續作中的 worker 純落地元件另以 `1ad4cd9c…` 通過 **48 案／692 檢查**：4 案基本交易、44 案故障及生命週期，含 12 次三層預約／世代／場景失敗、實際 Hub 斷線及關閉、主人換角／移場、晚到觀看者退役與弱參照失效。玩家 Repository 呼叫為零；原背包原地排空、HP 不偽造為零，落地保留原數量與資料；失敗保留原背包並回滾預約。場景資料只以弱參照及原身份持有，沒有增加執行緒、排程、ThreadLocal 或登記表。父版全部封存項目逐位元相同，只新增兩個未接線 class，凍結輸入可重建相同 SHA。

**此元件尚未接入正式派送或退休入口，不能宣告斷線／移位清理完成，也沒有把這 48 案與 `9b8d9567…` 的 1,138 案累算。** 父版實際清理／Hub 斷線及 16 個落點由真正伴隨物占用的移位入口，已重現 6 案背包仍留在不可用舊物件。接續須在移除舊持有者之前受理並持有清理工作；logout 優先還主人，worker 僅落地且禁止玩家 JDBC，並補齊排程拒絕／失敗重試、末位成員與場景關閉及原生 Main 的完整生命週期。現行整合檢查點維持 `9b8d9567…`，未部署、未寫 Source／玩家 SQL／WAL。[元件與原始缺口收據](../../tools/combat-continuation/integration-lifecycle/build/summon-retirement-progress-current.json)
''';doc.write_text(text,'utf-8')
doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md';text=doc2.read_text('utf-8');needle='| 本次更新 |';lines=text.splitlines();at=next(i for i,line in enumerate(lines) if line.startswith(needle));lines.insert(at+1,'| 尚未接線的續作 | `1ad4cd9c…` 的 worker 純落地交易元件通過 48 案／692 檢查、12 次故障與零玩家 Repository 呼叫；全數父版封存項目不變，可重建相同 SHA。原清理／斷線／真正占滿 16 落點的移位入口已重現 6 案不可用舊物件背包。退休工作持有與派送、logout 還主人、場景／角色退休及佇列拒絕仍待接線；沒有替換整合檢查點或把不同 SHA 案例累算。[收據](../../tools/combat-continuation/integration-lifecycle/build/summon-retirement-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
v=dict(status='passed-unwired-worker-ground-48-capability-retirement-handoff-still-open',integrated_runtime=current['runtime'],unwired_runtime=runtime,receipts=refs,observations=results,unwired_capability_cases=48,unwired_capability_checks=checks,baseline_inaccessible_bag_cases=6,integrated_checkpoint_unchanged=True,retirement_dispatch_wired=False,Source=old['Source'],owner_token_readonly_audit=pin(D/'summon-ground-owner-token-audit.txt'),next=remaining,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v);write(D/'summon-retirement-progress-current.json',dict(status=v['status'],integrated_runtime=current['runtime'],unwired_runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False))
audit=dict(old);audit.update(time_utc=datetime.now(timezone.utc).isoformat(),status='Source-integrated-1138-with-unwired-ground-48-capability-retirement-open',previous=pin(run/'previous-audit.json'),unwired_worker_ground=pin(run/'manifest.json'),retirement_handoff_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1138-unchanged-plus-separate-unwired-ground-48-evidence',previous_progress=pin(run/'previous-progress.json'),unwired_worker_ground=pin(run/'manifest.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
assert sha(R/'current.json')==current_hash and sha(continuation)==continuation_hash,'integrated pointers changed';print(json.dumps(dict(status=v['status'],integrated_runtime=current['runtime'],unwired_runtime=runtime,component_cases=48,checks=checks,full_goal_achieved=False)))
