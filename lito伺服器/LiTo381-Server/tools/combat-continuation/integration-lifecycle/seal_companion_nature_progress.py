"""Seal Nature's actor-scene increment without promoting or narrowing the unfinished full Source goal."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';continuation=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
integrated=read(R/'current.json');ih=sha(R/'current.json');previous_continuation=read(continuation);proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json')
for ref in proof['protected']+proof['live_helpers']+proof['experimental_live_helpers']+proof['docs']:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
names=['companion-nature-scene-build','companion-nature-scene-reproduction','companion-nature-scene-before','companion-nature-scene-after','companion-nature-scene-after-stopped','companion-nature-publication-before','companion-nature-publication-after','companion-nature-transfer-scene','companion-nature-regressions','companion-nature-gates-regressions','companion-nature-stage-regressions'];indices={};manifests={}
for name in names:
 index=read(D/(name+'-current.json'));assert index['status'].startswith(('built','passed','observed')),(name,index['status']);assert sha(Path(index['manifest']['path']))==index['manifest']['sha256'];assert sha(Path(index['runtime']['path']))==index['runtime']['sha256'];indices[name]=index;manifests[name]=read(Path(index['manifest']['path']))
runtime=indices['companion-nature-scene-build']['runtime'];build=manifests['companion-nature-scene-build']
for name in names:
 if name not in ['companion-nature-scene-before','companion-nature-publication-before']:assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(Path(ref['path']))==ref['sha256'] and sha(R/Path(ref['path']).name)==ref['sha256']
for row in build['Source']:
 for ref in row.values():assert sha(Path(ref['path']))==ref['sha256']
old=manifests['companion-nature-scene-before']['observations'];new=manifests['companion-nature-scene-after']['observations'];stop=manifests['companion-nature-scene-after-stopped']['observations'];pold=manifests['companion-nature-publication-before']['observations'];pnew=manifests['companion-nature-publication-after']['observations'];transfer=manifests['companion-nature-transfer-scene']['observations'];reg=manifests['companion-nature-regressions'];gates=manifests['companion-nature-gates-regressions'];stage=manifests['companion-nature-stage-regressions']
assert old['cases']==8 and old['old_scene_unresolved_targets']==4
assert new['cases']==32 and new['actual_waiting_casts']==16 and new['old_scene_unresolved_targets']==new['new_scene_ghost_hits']==0
assert stop['cases']==stop['actual_waiting_casts']==4 and stop['old_scene_unresolved_targets']==stop['new_scene_ghost_hits']==0
assert pold['cases']==pnew['cases']==4 and pold['stale_deletion_effects']==4 and pnew['stale_deletion_effects']==0
assert transfer['cases']==8 and transfer['actual_owner_flush_faults']==4
assert reg['cases']==180 and gates['cases']==332 and stage['cases']==124
count=sum(x['cases'] for x in [new,stop,pnew,transfer])+reg['cases']+gates['cases']+stage['cases'];assert count==684
known=sum(x['checks'] for x in [new,stop,pnew,transfer])+reg['checks']+gates['known_checks']+stage['known_checks']
run=D/'companion-nature-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
target=run/'resurrection-scene-ports-current.json';shutil.copyfile(D/target.name,target);refs['next_resurrection_ports']=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[0]='Finish the remaining material/class/target/Ctrl/area/effect matrix. Skills27,29,152,153,157 and now145 have separate actor-scene candidate scopes; neither these results nor the 34-skill count proves full Source coverage.';remaining[1]='Finish broader Return to Nature TAMED/PET/class/effect coverage, resurrection including inactive bodies, and remaining area primary/secondary selectors. Source145 ordinary SUMMON now captures original actor scene/pose/role before waits and retains canonical deletion/transfer; do not reuse live-target publication checks for committed deletion.'
v=dict(status='passed-scoped-Nature-actor-scene-owner-transfer-and-committed-deletion-coverage-open',integrated_runtime=integrated['runtime'],runtime=runtime,parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(before=old,after=new,STOPPED=stop,publication_before=pold,publication_after=pnew,owner_overflow_and_rollback=transfer),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v)
text='\n\n回歸自然145跨角色續作 `'+runtime['sha256'][:12]+'…` 同SHA **684案／已計數'+format(known,',')+'項檢查**。父版8案重現4個原場景漏找；新32案含16次真正主人鎖等待、角色／生命／位置／同ID替換及正常／Ctrl、缺料／MP，漏找與誤命中皆0；另4案STOPPED只完成已持有收據的施法。原背包交回實際遠端主人、刪除保留實際HP，效果與移除只送原場景。新增8案真滿庫存與4次主人儲存故障：兩個原非堆疊物件只落原位置／原圖，失敗完整保留原背包、主人庫存／記憶體帳與所有巢狀落地保留；結束無殘留施法Context／主人權限。\n\n提交刪除後、送包前的同ID替換另重現4案過期特效／移除：原Stage只掃原場景玩家，未見遠端主人持有的原場景新物件。現在沿用既有已連線名冊按物件場景補入主人，4案歸0；保留原已提交刪除／Source169與背包交易，不以活體判斷取消成功刪除。原Nature的136案封包與預期值逐一相等，8次材料故障、36案生命週期／競技、332案成對／範圍／Main68／73皆通過；共用Stage另重播124案主人退休／worker／回復重試／死亡送包／期限。凍結輸入可重建同SHA，僅PC的Nature目標查詢、Nature入口／入場接線及Stage刪除主人查詢三個既有class與一個無狀態helper；不新增玩家欄位、ThreadLocal、工作池、時鐘、全域名冊或SQL／檔案交易。\n\n舊生命週期測試的凍結副本保留原斷言與原檔，僅把私有收據數1改為有保留收據、將原始封包STOPPED等待點明確置於真正成對鎖，並區分同ID替換仍可送施法者MP與主人角色退役取消整批舊封包；已提交扣費仍恰一次。不同SHA的前輪證據不相加，整合檢查點仍3bda9d5d的1,206案；其餘材料／完整Source、復活、範圍次目標、視野／AI／毒傷／Ref／NPC建構／長數量容量與外控及最終全回歸持續開放。競技／記憶體／資料限制保留，131停用、超容量馴服整筆拒絕。未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-nature-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| 負向與地縛跨角色續作 |'));lines.insert(at+1,'| 回歸自然跨角色續作 | 145 **`'+runtime['sha256'][:12]+'…`** 同SHA **684案**；補原場景查詢及施法前角色／生命／位置捕捉，保留遠端主人原背包、實際HP與原場景移除。4案提交後同ID過期刪除缺口已重現修復；8案滿庫存／4次主人儲存故障完整回滾。含既有Nature／競技、成對／範圍、Main68／73及124案共用Stage回歸；不混加前輪SHA，完整Source及其餘技能仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-nature-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],integrated_runtime=integrated['runtime'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-nature-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_nature=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-684-Nature-actor-scene-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_nature=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionNature.java','CompanionNatureMethods.java','CompanionNatureDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
continuing=dict(previous_continuation);continuing['companion_nature_progress']=pin(D/'companion-nature-progress-current.json');write(continuation,continuing);assert sha(R/'current.json')==ih
for ref in proof['protected']+proof['live_helpers']+proof['experimental_live_helpers']+proof['docs']:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
assert {k:v for k,v in read(continuation).items() if k!='companion_nature_progress'}==previous_continuation
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
