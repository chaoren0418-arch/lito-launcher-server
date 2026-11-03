"""Seal the MP commit increment and leave HP/AI/full Source requirements open."""
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
names=['companion-combat-scene-build','companion-combat-scene-reproduction','companion-debit-boundary-before','companion-debit-boundary-after','companion-debit-scene-before','companion-debit-scene-after','companion-debit-frame-before','companion-debit-frame-after','companion-combat-actors','companion-combat-gates','companion-combat-origin'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]];assert runtime['sha256']=='30d414f0c0c8f027121c900fc75917dcfaba45d9b58c2a4e17c4008f5fae755b'
for name in names:
 if not name.endswith('-before'):assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert build['changed']==['local/lito381/server/NativeCombatAttacker.class'] and build['fresh']==['local/lito381/server/NativeCompanionCombatScene.class'] and build['new_player_fields_workers_schedulers_ThreadLocals_global_registries']==0
old=manifests['companion-debit-boundary-before']['observations'];new=manifests['companion-debit-boundary-after']['observations'];sold=manifests['companion-debit-scene-before']['observations'];scene=manifests['companion-debit-scene-after']['observations'];fold=manifests['companion-debit-frame-before']['observations'];frame=manifests['companion-debit-frame-after']['observations'];actors=manifests['companion-combat-actors']['observations'];gates=manifests['companion-combat-gates']
assert old['cases']==new['cases']==old['actual_postlookup_breakpoints']==new['actual_postlookup_breakpoints']==8 and old['stale_MP_debits']==8 and new['stale_MP_debits']==0 and new['actual_serialized_writers']==8
assert sold['cases']==scene['cases']==24 and sold['master_scene_ghost_debits']==4 and scene['master_scene_ghost_debits']==0 and sold['actual_MP_debit_total']==162 and scene['actual_MP_debit_total']==108
assert fold['cases']==frame['cases']==fold['actual_accepted_Frames']==frame['actual_accepted_Frames']==6 and fold['orphan_MP_debits']==6 and frame['orphan_MP_debits']==0 and actors['cases']==90 and gates['cases']==352
for name in ['companion-debit-frame-before','companion-debit-frame-after']:assert manifests[name]['native_method']=='debit' and manifests[name]['native_entry_bytecode_index']==0
for row in gates['results']:
 m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
 if 'gates-regressions' in row['manifest']['path']:
  for child in m['results']:verify(read(Path(child['manifest']['path'])))
count=new['cases']+scene['cases']+frame['cases']+actors['cases']+gates['cases'];known=new['checks']+scene['checks']+frame['checks']+actors['checks']+gates['known_checks'];assert count==480
run=D/'companion-combat-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[3]='Finish exact Source landing/terrain/door projection and real cross-instance showId failure semantics. WORLD companion MP debit now has original-JAR JDI proof for lookup-to-commit source/custody/actor/scene atomicity and frozen accepted-Frame recipient rejection, plus physical scene/numeric gates. Full reflected HP damage/effect publication boundaries, MP debit/recipient transfer across a STOPPED change during an already-owned critical section, all HP-writer/publication ordering, AI/hate/poison, non-healing Proc, specialized corpse/ground stamps and remaining factory/material/control matrices stay open; keep player/BATTLE action identities strict.'
v=dict(status='passed-scoped-WORLD-companion-MP-source-paired-and-Frame-retirement-coverage-open',runtime=runtime,integrated_runtime=integrated['runtime'],parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(postlookup_before=old,postlookup_after=new,scene_before=sold,scene_after=scene,accepted_Frame_before=fold,accepted_Frame_after=frame),remaining=remaining,all_HP_MP_writer_and_publication_races_verified=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\nWORLD隨從MP提交續作 `'+runtime['sha256'][:12]+'…` 同SHA **480案／已計數'+format(known,',')+'項檢查**。JDK JDI僅停住原JAR的實際事件執行緒，未改寫或覆蓋遊戲class；8案精準停在原debit的成功live查詢後，再由另一條真實執行緒完成主人／施法者角色退役、同ID替換或原生成功傳送，重現8次過期MP扣除。新8案在相同原數值核心位置暫停時，競爭寫入確實排在同一工作者的實際Source動作鎖後，原有效扣款恰一次，再完成退役／替換／換圖，過期扣款歸0。此為可控制的原生bytecode事件點與真實鎖等待，非以覆蓋PC或虛構Frame替代。\n\n另24案實際物件／主人分離場景與請求負值、零、正常、INT_MAX對照，4次依主人場景誤扣遠端MP歸0，原requested／available-MP截斷與正數核心不變。再6案由真正AI進入原Calc、原PC佇列並先提交HP，在原debit入口bytecode 0前退役施法者的凍結Frame：舊版6次NPC被扣MP而玩家無法收取，新版6案全部拒絕NPC副作用，已提交HP不重放。Frame這組事件點是debit入口，與前8案成功查詢後的事件點分開記錄。\n\nWORLD分支把施法者／主人的實際成對動作鎖、Hub、原custody、物件與NPC狀態鎖延續至原MP改值完成；已有accepted Frame沿用原目標Binding與validFrame，不重新捕捉新角色。正常主人換圖可維持原場景有效物件，施法者仍需符合物件場景與名冊。只改NativeCombatAttacker並新增無欄位helper；全部原數值、live、傷害與特效方法逐一正規化bytecode一致，原debit僅改名後由WORLD邊界委派，其他家族走原本體。整個其餘JAR逐位元不變，凍結重建同SHA，沒有新玩家欄位／工作池／時鐘／ThreadLocal／名冊／SQL／檔案／玩家庫存依賴。\n\n90案凍結九家族真實AI／MP上下限／反射／Mortal／Joy／角色與新生命回歸，另352案共用Ref／真實反饋／成對／範圍／Main68／73全部同SHA通過。3組新原生fixture classpath各31個遊戲class確認來自此JAR，唯讀來源核對不另計行為案例。夾具final繼承及傳送外加鎖造成的失敗保留，新版傳送採真實原生入口並保留無鎖內IO斷言；原有已封存Java／測試未覆寫。\n\n反射HP／特效完整提交與送包、已擁有MP臨界區中途STOPPED時的扣除／玩家收取合併、全部HP寫入／送包排序，以及AI／仇恨／毒傷、NPC factory、其餘材料／職業／目標／Ctrl／範圍／外控與最終同SHA全回歸仍開放。不同SHA不混加，整合檢查點仍3bda9d5d的1,206案；131停用、超容量馴服整筆拒絕、競技／記憶體／資料限制保留。正式檔與固定基準不變，未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-combat-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| WORLD特化Proc續作 |'));lines.insert(at+1,'| WORLD隨從MP提交續作 | **`'+runtime['sha256'][:12]+'…`** 同SHA **480案**；JDI原JAR查詢後8次過期扣款、4次主人場景誤扣及6次退役Frame孤立扣款歸0。延續真實成對／原容器／物件／生命鎖，原數值核心保留；90案九家族及352案Ref／成對／Main回歸通過。完整HP／MP排序與其餘Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-combat-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],runtime=runtime,integrated_runtime=integrated['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-combat-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_combat_scene=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-480-WORLD-companion-MP-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_combat_scene=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionCombatScene.java','CompanionCombatSceneMethods.java','CompanionCombatSceneDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['companion_combat_progress']=pin(D/'companion-combat-progress-current.json');write(continuation,following);assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_combat_progress'}==previous
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
