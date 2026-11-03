"""Carry the accepted 0.6.69 BUFF fix forward without rebuilding other source."""
from pathlib import Path
import hashlib,json,os,subprocess,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build';F=S/'tools/buff-schema';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
build=read(D/'build.json');base=Path(build['runtime']['path']);assert sha(base)==build['runtime']['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');asm=Path.home()/'.m2/repository/org/ow2/asm';classes=D/'buff-classes';classes.mkdir(exist_ok=True);tcp=';'.join(map(str,[classes,asm/'asm/9.7/asm-9.7.jar',asm/'asm-tree/9.7/asm-tree-9.7.jar']));candidate=D/'candidate-combat-buff.jar'
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
with (D/'buff-integration.log').open('w',encoding='utf-8') as out:
 def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=120)
 run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',base,'-d',classes,S/'src/main/java/local/lito381/server/SourceBuffRead.java',F/'BuffReadRegression.java')
 run(J/'javac.exe','-encoding','UTF-8','-cp',tcp,'-d',classes,F/'PatchBuffRead.java',F/'VerifyBuffPatch.java')
 run(J/'java.exe','-cp',tcp,'PatchBuffRead',base,classes,candidate)
 run(J/'java.exe','-cp',tcp,'VerifyBuffPatch',base,candidate)
 run(J/'java.exe','-Xverify:all','-cp',str(candidate)+';'+str(classes),'local.lito381.server.BuffReadRegression')
with zipfile.ZipFile(base) as a,zipfile.ZipFile(candidate) as b:
 changes=[n for n in b.namelist() if n not in a.namelist() or b.read(n)!=a.read(n)]
 assert set(changes)=={'local/lito381/server/LegacyMySqlRepository.class','local/lito381/server/SourceBuffRead.class'}
receipt={'status':'passed-buff-composition-only','runtime':pin(candidate),'base':pin(base),'changed_entries':changes,'combat_classes_byte_identical':True,'formal_deployment_changed':False,'full_goal_achieved':False}
(D/'integrated.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(receipt))
