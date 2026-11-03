from pathlib import Path
import subprocess
r=Path(__file__).resolve().parent;w=r.parent
j=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin')
cp=str(r/'candidate.jar')+';'+str(w/'server-details-033/lib/*')
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(r/'test-classes'),str(r/'GmRegression.java')],check=True)
args=[str(j/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(w/'common_catalog.bin'),'-Xverify:all','-cp',str(r/'test-classes')+';'+cp,'local.lito381.server.GmRegression',str(w/'server-skills-details/runs/development-f8693d57f1b34156bf7d561323819f66/config/server.dev.standard.properties')]
p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
(r/'gm-regression.log').write_bytes(p.stdout);print(p.stdout.decode('utf-8',errors='replace'));p.check_returncode()
