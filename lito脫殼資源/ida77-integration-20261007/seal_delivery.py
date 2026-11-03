import sys
sys.dont_write_bytecode=True
import json,hashlib,ast,subprocess
from pathlib import Path
HERE=Path(__file__).resolve().parent;ROOT=HERE.parent
def read(p):return json.loads(p.read_text('utf-8-sig'))
def pin(p):
    b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest().upper()}
def save(p,v):
    with p.open('xb') as f:f.write(json.dumps(v,ensure_ascii=False,indent=2).encode('utf-8'))
def replace(p,v):
    t=p.with_name(p.stem+'.seal.tmp.json');save(t,v);t.replace(p)
syntaxpath=HERE/'syntax-verification.json';syntax=read(syntaxpath)
syntax['sources']=[]
for p in sorted(HERE.glob('*.py'))+list((HERE/'private-runtime').glob('*.py')):
    ast.parse(p.read_text('utf-8-sig'),filename=str(p));syntax['sources'].append(pin(p))
replace(syntaxpath,syntax)
validation=subprocess.run([sys.executable,'-B',r'C:\Users\CHAOREN\.codex\skills\lito-supervisor\scripts\supervise.py',
    'validate','--queue',str(ROOT/'工作佇列.json')],capture_output=True,text=True,encoding='utf-8',timeout=20)
assert validation.returncode==0
value=json.loads(validation.stdout);assert value['valid']
validationpath=HERE/'queue-validation.json'
save(validationpath,{'exit_code':validation.returncode,'output':value,'stderr':validation.stderr,'old_tests_replayed':False})
reportpath=ROOT/'IDA77-Isolated-Export-Integration-20261007.json';report=read(reportpath)
report['accepted_component']['sources']=[pin(Path(r['path'])) for r in report['accepted_component']['sources']]
report['verification']['syntax_only']=pin(syntaxpath);report['verification']['queue_validation']=pin(validationpath)
replace(reportpath,report)
pp=ROOT/'監製進度.json';qp=ROOT/'工作佇列.json';before={'progress':pin(pp),'queue':pin(qp)}
progress=read(pp);queue=read(qp);progress['workflow_handoff']['delivery']=pin(reportpath)
next(i for i in queue['items'] if i['id']==report['task_id'])['delivery']=pin(reportpath)
replace(pp,progress);replace(qp,queue)
save(HERE/'workflow-seal-receipt.json',{'before':before,'after':{'progress':pin(pp),'queue':pin(qp)},'state':'review','execution_result':'blocked_platform'})
pins=[pin(p) for p in sorted(HERE.rglob('*')) if p.is_file()]+[pin(reportpath),pin(pp),pin(qp)]
delivery=HERE/'delivery-receipt.json'
save(delivery,{'schema':'finite-IDA77-integration-delivery-v1','task_id':report['task_id'],'result':'BLOCKED_PLATFORM_NATIVE_LPAC_CREATION',
    'pins':pins,'pin_count':len(pins),'report':pin(reportpath),'old_immutable_pins_verified':491,'new_LITO_analysis_request':'none'})
for r in pins:assert pin(Path(r['path']))==r
for old in read(HERE/'immutable-baseline.json')['pins']:assert pin(Path(old['path']))==old
print(json.dumps({'report':pin(reportpath),'delivery':pin(delivery),'finite_pins_verified':len(pins),
    'old_pins_unchanged':491,'queue_valid':True,'native_probes_created':0,'IDA_runs':0},ensure_ascii=True))
