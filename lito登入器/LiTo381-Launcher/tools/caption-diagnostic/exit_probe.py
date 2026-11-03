from pathlib import Path
import sys,struct
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'app'))
import lito_runtime_probe_shared as m
original=m.update_process_exit_trace
def trace(handle,report):
 original(handle,report)
 if report.get('telemetry',{}).get('call_count'):
  report['startup_bytes']={hex(a):m.read_process(handle,a,n).hex() for a,n in ((0x480ab3,4),(0x48253c,5),(0x48254a,5),(0x480a60,8),(0x4f1759,5))}
m.update_process_exit_trace=trace
raise SystemExit(m.run_probe(m.parse_args()))
