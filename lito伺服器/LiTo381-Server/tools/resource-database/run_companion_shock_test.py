from pathlib import Path
import subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/*')
sources=[R/(name+'.java') for name in ['CompanionShockRegression','CompanionDamageRegression','KiringkuRegression','ClassDamageRegression','WeaponBuffRegression','AwakenCombatRegression']]+[S/name for name in ['tools/durable-settlement/FlowRegression.java','tools/dragon-combat/Regression.java','tools/native-room-state/NativeDirectoryRegression.java','tools/shared-database/SourceLoginRegression.java']]
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],check=True)
with (B/'CompanionShockRegression.log').open('w',encoding='utf-8') as log:
 p=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.CompanionShockRegression',str(S/'config/server.properties'),str(S/'runtime/combat-companion-shock-regression')],cwd=S,stdout=log,stderr=subprocess.STDOUT,timeout=60)
print((B/'CompanionShockRegression.log').read_text('utf-8')[-4500:]);raise SystemExit(p.returncode)
