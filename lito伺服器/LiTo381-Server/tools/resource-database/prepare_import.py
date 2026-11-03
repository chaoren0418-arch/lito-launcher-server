"""Generate only the current ten-table installation SQL after read-only verification."""
from pathlib import Path
import subprocess,sys
subprocess.run([sys.executable,'-X','utf8',str(Path(__file__).with_name('verify_import.py'))],check=True)

r=Path(__file__).resolve().parent
(r/"build/import.sql").write_bytes((r.parents[1]/"db/lito_resource_compact.sql").read_bytes())
