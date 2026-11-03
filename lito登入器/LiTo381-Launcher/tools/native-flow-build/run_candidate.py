from pathlib import Path
import importlib.util,hashlib,sys
L=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('candidate',L/'src/development_startup.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
m.DLL=L/'native/ddraw.dll';m.PINS[m.DLL]=hashlib.sha256(m.DLL.read_bytes()).hexdigest().upper()
m.PINS[m.PROBE]=hashlib.sha256(m.PROBE.read_bytes()).hexdigest().upper()
m.PINS[m.DELIVERY/'compat/ddraw.dll']=hashlib.sha256((m.DELIVERY/'compat/ddraw.dll').read_bytes()).hexdigest().upper()
sys.exit(m.main())
