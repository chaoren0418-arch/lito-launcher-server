import sys
sys.dont_write_bytecode=True
import hashlib,json,struct
from pathlib import Path
from datetime import datetime,timezone,timedelta
HERE=Path(__file__).resolve().parent;ROOT=HERE.parent
TASK='bin_resources-ida77-isolated-export-integration-20261007'
def pin(p):
    b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest().upper()}
def save(p,v):
    with p.open('xb') as f:f.write(json.dumps(v,ensure_ascii=False,indent=2).encode('utf-8'))
def walk(x):
    if isinstance(x,dict):
        if x.get('id')==TASK:yield x
        for v in x.values():yield from walk(v)
    elif isinstance(x,list):
        for v in x:yield from walk(v)
def main():
    ix=Path(r'D:\lineage開發\3-lito開發\LITO-監製索引.json')
    task=list(walk(json.loads(ix.read_text('utf-8-sig'))));assert len(task)==1
    for p in task[0]['evidence']:assert pin(Path(p['path']))['sha256']==p['sha256']
    save(HERE/'task-at-read.json',{'index_at_read':pin(ix),'task':task[0]})
    source=ROOT/'watchdog-error-paths-20261007'/'owned_process.py'
    assert pin(source)['sha256']=='AD639F124F953C4992605E8BAC98E282E6B9F96C71EABFCF5B8B1101DD223C4B'
    for name,src in [('owned_process.py',source),('ida77_request.py',ROOT/'ida77-bounded-20261007'/'ida77_request.py'),
                     ('ida77_export_draft.py',ROOT/'ida77-bounded-20261007'/'ida77_export.py')]:
        with (HERE/name).open('xb') as f:f.write(src.read_bytes())
    immutable=[]
    for directory in ['ida77-bounded-20261007','owned-watchdog-20261007','watchdog-error-paths-20261007']:
        immutable.extend(pin(p) for p in sorted((ROOT/directory).rglob('*')) if p.is_file())
    immutable.extend(pin(ROOT/name) for name in ['IDA77-Bounded-Export-Adapter-20261007.json','Owned-Process-Watchdog-20261007.json','Owned-Watchdog-Error-Paths-20261007.json'])
    save(HERE/'immutable-baseline.json',{'pins':immutable,'accepted_watchdog_source':pin(source),'copied_watchdog':pin(HERE/'owned_process.py'),'old_PASS_execution':False})
    original=Path(r'D:\IDA_Pro_7.7');files=[]
    for directory in [original,original/'python38',original/'python'/'3']:
        files.append({'directory':str(directory),'files':[{'name':p.name,'bytes':p.stat().st_size} for p in sorted(directory.iterdir()) if p.is_file()]})
    def pe_machine(p):
        b=p.read_bytes();off=struct.unpack_from('<I',b,60)[0];return hex(struct.unpack_from('<H',b,off+4)[0])
    runtime={'installation':str(original),'inventory':files,
         'core_binaries':[dict(pin(original/n),pe_machine=pe_machine(original/n)) for n in ['idat.exe','ida.dll','plugins/idapython3.dll','plugins/hexrays.dll','loaders/pe.dll','procs/pc.dll']],
         'WindowsSandbox_executable_present':(Path(r'C:\Windows\System32\WindowsSandbox.exe')).is_file(),
         'local_compiler':pin(Path(r'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe')),
         'original_installation_or_global_settings_modified':False}
    save(HERE/'dependency-inventory.json',runtime)
    pp=ROOT/'監製進度.json';qp=ROOT/'工作佇列.json';before={'progress':pin(pp),'queue':pin(qp)}
    p=json.loads(pp.read_text('utf-8'));q=json.loads(qp.read_text('utf-8'))
    p.setdefault('workflow_history',[]).append({'superseded_by':TASK,'previous':{k:p.get(k) for k in ['workflow_task_id','workflow_task_state','workflow_handoff']}})
    now=datetime.now(timezone(timedelta(hours=8))).isoformat()
    p.update(updated_at=now,workflow_task_id=TASK,workflow_task_state='running',current_task='私人AppContainer profile資格驗證與IDA77 fixture整合進行中；不解析LITO，先驗證egress/write控制。',
             workflow_handoff={'proposal_state':'running','task':pin(HERE/'task-at-read.json'),'handoff_to':['supervisor']})
    dependency=next(i for i in q['items'] if i['id']=='bin_resources-watchdog-error-paths-20261007')
    dependency['state']='accepted';dependency['supervisor_assessment']=task[0]['evidence'][0]
    assert not any(i['id']==TASK for i in q['items'])
    current=dict(task[0]);current['state']='running';q['items'].append(current)
    q.update(updated_at=now,workflow_task_id=TASK,proposal_state='running',execution_state='running',implementation_selected=TASK,
             ready_count=sum(i['state']=='ready' for i in q['items']),blocked_count=sum(i['state']=='blocked' for i in q['items']))
    for path,v in [(pp,p),(qp,q)]:
        tmp=path.with_name(path.stem+'.ida77-integration.tmp.json');save(tmp,v);tmp.replace(path)
    save(HERE/'prepare-receipt.json',{'before':before,'after':{'progress':pin(pp),'queue':pin(qp)},'old_immutable_pins':len(immutable)})
    print(json.dumps({'task_state':'running','accepted_watchdog_pin_verified':True,'old_immutable_pins':len(immutable)},ensure_ascii=True))
if __name__=='__main__':main()
