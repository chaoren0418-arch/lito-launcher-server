"""Owned SQL startup for the class-icon child; original ownership/cleanup retained."""
import importlib.util,json,hashlib
from pathlib import Path
R=Path(__file__).resolve().parent
C=R/'build/class-icons'
receipt=json.loads((C/'server-build.json').read_text('utf-8'))
if receipt['status']!='qualified-packet-candidate':raise ValueError('Unqualified class-icon server')
for item in [receipt['parent'],receipt['candidate']]+receipt['dependencies']:
    if hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()!=item['sha256']:
        raise ValueError('Class-icon server input changed: '+item['path'])
spec=importlib.util.spec_from_file_location('class_icon_owned_parent',R/'selection_server_entry.py')
parent=importlib.util.module_from_spec(spec);spec.loader.exec_module(parent)
owned=parent._parent._owned
owned.JAR=Path(receipt['candidate']['path'])
owned.JAR_SHA256=receipt['candidate']['sha256']
owned.CANDIDATE_ROOT=C
def __getattr__(name):return getattr(parent,name)
