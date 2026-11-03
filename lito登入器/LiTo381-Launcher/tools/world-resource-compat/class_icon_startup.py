"""Isolated class icon candidate. Preserve formal release and owned startup."""
from pathlib import Path
import hashlib,importlib.util,json,os,sys
import named_startup
R=Path(__file__).resolve().parent
C=R/'build/class-icons'

def configure():
    module,parent=named_startup.configure()
    build=json.loads((C/'native-build.json').read_text('utf-8'))
    dll=Path(build['candidate']['path'])
    if hashlib.sha256(dll.read_bytes()).hexdigest()!=build['candidate']['sha256']:
        raise RuntimeError('Qualified class icon DLL changed')
    if build['parent']['sha256']!=parent['dll']['sha256']:
        raise RuntimeError('Class icon parent differs from qualified startup')
    resource=C/'resources.json'
    assets=json.loads(resource.read_text('utf-8'))
    for item in assets['original']+assets['package']:
        if hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()!=item['sha256']:
            raise RuntimeError('Class icon archive changed: '+item['path'])
    source=module.PROBE.read_text('utf-8')
    seam='                report["named_ddraw"] = install_named_ddraw(__import__("sys").modules[__name__], process.hProcess, root_pid, cwd)\n'
    if source.count(seam)!=1:raise RuntimeError('Class icon bootstrap seam changed')
    source=source.replace(seam,seam+'                from class_icon_bootstrap import install_archive\n'
        +'                report["class_icon_archive"] = install_archive(__import__("sys").modules[__name__], process.hProcess, root_pid, cwd)\n')
    probe=C/'lito_runtime_probe_class_icons.py'
    probe.write_text(source,'utf-8')
    module.PROBE=probe
    module.DLL=dll
    module.ENTRY=R/'class_icon_server_entry.py'
    spec=importlib.util.spec_from_file_location('class_icon_startup_check',module.ENTRY)
    entry=importlib.util.module_from_spec(spec);spec.loader.exec_module(entry)
    for name in ('__init__','preflight','start_pair','check_alive','cleanup','__enter__','__exit__'):
        assert getattr(entry.Pair,name).__globals__['JAR']==entry.owned.JAR
    for path in (dll,probe,resource,C/'native-build.json',R/'class_icon_bootstrap.py',Path(__file__),
                 module.ENTRY,entry.owned.JAR,C/'server-build.json'):
        module.PINS[path]=hashlib.sha256(path.read_bytes()).hexdigest().upper()
    os.environ['LITO_NAMED_DDRAW_SHA256']=build['candidate']['sha256']
    os.environ['LITO_CLASS_ICON_RECEIPT']=str(resource)
    os.environ['LITO_CLASS_ICON_RECEIPT_SHA256']=hashlib.sha256(resource.read_bytes()).hexdigest()
    return module,build

def main():
    module,build=configure()
    if len(sys.argv)==1 or sys.argv[1]=='--check-only':
        print(json.dumps({'status':'class-icon-candidate-preflight','dll':build['candidate'],
                          'formal_updated':False},ensure_ascii=False));return 0
    if sys.argv[1]!='--launch':raise ValueError('use --check-only or --launch')
    sys.argv=[str(module.__file__),*sys.argv[2:]]
    return module.main()
if __name__=='__main__':sys.exit(main())
