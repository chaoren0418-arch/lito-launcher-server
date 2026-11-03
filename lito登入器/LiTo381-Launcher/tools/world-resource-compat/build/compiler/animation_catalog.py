"""Lossless TW sprite-definition inventory and LiTo compatibility preflight.

This reads source files only. It does not publish a runnable catalog while any
required parser feature lacks a native implementation. Keep raw command spans:
the retained TW text contains doubled/missing parentheses accepted by its lexer.
"""
from __future__ import annotations
import argparse
from collections import Counter
from dataclasses import dataclass, asdict
import hashlib
import json
from pathlib import Path
import re

RECORD = re.compile(r"^#(\d+)\b", re.M)
HEADER = re.compile(r"#(\d+)\s+(\d+)(?:=(\d+))?")
# A command is delimited by its following command, not a closing parenthesis.
# This preserves malformed-but-native-readable punctuation and inline commands.
COMMAND = re.compile(r"(?<![\w.])(\d+)(?:-(\d+))?[.,][ \t]*([A-Za-z_\x80-\xff][^\r\n(]*?|)\(")
ALIAS = re.compile(r"(?<![\w.])(\d+)\s*=\s*(\d+)")
# Eight retained source rows omit '(' in a line such as '26.damage spear1 4,'.
# The native numeric lexer skips the label and reads the following 1 as direction.
BARE_COMMAND = re.compile(r"^[ \t]*(\d+)\.([A-Za-z_ ][^\r\n(\d]*?)(?=\d+\s+\d+[, ])",re.M)
NUMERIC_METADATA = re.compile(r"^[ \t]*(1[01]\d)\.(?=-?\d)",re.M)

@dataclass
class Command:
    opcode: int
    variant: int | None
    label: str
    raw: str
    body: str
    alias_sprite: int | None = None

@dataclass
class Sprite:
    id: int
    image_count: int
    image_alias: int | None
    raw: str
    commands: list[Command]

def parse(data: bytes) -> list[Sprite]:
    # Latin-1 is a reversible byte view; labels are never re-encoded as UTF-8.
    text = data.decode("latin1")
    first = text.splitlines()[0].split()
    if not first or not first[0].isdigit():
        raise ValueError("missing declared sprite count")
    declared = int(first[0]); boundaries = list(RECORD.finditer(text))
    if declared != len(boundaries):
        raise ValueError(f"declared {declared} sprites, found {len(boundaries)}")
    sprites = []
    for ordinal, start in enumerate(boundaries):
        end = boundaries[ordinal+1].start() if ordinal+1 < len(boundaries) else len(text)
        raw = text[start.start():end]
        header = HEADER.match(raw)
        if not header or int(header[1]) != ordinal:
            raise ValueError(f"invalid/non-contiguous sprite at ordinal {ordinal}")
        commands = []
        matches = sorted([(m,'command') for m in COMMAND.finditer(raw,header.end())]
                         +[(m,'alias') for m in ALIAS.finditer(raw,header.end())]
                         +[(m,'bare') for m in BARE_COMMAND.finditer(raw,header.end())]
                         +[(m,'numeric') for m in NUMERIC_METADATA.finditer(raw,header.end())],key=lambda pair:pair[0].start())
        for i, (match, kind) in enumerate(matches):
            stop = matches[i+1][0].start() if i+1 < len(matches) else len(raw)
            if kind=='alias':
                commands.append(Command(int(match[1]),None,"alias",raw[match.start():stop],"",int(match[2])))
            elif kind=='bare':
                commands.append(Command(int(match[1]),None,match[2].strip(),raw[match.start():stop],raw[match.end():stop]))
            elif kind=='numeric':
                commands.append(Command(int(match[1]),None,'',raw[match.start():stop],raw[match.end():stop]))
            else:
                commands.append(Command(int(match[1]), int(match[2]) if match[2] else None,
                                        match[3].strip(), raw[match.start():stop], raw[match.end():stop]))
        sprites.append(Sprite(ordinal, int(header[2]), int(header[3]) if header[3] else None, raw, commands))
    return sprites

