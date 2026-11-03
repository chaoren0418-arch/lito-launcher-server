from pathlib import Path
import hashlib,json,subprocess,os,shutil,sys,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/('build-repro' if '--reproduce' in sys.argv else 'build');D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
base=R.parent/'summon-material/build/candidate-summon-material.jar';assert sha(base)=='0efdc606c0d5fe1fc5511cabcac9c8b0f227a72ba34fd4183abe323dcb302a54'
inputs=D/'inputs';inputs.mkdir(exist_ok=True)
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
for p in [Path(__file__),R/'SummonSelectionMethods.java',B/'NativeSkillConsumptionMethods.java',source/'clientpackets/C_UseSkill.java',source/'model/skill/L1SkillUse.java',source/'model/skill/skillmode/SUMMON_MONSTER.java']:
    shutil.copyfile(p,inputs/p.name)
asm=[Path(p['path']) for p in read(R.parent/'build/integration-regression/preparation.json')['asm']]
tool=D/'tools';tool.mkdir(exist_ok=True);cp=';'.join(map(str,[tool,*asm]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='building',base=pin(base),formal_deployment_changed=False,full_goal_achieved=False)
try:
    with (D/'build.log').open('w',encoding='utf-8') as log:
        def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
        run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',tool,inputs/'SummonSelectionMethods.java',inputs/'NativeSkillConsumptionMethods.java')
        classes=D/'classes';classes.mkdir(exist_ok=True);run(J/'java.exe','-Xverify:all','-cp',cp,'SummonSelectionMethods','splice',base,classes)
        with zipfile.ZipFile(base) as z:entries={n:z.read(n) for n in z.namelist()}
        changed=[]
        for p in classes.rglob('*.class'):
            name=p.relative_to(classes).as_posix();assert name in entries
            if p.read_bytes()!=entries[name]:changed.append(name)
            entries[name]=p.read_bytes()
        assert set(changed)=={'local/lito381/server/'+name+'.class' for name in ['ClientSession','Protocol']}
        candidate=D/'candidate-summon-parity.jar'
        with zipfile.ZipFile(candidate,'w') as z:
            for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
        run(J/'java.exe','-Xverify:all','-cp',cp,'SummonSelectionMethods','verify',base,candidate)
    v.update(status='built-unqualified',runtime=pin(candidate),changed=changed,inputs=[pin(p) for p in inputs.iterdir()])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'build.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
