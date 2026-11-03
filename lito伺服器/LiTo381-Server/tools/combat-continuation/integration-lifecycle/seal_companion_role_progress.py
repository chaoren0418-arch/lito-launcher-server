"""Seal bounded WORLD role identity and keep the broader Source objective and integration checkpoint open."""
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
proof=read(D/'Source-lifecycle-progress-verified-current.json');audit=read(D/'source-summon-lifetime-audit-current.json');integrated=read(R/'current.json');ih=sha(R/'current.json');previous_continuation=read(continuation)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert integrated['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df'
names=['companion-portal-identity-build','companion-portal-identity-reproduction','companion-portal-publication-before','companion-portal-publication-after','companion-portal-pending-before','companion-portal-pending-after','companion-role-invariants-after','companion-role-regressions','companion-ref-consumers'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed','located')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]]
assert runtime['sha256']=='104716cdf0ad8a06ad6628cff2476134556d188366357096f9e8a9560aed0250'
for name in names:
 if not name.endswith('-before'):assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert len(build['changed'])==7 and len(build['fresh'])==3 and build['new_player_fields_workers_schedulers_ThreadLocals_global_registries']==0
old=manifests['companion-portal-publication-before']['observations'];new=manifests['companion-portal-publication-after']['observations'];pold=manifests['companion-portal-pending-before']['observations'];pnew=manifests['companion-portal-pending-after']['observations'];invariants=manifests['companion-role-invariants-after']['observations'];reg=manifests['companion-role-regressions']
assert old['cases']==new['cases']==40 and old['valid_portal_publications_lost']==8 and new['valid_portal_publications_lost']==0
assert pold['cases']==pnew['cases']==48 and pold['valid_portal_casts_rejected']==16 and pnew['valid_portal_casts_rejected']==0 and pold['actual_waiting_casts']==pnew['actual_waiting_casts']==48
assert invariants['cases']==12 and invariants['role_invariant_failures']==0 and reg['cases']==3340
for row in reg['results']:
 m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
 if 'gates-regressions' in row['manifest']['path'] or 'stage-regressions' in row['manifest']['path']:
  for child in m['results']:verify(read(Path(child['manifest']['path'])))
for name in ['companion-portal-publication-after','companion-portal-pending-after']:
 text=Path(manifests[name]['log']['path']).read_text('utf-8');assert 'Vital completion failed' not in text and 'role publication capture requires actual mutation lock' not in text
count=new['cases']+pnew['cases']+invariants['cases']+reg['cases'];assert count==3440
known=new['checks']+pnew['checks']+invariants['checks']+reg['known_checks']
run=D/'companion-role-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[3]='Finish exact Source landing/terrain/door projection, real cross-instance showId failure semantics and remaining concurrent portal consumers. This bounded-role increment qualifies common WORLD Context/Scope and placement continuity; specialized resurrection/Nature/ground owner stamps, common Ref AI/hate/poison/counter/feedback and every other family still need explicit wiring and behavior evidence. Keep player/BATTLE action fences strict and reject genuine role/ABA/life/pose/replacement changes.'
v=dict(status='passed-scoped-bounded-WORLD-role-and-action-identity-coverage-open',integrated_runtime=integrated['runtime'],runtime=runtime,parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(publication_before=old,publication_after=new,pending_before=pold,pending_after=pnew,role_invariants=invariants),remaining=remaining,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\nWORLD角色／場景動作識別續作 `'+runtime['sha256'][:12]+'…` 同SHA **3,440案／已計數'+format(known,',')+'項檢查**。共用動作token原先同時代表換圖與角色退役，40案真實獨立主人C_GOTO_PORTAL重現8次已提交原場景寵物效果被誤取消；新40案歸0，真正角色退役、生命、位置或同ID替換仍不送過期特效／速度。另48案在實際入鎖呼叫前以測試鎖注入排程停頓，完成真實主人傳送再續原封包：16次正常／STOPPED誤拒歸0，材料／MP只扣一次、無鎖內IO或殘留。此48案是可控制的入鎖前排程屏障，不混稱為另有鎖持有人造成的阻塞。\n\n沿用原sourcePlayerOwner欄位，以每次動作Epoch指向單一Role。WORLD內場景寫入仍更新動作版本、仍執行原排隊取消與退休fence，只保留角色marker；重新選取同名角色、退役、離開WORLD／競技邊界及未知外部owner皆不延續舊角色。12案檢查4,096次WORLD場景更新與ABA／邊界／外部owner，舊玩家Binding仍嚴格失效；Epoch只有Role指標、Role只有volatile布林，不連前一代、玩家或Hub。沿用既有來源狀態slot並改為volatile，角色檢查可讀既有volatile owner，避免在Hub下取得主人status鎖。\n\n共用WORLD物件捕捉與成對Scope改用角色、原Actor／生命／場景／位置，三個攻擊根共用同一份等待前Context；寵物State.role與傳送Transition.character同步辨識角色，禁止同名重選ABA後重新綁定舊物件。真玩家Binding、NativePairedCastSafety及NativePlayerLifetime的class逐位元不變；不全域放寬玩家換圖fence。僅改七個既有class、新增三個helper／bounded marker class，沒有新增玩家或既有class欄位、工作池、時鐘、ThreadLocal、全域名冊、SQL／檔案／玩家庫存依賴；凍結重建同SHA。\n\n舊測試凍結副本只修正新入鎖入口的堆疊名稱，並在提交時另存Scope以核對關閉／收據清空／弱參照釋放，同時要求Context指標已清空；原始Java與所有行為／封包／扣費／STOPPED斷言保留。另3,340案原技能本體／既有適配斷言回歸包含範圍、單體／線／區域攻擊、復活回覆競態、負向、地縛、回歸自然、故障、背包交回／worker／死亡與到期，以及成對／範圍／Main68／73。不同SHA不相加，整合檢查點仍3bda9d5d的1,206案。唯讀Ref普查定位'+str(len(manifests['companion-ref-consumers']['consumer_classes']))+'個class、'+str(manifests['companion-ref-consumers']['consumer_ports'])+'個方法／建構／欄位入口；此為後續AI／毒傷等入口定位，非新增行為案例。特化復活／刪除等主人stamp、完整Source／外控／其餘材料與最終全回歸仍開放；131停用、超容量馴服整筆拒絕，競技／記憶體／資料限制保留。正式檔與固定基準不變，未部署，未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-role-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| 範圍主次目標續作 |'));lines.insert(at+1,'| WORLD角色生命續作 | **`'+runtime['sha256'][:12]+'…`** 同SHA **3,440案**；真傳送後8次已提交效果取消、入鎖前16次誤拒皆歸0。bounded角色marker保留正常換圖，玩家動作／退役／ABA仍嚴格失效，無新增玩家欄位／工作池／名冊。3,340案既有技能／stage／Main回歸通過；特化stamp、Ref及完整Source仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-role-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],integrated_runtime=integrated['runtime'],runtime=runtime,manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-role-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_role_identity=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-3440-WORLD-role-identity-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_role_identity=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionRoleIdentity.java','CompanionRoleIdentityMethods.java','CompanionRoleIdentityDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous_continuation);following['companion_role_progress']=pin(D/'companion-role-progress-current.json');write(continuation,following)
assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_role_progress'}==previous_continuation
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
