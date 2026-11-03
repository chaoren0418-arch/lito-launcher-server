"""Reuse owned READY/lease/stop logic with the sealed presentation JAR.

Import performs file validation only. SQL, processes and writable run paths
are used only when the original Pair is explicitly invoked by a later launch.
"""
import hashlib
import importlib.util
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parent
receipt=json.loads((ROOT/"build/presentation-server.json").read_text("utf-8"))
client=json.loads((ROOT/"build/status-candidate.json").read_text("utf-8"))
if receipt["status"]!="paired-presentation-server-qualified":
    raise ValueError("unqualified paired server")
if client["paired_server"]!=receipt["candidate"]:
    raise ValueError("client/server candidate mismatch")
for row in (receipt["candidate"],receipt["formal_entry"],client["paired_server_receipt"]):
    if hashlib.sha256(Path(row["path"]).read_bytes()).hexdigest()!=row["sha256"]:
        raise ValueError("paired startup input changed: "+row["path"])
dependencies=json.loads((ROOT/"build/candidate/runtime-dependencies.json").read_text("utf-8"))
expected_dependencies={"mysql-connector-j-8.0.33.jar","protobuf-java-3.21.9.jar",
                       "HikariCP-5.1.0.jar","slf4j-api-1.7.36.jar"}
if {Path(row["path"]).name for row in dependencies}!=expected_dependencies:
    raise ValueError("candidate runtime dependency inventory mismatch")
for row in dependencies:
    target=ROOT/"build/candidate/lib"/Path(row["path"]).name
    if target.resolve()!=Path(row["path"]).resolve():
        raise ValueError("candidate runtime dependency path mismatch")
    if hashlib.sha256(target.read_bytes()).hexdigest()!=row["sha256"]:
        raise ValueError("candidate runtime dependency changed: "+target.name)
spec=importlib.util.spec_from_file_location("world_candidate_owned_pair",receipt["formal_entry"]["path"])
_owned=importlib.util.module_from_spec(spec)
spec.loader.exec_module(_owned)
_owned.JAR=Path(receipt["candidate"]["path"])
_owned.JAR_SHA256=receipt["candidate"]["sha256"]
_owned.CANDIDATE_ROOT=ROOT/"build/candidate"


def __getattr__(name):
    return getattr(_owned,name)
