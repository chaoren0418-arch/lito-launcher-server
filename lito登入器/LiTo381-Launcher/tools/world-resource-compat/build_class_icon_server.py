from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
from build_selection_recovery import EXPORTS
R=Path(__file__).resolve().parent
C=R/'build/class-icons'
P=R/'build/selection-recovery'
def pin(p):return {'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
def main():
    parent=json.loads((P/'qualification.json').read_text('utf-8'))
    jar=Path(parent['jar']['path'])
    assert pin(jar)==parent['jar']
    javac=shutil.which('javac');java=str(Path(javac).with_name('java.exe'))
    subprocess.run([javac,*EXPORTS,'-d',str(C),str(R/'ClassIconServerPatch.java')],check=True)
    changes={}
    with zipfile.ZipFile(jar) as source:
        for name in ['Protocol','ClientSession']:
            key='local/lito381/server/'+name+'.class'
            a=C/(name+'.before.class');b=C/(name+'.after.class');a.write_bytes(source.read(key))
            subprocess.run([java,*EXPORTS,'-cp',str(C),'ClassIconServerPatch',str(a),str(b)],check=True)
            changes[key]=b.read_bytes()
        target=C/jar.name
        with zipfile.ZipFile(target,'w') as out:
            for entry in source.infolist():out.writestr(entry,changes.get(entry.filename,source.read(entry.filename)))
    with zipfile.ZipFile(jar) as a,zipfile.ZipFile(target) as b:
        assert a.namelist()==b.namelist()
        assert {n for n in a.namelist() if a.read(n)!=b.read(n)}==set(changes)
    subprocess.run([javac,'-encoding','UTF-8','-cp',str(target),'-d',str(C),str(R/'ClassIconWireProbe.java')],check=True)
    result=subprocess.run([java,'-Xverify:all','-cp',str(target)+';'+str(C),'local.lito381.server.ClassIconWireProbe'],check=True,capture_output=True,text=True)
    print(result.stdout);(C/'server-test.log').write_text(result.stdout,'utf-8')
    deps=[];(C/'lib').mkdir(exist_ok=True)
    for item in parent['dependencies']:
        p=Path(item['path']);assert pin(p)==item
        q=C/'lib'/p.name;shutil.copy2(p,q);deps.append(pin(q))
    (C/'server-build.json').write_text(json.dumps({'status':'qualified-packet-candidate',
        'parent':pin(jar),'candidate':pin(target),'dependencies':deps,'changed_entries':list(changes),
        'formal_updated':False,'source_database_modified':False},indent=2),'utf-8')
if __name__=='__main__':main()
