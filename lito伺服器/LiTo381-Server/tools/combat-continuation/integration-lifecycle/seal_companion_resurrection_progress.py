"""Seal one Resurrection increment; retain the integrated checkpoint and unfinished full Source goal."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, shutil

R=Path(__file__).resolve().parent; S=R.parents[2]; D=R/'build'; continuation=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path' in v and 'sha256' in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)

integrated=read(R/'current.json'); ih=sha(R/'current.json'); previous_continuation=read(continuation)
proof=read(D/'Source-lifecycle-progress-verified-current.json'); audit=read(D/'source-summon-lifetime-audit-current.json')
assert integrated['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df'
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
names=['companion-resurrection-scene-build','companion-resurrection-scene-reproduction','companion-resurrection-scene-before','companion-resurrection-scene-after','companion-resurrection-scene-after-stopped','companion-resurrection-publication-before','companion-resurrection-publication-after','resurrection-NPC-capture-before','resurrection-NPC-capture-after','companion-resurrection-nested-before','companion-resurrection-nested-after','companion-resurrection-regressions','companion-resurrection-gates-regressions']
names.append('companion-resurrection-pair-provenance')
indices={}; manifests={}
for name in names:
 index=read(D/(name+'-current.json')); assert index['status'].startswith(('built','passed','observed')),(name,index['status']);verify(index)
 m=read(Path(index['manifest']['path']));verify(m);indices[name]=index;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]]
assert runtime['sha256']=='63f38c850e280837d4deead387117de82f16cbb1c6a6a241ab2c659d4719e8db'
for name in names:
 if not name.endswith('-before'):assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert len(build['changed'])==len(build['fresh'])==3
for row in manifests['companion-resurrection-gates-regressions']['results']:
 if 'manifest' in row:
  if Path(row['original']['path']).name=='test_pair.py':
   correction=manifests['companion-resurrection-pair-provenance'];assert correction['historical_pair_manifest']==row['manifest'] and correction['cases_added']==0
   verify(read(Path(correction['normalized_pair_manifest']['path'])))
  else:verify(read(Path(row['manifest']['path'])))
obs=lambda n:manifests[n]['observations']
old=obs('companion-resurrection-scene-before');new=obs('companion-resurrection-scene-after');stop=obs('companion-resurrection-scene-after-stopped')
pold=obs('companion-resurrection-publication-before');pnew=obs('companion-resurrection-publication-after')
nold=obs('resurrection-NPC-capture-before');nnew=obs('resurrection-NPC-capture-after')
xold=obs('companion-resurrection-nested-before');xnew=obs('companion-resurrection-nested-after')
reg=manifests['companion-resurrection-regressions'];gates=manifests['companion-resurrection-gates-regressions']
assert old['cases']==16 and old['old_scene_unresolved_targets']==8
assert new['cases']==64 and new['actual_waiting_casts']==32
assert stop['cases']==stop['actual_waiting_casts']==8
for o in [new,stop,xnew]:assert o['old_scene_unresolved_targets']==o['new_scene_ghost_hits']==0
assert pold['cases']==pnew['cases']==24 and pold['stale_resurrection_packets']==24 and pnew['stale_resurrection_packets']==0
assert nold['cases']==nnew['cases']==64 and nold['stale_corpse_admissions']==32 and nold['stale_resurrection_packets']==24
assert nnew['stale_corpse_admissions']==nnew['stale_resurrection_packets']==0
assert xold['cases']==xnew['cases']==16 and xold['actual_waiting_casts']==xnew['actual_waiting_casts']==8
assert reg['cases']==700 and gates['cases']==332
count=sum(o['cases'] for o in [new,stop,pnew,nnew,xnew])+reg['cases']+gates['cases'];assert count==1208
known=sum(o['checks'] for o in [new,stop,pnew,nnew,xnew])+reg['checks']+gates['known_checks']
run=D/'companion-resurrection-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
next_audit=read(D/'companion-area-selector-audit-current.json');verify(next_audit);assert next_audit['runtime']==runtime
verify(read(Path(next_audit['manifest']['path'])))
target=run/'companion-area-selector-audit-current.json';shutil.copyfile(D/target.name,target);refs['next_area_selector_audit']=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining'])
remaining[0]='Finish remaining material/class/target/Ctrl/area/effect coverage and all Source control consumers. Separate 27/29/152/153/157/145/61/75 actor-scene scopes do not prove completion of the 34 material skills or full Source.'
remaining[1]='Complete remaining area primary/secondary selectors and broader resurrection eligibility/class/scene/perception coverage. PET and ordinary NPC corpse scopes now capture before pair waits and guard post-revival publication, including foreign nested contexts; Source player-answer and BATTLE semantics remain unchanged.'
v=dict(status='passed-scoped-Resurrection-actor-scene-NPC-PET-nested-and-post-revival-lifecycle-coverage-open',integrated_runtime=integrated['runtime'],runtime=runtime,parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(before=old,after=new,STOPPED=stop,publication_before=pold,publication_after=pnew,NPC_before=nold,NPC_after=nnew,nested_before=xold,nested_after=xnew),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v)
text='\n\n復活61／75跨角色續作 `'+runtime['sha256'][:12]+'…` 同SHA **1,208案／已計數'+format(known,',')+'項檢查**。父版16案重現8個原場景屍體漏找；64案正常／Ctrl、兩技能與兩種app情境包含32次真正主人鎖等待、角色／死亡再復活／位置／同ID替換、缺材料與MP，漏找及誤命中均0；另8案STOPPED只完成已取得收據的施法。保留Source復活HP、原背包與主人狀態、玩家同意回覆及競技規則。\n\n普通NPC另64案：父版重現32次過期屍體入場及24次過期復活送包；捕捉實際actor／population／epoch／位置，在等待後及復活後送包前重驗，皆歸0。PET復活後另24案，死亡、同ID新物件或位置變更均撤銷過期效果／復活包；父版24次過期送包歸0，已提交材料與MP仍恰一次。巢狀施法16案含8次真鎖等待，先重現外層其他技能Context繞過屍體捕捉；以每次施法Context專用boolean辨識自己的範圍，復活建立獨立捕捉且完整還原外層狀態。\n\n700案既有玩家／NPC／PET／回覆競態／競技／材料故障／生命週期通過，材料256案原封包逐一相等；另332案成對鎖、範圍及Main68／73關閉通過。生命週期凍結副本沿用現行收據／整批取消契約，STOPPED等待點限真正成對鎖；原始測試不改寫。可重建同SHA，僅改PC、既有施法Context及復活入口三個class，新增helper與兩個每次施法捕捉class；唯一新增欄位在施法Context，無玩家欄位、ThreadLocal、工作池、時鐘、全域名冊或資料寫入機制。\n\n不同SHA案例不相加，整合檢查點仍3bda9d5d的1,206案。其餘材料技能、完整Source外控／範圍次目標／AI與最終全回歸持續開放；競技與記憶體限制保留，131停用、超容量馴服整筆拒絕。正式檔與固定基準雜湊不變；未部署，未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-resurrection-progress-current.json)。\n'
text+='\n成對鎖舊工具將改寫前來源雜湊誤指向改寫後同一路徑；另存原檔與正規化收據，核對唯一差異為過時runtime-origin class名稱。原收據、已編譯class與所有觀察結果完整保留，其他引用逐一雜湊核對；此證據路徑更正新增案例數為0。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8')
lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| 回歸自然跨角色續作 |'))
lines.insert(at+1,'| 復活跨角色續作 | 61／75 **`'+runtime['sha256'][:12]+'…`** 同SHA **1,208案**；原場景PET／NPC屍體、正常／Ctrl、成對等待及復活後生命／位置／同ID送包重驗。外層其他技能Context不再繞過捕捉，結束完整還原；700案既有行為與332案成對／範圍／Main68／73通過。完整Source、其餘技能及最終全回歸仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-resurrection-progress-current.json)。 |')
doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],integrated_runtime=integrated['runtime'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-resurrection-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_resurrection=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-1208-Resurrection-actor-scene-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_resurrection=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionResurrection.java','CompanionResurrectionMethods.java','CompanionResurrectionDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
continuing=dict(previous_continuation);continuing['companion_resurrection_progress']=pin(D/'companion-resurrection-progress-current.json');write(continuation,continuing)
assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_resurrection_progress'}==previous_continuation
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