def references(sprite: Sprite) -> set[int]:
    refs = {sprite.image_alias} if sprite.image_alias is not None else set()
    for command in sprite.commands:
        if command.alias_sprite is not None:
            refs.add(command.alias_sprite)
            continue
        # References proved by both parsers. Sounds (< and [) are a separate namespace.
        numbers = [int(n) for n in re.findall(r"-?\d+", command.body)]
        if command.opcode in (101, 113) and numbers:
            refs.add(numbers[0])
        elif command.opcode == 105 and numbers:
            refs.update(numbers[1:1+numbers[0]])
        elif command.opcode == 109 and len(numbers) >= 2 and numbers[1] >= 0:
            refs.add(numbers[1])
        if command.opcode < 100:
            refs.update(int(n) for n in re.findall(r"\](-?\d+)", command.body) if int(n) >= 0)
    return refs

def closure(sprites: list[Sprite], roots: list[int]) -> list[int]:
    todo = list(roots); seen = set()
    while todo:
        sid = todo.pop()
        if sid in seen: continue
        if not 0 <= sid < len(sprites): raise ValueError(f"undefined sprite reference {sid}")
        seen.add(sid); todo.extend(references(sprites[sid])-seen)
    return sorted(seen)

def compact(sprite: Sprite, action_slots: int = 71, sound_extensions: list | None = None,
            metadata_extensions: dict | None = None) -> bytes:
    """Compile only fully implemented semantics; never silently drop commands."""
    parts = [str(sprite.image_count)+(f"={sprite.image_alias}" if sprite.image_alias is not None else "")]
    pending_sounds=[]
    pending_morph={}
    pending_furniture={}
    pending_switching={}
    pending_fades=[]
    for command in sprite.commands:
        if command.variant is not None:
            raise ValueError(f"sprite {sprite.id}: variant {command.variant} unsupported")
        if command.opcode in (115,116,117):
            # TW 0x4A4745..0x4A47DE validates these into locals only. Preserve
            # them in IR, perform identical range checks, emit no native field.
            numbers=[int(n) for n in re.findall(r'-?\d+',command.body)]
            lower,upper=(1,2) if command.opcode==115 else (0,8)
            if len(numbers)!=1 or not lower<=numbers[0]<=upper:
                raise ValueError(f'sprite {sprite.id}: invalid validation-only metadata {command.opcode}')
            continue
        if command.opcode==114:
            numbers=[int(n) for n in re.findall(r'-?\d+',command.body)]
            # TW helper0x5AD920 returns0 for size1: the standard actor already
            # occupies the single cell. Proven by actual helper emulation.
            if numbers==[1]:continue
            if metadata_extensions is not None and len(numbers)==1 and numbers[0] in (2,3,8):
                pending_furniture[sprite.id]=numbers[0]
                continue
        if command.opcode==118 and metadata_extensions is not None:
            numbers=[int(n) for n in re.findall(r'-?\d+',command.body)]
            if numbers!=[1]:raise ValueError(f'sprite {sprite.id}: invalid SwitchingSpriteType')
            pending_switching[sprite.id]=1
            continue
        if command.opcode==113 and metadata_extensions is not None:
            numbers=[int(n) for n in re.findall(r'-?\d+',command.body)]
            if len(numbers)!=1 or not 0<=numbers[0]<32768:
                raise ValueError(f'sprite {sprite.id}: invalid morph effect reference')
            pending_morph[sprite.id]=numbers[0]
            continue
        if command.opcode >= 113 or (command.opcode < 100 and command.opcode >= action_slots):
            raise ValueError(f"sprite {sprite.id}: command {command.opcode} requires native extension")
        if command.alias_sprite is not None:
            parts.append(f"{command.opcode}={command.alias_sprite}")
            continue
        # LiTo 0x43EACC already supports ]gfx in frame+0x0E / flag+0x12 bit4.
        # Its three sound-choice slots still differ from TW's five slots.
        command_body=command.body
        has_extra_sounds=bool(re.search(r"(?:[<\[^]-?\d+){4,}", command_body))
        has_fades=bool(re.search(r'[{}]',command_body))
        if has_extra_sounds or has_fades:
            if has_extra_sounds and sound_extensions is None:
                raise ValueError(f"sprite {sprite.id}: more than three frame sound choices")
            if has_fades and metadata_extensions is None:
                raise ValueError(f'sprite {sprite.id}: fade requires explicit native extension')
            from frame_ir import parse_action
            direction,frames=parse_action(command_body)
            pieces=[str(direction),str(len(frames))]
            pending=[]
            for index,frame in enumerate(frames):
                if len(frame.sounds)>5:raise ValueError(f'sprite {sprite.id}: more than five frame sound choices')
                text=f'{frame.image} {frame.index} {frame.ticks}'
                text+=''.join(frame.sound_mode+str(s) for s in frame.sounds[:3])
                if frame.event in ('{','}'):
                    if not 1<=frame.event_value<=32767:raise ValueError('invalid fade duration')
                    # Native ] parser writes the compatible value field. The
                    # mandatory postparse installer replaces bit4 with bit8/16
                    # before any actor is allowed to consume these frames.
                    text+=']'+str(frame.event_value)
                    pending_fades.append(dict(sprite=sprite.id,action=command.opcode,frame=index,
                                              flag=8 if frame.event=='{' else 16,duration=frame.event_value))
                elif frame.event:text+=frame.event+str(frame.event_value)
                if frame.attack:text+='!'
                if frame.restart:text+='>'
                pieces.append(text)
                if len(frame.sounds)>3:
                    pending.append(dict(sprite=sprite.id,action=command.opcode,frame=index,
                                        mode=frame.sound_mode,sounds=list(frame.sounds)))
            command_body=' '.join(pieces)
            pending_sounds.extend(pending)
        # Retained i/j suffixes are skipped by both clients' numeric lexers.
        body=re.sub(r'(?<=\d)[ij](?=\s|\))','',command_body)
        if re.search(r"[{}A-Za-z]", body):
            raise ValueError(f"sprite {sprite.id}: unsupported frame event or nonnumeric argument")
        body = re.sub(r"[.,:()]", " ", body)
        if re.search(r"[^\d\s=\-<>\[\]!^]", body):
            raise ValueError(f"sprite {sprite.id}: unrecognized numeric token")
        parts.append(str(command.opcode)+" "+re.sub(r"\s+"," ",body).strip())
    output=(" \r ".join(parts)+" \r ").encode("ascii")
    if sound_extensions is not None:sound_extensions.extend(pending_sounds)
    if metadata_extensions is not None and pending_morph:
        metadata_extensions.setdefault('morph_effects',{}).update(pending_morph)
    if metadata_extensions is not None and pending_furniture:
        metadata_extensions.setdefault('furniture_sizes',{}).update(pending_furniture)
    if metadata_extensions is not None and pending_switching:
        metadata_extensions.setdefault('switching_types',{}).update(pending_switching)
    if metadata_extensions is not None and pending_fades:
        metadata_extensions.setdefault('fade_events',[]).extend(pending_fades)
    return output

