"""Read the actual loaded skill catalog without creating a Hub or account session."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
read=lambda p:json.loads(p.read_text('utf-8'))
pin=lambda p:dict(path=str(p.resolve()),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'companion-forest-area-build-current.json');assert pin(Path(b['runtime']['path']))==b['runtime']
run=D/'material-catalog-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
p=run/'MaterialCatalogProbe.java'
p.write_text('''package local.lito381.server;
import java.nio.file.*;import java.util.*;
public final class MaterialCatalogProbe {public static void main(String[]a)throws Exception{
 ResourceDatabase.load(ServerConfig.load(Path.of(a[0]),"standard"));var f=CombatDatabase.class.getDeclaredField("current");f.setAccessible(true);var c=(CombatDatabase.Snapshot)f.get(null);
 for(var e:new TreeMap<>(c.skills()).entrySet())if(e.getValue().reagent()>0&&e.getValue().reagentCount()>0)System.out.println("MATERIAL\\t"+e.getKey()+"\\t"+e.getValue().reagent()+"\\t"+e.getValue().reagentCount()+"\\t"+e.getValue().mp());
 System.out.println("ROW161\\t"+BattleSpellCatalog.areaSilenceSpell(161));System.out.println("ROW167\\t"+BattleSpellCatalog.windShackleSpell(167));}}
''','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',b['runtime']['path'],S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'audit.log'
with log.open('w',encoding='utf-8') as out:
 for a in [[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',p],[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.MaterialCatalogProbe',S/'config/server.properties']]:subprocess.run(list(map(str,a)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
rows=[l for l in log.read_text('utf-8').splitlines() if l.startswith(('MATERIAL\t','ROW161\t','ROW167\t'))];tsv=run/'catalog.tsv';tsv.write_text('\n'.join(rows)+'\n','utf-8');ids=[int(l.split('\t')[1]) for l in rows if l.startswith('MATERIAL\t')];assert len(ids)==58
m=dict(status='passed-readonly-loaded-material-catalog-census-not-skill-semantic-qualification',runtime=b['runtime'],inputs=[pin(p),pin(run/Path(__file__).name),pin(S/'config/server.properties')],catalog=pin(tsv),log=pin(log),material_rows=len(ids),ids=ids,explanation='58 loaded reagent-positive skill rows differ from the historical 34 remaining static-method census. Neither count proves complete Source target/control/status lifecycle coverage.',created_Hub_workers_listeners_SQL_sessions=False,full_goal_achieved=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',m);write(D/'material-catalog-audit-current.json',dict(status=m['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=m['status'],material_rows=len(ids))))
