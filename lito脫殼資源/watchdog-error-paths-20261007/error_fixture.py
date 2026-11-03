"""Successor-only harmless fixtures for the reviewed error paths."""
import sys
sys.dont_write_bytecode=True
import os,time,subprocess
from pathlib import Path
from owned_process import owned,save,identity,digest
def main():
    mode=sys.argv[1];directory=owned(Path.cwd())
    save(directory/(mode+'-start.json'),{'mode':mode,'identity':identity(-1,os.getpid()),
         'source_path':str(Path(__file__).resolve()),'source_sha256':digest(__file__)})
    if mode=='entrycap':
        for i in range(300):
            with (directory/('bounded-%03d.txt'%i)).open('xb') as f:f.write(b'E')
        save(directory/'entries-ready.json',{'files_written':300})
    if mode=='tree':
        p=subprocess.Popen([sys.executable,'-B',str(Path(__file__).resolve()),'leaf'],
             cwd=str(directory),creationflags=subprocess.CREATE_NO_WINDOW)
        save(directory/'child-created.json',{'pid':p.pid,'parent_pid':os.getpid()})
    if mode in ['hang','leaf','tree','entrycap']:
        print('error-path harmless stdout',flush=True)
        print('error-path harmless stderr',file=sys.stderr,flush=True)
        time.sleep(10);return 0
    if mode=='sentinel':
        end=time.monotonic()+20
        while time.monotonic()<end:
            if (directory/'stop.json').exists():return 0
            time.sleep(.01)
        return 8
    raise ValueError('Unknown error-path fixture')
if __name__=='__main__':raise SystemExit(main())
