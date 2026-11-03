from pathlib import Path
import sys,struct,ctypes
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'app'))
import lito_runtime_probe_shared as m
orig_install=m.install_gap_veh;orig_read=m.read_process;orig_write=m.write_process
state={}
def install(*args,**kwargs):
 kwargs['shared_resource_gate']=True
 r=orig_install(*args,**kwargs)
 ctx=m.WOW64_CONTEXT();ctx.ContextFlags=m.WOW64_CONTEXT_FULL|0x10
 if not m.kernel32.Wow64GetThreadContext(args[1],ctypes.byref(ctx)):raise m.win_error('startup GetContext')
 ctx.Dr0=0x480a60;ctx.Dr6=0;ctx.Dr7=1
 if not m.kernel32.Wow64SetThreadContext(args[1],ctypes.byref(ctx)):raise m.win_error('startup SetContext')
 state.update(report=r,handle=args[0]);r['early_gate_target']='0x480a60'
 return r
def read(handle,address,size):
 result=orig_read(handle,address,size)
 if state and not state.get('released'):
  r=state['report'];mark=orig_read(handle,int(r['oep_marker_address'],16),4)
  if mark==struct.pack('<I',0x480a60) and orig_read(handle,0x480ab3,2)==b'\xeb\x07' and orig_read(handle,0x480326,2)==b'\xeb\x09':
   orig_write(handle,int(r['shared_release_address'],16),struct.pack('<I',1));state['released']=True;r['early_gate_released']=True
 return result
m.install_gap_veh=install;m.read_process=read
raise SystemExit(m.run_probe(m.parse_args()))
