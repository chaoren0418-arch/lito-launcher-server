"""Package this task and append task-specific owner metadata, preserving history."""
from pathlib import Path
import sys,json,datetime,hashlib,struct
TASK=Path(__file__).resolve().parent
OWNER=TASK.parent
sys.path.insert(0,str(OWNER))
from audit import record,sha
ID='bin_resources-caption-message-boundary-20261007'
CONTRACT=OWNER/'caption-message-boundary-20261007-contract.json'

def save(path,obj):
    if path.exists():raise RuntimeError('Fresh artifact already exists: '+str(path))
    path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    now=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()
    witnesses=json.loads((TASK/'raw-witnesses.json').read_text(encoding='utf-8'))
    finite=json.loads((TASK/'finite-branch-verification.json').read_text(encoding='utf-8'))
    assert all(finite['checks'].values())
    state=next(x for x in witnesses['ranges'] if x['id']=='state_dispatch_table')
    state_targets=[f'{x:08X}' for x in struct.unpack('<11I',bytes.fromhex(state['raw_hex']))]
    blockers=[
        {'missing_input':'Qualified identity and semantics of fixed-PE external targets 05AD0BD1, 05AD0A70, 05AD0980 and registration target 05AE0922; the PE has no parsed imports.',
         'supplier':'controller / authorized original-source evidence supplier',
         'resume_when':'Controller reviews the semantic boundary and supplies newly qualified evidence that is independently permitted. No acquisition or expansion is authorized by this blocker.',
         'consumer':'Launcher causal-test planning; BIN semantic attribution'},
        {'missing_input':'Actual message-entry thread, UI/presenter/visibility instruction stacks and wait-object/lock ownership during a new hang; no current human-operable interval.',
         'supplier':'human / controller / Launcher owner with an originally permitted observation path',
         'resume_when':'Current human interval, exact PID/creation time/path/SHA, READY, unused claim, lease and cleanup gates all exist, and the controller has reviewed the observation plan. Consumed claims and exited T73/T81 remain unusable.',
         'consumer':'Launcher; product acceptance'},
        {'missing_input':'Semantics of state-dispatch descendant bodies; this task ends at the direct accessible dispatcher and its outgoing targets.',
         'supplier':'controller / original-source owner',
         'resume_when':'A new input-complete, explicit main-image scope outside retained refusals is reviewed; no recursive work is implied.',
         'consumer':'Launcher; BIN render attribution'},
    ]
    docs=[
      ('WM_MOVE','https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-move'),
      ('WM_SIZE','https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-size'),
      ('WM_PAINT','https://learn.microsoft.com/en-us/windows/win32/gdi/wm-paint'),
      ('WM_WINDOWPOSCHANGING','https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-windowposchanging'),
      ('WM_WINDOWPOSCHANGED','https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-windowposchanged'),
      ('WM_ENTERSIZEMOVE','https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-entersizemove'),
      ('WM_EXITSIZEMOVE','https://learn.microsoft.com/en-us/windows/win32/winmsg/wm-exitsizemove'),
      ('WM_SYSCOMMAND','https://learn.microsoft.com/en-us/windows/win32/menurc/wm-syscommand'),
      ('WNDCLASSEXA layout','https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-wndclassexa'),
    ]
    save(TASK/'documentation-references.json',{'provider':'Microsoft Learn','references':[{'name':n,'url':u} for n,u in docs],
      'use':'Message constants and documented ABI/OS behavior only; these documents do not resolve opaque original call targets.',
      'inference':'The 48-byte registration-shaped local structure is consistent with x86 WNDCLASSEXA and contains the candidate callback pointer. Successful registration and API identity are unproved.'})
    save(TASK/'exploration-corrections.json',{'failed_qualification_runs':0,
      'explained_navigation_or_output_failures':[
       {'receipt':'6110f7','result':'Navigation script StopIteration after several available functions because requested named proc was absent/collapsed. No missing WndProc body or failed byte test; no dependent body inferred from this absence.'},
       {'receipt':'fa5695','result':'Read-only metadata print failed on cp950 encoding of historical U+2264; rerun e85968 configured UTF-8. Evidence outputs were already written and unchanged.'}],
      'discarded_exploratory_computations':[
       {'receipt':'3694e5','result':'Exploratory nonrequested sys/high table guesses used wrong selector bases. They are not evidence; actual syscommand router uses 48013C and modal route uses comparisons, not guessed table entries.'},
       {'reviewer_receipt':'bdab2c','result':'Reviewer first low-route computation incorrectly used base480000; corrected receipt15cc70 uses raw jmp base47FFFC. Final formal finite route test independently uses Capstone memory operands.'}],
      'all_accepted_local_claims':'Qualified only by final raw-witnesses and finite verification; no stale PASS or erroneous computed line adopted.'})
    save(TASK/'test-receipts.json',{'task_id':ID,'evidence_level':'static',
      'executed':[{'command':'C:/Users/CHAOREN/AppData/Local/Programs/Python/Python312/python.exe -B ./caption-message-boundary-20261007/qualify_fixed_bytes.py',
      'cwd':str(OWNER),'tool_receipt':'d64ec9','exit_code':0,'stdout':record(TASK/'qualification.stdout.txt'),
      'executed_script':record(TASK/'qualify_fixed_bytes.executed.txt'),
      'current_script':record(TASK/'qualify_fixed_bytes.py'),
      'script_change_after_execution':'Only ASM navigation excerpt lines corrected; raw qualification logic unchanged. See navigation-repair-receipt.json.',
      'result':record(TASK/'finite-branch-verification.json'),
      'new_hypothesis':'Movement/modal/position routes versus a distinct paint conditional update call; all three pinned source hashes, 25 raw slices, 11 message routes, helper rejection, exact call/registration/reset bytes and rel32 targets.'}],
      'caption_filter_check_method':'Generator checks literal suppression-set exclusion; independent reviewer separately evaluates raw filter branches including low-nibble variants.',
      'independent_static_review':record(TASK/'independent-review.json'),
      'not_executed':['Original PE or game','IDA','native GUI/input/screenshot','process attach or memory acquisition','old T82/T81 tests','resource extraction','SQL/server/opcode changes'],
      'PASS_scope':'Finite static qualification only; no product or freeze acceptance.'})
    contract={
      'task_id':ID,'owner':'new BIN development owner','state':'review','updated_at':now,
      'accepted':False,'self_accepted':False,'product_complete':False,'evidence_level':'static',
      'question':'Which accessible fixed MAIN-IMAGE caption/move/size/position/modal/paint branches can synchronously enter update/render/rebase/wait paths, and what caller/thread facts do fixed raw bytes prove?',
      'source':witnesses['source'],'baseline':record(TASK/'baseline.json'),
      'navigation':record(TASK/'asm-navigation.json'),'raw_witnesses':record(TASK/'raw-witnesses.json'),
      'finite_verification':record(TASK/'finite-branch-verification.json'),
      'original_dispatcher':{'va':'0047F340','rva':'0007F340','file_offset':'0007F340',
        'qualification':'Four stack arguments; second argument dispatched from EDI; ret10h. Function pointer stored at4802B9 in registration-shaped caller480290; named API and successful registration unproved.',
        'caller':'00480290 writes0047F340 at004802B9 and passes local structure to external05AE0922 at004802F9. Only this observed pointer relationship is claimed; no exhaustive caller census.'},
      'branches':[
        {'messages':['WM_MOVE=0003','WM_SIZE=0005'],'entry':'0047F390',
         'proved':'If first argument equals[64BBEC], write zero to[64BBE0] and[64BBE4], then return1. If unequal, call external fallback05AD0980. Optional early helper4C6B30 rejects these messages without nested calls.',
         'not_proved':'No direct local render/rebase/wait call on matching handle path; reset-cell consumer semantics and external fallback behavior remain unknown.'},
        {'messages':['WM_PAINT=000F'],'entry':'0047F3BA',
         'proved':'First CALL at47F3C8 targets external05AD0BD1 using[64BBEC], local16-byte destination and zero. If return is nonzero, [6476DC] is nonzero, any of four local dwords is nonzero, and[64BBD4]!=4, CALL47CC90 at47F3FF before returning. After nonzero first return, CALL external05AD0A70 at47F40E. Return0.',
         'qualification':'Local dwords are opaque; calling them an update RECT or external calls GetUpdateRect/ValidateRect is unsupported by this snapshot.',
         'not_proved':'Actual first return/local writes, rendering success, live state, wait semantics, driver or lock ownership.'},
        {'messages':['WM_WINDOWPOSCHANGING=0046','WM_WINDOWPOSCHANGED=0047','WM_NCLBUTTONDOWN=00A1'],
         'entry':'0047FFA8','proved':'Position-selector value5 selects default branch. Helper4C6B30 rejects value without nested calls, then four original arguments go to external05AD0980 at47FFEB.',
         'not_proved':'No dedicated local rebase/update/wait body on these selected routes; default external behavior is opaque.'},
        {'messages':['WM_ENTERSIZEMOVE=0231','WM_EXITSIZEMOVE=0232','WM_SIZING=0214','WM_MOVING=0216'],
         'entry':'0047FFA8','proved':'Router47FE4A subtracts203; indices exceed7 and branch to47FFA8. Same rejecting helper and external fallback. No dedicated branch body selected for these values.',
         'not_proved':'No conclusion that modal behavior or waits are absent behind external fallback.'},
        {'messages':['WM_SYSCOMMAND=0112','SC_SIZE=F000','SC_MOVE=F010'],
         'entry':'0047FA7A','proved':'Mask wParam withFFF0 when[64BBE8]!=0; suppression set isF060/F090/F100/F140/F170. SC_SIZE and SC_MOVE reach47FFE4 under either flag value, which CALLs external05AD0980.',
         'not_proved':'The external target is not qualified as DefWindowProc. Documented DefWindowProc modal behavior is a conditional test hypothesis only.'},
      ],
      'direct_state_dispatch':{'va':'0047CC90','rva':'0007CC90','file_offset':'0007CC90',
        'table_va':'0047CD40','states_0_through_10':state_targets,
        'proved':'Read[64BBD4]; unsigned>10 returns. Otherwise dispatch via table. State0 sets[64BBD4]=8, resets[64B478], writes callback pointers496270/4961D0, CALLs4D65F0. State6 sets state7, CALLs497C60 then external05AD0986. Some states tail-JMP to outgoing main-image bodies.',
        'semantic_level':'Accessible synchronous state mutation/dispatch; rendering, rebase and wait semantics of outgoing bodies are not established.',
        'stop_main_image_descendants':['004D65F0','00478980','00495E40','0046D630','00478AC0','00497C60'],
        'reason':'Beyond this task\'s direct accessible call-body boundary; no recursive analysis performed.'},
      'caller_thread_facts':[
        'x86 direct CALL47CC90 preserves the current caller continuation; the locally selected paint branch contains no queue/worker dispatch instruction before that CALL.',
        'Raw control flow proves this direct synchronous boundary, not the runtime thread ID. A callee may have effects outside the bounded body.',
        'Registration-shaped pointer storage associates candidate callback47F340 with caller480290. No successful OS registration, HWND value, owner epoch, actual caller stack, UI/presenter/visibility thread or lock ownership is proved.',
        'No identifiable lock/wait API or local wait loop is established within the relevant bounded branches. This cannot exclude waits in opaque external or stopped descendant calls.'
      ],
      'exact_external_STOP_targets':[
        {'va':'05AD0BD1','site':'0047F3C8','role':'first paint CALL; identity unknown'},
        {'va':'05AD0A70','site':'0047F40E','role':'second paint CALL; identity unknown'},
        {'va':'05AD0980','site':'0047FFEB','role':'default four-argument fallback; identity unknown'},
        {'va':'05AD0986','site':'0047CD3A','role':'state6 call; identity unknown'},
        {'va':'05AE0922','site':'004802F9','role':'registration-shaped call; identity unknown'},
        {'va':'758C5F50','site':'004802D4','role':'registration helper; identity unknown'},
        {'cell':'004F7454','sites':['004802C9','004802EF'],'role':'indirect registration helper; target/API ownership unproved'}
      ],
      'retained_forbidden_boundaries':['0042E550','0042E6D0','0042EA50','050104CD',
        'map private05040E1D','external formatter773F5200','private provider backing and owner epochs',
        '495520 semantics boundary','native input/screenshot restrictions',
        'Server T291/T27/refund/rebid/auction/castle/full-WORLD dependent refusals'],
      'T82_nonduplication':{'scope':'Read-only review of Compat-only source hazard analysis; historical tests not replayed.',
        'handoff':{'path':'D:/lineage開發/3-lito開發/lito登入器/Native-T82-Caption-Drag-Fix-Handoff.json','sha256':'FC6FEC5269CC17C731632D14BFCDFC6053D635FF68175A8F213631F46D4EA98B'},
        'difference':'Original fixed main-image move/size only reset cells for matching handle; paint conditional state-dispatch path differs from Compat\'s rebase/Present logic. Neither evidence establishes the native hang cycle.'},
      'actionable_conclusion':'Launcher can plan observations at47F340 message entry,47F3FF paint update CALL/return,47FFEB external fallback CALL/return, and its existing Compat original-WndProc boundary. Separating durations there tests whether a drag stall is already inside original paint/default processing or occurs after return in Compat. No opcode/source change or runtime probe is authorized by this contract.',
      'prospective_causal_test':{'state':'blocked_missing_inputs','executed':False,
        'decision_points':['Record original message value and current OS thread at callback entry.',
          'Pair before/after observations of paint call47F3FF and fallback47FFEB with observed state64BBD4; avoid guessing API names.',
          'Compare time spent inside original callback with Compat rebase/Present boundary, using a controller-reviewed permitted path.',
          'If a boundary does not return, qualify actual instruction stack and wait ownership; do not infer deadlock from static call presence.'],
        'no_current_human_interval':True},
      'unknowns':['Identity/semantics of all opaque external calls; no parsed imports.',
        'Initialized runtime global values and owner epochs; snapshot zeros are not live values.',
        'Actual GUI registration/callback delivery/thread identity.',
        'Render/rebase semantics of state-dispatch descendants.',
        'Wait objects, lock ordering, driver state, exact drag hang cause and player-visible result.'],
      'blockers':blockers,'next_ready_task':None,
      'tests':record(TASK/'test-receipts.json'),
      'forbidden_actions_performed':False,'historical_owner_messages_sent':0,
      'heartbeat_changed':False,'scheduled_work_created':False,
      'no_product_completion_claim':True,
      'delivery_index':str(TASK/'delivery-manifest.json'),
    }
    save(CONTRACT,contract)
    # Only append this task's fields; keep existing top-level workflow/history intact.
    changes=[]
    for filename in ['監製進度.json','工作佇列.json']:
        p=OWNER/filename;old_bytes=p.read_bytes();old=json.loads(old_bytes.decode('utf-8-sig'))
        backup=TASK/(filename+'.before');backup.write_bytes(old_bytes)
        obj=json.loads(old_bytes.decode('utf-8-sig'))
        key='caption_message_boundary_20261007'
        if key in obj:raise RuntimeError('Task metadata already exists')
        obj[key]={'task_id':ID,'state':'review','updated_at':now,'delivery':record(CONTRACT),
          'evidence_level':'static','self_accepted':False,'product_complete':False,
          'blockers':blockers,'next_ready_task':None}
        if filename=='工作佇列.json':
            assert not any(x.get('id')==ID for x in obj['items'])
            obj['items'].append({'id':ID,'owner':'bin_resources','title':'Qualify original caption/message boundary from fixed MAIN-IMAGE bytes',
              'state':'review','evidence_level':'static','delivery':record(CONTRACT),
              'blockers':blockers,'next_ready_task':None,'self_accepted':False,
              'execution_result':'new_source_backed_main_image_evidence; no product acceptance'})
        # Demonstrate exact preservation of all historical JSON fields/items.
        preserved=json.loads(json.dumps(obj));del preserved[key]
        if filename=='工作佇列.json':preserved['items'].pop()
        assert preserved==old,'Unrelated metadata changed'
        p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        changes.append({'path':str(p),'before_sha256':sha(old_bytes),'after':record(p),
                        'before_snapshot':record(backup),'historical_json_preserved':True,
                        'modified_fields':[key]+(['items[append task_id='+ID+']'] if filename=='工作佇列.json' else [])})
    save(TASK/'owned-metadata-changes.json',{'task_id':ID,'changes':changes,
        'rule':'No pre-existing field, history entry or queue item changed; only task-specific additions.'})
    artifacts=[record(p) for p in sorted(TASK.rglob('*')) if p.is_file()]
    save(TASK/'delivery-manifest.json',{'task_id':ID,'state':'review','contract':record(CONTRACT),
      'created_files':[{'before_sha256':None,**r} for r in artifacts]+[{'before_sha256':None,**record(CONTRACT)}],
      'changed_existing_files':changes,'manifest_self_hash':'Provided by final handoff; self-reference excluded.',
      'forbidden_actions_performed':False})
    print(json.dumps({'task_id':ID,'state':'review','contract':record(CONTRACT),
      'manifest':record(TASK/'delivery-manifest.json'),'metadata_changes':changes},ensure_ascii=True))

if __name__=='__main__':main()
