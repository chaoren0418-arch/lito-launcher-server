"""Seal only the measured terminal increment; retain the full Source/skill objective as open."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
current=read(R/'current.json');assert current['runtime']['sha256']=='09ea36d1ce95eb15504715d16f243c0f7f3cb6748cabb2a4cd6c6dce5b8eda4c';assert sha(Path(current['runtime']['path']))==current['runtime']['sha256'] and sha(Path(current['manifest']['path']))==current['manifest']['sha256'];checkpoint=read(Path(current['manifest']['path']));assert checkpoint['qualified_scope']['latest_same_SHA_cases']==1026 and not checkpoint['full_goal_achieved']
old=read(D/'source-summon-lifetime-audit-current.json')
for ref in old['Source']+checkpoint['protected']:assert sha(Path(ref['path']))==ref['sha256']
run=D/'source-summon-lifecycle-audits'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name);shutil.copyfile(D/'source-summon-lifetime-audit-current.json',run/'previous-audit.json');shutil.copyfile(D/'Source-lifecycle-progress-verified-current.json',run/'previous-progress.json')
receipts={}
for name in ['summon-terminal-build','summon-terminal-suite','summon-terminal-reproduction','summon-terminal-delta','summon-death-ports']:
 p=D/(name+'-current.json');v=read(p);assert v['runtime']==current['runtime'] and sha(Path(v['manifest']['path']))==v['manifest']['sha256'];assert v['status'].startswith('passed') or name in ['summon-terminal-build','summon-death-ports'];f=run/p.name;shutil.copyfile(p,f);receipts[name]=pin(f)
build=read(Path(read(D/'summon-terminal-build-current.json')['manifest']['path']))
for ref in build['frozen_inputs']:
 if ref['path'].endswith('.java'):assert sha(R/Path(ref['path']).name)==ref['sha256']
suite=read(Path(read(D/'summon-terminal-suite-current.json')['manifest']['path']));assert suite['cases']==1026
review=read(Path(read(D/'on-summon-terminal-source-drift-review-current.json')['manifest']['path']))
for row in review['Source_inputs']:
 for ref in [row['original'],row['frozen'],row['diff']]:assert sha(Path(ref['path']))==ref['sha256']
for row in suite['results']:
 for ref in [row['receipt'],row['manifest'],row['log']]:assert sha(Path(ref['path']))==ref['sha256']
 assert read(Path(row['receipt']['path']))['runtime']==current['runtime']
known=suite['known_checks'];runtime=current['runtime'];digest=runtime['sha256']
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';text=doc.read_text('utf-8').replace('目前檢查點 `420eeb7d','父層檢查點 `420eeb7d')
start=text.index('一般召喚物已另重現待修缺口：');end=text.index('\n\n完整材料',start)
text=text[:start]+f'''目前檢查點 `{digest}` 同 SHA 通過 **1,026 案／已計數 {known:,} 項檢查**，含 Main 68／73 的 10＋8 案。一般 SUMMON 的原始數字 6 與既有到期入口先執行真實 HP0 死亡，再沿用既有原子移交及地面後備；失敗時保留同一個已死亡物件、原背包及原角色／場景／生命週期收據，由既有時鐘重試。角色退役、同 ID 替換、場景切換、斷線或 Hub 關閉後不會把原背包存入新上下文。移交成功後清除收據，Source 特效 169 與移除各發送一次，沒有再次扣料或施法。新增 8 案直接指令／到期、64 案滿載／交易失敗／重試／生命週期（實際注入 56 次儲存失敗）；另 12 案以真正技能 51／154／162 封包驗證三個既有 factory 的 1,800 秒共用十秒期限及到期完整移交。[凍結逐項收據](../../tools/combat-continuation/integration-lifecycle/build/summon-terminal-suite-current.json)

此增量只改四個既有 class，新增一個無狀態 helper；既有伴隨物增加一個 volatile 清理收據，沒有新增玩家欄位、工作執行緒、排程、ThreadLocal 或全域物件登記表。原群組及 TAMED 解放方法保留，三個 factory 與兩個時鐘的精確接點已驗證，凍結輸入可重建相同 SHA。先前父層的 4 案數字 6 不清理／4 案到期孤立背包，及活體移交能力觀察均保留為差異證據，未與新版本累算。

**仍待補齊一般召喚的戰鬥致死、斷線／移位清理及已退役主人仍持有物品的完整 Source 結算；本輪沒有將這些入口視為完成。** 真實 JAR 的下一入口見唯讀 [damage／deactivate 呼叫清單](../../tools/combat-continuation/integration-lifecycle/build/summon-death-ports-current.json)，靜態清單不代表執行驗收。數字指令空白／前置零等 Source 正規化、fake 0 無限期限及不同 fake corpse 類型也仍需實際案例；跟隨、防禦、伸展、警戒回家及 hate/path AI 消費端保持開放。'''+text[end:];doc.write_text(text,'utf-8')
doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md';text=doc2.read_text('utf-8');lines=text.splitlines()
for i,line in enumerate(lines):
 if line.startswith('| 本輪增量候選 |'):lines[i]=f'| 本輪增量候選 | **`09ea36d1…`**：同 SHA 通過 **1,026 案／已計數 {known:,} 項檢查**，含 Main 68／73 的 10＋8 案；一般召喚數字 6／到期的 HP0、原背包移交、地面後備、失敗重試及特效 169／移除已補。三個實際生產入口的 Source 1,800 秒期限已驗證。群組、造屍／馴服、成對／Ctrl／範圍一併重播，凍結增量可重建相同 SHA；完整 Source／技能及最後全回歸仍開放。[索引](../../tools/combat-continuation/integration-lifecycle/current.json)、[範圍](combat-integration-lifecycle.zh-TW.md)。 |'
 if line.startswith('| 本次更新 |'):lines[i]='| 本次更新 | 新增 8 案一般召喚終止、64 案滿載／失敗重試／退役隔離（56 次儲存故障）及 12 案原始技能 51／154／162 factory 期限。戰鬥致死、斷線／移位及主人退役後的完整物品結算、指令正規化與 fake 類型、跟隨／防禦／伸展／警戒回家 AI 仍開放。保留競技與資料安全限制，未部署、未啟動遊戲、未寫 Source／玩家 SQL／WAL。 |'
text='\n'.join(lines)+'\n';text=text.replace('其 AI 消費端及一般召喚物的 HP0／背包／期限／送包仍待補。','其 AI 消費端仍待補；一般召喚數字 6／到期的 HP0／背包／期限／送包已通過本輪限定驗證，戰鬥致死及斷線／移位清理仍開放。');doc2.write_text(text,'utf-8')
v=dict(time_utc=datetime.now(timezone.utc).isoformat(),status='Source-ordinary-summon-terminal-1026-qualified-broader-death-cleanup-open',runtime=runtime,checkpoint=current['manifest'],previous=pin(run/'previous-audit.json'),Source=old['Source'],current_Main_Source_review=pin(Path(read(D/'on-summon-terminal-source-drift-review-current.json')['manifest']['path'])),receipts=receipts,next_runtime_method_audit=pin(D/'ordinary-summon-death-next-methods.txt'),qualified_scope=checkpoint['qualified_scope'],remaining=checkpoint['remaining']+['Source numeric command normalization (trimmed6/leading zero/plus forms) and fake type-specific terminal behavior need raw-case qualification'],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,native_client_launched=False)
write(run/'manifest.json',v);write(D/'source-summon-lifetime-audit-current.json',v)
proof=dict(status='passed-current-1026-receipts-live-helpers-Source-and-protected-hashes',runtime=runtime,checkpoint=current['manifest'],audit=pin(run/'manifest.json'),protected=checkpoint['protected'],live_helpers=[pin(R/name) for name in ['NativeSummonOwnerLifecycle.java','NativeSummonTerminalLifecycle.java']],docs=[pin(doc),pin(doc2)],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(D/'Source-lifecycle-progress-verified-current.json',proof);print(json.dumps(dict(status=proof['status'],runtime=runtime,cases=1026,known_checks=known,full_goal_achieved=False)))
