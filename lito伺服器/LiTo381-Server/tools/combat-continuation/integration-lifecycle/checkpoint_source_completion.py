"""Pin inventory, goal definition and arena-poison program work; never infer native qualification."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,zipfile,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';index=D/'source-program-update-progress-current.json';proof=read(proofpath)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
latest=read(D/'source-arena-poison-build-current.json');repro=read(D/'source-arena-poison-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json')
for ref in [latest,repro,inventory]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime'] and repro['status'].startswith('passed-frozen-byte-identical')
previous=read(index);verify(previous);old=read(Path(previous['manifest']['path']));build=read(Path(latest['manifest']['path']));inv=read(Path(inventory['manifest']['path']))
goalpath=D/'source-completion-goal-current.json'
if goalpath.exists():
 g=read(goalpath)
 if g.get('status')=='active':inv['goal']=dict(system_goal_reset='created-active',status='active',objective=g['goal']['objective'],receipt=pin(goalpath))
assert build['base']==previous['runtime'] or ('build'in old and read(Path(old['build']['path'])).get('base')==build['base']);verify(build['inputs']);verify(build['asm']);verify(inv['Source_configuration_inputs']);verify(inv['artifacts'])
next_contracts=read(D/'source-next-producer-contracts-current.json');verify(next_contracts);assert next_contracts['runtime']==latest['runtime'];next_manifest=read(Path(next_contracts['manifest']['path']));verify(next_manifest['Source_excerpts']);verify(next_manifest['candidate_static_callers'])
runtime=latest['runtime'];digest=runtime['sha256'];sqlbase=old['readonly_SQL_consumers_byte_retained']['base'];verify(sqlbase)
extra=dict(path=str(S.parents[1]/'lito登入器/LiTo381-Launcher/app/compat/litoddraw.dll'),sha256='e47bd6c6ec9979c8747a523e4457133802d19f673c8dc1e67707abfa96216a8a');verify(extra)
anchors={p:sha(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json',D/'source-raw-producer-build-current.json',D/'source-raw-producer-reproduction-current.json',D/'source-raw-producer-policy-current.json']}
retained=[]
with zipfile.ZipFile(sqlbase['path'])as sql,zipfile.ZipFile(runtime['path'])as now:
 for name in old['readonly_SQL_consumers_byte_retained']['entries']:assert sql.read(name)==now.read(name),name;retained.append(name)
assert len(retained)==20
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md')]:shutil.copyfile(p,run/name)
for name in ['NativeSourceArenaPoison.java','SourceArenaPoisonMethods.java','SourceArenaPoisonDelta.java','SourceArenaPoisonLoad.java','build_source_arena_poison.py','reproduce_source_arena_poison.py','SourceCompletionCensus.java','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
for stem in ['source-arena-poison-build','source-arena-poison-reproduction','source-completion-inventory']:shutil.copyfile(D/(stem+'-current.json'),run/(stem+'-current.json'))
count=int(re.search(r'inverse: (\d+) original methods',Path(build['log']['path']).read_text('utf-8'))[1])
remaining=[
 'Program completion is still open. Review all58 actual reagent-positive rows and all applicable807 static rows/180 historical models; old34 is not the complete denominator. Preserve Source26 no-Ctrl report as unresolved until actual cursor acceptance.',
 'Other production HP roots still need Source semantic and lifecycle wiring: magic-doll proc, fatality single/area, cube ignition/harmony player and nonplayer callbacks, ground pulses, companion/bot and natural NPC physical/magic/area AI, group healing and undead inversion.61 direct HP call sites are a locator census, not61 missing or qualified cases.',
 'Natural arena Quest/Time poison now has install/HP/result/reward/final-frame wiring and Source HPBAR. New native qualification remains zero. Source-kind applicability, GM encounters, primary/secondary, actual pair/receiver waits, death/revival/role/scene/actor replacement, IO and failure/retry/Main shutdown still require concentrated validation.',
 'Full external controls remain open:252 explicit property-key references in8 Source config classes are inventoried;52 have exact candidate literals. Computed keys, aliases, class damage matrices, unused/nonproduct keys, SQL controls and actual reader-to-consumer paths require review. Missing exact text is not evidence of a missing implementation. NPC factories/AI and hit-reaction suppression consumers remain open.',
 'Real equipped-player Chinese MySQL readonly reload remains deferred by user. The old static/empty-query receipt does not qualify real equipment;20 SQL consumers are byte-identical. No new DB access is performed.',
 'After all program updates: compose in isolation with formal0.6.71 resources/icons/special-load DLL/GM7200 preserved, run the full same-SHA backend matrix, then concentrated real cursor/foreground/multiwindow/lobby acceptance. BATTLE is the product scope; retain WORLD fallback without adding a WORLD product. No deployment.'
]
scope=[
 'The natural-monster poison spell root captures actual NPC identity/life/generation/pose before the existing ordered pair wait. Successful Quest/Time installation validates that frozen selection; venom installations retain the already captured attack context and source life. A new role or same-id replacement cannot refresh authority at the installation boundary. One weak source lease is attached to the monster; it retains no Context/Scope/Receipt or global registry. Ordinary within-map movement is not frozen.',
 'The original applyDamage leaf is replayed behind a poison-only pre-HP guard, including the existing GM branch. All original numerical damage, poison poll clock, accepted costs, waves and battle-resolution bodies survive the independent inverse.',
 'Each original PoisonUpdate carries a detached proof and weak pre-publication viewer bindings. The Hub result stores the authoritative caster once at construction, rather than a new role recovered from a numerical ID. Original getters remain stable. Reward calls take the existing source action lock and revalidate the actual source.',
 'Nine explicit poison output ports use original deferred publication outside action locks, receiver role checks and an inherited output proof at the actual final frame. A replacement poison installation, role/life/encounter change, STOPPED or closed Hub drops stale visuals; original structural completion still runs.',
 'Poison has no invented damage-digit planner or jobs. Source HPBAR is emitted through the existing attacker-only bounded viewer cache and death-hide path; old poison HPBAR fan-out is suppressed. Visual failure cannot undo accepted HP or stop the original reward/wave/resolution body.'
]
v=dict(status='program-updated-arena-poison-provenance-final-output-HPBAR-full-Source-open',runtime=runtime,build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=next_contracts['manifest'],goal=inv['goal'],previous_program_progress=pin(run/'previous-program-progress.json'),previous_qualified_runtime=old['previous_qualified_runtime'],implemented_scope=scope,program_checks=dict(JVM_no_initialization_classes=17,original_methods_independently_inverted=count,byte_identical_frozen_rebuild=True,existing_changed_class_entries=12,new_helper_class_entries=5,poison_output_ports=9,actual_HP_guard=True,stable_result_attribution=True,actual_final_frame_guard=True,new_workers_TLS_registries=False),new_game_runtime_cases=0,new_database_connections=0,actual_role_and_cursor_validation_deferred_by_user=True,readonly_SQL_consumers_byte_retained=dict(base=sqlbase,program=runtime,entries=retained),previous_real_MySQL_evidence=pin(D/'source-equipment-mysql-reload-readonly-origin-current.json'),remaining=remaining,all_owned_JVMs_terminal=True,editable_inputs=[pin(p)for p in inputs.iterdir()],protected=proof['protected'],additional_protected=[extra],all_material_skills_complete=False,all_Source_consumers_wired=False,same_SHA_full_regression_passed=False,actual_client_cursor_verified=False,user_report_fixed=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,new_baseline_created=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
inventory_dir=Path(inventory['manifest']['path']).parent
work.write_text(f'''# Source 與材料技能完成工作清單

更新：2026-10-24。狀態：**進行中，完整補齊尚未完成；先更新程式，後集中驗收，不部署。**

新工作目標：以固定 Source 與正式0.6.71功能為依據，完整補齊競技場材料技能、Source外控、傷害生產端與跨角色生命週期。保留正常／Ctrl、競技關係、目標型別與免疫、原扣料／機率及有界記憶體生命週期；禁止 Source／玩家 SQL／schema／WAL 寫入及正式檔覆寫。LITO產品只有BATTLE；既有WORLD後備保留。

系統目標已於2026-10-24依使用者指示重新建立並啟用（active）。範圍涵蓋58列材料技能、完整Source外控與跨角色生命週期；先完成程式更新，再集中同SHA後端與實機驗收，維持不部署及資料安全限制。

## 盤點依據

- 現行候選：`{digest}`；歷史測試案數不移算到此SHA。
- 現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、61個特定HP消費端直接呼叫點；這些是定位數量，不能當作缺口或通過案數。
- 歷史靜態資料：807技能列、180原生模型、185個Source符號列、234解析路徑；實際材料技能為58列，舊「34個」不是完整分母。新候選尚無完整語意資格化。
- 八個相關Source設定類別：252個明確鍵引用，52列有完整字串定位。動態組鍵／別名需另核對；不得把其餘200列直接判為未實作。SQL外控與其他Source路徑不包含在此鍵數內。
- 逐技能：[807列清單]({inventory_dir.as_posix()}/skills.tsv)；[外控清單]({inventory_dir.as_posix()}/controls.tsv)；[HP生產端]({inventory_dir.as_posix()}/hp-producers.tsv)；[盤點收據]({(D/'source-completion-inventory-current.json').as_posix()})。

下一批生產端已凍結八個Source方法：[Source參考收據]({(D/'source-next-producer-contracts-current.json').as_posix()})。立方體的傷害來源是實際效果NPC；娃娃技能外層none僅代表acceptedSkillPlan，內層仍合成MagicHitCommitPlan，因此要保留普通魔法接受後數字；加傷／減傷則為各自ProcResult貢獻。須依類型補原來源身分、計算次序與生命週期，不能全部改用施法玩家或一般魔法顯示。七個靜態呼叫點仍不等於BATTLE可達性證明。

## 本次已更新的程式

Quest／Time競技怪物毒術在成對等待前固定實際怪物／生命／世代／位置，安裝時核對原捕捉；毒性附魔沿用原攻擊Context及來源生命。扣HP前再次重驗來源與怪物；結果建構時只保存實際施毒者，避免按數字ID認到新角色。獎勵沿用來源動作鎖再驗證，原getter保持穩定。

九個毒傷送包點沿用延後發布，綁定輪詢時接收者；真正Frame重驗來源、目標及接收者，包含重新施毒、退休／換角色／生命、STOPPED與關閉。HPBAR沿用Source僅攻擊者可見快取及死亡隱藏；毒傷維持無額外傷害數字。原毒傷時鐘、數值、成本、波次與結算本體保留。

程式檢查通過：17類JVM不初始化、凍結輸入同位元重建、{count}個原方法獨立inverse及封存差分；20個SQL消費class保持原位元。新增遊戲案0、資料庫連線0。這是程式更新證據，尚非原生或實機驗收。

## 尚未完成與完成門檻

| 順序 | 工作 | 尚需完成 |
| --- | --- | --- |
| P0 | 其餘HP生產端 | 魔法娃娃proc（技能／加傷／減傷分開核對）、fatality單體／範圍、cube ignition／harmony玩家與非玩家回呼（Source以效果NPC為HP來源）、ground pulse、隨從／bot／自然NPC物理魔法及範圍AI、群補與不死反轉。逐一核對Source raw／普通命中語意及實際生命週期，不能全部套一般魔法數字。 |
| P0 | 58材料技能與其餘適用模型 | 每列核對Source型別／權限、自己／友敵、正常／Ctrl、主次／直線／範圍／LOS、免疫與屍體、原材料／HP／MP／機率／回滾；先前專項只保留各SHA範圍。 |
| P0 | 完整外控 | 從讀取器追到實際消費端，補動態鍵／別名與SQL控制；NPC工廠／AI、受傷動作抑制消費端仍待稽核與接線。無產品適用性的欄位需具體分類，不新增WORLD產品。 |
| P0 | 生命週期 | 所有更新路徑之來源／帳號／角色／實際actor、成對鎖、接受與完成、退役／死復活／場景／同ID替換、最終接收端IO、DRAINING／STOPPED／Main、失敗重試與有界記憶體，仍待完整同SHA矩陣。 |
| P1 | 正式功能隔離合成 | 保全0.6.71資源／圖示／特殊載入DLL／GM7200；實驗祖先不能整顆覆蓋正式JAR。 |
| P1 | 同SHA後端驗證 | 全部程式補齊後集中執行，舊SHA案數不相加。 |
| P1 | 中文MySQL真裝備角色 | 依使用者指示延後；舊b85只有靜態裝備與空背包實際唯讀證據，真角色重載未完成，禁止遷移與寫入。 |
| P1 | 實機游標 | 程式全部完成後集中驗正常／Ctrl、自施／跨角色／範圍、多視窗／前景／大廳。Source26不按Ctrl無法自施仍未修復驗收，合成自施封包不能代替游標。 |

完成條件：所有適用程式／Source語意逐項有證據、正式功能隔離合成完成、同SHA全回歸及集中實機驗收通過。**以上尚未全部達成；未部署、未建立新基準。**
''','utf-8')
lines=current.read_text('utf-8').splitlines()
for n,line in enumerate(lines):
 if line.startswith('| 本次更新 |'):lines[n]=f'| 本次更新 | 程式候選 **`{digest[:12]}…`**：新增競技Quest／Time怪物毒術成對前捕捉、安裝及扣HP前、實際施毒者歸屬、獎勵及九個送包點／Frame接線；HPBAR回到Source僅攻擊者快取，毒傷不加傷害數字。17類JVM不初始化、凍結同位元重建及{count}原方法inverse通過；**新增遊戲驗證0案**。[程式收據](../../tools/combat-continuation/integration-lifecycle/build/source-program-update-progress-current.json)。 |'
 if line.startswith('| 外控下一步 |'):lines[n]='| 外控下一步 | 已重新盤點807技能列／58材料列、實際HP呼叫及252個Source明確鍵引用；鍵字／模型／呼叫存在不等於消費端完成。其餘proc／cube／doll／週期／NPC工廠AI／受傷動作、完整材料／Source與生命週期仍開放。[更新後目標與工作清單](source-completion-work-current.zh-TW.md)。程式全部完成後集中驗證，未部署。 |'
current.write_text('\n'.join(lines)+'\n','utf-8')
marker='\n\n2026-10-24 完整缺口盤點與競技怪物毒傷續作：'
doc.write_text(doc.read_text('utf-8').split(marker)[0]+marker+f'`{digest[:12]}…`補Quest／Time怪物毒術成對前捕捉、原安裝身分與HP葉節點前重驗、結果建構時固定實際施毒者、獎勵動作鎖及九個送包點的輪詢時接收身分／延後發布／真正Frame檢查。Proof不持有Context／Scope／Receipt；weak來源與單怪物lease、既有HPBAR有界快取、計時及關閉流程保留。新施毒安裝也會使舊發布證明失效；普通同地圖移動不被凍結。原數值／成本／毒傷時鐘／GM分支／波次／結算本體經{count}原方法inverse保留。17類JVM不初始化與凍結重建同位元；20個SQL class保持原位元，0新遊戲案／0資料庫連線。全技能／Source仍未完成；詳見同目錄source-completion-work-current.zh-TW.md與source-completion-inventory-current.json。舊目標工具拒絕替代未完成目標，未將舊目標虛報完成。維持BATTLE、Ctrl、資料安全及先程式後集中驗收；未部署。\n','utf-8')
continuation=read(C);continuation['source_program_update_progress']=dict(index=pin(index),runtime=runtime,new_game_runtime_cases=0,actual_validation_deferred_by_user=True,complete=False);continuation['source_completion_work']=dict(inventory=pin(D/'source-completion-inventory-current.json'),work=pin(work),goal=inv['goal'],complete=False);continuation['source_next_producer_contracts']=dict(index=pin(D/'source-next-producer-contracts-current.json'),runtime=runtime,program_wired=False,complete=False);continuation['source_arena_poison_progress']=dict(index=pin(index),runtime=runtime,program_wired=True,native_qualified=False,complete=False);write(C,continuation)
audit=read(auditpath);audit['source_program_update_progress']=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,remaining=remaining);write(auditpath,audit)
proof['docs']=[pin(current),pin(doc),pin(work)];proof['source_program_update_progress']=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(extra);write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=[extra],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,inventory=inventory['manifest'],original_methods_inverted=count,new_game_runtime_cases=0,new_database_connections=0)))
