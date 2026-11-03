"""Record authoritative product clarification without rewriting historical runtime receipts."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath);continuation=read(C);key='competition_only_product_scope';assert key not in continuation
for kind in ['protected','live_helpers','experimental_live_helpers','docs']:
 for x in proof[kind]:assert sha(Path(x['path']))==x['sha256'],x['path']
run=D/'competition-only-product-scope'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';current=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for p,n in [(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(current,'previous-current.md'),(Path(__file__),Path(__file__).name)]:shutil.copyfile(p,run/n)
v=dict(status='authoritative-competition-only-product-scope',user_clarification='lito只有競技場沒有WORLD 地圖',product_acceptance=['competition room modes and real team/relationship rules','self/friendly/enemy PC and NPC/ArenaBot/own-other pets/summons','Source primary and secondary target semantics','normal/Ctrl cursor, original outgoing object id, server admission, effects and one resource/reuse commit','cross-role pair locking, shutdown/lifetime, foreground multiwindow, lobby return'],WORLD_product_acceptance_required=False,WORLD_backend_behavior_retained=True,historical_receipts_not_rewritten=True,manual_game_UI_waits_until_authorized_concentrated_acceptance=True,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False)
write(run/'manifest.json',v);index=D/'competition-only-product-scope-current.json';write(index,dict(status=v['status'],manifest=pin(run/'manifest.json')))
text=current.read_text('utf-8').replace('WORLD／其餘材料與完整Source','其餘材料與完整Source').replace('WORLD／其餘技能模式／客戶端游標尚未驗收','其餘技能模式／客戶端游標尚未驗收');lines=text.splitlines();at=next(i for i,l in enumerate(lines)if l.startswith('| 本次更新 |'));lines.insert(at+1,'| **產品場景澄清** | 使用者確認 **LiTo只有競技場，沒有WORLD地圖**。全部目標／Ctrl與游標以競技模式、實際隊伍／關係及回大廳驗收；WORLD只保留既有後端回歸，不再列產品完成門檻。歷史收據維持原樣。[範圍](../../tools/combat-continuation/integration-lifecycle/build/competition-only-product-scope-current.json)。 |');current.write_text('\n'.join(lines)+'\n','utf-8')
doc.write_text(doc.read_text('utf-8')+'\n\n使用者場景澄清：「lito只有競技場沒有WORLD 地圖」。以此作現行產品驗收範圍；自身／友方／敵方、正常／Ctrl、跨來源角色、範圍主次目標、NPC／ArenaBot／寵召、前景多視窗及回大廳均以競技場路徑驗證。WORLD只保留既有後端行為及補充回歸，不列新產品完成條件；前面歷史收據的WORLD待驗文字不代表仍要求加入WORLD產品。未部署／未開遊戲UI／未改正式DLL或資料庫。\n','utf-8')
audit.update(previous=pin(run/'previous-audit.json'),competition_only_product_scope=pin(run/'manifest.json'));write(auditpath,audit);proof.update(previous_progress=pin(run/'previous-proof.json'),competition_only_product_scope=pin(run/'manifest.json'),audit=pin(auditpath),current_audit=pin(auditpath),docs=[pin(doc),pin(current)]);write(proofpath,proof);following=dict(continuation);following[key]=pin(index);write(C,following);assert {k:v for k,v in read(C).items()if k!=key}==continuation
for kind in ['protected','live_helpers','experimental_live_helpers','docs']:
 for x in proof[kind]:assert sha(Path(x['path']))==x['sha256']
print(json.dumps(dict(status=v['status'],WORLD_product_acceptance_required=False,formal_unchanged=True)))
