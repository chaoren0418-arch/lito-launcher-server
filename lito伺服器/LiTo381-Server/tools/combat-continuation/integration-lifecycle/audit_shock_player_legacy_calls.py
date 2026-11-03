from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,subprocess,shutil
R=Path(__file__).resolve().parent;D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'shock-player-build-current.json');jar=Path(index['runtime']['path']);assert sha(jar)==index['runtime']['sha256'];build=read(Path(index['manifest']['path']));run=D/'shock-player-legacy-call-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p in [Path(__file__),R/'LegacyMaterialCallAudit.java']:shutil.copyfile(p,run/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'tools',*[p['path']for p in build['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'audit.log'
with log.open('w',encoding='utf-8')as out:
 subprocess.run(list(map(str,[J/'javac.exe','-g:none','-cp',cp,'-d',run/'tools',run/'LegacyMaterialCallAudit.java'])),env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
 subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'LegacyMaterialCallAudit',jar,run/'calls.tsv'])),env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
lines=(run/'calls.tsv').read_text('utf-8').splitlines()[1:];assert len(lines)==1 and any('\tuseWorldRoiCancellation\t'in x for x in lines)
v=dict(status='audited-one-remaining-legacy-call-locator-not-semantic-qualification',runtime=index['runtime'],legacy_call_sites=1,table=pin(run/'calls.tsv'),inputs=[pin(run/n)for n in ['LegacyMaterialCallAudit.java',Path(__file__).name]],log=pin(log),created_Hub=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);write(run/'manifest.json',v);write(D/'shock-player-legacy-call-audit-current.json',dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')));print(json.dumps(dict(status=v['status'],legacy_call_sites=1)))
