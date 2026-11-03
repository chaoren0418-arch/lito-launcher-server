"""One-time conversion of the existing twelve-category backlog to queue v2.

No old analysis generator, source matrix, extraction, or runtime is replayed.
"""
import sys
sys.dont_write_bytecode = True
from pathlib import Path
import json
import hashlib
import datetime

ROOT = Path(__file__).resolve().parent
INDEX = ROOT.parent/'LITO-監製索引.json'
TASK_ID = 'bin_resources-workflow-v2'
CONTRACT = Path(r'C:\Users\CHAOREN\.codex\skills\lito-supervisor\references\task-contract.md')


def stamp():
    return datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()


def load(path):
    return json.loads(Path(path).read_text('utf-8-sig'))


def pin(path):
    path=Path(path)
    raw=path.read_bytes()
    return {'path':str(path),'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest().upper()}


def write(name,value):
    path=ROOT/name
    assert path.resolve().is_relative_to(ROOT)
    path.parent.mkdir(exist_ok=True)
    temporary=path.with_name(path.name+'.tmp')
    temporary.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf-8')
    temporary.replace(path)


def main():
    assert not (ROOT/'工作佇列.json').exists(), 'One-time conversion: preserve an existing queue and its task states'
    index=load(INDEX)
    original_task=next(t for t in index['dispatch_queue']['items'] if t['id']==TASK_ID)
    assert original_task['owner']=='bin_resources' and not original_task['policy_blocked']
    assert any(g['state']=='accepted' and g['id']=='user_workflow_redesign_authorized' for g in original_task['gates'])
    backlog=load(ROOT/'original-delivery-backlog.json')
    assert len(backlog['remaining'])==12
    manifest=load(ROOT/'artifact-manifest.json')
    references={str(Path(p['path']).resolve()).casefold():p for p in manifest['entries']}

    def evidence(*names):
        result=[]
        for name in names:
            path=ROOT/name
            entry=references[str(path.resolve()).casefold()]
            result.append({**entry,'qualification':'Previously sealed artifact reference; body/matrix not re-audited during workflow conversion'})
        return result

    # Each task owns only a bounded receipt/qualified output under ROOT. The
    # supplier/consumer, not this group, owns PE edits, Modern edits or live GUI.
    definitions=[
        ('portable-pe-receipt',80,['iat-version-receipt','host-init-receipt'],
         ['pe-audit.json','baseline.json'],['launcher','supervisor'],
         'launcher／PE組經監製交付',
         '修復候選PE的絕對來源路徑、SHA及版本；完整import/external-target與initializer清單；符合原授權條件的可重現啟動驗收報告。',
         '交付已接受的候選/驗收artifact，且本清單IAT與host/init receipts均accepted；本組不修PE。',
         '修復候選與原BIN/OEP baseline分開pin；import/initializer引用可追蹤；提供實際啟動驗收級別或明確未驗收，不能僅parseable就寫portable。',
         '接收候選identity/acceptance envelope並做有限來源對照；輸出portable-pe-receipt.json，附候選pin/來源版本/匯入依賴與未知，無PE修改。'),
        ('iat-version-receipt',70,[],
         ['priority-api-resolution.json','pe-audit.json'],['launcher','supervisor'],
         'launcher／PE組或監製的同期DLL證據供應者',
         'Dump同期DLL檔案及SHA、module base/version、每一未解析thunk的VA/RVA與export/forwarder對應；注明未覆蓋項。',
         '收到已接受且版本可對應固定PE的DLL/target binding artifact；本機新DLL或base+RVA猜測不足。',
         '有限比對每一新提交target→module/export/forwarder與來源SHA；coverage明列，未提交target仍unknown；不聲稱恢復全部IAT。',
         '輸出iat-version-receipt.json：新提交的版本化target binding、覆蓋範圍、來源pin及剩餘unknown；不執行/載入DLL。'),
        ('host-init-receipt',60,[],
         ['host-alias-lifecycle.json','reconnect-source-index/host-alias-port-contract.json'],['launcher','supervisor'],
         'launcher證據組＋監製',
         '已接受050104CD前提/回傳契約，或已有同一期host/alias選擇、owner、初始化/clear順序記錄，附版本及來源SHA。',
         '至少一份已接受的上述獨立artifact到達；不得自行取得Native capture或開050104CD下一層。',
         'host64AD74、alias64AD54、port64AD70仍分離；記錄epoch/owner/前提與來源；source條件及live觀察分級，缺項不升格。',
         '輸出host-init-receipt.json：新初始化证據和已交付host/alias契約的對應、epoch及未知。'),
        ('skill-owner-receipt',50,[],
         ['S1E-initialization-gates.json','S1E-receive-selection-boundary.json','launcher-skill-initialization.json','skill-initialization/LTSpellDesc-c-indexed.json'],['launcher','modern','supervisor'],
         'launcher同一期初始化證據組；監製整合',
         '同版本同一期完整S1E payload、class/order/metadata/cache owner與reset/teardown順序；class4八個未闭producer槽的實際來源，均有來源SHA/epoch。',
         '收到已接受的同一期typed owner bundle，而非只有frame存在或另一epoch的metadata。',
         '八槽21/22/23/29/30/37/38/39逐項來源可追蹤；176 metadata slots的非空/NULL不誤當live owner；未閉项仍unknown。',
         '輸出skill-owner-receipt.json：收到的typed fields/epoch/owner、與現有metadata/order對照及逐槽未知；不重建缺失payload。'),
        ('native-parity-receipt',55,[],
         ['native-icon-draw-contract.json','skill-pane-render-initialization-contract.json','skill-pane-layer-composition-contract.json'],['launcher','modern','supervisor'],
         'launcher／Native驗收組＋Modern渲染組，經監製審查',
         '935邊界4F1593→7741AC20的同期模組/rounding契約；已接受實際initialized LUT、pitch、signed offset、alpha/blit及對照像素artifact。',
         '收到具有來源版本/epoch與renderer條件的accepted parity evidence；靜態色表及現有PNG PASS不足。',
         '對限定新artifact保留pixel/layout條件、誤差或不等值；935外部rounding與liveLUT是否閉合分開；不得由component PASS宣稱GUI通過。',
         '輸出native-parity-receipt.json：限定圖形契約/像素對照及不等值和仍缺renderer條件；不開Native或重跑舊像素矩陣。'),
        ('missing-lticon-member',25,[],
         ['resources/LTicon-decode-manifest.json','resources/archive-manifest.json'],['modern','supervisor'],
         '監製接受的缺ID來源供應者；Modern提出consumer需求',
         '只選173..179中一個必要ID：來源版本/namespace、確切index row/offset/length/既有qualified encoding、原始blob SHA；或監製接受的缺失/替代政策。',
         '一個缺ID的完整qualified member contract，或正式缺失政策已接受；其他ID不搭便車。',
         '原始bytes與限定source range/hash一致；既有decoder適用時輸出尺寸/pixel格式/PNG及SHA；政策分支不得生成假原圖，剩餘6個維持unknown。',
         '輸出missing-lticon-member.json及僅一個必要原始member/既有decoder輸出至workflow-v2/artifacts/missing-lticon/；若是政策則只交政策receipt。'),
        ('provider-member-delivery',20,[],
         ['Native-image-bank-boundary-contract.json','Native-filename-route-contract.json','Native-provider-writer-contract.json','Native-provider-writer-contract/terminal-provider-identity-gap.json'],['modern','supervisor'],
         '監製接受的獨立provider-to-file binding供應者',
         '一個PNG ordinal或IMG79/80 request的bank/filename/版本→provider→實際backing file絕對路徑/SHA→member row/offset/length/encoding→原始blob SHA與尺寸契約。',
         '獨立binding已接受且具體consumer需要該一個member；Sprite/Sprite03 matching stem不能滿足gate。',
         '有限讀取bound範圍、verify原始SHA/header；已qualified parser才解碼；mapping保持source條件，不假定class/sex或bank capacity。',
         '輸出provider-member-delivery.json及一個qualified原始blob至workflow-v2/artifacts/provider-member/；若已有blob只交差異化來源binding，不重抽。'),
        ('background-binding-receipt',15,[],
         ['Native-background-scene-contract.json','resources/modern-rgba/manifest.json','Native-selection-resource-gap/Native-animation-and-slot-data.json'],['modern','launcher','supervisor'],
         '監製接受的scene/owner證據供應者；Modern UI組',
         '數值owner/factory/caller到具名scene、495520結果到背景draw座標/transform/mode、同版本initialized owner/flag lifecycle的獨立accepted artifact；class/variant與selected+3D/timer如需採用另附契約。',
         '已接受具名scene＋背景draw契約與同版本owner/flag條件，且提出具體consumer；直接writer或S03 count0本身不足。',
         'RGBA D25BED57原件不改；新binding引用原來源/row/hash、精確draw/scene/owner與artifact；未供應的class/variant/lifecycle欄位維持unknown。',
         '輸出background-binding-receipt.json作Modern可消費的附加binding，引用兩個既有RGBA；無重產圖、writer重核或Modern code edit。'),
        ('map-runtime-binding-receipt',40,[],
         ['map-table-source-contract.json','map-candidate-index-contract.json','map-candidate-index/catalog.json'],['modern','supervisor'],
         'Modern map組＋監製接受的runtime key binding供應者',
         '一個實際runtime key/formatter/05040E1D與773F5200輸出到已有candidate來源/確切row的accepted版本化binding，附query與SHA。',
         '精確key/row契約到達；不得只傳離線candidate名字或重跑五個表。',
         '只query新指定row並引用既有parsed catalog SHA；明列條件及ambiguous/無命中；不把candidate升為已載入地圖。',
         '輸出map-runtime-binding-receipt.json：一個runtime key對應已解析row的source-qualified結果/未知，無private展開。'),
        ('modern-adoption-receipt',45,[],
         ['Modern-ready-directory-adoption-contract.json','Modern-character-ui-reachability-contract.json','Native-background-scene-contract.json'],['modern','supervisor'],
         'Modern組封存最新採用/真連線/GUI證據；監製驗收',
         '最新實際採用/合併清單、default入口、封存真連線結果及授權GUI驗收artifact；用新版本替換backlog中R527未驗收歷史，不能假定其仍是最新。',
         'Modern已封存且監製提供已接受採用/GUI evidence；本組只核對自己交接物的接收範圍。',
         '每個本組handoff注明adopted/pending/not-applicable及對應artifact/SHA；code presence、test PASS、GUI accepted分開；R526舊缺口不重開。',
         '輸出modern-adoption-receipt.json：本組交接→Modern採用版本/範圍→驗收級別及剩餘缺口；不讀全project或build。'),
        ('wire-delta-gate',35,[],
         ['server-packet-delta.json','server-adoption-ack.json','S1E-receive-selection-boundary.json'],['supervisor'],
         '合法既有BIN來源研究或監製提供的新typed wire證據',
         '相對已封存B4C00BF1真正新增opcode或更正wire的byte witness與typed schema；已知S1E補證/API identity/owner條件不算。',
         '獨立新/更正wire證據被識別；只有新artifact hash或known corroboration不喚醒本task。',
         '交接direction/opcode、typed widths/signedness/endian/encoding、trigger/state、VA/RVA、source SHA、證據等級與unknown；逐欄寫相對既有schema差異；由監製一次通知server，追notified/adopted/reviewed。',
         '輸出wire-delta-gate.json與新wire envelope（若真有差異才產生），保留既有server-packet-delta baseline，不自行發訊息/改Java_SQL。'),
        ('qualified-member-delivery',10,[],
         ['resources/archive-manifest.json','extract_member.py','resources/validation.json'],['modern','supervisor'],
         'Modern提出具體consumer缺口；監製接受來源contract',
         '一個實際必要且目前未交付的member：固定來源pin、namespace/archive/row/offset/length≤16MiB、已qualified encoding/parser、所需輸出格式、consumer及預期header/用途。',
         '監製接受完整member request，且確定不是已有594members/後续限定輸出的重複；existing decoder適用，不依賴private backing body。',
         '只讀新限定來源range並核對SHA/length/header；交出consumer需要的原始/decoded或RGBA與dimensions/stride/encoding provenance；未知格式即停；不掃全部305295 records。',
         '輸出qualified-member-delivery.json和僅一個新必要blob／已qualified decoder輸出至workflow-v2/artifacts/qualified-member/；具體檔名由accepted request固定，不任意抽樣。'),
    ]
    shared_boundary='只寫本組ROOT；不改PE/BIN/Modern/launcher/server，不開Native/GUI/process/capture，不重跑封存矩陣，不重新核64AFA0 writer；42E550/42E6D0/42EA50及其他private下一層仍停止。既有外部pyc清理拒絕保留，不檢查/重試/轉交；不接手受攔截server工作。'
    items=[]
    for row,definition in zip(backlog['remaining'],definitions):
        suffix,priority,dependencies,names,consumers,supplier,missing,resume,acceptance,observable=definition
        task_id='bin_resources-'+suffix
        output=ROOT/'workflow-v2'/'artifacts'/(suffix+'.json')
        gate_id=task_id+'-qualified-input'
        refs=evidence(*names)
        item={
            'id':task_id,'owner':'bin_resources','title':row['deliverable']+'：接收新證據後的限定交付',
            'priority':priority,'state':'blocked','dependencies':['bin_resources-'+x for x in dependencies],
            'gates':[{'id':gate_id,'state':'missing','evidence':None,'input_contract':missing,
                      'lifetime':'Qualified immutable accepted artifact; no human/provider-ready lease asserted'}],
            'blockers':[{'reason':'尚無足以執行這個限定task的新qualified輸入；既有封存成果不重做。',
                         'supplier':supplier,'missing_input':missing,'resume_when':resume,
                         'consumer':consumers,'gate_id':gate_id}],
            'next_deliverable':str(output)+'；'+observable,
            'exact_deliverable':{'path':str(output),'observable_change':observable,
                                 'allowed_output_root':str(ROOT/'workflow-v2'/'artifacts'),
                                 'input_cardinality':'one bounded new contract/request; not a full corpus or report-only recurring task'},
            'acceptance':acceptance+' 證據等級逐項標source-qualified/accepted saved-runtime/GUI-reviewed；本組不自行授產品accepted。',
            'boundary':row['boundary']+' '+shared_boundary,'resume_when':resume,
            'policy_blocked':False,
            'policy_interpretation':'This task only receives legitimate independent evidence and writes owned finite outputs; prohibited acquisition/body-expansion/blocked actions are excluded, not reauthorized by its gates.',
            'evidence':refs,'handoff_to':consumers,
            'stop_condition':'只交付一份新限定結果後轉review，由監製驗收；輸入不完整/來源版本不一致/未知parser/越界或需要被禁body時立刻保留blocked並列具體缺口，不逐層續查。',
            'backlog_category':row['deliverable'],'backlog_reference':pin(ROOT/'original-delivery-backlog.json'),
            'existing_delivery':row['already_delivered'],'conversation_completion_is_task_acceptance':False,
        }
        items.append(item)
    packet_pin=pin(ROOT/'server-packet-delta.json')
    assert packet_pin['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    packet=load(ROOT/'server-packet-delta.json')
    assert packet['summary']['new_opcodes']==0 and packet['summary']['corrected_wire_contracts']==0
    assert all(not x['new_opcode'] and not x['wire_contract_corrected_this_batch'] for x in packet['packets'])
    wire_review={'baseline':packet_pin,'relative_baseline_changed':False,'new_opcodes':0,'corrected_wire_contracts':0,
                 'known_contracts_corroborated':packet['summary']['known_contracts_corroborated'],
                 'analysis_corrections_are_wire_delta':False,
                 'adoption_ack':evidence('server-adoption-ack.json')[0],
                 'notification_requested':False,'decision':'Known corroboration only; do not renotify F3/F4 or S1E. A genuine typed new delta triggers bin_resources-wire-delta-gate; supervisor owns notification.',
                 'review_method':'Read immutable packet summary/per-packet correction flags and existing adoption metadata once; no old source/Java/SQL/test matrix replay'}
    queue={
        'version':2,'owner':'bin_resources','updated_at':stamp(),'workflow_task_id':TASK_ID,
        'proposal_state':'review','execution_state':'waiting','overall_product_complete':False,
        'source_backlog':pin(ROOT/'original-delivery-backlog.json'),
        'task_contract':pin(CONTRACT),'supervisor_index_at_read':pin(INDEX),
        'index_task_identity':{'id':TASK_ID,'owner':original_task['owner'],'state_at_read':original_task['state'],
                               'authority':'One-time conversion and one legitimate independent implementation if available; not private or runtime scope expansion'},
        'items':items,'ready_count':0,'blocked_count':12,'implementation_selected':None,
        'implementation_selection':{'examined':'The existing twelve categories and sealed deliverable references only; no broad re-audit',
                                    'result':'No ready independent implementation with all required qualified inputs',
                                    'reason':'All twelve remaining objectives have missing external source/owner/binding/parity/adoption/consumer evidence. Two RGBA buffers and direct-writer branch are already delivered; cannot invent another extraction or rerun them.',
                                    'product_improvement_in_this_conversion':False},
        'next_task_id_if_unblocked':'bin_resources-qualified-member-delivery',
        'minimum_external_unblock':{'supplier':'Modern consumer＋監製',
                                  'missing_input':'One accepted non-duplicate needed member request with immutable source pin, qualified namespace/row/offset/length≤16MiB/encoding/parser, output format and consumer.',
                                  'resume_when':'This immutable request is accepted and it does not require stopped private backing or blocked actions.',
                                  'consumer':['bin_resources','modern'],
                                  'task_id':'bin_resources-qualified-member-delivery'},
        'wire_review':wire_review,
        'retained_boundaries':{'private_backing':['42E550','42E6D0','42EA50'],
                               'background_direct_writer':'Already sealed DDD86F5D; never redo under this conversion',
                               'scope_write_incident':evidence('CD1-mode-step/scope-write-correction.json')[0],
                               'cleanup_or_server_blocked_work_taken_over':False,
                               'current_human_ready_or_provider_ready_asserted':False},
        'update_rule':'One-time proposal; only change affected task/gate after a real new artifact, accepted handoff, or dependency change. No periodic rewrite/old PASS replay.',
        'validation_required':'supervise.py validate --queue '+str(ROOT/'工作佇列.json'),
    }
    write('工作佇列.json',queue)
    write('workflow-v2/wire-semantic-review.json',wire_review)
    print(json.dumps({'queue':pin(ROOT/'工作佇列.json'),'task_count':len(items),'ready_count':0,
                      'implementation_selected':None,'execution_state':'waiting','wire_notification':False},ensure_ascii=True))


if __name__=='__main__':
    main()