def audit(sprites: list[Sprite]) -> dict:
    opcodes = Counter(); missing = []; extensions = Counter(); variants = []
    for sprite in sprites:
        for ref in references(sprite):
            if not 0 <= ref < len(sprites): missing.append([sprite.id, ref])
        for command in sprite.commands:
            opcodes[command.opcode] += 1
            if command.variant is not None: variants.append([sprite.id, command.opcode, command.variant])
            if 71 <= command.opcode < 100: extensions["action_slots_71_to_99"] += 1
            if command.opcode >= 113 and command.opcode not in (115,116,117): extensions[f"metadata_{command.opcode}"] += 1
            if command.opcode < 100:
                for marker in "{}":
                    if marker in command.body: extensions[f"frame_marker_{marker}"] += 1
                if re.search(r"(?:[<\[^]-?\d+){4,}",command.body):extensions['sound_choice_slots_4_to_5']+=1
    return {"count": len(sprites), "command_count": sum(opcodes.values()),
            "opcodes": dict(sorted(opcodes.items())), "missing_references": missing,
            "variants": variants, "required_native_extensions": dict(extensions),
            "runtime_ready": False,
            "reason": "Native action-table/consumer expansion, metadata and frame events require implementation and validation."}

def main():
    ap=argparse.ArgumentParser();ap.add_argument("source",type=Path);ap.add_argument("output",type=Path)
    args=ap.parse_args();data=args.source.read_bytes();sprites=parse(data);args.output.mkdir(parents=True,exist_ok=True)
    with (args.output/"sprites.jsonl").open("w",encoding="utf-8") as f:
        for sprite in sprites: f.write(json.dumps(asdict(sprite),ensure_ascii=True)+"\n")
    report=audit(sprites);report.update(source=str(args.source.resolve()),source_sha256=hashlib.sha256(data).hexdigest())
    report["canaries"]={str(i):closure(sprites,[i]) for i in [66,11722,11723,11724,11725,11736,11738,11746,11748]}
    (args.output/"audit.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
    print(json.dumps({k:report[k] for k in ["count","command_count","required_native_extensions","canaries","runtime_ready"]}))

if __name__ == "__main__": main()
