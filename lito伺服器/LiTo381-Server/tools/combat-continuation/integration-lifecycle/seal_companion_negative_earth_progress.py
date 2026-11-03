"""Seal separate same-SHA scopes; retain the integrated checkpoint and protected formal inputs."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';continuation=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
integrated=read(R/'current.json');ih=sha(R/'current.json');previous_continuation=read(continuation)
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json')
for ref in proof['protected']+proof['live_helpers']+proof['experimental_live_helpers']+proof['docs']:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
names=['companion-negative-scene-build','companion-negative-scene-reproduction','companion-negative-scene-before','companion-negative-scene-after','companion-negative-scene-after-stopped','companion-negative-scene-publication-before','companion-negative-scene-publication-after','companion-negative-regressions','companion-negative-gates-regressions','companion-earth-scene-build','companion-earth-scene-reproduction','companion-earth-scene-before','companion-earth-scene-after','companion-earth-scene-after-stopped','companion-earth-regressions','companion-earth-gates-regressions']
indices={};manifests={}
for name in names:
 index=read(D/(name+'-current.json'));assert index['status'].startswith(('passed','built','observed')),(name,index['status']);assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];assert sha(Path(index['runtime']['path']))==index['runtime']['sha256'];indices[name]=index;manifests[name]=read(Path(index['manifest']['path']))
negative=indices['companion-negative-scene-build']['runtime'];earth=indices['companion-earth-scene-build']['runtime']
controls=read(D/'companion-control-inventory-current.json');assert controls['status'].startswith('verified-readonly') and controls['runtime']==earth and sha(Path(controls['manifest']['path']))==controls['manifest']['sha256'];control_manifest=read(Path(controls['manifest']['path']))
for ref in control_manifest['inputs']+control_manifest['original_inputs']:assert sha(Path(ref['path']))==ref['sha256']
assert manifests['companion-earth-scene-build']['base']==negative
for prefix,runtime,count in [('companion-negative',negative,792),('companion-earth',earth,208)]:
 for suffix in ['scene-reproduction','scene-after','scene-after-stopped','regressions','gates-regressions']:assert indices[prefix+'-'+suffix]['runtime']==runtime
 assert manifests[prefix+'-regressions']['cases']==count and manifests[prefix+'-gates-regressions']['cases']==332
for prefix,before,after,waiting,stopped in [('companion-negative',24,96,48,12),('companion-earth',8,32,16,4)]:
 old=manifests[prefix+'-scene-before']['observations'];new=manifests[prefix+'-scene-after']['observations'];stop=manifests[prefix+'-scene-after-stopped']['observations']
 assert old['cases']==before and old['old_scene_unresolved_targets']==before//2
 assert new['cases']==after and new['old_scene_unresolved_targets']==new['new_scene_ghost_hits']==0 and new['actual_waiting_casts']==waiting
 assert stop['cases']==stop['actual_waiting_casts']==stopped and stop['new_scene_ghost_hits']==stop['old_scene_unresolved_targets']==0
pubold=manifests['companion-negative-scene-publication-before']['observations'];pubnew=manifests['companion-negative-scene-publication-after']['observations'];assert indices['companion-negative-scene-publication-after']['runtime']==negative
assert pubold['cases']==pubnew['cases']==6 and pubold['stale_speed_packets']==4 and pubnew['stale_speed_packets']==0
for name in ['companion-negative-scene-build','companion-earth-scene-build']:
 for ref in manifests[name]['inputs']:assert sha(Path(ref['path']))==ref['sha256'] and sha(R/Path(ref['path']).name)==ref['sha256']
 for row in manifests[name]['Source']:
  for ref in row.values():assert sha(Path(ref['path']))==ref['sha256']
run=D/'companion-negative-earth-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
target=run/'companion-control-inventory-current.json';shutil.copyfile(D/target.name,target);refs['readonly_control_inventory']=pin(target)
target=run/'nature-scene-ports-current.json';shutil.copyfile(D/target.name,target);refs['next_Return_to_Nature_ports']=pin(target)
for name in ['negative-receipt-diagnostic','negative-paired-publication-diagnostic']:
 index=read(D/(name+'-current.json'));assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[0]='Finish the remaining material/class/target/Ctrl/area/effect matrix. Skills27,29,152,153,157 have separate actor-scene candidate evidence; none of these scopes declares the 34 skills or full Source qualified.';remaining[1]='Complete Return to Nature, resurrection including inactive bodies, and remaining area primary/secondary and other selectors with actor-scene identity captured before player-lock waits. Preserve native player predicates, competition policies and material order.'
remaining.append('Finish Source numeric owner action6 parsing, including trimmed and leading-zero forms, while preserving PET and competitive command policies; status1..5 parsing evidence does not qualify dismissal6.')
qualified={}
for prefix,scope in [('companion-negative','negative'),('companion-earth','earth')]:
 counts=[manifests[prefix+'-scene-after']['observations'],manifests[prefix+'-scene-after-stopped']['observations']]
 if scope=='negative':counts.append(pubnew)
 reg=manifests[prefix+'-regressions'];gates=manifests[prefix+'-gates-regressions'];qualified[scope]=dict(cases=sum(x['cases'] for x in counts)+reg['cases']+gates['cases'],known_checks=sum(x['checks'] for x in counts)+reg['checks']+gates['known_checks'],check_count_complete=False)
assert qualified['negative']['cases']==1238 and qualified['earth']['cases']==576
v=dict(status='passed-scoped-negative-and-Earth-Bind-actor-scene-pair-publication-and-Main-full-coverage-open',integrated_runtime=integrated['runtime'],negative_runtime=negative,earth_runtime=earth,receipts=refs,qualified_scope=qualified,readonly_control_inventory=controls,different_SHA_counts_not_added=True,remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v)
paragraph='\n\n負向材料與地縛的跨角色續作另存兩個候選，未提升整合檢查點。29／152／153 `'+negative['sha256'][:12]+'…` 同 SHA **1,238 案**：新增96案含48次主人鎖等待，12案實際 STOPPED 後既有收據完成／新施法拒絕，6案提交後送包前退役，以及既有792案玩家／NPC／伴隨物／競技／故障／生命週期與332案成對／範圍／Main68／73。原場景漏找的24案父版重現中有12案失敗；新圖誤命中與舊圖漏找皆0。另重現速度封包未驗目標生命的4案缺口，補入既有送包分類後歸0；效果仍只送一次、鎖外輸出，角色／生命／位置／同ID替換在等待後均拒絕。正常封包保留真正原生隨機結果與Source90%上限，不假定設定100即必定成功；提交後退役則使用既有typed roll1以確定測到速度封包。原玩家／競技判定、機率、扣料、MP及效果本體保留，既有預期封包逐一相等。\n\n地縛157 `'+earth['sha256'][:12]+'…` 同 SHA **576案**：32案跨場景／Ctrl／缺料／MP與16次等待，4案STOPPED，208案既有玩家／NPC／競技／故障／生命週期及332案成對／範圍／Main；父版8案中4案漏找，新候選歸0。原先只選玩家作成對鎖的缺口已補為捕捉到的伴隨物主人；只改WORLD伴隨物查詢及入場接線，BATTLE方法與玩家判定保留。兩候選皆可從凍結輸入重建同SHA，不新增玩家欄位、ThreadLocal、工作池、時鐘、全域名冊；正式程式、基準、固定輸入及整合版3bda9d5d的1,206案保持原雜湊。不同SHA不相加，34技能／完整Source、其餘範圍與回歸自然／復活、視野／AI／毒傷／Ref／容量／完整外控及最後全回歸仍開放；131維持停用，超容量馴服整筆拒絕。未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-negative-earth-progress-current.json)。\n'
paragraph+='\n既有生命週期測試保留原檔，凍結副本只調整兩項與現行成對契約不符的假設：等待中由私有數量1改為確有收據（父版與新候選實測均為2）；送包前目標退役改驗整批舊封包取消、已提交扣費恰一次，並由真正獨立MP生產端驗一次可解密更新。父版與新候選另案均通過，原鎖等待、角色／場景失效、STOPPED、效果、socket失敗及最終收據歸零保留。外控另做唯讀快照：目前69鍵，既有對照表57列Source技能／傷害鍵均對應；這僅證明設定載入，不代表完整Source公式、消費端或全部外控完成。\n'
doc.write_text(doc.read_text('utf-8')+paragraph,'utf-8')
text=doc2.read_text('utf-8');marker='| 傳送與跨角色目標續作 |';lines=text.splitlines();at=next(i for i,line in enumerate(lines) if line.startswith(marker));lines.insert(at+1,'| 負向與地縛跨角色續作 | 29／152／153 **`'+negative['sha256'][:12]+'…`** 同 SHA **1,238案**，157 **`'+earth['sha256'][:12]+'…`** 同 SHA **576案**；補原場景查詢、施法前捕捉及主人收據，驗角色／生命／位置／同ID替換、STOPPED、提交後送包退役、正常／Ctrl、競技、材料回滾與Main68／73。速度封包退役4案缺口已重現並修復；不同SHA不相加，完整34技能與Source／最終全回歸仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-negative-earth-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],integrated_runtime=integrated['runtime'],negative_runtime=negative,earth_runtime=earth,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-negative-earth-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_negative_earth=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-negative-1238-and-earth-576-scopes',previous_progress=pin(run/'previous-proof.json'),pending_companion_negative_earth=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionNegative.java','CompanionNegativeMethods.java','CompanionNegativeDelta.java','NativeCompanionEarth.java','CompanionEarthMethods.java','CompanionEarthDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
continuing=dict(previous_continuation);continuing['companion_negative_earth_progress']=pin(D/'companion-negative-earth-progress-current.json');write(continuation,continuing)
assert sha(R/'current.json')==ih
for ref in proof['protected']+proof['live_helpers']+proof['experimental_live_helpers']+proof['docs']:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
assert {k:v for k,v in read(continuation).items() if k!='companion_negative_earth_progress'}==previous_continuation
print(json.dumps(dict(status=v['status'],qualified_scope=qualified,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
