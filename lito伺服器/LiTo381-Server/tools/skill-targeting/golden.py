"""Generate the issue #305 fixture: 380_db.skills targeting columns + independent Source (1-伺服器
L1SkillUse.isTarget/makeTargetList/isTargetCalc) expectations for the 181 entry skills.

usage: py -3 -I golden.py <extract_skills.py 輸出> src/test/resources/server1/skills-targeting-181.tsv

Scenario (documented in docs/design/skill-targeting.md §5): PC caster, TYPE_NORMAL, not proc,
everyone alive / same instance / outside safety zone / no war / not GM, no barrier/hidden state,
clear line of sight, every candidate kind present inside the area of the primary target and on screen.
Candidate kinds: SELF, ENEMY (PC, no party/clan), PARTY (PC), CLAN (PC), MONSTER, PET (caster's own).
"""
import sys

ENTRY = [int(x) for x in (
    "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,"
    "41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,"
    "77,78,79,80,87,88,89,90,91,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,"
    "118,129,130,131,132,133,134,137,138,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,"
    "163,164,165,166,167,168,169,170,171,172,173,174,175,176,181,182,183,184,185,186,187,188,189,190,191,192,193,"
    "194,195,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220").split(',')]
assert len(ENTRY) == 181

KINDS = ['SELF', 'ENEMY', 'PARTY', 'CLAN', 'MONSTER', 'PET']
PCS = {'SELF', 'ENEMY', 'PARTY', 'CLAN'}
PRIMARIES = ['SELF', 'ENEMY', 'PARTY', 'MONSTER', 'PET']
HEAL_ALL, IMMUNE_TO_HARM, FOG, MASS_SLOW, MASS_TELEPORT = 49, 68, 66, 76, 69
TAMING, CREATE_ZOMBIE = 36, 41
NEEDS_DEAD = {41, 61, 75, 165}
PLACE = {58, 63}


def is_target(sid, r, primary, cha):
    target, tt, typ = r['target'], r['target_to'], r['type']
    pc_pc = primary in PCS
    is_pk = primary != 'MONSTER' and target == 'attack' and primary != 'SELF'
    attackish = target == 'attack' or typ == 64
    # L1SkillUse.java:1325-1336 (PC_NPC, primary is a non-pet NPC)
    if not pc_pc and primary == 'MONSTER' and (cha == 'PET' or cha in PCS):
        return False
    if typ == 64 and cha == 'SELF':  # :1378
        return False
    if cha == 'SELF' and sid == HEAL_ALL:  # :1384
        return False
    if (tt & 1 or tt & 4 or tt & 8) and cha == 'SELF' and sid != HEAL_ALL:  # :1389
        return True
    if attackish and not is_pk and cha == 'PET':  # :1397-1415
        return False
    if attackish and cha != 'MONSTER' and not is_pk and primary in PCS:  # :1417-1460
        return False
    # :1466 LOS clear in this scenario
    if sid in (TAMING, CREATE_ZOMBIE) and cha != 'MONSTER':  # :1497
        return False
    if sid in NEEDS_DEAD:  # :1507 target alive
        return False
    flg = False
    if tt & 1 and cha in PCS:  # :1571
        flg = True
    elif tt & 2 and cha in ('MONSTER', 'PET'):
        flg = True
    elif tt & 16 and cha == 'PET':
        flg = True
    if pc_pc and cha in PCS:  # :1606-1620
        if tt & 4 and cha == 'CLAN':
            return True
        if tt & 8 and cha == 'PARTY':
            return True
    return flg


def is_target_calc(sid, cha):  # :4049-4113 (safety zone / Non-PvP not modelled)
    if sid == FOG and cha == 'SELF':
        return False
    if sid == MASS_SLOW and cha in ('SELF', 'PET'):
        return False
    if sid == MASS_TELEPORT and cha != 'SELF':
        return False
    return True


def effects(sid, r, primary):
    if sid in PLACE:  # :457 FIRE_WALL / LIFE_STREAM return before makeTargetList (coordinate cast)
        return 'PLACE'
    if r['target_to'] == 0 and (r['type'] & 64) == 0 and sid != IMMUNE_TO_HARM:  # :1695
        lst = ['SELF']
    else:
        if not is_target(sid, r, primary, primary) and r['target'] != 'none':  # :1718
            return '-'
        lst = []
        if r['area'] == 0:  # :1758
            lst.append(primary)
        else:
            if r['target'] != 'none':
                lst.append(primary)
            if sid != HEAL_ALL and not (r['target'] == 'attack' or r['type'] == 64):
                lst.append('SELF')
            excluded = 'SELF' if r['area'] == -1 else primary  # getVisibleObjects(user) / (target, area) skip the centre object
            for cha in KINDS:
                if cha == excluded and cha not in ('ENEMY', 'MONSTER'):
                    continue
                if is_target(sid, r, primary, cha):
                    lst.append(cha)
    if not is_target_calc(sid, primary):  # :2779 aborts the whole skill
        return '-'
    out = sorted({c for c in lst if is_target_calc(sid, c)}, key=KINDS.index)
    return '+'.join(out) if out else '-'


def shape(sid, r):
    if sid in PLACE:
        return 'PLACE'
    if r['target_to'] == 0 and (r['type'] & 64) == 0 and sid != IMMUNE_TO_HARM:
        return 'SELF_ONLY'
    if r['area'] == 0:
        return 'SINGLE'
    if r['area'] == -1:
        return 'SCREEN'
    return 'AREA'


def los_blocks(r):  # :1466-1476 non-proc
    attack = (r['type'] & 64) == 64
    return attack or (not r['through'] and r['type'] not in (2, 32))


rows = {}
for line in open(sys.argv[1], encoding='utf-8'):
    if line.startswith('#') or line.startswith('skill_id'):
        continue
    f = line.rstrip('\n').split('\t')
    rows[int(f[0])] = dict(target=f[1], target_to=int(f[2]), type=int(f[3]), ranged=int(f[4]), area=int(f[5]),
                           through=int(f[6]), attr=int(f[8]), id=int(f[9]))
with open(sys.argv[2], 'w', encoding='utf-8', newline='\n') as w:
    w.write('# Issue #305 fixture. Columns 2-7: 1-伺服器 db/380_db.sql `skills` (target,target_to,type,ranged,area,through),\n')
    w.write('# read-only extract for the 181 entry skills. Remaining columns: independent expectations generated by\n')
    w.write('# tools/skill-targeting/golden.py from L1SkillUse.isTarget/makeTargetList/isTargetCalc (scenario: docs/design/skill-targeting.md §5).\n')
    w.write('skill_id\ttarget\ttarget_to\ttype\tranged\tarea\tthrough\tshape\tlos_blocks\t' + '\t'.join('on_' + p for p in PRIMARIES) + '\tattr\tid\n')
    for sid in ENTRY:
        r = rows[sid]
        cols = [sid, r['target'], r['target_to'], r['type'], r['ranged'], r['area'], r['through'], shape(sid, r),
                int(los_blocks(r))] + [effects(sid, r, p) for p in PRIMARIES] + [r['attr'], r['id']]
        w.write('\t'.join(map(str, cols)) + '\n')
print('ok', len(ENTRY))
