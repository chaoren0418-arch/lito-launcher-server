"""Seal the area actor-scene increment without promoting it or closing full Source coverage."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';continuation=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json');integrated=read(R/'current.json');integrated_hash=sha(R/'current.json');previous_continuation=read(continuation)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert integrated['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df'
names=['companion-area-scene-build','companion-area-scene-reproduction','companion-area-scene-before','companion-area-scene-after','companion-area-secondary-before','companion-area-secondary-after','companion-area-lifecycle-after','companion-area-publication-after','companion-area-secondary-retirement-before','companion-area-secondary-retirement-after','companion-area-regressions','companion-area-gates-regressions'];indices={};manifests={}
for name in names:
 index=read(D/(name+'-current.json'));assert index['status'].startswith(('built','passed','observed')),(name,index['status']);verify(index);m=read(Path(index['manifest']['path']));verify(m);indices[name]=index;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]]
assert runtime['sha256']=='6ffd0fc0df883ee02cfbaa6a5df1b91ade871320902c50dacf976b65c3da5120'
for name in names:
 if not name.endswith('-before'):assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert len(build['changed'])==2 and len(build['fresh'])==1 and build['new_player_fields_workers_schedulers_ThreadLocals_global_registries']==0
reg=manifests['companion-area-regressions'];gates=manifests['companion-area-gates-regressions']
assert reg['cases']==800 and not reg['fixture_bodies_changed'] and gates['cases']==332
for row in reg['fixture_sources']:assert row['original']['sha256']==row['frozen']['sha256']
assert sum(r['observations']['cases'] for r in reg['results'] if r.get('stable_wire_equal'))==288
for row in gates['results']:verify(read(Path(row['manifest']['path'])))
obs=lambda n:manifests[n]['observations']
old=obs('companion-area-scene-before');new=obs('companion-area-scene-after');sold=obs('companion-area-secondary-before');snew=obs('companion-area-secondary-after');life=obs('companion-area-lifecycle-after');pub=obs('companion-area-publication-after')
assert old['cases']==sold['cases']==16 and old['old_scene_unresolved_targets']==sold['old_scene_unresolved_targets']==8
assert new['cases']==snew['cases']==16 and life['cases']==64 and life['actual_waiting_casts']==48 and pub['cases']==32
retired_before=obs('companion-area-secondary-retirement-before');retired_after=obs('companion-area-secondary-retirement-after')
assert retired_before['cases']==retired_after['cases']==16 and retired_before['actual_waiting_casts']==retired_after['actual_waiting_casts']==16 and retired_before['old_scene_unresolved_targets']==8
for o in [new,snew,life,pub,retired_after]:assert o['new_scene_ghost_hits']==o['old_scene_unresolved_targets']==0
for name in ['companion-area-scene-after','companion-area-secondary-after','companion-area-lifecycle-after','companion-area-publication-after','companion-area-secondary-retirement-after']:
 text=Path(manifests[name]['log']['path']).read_text('utf-8');assert 'Vital completion failed' not in text and 'role publication capture requires actual mutation lock' not in text
count=sum(o['cases'] for o in [new,snew,life,pub,retired_after])+reg['cases']+gates['cases'];assert count==1276
known=sum(o['checks'] for o in [new,snew,life,pub,retired_after])+reg['checks']+gates['known_checks']
run=D/'companion-area-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[0]='Finish all remaining material/class/target/Ctrl/area/effect coverage and all Source control consumers. Separate 27/29/152/153/157/145/61/75/66/76 actor-scene scopes do not prove completion of the 34 material skills or full Source.';remaining[1]='Finish broader area primary/secondary selectors, class/eligibility/effect/range matrices, pre-dispatch versus caster-lock races, and other consumers including 161/167. Sleep66 and Slow76 have scoped actor-scene selection, captured owner waits and post-commit publication evidence; do not infer full Source or every area family.';remaining[3]=remaining[3].replace('pending death/status callbacks','pending casts, death/status callbacks and post-commit publication')
v=dict(status='passed-scoped-area-66-76-primary-secondary-owner-wait-and-publication-coverage-open',integrated_runtime=integrated['runtime'],runtime=runtime,parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(primary_before=old,primary_after=new,secondary_before=sold,secondary_after=snew,paired_lifecycle=life,post_commit_publication=pub,secondary_retirement_before=retired_before,secondary_retirement_after=retired_after),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v)
text='\n\n範圍睡霧66／集體緩速76續作 `'+runtime['sha256'][:12]+'…` 同SHA **1,276案／已計數'+format(known,',')+'項檢查**。實際失敗主人傳送後寵物留原圖：父版主目標16案與次目標16案各重現8個漏找；新主／次各16案的原圖命中與新圖隔離皆通過，正常與Ctrl、兩種app情境一致，MP／材料恰一次。次目標案必須同時真正影響有效玩家主目標，保留Source單一主目標施法特效，緩速次目標另驗原圖S_SPEED且遠端主人收不到。\n\n64案生命週期包含48次真正主人Source鎖等待：角色、生命token、位置、原場景同ID替換、缺材料／MP，以及STOPPED前已持有收據者完成、新封包不扣費不送包。另32案於效果提交後、送包前變更生命／位置／同ID／主人角色，過期特效與速度封包皆撤銷；已接受扣費仍恰一次，結束無Context／工作／角色收據殘留，無鎖內IO。\n\n只在WORLD的66／76沿用既有成對Scope與主人收據，在等待前固定同一個Context，再執行原有全玩家鎖／Hub／population路徑；不在等待後重抓新生命。私有patience／phantasm目標查詢另作限定副本，只擴充寵物迭代與物件場景判斷；原查詢、真玩家與BATTLE分支保留。施法本體期間才開啟原範圍輸出捕捉，送包前還原外層階段，修正延後送包再次捕捉而缺實際鎖的錯誤。僅改PC與NativeAreaMaterialAction兩個class、新增一個無狀態helper；不新增玩家／Context欄位、ThreadLocal、工作池、時鐘或全域名冊。凍結輸入可重建同SHA。\n\n混合目標另16案含16次真正等待：前候選重現8次次目標主人退役竟取消有效玩家主目標。現在只排除失效寵物，保留玩家主目標與一次扣費；另8案STOPPED仍完成已持有權限的玩家主目標及原場景寵物次目標。不新增名冊或玩家欄位，不沿用新角色／新物件。800案原範圍玩家／普通NPC／寵物／競技／生命週期本體不改寫；兩組材料共288案封包與原預期逐位元相等。另332案成對／範圍／Main68／73關閉回歸通過。成對測試來源原檔雜湊直接指向未改寫的原始檔，避免先前舊工具的路徑別名問題。不同SHA證據不相加，整合檢查點仍3bda9d5d的1,206案；其餘材料、完整Source外控／其他範圍與最終全回歸維持開放。競技、記憶體、資料限制保留；131停用、超容量馴服整筆拒絕。正式檔與固定基準不變，未部署，未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-area-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| 復活跨角色續作 |'));lines.insert(at+1,'| 範圍主次目標續作 | 66／76 **`'+runtime['sha256'][:12]+'…`** 同SHA **1,276案**；主／次各8個漏找歸0。64案含48次主人鎖等待與STOPPED，32案提交後生命／位置／同ID／角色送包重驗；16案補混合目標主人退役／STOPPED。800案原範圍及332案成對／範圍／Main68／73通過；其餘材料與完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-area-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],integrated_runtime=integrated['runtime'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-area-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_area=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-1276-area-66-76-actor-scene-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_area=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionArea.java','CompanionAreaMethods.java','CompanionAreaDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous_continuation);following['companion_area_progress']=pin(D/'companion-area-progress-current.json');write(continuation,following)
assert sha(R/'current.json')==integrated_hash
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_area_progress'}==previous_continuation
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
