"""One-time local queue conversion. No native/provider operations or dispatch."""
from pathlib import Path
from datetime import datetime, timezone, timedelta
import hashlib, json, subprocess, sys

S=Path(__file__).parent
INDEX=S.parent/'LITO-監製索引.json'
SKILL=Path('C:/Users/CHAOREN/.codex/skills/lito-supervisor')
W=Path('D:/lito-dev-runtime/20261003-supervision/launcher/observability-20261003-225704/ui-native-hookup-20261004-051000')
R=W.parent/'ui-v10-reentry-t76-prepared-20261005'
now=datetime.now(timezone(timedelta(hours=8))).isoformat()
def read(p):return json.loads(Path(p).read_text(encoding='utf-8-sig'))
def write(p,j):Path(p).write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def reference(p,level='existing_sealed_handoff_reused'):
    p=Path(p);b=p.read_bytes()
    return {'path':str(p),'sha256':hashlib.sha256(b).hexdigest().upper(),'evidence_level':level}

task_id='launcher-workflow-v2'
index=read(INDEX)
index_task=next(t for t in index['dispatch_queue']['items'] if t['id']==task_id)
assert index_task['owner']=='launcher'
if (S/'工作佇列.json').exists():
    raise RuntimeError('Existing queue: do not repeat one-time conversion or overwrite later states')
assessment_path=S/'Native-T73-Three-Failure-Remaining-Work.json'
assessment=read(assessment_path)
refs={k:reference(S/n) for k,n in {
    'assessment':'Native-T73-Three-Failure-Remaining-Work.json',
    'T76':'Native-T76-Reentry-Prepared-Handoff.json',
    'T77':'Native-T77-Skill-Source-Adoption-Handoff.json',
    'T78':'Native-T78-Channel-Back-Source-Handoff.json',
    'T79':'Native-T79-Next-Player-Back-Evidence-Handoff.json',
    'T80':'Native-T80-Server-Return-Adoption-Handoff.json',
}.items()}
t76=read(S/'Native-T76-Reentry-Prepared-Handoff.json')
t79=read(S/'Native-T79-Next-Player-Back-Evidence-Handoff.json')
candidate={'root':str(R),'manifest':read(W/'t76_reentry_prepared/deployment.json')['session_manifest'],
           'READY':read(W/'t76_reentry_prepared/deployment.json')['READY'],
           'claim_ready':read(W/'t76_reentry_prepared/deployment.json')['claim'],
           'T76_entry':str(W/'t76_reentry_prepared/Start-T76.ps1'),
           'T79_entry':t79['entry'],'guide':t79['player_guide'],
           'T79_deployment':t79['deployment'],'Back_callback_modified':False}
assert (R/'single-use-claim-ready.json').is_file()
assert not (R/'T71-launch.claim.json').exists() and not (R/'candidate-ui-status.json').exists()

BOUNDARY='僅本組目录與已授權唯一 runtime；保留 T76–T80、原版 BIN、共享 ddraw/TW/LHZ、未消耗 claim、provider/SQL 所有權。不得重啟已退出 T73、對它按 G、猜寫 phase/B8/class/list、合成封包或用新工具繞行原生 input/screenshot 限制。保留原生確認後返回／登出、BIN private-backing 停止及所有既有被拒工作邊界。'
def gate(gid,evidence=None,ephemeral=False,policy=None):
    g={'id':gid,'state':'missing','evidence':evidence}
    if ephemeral:
        g['expires_at']=now
        g['expiry_note']='缺失 gate 的失效佔位時間，不能用作 fresh-ready；接受時必須由 supplier 提供時區明確的有效期限。'
        g['acceptance_expiry_policy']=policy or '沿用原 lease／READY／instance 與真人可用時段，不採用新工作流的預設時限。'
    return g
def blocker(reason,supplier,missing,resume,consumer):
    return {'reason':reason,'supplier':supplier,'missing_input':missing,'resume_when':resume,'consumer':consumer}
def item(tid,title,priority,deps,gates,blockers,next_path,change,acceptance,evidence,stop,handoff=None):
    return {'id':tid,'owner':'launcher','title':title,'priority':priority,'state':'blocked',
        'next_deliverable':str(next_path)+'；'+change,'deliverable_path':str(next_path),
        'acceptance':acceptance,'dependencies':deps,'gates':gates,'blockers':blockers,
        'policy_blocked':False,'boundary':BOUNDARY,'resume_when':'; '.join(b['resume_when'] for b in blockers),
        'evidence':evidence,'handoff_to':handoff or ['supervisor'], 'stop_condition':stop,
        'product_acceptance':False}

