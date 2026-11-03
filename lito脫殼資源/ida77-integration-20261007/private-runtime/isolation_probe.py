import sys
sys.dont_write_bytecode=True
import os,json,socket,ctypes as C
from ctypes import wintypes as W
from pathlib import Path
out=Path(os.environ['LITO_IDA_JOB']);canary=Path(os.environ['LITO_CANARY'])
result={'python_version':sys.version,'sys_path':sys.path,'site_loaded':'site' in sys.modules,
    'cwd':os.getcwd(),'environment_paths':{k:os.environ[k] for k in ['IDAUSR','TEMP','APPDATA','LOCALAPPDATA','USERPROFILE']}}
def denied(name,call):
    try:call();result[name]={'denied':False}
    except OSError as e:result[name]={'denied':True,'winerror':getattr(e,'winerror',None),'errno':e.errno,'exception':type(e).__name__}
(out/'write-test.txt').write_text('private probe only','utf-8');result['private_write_succeeded']=True
denied('outside_read',lambda:canary.read_bytes())
def attempt_write():
    with canary.open('ab') as f:f.write(b'UNEXPECTED-WRITE')
denied('outside_write',attempt_write)
sock=socket.socket();sock.settimeout(.5)
denied('network_connect',lambda:sock.connect(('127.0.0.1',int(os.environ['LITO_LOCAL_PORT']))));sock.close()
result['no_network_payload_sent']=True
(out/'probe-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(result))
