"""Checkpoint Source76 lifetime/Ctrl work while explicitly preserving the newly proven Source pipeline gaps."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json');continuation=read(C)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert 'source_mass_slow_lifecycle_progress'not in continuation
anchors={p:sha(p)for p in [R/'current.json',D/'battle-controlled-cancellation-progress-current.json',D/'source-visibility-range-progress-current.json',D/'freeze-source-target-progress-current.json',D/'area-freeze-accepted-progress-current.json',D/'freeze-duration-progress-current.json',D/'static-respawn-progress-current.json']}
build=read(D/'source-mass-slow-target-build-current.json');verify(build);runtime=build['runtime'];assert runtime['sha256']=='de15938d60d20dbfcb2792c9350cf13e2f0ac503b4b6f0bec02d171c8f0b7aaa';bm=read(Path(build['manifest']['path']));verify(bm)
for p in bm['inputs']:assert sha(R/Path(p['path']).name)==p['sha256']
q=read(D/'source-mass-slow-qualification-current.json');verify(q);assert q['runtime']==runtime and q['status'].startswith('passed');qm=read(Path(q['manifest']['path']));verify(qm);assert qm['native_cases']==3750
assert len(bm['changed'])==3 and len(bm['fresh'])==3 and bm['base']==read(D/'battle-controlled-cancellation-progress-current.json')['runtime']
extras=['source-mass-slow-target-build','source-mass-slow-target-reproduction','source-mass-slow-scope-audit']
for name in extras:
 i=read(D/(name+'-current.json'));verify(i);assert i['runtime']==runtime;verify(read(Path(i['manifest']['path'])))
oracle=read(D/'source-mass-slow-pipeline-oracle-current.json');verify(oracle);om=read(Path(oracle['manifest']['path']));verify(om);assert om['observations']['Source_pipeline_reference_cases']==36 and om['native_runtime_cases']==0
for row in om['Source']:assert sha(Path(row['original']['path']))==row['original']['sha256']
query="@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"
assert subprocess.run(['powershell','-NoProfile','-Command',query],capture_output=True,text=True,check=True).stdout.strip()=='0'
run=D/'source-mass-slow-lifecycle-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
names=['NativeSourceMassSlowTargets.java','SourceMassSlowTargetMethods.java','SourceMassSlowTargetDelta.java','SourceMassSlowTargetLoad.java','SourceMassSlowScopeAudit.java','build_source_mass_slow_targets.py','reproduce_source_mass_slow_targets.py','verify_source_mass_slow_scope.py','qualify_source_mass_slow.py','source_mass_slow_pipeline_oracle.py','checkpoint_source_mass_slow_lifecycle.py','test_source_mass_slow_legacy.py','test_source_mass_slow_battle44_retained.py','test_source_mass_slow_retained.py']
for stem in ['Controlled','Mixed','State','Boundary','Fault','Io','PlayerIo','PlayerBoundary','CtrlArea']:names.append('SourceMassSlow'+stem+'Probe.java')
names+=['test_source_mass_slow_'+n+'.py'for n in ['controlled','mixed','state','boundary','faults','io','player_io','player_boundary','ctrl_area']]
for name in names:shutil.copyfile(R/name,inputs/name)
for row in read(R/'source76-legacy-fixtures/manifest.json')['rows']:verify(row);shutil.copyfile(Path(row['versioned']['path']),inputs/Path(row['versioned']['path']).name)
shutil.copyfile(R/'source76-legacy-fixtures/manifest.json',inputs/'legacy-version-manifest.json')
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';current=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for p,n in [(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(C,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(current,'previous-current.md')]:shutil.copyfile(p,run/n)
refs={}
for name in extras+['source-mass-slow-qualification','source-mass-slow-pipeline-oracle']:
 p=run/(name+'-current.json');shutil.copyfile(D/p.name,p);refs[name]=pin(p)
remaining=[
 'Source76 complete pipeline is NOT qualified: the 36-case verbatim Source reference proves that area buff retains the caster entry and sends one primary visual even when the sole controlled NPC fails; own primary PC/PET/SUMMON skips the entire effect loop. The current native lifetime candidate still needs those visual/whole-loop/self-PC consumers. Existing no-NPC-visual assertions are limited native observations and will require Source-backed versions.',
 'Full controlled actor Source failure18/29/39/152/153/167, Source17 actual area3 and133/148/149 range consumers; all other material/Source controls, factories, timer/AI/zero-negative-freeze paths remain open. Historical34 material roots and58 reagent-positive native IDs remain different denominators.',
 'Formal0.6.71 isolated integration preserving0.6.70/71 features and final one-SHA backend/readonly SQL startup/owned shutdown/native interface/concentrated acceptance remain open. No deployment or new baseline.'
]
v=dict(status='passed-scoped-Source76-paired-admission-state-lifetime-Ctrl-full-pipeline-open',runtime=runtime,build=build['manifest'],parent=read(D/'battle-controlled-cancellation-progress-current.json')['manifest'],receipts=refs,qualification=q['manifest'],native_groups=qm['results'],qualified_scope=dict(native_cases=3750,known_checks=qm['known_checks'],check_count_complete=False,retained_native_cases=2442,new_native_cases=1308,real_NPC_primary_owner_pair_waits=336,real_player_primary_pair_waits=56,real_native_output_monitor_waits=192,actual_material_memory_faults_and_immediate_retries=48,legacy_66_and_Source76_versioned_scene_cases=144,Ctrl_specific_primary_and_distinct_player_secondaries=12,three_real_controlled_actor_kinds=True,both_phases_and_material_application_modes=True,Source_reference_cases=36,reference_cases_excluded_from_native_count=True),archive_changed=bm['changed'],archive_fresh=bm['fresh'],no_new_player_fields_workers_TLS_or_actor_registry=True,new_bounded_context_object_fields=1,original_queue_and_finish_reused=True,final_frame_acquires_no_player_hub_status_locks=True,existing_numeric_probability_cost_bodies_independently_inverted=True,byte_identical_reproduction=True,ten_class_bodies_verified_without_initialization_Hub_SQL=True,live_helpers_sealed=False,input_snapshots=[pin(p)for p in inputs.iterdir()],remaining=remaining,full_goal_achieved=False,all_Source_consumers_wired=False,qualified_for_release=False,same_SHA_full_regression_passed=False,all_owned_native_fixtures_terminal=True,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);index=D/'source-mass-slow-lifecycle-progress-current.json';write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False,qualified_for_release=False))
text=current.read_text('utf-8');lines=text.splitlines();lines=[f'| 本次更新 | 技能76生命週期／Ctrl限定續作：同SHA 3,750原生案／已計數{qm["known_checks"]:,}檢查。Source完整管線36案另證實主目標特效與自身整段計算仍待補，未部署。 |'if l.startswith('| 本次更新 |')else l for l in lines];at=next(i for i,l in enumerate(lines)if l.startswith('| BATTLE44受控NPC成對與最終送包 |'));lines.insert(at+1,'| 76成對／Ctrl及生命週期 | **`de15938d60d2…` 同SHA 3,750原生案**；跨角色NPC與玩家主目標、392次成對等待、192次原生輸出鎖等待、48次材料記憶體故障回復；既有Main68／73及凍結／相消／範圍回歸通過。完整Source管線仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/source-mass-slow-lifecycle-progress-current.json)。 |');lines=[l.replace('BATTLE76原始封包漏找仍開放；','BATTLE76原始封包漏找及限定生命週期已補，Source76自身主目標整段計算／範圍buff顯示仍開放；')if l.startswith('| **新確認的受控NPC缺口** |')else l for l in lines];current.write_text('\n'.join(lines)+'\n','utf-8')
doc.write_text(doc.read_text('utf-8')+f'\n\n技能76限定生命週期續作：`de15938d60d2…`同SHA 3,750原生案／已計數{qm["known_checks"]:,}檢查，其中1,308新案。沿用既有固定順序角色鎖、範圍收據、材料交易與輸出批次；補NPC主目標辨識、NPC失敗前保留消魔／睡眠／魔法屏障、正常／Ctrl、392次實際取鎖與192次原生Frame等待、48次材料故障完整回復及立即重試。Ctrl指向友方NPC曾誤放行其主人作為玩家範圍目標，已改用既有指定物件判定。玩家主目標STOPPED等待後仍生效的缺口已補；原生3750案含既有2442案、Main68／73與144個經完整檔案逆向的66／76跨圖案例。只變三個既有class並新增三個helper class、每次施法一個Context Object欄位，無新玩家欄位／工作池／TLS／Actor名冊；10個class無初始化檢查、獨立原方法逆向、相同位元重建與最終送包不取角色／Hub／狀態鎖稽核通過。**這不是完整Source76完成**：後續36個完整原片段管線參考（另計，無Hub／SQL）證實範圍buff保留施法者清單項，NPC單獨效果失敗仍有主目標特效；自身PC／寵物／召喚物主目標則跳過整段效果計算。目前候選的顯示與自身整段計算仍待修，先前無NPC特效斷言須另存有Source依據的版本。完整34材料、其他Source失敗目標／控制／AI及正式整合仍開放；未部署，Source／玩家SQL及WAL寫入0。收據：`tools/combat-continuation/integration-lifecycle/build/source-mass-slow-lifecycle-progress-current.json`。\n','utf-8')
audit.update(previous=pin(run/'previous-audit.json'),pending_source_mass_slow_lifecycle=pin(run/'manifest.json'));write(D/'source-summon-lifetime-audit-current.json',audit);proof.update(status='passed-integrated1206-unchanged-plus-separate3750-Source76-lifecycle-Source-pipeline-open',previous_progress=pin(run/'previous-proof.json'),pending_source_mass_slow_lifecycle=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(current)]);write(D/'Source-lifecycle-progress-verified-current.json',proof);following=dict(continuation);following['source_mass_slow_lifecycle_progress']=pin(index);write(C,following);assert {k:v for k,v in read(C).items()if k!='source_mass_slow_lifecycle_progress'}==continuation
for p,h in anchors.items():assert sha(p)==h
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
print(json.dumps(dict(status=v['status'],native_cases=3750,known_checks=qm['known_checks'],Source_pipeline_consumers_open=True,formal_unchanged=True,full_goal_achieved=False)))
