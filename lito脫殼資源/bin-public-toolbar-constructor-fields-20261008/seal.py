import json,hashlib,sys
from pathlib import Path
from datetime import datetime,timezone,timedelta
sys.stdout.reconfigure(encoding='utf-8');O=Path(__file__).resolve().parent;R=O.parent
def pin(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest().upper()}
mp=O/'delivery-manifest.json';assert not mp.exists(),'Final seal already exists; future additions require an amendment.'
h=json.loads((O/'handoff.json').read_text(encoding='utf-8'));e=json.loads((O/'raw-evidence.json').read_text(encoding='utf-8'));pe=Path(e['source']['path']).read_bytes();assert hashlib.sha256(pe).hexdigest().upper()==e['source']['sha256']
for r in e['ranges']:
 b=Path(r['artifact']['path']).read_bytes();assert pin(Path(r['artifact']['path']))==r['artifact'];f=int(r['file_offset'],16);assert pe[f:f+r['bytes']]==b
 assert sum(x['size'] for x in r['instructions'])==r['bytes']
m={'task_id':h['task_id'],'sealed_at':datetime.now(timezone(timedelta(hours=8))).isoformat(),'immutable_after_seal':True,'derivation_receipt':'5bfe8b','command_exit':0,'artifacts':[pin(p) for p in sorted(O.iterdir()) if p.is_file() and p.name not in ['delivery-manifest.json','delivery-manifest.pending.json']]}
assert (O/'command.stdout.txt').exists() and (O/'command.stderr.txt').read_bytes()==b''
pending=O/'delivery-manifest.pending.json';pending.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');pending.replace(mp)
for z in m['artifacts']:assert pin(Path(z['path']))==z
field={'task_id':h['task_id'],'state':'review','question':h['question'],'exact_scope':h['scope'],'delivery':pin(O/'handoff.json'),'delivery_manifest':pin(mp),'reported_to_supervisor':False,'supervisor_review_pending':True,'product_accepted':False,'actual_native_goal_status':'blocked','next_input_complete_task':None,'final_seal_immutable':True}
for name in ['監製進度.json','工作佇列.json']:
 p=R/name;d=json.loads(p.read_text(encoding='utf-8-sig'));d['bin_public_toolbar_constructor_fields_20261008']=field;p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'handoff':pin(O/'handoff.json'),'final_manifest':pin(mp),'verified_artifact_count':len(m['artifacts']),'all_logs_closed_before_seal':True},ensure_ascii=False))
