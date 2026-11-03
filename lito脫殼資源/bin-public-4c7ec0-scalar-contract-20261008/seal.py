import sys,json,hashlib
from pathlib import Path
from datetime import datetime,timezone
sys.stdout.reconfigure(encoding='utf-8');O=Path(__file__).resolve().parent;R=O.parent
def pin(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest().upper()}
mp=O/'delivery-manifest.json';assert not mp.exists(),'One final immutable seal; future additions separately amended.'
assert (O/'extract.stderr.txt').read_bytes()==b'' and (O/'contract.stderr.txt').read_bytes()==b''
h=json.loads((O/'handoff.json').read_text(encoding='utf-8'));g=json.loads((O/'actual-goal-status.json').read_text(encoding='utf-8'))
assert h['BOUNDED_PSEUDOCODE'][:3]==['receiver = incomingECX;','p = opaque_00419DB0(ECX=receiver);','scalar = stack_arg_dword; // loaded after first helper returns']
assert pin(Path(h['SOURCE_VA_RVA']['artifact']['path']))==h['SOURCE_VA_RVA']['artifact']
m={'TASK_ID':h['TASK_ID'],'sealed_at':datetime.now(timezone.utc).isoformat(),'extract_receipt':'0bd2ec','extract_exit':0,'contract_receipt':'b07b26','contract_exit':0,'preseal_correction_receipt':'094413','preseal_correction_exit':0,'preseal_correction':'Pseudocode scalar load placed after first helper call, matching already qualified raw004C7EC7/004C7ECC; no raw/body/caller replay. Earlier contract.stdout preseal SHA precedes this text correction.','all_captured_logs_closed_before_first_final_manifest':True,'artifacts':[pin(p) for p in sorted(O.iterdir()) if p.is_file() and p.name not in ['delivery-manifest.json','delivery-manifest.pending.json']]}
p=O/'delivery-manifest.pending.json';p.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');p.replace(mp)
for z in m['artifacts']:assert pin(Path(z['path']))==z
f={'task_id':h['TASK_ID'],'state':h['STATE'],'handoff':pin(O/'handoff.json'),'delivery_manifest':pin(mp),'input_pins':h['INPUT_SHA'],'qualified_VA':'004C7EC0','qualified_RVA':'000C7EC0','raw_body_bytes':49,'evidence_level':h['LEVEL'],'reported_to_supervisor':False,'supervisor_review_pending':True,'consumer_adopted':False,'next_ready_task':None,'actual_native_goal_status':g['goal']['status'],'PRODUCT_ACCEPTED':False,'attached_or_monitoring':False,'stopped_after_one_body':True,'final_seal_immutable':True}
pr=json.loads((O/'prior-runtime-review-input.json').read_text(encoding='utf-8'))
for n in ['監製進度.json','工作佇列.json']:
 p=R/n;d=json.loads(p.read_text(encoding='utf-8-sig'));d['bin_public_4c7ec0_scalar_contract_20261008']=f
 old=d.get('bin_automated_native_public_session_20261008_fresh5')
 if old is not None:old.update(supervisor_review_pending=False,supervisor_decision='accepted',supervisor_assessment=pr['pin'],supervisor_scope=pr['scope'],runtime_native_accepted=False)
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'handoff':pin(O/'handoff.json'),'manifest':pin(mp),'artifact_count':len(m['artifacts']),'PRODUCT_ACCEPTED':False},ensure_ascii=False))
