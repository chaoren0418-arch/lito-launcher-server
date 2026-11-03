"""Owned startup for the qualified selection-recovery child, without publication."""
import importlib.util,json,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parent
WORK=ROOT/"build/selection-recovery"
receipt=json.loads((WORK/"qualification.json").read_text("utf-8"))
if receipt["status"]!="selection-recovery-candidate-qualified-native-ui-pending":
    raise ValueError("unqualified selection recovery candidate")
for row in [receipt[k] for k in ("jar","dll","parent_jar","parent_dll")]+receipt["dependencies"]+list(receipt["tests"].values())+receipt["sources"]:
    if hashlib.sha256(Path(row["path"]).read_bytes()).hexdigest()!=row["sha256"]:
        raise ValueError("selection recovery input changed: "+row["path"])
expected={"mysql-connector-j-8.0.33.jar","protobuf-java-3.21.9.jar",
          "HikariCP-5.1.0.jar","slf4j-api-1.7.36.jar"}
if {Path(r["path"]).name for r in receipt["dependencies"]}!=expected:
    raise ValueError("selection runtime dependencies incomplete")
for row in receipt["dependencies"]:
    if Path(row["path"]).resolve()!=(WORK/"lib"/Path(row["path"]).name).resolve():
        raise ValueError("selection runtime dependency outside candidate")
spec=importlib.util.spec_from_file_location("selection_owned_parent",ROOT/"candidate_server_entry.py")
_parent=importlib.util.module_from_spec(spec);spec.loader.exec_module(_parent)
_parent._owned.JAR=Path(receipt["jar"]["path"])
_parent._owned.JAR_SHA256=receipt["jar"]["sha256"]
_parent._owned.CANDIDATE_ROOT=WORK
def __getattr__(name):return getattr(_parent,name)
