"""Record program-only updates separately from earlier native qualification and bounded real MySQL evidence."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for child in v.values():verify(child)
 elif isinstance(v,list):
  for child in v:verify(child)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
query="@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"
assert subprocess.run(['powershell','-NoProfile','-Command',query],capture_output=True,text=True,check=True).stdout.strip()=='0','owned JVMs must be terminal'
latest=read(D/'source-companion-elemental-build-current.json');verify(latest);assert latest['runtime']['sha256']=='54583f1b9a0ec078c96487b77084111720538e192f7762703e32c0d19766ea8f'
run=D/'source-program-update-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';current=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for p,name in [(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(current,'previous-current.md')]:shutil.copyfile(p,run/name)
anchors={p:sha(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json',D/'source-area-weapon-progress-current.json',D/'source-negative-progress-current.json',D/'competition-only-product-scope-current.json']}
stems=['source-readonly-inventory-origin-build','source-readonly-inventory-origin-reproduction','source-equipment-mysql-reload','source-equipment-mysql-reload-readonly-origin','source-damage-display-build','source-monster-hp-build','source-remaining-material-build','source-program-safety-build','source-program-update-reproduction','source-npc-damage-display-build','source-npc-damage-display-reproduction','source-companion-elemental-build','source-companion-elemental-reproduction']
refs={}
for stem in stems:
 index=D/(stem+'-current.json');value=read(index);verify(value);m=read(Path(value['manifest']['path']));verify(m.get('inputs',[]));verify(m.get('log',{}));frozen=run/index.name;shutil.copyfile(index,frozen);refs[stem]=pin(frozen)
for stem in ['source-program-update-reproduction','source-npc-damage-display-reproduction','source-companion-elemental-reproduction']:assert read(D/(stem+'-current.json'))['status'].startswith('passed')
mysqlindex=read(D/'source-equipment-mysql-reload-readonly-origin-current.json');mysql=read(Path(mysqlindex['manifest']['path']));o=mysql['observations'];assert o['cases']==33 and o['checks']==94 and o['source_routes']==2 and not o['real_player_row_mapping_verified']
for opened,closed in [('connections','closed_connections'),('statements','closed_statements'),('resultsets','closed_resultsets')]:assert o[opened]==o[closed]
assert o['blocked_write_or_extra_authority_attempts']==0
sqlbase=read(D/'source-readonly-inventory-origin-build-current.json')['runtime'];retained=[]
with zipfile.ZipFile(sqlbase['path'])as old,zipfile.ZipFile(latest['runtime']['path'])as now:
 for name in old.namelist():
  if any(stem in name for stem in ['LegacyMySqlRepository','LegacyInventoryWeightLoader','NativeDeItemDefinitionsSql$Lease','NativeArmorProc','NativeDeItemTemplatesOwner','NativeDeItemDefinitionsOwner']):assert old.read(name)==now.read(name),name;retained.append(name)
assert len(retained)==20
patterns=['NativeSourceDamageDisplay.java','SourceDamageDisplay*.java','build_source_damage_display.py','NativeSourceMonsterHp.java','SourceMonsterHp*.java','build_source_monster_hp.py','NativeSourceRemainingMaterials.java','SourceRemainingMaterial*.java','build_source_remaining_material.py','SourceProgramUpdateDelta.java','build_source_program_safety.py','reproduce_source_program_updates.py','NativeSourceNpcDamageDisplay.java','SourceNpcDamageDisplay*.java','build_source_npc_damage_display.py','reproduce_source_npc_damage_display.py','NativeSourceCompanionElemental.java','SourceCompanionElemental*.java','build_source_companion_elemental.py','reproduce_source_companion_elemental.py','SourceReadonlyInventoryOrigin*.java','SourceReadonlyInventoryModeProbe.java','build_source_readonly_inventory_origin.py','reproduce_source_readonly_inventory_origin.py','SourceEquipmentMysqlReloadProbe.java','test_source_equipment_mysql_reload.py',Path(__file__).name]
for pattern in patterns:
 for p in R.glob(pattern):
  if p.is_file():shutil.copyfile(p,inputs/p.name)
remaining=[
 'This candidate has compile/JVM no-initialization checks and independent archive/original-method inverses only: all new18/39/133, HPBAR/digit/Kiringku NPC/cross-role/Ctrl/range/lifecycle behavior still requires concentrated native qualification after implementation, as requested by the user.',
 'Controlled PET/SUMMON/TAMED physical/magic digits and Kiringku target/HP admission remain open. The new NPC receipt/presentation scope is actual Quest/Time Attack monsters. Other magic/physical producers, NPC factory/AI, delayed poison/periodic producers and global final-frame coverage need a complete Source consumer audit.',
 'Full remaining material/Source actor-kind, probability, permission, normal/Ctrl, primary/secondary area, life/role/pose/room, memory failure/retry and competition-mode matrices remain open; static call count or compiled roots do not establish completion.',
 'Real MySQL qualification is bounded to native static equipment rows and empty player queries on b85b71d8. Twenty relevant SQL consumer entries are byte-identical in this program candidate, but this is not a rerun or verification of real equipped character row mapping; the user deferred further validation.',
 'Final isolated composition retaining formal0.6.71 resources/icons/special-load DLL/GM7200, same-SHA full regression, then concentrated actual cursor/foreground/multiwindow/lobby-return acceptance. Source26 no-Ctrl report remains unresolved. No deployment or new baseline.'
]
v=dict(status='program-updated-compile-only-Source18-39-133-HPBAR-digits-with-separate-real-readonly-MySQL-evidence-full-goal-open',runtime=latest['runtime'],build=latest['manifest'],previous_qualified_runtime=read(D/'source-kiringku-progress-current.json'),new_game_runtime_cases=0,actual_role_and_cursor_validation_deferred_by_user=True,evidence=refs,
 implemented_scope=['PC physical/magic digits, Source miss/critical markers, damage<=99999 high-to-low SPR, memory dmg_spr preference through the original validated helper action','Immutable visual metadata on existing HP CalcTicket/Event, original accepted completion, bounded256 visual jobs on existing lifetime workers/timer and drop-only shutdown/rejection; no new thread or TLS','Quest/Time Attack monster HPBAR attacker-only unicast, bounded1024 weak per-viewer ratio dedup, death hide/cache release, existing generation and life/pose checks','Quest/Time Attack monster damage digits at2 physical and18 explicit magic producer ports; one pre-output physical NPC capture also guards Kiringku presentation and actual HP admission; original HP arithmetic retained','Source18/39/133 BATTLE captured all-player locks, original numeric bodies and one material gate; controlled18/39 replay exact accepted debit before class failure;133 screen range and Source-buff self resolution','Controlled133 follows frozen Source executor: probability/counter/erase/sleep/cost and timed marker, with no -50 attribute mutation for non-Monster NPC; one actor-owned lease plus existing cancellation/expiry polling; PET_RACE remains rejected'],
 readonly_MySQL_runtime=mysqlindex['runtime'],readonly_MySQL_observations=o,readonly_MySQL_qualification_scope='existing native Owner static equipment reloads and two source routes of empty player SELECTs; no real player row mapping',readonly_SQL_consumers_byte_retained=dict(base=sqlbase,program=latest['runtime'],entries=retained),remaining=remaining,
 all_owned_JVMs_terminal=True,new_helpers_sealed=False,editable_inputs=[pin(p)for p in inputs.iterdir()],protected=proof['protected'],all_material_skills_complete=False,all_Source_consumers_wired=False,same_SHA_full_regression_passed=False,actual_client_cursor_verified=False,user_report_fixed=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);index=D/'source-program-update-progress-current.json';write(index,dict(status=v['status'],runtime=latest['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
lines=current.read_text('utf-8').splitlines();replacement={
 '| 本次更新 |':'| 本次更新 | 程式候選 **`54583f1b9a0e…`**：補18／39／133成對材料入口、受控18／39扣費後類型失敗、133受控NPC期限標記；加入PC與任務／限時怪物HPBAR／傷害數字及NPC奇古獸生命週期接線。僅編譯／不初始化類別／同位元重建與原方法inverse，**新增遊戲驗證0案**；使用者要求先更新程式、後續集中驗證。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |',
 '| 角色裝備中文欄位續作 |':'| 角色裝備中文欄位續作 | `b85b71d8…`已通過兩個來源的實際MySQL原生唯讀重載：33案／94檢查、6次靜態防具映射與6次空背包重載；24連線／36 statement／36 ResultSet全部關閉，0寫入嘗試。唯讀Source背包不再要求不存在的lito_item_origin；可寫模式保留。新程式候選保留20個相關SQL class entries逐位元相同；未驗真實裝備角色，使用者延後此驗證。 |',
 '| 外控下一步 |':'| 外控下一步 | 16鍵唯讀快照保留；PC數字／dmg_spr、任務／限時怪物HPBAR與數字，以及該NPC奇古獸接線已編譯，**尚未新增原生資格化**。受控NPC數字／奇古獸、其他生產端、完整Source／最後同SHA回歸仍待補齊；完成實作後才集中開遊戲驗游標，未部署。 |'
}
for i,line in enumerate(lines):
 for prefix,value in replacement.items():
  if line.startswith(prefix):lines[i]=value;break
current.write_text('\n'.join(lines)+'\n','utf-8')
doc.write_text(doc.read_text('utf-8')+'\n\n2026-10-24 程式先行續作：`54583f1b9a0e…`只完成編譯／JVM不初始化類別檢查、凍結同位元重建與原方法inverse，新增遊戲執行案為0；先前89e2b687的756案／9,280檢查保留原SHA界線，沒有移算到本候選。Source18／39／133取得原始目標與life／pose後，沿用既有全玩家排序鎖、材料、收據及鎖外輸出；18／39受控NPC在一次扣費後走Source類型必敗，不抽機率或消耗目標消魔／睡眠。133讀Source負射程的螢幕範圍與buff自施規則；受控NPC成功時依Source ELEMENTAL_FALL_DOWN只保存技能期限，不誤套僅PC／Monster分支的減抗，保留反制、消魔、睡眠、材料與PET_RACE限制。期限只有一個actor-owned記錄，沿用取消及既有到期輪詢，沒有新增timer／TLS。\n\nHPBAR限定任務／限時怪物，單播攻擊者、1024上限弱觀察者百分比去重、死亡0xFF隱藏與清空；玩家HUD及ArenaBot原流程不受HPBAR總開關誤擋。PC物理／魔法數字、miss／critical、dmg_spr記憶體偏好走既有HP ticket／event／accepted tail；256個有界延後顯示工作依既有lifetime worker／timer管理，拒絕或關閉只丟視覺。怪物兩個物理與18個明確魔法生產端取得原生HP提交收據；物理NPC身分在第一個輸出前固定，奇古獸SPR與HP准入均核對該生命／位置／房間世代。顯示失敗不撤銷HP或中止後續戰鬥效果。受控NPC數字／奇古獸、更廣生產端、自然NPC／AI與完整Source矩陣仍開放。\n\n中文裝備欄位已在實際MySQL透過既有原生唯讀讀取器完成33案／94檢查：兩個來源、6次靜態防具與6次空背包重載，24連線／36 statement／36 ResultSet全部關閉，沒有寫入／額外權威嘗試。首次實庫讀取找到來源庫沒有lito_item_origin；候選僅讓readOnly Source使用原有零origin分支，可寫模式保持，沒有建表或遷移。該SQL資格化在b85b71d8，最新程式保留20個相關class entries逐位元相同，但不是最新SHA重跑，也未驗真實裝備角色；使用者要求先更新程式、後續再驗證。完整材料／Source、正式0.6.71隔離組合與同SHA全回歸仍須完成，最後才集中實機游標／前景／多視窗／回大廳驗收。26不按Ctrl回報仍未宣告修復。未部署，未改Source／玩家SQL或WAL。收據：`source-program-update-progress-current.json`。\n','utf-8')
continuation=read(C);continuation['source_program_update_progress']=dict(index=pin(index),runtime=latest['runtime'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,complete=False);continuation['source_real_mysql_readonly_progress']=dict(index=refs['source-equipment-mysql-reload-readonly-origin'],runtime=mysqlindex['runtime'],cases=33,checks=94,real_player_row_mapping_verified=False);continuation['source_other_controls_remaining_native_consumers']=dict(program_progress=pin(index),PC_digits_and_Quest_Time_monster_controls_wired_unqualified=True,controlled_digits_and_Kiringku_pending=True,complete=False);write(C,continuation)
audit=read(auditpath);audit['source_program_update_progress']=dict(index=pin(index),runtime=latest['runtime'],native_runtime_qualified=False,remaining=remaining);write(auditpath,audit)
proof['docs']=[pin(current),pin(doc)];proof['source_program_update_progress']=dict(index=pin(index),runtime=latest['runtime'],native_runtime_qualified=False);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=[pin(current),pin(doc)],protected=proof['protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False));print(json.dumps(dict(status=v['status'],runtime=latest['runtime'],new_game_runtime_cases=0,readonly_MySQL_cases=33,SQL_class_entries_retained=20)))
