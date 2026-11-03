from pathlib import Path
import subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/*')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),str(R/'ProbeSpecialBuffOrigins.java')],check=True)
p=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Xverify:all',
 '-cp',str(B/'tests')+';'+cp,'local.lito381.server.ProbeSpecialBuffOrigins',str(S/'config/server.properties'),str(B/'source-blessing-map-inputs.tsv')],cwd=S,capture_output=True,encoding='utf-8',timeout=60)
(B/'special-buff-origins.log').write_text(p.stdout+p.stderr,encoding='utf-8');print(p.stdout+p.stderr);raise SystemExit(p.returncode)
