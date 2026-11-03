"""Isolated Source17 area geometry layer atop the pinned3636 candidate. No formal changes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
import sys,re
stage=sys.argv[1];assert stage in ['before','after'];parent=read(D/('source-lightning-output-build-current.json'if stage=='before'else'source-weapon-buff-target-build-current.json'));base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256'];seed=read(Path(parent['manifest']['path']));source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/datatables/EquipProcTable.java');text=source.read_text('utf-8');start=text.index('private static EquipProc readProc(');end=text.index('\n\t}',start);columns=re.findall(r'rs.getInt\("([^"\n]+)"\)',text[start:end]);assert len(columns)==18 and columns[:4]==['編號','道具編號','強化值','觸發效果群組'];canonical='id,item_id,enchant_level,proc_group,on_attack_chance,on_hurt_chance,flat_damage,rand_damage,heal_hp,heal_hp_rand,heal_mp,heal_mp_rand,heal_mode,trigger_skill,skill_time_sec,skill_target,gfx_id,gfx_mode'.split(',');old='SELECT '+','.join(canonical)+' FROM `裝備_armor_魔法` ORDER BY item_id,enchant_level,proc_group,id';next='SELECT '+','.join('`'+x+'` AS `'+y+'`'for x,y in zip(columns,canonical))+' FROM `裝備_armor_魔法` ORDER BY item_id,enchant_level,proc_group,id';run=D/('source-armor-sql-'+stage+'-builds')/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_source_armor_sql.py','SourceArmorSqlMethods.java','FreezeAcceptedMethods.java','SourceNegativeTargetLoad.java']:shutil.copyfile(R/name,inputs/name)
shutil.copyfile(source,inputs/source.name);(inputs/'old-query.txt').write_text(old,'utf-8');(inputs/'Source-query.txt').write_text(next,'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],Source=pin(source),asm=seed['asm'],inputs=[pin(p)for p in inputs.iterdir()],mapping=list(zip(canonical,columns)),full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',*[p for p in inputs.glob('*.java')if p.name!='EquipProcTable.java']]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceArmorSqlMethods','splice',base,classes,inputs/'old-query.txt',inputs/'Source-query.txt'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  jar=run/'candidate-source-armor-readonly-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceArmorSqlMethods','verify',base,jar,inputs/'old-query.txt',inputs/'Source-query.txt']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceNegativeTargetLoad',jar,S/'lib'])
 v.update(status='built-current-Source-armor-readonly-projection-unqualified',runtime=pin(jar))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source-armor-sql-'+stage+'-build-current.json'),dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
