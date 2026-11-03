"""Isolated existing readonly account equipment SELECT compatibility; never accesses Source/player DBs."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile,base64
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'source-kiringku-admission-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='89e2b6877c6215c029d3b760ade4013292bb85f9944f07014424cd2414452b58';seed=read(Path(parent['manifest']['path']));columns=read(Path(read(D/'source-equipment-columns-after-build-current.json')['manifest']['path']));mapping_ref=next(x for x in columns['inputs']if x['path'].endswith('.json'));source=Path(mapping_ref['path']);assert sha(source)==mapping_ref['sha256'];mapping=read(source)
run=D/'source-legacy-equipment-sql-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_source_legacy_equipment_sql.py','SourceLegacyEquipmentSqlMethods.java','FreezeAcceptedMethods.java','SourceNegativeTargetLoad.java']:shutil.copyfile(R/name,inputs/name)
shutil.copyfile(source,inputs/source.name)
for table in ['armor','weapon']:(inputs/(table+'-projection.txt')).write_text(','.join('`'+new+'` AS `'+old+'`'for old,new in mapping[table].items()),'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],asm=seed['asm'],inputs=[pin(p)for p in inputs.iterdir()],Source_or_player_DB_connections=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',*inputs.glob('*.java')]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLegacyEquipmentSqlMethods','splice',base,classes,inputs,run/'replacements.tsv'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  jar=run/'candidate-source-legacy-equipment-readonly-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLegacyEquipmentSqlMethods','verify',base,jar,inputs,run/'replacements.tsv']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceNegativeTargetLoad',jar,S/'lib'])
 changes=[]
 for line in (run/'replacements.tsv').read_text('utf-8').splitlines():
  owner,method,old,next=line.split('\t');old,next=map(lambda x:base64.b64decode(x).decode('utf-8'),[old,next]);assert old.count('?')==next.count('?')and old.count('%s')==next.count('%s')and old.count('\x01')==next.count('\x01');changes.append(dict(owner=owner,method=method,original=old,projected=next))
 assert len(changes)==5;v.update(status='built-five-existing-readonly-equipment-queries-unqualified',runtime=pin(jar),replacements=pin(run/'replacements.tsv'),changes=changes)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-legacy-equipment-sql-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