Q=S/'工作佇列.json'
V=S/'工作流驗證.json'
H=S/'工作流交接.json'
capture_id='launcher-t76-human-session-evidence'
reentry_id='launcher-channel-reentry-evidence-review'
skills_id='launcher-first-lobby-skills-evidence-review'
back_id='launcher-channel-back-evidence-review'
capture_path=S/'工作流採證/同場採證交接.json'
items=[{
    'id':task_id,'owner':'launcher','title':'一次性 launcher v2 工作契約轉換與驗證','priority':0,'state':'review',
    'next_deliverable':str(Q)+'；三項產品缺陷與共用場次形成可機讀依賴／解阻／驗收契約；'+str(V)+' 為必要 queue validator 結果。',
    'deliverable_path':str(Q),'acceptance':'version=2／owner=launcher、owner-prefixed 唯一 ID、dependencies 只指本清單且無環、每項有 task owner/operator/candidate/input/deliverable/acceptance/gates/blocker supplier/resume_when/consumer/boundary/handoff/stop；supervise.py validate --queue exit0。清單轉換不等於三项 GUI PASS。',
    'dependencies':[], 'gates':[{'id':'user_workflow_redesign_authorized','state':'accepted','evidence':index_task['gates'][0]['evidence']}],
    'blockers':[],'policy_blocked':False,'boundary':BOUNDARY+'本次僅一次性轉換；不建立週期報告任務、不要求 supervisor 再派相同清單。',
    'resume_when':'監製審查此 queue 及 validator 結果；僅實際證據／依賴變更才重新檢視受影響工作。',
    'evidence':[refs['assessment'],*list(refs.values())[1:]],'handoff_to':['supervisor'],
    'stop_condition':'queue 驗證完成即交 review；沒有可執行產品工作時 ready_count=0、execution_state=waiting，列最小外部解阻後停止。',
    'operator':{'implementation':'launcher 本對話','review':'supervisor'},'candidate':'不改／重編任何產品候選','product_acceptance':False,
}]

capture_gates=[
    gate('current_human_ready',ephemeral=True,policy='真人明確提供本次原版操作時段及有效期限；一般繼續／工作流授權不能替代。'),
    gate('fresh_provider_READY_lease_exact_instance',ephemeral=True),
    gate('original_claim_cleanup_new_entry_instance',ephemeral=True),
    gate('permitted_same_session_observation_and_wire_join',policy='只沿用已允許操作／採證；有 tool rejection 時停原事項，不換工具、借程序或轉派繞行。'),
]
capture_blockers=[
    blocker('目前沒有當下真人可操作證據','human／supervisor','有效期內 human-ready 與玩家可完成的操作範圍','真人明確提供本次可操作時段；supplier 釘選 expires_at','launcher/'+capture_id),
    blocker('歷史 READY 不證明現時 provider／claim／instance 就緒','server／supervisor／launcher original preflight','fresh READY／lease／exact PID+creation-time/provider identity／cleanup、原 claim 條件','原本 provider／client-only preflight 與未消耗 claim／cleanup 全通過，身份與期限仍有效','launcher/'+capture_id),
    blocker('缺同場採證時窗與 packet/socket attribution','human／launcher existing T76-T79／server','exact new EntryRun、G binding、cycles/markers/result、same socket packet delta 与必要非敏感字段及真人結果','按原指南提供確切新 EntryRun，所有採證與人手操作限於同一 PID/FILETIME/UTC 時窗；server 提供必要 connection 歸屬','launcher/'+reentry_id+','+skills_id+','+back_id),
]
cap=item(capture_id,'一次新 T76 原版場次：共用三項缺陷的有限採證',10,[task_id],capture_gates,capture_blockers,capture_path,
    '列明 exact EntryRun、PID/FILETIME、candidate/READY/lease/claim identity、G UTC、原 markers/cycles/results/packetdelta/launch-report 與實際人手結果，逐欄標明 availability/rejection；三項共用此場，不為每項再開原版。',
    '證據採集完成標準是 identity／候選／run／UTC 可歸屬，依原 T76-T79 limits 保存停止與拒讀結果；不得把缺欄補零、latest/PID猜測或 opcode-only 當 payload。可交有明確 coverage 的部分結果，缺失 gates 留 blocked；不是遊戲驗收。',
    list(refs.values()),'限原 T76 startup300秒、original G/instance exit guard；T79 綁定等候預設300/max600秒、active120秒/max1200 samples。exit／身份/候選/lease變化、unsafe/rejected tool 或有效期結束立即停止該採證；不重開補齊、不重用 claim。',
    ['launcher','supervisor'])
