"""Refresh current index wording after the separately evidenced formal release; preserve sealed native evidence."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
proofFile=D/'Source-lifecycle-progress-verified-current.json';proof=read(proofFile);progress=read(D/'blind-scene-progress-current.json');assert proof['pending_blind_scene']==progress['manifest']
for key in ['protected','live_helpers','experimental_live_helpers','docs']:
 for ref in proof[key]:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
doc=S/'docs/maintenance/combat-continuation-current.zh-TW.md';text=doc.read_text('utf-8');assert text.count('更新：2026-10-21。')==1;formal=next(l for l in text.splitlines()if l.startswith('| 正式版 |'));latest=next(l for l in text.splitlines()if l.startswith('| 本次更新 |'));assert '**0.6.69**'in formal
run=D/'blind-scope-docs-refresh'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p,name in [(proofFile,'previous-proof.json'),(doc,'previous-current.md'),(Path(__file__),Path(__file__).name)]:shutil.copyfile(p,run/name)
text=text.replace('更新：2026-10-21。','更新：2026-10-23。').replace(formal,'| 正式版 | **0.6.71**；由另一發布流程更新，職業圖示／GM外型功能依其發布紀錄管理。本戰鬥候選未部署，未來須隔離整合。[唯讀核對](../../tools/combat-continuation/integration-lifecycle/build/blind-external-formal-release-audit-current.json)。 |').replace(latest,'| 本次更新 | 材料40／103跨角色目標、目前Source單體語意及相消別名；同SHA 1,128個原生案＋54個獨立Source參考案，含Main68／73、48次成對等待與48次材料故障回復。完整34／58材料與Source仍開放；本工作未部署、未啟動遊戲、未寫Source／玩家SQL／WAL。 |')
doc.write_text(text,'utf-8');v=dict(status='refreshed-current-index-date-and-separate-formal-release-boundary',native_scope=progress['manifest'],external_release_audit=proof['external_formal_release_audit'],previous_proof=pin(run/'previous-proof.json'),previous_doc=pin(run/'previous-current.md'),current_doc=pin(doc),formal_deployment_changed_by_this_task=False,full_goal_achieved=False);write(run/'manifest.json',v)
proof['docs']=[pin(Path(ref['path']))for ref in proof['docs']];proof['documentation_refresh']=pin(run/'manifest.json');write(proofFile,proof)
assert read(D/'blind-scene-progress-current.json')==progress
for key in ['protected','live_helpers','experimental_live_helpers','docs']:
 for ref in proof[key]:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
print(json.dumps(dict(status=v['status'],protected_helpers_docs_verified=sum(len(proof[k])for k in ['protected','live_helpers','experimental_live_helpers','docs']),native_scope_unchanged=True)))
