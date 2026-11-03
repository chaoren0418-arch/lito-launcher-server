"""Isolated Source17 area geometry layer atop the pinned3636 candidate. No formal changes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
import sys,re
stage=sys.argv[1];assert stage in ['before','after'];parent=read(D/('source-armor-sql-'+stage+'-build-current.json'));base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256'];seed=read(Path(parent['manifest']['path']));source=Path('D:/lineage開發/1-伺服器/db/維護工具/20261023_裝備欄位對照.json');mapping=read(source);assert len(mapping)==21 and sum(map(len,mapping.values()))==899 and mapping['weapon']['item_id']==mapping['armor']['item_id']=='item_id'
orders={'裝備_weapon_屬性':' ORDER BY `id`','裝備_特殊_魔石_魔法':' ORDER BY `item_id`, `tier`','裝備_weapon_魔法':' ORDER BY `weapon_id`, `id`','裝備_armor_魔法':' ORDER BY `item_id`, `enchant_level`, `proc_group`, `id`'}
selected=['裝備_weapon_屬性','裝備_weapon_bless_類型','裝備_armor_bless_類型','裝備_weapon_enchant_類型','裝備_armor_enchant_類型','裝備_weapon_enchant_指定','裝備_armor_enchant_指定','裝備_weapon_greater_類型','裝備_armor_greater_類型','裝備_weapon_bless_指定','裝備_armor_bless_指定','裝備_特殊_魔石_魔法','裝備_特殊_卡片_能力','裝備_weapon_魔法','裝備_armor_魔法','weapon','armor'];queries={}
for table in selected:
 old='SELECT * FROM `'+table+'`'+orders.get(table,'');next='SELECT '+','.join('`'+new+'` AS `'+old+'`'for old,new in mapping[table].items())+' FROM `'+table+'`'+orders.get(table,'');queries[old]=next
quote=lambda x:json.dumps(x,ensure_ascii=False)
helper='package local.lito381.server;\nimport java.sql.*;import java.util.*;\n/** Exact current Source equipment-column names. Existing Owner whitelist and read-only transaction own all IO. */\nfinal class NativeSourceEquipmentColumns {\n static final Map<String,String> QUERIES=Map.ofEntries(\n'+',\n'.join('  Map.entry('+quote(k)+','+quote(v)+')'for k,v in queries.items())+');\n static String translate(String q)throws SQLException{if(!NativeDeItemDefinitionsSql.QUERIES.contains(q)&&!NativeDeItemDefinitionsSql.TEMPLATE_QUERIES.contains(q))throw new SQLException("not an existing Owner read query");return QUERIES.getOrDefault(q,q);}\n static PreparedStatement prepare(Connection connection,String query)throws SQLException{return connection.prepareStatement(translate(query));}\n private NativeSourceEquipmentColumns(){}\n}\n'
run=D/('source-equipment-columns-'+stage+'-builds')/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_source_equipment_columns.py','SourceEquipmentColumnsMethods.java','FreezeAcceptedMethods.java','SourceNegativeTargetLoad.java']:shutil.copyfile(R/name,inputs/name)
shutil.copyfile(source,inputs/source.name);(inputs/'NativeSourceEquipmentColumns.java').write_text(helper,'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],Source=pin(source),asm=seed['asm'],inputs=[pin(p)for p in inputs.iterdir()],canonical_queries=queries,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',*[p for p in inputs.glob('*.java')if p.name!='NativeSourceEquipmentColumns.java']]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceEquipmentColumnsMethods','splice',base,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceEquipmentColumns.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  jar=run/'candidate-source-equipment-column-projection-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceEquipmentColumnsMethods','verify',base,jar]);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceNegativeTargetLoad',jar,S/'lib'])
 v.update(status='built-current-Source-equipment-column-readonly-projection-unqualified',runtime=pin(jar))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source-equipment-columns-'+stage+'-build-current.json'),dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']}if'runtime'in v else{})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
