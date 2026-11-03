"""Explicit manual-test launch; original READY/claim/ownership/cleanup retained."""
import hashlib,importlib.util,json,sys
from pathlib import Path
import candidate_startup
R=Path(__file__).resolve().parent
def configure():
    module,_=candidate_startup.configure()
    spec=importlib.util.spec_from_file_location("selection_owned_check",R/"selection_server_entry.py")
    entry=importlib.util.module_from_spec(spec);spec.loader.exec_module(entry)
    receipt=entry.receipt
    module.DLL=Path(receipt["dll"]["path"])
    module.ENTRY=R/"selection_server_entry.py"
    for path in (module.DLL,module.ENTRY,entry.JAR,R/"build/selection-recovery/qualification.json"):
        module.PINS[path]=hashlib.sha256(path.read_bytes()).hexdigest().upper()
    module.check_source(module.CLIENT)
    for name in ("__init__","preflight","start_pair","check_alive","cleanup","__enter__","__exit__"):
        assert getattr(entry.Pair,name).__globals__["JAR"]==entry.JAR
    return module,receipt
def main():
    # Keep the established candidate entry, now with LiTo-specific DLL loading.
    from named_startup import main as named_main
    return named_main()
if __name__=="__main__":sys.exit(main())