cap['operator']={'native_login_clicks_and_GUI_result':'human only','T76_T79_original_console_start_G_markers':'human/authorized launcher operator under original guide','candidate_identity_and_numeric_collection':'existing pinned T76/T79 tooling, no new native tools','provider_and_connection_attribution':'server owner via supervisor; launcher不管理provider'}
cap['candidate']=candidate
cap['exact_inputs']={'reuse_existing_guide':candidate['guide'],'new_entry_run':'operator 提供單一 exact new T76 EntryRun；不得用 latest 選取','binding':'原 G 生成的 exact PID/FILETIME/instance binding，退出後不按G','packets':'same connection/PID/FILETIME window；方向、opcode、CA selection、C9 result/selection，技能S1E/S1F/DA非敏感payload；登入憑證仍遮罩','human_outcomes':'L/S/I、H/F/J、O/P/K 依原指南，保留缺失與實際可見提示'}
items.append(cap)

assessed={r['id']:r for r in assessment['remaining_product_work']}
def analysis_item(tid,key,title,priority,output,requirements,success,failure,refs_for_item,recipient):
    a=assessed[key]
    gates=[gate('accepted_exact_session_evidence_for_'+key)]
    blockers=[blocker('欠可歸屬且足夠決定此缺陷的同場觀測',
        'launcher/'+capture_id+'／human／server connection-evidence supplier',requirements,
        capture_id+' 證據任務 accepted 且此缺陷必要欄位 gate 經監製接受；不能以其他版本 component PASS 代替',
        'launcher/'+tid)]
    t=item(tid,title,priority,[task_id,capture_id],gates,blockers,S/'工作流採證'/output,
        '只消費上項 exact 場次輸入，交 PASS/FAIL/INSUFFICIENT、observed/inferred 分層、候選觸發/拒絕與 next consumer；只有發現可由來源決定的最小產品缺口才在本組形成具體 successor task/plan，做必要實作與新針對性驗證。不為未決因果重編候選。',
        'report 的每項判斷可追到 exact run/instance/file/UTC／原字段；缺任一必要觀測必須 INSUFFICIENT 並留具體 blocker，不能列產品 PASS。finite evidence triage 可審查 accepted，但缺陷只有满足 product_success_criteria 的真人結果才可關閉。',
        refs_for_item,'同一 evidence bundle 的一次有限判讀結束；若 missing/拒讀/時序不確定，記 supplier/missing_input/resume_when/consumer 即停。不重跑原場、不更換 tool 绕行、不補造 callback／GUI PASS。',recipient)
    t['operator']={'evidence_analysis':'launcher','GUI_outcome':'same-session human','final_review':'supervisor'}
    t['candidate']=candidate
    t['exact_inputs']={'assessment_input_requirements':a['minimum_missing_observations'],'specific_missing_input':requirements,'existing_data_cannot_decide':a['why_existing_data_cannot_decide'],'bundle':str(capture_path)}
    t['product_success_criteria']=success
    t['product_failure_criteria']=failure
    t['handoff_detail']='已知契約佐證只記 adopted，沒有新 wire difference 不另催 server；有經審查的實質新 delta 才由 supervisor 接棒原 owner，政策阻擋項不重派。'
    return t

items.append(analysis_item(reentry_id,'channel_reentry_fails','頻道再入：驗 T76 已整合 page40 rearm 與人手結果',20,'頻道再入判讀.json',
    '同新 EntryRun H/F/J 時窗的 phase/mode/page/62C21C/handlers；T76 activation history trigger+fresh_guard.status/snapshot 或 refusal；same-socket CA selection、server C9 result/selection、CA 與真人畫面。',
    '同一新候選場次，玩家由大廳回頻道後再選頻道確實回到可操作大廳；handler/root tuple 与同場接受回覆吻合。候選若觸發须有 activation history 的 page40 fresh_guard.written，不能只凭 flag0→1；若正常原生路徑已成功则如實記未觸發，不捏造修正歸因。',
    'J/no-lobby、回覆拒絕、guard refusal、仍 channel handlers 或只有 packet success 而 GUI 不成功，均非產品 PASS；缺payload／identity／GUI report 為 INSUFFICIENT。',
    [refs['assessment'],refs['T76'],refs['T78'],refs['T79'],refs['T80']],['supervisor']))
items.append(analysis_item(skills_id,'first_lobby_skills_blank','首次大廳技能：區分未發布／本地 reset／資料已填但畫面空白',30,'首次大廳技能判讀.json',
    '同 new PID/FILETIME L/S/I 時窗 typed skill owner/rows/count/table、class/order、pane-owner binding、metadata/icon roots/count；same socket S1E/S1F/DA有無及技能payload；同 source/physical 技能資格与真人可見結果。',
    '首次大廳 S 打開技能欄後，真人看見本次 source/physical 合格技能，typed rows/class/order/資源與pane綁定相符；需同 epoch 有可解釋數據來源及reset時序。Java SQL/session有技能或battle S1E不單獨構成PASS。',
    'I/空白、不匹配技能或同場typed rows仍placeholder為FAIL分支；缺received/reset/owner/GUI時窗則INSUFFICIENT。不能以test-only overlay或猜寫native list補成PASS。',
    [refs['assessment'],refs['T77'],refs['T79'],refs['T80']],['supervisor','server','bin_resources']))
