"""New harmless fixture: bounded lifetime/data; owned directory writes only."""
import sys
sys.dont_write_bytecode=True
import os
from pathlib import Path
import subprocess
import time
from owned_process import HERE,owned,save,identity,digest

def main():
    mode=sys.argv[1];directory=owned(Path.cwd())
    save(directory/(mode+'-start.json'),{'mode':mode,'identity':identity(-1,os.getpid()),
         'argv':sys.argv,'source_path':str(Path(__file__).resolve()),'source_sha256':digest(__file__),
         'start_monotonic':time.monotonic()})
    if mode=='normal':
        print('fixture normal stdout',flush=True);print('fixture normal stderr',file=sys.stderr,flush=True)
        time.sleep(.1);return 0
    if mode=='flood':
        for _ in range(256):
            os.write(1,b'O'*8192);os.write(2,b'E'*8192)
        return 0
    if mode=='disk':
        with (directory/'finite-payload.bin').open('xb',buffering=0) as f:
            for _ in range(64):
                f.write(b'D'*32768);time.sleep(.003)
        time.sleep(5);return 0
    if mode=='tree':
        p=subprocess.Popen([sys.executable,'-B',str(Path(__file__).resolve()),'leaf'],
              cwd=str(directory),creationflags=subprocess.CREATE_NO_WINDOW)
        save(directory/'child-created.json',{'child_pid':p.pid,'parent_pid':os.getpid(),
            'child_source_path':str(Path(__file__).resolve()),'child_source_sha256':digest(__file__)})
        time.sleep(30);return 0
    if mode=='sentinel':
        deadline=time.monotonic()+25
        while time.monotonic()<deadline:
            if (directory/'stop').exists():return 0
            time.sleep(.02)
        return 9
    if mode in ('hang','leaf'):
        time.sleep(30);return 0
    raise ValueError('Unknown fixture mode')

if __name__=='__main__':raise SystemExit(main())
