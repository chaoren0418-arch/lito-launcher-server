import sys
sys.dont_write_bytecode=True
import json
from pathlib import Path
from datetime import datetime,timezone,timedelta
from owned_process import HERE,digest,save
ROOT=HERE.parent
TASK='bin_resources-owned-process-watchdog-20261007'
def pin(p):return {'path':str(p),'bytes':p.stat().st_size,'sha256':digest(p)}
def matches(obj):
    if isinstance(obj,dict):
        if obj.get('id')==TASK:yield obj
        for v in obj.values():yield from matches(v)
    elif isinstance(obj,list):
        for v in obj:yield from matches(v)
def main():
    index=Path(r'D:\lineage開發\3-lito開發\LITO-監製索引.json')
    data=json.loads(index.read_text('utf-8-sig'));found=list(matches(data));assert len(found)==1
    task=found[0];assert task['owner']=='bin_resources'
    save(HERE/'authorized-task-at-read.json',{'index_pin_at_read':pin(index),'task':task})
    assessment=Path(r'D:\lineage開發\3-lito開發\監製工作流\bin_resources-ida77-bounded-export-adapter-20261007-assessment.json')
    olddir=ROOT/'ida77-bounded-20261007'
    baseline=[pin(p) for p in sorted(olddir.rglob('*')) if p.is_file()]+[pin(ROOT/'IDA77-Bounded-Export-Adapter-20261007.json')]
    save(HERE/'immutable-prior-adapter-pins.json',{'pins':baseline,'supervisor_assessment':pin(assessment),
              'policy':'Fingerprint preservation only; no old tests, IDA or fixture execution'})
    progress_path=ROOT/'監製進度.json';queue_path=ROOT/'工作佇列.json'
    before={'progress':pin(progress_path),'queue':pin(queue_path)}
    p=json.loads(progress_path.read_text('utf-8-sig'));q=json.loads(queue_path.read_text('utf-8-sig'))
    prior={k:p.get(k) for k in ['workflow_task_id','workflow_task_state','workflow_handoff','workflow_queue_path']}
    p.setdefault('workflow_history',[]).append({'superseded_by':TASK,'previous':prior})
    now=datetime.now(timezone(timedelta(hours=8))).isoformat()
    p.update(updated_at=now,workflow_task_id=TASK,workflow_task_state='running',
         current_task='自有程序樹watchdog實作與新無害fixtures測試；不啟動IDA，舊adapter保持封存。',
         workflow_handoff={'proposal_state':'running','task_snapshot':pin(HERE/'authorized-task-at-read.json'),'handoff_to':['supervisor']})
    assert q['version']==2 and q['owner']=='bin_resources' and not any(i['id']==TASK for i in q['items'])
    item=dict(task);item['state']='running';q['items'].append(item)
    q.update(updated_at=now,workflow_task_id=TASK,proposal_state='running',execution_state='running',
         ready_count=sum(i['state']=='ready' for i in q['items']),
         blocked_count=sum(i['state']=='blocked' for i in q['items']),implementation_selected=TASK)
    for path,value in [(progress_path,p),(queue_path,q)]:
        temporary=path.with_name(path.stem+'.watchdog.tmp.json')
        with temporary.open('xb') as f:f.write(json.dumps(value,ensure_ascii=False,indent=2).encode('utf-8'))
        temporary.replace(path)
    save(HERE/'prepare-receipt.json',{'task_id':TASK,'before':before,'after':{'progress':pin(progress_path),'queue':pin(queue_path)}})
    print(json.dumps({'task_id':TASK,'state':'running','immutable_prior_pins':len(baseline)}))
if __name__=='__main__':main()
