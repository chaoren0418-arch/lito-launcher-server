"""Deliberately exits without finally; tests kernel KILL_ON_JOB_CLOSE."""
import sys
sys.dont_write_bytecode=True
import os
from pathlib import Path
import time
from owned_process import ContainedProcess,save,owned

def main():
    directory=owned(Path.cwd())
    inner=directory/'inner';inner.mkdir()
    p=ContainedProcess([sys.executable,'-B',str(Path(__file__).with_name('fixture_helper.py')),'tree'],inner)
    p.resume()
    deadline=time.monotonic()+2
    while time.monotonic()<deadline:
        p.observe()
        if len(p.members)>=2 and (inner/'leaf-start.json').exists():break
        time.sleep(.01)
    save(directory/'crash-checkpoint.json',{'controller_pid':os.getpid(),'exit_code_planned':73,
         'inner_policy':p.policy,'inner_source':p.source,'inner_members':p.snapshots(),
         'checkpoint_monotonic':time.monotonic(),'cleanup_function_called':False})
    # Outer test deliberately keeps its job handle open until observing inner
    # members exit. No parent cleanup is allowed to count as crash-test success.
    time.sleep(.2)
    os._exit(73)

if __name__=='__main__':main()
