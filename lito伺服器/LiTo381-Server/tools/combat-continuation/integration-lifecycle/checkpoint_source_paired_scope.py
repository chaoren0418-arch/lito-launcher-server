"""Advance shared Source pair pre-scope contention evidence; preserve qualification."""
from pathlib import Path
from datetime import datetime, timezone
import json, hashlib, shutil, zipfile, re, subprocess, copy, csv

R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)

index=D/'source-program-update-progress-current.json';proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';C=R.parent/'continuation-current.json'
previous=read(index);verify(previous);old=read(Path(previous['manifest']['path']));verify(old)
proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
latest=read(D/'source-paired-scope-build-current.json');repro=read(D/'source-paired-scope-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_paired_scope_contract_scenarios']==31 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_paired_scope_contract_scenarios')==1918
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 5 original methods'in log and 'PASS290'in log
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
anchors={p:sha(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json',D/'source-raw-producer-build-current.json',D/'source-arena-poison-build-current.json']}
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as sql,zipfile.ZipFile(latest['runtime']['path'])as jar:
 entries=old['readonly_SQL_consumers_byte_retained']['entries'];assert len(entries)==20
 for name in entries:assert sql.read(name)==jar.read(name),name

run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md'
old_index_pin=pin(index)
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
frozen_index=pin(run/'previous-program-progress.json')
for ref in old['editable_inputs']:
 name=Path(ref['path']).name;shutil.copyfile(R/name,inputs/name)
for name in ['NativeSourcePairedScope.java','SourcePairedScopeMethods.java','SourcePairedScopeDelta.java','SourcePairedScopeLoad.java','SourcePairedScopePlan.java','build_source_paired_scope.py','reproduce_source_paired_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-shared-paired-scope-contention-before-registration-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'One original shared NativePairedCastSafety.run wrapper for loaded BATTLE. A nested request try-acquires its missing actual action mutex before any original scope entry. Rejection runs no body and never registers the blocked peer. Both-held/unowned/self original bodies and full WORLD/unloaded fallback retained.',
 'Success delegates to the exact retained original run, then releases the exact cached acquired mutex and invokes the original unlocked hook. No actor/Frame/Context/life capture, fields, lock domains, TLS, registry, queue, worker or SQL. Existing caller competition/Ctrl/cost/RNG/role/life/Main predicates remain at original boundaries.',
 'Five original shared-helper methods independently inverted;290 no-init classes;31 new actual detached shared-entry/mutex/contended-both-orientations/nested-scope/self/exception/fallback contracts plus1918 inherited same-SHA pure contracts; frozen byte-identical replay.76 frozen Source files,133 bounded excerpts and existing proc contracts;20 SQL class consumers byte-retained. Bound cast/cost/RNG/HP/role/IO/Main/database/game0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=290,original_methods_independently_inverted=5,byte_identical_frozen_rebuild=True,existing_changed_class_entries=1,new_helper_class_entries=1,new_original_entry_wrappers=1,pure_paired_scope_contract_scenarios=31,inherited_same_SHA_pure_contract_scenarios=1918,original_shared_direct_caller_sites=5,shared_direct_callers_are_static_locators_only=True,contended_rejection_before_scope_registration_detached_verified=True,exact_acquired_mutex_released_with_original_unlocked_hook=True,new_original_metadata_fields=0,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_HP_IO_role_Main_cases=0)
for key in ['paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['paired_scope_'+key]=build[key]
v.update(shared_paired_scope_pre_registration_contention_program_wired=True,shared_paired_scope_no_fresh_role_or_life_capture=True,shared_paired_scope_original_Ctrl_competition_cost_HP_Main_WORLD_inverse_retained=True,shared_paired_scope_all_callers_and_lifecycles_qualified=False)
v['remaining_work_groups']=[
 dict(id='paired-lifecycle',priority='P0',status='partial-program-wiring',work='Audit original shared callers receipt reservation/drain outside run and custom lock helpers; preserve initial role/life/pose through all accept/commit/final IO boundaries, death/revival/same-id/scene/owner changes, bounded memory and DRAINING/STOPPED/Main.'),
 dict(id='materials',priority='P0',status='partial-program-wiring',work='Complete each of58 reagent-positive skills and all applicable807 historical rows: packet/item/bookmark/none/self selection, normal/Ctrl, enemy/friend/clan/party, primary-secondary/line/area/LOS, full isTarget self/zone/ghost/hidden/dead/target bits/PK, class/type/immunity/corpse, cost/RNG/rollback and empty-PC-list normal consumption. Historical180 models/185 Source symbols/234 parsers are locators only.'),
 dict(id='proc-dispatch',priority='P0',status='partial-program-wiring',work='Complete equipment/stone/weapon/nonhealing68, doll skill/add/reduce, guardian/talent and Source GMBUFF/PROCS/direct-magic distinctions; preserve pre-RNG/HP/effect phase and original accepted attacker including actual NONE versus unknown.'),
 dict(id='hp-producers',priority='P0',status='partial-program-wiring',work='Complete wand/reflect/feedback/poison kinds, Fatality catalog applicability, cube effect-NPC source/player-nonplayer MP/status/aura, ground pulses, companion/bot/natural NPC physical/magic/area/periodic, direct/accepted mana-only effects and erase exact timers, signed healing/undead reversal/type-immunity. Static direct HP edges are not missing-path or qualified-case counts.'),
 dict(id='math-and-status',priority='P0',status='partial-program-wiring',work='Complete effective actor stats/INT/leverage/lawful/healing, full NPC immunity and IMMUNE_TO_HARM/SKNPC/PLNPC, guardian/talent, innate-haste/zero/oversize durations and status mutation/consume ordering; accepted corpse proof remains separate from initial live proof.'),
 dict(id='npc-factory-ai',priority='P0',status='partial-program-wiring',work='Complete missing/unknown factory policy, first static template and reload/rollback birth metadata, actor first-discovery/master birth/manual Ref origins, tower/door/guardian/bot/ARENA kind applicability, spawn/custom-work/full attributes, AI/hate/family-link/reward and controlled owner-meter. Frozen32 classes only qualify scoped type oracles.'),
 dict(id='presentation',priority='P0',status='partial-program-wiring',work='Complete all HPBAR/digits/kiringku/accepted effect recipients, healing positive meter/gfx IO/transport-carrier lifetime, observers, original actor proc_spr, action-damage suppression/PC exclusion/temp gfx, ordinary-target/RO login sync and multi-resource routes; no queued Frame/Context/registry ownership.'),
 dict(id='external-controls',priority='P0',status='inventoried-not-complete',work='Trace252 explicit Source property rows through dynamic keys/aliases/class-damage matrices/SQL readers and actual consumers; classify unused or nonproduct keys with evidence.52 exact candidate literals do not prove200 absent consumers. Preserve BATTLE product and original WORLD fallback.'),
 dict(id='formal-composition',priority='P1',status='pending-after-program-gaps',work='Compose formal0.6.71 resources/icons/special-load DLL/GM7200 in isolation; never overwrite formal artifacts with experimental ancestry or create a new baseline.'),
 dict(id='same-sha-backend',priority='P1',status='deferred-by-user-until-program-complete',work='After all code updates, run the complete same-SHA actual admission/rejection/cost/RNG/rollback/HP/role/life/scene/retirement/receiver/IO/retry/zero/fatal/DRAINING/STOPPED/Main and memory matrix; pure/static/no-init counts are not bound acceptance.'),
 dict(id='chinese-mysql',priority='P1',status='deferred-by-user-until-program-complete',work='Verify actual Chinese MySQL readonly equipped-role/static-reader/INT transaction and edited-bonus reload on final SHA. Prior static/empty-query and fake-JDBC evidence do not qualify new readers or real equipment. No Source/player/schema/WAL writes.'),
 dict(id='client-cursor',priority='P1',status='deferred-by-user-until-program-complete',work='Concentrated real normal/Ctrl/self/cross-role/area cursor, foreground/multiwindow/lobby acceptance after all program work; Source26 no-Ctrl self-cast report remains unresolved by actual cursor. No deployment.')
]
v['remaining']=[x['work']for x in v['remaining_work_groups']]
v['historical_remaining_preserved_in']=previous['manifest']

def static_calls(manifest):
 ref=next(x for x in manifest['artifacts']if Path(x['path']).name=='call-sites.tsv');verify(ref)
 with Path(ref['path']).open(encoding='utf-8',newline='')as f:return list(csv.DictReader(f,delimiter='\t'))
rows=static_calls(inv);prior_inv=read(Path(old['inventory']['path']));verify(prior_inv)
select=lambda data:[r for r in data if r['callee_owner']=='local/lito381/server/NativePairedCastSafety'and r['callee']=='run']
callers=select(rows);assert len(callers)==5 and callers==select(static_calls(prior_inv))
upstream=[r for r in rows if r['callee_owner']=='local/lito381/server/NativeLightAction'and r['callee']=='commitPair']
write(run/'shared-pair-caller-locators.json',dict(runtime=v['runtime'],static_only=True,original_direct_callers_bytecode_edges_retained=True,original_direct_callers=callers,upstream_commitPair_edges=upstream,full_lifecycle_qualified=False,bound_cases=0))
v['shared_pair_caller_locators']=pin(run/'shared-pair-caller-locators.json')

write(run/'goal-reset-request.json',dict(requested_by_user=True,tool_status='rejected-unfinished-existing-goal',tool_error='cannot create a new goal because this thread has an unfinished goal; complete the existing goal first',system_goal_recreated=False,existing_goal_falsely_completed=False,working_scope=v['remaining_work_groups'],full_goal_achieved=False))
v['goal_reset_request']=pin(run/'goal-reset-request.json')

for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=previous['system_goal'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='''## 本次已更新的程式

共用 NativePairedCastSafety.run 增加競技場 Source 入口：巢狀呼叫已持有一個角色鎖時，在原 scope 建立前先嘗試取得另一個原有鎖；取得失敗立即返回，不執行 body，也不把該角色加入 scope。這修正原先拒絕後仍可能在 scope.close 排空時等待另一角色鎖的路徑。

成功沿用完整原方法，再釋放實際取得的同一把鎖並呼叫原 unlocked。雙鎖已持有、自施、雙鎖未持有與 WORLD／未載入規則後備均保留原方法。無新角色／生命證明、Frame／Context、欄位、鎖域、worker、TLS、queue 或 SQL；競技、Ctrl、成本／RNG／HP、Main／DRAINING／STOPPED 仍由原呼叫端的既有守衛判定。

5 原方法獨立 inverse、290 類 no-init、31 新離線實際共用入口／雙向競爭／巢狀 scope／自施／例外／後備檢查，與同 SHA 1,918 繼承純契約通過；凍結輸入重建同位元。76 Source 檔、133 既有 bounded 參考與 proc 方法、20 SQL class 保留。**真 bound cast／成本／RNG／HP／角色／正向 IO／Main／MySQL／game 均未驗收**。

現行原共用入口有 5 個直接呼叫點，僅屬定位證據。呼叫端在 helper 外的 receipt 保留／排空、其他自訂鎖路徑、58 材料技能完整目標／免疫／成本語意、252 外控、其餘 proc／NPC 工廠與 AI／傷害數字與 HPBAR 最終接收、正式 0.6.71 隔離合成仍須補齊。程式全部完成後再集中實機與中文 MySQL 唯讀重載；不部署、不連實際 MySQL、不寫 Source／玩家 SQL／schema／WAL。

目標重設已嘗試，但系統目標工具仍回報舊目標 active，拒絕建立新目標；未將未完成目標偽報完成。上述工作仍依使用者指定範圍繼續。

'''
tail=text[b:]
tail=tail.replace('| P0 | 其餘HP生產端 |', '| P0 | 其餘HP生產端 | 魔杖／反射／反饋／毒傷與各原始來源、裝備／魔石／武器proc（含68非補血）、')
tail=tail.replace('| P0 | 生命週期 |', '| P0 | 生命週期 | 優先稽核共用入口外的receipt保留／排空與其他自訂鎖路徑；')
tail=tail.replace('| P1 | 正式功能隔離合成 |', '| P0 | 顯示與NPC語意 | 完整HPBAR／傷害數字／奇古獸最終接收與transport生命、NPC工廠／AI／hate／family、owner birth／manual Ref／tower／door／bot、免疫／有效數值／狀態時序、受傷動作／RO同步／多資源路徑。 |\n| P1 | 正式功能隔離合成 |')
work.write_text(text[:a]+block+tail,'utf-8')
note=f"\n\n2026-10-25 共用成對鎖續作：候選`{v['runtime']['sha256']}`，loaded BATTLE 巢狀競爭改在原 scope 前 tryLock；拒絕不執行 body／加入 peer，成功保留完整原方法、釋放實際同一把鎖與原 unlocked。5 原方法 inverse、290 no-init、31 新＋1918 同 SHA 繼承純契約與同位元重建；5 直接 caller 只定位，真 bound HP／角色／IO／Main／DB／game 均0。其他 receipt／鎖路徑及完整58材料／252外控／Source／NPC-AI／正式合成仍開放。未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-shared-paired-scope-contention-before-registration-program-only',full_goal_achieved=False)
continuation['source_paired_scope_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=5,new_game_runtime_cases=0,new_database_connections=0)))
