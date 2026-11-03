from pathlib import Path
import hashlib, json, os, shutil, subprocess, sys, zipfile
R=Path(__file__).resolve().parent; S=R.parents[2]; B=S/'tools/resource-database/build'
D=R/('build-repro' if '--reproduce' in sys.argv else 'build'); D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8')); sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
base=R.parent/'elemental-material/build/candidate-elemental-material.jar'
assert sha(base)=='b750acfc32e43963e163366e7f00cd1afcac817785f124625b012c468ea38c59'
inputs=D/'inputs'; inputs.mkdir(exist_ok=True)
for p in [Path(__file__),R/'ElementalLifetimeMethods.java',B/'NativeSkillConsumptionMethods.java',
          Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/Instance/L1SummonInstance.java')]:
    shutil.copyfile(p,inputs/p.name)
source=(inputs/'L1SummonInstance.java').read_text('utf-8')
assert 'this.set_time(1800);' in source
asm=[Path(p['path']) for p in read(R.parent/'build/integration-regression/preparation.json')['asm']]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'); tool=D/'tools'; tool.mkdir(exist_ok=True)
cp=';'.join(map(str,[tool,*asm])); env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='building',base=pin(base),formal_deployment_changed=False,full_goal_achieved=False)
try:
    with (D/'build.log').open('w',encoding='utf-8') as log:
        def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
        run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',tool,inputs/'ElementalLifetimeMethods.java',inputs/'NativeSkillConsumptionMethods.java')
        classes=D/'classes'; classes.mkdir(exist_ok=True)
        run(J/'java.exe','-Xverify:all','-cp',cp,'ElementalLifetimeMethods','splice',base,classes)
        with zipfile.ZipFile(base) as z: entries={n:z.read(n) for n in z.namelist()}
        n='local/lito381/server/ClientSession.class'; entries[n]=(classes/n).read_bytes()
        candidate=D/'candidate-elemental-lifetime.jar'
        with zipfile.ZipFile(candidate,'w') as z:
            for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
        run(J/'java.exe','-Xverify:all','-cp',cp,'ElementalLifetimeMethods','verify',base,candidate)
        with zipfile.ZipFile(base) as old,zipfile.ZipFile(candidate) as new:
            assert old.namelist()==new.namelist()
            assert [name for name in old.namelist() if old.read(name)!=new.read(name)]==[n]
    v.update(status='built-unqualified',runtime=pin(candidate),changed=[n],inputs=[pin(p) for p in inputs.iterdir()])
except BaseException as e:
    v.update(status='failed',error=str(e)); print((D/'build.log').read_text('utf-8',errors='replace')[-4000:]); raise
finally:(D/'build.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['runtime']))
