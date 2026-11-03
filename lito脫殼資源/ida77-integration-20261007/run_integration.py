import sys
sys.dont_write_bytecode=True
import json
from pathlib import Path
from owned_process import HERE,run_owned,save,digest
PIN='AD639F124F953C4992605E8BAC98E282E6B9F96C71EABFCF5B8B1101DD223C4B'
if digest(HERE/'owned_process.py')!=PIN:raise RuntimeError('Accepted watchdog SHA mismatch')
mode=sys.argv[1];runs=HERE/'runs';runs.mkdir(exist_ok=True)
job=runs/(mode+'-'+sys.argv[2] if len(sys.argv)>2 else mode)
if mode=='probe':
    canary=HERE/'outside-private-canary.txt'
    if not canary.exists():canary.write_bytes(b'BIN-owned medium-integrity sentinel; no private SID access')
    before=digest(canary)
result=run_owned([sys.executable,'-B',str(HERE/'appcontainer_launcher.py'),mode,str(job)],job,
    timeout_seconds=30,capture_bytes=8192,disk_limit_bytes=16*1024*1024,disk_poll_seconds=.05)
print(json.dumps(result,ensure_ascii=False))
if mode=='probe':
    native=json.loads((job/'native-launch.json').read_text('utf-8')) if (job/'native-launch.json').exists() else {}
    probe=json.loads((job/'profile-output'/'probe-result.json').read_text('utf-8')) if (job/'profile-output'/'probe-result.json').exists() else {}
    passed=(native.get('exit_code')==0 and not native.get('error') and native.get('token_before_resume',{}).get('is_lpac')==1
       and native.get('listener_accepted_connection') is False and probe.get('private_write_succeeded') is True
       and probe.get('outside_read',{}).get('winerror')==5 and probe.get('outside_write',{}).get('winerror')==5
       and probe.get('network_connect',{}).get('winerror')==10013 and before==digest(canary) and not probe.get('site_loaded',True))
    save(HERE/'isolation-qualification.json',{'decision':'QUALIFIED_PRIVATE_LPAC' if passed else 'BLOCKED_PRIVATE_LPAC',
        'probe_job':str(job),'native_receipt':str(job/'native-launch.json'),'probe_receipt':str(job/'profile-output'/'probe-result.json'),
        'sentinel_before_sha256':before,'sentinel_after_sha256':digest(canary),'watchdog_sha256':PIN,
        'requested_boundary':'Empty-capability LPAC token plus private RW, denied own sibling read/write, denied localhost connection; no profile registration',
        'native_process_created':native.get('created',False),'native_probe_executed':native.get('resumed',False),
        'qualified_for_IDA_fixture_run':passed})
