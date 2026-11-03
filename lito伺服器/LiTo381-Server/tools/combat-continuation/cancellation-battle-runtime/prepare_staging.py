from pathlib import Path
import ast,hashlib,json,shutil
R=Path(__file__).resolve().parent;old=R.parent/'call-nature-material/build';d=R/'build';s=d/'staging';s.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));read=lambda p:json.loads(p.read_text('utf-8'));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(d/'build.json');prior=read(old/'build.json');source=old/'staging/Start-Shared-Formal.py';text=source.read_text('utf-8')
text=text.replace(repr(prior['runtime']['path']),repr(b['runtime']['path'])).replace(prior['runtime']['sha256'],b['runtime']['sha256'])
q=s/source.name;q.write_text(text,'utf-8')
back=text.replace(repr(b['runtime']['path']),repr(prior['runtime']['path'])).replace(b['runtime']['sha256'],prior['runtime']['sha256']);assert ast.dump(ast.parse(back))==ast.dump(ast.parse(source.read_text('utf-8')))
(d/'lib').mkdir(exist_ok=True);deps=[]
for ref in read(old/'staging/adaptation-receipt.json')['dependencies']:
 p=Path(ref['path']);assert sha(p)==ref['sha256'];target=d/'lib'/p.name;shutil.copyfile(p,target);deps.append(pin(target))
write(s/'adaptation-receipt.json',dict(source=pin(source),adapted=pin(q),runtime=b['runtime'],dependencies=deps,change='Only JAR path/hash; AST unchanged elsewhere',formal_deployment_changed=False,source_database_modified=False))
source=old/'staging/development_startup.py';text=source.read_text('utf-8');oldEntry=str((old/'staging/Start-Shared-Formal.py').resolve());newEntry=str(q.resolve())
text=text.replace(repr(oldEntry),repr(newEntry)).replace(repr(prior['runtime']['path']),repr(b['runtime']['path'])).replace(sha(old/'staging/Start-Shared-Formal.py').upper(),sha(q).upper()).replace(prior['runtime']['sha256'].upper(),b['runtime']['sha256'].upper())
newNative=s/'development_startup.py';newNative.write_text(text,'utf-8')
back=text.replace(repr(newEntry),repr(oldEntry)).replace(repr(b['runtime']['path']),repr(prior['runtime']['path'])).replace(sha(q).upper(),sha(old/'staging/Start-Shared-Formal.py').upper()).replace(b['runtime']['sha256'].upper(),prior['runtime']['sha256'].upper());assert ast.dump(ast.parse(back))==ast.dump(ast.parse(source.read_text('utf-8')))
t=ast.parse(text);assignments={n.targets[0].id:n.value for n in t.body if isinstance(n,ast.Assign)and isinstance(n.targets[0],ast.Name)};pinsNode=assignments['PINS']
for k,v in zip(pinsNode.keys,pinsNode.values):assert sha(Path(ast.literal_eval(k.args[0])))==ast.literal_eval(v).lower()
binpath=Path(ast.literal_eval(assignments['CLIENT'].args[0]))/'LiTo.bin';assert sha(binpath)==ast.literal_eval(assignments['BIN_SHA']).lower()
write(s/'native-entry-receipt.json',dict(status='prepared-pins-verified-not-launched',source=pin(source),adapted=pin(newNative),runtime=b['runtime'],server_entry=pin(q),verified_native_pins=len(pinsNode.keys)+1,native_bin=pin(binpath),native_ui_verified=False,user_manual_validation='deferred until remaining work complete',formal_deployment_changed=False,source_database_modified=False))
print('Prepared SQL/native entries; verified',len(pinsNode.keys)+1,'pins; native not launched')
