"""Bounded fail-closed wrapper; this install's auto-plugin gate prevents launch.

No profile modification, installer, process attach, input execution or network.
"""
import sys
sys.dont_write_bytecode=True
import argparse
import json
import time
from pathlib import Path
from ida77_request import validate_request,plugin_preflight,Refusal,sha

ROOT=Path(__file__).resolve().parent
IDA=Path(r'D:\IDA_Pro_7.7')
FIXTURE_SHA='5E8BD11307A3F9F6E323062E9C7FC7F9FF536471196613A931D3F94A2F8FF1E3'


def run(request, receipt_path, execute=False):
    """An unsafe dependency never reaches process creation.

    The execution route is deliberately unqualified/disabled until a separately
    accepted profile enforces plugin, egress and owned-write isolation. This
    wrapper must not convert a supplied boolean into an isolation certificate.
    """
    receipt_path=Path(receipt_path).resolve()
    if ROOT not in receipt_path.parents:
        raise Refusal('OUTPUT_PATH_REFUSED','Fixed owned output root only')
    if receipt_path.exists():
        raise Refusal('OUTPUT_EXISTS','Never overwrite receipt/DB')
    started=time.monotonic()
    receipt={'schema':'ida77-bounded-receipt-v1','ida_started':False,'pid':None,
             'input_executed':False,'database_created':False,'execute_requested':execute}
    try:
        v=validate_request(request)
        # Current human authorization is tool-fixture-only, irrespective of a
        # new filename or copied LITO range. A tool adapter grants no new scope.
        if v['source_sha256']!=FIXTURE_SHA or ROOT not in Path(v['source_path']).resolve().parents:
            raise Refusal('SCOPE_NOT_AUTHORIZED','Only the owned qualified synthetic fixture is authorized here')
        receipt['validated_request']=v
        environment=plugin_preflight(IDA)
        receipt['environment']=environment
        if not execute:
            receipt['status']='VALIDATED_INPUT_ONLY'
        elif not environment['qualified_for_launch']:
            receipt['status']='STOPPED_UNSAFE_AUTOPLUGINS'
            receipt['missing_control']='Accepted isolated plugin/egress/owned-write execution profile; short timeout alone is insufficient'
        else:
            receipt['status']='STOPPED_EXECUTION_PROFILE_UNQUALIFIED'
            receipt['missing_control']='No verified egress/owned-write profile or tested process watchdog; no process is started'
    except Refusal as e:
        receipt.update(status='REFUSED',code=e.code,detail=e.detail)
    receipt['elapsed_seconds']=round(time.monotonic()-started,6)
    receipt_path.parent.mkdir(parents=True,exist_ok=True)
    encoded=json.dumps(receipt,ensure_ascii=False,indent=2).encode('utf-8')
    if len(encoded)>262144:
        raise Refusal('OUTPUT_LIMIT','Preflight receipt cap')
    with receipt_path.open('xb') as stream:
        stream.write(encoded)
    return receipt


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--request',required=True)
    p.add_argument('--receipt',required=True)
    p.add_argument('--execute',action='store_true')
    a=p.parse_args()
    request=json.loads(Path(a.request).read_text('utf-8-sig'))
    r=run(request,a.receipt,a.execute)
    print(json.dumps({'status':r['status'],'ida_started':r['ida_started']},ensure_ascii=True))
    return 0 if r['status']=='VALIDATED_INPUT_ONLY' else 2


if __name__=='__main__':
    raise SystemExit(main())
