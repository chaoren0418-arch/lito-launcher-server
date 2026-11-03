"""Record program-only controlled actor wiring, preserving all earlier native qualification boundaries."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for item in v.values():verify(item)
 elif isinstance(v,list):
  for item in v:verify(item)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';index=D/'source-program-update-progress-current.json';proof=read(proofpath)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
latest=read(D/'source-controlled-display-build-current.json');repro=read(D/'source-controlled-display-reproduction-current.json');verify(latest);verify(repro);assert repro['runtime']==latest['runtime'] and repro['status'].startswith('passed-byte-identical')
assert latest['runtime']['sha256']=='0ed15402188708cd394635574e1f907fc6132acd3b43ea20015d7abde42d184a'
for ref in [latest,repro]:
 m=read(Path(ref['manifest']['path']));verify(m.get('inputs',[]));verify(m['log'])
previous=read(index);verify(previous);assert previous['runtime']['sha256']=='54583f1b9a0ec078c96487b77084111720538e192f7762703e32c0d19766ea8f'
old=read(Path(previous['manifest']['path']));current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md'
run=D/'source-controlled-display-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md')]:shutil.copyfile(p,run/name)
anchors={p:sha(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json',D/'source-area-weapon-progress-current.json',D/'source-negative-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json']}
for name in ['NativeSourceControlledDisplay.java','NativeSourceNpcDamageDisplay.java','SourceControlledDisplayMethods.java','SourceControlledDisplayLoad.java','SourceControlledDisplayDelta.java','SourceControlledCallCensus.java','build_source_controlled_display.py','reproduce_source_controlled_display.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
retained=[];sqlbase=old['readonly_SQL_consumers_byte_retained']['base']
with zipfile.ZipFile(sqlbase['path'])as sql,zipfile.ZipFile(previous['runtime']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as now:
 for name in old['readonly_SQL_consumers_byte_retained']['entries']:assert sql.read(name)==now.read(name),name;retained.append(name)
 for name in parent.namelist():
  if any(name.startswith('local/lito381/server/'+prefix)for prefix in ['NativePlayerLifetime','NativeSourceDamageDisplay','NativeSourceOtherControls','NativeSourceDisplayPlan','NativeSourceMonsterHp']):assert parent.read(name)==now.read(name),name
assert len(retained)==20
roots=['attack(AttackRequest,AttackEnvelope)','useAttackSpell(AttackSpellRequest,AttackSpell,fixtures)','useHealingSpell','useAreaAttackSpell(AttackSpellRequest,AreaAttackSpell)','useCenteredAreaAttackSpell','nativeFreezeAcceptedOriginalBattle','nativeAreaFreezeOriginalBattle','nativeSourceLightningOriginalBody','NativeShockCompanionAction.shock before original pair wait']
remaining=[
 'The nine new entry roots and controlled physical/magic digits, Kiringku, normal/Ctrl, primary/secondary, death/revival, owner-role/pose/scene/replacement, pair waits, final receiver waits, IO, DRAINING/STOPPED and Main shutdown behavior have program wiring only. Native game validation remains deferred by the user; no previous SHA case count is transferred.',
 'Other controlled and natural NPC producers still need explicit Source classification and wiring: wand, defense/reflection feedback, poison/cube/doll/periodic producers, NPC factory/AI and global final-frame coverage. They are not broadly classified as ordinary PC magic.',
 'Source18/39/133 and the remaining material/Source models still require complete actor-kind, probability/cost/order, permission, self/normal/Ctrl, primary/secondary range, lifecycle and memory failure/retry matrices. Static inventories and compiled roots do not prove full Source completion.',
 'Actual MySQL evidence remains the earlier bounded b85b71d8 readonly static equipment and empty player queries. Twenty SQL consumer class entries remain byte-identical; no new database access or real equipped-character mapping was performed.',
 'Final isolated composition retaining formal0.6.71 resources/icons/special-load DLL/GM7200, same-SHA complete regression, then concentrated actual cursor/foreground/multiwindow/lobby acceptance remain pending. Source26 no-Ctrl report is unresolved; LITO product scope is BATTLE only; no deployment.'
]
implemented=[
 'PET/SUMMON/TAMED keep the actual owner, object, life token, bag, placement/master/pose and captured Source/player binding. The selected controlled primary is checked before original costs and each reviewed HP commit; stale or uncaptured controlled HP cannot be late-retargeted.',
 'Eight explicit PC roots and pre-pair87 reuse existing captured all-player action locks, peer receipts, lifetime admission and original material/HP bodies. Existing parent Context fields and companion scope are retained/restored; child and copy Contexts inherit the transient action without new TLS.',
 'One physical, three direct magic, two centered/freeze and two precise reflective controlled magic HP call sites carry visual plans through original accepted damage. Positive accepted HP seals its actual post-death token; numeric formulas, original costs, counters, permissions and unchanged producers pass independent method inverses.',
 'Controlled Kiringku hit-only Source flag/SPR, actual viewer binding/screen and selected target proof are checked at original receiver frame. Miss/critical/physical/magic numbers use existing Source display planner and bounded256 lifetime jobs. Detached immutable queued proofs retain neither Context nor receipt.',
 'Original controlled HP bars remain; existing Source Monster-only HPBAR gate is not applied to pets. Accepted caster HP/MP and arrow count/removal confirmations survive target retirement while stale target FX is dropped. Final new guards only read existing identities/volatile fields without acquiring actor or Hub locks.',
 'No new workers, TLS, global actor registry, database authority, SQL/schema/WAL write, new baseline or deployment. Source controls, lifetime jobs/shutdown and20 SQL class entries are byte-identical to the parent where specified.'
]
v=dict(status='program-updated-controlled-NPC-digits-Kiringku-nine-roots-unqualified-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],previous_program_progress=pin(run/'previous-program-progress.json'),previous_qualified_runtime=old['previous_qualified_runtime'],entry_roots=roots,implemented_scope=implemented,program_checks=dict(byte_identical_frozen_rebuild=True,independent_archive_field_all_original_method_inverse=True,JVM_no_initialization_classes=17,final_receiver_monitor_and_detached_proof_audit=True),new_game_runtime_cases=0,actual_role_and_cursor_validation_deferred_by_user=True,new_database_connections=0,readonly_SQL_consumers_byte_retained=dict(base=sqlbase,program=latest['runtime'],entries=retained),previous_real_MySQL_evidence=pin(D/'source-equipment-mysql-reload-readonly-origin-current.json'),remaining=remaining,all_owned_JVMs_terminal=True,editable_inputs=[pin(p)for p in inputs.iterdir()],protected=proof['protected'],all_material_skills_complete=False,all_Source_consumers_wired=False,same_SHA_full_regression_passed=False,actual_client_cursor_verified=False,user_report_fixed=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=latest['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
lines=current.read_text('utf-8').splitlines()
for n,line in enumerate(lines):
 if line.startswith('| 本次更新 |'):lines[n]='| 本次更新 | 程式候選 **`0ed154021887…`**：受控PET／SUMMON／TAMED物理／魔法數字、奇古獸、閃電／單體／範圍／凍結與87入鎖前捕捉，共9入口；沿用真主人收據、排序鎖與原傷害／扣費，提交及最終Frame重驗生命／角色／位置。編譯、17類JVM不初始化檢查、同位元重建及原方法inverse通過，**新增遊戲驗證0案**。其餘Source仍開放；先更新程式，之後集中驗收。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if line.startswith('| 外控下一步 |'):lines[n]='| 外控下一步 | 16鍵唯讀快照保留；PC／任務／限時怪物及本輪受控NPC數字、奇古獸與生命週期已接線，**新候選尚未原生資格化**。魔杖、反饋／反射、毒／cube／doll／週期、NPC工廠／AI與完整Source／最終同SHA回歸仍待補齊；完成實作後集中開遊戲驗游標，未部署。 |'
current.write_text('\n'.join(lines)+'\n','utf-8')
doc.write_text(doc.read_text('utf-8')+'\n\n2026-10-24 受控NPC程式續作：`0ed154021887…`加入PET／SUMMON／TAMED實際主人、物件／bag／生命／位置／placement與master證明。8個PC入口加87入鎖前入口，覆蓋物理、一般魔法、治癒轉傷害、閃電、範圍／中心範圍與單體／範圍凍結；既有6個直接與2個精確反射HP呼叫沿用原傷害、材料、MP／HP、權限與反制。主目標在成本前檢查，HP提交再次核對；未捕捉的受控目標不可在輸出後重新取得權威。原父Context與scope保留／還原，子Context只繼承本次暫態動作。\n\n受控奇古獸依Source命中、開關／SPR與觀察者螢幕檢查；miss／critical及數字沿用既有planner，死亡只在原HP接受後固定該次post-life。既有256有界顯示工作／關閉流程保留，延後證明不持有Context或主人收據，沒有新worker／TLS／全域名冊。新最終Frame guard只讀既有角色與volatile狀態，不在receiver內拿Hub／物件鎖；已提交箭矢及HP／MP確認仍可發布，過期目標特效丟棄。受控血條原流程保留，不誤套Monster-only HPBAR開關。\n\n本候選僅程式更新：17個實際類別JVM不初始化檢查、凍結輸入同位元重建、archive／欄位／每個原方法inverse與最終guard靜態稽核通過，新增遊戲案0、資料庫連線0。舊SHA的756案／9,280檢查與b85唯讀MySQL證據沒有移算；20個SQL消費class及原lifetime／Source顯示工作／外控保留逐位元相同。正常／Ctrl、跨角色等待、範圍主次、死復活、提交後IO／Frame、Main關閉及真實角色／游標驗收依使用者要求後續集中處理。魔杖、反射／反饋、毒／cube／doll／週期、NPC工廠／AI與完整材料／Source仍開放。未部署，未改Source／玩家SQL／schema／WAL。收據：`source-program-update-progress-current.json`。\n','utf-8')
continuation=read(C);continuation['source_program_update_progress']=dict(index=pin(index),runtime=latest['runtime'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,complete=False);continuation['source_other_controls_remaining_native_consumers']=dict(program_progress=pin(index),PC_Quest_Time_controlled_digits_and_Kiringku_wired_unqualified=True,controlled_entry_roots=9,remaining_explicit_producers=remaining[1],complete=False);write(C,continuation)
audit=read(auditpath);audit['source_program_update_progress']=dict(index=pin(index),runtime=latest['runtime'],native_runtime_qualified=False,remaining=remaining);write(auditpath,audit)
proof['docs']=[pin(current),pin(doc)];proof['source_program_update_progress']=dict(index=pin(index),runtime=latest['runtime'],native_runtime_qualified=False);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=[pin(current),pin(doc)],protected=proof['protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=latest['runtime'],entry_roots=9,new_game_runtime_cases=0,SQL_class_entries_retained=20)))
