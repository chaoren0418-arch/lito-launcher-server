"""封存限定 MP 轉移增量；其餘技能、HP 與完整 Source 驗證維持開放。"""
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
names=['companion-mana-transfer-build','companion-mana-transfer-reproduction','companion-mana-close-before','companion-mana-close-after','companion-mana-frame-after','companion-mana-admission','companion-mana-boundary-after','companion-mana-scene-after','companion-mana-actors','companion-mana-gates','companion-mana-origin'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]];assert runtime['sha256']=='c8520dd42e49e705a191312556dd4bda10900e6ece005c028cfa37cee4276c6c'
for name in names:
 if name!='companion-mana-close-before':assert indices[name]['runtime']==runtime,name
assert manifests['companion-mana-close-before']['runtime']['sha256']==build['base']['sha256']=='30d414f0c0c8f027121c900fc75917dcfaba45d9b58c2a4e17c4008f5fae755b'
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert build['changed']==['local/lito381/server/NativeEquipmentDefense.class','local/lito381/server/NativePlayerCommit.class'] and build['fresh']==['local/lito381/server/NativeCompanionManaTransfer.class'] and build['new_player_fields_workers_schedulers_ThreadLocals_global_registries']==0
old=manifests['companion-mana-close-before']['observations'];new=manifests['companion-mana-close-after']['observations'];frame=manifests['companion-mana-frame-after']['observations'];admission=manifests['companion-mana-admission']['observations'];boundary=manifests['companion-mana-boundary-after']['observations'];scene=manifests['companion-mana-scene-after']['observations'];actors=manifests['companion-mana-actors']['observations'];gates=manifests['companion-mana-gates']
assert old['cases']==new['cases']==old['actual_accepted_Frames']==new['actual_accepted_Frames']==6 and old['orphan_MP_transfers']==6 and new['orphan_MP_transfers']==0 and new['actual_close_waits']==6
assert frame['cases']==frame['actual_accepted_Frames']==6 and frame['orphan_MP_debits']==0
assert admission['cases']==admission['actual_accepted_Frames']==12 and admission['STOPPED_rejected_pairs']==admission['DRAINING_owned_committed_pairs']==6
assert boundary['cases']==boundary['actual_serialized_writers']==8 and boundary['stale_MP_debits']==0 and scene['cases']==24 and scene['master_scene_ghost_debits']==0 and scene['actual_MP_debit_total']==108 and actors['cases']==90 and gates['cases']==352
for name in ['companion-mana-frame-after','companion-mana-admission']:assert manifests[name]['native_method']=='drain' and manifests[name]['native_entry_bytecode_index']==0
for row in gates['results']:
 m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
 if 'gates-regressions' in row['manifest']['path']:
  for child in m['results']:verify(read(Path(child['manifest']['path'])))
rows=[new,frame,admission,boundary,scene,actors];count=sum(r['cases'] for r in rows)+gates['cases'];known=sum(r['checks'] for r in rows)+gates['known_checks'];assert count==498
run=D/'companion-mana-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[3]='WORLD equipment MP transfer now holds the original Source player pair, Hub/custody/actor/timed locks, existing lifetime admission and owned PC vital queue through original NPC numeric debit and original player recovery/completion. Actual accepted-Frame JDI proves STOPPED critical-section exclusion, STOPPED-before-entry pair rejection and DRAINING-owned completion with unlocked exact output. Full reflected HP damage/effect publication boundaries, every HP writer and asynchronous ordering, AI/hate/poison, non-healing Proc, specialized corpse/ground stamps, remaining factories/material/class/target/Ctrl/area/control matrices and final same-SHA full regression remain open. Retain strict player/BATTLE identities.'
v=dict(status='passed-scoped-atomic-WORLD-companion-MP-transfer-lifetime-coverage-open',runtime=runtime,integrated_runtime=integrated['runtime'],parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(STOPPED_split_before=old,STOPPED_atomic_after=new,retired_Frame=frame,admission=admission),remaining=remaining,all_HP_MP_writer_and_publication_races_verified=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\nWORLD隨從MP扣除／玩家收取合併續作 `'+runtime['sha256'][:12]+'…` 同SHA **498案／已計數'+format(known,',')+'項檢查**。6案原生AI→Calc→accepted PC Frame在原JAR成功NPC查詢後由JDI停住實際工作者：父層30d414f0有6次NPC扣7MP而玩家未收到；新候選6案關閉執行緒確實阻塞在該工作者持有的既有admission fence，NPC扣7、玩家收7、只送一次原生MP封包。另12案在新轉移入口bytecode 0先完成真實生命週期切換，STOPPED的6案雙方MP均不動，DRAINING的6案讓既有工作雙方恰一次完成；6案退役Frame亦全部拒絕NPC副作用。三組均為真正排程的accepted Frame，未虛構Frame、覆蓋遊戲class或新增工作者。\n\n原子區先保留原生完成收據，再取得實際成對Source動作鎖、Hub、原custody、物件／NPC狀態，最後延續既有admission及該Frame原queue的vital鎖；重驗角色、物件生命與實際場景後執行原數值debit。玩家收取沿用原recover數值／FIFO完成本體的精確複本，只改為既有Frame queue與預留收據，避免Hub內重新取得玩家status monitor。既有Source邊界將IO延後至全部動作、Hub、容器、物件、狀態、vital及admission鎖外；實際加密MP封包與主人資源隔離已斷言。\n\n只改equipment的一個呼叫及新增原recover複本；全部原PlayerCommit方法、數值與欄位均正規化一致，其他JAR逐位元不變，僅新增無欄位helper。凍結輸入重建同SHA，3組原生classpath各32個遊戲class確認來自同一JAR。8案查詢後真實角色／同ID／傳送競爭、24案實際場景／數值、90案凍結九家族與352案Ref／成對／範圍／Main68及73同SHA重播通過。失敗試跑保留；不同SHA不混加，整合檢查點仍3bda9d5d的1,206案。\n\n完整反射HP／特效與所有HP寫入／送包排序、AI／仇恨／毒傷、其餘材料／職業／Ctrl／範圍／Source外控及最終全回歸仍開放。131停用、超容量馴服整筆拒絕、競技與記憶體限制保留。未部署、未啟動遊戲、未寫Source／玩家SQL／WAL。[限定收據](../../tools/combat-continuation/integration-lifecycle/build/companion-mana-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| WORLD隨從MP提交續作 |'));lines.insert(at+1,'| WORLD隨從MP轉移生命週期 | **`'+runtime['sha256'][:12]+'…`** 同SHA **498案**；原生查詢後STOPPED造成的6次扣除／收取分裂歸0，6案真實關閉等待，12案STOPPED／DRAINING與6案退役Frame通過。原數值／完成本體保留、所有內部鎖外送包；含352案成對／範圍／Main68及73。完整HP與其餘Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-mana-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],runtime=runtime,integrated_runtime=integrated['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-mana-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_mana_transfer=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-498-atomic-companion-MP-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_mana_transfer=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionManaTransfer.java','CompanionManaTransferMethods.java','CompanionManaTransferDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['companion_mana_progress']=pin(D/'companion-mana-progress-current.json');write(continuation,following);assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_mana_progress'}==previous
write(D/'companion-mana-wip-current.json',dict(status='sealed-scoped-498-with-all-native-fixtures-terminal',runtime=runtime,progress=pin(D/'companion-mana-progress-current.json'),full_goal_achieved=False))
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
