"""Record actual final-monitor gaps separately from the verified precommit repair."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,shutil
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=pin(D/'source-program-update-progress-current.json');program=read(index['path']);assert program['runtime']['sha256']=='bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2';checked(program['runtime']);progress=read(checked(program['manifest']))
probe=read(D/'source-material-negative-NPC-final-current.json');assert probe['runtime']==program['runtime'];m=read(checked(probe['manifest']))
for ref in m['inputs']+m['artifacts']:checked(ref)
with checked(next(x for x in m['artifacts']if Path(x['path']).name=='results.tsv')).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
failed=[r for r in rows if r['error_type']];assert len(rows)==60 and len(failed)==10
expected={'final:lifetime','final:source-status-replaced','final:server-application','final:receiver-status-replaced','final:receiver-observer'}
for skill in ['29','152']:assert {r['mode']for r in failed if r['skill_id']==skill}==expected
assert all(r['error_type']=='java.lang.AssertionError'and r['error_message']=='source/Main/global rejection precedes all viewer cipher output'and not r['cleanup_error']for r in failed)
assert not any(r['skill_id']=='153'for r in rows)
run=D/'source-negative-final-gap'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(Path(__file__),inputs/Path(__file__).name)
detail=S/'docs/maintenance/source-negative-final-gap.zh-TW.md'
text=f'''# 負面材料技能的最終接收者缺口

隔離候選 `{program['runtime']['sha256']}`。29／152／153等待原主人鎖的首次角色證據修正已整合，150次一般／Ctrl等待、1878斷言通過；這不是完整最終接收者資格。

29／152各30次完整解碼施法後，工作執行緒在指定觀察者的原監視器等待，已確認來源／主人鎖釋放與原扣款完成。兩技能各有5種狀態替換仍讓舊封包通過cipher：Hub lifetime消失、來源status容器換成同owner／life的新容器、server application替換、接收者status同owner／life替換、接收者observer變更。總共60次觀察，50通過、10項實際最終拒絕斷言失敗；不能宣稱完整生命週期已完成。

153在Source召喚物必定失敗場景只有caster輸出，沒有這條觀察者送包；初輪30次未等到觀察者監視器不是生產錯誤，已從此矩陣移除，仍須獨立自接收驗證。

接續將原首次角色／status／life／lifetime／application與接收者身分沿既有完整送包副本帶到原cipher監視器，並在送包等待前釋放產生端Aim／Context／receipt容器；保留原Source成本／免疫／RNG／競技與Ctrl。正常、DRAINING及原有移動政策必須保留，不以全關輸出通過測試。

證據：[唯讀隔離probe]({probe['manifest']['path']})。目前沒有部署、建立正式基準、啟動實際後端或寫入Source／玩家SQL／schema／WAL。
'''
detail.write_text(text,'utf-8');shutil.copyfile(detail,run/'source-negative-final-gap.zh-TW.md')
v=dict(status='recorded-actual-negative29-152-final-receiver-gap-program-fix-next',runtime=program['runtime'],program_progress=program['manifest'],probe=probe['manifest'],actual_final_monitor_attempts=60,passed_scoped_cases=50,failed_final_rejection_cases=10,failures=[dict(skill_id=r['skill_id'],mode=r['mode'],mp=r['remaining_mp'],bytes=r['encrypted_memory_bytes'],message=r['error_message'])for r in failed],retracted_unobservable153_trial=pin(D/'source-material-negative-NPC-final/20261027T053904531527Z/manifest.json'),skill153_final_receiver_scope='caster-only output in this Source summon-failure scene; separate self receiver validation remains open',inputs=[pin(p)for p in inputs.iterdir()],detail=pin(run/'source-negative-final-gap.zh-TW.md'),remaining_work_group_count=len(progress['remaining_work_groups']),next='Wire immutable first source and receiver values to existing complete original final sender/cipher, release producing scope before waits, preserve normal/DRAINING/costs/competition/Source RNG and original movement rules; then test153 actual self-receiver path.',full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,new_SQL_connections=0,new_game_runtime_cases=0,Source_or_player_SQL_or_WAL_written=False)
assert v['remaining_work_group_count']==12
write(run/'manifest.json',v);write(D/'source-negative-final-gap-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
pending=D/'source-negative-pair-first-pending-integration-current.json';p=read(pending);assert p['runtime']==program['runtime'];p.update(next_final_receiver_gap=pin(run/'manifest.json'),next=v['next']);write(pending,p)
assert pin(index['path'])==index;checked(program['runtime'])
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),failed_final_rejection_cases=10),ensure_ascii=False))
