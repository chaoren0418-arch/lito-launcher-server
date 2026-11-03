"""Seal the exact-SHA 161 increment; retain the full material/Source objective as open."""
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
assert integrated['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df'
names=['companion-forest-area-build','companion-forest-area-reproduction','material-catalog-audit','companion-forest-scene-before','companion-forest-scene-after','companion-forest-lifecycle-after','companion-forest-publication-after','companion-forest-battle-after','companion-forest-world-after','selector-only-ctrl','companion-forest-area-regressions','companion-forest-gates','companion-forest-suite'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]]
assert runtime['sha256']=='c6611ff7b0099854d4641eb67782fd2095759cb4fbe86078e0fbaf6963c633ea'
assert build['base']['sha256']=='3386173a08686c16a5dad60ce7fa532a2e3541b4e686cabd07f95aac51f0f6a6'
for name in names:assert indices[name]['runtime']==(build['base'] if name.endswith('-before') else runtime),name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert build['changed']==['local/lito381/server/ClientSession.class','local/lito381/server/ForcedCombat.class','local/lito381/server/NativeAreaMaterialAction.class'] and build['fresh']==['local/lito381/server/NativeCompanionForestArea.class','local/lito381/server/NativeSelectorOnlyCtrl.class']
obs=lambda name:manifests[name]['observations']
assert obs('companion-forest-scene-before')['old_scene_unresolved_targets']==4
assert obs('companion-forest-scene-after')['scenarios']==8 and obs('companion-forest-scene-after')['old_scene_unresolved_targets']==obs('companion-forest-scene-after')['new_scene_ghost_hits']==0
assert obs('companion-forest-lifecycle-after')['scenarios']==32 and obs('companion-forest-lifecycle-after')['actual_waiting_casts']>=24
assert obs('companion-forest-publication-after')['scenarios']==16
assert obs('companion-forest-battle-after')['cases']==18 and obs('companion-forest-battle-after')['actual_kinds']==3
assert obs('companion-forest-world-after')['cases']==14 and obs('companion-forest-world-after')['native_wall_cases']==2 and obs('companion-forest-world-after')['invalid_metadata_rows_rejected']==6
assert obs('selector-only-ctrl')['cases']==236 and obs('selector-only-ctrl')['targeted_signature_collision_cases']==10
assert manifests['companion-forest-area-regressions']['cases']==304 and manifests['companion-forest-gates']['cases']==352
assert manifests['material-catalog-audit']['material_rows']==58
for row in manifests['companion-forest-gates']['results']:
 m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
 if 'gates-regressions' in row['manifest']['path']:
  for child in m['results']:verify(read(Path(child['manifest']['path'])))
counted=[obs(n) for n in ['companion-forest-scene-after','companion-forest-lifecycle-after','companion-forest-publication-after','companion-forest-battle-after','companion-forest-world-after','selector-only-ctrl']]
gates=manifests['companion-forest-gates'];area=manifests['companion-forest-area-regressions'];count=sum(x['cases'] for x in counted)+gates['cases']+area['cases'];known=sum(x['checks'] for x in counted)+gates['known_checks']+area['checks'];assert count>=980
run=D/'companion-forest-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining'])+['Skill161 scoped target/pair/commit/deferred-output validation is complete on this SHA. Full Source cancellation immunity controls, NPC spell/AI consumers, refresh/expiry/reset and remaining actor factories are not claimed. Skill167 still requires a bounded actor status and original attack/spell interval consumers plus selected-companion area semantics. Remaining material/class/target/Ctrl/area controls and final same-SHA full regression remain open. The historical 34-method census and the current 58 reagent-positive rows are different denominators; neither is a completed-skill count.']
v=dict(status='passed-scoped-area161-pair-scene-Ctrl-lifecycle-Main-Source-coverage-open',runtime=runtime,integrated_runtime=integrated['runtime'],parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(scene=obs('companion-forest-scene-after'),queued_lifecycle=obs('companion-forest-lifecycle-after'),deferred_publication=obs('companion-forest-publication-after'),arena=obs('companion-forest-battle-after'),ordinary_WORLD=obs('companion-forest-world-after'),selector_Ctrl=obs('selector-only-ctrl')),remaining=remaining,full_goal_achieved=False,all_Source_semantics_verified=False,remaining_material_skills_fully_qualified=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\n技能161受控NPC續作 `'+runtime['sha256'][:12]+'…` 同SHA **'+str(count)+'案／已計數'+format(known,',')+'項檢查**，屬限定範圍驗證。原場景正常／無目標Ctrl、跨角色Source鎖等待、角色／NPC世代／pose／同ID替換、缺材料／MP、STOPPED及提交後過期特效均有實際原生案例；競技真正PET／SUMMON／TAMED的敵方／隊友／自己18案保留原權限。普通WORLD真正型別與自己／他人12案，以及2案原生through=1隔牆施法與6筆非法資料列拒絕通過。Source的TYPE_PROBABILITY=1、TYPE_ATTACK=64，161 through=1可穿透；攻擊位元與Source常數一致，原資料列仍拒絕不支援的type／through組合；錯誤的早期位元試版收據保留，不混加。\n\n沿用既有Context收集、弱主人身分、雙方收據與Source成對鎖，再依Hub／NPC／狀態／既有admission驗證；原161材料、MP、機率核心與generic特效發布保留。空目標及原NPC效果階段補入真正受控物件；只開放已審查的161無目標Ctrl，10種有效定向封包的簽名碰撞維持原解析。僅3個既有class改動、2個無欄位helper，Context與其餘JAR逐位元不變；獨立逆向差異與凍結重建同SHA。304案既有161／167封包及生命週期、352案Ref／成對／範圍／Main68及73在同SHA通過。\n\n唯讀載入盤點為58個含材料資料列；歷史34是待補方法盤點，兩者均不是技能完成數。161的完整相消外控、NPC施法／AI消費端、重複／到期／重置與所有出生工廠，以及167和其餘材料／完整Source外控仍開放。整合檢查點仍為3bda9d5d的1,206案，不與其他SHA混加。保留競技、記憶體、131停用及超容量馴服整筆拒絕限制；未部署、未啟動遊戲、未寫Source／玩家SQL／WAL。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| WORLD反射HP與真正型別驗證 |'));lines.insert(at+1,'| 技能161 NPC／Ctrl成對生命週期 | **`'+runtime['sha256'][:12]+'…`** 同SHA **'+str(count)+'案**；跨圖、真實等待、提交後退役、競技三型別、普通WORLD與through／攻擊位元驗證，含Main68／73。161完整消費端、167與其餘材料／Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-forest-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],runtime=runtime,integrated_runtime=integrated['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-forest-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_forest=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
helpers=['NativeCompanionForestArea.java','NativeSelectorOnlyCtrl.java','CompanionForestAreaMethods.java','CompanionForestAreaDelta.java']
proof.update(status='passed-integrated-1206-unchanged-plus-separate-'+str(count)+'-area161-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_forest=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/n) for n in helpers]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['companion_forest_progress']=pin(D/'companion-forest-progress-current.json');write(continuation,following);assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_forest_progress'}==previous
write(D/'companion-forest-wip-current.json',dict(status='sealed-scoped-all-native-fixtures-terminal',runtime=runtime,progress=pin(D/'companion-forest-progress-current.json'),remaining=remaining,full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
