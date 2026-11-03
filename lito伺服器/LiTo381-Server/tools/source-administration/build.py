"""Build only the source-account authority patch from the verified .42 rollback archive."""
from pathlib import Path
import hashlib, json, subprocess, zipfile
R = Path(__file__).resolve().parent
S = R.parents[1]
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
B = R / 'build'
B.mkdir(exist_ok=True)
assert hashlib.sha256((B/'base.jar').read_bytes()).hexdigest()=='004b1ba8cc258f2f95a04ed3e6f1cbd63c1fe7737dc3db5d6b4c0de009130951', 'Retained historical input mismatch'

asm = Path.home() / '.m2/repository/org/ow2/asm'
acp = ';'.join(str(asm / f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree'])
def run(*args): subprocess.run(list(map(str,args)),check=True)
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',B/'base.jar','-d',B,
    S/'src/main/java/local/lito381/server/SourceAdministration.java',S/'tools/shared-database/UnifiedConfiguration.java')
run(J/'javac.exe','-encoding','UTF-8','-cp',acp,'-d',B,R/'Patch.java')
run(J/'java.exe','-cp',str(B)+';'+acp,'Patch',B/'base.jar',B,B/'candidate.jar')
print('Candidate SHA256:',hashlib.sha256((B/'candidate.jar').read_bytes()).hexdigest())
