"""未匯入、未接線。Re-run affected domains once, compare canonical artifacts.
No SELECT automatically rerun; only pure builders and offline resource readers.
"""
import hashlib,json,subprocess,sys
from pathlib import Path
P=Path(__file__).resolve().parent
FILES=['candidate-data.json','sources.json','provenance.json','01-candidate-schema.sql','02-candidate-data.sql','terrain-connectivity.json','configuration-comparison.json','item-resource-evidence.json','npc-service-evidence.json','skill-request-evidence.json','stage2-sprite-evidence.json','stage2-buff-evidence.json','stage2-map-page-evidence.json','stage2-portal-525-evidence.json','stage2-rental-evidence.json','pure-presentation-lito-legacy.json','pure-presentation-tw13081901-v1.json']
def hashes():return {n:hashlib.sha256((P/n).read_bytes()).hexdigest() for n in FILES}
def main():
    before=hashes()
    for script,args in [('enrich_stage2.py',['--refresh-packets']),('enrich_remaining.py',[]),('consolidate_sql.py',[])]:subprocess.run([sys.executable,str(P/script),*args],cwd=P,check=True)
    after=hashes();different=[n for n in FILES if before[n]!=after[n]]
    result=dict(package_status='未匯入、未接線',stage='all affected local domains',core_outputs_byte_identical=not different,files_checked=FILES,different_files=different,input_database_snapshot_refreshed=False,sql_executed=False,game_or_server_started=False)
    old=P/'reproducibility.json';prior=P/'phase1-reproducibility.json'
    if old.exists() and not prior.exists():prior.write_bytes(old.read_bytes())
    old.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    assert not different,('changed source or nondeterministic outputs; investigate, do not reseal as identical',different)
    print('full affected-domain byte-identical reproduction: PASS')
if __name__=='__main__':main()
