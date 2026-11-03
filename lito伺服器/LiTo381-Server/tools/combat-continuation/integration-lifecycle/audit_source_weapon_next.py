"""Exact candidate references for the remaining PC weapon and publication paths; no qualification."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, subprocess, sys

R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/((sys.argv[1]if len(sys.argv)>1 else 'source-weapon-counter-magic-build')+'-current.json'))
assert pin(index['manifest']['path'])==index['manifest'] and pin(index['runtime']['path'])==index['runtime']
build=read(index['manifest']['path']);run=D/'source-weapon-next-reference'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
source=run/'SourceWeaponNextRefs.java'
source.write_text('''import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class SourceWeaponNextRefs{public static void main(String[]a)throws Exception{String p="local/lito381/server/";var lines=new ArrayList<String>();lines.add("owner\\tmethod\\tdescriptor\\tinstruction\\tkind\\treference");try(var jar=new JarFile(a[0])){for(String owner:List.of("ClientSession","NativeWeaponBreakBattleCompanion","NativeWeaponBreakTargetLifecycle","NativeSourceWeaponCounterMagic")){var c=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(p+owner+".class"))).accept(c,ClassReader.SKIP_DEBUG);for(var m:c.methods){if(owner.equals("ClientSession")&&!m.name.equals("nativeMaterialWeaponBreakBody"))continue;int i=0;for(var n:m.instructions){if(n instanceof MethodInsnNode call)lines.add(owner+"\\t"+m.name+"\\t"+m.desc+"\\t"+i+"\\tcall\\t"+call.owner+"."+call.name+call.desc);else if(n instanceof FieldInsnNode f)lines.add(owner+"\\t"+m.name+"\\t"+m.desc+"\\t"+i+"\\tfield\\t"+f.owner+"."+f.name+":"+f.desc);else if(n instanceof LdcInsnNode l&&l.cst instanceof String text)lines.add(owner+"\\t"+m.name+"\\t"+m.desc+"\\t"+i+"\\tstring\\t"+text.replace('\\n',' ').replace('\\t',' '));i++;}}}}Files.write(Path.of(a[1]),lines);System.out.println("LOCATED "+(lines.size()-1)+" exact weapon PC/controlled/publication references; semantics remain unqualified");}}''','utf-8')
for ref in build['asm']:assert pin(ref['path'])==ref
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
cp=';'.join(x['path']for x in build['asm']);refs=run/'references.tsv'
with (run/'audit.log').open('w',encoding='utf-8')as log:
 subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(run/'tools'),str(source)],cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
 subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',str(run/'tools')+';'+cp,'SourceWeaponNextRefs',index['runtime']['path'],str(refs)],cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=120)
rows=refs.read_text('utf-8').splitlines()[1:]
signals=[line for line in rows if any(x in line.lower()for x in ['countermagic','targetpublication','captureoutput','reservetarget'])]
write(run/'signals.json',dict(exact_references=signals,meaning='Static call and field references only. PC dynamic exceptions, full weapon/equipment durability and output lifecycle need native execution; absent direct reference is not proof of a missing path.'))
v=dict(status='located-exact-remaining-weapon-PC-and-publication-references-no-semantic-qualification',runtime=index['runtime'],basis=index['manifest'],inputs=[pin(Path(__file__)),pin(source)],artifacts=[pin(refs),pin(run/'signals.json'),pin(run/'audit.log')],next_action='Continue real Source-equipment durability overlay and original exception rollback, accepted/final receiver lifecycle, Ctrl/game cursor and all other material/producer paths; the native runtime-weapon branch is tracked separately in current scoped progress.',new_SQL_connections=0,real_server_started=False,full_PC_weapon_qualified=False,full_goal_achieved=False,formal_deployment_changed=False)
write(run/'manifest.json',v);write(D/'source-weapon-next-reference-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],references=len(rows),signals=len(signals),manifest=pin(run/'manifest.json'))))
