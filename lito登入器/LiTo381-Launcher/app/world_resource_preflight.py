"""Preflight by default. --launch explicitly invokes the existing owned startup.

This file does not replace the formal launcher. A future native launch still
uses its exact READY/claim/identity/cleanup flow; no gate is bypassed.
"""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import struct
import sys

R = Path(__file__).resolve().parent / "resources/world"
L = R.parents[1]

def verify_resource_sources():
    receipt_path = R / "inputs/effectlist2.xml.extraction.json"
    if not receipt_path.exists():
        receipt_path = R / "inputs/extraction.json"
    receipt = json.loads(receipt_path.read_text("utf-8"))
    index = Path(receipt["archive"])
    raw = index.read_bytes()
    if hashlib.sha256(raw).hexdigest() != receipt["archiveIndexSha256"]:
        raise ValueError("shared Tile index changed; re-export and qualify resources")
    count = struct.unpack_from("<I", raw)[0]
    if len(raw) != 4 + count * 28:
        raise ValueError("shared Tile index layout changed")
    members = []
    for i in range(count):
        offset, name, size = struct.unpack_from("<I20sI", raw, 4+28*i)
        if name.split(b"\0")[0].lower() == b"effectlist2.xml":
            members.append((offset, size))
    if len(members) != 1:
        raise ValueError("effectlist2.xml archive identity changed")
    offset, size = members[0]
    archive = index.with_suffix(".pak")
    if size > 2_000_000 or offset+size > archive.stat().st_size:
        raise ValueError("effectlist2.xml archive bounds changed")
    with archive.open("rb") as stream:
        stream.seek(offset)
        encoded = stream.read(size)
    if hashlib.sha256(encoded).hexdigest() != receipt["encodedSha256"]:
        raise ValueError("shared effectlist2.xml changed; rebuild candidate catalog before launch")
    icons = json.loads((R / "icon-assets.json").read_text("utf-8"))
    for row in icons["index_pins"]:
        if hashlib.sha256(Path(row["path"]).read_bytes()).hexdigest() != row["sha256"]:
            raise ValueError("shared icon index changed; requalify resource routing")
    # Pin cache sizing too: an unchanged TBT member can still be unreachable
    # when list.ico changes without changing the archive index.
    for row in icons["capacity_candidates"]:
        with Path(row["archive"]).open("rb") as stream:
            stream.seek(row["offset"])
            payload = stream.read(row["bytes"])
        if hashlib.sha256(payload).hexdigest() != row["sha256"]:
            raise ValueError("shared TBT cache capacity changed; requalify list.ico")
    for icon in icons["assets"]:
        for row in icon["candidates"]:
            with Path(row["archive"]).open("rb") as stream:
                stream.seek(row["offset"])
                payload = stream.read(row["bytes"])
            if hashlib.sha256(payload).hexdigest() != row["sha256"]:
                raise ValueError("shared status icon changed: " + icon["member"])


def configure(client,dll):
    option=os.environ.get('LITO_SHARED_EXPIRED_ICONS','')
    if option not in ('','0','1'):raise ValueError('LITO_SHARED_EXPIRED_ICONS must be 0 or 1')
    verify_resource_sources()
    app=Path(__file__).resolve().parent
    if (Path(client)/'ddraw.dll').exists():raise ValueError('Unexpected client ddraw.dll; use the LiTo-specific DLL')
    receipt=app/'resources/class-icons/resources.json'
    os.environ['LITO_NAMED_DDRAW_SHA256']=hashlib.sha256(Path(dll).read_bytes()).hexdigest()
    os.environ['LITO_CLASS_ICON_RECEIPT']=str(receipt)
    os.environ['LITO_CLASS_ICON_RECEIPT_SHA256']=hashlib.sha256(receipt.read_bytes()).hexdigest()
    os.environ['LITO_SHARED_STATUS_CATALOG']=str(app/'compat/shared_status.bin')
