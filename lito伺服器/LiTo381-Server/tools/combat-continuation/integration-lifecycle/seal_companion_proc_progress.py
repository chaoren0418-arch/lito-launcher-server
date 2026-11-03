"""Seal one actual Proc increment; preserve integrated checkpoint and complete Source objective."""
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
names=['companion-proc-scene-build','companion-proc-scene-reproduction','companion-proc-scene-before','companion-proc-scene-after','companion-proc-lifecycle-before','companion-proc-lifecycle-after','companion-proc-publication-after','companion-proc-legacy','companion-proc-gates','companion-proc-regressions','companion-proc-origin','companion-proc-consumers'];indices={};manifests={}
for name in names:
 i=read(D/(name+'-current.json'));assert i['status'].startswith(('built','passed','observed','located')),(name,i['status']);verify(i);m=read(Path(i['manifest']['path']));verify(m);indices[name]=i;manifests[name]=m
runtime=indices[names[0]]['runtime'];build=manifests[names[0]];assert runtime['sha256']=='799c2001838adbafd8488f7f9acf5fbd7b204f2deb473dcbf3720047dedd6b74'
for name in names:
 if not name.endswith('-before'):assert indices[name]['runtime']==runtime,name
for ref in build['inputs']:assert sha(R/Path(ref['path']).name)==ref['sha256']
assert build['changed']==['local/lito381/server/NativeProcHealingCompanion.class'] and build['fresh']==['local/lito381/server/NativeCompanionProcScene.class'] and build['new_player_fields_workers_schedulers_ThreadLocals_global_registries']==0
old=manifests['companion-proc-scene-before']['observations'];new=manifests['companion-proc-scene-after']['observations'];lold=manifests['companion-proc-lifecycle-before']['observations'];life=manifests['companion-proc-lifecycle-after']['observations'];pub=manifests['companion-proc-publication-after']['observations'];reg=manifests['companion-proc-regressions'];legacy=manifests['companion-proc-legacy'];gates=manifests['companion-proc-gates']
assert old['cases']==new['cases']==6 and old['valid_recoveries_missing']==old['master_scene_ghost_recoveries']==2 and new['valid_recoveries_missing']==new['master_scene_ghost_recoveries']==0 and new['accepted_queues']==4
assert lold['cases']==life['cases']==22 and lold['valid_recoveries_missing']==4 and life['valid_recoveries_missing']==0 and life['accepted_queues']==22 and life['committed_recoveries']==6 and life['owned_STOPPED_claims']==2
assert pub['cases']==pub['controlled_prepublication_barriers']==18 and pub['valid_meters']==8 and pub['cancelled_stale_meters']==10 and legacy['cases']==887 and gates['cases']==352 and reg['cases']==1245
for parent in [reg,gates]:
 for row in parent['results']:
  m=read(Path(row['manifest']['path']));verify(m);assert m['runtime']==runtime
  if 'gates-regressions' in row['manifest']['path']:
   for child in m['results']:verify(read(Path(child['manifest']['path'])))
for name in ['companion-proc-scene-after','companion-proc-lifecycle-after','companion-proc-publication-after']:
 log=Path(manifests[name]['log']['path']).read_text('utf-8');assert 'publication/commit failed' not in log and 'role publication capture requires actual mutation lock' not in log
count=reg['cases']+life['cases']+pub['cases'];known=reg['known_checks']+life['checks']+pub['checks'];assert count==1285
run=D/'companion-proc-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);refs={}
for name in names:
 target=run/(name+'-current.json');shutil.copyfile(D/target.name,target);refs[name]=pin(target)
doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';doc2=S/'docs/maintenance/combat-continuation-current.zh-TW.md'
for source,name in [(Path(__file__),Path(__file__).name),(D/'Source-lifecycle-progress-verified-current.json','previous-proof.json'),(D/'source-summon-lifetime-audit-current.json','previous-audit.json'),(continuation,'previous-continuation.json'),(doc,'previous-lifecycle.md'),(doc2,'previous-continuation.md')]:shutil.copyfile(source,run/name)
remaining=list(audit['companion_placement_remaining']);remaining[3]='Finish exact Source landing/terrain/door projection and real cross-instance showId failure semantics. Common WORLD Ref and specialized companion Proc-healing now have separate scoped evidence for actor scene, actual caster/master paired locks, custody, role/life/membership, accepted drain/STOPPED and private owner meters. Full AI/hate/poison, debit/damage mutation/publication races, all HP-writer/publication ordering, non-healing Proc helpers, specialized corpse/ground stamps and all remaining factory/material/control matrices remain open; do not globally relax player/BATTLE actions.'
v=dict(status='passed-scoped-WORLD-companion-Proc-paired-lifecycle-and-owner-meter-coverage-open',runtime=runtime,integrated_runtime=integrated['runtime'],parent=build['base'],receipts=refs,qualified_scope=dict(cases=count,known_checks=known,check_count_complete=False),observations=dict(scene_before=old,scene_after=new,lifecycle_before=lold,lifecycle_after=life,publication=pub),remaining=remaining,all_HP_writer_and_publication_races_verified=False,full_ProcSkillEffectHelper_verified=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,scene_consumer_rewrite_complete=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v)
text='\n\nWORLD隨從Proc治療續作 `'+runtime['sha256'][:12]+'…` 同SHA **1,285案／已計數'+format(known,',')+'項檢查**。6案真傳送配置重現2次原場景有效治療漏接與2次依主人場景誤治療遠端物件，新6案皆歸0；22案先以原生NPC FIFO接受治療，再完成真實主人傳送／角色、生命、物件、場景、名冊及關閉變動，4次正常傳送／DRAINING的有效治療取消歸0。兩個已取得原生claim並真正阻塞在actor監視器的STOPPED案例仍完成原HP一次；排程屏障與實際監視器等待分別記錄，不混稱為額外施法鎖等待。\n\nWORLD特化withTarget在取得施法者／主人的實際Source成對動作鎖後才取Hub、原custody容器、actor及NPC狀態鎖。施法者保持完整動作世代與物理場景／名冊檢查；主人沿用bounded角色識別並核對原物件、生命、原場景與Hub／population。玩家狀態監視器不再於Hub內取得；舊887案的凍結副本僅把WORLD換角寫入的結構觀察改成確實等待同一原生工作者的Source動作鎖，背包容器與全部BATTLE監視器觀察，以及原HP、封包、材料／資源、來源向量、名冊、容量、死亡與關閉斷言保留。原始Java未覆寫。\n\n另18案在已提交HP、尚未取得最後主人送包鎖時控制原工作者：8次有效私有HP表送出，10次角色／生命／同ID物件／名冊／背包替換過期封包取消，HP不回滾或重放。已提交HP的私有主人表不因施法者退役或同一活物件正常換場景而取消；施法准入仍必須符合物件場景。最後送包捕捉Source角色Lease並再次解析原生命／custody；延後IO閉包只持弱主人／弱Hub及Ref、封包等值證據，無鎖內IO。測試夾具對STOPPED預期中斷及RuntimeInventory替換型別的失敗已保留，通過收據不含失敗場次。\n\n887案原Proc真實AI／accepted Frame／16個治療技能／全部狀態組合／來源HP向量／owner-only meter／1024共同容量與drain/drop通過；另352案含20案共用Ref／真實反饋及332案成對／範圍／Main68／73通過，全部為本候選SHA。3組新原生fixture classpath各29個遊戲class確認來自此JAR，唯讀載入不另計行為案例。只改NativeProcHealingCompanion、新增無欄位helper；原BATTLE withTarget本體、其餘原方法除指定publish橋接外、全部共用玩家／Ref／成對／生命週期／治療Kernel與私有receiver class均保留，凍結重建相同SHA。沒有新增玩家欄位、工作池、時鐘、ThreadLocal、全域名冊或SQL／檔案／玩家庫存依賴。\n\n完整HP寫入／送包排序、非治療Proc、AI／仇恨／毒傷、其餘NPC factory與全部材料／職業／目標／Ctrl／範圍／外控及最終同SHA全回歸仍開放。本輪不混加前輪718abe6b的3,460案；整合檢查點仍3bda9d5d的1,206案，131停用、超容量馴服整筆拒絕、競技與記憶體／資料限制保留。正式檔與固定基準不變，未部署、未寫Source／玩家SQL／WAL。[限定續作收據](../../tools/combat-continuation/integration-lifecycle/build/companion-proc-progress-current.json)。\n'
doc.write_text(doc.read_text('utf-8')+text,'utf-8');lines=doc2.read_text('utf-8').splitlines();at=next(i for i,line in enumerate(lines) if line.startswith('| WORLD共用Ref續作 |'));lines.insert(at+1,'| WORLD特化Proc續作 | **`'+runtime['sha256'][:12]+'…`** 同SHA **1,285案**；2次漏治療、2次主人場景誤治療及4次傳送／drain誤取消歸0。實際成對動作鎖＋原容器／物件鎖，22案佇列生命、18案提交後主人表重驗；887案原Proc及352案Ref／成對／Main回歸通過。完整Source、其餘材料與全HP送包排序仍開放。[收據](../../tools/combat-continuation/integration-lifecycle/build/companion-proc-progress-current.json)。 |');doc2.write_text('\n'.join(lines)+'\n','utf-8')
index=dict(status=v['status'],runtime=runtime,integrated_runtime=integrated['runtime'],manifest=pin(run/'manifest.json'),full_goal_achieved=False);write(D/'companion-proc-progress-current.json',index)
audit.update(previous=pin(run/'previous-audit.json'),pending_companion_proc_scene=pin(run/'manifest.json'),companion_placement_remaining=remaining);write(D/'source-summon-lifetime-audit-current.json',audit)
proof.update(status='passed-integrated-1206-unchanged-plus-separate-1285-WORLD-companion-Proc-scope',previous_progress=pin(run/'previous-proof.json'),pending_companion_proc_scene=pin(run/'manifest.json'),audit=pin(D/'source-summon-lifetime-audit-current.json'),current_audit=pin(D/'source-summon-lifetime-audit-current.json'),docs=[pin(doc),pin(doc2)],experimental_live_helpers=proof['experimental_live_helpers']+[pin(R/name) for name in ['NativeCompanionProcScene.java','CompanionProcSceneMethods.java','CompanionProcSceneDelta.java']]);write(D/'Source-lifecycle-progress-verified-current.json',proof)
following=dict(previous);following['companion_proc_progress']=pin(D/'companion-proc-progress-current.json');write(continuation,following);assert sha(R/'current.json')==ih
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
assert {k:v for k,v in read(continuation).items() if k!='companion_proc_progress'}==previous
print(json.dumps(dict(status=v['status'],cases=count,known_checks=known,integrated_checkpoint_promoted=False,full_goal_achieved=False)))
