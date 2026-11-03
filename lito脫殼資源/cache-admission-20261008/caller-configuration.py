"""Source-only caller qualification; does not execute the route."""
import hashlib,json
from pathlib import Path
OUT=Path(__file__).resolve().parent;M=OUT.parent.parent/'lito重建版/LiTo381-Modern/src/modern_lito'
def pin(p):return dict(path=str(p),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest().upper())
def source(name,expected,lo,hi):
 p=M/name;r=pin(p);assert r['sha256']==expected,'source drift';lines=p.read_text(encoding='utf-8').splitlines()
 return dict(**r,line_start=lo,line_end=hi,raw_lines=lines[lo-1:hi])
d=dict(evidence_level='static_source_caller_configuration',flow=source('native_city_flow.rs','BB962767D158D23CB087D406D83F1450EE5EF36990CC1C9CAC096B7CFE42634D',4,9),bounded_route=source('native_city_resources.rs','16837FE97B14CD3F7A0BABF882B34473CF3C3E08993E4FB3BDB9113978AA03A3',50,53),admission=source('native_city_resources.rs','16837FE97B14CD3F7A0BABF882B34473CF3C3E08993E4FB3BDB9113978AA03A3',77,80),limits=source('native_resources.rs','B410648F02C87523FC91716C869E9ECE98BDDBD436CAA19C52A927E21A23F93B',94,128),cache=source('native_city_tile_cache.rs','5B22DC2AD88568B38A11862EDF5AEEE7F237CADDC7EABB2C1EAA4965702E99A4',1,11),conclusion='At the recorded source SHAs, Scene::load chooses city for plans >=16 sectors using default limits; load_whole_bounded supplies Some(min(default16MiB,default decode64MiB))=Some16MiB. Thus the conditional admission discrepancy is on this static route for the exact16-sector plan. No current runtime invocation/binary execution is proved.',va_rva='N/A Rust text; source file lines and SHA retained.')
(OUT/'caller-configuration.json').write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('PASS source caller configuration pinned: >=16 sectors -> bounded route -> default64MiB decode and Some16MiB cache; execution not observed')
