"""Adapt the sealed native fixture runner; preserve failed packet-boundary evidence."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
source=R/'test_choices.py';text=source.read_text('utf-8')
for old,new in [("D=R/'build/choices'","D=R/'build/packets'"),('SummonChoicesProbe','SummonPacketProbe'),("row['cases']==62","row['cases']==8")]:
    assert old in text;text=text.replace(old,new)
D=R/'build/packets';D.mkdir(exist_ok=True)
target=D/'driver.py';target.write_text(text,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(D/'adaptation.json').write_text(json.dumps(dict(source=dict(path=str(source),sha256=sha(source)),driver=dict(path=str(target),sha256=sha(target)),changes='destination, probe class and expected case count only'),indent=2),encoding='utf-8')
exec(compile(text,str(target),'exec'),{'__file__':str(R/'test_packets.py'),'__name__':'__main__'})
