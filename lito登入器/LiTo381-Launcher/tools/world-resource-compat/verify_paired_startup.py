"""Check candidate ownership wiring and fail-closed identities without SQL."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile

R=Path(__file__).resolve().parent


def load(path,name):
    spec=importlib.util.spec_from_file_location(name,path)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def pin(path):
    return {"path":str(path),"sha256":hashlib.sha256(path.read_bytes()).hexdigest()}


def main():
    server=json.loads((R/"build/presentation-server.json").read_text("utf-8"))
    client=json.loads((R/"build/status-candidate.json").read_text("utf-8"))
    wrapper=load(R/"candidate_server_entry.py","paired_checked")
    official=load(Path(server["formal_entry"]["path"]),"official_checked")
    assert wrapper.JAR==Path(client["paired_server"]["path"])
    assert wrapper.JAR_SHA256==client["paired_server"]["sha256"]
    assert wrapper.CANDIDATE_ROOT==R/"build/candidate"
    assert wrapper.PROJECT==official.PROJECT and wrapper.CATALOG==official.CATALOG
    for name in ("__init__","preflight","start_pair","check_alive","cleanup","__enter__","__exit__"):
        left=getattr(wrapper.Pair,name);right=getattr(official.Pair,name)
        assert left.__code__.co_code==right.__code__.co_code
        assert left.__globals__["JAR"]==wrapper.JAR
    rejected=[]
    mutations=(
        ("unqualified",lambda s,c:s.update(status="incomplete")),
        ("different-client-pair",lambda s,c:c["paired_server"].update(sha256="0"*64)),
        ("wrong-jar",lambda s,c:(s["candidate"].update(sha256="0"*64),
                               c["paired_server"].update(sha256="0"*64))),
        ("wrong-formal-entry",lambda s,c:s["formal_entry"].update(sha256="0"*64)),
        ("changed-server-receipt",lambda s,c:c["paired_server_receipt"].update(sha256="0"*64)),
    )
    with tempfile.TemporaryDirectory(prefix="paired-entry-",dir=R/"build") as folder:
        tmp=Path(folder)
        assert tmp.resolve().parent==(R/"build").resolve() and not tmp.is_symlink()
        (tmp/"build").mkdir()
        entry=tmp/"candidate_server_entry.py"
        entry.write_bytes((R/"candidate_server_entry.py").read_bytes())
        for name,mutate in mutations:
            badserver,badclient=copy.deepcopy(server),copy.deepcopy(client)
            mutate(badserver,badclient)
            (tmp/"build/presentation-server.json").write_text(json.dumps(badserver),"utf-8")
            (tmp/"build/status-candidate.json").write_text(json.dumps(badclient),"utf-8")
            try:load(entry,"rejected_"+name.replace("-","_"))
            except ValueError:rejected.append(name)
            else:raise AssertionError("invalid pair accepted "+name)
        assert not (tmp/"build/candidate/runs").exists()
    result={
        "status":"passed-no-process-start",
        "inputs":[pin(R/"candidate_server_entry.py"),pin(R/"candidate_startup.py"),
                  pin(R/"build/status-candidate.json"),pin(R/"build/presentation-server.json")],
        "server":server["candidate"],"client":client["candidate"],
        "rejected":rejected,"unchanged_owned_methods":7,
        "scope":"Entry identity checks and reuse of READY/ownership/cleanup function bodies; isolated run root",
        "limits":["No SQL, live READY, process shutdown or UI executed by this check."],
        "formal_updated":False,"source_database_modified":False,
    }
    (R/"build/paired-startup.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),"utf-8")
    print("PASS paired entry: 7 owned methods retained, 5 invalid identities rejected before SQL/process start")


if __name__=="__main__":
    main()