items.append(analysis_item(back_id,'channel_Back_no_effect','頻道 Back：區分 input gates／phase-B8 回呼分支／返回封包',40,'頻道Back判讀.json',
    '一次實際paired down/up點位/viewport與O/P/K；typed slot4 bounds/enabled/pressed/rootactive/capture、75F6E5/overlay/down-suppression；phase/B8/sourceA/B/handlerroot以及same socket CB/CA100或0F/對應server事件。快照/marker不等于event trace。',
    '真人點實際Back後出現預期原生返回畫面且可操作；同場 control/phase/B8/packet條件一致。大廳離開仍保留原生確認後返回／登出，不以移除確認/直接回呼/合成packet取得PASS。',
    'K/畫面未返回、僅D0/0F而無可歸屬返回結果或phase不符不能PASS；paired事件/control瞬間狀態/回呼時序未捕捉應保留INSUFFICIENT，100ms快照不得宣称callback已執行。',
    [refs['assessment'],refs['T78'],refs['T79'],refs['T80']],['supervisor','bin_resources']))

queue={'version':2,'owner':'launcher','created_at':now,'updated_at':now,'workflow_task_id':task_id,
    'execution_state':'waiting','ready_count':0,'overall_product_completed':False,
    'one_time_conversion':True,'proposal_requires_supervisor_review':True,
    'selected_next_task_id':capture_id,'selection_reason':'已有再入修正候選，最高優先必要下一步是同場實機驗證；沒有尚未整合且可離線確定的產品修正或既有工具缺口。沒有為保持忙碌新增工作。',
    'minimal_external_unblock':['supervisor 審查並接受 launcher-workflow-v2 工作契約','真人提供本次有效原版操作時段，非一般工作流許可','server/supervisor 提供原約束下fresh READY/lease/exact provider instance，launcher原preflight確認cleanup/未消耗claim及exact新run','允許且可取得本次original numeric/packet/GUI觀測；限制保持且拒絕時不繞行'],
    'candidate_policy':'T76候選已整合，T77–T80主要來源/工具/判讀；不以清單完成或工具PASS宣稱遊戲修好。三個review共享一份exact場次，沒有要求三次launch。',
    'items':items}
write(Q,queue)
command=[sys.executable,'-B',str(SKILL/'scripts/supervise.py'),'validate','--queue',str(Q)]
result=subprocess.run(command,capture_output=True,text=True,encoding='utf-8',errors='replace')
verification={'command':command,'exit_code':result.returncode,'stdout':result.stdout.strip(),'stderr':result.stderr.strip(),'updated_at':now,
    'scope':'Required v2 queue validator only; no old tests,31-pin full audit,product rebuild,native/provider/claim operation.'}
write(V,verification)
if result.returncode:raise RuntimeError('Queue validator rejected queue: '+result.stdout+result.stderr)
handoff={'task_id':task_id,'state':'review','updated_at':now,'queue':reference(Q,'v2_proposed_task_contract'),
    'validation':reference(V,'required_queue_validation_exit0'),'conversion_completed':True,'execution_state':'waiting','ready_count':0,
    'next_handoff_task_id':capture_id,'minimal_external_unblock':queue['minimal_external_unblock'],
    'three_GUI_FAILs_open':True,'new_product_change':False,'new_offline_tool_change':False,'native_started':False,'claim_consumed':False,
    'skill_used':str(SKILL/'SKILL.md'),'review_consumer':'supervisor','no_automatic_dispatch':True,
    'note':'既有分析已列缺欄且T79補足已知工具欄位；没有新已證工具缺口可實作。工作流轉換提交review，產品任務blocked，不虛構ready。'}
write(H,handoff)
progress_path=S/'監製進度.json';p=read(progress_path)
p.update(workflow_queue_path=str(Q),workflow_task_id=task_id,workflow_task_state='review',workflow_handoff=reference(H,'workflow_delivery_for_supervisor_review'),updated_at=now)
p['workflow_execution_state']='waiting';p['workflow_ready_count']=0;p['workflow_next_task_id']=capture_id
write(progress_path,p)
text={'workflow_task_id':task_id,'state':'review','execution_state':'waiting','ready_count':0,'tasks':len(items),
      'validation_exit_code':result.returncode,'next_handoff_task_id':capture_id,'product_fixed':False,'claim_consumed':False,'native_started':False}
print(json.dumps(text,ensure_ascii=True))
