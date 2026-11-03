from pathlib import Path
import subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/*')
sources=[R/'GroupHealingRegression.java',R/'CombatRegression.java',R/'PolluteHealingRegression.java',R/'NpcDebuffRegression.java',R/'WorldCompanionPhysicalRegression.java',R/'RangedDamageRegression.java',R/'CompanionDamageRegression.java',R/'KiringkuRegression.java',R/'ClassDamageRegression.java',R/'WeaponBuffRegression.java',R/'AwakenCombatRegression.java',S/'tools/durable-settlement/FlowRegression.java',S/'tools/dragon-combat/Regression.java',S/'tools/native-room-state/NativeDirectoryRegression.java',S/'tools/shared-database/SourceLoginRegression.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],check=True)
with (B/'GroupHealingRegression.log').open('w',encoding='utf-8') as log:
 p=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.GroupHealingRegression',str(S/'config/server.properties'),str(S/'runtime/combat-group-healing-regression')],cwd=S,stdout=log,stderr=subprocess.STDOUT,timeout=60)
print((B/'GroupHealingRegression.log').read_text('utf-8')[-4500:]);raise SystemExit(p.returncode)
