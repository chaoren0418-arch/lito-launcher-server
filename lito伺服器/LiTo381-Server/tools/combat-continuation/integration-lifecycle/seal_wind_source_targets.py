"""Seal the actual Source167 area0 correction; keep NPC/material/Source coverage open."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';continuation=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json');integrated=read(R/'current.json');ih=sha(R/'current.json');previous=read(continuation)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
names=['wind-source-target-build','wind-source-target-reproduction','wind-source-target-before','wind-source-target-after','wind-source-npc-before','wind-source-npc-after','wind-target-snapshot','wind-source-area-regressions','wind-source-target-gates'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]];assert runtime['sha256']=='4627ff770917d480b3e6c73f02f3c126be79cef0126e550450d41c43eb68848e'
assert build['base']['sha256']=='f213c87f8ee21f89037f4d316b61555bff394f244e56662cd5dcdaab5074a96a'
for name in names:assert indices[name]['runtime']==(build['base'] if name.endswith('-before') else runtime),name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert build['changed']==['local/lito381/server/BattleSpellCatalog$WindShackleSpell.class','local/lito381/server/BattleSpellCatalog.class','local/lito381/server/ClientSession.class','local/lito381/server/CombatDatabase.class']
assert build['fresh']==['local/lito381/server/NativeSkillTargetMetadata$Row.class','local/lito381/server/NativeSkillTargetMetadata.class','local/lito381/server/NativeWindSourceTargets.class']
obs=lambda name:manifests[name]['observations']
before=obs('wind-source-target-before');after=obs('wind-source-target-after');npcBefore=obs('wind-source-npc-before');npc=obs('wind-source-npc-after');snapshot=obs('wind-target-snapshot');gates=manifests['wind-source-target-gates'];regressions=manifests['wind-source-area-regressions'];evidence=read(D/'wind-source-target-evidence-current.json');verify(evidence)
assert before['scenarios']==after['scenarios']==28 and before['secondary_player_status_hits']==94 and after['secondary_player_status_hits']==0
assert npcBefore['scenarios']==npc['scenarios']==8 and npcBefore['secondary_actor_status_hits']>0 and npc['secondary_actor_status_hits']==0
assert snapshot['cases']==1 and snapshot['metadata_rows']==807 and snapshot['observed_SELECT_queries']==11 and snapshot['SQL_writes']==0 and not snapshot['created_Hub']
assert gates['cases']==352 and regressions['cases']==232
for row in gates['results']:
 m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
 if 'gates-regressions' in row['manifest']['path']:
  for child in m['results']:verify(read(Path(child['manifest']['path'])))
count=after['cases']+npc['cases']+snapshot['cases']+gates['cases']+regressions['cases'];checks=after['checks']+npc['checks']+snapshot['checks']+gates['known_checks']+regressions['checks']
run=D/'wind-source-target-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
shutil.copyfile(D/'wind-source-target-evidence-current.json',run/'Source-evidence.json')
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining'])+['Source167 target=buff/area=0/through=0 is verified directly in shared380_db. Native single-target player and ordinary world/quest/time effect lists now use startup-only immutable Source metadata, preserving existing selection/admission/cost/numeric/output paths. Controlled NPC primary selection, actual actor wind status, attack/spell interval consumers, Source-pair/actor lifecycle matrices, other area shapes/target flags and full material/Source controls remain open. Loading807 metadata rows is not807 skills wired or qualified. The old167 third-player/AOE fixture asserts the old native bug and is not current Source semantic qualification; old records and goldens remain immutable. Final same-SHA full regression and cleanup still remain.']
v=dict(status='passed-scoped-Source167-single-target-live-metadata-coverage-open',runtime=runtime,parent=build['base'],integrated_runtime=integrated['runtime'],receipts=refs,Source_evidence=pin(run/'Source-evidence.json'),legacy_fixture_semantics_erratum=evidence['erratum'],qualified_scope=dict(cases=count,known_checks=checks,check_count_complete=False),observations=dict(player_before=before,player_after=after,NPC_before=npcBefore,NPC_after=npc,metadata=snapshot),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,all_Source_semantics_verified=False,all_target_metadata_consumers_wired=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\n技能167 Source目標語意修正 `'+runtime['sha256'][:12]+'…` 同SHA **'+str(count)+'案／已計數'+format(checks,',')+'項檢查**。直接在既有共用380_db做唯讀查詢，167為target=buff／target_to=3／ranged=-1／area=0／through=0／type=1／id=64；Source SkillsTable直讀此欄位，makeTargetList以area0走單體。舊原生卻用area=-1，28情境32次實際正常／Ctrl施法重現94次非選定角色中招；新候選28情境33次施法為0，隔牆不套狀態且保留Source空目標仍消耗一次材料／MP。兩個真正普通NPC同場另以8情境對照，舊9次施法38次非選定角色／NPC中招，新8次全部為0。\n\n既有CombatDatabase啟動唯讀交易載入807列不可變目標資料，保留合法空目標佔位列；807是資料列數，不是技能驗收數。六個167欄位讀取器接至此快照，原資料列及建構契約area改為0；只在已完成選定、入場與扣款後的四種原效果清單套Source目標／穿透篩選，原機率、數值、既有競技規則及輸出本體保留。沒有角色／NPC註冊表、新玩家欄位、時鐘或施法時SQL；資料快照只含字串／數值，啟動觀察11次SELECT、資料寫入0。獨立精確逆向稽核及凍結重建同SHA。\n\n**舊167夾具語意更正**：AreaMaterialProbe明確要求第三人中招；其記錄只能證明舊程式行為，不能作為目前Source單體語意驗收。舊夾具與golden完整保留，不改斷言湊綠燈。新Source原始封包對照、232案原161材料／普通NPC／生命週期及352案Ref／成對／範圍／Main68與73通過。受控NPC的167選定／狀態／攻擊及施法間隔、成對生命週期、其餘目標形狀／34材料與完整Source外控仍開放；此候選不提升整合檢查點、不與其他SHA混加。未部署、未啟動遊戲；131／容量、競技、記憶體及Source／玩家SQL／WAL資料限制保留。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/wind-source-target-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,l in enumerate(lines) if l.startswith('| 技能161 NPC相消外控 |'));lines.insert(at+1,'| 技能167 Source單體目標修正 | **`'+runtime['sha256'][:12]+'…`** 同SHA **'+str(count)+'案**；唯讀確認Source area0／through0，非選定角色／NPC誤中降為0；807列不可變啟動目標快照，含Main68／73。舊167第三人斷言更正為行為紀錄；受控NPC狀態／間隔及完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/wind-source-target-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
write(D/'wind-source-target-progress-current.json',dict(status=v['status'],runtime=runtime,integrated_runtime=integrated['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False))
audit.update(previous=pin(run/'previous-audit.json'),pending_wind_source_target=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-'+str(count)+'-Source167-target-scope',previous_progress=pin(run/'previous-proof.json'),pending_wind_source_target=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/n) for n in ['NativeSkillTargetMetadata.java','NativeWindSourceTargets.java','WindSourceTargetMethods.java','WindSourceTargetDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['wind_source_target_progress']=pin(D/'wind-source-target-progress-current.json');write(continuation,following);assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='wind_source_target_progress'}==previous
write(D/'wind-source-target-wip-current.json',dict(status='sealed-scoped-all-native-fixtures-terminal',runtime=runtime,progress=pin(D/'wind-source-target-progress-current.json'),remaining=remaining,full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],cases=count,known_checks=checks,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
