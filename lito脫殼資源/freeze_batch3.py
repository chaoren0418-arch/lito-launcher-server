"""Preserve accepted batch3 pins before any new boundary evidence."""
from audit import *
from seal_delta import seal
import shutil

def main():
    base=ROOT/'sealed-batch3/accepted-20261005'
    if (base/'snapshot-manifest.json').exists():
        print('Accepted batch3 snapshot already preserved; not overwritten.');return
    expected={'S1E-initialization-gates.json':'841FAE9D1234919AE6051490596AF5EB75629107D4D4A101AC93C6A25153C2DC','skill-gates/ranges.json':'885B4699FC4A986C86E649976A563061D4402689BC543CE5A68B8AD4EE25E39C','class4-slot-provenance.json':'845C8F7F6DC5834CCFC7DC2056311F596C2FF3C82CC26944744F60B54C2BA1B8'}
    for name,digest in expected.items():assert record(ROOT/name)['sha256']==digest
    names={'S1E-initialization-gates.json','class4-slot-provenance.json','launcher-skill-initialization.json','server-packet-delta.json','server-adoption-ack.json','handoff.json','監製進度.json','artifact-manifest.json','delivery-verification.json','skill_gate_audit.py','publish_skill_gates.py','閱讀入口.md'}
    names|={str(p.relative_to(ROOT))for p in (ROOT/'skill-gates').iterdir()if p.is_file()}
    rows=[]
    for name in sorted(names):
        src=ROOT/name;dst=base/name;dst.parent.mkdir(parents=True,exist_ok=True);pin=record(src);shutil.copyfile(src,dst);frozen=record(dst);assert pin['sha256']==frozen['sha256']
        rows.append({'original_path':pin['path'],'original_bytes':pin['bytes'],'original_sha256':pin['sha256'],'preserved':frozen})
    write('sealed-batch3/accepted-20261005/snapshot-manifest.json',{'updated_at':stamp(),'scope':'Accepted finite static batch3 stable copy. Nested original paths are historical identities; resolve by original_path -> preserved map. No pin rewrite or class/resource rerun.','accepted_primary_SHA256':expected,'files':rows,'runtime_closed':False,'GUI_accepted':False})
    names_out=['freeze_batch3.py']+[str(p.relative_to(ROOT))for p in base.rglob('*')if p.is_file()]
    checked=seal(names_out,Path(__file__));print(json.dumps({'preserved_files':len(rows),'delta_files_verified':checked,'snapshot_manifest':record(base/'snapshot-manifest.json')}))

if __name__=='__main__':main()
