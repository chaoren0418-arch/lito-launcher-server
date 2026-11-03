import sys
sys.dont_write_bytecode=True
import hashlib,json
from pathlib import Path
from datetime import datetime,timezone,timedelta
HERE=Path(__file__).resolve().parent;ROOT=HERE.parent
TASK='bin_resources-watchdog-error-paths-20261007'
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
    index=Path(r'D:\lineage開發\3-lito開發\LITO-監製索引.json')
    task=list(walk(json.loads(index.read_text('utf-8-sig'))));assert len(task)==1
    assessment=Path(r'D:\lineage開發\3-lito開發\監製工作流\bin_resources-owned-process-watchdog-20261007-assessment.json')
    assert pin(assessment)['sha256']=='96455CA62639C2FADA58465C0053B443702BE48A530E9A6755E2DB784B7BCA82'
    save(HERE/'task-at-read.json',{'index_pin_at_read':pin(index),'task':task[0],'assessment':pin(assessment)})
    old=[]
    for directory in [ROOT/'owned-watchdog-20261007',ROOT/'ida77-bounded-20261007']:
        old.extend(pin(p) for p in sorted(directory.rglob('*')) if p.is_file())
    old.extend(pin(ROOT/n) for n in ['Owned-Process-Watchdog-20261007.json','IDA77-Bounded-Export-Adapter-20261007.json'])
    save(HERE/'immutable-baseline.json',{'pins':old,'old_test_execution':False})
    source=ROOT/'owned-watchdog-20261007'/'owned_process.py'
    with (HERE/'owned_process.py').open('xb') as f:f.write(source.read_bytes())
    progress_path=ROOT/'監製進度.json';queue_path=ROOT/'工作佇列.json'
    before={'progress':pin(progress_path),'queue':pin(queue_path)}
    p=json.loads(progress_path.read_text('utf-8'));q=json.loads(queue_path.read_text('utf-8'))
    previous={k:p.get(k) for k in ['workflow_task_id','workflow_task_state','workflow_handoff']}
    p.setdefault('workflow_history',[]).append({'superseded_by':TASK,'previous':previous})
    now=datetime.now(timezone(timedelta(hours=8))).isoformat()
    p.update(updated_at=now,workflow_task_id=TASK,workflow_task_state='running',
             current_task='新successor修正constructor cleanup與錯誤收據持久化；僅新增受影響路徑fixtures。',
             workflow_handoff={'proposal_state':'running','task_at_read':pin(HERE/'task-at-read.json'),'handoff_to':['supervisor']})
    assert not any(t['id']==TASK for t in q['items'])
    item=dict(task[0]);item['state']='running';q['items'].append(item)
    q.update(updated_at=now,workflow_task_id=TASK,proposal_state='running',execution_state='running',implementation_selected=TASK,
             ready_count=sum(i['state']=='ready' for i in q['items']),blocked_count=sum(i['state']=='blocked' for i in q['items']))
    for path,v in [(progress_path,p),(queue_path,q)]:
        tmp=path.with_name(path.stem+'.error-paths.tmp.json');save(tmp,v);tmp.replace(path)
    save(HERE/'prepare-receipt.json',{'before':before,'after':{'progress':pin(progress_path),'queue':pin(queue_path)},'old_module':pin(source)})
    print(json.dumps({'state':'running','old_immutable_pins':len(old)}))
if __name__=='__main__':main()
