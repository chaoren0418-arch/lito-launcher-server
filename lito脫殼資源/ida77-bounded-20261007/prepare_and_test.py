"""One-shot, fixture-only preflight tests. Never launches IDA or fixture.exe."""
import sys
sys.dont_write_bytecode=True
import ast
import copy
import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace
from ida77_request import CAPS, DEFAULTS, validate_request, Refusal, sha, decompiler_probe
from ida77_runner import run, FIXTURE_SHA

HERE=Path(__file__).resolve().parent
SOURCE=Path(r'D:\lito-dev-runtime\20261003-supervision\supervisor\ida77-qualification-20261007\fixture.exe')

def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as f:
        f.write(json.dumps(value,ensure_ascii=False,indent=2).encode('utf-8'))

def record(path):
    raw=path.read_bytes()
    return {'path':str(path),'bytes':len(raw),'sha256':sha(raw)}

def main():
    fixture=HERE/'fixtures'/'fixture.exe'
    raw=SOURCE.read_bytes()
    assert sha(raw)==FIXTURE_SHA
    fixture.parent.mkdir(parents=True,exist_ok=True)
    with fixture.open('xb') as f:
        f.write(raw)
    request={'schema':'ida77-bounded-request-v1','source_path':str(fixture),
             'source_sha256':FIXTURE_SHA,'image_base':0x400000,
             'functions':[{'rva':0x1000,'end_rva':0x100d},{'rva':0x1010,'end_rva':0x1019}],
             'limits':DEFAULTS,'authorization_scope':'synthetic_tool_fixture_only'}
    properties={'schema':{'const':'ida77-bounded-request-v1'},
                'source_path':{'type':'string'},'source_sha256':{'type':'string','pattern':'^[A-Fa-f0-9]{64}$'},
                'image_base':{'type':'integer','minimum':0,'maximum':4294967295},
                'authorization_scope':{'const':'synthetic_tool_fixture_only'},
                'functions':{'type':'array','minItems':1,'maxItems':8,
                             'items':{'type':'object','additionalProperties':False,'required':['rva','end_rva'],
                                      'properties':{k:{'type':'integer','minimum':0,'maximum':67108864} for k in ['rva','end_rva']}}},
                'limits':{'type':'object','additionalProperties':False,
                          'properties':{k:{'type':'integer','minimum':1,'maximum':v} for k,v in CAPS.items()}}}
    save(HERE/'request.schema.json',{'$schema':'https://json-schema.org/draft/2020-12/schema',
         'title':'IDA77 bounded fixture request','type':'object','additionalProperties':False,
         'required':['schema','source_path','source_sha256','image_base','functions','authorization_scope'],
         'properties':properties,'description':'Semantic SHA, PE32, ranges and authorization checks are mandatory in ida77_request.py; JSON schema alone does not authorize launch.'})
    address={'type':'object','required':['va','rva','source_sha256','ida_version','evidence_level'],
             'properties':{'va':{'type':'integer'},'rva':{'type':'integer'},'source_sha256':{'type':'string'},
                           'ida_version':{'type':'string'},'evidence_level':{'type':'string'}}}
    provenance={'source_sha256':{'type':'string'},'ida_version':{'type':'string'},'evidence_level':{'type':'string'}}
    instruction={'type':'object','required':['address','bytes','text',*provenance],
                 'properties':{'address':address,'bytes':{'type':'string'},'text':{'type':'string'},**provenance}}
    xref={'type':'object','required':['direction','from','to','type',*provenance],
          'properties':{'direction':{'enum':['incoming_to_start','outgoing_from_item']},
                        'from':address,'to':address,'type':{'type':'integer'},**provenance}}
    pseudo={'type':'object','required':['address','text',*provenance],
            'properties':{'address':address,'text':{'type':'string','maxLength':8192},**provenance}}
    function={'type':'object','required':['address','end_address','instructions','direct_xrefs','pseudocode','pseudocode_status',*provenance],
              'properties':{'address':address,'end_address':address,'name':{'type':'string'},
                            'instructions':{'type':'array','maxItems':512,'items':instruction},
                            'direct_xrefs':{'type':'array','maxItems':64,'items':xref},
                            'pseudocode':{'anyOf':[{'type':'null'},pseudo]},'pseudocode_status':{'type':'string'},**provenance}}
    save(HERE/'result.schema.json',{'$schema':'https://json-schema.org/draft/2020-12/schema',
         'title':'IDA77 draft static export (no export generated in this delivery)','type':'object',
         'required':['schema','status','input_executed'],
         'properties':{'schema':{'const':'ida77-bounded-result-v1'},'status':{'enum':['EXPORTED','DISASSEMBLY_ONLY_DECOMPILER_UNAVAILABLE','REFUSED','EXPORT_ERROR']},
                       'input_executed':{'const':False},'followed_target_functions':{'const':False},
                       'functions':{'type':'array','maxItems':8,'items':function}},
         'allOf':[{'if':{'properties':{'status':{'enum':['EXPORTED','DISASSEMBLY_ONLY_DECOMPILER_UNAVAILABLE']}}},
                   'then':{'required':['source','ida_version','python_version','decompiler','functions','xref_policy','followed_target_functions']}}]})
    cases=[('positive_input',request,False,'VALIDATED_INPUT_ONLY',None),
           ('positive_execute_gate',request,True,'STOPPED_UNSAFE_AUTOPLUGINS',None)]
    for name,key,value,code in [
        ('wrong_sha','source_sha256','0'*64,'HASH_MISMATCH'),
        ('out_of_range','functions',[{'rva':0x1000,'end_rva':0x3000}],'OUT_OF_RANGE'),
        ('empty_allowlist','functions',[],'EMPTY_ALLOWLIST'),
        ('wrong_base','image_base',0x500000,'IMAGE_BASE_MISMATCH'),
        ('overlap','functions',[{'rva':0x1000,'end_rva':0x100d},{'rva':0x1004,'end_rva':0x1009}],'OVERLAPPING_ALLOWLIST'),
        ('non_code_range','functions',[{'rva':0x100,'end_rva':0x110}],'OUT_OF_RANGE'),
        ('excess_time_budget','limits',dict(DEFAULTS,timeout_seconds=61),'INVALID_LIMITS'),
        ('unauthorized_scope','authorization_scope','LITO','SCOPE_NOT_AUTHORIZED')]:
        r=copy.deepcopy(request);r[key]=value
        cases.append((name,r,True,'REFUSED',code))
    checks=[];commands=[]
    for name,r,execute,status,code in cases:
        rp=HERE/'requests'/(name+'.json');receipt=HERE/'receipts'/(name+'.json')
        save(rp,r)
        command=[sys.executable,'-B',str(HERE/'ida77_runner.py'),'--request',str(rp),'--receipt',str(receipt)]
        if execute:command.append('--execute')
        proc=subprocess.run(command,cwd=str(HERE),capture_output=True,text=True,timeout=15)
        actual=json.loads(receipt.read_text('utf-8'))
        passed=(proc.returncode==(0 if status=='VALIDATED_INPUT_ONLY' else 2) and actual['status']==status
                and actual.get('code')==code and actual['ida_started'] is False and actual['pid'] is None
                and actual['database_created'] is False and actual['input_executed'] is False)
        checks.append({'name':name,'passed':passed,'expected_status':status,'expected_code':code,
                       'receipt':record(receipt),'exit_code':proc.returncode})
        commands.append({'argv':command,'exit_code':proc.returncode,'stdout':proc.stdout.strip(),
                         'stderr':proc.stderr.strip(),'ida_launches':0})
    preserved=sha((HERE/'receipts'/'positive_input.json').read_bytes())
    try:
        run(request,HERE/'receipts'/'positive_input.json')
        code='NO_REFUSAL'
    except Refusal as exc:
        code=exc.code
    checks.append({'name':'existing_receipt_preserved','passed':code=='OUTPUT_EXISTS' and sha((HERE/'receipts'/'positive_input.json').read_bytes())==preserved,'actual_code':code})
    # Injection tests exercise classification only. They do not represent IDA's
    # installed decompiler availability or attempt to load any IDA module.
    def absent(_):raise ImportError('controlled classification fixture')
    def false_init(_):return SimpleNamespace(init_hexrays_plugin=lambda:False)
    for name,importer,expected in [('decompiler_module_absent',absent,'DECOMPILER_MODULE_UNAVAILABLE'),
                                  ('decompiler_init_false',false_init,'DECOMPILER_NOT_AVAILABLE')]:
        module,result=decompiler_probe(importer)
        checks.append({'name':name,'passed':module is None and result['status']==expected,
                       'actual':result,'evidence_level':'controlled pure-Python classification test, not installed IDA runtime evidence'})
    for name in ['ida77_request.py','ida77_runner.py','ida77_export.py']:
        ast.parse((HERE/name).read_text('utf-8'),feature_version=(3,8))
    checks.append({'name':'python38_syntax','passed':True,'runtime_import_or_IDA_API_compatibility_claimed':False})
    checks.append({'name':'no_database_or_bytecode_created','passed':not any(HERE.rglob('*.idb')) and not any(HERE.rglob('*.i64')) and not any(HERE.rglob('*.pyc'))})
    result={'schema':'ida77-new-preflight-tests-v1','created_at':datetime.now(timezone.utc).isoformat(),
            'status':'PASS' if all(x['passed'] for x in checks) else 'FAIL','checks':checks,
            'passed':sum(x['passed'] for x in checks),'total':len(checks),'commands':commands,
            'fixture_source':record(SOURCE),'fixture_owned_copy':record(fixture),
            'new_IDA_positive_export_test':'NOT_RUN_UNSAFE_AUTOPLUGINS',
            'new_IDA_timeout_or_disk_watchdog_tests':'NOT_RUN_EXECUTION_PROFILE_UNQUALIFIED',
            'IDA_launches':0,'fixture_executions':0,'old_ROOT_PASS_matrix_replayed':False}
    save(HERE/'new-tests.json',result)
    print(json.dumps({'status':result['status'],'passed':result['passed'],'total':result['total'],'IDA_launches':0}))
    return 0 if result['status']=='PASS' else 1

if __name__=='__main__':raise SystemExit(main())
