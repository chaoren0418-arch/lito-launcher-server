"""Seal measured combat-death semantics and preserve all broader Source/material obligations."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
current=read(R/'current.json');assert current['runtime']['sha256']=='9b8d9567da87634738b57b76d2f3e739808e1e6b328c1fd084dc3388d18eaf9c';assert sha(Path(current['runtime']['path']))==current['runtime']['sha256'] and sha(Path(current['manifest']['path']))==current['manifest']['sha256'];checkpoint=read(Path(current['manifest']['path']));assert checkpoint['qualified_scope']['latest_same_SHA_cases']==1138 and not checkpoint['full_goal_achieved']
old=read(D/'source-summon-lifetime-audit-current.json')
for ref in old['Source']+checkpoint['protected']:assert sha(Path(ref['path']))==ref['sha256']
suite=read(Path(read(D/'summon-combat-suite-current.json')['manifest']['path']));assert suite['cases']==1138
for row in suite['results']:
 for ref in [row['receipt'],row['manifest'],row['log']]:assert sha(Path(ref['path']))==ref['sha256']
 assert read(Path(row['receipt']['path']))['runtime']==current['runtime']
review_index=read(D/'on-summon-combat-source-drift-review-current.json');review=read(Path(review_index['manifest']['path']))
for row in review['Source_inputs']:
 for ref in [row['original'],row['frozen'],row['diff']]:assert sha(Path(ref['path']))==ref['sha256']
build=read(Path(read(D/'summon-combat-build-current.json')['manifest']['path']))
for ref in build['frozen_inputs']:
 if ref['path'].endswith('.java'):assert sha(R/Path(ref['path']).name)==ref['sha256']
run=D/'source-summon-lifecycle-audits'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name);shutil.copyfile(D/'source-summon-lifetime-audit-current.json',run/'previous-audit.json');shutil.copyfile(D/'Source-lifecycle-progress-verified-current.json',run/'previous-progress.json');receipts={}
for name in ['summon-combat-build','summon-combat-suite','summon-combat-reproduction','summon-combat-delta','summon-combat-before']:
 p=D/(name+'-current.json');v=read(p);assert v['runtime']==(build['base'] if name.endswith('-before') else current['runtime']) and sha(Path(v['manifest']['path']))==v['manifest']['sha256'];f=run/p.name;shutil.copyfile(p,f);receipts[name]=pin(f)
known=suite['known_checks'];runtime=current['runtime'];digest=runtime['sha256'];doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';text=doc.read_text('utf-8').replace('目前檢查點 `09ea36d1','父層檢查點 `09ea36d1')
start=text.index('**仍待補齊一般召喚的戰鬥致死、');end=text.index('\n\n完整材料',start)
text=text[:start]+f'''目前檢查點 `{digest}` 同 SHA 通過 **1,138 案／已計數 {known:,} 項檢查**，包含 Main 68／73 的 10＋8 案。新增 WORLD 受擊與 BATTLE 傷害回饋兩個實際共用核心的 8 案、技能 4／17／25 單體／直線／範圍跨主人及一般／Ctrl 原始封包 24 案、滿載地面後備／交易失敗／重試／角色與場景／生命週期／STOPPED 的 80 案；其中實際注入 64 次儲存失敗、8 案在已受理動作中切至 STOPPED 後仍完成結算。父版兩場景的 8 個背包未移交差異歸零。[凍結逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-combat-suite-current.json)

一般 SUMMON 受擊致死先保留原背包與死亡世代，再沿用原子移交／地面後備及 Source169／移除；成功後略過舊保留屍體動畫與重複移除。BATTLE 的呼叫者可為施法者，依既有同場景名冊及物件身分找主人，使用原本已受理的施法者收據與雙方鎖完成物品交易，不另受理一個施法。失敗保留原主人收據及原背包，由既有時鐘重試；已死亡後施法者退役不會取消仍有效的主人結算。直線技能也命中玩家時，原始封包驗證會等待玩家傷害工作及輸出收據都歸零，再判定鎖外送包與清理結果。

此增量只改四個既有 class，沒有新 class、欄位、工作執行緒、排程、ThreadLocal 或登記表；兩個傷害呼叫與兩個末端判斷的精確接點、既有作用域的兩個存取入口已比對。原主人／到期、群組、PET／TAMED 路徑及所有其餘封存項目保留，凍結輸入可重建相同 SHA。**這仍不是完整 Source／全部材料技能驗收。** 斷線／移位與主人退役後仍持有物品的最終結算（Source worker deleteMe 僅落地、logout deleteAfterInventoryReturn 優先還主人，兩者不能混用或讓 NPC worker 進玩家 JDBC）、其餘真正 NPC 致死生產端、全部攻擊與材料技能的施法受理／職業／目標／Ctrl／範圍／延後 I/O 矩陣、指令空白／前置零正規化、fake 類型，以及跟隨／防禦／警戒回家／hate/path AI 均保持開放。下一個實際 JAR 的[方法快照](../../tools/combat-continuation/integration-lifecycle/build/summon-combat-next-methods.txt)為唯讀稽核，沒有將靜態呼叫清單當作執行驗收。'''+text[end:];doc.write_text(text,'utf-8')
doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md';lines=doc2.read_text('utf-8').splitlines()
for i,line in enumerate(lines):
 if line.startswith('| 本輪增量候選 |'):lines[i]=f'| 本輪增量候選 | **`9b8d9567…`**：同 SHA 通過 **1,138 案／已計數 {known:,} 項檢查**，含 Main 68／73 的 10＋8 案；一般召喚戰鬥致死已接兩個共用核心，4／17／25 單體／直線／範圍的跨主人／Ctrl 原始封包及失敗重試、地面後備、退役隔離、受理後 STOPPED 結算已測。原指令／到期、群組、造屍／馴服、成對／Ctrl／範圍亦重播。凍結增量可重建相同 SHA；完整技能／Source 及最後全回歸仍開放。[索引](../../tools/combat-continuation/integration-lifecycle/current.json)、[範圍](combat-integration-lifecycle.zh-TW.md)。 |'
 if line.startswith('| 本次更新 |'):lines[i]='| 本次更新 | 新增致死核心 8 案、原始單體／直線／範圍跨主人／Ctrl 24 案、滿載／故障／生命週期／STOPPED 80 案（64 次儲存失敗、8 案沿用受理收據完成 STOPPED 結算）。斷線／移位及主人退役後的最終物品結算、其餘致死生產端與完整材料／外控矩陣、指令正規化／fake 類型、AI 消費端仍開放。未部署、未啟動遊戲、未寫 Source／玩家 SQL／WAL，保留競技與資料安全限制。 |'
 if line.startswith('| 材料進度 |'):lines[i]=line.replace('本輪 1,026 案','本輪 1,138 案')
text='\n'.join(lines)+'\n';text=text.replace('一般召喚數字 6／到期的 HP0／背包／期限／送包已通過本輪限定驗證，戰鬥致死及斷線／移位清理仍開放。','一般召喚指令／到期及兩個共用傷害核心的 HP0／背包／期限／送包已通過限定驗證；斷線／移位、其餘致死生產端及完整矩陣仍開放。');doc2.write_text(text,'utf-8')
v=dict(time_utc=datetime.now(timezone.utc).isoformat(),status='Source-ordinary-summon-combat-1138-qualified-disconnect-relocation-and-full-matrices-open',runtime=runtime,checkpoint=current['manifest'],previous=pin(run/'previous-audit.json'),Source=old['Source'],current_Main_Source_review=review_index['manifest'],receipts=receipts,next_runtime_method_audit=pin(D/'summon-combat-next-methods.txt'),next_cleanup_contract=dict(Source_worker_deleteMe='ground-only; never enter player JDBC',Source_logout_deleteAfterInventoryReturn='owner-first with ground fallback under owner action lock',failed_cleanup='retain canonical bag and retry; do not discard on actor retirement'),qualified_scope=checkpoint['qualified_scope'],remaining=checkpoint['remaining'],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,native_client_launched=False)
write(run/'manifest.json',v);write(D/'source-summon-lifetime-audit-current.json',v);proof=dict(status='passed-current-1138-receipts-live-helpers-Source-and-protected-hashes',runtime=runtime,checkpoint=current['manifest'],audit=pin(run/'manifest.json'),protected=checkpoint['protected'],live_helpers=[pin(R/name) for name in ['NativeSummonOwnerLifecycle.java','NativeSummonTerminalLifecycle.java']],docs=[pin(doc),pin(doc2)],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(D/'Source-lifecycle-progress-verified-current.json',proof);print(json.dumps(dict(status=proof['status'],runtime=runtime,cases=1138,known_checks=known,full_goal_achieved=False)))
