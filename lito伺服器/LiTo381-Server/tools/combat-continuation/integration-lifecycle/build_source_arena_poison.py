"""Isolated arena poison provenance layer. Frozen replay uses only pinned inputs."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
replay=len(sys.argv)>1
if replay:
 original=read(Path(sys.argv[1]));S=Path(sys.argv[2]);run=Path(sys.argv[3]);parent=dict(runtime=original['base']);seed=original
else:
 parent=read(D/'source-raw-producer-build-current.json');seed=read(Path(parent['manifest']['path']));run=D/'source-arena-poison-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='30c3754cf46232d7cced67b62fcf989d28c189da00bd9cf2246d8de8c9832b5d'
inputs=run/'inputs';inputs.mkdir(parents=True)
names=['NativeSourceArenaPoison.java','SourceArenaPoisonMethods.java','SourceArenaPoisonDelta.java','SourceArenaPoisonLoad.java','FreezeAcceptedMethods.java','FreezeSourceTargetDelta.java','build_source_arena_poison.py']
for name in names:shutil.copyfile(R/name,inputs/name)
for ref in seed['asm']:assert sha(Path(ref['path']))==ref['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log'
v=dict(status='building',base=parent['runtime'],asm=seed['asm'],inputs=[pin(inputs/n)for n in names],new_game_runtime_cases=0,new_SQL_connections=0,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',run/'tools',*[inputs/n for n in names if n.endswith('.java')and n!='NativeSourceArenaPoison.java']])
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceArenaPoisonMethods','splice',base,classes,S/'lib'])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceArenaPoison.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  jar=run/'candidate-source-arena-poison-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceArenaPoisonMethods','verify',base,jar,S/'lib'])
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceArenaPoisonDelta',base,jar])
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceArenaPoisonLoad',jar,S/'lib'])
 v.update(status='built-Source-arena-poison-provenance-program-unqualified',runtime=pin(jar))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:
 v['log']=pin(log);write(run/'manifest.json',v)
 if not replay:write(D/'source-arena-poison-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
