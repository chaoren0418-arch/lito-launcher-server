"""Rebuild the paired candidate and require identical bytes; no publication."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys

R=Path(__file__).resolve().parent


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    dll=R/"build/candidate/ddraw.dll"
    jar=R/"build/candidate/LiTo381-SharedPresentation-candidate.jar"
    before={p.name:digest(p) for p in (dll,jar)}
    sources={p.name:digest(p) for p in R.iterdir() if p.suffix in (".py",".java",".h",".inc",".cpp")}
    commands=[]
    for script in ("build_presentation_server.py","build_candidate.py"):
        command=[sys.executable,"-B","-X","utf8",str(R/script)]
        call=subprocess.run(command,cwd=R,capture_output=True,text=True,timeout=60)
        assert call.returncode==0,call.stdout+call.stderr
        commands.append({"argv":command,"exit_code":call.returncode})
    after={p.name:digest(p) for p in (dll,jar)}
    assert before==after,("nonreproducible candidate",before,after)
    assert sources=={p.name:digest(p) for p in R.iterdir() if p.suffix in (".py",".java",".h",".inc",".cpp")}
    (R/"build/reproducibility.json").write_text(json.dumps({
        "before":before[dll.name],"after":after[dll.name],"byte_identical":True,
        "paired_before":before,"paired_after":after,"source_sha256":sources,
        "commands":commands,"formal_updated":False,
    },ensure_ascii=False,indent=2),"utf-8")
    print("PASS reproducible paired DLL/JAR; source files unchanged during rebuild")


if __name__=="__main__":
    main()
